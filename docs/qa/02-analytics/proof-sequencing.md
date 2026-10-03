# Remaining proof records — 02-analytics

Candidate 862c52eea8294e438b1f98b832ae4f64f7a16923; QA packet qitem-20261003101510-d5f18be9.

QA can accept proof items 1–11 on the independently verified candidate. Item 12 requires both design and security review records for the memory-only daily salt, non-exposure and discard at its UTC-day end. The design review exists in docs/review/02-analytics/design-review.md, but the security review follows qa_check; do not accept item 12 before that record exists.

Item 13 expressly belongs to release_prep: measure redirect p95 with click recording enabled using 03-operate's bench, record its relation to NFR-L3, and state in docs/qa/GAPS.md if the added share cannot be isolated. The locked SPEC permits this pending numeric gap; AC-14's real Tomcat slow-write check is the compensating evidence. A green QA gate is not a p95 measurement.

Continuation for the lead: retain these obligations through combined review and release_prep, route the produced records back to QA for attributed judgments on the applicable commit subjects, and leave items 1–11 settled unless new evidence changes them. A-9 proxy alignment and CR-01 remain the already assigned lead backlog; QA grants no new scope or waiver.

## Re-check 5b3490c — current status

Candidate 5b3490c65915cf42594a4720350950bcefd2d7d0 repairs code-review CR-01's unit setup only; production and functional trees are identical. Fresh isolated/full QA gates passed. The explicit security salt record now exists at a3092bd in docs/review/02-analytics/02-security-review.md; QA read it and reran its actual-class lifetime probe on this candidate. Item 12 can now be accepted along with reaffirmed items 1–11. Only item 13 remains pending for release_prep. Lead continuation qitem-20261003103240-18e29a17 still owns the release measurement and any honest added-p95 gap; no new approval or waiver is needed.

## Release completion — merged 8e9c065

2026-10-03, packet qitem-20261003153201-3bc62f5f: QA independently audited the
corrected open-loop release bench and all retained JSON/status/click counts.
Item 13 accepted in receipt 00000025 against merged
8e9c065589e53385f60d6be3ddbc3683260285df. Sequential GET/HEAD p95 comparison
does not isolate added cost; final GAPS at 367567e states that limit under the
item's explicit allowance. No isolated numerical-cost guarantee or waiver.
Receipts 00000026–00000029 re-affirm unchanged claims in items 1, 4, 5 and 10
after shared-document changes. All 13 items are accepted; the lead's assigned
sequencing obligation is fulfilled. Evidence:
docs/qa/01-greenfield-core/release-judgments/ and proof/qa-release-8e9c065.md.
The mission's ship sign-off and failed 03-operate host AC-28 remain separate.
