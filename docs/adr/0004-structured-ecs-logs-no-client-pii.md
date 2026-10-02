# ADR-0004 — Structured ECS JSON logs with no client PII

- Status: accepted
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
- Any later slice that wants client analytics (for example per-click
  statistics) must not log or store a raw IP; it uses a salted hash and gets
  its own ADR.
- Logging is initialised before the `ApplicationContext`, so only
  Environment-level properties affect it. With the shipped file loaded as the
  suite's base, the functional suite logs ECS JSON from its first context and
  no per-test-class override is needed or allowed.
