package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * On a real Tomcat: a slow or failing click store neither slows nor fails the redirect (AC-14, AC-15,
 * AC-19's failing-store row), concurrent redirects lose no click (AC-16), the request object is never
 * read after the response is gone (W1-02: 200 stored rows all carry the reduced values), click data
 * stays out of the logs and every event is correlated (AC-18, AC-19), and {@code HEAD} on the
 * statistics path has no body and records nothing (AC-22). The {@link ClickStore} spy gives this class
 * its own context, so {@code ColdStartJourneyTest}'s server still sees only its one request.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class ClickResilienceJourneyTest {

	private static final String CANARY = "storecanary";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private ClickRecorder recorder;

	@MockitoSpyBean
	private ClickStore store;

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void AC14_aSlowClickStoreDoesNotSlowTheRedirect() throws Exception {
		String target = "https://example.com/slow";
		String code = create(target);
		doAnswer(call -> {
			Thread.sleep(2_000);
			return call.callRealMethod();
		}).when(store).insert(any());
		try {
			for (int i = 0; i < 20; i++) {
				long started = System.nanoTime();
				HttpResponse<String> response = send(get("/" + code).build());
				long millis = (System.nanoTime() - started) / 1_000_000;

				assertThat(response.statusCode()).isEqualTo(302);
				assertThat(response.headers().firstValue("Location")).contains(target);
				assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
				assertThat(millis).as("redirect %d", i + 1).isLessThan(250);
			}
		}
		finally {
			reset(store);
		}
		recorder.settle();
	}

	@Test
	void AC15_AC19_aFailingClickStoreDoesNotFailTheRedirectAndTheLossIsOneCorrelatedWarn(CapturedOutput output)
			throws Exception {
		String target = "https://example.com/failing";
		String code = create(target);
		recorder.settle();
		doThrow(new DataAccessResourceFailureException("store down " + CANARY)).when(store).insert(any());
		int windowStart = output.getAll().length();
		HttpResponse<String> response;
		try {
			response = send(get("/" + code).header("User-Agent", "Mozilla/5.0 " + CANARY).build());
			recorder.settle();
		}
		finally {
			reset(store);
		}
		String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
		String window = awaitCompleted(output, windowStart, requestId);

		assertThat(response.statusCode()).isEqualTo(302);
		assertThat(response.headers().firstValue("Location")).contains(target);
		assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
		assertThat(clickCount(code)).isZero();
		List<JsonNode> events = window.lines().filter(line -> !line.isBlank()).map(jsonMapper::readTree).toList();
		assertThat(events).filteredOn(event -> event.path("message").asString().equals("click lost")).singleElement()
				.satisfies(event -> {
					assertThat(event.path("log").path("level").asString()).isEqualTo("WARN");
					assertThat(event.path("requestId").asString()).isEqualTo(requestId);
				});
		assertThat(events).allSatisfy(event -> assertThat(event.path("requestId").asString()).isEqualTo(requestId));
		assertThat(window).doesNotContain(CANARY);
	}

	@Test
	void AC16_concurrentRedirectsLoseNoClicksAndTheRequestIsNeverReadAfterItsResponse() throws Exception {
		String code = create("https://example.com/concurrent");
		ExecutorService clients = Executors.newFixedThreadPool(20);
		List<Future<List<Integer>>> results = new ArrayList<>();
		for (int c = 0; c < 20; c++) {
			results.add(clients.submit(() -> {
				List<Integer> statuses = new ArrayList<>();
				for (int i = 0; i < 10; i++) {
					statuses.add(send(get("/" + code).header("Referer", "https://ref.example/p?q=realcanary")
							.header("User-Agent", "Mozilla/5.0 realcanary").build()).statusCode());
				}
				return statuses;
			}));
		}
		for (Future<List<Integer>> result : results) {
			assertThat(result.get()).containsOnly(302);
		}
		clients.shutdown();
		recorder.settle();

		assertThat(stats(code).get("totalClicks").asLong()).isEqualTo(200);
		List<Map<String, Object>> rows = jdbc.sql("SELECT c.referrer, c.user_agent_class FROM click c"
				+ " JOIN link l ON l.id = c.link_id WHERE l.code = :code").param("code", code).query().listOfRows();
		assertThat(rows).hasSize(200).allSatisfy(row -> {
			assertThat(row.get("REFERRER")).isEqualTo("https://ref.example");
			assertThat(row.get("USER_AGENT_CLASS")).isEqualTo("browser");
		});
	}

	@Test
	void AC18_AC19_onTomcatClickDataStaysOutOfTheLogsAndEveryEventIsCorrelated(CapturedOutput output)
			throws Exception {
		String code = create("https://example.com/tomcat-logs");
		recorder.settle();
		int windowStart = output.getAll().length();

		HttpResponse<String> redirect = send(get("/" + code).header("User-Agent", "Mozilla/5.0 tomcatuacanary")
				.header("Referer", "https://tomcatorigin.example/tomcatpathcanary?q=tomcatquerycanary")
				.header("X-Forwarded-For", "192.0.2.77").build());
		recorder.settle();
		String redirectId = redirect.headers().firstValue("X-Request-Id").orElseThrow();
		awaitCompleted(output, windowStart, redirectId);
		HttpResponse<String> stats = send(get("/api/links/" + code + "/stats").build());
		String statsId = stats.headers().firstValue("X-Request-Id").orElseThrow();
		String window = awaitCompleted(output, windowStart, statsId);

		assertThat(redirect.statusCode()).isEqualTo(302);
		assertThat(stats.statusCode()).isEqualTo(200);
		String hash = jdbc.sql("SELECT c.client_hash FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query(String.class).single();
		assertThat(window).doesNotContain("tomcatuacanary").doesNotContain("tomcatpathcanary")
				.doesNotContain("tomcatquerycanary").doesNotContain("tomcatorigin.example").doesNotContain("192.0.2.77")
				.doesNotContain(hash);
		assertThat(window.lines().filter(line -> !line.isBlank()).map(jsonMapper::readTree))
				.allSatisfy(event -> assertThat(event.path("requestId").asString()).isIn(redirectId, statsId));
	}

	@Test
	void AC22_headOnTheStatisticsPathHasNoBodyAndRecordsNothing() throws Exception {
		String code = create("https://example.com/head");
		send(get("/" + code).build());
		recorder.settle();
		long before = stats(code).get("totalClicks").asLong();

		HttpResponse<String> head = send(HttpRequest.newBuilder(uri("/api/links/" + code + "/stats"))
				.method("HEAD", HttpRequest.BodyPublishers.noBody()).build());
		recorder.settle();

		assertThat(head.statusCode()).isEqualTo(200);
		assertThat(head.body()).isEmpty();
		assertThat(stats(code).get("totalClicks").asLong()).isEqualTo(before);
	}

	/**
	 * Waits, bounded, for the filter's {@code request completed} event of {@code requestId}, which Tomcat
	 * may write after the client already has the response, and returns the log written since
	 * {@code windowStart}.
	 */
	private static String awaitCompleted(CapturedOutput output, int windowStart, String requestId) {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		String window = output.getAll().substring(windowStart);
		while (!window.lines().anyMatch(line -> line.contains("request completed") && line.contains(requestId))
				&& Instant.now().isBefore(deadline)) {
			Thread.onSpinWait();
			window = output.getAll().substring(windowStart);
		}
		return window;
	}

	private JsonNode stats(String code) throws Exception {
		return jsonMapper.readTree(send(get("/api/links/" + code + "/stats").build()).body());
	}

	private long clickCount(String code) {
		return jdbc.sql("SELECT COUNT(*) FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query(Long.class).single();
	}

	private String create(String url) throws Exception {
		HttpResponse<String> created = send(HttpRequest.newBuilder(uri("/api/links"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(Map.of("url", url)))).build());
		assertThat(created.statusCode()).isEqualTo(201);
		return jsonMapper.readTree(created.body()).get("code").asString();
	}

	private HttpRequest.Builder get(String path) {
		return HttpRequest.newBuilder(uri(path)).GET();
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
