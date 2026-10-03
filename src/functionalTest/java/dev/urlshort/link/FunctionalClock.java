package dev.urlshort.link;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The service clock under suite control (AC-19, A-19): real time in millisecond ticks plus an
 * adjustable offset, so every other journey still sees "now" while the idempotency journey can move
 * past the 24 h window. {@link #freeze()} pins the real part, so the rate-limit journeys can step time
 * exactly; every test that freezes calls {@link #reset()} in {@code @AfterEach}.
 */
public class FunctionalClock extends Clock {

	private final Clock real = Clock.tickMillis(ZoneOffset.UTC);
	private volatile Duration offset = Duration.ZERO;
	private volatile Instant frozenAt;

	/** Moves the service clock forward by {@code by} on top of the current offset. */
	public void shift(Duration by) {
		offset = offset.plus(by);
	}

	/** Stops the real part at the current instant; only {@link #shift} moves the clock afterwards. */
	public void freeze() {
		frozenAt = real.instant();
	}

	/** Returns the service clock to real time, unfrozen. */
	public void reset() {
		offset = Duration.ZERO;
		frozenAt = null;
	}

	@Override
	public Instant instant() {
		Instant base = frozenAt;
		return (base != null ? base : real.instant()).plus(offset);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException("the service clock is UTC");
	}
}
