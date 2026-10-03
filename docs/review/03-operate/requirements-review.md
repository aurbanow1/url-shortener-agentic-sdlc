# 03-operate — requirements review

- Candidate: `da719376088098b0fb17413d6e0897f2c593e6a6` (`docs(03-operate): requirements`).
- Reviewer: `review2-agent@urlshort-factory` (Codex); 2026-10-03 UTC.
- Packet: `qitem-20261003081312-64ea3c87`; instance `01M40CPBYR97637QGHT57BNEBY`.
- Verdict: **FAIL — RQ-01 (HIGH)**. RQ-02 and RQ-03 are MEDIUM wording fixes to make in passing. No implementation defect is asserted.

## Context proof

The Operator needs two per-client request budgets, safe client identification,
truthful health and metrics, and container/restart/latency evidence. The mission
explicitly separates the slice's suite proofs from release-level checks. Its
ordered API-document grant requires the implementation candidate to descend
from the analytics merge. Confidence: high in allocation, public contracts and
review scope; runtime feasibility belongs to design and has not been tested here.

Read the selected path `project.yaml` → mission `mission.yaml`/`SPEC.md` →
`03-operate/slice.yaml`/`SPEC.md`, the wave map, requirements baseline, recorded
human decisions, inherited create/redirect contracts, and requirements/review
guidance. The packet and authored workflow select ordinary requirements review;
there is no additional composition or deep-review gate.

## File ledger

`git diff-tree --no-commit-id --name-status -r da719376088098b0fb17413d6e0897f2c593e6a6`
lists exactly one changed file. Read all 402 lines; independently compared its
working bytes to the candidate blob with `git show` (equal).

| Changed file | Verdict |
|---|---|
| `missions/01-greenfield-core/slices/03-operate/SPEC.md` | Complete review; RQ-01 HIGH, RQ-02/RQ-03 MEDIUM |

## Coverage of the requirements review

| Check | Result |
|---|---|
| Allocated ids | All present: FR-10; R1–R4, S5, O3, X1, L1/L2; cross-cutting M1/M2/M3. Each has ACs or, for M1/M2, explicit artifact obligations. |
| AC inventory | AC-1 through AC-27 are contiguous, unique, GIVEN/WHEN/THEN; AC-1–20 suite and AC-21–27 release split matches the mission. RQ-01 concerns a missing failure condition; RQ-02 an under-specified initial state. |
| Errors and privacy | 429, request id, forwarding spoof, bad-input charging, database-down health, log/metric canaries and exposure limits are specified. RQ-03 narrows an over-broad privacy sentence without relaxing limiter privacy. |
| Business rules and scope | Classification, refill, charge order, retry rounding, proxy trust, restart reset and shutdown are explicit; out-of-scope list preserves territory. |
| Ambiguities | A-1–A-18 have reasons; none parked. A-1 implements the mission's every-operation 429 obligation; A-8 preserves decided defaults while allowing bench settings. A-16's refusal distinction is sound, but AC-25 must actually detect a cut-off accepted request. |
| Proof contract | Coverage reports, traceability, gaps, HTTP/log captures, OpenAPI diff and ancestry, ADRs, memory/privacy review and release checks are named. No release target is claimed as suite evidence. |
| Requirements/design boundary | No class/schema/dependency implementation prescribed. Existing platform names are public metric/API contracts; token-bucket and container constraints come from the baseline. |

## Findings

All SPEC locations below refer to the candidate's
`missions/01-greenfield-core/slices/03-operate/SPEC.md`.

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| RQ-01 | HIGH | `SPEC.md:246` (AC-25; also rule 13 at 272 and A-16 at 332) | AC-25 checks only requests that received an HTTP response, a shutdown log, and a configured timeout. A connection already accepted and then reset/closed without a response is outside that predicate. A continuous loop also need not leave any request in flight at stop. Thus the only AC allocated to NFR-R3 can pass without proving its core promise; baseline `docs/REQUIREMENTS.md:74` requires in-flight completion and no new accepted connections. See counterexample below. | Strengthen the release AC/proof to establish at least one request in flight before stop, require its successful completion within the shutdown bound, distinguish connection refusal before acceptance from reset/EOF/timeout after acceptance (the latter fail), and observe refusal of new connections during drain. Keep ordinary downtime refusals separately reported. State observable conditions; leave the probe mechanism to design. |
| RQ-02 | MEDIUM | `SPEC.md:111` (AC-3; rules 2/4 at 261/263) | The setup says only exhausted budget and last response 429. With the default continuous refill, an empty bucket at t=0 followed by another 429 at t=0.250 has 0.250 tokens and `Retry-After: 1`. At 0.999 s after that header the request is already admissible. The required 429/201/429 sequence is therefore not true for all allowed initial states. The author's self-check at 397 clearly intended immediate exhaustion on a frozen clock, so this is a bounded clarification. | Make the exact-zero/default-rate/frozen-time setup explicit for the boundary sequence. Separately specify rounded waiting from a partially refilled bucket: a retry after S is admissible if no intervening request consumes its budget, without asserting it cannot become admissible earlier. |
| RQ-03 | MEDIUM | `SPEC.md:265` (business rule 6) | The blanket ban on any submitted value in any response body/header also bans the normal link `url` and redirect `Location`. Inherited `01-create-redirect/SPEC.md:112`, `:289` and `:294` explicitly require those values verbatim. AC-11 makes the intended 429 scope clear; the business rule does not. | Limit the response-output ban to limiter-produced responses (429), retaining the ban on client identity in limiter output and on sensitive values in logs/metrics. Explicitly preserve the inherited successful create/read/redirect URL contract. |

## Reproduction of the contract gaps

These are counterexamples to the written acceptance predicates, **not results
from a running implementation**. Ran Python 3 with exact rational arithmetic
for rule 2 and a synthetic response trace for AC-25; observed:

```text
candidate: da719376088098b0fb17413d6e0897f2c593e6a6
working SPEC equals candidate: True
AC inventory: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27

AC-3, default refill = 1 token/second:
empty at t=0; last 429 at t=0.250; tokens=0.250; Retry-After=1
advance 0.999 seconds -> 201; remaining tokens=0.249
advance 0.001 seconds -> 429; remaining tokens=0.250
advance 0.000 seconds -> 429; remaining tokens=0.250
AC-3 expects: 429,201,429

AC-25 synthetic trace:
received HTTP responses: [302]
all received statuses 2xx/3xx: True
accepted requests lost with no response: 1
connection refused separately: 1
```

Reproduce the latter with three requests: an accepted request returns 302,
another accepted request closes before a response, and a new connection is
refused. Checking `all(200 <= status < 400 for status in received_statuses)`
passes while an accepted request is lost. A configured 10 s timeout and a
generic shutdown-completed log do not identify that lost request. A-16 already
states the intended stronger promise; carry it into the acceptance check.

RQ-03 is directly reproduced by reading the two incompatible response
contracts at the cited lines; no runtime experiment is needed.

## Disposition and residual risks

Return to requirements for RQ-01 and request RQ-02/RQ-03 in passing. No human
decision is needed: these repairs clarify the existing token-bucket and
graceful-shutdown contracts and preserve the inherited URL contract.

Design still owes the named checks for bounded limiter memory, template/status
metric support and pre-application server logging (A-10/A-17); release still
owes container, restart and latency measurements. This review does not claim
those outcomes or open another review assignment.

## Self-check

- Candidate SHA resolved in Git; reviewed SPEC bytes equal that candidate.
  Other seats may advance the main checkout; unrelated untracked rig startup
  files were present and untouched. No product worktree was switched.
- One changed file, one fully reviewed file; all 27 ACs inspected.
- No product gate run for this documentation-only requirements packet; no
  runtime, container or performance success claimed. Counterexample arithmetic
  and the response predicate were executed; source contradictions were read.
- Each finding has severity, source location, evidence and a required change.
- Verdict follows the severity rule: one HIGH blocks; two MEDIUM do not.
- Review ledger row appended. Edits are confined to `docs/review/`.
