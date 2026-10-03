package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * AC-16 on the migrations alone: {@code click} and {@code user_agent_class} gain
 * {@code created_at}/{@code updated_at}/{@code created_by}/{@code updated_by}, pre-existing rows are
 * backfilled, the v1 insert keeps working and fills them, and every v1 column, value, constraint and
 * index is unchanged (expand only). Pattern of {@link ClickSchemaTest}: its own pool, with one
 * connection keeping the database open while Flyway's are retired.
 */
class ClickAuditColumnsTest {

	private static final String HASH = "b".repeat(64);
	private static final List<String> AUDIT_COLUMNS = List.of("CREATED_AT", "UPDATED_AT", "CREATED_BY", "UPDATED_BY");

	@Test
	void theClickTablesGainFilledAuditColumnsAndKeepEveryV1ColumnAndConstraint() throws Exception {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:click-audit-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
		config.setUsername("sa");
		config.setMaximumPoolSize(3);
		try (HikariDataSource dataSource = new HikariDataSource(config);
				Connection keepOpen = dataSource.getConnection()) {
			Flyway.configure().dataSource(dataSource).target("2").load().migrate();
			JdbcClient jdbc = JdbcClient.create(dataSource);
			jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('Aud12345', 'https://example.com/', CURRENT_TIMESTAMP)")
					.update();
			long linkId = jdbc.sql("SELECT id FROM link WHERE code = 'Aud12345'").query(Long.class).single();
			insertV1(jdbc, linkId, "TIMESTAMP '2026-07-01 10:00:00+00'", "DATE '2026-07-01'", "https://old.example");
			insertV1(jdbc, linkId, "TIMESTAMP '2026-09-30 23:59:59+00'", "DATE '2026-09-30'", null);
			List<Map<String, Object>> v1Columns = columns(jdbc);
			List<Map<String, Object>> v1Constraints = constraints(jdbc);
			List<Map<String, Object>> v1Indexes = indexes(jdbc);
			List<Map<String, Object>> v1Clicks = jdbc.sql("SELECT * FROM click ORDER BY id").query().listOfRows();
			List<Map<String, Object>> v1Classes = jdbc.sql("SELECT * FROM user_agent_class ORDER BY token").query()
					.listOfRows();

			Flyway.configure().dataSource(dataSource).load().migrate();
			dataSource.getHikariPoolMXBean().softEvictConnections();
			insertV1(jdbc, linkId, "CURRENT_TIMESTAMP", "CURRENT_DATE", "https://new.example");

			for (String table : List.of("CLICK", "USER_AGENT_CLASS")) {
				List<Map<String, Object>> audit = jdbc.sql("SELECT column_name, data_type, is_nullable FROM"
						+ " information_schema.columns WHERE table_name = :table AND column_name IN (:names)"
						+ " ORDER BY column_name").param("table", table).param("names", AUDIT_COLUMNS).query().listOfRows();
				assertThat(audit).as(table).containsExactly(
						row("CREATED_AT", "TIMESTAMP WITH TIME ZONE"), row("CREATED_BY", "CHARACTER VARYING"),
						row("UPDATED_AT", "TIMESTAMP WITH TIME ZONE"), row("UPDATED_BY", "CHARACTER VARYING"));
			}
			assertThat(columns(jdbc)).isEqualTo(v1Columns);
			assertThat(constraints(jdbc)).isEqualTo(v1Constraints);
			assertThat(indexes(jdbc)).isEqualTo(v1Indexes);
			assertThat(jdbc.sql("SELECT id, link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash"
					+ " FROM click WHERE id <= :last ORDER BY id").param("last", v1Clicks.get(1).get("ID")).query()
					.listOfRows()).isEqualTo(v1Clicks);
			assertThat(jdbc.sql("SELECT token FROM user_agent_class ORDER BY token").query().listOfRows())
					.isEqualTo(v1Classes);

			assertThat(count(jdbc, "SELECT COUNT(*) FROM click WHERE id <= :last AND created_at = clicked_at"
					+ " AND updated_at = clicked_at AND created_by = 'anonymous' AND updated_by = 'anonymous'",
					v1Clicks.get(1).get("ID"))).as("backfilled clicks").isEqualTo(2);
			assertThat(count(jdbc, "SELECT COUNT(*) FROM click WHERE id > :last AND created_at = updated_at"
					+ " AND created_by = 'anonymous' AND updated_by = 'anonymous'", v1Clicks.get(1).get("ID")))
					.as("a v1-shaped insert after the upgrade").isEqualTo(1);
			assertThat(jdbc.sql("SELECT COUNT(*) FROM user_agent_class WHERE updated_at = created_at"
					+ " AND created_by = 'system' AND updated_by = 'system'").query(Long.class).single())
					.as("classes").isEqualTo(4);
			assertThat(jdbc.sql("SELECT COUNT(*) FROM click WHERE created_by IN (client_hash, referrer, user_agent_class)"
					+ " OR updated_by IN (client_hash, referrer, user_agent_class)").query(Long.class).single())
					.as("no client value in an audit column").isZero();
			assertThat(keepOpen.isClosed()).isFalse();
		}
	}

	private static Map<String, Object> row(String name, String type) {
		return Map.of("COLUMN_NAME", name, "DATA_TYPE", type, "IS_NULLABLE", "NO");
	}

	private static void insertV1(JdbcClient jdbc, long linkId, String at, String on, String referrer) {
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (:linkId, " + at + ", " + on + ", :referrer, 'browser', :hash)")
				.param("linkId", linkId).param("referrer", referrer).param("hash", HASH).update();
	}

	private static long count(JdbcClient jdbc, String sql, Object last) {
		return jdbc.sql(sql).param("last", last).query(Long.class).single();
	}

	private static List<Map<String, Object>> columns(JdbcClient jdbc) {
		return jdbc.sql("SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default"
				+ " FROM information_schema.columns WHERE table_schema = 'PUBLIC'"
				// link has its own v1 created_at; only the click tables' audit columns are new
				+ " AND (table_name NOT IN ('CLICK', 'USER_AGENT_CLASS') OR column_name NOT IN (:audit))"
				+ " ORDER BY table_name, ordinal_position").param("audit", AUDIT_COLUMNS).query().listOfRows();
	}

	private static List<Map<String, Object>> constraints(JdbcClient jdbc) {
		return jdbc.sql("SELECT tc.table_name, tc.constraint_name, tc.constraint_type, cc.check_clause"
				+ " FROM information_schema.table_constraints tc LEFT JOIN information_schema.check_constraints cc"
				+ " ON cc.constraint_name = tc.constraint_name WHERE tc.table_schema = 'PUBLIC'"
				+ " ORDER BY tc.table_name, tc.constraint_name").query().listOfRows();
	}

	/** Every index by its table, kind and columns; H2 renames its generated index names when ALTER rebuilds a table. */
	private static List<Map<String, Object>> indexes(JdbcClient jdbc) {
		return jdbc.sql("SELECT ic.table_name, i.index_type_name, ic.column_name, ic.ordinal_position"
				+ " FROM information_schema.index_columns ic JOIN information_schema.indexes i"
				+ " ON i.index_schema = ic.index_schema AND i.index_name = ic.index_name WHERE ic.table_schema = 'PUBLIC'"
				+ " ORDER BY ic.table_name, i.index_type_name, ic.column_name, ic.ordinal_position").query().listOfRows();
	}
}
