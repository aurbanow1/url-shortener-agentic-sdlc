# PROOF — OPR.99.0.2.3 Operate safely

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.2.3 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated>

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/screenshot-01.png — <what it shows>
- proof/capture-behavior.gif — <what it shows>
- proof/command-output.txt — <what it proves>

## Residue / caveats (if any)

<documented residue: what's not covered + where it's tracked>

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

1. **`RateLimiter` treats a TAT more than one full bucket ahead of now as a fresh client.** The design does not say this. A legitimate TAT is never more than `T + I` ahead. Only a backward step of the wall clock can produce one, and without the guard a step back of an hour refuses every recent client for an hour. It was found because `IdempotencyJourneyTest` shifts the shared suite clock 24 h forward and back, which locked `127.0.0.1` out of the base context. One condition; `RateLimiterTest.aBackwardClockStepStartsTheClientFreshInsteadOfLockingItOut`.
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
