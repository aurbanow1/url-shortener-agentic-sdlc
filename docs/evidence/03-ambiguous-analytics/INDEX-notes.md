# Final evidence notes — 03-ambiguous-analytics

Read [INDEX.md](INDEX.md) for the generated governance mapping and complete
packet/step tables. The final export follows the human's local-use decision
on product **50ad9c3** and independent pre-export QA reaffirmation.
It captures the mission at evidence_export, before the author's handoff and
mission_close; those future closures are not claimed by this snapshot.
The exporter also captures other missions and drills. Directory placement is
not mission ownership. [Validation](final-validation.json) checks every JSON
record, required instance/packet presence, decisions and receipt hashes.

| Governance clause | Mission-specific evidence |
|---|---|
| Explicit dependency graph; dynamic re-planning | [Compiled graph](compiled-graph.json); lifecycle [show](instances/01M40RVNDQ0KT7FPWN1KJW0DC3.show.json) / [trace](instances/01M40RVNDQ0KT7FPWN1KJW0DC3.trace.json) |
| Sequential paths; bounded retries | Analytics [slice trace](instances/01M416Z3CM54YQTX93V4KG0CPS.trace.json) and [show](instances/01M416Z3CM54YQTX93V4KG0CPS.show.json); no failed closure or retry in this slice; waits retain their trail entries |
| Decision lineage; human checkpoints | [Mission plan-lock](packets/qitem-20261003114944-9bd32a00.transitions.json), [six-choice ambiguity decision](packets/qitem-20261003154347-19e96a75.transitions.json), [human local-use approval1916](packets/qitem-20261004023723-f58044d0.transitions.json); stamped mission/slice SPECs linked from [RELEASE](../../../missions/03-ambiguous-analytics/RELEASE.md) |
| Independent review; controlled autonomy | [Final release review93d55bd](../../review/03-ambiguous-analytics/release-review.md) on package14815f9/product50ad9c3; slice code/security reviews and both wave vantages linked from RELEASE §5 |
| Audit-grade observability; traceability | [Final attributed proof](proof-readiness.json), ready12/12; QA [reaffirmation audit](../../../missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-final-reaffirmation-50ad9c3/audit.json) and [stamp audit](../../../missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-final-reaffirmation-50ad9c3/metadata-audit.json); [QA return transitions](packets/qitem-20261004024232-5aea9029.transitions.json) |
| Safe-stop; fallback | [Export packet](packets/qitem-20261004024238-651b51b1.show.json) and [transitions](packets/qitem-20261004024238-651b51b1.transitions.json) retain the route to release2 and wait on QA; rig-wide drill traces retain their own attribution |
| Rollback | [Branch-only revert rehearsal](../../scenarios/drills.md#mission-03-release-rollback-rehearsal), [artifact teardown](../../../missions/03-ambiguous-analytics/release/teardown-50ad9c3.json); no live main/service rollback or publication |
| Policy guardrails | [Scope audit](scope-audit.json); the historical qa-item11-ec466da.md C1-header advisory is retained without rewriting hash-bound evidence |
| Reliability metrics | [Usage window](usage-top.json), [shared derivation and limits](../../metrics/README.md); instance IDs establish mission ownership, export-container labels do not |

The approved candidate includes W2F-01 and precedes D21; the actual analytics
merge is c9b66dd. Human transition1916 accepts local use and the unverified
exact-SHA hosted-CI gap. It does not establish a hosted run for50ad9c3.
Mission/slice delivery stamps were recorded by the primary release seat/lead,
not by this export's author. Both wave LOWs and measurement/operational limits
remain in RELEASE; no agent pushed, release-tagged, published or exposed a
service beyond localhost.

## Snapshot history and final reaffirmation

The [original preparation proof](../../../missions/03-ambiguous-analytics/release/proof-readiness-50ad9c3.json)
has item12 pending. Its [complete GAPS copy](../../../missions/03-ambiguous-analytics/release/GAPS-snapshot.md),
manifest and verification remain unchanged. QA receipts13/14 supported the
[reviewed ready snapshot](../../../missions/03-ambiguous-analytics/release/proof-readiness-after-qa-50ad9c3.json)
and [post-QA GAPS copy](../../../missions/03-ambiguous-analytics/release/GAPS-after-qa-50ad9c3.md).
QA's fallback-model797f8fb attempt was re-derived on its restored seat before
those receipts. Independent release review separately re-derived8cf894a's
840 hashes,405 wire/log joins and38 statistics bodies from the recorded
01:27–01:33Z model window.

Later shared-document appends and slice delivery metadata caused the retained
[drift capture](../../../missions/03-ambiguous-analytics/release/proof-readiness-shared-doc-drift-50ad9c3.json).
Lead transition1917 kept the reviewed immutable snapshot for the human and
scheduled one reaffirmation before export. QA receipts15/16/17/18 independently
reaffirm items2/5/6/1 against50ad9c3; analytics text and SPEC body were unchanged.
The exported proof reports ready12/12 with those committed receipts, while
the earlier snapshots keep their original state. The QA return closes before
this export; it is not another product gate or D21 product dependency.

The exporter ran 02:57:12Z–02:58:48Z. It selects packets named in workflow
traces, so the standalone QA return was added with individual raw queue
show/transitions calls at 03:04Z. Final validation parses 427 raw JSON records,
checks 45 committed references across 35 files and preserves seven original
preparation/review records. It also checks the human decision and both stamps.
The [final full GAPS copy](../../../missions/03-ambiguous-analytics/release/GAPS-final-export-50ad9c3.md)
matches receipt17; later D21 sections remain shared context outside this product.

The disk compile digest is
`5d5530566e58e2a2e1c0aa2a9bce3e8d4d4a3045d9777a21750c70950861d8a6`;
the running binding retains
`67227ca5beeca2c2b1e2adbd3948505d8bcfb61e326fa82ef95065f9b93916ea`.
These distinct digests were already present in preparation. Their dependency
edges match. The disk compile omits an opaque instantiation operation key;
it is not a new binding or evidence of a failed running lifecycle.

## Self-check

Required records and all raw JSON parse; the final proof is ready12/12 and
all45 references match committed QA0e125ca7. Human transition1916 is quoted
verbatim, stamps are present, and historical records are unchanged. INDEX
tables are generated by tools/evidence-index.mjs, including the supplemental
QA packet. The export precedes its own closure and mission_close. The metadata
advisory, exact-hosted-CI gap and shared-host measurement limits remain visible.
No product artifact was rebuilt, pushed, tagged, published or exposed beyond
localhost during this export.

The initial metrics snapshot8d3c536 remains frozen in the release package.
A coordinated shared-metrics refresh uses this final export; its generation
time and limits are recorded separately from that preparation snapshot.
See [GOVERNANCE](../../GOVERNANCE.md) for the exact clause definitions.
