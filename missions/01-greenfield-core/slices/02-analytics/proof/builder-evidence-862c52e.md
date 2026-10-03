---
slice: OPR.99.0.2.2
candidate_sha: 862c52eea8294e438b1f98b832ae4f64f7a16923
artifact_type: qa
verdict: PASS
money_evidence: "bootRun on 862c52e: three redirects with UA, Referer path/query
  and X-Forwarded-For canaries answered 302 with no-store; stats answered
  totalClicks 3 with referrer https://proof.example; the three click rows hold
  origin, class browser and one 64-hex hash, no canary; one correlated log line
  per request and no canary, address or hash anywhere in the log"
evidences:
  - "6"
  - "7"
  - "8"
self_check: "I opened every capture: the three 302s carry Location,
  Cache-Control no-store and distinct X-Request-Ids; the stats body has exactly
  four fields with totalClicks 3 and the origin only; the click rows show the
  reduced values and the same hash; the four log lines carry exactly the
  captured ids; greps of the run log for the canaries, proof.example,
  192.0.2.88, the loopback address, curl/ and the stored hash each returned 0"
---

# Builder evidence — 02-analytics, candidate 862c52e

Builder seat `development-agent@urlshort-factory`. Not a verdict: QA and review judge independently.
Narrative, commands and deviations: `PROOF.md` §Builder.

- Contract item 6: captured exchange on bootRun — three `302` redirects (`X-Request-Id`, `Location`,
  `Cache-Control: no-store`) sent with a `User-Agent`, a `Referer` carrying path and query canaries and an
  `X-Forwarded-For` canary, then the statistics `200` with `totalClicks` 3, one day, referrer
  `https://proof.example`, four fields only.
- Contract item 7: the three stored click rows — origin `https://proof.example`, class `browser`, one
  64-hex hash for the day, no canary and no raw address in any column.
- Contract item 8: the four JSON log lines, each `requestId` equal to its captured header; the whole run
  log contains no canary, forwarding value, address, referrer origin or stored hash.
- Supporting: `builder-check-862c52e.txt` (full gate `--rerun-tasks`, unit 121/121, functional 126/126,
  100 % line/branch), `bootrun-log-862c52e.txt` (the full run log), `http-201-create-862c52e.txt`.

## Media

![http-201-create-862c52e.txt](http-201-create-862c52e.txt)
![http-302-redirect-1-862c52e.txt](http-302-redirect-1-862c52e.txt)
![http-302-redirect-2-862c52e.txt](http-302-redirect-2-862c52e.txt)
![http-302-redirect-3-862c52e.txt](http-302-redirect-3-862c52e.txt)
![http-200-stats-862c52e.txt](http-200-stats-862c52e.txt)
![click-rows-862c52e.txt](click-rows-862c52e.txt)
![log-lines-862c52e.txt](log-lines-862c52e.txt)
![bootrun-log-862c52e.txt](bootrun-log-862c52e.txt)
![builder-check-862c52e.txt](builder-check-862c52e.txt)
