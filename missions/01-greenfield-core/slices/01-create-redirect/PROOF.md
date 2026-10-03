# PROOF — OPR.99.0.2.1 Create and redirect

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.2.1 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

QA checked by: `qa-agent@urlshort-factory` (Codex), 2026-10-03. QA verdict: **PASS** on `a922f49144049db0228c316c474ac6e890742fa5`; delivery acceptance awaits the later review records.

## What this proves

The candidate creates, reads, retires and redirects short links with the specified error, idempotency, audit and privacy behavior. Independent QA passed 72 unit and 87 functional invocations and observed the running service's HTTP effects, correlated logs, audit rows and real audit-failure rollback. Merged coverage is 185/185 lines and 56/56 branches; the live OpenAPI document matches the committed candidate document.

## Artifacts (media in proof/)

QA captures are listed in §QA below; HTML/XML/CSV coverage reports and their totals are under `docs/qa/coverage/01-create-redirect/`. The attributed QA proof drop is `proof/qa-evidence-a922f49.md`.

## Residue / caveats (if any)

AC-19 uses the suite-controlled clock explicitly required by the SPEC, rather than a 24-hour wait. H2 only; no packaged artifact, PostgreSQL, release secret scan or concurrent same-key race claim. Proof items 13–14 remain open for code review and release prep, tracked by `qitem-20261003072643-917956c7`.

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
- **springdoc startup WARNs (INFO for `03-operate`).** At startup springdoc writes two WARN lines without `requestId`, recommending that `/v3/api-docs` and `/swagger-ui.html` be disabled in production. They are startup lines, outside any request window (`proof/log-lines-a922f49.txt`). Exposing them in production is `03-operate`'s decision (design §2.7).
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

## QA

Seat `qa-agent@urlshort-factory` (Codex), 2026-10-03; packet
`qitem-20261003071605-40d1d3aa`; exact candidate
`a922f49144049db0228c316c474ac6e890742fa5`. Product code and tests were read-only.

**Gate and coverage.** Independently ran
`../../scripts/gw --offline check --rerun-tasks` in the clean candidate
worktree. All 72 unit and 87 functional invocations passed with zero
failures, errors or skips, including the Javadoc gate
(`proof/qa-check-a922f49.txt`). Read every CSV counter: unit lines 169/185
(91.35%), branches 56/56 (100%); functional lines 176/185 (95.14%), branches
50/56 (89.29%); merged lines 185/185 and branches 56/56 (100% each).
Copied all three HTML/XML/CSV reports to `docs/qa/coverage/01-create-redirect/`.
`TRACEABILITY.md` maps all 42 unit and 45 functional methods and all 159
invocations they own to ACs or business rules, with FR/NFR ids. The exact
JUnit invocation inventory is `proof/qa-test-invocations-a922f49.txt`.

**HTTP effects.** Started the unchanged candidate on loopback port 18091
with an isolated database at `build/qa-h2/urlshort`, preserving the builder's
database. Captured text normalizes CRLF, status-line trailing spaces and
blank lines at EOF; response bodies and header values are unchanged.
`proof/qa-http-a922f49.txt` contains create/read/redirect/retire,
retired reads and browser errors, all AC-4 validation rows, all AC-5 body
shapes, multipart, the exact 16,384/16,385-byte boundary, all AC-14 and AC-15
requests, the route regression guard, three idempotent replays, mismatch
with binding preserved, all malformed key cases, the 255-character key
boundary, and retry after rejection. Verified the exact five-field body,
UTC creation interval, byte-identical reads and replay, verbatim Location,
no-store, an empty 204 body and no Location on 410. No-key duplicate creates
produced ten distinct codes. Replay after retirement returned the current
retired representation. The configured environment
`URLSHORT_PUBLIC_BASE_URL=https://sho.rt` produced `https://sho.rt/<code>`
despite spoofed Host/forwarding headers (`proof/qa-configured-base-a922f49.txt`).

The loopback wrapper rejected a plain-text body passed as a URL argument
before any HTTP request occurred; resent that body from a file and observed
415 (`proof/qa-rollback-http-a922f49.txt`). The environment-prefixed build
hit the sandbox's socket restriction; the same localhost launch succeeded
after approved escalation. Neither instrument issue was a product failure.

**Logs and privacy.** Independently inspected the full runtime logs,
including startup, and parsed every event in the 75 measured request
windows. Each window was nonempty and every event carried that response's
requestId; each response had its matching completion status. The five
canaries, target URL, loopback client addresses and SQL failure constraint
name were absent. The 500 path produced a class-only `request failed` event
and a correlated completion event. Raw logs are
`proof/qa-runtime-a922f49.jsonl` and `proof/qa-failure-runtime-a922f49.jsonl`;
selected correlated events are `proof/qa-correlated-events-a922f49.jsonl`.
The repeated operation run also has all eleven completion events in
`proof/qa-append-runtime-a922f49.jsonl`. The environment-override response
was captured; its run was stopped before its completion event was captured,
so that capture supplies AC-3 evidence only. Startup springdoc WARNs occur
before request windows and contain no client values.

**Audit and rollback.** With the app stopped, exported H2 audit/link rows
directly; the audit endpoint is not part of this slice. The main captured
link has exactly one create and one retire row, anonymous actor, entity
`link`, matching response requestIds, in-window UTC times, null before-state
on create and the correct active/retired snapshots. Fifteen unique links
and sixteen audit rows account exactly for the successful primary mutations;
all rejected requests and replays added no row. Added a temporary CHECK
constraint to the disposable QA database that rejects the audit insert for
one active link's retire, restarted the unchanged app and observed actual
500, then active read and 302 redirect. All sixteen audit and fifteen link
rows were identical before and after the failure. Removed the constraint.
After an environment create and another full operation sequence, the
original rows were still identical and the count grew by exactly three
mutations (environment create, fresh create, first retire): nineteen audit
rows, seventeen links. Evidence: `proof/qa-audit-{before,after,final}-a922f49.csv`,
the matching link exports, `qa-audit-injection-a922f49.txt`,
`qa-audit-comparison-a922f49.txt`, `qa-rollback-http-a922f49.txt` and
`qa-append-http-a922f49.txt`.

**Clock and API document.** Read and ran
`IdempotencyJourneyTest#AC19_aKeyIsHonouredFor24HoursAndNotLongerAndARejectionDoesNotExtendIt`
with `FunctionalClockConfig`'s primary `FunctionalClock.shift/reset` at 23h,
24h−1s and 24h+1s, as A-19 explicitly permits. Independently checked the live
document's paths, methods, required statuses, problem media types, Location
header and request/201/200 examples, then compared its parsed recursively
key-sorted content with the candidate's committed `docs/api/openapi.json`:
empty diff (`proof/qa-live-openapi-a922f49.json`,
`proof/qa-openapi-diff-a922f49.txt`).

**Earlier artifacts.** Inspected the first feature-branch commit: `20aef57`
changes only the dependency override lines in `build.gradle.kts`. Ran its
full gate independently in a temporary detached checkout: successful
(`proof/qa-overrides-check-20aef57.txt`), then removed that checkout. The
builder's fresh OSV capture at 06:42:17Z contains 90 coordinates, the
required Tomcat/Jackson versions and zero findings; QA did not rerun the
external service. ADR-0005–0010 exist and are indexed in `docs/DESIGN.md`;
their creation (`0aaab2f`), revision (`d93e7d5`) and acceptance (`0210716`)
precede the dependent feature commits (first code commit 06:42Z).

**Not verified here.** Packaged artifact, PostgreSQL, load, a simultaneous
same-key race, and the later code-review/release secret-scan records. Expiry
and rate limiting are outside this SPEC. Proof item 13's unit persistence
assertion and item 14's environment setting are covered, but the complete
items depend on future records and are left unjudged. Lead follow-up:
`qitem-20261003072643-917956c7`, evidence
`docs/qa/01-create-redirect/proof-sequencing.md`.

### Self-check

- Re-read SPEC; all 28 ACs have named functional coverage, and every
  externally controllable AC was exercised through real HTTP and effects;
  AC-19 uses the SPEC's explicit clock mechanism.
- Tried every listed failure input and response class, including a real
  failed audit insert and rollback. Read logs and rows independently.
- Read merged CSV totals rather than inferring coverage from a green gate;
  all reports copied; no exclusion, waiver or lowered threshold.
- Traceability covers every AC, all twelve business rules and all test
  methods/invocations; GAPS entry states none for this slice.
- Evidence inspection assertions passed (`proof/qa-inspection-a922f49.txt`);
  live/committed OpenAPI diff empty; proof drop names items 1–12.
- All QA app runs stopped; localhost health connection refused afterward.
  Candidate worktree is clean at the exact candidate SHA; product/test
  sources untouched. Temporary baseline checkout removed; builder database
  preserved. Complete proof items 13–14 remain for their owning later steps.

## QA release judgment — merged 8e9c065

QA Agent/Codex, 2026-10-03; packet qitem-20261003153201-3bc62f5f.
Item 13's independent review evidence was accepted previously. The explicit
NFR-S4 review records and release secret-scan/environment records now exist.
I read their observed effects, independently repeated documented committed-tree
signature and literal-assignment screens, and checked the built/merged product
input equality. No credential identified within those methods; installed
shortUrl confirms URLSHORT_PUBLIC_BASE_URL. No universal, full-history,
untracked-file or image scan claim. Receipt proof/judgments/00000014.md accepts
item 14 against 8e9c065589e53385f60d6be3ddbc3683260285df; all 14 items derive
accepted/ready. Evidence and hashes: docs/qa/01-greenfield-core/release-judgments/;
additive QA drop proof/qa-release-8e9c065.md covers item 14.

### Self-check

Read exact contract, explicit independent review rows and raw installed smoke;
ran and inspected committed-tree screens, including variable-read matches;
compared merged/built product inputs and current evidence hashes; attributed
the receipt to the merged subject. No fresh build/app/benchmark or product edit
in this judgment task. Separate packaged dogfood is recorded at
docs/qa/01-greenfield-core/dogfood.md and all its apps were stopped.
Delivery stamp/ship gate and failed 03-operate AC-28 remain separate.
