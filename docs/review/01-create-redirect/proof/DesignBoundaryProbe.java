import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;
import dev.urlshort.UrlshortApplication;

/**
 * Independent design review probe. Reuses the candidate's exact advice and body filter;
 * adds only diagnostic endpoints. No production classes or tests are edited.
 * The SQL collision forces the documented concurrent-insert loser path without timing.
 */
public class DesignBoundaryProbe {
    public static void main(String[] args) throws Exception {
        try (var ctx = new SpringApplicationBuilder(UrlshortApplication.class,
                MechanismProbe.ProbeConfig.class, ReviewConfig.class)
                .properties("server.port=0", "server.address=127.0.0.1",
                        "spring.servlet.multipart.enabled=false").run()) {
            var jdbc = ctx.getBean(JdbcClient.class);
            jdbc.sql("CREATE TABLE review_key (idempotency_key VARCHAR(255) UNIQUE)").update();
            String base = "http://127.0.0.1:" + ctx.getEnvironment()
                    .getRequiredProperty("local.server.port", Integer.class);
            var client = HttpClient.newHttpClient();
            String root = "{\"url\":\"https://example.com/\"}";
            for (int size : new int[] {16384, 16385, 50000}) {
                String padded = "{\"url\":\"https://example.com/\",\"pad\":\"" +
                        "x".repeat(size - "{\"url\":\"https://example.com/\",\"pad\":\"\"}".length()) + "\"}";
                String trailing = root + " ".repeat(size - root.length());
                for (boolean chunked : new boolean[] {false, true}) {
                    send(client, base, "inside-field-" + size + "-chunked-" + chunked,
                            "/review/body", "application/json", padded, chunked, null);
                    send(client, base, "trailing-space-" + size + "-chunked-" + chunked,
                            "/review/body", "application/json", trailing, chunked, null);
                }
            }
            send(client, base, "media-type-reflection", "/review/body",
                    "text/plain; note=review-media-canary-7d9e", root, false, null);
            send(client, base, "key-constraint-log", "/review/collision",
                    "application/json", "{}", false, "review-key-canary-68fd");
        }
        try (var ctx = new SpringApplicationBuilder(ProductionClockConfig.class, FunctionalClockConfig.class)
                .web(WebApplicationType.NONE).run()) {
            System.out.println("REVIEW duplicate-clock-name STARTED (unexpected)");
        } catch (Exception ex) {
            System.out.println("REVIEW duplicate-clock-name rejected=" + ex.getClass().getName());
            System.out.println("REVIEW duplicate-clock-name message=" + ex.getMessage());
        }
        try (var ctx = new SpringApplicationBuilder(ProductionClockConfig.class, RenamedClockConfig.class)
                .web(WebApplicationType.NONE).run()) {
            System.out.println("REVIEW distinct-clock-name selected=" + ctx.getBean(Clock.class).getClass().getName());
        }
    }

    static void send(HttpClient client, String base, String label, String path,
            String mediaType, String body, boolean chunked, String key) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var request = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", mediaType);
        if (key != null) request.header("Idempotency-Key", key);
        request.POST(chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes))
                : HttpRequest.BodyPublishers.ofByteArray(bytes));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("REVIEW " + label + " bytes=" + bytes.length + " status=" + response.statusCode()
                + " requestId=" + response.headers().firstValue("x-request-id").orElse("")
                + " contentType=" + response.headers().firstValue("content-type").orElse("")
                + " body=" + response.body());
    }

    @Configuration(proxyBeanMethods = false)
    static class ReviewConfig {
        @Bean ReviewController reviewController(JdbcClient jdbc) { return new ReviewController(jdbc); }
    }

    @RestController
    static class ReviewController {
        final JdbcClient jdbc;
        ReviewController(JdbcClient jdbc) { this.jdbc = jdbc; }
        @PostMapping(path = "/review/body", consumes = "application/json")
        Map<String, String> body(@RequestBody CreateLinkRequest body) { return Map.of("url", body.url()); }
        @PostMapping(path = "/review/collision", consumes = "application/json")
        void collision(@RequestHeader("Idempotency-Key") String key) {
            jdbc.sql("INSERT INTO review_key (idempotency_key) VALUES (:key)").param("key", key).update();
            jdbc.sql("INSERT INTO review_key (idempotency_key) VALUES (:key)").param("key", key).update();
        }
    }

    record CreateLinkRequest(String url) {}

    @Configuration(proxyBeanMethods = false)
    static class ProductionClockConfig {
        @Bean Clock clock() { return Clock.tickMillis(ZoneOffset.UTC); }
    }
    @Configuration(proxyBeanMethods = false)
    static class FunctionalClockConfig {
        @Bean @Primary FunctionalClock clock() { return new FunctionalClock(); }
    }
    @Configuration(proxyBeanMethods = false)
    static class RenamedClockConfig {
        @Bean @Primary FunctionalClock functionalClock() { return new FunctionalClock(); }
    }
    static class FunctionalClock extends Clock {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Clock.tickMillis(ZoneOffset.UTC).instant(); }
    }
}

