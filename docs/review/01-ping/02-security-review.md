# 01-ping — security and compliance review

Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003010318-807bbf8a`.
Candidate: `f286a10863e4a8081235226f2d56e51ac121b319`, identical to QA and code review.

## Verdict

**PASS — no blocking or non-blocking security findings.** Ready to integrate
this candidate. The complete nine-file code-review inventory was also judged
against the design's threat model and the architecture/database security
guides; no product edits were made. No source call reads client headers or
addresses, fetches a URL, redirects, or accesses product persistence.

Residual limits: this is an offline dependency inventory, not a current CVE
advisory scan; release must perform the network-backed advisory check.
Anonymous, unthrottled ping and clock exposure are accepted SPEC choices.
Packaged artifacts, load/abuse resistance and future persistence are not
proved here. The existing design-owner thread-example follow-up remains
non-blocking and does not reopen the resolved QA-01 finding.

## Checklist

| Item | Status | Evidence |
|---|---|---|
| Redirect only to stored validated HTTP(S) URLs; reject unsafe schemes | n-a | Ping returns a two-field record; no redirect or URL input exists. `PingController.java:16–19`. |
| No server-side fetch / SSRF | pass | Complete main source is application entry point, filter, controller and record; no fetch API or URL processing. |
| Parameterised SQL only | n-a | No repository, query or migration in this slice; no user value reaches the datasource. |
| Alias/code charset, length, reserved words and case | n-a | No alias/code surface. Correlation id is separately constrained by UUID generation and AC3/AC4/AC8. |
| Rate-limit spoofing / trusted proxy | n-a | SPEC explicitly excludes rate limiting. No forwarded-header value is read by the filter/controller; supplied X-Forwarded-For canary absent from live logs. No rate-limit guarantee claimed. |
| PII minimisation, hashing before storage, log/audit hygiene | pass | No stored PII or audit mutation. Filter uses only a server-issued UUID; controller logs constant `ping`. Fresh tests and live probe show id correlation and absence of User-Agent, inbound id, referrer and forwarded-address canaries. Thread-name exclusion prevents the resolved loopback metadata leak. Hashing/rotation not applicable without storage. |
| Error leakage and status semantics | pass | Fresh AC5 and live POST: 405 `application/problem+json`, id present, no trace/class/SQL. Live disabled env and H2 endpoints: 404 ProblemDetails with id. No 403/410 product state is specified for ping. |
| Cache/redirect/API headers; accidental CORS | pass | `X-Request-Id` on all five live responses; cross-origin GET has no `Access-Control-Allow-Origin`. No CORS configuration or annotation in source. SPEC excludes caching headers, so no `no-store` claim is made. |
| Actuator exposure and H2 console | pass | Shipped allow-list `health,info,metrics`; live `/actuator` lists only these endpoint families. `/actuator/env` and `/h2-console/` return 404. No H2-console enablement in configuration. |
| Dependencies and advisories | pass (inventory); advisory status unverified | Offline `runtimeClasspath` resolution succeeded, versions below; no dependencies added by the slice. No specific applicable CVE is established by this inventory. Advisory databases cannot be checked from the offline sandbox; release repeats with network access. |
| SPEC compliance: retention, deletion, audit completeness | n-a | Ping has no persistent data, mutation or retention/deletion obligation. Privacy obligations are covered by AC7/AC8 and independent evidence; no invented audit layer. |

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

Command (exit 0):

```sh
scripts/gw --log docs/review/01-ping/proof/runtime-dependencies-f286a10.txt --offline -p .worktrees/01-ping dependencies --configuration runtimeClasspath
```

[Complete resolved tree](proof/runtime-dependencies-f286a10.txt), including
transitive versions. Versions are observed locally, not a claim that every
dependency is free of known vulnerabilities.

## Independent live probes

Started the same candidate with its shipped logging settings, localhost-only
bind and isolated in-memory H2:

```sh
scripts/gw --log docs/review/01-ping/proof/security-bootrun-f286a10.txt --offline -p .worktrees/01-ping bootRun --args='--server.address=127.0.0.1 --server.port=18083 --spring.datasource.url=jdbc:h2:mem:review-ping'
```

Requests used `scripts/http --silent --show-error --max-time 10 --include
--output <capture> http://127.0.0.1:18083/<path>`. Ping GET additionally sent
`Origin: https://review.invalid`, `User-Agent: review-ua-f286a10`,
`X-Request-Id: review-rid-f286a10`, `X-Forwarded-For: 203.0.113.77`, and
`Referer: https://review.invalid/review-referrer-f286a10`; POST used
`--request POST`. No requests were sent to those header URLs.

| Probe | Observed | Capture |
|---|---|---|
| GET `/api/ping` with canaries and Origin | 200; newly issued id; no permissive CORS response; matching JSON ping event | [ping](proof/security-ping-f286a10.txt) |
| POST `/api/ping` | 405 ProblemDetail, id present, safe body | [post](proof/security-post-f286a10.txt) |
| GET `/actuator` | 200; only health/info/metrics endpoint families | [actuator](proof/security-actuator-f286a10.txt) |
| GET `/actuator/env` | 404 ProblemDetail, id present | [env](proof/security-env-f286a10.txt) |
| GET `/h2-console/` | 404 ProblemDetail, id present | [H2](proof/security-h2-f286a10.txt) |

[Full service log](proof/security-bootrun-f286a10.txt) contains none of the
supplied canaries, forwarded address or checked loopback-address strings.
Parsed the actual response bodies and matching ping log event; the event has
the issued id and no thread-name member. [Audit result](proof/security-audit-f286a10.txt).

The initial sandboxed HTTP calls could not connect; the approved reruns
against the same loopback listener succeeded. PID 82933 was stopped by
SIGTERM, with graceful shutdown and datasource closure observed. The
resulting bootRun task failure reports child exit 143 from this deliberate
stop; it is separate from the successful offline quality gate and dependency
task. No service or database was left running by this review.

## Findings

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| — | — | — | No findings | None |

## Self-check

- Same SHA as QA/code review, all nine changed files accounted for, threat
  model and applicable architecture/database guidance applied.
- Every checklist item has a scoped pass/n-a and evidence; offline advisory
  limitations are explicit rather than claiming a vulnerability-free stack.
- Fresh gate evidence reused from the immediately preceding code review;
  additional live probes cover exposure, headers and privacy without edits.
- Recorded actual responses and complete logs; service stopped gracefully;
  worktree remains clean at the candidate. No push, merge or publication.
- Ledger row records the verdict; handoff goes to the integrator with the
  release advisory-check limitation and existing documentation follow-up.
