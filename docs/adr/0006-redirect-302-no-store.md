# ADR-0006 — Redirects are `302 Found` with `Cache-Control: no-store` and a verbatim `Location`

- Status: accepted at the `01-create-redirect` plan-lock (2026-10-03T06:38Z)
- Date: 2026-10-03
- Slice: `01-create-redirect`

## Context

FR-2 asks for an observable redirect so that `02-analytics` can count every
click; a `301` is cached by browsers and intermediaries and later visits never
reach the service. `docs/REQUIREMENTS.md` and the mission brief fix `302`; the
slice SPEC (rule 7, A-5) adds `Cache-Control: no-store` and the byte-for-byte
`Location`.

## Decision

- `GET /{code}` on an active link answers **`302 Found`**, never `301`, `307`
  or `308`.
- The response carries **`Cache-Control: no-store`** and no other caching
  header, so no cache serves the redirect without the service seeing the
  request.
- **`Location` is the stored `url` byte for byte.** The header is set as a
  String (`ResponseEntity.header(LOCATION, url)`); `HttpHeaders.setLocation(URI)`
  is not used because it re-encodes through `toASCIIString()`. Validation at
  creation guarantees the value is visible ASCII with an `http`/`https` scheme
  and a host, so the header can never carry CR, LF or a non-HTTP scheme.
- The short link's query string and fragment are ignored and not forwarded.
- A retired link answers `410 Gone` as a problem detail with no `Location`;
  an unknown code `404`.
- `HEAD` and `OPTIONS` keep framework defaults.

## Consequences

- Every click costs one request to the service; that is the point.
- The redirect handler (`dev.urlshort.link.RedirectController`) stays one
  method and is the seam where `02-analytics` inserts its click hook.
- Forwarding query parameters, `307`/`308` for method preservation, or
  per-link caching would each be a deliberate change to this ADR.
