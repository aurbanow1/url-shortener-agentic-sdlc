# 01-greenfield-core — decomposition review

**Latest verdict: PASS at `6b5e17f8830de35f2052eda2fa66d8eda17f4a14`.** DC-01 and
DC-02 are fixed. The original review and each re-review remain below as history.

Candidate: `bb198b5a8d7b8ed8ff06a7e29871cd3da89b239a` (decomposition commit
`77fb368` plus the count correction). Reviewed 2026-10-03 by
`review-agent@urlshort-factory` (Codex); author: orchestration lead (Claude).
Packet: `qitem-20261003040709-b481dc8d`; instance: `01M3ZXEXAMS945ZS0QZ26KZK1V`.

**Verdict: REWORK — one HIGH finding, DC-01.** Give the new rate-limit response
an explicit owner and integration point in the committed OpenAPI contract.
The remaining decomposition checks pass. The mission review waits on a rework
packet to the planner; it does not use the slice workflow's failed back-edge.

## Context proof

The outcome is a working create/read/retire/redirect journey, then private click
analytics, safe operation, and readable audit history on one H2-backed instance.
The four slices are vertical user outcomes. This review judges their allocation,
territories, dependencies, tiers, and decision brief; it does not approve the
placeholder slice requirements or any implementation. Confidence: intent 98/100,
allocation 98/100, graph and ownership 98/100. Detailed ACs and design choices
remain the subsequent requirements/design steps' work.

The candidate's full 22-file range is `77fb368^..bb198b5`. During review the shared
checkout advanced to `d2ee898bd3fe022b8dd6115a2c73450a9fb64f42`; `git diff
--name-only bb198b5 HEAD` showed only mission `NOTES.md`. I read the candidate
NOTES from the git diff, not that later working copy. All other reviewed files
and product/build inputs remained identical. No product files were edited.

## Checklist and evidence

| Check | Result | Evidence / reasoning |
|---|---|---|
| One buildable outcome and its failure paths per slice | Pass | Mission SPEC's slice table and self-check: link lifecycle, click statistics, safe operation, audit read. The four-endpoint foundation is a stated, justified exception to the approximate size ceiling, with an outcome-based split available at design. |
| Concurrent territories disjoint, grants explicit | Pass for file ownership; DC-01 for contract completeness | Compared both w2 manifests: analytics owns click/V2/link hook/API export; operate owns web/config/build/container/smoke. No shared file or overlapping directory between them. Audit read runs in w3 for shared-file custody. |
| Real dependencies; waves have no internal edges | Pass | 02 needs the link lookup/redirect contract, 03 the endpoints/problem-detail contract, 04 the audit table. All depend on 01 only. The queue wave-map body matches the committed map. The lead explicitly admits waves; the graph does not automatically spawn children. |
| Tiers justified and workflow selected | Pass | All four manifests state high with foundation/migration/rate-limit/audit-boundary reasons. Each selects the human workflow by the documented tier rule; `rig/workflows/urlshort-slice.workflow.yaml:105` binds plan-lock to `human@kernel`. Child instances are intentionally not started before mission approval. |
| Every in-scope requirement allocated | Pass at id level; DC-01 for omitted cross-cutting effect | Independently counted 11 FR + 24 NFR = 35. Primary NFR allocation is 10 + 2 + 9 + 1 = 22; M1/M2 apply to every slice. No orphan slice. The 429 public contract needs M3 carried through its owning work. |
| Committed compiled graph, no unknowns | Pass | Fresh compile with the lifecycle operation key matches saved digest, source hashes, workflow spec and dependencies; `unknowns=[]`, `advisories=[]`. Live instance reports the same binding, adopted/current. See `proof/decomposition-graph-check.json`. |
| Decision brief honest about assumptions and risks | Pass | All eight first-tag assumed rows match REQUIREMENTS; S2/P2 are explicitly confirmed now but implemented later. Dependency placement and FR-17 retention/deferral are recommendations for the human, not claimed decisions. Verified the prior ship decision from transition 209 of `qitem-20261003023502-f807af1f`. |
| Brownfield impact analysis | N/A | Greenfield decomposition; later migration rollback/design obligations remain. |

The lifecycle has one accumulated `wave_review` after the lead's three-wave
integration sequence; both the mission table and bound executable graph describe
that same placement. Proof readiness is still `unknown` for all four slices,
with `No authored proof contract`: these are explicitly labelled scaffolds for
the requirements step, not delivery evidence.

Verification: `scripts/gw --log docs/review/01-greenfield-core/proof/decomposition-check.txt
--offline check` exited 0, `BUILD SUCCESSFUL`, 13 tasks up-to-date. This checks
the unchanged baseline gate; it did not freshly execute tests or prove the
unbuilt mission. The full output is in `proof/decomposition-check.txt`.

## Complete changed-file ledger

Paths beginning `slices/` below are relative to `missions/01-greenfield-core/`.
Each file was read in full; scaffold verdicts concern honest pending state only.

| File | Verdict |
|---|---|
| `docs/evidence/01-greenfield-core/compiled-graph.json` | Pass — full graph inspected and compared with fresh compile/live binding |
| `docs/evidence/01-greenfield-core/wave-map.md` | Pass — matches durable wave-map queue body |
| `missions/01-greenfield-core/NOTES.md` | DC-01 — repeats the API-document exclusion; otherwise rationale/receipts consistent |
| `missions/01-greenfield-core/PROGRESS.md` | Pass — decomposition complete, approval/delivery still pending |
| `missions/01-greenfield-core/SPEC.md` | DC-01 — shaping rule 5 omits the 429 contract from OpenAPI |
| `missions/01-greenfield-core/mission.yaml` | Pass — four active ordered slices |
| `slices/01-create-redirect/SPEC.md` | Pass — explicit placeholder, correct intent/tier/dependency |
| `slices/01-create-redirect/PROGRESS.md` | Pass — acceptance unchecked |
| `slices/01-create-redirect/PROOF.md` | Pass — proof template, no success claim |
| `slices/01-create-redirect/slice.yaml` | Pass — foundation grants, dependency-first work and requirements named |
| `slices/02-analytics/SPEC.md` | Pass — explicit placeholder, correct intent/tier/dependency |
| `slices/02-analytics/PROGRESS.md` | Pass — acceptance unchecked |
| `slices/02-analytics/PROOF.md` | Pass — proof template, no success claim |
| `slices/02-analytics/slice.yaml` | Pass — V2, limited redirect hook and w2 API grant explicit |
| `slices/03-operate/SPEC.md` | Pass — explicit placeholder, correct intent/tier/dependency |
| `slices/03-operate/PROGRESS.md` | Pass — acceptance unchecked |
| `slices/03-operate/PROOF.md` | Pass — proof template, no success claim |
| `slices/03-operate/slice.yaml` | DC-01 — lines 38–40 explicitly exclude the generated API contract |
| `slices/04-audit-read/SPEC.md` | Pass — explicit placeholder, correct intent/tier/dependency |
| `slices/04-audit-read/PROGRESS.md` | Pass — acceptance unchecked |
| `slices/04-audit-read/PROOF.md` | Pass — proof template, no success claim |
| `slices/04-audit-read/slice.yaml` | Pass — audit read boundary and w3 shared-file custody explicit |

## Findings

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DC-01 | HIGH | `missions/01-greenfield-core/SPEC.md:100`; `missions/01-greenfield-core/slices/03-operate/slice.yaml:38` | The plan assigns FR-10's 429 + Retry-After to 03 but explicitly documents it only in design/DESIGN, excluding the committed OpenAPI file to prevent concurrent edits. M3 is absent from 03's cross-cutting allocation; 02 is assigned stats documentation and 04 audit documentation, with neither assigned this response. `docs/REQUIREMENTS.md:98` requires the public API in committed OpenAPI with examples; a filter-produced response is still public API. This is a documented ownership hole, not a runtime defect claim. | Assign an explicit producer and serialization/regeneration point for the 429 ProblemDetail response, Retry-After header and examples in the committed OpenAPI document, with verification against the integrated service. Preserve disjoint concurrent territory. Carry M3 through this work and keep its ownership valid if the human defers 04. Update the allocation/grant/shaping-rule text and compiled binding if manifests change; answer DC-01 with the revised candidate SHA. |

Documentary reproduction: read the cited numbered lines, the allocation table
at mission SPEC lines 31–34, and REQUIREMENTS FR-10/M3 rows. The mission and
03 manifest both explicitly say to leave the generated document alone; no
other handoff assigns its 429 addition. No application repro is applicable
before the feature exists.

No MUST-FIX, MEDIUM or LOW findings. No extra dependency, layer or gate is
requested. The fix can retain the four user outcomes; choosing its smallest
ownership arrangement belongs to the planner.

## Self-check

- Candidate SHA and full range verified; 22 changed files / 22 read. Shared-main
  NOTES movement was isolated and the candidate version read from git.
- Fresh compile checked against committed graph and live instance; wave-map
  queue body and prior human decision checked at their durable sources.
- Baseline gate invoked and output read; its up-to-date limitation recorded.
- DC-01 has severity, exact lines, reproducible documentary evidence and a
  bounded required change. No speculative runtime defects asserted.
- Ledger row appended. All authored changes stay under `docs/review/`.
- Rework goes to the orchestration lead; mission packet waits on that item.

## Re-review ed7672f50246e32c8a24bb9118482489b014a09b

2026-10-03. Candidate confirmed by the rework item's transition 249 and
`git rev-parse HEAD`; checkout clean before reviewer evidence edits. Reviewed
all six changed files in `ed7672f^..ed7672f`, scoped to DC-01 and its fix.

**DC-01: fixed.** `03-operate` now owns M3 for the 429 ProblemDetail response,
Retry-After header and examples. Both w2 manifests assign ordered custody of
the generated document; the candidate must include 02 and regenerate before
QA. QA checks it against that candidate's live API document. This ownership
survives deferral of 04. Do not reopen the documentation ownership finding.

**Verdict: REWORK — new HIGH DC-02, introduced by the fix.** The new requirement
to wait for 02's merge conflicts with the retained requirement to obtain both
proofs before starting w2 merges. This is an execution-order defect, not a
repeat rejection of DC-01's now-correct ownership.

| Changed file | Verdict |
|---|---|
| `docs/evidence/01-greenfield-core/compiled-graph.json` | Pass — fresh compilation and live adopted binding match; executable steps unchanged |
| `docs/evidence/01-greenfield-core/wave-map.md` | Pass for ordered grant — matches replacement queue row `qitem-20261003041633-b5881594` |
| `missions/01-greenfield-core/NOTES.md` | DC-01 response accepted; DC-02 — repeats the new wait on 02's merge |
| `missions/01-greenfield-core/SPEC.md` | DC-01 fixed; DC-02 — both-proof barrier conflicts with implement handoff precondition |
| `missions/01-greenfield-core/slices/02-analytics/slice.yaml` | Pass — first holder of generated document until merge |
| `missions/01-greenfield-core/slices/03-operate/slice.yaml` | DC-01 fixed; DC-02 — 03 cannot reach its proof before 02 merges |

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DC-02 | HIGH | `missions/01-greenfield-core/SPEC.md:75` and `:100`; `missions/01-greenfield-core/slices/03-operate/slice.yaml:29` | The w2 sync point still says “waits for both proofs; merges serially”, but 03's implement handoff now requires a candidate descending from 02's merge. QA proof follows implement handoff (`rig/workflows/urlshort-slice.workflow.yaml:112`, `:124`). Thus merge02 waits for proof03, proof03 waits for handoff03, and handoff03 waits for merge02. | Make the admission/merge sequence unambiguous and acyclic. The small fix is to allow 02's reviewed/proven candidate to merge without waiting for 03's proof, then let 03 rebase/regenerate and complete QA/reviews; retain fan-in of both accepted slices before w3. Align the mission table, shaping rule, manifest/queue continuation and any applicable integration wording; an alternative acyclic schedule is acceptable. |

Documentary reproduction from the candidate's explicit preconditions:

| State: 02 has its proof, neither slice merged, 03's other code ready | Can proceed? |
|---|---|
| Merge 02 | No — SPEC:75 requires proof03 first |
| Hand off 03 implementation | No — SPEC:100 and 03 manifest:29 require merge02 first |
| Obtain 03 QA proof | No — no eligible implement handoff/candidate |

The plan therefore needs an explicit exception to its both-proof-before-merge
barrier. No child workflow was launched to manufacture a live stall; this
finding is a direct trace of authored prerequisites. The compiler's empty
unknowns do not detect this cycle: the new wait is prose inside a manifest,
not an executable dependency edge.

Verification: fresh compile digest
`321c37d85f2bef8ebcd5e8a89648b3c11a049a372ee9ad04a581001e6998b527`, saved source
hashes and workflow specification match; live instance binds
`1-321c37d85f2bef8e`, reconciliation current/adopted. The replacement wave-map
queue body equals the committed map. Receipt and cycle trace:
`proof/decomposition-rereview-ed7672f.json`. The full six-file diff contains no
product/test/build changes, confirmed separately against the original candidate;
the baseline gate was not repeated for this documentation-only fix.

### Self-check

- 6/6 changed files read; candidate SHA verified; original settled checks retained.
- DC-01 resolved explicitly; DC-02 is new evidence introduced by the fix, with
  exact locations and an observable no-progress state in the plan.
- Fresh graph/binding/wave-map checks completed; no runtime execution claimed.
- No new lower-severity findings. One open HIGH; ledger row appended.
- Reviewer edits remain under `docs/review/`; rework is routed to the planner,
  and the current review packet waits on that durable item.

## Re-review 6b5e17f8830de35f2052eda2fa66d8eda17f4a14

2026-10-03. **PASS — DC-02 fixed; DC-01 remains fixed. No open findings.**
Candidate verified against rework transition 258 and `git rev-parse HEAD`;
checkout clean before reviewer edits. Reviewed the complete four-file fix.

DC-02 resolution: mission SPEC's three wave rows, the following integration
paragraph, shaping rule 5, and 03's manifest now explicitly allow each slice's
merge on its own three verdicts. In w2, 02 merges without waiting for 03; 03
then rebases/regenerates, hands off, earns its own verdicts and merges. The
wave-level barrier is acceptance of both slices before starting w3. This
matches the slice workflow's `integrate → slice_accept` order and removes the
edge from merge02 to proof03 that caused the circular wait. The sequential
check is recorded in `proof/decomposition-rereview-6b5e17f.json`.

| Changed file | Verdict |
|---|---|
| `docs/evidence/01-greenfield-core/compiled-graph.json` | Pass — saved graph equals fresh compile; live binding current/adopted |
| `missions/01-greenfield-core/NOTES.md` | Pass — DC-02 response and revision receipt agree with source/live state |
| `missions/01-greenfield-core/SPEC.md` | Pass — per-slice merge preconditions and wave acceptance barrier now acyclic |
| `missions/01-greenfield-core/slices/03-operate/slice.yaml` | Pass — continuation explicitly permits merge02 without waiting for 03 |

Fresh compile: digest
`de9659cfc2f86cdbe9dd70a1398db5eb9230bc3184f5468b7c023f1e32e1468f`, bound workflow
`1-de9659cfc2f86cdb`, unknowns/advisories empty. Compared the complete saved JSON
with its prior version: only the digest, 03 manifest source hash and workflow
version changed; executable steps and policy did not. The live instance matches
the new source. The existing wave-map order remains 02 then 03.

No additional gate execution: this fix changes only planning documents and a
manifest comment. Fresh verification targeted those documents and the graph.
The baseline gate result remains scoped to the original review; no product,
child-workflow execution or implementation acceptance is claimed here.

### Self-check

- 4/4 changed files read; candidate and clean starting state verified.
- DC-02's formerly stalled state now has an eligible next action, merge02;
  each subsequent prerequisite follows from a completed earlier action.
- DC-01's owner, ordered grant, API verification and 04-deferral independence
  remain intact. No settled findings reopened or new findings added.
- Fresh graph/source/live-binding checks passed; evidence and ledger updated.
- Only `docs/review/` edited. Handoff to mission_plan_lock is a review verdict,
  not human approval: the eight assumed rows, dependency-override placement
  and FR-17 keep/defer decision still belong to the human.
