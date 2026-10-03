# 03-dogfood-fix — design review

**PASS on `0982cb5ef9e09320379118d857225d31996e2bf6`. No open findings.**
Two MEDIUM documentation/evidence findings were corrected in passing.

Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003193154-cf5ec0b6`; instance: `01M41HGF6AHB6AZR3Q0KQ8MQR7`.
Accepted SPEC: `8b63e5b`. Initial design: `0d000da`, self-check/notes `c794f76`;
impact analysis `e8969b5` precedes both. Producer handed correction `0982cb5` at 19:38Z.

## Context and complete coverage

The Creator needs the documented error extension to match the existing response. The Operator
needs disk gauges without the installation path. The design changes one shared OpenAPI component
and uses Micrometer's tag filter; it adds no request producer, data operation or dependency.
Confidence: high for the proposed mechanisms; implementation and post-w1 integration remain future work.

Read the complete SPEC, impact analysis and design; all source and output files below; the authored
shared-document deltas; relevant existing OpenAPI/error/metric code and functional tests; prior
requirements review; mission decisions and the adopted territory. Applied review guidance,
architecture §§3–8, databases §8 and brownfield §7. The 954-line producer test log was parsed
throughout, including JSON bodies/events, test failures and metric output. Its green Gradle footer
does not mean all tests passed: its three deliberate red controls are described below.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/03-dogfood-fix/impact-analysis.md` | PASS after DR-01/02; original committed before design; modules, consumers, compatibility and grants identified |
| `.../design.md` | PASS after correction; all nine ACs reachable, no new error producer or storage mechanism |
| `.../design-probe/DogfoodProbe.java` | Fully read; exact bean fixture works; original D2 compares shapes only, now honestly scoped |
| `.../design-probe/DogfoodRegressionProbeTest.java` | PASS as a mechanism probe; explicit shipped/fixed contexts and representative request assertions |
| `.../design-probe/dogfood-probe.gradle` | PASS; isolated source launcher, functional runtime, no product edit |
| `.../design-probe/design-test-probe.gradle` | PASS as a red/green demonstration; `ignoreFailures=true` requires inspecting individual outcomes |
| `.../design-probe/output.txt` | Complete output read; two disk path samples become zero; D2 scope corrected in design |
| `.../design-probe/test-output.txt` | All nine outcomes reconciled: three intended shipped failures, six passes; no unreported failed fixed case |
| `docs/DESIGN.md` | Complete authored delta read; proposed changes distinguished from shipped behavior; mechanisms/ADR index updated |
| `docs/adr/0010-committed-openapi-document.md` | PASS; one component correction, unchanged wire, current export command and future-extension limit recorded |
| `docs/adr/0016-metrics-and-health-exposure.md` | PASS after DR-02; scope of dropped tag, consumer adjustment and multiple-disk ceiling recorded |
| `missions/02-brownfield/NOTES.md` | Handed delta read; design provenance and plan-lock obligations recorded |

`.../` above means `missions/02-brownfield/slices/03-dogfood-fix/`.
**12 unique changed files / 12 reviewed**, including the three correction paths already in that set.
No product/build-file delta between the initial and corrected candidates; the review ran against
the same product baseline and handed bean fixture. No producer artifact was edited by the reviewer.

## Contract assessment

| Contract | Assessment |
|---|---|
| AC-1/2; rules 2/3 | One customiser removes the phantom member and adds optional `errors` referencing a component with required string fields. Existing operations retain their shared reference. No controller or error-factory edits. Representative conformance checks cover validation, idempotency, missing, retired, limited and post-merge audit errors. |
| AC-3/4 | Committed/live equality remains in the existing test. The whole-document control found no change outside the two intended schemas. Final AC-4 strategy explicitly compares values as well as names, with only request-specific identifiers excluded. |
| AC-5/6; rule 1 | Builder must commit and capture failing assertions before each fix. Fresh replay confirmed the expected failures and fixed successes. Design probes are not substitutes for the eventual builder's commit-order evidence. |
| AC-6; privacy and metrics | Both metrics surfaces lose `path`; both gauges remain. Platform filter preserves other tags and values. Existing queries filtering on the removed tag must change, now documented. No exposure or authentication changes. |
| AC-7/8 | QA owns gap closure; design owns shared docs/ADR amendments; README check is recorded even with no edit. These checks remain acceptance work. |
| AC-9; brownfield | Existing assertions/context settings remain; additional nested contexts provide the new low-budget and scrape observations. This is support for added assertions, not a weakened existing test. Impact recheck against both w1 merges remains required. |
| Scope and territory | Two small beans use the existing springdoc/Micrometer facilities, no dependency or speculative layer. Drop `Problems.java` and application.properties from the grant; add the named MetricsConfig and unit-test files at plan-lock. Preserve RateLimitFilter's other-mission grant and ordered OpenAPI custody. |
| Data, rollback, audit | No DDL, query or data mutation; audit-column policy creates no new obligation in this slice. There is no data rollback; reverting these metadata/filter changes restores the prior schema document and path tag, including the original defects. |
| Threat model and ceiling | Existing anonymous document/scrape surfaces covered; no new redirect/fetch/input path or logs. The single configured disk path is explicit: multiple paths would collide after tag removal and require a non-path discriminator before that expansion. |

## Independent verification

```sh
scripts/gw --log docs/review/03-dogfood-fix/proof/design-controls.txt --offline -I docs/review/03-dogfood-fix/proof/design-controls.gradle reviewDogfoodControls
scripts/gw --log docs/review/03-dogfood-fix/proof/design-regression-rerun.txt --offline -I missions/02-brownfield/slices/03-dogfood-fix/design-probe/design-test-probe.gradle designDogfoodTestProbe
scripts/gw --log docs/review/03-dogfood-fix/proof/design-baseline-check.txt --offline check
```

The [independent assertions](proof/DesignControls.java) reuse the producer's exact bean fixture
and HTTP launcher, then inspect real responses with separate checks. [Results](proof/design-controls.txt):

- Five pre-merge cases (400/422/404/410/429): same status/content type and complete JSON values
  after removing only `instance`; each original instance also matches its response request id.
- A changed-title negative control passes a names-only comparison but fails full-value equality.
- Whole document equal after excluding ProblemDetail and the one added ProblemFieldError;
  required item fields verified. Both disk endpoints return numeric measurements without tags,
  and both Prometheus samples remain with no path label.
- Old Actuator `tag=path:<value>` selector: 200 before, 404 after; unfiltered remains 200.
- Direct registry control: the standard filter removes `path`, retains `other=kept` and value 7.

The reviewer's first compile used try-with-resources for a registry that is not AutoCloseable;
that probe-only error was corrected to explicit close before the successful run. It was not a
product or toolchain failure.

Fresh [regression replay](proof/design-regression-rerun.txt) and parsed
[nine individual results](proof/design-regression-results.json) confirm exactly three intended
AssertionErrors on shipped behavior and six passes, including every fixed case and both nested
contexts. The Gradle task exits 0 because it deliberately ignores those failures; the individual
outcomes, not that exit code, establish red/green behavior. The display log retains all lines
with trailing whitespace stripped; its original hash is recorded with the parsed results.

The [baseline gate](proof/design-baseline-check.txt) exits 0 with all 14 tasks UP-TO-DATE.
These are design controls on the pre-w1 baseline, not candidate QA. Audit-read's sixth response,
the later merged document, final coverage and jar captures remain explicitly assigned downstream.

## Findings and resolution

| Id | Severity | File:line at initial candidate `c794f76` | Evidence | Required change |
|---|---|---|---|---|
| DR-01 | MEDIUM | `missions/02-brownfield/slices/03-dogfood-fix/design.md:208`, `:291`; `design-probe/DogfoodProbe.java:94` | D2 serializes status, content type, member names and error-item count/names, omitting values. A different title compares equal by that representation. The proposed beans pass the stricter control, so this is evidence/QA-strategy precision rather than an observed wire defect. | Qualify D2 and explicitly require full normalized values for AC-4, including title and every error field/rule/message. |
| DR-02 | MEDIUM | `missions/02-brownfield/slices/03-dogfood-fix/impact-analysis.md:81` | It claims a query filtered on path still matches after tag removal. The real Actuator control instead changes from 200 to 404. The intentionally removed tag requires consumers to adjust their selector. | State the compatibility consequence in the impact/design/ADR and tell consumers to drop the old selector. |

## Re-review 0982cb5

**DR-01 fixed:** D2 is now scoped to shape; the final QA strategy requires full-value equality
and cites the independent control. **DR-02 fixed:** impact analysis, design and ADR-0016 now
describe the old selector's loss of matches and the required consumer adjustment. Read all three
changed files' complete correction delta and the author's response. No new mechanism or product
change was introduced; the successful controls remain applicable. No open finding or backlog item.

## Self-check and handoff

Exact candidates, complete file ledger, AC mapping and empirical limits recorded. Findings have
locations, reproduced consequences and verified resolutions. Only reviewer artifacts authored.
PASS to plan-lock; retain both w1 merges, the impact recheck, narrowed grant and cross-mission
OpenAPI ordering before implementation begins.
