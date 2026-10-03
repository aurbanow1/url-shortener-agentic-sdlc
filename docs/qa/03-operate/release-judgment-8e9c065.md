# 03-operate release judgment

QA2 (`qa2-agent@urlshort-factory`, Codex), 2026-10-03. Assigned packet
`qitem-20261003153203-9be9445d`; retained obligation
`qitem-20261003120849-f4cbfa97`.

**Proof item 13: REJECT.** AC-28 requires the held request R0 to receive its
complete normal response through the compose published port. All five recorded
Mac-host runs end with curl 52, HTTP status 000, after the shutdown phase.
The lead explicitly kept this criterion in transition 806 of
`qitem-20261003151205-a94f1d92`. A disclosed gap is evidence for the human's ship
decision; no waiver has been granted in this judgment.

**Proof item 5: ACCEPT, re-affirmed.** GAPS.md still names every release-level
criterion/NFR and its check, as well as the honest limitations. The release
appendix and subsequent counter/retained-evidence corrections improve that
disclosure. This accepts the gap record, without accepting AC-28.

## Subject and evidence inspected

Subject: `commit:8e9c065589e53385f60d6be3ddbc3683260285df`, the integration merge.
The release build used `f09010396d584fdf41702bb99856aa17a1ec1206`.
QA independently ran `git diff --name-only 8e9c065 f090103 -- src
build.gradle.kts settings.gradle.kts Dockerfile compose.yaml gradle gradlew
.dockerignore scripts docs/api`: empty. This checks the recorded build inputs,
without claiming the image's jar equals the host jar byte for byte.

Read RELEASE.md §3 and the raw smoke, inspect, restart, control, benchmark and
JSON log files under `missions/01-greenfield-core/release/`. The release record
was corrected at `3ec7ab48b1a8eb5771d3095c09f03a1080ce90f2`, after the packet's
initial `40067fc`. `release-audit-8e9c065.json` records hashes of 30 files at
3ec7ab4, parsed results, independent log counts and the empty build-input diff.
All raw release files match those committed bytes. Concurrent RELEASE/GAPS
clarification identifies that only one of the two reported namespace controls
has retained output and log; QA independently credits that one retained control.
Other seats' release/review edits and subsequent rollback work are outside this
judgment. The instrument changes are release tooling, not a new product build.

## AC results from recorded effects

| AC | QA judgment | Evidence and observed effect |
|---|---|---|
| 21 | Supported | `compose-up-f090103.txt`, `smoke-container-f090103.txt`, `smoke-inspect-container-f090103.txt`: compose becomes healthy; its readiness HTTP health check requires `UP`; default smoke checks liveness/readiness, metric names and Prometheus and passes. |
| 22 | Supported | Inspect: only `127.0.0.1:8080`, no wildcard binding. |
| 23 | Supported | Inspect: uid 10001, `ReadonlyRootfs=true`, named persistent writable volume at `/app/data`, tmpfs `/tmp`; touch under `/app` fails, under data succeeds and is removed. Default smoke passes. |
| 24 | Supported | Restart runs 3, 4 and 5 reach the persistence checks: after both restart and separate down/up, redirect is 302 to the original target and API body is unchanged. Runs 1/2 stopped early and supply no persistence evidence. |
| 25 | Supported for the recorded jar workload | Three `smoke-drain-f090103*.txt` captures: R0 complete 201 at 514/513/514 ms, new probe refused; complete/refused counts 64/18, 58/20, 63/17; zero reported boundary losses or failures. First retained jar log contains 66 correlated completion events: 64 load creates, R0 create and one readiness probe; graceful phase completes after about 562 ms. The script reconciles server ids with complete client responses. Higher-rate stop-window losses were not exercised here. |
| 26 | Supported | `smoke-jar-ac26-f090103.txt`: plain jar default journey passes; short URL uses overridden `https://sho.rt.example`; file exists at overridden data path; trusted proxy yields 10 admitted requests then 429 under create budget 10; redirect budget 100 is configured. No separate exact redirect-budget exhaustion check is claimed. |
| 27 | Supported on the recorded jar run | `bench-jar-f090103.txt` and `tools/bench.mjs`: open-loop 60-second offered 100 redirects/s and 20 creates/s, achieved 100.0/20.0, 6000/1200 requests, zero non-2xx/3xx. NFR-L1 redirect p95 2.2 ms ≤20 and p99 3.3 ms ≤50; NFR-L2 create p95 2.8 ms ≤50. |
| 28 | **NOT MET** | Five `smoke-restart-container-f090103*.txt` captures lose R0 through the Mac published port; timeout inspect 20 s >10 s passes; received load HTTP responses have zero non-2xx/3xx; corrected proxy failures are reported separately. |

## Finding QA-REL-01 — AC-28 held response lost (HIGH)

Reproduction recorded by release: start the compose stack and run
`scripts/smoke.sh --restart http://127.0.0.1:8080 <restart-log>` on macOS 15.2,
Apple M4 Pro, Lima Ubuntu 24.04 VM, Docker Engine 28.4. The script starts a real
chunked create, holds part of its request body, issues `docker compose restart`,
then supplies the rest. Run 5 uses the corrected one-row-per-attempt loop.

Expected: R0 receives its complete normal 2xx/3xx response. Observed: curl exit
52, status 000, at 10485/10489/10393/10370/10663 ms after the stop across runs
1–5. These independent R0 results are unaffected by the earlier load-row bug.
Run 4's JSON log shows graceful phase start at 15:02:29.748Z, a 10000 ms timeout,
request `d28cbe37-566e-43f8-b18c-fcccd9b7a325` completing as 400 at 15:02:39.773Z,
and graceful shutdown aborted with an active request. The client did not receive
that response. R0 is an explicit judged clause and cannot be grouped with the
unjudged proxy connection failures.

Runs 3/4 printed 250/251 rows and 204/206 failure rows. The old loop printed
`000` twice per failed attempt; the corrected arithmetic is 148/148 attempts
and 102/103 failures. Run 5 prints 149 attempts and 105 failures. All three runs
report zero received non-2xx/3xx responses and retain both persistence checks.
The release record preserves the old captures and explains the correction.

Two VM-published-port controls retain complete chunked 201 bodies and terminators,
matching request ids and graceful completion logs. One namespace control has
retained equivalent evidence; the first reported namespace run was overwritten.
These controls support isolation to the additional Mac-to-VM path. The specific
forwarder mechanism was not observed, and native Linux Docker was not tested.
They do not fulfill the literal Mac-published-port result.

## Benchmark scope and gaps

The generator schedules each request independently of earlier completion and
measures from its due time to full response completion, including queue delay.
QA parsed the complete output and independently compared its numbers with
NFR-L1/L2. Jar logs contain 1201 create completions (1200 plus seed), 18000
redirect completions (12000 GET plus 6000 HEAD) and four operator reads; the
statistics response stores exactly 12000 GET clicks. No request or click drop
is inferred from a green script alone. The wrongly configured first run is
retained and discarded. This is one run on one laptop; no container benchmark,
capacity result or independent NFR-L3 judgment is added.

## Self-check

The locked criterion, proof contract and actual lead decision were read; exact
merged/build identity and raw-file hashes checked; five R0 failures and passing
control bodies/logs inspected; original counter bug and its correction checked;
jar completion totals and benchmark counts/targets independently parsed; GAPS
disclosure compared with the earlier accepted claim. No new app, container,
build or benchmark was started for this receipt packet. Product source, tests,
build configuration and other seats' edits were untouched. Prior QA coverage
and traceability receipts remain scoped to their exact candidate. The attributed
judgments and NOT-CLEAR proof drop record this result; ship approval is pending
the human's existing gate.
