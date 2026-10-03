---
id: OPR.99.0.2.1
slice: 01-create-redirect
mission: 01-greenfield-core
status: draft
stage: wip
tier: high
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "A Creator can create, read and retire a short link for a valid http(s) URL, and a Visitor who opens it is redirected (302) or told it is gone (410), with every mutation audited, every error a problem detail, and a retried create with the same Idempotency-Key returning the first link."
depends_on: []
---

# Slice 01 — Create and redirect

## Intent

A Creator can create, read and retire a short link for a valid http(s) URL, and a Visitor who opens it is redirected (302) or told it is gone (410), with every mutation audited, every error a problem detail, and a retried create with the same Idempotency-Key returning the first link.

Today the service answers `GET /api/ping` and nothing else: a Creator has no
way to turn a long URL into a short one, a Visitor has nothing to open, and an
Operator has no record of any change because nothing can change. This slice is
the foundation of the product: the first stored resource, the first public
contract that parses client input, the first audit row and the first committed
API document. Everything in missions 01 to 03 builds on the behaviour fixed
here, so the error, privacy and audit paths are specified as carefully as the
happy path.

## Mini-requirements

### Requirements covered

Allocated by `missions/01-greenfield-core/SPEC.md` (mission plan-lock
`qitem-20261003042553-03ac8b4b`). Every id has at least one acceptance
criterion or, where an id is proven by an artifact rather than an HTTP
exchange, a named obligation in *Non-functional* and the *Proof contract*.

| Id | Requirement (short) | Proven by |
|---|---|---|
| FR-1 | create a short link for an http(s) target, receive code and short URL | AC-1, AC-2, AC-3 |
| FR-2 | redirect with an observable `302`, not cached as permanent | AC-12 |
| FR-3 | read a link's details by code | AC-8, AC-9 |
| FR-4 | retire a link; visitors then get `410`; the record is kept | AC-9, AC-10, AC-11, AC-13 |
| FR-5 | invalid input is a problem detail naming field and rule | AC-4, AC-5, AC-6, AC-20 (the "bad alias" clause is FR-11, mission 02, see Scope) |
| FR-6 | unknown code `404`, wrong method `405`, both problem details | AC-14, AC-15 |
| FR-9 | `Idempotency-Key` on create: a retry returns the first link | AC-17, AC-18, AC-20, AC-21 |
| NFR-S1 | target allow-list: http/https only, ≤ 2 048 chars, no credentials | AC-4 (the private-host clause is not a rule in this slice, see A-13) |
| NFR-S3 | JSON body ≤ 16 KiB, no multipart, headers at server defaults | AC-6, AC-7 |
| NFR-S4 | no secrets in the repository; configuration by environment with safe defaults | AC-3 (the public base URL is the first such setting); no-secrets check in *Non-functional* and the proof contract |
| NFR-R5 | idempotency keys honoured for 24 h | AC-19 |
| NFR-R6 | fail closed: a `500` problem detail without stack trace or class names | AC-24 |
| NFR-A1 | one audit row per mutation, same transaction, failed audit write rolls back | AC-22, AC-23, AC-24 |
| NFR-A2 | no update or delete path for audit rows | AC-25 (observable half); application-code half in *Non-functional* and the proof contract |
| NFR-M3 | committed OpenAPI document with examples | AC-28; committed-equals-live check in the proof contract |
| NFR-O1 | server-issued `X-Request-Id` on every response, in every log event of the request | AC-26 |
| NFR-O2 | structured JSON logs, no PII, no client-controlled values | AC-26, AC-27 |
| NFR-M1 (cross-cutting) | 100 % line and branch coverage, honest gaps | proof contract (artifact, not HTTP) |
| NFR-M2 (cross-cutting) | an ADR before the code that depends on a cross-cutting decision | *Non-functional* obligation and proof contract (artifact, not HTTP) |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Creator (an API client, person or script, that turns a long URL into a short link and manages it); Visitor (anyone who opens a short link and expects to land on the target).
- **Secondary:** Operator (configures the public base URL, reads the audit rows and the logs; the audit *endpoint* is slice `04-audit-read`).
- Not in this slice: Analyst (slice `02-analytics`).

### User stories

- As a Creator, I want to post a long http(s) URL and receive a short code and the full short URL, so that I can share a compact link.
- As a Creator, I want to read a link's details by its code, so that I can confirm what it points to and whether it is still active.
- As a Creator, I want to retire a link, so that a wrong or outdated link stops redirecting while its history remains.
- As a Creator, I want a retried create carrying the same `Idempotency-Key` to return the first link, so that a network retry never produces a duplicate.
- As a Creator, I want invalid input refused with an error that names the field and the rule, so that I can fix the request without guessing.
- As a Visitor, I want opening a short link to redirect me to the target, so that I land where the Creator intended.
- As a Visitor, I want a retired or unknown link to tell me plainly that it is gone or not found, so that I am never sent somewhere unexpected.
- As an Operator, I want every create and retire written as an audit row that carries the request id, so that I can reconstruct what changed and when.
- As an Operator, I want every response to carry a request id that appears in a structured log event containing no client-controlled values, so that I can trace any request without leaking data.
- As an Operator, I want the public API described by a committed OpenAPI document that matches the running service, so that reviewers and clients see one truth.

### Acceptance criteria

Every criterion is observable from the public HTTP surface, from the
service's log output, or from a side effect the test suite can inspect (an
audit row). Each becomes at least one functional test; a tabled criterion
becomes one parameterised test. Ids in brackets are the `docs/REQUIREMENTS.md`
rows the criterion proves. "Problem detail" means: `Content-Type:
application/problem+json`, an RFC 9457 body whose `status` equals the HTTP
status, and no stack trace, exception class name or SQL text anywhere in the
body (business rule 8). "Browser `Accept`" means
`Accept: text/html,application/xhtml+xml,*/*;q=0.8`.

The public endpoints are those fixed by the mission decision brief:
`POST /api/links`, `GET /api/links/{code}`, `DELETE /api/links/{code}`,
`GET /{code}`.

#### Create

- **AC-1 — A valid URL becomes a short link.** [FR-1]
  GIVEN the service is running and the client records the instant just before sending and just after receiving
  WHEN a Creator sends `POST /api/links` with `Content-Type: application/json` and body `{"url": "https://example.com/some/path?q=1"}`
  THEN the status is `201`, the `Content-Type` is `application/json`, the response carries a `Location` header whose path is `/api/links/<code>`, and the body is a JSON object with exactly five fields: `code` matching `^[A-Za-z0-9]{6,32}$`; `shortUrl` equal to the public base URL followed by `/` and `code`; `url` equal to the submitted value byte for byte; `state` equal to `active`; `createdAt` an ISO-8601 instant ending in `Z` that lies within the client's recorded interval (compared at seconds precision).

- **AC-2 — Every create without a key is a new link.** [FR-1]
  GIVEN the service is running
  WHEN a Creator sends `POST /api/links` ten times with the same valid `url` and no `Idempotency-Key` header
  THEN all ten answer `201` and the ten `code` values are pairwise distinct.

- **AC-3 — The short URL is built from the configured public base URL, never from the request.** [FR-1, NFR-S4]
  GIVEN the service is running in one of the two configurations below
  WHEN a Creator creates a link while sending a `Host` header of `evil.example`
  THEN `shortUrl` begins with the expected base and the value `evil.example` appears nowhere in the response.

  | Configuration | Expected base |
  |---|---|
  | no public base URL set (shipped default) | `http://localhost:8080` |
  | public base URL set to `https://sho.rt` through the operator setting (an environment variable with a safe default) | `https://sho.rt` |

- **AC-4 — A target that violates the allow-list is rejected naming the field and the rule.** [FR-5, NFR-S1]
  GIVEN the service is running
  WHEN a Creator sends `POST /api/links` with a JSON object whose `url` is one of the inputs below
  THEN the status is `400` as a problem detail, the body has an `errors` array with exactly one element whose `field` is `url` and whose `rule` is the token of that row, the body does not contain the submitted value, and no audit row is written.

  | `rule` | Inputs (each is one test case) |
  |---|---|
  | `required` | field absent; `null`; `""`; `"   "` |
  | `too-long` | an `https://example.com/` URL padded to 2 049 characters (2 048 is accepted, see business rule 3) |
  | `scheme` | `javascript:alert(1)`; `data:text/html,hi`; `file:///etc/passwd`; `ftp://example.com/`; `example.com/path` (no scheme) |
  | `malformed` | `https://` (no host); `https://exa mple.com/` (space); `https://[bad/` |
  | `credentials` | `https://user:secret@example.com/`; `https://user@example.com/` |

- **AC-5 — A body that is not a JSON object is refused.** [FR-5]
  GIVEN the service is running
  WHEN a Creator sends `POST /api/links` with `Content-Type: application/json` and a body that is empty, truncated (`{"url": `), a JSON array, or an object whose `url` is itself an object
  THEN the status is `400` as a problem detail (an `errors` array is not required on this path).

- **AC-6 — A non-JSON content type is refused.** [FR-5, NFR-S3]
  GIVEN the service is running
  WHEN a Creator sends `POST /api/links` with `Content-Type: text/plain` and body `https://example.com/`, or with `Content-Type: multipart/form-data` carrying a `url` part
  THEN the status is `415` as a problem detail.

- **AC-7 — An oversized body is refused at the limit.** [NFR-S3]
  GIVEN the service is running
  WHEN a Creator sends `POST /api/links` with a JSON object of exactly 16 384 bytes holding a valid `url` and padding in an extra field, and again with the same object grown to 16 385 bytes
  THEN the first answers `201` and the second answers `413` as a problem detail.

#### Read

- **AC-8 — Reading a link returns its details.** [FR-3]
  GIVEN a link was created and its `201` body recorded
  WHEN a Creator sends `GET /api/links/<code>`
  THEN the status is `200`, the `Content-Type` is `application/json`, and the body is identical to the recorded create body (same five fields, same values).

- **AC-9 — A retired link is still readable, with its state.** [FR-3, FR-4]
  GIVEN a link was created and then retired
  WHEN a Creator sends `GET /api/links/<code>`
  THEN the status is `200` and the body has `state` equal to `retired` while `code`, `shortUrl`, `url` and `createdAt` are unchanged from creation.

#### Retire

- **AC-10 — Retiring a link.** [FR-4]
  GIVEN an active link
  WHEN a Creator sends `DELETE /api/links/<code>`
  THEN the status is `204` and the body is empty.

- **AC-11 — Retiring an already retired link is `410`, and is not a second mutation.** [FR-4]
  GIVEN a link was retired
  WHEN a Creator sends `DELETE /api/links/<code>` again
  THEN the status is `410` as a problem detail, and the audit trail holds exactly one retire row for that code.

#### Redirect

- **AC-12 — A Visitor is redirected with a non-cacheable `302`.** [FR-2]
  GIVEN an active link whose `url` is `T`
  WHEN a Visitor sends `GET /<code>` with a browser `Accept`
  THEN the status is `302`, the `Location` header equals `T` byte for byte, and the response carries `Cache-Control: no-store`.

- **AC-13 — A retired link tells the Visitor it is gone.** [FR-4]
  GIVEN a link was retired
  WHEN a Visitor sends `GET /<code>` with a browser `Accept`
  THEN the status is `410` as a problem detail and the response carries no `Location` header.

#### Not found and wrong method

- **AC-14 — An unknown code is `404` on every link operation.** [FR-6]
  GIVEN no link has the code used
  WHEN a client sends one of the requests below
  THEN the status is `404` as a problem detail.

  | Request | Note |
  |---|---|
  | `GET /api/links/nosuchcode1` | well-formed code, never issued |
  | `DELETE /api/links/nosuchcode1` | same |
  | `GET /nosuchcode1` with a browser `Accept` | the Visitor path |
  | `GET /api/links/<40 letters>` | a value that cannot be a code |
  | `GET /favicon.ico` with a browser `Accept` | a path segment outside the code charset |

- **AC-15 — A wrong method is `405`.** [FR-6]
  GIVEN the service is running and an active link exists
  WHEN a client sends `GET /api/links`, `PUT /api/links/<code>`, `PATCH /api/links/<code>`, `DELETE /api/links`, `POST /<code>` or `PUT /<code>`
  THEN the status is `405` as a problem detail.

- **AC-16 — The redirect route does not shadow the existing surface.** (regression guard, business rule 1)
  GIVEN the service is running
  WHEN a client sends `GET /api/ping`, `GET /actuator/health` and `GET /v3/api-docs`
  THEN each answers `200` exactly as before this slice (ping body `status` `ok`; health `status` `UP`; a JSON OpenAPI document).

#### Idempotent create

- **AC-17 — A replay returns the first link.** [FR-9]
  GIVEN a Creator sent `POST /api/links` with `Idempotency-Key: K` and a valid body `B` and received code `C`
  WHEN the same request (`K`, `B`) is sent again, three times, within 24 hours
  THEN each reply is `201` with the same `Location` and the same body as the first reply, and the audit trail holds exactly one create row for `C` and no create row for any other code produced by these requests.

- **AC-18 — The same key with a different URL is refused.** [FR-9]
  GIVEN a Creator sent (`K`, `B`) and received `201`
  WHEN the Creator sends `Idempotency-Key: K` with a valid body whose `url` differs from `B`'s
  THEN the status is `422` as a problem detail, the body has an `errors` array with exactly one element whose `field` is `Idempotency-Key` and whose `rule` is `mismatch`, and no link is created (no new audit row).

- **AC-19 — A key is honoured for 24 hours and not longer.** [NFR-R5]
  GIVEN a Creator sent (`K`, `B`) at service time `t0` and received code `C`, and the suite controls the service's clock
  WHEN the same (`K`, `B`) is sent at `t0 + 24 h − 1 s`, and then at `t0 + 24 h + 1 s`
  THEN the first reply is `201` with code `C`, and the second reply is `201` with a code different from `C`.

- **AC-20 — A malformed key is refused.** [FR-9, FR-5]
  GIVEN the service is running
  WHEN a Creator sends a valid create body with an `Idempotency-Key` header that is empty, 256 characters long, contains a space, or contains a non-ASCII character
  THEN the status is `400` as a problem detail, the body has an `errors` array with exactly one element whose `field` is `Idempotency-Key` and whose `rule` is `format`, and no link is created.

- **AC-21 — A rejected create does not consume the key.** [FR-9]
  GIVEN a Creator sent `Idempotency-Key: K` with an invalid body and received `400`
  WHEN the Creator sends `K` with a valid body `B`
  THEN the status is `201` with a new code `C`, and a further (`K`, `B`) replay answers `201` with code `C`.

#### Audit

- **AC-22 — Create writes exactly one audit row.** [NFR-A1]
  GIVEN the service is running and the client records the instant just before sending and just after receiving
  WHEN a Creator creates a link and the response carries `X-Request-Id` `R` and code `C`
  THEN exactly one audit row exists for the pair (`link`, `C`) with action `link.create`, actor `anonymous`, request id `R`, a null "before" state, an "after" state carrying `url` equal to the submitted URL and `state` `active`, and a time within the recorded interval.

- **AC-23 — Retire writes exactly one audit row.** [NFR-A1]
  GIVEN an active link `C`
  WHEN a Creator retires it and the response carries `X-Request-Id` `R`
  THEN exactly one audit row exists for (`link`, `C`) with action `link.retire`, actor `anonymous`, request id `R`, a "before" state with `state` `active` and an "after" state with `state` `retired`, and the create row from AC-22 is still present and unchanged.

- **AC-24 — A failed audit write rolls the mutation back and fails closed.** [NFR-A1, NFR-R6]
  GIVEN an active link `C` and the suite has made the audit write fail for the next request (the mechanism is the design's)
  WHEN a Creator sends `DELETE /api/links/C`
  THEN the status is `500` as a problem detail whose body contains no stack trace, exception class name or SQL text; afterwards `GET /C` is still `302`, `GET /api/links/C` still shows `state` `active`, and no retire row exists for `C`.

- **AC-25 — Audit rows are append-only under every operation of this slice.** [NFR-A2]
  GIVEN audit rows exist from creating and retiring several links, each row's content recorded
  WHEN the suite runs every operation in this slice once more (create, replayed create, read, retire, repeated retire, redirect, and the `404`/`405`/`400` paths)
  THEN every recorded row is still present with identical content, and the row count grew by exactly the number of successful creates and first retires performed.

#### Observability and privacy

- **AC-26 — Every response carries a request id that reaches a structured log event.** [NFR-O1, NFR-O2]
  GIVEN the service is running with its default logging configuration
  WHEN a client provokes each of these responses: `201` create, `200` read, `204` retire, `302` redirect, `404` unknown code, `410` retired, `400` invalid URL, `405` wrong method
  THEN each response carries a non-empty `X-Request-Id`, and for each one the log output contains at least one event that is a single JSON object on one line carrying `requestId` equal to that header value.

- **AC-27 — No client-controlled value reaches the logs.** [NFR-O2]
  GIVEN the service is running with its default logging configuration, and four canaries that occur nowhere else: a `User-Agent` value, a query-parameter value inside a valid `url`, an `Idempotency-Key` value, and a value inside a `javascript:` URL
  WHEN a Creator creates a link with the first three canaries, a Visitor opens it, and a Creator sends a create whose `url` is the `javascript:` canary and receives `400`
  THEN no log output produced while handling those requests contains any of the four canaries or the client's remote address.

#### API document

- **AC-28 — The live API document describes the slice.** [NFR-M3]
  GIVEN the service is running
  WHEN a client sends `GET /v3/api-docs`
  THEN the status is `200` with a JSON OpenAPI document whose `paths` contain `/api/ping` (get), `/api/links` (post), `/api/links/{code}` (get, delete) and `/{code}` (get); the create operation documents `201`, `400`, `413`, `415` and `422`; the read operation `200` and `404`; the delete operation `204`, `404` and `410`; the redirect operation `302` with its `Location` header, `404` and `410`; every error response is typed as `application/problem+json`; and the create request body and the `201` and `200` responses each carry at least one example.

### Business rules

1. **Short codes.** The service generates the code; the Creator cannot choose it in this slice. A code matches `^[A-Za-z0-9]{6,32}$`, is unique across every link ever created (a retired code is never reused), is matched case-sensitively (`/Abc123` and `/abc123` are different codes), is not predictable from earlier codes (no counter or timestamp encoding), and is never equal to a first path segment the service already serves (`api`, `actuator`, `v3`, `swagger-ui`, `error`) so the redirect route cannot shadow them (AC-16). The exact length and generation strategy inside these bounds is a design decision recorded in an ADR (NFR-M2).
2. **The target is stored verbatim.** No normalisation, no trimming, no case folding, no percent-encoding changes: the `url` returned by read, replayed by an idempotent create, and sent in `Location` is byte for byte what the Creator submitted. Leading or trailing whitespace makes the value fail `malformed` (or `required` if it is all whitespace).
3. **Target validation order and tokens.** Rules are evaluated in this order and the first failure is the one reported, so each input hits exactly one rule: `required` (absent, null, empty, whitespace-only) → `too-long` (more than 2 048 characters; exactly 2 048 is accepted) → `scheme` (the value does not start with `http://` or `https://`, scheme compared case-insensitively) → `malformed` (does not parse as an absolute URL per RFC 3986 with a non-empty host) → `credentials` (the URL has a userinfo component). Private, loopback and link-local hosts are not rejected in this slice (A-13). Unknown JSON fields in the request object are ignored (A-14).
4. **Every create without a key is a new link.** The same URL may be shortened any number of times, each time to a new code; the service never deduplicates by URL. The `Idempotency-Key` is the Creator's tool for "the same create", not the URL.
5. **Idempotency.** A key is a string of 1 to 255 visible ASCII characters (`0x21`–`0x7E`); anything else is `400` `format`. The key space is global (there are no users or tenants, NFR-S6). A key is bound to a link only by a `201`; a request refused with `4xx` or failed with `5xx` leaves the key unbound. Within 24 hours of the binding create, a request with the same key and the same `url` answers `201` with the same `Location` and the link's current representation (identical to the first reply unless the link was retired in between), writes no audit row and creates nothing; the same key with a different `url` answers `422` `mismatch`. After 24 hours the key is treated as never seen. Two concurrent creates with the same key produce at most one link. The header is ignored on every method other than `POST /api/links`.
6. **Retire is one-way.** A link is `active` from creation until the first successful `DELETE`, then `retired` forever: the record is kept, reads answer `200` with `state` `retired`, the Visitor gets `410`, a repeat `DELETE` gets `410`, and nothing in this slice reactivates or deletes it.
7. **Redirect.** Always `302`, never `301`, `307` or `308`. `Location` is the stored `url` exactly. The response carries `Cache-Control: no-store`. A query string or fragment on the short link (`/<code>?utm_source=x`) is ignored and not forwarded. The body of the `302` is not part of the contract. `HEAD` and `OPTIONS` behave as the framework defaults.
8. **Errors are problem details, regardless of `Accept`.** Every non-2xx, non-3xx response from the endpoints in this slice has `Content-Type: application/problem+json` and an RFC 9457 body whose `status` equals the HTTP status, including when the client sends a browser `Accept`. Bodies never contain a stack trace, an exception class name, SQL text, or a value the client submitted. Validation failures (`400` on `url` or `Idempotency-Key`, `422` on key mismatch) add an `errors` array whose elements have `field` (the JSON field or header name), `rule` (a stable token from this SPEC) and `message` (free text, not asserted). `title`, `detail`, `type` and `instance` wording is not asserted except where an AC says so.
9. **Audit.** Every successful mutation (`link.create`, `link.retire`) writes exactly one audit row in the same transaction as the change: if the row cannot be written the change does not happen and the response is `500`. A row carries: actor (`anonymous` in this mission, NFR-S6), action, entity type `link`, entity id (the code), a "before" state (empty on create) and an "after" state, each at least `url` and `state`, the `X-Request-Id` of the mutating request, and the UTC time. Reads, redirects, idempotent replays and rejected requests write no row. The application has no code path that updates or deletes an audit row.
10. **Logs.** Handling any request in this slice produces at least one JSON log event carrying `requestId` equal to the response header. Under the default logging configuration no event contains the client's remote address, the `User-Agent`, the submitted or stored `url`, the `Idempotency-Key`, or any value copied from an inbound header. The code and the status may be logged.
11. **Public base URL.** `shortUrl` is the operator-configured public base URL (default `http://localhost:8080`, overridable by environment, no trailing slash) followed by `/` and the code. The `Host`, `X-Forwarded-Host`, `X-Forwarded-Proto` and `Forwarded` headers are never used to build it.
12. **Time fields.** `createdAt` and the audit time are ISO-8601 UTC instants with the `Z` designator and at least seconds precision; tests compare at seconds precision.

### Non-functional

Only what this slice must prove.

- **Validation limits.** `url` at most 2 048 characters; JSON request body at most 16 KiB (16 384 bytes), larger is `413`; `Idempotency-Key` 1–255 visible ASCII characters; request headers at the embedded server's defaults (no change); no multipart support (`415`).
- **Logging obligations (NFR-O1, O2).** As business rule 10, proven by AC-26 and AC-27 with canaries under the shipped logging configuration.
- **Audit obligations (NFR-A1, A2).** As business rule 9. The "no update or delete path" half of A2 is not observable over HTTP: the design states it, the code review checks it, and a persistence-level test in the unit suite asserts that the audit store offers no update or delete operation. Named in the proof contract.
- **Secrets and configuration (NFR-S4).** No credential, token or private key is committed; the public base URL is the first operator setting of the product and is overridable by an environment variable with the safe default above. Checked by the code review and the release-prep secret scan; named in the proof contract.
- **Decisions needing an ADR before dependent code (NFR-M2).** This slice forces these cross-cutting decisions, each recorded as an ADR (or an amendment to an existing one) before the code that depends on it and indexed in `docs/DESIGN.md` §7: schema and migration conventions for the first migration; `302` with `no-store` versus `301`; short-code generation strategy within business rule 1; audit-record shape and the same-transaction rule; idempotency record storage and the 24 h window; the validation error-body extension (`errors[]`) and whether domain problems get `type` URIs (ADR-0002 left this to the first slice that needs it).
- **Coverage gate (NFR-M1).** The candidate passes `scripts/gw check` with 100 % line and branch coverage on the merged unit and functional execution data; any honest shortfall is written to `docs/qa/GAPS.md`, never configured away.
- **Dependency overrides (binding, mission plan-lock `qitem-20261003042553-03ac8b4b`).** The first commit on branch `slice/01-create-redirect` contains only the dependency overrides (Tomcat embed 11.0.25, Jackson 3.1.7, Jackson 2 constraint 2.21.7 in `build.gradle.kts`), is gated green on its own by `scripts/gw check` before any feature commit, and the builder captures a fresh OSV run for it. Not an acceptance criterion; a proof-contract item.
- **No latency target** is claimed by this slice (NFR-L1/L2 belong to `03-operate`).
- **No rate limiting, no authentication.** Every endpoint is anonymous and unthrottled (NFR-S6 decided; FR-10 is `03-operate`).

### Scope

**In scope**

- `POST /api/links`, `GET /api/links/{code}`, `DELETE /api/links/{code}` and `GET /{code}` with the contracts above, including every error path listed.
- `Idempotency-Key` on create with the 24 h window.
- The target URL allow-list and the request-size limit.
- The first stored link record and the write side of the audit trail (rows for create and retire).
- Validation error bodies that name field and rule; problem details on every error.
- The public base URL as the first operator setting.
- The committed OpenAPI document `docs/api/openapi.json` with examples, generated deterministically with sorted keys so later slices' regenerations diff cleanly (mission shaping rule 5), matching the live `/v3/api-docs`.
- The dependency overrides as the first, separately gated commit (binding constraint above).
- Unit and functional tests, coverage reports, traceability rows, and the proof artifacts in the proof contract.
- Permitted in passing, not required and not a criterion: converting the unit-suite properties file to the profile-overlay pattern (00-hello backlog item 4).

**Explicitly out of scope**

- Custom aliases and alias validation, including the "bad alias" clause of FR-5 and the reserved-word rule for aliases (FR-11, NFR-S2, mission 02).
- Expiry (FR-12, mission 02).
- Recording clicks and reading statistics (FR-7, FR-8, slice `02-analytics`); this slice only promises not to make a redirect hook impossible.
- Rate limiting and `429` (FR-10, slice `03-operate`); health, metrics, container and graceful-shutdown proofs (slice `03-operate`).
- Reading the audit trail over HTTP (FR-17, slice `04-audit-read`); this slice writes rows a later slice reads.
- Listing links (`GET /api/links` is `405`), updating a link (`PUT`/`PATCH` are `405`), reactivating a retired link, hard-deleting anything.
- Deduplicating creates by URL; normalising or rewriting target URLs.
- Rejecting private, loopback or link-local target hosts (A-13).
- Forwarding the short link's query string to the target; building `shortUrl` from `Host` or forwarding headers.
- `HEAD`/`OPTIONS` behaviour beyond framework defaults; trailing-slash variants of the endpoints (`/api/links/`); a landing page at `/`; `robots.txt`; `favicon.ico`.
- Link previews or any server-side fetch of the target (`docs/REQUIREMENTS.md` §4).
- `301`/`307`/`308` redirects; caching headers other than `Cache-Control: no-store` on the `302`.
- Authentication, tenants, CORS, per-client idempotency scopes.
- Latency measurements and load generation.

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Name of the target field in request and response: `url` or `target`? | `url`; `target` | **decided** `url`. `PLAN.md` (plan of record, Core API row) specifies `POST /api/links (url, …)`; `docs/REQUIREMENTS.md` says "target URL" descriptively. One name everywhere. |
| A-2 | May the link representation carry fields beyond `code`, `shortUrl`, `url`, `state`, `createdAt`? | exactly five; allow extras | **decided** exactly five. FR-3 names target, code, created time and state; `shortUrl` is FR-1's "full short URL". Mission 02 adds alias and expiry through its own impact analysis. |
| A-3 | Shape of a generated code? | fixed length; a bounded range; unconstrained | **assumed** `^[A-Za-z0-9]{6,32}$`, a strict subset of the alias namespace NFR-S2 fixes for mission 02 (`[A-Za-z0-9_-]{4,32}`), so generated codes and future aliases share one namespace without collision rules changing. Minimum 6 so links cannot be enumerated cheaply. Exact length and strategy are design (pending ADR in `docs/DESIGN.md` §7). Safe: the bounds are wide, and tightening later is a strict subset. |
| A-4 | Where does the host part of `shortUrl` come from? | the request's `Host`/forwarding headers; an operator setting with a default | **assumed** operator setting, default `http://localhost:8080`, headers never used. A `Host`-derived base lets any client mint short URLs on an attacker's domain (host-header injection); a setting is also NFR-S4's "configuration by environment with safe defaults". Safe: reversible, no data impact. |
| A-5 | What does "not cached as permanent" (FR-2) require on the `302`? | nothing beyond `302`; `Cache-Control: no-store`; `private, max-age=0` | **assumed** `Cache-Control: no-store`. The narrowest header that keeps every click visible to `02-analytics`; relaxing later is additive. |
| A-6 | Should the same URL posted twice yield the same link? | deduplicate by URL; new link each time | **decided** new link each time (business rule 4). FR-9 gives the Creator an explicit tool for "the same create"; deduplicating by URL would merge different Creators' links and, later, their analytics. |
| A-7 | What does a repeat `DELETE` on a retired link answer? | `204` (idempotent, silent); `410`; `404` | **assumed** `410`. It makes the retired state observable on every path (consistent with the Visitor's `410`), and a client retrying after a timeout learns the link is gone, which is the outcome it wanted. Either option writes one audit row. Safe: reversible, no data impact. |
| A-8 | Does reading a retired link answer `200` or `410`? | `200` with `state` `retired`; `410` | **decided** `200` with `state` `retired`. FR-4 keeps the record for audit and FR-3 lists `state`; a Creator must be able to see what a retired link pointed to. |
| A-9 | Status and body of an idempotent replay; same key with a different body; key scope; when the window starts | replay `200` vs `201`; stored first response vs current representation; `409` vs `422` on mismatch; per-client vs global; window from first attempt vs first success | **assumed** replay is `201` with the same `Location` and the link's current representation; mismatch is `422` (the IETF `Idempotency-Key` draft's convention); scope global (NFR-S6 decided no clients exist); window from the binding `201`; a `4xx`/`5xx` leaves the key unbound. Safe: a client that treats the replay as the original sees identical data; nothing stored is lost by any later change. |
| A-10 | Format limits for `Idempotency-Key`? | none; 1–255 visible ASCII | **assumed** 1–255 visible ASCII, else `400` `format`. The header is client input at a trust boundary and is stored; a bound keeps it safe in rows and logs. Safe: strict subset, widenable. |
| A-11 | How does a `400` "name the field and the rule" (FR-5)? | free-text `detail`; an `errors[]` extension with field and a stable token | **decided** `errors[{field, rule, message}]` with the tokens in business rule 3 and AC-18/AC-20; the submitted value is never echoed. Tokens make each test prove the right rejection without asserting prose, and ADR-0002 leaves framework `detail` wording unasserted. |
| A-12 | Which rule is reported when an input breaks several (e.g. a 3 000-character `javascript:` URL)? | unspecified; a fixed order | **decided** the fixed order in business rule 3, so every test input maps to exactly one token. |
| A-13 | Does NFR-S1's "no private/loopback hosts" apply? | reject now; not a rule until a server-side fetch exists | **decided** not a rule in this slice. NFR-S1 conditions it on a server-side fetch that is planned by nobody (`docs/REQUIREMENTS.md` §4 excludes previews); redirecting a browser to `http://localhost/` harms nothing the service holds. Recorded so the reviewer does not read it as a missing failure path. |
| A-14 | Unknown JSON fields in the create body? | reject with `400`; ignore | **assumed** ignore. A misspelt `url` already fails `required`, so nothing silently succeeds; ignoring is the platform default; and it lets AC-7 prove the body-size boundary with a request that is valid apart from its size. Safe: reversible, no data impact. |
| A-15 | Is an error for a browser (`Accept: text/html`) still a problem detail? | an empty body or HTML; `application/problem+json` regardless | **decided** problem detail regardless of `Accept` (business rule 8). The mission says every error is a problem detail, and the Visitor's `404`/`410` are the real user path. |
| A-16 | Is a query string on the short link forwarded to the target? | forward/merge; ignore | **assumed** ignore (business rule 7). Narrow and predictable; forwarding is a feature a later slice can add without breaking anything. |
| A-17 | Who is the audit "actor" when nobody authenticates? | the literal `anonymous`; a hashed client address; the request id | **decided** `anonymous`. NFR-S6 (decided at plan-lock) makes every request anonymous; a client address, even hashed, would put a client identifier in an append-only row the privacy rules never asked for; the request id already sits in its own field. |
| A-18 | How much of the link goes into the audit "before"/"after" states? | the whole representation; at least `url` and `state` | **decided** at least `url` and `state`, "before" empty on create. Enough for an Operator to see what changed; the exact record shape is the design's ADR. |
| A-19 | How is the 24 h window (NFR-R5) proven from outside? | real time (impossible); suite-controlled clock | **decided** the functional suite controls the service clock for AC-19; no by-effect capture is required for it. |
| A-20 | May the target URL appear in log events? | yes at INFO; no | **decided** no (business rule 10, AC-27). URLs routinely carry tokens and personal data in their query strings; the audit row holds the URL for whoever needs it. |
| A-21 | What does the `201` `Location` header point at? | the short URL; the management resource | **decided** `/api/links/<code>` (the created resource, per HTTP semantics); the short URL is the `shortUrl` field. |
| A-22 | How is a `500` (NFR-R6) and the rollback (NFR-A1) provoked? | not tested; the suite induces an audit-write failure | **decided** the suite induces an audit-write failure on a retire (AC-24); the mechanism is the design's and is named in the design review. |
| A-23 | Reserved words for generated codes? | none; the list in business rule 1 | **decided** the first path segments the service already serves. Only `actuator` is reachable with the code charset and length; the rule is stated once so a later change of length cannot reintroduce the collision. |

No question was parked on `human@kernel`: every row has a safe, narrow
default or follows from a decision the human already recorded at the mission
plan-lock (endpoints, NFR-R5, NFR-S3, NFR-S6, the dependency overrides).

## Proof contract

Each item is an observable outcome; the implement and qa_check steps attach
its evidence with `rig proof add`, artifacts under `proof/`.

- [ ] AC-1 through AC-28 are each covered by a named functional test in `src/functionalTest/java` (tabled criteria as one parameterised test each), and all are green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional execution data for the candidate SHA (NFR-M1).
- [ ] Unit and functional JaCoCo reports for the candidate are committed under `docs/qa/coverage/01-create-redirect/unit/` and `docs/qa/coverage/01-create-redirect/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `01-create-redirect` mapping AC-1 through AC-28 and business rules 1 to 12 to the tests that prove them, with the `FR`/`NFR` id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `01-create-redirect`, either "None for this slice" or an honest gap with its compensating check.
- [ ] The first commit on `slice/01-create-redirect` contains only the dependency overrides (Tomcat embed 11.0.25, Jackson 3.1.7, Jackson 2 constraint 2.21.7); `scripts/gw check` is green on that commit alone; the builder's fresh OSV run for it is captured under `proof/` (mission plan-lock `qitem-20261003042553-03ac8b4b`).
- [ ] `proof/` holds a captured exchange from the running service covering create (`201`), read (`200`), redirect (`302`), retire (`204`), the Visitor's `410` and one `400`, each with status line, response headers (`X-Request-Id`, `Location`, `Cache-Control` where applicable) and body: AC-1, AC-4, AC-8, AC-10, AC-12, AC-13 by effect.
- [ ] `proof/` holds the JSON log lines produced for those captured requests, showing `requestId` equal to each captured header and containing no URL, user agent, key or client address: AC-26 and AC-27 by effect.
- [ ] `proof/` holds the audit rows written for the captured create and retire (a query result or export), showing actor, action, entity, before/after, request id and time: AC-22 and AC-23 by effect.
- [ ] `docs/api/openapi.json` is committed, generated deterministically with sorted keys, carries the examples AC-28 requires, and QA's diff of it against the candidate's live `/v3/api-docs` (both key-sorted) is empty (NFR-M3; mission shaping rule 5).
- [ ] The ADRs listed under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them (NFR-M2).
- [ ] AC-19 is proven with suite-controlled time; the test and its clock control are named in the traceability row.
- [ ] The code review records that no application code path updates or deletes an audit row, and a persistence-level test in the unit suite asserts it (NFR-A2).
- [ ] The code review and the release-prep secret scan record no secret in the repository and the public base URL as an environment-overridable setting with the shipped default (NFR-S4).

## Source material

- `missions/01-greenfield-core/SPEC.md` — allocation, shaping rules, decision brief and the human's plan-lock text (`qitem-20261003042553-03ac8b4b`).
- `missions/01-greenfield-core/slices/01-create-redirect/slice.yaml` — tier, tier reason, territory, carried backlog.
- `docs/REQUIREMENTS.md` — FR/NFR rows and personas; §4 out-of-scope list.
- `PLAN.md` — plan of record (Core API row: endpoint shape and the `url` field).
- `missions/00-hello/slices/01-ping/SPEC.md` — vocabulary for request id, logging and problem-detail criteria.
- `docs/DESIGN.md`, `docs/adr/0001`–`0004` — shipped contracts this slice inherits (request id, problem details, ECS logs, no client PII) and the pending ADR list.
- `docs/RISKS.md` — open-redirect and reserved-path risks this slice's criteria answer.
- `docs/guidance/requirements.md` — shape, acceptance-criteria rules, ambiguity policy.

## Intent visual

N/A — non-visual slice.

## Status

- 2026-10-03 — requirements written; 28 acceptance criteria, 12 business rules, 23 ambiguity rows (10 assumed, 13 decided, none parked). Awaiting `requirements_review`.

## Dependencies

- None. `depends_on` is empty; this is the only slice in wave `w1`. Slices `02-analytics`, `03-operate` and `04-audit-read` depend on it (the link record, the redirect path, the audit rows, the API document).

## Self-check

Recorded 2026-10-03 before the requirements handoff.

- Every AC observable from outside: AC-1 to AC-21 and AC-26 to AC-28 through HTTP status, headers and bodies, or the log output; AC-22 to AC-25 through audit rows the suite inspects. No AC reads internal state.
- Error and privacy paths are ACs: `400` (AC-4, AC-5, AC-20), `413` (AC-7), `415` (AC-6), `404` (AC-14), `405` (AC-15), `410` (AC-11, AC-13), `422` (AC-18), `500` with rollback (AC-24); no client-controlled values in logs (AC-27); no echo of submitted values in error bodies (AC-4, rule 8); host-header independence (AC-3).
- Business rules cover the non-obvious logic: code shape and reserved segments, verbatim targets, validation order and tokens, no URL deduplication, the full idempotency contract, one-way retire, redirect headers, problem details regardless of `Accept`, audit content and transactionality, log content, base-URL source, time formats.
- Out of scope is explicit: eighteen named exclusions, each one a thing a builder might otherwise add, with the FR-5 alias clause and the NFR-S1 private-host clause called out by name.
- Every allocated id (FR-1–6, FR-9; NFR-S1, S3, S4, R5, R6, A1, A2, M3, O1, O2; cross-cutting M1, M2) appears in the *Requirements covered* table with at least one AC, or for M1 and M2 with a named artifact obligation in *Non-functional* and the proof contract.
- Every ambiguity resolved: A-1 to A-23, ten `assumed` with the reason the default is safe, thirteen `decided`; none parked, none left open; no ambiguity resolved by widening scope.
- Proof contract names coverage (merged 100 % plus per-suite reports), traceability rows with ids, a `GAPS.md` row, the first-commit dependency overrides with an OSV capture, by-effect captures (HTTP exchange, log lines, audit rows), the API-document diff, the ADR obligation, and the two review-recorded checks (A2, S4).
- No design leaked: no class, package, table, column, library, filter or migration is named; the endpoint paths and the `url`/`errors[]` field names are the public contract the mission brief already fixed or this SPEC decides; "same transaction" is the behaviour NFR-A1 states, not a mechanism.
- Consistent with the mission brief and the human's recorded decisions: endpoints and field from the decision brief and `PLAN.md`; NFR-R5 24 h and NFR-S3 16 KiB as decided; NFR-S6 anonymous everywhere; the dependency overrides as the first gated commit; nothing from the mission's "explicitly not in this mission" list.
- Sized for the brief's warning (one endpoint over the ceiling): criteria are grouped by endpoint in FR order so an FR-3/FR-4 split by outcome, if the design review asks for it, is a cut between the *Read*/*Retire* groups and the idempotency and audit groups that depend on them; tabled criteria keep the count at 28 rather than roughly 45 single-input criteria.
- `plan-review`: run twice, while drafting and on the finished draft. The engineering-clarity lens produced the validation-order rule (business rule 3, A-12), AC-16 (route shadowing), AC-21 (unbound key after a rejected create), and on the second pass tightened AC-22 ("null" before state), AC-5 (object-valued `url`) and the trailing-slash exclusion; the strategy lens confirmed no scope beyond the allocation; the UX lens applied only to the Visitor's browser path and produced the `Accept` clause in AC-12 to AC-14 and A-15 (a JSON `410` for a browser is accepted for a product with no web UI). No executive summary was produced.
- `rig scope audit --mission 01-greenfield-core`: no finding against `01-create-redirect` (its earlier "no authored proof contract" advisory cleared); the remaining six low advisories are the untouched placeholder SPECs of slices 02 to 04.
- Not verified by me: that `Cache-Control: no-store` and problem-detail bodies under a browser `Accept` are producible with zero friction on this stack is a design question; both are stated as contract, and the design review may dispute them with evidence.

---

> **How you work this slice (SOP):** conventions SSOT: `docs/reference/sdlc-conventions.md` (installed: `$OPENRIG_HOME/reference/sdlc-conventions.md`) — read its COMPONENT MENU first: your mission chooses the build path (the simple default flow · the wave model · the assigned rigorous overlay) and the planning rigor (the P0–P4 dial); do not assume the heavy flow unless your mission or dispatch assigns it. Full flow for the default path: the `mission-slice-sop` skill. The floor on every path: track on PROGRESS.md; evidence lands via `rig proof add` (never hand-placed); a slice is **not done** until its promised outcomes have evidence; verify with `rig scope audit`.
