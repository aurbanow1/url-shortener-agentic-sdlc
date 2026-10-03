# RELEASE — 00-hello (Hello: factory dry run)

Prepared by `release-agent@urlshort-factory` (Claude Code), 2026-10-03 UTC, on
packet `qitem-20261003020412-57cea2d9` of lifecycle `01M3Z8AJ1EDFTHQ1HPAWPNP2YN`.
This instance runs on its original eight-step graph: `release_prep` hands
directly to the human `ship_signoff` gate (the `release_review` step exists only
in the authored profile; the operator's 22:45Z note keeps running 00-hello
instances on their original graphs). Nothing here was published, pushed or
tagged by the release agent.

## 1. Decision brief for ship sign-off

**What ships.** `main` at `d189019fed076e9bc60209d4f42efa1adb25e012`. Its
product, test and build inputs are byte-identical to merge
`42a25db4a9c24fba3221c1ade4044719cab39ee3` (tag `slice/01-ping/accepted`):
`git diff --stat 42a25db4 HEAD -- src build.gradle.kts settings.gradle.kts Dockerfile compose.yaml gradle gradlew`
is empty. One endpoint, `GET /api/ping`, answering `200` with
`{"status":"ok","time":"<ISO-8601 UTC>"}`, plus a cross-cutting request-id
filter that puts a server-issued `X-Request-Id` on every response and on the
request's JSON log event. Every non-2xx response is an RFC 9457 problem detail.

**Proof status.** `rig proof show 00-hello --json`: state `ready`, no issues;
all seven proof-contract items of `01-ping` accepted by `qa-agent@urlshort-factory`
against the merge subject `42a25db4` (receipts
[`slices/01-ping/proof/judgments/00000008.md`](slices/01-ping/proof/judgments/00000008.md)
to `00000014.md`; projection
[`../../docs/evidence/00-hello/proof-readiness.json`](../../docs/evidence/00-hello/proof-readiness.json)).

**Review verdicts** (all in [`../../docs/review/REVIEW-LEDGER.md`](../../docs/review/REVIEW-LEDGER.md)):
design review FAIL (DR-01) then PASS on `d0521de`; code review PASS and security
review PASS on `f286a10`; wave review PASS from both vantages on `42a25db4`.
No MUST-FIX, HIGH or MEDIUM finding is open. Two LOW follow-ups (W1-01, W1-02)
are backlog on `qitem-20261003020105-13e2c995`.

**Installed smoke.** `scripts/smoke.sh` (health, ping, wrong-method problem
detail, OpenAPI) passed against the packaged jar on `127.0.0.1:18091` and
against the container image on `127.0.0.1:18090`; see §3.

**Human decisions honoured.** Mission plan-lock 2026-10-02T21:59:33Z:
"approve: one slice, one wave; keep the request-id filter minimal". Slice
plan-lock 2026-10-02T23:27:04Z: "approve: lock SPEC 4e581cc + design d0521de;
functional profile overlay accepted". Neither locked artifact was edited after
its stamp; the one post-lock configuration change (`process.thread.name`
excluded from the JSON log) was the orchestration lead's recorded decision on
QA-01 and is reviewed in both wave reports.

**Known gaps** (complete list in §7). The one that needs the human's attention:
the network-backed advisory check (§4) finds ten OSV advisories on the runtime
classpath, in embedded Tomcat 11.0.24 (three CRITICAL, all in authenticator
code this service does not enable) and in both Jackson lines (five HIGH parser
denial-of-service, two MODERATE). The current surface accepts no request body
and configures no Tomcat authentication, and the dry run is never exposed
beyond localhost. No Spring Boot 4.1.x patch bundles the fixes yet. The
remediation is filed with the orchestration lead as
`qitem-20261003021640-bc2477ef`.

**Rollback.** `git revert -m 1 42a25db4` on `main` removes the endpoint, the
filter, their tests and the two granted configuration lines; there is no
Flyway migration to reverse. Step by step in §8.

**Recommended default.** Approve. The mission's value is the proven pipeline,
not the ping; the artifact is verified end to end on the exact SHA; the
advisories are recorded, not hidden, and are routed to be fixed before any
slice that parses client input. Alternative: hold until the dependency
overrides land on `main`, which adds one slice and one more pass through QA,
review and release prep before this sign-off.

## 2. Artifact and gate

| Item | Value |
|---|---|
| `main` at build time | `d189019fed076e9bc60209d4f42efa1adb25e012`. Commits landing on `main` after the build are documentation, scripts and tools only (this record among them); the product claim is checked with `git diff --stat 42a25db4 main -- src build.gradle.kts settings.gradle.kts Dockerfile compose.yaml gradle gradlew`, which must stay empty |
| Product inputs | identical to `42a25db4a9c24fba3221c1ade4044719cab39ee3` (merge of candidate `f286a10863e4a8081235226f2d56e51ac121b319`, tag `slice/01-ping/accepted`) |
| Gate run | `scripts/gw --log release/check-bootjar-d189019.txt check bootJar --rerun-tasks`: BUILD SUCCESSFUL in 21 s, 15 of 15 tasks executed, log [`release/check-bootjar-d189019.txt`](release/check-bootjar-d189019.txt) |
| Tests from that run | unit 6 (`UrlshortApplicationTests` 2, `RequestIdFilterTest` 3, `PingControllerTest` 1), functional 9 (`HealthJourneyTest` 1, `PingJourneyTest` 8); 0 failures, 0 errors, 0 skipped |
| Coverage from that run | merged 15 of 15 lines (`PingResponse` 1, `PingController` 4, `RequestIdFilter` 7, `UrlshortApplication` 3); 0 of 0 branches; `jacocoTestCoverageVerification` passed with no exclusions |
| Jar | `build/libs/urlshort.jar`, version 0.1.0, 37 070 236 bytes, git blob `e9ba48ae7fe09bf06b119f3cd6e00debab10bb4b` |
| Image | `urlshort:local` built by `docker compose build` from the same working tree (log [`release/docker-build-d189019.txt`](release/docker-build-d189019.txt)); image id `db744838ce54` (`sha256:db744838ce540a72e44d6199eedeb95b37dd8ec3a6683ef253f69b63c3f95eca`, 557 MB, Temurin 21 JRE base). The container's jar is built inside the image from the same sources and may differ byte for byte from the host jar (not compared); the same-SHA claim rests on the clean working tree above |
| Toolchain | OpenJDK 21.0.10 (Homebrew), Gradle 9.7.1, Spring Boot 4.1.1, JaCoCo 0.8.15, Docker 28.4.0 with Compose 5.1.3, OpenRig 0.6.3 (8b5e9488) |

## 3. Installed smoke

`scripts/smoke.sh` was extended for this release: health, `GET /api/ping`
(200, `application/json`, `status` ok, `time` present, `X-Request-Id`
present), `POST /api/ping` (405, `application/problem+json`, `X-Request-Id`
present, no exception text in the body), OpenAPI document. All its HTTP goes
through `scripts/http`, so it can only ever target loopback. Sections for
create, redirect and stats arrive with the slices that ship them.

### 3.1 Jar

Started as a user would, with the shipped configuration (file-mode H2 under
`./data`, Flyway enabled, ECS console logging), bound to loopback and with a
JSON log file added for evidence:

```sh
java -jar build/libs/urlshort.jar --server.address=127.0.0.1 --server.port=18091 \
  --logging.structured.format.file=ecs --logging.file.name=missions/00-hello/release/jar-smoke-log-d189019.jsonl
scripts/smoke.sh http://127.0.0.1:18091
```

- Result: `SMOKE OK against http://127.0.0.1:18091`
  ([`release/smoke-jar-d189019.txt`](release/smoke-jar-d189019.txt)).
- Log ([`release/jar-smoke-log-d189019.jsonl`](release/jar-smoke-log-d189019.jsonl),
  34 events): `Database: jdbc:h2:file:./data/urlshort (H2 2.4)`,
  `Tomcat started on port 18091 (http)`, `Started UrlshortApplication in 1.547 seconds`,
  one `ping` event with `requestId 28ca3b10-cd23-45e7-a8a3-f65e9c2ae4f7` and
  `"process":{"pid":55575,"thread":{}}`; `Commencing graceful shutdown` and
  `Graceful shutdown complete` after `kill -TERM`. The strings `127.0.0.1`,
  `process.thread.name` and `http-nio` occur nowhere in the file, so the
  packaged jar keeps the QA-01 exclusion.
- Observation, not a failure: Flyway logs
  `Using H2 2.4.240 which is newer than the version Flyway has been verified with (2.3.232)`.
  Recorded in §7.

### 3.2 Container

`compose.yaml` publishes `8080:8080` on every interface, which an agent may not
do, so the smoke ran the built image with `docker run` bound to loopback:

```sh
docker compose build
docker run -d --name urlshort-smoke -p 127.0.0.1:18090:8080 urlshort:local
scripts/smoke.sh http://127.0.0.1:18090
docker logs urlshort-smoke   # captured, then docker stop / docker rm
```

- Image `db744838ce54`, container `urlshort-smoke`, published only on the host's
  `127.0.0.1:18090`. The build's in-container Gradle download took about twelve
  minutes; the build itself succeeded on the first attempt.
- Result: `SMOKE OK against http://127.0.0.1:18090`
  ([`release/smoke-container-d189019.txt`](release/smoke-container-d189019.txt)).
  The first health probe received an empty reply from Docker's port proxy
  before Tomcat was listening; the probe was retried on all errors and the
  smoke ran once health reported `UP`.
- Log ([`release/container-smoke-log-d189019.txt`](release/container-smoke-log-d189019.txt),
  captured before shutdown): `Database: jdbc:h2:file:/app/data/urlshort (H2 2.4)`,
  `Tomcat started on port 8080 (http)`, `Started UrlshortApplication in 1.731 seconds`;
  one `ping` event with `requestId e31997cf-b9f5-43c1-925f-65cf10c04b4c` and
  `"process":{"pid":1,"thread":{}}`. The strings `127.0.0.1`,
  `process.thread.name` and `http-nio` occur nowhere in the file. The container
  was stopped with `docker stop` (SIGTERM to pid 1) and removed; its H2 file
  lived in the container's writable layer, so nothing remains on the host.

### 3.3 Not exercised by the smoke

`HEAD` and `OPTIONS`; the privacy canaries and the inbound-id canary (QA's and
the security review's live captures hold those on the same code); the AC-2
time interval and the AC-4 uniqueness pair (functional tests); load; the
`docker compose up` path itself and its healthcheck (the image was run with
`docker run`); Flyway migrations (none exist yet); persistence (nothing is
stored).

## 4. Dependency advisories (network check)

The sandboxed security review could resolve the runtime classpath but not
query an advisory database. Done here with network:
`scripts/gw --log release/runtime-dependencies-d189019.txt dependencies --configuration runtimeClasspath`
([tree](release/runtime-dependencies-d189019.txt)), then
`node tools/dep-advisories.mjs` posting the 90 resolved coordinates to the OSV
`querybatch` API and fetching each hit's details. Raw response and details:
[`release/osv-advisories-d189019.json`](release/osv-advisories-d189019.json),
queried 2026-10-03T02:14:07Z.

| Advisory | Affects (resolved) | Severity | Fixed in | What it is |
|---|---|---|---|---|
| GHSA-9xv2-5v5q-p794 | `tomcat-embed-core` 11.0.24 | CRITICAL | 11.0.25 | DIGEST authenticator: authentication bypass by capture-replay |
| GHSA-gcx9-497g-6cp6 | `tomcat-embed-core` 11.0.24 | CRITICAL | 11.0.25 | improper access control / incorrect authorization |
| GHSA-h3x4-894j-xpx5 | `tomcat-embed-core` 11.0.24 | CRITICAL | 11.0.25 | FORM authentication: incorrect authorization |
| GHSA-cxp5-3px4-pw24 | `jackson-databind` 3.1.5 and 2.21.5 | HIGH | 3.1.7 / 2.21.7 | quadratic forward-reference completion |
| GHSA-wv8q-qhhj-9h54 | `jackson-databind` 3.1.5 and 2.21.5 | HIGH | 3.1.7 / 2.21.7 | retains every unknown raw type id |
| GHSA-q4xh-88c3-wmh7 | `jackson-databind` 3.1.5 and 2.21.5 | HIGH | 3.1.6 / 2.21.6 | Duration and XMLGregorianCalendar unbounded number parse (DoS) |
| GHSA-7hhh-6rmp-j9qf | `jackson-core` 3.1.5 and 2.21.5 | HIGH | 3.1.7 / 2.21.7 | invalid-token reporting without length limit (DoS) |
| GHSA-p6pp-m3f8-5c89 | `jackson-core` 3.1.5 and 2.21.5 | HIGH | 3.1.7 / 2.21.7 | ReDoS in float pattern matching |
| GHSA-gx83-3vf8-gh7j | `jackson-databind` 3.1.5 and 2.21.5 | MODERATE | 3.1.6 / 2.21.6 | incomplete polymorphic type validator denylist |
| GHSA-wjgm-6hv5-3cvf | `jackson-databind` 3.1.5 and 2.21.5 | MODERATE | 3.1.6 / 2.21.6 | Path deserialization without scheme allowlist |

Where they come from: Tomcat and Jackson 3 are managed by Spring Boot 4.1.1
(`spring-boot-starter-webmvc`); the Jackson 2 line arrives through
`springdoc-openapi-starter-webmvc-ui` 3.1.1 → `swagger-core-jakarta` 2.2.55.
No dependency was added by the slice. Spring Boot 4.1.1 is the newest 4.1.x
release on Maven Central as of the query, so there is no patch upgrade that
closes these; the fix is version overrides in `build.gradle.kts` followed by
the full gate and a fresh OSV run.

Reachability on this release: the service enables no Tomcat authentication
(no DIGEST or FORM realm, no security dependency), so the three CRITICAL paths
are not reachable; the service deserializes no client input (ping has no body,
no endpoint takes JSON), so the parser advisories are reachable only through
server-side serialization of the OpenAPI and actuator documents. This is a
reachability argument, not a claim that the stack is free of known
vulnerabilities. Follow-up filed: `qitem-20261003021640-bc2477ef` to the
orchestration lead, recommending a dependency-upgrade item first in mission 01,
before any slice that accepts JSON request bodies.

## 5. Evidence per slice

### 01-ping (tier high, wave w1)

| Artifact | Path |
|---|---|
| Requirements (locked `4e581cc`) | [`slices/01-ping/SPEC.md`](slices/01-ping/SPEC.md) |
| Design (locked `d0521de`), ADRs | [`slices/01-ping/design.md`](slices/01-ping/design.md), [`../../docs/adr/`](../../docs/adr/) 0001 to 0004, [`../../docs/DESIGN.md`](../../docs/DESIGN.md) |
| Design review (FAIL DR-01, then PASS) | [`../../docs/review/01-ping/design-review.md`](../../docs/review/01-ping/design-review.md) |
| Code review (PASS, `f286a10`) | [`../../docs/review/01-ping/01-code-review.md`](../../docs/review/01-ping/01-code-review.md) |
| Security review (PASS, `f286a10`) | [`../../docs/review/01-ping/02-security-review.md`](../../docs/review/01-ping/02-security-review.md) |
| QA coverage (unit, functional, merged) | [`../../docs/qa/coverage/01-ping/SUMMARY.md`](../../docs/qa/coverage/01-ping/SUMMARY.md) |
| Traceability (8 AC, 8 BR, 15 tests) | [`../../docs/qa/TRACEABILITY.md`](../../docs/qa/TRACEABILITY.md) |
| Gaps | [`../../docs/qa/GAPS.md`](../../docs/qa/GAPS.md) |
| QA findings (QA-01, closed) | [`../../docs/qa/01-ping/findings.md`](../../docs/qa/01-ping/findings.md) |
| Builder, QA, integrate and acceptance record | [`slices/01-ping/PROOF.md`](slices/01-ping/PROOF.md), captures in [`slices/01-ping/proof/`](slices/01-ping/proof/) |
| Progress | [`slices/01-ping/PROGRESS.md`](slices/01-ping/PROGRESS.md) |
| Proof readiness (7 of 7 accepted) | [`../../docs/evidence/00-hello/proof-readiness.json`](../../docs/evidence/00-hello/proof-readiness.json) |

### Mission level

| Artifact | Path |
|---|---|
| Mission intent and plan-lock brief | [`SPEC.md`](SPEC.md) |
| Wave w1 review, review-agent vantage | [`../../docs/review/00-hello/wave-w1-review-review-agent.md`](../../docs/review/00-hello/wave-w1-review-review-agent.md) |
| Wave w1 review, design-agent vantage | [`../../docs/review/00-hello/wave-w1-review-design-agent.md`](../../docs/review/00-hello/wave-w1-review-design-agent.md) |
| Wave follow-ups W1-01, W1-02 | [`../../docs/review/00-hello/wave-w1-followups.md`](../../docs/review/00-hello/wave-w1-followups.md) |
| Evidence export (2 instances, 20 packets) | [`../../docs/evidence/00-hello/`](../../docs/evidence/00-hello/) |
| Reliability metrics | [`../../docs/metrics/README.md`](../../docs/metrics/README.md), [`../../docs/metrics/metrics.json`](../../docs/metrics/metrics.json) |
| Natural failures observed | [`../../docs/scenarios/drills.md`](../../docs/scenarios/drills.md) |

## 6. Metrics, read plainly

From `node tools/sdlc-metrics.mjs` over the export above (definitions and
limits in `docs/metrics/README.md`):

- Two workflow instances: the slice instance completed; the lifecycle is still
  active (this step), so the instance success rate of 1 rests on one terminal
  instance.
- 31 step closures, 2 failed (design review DR-01, QA check QA-01): step
  success rate 0.935. Retries 6, all on the slice: the two failed verdicts plus
  the re-entries they caused (design, design review, implement, QA check each
  ran twice). The lifecycle's 13 `waiting` closures are the integrator
  re-presenting its wait on the slice and are not counted as retries.
- Rollbacks 0. There was no revert on `main` and no engine resume; the number
  is genuinely zero, not undefined.
- MTTR 21 min, the mean of two repairs (DR-01 about 13 min, QA-01 about 29 min).
- End-to-end latency of the completed slice instance: 3.8 h from instantiation
  to `done`, of which 14.5 min was the human slice plan-lock.
- Time parked on the human: mission plan-lock 21 min 16 s
  (21:38:17Z to 21:59:33Z), slice plan-lock 14 min 28 s (23:12:36Z to
  23:27:04Z), read from the gate packets' transitions; the tool's total is
  36 min. Two derivation fixes were needed before these numbers could be
  cited and are visible in the `tools/sdlc-metrics.mjs` history: `waiting`
  re-presentations are no longer counted as retries (the lifecycle had shown
  13), and only the engine's own gate park counts as human wait (the
  integrator's wait note mentioning the slice gate had double-counted it).

## 7. Known gaps, complete

Rows from `docs/qa/GAPS.md`, copied verbatim:

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 01-ping | Functional-only coverage 13/15 lines (86.67%); merged 15/15 (100%) | Functional suite does not invoke the two lines of UrlshortApplication.main; per-suite coverage is informational | Unit mainBootsWithoutAWebServer covers them; all three CSVs copied into coverage/01-ping | No merged coverage gap or exclusion; no waiver required |
| 01-ping @3886a04 | AC-7 live log contains 127.0.0.1 in Tomcat thread metadata | Required localhost bind adds the server address to the thread name; MockMvc with a distinct client address does not exercise this metadata | Real GET/canary captures and installed Tomcat bytecode; see 01-ping/findings.md QA-01 | Resolved on f286a10 by exclusion of process.thread.name; independently rechecked on the same bind; no waiver |
| 01-ping @f286a10 | None for the merged coverage gate or acceptance criteria | All 15 tests pass, merged lines 15/15, no branches; functional-only 13/15 remains informational | All AC-1–AC-8 observed live; startup-through-shutdown log has no canaries or loopback addresses; captures carry f286a10 suffix | No open verification gap; packaged artifact belongs to release checks |

Further known gaps and limits:

- **Dependency advisories** (§4): ten OSV advisories on the runtime classpath;
  not reachable on this surface; follow-up `qitem-20261003021640-bc2477ef`.
- **W1-01 (LOW, CONTEXT-GAP)**: no committed OpenAPI export (`docs/api/openapi.json`)
  although the later architecture guide expects one; the locked SPEC excluded
  it. Backlog for the first mission-01 API slice.
- **W1-02 (LOW, JUDGMENT-GAP)**: the functional suite is `MockMvc` only and
  cannot observe servlet-container metadata; QA-01 was caught by the required
  live capture. Backlog: one small real-server journey with the next
  log-privacy criterion.
- **Unit-suite properties shadow the shipped file** (`src/test/resources/application.properties`,
  the DR-01 mechanism); harmless today, lead's backlog.
- **`compose.yaml` publishes port 8080 on all interfaces.** Normal for a
  service, but it means `docker compose up` is not a loopback-only path; the
  release smoke used `docker run -p 127.0.0.1:…` instead, so the compose
  healthcheck was not exercised.
- **Flyway/H2 version notice**: Flyway 12.4.0 has verified H2 up to 2.3.232;
  the shipped H2 is 2.4.240. No migration exists yet, so nothing has been
  tested against this combination beyond Flyway creating its history table.
- **Zero branches**: the branch threshold passes vacuously; the production
  code has no conditional.
- **Operational limits** (owner: release agent, to be addressed in the
  brownfield mission or recorded as accepted): single node, no replica; H2
  file database under `./data` or the `urlshort-data` volume with no backup or
  restore procedure; no `docs/RUNBOOK.md` yet; restart durability,
  concurrency and load were not exercised by the smoke.
- **Dogfood pass not requested.** The mission SPEC states the product value
  is the proven pipeline, not the ping, and the endpoint has no user-facing
  journey to explore; a dogfood row would have been ceremony. Dogfood starts
  with the first user-facing mission-01 slice.
- **Compiled graph files**: `docs/evidence/00-hello/compiled-graph.json` is the
  graph this instance runs on (bound version `1-4322186fed9c15a8`, eight steps);
  `compiled-graph.authored-1-32dce218.json` is what `project.yaml` on disk
  compiles to today (ten steps, with `decomposition_review` and
  `release_review`). `rig workflow revise` reports the change as incompatible
  with the running instance, so it was deliberately not adopted; the
  reconciliation block in `instances/01M3Z8AJ1EDFTHQ1HPAWPNP2YN.show.json`
  records both digests.

## 8. Rollback

Described step by step and rehearsed once on a throwaway branch (drill row in
[`../../docs/scenarios/drills.md`](../../docs/scenarios/drills.md)): the
revert of `42a25db4` applied cleanly as `a9f3d2f` on `rollback-rehearsal`
(9 files, +1/−305) and the gate on the reverted tree was green
([`release/rollback-rehearsal-check.txt`](release/rollback-rehearsal-check.txt),
13 of 13 tasks, 3 tests, coverage verification passed). The branch and its
worktree were removed; `main` was not touched, because `main` has never been
red after the merge and a revert on `main` is the integrator's act.

**Verification after a real rollback:** `scripts/gw check` green on the
reverted `main`, then `scripts/smoke.sh` against the rebuilt jar, expecting the
ping section to fail and the health and OpenAPI sections to pass.
**Data lost:** none. No user data exists; the only persistent state is
Flyway's empty history table in the H2 file.

1. **Source.** On `main`: `git revert -m 1 42a25db4a9c24fba3221c1ade4044719cab39ee3`,
   then `scripts/gw check`. The revert removes `ping/` and `web/` in all three
   source sets, restores `src/functionalTest/resources/application.properties`
   from `application-functional.properties`, and drops the two granted lines
   (`spring.profiles.active=functional` in `build.gradle.kts`,
   `logging.structured.json.exclude=process.thread.name` in the shipped
   properties). The tag `slice/01-ping/accepted` stays as history.
2. **Database.** No Flyway migration was added, so there is no schema to
   reverse. The file database created by a run (`data/urlshort.mv.db`, or the
   `urlshort-data` volume for the container) holds only Flyway's empty history
   table and can be kept or deleted.
3. **Artifacts.** The jar is a build output (`build/libs/`, gitignored); the
   image is local only (`urlshort:local`), nothing was pushed to a registry.
   `docker compose down` stops the service; add `-v` to drop the volume.
   Rebuilding from the reverted commit produces the previous artifact.
4. **Factory.** The lifecycle instance needs no rollback; if the human declines
   sign-off, the gate packet stays parked and this step waits on what the human
   asks for.

## 9. Ship decision

Approved by `human@kernel` on gate `qitem-20261003023502-f807af1f` at
2026-10-03T02:44:55Z: "approve: ship the 00-hello dry run on 42a25db4;
Tomcat/Jackson advisories tracked in qitem-20261003021640-bc2477ef and fixed
before the first slice that parses client input". The 01-ping delivery stamp
was recorded by the orchestration lead on the human's behalf at
2026-10-03T02:45:45Z (action `01M3ZTEF1C876Q09Q5ND34N8KZ`, `approved-by` /
`approved-at` in `slices/01-ping/SPEC.md`, commit `9cd64f8`). The decision's
condition binds mission 01 planning. Publishing remains a human act; no agent
pushed, tagged or published anything for this release.

## Self-check

One line per section, written at `release_prep` and re-read at `evidence_export`.

- §1 Brief: the human's two prior decisions are quoted from the gate transition
  logs; the advisory item is the one judgment call and is stated with its
  reachability and its alternative's cost; nothing was published.
- §2 Artifact and gate: the jar is from the fresh `--rerun-tasks` gate on
  `main` at `d189019`; the image from the same clean tree; `git diff --stat
  42a25db4 HEAD` over product, test, build and container inputs is empty, and
  every path cited was listed with `ls` before the commit.
- §3 Smoke: ran on both artifacts bound to loopback, logs captured, process
  and container stopped and removed; what it did not exercise is listed.
- §4 Advisories: every OSV hit is in the table with severity, fixed-in and
  reachability; the remediation is a routed queue item with a deadline rule.
- §5 Evidence: every linked path exists and names the SHA it is about.
- §6 Metrics: derived from the engine's records by the committed tool; the two
  counting corrections are commits with their reason, not tuning.
- §7 Gaps: the three `docs/qa/GAPS.md` rows are copied verbatim; review LOWs,
  backlog, advisories and operational limits are listed with an owner.
- §8 Rollback: exact commands, rehearsed once on a throwaway branch with the
  gate green on the reverted tree (drill row linked), data loss stated.
- §9 Ship decision: quoted from the gate transition log with the stamp's
  action id and commit.
