import java.time.Duration;
import java.util.HashSet;
import java.util.List;

/** Independent assertions against b26cbbf's unchanged per-click ownership mechanism. */
public class OwnedExitBoundaryProbe {
    public static void main(String[] args) throws Exception {
        int successRaces = 0;
        int failureRaces = 0;
        for (boolean fail : List.of(false, true)) {
            for (int i = 0; i < 250; i++) {
                var recorder = new RevisionProbe.OwnedRecorder(Duration.ofMillis(200), fail);
                recorder.release.countDown();
                var click = new RevisionProbe.OwnedWrite("race-" + i, recorder);
                recorder.outstanding.add(click);
                var start = new java.util.concurrent.CountDownLatch(1);
                Thread worker = Thread.ofPlatform().start(() -> {
                    await(start);
                    click.run();
                });
                Thread closer = Thread.ofPlatform().start(() -> {
                    await(start);
                    recorder.claimOutstanding();
                });
                start.countDown();
                worker.join();
                closer.join();
                int reports = recorder.reported.size();
                if (fail && reports != 1) throw new AssertionError("Missing or duplicate failure report");
                if (!fail && (reports > 1 || reports + recorder.written.size() < 1)) {
                    throw new AssertionError("Success/claim race lost ownership");
                }
                if (reports == 1 && !recorder.reported.getFirst().requestId().equals("race-" + i)) {
                    throw new AssertionError("Wrong request identity");
                }
                if (fail) failureRaces++; else successRaces++;
                recorder.close();
            }
        }
        System.out.println("OWNED concurrent worker/claim: successRaces=" + successRaces
                + " failureRaces=" + failureRaces + " missingOrDuplicateReports=0");

        var recorder = new RevisionProbe.OwnedRecorder(Duration.ofSeconds(5), false);
        for (int i = 1; i <= 6; i++) recorder.record("req-" + i);
        long start = System.nanoTime();
        recorder.close();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        var ids = new HashSet<String>();
        int unknown = 0;
        for (var report : recorder.reported) {
            if (!ids.add(report.requestId())) throw new AssertionError("Duplicate report");
            if (report.reason().endsWith("outcome unknown")) unknown++;
        }
        if (elapsed > 6000 || ids.size() != 6 || unknown != 1 || recorder.writer.isTerminated()) {
            throw new AssertionError("Bound/accounting/still-blocked precondition failed");
        }
        System.out.println("OWNED normal exit: closeMs=" + elapsed + " reports=" + recorder.reported
                + " uniqueIds=" + ids.size() + " unknown=" + unknown + " daemonWriterStillBlocked=true");
        System.out.println("OWNED main returns normally without releasing the store; all six already reported");
    }

    static void await(java.util.concurrent.CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
    }
}
