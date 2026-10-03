---
slice: OPR.99.0.2.3
candidate_sha: b3370ef
artifact_type: qa
verdict: PASS
money_evidence: "reviewer's limiter probe on b3370ef: 60 admissions in the
  reordering case (was 120) and 2 clients after the rollback cleanup case (was
  10002); smoke R0 control 6/6, a header-only 201 is now rejected (curl exit
  18); --drain on the jar: R0 201 complete 521 ms after SIGTERM, probe refused,
  0 failures"
evidences:
  - "9"
  - "12"
self_check: "I read each output file: the probe prints totalAdmissions=60 and
  clientsAfter61SecondsAndRequest=2; the control prints 6/6 with the expected
  curl exit per case and headers held about 0.5 s before each body completed;
  the drain output shows R0 curl exit 0 status 201 and SMOKE DRAIN OK; the gate
  log ends BUILD SUCCESSFUL after check with --rerun-tasks; the ancestry command
  exited 0"
---

# Builder evidence — 03-operate rework, candidate b3370ef

This is evidence from the builder seat, `development-agent@urlshort-factory`, not a verdict. QA and review re-check the candidate independently. The finding-by-finding answers, commands and the disclosed clock limit are in `PROOF.md` §Builder, *Rework — b3370ef*.

- **CR-01 / SEC-01 (limiter reordering).** The reviewer's unchanged probe now gives `totalAdmissions=60 expectedMaximum=60` (it gave 120 on `a7c533f`). `RateLimiterTest` reproduces the same ordering deterministically.
- **CR-03 (release after a backward step).** The same probe now gives `clientsAfter61SecondsAndRequest=2` (it gave 10,002).
- **Contract item 12 / CR-02 (smoke R0).**
  - The R0 control runs the candidate's own `r0_open` and `r0_finish` against loopback peers. A complete 201 passes. These are rejected:
    - a header-only 201 (curl exit 18);
    - a short body (18);
    - a close with no response (52);
    - a complete 500;
    - a complete 201 sent 11 s after the stop.
  - `--drain` on the candidate jar: R0 curl exit 0, status 201, complete 521 ms after SIGTERM. The probe was refused. 64 ok, 20 refused, 0 boundary losses, 0 failures.
- **Contract item 9.** `git merge-base --is-ancestor 091ff46 b3370ef` succeeds.
- **Gate.** `check --rerun-tasks`: unit 165, functional 155, 443/443 lines, 162/162 branches, Javadoc green.

## Media

![rate-boundary-b3370ef.txt](rate-boundary-b3370ef.txt)
![smoke-r0-control-b3370ef.txt](smoke-r0-control-b3370ef.txt)
![smoke-r0-control.py](smoke-r0-control.py)
![smoke-drain-b3370ef.txt](smoke-drain-b3370ef.txt)
![builder-check-b3370ef.txt](builder-check-b3370ef.txt)
