import java.sql.DriverManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Normal JVM exit after the proposed recorder-then-pool shutdown order. */
public class ExitBoundaryProbe {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:mem:review-exit-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        var pool = RevisionBoundaryProbe.pool(url);
        var jdbc = JdbcClient.create(pool);
        jdbc.sql("CREATE TABLE locked_write (id INT PRIMARY KEY)").update();
        // Independent connection models another open transaction holding an engine lock.
        // Intentionally lives until process exit: releasing it would remove the tested stall.
        var blocker = DriverManager.getConnection(url, "sa", "");
        blocker.setAutoCommit(false);
        blocker.createStatement().executeUpdate("INSERT INTO locked_write VALUES (1)");
        var recorder = new RevisionProbe.Recorder(0, Duration.ofSeconds(5));
        var thirdSettled = new AtomicBoolean();
        // Same FIFO executor and close() as the producer. Adapt only the store operation:
        // two 2-second writes followed by a real H2 lock wait. Each write uses the design's
        // success-or-runtime-failure accounting, including the captured request identity.
        for (int i = 1; i <= 3; i++) {
            int id = i;
            recorder.writer.execute(() -> {
                try {
                    if (id < 3) Thread.sleep(2000);
                    jdbc.sql("INSERT INTO locked_write VALUES (:id)").param("id", id < 3 ? id + 10 : 1).update();
                    recorder.written.add("req-" + id);
                } catch (Exception failed) {
                    recorder.lost.add(new RevisionProbe.Lost("req-" + id, "write failed"));
                } finally {
                    if (id == 3) {
                        thirdSettled.set(true);
                        System.out.println("EXIT write req-3 settled; this line must exist to support complete accounting");
                    }
                }
            });
        }
        for (int i = 4; i <= 6; i++) recorder.record("req-" + i);
        long start = System.nanoTime();
        recorder.close();
        pool.close();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        int accounted = recorder.written.size() + recorder.lost.size();
        System.out.println("EXIT recorder and pool closed after " + elapsed + " ms; written=" + recorder.written
                + "; lost=" + recorder.lost + "; accounted=" + accounted + "/6; inFlightReq3Settled=" + thirdSettled);
        if (accounted != 5 || thirdSettled.get()) throw new AssertionError("Expected the unaccounted in-flight boundary");
        System.out.println("EXIT main returns normally; the remaining writer is daemon (producer thread factory)");
    }
}
