import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;

/**
 * Migration probe for 04-audit-columns (design.md section 3, section 12 rows K1-K5). It runs the shipped
 * V1 and V2, 02-click-retention's designed V3 and this slice's proposed V4 through Flyway on H2 2.4.240
 * file databases, and the designed link statements (stamp, retire). No file under src/ is touched.
 */
public class LinkMigrationProbe {

	static final Path ROOT = Path.of("build/link-migration-probe").toAbsolutePath();
	static final String V3_DIR = Path.of("missions/02-brownfield/slices/02-click-retention/design-probe/migration").toAbsolutePath().toString();
	static final String V4_DIR = Path.of("missions/02-brownfield/slices/04-audit-columns/design-probe/migration").toAbsolutePath().toString();
	static final Instant T0 = Instant.parse("2026-09-01T10:00:00Z");
	static final Instant T1 = Instant.parse("2026-09-20T15:30:00Z");
	static final String AUDIT_INSERT = "INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state)"
			+ " VALUES (?, 'anonymous', ?, 'link', ?, ?, ?, ?)";

	public static void main(String[] args) throws Exception {
		org.springframework.util.FileSystemUtils.deleteRecursively(ROOT);
		Files.createDirectories(ROOT);
		k1Fresh();
		k2Upgrade();
		k5Cost();
		System.out.println("PROBE done");
	}

	static void k1Fresh() throws Exception {
		String url = url("k1");
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, null);
			System.out.println("K1 fresh database: " + history(keeper));
			System.out.println("K1 LINK " + columns(keeper, "LINK"));
			System.out.println("K1 AUDIT_LOG " + columns(keeper, "AUDIT_LOG"));
		}
	}

	static void k2Upgrade() throws Exception {
		String url = url("k2");
		String v3Link;
		String v3Audit;
		String v3Constraints;
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, "3");
			long active = link(keeper, "probeAct1", T0, null, "key-active");
			long retired = link(keeper, "probeRet1", T0, T1, null);
			long released = link(keeper, "probeRel1", T0, null, null); // created with a key, later released: the key is gone
			audit(keeper, T0, "link.create", "probeAct1");
			audit(keeper, T0, "link.create", "probeRet1");
			audit(keeper, T1, "link.retire", "probeRet1");
			audit(keeper, Instant.now().plusSeconds(86_400), "link.create", "probeFut1"); // a service clock ahead of the database
			v3Link = rows(keeper, "SELECT id, code, url, created_at, retired_at, idempotency_key FROM link ORDER BY id");
			v3Audit = rows(keeper, "SELECT id, occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state FROM audit_log ORDER BY id");
			v3Constraints = constraints(keeper);
			String v3Columns = columns(keeper, "LINK") + " | " + columns(keeper, "AUDIT_LOG");
			Instant before = Instant.now();
			migrate(url, null);
			System.out.println("K2 V3 directory upgraded: " + history(keeper) + "; the upgrade began at " + before);
			System.out.println("K2 shipped columns unchanged: link " + v3Link.equals(rows(keeper,
					"SELECT id, code, url, created_at, retired_at, idempotency_key FROM link ORDER BY id")) + ", audit_log "
					+ v3Audit.equals(rows(keeper, "SELECT id, occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state FROM audit_log ORDER BY id"))
					+ "; constraints unchanged " + v3Constraints.equals(constraints(keeper)) + " " + v3Constraints);
			System.out.println("K2 links: " + rows(keeper,
					"SELECT code, created_at, retired_at, updated_at, created_by, updated_by, updated_at = COALESCE(retired_at, created_at) AS backfilled FROM link ORDER BY id"));
			System.out.println("K2 audit rows: " + rows(keeper,
					"SELECT entity_id, action, occurred_at, created_at, updated_at = created_at AS equal, created_at <= CURRENT_TIMESTAMP AS not_later,"
							+ " created_by = actor AND updated_by = actor AS actors FROM audit_log ORDER BY id"));
			// K3: after Flyway's own connection closed, with the database held open by this keeper
			try (Statement s = keeper.createStatement()) {
				s.executeUpdate("INSERT INTO link (code, url, created_at) VALUES ('probeV1x1', 'https://example.org/v1', TIMESTAMP WITH TIME ZONE '2026-10-03 12:00:00+00')");
			}
			audit(keeper, Instant.parse("2026-10-03T12:00:00Z"), "link.create", "probeV1x1");
			System.out.println("K3 v1-shaped link insert (as ClickSchemaTest and ObservabilityJourneyTest do): " + rows(keeper,
					"SELECT updated_at IS NOT NULL AS filled, created_by, updated_by FROM link WHERE code = 'probeV1x1'"));
			System.out.println("K3 the unchanged AuditLog insert: " + rows(keeper,
					"SELECT created_at = updated_at AS equal, created_by, updated_by, actor FROM audit_log WHERE entity_id = 'probeV1x1'"));
			// the designed link statements (LinkRepository): stamp after create and after a key release, retire inline
			Instant now = Instant.parse("2026-10-04T09:00:00Z");
			update(keeper, "UPDATE link SET idempotency_key = NULL WHERE id = ?", active);
			try (PreparedStatement ps = keeper.prepareStatement("UPDATE link SET updated_at = ?, updated_by = 'anonymous' WHERE id = ?")) {
				ps.setObject(1, now.atOffset(ZoneOffset.UTC));
				ps.setLong(2, active);
				ps.executeUpdate();
			}
			Instant retireAt = Instant.parse("2026-10-05T09:00:00Z");
			int changed;
			int again;
			try (PreparedStatement ps = keeper.prepareStatement(
					"UPDATE link SET retired_at = ?, updated_at = ?, updated_by = 'anonymous' WHERE id = ? AND retired_at IS NULL")) {
				ps.setObject(1, retireAt.atOffset(ZoneOffset.UTC));
				ps.setObject(2, retireAt.atOffset(ZoneOffset.UTC));
				ps.setLong(3, released);
				changed = ps.executeUpdate();
				ps.setObject(1, Instant.parse("2026-10-06T09:00:00Z").atOffset(ZoneOffset.UTC));
				ps.setObject(2, Instant.parse("2026-10-06T09:00:00Z").atOffset(ZoneOffset.UTC));
				again = ps.executeUpdate();
			}
			System.out.println("K3b key release + stamp at " + now + ": " + rows(keeper,
					"SELECT code, created_at, updated_at, idempotency_key FROM link WHERE id = " + active) + "; retire at " + retireAt
					+ " changed " + changed + " row, a second retire " + again + ": " + rows(keeper,
							"SELECT code, created_at, retired_at, updated_at FROM link WHERE id = " + released));
			System.out.println("K1b V3 schema kept for the rollback check: " + v3Columns.length() + " chars");
			// K4: the written rollback, then V4 again
			try (Statement s = keeper.createStatement()) {
				for (String sql : List.of("ALTER TABLE link DROP COLUMN updated_by", "ALTER TABLE link DROP COLUMN created_by",
						"ALTER TABLE link DROP COLUMN updated_at", "ALTER TABLE audit_log DROP COLUMN updated_by",
						"ALTER TABLE audit_log DROP COLUMN created_by", "ALTER TABLE audit_log DROP COLUMN updated_at",
						"ALTER TABLE audit_log DROP COLUMN created_at", "DELETE FROM \"flyway_schema_history\" WHERE \"version\" = '4'")) {
					s.execute(sql);
				}
			}
			System.out.println("K4 after the written rollback: schema equals V3 " + v3Columns.equals(columns(keeper, "LINK") + " | "
					+ columns(keeper, "AUDIT_LOG")) + ", constraints equal " + v3Constraints.equals(constraints(keeper)) + "; " + history(keeper));
		}
		migrate(url, null);
		try (Connection again = DriverManager.getConnection(url, "sa", "")) {
			System.out.println("K4 re-applied: " + history(again) + "; " + rows(again,
					"SELECT COUNT(*) AS links, SUM(CASE WHEN updated_at = COALESCE(retired_at, created_at) THEN 1 ELSE 0 END) AS backfilled FROM link"));
		}
	}

	static void k5Cost() throws Exception {
		String url = url("k5");
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, "3");
			try (Statement s = keeper.createStatement()) {
				// codes padded to 8 characters: V1's ck_link_code_length requires at least 6
				s.executeUpdate("INSERT INTO link (code, url, created_at, retired_at) SELECT 'c' || LPAD(CAST(X AS VARCHAR), 7, '0'), 'https://example.org/' || X,"
						+ " TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00' + X * INTERVAL '1' MINUTE,"
						+ " CASE WHEN MOD(X, 2) = 0 THEN TIMESTAMP WITH TIME ZONE '2026-06-01 00:00:00+00' END FROM SYSTEM_RANGE(1, 100000)");
				s.executeUpdate("INSERT INTO audit_log (occurred_at, actor, action, entity, entity_id, request_id, after_state)"
						+ " SELECT TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00' + X * INTERVAL '30' SECOND, 'anonymous', 'link.create', 'link',"
						+ " 'code' || X, 'req-' || X, '{\"url\":\"https://example.org/\",\"state\":\"active\"}' FROM SYSTEM_RANGE(1, 150000)");
			}
			long t0 = System.nanoTime();
			migrate(url, null);
			System.out.println("K5 V4 on 100 000 links and 150 000 audit rows: " + (System.nanoTime() - t0) / 1_000_000 + " ms wall; " + rows(keeper,
					"SELECT (SELECT COUNT(*) FROM link WHERE updated_at = COALESCE(retired_at, created_at)) AS links_backfilled,"
							+ " (SELECT COUNT(*) FROM audit_log WHERE created_at = occurred_at AND created_by = actor) AS audit_backfilled"));
		}
	}

	// ------------------------------------------------------------------ helpers

	static String url(String name) {
		return "jdbc:h2:file:" + ROOT.resolve(name).resolve("urlshort") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
	}

	static void migrate(String url, String target) {
		var config = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration", "filesystem:" + V3_DIR,
				"filesystem:" + V4_DIR);
		if (target != null) {
			config = config.target(target);
		}
		config.load().migrate();
	}

	static long link(Connection c, String code, Instant created, Instant retired, String key) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO link (code, url, created_at, retired_at, idempotency_key) VALUES (?, ?, ?, ?, ?)",
				new String[] { "ID" })) {
			ps.setString(1, code);
			ps.setString(2, "https://example.org/" + code);
			ps.setObject(3, created.atOffset(ZoneOffset.UTC));
			ps.setObject(4, retired == null ? null : retired.atOffset(ZoneOffset.UTC));
			ps.setString(5, key);
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	static void audit(Connection c, Instant at, String action, String code) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(AUDIT_INSERT)) {
			ps.setObject(1, at.atOffset(ZoneOffset.UTC));
			ps.setString(2, action);
			ps.setString(3, code);
			ps.setString(4, "req-" + code + "-" + action);
			ps.setString(5, action.equals("link.retire") ? "{\"url\":\"u\",\"state\":\"active\"}" : null);
			ps.setString(6, "{\"url\":\"u\",\"state\":\"" + (action.equals("link.retire") ? "retired" : "active") + "\"}");
			ps.executeUpdate();
		}
	}

	static void update(Connection c, String sql, long id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

	static String history(Connection c) throws SQLException {
		return "history " + rows(c, "SELECT \"version\", \"success\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"");
	}

	static String columns(Connection c, String table) throws SQLException {
		return rows(c, "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"
				+ table + "' ORDER BY ORDINAL_POSITION");
	}

	static String constraints(Connection c) throws SQLException {
		return rows(c, "SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
				+ " WHERE TABLE_NAME IN ('LINK', 'AUDIT_LOG') AND CONSTRAINT_TYPE <> 'PRIMARY KEY' ORDER BY 1, 2");
	}

	static String rows(Connection c, String sql) throws SQLException {
		List<String> out = new ArrayList<>();
		try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
			int n = rs.getMetaData().getColumnCount();
			while (rs.next()) {
				List<String> row = new ArrayList<>();
				for (int i = 1; i <= n; i++) {
					row.add(String.valueOf(rs.getObject(i)));
				}
				out.add(String.join("|", row));
			}
		}
		return "[" + String.join("; ", out) + "]";
	}
}
