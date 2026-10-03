# Security and compliance review — 05-ci-cd

**PASS — no findings.** Exact candidate `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14`; reviewer `review2-agent@urlshort-factory` (Codex), 2026-10-03. Same packet and three-file coverage as [code review](01-code-review.md). This reviews the workflow change and design threat model, not a new audit of unchanged product code. N-a rows explicitly identify that boundary.

## Checklist

| Item | Status | Evidence |
|---|---|---|
| Stored redirect target; http/https allow-list | n-a | No product delta. Fresh inherited gate and QA jar journey remain regression evidence; workflow creates no redirect endpoint. |
| No server-side fetch of user URLs / SSRF | n-a | No URL-fetch implementation change. Jobs fetch prescribed actions/build dependencies/base images, not shortened targets. |
| Parameterized JDBC SQL | n-a | No SQL, persistence or migration change. |
| Alias/code charset, length, reserved names, case | n-a | No validation or alias change. |
| Trusted-proxy rule; spoof-resistant limiting | n-a | No limiter/configuration delta. QA smoke observes its explicit trusted-loopback override; fresh inherited proxy tests pass. No new audit-read code is in this candidate. |
| Hashed client storage and privacy-safe logs/audits | n-a | No new client collection or storage path. Audited QA jar log excludes its three named client/input canaries; code review records the scope. Uploaded logs/reports come from synthetic runner traffic. |
| ProblemDetail/no stack, class or SQL leakage; 403/404/410 | n-a | No response code change. QA smoke and fresh inherited error tests pass; no new HTTP error contract. |
| Redirect/API headers and CORS | n-a | No header/configuration change. QA smoke checks redirect no-store and stored Location. |
| Actuator exposure / H2 console | n-a | Unchanged `application.properties` explicitly lists health/info/metrics/prometheus; no console/workflow exposure added. |
| Direct dependency inventory and versions | pass | Fresh offline runtimeClasspath inventory below; no runtime dependency delta. Advisory freshness is unassessed here. |
| SPEC retention/deletion/audit completeness obligations | pass | New obligations concern artifact retention: all three uploads specify 30 days. No new customer-data retention/deletion/audit rule. Actual GitHub retention still pending AC-13. |
| Server-issued request id | n-a | No request filter change; inherited tests and QA request capture remain regression evidence. |
| Enumeration/keyspace and rate-limit numbers | n-a | No code generation or request-budget change; keyspace is not reassessed by this workflow review. |
| OpenAPI/Swagger exposure decision | n-a | Runtime configuration/API document unchanged. CI merely uploads generated OpenAPI failure evidence. |
| Advisory scan and reachability | n-a | No live advisory-database query from this offline review; release owns a fresh network scan and per-advisory assessment. No CVE-free claim. |
| Audit append-only and transaction boundary | n-a | No writer change. Fresh inherited AuditJourneyTest covers stored rows, append-only behavior and rollback. |
| Repo/image secrets, non-root/read-only image | pass | All three new files contain no secret/reference, login or publishing step. Dockerfile/compose unchanged; builder image log retains USER urlshort (uid 10001). No new container policy is claimed. |
| Job least privilege | pass | ci:13, cd:11 contents read only; no job overrides, secret, environment or write permission. Checkout disables persisted credentials (ci:33, cd:31). |
| Untrusted input and pull-request execution | pass | Unfiltered pull_request, no pull_request_target, no expression in any run block; only controlled JAVA_HOME passed through env. No deployment credential available to untrusted changes. |
| Action/cache supply chain | pass | Nine full commit references agree with builder tag captures. Basic cache selected; pinned setup-gradle defaults were checked in design review (read-only off default branch, wrapper validation enabled). Actual action behavior remains AC-13 pending. |
| Bounds and failure evidence | pass | Both jobs 20 minutes, workflow/ref cancellation, explicit bash/pipefail; gate report and smoke-log uploads use always(), 30 days. |
| Smoke network binding and publication | pass | cd:56 calls unchanged jar_mode, which passes --server.address=127.0.0.1; image build is local. No publish/push/deploy step. |

## Dependency inventory

Ran `../../scripts/gw --log <main>/docs/review/05-ci-cd/proof/runtime-dependencies-add7ab5.txt --offline dependencies --configuration runtimeClasspath` from the exact candidate; exit 0. [Full resolved tree](proof/runtime-dependencies-add7ab5.txt).

| Direct runtime dependency | Resolved version |
|---|---|
| spring-boot-starter-actuator | 4.1.1 |
| spring-boot-starter-data-jdbc | 4.1.1 |
| spring-boot-starter-flyway | 4.1.1 |
| spring-boot-starter-validation | 4.1.1 |
| spring-boot-starter-webmvc | 4.1.1 |
| springdoc-openapi-starter-webmvc-ui | 3.1.1 |
| H2 | 2.4.240 |
| micrometer-registry-prometheus | 1.17.1 |

Build file preserves the earlier Tomcat 11.0.25/Jackson 3.1.7 and 2.21.7 remediation overrides. No runtime version changed. No new known vulnerable version identified from the inspected record; this is not a fresh vulnerability assessment. Workflow actions: checkout v7.0.1 (`3d3c42e5…`), setup-java v6.0.1 (`de7274f0…`), setup-gradle v6.4.0 (`3f5f9ada…`), upload-artifact v7.0.1 (`043fb46d…`), with full pins and builder tag evidence in [file checks](proof/code-file-check-add7ab5.txt).

## Verdict, residuals and self-check

MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fixes in passing/backlog requested. Read all three changed files and the design threat model; fresh regression gate and dependency inventory inspected. CI supply-chain, token, input, log and network boundaries meet the SPEC configuration contract.

Residuals: real GitHub behavior and advisory freshness remain unverified; action pinning is not an audit of action internals; anonymous base-image pulls can fail; branch protection remains human-owned. AC-13's pending state is authorized. The unrelated shared-evidence hash changes described in the code review must not be represented as current ready7/7. Combined packet verdict: **handoff to integrate**.
