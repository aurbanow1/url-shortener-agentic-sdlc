# W2F-01 forward-fix: proof (dev2-agent, 2026-10-04 00:33–00:45Z)

**Candidate `0552b815e39b9899fa4fdaacf3ea3454e116720c`** on `fix/w2f-01`, from `main` `cda00ef`. One
file changed: `src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java`
(+11 −4). No product change.

## The change

`AC08_aPurgeRunsEveryUtcDayWithoutAnOperator` polled only the stored rows, and then read the captured
log. `ClickPurge.run` commits its `DELETE` and only after that logs `clicks purged`
(`ClickPurge.java:99-100`), so a poll could see the rows gone before the event was written. Now:

- The bounded loop (the same 60 s deadline, the same 100 ms poll) continues while the `T−90` rows are
  still stored **or** the captured window holds no `clicks purged` event. One loop condition covers
  both outcomes.
- Then the unchanged assertions run: the rows are exactly two of `T−89`; the window holds **exactly
  one** `clicks purged` event (`singleElement`) with `cutoff` = `T−89`.
- The event filter moved into a `runs(output, windowStart)` helper, so the loop and the assertion
  read the window the same way.
- There is no arbitrary sleep, no log suppression, no loosened assertion and no product change.

## Runs

| What | Command | Result | Log |
|---|---|---|---|
| targeted × 10 | `scripts/gw --offline -p .worktrees/w2f-01 functionalTest --tests "*ClickRetentionScheduleJourneyTest" --rerun-tasks` | 10 of 10 BUILD SUCCESSFUL (14–15 s each); the suite XML shows `tests="1" failures="0"`, so the filter ran the test | `run-01.txt` … `run-10.txt` |
| full gate | `scripts/gw --offline -p .worktrees/w2f-01 check --rerun-tasks` | BUILD SUCCESSFUL; unit 226, functional 250, 0 failures/errors/skips; merged lines 582/582, branches 206/206 | `check-0552b81.txt` |

## Not done, and why

- **Review's probe** (`docs/review/02-brownfield/proof/PurgePublicationProbe.java`) was not pointed
  at the fixed test. It is a standalone `main` that runs `ClickPurge` directly against its own
  in-memory table, with a barrier appender that holds the `clicks purged` event. It proves the gap
  exists, but it does not run the journey test. Using it against the test would mean injecting that
  barrier into the Spring test context (a test-only appender or bean), which is new test code beyond
  this finding's one-file territory. Not cheap, so not done.
- **No forced-interleaving red run** (the item says it is not required). Ten green targeted runs show
  the fix holds on this machine. They cannot prove the race is gone under every scheduling. The
  argument is by construction: the loop now exits only once the event it asserts on is present.
