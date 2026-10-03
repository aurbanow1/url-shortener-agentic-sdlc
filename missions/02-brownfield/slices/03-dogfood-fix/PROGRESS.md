# Progress — Dogfood Fix

> Durable acceptance state for this slice. In-process steps belong in the
> working agent's todo tool. See the `mission-slice-sop` skill and the
> conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo,
> `$OPENRIG_HOME/reference/sdlc-conventions.md` when installed).

## Acceptance

- [ ] Implementation complete — built and green as `5233c29`, stacked on `01-audit-read` `35590f0`. The final candidate is the rebase onto audit-read's merge commit, which is not on `main` yet (builder, 2026-10-03)
- [ ] Tests passing — `check --rerun-tasks` on the stacked `5233c29`: unit 201/201, functional 205/205, 506/506 lines, 190/190 branches (`proof/builder-check-5233c29.txt`); to be re-run on the rebased candidate
- [ ] Review approved

## Builder

- [x] Impact re-check on this base (it replaces design §13 step 1). `GET /api/audit?limit=0` answers `400` with one `errors` element (`01-audit-read`'s `AC09` journey, and AC-2 case (e) here). `docs/api/openapi.json` carries the audit operation, whose `400`/`403`/`500` reference `ProblemDetail`. No other change to the impact analysis.
- [x] Test first, W2-01: `9b2788a` (test), red in `proof/red-w2-01-openapi.txt` and `red-w2-01-report.xml`, then `edc1815` (fix and regenerated document)
- [x] Test first, W2-03: `3224036` (test), red in `proof/red-w2-03-metrics.txt`, `red-w2-03-report.xml` and `red-w2-03-scrape-report.xml`, then `5233c29` (fix and `MetricsConfigTest`)
- [x] AC-8 `README.md` check: lines 12, 18 and 39 mention metrics generically. No line describes a problem member, the problem schema or a metric tag, so no change is needed.
- [x] AC-9, additions only: `git diff --stat 35590f0 5233c29` on `OpenApiDocumentTest` and `HealthMetricsJourneyTest` shows 143 insertions and 0 deletions
- [ ] Rebase onto `01-audit-read`'s merge commit, regenerate `docs/api/openapi.json`, re-run `check`, hand off that SHA
