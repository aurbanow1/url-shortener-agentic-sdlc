package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import dev.urlshort.UrlshortApplication;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * AC-7 and business rule 6 (slice 04-audit-columns): a database in the shipped shapes before this slice
 * (every migration before V4), holding an active link, a retired one, one whose key was released, and
 * their audit rows (one dated ahead of the clock); the candidate starts on it and V4 backfills every row
 * without changing a shipped value.
 */
class LinkUpgradeJourneyTest {

	private static final String LINK_COLUMNS = "SELECT id, code, url, created_at, retired_at, idempotency_key FROM link ORDER BY id";
	private static final String AUDIT_COLUMNS = "SELECT id, occurred_at, actor, action, entity, entity_id, request_id,"
			+ " before_state, after_state FROM audit_log ORDER BY id";

	@TempDir
	private Path data;

	@Test
	void AC07_anExistingDatabaseUpgradesInPlace() {
		String url = "jdbc:h2:file:" + data.resolve("urlshort") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
		DataSource shipped = new DriverManagerDataSource(url, "sa", "");
		Flyway.configure().dataSource(shipped).target(beforeV4(shipped)).load().migrate();
		JdbcClient before = JdbcClient.create(shipped);
		before.sql("INSERT INTO link (code, url, created_at, retired_at, idempotency_key) VALUES"
				+ " ('Active12', 'https://example.com/a', TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00', NULL, NULL),"
				+ " ('Retire12', 'https://example.com/r', TIMESTAMP WITH TIME ZONE '2026-10-01 09:05:00+00',"
				+ " TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00', NULL),"
				+ " ('Releas12', 'https://example.com/k', TIMESTAMP WITH TIME ZONE '2026-09-29 08:00:00+00', NULL, NULL),"
				+ " ('Bound123', 'https://example.com/k2', TIMESTAMP WITH TIME ZONE '2026-10-01 11:00:00+00', NULL, 'K')").update();
		before.sql("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state)"
				+ " VALUES (TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00', 'anonymous', 'link.create', 'link', 'Active12', 'r-1',"
				+ " NULL, '{\"url\":\"https://example.com/a\",\"state\":\"active\"}'),"
				+ " (TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00', 'anonymous', 'link.retire', 'link', 'Retire12', 'r-2',"
				+ " '{\"url\":\"https://example.com/r\",\"state\":\"active\"}', '{\"url\":\"https://example.com/r\",\"state\":\"retired\"}'),"
				+ " (CURRENT_TIMESTAMP + INTERVAL '1' DAY, 'anonymous', 'link.create', 'link', 'Bound123', 'r-ahead',"
				+ " NULL, '{\"url\":\"https://example.com/k2\",\"state\":\"active\"}')").update();
		List<Map<String, Object>> links = before.sql(LINK_COLUMNS).query().listOfRows();
		List<Map<String, Object>> audits = before.sql(AUDIT_COLUMNS).query().listOfRows();

		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(UrlshortApplication.class)
				.run("--server.port=0", "--server.address=127.0.0.1", "--spring.datasource.url=" + url)) {
			Instant upgraded = Instant.now();
			JdbcClient after = JdbcClient.create(app.getBean(DataSource.class));

			assertThat(after.sql("SELECT \"success\" FROM \"flyway_schema_history\" WHERE \"version\" = '4'").query(Boolean.class)
					.single()).isTrue();
			assertThat(after.sql(LINK_COLUMNS).query().listOfRows()).isEqualTo(links);
			assertThat(after.sql(AUDIT_COLUMNS).query().listOfRows()).isEqualTo(audits);
			for (Map<String, Object> link : after.sql("SELECT * FROM link").query().listOfRows()) {
				Object latest = link.get("RETIRED_AT") != null ? link.get("RETIRED_AT") : link.get("CREATED_AT");
				assertThat(link.get("UPDATED_AT")).as("%s", link.get("CODE")).isEqualTo(latest);
				assertThat(link).containsEntry("CREATED_BY", "anonymous").containsEntry("UPDATED_BY", "anonymous");
			}
			for (Map<String, Object> audit : after.sql("SELECT * FROM audit_log").query().listOfRows()) {
				Instant written = ((OffsetDateTime) audit.get("CREATED_AT")).toInstant();
				assertThat(audit.get("UPDATED_AT")).isEqualTo(audit.get("CREATED_AT"));
				assertThat(written).as("%s not later than the upgrade", audit.get("REQUEST_ID")).isBeforeOrEqualTo(upgraded);
				Instant occurred = ((OffsetDateTime) audit.get("OCCURRED_AT")).toInstant();
				if (occurred.isAfter(upgraded)) {
					assertThat(audit.get("REQUEST_ID")).as("only the row dated ahead is capped").isEqualTo("r-ahead");
				}
				else {
					assertThat(written).as("%s backfilled with its event time", audit.get("REQUEST_ID")).isEqualTo(occurred);
				}
				assertThat(audit.get("CREATED_BY")).isEqualTo(audit.get("ACTOR"));
				assertThat(audit.get("UPDATED_BY")).isEqualTo(audit.get("ACTOR"));
			}
		}
	}

	/** The last migration before V4: V3 once 02-click-retention's is on the classpath, V2 until then. */
	static MigrationVersion beforeV4(DataSource dataSource) {
		return Arrays.stream(Flyway.configure().dataSource(dataSource).load().info().all()).map(MigrationInfo::getVersion)
				.filter(version -> version.compareTo(MigrationVersion.fromVersion("4")) < 0).max(Comparator.naturalOrder())
				.orElseThrow();
	}
}
