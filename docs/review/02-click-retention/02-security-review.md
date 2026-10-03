# Security and compliance review — 02-click-retention

**PASS on X `a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1`; no findings.**
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03.
Same packet and 18/18-file coverage as [the code review](01-code-review.md).
Judged against design §6, architecture/database security guidance and the
extended checklist in `docs/guidance/review.md` §6.

The new input is operator configuration, not a public request. The new write
is a date-bound deletion of click rows; audit-column actors are static. Fresh
346-test gate and canonical-only 100% coverage passed. Evidence reconciliation
and scope are in [the audit](proof/evidence-audit-a8fc8b6.json); saved QA effects
are attributed to QA, not represented as a new review smoke.

## Checklist

Source paths below are relative to the exact candidate; saved QA effects are
under `missions/02-brownfield/slices/02-click-retention/proof/qa-effects-final-a8fc8b6/`.

| Item | Status | Evidence |
|---|---|---|
| Stored redirect target; http/https only | pass | Unchanged `link/RedirectController.java:52` uses the resolved stored URL. `LinkValidation.java:36` rejects other schemes; fresh inherited validation/redirect tests, QA 400 and 302 captures. Purge cannot alter links. |
| SSRF / server-side URL fetch | pass | No fetch introduced; source scan of `src/main` finds no HTTP client/openConnection path. Changed code invokes only the existing JDBC store. |
| Parameterized SQL | pass | `click/ClickStore.java:43` binds `LocalDate` as `:cutoff`; no request-derived SQL. V3 is static DDL/backfill. Existing parameterized reads/inserts unchanged. |
| Alias/code charset, length, reserved words and case | pass | No client alias input in this candidate (`CreateLinkRequest` has URL only). Generated codes are eight case-sensitive base62 characters using production SecureRandom; `ShortCodes` excludes reserved service segments; redirect route remains `[A-Za-z0-9]{6,32}`. `api`/`admin` cannot be generated at length eight; `actuator` is excluded. Fresh inherited tests pass. |
| Trusted-proxy rate limiting | pass | `web/RateLimitFilter.clientOf` trusts X-Forwarded-For only for configured immediate peers, then scans right-to-left; default trust list empty. Fresh limiter/filter journeys pass; QA captured 60 admissions then correlated 429. No limiter/header change in this delta. |
| PII, salt and log hygiene | pass | `DailySalt` retains daily in-memory HMAC-SHA256 keys; `ClickRecorder` reduces before queueing. New purge events contain only count/day/period/class, with no request/client fields. Rechecked 14 saved purge events and fault/UA canary absence; AC-12 WARN correlates to its redirect. V3 uses `anonymous`/`system`, not client identities. |
| Error leakage and semantics | pass | No new HTTP error path; unchanged advice returns ProblemDetail without throwable text. Fresh failure journeys plus saved 400/404/410/422/429 responses. Purge failures are class-only WARNs; startup failure-analysis event names only the rejected setting/value and origin. Existing startup JDBC URL logs remain outside AC-4 by the settled decision. 403 audit-read behavior belongs to the later base/X′, not X. |
| Headers / CORS | pass | Redirect keeps `Cache-Control: no-store`; no header/CORS change or permissive CORS configuration introduced. Fresh inherited HTTP suite passes; response captures retained. |
| Actuator and H2 console | pass | `application.properties` retains health/info/metrics/prometheus only, status-only health and no H2 console setting. No new management endpoint. The existing scrape disk-path residual is owned by `03-dogfood-fix`, not introduced here. |
| Dependencies and versions | pass | No dependency/build change. Fresh offline runtime inventory completed; direct versions listed below. Advisory freshness is explicitly unverified. |
| Retention / deletion / audit completeness | pass | AC-1–11/13/15/16 tests and saved installed-upgrade/clock/JDBC effects prove strict boundary deletion, failure recovery, explicit hold, row preservation and static audit values. Purge has no audit row by SPEC A-4; count event is its record. Data-subject deletion requests are explicitly out of scope. |
| Server-issued request ID | pass | `web/RequestIdFilter.java:44` creates a UUID and never reads inbound correlation headers. Fresh inherited canary tests pass; all 207 saved raw responses have matching server JSON request events/statuses. |
| Enumeration limits | pass | Unchanged eight base62 characters give 62^8 = 218,340,105,584,896 possible codes; shipped per-client budgets are 60 API and 600 redirect requests/minute. These impede guessing, not authorization; link visibility remains the specified public model. |
| API documentation exposure | pass | Existing `/v3/api-docs` and Swagger UI remain intentional public/operator surfaces in DESIGN/ADR-0010. No new profile or exposure. |
| Audit append-only and transaction boundary | pass | No new audit write path. Source scan finds no audit UPDATE/DELETE. AC-6 and the actual upgrade capture preserve all audit rows; existing link writes/audit transaction tests pass. |
| Secrets / container boundary | pass | All 18 changed files read: no credentials or secret value introduced. Unchanged Dockerfile runs uid 10001; Compose keeps read-only root, persistent data volume, ephemeral /tmp and loopback publication. Configuration inspected; no new image run or image-secret scan claimed. |

## Direct runtime dependency inventory

Fresh `../../scripts/gw --offline dependencies --configuration runtimeClasspath`
completed successfully; [complete output](proof/runtime-dependencies-a8fc8b6.txt).

| Dependency | Resolved version |
|---|---|
| Spring Boot starters: actuator, data-jdbc, flyway, validation, webmvc | 4.1.1 each |
| springdoc-openapi-starter-webmvc-ui | 3.1.1 |
| H2 | 2.4.240 |
| micrometer-registry-prometheus | 1.17.1 |

Existing remediation overrides remain Tomcat 11.0.25, Jackson 3.1.7 and Jackson
2.21.7. No newly known vulnerable dependency is identified by this delta review.
The offline sandbox review did not query advisory databases; this is not a
current vulnerability-clearance claim. Release reruns the network advisory
check and records reachability/remediation for each advisory.

## Findings and residual risks

None: MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No non-blocking repair
requested. The design's accepted residuals still apply: operator clock jumps or
a shorter period can delete data irreversibly; an explicit hold suspends
retention; a failed run waits for the next daily run; large V3 backfill plus
catch-up can delay readiness. Schema rollback cannot restore purged rows.
No new large-data, SIGTERM-during-purge, container or PostgreSQL claim is made.

This PASS covers X and the reviewed delta. The authorized X′ rebase must retain
audit-read's forwarding protections and pass the range-diff/fresh-gate/QA item-9
sequence in the companion review before integration and acceptance.

## Self-check

All required and extended checklist items have an evidence-based disposition.
New deletion and migration checked against the threat model; no client input
reaches the cutoff or audit actors; no stored value or driver message reaches
new events. Exact candidate and all current proof evidence hashes verified.
