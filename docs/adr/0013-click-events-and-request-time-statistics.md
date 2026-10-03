# ADR-0013 — Clicks are stored as reduced event rows; statistics are computed per request from one grouped query

- Status: proposed (becomes accepted at the `02-analytics` plan-lock)
- Date: 2026-10-03
- Slice: `02-analytics`

## Context

FR-8 asks for total clicks, clicks per UTC day and the top 10 referrer
origins per link (rule 7, with a code-point tie order). AC-11 requires the
three figures to agree with each other. NFR-P2 (built in mission 02)
will purge clicks older than 90 days, and FR-16 (mission 03) may count
unique visitors. `databases.md` §3 asks each design to say whether analytics
come from event rows at request time or from a summary table.

## Decision

- **Table `click`** (`V2__create_click.sql`): `id` identity; `link_id`
  `NOT NULL` with `FOREIGN KEY → link(id) ON DELETE CASCADE`;
  `clicked_at TIMESTAMP WITH TIME ZONE`; `clicked_on DATE`; `referrer
  VARCHAR(2048)` (origin or `NULL`); `user_agent_class VARCHAR(16)`
  referencing the four-row lookup table `user_agent_class (token)`;
  `client_hash VARCHAR(64)` with a `LENGTH` check; index
  `ix_click_link_day (link_id, clicked_on)`. There is no column for any raw
  value and none for the request id.
- **The closed set is a lookup table, not `CHECK (… IN …)`.** On H2 2.4.240
  a `CHECK` built from a multi-value condition (an `IN` list, or equalities
  joined by `OR`) answers "The database has been closed" (`90098`) for every
  insert once the pooled connection that created it has been retired.
  Hikari's `maxLifetime` does that after 30 minutes. Single comparisons,
  `LENGTH` checks and foreign keys keep working, and so do V1's two shipped
  checks (design review DR-04; constraint probe, memory and file
  databases). Later migrations follow the same rule (`docs/DESIGN.md` §4).
- **The UTC day is computed in Java and stored.** `clicked_on =
  LocalDate.ofInstant(clicked_at, UTC)` is written alongside the instant.
  A SQL day over `clicked_at` depends on the parameter binding and the
  session zone: with a `Timestamp` binding, H2 stored and returned
  `-07:00` values on the reference machine. The two engines also spell a
  UTC date differently. So grouping by a SQL expression would not be exact
  or portable. The day is derived
  data, not a new fact.
- **Statistics come from event rows at request time, from one statement.**
  `SELECT clicked_on, referrer, COUNT(*) FROM click WHERE link_id = ? GROUP
  BY clicked_on, referrer`. Java then folds the rows into the total, the
  per-day list (ascending) and the top 10 origins (`clicks` descending, then
  `String.compareTo`). One statement sees one consistent set of rows, so
  AC-11 holds while clicks are being written. Sorting in Java makes the tie
  order independent of database collation.
- **The code is resolved by the feature itself.** `SELECT id FROM link
  WHERE code = ?`. `link/`'s types are package-private, and the click table
  already references `link.id`.

## Consequences

- A statistics read costs one indexed range scan and returns one row per
  (day, origin) pair of the link. Referrer spam can inflate that;
  `03-operate`'s rate limit and mission 02's purge bound it.
- No summary table, so no second write per click. If reads ever dominate,
  a summary maintained by the writer thread is the upgrade, and the rows
  are still there to rebuild it.
- NFR-P2's purge is `DELETE FROM click WHERE clicked_on < ?`; it needs at
  most an index on `clicked_on`, and no schema change. FR-16 can count
  distinct `client_hash` per day from the same rows.
- Moving to PostgreSQL needs no change here: the DDL is portable, no
  database time function is used and no collation is relied on.
- Rollback: `DROP TABLE click; DROP TABLE user_agent_class;` removes click
  history only.
- Verified before implementation:
  `missions/01-greenfield-core/slices/02-analytics/design-probe/output.txt`
  (C0 DDL, C3 the session-zone instant under a `Timestamp` binding, C5 the
  fold, C6 H2's ordering for comparison) and
  `…/design-probe/constraint-output.txt` (every constraint form before and
  after connection retirement, including the revised V2).
