---
slice: OPR.99.0.1.1
candidate_sha: f286a10863e4a8081235226f2d56e51ac121b319
artifact_type: qa
verdict: PASS
money_evidence: All 8 ACs observed over loopback HTTP; QA-01 resolved; 6 unit
  and 9 functional tests pass; merged coverage 15/15 lines with zero branches.
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
self_check: Read fresh JUnit and all CSVs, checked exact response bodies and
  requestId-matched JSON events, found no canary or loopback address in
  startup-through-shutdown logs, confirmed app stopped and candidate unchanged.
---

# QA coverage — 01-ping

Candidate: `f286a10863e4a8081235226f2d56e51ac121b319` (`slice/01-ping`).
Independent QA: `qa-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Verdict: **PASS**. All eight ACs observed over live HTTP; QA-01 resolved.
Previous NOT-CLEAR evidence remains in the slice's `proof/qa-evidence.md`
and the finding history in `docs/qa/01-ping/findings.md`.

## Gate and coverage

From main, using its current log wrapper against the exact candidate project:

```sh
scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-check-f286a10.txt --offline -p .worktrees/01-ping check --rerun-tasks
```

Exit 0, BUILD SUCCESSFUL in 18s, all 13 tasks executed. Unit: 6 tests;
functional: 9 tests, including all eight named AC journeys. Zero failures,
errors or skips. Method inventory: `proof/qa-tests-f286a10.txt` under the
slice. All three reports are from this independent candidate run.

| Suite | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---|
| Unit | 15 / 15 | 100% | 0 / 0 | N/A: no branches |
| Functional | 13 / 15 | 86.67% | 0 / 0 | N/A: no branches |
| Merged | 15 / 15 | 100% | 0 / 0 | N/A: no branches; 100% threshold passes |

Counts read from `docs/qa/coverage/01-ping/{unit,functional,all}/jacocoTestReport.csv`.
HTML (`html/index.html`) and XML are present for each suite. No exclusions.
The two functional-only missed lines are `UrlshortApplication.main`, covered
by unit tests. That informational shortfall does not lower the merged gate.
Zero branches is not a claim of exercised branch logic.

## By-effect results

App startup command:

```sh
scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-bootrun-f286a10.txt --offline -p .worktrees/01-ping bootRun --args='--server.address=127.0.0.1 --server.port=18081 --spring.datasource.url=jdbc:h2:mem:qa-ping'
```

Loopback-only app; isolated in-memory H2; no logging override. Four requests
used `scripts/http`: two consecutive ordinary GETs, a GET with both canaries,
and POST. All following artifacts are under
`missions/00-hello/slices/01-ping/proof/`.

| AC | Observed | Evidence |
|---|---|---|
| AC-1 | 200 application/json, exactly status=ok and time | qa-ping-first-f286a10.txt |
| AC-2 | Each GET time parses as UTC, ends in Z, includes seconds, falls within recorded request bounds | qa-http-start-f286a10.txt, qa-http-end-f286a10.txt, GET captures |
| AC-3 | Header is 36 printable non-whitespace ASCII characters | qa-ping-first-f286a10.txt |
| AC-4 | Consecutive ids 0c1ecb83-71a0-4a98-af62-9df9a5c5a6e0 and e1f3f111-9ecc-4a68-ad81-fc16b976f62f differ | qa-ping-first-f286a10.txt, qa-ping-second-f286a10.txt |
| AC-5 | POST: 405 application/problem+json, status=405, title and instance, no stack trace/exception name, id present | qa-ping-post-f286a10.txt |
| AC-6 | Every GET has a single-line JSON event matching its response requestId | qa-ping-log-line-f286a10.json, qa-bootrun-f286a10.txt |
| AC-7 | No user-agent canary, IPv4/IPv6 loopback address or thread-name member in logs; original failure absent with same bind | qa-ping-canaries-f286a10.txt, qa-bootrun-f286a10.txt |
| AC-8 | Returned id differs from inbound canary; canary absent from all logs | qa-ping-canaries-f286a10.txt, qa-bootrun-f286a10.txt |

All eight results PASS. Entire startup-through-shutdown output checked for
both canaries and loopback forms. First JSON event has `process.thread = {}`,
matching the lead's narrow exclusion decision. AC-2 bounds include approval
latency; no performance claim is made.

## Limits and follow-up

No audit row applies: ping has no persistence. Validation, duplicates, expiry
and rate limits are outside scope. HEAD/OPTIONS, unknown paths, Accept
negotiation, load, packaged jar/image and persistence were not checked.
PID 23990 stopped with SIGTERM; graceful shutdown recorded. bootRun exit 143
is this intentional stop, separate from the green check gate.

Nonblocking documentation follow-up already flagged by the builder: ADR-0004
and docs/DESIGN.md log examples still show the excluded thread name. The
design owner should reconcile them at integration; the locked design sample
is historical. No AC or proof-contract item remains unverified. QA proof
covers items 1–7; attributed acceptance judgments remain for slice_accept.

## Media

![qa-check-f286a10.txt](qa-check-f286a10.txt)
![qa-tests-f286a10.txt](qa-tests-f286a10.txt)
![qa-ping-first-f286a10.txt](qa-ping-first-f286a10.txt)
![qa-ping-second-f286a10.txt](qa-ping-second-f286a10.txt)
![qa-ping-canaries-f286a10.txt](qa-ping-canaries-f286a10.txt)
![qa-ping-post-f286a10.txt](qa-ping-post-f286a10.txt)
![qa-ping-log-line-f286a10.json](qa-ping-log-line-f286a10.json)
![qa-http-start-f286a10.txt](qa-http-start-f286a10.txt)
![qa-http-end-f286a10.txt](qa-http-end-f286a10.txt)
![qa-bootrun-f286a10.txt](qa-bootrun-f286a10.txt)
