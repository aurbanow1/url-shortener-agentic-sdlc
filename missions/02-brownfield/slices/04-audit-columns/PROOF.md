# PROOF — OPR.99.0.3.4 Audit Columns

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.4 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

QA checked by: qa2-agent@urlshort-factory (Codex), 2026-10-03T23:47Z. Verdict: PASS on `305f8045d45b19a9e3287d5fe3508af6e04db9a4`. Integration/closure pending.

## What this proves

Link and audit rows carry the added audit columns without changing public behavior. All three link writes maintain service-clock update stamps; no-op/refused/rolled-back operations stamp nothing. A real shipped directory upgrades without changing legacy values and the literal migration rollback restores its prior schema on a copy before V4 reapplies.

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/qa-evidence-305f804.md — QA drop covering contract1–4 and6–9; item5 awaits its explicit merge-time gap closure.
- docs/qa/coverage/04-audit-columns/SUMMARY.md — fresh gate, CSV percentages and evidence scope.
- proof/qa-305f804/run-20261003T234116500899Z/ — independent HTTP, row, schema, rollback and log captures.
- proof/qa-report-hashes.json and qa-junit/ —366 copied coverage hashes and both saved suites.

## Residue / caveats (if any)

See docs/qa/GAPS.md §04-audit-columns for complementary per-suite misses, authorized migration pins, external fixtures, parameterized XML attribution and the explicit PostgreSQL/Docker/large-directory timing limits. No merged coverage or acceptance failure.

## Builder

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003223549-c479c189`.
This section is builder evidence, not a verdict.

**Candidate:** `305f8045d45b19a9e3287d5fe3508af6e04db9a4`. The branch was built on `main` `583278c`
(V1 and V2 only), then rebased cleanly onto `main` `2566c38`, which carries `02-click-retention`'s merge
`ed2b940` and its V3. `git merge-base --is-ancestor ed2b940 305f804` succeeds. **V4 is the next Flyway
number**: the migrations are `V1`, `V2`, `V3__add_click_audit_columns`, `V4__add_link_audit_columns`.
V3 touches only `click` and `user_agent_class`.

| SHA (rebased) | Commit |
|---|---|
| `8f72b2f` → `7d49984` | `test`: `LinkAuditColumnsTest` (AC-1, AC-11), `LinkServiceStampTest`, `LinkAuditColumnsJourneyTest` (AC-2 to AC-6, AC-8, AC-10), `LinkAuditColumnsFailureJourneyTest` (AC-5's failed retire), `LinkUpgradeJourneyTest` (AC-7) |
| `49f0cee` → `f784ed7` | `feat`: V4 copied verbatim from `design-probe/migration/`, the retire SQL that also stamps, `LinkRepository.stamp(id, at)`, and the two `LinkService.stamp` calls |
| `305f804` | `test`: the lead's grant (`slice.yaml` `132a884`, request `qitem-20261003231052-c29e30a9`). `ClickAuditColumnsTest`'s second migrate gains `.target("3")`, and `ClickRetentionStartupJourneyTest` AC13_AC16's `start(...)` gains `--spring.flyway.target=3`. Both tests treated "latest" as V3, and V4 made them fail. No assertion changed. |

### Commands and outcomes

| Command | Outcome | Record |
|---|---|---|
| the new journeys on `8f72b2f`, before V4 | red: 9 run, **7 failed** (no audit columns; no V4 in `flyway_schema_history`). AC-8 and the failed-retire case pass, as expected before the change: nothing is visible yet, and nothing is stamped | `proof/builder-red-functional.txt` |
| the unit suite on `8f72b2f` | red as a compile failure: `LinkRepository.stamp` does not exist | `proof/builder-red-unit.txt` |
| `scripts/gw --offline check --rerun-tasks` on `49f0cee` | BUILD SUCCESSFUL; **unit 208/208, functional 211/211**; merged **496/496 lines, 194/194 branches**; `javadoc` green. Every shipped test passes unchanged, `ClickSchemaTest`'s and `ObservabilityJourneyTest`'s v1-shaped inserts included (AC-9) | `proof/builder-check-49f0cee.txt` |
| `git diff --stat 583278c 49f0cee` | eight files: `link/` main and tests, and V4. No shipped test, `audit/` or response class changed | — |
| `check --rerun-tasks` after the rebase, before the grant | **red in exactly two shipped click-retention tests**: `ClickAuditColumnsTest` (columns of other tables changed) and `ClickRetentionStartupJourneyTest.AC13_AC16` (expected versions exactly 1–3; got 4). Every other test passed (unit 217/218, functional 232/233) | `proof/builder-check-f784ed7.txt` (it stops at the unit failure; the functional failure came from a separate `functionalTest` run, not kept) |
| `scripts/gw --offline check --rerun-tasks` on the candidate `305f804` | BUILD SUCCESSFUL; **unit 218/218, functional 233/233**; merged **557/557 lines, 200/200 branches**; `javadoc` green | `proof/builder-check-305f804.txt` |
| `git diff --stat 2566c38 305f804` | the territory (`link/`, V4) plus the two granted lines in `click/` tests | — |

### Deviations from design.md

1. **"Before V4" is computed, not `target("3")`.** Flyway 11 refuses a target with no migration ("No migration with a target version 3 could be found"), and this base has no V3. `LinkAuditColumnsTest` and `LinkUpgradeJourneyTest` therefore migrate to the highest migration version below 4 on the classpath. That is V2 today and V3 after the rebase, with no test change.
2. **Each journey test sends from its own loopback peer** (`127.44.0.n`). With one shared peer, AC-4's +25 h shift left that peer's rate-limit bucket ahead, and the next test got `429`s (first red run). Loopback peers also admit the AC-8 audit read.
3. **The upgrade journey's released link** is a link whose key is `NULL` next to a later link holding `K`, in the shipped shape. A key release leaves no other trace (rule 6).

### Verified by effect: the upgrade (AC-7, proof item 6)

The real `f6dd29e` jar and the candidate jar ran on one data directory.
- **Shipped jar:** it created an active link with an idempotency key and a second link, retired the
  second link, and redirected through the first (`proof/upgrade-0-shipped-f6dd29e.txt`).
- **Candidate jar** (built from `f784ed7`; product code identical to `305f804`, `git diff f784ed7 305f804 -- src/main` is empty):
  - Flyway applied 2 migrations, V3 and V4 (`proof/jar-log-upgrade.txt`).
  - The active link reads exactly as before, `createdAt` included (`upgrade-1`). The retired link answers `410` (`upgrade-2`).
  - `GET /api/audit` returns the three rows with exactly the eight fields (`upgrade-3`).
- **Columns, read with H2's Shell after the candidate stopped** (`upgrade-4-columns.txt`):
  - the active link: `updated_at` = `created_at`;
  - the retired link: `updated_at` = `retired_at`;
  - every audit row: `created_at` = `updated_at` = `occurred_at`;
  - every actor column `anonymous`;
  - `flyway_schema_history` 1–4 all `TRUE`.
- **Not in this capture:** a released key, because on the real clock a release needs a 24-hour wait. `LinkUpgradeJourneyTest` covers that row.
- **Schema and new rows after the upgrade (item 6):** the candidate was restarted on the directory, and one link was created and retired (`upgrade-5-schema-and-new-rows.txt`).
  - Both tables list every column with its type, length, nullability and default. The new ones are `NOT NULL`, with defaults `CURRENT_TIMESTAMP` or `'anonymous'` and `VARCHAR(64)`. The shipped columns are as before.
  - The new link: `updated_at` = its retirement instant.
  - The new audit rows: `created_at` = `updated_at` on the database clock (a few ms before the service-clock `occurred_at`), actors `anonymous`.

### Verified by effect: the rollback (AC-11, proof item 7)

On a copy of that upgraded directory (`rollback-data`):
1. **The rollback**, the header's seven `DROP COLUMN` statements plus the history delete, run with H2's Shell (`rollback-1-after-rollback.txt`):
   - `link` and `audit_log` have exactly the shipped columns and the six shipped constraints;
   - every link's `code`, `url`, `created_at` and `retired_at` is unchanged;
   - the history lists V1–V3.
2. **The candidate started on the rolled-back copy** (`jar-log-reapply.txt`), then stopped (`rollback-2-reapplied.txt`):
   - V4 re-applied (history 1–4 `TRUE`); the columns, defaults and nullability are back;
   - links are backfilled as `COALESCE(retired_at, created_at)`.
   - The two audit rows written after the first upgrade now carry `created_at` = `occurred_at`. The rollback dropped their database-clock times, and the re-apply backfills from the event time, as the header says.

### Not verified

- PostgreSQL's `LEAST` and `ALTER … DEFAULT … NOT NULL`, which the design did not run either.

## Self-check (builder)

- **Test before fix:** `7d49984` red (the logs above), then `f784ed7` green.
- **Territory:** `link/` main and tests, and V4, plus the two granted `click/` test lines. `audit/`, the `Link` record, every response and `AuditLog` are unchanged, so AC-8 holds by construction, and its journey checks it.
- **AC-9:** every shipped test passes. The only shipped tests changed are the two granted lines, and those change which migration they stop at, not what they assert.
- **Gate** on the candidate `305f804`, after the last change.


## QA

QA2 `qa2-agent@urlshort-factory` (Codex), 2026-10-03T23:47Z, packet `qitem-20261003232046-aa44c49f`.
**PASS** on exact `305f8045d45b19a9e3287d5fe3508af6e04db9a4`; fresh independent gate218 unit/233 functional, zero failures/errors/skips,
merged 557/557 lines 200/200 branches. No product or test edit. Read SPEC, design, impact analysis,
builder proof, prior independent design review and QA guide before judging effects.

Own complete run: `missions/02-brownfield/slices/04-audit-columns/proof/qa-305f804/run-20261003T234116500899Z/summary.json` (94 curl requests,787 assertions). Every response ID joins
its own JSON request-completed event/status; failed audit INSERT logs only exception class chain
and code origin, sanitized500, no new audit row, whole link unchanged. The successful creates and
retire each correlate to actual persisted audit rows. Full capture argv/headers/bodies/rows/logs are
retained. By-effect evidence is independently produced; builder captures are not substituted.

| ACs | Observed effect / primary evidence |
|---|---|
|1 | Fresh V1–V4 schema; seven new non-null columns/types; all legacy metadata and constraint semantics preserved (`fresh-schema.json`, `upgrade-before-f6.json`, `upgrade-after-candidate.json`) |
|2–3 | Created and retired link/audit rows: exact frozen service instants, preserved creation, equal database row clocks and anonymous actors (`AC2-created-row.json`, `AC3-retired-row.json`) |
|4 | Actual expired-key HTTP create releases A and stamps it at B's creation instant, preserving A's legacy data (`AC4-key-release.json`) |
|5–6 | Replay201/mismatch422/read200/redirect302/repeat410 leave stamps/trail intact; actual JDBC audit failure500 rolls back every link field; audit rows stay append-only (`AC5-real-jdbc-rollback.json`, HTTP and SQL assertions) |
|7 | Real f6 jar writes active/retired/released links; candidate starts on same directory; all old values unchanged, old stamps correctly backfilled, future-event cap observed (`upgrade-before-f6.json`, `upgrade-after-candidate.json`) |
|8 | Exact five-field link/four-field stats/eight-field audit bodies; correct status/Location/media/empty redirects; no added field/header/log; named SELECT/OpenAPI unchanged (`qa-evidence-recheck.json`, HTTP captures) |
|9 |59 baseline source/resource hashes:57 unchanged and2 authorized assertion-preserving V3 pins; all 213/224 inherited invocations green;271 mapped methods (`qa-baseline-file-hashes.json`, `qa-junit-summary.json`) |
|10 | UA/key/address/URL/request-ID canaries absent from audit columns; staticanonymous actor; no raw client values in logs (`AC10-audit-columns.json`, full JSON logs) |
|11 | Exact literal header eight SQL statements; rollback only on stopped copy restores prior schema/constraints/all legacy data; real candidate reapply successful (`rollback-literal-header.json`, `rollback-after.json`, `rollback-reapplied.json`) |

All captures in table except static/JUnit files are in the complete run directory. Prior32-control
design review read and ran the exact same migration/header; candidate/design migration bytes equal,
so contract 8's recorded review exists before the downstream implementation review. Contract 9's
impact analysis precedes design; plan-lock recheck recorded in slice.yaml and exact candidate named
SELECT/ancestry/nextV4 independently confirmed. Interim policy gap closes at merge, not at QA.

## Self-check (QA)

- Every AC exercised by effect; all failure cases including bad input, duplicates, expired key,
  repeated retire, actual audit-store failure and default-budget429 checked.94 response/log joins.
- CSVs independently read: per-suite87.97/97 and93.90/83; merged 100/100.366 report hashes match.
- All 271 test methods map to an AC/BR, every AC has named evidence;41 class-only parameterized XML
  attributions explicitly qualified. AC1/11 unit allowance and real functional rollback compensate
  the absence of a JUnit functional rollback method as permitted by the locked SPEC/design.
- GAPS entry written; no exclusion or threshold reduction. Original 437 invocations passed with
  only the granted2 migration pins; no assertion change. Original code/docs remain read-only.
- Proof drop covers1–4 and6–9 with candidate judgments before handoff; item5 requires the future merge-time interim-gap closure and is routed durably to the lead.
- All 7 app processes stopped; candidate remains clean at exact SHA; rollback only on copied database.
- Not checked: PostgreSQL, Docker, large-directory migration timing; normal shutdown is the only
  shutdown claim. Interrupted QA instrument runs preserved; only the complete final run is claimed.

Current accepted receipts: 2,3,5,6,7,8,9,10 (item1 receipt9 reaffirms final summary formatting; item4 receipt10 reaffirms another slice append to shared TRACEABILITY while this slice section stayed byte-identical; earlier receipts retained as history). Items1–4,6–9 accepted against305f804; item5 pending under lead-confirmed qitem-20261003234855-d2ef129b.
