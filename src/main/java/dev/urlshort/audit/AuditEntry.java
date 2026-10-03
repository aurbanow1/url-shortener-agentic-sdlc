package dev.urlshort.audit;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * One audit row as the read returns it, exactly SPEC rule 3's fields, every value as stored.
 *
 * @param occurredAt when the mutation happened, by the application clock
 * @param actor who acted; {@code anonymous} for every row (NFR-S6)
 * @param action what happened, for example {@code link.retire}
 * @param entity the kind of thing changed, for example {@code link}
 * @param entityId the public identifier of the thing changed
 * @param requestId the {@code X-Request-Id} of the request that made the change
 * @param before the stored state before the change; {@code null} when the change created the thing
 * @param after the stored state after the change
 */
record AuditEntry(
		@Schema(description = "When the mutation happened (UTC)", example = "2026-10-03T17:29:41.033Z") Instant occurredAt,
		@Schema(description = "Who acted", example = "anonymous") String actor,
		@Schema(description = "What happened", example = "link.retire") String action,
		@Schema(description = "The kind of thing changed", example = "link") String entity,
		@Schema(description = "The public identifier of the thing changed", example = "jHkOIMpK") String entityId,
		@Schema(description = "The X-Request-Id of the request that made the change") String requestId,
		// an open JSON object: without Object.class springdoc describes JsonNode's own getters; types, not
		// type, because the document is OpenAPI 3.1
		@Schema(implementation = Object.class, types = { "object", "null" },
				description = "The state before the change, as stored; null for a create") @Nullable JsonNode before,
		@Schema(implementation = Object.class, types = "object", description = "The state after the change, as stored") JsonNode after) {
}
