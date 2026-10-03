# QA findings — 01-ping

Current disposition: **QA-01 resolved; QA PASS on f286a10**. The original
finding below is retained as history; see the re-check section at the end.

Candidate `3886a04a4afac6117038b2884cf72749f57d28aa`; QA Agent (Codex),
2026-10-03 UTC. Verdict: **NOT-CLEAR / failed**. The fresh check passes
(6 unit, 9 functional, merged 15/15 lines; no branches), but the independent
live capture does not meet one literal acceptance assertion.

## QA-01 — AC-7 address absence fails with the required loopback bind

**Severity: MUST-FIX for acceptance** (contract/evidence mismatch; no
demonstrated client-input disclosure). Affected: AC-7, proof-contract item 7,
the repository's no-raw-IP logging rule.

Reproduce from the main checkout with `.worktrees/01-ping` at the candidate:

```sh
scripts/gw --log /tmp/01-ping-qa-repro.log --offline -p .worktrees/01-ping bootRun --args='--server.address=127.0.0.1 --server.port=18081 --spring.datasource.url=jdbc:h2:mem:qa-ping'
scripts/http --silent --show-error --max-time 10 --include --header 'User-Agent: qa-canary-user-agent-3886a04' --header 'X-Request-Id: qa-canary-inbound-id-3886a04' http://localhost:18081/api/ping
```

After startup, send the HTTP command from a second terminal, then inspect the
JSON log event whose requestId equals the response header. Stop the app after
the reproduction.

**Expected:** AC-7 says no log output while handling the request contains the
User-Agent canary or the client's remote address. On this IPv4-only loopback
listener the remote address is 127.0.0.1.

**Observed:** GET is 200; both canaries are absent; the ping log contains
`process.thread.name = http-nio-127.0.0.1-18081-exec-5`. The complete event is
in `missions/00-hello/slices/01-ping/proof/qa-bootrun.txt`, associated with
`qa-ping-canaries.txt` by request id
`e7e28278-4754-40fb-b6d1-beaab47f97b1`. The first two GETs show the same
address in their thread names. `qa-ping-log-line.json` preserves the first
event verbatim.

**Cause and scope:** the installed Tomcat `AbstractProtocol.getNameInternal`
appends `getAddress().getHostAddress()` to its name; its `getAddress()` reads
the endpoint's configured address. See the bytecode capture
`proof/qa-tomcat-bind-name.txt`. QA supplied that bind to obey the localhost-only
execution policy. The value is server metadata, not derived from a request;
the functional AC-7 test uses remote address 203.0.113.77 and passes because
MockMvc has no Tomcat worker thread. No claim is made that a distinct remote
client's address is logged, or that this mismatch occurs with the default
unbound connector.

**Required disposition:** make the captured log meet the locked literal
criterion, or route an explicit clarification of server bind metadata versus
client-derived addresses through the lead. QA cannot silently narrow AC-7 or
accept proof item 7. Product source, tests and locked documents were not edited.

## Self-check

Checked the real response and matching JSON event, reproduced the substring
in all three GET events, distinguished the server bind address from client
input using the installed dependency, and kept severity tied to acceptance
rather than asserting a privacy exploit. App stopped; candidate worktree clean.

## Re-check f286a10863e4a8081235226f2d56e51ac121b319

QA Agent (Codex), 2026-10-03 UTC. **QA-01 RESOLVED.** Independently reran the
full check with all tasks forced: 6 unit + 9 functional tests, zero failures,
errors or skips; merged 15/15 lines, 0/0 branches, coverage gate passes.

Repeated the original reproduction with the same `127.0.0.1:18081` bind and
unchanged shipped logging settings. All four HTTP checks pass, including
POST 405. Both ordinary GETs and the canary GET have matching JSON log events
with no `process.thread.name`; the entire startup-through-shutdown output
contains neither canary nor any checked IPv4/IPv6 loopback address form.
This satisfies literal AC-7 and proof item 7 without a contract waiver.

Evidence: `missions/00-hello/slices/01-ping/proof/qa-bootrun-f286a10.txt`,
`qa-ping-canaries-f286a10.txt`, `qa-ping-log-line-f286a10.json`,
`qa-check-f286a10.txt`; current totals in `docs/qa/coverage/01-ping/SUMMARY.md`.
App stopped gracefully. Worktree remains at the exact candidate; QA made no
product edits. No open QA findings.
