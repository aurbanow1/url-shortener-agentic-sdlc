# PROOF — OPR.99.0.2.3 Operate safely

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.2.3 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Current status: review FAIL at 43cccf5; returned to implement. Historical QA PASS on a7c533f is superseded; smoke proof item 12 withdrawn in receipt 00000013 (2026-10-03 UTC).

## What this proves

The candidate limits client requests with a correlated, private 429; health follows the database; metrics and the API document expose the promised contract. Independent QA observed all AC-1–AC-20, with a fresh 100% merged coverage gate. Container and release workload judgments remain pending under the locked SPEC.

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- Builder drop: `proof/builder-evidence-a7c533f.md`.
- QA drop: `proof/qa-evidence-a7c533f.md`; coverage, captures and limits are detailed below.

## Residue / caveats (if any)

Proof item 11 awaits code/security review and item 13 awaits release. `docs/qa/03-operate/proof-sequencing.md` and `docs/qa/GAPS.md` retain those obligations. QA bench offered rate is below the specified workload; its latency numbers are not a release verdict.

## Builder

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet
`qitem-20261003094226-6580fb8d`. Builder evidence, not a verdict.

**Candidate:** `a7c533ffef55650e5b422377ffe0c4e38d41400c` on `slice/03-operate` (`.worktrees/03-operate`).
It descends from `02-analytics`' merge: `git merge-base --is-ancestor 091ff46 a7c533f` succeeds (proof contract item 9).
The branch was built on base `6d7f6bb` and rebased onto `main` (`f046bbd`) once 02 had merged. The rebase was clean.
Pre-rebase → rebased SHAs: `a6781a9`→`a0abe1c`, `6282267`→`6f12a9d`, `387a15e`→`5f97db6`, `8581a78`→`2f3af5e`.
Each commit passed `scripts/gw --offline check` on its own before the rebase, and the rebased branch passed the gate again before step 5.

| SHA | Commit |
|---|---|
| `a0abe1c` | `chore`: Micrometer Prometheus registry only (`build.gradle.kts`, version from the Boot BOM) |
| `6f12a9d` | `feat`: rate limit — `RateLimiter` (GCRA per client and budget), `RateLimitFilter`, `RateLimitProperties`/`RateLimitConfig`, the rejection counter, body-limit order `+3`, functional overlay budgets, `FunctionalClock.freeze()`; unit and limiter journeys; `RateLimitDefaultsTest` |
| `5f97db6` | `feat`: readiness with `db`, `show-details=never`, Prometheus exposure, `Http11Processor`/`ResourceHandlerUtils` levels, 10 s phase; `HealthMetricsJourneyTest`, `DatabaseDownJourneyTest`, `ShutdownPhaseDefaultTest`, AC-16/17/19 and the DR-01 real-server canary |
| `2f3af5e` | `feat`: `compose.yaml` (loopback publish, read-only root, tmpfs `/tmp`, named volume, 20 s stop grace, readiness health check), Dockerfile note, `scripts/smoke.sh` default + `--restart`/`--drain`/`--bench` |
| `0b1f0d8` | `feat`: the `429` `OpenApiCustomizer` on every operation, `OpenApiDocumentTest` AC-20, regenerated `docs/api/openapi.json` |
| `a7c533f` | `fix`: the `429` body is written as bytes, not through `getWriter()`; a real-server test of its `Content-Type` |

### Commands and outcomes

| Command | Outcome | Record |
|---|---|---|
| `scripts/gw --offline check` on `a6781a9` (registry only), then `dependencies` + `node tools/dep-advisories.mjs` | green; 97 runtime coordinates, **0 OSV advisories** | `proof/osv-advisories-a6781a9.json`, `proof/runtime-dependencies-a6781a9.txt` |
| the same OSV run on the candidate `a7c533f` (its `build.gradle.kts` equals `a0abe1c`'s) | 97 coordinates, **0 advisories** | `proof/osv-advisories-0b1f0d8.json`, `proof/runtime-dependencies-0b1f0d8.txt` (the dependency tree does not change after `a0abe1c`) |
| `scripts/gw functionalTest` before the limiter (settings record only) | 105 run, **15 failed**: no `429` yet, and no shipped budgets | `proof/builder-red-functional-before-limiter.txt` |
| `scripts/gw functionalTest` before step 3's configuration | 114 run, **4 failed**: readiness ignored the database, the DR-01 canary was in the log, Prometheus was not exposed, the phase was 20 s | `proof/builder-red-functional-before-operate-config.txt` |
| `OpenApiDocumentTest` before the customiser | AC-20 failed: no `429` on any operation | build log, not kept |
| `RateLimitJourneyTest` before the bytes fix | the new real-server test failed on `Content-Type` | `proof/builder-red-429-charset-on-tomcat.txt` |
| `scripts/gw --offline check --rerun-tasks` on `a7c533f`, last after the last code edit | BUILD SUCCESSFUL; **unit 163/163, functional 155/155**, 0 skipped; merged JaCoCo **100 % line and branch**; `javadoc` green | `proof/builder-check-a7c533f.txt` |
| `docker compose config --quiet` | valid; nothing was started (Docker runs are `release_prep`'s, lead 10:20Z) | — |

### Verified by effect on the candidate jar (`java -jar`, real Tomcat, file H2)

- **Admitted create and 429.** The jar ran with the operator setting at 2 creates and 3 redirects per minute (AC-10's mechanism), so exhaustion is observable in a few requests. Captured: `201` (`http-201-create-a7c533f.txt`), then three `429`s: a create, a read in the same budget, and a redirect after three `302`s. Each `429` has `Retry-After` (24, 22 and 18 s), an `X-Request-Id`, `Content-Type: application/problem+json` and the body `{"instance":"urn:uuid:<id>","status":429,"title":"Too Many Requests"}`. None of them echoes the `User-Agent` marker, the `X-Forwarded-For` marker or the url marker (AC-1, AC-11).
- **Health.** Liveness and readiness are `200 {"status":"UP"}` (AC-13).
- **Prometheus.** The rejection counter reads `{budget="create"} 2.0` and `{budget="redirect"} 1.0`. The three redirects are counted under the route template, the pool gauges are present, and the `429`s appear as `uri="UNKNOWN"`. No code, marker, address or URL appears in the scrape (`prometheus-excerpt-a7c533f.txt`, AC-16 to AC-19).
- **Logs.** Exactly one `request completed` event per `429`, carrying its `requestId` and status 429. The whole run log has 0 matches for the markers, the loopback address and `curl/` (`log-lines-429-a7c533f.txt`, AC-12).
- **Smoke modes on the candidate:**
  - default smoke: OK;
  - `--drain`: R0 `201`, probe refused, 60 ok, 20 refused, 0 boundary losses, 0 failures (`smoke-drain-a7c533f.txt`);
  - `--bench`: 0 errors; redirects 82 req/s, p95 2.3 ms; creates 16 req/s, p95 2.7 ms (`smoke-bench-a7c533f.txt`). This shows the mode works. It is not the release verdict.

### Deviations from design.md, each with its reason

1. **Withdrawn in `be1258d` (code review CR-01); see *Rework — 1c8b2cf* below.** Original text: **`RateLimiter` treats a TAT more than one full bucket ahead of now as a fresh client.** The design does not say this. A legitimate TAT is never more than `T + I` ahead. Only a backward step of the wall clock can produce one, and without the guard a step back of an hour refuses every recent client for an hour. It was found because `IdempotencyJourneyTest` shifts the shared suite clock 24 h forward and back, which locked `127.0.0.1` out of the base context. One condition; `RateLimiterTest.aBackwardClockStepStartsTheClientFreshInsteadOfLockingItOut`.
2. **The 429 body is written to the output stream as bytes.** The design shows a write without naming the API. Through `getWriter()` Tomcat appended `;charset=ISO-8859-1` to the `Content-Type`, unlike every other problem response. MockMvc hid this; the first live capture caught it. Fixed in `a7c533f`, with a real-server test.
3. **The release gate uses `getAndUpdate` instead of a CAS.** Same semantics: at most once per second of application-clock time. It has no CAS-failure branch, which is reachable only under contention and would be uncoverable.
4. **AC-11 and AC-12 share the SPEC-fixed address `10.77.77.77`**, so they exhaust it until the first `429` rather than counting from a fresh bucket. This keeps them independent of run order. Every other test uses its own address.
5. **`@AutoConfigureMetrics` on `RateLimitJourneyTest`.** Boot's test support disables export to non-simple registries, so without it `/actuator/prometheus` does not exist in tests. That class already had its own context, so no extra context starts.
6. **`ShutdownPhaseDefaultTest` added.** It pins the shipped `10s` phase and `graceful` mode, the only in-suite check of a number the release checks rely on.
7. **`scripts/smoke.sh --drain [port]` picks the JDK the way `scripts/gw` does** (`URLSHORT_JAVA_HOME`, then the pinned JDK 21, then `java`), because `java` on PATH here is 11. The load loops record every curl exit code; an earlier version lost refusals under `set -e`, and the fix came before the recorded runs.
8. **`--bench` paces with a fixed 100 ms sleep per loop** (bash 3.2 has no sub-second clock). The achieved rate is printed and falls short of 100 and 20 req/s, which the output says.

### Not verified, and residual risks

- **Release-level AC-21 to AC-28 were not run against the container.** This is by the SPEC and the lead's instruction: `docker compose config` only. The `--restart` mode is written but untested here. `--drain` and `--bench` ran against the jar.
- **The Prometheus scrape includes Boot's disk-space gauges with a `path` tag holding the service's working directory.** It is an installation path on an anonymous endpoint, published on loopback only. It is not one of AC-19's values. For review: `management.metrics.enable.disk=false` would drop it if wanted.
- **A-9:** the click hash still uses `getRemoteAddr()`; this slice does not rewrite it. It stays on the lead's backlog.
- **CR-01 of slice 01** (the `errors[]` member in the ProblemDetail schema) is not addressed. The 429 references the same schema.
- **AC-25 boundary losses:** 0 were seen at about 40 req/s. The design probe measured 2–6 per stop under a closed loop of thousands per second, which is reported, not judged.
- **Not done by me:** QA's coverage copies, traceability, the `GAPS.md` rows for AC-21 to AC-28, the live-vs-committed API-document diff, and the review record on limiter memory and privacy.

## Self-check

- **Re-read the whole diff** against `main`. Territory holds: `web/` in all three source sets, `application.properties`, `build.gradle.kts` (one line), `Dockerfile`, `compose.yaml`, `scripts/smoke.sh`, `docs/api/openapi.json`, and the two granted test files (`application-functional.properties` budgets, `FunctionalClock.freeze()` with `reset()` unfreezing). Every test that freezes resets in `@AfterEach`. Nothing under `link/`, `click/` or `db/migration/`.
- **No client value in limiter output.** The 429 body is built from server values only; the filter logs nothing; the counter's only tag is `budget`. Checked by test (AC-11, AC-12, AC-17, AC-19, the DR-01 Tomcat canary) and by effect (the grep counts above).
- **The limit is checked first.** The filter runs at `+2`, before the body cap at `+3`, so `400`/`413`/`415` outcomes are charged (AC-9). Classification uses the decoded lookup path (the `/%61pi/links` test).
- **Memory is bounded as designed, opportunistically.** `RateLimiterTest` covers release on the next request, retention while idle, survival of a bucket that is not full, and at most one release per second.
- **Ladder applied.** No new library beyond the BOM's Prometheus registry; no timer thread, no interceptor, no `RemoteIpValve`. One map per budget, one `long` per client.
- **Every in-suite AC has a named test:** AC01–AC12 and AC16–AC19 in `RateLimitJourneyTest`; AC08 in `TrustedProxyJourneyTest`; AC10 in `RateLimitSettingsJourneyTest`; AC13, AC15 and AC18 in `HealthMetricsJourneyTest`; AC14 and AC15 in `DatabaseDownJourneyTest`; AC20 in `OpenApiDocumentTest`. Watched failing first: the red runs above. The limiter and filter unit tests were red only as compile failures. AC-13, AC-16, AC-17 and AC-18 already passed before step 3's configuration, because health and metrics were already exposed. The step-3 red was in readiness, Prometheus, the DR-01 log and the phase.
- **Gate:** `--offline check --rerun-tasks` ran after the last code edit, on `a7c533f`.

### Rework — 1c8b2cf (code review CR-01, CR-02, CR-03, SEC-01)

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003123104-ccd07ab0`.
**Candidate:** `1c8b2cff20ad8b73a060bc817c8d0011782f876f` on `slice/03-operate`, four commits on `a7c533f`. An earlier tip, `b3370ef`, was recorded and never handed off. Its `-b3370ef` proof files remain as history, and `1c8b2cf` refines its CR-03 threshold.

| SHA | Commit |
|---|---|
| `be1258d` | `fix`: `RateLimiter` reads the clock inside the per-client `compute` and uses the locked rule `start = max(tat, now)` again, without the TAT reset. The once-per-second release also runs when its deadline is more than 1 s ahead. |
| `aa43ff7` | `test`: requests sent at a shifted clock in `IdempotencyJourneyTest` AC19 and `StatsJourneyTest` AC09 come from their own peer addresses, `10.0.30.1` and `10.0.31.1`. No assertion changed. This uses the lead's test-side grant in `slice.yaml` (`61a5679`, `qitem-20261003123330-f28e75db`). |
| `b3370ef` | `fix`: `scripts/smoke.sh` R0 is now a curl upload whose chunked body arrives in two parts through a FIFO. It passes only on curl exit 0 with a 2xx/3xx no later than 10 s after the stop, in both `--drain` and `--restart`. The ids of R0 and of the readiness probes count as delivered only when curl completed them. |
| `1c8b2cf` | `fix`: the release treats a deadline as left behind by a backward step only when it is more than 2 s ahead, not 1 s. Two concurrent requests read the clock at slightly different times. With a 1 s margin, the request with the older read saw the other's fresh deadline as more than 1 s ahead and swept a second time. |

| Finding | Answer | Evidence |
|---|---|---|
| CR-01 / SEC-01 HIGH | **Fixed.** The TAT reset was the cause: a stale request time looked like a backward clock step and refilled the bucket. With it removed, the locked `max(tat, now)` can only make a stale time stricter. Reading the clock inside `compute` also removes stale times: a request now decides on a time no older than the TAT it sees, so its `Retry-After` is truthful. | `RateLimiterTest.aRequestOvertakenByNewerOnesDecidesOnTheTimeItReachesTheBucket` reproduces the review ordering deterministically: the first clock read pauses while 60 newer requests empty the bucket 1 ms later, and the overtaken request is refused with `Retry-After: 1`. Against the old code this request was admitted (traced by hand, not run). The reviewer's probe, re-run unchanged on the candidate: `before=60 olderRetryAfter=1 laterAdmissions=0 totalAdmissions=60` (`proof/rate-boundary-1c8b2cf.txt`; it was 120 on `a7c533f`). |
| CR-02 HIGH | **Fixed.** curl now judges completeness: exit 18 for a short body, 52 or 56 for a cut connection. The 10 s bound is enforced with millisecond timestamps, from the moment just before `kill -TERM` (`--drain`) or before `docker compose restart` is issued (`--restart`) until curl exits. Measuring this way overstates the elapsed time, so the check is conservative. | `proof/smoke-r0-control.py` runs the candidate's own `r0_open` and `r0_finish` against loopback peers, giving 6 of 6 expected verdicts (`proof/smoke-r0-control-1c8b2cf.txt`). A complete 201 passes. These are rejected: a 201 header with 0 of 100 body bytes (the review case, curl exit 18), a 201 with 10 of 100 bytes (18), a close with no response (52), a complete 500, and a complete 201 sent 11 s after the stop. In every case the peer received the headers about 0.5 s before the body completed, so R0 is held in flight. On the real jar, `--drain` gave: R0 curl exit 0, status 201, complete 518 ms after SIGTERM; probe refused; 62 ok, 18 refused, 0 boundary losses, 0 failures (`proof/smoke-drain-1c8b2cf.txt`). The review's `smoke-r0-boundary.py` relies on the old interface (`r0_open <port>`, `r0_finish` printing a status line), so it no longer runs against this script; the control above takes its place. |
| CR-03 MEDIUM | **Fixed**, a rule the lead allowed. A release deadline more than 2 s ahead can only follow a backward step, so the release runs and is rescheduled. This changes when cleanup runs, never a client's budget. The lead approved "more than 1 s ahead". I widened it to 2 s (`1c8b2cf`) so that clock reads by concurrent requests, a few nanoseconds apart, cannot trigger an extra sweep. A step back of 1–2 s delays the release by under 2 s. | `RateLimiterTest.theReleaseResumesAfterABackwardClockStep` covers 10,000 clients admitted after a 1 h step back, then 61 s and one request: 2 clients remain (the one stamped before the step, and the new one). The reviewer's probe gives `clientsAfter61SecondsAndRequest=2`; it was 10,002. Its `cleanupDeadlineStillInFuture=true` is a fixed string in the probe, not a measurement. |

**Disclosed limit (the clock policy, as the lead set it at 12:37Z):** a backward step of the wall clock is outside the limiter's contract, and the limiter fails closed. A client that was recently active waits until the clock catches up with its stored TAT, which is up to the length of the step. `RateLimiterTest.afterABackwardClockStepTheBucketRefillsFromItsStoredTat` pins this: an empty bucket and a 60 s step back give `Retry-After: 61`. Cleanup is not affected (CR-03).

| Command | Outcome | Record |
|---|---|---|
| `scripts/gw --offline check --rerun-tasks` on the tree committed as `1c8b2cf`, after the last code edit | BUILD SUCCESSFUL; **unit 165/165, functional 155/155**, 0 skipped; merged JaCoCo **443/443 lines, 162/162 branches**; `javadoc` green | `proof/builder-check-1c8b2cf.txt` |
| reviewer's `reviewRateBoundary` init script, unchanged | REORDER total 60 (max 60); ROLLBACK_MEMORY 2 | `proof/rate-boundary-1c8b2cf.txt` |
| `python3 missions/01-greenfield-core/slices/03-operate/proof/smoke-r0-control.py` | 6/6 | `proof/smoke-r0-control-1c8b2cf.txt` |
| `scripts/smoke.sh --drain build/libs/urlshort.jar 18090` (jar from `1c8b2cf`) | SMOKE DRAIN OK | `proof/smoke-drain-1c8b2cf.txt` |

Unit tests went from 163 to 165: `aBackwardClockStepStartsTheClientFreshInsteadOfLockingItOut` was removed, and three tests were added (the overtaken request, refill from the stored TAT, the release resuming).

#### Self-check (rework)

- **Locked rule restored and nothing else changed in production.** I re-read the diff `a7c533f..1c8b2cf`. Production changes are confined to `RateLimiter.java` and `scripts/smoke.sh`; the test files touched are `RateLimiterTest` and the two granted journeys. No assertion changed in the granted files: each shifted request gained a peer, and AC09's stats read is inlined only to carry that peer.
- **The grant is needed.** Without it the base context locks `127.0.0.1` out: 48 failures were seen before the TAT reset existed. I did not re-run without the grant in this round.
- **The `--restart` mode is still not run** (Docker is `release_prep`'s). Its R0 path shares `r0_open` and `r0_finish` with `--drain`, so the control and the jar run cover that code but not the compose context.
- **The concurrent sweep margin is reasoned, not tested.** No test runs two requests whose clock reads are skewed. The unit tests cover both sides of the 2 s condition: a 1 h step back, and a deadline 0.5 s ahead.
- **`--bench` was not re-run on `1c8b2cf`.** Its code has not changed since `a7c533f`. The limiter's only cost change is a second clock read per request.
- **The 10 s bound is measured, not configured.** The control's 11 s peer shows it rejects. curl's `--max-time 30` only keeps a hung R0 from stalling the script.
- **`scripts/smoke.sh` now needs `perl` with `Time::HiRes`** for its millisecond timestamps. `Time::HiRes` is a core module and works with the stock macOS perl used here; I did not check a Linux host. This is a new host requirement for `--drain` and `--restart`, which run on the host, not in the container.


## QA

Seat `qa2-agent@urlshort-factory` (Codex), 2026-10-03 UTC; packet
`qitem-20261003114044-007e2031`. Independent verdict: **PASS for AC-1–AC-20
and the assigned QA boundary**, on exact candidate
`a7c533ffef55650e5b422377ffe0c4e38d41400c`. Not slice closure or a release verdict.

### Build, coverage and traceability

- Worktree HEAD equaled the packet candidate before and after; clean throughout.
  No `src/`, tests, build or product configuration edited by QA.
- Fresh `../../scripts/gw --log ../../docs/qa/03-operate/check-a7c533f.txt --offline check --rerun-tasks`
  executed all 14 tasks: BUILD SUCCESSFUL, 163 unit / 155 functional invocations,
  zero failures/errors/skips, Javadoc and coverage verification passed.
- CSV totals: unit 398/441 lines and 160/160 branches; functional 406/441 and
  129/160; merged **441/441 lines and 160/160 branches**. Copies include all
  HTML/XML/CSV reports, 321 verified hashes, under `docs/qa/coverage/03-operate/`.
- `docs/qa/TRACEABILITY.md` maps all **184 source methods** plus all eight
  release criteria; complete method/invocation inventories are in proof/.
  Inherited methods ran again on this candidate; their original slice's AC/rule
  numbers remain explicit. Every current AC-1–AC-20 has a functional method.
- `GAPS.md` contains no in-suite or merged-coverage gap, and individually names
  AC-21–AC-28 with their pending release checks and supplemental observations.

### Independently observed effects

The runner recorded **2,303 real HTTP exchanges** via `scripts/http`, real
Tomcat and migrated file H2. `proof/qa-http-a7c533f.json` retains arguments,
status lines, headers and bodies; `qa-observed-a7c533f.json` names each effect.
`qa-instrument-a7c533f.md` discloses the temporary primary Clock, controlled
request peers and reversible DataSource failure switch. These are test controls,
not additions to the product jar. No real alternate-TCP-peer or natural outage
claim is made. All original SPEC-fixed peers and clocks also passed freshly in
functional tests.

- **AC-1/2:** exactly 60 creates then 429, with exactly 60 new create audit rows;
  exactly 600 redirects then 429 without Location. Real H2 before/after exports
  substantiate the storage effect.
- **AC-3/4:** at an empty create bucket, 999 ms is refused, 1000 ms admits one,
  immediate repetition is refused; rounded-up Retry-After admits the retry;
  a quiet controlled minute restores exactly 60 tokens.
- **AC-5/6/7/8/10:** budgets and controlled peers stay independent; changing
  XFF/Forwarded/X-Real-IP cannot evade default limits. A configured trusted real
  loopback peer uses the right-most untrusted XFF; absent header/untrusted
  controlled peer remains independent. Environment-configured 2/3 budgets
  produce 201/201/429 and 302/302/302/429.
- **AC-9:** 60 invalid URL bodies consume create tokens; valid and oversized
  follow-ups are both 429; rejection consumes no token. Encoded `/api` remains
  charged to the tighter budget, all six API operations really produce 429,
  and operator paths remain accessible after both buckets are exhausted.
- **AC-11/12:** exact problem media/body, delta Retry-After, server request id,
  no errors/detail or submitted value. **30/30** captured 429s across the
  controlled and plain-jar runs correlate to **exactly one** JSON event each,
  with the same request id and status. `qa-correlated-429-a7c533f.jsonl` is the
  retained correlation evidence; the default run's complete log has zero
  client/forwarded/UA/rejected-URL/inbound-id canary matches.
- **AC-13/14/15:** healthy probes 200 UP; while the controlled actual datasource
  refuses connections, readiness/root health 503 DOWN and liveness 200 UP;
  restored datasource restores readiness. All bodies contain only status and
  optional liveness/readiness group names, no installation details.
- **AC-16/17/18/19:** timer/rejection/pool metric names present; +2 create and +3
  redirect rejections add exactly those counts, with only a budget tag. Four
  redirects increment the 302 count by four under `/{code:[A-Za-z0-9]{6,32}}`.
  Prometheus includes the promised families and none of the code, path, raw
  address, forwarded, UA or target-URL canaries.
- **AC-20 / items 8–10:** all six operations document 429, integer Retry-After
  and an example. Whole live/committed documents are equal after key sorting,
  with no normalization (`qa-openapi-a7c533f.diff` is empty). Candidate descends
  from 02's merge `091ff46`; ADR-0014–0017 and their index were accepted before
  the commits that depend on them (`qa-ancestry-a7c533f.txt`).
- **Storage/privacy:** final exports contain 308 links, 309 audit rows and 1,806
  reduced clicks, with zero searched raw peer/forwarded/rejection-canary
  matches. The limiter adds no storage table. Admitted target URLs remain in
  link/audit records as required. Inherited failure paths were also observed:
  invalid URL 400, wrong method 405, missing link 404, oversized body 413,
  duplicate replay 201, key mismatch 422, retire 204/410, and a new key binding
  after controlled 24-hour idempotency expiry.

### Supplemental installed-artifact observations and limits

Separate **unmodified `java -jar`** runs on loopback used no test controls.
Plain-jar smoke passed; public URL, data location, budgets and trusted-proxy
settings were observably overridden through the environment (AC-26 support).
The actual file database appeared at the overridden location.

The shipped `--drain` script reported PASS: held R0 status 201 within **1 s**, new connection refused, 82 load
attempts reported as **62 ok / 20 refused / 0 boundary losses / 0 failures**;
its own reconciliation reported no dispatched-but-undelivered request. CR-02 later proved that this predicate can credit a header-only response: **complete body delivery was not independently verified and is not established by this result**. The final AC-25 release record remains pending. The functional configuration assertion pins 10 s.

The 60-second `--bench` mode executed and reports all requested fields (item
12): **4,948 redirects / 82.5 req/s**, p95 2.3 ms, p99 4.1 ms; **990 creates /
16.5 req/s**, p95 2.4 ms; zero non-2xx/3xx responses. This is **below the specified
100/20 input rates**, so it does not prove NFR-L1/L2. Fixed post-request sleeps
reduce achieved load. QA-OPR-01 and the release gap retain the required rate and
numeric judgment; no lowered target or waiver.

QA `docker compose config --quiet` exited 0. No container was started, so
AC-21–AC-24/AC-28 and container hardening/restart were **not observed**. The
locked SPEC makes them release-level; item 13 remains unaccepted until its
required RELEASE.md/inspect/restart/bench records exist. Item 11 requires the
following independent code/security review records and is likewise pending.
The anonymous Prometheus disk gauges disclose a working-directory path
(QA-OPR-02 LOW); this is outside AC-19's prohibited values and recorded for review.

The first two instrument attempts stopped on disclosed QA setup/assertion
limits, not candidate failures (macOS source-address bind; permitted root-health
`groups` member). Their captures are retained; see `qa-instrument-a7c533f.md`.

### Self-check

Every in-suite AC exercised by observed HTTP/storage/log/metric effects;
material failure cases tried; exact controlled mechanisms disclosed; merged
CSV totals read and 321 copied-file hashes checked; all 184 methods/318
invocations and release checks mapped; gap rows written; proof drop covers
only items 1–10 and 12; items 11/13 retained durably for later records. Apps
stopped (process receipt plus jar drain result), builder data preserved, exact
candidate unchanged and clean. No container, offered-rate latency-target,
natural wall-clock/outage or downstream-review claim.


### Attributed judgments

Evidence commit `0b10ca9` contains the coverage/captures/traceability. Receipts
`proof/judgments/00000001.md`–`00000012.md` accept items 1–10 and 12, attributed
to `qa2-agent@urlshort-factory` against commit
`a7c533ffef55650e5b422377ffe0c4e38d41400c`. Fresh `rig proof show` confirms
these accepted, only items 11 and 13 pending, and no issues. Overall readiness
correctly remains `not-ready` until the later review/release records arrive.
Lead accepted obligation `qitem-20261003120849-f4cbfa97`; item 11 returns after
review, item 13 after release_prep. The lead's commit `1248441` preserves the
QA Agent 2 mission NOTES entry unchanged.

The scope audit reports no 03-operate findings. Receipt 00000012 reaffirms item 6 after adding the required C1 header to the instrument disclosure; observed effects are unchanged.


### Review follow-up — 43cccf5

Independent review returned a7c533f to implement on CR-01/SEC-01 (120 admissions
in 1 ms from ordinary request-time reordering) and CR-02 (header-only 201
accepted by the R0 instrument), with CR-03 cleanup rollback MEDIUM. The
historical sequential/test/coverage observations stand; they do not establish
concurrent budget preservation or complete shutdown responses. QA withdrew
item 12 in `proof/judgments/00000013.md`; item 11 remains unaccepted and item
13 is release-owned. Current candidate is not merge-ready.

`docs/qa/03-operate/review-followup-a7c533f.md` and the NOT-CLEAR proof drop
`proof/qa-review-followup-a7c533f.md` explain the correction. QA read the two
reports and probe outputs; it did not rerun the probes or check another
candidate without a packet. Re-check must cover reordered timestamps, rollback
cleanup, complete-body/truncation predicates and the total shutdown deadline.
