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

## Scoped re-check a7945e69c6f250870e576e55f0cdbeaab22edb20 — fast plan

2026-10-03. **PASS with one MEDIUM documentation finding (DC-03); no blocking
findings.** Assigned by ordinary queue item `qitem-20261003053449-031dfc8b`.
This checks the human's amendment against decomposition guide §9 items 2–7;
it does not reopen the completed lifecycle step or settled DC-01/DC-02.

### Context and coverage

The human's decision on `qitem-20261003052736-7830d02a` delegates subsequent
slice plan-locks, moves audit read to mission 02, drops aliases/expiry, and
ends mission 01 after w2. Its verbatim record and re-approval receipt agree
with the amendment. Confidence is high in the scope, allocation and graph
checks; this is not implementation acceptance.

The candidate has 20 changed entries. This assignment covers the 17 planning
entries below (renames count once). The three files under
`missions/01-greenfield-core/slices/01-create-redirect/design-probe/`
(`MechanismProbe.java`, `mechanism-probe.gradle`, `output.txt`) belong to the
unassigned design step and are explicitly excluded, not approved. Later
shared-checkout changes were not substituted for the candidate's amendment;
NOTES was read from the candidate. The requirements baseline is supporting
context from its preceding amendment, not another change in this commit.

| Changed file | Verdict |
|---|---|
| `docs/evidence/01-greenfield-core/compiled-graph.json` | Pass — entire saved export equals fresh compile |
| `docs/evidence/01-greenfield-core/wave-map.md` | Pass — two waves, ordered 02/03 custody; equals v3 queue body |
| `missions/01-greenfield-core/NOTES.md` | Pass for amendment — decision, scope move and revision receipt recorded; earlier history is superseded |
| `missions/01-greenfield-core/PROGRESS.md` | Pass — scope acceptance row explicitly amended |
| `missions/01-greenfield-core/SPEC.md` | DC-03 — allocations and schedule pass; opening outcome still overpromises |
| `missions/01-greenfield-core/mission.yaml` | Pass — only 01, 02 and 03 active |
| `missions/01-greenfield-core/slices/02-analytics/SPEC.md` | Pass — placeholder tier matches manifest |
| `missions/01-greenfield-core/slices/02-analytics/slice.yaml` | Pass — human delegation, risks and first-holder grant explicit |
| `missions/01-greenfield-core/slices/03-operate/SPEC.md` | Pass — placeholder tier matches manifest |
| `missions/01-greenfield-core/slices/03-operate/slice.yaml` | Pass structurally; DC-03 includes stale w3 comment |
| `missions/01-greenfield-core/slices/04-audit-read/slice.yaml` (deleted) | Pass — removed from mission 01; successor exists |
| `missions/02-brownfield/SPEC.md` | Pass for move record — revised allocation and future decompose obligation explicit |
| `missions/02-brownfield/mission.yaml` | Pass — moved scaffold registered |
| `missions/02-brownfield/slices/01-audit-read/PROGRESS.md` (renamed) | Pass for move record — unchanged empty acceptance scaffold |
| `missions/02-brownfield/slices/01-audit-read/PROOF.md` (renamed) | Record only — empty template, including old sample dot-id; mission 02 must author its proof, no evidence accepted here |
| `missions/02-brownfield/slices/01-audit-read/SPEC.md` (renamed) | Pass for move record — new identity, mission and prerequisite framing |
| `missions/02-brownfield/slices/01-audit-read/slice.yaml` | Pass for move record — brownfield impact analysis required; mission prerequisite replaces sibling edge |

### Checks and evidence

- **§9.2, custody:** 02 owns click/V2 and the bounded redirect hook; 03 owns
  web/config/container/smoke. `application.properties` is 03-only. OpenAPI
  transfers from 02 to 03 at 02's merge; 03's candidate must descend from it.
- **§9.3, schedule:** both w2 slices depend on 01; 02 can merge on its own
  verdicts, then 03 regenerates, earns its verdicts and merges. Acceptance of
  both leads to `wave_review`. Removing w3 introduces no cycle. DC-01/DC-02
  remain fixed.
- **§9.4, tiers:** both low-tier reasons cite the human decision and retain
  migration/privacy or proxy/container risks for the lead's plan review.
  `urlshort-slice-delegated` routes the gate to the orchestrator. Slice 01's
  human gate and mission human gates remain. The operator's second-judge
  variant is a launch-time obligation; this review does not claim that an
  unlaunched 03 instance has selected it.
- **§9.5, allocation:** 10 primary FRs, 21 primary NFRs plus common M1/M2 =
  33 in-scope ids, with no missing or duplicate primary owner. Inherited
  M3/O1/O2 obligations are explicit. FR-17/S6 have mission-02 ownership;
  FR-11/FR-12/S2 are explicitly dropped in `docs/REQUIREMENTS.md` rows and
  §4–§5. Mission 02 depends on mission 01; no dangling sibling edge remains.
- **§9.6, graph:** fresh compile, committed export and live binding share
  digest `c233d13f29e0aafbd82a13c1c7e24e3d57fe95a8537302e98c61cb1b9e2c6ca2`.
  Live instance `01M3ZXEXAMS945ZS0QZ26KZK1V`, version 18, binds
  `1-c233d13f29e0aafb`; reconciliation is current/adopted, changes and unknowns
  empty. The map equals queue row `qitem-20261003053250-a2190c42`.
- **§9.7, brief:** the amended tables, exclusions, gate count and status
  state the human's decision honestly. The unamended opening prose needs the
  bounded cleanup below; the allocation and routing are unambiguous.

Receipt: `proof/decomposition-fast-plan-a7945e6.json`. These were fresh
documentary/runtime-binding checks. No product gate was rerun for this
planning-only assignment, and no claim is made about the unrelated probes.

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DC-03 | MEDIUM | `missions/01-greenfield-core/SPEC.md:62` (also `:23`, `:25`); `missions/01-greenfield-core/slices/03-operate/slice.yaml:31` | Candidate's current Outcome still says four slices/three waves and an audit read endpoint at mission close; doghouse promises readable audit, the next paragraph assigns dropped aliases/expiry to later missions, and the manifest retains a w3-launch comment. Compare with SPEC:29, :38, :80 and :145, which correctly move audit read, end at w2 and drop aliases/expiry. A reader of the opening brief receives a larger promise than the allocated work. | In passing, update the current doghouse/outcome and future-scope sentence to three slices/two waves, audit writes now and audit read in mission 02, aliases/expiry dropped; change the stale manifest continuation to wave_review. Preserve the original decision history with explicit historical labeling. |

DC-03 is non-blocking because the authoritative allocation, amended wave
table, manifest composition and recorded human decision agree. The lead
should fix it in passing before the next brief or release-package reuse;
it is not a request for another human decision or another decomposition
gate. No other new findings or backlog are introduced by this re-check.

### Self-check

- 17/17 assigned planning changes inspected; supporting baseline, human
  decision and live state checked; three unrelated probe entries excluded
  explicitly. No whole-commit code/design approval claimed.
- Candidate SHA and fresh graph equality verified; documentary repro gives
  exact locations. No settled finding reopened.
- Reviewer changes are confined to `docs/review/`; ledger row appended.
- Close the ordinary assigned queue task with this verdict; route DC-03 as
  non-blocking follow-up to the lead, not as lifecycle rework.
