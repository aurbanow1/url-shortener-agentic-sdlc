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
---

# Slice 01 — Analytics v2

## Intent

An Analyst gets the click analytics marketing actually needs, with what is counted, how long clicks are kept, what the hashed address may be used for and who reads the figures decided by the human before anything is built, then built and proven on top of the shipped v1.

The ask arrived as one sentence: "marketing says the analytics are not good
enough". The shipped v1 (`missions/01-greenfield-core/slices/02-analytics`,
merged `091ff46`) answers `GET /api/links/{code}/stats` with raw total
clicks, raw clicks per UTC day and the top 10 referrer origins over every
stored click. Behind "not good enough" sit product decisions that change what
gets built and what the service may do with data about people. **This SPEC is
in two stages.** Stage 1 (this version) is the decision request: the ambiguity
log below, parked on `human@kernel`. Stage 2 writes the user stories,
acceptance criteria, rules and proof contract from the human's recorded
answer. Nothing is designed or built before then (mission plan-lock,
2026-10-03T15:40Z: "park the analytics questions on me before design").

## Decision requested (parked on `human@kernel`)

Answer per question with a letter, or "accept all recommended", which means
`Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A`. Free text overrides any option.
Every recommendation is the narrowest option that still answers "better
analytics". An option marked **(+slice)** is a second buildable outcome, and
the orchestration lead would add a slice for it (mission SPEC, "Re-planning,
declared in advance").

| # | Question | Options | Recommended | Consequence of the recommendation |
|---|---|---|---|---|
| Q1 | **What is counted?** | **A** raw clicks only, as v1. **B** add **unique visitors per UTC day** (distinct client hashes in that day) beside raw clicks. **C** B plus unique visitors over a week or month. | **B** | Marketing gets "people per day" beside "clicks per day". Uniques exist only per UTC day, because the salt rotates at UTC midnight (ADR-0012), so there is no weekly or monthly unique figure and daily uniques cannot be summed into one. C requires Q2 C. |
| Q2 | **What may the daily-salted client hash be used for?** | **A** nothing (v1 today: stored, never read). **B** counting distinct visitors **within its own UTC day only**: never exposed, exported, joined with other data or compared across days; the salt stays in memory and is dropped at midnight as now. **C** a longer-lived identity (weekly or persistent salt) enabling cross-day uniques. This weakens NFR-P1 and replaces ADR-0012. | **B** (required by Q1 B) | The hash starts carrying meaning. **Fact from mission 01's wave review (W2-02):** the click hook hashes the connection address, while the rate limiter follows `X-Forwarded-For` from configured trusted proxies. With the shipped default (no trusted proxy, loopback-only) both see the real client. Behind a configured proxy, every visitor's clicks share the proxy's hash and would count as one unique. Under B the build therefore aligns click identity with the trusted-proxy rule (ADR-0015), which touches the redirect hook, so NFR-L1 (redirect latency) applies. Hashes already stored behind a proxy cannot be separated later. On today's loopback deployment no such hashes exist. |
| Q3 | **Are bots counted?** v1 stores a user-agent class per click (`browser`, `bot`, `other`, `unknown`) and reads nothing from it. | **A** count everything, as v1. **B** keep every existing figure as is and add **bot clicks per day**, so marketing can subtract them. **C** exclude `bot`-class clicks from every figure. | **B** | Additive and reversible: no existing number changes meaning. The class is a coarse substring rule, so bots that pose as browsers stay counted as browsers. C changes the meaning of v1's numbers for every existing reader. |
| Q4 | **How long are clicks kept, and what survives?** Mission 02 is about to build the NFR-P2 purge: delete clicks older than 90 days, the period an operator setting (mission 02 plan-lock, 15:40Z). | **A** keep 90 days, then delete (as mission 02 builds it). Statistics cover the last 90 days. **B** change the number of days (say which). **C** before deletion, roll each link's old clicks up into per-day counts (clicks, uniques, bot clicks) kept **indefinitely**, so the per-day history and lifetime total survive while per-click rows (with hashes) still go at 90 days. **(+slice, and it changes mission 02's purge)** | **A** | Simplest, already decided, and no per-click data lives past 90 days. The cost is that `totalClicks` and the per-day series shrink to a rolling 90-day window once the purge runs, so a link's lifetime total is lost. If marketing needs history beyond 90 days, choose C. C also changes mission 02's `02-click-retention` from delete to aggregate-then-delete, and that slice's requirements are on this seat now, so the answer reaches it before it is specified. |
| Q5 | **Who reads the figures?** | **A** the existing API only: the statistics endpoint gains the new figures (backward compatible: existing fields keep their meaning). **B** A plus a CSV export of the per-day series. **C** an HTML dashboard page. **(+slice)** **D** a cross-link view (top links by clicks). **(+slice)** | **A** | One endpoint, one contract, no UI, and the committed API document changes in one place. Marketing tooling (a spreadsheet, a BI tool) reads JSON. B, C and D are each a separate outcome with their own review. |
| Q6 | **Which time zone defines a day?** | **A** UTC only, as v1. **B** a time-zone parameter on the request. **C** one operator-configured zone. | **A** | Days stay aligned with the salt day, so a per-day unique count is exact. Under B or C a local day straddles two salt days, and a visitor seen at 23:00 and 01:00 UTC would count twice in one local day. Readers in other zones see UTC dates. |

If the answer is "accept all recommended", the build is one outcome on the
existing endpoint: add unique visitors per UTC day and bot clicks per UTC
day, put the hash to use for same-day distinct counting only, align click
identity with the trusted-proxy rule, and keep everything else as v1 and
mission 02 decided (UTC, the API as the only reader, 90-day delete).

## Mini-requirements

### Requirements covered

Allocated by `missions/03-ambiguous-analytics/SPEC.md` (mission plan-lock
`qitem-20261003114944-9bd32a00`). The "Proven by" column is filled in stage 2.

| Id | Requirement (short) | Status in this version |
|---|---|---|
| FR-16 | "better analytics" turned into decided requirements before anything is built | this version: the questions are parked on the human (Q1–Q6) |
| FR-8 (v2) | the per-link statistics the decision defines | stage 2 |
| NFR-P2 (revisited) | click retention | Q4 |
| NFR-O3 (new metrics) | metrics for what the decision adds | stage 2; A-2 below |
| NFR-L1 (conditional) | redirect latency, only if the redirect path changes | applies if Q2 B or C (identity alignment touches the redirect hook) |
| NFR-M1, M2 (cross-cutting); M3, O1, O2 (inherited) | coverage, ADRs, API document, request id, logs | stage 2 |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Analyst (reads how a link performs: clicks over time, where they came from; the "marketing" voice).
- **Secondary:** Visitor (whose clicks are counted and whose privacy the hash protects); Operator (retention setting, metrics, proxy configuration).

### User stories

Written in stage 2 from the recorded decision.

### Acceptance criteria

Written in stage 2 from the recorded decision. None is written before the
answer, so that no criterion presumes it.

### Business rules

Written in stage 2. v1's rules (`missions/01-greenfield-core/slices/02-analytics/SPEC.md`
rules 1–9) stay in force unless the decision changes one, and stage 2 names
each change.

### Non-functional

Stage 2. NFR-P1 is re-checked against the Q2 answer by the design and
security reviews (mission SPEC, Risks: privacy regression).

### Scope

**In scope:** what the human's answer selects, as one outcome. Anything
selected with **(+slice)** becomes a further slice added by the orchestration
lead.

**Explicitly out of scope, whatever the answer:** custom aliases and expiry
(FR-11, FR-12, dropped); the purge mechanism itself (mission 02's
`02-click-retention`, unless Q4 C moves its shape); changes to link
management, the redirect's response or the audit trail; raw IPs, full user
agents or referrer paths anywhere (NFR-P1, unchanged).

## Ambiguity log

Q1–Q6 are **parked on human** (see *Decision requested*; packet
`qitem-20261003154347-19e96a75`, parked 2026-10-03). The answer is recorded
here verbatim when it arrives. The rows below are not product decisions:
each has a safe default and does not wait for the human.

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Does the existing `GET /api/links/{code}/stats` contract stay backward compatible? | change field meanings; add fields only | **assumed** add fields only. Existing fields (`code`, `totalClicks`, `clicksPerDay[].date/clicks`, `topReferrers`) keep their meaning, except where the Q4 answer changes the window they cover, which stage 2 states. Safe: no current reader breaks. |
| A-2 | Which new metrics does NFR-O3 ask of this slice? | none; counters for what v2 adds | **assumed** stage 2 names at most the counters the decision makes useful (for example recorded versus dropped clicks), with names in the API's metric namespace. Safe: additive, no client values in tags (v1 rule 9, 03-operate rule 10). |
| A-3 | Does the top-referrers list change? | keep top 10 origins; more; referrer paths | **assumed** unchanged unless the answer says otherwise. Paths stay out under NFR-P1. Not raised as a question because no option widens it safely. |
| A-4 | Time-range parameters on the statistics endpoint? | none (v1); `from`/`to` | **assumed** none. The per-day series already lets a reader cut any window, and Q4 bounds the series. Safe: a range parameter can be added later without breaking clients. The human may override this in the answer. |

## Proof contract

Completed in stage 2. Fixed now, whatever the answer:

- [ ] The human's decision is recorded verbatim in this SPEC (ambiguity log) with its packet id before any design commit for this slice (FR-16).
- [ ] All stage-2 AC-n are green in the functional suite on the candidate SHA.
- [ ] Unit and functional coverage reports committed under `docs/qa/coverage/01-analytics-v2/`.
- [ ] AC ↔ test rows for this slice in `docs/qa/TRACEABILITY.md`.
- [ ] A `docs/qa/GAPS.md` entry if 100 % coverage is not met honestly.

## Source material

- `missions/03-ambiguous-analytics/SPEC.md` (decision brief, risks, re-planning rule), `NOTES.md` §1 (plan-lock text; W2-02 input), `slices/01-analytics-v2/slice.yaml`.
- Shipped v1: `missions/01-greenfield-core/slices/02-analytics/SPEC.md` and `design.md`; ADR-0011 (click handoff), ADR-0012 (daily salt in memory), ADR-0013 (click rows, per-day grouping, purge-ready); ADR-0015 (trusted proxies).
- `missions/02-brownfield/SPEC.md` (`02-click-retention`: 90-day delete as an operator setting).
- `docs/REQUIREMENTS.md`: FR-8, FR-16, NFR-P1, P2, O3, L1.

## Intent visual

N/A: non-visual slice, unless Q5 C (dashboard) is chosen, in which case its own slice carries one.

## Status

- 2026-10-03: stage 1 written: six product questions (Q1–Q6) with options, recommendations and consequences; four assumed rows. Parked on `human@kernel`. Stage 2 (stories, ACs, rules, proof) follows the recorded answer.

## Dependencies

- Mission 01 (merged): the click table, the redirect hook, the statistics endpoint (`091ff46`), the trusted-proxy rule (ADR-0015).
- Mission 02 `02-click-retention` (in flight, same `click/` package and the next Flyway number): the Q4 answer feeds it. The cross-mission plan-lock rule in the mission SPEC applies.

## Self-check (stage 1)

- The questions are the ones the mission brief and plan-lock name: counted (Q1, Q3 for bots), retention (Q4), privacy (Q2), reader (Q5), time zones (Q6). None is manufactured, because each changes what gets built or what the hash may be used for, and none has a default the human has already decided, except Q4, where the 90-day delete is decided for mission 02 and is offered as the recommended default.
- Each question has options, a recommendation, and the consequence of the recommendation. The consequences that cross missions or touch privacy are stated as facts with sources: the salt-day limit on uniques (ADR-0012), W2-02 identity behind proxies, Q4 C changing mission 02's purge, and the time-zone/salt-day clash.
- No scope widened: every recommendation is the narrowest option that adds value, extra outcomes are marked **(+slice)**, and nothing outside the human's choice is promised.
- No design leaked into the questions: table and class names appear only where they cite shipped facts (ADR references).
- Not done yet, and by design: stories, ACs, rules and the full proof contract wait for the answer.
