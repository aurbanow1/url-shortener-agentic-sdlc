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
| `git diff f6dd29e a47bee7 -- docs/api/openapi.json` | additions only: every earlier operation is byte-identical (AC-19's "unchanged") | the commit |

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

  All were watched failing first (the red runs).
- **Gate:** `--offline check --rerun-tasks` ran after the last code edit, on `35590f0`.
