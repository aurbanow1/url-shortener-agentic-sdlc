package dev.urlshort.audit;

import java.time.Clock;
import java.time.ZoneOffset;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Appends one row to {@code audit_log} per successful mutation. Insert-only by construction: this
 * class holds the application's only statement on the table, an {@code INSERT}, so no code path can
 * update or delete a row (NFR-A2).
 *
 * <p>Callers invoke {@link #append} inside the transaction of the change it records; a failure here
 * propagates and rolls the change back (NFR-A1, AC-24). The actor is always {@code anonymous}
 * (NFR-S6), the request id is the {@code X-Request-Id} of the current request, read from the MDC,
 * and the time comes from the application clock. Decision record: ADR-0008.
 */
@Component
public class AuditLog {

	static final String INSERT = "INSERT INTO audit_log"
			+ " (occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state)"
			+ " VALUES (:occurredAt, 'anonymous', :action, :entity, :entityId, :requestId, :before, :after)";

	private final JdbcClient jdbc;
	private final JsonMapper json;
	private final Clock clock;

	/**
	 * Creates the writer.
	 *
	 * @param jdbc the context's JDBC client, bound to the caller's transaction
	 * @param json serialises the before and after states
	 * @param clock the application clock that stamps {@code occurred_at}
	 */
	public AuditLog(JdbcClient jdbc, JsonMapper json, Clock clock) {
		this.jdbc = jdbc;
		this.json = json;
		this.clock = clock;
	}

	/**
	 * Records one mutation in the current transaction.
	 *
	 * @param action what happened, for example {@code link.create}
	 * @param entity the kind of thing changed, for example {@code link}
	 * @param entityId the public identifier of the thing changed, for example the short code
	 * @param before the state before the change, serialised as JSON; {@code null} when the change
	 *        created the thing (stored as SQL {@code NULL})
	 * @param after the state after the change, serialised as JSON
	 * @throws org.springframework.dao.DataAccessException when the row cannot be written; the caller's
	 *         transaction then rolls back
	 */
	public void append(String action, String entity, String entityId, @Nullable Object before, Object after) {
		jdbc.sql(INSERT)
				.param("occurredAt", clock.instant().atOffset(ZoneOffset.UTC))
				.param("action", action)
				.param("entity", entity)
				.param("entityId", entityId)
				.param("requestId", MDC.get("requestId"))
				.param("before", before == null ? null : json.writeValueAsString(before))
				.param("after", json.writeValueAsString(after))
				.update();
	}
}
