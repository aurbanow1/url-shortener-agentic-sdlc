---
slice: OPR.99.0.1.1
candidate_sha: f286a10863e4a8081235226f2d56e51ac121b319
artifact_type: qa
verdict: PASS
money_evidence: Bound to 127.0.0.1, GET /api/ping answered 200 with X-Request-Id
  73fa1c4b-e359-4f7f-87ec-40cc5f570dcb and the same id is on one ECS JSON log
  line whose process member is pid plus an empty thread object, with no address
  in any form anywhere in the log
evidences:
  - "6"
  - "7"
self_check: I opened the three f286a10 captures and confirmed the X-Request-Id
  in the GET capture equals the requestId in the log line, the log line has no
  process.thread.name member, the POST body is an RFC 9457 problem detail, and a
  grep of the whole loopback-bound bootRun stdout for the canaries and for
  127.0.0.1, ::1 and 0:0:0:0:0:0:0:1 returned no match
---

# Builder evidence — 01-ping, candidate f286a10863e4a8081235226f2d56e51ac121b319 (re-check after QA-01)

Produced by the development agent from the running candidate bound to 127.0.0.1 on port 18082, exercised through scripts/http. Independent verification is QA's; this drop attaches the by-effect captures for proof-contract items 6 and 7 on the new candidate.

- ping-get-exchange-f286a10.txt: GET /api/ping returned 200, application/json, X-Request-Id 73fa1c4b-e359-4f7f-87ec-40cc5f570dcb, body {"status":"ok","time":"2026-10-03T00:37:52.911465Z"} (AC-1, AC-2, AC-3). The request's inbound X-Request-Id canary was not echoed (AC-8).
- ping-post-405-exchange-f286a10.txt: POST /api/ping returned 405 application/problem+json with X-Request-Id and no stack trace (AC-5).
- ping-log-line-f286a10.json: the single ECS JSON line carrying that request id; requestId equals the header; process is {"pid":10917,"thread":{}} with no thread name; no client address or user agent (AC-6, AC-7). The whole loopback-bound stdout was grepped for the canaries and for 127.0.0.1, ::1 and 0:0:0:0:0:0:0:1: no match.
- builder-check-f286a10.txt: scripts/gw check on the candidate with every task re-executed; 6 unit and 9 functional tests green, 100% line and branch.

QA-01 addressed by excluding process.thread.name from the structured log (one line, logging.structured.json.exclude, key verified in the Boot 4.1.1 configuration metadata) and asserting its absence in the AC-7 journey, which was watched failing before the property existed. Details: PROOF.md, section "Builder re-check after QA-01".

## Media

![ping-get-exchange-f286a10.txt](ping-get-exchange-f286a10.txt)
![ping-post-405-exchange-f286a10.txt](ping-post-405-exchange-f286a10.txt)
![ping-log-line-f286a10.json](ping-log-line-f286a10.json)
![builder-check-f286a10.txt](builder-check-f286a10.txt)
