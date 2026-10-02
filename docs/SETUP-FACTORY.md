# Running the factory

The product in this repo builds and runs with Gradle alone (see `README.md`).
This page is for running the *factory* — the OpenRig rig that produced it.

## Prerequisites

| Need | Why | Check |
|---|---|---|
| macOS/Linux, Node 22/24, tmux | OpenRig runtime | `rig preflight` |
| OpenRig 0.6.3 | the control plane (`npm i -g @openrig/cli@0.6.3`) | `rig --version` |
| Claude Code, logged in | 5 seats run on Claude | `claude --version` |
| Codex CLI, logged in | QA and review seats run on Codex (cross-runtime independence) | `codex login status` |
| JDK 21 | Gradle 9 / Spring Boot 4 | `/opt/homebrew/opt/openjdk@21/bin/java -version` (edit `scripts/env.sh` for another path) |
| Docker (optional) | installed-smoke of the image | `docker info` |

## One-time setup

```sh
git clone <this repo> && cd url-shortener
scripts/gw check        # warms .gradle-home so sandboxed seats can build --offline
# register the repo as an OpenRig project (the daemon's work tree):
printf '  - id: urlshort\n    root: %s\n' "$PWD" >> ~/.openrig/workspace/workspace.yaml
rig spec validate rig/rig.yaml && rig workflow validate "$PWD/rig/workflows/urlshort-slice.workflow.yaml"
```

The shared OpenRig skill pool is vendored under `rig/agents/shared/`
(`scripts/vendor-openrig-shared.sh` refreshes it after an upgrade), so the role
specs resolve from the repo alone.

## Permissions (what the seats may do without asking)

- Rig posture: `permission_policy: builtin:standard` (Claude `acceptEdits`, Codex `workspace-write`).
- Claude seats: `.claude/settings.json` (committed) allows `rig`, `git`, `./gradlew`, `java`, read-only shell tools, localhost `curl`, `docker compose`; **denies** `git push`, `git reset --hard`, `git clean -f`, `rm -rf`, `docker push`.
- Codex seats: `.codex/rules/urlshort.rules` (project layer) allows the same families and marks `git push`, `git reset --hard`, `git clean`, `docker push` **forbidden**; a forbidden rule overrides any user-level allow. Verify: `codex execpolicy check --pretty --rules "$PWD/.codex/rules/urlshort.rules" git push origin main`.
- Nothing is published by an agent: no remote is configured, and ship sign-off is a human gate.

## Launch

```sh
rig up rig/rig.yaml --cwd "$PWD" --plan     # preview
rig up rig/rig.yaml --cwd "$PWD"            # boots 7 seats; each posts "<seat> READY" in the chatroom
rig ps --nodes --rig urlshort-factory
rig chatroom history urlshort-factory
```

Launch notes learned the hard way (OpenRig 0.6.3):

- All seats share this directory, so role files travel by `send_text` (a
  `guidance_merge` role block would collide across seats).
- Claude Code stops for approval on any command containing `$VAR`, `$(…)`,
  backticks or `source`, whatever the allow list says — seat instructions use
  literal paths and `scripts/gw`.
- Codex seats run sandboxed; `.codex/rules/urlshort.rules` lets `scripts/gw`
  run outside the sandbox (Gradle needs a loopback lock socket). Upgrade Codex
  *before* launching, or its "update available" dialog blocks the seat.
- While the rig runs, OpenRig appends managed blocks to `AGENTS.md` (Codex) and
  `CLAUDE.local.md` (Claude). Never `git add -A`; `AGENTS.md` is marked
  `git update-index --skip-worktree` during a run — reverse it after `rig down`
  with `git update-index --no-skip-worktree AGENTS.md` (the blocks are stripped
  by `rig down`, so the file returns to its committed content).
- Relaunching: `rig down urlshort-factory --force`, wait until
  `tmux ls | grep urlshort` is empty, then `rig down <rigId> --delete --force`,
  then `rig up …`. An immediate `rig up` after `rig down` can race the dying
  sessions and leave seats with "Cannot establish managed input target".

Then hand the orchestration lead a mission:

```sh
rig workflow instantiate-lifecycle "$PWD/missions/00-hello" \
  --operation-key hello-$(date +%s) --root-objective "Dry run: ping endpoint through every step" \
  --created-by operator-human@kernel --rig urlshort-factory --json
```

## Your gates (the human's job)

Open Mission Control: `rig ui open` (or `rig tui`). A seat that needs your
decision **parks its packet on `human@kernel`** (OpenRig 0.6.3's human
registry only supports Slack bindings, so there is no chat ping — the parked
item is the signal). Decisions are: mission plan-locks, high-tier slice
plan-locks, ambiguity questions, ship sign-offs. Each carries a summary and an
evidence path. Decide in the UI or with:

```sh
rig view show held                                   # everything parked, incl. on you
rig queue list --json | jq '.[] | select(.blockedOn=="human@kernel") | {qitemId,summary,evidenceRef}'
rig queue show <qitem> --full
rig queue resolve <qitem> --decision "approve: <one line of reasoning>"   # unparks + nudges the owner
```

The owning seat then records the stamp on your behalf
(`rig scope … approve --on-behalf-of human@kernel`) — both the decision text
and the stamp land in the append-only audit log.

## Watching and intervening

```sh
rig workflow status                    # what needs attention
rig workflow trace <instance>          # the step trail
rig queue transitions <qitem>          # a packet's audit log
rig capture <seat>  /  rig transcript <seat> --tail 100
rig workflow resume|route|abort …      # the orchestration lead's dial; you may use it too
rig down urlshort-factory --snapshot   # safe-stop everything (restorable with rig up urlshort-factory)
```

## Evidence

`tools/evidence-export.sh <mission>` writes the raw audit trail to
`docs/evidence/<mission>/`; `node tools/sdlc-metrics.mjs` derives the
reliability metrics into `docs/metrics/`.
