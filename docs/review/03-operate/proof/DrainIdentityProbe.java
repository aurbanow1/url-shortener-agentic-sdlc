import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import dev.urlshort.UrlshortApplication;

/** Review-only test of the design's completion-id reconciliation, including a known dispatched loss. */
public class DrainIdentityProbe {
    static volatile CountDownLatch entered;
    static volatile String heldId;

    public static void main(String[] args) throws Exception {
        run(false);
        run(true);
    }

    static void run(boolean abort) throws Exception {
        entered = new CountDownLatch(1);
        heldId = null;
        var ctx = new SpringApplicationBuilder(UrlshortApplication.class, ProbeConfig.class).run(
                "--server.port=0", "--server.address=127.0.0.1", "--spring.profiles.active=functional",
                "--spring.lifecycle.timeout-per-shutdown-phase=10s");
        var logged = ConcurrentHashMap.<String>newKeySet();
        var received = new HashSet<String>();
        var logger = (Logger) LoggerFactory.getLogger("dev.urlshort.web.RequestIdFilter");
        var collector = new AppenderBase<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) {
                if (event.getFormattedMessage().equals("request completed")) {
                    logged.add(event.getMDCPropertyMap().get("requestId"));
                }
            }
        };
        collector.start();
        logger.addAppender(collector);
        int port = Integer.parseInt(ctx.getEnvironment().getProperty("local.server.port"));
        try (var client = HttpClient.newHttpClient(); var held = new Socket("127.0.0.1", port)) {
            held.setSoTimeout(15_000);
            var ready = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                    + "/actuator/health")).build(), HttpResponse.BodyHandlers.ofString());
            require(ready.statusCode() == 200, "readiness response");
            received.add(ready.headers().firstValue("X-Request-Id").orElseThrow());
            String body = "{\"url\":\"https://example.com/drain-id\"}";
            held.getOutputStream().write(("POST /api/links HTTP/1.1\r\nHost: localhost\r\n"
                    + "Content-Type: application/json\r\nContent-Length: " + body.length()
                    + "\r\nConnection: close\r\n\r\n" + body.substring(0, 10)).getBytes(StandardCharsets.US_ASCII));
            require(entered.await(5, TimeUnit.SECONDS), "held request dispatched");
            long stop = System.nanoTime();
            var closer = new Thread(ctx::close);
            closer.start();
            Thread.sleep(500);
            boolean refused = false;
            try (var probe = new Socket("127.0.0.1", port)) { }
            catch (ConnectException expected) { refused = true; }
            require(refused, "new connection refused");
            if (abort) {
                held.setSoLinger(true, 0);
                held.close();
            }
            else {
                held.getOutputStream().write(body.substring(10).getBytes(StandardCharsets.US_ASCII));
                String response = new String(held.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
                require(response.startsWith("HTTP/1.1 201"), "held response 201");
                require(complete(response), "complete held response framing");
                require(response.contains("https://example.com/drain-id"), "held response body");
                received.add(header(response, "x-request-id"));
            }
            closer.join(12_000);
            require(!closer.isAlive(), "context closed");
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stop);
            require(elapsed < 10_000, "phase under ten seconds");
            var missing = new HashSet<>(logged);
            missing.removeAll(received);
            require(logged.size() == 2, "readiness and held request both logged");
            require(abort ? missing.equals(Set.of(heldId)) : missing.isEmpty(), "exact loss classification");
            System.out.println("RECONCILE abort=" + abort + " logged=" + logged + " received=" + received
                    + " missing=" + missing + " heldId=" + heldId + " elapsedMs=" + elapsed
                    + " refused=" + refused + " classification=" + (abort ? "dispatched-loss" : "no-loss"));
        }
        finally {
            ctx.close();
            logger.detachAppender(collector);
            collector.stop();
        }
    }

    static String header(String response, String name) {
        return response.lines().takeWhile(s -> !s.isEmpty()).filter(s -> s.toLowerCase().startsWith(name + ":"))
                .map(s -> s.substring(s.indexOf(':') + 1).trim()).findFirst().orElse("");
    }

    static boolean complete(String response) {
        int start = response.indexOf("\r\n\r\n") + 4;
        String length = header(response, "content-length");
        if (!length.isEmpty()) return response.length() - start == Integer.parseInt(length);
        if (!header(response, "transfer-encoding").equalsIgnoreCase("chunked")) return false;
        int pos = start;
        while (pos < response.length()) {
            int end = response.indexOf("\r\n", pos);
            if (end < 0) return false;
            int size = Integer.parseInt(response.substring(pos, end).split(";", 2)[0], 16);
            pos = end + 2;
            if (size == 0) return response.substring(pos).equals("\r\n");
            pos += size;
            if (!response.startsWith("\r\n", pos)) return false;
            pos += 2;
        }
        return false;
    }

    static void require(boolean test, String message) {
        if (!test) throw new AssertionError(message);
    }

    @Configuration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean HeldEntry heldEntry() { return new HeldEntry(); }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    static class HeldEntry extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            if (request.getRequestURI().equals("/api/links")) {
                heldId = MDC.get("requestId");
                entered.countDown();
            }
            chain.doFilter(request, response);
        }
    }
}
