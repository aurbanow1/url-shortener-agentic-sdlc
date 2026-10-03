import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.Flyway;

/** Independent design controls. Real H2 and shipped V1/V2; no product changes.
 * Identity SQL functions only pause after branch one and before branch two, without changing values.
 * The counter case is an explicit model of the proposed insert/CAS/increment sequence, not an
 * executed future ClickRecorder implementation.
 */
public class StatsConsistencyProbe {
    static final String URL = "jdbc:h2:mem:review-stats;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    static CountDownLatch secondBranch;
    static CountDownLatch proceed;
    static final AtomicBoolean firstSeen = new AtomicBoolean();
    static final AtomicBoolean held = new AtomicBoolean();
    static final String QUERY = "SELECT clicked_on, referrer, REVIEW_FIRST(COUNT(*)) AS clicks,"
            + " CAST(NULL AS BIGINT) AS unique_visitors, CAST(NULL AS BIGINT) AS bot_clicks"
            + " FROM click WHERE link_id = ? GROUP BY clicked_on, referrer"
            + " UNION ALL SELECT clicked_on, NULL, NULL, COUNT(DISTINCT client_hash),"
            + " SUM(CASE WHEN user_agent_class = 'bot' THEN 1 ELSE 0 END)"
            + " FROM click WHERE link_id = REVIEW_HOLD(?) GROUP BY clicked_on";

    public static long first(long value) { firstSeen.set(true); return value; }
    public static long hold(long value) throws Exception {
        if (held.compareAndSet(false, true)) {
            if (!firstSeen.get()) throw new AssertionError("Hold did not follow the first branch");
            secondBranch.countDown();
            if (!proceed.await(5, TimeUnit.SECONDS)) throw new AssertionError("Writer did not release query");
        }
        return value;
    }

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(StatsConsistencyProbe.class.getClassLoader());
        Flyway.configure().dataSource(URL, "sa", "").target("2").load().migrate();
        try (Connection writer = DriverManager.getConnection(URL, "sa", ""); Statement s = writer.createStatement()) {
            s.execute("CREATE ALIAS REVIEW_FIRST FOR 'StatsConsistencyProbe.first'");
            s.execute("CREATE ALIAS REVIEW_HOLD FOR 'StatsConsistencyProbe.hold'");
            s.executeUpdate("INSERT INTO link (code, url, created_at) VALUES ('review01', 'https://example.org', CURRENT_TIMESTAMP)");
            for (String change : List.of("insert", "delete")) {
                s.executeUpdate("DELETE FROM click");
                insert(writer, "a", "browser");
                firstSeen.set(false); held.set(false);
                secondBranch = new CountDownLatch(1); proceed = new CountDownLatch(1);
                try (var executor = Executors.newSingleThreadExecutor()) {
                    var future = executor.submit(() -> {
                        Thread.currentThread().setContextClassLoader(StatsConsistencyProbe.class.getClassLoader());
                        try (Connection reader = DriverManager.getConnection(URL, "sa", "")) {
                            System.out.println("SNAPSHOT " + change + " isolation=" + reader.getTransactionIsolation());
                            return rows(reader);
                        }
                    });
                    try {
                        if (!secondBranch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Second branch not reached");
                        if (change.equals("insert")) insert(writer, "b", "bot");
                        else s.executeUpdate("DELETE FROM click");
                    } finally { proceed.countDown(); }
                    List<String> observed = future.get(5, TimeUnit.SECONDS);
                    List<String> expected = List.of("2026-10-01|null|1|null|null", "2026-10-01|null|null|1|0");
                    if (!observed.equals(expected)) throw new AssertionError("Snapshot changed: " + observed);
                    try (ResultSet fresh = s.executeQuery("SELECT COUNT(*) FROM click")) {
                        fresh.next();
                        int count = fresh.getInt(1);
                        if (count != (change.equals("insert") ? 2 : 0)) throw new AssertionError("Writer did not commit");
                        System.out.println("SNAPSHOT " + change + " fresh count=" + count + ", same-statement rows=" + observed);
                    }
                }
            }
        }
        counterControl(false, false);
        counterControl(true, false);
        counterControl(true, true);
        System.out.println("CONTROLS complete");
    }

    static List<String> rows(Connection reader) throws Exception {
        List<String> values = new ArrayList<>();
        try (PreparedStatement ps = reader.prepareStatement(QUERY)) {
            ps.setLong(1, 1); ps.setLong(2, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) values.add(rs.getObject(1) + "|" + rs.getObject(2) + "|" + rs.getObject(3)
                        + "|" + rs.getObject(4) + "|" + rs.getObject(5));
            }
        }
        return values;
    }

    static void insert(Connection c, String hash, String kind) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
                + " VALUES (1, TIMESTAMP WITH TIME ZONE '2026-10-01 12:00:00+00', DATE '2026-10-01', NULL, ?, ?)")) {
            ps.setString(1, kind); ps.setString(2, "0".repeat(63) + hash); ps.executeUpdate();
        }
    }

    static void counterControl(boolean claimAtShutdown, boolean countBeforeCas) throws Exception {
        int RUNNING = 1, DONE = 2, CLAIMED = 3;
        AtomicInteger state = new AtomicInteger(RUNNING);
        AtomicInteger stored = new AtomicInteger(), recorded = new AtomicInteger(), lost = new AtomicInteger();
        CountDownLatch inStore = new CountDownLatch(1), returnFromStore = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var worker = executor.submit(() -> {
                inStore.countDown();
                try { if (!returnFromStore.await(5, TimeUnit.SECONDS)) throw new AssertionError("No store release"); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                stored.incrementAndGet(); // successful insert returns
                if (countBeforeCas) recorded.incrementAndGet(); // proposed in-passing correction
                boolean ownsReport = state.compareAndSet(RUNNING, DONE);
                if (!countBeforeCas && ownsReport) recorded.incrementAndGet(); // original design section 1
            });
            if (!inStore.await(5, TimeUnit.SECONDS)) throw new AssertionError("Store not entered");
            if (claimAtShutdown && state.compareAndSet(RUNNING, CLAIMED)) lost.incrementAndGet();
            returnFromStore.countDown(); worker.get(5, TimeUnit.SECONDS);
            int expectedRecorded = claimAtShutdown && !countBeforeCas ? 0 : 1;
            if (stored.get() != 1 || recorded.get() != expectedRecorded || lost.get() != (claimAtShutdown ? 1 : 0)) {
                throw new AssertionError("Counter model did not match its stated sequence");
            }
            System.out.println("COUNTER shutdownClaim=" + claimAtShutdown + " countBeforeCas=" + countBeforeCas
                    + " stored=" + stored + " recorded=" + recorded + " lost=" + lost);
        }
    }
}
