import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Checks the proposed V2 constraint after real pooled connections are retired. */
public class H2ConstraintProbe {
    public static void main(String[] args) {
        check("memory", "jdbc:h2:mem:review-constraint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        check("file", "jdbc:h2:file:./build/review-constraint-" + System.nanoTime()
                + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE");
    }

    static void check(String mode, String url) {
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl(url);
            pool.setUsername("sa");
            pool.setPassword("");
            pool.setMaximumPoolSize(2);
            System.out.println("CONSTRAINT mode=" + mode);
            JdbcClient jdbc = JdbcClient.create(pool);
            // Hold another physical session open so retiring the DDL session
            // cannot close/reopen the whole file database and hide the defect.
            try (var keepDatabaseOpen = pool.getConnection()) {
            jdbc.sql("CREATE TABLE link (id BIGINT PRIMARY KEY)").update();
            jdbc.sql("INSERT INTO link (id) VALUES (1)").update();
            for (String sql : ClickProbe.V2.split(";")) if (!sql.isBlank()) jdbc.sql(sql).update();
            insert(jdbc);
            System.out.println("CONSTRAINT valid insert before pool retirement=PASS");
            pool.getHikariPoolMXBean().softEvictConnections();
            System.out.println("CONSTRAINT replaced physical connections using Hikari softEvictConnections");
            try {
                insert(jdbc);
                System.out.println("CONSTRAINT valid insert after pool retirement=PASS");
            } catch (RuntimeException e) {
                System.out.println("CONSTRAINT valid insert after pool retirement=FAIL " + e.getClass().getSimpleName());
                for (Throwable t = e; t != null; t = t.getCause()) {
                    System.out.println("CONSTRAINT cause=" + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
            System.out.println("CONSTRAINT rows=" + jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single());
            }
            catch (java.sql.SQLException e) { throw new IllegalStateException(e); }
        }
    }

    static void insert(JdbcClient jdbc) {
        Instant now = Instant.now();
        jdbc.sql("INSERT INTO click (link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash) "
                + "VALUES (1,:at,:day,NULL,'browser',:hash)")
                .param("at", now.atOffset(ZoneOffset.UTC))
                .param("day", LocalDate.ofInstant(now, ZoneOffset.UTC))
                .param("hash", "0".repeat(64)).update();
    }
}
