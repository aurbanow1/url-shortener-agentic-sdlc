package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The audit-column migration on its own databases (slice 04-audit-columns): AC-1, the new columns and
 * every earlier column and constraint unchanged; AC-11, the rollback written in V4's header, read from
 * the classpath and run, restores the earlier schema and values, and V4 applies again.
 */
class LinkAuditColumnsTest {

	private static final String V4 = "db/migration/V4__add_link_audit_columns.sql";
	private static final String COLUMNS = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT,"
			+ " CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('LINK', 'AUDIT_LOG')"
			+ " ORDER BY TABLE_NAME, COLUMN_NAME";
	private static final String CONSTRAINTS = "SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE"
			+ " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME IN ('LINK', 'AUDIT_LOG')"
			+ " ORDER BY TABLE_NAME, CONSTRAINT_NAME";
	private static final String SHIPPED_VALUES = "SELECT l.id, l.code, l.url, l.created_at, l.retired_at, l.idempotency_key,"
			+ " a.occurred_at, a.actor, a.action, a.entity_id, a.request_id, a.before_state, a.after_state"
			+ " FROM link l JOIN audit_log a ON a.entity_id = l.code ORDER BY a.id";

	@Test
	void AC1_bothTablesGainTheAuditColumnsAndNothingElseChanges() throws Exception {
		try (Database before = new Database(true); Database after = new Database(false)) {
			List<Map<String, Object>> columns = after.jdbc.sql(COLUMNS).query().listOfRows();
			List<Map<String, Object>> shipped = before.jdbc.sql(COLUMNS).query().listOfRows();

			List<Map<String, Object>> added = new ArrayList<>(columns);
			added.removeAll(shipped);
			assertThat(columns).as("every V3 column unchanged").containsAll(shipped);
			assertThat(added).extracting(row -> row.get("TABLE_NAME") + "." + row.get("COLUMN_NAME") + " "
					+ row.get("DATA_TYPE") + " " + row.get("IS_NULLABLE") + " " + row.get("COLUMN_DEFAULT")).containsExactlyInAnyOrder(
							"AUDIT_LOG.CREATED_AT TIMESTAMP WITH TIME ZONE NO CURRENT_TIMESTAMP",
							"AUDIT_LOG.UPDATED_AT TIMESTAMP WITH TIME ZONE NO CURRENT_TIMESTAMP",
							"AUDIT_LOG.CREATED_BY CHARACTER VARYING NO 'anonymous'",
							"AUDIT_LOG.UPDATED_BY CHARACTER VARYING NO 'anonymous'",
							"LINK.UPDATED_AT TIMESTAMP WITH TIME ZONE NO CURRENT_TIMESTAMP",
							"LINK.CREATED_BY CHARACTER VARYING NO 'anonymous'",
							"LINK.UPDATED_BY CHARACTER VARYING NO 'anonymous'");
			assertThat(added).filteredOn(row -> "CHARACTER VARYING".equals(row.get("DATA_TYPE")))
					.allSatisfy(row -> assertThat(((Number) row.get("CHARACTER_MAXIMUM_LENGTH")).intValue()).isEqualTo(64));
			assertThat(after.jdbc.sql(CONSTRAINTS).query().listOfRows()).as("every V3 constraint unchanged")
					.isEqualTo(before.jdbc.sql(CONSTRAINTS).query().listOfRows());
		}
	}

	@Test
	void AC11_theWrittenRollbackRestoresTheEarlierSchemaAndV4AppliesAgain() throws Exception {
		try (Database before = new Database(true); Database database = new Database(true)) {
			database.jdbc.sql("INSERT INTO link (code, url, created_at, retired_at, idempotency_key) VALUES"
					+ " ('Active12', 'https://example.com/a', TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00', NULL, 'K1'),"
					+ " ('Retire12', 'https://example.com/r', TIMESTAMP WITH TIME ZONE '2026-10-01 09:05:00+00',"
					+ " TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00', NULL)").update();
			database.jdbc.sql("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state)"
					+ " VALUES (TIMESTAMP WITH TIME ZONE '2026-10-01 09:00:00+00', 'anonymous', 'link.create', 'link', 'Active12',"
					+ " 'r-1', NULL, '{\"url\":\"https://example.com/a\",\"state\":\"active\"}'),"
					+ " (TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00', 'anonymous', 'link.retire', 'link', 'Retire12',"
					+ " 'r-2', '{\"url\":\"https://example.com/r\",\"state\":\"active\"}', '{\"url\":\"https://example.com/r\",\"state\":\"retired\"}')")
					.update();
			List<Map<String, Object>> values = database.jdbc.sql(SHIPPED_VALUES).query().listOfRows();
			database.migrate(false);

			for (String statement : rollback()) {
				database.jdbc.sql(statement).update();
			}

			assertThat(database.jdbc.sql(COLUMNS).query().listOfRows()).isEqualTo(before.jdbc.sql(COLUMNS).query().listOfRows());
			assertThat(database.jdbc.sql(CONSTRAINTS).query().listOfRows())
					.isEqualTo(before.jdbc.sql(CONSTRAINTS).query().listOfRows());
			assertThat(database.jdbc.sql(SHIPPED_VALUES).query().listOfRows()).isEqualTo(values);

			database.migrate(false);

			assertThat(database.jdbc.sql("SELECT COUNT(*) FROM link WHERE updated_at = COALESCE(retired_at, created_at)"
					+ " AND created_by = 'anonymous' AND updated_by = 'anonymous'").query(Long.class).single()).isEqualTo(2);
			assertThat(database.jdbc.sql(SHIPPED_VALUES).query().listOfRows()).isEqualTo(values);
		}
	}

	/** The statements after V4's {@code -- rollback} line, each comment line stripped of its {@code --}. */
	private static List<String> rollback() throws Exception {
		String script = new ClassPathResource(V4).getContentAsString(StandardCharsets.UTF_8);
		StringBuilder rollback = new StringBuilder();
		boolean inRollback = false;
		for (String line : script.lines().toList()) {
			if (line.startsWith("-- rollback")) {
				inRollback = true;
			}
			else if (inRollback && line.startsWith("--   ")) {
				rollback.append(line.substring(5)).append(' ');
			}
			else if (inRollback) {
				break;
			}
		}
		List<String> statements = new ArrayList<>();
		for (String statement : rollback.toString().split(";")) {
			if (!statement.isBlank()) {
				statements.add(statement.strip());
			}
		}
		assertThat(statements).as("V4 carries a written rollback").hasSize(8);
		return statements;
	}

	/** A fresh in-memory database with the real migrations: all of them, or only those before V4. */
	private static final class Database implements AutoCloseable {

		private final HikariDataSource dataSource;
		private final Connection keepOpen;
		private final JdbcClient jdbc;

		Database(boolean beforeV4) throws Exception {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:h2:mem:link-audit-columns-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
			config.setUsername("sa");
			// one connection is held to keep the database open; Flyway needs two of its own
			config.setMaximumPoolSize(3);
			dataSource = new HikariDataSource(config);
			keepOpen = dataSource.getConnection();
			jdbc = JdbcClient.create(dataSource);
			migrate(beforeV4);
		}

		void migrate(boolean beforeV4) {
			var flyway = Flyway.configure().dataSource(dataSource);
			if (beforeV4) {
				// the last migration before V4: V3 once 02-click-retention's is on the classpath, V2 until then
				flyway.target(Arrays.stream(flyway.load().info().all()).map(MigrationInfo::getVersion)
						.filter(version -> version.compareTo(MigrationVersion.fromVersion("4")) < 0)
						.max(Comparator.naturalOrder()).orElseThrow());
			}
			flyway.load().migrate();
		}

		@Override
		public void close() throws Exception {
			keepOpen.close();
			dataSource.close();
		}
	}
}
