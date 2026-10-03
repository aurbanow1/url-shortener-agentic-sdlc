# 02-analytics — design review

**Latest verdict: FAIL on `45c98c469e7f18f7cdf4b098a03a851fa56d073d`: DR-01
remains HIGH for in-flight shutdown accounting. DR-02, DR-03 and DR-04 are
fixed.** The drain deadline works, but a normal JVM exit can still abandon
the daemon writer without the promised loss warning. This repeated finding
is escalated to the lead under the convergence rule; the review packet waits
for that resolution. See the appended re-review and
[escalation brief](design-dr01-escalation.md).

## Initial review — 71b2e10

Candidate: `71b2e10a7a32424f8f816db33db9984d7647bfd1` (producer commits
`20211ad` and `71b2e10`), against SPEC
`72001071cd38691b2ba0a41bf6b83be787ac02c0`.
Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03;
author: design agent (Claude).
Packet: `qitem-20261003084518-9957256a`;
instance: `01M40CP0JVZZWR8226MNN8XCCV`.

**Verdict: FAIL — DR-01, DR-02 and DR-04 HIGH.** Bound the writer's shutdown
wait, preserve the current day's salt when requests cross midnight out of
order, and make the UA-class constraint survive connection retirement on
the shipped H2 version. DR-03 is MEDIUM and should be aligned in passing.
No new service layer or human scope decision is required for these fixes.
This judges the proposed design and its executable probe, not unbuilt
production analytics code.

## Context proof

One redirect hook reduces request data before handing it to a bounded
single writer; a new endpoint reads raw-click aggregates from one grouped
query. Existing redirect responses, append-only audit and private correlated
logs must survive the addition. The accepted SPEC requires daily hash
stability during uninterrupted service, expiry of yesterday's salt, failure
isolation, and a five-second normal-load visibility bound. Confidence:
scope/territory 99/100; API/data/test design 97/100; reported boundary defects
99/100, reproduced using the unchanged producer probe classes.

Read the complete design, accepted SPEC and requirements review; current
system design changes and ADRs; architecture §§3–8, databases §8 and review
guidance; mission allocation and w2 grants. The small path-list grant is
confirmed by transition 455 of `qitem-20261003082940-4da408ce`, and appears
in `slice.yaml`. The grant adds one statistics path to the existing exact
OpenAPI path assertion; it does not authorize other `web/` test changes.

## Complete file ledger

The range `20211ad^..71b2e10` has **12 changed files / 12 reviewed**. All
working bytes matched the candidate before recording this review. The
separate lead-authored territory grant was also read as supporting input.

| Changed file | Verdict |
|---|---|
| `docs/DESIGN.md` | Reviewed all changes; coherent component/data/ADR additions, subject to DR-01/02/04 corrections |
| `docs/adr/0004-structured-ecs-logs-no-client-pii.md` | PASS — asynchronous MDC and safe exception logging amendment |
| `docs/adr/0011-click-handoff-bounded-single-writer.md` | DR-01 HIGH — unbounded destruction callback |
| `docs/adr/0012-client-hash-daily-salt.md` | DR-02 HIGH — documented straggler exception contradicts accepted same-day stability |
| `docs/adr/0013-click-events-and-request-time-statistics.md` | DR-04 HIGH — proposed enum CHECK fails after its creating connection retires; aggregation/index/rollback decisions otherwise coherent; PostgreSQL remains unexecuted |
| `docs/diagrams/click-sequence.mmd` | PASS — reduction before enqueue, failure isolation and read sequence agree with design |
| `docs/diagrams/container.mmd` | PASS — feature dependencies and asynchronous write boundary represented |
| `docs/diagrams/erd.mmd` | PASS — V2 columns, FK and lookup index agree with DDL |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/ClickProbe.java` | Read all 570 lines; boundary reproduction uses its actual salt/recorder/store classes and V2 DDL; DR-01/02/04 |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/click-probe.gradle` | PASS — JDK 21 task on the functional classpath; no product build changes |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/output.txt` | Inspected all 1,303 lines: parsed all 1,260 JSON events, grouped repeated messages/field shapes, read all 43 other lines and C0–C9/S1–S2 results; supports the narrow successful probe claims, not the omitted boundaries |
| `missions/01-greenfield-core/slices/02-analytics/design.md` | Full read; DR-01/02/04 HIGH and DR-03 MEDIUM; remaining coverage below |

Candidate file hashes and the complete log-pattern inventory are in
[design-source-audit.json](proof/design-source-audit.json).

## Contract coverage

| Area | Assessment |
|---|---|
| AC-1/2/16; rules 1/5/6 | Hook after successful resolve, HEAD skip, bounded FIFO queue, failure catches and a real-server concurrency journey are explicit. AC-1 polls before flushing, preserving the five-second visibility check. Shutdown requires DR-01. |
| AC-3/4/6/17/18; rules 2–4/9 | URI-origin reduction, four UA tokens, peer address only, HMAC before enqueue, aggregate-only output and canary checks cover the new input/storage/log boundaries. No raw request object crosses threads. |
| AC-5; rule 4 | Cryptographic primitive, random key, normal rotation and quiet-day expiry are specified and probed. The concurrency exception is not authorized by the SPEC: DR-02. No raw-address disclosure is alleged. |
| AC-7–13; rule 7 | One lookup plus one grouped statement and a Java fold give consistent totals, UTC dates and deterministic top-10 ties; retired rows remain readable. 404 and tested wrong-method 405 cases use the existing sanitized problem-detail contract. HEAD/OPTIONS wording needs DR-03. |
| AC-14/15/19 | Slow/failing-store seams, restored request id and one safe WARN are named. The real-server log window waits for both the writer and the request-completion event. Producer C8 contains one WARN, one completion, matching request ids and no throwable fields. |
| AC-20/21 | No audit writes; unchanged slice-01 tests plus audit snapshots. Statistics schema/example and 404 media type are specified, with deterministic document regeneration and the granted path assertion. |
| Schema and rollback | Additive V2, parameterized SQL, timezone-aware instant, derived UTC day, FK/CHECKs and one justified index. The enum CHECK needs DR-04. `DROP TABLE click` explicitly loses click history only. Retention enforcement remains mission 02; timestamps keep it possible. |
| Threat model | New headers/peer address, statistics path, storage, salt and logs covered. Spoofing, privacy, queue pressure and growing statistics are addressed; rate limiting/retention remain named external obligations. DR-01 adds the missed slow-store shutdown consequence. |
| Structure and territory | Six feature classes plus package documentation and one redirect hook are justified. No speculative service or summary table, no dependency added. The local code-to-id read is a documented consequence of the existing package/territory boundary. |
| ADRs/test plan | Three proposed ADRs plus the existing logging amendment are indexed before product implementation. Every AC and all nine rules map to suites; W1-02 gets a real-server class sharing the resilience context. L3's numerical target remains release measurement or an explicit gap. |

## Findings

Paths `design.md`, `SPEC.md` and `design-probe/ClickProbe.java` below are under
`missions/01-greenfield-core/slices/02-analytics/`.

| Id | Severity | File:line | Evidence / shipped consequence if implemented | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `docs/adr/0011-click-handoff-bounded-single-writer.md:48`; `design.md:33`, `design.md:505`; `design-probe/ClickProbe.java:509` | `@PreDestroy` delegates to `ExecutorService.close()` with no deadline, explicitly justified by an assumption that the embedded store completes at once. The independent Spring/Hikari/H2 probe enqueues six writes using the producer's two-second slow-store mode: context close still runs after 10.1 s and finishes around 12 s with all six rows persisted. A 10 s lifecycle phase timeout does not bound this bean-destruction callback. More queued or indefinitely stalled writes can hold process shutdown indefinitely. This violates architecture §7's timeout rule; deferring the fix to 03 is also outside that slice's `click/` territory. | Specify a finite drain deadline and the outcome for queued/in-flight work when it expires, with fail-open loss accounting and private correlated logs. Keep shutdown completion bounded even when a store does not promptly stop. Add a slow/backlogged-writer shutdown test; coordinate the bound with 03's container grace period. Amend ADR-0011 and related design text now rather than making correction conditional on a later smoke result. |
| DR-02 | HIGH | `docs/adr/0012-client-hash-daily-salt.md:38`; `design.md:35`; `design-probe/ClickProbe.java:370`; contract `SPEC.md:237` | A request captures day D and pauses before hashing; a second hashes D+1; the old request resumes and replaces the D+1 salt with a fresh D salt; the next D+1 request draws yet another key. Running exactly that ordering against `ProbeSalt` prints `sameDayHashStable=false` for the same address and D+1 instant, without a restart. The ADR acknowledges the race but silently relaxes the SPEC's uninterrupted same-day guarantee. It also allows rotation back into an expired day instead of an ordered day transition. | Make time selection and salt generation/expiry ordering coherent so a delayed request cannot replace or expire the current generation's key. Preserve rule 4 and avoid retaining yesterday's salt as a workaround. Add deterministic tests for reordered midnight requests and a stale expiry callback; document the resulting timestamp/hash boundary. No new persistence or master secret is required. |
| DR-04 | HIGH | `design.md:156`, `design.md:385`; `docs/adr/0013-click-events-and-request-time-statistics.md:22` | The exact proposed `CHECK (user_agent_class IN (...))` is invalidated when its creating physical connection is retired on cached H2 2.4.240. Independent Hikari probes on both memory and file databases insert a valid `browser` click, retire pooled connections while keeping the database open, then repeat the same valid insert: both return SQLState 23514, `CK_CLICK_USER_AGENT_CLASS` invalid, with underlying closed-session error 90098; `SELECT COUNT(*)` still works and remains 1. Thus ordinary pool connection replacement can turn valid clicks into permanent fail-open losses. The initial DDL smoke and assumption that CHECKs cannot fail miss this lifetime boundary. | Use a constraint representation verified to keep accepting all four valid classes and rejecting invalid ones after physical connection replacement on the shipped engine. Keep portable SQL and the reduced-data contract; do not rely on a connection living forever. Add a pooled-connection retirement regression with the real V2 migration and update the design/ADR/probe evidence. |
| DR-03 | MEDIUM | `design.md:113`, `design.md:115`; contract `SPEC.md:240` | The design serves statistics HEAD via the GET mapping and leaves OPTIONS to the platform, while rule 7 says any method other than GET answers 405. The design's 405 table only lists POST/DELETE/PUT/PATCH. This is a documentary contract mismatch; it does not change click counts or disclose extra body data. | Align the method contract explicitly. Implement the current rule, or have the requirements producer record the intended HEAD/OPTIONS framework-default exception and an observable check. Do not let an implicit framework behavior stand in for an undocumented SPEC change. |

## Independent reproduction and limits

Run from the repository root:

```sh
scripts/gw --log docs/review/02-analytics/proof/design-boundary.txt --offline -I docs/review/02-analytics/proof/design-boundary.gradle reviewDesignBoundary
```

[DesignBoundaryProbe.java](proof/DesignBoundaryProbe.java) compiles with the
unchanged producer probe; it does not copy or repair its algorithms. The
first check deterministically replays a possible concurrent invocation
order. The second uses its actual recorder, actual H2 inserts and existing
two-second store mode, destroys it through Spring with a ten-second
lifecycle phase timeout, and checks both elapsed time and persisted rows.
[The output](proof/design-boundary.txt) reproduces both findings. Successful
Gradle exit means the expected defects were observed, not that those design
boundaries passed.

The first shutdown fixture used disposable connections and hit a
`CK_CLICK_USER_AGENT_CLASS` closed-session error. A direct insert preflight
isolated the failure; keeping Hikari connections alive allowed the shutdown
test to isolate DR-01 with six successful inserts. Crucially, a second probe
then reproduced the constraint failure with actual Hikari connection
retirement on both memory and file databases, so it is DR-04 rather than an
excused fixture limitation:

```sh
scripts/gw --log docs/review/02-analytics/proof/design-h2-constraint.txt --offline -I docs/review/02-analytics/proof/design-boundary.gradle reviewH2Constraint
```

[H2ConstraintProbe.java](proof/H2ConstraintProbe.java) holds another database
session open while calling Hikari's `softEvictConnections()`, then repeats
the valid insert using the design's `atOffset(UTC)` binding. This models
physical connection replacement without restarting the database. The
[recorded result](proof/design-h2-constraint.txt) shows the two failures and
still-readable row counts. It deliberately advances the pool lifecycle;
it does not claim a timed soak through automatic max-lifetime eviction.
The shutdown probe is not a complete HTTP shutdown test, and no failure of
03's unbuilt AC-25 is claimed.

The producer's recorded C0–C9/S1–S2 evidence was inspected, not represented
as my fresh execution. Its happy-path timing is indicative only. No actual
PostgreSQL run, 02 implementation coverage or release latency result is
claimed. The optional DI/spy mechanics still need the builder's tests.

## Self-check and exit

- Exact candidate and all 12 producer files verified, including qualification
  commit and the full probe source. Coverage ledger complete.
- Fresh baseline `scripts/gw --log docs/review/02-analytics/proof/design-check.txt
  --offline check` passed: 14 tasks up to date. This is the unchanged
  integrated product gate, not analytics implementation validation.
- All three blockers have executable reproductions, candidate lines and bounded
  required changes. DR-03 is a documented source-to-source contradiction.
- No product, test, SPEC, design or ADR edits. Only `docs/review/` authored.
  The ledger is appended with 3 HIGH and 1 MEDIUM.
- `failed` returns to design. Answer all four findings as fixed or disputed
  with evidence; re-review will stay scoped to the response and changed
  artifacts. DR-03 is expected in passing, not a new blocking gate. Prior
  requirements RQ-01 remains settled; A-9/CR-01 and release measurement
  obligations remain where previously assigned.

## Re-review 45c98c469e7f18f7cdf4b098a03a851fa56d073d

2026-10-03, `review-agent@urlshort-factory` (Codex).
Packet `qitem-20261003092345-52dd8a6e`, same workflow instance.
Design commits `7cbf7a1` and `45c98c4`; requirements alignment `173bd60`.

**Verdict: FAIL — one remaining HIGH, DR-01.** The same finding survived
its correction round, so escalate with both positions and use `waiting`
rather than silently starting another producer loop. No new finding is
added. DR-02/03/04 are settled; no non-blocking review item remains open.

### Context and complete coverage

The revision keeps the same analytics scope and adds bounded draining,
atomic time/key selection, a lookup-table foreign key and explicit HTTP
method defaults. Confidence: 99/100 in scope and the four finding
dispositions; production implementation and final shutdown smoke remain
future evidence. This is a design review, not approval of unbuilt code.

The union of the three response commits changes **15 files / 15 reviewed**:

| Changed file | Verdict |
|---|---|
| `docs/DESIGN.md` | Candidate revisions read; components, clock edge and H2 guidance aligned; shutdown-accounting claim subject to DR-01 |
| `docs/adr/0011-click-handoff-bounded-single-writer.md` | DR-01 remains HIGH: finite wait proven, in-flight outcome not guaranteed |
| `docs/adr/0012-client-hash-daily-salt.md` | PASS: clock sampled under salt lock, stale-day expiry guarded, backward wall-clock boundary disclosed |
| `docs/adr/0013-click-events-and-request-time-statistics.md` | PASS: four-row lookup/FK and ordered two-table rollback |
| `docs/diagrams/click-sequence.mmd` | Reviewed: new stamp and cancellation sequence; universal shutdown-accounting note needs DR-01 correction |
| `docs/diagrams/container.mmd` | PASS: Clock dependency moved to DailySalt |
| `docs/diagrams/erd.mmd` | PASS: lookup relation and length check match revised DDL |
| `missions/01-greenfield-core/slices/02-analytics/SPEC.md` | PASS: rule 7, AC-22 and A-18 explicitly settle HEAD/OPTIONS; proof range updated |
| `missions/01-greenfield-core/slices/02-analytics/design.md` | All response changes reviewed; 22 ACs mapped, threat/test/schema changes coherent except remaining DR-01 |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/RevisionProbe.java` | Full 237-line read and fresh run; salt and interruptible drain results hold; sleep-only store misses non-interruptible JDBC tail |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/revision-probe.gradle` | PASS: isolated JDK 21 source launch, unchanged product build |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/revision-output.txt` | All 33 lines read; claims hold within the interruptible-sleep fixture |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/ConstraintProbe.java` | Full 141-line read and fresh run; connection retirement retains a live database and tests actual engine behavior |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/constraint-probe.gradle` | PASS: isolated JDK 21 source launch |
| `missions/01-greenfield-core/slices/02-analytics/design-probe/constraint-output.txt` | All 153 lines read; ten cases in each engine mode, both failures and successful alternatives inspected |

[File hashes](proof/design-revision-source-audit.json) bind the scope to the
candidate. Fourteen working files match it exactly. Shared `docs/DESIGN.md`
advanced concurrently for 03's logging/shutdown changes; its **candidate
diff** was reviewed, and the later two-row difference inspected only to
confirm it does not alter the 02 response. The unrelated 03 work is not
approved here. Every executed producer probe and the input DDL/SPEC match
the named candidate.

### Finding resolutions

| Id | Producer response | Re-review disposition and evidence |
|---|---|---|
| DR-01 | Fixed by five-second wait, interruption, queued-task reporting and assumed pool termination of the active write | **Partially fixed; HIGH remains.** Fresh sleep-based probe returns in 5,009 ms, accounts all six, and proves the old unbounded wait is removed. Independent real-JDBC checks show that pool close can return before the active call finishes. A fresh child JVM returns normally after 5,013 ms with only 5/6 requests accounted and kills its remaining daemon writer; no completion/loss callback follows. |
| DR-02 | Fixed by choosing instant and key together | **Fixed.** Fresh deterministic D / D+1 / delayed-D-hash / D+1 replay keeps D+1 stable and D distinct. Stale D expiry leaves D+1 unchanged; current-day disposal and quiet-day scheduled expiry also pass. Backward wall-clock steps remain an explicitly disclosed boundary, not the old request-reordering race. |
| DR-03 | Fixed through requirements producer at `173bd60` | **Fixed.** AC-22 observes HEAD 200/no body, OPTIONS 200/Allow GET and unchanged totals; rule 7, A-18, design response table and StatsJourneyTest plan agree. No implicit SPEC exception remains. |
| DR-04 | Fixed with seeded lookup table/FK | **Fixed.** Fresh matrix repeats the H2 multi-value CHECK failure and verifies V1 checks unaffected. Independent probe extracts V2 directly from design.md: after DDL-session retirement, all four valid tokens succeed, invalid token and short hash fail, and ordered rollback leaves link/audit intact, on both memory and file databases. The planned regression owns its Flyway database/pool, so it retires the actual migration session. |

### Remaining finding

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DR-01 | HIGH | `missions/01-greenfield-core/slices/02-analytics/design.md:34`; `docs/adr/0011-click-handoff-bounded-single-writer.md:51`; `missions/01-greenfield-core/slices/02-analytics/design-probe/RevisionProbe.java:93` | The shutdown loop reports only queued tasks. In-flight reporting depends on the daemon thread returning from JDBC, and the design asserts Hikari close forces that return. `design-revision-boundary.txt`: recorder returns in 209 ms; pool close takes 0 ms; write is still unfinished after both and only later reports QueryTimeoutException. `design-exit-boundary.txt`: two 2-second writes complete, a third waits on a real H2 lock, three queued writes are reported; recorder/pool close after 5,013 ms, accounting is 5/6, main returns normally and the third callback never runs. No SIGKILL is involved. | Account for the in-flight request before shutdown returns, without depending on interruption or a post-shutdown daemon callback; preserve a finite deadline, private request-correlated reporting and duplicate suppression. If the database outcome is unknown, say so rather than claiming a guaranteed rollback/loss. Add a store that ignores interruption and a process-exit/race check. Amend the universal written-or-lost assertion and the Hikari assumption. |

The producer acknowledged at 09:33Z that the pool-close claim had not been
probed and was wrong, and proposed an atomic in-flight owner so either
shutdown or completion emits the report. That is a proposed correction,
not reviewed candidate evidence. The lead receives this position alongside
the reproducible remaining defect; no product-intent change is requested.

### Fresh verification and limits

Executed through `scripts/gw --offline`, with the wrapper's `--log` flag:

| Task | Evidence and observed result |
|---|---|
| `-I missions/01-greenfield-core/slices/02-analytics/design-probe/revision-probe.gradle designRevisionProbe` | [revision rerun](proof/design-revision-rerun.txt): all reported salt/interruptible-drain predicates true |
| `-I missions/01-greenfield-core/slices/02-analytics/design-probe/constraint-probe.gradle designConstraintProbe` | [constraint rerun](proof/design-constraint-rerun.txt): complete engine matrix reproduced |
| `-I docs/review/02-analytics/proof/revision-boundary.gradle reviewRevisionBoundary` | [independent boundaries](proof/design-revision-boundary.txt): all-four-token and rollback checks pass; real JDBC remains unfinished after pool close |
| `-I docs/review/02-analytics/proof/revision-boundary.gradle reviewExitBoundary` | [normal-exit reproduction](proof/design-exit-boundary.txt): expected 5/6 accounting defect reproduced; successful task exit means reproduction succeeded |
| `check` | [baseline gate](proof/design-revision-check.txt): successful, all 14 tasks up to date; unchanged integrated product, not new analytics coverage |

The independent fixtures compile the unchanged producer recorder and use
its executor/close implementation. The exit probe adapts the store work to
two slow writes followed by an actual H2 lock wait; it does not claim the
planned click INSERT normally conflicts on that fixture's primary key.
The point verified is JDBC/cancellation/lifetime behavior at shutdown.
The current design explicitly claims to handle slow or stalled stores.
The child returns from main normally; its third operation cannot commit
while the independent blocking transaction remains open. No warning callback
for it appears before the Java process exits.

No PostgreSQL, integrated 02+03 shutdown, release latency result or analytics
coverage is claimed. NFR-L3 measurement, A-9 proxy alignment and prior CR-01
backlog remain assigned as before. The producer's H2 guidance caveat belongs
in the lead's existing follow-up; it is not another review blocker.

### Re-review self-check and continuation

- Candidate/commit scope and all 15 files checked; existing findings alone
  judged, with no reopening of settled requirements findings.
- Fresh gate and all four executable checks completed; the HIGH cites the
  actual failed outcome rather than the successful process exit code.
- Only `docs/review/` authored. Product, tests, requirements and design remain
  producer-owned. Ledger records one HIGH, zero other open findings.
- Repeated DR-01 goes to the orchestration lead with both positions. Resume
  after its resolution, verify the concrete correction on its named SHA,
  preserve DR-02/03/04 as fixed, then hand off to delegated plan-lock when clear.
