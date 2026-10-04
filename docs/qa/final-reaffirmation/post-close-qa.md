# Post-close documentation re-affirmation — QA

Operator packet `qitem-20261004060919-eb2b3934`; judge `qa-agent@urlshort-factory` (Codex).
Main read: `10915b6adf3bf5f50168b1498005f1641536a871`.
Baseline: `2a47ad282c4a1ef3458599cb86f5e7537f47379d`, whose file hashes match the
four retained judgments. All four original subjects are
`30f8de4e647b05ff54cde09f1019ae519b00069d`; these subjects are preserved.

| Slice / item | Current text supports the original claim | Previous → new receipt |
|---|---|---|
| 02-analytics / 10 | ADR-0011/0012/0013 files and historical chronology evidence retain their hashes; their DESIGN §7 index rows are byte-identical. Original-slice-only scope remains; no new acceptance of later retention or analytics-v2 amendments. | 36 → 39; READY 13/13 |
| 01-audit-read / 11 | ADR-0019 and historical provenance retain their hashes; its index row is byte-identical, including guard/keyset decisions and plan-lock chronology. Later client-identity refactor is outside this re-affirmation. | 30 → 32; READY 13/13 |
| 01-audit-read / 13 | DESIGN Audit read row is byte-identical. README's new Audit read paragraph explicitly says direct loopback, neither forwarding header, no opening setting and the connection/header/local-proxy trust boundary. | 31 → 33; READY 13/13 |
| 03-dogfood-fix / 8 | ADR-0010/0016 and historical document checks retain their hashes; both indexed amendment rows are byte-identical. The API-document row now correctly calls ADR-0010 accepted. | 13 → 14; READY 8/8 |

I diffed both bound files from their retained bytes. DESIGN's changes are the
three flowchart-label quotes (`859f31d1`), the purge hold setting, the accepted
OpenAPI amendment/exposure-backlog wording, and the V4 data-model entry
(`10915b6a`). These preserve the four claims. README's restructure (`540621ac`)
keeps the required operator boundary in full. No unsupported assigned item remains.
[Immutable audit](post-close-qa-audit.json) records old/current whole-file hashes,
the six unchanged ADR index rows, the unchanged audit rule and original receipt IDs.

## Self-check

Only the four assigned items were initially unknown. Fourteen other references
bound by those items match exactly; only four DESIGN references and one README
reference changed. The requested product/build diff from `30f8de4e` is empty.
No new product run, tests or benchmark, and no edits to README, DESIGN or product.
Final live proofs: all three slices READY, with no unknown items or issues.
All 134 latest evidence references rehashed without a mismatch; the 30
previously accepted judgment IDs remain unchanged. The four new receipts preserve
their exact original subjects and name the checked edits. Only the evidence record,
immutable comparison audit and four new receipts are included in the explicit-path commit.
