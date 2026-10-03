import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import dev.urlshort.UrlshortApplication;

/** Review-only observation of the design's exact lookup helper against real MVC routing. */
public class PathReviewProbe {
    public static void main(String[] args) throws Exception {
        try (var ctx = new SpringApplicationBuilder(UrlshortApplication.class, Config.class).run(
                "--server.port=0", "--server.address=127.0.0.1",
                "--logging.level.org.apache.coyote.http11.Http11Processor=warn",
                "--logging.level.org.springframework.web.servlet.resource.ResourceHandlerUtils=" + (args.length > 0 ? "error" : "info"))) {
            int port = ctx.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
            for (String path : List.of("/api/links", "/%61pi/links", "/a%70i/links", "/api;/links",
                    "/api/links;x=1", "//api/links", "/actuator/../api/links", "/actuator/..;/api/links",
                    "/actuator;%2f../api/links", "/actuator%2f../api/links", "/api%2flinks",
                    "/%2561pi/links", "/api%3b/links", "/api;/%2e%2e/api/links", "/api/%FF",
                    "/api/%", "/actuator/..%2fapi/links", "/actuator/../api/ping",
                    "/actuator/../pii-canary-10.77.77.77")) {
                String body = "{\"url\":\"https://example.com/path-review\"}";
                String response;
                try (Socket s = new Socket("127.0.0.1", port)) {
                    s.setSoTimeout(5000);
                    s.getOutputStream().write(("POST " + path + " HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: "
                            + body.length() + "\r\nConnection: close\r\n\r\n" + body).getBytes(StandardCharsets.US_ASCII));
                    response = new String(s.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
                }
                System.out.println("PATH " + path + " => " + response.lines().filter(l -> l.startsWith("HTTP/")
                        || l.toLowerCase().startsWith("x-review-")).toList());
            }
        }
    }
    @Configuration(proxyBeanMethods=false)
    static class Config {
        @Bean PathFilter reviewPathFilter() { return new PathFilter(); }
    }
    @Order(Ordered.HIGHEST_PRECEDENCE+2)
    static class PathFilter extends OncePerRequestFilter {
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(req);
            boolean exempt = path.equals("/actuator") || path.startsWith("/actuator/")
                    || path.startsWith("/v3/api-docs") || path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/");
            String budget = exempt ? "EXEMPT" : path.equals("/api") || path.startsWith("/api/") ? "CREATE" : "REDIRECT";
            res.setHeader("X-Review-Lookup", path);
            res.setHeader("X-Review-Budget", budget);
            chain.doFilter(req, res);
        }
    }
}
