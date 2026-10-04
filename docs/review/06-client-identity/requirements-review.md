# 06-client-identity — requirements review

**PASS — no findings.** Candidate `4f247f4da7868d3b124008f1c0571946dfa43c29`, packet `qitem-20261004004915-91f40366`; reviewer `review2-agent@urlshort-factory` (Codex), 2026-10-04 UTC.

**Independence:** author and reviewer are separate seats, both on Codex. The author confirmed the correct Codex commit trailer; `PLAN.md:298` (D17) and mission `NOTES.md:71` record the human-authorized switch and its reduced review independence. This is not a cross-runtime review. The lead's D17 continuation still requires its own SPEC/decision check at delegated plan-lock; no additional approval is requested here.

## Context and coverage

D21 requires one owner for the existing client rules, preserving all behavior: the rate-budget/analytics visitor is resolved from explicitly trusted forwarding, while audit access remains a separate direct-connection decision with the original CR-01 protections. Confidence is high after reading the complete candidate, mission amendment, manifest, D21 packet, requirements allocation and relevant baseline source/tests. The named baseline is `5cfdf8a`; product/build/API have no delta from that baseline to the requirements candidate.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/06-client-identity/SPEC.md` | PASS — complete file read; all 15 ACs and 17 proof items checked. Current bytes equal the exact handed-off Git blob. |

[Verification record](requirements-verification-4f247f4.json) saves the candidate/baseline, SPEC hash, AC/proof inventory and nine checked baseline source/API identities. This is an artifact review; no implementation or runtime-equivalence result is claimed.

## Requirements judgment

| Check | Result |
|---|---|
| D21 and allocated IDs | PASS. FR-13 covers preserved consumers; inherited FR-10/NFR-R2, FR-7/8, FR-17/S6, P1, O1/O2, R6 and M3 each map to concrete ACs. M1/M2 have explicit coverage/ADR artifact obligations. X2 is inapplicable because schema changes are forbidden. Primary requirement ownership is unchanged. |
| Observable ACs | PASS. Every AC has GIVEN/WHEN/THEN and an HTTP, log or stored-effect oracle. The identity matrix proves shared budgets and unique counts instead of exposing a diagnostic identity field. Frozen time and isolated setup peers avoid contaminating the budget oracle; two tokens/minute implies Retry-After 30 on immediate refusal in the existing limiter. |
| Error and privacy edges | PASS. 400/403/404/405/410/429 and injected audit 500 are explicit; real HEAD is bodyless. Non-loopback and forwarding-header cases, unsafe native/framework/Tomcat overrides, cloud detection, precedence and private canaries remain mandatory. Existing full journeys retain other inherited errors. |
| Existing rules preserved | PASS. Matrix agrees with the existing rightmost-untrusted parser, including blank/absent/all-trusted cases. Exact-text trust and opaque non-empty tokens are preserved. Audit continues to inspect the connection peer and header presence; trusting that peer never admits forwarded audit access. Missing/unparseable peers and unset effective strategy are explicitly characterized. |
| Scope / design separation | PASS. No new API, setting, dependency, migration, validation or data rewrite. D21 itself names the component move; the SPEC references that human-authorized manifest instruction without inventing a new implementation/API. Existing test/setting names identify evidence and baseline behavior. Impact analysis and amended ADR-0015/0019 precede dependent code. |
| Ambiguities | PASS. A-1/2/3/5/6 cite safe preservation decisions. A-4's documented run-specific substitutions preserve relationships and prohibit hiding changed statuses, headers, fields, access, grouping or event content. It makes independent-run comparisons possible without loosening the contract. No parked product decision. |
| Proof contract | PASS. All 15 ACs, matrix/settings cases, live before/after captures, real-server CR-01 behavior, characterization-before-move chronology, separate production commits, unchanged functional bytes, 100% merged coverage with per-suite reports, traceability and honest gaps are named. Original assertions cannot be weakened under an API-reference adaptation. |

## Findings

None. No non-blocking repair or new backlog is requested.

The W2F-01 scheduling-test correction is explicitly unrelated to this refactor (A-5 and manifest). A later rebase must record that upstream change separately; it must not be presented as a functional-test rewrite by this slice. Design must still demonstrate the single authority and preservation of the two distinct identity questions. QA must produce the promised live comparison; this requirements PASS does not substitute for it.

Self-check: exact candidate verified, 1/1 changed files reviewed; D17/D21 and allocation checked; every AC's observable result and boundary cases assessed against current baseline behavior; all ambiguity/proof rows read; no product/test/SPEC/design edit. No new gate run was needed for the documentation-only candidate: the preceding own wave gate exercised the same product, and future refactor validation remains expressly unperformed. Record the ledger row and hand off to design.
