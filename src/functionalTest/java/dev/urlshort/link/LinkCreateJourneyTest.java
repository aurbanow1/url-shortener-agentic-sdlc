package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@code POST /api/links}: AC-1 to AC-7 and the AC-16 shadowing guard. */
@SpringBootTest
@AutoConfigureMockMvc
class LinkCreateJourneyTest {

	private static final String PROBLEM_JSON = "application/problem+json";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void AC01_validUrlBecomesAShortLink() throws Exception {
		String url = "https://example.com/some/path?q=1";
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		MvcResult result = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(body(url)))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();
		Instant after = Instant.now();

		JsonNode created = jsonMapper.readTree(result.getResponse().getContentAsString());
		String code = created.get("code").asString();
		assertThat(code).matches("^[A-Za-z0-9]{6,32}$");
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/links/" + code);
		assertThat(created.propertyNames()).containsExactlyInAnyOrder("code", "shortUrl", "url", "state", "createdAt");
		assertThat(created.get("shortUrl").asString()).isEqualTo("http://localhost:8080/" + code);
		assertThat(created.get("url").asString()).isEqualTo(url);
		assertThat(created.get("state").asString()).isEqualTo("active");
		String createdAt = created.get("createdAt").asString();
		assertThat(createdAt).endsWith("Z");
		assertThat(Instant.parse(createdAt).truncatedTo(ChronoUnit.SECONDS)).isBetween(before, after);
	}

	@Test
	void AC02_everyCreateWithoutAKeyIsANewLink() throws Exception {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			codes.add(jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
					.content(body("https://example.com/same")))
					.andExpect(status().isCreated())
					.andReturn().getResponse().getContentAsString()).get("code").asString());
		}

		assertThat(codes).hasSize(10);
	}

	@Test
	void AC03_shortUrlUsesTheShippedBaseNeverTheHostHeader() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.header("Host", "evil.example").header("X-Forwarded-Host", "evil.example")
				.header("X-Forwarded-Proto", "https").header("Forwarded", "host=evil.example")
				.content(body("https://example.com/")))
				.andExpect(status().isCreated())
				.andReturn();

		assertThat(jsonMapper.readTree(result.getResponse().getContentAsString()).get("shortUrl").asString())
				.startsWith("http://localhost:8080/");
		assertThat(result.getResponse().getContentAsString()).doesNotContain("evil.example");
		for (String name : result.getResponse().getHeaderNames()) {
			assertThat(result.getResponse().getHeaders(name)).noneMatch(value -> value.contains("evil.example"));
		}
	}

	static Stream<Arguments> AC04_rejectedTargets() {
		String tooLong = "https://example.com/" + "a".repeat(2049 - "https://example.com/".length());
		return Stream.of(
				Arguments.of("required", null, false),
				Arguments.of("required", null, true),
				Arguments.of("required", "", true),
				Arguments.of("required", "   ", true),
				Arguments.of("too-long", tooLong, true),
				Arguments.of("scheme", "javascript:alert(1)", true),
				Arguments.of("scheme", "data:text/html,hi", true),
				Arguments.of("scheme", "file:///etc/passwd", true),
				Arguments.of("scheme", "ftp://example.com/", true),
				Arguments.of("scheme", "example.com/path", true),
				Arguments.of("scheme", " https://example.com/", true),
				Arguments.of("malformed", "https://", true),
				Arguments.of("malformed", "https://exa mple.com/", true),
				Arguments.of("malformed", "https://[bad/", true),
				Arguments.of("malformed", "https://example.com/ ", true),
				Arguments.of("credentials", "https://user:secret@example.com/", true),
				Arguments.of("credentials", "https://user@example.com/", true));
	}

	@ParameterizedTest(name = "[{index}] {0}: present={2} value=<{1}>")
	@MethodSource("AC04_rejectedTargets")
	void AC04_targetOutsideTheAllowListIsRejectedNamingFieldAndRule(String rule, String url, boolean present)
			throws Exception {
		Map<String, Object> request = new HashMap<>();
		if (present) {
			request.put("url", url);
		}
		long auditRows = auditRowCount();

		MvcResult result = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value("url"))
				.andExpect(jsonPath("$.errors[0].rule").value(rule))
				.andReturn();

		if (url != null && !url.isBlank()) {
			assertThat(result.getResponse().getContentAsString()).doesNotContain(url.strip());
		}
		assertThat(auditRowCount()).isEqualTo(auditRows);
	}

	@Test
	void AC04_aUrlOfExactly2048CharactersIsAccepted() throws Exception {
		String longest = "https://example.com/" + "a".repeat(2048 - "https://example.com/".length());

		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(body(longest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.url").value(longest));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "{\"url\": ", "[\"https://example.com/\"]", "{\"url\": {\"a\": 1}}" })
	void AC05_bodyThatIsNotAJsonObjectIsRefused(String raw) throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(raw))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void AC06_nonJsonContentTypeIsRefused() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.TEXT_PLAIN).content("https://example.com/"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(415));
		mockMvc.perform(multipart("/api/links").file(new MockMultipartFile("url", "https://example.com/".getBytes(
				StandardCharsets.UTF_8))))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(415));
	}

	@Test
	void AC07_bodyIsRefusedAtTheSixteenKibLimit() throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(jsonOfSize(16_384)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(jsonOfSize(16_385)))
				.andExpect(status().is(413))
				.andExpect(content().contentType(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(413));
	}

	@Test
	void AC16_redirectRouteDoesNotShadowTheExistingSurface() throws Exception {
		mockMvc.perform(get("/api/ping")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").isString());
	}

	private String body(String url) {
		return jsonMapper.writeValueAsString(Map.of("url", url));
	}

	private long auditRowCount() {
		return jdbc.sql("SELECT COUNT(*) FROM audit_log").query(Long.class).single();
	}

	/** A valid create body padded through an ignored member to exactly {@code size} bytes. */
	private static byte[] jsonOfSize(int size) {
		String head = "{\"url\":\"https://example.com/\",\"pad\":\"";
		String tail = "\"}";
		return (head + "x".repeat(size - head.length() - tail.length()) + tail).getBytes(StandardCharsets.UTF_8);
	}
}
