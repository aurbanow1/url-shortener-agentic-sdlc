# Mission 02 release package — candidate 30f8de4e

Prepared on main at **30f8de4e647b05ff54cde09f1019ae519b00069d**, after all six slices merged and both wave vantages passed. The pre-run at e227acf remains immutable history. Independent release review is **PASS**, committed446eca31, on packagead0c01f6. Human ship sign-off **approved local use** on qitem-20261004052127-b89c249b, transition2078 at2026-10-04T05:23:06.540Z; the [durable decision](release/ship-signoff-transitions.json) accepts the exact-SHA hosted CI gap and measurement limits. Nothing was pushed, tagged as a release or published.

## 1. Decision brief for ship sign-off

This candidate adds local paginated audit read, configurable scheduled click retention, the observed ProblemDetail/OpenAPI and anonymous-metric fixes, V3/V4 row-audit columns, GitHub CI/CD and weekly Dependabot, and the behavior-preserving ClientIdentity refactor. Existing links remain covered by unchanged regression journeys. The exact candidate passed a fresh 590-test gate, canonical 100% line/branch coverage and installed jar/image smoke. Independent QA/reviews and [four labelled drills](../../docs/scenarios/drills.md) are recorded.

[Formal release review](../../docs/review/02-brownfield/release-review.md),446eca31, independently reran590 tests and installed jar smoke; reconciled583/206 coverage,64/64 proof with174 committed evidence hashes,143 preparation links, all frozen metrics and raw rollback/container evidence. PR-01/02/03 and RR-01 are resolved; no MUST-FIX/HIGH remains. It is a separate-author, same-runtime review under the recorded D17 arrangement. A fresh ship-gate proof read at2026-10-04T05:22Z remains **ready64/64**, no issues, at the same revision as the [reviewed readiness snapshot](release/final-30f8de4e/proof-ready.json). This status annotation preserves the reviewed artifact and evidence snapshots.

The [mission SPEC](SPEC.md) quotes the human's original plan decision: “three slices in two waves as briefed, with the four labelled drills; purge stays here with the 90-day default as an operator setting”. Audit columns, CI/CD D14 and refactor D21 were explicitly added; the latest spec stamp is by the lead on behalf of the human at 2026-10-04T00:35:06.760Z. D21 requires one client-identity component with unchanged behavior. The mission delivery stamp was recorded by release-agent on behalf of human@kernel at2026-10-04T05:24:24.131Z, action01M42NXNY58M6EVQCMPG349RK4. All six slice delivery stamps are committed2020d53b; lead05:27Z rechecked every slice ready afterwards and confirmed no SPEC-only reaffirmation is needed.

**Decision:** the human approved the recommended local-use default and accepted the missing exact-candidate hosted CI and §7 measurement limits. The original alternative was to hold for a human push, both hosted runs and intended-host measurements. The decision's rationale references CI-verified e43ed246/PR14; this is preserved as human decision text in [NOTES](NOTES.md). The exact30f8de4e query still contains0 runs, so exact-SHA hosted success remains unverified. Rollback: stop writers and copy stopped H2 data; revert D21 alone to the preserved pre-run image, or reverse V4 then V3 and deploy the prepared mission01 binary.

## 2. Artifact and gate

[Gate manifest](release/final-30f8de4e/gate-artifact.json), [gate log](release/final-30f8de4e/check-bootjar.txt), [fresh test XML](release/final-30f8de4e/fresh-test-xml.tar.gz), [toolchain](release/final-30f8de4e/toolchain.txt), [candidate inputs](release/final-30f8de4e/candidate-input-tree.txt), [Docker build](release/final-30f8de4e/docker-build.txt), [image inspect](release/final-30f8de4e/image-inspect.json) and [jar comparison](release/final-30f8de4e/artifact-comparison.json) bind the artifacts.

~~~sh
scripts/gw --log missions/02-brownfield/release/final-30f8de4e/check-bootjar.txt --offline check bootJar --rerun-tasks
docker build -t urlshort:mission02-final-30f8de4e .
~~~

All 16 tasks executed: **268 unit + 322 functional**, zero failures/errors/skips. Canonical coverage is **583/583 source-deduplicated BUNDLE lines, 206/206 branches**, every missed counter zero. [Canonical log](release/final-30f8de4e/coverage-canonical.txt), [XML](release/final-30f8de4e/canonical-jacocoTestReport.xml), [CSV](release/final-30f8de4e/canonical-jacocoTestReport.csv) and [fresh exec inputs](release/final-30f8de4e/coverage-exec.tar.gz) preserve it. CSV sums 584 class lines because nested classes share source lines; both views are 100%. Only the two fresh suites' exec files were used.

Jar and installed image jar are byte-identical: **39,636,410 bytes**, SHA256 **fd6cf6f0c6c117c6138f50f9a2924c3f397edd65ae69b4a6dafebc4e6146132e**. Tag urlshort:mission02-final-30f8de4e; digest **sha256:827a4f977e3ee8c60b03d8c1c0db949b6a015e59819fcb23f4d60cb509e312b2**, Linux arm64. Host JDK 21.0.10/Gradle 9.8.0; image JRE 21.0.12.1+1; Boot 4.1.1; [Docker](release/final-30f8de4e/docker-version.txt) 28.4.0 on aarch64/Ubuntu 24.04.2 LTS in Colima.

The [pre-run-to-final diff](release/final-30f8de4e/product-delta.diff) contains ten reviewed source/config-comment/test paths. Source equals accepted e40b095; build, dependencies, migrations, workflows, container, smoke and bench inputs are unchanged. [Raw revalidation](release/final-30f8de4e/prerun-revalidation.json) checks that boundary.

| Exact-candidate hosted workflow | URL / conclusion |
|---|---|
| CI / ci.yml | No run URL returned; exact-SHA hosted success unverified |
| CD / cd.yml | No run URL returned; exact-SHA hosted success unverified |

The authorized repository [API capture](release/final-30f8de4e/hosted-runs.json) returned total_count 0 for this SHA. An initial wrong-repository 404 was corrected and excluded. Earlier [AC-13 first green runs](slices/05-ci-cd/PROOF.md#ac-13-first-github-runs-recorded-by-the-operator-2026-10-03) retain their own SHA/access/operator qualifications; they are not candidate runs. This explicit gap follows [CI/CD §5](../../docs/guidance/ci-cd.md#5-release-package).

The [initial prep proof snapshot](release/final-30f8de4e/proof-prep.json) had 15 drifted items in five slices, with ClientIdentity ready 17/17. Both required reaffirmation packets are now closed: [QA2](../../docs/qa/final-reaffirmation/qa2.md), a9b59315, and [QA](../../docs/qa/final-reaffirmation/qa.md), 2a47ad28. Their [closure records](../../docs/evidence/02-brownfield/INDEX.md#packets-workflow--step--state--owner) retain receipts and scope qualifications. The [fresh live proof](release/final-30f8de4e/proof-ready.json) is **ready: 64/64 accepted**, audit13, retention10, dogfood8, audit-columns9, CI7 and identity17, with no issues. The lead also checked all nine slices of missions01/02 ready at04:54Z. These are document/evidence reaffirmations against 30f8de4e, retaining prior run attribution. A small SPEC-only round follows delivery stamps before final export.

## 3. Installed smoke

The preserved jar ran on 127.0.0.1:18240 with disposable H2 data; the image ran on 127.0.0.1:18241 with its own disposable volume. Both passed [scripts/smoke.sh](../../scripts/smoke.sh): create201 → redirect302/no-store → read200 → stats200 → invalid400/unknown404 → retire204/410, ping/method errors, health/live/readiness, metrics, Prometheus and OpenAPI. [Jar smoke](release/final-30f8de4e/jar-smoke.txt), [jar log](release/final-30f8de4e/jar-console.jsonl), [container smoke](release/final-30f8de4e/container-smoke.txt) and [container log](release/final-30f8de4e/container-console.jsonl) retain the observations.

The jar also proved environment overrides, temporary datasource and a trusted forwarded client admitted 10 times then429. The image [full wire captures](release/final-30f8de4e/installed-wire.json) show one GET → one click/one unique/zero bots, HEAD → no click, origin-only referrer, and non-echoing invalid400. Actual /api/audit rejects forged XFF with403; [audit capture](release/final-30f8de4e/audit-forbidden.txt) corrects an initial wrong URL404.

[HostConfig](release/final-30f8de4e/container-inspect.json) proves localhost publishing, read-only root, disposable data mount and 20s stop timeout; image configuration defines UID10001. [Stopped inspect](release/final-30f8de4e/container-stopped.json) and logs show graceful shutdown. Container222433bf5990/volume were removed; final jar and rollback PID9004 stopped, ports released. Initial container smoke during startup got an empty reply; the complete post-startup run passed.

These fresh captures replace historical privacy/HostConfig/log claims whose complete raw evidence was missing. The old installed-v2 record remains dated stats/counter evidence. Smoke does not prove remote TCP/IPv6, natural disk failure, large catch-up, SIGTERM during purge or restart under load through the macOS VM forwarder; preserve the AC-28 exception and older lifecycle attribution.

## 4. Dependency advisories

[Fresh runtime classpath](release/final-30f8de4e/dependencies.txt) resolved 97 coordinates; [OSV raw results](release/final-30f8de4e/advisories.json), **2026-10-04T04:21:27.259Z**, contain 97 results and **zero advisories**. No affected-artifact/severity/fixed-in/reachability or remediation row is needed. Prior security qualifications remain in [review ledger](../../docs/review/REVIEW-LEDGER.md) and [RISKS](../../docs/RISKS.md).

The pre-run's exact tracked blobs were independently rescanned: 9,236 text, 497 binary exclusions, zero private-key/AWS/GitHub-token signature findings. [Revalidation](release/final-30f8de4e/prerun-revalidation.json) preserves the pin/method. This excludes entropy, history, untracked files and external stores.

## 5. Evidence per slice

[Independent final wave review](../../docs/review/02-brownfield/wave-review-review-agent.md#added-d21-range--pass-at-b8d7fc16--fda42757), bd74b509, is PASS; [structural vantage](../../docs/review/02-brownfield/wave-review-design-agent.md), fea4749b, is recorded. Prior five-slice/W2F-01 review is retained, then D21 added. All 13 register concerns and 159 complete original/current response/log pairs were checked; no MUST-FIX/HIGH remains. D21 actual merge b8d7fc16 contains accepted e40b095; fda42757 is its next documentary commit.

| Slice / merge | SPEC, design, proof, coverage, independent code/security review |
|---|---|
| 01-audit-read / cb148c4 | [SPEC](slices/01-audit-read/SPEC.md), [design](slices/01-audit-read/design.md), [PROOF](slices/01-audit-read/PROOF.md), [coverage](../../docs/qa/coverage/01-audit-read/SUMMARY.md), [code](../../docs/review/01-audit-read/01-code-review.md), [security](../../docs/review/01-audit-read/02-security-review.md) |
| 02-click-retention / ed2b940 | [SPEC](slices/02-click-retention/SPEC.md), [design](slices/02-click-retention/design.md), [PROOF](slices/02-click-retention/PROOF.md), [coverage](../../docs/qa/coverage/02-click-retention/SUMMARY.md), [code](../../docs/review/02-click-retention/01-code-review.md), [security](../../docs/review/02-click-retention/02-security-review.md) |
| 03-dogfood-fix / 5c264db | [SPEC](slices/03-dogfood-fix/SPEC.md), [design](slices/03-dogfood-fix/design.md), [PROOF](slices/03-dogfood-fix/PROOF.md), [coverage](../../docs/qa/coverage/03-dogfood-fix/SUMMARY.md), [code](../../docs/review/03-dogfood-fix/01-code-review.md), [security](../../docs/review/03-dogfood-fix/02-security-review.md) |
| 04-audit-columns / d55a502 | [SPEC](slices/04-audit-columns/SPEC.md), [design](slices/04-audit-columns/design.md), [PROOF](slices/04-audit-columns/PROOF.md), [coverage](../../docs/qa/coverage/04-audit-columns/SUMMARY.md), [code](../../docs/review/04-audit-columns/01-code-review.md), [security](../../docs/review/04-audit-columns/02-security-review.md) |
| 05-ci-cd / 0aa3695 | [SPEC](slices/05-ci-cd/SPEC.md), [design](slices/05-ci-cd/design.md), [PROOF](slices/05-ci-cd/PROOF.md), [coverage](../../docs/qa/coverage/05-ci-cd/SUMMARY.md), [code](../../docs/review/05-ci-cd/01-code-review.md), [security](../../docs/review/05-ci-cd/02-security-review.md) |
| 06-client-identity / b8d7fc16 | [SPEC](slices/06-client-identity/SPEC.md), [design](slices/06-client-identity/design.md), [PROOF](slices/06-client-identity/PROOF.md), [coverage](../../docs/qa/coverage/06-client-identity/SUMMARY.md), [code](../../docs/review/06-client-identity/01-code-review.md), [security](../../docs/review/06-client-identity/02-security-review.md) |

[TRACEABILITY](../../docs/qa/TRACEABILITY.md), [GAPS](../../docs/qa/GAPS.md) and [REVIEW-LEDGER](../../docs/review/REVIEW-LEDGER.md) retain criterion/run/judge attribution. D21 [post-merge item16](../../docs/qa/06-client-identity/item16-followup.md), dd3d69ad, completes its 17/17 after register custody/review. Retention W2F-01 is test-only at50ad9c3.

[Installed dogfood](../../docs/qa/dogfood/02-brownfield.md), d83bbc5, is a bounded PASS/no new defects, with fresh checks and explicitly equivalent reused observations. It did not accept D21 in advance. Carry-forward now rests on the reviewed preservation delta and fresh final gate/smoke; no new comprehensive final-SHA dogfood pass is claimed.

[Evidence INDEX](../../docs/evidence/02-brownfield/INDEX.md) supplies final generated packet/state/owner and step-trail tables tied to [GOVERNANCE](../../docs/GOVERNANCE.md), after release review, human approval and all seven delivery stamps. [Final export custody and verification](release/final-evidence-export/README.md) preserve the reviewed preparation export separately; [final validation](../../docs/evidence/02-brownfield/final-validation.json) binds current ready64/64 proof and committed evidence. This snapshot precedes its own export closure and mission_close. Preparation/runtime/metrics snapshots retain their original dates and bytes.

## 6. Metrics, read plainly

Current-state metrics were generated at **2026-10-04T04:46:43.295Z** with the unchanged generator in an isolated workspace containing only the fresh mission02 export, plus --live instance reads. This prevents older mission03 packet/trace copies from overriding current records. [Frozen metrics](release/final-30f8de4e/final-metrics.json), [all raw inputs](release/final-30f8de4e/final-metrics-inputs.tar.gz), [provenance](release/final-30f8de4e/final-metrics-provenance.json) and [replay PASS](release/final-30f8de4e/final-metrics-replay.json) reproduce **all 17 instance rows, 181 packet metrics and every total**, freezing only generation time and normalizing the live/export directory label. [Shared derivations](../../docs/metrics/README.md) preserve the scope/MTTR/human-wait limits and the separately frozen mission03 history.

Factory totals: 17 instances (15 completed, one active, one aborted), instance success0.938; 249 step closures/18 failed, step success0.928; 55 retry counts, nine rollback heuristics; mean-of-instance MTTR1,711s; completed E2E p50/p95 20,200/50,872s; counted human gate wait31,526s. These include other missions and drills.

| Bound mission02 instance | E2E seconds / state | Failed / retries / rollback heuristic | MTTR seconds / counted human wait seconds |
|---|---|---|---|
| Lifecycle 01M40SN34E37K96B38JPG9K41X | 60,935 elapsed, active at release_prep | 0 / 0 / 1 | absent / 12,869 |
| 01-audit-read / 01M416ZY5N11CDGZBM2DT4GAXS | 22,077, completed | 3 / 10 / 0 | 2,479 / 0 |
| 02-click-retention / 01M4170AA9E72WW1BXEA5PX0AP | 26,647, completed | 2 / 6 / 2 | 2,265 / 0 |
| 03-dogfood-fix / 01M41HFT4CTYPNWWNB8PJ99NFE | 6,340, completed | 0 / 0 / 0 | absent / 0 |
| 04-audit-columns / 01M41HG4P6DEVAKTCJC6J9QAWP | 20,200, completed | 0 / 0 / 0 | absent / 0 |
| 05-ci-cd / 01M41HGF6AHB6AZR3Q0KQ8MQR7 | 14,513, completed | 0 / 0 / 0 | absent / 0 |
| 06-client-identity / 01M425CY9Z4K03G14Y677AT0PA | 11,397, completed | 1 / 4 / 0 | 2,117 / 0 |

The six slices have six failed closures and 20 retry counts: audit requirements/design/code review, two retention design reviews and D21's test-only polling race. Other rework routed through ordinary packets is not automatically a retry. Lifecycle retries are zero and MTTR is absent, because it had no failed closure. Its one rollback hit is the original plan brief mentioning a revert drill; retention's two hits describe migration rollback requirements. These text matches do not count executed production rollbacks. The real drills and stopped-copy rehearsals have their own evidence. MTTR measures engine-step closure timing, not incident recovery; the implementation allows the next non-failed closure to be a wait and totals average instance means. Active latency is elapsed to generation and excluded from completed percentiles. Numeric human wait omits parks lacking the recognized field/note and delegated slice gates.

Historical8d3c536 reported 230 closures/8 rollback heuristics; its committed raw inputs yield **233/9**. D21 has six rather than three closures, and a release coordination note adds one rollback hit. Its then-used mission03 input set was not committed there. [Revalidation](release/final-30f8de4e/prerun-revalidation.json) and [historical raw archive](release/final-30f8de4e/historical-metrics-8d3c536-inputs.tar.gz) preserve this discrepancy without rewriting history. Other checked historical fields reconcile.

Historical [benchmark](release/pre-run-e227acf/bench.txt), e227acf: 1,200 creates/6,000 redirects together over60s achieved20/100 per second, zero bad responses, create p95/p99 3.7/7.7ms, redirect3.0/6.3ms. GET-alone4.0/16.2ms and HEAD-alone2.3/5.2ms give a1.7ms separate-distribution p95 comparison, not isolated click cost. Counts/printed quantiles were re-read; latency samples were not archived. No final-SHA load rerun, capacity or exact-final latency guarantee is claimed.

## 7. Known gaps, complete

Every current [GAPS](../../docs/qa/GAPS.md) row is copied without omission to the [complete snapshot](release/final-30f8de4e/GAPS-snapshot.md). Historical qualifications keep their original stage attribution.

| Area / owner | Release qualification |
|---|---|
| Hosted CI/CD / human, lead | Exact-candidate query returned0 runs; earlier greens are other SHAs. Human push is required for hosted proof; agents never push. |
| Gradle download / lead | Existing **MEDIUM**:10s timeout/0 retries; bounded retry/timeout follow-up after missions02/03, sooner on a third blocker. Old rollback-builder initialization stalled and was stopped; no network cause established. |
| Ping time / next ping owner | **LOW**: direct Instant.now predates shared Clock; inject/reconcile on next touch. |
| Future V5 tests / V5 owner | **LOW**: pin V4-specific tests when V5 arrives; reasoned future risk, not current reproduced failure. |
| Manifest / next owner | **INFO** W2D21-01: stale exclusion below explicit properties comment grant; grant governs. W2D21-02 records the intervening README correction. |
| AC-28 / Operator | Six original-criterion macOS/Colima held-upload failures remain. Direct/VM controls drained; amended55a197a under human decision1011 is accepted. Original host-path criterion did not pass. |
| Audit / Operator | Accepted compatibility grant remains narrow; headerless local relay, actual remote TCP/IPv6 and future Boot trigger behavior are limits. Peer guard is not authentication. |
| H2/retention/rollback / Operator | Single-node file DB; controlled clocks/JDBC faults/H2 copies covered, not PostgreSQL, crash, large catch-up or SIGTERM during purge. Purged rows need backup; DDL loses audit metadata. |
| Verification / release, QA | Dated single-host quantiles and bounded reused dogfood; no sample-level quantile reproduction, broad capacity, natural midnight or comprehensive final-SHA dogfood/load. Functional localhost collision stays LOW follow-up. |
| Evidence metrics / release | Historical8d input mismatch preserved. Export-directory labels are not ownership; rollback text/resume and custom-wait/MTTR limits remain explicit. |
| Security / release, review | OSV97/0 is point-in-time. High-signal tracked scan excludes binaries/entropy/history/untracked/external stores. Complete GAPS/RISKS retains all prior advisory qualifications. |

Both required QA closures are recorded. Reaffirmation adds no build and does not accept later content on an earlier slice's behalf.

## 8. Rollback

[Executable recipe](release/final-30f8de4e/rollback.md) names D21 merge b8d7fc16 and preserved pre-run image, then mission01 binary/schema target. Literal [V4](release/pre-run-e227acf/rollback-v4.sql) then [V3](release/pre-run-e227acf/rollback-v3.sql) remove only their columns/history.

[Raw28 CSV archive](release/final-30f8de4e/rollback-raw-csv.tar.gz) independently proves versions1–4 →1–3 →1–2 →1–4, with unchanged older domain projections through DDL. Actual data rows **exclude headers**: before1,203 links/1,204 audit rows/12,003 clicks; after reapplication/smoke1,204/1,206/12,004. The old header-inclusive wording was wrong and remains qualified history.

A fresh stopped-data copy received V4→V3 rollback; actual prior **f09010396d584fdf41702bb99856aa17a1ec1206** jar started on localhost18242 and passed smoke. [Artifact](release/final-30f8de4e/rollback-artifact.json), [build](release/final-30f8de4e/rollback-bootjar.txt), [smoke](release/final-30f8de4e/rollback-prior-smoke.txt) and [graceful log](release/final-30f8de4e/rollback-prior.jsonl) prove that path. Prepared image urlshort:rollback-mission01-f090103, **sha256:2d4e63d1eea1418114875cbf614e4689e46d1f878daf29c9324d2e0f7583e611**, was built/inspected, not separately smoked: [inspect](release/final-30f8de4e/rollback-image-inspect.json), [runtime packaging](release/final-30f8de4e/rollback-runtime.Dockerfile), [build qualification](release/final-30f8de4e/rollback-build-qualification.txt).

Earlier [integrator revert drill](../../docs/scenarios/drills.md) supplies the mission's source rollback rehearsal; D21-specific revert is described but not executed. Full rollback also returns statistics to v1. Retain the stopped backup: metadata is discarded, purged clicks cannot be recreated, restoring loses later writes. Main/user data were untouched.

## Self-check

- Exact candidate, honoured scope stamps and human local-use decision recorded; original default/alternative retained.
- Fresh gate/coverage and identical installed artifacts captured; final source equals reviewed e40b095; no exclusion added.
- Both smokes passed; container full wire/log/HostConfig and jar logs retained; own services/container/volume stopped.
- Fresh OSV97/0 and bounded secret-scan method/limits recorded.
- All six slices, both vantages, item16 and attributed dogfood link to evidence; package links and committed proof-reference hashes checked in [preparation validation](../../docs/evidence/02-brownfield/prep-validation.json).
- Fresh metrics reproduce from the archived raw inputs; both QA packets closed and live mission proof reads ready64/64.
- Complete GAPS copied; hosted CI, review backlog and original AC-28 host failure preserved.
- Raw rollback stages plus prior-version startup/smoke and exact commands retained; unrehearsed paths named.
- No push, release tag, image publication, remote exposure or self-approval.
