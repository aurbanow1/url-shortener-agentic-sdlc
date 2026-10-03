# Security review — 01-audit-read

Candidate: **35590f06c852543c29097a42c43b7802be90ba40**, same clean worktree and combined packet as [code review](01-code-review.md). Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03. **FAIL: CR-01 HIGH**, shared with code review; no other finding.

The new entry point returns existing sensitive audit content to local Operators. Main threats are disclosure through a rewritten peer, accidental mutation, error/log leakage and unbounded reads. All 15 changed files were read; the code report has the complete ledger. No authentication is required by the SPEC. Its documented local-relay boundary is retained.

## Security checklist

| Item | Status | Evidence and qualification |
|---|---|---|
| Stored redirect and http/https scheme allow-list | pass | Unchanged `RedirectController` uses `links.resolve(code).url()`; `LinkValidation` rejects other schemes, credentials and control characters. Fresh full gate includes original redirect/validation journeys. No new Location writer. |
| No server-side fetch / SSRF | pass | Read all new runtime code: SELECT and JSON mapping only, no network fetch. Existing redirect remains a response header, not a URL fetch. `InetAddress` receives the container's numeric peer; there is no user-URL resolver. |
| Parameterized SQL | pass | `AuditTrail.java:20` and `:33`: static SQL, bound `before` and `fetch`; base64 input becomes a positive long before binding. Existing AuditLog uses named INSERT parameters. No new write. |
| Alias/code charset, length, reserved names and case | pass | No alias creation surface. Unchanged eight-character SecureRandom `[A-Za-z0-9]` code; redirect pattern 6–32, case-sensitive lookup; reserved `api`, `actuator`, `v3`, `swagger-ui`, `error`. `admin` cannot be an eight-character generated code. Fresh original tests pass. |
| Rate limit and trusted-proxy rule | pass | Shipped NONE/default configuration preserves peer identity and unchanged `RateLimitFilter.clientOf` only trusts XFF from listed proxies. `/api/audit` uses create budget. QA measured a 90-read burst: 60 admitted, 30 refused with Retry-After; copied evidence audited. Configuration bypass in the audit guard is separately **fail CR-01** below; no assertion that native peer rewriting preserves the limiter's original peer premise. |
| PII reduction and log hygiene | pass | Unchanged DailySalt HMAC-SHA256, 32-byte random per-day key, memory only, zero/drop on UTC expiry and close; click rows retain hash, UA class, referrer origin. New audit code logs nothing; static validation errors and existing message-free 500 handler. Fresh AC-15/16/21 tests, QA canary logs and correlation. Audit before/after URLs are deliberately returned exactly as stored under the access rule; no new secret field. |
| Error leakage and status semantics | pass | Normal denied inputs → bare 403, invalid cursor/limit → safe 400, wrong method → 405, injected store error → bare 500 and recovery 200. Existing missing/retired links retain 404/410. HEAD body suppressed on actual Tomcat. Full gate and QA raw exchange prove these cases; CR-01 is an incorrectly admitted 200, not a claim that all requests are refused safely. |
| Redirect/API headers and CORS | pass | Redirect still `Cache-Control: no-store` with stored Location. New 200 explicitly application/json; errors problem+json; no CORS setting/annotation introduced. Audit no-store is not specified by this design. |
| Actuator/H2 exposure | pass | Candidate properties expose health/info/metrics/prometheus, status-only health; no H2 console. No new management endpoint. Fresh original health/metrics tests pass. |
| Dependencies and advisories | pass | No dependency/build change. Offline runtime listing below and [raw output](proof/runtime-dependencies-35590f0.txt). No newly identified known advisory. This is not a fresh network CVE scan: sandbox review cannot query advisory databases; release must rerun the advisory check with network. Existing mission-01 release §4 records 97 coordinates, zero OSV advisories at 14:42Z; no claim about later advisories. |
| Retention/right-to-delete obligations | n-a | No new storage or retention policy. Audit deletion explicitly out of scope, append-only invariant below. Click-retention remains its allocated sibling slice; no delete compliance promise invented here. |
| Audit completeness, transaction and append-only (NFR-A2) | pass | Runtime source has only AuditLog INSERT and AuditTrail SELECT for audit_log. No application update/delete path. Link mutation transaction and writer unchanged. Fresh `AuditLogTest` and AC-10 pass; independently parsed QA snapshots identical before/after, all 45 audit rows and eight links preserved. Fixture DELETEs occur only in tests, not product. |
| Server-issued request id | pass | Unchanged RequestIdFilter reads no inbound id and sets UUID before chain; original canary test and new correlation tests green. QA reconciled all captured response IDs to request events. |
| Enumeration cost | pass | Unchanged 62^8 = 218,340,105,584,896 generated codes (~47.6 bits); per-client defaults 60 API / 600 redirect per minute. No new code guessing surface or shorter key. This is a cost bound, not authentication. |
| API-document exposure | pass | Anonymous `/v3/api-docs` and Swagger UI retained, exposed by prior decision; production-profile policy remains recorded as open in DESIGN. New example is synthetic. No hidden trail contents added to the document. |
| Secrets, environment and container | pass | Complete changed-file scan found no secret or credential; existing base URL remains environment-overridable with localhost default, empty local H2 password unchanged. No image/config privilege change: UID10001, read-only root, writable data and tmpfs, loopback Compose publication. Container was inspected as source, not rebuilt by this review. |
| DDL, UTC, audit-column policy | pass | No migration or changed table in this candidate. UTC read maps OffsetDateTime to Instant. Existing missing columns are named in GAPS with V3/V4 remediation; no silent waiver. |
| Loopback, forwarding/settings, HEAD and error-body boundary (NFR-S6; proof item 12) | **fail** | **CR-01:** explicit `server.tomcat.remoteip.remote-ip-header` or `protocol-header` activates rewriting despite NONE. Real candidate returns stored audit URL to a request carrying forged loopback XFF; HEAD also admitted. [Control output](proof/code-controls-35590f0.txt). Default peer/header cases and native/framework refusal pass, but do not establish the universal claim. **Do not accept item 12 on this SHA.** |

## CR-01 reproduction and required correction

Location: `src/main/java/dev/urlshort/audit/AuditController.java:61`. `peerIsConnection` tests only `ForwardHeadersStrategy.NONE`, while container-specific properties independently enable rewriting.

From the exact candidate worktree:

```sh
scripts/gw --offline -I /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/01-audit-read/proof/audit-code.gradle reviewAuditCode
```

The independent Java launcher starts the actual application five times, each on a free loopback port and fresh in-memory H2, writes one synthetic link by HTTP, and reads its audit page with GET/HEAD. With `server.tomcat.remoteip.remote-ip-header=X-Forwarded-For`, or separately `server.tomcat.remoteip.protocol-header=X-Forwarded-Proto`, it prints effective strategy NONE and a **200 body containing AUDIT-CODE-CANARY** for `X-Forwarded-For: 127.0.0.2`. The same request under defaults is 403; native/framework strategies refuse all audit reads. The process exits successfully after recording observations; it is not an all-pass test.

Refuse audit access whenever container handling may rewrite the connection address, including both independent settings. Add real-container regression tests, retain the existing default/strategy tests, and update design/ADR via their owner. This single HIGH drives the combined failed exit. No producer file was changed by review.

## Proof item 12 scope and residuals

Recorded explicitly for `qitem-20261003194346-b74b8081`: **not ready** on this SHA. QA controlled peers cover `192.0.2.10`, `10.0.0.7`, four loopback spellings, trusted-proxy changes, errors and HEAD; real-container tests cover native/framework and the new failing remoteip settings. The observed bypass is on a loopback socket carrying a forbidden forwarding header; this review did not expose a listener externally or test an actual remote TCP source.

The SPEC's separate trust boundary still applies: a local relay that removes/omits both forwarding headers is indistinguishable from the Operator. Do not relay `/api/audit`, or make the relay add a refused forwarding header. That documented limit does **not** excuse CR-01, where the incoming request carries the header and the application's configured container consumes it. No authentication, proxy-wide security guarantee or human exception is claimed.

## Direct runtime dependencies

| Coordinate | Resolved version |
|---|---|
| `org.springframework.boot:spring-boot-starter-actuator` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-data-jdbc` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-flyway` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.1 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.1.1 |
| `com.h2database:h2` | 2.4.240 |
| `io.micrometer:micrometer-registry-prometheus` | 1.17.1 |

## Verdict and self-check

**FAIL, one HIGH (CR-01), no other severity findings.** The code report supplies the shared findings table, full file coverage, fresh 200+200 gate, 100% merged coverage and QA reconciliation. Every checklist item above has a disposition and evidence; tested inputs are distinguished from untested deployment boundaries. Product worktree remains clean at the named SHA; both ledger rows record this verdict. Return to implement and require exact-candidate QA/re-review before integration. No non-blocking work added.
