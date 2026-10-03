# QA custody boundary

QA packet `qitem-20261003214414-12d75069` names candidate
`a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1` (X), on old base `16312da`.
The lead's mission `NOTES.md` entry at 21:42Z, implementing operator packet
`qitem-20261003213839-e9da218c`, explicitly authorizes complete-candidate
QA and review before the rebase onto audit-read's merge.

Audit-read subsequently merged at `cb148c4`. X is deliberately left unchanged
through QA and review. Proof item 9 is unjudged on X; this is a sequencing
obligation, not an accepted ancestry claim.

Before acceptance, the lead must obtain X′ from the builder, verify
`git range-diff 16312da..X cb148c4..X′` under the stated exception for context
in `application.properties` and `README.md`, and run a fresh
`scripts/gw --offline check --rerun-tasks` on X′. Any other changed patch goes
back to QA for the changed part. Return proof item 9 to QA against X′ for
the actual ancestry check and next Flyway number. Items 1–8 and 10 remain
attributed to X under the lead's explicit rule; acceptance and merge name both
SHAs and the accepted tag targets X′.

## Self-check

Read the packet and the lead's amendment, confirmed X at the worktree HEAD,
and recorded the unresolved item explicitly. No ancestry judgment or rebase
was manufactured during QA.
