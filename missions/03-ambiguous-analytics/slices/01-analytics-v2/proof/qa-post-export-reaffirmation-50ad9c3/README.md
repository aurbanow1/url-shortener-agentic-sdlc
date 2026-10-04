# Post-export shared-document re-affirmation

QA packet `qitem-20261004052725-7bd25702`; judge `qa-agent@urlshort-factory` (Codex).
The evidence-export prerequisite closed at transition 2093 before this work resumed.
Shipped subject: `50ad9c3ab9e65baa4100ede1772b514322957fa5`.
Main read: `2522e6c2fe8cbedebabcc01e3a91a619432feed3`.
Compared with receipt commit `0e125ca70b02f005106d2e6fec572fd1ec4f039e`.

| Item | Verified by document/evidence comparison | Previous → new receipt |
|---|---|---|
| 2 — AC-1–15 green named-test coverage | Complete analytics TRACE section unchanged, original gate/effects/authorized replay evidence unchanged. Historical candidate attribution retained; no fresh suite run. | 15 → 19 |
| 5 — AC/rule/requirement mapping | Complete analytics TRACE section unchanged, including rules 1–11 and every candidate-test mapping; inventory and invocation-attribution hashes retained. | 16 → 20 |
| 6 — gaps and NFR-L1 measurement | Complete analytics GAPS section unchanged, including trusted loopback proxy, raised budgets, load-generator percentiles and unretained client samples. Historical measurement remains bounded to its original run. | 17 → 21 |

The only changes to the two shared QA files are the appended mission-02
`06-client-identity` recheck: 377 TRACE lines and four GAPS lines. The analytics
sections are exactly byte-equal: 50,747 TRACE bytes and 5,096 GAPS bytes, including
their separators. README's added CI/CD section also concerns mission 02; existing
analytics text is retained. [Comparison audit](audit.json) records both whole-file
hashes, section hashes, the main read SHA and all initial judgment IDs.

## Self-check

Initial live proof: 9 accepted, only items 2/5/6 unknown, no issues. Exact historical
shared-file hashes at `0e125ca7` match receipts 15/16/17. All 42 other latest evidence
references retain their hashes. No product/test edit, build, HTTP journey or benchmark
run in this task. Final live proof is READY 12/12 with no unknown items or issues.
All 48 latest evidence references were independently rehashed with zero mismatches;
the nine previously accepted judgment IDs remain unchanged. Only items 2, 5 and 6
were re-judged. Receipts 19/20/21 bind the shipped subject and name the main read SHA.
The explicit-path commit carries this record, the immutable comparison audit and
the three receipt files; the packet closure records its actual commit SHA.
