package dev.urlshort.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.click.ClickRecorder;
import dev.urlshort.link.FunctionalClock;
import jakarta.servlet.Filter;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** Executes the candidate's real AC-5 helper with a temporarily delayed writer, without modifying tests. */
public class QaStatsPollingResolution {
    public static void main(String[] args) throws Exception {
        try (var app = new SpringApplicationBuilder(UrlshortApplication.class).registerShutdownHook(false).run(
                "--server.port=0", "--server.address=127.0.0.1", "--spring.profiles.active=functional",
                "--spring.datasource.url=jdbc:h2:mem:qa-stats-poll-e40b095;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--urlshort.rate-limit.create-per-minute=60", "--urlshort.rate-limit.redirect-per-minute=600",
                "--urlshort.rate-limit.trusted-proxies=" + (args.length == 0 ? "" : "10.9.9.9"))) {
            var clock = app.getBean(FunctionalClock.class);
            ClientIdentityCharacterizationJourneyTest.onTheFixedDay(clock);
            var json = app.getBean(JsonMapper.class);
            var jdbc = app.getBean(JdbcClient.class);
            var recorder = app.getBean(ClickRecorder.class);
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var statsRequests = new AtomicInteger();
            var refused = new AtomicInteger();
            Filter observe = (request, response, chain) -> {
                boolean stats = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI().endsWith("/stats");
                if (stats) statsRequests.incrementAndGet();
                chain.doFilter(request, response);
                if (stats && ((jakarta.servlet.http.HttpServletResponse) response).getStatus() == 429) {
                    refused.incrementAndGet();
                    release.countDown();
                }
            };
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) app)
                    .addFilters(observe, app.getBean(RequestIdFilter.class), app.getBean(RateLimitFilter.class)).build();
            String code = ClientIdentityCharacterizationJourneyTest.create(mvc, json, "https://example.com/poll-probe");
            Field writerField = ClickRecorder.class.getDeclaredField("writer");
            writerField.setAccessible(true);
            var writer = (ThreadPoolExecutor) writerField.get(recorder);
            writer.execute(() -> {
                entered.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) { throw new RuntimeException(e); }
            });
            if (!entered.await(1, TimeUnit.SECONDS)) throw new AssertionError("writer did not enter");
            long started = System.nanoTime();
            Throwable failure = null;
            try {
                ClientIdentityCharacterizationJourneyTest.assertGroupsAsTheReference(mvc, json, jdbc,
                        ClientIdentityCharacterizationJourneyTest.withIdentity(get("/" + code), "10.9.9.9",
                                "203.0.113.7", true, "203.0.113.8"), args.length == 0 ? "10.9.9.9" : "203.0.113.7", code);
            } catch (Throwable e) { failure = e; }
            finally { release.countDown(); }
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            writer.submit(() -> {}).get(3, TimeUnit.SECONDS);
            var positive = mvc.perform(get("/api/links/" + code + "/stats")
                    .with(ClientIdentityCharacterizationJourneyTest.peer("192.0.2.99"))).andReturn().getResponse();
            var stats = json.readTree(positive.getContentAsString());
            if (positive.getStatus() != 200 || stats.get("totalClicks").asLong() != 3
                    || stats.get("clicksPerDay").get(0).get("uniqueVisitors").asLong() != 2) {
                throw new AssertionError("product did not finish correctly: " + positive.getContentAsString());
            }
            System.out.println("PROBE trust=" + (args.length == 0 ? "none" : "P") + " elapsed_ms=" + elapsed + " stats_requests=" + statsRequests.get()
                    + " stats_429=" + refused.get() + " helper_failure=" + (failure == null ? "none" : failure));
            System.out.println("POSITIVE_CONTROL status=" + positive.getStatus() + " body=" + stats);
            if (failure != null || refused.get() != 0 || statsRequests.get() != 2 || elapsed < 2500 || elapsed >= 10000) {
                throw new AssertionError("expected helper to wait for the held writer, read statistics once, and pass the complete oracle without429", failure);
            }
            System.out.println("RESOLVED: actual candidate helper waits for the held writer, performs one statistics read, passes its entire3/2/day/privacy oracle and leaves the frozen create budget available.");
        }
    }
}
