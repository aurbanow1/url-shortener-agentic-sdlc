# Security review — 03-dogfood-fix

Candidate **4fe70427bd0d182e886d6a19b217daa1d9e39f5d**, reviewer `review-agent@urlshort-factory` (Codex), 2026-10-03. Same combined packet and six-file range as [code review](01-code-review.md). **PASS; no findings.**

The changed surfaces are generated API metadata and registered meter tags. The design's main threat is installation-path disclosure by anonymous metrics. No new handler, input, SQL, storage, authentication or logging path. All six complete changed files read; relevant unchanged security mechanisms inspected and exercised by the fresh full gate.

## Checklist

| Item | Status | Evidence and scope |
|---|---|---|
| Stored redirect; http/https allow-list | pass | Unchanged `RedirectController` sets Location from `links.resolve(code).url()`, no reflected target. `LinkValidation` permits only http(s), rejects credentials/control characters and malformed hosts. Fresh original redirect/validation journeys pass, including unsupported schemes. |
| No server-side fetch / SSRF | pass | Neither new bean fetches a URL; the complete diff is metadata and meter registration. Existing redirect is a response header. No new HTTP client/resolver call. |
| Parameterized SQL | pass | No SQL change. Existing Spring Data/JdbcClient statements bind values; audit read and writer remain SELECT/INSERT. Fresh data journeys pass. |
| Code/alias charset, length, reserved words and case | pass | No alias surface introduced. Existing SecureRandom eight-character `[A-Za-z0-9]` generation and case-sensitive lookup; redirect accepts 6–32 characters; reserved api/actuator/v3/swagger-ui/error, and admin cannot be an eight-character generated code. Fresh original tests green. |
| Rate limiting and spoofed forwarding headers | pass | Filter/trusted-proxy rule unchanged: peer identity by default, right-most untrusted XFF only behind an explicitly trusted peer; shipped strategy NONE. Fresh spoofing/limiter tests pass. New nested test proves 429 still conforms; installed QA captures show the default create budget. No guarantee added for arbitrary container rewriting settings. |
| PII, salt and log hygiene | pass | New code logs nothing. Unchanged DailySalt stores a 32-byte daily HMAC key only in memory, never logs/exposes it, zeroes/drops it on UTC expiry and close; reduction/storage tests pass. Independently reconciled all 208 response IDs/statuses with both default-console and ECS-file completions and whole-run privacy canary absence. |
| ProblemDetail / error leakage and 404/410/403 semantics | pass | Runtime problem producers untouched; six installed full bodies equal baseline, including static error fields/messages and safe 404/410/429. Fresh inherited audit refusal/failure tests pass. Schema now documents existing optional errors; every documented problem response retains the same component. The old metrics-selector's expected empty 404 is an unchanged Actuator behavior for an unmatched tag, not a newly altered application problem producer. |
| Headers and CORS | pass | Stored redirects retain no-store; content types and all OpenAPI response/header metadata unchanged outside the two components. No CORS annotation/property or response-header change. |
| Actuator/H2 exposure | pass | Existing health/info/metrics/prometheus exposure and status-only health retained; no H2 console. Installed candidate scrape and disk endpoints have no path tag or known working directory while values remain positive. Two new functional assertions exercise both surfaces. |
| Dependency versions and advisories | pass | No dependency/build change; fresh offline inventory below. No newly identified known advisory from this review. Sandbox cannot query advisory databases; this is not a fresh CVE clearance. Release must run the network advisory check with reachability/remediation handling. Existing Tomcat/Jackson fixed-version overrides remain. |
| Retention/right-to-delete obligations | n-a | No new table, stored value, retention requirement or deletion promise. Retention remains the separately allocated slice. No compliance policy broadened by this change. |
| Audit completeness and append-only | pass | No application audit UPDATE/DELETE path in the inspected source; AuditLog INSERT remains inside mutation transactions, AuditTrail SELECT unchanged. Fresh audit/transaction tests pass; QA installed create/retire captures correlate audit rows and replay does not add a mutation. |
| Server-issued request ID | pass | Unchanged RequestIdFilter issues a UUID before the chain and ignores inbound IDs. Fresh canary tests and all 208 retained response/event correlations pass; error instance matches response ID. |
| Enumeration cost | pass | Existing 62^8 = 218,340,105,584,896 code space (~47.6 bits); budgets 60 API / 600 redirect per client per minute. No shorter identifier or new guessing route. This bounds guessing, not authentication. |
| API-document exposure | pass | Anonymous `/v3/api-docs` and Swagger remain the existing recorded profile decision. Only known error metadata is added; no secret or captured user content enters it. Seven operations and 22 problem references preserved. |
| Secrets and container privilege | pass | Complete changed-file inspection finds no credential/secret. Container files unchanged: UID 10001, read-only root, data volume/tmpfs and loopback Compose publication. No container rebuild by this review claimed. Base URL remains environment-overridable with localhost default. |
| DDL, UTC and audit-column policy | n-a | No DDL or time transformation. Existing column debt is explicitly allocated to named V3/V4 migrations in GAPS; this metadata/meter slice adds no table requiring migration. |
| Audit-read access boundary | pass | Audit-read CR-01 fix is inherited through merge cb148c4. No controller/settings change; fresh native/framework/both remoteip-trigger regression journeys remain green. Accepted service peer/header boundary and headerless-local-relay qualification are unchanged. |

## Direct runtime dependencies

Fresh `scripts/gw --offline dependencies --configuration runtimeClasspath` exits 0; [full inventory](proof/runtime-dependencies-4fe7042.txt).

| Coordinate | Version |
|---|---|
| `org.springframework.boot:spring-boot-starter-actuator` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-data-jdbc` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-flyway` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.1 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.1.1 |
| `com.h2database:h2` | 2.4.240 |
| `io.micrometer:micrometer-registry-prometheus` | 1.17.1 |

## Verdict and residuals

**Security PASS; zero findings at every severity.** Threat-model disclosure fix verified by fresh functional effects and independent audit of installed raw captures. [Reconciliation](proof/evidence-reconciliation-4fe7042.json) confirms positive gauges without path, full wire equality, raw archive integrity and log correlation. [Fresh gate](proof/code-check-4fe7042.txt): **204 unit + 207 functional; 508/508 lines and 194/194 branches**.

Dropping `path` deliberately invalidates old path selectors (Actuator 200→404). A future second disk path needs a non-sensitive distinguishing tag to avoid merging series; this is the explicitly documented ceiling in ADR-0016 and the code comment. No additional fix or backlog item is required on the single default path. Advisory freshness, deployment/proxy boundary and broader metrics exposure remain the existing limitations.

Self-check: exact candidate with only independently verified inherited wrapper line endings; all six files reviewed; full security checklist and offline inventory recorded; no source/test edits, no remote exposure or publishing. Combined handoff is PASS. QA resolved shared-GAPS reference item `qitem-20261003224103-2a21b284` with receipt 10 at `2f1987e`; independently checked [live 8/8 readiness and all 38 evidence-reference hashes](proof/proof-readiness-4fe7042.json). No remaining review or receipt blocker at handoff.
