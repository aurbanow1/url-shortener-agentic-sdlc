# W2F-01 independent QA — PASS

QA2 `qa2-agent@urlshort-factory` (Codex), 2026-10-04T00:48Z.
Packet `qitem-20261004003801-d75c4dbb`; exact candidate
`0552b815e39b9899fa4fdaacf3ea3454e116720c`, base `cda00ef`, worktree `.worktrees/w2f-01`.
Candidate remained exact and clean before and after all runs. Product/tests/build are read-only to QA.

The finding is HIGH W2F-01 in `docs/review/02-brownfield/proof/w2f-01-rework.md`:
row deletion becomes visible before its subsequent `clicks purged` event, so the original test
could falsely fail. That historical red result remains valid; these later passes do not erase it.

The only diff is `ClickRetentionScheduleJourneyTest.java`, +11/-4. QA read the full class and diff.
The existing loop continues while old rows remain **or** the post-window purge event is missing.
It can therefore exit successfully only after both observations, within the unchanged60-second
bound. The same100ms polling sleep remains; no new sleep, log suppression or product/build change.
The JSON event filter is extracted verbatim to a helper shared by the poll and assertion.
Exact surviving-row assertion, `singleElement` and exact `cutoff` assertion are retained.
The wait does not make a wrong cutoff, duplicate event or wrong stored result pass.
`source-check.json` and `candidate.diff` retain the scope/assertion checks.

Independent verification (no builder results substituted):

| Check | Result | Evidence |
|---|---|---|
| Targeted class ×10, each `--rerun-tasks` |10/10 BUILD SUCCESSFUL, one intended invocation each, zero failures/errors/skips |targeted-01…10.txt/xml/json; targeted-summary.json; commands.json |
| Full offline `check --rerun-tasks` |226 unit +250 functional, zero failures/errors/skips; all14 tasks executed; Javadoc green |check-0552b81.txt; full-junit/; full-gate-summary.json |
| Merged JaCoCo CSV |582/582 lines,206/206 branches,100/100 |full-coverage/all/jacocoTestReport.csv/xml |
| Separate suites |unit508/582 lines87.29%,198/206 branches96.12%; functional548/582 lines94.16%,171/206 branches83.01% |full-coverage/unit/ and functional/; merged gate unchanged |
| Preserved evidence hashes |109/109 raw/report/metadata files match SHA-256 |artifact-hashes.json (manifest and this README excluded) |

Actual saved structured output in each of the ten targeted runs and the full gate shows
one startup event (`deleted=0`, `cutoff=2026-07-06`) and one daily event (`deleted=2`,
`cutoff=2026-07-07`). The latter is inside the test's captured window, whose retained assertions
also require the exact two surviving next-cutoff rows and exactly one event. Source/terminal
assertions and saved log bodies were inspected, not only the BUILD SUCCESSFUL text.
Only canonical `test.exec` and `functionalTest.exec` existed at the full coverage gate.

Commands (run from repo root, wrapper's `--log` first):

```sh
scripts/gw --log <absolute-qa-path>/targeted-NN.txt --offline -p .worktrees/w2f-01 functionalTest --tests dev.urlshort.click.ClickRetentionScheduleJourneyTest --rerun-tasks
scripts/gw --log <absolute-qa-path>/check-0552b81.txt --offline -p .worktrees/w2f-01 check --rerun-tasks
```

The packet's `-p … --log …` order was rejected before tests because this wrapper only consumes
a leading `--log`; corrected order is recorded in command-order-note.md. The rejected command
is not counted among the ten executed tests. No wrapper/toolchain change was made.

## Self-check

Exact clean SHA and one-file territory confirmed; product/build diff empty; original deadline,
poll and terminal assertions unchanged. Ten independent executed class runs and one full gate
passed; every run's intended method/XML and stored/logged outcomes audited; CSVs summed directly;
all saved hashes rechecked. No product/test edit, threshold change, log suppression or arbitrary
new delay. No application launched outside Gradle; test contexts shut down in the captured logs.

Not independently injected: the review's forced log-publication barrier against the fixed test.
Ten passes establish these observed runs; no claim of exercising every possible scheduler
interleaving. The repair also closes the reproduced gap by its condition: rows alone cannot end
polling while the required event is absent. Per packet this is **not** a slice proof contract:
no `rig proof judge`. Closure records PASS; lead merges with a fresh gate and wave review rechecks.
