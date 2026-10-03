# ADR-0002 — Errors are RFC 9457 problem details, produced by the platform handler

- Status: accepted; amended 2026-10-03 by `01-create-redirect` (see *Amendment*)
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

## Amendment — `01-create-redirect` (2026-10-03)

The first slice with domain failures, validation and a `500` path takes the
second option foreseen above and fixes the extension shape.

- **One project advice, `dev.urlshort.web.ProblemDetailsAdvice extends
  ResponseEntityExceptionHandler`.** Boot's `ProblemDetailsExceptionHandler`
  backs off (it is `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`),
  so framework errors (`405`, `415`, `404` without a handler, unreadable body)
  keep their platform rendering through the same class. A second, coexisting
  advice was rejected: with two advices, the one holding
  `@ExceptionHandler(Exception.class)` wins by advice order, not by match
  specificity.
- **Domain failures are `org.springframework.web.ErrorResponseException`
  instances** created by the factories in `dev.urlshort.web.Problems`
  (`validation`, `notFound`, `gone`, `idempotencyMismatch`). The parent handler
  renders them; no project exception classes and no handler methods exist for
  them. A domain exception hierarchy returns to the table only if a non-HTTP
  caller or a second mapping of the same failure appears.
- **Validation extension.** `400` on a field or header and `422` on an
  idempotency mismatch add `errors: [{field, rule, message}]`; `field` is the
  JSON member or header name, `rule` a stable token from the SPEC, `message`
  free text that never contains the submitted value. Rendering relies on the
  `ProblemDetailJacksonMixin` that Boot registers on the auto-configured
  `JsonMapper`, so the property appears top-level.
- **`type` stays `about:blank`** (omitted from the JSON) and `title` is the
  reason phrase; the SPECs assert statuses and tokens, not type URIs. No
  problem-type registry is introduced until an API consumer needs one.
- **`instance` is Spring's default**, the request path. It is the resource
  identifier RFC 9457 intends, JSON-escaped, never rendered as HTML; the
  slice's "never echo a submitted value" rule is read as applying to body
  fields and header values, which the design never echoes.
- **Catch-all `500`.** `@ExceptionHandler(Exception.class)` logs
  `request failed` at ERROR with the throwable (the only place a stack trace
  goes) and answers a bare `ProblemDetail` with status `500`. This keeps every
  error inside MVC: an exception escaping to Boot's `/error` dispatch would
  render without `requestId` (the request-id filter skips ERROR dispatches)
  and not as a problem detail.
- **Wrapped limits.** `handleHttpMessageNotReadable` is overridden to walk the
  cause chain and render an `ErrorResponseException` found there (Jackson 3
  wraps an exception raised from the request stream while inside a value); the
  request body limit uses this to answer `413` whether the limit falls inside
  or between tokens.
- Verified by effect on a real Tomcat before implementation:
  `missions/01-greenfield-core/slices/01-create-redirect/design-probe/output.txt`.
