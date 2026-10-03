import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import dev.urlshort.UrlshortApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ConfigurableApplicationContext;

/** Independent controls using the actual candidate classes, no replacement controller. */
public class AuditCodeControls {
    static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static void main(String[] args) throws Exception {
        for (String variant : List.of("default", "remote-ip-header", "protocol-header", "native", "framework")) {
            var options = new ArrayList<>(List.of("--server.address=127.0.0.1", "--server.port=0",
                "--logging.level.root=warn", "--spring.datasource.url=jdbc:h2:mem:review-code-" + variant,
                "--urlshort.rate-limit.create-per-minute=1000000"));
            if (variant.equals("remote-ip-header")) options.add("--server.tomcat.remoteip.remote-ip-header=X-Forwarded-For");
            if (variant.equals("protocol-header")) options.add("--server.tomcat.remoteip.protocol-header=X-Forwarded-Proto");
            if (variant.equals("native") || variant.equals("framework")) options.add("--server.forward-headers-strategy=" + variant);
            try (ConfigurableApplicationContext app = new SpringApplicationBuilder(UrlshortApplication.class).run(options.toArray(String[]::new))) {
                String base = "http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port");
                var create = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/links"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                        "{\"url\":\"https://example.com/?review=AUDIT-CODE-CANARY\"}")).build(), HttpResponse.BodyHandlers.ofString());
                if (create.statusCode() != 201) throw new AssertionError(create.body());
                System.out.println("CONTROL variant=" + variant + " effective=" + app.getBean(ServerProperties.class).getForwardHeadersStrategy());
                for (String method : List.of("GET", "HEAD")) for (String xff : List.of("absent", "127.0.0.2", "198.51.100.9")) {
                    var req = HttpRequest.newBuilder(URI.create(base + "/api/audit")).method(method, HttpRequest.BodyPublishers.noBody());
                    if (!xff.equals("absent")) req.header("X-Forwarded-For", xff);
                    var response = CLIENT.send(req.build(), HttpResponse.BodyHandlers.ofString());
                    System.out.println("CONTROL variant=" + variant + " method=" + method + " xff=" + xff + " status=" + response.statusCode()
                        + " body=" + response.body());
                }
            }
        }
        System.out.println("CONTROL complete (observations, not an all-pass assertion)");
        System.exit(0);
    }
}
