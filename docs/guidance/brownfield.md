# Brownfield guidance — changing a system that already works

Enhancements, refactors, bug fixes and test/documentation improvements on the
shipped shortener. The assignment grades *codebase reasoning*: showing you
know what a change touches before you touch it, and that existing behaviour
survives. Read by the Design Agent (impact analysis), the Development Agent,
and the Review Agent (`design_review`, `code_review`, `security_review`).

## 1. Start from the shipped behaviour, not from memory

- Read `docs/DESIGN.md`, `docs/api/openapi.json`, the functional tests and the
  Flyway history; then **run the shipped jar** and exercise the journey you are
  about to change (`scripts/http`). *Reason:* documentation drifts; the
  running system and its tests are the truth. *Check:* the impact analysis
  cites observed behaviour (status codes, headers, rows), not only files.
- List the consumers of what you change: endpoints, callers inside the code,
  stored data, log/metric fields, documents. *Check:* `grep` results and the
  test names appear in the analysis.

## 2. The impact analysis (`impact-analysis.md`, before design)

| Section | Content | Check |
|---|---|---|
| Change in one sentence | what and why (requirement ids) | matches the slice intent |
| Impacted modules | packages/classes touched, and those that *call* them | every class in the diff later appears here (reviewer diffs the list) |
| Impacted endpoints | method + path, request/response deltas, status/header changes | additive only, or a deprecation plan |
| Impacted schema & data | tables/columns, data volume, migration steps, rollback | expand → migrate → contract, each step independently shippable |
| Impacted data flows | redirect path, click recording, audit, stats | sequence diagram delta |
| Blast radius | what breaks if this is wrong; worst case; detection | names the alarm (test, log, metric, smoke) |
| Compatibility | existing links, codes, aliases, clients, stored rows | FR-13 proof: the pre-change functional suite still passes unchanged |
| Test impact | tests added (regression first), tests changed and why, tests removed and why | no test deleted to go green |
| Observability impact | new log fields/metrics, dashboards or runbook changes | runbook updated in the same slice |
| Risks & mitigations | ranked | each has an owner step (design, QA, release) |
| Self-check | honest line per row above | — |

## 3. Safe change management

1. **Additive API changes only**; a breaking change needs a new path and a
   deprecation note — never a silent rename or type change.
2. **Expand → migrate → contract** for schema: add nullable / new table, move
   data, then drop — three migrations, three deployable states, rollback
   written for each.
3. **Risky behaviour behind configuration**, default off, flipped on by a
   property documented in `docs/DESIGN.md`; remove the switch in a later slice.
4. **One concern per commit**, refactor commits separate from behaviour
   commits; the functional suite must be green after each.
5. **Rollback rehearsed**: `git revert` of the merge commit plus the migration
   rollback restores the previous state; the release record lists the exact
   commands and what data is lost (ideally nothing).

## 4. Bug fixes

- Reproduce with a **failing test first**, named after the defect
  (`DF03_expiredLinkStillRedirects`), at the lowest layer that shows it and in
  a functional journey if the user sees it.
- Fix the **root cause in the shared path**, not the symptom in one caller;
  grep every caller of the function you touch and check its siblings.
- Record cause → fix → proof in the slice `PROOF.md`; add the regression test
  to `docs/qa/TRACEABILITY.md` against the defect id.
- Defect sources: dogfood (`docs/qa/dogfood/<mission>.md`), QA findings,
  review findings. A **seeded** defect (used when no real one surfaced) is
  disclosed as seeded in `docs/scenarios/brownfield.md`.

## 5. Test and documentation improvements are first-class changes

- Close or re-justify every `docs/qa/GAPS.md` entry you touch; add the missing
  functional journey rather than widening a unit test.
- `README.md`, `docs/DESIGN.md`, `docs/api/openapi.json`, the runbook and
  the ADR index must describe the shipped behaviour after the slice; a
  reviewer diffing docs against behaviour is a valid finding source.
- Traceability both directions remains complete after the change.

## 6. Refactors

Behaviour-preserving by definition: no API, schema or log-field change; proven
by the unchanged functional suite plus coverage at the gate; separated from
feature work; justified by a named smell (duplication, a ceiling marked
`// ponytail:`, a review finding), not by taste.

## 7. Review checklist additions for brownfield slices

1. Impact analysis exists, predates the design, and its module list matches the diff.
2. Existing functional tests pass **unchanged** (compatibility); changed tests have a stated reason.
3. Migration follows expand/migrate/contract with written, rehearsed rollback.
4. Bug fix has a regression test that fails on the parent commit (reviewer verifies by checkout or by reasoning from the diff).
5. Docs, OpenAPI, runbook and ADR index updated in the same slice.
6. No deleted test, no lowered threshold, no new dependency without an ADR.
