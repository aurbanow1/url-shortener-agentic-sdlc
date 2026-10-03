/**
 * The write side of the audit trail: an Operator can reconstruct every change to the service's
 * state from append-only rows written in the same transaction as the change (NFR-A1, NFR-A2).
 *
 * <p>Belongs here: the {@code audit_log} table and its insert-only writer. Does not belong here:
 * reading or exposing rows (the audit-read slice of mission 02) or deciding what to audit (each
 * feature's service calls {@link dev.urlshort.audit.AuditLog#append} for its own mutations).
 */
@NullMarked
package dev.urlshort.audit;

import org.jspecify.annotations.NullMarked;
