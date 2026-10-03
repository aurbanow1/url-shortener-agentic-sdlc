# ADR-0017 — Container: non-root, read-only root, loopback publish, readiness health check; 10 s graceful shutdown inside a 20 s stop grace

- Status: proposed (becomes accepted at the `03-operate` plan-lock)
- Date: 2026-10-03
- Slice: `03-operate`

## Context

NFR-S5 asks for a non-root process on a read-only filesystem except
`data/`. NFR-X1 asks for one container from `docker compose up --build`,
configured by environment, with data on a named volume. NFR-R3 asks for
graceful shutdown with a 10 s phase and no new connections. The slice SPEC
adds four requirements: a loopback-only publish (A-18), a health check that
asks readiness (AC-21), a stop timeout longer than the phase (AC-28), and
no accepted request reset during the drain (AC-25).

## Decision

- `Dockerfile`: unchanged in substance. It already runs as uid 10001 with an
  exec-form `ENTRYPOINT` (the JVM is PID 1 and receives `SIGTERM`) and the
  database under `/app/data`.
- `compose.yaml`: `ports: "127.0.0.1:8080:8080"`; `read_only: true`;
  `tmpfs: /tmp:size=64m,mode=1777` for JVM perf data, Tomcat's work
  directory and H2 temp files (A-14); the named volume at `/app/data` as the
  only persistent writable mount; a health check sending
  `GET /actuator/health/readiness` over HTTP/1.0 through bash's `/dev/tcp`
  (the JRE image has no `curl`) and requiring `"status":"UP"`; and
  `stop_grace_period: 20s`.
- `spring.lifecycle.timeout-per-shutdown-phase=10s` (was 20 s, A-15).
  Boot's Tomcat graceful shutdown pauses the connector and closes the
  listening socket, then waits for active requests. The context close that
  follows (`02-analytics`' click-writer drain, the pool) fits in the
  remaining 10 s of the stop grace.

## Consequences

- Measured on Tomcat: a request whose body is still arriving at the stop
  completes `201`, and a new connection 0.5 s after the stop is refused. At
  100 req/s, none of 10 stops reset an accepted connection.
- **Residual:** under a closed loop of about 3 500 new connections per
  second, about 2 connections per stop are reset after the kernel accepted
  them. Tomcat's acceptor stops a moment before the listening socket closes.
  That puts the window near 0.6 ms: about 0.06 expected failures per stop
  at 100 req/s, 0.01 at 20 req/s. AC-25's "zero after acceptance" holds at
  the smoke load rate but is not a guarantee. A reset at `release_prep` is
  this race and is recorded with its rate.
- Not added: `cap_drop: [ALL]` and `no-new-privileges`. They are cheap but
  beyond NFR-S5, and can be added if the release review asks.
- Not run here: the compose file and the health-check command on a Docker
  engine. AC-21 to AC-23 and AC-28 prove them at `release_prep`.
- Evidence:
  `missions/01-greenfield-core/slices/03-operate/design-probe/output.txt`
  (D1, D2, P1–P10).
