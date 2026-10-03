import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.ConfigurableApplicationContext;

/** Independent assertions using the handed design's exact bean fixture and HTTP launcher. */
public class DesignControls {
    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    static Map<String, Object> json(String body) throws Exception {
        return DogfoodProbe.JSON.readValue(body, Map.class);
    }

    static List<Map<String, Object>> bodies(String base) throws Exception {
        String code = (String) json(DogfoodProbe.post(base + "/api/links",
                "{\"url\":\"https://example.org/control\"}", "review-key").body()).get("code");
        var retired = DogfoodProbe.CLIENT.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(base + "/api/links/" + code)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        require(retired.statusCode() == 204, "retire fixture");
        var responses = new ArrayList<HttpResponse<String>>();
        responses.add(DogfoodProbe.post(base + "/api/links", "{\"url\":\"ftp://x/\"}", null));
        responses.add(DogfoodProbe.post(base + "/api/links", "{\"url\":\"https://example.org/other\"}", "review-key"));
        responses.add(DogfoodProbe.get(base + "/api/links/nosuch12"));
        responses.add(DogfoodProbe.get(base + "/" + code));
        for (int i = 0; i < 10; i++) {
            var response = DogfoodProbe.post(base + "/api/links", "{\"url\":\"https://example.org/limit\"}", null);
            if (response.statusCode() == 429) { responses.add(response); break; }
        }
        require(responses.size() == 5, "five pre-merge responses obtained");
        var result = new ArrayList<Map<String, Object>>();
        int[] statuses = {400, 422, 404, 410, 429};
        for (int i = 0; i < responses.size(); i++) {
            var response = responses.get(i);
            require(response.statusCode() == statuses[i], "expected status " + statuses[i]);
            require(response.headers().firstValue("content-type").orElse("").equals("application/problem+json"), "content type");
            var body = json(response.body());
            String instance = (String) body.remove("instance");
            require(instance.equals("urn:uuid:" + response.headers().firstValue("X-Request-Id").orElseThrow()), "request correlation");
            result.add(body);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        List<Map<String, Object>> before;
        Map<String, Object> beforeDocument;
        String pathSelector;
        try (ConfigurableApplicationContext context = DogfoodProbe.start(false)) {
            String base = DogfoodProbe.base(context);
            before = bodies(base);
            beforeDocument = json(DogfoodProbe.get(base + "/v3/api-docs").body());
            var metric = DogfoodProbe.JSON.readTree(DogfoodProbe.get(base + "/actuator/metrics/disk.free").body());
            String path = metric.at("/availableTags/0/values/0").asString();
            pathSelector = "?tag=" + URLEncoder.encode("path:" + path, StandardCharsets.UTF_8);
            require(DogfoodProbe.get(base + "/actuator/metrics/disk.free" + pathSelector).statusCode() == 200, "old selector matches before");
        }
        try (ConfigurableApplicationContext context = DogfoodProbe.start(true)) {
            String base = DogfoodProbe.base(context);
            var after = bodies(base);
            require(before.equals(after), "full normalized problem values unchanged");
            System.out.println("WIRE full JSON values equal after removing only instance: " + after);
            var afterDocument = json(DogfoodProbe.get(base + "/v3/api-docs").body());
            var beforeSchemas = (Map<String, Object>) ((Map<?, ?>) beforeDocument.get("components")).get("schemas");
            var afterSchemas = (Map<String, Object>) ((Map<?, ?>) afterDocument.get("components")).get("schemas");
            var item = (Map<?, ?>) afterSchemas.remove("ProblemFieldError");
            require(((Map<?, ?>) item.get("properties")).keySet().equals(Set.of("field", "rule", "message")), "exact documented item fields");
            require(Set.copyOf((List<?>) item.get("required")).equals(Set.of("field", "rule", "message")), "required item fields");
            beforeSchemas.remove("ProblemDetail");
            afterSchemas.remove("ProblemDetail");
            require(beforeDocument.equals(afterDocument), "whole document delta limited to two named schemas");
            System.out.println("DOCUMENT all other content equal");
            for (String name : List.of("disk.free", "disk.total")) {
                var response = DogfoodProbe.get(base + "/actuator/metrics/" + name);
                require(response.statusCode() == 200, "gauge endpoint remains");
                var metric = DogfoodProbe.JSON.readTree(response.body());
                require(metric.get("availableTags").isEmpty(), "gauge path absent");
                require(metric.at("/measurements/0/value").asDouble() >= 0, "gauge numeric value remains");
            }
            String scrape = DogfoodProbe.get(base + "/actuator/prometheus").body();
            require(scrape.contains("disk_free_bytes ") && scrape.contains("disk_total_bytes ") && !scrape.contains("path=\""), "scrape gauges present without path");
            int filtered = DogfoodProbe.get(base + "/actuator/metrics/disk.free" + pathSelector).statusCode();
            require(filtered == 404, "old path selector no longer matches");
            System.out.println("METRICS both gauges present without path; old tag selector before=200 after=" + filtered);
        }
        var mutated = new LinkedHashMap<>(before.getFirst());
        mutated.put("title", "wrong title");
        require(mutated.keySet().equals(before.getFirst().keySet()), "shape comparison misses changed value");
        require(!mutated.equals(before.getFirst()), "full-value comparison detects changed title");
        var registry = new SimpleMeterRegistry();
        try {
            registry.config().meterFilter(new DogfoodProbe.FixConfig().diskGaugesWithoutPath());
            registry.gauge("disk.free", List.of(io.micrometer.core.instrument.Tag.of("path", "/review-canary"),
                    io.micrometer.core.instrument.Tag.of("other", "kept")), 7);
            var gauge = registry.get("disk.free").gauge();
            require(gauge.value() == 7 && gauge.getId().getTags().equals(List.of(io.micrometer.core.instrument.Tag.of("other", "kept"))), "filter preserves other tag/value");
        } finally { registry.close(); }
        System.out.println("NEGATIVE CONTROL shape-only misses changed title; exact comparison detects it. FILTER other tag/value preserved. PASS");
        System.exit(0);
    }
}
