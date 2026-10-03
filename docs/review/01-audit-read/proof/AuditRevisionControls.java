import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import dev.urlshort.UrlshortApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Additional independent CR-01 controls. Actual product classes; loopback sockets only. */
public class AuditRevisionControls {
    static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public static void main(String[] args) throws Exception {
        Path bytecode = Path.of(args[0]);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(bytecode))) {
            int status = ToolProvider.findFirst("javap").orElseThrow().run(out, out, "-c", "-p", "-classpath",
                System.getProperty("java.class.path"), "org.springframework.boot.tomcat.autoconfigure.TomcatWebServerFactoryCustomizer");
            if (status != 0) throw new AssertionError("javap " + status);
        }
        int checks = 0;
        for (String variant : List.of("custom-header", "both", "blank", "spaces")) {
            String remote = switch (variant) { case "custom-header", "both" -> "X-Real-IP"; case "spaces" -> "   "; default -> ""; };
            String protocol = switch (variant) { case "both" -> "X-Custom-Proto"; case "spaces" -> "   "; default -> ""; };
            var options = new ArrayList<>(List.of("--server.address=127.0.0.1", "--server.port=0", "--logging.level.root=warn",
                "--spring.datasource.url=jdbc:h2:mem:revision-" + variant, "--urlshort.rate-limit.create-per-minute=1000000",
                "--server.tomcat.remoteip.remote-ip-header=" + remote, "--server.tomcat.remoteip.protocol-header=" + protocol));
            boolean closed = variant.equals("custom-header") || variant.equals("both");
            try (ConfigurableApplicationContext app = new SpringApplicationBuilder(UrlshortApplication.class).run(options.toArray(String[]::new))) {
                String base = "http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port");
                var created = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/links")).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                        "{\"url\":\"https://example.com/?review=AUDIT-REVISION-CANARY\"}")).build(), HttpResponse.BodyHandlers.ofString());
                if (created.statusCode() != 201) throw new AssertionError(created.body());
                for (String method : List.of("GET", "HEAD")) for (String header : List.of("none", "X-Real-IP", "X-Forwarded-For", "Forwarded")) {
                    var request = HttpRequest.newBuilder(URI.create(base + "/api/audit")).timeout(Duration.ofSeconds(5))
                        .header("Accept", "text/html").method(method, HttpRequest.BodyPublishers.noBody());
                    if (!header.equals("none")) request.header(header, header.equals("Forwarded") ? "for=127.0.0.2" : "127.0.0.2");
                    var response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
                    int expected = closed || header.equals("X-Forwarded-For") || header.equals("Forwarded") ? 403 : 200;
                    String type = response.headers().firstValue("Content-Type").orElse("");
                    if (response.statusCode() != expected || !type.equals(expected == 200 ? "application/json" : "application/problem+json")
                        || response.headers().firstValue("X-Request-Id").isEmpty()
                        || (method.equals("HEAD") && !response.body().isEmpty())
                        || (expected == 403 && (response.body().contains("AUDIT-REVISION-CANARY") || response.body().contains("items")))
                        || (expected == 200 && method.equals("GET") && !response.body().contains("AUDIT-REVISION-CANARY"))) {
                        throw new AssertionError(variant + " " + method + " " + header + " " + response.statusCode() + " " + response.body());
                    }
                    checks++;
                    System.out.println("PASS " + variant + " " + method + " " + header + " -> " + expected + " type=" + type);
                }
            }
        }
        System.out.println("REVISION PASS assertions=" + checks + "; Boot bytecode retained at " + bytecode);
        System.exit(0);
    }
}
