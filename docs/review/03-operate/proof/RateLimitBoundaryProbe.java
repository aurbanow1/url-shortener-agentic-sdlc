package dev.urlshort.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Review-only reproduction against the unmodified candidate classes. */
public class RateLimitBoundaryProbe {
    static class ControlledClock extends Clock {
        volatile Instant current = Instant.parse("2026-10-03T12:00:00Z");
        final CountDownLatch sampled = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        @Override public Instant instant() {
            Instant value = current;
            if (Thread.currentThread().getName().equals("older-request")) {
                sampled.countDown();
                try {
                    if (!resume.await(5, TimeUnit.SECONDS)) throw new AssertionError("resume timeout");
                }
                catch (InterruptedException e) { throw new AssertionError(e); }
            }
            return value;
        }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    public static void main(String[] args) throws Exception {
        var clock = new ControlledClock();
        var limiter = new RateLimiter(new RateLimitProperties(60, 600, Set.of()), clock);
        var olderResult = new AtomicLong(-1);
        var older = new Thread(() -> olderResult.set(limiter.tryTake(RateLimiter.Budget.CREATE, "same-client")),
                "older-request");
        older.start();
        if (!clock.sampled.await(5, TimeUnit.SECONDS)) throw new AssertionError("sample timeout");
        // Wall time only advances. The older thread is paused after sampling t0, before bucket compute.
        clock.current = clock.current.plusMillis(1);
        int before = 0;
        for (int i = 0; i < 60; i++) if (limiter.tryTake(RateLimiter.Budget.CREATE, "same-client") == 0) before++;
        long exhausted = limiter.tryTake(RateLimiter.Budget.CREATE, "same-client");
        clock.resume.countDown();
        older.join(5_000);
        if (older.isAlive()) throw new AssertionError("older thread timeout");
        int after = 0;
        for (int i = 0; i < 60; i++) if (limiter.tryTake(RateLimiter.Budget.CREATE, "same-client") == 0) after++;
        int total = before + (olderResult.get() == 0 ? 1 : 0) + after;
        System.out.println("REORDER wallClockAdvance=1ms clockMovedBackward=false before=" + before
                + " exhaustedRetryAfter=" + exhausted + " olderRetryAfter=" + olderResult.get()
                + " laterAdmissions=" + after + " totalAdmissions=" + total + " expectedMaximum=60");

        // The separately disclosed rollback accommodation resets TATs, but not the cleanup deadline.
        var memoryClock = new ControlledClock();
        var memory = new RateLimiter(new RateLimitProperties(60, 600, Set.of()), memoryClock);
        memoryClock.current = memoryClock.current.plusSeconds(3600);
        memory.tryTake(RateLimiter.Budget.CREATE, "before-rollback");
        memoryClock.current = memoryClock.current.minusSeconds(3600);
        for (int i = 0; i < 10_000; i++) memory.tryTake(RateLimiter.Budget.CREATE, "old-" + i);
        memoryClock.current = memoryClock.current.plusSeconds(61);
        memory.tryTake(RateLimiter.Budget.CREATE, "new");
        System.out.println("ROLLBACK_MEMORY clientsAfter61SecondsAndRequest=" + memory.clients(RateLimiter.Budget.CREATE)
                + " oldFullClients=10000 cleanupDeadlineStillInFuture=true");
        if (total > 60) throw new AssertionError("Ordinary request reordering replenished the same client's bucket");
    }
}
