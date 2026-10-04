package dev.urlshort.click;

import java.time.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Review-only control: widen the legal gap between committed DELETE and log publication. */
public class PurgePublicationProbe {
    public static void main(String[] args) throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:review-purge-publication;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = JdbcClient.create(ds);
        jdbc.sql("CREATE TABLE click (clicked_on DATE NOT NULL)").update();
        jdbc.sql("INSERT INTO click VALUES (DATE '2026-01-01'), (DATE '2026-07-07')").update();
        var purge = new ClickPurge(new ClickStore(jdbc), Clock.fixed(Instant.parse("2026-10-05T00:10:01Z"), ZoneOffset.UTC), new ClickRetentionProperties(90, true));
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var published = new CopyOnWriteArrayList<String>();
        Logger logger = (Logger) LoggerFactory.getLogger(ClickPurge.class);
        AppenderBase<ILoggingEvent> barrier = new AppenderBase<>() {
            protected void append(ILoggingEvent event) {
                if (!event.getFormattedMessage().equals("clicks purged")) return;
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("barrier timed out");
                } catch (InterruptedException ex) { throw new AssertionError(ex); }
                published.add(event.getFormattedMessage());
            }
        };
        barrier.setContext(logger.getLoggerContext());
        barrier.start();
        logger.addAppender(barrier);
        Thread worker = new Thread(purge::run, "review-purge");
        try {
            worker.start();
            if (!entered.await(5, TimeUnit.SECONDS)) throw new AssertionError("no purge event attempted");
            long remaining = jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single();
            if (remaining != 1 || !published.isEmpty()) throw new AssertionError("expected committed delete before publication");
            System.out.println("BEFORE RELEASE: committed DELETE observed (1 boundary row remains), published events=0");
            release.countDown();
            worker.join(5000);
            if (worker.isAlive() || published.size() != 1) throw new AssertionError("expected one eventual event");
            System.out.println("AFTER RELEASE: published events=1; observing rows does not synchronize with subsequent logging");
        } finally {
            release.countDown();
            worker.join(5000);
            logger.detachAppender(barrier);
            purge.close();
        }
    }
}
