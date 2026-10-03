package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import dev.urlshort.web.RateLimiter.Budget;
import org.junit.jupiter.api.Test;

/**
 * The GCRA buckets of business rules 2 to 4 replayed on a hand-stepped clock (AC-3, AC-4), and the
 * memory bound: full buckets are released at most once per second, and only when a request runs.
 */
class RateLimiterTest {

	private final SteppedClock clock = new SteppedClock(Instant.parse("2026-10-03T12:00:00Z"));
	private final RateLimiter limiter = new RateLimiter(new RateLimitProperties(60, 600, Set.of()), clock);

	@Test
	void AC03a_anExactlyEmptyBucketRefillsOneTokenAfterExactlyOneSecond() {
		take(Budget.CREATE, "c", 60);

		assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
		clock.step(Duration.ofMillis(999));
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
		clock.step(Duration.ofMillis(1));
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isZero();
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
	}

	@Test
	void AC03b_retryAfterIsRoundedUpAndHonoured() {
		take(Budget.CREATE, "c", 60);
		clock.step(Duration.ofMillis(250));

		long retryAfter = limiter.tryTake(Budget.CREATE, "c");
		clock.step(Duration.ofSeconds(retryAfter));

		assertThat(retryAfter).isEqualTo(1);
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isZero();
	}

	@Test
	void AC04_aQuietMinuteRefillsTheWholeBucket() {
		take(Budget.CREATE, "c", 60);
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isPositive();

		clock.step(Duration.ofSeconds(60));

		take(Budget.CREATE, "c", 60);
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isPositive();
	}

	@Test
	void refusalsTakeNothing() {
		take(Budget.CREATE, "c", 60);
		for (int i = 0; i < 100; i++) {
			assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
		}

		clock.step(Duration.ofSeconds(1));

		assertThat(limiter.tryTake(Budget.CREATE, "c")).isZero();
	}

	@Test
	void budgetsAndClientsAreIndependent() {
		take(Budget.CREATE, "a", 60);
		take(Budget.REDIRECT, "b", 600);

		assertThat(limiter.tryTake(Budget.CREATE, "a")).isPositive();
		assertThat(limiter.tryTake(Budget.REDIRECT, "b")).isPositive();
		assertThat(limiter.tryTake(Budget.REDIRECT, "a")).isZero();
		assertThat(limiter.tryTake(Budget.CREATE, "b")).isZero();
	}

	@Test
	void retryAfterIsTheWaitForOneTokenInWholeSeconds() {
		RateLimiter slow = new RateLimiter(new RateLimitProperties(2, 3, Set.of()), clock);
		slow.tryTake(Budget.CREATE, "c");
		slow.tryTake(Budget.CREATE, "c");

		assertThat(slow.tryTake(Budget.CREATE, "c")).as("2 per minute: one token every 30 s").isEqualTo(30);
		clock.step(Duration.ofMillis(29_500));
		assertThat(slow.tryTake(Budget.CREATE, "c")).as("0.5 s left rounds up").isEqualTo(1);
	}

	@Test
	void aRequestOvertakenByNewerOnesDecidesOnTheTimeItReachesTheBucket() {
		// the overtaken request is paused at its first clock read; meanwhile, 1 ms later, 60 newer
		// requests empty the same bucket
		clock.onFirstRead(() -> {
			clock.step(Duration.ofMillis(1));
			take(Budget.CREATE, "c", 60);
			assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
		});

		assertThat(limiter.tryTake(Budget.CREATE, "c")).as("61st admission in 1 ms").isEqualTo(1);
		clock.step(Duration.ofSeconds(1));
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isZero();
		assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(1);
	}

	@Test
	void afterABackwardClockStepTheBucketRefillsFromItsStoredTat() {
		take(Budget.CREATE, "c", 60);

		clock.step(Duration.ofMinutes(-1));

		assertThat(limiter.tryTake(Budget.CREATE, "c")).isEqualTo(61);
	}

	@Test
	void fullBucketsAreReleasedByTheNextRequestButNotWhileIdle() {
		for (int i = 0; i < 10_000; i++) {
			limiter.tryTake(Budget.CREATE, "client-" + i);
		}
		clock.step(Duration.ofSeconds(61));

		assertThat(limiter.clients(Budget.CREATE)).as("idle: nothing runs, nothing is released").isEqualTo(10_000);

		limiter.tryTake(Budget.CREATE, "next");

		assertThat(limiter.clients(Budget.CREATE)).isEqualTo(1);
	}

	@Test
	void aBucketThatIsNotYetFullSurvivesTheRelease() {
		limiter.tryTake(Budget.CREATE, "busy");
		limiter.tryTake(Budget.CREATE, "busy");
		clock.step(Duration.ofMillis(1_500));

		limiter.tryTake(Budget.CREATE, "other");

		assertThat(limiter.clients(Budget.CREATE)).isEqualTo(2);
	}

	@Test
	void theReleaseRunsAtMostOncePerSecond() {
		limiter.tryTake(Budget.REDIRECT, "x");
		clock.step(Duration.ofMillis(500));
		limiter.tryTake(Budget.REDIRECT, "y");
		assertThat(limiter.clients(Budget.REDIRECT)).as("x is full again, but no release within the second")
				.isEqualTo(2);

		clock.step(Duration.ofMillis(500));
		limiter.tryTake(Budget.REDIRECT, "z");

		assertThat(limiter.clients(Budget.REDIRECT)).isEqualTo(1);
	}

	@Test
	void theReleaseResumesAfterABackwardClockStep() {
		clock.step(Duration.ofHours(1));
		limiter.tryTake(Budget.CREATE, "before-the-step");
		clock.step(Duration.ofHours(-1));
		for (int i = 0; i < 10_000; i++) {
			limiter.tryTake(Budget.CREATE, "client-" + i);
		}
		clock.step(Duration.ofSeconds(61));

		limiter.tryTake(Budget.CREATE, "next");

		assertThat(limiter.clients(Budget.CREATE)).as("only the bucket stamped before the step is not yet full")
				.isEqualTo(2);
	}

	private void take(Budget budget, String client, int n) {
		for (int i = 0; i < n; i++) {
			assertThat(limiter.tryTake(budget, client)).as("token %d", i + 1).isZero();
		}
	}

	/** A clock that moves only when told, and can run one action on its next read. */
	private static final class SteppedClock extends Clock {
		private Instant now;
		private Runnable onRead;

		SteppedClock(Instant start) {
			now = start;
		}

		void step(Duration by) {
			now = now.plus(by);
		}

		void onFirstRead(Runnable action) {
			onRead = action;
		}

		@Override
		public Instant instant() {
			Instant read = now;
			Runnable action = onRead;
			onRead = null;
			if (action != null) {
				action.run();
			}
			return read;
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
