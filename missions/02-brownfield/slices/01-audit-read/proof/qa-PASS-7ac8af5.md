---
slice: OPR.99.0.3.1
candidate_sha: 7ac8af56ed04c27bbefbd416b3976c544d2f274a
artifact_type: qa
verdict: PASS
money_evidence: 203 unit / 202 functional; merged494/494 lines194/194 branches;
  258 HTTP captures and1482 checks; both explicit Tomcat rewriting triggers
  refuse GET/HEAD403; actual same-directory upgrade preserves data; AC17 exact
  grant428e9e1
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
self_check: Read original XML and CSV, reconciled all348 report hashes and227
  source mappings, independently exercised all21AC within recorded grant,
  reproduced repaired jar guard and actual upgrade, compared SQL snapshots,
  retained and corrected plain file capture with23 requests/24 default console
  JSON events, verified apps stopped and exact clean candidate. Item12 awaits
  independent review.
---

# Coverage — 01-audit-read

Candidate: 7ac8af56ed04c27bbefbd416b3976c544d2f274a. Independent QA verdict: **PASS within the
locked SPEC and accepted AC-17 grant**. CR-01 / QA-AUD-01 is fixed on this
candidate by independent installed-jar reproduction. The prior 35590f0 PASS
was superseded by the reproduced bypass; its history and rejected receipt
remain in findings.md and PROOF.md.

Fresh offline check --rerun-tasks: 203 unit / 202 functional invocations,
zero failures, errors or skips; all 14 tasks executed, Javadoc green.

| Suite | Lines | Line % | Branches | Branch % |
|---|---|---|---|---|
| unit | 438/494 | 88.66% | 188/194 | 96.91% |
| functional | 457/494 | 92.51% | 162/194 | 83.51% |
| all | 494/494 | 100.00% | 194/194 | 100.00% |

Merged per-class CSV totals are the gate. Per-suite percentages are
informational. All 348 HTML/XML/CSV report files were copied byte-for-byte
and hashed; no coverage exclusions or threshold changes.

All 21 ACs exercised by effect within their SPEC scope: 258 curl captures,
exact database-row and read-only comparisons, actual held JDBC write and
induced read failure, and shipped-f6dd29e-to-candidate installed upgrade on
one H2 directory. Six real-jar forwarding configurations cover default,
both explicit remoteip triggers, native/framework and empty header settings.
The new triggers refuse plain and forged GET/HEAD with no stored canary.
The original 155-test replay has 153 passes and only the two OpenAPI
enumeration failures authorized by grant428e9e1, transition1156. Candidate
versions pass.

Log capture qualification: the first added file sink used plain text while
the unchanged default console used JSON. Those captures are retained;
correlation was independently repeated for 23 obs- exchanges, including
200/400/403/405/500, and the exact installed two-page exchange plus identical
forwarded URI. All 24 request events match response IDs/statuses in both
default JSON console and ECS file captures. No claim that the earlier
plain-file run has retained JSON correlation. Privacy canaries are absent
from both sets of whole-run logs. GAPS records this instrument correction,
controlled-peer/JDBC scope and the Boot-trigger upgrade ceiling.

Evidence: missions/02-brownfield/slices/01-audit-read/proof/qa-recheck-7ac8af5/.

- Gate: ../qa-check-7ac8af5.txt, test-results/, test-invocations.json.
- Coverage: report-copy-hashes.json, coverage-totals.json, this CSV summary.
- Traceability: docs/qa/TRACEABILITY.md, source-methods.json (227 methods).
- Original replay: shipped-suite.txt, shipped-test-results/, shipped-source-hashes.json.
- Effects: qa-http-ledger.json, http-raw.tar.gz, snapshots, installed-launch-arguments.json, jar-provenance.json.
- Logs: observability-console*.txt, qa-observability.jsonl, qa-exchange.jsonl,
  request-log-lines.json and console-verification.json.
- Reconciliation: verification.json (1,482 assertions), verify.py.
- Provenance/API: artifact-provenance.json, api-diff.txt.
- Limits/self-check: docs/qa/GAPS.md and slice PROOF.md QA.

Items1–11 and13 are covered. Item12 awaits the corrected independent
security-review record under existing lead obligation
qitem-20261003194346-b74b8081; it is not pre-accepted here.

Raw HTTP and shipped XML bytes are retained in hashed archives. Displayed
headers/HEAD bodies and failure XML normalize only line endings and trailing
whitespace; verify.py checks that transformation against original bytes.

## Media

![qa-recheck-7ac8af5/verification.json](qa-recheck-7ac8af5/verification.json)
![qa-recheck-7ac8af5/console-verification.json](qa-recheck-7ac8af5/console-verification.json)
![qa-recheck-7ac8af5/http-raw.tar.gz](qa-recheck-7ac8af5/http-raw.tar.gz)
![qa-recheck-7ac8af5/qa-http-ledger.json](qa-recheck-7ac8af5/qa-http-ledger.json)
