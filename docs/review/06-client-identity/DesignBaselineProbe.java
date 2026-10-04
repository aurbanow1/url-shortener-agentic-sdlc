package dev.urlshort.audit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.mock.web.MockHttpServletRequest;

/** Independent controls against the unchanged implementation; no candidate implementation is loaded. */
public class DesignBaselineProbe {
    private static int checks;

    public static void main(String[] args) throws Exception {
        String p = "10.9.9.9", q = "10.9.9.8", u = "203.0.113.7", v = "203.0.113.8";
        Method client = Class.forName("dev.urlshort.web.RateLimitFilter")
                .getDeclaredMethod("clientOf", String.class, String.class, Set.class);
        client.setAccessible(true);
        String[][] rows = {
            {"", p, u, p}, {p, "10.0.0.5", u, "10.0.0.5"}, {p, p, u, u},
            {p, p, "198.51.100.1, " + u, u}, {p + "," + q, p, v + ", " + u + ", " + q, u},
            {p, p, "  " + u + " ,  ", u}, {p + "," + q, p, p + ", " + q, p},
            {p, p, null, p}, {p, p, "", p}, {p, p, " , ", p},
            {"", "127.0.0.1", null, "127.0.0.1"}, {"", "::1", u, "::1"},
            {p, p, u + ", " + v, v}, {p, p, u + ",,", u}, {p, p, "\t" + u + "\t", u},
            {p, p, "unknown", "unknown"}, {p, p, "[2001:db8::1]:443", "[2001:db8::1]:443"},
            {p, p, "203.0.113.7:8080", "203.0.113.7:8080"},
            {"::1", "0:0:0:0:0:0:0:1", u, "0:0:0:0:0:0:0:1"}
        };
        for (String[] row : rows) {
            Set<String> trusted = new HashSet<>(Arrays.asList(row[0].split(",")));
            trusted.remove("");
            equal(row[3], client.invoke(null, row[1], row[2], trusted), "client rule");
        }
        for (String peer : new String[]{"127.0.0.1", "127.0.0.2", "127.255.255.254", "::1",
                "0:0:0:0:0:0:0:1", "::ffff:127.0.0.1"}) {
            equal(true, AuditController.fromLoopback(request(peer)), "loopback");
        }
        for (String peer : new String[]{"192.0.2.10", "10.0.0.7", "::ffff:192.0.2.10", "fe80::1",
                "", null, "1::2::3"}) {
            equal(false, AuditController.fromLoopback(request(peer)), "nonloopback or invalid");
        }
        for (String peer : new String[]{"127.0.0.1", "192.0.2.10"}) {
            for (String header : new String[]{"X-Forwarded-For", "Forwarded"}) {
                for (String value : new String[]{u, "127.0.0.1", "", "   "}) {
                    var request = request(peer);
                    request.addHeader(header, value);
                    equal(false, AuditController.fromLoopback(request), "header presence");
                }
            }
        }
        Field safe = AuditController.class.getDeclaredField("peerIsConnection");
        safe.setAccessible(true);
        for (ForwardHeadersStrategy strategy : new ForwardHeadersStrategy[]{ForwardHeadersStrategy.NONE,
                ForwardHeadersStrategy.NATIVE, ForwardHeadersStrategy.FRAMEWORK, null}) {
            for (String[] headers : new String[][]{{null, null}, {"", ""}, {"  ", "\t"},
                    {"x-forwarded-for", null}, {null, "x-forwarded-proto"},
                    {"x-forwarded-for", "x-forwarded-proto"}}) {
                var server = new ServerProperties();
                server.setForwardHeadersStrategy(strategy);
                var tomcat = new TomcatServerProperties();
                tomcat.getRemoteip().setRemoteIpHeader(headers[0]);
                tomcat.getRemoteip().setProtocolHeader(headers[1]);
                boolean expected = strategy == ForwardHeadersStrategy.NONE
                        && (headers[0] == null || headers[0].isBlank())
                        && (headers[1] == null || headers[1].isBlank());
                equal(expected, safe.get(new AuditController(null, server, tomcat)), "configuration guard");
            }
        }
        System.out.println("PASS " + checks + " independent baseline controls: 19 resolver, 13 peer, 16 header, 24 configuration");
    }

    private static MockHttpServletRequest request(String peer) {
        var request = new MockHttpServletRequest("GET", "/api/audit");
        request.setRemoteAddr(peer);
        return request;
    }

    private static void equal(Object expected, Object actual, String context) {
        if (!expected.equals(actual)) throw new AssertionError(context + ": expected=" + expected + ", actual=" + actual);
        checks++;
    }
}
