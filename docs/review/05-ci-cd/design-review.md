# Design review — 05-ci-cd

**PASS — no findings.** Candidate `9e67e55f1c776be696e64c9337eee2cd7c6bb47f` (design `02dadd3`, preceding impact/probe `8bf1273`), SPEC `9ac54aa`. Reviewer: `review2-agent@urlshort-factory` (Codex), 2026-10-03. Packet `qitem-20261003192547-28bfe85e`.

## Context and complete coverage

Three configuration files run the existing gate on pull requests and main, build and smoke the jar on loopback, build the image without publishing, and propose dependency updates. This satisfies D14 within `.github/`; the first GitHub run remains pending under the explicit AC-13 exception. Confidence is high in the proposed configuration and local mechanisms; a hosted run is not claimed.

All **15** files authored in `8bf1273`, `02dadd3` and `9e67e55` were read. [Fingerprints and YAML checks](proof/design-source-check.txt) tie the drafts to this candidate and show they exactly equal the design's three YAML blocks. Shared `docs/DESIGN.md` changed concurrently; its candidate delta was read with `git show 02dadd3 -- docs/DESIGN.md`. The review does not approve another seat's later edits. No product, SPEC, design or test file was edited.

Reviewed against architecture §§3–8, databases §8 (no database change), review guidance, brownfield §7, CI/CD guidance §§2–6, D13/D14 and the SPEC. Review-team resolves to the ordinary path: no SDLC composition is selected. This is a design review in the main checkout, not an implementation worktree review.

Paths below are relative to `missions/02-brownfield/slices/05-ci-cd/` unless prefixed `docs/`.

| Changed file | Verdict |
|---|---|
| `impact-analysis.md` | PASS — consumers, Linux baseline, stack assumptions, test impact, risks and revert path named; committed first. |
| `design.md` | PASS — all 13 ACs mapped; territory, failure evidence, threat model and hosted-run limits explicit. |
| `design-probe/ci-probe.sh` | PASS — captures gate and smoke exit codes without hiding failures; no production fixture changes. |
| `design-probe/output-linux.txt` | PASS — actual gate, bootJar and smoke succeed; Ubuntu 26.04/aarch64 probe is distinguished from proposed hosted Ubuntu 24.04/x86_64. |
| `design-probe/docker-build.txt` | PASS — all 15 steps complete; cached layers and legacy-builder limitation disclosed. |
| `design-probe/draft/.github/workflows/ci.yml` | PASS — unfiltered pull requests, main and dispatch; exact check task, read token, always-uploaded reports. |
| `design-probe/draft/.github/workflows/cd.yml` | PASS — jar, shipped loopback smoke, image build; failure-preserving bash pipeline; no publish step. |
| `design-probe/draft/.github/dependabot.yml` | PASS — exactly the two required weekly ecosystems. |
| `design-probe/lint-drafts.txt` | PASS — clean workflow lint; independently repeated. |
| `design-probe/lint-control/.github/workflows/control.yml` | PASS — deliberate injection and quoting faults stay outside root `.github/`. |
| `design-probe/lint-control.txt` | PASS — both planted faults detected; audited, not independently rerun. |
| `design-probe/parse-yaml.mjs` | PASS — YAML 1.2 preserves `on`; parse warnings/errors fail. |
| `design-probe/parse-drafts.txt` | PASS — all three parsed configurations match the text; independently parsed again. |
| `docs/DESIGN.md` | PASS — candidate CI/CD contract row, defaults, stack caveat and changelog agree with design. |
| `docs/diagrams/ci-cd-sequence.mmd` | PASS — matches the proposed jobs and events. |

## Acceptance, security and simplicity

| ACs | Assessment |
|---|---|
| AC-1–3 | `gate` has the prescribed triggers, Temurin 21 and exact `./gradlew check --no-daemon`; no skip or failure mask. Reports, test XML, Javadoc and generated OpenAPI remain downloadable after failure for 30 days. |
| AC-4–5 | Package runs only on main push/dispatch. Existing `jar_mode` binds 127.0.0.1, uses a temporary database, smokes and stops the jar on success. Explicit bash enables pipefail for tee. Jar is retained before smoke; smoke logs upload always; Docker build is local. On a failed smoke, process cleanup relies on the hosted job lifecycle rather than a new script change. |
| AC-6–10 | Read-only permissions, no secret reference or publishing grant/step, no event interpolation in shell, full commit pins with tag comments, per-job timeout and per-workflow/ref cancellation. Builder is explicitly assigned fresh tag-to-SHA captures for AC-9. |
| AC-11 | Weekly github-actions and Gradle entries at `/`; exclusions such as Docker and unverified override updates are stated. |
| AC-12–13 | Candidate lint, gate, smoke, image-build evidence, unchanged territory diff and coverage are assigned before handoff. Hosted success URLs remain pending the human push; this review does not close AC-13. |

No HTTP entry point or error contract changes, schema migration or new application audit event. The threat model covers executable pull-request content, event text, action pins, wrapper validation, caches, token scope, artifacts and remote dependencies. Existing D14/CI-CD guidance supplies the cross-cutting decision; declining a duplicate ADR is justified here. Reusing the shipped smoke avoids a second start/stop implementation. One job per workflow and one cache provider are proportionate.

The stacked-PR reasoning depends on D13's in-order merge commits with no unrelated main changes. The documented close/reopen escape obtains a fresh merge-tree check after retargeting. Branch protection stays a human setting. No new gate or exception is requested.

## Verification and limits

- [Independent lint](proof/design-lint.txt): actionlint 1.7.12, cached image with networking disabled and draft files mounted read-only, **0 errors in 2 workflows**. YAML 1.2 independently parsed all three files, and byte comparisons matched the design blocks exactly.
- [Baseline check](proof/design-baseline-check.txt): `scripts/gw --offline check` **succeeded**, 2 tasks executed and 12 up-to-date; test tasks were up-to-date. Product, build, scripts and Docker inputs equal the author's probed `8b63e5b` baseline. This does not prove an unbuilt candidate or fresh hosted execution.
- Read the complete Linux and image-build captures and their probe source. Their successful command outcomes are supported by the logs. The stated 320-test/coverage totals were not independently recomputed from the Linux XML; implementation QA must supply its candidate reports. No duplicate Linux build, jar smoke or Docker build was run in this design review.
- Checked the pinned action definitions: [setup-gradle](https://raw.githubusercontent.com/gradle/actions/3f5f9adaf7d9fecd50b5935e54106014257a94e6/setup-gradle/action.yml) supports basic caching, wrapper validation and the stated read-only/default publication settings; [checkout](https://raw.githubusercontent.com/actions/checkout/3d3c42e5aac5ba805825da76410c181273ba90b1/action.yml) supports disabling credential persistence; [setup-java](https://raw.githubusercontent.com/actions/setup-java/de7274f081f381c8f8158605e0321c36c376e2e6/action.yml) defaults to setting JAVA_HOME/PATH; [upload-artifact](https://raw.githubusercontent.com/actions/upload-artifact/043fb46d1a93c77aae656e7c1c64a875d1fc6a0a/action.yml) supports the retention/path/failure inputs used. These are source checks, not a hosted action run or an audit of action internals.
- [GitHub shell syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#defaultsrun) confirms explicit bash/pipefail. [Pull-request event documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request) confirms the default activity types, merge ref and conflict restriction. Hosted cache operation, wrapper validation, uploads and Dependabot execution still need the first real run.

## Findings and handoff

None: MUST-FIX 0, HIGH 0, MEDIUM 0, LOW 0, INFO 0. No fixes in passing or backlog items requested. Ledger records the same candidate and 15/15 file coverage. Hand off to delegated plan-lock. Preserve the named limits: GitHub AC-13 pending, hosted-run differences, anonymous image-pull availability and branch protection owned by the human.
