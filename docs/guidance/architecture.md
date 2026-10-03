# Architecture guidance

The aim is the smallest structure that satisfies the SPEC and stays
changeable. Every section below states the practice, the reason and the
check; the Design Agent applies it in `design.md`, the Review Agent judges
against it in `design_review` and `wave_review`.

## 1. Principles (in priority order)

1. **Doghouse first.** Name the user outcome in one sentence; design the smallest system that delivers it end to end. Machinery that does not protect or deliver a named outcome is removed (the `ponytail` ladder: does it need to exist → is it already here → does the platform do it → one line → minimum code).
2. **Boundaries where change happens.** Separate what changes for different reasons: HTTP contract, domain rules, persistence, cross-cutting concerns. Do not separate what always changes together.
3. **Explicit contracts.** Every boundary has a written contract: API (OpenAPI + problem details), schema (Flyway migrations), events/logs (field names), configuration (properties with defaults).
4. **Fail closed, fail loud.** Invalid input is rejected at the edge; internal failures become problem details without leaking internals; nothing is swallowed.
5. **Observable by design.** Request id, structured logs, health, metrics are part of the first slice, not a later hardening phase.
6. **Decisions are recorded.** A cross-cutting choice gets an ADR (context, decision, consequences, status) before the code that depends on it.

## 2. Structure for a Spring Boot service

Package by feature, thin layers inside each feature:

```
dev.urlshort
├── web/           cross-cutting HTTP concerns: request-id filter, error advice (if ever needed), API docs config
├── <feature>/     e.g. link/, click/, ping/
│   ├── <Feature>Controller   HTTP in/out only: validate, call service, map to response
│   ├── <Feature>Service      the use case: rules, transactions, audit, idempotency
│   ├── <Feature>Repository   Spring Data JDBC interface or JdbcClient-based class
│   └── <Feature>…            records for requests/responses/domain values
└── audit/, config/ …        only when a second feature needs them
```

Checks: a controller contains no business rule; a service does not build HTTP responses; a repository contains no business rule; a feature can be deleted by removing its package and its migration. No `common/`, `util/`, `base/` packages until two features actually share the code.

## 3. API design

- Resource-oriented URLs, plural nouns for collections (`/api/links`), opaque codes for public redirects (`/{code}`).
- Status codes carry meaning: `201` with `Location` on create, `200` on read, `204` on delete, `302` for redirects that must be observable (ADR written by the first redirect slice; ADR-0002 is problem details), `400` validation, `404` unknown, `409` conflict, `410` gone/expired, `429` rate-limited, `405` wrong method.
- Errors are RFC 9457 problem details (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`; add typed extension members (`errors[]` for validation) rather than free text. Never a stack trace or a class name.
- Idempotency: creates that may be retried accept `Idempotency-Key`; deletes and updates are naturally idempotent.
- Versioning: none until a breaking change is unavoidable; then a new resource path, never a header-only flag.
- Pagination for any list that can grow: keyset (`?after=<cursor>&limit=`) rather than offset.
- Every endpoint appears in the OpenAPI document with examples; the document is a committed artifact (`docs/api/openapi.json`), diffed in review.

Check: for each endpoint, the design lists method, path, request, success response, every error response with its status and title, and the headers that matter (`Location`, `X-Request-Id`, `Cache-Control`).

## 4. Data design

- One owner of the schema: Flyway migrations under `src/main/resources/db/migration`; the application never creates tables.
- Constraints are the first line of validation: `NOT NULL`, `UNIQUE`, `CHECK`, foreign keys. Application validation gives better messages; the database guarantees truth.
- Time is `TIMESTAMP WITH TIME ZONE` in UTC; the code uses `Instant`. A `Clock` is injected only when a test needs to control time.
- Identifiers: surrogate `BIGINT` identity for rows; the public short code is a separate unique column; never expose row ids in URLs.
- Every table that matters has `created_at`; mutable tables have `updated_at`; soft delete (`deleted_at`) when history or audit requires it.
- Audit is a table (`audit_log`: `at`, `actor`, `action`, `entity`, `entity_id`, `request_id`, `before`, `after`), written in the same transaction as the change.
- Migrations are immutable once merged; changes are new versions; destructive changes follow expand → migrate → contract, each step its own migration with a written rollback.
- See `databases.md` for the full checklist.

## 5. Cross-cutting concerns

| Concern | Practice | Check |
|---|---|---|
| Request id | one servlet filter, first in the chain, server-issued, on every response header and in the MDC (ADR-0003) | AC with canary that an inbound id is ignored |
| Logging | structured JSON (ECS), one event per line, parameters as fields, no PII (ADR-0004) | log assertion with canaries |
| Errors | platform problem-details handler; project advice only for domain exceptions | functional tests on every error AC |
| Configuration | `@ConfigurationProperties` records with defaults; profiles for environments; no secrets in the repo | properties documented in `DESIGN.md` |
| Health & metrics | Actuator health (liveness/readiness) and metrics; Micrometer counters/timers named `urlshort.<feature>.<thing>` | smoke script asserts health |
| Rate limiting | smallest filter that satisfies the SPEC; trusted-proxy rule for `X-Forwarded-For` explicit | AC for limit and for spoofing |
| Shutdown | graceful shutdown with a bounded phase timeout | compose health probe passes during restart |

## 6. Security and privacy by design

- Validate at every trust boundary (HTTP input, headers, configuration) with allow-lists; reject early with `400`.
- Redirect only to stored, validated `http(s)` targets; never reflect user input into a `Location`.
- No server-side fetch of user-supplied URLs; if one is ever needed, block private ranges and document the policy.
- PII minimisation: hash client IPs with a rotating salt before storing; never log raw IP, user agent, full referrer; audit rows carry no secrets.
- Least privilege for the process (non-root container, read-only filesystem except `data/`).
- A threat model per slice (assets, entry points, STRIDE-lite table) — written by the Design Agent, judged by the Security & Compliance review.

## 7. Reliability

- Timeouts on everything that can wait; no unbounded queues; backpressure through `429`, not through memory.
- Idempotent writes where clients may retry; unique constraints make duplicates impossible rather than unlikely.
- Startup fails fast on bad configuration; the readiness probe stays down until migrations ran.
- Degrade explicitly: a feature that cannot work says so with a problem detail; it does not half-work.

## 8. Design document contract (`design.md`)

Components touched (new/changed, responsibility) · API contract (per endpoint, incl. errors) · Data model & migration (DDL, indexes, rollback) · Sequence (Mermaid `sequenceDiagram` of the journey) · Logging & audit events (names, fields, PII rule) · Threat model (STRIDE-lite table) · Test strategy hints (AC → suite) · Reachability check (AC → component) · Territory · ADRs · Self-check. Brownfield slices add `impact-analysis.md`: impacted modules/endpoints/schema/data flows, blast radius, migration + rollback plan, test impact, compatibility.

## 9. Diagrams that earn their place

Draw mechanism, not decoration: a context/container view of the service (once, in `docs/diagrams/`), a sequence per user journey, an ERD when a table is added. Mermaid in the repo; a diagram that is not updated with the slice is deleted, not kept stale.

## 10. Trade-offs: say them out loud

Every design records the alternatives it rejected and why, in a short table (`option | why not now | what would change the decision`). Reviewers judge the reasoning, not the taste.
