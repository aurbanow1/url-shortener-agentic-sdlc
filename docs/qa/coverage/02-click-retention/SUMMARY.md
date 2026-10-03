# QA coverage — 02-click-retention

QA2 / Codex, 2026-10-03. **PASS on candidate
`a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1` (X).** Proof item 9 remains
pending the authorized rebase X′; this is not a slice acceptance or ancestry claim.

Fresh verification, from the worktree at exactly X:

```sh
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/qa/02-click-retention/check-qa-a8fc8b6.txt --offline check --rerun-tasks
```

BUILD SUCCESSFUL, 14 tasks executed. Unit **174**, functional **172**;
0 failures, errors or skips. Javadoc and merged coverage verification passed.
Counts below are independently summed from the committed CSV files.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|
| Unit | 447/490 | 91.22% | 168/168 | 100.00% |
| Functional | 458/490 | 93.47% | 135/168 | 80.36% |
| Merged | 490/490 | 100.00% | 168/168 | 100.00% |

The policy gate is merged coverage. Each suite's shortfall is explicitly recorded
in `docs/qa/GAPS.md`; no exclusion or threshold change was made.
All **333** copied HTML/XML/CSV report files match their source SHA-256 hashes,
checked again after the auxiliary probes. The report manifest is
[`qa-coverage-audit-a8fc8b6.json`](../../../../missions/02-brownfield/slices/02-click-retention/proof/qa-coverage-audit-a8fc8b6.json).
The canonical reports were copied before the auxiliary replay added its own
execution data; the saved merged report belongs to the fresh canonical gate.

The unchanged original `f6dd29e` functional suite also passed against X:
**155 tests, 0 failures/errors/skips**. All 24 original Java source files equal
both their `f6dd29e` Git blobs and the candidate's inherited files. The replay
used the granted candidate functional profile pause setting. Its Gradle init
script and original-suite XML/HTML reports are saved in `proof/`.
Traceability maps all **210** source test methods (97 unit, 113 functional),
every AC-1..16 and BR-1..7. Parameterization accounts for the invocation counts.

Independent effects capture:
[`qa-effects-final-a8fc8b6/`](../../../../missions/02-brownfield/slices/02-click-retention/proof/qa-effects-final-a8fc8b6/),
249 assertions passed in the corrected driver run; 207 captured HTTP responses
all have their `X-Request-Id` in a JSON product log event. The driver and external
Java harness are saved; no product or canonical test source was edited.

- AC-1/2: real redirected clicks at four days; strict cutoff left six unchanged
  rows, then four on the next day. AC-3: actual environment value 7 retained
  the boundary and today, deleted day T−8.
- AC-4: actual environment values 0, −5 and `ninety` each exited unsuccessfully,
  served no observed request and preserved all original click rows. The failure
  analysis named its setting/value without another configuration value.
- AC-5/6: 96 daily redirects reduced to 91 retained days with unchanged counts,
  four-field statistics and retained referrers. A fully purged link returned
  empty statistics, then one click after a 302; creation body and all audit rows
  stayed identical.
- AC-7/9/13: the rebuilt real f6dd29e jar wrote links, an audit retirement and a
  current click; stopped-directory v1 inserts supplied old days. X then upgraded
  that same directory. At readiness, before another redirect, UTC day counts
  were 1 each for 2026-06-05, 07-04, 07-05, 09-23 and 10-03 before start, and 1
  each for 07-05, 09-23 and 10-03 after start. One startup INFO reported deleted=2,
  cutoff=2026-07-05, retentionDays=90, with no request/client values.
- AC-8: no-trigger daily crossing at 00:10Z removed the expired row in 3.651 real
  seconds, at one second past due on the controlled service clock. No run before
  due, and no second run on the same day.
- AC-9/10: zero and positive runs each emitted one INFO. An actual JDBC
  prepareStatement failure produced one WARN with exception class only; neither
  the driver canary nor its stack was logged. Redirect/statistics remained
  available, and a healthy subsequent run removed the old rows.
- AC-11: 5,000 old rows; an uncommitted old-row deletion held the actual global
  purge DELETE inside H2. A real HTTP 302 and today's writer commit completed
  while that statement remained pending. Releasing the blocker allowed the
  purge to finish; statistics showed exactly the new click.
- AC-12: an actual empty HMAC key caused reduction to fail before queueing.
  The 302 carried the same requestId as the single WARN, reason `reduction failed`,
  errorType `java.lang.IllegalArgumentException`, with no exception message.
- AC-15: environment pause=false deleted nothing at 60 service seconds after
  readiness and 60 seconds after the next scheduled time, with one naming WARN
  and no purge INFO. Each observation also waited longer than a real tick.
- AC-16: fresh and upgraded schema/rows were inspected. Original columns,
  lengths, defaults, precision, keys, FKs, checks and indexes were preserved;
  eight new non-null audit columns had the specified types/static actors.
  Existing clicks were backfilled from clicked_at; new clicks used equal database
  write timestamps while clicked_at followed the shifted service clock.

Additional public effects: invalid URL 400, missing code 404, distinct unkeyed
creates, keyed 201 replay with one audit row, changed-URL mismatch 422,
24-hour key expiry, retirement 410, and exactly 60 admitted API requests then
429 with a correlated ProblemDetail and truthful Retry-After.

Proof 7: the migration's exact SHA-256 equals the independently reviewed design
SQL; the design re-review read and exercised its literal eight-column/history
rollback and reapplication (29 assertions). Proof 10: ADR-0018/0020 and the
0011/0013 decisions were present and indexed before the dependent commits.
The custody return is durable as **qitem-20261003221121-7686465a**; item 9 must
be judged on X′ before acceptance under mission NOTES §2 21:42Z.

Scope limits: controlled-clock probes use an external primary Clock and disposable
JDBC controls, not a production trigger endpoint. No PostgreSQL, Docker, large
catch-up/load budget or SIGTERM-during-purge claim is made. The design's load and
shutdown probes remain separately attributed. Earlier driver setup/assertion
corrections and the sandbox binding denial are preserved, explained in
`proof/qa-instrumentation-notes.md`; the final run completed successfully.
All six healthy final-run JVMs stopped; three invalid starts exited; ports
18141..18149 subsequently refused connections. Worktree HEAD remains exactly X.
