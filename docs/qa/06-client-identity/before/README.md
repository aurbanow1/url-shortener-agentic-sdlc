# Client Identity — installed-jar baseline

QA2 completed pre-work packet `qitem-20261004004922-defcdcb0`. This is a
before-capture, not the slice's `qa_check` or an acceptance judgment.

Actual build baseline: **`b68ff80c6d132bee5f120174da9423e7e53f77e3`** on main.
The earlier identity/read observed `fb3468f`; documentation commits advanced
main before the capture was prepared. `source.json` records the actual pinned
SHA. Its product tree is identical to dispatched `5cfdf8a`:
`50387c729aa575a5eebc505f86c542ed22ff2995`.

Built with `scripts/gw --log docs/qa/06-client-identity/before/bootJar.txt
--offline bootJar --rerun-tasks`: exit 0, four tasks executed. The jar copied
to private scratch and executed in every scenario has SHA-256
`fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2`.
Java is the wrapper's JDK 21. No application fixture, injected clock, simulated
Servlet peer, source edit or test edit was used. Each process has its own real
H2 directory and binds only `127.0.0.1` on an available port.

Final capture: **59 requests, 336 assertions, no failed assertion**.
`summary.json` gives the run times and ending main `6772b68`.
The product tree, committed API document and build file have no difference
between the named build baseline and ending main. `custody.json` records a
further pre-commit check. Documentation-only movement does not require
recapturing this product baseline.

| Observed requests/configuration | Effect |
|---|---|
| Shipped settings, plain loopback audit read, after an actual create | 200 JSON page with the created audit row |
| Shipped settings, X-Forwarded-For or Forwarded present; separately remote, forged loopback and explicitly empty values | 403 problem, no trail |
| Same audit cases with connection peer 127.0.0.1 trusted for rate limiting | Same admission/refusals |
| Strategy none, remote-ip-header enabled alone | Headerless/forged-header GET and HEAD all 403; HEAD body empty |
| Strategy none, protocol-header enabled alone | Same four refusals |
| Both Tomcat remoteip header settings enabled | Same four refusals |
| Strategy native, then framework, separate real servers | Same four refusals per configuration |
| Kubernetes platform detected, shipped none pin retained | Plain loopback 200; forged X-Forwarded-For 403 |
| Create/API budget set to 2, no trusted proxy; two 404 reads with changing identity headers, then another changed header | 429 application/problem+json; Retry-After 30 |
| Create/API budget set to 2, trusted 127.0.0.1; two reads charged to the same forwarded client | Same 429 problem and Retry-After 30; distinct forwarded client still receives its ordinary 404 |
| Two successful browser redirects from actual 127.0.0.1, one with untrusted forwarding | totalClicks 2, uniqueVisitors 1, botClicks 0 |
| Trusted proxy; forwarded A, chain ending in A, then distinct B | totalClicks 3, uniqueVisitors 2, botClicks 0 |
| Live /v3/api-docs | Semantically equal to the byte-preserved committed docs/api/openapi.json |

The statistics reads waited for the expected asynchronous click count, with
an eight-second deadline; both settled on the first read. Each day's
statistics preserve the four-field public response and referrer origin
`https://ref.example`, without its submitted private path/query/fragment.
Actual 429 bodies have exactly `instance`, `status`, `title`; `instance`
uses the response's server-issued request ID. Refused audit GETs have the
same problem correlation. Raw headers, including content type, cache headers,
Location, Retry-After and body framing, remain in the `.http` files.

For each numbered request, `.json` records the exact curl arguments, method,
path, status, headers and parsed body; `.http` preserves the received bytes;
`.trace` shows the transmitted request. `*-launch.json` records every JVM
argument and process. `*.jsonl` contains complete startup/request/shutdown
output. `request-log-joins.json` preserves **all** events matching each
response request ID: 59 distinct IDs, exactly one `request completed` with
the matching status per request, 60 correlated events in total. The extra
event is springdoc's initialization-duration message during the API read;
it is retained. Request logs contain none of the tested address, full browser
agent or private referrer canaries.

## Instrument qualifications and later comparison

The first sandbox attempt could not bind a socket (`Operation not permitted`),
so the same driver was rerun through approved outside-sandbox execution.
`attempt-01-curl-whitespace/` retains the interrupted run: curl omitted an
all-space header and the real server admitted its resulting headerless read.
The final `instrument-curl-whitespace-omitted` trace independently confirms
the omission. This does **not** claim that a transmitted whitespace-only
header is admitted. An explicitly empty header sent with curl's semicolon
syntax was present and refused. The full SPEC whitespace matrix remains for
characterization and candidate QA.

`attempt-02-source-address/` preserves curl's failed attempt to bind source
127.0.0.2 (exit 45, errno 49). That request did not reach Tomcat. No direct
distinct-source-IP claim is made on this host; distinct actual forwarded
clients are exercised through the trusted localhost proxy configuration.
Both interrupted processes stopped and their ports closed. Interrupted
captures are not included in the final 59/336 count.

At the assigned `qa_check`, replay the named scenarios against the exact
candidate and compare them with these raw results. The driver is preserved
for that replay; use a separate after directory and candidate jar rather than
overwriting this baseline. The candidate's full AC matrix, coverage,
traceability, store-failure cases and structural proof are still required.

Run-specific values present here are generated codes, request IDs, response
and audit timestamps, the observed UTC statistics day, HTTP Date, ephemeral
port/path/PID and log timestamps. Any comparison substitution must be listed
and preserve code/ID references, header/body/log correlation and equality
relationships. Preserve status, header presence, target Location, problem
fields/stable values, Retry-After, audit decision, counts/grouping and event
level/message/count. Keep the springdoc duration event visible when assessing
differences; it has a run-specific duration inside its message. No comparison
or normalization has been performed in this before-only packet.

## Self-check

- Read the packet as a pre-work assignment; no proof item was judged in advance.
- Read the captured audit pages/problems, both 429s, both settled statistics
  and correlated log events; all requested CR-01 settings ran on real Tomcat.
- Saved committed and live API documents, raw HTTP/wire/log captures, commands,
  source SHA/tree and actual executed jar hash. Saved both interrupted attempts.
- Checked source/API/build custody and all evidence hashes before committing;
  `artifact-hashes.json` excludes itself and the later validation receipt.
- Verified every owned process stopped and every chosen port refused a
  connection. No owner application, product source, build file or test changed.
- Full candidate QA and the before/after comparison remain for the workflow's
  later assigned `qa_check`; no full AC or coverage verdict is asserted here.
