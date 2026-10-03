---
slice: OPR.99.0.1.1
candidate_sha: 3886a04a4afac6117038b2884cf72749f57d28aa
artifact_type: qa
verdict: NOT-CLEAR
money_evidence: 6 unit and 9 functional tests pass; merged 15/15 lines, no
  branches; live AC-7 literal address absence fails on Tomcat loopback bind
  metadata (QA-01).
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
self_check: Read all three coverage CSVs and JUnit XML; inspected four HTTP
  captures and every matching JSON event; preserved server-bind provenance and
  failed literal AC-7; stopped app and verified candidate unchanged.
---

# QA coverage — 01-ping

Candidate: `3886a04a4afac6117038b2884cf72749f57d28aa` (`slice/01-ping`).
Independent QA: `qa-agent@urlshort-factory` (Codex), 2026-10-02/03 UTC.
Verdict: **NOT-CLEAR** — build and coverage pass; live AC-7 has the literal
address-absence mismatch recorded in [findings](../../../../../docs/qa/01-ping/findings.md).

## Gate and coverage

From the main checkout, using its current logging wrapper against the exact
candidate project:

```sh
scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-check.txt --offline -p .worktrees/01-ping check --rerun-tasks
```

Exit 0, BUILD SUCCESSFUL in 18s, all 13 tasks executed. Unit: 6 tests;
functional: 9 tests (eight named AC journeys plus baseline health). Zero
failures, errors or skips. Test method inventory:
`missions/00-hello/slices/01-ping/proof/qa-tests.txt`.

Totals read from the CSV files below, not inferred from the build result:

| Suite | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---|
| [Unit](../../../../../docs/qa/coverage/01-ping/unit/html/index.html) | 15 / 15 | 100% | 0 / 0 | N/A: no branches |
| [Functional](../../../../../docs/qa/coverage/01-ping/functional/html/index.html) | 13 / 15 | 86.67% | 0 / 0 | N/A: no branches |
| [Merged](../../../../../docs/qa/coverage/01-ping/all/html/index.html) | 15 / 15 | 100% | 0 / 0 | N/A: no branches; 100% threshold passes |

Sources: `unit/jacocoTestReport.csv`, `functional/jacocoTestReport.csv`,
`all/jacocoTestReport.csv`; corresponding HTML and XML are committed too.
No exclusions. The functional-only shortfall is the two lines in
`UrlshortApplication.main`, covered by the unit suite. The merged gate is
satisfied; zero branches must not be interpreted as exercised branch logic.

## By-effect results

The app ran from the candidate on `127.0.0.1:18081`, with an isolated in-memory
H2 database and unchanged shipped logging configuration. All HTTP requests
used `scripts/http`. Artifacts are under
`missions/00-hello/slices/01-ping/proof/`.

| AC | Observed | Result / evidence |
|---|---|---|
| AC-1 | 200, application/json, exactly status=ok and time | PASS; qa-ping-first.txt |
| AC-2 | Both first and second times parse as UTC instants, include seconds, end in Z, lie between recorded before/after instants | PASS; qa-http-start.txt, qa-http-end.txt, qa-ping-first.txt, qa-ping-second.txt |
| AC-3 | Issued id is 36 printable non-whitespace ASCII characters | PASS; qa-ping-first.txt |
| AC-4 | First id 010b88fa-3871-48b6-a14e-5d7792d659c7 differs from second id 0ff0c2bd-3b07-45d6-8a00-9bf83a96c545 | PASS; both GET captures |
| AC-5 | POST returns 405, application/problem+json, status=405, title and instance, no stack trace or exception name, nonempty request id | PASS; qa-ping-post.txt |
| AC-6 | Each GET has a single-line JSON event with requestId matching its response | PASS; qa-ping-log-line.json, qa-bootrun.txt |
| AC-7 | User-Agent canary absent; literal remote address 127.0.0.1 present in server thread metadata | NOT-CLEAR; QA-01 in findings, qa-ping-canaries.txt, qa-bootrun.txt |
| AC-8 | Inbound id ignored in response and absent from all log output | PASS; qa-ping-canaries.txt, qa-bootrun.txt |

The two bracketed GETs were consecutive requests, with approval latency
between them; no performance or narrow timing-window claim is made.

## Limits and disposition

The AC-7 value is the **server bind address**, shown by the installed Tomcat
bytecode in `qa-tomcat-bind-name.txt`; this is not evidence that the service
reads or logs a remote client address. The functional test with a distinct
remote address passes. Nevertheless the independent live capture does not
satisfy the literal locked AC-7 or proof-contract item 7, so QA has not waived
them. The builder must address the finding or obtain an explicit contract
clarification through the lead.

No audit row exists by design (ping changes no state). HEAD/OPTIONS,
unacceptable Accept values, unknown paths, persistence, load, packaged jar
and image are not verified here. Duplicates, expiry, validation and rate
limits are outside this slice. App PID 40179 stopped with SIGTERM; the log
records graceful shutdown complete. Gradle's bootRun exit 143 reflects that
intentional stop, not failure of the separately completed check gate.

Proof-contract items 1–6 have evidence; item 7 remains unsatisfied. No proof
acceptance judgments are recorded at qa_check; those belong to slice_accept.

## Media

![qa-check.txt](qa-check.txt)
![qa-tests.txt](qa-tests.txt)
![qa-ping-first.txt](qa-ping-first.txt)
![qa-ping-second.txt](qa-ping-second.txt)
![qa-ping-canaries.txt](qa-ping-canaries.txt)
![qa-ping-post.txt](qa-ping-post.txt)
![qa-ping-log-line.json](qa-ping-log-line.json)
![qa-http-start.txt](qa-http-start.txt)
![qa-http-end.txt](qa-http-end.txt)
![qa-tomcat-bind-name.txt](qa-tomcat-bind-name.txt)
![qa-bootrun.txt](qa-bootrun.txt)
