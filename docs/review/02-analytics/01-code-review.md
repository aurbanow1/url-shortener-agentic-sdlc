# Code review — 02-analytics

- Candidate: `862c52eea8294e438b1f98b832ae4f64f7a16923`, identical to QA and the clean `.worktrees/02-analytics` HEAD.
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003104116-a5a61dcb`; instance `01M40CP0JVZZWR8226MNN8XCCV`; combined code/security assignment.
- **Verdict: FAIL — one HIGH, CR-01: the midnight regression test fails when run alone because it races a real 1 ms expiry.** The fresh full gate passes; no production rotation failure is inferred from this test failure. Security review separately passes.

## Context proof

The slice counts successful GET redirects using reduced, private click facts and exposes anonymous aggregate statistics. It must preserve the stored-target 302, avoid clicks for HEAD/errors, keep database delay/failure off the redirect path, preserve link/audit history, and never expose client hashes or raw client values. A bounded single writer, daily in-memory HMAC salt, one grouped query and a small aggregate fold implement the approved design. Retention, uniques, rate limiting and proxy alignment are explicitly later work.

Read the locked SPEC (22 ACs, nine business rules), design and its resolved DR-01–DR-04 history, ADRs 0011–0013, AGENTS.md, docs/DESIGN.md, producer PROGRESS/PROOF, QA SUMMARY/traceability/gaps/captures, Java/Spring §6/§8, QA §2–§3 and review guidance. The selected project/mission/slice path has no additional deep review composition. Territory is respected, including the ordered OpenAPI grant. Confidence: intent 98/100; implementation and test coverage 97/100; release/performance claims 80/100 because the assigned numerical benchmark remains pending. No missing design decision blocks this review.

## Complete changed-file ledger

Range: `git diff main...slice/02-analytics`, 20 files, 2,214 insertions / 5 deletions. Read every full diff and every changed file; unchanged context of the three modified existing files was also read. Paths below are relative to the candidate worktree.

| File | Verdict / what was checked |
|---|---|
| `docs/api/openapi.json` | PASS — stats operation, four-field aggregate and examples; previous operations preserved; equal to the captured live document |
| `src/functionalTest/java/dev/urlshort/click/ClickRecordingJourneyTest.java` | PASS — exact row count/time, non-click paths, complete referrer/UA tables, clock/hash boundary, spoofed headers, aggregate/log privacy |
| `src/functionalTest/java/dev/urlshort/click/ClickResilienceJourneyTest.java` | PASS — real Tomcat slow/failing store, concurrent 200 clicks, correlated safe WARN, no HEAD body |
| `src/functionalTest/java/dev/urlshort/click/ClickSchemaJourneyTest.java` | PASS — all four classes accepted after pool retirement; scope honestly a smoke check |
| `src/functionalTest/java/dev/urlshort/click/StatsJourneyTest.java` | PASS — empty/retired stats, UTC grouping, exact top ten/ties, aggregate consistency, errors, audit invariance, API, HEAD/OPTIONS |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | PASS — expected path set extended; existing live-document equality retained |
| `src/main/java/dev/urlshort/click/Click.java` | PASS — URI parts reconstruct origin, bounded input, ordered UA reduction, immutable reduced record |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS — request-thread reduction, bounded queue, private correlated errors, per-click shutdown ownership and five-second drain |
| `src/main/java/dev/urlshort/click/ClickStore.java` | PASS — fixed parameterized insert/lookups/grouped read; no link/audit mutation |
| `src/main/java/dev/urlshort/click/DailySalt.java` | PASS — locked instant/key selection, HMAC-SHA256, guarded scheduled expiry, zero/drop on expiry and close; independent lifetime probe passes |
| `src/main/java/dev/urlshort/click/LinkStats.java` | PASS — total/day/referrer counts from the same grouped snapshot; ascending days and count-descending/origin-ascending top ten |
| `src/main/java/dev/urlshort/click/StatsController.java` | PASS — bounded code route, static 404, aggregates only; no retired-link exclusion |
| `src/main/java/dev/urlshort/click/package-info.java` | PASS — nullness boundary and feature responsibility documented |
| `src/main/java/dev/urlshort/link/RedirectController.java` | PASS — hook after successful resolution; unchanged stored Location/no-store; recorder suppresses HEAD |
| `src/main/resources/db/migration/V2__create_click.sql` | PASS — timezone-aware instant plus UTC day, foreign keys, hash-length constraint, link/day index; no broken H2 multi-value CHECK |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — failure/rejection/reduction paths, bounded stuck-store close, per-request reports and no duplicate later completion/failure |
| `src/test/java/dev/urlshort/click/ClickSchemaTest.java` | PASS — real Flyway/H2 constraint checks after DDL connection retirement, four allowed values and invalid inputs |
| `src/test/java/dev/urlshort/click/ClickTest.java` | PASS — normalization/invalid-origin/length/UA boundaries and reduced record fields |
| `src/test/java/dev/urlshort/click/DailySaltTest.java` | **HIGH CR-01** — standalone midnight test fails at line 73; other salt cases pass in fresh full gate |
| `src/test/java/dev/urlshort/click/LinkStatsTest.java` | PASS — empty and grouped folds, omissions, ordering and cap |

## Verification and audit of QA evidence

Commands ran from `.worktrees/02-analytics` through the pinned offline wrapper:

```sh
scripts/gw --log ../../docs/review/02-analytics/proof/code-check.txt --offline check
scripts/gw --log ../../docs/review/02-analytics/proof/salt-midnight-isolated-test.txt --offline test --tests '*DailySaltTest.aSelectionMadeBeforeMidnightKeepsItsDayAndNeverReplacesTheNextDaysSalt' --rerun-tasks
scripts/gw --log ../../docs/review/02-analytics/proof/code-check-rerun.txt --offline check --rerun-tasks
```

The first check was up-to-date and passed. The isolated test failed, with its original XML preserved in `proof/salt-midnight-isolated-test.xml`. The final full check executed all 14 tasks and passed: **121 unit + 126 functional invocations**, zero failures/errors/skips, Javadoc and coverage verification green. No toolchain failure. Product/test sources were not changed.

`proof/audit-qa-evidence.py` independently reconciles the committed raw QA evidence and the fresh review test reports; results are in `proof/qa-evidence-audit.txt`. It verifies 312 unique response ids and matching completion statuses, exact stored-target/no-store redirects, safe problem bodies, every captured stats body's shape/order/sums, 256 reduced rows including the exact 200 concurrent clicks, one correlated loss WARN among 314 events, unchanged 24 audit/22 link rows, and live/committed OpenAPI equality. QA reports are honest for the full-suite run; CR-01 exposes a separate order/timing dependency.

Coverage CSV sums match QA: unit 325/359 lines and 118/118 branches; functional 324/359 and 92/118; merged 359/359 and 118/118. Both QA and fresh merged XML roots count **358/358 distinct source lines**: one line is shared by classes in `web`, so per-class CSV summation counts it twice. This is no coverage discrepancy or exclusion. Build configuration retains 100% line/branch enforcement.

| Contract | Source/test evidence and assessment |
|---|---|
| AC-1–2; rules 1–2 | Recording journeys poll within five seconds and prove exact once for successful GET, no row for non-click paths; immutable reduction before enqueue |
| AC-3–6; rules 3–4 | Full origin/UA tables, two controlled addresses, UTC rotation, forwarding spoof and hash tests; **midnight unit proof needs CR-01 repaired**; independent scheduled disposal/close probe is green |
| AC-7–13; rules 6–7 | Empty/retired aggregates, exact counts/day boundaries/top-ten order, arithmetic invariants and safe 404/405 tested; one SQL snapshot avoids inconsistent independent count queries |
| AC-14–16; rule 5 | Real-server slow/failure/concurrency journeys; queue/rejection/shutdown unit cases; reduced facts survive request completion |
| AC-17–19; rule 9 | Four-field public aggregate, no hash/class, no client values in logs, server-generated correlation id including loss WARN; source plus fresh tests and audited raw QA logs |
| AC-20; rule 8 | Prior link/redirect suites rerun; audit insert-only tests still pass; QA before/after snapshots identical |
| AC-21–22 | Live document equals committed document, existing operations preserved; framework HEAD/OPTIONS semantics have dedicated cases |
| NFR-L3 numerical p95 | Explicit release benchmark/gap under proof item 13; slow-store structural test does not establish the ≤2 ms added-p95 number |
| Proof item 12 | Explicit salt lifecycle row and independent actual-class probe in `02-security-review.md`; QA retains ownership of the proof judgment |

## Ponytail review

Lean already. Ship.

This is the complexity-only result, not the merge verdict. No extra dependency or speculative service layer. JDK/Spring facilities handle URI, crypto, bounded execution, SQL, errors and API mapping. The per-click state exists to meet the previously demonstrated shutdown-accounting requirement; it earns its place. No complexity findings.

## Findings

| id | severity | file:line | evidence | required change |
|---|---|---|---|---|
| CR-01 | HIGH | `src/test/java/dev/urlshort/click/DailySaltTest.java:63` (assertion line 73) | The unchanged isolated test exits 1: expected day-D hash differs from delayed selection hash. It seeds at 23:59:59.999, calls `stamp()` (including HMAC initialization), then independently selects again. `DailySalt.java:63` schedules a real 1 ms expiry, which can drop the first key between those selections although the test clock remains fixed. Full-suite execution passes after earlier work has warmed the runtime. See saved log/XML. | Make the midnight ordering regression independent of that wall-clock race, while retaining assertions that a pre-midnight selection finishes with its own key and cannot replace D+1's key. For example, establish D's initial salt well before midnight before moving the controlled clock to the boundary. Preserve the separate real scheduled-expiry case. Run this test alone and the full offline gate on the repaired candidate; update QA's candidate/evidence. |

The shipped consequence is an unreliable privacy regression and a failing supported isolated test run, not demonstrated corruption of production hashes. The requested correction is confined to test setup. Do not remove the assertions, disable expiry, add a sleep to outrun it, or expand the product architecture.

## Merge readiness and self-check

**Not ready: return to implement for CR-01.** No new non-blocking findings. Existing NFR-L3 release measurement, later retention, A-9 proxy alignment, and the earlier slice's separate CR-01 ProblemDetail-schema backlog remain where the SPEC placed them; this report's CR-01 is local to 02-analytics.

Exact SHA verified, clean candidate, 20/20 files read, full gate freshly executed, defect reproduced and cited, QA captures/counters audited, both review files and both ledger rows recorded. Security has no blocking finding. All authored files are under `docs/review/`; no product, test, SPEC or design edits. Combined packet exits `failed` with this report as evidence.
