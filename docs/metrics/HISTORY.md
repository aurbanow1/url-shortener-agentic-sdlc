# SDLC metrics: history of earlier snapshots

These notes were written beside earlier generations of [README.md](README.md), each dated, and are kept
unchanged as the record of those snapshots. **They use the previous definitions:**

- "rollback" counted transition and evidence notes that mentioned revert or rollback, plus engine
  resumes, so a described or planned rollback counted;
- MTTR could end at a `waiting` closure instead of a successful one;
- an offline regeneration let exports overwrite each other in folder order, so an older copy of an
  instance could replace a newer one.

An external review found all three (2026-10-04). The generator now counts only executed rollbacks
(`rollbacks.json`, raw evidence verified), ends MTTR at a `handoff` or `done` closure, keeps the
freshest copy of every record, and `--check` proves the committed numbers regenerate offline from
`docs/evidence/run-end/`. Under the new definitions the run has 6 executed rollbacks (and no revert
on `main`) instead of 10 rollback signals. MTTR is now the mean over every individual repair, 31 minutes
over 16 repairs, where the earlier snapshots reported a mean of per-instance means (29 minutes); every other
total is unchanged.

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

## Run-end refresh (all instances terminal)

Generated 2026-10-04T05:38:42.350Z with `--live` in a scratch git workspace
that contains **no** export directories, so every trace and packet was read
from the daemon and no exported copy could override a newer record. It was run
after mission 02's `mission_close` (lifecycle `01M40SN34E37K96B38JPG9K41X`
completed 2026-10-04T05:35:35.988Z), so this is the factory's final state: 17
instances, 16 completed, 1 aborted (DRILL 4), none active.

Against the mission 02 preparation refresh above, only the mission 02
lifecycle row changed: active to completed, five more step closures
(release_review through mission_close), and one more rollback text match. That
match is the ship sign-off packet, whose gate summary describes the rollback
recipe; no rollback was executed. Its counted human wait, 12,969 s, is the
12,869 s mission plan-lock plus the 100 s ship gate. Totals moved accordingly
(254 closures, 10 rollback heuristic hits, completed-instance E2E p95 15.7 h).
Every qualification above (scope labels, the rollback heuristic, MTTR as a
mean of per-instance means, omitted custom parks) still applies.
