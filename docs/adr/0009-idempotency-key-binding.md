# ADR-0009 — `Idempotency-Key`: bound on the link row by its `201`, honoured for 24 h from creation

- Status: proposed (becomes accepted at the `01-create-redirect` plan-lock)
- Date: 2026-10-03
- Slice: `01-create-redirect`

## Context

FR-9 and NFR-R5 (24 h, decided at the mission plan-lock) require a retried
create with the same key to return the first link. The slice SPEC (rule 5,
A-9, A-10, and the requirements-review finding RQ-01) fixes the full contract:
a key binds only by a `201`; a refused or failed request never changes a
binding; a bound key replays with `201` and the current representation; a
different `url` under a bound key is `422 mismatch`; after 24 h the key is as
if never seen; two concurrent creates with one key produce at most one link;
the key space is global; the key is 1–255 visible ASCII characters.

## Decision

- **Storage: a nullable `idempotency_key VARCHAR(255)` column on `link` with
  `UNIQUE (idempotency_key)`.** A key binds to exactly one link and only by the
  `201` that created it, so the binding time is the row's `created_at` and no
  second table, foreign key or join is needed. Multiple `NULL`s are allowed by
  both engines.
- **Window.** A binding is live while `now < created_at + 24 h`, `now` being
  the application `Clock` (ADR-0005). The window starts at the binding `201`
  and is never extended or shortened by later requests of any outcome.
- **Replay.** Same key, same `url`, live binding: `201`, the same `Location`,
  the link's **current** representation (`state` may be `retired`); no row
  written, no audit row.
- **Mismatch.** Same key, different `url`, live binding: `422 Unprocessable
  Content`, `errors: [{field: "Idempotency-Key", rule: "mismatch"}]`; nothing
  written; the binding and its window are untouched.
- **Expiry.** Same key, binding older than 24 h: the service nulls the old
  row's key (`UPDATE link SET idempotency_key = NULL WHERE id = :id`) and
  creates a new link bound to the key, in one transaction. The old link keeps
  existing and redirecting; it simply no longer owns the key.
- **Failures never bind.** Validation (`url` rules, key format `400 format`)
  runs before any lookup or write; a `422`, a `413`, a `500` all leave the
  table as it was. An unbound key stays unbound (AC-21); a bound key keeps its
  link (AC-18, AC-19).
- **Concurrency.** The unique constraint is the guarantee: of two creates that
  both see no binding, the second insert fails and that request answers
  `500` (fail closed, nothing stored); the client's next retry replays. No
  in-service retry: catching the violation inside the `@Transactional` method
  cannot work because the repository's own transactional proxy has already
  marked the shared transaction rollback-only, and a second transaction is
  machinery the SPEC does not ask for. `// ponytail: same-key race answers
  500; add an out-of-transaction retry if clients hit it`.
- **Scope.** One global key space (no clients or tenants, NFR-S6). The header
  is read only on `POST /api/links`; other endpoints ignore it.

## Consequences

- The `link` row carries the client's key; it is bounded, visible ASCII, and
  never logged or returned.
- Two clients that choose the same key collide: the second sees the first's
  link (replay) or a `422`. Accepted by the SPEC; clients should use UUIDs.
  Per-client scopes would move the binding to its own table keyed by
  `(client, key)` and are the change that reopens this ADR.
- Storing the first response verbatim (the IETF draft's alternative) was not
  chosen: the representation is a pure function of the row, and the SPEC
  wants the current state on replay.
