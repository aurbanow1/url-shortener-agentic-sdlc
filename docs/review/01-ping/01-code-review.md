# 01-ping — code review

Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003005806-5aaa453e`.
Candidate: `f286a10863e4a8081235226f2d56e51ac121b319`, identical to QA's PASS.
Worktree: `.worktrees/01-ping`, clean before and after verification.

## Verdict

**PASS — no blocking or non-blocking code findings.** Ready for the assigned
security review, then integration. All nine changed files were read in full;
the rename counts as one changed file.

The existing design-document follow-up remains with the design agent at
integration: reconcile the thread-name example with the approved exclusion.
It requires no code change or reopening of QA-01; the locked design remains
historical. No new backlog item is created by this review.

## Context proof

The locked outcome is a ping response with exactly `status=ok` and a current
UTC instant, fresh server-issued `X-Request-Id`, a corresponding JSON log
event with no client data, and a 405 ProblemDetail retaining the id. The
human's minimal-filter constraint rules out extra tracing machinery.
Implementation has a record, controller and ordered filter; no state,
domain service, SQL, custom error handler or new dependency.

Primed from the locked SPEC (`4e581cc`, later approval metadata only), design
(`d0521de`), `AGENTS.md`, `docs/DESIGN.md`, ADRs, QA coverage/traceability/gaps,
the QA-01 decision and re-check, and the Java/Spring and QA guides. Reviewed
the full `git diff main...slice/01-ping`; no unrelated main-checkout edits
were included. Confidence: contract 100/100, changed-code flow 100/100,
test/evidence interpretation 100/100. Packaged artifacts and dependency
advisory checks remain outside this code review.

## Independent verification and QA audit

```sh
scripts/gw --log docs/review/01-ping/proof/code-check-f286a10.txt --offline -p .worktrees/01-ping check --rerun-tasks
```

Exit 0, `BUILD SUCCESSFUL in 19s`, all 13 tasks executed. The main wrapper
supplies its current `--log` support; Gradle builds the exact candidate
project in the worktree. Fresh JUnit XML: 6 unit and 9 functional tests,
zero failures, errors or skips. JaCoCo: unit 15/15 lines, functional 13/15,
merged 15/15. There are zero branches; the branch gate passes without a
claim of exercised branch logic. No exclusions or lowered thresholds.

All three fresh CSVs match QA's committed CSVs byte for byte. Traceability
covers all eight ACs, eight business rules and all 15 executed test methods;
the informational functional-only shortfall and historical QA-01 are honest
in GAPS. QA's PASS proof names the same candidate and contract items 1–7.

Independently parsed QA's four live responses, UTC interval bounds and full
startup-through-shutdown log. Exact body shape, time, id shape/uniqueness,
405 body/media type and matching JSON GET events agree with the report.
No canary or checked loopback address occurs in that log; ping events omit
the thread-name member. This audits QA's live evidence; it does not claim
the reviewer repeated those socket requests during code review.

Evidence: [fresh gate](proof/code-check-f286a10.txt),
[XML/coverage/live-capture audit](proof/code-evidence-audit-f286a10.txt),
and the candidate-suffixed captures under the slice's `proof/` directory.

## Acceptance and error paths

| Contract | Source and check | Verdict |
|---|---|---|
| AC-1/2, BR-4/5/6 | Record plus `Instant.now().toString()`; exact fields and time interval asserted by AC1/AC2 and controller unit test | pass |
| AC-3/4/8, BR-1/2/3 | UUID generated without reading headers; filter sets response header before the chain; shape, uniqueness and spoofed-id tests | pass |
| AC-5, BR-3/7 | Highest-precedence filter plus enabled platform ProblemDetail handler; POST returns 405 with id and no trace/class name | pass |
| AC-6, BR-8 | INFO event inside MDC lifetime; functional profile retains shipped ECS format; AC6 requires events and parses every matching line | pass |
| AC-7 | No input copied to logs; native exclusion of address-bearing thread metadata; AC7 canaries and regression assertion, plus QA live proof | pass |
| Failure cleanup | `finally` removes only `requestId`; throwing-chain unit test verifies cleanup | pass |
| Scope/territory | Single functional-profile build line and single granted production logging line; other changes within ping/web territory | pass |

## File ledger

| Changed file | Verdict |
|---|---|
| `build.gradle.kts` | pass — only the granted functional-profile activation line; existing coverage gate unchanged |
| `src/functionalTest/java/dev/urlshort/ping/PingJourneyTest.java` | pass — eight named AC tests, real MVC/filter/configuration, strict JSON and privacy checks |
| `src/functionalTest/resources/application.properties` → `application-functional.properties` | pass — three H2 overrides retained, blank logging override removed; shipped base now loads |
| `src/main/java/dev/urlshort/ping/PingController.java` | pass — one INFO event and exact response; no unnecessary layer |
| `src/main/java/dev/urlshort/ping/PingResponse.java` | pass — immutable two-field record |
| `src/main/java/dev/urlshort/web/RequestIdFilter.java` | pass — explicit order, server-issued id, header before chain, cleanup on success/failure |
| `src/main/resources/application.properties` | pass — precisely the approved native thread-name exclusion |
| `src/test/java/dev/urlshort/ping/PingControllerTest.java` | pass — status and current UTC instant, no Spring context |
| `src/test/java/dev/urlshort/web/RequestIdFilterTest.java` | pass — observable header/MDC lifecycle, uniqueness, inbound policy and throwing-chain cleanup |

## Ponytail review

Lean already. Ship.

No extra dependency/layer, hand-rolled JDK/Spring facility, speculative
flexibility or worthwhile shrink finding. Native profile loading, structured
logging exclusion, UUID, Instant, MDC and the record satisfy the contract.

## Findings

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| — | — | — | No code findings | None |

## Self-check

- Candidate matches QA, worktree clean, nine of nine changed files read.
- Fresh gate and JUnit/CSV contents inspected; QA evidence independently
  checked rather than accepting its PASS label alone.
- All ACs and error/privacy obligations checked against source; no change to
  product code, tests, locked SPEC or design.
- Ponytail lens applied with no manufactured findings. Existing settled
  DR-01 and QA-01 remain resolved; design-owner follow-up stays non-blocking.
- Ledger row records this exact candidate and verdict; handoff is to the
  security step, not a claim of integration or release approval.
