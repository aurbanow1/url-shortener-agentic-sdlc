---
slice: OPR.99.0.3.1
candidate_sha: 35590f06c852543c29097a42c43b7802be90ba40
artifact_type: qa
verdict: PASS
money_evidence: shipped f6dd29e jar wrote links, clicks and audit rows; the
  candidate jar on the same directory kept stats at 2 clicks, 410 and 302
  unchanged, and GET /api/audit returned the 3 pre-existing rows as stored;
  limit=2 paged via next Mg to null; a forwarded request was 403; one
  request-completed line per request and 0 client values or audit content in the
  run log
evidences:
  - "7"
  - "8"
self_check: "I opened every capture: the shipped stats and the candidate stats
  are identical, the 410 and 302 Location match, the audit page lists the three
  rows the shipped jar wrote with their request ids; page 1 holds two rows and
  next Mg, page 2 one row and next null; the 403 body is instance, status and
  title only; the log-lines file has the three request ids; the grep of the
  whole run log returned 0"
---

# Builder evidence — 01-audit-read, candidate 35590f0

This is evidence from the builder seat, `development-agent@urlshort-factory`, not a verdict. QA and review judge independently. The narrative, commands and deviations are in `PROOF.md` §Builder.

- **Contract item 7 (AC-18 by effect).**
  - The jar built from `f6dd29e` wrote a fresh data directory: two links, one retired; two clicks; statistics at 2 clicks; `GET /api/audit` `404`. It was stopped with SIGTERM.
  - The candidate jar started on the same directory. Flyway validated 2 migrations and applied none.
  - Statistics unchanged; retired link `410`; active link `302` to the same `Location`.
  - `GET /api/audit` returned the three pre-existing rows as stored, newest first.
- **Contract item 8 (AC-2, AC-6, AC-13, AC-15, AC-16 by effect).**
  - `limit=2` returned two rows and `next` `Mg`; following `next` returned the last row with `next` `null`.
  - A forwarded request (`X-Forwarded-For` plus a canary `User-Agent`) answered `403 {instance, status, title}`.
  - Each request has one `request completed` log line with its `requestId`.
  - The whole run log has 0 matches for the forwarded address, the canary, URLs, the `state` content, the cursor or the loopback address.
- **Supporting.**
  - `builder-check-35590f0.txt`: full gate `--rerun-tasks`. Unit 200, functional 200, 492/492 lines and 190/190 branches merged, Javadoc green.
  - `ac17-shipped-suite-on-35590f0.txt`: the shipped functional suite run unchanged against the candidate. 155 run; the only 2 failures are the granted enumeration assertions.
  - The red runs: `builder-red-tests.txt` and `builder-red-functional.txt` (47 of 52 failed before the controller existed).
  - The failed gate on `a47bee7`: `builder-check-a47bee7.txt`.

## Media

![upgrade-0-shipped-f6dd29e.txt](upgrade-0-shipped-f6dd29e.txt)
![upgrade-1-stats-35590f0.txt](upgrade-1-stats-35590f0.txt)
![upgrade-2-retired-410-35590f0.txt](upgrade-2-retired-410-35590f0.txt)
![upgrade-3-redirect-302-35590f0.txt](upgrade-3-redirect-302-35590f0.txt)
![upgrade-4-audit-35590f0.txt](upgrade-4-audit-35590f0.txt)
![http-200-page1-35590f0.txt](http-200-page1-35590f0.txt)
![http-200-page2-35590f0.txt](http-200-page2-35590f0.txt)
![http-403-forwarded-35590f0.txt](http-403-forwarded-35590f0.txt)
![log-lines-35590f0.txt](log-lines-35590f0.txt)
![jar-log-35590f0.txt](jar-log-35590f0.txt)
![builder-check-35590f0.txt](builder-check-35590f0.txt)
![ac17-shipped-suite-on-35590f0.txt](ac17-shipped-suite-on-35590f0.txt)
![builder-red-tests.txt](builder-red-tests.txt)
![builder-red-functional.txt](builder-red-functional.txt)
![builder-check-a47bee7.txt](builder-check-a47bee7.txt)
