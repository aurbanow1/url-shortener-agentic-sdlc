---
slice: OPR.99.0.2.1
candidate_sha: a922f49144049db0228c316c474ac6e890742fa5
artifact_type: qa
verdict: PASS
money_evidence: 72 unit and 87 functional invocations pass; all 28 ACs covered;
  merged 185/185 lines and 56/56 branches; real HTTP, logs, audit rollback and
  append-only effects observed
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
  - "12"
self_check: Read SPEC, test sources and JUnit results; ran the full candidate
  gate; exercised all controllable ACs with curl; parsed correlated JSON logs;
  compared H2 snapshots and the live/committed document; read all CSV counters;
  stopped the app; kept candidate HEAD unchanged; later review records for items
  13–14 remain open
---

# QA coverage — 01-create-redirect

Candidate `a922f49144049db0228c316c474ac6e890742fa5`; independent QA run 2026-10-03 UTC.

`../../scripts/gw --offline check --rerun-tasks` completed successfully: 72 unit and 87 functional invocations; zero failures, errors or skips; Javadoc gate passed. Captured in `proof/qa-check-a922f49.txt` under the slice.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|
| unit | 169/185 | 91.35% | 56/56 | 100.00% |
| functional | 176/185 | 95.14% | 50/56 | 89.29% |
| all | 185/185 | 100.00% | 56/56 | 100.00% |

Numbers are sums of the committed per-class CSV counters. Per-suite shortfalls are informational; the merged gate has no missed line or branch and no exclusion. HTML, XML and CSV are copied for all three reports.

AC-1–AC-28 have named functional tests. Live HTTP covers every externally controllable AC, all listed invalid inputs, byte boundaries, browser errors, replay/mismatch, environment override, correlated logs, audit rows, rollback and append-only snapshots. AC-19 uses the explicitly permitted suite-controlled FunctionalClock. No expiry/rate-limit, PostgreSQL, packaged artifact or release secret-scan claim.

Proof items 13–14 remain assigned to code review and release prep; see `docs/qa/01-create-redirect/proof-sequencing.md` and queue `qitem-20261003072643-917956c7`.

## Media

![qa-check-a922f49.txt](qa-check-a922f49.txt)
![qa-http-a922f49.txt](qa-http-a922f49.txt)
![qa-rollback-http-a922f49.txt](qa-rollback-http-a922f49.txt)
![qa-append-http-a922f49.txt](qa-append-http-a922f49.txt)
![qa-correlated-events-a922f49.jsonl](qa-correlated-events-a922f49.jsonl)
![qa-audit-before-a922f49.csv](qa-audit-before-a922f49.csv)
![qa-audit-after-a922f49.csv](qa-audit-after-a922f49.csv)
![qa-audit-final-a922f49.csv](qa-audit-final-a922f49.csv)
![qa-audit-comparison-a922f49.txt](qa-audit-comparison-a922f49.txt)
![qa-openapi-diff-a922f49.txt](qa-openapi-diff-a922f49.txt)
![qa-configured-base-a922f49.txt](qa-configured-base-a922f49.txt)
![qa-inspection-a922f49.txt](qa-inspection-a922f49.txt)
![qa-overrides-check-20aef57.txt](qa-overrides-check-20aef57.txt)
