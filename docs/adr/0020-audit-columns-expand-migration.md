# ADR-0020 — Audit columns: database-clock defaults, constant actors, one expand migration per table set

- Status: proposed by `02-click-retention` (2026-10-03); accepted at that slice's plan-lock
- Date: 2026-10-03
- Slice: `02-click-retention` (mission 02); the pattern for `04-audit-columns` (`link`, `audit_log`)

## Context

The human decided on 2026-10-03 (`qitem-20261003175330-fb054f2f`, relayed by the operator and routed by
the orchestration lead; SPEC `02-click-retention` A-10) that every table carries `created_at` and
`updated_at`, plus `created_by` and `updated_by` where an actor exists. An existing table gets them
in the next migration that touches it, as an expand migration with a written rollback.

`02-click-retention` touches `click` and `user_agent_class` (AC-16). Three constraints apply:
- v1's writer, `ClickStore.insert`, and a shipped unit test insert clicks with the v1 column list,
  and AC-14 forbids changing shipped tests for this;
- `docs/guidance/databases.md` §4 puts row times on the database clock and business times on the
  application `Clock`;
- NFR-P1 forbids client values outside the reduced columns.

## Decision

- **One expand migration per slice.** Here it is `V3__add_click_audit_columns.sql`: the next Flyway
  number after `01-audit-read`, which takes none. It adds four columns to each table and changes
  nothing else.
- **Columns.**
  - `created_at` and `updated_at`: `TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL`, the
    database clock.
  - `created_by` and `updated_by`: `VARCHAR(16) DEFAULT '<actor>' NOT NULL`, where the actor is
    `anonymous` for click rows (the unauthenticated Visitor, NFR-S6) and `system` for the seeded
    user-agent classes (SPEC A-11).
  - The defaults fill every new row, so **no writer changes**: the v1 insert keeps working, and a
    new row gets `created_at = updated_at`, because `CURRENT_TIMESTAMP` is one value within a
    statement (probe M3: 500 of 500; M2b on a connection left open after the migration).
- **Backfill.**
  - A pre-existing click gets `created_at = updated_at = clicked_at`, the closest known write
    time, since the writer inserts within milliseconds of it.
  - A pre-existing class row gets the migration's time, with `updated_at` set to `created_at`.
  - Every v1 column, value and constraint stays as it was (M2).
- **Rollback**, written in the migration header and run in probe M4: drop the eight columns, then
  delete the `version = '3'` row of `flyway_schema_history`. That restores the exact V2 schema, and
  V3 then re-applies cleanly.
- **Times are never business times.** `clicked_at` stays the click's time on the service clock, and
  the purge, statistics and day grouping keep using `clicked_on`/`clicked_at`. Audit columns are
  for operators reading rows, not for logic.
- **No client value.** The actor columns hold constants, never a hash, address, user agent, referrer
  or request id.

## Alternatives

| Alternative | Why not |
|---|---|
| Set the columns in `ClickStore.insert` from the application `Clock` | changes the shipped writer for no gain; mixes business and row time (`databases.md` §4); a test-shifted clock would write shifted row times |
| Backfill old clicks with the migration time | true for no row; `clicked_at` is within milliseconds of the real write time |
| A trigger to maintain `updated_at` | click and class rows are never updated (insert-only, then purged); H2 triggers are Java classes |
| One migration per table | two deployable states for one decision; the rollback would be split |

## Consequences

- **Upgrade cost.** The `ALTER`s and the backfill run inside Flyway before readiness: 42 to 43 s
  for 1 300 000 clicks (probe M5), once per data directory. A directory written since the shipped
  release (2026-10-03) holds days of clicks, so seconds at most. A large one should be upgraded
  while compose's health check allows 80 s.
- `CURRENT_TIMESTAMP` is stored with the session's offset (`-07:00` on the reference machine, `+00` in
  the container). Comparisons are by instant, and reports should render in UTC.
- `04-audit-columns` applies the same pattern to `link` and `audit_log`. There an actor may exist (the
  request's), and that slice decides whether `anonymous` stays a default.
- Verified before implementation:
  `missions/02-brownfield/slices/02-click-retention/design-probe/output-6.txt` (M1–M5), on H2 2.4.240
  through Flyway, with the exact `design-probe/migration/V3__add_click_audit_columns.sql`.
