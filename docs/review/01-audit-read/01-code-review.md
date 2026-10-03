# Code review — 01-audit-read

Candidate: **35590f06c852543c29097a42c43b7802be90ba40**. Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03. Packet `qitem-20261003195938-d8b8a9c3` combines code and security, with one exit. **FAIL: one HIGH, CR-01.**

## Context proof

The Operator needs a read-only, loopback-only, paginated view of the existing mutation trail. Rows retain their eight stored values and descend by write sequence, not commit time. Keyset traversal preserves the SPEC's bounded guarantee for concurrent commits. Existing links, audit writes, statistics and error/log contracts must remain unchanged. Confidence: purpose/invariants 99%; implementation and recorded evidence 98%. No remote TCP or real IPv6 claim is made.

Read the SPEC, design and impact analysis, ADR-0019, AGENTS, system design, Java/Spring and QA guidance, review/security and brownfield checklists, builder/QA PROOF, coverage summary, traceability and GAPS. The worktree `.worktrees/01-audit-read` was clean at the exact QA candidate before and after review. Range: `main...slice/01-audit-read`, merge base `0df4841`; 15 files, +1463/-7. The full change and every changed file were read. No product, test, SPEC or design was edited by this reviewer.

## Verification and audit of QA evidence

- Independently ran `scripts/gw --offline check --rerun-tasks`, with the wrapper logging to [code-check-35590f0.txt](proof/code-check-35590f0.txt): **200 unit + 200 functional, zero failures/errors/skips, 14 tasks executed, Javadoc green**. Merged coverage **492/492 lines, 190/190 branches**. This green gate does not cover CR-01's configuration dimension.
- Read per-suite CSVs and reconciled them against the fresh run: unit 436/492 lines, 184/190 branches; functional 455/492 and 158/190. Verified all **348** committed report hashes against QA's manifest. [Reconciliation](proof/evidence-reconciliation-35590f0.json).
- QA's AC mapping names all 21 ACs and rules 1–9, with real JDBC held-transaction induction for AC-20 and controlled store failure for AC-21. Its functional spy is supplemented by a separate JDBC failure capture. The captured original/after read-only snapshots are exactly equal: **45 audit rows, eight links, zero clicks**. The installed two-page exchange contains 2+1 rows, and both forwarded repetitions return 403.
- Original-suite qualification is honest: 155 invocations, 153 pass and only the two API enumeration assertions fail. Read lead grant `428e9e1` (`qitem-20261003182833-40a842ff`, transition 1156); candidate changes exactly those assertions. Independently compared all five old OpenAPI path objects to `f6dd29e`: equal. Entire candidate document equals the fresh live export.
- QA preserves instrument corrections and limits in GAPS/PROOF. Its 271 captures and 276 events support the tested inputs; they do not prove every Tomcat setting. Contract item 12 remains pending. No new migration, dependency, lowered threshold or test deletion. Existing audit-column debt has named V3/V4 migrations in GAPS.
- New independent [control source](proof/AuditCodeControls.java), [Gradle task](proof/audit-code.gradle) and [output](proof/code-controls-35590f0.txt) run the **actual candidate controller**, not a modeled replacement, on real Tomcat bound only to `127.0.0.1`. Five configurations × GET/HEAD × absent/loopback/remote XFF = 30 observations. Two settings reproduce CR-01; default, native and framework controls distinguish it. The control's successful process exit means observations were collected, not that all security predicates passed. All five contexts closed.
- Reviewer reconciliation initially assumed Gradle's default CSV directories; corrected to this repository's configured `test`, `functionalTest`, `all` paths. No build or product correction was needed.

## Complete file ledger

Paths are relative to the candidate worktree. Each row includes the complete file, not just its hunk.

| File | Verdict / evidence |
|---|---|
| `README.md` | Reviewed; one granted endpoint line. Its no-opening-setting claim depends on CR-01 being fixed. |
| `docs/api/openapi.json` | PASS: only audit operation and two schemas added; nullable before/next, object payloads, example, 400/403/429/500; existing operations equal baseline and whole document equals live. |
| `src/functionalTest/java/dev/urlshort/audit/AuditAccessSettingsJourneyTest.java` | Reviewed; behavior checks for trusted-proxy/base URL/budgets pass. It does not cover the Tomcat settings in CR-01. |
| `src/functionalTest/java/dev/urlshort/audit/AuditForwardedHeadersJourneyTest.java` | Reviewed; real native/framework/cloud and HEAD checks are useful. Add the two independently activating remote-IP settings for CR-01. |
| `src/functionalTest/java/dev/urlshort/audit/AuditReadFailureJourneyTest.java` | PASS: named spy induction, safe 500 and sanitized correlated logs, recovery; independently supplemented by QA JDBC fault. |
| `src/functionalTest/java/dev/urlshort/audit/AuditReadJourneyTest.java` | PASS for exercised inputs: all fields, page boundaries, inserted/held writes, invalid input, immutable rows, access and logs. Separate in-memory fixture; no product mutation hook. |
| `src/functionalTest/java/dev/urlshort/audit/AuditUpgradeJourneyTest.java` | PASS: explicit V2 fixture, reads statistics before verification click; all stored fields retained. QA adds the actual shipped-jar upgrade. |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | PASS: exactly the two granted enumeration edits, no weakened response or equality assertion. |
| `src/main/java/dev/urlshort/audit/AuditController.java` | **CR-01 HIGH** at line 61: NONE does not prove the peer is the connection. Validation/error/negotiation order otherwise matches design. |
| `src/main/java/dev/urlshort/audit/AuditEntry.java` | PASS: eight fields, UTC instant, parsed objects and null before; package-private record with useful field documentation. |
| `src/main/java/dev/urlshort/audit/AuditPage.java` | PASS: bounded rows and nullable cursor, no total or speculative fields. |
| `src/main/java/dev/urlshort/audit/AuditTrail.java` | PASS: one parameterized SELECT, descending primary-key keyset, limit+1 lookahead, cursor uses last returned row; no write or separate count race. |
| `src/main/java/dev/urlshort/audit/package-info.java` | PASS: correct read/write responsibility boundary. |
| `src/main/resources/application.properties` | Reviewed: NONE pin preserves default behavior but does not prevent remoteip overrides (CR-01); no unrelated edit. |
| `src/test/java/dev/urlshort/audit/AuditControllerTest.java` | Reviewed: parsing, address classes and strategy branches pass; source-level strategy coverage cannot detect the container bypass. |

New types are package-private and document their contracts; the existing public Javadoc gate passes. Servlet-only conditional activation is justified by the pre-existing non-web boot test. OpenAPI Object/types annotations match measured 3.1 output. No other design deviation requires a finding.

## Acceptance assessment

| ACs | Assessment |
|---|---|
| 1–5 | PASS: empty page, create/retire values and UTC time, sequential order, exact stored-row comparison. |
| 6–9, 20 | PASS: page sizes/terminal cursor, new rows excluded from continuation, held-write boundary, static field errors. |
| 10 / NFR-A2 | PASS: no application audit UPDATE/DELETE path; one INSERT writer and one SELECT reader; fresh AC-10 plus identical QA row snapshots. |
| 11–14 / NFR-S6, rule 2 | Default and named strategy/peer cases pass; **overall no-opening-setting obligation fails CR-01**. This finding concerns header rewriting before the handler, not the documented headerless-local-relay limitation. |
| 15–16 | PASS for request/log hygiene under shipped logging: fresh captured-output tests plus QA whole-run canaries and request correlation. |
| 17–18 | PASS under explicit enumeration grant: existing behaviors and upgrade preserved; no DDL. |
| 19 | PASS: audit operation documented; older operations unchanged. Existing W2-01 schema defect remains assigned to dogfood-fix. |
| 21 | PASS: read failure is a safe 500, no empty success, recovers. |

## Findings

| id | severity | file:line | evidence | required change |
|---|---|---|---|---|
| CR-01 | HIGH | `src/main/java/dev/urlshort/audit/AuditController.java:61` (also 81–98); regression gap `src/functionalTest/java/dev/urlshort/audit/AuditForwardedHeadersJourneyTest.java:55` | Run the recorded Gradle control on 35590f0. With either `server.tomcat.remoteip.remote-ip-header=X-Forwarded-For` **or** `server.tomcat.remoteip.protocol-header=X-Forwarded-Proto`, effective strategy remains NONE, but `GET /api/audit` with `X-Forwarded-For: 127.0.0.2` returns **200 and the stored AUDIT-CODE-CANARY URL**. Baseline returns 403; HEAD similarly changes 403→200. The container rewrites/removes the input before `fromLoopback`, defeating rule 2 and AC-14's configuration invariant. | Make the guard refuse whenever configured container handling can rewrite the connection peer, including each independent remoteip setting; retain default local access. Add real-Tomcat regression cases for both settings (GET and HEAD, forged XFF), plus existing NONE/native/framework controls. Sync the design/ADR claim through its owner and rerun QA on the exact corrected candidate. |

This is one defect shared with security review, not two independent findings. It is new evidence about a different activation path from the earlier design DR-01. No settled finding is reopened merely on preference. Observed traffic is loopback with an explicit forwarding header; no actual remote TCP penetration is claimed.

## Ponytail review

Lean already. Ship.

This sentence is the complexity-only result. The correctness/security verdict remains FAIL on CR-01. Four small package-private types, one SQL statement and platform facilities are sufficient; no speculative layer or dependency to remove.

## Verdict and self-check

**Not merge-ready.** Return the combined packet to implement for CR-01. No MEDIUM/LOW/INFO backlog findings added. After correction: exact candidate through QA, then focused re-review of the guard and regression evidence; item 12 cannot be accepted from this report.

Producer response received before exit: the design owner agreed with CR-01 and committed a proposed design correction at `0052efb` (also inspect `TomcatServerProperties` remote-IP and protocol-header fields). The lead re-locked that plan (`01M41PCN4N25BR55E2ST2KKSRZ`) and explicitly requested an independent look at this design delta during the fixed-candidate re-review. That response is **acknowledged, not yet independently accepted**; the product candidate reviewed here remains 35590f0 and CR-01 remains open. The next review must inspect the design delta as well as its implementation.

Verified exact SHA, clean candidate, 15/15 files, fresh full gate, real reproduction and reconciled QA artifacts. Findings have consequence, location and executable evidence. Ledger has both review rows. This is an independent review; producer repairs remain with the producer.

## Re-review 7ac8af56ed04c27bbefbd416b3976c544d2f274a

2026-10-03, packet `qitem-20261003213051-77ffaad3`, combined code/security review. **PASS; CR-01 fixed, no open findings.** This verdict supersedes the initial FAIL for the corrected candidate only.

The clean worktree matches QA's exact SHA. Read the full correction and all three changed files, +72/-6 from 35590f0; the other 12 files in the original ledger are unchanged. **3/3 delta files reviewed, 15/15 unique candidate files reviewed** (full range from `0df4841`: +1529/-7). The accepted SPEC and public API are unchanged. Confidence in the correction and scoped evidence: 99%.

| Changed file | Verdict |
|---|---|
| `src/main/java/dev/urlshort/audit/AuditController.java` | PASS; lines 65–67 require NONE and both bound Tomcat remoteip header settings to lack text. The guard still precedes validation and query execution. |
| `src/test/java/dev/urlshort/audit/AuditControllerTest.java` | PASS; each independent trigger refuses; empty settings preserve local access; earlier strategy/null/address/parsing cases retained. |
| `src/functionalTest/java/dev/urlshort/audit/AuditForwardedHeadersJourneyTest.java` | PASS; real Tomcat under each trigger refuses plain/forged GET and HEAD, with a stored canary proving denial does not return audit content. Earlier native/framework/platform controls retained. |

| Finding | Resolution | Evidence |
|---|---|---|
| CR-01 HIGH | Fixed | The unchanged original reviewer reproduction now yields default plain GET/HEAD 200 and forwarded 403; either remoteip trigger, native and framework yield 403 for every plain/forged request. All 30 observations independently asserted in the reconciliation. A further 32 asserted real-container controls cover custom `X-Real-IP`, both settings, empty/whitespace settings, XFF/Forwarded, GET/HEAD and strict HTML Accept. Denials contain no audit canary/items; HEAD bodies are empty. |

### Fresh verification and QA evidence audit

Commands ran in the exact candidate worktree, with `--log` paths under this report's `proof/` directory:

```sh
scripts/gw --offline check --rerun-tasks
scripts/gw --offline -I /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/01-audit-read/proof/audit-code.gradle reviewAuditCode
scripts/gw --offline -I /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/01-audit-read/proof/audit-revision.gradle reviewAuditRevision
```

- [Full gate](proof/code-check-7ac8af5.txt): exit 0, 14 tasks executed, **203 unit + 202 functional**, zero failures/errors/skips; merged **494/494 lines and 194/194 branches**. Per-suite totals match committed QA reports: unit 438/494 lines, 188/194 branches; functional 457/494 and 162/194.
- [Original control rerun](proof/code-controls-7ac8af5.txt) and [additional asserted controls](proof/revision-controls-7ac8af5.txt) pass their expected effects. The original launcher only collects observations; [reconciliation](proof/evidence-reconciliation-7ac8af5.json) explicitly checks its 30 outcomes. The additional [source](proof/AuditRevisionControls.java) checks all 32 directly. Both use actual candidate classes and close their loopback-only contexts.
- Independently accepted the requested design correction `0052efb`: all six files read, installed Boot 4.1.1 customizer bytecode inspected, and implementation effects verified. See the focused entry in [design review](design-review.md). No new library, layer, schema, storage or public response was introduced.
- QA's corrected-candidate record (`a08650c`, receipt commit `0d92000`) names all 21 ACs, **258 HTTP captures and 1482 reconciliation checks**. Independently verified all **348** coverage report hashes, 36 installed-configuration observations, exact read-only snapshots for three intervals (4/45/45 audit rows, each with three links and zero clicks), all five original API path objects and equality of the complete live/committed OpenAPI document. QA's same-directory upgrade retains shipped rows and statistics; original-suite qualification remains 153/155 with only the two explicitly granted enumeration failures.
- Logging evidence is scoped honestly: the initial added file sink was plain text, so its 235 exchanges are not claimed as retained JSON correlation. QA repeated **23 requests**, retaining **24 default-console JSON request events**, including the two actual pages and a 403 for the identical second-page URI with a forwarding header. Independently matched the raw events to response IDs/statuses; GAPS and PROOF disclose the correction and whole-run canary checks. This does not turn unretained earlier events into proof.

### Ponytail review

Lean already. Ship.

The correction uses Boot's existing bound properties and Spring's `StringUtils.hasText`. The `ponytail:` comment names the real ceiling: a future Boot rewrite trigger requires review of this predicate. No new complexity finding.

### Verdict and self-check

**PASS for combined handoff; no remaining blocking or non-blocking findings.** CR-01 is resolved, and the unchanged acceptance assessments carry forward; ACs 11–14 now pass within the SPEC's peer/header boundary. The [security re-review](02-security-review.md) supplies the previously missing corrected record for proof item 12. QA must judge that item through lead sequencing item `qitem-20261003194346-b74b8081` before integration/acceptance; this review does not issue that QA receipt.

Exact candidate, complete delta/file coverage, fresh gate, independent reproduction, design correction and QA evidence checked. Product/tests/SPEC/design untouched. Actual remote TCP, real IPv6, headerless local relays and future Boot versions are not newly claimed as tested.
