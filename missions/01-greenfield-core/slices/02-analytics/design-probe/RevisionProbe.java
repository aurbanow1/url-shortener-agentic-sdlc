import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Revision probe for 02-analytics design review DR-01 and DR-02 (design.md section 12). Pure Java, no
 * Spring: it runs the revised mechanisms exactly as design.md section 1 specifies them.
 * DR-01: a recorder whose close() drains for at most a deadline, then cancels what is still queued and
 * accounts for every click once (written, or one "lost" record with its request id).
 * DR-02: a daily salt that chooses the instant and the key in one locked step, replayed in the
 * reviewer's interleaving (a request that selected day D finishes after day D+1 began) and with a
 * stale expiry callback. No file under src/ is touched.
 */
public class RevisionProbe {

	public static void main(String[] args) throws Exception {
		drain(Duration.ofSeconds(5), 6, 2000);
		drain(Duration.ofMillis(500), 6, 2000);
		drain(Duration.ofSeconds(5), 6, 0);
		salt();
	}

	// ---------------------------------------------------------------- DR-01

	record Lost(String requestId, String reason) {
	}

	/** A queued write that carries its request id, so a cancelled one can still be accounted for. */
	record ClickWrite(String requestId, Recorder recorder) implements Runnable {
		@Override
		public void run() {
			recorder.write(this);
		}
	}

	static final class Recorder {
		final long writeMillis;
		final Duration deadline;
		final List<String> written = java.util.Collections.synchronizedList(new ArrayList<>());
		final List<Lost> lost = java.util.Collections.synchronizedList(new ArrayList<>());
		final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10_000),
				r -> {
					Thread t = new Thread(r, "click-writer");
					t.setDaemon(true);
					return t;
				}, new ThreadPoolExecutor.AbortPolicy());

		Recorder(long writeMillis, Duration deadline) {
			this.writeMillis = writeMillis;
			this.deadline = deadline;
		}

		void record(String requestId) {
			writer.execute(new ClickWrite(requestId, this));
		}

		void write(ClickWrite w) {
			try {
				if (writeMillis > 0) {
					Thread.sleep(writeMillis); // the slow store
				}
				written.add(w.requestId());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				lost.add(new Lost(w.requestId(), "write failed (interrupted at the deadline)"));
			}
		}

		/** design.md section 1: drain for at most the deadline, then cancel and account for the rest. */
		void close() throws InterruptedException {
			writer.shutdown();
			if (writer.awaitTermination(deadline.toMillis(), TimeUnit.MILLISECONDS)) {
				return;
			}
			for (Runnable r : writer.shutdownNow()) {
				if (r instanceof ClickWrite w) {
					lost.add(new Lost(w.requestId(), "shutdown deadline"));
				}
			}
		}
	}

	static void drain(Duration deadline, int queued, long writeMillis) throws Exception {
		Recorder recorder = new Recorder(writeMillis, deadline);
		for (int i = 1; i <= queued; i++) {
			recorder.record("req-" + i);
		}
		long t0 = System.nanoTime();
		recorder.close();
		long closeMs = (System.nanoTime() - t0) / 1_000_000;
		Thread.sleep(50); // let an interrupted in-flight write record its loss
		List<String> accounted = new ArrayList<>(recorder.written);
		recorder.lost.forEach(l -> accounted.add(l.requestId()));
		out("DR-01 close() with deadline " + deadline.toMillis() + " ms, " + queued + " queued writes of " + writeMillis + " ms",
				"close returned after " + closeMs + " ms; written=" + recorder.written + "; lost=" + recorder.lost
						+ "\n    VERDICT boundedByDeadline=" + (closeMs <= deadline.toMillis() + 200) + " everyClickAccountedOnce="
						+ (accounted.size() == queued && accounted.stream().distinct().count() == queued));
	}

	// ---------------------------------------------------------------- DR-02

	static final class MutableClock extends Clock {
		volatile Instant now;

		MutableClock(Instant now) {
			this.now = now;
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

	/** The instant, its day and a copy of that day's key, chosen together under the salt's lock. */
	record Selection(Instant at, LocalDate day, SecretKeySpec key) {
	}

	static final class DailySalt {
		final Clock clock;
		final SecureRandom random = new SecureRandom();
		final AtomicInteger expiries = new AtomicInteger();
		LocalDate day;
		byte[] salt;

		DailySalt(Clock clock) {
			this.clock = clock;
		}

		synchronized Selection select() {
			Instant at = clock.instant();
			LocalDate today = LocalDate.ofInstant(at, ZoneOffset.UTC);
			if (!today.equals(day)) {
				discard();
				day = today;
				salt = new byte[32];
				random.nextBytes(salt);
				// production: CompletableFuture.delayedExecutor(until the day's end) runs expire(today)
			}
			return new Selection(at, day, new SecretKeySpec(salt, "HmacSHA256"));
		}

		static String hmac(Selection s, String address) throws Exception {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(s.key());
			return HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
		}

		String stamp(String address) throws Exception {
			return hmac(select(), address);
		}

		synchronized void expire(LocalDate ended) {
			expiries.incrementAndGet();
			if (ended.equals(day)) {
				discard();
			}
		}

		private void discard() {
			if (salt != null) {
				Arrays.fill(salt, (byte) 0);
			}
			salt = null;
			day = null;
		}
	}

	static void salt() throws Exception {
		String a = "203.0.113.77";
		MutableClock clock = new MutableClock(Instant.parse("2026-10-01T23:59:59.999Z"));
		DailySalt salt = new DailySalt(clock);

		String dayDReference = salt.stamp(a);
		Selection delayed = salt.select(); // request A: instant and key of day D chosen, then A "pauses"
		clock.now = Instant.parse("2026-10-02T00:00:00.000Z");
		String b1 = salt.stamp(a); // request B: day D+1 begins, the salt rotates
		String delayedHash = DailySalt.hmac(delayed, a); // A resumes after the rotation
		String b2 = salt.stamp(a);
		out("DR-02 reviewer's interleaving: A selects day D, B starts day D+1, A finishes, B again",
				"A's click: day " + delayed.day() + " at " + delayed.at() + "\n    VERDICT delayedHashEqualsDayDHash=" + delayedHash.equals(dayDReference)
						+ " sameDayHashStableOnD+1=" + b1.equals(b2) + " D+1DiffersFromD=" + !b1.equals(dayDReference));

		salt.expire(LocalDate.parse("2026-10-01")); // a stale callback for day D, firing late
		String b3 = salt.stamp(a);
		out("DR-02 stale expiry callback for day D after D+1 began", "VERDICT D+1HashUnchanged=" + b1.equals(b3));

		salt.expire(LocalDate.parse("2026-10-02")); // the real end of day D+1 (clock not yet moved)
		String b4 = salt.stamp(a);
		out("DR-02 expiry of the current day drops its salt", "VERDICT nextStampDrawsANewSalt=" + !b4.equals(b1));

		clock.now = Instant.parse("2026-10-01T12:00:00Z"); // the clock itself goes back across midnight
		String back = salt.stamp(a);
		out("DR-02 clock moved back across midnight (NTP step or a test shift)",
				"VERDICT newSaltForTheEarlierDay=" + (!back.equals(dayDReference) && !back.equals(b4)) + " (documented boundary)");

		MutableClock fast = new MutableClock(Instant.parse("2026-10-01T23:59:59.500Z"));
		DailySalt live = new DailySalt(fast);
		Selection s = live.select();
		long untilEnd = Duration.between(s.at(), s.day().plusDays(1).atStartOfDay(ZoneOffset.UTC)).toMillis();
		CompletableFuture.runAsync(() -> live.expire(s.day()), CompletableFuture.delayedExecutor(untilEnd, TimeUnit.MILLISECONDS));
		Thread.sleep(900);
		out("DR-02 scheduled expiry at the day's end", "VERDICT expiredWithoutAClick=" + (live.salt == null) + " expiries=" + live.expiries);
	}

	static void out(String title, String text) {
		System.out.println("PROBE " + title + "\n    " + text);
	}
}
