# ADR-0008 — Audit rows: one `audit_log` row per mutation, written in the same transaction, insert-only

- Status: accepted at the `01-create-redirect` plan-lock (2026-10-03T06:38Z)
- Date: 2026-10-03
- Slice: `01-create-redirect` (write side); the read endpoint is mission 02 (`01-audit-read`)

## Context

NFR-A1 requires one append-only row per mutation — actor, action, entity,
before/after, request id, time — written in the same transaction as the
change, and NFR-A2 that the application has no update or delete path for it.
The slice SPEC fixes the actions `link.create` and `link.retire`, the actor
`anonymous` (A-17), "before" empty on create and at least `url` and `state` in
each state (A-18), and that reads, redirects, replays and rejected requests
write nothing.

## Decision

- **Table `audit_log`** (`V1__create_link_and_audit_log.sql`):
  `id` identity, `occurred_at TIMESTAMP WITH TIME ZONE NOT NULL`,
  `actor VARCHAR(64) NOT NULL`, `action VARCHAR(64) NOT NULL`,
  `entity VARCHAR(32) NOT NULL`, `entity_id VARCHAR(64) NOT NULL`,
  `request_id VARCHAR(64) NOT NULL`, `before_state VARCHAR(4096)` (nullable),
  `after_state VARCHAR(4096) NOT NULL`. No foreign key to the audited table:
  a row identifies its subject by `entity` plus the public identifier and
  outlives any future deletion. No secondary index in this slice; the read
  slice adds what its pagination needs.
- **Row content.** `actor` is the literal `anonymous` until authentication
  exists (NFR-S6). `action` is `<entity>.<verb>` (`link.create`,
  `link.retire`). `entity_id` is the public code. `request_id` is the value
  of the MDC key `requestId` set by `RequestIdFilter` (ADR-0003), which equals
  the `X-Request-Id` header of the mutating response. `occurred_at` comes from
  the application `Clock` (ADR-0005). `before_state`/`after_state` are JSON
  text produced by the context's `JsonMapper` from a small record
  (`{"url":…,"state":…}` for links; `NULL` before a create). Rows carry no
  client address, no user agent, no idempotency key.
- **Same transaction.** The service's `@Transactional` use case calls the
  writer after the change; a `DataAccessException` from the insert propagates,
  the change rolls back, and the client receives a `500` problem detail
  (NFR-R6). Nothing catches it.
- **Insert-only writer.** `dev.urlshort.audit.AuditLog` is a component with
  one public method, `append(action, entity, entityId, before, after)`, that
  executes one `INSERT` through `JdbcClient`. No Spring Data repository is
  declared for the table, so no `save(existing)`, `delete` or `deleteAll`
  exists anywhere in the application. The unit suite asserts the class's
  public surface and that its statement is an insert; the code review checks
  the rule on every slice that touches `audit/`.
- **What is not audited.** Reads, redirects, idempotent replays, rejected
  requests, and housekeeping that does not change a representation (releasing
  an expired idempotency key).

## Consequences

- The audit trail is complete for every state change a client can observe and
  is reconstructible by `request_id` against the structured logs.
- Database-level immutability (revoking `UPDATE`/`DELETE`) is an operations
  concern for the container slice; the embedded H2 has a single user.
- A later entity (clicks are not mutations by a Creator and are not audited)
  reuses the same writer and the same `action` naming.
- The `url` in the row is the only place a target may legitimately be
  persisted outside the `link` table; the Operator needs it to see what
  changed.
