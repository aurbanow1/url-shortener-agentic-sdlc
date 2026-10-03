---
slice: OPR.99.0.3.3
candidate_sha: 4fe70427bd0d182e886d6a19b217daa1d9e39f5d
artifact_type: qa
verdict: PASS
money_evidence: "both regression tests were committed and seen failing on
  unfixed code before their fixes: the problem schema lacked errors and carried
  properties; the disk gauges carried a path tag on the metrics endpoint and the
  scrape; on 4fe7042 (descending from audit-read's merge cb148c4) the gate is
  green, unit 204, functional 207, 508/508 lines, 194/194 branches"
evidences:
  - "2"
self_check: "I read both red logs and their XML reports: the W2-01 messages name
  errors and properties, the W2-03 messages name the path tag and the path
  label; the git log shows each test commit before its fix; the gate log on
  4fe7042 ends BUILD SUCCESSFUL after check with --rerun-tasks"
---

# Builder evidence — 03-dogfood-fix, candidate 4fe7042

This is evidence from the builder seat, `development-agent@urlshort-factory`, not a verdict. The narrative is in `PROOF.md` §Builder.

- **Contract item 2 (test before fix).**
  - **W2-01:** the test commit `9b2788a` (rebased `cce7cf7`) failed on the unfixed code. AC-1 failed with "ProblemDetail documents the errors member the service sends and no properties member …" and AC-2 with "[400 member errors is documented]". The fix followed in `edc1815` (`a28a20a`).
  - **W2-03:** the test commit `3224036` (`72dfffb`) failed with "[disk.free carries no path tag]" and "[the scrape carries no path label]". The fix followed in `5233c29` (`4fe7042`).
- **Gate on the candidate.** Unit 204, functional 207, 508/508 lines and 194/194 branches merged, Javadoc green. The candidate descends from `01-audit-read`'s merge `cb148c4`.

## Media

![red-w2-01-openapi.txt](red-w2-01-openapi.txt)
![red-w2-01-report.xml](red-w2-01-report.xml)
![red-w2-03-metrics.txt](red-w2-03-metrics.txt)
![red-w2-03-report.xml](red-w2-03-report.xml)
![red-w2-03-scrape-report.xml](red-w2-03-scrape-report.xml)
![builder-check-5233c29.txt](builder-check-5233c29.txt)
![builder-check-4fe7042.txt](builder-check-4fe7042.txt)
