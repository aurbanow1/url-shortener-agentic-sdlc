# PROOF — OPR.99.0.3.5 Ci Cd

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.5 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated>

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/screenshot-01.png — <what it shows>
- proof/capture-behavior.gif — <what it shows>
- proof/command-output.txt — <what it proves>

## Residue / caveats (if any)

<documented residue: what's not covered + where it's tracked>

## Builder (dev2-agent@urlshort-factory)

**Candidate `add7ab5`** on `slice/05-ci-cd`. That is one commit,
`chore(05-ci-cd): gate, package and dependency updates on GitHub Actions`, on top of `main` at
`06818ca`. `git diff --stat main...slice/05-ci-cd` lists only `.github/dependabot.yml`,
`.github/workflows/cd.yml` and `.github/workflows/ci.yml` (129 insertions). Nothing under `src/`,
`scripts/`, `build.gradle.kts`, `Dockerfile` or `compose.yaml` changed.

| AC | What I ran | Outcome | Evidence |
|---|---|---|---|
| all (copy) | `cp -R design-probe/draft/.github .worktrees/05-ci-cd/.github`, then `diff -r` against the locked draft | `IDENTICAL`: the files are the design's, byte for byte | — |
| AC-9 | `git ls-remote --tags` for `actions/checkout`, `actions/setup-java`, `gradle/actions`, `actions/upload-artifact` (19:34Z) | every tag still points at the pinned commit; `gradle/actions` `v6.4.0` is annotated (tag object `b9bee63e…`, peeled `3f5f9ada…` = the pin). **No tag moved**, so there is no second SHA to record | [`proof/action-pins-ls-remote.txt`](proof/action-pins-ls-remote.txt) |
| AC-12 lint | `docker run --rm -v <worktree>:/repo --workdir /repo rhysd/actionlint:1.7.12 -verbose` | `Found 0 errors in 2 files` | [`proof/actionlint-add7ab5.txt`](proof/actionlint-add7ab5.txt) |
| AC-12 lint, control | the same image over `design-probe/lint-control/.github/workflows/control.yml` | 2 findings: the untrusted `github.event.pull_request.title` in `run:`, and SC2086. The lint discriminates | [`proof/actionlint-control.txt`](proof/actionlint-control.txt) |
| AC-11, AC-12 parse | `node design-probe/parse-yaml.mjs <@openrig/cli>/node_modules/yaml` over the three files (YAML 1.2) | all three `parsed`; no error or warning | [`proof/parse-yaml-add7ab5.txt`](proof/parse-yaml-add7ab5.txt) |
| AC-2, AC-12 gate | `scripts/gw --log proof/check-add7ab5.txt --offline -p .worktrees/05-ci-cd check --rerun-tasks` | `BUILD SUCCESSFUL`, 14 of 14 tasks executed. Unit 165 tests, functional 155 tests, 0 failures, errors or skips. Merged JaCoCo: lines 443/443, branches 162/162 (100 %). Javadoc doclint green | [`proof/check-add7ab5.txt`](proof/check-add7ab5.txt); reports in `docs/qa/coverage/05-ci-cd/{unit,functional,all}/` |
| AC-5, AC-12 jar | `scripts/gw --offline -p .worktrees/05-ci-cd bootJar` | `BUILD SUCCESSFUL` | — |
| AC-5, AC-12 smoke | `scripts/smoke.sh --jar .worktrees/05-ci-cd/build/libs/urlshort.jar 18091 proof/smoke-jar-log-add7ab5.txt`, which is `cd.yml`'s command; it binds `--server.address=127.0.0.1` itself | `SMOKE OK against http://127.0.0.1:18091`; the overrides held (`shortUrl` from `URLSHORT_PUBLIC_BASE_URL`, the database file where `SPRING_DATASOURCE_URL` points, the forwarded client admitted 10 times then `429`); `SMOKE JAR OK`; no `SMOKE FAIL`; exit 0; the jar log has `Graceful shutdown complete` | [`proof/smoke-add7ab5.txt`](proof/smoke-add7ab5.txt), [`proof/smoke-jar-log-add7ab5.txt`](proof/smoke-jar-log-add7ab5.txt) |
| AC-5, AC-12 image | `docker build --tag urlshort:cd .worktrees/05-ci-cd` | `Successfully built`, 15/15 steps, **every step from the local layer cache** | [`proof/docker-build-add7ab5.txt`](proof/docker-build-add7ab5.txt) |
| AC-12 image, uncached | `docker build --pull --no-cache --tag urlshort:cd .worktrees/05-ci-cd` (pulls the base images, as AC-12 asks) | not completed (Gradle download stalled at step 5); see *Uncached image build* below | [`proof/docker-build-nocache-add7ab5.txt`](proof/docker-build-nocache-add7ab5.txt) |
| AC-1, AC-3, AC-4, AC-6, AC-7, AC-8, AC-10 | read against design §7, line by line (the files are the reviewed drafts, byte for byte) | as designed: `pull_request` with no filter, plus `push` to `main` and `workflow_dispatch`; no `pull_request_target`; `if: always()` uploads at 30 days; `cd.yml` only on `main` and dispatch; no login, push, publish, `secrets.` or write grant; top-level `contents: read` and no job-level grant; no `${{` in any `run:`; `timeout-minutes: 20` and `concurrency` with `cancel-in-progress: true` in both | the files |

**AC-13: pending.** No GitHub run has happened. Agents cannot push (D2), so the first `gate` run and
the first `cd` run on `main` happen when the human pushes the stacked branch that carries this
slice's merge. The operator records their URLs here. Until then this criterion is not claimed.

### Uncached image build

**Not completed, and stopped by me.** `--pull` checked the base image against Docker Hub
(`eclipse-temurin:21-jdk`, digest `3e3c176f…`, "Image is up to date"). Steps 2–4 ran uncached. Step
5, `RUN ./gradlew --no-daemon --version`, downloads the Gradle distribution inside the container. It
showed no progress for about 18 minutes (19:37–19:55Z) on this host's link, so I stopped it. The log
is kept as it stood: [`proof/docker-build-nocache-add7ab5.txt`](proof/docker-build-nocache-add7ab5.txt).

What this does and does not show:
- The image build evidence for AC-5/AC-12 is the cached build above, from this exact worktree: all
  15 steps of the shipped `Dockerfile`, which this slice does not change. The design probe's L3 had
  the same limit (its steps 1–5 also came from the layer cache).
- I read the stall as the host network, not the build: steps 1–4 passed, the same step is cached from
  an earlier successful run of this `Dockerfile`, and the design probe's `check` on Linux spent about
  14 of its 15 minutes downloading.
- A from-scratch image build is first proven by `cd.yml` on a GitHub runner (AC-13, pending). Its
  `timeout-minutes: 20` bounds a stall like this one there.

## Self-check

- ci-cd guide §7. Both workflows exist and follow §2–§3: they are the reviewed drafts, byte for byte.
  They were linted with actionlint (with a control showing it discriminates), parsed as YAML 1.2, and
  read against §3. The Gradle tasks and the `--jar` smoke ran locally on `add7ab5`. AC-13 is named
  pending above.
- No product change, so there were no tests to write first. The gate is the same `check` with no skip.
- `URLSHORT_JAVA_HOME: ${{ env.JAVA_HOME }}` was not exercised. Locally the smoke used Homebrew's JDK
  21 by default, as the design says (§12, not run by the probe either).
- **Not verified by me:** a GitHub-hosted runner, `pipefail` under it, `setup-gradle`'s cache and
  wrapper validation, a Dependabot run. All of these are AC-13 or after it.
