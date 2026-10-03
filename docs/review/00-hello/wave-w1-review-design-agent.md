# Wave w1 — structural review (design-agent vantage)

Reviewer: `design-agent@urlshort-factory` (Claude Code), second vantage for the
`00-hello` wave review. Request packet: `qitem-20261003015306-f1842acf`;
lifecycle packet `qitem-20261003015049-51d1ba00`. Date: 2026-10-03 UTC.

## Scope

- **Subject:** the merged product range `f43ecd1..42a25db4a9c24fba3221c1ade4044719cab39ee3`
  on `main` (merge commit `42a25db`, parents `3e4f573` and `f286a10`, tag
  `slice/01-ping/accepted`). Nine product, build and test files, +305/−1.
- **Lens:** structure, coherence with the locked `01-ping` design, drift from
  the locked SPEC and the mission doghouse, and the architecture guidance
  (`docs/guidance/architecture.md`). Not line-level style; that was the code
  review's job.
- **Judged against:** `missions/00-hello/SPEC.md` (doghouse), the locked
  `slices/01-ping/SPEC.md` at `4e581cc` and `design.md` at `d0521de`,
  `docs/DESIGN.md`, ADR-0001..0004, `docs/guidance/architecture.md`.
- **Independence note.** I wrote the design under review and the post-lock
  documentation corrections (DR-01 rework, ADR-0004 and `docs/DESIGN.md`
  updates for QA-01, commit `23f7a8c`). Those authored corrections are
  inputs here, not independently approved by this review. The verdict below
  is about the *product range* against the locked contracts. I did not read
  the review agent's wave review before writing this, so the two vantages are
  independent.
- **Settled findings** DR-01 and QA-01 are not reopened; no new evidence
  arose.

## Verdict

**PASS.** No MUST-FIX or HIGH finding. The merged range is the design,
without additions: one cross-cutting filter, one controller, one record, one
granted build line, one granted shipped-configuration line, the functional
profile overlay, and tests named after the acceptance criteria. Two LOW and
two INFO observations below; none changes the merged outcome, all are
routed as backlog or recorded as accepted.

## Structure (architecture guidance §2, §5)

| Check | Result |
|---|---|
| Package by feature; cross-cutting HTTP in `web/` | ✔ `dev.urlshort.web.RequestIdFilter`; `dev.urlshort.ping.{PingController,PingResponse}` |
| Controller holds no business rule; no HTTP types below it | ✔ the controller logs one event and builds the record; there is no layer below because there is no rule and no state (design §1) |
| Feature deletable by removing its package | ✔ `ping/` has no migration and no shared code; `web/` is cross-cutting by design |
| No `common/`, `util/`, `base/` | ✔ none |
| Request id: one filter, first in the chain, server-issued, header on every response, MDC | ✔ `@Order(HIGHEST_PRECEDENCE)`, header set *before* `chain.doFilter`, `MDC.put` / `finally MDC.remove`, inbound headers never read |
| Errors: platform handler, no project advice | ✔ no advice class; `405` comes from `ProblemDetailsExceptionHandler` via the shipped property |
| Logging: ECS, one object per line, no PII | ✔ `log.info("ping")` with MDC; shipped `exclude=process.thread.name` keeps the bind address out |
| Configuration: shipped file is the base in tests | ✔ profile overlay (`application-functional.properties` + `functional` profile from the Gradle task); verified in my own gate run, see Evidence |
| Dependencies | ✔ none added |

## Coherence with the locked design

| Design section | Merged range | Note |
|---|---|---|
| §1 Components | exactly the listed classes; constants private to the filter | — |
| §2 API contract | `200` `application/json` `{"status","time"}` + `X-Request-Id`; `405` `application/problem+json` with the header | proven by `PingJourneyTest` AC-1..AC-5 and by the live captures in `proof/` |
| §4 Sequence | header before chain, MDC cleared in `finally`, platform 405 path | matches `RequestIdFilter` line for line |
| §5 Logging event | `ping` INFO with `requestId` from MDC | matches; **accepted drift:** the shipped configuration additionally excludes `process.thread.name` (see Drift) |
| §6 Threat model | inbound id ignored; no stack traces; no IP/UA in logs | mitigations are present in code and asserted (AC-5, AC-7, AC-8) |
| §7 Test strategy | one functional test per AC with the suggested names; filter and controller unit tests incl. the throwing-chain case | matches; AC-7 carries one extra assertion (see W1-03) |
| §9 Territory | nine files, all inside `slice.yaml` at `2b29248`/`5befb22`; `build.gradle.kts` and `application.properties` each changed by exactly the granted line | matches |

## Drift from the SPEC and the doghouse

- **Doghouse delivered:** `GET /api/ping` answers `200` with `{"status":"ok","time":"<ISO-8601 UTC>"}` and a server-issued `X-Request-Id`, logged as one ECS JSON event, merged to `main` through requirements, design, design review, human plan-lock, TDD implementation, QA, code review, security review, serial integration and proof acceptance.
- **Scope held.** No extra endpoint, header, caching, tracing, OpenAPI assertion, persistence or metrics change. Every item in the SPEC's "explicitly out of scope" list stayed out.
- **One post-lock change to shipped configuration:** `logging.structured.json.exclude=process.thread.name`. Origin: QA-01 (Tomcat names worker threads after the bound address, so a loopback-bound server printed its own address on every event; AC-7 read literally). Decided by the orchestration lead on 2026-10-03T00:33Z as a SPEC-interpretation question within the lead's authority, with the human's plan-lock constraint (minimal filter) respected: a property, no code. It is global (every event), which is the right scope for a log-hygiene rule, and it is recorded in ADR-0004 and `docs/DESIGN.md` (my authored correction). The locked `design.md` §5 example still shows the member; it is historical and correctly left unedited. **Not a finding.**

## Findings

| ID | Severity | Where | Finding | Disposition | Route |
|---|---|---|---|---|---|
| W1-01 | LOW | `docs/api/openapi.json` (absent) | `architecture.md` §3 expects the OpenAPI document to be a committed artifact diffed in review. The SPEC explicitly excluded asserting the endpoint in the OpenAPI document, and the guidance library landed (00:24Z) after the SPEC was written (22:11Z). The service does serve `/v3/api-docs`; nothing is committed. | **CONTEXT-GAP** (guide postdates the SPEC; conflict resolves SPEC → ADR → guide, so the slice is compliant) | Orchestration lead backlog: decide who exports `docs/api/openapi.json` (release prep for this mission, or the first API slice of mission 01) |
| W1-02 | LOW | functional suite shape (`MockMvc` only) | The functional suite runs in-process at the servlet layer and cannot observe servlet-container metadata; QA-01 was visible only on a live Tomcat. The SPEC's proof contract required a live capture, which caught it, so the gate held. | **JUDGMENT-GAP** in the design (mine): `MockMvc` was chosen per `TESTING.md`; a thin real-server journey for log-hygiene criteria would have surfaced the thread-name member in the suite | Design backlog for the next slice that adds a log-hygiene AC: one `@SpringBootTest(webEnvironment = RANDOM_PORT)` journey class with `RestTestClient`, kept small; weigh against context-cache cost |
| W1-03 | INFO | `PingJourneyTest.AC7_…`, assertion on `/process/thread/name` | The AC-7 journey asserts an ECS envelope path, which the design's test hints said not to bind to. It is a deliberate regression guard instructed by the lead's QA-01 decision and watched red first. | No gap (decided). If the ECS layout changes, this assertion needs updating with the configuration | None |
| W1-04 | INFO | `src/test/resources/application.properties` | The unit suite's `application.properties` still shadows the shipped file (same mechanism as DR-01). Harmless today; nothing in the unit suite depends on shipped values. | Settled backlog (orchestration lead), no new evidence | None here |

## Evidence

What I ran and looked at, by effect:

- **Range inventory:** `git diff --stat f43ecd1..42a25db -- src build.gradle.kts` → 9 files, +305/−1; the full diff of all nine files read (new files, so the diff is the content). `git log f43ecd1..42a25db -- src build.gradle.kts` → `aa66007`, `8cb03f6`, `3886a04`, `f286a10`. Merge `42a25db` has parents `3e4f573` (main) and `f286a10` (branch tip), tag `slice/01-ping/accepted`.
- **Main unchanged since the merge in product terms:** `git diff --stat 42a25db..HEAD -- src build.gradle.kts` is empty at `HEAD` = `b54ac9a`, so the gate run below exercises the merged content.
- **Gate on `main`, my own run** (`proof/wave-w1-check-design-agent.txt`):
  `scripts/gw --log docs/review/00-hello/proof/wave-w1-check-design-agent.txt --offline check --rerun-tasks`
  → `BUILD SUCCESSFUL in 18s`, 13 actionable tasks, 13 executed (a first run
  without `--rerun-tasks` came back entirely up-to-date and is not cited).
  JUnit XML from this run: `UrlshortApplicationTests` 2, `RequestIdFilterTest` 3,
  `PingControllerTest` 1 (unit 6); `HealthJourneyTest` 1, `PingJourneyTest` 8
  (functional 9); 0 failures, 0 errors, 0 skipped. Merged JaCoCo CSV:
  `PingResponse` 1/1, `PingController` 4/4, `RequestIdFilter` 7/7,
  `UrlshortApplication` 3/3 lines; 0 branches missed of 0.
- **Profile overlay and exclusion by effect, from the same run:** the
  functional suite's console output in the committed gate log is ECS JSON
  with `"process":{"pid":…,"thread":{}}` (the two HikariCP shutdown events in
  `proof/wave-w1-check-design-agent.txt`), while the unit suite's lines in the
  same log are plain text. In the `PingJourneyTest` JUnit report
  (`build/test-results/functionalTest/TEST-dev.urlshort.ping.PingJourneyTest.xml`,
  system-out plus messages, not committed): 10 occurrences of `requestId`, 0
  of `process.thread.name`, `127.0.0.1` or `canary-`. The canaries travel in
  request headers, so their absence from the report says the log output and
  the test messages did not echo them; the authoritative canary evidence is
  the AC-7 and AC-8 journeys passing plus the live captures in `proof/`.
- **Read:** slice `PROOF.md` (builder, QA NOT-CLEAR, builder re-check, QA PASS,
  integrate, QA acceptance), `docs/qa/TRACEABILITY.md` (all 8 AC, 8 BR and 15
  methods mapped both ways), `docs/qa/GAPS.md` (no open merged-coverage or AC
  gap), `docs/review/REVIEW-LEDGER.md` (design_review FAIL→PASS, code_review
  PASS, security_review PASS, all on `f286a10`), both diagrams, ADR-0001..0004,
  `docs/DESIGN.md`, `docs/guidance/architecture.md`.
- **Not done:** no product, test or build file edited; no server started (the
  live captures in `proof/` are the builder's and QA's); packaged jar and
  dependency advisories not checked (release's job); the review agent's wave
  review not read.

## Recommended actions

1. Lead: add W1-01 (OpenAPI artifact ownership) and W1-02 (one real-server
   journey for log-hygiene criteria) to the backlog; neither blocks this wave.
2. Nothing to fix in the merged range before release preparation.

## Self-check

- Read every one of the nine changed files in full and compared each against
  the design section that specifies it; the table above names the match or the
  deviation for each. No deviation is undocumented.
- Ran the full gate myself on `main` with every task re-executed and cited the
  numbers from that run, not from memory or from another seat's log.
- Separated my authored corrections (DR-01 rework, ADR-0004 and `DESIGN.md`
  updates) from the structural verdict; stated that they are not independently
  approved here.
- Did not reopen DR-01 or QA-01; recorded the QA-01 configuration change as
  accepted drift with its decision record, not as a finding.
- Findings carry severity, location, disposition and route; the one that
  reflects on my own design choice (W1-02) says so.
- Committed only this document and its proof log, with the runtime trailer.
  The lifecycle packet is the review agent's to project.
