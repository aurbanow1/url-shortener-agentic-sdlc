package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * The purge's own rules (business rules 2, 3 and 6; ADR-0018): the cutoff, one run per UTC day from
 * 00:10Z, none on a clock that steps back, no retry of a failed run before the next day, one event per
 * run, the hold, and the startup run finishing before {@code start()} returns.
 */
class ClickPurgeTest {

	private static final LocalDate T = LocalDate.parse("2026-10-03");
	private static final String CANARY = "purgecanary";

	private final ClickStore store = mock(ClickStore.class);
	private final SettableClock clock = new SettableClock(at(T, "12:00:00"));
	private final ListAppender<ILoggingEvent> events = new ListAppender<>();
	private final Logger purgeLogger = (Logger) LoggerFactory.getLogger(ClickPurge.class);
	private ClickPurge purge = new ClickPurge(store, clock, new ClickRetentionProperties(90, true));

	@BeforeEach
	void setUp() {
		events.start();
		purgeLogger.addAppender(events);
	}

	@AfterEach
	void tearDown() throws Exception {
		purgeLogger.detachAppender(events);
		purge.close();
	}

	@Test
	void aRunDeletesBeforeTheEarliestKeptDayAndLogsOneInfo() {
		when(store.deleteBefore(any())).thenReturn(7);

		purge.run();

		verify(store).deleteBefore(T.minusDays(90));
		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.INFO);
			assertThat(event.getFormattedMessage()).isEqualTo("clicks purged");
			assertThat(keyValues(event)).containsExactlyInAnyOrderEntriesOf(
					Map.of("deleted", 7, "cutoff", "2026-07-05", "retentionDays", 90));
		});
	}

	@Test
	void theCutoffFollowsThePeriod() {
		purge = new ClickPurge(store, clock, new ClickRetentionProperties(7, true));

		purge.run();

		verify(store).deleteBefore(T.minusDays(7));
		assertThat(keyValues(events.list.getFirst())).containsEntry("retentionDays", 7);
	}

	@Test
	void theTickRunsOnceADayFromTenPastMidnight() {
		purge.run();
		clock.set(at(T.plusDays(1), "00:09:59"));
		purge.tick();
		verify(store, times(1)).deleteBefore(any());

		clock.set(at(T.plusDays(1), "00:10:00"));
		purge.tick();
		verify(store).deleteBefore(T.plusDays(1).minusDays(90));

		clock.set(at(T.plusDays(1), "18:00:00"));
		purge.tick();
		verify(store, times(2)).deleteBefore(any());
	}

	@Test
	void aClockThatStepsBackRunsNothing() {
		clock.set(at(T.plusDays(1), "00:10:00"));
		purge.tick();
		reset(store);

		clock.set(at(T.minusDays(2), "12:00:00"));
		purge.tick();

		verifyNoInteractions(store);
	}

	@Test
	void aFailedRunIsOneWarnWithTheClassOnlyAndIsNotRetriedBeforeTheNextDay() {
		when(store.deleteBefore(any())).thenThrow(new DataAccessResourceFailureException("store down " + CANARY));

		purge.run();
		clock.set(at(T, "12:00:05"));
		purge.tick();

		verify(store, times(1)).deleteBefore(any());
		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage()).isEqualTo("click purge failed");
			assertThat(event.getThrowableProxy()).isNull();
			assertThat(keyValues(event)).containsExactlyInAnyOrderEntriesOf(Map.of("cutoff", "2026-07-05",
					"retentionDays", 90, "errorType", DataAccessResourceFailureException.class.getName()));
			assertThat(event.toString()).doesNotContain(CANARY);
		});

		clock.set(at(T.plusDays(1), "00:10:00"));
		purge.tick();
		verify(store, times(2)).deleteBefore(any());
	}

	@Test
	void startRunsOnThePurgeThreadAndReturnsAfterTheRun() throws Exception {
		AtomicReference<String> thread = new AtomicReference<>();
		doAnswer(call -> {
			thread.set(Thread.currentThread().getName());
			return 0;
		}).when(store).deleteBefore(any());

		purge.start();

		assertThat(thread.get()).isEqualTo("click-purge");
		assertThat(events.list).singleElement()
				.satisfies(event -> assertThat(event.getFormattedMessage()).isEqualTo("clicks purged"));
	}

	@Test
	void runNowRunsOnThePurgeThread() throws Exception {
		AtomicReference<String> thread = new AtomicReference<>();
		doAnswer(call -> {
			thread.set(Thread.currentThread().getName());
			return 3;
		}).when(store).deleteBefore(any());

		purge.runNow();

		assertThat(thread.get()).isEqualTo("click-purge");
	}

	@Test
	void onHoldStartDeletesNothingAndSaysSoOnce() throws Exception {
		purge = new ClickPurge(store, clock, new ClickRetentionProperties(90, false));

		purge.start();

		verify(store, never()).deleteBefore(any());
		assertThat(events.list).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage()).isEqualTo("click purge paused, no click is deleted");
			assertThat(keyValues(event)).containsExactlyInAnyOrderEntriesOf(
					Map.of("setting", "urlshort.click.purge-enabled", "retentionDays", 90));
		});
	}

	private static Map<String, Object> keyValues(ILoggingEvent event) {
		Map<String, Object> values = new HashMap<>();
		if (event.getKeyValuePairs() != null) {
			for (KeyValuePair pair : event.getKeyValuePairs()) {
				values.put(pair.key, pair.value);
			}
		}
		return values;
	}

	private static Instant at(LocalDate day, String time) {
		return Instant.parse(day + "T" + time + "Z");
	}

	/** A clock the test sets explicitly. */
	private static final class SettableClock extends Clock {
		private volatile Instant now;

		SettableClock(Instant start) {
			now = start;
		}

		void set(Instant instant) {
			now = instant;
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
