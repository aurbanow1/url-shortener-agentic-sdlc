# Running the factory

The product in this repo builds and runs with Gradle alone (see [README](../README.md)
and the [service/factory runbook](RUNBOOK.md)).
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
- Claude seats: `.claude/settings.json` (committed) allows `rig`, `git`, `scripts/*`, `tools/*`, `java`, `node`, read-only shell tools, `docker compose|build|run|stop|rm|logs`; **denies** `git push`, `git reset --hard`, `git clean -f`, `rm -rf`, `docker push`. Seats reach the running app only through `scripts/http` (refuses non-loopback URLs) and run Gradle only through `scripts/gw` (`--log <file>` replaces shell redirection).
- Codex seats: `.codex/rules/urlshort.rules` (project layer) allows the same families and marks `git push`, `git reset --hard`, `git clean`, `docker push` **forbidden**; a forbidden rule overrides any user-level allow. Verify: `codex execpolicy check --pretty --rules "$PWD/.codex/rules/urlshort.rules" git push origin main`.
- Nothing is published by an agent. The human configured the Git remote for hosted
  CI/CD; push, release publication and ship sign-off remain human actions.

## Models and reasoning effort

OpenRig pins a seat's *model* in its agent spec (`defaults.model`); `rig seat set-model <seat> --model <id>` changes a live seat (audited, effective at the seat's next launch). Reasoning *effort* is a runtime setting: the project `.claude/settings.json` sets the default for Claude seats, and a seat can raise its own with the `/effort <level>` slash command (`low|medium|high|xhigh|max`; `/effort status` reads it back), which is how the design seat runs at `xhigh` while the rest stay at `high`. From the operator shell it must be sent raw — `rig send --raw <seat> "/effort xhigh"` — because the default From/To envelope turns a slash command into plain message text; a `startup.actions` `send_text` cannot do it (startup text is delivered as a message, not as a command), so after every launch of `design-agent` the operator runs `rig send --raw <seat> "/effort xhigh"` and confirms with `/effort status`. Set the runtime defaults once before `rig up`:

- Claude Code — every seat's model is pinned to `claude-opus-5-5` in its agent spec (`defaults.model`), so the user-level `"model"` alias does not matter for seats; the project's `.claude/settings.json` sets `"effortLevel": "high"` for the author seats (project scope overrides user scope), plus `"modelSettings": {"claude-opus-5-5": {"effortLevel": "xhigh"}}` for the builder.
- Codex — `~/.codex/config.toml`: `model = "gpt-6-astra"`, `model_reasoning_effort = "xhigh"`.

| Seat | Runtime | Model | Effort |
|---|---|---|---|
| design-agent, design2-agent | Claude Code | Claude Opus 5.5 (pinned in its agent spec) | xhigh (operator sends `rig send --raw design-agent@urlshort-factory "/effort xhigh"` after each launch) |
| orchestration-lead | Claude Code | Claude Opus 5.5 (pinned) | high (project default; D12) |
| requirements-agent | Codex | GPT-6-Astra (pinned in its agent spec, D17) | xhigh (`~/.codex/config.toml`) |
| release-agent, release2-agent | Codex | GPT-6.1-Sol (pinned in its agent spec, D17) | xhigh (`~/.codex/config.toml`) |
| development-agent, dev2-agent | Claude Code | Claude Opus 5.5 (pinned in `rig/agents/development-agent/agent.yaml`) | high |
| qa-agent, qa2-agent | Codex | GPT-6.1-Sol (pinned in `rig/agents/qa-agent/agent.yaml`) | xhigh |
| review-agent, review2-agent | Codex | GPT-6-Astra | xhigh |

D17 moved requirements and release authoring to Codex. Their reviews still
use a separate seat, but can share the author's runtime; requirements review
can also share the model. Product implementation remains Claude Code with
Codex QA/review. Preserve the original runtime/SHA attribution in each receipt;
do not describe every stage as cross-runtime independent.

Verify after launch: `ps -axo args= | grep -- --model` lists the pinned Claude seats; a Codex seat prints its model and effort in its status bar (`rig capture qa-agent@urlshort-factory`).

## Launch

```sh
rig up rig/rig.yaml --cwd "$PWD" --plan     # preview
rig up rig/rig.yaml --cwd "$PWD"            # boots 12 seats; each posts "<seat> READY" in the chatroom
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

Open Mission Control with `rig tui`; `rig tui --shared` joins the kernel's
existing shared terminal (detach with Ctrl-b d). `rig ui open` opens the browser
UI. A seat that needs your
decision **parks its packet on `human@kernel`** (OpenRig 0.6.3's human
registry only supports Slack bindings, so there is no chat ping — the parked
item is the signal). Mission plan-locks, ambiguity questions and ship sign-offs
remain human decisions. D11 delegates all slice plan-locks to the orchestration
lead for this run; [GOVERNANCE.md](GOVERNANCE.md) records the original tier policy
and that delegation. Each gate carries a summary and an evidence path.
Decide in Mission Control or with:

```sh
rig view show held                                   # everything parked, incl. on you
rig queue show <qitem> --full
rig queue resolve <qitem> --decision "approve: <one line of reasoning>"   # unparks + nudges the owner
```

The orchestration lead records mission delivery stamps after the release seat
reports the recorded ship decision. Other gates follow their owning role's
contract. Stamps use `rig scope … approve --on-behalf-of human@kernel` to record
the human's decision; both that decision and the stamp remain in the audit log.

## Watching and intervening

```sh
rig workflow status                    # what needs attention
rig workflow trace <instance>          # the step trail
rig queue transitions <qitem>          # a packet's audit log
rig capture <seat>  /  rig transcript <seat> --tail 100
rig workflow resume|route|abort …      # the orchestration lead's dial; you may use it too
rig down urlshort-factory --snapshot   # safe-stop with a recorded snapshot
rig up urlshort-factory --existing --cwd "$PWD"   # resume the existing rig
```

## Evidence

`tools/evidence-export.sh <mission>` writes the raw audit trail to
`docs/evidence/<mission>/`; `node tools/sdlc-metrics.mjs` derives the
reliability metrics into `docs/metrics/`; `node tools/dep-advisories.mjs`
checks the resolved runtime classpath against OSV at release prep.
