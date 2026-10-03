package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
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
				"/{code}", "/api/links/{code}/stats");
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
		assertThat(operations).as("ping, create, read, retire, redirect, statistics").isEqualTo(6);
	}

	private static boolean hasExample(JsonNode mediaType) {
		return mediaType.has("examples") || mediaType.has("example");
	}
}
