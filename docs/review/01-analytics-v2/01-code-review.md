# Code review — 01-analytics-v2

Candidate **ec466da8da4b1efde9d612c6c8692070cc6fc4b9**, reviewer `review-agent@urlshort-factory` (Codex), 2026-10-04 UTC. **PASS; no findings.** Combined code/security packet `qitem-20261004000110-d0d07fdb`; [security review](02-security-review.md).

## Context and coverage

The human chose per-UTC-day unique visitors, same-day-only use of the daily hash, and an additional bot count while keeping raw counts, retention and the existing API. This candidate adds those two daily figures, aligns click identity with the limiter's existing trusted-proxy rule, and counts recorded/lost clicks. It does not add a cross-day identity, new endpoint, migration or dependency. Confidence is high from the complete SPEC/design, source, fresh gate and independently reconciled effects.

Read the locked SPEC `b8c327b`, design `80ca44c`, impact analysis, AGENTS, DESIGN, applicable review/Java/QA/brownfield guidance, QA SUMMARY/PROOF/traceability/GAPS and relevant ADR decisions. The explicit audit-test shape grant and AC-14 ruling `a12a0e2` preserve the intended inherited checks. Baseline is `2566c38c2e8c434e93703a3dabc2edba14eca60c`; `git diff main...slice/01-analytics-v2` has **18 files, +816/-65**. Every changed file and the full diff were read.

## File ledger

Paths below are relative to the exact candidate worktree.

| File | Verdict |
|---|---|
| docs/api/openapi.json | PASS — only the two DayClicks properties and statistics description/example change; live document equals committed JSON. |
| src/functionalTest/java/dev/urlshort/audit/AuditUpgradeJourneyTest.java | PASS — granted exact per-day expectation gains the two figures; existing upgrade/row assertions retained. |
| src/functionalTest/java/dev/urlshort/click/ClickMetricsJourneyTest.java | PASS — real management surfaces observe stored/failed click deltas and bounded label families. |
| src/functionalTest/java/dev/urlshort/click/ClickRecordingJourneyTest.java | PASS — aggregate-only exact daily shape updated; privacy/rotation and other recording assertions retained. |
| src/functionalTest/java/dev/urlshort/click/ClickResilienceTrustedProxyJourneyTest.java | PASS — reuses all five existing resilience journeys under the named-proxy configuration. |
| src/functionalTest/java/dev/urlshort/click/ClickRetentionJourneyTest.java | PASS — only expected daily figures updated; purge boundary, row preservation and concurrency assertions retained. |
| src/functionalTest/java/dev/urlshort/click/StatsJourneyTest.java | PASS — exact day shapes gain fields; totals, ordering, errors, HEAD/OPTIONS and audit effects retained. |
| src/functionalTest/java/dev/urlshort/click/StatsV2JourneyTest.java | PASS — shape, empty result, repeated clients, midnight, bot classes, mixed client and default forwarding cases. |
| src/functionalTest/java/dev/urlshort/click/TrustedProxyClickJourneyTest.java | PASS — right-most untrusted hop, proxy fallback, private storage/response/log effects. |
| src/main/java/dev/urlshort/click/ClickRecorder.java | PASS — uses the shared request attribute; fixed metric reasons; successful insert counted before report-ownership CAS, including late completion. |
| src/main/java/dev/urlshort/click/ClickStore.java | PASS — one parameterized UNION ALL snapshot; distinct hashes grouped only within clicked_on; no raw hash returned. |
| src/main/java/dev/urlshort/click/LinkStats.java | PASS — existing total/referrer fold preserved; matching per-day figures added without speculative fallback. |
| src/main/java/dev/urlshort/click/StatsController.java | PASS — existing handler/error behavior; new result and schema example wired correctly. |
| src/main/java/dev/urlshort/click/package-info.java | PASS — documents the permitted hash use and ownership. |
| src/main/java/dev/urlshort/web/RateLimitFilter.java | PASS — computes identity once and exposes it through a request attribute; leaves connection peer and limiter logic unchanged. |
| src/test/java/dev/urlshort/click/ClickRecorderTest.java | PASS — all reason counters, success/failure and shutdown claim/late insert behavior; existing ownership assertions retained. |
| src/test/java/dev/urlshort/click/LinkStatsTest.java | PASS — empty, ordering, summed totals and unchanged top-ten ties plus added daily figures. |
| src/test/java/dev/urlshort/web/RateLimitFilterTest.java | PASS — shared identity agrees with charged client; exempt request and unchanged connection peer checked. |

## Acceptance and empirical verification

| Contract | Source and observed proof | Result |
|---|---|---|
| AC-1–6: daily schema, uniques, UTC boundary, bots and v1 figures | StatsV2JourneyTest, StatsJourneyTest, LinkStatsTest; independently recomputed retained day/unique/bot/referrer values from DB snapshots. No top-level combined unique count. | PASS |
| AC-7–9: proxy/default identity and aggregate privacy | RateLimitFilter and ClickRecorder share the resolved client without rewriting getRemoteAddr; trusted/default HTTP journeys; retained trusted 4-click/3-identity case and default varied-header case; whole-run canary checks. | PASS |
| AC-10–11: counters and scrape privacy | ClickRecorderTest and ClickMetricsJourneyTest; both controlled QA contexts show recorded +3, lost +2, exactly five static reason series and an untagged recorded series. | PASS |
| AC-12: logs/correlation | Fresh privacy journeys; all 713 curl responses and three direct HEAD wire responses joined to exactly one completion in each of console and file sinks. Failure WARNs correlate; private canaries and stored hashes absent. | PASS |
| AC-13: API document | Fresh live/committed equality and explicit schema/example assertions; independently verified all other operations unchanged. | PASS |
| AC-14: inherited behavior | Fresh complete candidate suite plus audited original replay: literal 153/155 retained, authorized repeat 155/155 under a12a0e2. The two inherited audit operation enumerations and permitted day shapes are explicitly disclosed. | PASS |
| AC-15: slow/failing/concurrent recording | Both fresh functional contexts pass; QA physical H2 trigger controls retain 20 redirects below 250 ms with a two-second insert delay and 200 concurrent redirects/rows per context. These are resilience effects, not percentile measurements. | PASS |

I ran `scripts/gw --offline clean check --rerun-tasks` through the wrapper's `--log` option in the exact worktree: **BUILD SUCCESSFUL**, 15 tasks executed, **221 unit + 241 functional**, zero failures/errors/skips. [Gate log](proof/code-check-ec466da.txt). Fresh CSVs independently total **580/580 lines, 206/206 branches**; unit 506/580 and 198/206, functional 546/580 and 171/206. Only `test.exec` and `functionalTest.exec` exist after the clean gate; no old replay execution data or exclusions inflate coverage. Javadoc is in the gate.

[Independent reconciliation](proof/evidence-reconciliation-ec466da.json), using [this review's script](proof/reconcile-ec466da.py), verifies 372 saved report hashes, 1,601 original archive members, installed-jar provenance, 716 response/log pairs in each sink, 30 complete statistics bodies recalculated from stored rows, read-only snapshots, purge results, counter deltas and fixed scrape labels. All 282 source/context method names exist; all 462 QA invocation records match their saved XML, and fresh invocation identities match after normalizing only generated request-builder identities and privacy canaries. The 41 parameterized methods' class-group attribution remains qualified in QA, not represented as exact source-method joins.

The QA-only launcher supplies a controlled clock, Servlet peer and actual H2 insert trigger without editing candidate classes. Separate installed-jar captures exercise the unmodified service. This review audits those retained captures; it does not claim a second independent production-jar load run. Initial QA instrument mistakes remain disclosed. My reconciliation initially joined post-purge polls to the pre-purge snapshot; correcting that observation phase made the recorded values agree, without a product change.

## Ponytail review

No findings. The diff reuses the existing limiter, recorder, SQL/fold and Micrometer counters. The extra UNION branch buys same-statement consistency; the two local result records describe its actual rows. No new dependency, speculative layer or duplicate proxy parser. Existing explicit ceilings remain accepted intent.

## Findings

None: MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fix-in-passing or backlog item introduced.

## Verdict and handoff

**Code PASS and security PASS on the exact candidate.** [Live proof snapshot](proof/live-proof-ec466da.json): items 1–10 accepted on this SHA, all 32 referenced evidence hashes match; items 11 and 12 pending by the explicit a12a0e2 sequencing. The security record supplies item 11's construction evidence for QA judgment **before integration**. Item 12's redirect p95/p99 remains at release_prep, with the current NFR-L1 gap retained. These are not claims of full proof completion.

Restart may overcount same-day visitors; pre-v2 proxy hashes cannot be split retrospectively. The existing trusted-proxy configuration boundary, asynchronous loss semantics, H2-only observations and offline advisory limitation remain as designed.

Self-check: exact SHA and clean worktree checked; 18/18 complete files reviewed; fresh clean gate and upstream evidence audited; no findings manufactured; both verdict rows appended to the shared ledger. Only docs/review paths authored; no product/test, SPEC/design, QA artifact, release or publishing mutation.
