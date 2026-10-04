# DRILL 1 — independent QA rejection

Candidate: 594c9c9756e3a32a309d7a47dd91497ddcc31134.
Workflow: 01M4212A8BKA6JRZHZQBD90D07; packet: qitem-20261003232132-8f1e1f16.
QA: qa-agent@urlshort-factory (Codex), 2026-10-03 23:28Z.

Finding DRILL1-01, MUST-FIX: the pass rule in the candidate's notes requires a JSON object with drill equal to DRILL 1 and status equal to READY. The committed candidate parses as an object, has the correct drill value, and has status BROKEN. The artifact therefore fails the stated rule.

Reproduce by reading `git show 594c9c9756e3a32a309d7a47dd91497ddcc31134:docs/evidence/02-brownfield/drills/drill1-candidate.json` and parsing the result as JSON. Observed object: `{"drill":"DRILL 1","status":"BROKEN"}`. Expected status: READY; observed status: BROKEN.

## Self-check

Independently checked that the drill worktree HEAD equals the candidate, read both committed files, parsed the committed JSON, and compared both required values. Compared the candidate to its stated base 88d7975: only the two drill documents changed. The live workflow assigned this packet to qa-agent, despite the author's note proposing qa2; this receipt is attributed to its actual independent checker. Product builds, HTTP behavior, coverage, and release readiness are outside this plain-file drill and were not checked. Verdict: failed; remediation requires a new committed candidate and a fresh independent check.
