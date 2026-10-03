# Requirements review — 04-audit-columns

Initial candidate: `b95f3e2683d7dcbf06fbe5a27b378743f3f8211e`.
Final candidate: `e28cfeaa56f4cf5da59529915f4c5d34b6606899`
(AC-8 correction `0776182`, then its status-note correction).
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003185548-520967ab`;
instance: `01M41HG4P6DEVAKTCJC6J9QAWP`.

**PASS — initial HIGH RQ-01 fixed before packet exit; no open findings.**
The initial contradiction and focused re-review are retained below.

## Context and complete coverage

The outcome is auditable row creation/update metadata on the shipped `link`
and `audit_log` tables, with existing client behavior preserved. The three
link writes maintain update stamps; audit rows remain append-only. Confidence
is high in the contract and the corrected compatibility boundary. Implementation,
migration performance and merged audit-read behavior are not yet verified.

Read the entire SPEC and candidate diff: **one changed file, one reviewed**.
Also read the human policy packet `qitem-20261003175330-fb054f2f`, mission
amendment, slice allocation/territory/dependencies, database §§2/4/8,
requirements §6, brownfield §7, review guidance and ADR-0020. Checked the locked
audit-read representation and actual shipped V1, `Link`, `LinkService`,
`LinkRepository`, `LinkResponse`, `AuditLog` and the existing create/read test
assertions. No additional SDLC composition is selected.

| Changed file | Initial verdict |
|---|---|
| `missions/02-brownfield/slices/04-audit-columns/SPEC.md` | RQ-01 HIGH: AC-8's global value ban contradicts preserved public values; otherwise complete. |

| Requirement / AC | Assessment |
|---|---|
| Policy, NFR-X2 / AC-1 | Named columns/types/non-null constraints and unchanged shipped schema are observable; names come from the human policy and existing schema. |
| Policy / AC-2–4 | Create, conditional retire and expired-key release each have explicit row effects. Existing creation time is reused; link update time uses the same business clock, with an ADR obligation for the departure from database-default row times. |
| Policy, FR-13 / AC-5 | Replay, refused writes, reads and transactional rollback leave stamps unchanged; successful retire is the explicit exception. |
| NFR-A2 / AC-6 | Audit metadata equal for row lifetime, application remains insert-only; migration backfill is separately required. |
| FR-13, NFR-X2 / AC-7, AC-11 | Existing directory, old values, actor backfill, lost historical key-release time, actual rollback and reapplication all addressed. No fabricated historical timestamp is claimed. |
| FR-13 / AC-8 | Initial contradiction RQ-01; corrected below. Audit-read exact eight-field representation and OpenAPI preservation stay mandatory. |
| FR-13 / AC-9 | Existing suites unchanged, including out-of-territory inserts that omit added columns. The requirement is a compatibility constraint, not permission to edit those tests. |
| NFR-P1/O2 / AC-10 | Canary headers/key/address prohibited in audit columns; static actors. Inherited logging/privacy obligations remain applicable. |
| NFR-M1/M2 / proof and non-functional sections | Merged coverage, per-suite reports, traceability, GAPS, ADR and pre-design impact analysis explicit. |

All 11 ACs use GIVEN/WHEN/THEN; seven rules cover the write and compatibility
edges. Seven ambiguity rows have six safe assumptions and one locked-contract
decision, none parked. Backfill bounds and the missing historical key-release
time are honest. V4 is provisional: the lead must confirm migration order and
re-check the impact analysis after both wave-1 merges, as the manifest requires.
No additional endpoint, actor system, index, trigger or library is prescribed.

The nine proof items name tests, merged and per-suite coverage, traceability,
GAPS closure, upgraded schema/rows, a run rollback/reapply and impact analysis.
Schema/row inspection is the appropriate observable surface for this explicit
schema-policy slice. No unrelated public API is needed to prove it.

## Initial finding

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| RQ-01 | HIGH | `missions/02-brownfield/slices/04-audit-columns/SPEC.md:117` at `b95f3e2` | AC-8 preserves the shipped response and also forbids every audit-column name/value in a body, header or log. AC-1/2 reuse `link.created_at`; the shipped response exposes that instant as `createdAt` (`LinkResponse.java:19`). Fresh create and read-after-retire both expose `2026-10-03T18:57:55.676Z`, so the global absence condition cannot hold while preserving behavior. Independently, locked audit-read AC-1/rule 3 exposes `actor=anonymous`, which AC-2 requires in the new actor columns. | Forbid added response fields/exposure sourced from the newly added columns, while preserving existing `createdAt` and audit `actor`. Remove the global value-absence assertion; shared values are not proof of new-column exposure. Preserve a concrete unchanged-shape check after an update. |

Evidence: [`proof/requirements-source-check.txt`](proof/requirements-source-check.txt),
[`proof/baseline-create.json`](proof/baseline-create.json),
[`proof/baseline-read-retired.json`](proof/baseline-read-retired.json), and the
corresponding create/retire header captures. The audit-read example is a
locked-contract comparison, not a newly exercised audit endpoint.

## Re-review e28cfeaa56f4cf5da59529915f4c5d34b6606899

The author supplied `0776182` and `e28cfea` before the review packet closed.
Read both complete diffs, **one file changed/reviewed**, and byte-matched the
final SPEC. **RQ-01 fixed:** AC-8 enumerates only the added columns, preserves
the existing creation time and actor, and explicitly requires no update time
in a read after retirement. Rule 7 agrees. The status note no longer makes an
unverified blanket claim about existing logs. AC-10 still forbids client
values in the columns. No additional finding introduced by the correction.

## Verification and limits

- Exact initial/final SPEC hashes and AC checks are in the source-check file.
- `scripts/gw --offline bootJar` succeeded, all four tasks up-to-date; wrapper
  log: `proof/requirements-baseline-build.txt`.
- Fresh baseline jar used JDK 21, `127.0.0.1:18096` and an isolated in-memory
  database. Observed readiness `UP`, create `201`, retire `204`, read `200`;
  both bodies have exactly the five shipped fields and identical `createdAt`.
  The process was terminated and graceful shutdown observed in
  `proof/baseline-app.log`. HTTP header line endings were normalized for Git.
- The successful baseline gate from the immediately preceding review is
  reused: [`../05-ci-cd/proof/requirements-baseline-check.txt`](../05-ci-cd/proof/requirements-baseline-check.txt),
  all 14 tasks up-to-date. Verified `src/` and `build.gradle.kts` are identical;
  no repeated gate is warranted for another SPEC-only change.
- These observations prove the existing-value contradiction and its corrected
  boundary. They do not prove the unwritten migration, new stamps or future
  merged audit-read implementation. Those remain design/build/QA obligations.

## Self-check and exit

Complete original file and both correction diffs read; human decision checked;
one finding reproduced and resolved; final candidate verified. Only review
artifacts authored. **Handoff to design**, zero open findings. No new backlog
item. The lead retains the wave-1 prerequisite and migration-number decision.
