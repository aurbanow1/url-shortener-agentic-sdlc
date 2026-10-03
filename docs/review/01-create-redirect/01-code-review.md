# Code review — 01-create-redirect

- Candidate: `a922f49144049db0228c316c474ac6e890742fa5`.
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003074021-e3821d80`; instance `01M40149S2BGCAKVK83B6VCNKG`.
- **Verdict: PASS with one MEDIUM follow-up (CR-01); no blocking findings.**
- Security is reviewed in `02-security-review.md` on the same candidate and packet.

## Context proof

The outcome is a Creator's create/read/retire API and a Visitor's stored-target
redirect, with 24-hour idempotency and atomic audit. Invariants: targets remain
verbatim after ordered validation; no request header determines the public base;
retirement is one-way; a rejected retry never changes an existing key binding;
every mutation and audit insert share a transaction; errors and logs disclose no
submitted values. Confidence: 97% in these contracts and their implementation;
no PostgreSQL, load or packaged-release claim.

Primed from the approved slice SPEC and unchanged reviewed design, AGENTS.md,
docs/DESIGN.md, accepted ADRs, review/Java/QA guidance and the design threat model.
The addressed project → mission → slice path selects this combined review.
Reviewed the producer's six declared deviations in PROOF.md: the empty
constructors preserve behavior (territory accepted by the lead's packet note);
API export-by-copy and API version 1 remain deterministic; the host check
removes an unreachable case; the bounded cold-start wait and real database
failure injection exercise the intended behavior. None broadens product scope.

Worked in `.worktrees/01-create-redirect`, clean at the exact QA candidate
before and after verification. `git diff main...slice/01-create-redirect`
contains 48 files, 3,475 insertions and two deletions. Five feature-branch
commits start with dependency overrides, then the Javadoc gate, before the
feature. The required prior override-only gate has an independent QA capture;
the ADRs predate dependent feature commits. Product code/tests were read-only.

## Complete file ledger

Read the full diff and every changed file: **48/48**. Public production types
and methods carry useful contracts; the relevant rows name their content.

| File | Verdict |
|---|---|
| `build.gradle.kts` | Pass: required patch overrides first; full coverage and Javadoc gates retained |
| `docs/api/openapi.json` | CR-01 MEDIUM: error extension schema incomplete; AC-28 content and live equality pass |
| `src/functionalTest/java/dev/urlshort/audit/AuditJourneyTest.java` | Pass: exact audit rows, rollback, and immutable prior snapshots |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` | Pass: bounded suite-only offset clock, reset after each idempotency test |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClockConfig.java` | Pass: distinct primary bean name; no production packaging |
| `src/functionalTest/java/dev/urlshort/link/IdempotencyJourneyTest.java` | Pass: replay, preserved mismatch binding, 24-hour boundary and invalid keys |
| `src/functionalTest/java/dev/urlshort/link/LinkCreateJourneyTest.java` | Pass: AC-1–7, exact response shape, validation table and route regression |
| `src/functionalTest/java/dev/urlshort/link/LinkReadRetireJourneyTest.java` | Pass: current state, one-way retirement, 404/405 contracts |
| `src/functionalTest/java/dev/urlshort/link/PublicBaseUrlJourneyTest.java` | Pass: configured base despite Host; actual environment spelling also captured by QA |
| `src/functionalTest/java/dev/urlshort/link/RedirectJourneyTest.java` | Pass: verbatim target, no-store, browser 410 and ignored incoming query |
| `src/functionalTest/java/dev/urlshort/web/ColdStartJourneyTest.java` | Pass: first real request, bounded completion wait and every-event correlation |
| `src/functionalTest/java/dev/urlshort/web/ObservabilityJourneyTest.java` | Pass: twelve status windows, input canaries, real driver error and rollback |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | Pass for AC-28 and deterministic equality; CR-01's schema shape is outside its assertions |
| `src/main/java/dev/urlshort/UrlshortApplication.java` | Pass: entry-point contract; explicit empty constructor preserves behavior |
| `src/main/java/dev/urlshort/audit/AuditLog.java` | Pass: sole parameterized INSERT; public append contract states transaction and null-before semantics |
| `src/main/java/dev/urlshort/audit/package-info.java` | Pass: write-only audit boundary documented |
| `src/main/java/dev/urlshort/link/CreateLinkRequest.java` | Pass: one nullable input, ordered validation explained |
| `src/main/java/dev/urlshort/link/Link.java` | Pass: immutable aggregate, state derived from retirement |
| `src/main/java/dev/urlshort/link/LinkConfig.java` | Pass: UTC millisecond clock and SecureRandom, no extra infrastructure |
| `src/main/java/dev/urlshort/link/LinkController.java` | Pass behavior: validate before mutation, configured base; CR-01 originates in generic error-schema annotations |
| `src/main/java/dev/urlshort/link/LinkProperties.java` | Pass: safe public-base default and environment setting |
| `src/main/java/dev/urlshort/link/LinkRepository.java` | Pass: selective repository API, parameterized targeted updates |
| `src/main/java/dev/urlshort/link/LinkResponse.java` | Pass: exactly five public response fields |
| `src/main/java/dev/urlshort/link/LinkService.java` | Pass: transactions, preserved key bindings and conditional retire |
| `src/main/java/dev/urlshort/link/LinkSnapshot.java` | Pass: only required audit state |
| `src/main/java/dev/urlshort/link/LinkValidation.java` | Pass: fixed rule precedence, ASCII URI/host/userinfo checks and key bounds |
| `src/main/java/dev/urlshort/link/RedirectController.java` | Pass: stored-only String Location and no-store; no fetch |
| `src/main/java/dev/urlshort/link/ShortCodes.java` | Pass: SecureRandom draws, reserved redraw and constraint-owned uniqueness |
| `src/main/java/dev/urlshort/link/package-info.java` | Pass: feature and neighboring-package boundaries explicit |
| `src/main/java/dev/urlshort/ping/PingController.java` | Pass: public endpoint contract documents log behavior; empty constructor only |
| `src/main/java/dev/urlshort/ping/PingResponse.java` | Pass: public record documents exact shape and UTC instant |
| `src/main/java/dev/urlshort/web/OpenApiConfig.java` | Pass: fixed server prevents request-derived document drift |
| `src/main/java/dev/urlshort/web/ProblemDetailsAdvice.java` | Pass: one platform advice, no reflected detail/instance or throwable messages |
| `src/main/java/dev/urlshort/web/Problems.java` | Pass: static field/rule/message; public factories and FieldError contract documented |
| `src/main/java/dev/urlshort/web/RequestBodyLimitFilter.java` | Pass: one counting stream, single/bulk paths, MVC-rendered limit error |
| `src/main/java/dev/urlshort/web/RequestIdFilter.java` | Pass: public contract states ordering/MDC cleanup; completion status only |
| `src/main/java/dev/urlshort/web/package-info.java` | Pass: shared HTTP concerns and feature boundary documented |
| `src/main/resources/application.properties` | Pass: eager servlet, safe logging, no multipart, limited exposure and fixed base |
| `src/main/resources/db/migration/V1__create_link_and_audit_log.sql` | Pass: unique constraints, bounded columns, timestamp types and destructive rollback warning |
| `src/test/java/dev/urlshort/audit/AuditLogTest.java` | Pass: required NFR-A2 API/SQL assertion plus real H2 persistence |
| `src/test/java/dev/urlshort/link/LinkServiceTest.java` | Pass: branching rules, boundary at exactly 24 hours, no-write rejection |
| `src/test/java/dev/urlshort/link/LinkTest.java` | Pass: active/retired states and audit snapshot |
| `src/test/java/dev/urlshort/link/LinkValidationTest.java` | Pass: rule ordering, CRLF, credentials, lengths and ASCII boundary |
| `src/test/java/dev/urlshort/link/ShortCodesTest.java` | Pass: alphabet/length and deterministic reserved-word rejection |
| `src/test/java/dev/urlshort/web/ProblemDetailsAdviceTest.java` | Pass: wrapped limit, safe body fields, class-only diagnostic metadata |
| `src/test/java/dev/urlshort/web/ProblemsTest.java` | Pass: status and exact field-error extension |
| `src/test/java/dev/urlshort/web/RequestBodyLimitFilterTest.java` | Pass: boundary on both read paths and non-resettable count |
| `src/test/java/dev/urlshort/web/RequestIdFilterTest.java` | Pass: server-issued id, status-only completion and cleanup on failure |

## Findings

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| CR-01 | MEDIUM | `docs/api/openapi.json:52`; `src/main/java/dev/urlshort/link/LinkController.java:68` | A live invalid create returns top-level `errors[{field,rule,message}]` and no `properties` member (`proof/http-a922f49.txt`, invalid-target). The live and committed ProblemDetail schemas instead describe a generic `properties` object and omit `errors` (`proof/live-openapi-a922f49.json`). Clients deriving typed error models cannot discover the field/rule contract. The builder disclosed this limitation; AC-28's required paths, statuses, media types and examples still pass. | Describe the actual validation extension in the generated API schema and assert its field/rule/message shape for 400/422. This can be backlog for the next API-document change; no runtime layer or dependency is needed. |

No MUST-FIX/HIGH. CR-01 is a documentation follow-up, not a reason to repeat
implementation now. No other item is expected to be fixed in passing.

## Ponytail review

Lean already. Ship.

The platform repository/advice, records, one audit writer and counting stream
each serve a named contract. No dependency was added. The two `// ponytail:`
ceilings (collision and concurrent-key loser → 500, retry by client) match the
accepted ADRs; they are not findings.

## Verification and audit of QA evidence

Fresh command in the candidate worktree:

```sh
scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/01-create-redirect/proof/code-check-a922f49.txt --offline check --rerun-tasks
```

Passed, all 14 tasks executed: **72 unit and 87 functional invocations**,
zero failures/errors/skips, Javadoc green; merged **185/185 lines and 56/56
branches**. Compared every CSV row in each of QA's three committed reports to
the fresh output: identical. No exclusion or threshold change. Source inventory
finds 42 unit and 45 functional methods; each is in TRACEABILITY.md, which maps
all 28 ACs and twelve business rules. Recorded totals:
`proof/evidence-audit-a922f49.json`.

| Contract group | Verification |
|---|---|
| AC-1–7; BR-1–4/11/12 | Create journeys cover exact five-field output, configured/default base, all validation inputs, malformed bodies, multipart and 16 KiB boundary; validation/code unit tests cover token precedence, CRLF and reserved redraw |
| AC-8–16; BR-2/6/7/8 | Read/retire/redirect journeys assert preserved values, one-way state, repeat/unknown/wrong-method semantics, stored Location, no-store and existing-surface regression |
| AC-17–21; BR-5 | Idempotency journeys assert replay identity, no extra writes, binding preserved after mismatch, 24-hour boundary and rejected-first-use retry; unit test fixes the exact boundary |
| AC-22–25; BR-9 | Real migration/JDBC journeys prove audit content, rollback and immutable historical rows; the unit writer test covers the required API/SQL restriction |
| AC-26–27; BR-8/10 | Twelve response/log windows, five input canaries, framework reflection inputs, real H2 duplicate-key failure and one first real-server request all pass |
| AC-28; NFR-M3 | Required paths/statuses/media/examples pass and generated document equals committed bytes; CR-01 records the extension-schema limit separately |

Audited QA's SUMMARY, TRACEABILITY, GAPS and PROOF narrative; sampled raw HTTP
captures including environment override and actual rejected audit insert.
Independently compared exported data: all 16 audit and 15 link rows unchanged
after rollback; final data preserve the audit rows with exactly three additions
(and two new links). Parsed QA's live document and compared it to the candidate:
equal. These are checks of QA's stored evidence, not a second live execution of
its file-database experiment. QA's absence of a simultaneous-race, PostgreSQL
or packaged-artifact claim is honest.

My separate loopback/in-memory-H2 run observed read, stored redirect, replay,
retire, browser 410, safe 400/405/415, Actuator limits and absent H2 console.
All eleven saved response ids have matching completion events; runtime logs
contain none of the sent canaries. The live document matches the candidate.
Commands/responses and parsed checks are in `proof/http-a922f49.txt`,
`proof/runtime-a922f49.txt`, and `proof/live-inspection-a922f49.json`.
The initial create preceded the saved exchanges and is represented by the
same resource's read/replay and runtime event. The app was stopped; connection
to port 18101 then refused. The bootRun record ends in exit 143 because I sent
SIGTERM for cleanup, not because the app or quality gate failed.

## Explicit proof-contract records

| Requirement | Verdict and evidence |
|---|---|
| **NFR-A2 / proof item 13** | **PASS. No application code path updates or deletes an audit row.** The only production statement on audit_log is the parameterized INSERT in `src/main/java/dev/urlshort/audit/AuditLog.java:25`, executed at :59; source scan in `proof/source-check-a922f49.json`. `src/test/java/dev/urlshort/audit/AuditLogTest.java:36` asserts that append is the only public operation and the only SQL constant is INSERT with no UPDATE/DELETE; its second test writes/reads real H2 rows. Both passed in my fresh unit run; AC-25 and QA's before/final snapshots prove unchanged prior rows through the API. |
| **NFR-S4 / review portion of proof item 14** | **PASS for this candidate review: no committed credential, token or private key identified.** All changed files read and committed-tree secret-pattern screen clean (`proof/source-check-a922f49.json`); the embedded database's blank password is an explicit local default. `LinkProperties.java:14` and `application.properties:4` set `http://localhost:8080`; `URLSHORT_PUBLIC_BASE_URL` overrides it, demonstrated in QA's `proof/qa-configured-base-a922f49.txt` with `https://sho.rt` despite spoofed headers. My run and both AC-3 tests confirm the configured/default base behavior. The release-prep secret scan is still required; this row does not close that later obligation. |

## Self-check and handoff

Exact QA SHA verified, complete 48-file ledger, fresh full gate and live checks,
severity/evidence attached, and both review rows added to REVIEW-LEDGER.md.
No product/test/SPEC/design edits; candidate worktree remains clean. Hand off
to integrate with CR-01 as backlog and the release secret/advisory scans still
owned by release. NFR-A2 is recorded for QA's item-13 judgment; item 14 still
needs release evidence under `qitem-20261003072643-917956c7`.
