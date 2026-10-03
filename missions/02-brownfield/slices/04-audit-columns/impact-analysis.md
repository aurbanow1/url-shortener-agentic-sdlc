# Impact analysis — 04-audit-columns

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design-agent@urlshort-factory`,
2026-10-03, for SPEC `e28cfea` (requirements PASS; RQ-01 fixed).

**Baseline.** `main` at `80ca44c`, whose product code is byte-identical to `f6dd29e`. The w1 slices
are not merged yet. The shipped writes to `link` and `audit_log` were read on `main`:
- `LinkService.create` saves a `Link` record (an `INSERT` of `code`, `url`, `created_at`,
  `retired_at`, `idempotency_key`);
- `LinkRepository.retire` is `UPDATE link SET retired_at = :at WHERE id = :id AND retired_at IS NULL`;
- `LinkRepository.releaseIdempotencyKey` is `UPDATE link SET idempotency_key = NULL WHERE id = :id`;
- `AuditLog.INSERT` writes eight columns with the actor literal `'anonymous'`.

The observed shipped rows are in `02-click-retention/design-probe/baseline-f6dd29e.txt`: one
`link.create` audit row and its link. The migration and the designed statements were run in
[`design-probe/output.txt`](design-probe/output.txt) (K1–K5).

**Re-check at plan-lock (SPEC *Non-functional*).**
- `01-audit-read` changes `audit/` and `02-click-retention` adds V3. Both merge first. This
  analysis is re-read against the merged `main` before the lock.
- The one fact it depends on from `01-audit-read`: its read selects **named columns**
  (`SELECT id, occurred_at, actor, action, entity, entity_id, request_id, before_state, after_state …`,
  ADR-0019). The new `audit_log` columns therefore cannot reach `GET /api/audit` (AC-8). Re-check:
  `grep -n "SELECT" src/main/java/dev/urlshort/audit/AuditTrail.java` on the merged `main`.

## Change in one sentence

The human's audit-column policy reaches `link` and `audit_log` through one expand migration, V4.
- `link` keeps its `created_at` and gains `updated_at` (service clock, stamped by its three writes)
  and `created_by`/`updated_by`.
- `audit_log` gains row-write `created_at`/`updated_at` (database clock) and
  `created_by`/`updated_by` equal to its actor.

Nothing a client sees changes. Requirements: the audit-column policy, NFR-X2, FR-13, NFR-A2.

## Impacted modules

Found by reading every writer of the two tables (`grep -rn "link\b\|audit_log" src/main`) and every
test that inserts or selects them
(`grep -rln "INTO link\|FROM link\|audit_log" src/test src/functionalTest`).

| Class or file | Change | Callers and dependents |
|---|---|---|
| `db/migration/V4__add_link_audit_columns.sql` | **new**, verbatim from `design-probe/migration/` | Flyway at startup |
| `link.LinkRepository` | `retire`'s SQL also sets `updated_at = :at, updated_by = 'anonymous'` (same signature). One new method, `stamp(Long id, Instant at)`: `UPDATE link SET updated_at = :at, updated_by = 'anonymous' WHERE id = :id` | `LinkService` |
| `link.LinkService` | `create` calls `links.stamp(bound.id(), now)` after `releaseIdempotencyKey`, and `links.stamp(link.id(), now)` after `save`, both in the existing transaction. `retire` is unchanged | `LinkController`, `RedirectController` (unchanged) |
| `link.Link`, `LinkResponse`, `LinkSnapshot` | **unchanged**: the record does not map the new columns, so no response can read them (AC-8) | — |
| `audit.AuditLog` | **unchanged**: the defaults fill the new columns, and `created_by`/`updated_by` default to `'anonymous'`, the literal actor | `LinkService` |
| `audit.AuditTrail`, `AuditController` (`01-audit-read`, merging first) | **unchanged**: named-column `SELECT` | — |

**Not touched:** `click/`, `web/`, `docs/api/openapi.json`, `application.properties`.

## Impacted endpoints

None. `POST /api/links`, `GET/DELETE /api/links/{code}`, `GET /{code}`, the statistics and
`GET /api/audit` keep their status, headers and bodies. `createdAt` and the audit row's `actor`
are exposed exactly as before. No new column is read into a response (SPEC rule 7, AC-8).

## Impacted schema and data

- **V4, expand only** (K1):
  - `link` gains `updated_at` (`TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL`) and
    `created_by`/`updated_by` (`VARCHAR(64) DEFAULT 'anonymous' NOT NULL`).
  - `audit_log` gains `created_at`/`updated_at` (same type and default) and `created_by`/`updated_by`
    (`VARCHAR(64)`, the width of `actor`).
  - Every shipped column, value and constraint is unchanged (K2).
- **Backfill** (K2):
  - each link's `updated_at = COALESCE(retired_at, created_at)`, its latest known write;
  - each audit row's `created_at = updated_at = LEAST(occurred_at, CURRENT_TIMESTAMP)`, so a row
    whose event time is ahead of the database clock is capped at the upgrade; its actors equal its
    `actor`.
- **Volume and cost:** 100 000 links and 150 000 audit rows upgraded in 6.2 s wall (K5). A
  directory written since the release holds far fewer.
- **Rollback**, written in V4's header and run in K4: drop the seven columns and V4's history row.
  That gives back the exact V3 schema and constraints, and V4 re-applies with every link backfilled.

## Impacted data flows

| Flow | Change |
|---|---|
| Create | `INSERT` (unchanged) then `stamp(id, now)` in the same transaction: `updated_at` equals `created_at`, on the service clock |
| Create with an expired key | `releaseIdempotencyKey(A)` (unchanged), then `stamp(A, now)`, where `now` is `B`'s `created_at` (AC-4) |
| Retire | the conditional `UPDATE` also stamps (K3b: a second retire changes 0 rows, so no stamp, `410`) |
| Failed retire (audit write fails) | the transaction rolls back, stamp included (AC-5) |
| Audit write | unchanged statement; the database clock and `'anonymous'` defaults fill the new columns |
| Reads, redirects, statistics, the audit read | unchanged; none reads a new column |

## Blast radius

| If this is wrong | Worst case | Detection |
|---|---|---|
| A new column reaches a response | the client contract changes (FR-13) | AC-8; `Link` unchanged; the audit read's named-column `SELECT` |
| A refused or replayed request stamps a link | wrong `updated_at` | AC-5 |
| The backfill misdates rows | Operators misread history | AC-7 (K2) |
| A v1-shaped insert fails on the new `NOT NULL` columns | shipped tests outside the territory break | AC-9; K3 shows defaults fill them |
| The rollback does not restore V3 | an irreversible upgrade | AC-11 (K4) |

## Compatibility (FR-13)

- **Every response, header and the API document:** unchanged (AC-8).
- **Stored values:** every shipped column keeps its value (K2).
- **v1-shaped inserts** (`click/ClickSchemaTest`, `web/ObservabilityJourneyTest` insert `link` with
  v1 columns): succeed and leave every audit column filled (K3, AC-9).

## Test impact

**Unchanged, and why** (AC-9 requires every shipped test unchanged):
- `LinkServiceTest`: it pins `retire(Long, Instant)`, `releaseIdempotencyKey(Long)` and the
  six-argument `Link`, and all of them stay. The added `stamp` calls hit an unstubbed mock, and no
  test checks strict interactions (`grep verifyNoMoreInteractions` finds none in `link/`).
- `LinkTest`: `Link` is unchanged.
- `AuditLogTest`, `AuditJourneyTest`, `StatsJourneyTest`: they read `SELECT * FROM audit_log` with
  `containsEntry` or compare rows before and after. Extra columns keep both true.
- `ClickSchemaTest`, `ObservabilityJourneyTest`: v1-shaped `link` inserts are filled by the defaults
  (K3).

**Added:** the AC-1 to AC-11 journeys and units (design §7).

**Removed:** none.

## Observability impact

None. No log event, metric or document changes.

## Risks and mitigations, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | A new column leaks into a response | the record does not map it; the audit read names its columns; AC-8 | design → QA → plan-lock re-check |
| 2 | A shipped test breaks | signatures and the record kept; defaults for v1 inserts; K3 | design → QA (AC-9) |
| 3 | Flyway numbering clash | V4 assumed after V3; mission 03's `01-analytics-v2` takes none | plan-lock |
| 4 | Upgrade time | 6.2 s for 100k links and 150k audit rows (K5) | release |

## Self-check

- Every writer of both tables and every test touching them was read. The shipped signatures this
  design keeps are named, with the tests that pin them.
- The schema change was run on a V3 directory: columns, backfill, v1 inserts, the designed
  statements, rollback and re-apply, cost (K1–K5).
- Not verified here: the merged `main` (re-check at plan-lock), the shipped suite against a candidate
  (AC-9, QA).
