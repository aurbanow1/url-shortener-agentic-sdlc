---
slice: OPR.99.0.1.1
candidate_sha: 3886a04a4afac6117038b2884cf72749f57d28aa
artifact_type: qa
verdict: PASS
money_evidence: GET /api/ping on the running candidate answered 200 with
  X-Request-Id 7f88e171-58cf-4859-b21f-f271b3e9e9a5 and the same id is on one
  ECS JSON log line with no client address or user agent
evidences:
  - "6"
  - "7"
self_check: I opened ping-get-exchange.txt, ping-post-405-exchange.txt and
  ping-log-line.json and confirmed the X-Request-Id in the GET capture equals
  the requestId in the log line, the POST body is an RFC 9457 problem detail,
  and a grep of the whole bootRun stdout for the canaries and the loopback
  address returned 0
---

# Builder evidence — 01-ping, candidate 3886a04a4afac6117038b2884cf72749f57d28aa

Produced by the development agent from the running candidate (bootRun on port 18080), not from the test suites. Independent verification of the slice is QA's; this drop attaches the by-effect captures for proof-contract items 6 and 7.

- ping-get-exchange.txt: GET /api/ping returned 200, application/json, X-Request-Id 7f88e171-58cf-4859-b21f-f271b3e9e9a5, body {"status":"ok","time":"2026-10-02T23:43:51.496001Z"} (AC-1, AC-2, AC-3). The request carried an inbound X-Request-Id canary that was not echoed (AC-8).
- ping-post-405-exchange.txt: POST /api/ping returned 405 application/problem+json with X-Request-Id and no stack trace (AC-5).
- ping-log-line.json: the single ECS JSON line carrying that request id; requestId equals the response header; no client address or user agent (AC-6, AC-7). The whole stdout was grepped for the canaries and the loopback address: 0 hits.
- builder-check.txt: scripts/gw check on the candidate with every task re-executed; 6 unit and 9 functional tests green, 100% line and branch.

Commands, counts and what was not verified: PROOF.md, section Builder.

## Media

![ping-get-exchange.txt](ping-get-exchange.txt)
![ping-post-405-exchange.txt](ping-post-405-exchange.txt)
![ping-log-line.json](ping-log-line.json)
![builder-check.txt](builder-check.txt)
