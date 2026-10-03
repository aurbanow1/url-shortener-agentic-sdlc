import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** Independent requirements probe, using the shipped schema and H2 version. */
class AuditCommitOrderProbe {
    static void append(Connection connection, String entity) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO audit_log "
                + "(occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state) "
                + "VALUES ('2026-10-03T12:00:00Z', 'anonymous', 'link.create', 'link', ?, ?, NULL, '{}')")) {
            statement.setString(1, entity);
            statement.setString(2, "request-" + entity);
            statement.executeUpdate();
        }
    }

    static String rows(Connection connection) throws Exception {
        var output = new StringBuilder();
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT id, entity_id FROM audit_log ORDER BY id DESC")) {
            while (result.next()) {
                if (!output.isEmpty()) output.append(", ");
                output.append(result.getLong(1)).append(':').append(result.getString(2));
            }
        }
        return output.toString();
    }

    public static void main(String[] args) throws Exception {
        try (var reader = DriverManager.getConnection("jdbc:h2:mem:audit_order;DB_CLOSE_DELAY=-1");
             var first = DriverManager.getConnection("jdbc:h2:mem:audit_order");
             var second = DriverManager.getConnection("jdbc:h2:mem:audit_order")) {
            String schema = Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql"));
            try (Statement statement = reader.createStatement()) { statement.execute(schema); }
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            append(first, "A");
            append(second, "B");
            second.commit();
            String before = rows(reader);
            first.commit();
            String after = rows(reader);
            if (!before.equals("2:B") || !after.equals("2:B, 1:A")) throw new AssertionError(before + " / " + after);
            System.out.println("H2 " + reader.getMetaData().getDatabaseProductVersion());
            System.out.println("Append order A then B; commit order B then A");
            System.out.println("After only B commits: " + before);
            System.out.println("After A commits: " + after);
            System.out.println("Newest commit first would require A then B. Identity descending gives B then A.");
            System.out.println("Both occurred_at values equal; shipped columns do not record commit order.");
        }
    }
}
