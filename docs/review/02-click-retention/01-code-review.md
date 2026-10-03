# Code review — 02-click-retention

**PASS on X `a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1`; no findings.**
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03.
Packet: `qitem-20261003223905-762d759f`. The same packet's security verdict is
in [02-security-review.md](02-security-review.md).

## Context and coverage proof

Reviewed the complete candidate in `.worktrees/02-click-retention`, clean at X,
against SPEC AC-1–16, design `f044cbe`, impact analysis, AGENTS, current
`docs/DESIGN.md`, ADR-0011/0013/0018/0020, QA SUMMARY/TRACEABILITY/GAPS and
PROGRESS/PROOF. Applied review, Java/Spring, QA, database and brownfield guidance.
The outcome is one daily/startup deletion of expired click rows, preserving the
boundary day, with validated settings, an explicit hold, PII-free events and
V3 audit columns. No new endpoint, dependency or abstraction is needed.

`git diff main...slice/02-click-retention --stat` and the full change were read:
18 files, 1,482 insertions, 7 deletions; merge base
`16312da08836aa666671c6b11c50202ac3f69c04`. Every changed file is listed below.
Confidence is high for this candidate's specified behavior. Performance at the
documented large-data ceiling and the future integration remain separate claims.

## Verification and QA evidence audit

Fresh `../../scripts/gw --offline check --rerun-tasks` at X passed in 65 seconds,
14 executed tasks: **174 unit + 172 functional**, no failures/errors/skips,
Javadoc and coverage verification passed. See [gate log](proof/code-check-a8fc8b6.txt).

QA's auxiliary original-suite replay had left `qaShippedTest.exec` in the build
directory. The repository's unchanged wildcard coverage merge includes it.
To establish this review's own canonical result, the external
[init script](proof/canonical-coverage.gradle) limits verification/reporting to
the fresh `test.exec` and `functionalTest.exec`; both coverage tasks passed
without rerunning tests ([log](proof/canonical-coverage-a8fc8b6.txt)).
Canonical-only totals equal QA's saved canonical report: **490/490 lines,
168/168 branches**. Unit: 447/490 lines, 168/168 branches; functional:
458/490 lines, 135/168 branches. No product/build configuration was edited.

[Independent reconciliation](proof/evidence-audit-a8fc8b6.json), produced by
[audit-candidate.py](proof/audit-candidate.py), checked all 333 saved coverage
hashes, all 210 candidate source-method hashes/mappings, all AC and business-rule
labels, 24 unchanged original functional sources against `f6dd29e`, and the
original replay's 155 successful results. Read QA's complete Java harness and
Python driver, including its corrected assumptions and controls. Audited the
249 recorded passing checks, independently paired all **207 raw HTTP responses**
with matching JSON request events/statuses, checked 14 purge events and canary
absence, and recomputed upgrade row preservation and complete legacy schema
metadata comparisons. These saved external effects remain attributed to QA;
this review did not rerun that driver or claim a new installed smoke.

| Criteria | Source and evidence conclusion |
|---|---|
| AC-1/2/3 | Bound `clicked_on < cutoff` deletion; real redirected boundary rows preserved, next-day window advances, actual seven-day environment override in QA capture. |
| AC-4 | Validated positive integer; all three rejected values fail startup without deletion; assertions correctly target the failure-analysis event. |
| AC-5/6 | Retained statistics and empty-before-redirect order verified; creation body, links and audit rows preserved. |
| AC-7/8 | Awaited startup run; daily five-second executor follows the application clock; autonomous schedule capture supplements direct-run tests. |
| AC-9/10/12 | Count/cutoff/period only; failed store reports class only; actual empty HMAC key produces correlated `reduction failed` while returning 302. |
| AC-11 | Suite's locking/concurrent journeys pass. QA additionally proves the actual global DELETE has entered JDBC and remains pending while a 302 and current-day write complete. |
| AC-13/16 | V3 applies to fresh and prior directories, preserves legacy values/constraints/indexes, fills eight non-null columns with static actors and write timestamps. |
| AC-14 | Original 24 Java sources unchanged; QA replay 155/155; granted profile pause prevents scheduled events entering inherited request-log windows. |
| AC-15 | Default on; explicit false prevents startup/daily deletion, emits the naming WARN; both required +60 service-second observations captured. |

Read the literal rollback in V3's header: drop only its eight columns and its
history row. Candidate SQL equals the previously independently exercised design
SQL (29 assertions), SHA-256
`908715401b5c84aa8b4d1525b91a9bd472d2a9ada605a8dbd5651ff1fdfc5fb6`.
Rollback restores schema, not deleted clicks; recovery requires a stopped-copy
backup. The accepted migration/catch-up timing residual remains unchanged.

## File ledger

Paths relative to the candidate root. Each verdict includes reading the full file.

| File | Verdict |
|---|---|
| `README.md` | PASS — granted operator-setting addition. |
| `src/main/java/dev/urlshort/click/ClickPurge.java` | PASS — serial startup/tick execution, strict cutoff, bounded close, fixed event fields. |
| `src/main/java/dev/urlshort/click/ClickRetentionProperties.java` | PASS — validated/defaulted operator settings. |
| `src/main/java/dev/urlshort/click/ClickStore.java` | PASS — one parameterized delete; existing insert/reads preserved. |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS — reduction reason distinguished, existing fail-open path preserved. |
| `src/main/java/dev/urlshort/click/package-info.java` | PASS — package boundary accurate. |
| `src/main/resources/application.properties` | PASS — defaults and environment names; X′ custody still required. |
| `src/main/resources/db/migration/V3__add_click_audit_columns.sql` | PASS — reviewed expand/backfill/defaults and literal rollback. |
| `src/functionalTest/resources/application-functional.properties` | PASS — granted hold and explanation; no logging suppression. |
| `src/test/java/dev/urlshort/click/ClickPurgeTest.java` | PASS — cutoff, timing, backward-clock, failure, thread and hold behavior. |
| `src/test/java/dev/urlshort/click/ClickAuditColumnsTest.java` | PASS — legacy shape, values, constraints/indexes and retired-connection inserts. |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — precise reason assertion; other behavior preserved. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionJourneyTest.java` | PASS — boundary/window/statistics/audit/logging/concurrency journeys. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionSettingJourneyTest.java` | PASS — seven-day boundary and event. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java` | PASS — no-trigger clock crossing, isolated context closed after class. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionStartupJourneyTest.java` | PASS — invalid startup, upgrade, startup purge and full hold. |
| `src/functionalTest/java/dev/urlshort/click/ClickPurgeFailureJourneyTest.java` | PASS — store/reduction failures, response continuity and safe events. |
| `src/functionalTest/java/dev/urlshort/click/ClickPurgeHoldJourneyTest.java` | PASS — shared-context schedule isolation. |

## Ponytail review

Lean already. Ship. One JDK executor, one configuration record and one SQL
statement satisfy the mechanism. The `ponytail:` comment names the measured
startup ceiling and a concrete future change; it is accepted intent. No new
dependency, speculative layer or hand-written platform substitute found.

## Findings

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| None | — | — | Fresh gate, complete source review and evidence reconciliation found no defect. | None |

None: MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No passing fix or backlog
item requested by this review.

## Integration verdict

Handoff to integrate under the lead's **21:42Z custody amendment**, not an
ancestry or acceptance claim on X. [Proof snapshot](proof/proof-state-a8fc8b6.json)
has items 1–8 and 10 accepted with all current evidence hashes matching; item 9
is pending. Continuation `qitem-20261003221121-7686465a` requires builder X′,
`git range-diff 16312da..X cb148c4..X′` (only the authorized README/properties
context exception), a fresh full gate on X′, and QA's ancestry/next-Flyway
judgment before acceptance. Any other changed patch returns to QA. Integration
must name both SHAs. No rebase, merge, tag or product edit performed here.

## Self-check

18/18 changed files read; exact QA candidate retained; fresh gate and isolated
canonical coverage verified; QA reports/traceability/gaps/effects audited;
security checklist completed in the companion report. Large catch-up,
SIGTERM-during-purge, Docker/PostgreSQL and online advisories are not newly
verified. The existing release smoke remains the readiness-budget gate.
