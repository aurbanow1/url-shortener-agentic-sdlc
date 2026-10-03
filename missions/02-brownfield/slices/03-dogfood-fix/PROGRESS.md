# Progress — Dogfood Fix

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [x] Implementation complete — candidate `4fe7042`, rebased onto `01-audit-read`'s merge `cb148c4` and `main` `15db6c5` (builder, 2026-10-03)
- [x] Tests passing — `check --rerun-tasks` on `4fe7042`: unit 204/204, functional 207/207, 508/508 lines, 194/194 branches (`proof/builder-check-4fe7042.txt`; builder run, QA re-runs independently)
- [ ] Review approved

## Builder

- [x] Impact re-check on this base (it replaces design §13 step 1). `GET /api/audit?limit=0` answers `400` with one `errors` element (`01-audit-read`'s `AC09` journey, and AC-2 case (e) here). `docs/api/openapi.json` carries the audit operation, whose `400`/`403`/`500` reference `ProblemDetail`. No other change to the impact analysis.
- [x] Test first, W2-01: `9b2788a` (test), red in `proof/red-w2-01-openapi.txt` and `red-w2-01-report.xml`, then `edc1815` (fix and regenerated document)
- [x] Test first, W2-03: `3224036` (test), red in `proof/red-w2-03-metrics.txt`, `red-w2-03-report.xml` and `red-w2-03-scrape-report.xml`, then `5233c29` (fix and `MetricsConfigTest`)
- [x] AC-8 `README.md` check: lines 12, 18 and 39 mention metrics generically. No line describes a problem member, the problem schema or a metric tag, so no change is needed.
- [x] AC-9, additions only: `git diff --stat 35590f0 5233c29` on `OpenApiDocumentTest` and `HealthMetricsJourneyTest` shows 143 insertions and 0 deletions
- [x] Rebased onto `01-audit-read`'s merge commit (and `main` `15db6c5`). The regenerated `docs/api/openapi.json` was unchanged. `check` was re-run; handed off as `4fe7042`.


## QA

- [x] All nine ACs/five rules independently verified on exact 4fe7042.
- [x] Fresh 204 unit/207 functional; unchanged merged-baseline 203/202 green.
- [x] Merged 508/508 lines194/194 branches;354 copied report hashes checked.
- [x] Full six-case wire equality, live/committed schema equality, pathless numeric gauges, JSON logs and audit effects captured from real jars.
- [x] Independent red controls cce7cf7/72dfffb; ADR chronology/index and README check recorded.
- [x] All 233 source methods mapped; gap row written, QA-OPR-02 closed.
- [x] Apps stopped; exact candidate preserved, inherited gradlew.bat line endings disclosed.
- [ ] Attributed proof receipts committed and qa_check handed to review.
