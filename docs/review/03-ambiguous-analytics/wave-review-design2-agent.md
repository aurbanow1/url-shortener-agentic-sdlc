# Mission 03 wave review: structure and drift (second vantage, independent of the design)

Range `d55a502..94aa2c0`, which holds one slice, `01-analytics-v2`. Its product merge is `c9b66dd`
(second parent X′ `22fc8e2`, the judged `ec466da` rebased). The rest of the range is evidence and
coordination for both missions. Asked by the orchestration lead on `qitem-20261004002726-048a1660`
and `docs/review/03-ambiguous-analytics/wave-1-structure-request.md`. The primary vantage is
`review2-agent`'s (`qitem-20261004002516-ee95930d`). Author: `design2-agent@urlshort-factory`,
2026-10-04.

**Independence.** I did not write the SPEC, the design, the code or any review of this slice.
`design-agent` designed it end to end. My only design in flight today was mission 02's `05-ci-cd`,
which is not in this range.

**Verdict: nothing blocks the wave.**
- No MUST-FIX, HIGH or MEDIUM finding.
- The merged code matches the locked design (`80ca44c`), the ADR-0011, -0012, -0013, -0015 and
  -0016 amendments, and the human's six letters (Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A, transition 876).
  I found no behavioural drift between the SPEC, the design and the code.
- Two LOW findings, both one-line documentation repairs: `totalClicks` is described as a lifetime
  figure (M3S-01, CONTEXT-GAP), and the trusted-proxy setting's comment omits its new effect
  (M3S-02, JUDGMENT-GAP).
- Three INFO items.
- The register's final analytics-v2 lines (`b45029b`) and the status refresh (`15654b7`), written by
  `design-agent` as register upkeep, were judged independently against the merged code. I agree
  with each, and two rows need one more sentence (§4).

## What I inspected

| Area | Read |
|---|---|
| Decision and requirements | slice `SPEC.md` (*Decision*, AC-1 to AC-15, rules 1 to 11, A-1 to A-9); mission `SPEC.md` (risks, cross-mission coupling) |
| Locked set | `design.md` and `impact-analysis.md`. `git diff 80ca44c 94aa2c0` on both is empty; after the lock only the SPEC's approval stamp (`c764644`) changed |
| Decisions | ADR-0011, ADR-0012, ADR-0013 (amendment), ADR-0015 (amendment), ADR-0016 (amendment) at `94aa2c0` |
| Product, merged | `git diff d55a502 94aa2c0 -- src/main`: `ClickRecorder`, `ClickStore`, `LinkStats`, `StatsController`, `click/package-info`, `web/RateLimitFilter`. `docs/api/openapi.json` diff |
| Tests touched in place | diffs of `RateLimitFilterTest`, `AuditUpgradeJourneyTest`, `ClickRecordingJourneyTest`, `ClickRetentionJourneyTest`, `StatsJourneyTest`. The new classes I located but did not judge line by line; that is the primary vantage's |
| Grants and rulings | `slice.yaml`: the `web/` pair (18:36Z), the `AuditUpgradeJourneyTest` body (21:57Z) and the AC-14 reading (`a12a0e2`, 23:30Z); `PROOF.md` *Deviations* |
| V3, V4 and the purge | migrations present at `d55a502` (V1 to V4); `ClickStore`'s insert and statistics statements; ADR-0018; the purge's cutoff rule |
| Gate | `docs/evidence/03-ambiguous-analytics/integrate-01-analytics-v2-check-22fc8e2.txt` (`BUILD SUCCESSFUL`, 14 of 14 tasks executed) and its integrate record in `PROGRESS.md` (`18db1de`). `git diff --stat 22fc8e2 94aa2c0 -- src build.gradle.kts settings.gradle.kts gradle Dockerfile compose.yaml scripts` is empty, so the gated product tree is the accepted one. **I ran no build myself** |
| Register and design documents | `docs/guidance/architecture.md` §11 at `94aa2c0`, `ea84e77` (current at `d55a502`, analytics-v2 still *(pending merge)*) and `b45029b` (analytics-v2 lines final). `15654b7`: `docs/DESIGN.md` and the ADR-0013, -0015 and -0016 status lines after the merge |

## 1. Structure and coherence: the merged code against the decision, the design and the ADRs

| Check | Result |
|---|---|
| Q1 B, uniques per UTC day, never combined | `LinkStats` keeps four top-level fields; `DayClicks` is `date, clicks, uniqueVisitors, botClicks`, in that order. No top-level or cross-day unique figure exists |
| Q2 B, the hash only for same-day distinct counts | `grep -rn client_hash src/main` finds V2's column and check, the insert, and the one `COUNT(DISTINCT client_hash) … GROUP BY clicked_on`. No response, metric or log reads it. `DailySalt` is unchanged, so `clicked_on` is still the salt's day |
| Q3 B, bots counted and kept in every figure | `SUM(CASE WHEN user_agent_class = 'bot' …)` beside `COUNT(*)`; `clicks`, `totalClicks` and `topReferrers` come from the same rows as in v1 |
| Q4 A, nothing built for retention | no migration and no setting in the range (`git diff --stat`) |
| Q5 A, the existing API only | the document diff touches only `DayClicks` (two properties with descriptions), the statistics example and the `200` description. No path or operation is added |
| Q6 A, UTC days | no zone parameter or setting; `clicked_on` is computed in Java as before (ADR-0013) |
| Design §1 `RateLimitFilter` (grant) | becomes `public`; `CLIENT_ATTRIBUTE` is `RateLimitFilter.class.getName() + ".client"`, set to `clientOf(…)`'s result after the exempt check and before `tryTake`. `clientOf` stays package-private, narrower than the grant allowed. Decisions unchanged |
| Design §1 `ClickRecorder` | address = the attribute if it is a `String`, else `getRemoteAddr()`. Both counters are registered in the constructor, every `reason` up front. `recorded` increments as soon as `store.insert` returns, before the state change, as the design review asked. `lost(…)` is now an instance method, and every path (request thread, writer, `reportAtShutdown`) goes through it, so each `click lost` WARN has exactly one counter increment. The reason strings are constants shared by the log and the map |
| Design §1 `ClickStore`, ADR-0013 amendment | the `UNION ALL` statement is the amendment's, verbatim. `Stats` splits the rows by `unique_visitors` being null |
| Design §1 `LinkStats` | the referrer fold is v1's; each day's element takes its figures from the day row. The zero fallback is gone, as the design says |
| ADR-0015 amendment | `getRemoteAddr()` is not rewritten. `RateLimitFilterTest`'s new case asserts it ("the peer address is never rewritten") and that an exempt request carries no attribute |
| ADR-0016 amendment | `urlshort.clicks.recorded` and `urlshort.clicks.lost{reason}` with the five static reasons; nothing else is tagged |
| ADR-0011 | the writer, the queue, the drain and the claim logic are unchanged; only the counters are added |
| Grant: the `web/` pair | only `RateLimitFilter.java` and `RateLimitFilterTest.java` change under `web/`. The test diff adds one case and changes no existing one. Both conditions hold |
| Grant: `AuditUpgradeJourneyTest` | one line: the single `clicksPerDay` element gains `"uniqueVisitors":1,"botClicks":0`. Nothing else changes |
| AC-14 shape updates | `StatsJourneyTest.AC09` (one assertion), `ClickRecordingJourneyTest` (one), `AuditUpgradeJourneyTest` (one, granted), and `ClickRetentionJourneyTest` (three). Each diff only adds the two fields. The retention test's three are outside the impact analysis's list because that test merged after it was written. `PROOF.md` names them under *Deviations*, which is the honest record |
| AC-14 reading `a12a0e2` | agreed. The two excused `OpenApiDocumentTest` enumerations (path list, operation count) were changed by `01-audit-read` under its own grant. This slice adds no path or operation (document diff above) and does not touch that test |
| V3 and V4 | the base `d55a502` already has V1 to V4, so the slice was built and gated on them. The click insert's column list is unchanged, and V3's audit columns fill from their defaults. The statistics statement names its columns, so V3's new ones change nothing. V4 changes `link` and `audit_log` only; `SELECT id FROM link WHERE code = :code` is unaffected. `AuditUpgradeJourneyTest` custody with `04-audit-columns` held: that slice merged first, and this one's one line sits on top |
| Beside the purge (ADR-0018) | the purge deletes `clicked_on < today − P` on the same application clock that stamps `clicked_on`. It removes whole UTC days, so a day's `uniqueVisitors` and `botClicks` are either complete or gone, never partial. The figures cover a rolling window, as SPEC rule 7 says; see M3S-01 for where the contract does not say so |

**Drift between the SPEC, the design and the code:** none in behaviour. The test-side deviations
(dedicated peers for AC-3 and AC-8, AC-9 and AC-12 in the trusted-proxy class, a new AC-13 test)
are named in `PROOF.md` and change no criterion.

## 2. The design itself

- **The request attribute shared with the rate limiter: sound.**
  - It is the right choice over ADR-0015's wrapper sketch. The connection address stays true for
    the audit read's guard (ADR-0019) and for Boot's observation. `X-Forwarded-For` still has one
    parser. One operator setting governs both the limiter and the analytics, as SPEC A-7 asked.
  - Its cost is a coupling: the click's identity exists only because the limiter ran. `click/` now
    imports a concrete filter class made public for one constant, and the fallback to the peer is
    silent (M3S-03).
- **The single `UNION ALL` statistics read: sound.**
  - One statement gives one snapshot. The design review measured that under H2's `READ_COMMITTED`,
    and PostgreSQL's statement snapshot gives the same.
  - It is two plain grouped range scans, with its cost measured on a 90-day hot link (S3, S5).
  - The fold now relies on the single statement: there is no fallback for a missing day row
    (M3S-04).
- **The two click counters: sound.**
  - `recorded` counts every insert that returned. A click whose report shutdown claimed can count
    in both `recorded` and `lost`, which is the truth for it, and both the ADR and the class
    Javadoc say so.
  - `lost` cannot drift from the WARN line, because both happen in one method.
  - The counter map is a `final` field filled in the constructor, so it is safely published to the
    writer and request threads.
  - The `reason` tag cannot carry client data: the vocabulary is five constants.
- **Beside mission 02.**
  - The purge and the per-day figures share the clock and the day boundary, which keeps per-day
    uniques exact for every retained day.
  - No migration was needed, so the Flyway head stays at V4, and custody of `openapi.json` and
    `AuditUpgradeJourneyTest` ran in the order the lead set.
  - The one gap is the meaning of `totalClicks`, which the purge changed and no contract text yet
    states (M3S-01).

## 3. Findings

| Id | Severity | Class | Evidence | Finding | Repair and route |
|---|---|---|---|---|---|
| M3S-01 | LOW | CONTEXT-GAP | `LinkStats.java:19` (`@param totalClicks every click recorded for the link`, from `fe9e155`, mission 01); SPEC rule 7 ("there is no lifetime total"); ADR-0018; `docs/api/openapi.json` (`totalClicks` has no description) | Since `02-click-retention` merged (`ed2b940`), `totalClicks` and every other figure cover only the clicks still stored: a rolling 90-day window by default. The class's contract text still describes a lifetime figure. The API document, which this slice regenerated, gives the field no description, so an Analyst cannot learn it there. The SPEC states the fact (rule 7) but no criterion asks the contract to carry it, and the text went stale in mission 02, before this slice | One Javadoc line and a `@Schema(description = …)` on `totalClicks` ("clicks still stored: the retention period, 90 days by default"), regenerating the document. Route: lead backlog, for the next slice that holds `click/` and `openapi.json` |
| M3S-02 | LOW | JUDGMENT-GAP (a design call; the design is `design-agent`'s) | `application.properties:6–8` (the trusted-proxy comment speaks of the rate limit only); SPEC rule 6, A-7 and the Operator story ("unique visitors stay truthful behind the proxies I trust"); register row *Operator settings* ("a comment saying why an Operator changes it") | Since `c9b66dd`, `urlshort.rate-limit.trusted-proxies` also decides whose address is hashed for unique visitors. The SPEC said so; the design did not ask for the one-line `application.properties` grant that `01-audit-read` and `02-click-retention` asked for in mission 02. An Operator reading the shipped file learns only the rate-limit effect. `docs/DESIGN.md` §3 carries it, but under a *(designed)* marker | One comment line, for example "also the client counted in unique visitors (ADR-0015 amendment)". No rename: the key is operator contract. Route: lead backlog, for the next holder of `application.properties` |
| M3S-03 | INFO | — | `ClickRecorder.record` (attribute, else `getRemoteAddr()`); `RateLimitFilter` exemptions; `TrustedProxyClickJourneyTest` (AC-7) | The click's client exists only because the limiter ran first. A future change that lets a redirect skip `RateLimitFilter` (an exempt path, an off switch, a filter reorder) would silently revert uniques behind a proxy to the proxy's own address. Nothing would fail except AC-7's journey, and that only in its trusted-proxy context | Add one sentence to the register's *Client identity* rule: a change that lets a request skip the limiter must still set `CLIENT_ATTRIBUTE`. Route: `design-agent`, register owner |
| M3S-04 | INFO | — | `LinkStats.of` (`stats.days().get(day)` with no null branch); ADR-0013 amendment ("one statement … one snapshot") | The fold is correct only while both figures come from one statement. A later change that splits it into two statements would turn a concurrent insert or purge into a `NullPointerException` (a bare `500`), not wrong numbers. That fails loudly, and the ADR states the single statement | Record only. A design that splits the statement restores a fallback and amends ADR-0013 |
| M3S-05 | INFO | — | `integrate-01-analytics-v2-check-22fc8e2.txt` (no SHA inside); `PROGRESS.md` integrate record (`18db1de`: "Merged X′ … at `94aa2c0`") | Evidence hygiene. The gate log names X′ in its file name and the record names `main`'s tip, while the merge commit is `c9b66dd`, and the log itself carries no `git rev-parse HEAD`. I checked that the product trees of `22fc8e2` and `94aa2c0` are identical, so the result stands; a reader without that check cannot tie the log to a commit | Have the gate capture print the SHA it ran on (`scripts/gw --log` could write `git rev-parse HEAD` as its first line). Route: lead |

**Documentation currency after the merge: closed, not a finding.** At `94aa2c0`, these still
described `01-analytics-v2` as unmerged:
- `docs/DESIGN.md` had five "01-analytics-v2 (designed)" markers;
- the status lines of ADR-0013, ADR-0015 and ADR-0016 said "not merged yet";
- the register rows carried *(pending merge)*.

The lead filed the refresh at integrate (`qitem-20261004002550-2db4c1ae` to `design-agent`), the
process repair mission 02's W2P-03 asked for, and it landed while I wrote. `15654b7` leaves no
analytics-v2 *(designed)* marker in `DESIGN.md` (`grep -c`: 0), and each status line now reads
"merged in `c9b66dd`". `b45029b` finalises the register lines (§4). W2D-02/W2P-03 did not recur.

## 4. Register walk (D20), one line per concern

**What I judged.** The register as `design-agent` finalised it for this slice in `b45029b`
("verified against `main` at `94aa2c0`"). `design-agent` designed the slice and wrote these lines as
register upkeep from the merged code, and asked me to judge them independently. I checked each
analytics-v2 claim against the code and the diffs of §1, not against the design. Where I would add
a sentence, the row says so; those edits are `design-agent`'s, and I made none.

| Concern | Line |
|---|---|
| Client identity and proxy trust | **Consistent; I agree with the final line.** Checked against the code:<br>- `clientOf` has one caller (`grep -rn "clientOf(" src/main`: `RateLimitFilter:76` and its declaration);<br>- the attribute is set on every non-exempt request before charging, and `ClickRecorder` falls back to the peer only without it;<br>- `getRemoteAddr()` is not rewritten (asserted in `RateLimitFilterTest`).<br>The closed drift sentence (W2-02, W2D-03) is gone, and the rule is in the present tense. **One sentence to add (M3S-03):** a change that lets a request skip the limiter must still set `CLIENT_ATTRIBUTE` |
| Time | **Consistent; I agree.** Uniques group by `clicked_on`, the salt's day; `DailySalt.stamp` is unchanged. The purge cuts whole days on the same clock, so per-day figures are never partial. Optional wording: besides a restart, ADR-0012's clock stepping back across midnight is a second source of the upper bound. The `PingController` drift is carried |
| Schema change | **Consistent.** This slice takes no migration; `main` stays at V4. The V4 pin drift (W2F-01) is carried, untouched here, and bites only at V5 |
| Audit columns | **Consistent.** The click insert is unchanged and V3's defaults fill its audit columns; nothing in the slice sets them |
| Error shape | **Consistent; I agree with the final line.** No new error path; `StatsController` still answers `Problems.notFound`, and the new figures appear only in the `200` |
| Request id and logging | **Consistent; I agree with the final line.** No new event. Every `click lost` WARN is paired with exactly one increment of the same `reason`, because both happen in `lost(…)`, which every path calls (`reportAtShutdown` included). The shutdown reports still restore `requestId` |
| Audit trail writes | **Consistent.** A statistics read writes nothing |
| Client hashing for analytics | **Consistent; I agree with the final line.** The hash is read only in the statistics statement (`grep`: insert and `COUNT(DISTINCT …)` only), and reaches no response, log or metric; its input is the limiter's client |
| Metrics and health exposure | **Consistent; I agree with the final line.** The five reasons it lists are the five constants in `ClickRecorder`; each is registered at zero; `reason` is the only tag; `MetricsConfig`'s `ignoreTags("path")` does not touch them |
| API document | **Consistent.** Ordered custody held: the base already had `01-audit-read`'s and `03-dogfood-fix`'s changes, and this slice regenerated on top. The diff is the per-day schema, the example and one description. M3S-01 is the one missing description |
| CI/CD | **Consistent.** No workflow change. The new journeys run inside `gate`'s `./gradlew check`; `ClickMetricsJourneyTest` carries `@AutoConfigureMetrics`, without which the test scrape answered `404` (`PROOF.md`). Not observed on GitHub |
| Background work | **Consistent.** No new job. The writer's drain, interrupt and claim are unchanged; the two shutdown reasons are now counted |
| Operator settings | **Drift, one new and one carried.** No new setting, but `urlshort.rate-limit.trusted-proxies` gained an effect its shipped comment does not state (M3S-02). The row's carried drift (`URLSHORT_CLICK_PURGEENABLED` missing from `README.md`, mission 02's W2P-01) is still true. **One sentence to add:** M3S-02 beside W2P-01 under *Drift on `main`* |

**Added after `7c54ef7`.** `design-agent` added both sentences in `41eff65`, and I checked its
diff:
- the *Client identity* rule now says that a change that lets a redirect skip `RateLimitFilter`
  (a new exempt path, a filter reorder, a second entry point) must still set `CLIENT_ATTRIBUTE`
  (M3S-03);
- the *Operator settings* drift names `trusted-proxies` and its rate-limit-only comment (M3S-02).

The register has no open item from this walk. M3S-01 and M3S-02 remain code and comment repairs
for the lead's backlog.

## Not verified by this vantage

- No build, test or HTTP run of my own on `94aa2c0`. I relied on the integrate gate log (14 of 14
  tasks, `BUILD SUCCESSFUL`), with the product-tree identity checked by `git diff`, and on QA's
  by-effect captures as recorded in `PROGRESS.md` and `PROOF.md`.
- The new test classes beyond locating them; whether they prove each AC is the primary vantage's.
- The GitHub-hosted runs of `gate` and `package` on this merge.
- NFR-L1's percentiles: proof item 12, judged after `release_prep` under `a12a0e2` and
  `qitem-20261003195138-8eb72ecb`, by design; I did not treat it as open here.
- The rest of `15654b7`'s `DESIGN.md` wording beyond the markers and status lines.

## Self-check

- Independence stated: no part of this slice is mine.
- Each of the human's six letters and each design component has a line tied to code or a diff
  (§1).
- Every grant condition was checked against its diff. The AC-14 shape updates were counted against
  the impact analysis and `PROOF.md`. The AC-14 reading was checked against the document diff.
- V3, V4 and the purge were judged by reading, and stated as such.
- Each finding has evidence, a severity, a class and a route. Nothing here edits product code, the
  register or `REVIEW-LEDGER.md`.
- All thirteen register concerns have one line. The final analytics-v2 lines (`b45029b`), written
  by the slice's designer, were judged against the code rather than on their author's word, and the
  two sentences still missing are named.
