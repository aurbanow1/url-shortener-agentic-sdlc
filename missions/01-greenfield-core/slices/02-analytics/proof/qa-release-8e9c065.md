---
slice: OPR.99.0.2.2
candidate_sha: 8e9c065589e53385f60d6be3ddbc3683260285df
artifact_type: qa
verdict: PASS
money_evidence: Corrected100/s bench and12000clicks reconcile; sequential
  GET/HEAD isolation limit disclosed; receipts00000025-29 accept assigned
  measurement and reaffirmations.
evidences:
  - "1"
  - "4"
  - "5"
  - "10"
  - "13"
self_check: Read exact contracts/reviews and raw release records; independently
  checked product-input equality, secret screens, complete JSON/status/click
  counts, table/ADR chronology and final GAPS367567e. No fresh build/app/load
  claim; no isolated-cost or AC28 waiver.
---

# QA release judgments — merged 8e9c065

QA Agent (Codex), 2026-10-03; packet qitem-20261003153201-3bc62f5f.
Subject: `commit:8e9c065589e53385f60d6be3ddbc3683260285df`.
This is an audit of assigned release records, not a new application or build run.

The [machine-readable audit](audit-8e9c065.json) pins the files read and records
the independently executed checks. The release's built commit
`f09010396d584fdf41702bb99856aa17a1ec1206` has an empty product-input diff
from that merged subject (source, suites, build inputs, container configuration
and API document). Current records were read after `3ec7ab4`.
The discarded first bench is excluded.

| Item | Judgment | Evidence and scope |
|---|---|---|
| 01-create-redirect #14 | accept | Read explicit NFR-S4 rows in both independent review reports, RELEASE §4's tracked-tree scan and §3.1's environment smoke. Independently repeated the high-signal signature screen at f090103: git exit 1, zero matches across 2,012 tracked paths. The narrower literal-assignment pattern printed in the audit finds four occurrences, all reads of environment or parsed endpoint metadata, independently inspected; this count is not the release's differently scoped 12-match count. Embedded H2 password is empty; no tracked .env. The installed environment smoke records shortUrl https://sho.rt.example/dpMkbHzk, full smoke and 73 JSON events with searched canaries absent. Earlier QA independently observed the environment override on the original candidate. No credential identified within the specified methods; no universal, full-history, untracked-file, image or dedicated-scanner claim. |
| 02-analytics #13 | accept measurement and disclosed limit | Audited tools/bench.mjs: scheduling does not wait for earlier responses; duration includes late timers/connection waits and complete body consumption. Corrected simultaneous workload is 6,000 GETs at 100.0/s, p95 2.2 ms/p99 3.3 ms, plus 1,200 creates at 20.0/s, p95 2.8 ms, no bad responses. Separate GET/HEAD runs each offer/achieve 100.0/s for 60 s: p95 1.7/1.8 ms. Independent JSON parsing finds 19,205 completions: 18,000 302, 1,201 201, four 200; 19,237 events, three startup WARN and no ERROR. Stats total 12,000 equals all GETs, so click recording was active. Sequential GET-minus-HEAD p95 is a proxy, not an isolated added-cost quantile. GAPS explicitly records that limit as proof item 13 permits; no isolated numerical-cost guarantee or waiver. |
| 02-analytics #1 | reaffirm | Entire current 5b3490c analytics traceability section byte-compared with the prior accepted record at 5b2cdb2: unchanged, 138 methods (69 per suite), all 22 ACs have functional mappings. Independent earlier candidate gate/isolated regression evidence remains intact; merged release check log also records test, functionalTest, coverage verification, Javadoc and BUILD SUCCESSFUL. No new test execution claimed by this record. |
| 02-analytics #4 | reaffirm | Full table and all nine rule mappings checked, with FR/NFR IDs retained; changes elsewhere in shared TRACEABILITY add other slices rather than removing analytics coverage. |
| 02-analytics #5 | reaffirm | All earlier analytics gap rows retained. Latest release rows after 3ec7ab4 disclose five failed Mac AC-28 runs, one retained namespace control and the separate analytics isolation limit. No merged coverage exclusion or untested AC was introduced; benchmark isolation limit follows the locked contract. |
| 02-analytics #10 | reaffirm | ADR-0011/12/13 contents equal their accepted versions. DESIGN §7 still indexes all three. Accepted/indexed at plan-lock commit 4cfb745 before first dependent product commit 2499505 by commit timestamp; docs and product were on parallel branches, so no ancestry assertion is made. Later DESIGN additions concern 03-operate. |

The release bench reports rounded percentiles, not per-request latency samples;
QA cannot independently recompute their percentile values. The retained raw
summary, reconciled server event counts/statistics and inspected instrument
support the bounded measurement record. One laptop, one run, raised admission
budgets and the shipped file-H2 mode do not establish capacity or an isolated
counterfactual click-cost distribution. See the final analytics row in GAPS.

The generated API schema's missing errors[] and anonymous disk-path metric are
known MEDIUM/LOW defects independently reproduced in
[dogfood](../dogfood.md). Whole-document generation equality never attested
schema completeness. They do not change the specifically assigned receipt
claims. 03-operate #13 and the failed host restart AC-28 remain QA2/the human's
scope; this record supplies no delivery stamp, ship approval or AC-28 waiver.

## Self-check

Read exact contracts and prior judgments; inspected review/installed environment
records; reran tracked-tree scans without printing or storing matching values;
compared built and merged input trees; reconciled the complete bench log and
click totals; compared the entire analytics table and old gap rows; checked ADR
contents, index and chronology; wrote the SPEC-authorized isolation limit before
judging. No product, tests, build configuration or release artifact changed.
No extra application, build, benchmark, container or natural-midnight run.
Dogfood apps were stopped before this audit. Receipts use the merged commit
subject and current evidence hashes, with deliberate replacements for the four
invalidated analytics judgments.
