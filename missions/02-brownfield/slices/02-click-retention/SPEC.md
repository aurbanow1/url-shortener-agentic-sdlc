---
id: OPR.99.0.3.2
slice: 02-click-retention
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Operator can rely on clicks older than the retention period (90 days by default, an operator setting) being deleted on schedule, so click data stops growing without bound and no visit is kept longer than decided."
depends_on: []
---

# Slice 02 — Click Retention

## Intent

An Operator can rely on clicks older than the retention period (90 days by default, an operator setting) being deleted on schedule, so click data stops growing without bound and no visit is kept longer than decided.

The shipped analytics (`missions/01-greenfield-core/slices/02-analytics`,
merged `091ff46`) store one reduced row per click and keep every row forever.
NFR-P2 decided 90 days at mission 01's plan-lock, and mission 01 built no
purge. Each day of delay keeps more client hashes and referrer origins than
the product promised, and lets the table grow without bound. The human
confirmed at this mission's plan-lock (2026-10-03T15:40Z): "purge stays here
with the 90-day default as an operator setting".

## Mini-requirements

### Requirements covered

| Id | Requirement (short) | Proven by |
|---|---|---|
| NFR-P2 | click retention 90 days, then deleted, enforced by a job, not by hope | AC-1, AC-2, AC-3, AC-4, AC-7, AC-8, AC-9, AC-15 |
| FR-13 | existing links, redirects, statistics and audit keep working unchanged across the change, migration included | AC-5, AC-6, AC-11, AC-13, AC-14 |
| NFR-X2 | a versioned migration with a written rollback | AC-13, AC-16; *Non-functional* |
| Audit-column policy (human decision 2026-10-03, A-10; `docs/guidance/databases.md` §2) | every table carries `created_at`/`updated_at`, and `created_by`/`updated_by` where an actor exists; an existing table gets them in the next migration that touches it | AC-13, AC-16 |
| FR-15 (in passing, wave review W2-05) | a click-reduction failure reports its own reason | AC-12 |
| NFR-O1, O2 (inherited) | structured logs, request id on request events, no client values | AC-9, AC-10, AC-12 |
| NFR-M1, M2 (cross-cutting) | coverage gate; ADR before dependent code | proof contract; *Non-functional* |

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Operator (runs the service; sets the retention period; relies on the purge without watching it).
- **Secondary:** Analyst (whose statistics now cover the retained window); Visitor (whose click data is now deleted on time).

### User stories

- As an Operator, I want clicks older than the retention period deleted automatically every day, so that click data stops growing without bound and no visit is kept longer than decided.
- As an Operator, I want to set the retention period by configuration with a 90-day default, so that a changed policy is a setting, not a release.
- As an Operator, I want each purge run to leave one log event with its count and cutoff, and a failed run to say so, so that I can see the guarantee holding.
- As an Analyst, I want links and statistics to keep working after old clicks are deleted, so that only data past the period disappears.
- As a Visitor, I want nothing about my click kept past the period, so that the privacy promise has an end date.

### Acceptance criteria

"Suite-controlled clock" is the mechanism mission 01 uses (`FunctionalClock`).
Requests sent while the clock is shifted come from a dedicated peer address,
because the rate limiter keeps that peer's bucket after a reset (mission 01
NOTES §2 12:40Z). "Run a purge" means the scheduled purge runs, either because
its scheduled time passed on the suite clock or because the suite triggers the
same run the scheduler would start. The mechanism is the design's. "Problem
detail", the log rules and the statistics fields are inherited from mission
01 (`01-create-redirect` rules 8 and 10; `02-analytics` rule 7).

Stored click rows are inspected by the suite, as in `02-analytics`.

#### The purge

- **AC-1 — The boundary day is kept and the day before it is deleted.** [NFR-P2]
  GIVEN the default retention period (90 days), the service clock on UTC day `T`, and one link with clicks recorded on UTC days `T−91`, `T−90`, `T−89` and `T` (two on each day)
  WHEN a purge runs
  THEN the two clicks of `T−91` no longer exist, and the six clicks of `T−90`, `T−89` and `T` still exist unchanged.

- **AC-2 — The window moves with the day.** [NFR-P2]
  GIVEN the state after AC-1
  WHEN the service clock moves to UTC day `T+1` and a purge runs
  THEN the clicks of `T−90` no longer exist, and those of `T−89` and `T` still exist.

- **AC-3 — The period is an operator setting.** [NFR-P2]
  GIVEN the service started with the retention period set to `7` through its environment-overridable setting, and clicks on UTC days `T−8`, `T−7` and `T`
  WHEN a purge runs on day `T`
  THEN the clicks of `T−8` no longer exist, and those of `T−7` and `T` still exist.

- **AC-4 — An invalid period stops the service from starting.** [NFR-P2]
  GIVEN the retention period setting is `0`, `-5`, or `ninety`
  WHEN the service starts
  THEN it does not start (startup fails before any request is served), the startup failure report names the setting and the rejected value and echoes no other configuration value (in particular not the datasource URL or a credential), and no click is deleted. Log lines written earlier in the same startup are the shipped startup logging and are not part of this criterion.

- **AC-5 — Statistics cover the retained clicks only.** [NFR-P2, FR-13]
  GIVEN a link whose clicks span UTC days `T−95` to `T` with distinct referrer origins on the oldest days, and its statistics recorded before the purge
  WHEN a purge runs on day `T` and an Analyst reads `GET /api/links/<code>/stats`
  THEN the status is `200` with the same four fields as before; `clicksPerDay` lists only days from `T−90` on, each with the count it had before the purge; `totalClicks` equals the sum of those counts; and `topReferrers` is computed over the retained clicks only (an origin seen only on purged days is absent).

- **AC-6 — Links, redirects and the audit trail are untouched.** [FR-13]
  GIVEN an active link `C` whose only clicks are older than the period, and the audit rows recorded with their content
  WHEN a purge runs, and the suite then reads `C`'s statistics, opens `GET /C` once, and reads the statistics again after recording settles
  THEN the first statistics read answers `200` with `totalClicks` `0`, `clicksPerDay` `[]` and `topReferrers` `[]`; `GET /C` answers `302` with the same `Location`; the second statistics read shows `totalClicks` `1`, with one `clicksPerDay` element for day `T`; `GET /api/links/C` answers `200` with its creation body; every audit row is present with identical content; and no audit row was added.

- **AC-7 — A purge runs at startup.** [NFR-P2]
  GIVEN stored clicks older than the period, and a service that was not running at its scheduled purge time
  WHEN the service starts and no trigger is sent
  THEN within 60 seconds of the service reporting readiness, those clicks no longer exist.

- **AC-8 — A purge runs every UTC day without an operator.** [NFR-P2]
  GIVEN the service has run its startup purge and new clicks are stored for a day that will fall out of the window at the next UTC day
  WHEN the suite-controlled clock passes the next UTC day's scheduled purge time, and no trigger is sent
  THEN those clicks no longer exist within 60 seconds of that time on the suite clock. The scheduled time is recorded in `docs/DESIGN.md`.

#### Operator visibility

- **AC-9 — Each run logs one event with its outcome.** [NFR-P2, NFR-O2]
  GIVEN the shipped logging configuration
  WHEN a purge runs that deletes `n` clicks (including `n` = `0`)
  THEN exactly one `INFO` log event is written for the run, a single-line JSON object carrying the number of deleted clicks `n`, the cutoff (the earliest UTC day kept, `YYYY-MM-DD`) and the period in days, and no client hash, referrer, user-agent class, link code or link id.

- **AC-10 — A failed run is reported and leaves the service working.** [NFR-P2, NFR-O2]
  GIVEN the suite has made the next purge fail in the store (the mechanism is the design's), and clicks older than the period exist
  WHEN that purge runs, and then a further purge runs with the store working again
  THEN the failed run writes exactly one `WARN` event that carries the exception's class name and no exception message, stack trace or stored value; redirects and statistics answer as before during and after the failure; and the further run deletes the old clicks.

- **AC-11 — A redirect during a purge is served and counted.** [FR-13]
  GIVEN a link with many clicks older than the period
  WHEN a Visitor opens the link while a purge is deleting them
  THEN the redirect answers `302` as before, and its click (today's) is recorded and appears in the statistics once recording has settled.

- **AC-15 — A paused purge deletes nothing and says so at every start.** [NFR-P2]
  GIVEN the service is started with the purge paused through its setting, and clicks older than the period exist
  WHEN the service starts, and later the suite-controlled clock passes the next UTC day's scheduled purge time with no trigger sent
  THEN exactly one `WARN` event at startup names the setting and says no click is being deleted; no purge `INFO` event is written; and every click, including those older than the period, still exists 60 seconds after readiness and 60 seconds after the scheduled time on the suite clock.

#### In passing (FR-15, wave review W2-05)

- **AC-12 — A click-reduction failure reports its own reason.** [FR-15, NFR-O1]
  GIVEN the suite has made the reduction of a click fail before it is queued (the mechanism is the design's; the shipped test induces it in the keyed hash)
  WHEN a Visitor opens a link
  THEN the redirect answers `302`; exactly one `WARN` event `click lost` is written with the redirect's `requestId` and a `reason` that is distinct from `rejected` and names the reduction failure; and the operator-facing list of `click lost` reasons in `docs/DESIGN.md` includes it.

#### Upgrade

- **AC-13 — An existing data directory upgrades in place.** [FR-13, NFR-X2]
  GIVEN a data directory written by the shipped service at `f6dd29e` holding links, clicks within and beyond the period, and audit rows
  WHEN the candidate starts on that directory
  THEN migrations apply without error; every link and every audit row is present and unchanged; every click within the period and every user-agent class is present with its shipped columns unchanged and its four audit columns filled (AC-16); and only clicks beyond the period are deleted (by the startup purge of AC-7).

- **AC-14 — The shipped behaviour still passes.** [FR-13]
  GIVEN the functional suite as it stood at `f6dd29e`
  WHEN it runs against the candidate
  THEN every test passes unchanged. If a test must change because it stores clicks older than 90 days on the suite clock, the impact analysis names it and the reason, and nothing else about it changes. The functional profile overlay gains one line that pauses the purge for the shared test contexts (rule 3, design review DR-01; granted by the lead in `slice.yaml` at `cec7032`), and the purge journeys turn it back on in their own contexts. A run's event carries no request id (rule 6), and shipped journeys require every line in their capture window to carry one. The impact analysis records the line. No test source changes for it. The new audit columns (AC-16) need no test change either: shipped tests that insert click rows with their v1 column list keep working.

- **AC-16 — The click tables carry audit columns.** [NFR-X2, audit-column policy]
  GIVEN the candidate's migrations applied to an empty database, and separately to the `f6dd29e` data directory of AC-13
  WHEN the suite reads the database schema and the stored rows
  THEN tables `click` and `user_agent_class` each have `created_at` and `updated_at` (timestamp with time zone, not null) and `created_by` and `updated_by` (not null); every pre-existing row has all four filled, with `updated_at` equal to `created_at`; a click recorded by a redirect after the upgrade has `created_at` = `updated_at` (the row's write time; `clicked_at` keeps the click's time on the service clock), and `created_by` = `updated_by` = `anonymous`; every user-agent class row has `created_by` = `updated_by` = `system`; no audit column holds a client hash, address, user agent, referrer or request id; and every column and constraint of the v1 tables is unchanged (expand only, no column dropped, renamed or retyped).

### Business rules

1. **Retention period.** A positive whole number of days, default `90` (NFR-P2, decided). It is an operator setting overridable by an environment variable; the design records its name in `docs/DESIGN.md`. Any other value stops the service at startup with an error that names the setting and the rejected value, and no other configuration value. There is no silent fallback to the default and no "zero means keep nothing".
2. **What is deleted.** On UTC day `T`, with period `P`, the purge deletes every click whose stored UTC day is earlier than `T − P`. The click's stored UTC day is the day v1 already records for it (ADR-0013). The day `T − P` itself is kept, so every click is kept for at least `P` full days and deleted by the first run after its day leaves the window. Nothing is aggregated or kept in another form. Deletion removes the row.
3. **When it runs.** Once shortly after startup (AC-7), and once every UTC day at a fixed time that the design records (AC-8). An Operator can pause the purge through a documented setting, for example during an incident investigation or a legal hold. The purge runs by default. While it is paused, neither the startup run nor the daily run deletes anything, and the service says so in one `WARN` at every start (AC-15). Pausing is the Operator's documented choice and is never silent. A run that fails is retried by the next scheduled run, with no tighter retry loop. Runs never overlap.
4. **What it touches.** Click rows only. Links, audit rows, the redirect and the statistics contract are unchanged. Statistics are computed over the clicks that remain, so after a purge they cover the retained window (AC-5). The purge is not a mutation of a link: it writes no audit row. The run's log event is its record (A-4).
5. **Visitor first.** A purge runs off the request path. A redirect during a purge is served and its click recorded as v1 promises (AC-11, `02-analytics` rules 5 and 6).
6. **Logs.** One `INFO` event per run with count, cutoff day and period; one `WARN` per failed run with the exception class only. No event carries a stored click value, a link code or a link id. A run is not a request, so its events carry no `requestId` (NFR-O1 governs request events).
7. **`click lost` reasons (W2-05).** A click that fails reduction before it is queued is reported with a reason of its own, distinct from `rejected` (which keeps its meaning: a full or closed queue). The reason is a static token, the event keeps the v1 shape (`requestId`, `reason`, `errorType`), and no new logging layer is added.

### Non-functional

- **NFR-P2.** 90 days by default, an operator setting, enforced by a scheduled job with a run at startup. The purge runs by default; pausing it is loud. Proven by AC-1 to AC-4, AC-7 to AC-10 and AC-15.
- **NFR-X2.** This slice adds one expand migration with the next free Flyway number after `01-audit-read`'s (ordered custody, mission SPEC). It gives `click` and `user_agent_class` the audit columns (AC-16), and an index for the purge if the design needs one. It only adds. Its header carries a written rollback, as V1 and V2 do, that removes only what it added. Proven by AC-13, AC-16 and the review.
- **Audit-column policy (human decision, A-10).** `created_at` and `updated_at` say when the row was written; `clicked_at` stays the time of the click. Click rows are insert-only and the purge deletes them, so `updated_at` equals `created_at` for their whole life. The actor for a click row is the unauthenticated Visitor (`anonymous`), and for seeded reference rows it is `system` (A-11). How pre-existing rows are backfilled is the design's, recorded in the migration header.
- **Lock behaviour.** A purge over many rows must not stall redirects or click writes beyond what AC-11 tolerates. How it bounds that (for example by deleting in batches) is the design's and is recorded in an ADR (NFR-M2).
- **ADR before dependent code (NFR-M2).** The purge schedule and mechanism (time, startup run, no overlap, batching if any, failure handling); the retention setting; the audit-column migration and its backfill; the index, if added.
- **Coverage gate (NFR-M1).** `scripts/gw check` with 100 % line and branch coverage on merged unit and functional data; honest gaps in `docs/qa/GAPS.md`.
- **Territory.** `click/` (main, unit, functional), `db/migration/` for the one expand migration (AC-16, and an index if needed), and `application.properties` for the retention and purge-pause settings (second holder after `01-audit-read` merges). The functional profile overlay, one line (AC-14), by the lead's test-side grant (`cec7032`). Not `link/`, `audit/`, `web/` or `docs/api/openapi.json`: no endpoint changes.

### Scope

**In scope**

- The scheduled purge of click rows older than the retention period, with its startup run, log events and failure handling.
- The retention-period operator setting with default 90 and startup validation.
- One expand migration adding the audit columns to `click` and `user_agent_class` (AC-16), with an index for the purge if the design needs one.
- The distinct `click lost` reason for reduction failures (W2-05).
- Unit and functional tests, coverage, traceability, gaps, and the proof artifacts below.

**Explicitly out of scope**

- Aggregating old clicks into per-day counts or keeping any form of purged data. This is mission 03's question Q4 option C, parked on the human (packet `qitem-20261003154347-19e96a75`), and see *Dependencies*.
- Any change to the statistics endpoint, its fields or the API document.
- Purging or retaining anything other than clicks: links, audit rows, idempotency bindings.
- Audit columns on `link` and `audit_log`: a separate mission-02 slice the orchestration lead is creating (A-10). Until it merges, the gap is a `docs/qa/GAPS.md` entry, which is the lead's to route.
- An HTTP endpoint or operator command to run the purge on demand.
- A purge metric (NFR-O3 is not allocated to this slice); the log event is the record.
- Data-subject deletion requests (`docs/REQUIREMENTS.md` §4).
- Changes to the click hook, the rate limiter or the redirect.

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Is the boundary day kept? | delete `≤ T−P`; delete `< T−P` | **assumed** delete `< T−P` (rule 2), so day `T−P` is kept. "Older than 90 days" then means at least 90 full days kept and never deleted early. Safe: errs toward the decided minimum, and the difference is one day. |
| A-2 | When does the purge run? | daily only; daily plus a startup run; continuously | **assumed** daily at a fixed UTC time, plus once at startup (rule 3). A service that is down at the scheduled time still purges when it comes back. Safe: idempotent deletion by day; the exact time is design. |
| A-3 | What does an invalid period do? | fall back to 90; refuse to start | **assumed** refuse to start (rule 1). A silent fallback would hide a misconfiguration, and `0` must never mean "delete everything". Safe: the Operator sees the error at deploy time. |
| A-4 | Is a purge audited? | an audit row per run; a log event | **assumed** a log event (rule 4). The audit trail records mutations of links by requests (mission 01 rule 9, with request id and actor), while a purge is a scheduled data-retention action with no request or actor. The log event carries the count and cutoff an Operator needs. Safe: an audit row can be added later if FR-17's readers ask for it. |
| A-5 | Does the purge change what statistics mean? | statistics keep lifetime totals; statistics cover retained clicks | **decided** statistics cover the retained clicks (rule 4, AC-5). The human decided delete, not aggregate ("purge stays here with the 90-day default"), and a lifetime total would need data kept in another form, which is the question parked in mission 03 (Q4). |
| A-6 | Should the purge cover idempotency bindings or other data? | clicks only; also expired bindings | **decided** clicks only. NFR-P2 is about click data, and other data has its own rules (NFR-R5 already stops honouring keys after 24 h). |
| A-7 | What reason token does a reduction failure get (W2-05)? | widen `rejected`; a distinct token | **assumed** a distinct static token (rule 7). The design names it (for example `reduction failed`). Safe: operator-facing vocabulary only; `rejected` keeps its documented meaning. |
| A-8 | Does mission 03's open retention question block this slice? | wait for its answer; build the decided delete | **decided** build the decided delete. The human approved this mission's purge with the 90-day default at 15:40Z. Mission 03's park offers keeping it as the recommended default. If the answer there is to aggregate (Q4 C), the lead adds that as mission-03 work, and this slice's period setting and day-based deletion stay usable underneath it. |

| A-9 | May the Operator pause the purge? Design review DR-01 needs it paused in the shared test contexts, and the lead asked for an operational reason (incident investigation, legal hold). | no pause, so shipped tests change; turn off the daily run only; pause all deletion, default on | **assumed** pause all deletion, with the purge running by default and a `WARN` at every start while paused (rule 3, AC-15). Safe: the shipped default keeps NFR-P2's job. The pause gives the Operator no new power over retention, because the period setting the human granted can already keep clicks as long as the Operator wants. It is never silent, and it keeps AC-14's shipped tests unchanged. The human sees it at ship sign-off and can reverse it there. |
| A-10 | Do the click tables get the audit columns now? | defer to a later slice; add them here | **decided** by the human, relayed by the operator (`qitem-20261003175330-fb054f2f`, 2026-10-03), and routed here by the orchestration lead: every table carries `created_at`/`updated_at` plus `created_by`/`updated_by` where an actor exists; an existing table gets them through an expand migration with a written rollback in the slice that next touches it. For `click` and `user_agent_class` that is this slice (AC-16). `link` and `audit_log` go to a separate slice. |
| A-11 | Who is the actor for click and user-agent class rows? | no `created_by`/`updated_by` (no actor); `anonymous` for clicks and `system` for seeded rows; a client-derived identity | **assumed** `anonymous` for click rows (the Visitor is unauthenticated, NFR-S6) and `system` for the seeded user-agent classes, the two principals the policy names. Safe: static tokens that hold no client identity (NFR-P1), and the columns exist either way. A client-derived value is ruled out because it would defeat the reduction. |

No question is parked on `human@kernel`. The purge, its default and the
setting are the human's recorded decision. Every other row is a narrow,
reversible default.

## Proof contract

- [ ] AC-1 through AC-16 are each covered by a named test (functional, or unit where noted by the design) and are green on the candidate SHA with `scripts/gw check`.
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional data (NFR-M1).
- [ ] Unit and functional JaCoCo reports committed under `docs/qa/coverage/02-click-retention/unit/` and `docs/qa/coverage/02-click-retention/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `02-click-retention` mapping AC-1 to AC-16 and rules 1 to 7 to their tests, with the requirement id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `02-click-retention` ("None for this slice" or each honest gap with its compensating check).
- [ ] `proof/` holds a by-effect capture from the running service started on a data directory that holds clicks beyond the period: click row counts per UTC day before start and after the startup purge, and the run's `INFO` log line (count, cutoff, period, no click values). This is a by-effect check of AC-7, AC-9 and AC-13 on the real service. The boundary (AC-1 to AC-3) and the daily schedule (AC-8) are proven by the suite-clock tests.
- [ ] The expand migration's header carries a written rollback that removes only what it added, and the review records that the rollback was read (NFR-X2).
- [ ] `proof/` holds a by-effect schema capture from the upgraded `f6dd29e` data directory: the columns of `click` and `user_agent_class` with type and nullability, and a sample of pre-existing and new rows' audit columns (AC-16, by effect).
- [ ] The candidate descends from `01-audit-read`'s merge commit (`git merge-base --is-ancestor`), and its Flyway number is the next after `01-audit-read`'s (ordered custody).
- [ ] The ADRs named under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them (NFR-M2).

## Source material

- `missions/02-brownfield/SPEC.md` (slice table, ordered custody, risks), `NOTES.md` §1 (plan-lock text, W2-05 input).
- `missions/02-brownfield/slices/02-click-retention/slice.yaml` (territory, design hint, custody).
- Shipped v1: `missions/01-greenfield-core/slices/02-analytics/SPEC.md` (rules 2, 5–7) and `design.md` §5 (`click lost` reasons); ADR-0011, ADR-0013; `src/main/resources/db/migration/V2__create_click.sql`.
- `docs/review/01-greenfield-core/wave-2-review-review-agent.md` (W2-05).
- `missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md` (Q4, parked).
- `docs/REQUIREMENTS.md`: NFR-P2, FR-13, FR-15, NFR-X2.

## Intent visual

N/A: non-visual slice.

## Status

- 2026-10-03: requirements written; 14 acceptance criteria, 7 business rules, 8 ambiguity rows (5 assumed, 3 decided, none parked). Handed to `requirements_review`.

- 2026-10-03: requirements review **PASS** on `ee7a4de` (`docs/review/02-click-retention/requirements-review.md`, evidence `735abe2`), with one MEDIUM finding, RQ-01. Fixed in passing (see *Review response*).

- 2026-10-03: AC-4 and rule 1 clarified during design (see *Review response*, D-AC4).
- 2026-10-03: design review FAIL (DR-01 HIGH, DR-02 MEDIUM, `docs/review/02-click-retention/design-review.md`). The requirements side is answered in *Review response*. Rule 3 now lets an Operator pause the purge through a documented setting, with a `WARN` at every start. The purge runs by default. New AC-15, new A-9, AC-14 names the one granted overlay line, and AC-4 is scoped to the failure report. Now 15 acceptance criteria and 9 ambiguity rows (6 assumed, 3 decided, none parked).
- 2026-10-03: the human's audit-column decision (`qitem-20261003175330-fb054f2f`, routed by the orchestration lead) added AC-16. It is an expand migration giving `click` and `user_agent_class` `created_at`/`updated_at`/`created_by`/`updated_by`, observable from the schema and stored rows. AC-13, NFR-X2, scope, territory and the proof contract now carry it. `link` and `audit_log` are out of scope (a separate slice). Now 16 acceptance criteria and 11 ambiguity rows (7 assumed, 4 decided, none parked).

## Review response

| Id | Severity | Response |
|---|---|---|
| RQ-01 | MEDIUM | **Fixed.** AC-6's verification redirect recorded a new click before the statistics read, so `totalClicks` `0` could not hold. The statistics are now read first (empty), then `GET /C` is opened, then after recording settles the statistics show that one click on day `T`. No other AC, rule or ambiguity row changed. |
| D-AC4 | design note | **Fixed.** "Names the setting without echoing anything else" could not hold: Spring Boot's startup failure report names the setting and repeats the operator's rejected value and where it came from (command line or environment variable), and nothing else (design probe rows A4, `design-probe/output.txt`). The rejected value is the operator's own input, not client data, and helps them fix it. AC-4 and rule 1 now say: the failure names the setting and the rejected value and echoes no other configuration value. The property may be printed as `retention-days` or `retentionDays` depending on the failure kind; both name the setting. No other AC, rule or ambiguity row changed. |
| DR-01 (design review) | HIGH, on the design | **Requirements adjusted to the design's fix.** A daily run at a real 00:10Z writes an `INFO` line with no request id (rule 6). That line can land inside the capture window of shipped journeys that require every captured line to carry one, so AC-14 failed. The design adds a setting that pauses the purge, which runs by default. The lead asked for an operational reason (incident investigation, legal hold), so the pause stops both the startup and the daily run. The functional profile overlay pauses it for the shared contexts, and the purge journeys turn it back on (probe `design-probe/output-5.txt`, A8off, measured the daily-only variant). Rule 3 now lets an Operator pause the purge as a documented choice, with a `WARN` at every start. New AC-15 proves the `WARN` and that nothing is deleted while paused. A-9 records why the pause is safe. AC-14 names the one overlay line with no test source change, granted by the lead at `cec7032`. Rule 6 is untouched: no false request id, and no purge event is suppressed. |
| DR-02 (design review) | MEDIUM, on the design | **Wording tightened.** AC-4's "no other configuration value" applies to the startup failure report. Lines the shipped startup writes before binding fails (the pool and Flyway log the datasource URL there today) are not part of AC-4. The failed start and the no-deletion clauses are unchanged. |

## Dependencies

- Mission 01 (merged): the click table and its stored UTC day (ADR-0013), the click recorder, the statistics endpoint.
- `01-audit-read` (same wave): first holder of `application.properties` and the next Flyway number. This slice's candidate descends from its merge.
- Mission 03 `01-analytics-v2` (parked on the human, Q4): if the human chooses to aggregate old clicks (Q4 C), the orchestration lead routes that as mission-03 work on top of this purge. If the human chooses a different number of days (Q4 B), it is this slice's setting.

## Self-check

- Every AC is observable: stored click rows (AC-1 to AC-3, AC-6, AC-7, AC-8, AC-10, AC-13), HTTP responses (AC-5, AC-6, AC-11, AC-12), log events (AC-9, AC-10, AC-12), startup outcome (AC-4), and the shipped suite (AC-14).
- Error paths are ACs: invalid setting (AC-4), failed run (AC-10), reduction failure (AC-12), redirect during purge (AC-11). Privacy: no click values in the purge's log events (AC-9, AC-10).
- Business rules cover the non-obvious logic: the boundary day, when runs happen, startup validation, what is and is not touched, statistics after a purge, no audit row, and the log content.
- Out of scope is explicit, including aggregation, which is pointed to the parked mission-03 question.
- Every allocated id has coverage: NFR-P2, FR-13, NFR-X2 (AC-13, AC-16), the audit-column policy (AC-16), FR-15 in passing, O1/O2 inherited, M1/M2 in the proof contract.
- Every ambiguity is resolved: 5 assumed with reasons, 3 decided, none parked, none widened.
- No design leaked beyond shipped facts. The table, column and migration names appear only where they cite shipped artifacts. The schedule time, batching and setting name are the design's.
- Consistent with the human's recorded decisions: delete, 90 days, an operator setting (15:40Z); the mission-03 coupling is stated, not pre-empted.
- `plan-review` lenses applied by the author while drafting (the skill was not invoked separately for this slice): the engineering lens added AC-7 (startup run, so a service down at the scheduled time still purges), AC-11 (redirect during a purge) and the dedicated-peer note. Strategy lens kept aggregation out (the human's open question). UX lens: the Analyst sees `200` with empty arrays for a fully purged link (AC-6), not `404`. No executive summary.
- DR-01/DR-02 amendment: AC-15 is observable by a startup log event and stored click rows on the suite clock. A-9 is safe because the purge runs by default, the pause is loud at every start, and the period setting already bounds the Operator's power. AC-14 still forbids test source changes; only one overlay line is named, granted at `cec7032`. AC-4 now asserts on the failure report only. Counts updated in the coverage table, *Non-functional*, the proof contract and the status. Not verified by me: the full-pause behaviour. Probe A8off measured the earlier daily-only variant, so the full pause (no startup deletion) is the design's and the tests' to prove.
- Audit-column amendment (A-10, A-11): AC-16 is observable from the schema and stored rows, and its privacy clause bans client values from the new columns. AC-13's "unchanged" now applies to the shipped columns, because pre-existing rows gain filled audit columns. NFR-X2 is no longer conditional. Checked on `main`: `ClickStore` and the unit `ClickSchemaTest` insert into `click` with the v1 column list, and no shipped test asserts the exact column set. So the new NOT NULL columns must be filled without those inserts naming them, or AC-14 breaks. How is the design's. The column names are the human's policy, not design leakage. Not verified by me: the backfill values for pre-existing rows (the design's, recorded in the migration header).
- Not verified by me: whether the scheduler can follow the suite clock (AC-8). The design either makes it do so or records the gap with the trigger-based test as the compensating check.
