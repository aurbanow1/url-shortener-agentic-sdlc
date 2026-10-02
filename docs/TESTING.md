# Testing approach

Two layers are tested here: the **product** (automated, gated at 100 %
coverage) and the **factory** (verified by effect: the orchestration's own
records). This page explains how each is tested, how to read the reports, and
what the honest limits are.

## 1. Product tests

| Suite | Location | What it proves | Runs in |
|---|---|---|---|
| Unit (`test`) | `src/test/java` | domain logic in isolation: code generation, validation, expiry rules, hashing, filters | `scripts/gw test` (seconds) |
| Functional (`functionalTest`) | `src/functionalTest/java` | the public HTTP journeys end to end — `@SpringBootTest` + `MockMvc` against a temporary H2 database with Flyway applied; one test per acceptance criterion, named after it (`AC03_duplicateAliasReturns409Problem`) | `scripts/gw functionalTest` |
| Installed smoke | `scripts/smoke.sh` | a *running* instance (jar or container) answers the journey as a user would | release prep |

**The gate.** `scripts/gw check` runs both suites and `jacocoTestCoverageVerification`
over their merged execution data with `LINE` and `BRANCH` minimum `1.0`. A
candidate cannot leave the Development Agent, pass QA, or be merged by the
Integrator while the gate is red.

**Test-first.** The Development Agent writes the failing test for each
acceptance criterion before the production code and must watch it fail for the
right reason (`test-driven-development` skill); the Code Review Agent checks
that tests test behaviour, not implementation.

**Independence.** QA and review run on a different model runtime (Codex) than
the builder (Claude Code), against the exact candidate SHA the builder named;
the QA Agent is read-only on product code.

## 2. Reading the coverage reports

Per slice, the QA Agent commits three JaCoCo reports (HTML, XML, CSV):

| Report | Path | Meaning |
|---|---|---|
| unit | `docs/qa/coverage/<slice>/unit/` | lines/branches exercised by the unit suite alone |
| functional | `docs/qa/coverage/<slice>/functional/` | lines/branches exercised by the HTTP journeys alone |
| merged | `docs/qa/coverage/<slice>/all/` | the number the gate enforces (both suites) |

`docs/qa/coverage/<slice>/SUMMARY.md` states the percentages per suite and
overall. Both per-suite reports are informational: the unit suite is not
expected to reach 100 % of controller code, nor the functional suite 100 % of
domain branches — the merged report must.

`docs/qa/TRACEABILITY.md` maps every acceptance criterion to the test(s) that
prove it, and every test back to a criterion or business rule. This is the
"functional coverage" view: not lines, but promises.

## 3. Gaps policy

100 % is the target and the gate. When it cannot honestly be met, the gap is
written down in `docs/qa/GAPS.md` (slice, what, why, compensating manual check,
status) — never configured away. Exclusions in `build.gradle.kts` are not
permitted without a corresponding `GAPS.md` entry and a reviewer's verdict.

## 4. Testing the factory

The orchestration layer is not unit-tested; it is **verified by effect** on
every mission:

- the per-slice workflow and mission lifecycle are validated before use
  (`rig workflow validate`, `rig workflow compile`) and the compiled graph is
  committed under `docs/evidence/<mission>/compiled-graph.json`;
- every step closure, gate park and human decision is an append-only record
  exported to `docs/evidence/<mission>/` (`tools/evidence-export.sh`);
- `tools/sdlc-metrics.mjs` derives success rate, retries, rollbacks, MTTR,
  latency and human wait from those records;
- the dry-run mission `00-hello` exercised every step and all three human gates
  before any product work; the brownfield mission adds fault-injection drills
  for the retry, rollback, fallback and safe-stop paths (`docs/scenarios/drills.md`).

## 5. Limits (stated, not hidden)

- Functional tests use `MockMvc` (servlet layer in-process); the installed smoke
  is the only test that crosses a real socket and the container boundary.
- The H2 database in tests is in-memory; the product's file-mode H2 is covered
  by the smoke run and by Flyway's migration history.
- Coverage measures the production code under `src/main`; Gradle build logic
  and shell tooling (`scripts/`, `tools/`) are exercised by use, not by tests.
