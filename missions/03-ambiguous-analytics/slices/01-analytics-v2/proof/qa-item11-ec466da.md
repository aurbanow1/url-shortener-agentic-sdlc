# QA returned judgments — analytics ec466da

Packet qitem-20261004001915-1efe66cd; QA qa-agent@urlshort-factory (Codex), 2026-10-04 UTC. Subject remains the independently tested candidate ec466da8da4b1efde9d612c6c8692070cc6fc4b9. This return addresses proof item11 and the requested item6 evidence drift; it does not claim to check the subsequently rebased candidate or repeat the runtime gate.

## Item 11 — accept

Read the complete design review committed at97d832c and security review committed at61430eb, and verified each current file is byte-identical to its committed record. The design review has an explicit proof-item11 construction row: the only proposed client-hash read is a link-bound distinct count grouped by UTC day, with no exposed hash, join or cross-day comparison; the unchanged salt is memory-only and dropped at day end and close. The exact-candidate security review supplies both required rule5 rows: same-day-only hash use, and salt location/lifecycle/non-persistence.

The security record independently checks the product-wide hash references, the sole ClickStore distinct-count statement, synchronized instant/key selection, scheduled quiet expiry, rotation and close. It records all seven salt tests passing in its fresh221/241 gate and reconciles QA's previous two-day hashes and no-disclosure captures. It explicitly limits its claim: transient in-flight SecretKeySpec copies can complete after rotation; no JVM-wide memory erasure or hard timer deadline is claimed. This satisfies the contract's review-record obligation without enlarging the privacy promise.

QA also read the immutable ec466da ClickStore Git blob and searched that exact Git tree's product references. The sole analytical client_hash read is COUNT(DISTINCT client_hash) grouped by clicked_on, with no hash response or salt persistence binding. No new runtime observation is inferred from this source check; the accepted prior QA captures remain unchanged.

## Item 6 — reaffirm accept

Current docs/qa/GAPS.md SHA-256: 18149e871a3225f68323117da91202d2186662b6667642e3815068ae12974068. Compared the complete analytics section against f99457c: byte-identical. The full-file diff changes only mission02's four old audit-column rows to their merged closure status, including the corrected QA seat attribution. The analytics section still has the required pending NFR-L1 benchmark, complementary suite percentages and honest fixture/attribution limits. Its security-pending line records the original qa_check stage; the new item11 receipt resolves that later obligation. No GAPS edit is needed for this reaffirmation.

Item12 remains pending release_prep under the lead's a12a0e2 sequencing and qitem-20261003195138-8eb72ecb. The original QA candidate ec466da is the subject of these judgments. Integration's separately authorized rebase/range-diff/fresh gate and any merged-candidate acceptance belong to their later packets.

## Self-check

Read both independent review records and their explicit contract rows; checked committed-file equality and exact-candidate source references; checked current GAPS hash and byte-equal analytics section; preserved both records' observation limits. No product, tests, review record, GAPS or earlier cited PROOF evidence changed. Record attributed judgments for11 and6, verify their evidence hashes/readiness, commit only this receipt and the generated judgments, and close the returned queue item.
