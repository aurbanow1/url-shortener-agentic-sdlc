package dev.urlshort.audit;

import static dev.urlshort.audit.AuditForwardedHeadersJourneyTest.base;
import static dev.urlshort.audit.AuditForwardedHeadersJourneyTest.get;
import static dev.urlshort.audit.AuditForwardedHeadersJourneyTest.start;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-18: a database written in the shapes of the shipped service ({@code main} {@code f6dd29e}, schema
 * V2) holding an active and a retired link, clicks and audit rows; the candidate starts on it, every
 * link answers as before and {@code GET /api/audit} returns every pre-existing row as stored. The proof
 * by effect with the real {@code f6dd29e} jar is in the slice's {@code proof/}.
 */
class AuditUpgradeJourneyTest {

	private static final String ACTIVE = "Upgr4ctive";
	private static final String RETIRED = "Upgr3tired";
	private static final String KEPT = "https://example.com/kept?u=1";
	private static final String GONE = "https://example.com/gone";
	private static final String[][] ROWS = {
		{ "2026-10-01T09:00:00Z", "link.create", ACTIVE, "req-create-active", null, "{\"url\":\"" + KEPT + "\",\"state\":\"active\"}" },
		{ "2026-10-01T09:05:00Z", "link.create", RETIRED, "req-create-retired", null, "{\"url\":\"" + GONE + "\",\"state\":\"active\"}" },
		{ "2026-10-01T09:10:00Z", "link.retire", RETIRED, "req-retire", "{\"url\":\"" + GONE + "\",\"state\":\"active\"}",
			"{\"url\":\"" + GONE + "\",\"state\":\"retired\"}" } };

	private final JsonMapper json = JsonMapper.builder().build();

	@TempDir
	private Path data;

	@Test
	void AC18_anExistingDatabaseUpgradesInPlaceAndKeepsItsLinksAndRows() throws Exception {
		String url = "jdbc:h2:file:" + data.resolve("urlshort") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
		Flyway.configure().dataSource(url, "sa", "").target("2").load().migrate();
		writeShippedRows(url);

		try (ConfigurableApplicationContext app = start(url)) {
			String base = base(app);

			// statistics first: the redirect below records a click of its own
			assertThat(json.readTree(get(base + "/api/links/" + ACTIVE + "/stats").body())).isEqualTo(json.readTree(
					"{\"code\":\"" + ACTIVE + "\",\"totalClicks\":3,\"clicksPerDay\":[{\"date\":\"2026-10-01\",\"clicks\":3}],"
							+ "\"topReferrers\":[{\"referrer\":\"https://ref.example\",\"clicks\":2}]}"));
			HttpResponse<String> redirect = get(base + "/" + ACTIVE);
			assertThat(redirect.statusCode()).isEqualTo(302);
			assertThat(redirect.headers().firstValue("Location")).hasValue(KEPT);
			assertThat(get(base + "/" + RETIRED).statusCode()).isEqualTo(410);

			JsonNode page = json.readTree(get(base + "/api/audit").body());
			assertThat(page.get("next").isNull()).isTrue();
			assertThat(page.get("items").size()).isEqualTo(ROWS.length);
			for (int i = 0; i < ROWS.length; i++) {
				String[] stored = ROWS[ROWS.length - 1 - i];
				JsonNode row = page.get("items").get(i);
				assertThat(Instant.parse(row.get("occurredAt").asString())).isEqualTo(Instant.parse(stored[0]));
				assertThat(row.get("actor").asString()).isEqualTo("anonymous");
				assertThat(row.get("action").asString()).isEqualTo(stored[1]);
				assertThat(row.get("entity").asString()).isEqualTo("link");
				assertThat(row.get("entityId").asString()).isEqualTo(stored[2]);
				assertThat(row.get("requestId").asString()).isEqualTo(stored[3]);
				if (stored[4] == null) {
					assertThat(row.get("before").isNull()).isTrue();
				}
				else {
					assertThat(row.get("before")).isEqualTo(json.readTree(stored[4]));
				}
				assertThat(row.get("after")).isEqualTo(json.readTree(stored[5]));
			}
		}
	}

	private static void writeShippedRows(String url) throws Exception {
		try (Connection connection = DriverManager.getConnection(url, "sa", ""); Statement s = connection.createStatement()) {
			s.executeUpdate("INSERT INTO link (code, url, created_at) VALUES ('" + ACTIVE + "', '" + KEPT
					+ "', TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00')");
			s.executeUpdate("INSERT INTO link (code, url, created_at, retired_at) VALUES ('" + RETIRED + "', '" + GONE
					+ "', TIMESTAMP WITH TIME ZONE '2026-10-01 09:05:00+00', TIMESTAMP WITH TIME ZONE '2026-10-01 09:10:00+00')");
			for (String referrer : new String[] { "'https://ref.example'", "'https://ref.example'", "NULL" }) {
				s.executeUpdate("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
						+ " SELECT id, TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00', DATE '2026-10-01', " + referrer
						+ ", 'browser', REPEAT('a', 64) FROM link WHERE code = '" + ACTIVE + "'");
			}
			for (String[] row : ROWS) {
				s.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state)"
						+ " VALUES (TIMESTAMP WITH TIME ZONE '" + row[0].replace('T', ' ').replace("Z", "+00") + "', 'anonymous', '"
						+ row[1] + "', 'link', '" + row[2] + "', '" + row[3] + "', " + (row[4] == null ? "NULL" : "'" + row[4] + "'")
						+ ", '" + row[5] + "')");
			}
		}
	}
}
