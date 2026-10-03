# Testing approach

Two layers are tested here: the **product** (automated, gated at 100 %
coverage) and the **factory** (verified by effect: the orchestration's own
records). This page explains how each is tested, how to read the reports, and
what the honest limits are.

## 1. Product tests

| Suite | Location | What it proves | Runs in |
|---|---|---|---|
| Unit (`test`) | `src/test/java` | a rule or component in isolation, no Spring context (today: the request-id filter lifecycle, the ping controller, application bootstrap; later: code generation, validation, expiry rules, hashing) | `scripts/gw test` (seconds) |
| Functional (`functionalTest`) | `src/functionalTest/java` | the public HTTP journeys end to end — `@SpringBootTest` + `MockMvc` under the `functional` profile against an in-memory H2 database with Flyway applied; one test per acceptance criterion, named after it (`AC5_wrongMethodIsProblemDetailWithRequestId`) | `scripts/gw functionalTest` |
| Installed smoke | `scripts/smoke.sh` | a *running* instance (jar or container) answers the journey as a user would | release prep |
| Dependency advisories | `tools/dep-advisories.mjs` | the resolved runtime classpath checked against the OSV database; raw response kept under the mission's `release/` | release prep (needs network) |

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
- the dry-run mission `00-hello` carries one endpoint through every step and
  all three human gates (mission plan-lock, slice plan-lock, ship sign-off)
  before any product work; its release record is `missions/00-hello/RELEASE.md`.
  The brownfield mission adds fault-injection drills for the retry, rollback,
  fallback and safe-stop paths (`docs/scenarios/drills.md`).

## 5. Limits (stated, not hidden)

- Functional tests use `MockMvc` (servlet layer in-process), which cannot see
  servlet-container metadata: finding QA-01 (`docs/qa/01-ping/findings.md`) was
  visible only on a live Tomcat. Real sockets are crossed by QA's by-effect
  captures against `bootRun` and by the installed smoke against the jar and the
  container; only the smoke crosses the container boundary.
- The H2 database in tests is in-memory; the product's file-mode H2 is exercised
  by the installed smoke, which runs the jar with its shipped datasource. No
  Flyway migration exists yet, so migration history is not yet evidence.
- Coverage measures the production code under `src/main`; Gradle build logic
  and shell tooling (`scripts/`, `tools/`) are exercised by use, not by tests.
