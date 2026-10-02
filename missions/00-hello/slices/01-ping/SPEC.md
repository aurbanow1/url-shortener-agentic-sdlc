---
id: OPR.99.0.1.1
slice: 01-ping
mission: 00-hello
status: draft
stage: wip
tier: high
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "GET /api/ping returns 200 with a JSON body {status: ok, time: <ISO-8601 UTC>} and a request id header, is logged as structured JSON, and is covered 100% by unit and functional tests."
depends_on: []
---

# Slice 01 — Ping endpoint

## Intent

GET /api/ping returns 200 with a JSON body {status: ok, time: <ISO-8601 UTC>} and a request id header, is logged as structured JSON, and is covered 100% by unit and functional tests.

The pain is felt by two people. The operator who deploys `urlshort` has no
application-level endpoint to confirm the service answers and that its clock
is sane; `/actuator/health` says the process is up, not that the API is
served. The support engineer reading structured logs has no way to find the
log event that belongs to a given response, because nothing today stamps a
request id on responses or on log lines. This slice is also the factory's dry
run: its value is a proven pipeline, so the contract is deliberately tiny.

## Mini-requirements

### User stories

- As a service operator, I want `GET /api/ping` to answer `ok` with the
  server's current UTC time, so that I can confirm from outside that the API
  is live and its clock is correct.
- As a support engineer, I want every HTTP response to carry a request id that
  also appears on the log event for that request, so that I can go from a
  response in hand to its log line without guessing.
- As the person evaluating this factory, I want this endpoint to arrive on
  `main` with 100% line and branch coverage proof, so that the pipeline is
  shown to work before any real feature travels through it.

### Acceptance criteria

Every criterion below is observable from the public HTTP surface or from the
service's log output. Each becomes one functional test.

- **AC-1 — Ping answers ok as JSON.**
  GIVEN the service is running
  WHEN a client sends `GET /api/ping`
  THEN the response status is `200`, the `Content-Type` is `application/json`,
  the body is a JSON object with exactly two fields `status` and `time`, and
  `status` is the string `ok`.

- **AC-2 — Time is the current UTC instant.**
  GIVEN the service is running and the client records the instant just before
  sending and just after receiving
  WHEN a client sends `GET /api/ping`
  THEN the body's `time` is a string that parses as an ISO-8601 instant, ends
  with the `Z` designator, has at least seconds precision, and lies within the
  client's recorded interval (lower bound compared at seconds precision, since
  the server may truncate fractional seconds).

- **AC-3 — Every response carries a request id.**
  GIVEN the service is running
  WHEN a client sends `GET /api/ping` without any request-id header
  THEN the response carries a header `X-Request-Id` whose value is non-empty,
  at most 64 characters, printable ASCII, and contains no whitespace.

- **AC-4 — Request ids are unique per request.**
  GIVEN the service is running
  WHEN a client sends `GET /api/ping` twice in succession
  THEN the two `X-Request-Id` values differ.

- **AC-5 — Wrong method is a problem detail that still carries the id.**
  GIVEN the service is running
  WHEN a client sends `POST /api/ping`
  THEN the response status is `405`, the `Content-Type` is
  `application/problem+json`, the body is an RFC 9457 problem detail with
  `status` `405`, the body contains no stack trace, and the response still
  carries a non-empty `X-Request-Id` header.

- **AC-6 — The ping request is logged as JSON with its request id.**
  GIVEN the service is running with its default logging configuration
  WHEN a client sends `GET /api/ping` and receives `X-Request-Id` = `R`
  THEN the log output contains at least one event carrying a field
  `requestId` whose value equals `R`, and every such event is a single JSON
  object on one line.

- **AC-7 — The log event carries no client address and no user agent.**
  GIVEN the service is running
  WHEN a client sends `GET /api/ping` with a distinctive `User-Agent` value
  that occurs nowhere else (a canary)
  THEN no log output produced while handling that request contains the canary
  value or the client's remote address.

- **AC-8 — A client-supplied request id is ignored.**
  GIVEN the service is running
  WHEN a client sends `GET /api/ping` with a header `X-Request-Id` whose value
  is a distinctive canary
  THEN the response's `X-Request-Id` differs from the canary, and no log
  output produced while handling that request contains the canary.

### Business rules

1. **Request ids are server-issued.** The service generates a fresh id for
   every request it handles. An inbound `X-Request-Id` (or any other
   correlation header) is ignored: it is never echoed, logged, or used to
   derive the issued id.
2. **Request-id shape.** Non-empty, at most 64 characters, printable ASCII
   without whitespace, so the value is safe in an HTTP header and in a log
   line. Two consecutive requests must receive different ids. The exact
   generation scheme is not part of this contract.
3. **The header is on every response.** Success and error responses alike
   carry `X-Request-Id`, including responses the framework produces before any
   handler runs (wrong method, unknown path).
4. **`status` is always `ok`.** Ping has no degraded answer. If the service
   cannot answer, the client gets no `200` at all; health and readiness remain
   the job of `/actuator/health`.
5. **`time` is the server's clock.** It is read when the request is handled,
   rendered in UTC with the `Z` designator, with at least seconds precision.
   Fractional seconds are permitted. Offsets such as `+00:00` are not.
6. **Body shape is exact.** The JSON object has the two fields `status` and
   `time` and no others.
7. **Errors are problem details.** Any non-2xx response from this surface is
   an RFC 9457 problem detail with `Content-Type: application/problem+json`
   and never contains a stack trace or exception class name.
8. **At least one log event per handled ping.** Handling `GET /api/ping`
   produces at least one log event carrying the issued request id; AC-6 and
   AC-7 inspect every event that carries it. Whether other endpoints produce
   a per-request event is outside this slice.

### Non-functional

- **Logging obligations.** The log event for a ping request is a single JSON
  object under the service's default logging configuration (the one used by
  `scripts/gw bootRun` and the packaged jar). It carries `requestId` equal to
  the response header. It carries no client IP address, no `User-Agent`
  value, and no value copied from any inbound request header.
- **No input.** The endpoint accepts no parameters, headers, or body of
  significance; there are no validation limits to define.
- **No authentication, no rate limiting.** The endpoint is anonymous and
  unthrottled.
- **No latency target** is claimed for this slice.
- **Coverage gate.** The candidate passes `scripts/gw check` with 100% line
  and branch coverage across the merged unit and functional execution data.
  Any honest shortfall is written to `docs/qa/GAPS.md`, never configured away.

### Scope

**In scope**

- `GET /api/ping` with the response contract above.
- Issuing a request id for every request the service handles and exposing it
  as the `X-Request-Id` response header on every response.
- Making the issued id available to the log event for the ping request.
- Unit and functional tests, coverage reports, traceability rows, and the
  proof artifacts listed in the proof contract.

**Explicitly out of scope**

- Honoring, validating, or echoing a client-supplied request id.
- Distributed tracing or propagation of any trace or span headers.
- A per-request access log line for every endpoint; only the ping event is
  required here.
- Cache-control or other caching headers on the ping response.
- Authentication, authorization, rate limiting, or CORS.
- Behaviour of `HEAD` or `OPTIONS` on `/api/ping` beyond framework defaults.
- Asserting the endpoint's presence in the OpenAPI document.
- Any database read or write; ping touches no persistence.
- Metrics or health-indicator changes.

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Which response header carries the request id? | `X-Request-Id`; `Request-Id`; `X-Correlation-Id` | **assumed** `X-Request-Id`. It is the de facto convention clients and proxies already recognise; the functional test must name a header, so it is fixed here. Safe because renaming later is a one-line change with no data impact. |
| A-2 | Should an inbound request id be honored and echoed? | A: honor a well-formed inbound id, generate otherwise; B: always generate, ignore inbound | **assumed** B. The human's mission plan-lock constraint is "keep the request-id filter minimal"; honoring inbound ids adds a trust-boundary validation (length, charset, log-injection) that is not needed to prove this pipeline. Safe because B never produces a wrong result, only a less convenient one; A can be added by a later slice without breaking B's contract. |
| A-3 | What exactly does "ISO-8601 UTC" permit for `time`? | seconds only; fractional seconds; `Z` vs `+00:00` | **assumed** `Z` designator, at least seconds precision, fractional seconds permitted, no numeric offset. Safe because every ISO-8601 instant parser accepts it, and tests compare at seconds precision so the server's precision choice cannot break them. |
| A-4 | May the body carry fields beyond `status` and `time`? | exact two fields; allow extras | **decided** exactly two. The intent spells the body out; a tight contract keeps the test deterministic and leaves nothing for a reviewer to wonder about. |
| A-5 | Which log event must carry the request id: a generic per-request access line for all endpoints, or the ping event only? | A: generic access line from the request-id mechanism; B: only the ping request's event is required | **assumed** B. The human's constraint limits the request-id mechanism to the smallest thing that issues the id and exposes it; a generic access log is more mechanism. `AGENTS.md` requires that request lines which exist carry `requestId`; it does not require that every request produce one. Safe because B is a strict subset of A and A can follow later. |
| A-6 | The test property files blank `logging.structured.format.console`, so a test run as configured never emits JSON. Is MDC presence enough proof of "logged as structured JSON"? | A: accept MDC presence as proof; B: prove a JSON line by effect under the default format | **decided** B. The mission brief says the slice must verify, not assume, that the id reaches the JSON line. AC-6 therefore reads the event as JSON, and the proof contract requires a captured JSON line as an artifact. How the functional test obtains that line is a design and test-engineering matter. |
| A-7 | Does the request-id header have to appear on error responses too? | ping only; every response | **decided** every response. `AGENTS.md` names the request id as cross-cutting and the mission brief gives the slice the `web/` territory for exactly that reason. Proven by AC-5 on the wrong-method path. |
| A-9 | Does the inbound-id policy (A-2) need its own test, or is it implied by AC-3? | implied; explicit AC | **decided** explicit AC-8. It is the slice's only trust-boundary decision; an explicit test keeps it visible in the traceability table and flips deliberately if a later slice chooses to honor inbound ids. |
| A-8 | Can `status` ever be something other than `ok`? | always `ok`; `ok` or `degraded` | **decided** always `ok` (business rule 4). A degraded state would need a definition of degraded; `/actuator/health` already owns that. |

No question was parked on `human@kernel`: each ambiguity had a safe default
that does not change what a later slice could still build.

## Proof contract

Each item is an observable outcome; the implement and qa_check steps attach
its evidence with `rig proof add`, artifacts under `proof/`.

- [ ] AC-1 through AC-8 are each covered by a named functional test in
  `src/functionalTest/java`, and all are green on the candidate SHA with
  `scripts/gw check`.
- [ ] `scripts/gw check` reports 100% line and 100% branch coverage on the
  merged unit and functional execution data for the candidate SHA.
- [ ] Unit and functional JaCoCo reports for the candidate are committed under
  `docs/qa/coverage/01-ping/unit/` and `docs/qa/coverage/01-ping/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `01-ping` mapping AC-1
  through AC-8 and business rules 1 to 8 to the tests that prove them.
- [ ] `docs/qa/GAPS.md` holds a row for `01-ping`, either "None for this
  slice" or an honest gap with its compensating check.
- [ ] `proof/` holds a captured `GET /api/ping` exchange from the running
  service (status line, response headers including `X-Request-Id`, body)
  showing AC-1 to AC-3 by effect.
- [ ] `proof/` holds the single JSON log line produced for that same captured
  request, showing `requestId` equal to the captured header value and
  containing no client address or user agent.

## Source material

- `missions/00-hello/SPEC.md` — mission intent and the plan-lock decision
  brief, including the human's constraint on the request-id mechanism.
- `missions/00-hello/slices/01-ping/slice.yaml` — tier, territory.
- `AGENTS.md` — stack facts, error and logging rules, coverage gate.
- `src/main/resources/application.properties` — shipped defaults: problem
  details enabled, structured console logging in ECS format, actuator surface.
- `src/functionalTest/java/dev/urlshort/HealthJourneyTest.java` — the only
  shipped public journey; vocabulary for functional tests.

## Intent visual

N/A — non-visual slice.

## Status

- 2026-10-02 — requirements written; eight acceptance criteria, eight business
  rules, no parked decisions. Awaiting design.

## Dependencies

- None. `depends_on` is empty; this is the only slice in wave `w1`.

---

> **How you work this slice (SOP):** conventions SSOT: `docs/reference/sdlc-conventions.md` (installed: `$OPENRIG_HOME/reference/sdlc-conventions.md`) — read its COMPONENT MENU first: your mission chooses the build path (the simple default flow · the wave model · the assigned rigorous overlay) and the planning rigor (the P0–P4 dial); do not assume the heavy flow unless your mission or dispatch assigns it. Full flow for the default path: the `mission-slice-sop` skill. The floor on every path: track on PROGRESS.md; evidence lands via `rig proof add` (never hand-placed); a slice is **not done** until its promised outcomes have evidence; verify with `rig scope audit`.
