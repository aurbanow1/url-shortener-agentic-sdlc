# ADR-0002 — Errors are RFC 9457 problem details, produced by the platform handler

- Status: accepted
- Date: 2026-10-02
- Slice: `01-ping`

## Context

`AGENTS.md` requires every error response to be an RFC 9457 `ProblemDetail`
with `Content-Type: application/problem+json` and never a stack trace. Spring
Boot already ships a handler for framework exceptions (wrong method, unknown
path, binding and validation failures, unreadable bodies) that is switched on
by one property. The first slice has no domain exceptions.

## Decision

- Keep `spring.mvc.problemdetails.enabled=true` (shipped). Boot's
  `ProblemDetailsExceptionHandler` (package
  `org.springframework.boot.webmvc.autoconfigure`) handles framework
  exceptions; the project adds **no** advice class until a slice introduces a
  domain exception.
- When a slice does add a project `@RestControllerAdvice`, it either extends
  `ResponseEntityExceptionHandler` (Boot's handler then backs off, because it
  is conditional on that bean being absent) or handles only domain exceptions
  and coexists with Boot's. Either way the body is a `ProblemDetail`.
- Bodies never contain a stack trace or an exception class name. Domain
  problem `type` URIs are decided by the first slice that needs one.
- The request-id response header is set by the filter before the chain runs
  (ADR-0003), so it is present on error responses without the handler knowing
  about it.

## Consequences

- `01-ping` proves the wrong-method path (`405`) end to end with zero error
  handling code.
- Later slices owe a test per error AC that asserts status, `Content-Type`
  and absence of stack-trace markers; wording of `detail` is framework-owned
  and not asserted.
