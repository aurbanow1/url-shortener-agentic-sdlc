# QA evidence re-affirmation — final product 30f8de4e

Packet `qitem-20261004041607-09149d20`; judge `qa-agent@urlshort-factory` (Codex).
Subject: `30f8de4e647b05ff54cde09f1019ae519b00069d`.
Only the 15 initially unknown items below are in scope; already accepted items remain untouched.
Historical run evidence retains its original candidate and attribution. This is a document/evidence
audit, with no new application, test-suite or benchmark run.

| Slice | Initially unknown items | Independent comparison | New receipts | Readiness |
|---|---|---|---|---|
| 01-greenfield-core / 01-create-redirect | 14 | RELEASE versus `973bc1a`: original secret-scan/configuration record retained; shipping decision and self-check added. Six other references retain their hashes. | 14 → 15 | READY 14/14 |
| 01-greenfield-core / 02-analytics | 1, 4, 5, 10, 11, 13 | TRACE sections versus `f7ee87e` and all seven GAPS entries versus `15a8f9c` unchanged. ERD retains every original non-comment line, including click/link and user-agent relationships; later audit columns added. ADR-0011/0013 original bodies unchanged excluding Status; ADR-0012 unchanged. Original ADR index entries and historical chronology retained. Item 10 qualification below. | 1 → 33; 4 → 34; 5 → 35; 10 → 36; 11 → 37; 13 → 38 | READY 13/13 |
| 02-brownfield / 01-audit-read | 1, 4, 5, 11, 13 | TRACE/GAPS slice sections versus `2f97eee` unchanged. ADR-0019 original body versus `0052efb` unchanged excluding Status; separate client-identity amendment added. DESIGN audit rule/trust-boundary cells unchanged; label changes from designed to merged. README adds loopback launch/audit guidance and other slices' settings. Original indexed chronology retained. | 1 → 27; 4 → 28; 5 → 29; 11 → 30; 13 → 31 | READY 13/13 |
| 02-brownfield / 03-dogfood-fix | 5, 6, 8 | TRACE/GAPS slice sections versus `84d3604` unchanged, including QA-OPR-02 closure. ADR-0010 versus `0d000da` and ADR-0016 versus `0982cb5` unchanged except Status; both indexed amendments/chronology retained. | 5 → 11; 6 → 12; 8 → 13 | READY 8/8 |

## Content change reported to the lead

Analytics item 10: ADR-0011's later **02-click-retention** amendment changed substantive shutdown
wording at `bfc642db`: it now says the writer calls `shutdownNow()` after five seconds and a running
insert can ignore the interrupt, instead of claiming neither owner interrupts JDBC. The original
02-analytics decision body is unchanged and already describes the outcome-unknown deadline.
This correction belongs to a later slice; it is reported explicitly rather than silently treated as
metadata. The lead's 2026-10-04T04:23Z disposition permits original-slice-only re-affirmation:
the correction was approved before commit (mission 02 NOTES, 23:33Z), resolved as review-agent
W2P-01 in pre-review `82340b8`, and adopted by final wave review `bd74b509`.
I read that resolution and adoption in `docs/review/02-brownfield/wave-review-review-agent.md`.
Item 10 does not accept the later retention amendment on 02-analytics' behalf.

## Self-check

At the initial audit, 140 evidence references matched their retained receipts; 21 occurrences in
living documents changed. Historical bytes were located by matching their recorded SHA-256 in Git,
then compared with current bytes. Slice QA sections, operator trust boundary, original ADR bodies,
ERD relationships and relevant release record were checked independently. No product or test edits.
All 15 receipts bind the full subject SHA above; receipt numbers are slice-local files in each
`proof/judgments/` ledger (eight-digit filenames). Final `rig proof show` reads report all four
slices READY, with no unknown items or issues. Independently rehashed all 162 latest evidence
references: zero mismatches. All 33 previously accepted judgment IDs are unchanged. Main's
product, tests, build and scripts have no diff from the pinned subject despite later documentation
commits. The explicit-path commit and queue closure carry this table and those 15 receipt files.
