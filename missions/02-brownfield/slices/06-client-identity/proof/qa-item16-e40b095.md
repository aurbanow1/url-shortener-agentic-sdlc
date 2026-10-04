---
slice: OPR.99.0.3.6
candidate_sha: e40b09541feb0b7555c475baa82587fdd09e4890
artifact_type: qa
verdict: PASS
money_evidence: Exact e40b095 merged at b8d7fc16; independent code/security PASS
  981eb8e0; pre-move impact/ADR chronology checked; post-merge owner update
  3b2ecd0b matches final architecture/DESIGN hashes and all three consumers.
evidences:
  - "16"
self_check: Read owner changes, current authority and all consumers, both ADR
  amendments, impact record and independent review; checked merge
  parents/tag/source equality, chronology and owner update ancestry; final
  architecture/DESIGN hashes match packet. No new app or test run, no
  product/test edit; prior run evidence remains attributed.
---

# Post-merge QA — proof item 16

PASS on candidate `e40b09541feb0b7555c475baa82587fdd09e4890`, returned by
`qitem-20261004033729-f28865b7` under lead transition 1882. QA2 checked the
remaining document/chronology obligations; this is not another full journey run.

| Contract clause | Observed evidence |
|---|---|
| Impact analysis before design | `6772b68` is an ancestor of `57cb9ae`; commit times are 01:03:23Z and 01:07:54Z on 2026-10-04. |
| ADR-0015/0019 amendments before dependent code | Both amendments and design are in `57cb9ae`, before production move `7e23259` at 01:33:04Z. Their amendment bodies remain identical; current status lines record the accepted merge. |
| Independent single-authority review, all three consumers, D21 | Review2 code/security PASS `981eb8e0` explicitly verifies the separate client/peer questions. QA read those records and the merged authority/call sites: limiter `resolve` before `tryTake`, click recorder `of`, audit controller `peerIsConnection` and `fromLoopback` without trusted-list/resolved-client use. No remaining delegated methods in the three consumers. |
| Exact candidate merged | No-ff merge `b8d7fc16` has exact e40b095 as its second parent. Accepted tag targets e40b095; current main product/tests/build file equal that candidate. The integrator's captured gate is green (attributed to the integrator, not a new QA run). |
| Register owner updates after merge | Owner's commit `3b2ecd0b` follows the merge. Architecture §11 row 1 names the one stateless authority and all three consumers, with peer access separate from visitor identity. The redirect-skip duty now calls `ClientIdentity.resolve`. The hashing/operator rows agree with current code, settings comment and README. |
| Current system description | `docs/DESIGN.md` names the merged slice and updated components/client identity/ADR index/sequence link. Its snapshot `fda42757` contains the merge, verified by ancestry and source. No lingering designed-only label for this slice. |

Final owner-file SHA-256 values match the packet:

- `docs/guidance/architecture.md`: `cc903c2fec89e3c456991d27038e9ef88be3ea5ce7421a51732cecc4fd3b2129`
- `docs/DESIGN.md`: `a1028993f33e1c469ad6a81b76f0c019e60de279d0c4444496efe53db960f53d`

Machine-readable custody: `missions/02-brownfield/slices/06-client-identity/proof/qa-item16-custody.json`.
Independent reports: `docs/review/06-client-identity/01-code-review.md`,
`02-security-review.md`. Integrator gate:
`docs/evidence/02-brownfield/integrate-06-client-identity-check-e40b095.txt`.
The earlier pending-item statements in immutable run evidence describe their
recorded time; this follow-up completes that obligation. Product acceptance
observations and coverage remain the earlier exact-candidate QA/review runs.

## Self-check

Read the full owner changes, relevant register/system-description rows, both ADR
amendments, original impact record, independent re-review and current authority/
consumer code. Checked exact merge parents/tag/source equality, chronology,
owner update ancestry and both final hashes. No new application, tests or product
edit; no new network/capacity claim. Item 16's attributed receipt is committed
before closing this ordinary follow-up packet; the lead owns final workflow closure.

## Media

![qa-item16-custody.json](qa-item16-custody.json)
