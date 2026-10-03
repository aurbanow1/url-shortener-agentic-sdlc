# Remaining proof records — 02-analytics

Candidate 862c52eea8294e438b1f98b832ae4f64f7a16923; QA packet qitem-20261003101510-d5f18be9.

QA can accept proof items 1–11 on the independently verified candidate. Item 12 requires both design and security review records for the memory-only daily salt, non-exposure and discard at its UTC-day end. The design review exists in docs/review/02-analytics/design-review.md, but the security review follows qa_check; do not accept item 12 before that record exists.

Item 13 expressly belongs to release_prep: measure redirect p95 with click recording enabled using 03-operate's bench, record its relation to NFR-L3, and state in docs/qa/GAPS.md if the added share cannot be isolated. The locked SPEC permits this pending numeric gap; AC-14's real Tomcat slow-write check is the compensating evidence. A green QA gate is not a p95 measurement.

Continuation for the lead: retain these obligations through combined review and release_prep, route the produced records back to QA for attributed judgments on the applicable commit subjects, and leave items 1–11 settled unless new evidence changes them. A-9 proxy alignment and CR-01 remain the already assigned lead backlog; QA grants no new scope or waiver.
