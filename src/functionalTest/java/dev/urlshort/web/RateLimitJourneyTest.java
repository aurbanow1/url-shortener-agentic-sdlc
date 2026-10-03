package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
 * The per-client rate limit at the shipped budgets (60 creates, 600 redirects per minute): AC-1 to
 * AC-7, AC-9, AC-11, AC-12, its metrics (AC-16, AC-17, AC-19, with the Prometheus registry exported)
 * and business rules 1 and 3; plus design review DR-01's real-server log canary. The clock is frozen per test so a budget is
 * spent in no time; each test uses its own peer address so buckets never carry over, except AC-11
 * and AC-12, whose address the SPEC fixes and which therefore exhaust it until the first {@code 429}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "urlshort.rate-limit.create-per-minute=60", "urlshort.rate-limit.redirect-per-minute=600" })
@AutoConfigureMockMvc
@AutoConfigureMetrics
@ExtendWith(OutputCaptureExtension.class)
class RateLimitJourneyTest {

	private static final String PROBLEM_JSON = "application/problem+json";
	private static final String CANARY_PEER = "10.77.77.77";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private FunctionalClock clock;

	@BeforeEach
	void freeze() {
		clock.reset();
		clock.freeze();
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC01_theCreateBudgetAdmits60AndRefusesThe61st() throws Exception {
		long createRows = createRows();

		for (int i = 0; i < 60; i++) {
			assertThat(create("10.0.1.1").getStatus()).as("create %d", i + 1).isEqualTo(201);
		}
		MockHttpServletResponse refused = create("10.0.1.1");

		assertTooManyRequests(refused);
		assertThat(createRows()).isEqualTo(createRows + 60);
	}

	@Test
	void AC02_theRedirectBudgetAdmits600AndRefusesThe601st() throws Exception {
		String code = code(create("10.0.2.0"));

		for (int i = 0; i < 600; i++) {
			assertThat(redirect("10.0.2.1", code).getStatus()).as("redirect %d", i + 1).isEqualTo(302);
		}
		MockHttpServletResponse refused = redirect("10.0.2.1", code);

		assertTooManyRequests(refused);
		assertThat(refused.getHeader("Location")).isNull();
	}

	@Test
	void AC03a_retryAfterIsTruthfulFromAnExactlyEmptyBucket() throws Exception {
		spendCreates("10.0.3.1", 60);
		MockHttpServletResponse first = create("10.0.3.1");
		assertThat(first.getStatus()).isEqualTo(429);
		assertThat(first.getHeader("Retry-After")).isEqualTo("1");

		clock.shift(Duration.ofMillis(999));
		assertThat(create("10.0.3.1").getStatus()).isEqualTo(429);
		clock.shift(Duration.ofMillis(1));
		assertThat(create("10.0.3.1").getStatus()).isEqualTo(201);
		assertThat(create("10.0.3.1").getStatus()).isEqualTo(429);
	}

	@Test
	void AC03b_retryAfterIsAnUpperBoundFromAPartlyRefilledBucket() throws Exception {
		spendCreates("10.0.3.2", 60);
		clock.shift(Duration.ofMillis(250));
		MockHttpServletResponse refused = create("10.0.3.2");
		assertThat(refused.getStatus()).isEqualTo(429);
		long seconds = Long.parseLong(refused.getHeader("Retry-After"));
		assertThat(seconds).isGreaterThanOrEqualTo(1);

		clock.shift(Duration.ofSeconds(seconds));

		assertThat(create("10.0.3.2").getStatus()).isEqualTo(201);
	}

	@Test
	void AC04_aFullBudgetReturnsAfterAQuietMinute() throws Exception {
		spendCreates("10.0.4.1", 60);
		assertThat(create("10.0.4.1").getStatus()).isEqualTo(429);

		clock.shift(Duration.ofSeconds(60));

		spendCreates("10.0.4.1", 60);
		assertThat(create("10.0.4.1").getStatus()).isEqualTo(429);
	}

	@Test
	void AC05_theTwoBudgetsAreIndependent() throws Exception {
		String code = code(create("10.0.5.0"));
		spendCreates("10.0.5.1", 60);
		assertThat(create("10.0.5.1").getStatus()).isEqualTo(429);
		for (int i = 0; i < 600; i++) {
			redirect("10.0.5.2", code);
		}
		assertThat(redirect("10.0.5.2", code).getStatus()).isEqualTo(429);

		assertThat(redirect("10.0.5.1", code).getStatus()).isEqualTo(302);
		assertThat(create("10.0.5.2").getStatus()).isEqualTo(201);
	}

	@Test
	void AC06_clientsAreIndependent() throws Exception {
		spendCreates("10.0.0.1", 60);
		assertThat(create("10.0.0.1").getStatus()).isEqualTo(429);

		assertThat(create("10.0.0.2").getStatus()).isEqualTo(201);
	}

	@Test
	void AC07_aForgedForwardedAddressDoesNotChangeTheClient() throws Exception {
		for (int i = 0; i < 60; i++) {
			assertThat(perform(forged(i)).getStatus()).as("create %d", i + 1).isEqualTo(201);
		}

		assertThat(perform(forged(60)).getStatus()).isEqualTo(429);
	}

	@Test
	void AC09_everyRequestInABudgetCountsAndTheLimitIsCheckedFirst() throws Exception {
		for (int i = 0; i < 60; i++) {
			assertThat(perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"ftp://x/\"}")
					.with(peer("10.0.9.1"))).getStatus()).isEqualTo(400);
		}

		MockHttpServletResponse valid = create("10.0.9.1");
		MockHttpServletResponse oversized = perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/\",\"pad\":\"" + "x".repeat(16_400) + "\"}")
				.with(peer("10.0.9.1")));
		assertThat(valid.getStatus()).isEqualTo(429);
		assertThat(oversized.getStatus()).isEqualTo(429);

		clock.shift(Duration.ofSeconds(Long.parseLong(oversized.getHeader("Retry-After"))));
		assertThat(create("10.0.9.1").getStatus()).as("a 429 took no token").isEqualTo(201);
		assertThat(create("10.0.9.1").getStatus()).as("exactly one was admitted").isEqualTo(429);
	}

	@Test
	void AC11_theTooManyRequestsProblemNamesNoClient() throws Exception {
		String userAgent = "canary-ua-" + UUID.randomUUID();
		String urlCanary = "canaryurl" + UUID.randomUUID().toString().replace("-", "");
		exhaustCreates(CANARY_PEER);

		MockHttpServletResponse refused = perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", "192.0.2.201").header("User-Agent", userAgent)
				.content(json("https://example.com/?q=" + urlCanary)).with(peer(CANARY_PEER)));

		assertTooManyRequests(refused);
		JsonNode body = jsonMapper.readTree(refused.getContentAsString());
		assertThat(body.has("errors")).isFalse();
		assertThat(body.has("detail")).isFalse();
		assertThat(body.get("instance").asString()).isEqualTo("urn:uuid:" + refused.getHeader("X-Request-Id"));
		StringBuilder everything = new StringBuilder(refused.getContentAsString());
		for (String name : refused.getHeaderNames()) {
			everything.append(name).append(refused.getHeaders(name));
		}
		assertThat(everything.toString()).doesNotContain(CANARY_PEER).doesNotContain("192.0.2.201")
				.doesNotContain(userAgent).doesNotContain(urlCanary);
	}

	@Test
	void AC12_eachRejectionIsLoggedOnceCorrelatedAndWithoutClientValues(CapturedOutput output) throws Exception {
		String userAgent = "canary-ua-" + UUID.randomUUID();
		String urlCanary = "canaryurl" + UUID.randomUUID().toString().replace("-", "");
		String code = code(create("10.0.12.0"));
		exhaustCreates(CANARY_PEER);
		exhaustRedirects(CANARY_PEER, code);

		for (MockHttpServletRequestBuilder request : List.of(
				post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json("https://example.com/?q=" + urlCanary)),
				get("/" + code), get("/api/links/" + code))) {
			int windowStart = output.getAll().length();
			MockHttpServletResponse refused = perform(request.header("X-Forwarded-For", "192.0.2.201")
					.header("User-Agent", userAgent).with(peer(CANARY_PEER)));
			String window = output.getAll().substring(windowStart);

			assertThat(refused.getStatus()).isEqualTo(429);
			String requestId = refused.getHeader("X-Request-Id");
			List<String> lines = window.lines().filter(line -> !line.isBlank()).toList();
			assertThat(lines).singleElement().satisfies(line -> {
				JsonNode event = jsonMapper.readTree(line);
				assertThat(event.path("requestId").asString()).isEqualTo(requestId);
				assertThat(event.path("status").asInt()).isEqualTo(429);
			});
			assertThat(window).doesNotContain(CANARY_PEER).doesNotContain("192.0.2.201").doesNotContain(userAgent)
					.doesNotContain(urlCanary);
		}
	}

	@Test
	void rule1_percentEncodingDoesNotMoveARequestToTheLargerBudget() throws Exception {
		spendCreates("10.0.21.1", 60);

		MockHttpServletResponse encoded = perform(post(URI.create("/%61pi/links")).contentType(MediaType.APPLICATION_JSON)
				.content(json("https://example.com/")).with(peer("10.0.21.1")));

		assertThat(encoded.getStatus()).isEqualTo(429);
	}

	@Test
	void rule1_operatorSurfacesAreNeverLimited() throws Exception {
		String code = code(create("10.0.22.0"));
		exhaustCreates("10.0.22.1");
		exhaustRedirects("10.0.22.1", code);

		for (String path : List.of("/actuator/health", "/v3/api-docs", "/swagger-ui.html")) {
			for (int i = 0; i < 3; i++) {
				assertThat(perform(get(path).with(peer("10.0.22.1"))).getStatus()).as(path).isNotEqualTo(429);
			}
		}
	}

	@Test
	void rule1_aDotDotSegmentUnderAnExemptPrefixReachesNoLimitedOperation() throws Exception {
		String code = code(create("10.0.23.0"));

		MockHttpServletResponse response = perform(get(URI.create("/actuator/../" + code)).with(peer("10.0.23.1")));

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getHeader("Location")).isNull();
	}

	@Test
	void AC16_theMetricsSurfaceListsTheFourKindsOfMetric() throws Exception {
		String code = code(create("10.0.16.0"));
		redirect("10.0.16.0", code);
		exhaustCreates("10.0.16.1");

		JsonNode names = jsonMapper.readTree(perform(get("/actuator/metrics")).getContentAsString()).get("names");

		List<String> listed = new java.util.ArrayList<>();
		names.forEach(name -> listed.add(name.asString()));
		assertThat(listed).contains("http.server.requests", "urlshort.ratelimit.rejections",
				"hikaricp.connections.active", "hikaricp.connections.idle");
	}

	@Test
	void AC17_everyRejectionIsCountedOnceByBudget() throws Exception {
		String code = code(create("10.0.17.0"));
		double creates = rejections("create");
		double redirects = rejections("redirect");

		exhaustCreates("10.0.17.1");
		create("10.0.17.1");
		exhaustRedirects("10.0.17.1", code);
		redirect("10.0.17.1", code);
		redirect("10.0.17.1", code);
		create("10.0.17.2");
		redirect("10.0.17.2", code);

		assertThat(rejections("create")).isEqualTo(creates + 2);
		assertThat(rejections("redirect")).isEqualTo(redirects + 3);
		JsonNode tags = jsonMapper.readTree(perform(get("/actuator/metrics/urlshort.ratelimit.rejections"))
				.getContentAsString()).get("availableTags");
		assertThat(tags).singleElement().satisfies(tag -> {
			assertThat(tag.get("tag").asString()).isEqualTo("budget");
			assertThat(tag.toString()).doesNotContain("10.0.17");
		});
	}

	@Test
	void AC19_metricsAreExposedForScrapingWithoutClientOrLinkValues() throws Exception {
		String target = "https://example.com/scraped-" + UUID.randomUUID();
		String code = code(perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json(target))
				.with(peer("10.0.19.0"))));
		redirect("10.0.19.0", code);
		perform(get("/zzCanary99").with(peer("10.0.19.0")));
		exhaustCreates(CANARY_PEER);
		create(CANARY_PEER);

		MockHttpServletResponse scrape = perform(get("/actuator/prometheus"));

		assertThat(scrape.getStatus()).isEqualTo(200);
		assertThat(scrape.getContentType()).startsWith("text/plain");
		String body = scrape.getContentAsString();
		assertThat(body).contains("http_server_requests_seconds", "urlshort_ratelimit_rejections_total",
				"hikaricp_connections_active", "hikaricp_connections_idle");
		assertThat(body).doesNotContain(code).doesNotContain("zzCanary99").doesNotContain(CANARY_PEER)
				.doesNotContain(target);
	}

	@Test
	void designDR01_anInvalidPathUnderAnExemptPrefixLogsNoSubmittedValueOnTomcat(CapturedOutput output)
			throws Exception {
		int windowStart = output.getAll().length();
		java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
		URI target = URI.create("http://localhost:" + port + "/actuator/../pii-canary-10.77.77.77");

		int get = http.send(java.net.http.HttpRequest.newBuilder(target).GET().build(),
				java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
		int post = http.send(java.net.http.HttpRequest.newBuilder(target)
				.POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build(),
				java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
		String window = awaitTwoCompletions(output, windowStart);

		assertThat(get).isEqualTo(404);
		assertThat(post).isIn(404, 405);
		assertThat(window).doesNotContain("pii-canary").doesNotContain(CANARY_PEER);
	}

	private static String awaitTwoCompletions(CapturedOutput output, int windowStart) {
		java.time.Instant deadline = java.time.Instant.now().plusSeconds(5);
		String window = output.getAll().substring(windowStart);
		while (window.split("request completed", -1).length < 3 && java.time.Instant.now().isBefore(deadline)) {
			Thread.onSpinWait();
			window = output.getAll().substring(windowStart);
		}
		return window;
	}

	private double rejections(String budget) throws Exception {
		JsonNode metric = jsonMapper.readTree(perform(get("/actuator/metrics/urlshort.ratelimit.rejections")
				.param("tag", "budget:" + budget)).getContentAsString());
		return metric.get("measurements").get(0).get("value").asDouble();
	}

	private void assertTooManyRequests(MockHttpServletResponse refused) throws Exception {
		assertThat(refused.getStatus()).isEqualTo(429);
		assertThat(refused.getContentType()).isEqualTo(PROBLEM_JSON);
		assertThat(refused.getHeader("Retry-After")).matches("[1-9][0-9]*");
		assertThat(refused.getHeader("X-Request-Id")).isNotBlank();
		assertThat(jsonMapper.readTree(refused.getContentAsString()).get("status").asInt()).isEqualTo(429);
	}

	private MockHttpServletRequestBuilder forged(int i) {
		MockHttpServletRequestBuilder request = post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(json("https://example.com/")).header("X-Forwarded-For", "198.51.100." + (i % 250))
				.with(peer("10.0.7.1"));
		if (i % 3 == 0) {
			request.header("Forwarded", "for=203.0.113." + (i % 250)).header("X-Real-IP", "192.0.2." + (i % 250));
		}
		return request;
	}

	private void spendCreates(String peer, int n) throws Exception {
		for (int i = 0; i < n; i++) {
			assertThat(create(peer).getStatus()).as("create %d from %s", i + 1, peer).isEqualTo(201);
		}
	}

	private void exhaustCreates(String peer) throws Exception {
		for (int i = 0; i <= 60 && create(peer).getStatus() != 429; i++) {
			// spend until the first refusal
		}
	}

	private void exhaustRedirects(String peer, String code) throws Exception {
		for (int i = 0; i <= 600 && redirect(peer, code).getStatus() != 429; i++) {
			// spend until the first refusal
		}
	}

	private MockHttpServletResponse create(String peer) throws Exception {
		return perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json("https://example.com/"))
				.with(peer(peer)));
	}

	private MockHttpServletResponse redirect(String peer, String code) throws Exception {
		return perform(get("/" + code).with(peer(peer)));
	}

	private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request).andReturn().getResponse();
	}

	private String code(MockHttpServletResponse created) throws Exception {
		assertThat(created.getStatus()).isEqualTo(201);
		return jsonMapper.readTree(created.getContentAsString()).get("code").asString();
	}

	private String json(String url) {
		return jsonMapper.writeValueAsString(Map.of("url", url));
	}

	private long createRows() {
		return jdbc.sql("SELECT COUNT(*) FROM audit_log WHERE action = 'link.create'").query(Long.class).single();
	}

	static org.springframework.test.web.servlet.request.RequestPostProcessor peer(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
