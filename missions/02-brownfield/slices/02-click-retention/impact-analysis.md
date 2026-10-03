# Impact analysis — 02-click-retention

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design-agent@urlshort-factory`,
2026-10-03, for SPEC `69680e4` (requirements PASS on `ee7a4de`; RQ-01 fixed in passing at `96221e8`, the AC-4 wording at `69680e4`).

**Baseline.** `main` at `a74a2fe`, whose product code is byte-identical to `f6dd29e`, the SPEC's baseline.
`git diff --stat f6dd29e main -- src build.gradle.kts …` lists only `scripts/smoke.sh`. I built that
jar, ran it on an empty data directory, and created a link, redirected through it and read its
statistics. I then stopped it and read the database. Commands and output:
[`design-probe/baseline-f6dd29e.txt`](design-probe/baseline-f6dd29e.txt). The purge mechanics
were measured on H2 file databases of 1.3 million clicks:
[`design-probe/output.txt`](design-probe/output.txt) (one link),
[`design-probe/output-2.txt`](design-probe/output-2.txt) (2 000 links) and
[`design-probe/output-3.txt`](design-probe/output-3.txt) (a shutdown that outlasts the purge's
close deadline) and [`design-probe/output-4.txt`](design-probe/output-4.txt) (a process exit
in the middle of a run). The first attempt, whose batch form stalled, is
[`design-probe/output-first-attempt.txt`](design-probe/output-first-attempt.txt). The rows are
named in design §12.

## Change in one sentence

Click rows whose stored UTC day is older than the retention period are deleted. The period is
an operator setting with a 90-day default. The purge runs once at startup and once every UTC day. A
click lost because its reduction failed reports its own `click lost` reason. Requirements:
NFR-P2; FR-13; FR-15 in passing, through wave-review finding W2-05.

## Impacted modules

Found with `grep -rln -e ClickRecorder -e ClickStore -e "FROM click" -e "INTO click" -e DailySalt src`.

| Class or file | Change | Who calls it, and what it calls |
|---|---|---|
| `click.ClickPurge` | **new** package-private `@Component`: the startup run, a daily run at 00:10Z decided on the application `Clock`, one daemon thread | Spring calls it on `ApplicationReadyEvent` and at close; it calls `ClickStore` and reads `Clock` (`link.LinkConfig`'s bean) |
| `click.ClickRetentionProperties` | **new** `@ConfigurationProperties` record: `urlshort.click.retention-days`, default 90, positive | bound by Boot; read by `ClickPurge` |
| `click.ClickStore` | **one method added**: delete the clicks before a day | existing callers are unchanged: `ClickRecorder` (`insert`) and `StatsController` (`findLinkId`, `countByDayAndReferrer`); new caller `ClickPurge` |
| `click.ClickRecorder` | **one string**: a failure while reducing a click reports reason `reduction failed` instead of `rejected` (`ClickRecorder.java:93`) | called only by `link.RedirectController.java:46`, which is unchanged |
| `src/main/resources/application.properties` | **two settings** with their comments: `urlshort.click.retention-days=90`, and `urlshort.click.purge-enabled=true`, the operator hold (design review DR-01) | Boot; second holder after `01-audit-read` merges (ordered custody) |
| `src/functionalTest/resources/application-functional.properties` | **one line, granted** by the lead (slice.yaml `cec7032`): `urlshort.click.purge-enabled=false` | every functional context; the purge journeys override it (design §7) |
| `click.package-info` | the package description gains retention | — |
| `src/main/resources/db/migration/V3__add_click_audit_columns.sql` | **new** (AC-16, added by SPEC `32b1ae2` after design review; ADR-0020): audit columns on `click` and `user_agent_class`, with defaults and a backfill | Flyway at startup; every writer of `click` (`ClickStore.insert`) keeps its v1 column list, and the defaults fill the new columns |

Unchanged, read to confirm: `click.StatsController`, `click.LinkStats` (fold the remaining
rows), `click.DailySalt`, `click.Click`, `link.RedirectController`. **Not touched:** `link/`,
`audit/`, `web/`, `docs/api/openapi.json`. Also checked for AC-16: `grep -rn "INTO click"` finds
the v1 column list in `ClickStore.insert` and the unit `ClickSchemaTest`. No shipped test asserts
the exact column set of `click` or `user_agent_class`.

## Impacted endpoints

None. `GET /{code}`, `GET /api/links/{code}/stats` and the `/actuator/*` endpoints keep their
methods, paths, status codes, headers and bodies. Statistics keep their four fields. Their values
now cover only the retained window (AC-5), and a link whose clicks are all purged answers `200`
with `0` and empty arrays (AC-6). The baseline showed that this is already the shape of a link
with no clicks. No metric is added. The API document is not regenerated.

## Impacted schema and data

- **Rows.** The purge deletes `click` rows with `clicked_on < today − P`. `link`, `audit_log`,
  `user_agent_class` and `flyway_schema_history` are not touched: the statement names only
  `click`, and deleting a child never cascades to a parent.
- **No index.** The purge's statement scans the table. The probe measured the alternatives, and an
  index on `clicked_on` bought nothing (design §3, probe L1–L10):
  - Single `DELETE` of 1 000 000 of 1 300 000 rows: 7.9 s without the index, 9.5 s with it.
  - The same in 10 000-row batches: 43 s either way.
  - Creating the index on 1.3 million rows: 1.5 s.
- **One expand migration, V3: the audit columns** (AC-16, the human's decision A-10; ADR-0020).
  - `click` and `user_agent_class` each gain `created_at` and `updated_at` (database clock,
    defaults) and `created_by` and `updated_by` (`anonymous` / `system`, defaults).
  - Every v1 column, value and constraint is unchanged (probe M2).
  - Pre-existing clicks get `created_at = updated_at = clicked_at`, and classes the migration's
    time.
  - New rows are filled by the defaults, so the v1 insert is unchanged (M2b, M3).
  - V3 is the next Flyway number after `01-audit-read`, which takes none.
  - It costs 42 to 43 s inside Flyway for 1 300 000 clicks, once, before readiness (M5).
- **Volume.** In steady state a run deletes one day of clicks, about 1/91 of the table at the
  default. A larger run happens only when the period is lowered (90 → 7 deletes about 92 % at the
  next run) or after a long outage.
- **Expand → migrate → contract.** V3 is the expand step only: columns added, nothing dropped or
  retyped. No later contract step is planned.
- **Rollback.** `git revert` of the merge commit restores v1's behaviour. The V3 header's rollback
  statements, run on a stopped copy of the data directory, restore the V2 schema exactly (probe
  M4). Deleted rows cannot be recovered; deletion is the requirement. To keep an undo before lowering
  the period, the Operator copies `data/` while the service is stopped (`docs/guidance/databases.md`
  §5). `docs/DESIGN.md` §3 says so next to the setting.

## Impacted data flows

| Flow | Before | After |
|---|---|---|
| Redirect → click recording | request thread reduces, `click-writer` inserts | unchanged; a click inserted while a purge deletes is committed independently. MVCC: probe L1–L10, redirect p95 ≤ 2.5 ms during deletion of 1 000 000 rows, every redirect's click stored |
| Statistics | one grouped query over the link's rows | unchanged query; fewer rows after a purge |
| Audit | one row per link mutation | unchanged; a purge writes no audit row (SPEC A-4) |
| Startup | Flyway, then readiness | Flyway, then **the startup run on the `click-purge` thread, finished before Boot reports readiness**, then readiness |
| Daily | — | every 5 s the purge thread reads the application clock; at the first tick on or after 00:10Z of a UTC day later than the last run's day, it runs once |
| Shutdown | graceful phase, click drain ≤ 5 s, pool close | unchanged order; at close the purge thread takes no new run, and a running statement is awaited for up to 3 s and never interrupted. A statement still running when the process exits is undone by H2 when the file next opens (design §1, probe D2, D3, D4) |

The sequence delta is design §4 (`docs/diagrams/purge-sequence.mmd`).

## Blast radius

| If this is wrong | Worst case | Detection |
|---|---|---|
| The cutoff is wrong (an off-by-one, the wrong zone, the period read wrongly) | clicks deleted before their 90 days, irreversibly | boundary tests AC-1 to AC-3 on clicks recorded through real redirects at shifted days; the INFO event names `cutoff` and `retentionDays` on every run |
| The host clock jumps forward | one run deletes up to the jump's worth of days early; a clock years ahead deletes every click | INFO `cutoff`; accepted residual (design §6): the Operator owns the host clock, and a backward step deletes nothing |
| The period is set too low by mistake | the next run, including the startup run, deletes everything older | the setting is validated (positive whole days, AC-4); `docs/DESIGN.md` states the effect and the backup step |
| The purge fails every day | clicks kept longer than decided | one WARN `click purge failed` per failed run (AC-10) |
| The purge slows the Visitor | redirect latency | `http.server.requests` on `/actuator/prometheus`; probe evidence; AC-11 |
| The startup run is slow | readiness later by the run's length (about 8 s per million deleted rows) | readiness probe; the INFO event's timestamp |
| A shipped test is disturbed by a background run | a flaky functional suite (FR-13, AC-14) | the full suite on the candidate; the analysis below |

## Compatibility (FR-13)

- **Links and codes.** No link row is read, written or deleted by the purge.
- **Stored rows.** Clicks within the period are unchanged. Clicks beyond it are deleted by the
  first startup run on an upgraded directory (AC-13). The baseline service writes only
  `clicked_on = today`, so a real `f6dd29e` directory holds no click beyond the period. AC-13's
  directory gets its old clicks by backdated inserts after the shipped jar has written links,
  audit rows and today's clicks (design §7).
- **Clients.** The statistics shape is unchanged.
- **Operators.** One new optional setting with the decided default; an existing deployment needs no change.
- **Proof.** The functional suite as it stood at `f6dd29e` passes unchanged (AC-14). The test
  impact below explains why each shipped test that stores clicks or captures logs is unaffected.

## Test impact

**Added.**
- Functional classes for the purge journeys, each on its own in-memory database and context, so
  no other journey moves their clock and their deletions never reach the shared database. They
  cover AC-1 to AC-13 and are listed in design §7.
- Unit tests: `ClickPurgeTest` (the tick decision, the events, close) and the AC-12 case.

**Changed, one unit test, by intent.**
`ClickRecorderTest.aClickThatCannotBeReducedIsOneWarn` asserts reason `rejected` for a reduction
failure. W2-05 and AC-12 change that reason, so the assertion becomes `reduction failed`. The
stub, the request and the other assertions stay as they are.

**Functional suite: test files unchanged (AC-14); its profile overlay gains one granted line.**
The overlay sets `urlshort.click.purge-enabled=false`, so **no shared functional context runs a
purge**: no startup run, no daily tick. Only the purge journeys turn it on, in their own contexts
and databases, closed after use (design §7). A purge line therefore cannot appear while a
shipped journey runs. The table covers every shipped test that stores clicks, captures the log or
moves the clock, and why it is unaffected.

| Shipped test | What it does | Why the purge does not disturb it |
|---|---|---|
| `ClickRecordingJourneyTest` | records clicks with the clock set to 2026-09-01 and 2026-09-02, asserts them in the same test | the clock is set into the past, so no run is due during the test; a run at another context's start happens between classes. After 2026-11-30 those dates are beyond the period, still only between classes |
| `StatsJourneyTest` | records on 2026-10-01, -02 and -04 and reads statistics in the same test | when run before 2026-10-04 the shift to 2026-10-04T12:00Z makes a run due: its cutoff is 2026-07-06, older than every row of the test. Later the shift is into the past and nothing runs |
| `ClickResilienceJourneyTest` | `@MockitoSpyBean ClickStore`, stubs `insert` only, no `verify` on the store | the purge's call to the spy goes to the real method; nothing counts store interactions |
| `ClickSchemaJourneyTest` | reads the rows it just wrote | its rows are today's |
| `IdempotencyJourneyTest` | shifts the shared clock by +23 h, +24 h ± 1 s, sends creates | a run may become due during the +24 h shift (cutoff `T+1−90`); no shipped test holds clicks that old across it |
| `ColdStartJourneyTest`, `ObservabilityJourneyTest`, `PingJourneyTest` | every line captured while a request runs must carry that request's id | no purge runs in their contexts at all (the hold); before design review DR-01 this rested on timing, now it does not |
| `RateLimitJourneyTest`, `TrustedProxyJourneyTest`, `RateLimitSettingsJourneyTest` | freeze the clock and step it by seconds | never a UTC day forward; nothing becomes due |

**No residual.** The first version accepted a run at a real 00:10Z inside a capture window as
unlikely. Design review DR-01 reproduced it, and AC-14 does not allow it. The hold in the overlay
removes the case.

**Removed.** None.

## Observability impact

- New events on the `click-purge` thread, without `requestId` (SPEC rule 6, not a request):
  - INFO `clicks purged` with `deleted`, `cutoff` (the earliest UTC day kept) and `retentionDays`;
  - WARN `click purge failed` with `cutoff`, `retentionDays` and `errorType`;
  - WARN `click purge paused, no click is deleted` with `setting` (`urlshort.click.purge-enabled`)
    and `retentionDays`, at every start while the hold is on (AC-15).

  No message or stack trace, and no click value, link code or id (AC-9, AC-10).
- New `click lost` reason `reduction failed` (AC-12). `rejected` keeps its meaning: a full or
  closed queue.
- New startup failure: an invalid period stops the service with Boot's failure analysis. It names
  the setting, the rejected value and its origin, and nothing else (probe A4; design §2).
- No metric, health or dashboard change. There is no runbook file in the repository. The
  operator-facing facts go into `docs/DESIGN.md` §3, which is mine: the setting, the run time, the
  events and the backup step. `README.md` lists every operator setting and is outside this
  slice's territory, so design §9 asks the lead for a one-line grant.

## Risks and mitigations, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | Clicks deleted early, irreversibly | cutoff = the application clock's UTC day minus P, delete `<` it (A-1); boundary ACs through real redirects at shifted days; the statement names only `click`; the setting validated at startup | design → QA (AC-1 to AC-4) → security review |
| 2 | A shipped test made flaky by background runs | no purge in any shared functional context (the hold in the overlay, DR-01); the purge journeys on their own databases, closed after use | design → QA (full suite, twice) |
| 3 | The purge slows redirects or click writes | MVCC measured: no stall in any of ten variants; AC-11 in the suite | design probe → QA → release bench (NFR-L1) |
| 4 | A run in progress at shutdown delays the stop or damages the file | measured: the running statement is awaited for up to 3 s and never interrupted (D2, D3). Clicks queued before the stop are written (D3, 20 of 20). A process exit in the middle of a 1 000 000-row DELETE leaves a consistent file with the delete undone, so the next startup run repeats it (D4; reopening took 4 s). A long run can meet a stop only in a catch-up, and a catch-up happens in the startup run | design probe → release (`--drain`) |
| 5 | Ordered custody with `01-audit-read` (`application.properties`) | the candidate descends from `01-audit-read`'s merge; this slice takes no Flyway number | integrate (ancestry check) |
| 6 | Mission 03 changes retention | **checked**: the human chose Q4 A at 16:10:30Z (transition 876 on `qitem-20261003154347-19e96a75`, "accept all recommended: … Q4 A …"), the 90-day delete as built here; the lead re-checks at plan-lock | lead (plan-lock) |

## Self-check

- Baseline observed by effect on the shipped jar, not from memory: status codes, headers, rows,
  indexes, plan and log events are in `baseline-f6dd29e.txt`.
- Every class the change touches, and every class that calls or reads one of them, is in the
  module table. The `grep` that produced the list is quoted.
- Endpoints: none changed, checked against the baseline responses.
- Schema: no index, measured (probe L0 to L10). One expand migration for AC-16, measured on upgrade,
  insert compatibility, rollback and cost (M1 to M5).
- Data flows: each existing flow is checked, and the two new flows (startup, daily) are named.
- Blast radius: each failure has a worst case and a detector.
- Compatibility: every shipped test that stores clicks, captures logs or moves the clock is listed,
  with the reason it passes unchanged. The one changed unit test is changed by intent.
- Observability: every new event, field and failure text is named. The README grant is requested,
  not assumed.
- Not verified here: the full shipped functional suite against a candidate. No candidate exists
  yet, so that is AC-14, QA's.
