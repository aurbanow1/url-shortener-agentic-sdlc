# QA release proof return — 50ad9c3

QA: `qa-agent@urlshort-factory` (Codex), 2026-10-04 UTC. Assigned packet:
`qitem-20261004020825-9237265b`. Subject:
`50ad9c3ab9e65baa4100ede1772b514322957fa5`, pre-D21. The lead's transition
1900 and fresh restoration messages require a new review of item 12 and
`797f8fb`; the earlier fallback-model attempt supplies no acceptance here.

## Item 12 — accept the measurement and disclosure

Read the raw benchmark text, post-benchmark statistics and Prometheus
capture; compared the NFR-L1 row in `docs/REQUIREMENTS.md` with RELEASE.md
sections 3 and 7. Ran `rederive.py` from the repository root. Its fresh
`audit.json` records the source hashes, raw evidence hashes, completion
windows, configuration custody and observations below.

| Required condition | Observed evidence |
|---|---|
| One service instance, H2 file DB | Complete installed log has only PID 46435, starts the preserved candidate jar on port 18230, and names `jdbc:h2:file:/private/tmp/urlshort-m03-50ad9c3-db/urlshort`. Current preserved jar SHA-256 matches the release manifest: `fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2`. |
| 100 redirects/s for 60 s | Benchmark records 6,000 requests, offered 100/s for 60 s, elapsed 59,991 ms, achieved 100.0/s. First request is due at zero, last at 59.99 s; the elapsed value matches that schedule. The concurrent create phase records 1,200 requests at 20/s. |
| p95 ≤20 ms; p99 ≤50 ms | Captured client benchmark output: p95 3.7 ms, p99 9.5 ms for GET with creates. Both meet the limits on this run. |
| The specified load really occurred | Raw JSON log has 19,201 completions in the contiguous benchmark block: one seed create plus 19,200 load requests. Source ordering partitions the load into 7,200 concurrent GET/create completions, 6,000 GET-alone completions and 6,000 HEAD completions, each spanning approximately 60 s. The first partition is exactly 1,200 status 201 and 6,000 status 302; the remaining partitions are all 302. |
| Recording was active, without silent losses | Benchmark link has 12,000 stored clicks, one daily unique and no bot. Prometheus has 12,004 GET redirects/recorded clicks (including four prior installed clicks), 6,000 HEAD redirects and 1,202 creates (installed link and benchmark seed included). All five loss reasons and both quota rejection counters are zero. |

The checked `tools/bench.mjs` and `scripts/smoke.sh` bytes equal their Git
blobs at the pinned candidate. The generator schedules each request from
its due time independently of earlier completions, counts transport errors
as status zero, and measures through response completion. Its nearest-rank
percentiles include timer/socket delay. Its exit zero alone is insufficient:
the raw counts, offered rates, bad-response counts and quantiles were also
read and checked. The archived log hash matches the teardown record and
contains graceful shutdown and Hikari close.

The pending NFR-L1 measurement gap can close **for this candidate and run**.
Configuration and measurement qualifications remain: trusted loopback,
budgets raised to 1,000,000/minute, load generator sharing the host with the
service, and possible unrelated host activity. Client latency samples were
not retained, so the recorded percentiles were not independently recomputed.
Server logs deliberately omit paths/methods: phase attribution uses script
ordering plus aggregate Prometheus method counts. No capacity or container
performance result is inferred. GET-minus-HEAD compares two sequential
distributions and proves no isolated added-cost quantile.

## Item 6 — reaffirm the current gap record

The live proof read before this work showed items 1–5 and 7–11 accepted,
item 6 unknown solely because its `GAPS.md` hash changed, and item 12 pending.
`TRACEABILITY.md` and every other retained judgment were current on that read.

Read the complete analytics gap section and compared the `797f8fb` diff.
Its added numbers and bounded closure agree with the raw evidence. No
numerical correction is required. Added the configuration and sample-retention
qualifications above to that release paragraph and linked this audit. The
original qa_check qualifications, complementary suite percentages, controlled
fixture limits, authorized replay disclosures and instrumentation corrections
remain intact. The release snapshot remains a historical pre-judgment copy;
release2 will refresh it from the new attributed receipts.

## Self-check

- Recovered live identity, packet and lead continuation; treated the fallback
  attempt as unverified. Re-derived the result from captured bytes and the
  pinned load-tool source before any new receipt.
- Checked the prescribed rate, duration, one-PID/file-H2 custody, preserved
  jar hash, 19,200 load completions, aggregate method counts, 12,000 stored
  clicks, zero load failures, losses and quota rejections.
- Re-read NFR-L1 and both proof-contract items; checked `797f8fb` and the
  current analytics gap section. Kept numeric closure separate from the
  retained measurement qualifications.
- No new full candidate gate, runtime journey, latency measurement or product
  review was performed. The requested root `scripts/gw --offline test`
  succeeded with tasks up-to-date; earlier QA/review attribution remains.
- No product/tests changed, no app launched, and no other slice was judged.
  Record item 12 and item 6 against the pinned SHA, read their receipts and
  live readiness, then close this standalone packet with no-follow-on.
