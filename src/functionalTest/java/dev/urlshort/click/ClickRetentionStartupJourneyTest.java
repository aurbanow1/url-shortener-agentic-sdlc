package dev.urlshort.click;

import static dev.urlshort.click.ClickRecordingJourneyTest.BROWSER_ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.urlshort.UrlshortApplication;
import dev.urlshort.link.FunctionalClock;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Whole-service starts on a file database prepared before the start, as an Operator's data directory
 * is: an invalid period stops startup and deletes nothing (AC-4), the startup run has deleted the old
 * clicks by the time readiness is reported (AC-7), a directory written at {@code f6dd29e} (schema V2)
 * upgrades in place (AC-13, AC-16), and a held purge deletes nothing and says so (AC-15). Each start is
 * closed in its test, so no armed tick outlives it.
 */
@ExtendWith(OutputCaptureExtension.class)
class ClickRetentionStartupJourneyTest {

	private static final LocalDate T = LocalDate.now(ZoneOffset.UTC);
	private static final String HASH = "f".repeat(64);

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@TempDir
	private Path data;

	@ParameterizedTest
	@ValueSource(strings = { "0", "-5", "ninety" })
	void AC04_anInvalidPeriodStopsTheServiceFromStarting(String value, CapturedOutput output) {
		String url = database(null);
		JdbcClient jdbc = jdbc(url);
		long linkId = link(jdbc, "Invalid1");
		click(jdbc, linkId, T.minusDays(100), 3);
		int windowStart = output.getAll().length();

		assertThatThrownBy(() -> start(url, "--urlshort.click.purge-enabled=true",
				"--urlshort.click.retention-days=" + value)).isInstanceOf(Exception.class);

		String window = output.getAll().substring(windowStart);
		List<JsonNode> report = events(window).stream()
				.filter(event -> event.path("log").path("logger").asString().endsWith("LoggingFailureAnalysisReporter"))
				.toList();
		assertThat(report).singleElement().satisfies(event -> {
			String message = event.path("message").asString();
			assertThat(event.path("log").path("level").asString()).isEqualTo("ERROR");
			assertThat(message).containsAnyOf("retention-days", "retentionDays").contains(value);
			assertThat(message).doesNotContain("jdbc:").doesNotContain("datasource").doesNotContain("password");
		});
		assertThat(window).doesNotContain("clicks purged");
		assertThat(jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single()).isEqualTo(3);
	}

	@Test
	void AC07_aPurgeRunsAtStartup(CapturedOutput output) {
		String url = database(null);
		JdbcClient jdbc = jdbc(url);
		long linkId = link(jdbc, "Startup1");
		click(jdbc, linkId, T.minusDays(100), 2);
		click(jdbc, linkId, T.minusDays(10), 2);
		int windowStart = output.getAll().length();

		try (ConfigurableApplicationContext ignored = start(url, "--urlshort.click.purge-enabled=true")) {
			// run() returns once readiness is reported, which follows the awaited startup run
			assertThat(jdbc.sql("SELECT clicked_on FROM click").query(LocalDate.class).list())
					.containsExactly(T.minusDays(10), T.minusDays(10));
		}
		assertThat(events(output.getAll().substring(windowStart))).filteredOn(this::purged).singleElement()
				.satisfies(event -> assertThat(event.path("deleted").asInt()).isEqualTo(2));
	}

	@Test
	void AC13_AC16_anExistingDataDirectoryUpgradesInPlace() throws Exception {
		String url = database("2");
		JdbcClient jdbc = jdbc(url);
		long active = link(jdbc, "Upgrade1");
		long retired = link(jdbc, "Retired1");
		jdbc.sql("UPDATE link SET retired_at = CURRENT_TIMESTAMP WHERE id = :id").param("id", retired).update();
		jdbc.sql("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, before_state,"
				+ " after_state) VALUES (CURRENT_TIMESTAMP, 'anonymous', 'link.create', 'link', 'Upgrade1', 'req-1',"
				+ " NULL, '{\"code\":\"Upgrade1\"}'), (CURRENT_TIMESTAMP, 'anonymous', 'link.retire', 'link',"
				+ " 'Retired1', 'req-2', '{\"state\":\"active\"}', '{\"state\":\"retired\"}')").update();
		for (int age : new int[] { 10, 90, 91, 120 }) {
			click(jdbc, active, T.minusDays(age), 1);
		}
		List<Map<String, Object>> links = jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows();
		List<Map<String, Object>> audit = jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
		List<Map<String, Object>> kept = jdbc.sql("SELECT * FROM click WHERE clicked_on >= :cutoff ORDER BY id")
				.param("cutoff", T.minusDays(90)).query().listOfRows();
		List<Map<String, Object>> classes = jdbc.sql("SELECT * FROM user_agent_class ORDER BY token").query().listOfRows();

		try (ConfigurableApplicationContext context = start(url, "--urlshort.click.purge-enabled=true",
				"--spring.flyway.target=3")) {
			assertThat(jdbc.sql("SELECT \"version\", \"success\" FROM \"flyway_schema_history\""
					+ " WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"")
					.query().listOfRows()).extracting(row -> row.get("version") + "=" + row.get("success"))
					.containsExactly("1=true", "2=true", "3=true");
			assertThat(jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows()).isEqualTo(links);
			assertThat(jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows()).isEqualTo(audit);
			assertThat(jdbc.sql("SELECT id, link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash"
					+ " FROM click ORDER BY id").query().listOfRows()).isEqualTo(kept).hasSize(2);
			assertThat(jdbc.sql("SELECT token FROM user_agent_class ORDER BY token").query().listOfRows())
					.isEqualTo(classes);
			assertThat(jdbc.sql("SELECT COUNT(*) FROM click WHERE created_at = clicked_at AND updated_at = clicked_at"
					+ " AND created_by = 'anonymous' AND updated_by = 'anonymous'").query(Long.class).single()).isEqualTo(2);
			assertThat(jdbc.sql("SELECT COUNT(*) FROM user_agent_class WHERE updated_at = created_at"
					+ " AND created_by = 'system' AND updated_by = 'system'").query(Long.class).single()).isEqualTo(4);

			HttpResponse<String> redirect = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(
					"http://localhost:" + context.getEnvironment().getProperty("local.server.port") + "/Upgrade1"))
					.header("Accept", BROWSER_ACCEPT).build(), HttpResponse.BodyHandlers.ofString());
			context.getBean(ClickRecorder.class).settle();

			assertThat(redirect.statusCode()).isEqualTo(302);
			assertThat(jdbc.sql("SELECT created_at = updated_at AND created_by = 'anonymous' AND updated_by = 'anonymous'"
					+ " FROM click WHERE clicked_on = :today").param("today", T).query(Boolean.class).list())
					.containsExactly(true);
		}
	}

	@Test
	void AC15_aPausedPurgeDeletesNothingAndSaysSoAtEveryStart(CapturedOutput output) throws Exception {
		String url = database(null);
		JdbcClient jdbc = jdbc(url);
		long linkId = link(jdbc, "Paused01");
		click(jdbc, linkId, T.minusDays(100), 2);
		click(jdbc, linkId, T.minusDays(10), 2);
		int windowStart = output.getAll().length();

		try (ConfigurableApplicationContext context = start(url, "--urlshort.click.purge-enabled=false")) {
			FunctionalClock clock = context.getBean(FunctionalClock.class);
			// with the hold no tick is armed; more than one tick of real time stands for the 60 s
			clock.shift(Duration.ofSeconds(60));
			Thread.sleep(ClickPurge.TICK.plusSeconds(1).toMillis());
			assertThat(jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single()).as("60 s after readiness")
					.isEqualTo(4);

			clock.reset();
			clock.shift(Duration.between(clock.instant(),
					T.plusDays(1).atTime(ClickPurge.DAILY_AT).toInstant(ZoneOffset.UTC).plusSeconds(60)));
			Thread.sleep(ClickPurge.TICK.plusSeconds(1).toMillis());
			assertThat(jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single())
					.as("60 s after the next scheduled time").isEqualTo(4);
		}
		List<JsonNode> events = events(output.getAll().substring(windowStart));
		assertThat(events).filteredOn(event -> event.path("message").asString()
				.equals("click purge paused, no click is deleted")).singleElement().satisfies(event -> {
					assertThat(event.path("log").path("level").asString()).isEqualTo("WARN");
					assertThat(event.path("setting").asString()).isEqualTo("urlshort.click.purge-enabled");
				});
		assertThat(events).noneMatch(this::purged);
	}

	/** A file database under the temporary directory, migrated to {@code target} (latest when null). */
	private String database(String target) {
		String url = "jdbc:h2:file:" + data.resolve("urlshort").toAbsolutePath() + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
		Flyway.configure().dataSource(url, "sa", "").target(target == null ? "latest" : target).load().migrate();
		return url;
	}

	private static JdbcClient jdbc(String url) {
		return JdbcClient.create(new DriverManagerDataSource(url, "sa", ""));
	}

	private static long link(JdbcClient jdbc, String code) {
		jdbc.sql("INSERT INTO link (code, url, created_at) VALUES (:code, :url, CURRENT_TIMESTAMP)").param("code", code)
				.param("url", "https://example.com/" + code).update();
		return jdbc.sql("SELECT id FROM link WHERE code = :code").param("code", code).query(Long.class).single();
	}

	/** {@code count} clicks on {@code day}, with the v1 column list the shipped writer uses. */
	private static void click(JdbcClient jdbc, long linkId, LocalDate day, int count) {
		for (int i = 0; i < count; i++) {
			jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
					+ " VALUES (:linkId, :at, :on, 'https://ref.example', 'browser', :hash)").param("linkId", linkId)
					.param("at", day.atTime(12, 0, i).atOffset(ZoneOffset.UTC)).param("on", day).param("hash", HASH)
					.update();
		}
	}

	private static ConfigurableApplicationContext start(String url, String... settings) {
		List<String> args = new ArrayList<>(List.of("--spring.datasource.url=" + url, "--server.port=0"));
		args.addAll(List.of(settings));
		return new SpringApplicationBuilder(UrlshortApplication.class).registerShutdownHook(false)
				.run(args.toArray(String[]::new));
	}

	private List<JsonNode> events(String window) {
		return window.lines().filter(line -> line.startsWith("{")).map(jsonMapper::readTree).toList();
	}

	private boolean purged(JsonNode event) {
		return event.path("message").asString().equals("clicks purged");
	}
}
