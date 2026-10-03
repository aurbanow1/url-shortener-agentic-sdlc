---
slice: OPR.99.0.2.3
candidate_sha: a7c533ffef55650e5b422377ffe0c4e38d41400c
artifact_type: qa
verdict: PASS
money_evidence: "candidate jar with budgets 2/3 per minute: 201, then
  create/read/redirect 429s with Retry-After, X-Request-Id and plain
  application/problem+json, no client value echoed; readiness and liveness UP;
  Prometheus rejection counter create 2.0 redirect 1.0; one correlated log line
  per 429 and no marker or address in the log; candidate descends from 02's
  merge 091ff46"
evidences:
  - "6"
  - "7"
  - "9"
  - "12"
self_check: "I opened every capture: the three 429s carry Retry-After, their own
  X-Request-Id, Content-Type application/problem+json without a charset and a
  body with only instance, status and title; liveness and readiness bodies are
  status UP; the scrape excerpt shows the counter per budget and the redirect
  route template; the log lines carry exactly the three ids; greps of the run
  log and the scrape for the markers, the loopback address and curl/ returned 0;
  the ancestry command exited 0; the drain and bench outputs are the recorded
  runs"
---

# Builder evidence — 03-operate, candidate a7c533f

Builder seat `development-agent@urlshort-factory`. Not a verdict: QA (qa2-agent) and review (review2-agent)
judge independently. Narrative, commands and deviations: `PROOF.md` §Builder.

- Contract item 6: on the candidate jar with the operator budgets at 2 creates / 3 redirects per minute — an
  admitted `201`, then `429`s for a create, a read and a redirect, each with `Retry-After`, `X-Request-Id`,
  `application/problem+json` and the server-owned body; liveness and readiness `UP`; a Prometheus excerpt
  with `urlshort_ratelimit_rejections_total{budget="create"} 2.0` and `{budget="redirect"} 1.0`.
- Contract item 7: one `request completed` line per captured `429`, `requestId` equal to its header,
  status 429; no user-agent, forwarded, url marker or client address anywhere in the run log.
- Contract item 9: `git merge-base --is-ancestor 091ff46 a7c533f` succeeds (rebased onto 02's merge).
- Contract item 12: `scripts/smoke.sh` holds the release-level modes; `--drain` (R0 201, probe refused, 0
  failures, 0 boundary losses) and `--bench` (0 errors, achieved rates printed) ran on the candidate jar.
- Supporting: `builder-check-a7c533f.txt` (full gate `--rerun-tasks`: unit 163, functional 155, 100 %
  line/branch), the OSV runs (0 advisories), the red runs, `jar-log-a7c533f.txt`.

## Media

![http-201-create-a7c533f.txt](http-201-create-a7c533f.txt)
![http-429-create-a7c533f.txt](http-429-create-a7c533f.txt)
![http-429-read-a7c533f.txt](http-429-read-a7c533f.txt)
![http-429-redirect-a7c533f.txt](http-429-redirect-a7c533f.txt)
![http-200-liveness-a7c533f.txt](http-200-liveness-a7c533f.txt)
![http-200-readiness-a7c533f.txt](http-200-readiness-a7c533f.txt)
![prometheus-excerpt-a7c533f.txt](prometheus-excerpt-a7c533f.txt)
![log-lines-429-a7c533f.txt](log-lines-429-a7c533f.txt)
![jar-log-a7c533f.txt](jar-log-a7c533f.txt)
![smoke-drain-a7c533f.txt](smoke-drain-a7c533f.txt)
![smoke-bench-a7c533f.txt](smoke-bench-a7c533f.txt)
![builder-check-a7c533f.txt](builder-check-a7c533f.txt)
![osv-advisories-0b1f0d8.json](osv-advisories-0b1f0d8.json)
![runtime-dependencies-0b1f0d8.txt](runtime-dependencies-0b1f0d8.txt)
![builder-red-429-charset-on-tomcat.txt](builder-red-429-charset-on-tomcat.txt)
