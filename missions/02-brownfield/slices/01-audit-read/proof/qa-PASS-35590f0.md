---
slice: OPR.99.0.3.1
candidate_sha: 35590f06c852543c29097a42c43b7802be90ba40
artifact_type: qa
verdict: PASS
money_evidence: 200 unit / 200 functional; merged 492/492 lines and 190/190
  branches; 271 HTTP effects and installed upgrade; AC-17 within named
  enumeration grant
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
  - "8"
  - "9"
  - "10"
  - "11"
  - "13"
self_check: Read fresh gate, CSV and 348 report hashes; exact stored rows, 271
  HTTP responses, 276 correlated log events, full API diff and installed
  upgrade; exact two-page/refused exchange. All apps stopped; item 12 deferred
  under qitem-20261003194346-b74b8081.
---

# Coverage — 01-audit-read

Candidate: 35590f06c852543c29097a42c43b7802be90ba40. Independent offline check --rerun-tasks: 200 unit / 200 functional, zero failures, errors or skips; Javadoc green.

| Suite | Lines | Line % | Branches | Branch % |
|---|---|---|---|---|
| unit | 436/492 | 88.62% | 184/190 | 96.84% |
| functional | 455/492 | 92.48% | 158/190 | 83.16% |
| all | 492/492 | 100.00% | 190/190 | 100.00% |

Merged totals are sums of the per-class CSV, read directly. Per-suite percentages are informational. HTML/XML/CSV reports copied byte-for-byte and hashed in proof/qa-report-copy-hashes-35590f0.json. No coverage exclusions or threshold changes.

QA observed all 21 ACs within the locked SPEC scope: 271 curl captures,
276 correlated JSON request events, exact stored-row comparisons and an
installed shipped-f6dd29e-to-candidate upgrade on one H2 directory. The
original 155-test replay has 153 passes and exactly the two OpenAPI enumeration
failures expressly allowed by lead grant 428e9e1, transition1156; the candidate
versions pass. Controlled peer/JDBC inputs and instrument corrections are
disclosed in GAPS and PROOF; no actual remote TCP-client or natural hardware
failure claim. Proof item12 awaits the downstream independent security record,
tracked by qitem-20261003194346-b74b8081.

Evidence under missions/02-brownfield/slices/01-audit-read/proof/:

- Items1/2/3: qa-check-35590f0.txt, original JUnit XML, report-copy hashes and this CSV summary.
- Item4: docs/qa/TRACEABILITY.md and qa-source-methods-35590f0.json (223 named methods).
- Item5: docs/qa/GAPS.md (named grant, per-suite misses and observation limits).
- Items6/9/11: impact-analysis.md and qa-artifact-provenance-35590f0.json (chronology, indexed ADR0019, no migration).
- Items7/8: qa-http-ledger-35590f0.json, raw curl archive, qa-upgrade-before/after.json, qa-request-log-lines-35590f0.json; 1,641 assertions in qa-verification-35590f0.json.
- Item10: qa-api-diff-35590f0.txt; entire installed/live/committed document equality, earlier operations unchanged.
- Item13: candidate README and docs/DESIGN.md §3; localhost-only, no opening setting, documented local-relay boundary.

QA details and self-check: missions/02-brownfield/slices/01-audit-read/PROOF.md §QA.
