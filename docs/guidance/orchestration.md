# Orchestration guidance — running the graph with governance

The assignment's critical differentiator: non-linear, stateful execution with
entry/exit gates, bounded retries, rollback, safe-stop, human checkpoints,
lineage and metrics. `docs/GOVERNANCE.md` maps each clause to the mechanism;
this guide says how the seats — above all the orchestration lead — use those
mechanisms well. Everything here was exercised on the hello mission; the
drills table (`docs/scenarios/drills.md`) is the evidence.

## 1. The graph is the plan

- A mission runs one lifecycle instance (`depends_on` DAG from `project.yaml`);
  each slice runs one `urlshort-slice` instance (`next_hop` routing). One live
  packet per instance: whoever holds it owns the next move.
- Entry gate = the previous step's deliverables exist at the named paths;
  exit gate = this step's deliverables (the role file lists them) plus an
  authored exit: `handoff`, `failed`, `waiting`, `done`.
- Lineage travels in the packet: candidate SHA, evidence refs, prior result
  notes. *Check:* every `rig workflow project … --result-note` names the SHA
  it judged or produced and the file a stranger should open next.

## 2. Exit semantics (get these right and the metrics are right)

| Exit | Meaning | Routes to |
|---|---|---|
| `handoff` | my deliverables are complete | next step |
| `failed` | **the artefact** failed my check (QA, review) | the producer (bounded loop) |
| `waiting` | I am blocked (human, another packet, external) — with a continuation | same step, re-presented when unblocked; **not a retry** |
| `done` | terminal step | — |

Your own trouble is never `failed`: block with `--blocked-on` and a
continuation, or escalate to the lead with a queue item.

## 3. Bounded retries and convergence

- `loop_guards.max_hops: 36` bounds a slice; a loop that approaches it is a
  planning failure, not a builder failure — the lead intervenes earlier.
- Only MUST-FIX/HIGH findings fail a check; MEDIUM/LOW ride along as
  recorded observations.
- Re-review is scoped to the findings and to what the fix touched; settled
  findings are not reopened without new evidence.
- Two failed loops on the same finding → the lead decides: route to a
  different producer, re-plan (split the slice), or park on the human with
  options. The decision and its reason go in `NOTES.md` and the packet note.
- A finding that is really a locked-SPEC question (QA-01 was one) goes to the
  owner of the lock — the lead for interpretation, the human for change.

## 4. The exception dial (least force first)

1. **Resume** — a seat stalled on an operator-only prompt, compaction or a
   crash: the stuck-sweep files a recovery packet to the lead; the operator
   approves the prompt or relaunches the seat; work resumes on the same
   packet. Nothing in the graph changes.
2. **Route** — the right next actor is not the one the routing suggests
   (`rig workflow project … --exit failed` back to `implement`, or a lead
   `route` to another role). Recorded as a closure with its reason.
3. **Abort** — the instance cannot produce the outcome as planned
   (`rig workflow abort`), followed by a new slice or a revised plan; the
   abort note states what was learned and what replaces it.

Record every use of the dial in `missions/<m>/NOTES.md` with the packet id —
that is where the metrics tool reads retries, routes and rollbacks.

## 5. Rollback

- Product: the integrator reverts the merge on `main` (`git revert -m 1 <merge>`),
  runs the gate, and routes the slice back to `implement`; a Flyway change is
  reversed by its written rollback migration. The release record (§8 of
  `RELEASE.md`) spells the commands out in advance.
- Process: a wrong stamp or decision is superseded, never deleted — new
  transition, new stamp (`--re-approve`), old ones kept.
- Evidence of a rollback is a `git log` entry, a transition note containing
  "revert"/"rollback", and a drills row.

## 6. Safe-stop

- Stop one seat without losing work: `rig seat stop <seat>` (the packet
  stays owned; the successor resumes from files — `PROGRESS.md`, `NOTES.md`,
  the packet). Relaunch with `rig seat launch --fresh --stop` only when the
  seat is idle and owns no open packet; verify the effect before any retry
  (a timed-out launch may still have succeeded).
- Stop the rig: `rig down <rig>`; in-flight packets remain in the queue and
  resume on `rig up`.
- Pause the human gate itself: do nothing — a parked packet is a safe state;
  "parked" is never "idle".
- Never stop a seat mid-commit: check `.git/index.lock` and the seat's
  activity first.

## 7. Dynamic re-planning

- `rig workflow revise` applies an authored graph change to a running instance;
  it **refuses** changes that would rewrite completed steps. Then: finish the
  instance on its graph and start the new structure with the next instance,
  or add a new slice. Keep the receipt (accepted or refused) in
  `docs/evidence/<mission>/` — a refusal is evidence of change control, not
  a failure.
- Standalone slice-workflow spec edits apply to in-flight instances at their
  next hop; mission profile edits apply to new instances.
- Upstream artefact changes (a SPEC or design after lock) are human decisions
  via a parked packet; the re-plan follows the decision, never precedes it.

## 8. Human checkpoints

- Gates are steps with `gate:`; the engine parks the packet on `human@kernel`
  with a summary and an `evidence_ref`. The producing step makes the evidence
  final *before* handing off; the human reads one page (decision brief).
- The decision text is recorded verbatim (transition note), stamped on the
  human's behalf (`--on-behalf-of human@kernel`), and copied into `NOTES.md`.
- Outside gates, any question that changes what gets built is a parked
  packet with options, a recommended default and an evidence path — not a
  chat message.
- Agents never approve their own gate, never publish, push, tag a release or
  expose a port beyond localhost.

## 9. Lineage and audit observability

- Files are the ledger: SPEC → design → PROGRESS/PROOF → QA findings and
  coverage → review files and the ledger → RELEASE.md → evidence export.
- Queue transitions and workflow trails are append-only and attributed;
  `tools/evidence-export.sh` snapshots them into the repo; `tools/sdlc-metrics.mjs`
  derives success rate, retries, rollbacks, MTTR, end-to-end latency and
  human wait from them — not from anyone's self-report.
- Commit messages describe the change and why it is correct, never who
  approved it; approvals live in stamps and transitions.

## 10. Parallelism without collisions

Concurrent slice instances on disjoint territories; shared files granted to
one slice at a time; `wave_integration` is the barrier. Do not parallelise
what shares a schema migration or a configuration file; serialise it in the
wave map instead. One reviewer and one QA seat are the real capacity limit.

## 11. Lead's checklist at every mission step

1. Which packet do I hold, which step, which evidence is final?
2. Is anyone parked who should not be (stuck-sweep, prompts)? Resume first.
3. Is a loop converging? If not, pick the lowest rung of the dial and record it.
4. Does the next human page say outcome, options, default, evidence, risks?
5. Are SHAs, packet ids and evidence refs in my result note?
6. Did I write it in `NOTES.md` before I compact or hand over?
