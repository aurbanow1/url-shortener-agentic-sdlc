# DRILL 1 — deliberate QA rejection and remediation

Instance: `01M4212A8BKA6JRZHZQBD90D07`, workflow `urlshort-drill@1`.
Tracking item: `qitem-20261003154458-3d6d20e7`; lead decision transition
`1602`, 2026-10-03 23:18Z, orders the fallback now.

Worktree: `.worktrees/drill-qa-remediation`, branch `drill-qa-remediation`,
created off local `main` at `88d7975`. Only this plain-file drill candidate
and its notes are changed here. No product code or product slice is changed.

## Pass rule

Read [drill1-candidate.json](drill1-candidate.json) from the exact committed
candidate. It must parse as a JSON object with `drill` exactly `DRILL 1` and
`status` exactly `READY`. A value other than `READY` fails the rule.

## Initial candidate — deliberately defective

The first candidate has `status: BROKEN`. This is the disclosed injected
defect, not a product defect. QA must inspect the actual file and use the
workflow's `failed` exit with that finding. The back-edge then returns the
work to `implement`; the drill worker changes the value to `READY` in a new
commit and hands that commit to QA for an independent re-check. QA's `done`
exit is appropriate only for the corrected candidate that meets the rule.

The orchestration lead routes each generated `qa_check` packet to
`qa2-agent@urlshort-factory`; the release agent supplies its packet id.
Neither the author nor a self-check supplies the independent QA verdict.

## Self-check

The author reads the committed JSON and verifies that it parses and that the
deliberately defective status is `BROKEN`. The two-file Git diff stays under
`docs/evidence/02-brownfield/drills/` on the throwaway branch. Product builds,
HTTP, smoke, coverage and release readiness are outside this plain-file drill;
none is claimed. The final hop/packet record will be exported to the main
checkout after the independent rejection and successful re-check.
