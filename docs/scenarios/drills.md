# Fault-injection drills

Deliberate exercises of the governance paths, run by the Release & Reliability
Agent (mission 02 unless noted) so that retry, rollback, fallback and safe-stop
behaviour is demonstrated, not assumed. Each drill records the command, the
observed behaviour and the evidence path. Natural failures that occurred during
normal work are listed separately at the bottom.

| Drill | Governance path | Command(s) | Observed | Evidence |
|---|---|---|---|---|

## Natural failures observed during the missions

| When | Slice | What failed | How it was handled | Evidence |
|---|---|---|---|---|
| 2026-10-02 22:21–22:23Z | 00-hello / 01-ping (design step) | design-agent sat at a Claude Code WebFetch permission prompt (Spring Boot 4.1 release notes); packet idle 10 min | OpenRig stuck-sweep created `qitem-recovery-a562baedab298be5` for the orchestration lead; the lead diagnosed the root cause (operator-only prompt) and parked the finding on the underlying packet; operator approved the fetch and allow-listed documentation domains in `.claude/settings.json` | `docs/evidence/00-hello/packets/qitem-recovery-a562baedab298be5.*`, commit `4b67617` |
