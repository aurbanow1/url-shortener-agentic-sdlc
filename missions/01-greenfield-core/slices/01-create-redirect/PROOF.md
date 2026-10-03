# PROOF — OPR.99.0.2.1 Create and redirect

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.2.1 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated — written by QA at close>

## Artifacts (media in proof/)

<written by QA at close>

## Residue / caveats (if any)

<written by QA at close>

## Builder

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet
`qitem-20261003063930-0cb9c7eb`. Builder evidence, not a verdict: QA,
code review and security review judge it independently.

**Candidate:** `a922f49` on `slice/01-create-redirect`
(`.worktrees/01-create-redirect`, base `7636264`). Five commits, each green on
its own under `scripts/gw check` (100 % line and branch merged, Javadoc gate
from the second commit on):

| SHA | Commit |
|---|---|
| `20aef57` | `chore`: Tomcat 11.0.25, Jackson 3.1.7, Jackson 2 2.21.7 as BOM property overrides; `build.gradle.kts` only |
| `aa39560` | `chore`: `javadoc -Xdoclint:all -Werror`, `check` depends on it; Javadoc on the four 00-hello classes |
| `6dc8cb2` | `feat`: `Problems`, `ProblemDetailsAdvice`, `RequestBodyLimitFilter`, `request completed` event; multipart off, servlet eager, `PageNotFound` at ERROR |
| `b6d7ca6` | `feat`: V1 migration, link feature (create/read/retire/redirect, idempotency), `AuditLog`; all journeys AC-1 to AC-27 |
| `a922f49` | `feat`: OpenAPI annotations, `OpenApiConfig`, `OpenApiDocumentTest`, committed `docs/api/openapi.json` (AC-28) |

### Commands and outcomes

| Command (in the worktree) | Outcome | Record |
|---|---|---|
| `scripts/gw check` on `20aef57` alone | BUILD SUCCESSFUL | (run 06:42Z; not kept as a file) |
| `scripts/gw dependencies --configuration runtimeClasspath`, then `node tools/dep-advisories.mjs` on `20aef57` | 90 runtime coordinates, **0 OSV advisories** (2026-10-03T06:42:17Z); tree shows tomcat-embed-core 11.0.25, jackson 3.1.7, jackson 2 2.21.7 | `proof/runtime-dependencies-20aef57.txt`, `proof/osv-advisories-20aef57.json` |
| `scripts/gw check` on `aa39560`, `6dc8cb2`, `b6d7ca6` | BUILD SUCCESSFUL each, before committing | build logs, not kept |
| `scripts/gw functionalTest` before the link feature existed (test code + an `AuditLog` stub) | 85 tests, **64 failed** for the right reasons (404/405 where endpoints were missing, no `link` table) | `proof/builder-red-functional-before-link-feature.txt` |
| `scripts/gw check --rerun-tasks` on `a922f49` (last command after the last code edit) | BUILD SUCCESSFUL; **unit 72/72, functional 87/87**, 0 skipped; merged JaCoCo **100 % line, 100 % branch** (0 missed in every class of `build/reports/jacoco/all/jacocoTestReport.csv`); `javadoc` green | `proof/builder-check-a922f49.txt` |
| `scripts/gw bootRun --args=--server.port=18081`, six `scripts/http` exchanges, app stopped, H2 queried with `org.h2.tools.Shell` | see by-effect below | `proof/http-*-a922f49.txt`, `proof/log-lines-a922f49.txt`, `proof/bootrun-log-a922f49.txt`, `proof/audit-rows-a922f49.txt` |

Red first, per step: the `request completed` test failed before the event
existed (`RequestIdFilterTest`, AssertionError, 22 run / 1 failed), and the
OpenAPI test failed before the annotations and before the file was committed
(both assertions). The other web unit tests and the link and audit unit tests
were written before their classes. For those, "red" was a compile failure,
not a behavioural one; the 64-failure functional run above is the behavioural
red for the feature.

### Verified by effect (running jar via bootRun, real Tomcat, file H2)

- **201 create** (`http-201-create-a922f49.txt`): `Location: /api/links/QUPpzC7N`, `X-Request-Id`, five-field body, `shortUrl` on the shipped base `http://localhost:8080`, `createdAt` `…07:09:12.372Z`.
- **200 read** returns the identical body; **302** `Location` byte-for-byte the stored URL with `Cache-Control: no-store`, and `?utm_source=x` on the short link not forwarded. **204 retire** has an empty body. The **Visitor's 410** is `application/problem+json` `{"instance":"urn:uuid:<X-Request-Id>","status":410,"title":"Gone"}` with no `Location`. **400** for `javascript:` names `field` `url`, `rule` `scheme` and does not echo the value.
- **Logs:** exactly one ECS line per captured request, `request completed`, `requestId` equal to the header, `status` only. `grep -c -E "canary|127\.0\.0\.1|0:0:0:0:0:0:0:1|curl/"` over the whole run log returns 0. The run used a `User-Agent` canary, an `Idempotency-Key` canary, a query canary inside the URL, a `javascript:` canary, curl's own user agent and the loopback client address. The DispatcherServlet initialised before `Tomcat started`.
- **Audit rows:** one `link.create` row (actor `anonymous`, `before_state` null, request id of the create) and one `link.retire` row (before `active`, after `retired`, request id of the retire) for the captured code. The read, redirect, 410 and 400 requests wrote no row.

### Deviations from design.md, each with its reason

1. **Commit 2 adds three empty public constructors.** It touches `UrlshortApplication`, `PingController` and `RequestIdFilter`, which are under the Javadoc-only grant. `-Xdoclint:all` flags implicit default constructors ("use of default constructor, which does not provide a comment"), and javadoc cannot attach a comment to a constructor that is not declared. No narrower doclint switch silences only that warning. The explicit constructor is the one the compiler generated before, so the bytecode and behaviour are unchanged. The design (§13) expected comments only; reviewers should check this diff with that in mind.
2. **OpenAPI regeneration uses a copy, not `OPENAPI_EXPORT=1`.** `OpenApiDocumentTest` always writes the live document, key-sorted, to `build/openapi/openapi.json` and fails when `docs/api/openapi.json` differs. To regenerate, run the test and then `cp build/openapi/openapi.json docs/api/openapi.json`. The reason: an environment-prefixed command stops for manual approval on Claude seats. The determinism and the drift check are unchanged.
3. **`info.version` is `"1"` (the API contract version), not the build version.** The design said "version from the build". No build-info is generated, and a build version would change the committed document at every release without any change to the API.
4. **`malformed` checks `getHost() == null` only.** The design said "null or empty". `java.net.URI` never returns an empty host, so that branch could not be covered and would have been dead code. Every AC-4 `malformed` input is still rejected, including `https:///no-host` (unit test).
5. **The cold-start test polls with `Thread.onSpinWait()`**, bounded at 5 s, instead of 50 ms sleeps. It still waits for a condition, not for a fixed time.
6. **The database-failure canary uses the design's primary mechanism, not its fallback.** The spy answer's `INSERT … SELECT` sees the row inserted in the same transaction, so H2 raises the real duplicate-key failure. The test asserts `DuplicateKeyException` in `errorChain` and that the create was rolled back.

### Not verified, and residual risks

- **No PostgreSQL run.** Everything ran on H2 in PostgreSQL mode. This gap is pre-existing (databases.md §5).
- **Timestamp offsets differ by table.** `link.created_at` and `retired_at` are stored with the JVM's zone offset, because Spring Data JDBC binds `Instant` through `java.sql.Timestamp`. `audit_log.occurred_at` is stored at `+00`. The instants are identical and the API returns UTC (`proof/audit-rows-a922f49.txt`); only a raw SQL reader sees the different offsets. A low-risk follow-up if anyone wants one representation.
- **The API document's `ProblemDetail` schema is springdoc's reflection of the class.** It shows a nested `properties` member, whereas the runtime body has the extension (`errors`) at the top level. `errors[]` has no schema of its own. AC-28 does not ask for one; the rule tokens are documented in the SPEC.
- **`DELETE /api/links/<segment that is not a code>` answers 405, not 404.** The static resource handler accepts only GET and HEAD. This is outside every AC (AC-14's `DELETE` uses a well-formed code); recorded as an observation.
- **A same-key race and a code collision each answer 500.** Both are by design (`// ponytail:` comments in `LinkService` and `ShortCodes`; ADR-0007, ADR-0009). The unique constraint still guarantees at most one link per key. A true concurrent race was not exercised; the same failure was induced through the spy.
- **Not done by me:** the copies to `docs/qa/coverage/…`, `TRACEABILITY.md` and `GAPS.md` (QA's step), the code review's NFR-A2 and NFR-S4 records, and the release-prep secret scan.

## Self-check

- **Re-read the whole diff** (`git diff 7636264 a922f49`, 48 files). Every path is inside `slice.yaml`'s territory; the `ping/` and `UrlshortApplication` changes are Javadoc plus the constructors from deviation 1.
- **No dead code.** Each constant and method is used; the only unreachable branch (empty host) was removed rather than excluded.
- **No duplicated logic.** The error rendering lives in one advice; the problem shapes in `Problems`; validation in `LinkValidation`.
- **Every error path has a test,** at both levels: unit tests for the advice, the filter, validation and the service branches; journeys for 400, 404, 405, 410, 413, 415, 422 and 500.
- **No PII in any log line.** By test: AC-27's canaries, the database canary and the framework-echo canaries. By effect: the run-log grep above. The `500` event logs class names and one frame only; the method is not logged.
- **Ladder applied.** No new dependency. No interface with one implementation besides the Spring Data repository. No domain exception hierarchy (`ErrorResponseException` is the platform's). No Bean Validation group sequence, no second table, no retry loop. The two deliberate ceilings carry `// ponytail:` comments naming the ceiling and the upgrade path.
- **Every AC has a named test.** AC-1 to AC-28 each map to a test whose name starts with its id: tabled criteria as `@ParameterizedTest`; AC-19 on the suite clock `FunctionalClock`. Business rules 7, 8 and 10 have their own `rule*` tests, and NFR-M3 has `NFRM3_committedDocumentEqualsTheLiveOne`. All were watched failing first, as described above.
- **Possible false passes, checked.** The functional suite ran 87 of 87 with 0 skipped; parameterised tables run their rows (AC-4: 17, AC-26: 12). The AC-26 assertion covers every line written while the request ran, not only lines containing the id.
- **Gate run after the last edit:** `scripts/gw check --rerun-tasks` on `a922f49`, recorded in `proof/builder-check-a922f49.txt`.
