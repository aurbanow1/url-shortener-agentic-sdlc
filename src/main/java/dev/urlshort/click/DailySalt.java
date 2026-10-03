package dev.urlshort.click;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The client hash of business rule 4 (ADR-0012): HMAC-SHA256 of the address under a 32-byte random
 * salt that belongs to one UTC day. The salt lives only in this object, is never logged, returned or
 * stored, and is zeroed and dropped when its day ends, by a scheduled expiry even if no click follows.
 * A restart draws a new salt.
 *
 * <p>The click's instant and its key are chosen in one locked step, so a request can never carry an
 * older day into the lock after a newer day has rotated the salt (DR-02). A clock that itself moves
 * back across midnight draws a fresh salt for the earlier day.
 */
@Component
class DailySalt {

	private final Clock clock;
	private final SecureRandom random;
	private @Nullable LocalDate day;
	private byte @Nullable [] salt;

	DailySalt(Clock clock, SecureRandom random) {
		this.clock = clock;
		this.random = random;
	}

	/** The click's instant and the address's hash under that instant's day salt. */
	Stamp stamp(String address) throws GeneralSecurityException {
		Selection selection = select();
		return new Stamp(selection.at(), hmac(selection.key(), address));
	}

	/** Reads the clock, rotates the salt if the day changed, and copies the key, under one lock. */
	synchronized Selection select() {
		Instant at = clock.instant();
		LocalDate today = LocalDate.ofInstant(at, ZoneOffset.UTC);
		if (!today.equals(day)) {
			drop();
			byte[] fresh = new byte[32];
			random.nextBytes(fresh);
			salt = fresh;
			day = today;
			Duration untilDayEnd = Duration.between(at, today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
			CompletableFuture.delayedExecutor(untilDayEnd.toMillis(), TimeUnit.MILLISECONDS).execute(() -> expire(today));
		}
		return new Selection(at, new SecretKeySpec(salt, "HmacSHA256"));
	}

	/** Drops the salt if it still belongs to {@code expiring}; a stale expiry is a no-op. */
	synchronized void expire(LocalDate expiring) {
		if (expiring.equals(day)) {
			drop();
		}
	}

	@PreDestroy
	synchronized void close() {
		drop();
	}

	private void drop() {
		if (salt != null) {
			Arrays.fill(salt, (byte) 0);
		}
		salt = null;
		day = null;
	}

	static String hmac(SecretKeySpec key, String address) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(key);
		return HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
	}

	/** The click's instant and the client hash. */
	record Stamp(Instant at, String clientHash) {
	}

	/** One locked selection: the instant and a copy of that day's key. */
	record Selection(Instant at, SecretKeySpec key) {
	}
}
