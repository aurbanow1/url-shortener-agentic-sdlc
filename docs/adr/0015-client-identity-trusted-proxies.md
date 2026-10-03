# ADR-0015 — Client identity: the peer address, or the right-most untrusted `X-Forwarded-For` entry behind a listed proxy

- Status: accepted at the `03-operate` plan-lock (2026-10-03T09:41Z; status line set 09:48Z)
- Date: 2026-10-03
- Slice: `03-operate`

## Context

The rate limit is per client (NFR-R2). The slice SPEC's rule 5 defines the
client as the request's remote address, unless that address is an
operator-listed trusted proxy. In that case it is the right-most
`X-Forwarded-For` entry that is not itself a trusted proxy. No other header
counts. The default is no trusted proxy, so a forged header changes nothing
(AC-7). The proof must run in-suite, where MockMvc sets the remote address
per request (AC-8).

## Decision

- `RateLimitFilter.clientOf(remote, xff, trusted)`: return `remote` unless it
  is listed; otherwise walk the comma-separated, trimmed `X-Forwarded-For`
  entries from the right and return the first non-empty one that is not
  listed. Return `remote` when there is none. `Forwarded` and `X-Real-IP` are
  never read.
- Entries match by exact text, as configured
  (`urlshort.rate-limit.trusted-proxies`, comma-separated). Tomcat reports
  IPv4 dotted and IPv6 in its full form, and the setting must use the same
  text. CIDR ranges and host names are out of scope (A-6).
- Application code, not Tomcat's `RemoteIpValve`. MockMvc never runs a
  valve, so AC-8 could not be proven, and the valve's default internal-proxy
  list trusts `127/8` and `10/8`.
- `getRemoteAddr()` is **not** rewritten for the rest of the request.

## Consequences

- The client identity lives in memory as a map key only. It is never
  logged, tagged, stored or echoed (rule 6).
- `02-analytics` hashes `getRemoteAddr()` for clicks, so behind a configured
  proxy every click hashes the proxy's address. Nothing reads the hash in
  mission 01. Aligning it is the lead's backlog item A-9, and the mechanism
  is a request wrapper in `RateLimitFilter` overriding `getRemoteAddr()` with
  `clientOf(...)`, about four lines.
- A trusted proxy that passes client-supplied `X-Forwarded-For` through
  without appending would make the client's own entry right-most. Listing
  only proxies that append is the operator's responsibility.
