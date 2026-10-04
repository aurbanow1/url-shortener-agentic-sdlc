# Release preparation — mission 03: analytics v2

Prepared by `release2-agent@urlshort-factory`, 2026-10-04 UTC, under
`qitem-20261004004828-05d2aab9`. Product candidate:
**`50ad9c3ab9e65baa4100ede1772b514322957fa5`**. This package prepares a local
artifact approved for local use by the human; nothing has been published.

## 1. Decision brief for ship sign-off

An Analyst gets `uniqueVisitors` and `botClicks` within each UTC-day entry of
`GET /api/links/{code}/stats`. Raw clicks include bots; all figures use retained
rows. The limiter and click hashing share the existing trusted-proxy rule.
The Operator gets recorded/lost click counters with five fixed loss reasons.
There is no new reader, persistent identity or migration. The
[SPEC](slices/01-analytics-v2/SPEC.md), [wave review](../../docs/review/03-ambiguous-analytics/wave-1-review-review-agent.md)
and [installed results](release/installed-trusted-50ad9c3.json) support this scope.

The exact candidate includes the reviewed W2F-01 test repair merged at
`50ad9c3`; the analytics product merge is **`c9b66dd`**, whose second parent is
`22fc8e2`. `94aa2c0` was the subsequent documentary tip, not the merge to revert.
D21 (`06-client-identity`) is outside this candidate and imposes no dependency
on mission 03. [Wave custody and residual resolution](../../docs/review/03-ambiguous-analytics/wave-1-review-review-agent.md).

The human's mission plan-lock on `qitem-20261003114944-9bd32a00` was:

> approve: one slice 01-analytics-v2 in one wave; park the analytics questions on me before design

Transition 830, 2026-10-03T15:40:19Z; the [SPEC stamp](SPEC.md) was recorded
on behalf of the human at 15:40:43.879Z. The slice's delegated plan-lock stamp
is 20:02:59.400Z in its [SPEC](slices/01-analytics-v2/SPEC.md), under D11.
The human then resolved the ambiguity packet `qitem-20261003154347-19e96a75`
at 16:10:30.782Z, transition **876**:

> accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A

[Plan-lock transitions](../../docs/evidence/03-ambiguous-analytics/packets/qitem-20261003114944-9bd32a00.transitions.json),
[ambiguity transitions](../../docs/evidence/03-ambiguous-analytics/packets/qitem-20261003154347-19e96a75.transitions.json),
[recorded decision](slices/01-analytics-v2/SPEC.md#decision-recorded-verbatim).

| Question | Chosen behavior | Rejected alternatives |
|---|---|---|
| Q1 B: count | Daily uniques beside raw clicks, all user-agent classes | A raw-only; C weekly/monthly uniques |
| Q2 B: hash use | Same-day distinct counts only; never expose/export/join/compare across days; memory-only daily salt | A no hash use; C longer-lived identity |
| Q3 B: bots | Extra daily bot count; bots still in raw, unique and referrer figures | A no bot figure; C exclude bots |
| Q4 A: retention | Existing 90-day delete policy; retained-row figures | B different period; C indefinite daily rollups |
| Q5 A: reader | Existing statistics API | B CSV; C HTML dashboard; D cross-link view |
| Q6 A: day | UTC calendar days | B timezone parameter; C operator-selected zone |

**Reviewed proof snapshot:** the [post-QA attributed readiness](release/proof-readiness-after-qa-50ad9c3.json)
is **ready: all 12 items accepted, no issues**. QA's
[receipt 13](slices/01-analytics-v2/proof/judgments/00000013.md) accepts item 12
and [receipt 14](slices/01-analytics-v2/proof/judgments/00000014.md) reaffirms
item 6, both against `50ad9c3`. The [restored-seat QA return](slices/01-analytics-v2/proof/qa-release-50ad9c3/QA.md)
and [raw audit](slices/01-analytics-v2/proof/qa-release-50ad9c3/audit.json)
support the bounded NFR-L1 closure and current GAPS qualifications (§§3/7).
The [original prep readiness](release/proof-readiness-50ad9c3.json), with
items 1–11 accepted and item 12 pending, and the earlier
preparation validation remain unchanged as history. The mutable
[exported proof](../../docs/evidence/03-ambiguous-analytics/proof-readiness.json)
now contains the final reaffirmation described below. QA returned through
`qitem-20261004020825-9237265b`, separately from global
`qitem-20261003195138-8eb72ecb`, which retains mission01/02 drift on their
final main. Mission03 does not wait for D21. No self-judgment is substituted.
[Sequencing record](NOTES.md), [receipt/hash verification](release/post-qa-verification-50ad9c3.json).

**Final release review:** [PASS at `93d55bd`](../../docs/review/03-ambiguous-analytics/release-review.md#focused-re-review--14815f9)
for package `14815f9` and product `50ad9c3`. The reviewer independently checked
all 41 current evidence references across 33 committed files and the final
five-file delta. This is a package verdict, not human ship approval.

**Evidence change captured after review:** the retained [readiness read](release/proof-readiness-shared-doc-drift-50ad9c3.json)
was **unknown: items 2, 5 and 6 needed reaffirmation**, while the other nine
remain accepted. QA2 appended D21 sections to shared `TRACEABILITY.md` and
`GAPS.md`, changing their whole-file hashes; those edits were uncommitted
on this read. The earlier ready snapshot and final review remain historical
evidence for the pinned candidate. Per the lead's transition 1917, independent
QA reaffirms the changed references once immediately before final
`evidence_export`, against main as it then stands. The human decides on the
reviewed immutable snapshot. No product regression or D21 product dependency
is inferred; no new gate or product review is required.
The later slice delivery stamp also moved item 1's SPEC hash: only
`approved-by`/`approved-at` frontmatter was added. The same pre-export QA
item includes that stamp drift; the acceptance text is unchanged.

**Final pre-export proof:** QA committed the reaffirmation at **`0e125ca7`**:
receipts [15](slices/01-analytics-v2/proof/judgments/00000015.md),
[16](slices/01-analytics-v2/proof/judgments/00000016.md),
[17](slices/01-analytics-v2/proof/judgments/00000017.md) and
[18](slices/01-analytics-v2/proof/judgments/00000018.md) reaffirm items 2, 5,
6 and 1 against `50ad9c3`. [QA's record](slices/01-analytics-v2/proof/qa-final-reaffirmation-50ad9c3/QA.md)
and [receipt verification](slices/01-analytics-v2/proof/qa-final-reaffirmation-50ad9c3/receipt-verification.json)
support **ready: 12/12 accepted, no issues** in the final exported proof.
Release independently checked all **45 references across 35 files** against
current bytes and committed `0e125ca7`, including the final GAPS and stamped
SPEC hashes. [Final validation](../../docs/evidence/03-ambiguous-analytics/final-validation.json).
QA packet `qitem-20261004024232-5aea9029` closed before export; its corrected
closure names `0e125ca7`, replacing an erroneous placeholder. Earlier pending,
ready and drift snapshots retain their original bytes. No benchmark or
product gate was repeated for this document reaffirmation.

**Human approval:** existing gate `qitem-20261004023723-f58044d0`, handled by
the primary `release-agent`, was resolved by `human@kernel` at
**2026-10-04T02:38:55.409Z**, transition **1916**:

> approve: ship mission 03 analytics v2 at 50ad9c3 for local use; the exact-SHA hosted CI gap is accepted because the delta from the CI-verified 18db1de is one test-only change

[Decision transitions](release/ship-signoff-transitions.json),
[verbatim decision and delivery-stamp record](NOTES.md#release-agent--human-ship-sign-off).
This records the human's reason; it does not establish hosted CI/CD runs for
`50ad9c3`. The reviewed immutable package is `14815f9`, final review `93d55bd`.
Delivery stamps are handled by the primary release seat and lead on the
human's behalf; release2 makes no duplicate stamp or gate.

**Recommended default presented to the human:** approve this pinned pre-D21 candidate for local use, retaining
§7's limits. Exact-candidate hosted CI/CD is unverified and must be visible
in the human's decision. The alternative is to hold for a human-triggered
hosted CI/CD record, costing another external verification cycle. Rollback is
the rehearsed revert of `c9b66dd`, retaining V3/V4 and the data (§8). Nothing
has been published. The recorded human decision accepts the hosted-CI
gap for local use; the qualification itself remains in this package.

## 2. Artifact and gate

| Artifact | Identity / evidence |
|---|---|
| Product candidate | `50ad9c3ab9e65baa4100ede1772b514322957fa5`; [source comparison](release/candidate-source-diff.txt) shows product/build/runtime scripts unchanged at documentary HEAD `8d3c536` |
| Preserved installed jar | `/private/tmp/urlshort-mission03-50ad9c3.jar`; SHA-256 `fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2` |
| Local image | `urlshort:mission03-50ad9c3`; ID `sha256:277703a6510ec2924fef9b423c0f48714c081fd2fe9da76461f54d702afd4b3b` |
| Image provenance | `docker build -t urlshort:mission03-50ad9c3 .` exited 0; [inspect](release/image-inspect-50ad9c3.json); [jar comparison](release/jar-image-comparison-50ad9c3.json) finds identical whole-jar hash and ZIP entries |
| Toolchain | Gradle 9.8.0, Homebrew Java 21.0.10, macOS 15.2 aarch64 ([version capture](release/gradle-version.txt)); Docker client 29.2.0 / server 28.4.0 ([Docker capture](release/docker-version.json)) |

[Artifact manifest](release/artifact-manifest-50ad9c3.json). The shared root
jar may be rebuilt by another mission; this record identifies the preserved
bytes actually smoked. Image cache use is disclosed; no wholly uncached
build or registry digest is claimed.

Fresh root gate and artifact command:

```sh
scripts/gw --log missions/03-ambiguous-analytics/release/check-50ad9c3.txt --offline check bootJar --rerun-tasks
```

**BUILD SUCCESSFUL**, 16 tasks executed: **226 unit + 250 functional = 476**
invocations, zero failures/errors/skips. The fresh isolated JaCoCo verification reports **581/581 bundle lines and
206/206 branches**, 100% each. The archived class-row CSV sums are 582/582
lines: RequestBodyLimitFilter.java has one line shared by nested classes
(28 class-row lines, 27 distinct source lines). Unit 508/582 lines and 198/206
branches, functional 548/582 and 171/206 are informational class-row sums.
[Gate log](release/check-50ad9c3.txt), [summary and execution-source custody](release/gate-summary-50ad9c3.json),
[merged CSV](release/coverage-all-50ad9c3.csv), [unit CSV](release/coverage-test-50ad9c3.csv),
[functional CSV](release/coverage-functionalTest-50ad9c3.csv).
Release additionally re-reported and verified coverage using only
`test.exec` and `functionalTest.exec`, excluding the old probe execution file:
[isolated verification](release/coverage-canonical-50ad9c3.txt),
[CSV](release/coverage-canonical-50ad9c3.csv), [XML](release/coverage-canonical-50ad9c3.xml),
[input hashes and denominator reconciliation](release/coverage-canonical-50ad9c3.json).
The suite tasks were up-to-date after the fresh gate on the unchanged product;
only report/verification tasks re-executed. The [wave canonical record](../../docs/review/03-ambiguous-analytics/proof/wave-canonical-coverage-94aa2c0.txt)
retains its own 582 class-row attribution. There is no coverage deficit or
threshold change; the differing denominator counts the same covered source.

| Exact-candidate hosted run | URL | Conclusion |
|---|---|---|
| CI on `50ad9c3` | Unverified | GitHub Actions API query returned HTTP 404; no successful or missing run inferred |
| CD on `50ad9c3` | Unverified | Same inaccessible query; no artifact download/job conclusion observed |

[Exact-SHA query and response](release/github-runs-50ad9c3.txt).
Earlier `05-ci-cd` operator-recorded green runs concern `a3d6867`, not this
candidate. They remain historical evidence, not this package's CI/CD proof.
This is an explicit §7 gap under [CI/CD guidance §5](../../docs/guidance/ci-cd.md#5-release-package).

## 3. Installed smoke and performance

Both artifacts ran against disposable file H2 databases, with V1–V4 applied,
and only loopback published/bound ports. [Captured configuration](release/container-inspect-50ad9c3.json)
and [startup/shutdown log](release/container-log-50ad9c3.jsonl).

| Run | Observed / evidence |
|---|---|
| Jar environment journey, port 18230 | `scripts/smoke.sh --jar /private/tmp/urlshort-mission03-50ad9c3.jar 18230 missions/03-ambiguous-analytics/release/jar-env-log-50ad9c3.jsonl`: health/readiness, ping, create, redirect, read, statistics, 400/404/410/405, retirement, metrics and OpenAPI passed; public-base override, file DB, create budget 10, trusted loopback and 429 at the next admission observed. [Smoke](release/smoke-env-50ad9c3.txt), [log](release/jar-env-log-50ad9c3.jsonl) |
| Preserved jar, trusted loopback proxy, port 18230 | Four redirects from three clients, one bot: **4 raw / 3 unique / 1 bot**. Origin-only referrer; no input privacy canary in aggregates; recorded delta 4, five lost reasons zero, live OpenAPI equal. [Result](release/installed-trusted-50ad9c3.json), [raw exchanges](release/http-trusted-50ad9c3/stats-0.txt), [request/log reconciliation](release/installed-reconciliation-50ad9c3.json) |
| Container, default proxy trust, port 18231 | `scripts/smoke.sh http://127.0.0.1:18231` passed. Four varied XFF inputs from the same TCP peer give **4 raw / 1 unique / 1 bot**, recorded delta 4, lost zero, API equal. [Smoke](release/smoke-container-50ad9c3.txt), [result](release/installed-default-50ad9c3.json), [raw exchanges](release/http-default-50ad9c3/stats-0.txt) |

Container `33631c91e696b89de9556c25919813d02d18c7f418c559b81d6a4284cdcda421`
ran as `urlshort`, read-only root, one writable data volume plus `/tmp` tmpfs,
`127.0.0.1:18231:8080`, stop timeout 20s. The run used `docker run`, not a new
compose-lifecycle proof. [Inspect](release/container-inspect-50ad9c3.json).
Jar PID 46435 and rollback PID 42231 stopped with SIGTERM; container and its
disposable volume were removed; both ports refuse connections afterward.
Logs include graceful shutdown and pool close. [Teardown](release/teardown-50ad9c3.json),
[port checks](release/ports-after-teardown.json), [complete jar log, gzip](release/jar-installed-log-50ad9c3.jsonl.gz).
The initial sandbox bind refusal is retained as an instrument failure:
[rejected launch](release/jar-installed-sandbox-rejected-50ad9c3.jsonl).

**NFR-L1:** `scripts/smoke.sh --bench http://127.0.0.1:18230`, three open-loop
60s phases with budgets raised to 1,000,000/minute. Latency is measured from
the scheduled due time to response completion, including scheduling delay.
[Full benchmark](release/bench-50ad9c3.txt), [load method](../../tools/bench.mjs).

| Phase | Requests / achieved rate | p95 | p99 | Bad responses |
|---|---|---:|---:|---:|
| Redirect GET with 20 creates/s | 6,000 / 100.0/s | 3.7ms | 9.5ms | 0 |
| Create with 100 redirects/s | 1,200 / 20.0/s | 4.5ms | 12.9ms | 0 |
| Redirect GET alone | 6,000 / 100.0/s | 3.0ms | 8.2ms | 0 |
| Redirect HEAD alone | 6,000 / 100.0/s | 4.0ms | 17.1ms | 0 |

Redirect p95 ≤20ms / p99 ≤50ms and create p95 ≤50ms hold **on this run**.
Statistics contain exactly **12,000 GET clicks**; HEAD adds none.
[Post-bench statistics](release/bench-stats-50ad9c3.json), [counters](release/bench-prometheus-50ad9c3.txt).
GET-minus-HEAD p95 (-1.0ms) is a comparison proxy between separate runs,
not an isolated added-cost quantile. One laptop, service/load generator on
the same host, active factory seats, no container benchmark or capacity claim.
The mission-02 load generator started only after our load and rollback gate
ended; overlap of unrelated host activity remains a qualification.
QA accepted proof item 12 in receipt 13 from this measurement/disclosure.
Its [independent audit](slices/01-analytics-v2/proof/qa-release-50ad9c3/audit.json)
reconciles 19,200 load completions, one PID/file-H2 custody, stored GET clicks
and aggregate method/counter counts. Per-request client latency samples were
not retained; the captured generator percentiles were not independently
recomputed. Server logs omit paths/methods, so phase attribution uses script
ordering plus aggregate Prometheus counts. This closes the measurement gap
for the specified candidate and run, with those qualifications retained.

Per the lead's 02:16Z notice, QA authored the initial NFR-L1 verdict/GAPS
edit `797f8fb` on GPT-6-Luna medium. Per the lead's restoration
condition, QA re-derived the raw benchmark and rechecked that edit on its
restored seat before recording receipts 13/14. Its numbers and bounded
closure held; commit `f825706` adds the configuration and sample-retention
qualifications. The [QA return](slices/01-analytics-v2/proof/qa-release-50ad9c3/QA.md)
supplies the new attribution; the earlier fallback attempt supplies none.

QA dogfood `qitem-20261004005528-6b84b233`, commit **`8cf894a`**, explored the
preserved jar on ports 18232/18233 for 20m13s: **no new defects** in its bounded
scope. It observed 100 concurrent redirects, natural quota recovery and
same-day restart persistence/unique overcount. [Report](../../docs/qa/dogfood/03-ambiguous-analytics.md)
links all 401 curl + four HEAD responses, both log sinks and 840 raw archive
members. It does not judge proof12, salt construction or induced failures.

The lead recorded a **01:27–01:33Z Luna Reserve fallback window** in
[NOTES](NOTES.md), commit `4891097`; QA's `8cf894a` was committed in that
window. Release2 independently re-derived its release-relevant claims on the
current model from the archived raw responses, all 840 hashes, all 405 wire/log
joins, aggregate/ranking/error/limiter/concurrent/restart observations.
No correction was required within that audit scope.
[Audit program](release/rederive-dogfood.py), [fresh result](release/dogfood-rederivation-8cf894a.json).
The [independent release review](../../docs/review/03-ambiguous-analytics/release-review.md)
at `eedc97f` separately re-derived all 840 raw hashes, 405 wire/log joins and
38 statistics bodies from preceding request inputs; no correction was needed.
Its focused final check passed at `93d55bd` on package `14815f9`; the
later shared-document hash change is recorded in §1.

Installed smoke does not re-prove natural UTC midnight, natural disk failure,
90-day aging, purge-in-progress shutdown, full container restart/down-up or
held-R0 under load. Slice QA controls and the accepted mission-01 lifecycle
record retain their original scope; §7 carries all qualifications.

## 4. Dependency advisories

```sh
scripts/gw --log missions/03-ambiguous-analytics/release/dependencies-50ad9c3.txt --offline dependencies --configuration runtimeClasspath
node tools/dep-advisories.mjs missions/03-ambiguous-analytics/release/dependencies-50ad9c3.txt missions/03-ambiguous-analytics/release/osv-50ad9c3.json
```

Runtime resolution passed. The network OSV query at
**2026-10-04T00:52:33.399Z** checked **97 resolved coordinates** and returned
**zero advisories**. [Resolved inventory](release/dependencies-50ad9c3.txt),
[dated raw results](release/osv-50ad9c3.json).

| Affected artifact | Advisory / severity / fixed-in | Reachability / remediation |
|---|---|---|
| All 97 queried coordinates | None returned by this OSV query | No advisory remediation packet required from these results |

Tomcat 11.0.25 and Jackson 3.1.7/2.21.7 overrides remain in the
[build](../../build.gradle.kts). This closes the sandboxed review's online
lookup obligation; it is not a claim of universal vulnerability clearance,
image OS-package scanning or future advisory status.

## 5. Evidence per slice and governance

| Boundary | Evidence / subject |
|---|---|
| Contract / planning | [SPEC](slices/01-analytics-v2/SPEC.md), [design](slices/01-analytics-v2/design.md), [impact analysis](slices/01-analytics-v2/impact-analysis.md); decision `b8c327b`, locked design `80ca44c`; [requirements review](../../docs/review/01-analytics-v2/requirements-review.md), [design review](../../docs/review/01-analytics-v2/design-review.md) |
| Builder / QA | [PROOF](slices/01-analytics-v2/PROOF.md), [progress](slices/01-analytics-v2/PROGRESS.md), [QA verification](slices/01-analytics-v2/proof/qa-ec466da/verification-summary.json), [coverage summary](../../docs/qa/coverage/01-analytics-v2/SUMMARY.md); candidate `ec466da`, evidence `6672ed9` |
| Independent code / security | [Code PASS](../../docs/review/01-analytics-v2/01-code-review.md), [security PASS](../../docs/review/01-analytics-v2/02-security-review.md), exact `ec466da`, record `61430eb`; item11 construction judged in [QA return](slices/01-analytics-v2/proof/qa-item11-ec466da.md) |
| Integration / wave | X′ `22fc8e2`, merge `c9b66dd`; [merged gate](../../docs/evidence/03-ambiguous-analytics/integrate-01-analytics-v2-check-22fc8e2.txt), [independent wave PASS](../../docs/review/03-ambiguous-analytics/wave-1-review-review-agent.md), [structural vantage](../../docs/review/03-ambiguous-analytics/wave-review-design2-agent.md) `7c54ef7`/`9928513` |
| Shared W2F-01 repair | Test candidate `0552b81`, merge `50ad9c3`, [owning re-review](../../docs/review/02-brownfield/wave-review-review-agent.md) `79eda7e`, [integration gate](../../docs/evidence/02-brownfield/integrate-w2f-01-check-0552b81.txt); our fresh gate in §2 |
| Release QA return | [Restored-seat review](slices/01-analytics-v2/proof/qa-release-50ad9c3/QA.md) and [audit](slices/01-analytics-v2/proof/qa-release-50ad9c3/audit.json), `f825706`; final receipts/readiness `d203049`; receipts [13](slices/01-analytics-v2/proof/judgments/00000013.md) / [14](slices/01-analytics-v2/proof/judgments/00000014.md) on `50ad9c3`; [reviewed captured readiness](release/proof-readiness-after-qa-50ad9c3.json) ready; later shared-document drift in §1 |
| Independent release review | [Package review and raw re-derivation](../../docs/review/03-ambiguous-analytics/release-review.md), initial `eedc97f`, final PASS `93d55bd` on package `14815f9`/product `50ad9c3`; later shared-document drift recorded in §1 |
| Shared QA/review context | [TRACEABILITY](../../docs/qa/TRACEABILITY.md), [GAPS](../../docs/qa/GAPS.md), [review ledger](../../docs/review/REVIEW-LEDGER.md), [cross-cutting register](../../docs/guidance/architecture.md) |

The governance-indexed [export INDEX](../../docs/evidence/03-ambiguous-analytics/INDEX.md)
is the authoritative packet/step table: **17 instances and 193 packets**.
The final export ran **02:57:12Z–02:58:48Z**, after QA reaffirmation and human
approval; the standalone QA-return records were supplemented at 03:04Z.
It includes the mission lifecycle and analytics slice,
compiled graph, every exported instance trace/show and packet transition/show,
proof, scope audit, active queue, usage and workflow status.
[Final validation](../../docs/evidence/03-ambiguous-analytics/final-validation.json)
parsed **427 raw JSON records**, checked the required records, committed proof
hashes, delivery stamps and verbatim human decision. The validation report
itself is additional. The [preparation validation](release/export-validation.json)
retains its historical 407-record/183-packet capture. This exporter captures the rig's
cross-mission records too; directory placement is not mission ownership.
The [verification program](release/final-export-check.py) also checks every
listed instance, every packet named in its traces, paired raw records and
local links/anchors. Run it from the repository root with
`python3 missions/03-ambiguous-analytics/release/final-export-check.py`.
[Snapshot notes](../../docs/evidence/03-ambiguous-analytics/INDEX-notes.md)
map the remaining obligations and [governance clauses](../../docs/GOVERNANCE.md).
The lifecycle is captured at `evidence_export`, before this author's handoff
and the lead's `mission_close`; neither future closure is claimed here.
The disk compile and running instance retain distinct source digests already
present in preparation; their dependency edges match. The disk compile lacks
an instantiation operation key and does not replace the running binding.
The scope audit retains one medium historical QA-artifact header advisory (§7).

Shared operator docs were committed by their custodian at **`e227acf`** and
read here: [README](../../README.md) runs the product in three commands without
OpenRig; [TESTING](../../docs/TESTING.md) explains suites/reports/coverage and
GET-minus-HEAD limits; [SETUP-FACTORY](../../docs/SETUP-FACTORY.md) describes
current seats/gates/TUI and D17's same-runtime document-review qualifications;
[RUNBOOK](../../docs/RUNBOOK.md) describes start/stop/recovery/logs/metrics.

## 6. Metrics, read plainly

**Preparation snapshot:** the shared custodian ran `node tools/sdlc-metrics.mjs` after the coordinated
exports; commit **`8d3c536`**, generated **01:52:27.811Z**.
[Derivations and limits](../../docs/metrics/README.md), [engine-derived data](../../docs/metrics/metrics.json),
[frozen relevant rows](release/metrics-relevant-8d3c536.json).
Use bound instance IDs, not the JSON's export-container `mission` label.

| Mission-03 instance | State / latency | Retries / rollback count | MTTR / human wait |
|---|---|---|---|
| Lifecycle `01M40RVNDQ0KT7FPWN1KJW0DC3` | Active; 51,313s elapsed (14h15m13s), not completed E2E | 0 / 0 | MTTR absent: no failed closure; engine-gate human wait 13,836s (3h50m36s) |
| Slice `01M416Z3CM54YQTX93V4KG0CPS` | Completed; 31,241s (8h40m41s) | 0 / 0 | MTTR absent; engine-gate human wait 0 |

These two instances had zero failed closures and no non-waiting re-entry.
Their zero retries/rollbacks are plain counts, not a claim that the factory
had none: W2F-01 belongs to mission02, and this branch-only rollback rehearsal
is not an engine rollback event. The custom ambiguity park lasted **22m6.732s**
(15:48:24.050Z → 16:10:30.782Z), visible in its transitions; the engine-gate
human-wait metric misses that custom park because its exported transitions
omit the blocked-on field. Thus the slice's numeric zero is not zero human wait.

Factory-wide totals across all 17 exported instances: 13 completed, one
aborted, three active/waiting; terminal success 0.929; 230 closures / 17 failed,
reported step success 0.926; 51 retry events, eight rollback-note/resume counts,
mean MTTR 1,652s, completed E2E p50 20,200s / p95 31,241s.
These are factory totals including drills and historical missions. Rollback
notes are a text heuristic, not eight confirmed production rollbacks. Waiting
closures remain in the step-success denominator; MTTR averages instance means
and its implementation accepts a later non-failed closure, including waiting.
No production outage or recovery SLO is established by those figures.

## 7. Known gaps, complete

The post-QA shared gap record is copied byte-for-byte as
[GAPS-after-qa-50ad9c3.md](release/GAPS-after-qa-50ad9c3.md), SHA-256
`004509e6d7d92230fa3273e1c92e3b6b67c7bc42d44f90b6009dd135487fef43`,
matching receipt 14 and the [post-QA verification](release/post-qa-verification-50ad9c3.json).
The original [GAPS-snapshot.md](release/GAPS-snapshot.md) and its manifest
hash remain unchanged as preparation history.
Every historical row/qualification remains available; the table below reconciles
its applicable sections and the review residue at this exact candidate.
Living GAPS/proof hashes may change after this snapshot; QA alone reaffirms them.

The final pre-export [complete GAPS copy](release/GAPS-final-export-50ad9c3.md)
matches QA receipt 17 and current committed bytes, SHA-256
`e3335166c248c5e6587b07aa3eab3a5585dfe4985c97aea0ee9714cd0c7cb3b3`.
It retains every shared-file section, including later D21 context; those later
product sections do not change the approved `50ad9c3` candidate. QA reaffirmed
the unchanged analytics sections and the delivery-only SPEC metadata.

| Gap / qualification | Current disposition and evidence | Owner |
|---|---|---|
| NFR-L1 proof12 | CLOSED for the specified run by QA receipt13: p95 3.7ms/p99 9.5ms at100 redirects/s for60s, zero bad responses. Same-host activity, trusted loopback, raised budgets and unretained client latency samples remain qualified (§3). Historical pending row is preserved beside the attributed closure. | QA return qitem-20261004020825-9237265b, pinned50ad9c3 |
| Hosted CI/CD exact SHA | HTTP404 means unverified access; no candidate run URL/conclusion. Human accepted this gap for local use at transition1916. Earlier successful runs and configuration-only failure-upload/lint evidence retain their own subject. No red hosted run, artifact-content review or wholly uncached image corroboration. | Human/operator; accepted qualification remains visible |
| M3S-01 LOW | `totalClicks` Javadoc/schema can imply lifetime; README/RUNBOOK correctly explain retained rows, but candidate source/API wording remains. | Next authorized `click/` + OpenAPI holder; no slice scheduled |
| M3S-02 LOW | Candidate property comment omits analytics identity effect; shared docs explain it. Later D21 owns comment-only repair; no later-tree claim here. | Mission02 `06-client-identity` |
| M3S-03/04/05 INFO | Limiter-bypass fallback warning entered register `41eff65`; preserve single-statement snapshot; gate SHA custody explicitly recorded. | Future affected slice; current capture links in §§2/5 |
| Daily identity limits | Restart can count a returning client twice; backward Clock and fresh salt can overcount; old proxy hashes are not separable/backfilled; spoofed browser UA may evade bot class. No cross-day identity. | Operator/Analyst; SPEC rules3–6, dogfood restart |
| Hash/salt construction | Item11 now independently accepted, superseding the historical downstream-pending row. In-flight key copies may finish after rotation; no JVM-wide erasure or hard timer-deadline claim. | Security review; future salt-change holder |
| Counter semantics / asynchronous writes | Successful redirect can lose its click. Unknown shutdown outcome may later commit and count in both families; counters reset on restart and do not decrement on purge. Lost reasons observed zero here; induced failures retain prior QA attribution. | Operator; security/design/counter evidence |
| Controlled QA inputs | Servlet peers/UTC Clock/H2 triggers and JDBC blockers prove disclosed inputs, not real remote TCP topology, natural midnight/24h expiry/90-day aging, natural hardware crash or arbitrary cross-process stress. Instrument corrections are retained. | QA records in complete GAPS snapshot |
| Original-suite compatibility | Literal original replay153/155 retains two audit enumeration failures under grant428e9e1/a12a0e2; authorized replay155/155 includes exact permitted shape changes. V3 pin changes on two tests permitted by132a884. Not an untouched all-green replay. | Lead grants, QA provenance |
| Coverage / result attribution | Per-suite misses are informational; merged gate100/100 has no exclusion. Parameterized XML groups do not uniquely name every source method; copied report/hash checks are the stated evidence. gradlew.bat EOL-only observation remains historical. | QA; exact release CSVs in §2 |
| Audit-read boundary | Headerless local relay indistinguishable from Operator; deployment must forbid it or forward a denying header. Guard mirrors known Boot4.1.1 rewrite triggers; revisit on upgrade. Embedded H2 has no separate DB-role audit privilege guarantee. | Operator/future platform-upgrade owner |
| Retention / migrations | Clock-controlled deletion/fault fixture is not large catch-up performance, PostgreSQL portability or SIGTERM-during-purge proof. Purge timeout does not interrupt JDBC. Flyway Community has no automatic down migration; lost rows cannot be reconstructed. Earlier X-only ancestry pending row is historical after X′ integration. | Operator; retention and V4 QA/design records |
| Container / host lifecycle | Current image smoke proves start/journey/binding, not new restart/down-up/held-R0 under load. Mission01's Mac-to-VM held-body failure remains accepted only after human transition1011/A-19 changed measurement to direct paths. Native Linux host path not tested. Perl/Time::HiRes prerequisite remains; locale dependency was removed with LC_ALL=C. | Operator/release; mission01 RELEASE and GAPS |
| Single node / storage / exposure | File H2, one instance, no HA/capacity/backup-restore SLO or PostgreSQL operational proof. Actuator/OpenAPI exposure and short codes are not authentication. Operator must maintain loopback and safe proxy settings. | Operator; RUNBOOK/security review |
| Disk-gauge compatibility | Path privacy leak QA-OPR-02/W2-01 fixed at4fe7042/5c264db; old path-tag selectors must change. Future second disk path would collapse series and needs a non-sensitive distinguishing tag. | Future disk-meter/configuration holder |
| Audit-column historical debt | All four rows CLOSED: click/UA atV3 mergeed2b940; link/audit_log atV4 merged55a502. No remaining column waiver. | QA2/integrator closure; GAPS |
| Audit-read HIGH historical defect | QA-AUD-01/CR-01 fixed at7ac8af5, mergedcb148c4; default/forged/override controls independently rechecked. Prior rejection preserved, no waiver. | Mission02 QA/review; GAPS re-check |
| W2F-01 historical test race | Fixed by0552b81 merged50ad9c3; ten scheduled-test repeats and owning independent re-review, then fresh whole gate. Failure records remain historical; no claim a green run alone disproved the race. | Owning review79eda7e; wave residual resolution |
| Benchmark and factory metric limits | Same-host single-run regression only; GET−HEAD unisolated; export-container mission labels and custom-human-wait omission disclosed in§6. | Release/custodian; future measurement-tool holder |
| Model fallback / restoration | QA-authored8cf894a fell in01:27–01:33Z Luna Reserve window; release2 and independent release review re-derived its bounded claims. The initial NFR-L1 edit797f8fb was also rechecked on QA's restored seat before receipts13/14. D17 permits shared runtime; separate authorship retained. | QA, release2 and independent release reviewer; linked audits in§3 |
| Evidence metadata advisory | Scope audit reports medium `proof_artifact_c1_invalid` on historical `qa-item11-ec466da.md`: required frontmatter is absent. Its attributed judgment remains accepted; hash-bound historical bytes are preserved. | Lead / QA evidence owner; [raw audit](../../docs/evidence/03-ambiguous-analytics/scope-audit.json) |

Closed historical gaps remain visible above; none is presented as an open
HIGH or silently erased. Full source-specific controlled probes, parser/capture
corrections, historical coverage numerators and downstream sequencing entries
are preserved verbatim in the linked snapshot.

## 8. Rollback

Rehearsed on disposable branch `drill/mission03-rollback` from the exact
candidate, preserving main. Commands executed:

```sh
git worktree add -b drill/mission03-rollback .worktrees/drill-mission03-rollback 50ad9c3
git -C .worktrees/drill-mission03-rollback revert -m 1 --no-commit c9b66dd
scripts/gw --log missions/03-ambiguous-analytics/release/rollback-check-50ad9c3.txt --offline -p .worktrees/drill-mission03-rollback check bootJar --rerun-tasks
```

The revert applied without conflict. Rehearsal commit **`5de969f6005f9cb5ca86df576c074a7a61c675e6`**
retains V3/V4 and W2F-01, restoring v1 statistics/recording behavior. Gate:
**218 unit + 233 functional =451**, zero failures/errors/skips,
**557/557 class-row CSV lines,200/200 branches**. Rollback jar SHA-256
`812ce6900c47d25e067d9560e7f192a55734ef0eaffca9c8500754b2477652c6`.
[Gate](release/rollback-check-50ad9c3.txt), [summary](release/rollback-gate-summary-50ad9c3.json),
[exact revert patch](release/rollback.patch), [drill row](../../docs/scenarios/drills.md#mission-03-release-rollback-rehearsal).

After stopping the candidate, copied its H2 file into a separate disposable
directory and ran the rollback jar on 127.0.0.1:18230. ReadinessUP,
existing benchmark link/target available, **12,000 clicks retained**, daily
shape only `date`/`clicks`; new recorded counter endpoint404.
[Health](release/rollback-health-50ad9c3.txt), [link](release/rollback-link-50ad9c3.txt),
[stats](release/rollback-stats-before-50ad9c3.txt), [counter absence](release/rollback-counter-50ad9c3.txt),
[installed smoke PASS](release/rollback-smoke-50ad9c3.txt), [log](release/rollback-installed-log-50ad9c3.jsonl).
The owned worktree was removed; the rehearsal branch/artifact remain local.
No rollback image was built or deployed in this rehearsal.

Actual recovery, performed by the orchestration lead/Operator:

1. Stop the service (`docker compose down` or SIGTERM to its recorded jar PID).
   Preserve the stopped H2 data directory/volume and Flyway history; use a
   separate copy to verify recovery before replacing the original.
2. Revert the analytics merge: `git revert -m 1 c9b66dd` on the authorized
   rollback branch derived from 50ad9c3. Later D21 changes require their own
   conflict/behavior review; this recipe is pinned to the pre-D21 candidate.
3. Run `scripts/gw check bootJar`. There is **no mission03 migration to reverse**.
   Retain V1–V4 and mission02's purge; do not run V3/V4 rollback SQL for this
   analytics rollback.
4. Rebuild locally: `docker build -t urlshort:rollback-m03-5de969f .` on the
   reverted tree, then run it with the preserved copy volume and
   `-p127.0.0.1:18231:8080`, non-root/read-only/tmpfs settings as in §3.
   Alternatively run the verified rollback jar on loopback with the copied
   datasource. Do not point two instances at one H2 directory.
5. Run `scripts/smoke.sh http://127.0.0.1:<port>`, inspect readiness and stored
   link targets/totals/referrers, confirm v1 daily shape and absent new counters.
   Stop the test instance, then the Operator selects the recovered artifact/data.

The new daily fields/counters and proxy-aligned hashing are removed. This
revert does not rewrite stored hashes, restore purge-deleted clicks or undo
legitimate link/audit mutations. A backup can restore only its snapshot's data;
restoring it discards writes after that backup. The retained rollback jar and
patch permit recovery without claiming a previously published rollback image.

## 9. Self-check

- Decision brief quotes actual human transitions and links stamped SPECs;
  six alternatives are recorded. Reviewed snapshot has all12 proof items
  accepted and release review PASS; shared-document drift2/5/6 and stamp drift1,
  human approval1916 and completed pre-export reaffirmation at0e125ca7 are explicit in §1.
- Fresh exact-product gate and preserved jar/image identities are linked;
  every local evidence link is validated before commit. [Prep verification](release/package-verification.json),
  [post-QA verification](release/post-qa-verification-50ad9c3.json). Hosted runs are unverified.
- Both loopback artifacts passed installed journeys and logs; byte-identical
  jar custody, teardown and untested lifecycle cases are explicit.
- Runtime dependency resolution and fresh network OSV response inspected:
  97 coordinates,zero returned advisories; no unstated remediation or scan claim.
- Slice planning, QA, coverage, reviews, integration and proof paths open;
  INDEX supplies packet/step tables. Final export follows the human gate and QA return;
  427 raw JSON records parse and all45 committed judgment references match.
- Shared metrics generated at8d3c536; own instance rows and factory totals
  distinguished, zero counts and missing MTTR/human-wait limits read plainly.
- Post-QA GAPS copied byte-for-byte and matched to receipt14; original pending
  snapshots/manifest remain unchanged. Final GAPS copy matches receipt17.
  Closures, review LOWs, operational
  limits and both model recheck obligations are retained with owners.
- Rollback rehearsed, gate and copied-data installed smoke passed; no analytics
  down migration; data-loss and later-tree limits described step by step.
- Current-model audit of QA's8cf894a and independent release re-derivation
  open raw bytes; QA's restored-seat receipts bind the benchmark/GAPS return.
  Final release review passed; later shared-document drift is not hidden.
  Seven original preparation/review records remain byte-identical to14815f9.
  Nothing pushed,
  release-tagged, published or exposed beyond localhost.
