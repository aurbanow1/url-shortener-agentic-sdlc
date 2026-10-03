package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import io.micrometer.core.instrument.config.MeterFilter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Design probe for 03-dogfood-fix (design.md section 12, rows T1-T5), run in the functional suite's own
 * set-up (@SpringBootTest + MockMvc, profile functional). The assertions are the ones design.md section 7
 * gives the builder. The outer class is the shipped service: they must fail there, with messages naming
 * errors, properties and path (AC-5, AC-6 test first). Fixed imports the two fixes of design.md section 1
 * and they must pass. OverTheCreateBudget is the nested low-budget context that produces AC-2's 429;
 * ShippedScrape and FixedScrape are the nested metrics-export contexts that serve the scrape.
 * Copied into no source set; compiled and run by design-test-probe.gradle.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DogfoodRegressionProbeTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JsonMapper jsonMapper;

	@Autowired
	org.springframework.context.ApplicationContext context;

	JsonNode document;

	@BeforeEach
	void fetchDocument() throws Exception {
		document = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());
	}

	@Test
	void T1_shippedScrapeUnderMockMvcWithoutAutoConfigureMetrics() throws Exception {
		System.out.println("T1 /actuator/prometheus status in the plain functional context: "
				+ mockMvc.perform(get("/actuator/prometheus")).andReturn().getResponse().getStatus());
	}

	@Test
	void T2_shippedFailsAc1() {
		assertProblemSchemaMatchesTheWire(document);
	}

	@Test
	void T2_shippedFailsAc6() throws Exception {
		assertDiskGaugesCarryNoPath(mockMvc, jsonMapper);
	}

	@Nested
	@AutoConfigureMetrics
	class ShippedScrape {

		@Test
		void T5_shippedFailsAc6OnTheScrape() throws Exception {
			assertScrapeCarriesNoPath(mockMvc);
		}
	}

	@Nested
	@Import(DogfoodFixProbeConfig.class)
	class Fixed {

		// The property only makes this context's cache key unique. Without it, the first full run reused
		// ShippedScrape's context here (the inherited @Import did not reach the key), so the scrape showed
		// the path; run alone, the same class had the filter bean and a clean scrape.
		@Nested
		@AutoConfigureMetrics
		@TestPropertySource(properties = "probe.context=fixed-scrape")
		class FixedScrape {

			@Test
			void T5_fixedPassesAc6OnTheScrape() throws Exception {
				System.out.println("T5 fixed-scrape context has the filter bean: " + context.containsBean("diskGaugesWithoutPath")
						+ ", the customiser bean: " + context.containsBean("problemSchemaMatchesTheWire") + ", registries: "
						+ context.getBeansOfType(io.micrometer.core.instrument.MeterRegistry.class).keySet()
						+ ", disk.free tags: " + jsonMapper.readTree(mockMvc.perform(get("/actuator/metrics/disk.free")).andReturn()
								.getResponse().getContentAsString()).get("availableTags"));
				assertScrapeCarriesNoPath(mockMvc);
			}
		}

		@Test
		void T3_fixedPassesAc1() {
			assertProblemSchemaMatchesTheWire(document);
		}

		@Test
		void T3_fixedPassesAc2WithoutThe429() throws Exception {
			String code = jsonMapper.readTree(create("{\"url\":\"https://example.org/t3\"}", "K-t3", "127.0.0.1")
					.getContentAsString()).get("code").asString();
			mockMvc.perform(delete("/api/links/" + code));
			assertBodyConformsToTheSchema(document, create("{\"url\":\"ftp://x/\"}", null, "127.0.0.1"), 400, true);
			assertBodyConformsToTheSchema(document, create("{\"url\":\"https://example.org/other\"}", "K-t3", "127.0.0.1"), 422, true);
			assertBodyConformsToTheSchema(document, mockMvc.perform(get("/api/links/nosuch12")).andReturn().getResponse(), 404, false);
			assertBodyConformsToTheSchema(document, mockMvc.perform(get("/" + code)).andReturn().getResponse(), 410, false);
		}

		@Test
		void T3_fixedPassesAc6() throws Exception {
			assertDiskGaugesCarryNoPath(mockMvc, jsonMapper);
		}

		@Nested
		@TestPropertySource(properties = "urlshort.rate-limit.create-per-minute=1")
		class OverTheCreateBudget {

			@Test
			void T4_the429ConformsToo() throws Exception {
				assertThat(create("{\"url\":\"https://example.org/t4\"}", null, "10.99.0.4").getStatus()).isEqualTo(201);
				assertBodyConformsToTheSchema(document, create("{\"url\":\"https://example.org/t4b\"}", null, "10.99.0.4"), 429, false);
				System.out.println("T4 429 produced in the nested low-budget context; outer document fetched: "
						+ document.at("/components/schemas/ProblemDetail/properties").propertyNames());
			}
		}
	}

	MockHttpServletResponse create(String json, String key, String peer) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json)
				.with(r -> {
					r.setRemoteAddr(peer);
					return r;
				});
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return mockMvc.perform(request).andReturn().getResponse();
	}

	/** AC-1, as design.md section 9 words it. */
	static void assertProblemSchemaMatchesTheWire(JsonNode document) {
		JsonNode problem = document.at("/components/schemas/ProblemDetail");
		assertThat(new TreeSet<>(problem.get("properties").propertyNames()))
				.as("ProblemDetail documents the errors member the service sends and no properties member")
				.containsExactlyInAnyOrder("detail", "errors", "instance", "status", "title", "type");
		assertThat(problem.path("required").isMissingNode() || !problem.get("required").toString().contains("errors"))
				.as("errors is optional").isTrue();
		assertThat(problem.at("/properties/errors/type").asString()).as("errors is an array").isEqualTo("array");
		String ref = problem.at("/properties/errors/items/$ref").asString();
		JsonNode item = document.at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1));
		assertThat(new TreeSet<>(item.get("properties").propertyNames())).as("errors item members")
				.containsExactly("field", "message", "rule");
		item.get("properties").forEach(member -> assertThat(member.get("type").asString()).isEqualTo("string"));
		assertThat(new TreeSet<>(item.get("required").valueStream().map(JsonNode::asString).toList()))
				.as("errors item required members").containsExactly("field", "message", "rule");
		assertThat(problem.at("/properties/status/type").asString()).isEqualTo("integer");
		assertThat(problem.at("/properties/title/type").asString()).isEqualTo("string");
		assertThat(problem.at("/properties/detail/type").asString()).isEqualTo("string");
		assertThat(problem.at("/properties/type/format").asString()).isEqualTo("uri");
		assertThat(problem.at("/properties/instance/format").asString()).isEqualTo("uri");
	}

	/** AC-2's "validates against the schema", operationally: members documented, JSON types as documented, errors items exact. */
	static void assertBodyConformsToTheSchema(JsonNode document, MockHttpServletResponse response, int status, boolean withErrors)
			throws Exception {
		assertThat(response.getStatus()).isEqualTo(status);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		JsonNode body = JsonMapper.builder().build().readTree(response.getContentAsString());
		JsonNode documented = document.at("/components/schemas/ProblemDetail/properties");
		for (Map.Entry<String, JsonNode> member : body.properties()) {
			assertThat(documented.has(member.getKey())).as("%d member %s is documented", status, member.getKey()).isTrue();
			String type = documented.get(member.getKey()).get("type").asString();
			JsonNode value = member.getValue();
			assertThat(type.equals("string") && value.isString() || type.equals("integer") && value.isIntegralNumber()
					|| type.equals("array") && value.isArray()).as("%d member %s is a %s", status, member.getKey(), type).isTrue();
		}
		assertThat(body.has("errors")).as("%d carries errors", status).isEqualTo(withErrors);
		if (withErrors) {
			assertThat(body.get("errors").size()).isEqualTo(1);
			JsonNode error = body.get("errors").get(0);
			assertThat(new TreeSet<>(error.propertyNames())).containsExactly("field", "message", "rule");
			error.forEach(value -> assertThat(value.isString()).isTrue());
		}
		System.out.println("T3/T4 " + status + " members " + new TreeSet<>(body.propertyNames()) + " conform");
	}

	/** AC-6 on the Prometheus scrape, served only where metrics export is auto-configured (T1). */
	static void assertScrapeCarriesNoPath(MockMvc mockMvc) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/actuator/prometheus")).andReturn().getResponse();
		assertThat(response.getStatus()).as("scrape status").isEqualTo(200);
		List<String> disk = response.getContentAsString().lines().filter(line -> line.startsWith("disk_")).toList();
		System.out.println("T5 scrape disk samples: " + disk);
		assertThat(disk).as("disk free and total gauges").anyMatch(line -> line.startsWith("disk_free_bytes"))
				.anyMatch(line -> line.startsWith("disk_total_bytes"));
		assertThat(response.getContentAsString()).as("the scrape carries no path label")
				.doesNotContain("path=\"").doesNotContain(Path.of("").toAbsolutePath().toString());
	}

	/** AC-6 on the surface the plain functional context serves. */
	static void assertDiskGaugesCarryNoPath(MockMvc mockMvc, JsonMapper jsonMapper) throws Exception {
		String workingDirectory = Path.of("").toAbsolutePath().toString();
		for (String name : List.of("disk.free", "disk.total")) {
			MockHttpServletResponse response = mockMvc.perform(get("/actuator/metrics/" + name)).andReturn().getResponse();
			assertThat(response.getStatus()).as(name).isEqualTo(200);
			String body = response.getContentAsString();
			JsonNode metric = jsonMapper.readTree(body);
			List<String> tags = new ArrayList<>();
			metric.get("availableTags").forEach(tag -> tags.add(tag.get("tag").asString()));
			assertThat(tags).as("%s carries no path tag", name).doesNotContain("path");
			assertThat(body).as("%s does not disclose the working directory", name).doesNotContain(workingDirectory);
			assertThat(metric.at("/measurements/0/value").asDouble()).as("%s still has a value", name).isPositive();
		}
	}
}

/**
 * The two fixes exactly as design.md section 1 gives them. Top level on purpose: a static nested
 * {@code @TestConfiguration} of a {@code @SpringBootTest} class is added to that class's context
 * automatically, which put the fixes into the "shipped" context in this probe's first run.
 */
@TestConfiguration(proxyBeanMethods = false)
class DogfoodFixProbeConfig {

		@Bean
		OpenApiCustomizer problemSchemaMatchesTheWire() {
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
		MeterFilter diskGaugesWithoutPath() {
			return MeterFilter.ignoreTags("path");
		}
}
