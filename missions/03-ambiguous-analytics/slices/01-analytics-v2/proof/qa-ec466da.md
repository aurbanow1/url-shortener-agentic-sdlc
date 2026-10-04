---
slice: OPR.99.0.4.1
candidate_sha: ec466da8da4b1efde9d612c6c8692070cc6fc4b9
artifact_type: qa
verdict: PASS
money_evidence: Fresh221/241 gate, merged580/580 lines206/206 branches;
  installed4/3/1 figures, default/trusted slow/failing/concurrent effects and716
  dual-sink request correlations; authorized original155/155.
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
self_check: Independently read all15ACs and11rules; observed exact HTTP and
  stored rows, actual H2 insert faults/delay, both console/file logs and audit
  IDs; read copied CSV and372 report hashes, trace282 source rows/462
  invocations, GAPS limits; original replay155/155 under a12a0e2; apps stopped
  curl7, clean exact HEAD. Security11/release12 pending.
---

# QA coverage — 01-analytics-v2

Candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9. Independent QA: qa-agent@urlshort-factory (Codex), 2026-10-03. Verdict: PASS for qa_check; downstream security proof item 11 and release benchmark item 12 remain pending under lead sequencing a12a0e2.

Fresh exact-candidate command: `../../scripts/gw --log ../../missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-check-ec466da.txt --offline check --rerun-tasks`. BUILD SUCCESSFUL; 221 unit and 241 functional invocations, zero failures, errors or skips. Javadoc and merged coverage verification passed.

| Suite | Tests | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|---:|
| Unit | 221 | 506 / 580 | 87.24% | 198 / 206 | 96.12% |
| Functional | 241 | 546 / 580 | 94.14% | 171 / 206 | 83.01% |
| Merged execution data | 462 | 580 / 580 | 100.00% | 206 / 206 | 100.00% |

Numbers are sums of the copied CSV rows, not an average of suite percentages. The three report trees contain 372 files, independently compared by SHA-256 with the worktree outputs. Per-suite deficits are informational; no exclusion or threshold change. CSVs retain the JaCoCo filename jacocoTestReport.csv in each tree. Evidence and reconciliation: [verification-summary.json](qa-ec466da/verification-summary.json), [report-hashes.json](qa-ec466da/report-hashes.json), and [verify.py](qa-ec466da/verify.py).

Independent installed jar: SHA-256 a1f85572fd35035385d50696ceae7ff25a230a712a42631c46f57134e0a39050. On real Tomcat, four redirects from three forwarded clients (one twice and one bot) produced clicks=4, uniqueVisitors=3, botClicks=1; the real-loopback trusted setting is explicit. Controlled actual-candidate Tomcat/JDBC instances exercised the exact SPEC peers, default and trusted-proxy configurations, UTC boundary, bot classes, empty link, v1 seven/day/referrer values, privacy, metrics, error methods, duplicates, 24-hour key expiry and rate limits. Actual H2 trigger failures gave unchanged 302 responses, two class-only correlated loss warnings and lost+2 in each context; successful writes gave recorded+3. A two-second physical insert delay left all 20 redirects below 250 ms in each context; 200 concurrent redirects in each context stored 200 reduced rows. These are resilience observations, not release percentiles.

713 curl captures plus three direct-wire HEAD checks reconcile to 716 distinct request completions in both default console and ECS file output (installed24/default425/trusted267). Every failure response has a correlated ProblemDetail instance. Audit create/retire rows match response requestIds; reads and HEAD/OPTIONS leave database snapshots unchanged. The inherited 90-day purge removed old click rows after a controlled-clock advance without changing links. All three apps stopped; subsequent loopback probes returned curl exit7. Worktree HEAD remains the exact clean candidate.

The literal original f6dd29e replay was 153/155: only the inherited audit path and operation enumerations failed. Their carryforward was explicitly authorized by lead a12a0e2 (slice.yaml and mission NOTES); the repeat with those two expectations and the two allowed per-day shape updates passed 155/155. Every other original assertion remains byte-identical. The older retention fixtures run with purge disabled, as disclosed in the external replay init script. Raw initial failures remain preserved. Candidate tests, 282 source-method/context rows and all462 invocation results are mapped in TRACEABILITY and the source/invocation inventories; parameterized XML group attribution is explicitly qualified.

Limits are recorded in [GAPS.md](../../../../../docs/qa/GAPS.md): controlled Servlet peers and Clock do not establish a remote TCP-client boundary or natural midnight; the H2 trigger is a disposable fault/delay fixture; hash-use construction and salt non-persistence await downstream security review; NFR-L1 p95/p99 await release_prep. The mandatory NFR-L1 gap remains open, as the SPEC requires. No required AC or merged coverage gap remains at qa_check.

## Media

![qa-ec466da/http/installed-stats.body](qa-ec466da/http/installed-stats.body)
![qa-ec466da/http/installed-openapi.body](qa-ec466da/http/installed-openapi.body)
![qa-ec466da/default-click-counters.txt](qa-ec466da/default-click-counters.txt)
![qa-ec466da/trusted-click-counters.txt](qa-ec466da/trusted-click-counters.txt)
![qa-ec466da/verification-summary.json](qa-ec466da/verification-summary.json)
