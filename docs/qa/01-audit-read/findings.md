# QA finding — audit-read candidate 35590f0

Candidate: `35590f06c852543c29097a42c43b7802be90ba40`.
Status: **FAIL; original QA PASS superseded for the access boundary.**
Review Agent found the missing configuration axis after QA handoff. QA then
reproduced it independently on the preserved unmodified candidate jar,
SHA-256 `bd21c00e5e6471880153ba24bc1119992ce7e53859e513f0c4d2cbb452f29acb`.
The original 200-unit/200-functional gate and coverage remain actual results;
they do not prove the unmet access requirements.

## QA-AUD-01 — explicit Tomcat remote-IP settings bypass header refusal

- **Severity:** HIGH.
- **Criteria:** AC-13, AC-14; rule 2, NFR-S6; rejected proof item 1. Security
  item 12 remains unaccepted. This is a product defect, not an accepted gap.
- **Expected:** Any audit request containing X-Forwarded-For or Forwarded
  is refused with403. No operator setting opens the read; HEAD follows the
  same refusal status. A strategy value of NONE must not authorize an audit
  read when the container still rewrites its peer/header inputs.
- **Observed:** Default candidate: forged loopback XFF GET/HEAD403. With either
  `server.tomcat.remoteip.remote-ip-header=X-Forwarded-For` or
  `server.tomcat.remoteip.protocol-header=X-Forwarded-Proto`, the same GET is
  200 and returns the stored audit target canary; HEAD is200. XFF with the
  non-loopback value198.51.100.9 still returns403. All15 requests have correlated
  X-Request-Id and JSON completion status.

Repro on the preserved candidate, with Java21 and an otherwise default app:

```sh
/opt/homebrew/opt/openjdk@21/bin/java -jar /private/tmp/urlshort-audit-qa-35590f0/candidate-35590f0.jar --server.address=127.0.0.1 --server.port=18134 --spring.datasource.url='jdbc:h2:mem:qa-remoteip-repro;MODE=PostgreSQL' --server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
scripts/http -i -H 'Content-Type: application/json' --data-binary @docs/qa/01-audit-read/post-review-remoteip/create.request http://127.0.0.1:18134/api/links
scripts/http -i -H 'X-Forwarded-For: 127.0.0.2' http://127.0.0.1:18134/api/audit
scripts/http --head -H 'X-Forwarded-For: 127.0.0.2' http://127.0.0.1:18134/api/audit
```

Stop that app. Repeat on a fresh database replacing the last startup option
with `--server.tomcat.remoteip.protocol-header=X-Forwarded-Proto`. Repeat
without either override as the403 control. Exact commands, headers/bodies,
15 statuses and correlation verification are under `post-review-remoteip/`.
Raw captures have an archive and hash manifest; displayed headers only strip
CRLF/trailing blank whitespace.

The Review Agent's actual-class controls independently observe effective
strategy NONE in both failing variants:
`docs/review/01-audit-read/proof/code-controls-35590f0.txt` and
`AuditCodeControls.java`. The candidate controller currently checks the
strategy enum plus the Servlet peer/headers; the reported container rewrite
occurs before those checks. QA's prior configurations exercised native and
framework strategies but omitted these explicit RemoteIpValve properties.

The builder owns the fix and regression tests on a new candidate. QA does not
edit product or tests, narrow the requirement, or accept this defect as a gap.
Review owns routing its HIGH finding back to implement; no duplicate workflow
packet is created by this evidence correction. New QA work requires the new
candidate packet. No claim about an actual external TCP client is made here:
the independently observed failure is the explicit forwarding-header refusal
required even for a loopback connection.

## Self-check

Inspected review probe source and complete control output, then used the
original jar independently with default and both explicit settings. Verified
returned canary, GET/HEAD status changes and all15 request/log correlations.
Stopped all three repro apps; port18134 refused afterwards. Product untouched;
historical coverage, upgrade and read-only evidence remain preserved.

## Re-check 7ac8af56ed04c27bbefbd416b3976c544d2f274a

QA-AUD-01 / CR-01 HIGH — **fixed on this candidate**.
Independent fresh gate203 unit/202 functional, merged494/494 lines194/194
branches. Actual candidate jar SHA256
48fca1b85ef5e78f0b74e15b5f24d9f612bab544bc53dd6b4d5cba46f9b28a7f
reproduces the original inputs:
- remote-ip-header=X-Forwarded-For: plain/forged GET403 and HEAD403.
- protocol-header=X-Forwarded-Proto: plain/forged GET403 and HEAD403.
- Default and explicit empty settings: plain GET/HEAD200, forwarding403.
- native/framework: plain/forged GET/HEAD403.
No refused response contains stored canary or items. Each configuration
created a link first so the trail was nonempty. Exact launch arguments and
curl responses are in slice proof/qa-recheck-7ac8af5/
(installed-launch-arguments.json; qa-http/remoteip-* and strategy-*).
The new two-invocation real-container regression passes independently;
no gap exception or product edit by QA. The prior rejection remains an
accurate judgment of35590f0. Item12 awaits independent security re-review.
