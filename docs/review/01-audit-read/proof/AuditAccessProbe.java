import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Independent design-mechanism controls, not a test of unbuilt product code.
 * Uses the shipped app/advice, a minimal copy of the specified handler guard and produces condition,
 * and a synthetic confidential row. All listeners and clients stay on loopback; all databases are
 * temporary in-memory H2. A real config file supplies the proposed none default, then command-line
 * settings override it as normal Spring operators can. No product or producer evidence is changed.
 */
public class AuditAccessProbe {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (String strategy : List.of("default", "native", "framework")) {
            List<String> options = new ArrayList<>(List.of(
                    "--server.address=127.0.0.1", "--server.port=0", "--logging.level.root=warn",
                    "--spring.datasource.url=jdbc:h2:mem:review-audit-" + strategy + ";DB_CLOSE_DELAY=-1",
                    "--spring.config.additional-location=" + Path.of(
                            "docs/review/01-audit-read/proof/forwarding-default.properties").toUri(),
                    "--urlshort.rate-limit.create-per-minute=1000000",
                    "--urlshort.rate-limit.redirect-per-minute=1000000"));
            if (!strategy.equals("default")) options.add("--server.forward-headers-strategy=" + strategy);
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                    UrlshortApplication.class, ProbeConfig.class).run(options.toArray(String[]::new))) {
                String base = "http://127.0.0.1:" + context.getEnvironment().getRequiredProperty("local.server.port");
                String effective = context.getEnvironment().getRequiredProperty("server.forward-headers-strategy");
                System.out.println("CONTROL strategy=" + strategy + " effective=" + effective);
                check(client, base, strategy + " plain", "", "*/*", null, null);
                check(client, base, strategy + " XFF-forged-loopback", "", "*/*", "X-Forwarded-For", "127.0.0.2");
                check(client, base, strategy + " XFF-real-remote", "", "*/*", "X-Forwarded-For", "192.0.2.10");
                check(client, base, strategy + " Forwarded-forged-loopback", "", "*/*", "Forwarded", "for=127.0.0.2");
                if (strategy.equals("default")) {
                    for (String accept : List.of("*/*", "text/html", "application/problem+json",
                            "text/html,application/xhtml+xml,*/*;q=0.8")) {
                        check(client, base, "accept=" + accept + " invalid-limit", "?limit=bad", accept, null, null);
                        check(client, base, "accept=" + accept + " denied", "", accept, "X-Forwarded-For", "192.0.2.10");
                    }
                }
            }
        }
        System.out.println("CONTROL complete");
        System.exit(0);
    }

    static void check(HttpClient client, String base, String label, String query, String accept,
            String header, String value) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + "/api/audit" + query)).header("Accept", accept);
        if (header != null) request.header(header, value);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("CONTROL " + label + " -> " + response.statusCode() + " type="
                + response.headers().firstValue("Content-Type").orElse("none") + " body=" + response.body());
    }

    @Configuration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping(path = "/api/audit", produces = "application/json")
        Map<String, Object> page(@RequestParam(name = "limit", required = false) String limit,
                HttpServletRequest request) throws Exception {
            String peer = request.getRemoteAddr();
            if (request.getHeader("X-Forwarded-For") != null || request.getHeader("Forwarded") != null
                    || peer == null || peer.isEmpty() || !InetAddress.getByName(peer).isLoopbackAddress()) {
                throw new ErrorResponseException(HttpStatus.FORBIDDEN);
            }
            if (limit != null) {
                try { Integer.parseInt(limit); }
                catch (NumberFormatException ex) { throw Problems.validation("limit", "format", "must be a whole number"); }
            }
            return Map.of("items", List.of(Map.of("entityId", "AUDIT-REVIEW-CANARY")), "next", "synthetic");
        }
    }
}
