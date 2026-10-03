# Scenario: greenfield — mission `01-greenfield-core`

> Numbers marked `[final]` are copied from `missions/01-greenfield-core/RELEASE.md`
> and `docs/metrics/` at mission close.

## Decomposition

The human's brief (`missions/01-greenfield-core/SPEC.md`) allocated the product
baseline's ids (FR-1…FR-10; NFR L1–L3, R1–R6, S1, S3–S6, P1, O1–O3, A1–A2,
M1–M3, X1) to slices. The lead proposed four slices over three waves; the
decomposition review found that the `429`/`Retry-After` contract had no owner
in the shared OpenAPI document (DC-01) and, after the fix, a circular wait in
wave two (DC-02); both were corrected before the human saw the plan. The human
approved at the mission plan-lock and confirmed the eight `assumed` requirement
rows. The fast plan (D7) then moved `04-audit-read` to the brownfield mission,
leaving:

| Wave | Slices | Shared-file custody |
|---|---|---|
| w1 | `01-create-redirect` — links, audit log, problem details, OpenAPI export, the Tomcat/Jackson overrides as a first gated commit | foundation |
| w2 | `02-analytics` ∥ `03-operate` | `docs/api/openapi.json` to 02, `application.properties` to 03; 02 merges before 03 hands off |

## Orchestration

Each slice ran the nine-step slice workflow with an independent review after
every producing step; QA, code and security review ran on the other model
family (GPT-6.1-Sol, GPT-6-Astra) against the exact candidate SHA. Slice
plan-locks were delegated to the lead (D11) and recorded; the mission plan-lock
and the ship sign-off were the human's. The two w2 slices ran as concurrent
instances with the second judge pair (`qa2`, `review2`) on `03-operate`.

What the loops caught before merge:

| Slice | Step | Finding | Loop |
|---|---|---|---|
| 01 | requirements_review | RQ-01 — a `422` on an already-bound idempotency key must not unbind it | 1 (3 min) |
| 01 | design_review | DR-01 throwable/driver values leaking into error bodies; DR-02 reflected media type in `415` | 1 |
| 02 | design_review | DR-01 — bounded shutdown without honest accounting of in-flight clicks; converged through a focused escalation to the lead | 2 |
| 02 | code_review | CR-01 — midnight test flaky in isolation (raced a 1 ms expiry) | 1 (3 min) |
| 03 | design_review | first pass FAIL, fixed | 1 |
| 03 | code_review + security | CR-01/SEC-01 rate-limiter race (120 admissions in 1 ms, proven with an actual-class probe); CR-02 smoke reader failed open on a truncated response | 1 |
| mission | release_review | RR-01 rollback procedure did not preserve the loopback bind; RR-02/03 reporting | 1 |

## Validation

- Fresh gate on the shipped SHA: 165 unit + 155 functional tests, 0 failures,
  442/442 lines and 162/162 branches merged, Javadoc doclint green `[final]`.
- Installed smoke of the jar and the container on loopback; bench at the
  specified offered rates: redirects 100/s with p95 2.2 ms (target ≤ 20 ms),
  creates 20/s with p95 2.8 ms (≤ 50 ms), no measurable added p95 from click
  recording `[final]`.
- OSV: 0 advisories on 97 runtime dependencies (the ten open at the dry run
  were closed by the overrides); secret scan clean.
- One criterion the human decided: AC-28's published-port clause is cut by
  macOS Docker's host forwarder (6/6) while the service drains correctly on
  every path that reaches it; shipped as a disclosed host gap, the SPEC
  amended to the paths that reach the service, qa2 re-judged.

## Governance events (all in the queue exports under `docs/evidence/01-greenfield-core/`)

mission plan-lock (human) · decomposition rework (DC-01, DC-02) · three
delegated slice plan-locks (lead) · one focused convergence escalation · wave
review from two vantages · release review FAIL → rework → PASS · ship sign-off
(human) with an explicit, recorded exception.

## Metrics

From `docs/metrics/README.md` (generated 2026-10-03T17:44:03.313Z from the workflow trails and queue transitions exported under `docs/evidence/01-greenfield-core/`):

| Instance | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|
| mission lifecycle `01-greenfield-core` | 14.1 h | 8 | 41 | 0 | 2 | 33 min | – |
| `01-create-redirect` | 3.3 h | 12 | 13 | 6 | 1 | 0 s | 17 min |
| `02-analytics` | 3.4 h | 13 | 16 | 7 | 0 | 0 s | 36 min |
| `03-operate` | 5.9 h | 15 | 22 | 10 | 1 | 0 s | 36 min |

Retries count failed verdicts plus step re-entries (the review loops listed above); the two rollbacks are the integrator's rehearsed reverts, not reverts of `main`. Human wait is the time packets spent parked on `human@kernel` — the mission plan-lock and the ship sign-off — reported separately from agent throughput.
