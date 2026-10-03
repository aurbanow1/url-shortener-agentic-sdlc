# 01-analytics-v2 — requirements review

Candidate: `b8c327be315e991c84b094c7a3b773b5622429dd`.
Packet: `qitem-20261003161333-c4da0117`; instance: `01M416Z3CM54YQTX93V4KG0CPS`.
Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.

**Verdict: PASS. No findings.** Hand off to design. No additional product
decision is required by this review.

## Context and coverage

The selected outcome adds two per-day figures to the existing statistics
response and aligns the click identity with the already-configured proxy rule.
Existing figures, privacy reductions, UTC grouping and the separately owned
purge remain. Read the complete candidate SPEC and its single-file change;
mission brief/allocation, slice manifest, NOTES/PROGRESS/PROOF; the earlier
question document at `88d87a9`; relevant baseline requirements, v1 criteria,
current recorder/store/salt and ADR-0015; requirements/review/brownfield guides.
Confidence in scope, recorded intent and proof mapping: high. Query structure,
identity sharing, counter placement and test mechanisms remain design work.

| Changed file | Verdict |
|---|---|
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md` | Read in full; PASS |

**1 changed file / 1 reviewed.** Independently verified the working document
equals `git show b8c327b:<path>` and all 15 numbered ACs contain GIVEN/WHEN/THEN.

| Review area | Evidence and assessment |
|---|---|
| FR-16 / human intent | Live transition 876 on `qitem-20261003154347-19e96a75` is attributed to `human@kernel`, 16:10:30Z, and exactly matches the quoted six answers. Compared their meanings with the parked `88d87a9` questions, including proxy alignment in Q2. No extra outcome selected. This is a recorded-decision proof obligation, not an HTTP feature. |
| FR-8 v2 | AC-1–6 specify exact empty/nonempty shapes, repeated clients, UTC rollover, six UA cases, mixed bot/browser identity and unchanged v1 counts/ranking. Independently checked examples: 3+2+1 visits = six clicks/three uniques; six distinct UA peers = six uniques/three bots; one peer with two classes = one unique/one bot click. |
| Privacy and identity | AC-7–9 cover the right-most untrusted forwarded client, default header rejection for identity, stored-value canaries and aggregate-only output. The four proxy requests yield three identities. BR-5 prohibits cross-day use/exposure and retained salt. Existing proxy hashes cannot be separated retroactively; restart inflation is disclosed. These are address-based distinct counts as defined, not a promise to identify individual people. |
| NFR-P2 | Q4 A expressly retains the separate mission-02 purge. BR-7 defines statistics over remaining rows. No second purge or retention change is implied; the existing operator setting belongs to mission 02. |
| NFR-O3 | AC-10/11 define recorded/lost deltas, static reason tags and scrapeable counters without client values. BR-10 makes loss mean the existing loss reports, including their outcome-unknown semantics, rather than an assertion that no late insert can occur. |
| Regression, error and logging contracts | AC-12/14/15 retain v1 behavior, correlation, canaries, slow/failing recording and concurrent redirects. BR-1 preserves existing error/method semantics. The authorized addition of per-day fields explicitly supersedes only old exact-shape assertions; the impact analysis must identify those tests. |
| NFR-L1 | Redirect identity changes activate the existing numeric target. AC-15 checks the structural non-blocking behavior; the separate release benchmark must establish p95/p99. A retained GAPS entry is disclosure, not a claim that a numerical target passed. |
| NFR-M1/M2/M3 and proof | All 12 proof items read: decision chronology, named tests, merged and separate coverage, traceability for 15 ACs/11 rules, GAPS, live statistics and metrics, committed/live OpenAPI equality, impact analysis, privacy review and release load. ADRs before dependent code are named in Non-functional. |
| Scope and ambiguity | All six product decisions and nine remaining rows read. API-only/UTC/additive figures stay within the chosen outcome; no export, UI, long-lived identity or aggregate-retention build added. A-9 is an explicit plan-lock custody obligation, not an unanswered product question. Existing table/ADR references identify brownfield inputs rather than mandate a new implementation. |

## Handoff obligations and self-check

The lead must settle `click/`, migration numbering and OpenAPI custody against
mission 02 before implementation; design requests a `web/` grant only if its
chosen identity-sharing mechanism needs it. The impact analysis precedes design
and identifies every changed exact-shape test. Do not infer that a forwarded
client identity is also the connection peer used by the separate audit access
rule: both contracts must survive the chosen mechanism.

The live three-peer capture is representative (four clicks, three uniques,
one bot if the repeated peer is the browser); the mandatory controlled tests
remain the complete AC-1/2/4 examples. Privacy reviews must inspect hash use and
salt lifetime directly, as the proof contract requires. No new finding or gate
is attached to these already-declared obligations.

No product or producer document edited, no runtime acceptance claimed, and no
project build repeated for this requirements-only change. Ledger row and
authored handoff record this independent verdict.
