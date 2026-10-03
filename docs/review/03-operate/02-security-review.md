# Security and compliance review — 03-operate

Candidate: `a7c533ffef55650e5b422377ffe0c4e38d41400c`. Reviewer:
`review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC. Same assigned packet,
candidate, 24-file range and empirical gate as [code review](01-code-review.md).
**FAIL: SEC-01 HIGH, the same defect as CR-01.** CR-03 MEDIUM and QA-OPR-02
LOW are carried explicitly below, not newly discovered duplicate findings.

## Context and threat assessment

The new entry points are the limiter on existing public routes, trusted proxy
identity handling, anonymous health/metrics/Prometheus and documented API UI;
container and smoke changes support release. The threat model correctly avoids
URL fetching, raw client identity in persistence/output and untrusted forwarding
headers. Its rate-limit protection does not hold under ordinary request
reordering (SEC-01); its memory bound needs the rollback qualification (CR-03).
The fixed single-node scope, no authentication/custom aliases, operator-only
loopback deployment and existing URL contract remain the accepted boundaries.

## Checklist

Paths below are relative to the candidate unless prefixed with a root evidence
path. Prior link/analytics defenses were read in source and exercised by the
fresh regression gate; a pass is scoped to the stated evidence.

| Item | Status | Evidence |
|---|---|---|
| Redirect target is the stored validated URL; only http/https | pass | `link/LinkValidation.java` validates scheme, ASCII/length, host and absent userinfo; `link/RedirectController.java` resolves stored state and emits its URL. Inherited create/redirect validation journeys pass, including javascript/data/file refusals. No reflected target added. |
| No server-side fetch / SSRF | pass | Reviewed main source has no outbound URL fetch; submitted target is validated, persisted and returned as Location only. No HttpClient/RestClient/RestTemplate/URLConnection use. |
| SQL parameterization | pass | `link/LinkRepository.java`, `audit/AuditLog.java`, `click/ClickStore.java` use static SQL with bound parameters; limiter does not access the database. QA's separately disclosed table snapshot launcher is not product code. |
| Code charset, length, reserved words and case | pass | `link/ShortCodes.java`: random eight-character case-sensitive alphanumeric codes; reserved set checked. `api`, `actuator`, operator routes protected by routing/reservation; `admin` cannot be generated at length five. Alias creation remains out of scope. Redirect route validates 6–32 alphanumerics; invalid/unknown paths follow existing errors. |
| Trusted proxy / forged X-Forwarded-For | pass | `web/RateLimitFilter.java:96`: peer must be in exact configured trust set before forwarded hops are considered; right-most untrusted nonempty hop, otherwise peer. Defaults trust none. Filter/unit and TrustedProxy journeys plus QA peer controls cover spoofing and fallbacks. Framework forwarding is not enabled. |
| Rate-limit budget under concurrency | **fail** | **SEC-01/CR-01** at `web/RateLimiter.java:95`: actual candidate admitted 120 requests for one 60-token client across 1 ms without any backwards clock movement. [Probe output](proof/rate-boundary-a7c533f.txt). |
| IP hashing, rotating salt, UA/referrer reduction, no secrets in audit | pass | `click/DailySalt.java`, `Click.java` and `ClickStore.java`: inherited random daily salt and HMAC, UA classification and origin-only referrer; source and inherited privacy/expiry tests checked. Limiter raw identity is only its in-memory key. Audit stores operation facts and server request id, not peer/header secrets. |
| Error leakage and 404/410/403 contract | pass | Filter :76–82 returns minimal ProblemDetail, UUID instance and Retry-After, no submitted values. `ProblemDetailsAdvice` uses fixed error bodies. 30 captured 429s correlate exactly once; inherited error journeys pass. Unknown code 404 and retired 410 preserved. 403 is not an endpoint outcome required by this unauthenticated SPEC. Health failures are intentionally status-only health documents, not API ProblemDetails. |
| Response headers and CORS | pass | Redirect still sends `Cache-Control: no-store`; API/429 use their specified media types and server request id, 429 supplies Retry-After. No permissive CORS configuration or `@CrossOrigin` added/found. SPEC does not require blanket no-store for every API response. |
| Actuator exposure and H2 console | pass | `application.properties` exposes only health/info/metrics/prometheus; health details never shown and readiness includes DB. No H2 console setting/dependency. HealthMetrics and DatabaseDown journeys plus QA captures agree. Default disk path gauges assessed separately below. |
| Direct dependency inventory and known advisory remediation | pass | Fresh offline [runtime dependency tree](proof/runtime-dependencies-a7c533f.txt); direct versions below. Existing Tomcat/Jackson patch overrides preserved; no new known advisory identified in reviewed local evidence. This is not a fresh advisory-database clearance. |
| Compliance: retention, deletion and audit completeness | pass | No new persistent data/schema/retention policy. Existing retirement/audit transaction and privacy suites pass. Limiter records no database rows and a rejection does not create a link/audit mutation. Future persistent-retention/right-to-delete work is outside this SPEC; not claimed implemented. |
| Request id is server-issued; inbound value ignored | pass | `web/RequestIdFilter.java` issues UUID and uses request-scoped MDC; inherited Observability journeys test spoofed id canary, bodies and logs. New filter runs inside that scope. |
| Enumeration: keyspace and effective budget | **fail** | Eight base62 characters give 218,340,105,584,896 possibilities (about 47.6 bits). Intended redirect rate is 600/min, burst 600, refill 10/s. SEC-01 also affects that shared bucket implementation, so the intended throttling assurance cannot be accepted yet. No additional distinct finding. |
| API documentation exposure decided per profile | pass | Design operator-surface decision and `application.properties` explicitly retain `/v3/api-docs` and `/swagger-ui.html`; filter exempts them per SPEC. No undocumented auth/profile boundary introduced. |
| Release advisory scan with reachability/remediation per finding | n-a | Fresh network scan is release-owned. Builder's `proof/osv-advisories-0b1f0d8.json` records 97 coordinates, zero findings at its recorded query time; artifact inspected, not independently re-queried. Release must rescan exact runtime/image and route any remediation with dates; reachability alone is not a fix. |
| Audit has no update/delete path; write joins change transaction | pass | `audit/AuditLog.java` static insert only, called by transactional link service; no audit UPDATE/DELETE product path. Existing audit completeness/failure rollback tests pass in fresh full gate. |
| Secrets and container privileges/filesystem | pass | All candidate changes read: no secret added. Dockerfile keeps uid 10001; compose root read-only, named data volume and bounded tmpfs, localhost publish. Image contents/runtime inspect not independently executed here; AC-22/23 proof remains release-owned. |

## Findings and proof item 11

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| SEC-01 (= CR-01) | HIGH | `src/main/java/dev/urlshort/web/RateLimiter.java:95` | Delayed older request resets a legitimately full newer TAT; 120 admissions in 1 ms, [independent actual-class output](proof/rate-boundary-a7c533f.txt). No spoofing or actual clock rollback needed. | Resolve CR-01 and provide a deterministic concurrent time-ordering regression. This is one underlying defect across both reports. |
| CR-03 (carried) | MEDIUM | `src/main/java/dev/urlshort/web/RateLimiter.java:65` | Supported one-hour backward step leaves 10,000 fully refilled clients resident after 61 s and another limited request. Same probe/output. | Repair cleanup time handling in passing with CR-01; MEDIUM alone does not fail the packet. |
| QA-OPR-02 (retained) | LOW | `src/main/resources/application.properties:42`; slice `proof/qa-prometheus-a7c533f.txt:9` | Default disk gauges expose service working-directory path. No peer, forwarded address or other prohibited client value appears in scrape. | **Retain the default gauges in this loopback operator scope.** No slice repair required; revisit before expanding exposure. This installation metadata residual is explicitly accepted, not silently classified as client PII. |

| Proof-item-11 obligation | Status | Explicit review verdict |
|---|---|---|
| Limiter memory bounded | **fail — MEDIUM CR-03** | Normal forward-clock cleanup passes (unit release test); backward-clock support stalls the sweep deadline and defeats the stated previous-61-seconds retention bound during ongoing traffic. State depends on recently admitted distinct clients, with no allocation while idle; no absolute client-cardinality cap is claimed. Do not mark this proof item unconditionally accepted yet. |
| No client address in a log, metric, response or stored row | **pass** | Limiter does not log/persist its key or use it in metric tags/response. Independent audit of saved response headers/bodies, five QA logs, scrape and link/audit/click snapshots found zero peer/forwarded address canary occurrences; [counts](proof/qa-audit-a7c533f.json). Existing clicks persist salted hashes only. Scope is derived client identity, while the explicit SPEC rule-6 exception keeps submitted target URLs verbatim in admitted responses/storage. |

## Dependencies and residual risks

| Direct runtime dependency | Resolved version |
|---|---|
| `spring-boot-starter-actuator` | 4.1.1 |
| `spring-boot-starter-data-jdbc` | 4.1.1 |
| `spring-boot-starter-flyway` | 4.1.1 |
| `spring-boot-starter-validation` | 4.1.1 |
| `spring-boot-starter-webmvc` | 4.1.1 |
| `springdoc-openapi-starter-webmvc-ui` | 3.1.1 |
| `com.h2database:h2` | 2.4.240 |
| `micrometer-registry-prometheus` | 1.17.1 |

The last dependency is the sole new runtime dependency and implements the
required Prometheus surface; its Prometheus client resolves to 1.7.0. Existing
patch overrides resolve Tomcat 11.0.25, Jackson 3.1.7 and Jackson 2.21.7.
This sandbox review cannot query advisory databases; release reruns the network
scan. No claim of universal CVE absence is made from an offline dependency tree.

The trusted proxy operator must supply the real proxy set; the default trusts
none. Limits and keys are in memory per instance and reset on restart by design.
Unauthenticated operator endpoints are acceptable only in the recorded deployment
scope; the retained disk gauge path is a LOW residual. Container execution and
corrected shutdown smoke, reference-rate latency and advisory verification remain
release obligations. CR-02's false-pass instrument is a separate code blocker.

## Self-check and verdict

Every changed file is accounted for in the shared 24/24 code ledger. Exact QA
SHA verified, fresh offline gate/dependencies read, threat-model entry points and
every role/extended-checklist item judged with source or evidence. Peer identity
is memory-only in this change; proof-item-11's memory exception is explicit.
No product/test/spec/design edits. Separate security ledger row recorded.
**Combined packet exits failed** until CR-01 and CR-02 are resolved; fix CR-03
in passing, retain the stated LOW, and carry the named release obligations.

## Re-review 1c8b2cff20ad8b73a060bc817c8d0011782f876f

2026-10-03 UTC, same reviewer, packet `qitem-20261003134551-e4b16c7a`.
**PASS. SEC-01/CR-01 and CR-03 fixed; CR-02 also resolved in code review.**
No new blocking security findings. This disposition supersedes the earlier
candidate's FAIL without rewriting its evidence.

All five delta files read; cumulative 26/26-file coverage and the full finding
responses are in the [code re-review](01-code-review.md#re-review-1c8b2cff20ad8b73a060bc817c8d0011782f876f).
Only `RateLimiter` changes production behavior: no new entry point, identity
source, persistence/log/metric sink, exposure setting or runtime dependency.
The rest of the original checklist remains valid for unchanged bytes; the rows
affected by the repair are reassessed explicitly below.

| Checklist item / finding | Status | Fresh evidence and resolution |
|---|---|---|
| Concurrent rate limit, SEC-01 (= CR-01) | **pass — fixed** | Removing the reset branch and reading time inside per-client compute preserves the spent bucket. Unchanged two-thread probe: 60 admissions, overtaken request retry 1, no additional admission. [Output](proof/rate-boundary-1c8b2cf.txt). |
| Enumeration protection | **pass within the stated single-node scope** | Same eight-character base62 keyspace (about 47.6 bits), intended 600/min redirect bucket and 10/s refill; reset bypass removed. Existing limits/spoof tests and fresh ordering regression pass. This is risk reduction, not authorization or a guarantee against distributed enumeration. |
| Memory cleanup, CR-03 | **pass — fixed** | 10,000 post-rollback clients reclaimed after refill; probe leaves only the pre-step client and new client. Lead transition 726 explicitly makes backward steps fail closed; pre-step keys wait for their TAT rather than gaining new budget. |
| Identity, output/log/storage privacy | **pass** | No changed sink. Fresh QA evidence audit finds neither client-address canary in response headers/bodies, five logs, scrape or link/audit/click rows. [Audit](proof/qa-audit-1c8b2cf.json). Existing raw-key memory-only and salted click persistence boundaries remain. |
| Direct dependencies / exposure / credentials | **pass for unchanged surface** | Fresh [offline tree](proof/runtime-dependencies-1c8b2cf.txt) is identical to the first candidate, including all eight direct versions listed above. Build, properties, containers and API document unchanged. No new secret. Advisory database and image verification remain release-owned. |
| Shutdown evidence, CR-02 | **pass — fixed instrument** | Reviewer executed all eight complete/truncated/failed/late response controls, 8/8 expected outcomes; 11,012 ms complete 201 rejects. curl framing result controls delivery credit; both callers enforce R0_OK. Real container drain remains unverified here. |
| QA-OPR-02 disk-path disclosure | **pass with retained LOW** | Same deliberate decision: retain useful default disk gauges in the loopback operator scope. Reassess before expanding exposure; no client-identity leakage found. |
| QA-OPR-03 host locale | **n-a to product security; LOW release prerequisite** | Smoke timestamp helper needs working Perl/Time::HiRes and locale. Reviewer controls used supported `C`; QA's initial unsupported `C.UTF-8` setup failures remain disclosed. Release verifies its host. |

| Proof-item-11 obligation | Verdict | Explicit current record |
|---|---|---|
| Limiter per-client memory is bounded | **PASS** | Forward-clock contract: opportunistic full-bucket release limits state to recent admitted distinct clients; idle traffic adds no keys. Fresh normal-cleanup suite and rollback repair probe pass. Pre-step entries after a backwards clock step remain until catch-up under the explicitly accepted fail-closed operational limit. There is no claimed absolute cardinality cap. |
| No client address reaches a log, metric, response or stored row | **PASS** | The changed class has no output/persistence sink; client identity remains an in-memory key. Independently audited new captured responses, logs, metric scrape and three tables contain zero peer/forwarded canary occurrences. Existing salted click hash and explicit submitted-target URL exception are preserved. |

Fresh full gate passed **165 unit / 155 functional**, zero failures/errors/skips,
100% merged line/branch coverage and Javadoc. All 321 saved QA coverage hashes,
186 current-method trace rows, 2,303 exchanges and 30 log correlations reconcile.
The unchanged original limiter probe passed; its literal old diagnostic labels
are not current measurements. No advisory-network access or container execution
is claimed. These are independent reviewer executions/audits, not an inferred
pass from the builder's response.

Residuals: backward wall-clock steps can temporarily refuse pre-step clients;
disk gauges disclose the operator working directory; smoke needs supported
Perl/locale; final container/drain/advisory and 100/20 req/s latency checks remain
release obligations. None reopens a settled finding or adds a product gate.
Code and security **handoff together**. Both proof-item-11 review rows are now
positive; the separate attributed receipt belongs to QA2 under the slice policy.

Self-check: candidate and five-file delta verified, resolved finding behavior
reproduced, affected security checklist and explicit item-11 obligations judged,
unchanged surface checked by Git objects/dependency-tree equality, review ledger
row appended. Product/tests/SPEC/design and QA evidence were not changed.
