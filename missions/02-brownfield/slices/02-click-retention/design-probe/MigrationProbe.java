import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;

/**
 * Migration probe for 02-click-retention design review DR-03 (design.md section 3, AC-13, AC-16). It
 * runs the shipped V1 and V2 plus the proposed V3 (design-probe/migration/) on H2 2.4.240 file databases
 * through Flyway, the way the service does. No file under src/ is touched.
 */
public class MigrationProbe {

	static final Path ROOT = Path.of("build/migration-probe").toAbsolutePath();
	static final String V3_DIR = Path.of("missions/02-brownfield/slices/02-click-retention/design-probe/migration").toAbsolutePath().toString();
	static final String HASH = "0123456789abcdef".repeat(4);

	public static void main(String[] args) throws Exception {
		org.springframework.util.FileSystemUtils.deleteRecursively(ROOT);
		Files.createDirectories(ROOT);
		m1FreshDatabase();
		m2UpgradeKeepsV1AndFillsAudit();
		m5UpgradeCost();
		System.out.println("PROBE done");
	}

	static void m1FreshDatabase() throws Exception {
		String url = url("m1");
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, null);
			System.out.println("M1 fresh database, all migrations: " + history(keeper));
			for (String table : List.of("CLICK", "USER_AGENT_CLASS")) {
				System.out.println("M1 " + table + " columns: " + columns(keeper, table));
			}
		}
	}

	static void m2UpgradeKeepsV1AndFillsAudit() throws Exception {
		String url = url("m2");
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, "2");
			long link = insertLink(keeper, "probeM2x1");
			insertClick(keeper, link, Instant.parse("2026-06-01T08:00:00Z"));
			insertClick(keeper, link, Instant.parse("2026-10-02T23:59:59.123Z"));
			String clicksBefore = rows(keeper, "SELECT id, link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash FROM click ORDER BY id");
			String classesBefore = rows(keeper, "SELECT token FROM user_agent_class ORDER BY token");
			String v2Columns = columns(keeper, "CLICK") + " | " + columns(keeper, "USER_AGENT_CLASS");
			String constraintsBefore = constraints(keeper);
			long t0 = System.nanoTime();
			migrate(url, null);
			System.out.println("M2 V2 directory upgraded in " + (System.nanoTime() - t0) / 1_000_000 + " ms: " + history(keeper));
			System.out.println("M2 v1 click columns unchanged: " + clicksBefore.equals(rows(keeper,
					"SELECT id, link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash FROM click ORDER BY id"))
					+ "; classes unchanged: " + classesBefore.equals(rows(keeper, "SELECT token FROM user_agent_class ORDER BY token"))
					+ "; constraints unchanged: " + constraintsBefore.equals(constraints(keeper)) + " " + constraintsBefore);
			System.out.println("M2 pre-existing clicks: " + rows(keeper,
					"SELECT id, clicked_at, created_at, updated_at, created_by, updated_by, created_at = clicked_at AND updated_at = created_at AS backfilled FROM click ORDER BY id"));
			System.out.println("M2 classes: " + rows(keeper,
					"SELECT token, created_by, updated_by, updated_at = created_at AS equal FROM user_agent_class ORDER BY token"));
			// H2 2.4.240's multi-value CHECK defect showed when the connection that ran the DDL had been retired
			// while the database stayed open (02-analytics DR-04): insert now, Flyway's connection closed, the
			// database held open by this keeper
			insertClick(keeper, link, Instant.parse("2026-10-03T13:00:00Z"));
			System.out.println("M2b v1-shaped insert after Flyway's connection closed, database still open: " + rows(keeper,
					"SELECT created_at = updated_at AS equal, created_by, updated_by FROM click WHERE clicked_at = TIMESTAMP WITH TIME ZONE '2026-10-03 13:00:00+00'"));
		}
		// a fresh connection after Flyway's own has closed: the v1 insert, unchanged, on the upgraded schema
		try (Connection fresh = DriverManager.getConnection(url, "sa", "")) {
			long link = fresh.createStatement().executeQuery("SELECT id FROM link").next() ? 1 : 0;
			Instant before = Instant.now();
			for (int i = 0; i < 500; i++) {
				insertClick(fresh, link, Instant.parse("2026-10-03T12:00:00Z"));
			}
			System.out.println("M3 500 v1-shaped inserts on a new connection after the migration: " + rows(fresh,
					"SELECT COUNT(*) AS rows_, SUM(CASE WHEN created_at = updated_at THEN 1 ELSE 0 END) AS equal,"
							+ " SUM(CASE WHEN created_by = 'anonymous' AND updated_by = 'anonymous' THEN 1 ELSE 0 END) AS anonymous,"
							+ " MIN(created_at) AS first_written FROM click WHERE clicked_at = TIMESTAMP WITH TIME ZONE '2026-10-03 12:00:00+00'")
					+ " (database clock; the probe started them at " + before + ")");
			String rollback = String.join("\n", "ALTER TABLE click DROP COLUMN updated_by", "ALTER TABLE click DROP COLUMN created_by",
					"ALTER TABLE click DROP COLUMN updated_at", "ALTER TABLE click DROP COLUMN created_at",
					"ALTER TABLE user_agent_class DROP COLUMN updated_by", "ALTER TABLE user_agent_class DROP COLUMN created_by",
					"ALTER TABLE user_agent_class DROP COLUMN updated_at", "ALTER TABLE user_agent_class DROP COLUMN created_at",
					"DELETE FROM \"flyway_schema_history\" WHERE \"version\" = '3'");
			try (Statement s = fresh.createStatement()) {
				for (String sql : rollback.split("\n")) {
					s.execute(sql);
				}
			}
			System.out.println("M4 after the written rollback: click " + columns(fresh, "CLICK") + "; user_agent_class "
					+ columns(fresh, "USER_AGENT_CLASS") + "; " + history(fresh));
		}
		migrate(url, null);
		try (Connection again = DriverManager.getConnection(url, "sa", "")) {
			System.out.println("M4 re-applied after the rollback: " + history(again) + "; rows " + rows(again,
					"SELECT COUNT(*) AS clicks, SUM(CASE WHEN created_at = clicked_at THEN 1 ELSE 0 END) AS backfilled FROM click"));
		}
	}

	static void m5UpgradeCost() throws Exception {
		String url = url("m5");
		try (Connection keeper = DriverManager.getConnection(url, "sa", "")) {
			migrate(url, "2");
			long link = insertLink(keeper, "probeM5x1");
			try (Statement s = keeper.createStatement()) {
				s.executeUpdate("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
						+ " SELECT " + link + ", TIMESTAMP WITH TIME ZONE '2026-07-01 00:00:00+00' + X * INTERVAL '6' SECOND,"
						+ " DATE '2026-07-01' + CAST(X / 14400 AS INT), NULL, 'browser', '" + HASH + "' FROM SYSTEM_RANGE(1, 1300000)");
			}
			long t0 = System.nanoTime();
			migrate(url, null);
			System.out.println("M5 V3 on 1 300 000 clicks: " + (System.nanoTime() - t0) / 1_000_000 + " ms; " + rows(keeper,
					"SELECT COUNT(*) AS clicks, SUM(CASE WHEN created_at = clicked_at AND updated_at = clicked_at THEN 1 ELSE 0 END) AS backfilled FROM click"));
		}
	}

	// ------------------------------------------------------------------ helpers

	static String url(String name) {
		return "jdbc:h2:file:" + ROOT.resolve(name).resolve("urlshort") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
	}

	static void migrate(String url, String target) {
		var config = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration", "filesystem:" + V3_DIR);
		if (target != null) {
			config = config.target(target);
		}
		config.load().migrate();
	}

	static long insertLink(Connection c, String code) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO link (code, url, created_at) VALUES (?, ?, ?)", new String[] { "ID" })) {
			ps.setString(1, code);
			ps.setString(2, "https://example.org/" + code);
			ps.setObject(3, LocalDate.of(2026, 5, 1).atStartOfDay().atOffset(ZoneOffset.UTC));
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				keys.next();
				return keys.getLong(1);
			}
		}
	}

	/** The v1 column list, exactly as ClickStore.insert writes it. */
	static void insertClick(Connection c, long linkId, Instant at) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (?, ?, ?, ?, ?, ?)")) {
			ps.setLong(1, linkId);
			ps.setObject(2, at.atOffset(ZoneOffset.UTC));
			ps.setObject(3, LocalDate.ofInstant(at, ZoneOffset.UTC));
			ps.setString(4, "https://news.example.com");
			ps.setString(5, "browser");
			ps.setString(6, HASH);
			ps.executeUpdate();
		}
	}

	static String history(Connection c) throws SQLException {
		return "history " + rows(c, "SELECT \"version\", \"success\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"");
	}

	static String columns(Connection c, String table) throws SQLException {
		return rows(c, "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + table
				+ "' ORDER BY ORDINAL_POSITION");
	}

	static String constraints(Connection c) throws SQLException {
		return rows(c, "SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
				+ " WHERE TABLE_NAME IN ('CLICK', 'USER_AGENT_CLASS') AND CONSTRAINT_TYPE <> 'PRIMARY KEY' ORDER BY 1, 2");
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
