# Re-check e40b095 — polling race resolution

QA PASS on exact `e40b09541feb0b7555c475baa82587fdd09e4890`, packet
`qitem-20261004025925-a5886b4c`, independent QA2/Codex. CR-01 resolution is
observed by QA; the formal code re-review follows.

The range from fb63a88 changes only the new characterization file's wait helper
and its caller. It waits on the target link's stored rows within the same 10-second
bound, then performs one statistics read with explicit 200 before parsing.
Shipped 60/600 limits, frozen UTC day, every matrix input and full grouping/day/
privacy assertions remain. Production tree 4c945cf11bf38036bba900425dacda9bb8ca7a83
and unit tree are identical to fb63a88; all 43 original functional files remain
unchanged from the slice base. Current `bootJar` is byte-identical to the earlier
QA jar: SHA-256 `92e1b7aef3b91749facdc39bfbc35cc8df8367119294d801436a7db34b38e58f`.
See `candidate.diff`, `source-custody.json`, `jar-custody.json`.

The fresh full gate (`check.txt`) passes 268 unit and 322 functional tests, zero failures/
errors/skips, Javadoc and coverage verification. Per-suite CSV totals remain
unit 510/584 lines, 198/206 branches; functional 550/584 lines, 172/206 branches;
merged 584/584 lines, 206/206 branches. Only fresh test.exec/functionalTest.exec are
canonical coverage data. All 378 copied report-resource hashes verified.

## Re-check CR-01

`QaStatsPollingResolution.java` calls the actual candidate
`assertGroupsAsTheReference`/`settledStats` with the real writer held 3 seconds,
a frozen clock and unchanged 60/600 budgets. It runs once without trust and
once with P trusted/reference U. Both full day/count/privacy oracles pass:

| Trust | Helper elapsed | Helper statistics reads | Separate positive control reads | 429 / NPE |
|---|---:|---:|---:|---|
| none |3036ms|1|1|0 / none|
| P |3042ms|1|1|0 / none|

The fresh-peer positive control still sees 200, exactly 3 clicks/2 uniques/0 bots
on 2026-10-01. Own probe exits 0/BUILD SUCCESSFUL; it does not rely on the builder's
old-failure probe exiting red. It derives the original reviewer control, removes
the timeout-thread exception and asserts resolution instead of expecting NPE.
All changes are external QA fixture files, not shipped tests. The old reproduced
failure remains in review2's committed report and QA's post-handoff qualification.
Commands from the candidate worktree:

```text
../../scripts/gw --log /absolute/docs/qa/06-client-identity/recheck-e40b095/held-writer-probe.txt --offline -I ../../docs/qa/06-client-identity/recheck-e40b095/probe.gradle qaStatsPollingResolution qaStatsPollingTrustedResolution
```

`held-writer-probe.txt` and `held-writer-summary.json` contain actual effects.
Both application contexts shut down and their JavaExec processes exited.
No new latency promise or universal scheduler guarantee is inferred.

Current characterization was also shadow-compiled, through an external Gradle
init file, onto the independently pinned original production at 1b4e0a7.
All 72 functional characterization invocations passed. No baseline source/test
file was edited. `current-characterization-on-original/summary.json`, XML and
the complete log record this compatibility replay. External compilation emitted
annotation-status classpath warnings; it completed successfully. This confirms
the corrected wait retains the pre-move oracles; it does not rewrite the original
test-before-move chronology. This external replay makes no coverage claim.

## Affected ACs by effect

The same declared external Clock/Servlet-peer fixture replays every M01–M12 row
on the exact candidate production classpath, with separate empty/P/P,Q trust
settings. For both 2/minute budgets: shared 429 with Retry-After 30, unrelated 201/302; each
row's three clicks give exactly one fixed-day entry with 3/2/0 and private hash
groups. Budget-before-audit precedence 403/429 and unchanged trail also pass.
There are 159 HTTP responses, 818 assertions and 3 stopped apps. All 159 raw bodies/
statuses and complete correlated JSON event windows were independently joined
and compared to the prior candidate: no difference after explicit UUID/code,
HTTP Date, JSON log timestamp/PID substitutions. Fixed day/time, status/header
presence, stable fields, event message/level/count and grouping are not normalized.
Read `matrix-last-run.txt`, `matrix-comparison.json`, `matrix-substitutions.json`
and the retained raw/SQL/app logs.

Per QA guidance §5, this re-check runs the full gate and affected ACs by effect.
The other AC observations are carried from the independently verified prior
candidate by exact production-tree and actual jar-byte equality. They were not
all re-executed in this turn. Prior evidence includes 2041 controlled responses,
original/candidate real-jar 59+59 and 72 blank-setting responses, with all 15 AC
effects and declared Servlet/network/telemetry qualifications. The original
metrics comparator false result and unnormalized springdoc measurement remain
visible. No new Docker/PostgreSQL/NAT/capacity/in-flight-shutdown claim.

Prior canonical coverage and SUMMARY were copied byte-for-byte into
`docs/qa/coverage/06-client-identity/archive-fb63a88/` before fresh reports occupied
the required suite directories. `historic-custody-aliases.json` verifies all 3490
original artifact hashes, using 379 explicit archive aliases for those prior
coverage files/SUMMARY. Nothing in the old raw captures/comparison oracles is
rewritten. Current 326 method mappings across 74 reports are verified; the 56
class-only parameterized attributions stay labelled. TRACE/GAPS have current
candidate appends and explicitly close the QA-observed polling gap, subject to
formal re-review.

## Self-check

Exact clean candidate, one-file diff and unchanged production/unit/jar bytes
verified; own fresh 590 gate; held real writer/no budget evasion/full oracle;
current characterization on original production: 72 passes; affected 12 rows and
failure/precedence cases observed; 159 complete raw/log joins checked; merged
CSV totals, 378 hashes and 3490 historical hashes verified; traceability complete
both ways with parameterization limits; GAPS written; both probes and 3 HTTP
apps stopped; no product/test mutation. QA drop and renewed attributed judgments
1–15/17 bind this exact SHA. Proof 16 remains pending under lead transition 1882;
independent re-review/merge/design-owner updates return it to QA.
