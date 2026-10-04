package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

/**
 * The click counters (AC-10, AC-11; business rule 10; ADR-0016 amendment): {@code urlshort.clicks.recorded}
 * grows by every stored click, {@code urlshort.clicks.lost} by every {@code click lost} event under that
 * event's {@code reason}, and the scrape names no client. A real Tomcat with a {@link ClickStore} spy, as
 * in {@code ClickResilienceJourneyTest} (v1's AC-15 mechanism); {@code @AutoConfigureMetrics} turns on the
 * Prometheus export that Boot's test support otherwise leaves off, as in {@code RateLimitJourneyTest}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
class ClickMetricsJourneyTest {

	private static final Set<String> REASONS = Set.of("rejected", "reduction failed", "write failed",
			"shutdown deadline", "shutdown deadline, outcome unknown");

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private ClickRecorder recorder;

	@MockitoSpyBean
	private ClickStore store;

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void AC10_recordedAndLostClicksAreCounted() throws Exception {
		String code = create("https://example.com/v2-metrics");
		recorder.settle();
		double recordedBefore = meter("urlshort.clicks.recorded");
		double lostBefore = meter("urlshort.clicks.lost?tag=reason:write%20failed");

		for (int i = 0; i < 3; i++) {
			assertThat(send("/" + code).statusCode()).isEqualTo(302);
		}
		recorder.settle();
		double recordedAfterWrites = meter("urlshort.clicks.recorded");
		doThrow(new DataAccessResourceFailureException("store down")).when(store).insert(any());
		try {
			for (int i = 0; i < 2; i++) {
				assertThat(send("/" + code).statusCode()).isEqualTo(302);
			}
			recorder.settle();
		}
		finally {
			reset(store);
		}

		assertThat(recordedAfterWrites - recordedBefore).isEqualTo(3.0);
		assertThat(meter("urlshort.clicks.recorded")).isEqualTo(recordedAfterWrites);
		assertThat(meter("urlshort.clicks.lost?tag=reason:write%20failed") - lostBefore).isEqualTo(2.0);
	}

	@Test
	void AC11_theClickCountersAreScrapeableAndNameNoClient() throws Exception {
		String code = create("https://example.com/v2-scrape");
		send("/" + code);
		recorder.settle();

		HttpResponse<String> response = send("/actuator/prometheus");
		String scrape = response.body();

		assertThat(response.statusCode()).as(scrape.substring(0, Math.min(300, scrape.length()))).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("text/plain"));
		List<String> lines = scrape.lines().filter(line -> line.startsWith("urlshort_clicks_")).toList();
		assertThat(lines).anyMatch(line -> line.startsWith("urlshort_clicks_recorded_total "));
		assertThat(lines).filteredOn(line -> line.startsWith("urlshort_clicks_lost_total")).hasSize(REASONS.size())
				.allSatisfy(line -> {
					Matcher labels = Pattern.compile("^urlshort_clicks_lost_total\\{reason=\"([^\"]*)\"} .*$")
							.matcher(line);
					assertThat(labels.matches()).as(line).isTrue();
					assertThat(labels.group(1)).isIn(REASONS);
				});
		assertThat(String.join("\n", lines)).doesNotContain(code).doesNotContain("127.0.0.1")
				.doesNotContain("example.com");
	}

	/** The meter's {@code COUNT} measurement from {@code /actuator/metrics/<name>}. */
	private double meter(String nameAndQuery) throws Exception {
		return jsonMapper.readTree(send("/actuator/metrics/" + nameAndQuery).body()).get("measurements").get(0)
				.get("value").asDouble();
	}

	private String create(String url) throws Exception {
		HttpResponse<String> created = http.send(HttpRequest.newBuilder(uri("/api/links"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(Map.of("url", url)))).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(created.statusCode()).isEqualTo(201);
		return jsonMapper.readTree(created.body()).get("code").asString();
	}

	private HttpResponse<String> send(String path) throws Exception {
		return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}
