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
  prompt), diagnosed by the lead; two later findings of the same class on the
  release seat (`qitem-recovery-0771ba919d4f9545`, `qitem-recovery-1500f49a88f65f64`)
  are not referenced by any trail and so are not exported here; the lead's
  notes in `missions/00-hello/NOTES.md` §2 and the natural-failures table in
  `docs/scenarios/drills.md` record them. Governance clause: fallback.
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
