package dev.urlshort.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-26 on a real server's first request (design DR-04): MockMvc initialises its servlet with the
 * context, so only a real Tomcat shows whether the DispatcherServlet logs inside the first request
 * without that request's id. This is the only real-server class, so its Tomcat sees exactly one
 * request.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class ColdStartJourneyTest {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void AC26_theFirstRequestOnARealServerLogsOnlyItsOwnCorrelatedEvents(CapturedOutput output) throws Exception {
		int windowStart = output.getAll().length();

		HttpResponse<String> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/ping")).build(),
				HttpResponse.BodyHandlers.ofString());
		String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
		// the filter writes "request completed" in its finally block, which may run after the client
		// already has the response: wait for it, bounded, before reading the window
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		while (!output.getAll().substring(windowStart).contains("request completed")
				&& Instant.now().isBefore(deadline)) {
			Thread.onSpinWait();
		}

		assertThat(response.statusCode()).isEqualTo(200);
		List<String> lines = output.getAll().substring(windowStart).lines().filter(line -> !line.isBlank()).toList();
		assertThat(lines).anyMatch(line -> line.contains("request completed"));
		for (String line : lines) {
			JsonNode event = jsonMapper.readTree(line);
			assertThat(event.path("requestId").asString()).as("event carries the first request's id: %s", line)
					.isEqualTo(requestId);
		}
	}
}
