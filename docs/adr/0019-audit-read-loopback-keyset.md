# ADR-0019 — Audit read: loopback by peer address with forwarding headers refused, forwarded-header handling pinned off, keyset pages by write sequence

- Status: proposed by `01-audit-read` (2026-10-03); accepted at that slice's plan-lock
- Date: 2026-10-03
- Slice: `01-audit-read` (mission 02)

## Context

FR-17 asks for a read-only, paginated read of the audit trail. NFR-S6 makes it anonymous and
loopback-only by default. The human-approved mission 02 SPEC excludes any access beyond
loopback, and the slice SPEC (`7b753b7`) requires the following:
- a `403` for any peer that is not loopback, and for any request carrying `X-Forwarded-For` or
  `Forwarded`, whatever the rate limiter trusts (rules 2 and 6, AC-11 to AC-14);
- newest first by write sequence, which is not commit order (rule 4);
- a bounded paging guarantee for writes in flight (rule 5, AC-20);
- a page cost bounded by its own `limit` (*Non-functional*);
- an ADR for where the check sits, the cursor and the ordering key (NFR-M2).

`audit_log` has an identity primary key. It is assigned when a row is inserted, before commit, and
it is the table's only index.

## Decision

- **Where.** `GET /api/audit` is a public API path handled by `audit.AuditController` (A-1). Its
  first statement is the loopback check, so the check runs after `RequestIdFilter` (every `403`
  carries `X-Request-Id` and its `request completed` event), after Boot's observation filter and
  after `RateLimitFilter`. Like every `/api` request, the read is charged to the create budget,
  `403`s included (`03-operate` rule 1). The check also comes before any parameter is parsed or
  the store is touched, so a refused client learns nothing, not even that its `limit` was bad.
  - Methods other than `GET`, `HEAD` and `OPTIONS` are refused by MVC with `405` before any handler
    runs, and carry no trail content.
  - A filter would repeat the path matching for one endpoint.
- **The rule.** A request is admitted only when all three hold:
  - it carries no `X-Forwarded-For` header;
  - it carries no `Forwarded` header;
  - `request.getRemoteAddr()` is non-empty and
    `InetAddress.getByName(remoteAddr).isLoopbackAddress()` holds.

  Everything else is
  `throw new ErrorResponseException(HttpStatus.FORBIDDEN)`, which `ProblemDetailsAdvice` renders as
  `{instance, status: 403, title: "Forbidden"}` with no detail. `HEAD` gets the same status
  without a body.
  - `getRemoteAddr()` is the socket's numeric address (Tomcat does no lookups by default), so
    `getByName` parses a literal and resolves no name.
  - Java parses `::ffff:a.b.c.d` to an IPv4 address, so `127.0.0.0/8`, `::1` and IPv4-mapped
    `127.x` are admitted, and `::ffff:192.0.2.10` is not (probe P1).
  - An unparsable literal (`UnknownHostException`) is refused.
  - No setting widens the rule.
- **Forwarded-header handling is pinned off:** `server.forward-headers-strategy=none` in the shipped
  `application.properties`.
  - With the strategy unset, Boot turns on Tomcat's `RemoteIpValve` whenever it detects a cloud
    platform. On Kubernetes that happens through `KUBERNETES_SERVICE_HOST`. The valve then
    rewrites `getRemoteAddr()` from `X-Forwarded-For` and **removes the header**.
  - Measured with `spring.main.cloud-platform=kubernetes`: a request from `127.0.0.1` carrying
    `X-Forwarded-For: 198.51.100.9` reached the application as `remoteAddr=198.51.100.9` with no
    header. A forged `X-Forwarded-For: 127.0.0.2` was **admitted (`200`)**. With the pin, the
    application saw the peer and the header, and the forged request answered `403` (probe P4, P4b).
  - The pin also keeps `03-operate`'s rate-limit identity what ADR-0015 says: application code reads
    `X-Forwarded-For`. In the shipped deployments (jar, compose) no platform is detected, so their
    behaviour does not change.
- **Order and keyset.**
  - Rows are ordered newest first by `id`, the write sequence of SPEC rule 4.
  - A page is `SELECT … FROM audit_log WHERE id < :before ORDER BY id DESC FETCH FIRST :fetch ROWS
    ONLY`, with `fetch = limit + 1`, under the default read-committed isolation.
  - If `limit + 1` rows come back, the page holds the first `limit` and `next` is the cursor of
    its last row. Otherwise `next` is `null`, which happens exactly when no earlier committed row
    exists (rule 5).
  - H2 walks the primary key backwards and reads `limit + 1` rows at any depth (P5: `scanCount` 51
    and 52 on 1 000 000 rows), so **no index and no migration** are needed.
- **The cursor.**
  - It is the unpadded base64url encoding of the decimal `id` of the page's last row, for example
    `Mw` for 3.
  - A value that is not base64url, does not decode to digits, or is not a positive `long` answers
    `400` `cursor` `format`.
  - A well-formed cursor that was never issued pages from the position it encodes. That is allowed
    by rule 5, and it can only return rows the endpoint would return anyway.
  - It is not signed: it carries no secret and grants nothing.
- **What a traversal guarantees, by construction.** Positions strictly decrease across pages, so
  no row is returned twice.
  - Every row committed before the first page has an `id` below that page's top, so it is returned
    exactly once.
  - A row written after the first page has a larger `id` and is never reached.
  - A row whose insert was in flight is returned at most once: only if it commits before the
    traversal passes its position.
  - Probe P3 held an insert open across a traversal: the first traversal returned 30 distinct rows
    and no held row; a fresh traversal returned 31, the held row first.
- **Representation.** Rows are returned as stored, with no masking or derivation (rule 3):
  - `occurredAt` (an `Instant`, ISO-8601 with `Z`);
  - `actor`, `action`, `entity`, `entityId`, `requestId`;
  - `before` and `after`, parsed from the stored JSON text into JSON values, with `before` `null`
    when none was stored.

  Nulls are serialised, so the empty page is exactly `{"items":[],"next":null}` (P2a).
- **Read-only.** `audit.AuditTrail` holds the read's only statement, a `SELECT`. `AuditLog` stays
  the only writer, an `INSERT`. A read writes no audit row (A-11).
- **Logs.** No new event. The request's `request completed` line is its record. A store failure
  is the existing catch-all `500` and its ERROR event (class names and one code frame), with no
  `items` or `next` member (AC-21).

## Alternatives

| Alternative | Why not |
|---|---|
| An actuator endpoint on a loopback management port | decided against in SPEC A-1: not in the API document (NFR-M3), and only a real server can test a bind |
| A servlet filter or Tomcat's `RemoteAddrValve` for the check | a filter repeats the path matching; MockMvc never runs a valve, so AC-11 to AC-14 could not be proven in the suite |
| Trusting the rate limiter's client identity | ADR-0015 believes `X-Forwarded-For` from a listed proxy, and rule 2 forbids forwarding headers from granting anything |
| Leaving `server.forward-headers-strategy` unset | P4b: a detected cloud platform admits a forged loopback header |
| Offset paging (`page`, `size`) | shifts while rows are written: repeats and skips (SPEC A-3, AC-8) |
| Ordering by `occurred_at` | the service clock can step (SPEC A-10) |
| A descending index on `id` | the primary key already serves both directions (P5) |
| A signed cursor | rule 5 needs only well-formedness; the cursor grants nothing |

## Consequences

- **The loopback rule depends on two things,** both stated in `docs/DESIGN.md` §3:
  - the connection address being the client's;
  - forwarded-header handling staying off.

  An Operator who sets `server.forward-headers-strategy` (or `SERVER_FORWARDHEADERSSTRATEGY`)
  to `native` or `framework` voids it. A local relay that adds no forwarding header looks like a
  local Operator (SPEC rule 2, review RQ-04). The deployment must not relay `/api/audit`, or the
  relay must add a forwarding header, which then refuses.
- **Tests.** A real-Tomcat journey with `spring.main.cloud-platform=kubernetes` proves the pin by
  effect: without it the forged request is admitted (P4b). A test also reads the shipped file for
  `none`.
- **Page cost.** `limit + 1` index reads, at most 101 rows. No count and no filter.
- **Write sequence is not commit order.** Two creates in flight can commit in the opposite order
  to their `id`s (`docs/review/01-audit-read/proof/commit-order.txt`). The read states this and
  does not fix it; that would be a write-side change, out of scope.
- **Moving to PostgreSQL:** `FETCH FIRST :n ROWS ONLY` with a parameter and a backward
  primary-key scan are both supported. The identity column is portable.
- Verified before implementation: `missions/02-brownfield/slices/01-audit-read/design-probe/output.txt`
  (P1 to P5).
