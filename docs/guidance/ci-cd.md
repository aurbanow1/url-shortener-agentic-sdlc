# CI/CD guidance — GitHub Actions on every repository

Read by the Development Agent before adding or changing anything under
`.github/`, by the Review Agent at `code_review` when a change touches it, by
the Release & Reliability Agent at `release_prep`, and by the lead at
`decompose` (a repository without CI/CD gets it in the next wave).

## 1. Principle

Every repository has CI/CD in **GitHub Actions** from its first pushed
commit. CI re-runs the same gate the agents run locally on a clean machine for
every pull request and every push to `main`, so a green result is visible on
the pull request instead of being claimed in a handoff note. CD builds the
artefacts that would ship from the exact SHA and proves that they start.
Publishing or deploying stays a human act: an agent never adds a step that
pushes an image, a package or a release without a human trigger.

Check: `.github/workflows/ci.yml` and `.github/workflows/cd.yml` exist on
`main`, and the newest pull request shows a `gate` check run.

## 2. Required workflows

| File | Triggers | Jobs | What it proves |
|---|---|---|---|
| `.github/workflows/ci.yml` | `pull_request` with no branch filter, so stacked pull requests based on another branch also run; `push` to `main`; `workflow_dispatch` | `gate`: checkout, `actions/setup-java` with Temurin 21, `gradle/actions/setup-gradle` for caching, `./gradlew check --no-daemon`, upload the test, coverage and Javadoc reports with `if: always()` | unit and functional suites, 100 % merged line and branch coverage, Javadoc doclint, on a clean runner |
| `.github/workflows/cd.yml` | `push` to `main`; `workflow_dispatch` | `package`: `./gradlew bootJar`, start the jar bound to `127.0.0.1`, run `scripts/smoke.sh http://127.0.0.1:<port>`, stop it, `docker build` the image (not pushed), upload the jar and the smoke log as artefacts | the artefact that would ship starts and answers the public journey; the Dockerfile still builds |

- CI runs `./gradlew`, not `scripts/gw`. `scripts/gw` exists to pin the local
  JDK and Gradle home for seats; on a runner `actions/setup-java` pins the JDK.
  The gradle task list is the same, so the two cannot drift: if CI and a local
  run disagree, that is a finding against the build or a test, not a reason to
  skip it on CI.
- A deploy job exists only once a deploy target exists. It runs on
  `workflow_dispatch` alone, uses a protected `environment` with required
  reviewers where the GitHub plan supports them, and reads its credentials
  from repository secrets that the human created. No such target exists for
  this project: the service runs on loopback.

## 3. Workflow rules

1. **Least privilege.** `permissions: contents: read` at the top of every
   workflow; a job asks for more only when it needs it, with the reason in a
   comment.
2. **Pinned actions.** Every `uses:` names a full commit SHA with the version
   in a trailing comment (`uses: actions/checkout@<sha> # v4.2.2`). Resolve a
   tag with `git ls-remote --tags https://github.com/actions/checkout`.
   `.github/dependabot.yml` keeps the `github-actions` and `gradle` ecosystems
   current with weekly pull requests.
3. **No untrusted code with secrets.** Never `pull_request_target`, and never
   interpolate `${{ github.event.* }}` text into a `run:` line; pass it
   through `env:` instead.
4. **Bounded runs.** `timeout-minutes` on every job;
   `concurrency: { group: <workflow>-<ref>, cancel-in-progress: true }` so a
   new push supersedes the old run on the same branch.
5. **Same gate, no CI-only skips.** No `-x test`, no `continue-on-error` on the
   gate, no coverage exclusion that exists only on CI. A test that cannot run
   on a runner is a gap in `docs/qa/GAPS.md`, decided like any other.
6. **Loopback only, also on runners.** The CD smoke binds `127.0.0.1`, like
   every agent smoke (`release.md` §3).
7. **Evidence stays reachable.** Reports upload with `if: always()` and a
   retention of at least 30 days, so a failing run can be read without a rerun.

## 4. Verifying before handoff

Agents cannot push, so the first real run happens when the human pushes the
pull request branch. Before handing off, the author:

- lints the workflow with `actionlint` when it is installed; otherwise parses
  every file as YAML and reads it against §3 line by line;
- runs the same gradle tasks locally through `scripts/gw` (`check`, then
  `bootJar` and `scripts/smoke.sh` against a loopback jar);
- says in `PROOF.md` that the GitHub run has not happened yet. The first green
  `gate` run on the pull request closes that item, and its URL is recorded in
  `PROOF.md`.

The human merges a pull request only on a green `gate`. Where the GitHub plan
supports branch protection, `gate` is a required check on `main`.

## 5. Release package

`RELEASE.md` §2 (Artifact and gate) quotes the CI run and the CD run for the
candidate SHA, with their URLs and conclusions, beside the local gate run. A
red or missing run is a known gap in §7, not a footnote.

## 6. Review checklist (`code_review` on any change under `.github/`)

| Check | Severity if missing |
|---|---|
| `ci.yml` runs `check` on every pull request and on `main` | HIGH |
| `permissions: contents: read` at the top; any broader grant justified | HIGH |
| no `pull_request_target`; no `${{ github.event.* }}` text in `run:` | HIGH |
| every action pinned to a commit SHA with a version comment | MEDIUM |
| no CI-only skip, exclusion or `continue-on-error` on the gate | HIGH |
| `cd.yml` builds the jar and the image and smokes the jar on loopback; nothing published without a human trigger | HIGH |
| timeouts and concurrency on every job; reports uploaded with `if: always()` | LOW |
| Dependabot covers `github-actions` and `gradle` | LOW |

## 7. Self-check (author, before handoff)

- [ ] Both workflows exist and follow §2–§3.
- [ ] Linted or parsed, and read against §3.
- [ ] Gradle tasks and the smoke ran locally on the candidate SHA.
- [ ] `PROOF.md` names the first GitHub run as pending, or links it.
