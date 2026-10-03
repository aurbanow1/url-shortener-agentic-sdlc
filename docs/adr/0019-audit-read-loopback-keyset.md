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
- **The rule.** A request is admitted only when all four hold:
  - nothing rewrites the peer address. Boot's effective forwarded-header strategy is `NONE`
    (`ServerProperties.getForwardHeadersStrategy()`), **and** neither
    `server.tomcat.remoteip.remote-ip-header` nor `server.tomcat.remoteip.protocol-header` has
    text (`TomcatServerProperties.getRemoteip()`). These are exactly the conditions under which Boot
    4.1.1 installs no `RemoteIpValve`: `TomcatWebServerFactoryCustomizer.customizeRemoteIpValve`
    adds it when either header has text or the strategy resolves to native. The values are read
    once, in the controller's constructor (code review, P7);
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
  - **Enforced, not just defaulted** (design review DR-01). The first rule above makes the endpoint
    refuse every request unless the effective strategy is `NONE`. So an operator override
    (`SERVER_FORWARDHEADERSSTRATEGY=native` or `framework`), or a removed pin on a detected
    platform (`null`), closes the read; it does not open it.
  - The reviewer's controls had admitted forged loopback headers under both overrides. Probe P6
    measured `403` for plain and forged requests under `native`, `framework`, unset, and unset +
    `kubernetes`, and plain-only admission under `none`.
  - **The strategy is not the valve's only trigger** (code review on `35590f0`). With the strategy
    pinned to `none`, setting `server.tomcat.remoteip.remote-ip-header` or `…protocol-header`
    still installs the valve. It turns a forged `X-Forwarded-For: 127.0.0.2` into the peer
    address and removes the header, so a guard keyed on the strategy alone admitted it
    (`docs/review/01-audit-read/proof/code-controls-35590f0.txt`).
  - With `remote-ip-header=X-Real-IP`, a forged `X-Real-IP: 127.0.0.2` did the same, a header
    this rule never inspects.
  - Probe P7 (`design-probe/remote-ip-output.txt`), on a real Tomcat, ran the rule with all three
    conditions under each of the three valve settings, `native` and `framework`. Every request was
    `403`, the plain loopback one included. Under the pin alone, plain loopback stayed `200` and
    forged `X-Forwarded-For` `403`.
- **Content negotiation never precedes the rule** (design review DR-02).
  - The mapping has no `produces` condition, which had answered `406` before the guard for a
    strict `Accept`.
  - The handler returns `ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)`, so an
    admitted, valid read is `200 application/json` whatever the `Accept`.
  - Refusals and validation errors are problems whatever the `Accept` (P6). The endpoint answers
    no `406`.
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
| The pin alone, with an override documented as voiding the rule (the first version) | design review DR-01: an override admitted forged loopback headers, and no decision allows a setting to open the endpoint |
| `produces = APPLICATION_JSON_VALUE` on the mapping | design review DR-02: a strict `Accept` got `406` before the guard and the validation |
| Offset paging (`page`, `size`) | shifts while rows are written: repeats and skips (SPEC A-3, AC-8) |
| Ordering by `occurred_at` | the service clock can step (SPEC A-10) |
| A descending index on `id` | the primary key already serves both directions (P5) |
| A signed cursor | rule 5 needs only well-formedness; the cursor grants nothing |

## Consequences

- **The loopback rule depends on the connection address being the client's.** The service
  enforces the part it can see: forwarded-header handling must be off, or the read closes.
  - Setting `server.forward-headers-strategy` (or `SERVER_FORWARDHEADERSSTRATEGY`) to `native`
    or `framework` turns the audit read off: every request is `403`. It does not open it.
  - So does setting `server.tomcat.remoteip.remote-ip-header` or
    `server.tomcat.remoteip.protocol-header` (or `SERVER_TOMCAT_REMOTEIP_REMOTEIPHEADER`,
    `…_PROTOCOLHEADER`) to any value.
  - The rule mirrors Boot 4.1.1's own condition for installing the valve. A Boot upgrade that adds
    another trigger would need adding here; the real-Tomcat journeys for the known triggers
    catch a regression in these three, not a new trigger.
  - What the service cannot see stays the documented boundary (`docs/DESIGN.md` §3; SPEC rule 2,
    review RQ-04): a local relay that adds no forwarding header looks like a local Operator. The
    deployment must not relay `/api/audit`, or the relay must add a forwarding header, which then
    refuses.
- **Later slices that rewrite the client address must keep the headers.** Wave-review finding
  W2D-03 (A-9) and mission 03's `01-analytics-v2` (its A-7) may add a request wrapper in
  `RateLimitFilter` that overrides `getRemoteAddr()` behind a listed proxy (ADR-0015's sketch).
  That stays safe for this endpoint only while the wrapper leaves `X-Forwarded-For` and
  `Forwarded` visible: their presence refuses the request here before the address is read. A
  wrapper that removes or hides them must also leave this check on the raw connection address.
  That slice's design states which.
- **Tests.**
  - MockMvc contexts with `server.forward-headers-strategy=native` and `=framework` prove that
    the overrides close the read (P6).
  - A real-Tomcat journey with `spring.main.cloud-platform=kubernetes` and the shipped pin proves
    plain admission and forged refusal (P4b).
  - Real-Tomcat journeys with the shipped pin plus `server.tomcat.remoteip.remote-ip-header` and
    plus `server.tomcat.remoteip.protocol-header` prove those close the read too, for plain and
    forged requests (P7). MockMvc never runs the valve, so only a real server shows it.
  - A test reads the shipped file for `none`.
  - A unit test builds the controller under each strategy.
  - Strict-`Accept` journeys cover DR-02.
- **Page cost.** `limit + 1` index reads, at most 101 rows. No count and no filter.
- **Write sequence is not commit order.** Two creates in flight can commit in the opposite order
  to their `id`s (`docs/review/01-audit-read/proof/commit-order.txt`). The read states this and
  does not fix it; that would be a write-side change, out of scope.
- **Moving to PostgreSQL:** `FETCH FIRST :n ROWS ONLY` with a parameter and a backward
  primary-key scan are both supported. The identity column is portable.
- Verified before implementation: `missions/02-brownfield/slices/01-audit-read/design-probe/output.txt`
  (P1 to P6) and `remote-ip-output.txt` (P7, after code review).
