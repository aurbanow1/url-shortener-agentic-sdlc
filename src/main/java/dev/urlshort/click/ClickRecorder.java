package dev.urlshort.click;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dev.urlshort.web.RateLimitFilter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Records one click per redirect without ever delaying, changing or failing it (FR-7, business rules
 * 1, 2 and 5; ADR-0011). The request is reduced to a {@link Click} on the request thread, and only
 * that crosses to a single writer thread through a queue of {@value #QUEUE_CAPACITY}.
 *
 * <p>Fail open: a click that cannot be reduced (reason {@code reduction failed}), a full or closed queue
 * ({@code rejected}) or a failed write loses that click and writes exactly one WARN
 * {@code click lost} carrying the redirect's {@code requestId}, a {@code reason} and the exception's
 * class, never its message or any click value. On shutdown the writer drains for at most five seconds;
 * every click still unwritten is then reported once, before {@link #close()} returns (DR-01). Each
 * click's state decides which of the request thread, the writer or the shutdown reports it, so no
 * click is reported twice.
 *
 * <p>Counters (NFR-O3, ADR-0016 amendment): {@code urlshort.clicks.recorded} counts every insert that
 * returned, and {@code urlshort.clicks.lost}, tagged with the same {@code reason}, counts every
 * {@code click lost} event. A click whose insert returns after shutdown claimed it counts in both,
 * which is the truth for it.
 */
@Component
public class ClickRecorder {

	static final int QUEUE_CAPACITY = 10_000;
	static final Duration DRAIN_DEADLINE = Duration.ofSeconds(5);

	private static final Logger log = LoggerFactory.getLogger(ClickRecorder.class);
	private static final int QUEUED = 0;
	private static final int RUNNING = 1;
	private static final int DONE = 2;
	private static final int CLAIMED = 3;
	private static final String REJECTED = "rejected";
	private static final String REDUCTION_FAILED = "reduction failed";
	private static final String WRITE_FAILED = "write failed";
	private static final String SHUTDOWN_DEADLINE = "shutdown deadline";
	private static final String OUTCOME_UNKNOWN = "shutdown deadline, outcome unknown";

	private final ClickStore store;
	private final DailySalt salt;
	private final Duration drainDeadline;
	private final Counter recorded;
	private final Map<String, Counter> lostByReason = new HashMap<>();
	private final Set<ClickWrite> outstanding = ConcurrentHashMap.newKeySet();
	private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(QUEUE_CAPACITY), task -> {
				Thread thread = new Thread(task, "click-writer");
				thread.setDaemon(true);
				return thread;
			}, new ThreadPoolExecutor.AbortPolicy());

	@Autowired
	ClickRecorder(ClickStore store, DailySalt salt, MeterRegistry registry) {
		this(store, salt, registry, DRAIN_DEADLINE);
	}

	ClickRecorder(ClickStore store, DailySalt salt, MeterRegistry registry, Duration drainDeadline) {
		this.store = store;
		this.salt = salt;
		this.drainDeadline = drainDeadline;
		recorded = Counter.builder("urlshort.clicks.recorded").description("Clicks written to the click store")
				.register(registry);
		// every reason registered up front, so the families are scrapeable at zero (ADR-0016 amendment)
		for (String reason : List.of(REJECTED, REDUCTION_FAILED, WRITE_FAILED, SHUTDOWN_DEADLINE, OUTCOME_UNKNOWN)) {
			lostByReason.put(reason, Counter.builder("urlshort.clicks.lost")
					.description("Clicks reported by a click lost event").tag("reason", reason).register(registry));
		}
	}

	/**
	 * Hands the redirect just answered to the click writer; the redirect hook in
	 * {@code link.RedirectController}. Returns at once and never throws. A {@code HEAD} request is not
	 * a click (rule 1) and records nothing.
	 *
	 * @param linkId the id of the link the Visitor was redirected to
	 * @param request the redirect request; only its {@code Referer}, {@code User-Agent} and client are
	 *        read, on this thread. The client is the one the rate limiter charged
	 *        ({@link RateLimitFilter#CLIENT_ATTRIBUTE}, ADR-0015), else the peer address; it is hashed
	 *        and never stored
	 */
	public void record(long linkId, HttpServletRequest request) {
		if ("HEAD".equals(request.getMethod())) {
			return;
		}
		ClickWrite write;
		try {
			// the rate limiter's client (rule 6); the peer only where the limiter did not run
			String address = request.getAttribute(RateLimitFilter.CLIENT_ATTRIBUTE) instanceof String client
					? client : request.getRemoteAddr();
			DailySalt.Stamp stamp = salt.stamp(address);
			write = new ClickWrite(new Click(linkId, stamp.at(), LocalDate.ofInstant(stamp.at(), ZoneOffset.UTC),
					Click.referrerOrigin(request.getHeader("Referer")),
					Click.userAgentClass(request.getHeader("User-Agent")), stamp.clientHash()),
					MDC.get("requestId"));
		}
		catch (Exception ex) {
			lost(REDUCTION_FAILED, ex);
			return;
		}
		outstanding.add(write);
		try {
			writer.execute(write);
		}
		catch (RejectedExecutionException ex) {
			outstanding.remove(write);
			lost(REJECTED, ex);
		}
	}

	/** Waits until every click queued before the call is written or reported; for the functional suite. */
	void settle() throws Exception {
		writer.submit(() -> {
		}).get(10, TimeUnit.SECONDS);
	}

	/**
	 * Drains the queue for at most the drain deadline, then stops the writer and reports every click
	 * not yet written, so a normal process exit right after this returns loses no report.
	 */
	@PreDestroy
	void close() throws InterruptedException {
		writer.shutdown();
		if (!writer.awaitTermination(drainDeadline.toMillis(), TimeUnit.MILLISECONDS)) {
			for (Runnable pending : writer.shutdownNow()) {
				// a settle() waiting behind a stuck write must not wait for ever
				if (pending instanceof Future<?> future) {
					future.cancel(false);
				}
			}
			claimOutstanding();
		}
	}

	/** Shutdown takes over every unfinished click and reports it once. */
	void claimOutstanding() {
		for (ClickWrite write : outstanding) {
			if (write.state.compareAndSet(QUEUED, CLAIMED)) {
				write.reportAtShutdown(SHUTDOWN_DEADLINE);
			}
			else if (write.state.compareAndSet(RUNNING, CLAIMED)) {
				// the insert may still commit after this report, so its outcome is not claimed
				write.reportAtShutdown(OUTCOME_UNKNOWN);
			}
		}
	}

	/** The one {@code click lost} WARN and its one {@code urlshort.clicks.lost} increment, same reason. */
	private void lost(String reason, @Nullable Throwable error) {
		LoggingEventBuilder event = log.atWarn().setMessage("click lost").addKeyValue("reason", reason);
		if (error != null) {
			event = event.addKeyValue("errorType", error.getClass().getName());
		}
		event.log();
		lostByReason.get(reason).increment();
	}

	/** One queued click; whoever moves its state first owns its report. */
	final class ClickWrite implements Runnable {

		final Click click;
		final @Nullable String requestId;
		final AtomicInteger state = new AtomicInteger(QUEUED);

		ClickWrite(Click click, @Nullable String requestId) {
			this.click = click;
			this.requestId = requestId;
		}

		@Override
		public void run() {
			if (!state.compareAndSet(QUEUED, RUNNING)) {
				return;
			}
			MDC.put("requestId", requestId);
			try {
				store.insert(click);
				// counted whoever owns the report: a write shutdown already claimed can still commit
				recorded.increment();
				state.compareAndSet(RUNNING, DONE);
			}
			catch (RuntimeException ex) {
				if (state.compareAndSet(RUNNING, DONE)) {
					lost(WRITE_FAILED, ex);
				}
			}
			finally {
				outstanding.remove(this);
				MDC.remove("requestId");
			}
		}

		void reportAtShutdown(String reason) {
			try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
				lost(reason, null);
			}
		}
	}
}
