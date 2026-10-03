# 01-greenfield-core — decomposition review

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
