package dev.urlshort.click;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

/** Review-only checks against the candidate class; never prints the secret or its hash. */
public class SaltLifetimeProbe {
    private static final Field SALT;
    private static final Field DAY;
    static {
        try {
            SALT = DailySalt.class.getDeclaredField("salt");
            DAY = DailySalt.class.getDeclaredField("day");
            SALT.setAccessible(true);
            DAY.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        DailySalt quiet = new DailySalt(Clock.fixed(Instant.parse("2026-10-01T23:59:59.800Z"), ZoneOffset.UTC), new SecureRandom());
        quiet.select();
        byte[] original = bytes(quiet);
        check(original != null && original.length == 32, "32-byte managed salt");
        long deadline = System.nanoTime() + 2_000_000_000L;
        // No stamp, select, expire or close call during this wait: disposal must be scheduled.
        while (bytes(quiet) != null && System.nanoTime() < deadline) Thread.sleep(5);
        check(bytes(quiet) == null && day(quiet) == null, "quiet expiry drops both fields");
        check(Arrays.equals(original, new byte[32]), "quiet expiry zeros the former array");
        System.out.println("PASS: scheduled expiry with no further click nulls salt/day and zeros all 32 bytes");

        DailySalt closing = new DailySalt(Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC), new SecureRandom());
        DailySalt.Selection copied = closing.select();
        byte[] held = bytes(closing);
        closing.expire(LocalDate.parse("2026-09-30"));
        check(bytes(closing) == held, "stale expiry leaves the current salt alone");
        closing.close();
        check(bytes(closing) == null && day(closing) == null, "close drops fields");
        check(Arrays.equals(held, new byte[32]), "close zeros the former array");
        closing.close();
        // The transient request key copy is intentional: in-flight stamps may finish after expiry.
        check(copied.key().getEncoded().length == 32, "transient request key copy remains usable");
        System.out.println("PASS: close zeros and drops managed salt/day; close is idempotent; stale callback is harmless");
        System.out.println("LIMIT: in-flight SecretKeySpec copies are ordinary JVM objects, not claimed to be securely wiped");
    }

    private static byte[] bytes(DailySalt target) throws Exception {
        synchronized (target) { return (byte[]) SALT.get(target); }
    }
    private static Object day(DailySalt target) throws Exception {
        synchronized (target) { return DAY.get(target); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
