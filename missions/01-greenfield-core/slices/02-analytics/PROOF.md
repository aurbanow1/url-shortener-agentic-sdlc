# PROOF — OPR.99.0.2.2 Click analytics

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.2.2 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

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
`qitem-20261003094850-c667801b`. Builder evidence, not a verdict.

**Candidate:** `862c52eea8294e438b1f98b832ae4f64f7a16923` on `slice/02-analytics`
(`.worktrees/02-analytics`, base `6d7f6bb`). Four commits; each passed `scripts/gw --offline check`
on its own (100 % line and branch merged, Javadoc gate):

| SHA | Commit |
|---|---|
| `2499505` | V2 (`click`, `user_agent_class` lookup FK), `Click` reductions, `DailySalt`; `ClickTest`, `DailySaltTest`, `ClickSchemaTest` |
| `646512e` | `ClickStore.insert`, `ClickRecorder` (bounded single writer, fail open, shutdown ownership), the `RedirectController` hook; `ClickRecorderTest`, `ClickRecordingJourneyTest` |
| `fe9e155` | `StatsController`, `LinkStats`, the grouped read; `LinkStatsTest`, `StatsJourneyTest`, AC-17/AC-18 journeys, the granted `OpenApiDocumentTest` line, regenerated `docs/api/openapi.json` |
| `862c52e` | `ClickResilienceJourneyTest` (Tomcat + `ClickStore` spy: AC-14, AC-15, AC-16, AC-18/AC-19 on Tomcat, AC-22 HEAD body), `ClickSchemaJourneyTest` |

### Commands and outcomes

| Command (in the worktree) | Outcome | Record |
|---|---|---|
| `scripts/gw functionalTest --tests …ClickRecordingJourneyTest` before the hook (no-op recorder stub) | 18 run, **17 failed**: no click recorded. AC-02, which expects no clicks, passed trivially | build log, not kept |
| `scripts/gw functionalTest --tests …StatsJourneyTest` before the endpoint | 13 run, **9 failed**: `404` instead of `200`/`405`. The `404` rows, AC-19 and AC-20 passed vacuously | build log, not kept |
| `scripts/gw --offline check --rerun-tasks` on `862c52e`, the last run after the last edit | BUILD SUCCESSFUL; **unit 121/121, functional 126/126**, 0 skipped; merged JaCoCo **100 % line and branch** (no class with a missed line or branch in `jacocoAllReport`); `javadoc` green | `proof/builder-check-862c52e.txt` |
| `scripts/gw --offline bootRun --args=--server.port=18082`, then 1 create, 3 redirects with canaries and 1 stats read via `scripts/http`; app stopped; click rows read with `org.h2.tools.Shell` | see below | `proof/http-*-862c52e.txt`, `proof/click-rows-862c52e.txt`, `proof/log-lines-862c52e.txt`, `proof/bootrun-log-862c52e.txt` |

### Verified by effect (bootRun, real Tomcat, file H2)

- **Redirects and statistics.** Three redirects each answered `302`, with `Location` equal to the stored url, `Cache-Control: no-store` and their own `X-Request-Id`. Each sent the `User-Agent` `Mozilla/5.0 proofuacanary`, the `Referer` `https://Proof.Example/proofpathcanary?q=proofquerycanary` and `X-Forwarded-For: 192.0.2.88`. The statistics read then answered `200` with `{"code":…,"totalClicks":3,"clicksPerDay":[{"date":"2026-10-03","clicks":3}],"topReferrers":[{"referrer":"https://proof.example","clicks":3}]}`, i.e. the four fields and aggregates only (AC-1, AC-8, AC-3 one row, AC-17).
- **Stored rows.** Three click rows, each with referrer `https://proof.example`, class `browser` and the same 64-hex hash. No row holds the address, the user agent, a path, a query or the forwarding value; the table has no column for any of them.
- **Logs.** Exactly one `request completed` line per captured request, carrying its `requestId`; recording a click logs nothing. A grep of the whole run log for every canary, `proof.example`, `192.0.2.88`, the loopback address and `curl/` returns 0, and so does a grep for the stored hash. Every line after startup carries a `requestId`; the only lines without one are startup lines (Flyway, Tomcat, two springdoc WARNs).

### Deviations from design.md, each with its reason

1. **`DailySalt.stamp` throws `GeneralSecurityException`, and `ClickRecorder.record` catches `Exception`**, where the design said `RuntimeException`. `Mac.getInstance("HmacSHA256")` declares a checked exception that cannot happen on a JDK. Catching it inside `DailySalt` would leave an unreachable `catch` that the coverage gate cannot cover; propagating it reuses the recorder's existing fail-open path (`rejected`, one WARN), which `ClickRecorderTest` covers.
2. **`record` has two `try` blocks**, one around building the click and one around `execute`, instead of one. A click is added to `outstanding` only once it exists, and is removed only if `execute` rejects it. This avoids a `null` remove on the concurrent set.
3. **`DailySalt.Selection` carries the instant and the key, not the day.** Nothing used the day.
4. **`ClickSchemaTest`'s pool holds 3 connections, not 2.** With one connection held to keep the database open, Flyway needs two more. With 2 the test timed out acquiring a connection. Evicting the idle connections still retires every connection Flyway used.
5. **AC-22's "`HEAD` has no body" is asserted on Tomcat** (`ClickResilienceJourneyTest`), not in the MockMvc `StatsJourneyTest`. MockMvc keeps a `HEAD` body that the servlet container strips, so the MockMvc test asserts only `HEAD`'s `200` and the unchanged totals.
6. **The suite clock is shifted relative to `FunctionalClock.instant()`, not `Instant.now()`** (design §7.2). `FunctionalClock` ticks in milliseconds, so a shift computed from the microsecond `Instant.now()` could land up to 1 ms before the target. In one run `00:00:00Z` became the previous day and AC-09 failed. Computing the shift from the clock itself makes elapsed time only move the clock later.

### Not verified, and residual risks

- **The NFR-L3 number** (≤ 2 ms p95 added to the redirect) is release-level and needs `03-operate`'s bench. AC-14 proves the structural property only: twenty redirects under 250 ms each while every write takes 2 s.
- **A-9: the click hash uses `getRemoteAddr()`.** `03-operate`'s trusted-proxy rule resolves the client privately and does not rewrite the address, so behind a trusted proxy the hash would be the proxy's. This is on the lead's backlog for `wave_review` (both designs say so). Under the shipped default (no proxy) the two agree.
- **CR-01** (the `errors[]` member in the ProblemDetail schema) is not addressed here, per A-15.
- **The ERD** (`docs/diagrams/erd.mmd`) already shows `click` and `user_agent_class` on `main`, where the design added them, so the branch does not change it.
- **No real midnight was crossed.** Salt rotation and expiry are proven on the suite clock and in `DailySaltTest`; the scheduled expiry test waits about 100 ms of real time.
- **No PostgreSQL run** (pre-existing gap).
- **Not done by me:** QA's coverage copies, traceability, the `GAPS.md` row (NFR-L3), the API-document diff against the live document, and the review records for salt handling.

## Self-check

- **Re-read the whole diff** (`git diff 6d7f6bb 862c52e`, 20 files). Territory holds: `click/` in all three source sets, V2, `docs/api/openapi.json`. Under `link/` only `RedirectController`'s hook changed: the field, the constructor parameter, the request parameter, one call and one Javadoc sentence. Under `web/` only the path list of `OpenApiDocumentTest` changed.
- **No dead code.** The only checked exception that cannot happen is propagated rather than caught, and every branch is covered (merged report: no missed line or branch).
- **No click value reaches a logger.** The only WARN carries `reason` and the exception class (`ClickRecorderTest` asserts the message, which quotes the hash, is absent). AC-18 checks this on MockMvc and on Tomcat, and the bootRun log grep returns 0.
- **The request is never read off-thread.** It is reduced on the request thread, and only a `Click` crosses. AC-16 on Tomcat shows all 200 stored rows carry the reduced values.
- **Every unwritten click is reported exactly once.** Ownership is a `compareAndSet` on each click's state, and `ClickRecorderTest` covers the stuck-write close, the start race, both late-completion races and the rejected `execute`. The shutdown reports are present when `close()` returns.
- **Ladder applied.** No new dependency, property, `@Async`, scheduler framework, service class or summary table. `CompletableFuture.delayedExecutor` expires the salt; HMAC and hex come from the JDK.
- **Every AC has a named test:** AC01–AC22 in the journey classes, AC-11 inside AC-08/09/10, AC-20 in `StatsJourneyTest`. Watched failing first: the recording and statistics journeys (the two red runs above). **Not watched failing:** `ClickResilienceJourneyTest` (AC-14, AC-15, AC-16, the Tomcat rows of AC-18, AC-19 and AC-22) and `ClickSchemaJourneyTest` were written in commit 4 against working code. AC-14's non-blocking redirect, AC-15's fail-open WARN and AC-16's count under concurrency are behaviour those classes prove for the first time, not re-runs of a MockMvc red. Commit 1's unit tests were red only as compile failures, as on slice 01.
- **One flaky test was found and fixed at the root:** the 1 ms clock-shift race (deviation 6). It was not retried away.

