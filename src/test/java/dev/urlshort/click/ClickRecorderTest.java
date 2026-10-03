package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The bounded single writer of ADR-0011: what is recorded, fail-open with exactly one WARN per lost
 * click, and the shutdown ownership of DR-01 (every unwritten click reported once, before
 * {@code close()} returns, never twice).
 */
class ClickRecorderTest {

	private static final Instant AT = Instant.parse("2026-10-01T12:00:00Z");
	private static final String HASH = "b".repeat(64);
	private static final String CANARY = "canary-store-message";

	private final ClickStore store = mock(ClickStore.class);
	private final DailySalt salt = mock(DailySalt.class);
	private final ClickRecorder recorder = new ClickRecorder(store, salt, Duration.ofMillis(200));
	private final ListAppender<ILoggingEvent> events = new ListAppender<>();
	private final Logger recorderLogger = (Logger) LoggerFactory.getLogger(ClickRecorder.class);

	@BeforeEach
	void setUp() throws Exception {
		when(salt.stamp(anyString())).thenReturn(new DailySalt.Stamp(AT, HASH));
		events.start();
		recorderLogger.addAppender(events);
	}

	@AfterEach
	void tearDown() throws Exception {
		recorderLogger.detachAppender(events);
		MDC.remove("requestId");
		recorder.close();
	}

	@Test
	void aHeadRequestIsNotAClick() throws Exception {
		recorder.record(1L, request("HEAD", "req-head"));
		recorder.settle();

		verify(store, never()).insert(any());
		verify(salt, never()).stamp(anyString());
	}

	@Test
	void aRedirectIsStoredAsItsReducedFactsOnly() throws Exception {
		MockHttpServletRequest request = request("GET", "req-get");
		request.addHeader("Referer", "https://News.Example/path?q=" + CANARY);
		request.addHeader("User-Agent", "Mozilla/5.0 " + CANARY);

		recorder.record(7L, request);
		recorder.settle();

		ArgumentCaptor<Click> stored = ArgumentCaptor.forClass(Click.class);
		verify(store).insert(stored.capture());
		verify(salt).stamp("203.0.113.77");
		assertThat(stored.getValue()).isEqualTo(
				new Click(7L, AT, LocalDate.parse("2026-10-01"), "https://news.example", "browser", HASH));
		assertThat(events.list).isEmpty();
	}

	@Test
	void aFailedWriteIsOneWarnWithTheRequestIdAndNoClickValue() throws Exception {
		doThrow(new DataAccessResourceFailureException("duplicate '" + HASH + "' " + CANARY)).when(store).insert(any());

		recorder.record(1L, request("GET", "req-fail"));
		recorder.settle();

		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getFormattedMessage()).isEqualTo("click lost");
			assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "req-fail");
			assertThat(keyValues(event)).containsEntry("reason", "write failed").containsEntry("errorType",
					DataAccessResourceFailureException.class.getName());
			assertThat(event.getThrowableProxy()).isNull();
			assertThat(event.toString() + keyValues(event)).doesNotContain(CANARY).doesNotContain(HASH);
		});
	}

	@Test
	void aClickThatCannotBeQueuedIsOneWarnAndTheRedirectGoesOn() throws Exception {
		recorder.close();

		assertThatCode(() -> recorder.record(1L, request("GET", "req-closed"))).doesNotThrowAnyException();

		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "req-closed");
			assertThat(keyValues(event)).containsEntry("reason", "rejected").containsEntry("errorType",
					"java.util.concurrent.RejectedExecutionException");
		});
		recorder.claimOutstanding();
		assertThat(events.list).as("a rejected click leaves nothing outstanding").hasSize(1);
	}

	@Test
	void aClickThatCannotBeReducedIsOneWarn() throws Exception {
		when(salt.stamp(anyString())).thenThrow(new GeneralSecurityException("no HMAC"));

		recorder.record(1L, request("GET", "req-salt"));

		assertThat(events.list).singleElement().satisfies(
				event -> assertThat(keyValues(event)).containsEntry("reason", "rejected").containsEntry("errorType",
						GeneralSecurityException.class.getName()));
		verify(store, never()).insert(any());
	}

	@Test
	void aFastStoreIsDrainedOnCloseAndNothingIsReported() throws Exception {
		for (int i = 0; i < 5; i++) {
			recorder.record(i, request("GET", "req-" + i));
		}

		recorder.close();

		verify(store, times(5)).insert(any());
		assertThat(events.list).isEmpty();
	}

	@Test
	void aStuckWriteIsBoundedAndEveryUnwrittenClickIsReportedOnceBeforeCloseReturns() throws Exception {
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger inserts = new AtomicInteger();
		doAnswer(call -> {
			if (inserts.incrementAndGet() == 1) {
				entered.countDown();
				awaitIgnoringInterrupts(release);
			}
			return null;
		}).when(store).insert(any());
		for (int i = 1; i <= 5; i++) {
			recorder.record(i, request("GET", "req-" + i));
		}
		entered.await();
		Thread settler = new Thread(() -> {
			try {
				recorder.settle();
			}
			catch (Exception expected) {
				// cancelled by close(): a settle() never outlives the drain
			}
		});
		settler.start();
		while (settler.getState() != Thread.State.TIMED_WAITING) {
			// parked in settle()'s timed get(): its task is queued behind the stuck write
			Thread.onSpinWait();
		}

		long started = System.nanoTime();
		recorder.close();
		long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
		List<ILoggingEvent> atClose = List.copyOf(events.list);
		recorder.claimOutstanding();
		release.countDown();
		settler.join(5_000);

		assertThat(elapsedMillis).isLessThan(2_000);
		assertThat(atClose).hasSize(5);
		Map<String, String> reasons = new HashMap<>();
		for (ILoggingEvent event : atClose) {
			reasons.put(event.getMDCPropertyMap().get("requestId"), (String) keyValues(event).get("reason"));
		}
		assertThat(reasons).containsEntry("req-1", "shutdown deadline, outcome unknown")
				.containsEntry("req-2", "shutdown deadline").containsEntry("req-3", "shutdown deadline")
				.containsEntry("req-4", "shutdown deadline").containsEntry("req-5", "shutdown deadline");
		assertThat(events.list).as("a second claim finds nothing new").hasSize(5);
		assertThat(settler.isAlive()).isFalse();
	}

	@Test
	void aClaimedClickIsNeverWrittenWhenTheWriterReachesItLater() throws Exception {
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger inserts = new AtomicInteger();
		doAnswer(call -> {
			if (inserts.incrementAndGet() == 1) {
				entered.countDown();
				release.await();
			}
			return null;
		}).when(store).insert(any());
		recorder.record(1L, request("GET", "req-running"));
		recorder.record(2L, request("GET", "req-queued"));
		entered.await();

		recorder.claimOutstanding();
		release.countDown();
		recorder.settle();

		assertThat(inserts.get()).as("the claimed, queued click is skipped by the writer").isEqualTo(1);
		assertThat(events.list).extracting(event -> event.getMDCPropertyMap().get("requestId"))
				.containsExactlyInAnyOrder("req-running", "req-queued");
	}

	@Test
	void aWriteThatFailsAfterShutdownClaimedItIsNotReportedAgain() throws Exception {
		doAnswer(call -> {
			recorder.claimOutstanding();
			throw new DataAccessResourceFailureException("late failure");
		}).when(store).insert(any());

		recorder.record(1L, request("GET", "req-late"));
		recorder.settle();

		assertThat(events.list).singleElement().satisfies(
				event -> assertThat(keyValues(event)).containsEntry("reason", "shutdown deadline, outcome unknown"));
	}

	@Test
	void aWriteThatCompletesAfterShutdownClaimedItIsNotReportedAgain() throws Exception {
		doAnswer(call -> {
			recorder.claimOutstanding();
			return null;
		}).when(store).insert(any());

		recorder.record(1L, request("GET", "req-late-ok"));
		recorder.settle();

		assertThat(events.list).singleElement().satisfies(
				event -> assertThat(keyValues(event)).containsEntry("reason", "shutdown deadline, outcome unknown"));
	}

	private static MockHttpServletRequest request(String method, String requestId) {
		MDC.put("requestId", requestId);
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/Abc12345");
		request.setRemoteAddr("203.0.113.77");
		return request;
	}

	private static void awaitIgnoringInterrupts(CountDownLatch latch) {
		while (true) {
			try {
				latch.await();
				return;
			}
			catch (InterruptedException ignored) {
				// an H2 lock wait ignores the interrupt too; that is the case under test
			}
		}
	}

	private static Map<String, Object> keyValues(ILoggingEvent event) {
		Map<String, Object> values = new HashMap<>();
		for (KeyValuePair pair : event.getKeyValuePairs()) {
			values.put(pair.key, pair.value);
		}
		return values;
	}
}
