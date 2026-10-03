# PROOF — OPR.99.0.3.1 Audit trail read

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.1 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

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

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003182724-0843aca3`.
This section is builder evidence, not a verdict.

**Candidate:** `35590f0` on `slice/01-audit-read` (`.worktrees/01-audit-read`). It sits on `main` `0df4841`; the branch was fast-forwarded from `f6dd29e` before the first commit.

| SHA | Commit |
|---|---|
| `62c47a7` | `test`: the five journey classes and `AuditControllerTest` (red). It includes the granted two-line `OpenApiDocumentTest` change (`slice.yaml` `428e9e1`, `qitem-20261003182833-40a842ff`). |
| `d183d0c` | `feat`: `AuditController`, `AuditTrail`, `AuditEntry`, `AuditPage`, the `package-info` text, and the `server.forward-headers-strategy=none` pin. Three test corrections, listed under the deviations. |
| `a47bee7` | `docs`: regenerated `docs/api/openapi.json` (192 lines added, 0 removed) and the granted README line |
| `35590f0` | `fix`: `@ConditionalOnWebApplication(SERVLET)` on the controller, because the non-web boot test could not start |

### Commands and outcomes

| Command | Outcome | Record |
|---|---|---|
| `compileTestJava compileFunctionalTestJava` on the test commit | red: `AuditController` and `AuditTrail` do not exist | `proof/builder-red-tests.txt` |
| the audit journeys and `OpenApiDocumentTest` with `AuditTrail` present but no controller | red by behaviour: 52 run, **47 failed** | `proof/builder-red-functional.txt` |
| `check --rerun-tasks` on `a47bee7` | **failed**: `UrlshortApplicationTests.mainBootsWithoutAWebServer` had no `ServerProperties` bean, so `35590f0` | `proof/builder-check-a47bee7.txt` |
| `scripts/gw --offline check --rerun-tasks` on `35590f0`, after the last code edit | BUILD SUCCESSFUL; **unit 200/200, functional 200/200**, 0 skipped; merged JaCoCo **492/492 lines, 190/190 branches**; `javadoc` green | `proof/builder-check-35590f0.txt` |
| AC-17: `f6dd29e`'s `src/functionalTest` checked out unchanged onto `35590f0` in a scratch worktree (the five new journeys removed; `git diff f6dd29e -- src/functionalTest` empty), `functionalTest` | 155 run, **exactly 2 failed**: `OpenApiDocumentTest.AC28_liveDocumentDescribesTheSlice` (line 71, exact path list) and `AC20_everyOperationDocumentsTheTooManyRequestsProblem` (line 116, six operations). Both are the granted enumeration assertions; every behavioural test passed unchanged | `proof/ac17-shipped-suite-on-35590f0.txt` |
| `git diff --stat f6dd29e slice/01-audit-read -- docs/api/openapi.json` | 192 insertions, 0 deletions: every earlier operation is byte-identical (AC-19's "unchanged") | the command's output |

### Verified by effect (real jars, real Tomcat, file H2)

- **AC-18, the upgrade (proof item 7).**
  - The jar built from `f6dd29e` wrote a fresh data directory: two links, one of them retired, and two clicks (one with a referrer). Its statistics read `totalClicks` 2. Its `GET /api/audit` is `404` (`proof/upgrade-0-shipped-f6dd29e.txt`). It was then stopped with SIGTERM.
  - The candidate jar started on the same directory. Flyway validated 2 migrations and applied none.
  - Statistics were unchanged (`upgrade-1`). The retired link answers `410` (`upgrade-2`). The active link answers `302` to the same `Location` (`upgrade-3`).
  - `GET /api/audit` returned all three pre-existing rows, newest first, as stored, with `next` `null` (`upgrade-4`).
- **The exchange (proof item 8).**
  - `GET /api/audit?limit=2` returned `200` with two rows and `next` `Mg` (`http-200-page1`).
  - Following `next` returned the last row with `next` `null` (`http-200-page2`).
  - The same request with `X-Forwarded-For: 198.51.100.9` and a canary `User-Agent` returned `403 {instance, status, title}` (`http-403-forwarded`).
  - Each of the three has one `request completed` line carrying its `requestId` and status (`log-lines-35590f0.txt`).
  - Across the whole run log (`jar-log-35590f0.txt`), a grep for `198.51.100.9`, the User-Agent canary, `example.com`, `"state"`, the cursor `Mg` and `127.0.0.1` matched 0 lines.

### Deviations from design.md, each with its reason

1. **`@ConditionalOnWebApplication(SERVLET)` on `AuditController`.** The design does not mention it. `ServerProperties` exists only in a web application, and the shipped `mainBootsWithoutAWebServer` test (`web-application-type=none`) could not start. Without a servlet application there is nothing to guard.
2. **`@Schema(implementation = Object.class, types = …)` for `before`/`after`, not `type = "object"`.** The design flagged this as unverified. On springdoc's OpenAPI 3.1 output, `type = "object"` was ignored and the fields were documented as strings. `types` alone added a `$ref` to a `JsonNode` component listing Jackson's getters. The chosen form renders `{"type": "object"}` and `{"type": ["object", "null"]}` with no `JsonNode` component, and AC-19's test now asserts it.
3. **`operationId = "readAudit"`**, in line with `createLink`/`readLink`/`retireLink`. Otherwise it would be the method name, `page`.
4. **The design's MockMvc override contexts (§7 a) became real-Tomcat contexts.** `native` and `framework` each start the application on `127.0.0.1` and send a plain request, `X-Forwarded-For: 127.0.0.2` and `Forwarded: for=127.0.0.2`: all `403`. This is stronger than MockMvc, which never runs Tomcat's valve. The **unset** strategy is covered by the unit test only. With the pin in the shipped file, no argument can unset it.
5. **A refused `HEAD` with no body is checked on Tomcat**, because MockMvc keeps a `HEAD` body (as found on `02-analytics`). AC-11's MockMvc test checks the status and that the body has no trail content.
6. **AC-13's trusted-proxy configuration is tested together with AC-14** in `AuditAccessSettingsJourneyTest`: trusted proxies `127.0.0.1,192.0.2.10`, raised budgets and a non-default public base URL. AC-13's default-configuration cases are in `AuditReadJourneyTest`.
7. **AC-9 `limit=0`**: the body is not searched for a lone `0`, because `400` contains it. The unit test checks that the static messages never carry a submitted value of two or more characters.
8. **Test corrections in `d183d0c`.**
   - The unit message check skips one-character values, because "must be from 1 to 100" contains `0`.
   - The upgrade journey reads statistics before redirecting, because its own redirect records a click.
   - AC-19 asserts that `before` and `after` are documented as objects.
9. **Impact analysis, *Test impact* "Changed: none" was wrong.** `OpenApiDocumentTest` enumerates paths and operations. The lead granted the two lines at 18:30Z; the AC-17 run above names them as the only failures.

### Not verified, and residual risks

- **Real IPv6 sockets.** `::1` and `::ffff:127.0.0.1` are covered by MockMvc peers and by the unit classification. The real servers are bound to `127.0.0.1`.
- **The rate limit on `/api/audit`** (rule 9, inherited) has no journey of its own. It is the `/api` classification of `03-operate`, which classifies `/api/audit` with no change.
- **Not mine:** QA's coverage copies, traceability, the `GAPS.md` row, the live-vs-committed document diff, and the security review's record (proof items 3–5, 10, 12). The ADR and the operator documentation are the design step's: ADR-0019 and `docs/DESIGN.md` §3.

## Self-check (builder)

- **Re-read the diff** `0df4841..35590f0`. Territory holds: `audit/` in all three source sets, one line of `application.properties`, `docs/api/openapi.json`, one README line, and the two granted lines of `OpenApiDocumentTest`. Nothing under `db/migration/`, `link/`, `click/` or `web/` main code.
- **Guard first.** `page` throws `403` before parsing `limit` or `cursor`. The mapping has no `produces`, so `Accept` cannot pre-empt it. Checked with `text/html`, `application/problem+json` and `*/*`: refused and invalid requests are problems, valid requests are `200 application/json`.
- **No submitted value in a body or log.** The messages are static. `limit` and `cursor` are bound as strings and parsed by hand. The by-effect grep above found 0 matches.
- **Every AC has a named test:**
  - AC-1 to AC-13, AC-15, AC-16, AC-19 and AC-20 in `AuditReadJourneyTest`;
  - AC-13 and AC-14 in `AuditAccessSettingsJourneyTest`;
  - rule 2's strategy and the `HEAD` body in `AuditForwardedHeadersJourneyTest`;
  - AC-18 in `AuditUpgradeJourneyTest`, and by effect;
  - AC-21 in `AuditReadFailureJourneyTest` (induction: a spy on `AuditTrail`);
  - AC-20's induction is a held JDBC transaction on a second connection;
  - AC-17 is the shipped-suite run above.

  The journeys were watched failing by behaviour (47 of 52 in `builder-red-functional.txt`). The unit tests were red only as compile failures (`builder-red-tests.txt`).
- **Gate:** `--offline check --rerun-tasks` ran after the last code edit, on `35590f0`.

### Rework — 7ac8af5 (code review CR-01)

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003201620-c74de839`.
**Candidate:** `7ac8af56ed04c27bbefbd416b3976c544d2f274a`, three commits on `35590f0`. It implements the
re-locked design response `0052efb` (§1 guard, §7 test (d)).

| SHA | Commit |
|---|---|
| `ea7e6f4` | `test`: unit cases for a remote-ip header, a protocol header and both empty. Real-Tomcat test (d) with each `server.tomcat.remoteip` setting under the shipped pin: plain and forged `X-Forwarded-For: 127.0.0.2`, against a stored canary row |
| `1fe1cbf` | `fix`: `AuditController` also takes `TomcatServerProperties`; `peerIsConnection` is strategy `NONE` **and** neither remoteip header has text |
| `7ac8af5` | `test`: test (d) sends `HEAD` as well as `GET`, as CR-01's required change lists |

| Finding | Answer | Evidence |
|---|---|---|
| CR-01 HIGH | **Fixed** as design `0052efb` §1 specifies. The guard is the negation of Boot 4.1.1's whole condition for installing `RemoteIpValve`. Either remoteip setting now closes the read for every request, as `native` and `framework` already did. The shipped pin with no remoteip setting keeps plain loopback reads at `200`. **Ceiling**, named in the code and the design's §6: the guard mirrors Boot's trigger list, so a Boot upgrade that adds a trigger must be added here. | Red on `35590f0`'s code: both settings failed (`proof/builder-red-cr01-remoteip.txt`; the plain loopback read answered `200` under each). On `7ac8af5`: test (d) gets `403` and no canary for plain and forged `GET` and `HEAD` under each setting. The earlier strategy cases (`native`, `framework`, platform plus pin) still pass, and the unit tests cover each `hasText` branch. |

| Command | Outcome | Record |
|---|---|---|
| `functionalTest --tests '*AuditForwardedHeadersJourneyTest*'` with test (d) on the `35590f0` code | 6 run, **2 failed** (both settings) | `proof/builder-red-cr01-remoteip.txt` |
| `scripts/gw --offline check --rerun-tasks` on `1fe1cbf` | green: unit 203, functional 202, 494/494 lines, 194/194 branches | `proof/builder-check-1fe1cbf.txt` |
| `scripts/gw --offline check --rerun-tasks` on `7ac8af5`, after the last edit | BUILD SUCCESSFUL; **unit 203/203, functional 202/202**; merged **494/494 lines, 194/194 branches**; `javadoc` green | `proof/builder-check-7ac8af5.txt` |

**Not re-run on `7ac8af5`, by me:**
- The AC-17 shipped-suite run and the two by-effect jar captures (items 7 and 8).
- The fix only narrows when the read is admitted. No path, response shape, query or log line changed, and the shipped configuration (no remoteip setting) still admits loopback reads, which the gate's journeys show.
- QA re-runs the exact candidate.
- The `HEAD` cases were added after the fix, so their red on `35590f0` comes from the review's control (`docs/review/01-audit-read/proof/code-controls-35590f0.txt`), not from my run.

## QA

### Re-check 7ac8af56ed04c27bbefbd416b3976c544d2f274a

Independent Codex QA, 2026-10-03, packet qitem-20261003202154-65e6d10f,
instance01M416ZY5N11CDGZBM2DT4GAXS. **PASS within the locked SPEC and the
accepted AC-17 grant.** QA-AUD-01/CR-01 is fixed by independent installed-jar
reproduction. Product worktree HEAD equals the named candidate and is clean.
No product code, build file or tests edited by QA.

Fresh offline check --rerun-tasks: **203 unit / 202 functional**, no
failures/errors/skips, all14 tasks executed, Javadoc green. CSV totals:
unit438/494 lines (88.66%),188/194 branches (96.91%); functional457/494
(92.51%),162/194 (83.51%); merged494/494 and194/194 (100% each). All348 copied
HTML/XML/CSV report hashes verified. Summary:
docs/qa/coverage/01-audit-read/SUMMARY.md. Gate log:
proof/qa-check-7ac8af5.txt. All following fresh captures live under
proof/qa-recheck-7ac8af5/; earlier35590f0 evidence remains historical.

| Acceptance criteria | Independently observed effect |
|---|---|
| AC-1 | Empty200 application/json, exact items[]/next:null. |
| AC-2,3,4,5 | Create A/B/C, retire A; four rows in retireA,C,B,A write order; all eight fields equal actual SQL rows, create/retire request IDs and before/after content agree; create time lies within client interval. |
| AC-6 | Three20/20/5 pages via returned next, final null; every field equals all45 SQL rows in descending identity order while seeded times run oppositely. |
| AC-7 | Seed150: default50 and maximum100, both with next. |
| AC-8 | Seed30, first10, five real HTTP creates, continuation returns only original30 once; fresh35 starts with five new writes. |
| AC-9 | limit0/101/ten and cursor*** each400 with one static field/rule, also negative cursor and overflowing limit; strict Accept still safe problem. |
| AC-10 | Traverse45 twice; POST/PUT/PATCH/DELETE405. Three stable intervals compare all audit/link/click rows byte-equal before/after; no read audit rows. |
| AC-11,12 | Controlled192.0.2.10/10.0.0.7 GET/HEAD403; four loopback representations200; no forbidden trail/client echo. |
| AC-13,14 | Default/trusted-proxy profiles refuse required forwarded inputs before validation. Installed jars prove both explicit Tomcat header triggers and native/framework close plain/forged GET/HEAD403 without canary; default/empty settings plain200, forwarded403. Six configuration variants each contain an actual created audit row. Non-default public base, budgets and trusted proxies do not open the read. |
| AC-15,16 | 23 repeated obs- requests match24 JSON events in unchanged default console and ECS file sink; 200/400/403/405/500 covered. Whole-run logs in both capture sets omit audit/client/cursor/SQL canaries. Original plain-file capture limitation is disclosed below. |
| AC-17 | Original25 shipped files equal f6dd29e blobs. Replay155 tests:153 pass, only two expressly authorized enumeration failures; candidate versions pass. Grant428e9e1, transition1156. No claim of155 unchanged assertions green. |
| AC-18 | Shipped immutable f6dd29e jar writes fresh H2 directory: active/retired links,3 clicks,3 audit rows. Stop, candidate jar on SAME directory; statistics captured before verification GET remain exactly equal, audit/link columns preserved,302 same target and410. Only verification GET adds fourth click. |
| AC-19 | Whole key-sorted live document from fixture and installed candidate equals candidate docs/api/openapi.json; every shipped path/response/example unchanged; audit queries, row/page example,200/400/403/429/500 documented. |
| AC-20 | Actual JDBC INSERT held uncommitted after30 fixture rows; first10 read, transaction committed, continuation original30 once; fresh31 includes held write first. |
| AC-21 | External DataSource wrapper throws actual SQLException during audit-read preparation. Safe500 with no page, SQL/message/cursor/audit content; next read after removing flag200. Repeated JSON error/completion events correlate to response ID. |

Raw HTTP and shipped XML bytes are retained in hashed archives; displayed
headers/HEAD bodies and failure XML normalize only line endings/trailing
whitespace, independently checked against originals.

258 HTTP captures and1,482 reconciliation assertions are recorded in
verification.json; all raw curl headers/bodies/requests are archived and
their bytes/hash checked. Installed jar provenance:
candidate SHA25648fca1b85ef5e78f0b74e15b5f24d9f612bab544bc53dd6b4d5cba46f9b28a7f;
baseline SHA25661d9ce3417be75abdcac463f2171409905ceb6be6ef0f4d988378c9d11ff9377.
Installed-launch-arguments.json names each profile. Exact actual exchange:
obs-exchange-page1/page2/samepage-forwarded returns200/200/403 on two pages
via next and the IDENTICAL second-page URI. Default JSON console and file
events independently match these response IDs/statuses.

All227 source test methods map both ways in TRACEABILITY (103 unit,
124 functional), including all21 ACs and9 business rules; full original
class XML plus invocation inventory retained. Artifact provenance verifies
impact analysis precedes design, indexed ADR0019 precedes dependent code,
re-lock0052efb precedes fix1fe1cbf, empty migration diff and documented
loopback/local-relay boundary. No separate design approval is invented here;
the lead re-lock stands and independent reviewer evaluates its delta.

**Limits and capture correction.** The external QaLauncher controls only
fixture rows, actual JDBC held-write/failure inputs and Servlet peers,
as SPEC authorizes; real remote TCP peers, natural disk failures, sustained
concurrent stress, Docker and future Boot forwarding triggers were not
tested. The preserved immutable shipped jar is reused with its prior
source-product provenance and rechecked hash; it was not rebuilt from a
possibly newer main. No migration was added, so rollback execution is N/A.

QA initially added logging.file.name without an ECS file encoder. The first
file sink was plain text while the default console was JSON; original
captures are retained under plain-file-captures/. QA repeated observability
and the installed two-page/forwarded-page exchange with
logging.structured.format.file=ecs, keeping the default console unchanged.
Those23 requests and all24 request events match in both retained default
console and file logs. Earlier235 exchanges are not claimed to have
retained JSON correlation. Privacy canary checks cover all whole-run logs.
GAPS records the correction, per-suite misses, accepted enumeration grant,
input scope and Boot-trigger upgrade ceiling. No material AC remains
unverified within its specified scope.

Items1–11 and13 are covered by this QA drop and will receive fresh
commit-subject judgments. **Item12 remains pending** corrected independent
security review, under existing lead sequencing obligation
qitem-20261003194346-b74b8081. The earlier35590f0 rejection is preserved.

#### Self-check — re-check7ac8af5

- EveryAC exercised by effect with AC-17's exact recorded grant; invalid input,
  duplicates/conflict, missing/retired links, wrong methods, remote/forwarded
  denial, both Tomcat trigger overrides, rate429 and SQL500 tried.
- Coverage read from merged CSV, all copied hashes checked; all227 methods
  mapped, functional evidence for everyAC, gap entry written.
- Actual installed same-directory upgrade, exact live/committed API equality,
  identical page URI403 and default JSON console correlation inspected.
- Proof drop names items1–11,13; independent security item12 deferred honestly.
- All app ports18131/18132/18133/18134 refuse connections; worktree remains
  clean at exact candidateSHA. No product/build/test edits.

### Historical QA — 35590f0 (superseded)


**Correction on candidate35590f0:** the original PASS below is superseded.
Review found HIGH CR-01 and QA independently reproduced both explicit Tomcat
RemoteIpValve overrides with the original jar: forwarded GET200 with audit
canary and HEAD200, default403 control. AC-13/14 are unmet. See
docs/qa/01-audit-read/findings.md and post-review-remoteip/verification.json.
The suite/coverage/upgrade/read-only results below remain historical facts;
they do not establish the complete settings boundary. New candidate7ac8af5
is handed back in qitem-20261003202154-65e6d10f and requires a fresh QA check.

Independent Codex QA, 2026-10-03. Packet `qitem-20261003185423-545a1365`,
instance `01M416ZY5N11CDGZBM2DT4GAXS`. Product worktree HEAD was and remains
`35590f06c852543c29097a42c43b7802be90ba40`, clean. No product, build file or
test edited by QA. Verdict: **PASS within the locked SPEC and recorded AC-17
grant**. The independent security-review record (contract item 12) is still
downstream; lead obligation `qitem-20261003194346-b74b8081` routes it back for
its judgment before acceptance. Items 1–11 and 13 are covered here.

### Fresh gate and coverage

Ran `../../scripts/gw --log ../../missions/02-brownfield/slices/01-audit-read/proof/qa-check-35590f0.txt --offline check --rerun-tasks`
in the exact candidate worktree. Exit 0, all 14 tasks executed, Javadoc green,
200 unit and 200 functional invocations, zero failures/errors/skips.
Read the per-class CSV: unit 436/492 lines and 184/190 branches; functional
455/492 and 158/190; merged **492/492 lines, 190/190 branches**. All 348 report
files copied byte-for-byte to `docs/qa/coverage/01-audit-read/{unit,functional,all}`
and independently reconciled against `qa-report-copy-hashes-35590f0.json`.
Per-suite percentages are reported, not represented as separate 100% gates.

Replayed the original `f6dd29e` functional sources via an external Gradle init
script, compiling unchanged sources against candidate classes. Git-blob
SHA-256 checks cover all 25 original files. Actual result: **155 invocations,
153 pass, exactly the two granted OpenApiDocumentTest enumeration failures**,
zero errors/skips. Their candidate versions pass in the full 200-test gate.
Grant: `qitem-20261003182833-40a842ff`, transition 1156, commit `428e9e1`.
`qa-shipped-suite-35590f0.txt`, original XML, source hashes and summary are the
evidence; no claim that all 155 original assertions pass unchanged.

Exact original shipped-suite XML is archived in
`qa-shipped-xml-raw-f6dd29e.tar.gz` with its hash manifest; displayed copies
only strip trailing line whitespace from Gradle's failure-message text.

### Observed acceptance criteria

Every request used loopback-only `scripts/http` curl. Headers, bodies, sent
JSON and request metadata are under `proof/qa-http/` and the ledger. Exact
original curl bytes are archived in `qa-http-raw-35590f0.tar.gz`, with SHA-256
manifest; displayed header files only have CRLF/trailing blank normalization.
HEAD captures use curl's HEAD mode and contain headers alone, never JSON.

The installed jar's `qa-exact-exchange-35590f0.json` captures two pages via
next and repeats each identical page URI with a forwarding header: both
repeats return403. Their four request IDs correlate with JSON events in the
candidate-upgrade log; the final app was stopped and its port refused.

Attributed judgments: receipts 1–12 accept contract items 1–11 and 13 against
commit 35590f06c852543c29097a42c43b7802be90ba40, preserved in
`qa-judgment-receipts-35590f0.json`. All evidence hashes were checked after
recording. Item 12 remains pending under the downstream sequencing obligation.
Evidence commits: `29a141e` and `63e04b5`.

| AC | Effect independently observed | Evidence |
|---|---|---|
| 1 | Empty store: 200, exactly items=[] and next=null | fixed-ac01-empty |
| 2 | Real create: exact eight audit fields, anonymous/link.create/link, before=null, after target and active state; audit UTC instant within curl interval and requestId equals create header | fixed-ac02-create-A/B/C; qa-live-real-before.json |
| 3 | Real retire: before active, after retired, requestId equals retire header; original create follows | ac03-retire-A; ac04-order-read-1 |
| 4 | Retire A, create C, B, A; seeded timestamps move backwards while rows remain ordered by increasing write position, not wall-clock time | ac04-order-read-*; ac06-page-* |
| 5 | All eight fields compared to every stored row, including parsed before/after; no numeric store id exposed | real snapshots and all 45-row traversals; qa-verify-35590f0.py |
| 6 | 45 committed rows:20,20,5, unique and newest-first; last next=null | ac06-page-1/2/3; seed45 snapshots |
| 7 | 150 committed rows:default50 and limit100, both next non-null | ac07-default/max |
| 8 | First10 of original30, five real creates, continuation returns all and only originals once; fresh35 begins with the five new rows | ac08-first/new-*/continuation-*/fresh; qa-ac08-before.json |
| 9 | 0/101 range errors, ten/overflow format errors, malformed/nonpositive cursors format errors; one static field/rule message, 400 problem regardless of Accept | ac09-* |
| 10 | All45 traversed twice; POST/PUT/PATCH/DELETE405. Audit/link/click snapshots identical before and after. GET/HEAD/OPTIONS behavior inspected separately | ac10-pass*/allpages-*; qa-readonly-before/after.json; head-local/options-local |
| 11 | Controlled192.0.2.10 and10.0.0.7:GET/HEAD403, no rows, counts, peer or audit content | ac11-* |
| 12 | Controlled127.0.0.1/.2, ::1 and ::ffff:127.0.0.1 all200 | ac12-* |
| 13 | All required forwarding cases refused under default and trusted-loopback settings; empty/case-varied headers also refuse; guard precedes invalid query and HTML Accept | ac13-*; settings-* |
| 14 | Nondefault base URL, raised budgets, trusted loopback plus remote peer:plain local200, remote/forged GET/HEAD403. Native/framework strategy overrides also403 on real Tomcat | settings-*, native-*, framework-* |
| 15 | All271 captures have server-issued X-Request-Id; all276 JSON request events reconcile to headers and completion status, including200/400/403/405/429/500 | qa-request-log-lines-35590f0.json; qa-verification-35590f0.json |
| 16 | Target/query, stored payload, cursor, User-Agent, peer, forwarding and induced-SQL-message canaries absent from whole-run JSON logs; target remains in allowed audit response | qa-verify-35590f0.py; qa-*.jsonl |
| 17 | Original155-test replay has153passes plus exactly the two expressly granted enumeration failures; old live OpenAPI operations, parameters, responses and examples unchanged | qa-shipped-*; qa-api-diff-35590f0.txt |
| 18 | Actual shipped jar creates active/retired links, three clicks and three mutation rows; stop; candidate jar uses identical H2 directory. All stored link/audit columns preserved, full statistics2/1 unchanged before verification, active302 exact Location, retired410; HEAD no click, verificationGET adds only fourth click | qa-upgrade-before/after.json; qa-http/upgrade-*; qa-artifact-provenance-35590f0.json |
| 19 | Entire candidate live document equals committed candidate JSON; audit limit/cursor, example, 200/400/403/429/500 documented; all existing operation objects equal shipped live document | live-api-document, upgrade-candidate-api; qa-api-diff-35590f0.txt |
| 20 | Real JDBC transaction held uncommitted after30 originals; read10, commit, continue all originals exactly once, held row at most once; fresh31 contains it first | ac20-*; qa-ac20-before/after.json; qa-instrument-QaLauncher.java |
| 21 | Actual SELECT preparation throws SQLException; 500 problem contains only instance/status/title, no page/content/cursor/SQL/class; correlated events omit exception message; removing fault gives200 | ac21-store-failed/recovered; qa-live-correct-35590f0.jsonl |

Rule 9 also observed independently: a rapid 90-read audit burst in 0.17s
admitted the first 60 (200), then refused the remaining 30 (429) with positive
Retry-After. A preceding paced 61-read probe allowed continuous GCRA refill;
it is retained, not misrepresented as an exhausted-bucket check. Duplicate
idempotent create returned201 with the same code; conflicting key422, bad
URL400, missing redirect404 and retired410 were also observed.

### Instruments, artifact checks and limits

The controlled app runs the unchanged candidate classes on real Tomcat with
the disclosed external `QaLauncher` and Gradle init script. The filter supplies
per-request Servlet peer inputs (SPEC-authorized); the DataSource wrapper
arranges fixture resets, a real held JDBC write and a deliberate SQLException.
They supply no new product endpoint or product code. Fixture resets are
explicit setup; read-only claims compare stable before/after intervals.
The installed upgrade uses the two unmodified jars, with no QA filter.

Both jars were built through `scripts/gw --offline bootJar`; root product/build
inputs were independently compared to `f6dd29e` before using its jar. Saved jar
hashes and sizes are in `qa-artifact-provenance-35590f0.json`. The migration diff
against shipped V1/V2 is empty. Impact analysis was first added at `a686b2a`,
before design and ADR0019 at `c1be728`, before dependent implementation;
ADR0019 was already indexed in DESIGN at that commit. Its modules/endpoints/
schema/flows/blast-radius/compatibility/risk rows were read. Its outdated
"tests unchanged" claim is explicitly superseded by the named enumeration
grant. README names loopback-only/no-opening-setting; DESIGN §3 states the
headerless local-relay boundary and not to relay or to add a refused header.

Not checked: actual non-loopback TCP clients, actual IPv6 sockets, headerless
local relays, natural hardware/OS store failures, crash recovery, broad
concurrency/latency, or container upgrade. Controlled peer evidence is qualified
to the address and header inputs the service receives, as the SPEC requires.
No authentication or end-to-end proxy guarantee is claimed. Security item12
requires the separate review, not this QA authorship.

Initial instrument failures are retained in GAPS and raw evidence: no-source
test setup and unsupported QA H2 case folding. Both were corrected outside
product and supplied no passing proof. The reconciler's date-format handling
and old in-memory ledger contamination were corrected against raw files;
the current slice ledger is independently reconciled, not assumed complete.

## Self-check (QA)

- Read the builder proof, then ran the full candidate gate and each AC's effects
  independently; checked all new failure paths and inherited bad-input,
  duplicate, retirement and rate-limit paths.
- Read merged CSV totals directly; verified all348 copied report hashes.
- Traceability maps all223 named source methods both ways, all21 ACs and
  rules1–9, with FR/NFR ids and explicit AC20/21 induction mechanisms.
- GAPS entry records per-suite misses, accepted AC17 enumeration scope,
  controlled-peer/JDBC limits and downstream item12; no merged shortfall.
- Reconciled all271 raw HTTP captures with276 request events and whole-run
  privacy canaries; 1,641 durable verification assertions passed.
- Proof drop names items1–11 and13. Item12 remains unjudged under the durable
  sequencing obligation, not counted as this step's security-review proof.
- All QA apps stopped gracefully; localhost ports18131/18132/18133 refused
  connections afterwards. Worktree remains clean at the exact candidate SHA.
