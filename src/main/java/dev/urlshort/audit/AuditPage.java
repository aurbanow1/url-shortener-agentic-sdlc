package dev.urlshort.audit;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * One page of the audit trail (SPEC rule 5): the rows newest first, and the cursor of the next page.
 *
 * @param items at most {@code limit} rows, newest first by write sequence
 * @param next the {@code cursor} of the next page; {@code null} when no older row is committed
 */
record AuditPage(List<AuditEntry> items,
		@Schema(nullable = true, description = "Pass back as cursor for the next page; null on the last page") @Nullable String next) {
}
