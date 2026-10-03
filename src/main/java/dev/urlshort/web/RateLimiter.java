package dev.urlshort.web;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Component;

/**
 * Per-client token buckets for the two budgets (business rules 2 to 4, 8; ADR-0014). Each bucket is
 * stored in its GCRA form, one theoretical arrival time (TAT) per client: with {@code N} tokens per
 * minute the emission interval is {@code I = 60 s / N} and the tolerance {@code T = I × (N − 1)}, so a
 * bucket holds {@code N} tokens, refills {@code N / 60} per second, and is full again exactly when
 * {@code TAT ≤ now}.
 *
 * <p>State is in memory, per instance, and bounded: at most once per second of application-clock
 * time, a call releases every client whose bucket is full again, so after any such call only clients
 * admitted in the previous 61 s remain. While no limited request arrives nothing is added or
 * removed. Thread-safe: each bucket changes under {@link ConcurrentHashMap#compute}, which also reads
 * the clock, so a request never decides on a time older than the TAT it sees; the release removes an
 * entry only while it still holds the value it tested.
 *
 * <p>The application clock is the wall clock. A backward step is outside the contract and fails closed:
 * a client's bucket refills only from its stored TAT, so a recently active client waits up to the length
 * of the step. The release is rescheduled at the next request after such a step.
 */
@Component
class RateLimiter {

	private static final long SECOND = 1_000_000_000L;

	private final Clock clock;
	private final Map<Budget, Buckets> buckets = new EnumMap<>(Budget.class);
	private final AtomicLong nextSweep = new AtomicLong(Long.MIN_VALUE);

	RateLimiter(RateLimitProperties properties, Clock clock) {
		this.clock = clock;
		buckets.put(Budget.CREATE, new Buckets(properties.createPerMinute()));
		buckets.put(Budget.REDIRECT, new Buckets(properties.redirectPerMinute()));
	}

	/**
	 * Takes one token from the client's bucket in {@code budget}.
	 *
	 * @return {@code 0} when admitted; otherwise the {@code Retry-After} in whole seconds, at least
	 *         {@code 1}: the time until the bucket holds a token again, rounded up. A refusal takes
	 *         nothing.
	 */
	long tryTake(Budget budget, String client) {
		releaseFullBuckets(now());
		return buckets.get(budget).tryTake(client, this::now);
	}

	private long now() {
		Instant instant = clock.instant();
		return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), SECOND), instant.getNano());
	}

	/** The number of clients currently held for {@code budget}; for the memory-bound tests. */
	int clients(Budget budget) {
		return buckets.get(budget).tats.size();
	}

	private void releaseFullBuckets(long now) {
		// due once a second; a deadline more than a second ahead can only follow a backward clock step
		LongPredicate due = next -> now >= next || next - now > SECOND;
		if (due.test(nextSweep.getAndUpdate(next -> due.test(next) ? now + SECOND : next))) {
			buckets.values().forEach(b -> b.tats.values().removeIf(tat -> tat <= now));
		}
	}

	/** The two budgets of business rule 1; the lower-case name is the counter's {@code budget} tag. */
	enum Budget {
		CREATE, REDIRECT;

		String tag() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final class Buckets {

		private final long interval;
		private final long tolerance;
		private final ConcurrentHashMap<String, Long> tats = new ConcurrentHashMap<>();

		Buckets(int perMinute) {
			interval = 60 * SECOND / perMinute;
			tolerance = interval * (perMinute - 1);
		}

		long tryTake(String client, LongSupplier clock) {
			long[] retryAfter = new long[1];
			tats.compute(client, (key, tat) -> {
				long now = clock.getAsLong();
				long start = tat == null ? now : Math.max(tat, now);
				long wait = start - tolerance - now;
				if (wait > 0) {
					retryAfter[0] = Math.ceilDiv(wait, SECOND);
					return tat;
				}
				return start + interval;
			});
			return retryAfter[0];
		}
	}
}
