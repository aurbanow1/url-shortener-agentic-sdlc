# Requirements review — 05-ci-cd

Initial candidate: `f27165b92399223715fc564d5ef0307859b09d06`.
Final candidate after in-packet correction: `9ac54aa50c0dc1746afa512eb35e88afe2f217f2`.
Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003185217-9f98ac8d`;
instance: `01M41HFT4CTYPNWWNB8PJ99NFE`.

**PASS — RQ-01 MEDIUM fixed in passing; no open findings.** The initial finding
and focused re-review below preserve its resolution. The slice prepares CI/CD configuration
and local evidence; real GitHub runs remain explicitly pending the human's push.

## Context and coverage proof

The human needs the same gate on every stacked pull request and on `main`,
plus a packaged jar smoke and image build that publish nothing. Territory is
`.github/` only. Confidence is high in the requirements and their alignment
with D14; no assertion is made about workflows or remote runs that do not yet
exist.

Read the complete candidate SPEC and its diff: **one changed file, one
reviewed**. Also read the mission brief and CI/CD amendment, slice manifest,
`PLAN.md` D2/D13/D14, `docs/REQUIREMENTS.md` cross-cutting obligations,
requirements §6, review guidance, brownfield §7 and CI/CD guidance §§1–7.
Checked the human relay at `qitem-20261003182906-74ccb677`, including its
explicit pending-run instruction. Workflow guidance selects no extra SDLC
composition; this is the ordinary independent requirements review.

| Changed file | Verdict |
|---|---|
| `missions/02-brownfield/slices/05-ci-cd/SPEC.md` | PASS with RQ-01: complete bounded outcome, 13 GIVEN/WHEN/THEN ACs, seven rules, six resolved ambiguity rows and complete proof contract. |

Fingerprint and exact-line checks:
[`proof/requirements-source-check.txt`](proof/requirements-source-check.txt).

| Contract | Assessment |
|---|---|
| D14 / AC-1–3 | Every PR base and push to main covered; named gate uses the existing task without skips; failure-report retention explicit. |
| D14 / AC-4–5 | Main/dispatch packaging, loopback-only jar, unchanged installed smoke, image build and retained artifacts are concrete. |
| D2 / AC-6–10 | No publishing, credentials or write grants named; least privilege, shell-expression boundary, commit pins with tag evidence, timeouts and cancellation specified. RQ-01 clarifies which files are workflows. |
| D14 / AC-11 | Weekly actions and Gradle updates, matching the adopted guide. Docker updates are explicitly outside this slice. |
| NFR-M1 / AC-2, AC-12 | Same merged coverage gate; candidate-specific local gate, smoke, lint/parse, image evidence and territory check. No CI-only exclusion. NFR-M2 remains explicit in the non-functional section for any new cross-cutting choice. |
| AC-13 / actual remote effect | PR gate and main CD run URLs/conclusions required after the human push. Static checks and local commands are not relabelled as remote success. The human's recorded constraint authorizes this pending item. |
| Ambiguities and scope | Four narrow assumptions, two cited decisions, none parked. Runner image/timeouts/start mechanism stay with design. No new product endpoint, data or privacy surface. |
| Proof contract | Candidate file checks, local captures, builder network captures, both suite reports, AC/rule traceability, GAPS and pending GitHub evidence all named. |

AC-1–11 deliberately inspect the delivered configuration: the adopted human
guide fixes those files and rules, and explicitly permits local verification
before a push. That is the applicable observable surface for this infrastructure
slice; requiring a new HTTP endpoint or premature GitHub run would change the
authorized task. Named Actions/JDK/workflow choices come from D14's guide,
not an unauthorized requirements-layer design choice.

## Findings

| ID | Severity | File:line | Evidence / consequence | Required change |
|---|---|---|---|---|
| RQ-01 | MEDIUM | `missions/02-brownfield/slices/05-ci-cd/SPEC.md:62` | The definition says “workflow files” means all three `.github/` files, including `dependabot.yml`. AC-7 then requires permissions on each workflow file; AC-10 requires workflow concurrency. The same SPEC separately identifies Dependabot as the version-2 updates configuration in AC-11. Literal checking would apply Actions-only requirements to that different configuration. Exact lines 62–63, 102–104, 117–126 are captured in `proof/requirements-source-check.txt`. | Define “workflow files” as `ci.yml` and `cd.yml`, and use “configuration files” for all three where intended. Fix in passing; no new requirement, scope or backlog slice is needed. |

## Verification and limits

Inspected the shipped build: `check` includes both suites, merged line/branch
coverage verification and Javadoc doclint; Java toolchain is 21. Inspected the
smoke's public journey and jar mode: loopback binding is explicit, and a
separate-start/base-URL call is also available. The local wrapper and CI wrapper
distinction is required by the human's guide. The Dockerfile is already present.

`scripts/gw --log docs/review/05-ci-cd/proof/requirements-baseline-check.txt
--offline check`: **BUILD SUCCESSFUL, all 14 tasks up-to-date**. This verifies
the unchanged baseline/toolchain; it is not a fresh Linux runner test, workflow
execution, image build or fulfillment of AC-12/13. No product/test changes,
remote requests, push, publish or workflow dispatch were performed.

## Initial self-check

Exact one-file candidate reviewed; all ACs and safety/failure boundaries mapped;
human exception verified at its source; one non-blocking finding cited with
exact text. Only review artifacts authored. The initial assessment permits
handoff with RQ-01 fixed in passing and AC-13 pending until the human push.

## Re-review 9ac54aa50c0dc1746afa512eb35e88afe2f217f2

Same packet, 2026-10-03. The author supplied the correction before this review
closed. **RQ-01: fixed.** The definition now names `ci.yml` and `cd.yml` as the
workflow files, while “the three files” includes Dependabot. The status and
self-check were adjusted to match. The complete one-file delta was read and
the final SPEC byte-matched; evidence is appended to
`proof/requirements-source-check.txt`. No other AC or requirement changed.

| Re-review file | Verdict |
|---|---|
| `missions/02-brownfield/slices/05-ci-cd/SPEC.md` | PASS: RQ-01 resolved; one file changed/reviewed, no new finding. |

No repeated product test is warranted for this wording correction. The prior
baseline result retains its stated limits. **Final verdict: PASS, handoff to
design**, zero open MUST-FIX/HIGH/MEDIUM/LOW/INFO. Real GitHub runs and the
deliberate red-run experiment remain explicitly unproven as authorized; the
former must retain its pending state until the operator records the URLs.
