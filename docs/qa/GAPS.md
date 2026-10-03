# Coverage and verification gaps

Appended by the QA Agent at every `qa_check`. The gate is 100 % line and branch
coverage across both suites; anything below, or any acceptance criterion not
verifiable by an automated test, is recorded here with the reason and the
manual check performed instead. "None for this slice" is a valid entry.

| Slice | Gap | Why | Compensating check | Status |
|---|---|---|---|---|
| 01-ping | Functional-only coverage 13/15 lines (86.67%); merged 15/15 (100%) | Functional suite does not invoke the two lines of UrlshortApplication.main; per-suite coverage is informational | Unit mainBootsWithoutAWebServer covers them; all three CSVs copied into coverage/01-ping | No merged coverage gap or exclusion; no waiver required |
| 01-ping @3886a04 | AC-7 live log contains 127.0.0.1 in Tomcat thread metadata | Required localhost bind adds the server address to the thread name; MockMvc with a distinct client address does not exercise this metadata | Real GET/canary captures and installed Tomcat bytecode; see 01-ping/findings.md QA-01 | Resolved on f286a10 by exclusion of process.thread.name; independently rechecked on the same bind; no waiver |
| 01-ping @f286a10 | None for the merged coverage gate or acceptance criteria | All 15 tests pass, merged lines 15/15, no branches; functional-only 13/15 remains informational | All AC-1–AC-8 observed live; startup-through-shutdown log has no canaries or loopback addresses; captures carry f286a10 suffix | No open verification gap; packaged artifact belongs to release checks |
