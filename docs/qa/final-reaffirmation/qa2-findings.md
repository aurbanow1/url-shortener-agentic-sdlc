# Real content changes in final proof reconciliation

Packet `qitem-20261004041605-18a958ae` authorizes reaffirmation only when the
slice's own content is unchanged. Comparison against the exact retained
evidence hashes found these substantive document changes at final main
`30f8de4e`. They may be correct, already completed corrections; this report
does not assert a new product failure. Their current judgments remain unknown,
rather than being accepted as mere whole-file hash drift.

| Finding | Slice / items | Before → current | Evidence |
|---|---|---|---|
| RF-01 | 03-operate #5, #13 | Its own disk-gauge gap changed from LOW retained to CLOSED by 03-dogfood-fix, with new baseline/candidate effects cited. The release raw files, stamped SPEC, RISKS and prior human-exception audit still match their retained hashes. | GAPS own row “Anonymous disk-gauge working-directory path”; old whole-file SHA c3be44240a44… at 15a8f9c. |
| RF-02 | 02-click-retention #5 | Its deferred click/user_agent_class audit-column rows changed from OPEN until retention merges to CLOSED, with V3/merge/test attribution. The slice's separate QA gap section remains identical. | GAPS deferred-column rows; old whole-file SHA ab9983a9a8f6… at 84d3604. |
| RF-03 | 02-click-retention #10 | ADR-0011/0018 decision text now distinguishes a click writer that interrupts after its drain deadline from a purge that never interrupts. The prior text said neither interrupts a JDBC call. DESIGN's ADR-0018 summary also adds the purge hold. These exceed status/merge metadata; ADR-0020/0013 changes are status-only. | ADR-0011 amendment shutdown paragraph, ADR-0018 Shutdown consequence, DESIGN §7 ADR-0018 row. Retained versions and hashes in qa2-version-lookup.json. |

Reproduce by comparing each retained version identified in
`qa2-version-lookup.json` with `git show 30f8de4e:<path>`; the original receipt
evidence/hash list is in `qa2-input.json`. `qa2-section-checks.json` records
the narrower top-level slice-section comparisons, including the unchanged
retention QA section; it does not omit the changed deferred rows above.

Six unchanged-content items can be reaffirmed: 03-operate #4,
02-click-retention #4, 04-audit-columns #4/#5, 05-ci-cd #5/#6.
Accepted items are untouched. No new test, app, hosted-workflow or load run.

Requested routing: return a small content-verification packet for the four
items using the already committed dogfood, merge/closure and shutdown-correction
evidence, or route an actual erroneous claim to its author. This respects the
current packet's distinction between content change and hash-only drift.

## Assigned content verification

Follow-up `qitem-20261004042358-6587e4be` authorized verification against each later correction and review. RF-01, RF-02 and RF-03 passed the source/evidence comparisons recorded in `qa2-content-checks.json`; all four original item claims still hold. The original unknown states above describe the pre-verification snapshot. Reaffirmations renew only those original claims and preserve attribution to the later slices and reviewers. They add no application run, benchmark or hosted verification.

For RF-03, Git history resolves the ADR-0018 index hold addition specifically to `4975d25f`; the named `2d3de57`/`3b2ecd0b` upkeep retains it. The pre-implementation ADR already defines the hold. This precise provenance supplements the follow-up packet without changing its scope. Final receipts and readiness are recorded in `qa2.md`.
