---
id: OPR.99.0.2.2
slice: 02-analytics
mission: 01-greenfield-core
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Analyst can read a link's click statistics (total clicks, clicks per day, top referrers) because every redirect records a privacy-safe click event without slowing the Visitor."
depends_on: ["OPR.99.0.2.1"]
---

# Slice 02 — Click analytics

## Intent

An Analyst can read a link's click statistics (total clicks, clicks per day, top referrers) because every redirect records a privacy-safe click event without slowing the Visitor.

Since `01-create-redirect` (merged as `16c355f`) a Visitor is redirected with
a non-cacheable `302`, but nothing remembers that it happened: an Analyst who
wants to know whether a link works, when it is used and where its traffic
comes from has no answer. Click data is also the first data in the product
that describes people (their address, browser and the page they came from),
so this slice makes "privacy-safe" and "without slowing the Visitor" as
testable as the counts themselves.

## Mini-requirements

### Requirements covered

Allocated by `missions/01-greenfield-core/SPEC.md` (allocation table; mission
plan-lock `qitem-20261003042553-03ac8b4b`, amended by the fast plan
`qitem-20261003052736-7830d02a`).

| Id | Requirement (short) | Proven by |
|---|---|---|
| FR-7 | every redirect records a click event (time, referrer if present, user-agent class, hashed client address) without blocking the redirect | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-14, AC-15, AC-16 |
| FR-8 | an Analyst reads per-link statistics: total clicks, clicks per day, top referrers | AC-7, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13, AC-22 |
| NFR-P1 | client address stored only as a salted hash with a daily-rotated salt; raw IP, full user agent and full referrer query strings never logged or stored | AC-3, AC-4, AC-5, AC-17, AC-18 |
| NFR-L3 | click recording adds ≤ 2 ms p95 to the redirect | AC-14, AC-15 (in-suite: the redirect does not wait for the click store); the 2 ms p95 number in *Non-functional* and the proof contract (release-level bench) |
| NFR-O1 (cross-cutting) | server-issued `X-Request-Id` on every response, in every log event of the request | AC-19 |
| NFR-O2 (cross-cutting) | structured JSON logs, no PII, no client-controlled values | AC-18, AC-19 |
| NFR-M3 (cross-cutting) | committed OpenAPI document with examples | AC-21; committed-equals-live check in the proof contract |
| NFR-M1 (cross-cutting) | 100 % line and branch coverage, honest gaps | proof contract (artifact, not HTTP) |
| NFR-M2 (cross-cutting) | an ADR before the code that depends on a cross-cutting decision | *Non-functional* obligation and proof contract (artifact, not HTTP) |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Analyst (reads how a link performs: clicks over time, where they came from); Visitor (anyone who opens a short link and expects to land on the target quickly).
- **Secondary:** Operator (reads the logs; must be able to trust that click data holds no raw personal data).
- Not in this slice: Creator (creating, reading and retiring links is `01-create-redirect`, unchanged here).

### User stories

- As a Visitor, I want opening a short link to redirect me exactly as fast and as reliably as before, so that click counting never costs me anything.
- As a Visitor, I want my address, my browser string and the page I came from not to be stored or logged in a recoverable form, so that opening a short link does not leave a record of me.
- As an Analyst, I want to read a link's total number of clicks, so that I know whether it is being used.
- As an Analyst, I want to read a link's clicks per day, so that I can see when it is used.
- As an Analyst, I want to read a link's top referring sites, so that I can see where its traffic comes from.
- As an Operator, I want the statistics endpoint to carry a request id and log like every other endpoint, so that I can trace it without leaking data.
- As an Operator, I want the statistics endpoint in the committed OpenAPI document, so that reviewers and clients see one truth.

### Acceptance criteria

Every criterion is observable from the public HTTP surface, from the
service's log output, or from a stored click record the test suite can
inspect (as `01-create-redirect`'s audit criteria inspect audit rows). "Problem
detail", "browser `Accept`" and the problem-detail rules (no echo of submitted
values, regardless of `Accept`) are as defined in `01-create-redirect`'s SPEC
(acceptance-criteria preamble and business rule 8); they are inherited, not
restated. "Statistics" means the body of `GET /api/links/<code>/stats`
(business rule 7). "The suite controls the service clock" is the mechanism
`01-create-redirect` already uses for its AC-19. "Settled" means: the suite has
waited for click recording to complete, either by the visibility bound of
business rule 6 or by a flush the design provides to the test context.

#### Recording a click

- **AC-1 — A redirect records exactly one click with its time.** [FR-7]
  GIVEN an active link `C` with no clicks, and the client records the instant just before sending and just after receiving
  WHEN a Visitor sends `GET /C` with a browser `Accept` and receives `302`, and recording has settled
  THEN exactly one click record exists for `C`, and its time is an instant within the client's recorded interval (compared at seconds precision).

- **AC-2 — Only a redirect is a click.** [FR-7]
  GIVEN an active link `A` and a retired link `R`, neither with clicks
  WHEN a client sends each request below, and recording has settled
  THEN no click record exists for `A`, `R` or any other code.

  | Request | Answer (from `01-create-redirect`) |
  |---|---|
  | `GET /R` with a browser `Accept` | `410` |
  | `GET /nosuchcode1` with a browser `Accept` | `404` |
  | `HEAD /A` | framework default (business rule 1) |
  | `POST /A` | `405` |
  | `GET /api/links/A` | `200` |
  | `GET /api/links/A/stats` | `200` |

- **AC-3 — The referrer is stored as its origin only.** [FR-7, NFR-P1]
  GIVEN an active link `C`
  WHEN a Visitor opens `C` once with each `Referer` value below and recording has settled
  THEN each click record's referrer is the value in the second column, and none of the strings `refpathcanary`, `refquerycanary`, `reffragcanary` or `refusercanary` appears in any click record.

  | `Referer` sent | Stored referrer |
  |---|---|
  | `https://News.Example/a/refpathcanary?t=refquerycanary#reffragcanary` | `https://news.example` |
  | `http://blog.example:8081/post` | `http://blog.example:8081` |
  | `https://refusercanary:pw@forum.example/x` | `https://forum.example` |
  | header absent | none |
  | `android-app://com.example.app/` | none (business rule 3) |
  | `not a url` | none |
  | an `https://` value longer than 2 048 characters | none |

- **AC-4 — The user agent is stored as a class only.** [FR-7, NFR-P1]
  GIVEN an active link `C`
  WHEN a Visitor opens `C` once with each `User-Agent` value below and recording has settled
  THEN each click record's user-agent class is the token in the second column, and the string `uacanary` appears in no click record.

  | `User-Agent` sent | Class |
  |---|---|
  | `Mozilla/5.0 (X11; Linux x86_64) uacanary Firefox/131.0` | `browser` |
  | `Mozilla/5.0 (compatible; Googlebot/2.1; uacanary)` | `bot` |
  | `uacanary-crawler/1.0` | `bot` |
  | `uacanary-spider` | `bot` |
  | `curl/8.7.1 uacanary` | `other` |
  | header absent, or empty | `unknown` |

- **AC-5 — The client address is stored only as a salted hash that rotates every UTC day.** [FR-7, NFR-P1]
  GIVEN an active link `C`, the suite controls the service clock, and requests can be sent from client address `203.0.113.77` or `198.51.100.23`
  WHEN a Visitor opens `C` twice from `203.0.113.77` on UTC day `D` (once at `00:00:01Z`, once at `23:59:59Z`), once from `198.51.100.23` on day `D`, and once from `203.0.113.77` on day `D+1`, and recording has settled
  THEN the four click records carry a non-empty client hash; the two day-`D` hashes of `203.0.113.77` are equal; the day-`D` hash of `198.51.100.23` differs from them; the day-`D+1` hash of `203.0.113.77` differs from its day-`D` hash; no click record contains `203.0.113.77` or `198.51.100.23` in any field; and no stored hash equals the unsalted SHA-256 (hex or Base64) of either address.

- **AC-6 — Forwarding headers do not change the recorded client.** [FR-7, NFR-P1]
  GIVEN an active link `C` and the suite controls the service clock
  WHEN a Visitor opens `C` twice on the same UTC day from client address `203.0.113.77`, first with no forwarding header and then with `X-Forwarded-For: 192.0.2.99` and `Forwarded: for=192.0.2.99`, and recording has settled
  THEN the two click records carry the same client hash, and `192.0.2.99` appears in no click record (business rule 4).

#### Reading statistics

- **AC-7 — A link with no clicks has empty statistics.** [FR-8]
  GIVEN an active link `C` that has never been opened
  WHEN an Analyst sends `GET /api/links/C/stats`
  THEN the status is `200`, the `Content-Type` is `application/json`, and the body is exactly `{"code": "C", "totalClicks": 0, "clicksPerDay": [], "topReferrers": []}` (as JSON, field order not asserted).

- **AC-8 — Total clicks counts every redirect.** [FR-8]
  GIVEN an active link `C` and the suite controls the service clock
  WHEN a Visitor opens `C` seven times and recording has settled
  THEN `GET /api/links/C/stats` answers `200` with `totalClicks` equal to `7`.

- **AC-9 — Clicks per day are grouped by UTC calendar day.** [FR-8]
  GIVEN an active link `C` and the suite controls the service clock
  WHEN a Visitor opens `C` twice at `2026-10-01T23:59:59Z`, three times at `2026-10-02T00:00:00Z`, zero times on `2026-10-03`, and once at `2026-10-04T12:00:00Z`, and recording has settled
  THEN the statistics have `clicksPerDay` equal to `[{"date": "2026-10-01", "clicks": 2}, {"date": "2026-10-02", "clicks": 3}, {"date": "2026-10-04", "clicks": 1}]` in that order, and `totalClicks` equal to `6`.

- **AC-10 — Top referrers are ranked and capped.** [FR-8]
  GIVEN an active link `C`
  WHEN a Visitor opens `C` with these referrers and recording has settled: `https://a.example/…` 5 times (with five different paths and query strings), `https://b.example/` 3 times, `https://c.example/` 3 times, eleven further distinct origins `https://d1.example/` … `https://d11.example/` once each, and 4 times with no `Referer`
  THEN `topReferrers` has exactly 10 elements, each `{"referrer": <origin>, "clicks": <n>}`; the first three are `https://a.example` (5), `https://b.example` (3), `https://c.example` (3) in that order; the remaining seven are `https://d1.example`, `https://d10.example`, `https://d11.example`, `https://d2.example`, `https://d3.example`, `https://d4.example`, `https://d5.example` in that order (ascending by code point), each with `1`; and `totalClicks` is `26`.

- **AC-11 — The statistics are consistent with each other.** [FR-8]
  GIVEN any link for which AC-8, AC-9 or AC-10 produced statistics
  WHEN an Analyst reads them
  THEN `totalClicks` equals the sum of `clicks` over `clicksPerDay`, and the sum of `clicks` over `topReferrers` is at most `totalClicks`.

- **AC-12 — A retired link's statistics are still readable.** [FR-8]
  GIVEN an active link `C` that was opened four times, recording settled, and `C` was then retired
  WHEN a Visitor opens `C` again (answered `410`) and an Analyst sends `GET /api/links/C/stats`
  THEN the status is `200` and `totalClicks` is `4`.

- **AC-13 — Statistics of an unknown code, and wrong methods, are problem details.** [FR-8]
  GIVEN the service is running and an active link `C` exists
  WHEN a client sends one of the requests below
  THEN the status is the one in the second column as a problem detail, and the body does not contain the submitted code.

  | Request | Status |
  |---|---|
  | `GET /api/links/nosuchcode1/stats` | `404` |
  | `GET /api/links/<40 letters>/stats` | `404` |
  | `POST /api/links/C/stats` | `405` |
  | `DELETE /api/links/C/stats` | `405` |

- **AC-22 — `HEAD` and `OPTIONS` on the statistics path keep the framework defaults and record nothing.** [FR-8]
  GIVEN an active link `C` whose statistics show `totalClicks` `N`
  WHEN a client sends `HEAD /api/links/C/stats` and `OPTIONS /api/links/C/stats`
  THEN `HEAD` answers `200` with no body; `OPTIONS` answers `200` with an `Allow` header that contains `GET`; and a following `GET /api/links/C/stats` still shows `totalClicks` `N`.
  (AC-22 is numbered after AC-21 because it was added after the requirements review, so existing references hold.)

#### Without slowing or failing the Visitor

- **AC-14 — A slow click store does not slow the redirect.** [FR-7, NFR-L3]
  GIVEN an active link `C` whose `url` is `T`, and the suite has made every click write take 2 seconds (the mechanism is the design's, as `01-create-redirect`'s AC-24)
  WHEN a Visitor opens `C` twenty times in sequence
  THEN every reply is `302` with `Location` equal to `T` and `Cache-Control: no-store`, and every reply arrives within 250 ms of its request being sent.

- **AC-15 — A failing click store does not fail the redirect.** [FR-7, NFR-L3]
  GIVEN an active link `C` whose `url` is `T`, and the suite has made every click write fail
  WHEN a Visitor opens `C`
  THEN the reply is `302` with `Location` equal to `T` and `Cache-Control: no-store`; no click record exists for `C`; and the log output contains exactly one event at `WARN` for the lost click, carrying `requestId` equal to that reply's `X-Request-Id` and none of the request's client values (business rule 5).

- **AC-16 — Concurrent redirects lose no clicks.** [FR-7]
  GIVEN an active link `C` with no clicks
  WHEN 200 redirects of `C` are sent from 20 concurrent clients, each answered `302`, and recording has settled
  THEN `totalClicks` is exactly `200` and exactly 200 click records exist for `C`.

#### Privacy and observability

- **AC-17 — The statistics expose aggregates only.** [NFR-P1]
  GIVEN a link `C` opened by Visitors with the client addresses, `User-Agent` and `Referer` canaries of AC-3 to AC-5, recording settled
  WHEN an Analyst reads its statistics
  THEN the body has exactly the four fields of AC-7, every `clicksPerDay` element has exactly `date` and `clicks`, every `topReferrers` element has exactly `referrer` and `clicks`, and the body contains no client address, no client hash, no user-agent class, none of the canaries and no referrer path, query or fragment.

- **AC-18 — No click data reaches the logs.** [NFR-P1, NFR-O2]
  GIVEN the service is running with its default logging configuration, and canaries that occur nowhere else: a `User-Agent` value, a `Referer` whose path and query hold a canary, an `X-Forwarded-For` value, and the client address `203.0.113.77`
  WHEN a Visitor opens a link sending all of them, recording settles, and an Analyst reads its statistics
  THEN no log output produced while handling those requests and recording that click contains any of the canaries, the client address, the stored client hash, or the referrer origin.

- **AC-19 — Request correlation on the new paths.** [NFR-O1, NFR-O2]
  GIVEN the service is running with its default logging configuration
  WHEN a client provokes each of these responses: `200` statistics, `404` statistics of an unknown code, `405` on the statistics path, `302` redirect with recording settled, `302` redirect with the click store failing (AC-15)
  THEN each response carries a non-empty `X-Request-Id` `R`; and every log event produced for that request, including any event emitted while recording its click after the response was sent, is a single JSON object on one line carrying `requestId` equal to `R`.

#### Regression guards

- **AC-20 — Redirect and audit behaviour are unchanged.** (regression guard, business rule 8)
  GIVEN audit rows exist from creating and retiring several links, each row's content recorded
  WHEN the suite opens active links (recording settled), opens a retired link, reads statistics and provokes the `404`/`405` statistics paths
  THEN `01-create-redirect`'s AC-12, AC-13 and AC-14 still pass unchanged, every recorded audit row is still present with identical content, and no audit row was added.

#### API document

- **AC-21 — The live API document describes the statistics endpoint.** [NFR-M3]
  GIVEN the service is running
  WHEN a client sends `GET /v3/api-docs`
  THEN the document's `paths` contain `/api/links/{code}/stats` (get) documenting `200` with a schema of the four fields and at least one example, and `404` typed as `application/problem+json`; and every path and operation `01-create-redirect`'s AC-28 requires is still present with its responses unchanged.

### Business rules

1. **What a click is.** A click is a `GET /<code>` answered `302`. Nothing else records a click: not a `410` or `404` on the redirect path, not `HEAD /<code>` (even though the framework answers it from the `GET` mapping), not `405`, not a read of the link or its statistics. Every such redirect is one click: there is no deduplication by client, no bot filtering and no unique-visitor counting in this slice (raw clicks; unique-versus-raw is FR-16, mission 03). A query string or fragment on the short link (ignored by `01-create-redirect`'s rule 7) does not change what is recorded.
2. **A click record holds exactly four facts.** The link it belongs to, the time (the service clock's UTC instant when the redirect was answered), the referrer origin or none (rule 3), the user-agent class (rule 4), and the client hash (rule 4). It holds no raw client address, no `User-Agent` text, no referrer path, query, fragment or userinfo, no forwarding-header value, no request id, and no other request data.
3. **Referrer origin.** The `Referer` header is reduced to its origin: lowercase scheme, `://`, lowercase host, and `:port` only when the port is present and is not the scheme's default. It is recorded only when the header is present, at most 2 048 characters, and parses as an absolute `http` or `https` URL with a non-empty host; in every other case the click is recorded with no referrer. Clicks with no referrer count in `totalClicks` and `clicksPerDay` but never appear in `topReferrers`.
4. **Client identity.** The client address is the address of the network connection that sent the redirect request; `X-Forwarded-For`, `Forwarded` and every other header are ignored (the trusted-proxy rule is `03-operate`'s, A-9). It is stored only as a salted hash. The salt is secret, random, never logged and never exposed. The salt changes at every UTC midnight, so the same address hashes to the same value within one UTC day of uninterrupted service and to an unrelated value on another day. A restart may start a fresh salt for the rest of the day, so the same address may then hash to a second value that day. A salt is not kept after its day ends, so a stored hash cannot be recomputed from a guessed address after that day. The user-agent class is one token, decided in this order: header absent or empty → `unknown`; the value contains `bot`, `crawler` or `spider`, compared case-insensitively → `bot`; the value starts with `Mozilla/` → `browser`; otherwise → `other`.
5. **The Visitor comes first (fail open).** Recording a click never delays, changes or fails the redirect: the `302`, its `Location` and its `Cache-Control` are the same whether recording succeeds, is slow or fails. When a click cannot be recorded it is lost, not retried indefinitely and not turned into an error response. Each lost click produces exactly one `WARN` log event that carries the redirect's `requestId` and no client value. Clicks in flight when the process stops abruptly may be lost (A-11).
6. **Visibility.** A recorded click appears in the statistics within 5 seconds of its redirect being answered while the service is under normal load. A statistics read never fails because recording is behind.
7. **Statistics.** `GET /api/links/{code}/stats` answers `200` with a JSON object of exactly four fields: `code` (the link's code); `totalClicks` (every click recorded for the link); `clicksPerDay` (one element `{"date", "clicks"}` per UTC calendar day with at least one click, `date` as `YYYY-MM-DD`, ascending by date, days without clicks omitted); `topReferrers` (at most 10 elements `{"referrer", "clicks"}`, one per referrer origin, ordered by `clicks` descending and then by `referrer` ascending by code point). All figures cover every click recorded for the link since it was created; there is no time-range parameter. The statistics of a retired link are readable with `200`. An unknown code, or a path segment that cannot be a code, answers `404`; any method other than `GET`, `HEAD` and `OPTIONS` on the path answers `405`. That `404` and that `405` are problem details as `01-create-redirect`'s rule 8 defines. `HEAD` and `OPTIONS` behave as the framework defaults, as on `01-create-redirect`'s redirect path (its rule 7): `HEAD` answers the `GET` status and headers without a body, and `OPTIONS` answers `200` with an `Allow` header (AC-22, A-18). Reading statistics is anonymous (NFR-S6) and changes nothing.
8. **Unchanged surface.** The redirect keeps every contract of `01-create-redirect` (status, `Location`, `Cache-Control: no-store`, `404`/`410` paths). Recording a click and reading statistics are not mutations of a link: they write no audit row, and the audit trail stays append-only (`01-create-redirect` rule 9).
9. **Logs.** `01-create-redirect`'s rule 10 applies to the statistics endpoint and to click recording: every log event produced on behalf of a request, including events emitted after its response was sent, carries that request's `requestId`, and no event contains the client address, the client hash, the `User-Agent`, the `Referer` (whole or origin), or any forwarding-header value. The code and the status may be logged.

### Non-functional

Only what this slice must prove.

- **Latency (NFR-L3, ≤ 2 ms p95 added to the redirect).** In-suite, AC-14 and AC-15 prove the structural property: the redirect does not wait for the click store. The number itself needs a load generator, and that tool is `03-operate`'s (`scripts/smoke.sh --bench`, mission brief risks). The redirect p95 is measured with recording enabled at `release_prep` and compared with the NFR-L1 budget. If the "added" share cannot be isolated on the reference laptop, that is written to `docs/qa/GAPS.md` as an honest gap with AC-14 as the compensating check, never claimed.
- **Privacy (NFR-P1).** As business rules 2–4 and 9, proven with canaries by AC-3 to AC-6, AC-17 and AC-18. Rule 4's "a salt is not kept after its day ends" and "never logged or exposed" cannot be observed over HTTP. The design states where the salt lives and how it is discarded, the design review and the security review check it, and the proof contract names it.
- **Retention compatibility (NFR-P2, built in mission 02).** Every click record carries its own time (rule 2), so a 90-day purge or aggregation by age is possible without a schema change. This slice builds no purge.
- **Logging obligations (NFR-O1, O2).** As business rule 9, proven by AC-18 and AC-19 under the shipped logging configuration. Carried from `00-hello` (backlog W1-02): because AC-18 constrains log content, the design weighs one functional journey against a real server. Only a live capture caught QA-01. The choice and its reason are the design's.
- **Decisions needing an ADR before dependent code (NFR-M2).** Recorded as ADRs (or amendments) and indexed in `docs/DESIGN.md` §7 before the code that depends on them: how a click is handed off without blocking the redirect, what happens under overload, and the fail-open policy (rules 5–6); the client-hash scheme, salt storage and daily rotation (rule 4); the click storage shape and its migration under the existing schema conventions, with NFR-P2 compatibility; and the statistics aggregation semantics, if the design departs from computing them over stored clicks.
- **API document (NFR-M3).** The statistics operation joins `docs/api/openapi.json` (this slice is the first holder of the file in wave `w2`, mission shaping rule 5), regenerated deterministically on this slice's candidate and equal to its live `/v3/api-docs`.
- **Coverage gate (NFR-M1).** The candidate passes `scripts/gw check` with 100 % line and branch coverage on the merged unit and functional execution data; any honest shortfall goes to `docs/qa/GAPS.md`.
- **Territory.** Changes under `link/` are limited to the one hook that hands a `302` redirect to click recording (`slice.yaml` grant; the seam named in `01-create-redirect`'s design §1).
- **No new rate limit, authentication or latency target** beyond NFR-L3 (FR-10 and NFR-L1/L2 are `03-operate`'s; NFR-S6 keeps every endpoint anonymous).

### Scope

**In scope**

- Recording one click per `302` redirect, with the four facts of rule 2, without blocking or failing the redirect.
- `GET /api/links/{code}/stats` with total clicks, clicks per day and top referrers, and its `404`/`405` paths.
- The privacy reductions: referrer origin, user-agent class, salted client hash with daily rotation.
- The click storage and its migration (`V2`, `slice.yaml`).
- The statistics operation in `docs/api/openapi.json`, and the click table in `docs/diagrams/erd.mmd`.
- Unit and functional tests, coverage reports, traceability rows, and the proof artifacts in the proof contract.

**Explicitly out of scope**

- Unique visitors, bot filtering, deduplication, or any count other than raw clicks (FR-16, mission 03). The user-agent class and client hash are stored and nothing in this slice reads them.
- Any statistic beyond the three FR-8 names, for example per-user-agent-class breakdowns, hourly series, countries, devices or conversion. The plan-lock's secondary assumption sends these to mission 03.
- Time-range, pagination or limit parameters on the statistics endpoint; a statistics listing across links; CSV or report exports.
- Zero-filled days in `clicksPerDay`; referrer paths or full URLs in `topReferrers`; a "direct" bucket for clicks without a referrer.
- Retention purge or aggregation of old clicks (NFR-P2, mission 02); data-subject requests (`docs/REQUIREMENTS.md` §4).
- Honouring `X-Forwarded-For`/`Forwarded` for the client address (the trusted-proxy rule is `03-operate`'s, A-9).
- Durable delivery of clicks in flight across an abrupt process stop (A-11); retries of failed click writes.
- Counting `HEAD` requests, `410`/`404` redirect attempts or statistics reads as clicks.
- Rate limiting the statistics endpoint (FR-10, `03-operate` applies it to every operation).
- Any change to create, read, retire or the redirect's response; any audit row for clicks.
- Backlog CR-01 (the `errors[]` member in the generated ProblemDetail schema). The document customisation lives in `web/`, which is `03-operate`'s territory in `w2`, and this slice adds no validation error. It stays on the orchestration lead's backlog (A-15).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Path of the statistics endpoint | `/api/links/{code}/stats`; `/api/stats/{code}`; a query parameter | **decided** `GET /api/links/{code}/stats`, fixed by the mission decision brief ("Outcome" lists it among the public API). |
| A-2 | Which statistics? | the three FR-8 names; more (UA breakdown, hourly) | **decided** exactly total clicks, clicks per day, top referrers. Mission plan-lock secondary assumption (confirmed by the human, `qitem-20261003042553-03ac8b4b`): "FR-8's statistics shape is exactly total clicks, clicks per day and top referrers, and anything beyond is mission 03". |
| A-3 | Raw or unique clicks? Are bots counted? | raw; unique per hashed client/day; bots excluded | **decided** raw clicks, bots included (rule 1). FR-16 (mission 03) owns unique-versus-raw as the ambiguous-analytics question. Deciding it here would pre-empt the human's decision there. Raw is the strict superset, so storing the hash and class keeps every later choice possible. |
| A-4 | What counts as a click: `HEAD`, `410`, `404`? | every hit on `/{code}`; only `GET` answered `302` | **assumed** only a `GET` answered `302` (rule 1). A `HEAD` is a probe (link checkers, previews) and no Visitor lands anywhere. `410`/`404` redirect nobody. Safe: narrow, reversible, no data impact (recording `HEAD` later only adds rows). |
| A-5 | What is stored of the referrer? | full URL; URL without query; origin only | **assumed** origin only (rule 3). NFR-P1 forbids query strings. Paths also carry identifiers (`/users/<name>`, document ids), and "where they came from" (the Analyst persona) is answered by the site. Safe: strict subset of every wider option. A path can be added by a later decision for new clicks, though not recovered for stored ones. That is the privacy-preserving direction. |
| A-6 | Referrers that are not `http(s)` URLs (`android-app://`, garbage, over-long) | store raw; store scheme; treat as absent | **assumed** treated as absent (rule 3). Storing arbitrary client strings re-opens the NFR-P1 hole. A `topReferrers` entry the Analyst cannot read is noise. Safe: those clicks are still counted. |
| A-7 | Clicks without a referrer in `topReferrers` | omitted; a `null`/"direct" bucket | **assumed** omitted (rule 3). The list is "top referrers", and a click without one is in the total. AC-11 states the resulting inequality. Safe: additive later (a bucket can be added without changing existing elements). |
| A-8 | What is a user-agent "class"? | browser/bot/other/unknown with a fixed rule; a library-driven parser; device families | **assumed** four tokens and the ordered substring rule of rule 4. FR-7 asks for a class and nothing reads it in this slice (A-3). The narrowest rule that is fully testable from outside is the right size. Safe: the class is never exposed (AC-17). Mission 03 can refine it for new clicks. |
| A-9 | Which address is hashed when a proxy is in front? | the connection's address; the left-most `X-Forwarded-For`; `03-operate`'s trusted-proxy rule | **assumed** the connection's address, forwarding headers ignored (rule 4, AC-6). `01-create-redirect`'s rule 11 already refuses forwarding headers. The trusted-proxy rule (NFR-R2) is built by `03-operate` in the same wave and cannot be depended on. Mission 01 publishes the container on loopback with no proxy (NFR-S5), so the connection's address is the real client. Cross-slice note: once `03-operate`'s rule exists, aligning the click hook with it is a small later change, and the orchestration lead may route it at `wave_review`. Safe: never trusts spoofable input. |
| A-10 | When does the salt rotate, and what is a "day"? | UTC midnight; rolling 24 h from start; local midnight | **assumed** UTC midnight (rule 4), the same day boundary as `clicksPerDay` (rule 7). Hashes then line up with the reporting day, and the boundary is testable with the suite's clock (AC-5). A restart within a day may start a fresh salt (rule 4). Requiring the salt to survive a restart would mean storing it, which is a design choice that keeps the secret longer, and nothing in this slice reads the hash (A-3). Safe: no data impact; the stored hash is opaque. |
| A-11 | May clicks be lost? | never (synchronous write); lost only on store failure or abrupt stop | **decided** lost under failure, never at the Visitor's expense (rule 5). The slice intent says "without slowing the Visitor" and NFR-L3 caps the added latency at 2 ms p95. A synchronous write that must not fail the redirect cannot also be lossless when the store fails. AC-16 forbids losing clicks under normal concurrent load, and AC-15 makes every loss visible in the logs. |
| A-12 | How soon must a click be visible in the statistics? | immediately; within a bound | **assumed** within 5 seconds under normal load (rule 6). The bound keeps tests deterministic (the "settled" definition) and leaves the design free to hand clicks off without blocking. Safe: an Analyst reading per-day figures does not need sub-second freshness. |
| A-13 | Days with zero clicks in `clicksPerDay`; time range of the figures | zero-filled over a window; only days with clicks, all time; a `from`/`to` parameter | **assumed** only days with clicks, all time, no parameter (rule 7). It needs no window choice, which is FR-16's territory. Once NFR-P2 retention lands in mission 02, "all time" means "all retained". Safe: zero-filling or a range parameter can be added later without breaking existing clients. |
| A-14 | How many top referrers, and how are ties ordered? | 5; 10; all; unspecified order | **assumed** at most 10, ties by `referrer` ascending by code point (rule 7). A stable order makes AC-10 deterministic. 10 is the common default of analytics dashboards. Safe: the cap can be raised later without breaking clients. |
| A-15 | Should this slice fix backlog CR-01 (the generated ProblemDetail schema omits `errors[]`)? | fix here as `docs/api/openapi.json`'s first `w2` holder; leave to the lead's backlog | **decided** not here. The fix is in the document customisation under `src/main/java/dev/urlshort/web/`, which is `03-operate`'s territory in `w2` (mission territory table). This slice's endpoint emits no `errors[]`. The root objective names CR-01 as context, not a requirement. It stays on the orchestration lead's backlog and goes to `wave_review` if `03-operate` does not take it. |
| A-16 | Does the click record carry the redirect's request id? | yes; no | **assumed** no (rule 2). Nothing in FR-7/FR-8 reads it, and a per-click request id is a per-visit identifier that joins click data to the logs. Safe: strict subset; AC-19 already correlates the logs. |
| A-17 | What does a statistics read of a retired link answer? | `200`; `410` | **decided** `200` (rule 7, AC-12), consistent with `01-create-redirect`'s A-8: a retired link's record is kept (FR-4) and its history is exactly what an Analyst looks at after retiring it. |
| A-18 | `HEAD` and `OPTIONS` on the statistics path: `405`, or the framework defaults? (design review DR-03) | `405` for every method but `GET`; framework defaults | **decided** framework defaults (rule 7, AC-22), the same as `01-create-redirect`'s rule 7 for the redirect path. RFC 9110 expects `HEAD` wherever `GET` is supported. A `405` would make this the only `GET` resource in the API that refuses `HEAD`, and it would need handlers written only to refuse. Neither method changes any statistic. |

No question was parked on `human@kernel`. Every row either follows from a
decision the human already recorded (the endpoint set and the FR-8 shape at
the mission plan-lock, NFR-S6 anonymous, the fast plan's wave and territory
split) or has a narrow, reversible default that stores less, not more.

## Proof contract

Each item is an observable outcome. The implement and qa_check steps attach
its evidence with `rig proof add`, with artifacts under `proof/`.

- [ ] AC-1 through AC-22 are each covered by a named functional test in `src/functionalTest/java` (each tabled criterion as one parameterised test), and all are green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional execution data for the candidate SHA (NFR-M1).
- [ ] Unit and functional JaCoCo reports for the candidate are committed under `docs/qa/coverage/02-analytics/unit/` and `docs/qa/coverage/02-analytics/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `02-analytics` mapping AC-1 through AC-22 and business rules 1 to 9 to the tests that prove them, with the `FR`/`NFR` id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `02-analytics`: either "None for this slice" or each honest gap with its compensating check. The NFR-L3 number is a gap until the release-level bench measures it.
- [ ] `proof/` holds a captured exchange from the running service: three redirects of one link (`302` with `X-Request-Id`, `Location`, `Cache-Control`), sent with a `User-Agent`, a `Referer` carrying a path-and-query canary and a forwarding-header canary, followed by its statistics (`200` body). This is a representative by-effect check, on the running service, of three things: each redirect is counted (AC-1, AC-8 with `totalClicks` `3`), the referrer is reduced to its origin (AC-3, one row), and the statistics body has the AC-7 field shape with aggregates only (AC-17). The full tables and boundaries (empty statistics in AC-7, UTC-day grouping in AC-9, ranking in AC-10) are proven by the controlled functional tests of the first item.
- [ ] `proof/` holds the stored click records for that capture (a query result or export), showing time, referrer origin, user-agent class and client hash, and the absence of the canaries and the raw address. This is a representative by-effect check of the stored-record privacy reductions for one client on one day (AC-3, AC-4 for the class sent, AC-5's "no raw address, non-empty hash"). Hash equality within a day, rotation across UTC midnight and the second address in AC-5 are proven by the suite-clock functional test, with no wait through a real midnight.
- [ ] `proof/` holds the JSON log lines produced for those captured requests and their click recording. They show `requestId` equal to each captured header and contain no canary, client address, client hash or referrer. This is a by-effect check of AC-18, and of AC-19's `200` and settled-`302` rows, on the running service per W1-02. AC-19's `404`, `405` and failing-store rows are proven by the functional tests.
- [ ] `docs/api/openapi.json` is regenerated on the candidate with the statistics operation and its examples, and QA's diff of it against the candidate's live `/v3/api-docs` (both key-sorted) is empty (NFR-M3; mission shaping rule 5).
- [ ] The ADRs listed under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them (NFR-M2).
- [ ] `docs/diagrams/erd.mmd` shows the click storage and its relation to links.
- [ ] The design review and the security review record where the salt lives, that it is never logged or exposed, and that no salt survives its UTC day (NFR-P1, rule 4, the part not observable over HTTP).
- [ ] At `release_prep`, the redirect p95 with click recording enabled is measured with `03-operate`'s bench and recorded against NFR-L3. If the added share cannot be isolated, the `GAPS.md` row says so (NFR-L3).

## Source material

- `missions/01-greenfield-core/SPEC.md`: allocation table, decision brief (endpoint list, FR-8 shape assumption), shaping rule 5, w2 territory table.
- `missions/01-greenfield-core/slices/02-analytics/slice.yaml`: tier, territory, the `link/` hook grant, the `openapi.json` ordered grant, W1-02.
- `missions/01-greenfield-core/NOTES.md` §1: fast plan, D11, backlog CR-01.
- `docs/REQUIREMENTS.md`: FR-7, FR-8, FR-16, NFR-P1, NFR-P2, NFR-L3, NFR-O1, NFR-O2, NFR-M1–M3, personas.
- `missions/01-greenfield-core/slices/01-create-redirect/SPEC.md`: rules 1, 7, 8, 9, 10, 11; AC-12–AC-14, AC-19, AC-24, AC-28; A-8 (vocabulary and inherited contracts).
- `missions/01-greenfield-core/slices/01-create-redirect/design.md` §1 (the redirect seam), §2.4, §2.6, §5 (log events).
- `docs/guidance/requirements.md`.

## Intent visual

N/A: non-visual slice.

## Status

- 2026-10-03: requirements written; 21 acceptance criteria, 9 business rules, 17 ambiguity rows (11 assumed, 6 decided, none parked). Handed to `requirements_review`.

- 2026-10-03: requirements review **PASS** on `488788f` (`docs/review/02-analytics/requirements-review.md`, evidence `d882fdb`), with one MEDIUM finding, RQ-01. Fixed in passing (see *Review response*); no AC, rule or ambiguity row changed.

## Review response

| Id | Severity | Response |
|---|---|---|
| DR-03 (design review, `docs/review/02-analytics/design-review.md`) | MEDIUM | **Fixed, in the SPEC as the design agent recommended (queue item `qitem-20261003091357-40bfa3a7`).** Rule 7 said every method other than `GET` answers `405`, but the platform serves `HEAD` from the `GET` mapping and answers `OPTIONS` itself. Rule 7 now leaves `HEAD` and `OPTIONS` at the framework defaults, as `01-create-redirect`'s rule 7 does for the redirect path. New AC-22 makes that observable and checks that neither method changes `totalClicks`. A-18 records the reasoning. No other AC, rule or ambiguity row changed. 22 acceptance criteria. |
| RQ-01 | MEDIUM | **Fixed.** The three by-effect capture items in the proof contract no longer claim full proof of AC-5, AC-7, AC-9 or AC-19. Each now names the assertions it actually shows on the running service and points to the controlled functional tests for the tables, the empty case, the UTC-day boundary, the salt rotation, and the `404`/`405`/failing-store rows. Every AC and the all-AC functional-test obligation are unchanged. |

## Dependencies

- `01-create-redirect` (`OPR.99.0.2.1`, merged as `16c355f`): the link record and code lookup, the redirect path this slice hooks, the problem-detail and log contracts it inherits, and the API document it extends.
- Wave partner `03-operate` (no edge): takes `docs/api/openapi.json` after this slice merges and may later align the client address with its trusted-proxy rule (A-9).

## Self-check

Recorded 2026-10-03 before the first requirements handoff.

- Every AC is observable from outside. AC-7 to AC-13, AC-21 work through HTTP status, headers and bodies. AC-14 to AC-16 work through HTTP replies and their timing. AC-18 and AC-19 work through log output. AC-1 to AC-6 work through stored click records the suite inspects, as `01-create-redirect` inspects audit rows. AC-20 works through audit rows and `01`'s own criteria. No AC reads internal state beyond a stored record.
- Error and privacy paths are ACs: `404`/`405` on statistics (AC-13); non-click paths (AC-2); slow and failing store (AC-14, AC-15); no raw address, user agent, referrer path/query or forwarding value stored (AC-3 to AC-6), exposed (AC-17) or logged (AC-18); salted daily-rotated hash, including the unsalted-hash negative check (AC-5).
- The business rules cover the non-obvious logic: what a click is, the four stored facts, referrer reduction, client identity, salt rotation, UA class order, fail-open with a visible loss, the visibility bound, the exact statistics shape and ordering, the unchanged audit and redirect surface, and log content.
- Out of scope is explicit in eleven bullets, each something a builder could plausibly add (uniques, bots, extra breakdowns, ranges, direct bucket, purge, proxy headers, durable delivery, `HEAD` clicks, rate limiting, CR-01).
- Every allocated id has coverage: FR-7, FR-8, NFR-P1, NFR-L3, and cross-cutting O1, O2, M3 appear in *Requirements covered* with at least one AC. NFR-L3's number and M1/M2 have named artifact obligations in *Non-functional* and the proof contract.
- Every ambiguity is resolved: A-1 to A-17, eleven `assumed` with the reason the default is safe, six `decided`. None is parked and none is resolved by widening scope. Each assumed default stores or exposes less, not more.
- The proof contract names merged coverage plus per-suite reports, traceability rows with ids, a `GAPS.md` row (NFR-L3 named), and by-effect captures: HTTP exchange, click records and log lines on the running service (W1-02). It also names the API-document diff, the ADR obligation, the ERD, the review-recorded salt-handling check, and the release-level latency measurement.
- No design is leaked. No class, package, table, column, library, queue or thread mechanism is named. "Salted hash" and "daily-rotated salt" are NFR-P1's own words; SHA-256 appears only as the negative check in AC-5. The JSON field names are this SPEC's public contract, and the endpoint path is the mission brief's.
- Consistent with the mission brief and the human's recorded decisions: the endpoint is the brief's, the FR-8 shape is the plan-lock's secondary assumption, raw clicks defer to FR-16, NFR-S6 keeps the endpoint anonymous, the `link/` grant is limited to the hook, this slice is the first holder of the API document in `w2`, P2 is not precluded, and nothing from the mission's "not in this mission" list is included.
- `plan-review`: applied on the finished draft by its three lenses. Strategy: nothing beyond FR-7/FR-8, and FR-16's question left open on purpose (A-3). Engineering: it added the concurrency criterion (AC-16), the settled/visibility definition (rule 6, A-12) that keeps async designs testable, and the request id on events emitted after the response (AC-19, rule 9). UX: there is no UI. The Analyst sees `200` with empty arrays rather than `404` for an unused link (AC-7), and a retired link's history stays readable (AC-12). One blocking finding was fixed before handoff: rule 4 promised equal hashes for the whole UTC day, which would have silently required the salt to survive a restart. It now says "uninterrupted service", and A-10 records the restart case. No executive summary was produced, because this is a slice SPEC rather than a product feature brief, as on `01-create-redirect`.
- `rig scope audit --mission 01-greenfield-core`: no finding against `02-analytics`. The two low advisories were against `03-operate`'s SPEC, which was being written at the same time.
- Not verified by me: whether 250 ms (AC-14) is a comfortable bound in the functional test context on the reference laptop is a design and QA question. The point of the criterion is "far below the 2 s store delay", and the design review may adjust the number with evidence. Whether a remote address can be set per request in the functional suite (AC-5, AC-6) is a test-mechanics question for the design.

---

> **How you work this slice (SOP):** conventions SSOT: `docs/reference/sdlc-conventions.md` (installed: `$OPENRIG_HOME/reference/sdlc-conventions.md`) — read its COMPONENT MENU first: your mission chooses the build path (the simple default flow · the wave model · the assigned rigorous overlay) and the planning rigor (the P0–P4 dial); do not assume the heavy flow unless your mission or dispatch assigns it. Full flow for the default path: the `mission-slice-sop` skill. The floor on every path: track on PROGRESS.md; evidence lands via `rig proof add` (never hand-placed); a slice is **not done** until its promised outcomes have evidence; verify with `rig scope audit`.
