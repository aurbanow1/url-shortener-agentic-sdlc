---
id: OPR.99.0.3.1
slice: 01-audit-read
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Operator can read the audit trail of every mutation (who, what, when, before, after, request id) through a read-only, paginated endpoint that is loopback-only by default."
depends_on: []
moved-on: 2026-10-03
moved-from: 01-greenfield-core
---

# Slice 01 — Audit trail read (moved from mission 01 as its `04-audit-read` by the fast plan of 2026-10-03; prerequisite: mission 01 shipped the `audit_log` table)

## Intent

An Operator can read the audit trail of every mutation (who, what, when, before, after, request id) through a read-only, paginated endpoint that is loopback-only by default.

Since mission 01 every create and retire writes an append-only audit row in
the same transaction as the change, but nobody can read those rows without
opening the database file: an Operator who wants to know who retired a link,
what it pointed to and which request did it has to stop the service or attach
a database tool. An audit trail nobody can read is not audit-grade. This is
also mission 02's brownfield demonstration: the read is added to a shipped
service whose links, redirects, statistics, rate limit and existing audit rows
must behave exactly as before (FR-13).

## Mini-requirements

### Requirements covered

Allocated by `missions/02-brownfield/SPEC.md` (allocation table; mission
plan-lock `qitem-20261003120551-4e8acd30`, approved by the human 2026-10-03T15:40Z).

| Id | Requirement (short) | Proven by |
|---|---|---|
| FR-17 | an Operator reads the audit trail of mutations (who/what/when/before/after/request id) through a read-only, paginated endpoint | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10, AC-20, AC-21 |
| NFR-R6 (inherited, failure path) | fail closed: a `500` problem detail without stack trace or class names | AC-21 |
| NFR-S6 | every endpoint anonymous; the audit endpoint loopback-only by default | AC-11, AC-12, AC-13, AC-14 |
| FR-13 (cross-cutting, brownfield) | existing links keep working unchanged across the change, schema migration included | AC-17, AC-18; the impact analysis and the unchanged pre-change functional suite in the proof contract |
| NFR-X2 (cross-cutting, if the design adds a migration) | versioned Flyway migration with a written rollback | AC-18; proof contract (artifact) |
| NFR-A2 (inherited, regression) | no update or delete path for audit rows | AC-10 |
| NFR-O1 (cross-cutting) | server-issued `X-Request-Id` on every response, in every log event of the request | AC-15 |
| NFR-O2 (cross-cutting) | structured JSON logs, no PII, no client-controlled values | AC-15, AC-16 |
| NFR-M3 (cross-cutting) | committed OpenAPI document with examples | AC-19; committed-equals-live check in the proof contract |
| NFR-M1 (cross-cutting) | 100 % line and branch coverage, honest gaps | proof contract (artifact, not HTTP) |
| NFR-M2 (cross-cutting) | an ADR before the code that depends on a cross-cutting decision | *Non-functional* obligation and proof contract (artifact, not HTTP) |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Operator (runs the service: health, logs, metrics, audit trail, rate-limit tuning).
- **Secondary:** Creator and Visitor, only as the people whose links must keep working (FR-13) and whose requests produced the rows.
- Not in this slice: Analyst.

### User stories

- As an Operator, I want to read the audit rows newest first, so that I can see what changed most recently.
- As an Operator, I want each row to show who acted, what action on which entity, the state before and after, the request id and the time, so that I can reconstruct a change and find its log lines.
- As an Operator, I want to page through every row that existed when I started reading without missing or repeating any, even while new rows are being written, so that I can read a long history reliably.
- As an Operator, I want the trail readable only from the machine itself, so that link targets and request ids are never exposed to the network.
- As an Operator, I want a failed read to say so plainly, so that I never mistake a broken read for an empty trail.
- As a Creator or Visitor, I want my existing links to keep working exactly as before, so that adding the audit read costs me nothing.

### Acceptance criteria

Every criterion is observable from the public HTTP surface, from the
service's log output, or from stored rows the suite can inspect. "Problem
detail" and its rules (no stack trace, class name, SQL text or submitted value
in the body; `errors[{field, rule, message}]` on validation failures;
regardless of `Accept`) are as defined in `01-create-redirect`'s SPEC
(acceptance-criteria preamble and business rule 8); they are inherited, not
restated. "Loopback client" means a request whose connection (peer) address
is a loopback address and which carries no forwarding header (business rule
2); the functional suite sets the connection address per request, and any
test that shifts the suite clock sends from a dedicated loopback peer address
such as `127.0.0.2` (mission 02 NOTES, standing continuation (d)). "Row
representation" is the JSON object fixed by business rule 3. "N audit rows
exist" may be arranged by any means the suite has (creates spread over
several peer addresses so the inherited 60-per-minute `/api` budget is not
exhausted, or rows seeded directly); the criterion is about reading them.

#### Reading the trail

- **AC-1 — An empty trail is an empty page.** [FR-17]
  GIVEN no audit row exists
  WHEN a loopback client sends `GET /api/audit`
  THEN the status is `200`, the `Content-Type` is `application/json`, and the body is exactly `{"items": [], "next": null}`.

- **AC-2 — A create row is readable as written.** [FR-17]
  GIVEN a Creator created a link whose response carried code `C` and `X-Request-Id` `R`, and the client recorded the instant just before sending and just after receiving
  WHEN a loopback client sends `GET /api/audit`
  THEN the first element of `items` is the row representation with `actor` `anonymous`, `action` `link.create`, `entity` `link`, `entityId` `C`, `before` `null`, `after` an object with `url` equal to the submitted URL and `state` `active`, `requestId` `R`, and `occurredAt` an ISO-8601 instant ending in `Z` within the recorded interval (seconds precision).

- **AC-3 — A retire row carries before and after.** [FR-17]
  GIVEN an active link `C` with target `T` was retired by a request whose response carried `X-Request-Id` `R`
  WHEN a loopback client sends `GET /api/audit`
  THEN the first element of `items` has `action` `link.retire`, `entityId` `C`, `before` `{"url": T, "state": "active"}`, `after` `{"url": T, "state": "retired"}` and `requestId` `R`, and the `link.create` row for `C` follows it.

- **AC-4 — Rows come newest first, in the order they were written.** [FR-17] (sequential writes, each completed before the next starts; rule 4 for concurrent ones)
  GIVEN the suite created links `A`, `B` and `C` in that order and then retired `A`
  WHEN a loopback client sends `GET /api/audit`
  THEN `items` lists, in this order: retire of `A`, create of `C`, create of `B`, create of `A`.

- **AC-5 — Every field matches the stored row.** [FR-17]
  GIVEN audit rows exist from several creates and retires
  WHEN a loopback client reads every page of the trail
  THEN the number of rows returned equals the number of stored audit rows, and for each stored row exactly one returned row carries the same actor, action, entity, entity id, request id, time (seconds precision) and the same before and after content.

#### Pagination

- **AC-6 — Pages follow `next` to the end.** [FR-17]
  GIVEN 45 audit rows exist
  WHEN a loopback client sends `GET /api/audit?limit=20`, then `GET /api/audit?limit=20&cursor=<next>` with each returned `next` until `next` is `null`
  THEN the pages hold 20, 20 and 5 rows; the third page's `next` is `null`; and the 45 rows are distinct and in the order of AC-4.

- **AC-7 — The default and maximum page sizes.** [FR-17]
  GIVEN 150 audit rows exist
  WHEN a loopback client sends `GET /api/audit` (no `limit`) and `GET /api/audit?limit=100`
  THEN the first returns 50 rows with a non-null `next`, and the second returns 100 rows with a non-null `next`.

- **AC-8 — Paging is stable while rows are written.** [FR-17]
  GIVEN 30 audit rows exist and a loopback client has read `GET /api/audit?limit=10` and kept its `next`
  WHEN a Creator creates five more links, and the client then follows `next` until it is `null`
  THEN the client has received each of the original 30 rows exactly once and none of the five new rows; a fresh `GET /api/audit` then starts with the five new rows.

- **AC-9 — Invalid paging parameters are refused naming the field.** [FR-17]
  GIVEN the service is running
  WHEN a loopback client sends `GET /api/audit` with each query below
  THEN the status is `400` as a problem detail whose `errors` array has exactly one element with the `field` and `rule` of that row, and the body does not contain the submitted value.

  | Query | `field` | `rule` |
  |---|---|---|
  | `limit=0` | `limit` | `range` |
  | `limit=101` | `limit` | `range` |
  | `limit=ten` | `limit` | `format` |
  | `cursor=***` | `cursor` | `format` |

#### Read-only

- **AC-10 — Reading changes nothing.** [FR-17, NFR-A2]
  GIVEN audit rows exist, each row's content recorded, and the link they describe recorded
  WHEN a loopback client reads every page of the trail twice, and sends `POST /api/audit`, `PUT /api/audit`, `PATCH /api/audit` and `DELETE /api/audit`
  THEN each of the four writes answers `405` as a problem detail; every recorded row is still present with identical content; the row count is unchanged (reads write no audit row); and the links are unchanged.

#### Loopback only by default

- **AC-11 — A non-loopback client is refused.** [NFR-S6]
  GIVEN the shipped default configuration and existing audit rows
  WHEN a client whose connection address is `192.0.2.10`, and another whose connection address is `10.0.0.7`, each send `GET /api/audit` and `HEAD /api/audit`
  THEN each answers `403` (the `GET` as a problem detail, the `HEAD` without a body), and no response contains any audit row content, any code, URL or request id from the trail, or the client's address.

- **AC-12 — Every loopback address is admitted.** [NFR-S6]
  GIVEN the shipped default configuration
  WHEN clients whose connection addresses are `127.0.0.1`, `127.0.0.2`, `::1` and `::ffff:127.0.0.1` each send `GET /api/audit`
  THEN each answers `200`.

- **AC-13 — Forwarding headers never grant access.** [NFR-S6]
  GIVEN the shipped default configuration, and also a configuration that lists `127.0.0.1` as a trusted proxy for the rate limit
  WHEN a client whose connection address is `127.0.0.1` sends `GET /api/audit` carrying `X-Forwarded-For: 198.51.100.9`, then `X-Forwarded-For: 127.0.0.1`, then `Forwarded: for=198.51.100.9`, and a client whose connection address is `192.0.2.10` sends it carrying `X-Forwarded-For: 127.0.0.1`
  THEN all four answer `403` as a problem detail.

- **AC-14 — No configuration opens the endpoint beyond loopback.** [NFR-S6]
  GIVEN the service started with every operator setting that exists after this slice at a non-default value, including the rate limiter's trusted-proxy list naming `192.0.2.10` and raised rate budgets
  WHEN a client whose connection address is `192.0.2.10` sends `GET /api/audit`, with and without `X-Forwarded-For: 127.0.0.1`
  THEN both answer `403` as a problem detail with no trail content (rule 2; access beyond loopback is out of scope, A-6).

#### Observability and privacy

- **AC-15 — Request correlation on the new paths.** [NFR-O1, NFR-O2]
  GIVEN the service is running with its default logging configuration
  WHEN a client provokes each of these responses: `200` page, `400` invalid `limit`, `403` non-loopback, `405` wrong method
  THEN each response carries a non-empty `X-Request-Id` `R`, the log output produced for that request contains at least one event, and every such event is a single JSON object on one line carrying `requestId` equal to `R`.

- **AC-16 — Audit content and client values stay out of the logs.** [NFR-O2]
  GIVEN the service is running with its default logging configuration, a link created with a target URL holding a canary in its query string, and a refused request from connection address `192.0.2.10` carrying `X-Forwarded-For: 198.51.100.9` and a canary `User-Agent`
  WHEN a loopback client reads the trail (which returns the canary URL in `after`) and the refused request is sent
  THEN no log output produced while handling those requests contains the canary URL, any `before`/`after` content, the `cursor` value, `192.0.2.10`, `198.51.100.9` or the `User-Agent` canary.

#### Brownfield compatibility

- **AC-17 — The shipped behaviour is unchanged.** [FR-13]
  GIVEN the candidate
  WHEN the functional suite of `main` before this slice (`f6dd29e`) runs against it unchanged
  THEN every test passes; in particular create, read, retire, redirect, statistics, rate limit, health and metrics answer exactly as before, and the create and retire audit rows are written exactly as before (`01-create-redirect` AC-22 to AC-25).

- **AC-18 — An existing database upgrades in place and keeps its links and rows.** [FR-13, NFR-X2]
  GIVEN a data directory written by the shipped service before this slice (`main` `f6dd29e`), holding active and retired links, their clicks and their audit rows
  WHEN the candidate starts on that data directory
  THEN it starts, applies any new migration without error, every previously active link still redirects (`302`, same `Location`), every previously retired link still answers `410`, the statistics of a previously clicked link are unchanged, and `GET /api/audit` from a loopback client returns every pre-existing audit row with its original content.

#### API document

- **AC-19 — The live API document describes the audit read.** [NFR-M3]
  GIVEN the service is running
  WHEN a client sends `GET /v3/api-docs`
  THEN its `paths` contain `/api/audit` (get) documenting the `limit` and `cursor` query parameters, `200` with a schema of the page and row representation and at least one example, and `400`, `403` and `500` typed as `application/problem+json`; and every operation already in the document is still present with its responses unchanged.

#### Added after requirements review (numbered after AC-19 so existing references hold)

- **AC-20 — A traversal across an in-flight write neither repeats nor skips committed rows.** [FR-17] (review RQ-01)
  GIVEN 30 committed audit rows, and an audit write that has started but not committed (the suite holds it open; the mechanism is the design's)
  WHEN a loopback client reads `GET /api/audit?limit=10`, the held write then commits, and the client follows `next` until it is `null`; and then a fresh traversal reads every page
  THEN the first traversal returns each of the 30 rows exactly once and the held row at most once, with no row twice; and the fresh traversal returns all 31 rows exactly once, with the held row in its write-sequence position (rule 4).

- **AC-21 — A failed read is a `500` problem detail, never an empty or partial page.** [FR-17, NFR-R6] (review RQ-03)
  GIVEN audit rows exist and the suite makes the next audit read fail in the store (the mechanism is the design's, as `01-create-redirect`'s AC-24)
  WHEN a loopback client sends `GET /api/audit` with a canary value as `cursor` (well-formed) and the store fails, and then sends `GET /api/audit` again after the store recovers
  THEN the first answer is `500` as a problem detail, with no `items` or `next` member and no stack trace, exception class name, SQL text, cursor value or audit content in the body; the log output for that request contains its `requestId` on every event and no exception message, SQL text, cursor value or audit content; and the second answer is `200` with the trail.

### Business rules

1. **The endpoint.** `GET /api/audit` returns one page of the audit trail. It is anonymous (NFR-S6: no authentication anywhere) and read-only: it changes no row and writes no audit row. Methods other than `GET`, `HEAD` and `OPTIONS` answer `405`; `HEAD` answers as `GET` without a body (so a refused `HEAD` is `403`), and `OPTIONS` keeps the framework default (as `01-create-redirect`'s rule 7 and `02-analytics`'s rule 7).
2. **Loopback only by default.** A request is served only when its connection (peer) address is a loopback address (`127.0.0.0/8`, `::1`, or an IPv4-mapped `::ffff:127.x.x.x`) **and** it carries no `X-Forwarded-For` or `Forwarded` header; every other request answers `403` as a problem detail. Forwarding headers can only refuse, never grant. The rate limiter's trusted-proxy list (`03-operate`, ADR-0015) does not change this rule, and **no setting opens the endpoint beyond loopback** (AC-14; access beyond loopback is out of scope, mission 02 SPEC, A-6). **Trust boundary (review RQ-04):** the service sees only the peer address and the request headers. A process on the same host that relays requests to `/api/audit` without adding `X-Forwarded-For` or `Forwarded` is indistinguishable from a local Operator. The deployment must therefore not relay this endpoint through a local proxy, or the proxy must add one of those headers (which then refuses). The shipped deployment has no proxy (the container publishes on loopback, NFR-S5). The operator documentation states this boundary.
3. **Row representation.** Each element of `items` is a JSON object with exactly these fields: `occurredAt` (ISO-8601 UTC instant ending in `Z`), `actor`, `action`, `entity`, `entityId`, `requestId` (strings, as stored), `before` (the stored before-state as a JSON object, or `null` when none was stored) and `after` (the stored after-state as a JSON object). Values are returned exactly as stored; nothing is masked, derived or added.
4. **Order.** Rows are returned newest first by **write sequence**: the strictly increasing position the store assigns to each audit row when it is written, which the shipped rows already carry. Among writes that each complete before the next starts, a later write always comes first, whatever its `occurredAt` (a clock step does not reorder the trail). Write sequence is not commit order. Two writes in flight at once may commit in the opposite order to their sequence, and the shipped rows do not record commit order (review RQ-01, probe `docs/review/01-audit-read/proof/commit-order.txt`). This slice does not change the write side to add it.
5. **Pagination.** The page body is exactly `{"items": [...], "next": <string or null>}`. `limit` is an integer from 1 to 100, default 50. `next` is an opaque cursor; passing it back as `cursor` returns the rows written before the last row of the previous page. `next` is `null` exactly when no row with an earlier write sequence is committed at the time that page is read. **Guarantee, bounded:** a traversal (a first page and the cursors it hands out) returns every row that was committed before its first page was read exactly once, and never returns any row twice. A row written after the first page was read has a later write sequence and is not returned by that traversal (AC-8). A row whose write was in flight while the traversal ran may be returned at most once, or not at all if it commits after the traversal has passed its position (AC-20). A fresh traversal returns it. A `cursor` that is not well-formed answers `400` `cursor` `format` (a well-formed cursor the service never issued is not required to be detected; it pages from wherever it decodes to and can never return a row the endpoint would not otherwise return); `limit` out of range answers `400` `limit` `range`; a non-integer `limit` answers `400` `limit` `format`. Unknown query parameters are ignored. The cursor's content is the design's and is not part of the contract.
6. **Errors** are problem details as `01-create-redirect`'s rule 8 defines, regardless of `Accept`. A `403` reveals nothing about the trail (no count, no row, no hint whether rows exist) and never echoes the client's address or any header value. A read that fails in the store answers `500` (NFR-R6, `01-create-redirect` AC-24's fail-closed contract): never an empty or partial page, and the body carries no `items` or `next` member (AC-21).
7. **Logs.** `01-create-redirect`'s rule 10 applies: every log event of the request carries its `requestId`; no event contains the client address, a forwarding-header value, the `User-Agent`, the `cursor`, or any audit content (`before`, `after`, a URL). The status may be logged.
8. **Existing behaviour is untouched (FR-13).** Every shipped endpoint answers as before; the audit write side (rows, actions, same-transaction rule, append-only) is unchanged; any migration this slice adds is additive, leaves every existing row and link as it was, and carries a written rollback (NFR-X2).
9. **Inherited, not re-specified.** The rate limit applies: `/api/audit` is under `/api`, so it is charged to the create budget (`03-operate` rule 1) and a client over its budget gets that slice's `429`. The `429` joins this operation in the API document as `03-operate` AC-20 requires for every operation.

### Non-functional

Only what this slice must prove.

- **Brownfield (FR-13).** The design step writes `impact-analysis.md` over the shipped `audit_log` table and request path before `design.md` (`docs/guidance/brownfield.md` §2), naming impacted modules, endpoints, schema, data flows, blast radius and link compatibility. AC-17 and AC-18 are the compatibility proof.
- **Migration (NFR-X2).** If pagination needs an index (`01-create-redirect`'s design left "the audit-read slice adds the index its pagination needs"), it is the next free Flyway version (this slice holds the next number first in w1, mission 02 ordered custody), additive, with a written rollback in the migration file. No existing column or row changes.
- **Page cost.** A page read does not scale with the size of the trail beyond its own `limit` rows plus a bounded lookup; the design states how (index or key), and the design review checks it. No latency number is claimed.
- **Security (NFR-S6).** Loopback-only is proven in-suite with per-request connection addresses and headers (AC-11 to AC-14). The proof covers the inputs the service sees, the peer address and the forwarding headers. A headerless local relay is outside it, and rule 2's trust boundary and the operator documentation cover that case (review RQ-04). The security review checks that no path (forwarding headers, trusted-proxy configuration, any setting, `HEAD`, error bodies) exposes trail content to a non-loopback client.
- **Decisions needing an ADR before dependent code (NFR-M2).** The loopback rule (where the check sits relative to the request-id assignment and the rate limiter); the pagination cursor, the write-sequence ordering key and the bounded guarantee of rule 5; the index migration if any.
- **Coverage gate (NFR-M1).** The candidate passes `scripts/gw check` with 100 % line and branch coverage on the merged unit and functional execution data; honest shortfalls go to `docs/qa/GAPS.md`.
- **Ordered custody (mission 02 w1).** This slice holds `src/main/resources/application.properties` and the next Flyway version number first; `02-click-retention` takes them from this slice's merge. It is the only w1 holder of `docs/api/openapi.json`. Not an acceptance criterion.

### Scope

**In scope**

- `GET /api/audit` with the page and row representation, newest-first order by write sequence, cursor pagination with the stated limits and bounded guarantee, and its `400`/`403`/`405`/`500` paths.
- The loopback-only rule, with no setting that opens it, and the operator documentation of its trust boundary.
- An index migration if pagination needs one, with its rollback.
- The operation in `docs/api/openapi.json`; the impact analysis; unit and functional tests, coverage reports, traceability rows and the proof contract items.

**Explicitly out of scope**

- Filters or search (by entity id, action, actor, request id, time range); sorting options other than newest first; a total count.
- Authentication, API keys, roles, or any access rule other than the loopback check (NFR-S6 decided anonymous).
- Any setting or mechanism that opens the endpoint beyond loopback (mission 02 SPEC, "Not in this mission": "access beyond loopback for the audit read"; A-6).
- Recording commit order on the audit write side, or any other write-side change to make the read order stronger than write sequence (rule 4).
- Exposing the trail as an actuator endpoint or on a separate management port (A-1).
- Any change to the audit write side: new actions, new fields, reads becoming audited, masking stored URLs.
- Export formats (CSV, NDJSON), streaming, or retention or deletion of audit rows.
- Audit rows for click recording or for the click purge (`02-click-retention`).
- The API document's ProblemDetail `errors[]` schema defect W2-01 (mission 02's `03-dogfood-fix`).
- Honouring `X-Forwarded-For`/`Forwarded` to identify a loopback client behind a proxy (A-5).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Public API path, or an actuator endpoint bound to a loopback management port (`slice.yaml` leaves "the design chooses")? | `/api/...` with an in-application loopback check; actuator on a management address | **decided** public API path. The mission allocation gives this slice NFR-M3 for "the audit endpoint in the API document", and actuator endpoints are not in that document; an in-application check is provable in-suite with per-request addresses (NFR-S6's proof column), a management bind needs a real server; and it keeps the problem-detail, request-id and rate-limit contracts the API already has. (No remote-access setting exists after review RQ-02, so the manifest's `application.properties` grant is needed only if the design adds another setting.) |
| A-2 | Path name? | `/api/audit`; `/api/audit-log`; `/api/audit/events` | **assumed** `GET /api/audit` (rule 1). Shortest name that matches the persona's word ("audit trail"); renaming before release costs nothing. |
| A-3 | Page/size (offset) or cursor pagination? | `page`+`size`; an opaque cursor | **assumed** opaque cursor with `limit` (rule 5). The trail grows while it is read; offsets shift and repeat or skip rows (AC-8 forbids both). Opaque keeps the key a design choice. Safe: a page-number view can be added later; a cursor contract cannot be retrofitted onto offset clients without breaking them. |
| A-4 | Page size limits? | fixed size; default 50, max 100; unlimited | **assumed** default 50, max 100, min 1 (rule 5). Bounds the cost of one read (*Non-functional*); a value outside answers `400` rather than being clamped, so a client learns the rule. Safe: raising the cap is additive. |
| A-5 | Which address does "loopback-only" check, given `03-operate`'s trusted-proxy rule? | the connection address; the forwarded client behind a trusted proxy; either | **assumed** the connection address, and any `X-Forwarded-For`/`Forwarded` header refuses (rule 2). A local reverse proxy connects from `127.0.0.1` and forwards remote clients; checking the address alone would expose the trail to the whole network behind such a proxy, and trusting the forwarded value would let a forged header grant access. Refusing on the header covers both, and a local Operator's `curl` sends neither header. Safe: strictly narrower than either alternative. Limit (review RQ-04): a local relay that adds no forwarding header looks like a local Operator; rule 2 states the deployment boundary rather than claiming the check covers it. |
| A-6 | How does an Operator open the endpoint beyond loopback (NFR-S6 "documented operator setting")? | no way; one boolean setting; an allow-list of addresses | **decided** no way (rule 2, AC-14), after requirements review RQ-02. The human-approved mission 02 SPEC lists "access beyond loopback for the audit read (NFR-S6)" under "Not in this mission". The first version assumed a boolean from NFR-S6's "loopback-only by default" and its proof-column phrase "documented operator setting". That widened scope against the mission's recorded boundary, and no explicit decision authorises it. NFR-S6's "documented operator setting" is met by documenting the loopback rule and its trust boundary for the Operator. Opening the endpoint would be a new human decision and a new slice. |
| A-7 | Refusal status for a non-loopback client? | `403`; `404` (hide the endpoint) | **assumed** `403` problem detail with nothing about the trail (rule 6). Honest about why, and the endpoint's existence is public in the API document anyway; `404` would contradict that document. |
| A-8 | Filters (entity id, action, time)? | none; by entity id; several | **assumed** none (Scope). FR-17 asks for a read-only, paginated read; filters are additive later without breaking clients. Narrow is safe. |
| A-9 | Shape of `before`/`after` in the response: JSON objects or the stored strings? | objects; raw strings | **assumed** JSON objects as stored (rule 3), `null` when no before-state. The stored values are JSON written by the service itself (`01-create-redirect` design §5); returning objects is what an Operator reads, and no value is changed. |
| A-10 | Order by `occurredAt`, by write sequence, or by commit order? | time; write sequence; commit order | **decided** write sequence, newest first (rule 4), revised after requirements review RQ-01. The audit time comes from the service clock, which can step. Commit order is not recorded by the shipped rows: the reviewer's probe committed B before A and the stored sequence and times could not show it. Recording it would be a write-side change outside this read-only slice. Write sequence is the stable order the existing rows support, and rule 5 bounds what a traversal promises about writes in flight (AC-20). |
| A-11 | Should reading the trail itself be audited? | yes; no | **decided** no (rule 1). NFR-A1 audits mutations; a read is not one, and auditing reads would make the trail grow with every page and break AC-10. |
| A-12 | Does the rate limit apply to the audit read? | exempt it as an operator surface; inherit `/api` | **decided** inherited (rule 9). `03-operate` rule 1 already classifies every `/api` path; exempting one path is a change to that slice's contract, and 60 reads per minute is ample for one Operator at a terminal. |
| A-13 | Include the stored row's numeric id? | yes; no | **assumed** no (rule 3). FR-17 names who, what, when, before, after, request id; the cursor covers paging and `requestId` covers correlation. Safe: adding a field later is additive. |

No question was parked on `human@kernel`: NFR-S6 (anonymous, loopback-only by
default) was decided at mission 01's plan-lock, FR-17 fixes read-only and
paginated, and every other row has a narrow, reversible default.

## Proof contract

Each item is an observable outcome; the implement and qa_check steps attach
its evidence with `rig proof add`, artifacts under `proof/`.

- [ ] AC-1 through AC-21 are each covered by a named test (functional; AC-17 is the pre-change functional suite run unchanged; AC-18 may be a functional journey or the by-effect capture below, named in the traceability row), tabled criteria as one parameterised test, all green on the candidate SHA with `scripts/gw check`. AC-20 (held write) and AC-21 (failed read) each name their induction mechanism in the traceability row.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional execution data for the candidate SHA (NFR-M1).
- [ ] Unit and functional JaCoCo reports for the candidate are committed under `docs/qa/coverage/01-audit-read/unit/` and `docs/qa/coverage/01-audit-read/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `01-audit-read` mapping AC-1 through AC-21 and business rules 1 to 9 to the tests that prove them, with the `FR`/`NFR` id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `01-audit-read`: "None for this slice" or each honest gap with its compensating check.
- [ ] `impact-analysis.md` exists in this slice folder, written before `design.md`, covering the rows of `docs/guidance/brownfield.md` §2 (FR-13).
- [ ] `proof/` holds a by-effect upgrade capture: the shipped jar from `f6dd29e` run on a fresh data directory, links created, retired and clicked; that jar stopped; the candidate started on the same directory; a redirect, a `410`, the statistics and `GET /api/audit` captured showing the pre-existing rows (AC-18 by effect).
- [ ] `proof/` holds a captured exchange from the running service: `GET /api/audit` from loopback (`200`, two pages via `next`, with `X-Request-Id`) and the same request refused by the forwarding-header rule (`403`), with the JSON log lines for those requests showing `requestId` and no audit content or client values (AC-2, AC-6, AC-13, AC-15, AC-16 by effect).
- [ ] If a migration is added, it is the next free version at plan-lock, additive, and its file carries a written rollback that the design review checked (NFR-X2).
- [ ] `docs/api/openapi.json` is regenerated on the candidate with the audit operation and its example, and QA's diff of it against the candidate's live `/v3/api-docs` (both key-sorted) is empty (NFR-M3).
- [ ] The ADRs listed under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them (NFR-M2).
- [ ] The security review records that no non-loopback path (forwarding headers, trusted-proxy list, any setting, `HEAD`, error bodies) returns trail content, with that proof qualified to the peer address and header inputs the service sees (NFR-S6, rule 2).
- [ ] The operator documentation (`README` or `docs/DESIGN.md`) states that `/api/audit` is loopback-only with no setting to open it, and states the trust boundary of rule 2: do not relay it through a local proxy, or have the proxy add a forwarding header.

## Source material

- `missions/02-brownfield/SPEC.md` — allocation, decision brief, w1 ordered custody, the human's plan-lock (`qitem-20261003120551-4e8acd30`).
- `missions/02-brownfield/NOTES.md` — standing continuation (d) on shifted-clock peers; W2-01 placement.
- `missions/02-brownfield/slices/01-audit-read/slice.yaml` — tier, territory, the design's endpoint choice, the pagination index.
- `docs/REQUIREMENTS.md` — FR-13, FR-17, NFR-S6, NFR-X2, NFR-A1/A2, O1, O2, M1–M3, personas.
- `missions/01-greenfield-core/slices/01-create-redirect/SPEC.md` (rules 7–10, AC-22–AC-25) and `design.md` §5 (audit events); `docs/adr/0008-audit-record-same-transaction.md`; `src/main/resources/db/migration/V1__create_link_and_audit_log.sql`.
- `missions/01-greenfield-core/slices/03-operate/SPEC.md` rules 1, 5, 6 and `docs/adr/0015-client-identity-trusted-proxies.md` (rate-limit classification and trusted proxies).
- `docs/guidance/requirements.md`, `docs/guidance/brownfield.md`.

## Intent visual

N/A — non-visual slice.

## Status

- 2026-10-03 — requirements written: 19 acceptance criteria, 9 business rules, 13 ambiguity rows (10 assumed, 3 decided, none parked). Handed to `requirements_review` at `411a50c`.
- 2026-10-03 — requirements review **FAIL** on `411a50c` (`docs/review/01-audit-read/requirements-review.md`): RQ-01, RQ-02 and RQ-03 HIGH, RQ-04 MEDIUM. All four fixed, see *Review response*. Now 21 acceptance criteria (AC-20, AC-21 added; AC-14 rewritten; AC-1 to AC-19 keep their numbers); 13 ambiguity rows (8 assumed, 5 decided).

## Review response

Review `docs/review/01-audit-read/requirements-review.md` on candidate
`411a50c`: FAIL on RQ-01, RQ-02, RQ-03 (HIGH), with RQ-04 (MEDIUM). Every
finding is answered below; none is disputed.

| Id | Severity | Response |
|---|---|---|
| RQ-01 | HIGH | **Fixed.** Rule 4 no longer promises commit order. Rows are ordered newest first by write sequence, the strictly increasing position the store assigns on write, which the shipped rows carry. The rule says explicitly that this is not commit order and why: the reviewer's probe shows concurrent writes can commit out of sequence, and the rows do not record commit order. Rule 5's paging guarantee is now bounded: rows committed before the traversal's first page are returned exactly once; no row is returned twice; a row in flight during the traversal is returned at most once and is returned by a fresh traversal. New AC-20 makes that boundary observable with a held write. A-10 is revised. Recording commit order (a write-side change) is listed out of scope. Cursor encoding stays a design choice. |
| RQ-02 | HIGH | **Fixed, the narrow way.** The remote-access setting is removed from AC-14, rule 2, A-1, A-5, A-6, the scope, the user story, the non-functional ADR list and the proof contract. AC-14 now proves that no configuration (including a trusted-proxy list naming the client, and a forged `X-Forwarded-For: 127.0.0.1`) opens the endpoint. A-6 is decided from the mission 02 SPEC's "Not in this mission" line. Opening it would need a new human decision. NFR-S6's "documented operator setting" is met by documenting the loopback rule (new proof-contract item). |
| RQ-03 | HIGH | **Fixed.** New AC-21: with the store made to fail, `GET /api/audit` answers `500` as a problem detail with no `items`/`next` and no stack trace, class name, SQL text, cursor value or audit content. Its log events carry the `requestId` and no exception message, SQL, cursor or audit content. The next read after recovery answers `200`. Rule 6 states the fail-closed rule, NFR-R6 joins the coverage table, AC-19 documents the `500`, and the proof contract maps AC-20 and AC-21. |
| RQ-04 | MEDIUM | **Fixed.** Rule 2 now states the trust boundary. The service sees only the peer address and headers, so a headerless local relay is indistinguishable from a local Operator, and the deployment must not relay `/api/audit` through a local proxy or must have the proxy add a forwarding header. The security proof is qualified to those inputs (*Non-functional*, proof contract), A-5 records the limit, and the operator documentation must state it. No authentication or proxy machinery is added. |

## Dependencies

- Mission 01 shipped (`main` `f6dd29e`, product `8e9c065`): the `audit_log` table and its row shape (`01-create-redirect`), the problem-detail, request-id and logging contracts, the rate limiter and trusted-proxy rule (`03-operate`).
- Wave partner `02-click-retention` (no edge): takes `application.properties` and the next Flyway number from this slice's merge.

## Self-check

Recorded 2026-10-03 before the first requirements handoff.

- Every AC observable from outside: AC-1 to AC-14 and AC-19 through HTTP status, headers and bodies; AC-15 and AC-16 through log output; AC-5 and AC-10 through stored rows the suite inspects; AC-17 through the unchanged pre-change suite; AC-18 through a data directory written by the shipped service.
- Error and privacy paths are ACs: `400` (AC-9), `403` with no trail content (AC-11, AC-13), forged forwarding headers (AC-13), `405` (AC-10), no audit content or client values in logs (AC-16), read-only (AC-10).
- Business rules cover the non-obvious logic: loopback definition, the forwarding-header refusal and the local-relay trust boundary, no opening setting, exact row fields, write-sequence order under clock steps, the bounded cursor guarantee for writes in flight, error (including the failed-read `500`) and log content, brownfield untouchability, inherited rate limit.
- Out of scope explicit: eight exclusions, each a plausible addition (filters, auth, actuator, write-side changes, export, click audit, W2-01, forwarded identity).
- Every allocated id covered: FR-17, NFR-S6 with ACs; FR-13 and X2 with AC-17/AC-18 plus the impact analysis and migration items; O1, O2, M3 with ACs; M1, M2 as artifact obligations.
- Every ambiguity resolved: A-1 to A-13, eight `assumed` with the reason each is safe (A-2, A-3, A-4, A-5, A-7, A-8, A-9, A-13), five `decided` (A-1, A-6, A-10, A-11, A-12); none parked. The first version's count ("ten assumed, three decided") was wrong, and A-6 widened scope; both are corrected after review RQ-02.
- Proof contract names coverage (merged and per-suite), traceability, the `GAPS.md` row, the impact analysis, by-effect captures (upgrade, HTTP exchange, log lines), the migration rollback, the API-document diff, the ADRs and the security-review record.
- No design leaked: no class, table, index, column or library is named in a requirement; the path, query parameters and JSON field names are the public contract this SPEC decides; "cursor" is opaque and its content is the design's.
- Consistent with the mission brief and the human's decisions: FR-17 read-only and paginated; NFR-S6 anonymous and loopback-only by default; ordered custody of `application.properties` and the Flyway number; W2-01 left to `03-dogfood-fix`; nothing from the mission's "not in this mission" list.
- `plan-review` run on the finished draft. Engineering lens, three fixes applied: (1) rule 5 said a cursor "the service did not issue" answers `400`, which only a signed cursor could detect; it now requires only well-formed cursors and AC-9 uses a value no encoding accepts (`***`); (2) AC-7 needs 150 rows, more than one client's inherited 60-per-minute `/api` budget, so the preamble now lets the suite seed rows from several peers or directly; (3) confirmed the forwarding-header refusal (AC-13) holds even when the rate limiter trusts `127.0.0.1`. Strategy lens: scope equals FR-17 and NFR-S6; filters, auth and export stay out. UX lens: the Operator's empty (AC-1), error (AC-9, AC-11) and long-history (AC-6, AC-8) states are covered. No executive summary was produced, as on mission 01's slices.
- `rig scope audit --mission 02-brownfield`: no finding against `01-audit-read`; the four low advisories are the untouched placeholder SPECs of `02-click-retention` and `03-dogfood-fix`.
- Review response (before the second handoff, written by the requirements seat on the review findings): RQ-01 to RQ-04 answered as fixed in the *Review response* table; no settled point reopened; AC-1 to AC-19 keep their numbers and AC-20, AC-21 are appended; the self-check lines above were updated to match. Not verified by me: how the suite holds an audit write open for AC-20 without a test-only path in the shipped service; the design names the mechanism, and the design review checks it.
- Not verified by me: whether the functional suite can set an IPv6 and an IPv4-mapped peer address per request (AC-12); whether the pre-change functional suite at `f6dd29e` runs unchanged against the candidate without fixture edits (AC-17), which the impact analysis must check and, if not, explain each edit.
