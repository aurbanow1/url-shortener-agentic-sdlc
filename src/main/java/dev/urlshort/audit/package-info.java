/**
 * The audit trail: an Operator can reconstruct every change to the service's state from append-only
 * rows written in the same transaction as the change (NFR-A1, NFR-A2), and read them through the
 * loopback-only {@code GET /api/audit} (FR-17, NFR-S6).
 *
 * <p>Belongs here: the {@code audit_log} table, its insert-only writer and its read-only page. Does not
 * belong here: deciding what to audit (each feature's service calls
 * {@link dev.urlshort.audit.AuditLog#append} for its own mutations).
 */
@NullMarked
package dev.urlshort.audit;

import org.jspecify.annotations.NullMarked;
