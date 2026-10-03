---
slice: OPR.99.0.3.3
candidate_sha: 4fe70427bd0d182e886d6a19b217daa1d9e39f5d
artifact_type: qa
verdict: PASS
money_evidence: Six complete problem responses match baseline; corrected schema
  and pathless numeric disk gauges independently observed; fresh 204/207,
  unchanged 203/202, merged 100/100.
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
  - "8"
self_check: Read all 9 ACs and 5 rules; independently ran gate, original suites
  and both pre-fix red controls; compared full jar wire/schema/metrics effects,
  logs and audit rows; checked CSVs, 354 report hashes, 233 trace rows, GAPS
  closure and ADR chronology; apps stopped, exact candidate retained.
---

# Coverage — 03-dogfood-fix

Exact candidate: 4fe70427bd0d182e886d6a19b217daa1d9e39f5d.
Independent QA: **PASS** for all nine ACs and five business rules.

Fresh offline check --rerun-tasks: 204 unit and 207 functional invocations,
zero failures/errors/skips; all 14 tasks executed, Javadoc and coverage
verification green. Original merged-15db6c5 suites ran unchanged against
candidate classes: **203 unit / 202 functional**, all green.

| Suite | Lines | Line % | Branches | Branch % |
|---|---|---|---|---|
| unit | 441/508 | 86.81% | 188/194 | 96.91% |
| functional | 471/508 | 92.72% | 162/194 | 83.51% |
| all | 508/508 | 100.00% | 194/194 | 100.00% |

Totals come from the copied per-class CSVs. Per-suite misses are informational;
the enforced merged gate has no miss, exclusion or waiver. All 354 HTML/XML/CSV
report files were copied byte-for-byte and their hashes rechecked.

Installed baseline and candidate jars produced208 retained HTTP exchanges.
All six specified problem cases have identical status, content type and
complete body values after removing only instance; request IDs vary as allowed.
Every body member matches the corrected live schema. The live document equals
the committed candidate document; the entire document outside ProblemDetail
and its new item schema is unchanged from the merged baseline. All 22 documented
problem responses reference that one component.

The baseline reproduces the disk-path disclosure. The candidate's anonymous
scrape and both disk metrics have no path tag or known working-directory path;
both gauges retain positive numeric values. The old path selector changes
from 200 to the documented 404. QA-OPR-02 is closed in GAPS.md.

QA independently checked out the rebased test-before-fix commits: cce7cf7
fails the two OpenAPI assertions;72dfffb fails the two disk-path assertions.
Fresh candidate versions pass. Both ADR amendments and their DESIGN index
entries precede the dependent commits. README was checked: its smoke, endpoint
and documentation statements remain consistent, so no edit was needed.

All 104 unit and129 functional source methods have traceability rows.
Parameterized XML display names omit the method; those rows are explicitly
attributed to their green class group, with source methods separately
inventoried. Every invocation maps to a named method or that group.
Each of104 requests per jar has one matching response ID/status in both
default JSON console and ECS file logs; create/retire audit rows match response
IDs and link states. Whole-run privacy canaries are absent.

Evidence under missions/02-brownfield/slices/03-dogfood-fix/proof/:

- qa-check-4fe7042.txt: fresh gate.
- qa-4fe7042/verification.json and verify.py:2592 reconciliation assertions,
  including retained raw-byte transformations and copied report hashes.
- qa-4fe7042/http/, raw-captures.tar.gz, wire-comparison.json,
  schema-before-after.json, committed-vs-live.diff (empty): installed effects.
- qa-4fe7042/request-log-correlation.json, console and JSON captures:
  request/log and audit correlation.
- qa-4fe7042/red-*.txt, red-*-results/, regression-red-controls.json,
  history-and-document-checks.json: independently rerun regression failures.
- qa-4fe7042/shipped-suites.txt, shipped-*-results/, qa.init.gradle,
  shipped-source-hashes.json: unchanged original suites.
- qa-4fe7042/report-hashes.json, coverage.json, test-invocations.json:
  copied reports, CSV counts and fresh test XML.
- qa-4fe7042/jar-provenance.json, installed-launch-arguments.json,
  ports-stopped.json, worktree-note.json: artifact/environment provenance.

Both localhost apps are stopped. Primary worktree remains at exact candidate;
its only inherited modification is gradlew.bat line endings, with normalized
byte equivalence recorded. QA authored no product, test or build change.
Checks use the default single disk path; a future second path needs a
non-sensitive distinguishing tag. No new container, latency, migration,
Swagger rendering or natural-clock-boundary claim. Documentation/history ACs
use recorded checks expressly allowed by this SPEC. Raw HTTP/JUnit/console
bytes remain in a hashed archive; displayed headers/XML/text remove only
line endings/trailing whitespace, verified against that archive.
All eight proof-contract items have supporting evidence.

## Media

![qa-check-4fe7042.txt](qa-check-4fe7042.txt)
![qa-4fe7042/verification.json](qa-4fe7042/verification.json)
![qa-4fe7042/wire-comparison.json](qa-4fe7042/wire-comparison.json)
![qa-4fe7042/schema-before-after.json](qa-4fe7042/schema-before-after.json)
![qa-4fe7042/http/candidate-bad-create.body](qa-4fe7042/http/candidate-bad-create.body)
![qa-4fe7042/http/candidate-prometheus.body](qa-4fe7042/http/candidate-prometheus.body)
![qa-4fe7042/http/candidate-disk-free.body](qa-4fe7042/http/candidate-disk-free.body)
![qa-4fe7042/committed-vs-live.diff](qa-4fe7042/committed-vs-live.diff)
![qa-4fe7042/regression-red-controls.json](qa-4fe7042/regression-red-controls.json)
![qa-4fe7042/history-and-document-checks.json](qa-4fe7042/history-and-document-checks.json)
![qa-4fe7042/request-log-correlation.json](qa-4fe7042/request-log-correlation.json)
![qa-4fe7042/ports-stopped.json](qa-4fe7042/ports-stopped.json)
