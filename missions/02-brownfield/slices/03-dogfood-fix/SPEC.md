---
id: OPR.99.0.3.3
slice: 03-dogfood-fix
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "A defect found by using the shipped service is fixed with a regression test written first, and the tests and documentation it touches are brought in line with the shipped behaviour."
depends_on: []
---

# Slice 03 — Dogfood Fix

## Intent

A defect found by using the shipped service is fixed with a regression test written first, and the tests and documentation it touches are brought in line with the shipped behaviour.

The defect is real, not seeded. QA's dogfood pass on the packaged jar
(`docs/qa/01-greenfield-core/dogfood.md`, `305dce5`) reproduced it twice
(**W2-01**, MEDIUM): when a Creator's request is invalid, the service answers
a problem detail with a top-level `errors` array (`field`, `rule`, `message`).
But the published API document's `ProblemDetail` schema has no `errors`
member, and describes a nested `properties` object the service never sends. A
client generated from the document cannot read why its request was refused.
The same pass found one LOW item (**W2-03**): the anonymous Prometheus scrape
tags the disk gauges with the service's working-directory path. It is already
a retained gap (`docs/qa/GAPS.md`, QA-OPR-02). This slice fixes it in passing
(A-1). No other severe defect was found.

## Mini-requirements

### Requirements covered

| Id | Requirement (short) | Proven by |
|---|---|---|
| FR-14 | a defect found by using the shipped service is fixed with a regression test written first | AC-1, AC-2, AC-5, AC-6 |
| FR-15 | test and documentation improvements the fix touches: `GAPS.md` rows closed or re-justified; `README`/`docs/DESIGN.md` match shipped behaviour | AC-7, AC-8 |
| FR-13 | existing behaviour unchanged (impact analysis) | AC-3, AC-4, AC-9 |
| NFR-M3 (inherited) | the committed API document equals the live one | AC-3 |
| NFR-M1, M2 (cross-cutting) | coverage gate; ADR before dependent code | proof contract; *Non-functional* |

NFR-X2 does not apply: neither fix touches the schema.

### Personas

Verbatim from `docs/REQUIREMENTS.md` §1.

- **Primary:** Creator (an API client that reads the API document to handle refusals).
- **Secondary:** Operator (scrapes metrics anonymously; reads the documentation).

### User stories

- As a Creator, I want the API document to describe the `errors` array the service sends with a refusal, so that my client can show which field failed which rule.
- As a Creator, I want the API document to stop describing a member the service never sends, so that my generated client has no dead field.
- As an Operator, I want the anonymous metrics scrape to carry no filesystem path, so that it discloses nothing about the installation, like the health endpoints.
- As an Operator, I want the documentation and the gaps list to match what ships, so that I can trust them.

### Acceptance criteria

"The live document" is `GET /v3/api-docs` on the candidate. "The committed
document" is `docs/api/openapi.json`. Problem bodies are the shipped RFC 9457
contract (`01-create-redirect` rule 8; ADR-0002).

#### W2-01: the problem schema matches the wire

- **AC-1 — The problem schema documents `errors`.** [FR-14]
  GIVEN the live document
  WHEN `components.schemas.ProblemDetail` is read
  THEN it has an optional top-level member `errors`: an array whose items are objects with exactly the required string members `field`, `rule` and `message`. It has no member `properties`. Its other members (`type`, `title`, `status`, `detail`, `instance`) are as before.

- **AC-2 — Every shipped problem body conforms to the documented schema.** [FR-14]
  GIVEN the live document's `ProblemDetail` schema
  WHEN the suite sends a create with an invalid URL (`400`), a create whose idempotency key is bound to a different URL within its window (`422`), a read of an unknown code (`404`), a redirect to a retired link (`410`), a request over the create budget (`429`), and a malformed `GET /api/audit` query (`400`, the audit read merged from `01-audit-read`)
  THEN each response body validates against that schema. Each body's members are all described by it. The `400` and `422` bodies carry `errors` with one element each, and the others carry no `errors`.

- **AC-3 — The committed document equals the live one, and only the problem schema changed.** [NFR-M3, FR-13]
  GIVEN the committed document on the candidate and on the merged `main` it starts from
  WHEN both are compared with the live document (key-sorted)
  THEN the committed document equals the live one. Its difference from the merged `main`'s committed document is confined to `components.schemas.ProblemDetail` (and any schema it adds for an `errors` item). Every path, operation, response, header and example is unchanged.

- **AC-4 — The service's responses are unchanged.** [FR-13]
  GIVEN the requests of AC-2 sent to the merged `main` and to the candidate
  WHEN their responses are compared
  THEN status, content type and every body member are identical, apart from per-request values (`instance`, `X-Request-Id`). The fix changes the document, not the wire.

- **AC-5 — The W2-01 regression test was written first.** [FR-14]
  GIVEN the slice branch's history
  WHEN the commit that adds the AC-1/AC-2 assertions is checked out without the fix and its tests run
  THEN those assertions fail with a message naming the missing `errors` member or the extra `properties` member; and on the candidate they pass. The failing run is captured.

#### W2-03: no installation path in the anonymous scrape (in passing)

- **AC-6 — Disk gauges carry no path, and the regression test came first.** [FR-14, FR-15]
  GIVEN the candidate running from a working directory with a known absolute path
  WHEN an anonymous client reads `GET /actuator/prometheus` and `GET /actuator/metrics/disk.free`
  THEN no sample of the scrape, and no available tag of the metric, carries a `path` tag or contains that working-directory path. The disk free and total gauges are still present with a value. As in AC-5, the assertion is committed before the fix and its failing run is captured.

#### Documentation and gaps

- **AC-7 — The gaps list matches what ships.** [FR-15]
  GIVEN `docs/qa/GAPS.md` on the candidate's acceptance
  WHEN its rows about the API document's problem schema and the disk-gauge path are read
  THEN the `03-operate` row "Anonymous disk-gauge working-directory path" (QA-OPR-02) is closed with this slice's evidence. No row describes W2-01 as open.

- **AC-8 — The design document matches what ships.** [FR-15]
  GIVEN `docs/DESIGN.md` on the candidate
  WHEN its description of problem details and of the metrics surface is read
  THEN it states that the API document's problem schema includes the optional `errors` array, and that disk gauges are exported without a path. `README.md` is checked for statements about either. Any that disagree with the candidate are corrected, and the check is recorded even when nothing changes.

#### Unchanged

- **AC-9 — The shipped suite passes.** [FR-13]
  GIVEN the unit and functional suites on the merged `main` this slice starts from
  WHEN they run against the candidate
  THEN every test passes. The only shipped tests changed are the two that receive the regression assertions (`OpenApiDocumentTest`, `HealthMetricsJourneyTest`), and those changes only add assertions.

### Business rules

1. **Test first, then fix.** Each defect's regression assertion is committed and seen failing on the unfixed code before the fix is committed (AC-5, AC-6). A fix commit without a preceding failing test does not meet FR-14.
2. **The wire is the truth for W2-01.** The service's problem bodies are the shipped contract, proven by `01-create-redirect`'s tests and the dogfood pass. The document is corrected to match the service, never the reverse. `errors` is optional because only validation (`400`) and idempotency-mismatch (`422`) problems carry it.
3. **One schema for every problem.** Every operation's problem responses reference the one `ProblemDetail` schema, so the correction applies to all of them at once, the audit read's included.
4. **No installation details on anonymous surfaces.** This extends `03-operate`'s rule 9 for health bodies to the Prometheus scrape and the metrics endpoint: a filesystem path is an installation detail. The disk gauges stay, because their values are what an Operator needs.
5. **Smallest fix.** Documentation metadata and one meter rule. No change to how problems are produced, to any endpoint, to the rate limiter, to `click/`, `link/`, `audit/` or the schema.

### Non-functional

- **Brownfield (FR-13).** The design writes `impact-analysis.md` over the code the two fixes touch before `design.md`. It re-checks it against the merged `main` at plan-lock, because `01-audit-read` regenerates `docs/api/openapi.json` and adds `GET /api/audit`'s problems.
- **ADR (NFR-M2).** An amendment to ADR-0010 (committed API document) or ADR-0002 (problem details) for the documented `errors` member, and to ADR-0016 (metrics exposure) for the dropped tag. The design says which.
- **Coverage gate (NFR-M1).** `scripts/gw check` with 100 % line and branch coverage on merged unit and functional data; honest gaps in `docs/qa/GAPS.md`.
- **Territory**, named for the lead to adopt with `rig workflow revise`:
  - `src/main/java/dev/urlshort/web/OpenApiConfig.java` (the schema correction);
  - `src/main/java/dev/urlshort/web/Problems.java`, documentation annotations only, if the design documents `errors` there; its runtime behaviour must not change (AC-4);
  - `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` (W2-01 regression assertions, added only);
  - `docs/api/openapi.json` (regenerated);
  - for W2-03, exactly one of: `src/main/resources/application.properties` (one setting, if a property drops the tag), or one new configuration class under `src/main/java/dev/urlshort/web/` with its unit test under `src/test/java/dev/urlshort/web/` (the design names both files);
  - `src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java` (W2-03 regression assertions, added only);
  - `docs/DESIGN.md` and `README.md` (AC-8).
  - **Not:** `web/RateLimitFilter.java` and its unit test (granted to mission 03's `01-analytics-v2`, `c78500e`), `click/`, `link/`, `audit/`, `db/migration/`. `docs/qa/GAPS.md` belongs to QA (AC-7).

### Scope

**In scope**

- W2-01: the API document's `ProblemDetail` schema corrected to match the problem bodies the service sends, with a regression test written first.
- W2-03: the disk gauges' path tag removed from the anonymous metrics surfaces, with a regression test written first.
- The documentation and gaps rows these fixes touch.

**Explicitly out of scope**

- Any change to the problem bodies themselves, to their producers, or to which requests produce them.
- Other dogfood observations, which are not defects: the accepted concurrent same-key boundary (`01-create-redirect` rule 5, ADR-0007), and Swagger UI rendering (reachable, rendering untested).
- Disabling or restricting the metrics surface beyond the path tag (a separate operator decision, ADR-0016).
- A schema migration (NFR-X2 not triggered).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Fix W2-03 (LOW) in passing or defer it? | fix in passing; defer to backlog | **assumed** fix in passing (AC-6, rule 4). It is small and already a retained `GAPS.md` row that FR-15 asks this slice to close or re-justify. It follows the shipped "no installation details" rule for health. Its territory is disjoint from the other w2 slices. Safe: no client-visible change. The gauges keep their values. |
| A-2 | Correct the document to the wire, or the wire to the document? | document to wire; wire to document | **decided** document to wire (rule 2). The `errors` array is the shipped, tested contract (`01-create-redirect`), and the `properties` member is a serialisation artefact the service never sends. Changing the wire would break clients for no gain. |
| A-3 | Is `errors` required or optional in the schema? | required; optional | **decided** by the shipped behaviour: optional. Only `400` validation and `422` mismatch problems carry it (dogfood exchanges; `Problems`), and one schema serves every problem (rule 3). |
| A-4 | Keep the disk gauges without the path, or remove them? | drop the tag; remove the gauges | **assumed** drop the tag, keep the gauges (AC-6). Free disk space is an operational signal the Operator uses for a file database. Safe: the smallest change that removes the disclosure. |
| A-5 | Does the audit read's `400` (merged from `01-audit-read`) need its own check? | yes; it inherits the schema | **assumed** yes, it is one of AC-2's requests. It references the same schema (rule 3), and checking it costs one request. Safe: no new behaviour. |

No question is parked on `human@kernel`. The defect, its severity and its
evidence come from QA's dogfood report. Every row is a narrow, reversible
default.

## Proof contract

- [ ] AC-1 to AC-9 are each covered by a named test or a recorded check, green on the candidate SHA with `scripts/gw check`.
- [ ] `proof/` holds both regression tests' failing runs on the unfixed code, with the commit SHAs that show test before fix (AC-5, AC-6; FR-14).
- [ ] `scripts/gw check` reports 100 % line and 100 % branch coverage on the merged unit and functional data (NFR-M1).
- [ ] Unit and functional JaCoCo reports committed under `docs/qa/coverage/03-dogfood-fix/unit/` and `docs/qa/coverage/03-dogfood-fix/functional/`.
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `03-dogfood-fix` mapping AC-1 to AC-9 and rules 1 to 5 to their tests or checks, with the requirement id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `03-dogfood-fix` ("None for this slice" or each honest gap), and the QA-OPR-02 row is closed (AC-7).
- [ ] `proof/` holds a by-effect capture from the running jar. It shows: a `400` invalid-create exchange beside the live `ProblemDetail` schema (W2-01, the dogfood reproduction, now consistent), the key-sorted diff of committed against live documents (empty), and a Prometheus scrape plus `/actuator/metrics/disk.free` with no path (W2-03).
- [ ] The ADR amendments named under *Non-functional* exist and are indexed in `docs/DESIGN.md` §7 before the commits that depend on them.

## Source material

- `docs/qa/01-greenfield-core/dogfood.md` (`305dce5`): W2-01, W2-03, the observed journeys and the exchanges under `dogfood/exchanges/`.
- `docs/review/01-greenfield-core/wave-2-review-review-agent.md` (W2-01, W2-03 first recorded); `docs/qa/GAPS.md` row QA-OPR-02.
- Shipped baseline on `main`: `web/OpenApiConfig.java`, `web/Problems.java` (`errors` with `field`/`rule`/`message`), `docs/api/openapi.json` (`ProblemDetail` with a nested `properties` member and no `errors`), `web/OpenApiDocumentTest.java`, `web/HealthMetricsJourneyTest.java`, `application.properties` (metrics exposure).
- `missions/01-greenfield-core/slices/03-operate/SPEC.md` rule 9 and AC-15 (no installation details); `missions/02-brownfield/slices/01-audit-read/design.md` (`4eb1eb4`; `openapi.json` regenerated, audit read `400` with `errors`).
- ADR-0002, ADR-0010, ADR-0016; `docs/REQUIREMENTS.md` FR-13, FR-14, FR-15, NFR-M3.

## Intent visual

N/A: non-visual slice.

## Status

- 2026-10-03: requirements written: 9 acceptance criteria, 5 business rules, 5 ambiguity rows (3 assumed, 2 decided, none parked). Territory named under *Non-functional* for the lead to adopt. The plan-lock waits for both w1 slices to merge.

## Dependencies

- `01-audit-read` (w1) merges first: it regenerates `docs/api/openapi.json` and adds the audit read's problems (AC-2, AC-3). The plan-lock re-checks the impact analysis against the merged `main`.
- `02-click-retention` (w1) merges first: it changes `application.properties`, a W2-03 candidate file.
- Disjoint from `04-audit-columns` (`link/`, `audit/`, `db/migration/`) and `05-ci-cd` (`.github/`).

## Self-check

- Every AC is observable: the live and committed API documents (AC-1, AC-3), HTTP responses validated against the schema (AC-2, AC-4), git history and captured failing runs (AC-5, AC-6), the Prometheus and metrics endpoints (AC-6), and the documents (AC-7, AC-8).
- Error and privacy paths are ACs: every problem status the service sends is checked against the schema (AC-2), and the installation-path disclosure is removed (AC-6).
- Business rules cover the non-obvious logic: test before fix, the wire as the truth, one schema for all problems, the "no installation details" rule extended to metrics, smallest fix.
- Out of scope is explicit, including the non-defect dogfood observations and any change to problem bodies.
- Every ambiguity is resolved: 3 assumed, 2 decided, none parked.
- The proof contract names the per-suite coverage reports, the traceability rows, the `GAPS.md` row and closure, and the by-effect captures (the dogfood reproduction, now consistent, and the path-free scrape), plus the test-first evidence.
- No design leaked: how the schema is corrected (customiser or annotations) and how the tag is dropped (property or meter rule) are the design's. Territory lists both candidate files for W2-03 so the lead can adopt it once.
- Consistent with the dogfood report, the mission brief (FR-14 real defect, FR-15 in the slice that touches it) and the dispatch's exclusions.
- Checked on `main` by me: the committed `ProblemDetail` schema (`docs/api/openapi.json` lines 81–107) has `properties` and no `errors`; `Problems` builds `errors` from `field`/`rule`/`message`; no shipped metrics configuration class exists (`RateLimitFilter` only registers its counter); `GAPS.md` line 48 holds QA-OPR-02. `HealthMetricsJourneyTest` and the smoke do not read the disk gauges.
- Not verified by me: whether one Boot property can drop only the `path` tag (A-4 leaves both routes open), and what `01-audit-read`'s regenerated document will look like after its merge (the plan-lock re-check covers it).
- `plan-review` lenses applied while drafting (not invoked separately). Engineering lens: AC-2 validates every problem status, not just the reported `400`, and AC-4 proves the wire did not move. Strategy lens: W2-03 accepted only because it closes an existing gaps row. UX lens: the Creator's interface is the generated client; AC-1 removes the dead `properties` member.
