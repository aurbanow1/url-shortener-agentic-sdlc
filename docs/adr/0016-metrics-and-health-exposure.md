# ADR-0016 — Metrics in Prometheus format, readiness with the database, status-only health, quiet parser errors

- Status: proposed (becomes accepted at the `03-operate` plan-lock)
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
