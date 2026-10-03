# Decomposition review — 02-brownfield

- Candidate: `b689f365baf3da8c42bbd91dda49cd88579e0c3d` (15 decomposition artifacts).
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003115740-6f882a4a`; lifecycle `01M40SN34E37K96B38JPG9K41X`.
- **Verdict: PASS — no findings. Ready for the human mission plan-lock.** The decision brief explicitly requests three slices rather than the earlier two; this review does not supply that human approval.

## Context and complete coverage

Doghouse: preserve existing links while an Operator gains local audit access and scheduled deletion of old clicks, fix one observed defect with a regression test first, and demonstrate the factory's recovery paths through labelled drills.

Read all 15 candidate artifacts below, the existing audit-read scaffold, requirements baseline, recorded D7/D11 decisions, project and slice workflows, mission-03 coupling, decomposition guidance including §9, brownfield guidance, review contract and existing drill record. Confidence: 97/100 for the decomposition. The unknown defect and final migration need are explicitly deferred to requirements/impact analysis. Later commit `15e108b` adds mission progress only; all 15 reviewed files remain byte-identical to the candidate.

| Candidate file | Verdict |
|---|---|
| `missions/02-brownfield/SPEC.md` | PASS — current scope, allocation, slice-count decision, waves, drills, risks and exclusions |
| `missions/02-brownfield/mission.yaml` | PASS — three active slices and project lifecycle profile |
| `missions/02-brownfield/NOTES.md` | PASS — adopted revision receipt and concrete continuations for launches, drills and defect input |
| `missions/02-brownfield/slices/01-audit-read/slice.yaml` | PASS — audit outcome, justified delegation, first shared-file holder, impact analysis before design |
| `missions/02-brownfield/slices/01-audit-read/PROOF.md` | PASS as scaffold — corrected mission identity, no false implementation proof |
| `missions/02-brownfield/slices/02-click-retention/slice.yaml` | PASS — retention outcome, second shared-file holder, explicit qa2 proof policy and matching workflow |
| `missions/02-brownfield/slices/02-click-retention/SPEC.md` | PASS as scaffold — detailed observable requirements and proof contract still owed |
| `missions/02-brownfield/slices/02-click-retention/PROGRESS.md` | PASS as scaffold — no completed acceptance claim |
| `missions/02-brownfield/slices/02-click-retention/PROOF.md` | PASS as scaffold — no claimed implementation or proof |
| `missions/02-brownfield/slices/03-dogfood-fix/slice.yaml` | PASS — one defect outcome, external input, isolated wave, territory adoption before implementation |
| `missions/02-brownfield/slices/03-dogfood-fix/SPEC.md` | PASS as scaffold — does not invent the defect or its solution |
| `missions/02-brownfield/slices/03-dogfood-fix/PROGRESS.md` | PASS as scaffold — no completed acceptance claim |
| `missions/02-brownfield/slices/03-dogfood-fix/PROOF.md` | PASS as scaffold — no claimed implementation or proof |
| `docs/evidence/02-brownfield/wave-map.md` | PASS — two waves and ordered w1 custody match the durable queue record |
| `docs/evidence/02-brownfield/compiled-graph.json` | PASS — identical to fresh compile; bound digest and source hashes match; unknowns empty |

## Decomposition checklist

| Check | Result and evidence |
|---|---|
| One buildable outcome per slice | PASS — audit read, retention deletion and one defect repair each serve a distinct outcome. FR-15 improvements stay with the defect's tests/docs. Recovery drills have a separate owner and record rather than masquerading as product slices. |
| Territory and concurrency | PASS — w1 feature packages are disjoint (`audit/`, `click/`). Both shared grants are explicit: application configuration and the next Flyway number belong to audit first, then retention. Retention rebases onto audit's merge; audit merges on its own verdicts without waiting for retention. w1 cannot launch before mission-01 03-operate releases configuration/OpenAPI ownership. The unknown defect runs alone in w2; its territory must be set at requirements and adopted through workflow revision. |
| Dependencies and synchronization | PASS — inherited audit/click foundations exist, including analytics merge `091ff46` on main (ancestry verified). Remaining launch condition is 03-operate integration; no claim is made that it has already happened. w2 follows w1 and its requirements step waits if mission-01 dogfood evidence is absent. Wave queue `qitem-20261003115650-35b315f4` matches the file. These are lead-owned launch/merge obligations; the outer graph does not automatically schedule child workflows. |
| Tiers and gates | PASS — low tiers follow recorded D7 fast-plan delegation and D11, with normally high concerns explicitly named: audit exposure, schema changes and irreversible deletion. The lead re-weighs the defect when known. Actual slice plan-lock targets the orchestrator; mission plan-lock and ship sign-off target `human@kernel`. The exception does not remove independent requirements, design, QA or combined code/security reviews. |
| Requirement allocation | PASS — FR-17/S6 belong to audit; P2 to retention; FR-14/15 to the defect fix; FR-13 and applicable X2 span all three. M1/M2 are carried throughout, with API/logging obligations where applicable. The earlier dropped aliases/expiry remain out of scope. Three slices are justified openly because the same D7 inputs assign undelivered P2 here; the human is offered the alternative of moving it to mission 03. |
| Routing and live graph | PASS — regular judges on audit/fix; the retention `-b` workflow routes QA to qa2 and all reviews to review2, matching its explicit proof policy. Fresh graph, source hashes and adopted live binding agree at digest `dc8376c3115331b72fb954ac646e3cac2b365ecd697c2194e0a0fe9782c8424c`, version `1-dc8376c3115331b7`, with no unknowns or pending revision. |
| Drills and independence | PASS — four scenarios name actors, triggers and evidence. The lead must queue release-agent work at w1 launch. Natural QA rejection is preferred, otherwise a labelled drill instance; revert rehearsal uses a throwaway branch/worktree; route/abort drills keep product custody separate. If a defect must be seeded, release owns and discloses it, preserving separation from merger and QA judge. These are planned demonstrations, not completion claims. |
| Brownfield analysis and decision risks | PASS at this boundary — each manifest requires impact analysis before design, compatibility of existing links, and rollback for schema changes. Retention boundary tests, mission-03 click/migration coordination, missing dogfood input and drill disruption are named with actions and owners. The 90-day default is an existing decision; a different retention mechanism remains mission-03 scope. |

## Empirical checks and limits

Fresh `rig workflow compile missions/02-brownfield --operation-key urlshort-02-brownfield-lifecycle-1 --json` equals the entire committed graph. `rig workflow show 01M40SN34E37K96B38JPG9K41X --json` confirms current reconciliation and receipt `revision-e7217bc8c78321b66bb68cce`. All five compiler source hashes and all 15 candidate files were checked against actual bytes; the wave queue and judge routing were read directly. Results are in `decomposition-verification.json`.

`rig scope audit --mission 02-brownfield` reports six low scaffold advisories: missing authored mini-requirements and proof contract for each slice. The graph likewise records unknown proof readiness. These are expected before the scheduled requirements steps; they neither block decomposition nor demonstrate implementation readiness. No build/test rerun was warranted for this documentation-only review.

## Findings and continuation

No MUST-FIX, HIGH, MEDIUM, LOW or INFO findings; no repair/backlog item created. The launch prerequisites, shared-file ordering, impact analyses, authored AC/proof contracts, defect territory adoption and drill triggers remain obligations at their named steps. Human plan-lock must decide the explicit three-slice proposal.

## Self-check

Independent of the producer; all 15 changed artifacts read and recorded; live graph/binding, source bytes, wave record and judge routes verified; no product, test, SPEC or design edits. Review ledger row appended. Exit `handoff` to the human mission plan-lock, with the decision brief at `missions/02-brownfield/SPEC.md`.
