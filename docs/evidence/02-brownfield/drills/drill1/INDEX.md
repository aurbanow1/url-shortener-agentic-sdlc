# DRILL 1 evidence — QA rejection and remediation

Completed 2026-10-03 23:32:07.695Z. Instance
`01M4212A8BKA6JRZHZQBD90D07`, workflow `urlshort-drill@1`.
This is the mission SPEC's labelled fallback: a disclosed plain-file fault,
independent rejection, one remediation and independent successful re-check.

The release worker authored candidate `594c9c9756e3a32a309d7a47dd91497ddcc31134`
with `status: BROKEN`; qa-agent recorded MUST-FIX DRILL1-01 and exited
`failed`. The worker committed `8227b8c3bd87b757af377f8a029ebe68a1322452`
with `status: READY`; qa-agent checked that exact commit and exited `done`.
Both candidates have `drill: DRILL 1`, and the pass rule remained unchanged.
The worker and checker are separate seats, both running Codex; this drill
makes no claim of cross-runtime product review.

The lead authorized this fallback in tracking transition **1602** at
23:18:29.984Z. Its initial QA2-routing proposal was superseded by the lead's
23:23Z instruction to keep the default qa-agent owner with product QA first.
The tables below show the actual actors. A QA packet's queue state `done`
means its obligation closed; its verdict is the workflow trail's `failed`
or `done`, and the QA receipt.

## Evidence and governance

The clauses below are from [GOVERNANCE.md](../../../../GOVERNANCE.md).
Raw JSON and Git output were captured on 2026-10-03 at 23:33Z using separate
commands. This export covers this drill only; the full mission evidence and
metrics are refreshed at their assigned lifecycle steps.

| Artifact | Governance clause and what it demonstrates |
|---|---|
| [Workflow source](urlshort-drill.workflow.yaml), read from commit `c0ba6b6` | Explicit dependency graph; bounded retries: implement → QA, failed → implement, `max_hops: 8`, no spawned subworkflows |
| [Final trace](instances/01M4212A8BKA6JRZHZQBD90D07.trace.json), [final instance](instances/01M4212A8BKA6JRZHZQBD90D07.show.json) | Bounded retries; cross-stage context and decision lineage; audit-grade observability: actual four step closures, actors, evidence references, final completed state |
| [Initial worker transitions](packets/qitem-20261003231955-aed54014.transitions.json), [packet](packets/qitem-20261003231955-aed54014.show.json) | Cross-stage context and decision lineage: labelled entry objective and first exact candidate handoff |
| [Rejected QA transitions](packets/qitem-20261003232132-8f1e1f16.transitions.json), [packet](packets/qitem-20261003232132-8f1e1f16.show.json), [independent rejection receipt](../drill1-qa-594c9c9.md) | Review at every stage; bounded retries: actual failed verdict generates the worker back-edge |
| [Remediation transitions](packets/qitem-20261003232904-3c07d8d6.transitions.json), [packet](packets/qitem-20261003232904-3c07d8d6.show.json) | Cross-stage context and decision lineage: failed finding answered by a new candidate SHA |
| [Re-check transitions](packets/qitem-20261003233041-b3b78d0d.transitions.json), [packet](packets/qitem-20261003233041-b3b78d0d.show.json), [independent pass receipt](../drill1-qa-8227b8c.md) | Review at every stage; audit-grade observability: independent successful re-check of the corrected committed bytes |
| [Tracking item transitions](tracking-item.transitions.json) | Controlled agent autonomy; safe-stop: lead's fallback authorization, parked continuations, blocker resolution and pickup history through export time; this snapshot precedes the tracking item's final documentation closure |
| [Initial candidate](initial/drill1-candidate.json), [initial notes](initial/drill1-notes.md) | Policy guardrails; audit-grade observability: exact disclosed injected fault and stated pass rule |
| [Corrected candidate](corrected/drill1-candidate.json), [corrected notes](corrected/drill1-notes.md), [candidate diff](candidate-diff.patch), [branch history](branch-history.txt) | Policy guardrails; audit-grade observability: exact remediation, two documentation paths only, no product changes |
| [Scenario record](../../../../scenarios/drills.md) | Bounded retries: labelled observation alongside the separately recorded other drills |

Reproduce the raw exports with `rig workflow trace <instance> --json`,
`rig workflow show <instance> --json`, and for each of the four tabled packets
`rig queue transitions <packet> --json` and
`rig queue show <packet> --full --json`. The tracking history uses the same
transitions command for `qitem-20261003154458-3d6d20e7`.
Candidate and note snapshots use `git show <full-sha>:docs/evidence/02-brownfield/drills/<filename>`;
the history uses `git log --format=fuller --stat 88d7975..8227b8c`.
The original worktree evidence references in the trace refer to the notes at
their candidate SHAs; both versions are preserved above.

The isolated branch `drill-qa-remediation` and worktree
`.worktrees/drill-qa-remediation` are retained at `8227b8c`. Their candidate
bytes and history are also exported here, so reading this record does not
depend on keeping that worktree. Nothing was merged from the drill branch.

## Self-check

Compared exported candidates to both committed versions and parsed the actual
objects: initial BROKEN, corrected READY. Checked the unchanged pass rule,
both independent QA receipts, the chronological handoff → failed → handoff →
done trail and the four linked packet histories. Confirmed final instance
status completed, zero frontier packets, and a two-document candidate diff.
The evidence links resolve in the repository. No product build, installed
smoke, coverage or release-readiness claim is made by this plain-file drill.
Nothing was pushed, tagged or published.

Packet and trail tables below are generated by
`node tools/evidence-index.mjs docs/evidence/02-brownfield/drills/drill1`.

## Packets (workflow · step · state · owner)

| Packet | Workflow | Step | State | Owner |
|---|---|---|---|---|
| qitem-20261003231955-aed54014 | urlshort-drill | implement | handed-off | release-agent |
| qitem-20261003232132-8f1e1f16 | urlshort-drill | qa_check | done | qa-agent |
| qitem-20261003232904-3c07d8d6 | urlshort-drill | implement | handed-off | release-agent |
| qitem-20261003233041-b3b78d0d | urlshort-drill | qa_check | done | qa-agent |

## Step trails (closed at · step · exit · packet · actor)

### 01M4212A8BKA6JRZHZQBD90D07

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T23:32:07.695Z | qa_check | done | qitem-20261003233041-b3b78d0d | qa-agent |
| 2026-10-03T23:30:41.601Z | implement | handoff | qitem-20261003232904-3c07d8d6 | release-agent |
| 2026-10-03T23:29:04.090Z | qa_check | failed | qitem-20261003232132-8f1e1f16 | qa-agent |
| 2026-10-03T23:21:32.117Z | implement | handoff | qitem-20261003231955-aed54014 | release-agent |
