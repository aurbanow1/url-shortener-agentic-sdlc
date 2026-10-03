---
id: OPR.99.0.3.5
slice: 05-ci-cd
mission: 02-brownfield
status: draft
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "Every change to the repository is checked by the same quality gate in GitHub Actions (on every pull request and on push to main), the jar and image are built and the jar is smoked on loopback without publishing anything, and dependency updates are proposed automatically, as the human decided (D14, docs/guidance/ci-cd.md)."
depends_on: []
---

# Slice 05 — CI/CD

## Intent

Every change to the repository is checked by the same quality gate in GitHub Actions (on every pull request and on push to main), the jar and image are built and the jar is smoked on loopback without publishing anything, and dependency updates are proposed automatically, as the human decided (D14, docs/guidance/ci-cd.md).

Today the gate runs only on the agents' own machine, and a handoff note
claims it passed. The human merges stacked pull requests into the GitHub
repository (D13) without seeing a check run on any of them. Two people feel
this: the human, who merges on trust, and the Operator, who has no proof that
the artefact on `main` starts on a clean machine. This slice makes the gate's
result visible on every pull request and on `main`, so the decision to merge
rests on a check run and not on a note.

## Mini-requirements

### Requirements covered

| Id | Requirement (short) | Proven by |
|---|---|---|
| D14 (human decision, `PLAN.md` §10; `docs/guidance/ci-cd.md` §1–§3) | CI runs the `check` gate on every pull request (stacked ones included) and on `main`; CD builds the jar and the image and smokes the jar on loopback; nothing is published without a human trigger; dependency updates are proposed | AC-1 to AC-13 |
| NFR-M1 (cross-cutting) | 100 % line and branch coverage enforced in the build | AC-2, AC-12 (the same `check` gate, no CI-only exclusion) |
| D2 / culture §4 | no agent pushes or publishes; the service is never exposed beyond loopback | AC-5, AC-6, AC-13 |

`docs/REQUIREMENTS.md` allocates no FR or NFR id to this slice beyond NFR-M1.
D14 is the origin (mission SPEC, second amendment).

### Personas

- **Primary:** the human who merges pull requests (reviewer and owner of the repository).
- **Secondary:** Operator (relies on the artefact on `main` starting); Development Agent (reads a red check instead of a reviewer's note).

### User stories

- As the human who merges, I want every pull request, stacked ones included, to show a `gate` check run with the full quality gate, so that I merge on a visible green result and not on a claim.
- As the human who merges, I want the same gate to run on every push to `main`, so that a merge that breaks the gate is visible at once.
- As an Operator, I want every push to `main` to build the jar and the image and to prove the jar answers the public journey on loopback, so that I know the artefact that would ship starts.
- As the human who owns the repository, I want nothing pushed, published or deployed by a workflow, so that releasing stays my act.
- As the human who owns the repository, I want dependency and action updates proposed as pull requests, so that the gate checks them like any other change.

### Acceptance criteria

The GitHub-side outcomes (AC-13) happen only when the human pushes. AC-1 to
AC-12 are checked on the candidate SHA without GitHub: AC-1 to AC-11 by
reading the parsed files, AC-12 by running the same commands locally. The QA
judge (`qa2-agent`) runs without network. The two network-dependent captures,
the `git ls-remote` output for AC-9 and the `docker build` log for AC-12, are
produced by the builder on a networked seat and attached under `proof/`, and
the judge verifies against them. "The workflow files" means the three files
under `.github/` on the candidate.

#### CI: the gate on every change

- **AC-1 — CI triggers on every pull request, on `main` and on demand.** [D14]
  GIVEN `.github/workflows/ci.yml` on the candidate
  WHEN its triggers are read
  THEN it runs on `pull_request` with no branch or path filter (so a pull request based on another pull request's branch runs too, D13), on `push` to `main`, and on `workflow_dispatch`; and it does not use `pull_request_target`.

- **AC-2 — The gate job runs the full `check` on JDK 21 with no CI-only skip.** [D14, NFR-M1]
  GIVEN `ci.yml` on the candidate
  WHEN its jobs are read
  THEN a job whose check run is named `gate` checks out the commit, sets up a Temurin 21 JDK, and runs the repository's own Gradle wrapper with the `check` task. The run line excludes no task (no `-x`), the job has no `continue-on-error`, and no step masks a failure (no `|| true` or equivalent). The repository's local entry point `scripts/gw` is not used.

- **AC-3 — Gate reports stay readable after a failure.** [D14]
  GIVEN `ci.yml` on the candidate
  WHEN its steps are read
  THEN after the gate step the test, coverage and Javadoc reports are uploaded as an artefact with `if: always()` and a retention of at least 30 days.

#### CD: the artefact that would ship starts

- **AC-4 — CD runs on `main` and on demand.** [D14]
  GIVEN `.github/workflows/cd.yml` on the candidate
  WHEN its triggers are read
  THEN it runs on `push` to `main` and on `workflow_dispatch`, and on nothing else.

- **AC-5 — CD builds the jar, smokes it on loopback, and builds the image.** [D14]
  GIVEN `cd.yml` on the candidate
  WHEN its steps are read
  THEN one job builds the jar with the repository's Gradle wrapper and starts it listening on `127.0.0.1` only. It runs `scripts/smoke.sh` as shipped (unmodified) against that address, stops the jar, builds the image from the repository's `Dockerfile`, and uploads the jar and the smoke log as artefacts with a retention of at least 30 days. The smoke log is uploaded with `if: always()`.

- **AC-6 — Nothing is published.** [D14, D2]
  GIVEN the workflow files on the candidate
  WHEN every step and permission is read
  THEN no step logs in to a registry, pushes an image, publishes a package, creates a release or tag, or deploys; no job holds `packages: write`, `contents: write`, `deployments: write` or `id-token: write`; and no workflow references a repository secret (`secrets.*`; the automatic read-only job token of AC-7 is not a repository secret).

#### Rules for every workflow

- **AC-7 — Least privilege.** [D14, `ci-cd.md` §3.1]
  GIVEN each workflow file on the candidate
  WHEN its permissions are read
  THEN it declares `permissions: contents: read` at the top level, and any job-level grant beyond that has a comment giving the reason.

- **AC-8 — No untrusted text reaches a shell.** [D14, `ci-cd.md` §3.3]
  GIVEN each workflow file on the candidate
  WHEN every `run:` block is read
  THEN no `run:` block contains a `${{ github.event.* }}` or `${{ github.head_ref }}` expression; any such value reaches a step through `env:`.

- **AC-9 — Every action is pinned to a commit.** [D14, `ci-cd.md` §3.2]
  GIVEN each workflow file on the candidate
  WHEN every `uses:` is read
  THEN it names a full 40-character commit SHA followed by a comment with the version tag. For each one, the `git ls-remote --tags` capture of the action's repository (taken by the builder, under `proof/`) shows that tag pointing at that SHA, or for an annotated tag its peeled `^{}` entry does.

- **AC-10 — Every job is bounded and superseded by a newer push.** [D14, `ci-cd.md` §3.4]
  GIVEN each workflow file on the candidate
  WHEN its jobs are read
  THEN every job sets `timeout-minutes`, and every workflow sets a `concurrency` group keyed on the workflow and the ref with `cancel-in-progress: true`.

#### Dependency updates

- **AC-11 — Updates are proposed weekly for actions and Gradle.** [D14, `ci-cd.md` §3.2]
  GIVEN `.github/dependabot.yml` on the candidate
  WHEN it is read
  THEN it is a version-2 configuration with one `github-actions` entry and one `gradle` entry, each on directory `/` with a weekly schedule.

#### Local proof before the first GitHub run

- **AC-12 — The files are valid, and the same commands pass locally.** [D14, `ci-cd.md` §4]
  GIVEN the candidate SHA in a clean worktree
  WHEN the three files are linted (with `actionlint` if installed, otherwise each is parsed as YAML), the gate's Gradle task runs through `scripts/gw --offline check`, the jar is built and started on `127.0.0.1` and `scripts/smoke.sh` runs against it, and the builder runs `docker build` on the repository's `Dockerfile` on a networked seat (the base image is pulled)
  THEN the lint or parse reports no error. `check` passes with 100 % line and branch coverage. The smoke prints no `SMOKE FAIL` and exits `0`. The image builds. `git diff --stat <base> <candidate>` lists only paths under `.github/`. The QA judge runs every step except the image build, and judges that from the builder's log.

#### On GitHub (the human's push)

- **AC-13 — The first GitHub runs are green and recorded.** [D14, D13]
  GIVEN the human has pushed the stacked pull-request branch that carries this slice's merge (agents cannot push, D2)
  WHEN GitHub Actions runs
  THEN that pull request shows a `gate` check run that concluded `success`. After the human merges and `main` is pushed, a `cd.yml` run on `main` concludes `success` with the jar and smoke-log artefacts attached. Each run's URL is recorded in `PROOF.md` by the operator. Until then `PROOF.md` lists this criterion as pending. It is not claimed.

### Business rules

1. **Same gate, everywhere.** CI runs the same Gradle task list the agents run locally (`check`), on a clean runner, through the repository's wrapper. If CI and a local run disagree, that is a finding against the build or a test. CI never skips, excludes or tolerates it. A test that cannot run on a runner becomes a `docs/qa/GAPS.md` entry decided like any other gap, not a CI-only exclusion.
2. **Stacked pull requests are first-class.** The human merges a stack of pull requests, each based on the one before (D13). The gate therefore runs on every pull request whatever its base branch. A filter on `main` alone would leave most of the stack unchecked.
3. **Publishing is a human act.** No workflow pushes, publishes, tags, releases or deploys. CD proves the artefact starts; it does not ship it. No deploy job exists, because no deploy target exists (the service runs on loopback).
4. **Loopback, also on runners.** The CD smoke binds the jar to `127.0.0.1` and calls `scripts/smoke.sh`, which only ever reaches loopback.
5. **The smoke is called, not rewritten.** `cd.yml` calls `scripts/smoke.sh` as shipped. If a mode the workflow needs is missing from it, the change is a grant request to the orchestration lead at plan-lock, not an inline script in the workflow.
6. **Evidence of a red run survives.** Reports and the smoke log upload even when the step before them failed, and are kept at least 30 days, so a failure can be read without a rerun.
7. **A pending run is stated, not implied.** Until the human's push produces a run, every proof item that depends on GitHub says "pending".

### Non-functional

- **Bounded runs.** Every job has a timeout. A newer push to the same ref cancels the older run (AC-10). The timeout values are the design's.
- **Least privilege and supply chain.** Read-only token, actions pinned to commits, no untrusted text in a shell (AC-7 to AC-9).
- **Coverage gate (NFR-M1).** Unchanged: this slice changes no product code. The gate on the candidate still reports 100 % line and branch coverage (AC-12), and its reports are committed like any slice's.
- **ADR (NFR-M2).** None required unless the design chooses something cross-cutting (for example a runner image policy). The design says which.
- **Territory.** `.github/` only. Not `src/`, `scripts/`, `build.gradle.kts`, `Dockerfile`, `compose.yaml` or `docs/api/`.

### Scope

**In scope**

- `.github/workflows/ci.yml`, `.github/workflows/cd.yml` and `.github/dependabot.yml`, meeting AC-1 to AC-11.
- Local verification (AC-12) and the pending record of the first GitHub runs (AC-13).

**Explicitly out of scope**

- Pushing, opening pull requests or merging: the human's acts (D2, D13).
- Branch protection and making `gate` a required check: a repository setting the human owns (`ci-cd.md` §4).
- Any deploy job, environment or repository secret: no deploy target exists (`ci-cd.md` §2).
- Publishing the image or the jar to any registry or release.
- Changing the product, the build script, the `Dockerfile`, `compose.yaml` or `scripts/` (territory).
- Running the release-level checks of mission 01 (`--restart`, `--inspect`, `--drain`, `--bench`) on runners. CD runs the installed smoke only.
- Dependabot for the `docker` ecosystem (the `Dockerfile` base image). `ci-cd.md` names only `github-actions` and `gradle` (A-4).
- A GitHub run on a failing commit to prove the gate turns red. The configuration-level checks of AC-2 stand in for it (A-6).

## Ambiguity log

| # | Question | Options | Resolution |
|---|---|---|---|
| A-1 | Does CD also run on pull requests? | `push` to `main` + dispatch; also every pull request | **decided** by the guide the human adopted with D14 (`ci-cd.md` §2): `push` to `main` and `workflow_dispatch`. A pull request already runs the full gate. A dispatch builds the artefact for any ref on demand. |
| A-2 | How does CD start the jar for the smoke: the smoke script's own jar mode, or a separate start followed by the base-URL smoke? | either | **assumed** either, the design's choice. The requirement is the observable outcome (AC-5): the jar listens on `127.0.0.1` only and the shipped smoke passes against it. Safe: both are shipped modes of the same script. |
| A-3 | Which runner? | a GitHub-hosted Linux runner; macOS; self-hosted | **assumed** a GitHub-hosted Linux runner (the design names the image). The smoke's prerequisites (bash, curl, Perl, mkfifo) and Docker are present there. No self-hosted runner exists to trust. Safe: one line to change. |
| A-4 | Should Dependabot also watch the `Dockerfile` base image? | `github-actions` + `gradle`; also `docker` | **assumed** the two the guide names (AC-11). Safe: adding an ecosystem later is one entry, and narrowing keeps the human's guide as the contract. |
| A-5 | The first GitHub run cannot happen before this slice is accepted (agents cannot push). Is acceptance blocked on it? | block acceptance; accept with the run pending and recorded later | **decided** by the operator's constraint when relaying D14 (`qitem-20261003182906-74ccb677`, `slice.yaml`) and `ci-cd.md` §4: the slice is accepted on AC-1 to AC-12, and `PROOF.md` names AC-13 as pending until the operator records the run URLs. |
| A-6 | How is "a failing gate turns the check red" proven without a GitHub run on a failing commit? | push a deliberately failing branch; configuration evidence | **assumed** configuration evidence (AC-2: no skip, no `continue-on-error`, no masked exit), plus the local `check` exit code. Safe: a deliberate red run needs the human's push and adds no information beyond what the files and GitHub's documented behaviour show. The `GAPS.md` row names it. |

No question is parked on `human@kernel`. D14, D13 and the guide the human
adopted settle the product questions; the rest are reversible, single-line
defaults.

## Proof contract

- [ ] AC-1 to AC-11 each checked against the candidate SHA's files by the QA judge (`qa2-agent`), with the line that satisfies each recorded.
- [ ] AC-12: the lint or parse output, the `scripts/gw --offline check` log (100 % line and branch coverage), the loopback jar smoke log and the `git diff --stat` showing only `.github/`, all for the candidate SHA, under `proof/`. The `docker build` log, taken by the builder on a networked seat, is attached with `rig proof add` under `proof/`.
- [ ] AC-9: the `git ls-remote --tags` output for every pinned action, taken by the builder on a networked seat and attached with `rig proof add` under `proof/`, matching each SHA to its tag.
- [ ] Unit and functional JaCoCo reports for the candidate committed under `docs/qa/coverage/05-ci-cd/unit/` and `docs/qa/coverage/05-ci-cd/functional/` (unchanged product; they show the gate still holds at 100 %).
- [ ] `docs/qa/TRACEABILITY.md` holds a table for `05-ci-cd` mapping AC-1 to AC-13 and rules 1 to 7 to the check that proves each (file line, local command or GitHub run), with the requirement id beside each AC.
- [ ] `docs/qa/GAPS.md` holds a row for `05-ci-cd`. It names AC-13 as pending the human's push, and the deliberate red run as not exercised (A-6).
- [ ] `PROOF.md` names AC-13 as pending. Once the human has pushed, the operator records the `gate` run URL on the pull request and the `cd.yml` run URL on `main`, each with its conclusion.

## Source material

- `PLAN.md` §10, decisions D2, D5, D13, D14.
- `docs/guidance/ci-cd.md` (`ed1d314`): §2 required workflows, §3 rules, §4 verification, §6 review checklist.
- `missions/02-brownfield/SPEC.md` (slice table, second amendment); `slice.yaml` (territory, operator constraints, judges).
- Shipped baseline on `main`: `build.gradle.kts` (`check` = unit + functional suites, JaCoCo merged verification, Javadoc doclint; Java toolchain 21), `gradlew` and `gradle/wrapper/` (tracked, executable), `scripts/gw` (pins a local JDK, not for runners), `scripts/smoke.sh` (base-URL mode and `--jar` mode; loopback only through `scripts/http`), `Dockerfile`. No `gradle.properties`.

## Intent visual

N/A: non-visual slice.

## Status

- 2026-10-03: requirements written: 13 acceptance criteria (12 checked on the candidate without GitHub, 1 pending the human's push), 7 business rules, 6 ambiguity rows (4 assumed, 2 decided, none parked).

## Dependencies

- None on other slices (`depends_on: []`). Territory `.github/` is disjoint from every slice in flight.
- The first GitHub run depends on the human pushing the stacked pull-request branch (D13).

## Self-check

- Every AC is observable: AC-1 to AC-11 from the workflow files a reader parses, AC-12 from commands anyone can run on the candidate, AC-13 from GitHub check runs and their URLs. None reads intent from a note.
- Error and safety paths are ACs: a failing gate cannot be masked (AC-2), red-run evidence survives (AC-3, AC-5), nothing is published (AC-6), least privilege (AC-7), no untrusted text in a shell (AC-8), supply-chain pinning (AC-9), runaway runs bounded (AC-10). There is no product privacy path, because no product code changes. AC-12's diff check proves that.
- Business rules cover the non-obvious logic: stacked pull requests need an unfiltered trigger, the same gate with no CI-only skip, the smoke called not rewritten, a pending run stated.
- Out of scope is explicit, including branch protection, deploys, the docker ecosystem and the release-level smoke modes.
- Every ambiguity is resolved: 4 assumed with reasons, 2 decided (the guide adopted with D14, the operator's constraint), none parked.
- The proof contract names the per-suite coverage reports, the traceability rows, the `GAPS.md` row and the by-effect captures (lint, gate, smoke, image build, ls-remote). It assigns the two network-dependent captures to the builder, because the QA judge's seat has no network.
- No design leaked: job and step layout, runner image, timeout values, caching and the jar-start mechanism are left to the design. Named on purpose as contract: the three file paths, the `gate` check-run name, and the triggers and rules the human's guide fixes.
- Consistent with D14, D13, D2 and the slice's territory. `scripts/smoke.sh` and the build stay untouched.
- Checked on `main` by me: the wrapper files are tracked and executable, as are `scripts/smoke.sh` and `scripts/http`. There is no `gradle.properties` pinning a local JDK. `build.gradle.kts` uses a Java 21 toolchain, which a Temurin 21 setup satisfies. A grep for host-tool references (Homebrew paths, docker, process launches, the smoke script) in the unit and functional sources hit one functional test, and only in a comment. Not verified by me: that the functional suite passes on a Linux runner (timing-sensitive journeys may behave differently there). AC-13's first run will show it, and a disagreement is a finding under rule 1.
- `plan-review` lenses applied while drafting (the skill was not invoked separately for this small slice). The engineering lens added rule 2 (stacked pull requests), AC-12's diff check and the ls-remote proof for AC-9. The strategy lens kept deploys and branch protection out. The UX lens: the human's interface is the `gate` check run on a pull request, so its name is contract (AC-2).
