package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static dev.urlshort.click.ClickRecordingJourneyTest.peer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import dev.urlshort.link.FunctionalClock;
import org.junit.jupiter.api.AfterEach;
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
 * The purge on the default period (AC-1, AC-2, AC-5, AC-6, AC-9, AC-11; business rules 2, 4, 5 and 6).
 * Its own database, so its deletions never reach the shared suite's. Clicks at past days are recorded
 * by real redirects with this context's clock shifted, so the purge is proven on the day the recorder
 * stores. "Run a purge" is {@link ClickPurge#runNow()}, the run the scheduler starts, on its thread.
 *
 * <p>The rate limiter keeps a client's bucket at the latest time it was used and refuses a client whose
 * clock steps back by more than its tolerance, so every test sends its shifted requests from a peer of
 * its own, in increasing time; {@code 127.0.0.1} only ever sends at the unshifted clock.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:click-retention;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ClickRetentionJourneyTest {

	private static final LocalDate T = LocalDate.now(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ClickRecorder recorder;

	@Autowired
	private ClickPurge purge;

	@Autowired
	private FunctionalClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void AC01_theBoundaryDayIsKeptAndTheDayBeforeItIsDeleted() throws Exception {
		String peer = "10.0.41.1";
		String code = create("https://example.com/boundary");
		openOn(code, peer, null, 2, T.minusDays(91), T.minusDays(90), T.minusDays(89), T);
		List<Map<String, Object>> kept = rows(code, T.minusDays(90));

		at(T);
		purge.runNow();

		assertThat(rows(code, LocalDate.MIN)).isEqualTo(kept).hasSize(6);
	}

	@Test
	void AC02_theWindowMovesWithTheDay() throws Exception {
		String peer = "10.0.41.2";
		String code = create("https://example.com/window");
		openOn(code, peer, null, 2, T.minusDays(91), T.minusDays(90), T.minusDays(89), T);
		at(T);
		purge.runNow();
		List<Map<String, Object>> kept = rows(code, T.minusDays(89));

		at(T.plusDays(1));
		purge.runNow();

		assertThat(rows(code, LocalDate.MIN)).isEqualTo(kept).hasSize(4);
	}

	@Test
	void AC05_statisticsCoverTheRetainedClicksOnly() throws Exception {
		String peer = "10.0.41.5";
		String code = create("https://example.com/retained-stats");
		for (int age = 95; age >= 91; age--) {
			openOn(code, peer, "https://old" + age + ".example/p", 1, T.minusDays(age));
		}
		openOn(code, peer, "https://kept.example/p", 2, T.minusDays(90), T.minusDays(89), T.minusDays(30), T);
		openOn(code, peer, null, 1, T);
		JsonNode before = stats(code, peer);

		purge.runNow();
		JsonNode after = stats(code, peer);

		List<JsonNode> retainedDays = new ArrayList<>();
		long sum = 0;
		for (JsonNode day : before.get("clicksPerDay")) {
			if (!LocalDate.parse(day.get("date").asString()).isBefore(T.minusDays(90))) {
				retainedDays.add(day);
				sum += day.get("clicks").asLong();
			}
		}
		assertThat(before.get("totalClicks").asLong()).isEqualTo(14);
		assertThat(after.propertyNames()).containsExactlyInAnyOrder("code", "totalClicks", "clicksPerDay",
				"topReferrers");
		assertThat(after.get("clicksPerDay")).containsExactlyElementsOf(retainedDays);
		assertThat(after.get("totalClicks").asLong()).isEqualTo(sum).isEqualTo(9);
		assertThat(after.get("topReferrers")).containsExactly(
				jsonMapper.readTree("{\"referrer\":\"https://kept.example\",\"clicks\":8}"));
	}

	@Test
	void AC06_linksRedirectsAndTheAuditTrailAreUntouched() throws Exception {
		String peer = "10.0.41.6";
		JsonNode created = jsonMapper.readTree(createBody("https://example.com/only-old"));
		String code = created.get("code").asString();
		openOn(code, peer, null, 3, T.minusDays(100));
		List<Map<String, Object>> audit = jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();

		at(T);
		purge.runNow();
		JsonNode empty = stats(code, peer);
		MockHttpServletResponse redirect = mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT)
				.with(peer(peer))).andReturn().getResponse();
		recorder.settle();
		JsonNode one = stats(code, peer);
		MockHttpServletResponse read = mockMvc.perform(get("/api/links/" + code).with(peer(peer))).andReturn()
				.getResponse();

		assertThat(empty).isEqualTo(jsonMapper.readTree(
				"{\"code\":\"" + code + "\",\"totalClicks\":0,\"clicksPerDay\":[],\"topReferrers\":[]}"));
		assertThat(redirect.getStatus()).isEqualTo(302);
		assertThat(redirect.getHeader("Location")).isEqualTo("https://example.com/only-old");
		assertThat(one.get("totalClicks").asLong()).isEqualTo(1);
		assertThat(one.get("clicksPerDay")).containsExactly(
				jsonMapper.readTree("{\"date\":\"" + T + "\",\"clicks\":1}"));
		assertThat(read.getStatus()).isEqualTo(200);
		assertThat(jsonMapper.readTree(read.getContentAsString())).isEqualTo(created);
		assertThat(jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows()).isEqualTo(audit);
	}

	@Test
	void AC09_eachRunLogsOneInfoWithItsCountCutoffAndPeriodAndNoClickValue(CapturedOutput output) throws Exception {
		String peer = "10.0.41.9";
		String code = create("https://example.com/purge-log");
		openOn(code, peer, "https://logcanary.example/p", 3, T.minusDays(120));
		long linkId = jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).single();
		String hash = jdbc.sql("SELECT client_hash FROM click WHERE link_id = :id FETCH FIRST 1 ROW ONLY")
				.param("id", linkId).query(String.class).single();
		at(T);

		for (int expected : new int[] { 3, 0 }) {
			int windowStart = output.getAll().length();
			purge.runNow();
			List<String> lines = output.getAll().substring(windowStart).lines().filter(line -> !line.isBlank())
					.toList();

			assertThat(lines).singleElement().satisfies(line -> {
				JsonNode event = jsonMapper.readTree(line);
				assertThat(event.path("log").path("level").asString()).isEqualTo("INFO");
				assertThat(event.path("message").asString()).isEqualTo("clicks purged");
				assertThat(event.path("deleted").asInt()).as("deleted").isEqualTo(expected);
				assertThat(event.path("cutoff").asString()).isEqualTo(T.minusDays(90).toString());
				assertThat(event.path("retentionDays").asInt()).isEqualTo(90);
				assertThat(event.has("requestId")).isFalse();
				assertThat(event.propertyNames()).doesNotContain("linkId", "code", "referrer", "clientHash",
						"userAgentClass");
				assertThat(event.propertyNames()).doesNotContain("linkId", "link", "id");
				assertThat(line).doesNotContain(code).doesNotContain(hash).doesNotContain("logcanary")
						.doesNotContain("browser");
			});
		}
	}

	@Test
	void AC11_aRedirectWhileOldClicksAreBeingDeletedIsServedAndCounted() throws Exception {
		String peer = "10.0.41.11";
		String code = create("https://example.com/during-purge");
		long linkId = jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).single();
		insertOld(linkId, 50_000);
		at(T);

		// (a) a deletion of the old clicks holds its row locks, uncommitted, while the Visitor is served
		try (Connection deleting = dataSource.getConnection()) {
			deleting.setAutoCommit(false);
			try (PreparedStatement delete = deleting
					.prepareStatement("DELETE FROM click WHERE clicked_on < ? AND link_id = ?")) {
				delete.setDate(1, Date.valueOf(T.minusDays(90)));
				delete.setLong(2, linkId);
				assertThat(delete.executeUpdate()).isEqualTo(50_000);
			}
			assertThat(redirect(code, peer).getStatus()).isEqualTo(302);
			recorder.settle();
			assertThat(stats(code, peer).get("clicksPerDay")).contains(
					jsonMapper.readTree("{\"date\":\"" + T + "\",\"clicks\":1}"));
			deleting.rollback();
		}

		// (b) redirects sent while the purge itself deletes them
		CompletableFuture<Void> run = CompletableFuture.runAsync(() -> {
			try {
				purge.runNow();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
		int sent = 0;
		int startedDuringRun = 0;
		do {
			startedDuringRun += run.isDone() ? 0 : 1;
			assertThat(redirect(code, peer).getStatus()).isEqualTo(302);
			sent++;
		}
		while (!run.isDone());
		run.join();
		recorder.settle();

		assertThat(startedDuringRun).as("redirects started before the run ended").isPositive();
		assertThat(stats(code, peer).get("clicksPerDay")).containsExactly(
				jsonMapper.readTree("{\"date\":\"" + T + "\",\"clicks\":" + (sent + 1) + "}"));
	}

	/** Records {@code times} clicks on each day, at noon of this context's clock, from {@code peer}. */
	private void openOn(String code, String peer, String referer, int times, LocalDate... days) throws Exception {
		for (LocalDate day : days) {
			at(day);
			for (int i = 0; i < times; i++) {
				MockHttpServletRequestBuilder request = get("/" + code).header("Accept", BROWSER_ACCEPT)
						.with(peer(peer));
				if (referer != null) {
					request.header("Referer", referer);
				}
				assertThat(mockMvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(302);
			}
		}
		recorder.settle();
	}

	private void at(LocalDate day) {
		clock.reset();
		clock.shift(Duration.between(clock.instant(), day.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)));
	}

	private MockHttpServletResponse redirect(String code, String peer) throws Exception {
		return mockMvc.perform(get("/" + code).header("Accept", BROWSER_ACCEPT).with(peer(peer))).andReturn()
				.getResponse();
	}

	/** The link's click rows on or after {@code from}, every column. */
	private List<Map<String, Object>> rows(String code, LocalDate from) {
		return jdbc.sql("SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code"
				+ " AND c.clicked_on >= :from ORDER BY c.id").param("code", code).param("from", from).query().listOfRows();
	}

	private void insertOld(long linkId, int count) {
		Instant at = T.minusDays(100).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC);
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " SELECT :linkId, :at, :on, NULL, 'browser', :hash FROM SYSTEM_RANGE(1, :count)")
				.param("linkId", linkId).param("at", at.atOffset(ZoneOffset.UTC)).param("on", T.minusDays(100))
				.param("hash", "c".repeat(64)).param("count", count).update();
	}

	private JsonNode stats(String code, String peer) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/api/links/" + code + "/stats").with(peer(peer)))
				.andReturn().getResponse();
		assertThat(response.getStatus()).isEqualTo(200);
		return jsonMapper.readTree(response.getContentAsString());
	}

	private String create(String url) throws Exception {
		return jsonMapper.readTree(createBody(url)).get("code").asString();
	}

	private String createBody(String url) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of("url", url)))).andReturn().getResponse()
				.getContentAsString();
	}
}
