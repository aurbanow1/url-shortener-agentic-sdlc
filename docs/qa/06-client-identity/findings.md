# Findings — 06-client-identity

## Re-check e40b09541feb0b7555c475baa82587fdd09e4890

| Finding | AC / rule | Severity | Expected versus prior observed | QA re-check |
|---|---|---|---|---|
| CR-01 (review2 origin) | AC-5; deterministic characterization | HIGH | Writer delay should fit the 10-second wait without consuming the frozen HTTP budget. Reviewer on fb63a88 observed 429/NPE after 170 ms; fresh peer after release gave correct 3/2. | QA resolution observed: unchanged budgets/oracles; actual helper with a 3-second hold passes without trust and behind P, one HTTP stats read, no 429/NPE. Fresh 590 gate and original-production: 72 characterization replay pass. Formal code re-review remains pending. |

Reproduction/resolution command and actual outputs:
`recheck-e40b095/README.md`, `probe.gradle`, `QaStatsPollingResolution.java`,
`held-writer-probe.txt`. Prior source/output/hash qualification is retained in
`review-polling-race.md`; QA did not claim to have independently run that old probe.
No new QA finding. No product or test edit by QA.
