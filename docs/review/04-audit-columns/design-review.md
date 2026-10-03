# Design review — 04-audit-columns

**Verdict: PASS, no findings.** Candidate `5a6d16824c8e7f071ebfca4ef8f0b17ce44d5e04`, with prior impact-analysis/probe commit `aecb0d94093e6efa28a4b316a4eb8238b1ad0259`; SPEC `e28cfeaa56f4cf5da59529915f4c5d34b6606899`. Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03. Packet: `qitem-20261003190948-2c1e2288`.

## Context and coverage

The change completes the human's audit-column policy on `link` and `audit_log`, preserving existing responses, stored values and shipped tests. One expand migration fills the columns; the three existing link writes maintain update stamps in their existing transactions. Confidence is high in this design boundary: all nine authored files were read, every AC has a reachable mechanism and named test, and the exact migration and its literal rollback passed independent controls.

This is a document review in the shared main checkout; no implementation worktree exists yet. [Source verification](proof/design-source-check.txt) records candidate hashes matching all nine files, the SPEC and approved V3 input. The product/build inputs still equal baseline `f6dd29e`. Reviewed against `review.md`, architecture §§3–8, databases §8 (including the human's audit-column decision), brownfield §7, the SPEC, ADR-0020 and the existing writers/tests. Review-team selected the ordinary path: no SDLC composition is selected for this packet.

Paths below are relative to `missions/02-brownfield/slices/04-audit-columns/` unless prefixed `docs/`.

| Changed file | Verdict |
|---|---|
| `impact-analysis.md` | PASS — all writers, mapping and shipped test constraints identified; committed before design; merged-main recheck assigned. |
| `design.md` | PASS — all 11 ACs reachable, existing transaction/error contract retained, territory and test plan explicit. |
| `design-probe/LinkMigrationProbe.java` | PASS — read K1–K5 implementation; printed comparisons checked against output, independently asserted below. |
| `design-probe/link-migration-probe.gradle` | PASS — uses existing functional runtime classpath and Java 21; no dependency added. |
| `design-probe/migration/V4__add_link_audit_columns.sql` | PASS — seven added columns, specified backfill, defaults preserve legacy inserts; written rollback run verbatim. |
| `design-probe/output.txt` | PASS — results agree with claims; K5 measures 6,153 ms wall for 100,000 links and 150,000 audit rows. |
| `docs/DESIGN.md` | PASS — records the additive columns and service-clock link stamps without widening the public contract. |
| `docs/adr/0020-audit-columns-expand-migration.md` | PASS — amendment records reused link creation time, three write stamps, actor width and audit backfill. |
| `docs/diagrams/erd.mmd` | PASS — matches the proposed columns; existing relationships retained. |

## Acceptance and design checks

| ACs | Assessment |
|---|---|
| AC-1, AC-7, AC-11 | Exact V4 adds the missing policy columns, preserves prior values/types/nullability/defaults and keys, caps future audit event times at migration time, and supports literal rollback/reapply. Schema/upgrade tests and the real baseline-directory capture are named. |
| AC-2, AC-3, AC-4 | Create and expired-key release stamp with the create's single service-clock instant; conditional retire stamps inline. Audit insert defaults use the database clock. Clock-controlled journeys distinguish these clocks. |
| AC-5 | Replay/mismatch return before mutations; repeat retire changes zero rows. Existing transaction encloses mutation, stamp and audit. Planned failure journey extends shipped AC-24; independent SQL failure/rollback preserved all link fields. Existing 422/410 and sanitized 500 `ProblemDetail` contract is retained. |
| AC-6 | AuditLog stays insert-only; defaults fill equal row timestamps and anonymous actors. Shipped append-only journey compares complete rows, so added fields participate. |
| AC-8 | Link record/responses remain unchanged; audit read uses named columns. Exact field/header/status comparisons and unchanged OpenAPI test are planned. Audit read must be rechecked on merged main at plan-lock. |
| AC-9 | Existing six-argument Link constructor and repository signatures remain intact. Current mocks tolerate added calls; v1-shaped inserts succeed after migration connection retirement. Full unchanged shipped suites remain implementation/QA evidence, not a design-probe claim. |
| AC-10 | New values are timestamps or fixed actors; backfilled audit actor already belongs to the row. Named canary test covers client values. No new log, metric, endpoint or network entry point. |

The second keyed update after insertion avoids changing the existing record and shipped test constructors. It follows the repository's targeted-update pattern and stays in the same transaction. No speculative layer, trigger, library or index is introduced. New runtime SQL binds ids and instants; constants supply actors. The threat model covers history integrity, disclosure, append-only audit and migration cost. The migration's one-time audit backfill is distinct from an application update path.

## Independent verification and limits

- [Migration check](proof/design-migration-check.txt): `scripts/gw --log docs/review/04-audit-columns/proof/design-migration-check.txt --offline -I docs/review/04-audit-columns/proof/review-link-migration.gradle reviewLinkMigration` succeeded. [Independent source](proof/ReviewLinkMigrationProbe.java) asserts **32 controls** on temporary H2 2.4.240 file databases: fresh/upgrade schema, old values, active/retired/released backfill, 64-character historical actor, future-time cap, legacy inserts after Flyway connection retirement, click foreign key before/after reopen, uniqueness/check rejection, failed audit transaction rollback, conditional retire, key release, literal rollback and reapplication. A failed assertion throws; a clean process alone is insufficient.
- [Baseline gate](proof/design-baseline-check.txt): `scripts/gw --offline check` succeeded; all 14 tasks were **UP-TO-DATE**. This checks the unchanged baseline build state; it does not execute an unbuilt implementation or prove future coverage.
- Read the author's K5 source and recorded timing; did not rerun its larger benchmark or measure combined V3+V4+startup readiness. Release owns installed startup checks. PostgreSQL was not run.
- Plan-lock remains held until both wave-1 slices merge. Recheck impact analysis, audit read's named selection and next Flyway number on that merged main before implementation. This is the existing dependency, not a new finding.

## Findings

None. MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fixes in passing or backlog items requested.

## Self-check and handoff

All nine candidate files read and fingerprinted; independent migration controls and baseline gate inspected; no product, test, SPEC or design edited. This review's ledger row records the same candidate and counts. Hand off to delegated plan-lock with the existing wave-1 dependency and merged-main check intact.
