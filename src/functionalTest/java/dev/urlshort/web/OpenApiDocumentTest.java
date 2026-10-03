package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-28 (slice 01), AC-20 (slice 03: the {@code 429} on every operation) and NFR-M3: the live API
 * document describes the service, and the committed
 * {@code docs/api/openapi.json} equals it. Every run writes the live document, key-sorted and
 * indented, to {@code build/openapi/openapi.json}; when the API changes, regenerate with
 * {@code scripts/gw functionalTest --tests '*OpenApiDocumentTest*'} and
 * {@code cp build/openapi/openapi.json docs/api/openapi.json}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentTest {

	private static final Path COMMITTED = Path.of("docs/api/openapi.json");
	private static final Path GENERATED = Path.of("build/openapi/openapi.json");
	private static final String PROBLEM_JSON = "application/problem+json";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	private JsonNode document;

	@BeforeEach
	void fetchAndExport() throws Exception {
		String live = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		Map<?, ?> tree = jsonMapper.readValue(live, Map.class);
		String sorted = JsonMapper.builder()
				.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, SerializationFeature.INDENT_OUTPUT)
				.build().writeValueAsString(tree) + "\n";
		Files.createDirectories(GENERATED.getParent());
		Files.writeString(GENERATED, sorted);
		document = jsonMapper.readTree(live);
	}

	@Test
	void NFRM3_committedDocumentEqualsTheLiveOne() throws Exception {
		assertThat(Files.exists(COMMITTED)).as("docs/api/openapi.json is committed; generate it with "
				+ "cp build/openapi/openapi.json docs/api/openapi.json").isTrue();
		assertThat(Files.readString(COMMITTED)).as("docs/api/openapi.json drifted from the live document; "
				+ "regenerate with cp build/openapi/openapi.json docs/api/openapi.json")
				.isEqualTo(Files.readString(GENERATED));
	}

	@Test
	void AC28_liveDocumentDescribesTheSlice() {
		JsonNode paths = document.get("paths");
		assertThat(paths.propertyNames()).containsExactlyInAnyOrder("/api/ping", "/api/links", "/api/links/{code}",
				"/{code}", "/api/links/{code}/stats", "/api/audit");
		assertThat(paths.get("/api/ping").has("get")).isTrue();
		assertThat(paths.get("/api/links").propertyNames()).containsExactly("post");
		assertThat(paths.get("/api/links/{code}").propertyNames()).containsExactlyInAnyOrder("get", "delete");
		assertThat(paths.get("/{code}").propertyNames()).containsExactly("get");

		JsonNode create = paths.get("/api/links").get("post");
		assertThat(create.get("responses").propertyNames()).contains("201", "400", "413", "415", "422");
		JsonNode read = paths.get("/api/links/{code}").get("get");
		assertThat(read.get("responses").propertyNames()).contains("200", "404");
		JsonNode retire = paths.get("/api/links/{code}").get("delete");
		assertThat(retire.get("responses").propertyNames()).contains("204", "404", "410");
		JsonNode redirect = paths.get("/{code}").get("get");
		assertThat(redirect.get("responses").propertyNames()).contains("302", "404", "410");
		assertThat(redirect.get("responses").get("302").get("headers").has("Location")).isTrue();

		for (JsonNode operation : new JsonNode[] { create, read, retire, redirect }) {
			for (String code : operation.get("responses").propertyNames()) {
				if (code.startsWith("4") || code.startsWith("5")) {
					assertThat(operation.get("responses").get(code).get("content").propertyNames())
							.as("%s %s", operation.get("operationId"), code).containsExactly(PROBLEM_JSON);
				}
			}
		}
		assertThat(create.at("/requestBody/content/application~1json").has("examples")
				|| create.at("/requestBody/content/application~1json").has("example")).isTrue();
		assertThat(hasExample(create.at("/responses/201/content/application~1json"))).isTrue();
		assertThat(hasExample(read.at("/responses/200/content/application~1json"))).isTrue();
	}

	@Test
	void AC20_everyOperationDocumentsTheTooManyRequestsProblem() {
		int operations = 0;
		for (JsonNode path : document.get("paths")) {
			for (JsonNode operation : path) {
				operations++;
				JsonNode tooMany = operation.at("/responses/429");
				String id = operation.get("operationId").asString();
				assertThat(tooMany.isMissingNode()).as("%s documents 429", id).isFalse();
				assertThat(tooMany.get("content").propertyNames()).as(id).containsExactly(PROBLEM_JSON);
				assertThat(hasExample(tooMany.at("/content/application~1problem+json"))).as(id).isTrue();
				assertThat(tooMany.at("/headers/Retry-After/schema/type").asString()).as(id).isEqualTo("integer");
			}
		}
		assertThat(operations).as("ping, create, read, retire, redirect, statistics, audit").isEqualTo(7);
	}

	private static boolean hasExample(JsonNode mediaType) {
		return mediaType.has("examples") || mediaType.has("example");
	}

	// ------------------------------------------------------------------ slice 03-dogfood-fix, W2-01

	/** AC-1: the one problem schema documents the {@code errors} the service sends, and no {@code properties}. */
	@Test
	void AC1_problemSchemaDocumentsErrorsAndNoProperties() {
		JsonNode problem = document.at("/components/schemas/ProblemDetail");
		assertThat(new TreeSet<>(problem.get("properties").propertyNames()))
				.as("ProblemDetail documents the errors member the service sends and no properties member")
				.containsExactly("detail", "errors", "instance", "status", "title", "type");
		assertThat(problem.path("required").toString()).as("errors is optional").doesNotContain("errors");
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

	/** AC-2: problem bodies of both shapes, from three controllers and the audit read, conform to that schema. */
	@Test
	void AC2_problemBodiesConformToTheDocumentedSchema() throws Exception {
		String key = "key-" + UUID.randomUUID();
		String code = jsonMapper.readTree(create("https://example.com/conform", key, "127.0.0.1").getContentAsString())
				.get("code").asString();
		mockMvc.perform(delete("/api/links/" + code));

		assertConformsToProblemSchema(create("ftp://x/", null, "127.0.0.1"), 400, true);
		assertConformsToProblemSchema(create("https://example.com/other", key, "127.0.0.1"), 422, true);
		assertConformsToProblemSchema(mockMvc.perform(get("/api/links/nosuch12")).andReturn().getResponse(), 404, false);
		assertConformsToProblemSchema(mockMvc.perform(get("/" + code)).andReturn().getResponse(), 410, false);
		assertConformsToProblemSchema(mockMvc.perform(get("/api/audit?limit=0")).andReturn().getResponse(), 400, true);
	}

	/** AC-2's {@code 429}: the functional overlay's budgets never refuse, so this context allows one create. */
	@Nested
	@TestPropertySource(properties = "urlshort.rate-limit.create-per-minute=1")
	class OverTheCreateBudget {

		@Test
		void AC2_theTooManyRequestsProblemConformsToo() throws Exception {
			assertThat(create("https://example.com/budget", null, "10.88.0.2").getStatus()).isEqualTo(201);

			assertConformsToProblemSchema(create("https://example.com/budget-2", null, "10.88.0.2"), 429, false);
		}
	}

	private MockHttpServletResponse create(String url, @Nullable String key, String peer) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url))).with(r -> {
					r.setRemoteAddr(peer);
					return r;
				});
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return mockMvc.perform(request).andReturn().getResponse();
	}

	/**
	 * "Validates against the schema", operationally: every member is a documented property of
	 * {@code ProblemDetail} with the documented JSON type, and {@code errors}, when present, holds one item
	 * of exactly {@code field}, {@code message} and {@code rule}, all strings.
	 */
	private void assertConformsToProblemSchema(MockHttpServletResponse response, int status, boolean withErrors)
			throws Exception {
		assertThat(response.getStatus()).isEqualTo(status);
		assertThat(response.getContentType()).isEqualTo(PROBLEM_JSON);
		JsonNode body = jsonMapper.readTree(response.getContentAsString());
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
			error.forEach(value -> assertThat(value.isString()).as("%d errors item values are strings", status).isTrue());
		}
	}
}
