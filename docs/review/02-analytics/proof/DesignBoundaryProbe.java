import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.mock.web.MockHttpServletRequest;

/** Independent boundary checks against the unchanged producer probe classes at 71b2e10. */
public class DesignBoundaryProbe {
    public static void main(String[] args) throws Exception {
        Instant before = Instant.parse("2026-10-01T23:59:59.999Z");
        Instant after = Instant.parse("2026-10-02T00:00:00.001Z");
        // Replay a valid concurrent ordering: A captured D and paused before hash;
        // B hashes D+1; A resumes; C hashes D+1. No actual clock goes backward.
        ClickProbe.ProbeSalt salt = new ClickProbe.ProbeSalt(
                Clock.fixed(after, ZoneOffset.UTC), new SecureRandom());
        String first = salt.hash("203.0.113.77", after);
        salt.hash("203.0.113.77", before);
        String second = salt.hash("203.0.113.77", after);
        boolean stable = first.equals(second);
        System.out.println("BOUNDARY salt D+1 / stale D / D+1: sameDayHashStable=" + stable);
        if (stable) throw new AssertionError("Expected the current candidate's salt replacement defect");

        // The real candidate recorder, real H2 inserts and its existing slow-store mode.
        // Keep pooled connections alive, as the shipped Hikari configuration does.
        // This isolates shutdown from the separate connection-retirement defect
        // reproduced in H2ConstraintProbe.
        var ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:review-design-boundary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("CREATE TABLE link (id BIGINT PRIMARY KEY)").update();
        jdbc.sql("INSERT INTO link (id) VALUES (1)").update();
        for (String statement : ClickProbe.V2.split(";")) {
            if (!statement.isBlank()) jdbc.sql(statement).update();
        }
        Clock clock = Clock.systemUTC();
        // Verify the review fixture before exercising the shutdown path.
        var store = new ClickProbe.ProbeStore(jdbc);
        Instant now = clock.instant();
        store.insert(new ClickProbe.ProbeClick(1, now,
                java.time.LocalDate.ofInstant(now, ZoneOffset.UTC), null, "unknown", "0".repeat(64)));
        jdbc.sql("DELETE FROM click").update();
        var recorder = new ClickProbe.ProbeRecorder(new ClickProbe.ProbeStore(jdbc),
                new ClickProbe.ProbeSalt(clock, new SecureRandom()), clock);
        var context = new AnnotationConfigApplicationContext();
        context.registerBean("lifecycleProcessor", org.springframework.context.support.DefaultLifecycleProcessor.class,
                () -> {
                    var lifecycle = new org.springframework.context.support.DefaultLifecycleProcessor();
                    lifecycle.setTimeoutPerShutdownPhase(10_000);
                    return lifecycle;
                });
        context.registerBean("clickRecorder", ClickProbe.ProbeRecorder.class, () -> recorder,
                bd -> bd.setDestroyMethodName("close"));
        context.refresh();
        var request = new MockHttpServletRequest("GET", "/Ab3dE9fG");
        request.setRemoteAddr("203.0.113.77");
        ClickProbe.ProbeStore.MODE = 1; // exactly the producer's AC-14 two-second write
        for (int i = 0; i < 6; i++) recorder.record(1, request);
        long start = System.nanoTime();
        Thread closing = Thread.ofPlatform().start(context::close);
        closing.join(10_100);
        boolean exceedsTenSeconds = closing.isAlive();
        System.out.println("BOUNDARY six 2s writes, Spring phase timeout 10s: contextCloseStillRunningAfter10s="
                + exceedsTenSeconds + " queued=" + recorder.queueDepth());
        closing.join(6_000);
        ClickProbe.ProbeStore.MODE = 0;
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        System.out.println("BOUNDARY shutdown finished=" + !closing.isAlive() + " elapsedMs=" + elapsed
                + " persisted=" + jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single());
        if (closing.isAlive()) throw new AssertionError("Probe cleanup did not finish");
        if (!exceedsTenSeconds) throw new AssertionError("Expected the unbounded drain to exceed 10s");
        ds.close();
        System.out.println("BOUNDARY reproduced both candidate defects; no product/test source modified");
    }
}
