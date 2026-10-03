package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recording a click on every {@code 302} redirect and nothing else, reduced before it is stored:
 * AC-1 to AC-6, AC-17, AC-18 and business rules 1 to 4. Each test creates its own links and reads only their
 * clicks, because the in-memory database is shared by every context of the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClickRecordingJourneyTest {

	static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,*/*;q=0.8";

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
	void AC01_aRedirectRecordsExactlyOneClickWithItsTime() throws Exception {
		String code = create("https://example.com/one");
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		int status = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)).andReturn().getResponse()
				.getStatus();
		Instant after = Instant.now();

		assertThat(status).isEqualTo(302);
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (clicks(code).isEmpty() && System.nanoTime() < deadline) {
			Thread.onSpinWait();
		}
		assertThat(clicks(code)).as("visible within rule 6's five seconds").hasSize(1);
		recorder.settle();
		List<Map<String, Object>> clicks = clicks(code);
		assertThat(clicks).hasSize(1);
		assertThat(clickedAt(clicks.getFirst()).truncatedTo(ChronoUnit.SECONDS)).isBetween(before, after);
	}

	@Test
	void AC02_onlyARedirectIsAClick() throws Exception {
		String active = create("https://example.com/active");
		String retired = create("https://example.com/retired");
		mockMvc.perform(delete("/api/links/" + retired));
		long before = allClicks();

		for (MockHttpServletRequestBuilder request : List.of(get("/" + retired).header("Accept", BROWSER_ACCEPT),
				get("/nosuchcode1").header("Accept", BROWSER_ACCEPT), head("/" + active), post("/" + active),
				get("/api/links/" + active), get("/api/links/" + active + "/stats"))) {
			mockMvc.perform(request);
		}
		recorder.settle();

		assertThat(clicks(active)).isEmpty();
		assertThat(clicks(retired)).isEmpty();
		assertThat(allClicks()).isEqualTo(before);
	}

	static Stream<Arguments> AC03_referrers() {
		return Stream.of(
				Arguments.of("https://News.Example/a/refpathcanary?t=refquerycanary#reffragcanary", "https://news.example"),
				Arguments.of("http://blog.example:8081/post", "http://blog.example:8081"),
				Arguments.of("https://refusercanary:pw@forum.example/x", "https://forum.example"),
				Arguments.of(null, null),
				Arguments.of("android-app://com.example.app/", null),
				Arguments.of("not a url", null),
				Arguments.of("https://long.example/" + "a".repeat(2049 - "https://long.example/".length()), null));
	}

	@ParameterizedTest
	@MethodSource("AC03_referrers")
	void AC03_theReferrerIsStoredAsItsOriginOnly(String referer, String stored) throws Exception {
		String code = create("https://example.com/ref");
		MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT);
		if (referer != null) {
			request.header("Referer", referer);
		}

		mockMvc.perform(request);
		recorder.settle();

		List<Map<String, Object>> clicks = clicks(code);
		assertThat(clicks).singleElement().satisfies(click -> assertThat(click.get("REFERRER")).isEqualTo(stored));
		assertThat(clicks.toString()).doesNotContain("refpathcanary").doesNotContain("refquerycanary")
				.doesNotContain("reffragcanary").doesNotContain("refusercanary");
	}

	static Stream<Arguments> AC04_userAgents() {
		return Stream.of(
				Arguments.of("Mozilla/5.0 (X11; Linux x86_64) uacanary Firefox/131.0", "browser"),
				Arguments.of("Mozilla/5.0 (compatible; Googlebot/2.1; uacanary)", "bot"),
				Arguments.of("uacanary-crawler/1.0", "bot"),
				Arguments.of("uacanary-spider", "bot"),
				Arguments.of("curl/8.7.1 uacanary", "other"),
				Arguments.of(null, "unknown"),
				Arguments.of("", "unknown"));
	}

	@ParameterizedTest
	@MethodSource("AC04_userAgents")
	void AC04_theUserAgentIsStoredAsAClassOnly(String userAgent, String userAgentClass) throws Exception {
		String code = create("https://example.com/ua");
		MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT);
		if (userAgent != null) {
			request.header("User-Agent", userAgent);
		}

		mockMvc.perform(request);
		recorder.settle();

		List<Map<String, Object>> clicks = clicks(code);
		assertThat(clicks).singleElement()
				.satisfies(click -> assertThat(click.get("USER_AGENT_CLASS")).isEqualTo(userAgentClass));
		assertThat(clicks.toString()).doesNotContain("uacanary");
	}

	@Test
	void AC05_theClientAddressIsStoredOnlyAsASaltedHashThatRotatesEveryUtcDay() throws Exception {
		String code = create("https://example.com/hash");

		redirectAt(code, "2026-09-01T00:00:01Z", "203.0.113.77");
		redirectAt(code, "2026-09-01T23:59:59Z", "203.0.113.77");
		redirectAt(code, "2026-09-01T12:00:00Z", "198.51.100.23");
		redirectAt(code, "2026-09-02T00:00:01Z", "203.0.113.77");
		recorder.settle();

		List<String> hashes = clicks(code).stream().map(click -> (String) click.get("CLIENT_HASH")).toList();
		assertThat(hashes).hasSize(4).allMatch(hash -> hash.matches("[0-9a-f]{64}"));
		assertThat(hashes.get(1)).isEqualTo(hashes.get(0));
		assertThat(hashes.get(2)).isNotEqualTo(hashes.get(0));
		assertThat(hashes.get(3)).isNotEqualTo(hashes.get(0));
		String stored = clicks(code).toString();
		assertThat(stored).doesNotContain("203.0.113.77").doesNotContain("198.51.100.23");
		for (String address : List.of("203.0.113.77", "198.51.100.23")) {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
			assertThat(hashes).doesNotContain(HexFormat.of().formatHex(digest),
					Base64.getEncoder().encodeToString(digest));
		}
	}

	@Test
	void AC06_forwardingHeadersDoNotChangeTheRecordedClient() throws Exception {
		String code = create("https://example.com/forwarded");

		mockMvc.perform(get("/" + code).with(peer("203.0.113.77")));
		mockMvc.perform(get("/" + code).with(peer("203.0.113.77")).header("X-Forwarded-For", "192.0.2.99")
				.header("Forwarded", "for=192.0.2.99"));
		recorder.settle();

		List<Map<String, Object>> clicks = clicks(code);
		assertThat(clicks).hasSize(2);
		assertThat(clicks.get(1).get("CLIENT_HASH")).isEqualTo(clicks.get(0).get("CLIENT_HASH"));
		assertThat(clicks.toString()).doesNotContain("192.0.2.99");
	}

	@Test
	void AC17_theStatisticsExposeAggregatesOnly() throws Exception {
		String code = create("https://example.com/aggregates");
		mockMvc.perform(get("/" + code).with(peer("203.0.113.77")).header("User-Agent", "Mozilla/5.0 uacanary")
				.header("Referer", "https://News.Example/a/refpathcanary?t=refquerycanary#reffragcanary"));
		mockMvc.perform(get("/" + code).with(peer("198.51.100.23")).header("User-Agent", "uacanary-crawler/1.0"));
		recorder.settle();
		String hash = (String) clicks(code).getFirst().get("CLIENT_HASH");

		String body = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn().getResponse().getContentAsString();

		tools.jackson.databind.JsonNode stats = jsonMapper.readTree(body);
		assertThat(stats.propertyNames()).containsExactlyInAnyOrder("code", "totalClicks", "clicksPerDay",
				"topReferrers");
		stats.get("clicksPerDay").forEach(day -> assertThat(day.propertyNames()).containsExactlyInAnyOrder("date", "clicks",
				"uniqueVisitors", "botClicks"));
		stats.get("topReferrers")
				.forEach(ref -> assertThat(ref.propertyNames()).containsExactlyInAnyOrder("referrer", "clicks"));
		assertThat(body).doesNotContain("203.0.113.77").doesNotContain("198.51.100.23").doesNotContain(hash)
				.doesNotContain("browser").doesNotContain("\"bot\"").doesNotContain("uacanary")
				.doesNotContain("refpathcanary").doesNotContain("refquerycanary").doesNotContain("reffragcanary")
				.doesNotContain("/a/");
	}

	@Test
	void AC18_noClickDataReachesTheLogs(CapturedOutput output) throws Exception {
		String code = create("https://example.com/no-logs");
		recorder.settle();
		int windowStart = output.getAll().length();

		mockMvc.perform(get("/" + code).with(peer("203.0.113.77")).header("User-Agent", "Mozilla/5.0 logcanaryua")
				.header("Referer", "https://logorigin.example/logcanarypath?q=logcanaryquery")
				.header("X-Forwarded-For", "192.0.2.123"));
		recorder.settle();
		mockMvc.perform(get("/api/links/" + code + "/stats"));
		String window = output.getAll().substring(windowStart);

		String hash = (String) clicks(code).getFirst().get("CLIENT_HASH");
		assertThat(window).isNotBlank().doesNotContain("logcanaryua").doesNotContain("logcanarypath")
				.doesNotContain("logcanaryquery").doesNotContain("192.0.2.123").doesNotContain("203.0.113.77")
				.doesNotContain(hash).doesNotContain("logorigin.example");
	}

	private void redirectAt(String code, String instant, String address) throws Exception {
		clock.reset();
		// from the suite clock's own millisecond tick, so elapsed real time can only move it later
		clock.shift(Duration.between(clock.instant(), Instant.parse(instant)));
		assertThat(mockMvc.perform(get("/" + code).with(peer(address))).andReturn().getResponse().getStatus())
				.isEqualTo(302);
	}

	String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}

	List<Map<String, Object>> clicks(String code) {
		return jdbc.sql("SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code ORDER BY c.id")
				.param("code", code).query().listOfRows();
	}

	private long allClicks() {
		return jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single();
	}

	private static Instant clickedAt(Map<String, Object> click) {
		return ((OffsetDateTime) click.get("CLICKED_AT")).toInstant();
	}

	static RequestPostProcessor peer(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}
}
