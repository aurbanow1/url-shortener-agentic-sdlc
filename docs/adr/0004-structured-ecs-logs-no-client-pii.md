# ADR-0004 — Structured ECS JSON logs with no client PII

- Status: accepted; amended 2026-10-03 by `01-create-redirect` (see *Amendment*)
- Date: 2026-10-02
- Slice: `01-ping`

## Context

`AGENTS.md` requires structured JSON logging where every request line carries
`requestId` and no raw IP addresses or user agents appear. Spring Boot's
structured logging renders Logback events as JSON and adds every MDC entry as
a top-level member, so the request id reaches the line with no custom encoder.
The test suites blank the structured format for human-readable output, which
would hide the JSON contract from the tests that are supposed to prove it.

## Decision

- Shipped configuration: `logging.structured.format.console=ecs` (Elastic
  Common Schema). `service.name` follows `spring.application.name`.
- Request correlation is the MDC key `requestId` (ADR-0003), rendered as a
  top-level JSON member.
- Log events **never** carry the client IP / remote address, the `User-Agent`
  value, or any value copied from an inbound request header. Framework
  loggers stay at INFO in the shipped configuration; Tomcat access logging
  stays off.
- The ECS member `process.thread.name` is excluded from every event
  (`logging.structured.json.exclude=process.thread.name` in the shipped
  `application.properties`). Tomcat names its worker threads after the bound
  address (`http-nio-127.0.0.1-8080-exec-1`), so on a loopback-bound instance
  the server's own address would appear on every line and collide literally
  with the rule above and with the `01-ping` acceptance criterion AC-7
  (finding QA-01 at `qa_check`, decided by the orchestration lead on
  2026-10-03). Bind metadata is not client data, but one property line keeps
  the rule literal and adds no code. The `process` member therefore renders
  as `{"pid":<n>,"thread":{}}`.
- The functional suite runs under the shipped structured format so HTTP
  journeys prove the JSON contract by effect; tests assert only the members
  they own (`requestId`, absence of canaries), never the ECS envelope.
- Suite-specific overrides live in profile-specific files
  (`application-<suite>.properties`, for example `application-functional.properties`)
  activated by the suite's Gradle task. A suite-level `application.properties`
  is not used: Boot loads `classpath:/application.properties` as a single
  resource, so a test copy shadows the shipped file instead of overriding it
  (found by the `01-ping` design review, DR-01). The unit suite still carries
  such a file; converting it is a follow-up.

## Consequences

- `01-ping` proves by effect that `requestId` reaches a one-line JSON object.
- Thread names are not available in the logs for correlation; `requestId` is
  the correlation key. A later slice that needs thread identity must find a
  rendering that cannot carry an address, and amend this ADR.
- Any later slice that wants client analytics (for example per-click
  statistics) must not log or store a raw IP; it uses a salted hash and gets
  its own ADR.
- Logging is initialised before the `ApplicationContext`, so only
  Environment-level properties affect it. With the shipped file loaded as the
  suite's base, the functional suite logs ECS JSON from its first context and
  no per-test-class override is needed or allowed.

## Amendment — `01-create-redirect` (2026-10-03)

The first slice that parses client input and talks to a database found three
ways a client value or an uncorrelated line could reach the log stream (design
review DR-01 and DR-04, plus one found while fixing them). The rule above is
unchanged. These are the decisions that keep it true:

- **Never pass a throwable to a logger on a request path.** A throwable
  renders as `error.message` and `error.stack_trace`, and driver messages
  quote bound values: an H2 unique violation names the duplicate
  `Idempotency-Key` (`docs/review/01-create-redirect/proof/design-boundary-probe.txt:51`).
  The one ERROR event, `request failed` (ADR-0002), carries `errorChain` (the
  cause chain's class names) and `errorOrigin` (the first `dev.urlshort.`
  stack frame) as SLF4J key-values. Class names and code locations are the
  only exception data that reaches the log.
- **The per-request event logs `status` only.** The HTTP method is not
  logged: Tomcat accepts any token as a method, so it is client-controlled.
- **Framework categories that echo request data are raised.**
  `logging.level.org.springframework.web.servlet.PageNotFound=error` (shipped),
  because `ResponseEntityExceptionHandler` WARNs `Request method '<token>' is
  not supported` there.
- **Every line written inside a request carries `requestId`, including the
  first request after startup.** `spring.mvc.servlet.load-on-startup=1`
  (shipped) initialises the DispatcherServlet before Tomcat accepts
  connections. Its three INFO lines would otherwise be written inside the
  first request, before the request-id filter runs.

Consequences: an Operator has no stack trace for a `500` under the default
configuration (ADR-0002, *Consequences of the amendment*). Verified by effect:
`missions/01-greenfield-core/slices/01-create-redirect/design-probe/revision-output.txt`.
H2's own trace file (`data/*.trace.db`) is not this service's log stream; its
default level keeps integrity violations out of it (`01-create-redirect`
design §6).
