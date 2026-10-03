# Design review — 02-click-retention

Candidate: `be803067cd5901d320a03bf3900fe6e4abc19830` (design `7fbdcf4` plus correction).
Impact analysis/probe: `16f3de0f2f28449ed2a907f15203c2d8f0450621`, before the design.
SPEC: `69680e4e025824cfd321d13b698f933191699c0c`.
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003172504-1e5d5337`; instance `01M4170AA9E72WW1BXEA5PX0AP`.

**FAIL — one HIGH (DR-01), one MEDIUM (DR-02). Return to design.** The deletion,
schedule and privacy model are proportionate. The unresolved compatibility
contract must be corrected before plan-lock. DR-02 should be fixed in passing
with that rework, not deferred to backlog.

## Context proof

The outcome is deletion of reduced click rows before the configured UTC cutoff,
default 90 days, without changing links, audit or HTTP representations. The plan
adds one owned executor, one properties record and one parameterized store method;
there is no new dependency, schema migration or endpoint. It also fixes W2-05's
reduction-failure reason. Confidence is high in that boundary and the demonstrated
small-data mechanism; load and shutdown measurements below are the author's
evidence, not fresh reviewer benchmarks or a candidate implementation gate.

Read the complete SPEC, design, impact analysis, ADR additions and changes,
diagrams, probe source, outputs and build script; architecture §§3–8, database §8,
brownfield §7 and review guidance. The selected workflow has no additional SDLC
composition. Human Q4 A was checked at actual transition 876. RQ-01 is fixed in
SPEC AC-6 and its design test ordering. D-AC4 is an explicit producer-authored
requirements amendment, not an inferred reviewer exception.

## Complete file ledger

The handed-off set is the union of `16f3de0`, `7fbdcf4`, `be80306`: **18 files,
18 reviewed**. SPEC is separately read context. Fingerprints are in
[`proof/design-candidate.txt`](proof/design-candidate.txt). All target design and
probe bytes matched the candidate. Mission NOTES has later concurrent additions;
only its handed-off delta was reviewed.

| File | Verdict |
|---|---|
| `docs/DESIGN.md` | Contracts and pending-design labels consistent; carry DR-01/02 corrections as needed. |
| `docs/adr/0011-click-handoff-bounded-single-writer.md` | Separate executor amendment justified by different work and shutdown ownership. |
| `docs/adr/0013-click-events-and-request-time-statistics.md` | No-index note supported by measured alternatives. |
| `docs/adr/0018-click-retention-daily-purge.md` | Mechanism, ceiling, rollback and alternatives explicit; test-isolation reasoning needs DR-01 resolution. |
| `docs/diagrams/container.mmd` | Purge dependency and log paths match design. |
| `docs/diagrams/purge-sequence.mmd` | Startup, daily, failure and stop sequence consistent. |
| `missions/02-brownfield/NOTES.md` | Candidate handoff delta accurately records scope and remaining grants. |
| `missions/02-brownfield/slices/02-click-retention/design.md` | DR-01 HIGH; DR-02 MEDIUM; all 14 ACs otherwise have mechanisms and named tests. |
| `missions/02-brownfield/slices/02-click-retention/impact-analysis.md` | Baseline, consumers, compatibility and rollback covered; DR-01 rejects its accepted test-flake residual. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/RetentionProbe.java` | Read all 819 lines; variants distinguish single/batched DELETE and synchronous/asynchronous startup. Root WARN matters to A4. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/retention-probe.gradle` | Probe compiles/runs on existing functional classpath; no product build edit. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/baseline-f6dd29e.txt` | HTTP, storage, indexes and log observations support existing-system analysis. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/batch-subquery-jstack.txt` | Stack supports the rejected IN-subquery variant; not evidence of a problem in the selected statement. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output.txt` | S1/A8/A4/L0–L4/D1–D2 read against source; A4 observation scope needs DR-02. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-2.txt` | L5–L10/A7 support no-index tradeoff and readiness ordering; timings are indicative. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-3.txt` | D3 supports completion after pool closure while JVM remains alive; 20/20 clicks observed. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-4.txt` | D4 supports consistent recovery after process exit, not a SIGTERM-to-candidate test. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-first-attempt.txt` | Failed, superseded batch experiment retained honestly; not counted as selected-mechanism success. |

## Contract assessment

| Contract | Assessment |
|---|---|
| AC-1–3, boundary and period | Bound `LocalDate`, strict `<`, positive startup setting; boundary/next-day/configuration journeys planned. |
| AC-4, invalid setting | Binding prevents ready event and purge. Failure analysis is clean in the independent control; whole-capture assertion is wrong, DR-02. No HTTP ProblemDetail is applicable to a startup failure. |
| AC-5–6, retained statistics and unaffected data | Query shape unchanged; links/audit outside the DELETE; RQ-01 observation ordering retained. |
| AC-7–8, startup and autonomous daily scheduling | Startup awaited before readiness; actual 5-second scheduler follows the suite clock. Fresh single-DELETE control deleted two expired rows and kept two boundary rows after 4,564 ms without a trigger. |
| AC-9–10, events and failure recovery | Static event fields, no throwable/message, one class-only WARN, next-day retry; named canary and store-failure tests. No new HTTP error path. |
| AC-11, redirects during deletion | Author's load/lock probes support separate-row reads/writes; deterministic lock-held and concurrent-deletion journeys planned. No independent throughput claim made. |
| AC-12, reduction failure | One reason token changes in the existing catch; test uses the existing hash failure seam. |
| AC-13, upgrade | V2 fixture plus real prior-jar data-directory capture; no migration from this slice. All retained columns/audit/link rows compared. |
| AC-14, unchanged suite | Not secured: a legitimate daily purge invalidates existing global log-window assertions, DR-01. |
| Security and privacy | Startup configuration and clock are operator trust boundaries; no client purge endpoint; parameterized SQL touches only click rows; no audit mutation; logs contain no stored client values. Clock-step and irreversible-deletion risks explicit. |
| Scope, rollback, simplicity | No speculative framework/index/schema. Revert restores behavior, not deleted data; stopped-directory backup documented. Ordered properties custody and audit-read ancestry remain; README grant is still the lead's plan-lock decision. |

## Findings

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `missions/02-brownfield/slices/02-click-retention/design.md:317` | The plan explicitly accepts a daily purge line landing in an existing request capture. AC-14 requires the shipped suite unchanged; `ObservabilityJourneyTest.java:87` requires every captured event to carry that request's id, while SPEC rule 6 forbids a request id on purge events. Fresh control invokes the actual compiled, unchanged AC26 test method: no overlap PASS; a scheduled purge between capture start/end produces `AssertionFailedError`, expected the response id but got empty. Evidence: `proof/design-controls-filtered.txt`. Thus two successful ordinary runs do not discharge the compatibility claim. | Resolve compatibility before locking: choose a deterministic observation/isolation strategy that preserves both log contracts and prove the overlap case. If a narrow test change is needed, obtain the corresponding SPEC/territory adjustment from its owners and record it in the impact analysis. Do not suppress production purge logs, attach a false request id, or silently discard all uncorrelated events. A GAPS note alone is not a resolution of the current AC-14. |
| DR-02 | MEDIUM | `missions/02-brownfield/slices/02-click-retention/design.md:300` | Planned AC-4 assertion checks the full captured startup output for absence of datasource URL. The author's probe sets root WARN (`RetentionProbe.java:78`), hiding normal startup INFO. With shipped logging, independent control reports `capturedDatasource=true`, `failureAnalysisDatasource=false`, `purgeStarts=0`: Hikari and Flyway log the JDBC URL before binding fails. The failure-analysis event itself satisfies the amended requirement. Evidence: `proof/invalid-setting-control.txt`. | Scope the AC-4 no-other-configuration assertion explicitly to the failure-analysis event under shipped logging and correct claims that A4 proved a clean whole capture. Preserve separate failed-start/no-delete assertions. Fix in passing; no new production logging layer is needed. |

The producer acknowledged both issues at 17:32Z and will answer them in the
rework packet; that acknowledgement is not a resolution or re-review.

## Independent verification and limits

- `scripts/gw --offline check`, with wrapper log in
  [`proof/baseline-check.txt`](proof/baseline-check.txt): SUCCESS; all 14 tasks
  up-to-date. Product sources/build are unchanged from `f6dd29e`. This checks
  the baseline and toolchain, not an as-yet-unwritten retention implementation.
- The commands in [`proof/review-retention.gradle`](proof/review-retention.gradle)
  run the two fresh controls using the handed-off probe component with the
  selected single DELETE, synchronous startup and 3-second close settings.
  No product source, test or build file changed. The AC26 overlap control widens
  only the observation window via a MockMvc result handler to force a legal
  interleaving; it does not claim a spontaneous failure of the full suite.
- First control attempt omitted RequestIdFilter from manual MockMvc setup;
  [`proof/design-controls.txt`](proof/design-controls.txt) is retained as an
  invalid harness attempt. The corrected run registers the actual filter and
  first proves the same unchanged test passes without overlap. No finding is
  based on the invalid attempt. Trailing whitespace in the corrected control log
  was normalized; its event and assertion text is retained.
- The author's million-row timing and process-recovery evidence was audited,
  not rerun. Candidate SIGTERM/drain, actual class lifecycle, upgrade captures,
  full suite and coverage remain builder/QA/release obligations. The explicit
  startup-size ceiling and host-clock risk remain design limitations, not
  newly manufactured findings.

## Self-check and exit

Exact candidate identified; all 18 handed-off files reviewed; all 14 ACs mapped;
both findings reproduced with scoped evidence and cited lines. Only review
artifacts authored. One design-review ledger row records 0 MUST-FIX, 1 HIGH,
1 MEDIUM, 0 LOW/INFO. **Exit failed to design**, then focused re-review of the
responses and changed contract/test strategy. No request for a human gate here.

## Re-review 59c8762e660cd0b9a83e08a8dbf28b7ac9746e6c

2026-10-03, `review2-agent@urlshort-factory`; packet
`qitem-20261003181452-6bc9f909`, same workflow instance. **FAIL — DR-01 and DR-02
fixed; new DR-03 HIGH and DR-04 MEDIUM.** DR-03 concerns the newly allocated
human-policy requirement, so this is not a repeated failure of DR-01 and does
not trigger its deadlock escalation.

### Scope and complete delta ledger

All seven files in producer commit `59c8762` read; exact candidate fingerprints
and the contract mismatch are recorded in
[`proof/rereview-candidate-59c8762.txt`](proof/rereview-candidate-59c8762.txt).
Prior unchanged design content retains the first review. Current requirements
are SPEC **`32b1ae2`**, with AC-15 from the hold amendment and AC-16 from the human
audit-column decision. Also read the revised manifest/grant, GAPS allocation,
and database policy. The design's old SPEC reference does not supersede these
handed-down requirements.

| Rework file | Verdict |
|---|---|
| `docs/DESIGN.md` | Hold and startup-failure observation changes match DR-01/02 resolutions. |
| `docs/adr/0018-click-retention-daily-purge.md` | Hold, operational reason and isolated test contexts recorded; migration decision still missing, DR-03. |
| `missions/02-brownfield/slices/02-click-retention/design.md` | Original findings fixed; AC-16 missing, DR-03; AC-15 event/test detail, DR-04. |
| `missions/02-brownfield/slices/02-click-retention/impact-analysis.md` | Overlay and context lifecycle updated; no-migration claim must change with DR-03. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/RetentionProbe.java` | Full changed portion read: bound boolean defaults true; disabled branch warns and returns before submitting work or arming the tick. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/retention-probe.gradle` | Part 5 command correctly selects hold control. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-5.txt` | No startup/daily run observed for disabled purge; scope is 11 seconds, not full AC-15 acceptance evidence. |

### Resolutions

| Finding | Resolution | Evidence |
|---|---|---|
| DR-01 HIGH | **Fixed.** The authorized functional overlay disables startup and scheduled work in shared contexts; enabled scheduling tests own and close their contexts. Production defaults remain enabled, the hold is observable, and the SPEC now explicitly allows it and the overlay. | `slice.yaml` grant `cec7032` as updated, SPEC AC-14/rule 3/A-9, design §§1/7. Fresh `scripts/gw --offline -I docs/review/02-click-retention/proof/review-retention.gradle reviewRetention -PreviewCase=hold`: SUCCESS; no runs after readiness or after 11 seconds beyond the next scheduled time; both seeded rows retained. Log: `proof/hold-rereview-59c8762.txt`. |
| DR-02 MEDIUM | **Fixed.** The test now selects the failure-analysis ERROR event, while separately checking no purge event and unchanged rows. Root-WARN limitation of the earlier probe is acknowledged. | Design AC-4 row at line 322, revised SPEC AC-4 and DR-02 response. Existing independent INFO-level control already proves the scoped assertion; no changed production logging requires repeating it. |

The fresh hold run uses the changed, handed-off probe component. It supports
the design fix; it is not a completed implementation of the new AC-15 journey.
Product sources/build are unchanged. No repeat of the baseline full gate was
needed for this documentation-only re-review; its successful first-review
result remains scoped to the baseline.

### New findings from the revised requirements and fix

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| DR-03 | HIGH | `missions/02-brownfield/slices/02-click-retention/design.md:168` | Design still specifies **no migration/no Flyway number**, impact analysis line 58 agrees, and neither document supplies audit-column DDL, backfill, insertion behavior, rollback or an AC-16 test. Current SPEC `32b1ae2` AC-16 requires all four audit columns on `click` and `user_agent_class`; the manifest allocates V3 and GAPS explicitly assigns it to this slice. The exact-text check is in `proof/rereview-candidate-59c8762.txt`; the producer confirmed the omission at 18:16Z. Building this design cannot satisfy the newly assigned AC-16/AC-13. | Design the V3 expand migration, four columns/types/nullability/static actors, old-row backfill and new-row write timestamps, compatibility with existing v1-column-list inserts, written rollback and upgrade/AC-16 tests. Update the impact analysis, ADR/DESIGN contracts and custody text against current SPEC. Keep `link`/`audit_log` on the named 04-audit-columns migration. |
| DR-04 | MEDIUM | `missions/02-brownfield/slices/02-click-retention/design.md:74` | New AC-15 requires the startup WARN to name the disabling setting. The proposed and freshly observed event is only `message="click purge off", retentionDays=90`; it never names `urlshort.click.purge-enabled`. The proposed hold journey at line 333 seeds after context startup and observes six seconds, whereas AC-15 includes old data present before startup and observations 60 seconds after readiness and the scheduled time. Core hold behavior works, but its operator message and acceptance plan are incomplete. | Include the setting name/state in the WARN with static text/fields. Map a named test explicitly to AC-15, with pre-existing old rows before startup and the two required observation points; keep the shorter isolation probe as supporting evidence. Fix in passing during DR-03 rework. |

The GAPS rows validly track the shipped tables until their assigned migrations
merge. They assign V3 to this slice; they do not defer this slice's AC-16.
The human policy was read from `qitem-20261003175330-fb054f2f` and database
guidance §§2/8 before this review. No new finding is raised on the unchanged
`link` or `audit_log` schema, which has a named follow-on migration.

### Re-review self-check and exit

Seven producer delta files reviewed, current requirements/allocation checked,
hold control run and read, original findings explicitly resolved. Only review
evidence changed; no product/test/SPEC/design edit. One new ledger row records
0 MUST-FIX, 1 HIGH, 1 MEDIUM, 0 LOW/INFO. **Exit failed to design** for DR-03;
DR-04 expected fixed in the same revision. Next review is scoped to those
responses and the added migration design; DR-01/02 stay settled absent new
evidence.

## Re-review f044cbe0f4528c287e95fcb5e24298add35240af

2026-10-03, `review2-agent@urlshort-factory`; packet
`qitem-20261003182930-370196d0`, same workflow instance. **PASS — DR-03 and DR-04
fixed; no open findings.** DR-01/02 remain settled. Current SPEC is `32b1ae2`.
This is approval of the design for plan-lock, not implementation acceptance.

### Complete delta ledger

All nine producer delta files read and byte-matched to the candidate; fingerprints:
[`proof/rereview-candidate-f044cbe.txt`](proof/rereview-candidate-f044cbe.txt).
Unchanged content retains the previous reviews. Confidence is high in the added
migration mechanism and acceptance mapping; large-data startup timing remains
limited to the author's measurements below.

| Rework file | Verdict |
|---|---|
| `docs/DESIGN.md` | PASS: hold event, V3 contract, ERD and ADR entry match the chosen mechanism. |
| `docs/adr/0020-audit-columns-expand-migration.md` | PASS: row/domain clocks separated, constant actors, defaults, backfill, rollback and alternatives explicit. Insert-only tables need no update trigger. |
| `docs/diagrams/erd.mmd` | PASS: all eight new columns represented and labelled designed. |
| `missions/02-brownfield/slices/02-click-retention/design.md` | PASS: DR-03/04 resolved; AC-13/15/16 now have concrete mechanisms and named checks. |
| `missions/02-brownfield/slices/02-click-retention/impact-analysis.md` | PASS: V3, legacy insert compatibility, backfill, custody and rollback covered. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/MigrationProbe.java` | PASS: fresh/upgrade/connection-retirement/rollback/load controls read; printed results independently asserted below where material. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/migration-probe.gradle` | PASS: existing offline classpath, no product build change. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/migration/V3__add_click_audit_columns.sql` | PASS: exact DDL and literal rollback header exercised independently. |
| `missions/02-brownfield/slices/02-click-retention/design-probe/output-6.txt` | PASS as scoped evidence: M1–M4 support migration behavior; M5 wall elapsed is 56,811 ms, distinct from Flyway's reported 42.041 s. |

### Resolutions and evidence

| Finding | Resolution | Evidence |
|---|---|---|
| DR-03 HIGH | **Fixed.** V3 adds the required four non-null columns to each table, fills legacy rows, gives unchanged inserts database write timestamps and static actors, and has a working rollback. No client value enters the new columns. AC-13 maps the real prior-directory upgrade and subsequent redirect; AC-16 maps schema, legacy columns/constraints and row assertions. | Design §§1/3/7, ADR-0020, exact migration lines 9–26. Fresh independent file-database control: 29 assertions PASS, including legacy column metadata/values, FK/check behavior, primary-key columns, keeper connection after Flyway retirement, reopened database, row-time/domain-time separation, literal header rollback and reapplication. |
| DR-04 MEDIUM | **Fixed at the design boundary.** Startup WARN now says `click purge paused, no click is deleted` and names `urlshort.click.purge-enabled` in `setting`. AC-15's journey seeds expired data before startup and observes retained rows/no purge event at both required suite-clock points, 60 seconds after readiness and the next scheduled time. | Design lines 80–81, event table and AC-15 row at line 362. Prior hold mechanism control remains supporting evidence; the new message and full acceptance journey are implementation/QA obligations. |

Command, run from the repository root:

```text
scripts/gw --log docs/review/02-click-retention/proof/migration-rereview-f044cbe.txt --offline -I docs/review/02-click-retention/proof/review-migration.gradle reviewMigration
```

**BUILD SUCCESSFUL**, 29 assertions. Source:
[`proof/ReviewMigrationProbe.java`](proof/ReviewMigrationProbe.java); output:
[`proof/migration-rereview-f044cbe.txt`](proof/migration-rereview-f044cbe.txt).
The control uses the exact candidate SQL and extracts rollback statements from
its header; it does not substitute a reviewer-written migration. The database
is a new temporary file, separate from the author's probe and service data.

No full product gate was rerun for this documentation-only delta; product
sources/build are unchanged from the previously checked baseline. The new
Flyway mechanism was executed directly. Actual HTTP upgrade evidence,
candidate coverage and full unchanged-suite validation remain builder/QA work.
The million-row benchmark was audited, not rerun. Its 56.8-second wall duration
does not establish that migration **plus** startup deletion fits compose's
80-second health window at that volume; implementation/release must measure the
combined path before claiming that. The separate, named `04-audit-columns`
allocation for `link` and `audit_log` remains valid.

### Self-check and exit

Nine changed files reviewed; exact candidate and current SPEC verified; both
responses adjudicated; fresh assertions read after completion. Only review
artifacts changed. Zero MUST-FIX/HIGH/MEDIUM/LOW/INFO remain open, with no new
backlog item. **Exit handoff to plan-lock.** Ordered application-properties
custody and the documented README grant remain the lead's plan-lock work.
