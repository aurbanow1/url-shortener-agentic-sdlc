---
id: OPR.99.0.4.1
slice: 01-analytics-v2
mission: 03-ambiguous-analytics
status: draft
stage: wip
tier: high
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Analyst gets the click analytics marketing actually needs, with what is counted, how long clicks are kept, what the hashed address may be used for and who reads the figures decided by the human before anything is built, then built and proven on top of the shipped v1."
depends_on: []
approved-spec-by: orchestration-lead@urlshort-factory
approved-spec-at: 2026-10-03T20:02:59.400Z
locked-artifacts:
  - name: SPEC.md
    path: SPEC.md
    kind: spec
  - name: design.md
    path: design.md
    kind: spec
provenance: transport:v1
---

# Slice 01 — Analytics v2

## Intent

An Analyst gets the click analytics marketing actually needs, with what is counted, how long clicks are kept, what the hashed address may be used for and who reads the figures decided by the human before anything is built, then built and proven on top of the shipped v1.

The ask arrived as one sentence: "marketing says the analytics are not good
enough". The shipped v1 (`missions/01-greenfield-core/slices/02-analytics`,
merged `091ff46`) answers `GET /api/links/{code}/stats` with raw total
clicks, raw clicks per UTC day and the top 10 referrer origins. It counts
bots like people and cannot tell five visits by one person from five people.
The human decided what "better" means (below). This slice adds **unique
visitors per UTC day** and **bot clicks per UTC day** to the same endpoint,
uses the daily-salted client hash for that same-day count and nothing else,
and makes the click's client identity follow the operator's trusted-proxy
rule so that uniques stay truthful behind a proxy.

## Decision (recorded verbatim)

The analytics questions were parked on `human@kernel` on packet
`qitem-20261003154347-19e96a75` at 2026-10-03T15:48Z, with this SPEC at
`88d87a9` as evidence. The human resolved the park at **2026-10-03T16:10:30Z**
(transition 876):

> accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A

What was asked and what each letter means (the stage-1 table, kept as the record):

| # | Question | Options | Decided | What it means for this slice |
|---|---|---|---|---|
| Q1 | What is counted? | A raw clicks only (v1). B add unique visitors per UTC day beside raw clicks. C B plus weekly or monthly uniques. | **B** | `uniqueVisitors` per UTC day (rule 3). No figure combines uniques across days, because the salt rotates at UTC midnight (ADR-0012). |
| Q2 | What may the daily-salted client hash be used for? | A nothing (v1). B counting distinct visitors within its own UTC day only: never exposed, exported, joined with other data or compared across days; the salt stays in memory and is dropped at midnight. C a longer-lived identity. | **B** | Rule 5. The click's client identity is aligned with the trusted-proxy rule (rule 6, wave review W2-02), so the redirect path changes and NFR-L1 applies. |
| Q3 | Are bots counted? | A count everything (v1). B keep every existing figure and add bot clicks per day. C exclude bots from every figure. | **B** | `botClicks` per UTC day (rule 4); `clicks`, `totalClicks` and `topReferrers` keep their v1 meaning. |
| Q4 | How long are clicks kept? | A keep 90 days, then delete (mission 02's purge). B a different number. C roll old clicks into per-day counts kept indefinitely. | **A** | Nothing built here. Figures cover the clicks that remain, a rolling 90-day window once mission 02's purge runs (rule 7). |
| Q5 | Who reads the figures? | A the existing API only, fields added. B plus CSV export. C HTML dashboard. D cross-link view. | **A** | No new endpoint, export or UI (rule 8). |
| Q6 | Which time zone defines a day? | A UTC only (v1). B a time-zone parameter. C an operator-configured zone. | **A** | UTC days, as v1 (rule 9). |

No **(+slice)** option was chosen, so the decided scope is one buildable
outcome and the slice shape stands (mission SPEC, "Re-planning, declared in
advance").

## Mini-requirements

### Requirements covered

Allocated by `missions/03-ambiguous-analytics/SPEC.md` (mission plan-lock
`qitem-20261003114944-9bd32a00`).

| Id | Requirement (short) | Proven by |
|---|---|---|
| FR-16 | "better analytics" decided before anything is built | the *Decision* section (park and verbatim answer); proof contract item 1 |
| FR-8 (v2) | per-link statistics as decided: raw clicks, unique visitors per UTC day, bot clicks per UTC day, top referrers | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6 |
| NFR-P1 (re-checked against Q2 B) | the hash is used only for same-day distinct counts; nothing about a client is exposed | AC-3, AC-7, AC-8, AC-9 |
| NFR-P2 (revisited) | retention stays 90 days, deleted by mission 02's purge | Q4 A; rule 7 (nothing built here) |
| NFR-O3 (new metrics) | counters for recorded and lost clicks | AC-10, AC-11 |
| NFR-L1 (conditional: applies) | redirect p95 ≤ 20 ms, p99 ≤ 50 ms; applies because rule 6 changes the redirect path | AC-15 (structural, in-suite); release-level bench in the proof contract |
| FR-13 (brownfield) | the shipped behaviour keeps working | AC-6, AC-14, AC-15 |
| NFR-O1, O2 (inherited) | request id and log content | AC-12 |
| NFR-M3 (inherited) | committed API document | AC-13 |
| NFR-M1, M2 (cross-cutting) | coverage; ADR before dependent code | proof contract; *Non-functional* |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Analyst (reads how a link performs; the "marketing" voice).
- **Secondary:** Visitor (whose clicks are counted and whose privacy the hash protects); Operator (trusted-proxy setting, metrics).

### User stories

- As an Analyst, I want the number of unique visitors per UTC day beside the clicks, so that I can tell reach from repeat visits.
- As an Analyst, I want the number of bot clicks per UTC day, so that I can discount automated traffic without the existing figures changing meaning.
- As an Analyst, I want every figure I read today to keep its meaning, so that my existing reports stay valid.
- As a Visitor, I want my hashed address used only to count me once per day, so that it never becomes a way to follow me.
- As an Operator, I want unique visitors to stay truthful behind the proxies I trust, so that a proxy deployment does not collapse every visitor into one.
- As an Operator, I want counters for recorded and lost clicks, so that I can see when analytics are losing data.

### Acceptance criteria

Vocabulary inherited from v1 (`missions/01-greenfield-core/slices/02-analytics/SPEC.md`):
"problem detail", "settled", "statistics", "browser `Accept`", and the
suite-controlled clock. Requests sent while the suite clock is shifted come
from a dedicated peer address (the rate limiter keeps a peer's bucket after
reset; mission 01 NOTES §2 12:40Z). The functional suite sets the peer address
and headers per request, as v1's AC-5 and 03-operate's AC-8 do.

#### The new figures

- **AC-1 — The per-day element carries four figures.** [FR-8 v2]
  GIVEN an active link `C` opened once by a browser on UTC day `D`, recording settled
  WHEN an Analyst sends `GET /api/links/C/stats`
  THEN the status is `200`; the body has exactly the four top-level fields of v1 (`code`, `totalClicks`, `clicksPerDay`, `topReferrers`); and every `clicksPerDay` element has exactly `date`, `clicks`, `uniqueVisitors` and `botClicks`, here `{"date": "<D>", "clicks": 1, "uniqueVisitors": 1, "botClicks": 0}`. A link never opened still answers exactly `{"code": "C", "totalClicks": 0, "clicksPerDay": [], "topReferrers": []}`.

- **AC-2 — Unique visitors count distinct clients within a UTC day.** [FR-8 v2, Q1 B]
  GIVEN an active link `C`, the suite clock on UTC day `D`, and clients at peer addresses `203.0.113.1`, `203.0.113.2` and `203.0.113.3`
  WHEN `.1` opens `C` three times, `.2` twice and `.3` once, all on day `D` with a browser `User-Agent`, and recording has settled
  THEN the day-`D` element is `{"date": "<D>", "clicks": 6, "uniqueVisitors": 3, "botClicks": 0}`.

- **AC-3 — Uniques are per UTC day and never combined across days.** [FR-8 v2, Q1 B, Q2 B]
  GIVEN an active link `C` and the suite clock
  WHEN the client at `203.0.113.1` opens `C` at `<D>T23:59:59Z` and again at `<D+1>T00:00:01Z`, and recording has settled
  THEN the elements for `D` and `D+1` each have `uniqueVisitors` `1`, and the body contains no field that sums or combines unique visitors across days (no top-level unique figure).

- **AC-4 — Bot clicks are counted per day, and nothing else changes meaning.** [FR-8 v2, Q3 B]
  GIVEN an active link `C` on UTC day `D`
  WHEN it is opened once with each `User-Agent` row of v1's AC-4 table (one `browser`, three `bot`, one `other`, one `unknown`) from six different peer addresses, and recording has settled
  THEN the day-`D` element is `{"date": "<D>", "clicks": 6, "uniqueVisitors": 6, "botClicks": 3}`, and `totalClicks` is `6` (bots stay in `clicks`, `totalClicks` and `uniqueVisitors`; rule 4).

- **AC-5 — One client's bot and browser clicks are one visitor.** [FR-8 v2]
  GIVEN an active link `C` on UTC day `D`
  WHEN the client at `203.0.113.9` opens `C` once with a browser `User-Agent` and once with a `bot` `User-Agent`, and recording has settled
  THEN the day-`D` element has `clicks` `2`, `uniqueVisitors` `1` and `botClicks` `1`.

- **AC-6 — v1's figures are unchanged on the same clicks.** [FR-13, A-1]
  GIVEN the click sequences of v1's AC-8, AC-9 and AC-10
  WHEN an Analyst reads the statistics
  THEN `totalClicks`, each element's `date` and `clicks`, and `topReferrers` equal v1's expected values exactly, and v1's AC-11 consistency still holds (`totalClicks` equals the sum of `clicks`).

#### Client identity and privacy

- **AC-7 — Behind a trusted proxy, uniques count the forwarded clients.** [Q2 B, W2-02]
  GIVEN the trusted-proxy setting names `10.9.9.9` (the rate limiter's setting, ADR-0015), and an active link `C` on UTC day `D`
  WHEN these redirects of `C` are sent, and recording has settled:

  | Peer address | `X-Forwarded-For` |
  |---|---|
  | `10.9.9.9` | `203.0.113.7` |
  | `10.9.9.9` | `203.0.113.7` |
  | `10.9.9.9` | `198.51.100.1, 203.0.113.8` (left entry forged by the client) |
  | `10.9.9.9` | absent |

  THEN the day-`D` element has `clicks` `4` and `uniqueVisitors` `3` (clients `203.0.113.7`, `203.0.113.8` and the proxy itself); and no click record contains `203.0.113.7`, `203.0.113.8`, `198.51.100.1` or `10.9.9.9`.

- **AC-8 — Without a trusted proxy, forwarding headers change nothing.** [Q2 B, NFR-P1]
  GIVEN the default configuration (no trusted proxy) and an active link `C` on UTC day `D`
  WHEN the client at `203.0.113.77` opens `C` three times, each with a different `X-Forwarded-For` value, and recording has settled
  THEN the day-`D` element has `clicks` `3` and `uniqueVisitors` `1`, and no click record contains any of the `X-Forwarded-For` values.

- **AC-9 — The statistics still expose aggregates only.** [NFR-P1, Q2 B]
  GIVEN a link opened by the clients and canaries of v1's AC-17, and of AC-7 above, recording settled
  WHEN an Analyst reads its statistics
  THEN the body has only the fields of AC-1, and contains no client address, no client hash, no user-agent class, no forwarded value, none of the canaries and no referrer path, query or fragment.

#### Metrics

- **AC-10 — Recorded and lost clicks are counted.** [NFR-O3]
  GIVEN the service is running, and the client records `urlshort.clicks.recorded` and `urlshort.clicks.lost` from `GET /actuator/metrics/<name>`
  WHEN `n` redirects are recorded and settle, then the suite makes the click store fail and `k` further redirects are answered (v1's AC-15 mechanism)
  THEN `urlshort.clicks.recorded` has grown by exactly `n`, and `urlshort.clicks.lost` has grown by exactly `k`, tagged with the same `reason` token the `click lost` log event carries.

- **AC-11 — The click counters are scrapeable and name no client.** [NFR-O3, NFR-P1]
  GIVEN the state after AC-10
  WHEN a scraper sends `GET /actuator/prometheus`
  THEN the body contains the families for both counters, and no tag value on them is a client address, a hash, a link code, a link id, a referrer or a user-agent value (tags are only `reason`, from the static reason vocabulary).

#### Regression guards

- **AC-12 — Request correlation and log content are unchanged.** [NFR-O1, NFR-O2]
  GIVEN the shipped logging configuration
  WHEN v1's AC-18 and AC-19 scenarios run, including AC-7's trusted-proxy redirects
  THEN they pass as written, and no log event contains a forwarded value or a client address.

- **AC-13 — The API document describes v2.** [NFR-M3]
  GIVEN the service is running
  WHEN a client sends `GET /v3/api-docs`
  THEN the statistics operation's `200` schema shows the per-day element with exactly `date`, `clicks`, `uniqueVisitors` and `botClicks`, with an example carrying all four; every other operation is unchanged; and the committed `docs/api/openapi.json` equals the live document (both key-sorted).

- **AC-14 — The shipped suite passes, with only the per-day shape updated.** [FR-13]
  GIVEN the functional suite as it stood at `f6dd29e`
  WHEN it runs against the candidate
  THEN every test passes unchanged, except assertions of the exact per-day element shape (v1's AC-17 "exactly `date` and `clicks`", and any exact-body assertion of a non-empty `clicksPerDay`). Those are updated to the AC-1 shape and nothing else in them changes. The impact analysis names each one.

- **AC-15 — The redirect stays fast and fail-open.** [FR-13, NFR-L1]
  GIVEN the identity change of rule 6 on the redirect path
  WHEN v1's AC-14 (slow store), AC-15 (failing store) and AC-16 (concurrency) run, with the trusted-proxy setting both empty and naming a proxy
  THEN they pass as written.

### Business rules

1. **Response shape.** `GET /api/links/{code}/stats` keeps v1's four top-level fields and their meaning (`code`, `totalClicks`, `clicksPerDay`, `topReferrers`). Each `clicksPerDay` element has exactly `date`, `clicks`, `uniqueVisitors` and `botClicks`, in v1's order and with v1's omission of days without clicks. `topReferrers` is unchanged. No other field is added. Error paths, `HEAD`/`OPTIONS` and anonymous access are as v1 (v1 rule 7).
2. **`clicks`** keeps v1's meaning: every recorded click that UTC day, bots included (v1 rule 1).
3. **`uniqueVisitors`** is the number of distinct stored client hashes among that UTC day's clicks of the link, all user-agent classes included. It is defined per UTC day only. No field adds, averages or otherwise combines it across days, and no figure spans days. A restart during a day draws a fresh salt (ADR-0012, v1 A-10), so a visitor seen before and after it counts twice that day. `uniqueVisitors` is an upper bound in that case, never an undercount.
4. **`botClicks`** is the number of that UTC day's clicks whose stored user-agent class is `bot` (v1 rule 4's classification, unchanged). Bots that pose as browsers are counted as browsers. Bot clicks stay in `clicks`, `totalClicks` and `uniqueVisitors`: nothing is excluded (Q3 B).
5. **What the client hash may be used for (Q2 B).** Only to count distinct visitors within its own UTC day. It is never exposed in a response, metric, log or export, never joined with other data, and never compared across days. The salt stays in memory and is dropped at UTC midnight, as ADR-0012 decides. Nothing in this slice persists or derives it.
6. **Client identity for clicks.** The address that is hashed is the same client the rate limiter identifies (ADR-0015): the peer address, unless the peer is an operator-listed trusted proxy, in which case it is the right-most `X-Forwarded-For` entry that is not itself a trusted proxy (the peer when there is none). `Forwarded`, `X-Real-IP` and every other header are never used. Forwarded values are never stored or logged. Click hashes stored before this change are not rewritten. Behind a proxy that was already configured, they cannot be separated. On the shipped loopback deployment no proxy is configured, so they are already per client.
7. **Retention (Q4 A).** Figures are computed over the clicks that are stored. Mission 02's purge deletes clicks older than 90 days, so once it runs, every figure covers a rolling window, and there is no lifetime total. This slice builds no retention mechanism.
8. **Reader (Q5 A).** The existing statistics endpoint is the only reader. No new endpoint, export, report or UI.
9. **Days (Q6 A).** UTC calendar days, `YYYY-MM-DD`, as v1. The unique count's day is the salt's day.
10. **Metrics.** `urlshort.clicks.recorded` counts clicks written to the store. `urlshort.clicks.lost` counts clicks reported by a `click lost` event, tagged `reason` with that event's static token. No other tag. Both are exposed on `/actuator/metrics` and `/actuator/prometheus`.
11. **Logs.** v1 rule 9 applies unchanged; forwarded values join the list of values that never appear in an event.

### Non-functional

- **Privacy (NFR-P1, re-checked).** Rules 5 and 6, proven by AC-3, AC-7, AC-8, AC-9 and AC-11. "Never compared across days" and "salt never persisted" are not observable over HTTP; the design states them, and the design and security reviews check them (mission SPEC, Risks: privacy regression).
- **Redirect latency (NFR-L1, applies).** Rule 6 adds the identity computation to the redirect's request thread. In-suite, AC-15 proves v1's non-blocking contract still holds. The p95/p99 numbers are measured with `scripts/smoke.sh --bench` at `release_prep`. Until then they are a `docs/qa/GAPS.md` row, never a claim.
- **Statistics cost.** The distinct count is per link and per day over stored clicks (v1 ADR-0013's grouped query). No latency target is set for the statistics read. If the design adds an index, it is a versioned migration with a written rollback (NFR-X2), numbered at plan-lock against mission 02's in-flight migrations.
- **ADR before dependent code (NFR-M2).** The unique and bot aggregation (an amendment to ADR-0013 or a new ADR); the click identity alignment and where it lives (an amendment to ADR-0015 or ADR-0012); the click counters.
- **Coverage gate (NFR-M1).** `scripts/gw check` with 100 % line and branch coverage on merged unit and functional data; honest gaps in `docs/qa/GAPS.md`.

### Scope

**In scope:** `uniqueVisitors` and `botClicks` per UTC day on the existing statistics endpoint; click identity aligned with the trusted-proxy rule; the two click counters; the API document update; the impact analysis over v1; tests, coverage, traceability, gaps and the proof artifacts below.

**Explicitly out of scope:**

- Uniques over a week, a month or a link's lifetime (Q1 C not chosen; impossible under the daily salt).
- Excluding bots from any figure (Q3 C not chosen); a finer user-agent classification.
- Retention changes, aggregation of old clicks, lifetime totals (Q4 A); the purge itself (mission 02).
- A CSV export, report, dashboard or cross-link view (Q5 B/C/D not chosen).
- Time-zone parameters or settings (Q6 A).
- Time-range parameters (A-4), top-referrer changes (A-3), any change to the redirect's response or to link management or audit.
- Rewriting or back-filling stored click hashes.

## Ambiguity log

The six product questions are **decided by the human** (see *Decision*). The
rows below are the remaining questions, each with a safe default.

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Does the statistics contract stay backward compatible? | change meanings; add fields only | **decided** add fields only, as the Q5 A option text says ("fields added"). Top-level fields and v1 figures keep their meaning (AC-6). The one visible change is the per-day element gaining two fields. v1's "exactly `date` and `clicks`" assertion is superseded by AC-1 and named in the impact analysis (AC-14). |
| A-2 | Which new metrics does NFR-O3 ask of this slice? | none; recorded and lost click counters | **assumed** the two counters of rule 10. They make the analytics' own data loss visible to scraping, where today it is only a WARN line. Safe: additive, static tags only. |
| A-3 | Does the top-referrers list change? | keep; more; paths | **assumed** unchanged (Q5 A adds fields to the per-day element only; paths stay out under NFR-P1). |
| A-4 | Time-range parameters? | none; `from`/`to` | **assumed** none. The per-day series lets a reader cut any window, and Q4 A bounds it to 90 days. Safe: can be added later without breaking clients. |
| A-5 | Do `uniqueVisitors` count bot clicks? | all classes; browsers only | **assumed** all classes (rule 3). Q3 B says nothing is excluded and bots are reported beside the figures, and excluding them from uniques alone would make the per-day figures inconsistent with each other. Safe: an Analyst can see `botClicks` beside it. A bots-excluded unique count can be added later as a field. |
| A-6 | Is there a total of unique visitors over the window? | yes; no | **decided** no (rule 3, AC-3). The Q1 B consequence the human accepted: uniques exist per UTC day only, and summing them would count a returning visitor once per day, a misleading figure. |
| A-7 | Where is the identity alignment built? | in `click/`; reuse the rate limiter's rule | **assumed** the click's client is the rate limiter's client by the same rule and the same setting (rule 6), so one operator setting governs both. Where the code lives is the design's. ADR-0015 sketches a request wrapper in the rate-limit filter, which is outside this slice's predicted territory (`web/`). The design requests the grant if it needs one, and the lead decides it at plan-lock. |
| A-8 | Does the restart double-count matter? | require a persistent salt; accept an upper bound | **decided** accept the upper bound (rule 3). Q2 B keeps the salt in memory and dropped at midnight, which rules out persisting it. |
| A-9 | Who holds `docs/api/openapi.json` while mission 02's `01-audit-read` also adds an operation? | — | **for the lead's plan-lock** (not a product question). Both slices regenerate the committed document. The cross-mission rule in the mission SPEC covers the migration number and `click/`. This file needs the same ordering (the later slice rebases and regenerates). Recorded so the plan-lock checks it. |

## Proof contract

- [ ] The human's decision is recorded verbatim in this SPEC with its packet id and time, before any design commit for this slice (FR-16). Done at stage 2 (*Decision*).
- [ ] AC-1 through AC-15 are each covered by a named test (tabled criteria as one parameterised test) and are green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional data (NFR-M1).
- [ ] Unit and functional JaCoCo reports committed under `docs/qa/coverage/01-analytics-v2/unit/` and `docs/qa/coverage/01-analytics-v2/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `01-analytics-v2` mapping AC-1 to AC-15 and rules 1 to 11 to their tests, with the requirement id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `01-analytics-v2`: at least the NFR-L1 numbers until the release bench measures them, and any other honest gap.
- [ ] `proof/` holds a by-effect capture from the running service: redirects of one link from three peer addresses (one twice, one with a bot `User-Agent`) and the statistics body showing `clicks`, `uniqueVisitors` and `botClicks` for the day, with no hash or address in it. This checks AC-1, AC-2 and AC-4 on the real service. AC-3, AC-5 and AC-7 are proven by the controlled functional tests.
- [ ] `proof/` holds an excerpt of `GET /actuator/prometheus` showing both click counters (AC-11 by effect).
- [ ] `docs/api/openapi.json` is regenerated on the candidate and QA's diff against the live `/v3/api-docs` is empty (AC-13).
- [ ] The impact analysis over v1 (`impact-analysis.md`) names every v1 test whose exact per-day shape assertion changes (AC-14), the redirect-path change (rule 6), and the cross-mission custody of `click/`, the migration number and `docs/api/openapi.json`.
- [ ] The design and security reviews record that the hash is used only for same-day distinct counts and that the salt is never persisted (rule 5).
- [ ] At `release_prep`, redirect p95/p99 are measured with `scripts/smoke.sh --bench` against NFR-L1, and the result is recorded or the gap stays in `GAPS.md`.

## Source material

- `missions/03-ambiguous-analytics/SPEC.md` (decision brief, risks, re-planning rule), `NOTES.md` §1 (plan-lock text; W2-02 input), `slices/01-analytics-v2/slice.yaml`.
- The human's decision: packet `qitem-20261003154347-19e96a75`, transition 876.
- Shipped v1: `missions/01-greenfield-core/slices/02-analytics/SPEC.md` (AC-4, AC-8 to AC-11, AC-14 to AC-19; rules 1–9) and `design.md`; ADR-0011, ADR-0012, ADR-0013; ADR-0015 (trusted proxies).
- `missions/02-brownfield/SPEC.md` and `slices/02-click-retention/SPEC.md` (the 90-day purge); `slices/01-audit-read` (also regenerates the API document).
- `docs/REQUIREMENTS.md`: FR-8, FR-16, NFR-P1, P2, O3, L1.

## Intent visual

N/A: non-visual slice (Q5 A, no dashboard).

## Status

- 2026-10-03: stage 1 written: six product questions (Q1–Q6) with options, recommendations and consequences; four assumed rows. Parked on `human@kernel` at 15:48Z (`88d87a9`).
- 2026-10-03T16:10Z: the human answered "accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A". No extra slice is needed.
- 2026-10-03: stage 2 written from the decision: 15 acceptance criteria, 11 business rules, 9 ambiguity rows (5 assumed, 3 decided, 1 for the lead's plan-lock), none parked. Handed to `requirements_review`.

## Dependencies

- Mission 01 (merged): the click table and recorder, the statistics endpoint (`091ff46`), the trusted-proxy rule and setting (ADR-0015).
- Mission 02, in flight: `02-click-retention` (same `click/` package, the next Flyway number, the 90-day purge that Q4 A keeps) and `01-audit-read` (also regenerates `docs/api/openapi.json`, A-9). The ordering is decided at this slice's plan-lock under the cross-mission rule.

## Self-check (stage 2)

- The decision is recorded verbatim with packet, transition and time. Every rule and AC traces to a decided letter or to an assumed row with a reason. Nothing chosen by the human is widened, and every option the human did not choose is listed out of scope.
- Every AC is observable: statistics bodies (AC-1 to AC-9), stored click rows (AC-7, AC-8), metrics and Prometheus (AC-10, AC-11), logs (AC-12), the live API document (AC-13), and v1's own criteria re-run (AC-6, AC-12, AC-14, AC-15).
- Privacy paths are ACs: no hash or address in the body (AC-9), forwarded values never stored (AC-7, AC-8), no client values in metric tags (AC-11) or logs (AC-12). The cross-day prohibition is an AC where it is observable (AC-3) and a review obligation where it is not.
- The non-obvious logic is in the rules: uniques counted per salt day as an upper bound across restarts, bots in every figure, identity aligned with the rate limiter, existing hashes not rewritten, and a rolling window after the purge.
- The brownfield contract change is named, not hidden: the per-day element gains two fields, v1's exact-shape assertion is superseded, and the impact analysis must list each changed test (AC-14).
- No design leaked. ADR and setting names cite shipped facts. Where the identity code lives is the design's (A-7). Metric names are operator contract, as on `03-operate`.
- Cross-mission custody is surfaced for the lead (A-9; *Dependencies*).
- `plan-review` lenses applied by the author (the skill was not invoked separately). The engineering lens added AC-5 (one client, two classes), the restart upper bound in rule 3, and A-9 (the API-document custody the mission's cross-mission rule did not name). The strategy lens kept every unchosen option out. The UX lens kept the empty-link body identical to v1 (AC-1).
- `rig scope audit --mission 03-ambiguous-analytics` is run before handoff.
- Not verified by me: that ADR-0015's identity function can be reached from the click hook without a grant on `web/` (A-7). This is for the design and the lead's plan-lock.
