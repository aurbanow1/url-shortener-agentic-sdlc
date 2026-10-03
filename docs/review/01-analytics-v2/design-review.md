# 01-analytics-v2 — design review

**Verdict: PASS on `80ca44cdea386e9f5a162b2e17cdb365aece6a09`.** No open findings.
One MEDIUM counter finding was fixed in passing; its evidence and resolution are retained below.

Packet: `qitem-20261003184238-e2f2be28`; instance: `01M416Z3CM54YQTX93V4KG0CPS`.
Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
Handed design: `16312da08836aa666671c6b11c50202ac3f69c04`, following impact analysis `3ebfb09`
and design `aa36c8b`. The producer explicitly handed the in-passing correction `80ca44c` at
18:48Z, and its entire three-file delta was reviewed. Accepted SPEC: `b8c327b`.

## Context and complete coverage

The human chose per-UTC-day uniques and bot clicks beside unchanged raw figures, using the
existing daily hash only within its own day. The same trusted-proxy rule must identify clients
for both the limiter and the click recorder; two counters expose successful writes and loss
reports. No export, cross-day identity, new reader or retention mechanism belongs here.
Understanding and design confidence: high. This review judges the design, not unbuilt code.

Read the complete SPEC, design and impact analysis; mission decision/NOTES, slice manifest,
PROGRESS/PROOF, the recorded `c78500e` grant, prior requirements review, existing click storage,
fold, recorder, salt and limiter, and the resilience suite used by the proposed regression
tests. Applied review guidance, architecture §§3–8, databases §8 including the new audit-column
policy, and brownfield §7. Impact analysis precedes the design. The implementation prerequisite
is both mission-02 w1 merges, including V3 audit columns and the forwarded-header default.

| Changed file | Verdict |
|---|---|
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/impact-analysis.md` | PASS; modules, endpoints, data, rollback, exact-shape test changes and cross-mission custody covered |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/design-probe/StatsProbe.java` | PASS; complete probe read, SQL shapes and meter registration measured; counter timing and identity handoff not claimed by this probe |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/design-probe/stats-probe.gradle` | PASS; isolated source launcher on the functional classpath, no product mutation |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/design-probe/output.txt` | PASS; complete output read; per-day inputs, plans, scoped cost and Prometheus names supported |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/design.md` | PASS after DR-01 correction and removal of the unsupported missing-day fallback |
| `docs/DESIGN.md` | Complete authored deltas read; shared privacy, identity, metrics and snapshot statements agree with the final design |
| `docs/adr/0013-click-events-and-request-time-statistics.md` | PASS; same-day aggregate query, fold, alternatives and scoped cost recorded |
| `docs/adr/0015-client-identity-trusted-proxies.md` | PASS; one existing identity decision shared through a request attribute, connection address preserved |
| `docs/adr/0016-metrics-and-health-exposure.md` | PASS after in-passing correction; successful writes counted independently of loss-report ownership |
| `docs/diagrams/analytics-v2-sequence.mmd` | PASS; request-thread reduction, writer, meter and grouped-read flow covered |
| `missions/03-ambiguous-analytics/NOTES.md` | Complete handed delta read; design mechanism, ADRs, grant and integration prerequisites recorded |

**11 unique changed files / 11 reviewed**, including all three correction paths already in
that set. Product/build files have no diff between the handed final candidate and the checkout
used by the reviewer controls. No product, tests, SPEC or design was edited by the reviewer.

## Contract assessment

| Contract | Assessment |
|---|---|
| AC-1–6; rules 1–4 | Reachable. One parameterized UNION returns referrer counts plus one set of distinct/bot counts per link/day. Existing fold preserves totals, day order, omissions and top-referrer ranking; only two per-day fields are added. Empty shape and bot inclusion are explicit. |
| AC-7/8; rule 6; grant | Reachable. The limiter stores exactly its existing `clientOf` result before charging; the recorder hashes it, with peer fallback. No second header parser, new trust setting or peer rewrite. AC-7 includes the forged leftmost entry; AC-8 ignores forwarding headers from an untrusted peer. Existing limiter cases must remain unchanged. |
| **Proof item 11: same-day use and salt privacy** | **PASS at design level.** The only new read of `client_hash` is `COUNT(DISTINCT client_hash)` scoped by link and grouped by `clicked_on`; the hash is not returned or tagged and is not joined to other data or compared across days. Unchanged `DailySalt.stamp` selects instant/key together; salt stays in memory, is zeroed/dropped at day end and close, and no persistence is introduced. Restart overcount remains the accepted limit. Implementation/security review must verify these exact properties on the built candidate. |
| AC-9/12; safe errors and logs | No new input or error contract. Existing stats 404/405/500 ProblemDetail behavior and request-id handling remain; the query propagates failures to existing sanitized advice. The request attribute never enters the queued Click, a response, a metric or a log. Canary tests extend to forwarded clients. |
| AC-10/11; rule 10 | Reachable after DR-01. All five loss reasons are registered at zero and increment beside their existing WARN, with only a static reason tag. Recorded counts every successfully returned insert. A shutdown outcome-unknown report and a later successful write may count in both families; these are not disjoint populations. |
| AC-13/14; compatibility | API schema/example and committed/live equality are explicit. The impact analysis names both existing functional exact-shape assertions and affected unit signatures; other functional behavior remains unchanged. |
| AC-15; NFR-L1 | Existing slow-store/failure/concurrency journeys run under empty and configured proxy settings; no new blocking request-thread work is designed. Actual p95/p99 remains release-bench evidence, not proven by source structure or the stats timing probe. |
| Storage, rollback, policy | No DDL/index/dependency added. Existing indexes serve the grouped scans; no statistics latency target justifies another index. Merge-revert rollback has no data undo. Builds on retention's V3 audit columns; other existing column debt remains explicitly routed in GAPS. No new-table policy exception is introduced. |
| Threat model and territory | Covers same-day privacy, forged identity, disclosure through storage/logs/metrics, read cost and audit access interaction. `getRemoteAddr()` stays unchanged, preserving ADR-0019's raw-peer premise. `click/` and OpenAPI custody require rebasing after both w1 merges; only the two granted `web/` files may change. |

## Independent evidence

```sh
scripts/gw --log docs/review/01-analytics-v2/proof/consistency-controls.txt --offline -I docs/review/01-analytics-v2/proof/stats-consistency.gradle reviewStatsConsistency
scripts/gw --log docs/review/01-analytics-v2/proof/baseline-check.txt --offline check
```

Both exit 0. The baseline gate is **UP-TO-DATE**, not a fresh implementation suite.
The [reviewer probe](proof/StatsConsistencyProbe.java) uses real H2 2.4.240 and shipped V1/V2
in memory. Identity SQL functions mark the first aggregate and pause before the second branch,
without changing values. A separate connection commits a mutation during the pause:

| Control | Observed and asserted |
|---|---|
| Insert a new distinct bot between branches | Fresh table count 2; current statement retains 1 click / 1 unique / 0 bots |
| Delete all clicks between branches | Fresh table count 0; current statement still retains 1 click / 1 unique / 0 bots |
| Original counter sequence, ordinary completion | stored 1, recorded 1, lost 0 |
| Original counter sequence, shutdown claims before successful return | stored 1, recorded 0, lost 1 |
| Corrected counter sequence, same shutdown interleaving | stored 1, recorded 1, lost 1 |

The two query controls demonstrate the particular snapshot claim under READ_COMMITTED with
the tested engine, not general database isolation beyond this query. They support removing the
zero fallback added speculatively for purge concurrency. The counter controls are a bounded
latch model of the specified insert/CAS/increment order, grounded in the existing recorder's
claim mechanism; they do not claim to execute a future product implementation. Full
[output](proof/consistency-controls.txt) and [baseline gate](proof/baseline-check.txt) are retained.

Producer S1–S5 additionally support the aggregate shapes and static metric names. Its 146–202 ms
timings are scoped to 225,000-click in-memory fixtures; no redirect latency or larger deployment
capacity claim follows from them. Actual attribute handoff and complete metrics wiring remain
candidate-suite work, explicitly named in the design.

## Finding and in-passing resolution

| Id | Severity | File:line at initial candidate `16312da` | Evidence | Required change |
|---|---|---|---|---|
| DR-01 | MEDIUM | `missions/03-ambiguous-analytics/slices/01-analytics-v2/design.md:36`, `:141` | Incrementing recorded only after a successful RUNNING→DONE CAS excludes a successful insert whose report shutdown already claimed. The controlled sequence yields stored=1/recorded=0/lost=1, contrary to rule 10's stored-click count. Data is still written; consequence is shutdown observability rather than lost clicks. The author confirmed this was unintended. | Increment after successful insert return independently of report ownership; preserve one loss increment per report and test the late-success case. |

### Re-review 80ca44c

**DR-01: fixed.** The three-file correction moves the increment immediately after insert return,
updates ADR-0016, states the legitimate overlap with an outcome-unknown report, and adds a
latch-controlled unit case released after close. The corrected control yields 1/1/1.
The unrelated defensive zero fallback was also removed in passing after the snapshot checks,
and the measured H2 behavior was recorded as a shared stack fact. No open finding or new backlog
item remains. Prior requirements are unchanged.

## Self-check and handoff

Complete file ledger, exact candidates, privacy proof row and empirical limits recorded.
Only reviewer artifacts authored; all evidence commands completed successfully. Append the
ledger row and hand off to plan-lock. The lead retains cross-mission custody; implementation
and QA retain the exact-candidate gate, complete AC/traceability/coverage proof, post-V3
compatibility, live OpenAPI and release latency measurements.
