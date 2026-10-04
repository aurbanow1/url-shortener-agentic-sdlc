# 06-client-identity — code review

Candidate: `fb63a88a9b92c1fec97ba74686af1a2f30304160`.
Base: `50ad9c3ab9e65baa4100ede1772b514322957fa5`.
Packet: `qitem-20261004024533-3e9ae475`; reviewer: `review2-agent@urlshort-factory` (Codex), independent of the Claude builder.
Date: 2026-10-04. **Verdict: FAIL — one HIGH, CR-01.** The same candidate's security review passes; this combined packet returns to implement once.

## Context proof

D21 asks for one stateless authority for existing client-identity rules, preserving two different questions: the resolved visitor charged by the limiter and hashed by click recording, and the original direct peer permitted to read the audit trail. Trust for the first must never grant the second. The SPEC has fifteen preservation ACs; no new policy, schema, settings, dependency or public HTTP contract is intended. Confidence: high in intent, boundaries and preservation after reading the SPEC, design, impact analysis, ADR amendments, AGENTS, DESIGN, guidance and QA record.

The explicit plan-lock grant in `slice.yaml:64` permits the single `application.properties` comment explaining unique-visitor identity and audit exclusion. It changes no setting. Impact analysis and ADR amendments precede dependent code. Characterization `1b4e0a7` has exactly the baseline production tree; the production-only move `7e23259` changes no tests. All 43 original functional source files are byte-identical across the candidate range. Final unit-test changes preserve existing expected answers while moving references and adding the planned cases. No settled design finding is reopened.

## Complete file ledger

`git diff --stat main...slice/06-client-identity` and the full diff were read, followed by every changed file in full: 10/10 files, 894 insertions and 71 deletions. Paths below are relative to the candidate worktree.

| File | Verdict and evidence |
|---|---|
| `src/functionalTest/java/dev/urlshort/web/ClientIdentityCharacterizationJourneyTest.java` | **HIGH CR-01** at line 130. The characterization covers the intended matrices and full fixed-day grouping, but its statistics wait consumes a frozen finite budget. |
| `src/main/java/dev/urlshort/audit/AuditController.java` | PASS. Same constructor-time container-safety expression and request guard, now delegated to ClientIdentity; refusal still precedes paging, negotiation and store access. |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS. Reads the same attributed client/peer fallback through `ClientIdentity.of`; reduction, hashing, queue, logging and shutdown unchanged. |
| `src/main/java/dev/urlshort/web/ClientIdentity.java` | PASS. Existing parser and loopback predicate moved without policy changes; single attribute key for both consumers; explicit separate audit predicates; no state, bean or I/O added. Public API has meaningful Javadoc. |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java` | PASS. Identity resolved at the same point before charging, after exemptions. One authority; no leftover parser/delegate. Package-private visibility restored. |
| `src/main/resources/application.properties` | PASS. One explicitly granted explanatory comment; all values unchanged. |
| `src/test/java/dev/urlshort/audit/AuditControllerTest.java` | PASS. Direct calls replace moved predicate references; whitespace and combined remote-IP controls retain existing policy. |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS. New shared attribute reference, unchanged old assertions; absent/non-string attribute fallback controls. |
| `src/test/java/dev/urlshort/web/ClientIdentityTest.java` | PASS. Tests resolve/store/read and direct-peer separation, including exact-text trust, opaque tokens, blanks and bad address forms. |
| `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | PASS. Reference adaptation only for existing matrix assertions. |

## Verification and QA audit

In `.worktrees/06-client-identity`, verified clean HEAD equals the QA candidate and ran:

```text
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/06-client-identity/code-check-fb63a88.txt --offline check --rerun-tasks
```

Exit 0, 14 tasks executed, including Javadoc. Fresh XML: **268 unit + 322 functional**, zero failures/errors/skips. Fresh merged CSV: **584/584 lines, 206/206 branches**. Only `test.exec` and `functionalTest.exec` feed this measurement; the external reproduction is not coverage data. Per-suite totals and fresh CSV copies are in this directory. A passing run establishes that run; it does not refute the reproduced schedule below.

Independent saved-evidence reconciliation is executable with `python3 docs/review/06-client-identity/audit-code-evidence.py`; its result is [code-verification-fb63a88.json](code-verification-fb63a88.json). It verified:

- All **3,490 QA artifact hashes** and **378 coverage report hashes**, source custody, unchanged original functional files and pre-move test chronology.
- **326 method mappings / 321 distinct source methods / 74 XML reports**, including five inherited mappings. The 56 class-only parameterized attributions are explicitly limited to green class reports, not invented per-method XML proof. All fifteen ACs occur in committed traceability; the QA gap append is present.
- **2,041 controlled raw HTTP records** against their status, headers and bodies, and each request's complete correlated events against the original app log. Recomputed substitutions and normalization reproduce **1,017 exact non-metric pairs**. Each of the two differing scrapes retains its original body/length; eight business samples match. The recorded readiness-503 and JVM-GC series differences are outside the metric privacy promise, and their startup explanation remains an inference.
- The **59 original-jar + 59 candidate-jar + 72 blank-setting responses** against raw HTTP and complete request/log joins. The real jars provide container-override evidence separately from simulated servlet peers. The retained springdoc initialization-duration difference is disclosed outside AC-14's named audit-200 comparison; live OpenAPI content remains unchanged.

QA's external clock, peer/header wrapper and held writer are instruments, not product changes. Their controlled matrix is not a physical nonlocal-client test. The earlier two 401s from the pre-existing localhost listener are retained with the same-candidate isolated/full replay and are not this review's defect. No new network, Docker, broad latency or shutdown experiment is claimed.

| AC / obligation | Source and effect assessment |
|---|---|
| AC-1, AC-2 | Unchanged resolve/retire and error paths; baseline-origin stored links retain target/cache/error outcomes in paired captures. |
| AC-3, AC-4 | Frozen 60/600 default budgets and both two-token matrix budgets; identical right-most-untrusted parsing and unrelated-peer controls. |
| AC-5 | Product grouping preserves exactly three clicks, two visitors, no bots on the fixed day; raw values absent from returned/private rows. **New suite wait is unreliable: CR-01.** |
| AC-6–AC-9 | Direct/trusted loopback and nonlocal matrices; forwarding-header presence denies; original/candidate real Tomcat override and individual/combined blank settings retain results. |
| AC-10, AC-11 | Budget rejection precedes audit guard; audit denial precedes validation/negotiation. Paging, wrong method and failed-store ProblemDetails preserved. |
| AC-12 | Source data flow and canaries preserve private identity, referrer reduction and no raw client values in logs/metrics. Detailed salt/hash assessment in security review. |
| AC-13 | Existing success/failure/concurrency assertions unchanged and green; paired slow/failed-store, HEAD and concurrent effects retain behavior. These did not exercise the new helper's polling-budget interaction. |
| AC-14, AC-15 | Server-generated correlation and complete named event comparisons preserved; API source blob and live/committed document equality retained. |
| Brownfield proof / D21 | Chronology and single-authority extraction hold. Proof 16's post-merge register/current-system update remains assigned downstream under lead transition 1882. |

## Findings

| ID | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| CR-01 | HIGH | `src/functionalTest/java/dev/urlshort/web/ClientIdentityCharacterizationJourneyTest.java:130` (`settledStats`, request at 127; shipped-budget caller at 295) | With the real writer temporarily held, actual candidate helper performs HTTP statistics polls under the frozen 60/minute create budget. Setup creation spends one token; 59 successful polls exhaust the remainder. The next poll returns 429 and `stats.get("totalClicks").asLong()` throws NPE after **170 ms**, before the ten-second wait. Releasing the writer yields the correct 200 / three-click / two-visitor result from a fresh peer. [Probe source](StatsPollingProbe.java), [init script](polling-probe.gradle), [full output](polling-probe-fb63a88.txt). | Make waiting for asynchronous storage independent of the HTTP rate budget, then read statistics with an explicit 200 assertion before parsing. Keep the shipped 60/600 limits, frozen fixed-day clock, all matrix cases and exact day/count/privacy assertions. Verify with a deliberately delayed writer plus the full gate; no retries, raised budget or weakened oracle to conceal the race. |

This is a **test-only deterministic-gate defect**, not a discovered product grouping or security regression. The guide explicitly requires deterministic tests (`qa.md:27,69–72`, `java-spring.md:60`). A valid short writer delay can fail the mandatory gate independently of the feature, so HIGH is warranted despite the fresh passing suite.

Reproduction command, from the candidate worktree:

```text
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/06-client-identity/polling-probe-fb63a88.txt --offline -I ../../docs/review/06-client-identity/polling-probe.gradle identityPollingProbe
```

The probe calls the actual candidate `assertGroupsAsTheReference` and `settledStats`, uses real filters/limiter/JDBC, and places a latch ahead of the real recorder's queued writes. Release occurs on the first 429, with a three-second fallback. It does not edit any test. The printed 61 statistics requests include the subsequent successful fresh-peer control; the failing helper performs 60. Probe exit 0 means the expected fault and positive control were observed, **not** that the helper passed.

Builder response at 02:48Z: acknowledged the defect; proposes bounded JDBC click-row waiting, followed by one HTTP statistics read with an explicit 200 assertion, retaining all budgets and oracles. **Resolution pending:** no fixed candidate has been handed over. QA read the reproduction, documented its omitted helper control and withdrew proof items 1 and 11 in receipts 17/18, reaffirming the qualified gap record in receipt 19 (`4f8b2d74`). [Live proof snapshot](proof-at-review-fb63a88.json): 14 accepted, 1/11 withdrawn, 16 pending; no proof issues. Historical green runs remain evidence of their observations, not permission to integrate this candidate.

## Ponytail review

No complexity finding. The single static utility is D21's explicit structural request; it replaces duplicate ownership and removes obsolete public filter exposure without a bean, interface, dependency or speculative extension. The Boot-trigger `ponytail:` comment names a real upgrade ceiling. The characterization matrix serves the preservation contract. CR-01 is recorded once as a correctness finding.

## Merge readiness and self-check

**Do not integrate fb63a88.** Return once to implement for CR-01, then QA and focused re-review. Security verdict: PASS, with existing Boot-upgrade, fallback-path and offline-advisory limits retained. No new non-blocking backlog item. The producer's proposed repair is not yet accepted.

Candidate and clean worktree checked; all ten files read; fresh gate and independent reproducer executed; every finding has location, consequence, evidence and required change. This report, security report, independent evidence and two ledger rows are committed with explicit review-only paths. Product/tests/spec/design remain untouched by the reviewer.

## Re-review e40b09541feb0b7555c475baa82587fdd09e4890

2026-10-04, packet `qitem-20261004032752-d162e1b3`. **PASS — CR-01 fixed; no new finding.** Independent Codex re-review of the Claude builder's response in `PROOF.md`, scoped to the finding and its one-file repair. Exact clean worktree HEAD matches QA. Earlier findings and evidence above remain historical.

| File changed since fb63a88 | Re-review verdict |
|---|---|
| `src/functionalTest/java/dev/urlshort/web/ClientIdentityCharacterizationJourneyTest.java` | PASS; full file accounted for against the earlier complete read and the complete one-file diff (16 additions/9 removals). `settledStats` now polls a parameterized count of this link's stored click rows, then performs one statistics request and asserts 200 before parsing. Its caller passes the existing JdbcClient. All other contents are unchanged. |

| Finding | Resolution | Independent evidence |
|---|---|---|
| CR-01 HIGH | **Fixed.** Waiting no longer spends the rate budget. The ten-second bound, explicit shipped 60/600 settings, frozen fixed day and complete grouping/privacy assertions remain. The 20 ms interval spaces condition checks; elapsed time is not the success condition. | [StatsPollingResolutionProbe.java](StatsPollingResolutionProbe.java) derives from the original reviewer reproducer, calls the actual candidate helper and holds the real writer for three seconds. Without trust: **3031 ms**; with P trusted: **3056 ms**. Each helper makes **one** HTTP stats read, no 429/NPE, passes the entire original day/count/privacy oracle, then an independent fresh-peer read confirms 200 / three clicks / two visitors / zero bots on 2026-10-01. Both app contexts close. [Full output](polling-resolution-e40b095.txt). |

The resolution probe deliberately expects success after the writer's timed release. It does not mistake the old reproduction's failure-expecting exit for a successful regression test. Commands run from the exact worktree:

```text
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/06-client-identity/code-check-e40b095.txt --offline check --rerun-tasks
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/06-client-identity/polling-resolution-e40b095.txt --offline -I ../../docs/review/06-client-identity/polling-resolution-probe.gradle identityPollingResolutionProbe identityPollingTrustedResolutionProbe
```

Both exit 0. Fresh full gate: **268 unit + 322 functional**, no failures/errors/skips, Javadoc green; **584/584 lines and 206/206 branches**, only canonical unit/functional execution data. The external resolution probe contributes no JaCoCo data. Fresh CSV copies are named `code-coverage-e40b095-*.csv`.

[audit-recheck-e40b095.py](audit-recheck-e40b095.py) independently reconciles the return; [results](code-verification-e40b095.json): **672 current artifact hashes**, **378 coverage hashes**, all **3,490 historical hashes** through **379 explicit archived-coverage aliases**, **326 source/report mappings**, and all **159 fresh affected raw HTTP/complete log pairs** against the earlier candidate with the declared substitutions. All 818 recorded affected-effect assertions pass. QA's external current-characterization replay reports **72/72** green invocations on original production; inspected the shadow-compilation init, source provenance and XML rather than claiming to have rerun that replay. The original 43 functional source files remain unchanged.

Product, unit-test, build and API trees/blobs equal fb63a88; the actual retained bootJar hash is again `92e1b7aef3b91749facdc39bfbc35cc8df8367119294d801436a7db34b38e58f`. Consequently unaffected AC effects and the security checklist carry from the earlier review under QA guidance §5; they were not all re-executed. Runtime dependency graph was freshly queried offline and is identical. No complexity finding from the focused Ponytail pass; no new layer/dependency or policy change.

[Live proof snapshot](proof-at-review-e40b095.json): **1–15 and 17 accepted on exact e40b095**, item **16 pending**, no issues. That downstream register/current-system update and post-merge QA return remain authorized by lead transition 1882; this review supplies only the independent-review part. No premature final acceptance.

**Merge readiness: PASS for integration.** Security re-review also PASS. No remaining CR-01 action and no new non-blocking item. Existing deployment/framework/advisory limitations remain named in the security report. Self-check: one changed file of one reviewed, exact candidate and unchanged production verified, independent delayed-writer controls and full gate executed, QA archive qualifications preserved, both resolution ledger rows recorded; only review artifacts changed.
