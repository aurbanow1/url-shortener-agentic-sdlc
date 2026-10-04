# Security review — 01-analytics-v2

Candidate **ec466da8da4b1efde9d612c6c8692070cc6fc4b9**, reviewer `review-agent@urlshort-factory` (Codex), 2026-10-04 UTC. **PASS; no findings.** Same combined packet and 18/18-file range as [code review](01-code-review.md).

Threat model checked against the changed surfaces: GET statistics adds daily aggregates; the redirect recorder consumes the limiter's resolved address; anonymous metrics add two bounded counter families. No new endpoint, persistence column or external service. The principal risks are cross-day identity use, forged forwarding input, sensitive metric/log labels and altered fail-open recording.

## Checklist

| Item | Status | Evidence and scope |
|---|---|---|
| Redirect target and scheme allow-list | pass | Unchanged RedirectController takes Location only from links.resolve(code).url(); LinkValidation allows http/https, rejects other schemes, credentials and controls. Fresh original redirect/validation tests pass; captured delayed/concurrent responses retain exact stored Location and no-store. |
| No server-side fetch / SSRF | pass | Complete diff and relevant product source introduce no outbound client/fetch. Stored target is a response header; the new SQL and hash operations do not resolve or fetch user URLs. |
| Parameterized SQL | pass | ClickStore.java:61 uses one static UNION ALL statement with bound linkId in both branches; insert, code lookup and retention cutoff also bind values. No user-built SQL, join on hashes or new DDL. |
| Alias/code validation and reserved paths | pass | No alias feature added. Existing eight-character SecureRandom alphabet A–Z/a–z/0–9, case-sensitive storage/lookup; redirect pattern 6–32. Reserved api/actuator/v3/swagger-ui/error retained; admin cannot be generated at length eight. Original tests remain green. |
| Trusted proxy / rate-limit spoofing | pass | RateLimitFilter selects the peer by default, or right-most untrusted XFF hop only behind an explicitly configured trusted peer. Same identity is assigned to CLIENT_ATTRIBUTE before charging and consumed by ClickRecorder.java:115. Untrusted XFF/Forwarded values do not change identity. No getRemoteAddr rewrite. Fresh unit/journey tests and retained default/trusted captures pass. Operator proxy trust and safe container forwarding settings remain prerequisites. |
| **Rule 5 / proof item 11: same-day-only hash use** | **pass** | Product-wide client_hash/clientHash source search finds the sole analytical read in ClickStore.java:66: COUNT(DISTINCT client_hash) with GROUP BY clicked_on and bound linkId. Only aggregate counts leave SQL. No cross-day comparison, raw-hash result, profile, join, metric label or log field. ClickRecorder.java:117–120 derives clicked_on from the same Stamp instant/key selection, in UTC. DailySaltTest and midnight/identity journeys pass; retained two-day hashes differ and response figures remain per day. This explicitly supplies item 11's required construction judgment. |
| **Rule 5 / proof item 11: salt location, lifecycle, non-persistence** | **pass** | DailySalt.java:39 holds the 32-byte SecureRandom salt only in its private in-memory array. Synchronized select():53 picks UTC instant/day and an ephemeral SecretKeySpec copy; stamp returns only instant/HMAC. Scheduled expire():69 zeroes/drops the owning array at day end even without another click; rotation and @PreDestroy close():76 also call drop():80. No salt column, SQL binding, log, metric or HTTP exposure exists. All seven DailySaltTest cases pass in the fresh gate, including expiry without another click, close, stale expiry and a pre-midnight selection completing under its original key. In-flight key copies are transient and can finish after rotation; this is not a claim of JVM-wide memory erasure or a hard real-time timer deadline. |
| PII/log and metric hygiene | pass | Only origin, UA class and HMAC cross into ClickWrite/SQL; raw resolved address stays on the request/reduction path. ClickRecorder.java:175 emits constant reason and exception class, never exception message/click fields; :88 registers an untagged recorded counter and five fixed reason values. No dynamic labels. Independently checked 716 responses in both console/file sinks, no private canary or stored hash; four physical failed inserts produce only the correlated safe WARNs. |
| Error leakage / 404, 410, 403 | pass | Existing ProblemDetail handlers unchanged. Fresh missing/wrong-method/failing-store/retired and inherited audit refusal tests pass; captured problem bodies have matching status, application/problem+json and server-id instance. Recording failure preserves 302; stats read failure remains the safe error path. No stack trace or SQL added to responses. |
| Headers / CORS | pass | Stored redirects retain Cache-Control: no-store and exact Location. No CORS/config change; no permissive annotation or new header producer. Three retained actual-wire HEAD responses have zero body bytes; HEAD and OPTIONS record no click. |
| Actuator / H2 exposure | pass | Unchanged application.properties exposes only health/info/metrics/prometheus, health status only; H2 console absent. New counters expose aggregates and fixed reason labels through the existing operator surfaces. Exposure is not authentication. |
| Dependencies / known advisories | pass | No dependency change; fresh offline resolved inventory below. Existing fixed Tomcat 11.0.25 and Jackson 3.1.7/2.21.7 overrides remain. No additional known advisory identified here; no fresh advisory database query or CVE clearance claimed. Release must run the network advisory check and record reachability plus dated remediation where needed. |
| Retention / deletion compliance | pass | No new stored identifier/column or expanded retention. Existing 90-day default purge and operator hold unchanged. Fresh retention suite passes; independently reconciled scheduled-purge snapshots remove clicks while preserving links. Link DELETE remains retirement, not a newly promised erasure feature. |
| Audit completeness / append-only | pass | AuditLog INSERT and AuditTrail SELECT unchanged; no application audit UPDATE/DELETE path. Mutation transaction tests pass; QA read-only snapshots preserve all rows. This is the application-path guarantee, not separate database-role privilege isolation in embedded H2. |
| Server-issued request ID | pass | RequestIdFilter creates a UUID before the chain, ignores inbound IDs and clears MDC afterward. Fresh canary tests pass; independently parsed retained headers/logs correlate exactly once, and error instance uses the same server id. |
| Enumeration cost | pass | Existing 62^8 = 218,340,105,584,896 code space (~47.6 bits) and per-client 60 API / 600 redirect requests/minute retained. No identifier shortening or extra guessing route. Rate limiting reduces guessing; a short code is not authentication. |
| API-document exposure | pass | Existing anonymous /v3/api-docs and Swagger exposure decision retained. Added documentation contains synthetic example counts only; all other operations are unchanged. |
| Secrets and container privileges | pass | Complete changed-file inspection finds no committed credential/secret; public base URL remains env-overridable with localhost default. Unchanged Dockerfile runs UID 10001; Compose read-only root, data volume/tmpfs and loopback-only published port retained. No image rebuild or image-forensics scan claimed in this review. |
| UTC, audit columns and rollback | n-a | This slice has no DDL/migration. It inherits V3 for click and UA-reference audit columns; the existing GAPS entry names V4 for link/audit_log and is not silently treated as implemented by analytics. No new rollback obligation. |
| Audit-read boundary | pass | The shared attribute does not rewrite the peer. Inherited AuditController still requires NONE and both Tomcat remoteip header settings absent, then loopback/no-forwarding-header input. Fresh native/framework/remoteip refusal journeys pass. The previously accepted headerless-local-relay boundary remains. |

## Direct runtime dependencies

Fresh `scripts/gw --offline dependencies --configuration runtimeClasspath` exits 0; [full inventory](proof/runtime-dependencies-ec466da.txt).

| Coordinate | Resolved version |
|---|---|
| org.springframework.boot:spring-boot-starter-actuator | 4.1.1 |
| org.springframework.boot:spring-boot-starter-data-jdbc | 4.1.1 |
| org.springframework.boot:spring-boot-starter-flyway | 4.1.1 |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 3.1.1 |
| com.h2database:h2 | 2.4.240 |
| io.micrometer:micrometer-registry-prometheus | 1.17.1 |

## Verdict, residuals and self-check

**Security PASS; MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0.** No new fix or backlog item. The changed entry points satisfy the design's privacy boundary and preserve fail-open recording. recorded counts a successful insert even if shutdown already reported its outcome unknown; the counters can both increase for that click, as explicitly designed and freshly tested.

Evidence: [clean gate](proof/code-check-ec466da.txt), **221 unit + 241 functional; 580/580 lines and 206/206 branches**; [independent capture/hash/statistics reconciliation](proof/evidence-reconciliation-ec466da.json). Controlled peers/clock/H2 faults are separate from the unchanged installed-jar observations. No natural-midnight, remote-network proxy-boundary, production latency or fresh advisory scan is claimed.

**QA must judge proof item 11 from the two explicit rule-5 rows above before integration** under a12a0e2. Proof item 12 remains for release_prep's redirect p95/p99 benchmark or recorded gap. Same-day restart overcount, pre-v2 proxy-hash coalescing, existing proxy configuration assumptions and transient key-copy lifetime remain disclosed design limits.

Self-check: exact SHA and clean worktree; every changed file reviewed; full checklist, threat model, fresh gate and resolved dependencies inspected; paired security ledger row appended. Only docs/review artifacts changed; no product code, tests, QA evidence or producer documents edited, and nothing published.
