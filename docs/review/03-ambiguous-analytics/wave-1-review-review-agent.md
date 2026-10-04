# Mission 03 — accumulated wave 1 review

**Verdict: PASS for the assigned analytics wave, with two LOW documentation findings and three INFO observations from the structural vantage. No new MUST-FIX, HIGH or MEDIUM in this wave.** This is not a release-readiness judgment: the inherited mission-02 HIGH scheduling-test race, its assigned forward fix, and proof item 12 remain outstanding as described below.

Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-04 UTC. Packet `qitem-20261004002516-ee95930d`, instance `01M40RVNDQ0KT7FPWN1KJW0DC3`. Independent structural reviewer: `design2-agent@urlshort-factory`, [report](wave-review-design2-agent.md), commits `7c54ef7` and `9928513`; neither reviewer authored this slice. The original designer supplied register upkeep, not the independent structural judgment.

## Context and exact scope

The doghouse is two additional per-UTC-day statistics on the existing endpoint, consistent client identity behind operator-listed proxies, and recorded/lost click counters. No cross-day unique total, bot exclusion, new reader, retained identifier, schema change or new dependency. Existing redirects remain fail-open; existing totals/referrer ranking count the retained raw clicks. The salt's day and the stored day must agree; no raw client data may reach storage, responses, metrics or logs.

Reviewed the mission brief and human decision (Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A; live transition 876), all 15 slice ACs, locked design `80ca44c`, impact analysis, v1 SPEC/design, ADR-0011/12/13/15/16 and retention/audit context. Checked grants for the `web/` pair (18:36Z) and the audit-upgrade assertion (21:57Z), and AC-14's recorded ruling `a12a0e2`. Confidence is high for the source, HTTP/storage effects and gate at this boundary; release percentiles, future combined changes and online advisory checks are not inferred from them.

The assigned range is **`d55a502f14127ac234c01d12efb46dda3c1a1e64..94aa2c0720bdad876f7ca58e95ba01778dffbd23`**: 45 changed paths, comprising 18 product/test/API paths and 27 concurrent evidence/coordination paths. The actual product merge is **`c9b66dd`**; `94aa2c0` is the following documentary tip. Its second parent is X′ **`22fc8e2`**, rebased from the judged **`ec466da8da4b1efde9d612c6c8692070cc6fc4b9`** onto `d55a502`. [Own range-diff](proof/wave-range-diff.txt) independently confirms all eight patches identical. Every one of the 18 product paths is byte-identical across candidate, X′ and target. [Range log](proof/wave-range-log.txt), [45-file ledger](proof/wave-file-ledger-94aa2c0.md), [hashes and reconciliation](proof/wave-evidence-94aa2c0.json).

The fresh root gate began at **`18db1ded0ca96fdde80bc61f4609bdf927761513`**. Before/after source comparisons establish that its product, build, scripts and API files equal `94aa2c0`; later shared-checkout changes during this review were documents. The supplemental register/status commits `b45029b`, `15654b7`, `41eff65` are explicitly after the assigned boundary and do not relabel the tested product.

## Independent verification

- `scripts/gw --log docs/review/03-ambiguous-analytics/proof/wave-check-94aa2c0.txt --offline check --rerun-tasks`: **226 unit + 250 functional tests**, zero failures/errors/skips, all 14 tasks executed, Javadoc passed. [Actual gate log](proof/wave-check-94aa2c0.txt).
- The root build directory contained an old `designDogfoodTestProbe.exec`. I did not treat a merged wildcard report as proof of the two suites alone. [Review-only init](proof/canonical-coverage.gradle) reran the report and verification using only fresh `test.exec` and `functionalTest.exec`: **582/582 lines, 206/206 branches**. [Canonical verification](proof/wave-canonical-coverage-94aa2c0.txt). Individually, unit covers 508 lines/198 branches and functional 548/171; neither is claimed separately 100%.
- [Independent control](proof/ReviewWaveProbe.java), [init](proof/wave-probe.gradle), [run](proof/wave-probe-94aa2c0.txt), [complete HTTP captures](proof/wave-effects-94aa2c0.json): **136 checks, 42 actual loopback HTTP requests** across two separately started Tomcat/file-H2 contexts, default trust and explicitly trusted loopback proxy. Requests use `scripts/http`; both owned contexts close. This is a main-runtime probe, without a JaCoCo agent, not an extra coverage suite.
- The control records four redirects as four clicks/one bot, with one unique under default trust and three under configured trust; a forged leftmost forwarded hop has no effect. The next UTC day has its own unique count. Exact response fields, stored redirect target/no-store and aggregate privacy hold. V3 defaults populate click audit columns; V4 link stamps and complete link/audit snapshots survive analytics and deletion.
- An actual H2 constraint failure on a disposable link still answers `302`, adds no click, increments only `lost{reason="write failed"}` and emits a safe correlated WARN. Both contexts expose exactly the six click series (recorded plus five static reasons). After five successful inserts, deleting the old rows empties their raw/unique figures while the cumulative recorded meter stays five.
- The audit seam is checked in both trust configurations: forged forwarding on `/api/audit` returns `403 application/problem+json`; a bare direct loopback read returns `200`. Sharing the identity attribute does not rewrite the connection address or bypass the access guard.
- [Reconciliation script](proof/audit-wave.py) independently joins **42/42 full wire captures to exactly one request-completion event with matching UUID/status**, two write-failure events to their successful redirects, and two purge events to five deletions each. Client/referrer/UA canaries are absent from the product log. The live OpenAPI equals the committed target; semantic diff has only two daily fields, the statistics example and its success description.

Probe limits: its explicit UTC clock is a fixture; automatic purge is held and `runNow()` is called after advancing it, so it proves retention/analytics coexistence, not scheduling. It does not reproduce the inherited scheduling-test race or measure release latency. Its failure constraint exists only in disposable databases. Prior QA installed-directory, concurrency, original-suite and salt-lifecycle captures retain their original attribution.

## Claims against the accumulated source

| Contract | Judgment and evidence |
|---|---|
| FR-16 / six human choices | Decision predates substantive design; no extra product choice added. The only added reader fields are within each UTC day; neither bots nor unknown agents are excluded. |
| AC-1–5, rules 1–5/9 | PASS. One bound `UNION ALL` statistics statement takes the referrer/day totals and link/day `COUNT(DISTINCT client_hash)`/bot sum from one snapshot. `LinkStats` retains its old fold and adds the two day fields. Fresh `StatsV2JourneyTest`, `LinkStatsTest` and the live control verify counts, empty state, bot/browser identity and day separation. No hash joins, return field or cross-day comparison. |
| AC-6 | PASS. Existing raw totals, UTC grouping, top-ten tie ordering and consistency journeys remain in the fresh gate. Only their non-empty daily-shape assertions acquire the two agreed fields. Purge changes figures by deleting stored rows, not by a second aggregation policy. |
| AC-7–9, rule 6 | PASS. `RateLimitFilter` resolves once, sets its namespaced attribute before charging, and keeps `getRemoteAddr()` unchanged. Recorder hashes that attribute, falling back to the peer when absent. Source parser is unchanged; new unit/MockMvc journeys cover missing header/rightmost-untrusted cases, and the real trust/no-trust probe confirms the integration. Responses and stored reduced rows contain no raw addresses or canaries. |
| AC-10/11 | PASS. Constructor registers recorded and all five fixed loss reasons; successful inserts increment recorded immediately after return, and the existing one-event loss function increments the matching reason. Queue/close/reduction/write paths and late-commit accounting have fresh unit coverage; real failing JDBC and Prometheus effects are observed above. No client-derived labels. |
| AC-12 | PASS. Existing v1 correlation/privacy scenarios plus trusted-proxy scenarios pass; our independent wire/log joins and canary scan agree. The asynchronous writer retains MDC restoration. No new log event or error body. |
| AC-13 | PASS. Live export equality and full JSON semantic comparison preserve all other operations, including audit and the dogfood ProblemDetail correction. The statistics success example has all four daily fields. |
| AC-14 | PASS under the explicit `a12a0e2` reading. The original f6dd29e suite's two path/operation enumerations were separately authorized in audit-read; analytics adds neither. Read QA's original 153/155 literal result and authorized 155/155 replay, without calling it an untouched all-green literal run. Current full integrated suite passes. Audit-upgrade change is one granted shape line; retention's three shape additions are disclosed in PROOF, retaining every other assertion. |
| AC-15 | PASS for the in-suite contract: inherited slow/failing/concurrent store journeys run under default and configured proxy contexts, and actual failure remains a successful redirect in our control. No percentile claim: proof 12 waits for release_prep. |
| V3/V4, error and audit seams | PASS. Analytics introduces no migration/column writer. Named inserts/selects coexist with row-audit defaults, latest V4 startup and the granted V3-pinned tests. New V4 upgrade/atomicity/rollback journeys execute in the integrated gate. Error producers, request IDs, URL validation, SQL parameter binding, audit writes and exposure configuration are unchanged. |

## QA and proof custody

Read the coverage summary, traceability/gap qualifications, combined exact-candidate reviews and their reconciliation. They report 221/241 tests and 580/206 canonical coverage at `ec466da`, 372 report hashes, 1,601 archive files, 716 response/log joins per sink and 30 recomputed statistics bodies. Those are the prior reviewers' observations, not new counts from this wave. V4 adds the integrated test/line difference seen above. Source test mappings and the new suites support the assigned ACs; class-level attribution for parameterized XML rows remains qualified.

[Captured live proof](proof/live-wave-proof-94aa2c0.json) has **1–11 accepted, 12 pending, no issues**. Independently rehashed all **28 distinct evidence references** in current accepted receipts: all match. Item 11 explicitly covers same-day-only hash use and memory-only salt lifecycle; the reported transient key copies/timer/JVM-erasure limits remain. Item 6 was reaffirmed after all four audit-column gaps closed. Read the concurrent V4 closure receipts, their one-token QA-actor correction, and the actual migration/test custody: these documentary changes do not broaden analytics behavior. No new online dependency advisory check; dependency declarations are unchanged, and the existing offline inventory is retained.

## Cross-cutting register — all 13 concerns

Reviewed §11 at the assigned tip and the designer's finalization `b45029b`, status update `15654b7`, and follow-up `41eff65` against source. Design2's independent judgments agree; its requested register sentences are now present.

| Concern | Verdict at this product boundary |
|---|---|
| Client identity / proxy trust | Consistent. One parser, shared attribute, unmodified peer and explicit audit guard. Prior raw-peer drift W2-02/W2D-03 is closed here. Register now warns that future limiter-skipping redirects must still resolve the attribute; the future D21 shared-component slice is not pre-reviewed. |
| Time | Same application Clock and atomic salt/day selection. No zone option. Row-audit DB-clock exceptions are explicit; ping's direct-clock backlog remains separate. |
| Schema change | V1–V4; analytics adds none. V3 pins/grants preserved. Design vantage's LOW future V5 pinning risk remains with the V5 owner; it is distinct from review-agent's similarly named HIGH W2F-01. |
| Audit columns | All four tables comply after V4; analytics' named insert lets V3 defaults work. Link stamp is in its existing transaction per ADR-0020. Four GAPS rows closed with correct attribution. |
| Error shape | No new handler or error format. Same not-found, filter 429 and audit 403 behavior; matched API document. |
| Request ID / logging | Same request/writer events and UUID. Purge job has no request UUID. The inherited scheduling-test publication race remains open below. |
| Audit writes | No new writer. Statistics/redirect recording/purge preserve the contractual link audit trail; complete snapshot equality observed. |
| Client hashing | Only link/day distinct count; salt remains memory-only and unchanged. No client hash in response/log/metric. Same-day restart/backward-clock upper-bound limitation remains. |
| Metrics / health | Two click families, five pre-registered static reasons, no client tags; no new exposure. Recorded is cumulative even after deletion; late commit may appear in both recorded and lost as explicitly designed. |
| API document | One generator, ordered custody preserved, live equality checked. M3S-01 remains a small missing retention description. |
| CI/CD | No workflow or dependency change in this wave. Hosted runs at this merged SHA are not claimed. Inherited test race can still make CI intermittently red. |
| Background work | Same bounded queue/writer and purge executors. Writer can interrupt after five seconds; purge waits three and never interrupts. No new job/framework. |
| Operator settings | No new setting. M3S-02: trusted-proxies now affects uniques but its shipped comment says only rate limiting. Register records this, plus existing purge-hold README backlog. |

## Findings and dispositions

The primary source/effect pass adds no new defect. I agree with the structural vantage's two LOWs after checking the cited text, and retain its INFO constraints without manufacturing implementation work.

| ID | Severity / class | File:line / evidence | Disposition |
|---|---|---|---|
| M3S-01 | LOW / CONTEXT-GAP | `src/main/java/dev/urlshort/click/LinkStats.java:19`: totalClicks described as every recorded click; API property has no retention description; SPEC rule 7 says retained rows only. Our control observes five becoming zero after purge. | Backlog: describe retained rows/default retention in Javadoc and schema, regenerate the document under its next granted custody. Existing purge changed the meaning before this wave. |
| M3S-02 | LOW / JUDGMENT-GAP | `src/main/resources/application.properties:6`: trusted-proxy comment describes rate-limit effect, omits analytics identity; SPEC rule 6/operator story requires that shared meaning. | Backlog: one operator-facing comment documenting the unique-visitor effect, no key rename. Drift recorded at `41eff65`; comment repair still open. |
| M3S-03 | INFO | `src/main/java/dev/urlshort/click/ClickRecorder.java:116`: fallback when the filter attribute is absent. Current trusted-proxy and guard tests pass. | Register precaution completed in `41eff65`, independently checked by both vantages. No current behavior defect. |
| M3S-04 | INFO | `src/main/java/dev/urlshort/click/LinkStats.java:49`: day lookup relies on both grouped branches sharing one SQL snapshot; ADR-0013. | Record only. Any future split query must revisit snapshot consistency. No additional abstraction or fallback requested now. |
| M3S-05 | INFO | Integration gate log names X′ in the path without an embedded SHA; actual merge is `c9b66dd`, documentary tip `94aa2c0`. | Custody verified explicitly by this review's eight equal patches and product-tree equality. Future gate captures should record their execution SHA; no rerun/fix needed to validate this already-reconciled result. |

**Inherited shared HIGH, not resolved here:** mission-02 `W2F-01` (review-agent's ID), `ClickRetentionScheduleJourneyTest.java:87,96`, `ClickPurge.java:99`. Read the original failing XML/gate record, subsequent unchanged isolated pass, and the deterministic [publication control](../02-brownfield/proof/purge-publication-control.txt)/source: committed DELETE may be visible before its log event. Analytics changes neither file. Our green run does not disprove the race. One forward-fix custody: **`qitem-20261004003207-25b6f4c0`**, mission-02 review commit `cd88fd3`; require independent correction evidence and a fresh complete final merged gate before declaring the shared release ready. Do not duplicate or downgrade it into this wave's LOW backlog.

## Ponytail review

No over-engineering finding in the wave delta. It reuses the existing parser, bounded writer, grouped query and native Micrometer counters. No new dependency, job, persistence layer, index or speculative interface. The day-record fold is small and directly earns its shape from the response contract.

## Handoff and limits

Both vantages are recorded. Forward-disposition packet **`qitem-20261004004643-c5f63115`** routes M3S-01/02 to the lead's backlog; M3S-03 is completed register guidance, M3S-04 recorded intent, M3S-05 capture hygiene. No non-blocking code edit is required in passing to finish this wave review. Release must retain proof **12** (NFR-L1 bench or honest GAPS judgment) under `qitem-20261003195138-8eb72ecb`, the shared HIGH's fix, and the exact candidate chosen for release. **D21 explicitly says mission 03 proceeds independently; it does not wait for mission 02's `06-client-identity`.** If a later release candidate includes that refactor, its own review evidence applies; this report does not pre-accept it. This report is not that later review, a release approval, or proof of hosted CI on this SHA.

Self-check: exact target and QA-to-merge custody verified; all 45 changed paths accounted for; source/AC/grant claims checked; fresh mandatory gate and canonical coverage read; independent real HTTP/storage effects reconciled; all current receipt evidence hashes match; 13 register rows judged; both vantages recorded with scoped limits. Product, tests, SPECs and designs remain unedited by this reviewer. Review/ledger committed with explicit paths; lifecycle handoff carries the inherited blocker and remaining proof obligation.
