# 02-analytics — requirements review

Candidate: `488788f1b6bcdbb7caccc89c15217607bffaaa2c`.
Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03.
Author: requirements agent (Claude).
Packet: `qitem-20261003081331-87faf358`.
Instance: `01M40CP0JVZZWR8226MNN8XCCV`, step `requirements_review`.

**Verdict: PASS with one MEDIUM finding, RQ-01.** No MUST-FIX or HIGH.
Hand off to design. Correct the capture-to-AC wording in passing before QA
judges the proof; this does not require another requirements gate or a change
to the acceptance criteria. No implemented analytics behavior is approved by
this review.

## Context proof

The slice adds raw click recording and an anonymous per-link statistics read
to the accepted create/redirect service. It must protect redirect latency and
availability, reduce client data before persistence, and keep existing audit,
error and logging contracts. Unique visitors, retention enforcement, rate
limiting and extra statistics remain with their allocated slices/missions.
Confidence: scope/allocation 99/100; contract/proof interpretation 97/100.
Async handoff, overload behavior, salt disposal and test mechanics remain
design work, explicitly called out in the SPEC.

Read the selected project → mission → slice manifests, the mission decision
brief, REQUIREMENTS baseline, NOTES §1 and current w2 notes, the inherited
slice-01 contracts, system design, and requirements/review guides. Verified
the original human plan-lock in transition 266 of
`qitem-20261003042553-03ac8b4b`; the fast-plan and D11 decisions remain as
recorded in NOTES. No additional human decision is needed for these narrow
defaults. The root assignment explicitly makes CR-01 optional context.

## Complete file and contract coverage

| Changed file | Verdict |
|---|---|
| `missions/01-greenfield-core/slices/02-analytics/SPEC.md` | Read in full, including the complete one-file commit diff; PASS with RQ-01 MEDIUM |

**1 changed file / 1 reviewed.** `git diff-tree` confirms the candidate changes
only this SPEC. An independent byte comparison of the working document with
`git show 488788f:<path>` passed. Documents are reviewed in the main checkout
under the repository agreement; no moving worktree code is part of this pass.
Other seats' review edits were left untouched.

| Contract area | Assessment |
|---|---|
| FR-7 | AC-1–6, AC-14–16 cover one event per successful GET redirect, non-click paths, reduced data, forwarding-header independence, store delay/failure and 200 concurrent redirects without loss under normal conditions. |
| FR-8 | AC-7–13 cover unused/used/retired/unknown links, UTC days, top-10 ranking, deterministic ties, consistency and wrong methods. Independently checked AC-10's arithmetic (26) and lexical tail order. |
| NFR-P1 | AC-3–6/17/18 cover persisted fields, daily hash changes, aggregate-only responses and default-config log canaries. Rule 4 additionally requires secret random salt, same-day stability during uninterrupted service and disposal after the day; the proof explicitly assigns non-HTTP salt checks to design/security review. Restart behavior is disclosed. |
| NFR-L3 | AC-14/15 prove that the redirect does not wait for a slow/failing store. The separate ≤2 ms p95 target remains required, with release measurement and an explicit GAPS entry until measured. A total redirect p95 within L1 cannot by itself prove the added latency meets L3; the SPEC requires that isolation limitation to remain a gap. |
| NFR-O1/O2 | AC-18/19 and inherited rule 10 require the response id on every request-associated JSON event, including asynchronous failure events, under shipped logging settings. W1-02's live log capture is preserved. |
| NFR-M3 | AC-21 specifies the new operation/schema/example and not-found media type, preserves prior operations, and separately requires candidate committed-versus-live equality. Ordered OpenAPI custody remains 02 then 03. |
| NFR-M1/M2 | Artifact obligations are explicit: merged line/branch gate, separate suite reports, gaps, and indexed ADRs before dependent code. These are appropriately artifact checks rather than invented HTTP criteria, as in the accepted slice-01 SPEC. |
| Regression | AC-20 retains redirect behavior and byte-equivalent prior audit rows, with no click/read audit mutations. HEAD redirects are expressly not clicks. |
| Business rules and ambiguity | All nine rules and all 17 ambiguity rows read: 11 assumed with reasons, six decided, none parked. Origin-only referrers, fixed UA classes, raw clicks and connection-address hashing are bounded defaults. The five-second visibility bound must retain a test in the promised rule traceability, even if other tests use a flush. |
| Scope/design boundary | Explicit exclusions retain the mission split. HTTP JSON fields and normalization rules are observable contracts; V2/path references repeat granted territory, without imposing new classes, schema columns, dependencies or queue/thread mechanisms. |
| Proof contract | All 13 items read. Coverage, per-suite reports, AC/rule traceability, GAPS, HTTP/row/log captures, live OpenAPI equality, ADR timing, ERD, salt review and release latency measurement are named. Capture descriptions have the limited RQ-01 mismatch below. |

## Findings

SPEC references below mean
`missions/01-greenfield-core/slices/02-analytics/SPEC.md` at the candidate.

| Id | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| RQ-01 | MEDIUM | `SPEC.md:320`, `SPEC.md:321`, `SPEC.md:322` | The prescribed capture is three redirects followed by statistics and a row/log export, yet it says it proves AC-7 (empty statistics), AC-9 (multiple UTC days) and AC-5 (two addresses across a day boundary). That sequence requires none of those conditions. Likewise it samples AC-19's success paths, not its 404/405/failing-store cases. A later proof judgment could overstate what this capture demonstrates. The separate mandatory functional tests cover the full criteria, so this is not a missing requirement or blocking proof gap. | Describe these captures as representative by-effect checks of the specific assertions they exercise, and identify the controlled functional tests as the full table/boundary proof. Alternatively extend a capture where practical. Keep every AC and the all-AC functional-test obligation unchanged; no requirement to wait through a real midnight. |

Documentary reproduction: run the exact planned three-click sequence at
12:00 UTC from one connection and then export its rows and logs. It satisfies
the capture setup. It cannot observe zero-click statistics, a UTC rollover,
a salt change, or the missing error paths. This is a counterexample to the
planned evidence mapping, not a claim that an unbuilt runtime failed a test.

## Handoff and residual obligations

RQ-01 should be corrected in passing before proof judgments, rather than
parked as product backlog. Existing A-9 (alignment with 03's trusted-proxy
policy) and CR-01 (error-extension OpenAPI schema) remain lead backlog; this
pass neither reopens the accepted slice-01 review nor grants new territory.
Design must cover all nine business rules as promised, including visibility,
overload and salt lifetime. The L3 numerical measurement remains release work
and must not be reported as measured based on the slow-store test alone.

## Verification and self-check

- Independently extracted 21 consecutively numbered ACs, each with GIVEN,
  WHEN and THEN; nine business rules; all nine allocated/cross-cutting IDs;
  17 numbered ambiguity rows; and 13 proof items. The candidate document
  matches the reviewed bytes. All changed content was read.
- `rig scope audit --mission 01-greenfield-core`: PASS, valid rails. This is
  structural evidence only, not a semantic or delivery verdict.
- `scripts/gw --log docs/review/02-analytics/proof/requirements-check.txt
  --offline check`: exit 0, BUILD SUCCESSFUL, 14 tasks up to date. This checks
  the unchanged integrated baseline; it does not execute analytics tests.
  The [command log](proof/requirements-check.txt) records the result.
- RQ-01 has candidate lines, a documentary counterexample and a bounded
  correction. No runtime defect or fresh implementation coverage is claimed.
- Only review artifacts authored; no product, tests, SPEC or design edits.
  Ledger row appended. Verdict follows the severity rule; handoff to design
  carries the non-blocking correction and release measurement limitation.
