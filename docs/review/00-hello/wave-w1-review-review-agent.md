# Wave w1 — integrated outcome review

**PASS for release preparation.** Both vantages are recorded; no MUST-FIX,
HIGH or MEDIUM findings. Two LOW forward-looking observations from the
structural review are routed to the orchestration lead. Mission ship
sign-off and release checks remain outstanding.

Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003015049-51d1ba00`.
Lifecycle: `01M3Z8AJ1EDFTHQ1HPAWPNP2YN`.
Subject: merge `42a25db4a9c24fba3221c1ade4044719cab39ee3` on `main`.
Accepted source candidate: `f286a10863e4a8081235226f2d56e51ac121b319`.

## Context and scope

The mission is one ping endpoint carried through the factory, with a minimal
request-id mechanism and inspectable proof. It does not introduce URL
shortening, persistence, authentication, access logging or tracing. The
human's two recorded plan-lock decisions constrain the filter to issuing
the id, exposing the header and placing the id in MDC. Confidence is high
for this bounded HTTP/logging outcome; this is not a completed release claim.

Read the wave history (`git log --oneline f43ecd1..42a25db4`, then the
documentation-only commits through `2bd7c68`), the complete assigned product
diff and every one of its nine files. The packet scopes this review to
`git diff f43ecd1 42a25db4 -- src build.gradle.kts`, plus the mission/slice
contract and evidence. The repository-wide range also contains factory
configuration, generated reports and later mission scaffolds; this review
does not claim an independent audit of all 255 paths in that broader stat.

Inputs include mission SPEC/NOTES, slice SPEC/design/PROGRESS/PROOF, current
system design and ADRs, QA coverage summary, traceability, gaps, captures and
acceptance receipts. The source is judged directly against these contracts;
the earlier code/security reviews provide history, not substitute approval.

## Merge and fresh verification

- Merge parents are `3e4f5732a8d14a10dd6a5813b6a52e0195f3475f` and the
  accepted candidate `f286a10863e4a8081235226f2d56e51ac121b319`.
  `slice/01-ping/accepted` resolves to the merge. Product/test/build diffs
  from candidate to merge and merge to current main are empty.
- Ran `scripts/gw --log docs/review/00-hello/proof/wave-w1-check-42a25db4.txt --offline check --rerun-tasks`
  on main at `b54ac9a`: exit 0, 20 seconds, all 13 tasks executed.
- Inspected generated JUnit XML: 6 unit and 9 functional tests, zero
  failures/errors/skips. All 15 methods appear in the current traceability
  table. The design agent also reran the unchanged main inputs; generated
  reports are shared, so their ownership is not exclusive to my run.
- All three current CSVs match the committed QA candidate CSVs byte for
  byte. Unit lines 15/15, functional lines 13/15, merged lines 15/15; no
  branches exist. The merged threshold passes without exclusions. The
  mission's shorthand coverage wording is supported by the locked slice's
  explicit merged gate, not by claiming 100% functional-only coverage.
- Live `rig proof show` reports `ready`, no issues, seven accepted items;
  receipts 8–14 are attributed to QA against the exact merge subject.
  Audit at main `2bd7c68` again confirmed unchanged tested inputs.

Evidence: [own gate log](proof/wave-w1-check-42a25db4.txt),
[XML/CSV and input audit](proof/wave-w1-audit-42a25db4.txt),
[proof-readiness projection](proof/wave-w1-proof-readiness.json),
[QA acceptance](../../../missions/00-hello/slices/01-ping/proof/qa-acceptance-42a25db.md).

## Claim-by-claim result

| Claim | Source and independent evidence | Result |
|---|---|---|
| AC-1 exact JSON body and AC-2 current UTC time | `PingController.java:17`, `PingResponse.java:4`; named AC1/AC2 functional methods and controller unit test pass. Saved live GET time `00:43:57.282696Z` is within recorded `00:42:55.380979Z`–`00:43:57.417878Z` bounds | PASS |
| AC-3 valid id and AC-4 consecutive uniqueness | `RequestIdFilter.java:31` generates UUID per call and sets header before chain; AC3/AC4 plus filter unit tests pass; saved first and second QA response ids differ | PASS |
| AC-5 wrong method remains a safe problem detail with id | `application.properties:10` enables Spring handler; filter precedes handler; AC5 passes. Saved POST and prior security capture show 405 problem+json, header and no exception/trace | PASS |
| AC-6 one-line JSON event with matching id | `PingController.java:18` emits constant message, filter puts MDC id; shipped ECS format remains the functional-test base. AC6 parses actual captured lines. Saved live event id equals response `0c1ecb83-71a0-4a98-af62-9df9a5c5a6e0` | PASS |
| AC-7 no client address or UA; AC-8 inbound id ignored | No inbound values are read by filter/controller. Shipped exclusion at `application.properties:16`; AC7/AC8 pass. QA's loopback re-capture and independently inspected security capture contain neither canaries nor checked addresses | PASS |
| Request context does not leak into later work | `RequestIdFilter.java:38` removes its MDC member in finally; success and throwing-chain unit tests pass | PASS |
| Proof describes what was exercised | All eight AC methods, all eight business rules and 15 methods mapped; coverage differences, zero-branch result and historical QA-01 retained in GAPS. Seven attributed merge judgments ready | PASS |
| Scope remains one small endpoint | No new dependency, data access, migration, service layer, tracing machinery or generic request logger | PASS |

The saved wire/log artifacts were inspected; this wave pass did not start
another server. Fresh functional journeys plus unchanged inputs connect the
earlier real-server observations to the merge. Boot jar behavior and current
dependency advisories remain release checks. Async dispatch, HEAD/OPTIONS,
load and behavior outside the locked ping surface are not claimed here.

## File ledger

Paths below are relative to repo root; renamed properties count as one diff entry.

| File | Verdict and reason |
|---|---|
| `build.gradle.kts` | PASS — single functional-profile activation; no dependency or coverage weakening |
| `src/functionalTest/java/dev/urlshort/ping/PingJourneyTest.java` | PASS — eight observable AC journeys; emitted JSON inspected, privacy canaries and regression exclusion checked |
| `src/functionalTest/resources/application.properties` → `application-functional.properties` | PASS — datasource-only overlay preserves shipped logging and error settings |
| `src/main/java/dev/urlshort/ping/PingController.java` | PASS — constant log event and current UTC response only |
| `src/main/java/dev/urlshort/ping/PingResponse.java` | PASS — exact two-field record |
| `src/main/java/dev/urlshort/web/RequestIdFilter.java` | PASS — fresh server id, header before chain, narrowly cleaned MDC |
| `src/main/resources/application.properties` | PASS — native exclusion implements the recorded privacy decision |
| `src/test/java/dev/urlshort/ping/PingControllerTest.java` | PASS — status and bounded current time behavior |
| `src/test/java/dev/urlshort/web/RequestIdFilterTest.java` | PASS — id lifecycle, inbound distrust and exception cleanup |

## Two vantages and dispositions

The designated [structural vantage](wave-w1-review-design-agent.md), committed
at `7502b2d` with evidence wording clarified at `2bd7c68`, also passes. Its
author disclosed authorship of the design and its corrections; its judgment
is on the builder's integrated product, not independent approval of those
authored documents. I independently checked the corrected source/configuration
against the locked SPEC. Request `qitem-20261003015306-f1842acf` is closed.

| ID | Severity | Location | Evidence and consequence | Disposition / required change |
|---|---|---|---|---|
| W1-01 | LOW | `docs/guidance/architecture.md:42`; `docs/api/openapi.json` absent | Later guide expects a committed OpenAPI artifact; locked ping SPEC excludes asserting this endpoint in OpenAPI. No unmet ping AC | CONTEXT-GAP — lead assigns export ownership in future API work; no current product fix |
| W1-02 | LOW | `src/functionalTest/java/dev/urlshort/ping/PingJourneyTest.java:34` | MockMvc omits Tomcat metadata; historical QA-01 demonstrated the difference. Required real-server capture caught it and the accepted fix is tested | JUDGMENT-GAP (design choice) — consider a small real-server journey with the next log-privacy AC; no current proof gap |

These are consolidated structural observations, not two additional primary
defects. They are routed in `qitem-20261003020105-13e2c995` with
[follow-up instructions](wave-w1-followups.md). The structural INFO notes
retain the intentional ECS-path regression assertion and existing unit-profile
backlog; neither is reopened or counted as a new defect.

Historical DR-01 (test configuration shadowing) and QA-01 (container thread
metadata in logs) remain fixed JUDGMENT-GAPs: the contract already required
the properties and privacy behavior; the original mechanisms missed them.
The post-lock logging exclusion is an explicitly recorded lead decision,
not silent scope drift. The formerly open ADR/system-design reconciliation
was completed in `23f7a8c`; current `docs/DESIGN.md:135` and ADR-0004 describe
the exclusion. The locked design example remains historical. No forward-fix
slice is required to repair the currently accepted ping behavior.

## Ponytail review

Lean already. Ship. The three small production types each serve the locked
outcome, and JDK/Spring facilities supply IDs, time, errors and structured logs.

## Self-check

- Recovered live identity and the assigned packet; destination queue returned
  one active obligation, below the 10,000-item limit, with no truncation.
- Read all nine assigned files and their full diff; stated the broader
  repository-range limit rather than claiming unread factory files.
- Checked exact candidate/merge/main input equivalence, reran the full gate,
  inspected XML/CSV results and traceability, and queried attributed proof.
- Read the independent structural report and separated its disclosed authored
  corrections from product judgment; both vantages now recorded.
- Kept settled findings closed, classified and routed the two LOW follow-ups,
  and distinguished existing backlog and release work from current defects.
- No product/test/SPEC/design edits, no push, no release tag or publication.
  Review ledger appended; handoff evidence is this report.
