# Impact analysis — 01-analytics-v2

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design-agent@urlshort-factory`,
2026-10-03, for SPEC `b8c327b` (requirements PASS, no findings; the human's decision is transition
876, "accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A").

**Baseline.** `main` at `3799209`, whose product code is byte-identical to `f6dd29e`:
`git diff --stat f6dd29e HEAD -- src build.gradle.kts compose.yaml Dockerfile` is empty.

The shipped jar was observed for mission 02
(`missions/02-brownfield/slices/02-click-retention/design-probe/baseline-f6dd29e.txt`):
- a redirect stored one row with `referrer` origin, `user_agent_class` `browser` and a 64-character
  hash;
- the statistics answered
  `{"code":…,"totalClicks":1,"clicksPerDay":[{"date":"2026-10-03","clicks":1}],"topReferrers":[…]}`.

The new statement and counters were run in [`design-probe/output.txt`](design-probe/output.txt).

**What lands before this slice builds.** The lead's order is that both mission-02 w1 slices merge
first. They change the base this analysis was written against:
- `02-click-retention` adds `ClickPurge`, V3 (audit columns on `click`) and the `click lost` reason
  `reduction failed`;
- `01-audit-read` adds `audit/` read classes, pins `server.forward-headers-strategy=none` and
  regenerates `docs/api/openapi.json`.

This slice's builder rebases onto both merges. The rows below say where they matter.

## Change in one sentence

The statistics' per-day element gains `uniqueVisitors` and `botClicks`. The click's hashed client
becomes the rate limiter's client under the trusted-proxy rule, and two click counters are
exposed. Requirements: FR-8 v2, FR-16, NFR-P1, NFR-O3; FR-13, NFR-L1, O1, O2 and M1 to M3 apply.

## Impacted modules

Found with `grep -rn "clicksPerDay\|DayClicks\|DayReferrerCount\|new ClickRecorder(\|clientOf" src`.

| Class or file | Change | Callers and dependents |
|---|---|---|
| `click.ClickStore` | `countByDayAndReferrer` becomes one `UNION ALL` statement: v1's (day, referrer) groups plus a per-day row with `COUNT(DISTINCT client_hash)` and the bot count. Its row record gains two nullable fields | `StatsController` only |
| `click.LinkStats` | `DayClicks` gains `uniqueVisitors` and `botClicks`; the fold reads the day rows | `StatsController`; unit `LinkStatsTest` |
| `click.StatsController` | the OpenAPI example and schema of the per-day element | Spring MVC |
| `click.ClickRecorder` | hashes the request attribute `RateLimitFilter.CLIENT_ATTRIBUTE` when present, else `getRemoteAddr()`. Takes a `MeterRegistry`: `urlshort.clicks.recorded` after a successful insert, and `urlshort.clicks.lost{reason}` wherever `click lost` is logged | `link.RedirectController:46` (unchanged); unit `ClickRecorderTest` (constructor) |
| `web.RateLimitFilter` (**by grant**, slice.yaml `c78500e`) | becomes `public`; a `public static final String CLIENT_ATTRIBUTE`; `doFilterInternal` sets it to the computed client before charging. Its decisions are unchanged | every non-exempt request; `RateLimitFilterTest` (existing cases unchanged, one new case) |
| `docs/api/openapi.json` | the per-day schema and example | `OpenApiDocumentTest` |

**Not touched:** `link/`, `audit/`, `DailySalt`, `Click`, every other `web/` class, the schema
(no migration), `application.properties`.

## Impacted endpoints

`GET /api/links/{code}/stats` changes only additively:
- each `clicksPerDay` element gains `uniqueVisitors` and `botClicks`;
- the top-level fields, `topReferrers`, error paths, `HEAD` and `OPTIONS` are unchanged (SPEC
  rule 1, A-1).

`/actuator/metrics` and `/actuator/prometheus` gain two meter families:
- `urlshort_clicks_recorded_total`;
- `urlshort_clicks_lost_total{reason}` (probe S4).

The redirect's response does not change.

## Impacted schema and data

- **No migration.** Both figures come from v1's columns (`client_hash`, `user_agent_class`) in one
  statement served by `ix_click_link_day`. One link with 225 000 clicks over 90 days read in
  146 to 202 ms cold (S3, S5). The SPEC sets no latency target. This slice takes no Flyway number;
  `02-click-retention` takes V3.
- **Stored rows.** Not rewritten (SPEC rule 6). Clicks written after the change hash the
  forwarded client behind a trusted proxy. Older ones hashed the peer. On the shipped loopback
  deployment no proxy is configured, so both are per client.
- **Rollback.** `git revert` of the merge commit. Nothing stored needs undoing.

## Impacted data flows

| Flow | Change |
|---|---|
| Redirect → click reduction (request thread) | the address hashed comes from the request attribute the rate limiter already computed (`clientOf`, ADR-0015). That is one attribute read and no new parsing on the Visitor's path. `getRemoteAddr()` itself is unchanged, so `01-audit-read`'s loopback check (ADR-0019) is unaffected |
| Click writer | one counter increment after a successful insert; one tagged increment beside each `click lost` |
| Statistics | one statement instead of one statement; two grouped scans instead of one |
| Rate limiter | unchanged decisions; it also leaves its client on the request |

## Blast radius

| If this is wrong | Worst case | Detection |
|---|---|---|
| The identity attribute is wrong or missing | uniques collapse to the proxy, or a forged `X-Forwarded-For` splits one client into many | AC-7 (trusted proxy with a forged left entry), AC-8 (no proxy, varied headers) |
| A forwarded value leaks | a client address in a row, log or metric | AC-7 and AC-8 inspect stored rows; AC-11 tags; AC-12 logs |
| The hash is used across days | linkability of visitors across days, against Q2 B | design and security review (SPEC *Non-functional*); the statement groups by `clicked_on`, the salt's day |
| v1 figures change meaning | Analysts' reports drift | AC-6 re-runs v1's AC-8 to AC-11 |
| The redirect slows | NFR-L1 | AC-15 re-runs v1's AC-14 to AC-16; the release bench |
| Counters drift from the log | Operators trust a wrong loss rate | AC-10: the counter grows by exactly the reported losses |

## Compatibility (FR-13)

- **Clients.** Additive fields only. A strict client that rejects unknown fields would see the
  two new per-day members. The SPEC accepted that (A-1, Q5 A "fields added").
- **Links, redirects, audit.** Unchanged.
- **Operators.** The trusted-proxy setting now also governs click identity, so one setting governs
  both (SPEC A-7).

## Test impact

Changed by intent, each named for AC-14 and nothing else in them changes:
- `StatsJourneyTest.AC09_clicksPerDayAreGroupedByUtcCalendarDay` (lines 113–114): the exact
  `clicksPerDay` array gains `uniqueVisitors` and `botClicks` per element.
- `ClickRecordingJourneyTest` (line 223): "each day has exactly `date`, `clicks`" becomes the four
  fields of AC-1.
- Unit `LinkStatsTest` (line 30): `DayClicks` and the fold's input rows gain the two fields.
- Unit `ClickRecorderTest` (line 51): the constructor takes a `SimpleMeterRegistry`.

Unchanged and why:
- `StatsJourneyTest` line 83, the empty link's exact body: v1's shape kept (AC-1).
- `StatsJourneyTest` line 277, `totalClicks` equal to the sum of `clicks`: it still holds.
- `OpenApiDocumentTest` asserts paths and responses, not the per-day schema; the committed document
  is regenerated.
- `RateLimitFilterTest` and `TrustedProxyJourneyTest`: decisions unchanged (the grant's condition).
- `ClickResilienceJourneyTest`: AC-15 runs it with the trusted-proxy setting empty and naming a
  proxy, as written.

Added: the AC-1 to AC-15 journeys and unit cases (design §7).

## Observability impact

- Two counters (rule 10). No new log event. `click lost` keeps its shape, and its counter carries
  the same `reason` token.
- The runbook rows in `docs/DESIGN.md` §3 (Metrics) gain the two families.

## Custody (SPEC A-9, *Dependencies*)

- `click/` is held by `02-click-retention` until its merge.
- `docs/api/openapi.json` is held by `01-audit-read` until its merge.
- `web/RateLimitFilter.java` is held by no slice (granted to this one).
- This slice rebases onto both mission-02 merges and regenerates the document. It takes no Flyway
  number.

## Risks and mitigations, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | Privacy regression: the hash used beyond same-day counting (Q2 B) | it is used only in `COUNT(DISTINCT …) … GROUP BY clicked_on`, and the salt's day is the click's day (ADR-0012 `stamp`); no response, metric or log field carries it | design → security review |
| 2 | Forwarded identity forged | `clientOf` takes the right-most entry that is not a trusted proxy, and only behind a listed proxy (ADR-0015) | QA (AC-7, AC-8) |
| 3 | v1 contract drift | additive fields; AC-6 re-runs v1 | QA |
| 4 | Redirect latency | one attribute read; AC-15; release bench | QA → release |
| 5 | Custody clashes with mission 02 | builds after both merges; rebase and regenerate | lead (plan-lock) |

## Self-check

- Baseline product identical to `f6dd29e`, checked with `git diff`. Shipped behaviour cited from the
  observed jar run. The mission-02 changes that land first are named.
- Every class touched and every caller is listed. The `web/` change is by grant, with the lead's
  conditions.
- Endpoint change additive; metrics named, measured in S4.
- No schema change; the statement and its cost were run (S1 to S5).
- Every shipped test with an exact per-day shape assertion is named (two functional, two unit).
- Not verified here: the shipped suite against a candidate (AC-14, QA); the release bench (NFR-L1).
