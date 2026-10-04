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

The lead's 23:23Z instruction supersedes the initial proposal to route QA to
qa2. Both checks remain with the default owner, `qa-agent@urlshort-factory`,
and product QA takes priority. Neither the author nor a self-check supplies
the independent QA verdict.

## Independent rejection and remediation

QA inspected initial commit `594c9c9756e3a32a309d7a47dd91497ddcc31134`
and recorded MUST-FIX `DRILL1-01`: expected `READY`, observed `BROKEN`.
Its receipt is `docs/evidence/02-brownfield/drills/drill1-qa-594c9c9.md`
in the main checkout. The actual `qa_check failed` exit was authored by
`qa-agent@urlshort-factory` at 2026-10-03 23:29:04.090Z on packet
`qitem-20261003232132-8f1e1f16`; the generated back-edge is implement packet
`qitem-20261003232904-3c07d8d6`.

The worker reproduced the committed `BROKEN` value and changed only that
candidate field to `READY`. This new commit answers `DRILL1-01`; it requires
a fresh independent QA check before the instance can close. No QA pass is
claimed here.

## Self-check

The author reads the corrected JSON and verifies that it parses as the exact
object `{"drill":"DRILL 1","status":"READY"}`. The two-file Git diff stays under
`docs/evidence/02-brownfield/drills/` on the throwaway branch. Product builds,
HTTP, smoke, coverage and release readiness are outside this plain-file drill;
none is claimed. The final hop/packet record will be exported to the main
checkout after the independent rejection and successful re-check.
