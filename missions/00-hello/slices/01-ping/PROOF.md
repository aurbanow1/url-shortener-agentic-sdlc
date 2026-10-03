# PROOF — OPR.99.0.1.1 Ping endpoint

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.1.1 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

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

Development agent, 2026-10-02. Candidate `3886a04a4afac6117038b2884cf72749f57d28aa`
on branch `slice/01-ping`, three commits on top of `f43ecd1`: `aa66007` functional
profile overlay, `8cb03f6` request-id filter, `3886a04` ping endpoint. Code lives in
`.worktrees/01-ping`; on `main` this slice touched only this file, `PROGRESS.md` and
`proof/`.

### Commands run and their outcome

Every Gradle call was made from the main checkout with `-p .worktrees/01-ping`
(main's `scripts/gw` for its `--log` flag; same wrapper, JDK 21 and repo-local
Gradle home, `build/` lands in the worktree).

| Step | Command | Outcome |
|---|---|---|
| Red, no production class yet | `scripts/gw --offline -p .worktrees/01-ping --continue test functionalTest` | Unit suite: 4 compile errors (`RequestIdFilter`, `PingController`, `PingResponse` unknown). Functional suite: 9 tests, 8 failed, `HealthJourneyTest` green. Messages: AC-1/2/4/6/7/8 `Status expected:<200> but was:<404>`; AC-3 `Response should contain header 'X-Request-Id'`; AC-5 `Status expected:<405> but was:<404>`. The captured output was already ECS JSON, so the profile overlay loads the shipped configuration. |
| Green, after the last code edit | `scripts/gw --offline -p .worktrees/01-ping check` | BUILD SUCCESSFUL in 19s, 13 tasks executed. |
| Gate on the committed candidate | `scripts/gw --offline -p .worktrees/01-ping check --rerun-tasks` | BUILD SUCCESSFUL in 18s, all 13 tasks executed. Log: `proof/builder-check.txt`. |

Test counts from `build/test-results`: unit 6 (`UrlshortApplicationTests` 2,
`RequestIdFilterTest` 3, `PingControllerTest` 1); functional 9
(`HealthJourneyTest` 1, `PingJourneyTest` 8, one per AC).

Coverage from the merged report `build/reports/jacoco/all/jacocoTestReport.csv`:
`PingResponse` 1/1 lines, `PingController` 4/4, `RequestIdFilter` 7/7,
`UrlshortApplication` 3/3, so 15/15 lines, 100%. Branches: 0 missed of 0. The new
code has no conditional, so the branch ratio holds trivially rather than by test
design. `jacocoTestCoverageVerification` passed.

### Verified by effect

App started from the candidate with
`scripts/gw --offline -p .worktrees/01-ping bootRun --args=--server.port=18080`
(pid 14905, `Tomcat started on port 18080` at 23:42:24Z), exercised with curl,
stopped with SIGTERM (graceful shutdown at 23:45:12Z).

- `proof/ping-get-exchange.txt`: `GET /api/ping` sent with
  `User-Agent: canary-ua-bootrun` and `X-Request-Id: canary-rid-bootrun`.
  Response `200`, `Content-Type: application/json`,
  `X-Request-Id: 7f88e171-58cf-4859-b21f-f271b3e9e9a5` (36 printable ASCII
  characters, not the canary), body
  `{"status":"ok","time":"2026-10-02T23:43:51.496001Z"}`: exactly two fields, a
  `Z`-terminated instant in the same second as the `Date` header. Shows AC-1,
  AC-2, AC-3 and AC-8.
- `proof/ping-post-405-exchange.txt`: `POST /api/ping`. Response `405`,
  `Content-Type: application/problem+json`, `X-Request-Id` present, `Allow: GET`,
  body an RFC 9457 object with `status` 405 and no stack trace or exception class.
  Shows AC-5.
- `proof/ping-log-line.json`: the one line of the app's stdout carrying the GET's
  request id, copied verbatim. A single ECS JSON object, `requestId` equal to the
  response header, `message` `ping`, logger `dev.urlshort.ping.PingController`.
  Shows AC-6.
- AC-7: `grep -c "canary\|127.0.0.1\|::1\|0:0:0:0:0:0:0:1"` over the whole
  bootRun stdout, startup through shutdown, returns 0: neither canary nor the
  loopback client address in its IPv4 or IPv6 forms appears anywhere. The ping
  log line itself has no address field at all.
- `proof/builder-check.txt`: the gate log on the candidate, every task executed.

### Not verified

- AC-4 (two ids differ) only through its functional test; the two captures above
  carry different ids but were not taken as a dedicated pair.
- `HEAD` and `OPTIONS` on `/api/ping`, unknown paths and `Accept` negotiation:
  framework defaults, out of SPEC scope, not exercised.
- The packaged jar (`bootJar`) was not run; by-effect checks used `bootRun` from
  the same classes.
- Proof-contract items 1 to 5 (coverage reports under `docs/qa/`, traceability,
  GAPS row) are QA's. The numbers above come from the worktree's `build/` reports
  and are not yet committed under `docs/qa/`.

### Deviations from the generic builder checklist, all per the locked design

No project `@RestControllerAdvice` (the platform `ProblemDetailsExceptionHandler`
produces the 405 body, design §1, ADR-0002). No audit row (ping mutates nothing).
No Flyway migration (no schema). No OpenAPI annotations (springdoc lists the
endpoint; the SPEC excludes asserting it). The `build.gradle.kts` change is the
single granted line inside the `functionalTest` block.

## Self-check

- Re-read `git diff main...slice/01-ping` (8 files, +298/−1) as a reviewer. No
  dead code. The only repeated logic is the request-id shape assertion, once in
  the filter unit test and once in AC-3, intentional (unit versus journey). The
  error path (405) is tested at the layer that owns it. The log event carries only
  `message` and the MDC id: no IP, user agent or inbound header value. No test can
  pass for the wrong reason: AC-6 asserts a non-empty line set and lets a non-JSON
  line throw; AC-7 asserts the ping event exists before asserting the canaries are
  absent; AC-8 asserts `200` and the issued header before asserting the canary is
  absent.
- Ladder applied: the filter is `OncePerRequestFilter` plus `UUID` plus `MDC`; the
  body is a record; the time is `Instant.toString()`. No `Clock` bean, no exposed
  constants, no `produces` attribute (the converter already yields
  `application/json`), no new dependency. No `ponytail:` comment, because nothing
  was simplified below the SPEC; there is no ceiling to name.
- Every AC has a named failing-first test, `AC1_pingAnswersOkAsJson` through
  `AC8_clientSuppliedRequestIdIsIgnored`, each watched red for the reason listed
  above before going green.
- `scripts/gw check` ran after the last edit, then again with `--rerun-tasks` on
  the committed candidate. No edit between the last check and this handoff.
  Worktree clean at `3886a04`.

## QA

QA Agent (Codex), 2026-10-02/03 UTC. Candidate
`3886a04a4afac6117038b2884cf72749f57d28aa`, verified before execution and after
shutdown; no checkout change or product edit. Verdict **NOT-CLEAR**: fresh
gate green, independent live AC-7 literal address-absence check fails.

Ran `scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-check.txt --offline -p .worktrees/01-ping check --rerun-tasks`
from main to use the operator's updated log wrapper. All 13 tasks executed,
exit 0. JUnit XML: 6 unit plus 9 functional tests, zero failures/errors/skips.
Read all three CSVs: unit 15/15 lines, functional 13/15, merged 15/15; no
branches exist. HTML/XML/CSV reports and summary are under
`docs/qa/coverage/01-ping/`; traceability and gaps are updated.

Started the candidate with
`scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-bootrun.txt --offline -p .worktrees/01-ping bootRun --args='--server.address=127.0.0.1 --server.port=18081 --spring.datasource.url=jdbc:h2:mem:qa-ping'`.
The bind confines the app to localhost; the database override isolates QA
state; shipped ECS logging and problem-details configuration are unchanged.
Initial sandboxed curl calls could not connect; the same `scripts/http`
commands succeeded with approved execution outside the sandbox.

Independent observations:

- AC-1/2/3: `proof/qa-ping-first.txt` is 200 application/json with exactly
  status=ok and a Z-terminated current instant; id
  `010b88fa-3871-48b6-a14e-5d7792d659c7` has the required shape. The first
  and second response times lie within `qa-http-start.txt` and
  `qa-http-end.txt` (lower bound truncated to seconds). Approval latency
  widened that interval; this makes no latency claim.
- AC-4: `proof/qa-ping-second.txt` is the consecutive GET, with distinct id
  `0ff0c2bd-3b07-45d6-8a00-9bf83a96c545`.
- AC-5: `proof/qa-ping-post.txt` is 405 application/problem+json, body status
  405, title and instance, no stack trace or exception class, id present.
- AC-6: every GET's matching line parses as one JSON object with the same
  requestId as its response. `proof/qa-ping-log-line.json` is the first
  GET's unedited event; `proof/qa-bootrun.txt` contains every event.
- AC-7: the User-Agent canary is absent from the complete log, but the
  literal loopback address occurs in `process.thread.name`. This is server
  bind metadata: verified with installed Tomcat bytecode, captured in
  `proof/qa-tomcat-bind-name.txt`. It is not evidence of client-derived
  address logging, yet the literal locked assertion and proof item 7 are
  not met. See `docs/qa/01-ping/findings.md` QA-01; no waiver applied.
- AC-8: `proof/qa-ping-canaries.txt` supplies a distinctive inbound id and
  user agent; the returned id differs and neither canary appears in logs.

No audit row is applicable: ping has no persistence. Validation, duplicates,
expiry and rate limits are outside this slice. HEAD/OPTIONS, unknown paths,
Accept negotiation, load, packaged jar/image and persistence were not tested.
The app (PID 40179) was stopped with SIGTERM and logged graceful shutdown
complete. The resulting bootRun status 143 is the intentional stop, separate
from the green check result.

## Self-check

QA pre-handoff check:

- Exercised all eight ACs through live HTTP and inspected the actual bodies,
  headers and matching log events, including the sole specified error case
  (POST), privacy canary and spoofed id. Recorded the failing assertion
  rather than substituting its passing MockMvc result.
- Read merged and per-suite CSV counts; every executed test and all AC/BR
  mappings appear in TRACEABILITY; GAPS records the informational per-suite
  shortfall and open AC-7 discrepancy.
- Proof drop `proof/qa-evidence.md` uses NOT-CLEAR and names contract items
  1–6. Item 7 is deliberately not attested as satisfied. No slice_accept
  judgments or locked-document edits performed.
- App stopped and graceful shutdown observed. Worktree remains clean at the
  exact candidate. No product code, tests or build files edited.
