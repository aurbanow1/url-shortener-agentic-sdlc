# 01-create-redirect — requirements review

**Latest verdict: PASS at `0acbc9d4898a04df73cf61bab20570a75484629a`.**
RQ-01, RQ-02 and RQ-03 are fixed; earlier findings are retained below as history.

Candidate: `d1794980a8169ef099dc6420cd2ae6f45c47d463`, requirements range
`6b6b2d2^..d179498`. Reviewer: `review-agent@urlshort-factory` (Codex),
2026-10-03; author: requirements agent (Claude).
Packet `qitem-20261003050304-b7cdaab5`, instance `01M40149S2BGCAKVK83B6VCNKG`.

**Verdict: FAIL — RQ-01 HIGH.** Preserve an existing idempotency binding after
a rejected request. RQ-02 and RQ-03 are MEDIUM and do not independently block;
please tighten them in passing while answering this review. No product defect
is claimed: these are requirements findings before design/implementation.

## Context proof

This foundation slice gives Creators create/read/retire and idempotent retry,
Visitors stored-target redirects, and Operators transactional audit, private
structured logs and a committed API contract. Analytics, rate limiting and the
audit read endpoint belong to later slices. Confidence: scope/allocation 99/100,
business rules and proof completeness 97/100. The intended failed-request
idempotency behavior is the blocking ambiguity, not a need for new scope.

I checked the mission allocation, slice manifest, REQUIREMENTS baseline,
requirements guide §6, prior design conventions and human plan-lock transition
266 of `qitem-20261003042553-03ac8b4b`. The eight baseline assumptions are
approved; the dependency overrides remain the first independently gated commit.
The SPEC carries that constraint in its proof contract.

## Complete file and contract coverage

| Changed file | Verdict |
|---|---|
| `missions/01-greenfield-core/slices/01-create-redirect/SPEC.md` | Read in full; RQ-01 HIGH, RQ-02/RQ-03 MEDIUM |

1 changed file / 1 reviewed. Shared main advanced during review to
`d67f6a6e4e78c153fecf148e07b79183c046fe4f`; `git diff --name-only d179498 HEAD`
showed only mission NOTES changed, so the reviewed SPEC remained the exact
candidate. The initial working tree was clean.

| Area | Result |
|---|---|
| Allocation | All 19 ids present: FR-1–6 and FR-9; S1/S3/S4/R5/R6/A1/A2/M3/O1/O2; cross-cutting M1/M2. Artifact-only M1/M2 are honestly specified in non-functional/proof obligations. A2 and S4 pair public behavior with code/artifact checks. |
| AC-1–7: create, validation, limits | Observable HTTP criteria with explicit input tables, boundary sizes, public-base independence and no validation-value echo; RQ-02 concerns rule precedence beyond the current input table. |
| AC-8–16: read, retire, redirect, route errors | Observable statuses/fields/headers; retired state retained, no redirect Location on 410, browser Accept explicit, existing surfaces protected. |
| AC-17–21: idempotency | Sequential replay, mismatch, expiry, malformed key and a rejected first create covered. RQ-01 identifies the missing preserve-existing-binding case. |
| AC-22–25: audit | Stored side effects observable without prescribing a schema; rollback includes post-failure HTTP checks; append-only rule backed by code review and persistence-level proof. |
| AC-26–27: observability/privacy | Canary values and inbound request-id rejection specified; RQ-03 concerns the weaker per-request log quantifier and omitted error classes. |
| AC-28: API documentation | Operation/status/schema/examples observable; committed-versus-live equality separately required. |
| Business rules | All 12 read, including concurrent same-key creates, no target normalization, one-way retire, immutable audit and no forwarded-base trust. Proof contract maps business rules as well as ACs to tests. |
| Ambiguities | 23 rows: 8 assumed, 15 decided, no parked rows. Narrow defaults documented; RQ-01 qualifies A-9. No extra human question is necessary to preserve FR-9/R5. |
| Scope and design boundary | Explicit exclusions retain the approved mission split. No prescribed application class/schema/layer; inherited dependency constraints and artifact paths are authorized obligations. |
| Proof contract | 14 items: AC/rule traceability, merged 100% coverage, both suite reports, GAPS, dependency-first evidence, HTTP/log/audit captures, deterministic API diff, ADR timing, clock-controlled expiry, A2 and S4 reviews. |

## Findings

All SPEC references below point to
`missions/01-greenfield-core/slices/01-create-redirect/SPEC.md` at the candidate.

| Id | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| RQ-01 | HIGH | `SPEC.md:282`, `SPEC.md:349`; AC-18 at `SPEC.md:215` | Rule 5 and A-9 say a request refused with any 4xx or failed with any 5xx leaves the key unbound. But AC-18's 422 occurs on a key already bound by 201, and FR-9/R5 require that original result to remain replayable for 24 h. AC-18 only checks no new link/audit row; AC-21 only covers a rejected first use. The binding-preservation failure path is unspecified and conflicts with the unconditional unbound rule. | State that a failed first use creates no binding, while failure on an already-bound key preserves its existing link and original expiry. Add/extend a public criterion: create `(K,A)` → reject `(K,B)` → retry `(K,A)` still returns the original code/body, with one create audit row. Apply the distinction in A-9 as well. |
| RQ-02 | MEDIUM | `SPEC.md:279`, `SPEC.md:280` | Rule 2 assigns leading/trailing whitespace to `malformed`; ordered rule 3 assigns `scheme` first when a value does not begin with http:// or https://. For the literal value ` https://example.com/`, required and length pass, then scheme fails before malformed. The rejection itself is clear (400), but the promised stable token has two answers. | Choose one token/precedence for leading whitespace, align both rules, and add it to AC-4's table. Keep verbatim storage and rejection of whitespace unchanged. |
| RQ-03 | MEDIUM | `SPEC.md:259`, `SPEC.md:287`, `SPEC.md:296`; baseline `docs/REQUIREMENTS.md:89` | NFR-O1 requires the response id in every log event for that request. AC-26 and rule 10 only require one matching JSON event, so an additional request event lacking its id would pass. AC-26 also excludes the specified 413/415/422/500 responses. The allocation row retains the stronger contract, making this an incomplete acceptance check rather than an absent requirement. | Preserve at least one event per request, additionally require every event emitted while handling that request to carry the same id, and extend the response table to the four omitted error paths. |

## Documentary reproductions

RQ-01: at time t0 create key K with URL A and receive code C; within 24 h,
submit K with different valid URL B and receive AC-18's 422. The blanket
failure sentence says K is unbound, while the 24-hour replay promise says
a subsequent K/A must return C. A builder needs an explicit preservation rule
to reconcile those statements. Removing the binding would not create a new
link or audit row at the 422, so AC-18 alone would not catch it. This is a
contract contradiction, not an assertion that existing product code deletes keys.

RQ-02: applying the written precedence to ` https://example.com/` yields
`scheme`, while rule 2 explicitly yields `malformed`. Both return 400; the
limited consequence is why this is MEDIUM.

RQ-03: a response header `X-Request-Id: R` plus two log objects
`{"requestId":"R","message":"request complete"}` and
`{"message":"request failed"}` satisfies AC-26's at-least-one clause while
violating the baseline's every-event clause. No execution is claimed for this
synthetic counterexample; it demonstrates the acceptance predicate's gap.

## Verification and self-check

- Exact candidate and the one-file range verified; every AC, business rule,
  ambiguity row and proof item read. Counts independently extracted: 28 ACs,
  19 allocated/cross-cutting ids, 23 ambiguity rows, 14 proof items.
- `rig scope audit --mission 01-greenfield-core` reported no finding for
  01-create-redirect; six LOW scaffold advisories concern only slices 02–04.
  That structural audit does not establish semantic contract consistency.
- `scripts/gw --log docs/review/01-create-redirect/proof/requirements-check.txt
  --offline check` exited 0: BUILD SUCCESSFUL, 13 tasks up-to-date. This is the
  unchanged baseline gate, not execution of tests for the unbuilt feature.
- Every finding has exact candidate lines, an explicit counterexample and a
  bounded change. No framework feasibility assertion was needed for the
  browser Accept/no-store contracts; design must demonstrate those by effect.
- No product, SPEC or design edits. Ledger appended; only `docs/review/`
  authored. `failed` routes to requirements for a response to all findings;
  re-review stays on those responses and the changed text.

## Re-review 0acbc9d4898a04df73cf61bab20570a75484629a

2026-10-03, packet `qitem-20261003051110-97f19fd2`. **PASS; no open findings.**
The packet names this SHA, `git rev-parse HEAD` matches, and the checkout was
clean before reviewer edits. Read every change in the producer's one-file
commit `0acbc9d`, including its Review response table and self-check.

| Finding | Resolution verified in the revised SPEC |
|---|---|
| RQ-01 (HIGH) | Fixed. Rule 5 and A-9 explicitly distinguish failed first use from failure on a bound key; existing link and original expiry survive. AC-18 now checks mismatch then original replay and one create audit row. AC-19 adds a mismatch at t0+23h, then verifies the original code before 24h and a new code afterward. The NFR-R5 mapping includes both criteria. |
| RQ-02 (MEDIUM) | Fixed. Rule 2 defers to ordered rule 3; AC-4 explicitly covers a leading space as `scheme` and a trailing space as `malformed`. Required/length precedence and verbatim storage remain intact. |
| RQ-03 (MEDIUM) | Fixed. AC-26 and rule 10 require at least one event and the same request id on every event of the request; 413, 415, 422 and induced 500 are added to the response cases. The prior two-event counterexample now fails the stated criterion. |

| Changed file | Verdict |
|---|---|
| `missions/01-greenfield-core/slices/01-create-redirect/SPEC.md` | Pass — 1/1 producer files reviewed; all responses substantiated by changed contract text |

### Self-check

- Candidate and clean starting state verified. Replayed the documentary
  counterexamples against the amended rules/ACs; all three are resolved.
- Changes remain within the assigned findings; 28 ACs and the existing proof
  contract/requirement allocation are retained. No new findings or scope.
- Fresh `rig scope audit --mission 01-greenfield-core` reports no issue for
  this slice; six LOW advisories still concern only other slices' scaffolds.
- No product/test/build changes in this fix; baseline tests were not rerun.
  This pass approves the requirements for design, not implementation behavior.
- Only review artifacts edited; ledger updated. Handoff to design should
  retain the dependency-first constraint and demonstrate browser problem
  details, redirect no-store, atomic audit/idempotency and full log correlation.
