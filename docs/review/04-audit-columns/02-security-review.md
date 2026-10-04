# Security and compliance review — 04-audit-columns

**PASS, no findings.** Candidate `305f8045d45b19a9e3287d5fe3508af6e04db9a4`. Reviewer `review2-agent@urlshort-factory` (Codex), 2026-10-04 UTC. Same combined packet and 10/10 changed files as [01-code-review.md](01-code-review.md); one workflow exit covers both reviews.

V4 adds row-audit metadata to two existing tables. The only runtime changes stamp the three existing link writes inside their current transactions. No new endpoint, client input, outbound operation, log, metric or dependency is introduced. The design's integrity/disclosure/append-only/migration-cost threats match that scope. All changed files and relevant inherited security boundaries were read; confidence is high for this delta, subject to the explicit limits below.

Evidence abbreviations: **gate** = [fresh reviewer gate](proof/code-check-305f804.txt), 218 unit + 233 functional with 100% merged coverage; **audit** = [independent reconciliation](proof/evidence-audit-305f804.json); **QA run** = `missions/02-brownfield/slices/04-audit-columns/proof/qa-305f804/run-20261003T234116500899Z`. Source paths below are relative to the exact candidate's `src/main/` unless stated otherwise. The full changed-file ledger is in the companion code review.

| Checklist item | Status | Evidence and scope |
|---|---|---|
| Redirect target and scheme allow-list | pass | `java/dev/urlshort/link/RedirectController.java` uses only resolved `link.url()` as Location and ignores query input. `LinkValidation.java` accepts only http/https, valid host, visible ASCII, max 2048 and no credentials. Inherited scheme/redirect cases pass in gate; javascript/data/file cannot reach storage. |
| No server-side URL fetch / SSRF | pass | Validation parses a URI; redirect emits a header. New code is JDBC-only. No HTTP fetch of submitted URLs is introduced or present in this path. |
| Parameterized SQL | pass | `LinkRepository.java` binds ids and instants, with literal anonymous actor. `LinkService.java` passes stored ids and its clock value. Migration contains static SQL; audit insert and read remain parameterized JdbcClient statements. |
| Alias/code validation and reserved routes | pass | No client alias feature. `ShortCodes.java` produces eight case-sensitive base62 characters using the production SecureRandom; redirect route accepts 6–32 alphanumerics. Reserved set is api/actuator/v3/swagger-ui/error; admin is not generatable at eight characters. Existing collision and route tests pass. |
| Trusted proxy / spoofed X-Forwarded-For | pass | `web/RateLimitFilter.java:96` uses the peer unless explicitly trusted, then the right-most untrusted hop; default trusted set is empty. Inherited RateLimitFilter/Settings/Journey tests pass. `server.forward-headers-strategy=none` is shipped. Audit read additionally closes on native/framework or either Tomcat remoteip header override and rejects forwarding headers; inherited real-Tomcat controls pass. |
| PII, rotating salt and log hygiene | pass | `click/DailySalt.java` uses HMAC-SHA256 with a random 32-byte UTC-day key, zeroed/dropped on expiry; ClickRecorder reduces raw input before its bounded queue. Gate includes inherited privacy tests. New audit actors are static and timestamps contain no client values. Audit reconciles QA canaries and all 94 response/log joins; no raw address, UA, key or target canary in those logs. Existing audit snapshots intentionally preserve the link URL; this slice adds no new snapshot content. |
| Error leakage / 404, 410, 403 | pass | No new response handler. Existing missing/retired/audit-denied semantics and ProblemDetail paths pass gate. QA's actual JDBC rejection returns sanitized 500 and preserves the row; audited saved error bodies have no SQL, class names or traces. |
| Redirect/API headers and CORS | pass | Redirect retains `Cache-Control: no-store`; no CORS configuration or new header is introduced. Gate and QA captures retain existing Location/content types/request-id and error behavior. New audit-column fields are absent from saved bodies; existing `createdAt`/audit actor remain unchanged. |
| Actuator and H2 console exposure | pass | `resources/application.properties` exposes health/info/metrics/prometheus only, health details never. No H2-console enablement. Operational gate tests pass; dependency inventory remains unchanged. |
| Runtime dependencies | pass | Fresh offline [runtime inventory](proof/dependencies-305f804.txt) succeeded. Direct coordinates/resolved versions below; no dependency changes in the diff. No newly identified vulnerable coordinate is claimed by this offline review. |
| SPEC compliance / retention / deletion / audit columns | pass | AC-1–11 coverage is mapped in code review. New row columns, defaults, backfill, all writes/no-ops and rollback have tests; real f6 directory preserves old values. Inherited retention/startup/hold tests pass. No new right-to-delete or retention behavior is specified. GAPS closure remains item 5's explicit post-merge action. |
| Server-issued request id | pass | `web/RequestIdFilter.java:44` creates UUID independently of inbound header. Inherited RequestIdFilter tests and gate HTTP journeys pass; audited response ids each join exactly one request-completed event. |
| Short-code keyspace and rate limits | pass | `62^8 = 218,340,105,584,896` possible codes. Default budgets 60 API and 600 redirect requests/min/client, including error outcomes; limits/config tests and QA's 61st API request check pass. Random guessing of a particular code is constrained, not authentication; distributed clients are an inherited boundary. |
| API documentation exposure by profile | pass | Shared DESIGN's API-document decision keeps `/v3/api-docs` and `/swagger-ui.html` enabled in every shipped profile. Application properties and unchanged OpenAPIDocumentTest agree; no added column is exposed in the generated public contract. |
| Release dependency advisory/reachability check | n-a | No network advisory check performed in this sandbox review. Release must rerun `tools/dep-advisories.mjs` with network, record each advisory's reachability and route dated remediation; reachability is not a fix. Existing Tomcat/Jackson patched-version overrides are retained. |
| Audit append-only and transaction integrity | pass | `audit/AuditLog.java` still only INSERTs; `AuditTrail.java` uses named SELECT columns. V4's historical backfill is a one-time migration, with no new application UPDATE/DELETE of audit_log. Link mutation/stamp/audit share `@Transactional`; fresh failure journey and actual QA CHECK rejection preserve all link/audit fields on failure. Full audit rows remain unchanged by subsequent app writes. |
| Secrets and container execution | pass | Changed production text contains no secret/config credential. Dockerfile/compose unchanged: UID 10001, read-only root, data volume and bounded temporary tmpfs, loopback port publication. Source/config inspection only for container behavior; no image build/runtime claim. |

## Dependencies

| Direct runtime dependency | Resolved version |
|---|---|
| `org.springframework.boot:spring-boot-starter-actuator` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-data-jdbc` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-flyway` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.1 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.1.1 |
| `com.h2database:h2` | 2.4.240 |
| `io.micrometer:micrometer-registry-prometheus` | 1.17.1 |

Unchanged explicit security patch overrides resolve Tomcat to 11.0.25, Jackson 3 to 3.1.7 and Jackson 2 to 2.21.7. The full transitive tree is saved. This is an inventory and delta review, not a claim of current advisory-database clearance.

## Findings, limits and handoff

None. MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No new remediation/backlog requested. **Security PASS**, folded into the companion code PASS and single integration handoff.

Residual boundaries: service/database clocks intentionally differ; historical release time is not reconstructible; migration timing on an installed large directory and container execution were not rerun. Audit read remains anonymous for direct loopback callers, and a headerless local relay cannot be distinguished from such a caller; its documented operator boundary and Boot-trigger ceiling remain. Product defaults remain the trusted-proxy boundary. Fresh advisory/reachability checks belong to release. Before slice acceptance, the lead must close the interim audit-column GAPS rows and return proof item 5 to QA under `qitem-20261003234855-d2ef129b`; later shared-file changes require final evidence reconciliation.

## Self-check

Exact candidate and clean worktree verified; ten changed files read and ledgered; threat model and all eleven core plus six extended checklist items assessed with scoped evidence; fresh gate and offline dependency inventory inspected. No product changes or unsupported defect claim. Separate security row recorded in REVIEW-LEDGER; one authored workflow exit covers both reports.
