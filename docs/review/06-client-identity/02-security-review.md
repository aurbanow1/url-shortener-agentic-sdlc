# 06-client-identity — security and compliance review

Candidate: `fb63a88a9b92c1fec97ba74686af1a2f30304160`; base `50ad9c3ab9e65baa4100ede1772b514322957fa5`.
Date: 2026-10-04. Reviewer: `review2-agent@urlshort-factory` (Codex), independent of the Claude builder.
**Security verdict: PASS, no findings. Combined packet: FAIL on code-review CR-01.** This security result does not authorize integration past the test defect.

## Context and threat model

Reviewed all ten candidate files listed in [01-code-review.md](01-code-review.md), the design's five refactor threats, and the unchanged security-sensitive consumers. There is no new HTTP entry point. The moved authority retains separate resolved-client and original-connection-peer decisions: no trust-list argument reaches the audit predicate. Confidence is high in the extraction's preservation; actual network topologies and a future framework upgrade remain outside this check.

The fresh reviewer gate passed 268 unit and 322 functional invocations; canonical merged coverage is 584/584 lines and 206/206 branches. [Independent evidence reconciliation](code-verification-fb63a88.json) checks complete raw responses/log joins, exact-candidate custody, hashes and coverage. This audits QA's retained observations; it does not claim the reviewer reran all external QA apps. The separately executed delayed-writer probe exposes a test-helper fault, not a product security failure.

## Checklist

| Item | Status | Evidence and scope |
|---|---|---|
| Redirect uses only stored validated target; scheme allow-list | pass | Unchanged `LinkController` validates through `LinkValidation` before `LinkService`; `RedirectController` takes Location from `links.resolve(code).url()`, preserving bytes and ignoring request query/Host. Validation allows only http/https, rejects javascript/data/file, user-info, non-visible ASCII and malformed hosts. Fresh `LinkCreateJourneyTest`/`RedirectJourneyTest`; paired baseline-origin active/retired captures. |
| No server-side fetch / SSRF | pass | No HTTP client, URL connection or target fetch in production source. Target is stored and emitted in Location only. `ClientIdentity.fromLoopback` preserves existing JDK numeric servlet-peer parsing, not user-URL resolution. |
| Parameterized SQL | pass | No SQL changed. Spring Data JDBC and `JdbcClient.param` own link/audit/click data; ClickStore and AuditLog statement concatenations concatenate fixed SQL literals, never input. Real failed-table audit read yields sanitized 500. |
| Alias/code charset, length, reserved words, case | pass | Custom aliases are out of scope. SecureRandom generates case-sensitive 8-character base62 codes; route accepts `[A-Za-z0-9]{6,32}`. Existing reserved route set is api/actuator/v3/swagger-ui/error; `admin` cannot be generated at length 8 and fails route length. No new route or validator. |
| Spoofed forwarding headers cannot evade limits | pass | `ClientIdentity.clientOf` is the old exact-text trusted-peer/right-to-left XFF parser. Empty trust by default; Forwarded and X-Real-IP ignored. Frozen 60/600 and two-token shared/unrelated controls exercised before/after. Opaque tokens behind an explicitly trusted peer retain SPEC rule 2 treatment; no new validation policy is implied. |
| Limiter memory retention | pass | Unchanged RateLimiter releases full buckets on request-triggered periodic sweeps; retained keys are tied to recently admitted clients (61-second normal-clock horizon), not a hard cap independent of offered unique peers. Backward-clock behavior and concurrent remove semantics remain documented/tested. Identity extraction adds no map or cache. |
| Audit access is direct-peer only, including container overrides | pass | `AuditController` combines `peerIsConnection` and `fromLoopback` before paging/store access. No trusted list; header presence rejects even empty/whitespace values. The guard denies unset/native/framework strategy and either nonblank remote-IP/protocol header setting. Real original/candidate jar replay plus 72 individual/combined blank-setting responses preserve CR-01/cloud behavior. Servlet fixture matrices are explicitly simulated inputs. |
| PII/log hygiene and private client identity | pass | Limiter's raw identity remains transient memory; ClickRecorder passes `ClientIdentity.of(request)` directly to DailySalt and enqueues reduced Click data. No new log/metric event or tag. All 19 controlled app logs exclude checked address, forwarding, full-UA/referrer-path/query/fragment, inbound-ID and target canaries. Captured stats/rows and inherited privacy tests preserve reduced referrer and classified UA. Audit intentionally retains target snapshots, not request headers, salts, passwords or idempotency keys; this is not a promise to classify arbitrary URL content as secret-free. |
| Daily-salt lifetime — explicit private-hash review | pass | Unchanged `DailySalt.select` synchronizes clock/day/key selection; 32 random bytes in memory, HMAC-SHA256, scheduled UTC-day expiry, zero/drop on rotation/close, fresh salt on restart. It has no persistence/log path. Fresh DailySalt tests cover rotation, stale expiry and close. Timer/JVM scheduling and transient in-flight key copies are existing limits, not a claim of instantaneous physical erasure. |
| Hash use — explicit same-day grouping review | pass | Unchanged ClickStore distinct-client subquery groups by `clicked_on` under the selected link; no cross-day visitor union or externally returned hash. ClickRecorder's input remains the charged identity, with the same peer fallback if the limiter was skipped. Fixed-day matrix bodies/rows and trusted-proxy journeys preserve three clicks/two distinct clients. Test-helper robustness is CR-01, not a changed hash result. |
| Error leakage and 404/410/403 distinctions | pass | Existing ProblemDetailsAdvice strips detail and derives instance from the issued UUID; Problems has static field/rule text. Raw controlled and jar records verify status/media/body correlation, including real SQL-failure 500, audit 403, missing 404, retired 410 and budget 429. Error class chains remain log-only, without driver message/value leakage. HEAD has no body by HTTP semantics. |
| Redirect/API cache headers and CORS | pass | 302 remains `Cache-Control: no-store`; audit responses preserve their existing headers. Compared every saved response header, including named AC-14 cases. No CrossOrigin/CORS configuration added or found in production. No blanket no-store promise for APIs outside the design. |
| Actuator and H2 exposure | pass | Unchanged properties expose only health/info/metrics/prometheus, health details never. No H2 console enabled. Fresh exposure/health tests and captured scrapes retain the existing surface. |
| Direct dependencies and advisories | pass | Eight direct runtime dependencies and resolved versions listed below from my offline dependency task; build files and dependency set unchanged. No newly introduced dependency or known new vulnerable version identified. No fresh advisory-database clearance claimed; release owns the networked OSV/reachability check. |
| Retention, deletion and audit obligations | pass | No retention/schema/access change; existing ClickRetentionJourneyTest, restart/scheduled-purge and failure tests pass in the fresh gate, alongside audit rollback/read-only/completeness cases. No new right-to-delete endpoint is promised by this preservation SPEC. Existing V3/V4 audit columns remain intact. |
| Server-issued request ID; inbound canary ignored | pass | Unchanged RequestIdFilter generates UUID before limiter, logs static completion/status and clears MDC; inbound ID never read. All 2,041 controlled raw responses match their complete event joins and valid UUID, with inbound canary absent from logs. |
| Enumeration budget/keyspace | pass | 62^8 = 218,340,105,584,896 possible generated codes; SecureRandom and unchanged collision constraint. Default 600/min redirect and 60/min API budgets bound one resolved client, with exact trust rule above. No resistance to an unlimited distributed attacker is claimed. |
| API-documentation exposure | pass | Existing default `/v3/api-docs` and `/swagger-ui.html` exposure is documented, unchanged and intentionally exempt from limiting. Live/committed OpenAPI equality and original API blob retained; operator production toggles remain available. |
| Audit append-only and transactional mutation | pass | Unchanged AuditLog offers only append INSERT; LinkService create/retire transaction encloses audit append. No audit update/delete source path added. Existing AuditLogTest, rollback and unchanged-row read controls are green. |
| Secrets and container posture | pass | Read all changed files: no credential/key introduced. Dockerfile and compose unchanged: uid 10001, read-only root, persistent data volume plus bounded ephemeral `/tmp`, host loopback publication. Source posture checked; no fresh image scan or Docker run claimed. |

## Runtime dependencies

Independent command from the exact worktree:

```text
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/06-client-identity/runtime-dependencies-fb63a88.txt --offline dependencies --configuration runtimeClasspath
```

Exit 0. Full graph: [runtime-dependencies-fb63a88.txt](runtime-dependencies-fb63a88.txt).

| Direct coordinate | Resolved version |
|---|---|
| `org.springframework.boot:spring-boot-starter-actuator` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-data-jdbc` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-flyway` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.1 |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.1 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.1.1 |
| `com.h2database:h2` | 2.4.240 |
| `io.micrometer:micrometer-registry-prometheus` | 1.17.1 |

The repository's existing Tomcat/Jackson constraints remain unchanged. This offline review cannot query advisory databases; it is not a clean-CVE certification.

## Residual risks and self-check

The design's named residuals remain: a Boot upgrade could add a peer-rewrite trigger and requires revisiting the guard; a future redirect path bypassing the limiter would retain peer fallback instead of proxy resolution. Deployment must configure exact trusted peers correctly. No new runtime performance or network-topology guarantee is added. Saved evidence retains the documented instrumentation, runtime metric and springdoc-duration differences.

No MUST-FIX/HIGH/MEDIUM/LOW security finding. Code CR-01 still fails the combined packet. Candidate exactness, complete changed-file coverage, fresh gate, offline graph and relevant security data flows were checked; the separate code report carries the ten-file ledger and reproduction. One security ledger row accompanies the code row. Only review artifacts were edited. Post-merge proof 16/register upkeep remains downstream and must not be treated as already complete.

## Re-review e40b09541feb0b7555c475baa82587fdd09e4890

2026-10-04, packet `qitem-20261004032752-d162e1b3`. **PASS — no security finding; combined code/security PASS.** One changed file of one reviewed in this return: the new characterization test's wait helper/caller. CR-01 is fixed by independent held-writer controls and the fresh full gate, as recorded in the code re-review.

| Focused check | Status | Evidence |
|---|---|---|
| Security-sensitive production, settings, schema and public API unchanged | pass | Exact product tree `4c945cf11bf38036bba900425dacda9bb8ca7a83`, unit tree/build/API blobs and actual bootJar bytes equal fb63a88. Sole delta is test-side parameterized click-count polling followed by one status-checked HTTP read. No new entry point, policy, stored field or log event. |
| Previous row-per-item checklist and threat-model conclusions | pass | Carried by unchanged production and retained evidence, not a claim to have repeated every security experiment. All 3,490 historical hashes reconcile through explicit coverage archive aliases; current 672 artifact/378 report hashes verify. Fresh affected 159 raw HTTP/log comparisons remain identical. |
| Same identity grouping and private data under writer delay | pass | Reviewer executes actual complete candidate oracle with real three-second writer holds, no trust and P trusted: 3031/3056 ms, one helper HTTP read, zero 429/NPE; one-day three-click/two-visitor/zero-bot and raw-value absence assertions pass. DailySalt/ClickStore are unchanged, so the explicit salt-lifetime and same-day-only hash findings above stand. |
| Dependencies | pass | Fresh `scripts/gw --offline dependencies --configuration runtimeClasspath` exits 0; [runtime-dependencies-e40b095.txt](runtime-dependencies-e40b095.txt) has the identical resolved graph and eight direct versions listed above. No network advisory query; release retains that check. |
| Gate and proof boundaries | pass | Fresh 268 unit + 322 functional; canonical 584/584 lines,206/206 branches; independent code review now resolves CR-01. QA judgments 1–15/17 bind exact e40b095; item16 remains pending for merge/register/current-system updates and QA return under lead1882. |

No new residual risk from this test-only fix. The original Boot-trigger upgrade ceiling, skipped-limiter peer fallback, exact trusted-peer configuration and offline advisory limits remain. Self-check: exact clean candidate confirmed, complete delta read, fresh gate/probe/dependency evidence examined, both ledger resolution rows recorded, no product/test edits by reviewer. **Handoff to integrate** with downstream proof16 custody retained.
