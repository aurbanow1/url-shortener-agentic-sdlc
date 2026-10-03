import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Independent re-review: exact documented DDL and a real H2 blocked JDBC write. */
public class RevisionBoundaryProbe {
    public static void main(String[] args) throws Exception {
        schema("mem");
        schema("file");
        blockedJdbc();
    }

    static HikariDataSource pool(String url) {
        var ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMaximumPoolSize(2);
        return ds;
    }

    static void ddl(JdbcClient jdbc, String sql) {
        for (String statement : sql.replaceAll("(?m)^--.*$", "").split(";")) {
            if (!statement.isBlank()) jdbc.sql(statement).update();
        }
    }

    static void schema(String mode) throws Exception {
        String url = mode.equals("mem")
                ? "jdbc:h2:mem:review-revised-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                : "jdbc:h2:file:./build/review-revised-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
        try (var ds = pool(url); var keeper = ds.getConnection()) {
            var jdbc = JdbcClient.create(ds);
            ddl(jdbc, Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql")));
            String design = Files.readString(Path.of("missions/01-greenfield-core/slices/02-analytics/design.md"));
            String v2 = design.substring(design.indexOf("```sql") + 6);
            v2 = v2.substring(0, v2.indexOf("```"));
            ddl(jdbc, v2);
            jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('Probe001', 'https://example.com/', :t)")
                    .param("t", Instant.now().atOffset(ZoneOffset.UTC)).update();
            ds.getHikariPoolMXBean().softEvictConnections();
            for (String token : List.of("browser", "bot", "other", "unknown")) {
                ConstraintProbe.clickInsert(jdbc, token, "a".repeat(64));
            }
            int invalidRejected = 0;
            try { ConstraintProbe.clickInsert(jdbc, "tablet", "a".repeat(64)); }
            catch (org.springframework.dao.DataIntegrityViolationException expected) { invalidRejected++; }
            try { ConstraintProbe.clickInsert(jdbc, "bot", "abc"); }
            catch (org.springframework.dao.DataIntegrityViolationException expected) { invalidRejected++; }
            long count = jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single();
            if (count != 4 || invalidRejected != 2) throw new AssertionError("Revised schema boundary failed");
            ddl(jdbc, "DROP TABLE click; DROP TABLE user_agent_class;");
            long links = jdbc.sql("SELECT COUNT(*) FROM link").query(Long.class).single();
            jdbc.sql("SELECT COUNT(*) FROM audit_log").query(Long.class).single();
            if (links != 1) throw new AssertionError("Rollback touched links");
            System.out.println("REVIEW " + mode + " exact documented V2 after DDL-session retirement: allFourClasses=true invalidClassAndHashRejected=true rollbackLeavesV1=true");
        }
    }

    static void blockedJdbc() throws Exception {
        // Engine-boundary fixture only: one row lock, default H2 2s lock timeout.
        // This does not claim the click insert normally conflicts on this unique key.
        String url = "jdbc:h2:mem:review-jdbc-lock-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        var ds = pool(url);
        var jdbc = JdbcClient.create(ds);
        jdbc.sql("CREATE TABLE locked_write (id INT PRIMARY KEY)").update();
        try (var holder = DriverManager.getConnection(url, "sa", "")) {
            holder.setAutoCommit(false);
            holder.createStatement().executeUpdate("INSERT INTO locked_write VALUES (1)");
            var recorder = new RevisionProbe.Recorder(0, Duration.ofMillis(200));
            var entered = new CountDownLatch(1);
            var finished = new CountDownLatch(1);
            var outcome = new AtomicReference<String>("pending");
            recorder.writer.execute(() -> {
                entered.countDown();
                try {
                    jdbc.sql("INSERT INTO locked_write VALUES (1)").update();
                    outcome.set("written");
                } catch (RuntimeException failure) {
                    outcome.set(failure.getClass().getSimpleName());
                } finally { finished.countDown(); }
            });
            if (!entered.await(2, TimeUnit.SECONDS)) throw new AssertionError("Writer did not enter");
            Thread.sleep(100);
            long start = System.nanoTime();
            recorder.close();
            long recorderMs = (System.nanoTime() - start) / 1_000_000;
            boolean unfinishedAtReturn = finished.getCount() == 1;
            start = System.nanoTime();
            ds.close();
            long poolMs = (System.nanoTime() - start) / 1_000_000;
            boolean unfinishedAfterPoolClose = finished.getCount() == 1;
            boolean settled = finished.await(3, TimeUnit.SECONDS);
            holder.rollback();
            System.out.println("REVIEW blocked H2 JDBC: recorderCloseMs=" + recorderMs
                    + " unfinishedAtRecorderReturn=" + unfinishedAtReturn + " poolCloseMs=" + poolMs
                    + " unfinishedAfterPoolClose=" + unfinishedAfterPoolClose
                    + " writeSettled=" + settled + " outcome=" + outcome.get());
            if (recorderMs > 1000 || !settled) throw new AssertionError("Bounded-close boundary failed");
        } finally { ds.close(); }
    }
}
