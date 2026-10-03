import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Design probe for 02-analytics review finding DR-04: which CHECK forms survive the retirement of the
 * pooled connection that created them on H2 2.4.240 (PostgreSQL mode), for the shipped V1 schema and
 * for each candidate form of V2's closed-set rule. For every case: run the DDL on a pooled connection,
 * insert a valid row, retire every pooled connection (Hikari soft eviction, the same thing maxLifetime
 * does after 30 minutes), then insert a valid row and an invalid row. A second connection is held open
 * throughout, so the database itself never closes. No file under src/ is touched.
 */
public class ConstraintProbe {

	public static void main(String[] args) throws Exception {
		for (String mode : List.of("mem", "file")) {
			run(mode, "V1 link (shipped): LENGTH(code) >= 6 and url <> ''",
					Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql")),
					jdbc -> jdbc.sql("INSERT INTO link (code, url, created_at) VALUES (:c, 'https://example.com/', :t)")
							.param("c", "Ab" + System.nanoTime() % 100000000).param("t", Instant.now().atOffset(ZoneOffset.UTC)).update(),
					jdbc -> jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('short', '', :t)")
							.param("t", Instant.now().atOffset(ZoneOffset.UTC)).update());
			run(mode, "V1 link (shipped), url rule alone: url <> ''",
					Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql")),
					jdbc -> jdbc.sql("INSERT INTO link (code, url, created_at) VALUES (:c, 'https://example.com/', :t)")
							.param("c", "Ab" + System.nanoTime() % 100000000).param("t", Instant.now().atOffset(ZoneOffset.UTC)).update(),
					jdbc -> jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('Abcdef99', '', :t)")
							.param("t", Instant.now().atOffset(ZoneOffset.UTC)).update());
			run(mode, "CHECK (c = 'browser') (one equality)",
					"CREATE TABLE t (c VARCHAR(16) NOT NULL, CONSTRAINT ck_t CHECK (c = 'browser'))",
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('browser')").update(),
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('nope')").update());
			run(mode, "CHECK (c <> 'nope') (one inequality)",
					"CREATE TABLE t (c VARCHAR(16) NOT NULL, CONSTRAINT ck_t CHECK (c <> 'nope'))",
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('browser')").update(),
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('nope')").update());
			run(mode, "CHECK (c IN ('browser','bot','other','unknown'))",
					"CREATE TABLE t (c VARCHAR(16) NOT NULL, CONSTRAINT ck_t CHECK (c IN ('browser', 'bot', 'other', 'unknown')))",
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('browser')").update(),
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('nope')").update());
			run(mode, "CHECK (c = 'browser' OR c = 'bot' OR ...)",
					"CREATE TABLE t (c VARCHAR(16) NOT NULL, CONSTRAINT ck_t CHECK (c = 'browser' OR c = 'bot' OR c = 'other' OR c = 'unknown'))",
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('browser')").update(),
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('nope')").update());
			run(mode, "CHECK (LENGTH(h) = 64)",
					"CREATE TABLE t (h VARCHAR(64) NOT NULL, CONSTRAINT ck_t CHECK (LENGTH(h) = 64))",
					jdbc -> jdbc.sql("INSERT INTO t (h) VALUES (:h)").param("h", "0".repeat(64)).update(),
					jdbc -> jdbc.sql("INSERT INTO t (h) VALUES ('abc')").update());
			run(mode, "revised V2 (V1 + user_agent_class lookup + click with FK and LENGTH check): valid click / unknown class",
					Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql")) + ";" + V2_REVISED
							+ "; INSERT INTO link (code, url, created_at) VALUES ('Probe001', 'https://example.com/', CURRENT_TIMESTAMP)",
					jdbc -> clickInsert(jdbc, "browser", "0".repeat(64)),
					jdbc -> clickInsert(jdbc, "tablet", "0".repeat(64)));
			run(mode, "revised V2: valid click / short hash",
					Files.readString(Path.of("src/main/resources/db/migration/V1__create_link_and_audit_log.sql")) + ";" + V2_REVISED
							+ "; INSERT INTO link (code, url, created_at) VALUES ('Probe001', 'https://example.com/', CURRENT_TIMESTAMP)",
					jdbc -> clickInsert(jdbc, "bot", "f".repeat(64)),
					jdbc -> clickInsert(jdbc, "bot", "abc"));
			run(mode, "FOREIGN KEY to a four-row lookup table (no CHECK)",
					"CREATE TABLE ua (name VARCHAR(16) PRIMARY KEY); INSERT INTO ua (name) VALUES ('browser'), ('bot'), ('other'), ('unknown');"
							+ " CREATE TABLE t (c VARCHAR(16) NOT NULL, CONSTRAINT fk_t FOREIGN KEY (c) REFERENCES ua (name))",
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('browser')").update(),
					jdbc -> jdbc.sql("INSERT INTO t (c) VALUES ('nope')").update());
		}
	}

	/** design.md section 3, revised for DR-04: the closed set is a lookup table, not an IN-list CHECK. */
	static final String V2_REVISED = """
			CREATE TABLE user_agent_class (
			    token VARCHAR(16) PRIMARY KEY
			);
			INSERT INTO user_agent_class (token) VALUES ('browser'), ('bot'), ('other'), ('unknown');
			CREATE TABLE click (
			    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
			    link_id          BIGINT        NOT NULL,
			    clicked_at       TIMESTAMP WITH TIME ZONE NOT NULL,
			    clicked_on       DATE          NOT NULL,
			    referrer         VARCHAR(2048),
			    user_agent_class VARCHAR(16)   NOT NULL,
			    client_hash      VARCHAR(64)   NOT NULL,
			    CONSTRAINT fk_click_link FOREIGN KEY (link_id) REFERENCES link (id) ON DELETE CASCADE,
			    CONSTRAINT fk_click_user_agent_class FOREIGN KEY (user_agent_class) REFERENCES user_agent_class (token),
			    CONSTRAINT ck_click_client_hash_length CHECK (LENGTH(client_hash) = 64)
			);
			CREATE INDEX ix_click_link_day ON click (link_id, clicked_on)
			""";

	static void clickInsert(JdbcClient jdbc, String userAgentClass, String hash) {
		Instant now = Instant.now();
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash) "
				+ "SELECT id, :at, :on, NULL, :ua, :h FROM link WHERE code = 'Probe001'")
				.param("at", now.atOffset(ZoneOffset.UTC)).param("on", java.time.LocalDate.ofInstant(now, ZoneOffset.UTC))
				.param("ua", userAgentClass).param("h", hash).update();
	}

	static void run(String mode, String label, String ddl, Consumer<JdbcClient> valid, Consumer<JdbcClient> invalid) throws Exception {
		String url = mode.equals("mem") ? "jdbc:h2:mem:constraint-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
				: "jdbc:h2:file:./build/constraint-probe-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE";
		try (HikariDataSource pool = new HikariDataSource()) {
			pool.setJdbcUrl(url);
			pool.setUsername("sa");
			pool.setPassword("");
			pool.setMaximumPoolSize(2);
			JdbcClient jdbc = JdbcClient.create(pool);
			try (var keepOpen = pool.getConnection()) {
				for (String statement : ddl.replaceAll("(?m)^--.*$", "").split(";")) {
					if (!statement.isBlank()) {
						jdbc.sql(statement).update();
					}
				}
				String before = attempt(jdbc, valid);
				pool.getHikariPoolMXBean().softEvictConnections();
				String after = attempt(jdbc, valid);
				String rejects = attempt(jdbc, invalid);
				System.out.println("PROBE " + mode + " | " + label + "\n    valid before retirement=" + before
						+ " | valid after retirement=" + after + " | invalid after retirement=" + rejects);
			}
		}
	}

	static String attempt(JdbcClient jdbc, Consumer<JdbcClient> insert) {
		try {
			insert.accept(jdbc);
			return "accepted";
		}
		catch (RuntimeException e) {
			Throwable root = e;
			while (root.getCause() != null) {
				root = root.getCause();
			}
			String message = root.getMessage() == null ? "" : root.getMessage();
			return "rejected (" + (message.contains("closed") ? "DATABASE CLOSED: " : "") + message.lines().findFirst().orElse("") + ")";
		}
	}
}
