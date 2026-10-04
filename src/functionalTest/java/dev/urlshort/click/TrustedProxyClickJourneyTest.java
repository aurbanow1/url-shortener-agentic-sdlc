package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static dev.urlshort.click.ClickRecordingJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Behind an operator-listed proxy the click's client is the rate limiter's client (AC-7, AC-9, AC-12;
 * business rules 5, 6 and 11; ADR-0015): the right-most untrusted {@code X-Forwarded-For} entry, or the
 * proxy when there is none. Forwarded values are never stored, logged or exposed.
 */
@SpringBootTest(properties = "urlshort.rate-limit.trusted-proxies=10.9.9.9")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class TrustedProxyClickJourneyTest {

	private static final String PROXY = "10.9.9.9";
	private static final List<String> FORWARDED = List.of("203.0.113.7", "203.0.113.8", "198.51.100.1");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private ClickRecorder recorder;

	@Test
	void AC07_behindATrustedProxyUniquesCountTheForwardedClients() throws Exception {
		String code = create("https://example.com/v2-proxy");

		sendAc07Redirects(code);
		recorder.settle();

		JsonNode stats = stats(code);
		assertThat(stats.get("clicksPerDay")).containsExactly(jsonMapper.readTree("{\"date\":\""
				+ LocalDate.now(ZoneOffset.UTC) + "\",\"clicks\":4,\"uniqueVisitors\":3,\"botClicks\":0}"));
		String rows = jdbc.sql("SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query().listOfRows().toString();
		for (String value : List.of("203.0.113.7", "203.0.113.8", "198.51.100.1", PROXY)) {
			assertThat(rows).doesNotContain(value);
		}
	}

	@Test
	void AC09_theStatisticsStillExposeAggregatesOnly() throws Exception {
		String code = create("https://example.com/v2-aggregates");
		open(code, "203.0.113.77", "Mozilla/5.0 uacanary", null,
				"https://News.Example/a/refpathcanary?t=refquerycanary#reffragcanary");
		open(code, "198.51.100.23", "uacanary-crawler/1.0", null, null);
		sendAc07Redirects(code);
		recorder.settle();
		List<Object> hashes = jdbc.sql("SELECT c.client_hash FROM click c JOIN link l ON l.id = c.link_id"
				+ " WHERE l.code = :code").param("code", code).query().singleColumn();

		String body = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn().getResponse()
				.getContentAsString();

		JsonNode stats = jsonMapper.readTree(body);
		assertThat(stats.propertyNames()).containsExactly("code", "totalClicks", "clicksPerDay", "topReferrers");
		stats.get("clicksPerDay").forEach(day -> assertThat(day.propertyNames()).containsExactly("date", "clicks",
				"uniqueVisitors", "botClicks"));
		assertThat(body).doesNotContain("203.0.113.77").doesNotContain("198.51.100.23").doesNotContain(PROXY)
				.doesNotContain("browser").doesNotContain("\"bot\"").doesNotContain("uacanary")
				.doesNotContain("refpathcanary").doesNotContain("refquerycanary").doesNotContain("reffragcanary")
				.doesNotContain("/a/");
		for (String value : FORWARDED) {
			assertThat(body).doesNotContain(value);
		}
		for (Object hash : hashes) {
			assertThat(body).doesNotContain((String) hash);
		}
	}

	@Test
	void AC12_trustedProxyRedirectsLogNoForwardedValueOrAddress(CapturedOutput output) throws Exception {
		String code = create("https://example.com/v2-proxy-logs");
		recorder.settle();
		int windowStart = output.getAll().length();

		sendAc07Redirects(code);
		recorder.settle();

		String window = output.getAll().substring(windowStart);
		assertThat(window.lines().filter(line -> !line.isBlank())).isNotEmpty()
				.allSatisfy(line -> assertThat(jsonMapper.readTree(line).path("requestId").asString()).isNotBlank());
		for (String value : List.of("203.0.113.7", "203.0.113.8", "198.51.100.1", PROXY)) {
			assertThat(window).doesNotContain(value);
		}
	}

	/** AC-7's table, all from the trusted proxy: two for one client, one with a forged left entry, one bare. */
	private void sendAc07Redirects(String code) throws Exception {
		open(code, PROXY, "Mozilla/5.0 Firefox/131.0", "203.0.113.7", null);
		open(code, PROXY, "Mozilla/5.0 Firefox/131.0", "203.0.113.7", null);
		open(code, PROXY, "Mozilla/5.0 Firefox/131.0", "198.51.100.1, 203.0.113.8", null);
		open(code, PROXY, "Mozilla/5.0 Firefox/131.0", null, null);
	}

	private void open(String code, String peerAddress, String userAgent, String forwardedFor, String referer)
			throws Exception {
		MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT)
				.header("User-Agent", userAgent).with(peer(peerAddress));
		if (forwardedFor != null) {
			request.header("X-Forwarded-For", forwardedFor);
		}
		if (referer != null) {
			request.header("Referer", referer);
		}
		assertThat(mockMvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(302);
	}

	private JsonNode stats(String code) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}
}
