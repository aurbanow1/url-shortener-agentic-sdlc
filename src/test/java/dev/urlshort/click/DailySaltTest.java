package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * Business rule 4 and ADR-0012: an HMAC of the address under a salt per UTC day, the instant and the
 * key chosen in one locked step (DR-02), and a salt that never outlives its day.
 */
class DailySaltTest {

	private static final String ADDRESS = "203.0.113.77";

	private final SettableClock clock = new SettableClock(Instant.parse("2026-10-01T12:00:00Z"));
	private final DailySalt salt = new DailySalt(clock, new SecureRandom());

	@Test
	void sameAddressAndDayHashEquallyAndTheStampCarriesTheClocksInstant() throws Exception {
		DailySalt.Stamp first = salt.stamp(ADDRESS);
		clock.set(Instant.parse("2026-10-01T23:59:59Z"));
		DailySalt.Stamp second = salt.stamp(ADDRESS);

		assertThat(first.at()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
		assertThat(second.at()).isEqualTo(Instant.parse("2026-10-01T23:59:59Z"));
		assertThat(second.clientHash()).isEqualTo(first.clientHash()).matches("[0-9a-f]{64}");
	}

	@Test
	void anotherAddressOrAnotherDayHashesDifferently() throws Exception {
		String day1 = salt.stamp(ADDRESS).clientHash();
		String other = salt.stamp("198.51.100.23").clientHash();
		clock.set(Instant.parse("2026-10-02T00:00:00Z"));
		String day2 = salt.stamp(ADDRESS).clientHash();

		assertThat(other).isNotEqualTo(day1);
		assertThat(day2).isNotEqualTo(day1);
	}

	@Test
	void theHashIsNeverTheUnsaltedDigest() throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(ADDRESS.getBytes(StandardCharsets.UTF_8));

		String hash = salt.stamp(ADDRESS).clientHash();

		assertThat(hash).isNotEqualTo(HexFormat.of().formatHex(digest))
				.isNotEqualTo(Base64.getEncoder().encodeToString(digest));
	}

	@Test
	void aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt() throws Exception {
		// draw day D's salt at noon, so its real expiry timer is hours away; drawn at 23:59:59.999 the timer
		// would fire 1 ms later and could drop D's key between the selections below (CR-01)
		clock.set(Instant.parse("2026-10-01T12:00:00Z"));
		salt.stamp(ADDRESS);
		clock.set(Instant.parse("2026-10-01T23:59:59.999Z"));
		String dayD = salt.stamp(ADDRESS).clientHash();
		clock.set(Instant.parse("2026-10-01T23:59:59.999Z"));
		DailySalt.Selection delayed = salt.select();
		clock.set(Instant.parse("2026-10-02T00:00:00Z"));
		String nextDay = salt.stamp(ADDRESS).clientHash();

		String delayedHash = DailySalt.hmac(delayed.key(), ADDRESS);
		String nextDayAgain = salt.stamp(ADDRESS).clientHash();

		assertThat(delayedHash).as("the delayed request finishes with day D's key").isEqualTo(dayD);
		assertThat(nextDayAgain).as("day D+1's salt was not replaced").isEqualTo(nextDay);
		assertThat(nextDay).isNotEqualTo(dayD);
	}

	@Test
	void aStaleExpiryIsANoOpAndTheCurrentDaysExpiryDropsTheSalt() throws Exception {
		clock.set(Instant.parse("2026-10-02T08:00:00Z"));
		String before = salt.stamp(ADDRESS).clientHash();

		salt.expire(LocalDate.parse("2026-10-01"));
		assertThat(salt.stamp(ADDRESS).clientHash()).isEqualTo(before);

		salt.expire(LocalDate.parse("2026-10-02"));
		assertThat(salt.stamp(ADDRESS).clientHash()).as("a new salt was drawn").isNotEqualTo(before);
	}

	@Test
	void closeDropsTheSalt() throws Exception {
		String before = salt.stamp(ADDRESS).clientHash();

		salt.close();
		salt.close();

		assertThat(salt.stamp(ADDRESS).clientHash()).isNotEqualTo(before);
	}

	@Test
	void aSaltIsDroppedAtTheEndOfItsDayWithoutAnyFurtherClick() throws Exception {
		clock.set(Instant.parse("2026-10-01T23:59:59.900Z"));
		String before = salt.stamp(ADDRESS).clientHash();

		long deadline = System.nanoTime() + 2_000_000_000L;
		String after = before;
		while (after.equals(before) && System.nanoTime() < deadline) {
			Thread.onSpinWait();
			// the clock stays at 23:59:59.900, so only the scheduled expiry can make the next stamp differ
			after = salt.stamp(ADDRESS).clientHash();
		}

		assertThat(after).isNotEqualTo(before);
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
