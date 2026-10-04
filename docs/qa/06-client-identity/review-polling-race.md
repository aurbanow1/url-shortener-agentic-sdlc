# Post-handoff qualification — delayed-writer polling race

Candidate: `fb63a88a9b92c1fec97ba74686af1a2f30304160`.
Source: review2's `docs/review/06-client-identity/StatsPollingProbe.java` and
`polling-probe-fb63a88.txt`, read by QA2 after the02:47Z notification.
Their hashes are frozen in `review-polling-race-source.json`.

The reviewer calls the candidate's actual `assertGroupsAsTheReference`/`settledStats`
helper with its clock frozen and the real writer temporarily blocked. Its default-peer
statistics polls spend the create60 budget already charged for fixture creation.
The next response is429; the helper assumes `totalClicks` exists and throws NPE
after170ms, before its10-second wait. The recorded61 statistics requests include
the subsequent fresh-peer positive control. After release that control gets200,
clicks3, one2026-10-01 day, uniqueVisitors2 and botClicks0. The external probe
task succeeds because it asserts this reproduction; it is not a failing590-test
JUnit gate. The reviewer owns the formal HIGH finding and workflow failed exit.

This is credible new evidence against the reliability of the new AC-5
characterization. QA's own fresh268/322 gate and original/candidate business
effects remain observations of the runs actually performed. QA did not run this
reviewer's probe independently and does not claim to have done so. In QA's
delayed-writer experiment, the fixture settled the recorder before querying
statistics; it therefore did not expose this polling-helper/budget interaction.
The omission matters and is now explicit.

QA withdraws the broad functional-completion/characterization acceptance
judgments for proof items1 and11. Other by-effect/data/coverage/custody facts
are not retracted as product failures; item16 still awaits the already agreed
downstream review/merge/register sequence. The earlier PASS drop and immutable
captures remain as historical evidence, not authority to merge past this HIGH.
The current candidate is not accepted for integration until the builder fixes
the test and a new assigned QA/review packet establishes the resolution.

No product, build file or test was edited by QA. No unassigned candidate
re-review or test run was started. No gap waiver is requested.

## Self-check

Read actual probe source, output429/NPE and fresh-peer200 positive control;
distinguished test-helper failure from product grouping; recorded reviewer
attribution and own untested boundary; preserved prior captures; withdrew
affected judgments on the exact SHA; informed reviewer/lead and committed
the qualification and receipts with explicit pathspecs.
