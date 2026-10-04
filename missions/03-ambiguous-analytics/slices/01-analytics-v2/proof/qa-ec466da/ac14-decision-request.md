# AC-14 — inherited audit API enumeration exception

Candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9 passed the independent 221-unit/241-functional gate, with merged 580/580 lines and 206/206 branches. Independently replayed the original f6dd29e functional sources: only the two permitted nonempty per-day expectations were updated; 153/155 tests pass. The two unchanged OpenApiDocumentTest methods fail because the already-merged audit API adds /api/audit and a seventh operation. Exact failures, original/replayed file hashes, and fresh output are in v1-replay-summary.json, shipped-functional-source-provenance.json, and v1-replay.txt here. The purge-disabled replay fixture is also disclosed in qa-v1.init.gradle.

Decision needed: does the narrowly documented audit-read grant 428e9e1 (transition 1156) carry forward to analytics AC-14? Its current SPEC permits only per-day expectation changes, so QA has not silently extended the grant. Recommended: the lead records the inherited two enumeration exceptions in this SPEC and GAPS, preserving all remaining original assertions and the green current audit API tests. Alternative: require the literal original assertions; QA then reports AC-14 failed because the merged audit API violates those two original enumerations. No product fix or QA-authored test change is proposed.

Return sequencing also needed: proof item 11 requires the downstream security review before QA can judge it; item 12 requires the release benchmark or its honest accepted latency gap at release_prep. Please route those evidence-backed judgments back to QA at their stages. The present QA check will independently verify items 1–10 and declare 11–12 pending.

QA continues independent HTTP checks while the AC-14 decision is pending.
