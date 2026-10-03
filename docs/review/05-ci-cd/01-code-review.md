# Code review — 05-ci-cd

**PASS — merge-ready under the explicit AC-13 pending-run exception; no findings.**
Candidate `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14`, base `06818cab00cab0c3a9ab225172307aaea96d8d53`. Reviewer `review2-agent@urlshort-factory` (Codex), 2026-10-03. Packet `qitem-20261003202216-eba453ef`; combined code/security exit.

## Context proof

The slice adds GitHub gate/package workflows and weekly dependency proposals without changing product behavior or publishing. I read SPEC `9ac54aa`, design `9e67e55`, worktree AGENTS, the CI/CD architecture contract, QA SUMMARY/PROOF/traceability/gaps and the full `main...slice/05-ci-cd` diff. High confidence in this configuration boundary: HEAD equals QA's candidate, the worktree is clean, all three files exactly match the independently reviewed/linted design drafts. No implementation or design edit was made.

## Complete file ledger

| Changed file | Verdict |
|---|---|
| `.github/workflows/ci.yml` (53 lines) | PASS — unfiltered PR, main and dispatch; Temurin 21 and exact wrapper check; no skip/failure mask; reports always uploaded for 30 days; bounded read-only job. |
| `.github/workflows/cd.yml` (64 lines) | PASS — main/dispatch only; jar retained, shipped loopback smoke, explicit bash/pipefail, logs always retained, image built locally; no registry login, publish, secret or write grant. |
| `.github/dependabot.yml` (12 lines) | PASS — exactly the two weekly root ecosystems specified. |

Three changed, three reviewed; 129 additions. No `src/`, tests, build, scripts, Dockerfile, schema or API-contract delta. Java-layer and new-Javadoc checks are not applicable to this diff; existing doclint remains in the gate.

## Acceptance and evidence audit

- AC-1–11: read every line and parsed the YAML; checked the QA checker itself before replaying it. [Recorded replay](proof/code-file-check-add7ab5.txt) passes all configuration checks, including nine references matched to the builder's four captured action tags (Gradle uses the peeled annotated tag). No design deviation.
- AC-12: my fresh `../../scripts/gw --log <main>/docs/review/05-ci-cd/proof/code-check-add7ab5.txt --offline check --rerun-tasks` in the exact worktree passed in 32 seconds, **14/14 tasks executed**. [Gate log](proof/code-check-add7ab5.txt). XML: **165 unit + 155 functional, zero failures/errors/skips**. Fresh merged CSV: **443/443 lines, 162/162 branches**. Unit 400/443 lines and 162/162 branches; functional 408/443 and 131/162. Same per-suite totals as QA, no exclusions or threshold change.
- [Independent evidence audit](proof/code-evidence-audit-add7ab5.txt): all **321 committed QA report hashes** match the recorded inventory; all **186 source-method mappings** exist in source and TRACEABILITY. Reviewer invocation inventory matches QA after normalizing only object identities/generated canaries in 11 parameterized display names; the initial exact-name comparison failed on those values, not tests. All 13 ACs/seven rules have scoped trace rows.
- Read QA's `qa-smoke-add7ab5.txt` and structured jar log: successful public journey and error cases, 29 request-completed events, stated privacy canaries absent, graceful shutdown. Read the unchanged smoke implementation, including loopback binding and stop. I did not launch another jar. QA's manual duplicate/expiry/audit limitations remain stated; inherited tests, including JDBC audit assertions, passed in my gate.
- Read `docker-build-pull-add7ab5.txt` and both candidate-linked builder attachment records: both base pulls/digests, all 15 steps, real steps 5–7, bootJar success and final image success. Image/pin captures match QA's stored hashes. The SPEC assigns these network captures to the builder; this is an audit of them, not an independent image build. The abandoned fully uncached attempt is disclosed honestly.
- AC-13 remains **PENDING** under SPEC A-5/D2/D13. No hosted run, upload, cache, wrapper-validation or Dependabot execution is claimed. Reports and smoke retention after a red run are verified as configuration under A-6.

Current [proof projection](proof/proof-state-add7ab5.json) has five accepted items and items 5/6 unknown: the shared TRACEABILITY/GAPS hashes changed after unrelated `01-audit-read` corrections. Diff from QA evidence commit `f10c796` confirms the `05-ci-cd` sections are unchanged. Retained QA judgments accept seven items on this SHA, with item 7 accepting the pending record only. This is a projection qualification for the integrator, not a product or evidence-content defect; QA was informed. I did not replace its judgments or claim current ready7/7.

## Ponytail review

Lean already. Ship.

## Findings and verdict

No findings: MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fixes in passing or backlog requests. The workflows reuse the platform and shipped smoke, with no speculative layer or duplicate process lifecycle. Together with [security review](02-security-review.md), hand off to integrate, preserving AC-13 and the proof-projection qualification above.

## Self-check

Exact candidate and clean worktree verified; every changed file read; fresh full gate and QA audit completed; scope and limits stated; both ledger rows carry the exact SHA. No product/tests/SPEC/design edits and no publishing action.
