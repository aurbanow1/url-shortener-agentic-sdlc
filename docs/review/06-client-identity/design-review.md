# Design review — 06-client-identity

**Final verdict: PASS on `24b80bedb672489482220212eba37295a9ba7fbd`.** One MEDIUM
characterization-plan finding was corrected in passing. No open finding; hand off to the
delegated plan-lock. This approves the design, not an implementation or its eventual QA evidence.

- Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-04; designer: `design2-agent`
  (Claude, verified commit trailer). Independent seat and runtime.
- Assigned packet: `qitem-20261004011019-f67b1c6d`, instance `01M425CY9Z4K03G14Y677AT0PA`.
- Initial design candidate: `9f6508aa89757fa13e722c3358eeccc49e95cf33`, including impact analysis
  `6772b68` and design/ADR/diagram changes `57cb9ae`. SPEC remains exact `4f247f4`.
- Context selection: project → mission 02 → slice 06 manifest; workflow guidance reports no
  selected SDLC composition. The explicit design-review contract applies, with review-team's
  ordinary review discipline and verification-before-completion.

## Context proof

D21 asks for one maintained home for existing client-identity decisions, preserving all observable
behaviour. The resolved visitor identifies the rate bucket and click hash; audit admission asks
about the connection's own peer and forwarding safety. These must remain separate questions even
though their code moves to one class. Confidence: high in this scope and in the extraction's
reachability; implementation, completed characterization additions and before/after QA captures
remain unperformed at this boundary.

Read the SPEC's 15 ACs, manifest/territory, impact analysis, full design and diagram, amended
ADRs 0015/0019, system-design changes, register row 1 and its consistency judgment, and the actual
three consumers and relevant tests. Reviewed `240b230`'s two characterization files as evidence
for the design's retained rows; this is not an early code approval of that branch.

## File ledger

Six distinct authored design files reviewed. The re-review changes only the first. Hashes at both
candidates, exact SPEC equality and current-file equality are in `design-verification.json`.

| File | Verdict |
|---|---|
| `missions/02-brownfield/slices/06-client-identity/design.md` | PASS after DR-01 correction; smallest static extraction, two distinct questions, all three consumers, characterization first |
| `missions/02-brownfield/slices/06-client-identity/impact-analysis.md` | PASS; predates design, names consumers, test adaptations, unchanged endpoints/data, CR-01 and non-web startup risks |
| `docs/DESIGN.md` | PASS; planned component and ADR amendments explicitly labelled designed/proposed, not falsely presented as merged |
| `docs/adr/0015-client-identity-trusted-proxies.md` | PASS; existing parser and attribute transfer move together; no request wrapper or second resolver |
| `docs/adr/0019-audit-read-loopback-keyset.md` | PASS; all configuration checks and direct-peer conditions preserved; trusted list cannot grant audit access |
| `docs/diagrams/client-identity-sequence.mmd` | PASS; matches inline sequence and actual call order |

Additional input: register-owner judgment `6fcb134`, explicitly rechecked on final original design
in `a6dc733`, is consistent with source. Its author discloses authorship of the earlier designs;
this review independently checks the separation. The correction changes test planning only.

## Reachability and contracts

| SPEC / concern | Mechanism and proof plan judged |
|---|---|
| AC-1, AC-2 | Redirect/link code unchanged; existing success, stored-target, cache, validation, 404/410/405 and no-click journeys retained; QA baseline-origin fixtures |
| AC-3, AC-4 | Same exact-text parser; `resolve` remains after exemptions and before charging. Explicit 60/600 and 2/2 settings, isolated peers, frozen clock, all 12 budget rows and Retry-After oracles |
| AC-5 | Same String attribute/fallback passed to the unchanged daily salt. Twelve grouping rows, fixed day, complete sole-bucket oracle and raw-value checks in response and click rows, after DR-01 |
| AC-6–AC-8 | `fromLoopback` moved verbatim, no trusted-list parameter; all named peers and header-presence values on both sides of trust after DR-01; no rewrite of `getRemoteAddr()` |
| AC-9 | Constructor still computes the configuration guard once, using NONE plus both `hasText` negations; servlet condition retained. Existing cloud/remoteip journeys plus new combined/whitespace real-server cases |
| AC-10, AC-11 | Rate limiter precedes handler; guard precedes parameter parsing/negotiation; unchanged 429, 403, 400, 500 and MVC 405 ProblemDetail paths. No new error representation or exception handler |
| AC-12, AC-13 | New utility logs/registers/stores nothing; hash, salt, retention, bounded writer, loss events/counters and HEAD logic unchanged. Existing privacy/resilience suites remain byte-identical; QA checks every named canary surface |
| AC-14, AC-15 | No response/log/API change; exact before/after response/log captures with SPEC rule-6 substitutions and unchanged OpenAPI remain mandatory QA deliverables |
| Brownfield / chronology | Impact `6772b68` before design `57cb9ae`; amended ADRs before dependent production code. Commit 1b completes tests on baseline; commit 2 moves production with delegates and untouched tests; commit 3 only adapts references and removes delegates |
| Structure / territory | One public final static utility, no state/bean/dependency; permitted three consumers only. Package-private filter restored when its external constant reader moves. Existing unit behavioural assertions and all functional files remain unchanged |
| Data / rollback | No schema, query, stored-data rewrite or migration; database checklist's new-DDL obligations are inapplicable. Existing V1–V4 remain unchanged; no new rollback SQL is needed |

Threat model covers spoofed visitor identity, audit elevation through conflation or a lost CR-01
condition, mismatched attribute keys, privacy/grouping drift and non-web startup. The temporary
constant delegates to `ClientIdentity.CLIENT_ATTRIBUTE`, so the renamed internal key has one value
through the transition. The existing `instanceof String` fallback is preserved, including non-String
attributes. No speculative layer or additional public HTTP surface is introduced.

## Findings on 9f6508a

| Id | Severity | File:line | Evidence | Required change |
|---|---|---|---|---|
| DR-01 | MEDIUM | `missions/02-brownfield/slices/06-client-identity/design.md:223` (also 226, 262) | The purported complete characterization plan retains `240b230`'s trusted audit cases, which only use trusted P and a nonempty XFF from trusted loopback. `AuditAccessSettingsJourneyTest` adds only 192.0.2.10 GET, not the four AC-7 peers under trust with GET/HEAD or AC-8's blank/whitespace/both-header trust axis. The retained AC-5 contexts have running clocks and no sole dated-bucket assertion; M1 checks only clicks/uniques. Verified by full source reads at 240b230 and baseline; source hash recorded in `design-verification.json`. | Explicitly add the missing trusted-peer/header combinations and isolate/freeze AC-5 on a fixed UTC day with the complete aggregate oracle, in commit 1b before the move. |

This is a test-plan completeness defect, not evidence that the proposed product path fails an AC.
All outcomes remain reachable through the unchanged predicates; severity is MEDIUM and does not
fail the packet. Initial verdict: PASS with DR-01 expected to be corrected in passing.

## Re-review 24b80bedb672489482220212eba37295a9ba7fbd

**DR-01: fixed.** Read the complete one-file correction and author's response. `TrustedAuditPeers`
now lists all six loopback forms and four non-loopback peers in exact text, with admission,
GET/HEAD refusal and both forwarding headers including empty/whitespace under trust. The untrusted
cases remain. M1 moves into an isolated `ShippedBudgets` context; it and `TrustedProxies` freeze
at a named UTC noon, step forward between cases, and close their contexts. Every grouping case
asserts total, exactly one dated bucket, clicks, uniques and bots. `FunctionalClock` supports the
specified freeze/shift; `DailySalt.select` calculates real expiry delay from that application-clock
noon, avoiding a near-midnight expiry during a case. The correction remains in the new test file
and leaves existing assertions intact. No new finding introduced. Final verdict: PASS, no open items.

## Independent verification

Ran on root checkout `10f1ed8` while reviewing; its product/build bytes equal the design candidate
and remain identical at `24b80be`. Relative to SPEC baseline `5cfdf8a`, the only source delta is the
separately reviewed W2F-01 fix in `ClickRetentionScheduleJourneyTest`; it is not this refactor.

- `scripts/gw --log docs/review/06-client-identity/design-baseline-check.txt --offline check --rerun-tasks`
  — success; fresh **226 unit + 250 functional**, zero failures/errors/skips; Javadoc passed.
- Restricted coverage to fresh `test.exec` and `functionalTest.exec` using the already committed
  `docs/review/03-ambiguous-analytics/proof/canonical-coverage.gradle`, then ran `jacocoAllReport`
  and `jacocoTestCoverageVerification` with `-x test -x functionalTest`. Success: **582/582 lines,
  206/206 branches**. Log: `design-canonical-coverage.txt`; retained CSV: `design-canonical-coverage.csv`.
- `scripts/gw --log docs/review/06-client-identity/design-controls.txt --offline -I docs/review/06-client-identity/design-controls.gradle identityDesignControls`
  — **72 independent baseline controls pass**: 19 parser rows, 13 peer forms, 16 header-presence
  cases, 24 strategy/header-setting combinations. Source: `DesignBaselineProbe.java`. These invoke
  the actual baseline predicates/configuration field; they are not claimed as new HTTP captures or
  as tests of the future class. They add no JaCoCo data to the product gate.
- `design-verification.json` records six-file candidate hashes, exact SPEC equality, product diff,
  XML totals/hashes, canonical coverage inputs and log hashes. No product, test, SPEC or design
  file was edited by this reviewer.

## Residual risks and self-check

Known boundaries remain: a headerless local relay is indistinguishable from an Operator; a Boot
upgrade adding another rewrite trigger needs an explicit guard review; a future redirect path that
skips the limiter must still resolve identity. None is introduced or widened here. Implementation
must still supply the complete characterization matrix before moving code, exact candidate QA
captures, unchanged-suite comparison and final structural/security review. No completed candidate
proof is inferred from this baseline gate.

Exact candidate and correction verified; all six design files covered; finding supported by named
source and resolved with a recorded re-review; gates and controls independently run; ledger has
initial and resolution rows. Only `docs/review/` artifacts changed. Handoff to plan-lock, no blocker.
