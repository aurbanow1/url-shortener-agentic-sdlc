package dev.urlshort.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The DR-04 regression: on H2 2.4.240 a multi-value {@code CHECK} stops working once the connection
 * that created it is retired. The real migrations run on a fresh database with its own pool, the
 * pool retires the DDL connection while another keeps the database open, and the click constraints
 * must still accept every valid class and reject invalid rows.
 */
class ClickSchemaTest {

	private static final String HASH = "a".repeat(64);

	@Test
	void theClickConstraintsStillWorkAfterTheDdlConnectionIsRetired() throws Exception {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:click-schema-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
		config.setUsername("sa");
		// one connection is held to keep the database open; Flyway needs two of its own
		config.setMaximumPoolSize(3);
		try (HikariDataSource dataSource = new HikariDataSource(config);
				Connection keepOpen = dataSource.getConnection()) {
			Flyway.configure().dataSource(dataSource).load().migrate();
			JdbcClient jdbc = JdbcClient.create(dataSource);
			jdbc.sql("INSERT INTO link (code, url, created_at) VALUES ('Abc12345', 'https://example.com/', CURRENT_TIMESTAMP)")
					.update();
			long linkId = jdbc.sql("SELECT id FROM link WHERE code = 'Abc12345'").query(Long.class).single();
			insert(jdbc, linkId, "browser", HASH);

			dataSource.getHikariPoolMXBean().softEvictConnections();

			for (String userAgentClass : List.of("browser", "bot", "other", "unknown")) {
				insert(jdbc, linkId, userAgentClass, HASH);
			}
			assertThat(jdbc.sql("SELECT COUNT(*) FROM click").query(Long.class).single()).isEqualTo(5);
			assertThatThrownBy(() -> insert(jdbc, linkId, "tablet", HASH))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThatThrownBy(() -> insert(jdbc, linkId, "browser", "abc"))
					.isInstanceOf(DataIntegrityViolationException.class);
			assertThat(keepOpen.isClosed()).isFalse();
		}
	}

	private static void insert(JdbcClient jdbc, long linkId, String userAgentClass, String hash) {
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (:linkId, CURRENT_TIMESTAMP, CURRENT_DATE, NULL, :class, :hash)")
				.param("linkId", linkId).param("class", userAgentClass).param("hash", hash).update();
	}
}
