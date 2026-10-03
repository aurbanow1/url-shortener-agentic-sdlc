# ADR-0014 — Rate limit: one servlet filter, two per-client budgets, GCRA buckets in memory

- Status: accepted at the `03-operate` plan-lock (2026-10-03T09:41Z; status line set 09:48Z)
- Date: 2026-10-03
- Slice: `03-operate`

## Context

FR-10 and NFR-R2 (decided: 60 creates and 600 redirects per minute per
client, token bucket) and the slice SPEC's rules 1 to 4, 6 to 8 and 11
require four things. Every non-operator request is charged before anything
else about it is examined, whatever its outcome, so `400` to `415` count too.
An empty bucket answers a `429` problem detail with an honest `Retry-After`.
Operator paths are never charged. The state is in memory, per instance, and
bounded. No new library is wanted, and none is in the offline build cache.

## Decision

- **Where.** `web.RateLimitFilter`, a `OncePerRequestFilter` at
  `HIGHEST_PRECEDENCE + 2`. It runs after `RequestIdFilter` (so the `429`
  carries `X-Request-Id`) and after Boot's `ServerHttpObservationFilter` (so
  `429`s are timed), and before `RequestBodyLimitFilter` (moved to `+ 3`). A
  `HandlerInterceptor` would miss `405`s and no-handler `404`s, which are
  decided before interceptors run.
- **Which budget.** Classification uses MVC's decoded lookup path
  (`UrlPathHelper.getLookupPathForRequest`). `/actuator`, `/v3/api-docs`,
  `/swagger-ui.html` and `/swagger-ui/` are exempt. `/api` and `/api/…` go to
  the create budget, everything else to the redirect budget. The raw URI is
  not used: `/%61pi/links` reaches the create endpoint and must not be
  charged to the larger budget.
- **The bucket.** GCRA, the single-number form of rule 2's token bucket. Per
  budget, a map from client to theoretical arrival time (TAT, epoch
  nanoseconds from the application `Clock`). Interval `I = 60 s / N`,
  tolerance `T = I × (N − 1)`. A request is admitted when
  `max(TAT, now) − T ≤ now`, and the TAT becomes `max(TAT, now) + I`.
  Otherwise it is refused with `Retry-After = ceil((max(TAT, now) − T −
  now) / 1 s)` and the TAT is unchanged, so a `429` takes nothing. The
  capacity is `N`, refilling at `N/60` per second, exactly as rule 2 states.
- **The `429`.** The filter writes it itself: status `429`, `Retry-After`,
  `Content-Type: application/problem+json`, and the context's `JsonMapper`
  rendering `ProblemDetail.forStatus(429)` with `instance =
  urn:uuid:<request id>`. That is the same shape as every other error
  (ADR-0002). It logs nothing; the request's one event is the filter's
  `request completed` with status `429`.
- **Memory bound, released opportunistically.** A bucket is full again
  exactly when `TAT ≤ now`. Inside `tryTake`, at most once per second of
  application-`Clock` time, both maps drop such entries (a value-conditional
  remove, safe against concurrent updates). After any limited request that
  runs a sweep, the maps hold only clients admitted in the 61 s before it.
  While no limited request arrives, nothing is added or removed, so an idle
  service keeps its last minute of entries until the next request releases
  them. There is no growth while idle and no timer thread (design review
  DR-03 asked for the guarantee to be stated, not for a timer).
- **Settings.** `urlshort.rate-limit.create-per-minute` (60),
  `redirect-per-minute` (600) and `trusted-proxies` (empty), in a validated
  `@ConfigurationProperties` record. The shipped `application.properties`
  restates the defaults, and environment variables override them.
- **Counting.** `urlshort.ratelimit.rejections`, tagged `budget` only, both
  tags registered at startup.

## Consequences

- One `long` per active client and budget; no dependency added. The
  arithmetic replays AC-3 and AC-4 exactly on a fixed clock.
- Limits are per instance and reset on restart (NFR-R4 single node).
- `/actuator/**` stays unlimited by the SPEC's choice (A-7). An
  `/actuator/../<code>` request routes nowhere (`404`).
- The functional suite raises both budgets in its profile overlay, so the
  journeys of other slices are not limited. The limiter's own journeys set
  the shipped numbers inline, and a test reads the shipped file so a wrong
  default cannot hide behind the overlay.
- Verified before implementation:
  `missions/01-greenfield-core/slices/03-operate/design-probe/output.txt`
  (B1–B3 arithmetic, O1 `429` shape and single log line, O1b filter order).
