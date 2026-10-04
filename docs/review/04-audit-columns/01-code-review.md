# Code review — 04-audit-columns

**PASS, no findings.** Exact candidate `305f8045d45b19a9e3287d5fe3508af6e04db9a4`; base `2566c38c2e8c434e93703a3dabc2edba14eca60c`. Reviewer `review2-agent@urlshort-factory` (Codex), 2026-10-04 UTC. Combined code/security packet `qitem-20261003235705-60b5c52b`, instance `01M41HG4P6DEVAKTCJC6J9QAWP`. Security verdict is in [02-security-review.md](02-security-review.md).

## Context and coverage

This completes row-audit columns on `link` and `audit_log` without changing their existing data or HTTP representations. V4 supplies defaults and backfills; create, retire and expired-key release maintain link stamps inside the existing transactions. Confidence is high within this boundary: the full diff and all ten changed files were read, the exact candidate is clean in `.worktrees/04-audit-columns`, and the reviewer executed the complete gate.

Primed from SPEC `e28cfea`, design `5a6d168`, impact analysis `aecb0d9`, AGENTS, shared DESIGN/ADRs, QA SUMMARY/PROOF/traceability/GAPS and the prior design review. Applied review guidance, Java/Spring §§6/8, QA §§2–3, brownfield §7 and the security/database guidance. The ordinary review-team path applies; no SDLC composition was selected. The recorded plan-lock recheck and retention merge `ed2b940` precede this candidate. No product, test, SPEC or design was edited by this review.

`git diff main...slice/04-audit-columns --stat`: ten files, 739 insertions, four deletions. The following ledger covers the full files, including unchanged surrounding code; hashes are in [evidence-audit-305f804.json](proof/evidence-audit-305f804.json).

| Changed file | Verdict |
|---|---|
| `src/main/java/dev/urlshort/link/LinkRepository.java` | PASS — bound ids/instants; retire stamps in its conditional UPDATE; targeted stamp retains the existing record shape. |
| `src/main/java/dev/urlshort/link/LinkService.java` | PASS — two stamp calls use the create's single clock reading; save/release/stamp/audit share the transaction; replay and mismatch return before mutation. |
| `src/main/resources/db/migration/V4__add_link_audit_columns.sql` | PASS — exact reviewed SQL, seven added non-null columns, defaults and specified backfill; literal rollback removes only V4 additions/history. |
| `src/test/java/dev/urlshort/link/LinkAuditColumnsTest.java` | PASS — compares old metadata/constraints, executes the header rollback and reapplies V4 on file databases. |
| `src/test/java/dev/urlshort/link/LinkServiceStampTest.java` | PASS — verifies the same instant and transaction-local sequencing at the service boundary; functional storage assertions complement these collaborator checks. |
| `src/functionalTest/java/dev/urlshort/link/LinkAuditColumnsJourneyTest.java` | PASS — fixed clock distinguishes writes/no-ops; full row comparisons, unchanged responses, append-only audit and client-value canaries. |
| `src/functionalTest/java/dev/urlshort/link/LinkAuditColumnsFailureJourneyTest.java` | PASS — failing audit append rolls back the link retirement and stamp; full before/after row checked. |
| `src/functionalTest/java/dev/urlshort/link/LinkUpgradeJourneyTest.java` | PASS — real startup on an existing directory, old values and backfill including future event time; synthetic fixture scope explicit. |
| `src/test/java/dev/urlshort/click/ClickAuditColumnsTest.java` | PASS — sole change pins its V3-specific migration check to V3, under the lead's explicit grant. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionStartupJourneyTest.java` | PASS — sole startup argument pins the V3 schema assertion to V3; assertions retained and the scenario does not call the new link-write path. |

The two inherited-test pins were authorized in `132a884cee2e254b68d29eb90e6d644e2b7cb009`, recorded in `slice.yaml` and mission NOTES. They preserve the tests' V3-specific contract; new V4 journeys exercise the latest schema. Independently hashed all 59 baseline test/resource files: 57 identical, only these two granted changes. No assertion was removed.

## Acceptance and QA evidence

| AC / rules | Reviewed implementation and behavioral evidence |
|---|---|
| AC-1, AC-7; BR-1/5/6 | V4 matches the design byte-for-byte (SHA-256 `e1cf67add7d5dc55152504e19a60cbc5f3ad3291c9a1bc1af33e168e6da055a7`). Schema tests and QA's actual `f6dd29e` service directory preserve all old columns, constraints and values. Active/retired/released links backfill as specified; future audit timestamps are capped. |
| AC-2, AC-3, AC-4; BR-1/2/3 | `LinkAuditColumnsJourneyTest` create/retire/key-release cases plus service unit tests verify service-clock stamps, fixed actors and preserved creation time. QA row captures independently distinguish service time from audit insert's database time. |
| AC-5; BR-2 | Replay, mismatched key, repeat retire, invalid URL and failed audit leave stamps unchanged. Failure journey is supplemented by QA's real JDBC CHECK rejection after the retirement UPDATE: full link row and audit list unchanged, sanitized 500. |
| AC-6; BR-3/4 | Fresh `AuditJourneyTest` and `AC06_auditRowsAreNeverUpdated` compare stored audit rows; unchanged `AuditLog` remains INSERT-only inside the caller transaction. V4's one-time backfill is the specified migration operation. |
| AC-8; BR-7 | `AC08_noResponseShowsTheNewColumns`, unchanged OpenAPI test and QA HTTP captures. `Link`, responses and audit read's named SELECT are unchanged. Existing `createdAt` and audit `actor` remain permitted values under the clarified AC. |
| AC-9 | Fresh full gate: 218 unit and 233 functional invocations, including 437 inherited and 14 new. Two V3 pins are authorized as above. |
| AC-10; BR-3 | `AC10_theAuditColumnsHoldNoClientValue` and QA canaries verify timestamps/static actors only; no new logs, metrics or public fields. |
| AC-11; BR-5 | Header rollback read in full and executed by fresh `AC11_theWrittenRollbackRestoresTheEarlierSchemaAndV4AppliesAgain`. QA also ran the same eight literal statements on a stopped copy, then started the real candidate to reapply V4. Prior independent design controls already exercised this identical SQL. |

[Fresh gate log](proof/code-check-305f804.txt): `../../scripts/gw --offline check --rerun-tasks` (with wrapper `--log`) succeeded in 1m 8s, all 14 tasks executed, including Javadoc. XML sums show **218 unit + 233 functional**, zero failures/errors/skips. Fresh coverage sums match QA: unit 490/557 lines and 194/200 branches; functional 523/557 and 166/200; merged **557/557 lines, 200/200 branches**. Only `test.exec` and `functionalTest.exec` are present; no auxiliary execution data contributes.

[Read-only reconciliation](proof/audit-candidate.py) independently checked all **366 committed report hashes**, saved and fresh JUnit totals, 271 source-method/traceability mappings, all 11 ACs and seven rules, and the identical slice traceability section. Of those methods, 230 have individual XML name matches; 41 parameterized methods are attributed at class level, as QA disclosed. This does not claim invocation-level names where the XML lacks them.

Read QA's entire external runner, Gradle fixture and Python driver. Audited the final `proof/qa-305f804/run-20261003T234116500899Z` captures: 787 recorded passing assertions, **94 independently recomputed request-id/status joins** to the correct app's JSON log, complete saved response headers/bodies, schema/data comparisons, actual failed-write rollback and literal rollback/reapply. Both recorded jar hashes still match. All seven applications have recorded exit statuses. These are audited QA observations, not a new reviewer execution of that external driver; the canonical suites above are the reviewer's fresh execution. Stopped timestamp/future-event fixtures are explicit, not claimed as ordinary HTTP writes.

## Ponytail review

Lean already. Ship.

One migration, one targeted repository method and two service calls meet the approved contract. Existing Spring transactions, parameter binding and database defaults do the work; no dependency, layer, trigger or unused generalization is added. The existing `ponytail:` collision/concurrent-key ceilings are accepted intent and unaffected.

## Findings and merge readiness

None. MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fixes in passing or new backlog items requested. **Code PASS; security PASS; hand off to integrate.**

[Live proof snapshot](proof/live-proof-305f804.json) has items 1–4 and 6–9 accepted on this exact candidate, with all cited hashes reconciling; **item 5 is pending**, so this is not a claim that the slice is already proof-ready. In `qitem-20261003234855-d2ef129b` the lead explicitly committed to close the interim link/audit_log rows at this merge, also close the missed click/user_agent_class rows from `ed2b940`, and return item 5 to QA with the final GAPS hash. The integration handoff must retain that obligation and final reconciliation of later shared-file appends.

Residual limits: no new PostgreSQL, Docker or large-directory timing run; release owns installed startup/readiness and advisory freshness. Link timestamps use the service clock, audit insert timestamps use the database clock, and historical key-release times cannot be recovered; these are specified boundaries, not new findings.

## Self-check

Exact SHA/clean worktree verified; 10/10 changed files read; fresh gate and QA evidence audited; no unsupported defect claim; severity rule applied; code and security ledger rows recorded with this SHA. All review-authored changes are confined to `docs/review/`.
