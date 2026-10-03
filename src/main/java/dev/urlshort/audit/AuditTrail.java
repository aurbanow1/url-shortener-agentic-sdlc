package dev.urlshort.audit;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the audit trail one keyset page at a time, newest first by {@code id}, the write sequence of
 * SPEC rule 4. One {@code SELECT}; {@link AuditLog} stays the only statement that writes the table. The
 * primary key serves the backwards walk, so a page reads {@code limit + 1} rows at any depth (ADR-0019).
 */
@Component
class AuditTrail {

	static final String PAGE = "SELECT id, occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state"
			+ " FROM audit_log WHERE id < :before ORDER BY id DESC FETCH FIRST :fetch ROWS ONLY";

	private final JdbcClient jdbc;
	private final JsonMapper json;

	AuditTrail(JdbcClient jdbc, JsonMapper json) {
		this.jdbc = jdbc;
		this.json = json;
	}

	/** The newest {@code limit} rows written before the row with id {@code before}. */
	AuditPage page(int limit, long before) {
		List<Row> rows = jdbc.sql(PAGE).param("before", before).param("fetch", limit + 1).query((rs, n) -> {
			String beforeState = rs.getString("before_state");
			return new Row(rs.getLong("id"), new AuditEntry(rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
					rs.getString("actor"), rs.getString("action"), rs.getString("entity"), rs.getString("entity_id"),
					rs.getString("request_id"), beforeState == null ? null : json.readTree(beforeState),
					json.readTree(rs.getString("after_state"))));
		}).list();
		if (rows.size() <= limit) {
			return new AuditPage(rows.stream().map(Row::entry).toList(), null);
		}
		return new AuditPage(rows.subList(0, limit).stream().map(Row::entry).toList(), cursorOf(rows.get(limit - 1).id()));
	}

	/** The opaque {@code next}: base64url of the id, so a client copies it rather than computes it. */
	static String cursorOf(long id) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(Long.toString(id).getBytes(StandardCharsets.US_ASCII));
	}

	private record Row(long id, AuditEntry entry) {
	}
}
