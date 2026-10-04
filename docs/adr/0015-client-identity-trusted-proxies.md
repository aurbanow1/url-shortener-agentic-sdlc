# ADR-0015 — Client identity: the peer address, or the right-most untrusted `X-Forwarded-For` entry behind a listed proxy

- Status: accepted at the `03-operate` plan-lock (2026-10-03T09:41Z; status line set 09:48Z); `01-analytics-v2` amendment accepted at its plan-lock and merged in `c9b66dd`; `06-client-identity` amendment accepted at its plan-lock and merged in `b8d7fc16` (see *Amendment* sections)
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

## Amendment — `01-analytics-v2` (2026-10-03, proposed; accepted at that slice's plan-lock)

The click's hashed client becomes this ADR's client (wave-review finding W2-02/W2D-03, mission 03
SPEC rule 6, A-7), so one rule and one setting govern both the rate limit and unique visitors:

- **How.**
  - `RateLimitFilter` becomes `public` and gains `public static final String CLIENT_ATTRIBUTE`.
    After computing `clientOf(...)` for a limited request, it sets that request attribute and then
    charges the budget. Its decisions are unchanged.
  - `click.ClickRecorder.record` hashes the attribute when it is a `String` and
    `request.getRemoteAddr()` otherwise. That fallback covers an exempt path or a unit test; a
    redirect is never exempt.
  - Granted by the lead (slice.yaml `c78500e`) on two conditions: the existing `RateLimitFilterTest`
    cases pass unchanged, and no other `web/` file changes.
- **Why not the request wrapper this ADR sketched.** A wrapper overriding `getRemoteAddr()` would
  change the address for every reader of the request, not only the click recorder:
  - `01-audit-read`'s loopback check (ADR-0019) would then see a forwarded client. It stays safe
    only because a forwarding header refuses first, a coupling ADR-0019 had to spell out;
  - Boot's observation and any future reader would be affected too.

  The attribute is additive and read by exactly one consumer, so `getRemoteAddr()` stays the
  connection's address everywhere.
- **Privacy.** The attribute lives only in the request. Forwarded values are never stored or logged:
  only the hash of the chosen client is stored, as before (NFR-P1).
- Hashes stored before the change are not rewritten (SPEC rule 6). On the shipped deployment, which
  lists no trusted proxy, the attribute equals the peer, so nothing changes there.

## Amendment — `06-client-identity` (2026-10-04, proposed; behaviour unchanged)

Human decision D21: one code path answers "who is this client". **`web.ClientIdentity` owns this
ADR's rule and its hand-off:**
- `clientOf(remote, forwardedFor, trusted)`, moved verbatim from `RateLimitFilter`;
- `resolve(request, trusted)`, which records the result in `ClientIdentity.CLIENT_ATTRIBUTE` and
  returns it. The limiter calls it after the exempt check and before it charges, as before;
- `of(request)`, the recorded client, else the peer. The click recorder calls it.

The limiter keeps the setting (`urlshort.rate-limit.trusted-proxies`, `RateLimitProperties`) and
passes it in. `RateLimitFilter` returns to package-private: it was public only so `click/` could read
the constant, which now lives in `ClientIdentity`.

**A static class, not a bean.** The rule holds no state. The beans the audit half reads
(`ServerProperties`, `TomcatServerProperties`, ADR-0019) do not exist in a non-web context, where
`UrlshortApplicationTests.mainBootsWithoutAWebServer` still starts the limiter and the click
recorder.

**The audit read's question is not this one.** ADR-0019's guard has its own methods in the same
class and never uses this rule or the resolved client. Characterization tests pin every edge on the
baseline and after the move (`06-client-identity` design §5).
