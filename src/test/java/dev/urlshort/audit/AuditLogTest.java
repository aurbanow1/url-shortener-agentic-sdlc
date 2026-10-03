package dev.urlshort.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * NFR-A2's persistence-level half: the writer offers exactly one operation and its only statement is
 * an insert; plus the row it writes (actor, request id from the MDC, clock, JSON states).
 */
class AuditLogTest {

	private static final Instant NOW = Instant.parse("2026-10-03T12:00:00.123Z");

	@AfterEach
	void clearMdc() {
		MDC.remove("requestId");
	}

	@Test
	void theWriterOffersOnlyAppendAndItsOnlyStatementIsAnInsert() {
		List<String> publicMethods = Arrays.stream(AuditLog.class.getDeclaredMethods())
				.filter(method -> Modifier.isPublic(method.getModifiers())).map(Method::getName).toList();

		assertThat(publicMethods).containsExactly("append");
		assertThat(AuditLog.INSERT).startsWith("INSERT INTO audit_log").doesNotContainIgnoringCase("UPDATE")
				.doesNotContainIgnoringCase("DELETE");
		assertThat(Arrays.stream(AuditLog.class.getDeclaredFields()).filter(f -> f.getType() == String.class))
				.as("no other SQL constant").hasSize(1);
	}

	@Test
	void appendWritesOneRowWithServerOwnedValues() {
		JdbcClient jdbc = JdbcClient.create(new DriverManagerDataSource("jdbc:h2:mem:audit-log-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"));
		jdbc.sql("CREATE TABLE audit_log (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
				+ " occurred_at TIMESTAMP WITH TIME ZONE NOT NULL, actor VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL,"
				+ " entity VARCHAR(32) NOT NULL, entity_id VARCHAR(64) NOT NULL, request_id VARCHAR(64) NOT NULL,"
				+ " before_state VARCHAR(4096), after_state VARCHAR(4096) NOT NULL)").update();
		AuditLog log = new AuditLog(jdbc, JsonMapper.builder().build(), Clock.fixed(NOW, ZoneOffset.UTC));
		MDC.put("requestId", "rid-1");

		log.append("link.create", "link", "Abc12345", null, Map.of("state", "active"));
		log.append("link.retire", "link", "Abc12345", Map.of("state", "active"), Map.of("state", "retired"));

		List<Map<String, Object>> rows = jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
		assertThat(rows).hasSize(2);
		assertThat(rows.get(0)).containsEntry("ACTOR", "anonymous").containsEntry("REQUEST_ID", "rid-1")
				.containsEntry("ACTION", "link.create").containsEntry("BEFORE_STATE", null)
				.containsEntry("AFTER_STATE", "{\"state\":\"active\"}");
		assertThat(((OffsetDateTime) rows.get(0).get("OCCURRED_AT")).toInstant()).isEqualTo(NOW);
		assertThat(rows.get(1)).containsEntry("BEFORE_STATE", "{\"state\":\"active\"}");
	}
}
