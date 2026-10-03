import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reviewer controls for design 4eb1eb4, derived from the original independent reproducer.
 * Only the two proposed changes are modeled: effective NONE admission, and no produces condition
 * with an explicit successful content type. Synthetic rows, real shipped advice and Tomcat,
 * loopback listeners, temporary in-memory databases; no product or producer file changes.
 */
public class AuditAccessRevisionProbe {
    static int controls;

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        for (String strategy : List.of("default", "native", "framework", "unset", "unset-cloud")) {
            List<String> options = new ArrayList<>(List.of(
                    "--server.address=127.0.0.1", "--server.port=0", "--logging.level.root=warn",
                    "--spring.datasource.url=jdbc:h2:mem:review-revision-" + strategy + ";DB_CLOSE_DELAY=-1",
                    "--urlshort.rate-limit.create-per-minute=1000000",
                    "--urlshort.rate-limit.redirect-per-minute=1000000"));
            if (!strategy.startsWith("unset")) options.add("--spring.config.additional-location=" + Path.of(
                    "docs/review/01-audit-read/proof/forwarding-default.properties").toUri());
            if (strategy.equals("native") || strategy.equals("framework")) {
                options.add("--server.forward-headers-strategy=" + strategy);
            }
            if (strategy.equals("unset-cloud")) options.add("--spring.main.cloud-platform=kubernetes");
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                    UrlshortApplication.class, ProbeConfig.class).run(options.toArray(String[]::new))) {
                String base = "http://127.0.0.1:" + context.getEnvironment().getRequiredProperty("local.server.port");
                var effective = context.getBean(ServerProperties.class).getForwardHeadersStrategy();
                String expected = strategy.equals("default") ? "NONE" : strategy.startsWith("unset") ? "null" : strategy.toUpperCase();
                if (!String.valueOf(effective).equals(expected)) throw new AssertionError("Wrong effective strategy: " + effective);
                System.out.println("REVISION strategy=" + strategy + " effective=" + effective);
                int plain = strategy.equals("default") ? 200 : 403;
                for (String method : List.of("GET", "HEAD")) {
                    check(client, base, strategy + " plain", method, "", "*/*", null, null, plain);
                    check(client, base, strategy + " forged-XFF", method, "", "*/*", "X-Forwarded-For", "127.0.0.2", 403);
                    check(client, base, strategy + " forged-Forwarded", method, "", "*/*", "Forwarded", "for=127.0.0.2", 403);
                }
                check(client, base, strategy + " remote-XFF", "GET", "", "*/*", "X-Forwarded-For", "192.0.2.10", 403);
                if (strategy.equals("default")) {
                    for (String accept : List.of("*/*", "text/html", "application/problem+json",
                            "text/html,application/xhtml+xml,*/*;q=0.8")) {
                        check(client, base, "accept=" + accept + " invalid-limit", "GET", "?limit=bad", accept, null, null, 400);
                        check(client, base, "accept=" + accept + " denied", "GET", "", accept, "X-Forwarded-For", "192.0.2.10", 403);
                        check(client, base, "accept=" + accept + " valid", "GET", "", accept, null, null, 200);
                    }
                }
            }
        }
        System.out.println("REVISION PASS controls=" + controls);
        System.exit(0);
    }

    static void check(HttpClient client, String base, String label, String method, String query, String accept,
            String header, String value, int expected) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + "/api/audit" + query))
                .timeout(Duration.ofSeconds(5)).method(method, HttpRequest.BodyPublishers.noBody()).header("Accept", accept);
        if (header != null) request.header(header, value);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        String type = response.headers().firstValue("Content-Type").orElse("none");
        String expectedType = expected == 200 ? "application/json" : "application/problem+json";
        boolean head = method.equals("HEAD");
        boolean correctBody = head ? response.body().isEmpty() : expected == 200
                ? response.body().contains("AUDIT-REVIEW-CANARY")
                : response.body().contains("\"status\":" + expected) && !response.body().contains("AUDIT-REVIEW-CANARY");
        if (response.statusCode() != expected || !type.equals(expectedType) || !correctBody
                || response.headers().firstValue("X-Request-Id").isEmpty()) {
            throw new AssertionError(label + " -> " + response.statusCode() + " " + type + " " + response.body());
        }
        controls++;
        System.out.println("REVISION " + method + " " + label + " -> " + response.statusCode()
                + " type=" + type + " body=" + response.body());
    }

    @Configuration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean ProbeController probeController(ServerProperties properties) { return new ProbeController(properties); }
    }

    @RestController
    static class ProbeController {
        final boolean peerIsConnection;
        ProbeController(ServerProperties properties) {
            peerIsConnection = properties.getForwardHeadersStrategy() == ServerProperties.ForwardHeadersStrategy.NONE;
        }

        @GetMapping("/api/audit")
        ResponseEntity<Map<String, Object>> page(@RequestParam(name = "limit", required = false) String limit,
                HttpServletRequest request) throws Exception {
            String peer = request.getRemoteAddr();
            if (!peerIsConnection || request.getHeader("X-Forwarded-For") != null || request.getHeader("Forwarded") != null
                    || peer == null || peer.isEmpty() || !InetAddress.getByName(peer).isLoopbackAddress()) {
                throw new ErrorResponseException(HttpStatus.FORBIDDEN);
            }
            if (limit != null) {
                try { Integer.parseInt(limit); }
                catch (NumberFormatException ex) { throw Problems.validation("limit", "format", "must be a whole number"); }
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("items", List.of(Map.of("entityId", "AUDIT-REVIEW-CANARY")), "next", "synthetic"));
        }
    }
}
