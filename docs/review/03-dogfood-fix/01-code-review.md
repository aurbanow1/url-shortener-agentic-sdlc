# Code review — 03-dogfood-fix

Candidate **4fe70427bd0d182e886d6a19b217daa1d9e39f5d**, reviewer `review-agent@urlshort-factory` (Codex), 2026-10-03. Packet `qitem-20261003223352-97e556cd` combines code and security with one exit. **PASS; no findings.**

## Context proof

The Creator's generated client needs the API document to describe the `errors` array already sent on validation and idempotency failures. The Operator needs the disk gauges without disclosure of the installation path. Two existing defects are corrected with tests committed first; request handling, data and other API metadata must stay unchanged. Confidence: intent/invariants 99%, implementation/evidence 99%.

Read the SPEC, design, impact analysis, ADR-0010/0016 amendments, AGENTS, relevant system design, review/Java/QA/security/brownfield guidance, builder/QA PROOF and PROGRESS, coverage summary, traceability and GAPS. Resolved project → mission → slice territory and the explicit combined packet. No additional review protocol selected.

Exact worktree HEAD matches QA. `main...slice/03-dogfood-fix` has merge base `15db6c52786e65201bb8ab8228b84efe37650416`: **6 files, +240/-5**. Candidate descends from audit-read's merge `cb148c4`. Its sole pre-existing tracked worktree difference is `gradlew.bat`; independently verified CRLF-normalized byte equality to the candidate blob. It is not part of this change and was not repaired or committed. All product/test/build/document inputs used for the review match the named candidate.

## Complete file ledger

Full diff and every complete changed file read. Paths are relative to `.worktrees/03-dogfood-fix`.

| File | Verdict |
|---|---|
| `docs/api/openapi.json` | PASS; only ProblemDetail and the added ProblemFieldError differ. Optional array, three required string fields, no nested properties member. All other schemas, seven operations, responses, examples, headers and server metadata preserved; all 22 documented problem responses reference the same component. |
| `src/main/java/dev/urlshort/web/OpenApiConfig.java` | PASS; one platform customizer in the existing metadata owner, no request-path change. Corrects the generated component and names the item schema. The schema is guaranteed by existing controllers and checked at document generation; no speculative null fallback. |
| `src/main/java/dev/urlshort/web/MetricsConfig.java` | PASS; one package-private configuration and native `MeterFilter.ignoreTags("path")`. Keeps meters and unrelated tags, no dependency or custom filtering framework. Its comment names the accepted single-disk-path ceiling. |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | PASS; 102 added lines, no existing line removed. Schema assertions and six real response shapes include audit 400 and a nested one-create-budget 429 context. Existing live/committed equality and operation checks retained. |
| `src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java` | PASS; 41 added lines, no existing line removed. Both disk endpoints keep values without tags; nested metrics-export context checks the actual scrape. Original health and route-template assertions retained. |
| `src/test/java/dev/urlshort/web/MetricsConfigTest.java` | PASS; verifies observable registered gauge value and preservation of an unrelated tag. No implementation-call assertion. |

New production type is package-private and documents its purpose and limitation; the existing public Javadoc gate passes. Impact analysis module list and territory match the diff. No schema, dependency, configuration-property or request-handler change; no lowered threshold, exclusion or deleted test.

## Verification and audit of QA evidence

Independently ran, in the exact worktree:

```sh
scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/03-dogfood-fix/proof/code-check-4fe7042.txt --offline check --rerun-tasks
```

[Fresh gate](proof/code-check-4fe7042.txt): exit 0, 40 seconds, all 14 tasks executed; **204 unit + 207 functional**, zero failures/errors/skips. Merged **508/508 lines, 194/194 branches**. Fresh per-class CSVs equal QA's committed CSVs: unit 441/508 lines and 188/194 branches; functional 471/508 and 162/194. No merged shortfall.

Independent [reconciliation source](proof/reconcile-4fe7042.py) and [result](proof/evidence-reconciliation-4fe7042.json) establish:

- All **354** copied coverage report hashes match; all **532** retained raw capture/archive hashes and their declared whitespace-only display transformations match.
- The freshly generated document and installed-candidate capture equal the committed document; the baseline capture equals its committed document. A whole-document comparison differs only in the two allowed problem components.
- Six retained baseline/candidate HTTP pairs have identical status, content type and **full body values**, including title and every field/rule/message, excluding only instance. Each instance matches its response request ID. Both error shapes conform to the documented members; this is not merely a shape comparison.
- Installed candidate captures retain positive numeric disk free/total values, no path tag and no known working directory. Baseline reproduces the path disclosure. The old `path` selector returns 200 before and 404 after, as the accepted design documents.
- All **208** retained responses match exactly one completion ID/status in both default JSON console and ECS file logs, 104 per jar. Whole-run privacy canaries are absent. QA's audit captures record the two create/retire mutations and replay without an extra row; no new writer exists in the diff.
- All **233** source methods have traceability rows; every AC 1–9 and rule 1–5 has an attributed test/check. Parameterized invocation names are attributed to green class groups where JUnit omits the method, as disclosed in GAPS.
- QA ran the original baseline suites unchanged: **203 unit + 202 functional**, zero failures/errors/skips. Independently parsed those XML results and verified all 50 baseline test/resource hashes, plus baseline jar input hashes. This reviewer reran the candidate gate, not the historical suites.
- Test-first history is `cce7cf7` → `a28a20a` for the schema and `72dfffb` → `4fe7042` for metrics. QA's retained red runs each contain exactly two expected failures. Read the pre-fix source: the schema customizer is absent at the first test commit and MetricsConfig is absent at the second; the assertions necessarily detect the original missing-errors/extra-properties and path defects. ADR design/amendment commits `0d000da`/`0982cb5` precede the tests/fixes by ancestry. No new historical checkout/run by this reviewer is claimed.

The reconciliation initially attempted to JSON-parse the empty body of the documented metrics-selector 404. Corrected that reviewer-only instrument to inspect the status header; the complete reconciliation then passed. No product change followed from that instrument error.

## Acceptance assessment

| AC / requirement | Evidence / result |
|---|---|
| 1–2 / FR-14 | PASS; fresh schema and six response-conformance tests; audited installed bodies and 22 shared references. |
| 3 / NFR-M3, FR-13 | PASS; fresh live export equals committed; complete baseline diff only the two allowed components. |
| 4 / FR-13 | PASS; six full-value wire comparisons preserve the response contract. |
| 5 / FR-14 | PASS; preceding schema test commit, retained QA red execution and candidate green gate. |
| 6 / FR-14, FR-15 | PASS; endpoint/scrape and unrelated-tag tests, installed path-free positive gauges, preceding metrics test commit and retained red execution. |
| 7 / FR-15 | PASS by content; QA-OPR-02 closed with evidence and W2-01 recorded fixed. Shared-file proof-reference freshness is routed below. |
| 8 / FR-15 | PASS; DESIGN describes optional errors and path-free meters, ADR amendments indexed. README's endpoint/smoke descriptions remain accurate; no edit needed. |
| 9 / FR-13 | PASS; original suites independently executed by QA and audited here; both modified shipped tests add assertions only, new unit test allowed. |

## Findings

None. MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0.

## Ponytail review

Lean already. Ship.

Both mechanisms use the platform in the existing ownership boundary. The named second-disk-path ceiling is accepted intent, not speculative work to add now.

## Verdict and self-check

**Code PASS, merge-ready subject to current QA proof receipts and the lead's integration check.** No non-blocking fixes or backlog findings. Security has its own [record](02-security-review.md) in this combined packet.

During review, retention's commit `84d3604` appended its own section to shared GAPS, leaving this slice's section and QA-OPR-02 closure byte-identical. Item 6 briefly became unknown. Routed narrow reaffirmation as `qitem-20261003224103-2a21b284`; QA resolved it with receipt 10, commit `2f1987e`. Independently re-read live readiness: **8/8 accepted, ready, no issues**, and all **38 evidence references / 32 distinct files** match their current hashes ([snapshot](proof/proof-readiness-4fe7042.json)). No product failure or extra build. Future shared-file changes may require final reconciliation again.

Exact SHA and inherited wrapper-only noise verified, all six files covered, fresh full gate and upstream evidence audited, no producer code/tests/SPEC/design edited, both ledger rows recorded. Scope limits remain the single configured disk path, changed old-path selector, and no new Docker/load/migration/Swagger-rendering claim.
