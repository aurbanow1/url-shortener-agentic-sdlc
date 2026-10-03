# Mission 01 wave review — structure and drift (design agent, second vantage)

- Range: `7636264..8e9c065` on `main` (190 commits; product code changed only through the merges
  `16c355f`, `091ff46`, `8e9c065`).
- Reviewer: `design-agent@urlshort-factory` (Claude), 2026-10-03, item
  `qitem-20261003140048-4a960682` for packet `qitem-20261003135957-0f6e0c8b` (review-agent owns
  source and behaviour verification and the combined record).
- Vantage: final structure, cross-slice consistency, drift from the doghouse and from the locked
  designs. Product code was read, not changed. No runtime was started for this vantage.
- **Verdict: structure coherent, no drift from the doghouse; two MEDIUM, three LOW, two INFO.**
  Neither MEDIUM blocks the wave. W2D-02 (stale design documents) is fixed in this step. W2D-01
  (the API document's problem schema) goes to the lead's backlog.

## What I inspected

| Area | Read |
|---|---|
| Integration record | `docs/evidence/01-greenfield-core/wave-integration.md` (open items, decisions) |
| Intent | mission `SPEC.md` §The doghouse; the three slice SPECs at their locked SHAs (`0acbc9d`, `173bd60`, `f24f373`) |
| Designs and decisions | the three `design.md`, ADR-0002, 0004, 0011–0017, `docs/DESIGN.md`, NOTES §2 (DR-02 decision 09:30Z, clock policy 12:40Z) |
| Product, merged | `git diff --stat 7636264 8e9c065` (82 files); full reads of `click/ClickRecorder`, `click/DailySalt`, `click/ClickStore`, `web/RateLimiter`, `web/RateLimitFilter`, `web/OpenApiConfig`, `link/RedirectController` (the hook), `V2__create_click.sql`, `application.properties`, `compose.yaml`, `Dockerfile`; `@Order` of every project filter; `scripts/smoke.sh` `--drain` reconciliation; `FunctionalClock` |
| API document | `docs/api/openapi.json`: operations, response codes, component schemas |

## Structure

| Check | Result |
|---|---|
| Package boundaries | `link/` → `click/` is one call (`RedirectController.java:46`, `clicks.record(link.id(), request)`); `click/` reads `link` only by code → id (`ClickStore.findLinkId`); `web/` holds only cross-cutting HTTP concerns (request id, body limit, limiter, advice, API config); no `common/` or `util/`. Matches `docs/DESIGN.md` §2's layering rule |
| Filter chain | `RequestIdFilter` `HIGHEST_PRECEDENCE`, Boot's observation filter, `RateLimitFilter` `+2`, `RequestBodyLimitFilter` `+3`: distinct orders, no registration-order tie, as ADR-0014 requires |
| One problem shape | the advice's `createResponseEntity` and the limiter's `429` writer both produce `{instance: urn:uuid:<id>, status, title}`, no `detail` (`RateLimitFilter.java:76-82`) |
| One clock | every stored or decided instant comes from the application `Clock` (link, audit, `DailySalt.select`, `RateLimiter.now`); tests replace it once (`FunctionalClock`, with `freeze()` for the limiter journeys) |
| Logging privacy, layered | three framework categories that echo request data are raised in the shipped file (`PageNotFound`, `Http11Processor`, `ResourceHandlerUtils`); the limiter logs nothing; the click writer logs only `click lost` with a reason and an exception class; asynchronous events restore `requestId` (ADR-0004 amendments) |
| Shutdown budget | 10 s graceful phase (`application.properties`), then the click drain of at most 5 s (`ClickRecorder.DRAIN_DEADLINE`) and the pool close, inside compose's 20 s stop grace (`compose.yaml`). Consistent on paper; the timing is `release_prep`'s to measure |
| Schema | V1 + V2 as designed; the closed set is a lookup-table FK (H2 2.4.240 multi-value `CHECK` defect, `docs/DESIGN.md` §4) |
| Decisions in code | DR-02 boundary: the smoke `--drain` mode reconciles server `request completed` ids with client ids and reports boundary losses separately (`scripts/smoke.sh` ~232–250). Clock policy: `start = max(tat, now)` inside `compute`, the sweep rescheduled after a step of more than 2 s (`RateLimiter.java:74`, `:104`), as decided at 12:40Z |

## Drift from the doghouse

None. Each promise of the mission's doghouse is delivered:
- A Creator gets a short link and a Visitor a `302`.
- The click is recorded off the request thread.
- An Analyst reads the counts.
- An Operator gets `429`s, readiness that follows the database, Prometheus metrics, and an audit
  row for every mutation.
- It all runs in one container on one H2 file.

The out-of-scope moves (audit read and retention to mission 02, aliases and expiry dropped) are
recorded in the mission SPEC and nothing in the range builds them.

## Findings

| Id | Severity | Evidence | Finding | Disposition / repair |
|---|---|---|---|---|
| W2D-01 | MEDIUM | `docs/api/openapi.json` `components.schemas.ProblemDetail`; ADR-0002 amendment; `01-create-redirect` design §7.4 | Carry-over CR-01, and wider than recorded. The schema every problem response references describes Spring's `ProblemDetail` Java type, not the body on the wire. It lists a nested `properties` object (`additionalProperties: {}`) that no response carries, because extension members render top-level. It also has no `errors` member, though `400` and `422` carry `errors[{field, rule, message}]` (only the `400` description mentions it in prose). The committed document is NFR-M3's "one truth", and here it misdescribes the error contract | **JUDGMENT-GAP (mine).** My `01-create-redirect` design specified `@Schema(implementation = ProblemDetail.class)` and checked media types and examples (§7.4), not what springdoc renders for that type's getters. Repair, small: in `web.OpenApiConfig`'s existing customiser, replace the `ProblemDetail` component with the real shape (`type`, `title`, `status`, `detail`, `instance`, and an optional `errors` array of `{field, rule, message}`, no `properties`); `OpenApiDocumentTest` asserts `errors` present and `properties` absent; regenerate the document. Route: lead backlog, the next slice that holds `web/` and `docs/api/openapi.json` (mission 02) |
| W2D-02 | MEDIUM | `docs/DESIGN.md` preamble; `docs/adr/0004…` status and amendment headers; `docs/adr/0014…` | The system design still said wave 2 was "not yet merged". ADR-0004's second and third amendments still read "proposed" after both plan-locks. ADR-0014 lacked the lead's 12:40Z clock policy (backward step fails closed, sweep rescheduled after a step) and the testing rule that came with it; both lived only in `RateLimiter`'s Javadoc and NOTES | **JUDGMENT-GAP (mine):** I keep `docs/DESIGN.md` current at design time but had no step at merge time, and the clock policy was decided after my last touch of `03`. **Fixed in this step**: preamble, ADR-0004 status lines, ADR-0014 clock-policy amendment, `docs/DESIGN.md` §4 testing convention (commit below). No product change |
| W2D-03 | LOW | `ClickRecorder.java:86` (`getRemoteAddr()`); `RateLimitFilter.java:70` (`clientOf`); ADR-0015 *Consequences* | Carry-over A-9: two notions of "client". The limiter resolves a listed proxy's `X-Forwarded-For`; the click hash uses the peer. Identical under the shipped default (no trusted proxy), and mission 01 publishes on loopback with no proxy (ADR-0017), so nothing differs today and nothing reads the hash | Recorded backlog, not a miss. **Trigger:** before any deployment configures `urlshort.rate-limit.trusted-proxies`, or before FR-16 (mission 03) reads hashes, since clicks stored behind a proxy would all carry the proxy's hash. Mechanism named in ADR-0015: a request wrapper in `RateLimitFilter` overriding `getRemoteAddr()` |
| W2D-04 | LOW | QA `03-operate` LOW; `/actuator/prometheus` `disk_*{path=…}` | Boot's disk-space meter tags the working-directory path. It is not client data and AC-19 holds, but it is an installation detail on an anonymous endpoint, where health bodies deliberately show none (AC-15) | **JUDGMENT-GAP (mine, minor):** `03`'s threat row called Prometheus "operational, not personal" without listing Boot's default meters. Repair when `web/` or the actuator config is next touched: deny the disk meter (a `MeterFilter` or the Boot property, verified then). Loopback-only publish keeps it inside the Operator's scope meanwhile |
| W2D-05 | LOW | `ClickRecorder.java:92-93` | A failure while reducing a click (hashing, building the record) is reported `click lost` with reason `rejected`, which `02`'s design §5 defines as "full or closed queue". One WARN per lost click still holds | Implementation detail the design did not name. Repair in passing: reason `failed` (or widen §5's definition). Matters only if operators alert on `rejected` as overload |
| W2D-06 | INFO | NOTES §2 12:40Z; `IdempotencyJourneyTest`, `StatsJourneyTest` | Cross-slice test coupling: the shared suite clock and the limiter's per-peer state share a context, so a journey that moves the clock leaves its peer's bucket in the future | Recorded as a testing convention in `docs/DESIGN.md` §4 and ADR-0014's amendment, so mission 02's slices inherit it |
| W2D-07 | INFO | `docs/qa/03-operate/findings.md` | `smoke.sh` needs a supported C locale on macOS | QA's LOW; no structural consequence |

Carry-over decisions checked against the code: the DR-02 shutdown boundary (dispatched requests
judged, backlog losses counted and reported) is implemented by the smoke `--drain` reconciliation.
The 12:40Z clock policy is implemented and is now in ADR-0014.

## Not verified by this vantage

- Runtime behaviour, coverage and the gate: review-agent's part, and the integration record's gate
  log (`unit 165, functional 155`).
- The container, the shutdown timing and the bench: `release_prep`.
- Mission 02's eventual repair of W2D-01, W2D-04 and W2D-05.

## Self-check

- The range and the three merges were confirmed with `git log`; every finding cites a file and
  line or a document section.
- The structure was checked against the layering rule, the filter order, the problem shape, the
  clock, the logging layers, the shutdown budget and the schema.
- The doghouse was checked line by line.
- The carry-overs were dispositioned: CR-01 → W2D-01, A-9 → W2D-03, the QA LOWs → W2D-04 and
  W2D-07, and both recorded decisions checked in code.
- My own misses are labelled JUDGMENT-GAP (W2D-01, W2D-02, W2D-04). None is a CONTEXT-GAP: each
  fact was available to me at design time.
- Edits stayed in my documents (`docs/DESIGN.md`, ADR-0004 status lines, ADR-0014 amendment) and
  this file. The shared ledger was not edited and no product file changed.
