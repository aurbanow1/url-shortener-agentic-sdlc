# ADR-0003 — Request id: server-issued `X-Request-Id`, MDC `requestId`, inbound ignored

- Status: accepted
- Date: 2026-10-02
- Slice: `01-ping`

## Context

A support engineer must get from a response in hand to its log line. The
mission plan-lock set a binding constraint: keep the request-id mechanism
minimal — the smallest thing that puts `requestId` on the MDC and on the
response header. Honouring a client-supplied id would add a trust-boundary
validation (length, charset, log injection) that no acceptance criterion
needs yet.

## Decision

- One servlet filter, `dev.urlshort.web.RequestIdFilter extends
  OncePerRequestFilter`, registered as a bean at
  `Ordered.HIGHEST_PRECEDENCE`.
- Per request it generates `UUID.randomUUID().toString()`, sets the response
  header **`X-Request-Id`** *before* invoking the chain, puts the value into the
  SLF4J MDC under the key **`requestId`**, and removes that key in `finally`.
- Inbound `X-Request-Id` (or any correlation header) is **not read**: never
  echoed, never logged, never used to derive the issued id.
- The header name and the MDC key are contracts for every later slice. The
  generation scheme is not.

## Consequences

- Every response, including framework-produced error responses, carries the
  header (proven on the wrong-method path in `01-ping`).
- Every log event emitted while a request is being handled carries
  `requestId` once structured logging renders the MDC (ADR-0004).
- No distributed tracing or trace-context propagation; a later slice may
  choose to honour validated inbound ids without breaking this contract.
- `OncePerRequestFilter` skips ASYNC and ERROR dispatches by default; a slice
  that introduces either must revisit this.
