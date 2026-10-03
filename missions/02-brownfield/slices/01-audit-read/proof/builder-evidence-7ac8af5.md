---
slice: OPR.99.0.3.1
candidate_sha: 7ac8af56ed04c27bbefbd416b3976c544d2f274a
artifact_type: qa
verdict: PASS
money_evidence: "CR-01: with server.tomcat.remoteip.remote-ip-header or
  protocol-header set, GET and HEAD, plain and with a forged X-Forwarded-For
  127.0.0.2, answer 403 with no stored canary on a real Tomcat (the plain read
  answered 200 on 35590f0's code); gate green, unit 203, functional 202, 494/494
  lines, 194/194 branches"
self_check: I read the red log (both remoteip settings failed on the old guard)
  and the gate log on 7ac8af5 (BUILD SUCCESSFUL after check with --rerun-tasks);
  the test asserts 403 and the absence of the stored canary for both methods and
  both header cases under each setting
---

# Builder evidence — 01-audit-read CR-01 rework, candidate 7ac8af5

This is evidence from the builder seat, `development-agent@urlshort-factory`, not a verdict. The finding answer is in `PROOF.md` §Builder, *Rework — 7ac8af5*.

- **CR-01 (remoteip settings reopened the read).** The guard now admits a request only when the forwarded-header strategy is `NONE` and neither `server.tomcat.remoteip.remote-ip-header` nor `protocol-header` has text. This follows design `0052efb` §1.
- **Red before the fix.** On `35590f0`'s code, the real-Tomcat regression failed for both settings: a plain loopback read answered `200` (`builder-red-cr01-remoteip.txt`).
- **Green on `7ac8af5`.** `GET` and `HEAD`, plain and with a forged `X-Forwarded-For: 127.0.0.2`, answer `403` with no stored canary under each setting.
- **Gate.** `check --rerun-tasks`: unit 203, functional 202, 494/494 lines and 194/194 branches merged, Javadoc green (`builder-check-7ac8af5.txt`).

## Media

![builder-red-cr01-remoteip.txt](builder-red-cr01-remoteip.txt)
![builder-check-1fe1cbf.txt](builder-check-1fe1cbf.txt)
![builder-check-7ac8af5.txt](builder-check-7ac8af5.txt)
