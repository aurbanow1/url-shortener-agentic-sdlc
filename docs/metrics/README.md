# SDLC reliability metrics

Generated 2026-10-04T01:52:27.811Z by `tools/sdlc-metrics.mjs` from OpenRig workflow trails and queue transition logs (docs/evidence exports).

## Totals

| Metric | Value |
|---|---|
| Workflow instances (active / completed / failed / aborted) | 17 (3 / 13 / 0 / 1) |
| Instance success rate (completed ÷ terminal) | 0.929 |
| Step closures (failed) | 230 (17) |
| Step success rate | 0.926 |
| Retries (failed closures + step re-entries) | 51 |
| Rollbacks (revert notes + engine resumes) | 8 |
| MTTR, mean (failed closure → next successful closure of that step) | 28 min |
| End-to-end latency, completed instances p50 / p95 | 5.6 h / 8.7 h |
| Time parked on the human (all gates) | 8.7 h |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
| 01M3Z8AJ1E… | 00-hello | completed | 5.6 h | 7 | 21 | 0 | 0 | 31 min | – |
| 01M3ZA8Q39… | urlshort-slice | completed | 3.8 h | 13 | 14 | 6 | 0 | 14 min | 21 min |
| 01M3ZXEXAM… | 01-greenfield-core | completed | 14.1 h | 9 | 43 | 0 | 2 | 33 min | – |
| 01M40149S2… | urlshort-slice | completed | 3.3 h | 12 | 13 | 6 | 1 | 0 s | 17 min |
| 01M40CP0JV… | urlshort-slice-delegated | completed | 3.4 h | 13 | 16 | 7 | 0 | 0 s | 36 min |
| 01M40CPBYR… | urlshort-slice-delegated-b | completed | 5.9 h | 15 | 22 | 10 | 1 | 0 s | 36 min |
| 01M40RVNDQ… | 03-ambiguous-analytics | active | 14.3 h | 5 | 5 | 0 | 0 | 3.8 h | – |
| 01M40SN34E… | 02-brownfield | waiting | 14.0 h | 4 | 8 | 0 | 1 | 3.6 h | – |
| 01M416Z3CM… | urlshort-slice | completed | 8.7 h | 8 | 15 | 0 | 0 | 0 s | – |
| 01M416ZY5N… | urlshort-slice-delegated | completed | 6.1 h | 15 | 16 | 10 | 0 | 0 s | 41 min |
| 01M4170AA9… | urlshort-slice-delegated-b | completed | 7.4 h | 12 | 18 | 6 | 2 | 0 s | 38 min |
| 01M41B1ABG… | urlshort-drill | aborted | 3 min | 0 | 2 | 3 | 1 | 0 s | – |
| 01M41HFT4C… | urlshort-slice-delegated-b | completed | 1.8 h | 8 | 9 | 0 | 0 | 0 s | – |
| 01M41HG4P6… | urlshort-slice-delegated-b | completed | 5.6 h | 8 | 11 | 0 | 0 | 0 s | – |
| 01M41HGF6A… | urlshort-slice-delegated | completed | 4.0 h | 8 | 10 | 0 | 0 | 0 s | – |
| 01M4212A8B… | urlshort-drill | completed | 12 min | 3 | 4 | 3 | 0 | 0 s | 3 min |
| 01M425CY9Z… | urlshort-slice-delegated-b | active | 1.3 h | 3 | 3 | 0 | 0 | 0 s | – |

## Derivations and honest limits

- **Source of truth**: `rig workflow trace --json` (append-only step trail; one entry per closed packet with `closureReason` handoff/done/failed) and `rig queue transitions --json` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Retry** counts an artifact verdict of `failed` (QA, code review or security review sent the candidate back) plus any step closed more than once with a non-`waiting` exit. A `waiting` closure re-presents the same step (the integrator waiting on a slice's proof) and is not counted. One remediation round therefore shows as one failed closure plus two re-entries (the checking step and the building step both run again). A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts transition or evidence notes mentioning revert/rollback and engine `resumeCount`; a Git revert by the integrator is visible in `git log` as well.
- **MTTR** is measured from a failed closure to the next successful closure of the same step, i.e. the time to repair the candidate and pass that check again. Instances with no failure have no MTTR (shown as –), not zero.
- **Human wait** is time a packet spent parked on `human@kernel`; it is reported separately so agent throughput and human latency are not conflated. The per-instance column sums the gate packets in that instance's trail; the total counts each packet once, because a slice gate also appears in the mission trail as the blocker of `wave_integration`.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: `rig usage top --json` in `docs/evidence/<mission>/usage-top.json`.
