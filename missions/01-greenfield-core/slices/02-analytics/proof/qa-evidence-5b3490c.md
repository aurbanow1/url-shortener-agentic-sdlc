---
slice: OPR.99.0.2.2
candidate_sha: 5b3490c65915cf42594a4720350950bcefd2d7d0
artifact_type: qa
verdict: PASS
money_evidence: Isolated midnight regression 1/1 and full 121/126 green; merged
  coverage100/100; unchanged product verified, fresh click/failure effects, salt
  lifecycle probe green
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
self_check: Exact SHA; sole test setup fix; assertions retained; isolated and
  full gate; CSV/report copies and mappings; source-equivalent prior effects
  plus live recheck; setup capture limit stated; security salt record/probe;
  apps stopped; item13 pending
---

# QA coverage — 02-analytics

Current candidate 5b3490c65915cf42594a4720350950bcefd2d7d0; independent re-check 2026-10-03 UTC. Previous candidate 862c52e reports remain in Git commit 2aedfd1; earlier captures retain their actual candidate attribution.

Fresh ../../scripts/gw --offline check --rerun-tasks: BUILD SUCCESSFUL; 121 unit / 126 functional, zero failures/errors/skips; Javadoc and merged gate passed. Isolated midnight regression: 1/1 PASS in a separate test process before the full gate.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|
| unit | 325/359 | 90.53% | 118/118 | 100.00% |
| functional | 324/359 | 90.25% | 92/118 | 77.97% |
| all | 359/359 | 100.00% | 118/118 | 100.00% |

Per-class CSV sums; all 291 HTML/XML/CSV report files copied and byte-checked. Root XML reports 358 distinct source lines (one line shared across classes is counted twice by CSV sums); both are 100%, no exclusion.

The sole revision draws the unit test salt at noon before its midnight ordering assertions, leaving real scheduled expiry and production unchanged. All 22 ACs and nine rules reran through functional effects. Exact Git object comparison adopts the previous 312-exchange comprehensive evidence, including full header tables, 200 concurrent clicks and unchanged audit. Fresh live check on this candidate adds 15 exchanges, three reduced rows, unchanged two audit/two link rows, a real rejected H2 insert with exactly one correlated safe WARN, HEAD/OPTIONS and 404/405 checks, and empty live API diff.

Item 12 now has the explicit security-review record plus a fresh QA actual-class salt zero/drop/stale-expiry probe. Items 1–12 can be attributed to this candidate; item 13 remains release_prep's NFR-L3 numerical bench/gap in GAPS.md and qitem-20261003103240-18e29a17. Apps stopped, constraint removed, worktree clean. No natural-midnight, added-p95, PostgreSQL, packaged-release or exhaustive scheduling claim.

Fresh correlation covers all 13 journey requests (15 JSON events). Two additional setup-create exchanges are retained; the second setup completion line was not captured before immediate app stop, so setup correlation is not claimed. See PROOF.md QA re-check and proof/qa-recheck-comparison-5b3490c.txt.

## Media

![qa-cr01-isolated-5b3490c.txt](qa-cr01-isolated-5b3490c.txt)
![qa-cr01-isolated-5b3490c.xml](qa-cr01-isolated-5b3490c.xml)
![qa-check-5b3490c.txt](qa-check-5b3490c.txt)
![qa-candidate-equivalence-5b3490c.txt](qa-candidate-equivalence-5b3490c.txt)
![qa-http-5b3490c.json](qa-http-5b3490c.json)
![qa-clicks-5b3490c.csv](qa-clicks-5b3490c.csv)
![qa-correlated-events-5b3490c.jsonl](qa-correlated-events-5b3490c.jsonl)
![qa-recheck-comparison-5b3490c.txt](qa-recheck-comparison-5b3490c.txt)
![qa-salt-lifetime-5b3490c.txt](qa-salt-lifetime-5b3490c.txt)
