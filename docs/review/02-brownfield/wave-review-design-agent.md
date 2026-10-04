# Mission 02 wave review — structure and drift (design agent, second vantage), final at `d55a502`

**Verdict from this structural vantage: the merged code is coherent with its designs and the
register. I found no MUST-FIX and no HIGH.** The step is not clean, though: the primary vantage
found a HIGH test race at `d55a502` in `02-click-retention`'s AC-8 journey. Its design-level cause
is mine (W2F-02), and the lead has routed a test-only forward fix. Mine besides:
- one new LOW, W2F-01: the V4 tests migrate to latest;
- one carried LOW, W2P-01: the README purge-hold variable.

Everything else is resolved or recorded below.

The file began as the PRE-REVIEW of `8e9c065..ed2b940`. Those sections follow unchanged. The
**Final wave** section at the end adds `ed2b940..d55a502` and the 13-row register walk at `d55a502`.

---

## PRE-REVIEW of the merged range at `ed2b940` (draft; wave_review adds 04-audit-columns)

Mission 02 wave review, structure and drift: the design agent's second vantage. The review agent
holds the primary vantage. Asked by the orchestration lead, `qitem-20261003231759-0606a747`, as a
plain queue item ahead of the workflow step, because the human asked for more in parallel (operator
`qitem-20261003231707-8c00c321`). Author: `design-agent@urlshort-factory`, 2026-10-03.

**Range.** `8e9c065..ed2b940`. Four slices are merged:
- `05-ci-cd` `0aa3695`;
- `01-audit-read` `cb148c4`;
- `03-dogfood-fix` `5c264db`;
- `02-click-retention` `ed2b940`.

Also in the range: the Gradle 9.8.0 wrapper (`f3e6b0b`, renormalised by `9bbf6e5`), and mission
01's release commits to `scripts/smoke.sh` (`34308c9`, `3ec7ab4`, `973bc1a`). `04-audit-columns`
is not merged and is out of this draft.

**Where my own design is the subject.** I designed `01-audit-read`, `02-click-retention` and
`03-dogfood-fix`, so three of the four merges are my designs. `05-ci-cd` was designed by
`design2-agent`. Findings against my designs are marked **(mine)** and are not softened. An
independent reader should weigh this vantage accordingly: the review agent's primary vantage is
the independent one.

## What I inspected

| Area | Read |
|---|---|
| Product, merged | `git diff --stat 8e9c065 ed2b940` over product files (41 files). Full reads at `ed2b940` of: `audit/AuditController`, `audit/AuditTrail`, `click/ClickPurge`, `click/ClickRetentionProperties`, `web/MetricsConfig`, `V3__add_click_audit_columns.sql`, `docs/api/openapi.json`, `.github/workflows/ci.yml` and `cd.yml`, `AuditUpgradeJourneyTest`, and `AuditForwardedHeadersJourneyTest.start`. Diffs of `ClickRecorder`, `ClickStore`, `OpenApiConfig`, `application.properties`, `README.md` and `OpenApiDocumentTest`. `HealthMetricsJourneyTest` as merged |
| Designs and decisions | the three designs of mine with their review responses (including `01-audit-read`'s `0052efb` correction); ADR-0010, 0011, 0013, 0015, 0016, 0018, 0019 and 0020; `docs/DESIGN.md`; the register (`docs/guidance/architecture.md` §11) |
| Gates | the integrator's fresh `check` on each merge: `docs/evidence/02-brownfield/integrate-*-check-*.txt`, all four `BUILD SUCCESSFUL`, 14 of 14 tasks executed. I ran no build myself |
| Gaps | `docs/qa/GAPS.md`: the audit-column rows, QA-OPR-02, and the `02-click-retention` QA section |
| Mission 01 forward items | `docs/review/01-greenfield-core/wave-2-review-design-agent.md` (W2D-01 to W2D-07) |

## Structure and coherence

| Check | Result |
|---|---|
| Package boundaries | `audit/` gains its only reader (`AuditController`, `AuditTrail`, `AuditEntry`, `AuditPage`). `AuditLog` stays the only writer, and `AuditTrail` names its columns, so V4 can add columns without changing a response. `click/` gains `ClickPurge`, `ClickRetentionProperties` and `ClickStore.deleteBefore`. `web/` gains `MetricsConfig` and a second `OpenApiConfig` customiser. Features still depend on `web.Problems`, never the reverse (no `web/` file names a feature package: grep). `audit/` now depends on Boot's `ServerProperties` and `TomcatServerProperties`, by design (ADR-0019, P7) |
| Filter chain and budgets | unchanged. `/api/audit` is charged to the create budget like every `/api` path; `/actuator/**` and the API document stay exempt |
| One problem shape | the audit read's `400` and `403` come from `Problems.validation` and `ErrorResponseException`, through the one advice. The document now describes the one shape. `OpenApiDocumentTest.AC2_…` checks six real bodies against it, the audit read's `400` and the limiter's `429` included |
| One clock | `ClickPurge` reads the application `Clock` on a 5 s tick. V3's row-write timestamps use the database clock, as `databases.md` §4 and ADR-0020 say. `PingController`'s `Instant.now()` is carried drift (register) |
| Background work and shutdown | two owned single-thread executors (`click-writer`, `click-purge`); no `@Async` or `@Scheduled` (grep). On paper, the worst-case shutdown is the 10 s graceful phase, then up to 5 s of click drain, then up to 3 s of purge close: 18 s inside compose's 20 s `stop_grace_period`. The margin fell from 5 s to 2 s (W2P-04) |
| Readiness | the startup purge is awaited on `ApplicationReadyEvent`, so readiness waits for it (ADR-0018); `05-ci-cd`'s jar smoke on an empty database is unaffected |
| Schema | V1 to V3. V3 is byte-identical to the designed file (`diff` with `missions/02-brownfield/slices/02-click-retention/design-probe/migration/V3__add_click_audit_columns.sql`: empty) |
| Settings | the shipped file gains the forwarded-header pin (commented as part of ADR-0019, not an operator knob), `urlshort.click.retention-days=90` and `urlshort.click.purge-enabled=true`, each with an operator comment |
| API document | `/api/audit` with its `200`, `400`, `403`, `429` and `500`; `ProblemDetail` with optional `errors` of `ProblemFieldError` and no `properties`. Committed equals live by `OpenApiDocumentTest`, green in each integrate gate. Custody was respected: `01-audit-read` regenerated first, `03-dogfood-fix` on top, and both are present |
| CI/CD | `ci.yml`: `gate` on every `pull_request` with no filter, `main` and on demand. `cd.yml`: `package` on `main` and on demand, a jar smoke on `127.0.0.1`, `docker build` with no push. Both have `contents: read`, SHA-pinned actions, `timeout-minutes: 20`, cancelling concurrency, explicit `bash`, `persist-credentials: false` and `cache-provider: basic`. `${{ }}` appears only in `concurrency.group` (both) and in `cd.yml`'s step `env:`, never in a `run:` block. This matches `docs/DESIGN.md` §3's CI/CD row |

## Drift: SPEC → design → merged

| Slice | Designer | Result |
|---|---|---|
| `05-ci-cd` | `design2-agent` | No drift found at this vantage (CI/CD row above). The `--jar` smoke mode that `cd.yml` relies on came from mission 01's release commits on `main`, outside a slice (W2P-07) |
| `01-audit-read` | **mine** | Code matches design §1 as corrected at `0052efb`. The guard is strategy `NONE` and both `remoteip` headers unset. The keyset `SELECT` is verbatim, the cursor is base64url of the `id`, the `200` is preset to `application/json`, and the pin is in the shipped file. The builder added `@ConditionalOnWebApplication(SERVLET)`, which the design did not name: outside a servlet application the endpoint does not exist, which fails closed. `README.md` gained the endpoint line under its grant |
| `03-dogfood-fix` | **mine** | Code matches design §1. The builder put the `ProblemFieldError` schema in a local variable, avoiding the unchecked call the design noted. `MetricsConfig` is the one-line `ignoreTags("path")`. Tests match §7: the nested `429` context, the nested `@AutoConfigureMetrics` scrape, AC-2 including the audit read's `400`, and `MetricsConfigTest`. `GAPS.md` QA-OPR-02 is closed |
| `02-click-retention` | **mine** | Code matches the design and its four review answers: the hold with its WARN, `runNow()` for the suite, the `reduction failed` reason (W2D-05), one `DELETE` per run, and V3 verbatim. **Drift (mine):** `README.md` names `URLSHORT_CLICK_RETENTIONDAYS` but not `URLSHORT_CLICK_PURGEENABLED`. My README grant request predates the DR-01 rework that added the hold, and I did not widen it (W2P-01) |

**Mission 01 forward items closed in this range:**
- W2D-01, the problem schema: closed by `03-dogfood-fix`.
- W2D-04, the disk path: closed by `03-dogfood-fix`.
- W2D-05, the reduction reason: closed by `02-click-retention`.
- W2D-03, the two notions of client: still open on `main`; closes with `01-analytics-v2`, pending merge.
- W2D-02, status lines stale after merges: **recurred** (W2P-03).

## Findings

| Id | Severity | Evidence | Finding | Disposition / repair |
|---|---|---|---|---|
| W2P-01 | LOW | `README.md` at `ed2b940` (settings sentence); `application.properties` (`urlshort.click.purge-enabled`); `02-click-retention` design §9, *Grant request for the lead at plan-lock* | **(mine)** The README lists every operator setting's environment variable except the purge hold, `URLSHORT_CLICK_PURGEENABLED`. An Operator reading the README cannot find the incident and legal-hold switch. It is documented only in `application.properties`, `DESIGN.md` §3 and ADR-0018 | One line in `README.md`'s settings sentence. Route: lead backlog, under a README grant to the next slice that holds `README.md`, or in passing by the lead. The register's *Operator settings* row names it as drift |
| W2P-02 | LOW | `docs/qa/GAPS.md` lines 82–83 | The `click @8e9c065` and `user_agent_class @8e9c065` audit-column rows still read "OPEN until 02-click-retention merges", after that merge at `ed2b940`. V3 added and backfilled all four columns on both tables (`ClickAuditColumnsTest`; QA2's section at line 235) | QA owns `GAPS.md`: close both rows with the merge SHA and V3's evidence. Route: QA, through the lead. No product change |
| W2P-03 | MEDIUM | `docs/DESIGN.md` preamble and 28 markers; `diagrams/container.mmd`, `erd.mmd`; ADR-0010, 0011, 0013, 0015, 0016, 0018, 0019 and 0020 status lines; DESIGN.md §7 | **(mine, recurrence of W2D-02)** After four merges, the system design still marked all four slices as designed. The ADR index listed 0018 to 0020 as "proposed". Merged or plan-locked amendments still read "proposed". ADR-0019's index row omitted the `remoteip` closure | **Fixed in this step**, commit `4975d25`. **Process repair, so it stops recurring:** at integrate, the lead files a plain queue item to `design-agent` ("<slice> merged at <sha>"), and I refresh the markers and status lines then. My design-time step cannot see a merge, which is why this recurred. The lead decides |
| W2P-04 | INFO | `application.properties` (`timeout-per-shutdown-phase=10s`); `ClickRecorder.DRAIN_DEADLINE` 5 s; `ClickPurge.CLOSE_DEADLINE` 3 s; `compose.yaml` `stop_grace_period: 20s` (ADR-0017) | Worst-case shutdown on paper: 18 s inside a 20 s grace; mission 01's budget was 15 s. The purge close waits only if a run is in progress, so the common case is unchanged | Nothing to change now; `release_prep` measures shutdown on the merged candidate (`scripts/smoke.sh --drain`), with a purge run in flight if it can arrange one |
| W2P-05 | INFO | `AuditUpgradeJourneyTest` (clicks dated 2026-10-01, started through `SpringApplicationBuilder` on the functional clock, which tracks real time); `application-functional.properties` (`urlshort.click.purge-enabled=false`) | A cross-slice dependency. The audit read's upgrade journey seeds clicks with a fixed date and asserts the exact statistics. After 2026-12-30 those clicks are older than the 90-day period. The test stays green only because the functional overlay holds the purge in every context it starts. If the hold were lifted from the overlay, this `audit/` test would fail on that date | Record only. The overlay comment names the log-window reason; the next edit to that comment can add this one. A dated fixture in a test that starts the application should pass `--urlshort.click.purge-enabled=false` itself, or seed relative to the clock |
| W2P-06 | INFO | `AuditUpgradeJourneyTest` (exact v1 statistics body); `35e5951` (the `01-analytics-v2` grant) | An `audit/` test pins `click/`'s statistics contract exactly, as `01-audit-read` AC-18 ("statistics unchanged") asks. `01-analytics-v2` needed a grant to change it | Expected by the AC; noted so wave review can see the coupling. A later upgrade test can compare against the shipped jar's own response rather than a literal |
| W2P-07 | INFO | `git log 8e9c065..ed2b940 -- scripts/smoke.sh` | `scripts/smoke.sh`'s `--jar` mode, which `cd.yml` runs on every `main` push, came from mission 01's release commits on `main`, not from a slice, and was reviewed in mission 01's release review. Mission 02's CD therefore depends on a script no mission 02 slice owns | Record only. The next slice that changes `smoke.sh` runs the CD job's command locally before handoff |
| W2P-08 | INFO | `audit/AuditController` (`TomcatServerProperties`, `ponytail:` comment); ADR-0019 *Consequences* | The guard mirrors Boot 4.1.1's three valve triggers. A Boot upgrade that adds a trigger would reopen the read, and no test would notice, because the real-Tomcat journeys cover the known three | Already a named ceiling in ADR-0019 and the code. Add to the Boot-upgrade checklist when one exists: re-read `TomcatWebServerFactoryCustomizer.customizeRemoteIpValve` |

**Added after the draft:** a finding from the review agent's vantage against my wording.

| Id | Severity | Evidence | Finding | Disposition / repair |
|---|---|---|---|---|
| W2P-09 | MEDIUM | `ClickRecorder.close` (`shutdownNow()` after a failed `awaitTermination`); `ClickPurge.close` (waits only); ADR-0011 lines 57–61 | **(mine, found by `review-agent`, 23:30Z)** My register row *Background work* (`5f90090`), `DESIGN.md`'s *Asynchronous work* row, ADR-0011's `02-click-retention` amendment and ADR-0018's shutdown consequence all said neither background job interrupts a JDBC call. That is true only for the purge. The writer interrupts its thread after the 5 s drain, and reports a running insert as `shutdown deadline, outcome unknown` | **Fixed:** register and `DESIGN.md` at `2d3de57`; both ADRs at `bfc642d`, with the lead's OK, as wording only. No design or behaviour change |

**Carried drift on `main`, not new:**
- `ClickRecorder` hashes the raw peer (W2D-03), which `01-analytics-v2` closes;
- `PingController.Instant.now()`, on the lead's backlog as `qitem-20261003213157-c0a336f9`.

## Register walk (D20), one line per concern, range `8e9c065..ed2b940`

Register as updated in commit `5f90090`.

| Concern | Line |
|---|---|
| Client identity and proxy trust | **Consistent, with carried drift.** The audit read admits only while nothing can rewrite the peer, and the pin is shipped. The limiter's `clientOf` is unchanged. Drift: the click hash still uses the raw peer (W2D-03), which `01-analytics-v2` closes |
| Time | **Consistent, with carried drift.** `ClickPurge` is on the application `Clock` and polls it. V3's row-write defaults use the database clock by policy. Drift: `PingController` (backlog) |
| Schema change | **Consistent.** V3 is expand-only with its rollback in the header, the number assigned at plan-lock (ordered custody). The register gained the pin-the-target rule after two V3 tests broke on V4 (`b115e7b`) |
| Audit columns | **Consistent in code; drift in the gaps list.** `click` and `user_agent_class` have all four columns by default; `link` and `audit_log` wait for V4. `GAPS.md` still lists the first two as open (W2P-02) |
| Error shape | **Consistent.** The audit read uses the one path. The document now describes the one shape, and six real bodies are checked against it: from three controllers (`LinkController`, `RedirectController`, `AuditController`) and the limiter |
| Request id and logging | **Consistent.** The purge's events carry no request id and log exception classes only. Shared test contexts hold the purge, so log-window journeys stay clean |
| Audit trail writes | **Consistent.** `AuditLog` is the only writer; `AuditTrail` is the only reader, with one `SELECT` that names its columns. The purge writes no audit row, because clicks are not mutations |
| Client hashing for analytics | **Consistent, with carried drift.** `DailySalt` is unchanged; its input address is the drift above |
| Metrics and health exposure | **Consistent.** No meter carries a filesystem path (`MetricsConfig`); exposure is unchanged |
| API document | **Consistent.** Ordered custody held (audit-read, then dogfood-fix); committed equals live in every integrate gate |
| CI/CD | **Consistent.** Both workflows keep `ci-cd.md` §3; nothing is published |
| Background work (new row) | **Consistent.** Two owned executors, with bounded close deadlines that never interrupt JDBC; no scheduler framework. The shutdown sum is W2P-04 |
| Operator settings (new row) | **Drift (mine).** Two new settings with defaults, a constraint and operator comments, but the README omits `URLSHORT_CLICK_PURGEENABLED` (W2P-01) |

## `01-analytics-v2` register lines (pending merge; locked design `80ca44c`, candidate `ec466da` in QA)

Drafted from the locked design; they are in the register marked *(pending merge)*. Once it merges, I
check them against the merged code and drop the marker.

- **Client identity.** `RateLimitFilter` becomes `public`, with
  `public static final String CLIENT_ATTRIBUTE`. Each non-exempt request carries `clientOf`'s
  result in that attribute before it is charged. `ClickRecorder` hashes the attribute and falls back
  to `getRemoteAddr()` only when it is absent; that path is reached by unit tests. `clientOf` stays
  package-private, and `getRemoteAddr()` is never rewritten, so the audit read's guard is unaffected
  (ADR-0015 amendment). This closes W2D-03.
- **Time.** Unique visitors are counted only within `GROUP BY clicked_on`, and `clicked_on` is the
  UTC day of the salt that produced the hash, because `DailySalt.stamp` picks the instant and the
  key together (ADR-0012, ADR-0013 amendment). No figure combines days. A restart within a day draws
  a new salt, so the figure is an upper bound.
- **Error shape.** No new error path. The statistics endpoint keeps v1's `404` problem; the new
  per-day fields appear only in the `200`.
- **Metrics** (also settled): `urlshort.clicks.recorded` and `urlshort.clicks.lost{reason}`, with
  every reason from a static vocabulary registered at zero (ADR-0016 amendment).

## Not verified by this vantage

- No build, test or HTTP run of my own on `ed2b940`. I relied on the integrator's four gate logs
  and on QA's by-effect captures for the slices.
- The shutdown sum (W2P-04) is arithmetic on configuration, not a measurement.
- The GitHub-hosted CI and CD runs: not observed by me; `05-ci-cd`'s AC-13 record holds them.
- `04-audit-columns` (not merged) and `01-analytics-v2` beyond its locked design.

## Self-check

- Every merged slice in the range was compared with its design and, where relevant, its SPEC. The
  three that are my designs are marked, with the drift found in them (W2P-01, W2P-03) stated as
  mine.
- Each register concern has one line for this range, the two new rows included. The register and
  `DESIGN.md` edits are committed separately (`5f90090`, `4975d25`) before this file.
- Each finding names its evidence, a severity and an owner. Nothing here is a product change I
  made; the two documentation repairs are mine, and are done.

---

## Final wave: `ed2b940..d55a502`, and the accumulated range `8e9c065..d55a502`

Asked by the lead (`qitem-20261004002548-140c0e49`) and by the step owner, review-agent
(`qitem-20261004002621-3d2dc808`, parent `qitem-20261004002455-b7ea811b`). Written 2026-10-04.

**Where my own design is the subject.** I designed `04-audit-columns`, the only product slice in
this delta, as well as `01-audit-read`, `02-click-retention` and `03-dogfood-fix` earlier in the
range. `05-ci-cd` is `design2-agent`'s. What follows compares merged code with my own designs, so it
is a consistency check, not an independent judgment. The independent evidence is QA's (`qa2-agent`
on `305f804`), the code and security review's (`review2-agent`), and the review agent's own vantage
and final offline gate on the merged range. Mission 03 (`01-analytics-v2`) is outside this range.
Its structural vantage went to `design2-agent` (lead, 00:27Z), because I designed that slice too.

### What I inspected in the delta

| Area | Read |
|---|---|
| Product | `git diff --stat ed2b940 d55a502` over product files (10 files). The full diff of `LinkRepository`, `LinkService`, `ClickAuditColumnsTest` and `ClickRetentionStartupJourneyTest`. `diff` of the merged `V4__add_link_audit_columns.sql` with the designed file. `LinkAuditColumnsTest` (cases and its `Database.migrate`) and `LinkUpgradeJourneyTest` in full. A grep for any `UPDATE` or `DELETE` of `audit_log` in `src/main` |
| Integration | `d55a502`'s parents (`d7459ff`, second parent `305f804`: a `--no-ff` merge) and its message; the grant `132a884` |
| Documents | `docs/qa/GAPS.md` rows 82–85 after `17593aa` and `382a7b2`; my `04-audit-columns` design §1, §3 and §7; ADR-0020 and its amendment |

### `04-audit-columns` against its locked design and ADR-0020

| Check | Result |
|---|---|
| Migration | V4 is byte-identical to the designed `design-probe/migration/V4__add_link_audit_columns.sql` (`diff`: empty): expand only, with the eight-statement rollback in its header |
| Link stamping | `LinkRepository.retire` stamps `updated_at = :at, updated_by = 'anonymous'` in its own conditional `UPDATE`. The new `stamp(id, at)` runs after a key release and after the insert, inside `LinkService.create`'s transaction with its single `now`. This is design §1 exactly. `Link`, the responses and `AuditLog` are unchanged |
| Append-only audit | the only statement that updates `audit_log` is V4's one-time backfill. `src/main` has no application `UPDATE` or `DELETE` of it |
| Coverage of the mechanisms | every test design §7 names is present: `LinkAuditColumnsTest` (AC-1, and AC-11 running the header's rollback read from the classpath), `LinkServiceStampTest`, `LinkAuditColumnsJourneyTest`, `LinkAuditColumnsFailureJourneyTest` (an audit failure rolls the retire and its stamp back) and `LinkUpgradeJourneyTest` (a pre-V4 file database upgraded in place). The pre-V4 state is computed as "the last version below 4", which survived V3's arrival. The post-V4 state is not pinned (W2F-01) |
| The two V3 pins (`132a884`) | `ClickAuditColumnsTest`'s second migrate gains `.target("3")`, and `ClickRetentionStartupJourneyTest.AC13_AC16`'s start gains `--spring.flyway.target=3`. No assertion changed. This matches the grant and the register's schema rule |
| Integration shape | a `--no-ff` merge, as for the other four. Its subject, `feat(04-audit-columns): …`, differs from the other four's `Merge slice/…`; that is cosmetic and has no effect |

### Documentary follow-through (not product in the range)

`17593aa` closes the `GAPS.md` rows for `link` and `audit_log` at `d55a502`. It also closes the
`click` and `user_agent_class` rows at `ed2b940`, stating that they were closed late. `382a7b2`
corrects the named QA judge. This resolves my W2P-02.

### Findings, final

| Id | Severity / class | Evidence | Finding | Disposition |
|---|---|---|---|---|
| W2F-01 | LOW / JUDGMENT-GAP **(mine)** | `LinkAuditColumnsTest.Database.migrate(false)` → `flyway.load().migrate()` (latest); `AC11_…` runs V4's rollback and then `migrate(false)` again; `LinkUpgradeJourneyTest` starts the application unpinned; my design §7: AC-1 "after all migrations", AC-11 "migrate to latest" | After V4, the slice's own schema tests migrate to latest, not to `4`. Once any V5 exists, AC-11's rollback-and-re-apply fails by construction: Flyway finds V4 unapplied below an applied V5. AC-1 and AC-7 also fail if V5 touches `link` or `audit_log`. This is the class the register's schema row records, which cost the `132a884` grant. My design specified "latest", and the slice's build pinned V3's tests but not its own | No product risk today. **Repair:** the slice that takes V5 pins `migrate(false)` to `target("4")`, and the upgrade journey's start to `--spring.flyway.target=4`, as `132a884` did. **Route:** lead backlog, with that trigger; named as drift in the register's schema row (`ea84e77`) |

| W2F-02 | HIGH (the review agent's finding) / JUDGMENT-GAP **(mine, design origin)** | the review agent's first full gate at `d55a502` failed `ClickRetentionScheduleJourneyTest:96` with zero `clicks purged` events while the deletion was already visible (`docs/review/02-brownfield/proof/final-wave-first-failure-d55a502.xml`); a focused rerun passes; its publication control shows a committed deletion with zero published events, then one (`…/purge-publication-control.txt`). `ClickPurge.run` logs only after `store.deleteBefore` returns; my design §7 AC-8 row: "poll the rows for at most 60 s", then "one `clicks purged` line" | The journey waits for the rows, then reads the log once. The run publishes its event after the `DELETE` commits, so a check in that window sees the deletion and no event: a race in the test, not in the product. **Its cause is my test row.** It told the builder to poll the rows and then expect the line, and probe A8b timed the deletion, not the event. | **Test-only forward fix**, routed by the lead through the review agent. The test polls for the event, or for both, within the same 60 s bound, then asserts the single event's `cutoff`. No product change. This vantage did not find it; it is recorded here because the design that caused it is mine.<br>**Fixed** on merge `50ad9c3` (the review agent's resolution `79eda7e`, where it is W2F-01). The journey now waits for the deletion **and** the `clicks purged` event within the original 60 s bound, then asserts the single event's `cutoff` exactly. Read by me in `git diff 4f247f4 50ad9c3`. The review agent ran the fresh full gate (476 tests) and coverage |

**Earlier items, status at `d55a502`:**

| Id | Status |
|---|---|
| W2P-01 (README purge-hold variable, LOW, mine) | **Open.** `README.md` is unchanged in the delta and still omits `URLSHORT_CLICK_PURGEENABLED`. The review agent records it as LOW CONTEXT-GAP for shared-document reconciliation. Route unchanged: the lead, under a README grant or in passing |
| W2P-02 (`GAPS.md` rows not closed after V3, LOW) | **Resolved** by `17593aa` and `382a7b2` |
| W2P-03 (stale design documents after merges, MEDIUM, mine) | **Resolved twice:** at `ed2b940` (`4975d25`) and at `d55a502` (`fe9529a`). The process proposal stands, so it stops recurring: at integrate, the lead files me a queue item per merge |
| W2P-09 (no-interrupt wording, MEDIUM, mine; the review agent's W2P-01) | **Resolved** at `2d3de57` and `bfc642d`. The review agent's file numbers this finding W2P-01. Its W2P-01 and this file's W2P-01 are different findings |
| W2P-04 to W2P-08 (INFO) | Unchanged. W2P-04's shutdown sum stays 18 s: V4 adds no background work |

**Backlog carried, not new:**
- the ping clock (`qitem-20261003213157-c0a336f9`);
- the Gradle download retries (MEDIUM, NOTES §2 22:00Z and 23:06Z, triggered after missions 02 and
  03 ship);
- the click hash's raw peer (W2D-03), which closes with mission 03's `01-analytics-v2` and is
  outside this range.

### Register walk (D20) at `d55a502`, all 13 rows

Register as committed at `ea84e77`.

| Concern | Verdict (structure / drift) |
|---|---|
| Client identity and proxy trust | **Consistent; carried drift.** The audit read's guard and the pin are unchanged by V4. The click hash still uses the raw peer (W2D-03), which closes with mission 03, outside this range |
| Time | **Consistent; carried drift.** `link.updated_at` takes the write's own instant from the application `Clock`; `audit_log`'s row-write times use the database clock, by policy. `PingController` remains backlog |
| Schema change | **Consistent, one drift (W2F-01).** V3 and V4 are expand-only with written rollbacks and numbers assigned at plan-lock, and V3's tests are pinned. V4's own tests are not |
| Audit columns | **Consistent.** All four tables carry the columns: defaults for the insert-only three, and `link`'s update stamp in the transaction of each write. The `GAPS.md` rows are closed |
| Error shape | **Consistent.** The delta adds no error path. An audit failure on retire rolls back the stamp with the retire (`LinkAuditColumnsFailureJourneyTest`) |
| Request id and logging | **Consistent.** No new log event |
| Audit trail writes | **Consistent.** `AuditLog` remains the only writer; V4's backfill is the only `UPDATE` of `audit_log`, at migration time. `AuditTrail` still names its columns, so no new column reaches a response |
| Client hashing for analytics | **Consistent.** Untouched in the range |
| Metrics and health exposure | **Consistent.** Untouched in the delta |
| API document | **Consistent.** Not regenerated in the delta; no response changed, as V4's design required |
| CI/CD | **Consistent.** Untouched in the delta |
| Background work | **Consistent.** No new job; the no-interrupt wording is corrected (W2P-09) |
| Operator settings | **Carried drift (W2P-01).** No new setting in the delta |

### Not verified by this vantage (final)

- No build or test run of my own. The integrator's gate on the merged main is in
  `docs/evidence/02-brownfield/integrate-04-audit-columns-check-305f804.txt`. The review agent runs
  the final offline gate on the merged range.
- W2F-01's failure under a V5 is reasoned from the code (Flyway refuses to apply a lower unapplied
  version without out-of-order), not run.

### Self-check (final)

- The delta's only product slice was compared with its locked design and ADR-0020, and marked as
  my design. The two V3 pins were checked against their grant.
- The `GAPS.md` closures were read as documentary follow-through, not as product.
- All 13 register rows have a final verdict. The register (`ea84e77`) and the design documents
  (`fe9529a`) are committed before this section.
- The one new miss I found is mine, classified with evidence and a route. The primary vantage's
  HIGH, whose design-level cause is mine, is recorded as W2F-02 and not claimed as this vantage's
  finding. ID collisions with the review agent's file are mapped.
