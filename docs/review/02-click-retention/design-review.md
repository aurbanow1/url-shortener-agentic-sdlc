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
