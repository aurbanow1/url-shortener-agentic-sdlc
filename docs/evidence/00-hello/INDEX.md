# Evidence export — 00-hello

Exported 2026-10-03T03:02:14Z by tools/evidence-export.sh from OpenRig 0.6.3 (8b5e9488).

| Artifact | Governance clause (docs/GOVERNANCE.md) |
|---|---|
| compiled-graph.json | Explicit dependency graph with entry/exit gates: the mission DAG as compiled from the authored sources at export time (if a running instance was bound to an earlier version, the lead keeps that version here and the disk compile beside it as compiled-graph.authored-*.json; see Dynamic re-planning) |
| instances/*.trace.json | Cross-stage context and decision lineage; Bounded retries: the append-only step trail, one entry per closed packet with closureReason handoff / waiting / failed / done, actor and evidence_ref; failed → implement hops are the retries |
| instances/*.show.json | Dynamic re-planning; Fallback: bound sources and digests, revisionHistory (rig workflow revise receipts), the reconciliation block comparing the bound graph with the authored one, exception routing, resumeCount |
| packets/*.transitions.json | Human approval checkpoints; Safe-stop; Audit-grade observability: every state change of every packet with actor and timestamp, including the engine's gate park on human@kernel and the human's decision text on rig queue resolve |
| packets/*.show.json | Human approval checkpoints: the packet as last seen, with summary, evidence_ref, tier, tags (step, gate) and chain of record |
| proof-readiness.json | Controlled agent autonomy; Audit-grade observability: attributed proof judgments per slice (rig proof judge receipts, judge seat, subject commit, evidence hashes) and readiness |
| scope-audit.json | Policy guardrails: convention audit of mission and slice files (advisory) |
| workflow-status.json / workflow-list.json | Sequential and parallel paths: instance states and attention classes at export time (overlapping instance timestamps show pipeline parallelism) |
| queue-active.json | Safe-stop: live queue rows at export time (what was still held or parked) |
| usage-top.json | Reliability metrics: per-seat token burn over the window (24 h) |
| ../../metrics/metrics.json, ../../metrics/README.md | Reliability metrics: success rate, retries, rollbacks, MTTR, latency, human wait derived from these files by tools/sdlc-metrics.mjs |

Approval stamps are not in this export: they live in the stamped files' frontmatter (missions/<m>/SPEC.md, slices/*/SPEC.md: approved-spec-*, approved-*) with append-only audit rows daemon-side; the decision text behind each stamp is in the gate packet's transitions here.

Instances exported: 2; packets exported: 23.

## Packets (workflow · step · state · owner)

| Packet | Workflow | Step | State | Owner |
|---|---|---|---|---|
| qitem-20261002212902-0f6e128b | 00-hello | decompose | handed-off | orchestration-lead |
| qitem-20261002213817-6d848ac6 | 00-hello | mission_plan_lock | handed-off | orchestration-lead |
| qitem-20261002220222-65f568f1 | 00-hello | wave_integration | handed-off | orchestration-lead |
| qitem-20261002220259-281a4efc | urlshort-slice | requirements | handed-off | requirements-agent |
| qitem-20261002221159-a35e50bc | urlshort-slice | design | handed-off | design-agent |
| qitem-20261002225126-b5c94d4f | urlshort-slice | design_review | done | review-agent |
| qitem-20261002225911-1ad25865 | urlshort-slice | design | handed-off | design-agent |
| qitem-20261002230841-dc5cdf2b | urlshort-slice | design_review | handed-off | review-agent |
| qitem-20261002231236-cdda3066 | urlshort-slice | plan_lock | handed-off | design-agent |
| qitem-20261002232943-d0903176 | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261002235024-355f67c0 | urlshort-slice | qa_check | done | qa-agent |
| qitem-20261003003003-9cb0ed55 | urlshort-slice | implement | handed-off | development-agent |
| qitem-20261003004117-c920fe49 | urlshort-slice | qa_check | handed-off | qa-agent |
| qitem-20261003005806-5aaa453e | urlshort-slice | code_review | handed-off | review-agent |
| qitem-20261003010318-807bbf8a | urlshort-slice | security_review | handed-off | review-agent |
| qitem-20261003013220-670f8532 | urlshort-slice | integrate | handed-off | orchestration-lead |
| qitem-20261003013423-99b71ff1 | urlshort-slice | slice_accept | done | qa-agent |
| qitem-20261003015049-51d1ba00 | 00-hello | wave_review | handed-off | review-agent |
| qitem-20261003020412-57cea2d9 | 00-hello | release_prep | handed-off | release-agent |
| qitem-20261003023502-f807af1f | 00-hello | ship_signoff | handed-off | release-agent |
| qitem-20261003024639-d36d76cc | 00-hello | evidence_export | in-progress | release-agent |
| qitem-recovery-1500f49a88f65f64 | - | - | done | orchestration-lead |
| qitem-recovery-a562baedab298be5 | - | - | blocked | orchestration-lead |

## Step trails (closed at · step · exit · packet · actor)

### 01M3Z8AJ1EDFTHQ1HPAWPNP2YN

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T02:46:39.191Z | ship_signoff | handoff | qitem-20261003023502-f807af1f | release-agent |
| 2026-10-03T02:35:02.387Z | release_prep | handoff | qitem-20261003020412-57cea2d9 | release-agent |
| 2026-10-03T02:04:12.497Z | wave_review | handoff | qitem-20261003015049-51d1ba00 | review-agent |
| 2026-10-03T01:50:49.320Z | wave_integration | handoff | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T01:34:35.690Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T01:03:31.983Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:58:22.054Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:41:36.327Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-03T00:30:18.276Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:50:40.073Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:30:00.966Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:12:47.752Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T23:08:58.502Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:59:27.677Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:52:38.946Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:12:19.674Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:03:49.310Z | wave_integration | waiting | qitem-20261002220222-65f568f1 | orchestration-lead |
| 2026-10-02T22:02:22.490Z | mission_plan_lock | handoff | qitem-20261002213817-6d848ac6 | orchestration-lead |
| 2026-10-02T21:38:17.102Z | decompose | handoff | qitem-20261002212902-0f6e128b | orchestration-lead |

### 01M3ZA8Q39QEB3R1QDQCVTER18

| Closed at | Step | Exit | Packet | Actor |
|---|---|---|---|---|
| 2026-10-03T01:49:36.477Z | slice_accept | done | qitem-20261003013423-99b71ff1 | qa-agent |
| 2026-10-03T01:34:23.192Z | integrate | handoff | qitem-20261003013220-670f8532 | orchestration-lead |
| 2026-10-03T01:32:20.584Z | security_review | handoff | qitem-20261003010318-807bbf8a | review-agent |
| 2026-10-03T01:03:18.901Z | code_review | handoff | qitem-20261003005806-5aaa453e | review-agent |
| 2026-10-03T00:58:06.246Z | qa_check | handoff | qitem-20261003004117-c920fe49 | qa-agent |
| 2026-10-03T00:41:17.523Z | implement | handoff | qitem-20261003003003-9cb0ed55 | development-agent |
| 2026-10-03T00:30:03.244Z | qa_check | failed | qitem-20261002235024-355f67c0 | qa-agent |
| 2026-10-02T23:50:24.208Z | implement | handoff | qitem-20261002232943-d0903176 | development-agent |
| 2026-10-02T23:29:43.738Z | plan_lock | handoff | qitem-20261002231236-cdda3066 | design-agent |
| 2026-10-02T23:12:36.097Z | design_review | handoff | qitem-20261002230841-dc5cdf2b | review-agent |
| 2026-10-02T23:08:40.997Z | design | handoff | qitem-20261002225911-1ad25865 | design-agent |
| 2026-10-02T22:59:11.100Z | design_review | failed | qitem-20261002225126-b5c94d4f | review-agent |
| 2026-10-02T22:51:26.673Z | design | handoff | qitem-20261002221159-a35e50bc | design-agent |
| 2026-10-02T22:11:59.875Z | requirements | handoff | qitem-20261002220259-281a4efc | requirements-agent |


## Notes for this export (release agent, `evidence_export`, 2026-10-03)

- **Two compiled graphs.** `compiled-graph.json` is the eight-step graph the
  lifecycle instance `01M3Z8AJ1EDFTHQ1HPAWPNP2YN` actually ran on (bound
  version `1-4322186fed9c15a8`: decompose → mission_plan_lock →
  wave_integration → wave_review → release_prep → ship_signoff →
  evidence_export → mission_close). `compiled-graph.authored-1-32dce218.json`
  is what `project.yaml` on disk compiled to at export time: ten steps, adding
  `decomposition_review` and `release_review`. The operator added those steps
  while this instance was running; `rig workflow revise` reported the change
  incompatible (a completed step would gain a new predecessor) and the lead
  adopted nothing, so 00-hello finished on its original graph. The
  `reconciliation` block in `instances/01M3Z8AJ1EDFTHQ1HPAWPNP2YN.show.json`
  records both digests and the reasons. Governance clause: dynamic re-planning,
  refused case.
- **Two instances.** The lifecycle above and the slice instance
  `01M3ZA8Q39QEB3R1QDQCVTER18` (`urlshort-slice`, tier high) launched by
  `wave_integration`. The slice trail holds the two bounded retries of the
  mission: `design_review` failed (DR-01, 22:59Z) → `design` again; `qa_check`
  failed (QA-01, 00:30Z) → `implement` again. Governance clause: bounded
  retries.
- **Three human decisions, verbatim in the transitions:**
  `packets/qitem-20261002213817-6d848ac6.transitions.json` (mission plan-lock,
  21:59:33Z), `packets/qitem-20261002231236-cdda3066.transitions.json` (slice
  plan-lock, 23:27:04Z), `packets/qitem-20261003023502-f807af1f.transitions.json`
  (ship sign-off, 02:44:55Z). Each shows the engine's park on `human@kernel`
  and the human's `rig queue resolve` text. Governance clause: human approval
  checkpoints.
- **Stamps.** Mission plan-lock: `missions/00-hello/SPEC.md` frontmatter
  (`approved-spec-at 2026-10-02T22:02:45Z`). Slice plan-lock:
  `slices/01-ping/SPEC.md` (`approved-spec-at 2026-10-02T23:27:40Z`).
  Delivery: `slices/01-ping/SPEC.md` (`approved-at 2026-10-03T02:45:45Z`,
  action `01M3ZTEF1C876Q09Q5ND34N8KZ`, recorded by the lead on the human's
  behalf after the ship decision). Governance clause: controlled agent
  autonomy.
- **Fallback and recovery packets.** `packets/qitem-recovery-a562baedab298be5.*`
  is the stuck-sweep finding on the design step (a seat at a permission
  prompt), diagnosed by the lead. Two later findings of the same class landed
  on the release seat: `qitem-recovery-1500f49a88f65f64` (parked on the
  `evidence_export` packet) is exported here; `qitem-recovery-0771ba919d4f9545`
  (on the closed `release_prep` packet) is not referenced by any trail and so
  is not. The lead's notes in `missions/00-hello/NOTES.md` §2 record all
  three. Governance clause: fallback.
- **What this export cannot contain.** Its own step's closure
  (`evidence_export`) and `mission_close`, which happen after it. The lead may
  re-run `tools/evidence-export.sh 00-hello` at close; `compiled-graph.json`
  must then be restored to the bound version again (the script compiles from
  disk).
- **Rollback.** No revert happened on `main` (rollbacks 0 in the metrics is
  genuine). The rollback path was rehearsed on a throwaway branch at
  `evidence_export`: drill row in `docs/scenarios/drills.md`, gate log
  `missions/00-hello/release/rollback-rehearsal-check.txt`. Governance clause:
  rollback.
