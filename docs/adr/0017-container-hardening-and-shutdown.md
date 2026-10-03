# ADR-0017 — Container: non-root, read-only root, loopback publish, readiness health check; 10 s graceful shutdown inside a 20 s stop grace

- Status: accepted at the `03-operate` plan-lock (2026-10-03T09:41Z; status line set 09:48Z)
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
  a 64 MiB tmpfs at `/tmp` (Compose long volume syntax) for JVM perf data, Tomcat's work
  directory and H2 temp files (A-14); the named volume at `/app/data` as the
  only persistent writable mount; a health check sending
  `GET /actuator/health/readiness` over HTTP/1.0 through bash's `/dev/tcp`
  (the JRE image has no `curl`) and requiring `"status":"UP"`; and
  `stop_grace_period: 20s`.
- `spring.lifecycle.timeout-per-shutdown-phase=10s` (was 20 s, A-15).
  Boot's Tomcat graceful shutdown pauses the connector and closes the
  listening socket, then waits for active requests. The context close that
  follows (`02-analytics`' click-writer drain, given a finite deadline by
  `02`'s design revision for review finding DR-01, and the pool) fits in the
  remaining 10 s of the stop grace.

## Consequences

- Measured on Tomcat: a request whose body is still arriving at the stop
  completes `201`, and a new connection 0.5 s after the stop is refused. At
  100 req/s, none of 10 stops reset an accepted connection.
- **The shutdown contract (decided 09:25Z, `qitem-20261003092210-1156d6e8`;
  SPEC rule 13, AC-25, A-16 revised at `f24f373`).** Requests the application has
  dispatched complete within the 10 s phase with zero failures. The held
  `R0` check stays, and new connections are refused. Connections lost from
  the kernel backlog at listener close are counted and reported, not
  judged. AC-28 is unchanged. The reason: the kernel completes the
  handshake for a connection waiting in the listen backlog before the
  application accepts it, and closing the listening socket, which refusing
  new connections requires, resets every such connection. No
  application-level design prevents that; Tomcat only shortens the window
  (it stops accepting a moment before it closes). Measured: 2 to 6 per stop
  under a closed loop of about 3 500 to 5 000 new connections per second,
  0 in 10 stops at 100 req/s, every dispatched request completed.
- **Each cut-off is classified from evidence** (SPEC `f24f373`, AC-25). Every
  request handed to the application gets an id and one `request completed`
  event from `RequestIdFilter`, the first filter. The smoke `--drain` mode
  keeps the jar's log and the `X-Request-Id` of every complete client
  response, and reconciles the two:
  - an id logged with no complete response is a failure;
  - a client cut-off when no such id exists is a boundary loss, counted and
    reported with the load rate.

  The design probe showed it by effect with counters at the same two
  points. In the closed-loop stops, 11 143 and 15 212 requests were
  dispatched, returned and received complete, so the 4 and 6 client
  cut-offs were connections the server never dispatched.
- Not added: `cap_drop: [ALL]` and `no-new-privileges`. They are cheap but
  beyond NFR-S5, and can be added if the release review asks.
- Not run here: the compose file and the health-check command on a Docker
  engine. AC-21 to AC-23 and AC-28 prove them at `release_prep`.
- Evidence:
  `missions/01-greenfield-core/slices/03-operate/design-probe/output.txt`
  (D1, D2, P1–P10).
