# SDLC reliability metrics

Generated 2026-10-03T03:07:20.488Z by `tools/sdlc-metrics.mjs` from OpenRig workflow trails and queue transition logs (docs/evidence exports).

## Totals

| Metric | Value |
|---|---|
| Workflow instances (active / completed / failed / aborted) | 2 (0 / 2 / 0 / 0) |
| Instance success rate (completed ÷ terminal) | 1 |
| Step closures (failed) | 35 (2) |
| Step success rate | 0.943 |
| Retries (failed closures + step re-entries) | 6 |
| Rollbacks (revert notes + engine resumes) | 0 |
| MTTR, mean (failed closure → next successful closure of that step) | 21 min |
| End-to-end latency, completed instances p50 / p95 | 3.8 h / 3.8 h |
| Time parked on the human (all gates) | 46 min |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
| 01M3Z8AJ1E… | 00-hello | completed | 5.6 h | 7 | 21 | 0 | 0 | 31 min | – |
| 01M3ZA8Q39… | urlshort-slice | completed | 3.8 h | 13 | 14 | 6 | 0 | 14 min | 21 min |

## Derivations and honest limits

- **Source of truth**: `rig workflow trace --json` (append-only step trail; one entry per closed packet with `closureReason` handoff/done/failed) and `rig queue transitions --json` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Retry** counts an artifact verdict of `failed` (QA, code review or security review sent the candidate back) plus any step closed more than once with a non-`waiting` exit. A `waiting` closure re-presents the same step (the integrator waiting on a slice's proof) and is not counted. One remediation round therefore shows as one failed closure plus two re-entries (the checking step and the building step both run again). A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts transition or evidence notes mentioning revert/rollback and engine `resumeCount`; a Git revert by the integrator is visible in `git log` as well.
- **MTTR** is measured from a failed closure to the next successful closure of the same step, i.e. the time to repair the candidate and pass that check again. Instances with no failure have no MTTR (shown as –), not zero.
- **Human wait** is time a packet spent parked on `human@kernel`; it is reported separately so agent throughput and human latency are not conflated. The per-instance column sums the gate packets in that instance's trail; the total counts each packet once, because a slice gate also appears in the mission trail as the blocker of `wave_integration`.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: `rig usage top --json` in `docs/evidence/<mission>/usage-top.json`.
