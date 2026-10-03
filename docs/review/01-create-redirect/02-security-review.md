# Security review — 01-create-redirect

- Candidate: `a922f49144049db0228c316c474ac6e890742fa5`, identical to QA and code review.
- Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
- Packet: `qitem-20261003074021-e3821d80`; combined code/security assignment.
- **Verdict: PASS — no security findings.** Code review's CR-01 is a non-blocking API-document follow-up.

## Scope and evidence

Reviewed all 48 changed files, the approved design's threat model (§6), review
guidance §6, architecture §6 and databases §6–§8. The complete file ledger and
fresh quality-gate/QA evidence audit are in `01-code-review.md`. New entry
points are create JSON/key, management and redirect paths, wrong methods, the
base-URL setting, and link/audit persistence. Confidence is high within this
anonymous, unthrottled slice; rate limiting and deployment hardening belong to
03-operate.

Evidence below is relative to this directory unless explicitly marked QA.
The fresh full gate passed 72 unit and 87 functional invocations; merged
coverage 185/185 lines and 56/56 branches. Own live responses:
`proof/http-a922f49.txt`; application events: `proof/runtime-a922f49.txt`;
parsed checks: `proof/live-inspection-a922f49.json`. Tests named below were
read and executed in that gate.

## Checklist

| Item | Status | Evidence / boundary |
|---|---|---|
| Redirect target and scheme allow-list | pass | `link/RedirectController.java:46` uses only the resolved stored URL; `LinkValidation.java:30` applies required/length/scheme/URI/credentials in order. AC-4 rejects javascript/data/file/ftp, missing host and userinfo; CRLF is rejected by a unit case. AC-12 and own redirect show exact stored Location plus no-store; incoming query is ignored. Any http(s) destination is intentionally permitted. |
| Server-side fetch / SSRF | pass | Read every changed production source: no target fetch, DNS lookup, HTTP client or preview path. URI parsing and response-header assignment only. Local/private targets are expressly allowed by A-13 because the service never fetches them. |
| Parameterized SQL | pass | Spring Data derived lookups/save and fixed named-parameter updates in `link/LinkRepository.java:25`/:30; fixed INSERT with bound values in `audit/AuditLog.java:25`/:59. SQL text is never constructed from request data. |
| Code/alias charset, length, reserved names and case | pass | `ShortCodes.java:18`/:20 and `LinkConfig.java:26`: eight SecureRandom alphanumeric characters; reserved list redrawn (deterministic unit test for actuator); unique code constraint; regex routes are case-sensitive, with normal case-sensitive H2 string comparison. Custom aliases are out of scope; shorter reserved paths such as api/admin cannot be generated. |
| Rate-limit spoofing / trusted proxies | n-a | This SPEC explicitly has no rate limiter (FR-10 belongs to 03-operate); no safety claim about X-Forwarded-For throttling is made. Base URL ignores Host/forwarding headers, separately proven by AC-3 and the live spoofed-header replay. |
| PII, IP hashing, log and audit hygiene | pass | No client address/UA/referrer is stored in this slice, so salt rotation is not applicable yet. AuditLog records actor anonymous, fixed action/entity, code, request id and required before/after URL/state; no idempotency key or inbound header is copied. URLs, including their query text, are deliberately retained in link/audit under A-18/A-20; this is not a claim that arbitrary target URLs contain no secrets. AC-27, real driver-error canaries and own runtime scan show no input canary/address in service logs. Failure logging contains only class names and a code location, never throwable messages. |
| Error leakage / 404–410–403 semantics | pass | One advice strips detail and sets a server-issued URN instance; static validation errors. Own browser 410, 400/405/415 and unavailable-resource 404 are problem JSON with safe bodies; AC-24/AC-27 exercise a 500 and rollback. 403 is not a link contract in this anonymous slice; HEAD/OPTIONS retain framework defaults. CR-01 concerns only the extension's documentation. |
| Redirect/API headers and CORS | pass | 302 has no-store and exact Location; 410 has no Location. Live Origin-bearing redirect has no Access-Control-Allow-Origin; no CORS annotation/configuration exists in the reviewed application. Other cache-header promises are explicitly outside the SPEC. Allow on 405 and Accept on 415 survive error sanitization. |
| Actuator and H2 exposure | pass | `application.properties:29` exposes only health/info/metrics; live Actuator index agrees. /actuator/env and /h2-console return safe 404. No H2 console dependency/configuration added. Prometheus is not part of this candidate. |
| Dependencies and known advisories | pass | Offline runtime tree captured below; mandatory Tomcat/Jackson fixes resolve to their intended versions. The builder's existing OSV record covers 90 coordinates and reports zero findings at 2026-10-03T06:42:17.506Z. This sandbox cannot query advisory databases; no fresh online scan or absence of all vulnerabilities is claimed. Release must rerun the network advisory check. |
| Compliance: retention, deletion and audit completeness | pass | The contract retains retired links and append-only audit, not hard deletion or personal-data erasure. AC-22–25 prove required rows, same-transaction rollback and preserved history; QA's actual rejected audit insert leaves all prior link/audit rows unchanged. Click/IP retention belongs to later slices; no new retention/erasure promise here. |
| Server-issued request id | pass | RequestIdFilter creates UUID, sets header before chain, logs completion before MDC removal, never reads inbound id. Own spoofed inbound id differs from the response; eleven saved responses match their runtime completion events. Twelve functional status windows and cold first request enforce every-event correlation. |
| Enumeration resistance | pass | 62^8 = 218,340,105,584,896 possible codes (about 47.6 bits), generated with SecureRandom. At one million live links a random guess hits with probability about 4.58e-9; this is arithmetic, not a load/timing claim. Anonymous read/retire and lack of throttling are accepted residuals until later hardening; unpredictable identifiers are not authorization. |
| API documentation exposure by profile | pass | OpenApiConfig pins server to /; document and UI intentionally public under this design until 03-operate decides production-profile exposure. Own live document equals committed JSON; no submitted host embedded. |
| **NFR-A2: no audit UPDATE/DELETE path** | pass | Only production audit SQL is `AuditLog.java:25` INSERT. `AuditLogTest.java:36` asserts the sole append operation/INSERT constant; its real H2 row test and AC-25 passed. `proof/source-check-a922f49.json` records every production audit_log reference. Explicit proof-item-13 record also appears in code review. |
| **NFR-S4: committed secrets and environment setting** | pass | Full changed-file inspection plus committed-tree private-key/token-pattern screen found no credential/token/private key (`proof/source-check-a922f49.json`). Blank embedded-H2 password is the declared local default. `LinkProperties.java:14`/`application.properties:4` ship http://localhost:8080; QA's `qa-configured-base-a922f49.txt` demonstrates URLSHORT_PUBLIC_BASE_URL=https://sho.rt. Both AC-3 configurations pass; release-prep secret scan remains outstanding for proof item 14. |
| Image secrets, non-root process and read-only filesystem | n-a | This candidate adds no image/deployment change; container policy is explicitly 03-operate/release scope. No claim inferred from source-only review. |
| Request-size/early-container boundary | pass | Fresh AC-7 accepts 16,384 bytes and rejects 16,385; unit stream tests cover single/bulk reads and unchanged count across repeated access. Multipart disabled. Malformed request lines/oversized headers rejected before filters remain the design's explicit 03-operate boundary; no request-id/problem-body claim made for those paths. |

Paths `link/…`, `audit/…`, and `web/…` above are under
`src/main/java/dev/urlshort/`; test names are under their corresponding
unit/functional source roots. NFR-A2/A1 are application guarantees: the embedded
database still uses one local user; database grant restrictions are not claimed.

## Dependency inventory

Fresh command from the exact candidate worktree:

```sh
scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/01-create-redirect/proof/runtime-dependencies-a922f49.txt --offline dependencies --configuration runtimeClasspath
```

| Direct runtime dependency | Resolved version |
|---|---|
| org.springframework.boot:spring-boot-starter-actuator | 4.1.1 |
| org.springframework.boot:spring-boot-starter-data-jdbc | 4.1.1 |
| org.springframework.boot:spring-boot-starter-flyway | 4.1.1 |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 3.1.1 |
| com.h2database:h2 | 2.4.240 |

No new dependency. The override-only first commit raises Tomcat embed to
11.0.25, Jackson 3 core/databind to 3.1.7, and Jackson 2 components to 2.21.7;
all appear in my resolved tree. The known prior Tomcat/Jackson advisory
remediations are therefore present. Source of the earlier online result:
`missions/01-greenfield-core/slices/01-create-redirect/proof/osv-advisories-20aef57.json`.
Release owns the fresh advisory lookup and any reachability/remediation record.

## Findings and residual risks

No security finding. The accepted global key namespace, anonymous API,
collision/concurrent-key 500, retained audit URLs, H2-only verification and
later rate-limit/deployment work remain visible in the design and PROOF.
No simultaneous race, PostgreSQL, image or release scan was performed by me.
Code CR-01 can be backlog; it does not weaken the observed HTTP error behavior.

## Self-check

Same candidate as QA; 48/48 files read and linked by the code-review ledger.
Threat model checked against each new entry point; row per checklist item with
scoped evidence and n-a reasons. Fresh gate, offline dependency inventory and
loopback checks completed. App stopped (intentional SIGTERM, bootRun exit 143)
and port 18101 confirmed closed; worktree clean. No product/test changes.
Both review rows appended to REVIEW-LEDGER.md. Exit handoff to integrate,
carrying CR-01 and the pending release secret/advisory scans.
