# Coverage and verification gaps

Appended by the QA Agent at every `qa_check`. The gate is 100 % line and branch
coverage across both suites; anything below, or any acceptance criterion not
verifiable by an automated test, is recorded here with the reason and the
manual check performed instead. "None for this slice" is a valid entry.

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 01-ping | Functional-only coverage 13/15 lines (86.67%); merged 15/15 (100%) | Functional suite does not invoke the two lines of UrlshortApplication.main; per-suite coverage is informational | Unit mainBootsWithoutAWebServer covers them; all three CSVs copied into coverage/01-ping | No merged coverage gap or exclusion; no waiver required |
| 01-ping | AC-7 live log contains 127.0.0.1 in Tomcat thread metadata | Required localhost bind adds the server address to the thread name; MockMvc with a distinct client address does not exercise this metadata | Real GET/canary captures and installed Tomcat bytecode; see 01-ping/findings.md QA-01 | Open; no accepted waiver; proof-contract item 7 unsatisfied |
