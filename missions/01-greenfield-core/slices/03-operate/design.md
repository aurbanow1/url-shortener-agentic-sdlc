---
slice: 03-operate
mission: 01-greenfield-core
spec: SPEC.md
spec_candidate: b53372f
status: proposed
created: 2026-10-03
---

# Design — Slice 03 Operate safely

Reader: the Development Agent, with `SPEC.md` (candidate `b53372f`, passed on
re-review) open beside this file. The design sits on `main` with
`01-create-redirect` merged and `02-analytics`'s design (`71b2e10`) in review;
this slice's candidate must descend from `02`'s merge (SPEC, *Non-functional*).
Tags as before: **[probe]** = run by `design-probe/OperateProbe.java` on the
shipped app and real Tomcat (§12); **[jar]** = read from the class files;
**[docs]** = reference documentation, not executed.

The shape in one paragraph. One servlet filter, after the request id and
Boot's observation filter, charges every non-operator request to one of two
per-client buckets and answers `429` itself when the bucket is empty. A bucket
is one `long` (the GCRA form of rule 2's token bucket), kept in a map that
forgets a client once its bucket is full again. Everything else is
configuration: the Prometheus registry and exposure, a readiness group that
includes the database, a 10 s shutdown phase, one logging level that keeps
Tomcat's parse errors out of the log, and the container and compose
hardening. `scripts/smoke.sh` gains the release-level checks.

## 1. Components touched

| Component | Location | New or changed | Responsibility |
|---|---|---|---|
| Rate-limit filter | `web.RateLimitFilter extends OncePerRequestFilter` (`@Component`, `@Order(Ordered.HIGHEST_PRECEDENCE + 2)`) | **new** | For each request: (1) the lookup path is `UrlPathHelper.defaultInstance.getLookupPathForRequest(request)`, which is decoded and has `;` content removed, the same decoded segments MVC routes on. If it is `/actuator` or starts with `/actuator/`, `/v3/api-docs`, `/swagger-ui.html` or `/swagger-ui/`, the request passes uncounted (rule 1). (2) The budget is `CREATE` when the path is `/api` or starts with `/api/`, else `REDIRECT`. (3) The client is `clientOf(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), trustedProxies)` (rule 5, below). (4) `retryAfter = limiter.tryTake(budget, client)`. If it is `0`, `chain.doFilter`. Otherwise it increments the budget's counter and writes the `429` (§2) without calling the chain. It never logs: the request's one event is `RequestIdFilter`'s `request completed` with status `429` (rule 7) **[probe O1]**. |
| Client identity | `RateLimitFilter.clientOf(String remote, @Nullable String xff, Set<String> trusted)` (static, package-private) | **new** | If `remote` is not in `trusted`, return `remote`. Otherwise split `xff` on `,`, trim each entry, walk from the right, and return the first non-empty entry that is not in `trusted`; if there is none, or the header is absent or empty, return `remote`. `Forwarded`, `X-Real-IP` and everything else are never read. Entries are compared as text, exactly as configured. Tomcat reports IPv4 in dotted form and IPv6 in its full form, and the operator setting must use the same text (CIDR and normalisation are out of scope, A-6). |
| Buckets | `web.RateLimiter` (`@Component`) | **new** | `long tryTake(Budget budget, String client)` → `0` (admitted) or the `Retry-After` in whole seconds (≥ 1). Per budget, a `ConcurrentHashMap<String, Long>` from client to its **theoretical arrival time** (TAT, epoch nanoseconds from the application `Clock`). With `N` per minute: emission interval `I = 60_000_000_000 / N` ns, tolerance `T = I × (N − 1)`. Under `map.compute`: `start = max(tat, now)`; `wait = start − T − now`. If `wait > 0`, the request is refused, the TAT is unchanged (a `429` takes nothing, rule 2) and the result is `Math.ceilDiv(wait, 1_000_000_000)`. Otherwise the TAT becomes `start + I` and the result is `0`. This is rule 2's bucket: capacity `N`, refill `N/60` per second, one token per admitted request. AC-3(a), AC-3(b) and AC-4 replay exactly **[probe B1–B3]**. **Release (memory bound):** a client's bucket is full again exactly when `tat ≤ now`. At most once per second (`AtomicLong nextSweep`, CAS), the limiter runs `map.values().removeIf(tat -> tat <= now)` on both maps. That is conditional on the value, so it cannot drop a bucket that `compute` updates concurrently. An entry therefore lives at most 61 s after its client's last admitted request. Memory is bounded by the distinct clients of the last 61 s, at about 200 bytes each (§6). |
| Settings | `web.RateLimitProperties` (`@ConfigurationProperties("urlshort.rate-limit")`, `@Validated` record) and `web.RateLimitConfig` (`@Configuration(proxyBeanMethods = false)`, `@EnableConfigurationProperties(RateLimitProperties.class)`) | **new** | `@DefaultValue("60") @Positive int createPerMinute`, `@DefaultValue("600") @Positive int redirectPerMinute`, `@DefaultValue({}) Set<String> trustedProxies` (rule 11). Environment overrides by relaxed binding **[docs]**: `URLSHORT_RATELIMIT_CREATEPERMINUTE`, `URLSHORT_RATELIMIT_REDIRECTPERMINUTE`, `URLSHORT_RATELIMIT_TRUSTEDPROXIES` (comma-separated). |
| Rejection counter | in `RateLimitFilter`'s constructor | **new** | `Counter.builder("urlshort.ratelimit.rejections").tag("budget", "create" / "redirect").register(registry)`, both registered at startup, so AC-17 can read either tag before the first rejection. The only tag is `budget` (rule 10). |
| Body-limit filter | `web.RequestBodyLimitFilter` | **changed** | `@Order(Ordered.HIGHEST_PRECEDENCE + 3)` (was `+ 1`), so the limiter runs before it. Order ties are broken by registration order, which the design must not rely on. The order today, observed **[probe O1b]**: `RequestIdFilter` → `CharacterEncodingFilter` → `ServerHttpObservationFilter` → (limiter) → `RequestBodyLimitFilter`. A `413` body is never read before the charge (AC-9). |
| API document customiser | `web.OpenApiConfig`: `@Bean OpenApiCustomizer tooManyRequests()` | **changed** | Adds to **every** operation a `429` response (rule 1 limits every documented operation). Description `"Too many requests from this client; retry after Retry-After seconds"`; header `Retry-After` with schema `integer`, `minimum: 1`; content `application/problem+json` with `$ref: #/components/schemas/ProblemDetail` and one example `{"instance":"urn:uuid:3b1f0e6a-9c2d-4f57-8a41-0d5e2c7b9f10","status":429,"title":"Too Many Requests"}` (AC-20). |
| Shipped configuration | `src/main/resources/application.properties` | **changed** | `urlshort.rate-limit.create-per-minute=60`, `urlshort.rate-limit.redirect-per-minute=600`, `urlshort.rate-limit.trusted-proxies=` (operator-visible defaults, matching the record); `spring.lifecycle.timeout-per-shutdown-phase=10s` (A-15); `management.endpoints.web.exposure.include=health,info,metrics,prometheus`; `management.endpoint.health.group.readiness.include=readinessState,db` (liveness keeps its default, `livenessState` only); `management.endpoint.health.show-details=never`, pinning the default that AC-15 relies on; `logging.level.org.apache.coyote.http11.Http11Processor=warn` (A-17 finding, §5) **[probe O6, O6b]**. |
| Build | `build.gradle.kts` | **changed** | `runtimeOnly("io.micrometer:micrometer-registry-prometheus")`, version from the Boot BOM (1.17.1, bringing `io.prometheus:prometheus-metrics-*` 1.7.0). Boot 4.1.1's `spring-boot-micrometer-metrics`, already on the classpath through the actuator starter, holds the Prometheus auto-configuration **[jar]**. **Offline seats:** the design probe resolved these artifacts online on 2026-10-03, so they now sit in the shared `.gradle-home` (`scripts/gw` pins it for every worktree). The builder captures a fresh OSV run for the new coordinates, as slice 01 did for its overrides. |
| Functional-suite overlay | `src/functionalTest/resources/application-functional.properties` | **changed (grant)** | `urlshort.rate-limit.create-per-minute=1000000` and `…redirect-per-minute=1000000`, with a comment: the suite sends hundreds of requests a minute from MockMvc's `127.0.0.1`; the limiter journeys set the shipped numbers inline. Granted on `qitem-20261003084948-76a4a31d`. |
| Suite clock | `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` | **changed (grant)** | `freeze()` pins the real part at the current instant, after which `shift` moves a frozen clock exactly. `reset()` also unfreezes. Granted on the same item; every test that freezes resets in `@AfterEach` (the lead's condition). |
| Container | `compose.yaml`, `Dockerfile` | **changed** | §2.4. |
| Smoke and bench | `scripts/smoke.sh` | **changed** | §2.5. |

**Not touched:** `link/`, `click/`, `db/migration/` (SPEC scope); `docs/api/openapi.json` until this slice's candidate is rebased onto `02`'s merge.

**Why a filter, not an interceptor.** Rule 3 charges `405`, `404` and `415`
the same as successes. A `405` is decided during handler lookup, before any
`HandlerInterceptor` runs, so only a filter sees every request. The filter
sits after `ServerHttpObservationFilter`, so `429`s appear in
`http.server.requests` (status `429`, `uri` `UNKNOWN`) **[probe O2]**, which
is what an Operator watching load wants. The observation filter examines
nothing about the request, so rule 3's "charged before anything else" still
holds.

**Why classify on the decoded lookup path.** MVC matches decoded segments,
so `/%61pi/links` reaches the create endpoint. Classifying on the raw URI
would charge it to the 600/min budget, a tenfold evasion. With the lookup
path it is charged to the create budget. A `..` segment is not resolved by
MVC's matcher, so `/actuator/../<code>` routes nowhere: it stays exempt and
answers `404`, and never reaches the redirect (§7 test row).

**A-9 (02-analytics' click address) is not changed here.** A request wrapper
overriding `getRemoteAddr()` with `clientOf(...)` would make `02`'s click hash
follow the trusted-proxy rule in about four lines. The `03` SPEC does not ask
for it, and nothing reads the hash in mission 01, so it stays on the lead's
backlog with this mechanism named (§11).

Mechanics the builder relies on:

- **A `ProblemDetail` serialised by the context's `JsonMapper` bean** has
  the same shape as the advice's: `{"instance":"urn:uuid:<id>","status":429,"title":"Too Many Requests"}`,
  with no `type` and no `detail` **[probe O1]**. The filter therefore needs no
  MVC machinery to keep ADR-0002's contract.
- **`ServerHttpObservationFilter` tags** use route templates
  (`/{code:[A-Za-z0-9]{6,32}}`, `/api/links`), `/**` for a no-handler `404`,
  `UNKNOWN` for a filter `429` and for a non-standard method token, never a
  concrete path, code or method **[probe O2, O3]**. Rule 10 holds with no
  code here.
- **Pool gauges** are `hikaricp.connections.active` and
  `hikaricp.connections.idle` (also `jdbc.connections.active`/`idle`); in
  Prometheus: `hikaricp_connections_active`, `hikaricp_connections_idle`
  **[probe O2b, O3]**.
- **Readiness with the database:** with `getConnection` failing, readiness
  answers `503 {"status":"DOWN"}`, liveness `200 {"status":"UP"}`, and
  `/actuator/health` `503 {"groups":["liveness","readiness"],"status":"DOWN"}`.
  No body carries a URL, path, product or host, and readiness returns to `UP`
  when the database answers **[probe O4, O5]**.
- **Graceful shutdown in Boot 4.1.1** pauses each connector and calls
  `closeServerSocketGraceful()`, then polls every 50 ms until no request is
  active or the phase ends. It logs `Commencing graceful shutdown…` and
  `Graceful shutdown complete` **[jar]**. A new connection is refused once the
  socket is closed **[probe D, P]**.
- **`SpringApplicationBuilder.properties(...)` are default properties**, so
  the shipped file outranks them. Probes and tests set overrides as arguments
  or inline test properties.

## 2. API contract

### 2.1 The `429` (every limited request, AC-1 to AC-12)

| Item | Value |
|---|---|
| Status | `429 Too Many Requests` |
| Headers | `Retry-After: <whole seconds ≥ 1>` (rule 4); `X-Request-Id` (set before the limiter, ADR-0003); `Content-Type: application/problem+json`; nothing else from this slice |
| Body | `{"instance":"urn:uuid:<X-Request-Id>","status":429,"title":"Too Many Requests"}`. No `detail`, no `errors`, no client value (rule 6, AC-11) |
| Not produced | `Location` (a redirect over the limit is never resolved, AC-2), any audit row (AC-1 counts `link.create` rows), any click (the hook is never reached) |

Admitted requests are unchanged (rule 6). Exempt paths (`/actuator/**`,
`/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**`) are never charged.

### 2.2 Operational endpoints

| Endpoint | Answer |
|---|---|
| `GET /actuator/health/liveness` | `200 {"status":"UP"}` while the process runs (database not consulted) |
| `GET /actuator/health/readiness` | `200 {"status":"UP"}` when started and the database answers; else `503 {"status":"DOWN"}` |
| `GET /actuator/health` | `{"groups":["liveness","readiness"],"status":…}`, status only (`show-details=never`) |
| `GET /actuator/metrics`, `/actuator/metrics/{name}` | names include `http.server.requests`, `urlshort.ratelimit.rejections`, `hikaricp.connections.active`, `hikaricp.connections.idle` |
| `GET /actuator/prometheus` | `200 text/plain;version=0.0.4` (Prometheus text format) with `http_server_requests_seconds`, `urlshort_ratelimit_rejections_total{budget=…}`, `hikaricp_connections_active`/`_idle` |

### 2.3 API document

Every operation in `paths` gains the `429` of §1's customiser, including
`02-analytics`' `GET /api/links/{code}/stats` once rebased. The committed
`docs/api/openapi.json` is regenerated on the rebased candidate, key-sorted
(ADR-0010), and the ancestry check of the proof contract holds.

### 2.4 Container (`compose.yaml`, `Dockerfile`)

```yaml
services:
  urlshort:
    build: .
    image: urlshort:local
    ports:
      - "127.0.0.1:8080:8080"          # AC-22: loopback only
    read_only: true                     # AC-23: read-only root filesystem
    tmpfs:
      - /tmp:size=64m,mode=1777         # A-14: JVM perf data, Tomcat work dir, H2 temp files
    volumes:
      - urlshort-data:/app/data         # the only persistent writable mount
    stop_grace_period: 20s              # AC-28: > the 10 s phase + context close (click drain, pool)
    healthcheck:                        # AC-21: asks readiness, not an open port
      test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
      interval: 10s
      timeout: 3s
      retries: 6
      start_period: 20s
volumes:
  urlshort-data: {}
```

`Dockerfile`: unchanged in substance. It already runs as uid 10001
(`urlshort`), uses the exec-form `ENTRYPOINT`, so the JVM is PID 1 and gets
`SIGTERM`, and points the database at `/app/data`. One comment line records
that the image expects a read-only root with `/tmp` as tmpfs. The health check
uses HTTP/1.0, so the server closes the connection after the response and
`grep` reads to the end; the JRE image has no `curl`. Not run by me (no Docker
step in this design); AC-21 to AC-23 and AC-28 prove it at `release_prep`.

### 2.5 `scripts/smoke.sh`

All HTTP goes through `scripts/http` (loopback-only curl). Modes:

| Invocation | Steps (each asserts an effect) | ACs |
|---|---|---|
| `scripts/smoke.sh [base]` (default, as today) | existing checks; plus liveness and readiness `UP`; `/actuator/metrics` names contain `http.server.requests`, `urlshort.ratelimit.rejections`, `hikaricp.connections.active`, `hikaricp.connections.idle`; `/actuator/prometheus` is non-empty and holds `urlshort_ratelimit_rejections_total` | AC-21 (and AC-26 when run against the jar) |
| `scripts/smoke.sh --restart` | creates link `C` through the published port; `docker compose restart`, wait for `healthy`, `GET /C` → `302` same target, `GET /api/links/C` → `200` same body; then `docker compose down` (no `-v`) + `up -d`, same checks. A background load loop and a held `R0` (as in `--drain`) run across the restart: `R0` must complete `2xx/3xx`, every HTTP response received is `2xx/3xx`, connection failures are counted and printed, not judged; `docker inspect -f '{{.Config.StopTimeout}}'` > 10 | AC-24, AC-28 |
| `scripts/smoke.sh --drain <jar>` | starts `java -jar <jar>` on a free loopback port with a temp data dir; runs one or two load loops, each one request per connection (`-H 'Connection: close'`), paced with `sleep 0.05` (about 20 req/s per loop), classifying curl exit codes: `0` with `2xx`/`3xx` → ok, `7` (could not connect) → refused (reported, not a failure), `52`/`56`/`28` → failure after acceptance; holds `R0` with bash `exec 3<>/dev/tcp/…`, sending the headers of a `POST /api/links` with `Content-Length` and half the body; sends `SIGTERM`; after 0.5 s tries `exec 4<>/dev/tcp/…` (must fail: refused); sends the rest of `R0`'s body and reads its status (must be `2xx`, within 10 s of the signal); checks the process log for `Graceful shutdown complete`; prints ok / refused / failure counts and fails on any failure | AC-25 |
| `scripts/smoke.sh --bench [base]` | prints the budgets it expects to have been raised (AC-27); runs 10 parallel loops of 10 req/s for 60 s on `GET /<code>` (100 req/s) and 2 loops of 10 req/s on `POST /api/links` (20 req/s), each request timed by curl's `%{time_total}`; prints, per scenario, achieved rate, count, non-`2xx`/`3xx` count and p50, p95, p99 in ms (`sort -n` + `awk`); states its own limits (a curl process per request, closed client loops on one laptop): a noisy run is a gap, not a pass | AC-27 |

**Holding `R0` without a test endpoint:** the request is a real create
whose body arrives slowly. Tomcat dispatches it once the headers are in, so
it counts as active while Jackson waits for the rest **[probe D, P: `R0` →
`201` after about 0.5 s every time]**.

## 3. Data model & migration

None. Limiter state is in memory only and never stored (rule 6, rule 8). No
migration, no ERD change.

## 4. Sequences

Rate limit (`docs/diagrams/ratelimit-sequence.mmd`):

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (peer P, optional X-Forwarded-For)
    participant F as RequestIdFilter
    participant O as ServerHttpObservationFilter (Boot)
    participant RL as RateLimitFilter
    participant L as RateLimiter (two maps client→TAT)
    participant B as RequestBodyLimitFilter → DispatcherServlet
    participant M as MeterRegistry

    C->>F: request
    F->>F: X-Request-Id R, MDC requestId=R
    F->>O: chain (timer starts)
    O->>RL: chain
    alt lookup path under /actuator, /v3/api-docs, /swagger-ui*
        RL->>B: chain (not charged)
    else limited
        RL->>RL: budget = /api… ? CREATE : REDIRECT; client = clientOf(P, XFF, trusted)
        RL->>L: tryTake(budget, client)
        alt admitted (0)
            L-->>RL: 0 (TAT advanced)
            RL->>B: chain: 2xx…5xx as before
        else empty bucket
            L-->>RL: Retry-After S (TAT unchanged)
            RL->>M: urlshort.ratelimit.rejections{budget}++
            RL-->>C: 429 problem+json, Retry-After: S, instance urn:uuid:R
        end
    end
    O->>O: http.server.requests{uri template or UNKNOWN, status}
    F->>F: INFO "request completed" {status}; MDC.remove
```

Shutdown: `SIGTERM` → Boot's shutdown hook → web server graceful phase
(connector paused, listening socket closed, so new connections are refused;
in-flight requests finish, at most 10 s) → context close (`02`'s click writer
drains, the pool closes) → exit. All of this sits inside compose's 20 s stop
grace.

## 5. Logging & audit events

| Event | Logger | Level | Fields | Must never appear |
|---|---|---|---|---|
| request completed (a `429` included) | `dev.urlshort.web.RequestIdFilter` | INFO | `requestId`, `status` | as slice 01; the limiter adds no event (rule 7) |
| DataSource health check failed | `org.springframework.boot.jdbc.health.DataSourceHealthIndicator` (framework) | WARN | `requestId` (the probe's request), the throwable (`error.type`, `error.message`, `error.stack_trace`) | **accepted exception to ADR-0004's "no throwable on a request path"**: the request is a health probe and carries no client value. The exception describes the database (it may name the JDBC URL or file), and that is the Operator's diagnosis for a `503` readiness. One line per probe while the database is down **[probe O5b]** |
| Tomcat request-parse errors (malformed request line, bad method, oversized header) | `org.apache.coyote.http11.Http11Processor` | INFO before, **suppressed now** | — | Tomcat's first parse error per connector is logged at INFO **with the offending bytes**: the request target canary appeared in the log line, and the line has no `requestId` (it happens before any filter) **[probe O6]**. Shipped `Http11Processor=warn` removes it **[probe O6b]**. This answers A-17: no request id or problem detail is promised there, and now no client value is logged |

Audit: none (no mutation). Metrics are §2.2; no metric tag carries a
client address, code, URL or header value (rule 10, **[probe O3]**).

## 6. Threat model

Assets: availability for every client; fairness between clients; client
privacy in the limiter; the operator surfaces; the host the container runs
on.

Entry points: every request (peer address, `X-Forwarded-For`, path, method);
the operator settings (budgets, trusted proxies); `/actuator/**` including
the new `/actuator/prometheus`; the container (filesystem, user, port, stop
signal).

| STRIDE | Threat | Mitigation in this slice | Residual / owner |
|---|---|---|---|
| Spoofing | A client forges `X-Forwarded-For`, `Forwarded` or `X-Real-IP` to get a fresh bucket | only `X-Forwarded-For`, only from an exactly configured trusted peer, right-most untrusted entry (the one the proxy appended); default: no trusted proxy (AC-7, AC-8) | an operator who lists a proxy that forwards client-supplied headers unchanged; documented |
| Tampering | Percent-encoding or `;` content shifts a request to the larger budget (`/%61pi/links`) | classify on MVC's decoded lookup path (§1) | none |
| Tampering | Path tricks into the exempt prefixes (`/actuator/../<code>`) | the exempt request is routed by the same decoded segments: no handler matches, so it answers `404` and never reaches a limited operation (§7) | unlimited `404`s under `/actuator/**` are the SPEC's own exemption (A-7) |
| Repudiation | Rejections invisible to the Operator | one correlated event per `429`; counter per budget; `429`s in `http.server.requests` | none |
| Information disclosure | Client address, forwarded value, `User-Agent` or submitted value in a `429` | body built from server values only; one extra header (`Retry-After`) (AC-11) | none |
| Information disclosure | Client values in logs or metric tags | limiter logs nothing; tags are `budget`, route templates, `UNKNOWN` for non-standard methods (**[probe O2, O3]**); Tomcat parse errors no longer logged (**[probe O6b]**) | none |
| Information disclosure | Installation details in health bodies | `show-details=never`; status only (**[probe O4, O5]**) | the DB-health WARN in logs names the database (accepted, §5) |
| Information disclosure | `/actuator/prometheus` describes load and pool sizes | operational, not personal; anonymous by NFR-S6; published on loopback only | exposure beyond loopback is out of scope |
| Denial of service | One client floods creates or redirects | per-client buckets, 60/min and 600/min; checked before any body is read (rule 3, AC-9) | distributed floods from many addresses (rate limiting per client only; NFR-R4 single node) |
| Denial of service | Limiter memory with many distinct clients | entries released once full (≤ 61 s after the last admitted request); bound = distinct clients in the last 61 s × ~200 B per budget; sweep O(n) at most once a second; unit test proves release | a very large number of distinct real peers per minute; bounded by connection rate |
| Denial of service | Unlimited operator paths | exempt by rule 1 (A-7) | accepted by the SPEC |
| Denial of service | Deploy kills in-flight requests | graceful 10 s phase; listening socket closed first; compose stop grace 20 s | **measured residual:** under a closed loop of about 3 500 new connections per second, 2 connections per stop are reset after the kernel accepted them (Tomcat's acceptor stops before the socket closes); at 100 req/s, 0 in 10 stops (**[probe D1, D2, P1–P10]**). Estimated at about 0.06 per stop at 100 req/s. See §12 for AC-25 |
| Elevation of privilege | A compromised process writes to the image or the host | non-root uid 10001; read-only root; only `/app/data` (named volume) and a 64 MB tmpfs `/tmp` writable; loopback publish | no `cap_drop`/`no-new-privileges` (§11) |

## 7. Test strategy hints

### 7.1 Suites and classes

Functional (`src/functionalTest/java/dev/urlshort/web/`):

| Class | Context | ACs |
|---|---|---|
| `RateLimitJourneyTest` | `@SpringBootTest(properties = {"urlshort.rate-limit.create-per-minute=60", "urlshort.rate-limit.redirect-per-minute=600"})` + MockMvc (inline properties outrank the overlay) | AC-1, AC-2, AC-3 (a, b), AC-4, AC-5, AC-6, AC-7, AC-9, AC-11, AC-12, AC-16, AC-17, AC-19; plus rows: `/%61pi/links` charged to the create budget; `/actuator/../<code>` → `404`, no redirect; exempt paths never `429` under exhaustion |
| `TrustedProxyJourneyTest` | as above + `urlshort.rate-limit.trusted-proxies=10.9.9.9` | AC-8 |
| `RateLimitSettingsJourneyTest` | `create-per-minute=2`, `redirect-per-minute=3` | AC-10 |
| `RateLimitDefaultsTest` | none (reads the classpath) | the lead's condition: loads the shipped `application.properties` (the functional suite has no shadowing copy) and asserts `60`, `600` and an empty proxy list; binds `RateLimitProperties` from an empty source and asserts the same record defaults. A wrong shipped default fails here even though the overlay and the inline numbers hide it elsewhere |
| `HealthMetricsJourneyTest` | base | AC-13, AC-15 (database up), AC-18 (count delta of `http.server.requests` for the redirect template and `302`, through `/actuator/metrics/http.server.requests?tag=uri:…&tag=status:302`) |
| `DatabaseDownJourneyTest` | `@MockitoSpyBean DataSource dataSource` | AC-14, AC-15 (database down): `doThrow(new SQLException("database not answering")).when(dataSource).getConnection()`, then `reset(dataSource)` |
| `OpenApiDocumentTest` | base (existing) | AC-20 added: every operation has a `429` with `application/problem+json`, a `Retry-After` header with integer schema, and an example; the committed file still equals the live one |

Unit (`src/test/java/dev/urlshort/web/`):

| Class | What it proves |
|---|---|
| `RateLimiterTest` | with a mutable fixed clock: AC-3(a), AC-3(b) and AC-4 sequences verbatim; budgets and clients independent; a refusal does not move the TAT; release: 10 000 distinct clients one request each, clock + 61 s, one request → map holds 1 entry; an entry whose bucket is not yet full survives a sweep; the sweep runs at most once per second |
| `RateLimitFilterTest` | classification table (`/api`, `/api/`, `/api/links/x/stats`, `/apix` → redirect budget, `/%61pi/links` → create, each exempt prefix, `/actuatorx` → limited); `clientOf` table (AC-8's four rows, spaces around entries, all-trusted chain → peer, empty header → peer); the `429` written to a `MockHttpServletResponse` (status, `Retry-After`, content type, body equal to the advice's shape with the MDC id); counter increments per budget; no logger call |

### 7.2 Mechanisms

- **Frozen clock.** `clock.freeze()` in each limiter test (after
  `reset()`), `clock.shift(Duration.ofMillis(999))` for AC-3(a)'s steps,
  `@AfterEach clock.reset()` (the lead's condition). Because the clock is
  frozen, AC-1's 61 requests take no time for the bucket.
- **Distinct clients per test.** The limiter context is shared by every test
  in it, so each test uses its own peer address
  (`r.setRemoteAddr("10.0.<n>.<m>")`). Buckets never carry over between
  tests, and no test depends on order.
- **AC-12 windows:** capture from before the request to after the response;
  a `429` never reaches `02`'s click hook, so no asynchronous event can
  follow it (no `settle()` needed).
- **AC-17, AC-19:** read the counter through `/actuator/metrics/…?tag=budget:create`
  before and after; the Prometheus body is read once and checked for the
  absence of the code, `zzCanary99`, `10.77.77.77` and every target URL.
- **Contexts:** the suite's five plus four (`RateLimitJourneyTest`,
  `TrustedProxyJourneyTest`, `RateLimitSettingsJourneyTest`,
  `DatabaseDownJourneyTest`), about 1.5 s each. AC-7 says "default
  configuration (no trusted proxy)", so it is not folded into the proxy
  context.
- **Existing journeys keep passing** because the overlay raises both
  budgets suite-wide. That includes `02`'s 200-redirect real-server test and
  every `127.0.0.1` create in slice 01.

### 7.3 Coverage

Every branch has a named test: the exemption and classification branches,
both `clientOf` exits, admitted versus refused, sweep gate taken or skipped,
release true or false (`RateLimitFilterTest`, `RateLimiterTest`). The
release-level ACs (AC-21 to AC-28) are `GAPS.md` rows with their release
checks (SPEC proof contract). Expected unit-level gap: none.

## 8. Reachability check

| AC / rule | Reached by | Error path explicit? | Threat model? |
|---|---|---|---|
| AC-1, AC-2, AC-4, AC-5, AC-6 | `RateLimitFilter` → `RateLimiter.tryTake` per budget and client | `429` (§2.1) | flooding ✔ |
| AC-3 | GCRA `wait` → `ceilDiv` | — | ✔ |
| AC-7, AC-8 | `clientOf` | — | spoofing ✔ |
| AC-9 | limiter before the body is read; every outcome charged | — | ✔ |
| AC-10 | `RateLimitProperties` | — | ✔ |
| AC-11, AC-12 | server-only `429` body; no limiter log; filter event | — | disclosure ✔ |
| AC-13, AC-14, AC-15 | readiness group with `db`; liveness default; `show-details=never` | `503` | health ✔ |
| AC-16, AC-17, AC-18, AC-19 | Prometheus registry; counter by budget; Boot's route-templated timer | — | metrics ✔ |
| AC-20 | `OpenApiCustomizer` | — | ✔ |
| AC-21 to AC-28 | `compose.yaml`, `Dockerfile`, `application.properties`, `scripts/smoke.sh` modes | — | container, shutdown ✔ |
| rules 1–13 | 1: exemptions + `/api` rule; 2–4: GCRA; 5: `clientOf`; 6: §2.1, §5; 7: no limiter log; 8: in-memory maps; 9: health config; 10: tags; 11: properties; 12: compose; 13: 10 s phase + 20 s grace | — | — |

## 9. Territory (`slice.yaml`, with the 08:50Z grants)

| Path | In `slice.yaml` | Needed |
|---|---|---|
| `src/{main,test,functionalTest}/java/dev/urlshort/web/` | yes | `RateLimitFilter`, `RateLimiter`, `RateLimitProperties`, `RateLimitConfig`, `RequestBodyLimitFilter` order, `OpenApiConfig` customiser; their tests; `OpenApiDocumentTest` (after `02`'s one-line change) |
| `src/main/resources/application.properties` | yes | §1 |
| `build.gradle.kts` | yes | one `runtimeOnly` line |
| `Dockerfile`, `compose.yaml`, `scripts/smoke.sh` | yes | §2.4, §2.5 |
| `docs/api/openapi.json` | yes, second holder after `02`'s merge | regenerated on the rebased candidate |
| `src/functionalTest/resources/application-functional.properties` | granted (`qitem-20261003084948-76a4a31d`) | two budget lines |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClock.java` | granted (same item) | `freeze()` |
| `link/`, `click/`, `db/migration/` | no | not touched |

## 10. Decisions recorded as ADRs

| ADR | Decision | Status |
|---|---|---|
| [ADR-0014](../../../../docs/adr/0014-rate-limit-filter-gcra.md) | per-client rate limit in a servlet filter after the request id and observation filters; two budgets by decoded path (`/api…` = create), operator paths exempt; GCRA bucket per client and budget, every outcome charged, a `429` takes nothing; `429` problem written by the filter; `Retry-After` rounded up; in-memory state released when full; settings | proposed |
| [ADR-0015](../../../../docs/adr/0015-client-identity-trusted-proxies.md) | client = peer address; `X-Forwarded-For` only from exactly listed proxies, right-most untrusted entry; no other header; `getRemoteAddr()` not rewritten (A-9 stays backlog) | proposed |
| [ADR-0016](../../../../docs/adr/0016-metrics-and-health-exposure.md) | Prometheus registry and `/actuator/prometheus`; `urlshort.ratelimit.rejections{budget}`; route-template timers; pool gauges; readiness group with `db`, liveness without; status-only health bodies; `Http11Processor` at WARN; the DB-health WARN accepted | proposed |
| [ADR-0017](../../../../docs/adr/0017-container-hardening-and-shutdown.md) | non-root, read-only root with tmpfs `/tmp`, named data volume, loopback publish, readiness health check, 10 s shutdown phase, 20 s stop grace; drain behaviour and its measured residual | proposed |
| [ADR-0004](../../../../docs/adr/0004-structured-ecs-logs-no-client-pii.md) (amended) | third amendment: Tomcat's parse errors suppressed; the DB-health WARN is the one accepted framework throwable on a request path | amended |

## 11. Trade-offs

| Option | Why not now | What would change the decision |
|---|---|---|
| A `HandlerInterceptor` | runs after handler lookup, so `405`s and no-handler `404`s would never be charged (rule 3) | — |
| Classify on the raw `getRequestURI()` | `/%61pi/links` reaches the create endpoint but would be charged to the 600/min budget | — |
| Bucket4j, Resilience4j or Caffeine | a new dependency (and not in the offline cache) for one map and three lines of arithmetic | distributed limits across instances (NFR-R4 says single node) |
| Token count + last-refill time per bucket (two numbers) | GCRA keeps one `long`, gives `Retry-After` directly, and "full again" is `tat ≤ now` | — |
| Tomcat `RemoteIpValve` (`server.forward-headers-strategy=native`) | MockMvc never runs a valve, so AC-8 could not be proven in-suite; its default internal-proxy regex trusts `127/8` and `10/8` | the trusted-proxy rule needing CIDR or `Forwarded` |
| Rewrite `getRemoteAddr()` with the resolved client (aligns `02`'s click hash, A-9) | not asked by this SPEC; nothing reads the hash in mission 01 | the lead routing A-9 at `wave_review`: a request wrapper in `RateLimitFilter`, about four lines |
| Limiter before `ServerHttpObservationFilter` | `429`s would vanish from `http.server.requests`; the Operator wants to see them | — |
| Log each `429` from the limiter | rule 7: one event per rejection; the filter's event already carries status `429` | — |
| Keep Tomcat's parse-error INFO | it logged the client's request target bytes (**[probe O6]**) | — |
| Silence the DB-health WARN | the Operator needs the reason readiness is down; the request carries no client value | the health exception ever carrying request data |
| `cap_drop: [ALL]`, `security_opt: no-new-privileges` in compose | hardening beyond NFR-S5's non-root + read-only, not required here; cheap to add | the release review asking for it |
| Bench in Java (`HttpClient` single-file) instead of bash + curl | a second script outside the slice's territory (`scripts/smoke.sh` only) | noisy bash numbers making AC-27 unjudgeable |
| A test-only slow endpoint to hold `R0` | would ship a test seam in the product; a slowly sent body holds a real create in flight | — |

## 12. Design probe (what was verified by effect)

`design-probe/OperateProbe.java` boots the shipped main code (functional
profile, in-memory H2, Tomcat on a random loopback port). It passes the
design's settings as command-line arguments, and the Prometheus registry is
resolved through the Boot BOM by `operate-probe.gradle`. Probe beans: a
minimal limiter filter that writes the `429` the way §1 specifies (rejecting
on a probe header), and a `DataSource` proxy whose `getConnection` fails
while a flag is set. Run from the repo root (online the first time, to
resolve the registry):

```sh
scripts/gw --log missions/01-greenfield-core/slices/03-operate/design-probe/output.txt \
    -I missions/01-greenfield-core/slices/03-operate/design-probe/operate-probe.gradle designOperateProbe
```

| Case | Result in `design-probe/output.txt` | Design item |
|---|---|---|
| B1 | AC-3(a) on a frozen clock: 60 admitted, 61st `Retry-After` 1, +999 ms refused, +1 000 ms admitted, again refused | GCRA, rule 2–4 |
| B2 | AC-3(b): after 250 ms `Retry-After` 1; after 1 s more, admitted | rule 4 |
| B3 | AC-4: after a quiet minute 60 admitted, 61st refused; "full again" true exactly when `tat ≤ now` | release rule |
| O0 | `PrometheusMeterRegistry` on the classpath after the online resolution | build |
| O1 | `429`: `content-type application/problem+json`, `retry-after 1`, `x-request-id`, body `{"instance":"urn:uuid:<id>","status":429,"title":"Too Many Requests"}`; exactly one log line, `request completed` status 429 with the id; no `User-Agent`, `X-Forwarded-For` or `url` canary in the log | AC-11, AC-12 |
| O1b | filter order: `RequestIdFilter` → `CharacterEncodingFilter` → `ServerHttpObservationFilter` → `RequestBodyLimitFilter` → probe limiter (at `+2`, which is why the design moves the body limit to `+3`) | §1 order |
| O2 | `http.server.requests` tags: `uri` ∈ {`/{code:[A-Za-z0-9]{6,32}}`, `UNKNOWN`, `/**`, `/api/links`}, `method` ∈ {GET, POST, UNKNOWN} after a `PROBEMETHODCANARY` request, `status` includes `429` | AC-18, rule 10 |
| O2b | pool metric names `hikaricp.connections.active`, `hikaricp.connections.idle`, `jdbc.connections.*` | AC-16 |
| O3 | `/actuator/prometheus` `200`; families `http_server_requests_seconds`, `hikaricp_connections_active`/`_idle`; the redirect line carries the route template and `status="302"`; no code, `zzCanary99`, method canary, favicon canary or target URL | AC-18, AC-19 |
| O4, O5, O5c | health `UP` everywhere; with `getConnection` failing: readiness `503 DOWN`, liveness `200 UP`, health `503 DOWN`, status-only bodies; readiness `UP` again afterwards | AC-13 to AC-15 |
| O5b | `DataSourceHealthIndicator` WARN with the throwable and the request's id, once per health request | §5 accepted exception |
| O6 | malformed request target, bad method, 9 KB header: Tomcat answers `400` itself; its INFO line **contains the request-target canary** and no `requestId` | A-17 finding |
| O6b | the same in a fresh context with `Http11Processor` at WARN: no line, no canary | §1 shipped setting |
| D1, D2 | drain under a closed loop (4 clients, about 3 500 new connections/s): `R0` (a create with a half-sent body) → `201` 0.5 s after the stop; probe connect at +0.5 s **refused**; context closed in about 0.55 s; **2 failures after acceptance per stop** (resets or end of stream) | AC-25 residual |
| P1–P10 | drain at 100 req/s (5 paced clients): `R0` → `201` every time; probe refused every time; about 45–50 refusals before acceptance per stop; **0 failures after acceptance in 10 stops** | AC-25 |

**AC-25, read honestly.** The predicate "zero failures after acceptance"
holds at NFR-L1's 100 req/s in all ten stops, and the smoke drain paces its
load below that (about 20 req/s per loop). It is not a guarantee. The closed
loop shows that Tomcat's acceptor stops a moment before the listening socket
closes, and a connection the kernel accepts in that window is reset. The
closed-loop counts put the window near 0.6 ms, which is about 0.06 expected
failures per stop at 100 req/s and about 0.01 at 20 req/s. The SPEC's
self-check names exactly this risk and leaves it to the design to show by
effect at the load client's rate, or to the review to bring a different
predicate. This is that evidence. If a single reset appears at
`release_prep`, it is this known race and is recorded as such with the run's
rate, not explained away.

Not verified by me: the compose file and health-check command on a Docker
engine (AC-21 to AC-23, AC-28 at release); `@MockitoSpyBean` on the
`DataSource` (the probe used a proxy); the springdoc output of the
customiser; the bash drain and bench modes themselves.

## 13. Build plan

1. `chore(03-operate): Prometheus registry` (`build.gradle.kts`, resolved
   online once; OSV capture under `proof/`), gated green alone.
2. `feat(03-operate): per-client rate limit with 429 problem details`
   (`RateLimiter`, `RateLimitFilter`, settings, counter, body-limit order,
   the overlay and `FunctionalClock.freeze()`, unit and limiter journeys,
   `RateLimitDefaultsTest`).
3. `feat(03-operate): readiness with the database, Prometheus scrape, quiet
   parser errors, 10 s shutdown` (`application.properties`, health and
   metrics journeys, `DatabaseDownJourneyTest`).
4. `feat(03-operate): container hardening and smoke modes` (`compose.yaml`,
   `Dockerfile` comment, `scripts/smoke.sh`); one `--bench` run captured
   under `proof/` (SPEC proof contract).
5. Rebase onto `02`'s merge; `feat(03-operate): 429 on every documented
   operation` (customiser, `OpenApiDocumentTest`, regenerated
   `docs/api/openapi.json`); ancestry check; traceability, coverage,
   `GAPS.md` rows for AC-21 to AC-28.

## Status

- 2026-10-03 08:45Z — design packet `qitem-20261003082416-b1057e7a` claimed after `02-analytics`' design handoff; SPEC `b53372f` (re-review PASS, evidence `46864cc`).
- 2026-10-03 08:49Z — two test-side grants requested (`qitem-20261003084948-76a4a31d`); granted 08:50Z with two conditions: prove the shipped default budgets (`RateLimitDefaultsTest`) and reset every frozen clock in `@AfterEach` (§7.2).
- 2026-10-03 08:55Z–09:00Z — design probe run twice. In the first run the probe passed settings as builder default properties, which the shipped file outranked, so `/actuator/prometheus` stayed unexposed. The recorded second run passes them as arguments and adds the paced drain cycles.
- 2026-10-03 — design written; ADR-0014 to ADR-0017 and a third ADR-0004 amendment drafted; `docs/DESIGN.md`, `container.mmd` and `ratelimit-sequence.mmd` updated. No question parked on `human@kernel`. Handed to `design_review`.

## Self-check

| # | Item | Result | Where |
|---|---|---|---|
| 1 | Every AC reachable, component named | ✔ AC-1 to AC-28 and rules 1–13 | §8 |
| 2 | Every error AC an explicit problem detail | ✔ the `429` (§2.1); readiness `503` is an actuator status body (AC-14 asks for `status` `DOWN`), not a problem detail, as Boot's health contract | §2 |
| 3 | Migration has a written rollback | n/a: no schema change | §3 |
| 4 | Log/audit events PII-free | ✔ limiter logs nothing; Tomcat's leaking INFO suppressed; the DB-health WARN carries no client value (accepted, written down) | §5 |
| 5 | Threat model covers every new entry point | ✔ peer address and forwarding headers, path classification, settings, `/actuator/prometheus`, container, shutdown; 13 rows | §6 |
| 6 | Test strategy maps each AC to a suite | ✔ in-suite ACs to seven functional and two unit classes; release ACs to smoke modes and `GAPS.md` | §7, §2.5 |
| 7 | No structure beyond the SPEC | ✔ no library; one filter, one map holder, one settings record; A-9 wrapper and compose extras recorded, not built | §11 |
| 8 | Territory respected | ✔ with the two granted test files | §9 |
| 9 | ADRs for cross-cutting choices before dependent code | ✔ ADR-0014 to ADR-0017 (the four NFR-M2 items) and the ADR-0004 amendment | §10 |
| 10 | Review carry-overs | ✔ held request and socket drain by effect (**[probe D, P]**), with the residual stated; memory bound and release test; template/status metric support (**[probe O2, O3]**); pre-application logging (**[probe O6, O6b]**) | §1, §5, §12 |
| 11 | Lead's grant conditions | ✔ `RateLimitDefaultsTest`; `@AfterEach clock.reset()` | §7 |
| 12 | Coherence with `02-analytics` | ✔ the stats operation is in the create budget and gets the `429`; a `429` never reaches the click hook; the click writer's drain runs inside the 20 s stop grace (ADR-0011's bounded upgrade if needed); the suite overlay keeps `02`'s 200-redirect test under budget | §2, §4, §7 |

### plan-review (three lenses, by hand)

**Strategy — 9/10.** Scope is the allocation (FR-10, R1–R4, S5, O3, X1, L1,
L2), split as `slice.yaml` says between what the suites prove and what the
release proves. Nothing extra is built; two cheap hardening options and A-9
are recorded, not taken.

**Design (operator surface) — 8/10.** One `429` shape that matches every
other error; a `Retry-After` that is honest to the millisecond; health that
says only `UP`/`DOWN`; metrics named once in `DESIGN.md`.

**Engineering — 8/10.** Every platform behaviour the slice relies on ran on
Tomcat. The probe found two things the SPEC could not see: Tomcat logs
client bytes on parse errors (fixed by one level), and the drain has a
sub-millisecond race that only extreme connection rates hit (measured and
stated). Issues fixed in the draft: the limiter's position relative to the
body limit (an order tie would have made it registration-dependent); raw-URI
classification (budget evasion by percent-encoding); the lead's
default-budget condition (a test that reads the shipped file).

**Recommended action:** approve `SPEC.md` + `design.md` at the delegated
plan-lock once the independent design review passes. The AC-25 residual is
the reviewers' to judge against the predicate. No human question is open.
