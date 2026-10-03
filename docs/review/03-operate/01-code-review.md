# Code review — 03-operate

Candidate: `a7c533ffef55650e5b422377ffe0c4e38d41400c`. Reviewer:
`review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC. Packet:
`qitem-20261003121811-d329afbc`. **FAIL: two HIGH findings, one MEDIUM.**
The accompanying [security review](02-security-review.md) fails on the same
limiter race; both reviews have one combined workflow exit back to implement.

## Context proof

This slice makes the single-instance shortener operable: separate per-client
60/min API and 600/min redirect budgets, private and correlated rejections,
health/metrics, container settings and release smoke instruments. Existing
link, audit and analytics behavior must survive. The accepted shutdown contract
distinguishes dispatched requests, which must complete, from never-dispatched
kernel-backlog losses, which must be counted. This review keeps that distinction.

Primed from the slice SPEC, design, PROGRESS and PROOF, mission decisions,
AGENTS.md, docs/DESIGN.md, ADRs 0014–0017 and the review, Java/Spring, QA,
architecture and database guidance. Confidence: high on implementation and
in-suite evidence; container/release behavior remains unverified here and is
assigned to release by the SPEC. No additional gate or expanded product scope.

## Candidate and coverage

The clean `.worktrees/03-operate` HEAD matches QA's full SHA. The reviewed
`main...slice/03-operate` range has merge base
`f046bbdc0d19e0a262a95436c6905caafe16d2bc`: 24 files, 1,952 insertions,
38 deletions. All 24 files were read, including complete source/test/script
contents and every OpenAPI operation/component. Candidate includes analytics
merge `091ff46`. [Audit record](proof/qa-audit-a7c533f.json) records all 24
candidate file hashes, checked against Git objects.

| Changed file | Verdict |
|---|---|
| `Dockerfile` | Reviewed; existing non-root runtime and updated writable-directory explanation consistent with compose. Runtime proof belongs to release. |
| `build.gradle.kts` | Reviewed; Prometheus registry required by SPEC; existing patch overrides and coverage/Javadoc gates preserved. |
| `compose.yaml` | Reviewed; loopback publish, readiness check, read-only root, bounded tmpfs, named data volume and 20 s stop grace. Release execution pending. |
| `docs/api/openapi.json` | Reviewed; all six operations document the same 429/Retry-After contract; live/committed equality checked. |
| `scripts/smoke.sh` | **CR-02 HIGH**: R0 completion gate accepts a truncated response. Bench workload gap carried from QA. |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` | Reviewed; explicit freeze/advance support, restored by callers. |
| `src/functionalTest/java/dev/urlshort/web/DatabaseDownJourneyTest.java` | Reviewed; controlled DB availability changes readiness but not liveness. |
| `src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java` | Reviewed; health details, metric families, rejection counts and scrape privacy assertions. |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | Reviewed; generated document compared with committed document, 429 contract covered. |
| `src/functionalTest/java/dev/urlshort/web/RateLimitDefaultsTest.java` | Reviewed; shipped limits/configuration binding. |
| `src/functionalTest/java/dev/urlshort/web/RateLimitJourneyTest.java` | Reviewed; budget/refill boundaries, error ordering, exemptions and rejection privacy; concurrency time-ordering case absent, CR-01. |
| `src/functionalTest/java/dev/urlshort/web/RateLimitSettingsJourneyTest.java` | Reviewed; configured budgets exercised through HTTP. |
| `src/functionalTest/java/dev/urlshort/web/ShutdownPhaseDefaultTest.java` | Reviewed; default phase setting asserted; does not claim a real drain proof. |
| `src/functionalTest/java/dev/urlshort/web/TrustedProxyJourneyTest.java` | Reviewed; trusted-peer chain selection and fallback behavior. |
| `src/functionalTest/resources/application-functional.properties` | Reviewed; shipped configuration retained with explicit functional overlays. |
| `src/main/java/dev/urlshort/web/OpenApiConfig.java` | Reviewed; shared documented response built using existing Springdoc facilities. |
| `src/main/java/dev/urlshort/web/RateLimitConfig.java` | Reviewed; configuration registration only. |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java` | Reviewed; canonical path classification, explicit proxy trust, fixed metric tags, ProblemDetail and correct filter order; inherits CR-01. |
| `src/main/java/dev/urlshort/web/RateLimitProperties.java` | Reviewed; bounded positive settings and explicit trusted set. |
| `src/main/java/dev/urlshort/web/RateLimiter.java` | **CR-01 HIGH**, **CR-03 MEDIUM**: stale request time resets spent bucket; backward time step stalls cleanup. |
| `src/main/java/dev/urlshort/web/RequestBodyLimitFilter.java` | Reviewed; moved after limiter so rejected/oversized requests consume the specified budget. |
| `src/main/resources/application.properties` | Reviewed; defaults, health/metrics exposure, 10 s phase, client-path logging suppression and operator documentation. |
| `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | Reviewed; classification/encoding, trust cases, minimal rejection body and tags. |
| `src/test/java/dev/urlshort/web/RateLimiterTest.java` | Reviewed; boundaries, concurrency at a fixed clock, sequential rollback, normal cleanup. Does not cover the two reproduced time-ordering failures. |

## Verification and QA audit

Run in the exact candidate worktree:

```sh
../../scripts/gw --log ../../docs/review/03-operate/proof/code-check-a7c533f.txt --offline check --rerun-tasks
../../scripts/gw --log ../../docs/review/03-operate/proof/runtime-dependencies-a7c533f.txt --offline dependencies --configuration runtimeClasspath
```

The [fresh gate](proof/code-check-a7c533f.txt) passed: all 14 tasks executed,
163 unit and 155 functional invocations, no failures/errors/skips, Javadoc
included. Merged coverage is 441/441 lines and 160/160 branches. Separate unit
coverage is 398/441 lines, 160/160 branches; functional is 406/441 and 129/160.
No threshold reduction or exclusion was introduced.

The evidence audit checked all 321 saved coverage-file hashes; all match.
All 184 source test methods match QA's source inventory and the 184 traceability
rows, with no missing/extra methods. Parameterized XML display names were not
treated as method identifiers. All 2,303 saved HTTP exchanges were parsed;
the 30 rejections have proper status/body/Retry-After and exactly one matching
429 completion event. Live, QA-captured and committed OpenAPI JSON agree.
The QA launcher explicitly substitutes a controlled clock, peer wrapper and
switchable real H2 datasource; those controls do not enter the product.

| SPEC boundary | Assessment |
|---|---|
| AC-1–AC-10, limiter semantics | Sequential/default/proxy behavior evidenced by tests and QA captures. **CR-01 invalidates the concurrency guarantee** despite the green suite. |
| AC-11–AC-12, rejection privacy/logging | Pass for reviewed code and captures; privacy record below. |
| AC-13–AC-19, probes and metrics | Pass for in-suite behavior and audited QA effects; QA's installation-path LOW retained explicitly in security review. |
| AC-20, API document | Pass; all operations include 429 and generated/committed documents agree. |
| AC-21–AC-24, AC-26, container/jar/restart | Configuration and instrument source reviewed. QA jar smoke audited; container execution and final release judgment remain pending. |
| AC-25/AC-28, shutdown | **CR-02 blocks reliance on the shipped R0 instrument.** This is a demonstrated false-pass predicate, not a claim that the normal application truncated a response. |
| AC-27, bench | Mode executes and reports limitations. QA-OPR-01 remains MEDIUM release work: achieved 82.5/16.5 req/s does not establish NFR-L1/L2 at 100/20 req/s. |

## Findings

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| CR-01 | HIGH | `src/main/java/dev/urlshort/web/RateLimiter.java:95` (time sampled at :53) | An older request samples t0 then pauses. At t0+1 ms, 60 requests exhaust its client's API bucket and the next is refused. Resuming the older request treats the valid newer TAT as a backward clock step and resets it: the old request plus 59 more are admitted. **120 admissions in 1 ms, no backward clock movement.** [Actual-class probe](proof/RateLimitBoundaryProbe.java), [output](proof/rate-boundary-a7c533f.txt). | Make time handling and bucket mutation preserve the budget under reordered concurrent requests; do not equate a stale request timestamp with a wall-clock rollback. Add a deterministic regression for this ordering and retain truthful refill/Retry-After behavior. |
| CR-02 | HIGH | `scripts/smoke.sh:78`, `:225`, `:234` (also restart predicate :126) | Independent loopback peer advertises `201`, `Content-Length: 100`, and a request id, then sends **zero body bytes** and EOF. Candidate's unmodified R0 reader and exact final predicate return success; its id is credited as delivered. [Control](proof/smoke-r0-boundary.py), [output](proof/smoke-r0-boundary-a7c533f.txt). | Require the complete normal response before declaring R0 successful or crediting its id, in both drain/restart modes. Demonstrate rejection of truncated/failed responses. Enforce AC-25's overall 10 s completion bound; current :205 only prints elapsed time, while :76/:78 have separate read timeouts. Keep reviewable completion/reconciliation evidence. |
| CR-03 | MEDIUM | `src/main/java/dev/urlshort/web/RateLimiter.java:65` | After a request sets the sweep deadline, roll the supported wall clock back one hour, admit 10,000 new clients, advance 61 s and make another request. All 10,000 full buckets remain: **10,002 stored clients**, deadline still nearly an hour ahead. [Probe/output](proof/rate-boundary-a7c533f.txt). This exceeds the documented previous-61-seconds bound during continued traffic. | Recover the sweep schedule consistently with the limiter's rollback policy; verify stale clients are reclaimed after refill even following the supported rollback. Fix in passing with CR-01; MEDIUM alone would not block. |

Reproduce CR-01/CR-03 in the candidate worktree:

```sh
../../scripts/gw --log ../../docs/review/03-operate/proof/rate-boundary-a7c533f.txt --offline -I ../../docs/review/03-operate/proof/rate-boundary.gradle reviewRateBoundary
```

The probe compiles outside product source against unchanged candidate classes.
Its exit 1 is the intentional CR-01 assertion failure, not a toolchain failure.
CR-03's measured count is printed before that assertion. Reproduce CR-02 from
the root with `python3 docs/review/03-operate/proof/smoke-r0-boundary.py`;
it opens only an ephemeral loopback peer and tests the smoke instrument.
The missing total deadline is source evidence, not an independently timed probe.

## Explicit proof-item-11 record

| Obligation | Verdict | Evidence and scope |
|---|---|---|
| Limiter memory is bounded | **Not fully satisfied — CR-03 MEDIUM.** Normal advancing-clock cleanup passes; supported rollback stalls it. | Actual-class unit cleanup test plus independent rollback count above. Normal state is proportional to recently admitted distinct clients, not a fixed cardinality cap; no new state is added while idle. Do not accept an unconditional 61 s bound yet. |
| No client address in a log, metric, response or stored row | **PASS for limiter-derived client identity.** | Filter :53–82 emits only fixed budget tags, server UUID and wait; no persistence calls. Audited QA logs, scrape, 2,303 response headers/bodies and link/audit/click snapshots contain neither peer canary `10.77.77.77` nor forwarded canary `192.0.2.201`; detailed counts in the audit JSON. Click persistence retains the existing salted hash. SPEC rule 6 preserves intentionally submitted target URLs in admitted link responses/storage; this verdict does not redefine that contract. |

## Ponytail review

Lean already. Ship.

That is the complexity-only verdict: no speculative layer or dependency to cut.
The overall correctness/security verdict remains **FAIL** on CR-01/CR-02.

## Merge readiness and residual work

Return to implement for the two HIGHs. Expect CR-03 fixed alongside limiter
time handling. Carry QA-OPR-01 to release and retain QA-OPR-02 as the explicitly
accepted operational LOW described in security review. Container execution,
latency at the prescribed rate and fresh network advisory checks remain release
obligations, not proven by this gate. Proof item 11 has an explicit privacy pass
and memory exception; it is not an unconditional clean receipt.

## Self-check

Exact candidate and clean worktree verified; 24/24 changed files reviewed;
fresh offline gate and direct dependencies read; two blocking defects reproduced
against unmodified candidate behavior. All findings have location, consequence,
evidence and required change. QA evidence reconciled independently. Only
`docs/review/` authored; product, tests, SPEC and design untouched. Both review
rows recorded in `docs/review/REVIEW-LEDGER.md` with one combined failed exit.
