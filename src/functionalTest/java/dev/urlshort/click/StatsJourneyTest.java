package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code GET /api/links/{code}/stats}: AC-7 to AC-13, AC-19 (its MockMvc rows), AC-20, AC-21, AC-22 and
 * business rule 7. Every test creates its own links; figures are read through the endpoint only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StatsJourneyTest {

	private static final String REQUEST_ID = "X-Request-Id";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private ClickRecorder recorder;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC07_aLinkWithNoClicksHasEmptyStatistics() throws Exception {
		String code = create("https://example.com/empty");

		MockHttpServletResponse response = stats(code);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(jsonMapper.readTree(response.getContentAsString())).isEqualTo(jsonMapper.readTree(
				"{\"code\":\"" + code + "\",\"totalClicks\":0,\"clicksPerDay\":[],\"topReferrers\":[]}"));
	}

	@Test
	void AC08_totalClicksCountsEveryRedirect() throws Exception {
		String code = create("https://example.com/seven");
		for (int i = 0; i < 7; i++) {
			open(code, null);
		}
		recorder.settle();

		JsonNode stats = body(code);

		assertThat(stats.get("totalClicks").asLong()).isEqualTo(7);
		assertConsistent(stats);
	}

	@Test
	void AC09_clicksPerDayAreGroupedByUtcCalendarDay() throws Exception {
		String code = create("https://example.com/days");
		openAt(code, "2026-10-01T23:59:59Z", 2);
		openAt(code, "2026-10-02T00:00:00Z", 3);
		openAt(code, "2026-10-04T12:00:00Z", 1);
		recorder.settle();

		JsonNode stats = body(code);

		assertThat(stats.get("clicksPerDay")).isEqualTo(jsonMapper.readTree("[{\"date\":\"2026-10-01\",\"clicks\":2},"
				+ "{\"date\":\"2026-10-02\",\"clicks\":3},{\"date\":\"2026-10-04\",\"clicks\":1}]"));
		assertThat(stats.get("totalClicks").asLong()).isEqualTo(6);
		assertConsistent(stats);
	}

	@Test
	void AC10_topReferrersAreRankedAndCapped() throws Exception {
		String code = create("https://example.com/referrers");
		for (int i = 0; i < 5; i++) {
			open(code, "https://a.example/path" + i + "?q=" + i);
		}
		for (int i = 0; i < 3; i++) {
			open(code, "https://b.example/");
			open(code, "https://c.example/");
		}
		for (int i = 1; i <= 11; i++) {
			open(code, "https://d" + i + ".example/");
		}
		for (int i = 0; i < 4; i++) {
			open(code, null);
		}
		recorder.settle();

		JsonNode stats = body(code);

		List<String> order = new ArrayList<>();
		stats.get("topReferrers").forEach(element -> {
			assertThat(element.propertyNames()).containsExactlyInAnyOrder("referrer", "clicks");
			order.add(element.get("referrer").asString() + "=" + element.get("clicks").asLong());
		});
		assertThat(order).containsExactly("https://a.example=5", "https://b.example=3", "https://c.example=3",
				"https://d1.example=1", "https://d10.example=1", "https://d11.example=1", "https://d2.example=1",
				"https://d3.example=1", "https://d4.example=1", "https://d5.example=1");
		assertThat(stats.get("totalClicks").asLong()).isEqualTo(26);
		assertConsistent(stats);
	}

	@Test
	void AC12_aRetiredLinksStatisticsAreStillReadable() throws Exception {
		String code = create("https://example.com/retire");
		for (int i = 0; i < 4; i++) {
			open(code, null);
		}
		recorder.settle();
		mockMvc.perform(delete("/api/links/" + code));

		assertThat(mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse()
				.getStatus()).isEqualTo(410);
		recorder.settle();
		MockHttpServletResponse response = stats(code);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(jsonMapper.readTree(response.getContentAsString()).get("totalClicks").asLong()).isEqualTo(4);
	}

	static Stream<Arguments> AC13_unknownCodesAndWrongMethods() {
		return Stream.of(Arguments.of("GET", "nosuchcode1", 404), Arguments.of("GET", "a".repeat(40), 404),
				Arguments.of("POST", null, 405), Arguments.of("DELETE", null, 405));
	}

	@ParameterizedTest
	@MethodSource("AC13_unknownCodesAndWrongMethods")
	void AC13_statisticsOfAnUnknownCodeAndWrongMethodsAreProblemDetails(String method, String code, int status)
			throws Exception {
		String path = code != null ? code : create("https://example.com/methods");

		MockHttpServletResponse response = mockMvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
						org.springframework.http.HttpMethod.valueOf(method), "/api/links/" + path + "/stats"))
				.andReturn().getResponse();

		assertThat(response.getStatus()).isEqualTo(status);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(jsonMapper.readTree(response.getContentAsString()).get("status").asInt()).isEqualTo(status);
		assertThat(response.getContentAsString()).doesNotContain(path);
	}

	@Test
	void AC19_theStatisticsPathAndASettledRedirectAreCorrelated(CapturedOutput output) throws Exception {
		String code = create("https://example.com/correlated");

		for (MockHttpServletRequestBuilder request : List.of(get("/api/links/" + code + "/stats"),
				get("/api/links/nosuchcode1/stats"), post("/api/links/" + code + "/stats"),
				get("/" + code).header("Accept", BROWSER_ACCEPT))) {
			recorder.settle();
			int windowStart = output.getAll().length();
			MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
			recorder.settle();
			String window = output.getAll().substring(windowStart);

			String requestId = response.getHeader(REQUEST_ID);
			assertThat(requestId).isNotBlank();
			List<String> lines = window.lines().filter(line -> !line.isBlank()).toList();
			assertThat(lines).isNotEmpty();
			for (String line : lines) {
				assertThat(jsonMapper.readTree(line).path("requestId").asString()).as(line).isEqualTo(requestId);
			}
		}
	}

	@Test
	void AC20_redirectAndAuditBehaviourAreUnchanged() throws Exception {
		String active = create("https://example.com/audit-active");
		String retired = create("https://example.com/audit-retired");
		mockMvc.perform(delete("/api/links/" + retired));
		List<Map<String, Object>> recorded = auditRows();

		assertThat(mockMvc.perform(get("/" + active).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse()
				.getHeader("Location")).isEqualTo("https://example.com/audit-active");
		recorder.settle();
		assertThat(mockMvc.perform(get("/" + retired).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse()
				.getStatus()).isEqualTo(410);
		stats(active);
		stats("nosuchcode1");
		mockMvc.perform(post("/api/links/" + active + "/stats"));

		assertThat(auditRows()).isEqualTo(recorded);
	}

	@Test
	void AC21_theLiveApiDocumentDescribesTheStatisticsEndpoint() throws Exception {
		JsonNode document = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse()
				.getContentAsString());

		JsonNode operation = document.at("/paths/~1api~1links~1{code}~1stats/get");
		assertThat(operation.isMissingNode()).isFalse();
		JsonNode ok = operation.at("/responses/200/content/application~1json");
		String ref = ok.at("/schema/$ref").asString();
		JsonNode schema = document.at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1));
		assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrder("code", "totalClicks",
				"clicksPerDay", "topReferrers");
		assertThat(ok.has("examples") || ok.has("example")).isTrue();
		assertThat(operation.at("/responses/404/content").propertyNames()).containsExactly("application/problem+json");
		for (String path : List.of("/api/ping", "/api/links", "/api/links/{code}", "/{code}")) {
			assertThat(document.get("paths").has(path)).as(path).isTrue();
		}
	}

	@Test
	void AC22_headAndOptionsKeepTheFrameworkDefaultsAndRecordNothing() throws Exception {
		String code = create("https://example.com/head-options");
		open(code, null);
		recorder.settle();
		long before = body(code).get("totalClicks").asLong();

		MockHttpServletResponse headResponse = mockMvc.perform(head("/api/links/" + code + "/stats")).andReturn()
				.getResponse();
		MockHttpServletResponse optionsResponse = mockMvc.perform(options("/api/links/" + code + "/stats")).andReturn()
				.getResponse();
		recorder.settle();

		// MockMvc keeps a HEAD body that the servlet container strips; "no body" is proven on Tomcat in
		// ClickResilienceJourneyTest
		assertThat(headResponse.getStatus()).isEqualTo(200);
		assertThat(optionsResponse.getStatus()).isEqualTo(200);
		assertThat(optionsResponse.getHeader("Allow")).contains("GET");
		assertThat(body(code).get("totalClicks").asLong()).isEqualTo(before);
	}

	/** AC-11: total = sum per day; sum over the top referrers ≤ total. */
	private static void assertConsistent(JsonNode stats) {
		long total = stats.get("totalClicks").asLong();
		long perDay = 0;
		for (JsonNode day : stats.get("clicksPerDay")) {
			perDay += day.get("clicks").asLong();
		}
		long referred = 0;
		for (JsonNode referrer : stats.get("topReferrers")) {
			referred += referrer.get("clicks").asLong();
		}
		assertThat(perDay).as("AC-11").isEqualTo(total);
		assertThat(referred).as("AC-11").isLessThanOrEqualTo(total);
	}

	private void openAt(String code, String instant, int times) throws Exception {
		clock.reset();
		// from the suite clock's own millisecond tick, so elapsed real time can only move it later
		clock.shift(Duration.between(clock.instant(), Instant.parse(instant)));
		for (int i = 0; i < times; i++) {
			open(code, null);
		}
	}

	private void open(String code, String referer) throws Exception {
		MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT);
		if (referer != null) {
			request.header("Referer", referer);
		}
		assertThat(mockMvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(302);
	}

	private JsonNode body(String code) throws Exception {
		MockHttpServletResponse response = stats(code);
		assertThat(response.getStatus()).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	private MockHttpServletResponse stats(String code) throws Exception {
		return mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn().getResponse();
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}

	private List<Map<String, Object>> auditRows() {
		return jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
	}
}
