# SDLC reliability metrics

Generated 2026-10-04T18:41:37.041Z by `tools/sdlc-metrics.mjs` from OpenRig workflow trails and queue transition logs (live daemon read).

## Totals

| Metric | Value |
|---|---|
| Workflow instances (active / completed / failed / aborted) | 17 (0 / 16 / 0 / 1) |
| Instance success rate (completed ÷ terminal) | 0.941 |
| Step closures (failed) | 254 (18) |
| Step success rate | 0.929 |
| Retries (failed closures + step re-entries) | 55 |
| Rollbacks executed (rehearsals and drills with raw evidence + reverts on `main`) | 6 (6 + 0) |
| Engine recoveries (resumes / aborted instances) | 1 / 1 |
| MTTR, mean (failed closure → next handoff or done closure of that step) | 29 min |
| End-to-end latency, completed instances p50 / p95 | 5.6 h / 15.7 h |
| Time parked on the human (all gates) | 8.8 h |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
| 01M3Z8AJ1E… | 00-hello | completed | 5.6 h | 7 | 21 | 0 | 1 | 31 min | – |
| 01M3ZA8Q39… | urlshort-slice | completed | 3.8 h | 13 | 14 | 6 | 0 | 14 min | 21 min |
| 01M3ZXEXAM… | 01-greenfield-core | completed | 14.1 h | 9 | 43 | 0 | 2 | 33 min | – |
| 01M40149S2… | urlshort-slice | completed | 3.3 h | 12 | 13 | 6 | 0 | 0 s | 17 min |
| 01M40CP0JV… | urlshort-slice-delegated | completed | 3.4 h | 13 | 16 | 7 | 0 | 0 s | 36 min |
| 01M40CPBYR… | urlshort-slice-delegated-b | completed | 5.9 h | 15 | 22 | 10 | 0 | 0 s | 36 min |
| 01M40RVNDQ… | 03-ambiguous-analytics | completed | 15.7 h | 9 | 12 | 0 | 1 | 3.9 h | – |
| 01M40SN34E… | 02-brownfield | completed | 17.7 h | 9 | 14 | 0 | 2 | 3.6 h | – |
| 01M416Z3CM… | urlshort-slice | completed | 8.7 h | 8 | 15 | 0 | 0 | 0 s | – |
| 01M416ZY5N… | urlshort-slice-delegated | completed | 6.1 h | 15 | 16 | 10 | 0 | 0 s | 41 min |
| 01M4170AA9… | urlshort-slice-delegated-b | completed | 7.4 h | 12 | 18 | 6 | 0 | 0 s | 38 min |
| 01M41B1ABG… | urlshort-drill | aborted | 3 min | 0 | 2 | 3 | 0 | 0 s | – |
| 01M41HFT4C… | urlshort-slice-delegated-b | completed | 1.8 h | 8 | 9 | 0 | 0 | 0 s | – |
| 01M41HG4P6… | urlshort-slice-delegated-b | completed | 5.6 h | 8 | 11 | 0 | 0 | 0 s | – |
| 01M41HGF6A… | urlshort-slice-delegated | completed | 4.0 h | 8 | 10 | 0 | 0 | 0 s | – |
| 01M4212A8B… | urlshort-drill | completed | 12 min | 3 | 4 | 3 | 0 | 0 s | 3 min |
| 01M425CY9Z… | urlshort-slice-delegated-b | completed | 3.2 h | 11 | 14 | 4 | 0 | 0 s | 35 min |

## Derivations and honest limits

- **Source of truth**: `rig workflow trace --json` (append-only step trail; one entry per closed packet with `closureReason` handoff/done/failed) and `rig queue transitions --json` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Retry** counts an artifact verdict of `failed` (QA, code review or security review sent the candidate back) plus any step closed more than once with a non-`waiting` exit. A `waiting` closure re-presents the same step (the integrator waiting on a slice's proof) and is not counted. One remediation round therefore shows as one failed closure plus two re-entries (the checking step and the building step both run again). A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts only rollbacks that were executed: a merged change reverted, or a migration rolled back with the earlier binary started on the rolled-back data, each checked by the gate or an installed smoke. They are listed in [`rollbacks.json`](rollbacks.json) with their raw evidence, and the generator refuses to run if a listed file is missing; reverts that landed on `main` are counted from `git log`. Plans and descriptions of rollbacks do not count, and engine resumes and aborts are reported separately as recoveries. OpenRig 0.6.3 has no rollback event of its own, so the register is the record; every entry points at raw files.
- **MTTR** is measured from a failed closure to the next `handoff` or `done` closure of the same step, i.e. the time to repair the candidate and pass that check again; a `waiting` closure is not a recovery. It measures repair of a rejected candidate, not production incident recovery: no production incident occurred. Instances with no failure have no MTTR (shown as –), not zero.
- **Reproducibility**: when the same record appears in several exports, the freshest copy wins (trace `instance.version`, highest `transitionId`, packet `tsUpdated`). `docs/evidence/run-end/` holds the exact inputs of this report, and `node tools/sdlc-metrics.mjs --check` regenerates it offline and fails on any difference.
- **Human wait** is time a packet spent parked on `human@kernel`; it is reported separately so agent throughput and human latency are not conflated. The per-instance column sums the gate packets in that instance's trail; the total counts each packet once, because a slice gate also appears in the mission trail as the blocker of `wave_integration`.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: `rig usage top --json` in `docs/evidence/<mission>/usage-top.json`.
- Notes on earlier snapshots, generated under the previous definitions, are kept in [`HISTORY.md`](HISTORY.md).
