# Impact analysis — 03-dogfood-fix

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design-agent@urlshort-factory`,
2026-10-03, for SPEC `8b63e5b` (requirements PASS; RQ-01 fixed).

**Baseline.** `main`, product code byte-identical to `f6dd29e` (the w1 slices are not merged yet).
The defects were observed by QA's dogfood pass on the packaged jar
(`docs/qa/01-greenfield-core/dogfood.md`, `305dce5`) and again by this design's probe on the shipped
code ([`design-probe/output.txt`](design-probe/output.txt), D0):
- **W2-01.** The live `components.schemas.ProblemDetail` has members `detail`, `instance`,
  `properties` (an object), `status`, `title` and `type`, and no `errors`. The service's `400`
  answers `{errors, instance, status, title}`.
- **W2-03.** The anonymous scrape shows
  `disk_free_bytes{path="/Users/…/url-shortener/."}`, and `/actuator/metrics/disk.free` lists the
  `path` tag with that value.

**Re-check at plan-lock.** `01-audit-read` regenerates `docs/api/openapi.json` and adds
`GET /api/audit`, whose problems reference the same schema. `02-click-retention` changes
`application.properties`. This analysis is re-read against the merged `main`.

## Change in one sentence

The API document's problem schema is corrected to the bodies the service sends: an optional
`errors` array of `{field, rule, message}`, and no `properties`. The disk gauges lose their `path`
tag on the anonymous metrics surfaces. Each fix has a regression test written first. Requirements:
FR-14, FR-15, FR-13, NFR-M3.

## Impacted modules

Found by reading `OpenApiConfig`, `Problems`, the three controllers whose `@ApiResponse`s reference
`ProblemDetail.class` (`LinkController`, `RedirectController`, `StatsController`; `grep -rln
"ProblemDetail.class" src/main`), and the metrics configuration: no `MeterFilter` exists in
`src/main` (same grep).

| Class or file | Change | Callers and dependents |
|---|---|---|
| `web.OpenApiConfig` | one more `OpenApiCustomizer` bean: replaces the generated `ProblemDetail` component's `properties` member with `errors` (array of `ProblemFieldError`) and adds the `ProblemFieldError` component | springdoc at document build; every operation's problem responses already reference `#/components/schemas/ProblemDetail` (the three controllers above, the `429` customiser, the audit read after its merge) |
| `web.MetricsConfig` (**new**, the SPEC's second W2-03 option) and its unit test `web.MetricsConfigTest` | one bean, Micrometer's own `MeterFilter.ignoreTags("path")` | Boot applies every `MeterFilter` bean to every registry (the Prometheus one in production, the simple one under `@SpringBootTest`) |
| `web.Problems` | **unchanged**: runtime and annotations both. The schema is described in `OpenApiConfig`, not on `Problems` | — |
| `docs/api/openapi.json` | regenerated: only `components.schemas.ProblemDetail` changes and `ProblemFieldError` is added (D1) | `OpenApiDocumentTest` |
| `web.OpenApiDocumentTest`, `web.HealthMetricsJourneyTest` | additions only (AC-9): assertions, and one `@Nested` class each, because two observations need a context the shipped class does not have (*Test impact*) | — |
| `README.md` | checked for statements about problem bodies or disk metrics (AC-8): `grep -n -i "errors\|problem\|disk\|metric" README.md` finds lines 12, 17 and 38, which name the smoke's "error cases, metrics", the `/actuator/prometheus` path and `docs/metrics/`. None describes a problem member or a metric tag, so nothing disagrees. The builder re-checks on the merged `main` | — |

**Not touched:** `application.properties` (the property route cannot drop only the tag, below),
`web.RateLimitFilter` (granted to mission 03), `link/`, `click/`, `audit/`, the schema.

## Impacted endpoints

- **The wire:** no change. Every status, content type and body member is as before (AC-4). The probe
  sent AC-2's five pre-merge requests to the shipped service and to the fixed one: status, content
  type, members and `errors` item members are equal (D2, "wire unchanged: true").
- `GET /v3/api-docs`: `ProblemDetail` corrected, `ProblemFieldError` added; every path, operation,
  response, header and example unchanged (D1).
- `GET /actuator/prometheus`, `GET /actuator/metrics/disk.free` and `disk.total`: the same gauges and
  values, without the `path` tag (D3).

## Impacted schema and data

None.

## Impacted data flows

None at request time. The document is built once by springdoc. The meter filter is applied when a
meter is registered.

## Blast radius

| If this is wrong | Worst case | Detection |
|---|---|---|
| The customiser removes or alters something else | a client generated from the document loses an operation or type | AC-3: the committed-document diff is confined to the problem schema (D1: paths and other schemas equal) |
| The schema still misdescribes a body | a generated client cannot read refusals | AC-2: six bodies validated against the live schema, including the audit read's `400` after the merge |
| The filter drops the gauges, or a tag another meter needs | lost disk alerting or merged series | it drops one tag key and keeps the meters and their values (D3). On the shipped scrape only the two disk gauges carry `path` (D0: 2 samples; D3: 0). `MetricsConfigTest` pins that other tags survive |
| A later meter carries a `path` that matters | its series merge silently | ADR-0016 already bars client values (a request path) from tags, and a filesystem path is an installation detail (SPEC rule 4). The ADR-0016 amendment names the filter, so a new meter's author meets it |
| A second disk path is configured later | two `disk.free` gauges would collide without their `path` tag | only Boot's default path (`.`) is configured; design §6 names this |

## Compatibility (FR-13)

- **The wire is unchanged** (AC-4), so every client keeps working.
- **Generated clients:** they gain a typed `errors` field and lose the dead `properties` field.
  That is the fix the dogfood report asks for.
- **Metric consumers:** a query that filtered on `path` now matches the single series without it.
  The tag's value was an installation detail on an anonymous surface.

## Test impact

**Changed by additions only (AC-9). No existing line is edited:**
- `OpenApiDocumentTest`: AC-1's schema assertions, and AC-2's `400`, `422`, `404`, `410` and audit
  `400` bodies against the schema. Plus one `@Nested` class with
  `@TestPropertySource(properties = "urlshort.rate-limit.create-per-minute=1")` for the `429`.
  The functional overlay sets the budgets to 1,000,000 a minute, so the shared context cannot
  produce a `429` (probe T4).
- `HealthMetricsJourneyTest`: AC-6's no-path assertions on `/actuator/metrics/disk.free` and
  `disk.total`. Plus one `@Nested @AutoConfigureMetrics` class for the scrape:
  `/actuator/prometheus` answers `404` in the plain `@SpringBootTest` context, because Boot's test
  support turns metrics export off (probe T1).
- The two nested classes are two more cached application contexts in the functional suite. That
  is about two seconds each; `RateLimitJourneyTest` already pays the same for its own.

**Unchanged:** every other test. No shipped test asserts the problem schema's members or the disk
tag. `grep -rn "properties\|disk" src/functionalTest src/test` finds `@SpringBootTest(properties=…)`
attributes, `Properties` loading and one schema assertion, `StatsJourneyTest:243` on the
statistics schema's members.

**Added:** `MetricsConfigTest` (unit).

## Observability impact

The disk gauges lose one tag. No other metric, log or event changes.

## Risks and mitigations, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | The document drifts elsewhere | AC-3 diff on the regenerated document; D1 | QA |
| 2 | The test-first evidence is missing | build plan: assertions committed and seen failing before the fix (AC-5, AC-6) | builder → QA |
| 3 | OpenAPI custody with mission 03's `01-analytics-v2` | the later plan-lock rebases and regenerates | lead |

## Self-check

- Both defects were reproduced on the shipped code (D0) before anything was designed.
- Every file the fixes touch is listed. `Problems` and `application.properties` are explicitly not
  touched, with the reason.
- The fixes were run on a real server (D1–D3): the wire is equal before and after, the document
  changes only where intended, and the scrape has no `path` label. The fixes were also run in the
  suite's own set-up (T1–T5): the regression assertions fail on the shipped code with messages that
  name `errors`, `properties` and `path`, and pass with the fixes.
- Not verified here: the audit read's `400` against the schema (merged later; AC-2 covers it); the
  merged `main` (plan-lock).
