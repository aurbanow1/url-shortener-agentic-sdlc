---
id: OPR.99.0.2.3
slice: 03-operate
mission: 01-greenfield-core
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Operator can run urlshort in production shape: clients above the rate limit are answered 429 with Retry-After and a counted rejection, readiness reflects the database, metrics are exposed for scraping, and the container runs non-root with durable data on a loopback-published port."
depends_on: ["OPR.99.0.2.1"]
---

# Slice 03 — Operate safely

## Intent

An Operator can run urlshort in production shape: clients above the rate limit are answered 429 with Retry-After and a counted rejection, readiness reflects the database, metrics are exposed for scraping, and the container runs non-root with durable data on a loopback-published port.

After `01-create-redirect` the service has a public API but nothing an
Operator needs to run it: any client can flood creates or redirects without
limit, `/actuator/health` says `UP` without asking the database, there is no
scrapeable metrics endpoint, the compose health check only tests that a TCP
port is open, the container publishes its port on every host interface with a
writable root filesystem, and the graceful-shutdown timeout is 20 s where the
requirement says 10 s. The Operator feels this pain first (abuse, false
health, no numbers); Creators, Visitors and Analysts feel it as an outage. This
slice adds the rate limit and the operational surface, and ships the container,
smoke and bench lines whose proofs are taken at release.

## Mini-requirements

### Requirements covered

Allocated by `missions/01-greenfield-core/SPEC.md` (mission plan-lock
`qitem-20261003042553-03ac8b4b`, amended by the fast plan
`qitem-20261003052736-7830d02a`) and `slice.yaml`. The ids fall into two
groups, and this SPEC never promises an in-suite proof for the second:

- **built and proven in-suite** (functional and unit suites): FR-10, NFR-R2, NFR-R1, NFR-O3, and the `429` contract in the committed API document (NFR-M3);
- **configured here, proven at release level** (installed smoke against the container, `docker inspect`, restart loop, bench at `release_prep`): NFR-R3, R4, S5, X1, L1, L2. Their criteria are written below as ACs observable from the running container or `scripts/smoke.sh`; what the suites cannot show is a `docs/qa/GAPS.md` row, never a claim.

| Id | Requirement (short) | Proven by | Proof level |
|---|---|---|---|
| FR-10 | over-limit requests answer `429` problem detail with `Retry-After` | AC-1, AC-2, AC-3, AC-11 | in-suite |
| NFR-R2 | 60 creates/min and 600 redirects/min per client, token bucket, client = remote address behind an explicit trusted-proxy rule | AC-1 to AC-10 | in-suite |
| NFR-R1 | liveness and readiness probes; readiness down until migrations ran and the DB answers | AC-13, AC-14, AC-15 (in-suite); AC-21 (compose health) | in-suite + release |
| NFR-O3 | request timers per endpoint, redirect counter, rate-limit rejection counter, DB pool gauges, Prometheus format | AC-16, AC-17, AC-18, AC-19 | in-suite (+ smoke reads names, AC-21) |
| NFR-M3 | committed OpenAPI document with examples | AC-20 (the `429` on every operation) | in-suite + QA diff |
| NFR-R3 | graceful shutdown, 10 s phase timeout, no new connections | AC-25 | release |
| NFR-R4 | committed links survive a restart; single-node ceiling stated | AC-24 | release |
| NFR-S5 | non-root user, read-only filesystem except `data/` | AC-23 | release |
| NFR-X1 | one container via `docker compose up --build` and a plain jar; configuration by environment; data on a named volume | AC-21, AC-22, AC-26 | release |
| NFR-L1 | redirect p95 ≤ 20 ms, p99 ≤ 50 ms at 100 req/s for 60 s | AC-27 (the bench mode exists and reports these numbers); the target is judged at `release_prep` | release |
| NFR-L2 | create p95 ≤ 50 ms at 20 req/s | AC-27, same | release |
| NFR-M1 (cross-cutting) | 100 % line and branch coverage, honest gaps | proof contract (artifact, not HTTP) | build gate |
| NFR-M2 (cross-cutting) | an ADR before the code that depends on a cross-cutting decision | *Non-functional* obligation and proof contract | artifact |

Inherited, not allocated: NFR-O1 and NFR-O2 (every new response, the `429`
included, carries the request id and logs no client-controlled value; mission
allocation note "every later endpoint-adding slice inherits the obligation")
and NFR-P1's "raw IP never logged or stored" (the limiter is the first code
that reads the client address). AC-11 and AC-12 prove them for this slice.

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Operator (runs the service: health, logs, metrics, rate-limit tuning).
- **Secondary:** Creator, Visitor and Analyst, as the clients whose requests are limited and who must never be limited by someone else's traffic.

### User stories

- As an Operator, I want a client that exceeds its request budget to be answered `429` with a `Retry-After`, so that one client cannot exhaust the service for everyone.
- As an Operator, I want the client identified by its connection address, and a forwarded address trusted only from proxies I name, so that a client cannot dodge the limit by forging a header.
- As an Operator, I want to change the limits and the trusted proxies through configuration, so that I can tune the service and run the bench without rebuilding it.
- As a Creator, Visitor or Analyst, I want the `429` to tell me when I may retry, so that a well-behaved client recovers without guessing.
- As an Operator, I want readiness to report down while the database does not answer and liveness to stay up, so that traffic is withheld from a broken instance without restarting a healthy process.
- As an Operator, I want request timings, redirect counts, rejection counts and database pool gauges exposed for scraping, so that I can see load and abuse.
- As an Operator, I want to start the service with one `docker compose up --build`, running non-root on a read-only filesystem with its data on a named volume and its port published on loopback only, so that it is safe by default.
- As an Operator, I want links to survive a restart and in-flight requests to finish on shutdown, so that a deploy loses nothing.
- As an Operator, I want a bench mode in the smoke script, so that the latency targets can be measured instead of assumed.

### Acceptance criteria

"Problem detail" has the meaning fixed by `01-create-redirect` (business rule
8 there): `Content-Type: application/problem+json`, an RFC 9457 body whose
`status` equals the HTTP status, no stack trace, exception class name, SQL
text or client-submitted value. "Suite-controlled clock" is the mechanism
`01-create-redirect` A-19 already authorised: the functional suite controls
the time the service sees. "Remote address" is the TCP peer address of the
request, which the functional suite sets per request.

The defaults below are the decided NFR-R2 numbers: **create budget 60 per
minute, redirect budget 600 per minute** (business rules 1 to 3 define which
requests fall in which budget and how the bucket refills).

#### Rate limit (in-suite)

- **AC-1 — The create budget admits 60 and refuses the 61st.** [FR-10, NFR-R2]
  GIVEN the default limits, a frozen suite-controlled clock and a remote address that has sent nothing
  WHEN that client sends 61 valid `POST /api/links` requests in a row
  THEN the first 60 answer `201`; the 61st answers `429` as a problem detail with a `Retry-After` header and an `X-Request-Id` header; and the number of `link.create` audit rows grew by exactly 60 (the 61st created nothing).

- **AC-2 — The redirect budget admits 600 and refuses the 601st.** [FR-10, NFR-R2]
  GIVEN the default limits, a frozen suite-controlled clock, an active link `C` and a remote address that has sent nothing
  WHEN that client sends 601 `GET /C` requests in a row
  THEN the first 600 answer `302`; the 601st answers `429` as a problem detail with `Retry-After` and `X-Request-Id`, and carries no `Location` header.

- **AC-3 — `Retry-After` is truthful.** [FR-10, NFR-R2]
  GIVEN a client whose create budget is exhausted and whose last request was answered `429` with `Retry-After: S`
  WHEN the suite-controlled clock advances by `S` seconds minus one millisecond and the client sends a valid create, then the clock advances by one more millisecond and the client sends another valid create, then the client immediately sends a third
  THEN `S` is a whole number of seconds ≥ 1; the first request answers `429`; the second answers `201`; the third answers `429`.

- **AC-4 — A full budget returns after a quiet minute.** [NFR-R2]
  GIVEN a client whose create budget is exhausted
  WHEN the suite-controlled clock advances by 60 seconds with no requests from that client, and the client then sends 61 valid creates in a row
  THEN the first 60 answer `201` and the 61st answers `429`.

- **AC-5 — The two budgets are independent.** [NFR-R2]
  GIVEN a frozen clock, an active link `C`, and one client whose create budget is exhausted, and another client whose redirect budget is exhausted
  WHEN the first client sends `GET /C` and the second client sends a valid `POST /api/links`
  THEN the first answers `302` and the second answers `201`.

- **AC-6 — Clients are independent.** [NFR-R2]
  GIVEN a frozen clock and remote address `10.0.0.1` whose create budget is exhausted
  WHEN remote address `10.0.0.2` sends a valid create
  THEN it answers `201`.

- **AC-7 — A forged `X-Forwarded-For` does not change the client (default configuration).** [NFR-R2]
  GIVEN the default configuration (no trusted proxy) and a frozen clock
  WHEN one remote address sends 61 valid creates, each carrying a different `X-Forwarded-For` value (and, on some, a `Forwarded` and an `X-Real-IP` header with further different values)
  THEN the first 60 answer `201` and the 61st answers `429`.

- **AC-8 — A trusted proxy's forwarded address identifies the client.** [NFR-R2]
  GIVEN the trusted-proxy setting names `10.9.9.9`, a frozen clock, and the requests below
  WHEN the suite sends them
  THEN each budget is charged to the client in the last column:

  | Remote address | `X-Forwarded-For` | Charged to |
  |---|---|---|
  | `10.9.9.9` | `203.0.113.7` | `203.0.113.7` |
  | `10.9.9.9` | `198.51.100.1, 203.0.113.7` (left entry forged by the client) | `203.0.113.7` |
  | `10.9.9.9` | absent | `10.9.9.9` |
  | `10.0.0.5` (not trusted) | `203.0.113.7` | `10.0.0.5` |

  Observable as: after 60 creates through the first row, a create through the second row answers `429`, a create through the third row answers `201`, and a create through the fourth row answers `201`.

- **AC-9 — Every request in a budget counts, whatever its outcome, and the limit is checked first.** [NFR-R2, FR-10]
  GIVEN a frozen clock and a client that has sent nothing
  WHEN the client sends 60 `POST /api/links` requests that each answer `400` (invalid `url`), then one valid create, then one create whose body exceeds 16 KiB
  THEN the valid create answers `429` and the oversized create answers `429` (not `201`, `400` or `413`); a `429` itself does not use up budget (after `Retry-After` seconds exactly one request is admitted, as AC-3).

- **AC-10 — The limits and the trusted proxies are operator settings.** [NFR-R2, NFR-X1]
  GIVEN the service started with the create budget set to 2 per minute and the redirect budget set to 3 per minute through their environment-overridable settings, and a frozen clock
  WHEN one client sends 3 valid creates and 4 redirects to an active link
  THEN the 3rd create and the 4th redirect answer `429`, and the others answer `201` and `302`.

#### The `429` response, logs and privacy (in-suite)

- **AC-11 — The `429` is a problem detail that names no client.** [FR-10, NFR-O1, NFR-P1]
  GIVEN a client whose create budget is exhausted, sending from remote address `10.77.77.77` with `X-Forwarded-For: 192.0.2.201` and a canary `User-Agent`
  WHEN it sends a create carrying a canary value in its `url`
  THEN the response is `429` with `Content-Type: application/problem+json`, body `status` `429`, a `Retry-After` header equal to a whole number ≥ 1, a non-empty `X-Request-Id`, no `errors` member, and neither the body nor any header contains `10.77.77.77`, `192.0.2.201`, the `User-Agent` canary or the `url` canary.

- **AC-12 — Rejections are logged once, correlated, without client values.** [NFR-O1, NFR-O2, NFR-P1]
  GIVEN the shipped logging configuration and the client and canaries of AC-11
  WHEN that client sends three over-limit requests (a create, a redirect after exhausting that budget, and a `GET /api/links/<code>`)
  THEN each response's `X-Request-Id` `R` appears on exactly one log event produced while handling that request, that event is a single-line JSON object carrying `requestId` `R` and the status `429`, and no log output produced while handling them contains `10.77.77.77`, `192.0.2.201`, the `User-Agent` canary or the `url` canary.

#### Health (in-suite)

- **AC-13 — Liveness and readiness answer `UP` with a working database.** [NFR-R1]
  GIVEN the service is running with its database answering
  WHEN a client sends `GET /actuator/health/liveness` and `GET /actuator/health/readiness`
  THEN both answer `200` with a JSON body whose `status` is `UP`.

- **AC-14 — Readiness follows the database; liveness does not.** [NFR-R1]
  GIVEN the service is running and the suite makes the database stop answering (the mechanism is the design's)
  WHEN a client sends `GET /actuator/health/readiness` and `GET /actuator/health/liveness`, then the suite lets the database answer again and the client sends `GET /actuator/health/readiness` once more
  THEN readiness first answers `503` with `status` `DOWN`, liveness answers `200` with `status` `UP`, and readiness then answers `200` with `status` `UP`.

- **AC-15 — Health bodies disclose nothing about the installation.** [NFR-R1]
  GIVEN the service is running
  WHEN a client sends `GET /actuator/health`, `GET /actuator/health/liveness` and `GET /actuator/health/readiness`, with the database answering and again while it does not
  THEN no body contains the JDBC URL, the database file path, a database product name or version, or a host name; each body carries a `status` member.

#### Metrics (in-suite)

- **AC-16 — The metrics surface lists the four kinds of metric.** [NFR-O3]
  GIVEN the service is running and has served at least one create, one redirect and one `429`
  WHEN a client sends `GET /actuator/metrics`
  THEN the `names` list contains `http.server.requests` (the per-endpoint request timer), `urlshort.ratelimit.rejections` (the rejection counter), and at least one gauge of the database connection pool's active and idle connections (names recorded in `docs/DESIGN.md`).

- **AC-17 — Every rejection is counted once, by budget.** [NFR-O3, FR-10]
  GIVEN the service is running and the client records `GET /actuator/metrics/urlshort.ratelimit.rejections` for each budget tag
  WHEN one client receives `k` create `429`s and `m` redirect `429`s, and other clients make admitted requests
  THEN the counter for the create budget has grown by exactly `k`, the counter for the redirect budget by exactly `m`, and the counter carries no tag whose value is a client address.

- **AC-18 — Redirects are countable.** [NFR-O3]
  GIVEN the service is running and the client records the count of `http.server.requests` for the redirect route with status `302`
  WHEN Visitors receive `n` redirects
  THEN that count has grown by exactly `n`, and the route is reported as a template (no individual code appears as a tag value).

- **AC-19 — Metrics are exposed for scraping, without client or link values.** [NFR-O3, NFR-P1]
  GIVEN the service is running and has served creates, redirects to code `C`, a `404` for the path `/zzCanary99`, and `429`s to remote address `10.77.77.77`
  WHEN a scraper sends `GET /actuator/prometheus`
  THEN the status is `200` with a Prometheus text-format body containing the families for the request timer, the rejection counter and the pool gauges; and the body does not contain `C`, `zzCanary99`, `10.77.77.77`, or any target URL.

#### API document (in-suite + QA diff)

- **AC-20 — Every operation documents the `429`.** [NFR-M3, FR-10]
  GIVEN the candidate, which descends from `02-analytics`'s merge commit
  WHEN a client sends `GET /v3/api-docs`
  THEN every operation in `paths` (at least `GET /api/ping`, `POST /api/links`, `GET` and `DELETE /api/links/{code}`, `GET /{code}`, and `02-analytics`'s statistics operation, `GET /api/links/{code}/stats` in the mission brief) documents a `429` response typed `application/problem+json` with a `Retry-After` header whose schema is an integer, and at least one example; and the committed `docs/api/openapi.json` is identical to that live document (both key-sorted).

#### Container, restart, shutdown and bench (release-level)

These criteria are proven against the running container or the packaged jar
by `scripts/smoke.sh` and `docker inspect` at `release_prep`. The slice ships
the configuration and the smoke and bench steps; the suites do not claim them.

- **AC-21 — One command brings up a healthy service whose health check is readiness.** [NFR-X1, NFR-R1, NFR-O3]
  GIVEN a clean checkout and a Docker engine
  WHEN the Operator runs `docker compose up --build` and then `scripts/smoke.sh`
  THEN the container's health status becomes `healthy` only after `GET /actuator/health/readiness` answers `UP` (the health check asks readiness, not an open port), and the smoke script passes, asserting liveness and readiness `UP`, the metric names of AC-16 and a non-empty `GET /actuator/prometheus`.

- **AC-22 — The port is published on loopback only.** [NFR-X1]
  GIVEN the compose stack is up
  WHEN the Operator runs `docker compose port urlshort 8080` and `docker inspect` on the container
  THEN the published binding is `127.0.0.1:8080` and no binding exists on `0.0.0.0` or `::`.

- **AC-23 — The process runs non-root on a read-only filesystem with only its data writable.** [NFR-S5]
  GIVEN the compose stack is up
  WHEN the Operator runs `docker inspect` on the container and tries, inside it, to create a file under `/app` (outside the data directory) and under the data directory
  THEN `Config.User` is a non-root user, `HostConfig.ReadonlyRootfs` is `true`, the only persistent writable mount is the named data volume, the write outside the data directory fails, the write inside it succeeds, and the smoke script still passes.

- **AC-24 — Links survive a restart.** [NFR-R4]
  GIVEN the compose stack is up and a link `C` was created
  WHEN the Operator runs `docker compose restart`, and separately `docker compose down` (without `-v`) followed by `docker compose up`
  THEN after each, `GET /C` answers `302` to the same target and `GET /api/links/C` answers `200` with the body recorded at creation.

- **AC-25 — Shutdown is graceful within 10 s.** [NFR-R3]
  GIVEN the compose stack is up and the smoke loop is sending requests continuously
  WHEN the Operator runs `docker compose restart`
  THEN every request that received an HTTP response received a `2xx` or `3xx` (refused connections while the process is down are counted and reported separately, not as failures), the shutdown log shows the graceful-shutdown phase completing, and the shipped graceful-shutdown phase timeout is 10 s.

- **AC-26 — The plain jar runs with configuration by environment.** [NFR-X1]
  GIVEN `scripts/gw bootJar` has produced the jar
  WHEN the Operator starts it with `java -jar` and environment variables overriding the public base URL, the data location, the two budgets and the trusted proxies, and runs `scripts/smoke.sh`
  THEN the smoke passes, and the overridden values are observably in effect (`shortUrl` uses the overridden base; the overridden create budget yields `429` at its value).

- **AC-27 — The bench mode measures the latency targets.** [NFR-L1, NFR-L2]
  GIVEN a running instance whose two budgets were raised through their settings so that a single load client is not refused (stated in the bench output)
  WHEN the Operator runs `scripts/smoke.sh --bench`
  THEN it drives redirects at 100 requests per second for 60 seconds and creates at 20 requests per second, and prints for each scenario the achieved rate, the request count, the count of responses outside `2xx`/`3xx`, and p50, p95 and p99 latency in milliseconds; the run is judged against NFR-L1 and NFR-L2 at `release_prep`, and a noisy or failing run is recorded as a gap, not as a pass.

### Business rules

1. **Which requests are limited, and by which budget.** Every request is limited except the operator surfaces: `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui.html` and `/swagger-ui/**` are never limited and never counted, so probes and scrapers keep working under a flood. Requests whose path is `/api` or starts with `/api/` (create, read, retire, statistics, ping, whatever their method) are charged to the **create budget** (default 60 per minute). Every other limited request (the Visitor's `GET /{code}` and anything else outside `/api`) is charged to the **redirect budget** (default 600 per minute). A client has one bucket per budget.
2. **Token bucket.** Each bucket holds at most its per-minute number of tokens, starts full, and refills continuously at that number divided by 60 per second. An admitted request takes one token; a request that finds less than one token is answered `429` and takes nothing.
3. **Every request counts.** A request is charged before anything else about it is examined (body, content type, validation, existence of the code), so `400`, `404`, `405`, `410`, `413` and `415` outcomes use budget the same as successes, and an over-limit request is answered `429` regardless of what it would otherwise have produced. Only the request id is assigned before the check, so the `429` carries it.
4. **`Retry-After`.** A whole number of seconds, at least 1: the time until the bucket holds one token again, rounded up. Never an HTTP date.
5. **Client identity.** The client is the request's remote address. `X-Forwarded-For` is read only when the remote address is one of the operator-configured trusted proxies (exact addresses; default: none); the client is then the right-most `X-Forwarded-For` entry that is not itself a trusted proxy, or the remote address when the header is absent, empty or holds only trusted proxies. `Forwarded`, `X-Real-IP` and every other header are never used to identify the client.
6. **No client values in output.** The client address, every forwarded-address value, the `User-Agent` and any submitted value never appear in a response body or header, a log event, a metric tag or the Prometheus output; the limiter's per-client state lives only in memory and is never written to the database.
7. **One log event per rejection.** A `429` produces exactly one log event (the request's own, carrying `requestId` and the status), so a flood cannot multiply log volume.
8. **Limiter state is per instance and in memory.** A restart starts every bucket full; this is acceptable under the single-node ceiling (NFR-R4).
9. **Readiness and liveness.** Readiness is `UP` only when the application has started (migrations included) and the database answers a query; otherwise `503` `DOWN`. Liveness does not depend on the database. Health bodies carry a `status` and no installation details.
10. **Metrics.** The request timer reports routes as templates (`/{code}`, `/api/links/{code}`), never as concrete paths; the rejection counter is named `urlshort.ratelimit.rejections` and is tagged by budget only. No metric tag carries a client address, a code, a URL or any header value.
11. **Operator settings.** The two budgets and the trusted-proxy list are configuration, overridable by environment variable, with the defaults above (60, 600, none). Raising them is how the bench runs (AC-27).
12. **Container.** The service runs as one container built by `docker compose up --build`, as a non-root user, with a read-only root filesystem, its database on a named volume mounted at the data directory, its port published on `127.0.0.1` only, and a health check that asks readiness. A size-limited in-memory temporary directory is permitted (it holds nothing across restarts; A-14).
13. **Graceful shutdown.** On stop, the service stops accepting new connections and lets in-flight requests finish, for at most a 10 s shutdown phase.

### Non-functional

Only what this slice must prove.

- **Rate-limit numbers (NFR-R2, decided).** 60 per minute (create budget), 600 per minute (redirect budget), per client, token bucket; defaults overridable by the Operator.
- **Limiter memory is bounded.** The per-client state must not grow without bound under many distinct client addresses: a client's state is released once its buckets would be full again. Not observable over HTTP; the design states the bound, a unit test proves the release, and the security review checks it.
- **Latency (NFR-L1, L2).** Measured by AC-27's bench at `release_prep` on the reference laptop against a single instance on the H2 file; the slice claims the bench mode, not the numbers. The limiter sits on every request path, so its cost is inside the measured redirect latency.
- **Graceful shutdown (NFR-R3).** Phase timeout 10 s; the shipped configuration today says 20 s and is changed by this slice (A-15).
- **Single-node ceiling (NFR-R4).** No replication, no HA, limiter state per instance; already recorded in `docs/RISKS.md` (single-node row) and kept true.
- **Logging (NFR-O1, O2, inherited).** As business rules 6 and 7, proven by AC-11 and AC-12 with canaries under the shipped logging configuration.
- **Decisions needing an ADR before dependent code (NFR-M2).** The rate-limit mechanism (algorithm, budgets, request classification, where the check sits relative to the request-id assignment, the memory bound); the client-identity and trusted-proxy rule; the metrics exposure (Prometheus format, the rejection counter's name and tags, route-template tags); the container hardening posture (user, read-only root, tmpfs, loopback publish, readiness health check). Each recorded as an ADR (or an amendment) and indexed in `docs/DESIGN.md` §7 before the code that depends on it.
- **Coverage gate (NFR-M1).** The candidate passes `scripts/gw check` with 100 % line and branch coverage on the merged unit and functional data; honest shortfalls go to `docs/qa/GAPS.md`.
- **Serialisation on the API document (binding; mission shaping rule 5, decomposition review DC-01/DC-02).** This slice is the second holder of `docs/api/openapi.json` in wave `w2`. Its `implement` handoff names a candidate that descends from `02-analytics`'s merge commit on `main`, with the document regenerated on that base so it carries `02`'s statistics operation and this slice's `429` on every operation. If the build is otherwise done before `02` has merged, the builder exits `waiting` blocked on `02`'s frontier packet with the continuation "rebase onto main, regenerate `docs/api/openapi.json`, hand off". Not an acceptance criterion; a proof-contract item.

### Scope

**In scope**

- The per-client rate limit on every non-operator request, two budgets, token bucket, `429` problem detail with `Retry-After`, trusted-proxy rule, operator settings for budgets and proxies.
- The rejection counter, the Prometheus scrape endpoint, route-templated request timers and pool gauges on the metrics surface.
- Readiness that includes the database; liveness independent of it; health bodies without installation details.
- The `429` response on every operation of the committed API document, regenerated on a base containing `02-analytics`.
- `Dockerfile` and `compose.yaml` changes for S5 and X1 (non-root, read-only root, named volume, loopback publish, readiness health check); the 10 s graceful-shutdown timeout.
- `scripts/smoke.sh` steps for liveness, readiness, metric names and Prometheus, the restart and shutdown loops of AC-24/AC-25, and the `--bench` mode of AC-27.
- Unit and functional tests, coverage reports, traceability rows, the `GAPS.md` row for the release-level ids, and the proof contract items.

**Explicitly out of scope**

- Authentication, API keys, per-user or per-tenant quotas, allow-lists of privileged clients (NFR-S6 decided anonymous).
- Distributed or persistent limiter state, limits shared across instances (single node, NFR-R4).
- `RateLimit-*` / `X-RateLimit-*` informational headers on admitted responses; any header other than `Retry-After` on the `429`.
- CIDR ranges or host names in the trusted-proxy list; reading `Forwarded` (RFC 7239) or `X-Real-IP`.
- Limiting or securing the actuator, API-document or Swagger UI paths; exposing actuator endpoints beyond health, info, metrics and Prometheus.
- Changing `link/`, `click/` or `db/migration/`; any new counter inside the redirect or click code (the redirect count comes from the request timer, A-10).
- TLS termination, reverse-proxy configuration, publishing beyond loopback, image publishing to a registry, Kubernetes manifests.
- Alerting rules, dashboards, log shipping.
- Meeting NFR-L1/L2 in this slice (the bench mode is in scope; the verdict is `release_prep`'s), and NFR-L3 (slice `02-analytics`).
- Responses to requests the embedded server rejects before the application sees them (malformed request line, headers over the server default): no request id and no problem detail is promised there (A-17).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | NFR-R2 names a create limit and a redirect limit; the mission (shaping rule 5, approved at plan-lock) puts the `429` on **every** operation of the API document. Which budget limits read, retire, statistics and ping? | only create and redirect limited (the `429` documented on two operations); a third budget with an invented number; every `/api` request in the create budget, everything else in the redirect budget | **assumed** every `/api` request in the 60/min budget, everything else limited in the 600/min budget (rule 1). Documenting a `429` on an operation that never produces one would be a false contract (the lead rejected exactly that in DC-01); `/api` is the Creator/Analyst surface (writes and the costly statistics query) and the tighter number fits it; the Visitor surface gets the redirect number. No new number is invented. Safe: the budgets are configuration (rule 11) and the classification is one rule, reversible without data impact. The plan-lock may revise it. |
| A-2 | Bucket size (burst) for "60 per minute"? | burst = per-minute number; a smaller burst | **assumed** the bucket holds the per-minute number and refills continuously (rule 2). It is the literal reading from a fresh client ("at the limit, one over") and needs no extra number. Safe: tightening the burst later is a configuration change. |
| A-3 | Do rejected (`4xx`) requests use budget? Does a `429` use budget? | count only successes; count every admitted request; count `429`s too | **decided** every admitted request counts whatever its outcome (rule 3), a `429` does not. Bad input is the cheapest flood; charging `429`s would lock a retrying client out for ever. |
| A-4 | Is the limit checked before or after the body is read and validated? | after; before | **decided** before (rule 3): a limiter that reads 16 KiB bodies first protects nothing. The request id is assigned first so the `429` is correlated (AC-11, AC-12). |
| A-5 | `Retry-After` format? | HTTP date; delta seconds | **decided** whole seconds ≥ 1, rounded up (rule 4); RFC 9110 permits both, seconds need no clock agreement. |
| A-6 | Shape of the "explicit trusted-proxy rule"? | trust every `X-Forwarded-For`; trust named proxies (exact addresses or CIDR); also `Forwarded`/`X-Real-IP` | **assumed** exact addresses, default none, right-most untrusted `X-Forwarded-For` entry, no other header (rule 5). Default none means the forged-header test (AC-7) holds with zero configuration; CIDR and other headers can be added later without breaking anyone. Safe: strict subset. |
| A-7 | Are the operator surfaces limited? | limit everything; exempt actuator and API docs | **decided** exempt (rule 1). A throttled readiness probe or scraper would report a healthy instance as down under exactly the load the Operator wants to see (NFR-R1, O3). |
| A-8 | Are the limits configurable? | hard-coded; operator settings with the decided defaults | **assumed** operator settings (rule 11). NFR-L1's bench sends 100 redirects per second from one client, ten times the 600/min budget; without a setting the latency target cannot be measured. The Operator persona names "rate-limit tuning". Safe: the defaults are the decided numbers. |
| A-9 | Does "Prometheus format" (NFR-O3) require a scrape endpoint, or is `/actuator/metrics` enough? | `/actuator/metrics` only; add `/actuator/prometheus` | **decided** `/actuator/prometheus` (AC-19). The intent says "exposed for scraping" and the NFR says "Prometheus format"; `/actuator/metrics` is neither. |
| A-10 | Where does the "redirect counter" come from, given this slice may not touch `link/`? | a new counter in the redirect code; the request timer's count for the redirect route and status `302` | **assumed** the request timer (AC-18). It already counts every redirect by route and status, keeps one source of truth, and stays inside this slice's territory. Safe: a dedicated counter can be added later by the slice that owns `link/`. |
| A-11 | Name of the rejection counter? | leave to design; fix it here | **assumed** `urlshort.ratelimit.rejections`, tagged by budget (rule 10). It is operator-facing contract (the smoke script and any dashboard read it), so it belongs with the requirement. Safe: renaming before release costs nothing. |
| A-12 | May health responses show components and details? | show details; status only | **decided** status only (AC-15, rule 9): the endpoints are anonymous (NFR-S6) and details reveal paths and versions. Readiness failure is still observable as `503`. |
| A-13 | Does liveness depend on the database? | yes; no | **decided** no (AC-14): a database outage would otherwise make the orchestrator restart a healthy process in a loop. |
| A-14 | NFR-S5 says "read-only filesystem except `data/`"; the JVM and the embedded server need a temporary directory. | temp files on the data volume; a size-limited in-memory tmpfs | **assumed** in-memory tmpfs permitted (rule 12): it holds nothing across restarts, so no state escapes the data volume, and putting scratch files on the durable volume would mix them with the database. Safe: the persistent-write surface is still exactly `data/`. |
| A-15 | Graceful-shutdown timeout: NFR-R3 says 10 s, the shipped configuration says 20 s. | keep 20 s; 10 s | **decided** 10 s (NFR-R3 is the requirement; the 20 s line predates it). |
| A-16 | NFR-R3's proof is "zero non-2xx/3xx" during a restart, but a stopped process refuses connections. | count refusals as failures; count them separately | **assumed** refusals are not HTTP responses and are reported separately (AC-25); the claim is that no accepted request is answered with an error or cut off. Safe: it is how any single-node restart behaves; the count is still reported. |
| A-17 | `01-create-redirect`'s threat model left to this slice what the embedded server answers and logs for requests it rejects before the application (malformed request line, oversized headers). | specify a contract; leave out of scope | **assumed** out of scope (Scope list): no application code runs there, so no request id, problem detail or rate limit can be promised; NFR-S3 keeps header limits at server defaults. The design checks by effect whether those rejections log client-controlled values and, if they do, records the finding (the logging configuration is in this slice's territory). Safe: no product behaviour changes. |
| A-18 | Which port and interface does the container publish? | all interfaces; loopback | **decided** `127.0.0.1:8080` (AC-22): the intent says "loopback-published", and the rig's culture forbids exposing the service beyond localhost. |

No question was parked on `human@kernel`: every row has a safe, reversible
default or follows from the decided rows of `docs/REQUIREMENTS.md` (NFR-R2
numbers, NFR-S3, NFR-S6) and the mission's plan-locked brief.

## Proof contract

Each item is an observable outcome; the implement and qa_check steps attach
its evidence with `rig proof add`, artifacts under `proof/`.

- [ ] AC-1 through AC-20 are each covered by a named test (functional, or unit where the AC is about a pure rule), tabled criteria as one parameterised test, all green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional execution data for the candidate SHA (NFR-M1).
- [ ] Unit and functional JaCoCo reports for the candidate are committed under `docs/qa/coverage/03-operate/unit/` and `docs/qa/coverage/03-operate/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `03-operate` mapping AC-1 through AC-27 and business rules 1 to 13 to the tests or release checks that prove them, with the `FR`/`NFR` id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `03-operate` naming every criterion not proven in-suite (AC-21 to AC-27; NFR-R3, R4, S5, X1, L1, L2), the release check that will prove each, and any other honest gap.
- [ ] `proof/` holds a captured exchange from the running service: an admitted create, the `429` that follows exhaustion (status line, `Retry-After`, `X-Request-Id`, body), readiness and liveness `UP`, and an excerpt of `GET /actuator/prometheus` showing the rejection counter: AC-1, AC-11, AC-13, AC-19 by effect.
- [ ] `proof/` holds the JSON log lines for the captured `429`s, one per request, each with `requestId` equal to the captured header and containing no client address, forwarded value, user agent or URL: AC-12 by effect.
- [ ] `docs/api/openapi.json` is regenerated on a base containing `02-analytics`'s merge, documents the `429` with `Retry-After` and an example on every operation, and QA's diff against the candidate's live `/v3/api-docs` (both key-sorted) is empty (AC-20, NFR-M3).
- [ ] `git merge-base --is-ancestor <02-analytics merge commit> <candidate>` succeeds for the handed-off candidate (mission shaping rule 5).
- [ ] The ADRs listed under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them (NFR-M2).
- [ ] The code and security review records that the limiter's per-client memory is bounded and that no client address reaches a log, metric, response or stored row.
- [ ] `scripts/smoke.sh` contains the release-level steps of AC-21 to AC-27 (health, metric names, restart, shutdown loop, `--bench`); one run of `--bench` against a local instance is captured under `proof/` to show the mode works (its numbers are not the release verdict).
- [ ] At `release_prep` (recorded in `missions/01-greenfield-core/RELEASE.md`, not judged at `qa_check`): AC-21 to AC-27 pass against the container and the jar, with `docker inspect` output for AC-22 and AC-23 and the bench numbers judged against NFR-L1 and NFR-L2.

## Source material

- `missions/01-greenfield-core/SPEC.md` — allocation, shaping rule 5, decision brief and the decided rows.
- `missions/01-greenfield-core/slices/03-operate/slice.yaml` — tier, the two groups of ids, territory, the serialisation point.
- `docs/REQUIREMENTS.md` — FR-10, NFR-R1–R4, S5, O3, X1, L1, L2, M1–M3 and the personas.
- `missions/01-greenfield-core/slices/01-create-redirect/SPEC.md` (business rules 8 and 10, A-19) and `design.md` (§1 filters and configuration, §5 logging, §6 threat model rows left to this slice).
- `src/main/resources/application.properties`, `Dockerfile`, `compose.yaml`, `scripts/smoke.sh` on `main` — the shipped baseline this slice changes.
- `docs/RISKS.md` — flooding and single-node rows.
- `docs/guidance/requirements.md` — shape, criteria rules, ambiguity policy.

## Intent visual

N/A — non-visual slice.

## Status

- 2026-10-03 — requirements written: 27 acceptance criteria (20 in-suite, 7 release-level), 13 business rules, 18 ambiguity rows (9 assumed, 9 decided, none parked).

## Dependencies

- `01-create-redirect` (merged as `16c355f`): the endpoints this slice limits, the problem-detail contract it reuses for the `429`, the request-id and logging rules.
- `02-analytics` (same wave, no `depends_on` edge): this slice's `implement` handoff must descend from `02`'s merge so the regenerated API document carries both changes (mission shaping rule 5).

## Self-check

Recorded 2026-10-03 before the first requirements handoff.

- Every AC observable from outside: AC-1 to AC-20 through HTTP status, headers, bodies, the actuator endpoints, the log output and the live API document; AC-21 to AC-27 through `docker`, `docker compose`, the jar and `scripts/smoke.sh`, labelled release-level. No AC reads internal state.
- Error and privacy paths are ACs: `429` on both budgets (AC-1, AC-2), at-limit and one-over (AC-1, AC-2, AC-4, AC-10), forged forwarding headers (AC-7, AC-8), rejected inputs still charged (AC-9), readiness down (AC-14), no client address or canary in `429` bodies, logs, metric tags or Prometheus output (AC-11, AC-12, AC-17, AC-19), no installation details in health (AC-15).
- Business rules cover the non-obvious logic: request classification and exemptions, bucket size and refill, what counts, `Retry-After` rounding, client identity with trusted proxies, output privacy, log volume per rejection, in-memory state, health semantics, metric tags, settings, container posture, shutdown.
- Out of scope is explicit: ten exclusions, including CIDR proxies, informational rate-limit headers, distributed state and server-level rejections.
- Every allocated id (FR-10; NFR-R1, R2, R3, R4, S5, O3, X1, L1, L2; cross-cutting M1, M2, M3) has a row in *Requirements covered* with at least one AC or, for M1 and M2, a named artifact obligation; the six release-level ids are marked as such and promised to `GAPS.md`, not claimed in-suite.
- Every ambiguity resolved: A-1 to A-18, nine `assumed` (A-1, A-2, A-6, A-8, A-10, A-11, A-14, A-16, A-17) with the reason each default is safe, nine `decided`; none parked; no ambiguity resolved by widening scope (A-1 limits more operations, but with the two decided numbers only, because the plan-locked mission already puts the `429` on every operation).
- Proof contract names coverage (merged 100 % and per-suite reports), traceability, the `GAPS.md` row, by-effect captures (exchange, log lines, Prometheus excerpt), the API-document diff, the ancestry check, ADRs, the review record for memory and privacy, and the release-level checks.
- No design leaked: no class, package, filter, library or registry named. Named on purpose as public contract: endpoint and actuator paths, `http.server.requests` (the platform's operator-visible timer name), `urlshort.ratelimit.rejections` (A-11), header names, and the container facts NFR-S5/X1 state.
- Consistent with the mission brief and the human's decisions: NFR-R2 numbers as decided; NFR-S6 anonymous; the two groups of ids from `slice.yaml`; the ordered API-document grant and serialisation point; `link/`, `click/`, `db/migration/` untouched (A-10); nothing from the mission's "not in this mission" list.
- Baseline facts checked on `main`: `compose.yaml` publishes `8080:8080` on every interface with a TCP health check; `Dockerfile` already runs as uid 10001 but the root filesystem is writable; `application.properties` sets a 20 s shutdown phase and exposes `health,info,metrics` only. Each is a change this SPEC requires (AC-21, AC-22, AC-23, AC-25, AC-19).
- Found while drafting, from the shipped baseline and the decided numbers: A-8 (the bench cannot run under the default budget), A-15 (20 s vs 10 s), A-16 (refused connections during restart), AC-9 (limit checked before validation) and the memory-bound line.
- `plan-review` run once on the finished draft. Engineering lens: rule 1 now also exempts `/swagger-ui.html` (the configured UI path); AC-1 states "created nothing" as an audit-row delta the suite can count (the in-memory database is shared across contexts); AC-20 names the statistics path from the mission brief; AC-3's millisecond boundary checked against rule 2 (a fresh 60-token bucket exhausted on a frozen clock needs exactly 1 s, so `S` = 1 and 999 ms is still short). Strategy lens: scope equals the allocation; the release-level split follows `slice.yaml`. UX lens: the only "interface" is the client's view of a `429` (truthful `Retry-After`, AC-3; no client values echoed, AC-11). No executive summary was produced, as on `01-create-redirect`.
- Not verified by me: that the redirect count is readable per status from the request timer on this stack (A-10), and whether the embedded server logs client-controlled values on pre-application rejections (A-17); both are design questions with a named fallback.

---

> **How you work this slice (SOP):** conventions SSOT: `docs/reference/sdlc-conventions.md` (installed: `$OPENRIG_HOME/reference/sdlc-conventions.md`) — read its COMPONENT MENU first: your mission chooses the build path (the simple default flow · the wave model · the assigned rigorous overlay) and the planning rigor (the P0–P4 dial); do not assume the heavy flow unless your mission or dispatch assigns it. Full flow for the default path: the `mission-slice-sop` skill. The floor on every path: track on PROGRESS.md; evidence lands via `rig proof add` (never hand-placed); a slice is **not done** until its promised outcomes have evidence; verify with `rig scope audit`.
