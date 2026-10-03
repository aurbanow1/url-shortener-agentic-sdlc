# Requirements review — 02-click-retention

Candidate SPEC commit: `ee7a4deaff46c4e51bdf541312fca260718b508f`.
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003155116-26bce90f`, instance
`01M4170AA9E72WW1BXEA5PX0AP`. **PASS with one MEDIUM clarification; handoff
to design.** No MUST-FIX or HIGH finding.

## Context proof and coverage

The outcome is automatic deletion of old reduced click rows, with a default
90-day configurable period, while links, audit and the retained statistics
contract survive. This is the decided mission-02 purge, not permission to build
mission-03 aggregation. W2-05 adds a distinct reduction-failure reason inside
the already touched click area. Confidence: high in the requirements boundary;
the scheduler, lock behavior and migration need the upcoming brownfield design.

Read the complete handed-off SPEC, mission SPEC and recorded decisions, slice
manifest, product requirements, relevant DESIGN contracts, mission-03 Q4 context,
the source W2-05 finding, and requirements §6 / brownfield §7 guidance. Verified
the actual human transition **831** on `qitem-20261003120551-4e8acd30`: three
slices/two waves, purge here, 90-day default as an operator setting. The current
SPEC bytes equal the handed-off Git object; no producer change was reviewed
from a moving working copy.

| Handed-off changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/02-click-retention/SPEC.md` | Fully read: 14 ACs, seven rules, eight resolved ambiguity rows, nine proof items. PASS with RQ-01 MEDIUM. |

One target file changed, one reviewed. Context references are not additional
candidate changes. Every numbered AC contains GIVEN/WHEN/THEN; an automated
inventory confirmed exactly AC-1 through AC-14 with no missing/duplicate id.

## Requirements and criterion assessment

| Requirement / criteria | Assessment |
|---|---|
| NFR-P2; AC-1, AC-2 | Exact UTC-day boundary and next-day movement specified. AC-2's predecessor state can be recreated as its fixture; it need not depend on test execution order. The boundary-day assumption is explicit, favors avoiding premature deletion, and is visible for plan-lock. |
| NFR-P2; AC-3, AC-4 | Environment-overridable period and invalid zero/negative/nonnumeric settings have observable startup/deletion outcomes. No silent fallback. |
| NFR-P2 / FR-13; AC-5 | Retained statistics specified through HTTP: field shape, daily counts, total consistency and old-only referrer removal. |
| FR-13; AC-6 | Links/audit survive and fully purged statistics are empty; clarify the observation point relative to the verification redirect, RQ-01. |
| NFR-P2; AC-7, AC-8 | Startup catch-up and autonomous daily execution are distinct, bounded observations. AC-8 requires the scheduling mechanism to run without a direct trigger; see design continuation below. |
| NFR-P2 / NFR-O2; AC-9, AC-10 | Success/zero-delete/failure logging, exception-message suppression, absent stored values and recovery on the next run are explicit. HTTP survival during failure is included. |
| FR-13; AC-11 | Concurrent redirect remains successful and its new click is eventually reflected; inherited analytics timing/fail-open rules remain applicable. Design must establish the claimed nonblocking behavior under deletion load. |
| FR-15 / NFR-O1; AC-12 | Narrow, observable W2-05 repair: 302 survives, one correlated WARN with a distinct static reduction reason, operator documentation updated. No new logging layer requested. |
| FR-13 / conditional NFR-X2; AC-13, AC-14 | Real prior data directory upgrades and retained data survives; prior functional behavior remains green. Any changed old test must have a specific impact-analysis reason. Migration rollback is an additional explicit proof obligation if an index is needed. |
| Cross-cutting NFR-M1/M2 | Engineering acceptance checks are explicit proof-contract items: full gate, 100% merged line/branch reports and ADR chronology before dependent code. These are not hidden HTTP feature requirements. |

The requirements-covered table lists every mission allocation: P2, FR-13,
conditional X2, M1/M2 and inherited O1/O2, plus authorized FR-15 work in passing.
HTTP statistics/redirects, startup outcomes and run logs provide external
observations; retained-row inspection supplements those observations to prove
actual deletion and unchanged data, rather than merely hiding old statistics.

Privacy and errors are criteria, not footnotes. Business rules define boundary,
cadence, non-overlap, affected data, audit exclusion, failure retry and log content.
All eight ambiguity rows are resolved (five assumed, three decided); no scope
expansion requires a new human park. Aggregation, on-demand endpoints, new
metrics and non-click deletion are explicitly excluded. Shipped class/schema
references identify baseline facts and test context; the SPEC does not prescribe
a new component, library or schema. Optional indexing/batching remains design's
choice under the mission's existing territory and migration rules.

The nine proof items cover candidate tests, separate/merged coverage,
traceability, honest gaps, actual startup purge row/log captures, conditional
rollback, ancestry after audit-read's merge and ADR chronology. The brownfield
impact analysis and actual running-baseline checks are due before design; their
absence at this requirements step is not a finding.

## Finding

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| RQ-01 | MEDIUM | `missions/02-brownfield/slices/02-click-retention/SPEC.md:99` | AC-6 lists a successful `GET /C` and zero-click statistics in the same outcome without fixing their order. The verification GET itself records today's click (`src/main/java/dev/urlshort/link/RedirectController.java:50`; `ClickRecordingJourneyTest.java:73` proves one successful redirect creates one row after settling). Following the listed checks in order and waiting for recording yields total 1, not 0; checking before it yields 0. This can produce a racy or misleading acceptance test. | State that empty statistics are checked immediately after purge, before the verification redirect; then allow/assert that redirect's new click after settling. Alternatively split the two observations. Preserve the existing click-counting contract. Fix in passing; no redesign or blocking loop needed. |

This is a specification ordering ambiguity supported by the shipped source and
existing regression assertion, not a newly reproduced product defect. No product
gate or live server run was needed for this documentation-only boundary.

## Design continuations and verdict

- **AC-8's proof boundary:** the self-check at SPEC line 249 honestly leaves
  scheduler-clock feasibility open. Invoking the purge callback proves deletion,
  not autonomous scheduling. Design must demonstrate the latter without an
  operator trigger, or keep AC-8 explicitly unproven in GAPS with its compensating
  check and named owner; do not relabel the trigger-based check as an AC-8 pass.
- Preserve the explicit `T−P` day policy in operator documentation; it is a
  calendar-day cutoff evaluated on scheduled runs, not exact per-click expiry.
- Honor ordered properties/migration custody after `01-audit-read` and its
  ancestry check. Check mission-03 Q4 again at plan-lock: the lead's recorded
  continuation can route an aggregation decision into this unbuilt slice before
  lock, rather than treating the present default as irreversible authorization.
- RQ-01 is expected to be clarified in passing by requirements. No remaining
  issue warrants withholding the design handoff.

## Self-check

Exact handed-off SPEC verified and read in full; mission decision checked at its
queue transition; all ACs, allocation, errors, privacy, ambiguity rows, scope and
proof items reviewed. Finding cites both SPEC and shipped behavior and is graded
by consequence. Only this review and the shared ledger authored; no source,
test, SPEC or design edits. One review ledger row appended; **handoff to design**.
