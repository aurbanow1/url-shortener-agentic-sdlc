---
id: OPR.99.0.3.4
slice: 04-audit-columns
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "Every shipped table that records links and the audit trail carries the audit columns the human decided (created_at, updated_at, and created_by/updated_by where an actor exists), added by one expand migration with a written rollback, with existing links and audit rows unchanged in meaning."
depends_on: []
approved-spec-by: orchestration-lead@urlshort-factory
approved-spec-at: 2026-10-03T22:34:49.645Z
locked-artifacts:
  - name: SPEC.md
    path: SPEC.md
    kind: spec
  - name: design.md
    path: design.md
    kind: spec
provenance: transport:v1
approved-by: orchestration-lead@urlshort-factory
approved-at: 2026-10-04T05:26:34.166Z
---

# Slice 04 — Audit Columns

## Intent

Every shipped table that records links and the audit trail carries the audit columns the human decided (created_at, updated_at, and created_by/updated_by where an actor exists), added by one expand migration with a written rollback, with existing links and audit rows unchanged in meaning.

The human decided on 2026-10-03 that every table says when each row was
written and changed, and by whom. `link` has only its creation time, and
`audit_log` has only the time of the event it records. An Operator reading
rows directly cannot tell when a link was last changed (retired, or its
idempotency key released) or when an audit row was written. `click` and
`user_agent_class` get the same columns in `02-click-retention`. This slice
covers `link` and `audit_log` and follows the same pattern (ADR-0020).
Nothing a Creator, Visitor or Analyst sees may change.

## Mini-requirements

### Requirements covered

| Id | Requirement (short) | Proven by |
|---|---|---|
| Audit-column policy (human decision 2026-10-03, `qitem-20261003175330-fb054f2f`; `docs/guidance/databases.md` §2) | every table carries `created_at`/`updated_at`, and `created_by`/`updated_by` where an actor exists; `updated_at` maintained on every write; an existing table gets them in an expand migration with a written rollback | AC-1 to AC-7 |
| NFR-X2 | a versioned migration with a written rollback | AC-1, AC-7, AC-11 |
| FR-13 | existing links, redirects, statistics and the audit read behave exactly as before, migration included | AC-7, AC-8, AC-9 |
| NFR-A2 | the audit table has no update or delete path in the application | AC-6 |
| NFR-P1, NFR-O2 (inherited) | no client value stored outside the reduced columns, or logged | AC-10 |
| NFR-M1, M2 (cross-cutting) | coverage gate; ADR before dependent code | proof contract; *Non-functional* |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Operator (reads rows to investigate; relies on knowing when and by whom each row was written and changed).
- **Secondary:** Creator, Visitor, Analyst (whose responses must not change).

### User stories

- As an Operator, I want every link row to say when it was created and last changed and by whom, so that I can investigate a link's history from the row itself.
- As an Operator, I want every audit row to say when the row was written and by whom, separately from when the recorded event happened, so that the trail follows the same policy as every other table.
- As an Operator, I want the change applied to my existing data directory in place, with a written rollback, so that upgrading is safe and reversible.
- As a Creator, Visitor or Analyst, I want every response to stay exactly as it was, so that the change costs me nothing.

### Acceptance criteria

The "suite clock" is the functional suite's controlled clock (`FunctionalClock`,
mission 01). Stored rows are inspected by the suite, as in `02-analytics` and
`02-click-retention`. "The audit columns" means `created_at`, `updated_at`,
`created_by` and `updated_by`. The actor of every request is `anonymous`
(NFR-S6; `01-create-redirect` rule 9).

#### Schema

- **AC-1 — Both tables carry the audit columns, and nothing else changes.** [policy, NFR-X2]
  GIVEN the candidate's migrations applied to an empty database
  WHEN the suite reads the schema
  THEN `link` has `updated_at` (timestamp with time zone, not null), `created_by` and `updated_by` (not null), and keeps its shipped `created_at` with the same type, nullability and meaning. `audit_log` has `created_at` and `updated_at` (timestamp with time zone, not null) and `created_by` and `updated_by` (not null), and keeps `occurred_at`. Every other shipped column and constraint of both tables is unchanged: none dropped, renamed or retyped.

#### Writes keep the columns current

- **AC-2 — A create stamps the new link and its audit row.** [policy]
  GIVEN the suite clock at instant `t`
  WHEN a Creator creates a link `C`
  THEN `C`'s row has `created_at` = `updated_at` = `t` and `created_by` = `updated_by` = `anonymous`. The `link.create` audit row has `created_at` = `updated_at` (both filled) and `created_by` = `updated_by` = `anonymous`, equal to its `actor`.

- **AC-3 — A retire moves the link's update stamp only.** [policy]
  GIVEN link `C` created at suite-clock instant `t0`
  WHEN the suite clock moves to `t1` and a Creator retires `C`
  THEN `C`'s row has `updated_at` = `t1` (equal to its retirement time) and `updated_by` = `anonymous`, and its `created_at` (`t0`) and `created_by` are unchanged. The `link.retire` audit row is stamped as in AC-2.

- **AC-4 — A release of an expired idempotency key is a write.** [policy]
  GIVEN link `A` created with idempotency key `K` at suite-clock instant `t0`
  WHEN the suite clock moves past `A`'s 24-hour key window to `t1` and a Creator creates a link with key `K` and a different URL, which binds `K` to new link `B` and releases it from `A`
  THEN `A`'s row has `updated_at` = `t1` (equal to `B`'s `created_at`) and `updated_by` = `anonymous`; `A`'s `created_at`, `created_by`, `url` and retirement are unchanged; and `B` is stamped as in AC-2.

- **AC-5 — Requests that change nothing stamp nothing.** [policy, FR-13]
  GIVEN link `C` with its stamps recorded, created with key `K`
  WHEN the suite sends, one after another: a create replaying `K` within its window (same URL); a create with `K` and a different URL within its window (`422`); `GET /api/links/C`; `GET /C`; a retire of `C`, followed by a second retire of `C` (`410`); and a retire whose audit write the suite makes fail (as in `01-create-redirect` AC-24, on a fresh link `D`)
  THEN after each request every stamp of every link is as it was before that request, except `C`'s after its first, successful retire (AC-3). `D` stays active with its stamps unchanged, and no audit row was added for `D`.

#### The audit trail stays append-only

- **AC-6 — Audit rows are never updated.** [NFR-A2, policy]
  GIVEN audit rows written by creates and retires, before and after the upgrade
  WHEN the suite reads `audit_log`
  THEN every row has `updated_at` = `created_at` and `updated_by` = `created_by`; and the application still holds no statement that updates or deletes an `audit_log` row (the shipped repository test of NFR-A2 still passes, extended to the new columns if it enumerates columns).

#### Upgrade and rollback

- **AC-7 — An existing data directory upgrades in place.** [policy, NFR-X2, FR-13]
  GIVEN a data directory written by the shipped service at `f6dd29e` holding active links, retired links, a link whose key was released, and their audit rows
  WHEN the candidate starts on that directory
  THEN migrations apply without error; every shipped column of every link and audit row holds the value it held before; every pre-existing link has `updated_at` equal to its `retired_at` when retired and to its `created_at` otherwise, and `created_by` = `updated_by` = `anonymous`; and every pre-existing audit row has `created_at` = `updated_at` (filled, not later than the upgrade) and `created_by` = `updated_by` = its `actor`.

- **AC-11 — The written rollback restores the previous schema.** [NFR-X2]
  GIVEN a copy of a database migrated by the candidate
  WHEN the rollback written in the new migration's header is run on it
  THEN the schema of `link` and `audit_log` equals the schema before this slice's migration (same columns, types, nullability and constraints); every shipped column value is unchanged; and starting the candidate on the rolled-back copy applies the migration again without error.

#### Nothing visible changes

- **AC-8 — No response, document or log shows the new columns.** [FR-13]
  GIVEN links created and retired after the upgrade
  WHEN a client creates a link, reads it with `GET /api/links/<code>`, opens `GET /<code>`, reads its statistics, and an Operator reads `GET /api/audit`
  THEN each response has exactly the status, headers and body fields it had before this slice (`01-create-redirect`, `02-analytics` and `01-audit-read` contracts). In particular each `GET /api/audit` row has exactly rule 3's eight fields of `01-audit-read`. No response body or header gains a member, header or value read from a column this slice adds (`link.updated_at`, `link.created_by`, `link.updated_by`, and the four new `audit_log` columns). In particular, after a retire `GET /api/links/<code>` still shows the link's shipped `createdAt` and no update time. Values already exposed stay exactly as before: `createdAt` (the reused `link.created_at`) and the audit row's `actor`. `docs/api/openapi.json` is unchanged.

- **AC-9 — The shipped suite passes unchanged.** [FR-13]
  GIVEN the unit and functional suites as they stand on the merged `main` this slice starts from
  WHEN they run against the candidate
  THEN every shipped test passes without change. This includes the tests outside this slice's territory that insert `link` rows naming only the v1 columns (`click/ClickSchemaTest`, `web/ObservabilityJourneyTest`): such an insert still succeeds and leaves every audit column filled.

#### Privacy

- **AC-10 — The audit columns hold no client value.** [NFR-P1, NFR-O2]
  GIVEN creates and retires sent with a `User-Agent` canary, an `X-Forwarded-For` of `192.0.2.10`, and an `Idempotency-Key` canary
  WHEN the suite reads every audit column of `link` and `audit_log`
  THEN no audit column contains the canaries, the address, a request id or a URL; the actor columns hold only `anonymous` (or `system`, if the design seeds a row, rule 3).

### Business rules

1. **Row time and business time.** `link.created_at` is the shipped creation time on the service clock. It is what `GET /api/links/<code>` reports and where the idempotency window starts. It already exists, so it is the policy's `created_at` for `link`, kept as is. The link's `updated_at` is stamped on the same clock, so a link row can never read as updated before it was created. `audit_log.occurred_at` stays the time of the recorded event. The audit row's `created_at`/`updated_at` say when the row was written; which clock stamps them is the design's (ADR-0020 uses the database clock).
2. **What counts as a write to a link.** Exactly three operations change a link row: its create, its retire, and the release of its idempotency key by a later create after the key's window (AC-4). Each sets `updated_at` and `updated_by`. Nothing else writes: replays, refused creates (`422`), refused retires (`410`), failed retires rolled back with their audit write, reads, redirects and statistics leave every stamp unchanged.
3. **Actor.** Every request is anonymous (NFR-S6), so `created_by` and `updated_by` hold `anonymous` for link rows. An audit row's `created_by` and `updated_by` equal its `actor`, which is `anonymous` today. `system` is reserved for rows written by no request (none exist in these tables today). No actor column ever holds a client-derived value.
4. **The audit trail is append-only.** An audit row is written once and never updated, so `updated_at` = `created_at` and `updated_by` = `created_by` for its whole life (NFR-A2). No update or delete path is added for the new columns.
5. **Expand only, one migration.** One new versioned migration with the next free Flyway number. V4 is assumed, after `02-click-retention`'s V3, and the plan-lock confirms it. It adds the columns and fills pre-existing rows (rule 6). It drops, renames or retypes nothing, and its header carries a rollback that removes only what it added (AC-11).
6. **Backfill.** A pre-existing link's `updated_at` is its latest known write time: `retired_at` when retired, otherwise `created_at`. A key release left no time, so this is the best known value. Its actors are `anonymous`. A pre-existing audit row's `created_at` and `updated_at` are equal and no later than the upgrade, and its actors equal its `actor`. The exact time value is the design's, recorded in the migration header (ADR-0020 uses the event time).
7. **Invisible to clients.** No endpoint, response field, header or API document changes, and no column this slice adds is read into a response. Shipped values that coincide with an audit column (`createdAt`, the audit row's `actor`) are exposed exactly as before. The audit read's row representation stays exactly `01-audit-read` rule 3's eight fields. The audit columns are for operators reading rows, never for logic.

### Non-functional

- **NFR-X2.** One expand migration, next free Flyway number (V4 assumed), written rollback in its header, proven by AC-1, AC-7 and AC-11.
- **Upgrade cost.** The migration runs before readiness. The design measures it on a realistic `link`/`audit_log` size and records it in the impact analysis (ADR-0020 recorded `click`'s).
- **ADR before dependent code (NFR-M2).** ADR-0020's pattern applies. The design records, in ADR-0020's consequences or a new ADR, how `link` departs from it: `created_at` is reused, and `updated_at` is on the service clock (rule 1) and maintained by the three writes (rule 2).
- **Brownfield.** The design writes `impact-analysis.md` over `link/`, `audit/`, V1 and the shipped tests that write `link` rows, before `design.md`. It re-checks it against the merged `main` at plan-lock (`01-audit-read` changes `audit/`).
- **Coverage gate (NFR-M1).** `scripts/gw check` with 100 % line and branch coverage on merged unit and functional data; honest gaps in `docs/qa/GAPS.md`.
- **Territory.** `link/` and `audit/` (main, unit, functional) and `db/migration/`. Not `click/`, `web/` or `docs/api/openapi.json`. AC-9 makes the out-of-territory tests a constraint, not an edit.

### Scope

**In scope**

- The expand migration adding the audit columns to `link` and `audit_log`, with backfill and a written rollback.
- Keeping `updated_at`/`updated_by` current on the three link writes, and stamping new audit rows.
- Unit and functional tests, coverage, traceability, gaps, and the proof artifacts below.

**Explicitly out of scope**

- `click` and `user_agent_class`: `02-click-retention` (its AC-16, V3).
- Exposing any audit column through an endpoint, the audit read or the API document (rule 7).
- Authentication or any actor other than `anonymous` (NFR-S6). `system` is named only as the policy's reserved value.
- Soft deletes or `deleted_at`: no table here deletes rows.
- Changing `link.created_at`, `audit_log.occurred_at` or any other shipped column's meaning or clock.
- Recording when a pre-existing link's key was released (that time was never stored; rule 6).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | `link` already has `created_at`, the API-visible creation time on the service clock. Is it the policy's `created_at`, or is a separate row-time column added? | reuse it; add a second creation column | **assumed** reuse it (rule 1). The policy names the column and it exists. A second creation time would be a duplicate the Operator has to reconcile, and changing the existing one would break FR-13 (it is in the API and starts the key window). Safe: expand only; nothing visible changes. |
| A-2 | Which clock stamps `link.updated_at`? | the service clock, like `created_at` and `retired_at`; the database clock | **assumed** the service clock (rule 1, AC-2 to AC-4). Mixing clocks in one row could show a link updated before it was created whenever the service clock is shifted (the suite does this, and an Operator can). Safe: every other time in the row already uses it. `audit_log`'s row times stay the design's, because `occurred_at` is a separate event time there. |
| A-3 | What do `created_by`/`updated_by` hold? | `anonymous`; `system`; omit them ("no actor") | **assumed** `anonymous` for link rows and the audit row's own `actor` for audit rows (rule 3). Every request is an unauthenticated actor (NFR-S6), so an actor exists, and `01-create-redirect` already records it as `anonymous`. Safe: static tokens with no client identity (AC-10). |
| A-4 | Does the audit read (`01-audit-read`, `GET /api/audit`) expose the new columns? | expose them; do not | **decided** by `01-audit-read`'s locked SPEC (`7b753b7`) rule 3: each row has exactly eight fields, nothing added. Exposing them would change that contract and the API document, outside this slice's territory. AC-8 proves it. |
| A-5 | Is the release of an expired idempotency key a write that moves `updated_at`? | yes; no (not a client-visible change) | **assumed** yes (rule 2, AC-4). The policy says `updated_at` is maintained on every write, and the release changes the row (`idempotency_key` set to null). Safe: nothing visible changes, and the stamp tells an Operator why a link lost its key. |
| A-6 | How are pre-existing rows backfilled? | the latest known write time; the migration time | **assumed** the latest known write time for links (rule 6). For audit rows, equal stamps no later than the upgrade, with the value the design's. Safe: no row claims a time later than it can have been written, and `occurred_at` keeps the event's own time. |
| A-7 | Which Flyway number? | V4; another | **assumed** V4, after `02-click-retention`'s V3, as the dispatch states; the plan-lock confirms it against mission 03's `01-analytics-v2` (`slice.yaml`). |

No question is parked on `human@kernel`. The policy itself is the human's
decision. Every row above is a reversible default inside it.

## Proof contract

- [ ] AC-1 to AC-11 are each covered by a named test (functional, or unit where the design says so), green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional data (NFR-M1).
- [ ] Unit and functional JaCoCo reports committed under `docs/qa/coverage/04-audit-columns/unit/` and `docs/qa/coverage/04-audit-columns/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `04-audit-columns` mapping AC-1 to AC-11 and rules 1 to 7 to their tests, with the requirement id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `04-audit-columns` ("None for this slice" or each honest gap with its compensating check), and the interim row for `link`/`audit_log` audit columns, if the lead filed one, is closed by this slice's merge.
- [ ] `proof/` holds a by-effect capture from the running service started on an `f6dd29e` data directory: the columns of `link` and `audit_log` with type and nullability after the upgrade, and the audit columns of a sample of pre-existing and new rows (AC-1, AC-7, by effect).
- [ ] `proof/` holds the rollback run of AC-11: the schema before the migration, after it, and after the rollback, and the re-apply.
- [ ] The migration's header carries a written rollback, and the review records that the rollback was read and run (NFR-X2).
- [ ] `impact-analysis.md` exists before `design.md` and is re-checked against the merged `main` at plan-lock.

## Source material

- Human decision `qitem-20261003175330-fb054f2f` (verbatim in `missions/02-brownfield/NOTES.md` §2, 18:00Z); `docs/guidance/databases.md` §2, §4 and §8.
- ADR-0020 (`02-click-retention`, `f044cbe`): the pattern this slice follows.
- `missions/02-brownfield/slices/04-audit-columns/slice.yaml` (allocation, territory, schedule).
- `missions/02-brownfield/slices/01-audit-read/SPEC.md` (`7b753b7`, rule 3 row representation) and `design.md` (`4eb1eb4`).
- Shipped baseline on `main`: `V1__create_link_and_audit_log.sql`, `link/LinkRepository.java` (create, retire, key-release statements), `link/LinkService.java`, `audit/AuditLog.java` (insert-only, actor `anonymous`, `occurred_at` from the service clock); shipped tests inserting `link` rows with the v1 column list (`click/ClickSchemaTest`, `web/ObservabilityJourneyTest`).
- `docs/REQUIREMENTS.md`: FR-13, NFR-X2, NFR-A2, NFR-S6, NFR-P1.

## Intent visual

N/A: non-visual slice.

## Status

- 2026-10-03: requirements written: 11 acceptance criteria, 7 business rules, 7 ambiguity rows (6 assumed, 1 decided, none parked). The plan-lock waits for both w1 slices to merge (`slice.yaml`).
- 2026-10-03: requirements review finding (`review2-agent`, 18:56Z) fixed: AC-8 banned every audit column's name and value from responses. That contradicts the reused `link.created_at` (exposed as `createdAt`) and the audit read's `actor` (`anonymous`, equal to the new actor columns). AC-8 and rule 7 now forbid exposing the columns this slice adds and keep the shipped values exactly as before. The `log event` clause was dropped from AC-8, because it had the same contradiction. AC-10 still bans client values from the columns.

## Dependencies

- `01-audit-read` and `02-click-retention` (w1) merge first (`slice.yaml` `execution.depends_on`). `01-audit-read` adds the audit read whose representation AC-8 holds fixed. `02-click-retention` takes V3 and sets the ADR-0020 pattern.
- Mission 03 `01-analytics-v2` also takes a Flyway number. The later plan-lock takes the next free number (A-7).

## Self-check

- Every AC is observable: the schema (AC-1, AC-11), stored rows after HTTP requests on the suite clock (AC-2 to AC-7, AC-10), HTTP responses and the API document (AC-8), and the shipped suite (AC-9).
- Error and privacy paths are ACs: refused and failed writes stamp nothing (AC-5, including the rolled-back retire), canaries never reach an audit column (AC-10), and no response or log shows the columns (AC-8).
- Business rules cover the non-obvious logic: the existing `created_at` reused, one clock per link row, exactly three link writes (including the key release, which writes no audit row), the actor, append-only audit rows, expand only, the backfill.
- Out of scope is explicit, including exposure through the audit read, soft deletes and other actors.
- Every ambiguity is resolved: 6 assumed with reasons, 1 decided (by the locked `01-audit-read` contract), none parked.
- The proof contract names the per-suite coverage reports, the traceability rows, the `GAPS.md` row and the by-effect captures (upgraded schema and rows, the rollback run).
- No design leaked beyond the policy and shipped facts. Column names are the human's policy. Table and column names cite the shipped schema. The audit row's clock, the backfill's exact audit-row time, how the columns are kept current and the migration text are the design's.
- Consistent with the human's decision, ADR-0020, the locked `01-audit-read` SPEC, NFR-S6 and the slice's territory.
- Checked on `main` by me: `link` is written by exactly three statements (insert, the conditional retire update, the key-release update), and `audit_log` by one insert with the actor fixed at `anonymous`. Two shipped tests outside this territory insert `link` rows naming only v1 columns, which is the reason for AC-9's explicit clause.
- Not verified by me: whether `01-audit-read`'s merged read maps rows by column name or by a fixed list (the impact analysis must say, because a column-agnostic mapping could leak new columns into AC-8). And the upgrade cost on a large trail (the design measures it).
- `plan-review` lenses applied while drafting (not invoked separately). Engineering lens: AC-4 (key release as a write), AC-5 (refused and rolled-back writes), AC-9's out-of-territory inserts, and AC-11 (rollback run, not only read). Strategy lens: exposure through the audit read kept out (A-4). UX lens: there is no client-facing surface, and AC-8 holds every response fixed.
