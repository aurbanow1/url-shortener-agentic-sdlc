# SDLC reliability metrics

Generated 2026-10-04T04:46:43.295Z by `tools/sdlc-metrics.mjs` from OpenRig workflow trails and queue transition logs (live daemon read).

## Totals

| Metric | Value |
|---|---|
| Workflow instances (active / completed / failed / aborted) | 17 (1 / 15 / 0 / 1) |
| Instance success rate (completed ÷ terminal) | 0.938 |
| Step closures (failed) | 249 (18) |
| Step success rate | 0.928 |
| Retries (failed closures + step re-entries) | 55 |
| Rollbacks (revert notes + engine resumes) | 9 |
| MTTR, mean (failed closure → next successful closure of that step) | 29 min |
| End-to-end latency, completed instances p50 / p95 | 5.6 h / 14.1 h |
| Time parked on the human (all gates) | 8.8 h |

## Per instance

| Instance | Workflow | Status | E2E latency | Hops | Closures | Retries | Rollbacks | Human wait | MTTR |
|---|---|---|---|---|---|---|---|---|---|
| 01M3Z8AJ1E… | 00-hello | completed | 5.6 h | 7 | 21 | 0 | 0 | 31 min | – |
| 01M3ZA8Q39… | urlshort-slice | completed | 3.8 h | 13 | 14 | 6 | 0 | 14 min | 21 min |
| 01M3ZXEXAM… | 01-greenfield-core | completed | 14.1 h | 9 | 43 | 0 | 2 | 33 min | – |
| 01M40149S2… | urlshort-slice | completed | 3.3 h | 12 | 13 | 6 | 1 | 0 s | 17 min |
| 01M40CP0JV… | urlshort-slice-delegated | completed | 3.4 h | 13 | 16 | 7 | 0 | 0 s | 36 min |
| 01M40CPBYR… | urlshort-slice-delegated-b | completed | 5.9 h | 15 | 22 | 10 | 1 | 0 s | 36 min |
| 01M40RVNDQ… | 03-ambiguous-analytics | completed | 15.7 h | 9 | 12 | 0 | 1 | 3.9 h | – |
| 01M40SN34E… | 02-brownfield | active | 16.9 h | 5 | 9 | 0 | 1 | 3.6 h | – |
| 01M416Z3CM… | urlshort-slice | completed | 8.7 h | 8 | 15 | 0 | 0 | 0 s | – |
| 01M416ZY5N… | urlshort-slice-delegated | completed | 6.1 h | 15 | 16 | 10 | 0 | 0 s | 41 min |
| 01M4170AA9… | urlshort-slice-delegated-b | completed | 7.4 h | 12 | 18 | 6 | 2 | 0 s | 38 min |
| 01M41B1ABG… | urlshort-drill | aborted | 3 min | 0 | 2 | 3 | 1 | 0 s | – |
| 01M41HFT4C… | urlshort-slice-delegated-b | completed | 1.8 h | 8 | 9 | 0 | 0 | 0 s | – |
| 01M41HG4P6… | urlshort-slice-delegated-b | completed | 5.6 h | 8 | 11 | 0 | 0 | 0 s | – |
| 01M41HGF6A… | urlshort-slice-delegated | completed | 4.0 h | 8 | 10 | 0 | 0 | 0 s | – |
| 01M4212A8B… | urlshort-drill | completed | 12 min | 3 | 4 | 3 | 0 | 0 s | 3 min |
| 01M425CY9Z… | urlshort-slice-delegated-b | completed | 3.2 h | 11 | 14 | 4 | 0 | 0 s | 35 min |

## Derivations and honest limits

- **Source of truth**: `rig workflow trace --json` (append-only step trail; one entry per closed packet with `closureReason` handoff/done/failed) and `rig queue transitions --json` (every state change of a packet with actor and timestamp). Nothing is self-reported by agents.
- **Retry** counts an artifact verdict of `failed` (QA, code review or security review sent the candidate back) plus any step closed more than once with a non-`waiting` exit. A `waiting` closure re-presents the same step (the integrator waiting on a slice's proof) and is not counted. One remediation round therefore shows as one failed closure plus two re-entries (the checking step and the building step both run again). A retry is a governance event working as designed, not a defect of the factory.
- **Rollback** counts transition or evidence notes mentioning revert/rollback and engine `resumeCount`; a Git revert by the integrator is visible in `git log` as well.
- **MTTR** is measured from a failed closure to the next successful closure of the same step, i.e. the time to repair the candidate and pass that check again. Instances with no failure have no MTTR (shown as –), not zero.
- **Human wait** is time a packet spent parked on `human@kernel`; it is reported separately so agent throughput and human latency are not conflated. The per-instance column sums the gate packets in that instance's trail; the total counts each packet once, because a slice gate also appears in the mission trail as the blocker of `wave_integration`.
- Active instances contribute latency up to the generation time and are excluded from the p50/p95.
- Token burn per seat is a separate record: `rig usage top --json` in `docs/evidence/<mission>/usage-top.json`.

## Export-container scope limitation

`tools/sdlc-metrics.mjs` derives rows from every evidence directory it finds.
The generated per-instance table currently retains the directory as the
mission label; an export for one mission can therefore contain workflow
instances belonging to another mission or to a drill. Do not sum those rows as
a mission aggregate. For mission-specific reporting, use the lifecycle
instance and slice instance named by the mission's bound graph and cite the
corresponding `docs/evidence/<mission>/INDEX.md`; for example mission 03's
lifecycle is `01M40RVNDQ0KT7FPWN1KJW0DC3` and its analytics slice is
`01M416Z3CM54YQTX93V4KG0CPS`. This is a labeling limitation, not a claim that
the underlying traces are missing.

## Final mission03 snapshot and derivation qualifications

This coordinated refresh uses the committed final export at
`3c48d0a9e744162179177145e3fac9471e8ccf25`; its
[INDEX](../evidence/03-ambiguous-analytics/INDEX.md) and
[validation](../evidence/03-ambiguous-analytics/final-validation.json)
retain the human's local-use approval of product `50ad9c3`, its accepted but
unverified exact-SHA hosted CI gap, and the final attributed proof snapshot.
The exporter captured 17 instances and 193 packets before its own closure
and mission_close. Metrics generation at 2026-10-04T03:08:55.760Z does not
make those exported states a live observation of the daemon.

| Bound mission03 instance | Exported status / latency | Retries / rollback heuristic | MTTR / human gate wait |
|---|---|---|---|
| Lifecycle `01M40RVNDQ0KT7FPWN1KJW0DC3` | waiting at evidence_export; 55,901s elapsed to generation, not completed E2E | 0 / 1 note match | absent / 13,928s |
| Analytics `01M416Z3CM54YQTX93V4KG0CPS` | completed; 31,241s | 0 / 0 | absent / 0s counted |

The lifecycle's one rollback match is a release-preparation coordination
note: "My bench has not begun; waiting for your rollback gate completion".
It describes a wait before timed runs, without establishing a production
rollback. The separately documented mission03 rehearsal retains its own
evidence. Both instances have
zero failed closures and zero counted retries. No failure means no measured
MTTR, rather than a measured zero.

The factory-wide 52 retries and nine rollback counts include other missions
and drills. Rollback is a text-match/resume heuristic. The step-success
denominator includes waiting closures. The implemented MTTR picks the next
non-failed closure of the same step, which can be a wait, and the overall mean
averages per-instance means. It is not a production outage-recovery SLO.

The human-wait detector recognizes engine gate parks and explicit blocked-on
fields. The custom analytics ambiguity park lacks that field in exported
transitions, so its 22m6.732s wait is omitted from the slice's numeric zero.
The lifecycle's counted 13,928s includes the 13,836s mission plan-lock and
92s ship gate. Active/waiting latency uses the generation time, while states
and trail events come from the earlier export; completed-instance percentiles
exclude those unfinished instances. Future export/mission-close events are
not part of this snapshot.


## Mission02 final preparation refresh

Generated2026-10-04T04:46:43.295Z with --live in an isolated workspace containing only the fresh mission02 raw export. The unchanged generator also prefers exported packet files over live reads, so isolation prevents stale mission03 packet/trace copies from overriding current mission02 evidence. All17 instance rows and every packet metric reproduce from [frozen inputs](../../missions/02-brownfield/release/final-30f8de4e/final-metrics-inputs.tar.gz) and the [provenance](../../missions/02-brownfield/release/final-30f8de4e/final-metrics-provenance.json); [replay](../../missions/02-brownfield/release/final-30f8de4e/final-metrics-replay.json) freezes only generation time and normalizes the export-directory label. The seven bound mission02 instances are identified in its RELEASE, not by this table's mission label.

The older8d3c536 report cannot be reproduced from its committed inputs: they yield233closures/9rollback heuristic hits instead of230/8; D21 has6closures versus3 and a release coordination note adds one text match. The missing then-used mission03 input set remains a historical custody qualification, not a silently corrected snapshot. Later mission03's committed/frozen c735190b snapshot above is a separate record.

Current factory totals17instances,15completed/1active/1aborted;249closures/18failed;55retry counts;9rollback heuristics;1711s mean-of-instance MTTR. Mission02 lifecycle itself has0failed/0retries and no measured MTTR; its6slices have6failed closures and20retry counts across requirements/design/code repair loops. These are factory governance events, not production incidents. Human gate wait counted for its lifecycle is12869s; custom/delegated parks can be omitted.

## Self-check

The fresh generator output was archived and reproduced from the preserved raw input set: all17 instance rows, packet metrics and totals match, with only live/export labeling normalized. The product and mission03 frozen evidence remain unchanged; no publication.
