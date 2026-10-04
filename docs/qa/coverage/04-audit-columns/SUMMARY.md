# 04-audit-columns — independent QA

QA2 `qa2-agent@urlshort-factory` (Codex), 2026-10-03T23:47Z. **PASS** on exact candidate `305f8045d45b19a9e3287d5fe3508af6e04db9a4`.
Worktree `.worktrees/04-audit-columns` remained clean at that SHA throughout; the candidate descends
from retention merge `ed2b940` and baseline `main` `2566c38`. No product, test, build or migration edit by QA.

`../../scripts/gw --log <root>/docs/qa/04-audit-columns/check-qa-305f804.txt --offline check --rerun-tasks`
passed: **218 unit + 233 functional**, zero failures/errors/skips, all 14 tasks executed, Javadoc green.
Only fresh `test.exec` and `functionalTest.exec` existed when merged verification ran.
Counts are independently summed from the saved JUnit XML (`proof/qa-junit/`, `qa-junit-summary.json`).

| Suite | Lines covered/total | Line % | Branches covered/total | Branch % |
|---|---:|---:|---:|---:|
| unit | 490/557 | 87.97% | 194/200 | 97.00% |
| functional | 523/557 | 93.90% | 166/200 | 83.00% |
| all | 557/557 | 100.00% | 200/200 | 100.00% |

All 366 HTML/XML/CSV files copied from the fresh reports into `unit/`, `functional/` and `all/`;
SHA-256 manifest `proof/qa-report-hashes.json` rechecked 366/366. Percentages calculated from CSV
covered/missed sums, not averaged class percentages. Complementary per-suite misses are recorded
in `docs/qa/GAPS.md`; merged gate 100/100 with no exclusion or threshold change.

The clean full by-effect run is [`summary.json`](../../../../missions/02-brownfield/slices/04-audit-columns/proof/qa-305f804/run-20261003T234116500899Z/summary.json), with
**94 HTTP requests / 787 assertions**, all passing, and94/94 response IDs joined to their own
structured request-completed log events with the observed status. Full bodies, headers, argv,
JDBC snapshots and logs live beside that summary. `qa-evidence-recheck.json` separately verifies
creation Location, JSON media, empty204/302 bodies, absence of added fields and report hashes.

- AC1: both fresh/upgraded schema inspected; seven added columns' types/nullability verified;
  all15 legacy column definitions and constraint semantics preserved (22 columns after V4).
- AC2–6: controlled service-clock create/retire/key release; replay201, mismatch422, read200,
  redirect302, repeated retire410; actual H2 constraint rejects a retire audit INSERT, yielding
  sanitized500 and rolling back every link field/stamp with no audit append. Old audit rows stay identical.
- AC7: real `f6dd29e` jar wrote active, retired and genuinely key-released links; candidate jar
  upgraded its directory. Every legacy link/audit value stayed equal; link backfill used its latest
  known write; all audit row clocks equal and not later than upgrade, including a capped future event.
- AC8: five-field link bodies, empty redirects, four-field statistics and exact eight-field audit
  entries observed. Audit forwarded-header403 and invalid-limit400 retained; OpenAPI/AuditTrail/AuditLog
  byte-identical to baseline. No added column in responses/headers/logs.
- AC9: 59 baseline test/resource files compared:57 byte-identical, only two granted migration pins
  under `132a884`, with assertions unchanged. All213 inherited unit +224 inherited functional
  invocations passed in the fresh gate;5/9 additional invocations. Full271-method traceability saved.
- AC10: User-Agent/key/address/requestId/URL canaries absent from all audit columns; actors anonymous,
  times typed, and raw client canaries/address absent from logs.
- AC11: literal eight SQL statements extracted from the exact migration header ran on a stopped copy.
  Pre-V4 schema/constraints and every legacy value restored; V1–V3 retained; real candidate restarted
  the copy and reapplied V4 with history1–4 successful and legacy values unchanged.
- Additional failures: invalid URL/body/key400, missing404, default API-budget61st request429 with
  Retry-After1 (frozen clock prevents refill; operator audit route intentionally exempt).

Fixture boundaries: external primary Clock/file-based JDBC controls, disposable H2 constraint,
one stopped f6 link timestamp backdated two days to exercise real shipped expiry without a24h wait,
and one explicitly synthetic future audit event. No product/test changes. All seven app processes
stopped; rollback touched only a copy. AC1/11 use the SPEC/design-authorized unit migration tests plus
real-server/JDBC functional captures, not a claimed JUnit functional rollback test. Parameterized
report names identify41 methods only at their green class group; other 230 methods have individual
XML attribution. PostgreSQL, Docker and large-directory migration timing not independently checked.

Interrupted QA setup runs are retained under `proof/qa-305f804/`; only the final run above claims
complete AC verification. Timestamp parser normalization and a corrected exempt-route limiter
expectation were QA instrument fixes, not product findings. Socket binding required scoped sandbox approval.

Proof item8 also has prior independent design review `docs/review/04-audit-columns/design-review.md`
and its32-control literal rollback run. Candidate migration SHA-256 equals the reviewed design exactly
(`e1cf67add7d5dc55152504e19a60cbc5f3ad3291c9a1bc1af33e168e6da055a7`). Item9 impact analysis
preceded design (`aecb0d9` before `5a6d168`); `slice.yaml` records the plan-lock merged-main recheck,
and QA rechecked the candidate's named audit SELECT and V4 numbering. Code/security review remains downstream.
