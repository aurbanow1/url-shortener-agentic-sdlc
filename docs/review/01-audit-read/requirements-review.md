# 01-audit-read — requirements review

**Latest verdict: PASS at `7b753b7`. RQ-01–04 fixed.** Original verdict retained below.

Candidate: `411a50cc35fb3b889346888bfa7a5e8f529f5470`.
Packet: `qitem-20261003154828-764bc65c`; instance: `01M416ZY5N11CDGZBM2DT4GAXS`.
Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC.

**Verdict: FAIL — three HIGH findings and one MEDIUM.** Return to requirements.
Fix the MEDIUM wording in passing with the access-rule correction; no new access-control feature is requested.

## Context and complete coverage

The outcome is a bounded, read-only operator view of existing mutation records,
with private-by-default access and unchanged links, writes and statistics. Read
the complete 323-line SPEC and its one-file candidate change; mission decision
brief, allocation and slice manifest; NOTES/PROGRESS/PROOF; REQUIREMENTS;
requirements §6, review and brownfield §7 guides; inherited audit/error rules,
ADR-0008 and the current writer, transaction caller and V1 schema. The baseline
is `f6dd29e` (application `8e9c065`). Scope and contract confidence: high.
This is a requirements review; no claim that the unbuilt endpoint passed or failed
a runtime test. The independent database probe tests the existing storage premise.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/01-audit-read/SPEC.md` | Read in full; FAIL RQ-01–03, MEDIUM RQ-04 |

**1 changed file / 1 reviewed.** No product, tests, SPEC or design edited.

| Area | Assessment |
|---|---|
| FR-17 | AC-1–10 cover empty/create/retire rows, exact stored values, bounded pages, malformed parameters and read-only behavior. The ordering/concurrency contract has RQ-01; the failed read has RQ-03. |
| NFR-S6 | AC-11–14 cover remote refusal, loopback forms and forwarding headers. RQ-02 removes an unsupported widening; RQ-04 qualifies the proxy claim. |
| FR-13 / NFR-X2 | AC-17/18 require unchanged baseline tests and a real old-data upgrade. Impact analysis before design, additive-only migration and written rollback are explicit. Conditional migration is an obligation, not a prescribed new schema. |
| NFR-A2 / O1 / O2 | AC-10 preserves audit rows and refuses writes; AC-15/16 cover response correlation and canary hygiene. Extend these to the failed-query path under RQ-03. |
| NFR-M1 / M2 / M3 | Coverage, ADR timing and candidate live/committed OpenAPI equality are named. AC-19 covers the new operation and preservation of existing operations. Artifact requirements correctly use proof items rather than artificial HTTP assertions. |
| AC form and business rules | All 19 ACs are GIVEN/WHEN/THEN and externally observable through HTTP, logs, persisted rows or the upgrade journey. All nine rules read, including inherited 429 behavior. No implementation classes or dependency choices imposed. |
| Scope, ambiguity, evidence | Explicit exclusions and all 13 ambiguity rows read. RQ-01/02 challenge two claimed safe resolutions. All 12 proof items cover per-suite reports, merged coverage, traceability, gaps, upgrade and HTTP/log captures, rollback, OpenAPI, ADRs and security review. |

## Findings

`SPEC.md` references below denote this slice's candidate SPEC.

| Id | Severity | File:line | Evidence and consequence | Required change |
|---|---|---|---|---|
| RQ-01 | HIGH | `SPEC.md:258`, `:206`, `:207`, `:239` | A-10 explicitly promises commit order, BR-4 promises later writes always first, and BR-5 promises no skipped rows regardless of concurrent writes, while the write side is excluded. The shipped identity and application timestamp are assigned before transaction commit (`AuditLog.java:58–67`, `LinkService` transactional callers). The independent two-connection probe appends A then B, commits B then A, and observes ids `2:B, 1:A`; both timestamps are equal. Neither field records commit order. AC-4/8 use sequential completed writes and cannot prove the stronger promise. Historical commit order cannot be recovered by a read-only change over these rows. | Define a stable order the existing records actually support, and explicitly bound pagination guarantees for rows committed/visible at each read versus in-flight writes. Add an observable concurrency criterion for that boundary. Keep cursor encoding a design choice. If true commit order is essential, route the write-side scope change for a decision rather than asserting it is already available. |
| RQ-02 | HIGH | `SPEC.md:165`, `:204`, `:254`; `missions/02-brownfield/SPEC.md:94` | AC-14 and A-6 add a switch exposing the anonymous trail to remote clients. The human-approved mission excludes access beyond loopback. At 16:31Z the author confirmed no explicit opening decision exists; the setting was inferred from the baseline's “by default” and proof-column wording. That resolves a conflict by widening scope and exposes stored targets/request ids if enabled. | Follow the narrower mission scope: remove remote opening from ACs, rules, assumptions and proof wording, or obtain an explicit scoped decision before design. The author accepts removal as the narrow fix; no new human question is needed to retain the approved boundary. |
| RQ-03 | HIGH | `SPEC.md:174`, `:229`, `:272`; `docs/REQUIREMENTS.md:77` | The SPEC enumerates 200/400/403/405 and tests successful row reads; none of AC-1–19 injects a failed audit query. A query that throws, or one incorrectly converted to an empty 200 page, is unconstrained by those examples. Generic inherited ProblemDetail prose is not the explicit failure-path AC required by requirements §6 and baseline NFR-R6. | Add a failed-read GIVEN/WHEN/THEN: safe 500 ProblemDetail, no fabricated success/partial trail, no SQL/class/stack/input leakage, and correlated safe logs. Name its functional proof and extend the coverage/traceability mapping. This adds proof of the inherited contract, not a new product capability. |
| RQ-04 | MEDIUM | `SPEC.md:204`, `:253`, `:283` | The text assumes every local reverse proxy supplies a forwarding header, and says refusing those headers covers remote clients behind local proxies. A local proxy can instead send an ordinary headerless request from `127.0.0.1`; AC-12 requires that request to pass. That peer is indistinguishable from a local operator on the specified inputs. This is a logical counterexample to the universal security claim, not a reproduced deployed proxy defect. | State the deployment trust boundary: local forwarding of this endpoint must be excluded, or must carry the refused header. Qualify the security proof to the actual peer/header inputs and record the limitation. Do not add authentication or proxy machinery to this slice. |

The RQ-01 reproduction is [AuditCommitOrderProbe.java](proof/AuditCommitOrderProbe.java)
and its [exit-0 output](proof/commit-order.txt), run with shipped H2 2.4.240 and
the unchanged V1 schema in memory. It demonstrates why an identity key cannot
be described as commit order; it does not dictate the pagination design.

## Self-check and continuation

All changed content and allocated obligations reviewed. Findings cite source
or precise documentary counterexamples; no missing build was turned into a
product failure. Product code is unchanged, so no additional project gate was
needed for this SPEC-only review. Return through the authored `failed` exit.
On re-review, check each producer response and changed contract/proof mapping;
retain settled requirements and avoid unrelated new polish findings.

## Re-review 7b753b7

Packet `qitem-20261003163921-0a8e2926`, same instance, 2026-10-03 UTC.
Read the complete one-file correction and each response; independently verified
the file equals the candidate and contains AC-1–21. No product changes.

| Finding | Resolution | Re-review evidence |
|---|---|---|
| RQ-01 HIGH | Fixed | BR-4/A-10 now distinguish existing write sequence from unrecorded commit order. BR-5 guarantees exactly once for rows committed before the first page, at most once for in-flight rows, with a fresh traversal needed after late commits. AC-20 holds a write open and checks traversal/re-read; proof and traceability include it. The old H2 counterexample is acknowledged without adding a write-side feature. |
| RQ-02 HIGH | Fixed | The opening switch is removed from the contract, exclusions and ambiguity resolution. AC-14 requires refusal with nondefault operator/proxy settings; A-6 follows the narrower mission decision. |
| RQ-03 HIGH | Fixed | AC-21 injects a failing read: safe 500, no partial/empty-success representation, correlated safe logs, then successful recovery. NFR-R6, BR-6, OpenAPI AC-19 and proof/test mapping include it. |
| RQ-04 MEDIUM | Fixed | BR-2/A-5 explicitly disclose headerless local relays; security proof is qualified to observed peer/header inputs, and an operator-documentation proof item records the deployment boundary. |

**Verdict: PASS, no open findings.** Handoff to design. Prior unaffected
requirements remain settled. Design still owes the brownfield analysis,
actual peer-address handling, bounded pagination implementation and controlled
test mechanisms; this review does not claim they have been implemented.
