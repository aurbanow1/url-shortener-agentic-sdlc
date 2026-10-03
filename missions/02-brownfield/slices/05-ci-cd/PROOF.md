# PROOF — OPR.99.0.3.5 CI/CD

Candidate: `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14`.
QA seat: `qa2-agent@urlshort-factory` (Codex), 2026-10-03.
QA verdict: **PASS for AC-1–12; AC-13 PENDING under SPEC A-5.**
Slice closure and review remain the workflow owners' acts.

## What this proves

Candidate configuration meets AC-1 through AC-11. An independently rerun
quality gate and installed loopback jar smoke show the unchanged product still
passes locally. GitHub execution is **PENDING** the human's push (AC-13/A-5).
The supplemental builder capture independently reconciles both base pulls and the completed image build required by AC-12.

## Artifacts

QA media under `proof/`: `qa-file-checks-add7ab5.txt`, `qa-check-add7ab5.txt`,
`qa-bootjar-add7ab5.txt`, `qa-smoke-add7ab5.txt`, `qa-jar-log-add7ab5.jsonl`,
`qa-diff-add7ab5.txt`, `qa-report-audit-add7ab5.json`. Coverage HTML/XML/CSV
and the CSV summary are in `docs/qa/coverage/05-ci-cd/`; the check table and
qualifications are in `docs/qa/TRACEABILITY.md` and `docs/qa/GAPS.md`.
QA drop: `proof/qa-evidence-add7ab5.md`, attached by `rig proof add`. Builder network drops: `proof/builder-docker-build-pull-add7ab5.md` and `proof/builder-action-pins-add7ab5.md` (commit `ad79bb4`). Attributed QA judgments cover all seven proof-contract items against the exact candidate; item 7 accepts the explicit pending record, not a GitHub success.

## Residue / caveats

AC-13 remains **PENDING**: the operator must record the successful PR `gate`
run URL and successful main `cd.yml` URL with jar and smoke-log artifacts after
the human's push. No run or URL is claimed here. Deliberate red GitHub run,
action/cache/wrapper-validation execution, hosted-runner shell/JAVA_HOME,
Dependabot proposal execution and a from-scratch image build remain unobserved.
These limits are recorded in GAPS; configuration checks use SPEC A-6 and
native-actionlint absence uses AC-12's YAML fallback, without waiving an AC.

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


## QA — independent candidate verification

I read the SPEC, locked design, builder's PROOF, QA and CI/CD guidance. The
addressed project/mission/slice path and packet select QA2; the exact candidate
HEAD matches the packet, and the clean product worktree remains unchanged.
I did not edit product, tests, build scripts, Dockerfile or workflows.

**AC-1 through AC-11:** independently parsed all three files with installed
YAML 1.2, checked their values and read numbered source. The check output names
each satisfying line. Every PR is unfiltered; main and dispatch triggers are
correct; gate checks out and uses Temurin 21 and ./gradlew check without a skip
or tolerated failure. Reports and smoke logs upload always() with 30-day
retention. CD calls the unchanged shipped --jar mode, uploads the jar and
builds the shipped Dockerfile without publishing. Contents read is the only
grant; all run blocks contain no untrusted event/head-ref interpolation. All
nine action references match the builder's four network tag captures (Gradle
uses the annotated tag's peeled commit). Both jobs have 20-minute timeouts and
workflow/ref concurrency with cancellation. Both Dependabot root entries are
weekly, version 2. These are configuration effects, not hosted-runner execution.

**AC-12:** my fresh --offline check --rerun-tasks succeeded in 36 seconds with
165 unit and 155 functional invocations, no failures/errors/skips, and Javadoc
in the gate. Summed CSV lines are400/443 unit,408/443 functional,443/443 merged;
branches162/162 unit,131/162 functional,162/162 merged. All 321 copied report
hashes match. The merged100/100 threshold is unchanged. The candidate/base
diff contains exactly three .github files and 129 insertions.

My independent bootJar succeeded, then I ran the unchanged shipped jar smoke
on 127.0.0.1:18105, exit 0. Its checks observed response statuses, content types,
request-ID headers, create response, redirect Location and no-store, active
read, click stats/reduced referrer, invalid-input400 without echo, unknown404,
wrong-method405, retirement204 followed by410, metrics/Prometheus/OpenAPI and
environment overrides; a fresh forwarded client was admitted 10 times then
429. The captured JSON log contains 29 completed requests with 29 unique IDs
and all those statuses. No invalid input, full referrer path or test client IP
is present. Graceful shutdown was logged; process 54941 was absent and the
port refused connections. The inherited functional AuditJourneyTest methods
query actual H2 rows and request IDs, append-only state and rollback; all four
passed. A manual audit read and duplicate/expiry curl replay on this jar are
not claimed: this slice's SPEC uses configuration ACs and inherited regression
coverage, rather than inventing workflow-to-product AC mappings.

**AC-12 network evidence, independently judged:** supplemental
qitem-20261003200244-22f2ea52 completed at builder commit `ad79bb4`. The new
`docker-build-pull-add7ab5.txt` names the exact SHA and unpiped exit 0. I read
both `Pulling`/`Digest`/up-to-date entries (Temurin 21-jdk and 21-jre), all 15
ordered Dockerfile steps, real steps 5–7 execution and in-container bootJar
success in 55s, and final image/tag success. Cache was used for other steps;
this is the SPEC's required pulled-base build, not a wholly uncached claim.
The initial stopped --no-cache attempt remains historical evidence.
`builder-docker-build-pull-add7ab5.md` attaches the build log and
`builder-action-pins-add7ab5.md` attaches all four pin captures, both with
candidate-linked C1 headers and media references. I performed no networked
image build or independent live tag lookup; the SPEC explicitly assigns those
captures to the builder. Source/capture agreement and hashes are in my report
audit. AC-1–12 are therefore PASS within their specified evidence strategy.

**AC-13 PENDING:** no push, GitHub run, run URL, artifact upload or dependency
proposal observed. This pending state is explicitly accepted by SPEC A-5; an
operator records URLs and successful conclusions after the human pushes.

### Self-check

- Exact candidate HEAD and product tree checked clean; no code/test/build edit.
- AC-1..11 each checked in parsed and numbered files;13 AC/7 rule trace rows
  complete. All 320 inherited invocations map to NFR-M1/rule 1 and earlier
  product AC tables, as the slice's specific evidence strategy requires.
- Real public jar journey and failure checks 400/404/405/410/429 observed.
  No manual duplicate/expiry/audit read or hosted-runner claim.
- Coverage read from all three fresh CSVs; all321 copied file hashes verified;
  per-suite shortfalls and every qualification recorded in GAPS.
- App stopped, PID gone, port refused; worktree left at exact candidate.
- PASS drop attaches the checked QA media and names contract items 1..7. Attributed judgments are against the exact candidate; AC-13 is not asserted green.

## AC-13: first GitHub runs (recorded by the operator, 2026-10-03)

The human pushed `pr/07-ci-cd-merged` (`2e33568`) and opened pull request #7. Read with `gh api` on `aurbanow1/url-shortener-agentic-sdlc`:

| Run | Event, ref, commit | Conclusion | Artifacts |
|---|---|---|---|
| [`ci` 37153245436](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153245436), job `gate` | `pull_request`, `pr/07-ci-cd-merged`, `2e33568` | success, 20:55:04–20:57:40Z | `gate-reports` (2.0 MB) |
| [`ci` 37153380482](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380482) | `push`, `main`, `a3d6867` | success, 20:57:22–20:59:44Z | `gate-reports` (2.0 MB) |
| [`cd` 37153380418](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380418), job `package` | `push`, `main`, `a3d6867` | success, 20:57:22–21:00:46Z; steps `bootJar`, `Smoke the jar on 127.0.0.1` and `docker build` each succeeded | `urlshort-jar` (35.8 MB), `smoke-logs` (4 KB) |

Caveats, stated plainly:
- The two `main` runs ran on `a3d6867`, not on `ecf8dfd`, the merge of #7. That commit also contains Dependabot's Gradle wrapper bump from 9.7.1 to 9.8.0 (pull request #8), which the human merged on GitHub two minutes after #7. The first `main` runs on `ecf8dfd` (`ci` 37153261623, `cd` 37153261626) were cancelled by the workflows' concurrency rule when `a3d6867` arrived. So the `main` evidence is for the gate and the CD job under Gradle 9.8.0. The pull-request gate ran under 9.7.1.
- The image build on the hosted runner is the first from-scratch build: the runner has no layer cache.
- Still unobserved: a deliberately red GitHub run, and branch protection, which the private repository's plan may not offer.
- The attributed judgment of AC-13 belongs to QA (`rig proof judge`). This section records the evidence only.
