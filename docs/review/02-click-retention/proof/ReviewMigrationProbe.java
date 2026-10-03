import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;

/** Independent assertions against the handed-off V3 and its literal rollback header. */
public class ReviewMigrationProbe {
    static final Path MIGRATION = Path.of("missions/02-brownfield/slices/02-click-retention/design-probe/migration/V3__add_click_audit_columns.sql");
    static final String OLD_ROWS = "SELECT id,link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash FROM click ORDER BY id";
    static int assertions;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("retention-review-v3-");
        String url = "jdbc:h2:file:" + root.resolve("upgrade") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
        String beforeColumns, beforeConstraints, beforeRows;
        migrate(url, "2");
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            execute(c, "INSERT INTO link (code,url,created_at) VALUES ('reviewV3','https://example.org/',CURRENT_TIMESTAMP)");
            insert(c);
            beforeColumns = columns(c, true);
            beforeConstraints = constraints(c);
            beforeRows = rows(c, OLD_ROWS);
            migrate(url, null);
            equal(beforeColumns, columns(c, true), "old column types/defaults/nullability preserved");
            equal(beforeConstraints, constraints(c), "named checks/FKs and primary-key columns preserved");
            equal(beforeRows, rows(c, OLD_ROWS), "old click values preserved");
            equal("[1]", rows(c, "SELECT COUNT(*) FROM click WHERE created_at=clicked_at AND updated_at=created_at AND created_by='anonymous' AND updated_by='anonymous'"), "old click backfilled");
            auditSchema(c);
            newInsert(c, "keeper after Flyway connection retirement");
        }
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            newInsert(c, "fresh connection after database reopen");
            rejected(c, "UPDATE click SET user_agent_class='invalid' WHERE id=1", "23506");
            rejected(c, "UPDATE click SET client_hash='short' WHERE id=1", "23513");
            rejected(c, "UPDATE click SET link_id=999 WHERE id=1", "23506");
            rejected(c, "UPDATE click SET created_at=NULL WHERE id=1", "23502");
            String rollbackRows = rows(c, OLD_ROWS);
            var rollback = Files.readAllLines(MIGRATION).stream().filter(s -> s.startsWith("--   ")).toList();
            equal(5, rollback.size(), "five literal rollback header lines");
            for (String line : rollback) for (String sql : line.substring(5).split(";")) if (!sql.isBlank()) execute(c, sql);
            equal(beforeColumns, columns(c, false), "rollback restores complete V2 column schema");
            equal(beforeConstraints, constraints(c), "rollback preserves constraints");
            equal(rollbackRows, rows(c, OLD_ROWS), "rollback preserves all click rows");
            equal("[1, 2]", rows(c, "SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\""), "rollback removes only V3 history");
            migrate(url, null);
            equal("[3]", rows(c, "SELECT COUNT(*) FROM click WHERE created_at=clicked_at AND updated_at=created_at"), "V3 reapplies and backfills all rows");
            auditSchema(c);
        }
        String freshUrl = "jdbc:h2:file:" + root.resolve("fresh") + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
        migrate(freshUrl, null);
        try (Connection c = DriverManager.getConnection(freshUrl, "sa", "")) { auditSchema(c); }
        System.out.println("PASS: " + assertions + " independent assertions; temporary file databases: " + root);
    }

    static void migrate(String url, String target) {
        var cfg = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration", "filesystem:" + MIGRATION.getParent().toAbsolutePath());
        if (target != null) cfg.target(target);
        cfg.load().migrate();
    }
    static void insert(Connection c) throws Exception {
        execute(c, "INSERT INTO click (link_id,clicked_at,clicked_on,referrer,user_agent_class,client_hash) VALUES (1,TIMESTAMP WITH TIME ZONE '2001-01-01 00:00:00+00',DATE '2001-01-01',NULL,'browser','" + "a".repeat(64) + "')");
    }
    static void newInsert(Connection c, String label) throws Exception {
        Instant before = Instant.now();
        insert(c);
        Instant after = Instant.now();
        try (var s = c.createStatement(); var rs = s.executeQuery("SELECT created_at,updated_at,created_by,updated_by FROM click ORDER BY id DESC FETCH FIRST ROW ONLY")) {
            rs.next();
            Instant created = rs.getObject(1, OffsetDateTime.class).toInstant();
            equal(created, rs.getObject(2, OffsetDateTime.class).toInstant(), label + ": equal row times");
            equal(true, !created.isBefore(before.minusMillis(1)) && !created.isAfter(after.plusMillis(1)), label + ": database write time distinct from shifted domain time");
            equal("anonymous|anonymous", rs.getString(3) + "|" + rs.getString(4), label + ": static actors");
        }
    }
    static void auditSchema(Connection c) throws Exception {
        equal("[4]", rows(c, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('CLICK','USER_AGENT_CLASS') AND COLUMN_NAME IN ('CREATED_AT','UPDATED_AT') AND DATA_TYPE='TIMESTAMP WITH TIME ZONE' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='CURRENT_TIMESTAMP'"), "four non-null timestamp defaults");
        equal("[4]", rows(c, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('CLICK','USER_AGENT_CLASS') AND COLUMN_NAME IN ('CREATED_BY','UPDATED_BY') AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NOT NULL"), "four non-null actor defaults");
        equal("[4]", rows(c, "SELECT COUNT(*) FROM user_agent_class WHERE created_at=updated_at AND created_by='system' AND updated_by='system'"), "all seeded classes have equal times and system actors");
    }
    static String columns(Connection c, boolean oldOnly) throws Exception {
        return rows(c, "SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,IS_NULLABLE,COLUMN_DEFAULT,CHARACTER_MAXIMUM_LENGTH,NUMERIC_PRECISION,DATETIME_PRECISION,IS_IDENTITY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('CLICK','USER_AGENT_CLASS')" + (oldOnly ? " AND COLUMN_NAME NOT IN ('CREATED_AT','UPDATED_AT','CREATED_BY','UPDATED_BY')" : "") + " ORDER BY TABLE_NAME,ORDINAL_POSITION");
    }
    static String constraints(Connection c) throws Exception {
        String named = rows(c, "SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME IN ('CLICK','USER_AGENT_CLASS') AND CONSTRAINT_TYPE<>'PRIMARY KEY' ORDER BY 1,2");
        return named + rows(c, "SELECT k.TABLE_NAME,k.COLUMN_NAME,k.ORDINAL_POSITION FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME=t.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA WHERE t.CONSTRAINT_TYPE='PRIMARY KEY' AND k.TABLE_NAME IN ('CLICK','USER_AGENT_CLASS') ORDER BY 1,3");
    }
    static void rejected(Connection c, String sql, String state) throws Exception {
        try { execute(c, sql); throw new AssertionError("Constraint accepted: " + sql); }
        catch (SQLException ex) { equal(state, ex.getSQLState(), "constraint rejection " + state); }
    }
    static void execute(Connection c, String sql) throws SQLException { try (var s = c.createStatement()) { s.execute(sql); } }
    static String rows(Connection c, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (var s = c.createStatement(); var rs = s.executeQuery(sql)) {
            while (rs.next()) {
                List<String> fields = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) fields.add(String.valueOf(rs.getObject(i)));
                rows.add(String.join("|", fields));
            }
        }
        return rows.toString();
    }
    static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + ", actual " + actual);
        assertions++;
        System.out.println("PASS " + label);
    }
}
