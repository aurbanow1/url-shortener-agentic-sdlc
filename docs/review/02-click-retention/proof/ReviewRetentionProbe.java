import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.link.FunctionalClock;
import dev.urlshort.web.RequestIdFilter;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Independent review controls using the handed-off design's probe component, no product edits. */
public class ReviewRetentionProbe {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("invalid")) {
            invalid();
        } else {
            scheduleAndCapture();
        }
    }

    static void invalid() {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        try {
            new SpringApplicationBuilder(UrlshortApplication.class, RetentionProbe.ProbeConfig.class)
                .registerShutdownHook(false).run("--server.port=0", "--server.address=127.0.0.1",
                    "--spring.datasource.url=jdbc:h2:mem:review-retention-invalid;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "--urlshort.click.retention-days=0");
            throw new AssertionError("invalid value started");
        } catch (Exception expected) {
            System.setOut(original);
            String capture = bytes.toString(StandardCharsets.UTF_8);
            capture.lines().filter(s -> s.contains("jdbc:h2") || s.contains("APPLICATION FAILED TO START"))
                .forEach(original::println);
            original.println("REVIEW A4 shipped INFO logs: capturedDatasource=" + capture.contains("jdbc:h2:mem:review-retention-invalid")
                + " failureAnalysisDatasource=" + capture.lines().filter(s -> s.contains("APPLICATION FAILED TO START"))
                    .anyMatch(s -> s.contains("jdbc:h2"))
                + " purgeStarts=" + RetentionProbe.ProbePurge.starts.get());
        } finally { System.setOut(original); }
    }

    static void scheduleAndCapture() throws Exception {
        RetentionProbe.ProbePurge.autoStart = true;
        RetentionProbe.ProbePurge.batched = false;
        RetentionProbe.ProbePurge.syncStartup = true;
        RetentionProbe.ProbePurge.closeDeadlineMillis = 3000;
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(UrlshortApplication.class,
                RetentionProbe.ProbeConfig.class).registerShutdownHook(false).run("--server.port=0",
                "--server.address=127.0.0.1",
                "--spring.datasource.url=jdbc:h2:mem:review-retention-schedule;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")) {
            var purge = ctx.getBean(RetentionProbe.ProbePurge.class);
            var clock = ctx.getBean(FunctionalClock.class);
            var jdbc = ctx.getBean(JdbcClient.class);
            LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
            long link = jdbc.sql("INSERT INTO link (code,url,created_at) VALUES ('ReviewRt','https://example.com/',:at)")
                .param("at", clock.instant()).update();
            link = jdbc.sql("SELECT id FROM link WHERE code='ReviewRt'").query(Long.class).single();
            RetentionProbe.insert(jdbc, link, today.minusDays(90), 2);
            RetentionProbe.insert(jdbc, link, today.minusDays(89), 2);
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx)
                .addFilters(ctx.getBean(RequestIdFilter.class)).build();
            var overlapMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx)
                .addFilters(ctx.getBean(RequestIdFilter.class))
                .alwaysDo(result -> {
                    if (!RetentionProbe.await(() -> purge.runs.size() == 2, 7000))
                        throw new AssertionError("scheduled run did not overlap capture");
                }).build();
            PrintStream original = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CapturedOutput capture = org.mockito.Mockito.mock(CapturedOutput.class);
            org.mockito.Mockito.when(capture.getAll()).thenAnswer(invocation -> bytes.toString(StandardCharsets.UTF_8));
            Class<?> testType = Class.forName("dev.urlshort.web.ObservabilityJourneyTest");
            var constructor = testType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object shippedTest = constructor.newInstance();
            var mvcField = testType.getDeclaredField("mockMvc");
            mvcField.setAccessible(true);
            mvcField.set(shippedTest, mvc);
            var mapperField = testType.getDeclaredField("jsonMapper");
            mapperField.setAccessible(true);
            mapperField.set(shippedTest, ctx.getBean(JsonMapper.class));
            var assertion = testType.getDeclaredMethod("AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId",
                String.class, CapturedOutput.class);
            assertion.setAccessible(true);
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            assertion.invoke(shippedTest, "404 unknown code", capture);
            System.setOut(original);
            original.println("REVIEW unchanged ObservabilityJourneyTest AC26, no overlap: PASS");
            bytes.reset();
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            long t0 = System.nanoTime();
            clock.shift(Duration.between(clock.instant(), today.plusDays(1).atTime(0, 10, 1).toInstant(ZoneOffset.UTC)));
            // Widen only the test observation window to deterministically reproduce the legal
            // interleaving admitted by design.md: scheduler logs between windowStart and getAll.
            // The compiled shipped assertion is unchanged. No purge trigger is sent.
            mvcField.set(shippedTest, overlapMvc);
            Throwable failure = null;
            try { assertion.invoke(shippedTest, "404 unknown code", capture); }
            catch (java.lang.reflect.InvocationTargetException ex) { failure = ex.getCause(); }
            boolean ran = purge.runs.size() == 2;
            System.setOut(original);
            var mapper = JsonMapper.builder().build();
            List<String> lines = bytes.toString(StandardCharsets.UTF_8).lines().filter(s -> s.startsWith("{")).toList();
            lines.forEach(original::println);
            long unrelated = lines.stream().map(mapper::readTree).filter(event -> !event.has("requestId")).count();
            original.println("REVIEW A8 single DELETE, synchronous startup, no trigger: ran=" + ran
                + " elapsedMs=" + (System.nanoTime()-t0)/1000000
                + " boundaryOld=" + RetentionProbe.count(jdbc, "clicked_on = DATE '" + today.minusDays(90) + "'")
                + " retained=" + RetentionProbe.count(jdbc, "clicked_on = DATE '" + today.minusDays(89) + "'")
                + " requestWindowUncorrelated=" + unrelated);
            original.println("REVIEW unchanged ObservabilityJourneyTest AC26, forced legal overlap: " + failure);
            if (!ran || unrelated != 1 || !(failure instanceof AssertionError))
                throw new AssertionError("control did not reproduce the shipped assertion failure");
            clock.reset();
        }
    }
}
