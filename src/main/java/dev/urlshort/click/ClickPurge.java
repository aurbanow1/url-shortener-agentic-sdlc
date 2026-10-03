package dev.urlshort.click;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Deletes clicks older than the retention period (NFR-P2, business rules 2, 3 and 6; ADR-0018): once
 * at startup, before readiness is reported, then once every UTC day at the first tick on or after
 * {@link #DAILY_AT} (00:10Z). Every run is one {@code DELETE} of the clicks stored on a UTC day
 * before the application clock's day minus the period, so that day itself is kept.
 *
 * <p>All runs share one daemon thread, {@code click-purge}, so they never overlap. A run writes one
 * INFO {@code clicks purged} with its count, cutoff and period, or one WARN {@code click purge failed}
 * with the exception's class only; neither carries a request id or a click value. A failed run is
 * retried by the next UTC day's run, never sooner. With the operator hold
 * ({@code urlshort.click.purge-enabled=false}) nothing runs and every start logs one WARN saying so.
 */
@Component
@EnableConfigurationProperties(ClickRetentionProperties.class)
class ClickPurge {

	/** The UTC time of day from which each day's run is due; recorded in {@code docs/DESIGN.md} §3 (AC-8). */
	static final LocalTime DAILY_AT = LocalTime.of(0, 10);
	static final Duration TICK = Duration.ofSeconds(5);
	static final Duration CLOSE_DEADLINE = Duration.ofSeconds(3);

	private static final Logger log = LoggerFactory.getLogger(ClickPurge.class);

	private final ClickStore store;
	private final Clock clock;
	private final int retentionDays;
	private final boolean purgeEnabled;
	private final ScheduledExecutorService purger = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, "click-purge");
		thread.setDaemon(true);
		return thread;
	});
	// read and written only on the purge thread (and by unit tests calling run/tick directly)
	private LocalDate lastRunDay = LocalDate.MIN;

	ClickPurge(ClickStore store, Clock clock, ClickRetentionProperties properties) {
		this.store = store;
		this.clock = clock;
		this.retentionDays = properties.retentionDays();
		this.purgeEnabled = properties.purgeEnabled();
	}

	/** The startup run, awaited so it ends before Boot reports readiness, then the daily tick (AC-7). */
	@EventListener(ApplicationReadyEvent.class)
	void start() throws InterruptedException, ExecutionException {
		if (!purgeEnabled) {
			log.atWarn().setMessage("click purge paused, no click is deleted")
					.addKeyValue("setting", "urlshort.click.purge-enabled").addKeyValue("retentionDays", retentionDays)
					.log();
			return;
		}
		purger.submit(this::run).get();
		purger.scheduleWithFixedDelay(this::tick, TICK.toMillis(), TICK.toMillis(), TimeUnit.MILLISECONDS);
	}

	/** The run the scheduler would start, on the same thread, whatever the hold; for the functional suite. */
	void runNow() throws Exception {
		purger.submit(this::run).get(10, TimeUnit.SECONDS);
	}

	/** Runs when the UTC day is later than the last run's and the time is at or past {@link #DAILY_AT}. */
	void tick() {
		Instant now = clock.instant();
		if (LocalDate.ofInstant(now, ZoneOffset.UTC).isAfter(lastRunDay)
				&& !LocalTime.ofInstant(now, ZoneOffset.UTC).isBefore(DAILY_AT)) {
			run();
		}
	}

	void run() {
		LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
		// set before the delete, so a failed run is not retried the same day (rule 3)
		lastRunDay = today;
		LocalDate cutoff = today.minusDays(retentionDays);
		try {
			// ponytail: one DELETE and a table scan per run; past ~2.5 M rows in one startup run,
			// add ix_click_day and id-range batches (ADR-0018, probe L6/L9)
			int deleted = store.deleteBefore(cutoff);
			log.atInfo().setMessage("clicks purged").addKeyValue("deleted", deleted)
					.addKeyValue("cutoff", cutoff.toString()).addKeyValue("retentionDays", retentionDays).log();
		}
		catch (RuntimeException ex) {
			// the class only: a driver message can quote stored values
			log.atWarn().setMessage("click purge failed").addKeyValue("cutoff", cutoff.toString())
					.addKeyValue("retentionDays", retentionDays).addKeyValue("errorType", ex.getClass().getName())
					.log();
		}
	}

	/** Takes no new run and waits up to {@link #CLOSE_DEADLINE} for one in progress; never interrupts JDBC. */
	@PreDestroy
	void close() throws InterruptedException {
		purger.shutdown();
		purger.awaitTermination(CLOSE_DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
	}
}
