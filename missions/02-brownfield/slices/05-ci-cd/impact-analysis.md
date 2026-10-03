# Impact analysis — 05-ci-cd

Written before `design.md` (`docs/guidance/brownfield.md` §2) by `design2-agent@urlshort-factory`,
2026-10-03, for SPEC `9ac54aa` (requirements PASS, RQ-01 fixed in passing; review `aa0143f`).

**Baseline.** `main` at `8b63e5b`. The repository has no `.github/` directory. The gate and the
jar smoke had only ever run on the seats' macOS machine, through `scripts/gw`. The probe
(`design-probe/ci-probe.sh`, output in [`design-probe/output-linux.txt`](design-probe/output-linux.txt))
ran a clean checkout of `8b63e5b` with the steps this design puts in the workflows. It used
Ubuntu userland, Temurin 21 and a non-root user (uid 1001, like a GitHub runner's `runner`), with
`./gradlew` and no `scripts/gw`:

- `./gradlew check --no-daemon`: **exit 0**, `BUILD SUCCESSFUL`.
  - 320 tests (unit and functional), with 0 failures, 0 errors and 0 skipped (JUnit XML).
  - Merged coverage 443/443 lines and 162/162 branches (`build/reports/jacoco/all`).
  - Javadoc built.
  - It took 915 s wall time, of which about 14 minutes was downloading Gradle 9.7.1 and the
    dependencies over this machine's slow link. Compiling, both suites, the reports and Javadoc
    took about 60 s: main classes were written at 19:18:06Z, the Javadoc at 19:18:51Z.
- `./gradlew bootJar --no-daemon` took 6 s. Then
  `scripts/smoke.sh --jar build/libs/urlshort.jar 18091 build/smoke/jar.log`, with `java` from
  `PATH` (no `URLSHORT_JAVA_HOME`), ran to **`SMOKE JAR OK`, exit 0** in 6 s:
  - health, ping, the public journey, metrics, Prometheus and OpenAPI all passed;
  - the environment overrides held: a forwarded client was admitted 10 times, then got `429`;
  - the jar logged `Graceful shutdown complete`.
- `docker build` of the shipped `Dockerfile` on the host: see `design.md` §12, row L3.

## Change in one sentence

Three new files under `.github/`. They run the existing `check` gate on every pull request and every
push to `main`, build the jar and the image on `main` and prove that the jar answers the public
journey on loopback, and propose dependency updates weekly. Nothing is published, and no product
file changes. Requirements: D14 (with D13 and D2), NFR-M1.

## Impacted modules

Found with `grep -rln -E '\.github|GitHub Actions|ci\.yml|cd\.yml|dependabot'` outside `.git`,
`build` and the worktrees. It hits only planning, guidance, review and slice documents, plus
`.claude/settings.json` (a WebFetch allow rule for `github.com`). No code, script or build file
refers to `.github/`.

| File | Change | Who reads it |
|---|---|---|
| `.github/workflows/ci.yml` | **new**: the `gate` job | GitHub Actions on `pull_request`, `push` to `main`, `workflow_dispatch` |
| `.github/workflows/cd.yml` | **new**: the `package` job | GitHub Actions on `push` to `main`, `workflow_dispatch` |
| `.github/dependabot.yml` | **new**: `github-actions` and `gradle`, weekly | GitHub Dependabot |

Read by the workflows, **unchanged** (territory forbids them; each was read to confirm it works as is):

| File | Used by | Confirmed |
|---|---|---|
| `gradlew`, `gradle/wrapper/gradle-wrapper.{jar,properties}` | both jobs | `gradlew` tracked `100755`; Gradle 9.7.1 from `services.gradle.org` with `validateDistributionUrl=true`; the wrapper jar is checked by `setup-gradle`'s wrapper validation, on by default |
| `settings.gradle.kts`, `build.gradle.kts` | both jobs | `check` = unit + functional suites, three JaCoCo reports, merged 100 % line and branch verification, Javadoc with `-Xdoclint:all -Werror`. Java toolchain 21, satisfied by the Temurin 21 that `setup-java` installs. No toolchain download plugin, none needed |
| `src/**` | `gate`; `package` (compile only) | the functional suite writes only under `build/` (`OpenApiDocumentTest` writes `build/openapi/openapi.json` and compares it with the committed `docs/api/openapi.json`) |
| `scripts/smoke.sh`, `scripts/http` | `package` | both tracked `100755`. The `--jar` mode (`smoke.sh:319–354`) starts the jar with `--server.address=127.0.0.1` on a temporary H2 file, runs the full smoke, checks the environment overrides, sends `SIGTERM` and requires `Graceful shutdown complete`. Its JDK is `URLSHORT_JAVA_HOME`, else `java` on `PATH` (`smoke.sh:28–31`). `scripts/http` refuses any non-loopback URL |
| `Dockerfile`, `.dockerignore` | `package` | multi-stage, `eclipse-temurin:21-jdk` → `21-jre`; the image's own Gradle run skips the suites (`-x test -x functionalTest`), which is the image recipe, not the gate (the gate is `ci.yml`) |

## Impacted endpoints

None. The service's HTTP surface and `docs/api/openapi.json` are unchanged. The CD smoke calls the
existing endpoints of a jar on the runner's loopback, against a throwaway database.

## Impacted schema and data

None. No migration, no Flyway number. The CD smoke writes to an H2 file inside the smoke's own
`mktemp -d` directory (`smoke.sh:325`), which disappears with the runner.

## Impacted data flows

The product's flows are unchanged. New flows, all outside the running service:

| Flow | Path |
|---|---|
| Change check | push to a pull request branch, or to `main` → `ci.yml` `gate` → check run `gate` on the commit + artifact `gate-reports` |
| Artefact proof | push to `main`, or a manual dispatch → `cd.yml` `package` → artifacts `urlshort-jar`, `smoke-logs`; the image is built on the runner and discarded |
| Dependency updates | Dependabot, weekly → pull requests → `gate` |

Sequence: `design.md` §4.

### How GitHub runs the gate on the human's stacked pull requests (D13)

These facts decide whether rule 2 holds. They come from the GitHub Actions documentation, read on
2026-10-03, and are quoted in `design.md` §12:

1. **Which pull requests get a `gate`.** A `pull_request` run uses the workflow files of the pull
   request's merge commit (`GITHUB_SHA` = the last merge commit on `refs/pull/<n>/merge`). The
   first pull request with a `gate` is the one whose tree carries this slice's merge. Pull requests
   below it in the stack were cut before `ci.yml` existed. They show no check run, and that is
   expected, not a failure.
2. **What a `gate` checks on a pull request.** The merge of the head into the base, which is the
   tree the merge would produce. It does not check the head alone. A pull request with a merge
   conflict gets no run at all ("Workflows will not run on `pull_request` activity if the pull
   request has a merge conflict").
3. **After a merge, the next pull request's base changes, and the gate does not re-run.** D13
   deletes merged branches, so GitHub retargets the next pull request to `main`. That is the
   `edited` activity ("the base branch of a pull request was changed"), and a workflow runs by
   default only on `opened`, `synchronize` and `reopened`. The earlier `gate` result stays on the
   head commit.

   **Why that is sound under D13.** The human merges in order with merge commits only. After pull
   request *n* merges, `main`'s tree equals branch `pr/n`'s, because `pr/n` already contains
   everything on `main`. So pull request *n+1* merged into `main` gives the same tree its `gate`
   already checked against `pr/n`. The **push to `main` re-runs the gate** after every merge, which
   catches any divergence: an out-of-order merge, or a commit on `main` outside the stack.

   **Escape.** To re-check a pull request against its new base before merging, close and reopen it
   (`reopened` builds a fresh merge commit), or push to its branch. "Re-run jobs" is not an escape:
   it reuses the original event's `GITHUB_SHA`, the old merge commit.

   Adding `edited` to the triggers was rejected. It also fires on every title or body edit, and
   under D13 the result it would add is the one the push to `main` gives anyway (design §11).
4. **Dependabot pull requests** run "as though they are from a forked repository": a read-only
   token and no secrets. This design uses neither, so their `gate` is the same gate.
5. **A manual dispatch** needs the workflow on the default branch. It is available once this
   slice's merge reaches GitHub's `main`.

## Blast radius

| If this is wrong | Worst case | Detection |
|---|---|---|
| The gate passes while a check failed (a skipped task, `continue-on-error`, `|| true`, a pipe that drops the exit code) | the human merges a red change on a green check | AC-2 by reading; `ci.yml` has no pipe at all; `defaults.run.shell: bash` makes every multi-command `run:` fail fast with `pipefail`; the push to `main` runs the gate again |
| A trigger filter leaves part of the stack unchecked | a stacked pull request merges without a check | AC-1: `pull_request` with no `branches`, `paths` or `types` filter |
| A workflow writes to the repository, publishes or deploys | an agent-authored workflow ships without the human (D2) | AC-6, AC-7: only `contents: read`, no secret, no login, push, release or deploy step |
| Text from a pull request reaches a shell | code injection on the runner | AC-8: no `run:` block contains any `${{ }}` expression |
| An action's tag is moved to malicious code | arbitrary code in the job | AC-9: every `uses:` is a commit SHA; Dependabot proposes moves as pull requests the gate checks |
| A pull request swaps `gradle-wrapper.jar` | arbitrary code in the job | `setup-gradle` validates every wrapper jar in the repository by default (`validate-wrappers: true`) |
| A pull request poisons the Gradle cache `main` later reads | tampered dependencies in a `main` build | `setup-gradle` writes the cache only on the default branch (`cache-read-only` defaults to true elsewhere) |
| A job hangs | runner minutes burnt; a stale run blocks the newest | `timeout-minutes` on both jobs; `concurrency` cancels the older run on the same ref |
| The suites behave differently on Linux | a red first run that local runs never showed | the Linux probe (baseline above); after that, rule 1: a finding against the test, or a `docs/qa/GAPS.md` decision, never a CI-only skip |
| The CD jar listens beyond loopback | an exposed service on a shared runner | `smoke.sh` passes `--server.address=127.0.0.1`; `scripts/http` refuses non-loopback URLs |
| Docker Hub refuses the anonymous base-image pull (rate limit on a shared runner address) | `package` red for an infrastructure reason | the job log; re-run the job. **Not mitigated:** authenticating the pull needs a repository secret, which AC-6 forbids |
| Runner minutes | the private repository's included minutes run out, and runs stop | GitHub's billing page (the human's). Every pull-request push and every `main` push costs one `gate`, and a `main` push also costs one `package`. The probe's build work took about 1 minute after the downloads. A runner adds its setup and its own download time. Not measured on GitHub |

## Compatibility (FR-13)

- **Product.** No file outside `.github/` changes (AC-12's `git diff --stat`). Links, codes,
  stored rows, the API document and the deployment files are untouched.
- **The seats' local workflow.** Unchanged: `scripts/gw check` stays the seats' entry point. CI
  runs the same `check` task through `./gradlew`, so the two cannot drift by configuration (rule 1).
- **The repository on GitHub.** The workflows start on the first push that carries them (above).
  Branch protection stays the human's setting (out of scope).

## Test impact

- **Added, changed, removed:** none under `src/`. This slice's checks are the file readings
  (AC-1 to AC-11), the local commands (AC-12) and the first GitHub runs (AC-13, pending).
- **The existing suites, on a new platform:** the functional suite now runs on Linux for every
  change. The probe ran all 320 tests there before the design was written, green, with coverage
  at 100 % (baseline above). Nothing in the suites depends on macOS.

## Observability impact

- No product log, metric or health change.
- New, on GitHub: the `gate` check run per pull request and per `main` push, the `package` run per
  `main` push, the job logs, and `setup-gradle`'s job summary. The artifacts are `gate-reports`,
  `urlshort-jar` and `smoke-logs`, each kept 30 days.
- Operator documentation: the CI/CD row in `docs/DESIGN.md` §3 (mine). `RELEASE.md` §2 quotes
  both runs from the next release on (`docs/guidance/ci-cd.md` §5, the release seat's).

## Risks and mitigations, ranked

| # | Risk | Mitigation | Owner step |
|---|---|---|---|
| 1 | A green `gate` that did not run the whole gate | the gate is one `./gradlew check` line, no `-x`, no `continue-on-error`, no pipe; reviewed against `ci-cd.md` §6 | design → code review → QA (AC-2) |
| 2 | A workflow that publishes or escalates the token | `contents: read` only, no secrets, no publishing step | design → security review (AC-6, AC-7) |
| 3 | Supply chain: actions, the wrapper jar, the cache | SHA pins with the `ls-remote` proof; wrapper validation and a read-only cache on non-default branches, both `setup-gradle` defaults; the MIT `basic` cache provider instead of the proprietary default (design §1) | build (AC-9 proof) → review |
| 4 | The suites fail on a Linux runner | measured by the probe; rule 1 if the runner disagrees | QA (AC-12), then AC-13 |
| 5 | Stacked pull requests: no re-run after a retarget | sound under D13 (above); the push to `main` re-checks; the escape is documented | design (this file, `docs/DESIGN.md` §3) |
| 6 | Docker Hub anonymous pull limits | accepted residual, named in `docs/qa/GAPS.md` by QA if it occurs | release |

## Rollback

`git revert` of the slice's merge commit removes `.github/`. The human can also disable either
workflow in the repository settings without a commit. There is no data, schema or artefact to undo,
because nothing was published.

## Self-check

- The baseline was run, not recalled: the gate, the jar smoke and the image build on the shipped
  `main`, with the workflows' own commands, on Linux.
- Every file the workflows read is listed and was opened, including file modes and the smoke mode's
  loopback binding.
- The GitHub behaviours the stacked-pull-request rule rests on come from the documentation and are
  quoted in the design. The retarget case is analysed, and its escape is written down.
- Not verified here: a real GitHub run. That needs the human's push (AC-13). The probe ran on
  aarch64 with Ubuntu userland from the Temurin image, not on GitHub's x86_64 `ubuntu-24.04` image
  (design §12).
