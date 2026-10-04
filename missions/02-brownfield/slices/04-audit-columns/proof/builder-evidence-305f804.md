---
slice: OPR.99.0.3.4
candidate_sha: 305f8045d45b19a9e3287d5fe3508af6e04db9a4
artifact_type: qa
verdict: PASS
money_evidence: the real f6dd29e jar wrote links and audit rows; the candidate
  jar on the same directory applied V3 and V4, kept every response, and
  backfilled link updated_at to COALESCE(retired_at, created_at) and audit
  created_at = updated_at = occurred_at with anonymous actors; V4's written
  rollback restored the shipped schema and values on a copy, and the candidate
  re-applied V4; gate green, unit 218, functional 233, 557/557 lines, 200/200
  branches
evidences:
  - "6"
  - "7"
self_check: "I read every capture: the column listing shows the new columns NOT
  NULL with their defaults and lengths; the link and audit rows show the
  backfill and the new-row stamps; after the rollback the column and constraint
  listings match the shipped schema and the link values are unchanged; after the
  re-apply the history shows 1 to 4 TRUE; the gate log ends BUILD SUCCESSFUL"
---

# Builder evidence — 04-audit-columns, candidate 305f804

This is evidence from the builder seat, `development-agent@urlshort-factory`, not a verdict. The narrative is in `PROOF.md` §Builder.

- **Contract item 6 (AC-1, AC-7 by effect).**
  - The real `f6dd29e` jar wrote a data directory: an active link with a key, a retired link, a click.
  - The candidate jar started on it and Flyway applied V3 and V4. Responses are unchanged, and the audit read shows exactly its eight fields.
  - The columns after the upgrade: the new ones are `NOT NULL` with their defaults and `VARCHAR(64)`; the shipped ones are unchanged.
  - Links are backfilled to `COALESCE(retired_at, created_at)`. Audit rows have `created_at` = `updated_at` = `occurred_at`, and their actors are `anonymous`.
  - Rows written after the upgrade are stamped as designed.
- **Contract item 7 (AC-11 by effect).** On a copy:
  - V4's written rollback restored the shipped columns and constraints and left the values unchanged, with history V1–V3;
  - the candidate then re-applied V4 and backfilled again.
- **Gate.** `check --rerun-tasks` on `305f804`: unit 218, functional 233, 557/557 lines and 200/200 branches merged. The only shipped tests changed are the two lines granted at `132a884`.

## Media

![upgrade-0-shipped-f6dd29e.txt](upgrade-0-shipped-f6dd29e.txt)
![upgrade-1-read-active.txt](upgrade-1-read-active.txt)
![upgrade-2-retired-410.txt](upgrade-2-retired-410.txt)
![upgrade-3-audit.txt](upgrade-3-audit.txt)
![upgrade-4-columns.txt](upgrade-4-columns.txt)
![upgrade-5-schema-and-new-rows.txt](upgrade-5-schema-and-new-rows.txt)
![jar-log-upgrade.txt](jar-log-upgrade.txt)
![rollback-1-after-rollback.txt](rollback-1-after-rollback.txt)
![rollback-2-reapplied.txt](rollback-2-reapplied.txt)
![jar-log-reapply.txt](jar-log-reapply.txt)
![builder-check-305f804.txt](builder-check-305f804.txt)
![builder-red-functional.txt](builder-red-functional.txt)
![builder-red-unit.txt](builder-red-unit.txt)
