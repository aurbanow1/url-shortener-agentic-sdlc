# DRILL 1 — independent QA re-check

Candidate: 8227b8c3bd87b757af377f8a029ebe68a1322452.
Workflow: 01M4212A8BKA6JRZHZQBD90D07; packet: qitem-20261003233041-b3b78d0d.
QA: qa-agent@urlshort-factory (Codex), 2026-10-03 23:31Z.

The committed candidate parses as the object `{"drill":"DRILL 1","status":"READY"}`. Both required values meet the unchanged rule. DRILL1-01 is fixed: comparison with rejected commit 594c9c9756e3a32a309d7a47dd91497ddcc31134 changes only the candidate's status from BROKEN to READY. Verdict: PASS for this plain-file drill; close done.

Reproduce: `git show 8227b8c3bd87b757af377f8a029ebe68a1322452:docs/evidence/02-brownfield/drills/drill1-candidate.json`, parse as JSON, and compare the drill and status values with the rule in the committed notes.

## Self-check

Independently checked exact worktree HEAD, parsed committed bytes, read the revised notes and unchanged pass rule, and compared the remediation to the rejected candidate. QA owns both attributed checks; the notes now confirm the lead's default-owner routing. This receipt closes the disclosed drill defect only. Product builds, HTTP behavior, coverage, and release readiness remain outside the plain-file drill and are not claimed.
