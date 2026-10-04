# Product requirements baseline — urlshort

**What this is.** The human's brief for the product, written by the operator
from the assignment and the decisions recorded in `PLAN.md` §10. It is the
single list of functional (FR) and non-functional (NFR) requirements that the
missions decompose and the slice `SPEC.md`s refine into acceptance criteria.
It is *not* a SPEC: slice SPECs cite the ids they implement, and the AC ↔ test
traceability (`docs/qa/TRACEABILITY.md`) carries the ids through to tests.

**Provenance — read this column first.** The assignment specifies the product
in one sentence ("a URL shortener service from scratch with core APIs,
analytics, and reliability features", §2) and one scope line ("test and
documentation improvements", §3). Everything else here is interpretation, and
the assignment grades exactly that ("interpret intent, identify ambiguity,
normalize into a clear engineering problem", §4.1). So every row says where it
came from:

| Tag | Meaning | Who can change it |
|---|---|---|
| `stated` | the assignment says it (section quoted) | nobody |
| `derived` | follows from a stated item plus ordinary engineering practice; the reason is given | review (`decomposition_review`, `requirements_review`) with a recorded reason |
| `assumed` | a number or choice we made because something had to be chosen; reversible | **the human** — confirmed or changed at the mission-01 plan-lock (`mission_plan_lock` gate evidence points here) |

Status column: `baseline` (not yet allocated to a slice SPEC), `allocated`
(a slice SPEC cites it), `proven` (traceability row exists and the slice was
accepted), `dropped` (with the decision reference).

## 1. Personas (used verbatim by every SPEC)

| Persona | Goal |
|---|---|
| **Creator** | an API client (person or script) that turns a long URL into a short link and manages it |
| **Visitor** | anyone who opens a short link and expects to land on the target quickly |
| **Analyst** | reads how a link performs (clicks over time, where they came from) — the "marketing" voice in mission 03 |
| **Operator** | runs the service: health, logs, metrics, audit trail, rate-limit tuning |

## 2. Functional requirements

| Id | Requirement | Provenance | Mission → slice | Status |
|---|---|---|---|---|
| FR-1 | A Creator can create a short link for an `http`/`https` target URL and receives the short code and the full short URL. | `stated` §2 "core APIs" + `derived` (http(s) only: a shortener that redirects to `javascript:`/`data:`/`file:` targets is an open redirector) | 01 → create+redirect | proven (01-create-redirect) |
| FR-2 | A Visitor who opens a short link is redirected to the target with an observable redirect (`302`, not cached as permanent) so clicks can be counted. | `stated` §2 "core APIs" + `derived` (302 vs 301 decided in ADR-0002 line of reasoning: clicks must stay observable for analytics) | 01 → create+redirect | proven (01-create-redirect) |
| FR-3 | A Creator can read a link's details (target, code, created time, state) by its code. | `derived` from FR-1 (a created resource is readable) | 01 → create+redirect | proven (01-create-redirect) |
| FR-4 | A Creator can delete (retire) a short link; afterwards visitors get `410 Gone`, not a redirect, and the record is kept for audit. | `derived` from FR-1 + NFR-A1 (audit needs the history; a hard delete loses it) | 01 → create+redirect | proven (01-create-redirect) |
| FR-5 | Invalid input is rejected with a problem-detail error that names the field and the rule (malformed URL, unsupported scheme, over-long URL; the bad-alias case was dropped with FR-11, D7). | `derived` from §4.5 "production-quality code" and §6 "secure" | 01 → create+redirect | proven (01-create-redirect) |
| FR-6 | Unknown short codes answer `404` as a problem detail; wrong methods answer `405`. | `derived` (REST contract completeness) | 01 → create+redirect | proven (01-create-redirect) |
| FR-7 | Every redirect records a click event (time, referrer if present, user-agent *class*, hashed client address) without blocking the redirect. | `stated` §2 "analytics" + NFR-P1 (privacy shapes what is stored) | 01 → analytics | proven (02-analytics) |
| FR-8 | An Analyst can read per-link statistics: total clicks, clicks per day, top referrers. | `stated` §2 "analytics"; the exact shape beyond these three is `assumed` and is the subject of mission 03 | 01 → analytics (v1) · 03 → analytics v2 | proven (02-analytics, v1; 01-analytics-v2, v2) |
| FR-9 | Creates may carry an `Idempotency-Key`; a retried create with the same key returns the first result instead of a second link. | `stated` §2 "reliability features" + `derived` (clients retry; duplicates are a reliability defect) | 01 → reliability | proven (01-create-redirect) |
| FR-10 | Requests above the rate limit (NFR-R2) are answered `429` as a problem detail with `Retry-After`. | `stated` §2 "reliability features" | 01 → reliability | proven (03-operate) |
| FR-11 | A Creator may choose a custom alias instead of a generated code; aliases are unique, limited to a safe charset and length, and reserved words (`api`, `actuator`, `health`, …) are refused. | `derived` from §3 "enhancements" (the brownfield enhancement) + §6 "secure" | dropped | dropped (fast plan, `qitem-20261003052736-7830d02a`, 2026-10-03) |
| FR-12 | A Creator may set an expiry time; after it, visitors get `410 Gone` and statistics still work. | `derived` from §3 "enhancements" | dropped | dropped (fast plan, `qitem-20261003052736-7830d02a`, 2026-10-03) |
| FR-13 | Existing links keep working unchanged across every brownfield change (schema migration included). | `stated` §3 "brownfield … refactors, bug fixes" + §6 "safe change management" | 02 → every slice (impact analysis) | proven (every brownfield slice) |
| FR-14 | A defect found by using the shipped service is fixed with a regression test written first. | `stated` §3 "bug fixes" | 02 → bug fix | proven (03-dogfood-fix) |
| FR-15 | Test and documentation improvements on the shipped code are a first-class change: gaps in `docs/qa/GAPS.md` are closed or re-justified, and `README`/`docs/DESIGN.md` match the shipped behaviour. | `stated` §3 "test and documentation improvements" | 02 → bug fix slice (test/doc improvement items) | proven (03-dogfood-fix) |
| FR-16 | "Better analytics" is turned into decided requirements: what is counted (unique vs raw clicks), retention, privacy, and who reads it (API vs report) are each resolved as `decided` / `assumed` / `parked on human` before anything is built. | `stated` §3 "ambiguous requirements" + §4.1 | 03 → analytics v2 | proven (01-analytics-v2) |
| FR-17 | An Operator can read the audit trail of mutations (who/what/when/before/after/request id) through a read-only, paginated endpoint. | `derived` from NFR-A1 (an audit log nobody can read is not audit-grade) | 02 → audit-read (moved out of mission 01 by the fast plan, `qitem-20261003052736-7830d02a`, 2026-10-03; the brownfield enhancement slice) | proven (01-audit-read) |

## 3. Non-functional requirements

Numbers are proof targets for this prototype on the reference laptop, not
capacity claims. Where a measurement tool does not exist yet the proof column
says so — a gap is recorded, not a checkmark.

| Id | Requirement | Target / rule | Provenance | Proof | Status |
|---|---|---|---|---|---|
| **Performance** | | | | | |
| NFR-L1 | Redirect latency | p95 ≤ 20 ms, p99 ≤ 50 ms at 100 req/s sustained for 60 s, single instance, H2 file DB | `decided (qitem-20261003042553-03ac8b4b)`, was `assumed` (no number in the brief; chosen so a regression is visible) | `scripts/smoke.sh --bench` (open-loop, `tools/bench.mjs`): redirect p95 2.2 ms / p99 3.3 ms at 100 req/s for 60 s (`missions/01-greenfield-core/RELEASE.md` §3.5); p95 3.7 ms / p99 9.5 ms after 01-analytics-v2 changed the redirect path (`missions/03-ambiguous-analytics/RELEASE.md`). One laptop, not a capacity claim | proven (03-operate; re-measured after 01-analytics-v2) |
| NFR-L2 | Create latency | p95 ≤ 50 ms at 20 req/s | `decided (qitem-20261003042553-03ac8b4b)`, was `assumed` | same bench | proven (03-operate) |
| NFR-L3 | Click recording must not slow the redirect | ≤ 2 ms p95 added to the redirect path | `derived` from FR-2/FR-7 (analytics must not slow visitors) | functional test with a slow-store fake + bench | proven (02-analytics) |
| **Reliability** | | | | | |
| NFR-R1 | Health: liveness and readiness probes; readiness is down until migrations ran and the DB answers | always | `stated` §2 "reliability features" | smoke script asserts `/actuator/health/{liveness,readiness}`; compose health probe | proven (03-operate) |
| NFR-R2 | Rate limit | 60 create requests / minute / client and 600 redirects / minute / client, GCRA (ADR-0014), `429` + `Retry-After`; client = remote address behind an explicit trusted-proxy rule | `decided (qitem-20261003042553-03ac8b4b)` numbers, were `assumed`; mechanism `derived` | functional tests at the limit and one over; spoofed `X-Forwarded-For` test | proven (03-operate) |
| NFR-R3 | Graceful shutdown | in-flight requests complete within a 10 s phase timeout; no new connections accepted | `derived` from "reliability" | compose restart while the smoke loop runs: zero non-2xx/3xx | proven (03-operate; the macOS Docker port-forwarder cut is disclosed in `docs/RISKS.md`) |
| NFR-R4 | Durability | committed links survive a process restart (H2 file on a volume); **single-node ceiling stated, not hidden**: no replication, no HA | `derived`; ceiling `assumed` acceptable for the prototype | restart test in the smoke script; ceiling recorded in `docs/RISKS.md` | proven (03-operate) |
| NFR-R5 | Idempotency | FR-9 keys are honoured for 24 h | `decided (qitem-20261003042553-03ac8b4b)` window, was `assumed` | functional test | proven (01-create-redirect) |
| NFR-R6 | Fail closed | an internal error is a `500` problem detail without stack trace or class names; nothing is swallowed | `derived` §4.5 | functional test with an injected failure | proven (01-create-redirect) |
| **Security** | | | | | |
| NFR-S1 | Target URL allow-list | only `http`/`https`, max 2 048 chars, no credentials in the URL, no private/loopback hosts when a server-side fetch is ever added (none is planned) | `derived` §6 "secure" | functional tests per rejected class | proven (01-create-redirect) |
| NFR-S2 | Alias rules | `[A-Za-z0-9_-]{4,32}`, case-sensitive match, reserved list refused, no path separators | `decided (qitem-20261003042553-03ac8b4b)` (charset/length), was `assumed`; FR-11 dropped by the fast plan (`qitem-20261003052736-7830d02a`) | none | dropped |
| NFR-S3 | Request limits | JSON body ≤ 16 KiB, headers at Tomcat defaults, no multipart | `decided (qitem-20261003042553-03ac8b4b)`, was `assumed` | functional test (`413`) | proven (01-create-redirect) |
| NFR-S4 | No secrets in the repository; configuration by environment variables with safe defaults | always | `derived` §6 | review checklist; `gitleaks`-style grep in release prep | proven (01-create-redirect) |
| NFR-S5 | Process least privilege | non-root container user, read-only filesystem except `data/` | `derived` §6 | Dockerfile review + `docker inspect` in release smoke | proven (03-operate) |
| NFR-S6 | No authentication in scope | all endpoints anonymous; the audit endpoint is loopback-only | `decided (qitem-20261003042553-03ac8b4b)` (the brief names no users or tenants), was `assumed`; confirmed at the mission-01 plan-lock | functional tests that the audit endpoint refuses non-loopback peers and forwarding headers; no setting opens it (ADR-0019) | proven (01-audit-read) |
| **Privacy** | | | | | |
| NFR-P1 | Client address and agent handling | client address stored only as a salted hash with a daily-rotated salt; raw IP, full user agent and full referrer query strings never logged or stored | `derived` §6 "secure" + good practice for click data | canary tests: unique UA/header/URL values must not appear in logs or rows | proven (02-analytics) |
| NFR-P2 | Click retention | 90 days, then deleted (nothing is aggregated: mission 03 Q4 A); enforced by a job or migration, not by hope | `decided (qitem-20261003042553-03ac8b4b)` number, was `assumed`; confirmed at the mission-01 plan-lock, built in 02-click-retention; mission 03 kept it (Q4 A) | functional test of the purge; stated in `docs/DESIGN.md` | proven (02-click-retention) |
| **Observability** | | | | | |
| NFR-O1 | Request id | server-issued `X-Request-Id` on every response; inbound ids ignored; the id appears in every log event for that request | `derived` from §4.4 "audit-grade observability and traceability" | functional tests (01-ping already proves the mechanism) | **proven in 00-hello** |
| NFR-O2 | Structured logs | JSON (ECS), one event per line, no PII (NFR-P1), no server bind address in event fields | `derived` §4.4 | log-capture tests with canaries (QA-01 lesson) | **proven in 00-hello** |
| NFR-O3 | Metrics | request timers per endpoint, redirect counter, rate-limit rejections counter, DB pool gauges, via Actuator (`/actuator/metrics`, Prometheus format) | `derived` §4.4 | smoke script reads the metric names | proven (03-operate; extended by 01-analytics-v2) |
| **Audit** | | | | | |
| NFR-A1 | Audit row per mutation | one append-only row per create/delete change (alias and expiry changes were dropped with FR-11/FR-12, D7) — actor, action, entity, before/after, request id, time — written in the same transaction as the change | `derived` from §4.4 "audit-grade" and the assignment's "auditing" artifact | functional tests assert the row and that a failed audit write rolls the change back | proven (01-create-redirect) |
| NFR-A2 | The audit table has no update/delete path in the application | always | `derived` | code review checklist + repository test | proven (01-create-redirect) |
| **Maintainability** | | | | | |
| NFR-M1 | 100 % line and branch coverage across unit + functional suites, enforced in the build; honest gaps recorded in `docs/qa/GAPS.md` | always | `stated` (the AI-SDLC artifact list: "100 % coverage target, identify gaps") | `scripts/gw check`; JaCoCo reports per slice | **proven in 00-hello** |
| NFR-M2 | Every cross-cutting decision has an ADR before the code that depends on it | always | `derived` §6 "clarity and defensibility of decisions" | `docs/adr/` index in `docs/DESIGN.md` | **proven in 00-hello** |
| NFR-M3 | Public API documented by a committed OpenAPI document with examples, diffed in review | always | `stated` §4.5 "API/schema definitions" | `docs/api/openapi.json` | proven (01-create-redirect) |
| **Portability / operations** | | | | | |
| NFR-X1 | Runs as one container (`docker compose up --build`) and as a plain jar; configuration by environment; data on a named volume | always | `stated` §5 "runnable end-to-end" + §5 "setup instructions" | release smoke against the container | proven (03-operate) |
| NFR-X2 | Schema changes are versioned Flyway migrations with a written rollback; expand → migrate → contract for destructive changes | always | `stated` §6 "safe change management" | design review checklist; mission 02 migration | proven (02-click-retention, 04-audit-columns) |

## 4. Explicitly out of scope (say it once)

Authentication and user accounts · multi-tenancy · custom domains and TLS
termination (the container listens on plain HTTP behind whatever proxy the
operator chooses) · a web UI · link preview / server-side fetching of targets ·
horizontal scaling and HA (NFR-R4 states the ceiling) · GDPR workflows beyond
NFR-P1/P2 (no data-subject request endpoint) · QR codes, link bundles, A/B
redirects. Anything on this list that a mission wants back goes through the
ambiguity log and a human decision. Dropped from the plan by the human's fast
plan of 2026-10-03 (`qitem-20261003052736-7830d02a`): custom aliases and link
expiry (FR-11, FR-12, NFR-S2); the brownfield enhancement is the audit-trail
read (FR-17) instead.

## 5. Allocation summary

| Mission | FRs | NFRs | Decided by |
|---|---|---|---|
| 01 greenfield core | FR-1…FR-10 | L1–L3, R1–R6, S1, S3–S5, P1, O1–O3, A1–A2, M1–M3, X1 | mission-01 plan-lock confirmed the `assumed` rows (2026-10-03T04:39Z); the fast plan (`qitem-20261003052736-7830d02a`) moved FR-17 and S6 to mission 02 |
| 02 brownfield | FR-13…FR-15, FR-17 (FR-13 impact analysis on every slice; FR-11, FR-12 dropped) | S6, X2, P2 (02-click-retention) (S2 dropped) | mission-02 plan-lock |
| 03 ambiguous analytics | FR-16, FR-8 v2 | P2 (retention revisited), O3 (new metrics), possibly L1 | human decision gate inside the slice |

Counts (by the first tag in each row at baseline): **15 stated**, **21 derived**, **8 assumed** of 44 rows. The eight
`assumed` rows (L1, L2, R2, R5, S2, S3, S6, P2) were confirmed as stated by the
human at the mission-01 plan-lock (`qitem-20261003042553-03ac8b4b`,
2026-10-03T04:39Z) and now carry `decided`; the original tag is kept beside it
for provenance.

## 6. How this document is used

1. `decompose` (orchestration lead) allocates ids to slices and lists them in
   each `slice.yaml`/mission `SPEC.md` "Requirements in scope".
2. `requirements` (Requirements Agent) cites the ids in each slice `SPEC.md`
   and turns them into ACs; an allocated id with no AC is a `requirements_review`
   failure.
3. `qa_check` (QA Agent) carries the id next to the AC in
   `docs/qa/TRACEABILITY.md`; the Status column here moves to `proven` when
   the slice is accepted.
4. A change to a `stated` row is impossible; to a `derived` row needs a review
   note; to an `assumed` row needs the human's recorded decision (gate
   resolution or parked packet), after which the tag becomes `decided (<ref>)`.
