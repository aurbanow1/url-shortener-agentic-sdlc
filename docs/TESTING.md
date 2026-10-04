# Testing approach

Two layers are tested here: the **product** (automated, gated at 100 %
coverage) and the **factory** (verified by effect: the orchestration's own
records). This page explains how each is tested, how to read the reports, and
what the honest limits are.

## 1. Product tests

| Suite | Location | What it proves | Runs in |
|---|---|---|---|
| Unit (`test`) | `src/test/java` | a rule or component in isolation, no Spring context (code generation, URL validation, click reduction and salts, the token-bucket limiter, filters, problem details) | `scripts/gw test` (seconds) |
| Functional (`functionalTest`) | `src/functionalTest/java` | HTTP journeys with `@SpringBootTest`, both `MockMvc` and real loopback Tomcat requests, under the `functional` profile against H2 with Flyway applied; tests name the acceptance outcomes they cover | `scripts/gw functionalTest` |
| Installed smoke | `scripts/smoke.sh <base-url>` | a *running* instance (jar or container) answers the journey as a user would: health, ping, create → redirect → read → stats → retire, error cases, metric names, Prometheus, OpenAPI | release prep |
| Installed lifecycle | `scripts/smoke.sh --jar` / `--drain` / `--inspect` / `--restart` | the plain jar configured by environment; graceful shutdown with a held in-flight request; the container's binding, user, read-only filesystem and stop timeout; links surviving compose restart and down/up under load | release prep |
| Latency bench | `scripts/smoke.sh --bench <base-url>` (`tools/bench.mjs`) | redirect and create latency at the specified **offered** rates (100/s and 20/s for 60 s), open loop: request *i* is due at *i*/rate s whatever earlier requests do, and latency runs from the due time, so a slow response cannot hide later ones. Separate GET/HEAD runs compare redirects with and without click recording | release prep |
| Dependency advisories | `tools/dep-advisories.mjs` | the resolved runtime classpath checked against the OSV database; raw response kept under the mission's `release/` | release prep (needs network) |

**The gate.** `scripts/gw check` runs both suites and `jacocoTestCoverageVerification`
over their merged execution data with `LINE` and `BRANCH` minimum `1.0`. A
candidate cannot leave the Development Agent, pass QA, or be merged by the
Integrator while the gate is red.

**Test-first.** The Development Agent writes the failing test for each
acceptance criterion before the production code and must watch it fail for the
right reason (`test-driven-development` skill); the Code Review Agent checks
that tests test behaviour, not implementation.

**Independence.** Product builders run Claude Code; QA and code/security
reviewers run Codex on separate seats against the exact candidate SHA. QA is
read-only on product code. Human decision D17 moved requirements and release
authoring to Codex: their independent reviewers can share the author's runtime,
and requirements author/reviewer can share the model. Separate seats and
authorship boundaries remain, but those documents and release packages do not
claim cross-runtime review. SPECs written before D17 retain their original
attribution. See [the decision record](../PLAN.md) and each review receipt.

**Analytics journeys.** `StatsV2JourneyTest` covers the four per-UTC-day fields,
same-day distinct visitors, bot inclusion and separation of days.
`TrustedProxyClickJourneyTest` covers the limiter's shared identity rule and
aggregate/log privacy; `ClickMetricsJourneyTest` checks recorded/lost counter
deltas and exactly the static Prometheus labels. The inherited resilience
journeys also run with trusted proxies configured. Retention journeys check
startup and scheduled deletion, the cutoff day, validation, failure and the
operator hold. Statistics use the remaining rows; the process counters remain
cumulative after deletion. Same-day restart can overcount uniques, and old
proxy hashes are not repaired. The [analytics SPEC](../missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md)
states these qualifications; [the runbook](RUNBOOK.md) describes operation.

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

- `MockMvc` journeys (servlet layer in-process) cannot see
  servlet-container metadata: finding QA-01 (`docs/qa/01-ping/findings.md`) was
  visible only on a live Tomcat. Real sockets are crossed by QA's by-effect
  captures against `bootRun` and by the installed smoke against the jar and the
  container. Several functional journeys also start real loopback Tomcat; the
  installed smoke crosses the container boundary.
- The H2 database in tests is in-memory; the product's file-mode H2 is exercised
  by the installed smoke, which runs the jar with its shipped datasource, and by
  the container on its volume. Flyway Community applies the V1–V4 migrations
  forward only; there is no automated down-migration.
- The bench runs on the same laptop as the service, against one instance with
  its two rate budgets raised; one 60 s run per scenario is a regression signal,
  not a capacity claim. NFR-L3's `p95(GET) − p95(HEAD)` is a comparison proxy
  between separate runs, not the p95 of isolated click-recording cost. It includes
  run-to-run noise and other method differences; both methods still resolve
  client identity. In-suite fail-open/slow-store checks do not establish release
  p95/p99. Read the candidate's release benchmark and [GAPS.md](qa/GAPS.md).
- On a macOS host whose Docker engine runs in a Lima VM, a request still sending
  its body through the published port when the container stops is cut by the
  host's port forwarder (mission 01 `RELEASE.md` §3, AC-28); inside the VM and
  inside the container's network namespace the same request completes.
- Coverage measures the production code under `src/main`; Gradle build logic
  and shell tooling (`scripts/`, `tools/`) are exercised by use, not by tests.
