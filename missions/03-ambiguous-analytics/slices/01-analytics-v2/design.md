# Design — 01-analytics-v2

- Slice: `01-analytics-v2` (mission `03-ambiguous-analytics`). Tier high, decided by the human at
  the ambiguity park; plan-lock handler is the orchestration lead (D11). Workflow `urlshort-slice`.
- SPEC: `b8c327b` (requirements PASS, no findings). The human's decision is transition 876,
  "accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A".
- Impact analysis, committed before this design: [`impact-analysis.md`](impact-analysis.md) (`3ebfb09`).
- Decision records: amendments to
  [ADR-0013](../../../../docs/adr/0013-click-events-and-request-time-statistics.md) (the figures),
  [ADR-0015](../../../../docs/adr/0015-client-identity-trusted-proxies.md) (the click's client) and
  [ADR-0016](../../../../docs/adr/0016-metrics-and-health-exposure.md) (the counters).
- Probe: [`design-probe/`](design-probe/) (§12). Author: `design-agent@urlshort-factory`, 2026-10-03.

**In one paragraph.**
- **The figures.** `GET /api/links/{code}/stats` keeps v1's body. Each `clicksPerDay` element
  gains `uniqueVisitors`, the distinct client hashes of that UTC day, and `botClicks`, that day's
  clicks of class `bot`. Both come from one `UNION ALL` statement beside v1's (day, referrer)
  groups.
- **The client.** The click's hashed client becomes the rate limiter's client:
  `RateLimitFilter` leaves its `clientOf` result on the request as an attribute, and `ClickRecorder`
  hashes it (granted, slice.yaml `c78500e`).
- **The counters.** `urlshort.clicks.recorded` and `urlshort.clicks.lost{reason}` count what the
  writer stores and what `click lost` reports.
- **What it does not need.** No migration, no new endpoint, no new log event. The hash is never
  read outside one `COUNT(DISTINCT …) GROUP BY clicked_on`.

**Builds after** both mission-02 w1 merges (the lead's order). It rebases onto them for `click/`,
`docs/api/openapi.json`, `server.forward-headers-strategy=none` and the `reduction failed` reason.
It takes no Flyway number; `02-click-retention` takes V3.

## 1. Components touched

| Component | Change | Specification |
|---|---|---|
| `web.RateLimitFilter` (**grant** `c78500e`: additive, decisions unchanged, existing tests unchanged, no other `web/` file) | becomes `public`; one constant; one statement | `/** Request attribute holding the client this request was charged to (ADR-0015); read by the click recorder. */ public static final String CLIENT_ATTRIBUTE = RateLimitFilter.class.getName() + ".client";` In `doFilterInternal` after the exempt check: `String client = clientOf(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), trustedProxies); request.setAttribute(CLIENT_ATTRIBUTE, client); long retryAfter = limiter.tryTake(budget, client);`. `clientOf` stays package-private; the grant allowed it public, and nothing needs it. Constructor stays package-private |
| `click.ClickRecorder` | the address; the counters | **Address:** in `record`, `String address = request.getAttribute(RateLimitFilter.CLIENT_ATTRIBUTE) instanceof String client ? client : request.getRemoteAddr();` then `salt.stamp(address)` as before. **Counters:** the `@Autowired` constructor takes a `MeterRegistry`, and so does the package-private test constructor. It registers `recorded = Counter.builder("urlshort.clicks.recorded").description("Clicks written to the click store").register(registry)`. For each reason in `REASONS` (`rejected`, `reduction failed`, `write failed`, `shutdown deadline`, `shutdown deadline, outcome unknown`), it registers `Counter.builder("urlshort.clicks.lost").description("Clicks reported by a click lost event").tag("reason", r).register(registry)` into an `EnumMap`-like `Map<String, Counter>`. `lost(reason, error)` becomes an instance method that logs as before and then increments `lostCounters.get(reason)`. `ClickWrite.run` increments `recorded` after `compareAndSet(RUNNING, DONE)` succeeds post-insert. The reason strings become constants used by both the log and the map |
| `click.ClickStore` | the statistics statement | `Stats stats(long linkId)` replaces `countByDayAndReferrer`. It runs the `UNION ALL` of §3 and reads each row into `referrers` (`clicks` not null → `DayReferrerCount(day, referrer, clicks)`, as v1) or `days` (`unique_visitors` not null → `DayFigures(day, uniqueVisitors, botClicks)` into a `Map<LocalDate, DayFigures>`). `record Stats(List<DayReferrerCount> referrers, Map<LocalDate, DayFigures> days)`. The Javadoc says the hash is read only here, only to count within a day |
| `click.LinkStats` | the per-day element | `record DayClicks(LocalDate date, long clicks, long uniqueVisitors, long botClicks)`: component order is JSON order, rule 1. `of(String code, ClickStore.Stats stats)` folds the referrer rows exactly as v1 and builds each day's element from v1's per-day sum plus `stats.days().get(day)`. Every day with clicks has a day row, because both groupings read the same rows in one statement |
| `click.StatsController` | OpenAPI | the `200` example gains the two fields per element: `{"code":"aB3dE5fG","totalClicks":6,"clicksPerDay":[{"date":"2026-10-01","clicks":6,"uniqueVisitors":3,"botClicks":1}],"topReferrers":[{"referrer":"https://news.example.com","clicks":4}]}`. `DayClicks` components carry `@Schema` descriptions: unique visitors are "distinct visitors that UTC day; never combined across days", and bot clicks "clicks whose user agent was classified bot" |
| `click.package-info` | text | the hash's single permitted use (Q2 B) |
| `docs/api/openapi.json` | regenerated | after rebasing onto `01-audit-read`'s merge (SPEC A-9) |

No migration, no `application.properties` change, no new log event.

## 2. API contract

`GET /api/links/{code}/stats` → `200 application/json`:

```json
{"code": "aB3dE5fG", "totalClicks": 6,
 "clicksPerDay": [{"date": "2026-10-01", "clicks": 6, "uniqueVisitors": 3, "botClicks": 1}],
 "topReferrers": [{"referrer": "https://news.example.com", "clicks": 4}]}
```

- **Fields.** The four top-level fields, `topReferrers` and the error paths are v1's (rule 1, A-1).
  `uniqueVisitors` ≤ `clicks` and `botClicks` ≤ `clicks` in every element.
- **No combined figure.** No top-level unique figure exists, and none combines uniques across
  days (rule 3, A-6).
- **Empty link.** A link never opened still answers v1's
  `{"code":…, "totalClicks":0, "clicksPerDay":[], "topReferrers":[]}`.

**Metrics.** `GET /actuator/metrics/urlshort.clicks.recorded`, and
`/actuator/metrics/urlshort.clicks.lost?tag=reason:<token>`. On `/actuator/prometheus` they appear as
`urlshort_clicks_recorded_total` and `urlshort_clicks_lost_total{reason="…"}` (S4).

## 3. Data model and queries

No schema change. The statistics statement (ADR-0013 amendment), with `:linkId` bound twice:

```sql
SELECT clicked_on, referrer, COUNT(*) AS clicks, CAST(NULL AS BIGINT) AS unique_visitors,
       CAST(NULL AS BIGINT) AS bot_clicks
  FROM click WHERE link_id = :linkId GROUP BY clicked_on, referrer
UNION ALL
SELECT clicked_on, NULL, NULL, COUNT(DISTINCT client_hash),
       SUM(CASE WHEN user_agent_class = 'bot' THEN 1 ELSE 0 END)
  FROM click WHERE link_id = :linkId GROUP BY clicked_on
```

| Query | Served by | Measured |
|---|---|---|
| the statement above | each branch a range of `ix_click_link_day` (or `fk_click_link`) for the link | S1: the AC-2 to AC-5 shapes exact; S3 and S5: 146 to 202 ms cold for one link of 225 000 clicks over 90 days, with 10 or 300 referrers a day |
| `INSERT INTO click …` | unchanged | — |

**One statement, one snapshot.** `totalClicks`, `clicksPerDay` and `topReferrers` agree with each
other as in v1. The new per-day figures come from the same snapshot.

**Why not a join.** S2: H2 pushes the join condition into the per-day subquery and evaluates it per
(day, referrer) row. It measured close to the union (§12). The union keeps two plain grouped scans.

**Q2 B by construction.** `client_hash` appears in no other statement and no response. It is only
compared within `GROUP BY clicked_on`, and `clicked_on` is the day of the salt that produced it
(`DailySalt.stamp` picks instant and key together, ADR-0012).

## 4. Sequences

Source: [`docs/diagrams/analytics-v2-sequence.mmd`](../../../../docs/diagrams/analytics-v2-sequence.mmd).

```mermaid
sequenceDiagram
    autonumber
    participant V as Visitor (maybe behind a listed proxy)
    participant RL as RateLimitFilter (web)
    participant RC as RedirectController (link)
    participant CR as ClickRecorder (click)
    participant DS as DailySalt (click)
    participant W as click-writer thread
    participant ST as ClickStore → H2 click
    participant M as MeterRegistry
    participant A as Analyst

    V->>RL: GET /{code} (peer P, maybe X-Forwarded-For)
    RL->>RL: client = clientOf(P, X-Forwarded-For, trusted proxies) (ADR-0015)
    RL->>RL: request.setAttribute(CLIENT_ATTRIBUTE, client); charge client's bucket
    RL->>RC: chain
    RC-->>V: 302
    RC->>CR: record(linkId, request)
    CR->>CR: address = CLIENT_ATTRIBUTE if present, else getRemoteAddr()
    CR->>DS: stamp(address) → instant + HMAC under that instant's day salt
    CR->>W: queue Click (no address, no forwarded value)
    W->>ST: INSERT click
    alt written
        W->>M: urlshort.clicks.recorded +1
    else lost (any reason)
        W->>M: urlshort.clicks.lost{reason} +1 (beside the click lost WARN)
    end

    A->>ST: GET /api/links/{code}/stats → one UNION ALL statement
    ST-->>A: (day, referrer, clicks) rows + one (day, uniqueVisitors, botClicks) row per day
    Note over A: fold: clicksPerDay = [{date, clicks, uniqueVisitors, botClicks}], totalClicks, topReferrers
```

## 5. Logging, metrics and audit

No new log event, and `click lost` keeps v1's shape. **Every `click lost` WARN has exactly one
`urlshort.clicks.lost` increment with the same `reason`:**
- on the request thread for `rejected` and `reduction failed`;
- on the writer for `write failed`;
- in `close()` for the two shutdown reasons.

`urlshort.clicks.recorded` counts inserts that returned.

Never logged or tagged: the address, the forwarded value, the hash, the user agent, the referrer, the
link code or id (rule 11, AC-11, AC-12). No audit row: a read is not a mutation.

## 6. Threat model (STRIDE-lite)

| Threat | Mitigation | Residual |
|---|---|---|
| **Information disclosure / privacy (Q2 B):** the hash used to follow a visitor | read only in `COUNT(DISTINCT client_hash) … GROUP BY clicked_on`; `clicked_on` is the salt's day; no field, metric or log carries it; the salt stays in memory and is dropped at midnight (ADR-0012, unchanged) | a restart in a day draws a new salt: an upper bound, never an undercount (rule 3, A-8) |
| **Spoofing:** a client forges `X-Forwarded-For` to split itself into many visitors or to impersonate | the forwarded entry is believed only from a listed proxy, right-most untrusted (ADR-0015); without a listed proxy every header is ignored (AC-8) | a listed proxy that passes client entries through unappended (ADR-0015, the operator's responsibility) |
| Information disclosure: forwarded values stored | only the hash of the chosen client is stored; the attribute lives in the request (AC-7, AC-8 inspect every column) | — |
| **Denial of service:** an expensive statistics read | one statement, two grouped range scans per link; 146 to 202 ms for 225 000 clicks; the `/api` budget of 60 per minute | no latency target (SPEC); referrer spam bounded by the rate limit and the 90-day purge |
| Information disclosure: metrics | one `reason` tag from a static vocabulary (AC-11) | — |
| The audit read's loopback rule | `getRemoteAddr()` is not rewritten (ADR-0015 amendment, ADR-0019) | — |

## 7. Test strategy

Functional journeys use the v1 conventions: peers set per request (`.with(r -> { r.setRemoteAddr(…); return r; })`),
`ClickRecorder.settle()`, and the dedicated-peer rule for shifted-clock requests.

| AC / rule | Test (class: mechanism) | Asserts |
|---|---|---|
| AC-1, rule 1 | `StatsV2JourneyTest`: one browser redirect, settle; and a link never opened | the exact body, element `{"date": D, "clicks": 1, "uniqueVisitors": 1, "botClicks": 0}`; the empty body is v1's |
| AC-2, rule 3 | same: peers `203.0.113.1` ×3, `.2` ×2, `.3` ×1 | `{6, 3, 0}` |
| AC-3, rule 3 | same: the clock at `<D>T23:59:59Z` and `<D+1>T00:00:01Z`, peer `203.0.113.1` (dedicated to this test) | each day `uniqueVisitors` 1; the body's field set is exactly AC-1's |
| AC-4, rule 4 | same: v1's AC-4 user agents from six peers | `{6, 6, 3}`, `totalClicks` 6 |
| AC-5 | same: peer `203.0.113.9`, a browser click and a bot click | `{2, 1, 1}` |
| AC-6, A-1 | `StatsJourneyTest` (v1's AC-8 to AC-11), shape updated only (AC-14) | v1 values exact |
| AC-7, rule 6 | `TrustedProxyClickJourneyTest` (`urlshort.rate-limit.trusted-proxies=10.9.9.9`, budgets raised): the four redirects of AC-7's table from peer `10.9.9.9` | `{4 clicks, 3 uniques}`; `SELECT *` of the link's click rows contains none of `203.0.113.7`, `203.0.113.8`, `198.51.100.1`, `10.9.9.9` |
| AC-8, rule 6 | `StatsV2JourneyTest`: peer `203.0.113.77` ×3 with three different `X-Forwarded-For` | `{3, 1}`; no forwarded value in any column |
| AC-9, rule 5 | same: v1's AC-17 canaries plus AC-7's clients | only the AC-1 fields; no address, hash, class, forwarded value, canary, referrer path |
| AC-10, rule 10 | `ClickMetricsJourneyTest` (the `ClickStore` spy context of v1's AC-15): read both meters through `/actuator/metrics`, `n` redirects plus settle, make the store fail, `k` redirects | recorded +`n`; lost `{reason="write failed"}` +`k` |
| AC-11 | same: `/actuator/prometheus` | both families; the only tag is `reason`, with values from the vocabulary |
| AC-12, rule 11 | v1's `ClickRecordingJourneyTest` AC-18/AC-19 unchanged, plus AC-7's redirects under `OutputCaptureExtension` | no forwarded value or address in the log |
| AC-13 | `StatsJourneyTest`'s document test plus `OpenApiDocumentTest` | the per-day schema has exactly the four properties, with the example; committed equals live |
| AC-14 | the shipped suite: the two named exact-shape assertions updated (impact analysis) | everything else unchanged |
| AC-15, NFR-L1 | `ClickResilienceJourneyTest` unchanged, and `ClickResilienceTrustedProxyJourneyTest extends ClickResilienceJourneyTest` with `@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "urlshort.rate-limit.trusted-proxies=10.9.9.9")` | v1's AC-14 to AC-16 pass as written in both contexts |
| units | `ClickRecorderTest` (constructor with `SimpleMeterRegistry`; attribute used when present, peer otherwise; each reason increments its own counter; a write increments `recorded`); `LinkStatsTest` (the fold with day rows); `RateLimitFilterTest`, existing cases unchanged plus one: the attribute equals `clientOf` for a limited path and is absent for an exempt one | — |

**NFR-L1** stays a `GAPS.md` row until `release_prep`'s `scripts/smoke.sh --bench`. The added
redirect work is one attribute read.

## 8. Reachability check

| Mechanism | Reached by |
|---|---|
| `CLIENT_ATTRIBUTE` set | every non-exempt request, before charging |
| attribute read | `ClickRecorder.record` for every non-`HEAD` redirect; the fallback for a missing attribute is reached by unit tests |
| `recorded`, `lost{reason}` | the writer, the request thread, `close()` |
| the union statement | `StatsController` only |

## 9. Territory

| Path | Use |
|---|---|
| `src/main/java/dev/urlshort/click/`, `src/test/java/dev/urlshort/click/`, `src/functionalTest/java/dev/urlshort/click/` | the changes and tests of §1 and §7 |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java`, `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | **granted** (`c78500e`) under its conditions |
| `docs/api/openapi.json` | regenerated after `01-audit-read`'s merge |
| `src/main/resources/db/migration/` | **not used** |

Mine, done in this design step: ADR-0013, ADR-0015 and ADR-0016 amendments, `docs/DESIGN.md` and
`docs/diagrams/analytics-v2-sequence.mmd`.

## 10. Decisions recorded as ADRs

- **ADR-0013 amendment:** the union statement, the fold, the hash compared only within its day, and
  why not a join.
- **ADR-0015 amendment:** the click's client is the limiter's client through a request attribute,
  and why not ADR-0015's request-wrapper sketch: a wrapper rewrites `getRemoteAddr()` for every
  reader, including `01-audit-read`'s loopback check (ADR-0019) and Boot's observation. The
  attribute is additive and has one reader.
- **ADR-0016 amendment:** the two counters and their vocabulary.

## 11. Trade-offs

| Chosen | Over | Because |
|---|---|---|
| request attribute set by the rate limiter | ADR-0015's wrapper; re-parsing `X-Forwarded-For` in `click/` | one rule and one setting; `getRemoteAddr()` unchanged for everyone else; no second parser of a security-relevant header |
| `UNION ALL` in one statement | two statements; a join | one snapshot as v1; two plain scans (S2, S3, S5) |
| counters pre-registered for every reason | registered on first loss | families visible at zero for scraping and alerting |
| no index | `(link_id, clicked_on, client_hash)` | no latency target; 146 to 202 ms measured on a 225 000-click link |

## 12. Design probe (what was verified by effect)

`StatsProbe.java` (`output.txt`), on the functional classpath; no product file touched.

| Row | Setup | Result | Proves |
|---|---|---|---|
| S1 | V1 and V2 schema; AC-2, AC-5, AC-4 and AC-3 shapes on one link, referrers spread | join form: day D `6/3/0` over three referrer rows; D+1 `2/1/1`; D+2 `6/6/3`; D+3 and D+4 `1` unique each. Union form (S1u): the same figures, the day rows separate | the figures and the fold's input |
| S2 | `EXPLAIN` of the join form | the per-day subquery is evaluated per row (`CLICKED_ON IS NOT DISTINCT FROM ?3`) | why the union |
| S3, S5 | one link, 225 000 clicks, 90 days, about 1 000 clients a day; 10 referrers (S3) and 300 a day (S5); cold | join 207 ms, union 146 ms (S3); join 209 ms, union 202 ms (S5) | cost |
| S4 | the shipped application with both counters, every reason pre-registered | `urlshort_clicks_recorded_total 3.0`; `urlshort_clicks_lost_total{reason="write failed"} 2.0` and the others at `0.0`, the comma-containing reason quoted | metric names and tags (AC-11) |

Not run by the probe: the attribute hand-off. It is two lines whose effect AC-7 and AC-8 prove in
the suite. A first S3 attempt with 450 000-click links ran out of the 1 GB heap in the in-memory
database. The recorded run halves the links.

## 13. Build plan

1. Rebase onto `main` after both mission-02 merges.
2. `test(01-analytics-v2): v2 statistics, identity and click counters`: the journeys and units of
   §7, plus the two named shape updates. Red.
3. `feat(01-analytics-v2): unique visitors and bot clicks per day`: `ClickStore`, `LinkStats`,
   `StatsController`.
4. `feat(01-analytics-v2): click identity follows the trusted-proxy rule`: `RateLimitFilter` (grant)
   and `ClickRecorder`.
5. `feat(01-analytics-v2): click counters`: `ClickRecorder`.
6. `docs(01-analytics-v2): regenerate the API document`.
7. Run `scripts/gw check`, commit the coverage reports, and hand off naming the SHA.

## Status

- 2026-10-03 — design written on SPEC `b8c327b`; impact analysis first (`3ebfb09`); handed to
  `design_review`.

## Self-check

- Every AC (1 to 15) and rule (1 to 11) has a mechanism and a named test (§7). The privacy rules
  not observable over HTTP (never across days, salt never persisted) are stated by construction (§3)
  for the design and security reviews (SPEC *Non-functional*).
- Every mechanism claim was run (§12) except the attribute hand-off, which AC-7 and AC-8 prove.
- The grant is used within its conditions: `RateLimitFilter` decisions unchanged, existing tests
  unchanged, no other `web/` file. Why not the wrapper is stated (§10, ADR-0015 amendment), as the
  lead asked.
- Scope: only Q1 B, Q2 B, Q3 B, with no export, no time-zone parameter and no retention work
  (Q4 A, Q5 A, Q6 A).
- Custody: builds after both mission-02 merges; no Flyway number; regenerates the API document last.
- **Not verified:**
  - the shipped suite against a candidate (AC-14, QA);
  - NFR-L1 numbers (release bench);
  - springdoc's rendering of the new `@Schema` descriptions (AC-13 on the candidate).

## Plan review (author's lenses; the skill was not invoked separately)

- **Engineering.**
  - The probe moved the statement from a join to a union, and measured the cost on a hot link.
  - The identity alignment reuses the limiter's own result instead of a second parser of a
    security-relevant header.
- **Strategy.** Exactly the human's letters, with no unchosen option smuggled in. The figures stay
  per day, as Q1 B and Q2 B require.
- **Analyst experience.**
  - Old reports keep working: the fields are additive.
  - Two new numbers sit where the Analyst already reads.
  - The Operator gets the analytics' own loss rate on the scrape they already have.
