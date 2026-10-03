package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import dev.urlshort.audit.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Request ids, log events and privacy: AC-26, AC-27, business rule 8's "no submitted value in a body"
 * for values the framework would echo, and business rule 10 on a real database failure whose driver
 * message quotes the client's key (design DR-01, DR-02). Shares the {@link AuditLog} spy context with
 * {@code AuditJourneyTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ObservabilityJourneyTest {

	private static final String REQUEST_ID = "X-Request-Id";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@MockitoSpyBean
	private AuditLog auditLog;

	@ParameterizedTest
	@ValueSource(strings = { "201 create", "200 read", "204 retire", "302 redirect", "404 unknown code", "410 retired",
			"400 invalid url", "405 wrong method", "413 oversized body", "415 wrong content type", "422 key mismatch",
			"500 induced audit failure" })
	void AC26_everyResponseCarriesARequestIdAndEveryEventOfTheRequestTheSameId(String scenario, CapturedOutput output)
			throws Exception {
		MockHttpServletRequestBuilder request = arrange(scenario);
		int windowStart = output.getAll().length();

		MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();

		String window = output.getAll().substring(windowStart);
		assertThat(response.getStatus()).isEqualTo(Integer.parseInt(scenario.substring(0, 3)));
		String requestId = response.getHeader(REQUEST_ID);
		assertThat(requestId).isNotBlank();
		List<String> lines = window.lines().filter(line -> !line.isBlank()).toList();
		assertThat(lines).as("at least one event for %s", scenario).isNotEmpty();
		for (String line : lines) {
			JsonNode event = jsonMapper.readTree(line);
			assertThat(event.isObject()).as("one JSON object per line: %s", line).isTrue();
			assertThat(event.path("requestId").asString()).as("event carries this request's id: %s", line)
					.isEqualTo(requestId);
		}
	}

	@Test
	void AC27_noClientControlledValueReachesTheLogs(CapturedOutput output) throws Exception {
		String userAgent = "canary-ua-" + UUID.randomUUID();
		String queryCanary = "canaryq" + UUID.randomUUID().toString().replace("-", "");
		String keyCanary = "canary-key-" + UUID.randomUUID();
		String inboundId = "canary-rid-" + UUID.randomUUID();
		String scriptCanary = "canaryjs" + UUID.randomUUID().toString().replace("-", "");
		String remoteAddr = "203.0.113.77";

		MockHttpServletResponse created = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", userAgent).header("Idempotency-Key", keyCanary).header(REQUEST_ID, inboundId)
				.content(json("https://example.com/p?token=" + queryCanary))
				.with(r -> {
					r.setRemoteAddr(remoteAddr);
					return r;
				})).andReturn().getResponse();
		String code = jsonMapper.readTree(created.getContentAsString()).get("code").asString();
		mockMvc.perform(get("/" + code).header("User-Agent", userAgent).with(r -> {
			r.setRemoteAddr(remoteAddr);
			return r;
		}));
		int rejected = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(json("javascript:alert('" + scriptCanary + "')"))).andReturn().getResponse().getStatus();

		assertThat(created.getStatus()).isEqualTo(201);
		assertThat(rejected).isEqualTo(400);
		assertThat(created.getHeader(REQUEST_ID)).isNotEqualTo(inboundId);
		assertThat(output.getAll()).doesNotContain(userAgent).doesNotContain(queryCanary).doesNotContain(keyCanary)
				.doesNotContain(inboundId).doesNotContain(scriptCanary).doesNotContain(remoteAddr);
	}

	@Test
	void AC27_aDatabaseFailureQuotingTheKeyLogsOnlyClassNames(CapturedOutput output) throws Exception {
		String keyCanary = "canary-key-" + UUID.randomUUID();
		String queryCanary = "canaryq" + UUID.randomUUID().toString().replace("-", "");
		// copy the row just inserted under another code: the unique key constraint fails and H2's message
		// quotes the key, exactly as when two creates race on one key
		doAnswer(invocation -> jdbc.sql("INSERT INTO link (code, url, created_at, idempotency_key) "
				+ "SELECT CONCAT('Zz', code), url, created_at, idempotency_key FROM link WHERE idempotency_key = :k")
				.param("k", keyCanary).update())
				.when(auditLog).append(eq("link.create"), any(), any(), any(), any());

		MockHttpServletResponse response = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", keyCanary).content(json("https://example.com/p?token=" + queryCanary)))
				.andReturn().getResponse();

		String requestId = response.getHeader(REQUEST_ID);
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getContentAsString()).doesNotContain(keyCanary).doesNotContain(queryCanary);
		assertThat(output.getAll()).doesNotContain(keyCanary).doesNotContain(queryCanary);
		List<JsonNode> failures = output.getAll().lines().filter(line -> line.contains("request failed"))
				.map(jsonMapper::readTree).toList();
		assertThat(failures).singleElement().satisfies(event -> {
			assertThat(event.path("requestId").asString()).isEqualTo(requestId);
			assertThat(event.path("errorChain").asString()).contains("org.springframework.dao.DuplicateKeyException");
			assertThat(event.has("errorOrigin")).isTrue();
		});
		assertThat(jdbc.sql("SELECT COUNT(*) FROM link WHERE idempotency_key = :k").param("k", keyCanary)
				.query(Long.class).single()).as("the create rolled back; the key stays unbound").isZero();
	}

	static Stream<Arguments> rule8_valuesTheFrameworkWouldEcho() {
		String letters = UUID.randomUUID().toString().replaceAll("[^a-f]", "");
		String contentTypeCanary = "canaryct" + letters;
		String pathCanary = ("canarypath" + letters + "x".repeat(45)).substring(0, 45);
		String icoCanary = "canaryico" + letters;
		String codeShaped = ("Cn" + UUID.randomUUID().toString().replace("-", "")).substring(0, 24);
		String codeShapedVisitor = ("Cv" + UUID.randomUUID().toString().replace("-", "")).substring(0, 24);
		String methodCanary = ("CANARYMETHOD" + letters.toUpperCase());
		return Stream.of(
				Arguments.of(post("/api/links").contentType("text/plain; note=" + contentTypeCanary).content("x"), 415,
						contentTypeCanary),
				Arguments.of(get("/api/links/" + pathCanary), 404, pathCanary),
				Arguments.of(get("/" + icoCanary + ".ico"), 404, icoCanary),
				Arguments.of(get("/api/links/" + codeShaped), 404, codeShaped),
				Arguments.of(get("/" + codeShapedVisitor), 404, codeShapedVisitor),
				Arguments.of(request(HttpMethod.valueOf(methodCanary), "/api/links"), 405, methodCanary));
	}

	@ParameterizedTest
	@MethodSource("rule8_valuesTheFrameworkWouldEcho")
	void rule8_problemBodiesAndLogsNeverEchoASubmittedValue(MockHttpServletRequestBuilder request, int status,
			String canary, CapturedOutput output) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(status);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		String body = response.getContentAsString(StandardCharsets.UTF_8);
		assertThat(body).doesNotContain(canary);
		assertThat(output.getAll()).doesNotContain(canary);
		JsonNode problem = jsonMapper.readTree(body);
		assertThat(problem.has("detail")).isFalse();
		assertThat(problem.get("instance").asString()).isEqualTo("urn:uuid:" + response.getHeader(REQUEST_ID));
	}

	private MockHttpServletRequestBuilder arrange(String scenario) throws Exception {
		return switch (scenario) {
			case "201 create" -> post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json("https://example.com/"));
			case "200 read" -> get("/api/links/" + create());
			case "204 retire" -> delete("/api/links/" + create());
			case "302 redirect" -> get("/" + create());
			case "404 unknown code" -> get("/api/links/nosuchcode1");
			case "410 retired" -> {
				String code = create();
				mockMvc.perform(delete("/api/links/" + code));
				yield get("/" + code);
			}
			case "400 invalid url" -> post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json("ftp://x/"));
			case "405 wrong method" -> get("/api/links");
			case "413 oversized body" -> post("/api/links").contentType(MediaType.APPLICATION_JSON)
					.content("{\"url\":\"https://example.com/\",\"pad\":\"" + "x".repeat(16_400) + "\"}");
			case "415 wrong content type" -> post("/api/links").contentType(MediaType.TEXT_PLAIN).content("https://example.com/");
			case "422 key mismatch" -> {
				String key = "key-" + UUID.randomUUID();
				mockMvc.perform(post("/api/links").header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
						.content(json("https://example.com/a")));
				yield post("/api/links").header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
						.content(json("https://example.com/b"));
			}
			case "500 induced audit failure" -> {
				String code = create();
				doThrow(new DataAccessResourceFailureException("audit store unavailable")).when(auditLog)
						.append(eq("link.retire"), any(), any(), any(), any());
				yield delete("/api/links/" + code);
			}
			default -> throw new IllegalArgumentException(scenario);
		};
	}

	private String create() throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(json("https://example.com/obs"))).andReturn().getResponse().getContentAsString())
				.get("code").asString();
	}

	private String json(String url) {
		return jsonMapper.writeValueAsString(Map.of("url", url));
	}
}
