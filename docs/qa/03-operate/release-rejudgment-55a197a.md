# 03-operate release judgment after the human's AC-28 amendment

QA2 (`qa2-agent@urlshort-factory`, Codex), 2026-10-03; assigned judgment-only
packet `qitem-20261003171429-897aa374`. Subject:
`commit:8e9c065589e53385f60d6be3ddbc3683260285df`.

**Item 13: ACCEPT against amended AC-28 at `55a197a`.** This supersedes the
attributed rejection in receipt 27 for the original criterion; the original
failure and raw evidence remain intact. The host's R0 did not start succeeding.

## Authorization and criterion

Independently read gate `qitem-20261003165209-ce7abb0e` transitions: transition
1011 is attributed to `human@kernel` at 2026-10-03T17:11:19.239Z. It approves
shipping product 8e9c065, package 973bc1a, accepts the Mac published-port cut
as a disclosed host gap, directs that R0 be measured on the jar, in-VM Docker
published port and container namespace, and requires QA re-judgment before
delivery stamps. The SPEC diff at
`55a197a419e009c7ffe67441cf55123bd8864576` changes that R0 measurement and adds
the attributed A-19 decision. The complete received-response, proxy-count and
stop-timeout clauses remain unchanged. The lead re-stamped plan-lock at 461b689;
QA makes no delivery stamp.

## Evidence against the amended criterion

| Obligation | Retained effect and QA judgment |
|---|---|
| R0 on packaged jar | Three `smoke-drain-f090103*.txt` captures: complete 201, curl 0, 514/513/514 ms after SIGTERM; probe refused. First retained jar log shows graceful shutdown complete. Satisfied. |
| R0 on VM's Docker-published port | Both `r0-vm-published-port-f090103-run*.txt` captures retain normal chunked 201 JSON bodies, final zero chunk and successful reader exit. Response request ids match the VM log's 201 completions, followed by graceful completion within the 10-second phase. Satisfied. |
| R0 in container namespace | `r0-in-namespace-control-f090103.txt` retains complete chunked 201 and successful reader exit; id 32113e20-24cf-4568-93ae-e352d6391028 matches the namespace log's completion, followed by graceful shutdown complete about 3.8 seconds into the phase. One retained run establishes this effect; the first reported run's overwritten evidence is not independently credited. Satisfied. |
| Received load HTTP responses during compose restart | Run 6, using the corrected curl-exit-aware loop at 973bc1a: 149 attempts, zero non-2xx/3xx or cut-short responses, 103 proxy connection failures reported separately. R0's host cut remains disclosed under A-19. Satisfied. |
| Runtime stop timeout | `smoke-inspect-container-f090103.txt`: StopTimeout=20, greater than the shipped 10-second graceful phase. Satisfied. |
| Host-gap disclosure | Updated AC-28 GAPS row records the actual human decision, macOS/Lima/Docker host, all six failures, corrected counts, retained-control limit and raw paths; RISKS contains the gap and names its mechanism as unestablished. Satisfied. |
| Other item-13 criteria | Previously audited AC-21–27 effects retained; hardening, health, persistence and configuration records are unchanged. Jar bench offers/achieves 100 redirects/s and 20 creates/s for 60 seconds with zero errors; NFR-L1 redirect p95/p99 2.2/3.3 ms and NFR-L2 create p95 2.8 ms meet bounds. No container bench or capacity claim. |

RELEASE.md §3 is the source of these installed-effect captures. The earlier
audit at `docs/qa/03-operate/release-audit-8e9c065.json` remains historical.
`release-rejudgment-audit-55a197a.json` verifies its original raw-file hashes
remain unchanged, adds run 6 and checks complete response/log correlation on
each retained direct path. Product build inputs remain equivalent to 8e9c065;
release tooling fixes are identified separately. Run 6's rebuilt image has a
different id; no byte-for-byte equality with the earlier image is claimed.

## Scope and self-check

Decision actor/text, amended SPEC and unchanged clauses read; GAPS and RISKS
disclosures checked; raw control bodies, terminal chunks, completion ids and
shutdown timing independently inspected; corrected run-6 loop and output read;
original raw-file hashes and product-input equivalence verified. The first
namespace run remains unretained. The host still loses R0 in all six captures;
native Linux and the precise forwarding mechanism remain untested. No app,
build, bench, product or test change was made for this packet. QA accepts the
amended contract on the recorded evidence, with the human-authorized host gap
visible; the former rejection is preserved rather than rewritten.
