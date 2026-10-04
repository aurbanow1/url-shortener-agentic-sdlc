# Mission 03 — release review

Product **50ad9c3ab9e65baa4100ede1772b514322957fa5**; final package **14815f9**, initial package **07f44e2**, routing clarification **26cd845**. Reviewer: review-agent@urlshort-factory (Codex), 2026-10-04 UTC. Packet **qitem-20261004020758-49ceba8e**, instance **01M40RVNDQ0KT7FPWN1KJW0DC3**.

**Final verdict: PASS for handoff to the human ship gate. All12 proof items accepted; no new MUST-FIX, HIGH, MEDIUM or LOW finding.** Exact-candidate hosted CI/CD remains unverified and the package's operational limits remain visible for the human's decision. This review is not ship approval. The initial wait and its focused resolution are retained below.

## Context and independence

The outcome is daily unique and bot counts on the existing statistics API, proxy-consistent click identity and bounded-cardinality click counters. Bots remain in raw figures; only retained rows count; memory-only daily salts must not become persistent or cross-day identities. The human's six choices, prior exact-candidate reviews and merged-wave custody support that scope. Confidence is high for the bounded package claims; the explicitly unverified hosted runs and operational limits remain unverified.

This is a separate-author review of release2-agent's package. Both seats use Codex, as permitted by D17; it is not a different-model-family review. I read the release, applicable guidance, QA summary, proof contract/judgments, ledger, wave findings/resolution, GAPS and the cited artifact/rollback/metrics evidence. The independent review of the producer's QA dogfood source from the recorded model-fallback window is explicit below.

The [487-path file ledger](proof/release-file-ledger-26cd845.json) records every package path and its hash/verdict: 80 primary package/evidence files and 407 exported JSON records. Exported records were parsed and consumed for the release's governance claims; unrelated missions in that rig-wide snapshot are context, not freshly approved product. Product source/build/runtime/API equality from 50ad9c3 to the documented package source boundary was independently checked. D21 is outside this candidate and outside this release judgment.

## Release checklist and claim reconciliation

| Release guidance §9 | Judgment and independent evidence |
|---|---|
| 1. Claims trace to the stated candidate | PASS. [Own audit](proof/release-audit.py), [result](proof/release-audit-50ad9c3.json). Actual preserved release jar, extracted image jar and dogfood jar all hash to `fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2`. The actual analytics merge is c9b66dd, not documentary tip 94aa2c0. Prior ec466da → X′22fc8e2 → merge custody is reconciled in the wave review; W2F-01 is resolved at50ad9c3/79eda7e. All 100 local Markdown links in the initial RELEASE resolve. |
| 2. Fresh gate, policy coverage, proof ready | Gate/coverage PASS; proof disposition below. Release's fresh gate records 226 unit +250 functional invocations, no failures/errors/skips, 16 executed tasks. I had independently run the same exact product's 476-test gate during W2F-01 re-review, recorded in79eda7e; this release turn audits the fresh release run rather than claiming another full invocation. Canonical XML from only test.exec/functionalTest.exec has BUNDLE581/581 lines,206/206 branches. CSV582 is correct class-row summation: RequestBodyLimitFilter contributes28 class lines but27 distinct source lines. No exclusion or weakened threshold. |
| 3. Jar/container loopback smoke and logs | PASS. Read both smoke outputs, environment override assertions, complete HTTP captures and source of the installed driver. Independently joined all22 release analytics exchanges to their exact request-completion records, checked full daily4/3/1 trusted and4/1/1 default bodies, recordedΔ4, lost zeros, redirects and full live OpenAPI equality. Inspect pins image277703a… to non-root/read-only, data volume, 127.0.0.1:18231,20s stop. Raw logs contain graceful shutdown; owned-port captures refuse connections. Scope qualifications separate these observations from midnight, crash, remote-client and large-data claims. |
| 4. Dependency advisories | PASS for the dated scan. Independently parsed resolved Gradle coordinates: all97 equal the query inventory; the raw OSV response contains97 empty results, zero findings/details at00:52:33.399Z. No returned advisory needs a reachability/remediation row. No new online query by this reviewer, OS-package scan, future advisory guarantee or proof of universal absence is claimed. |
| 5. Gaps complete and consistent | PASS for prep snapshot, with final QA update below. Historical rejected audit/privacy and scheduling evidence is retained with its actual fixed status. Both wave LOWs M3S-01/02 and INFO constraints remain visible. NFR-L3 remains an unisolated GET-minus-HEAD proxy; memory-salt/restart, controlled peers/clocks/faults, original-suite grant, async uncertain commit, single-node H2, retention deletion, native-host shutdown and hosted-run limits are stated. Exact-SHA GitHub HTTP404 is unverified access, not evidence of no run. Earlier a3d6867 hosted runs are not borrowed. |
| 6. Rehearsed rollback and loss | PASS. Independently compared the reversal at5de969f to c9b66dd's inverse product patch: identical, while V1–V4, W2F-01, build/container/CI remain. Audited451-test rollback gate and smoke, raw existing link,12,000-click v1 statistics and404 counter response, with shutdown log. The recipe pins the old product, preserves/copies stopped H2, keeps loopback and avoids simultaneous database users. It explicitly states no rollback image was rehearsed, later D21 needs separate review, purge-deleted rows/hashes cannot be reconstructed and backup recovery loses later writes. |
| 7. Engine-derived metrics | PASS. Independently parsed407 export JSON files, matched frozen factory totals to8d3c536, rederived mission51,313s and slice31,241s from creation/completion/generation timestamps, and checked respective trail closure counts and absence of failed closures. Human decisions830/876 match their quoted text/actor; gate wait and the separately qualified22m6.732s ambiguity park are not conflated. Factory-wide retries/heuristic rollbacks are not presented as this mission's. Final export remains after the human gate. |
| 8. Human default/alternative; unpublished | PASS. Decision brief recommends this pinned local candidate after review/proof return, with the alternative of waiting for human-triggered exact hosted evidence. Existing human plan-lock and six answers are quoted; neither implies ship approval. Local jar/image/rollback branch and preparation records show no publication action; this reviewer performed none. The human must see the exact-hosted-CI gap and retained operational limits. |

The benchmark source schedules requests by due time and measures through response completion. Its captured mixed load is6,000 redirects plus1,200 creates over60s; redirect p95=3.7ms/p99=9.5ms, zero bad responses. The following GET/HEAD phases and actual statistics show12,000 stored GET clicks. Percentiles are attributed to the inspected generator/output: individual latency samples are not retained, so I do not claim independent percentile recomputation. The negative L3 difference is not interpreted as negative recording cost or isolated NFR-L3 proof.

## Independent re-derivation of QA8cf894a

The lead specifically required another look at QA-authored8cf894a because it fell in the recorded01:27–01:33Z Luna Reserve window. I read its driver and raw ledger, but wrote and ran a separate [audit](proof/release-audit.py); this result does not rely on release2's re-derivation assertions.

- Archive SHA and all840 member names/hashes match; ledger, manifest and runtime settings match8cf894a bytes.
- All401 curl responses and four direct-wire HEAD observations have405 unique request IDs and match request-completion status exactly once in both console and file sinks (94 default,311 trusted per sink). Error content type/status/instance and canary absence agree; HEAD bodies are empty.
- Recomputed all38 observed statistics bodies from preceding successful GET inputs, including trusted/default identity, restart salt epochs, bot classifier, normalized origin, deterministic top-ten ordering and empty state. Full values match; this is more than checking an expected result copied into a report.
- Verified replay equality/mismatch422, forwarding-audit403,100 concurrent stored-target/no-store302s, natural quota60 admitted/15 refused with Retry-After and recovery, persisted link/audit/statistics across restart, and the documented cumulative-meter reset/late counts.

These are fresh computations over retained observations, not a new application run. The archive does not establish raw salt-memory erasure, natural midnight, exhaustive URI parsing, remote TCP identity or production crash behavior; prior explicit limits continue. No correction to the QA result was needed.

## Findings and residual disposition

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| — | — | — | No new release defect found | None |

Existing M3S-01/02 LOWs remain the assigned backlog, not blockers or new findings. No in-passing product fix is requested. Release2 is refreshing the current proof/GAPS statements once the already-routed QA receipts land, while retaining the original preparation snapshots. This is the expected completion of parallel evidence custody, not a failure invented against an intentionally unfinished downstream item.

## Initial readiness disposition — superseded by the return below

**WAITING on QA packet qitem-20261004020825-9237265b**, currently parked on external:daemon-mutation: item12 benchmark judgment and item6 shared-GAPS reaffirmation. QA's appended797f8fb GAPS paragraph correctly closes the measured NFR-L1 condition while retaining host/load limits; I reviewed that delta. It does not substitute for an attributed receipt. The [live readiness capture](proof/release-readiness-waiting.json) has item6 unknown after the shared-file change, item12 pending and the other ten accepted; no issues. No ship handoff before current attributed readiness is accepted and the final doc-only update is checked.

Continuation: after that item closes, read its exact receipts, independently hash every current receipt reference, read release2's final RELEASE/GAPS/readiness refresh, append a focused final disposition and ledger row, then hand off to ship_signoff if ready. Do not re-run settled product reviews, couple this release to D21 or replace the immutable prep snapshots. The blocked QA mutation is a workflow-service dependency, not a new product finding.

Self-check: exact product/package identity and every package path accounted for; own fresh archived-data checks passed; full gate observations accurately attributed; no invented finding or reopened settled issue; review authored only under docs/review; final ledger/queue exit follows the readiness disposition. Nothing pushed, release-tagged, published, or exposed by this reviewer.

## Readiness return — receipts 13 and 14

QA packet9237265b closed at transition1907. Read the restored-seat audit and source, the complete797f8fb → f825706 GAPS correction, receipt13 for item12 and receipt14 for item6, and the committed follow-upd203049. Both new judgments name50ad9c3 and qa-agent as author; the other ten retain their original ec466da subject and previously established merge custody. This does not relabel earlier QA as a new release-candidate gate.

The [fresh live readiness capture](proof/release-readiness-ready.json) is **ready,12/12 accepted, no issues**. Independently rehashed every current judgment reference: **41 references across33 distinct files**, all equal current bytes and committedd203049 content. The earlier sandbox/path failure and fallback-model attempt supply no acceptance; the new attributed receipts do.

The revised GAPS paragraph closes only NFR-L1's specified single run. Trusted loopback, raised budgets, shared-host activity, no individual latency samples and no isolated GET-minus-HEAD cost proof remain explicit. These qualifications agree with the previously reviewed raw benchmark and the QA re-derivation. Original preparation snapshots remain historical evidence. No new finding or product change results from this return; final package-commit verification follows.

## Focused re-review — 14815f9

**PASS; readiness dependency resolved.** Read all five changed release files plus the QA return/receipt deltas in f825706 and d203049. The [focused audit](proof/release-final-audit.py), run against14815f9, [passes](proof/release-final-audit-50ad9c3.json): all41 evidence references are both hash-current and committed; receipts13/14 match the live judgment IDs; the new complete GAPS snapshot equals receipt14/current GAPS; the original manifest, pending GAPS and pending readiness bytes are unchanged. All118 local links in the current RELEASE resolve. The five-path delta ledger is embedded in that result.

RELEASE/INDEX now distinguish ready post-QA evidence from the original pending snapshots and name both model-window rechecks. The source/build/tool/API diff between50ad9c3 and14815f9 is empty. All eight release-checklist rows now pass within the stated scope; no additional build, benchmark or product review was needed for this documentary completion.

Handoff to release2-agent's human ship_signoff gate on exact50ad9c3, with final package14815f9. Preserve the unverified exact hosted CI/CD result, both wave LOWs and all measurement/operational limits in the decision brief. D21 remains outside this release. No non-blocking fix is expected in passing; assigned backlog remains backlog. Final evidence export/metrics follow the human decision. No release approval, publication or delivery stamp is made by this review.

Final self-check: fresh attributed readiness12/12; all current receipt hashes/committed bytes verified; every final package delta file read; earlier empirical checks and scopes retained; no new findings; ledger resolution appended and exact-path commit prepared before the authored handoff.
