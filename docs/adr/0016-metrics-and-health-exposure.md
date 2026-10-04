# ADR-0016 — Metrics in Prometheus format, readiness with the database, status-only health, quiet parser errors

- Status: accepted at the `03-operate` plan-lock (2026-10-03T09:41Z; status line set 09:48Z); `03-dogfood-fix` amendment accepted at that slice's plan-lock and merged in `5c264db`; `01-analytics-v2` amendment accepted at its plan-lock and merged in `c9b66dd` (see *Amendment* sections)
- Date: 2026-10-03
- Slice: `03-operate`

## Context

NFR-O3 asks for request timers per endpoint, a redirect count, a rejection
counter and pool gauges in Prometheus format. NFR-R1 asks for readiness that
follows the database and liveness that does not. The slice SPEC (rules 9 and
10, AC-15, AC-19) forbids installation details in health bodies and any
client, code, URL or header value in metric tags. A-17 asks the design to
check what the embedded server logs for requests it rejects before the
application sees them.

## Decision

- **Registry.** `runtimeOnly("io.micrometer:micrometer-registry-prometheus")`,
  version from the Boot BOM (1.17.1). Boot 4.1.1's
  `spring-boot-micrometer-metrics` already holds the Prometheus
  auto-configuration. Exposure becomes `health,info,metrics,prometheus`.
- **Names.** `http.server.requests`: Boot's observation timer, tagged with
  route templates (`/{code:[A-Za-z0-9]{6,32}}`, `/api/links`), `/**` for a
  no-handler `404`, and `UNKNOWN` for a filter `429` and for a non-standard
  method. The redirect count is its count for the redirect template with
  status `302` (A-10). `urlshort.ratelimit.rejections{budget}` is the
  rejection counter (ADR-0014). `hikaricp.connections.active` and
  `hikaricp.connections.idle` are the pool gauges (`hikaricp_connections_*`
  in Prometheus).
- **Health.** `management.endpoint.health.group.readiness.include=readinessState,db`.
  Liveness keeps `livenessState` only, and `show-details=never` is pinned.
  Bodies are `{"status":…}` (plus the group names on `/actuator/health`).
- **Invalid resource paths.**
  `logging.level.org.springframework.web.servlet.resource.ResourceHandlerUtils=error`.
  MVC's resource handler WARNs the whole submitted path when it refuses one
  (`/actuator/../<anything>` is exempt from the limiter and answers `404`),
  so a client's path, IP-shaped or not, reached the log (design review
  DR-01). The reviewer's control showed the same `404` with no log line.
- **Parser errors.** `logging.level.org.apache.coyote.http11.Http11Processor=warn`.
  Tomcat logs its first request-parse error at INFO with the offending
  bytes, so a client's request target reached the log, without a request id.
- **One accepted framework throwable.** When the database check fails,
  `DataSourceHealthIndicator` WARNs with the exception, once per health
  request. The request is a probe and carries no client value. The exception
  names the database (URL or file), which is the Operator's reason for a
  `503` readiness. It stays, as the single exception to ADR-0004's "no
  throwable on a request path" (ADR-0004, third amendment).

## Consequences

- `/actuator/prometheus` is anonymous like every endpoint (NFR-S6) and
  published on loopback only (ADR-0017).
- A new runtime dependency: the probe resolved it online once into the
  shared `.gradle-home`, so offline seats build. The builder captures an OSV
  run for it.
- Verified on Tomcat before implementation:
  `missions/01-greenfield-core/slices/03-operate/design-probe/output.txt`
  (O2, O2b, O3 tags and names; O4–O5c health; O5b the WARN; O6/O6b parser
  errors before and after).

## Amendment — `01-analytics-v2` (2026-10-03, proposed; accepted at that slice's plan-lock)

Two counters make the analytics' own data loss scrapeable (NFR-O3, mission 03 SPEC rule 10):
- **`urlshort.clicks.recorded`**, incremented by the click writer whenever an insert returns,
  including one whose report shutdown already claimed (it can still commit);
- **`urlshort.clicks.lost`**, tag `reason`, incremented beside every `click lost` event with that
  event's static token: `rejected`, `reduction failed` (from `02-click-retention`), `write failed`,
  `shutdown deadline`, `shutdown deadline, outcome unknown`.

Both are registered by `ClickRecorder` at construction, every `reason` included, so the families
are present at zero. They render as `urlshort_clicks_recorded_total` and
`urlshort_clicks_lost_total{reason="…"}`; a value with a comma is quoted as it is (`01-analytics-v2`
probe S4). No other tag exists, and no tag can carry a client value, link code, id or referrer.

## Amendment — `03-dogfood-fix` (2026-10-03, proposed; accepted at that slice's plan-lock)

**Context.** The anonymous scrape tagged Micrometer's disk gauges with the absolute data path
(`disk_free_bytes{path="/Users/…/url-shortener/."}`), and `/actuator/metrics/disk.free` listed it
(QA's dogfood W2-03; `GAPS.md` QA-OPR-02). `03-operate`'s rule 9 keeps installation details out of
health bodies, and the same holds for the metrics surfaces. No Boot property removes one tag:
`management.metrics.*` can add tags, switch meters off or choose the disk paths.

**Decision.**
- `web.MetricsConfig` declares `MeterFilter.ignoreTags("path")`. Boot applies it to every
  registry, so the Prometheus scrape and `/actuator/metrics` both show `disk.free` and
  `disk.total` without tags, with their values.
- The filter covers every meter, not only `disk.*`. On the shipped scrape only the two disk gauges
  carry `path`. This ADR already bars a request path from tags, and a filesystem path is an
  installation detail, so no meter here may carry a `path` worth keeping.
- The gauges stay. Free space is what an Operator watches for a file database. Switching them off
  (`management.metrics.enable.disk=false`) was rejected.

**Consequences.**
- No metric tag carries a filesystem path.
- A consumer that selected the disk gauges by `path` must drop that selector. An Actuator
  `tag=path:<value>` filter answers `404`, and a PromQL `path="…"` matcher selects nothing. The
  unfiltered gauge is the same single series (`03-dogfood-fix` design review, DR-02).
- If a second disk path is configured (`management.metrics.system.diskspace.paths`), its gauges
  would collide with the first. The change that adds the path also adds a non-path tag per path
  (for example `volume`).
- Verified before implementation:
  `missions/02-brownfield/slices/03-dogfood-fix/design-probe/output.txt` (D0, D3) and
  `test-output.txt` (T1, T2, T5).
