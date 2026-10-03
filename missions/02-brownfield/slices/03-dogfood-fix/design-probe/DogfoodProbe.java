import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.micrometer.core.instrument.config.MeterFilter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import dev.urlshort.UrlshortApplication;

/**
 * Design probe for 03-dogfood-fix (design.md section 12). D1-D3 compare the shipped service with one that
 * carries the two fixes of design.md section 1 (the ProblemDetail schema correction in an OpenApiCustomizer,
 * and a MeterFilter dropping the disk gauges' path tag), registered here as beans. No file under src/ is
 * touched.
 */
public class DogfoodProbe {

	static final JsonMapper JSON = JsonMapper.builder().build();
	static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

	public static void main(String[] args) throws Exception {
		JsonNode shipped;
		List<String> shippedWire;
		try (ConfigurableApplicationContext ctx = start(false)) {
			String base = base(ctx);
			shipped = JSON.readTree(get(base + "/v3/api-docs").body());
			System.out.println("D0 shipped ProblemDetail: " + shipped.at("/components/schemas/ProblemDetail"));
			shippedWire = wire(base);
			shippedWire.forEach(line -> System.out.println("D0 shipped wire " + line));
			scrape("D0 shipped", base);
		}
		try (ConfigurableApplicationContext ctx = start(true)) {
			String base = base(ctx);
			JsonNode fixed = JSON.readTree(get(base + "/v3/api-docs").body());
			JsonNode problem = fixed.at("/components/schemas/ProblemDetail");
			System.out.println("D1 fixed ProblemDetail: " + problem);
			System.out.println("D1 fixed ProblemFieldError: " + fixed.at("/components/schemas/ProblemFieldError"));
			System.out.println("D1 paths unchanged: " + shipped.get("paths").equals(fixed.get("paths")) + "; other schemas unchanged: "
					+ otherSchemasEqual(shipped, fixed) + "; schema names added " + added(shipped, fixed));
			Set<String> documented = new TreeSet<>(problem.get("properties").propertyNames());
			Set<String> itemMembers = new TreeSet<>(fixed.at("/components/schemas/ProblemFieldError/properties").propertyNames());
			List<String> fixedWire = wire(base);
			for (String line : fixedWire) {
				Set<String> members = new TreeSet<>(List.of(line.substring(line.indexOf('[') + 1, line.indexOf(']')).split(", ")));
				boolean itemsDescribed = !line.contains("items [") || itemMembers.containsAll(List.of(
						line.substring(line.lastIndexOf('[') + 1, line.lastIndexOf(']')).split(", ")));
				System.out.println("D2 " + line + "; members documented " + documented.containsAll(members) + ", items documented " + itemsDescribed);
			}
			System.out.println("D2 wire unchanged (status, content type, members, error items): " + shippedWire.equals(fixedWire));
			scrape("D3 fixed", base);
		}
		System.out.println("PROBE done");
		System.exit(0);
	}

	/** AC-2's five pre-merge requests (the audit read is not on this base); one line per response, per-request values left out. */
	static List<String> wire(String base) throws Exception {
		String code = JSON.readTree(post(base + "/api/links", "{\"url\":\"https://example.org/d\"}", "K-probe").body()).get("code").asString();
		CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/links/" + code)).DELETE().build(), HttpResponse.BodyHandlers.discarding());
		List<HttpResponse<String>> cases = new ArrayList<>();
		cases.add(post(base + "/api/links", "{\"url\":\"ftp://x/\"}", null));
		cases.add(post(base + "/api/links", "{\"url\":\"https://example.org/other\"}", "K-probe"));
		cases.add(get(base + "/api/links/nosuch12"));
		cases.add(get(base + "/" + code));
		HttpResponse<String> limited = null;
		for (int i = 0; i < 10 && limited == null; i++) {
			HttpResponse<String> r = post(base + "/api/links", "{\"url\":\"https://example.org/x" + i + "\"}", null);
			if (r.statusCode() == 429) {
				limited = r;
			}
		}
		cases.add(limited);
		List<String> lines = new ArrayList<>();
		for (HttpResponse<String> r : cases) {
			JsonNode body = JSON.readTree(r.body());
			String errors = body.has("errors") ? " errors " + body.get("errors").size() + " items "
					+ new TreeSet<>(body.get("errors").get(0).propertyNames()) : " no errors";
			lines.add(r.statusCode() + " " + r.headers().firstValue("Content-Type").orElse("-") + " members "
					+ new TreeSet<>(body.propertyNames()) + errors);
		}
		return lines;
	}

	static void scrape(String label, String base) throws Exception {
		String scrape = get(base + "/actuator/prometheus").body();
		scrape.lines().filter(line -> line.startsWith("disk_")).forEach(line -> System.out.println(label + " scrape: " + line));
		System.out.println(label + " scrape samples with a path label: "
				+ scrape.lines().filter(line -> !line.startsWith("#") && line.contains("path=\"")).count());
		System.out.println(label + " /actuator/metrics/disk.free: " + get(base + "/actuator/metrics/disk.free").body());
	}

	static boolean otherSchemasEqual(JsonNode a, JsonNode b) {
		for (String name : a.get("components").get("schemas").propertyNames()) {
			if (!name.equals("ProblemDetail") && !a.at("/components/schemas/" + name).equals(b.at("/components/schemas/" + name))) {
				return false;
			}
		}
		return true;
	}

	static Set<String> added(JsonNode a, JsonNode b) {
		Set<String> names = new TreeSet<>(b.get("components").get("schemas").propertyNames());
		names.removeAll(new TreeSet<>(a.get("components").get("schemas").propertyNames()));
		return names;
	}

	static ConfigurableApplicationContext start(boolean fixed) {
		List<String> args = List.of("--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=warn",
				"--urlshort.rate-limit.create-per-minute=5", "--spring.datasource.url=jdbc:h2:mem:dogfood-" + fixed + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
		Class<?>[] sources = fixed ? new Class<?>[] { UrlshortApplication.class, FixConfig.class } : new Class<?>[] { UrlshortApplication.class };
		return new SpringApplicationBuilder(sources).run(args.toArray(String[]::new));
	}

	/** The two fixes exactly as design.md section 1 specifies them. */
	@Configuration(proxyBeanMethods = false)
	public static class FixConfig {

		@Bean
		public OpenApiCustomizer problemSchemaMatchesTheWire() {
			return openApi -> {
				Schema<?> problem = openApi.getComponents().getSchemas().get("ProblemDetail");
				problem.getProperties().remove("properties");
				problem.addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/ProblemFieldError"))
						.description("Present on 400 validation and 422 idempotency-mismatch problems only"));
				openApi.getComponents().addSchemas("ProblemFieldError", new ObjectSchema()
						.addProperty("field", new StringSchema()).addProperty("rule", new StringSchema())
						.addProperty("message", new StringSchema()).required(List.of("field", "rule", "message")));
			};
		}

		@Bean
		public MeterFilter diskGaugesWithoutPath() {
			return MeterFilter.ignoreTags("path");
		}
	}

	static String base(ConfigurableApplicationContext ctx) {
		return "http://127.0.0.1:" + ctx.getEnvironment().getRequiredProperty("local.server.port");
	}

	static HttpResponse<String> get(String uri) throws Exception {
		return CLIENT.send(HttpRequest.newBuilder(URI.create(uri)).build(), HttpResponse.BodyHandlers.ofString());
	}

	static HttpResponse<String> post(String uri, String body, String key) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(uri)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body));
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
