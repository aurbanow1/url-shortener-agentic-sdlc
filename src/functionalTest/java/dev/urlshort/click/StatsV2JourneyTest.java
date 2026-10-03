package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static dev.urlshort.click.ClickRecordingJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The v2 per-day figures without a trusted proxy (AC-1 to AC-5, AC-8, AC-13; business rules 1 to 6):
 * {@code uniqueVisitors} counts distinct clients within a UTC day, {@code botClicks} counts that day's
 * {@code bot} clicks, and forwarding headers change nothing. Every test creates its own links.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatsV2JourneyTest {

	private static final String BROWSER = "Mozilla/5.0 (X11; Linux x86_64) Firefox/131.0";
	private static final String BOT = "Mozilla/5.0 (compatible; Googlebot/2.1)";

	// day-shifted requests leave this peer's bucket a day ahead, so no other test uses it
	private static final String SHIFTED_PEER = "203.0.113.31";

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
	void AC01_thePerDayElementCarriesFourFigures() throws Exception {
		String code = create("https://example.com/v2-one");
		String never = create("https://example.com/v2-never");
		open(code, "203.0.113.21", BROWSER, null);
		recorder.settle();

		JsonNode stats = stats(code);

		assertThat(stats.propertyNames()).containsExactly("code", "totalClicks", "clicksPerDay", "topReferrers");
		assertThat(stats.get("clicksPerDay")).containsExactly(day(today(), 1, 1, 0));
		assertThat(stats.get("clicksPerDay").get(0).propertyNames()).containsExactly("date", "clicks",
				"uniqueVisitors", "botClicks");
		assertThat(stats(never)).isEqualTo(jsonMapper.readTree(
				"{\"code\":\"" + never + "\",\"totalClicks\":0,\"clicksPerDay\":[],\"topReferrers\":[]}"));
	}

	@Test
	void AC02_uniqueVisitorsCountDistinctClientsWithinAUtcDay() throws Exception {
		String code = create("https://example.com/v2-uniques");
		for (int i = 0; i < 3; i++) {
			open(code, "203.0.113.1", BROWSER, null);
		}
		for (int i = 0; i < 2; i++) {
			open(code, "203.0.113.2", BROWSER, null);
		}
		open(code, "203.0.113.3", BROWSER, null);
		recorder.settle();

		assertThat(stats(code).get("clicksPerDay")).containsExactly(day(today(), 6, 3, 0));
	}

	@Test
	void AC03_uniquesArePerUtcDayAndNeverCombinedAcrossDays() throws Exception {
		String code = create("https://example.com/v2-days");
		LocalDate d = today().minusDays(10);
		at(d.atTime(23, 59, 59).toInstant(ZoneOffset.UTC));
		open(code, SHIFTED_PEER, BROWSER, null);
		at(d.plusDays(1).atStartOfDay().plusSeconds(1).toInstant(ZoneOffset.UTC));
		open(code, SHIFTED_PEER, BROWSER, null);
		recorder.settle();
		clock.reset();

		JsonNode stats = stats(code);

		assertThat(stats.get("clicksPerDay")).containsExactly(day(d, 1, 1, 0), day(d.plusDays(1), 1, 1, 0));
		assertThat(stats.propertyNames()).containsExactly("code", "totalClicks", "clicksPerDay", "topReferrers");
	}

	@Test
	void AC04_botClicksAreCountedPerDayAndNothingElseChangesMeaning() throws Exception {
		String code = create("https://example.com/v2-bots");
		List<String> userAgents = Arrays.asList("Mozilla/5.0 (X11; Linux x86_64) Firefox/131.0",
				"Mozilla/5.0 (compatible; Googlebot/2.1)", "acme-crawler/1.0", "acme-spider", "curl/8.7.1", null);
		for (int i = 0; i < userAgents.size(); i++) {
			open(code, "203.0.113." + (41 + i), userAgents.get(i), null);
		}
		recorder.settle();

		JsonNode stats = stats(code);

		assertThat(stats.get("clicksPerDay")).containsExactly(day(today(), 6, 6, 3));
		assertThat(stats.get("totalClicks").asLong()).isEqualTo(6);
	}

	@Test
	void AC05_oneClientsBotAndBrowserClicksAreOneVisitor() throws Exception {
		String code = create("https://example.com/v2-mixed");
		open(code, "203.0.113.9", BROWSER, null);
		open(code, "203.0.113.9", BOT, null);
		recorder.settle();

		assertThat(stats(code).get("clicksPerDay")).containsExactly(day(today(), 2, 1, 1));
	}

	@Test
	void AC08_withoutATrustedProxyForwardingHeadersChangeNothing() throws Exception {
		String code = create("https://example.com/v2-no-proxy");
		List<String> forwarded = List.of("192.0.2.81", "192.0.2.82, 198.51.100.83", "203.0.113.84");
		for (String value : forwarded) {
			// the SPEC's 203.0.113.77 is v1's AC-5 peer, which that test sends from a September clock;
			// sending from it at today's clock first would leave its bucket ahead and refuse AC-5
			open(code, "203.0.113.87", BROWSER, value);
		}
		recorder.settle();

		assertThat(stats(code).get("clicksPerDay")).containsExactly(day(today(), 3, 1, 0));
		String rows = clickRows(code).toString();
		for (String value : List.of("192.0.2.81", "192.0.2.82", "198.51.100.83", "203.0.113.84")) {
			assertThat(rows).doesNotContain(value);
		}
	}

	@Test
	void AC13_theApiDocumentDescribesTheV2PerDayElement() throws Exception {
		JsonNode document = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse()
				.getContentAsString());

		JsonNode ok = document.at("/paths/~1api~1links~1{code}~1stats/get/responses/200/content/application~1json");
		JsonNode stats = schema(document, ok.at("/schema/$ref").asString());
		JsonNode element = schema(document, stats.at("/properties/clicksPerDay/items/$ref").asString());
		assertThat(element.get("properties").propertyNames()).containsExactlyInAnyOrder("date", "clicks",
				"uniqueVisitors", "botClicks");
		JsonNode value = ok.at("/examples/stats/value");
		JsonNode example = value.isString() ? jsonMapper.readTree(value.asString()) : value;
		assertThat(example.get("clicksPerDay")).isNotEmpty().allSatisfy(day -> assertThat(day.propertyNames())
				.containsExactly("date", "clicks", "uniqueVisitors", "botClicks"));
	}

	private JsonNode schema(JsonNode document, String ref) {
		return document.at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1));
	}

	private JsonNode day(LocalDate date, long clicks, long uniqueVisitors, long botClicks) {
		return jsonMapper.readTree("{\"date\":\"" + date + "\",\"clicks\":" + clicks + ",\"uniqueVisitors\":"
				+ uniqueVisitors + ",\"botClicks\":" + botClicks + "}");
	}

	private LocalDate today() {
		return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
	}

	private void at(Instant instant) {
		clock.reset();
		clock.shift(Duration.between(clock.instant(), instant));
	}

	private void open(String code, String peerAddress, String userAgent, String forwardedFor) throws Exception {
		MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT)
				.with(peer(peerAddress));
		if (userAgent != null) {
			request.header("User-Agent", userAgent);
		}
		if (forwardedFor != null) {
			request.header("X-Forwarded-For", forwardedFor);
		}
		assertThat(mockMvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(302);
	}

	private JsonNode stats(String code) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/api/links/" + code + "/stats")).andReturn()
				.getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	private List<Map<String, Object>> clickRows(String code) {
		return jdbc.sql("SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code")
				.param("code", code).query().listOfRows();
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString()).get("code").asString();
	}
}
