# QA guidance

QA here means proving, with evidence a stranger can check, that the promised
outcome works — not running a test suite and reporting green. The QA Agent
owns this; the Development Agent writes tests first under the same rules; the
Code Review Agent audits the QA evidence.

## 1. Strategy

| Layer | Owner | Proves | Where |
|---|---|---|---|
| Unit tests | builder | a rule or component in isolation (codec, validation, expiry, filter) | `src/test/java`, no Spring context |
| Functional tests | builder writes, QA verifies | each acceptance criterion through the public HTTP surface against real migrations | `src/functionalTest/java`, `@SpringBootTest` + `MockMvc`, `functional` profile |
| By-effect checks | QA | the running artifact behaves as the ACs say: status, headers, body, log line, stored row | `scripts/http` against `bootRun`/jar/container; captures under `proof/` |
| Installed smoke | release | the shipped artifact (jar, image) answers the journey | `scripts/smoke.sh` |
| Dogfood | QA on request | real use finds real defects; feeds the brownfield mission | `docs/qa/dogfood/<mission>.md` |

The gate is `scripts/gw check`: both suites green and JaCoCo at 100 % line and branch over the merged execution data.

## 2. Writing tests that prove something

- **One AC, one test, named after it**: `AC05_postPingReturns405ProblemDetailWithRequestId`. The traceability table is derivable from the names.
- **Arrange / Act / Assert** with nothing clever in between; the assertion names the observable (`status`, header, JSON path, log field, row).
- **Fail for the right reason first**: the builder watches each new test fail before making it pass; a test that never failed proves nothing. The reviewer asks "what input would break this code, and which test would notice?".
- **Failure paths are first-class**: invalid input, duplicates, not found, expired, rate-limited, wrong method, spoofed headers.
- **Privacy by canary**: send a unique canary in the User-Agent / inbound header / URL and assert it appears nowhere in logs or storage.
- **Deterministic**: no `Thread.sleep`, no wall-clock races (compare instants against a recorded interval or inject a `Clock`), random ports, isolated data per test, no order dependence.
- **Honest assertions**: assert the exact body shape when the SPEC says "exactly"; assert absence as well as presence (a canary must be absent; a stack trace must be absent).
- **Readable over DRY**: a little duplication in tests beats a helper that hides the behaviour under test.
- **Server metadata needs a real server**: MockMvc never starts Tomcat, so anything the container adds (thread names, connector-bound addresses, real header casing) is invisible to it. An AC about such metadata gets one real-server journey (`webEnvironment = RANDOM_PORT` + a loopback client) or a by-effect capture at `qa_check` — the QA-01 lesson, wave w1 follow-up W1-02.

## 3. Coverage policy

- 100 % line and branch, merged across `test` and `functionalTest`, enforced by `jacocoTestCoverageVerification`.
- Per-suite reports are informational: the unit suite is not expected to cover controllers, nor the functional suite every domain branch. The merged number is the gate.
- An uncovered line is either a missing test or dead code. Decide which; write the test or delete the code. Exclusions are not configured by builders; a genuinely untestable line is a `docs/qa/GAPS.md` entry with the compensating manual check, approved in review.
- **Functional coverage** is a separate view: `docs/qa/TRACEABILITY.md` maps every AC and business rule to the tests that prove it, and every test back; each row also carries the `docs/REQUIREMENTS.md` id (`FR-n`/`NFR-n`) the AC serves, so product requirements trace to tests in one hop. Both directions complete, or there is a gap entry.

## 4. Verifying by effect (the `qa_check` step)

1. Confirm the candidate: the worktree HEAD equals the SHA in the packet.
2. `scripts/gw --log docs/qa/<slice>/check.txt --offline check` in the worktree; read the summary, not just the exit code.
3. Start the app on a free port and exercise **every AC** with `scripts/http`, including failure cases; capture status line, headers and body under `missions/<m>/slices/<s>/proof/`; capture the corresponding log line(s); inspect stored rows through the API or a functional test.
4. Copy the three JaCoCo reports to `docs/qa/coverage/<slice>/{unit,functional,all}/`; write `SUMMARY.md` with the percentages from the CSVs.
5. Append the slice's traceability table; append the gap entry (even "none").
6. Record what you did **not** verify.
7. `rig proof add … --evidences …` for the contract items you covered; exit `handoff` on PASS, `failed` with `docs/qa/<slice>/findings.md` otherwise.

Stop the app, leave the worktree at the candidate SHA, never edit product code or tests (a fix would make you an author).

## 5. Findings and verdicts

- A finding names: the AC or rule, the exact command to reproduce, expected vs observed, severity (MUST-FIX / HIGH / MEDIUM / LOW / INFO).
- Verdict rules: any failing AC, red build, or coverage below the gate without an accepted gap → `failed`. Observations that do not break an AC are MEDIUM or lower and do not block.
- On re-check after a fix: re-run the full gate and the affected ACs by effect; append `## Re-check <sha>` with each finding's status.

## 6. Evidence conventions

- `proof/<what>-<sha7>.txt` for captured exchanges and log lines; `docs/qa/coverage/<slice>/` for reports; `docs/qa/<slice>/findings.md` for failures; `PROOF.md` §QA for the narrative (what verified, how, what not).
- Evidence is files, not chat. Queue notes carry paths and one-paragraph summaries.
- Never copy the builder's claims into QA evidence; verify independently and cite your own captures.

## 7. Dogfood protocol

When the release agent asks: use the installed artifact as a user would for 20–30 minutes; follow curiosity, not the test plan; record each defect with steps, expected/observed, severity and a capture; file them in `docs/qa/dogfood/<mission>.md`. Real defects feed the brownfield mission; they are not blockers for the current release unless severe.

## 8. Flaky tests, retries and other things we do not do

- A flaky test is a defect: fix the determinism or delete the test; never add retries to hide it.
- No `@Disabled` to go green; no lowering thresholds; no excluding files from coverage without a gap entry.
- No testing framework internals (that Spring parses JSON) — test our contract.
- No sleeping for asynchronous effects: poll a condition with a bounded timeout, or make the effect synchronous in tests.
