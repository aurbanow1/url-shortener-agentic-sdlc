package dev.urlshort.link;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The service clock under suite control (AC-19, A-19): real time in millisecond ticks plus an
 * adjustable offset, so every other journey still sees "now" while the idempotency journey can move
 * past the 24 h window.
 */
public class FunctionalClock extends Clock {

	private final Clock real = Clock.tickMillis(ZoneOffset.UTC);
	private volatile Duration offset = Duration.ZERO;

	/** Moves the service clock forward by {@code by} on top of the current offset. */
	public void shift(Duration by) {
		offset = offset.plus(by);
	}

	/** Returns the service clock to real time. */
	public void reset() {
		offset = Duration.ZERO;
	}

	@Override
	public Instant instant() {
		return real.instant().plus(offset);
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
