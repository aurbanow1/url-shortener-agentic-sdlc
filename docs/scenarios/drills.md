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
| 2026-10-02 22:51Z | 00-hello / 01-ping | standalone slice workflow spec edited (design_review inserted) while the slice instance was in `design` | the next projection read the spec through and routed `design → design_review` to the Review Agent: standalone specs apply to in-flight instances at their next hop, whereas the compiled mission lifecycle required an explicit (and here refused) `rig workflow revise` | `docs/evidence/00-hello/instances/01M3ZA8Q39QEB3R1QDQCVTER18.trace.json` (next export) |
| 2026-10-02 22:3xZ | 00-hello lifecycle | per-chunk review steps added to the mission profile while the lifecycle was running | `rig workflow revise 01M3Z8AJ…` reported `incompatible` (mission_plan_lock already completed; new predecessor would rewrite completed work) and adopted nothing — the dry run finishes on its original graph, later missions use the new one | revise receipt in `docs/evidence/00-hello/` (next export), commit of `project.yaml` |
| 2026-10-02 22:21–22:23Z | 00-hello / 01-ping (design step) | design-agent sat at a Claude Code WebFetch permission prompt (Spring Boot 4.1 release notes); packet idle 10 min | OpenRig stuck-sweep created `qitem-recovery-a562baedab298be5` for the orchestration lead; the lead diagnosed the root cause (operator-only prompt) and parked the finding on the underlying packet; operator approved the fetch and allow-listed documentation domains in `.claude/settings.json` | `docs/evidence/00-hello/packets/qitem-recovery-a562baedab298be5.*`, commit `4b67617` |
