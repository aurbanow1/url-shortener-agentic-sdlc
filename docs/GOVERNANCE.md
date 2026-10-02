# Governance — how each orchestration requirement is met, and where the evidence is

Living document. The assignment's workflow-orchestration requirement (PDF §4.4)
is decomposed clause by clause; each row names the mechanism in this repository
or in OpenRig, where it is configured, and the artifact a reviewer can open
without running anything. `docs/evidence/<mission>/INDEX.md` indexes the raw
exports against these rows.

| Clause | Mechanism | Configured in | Evidence |
|---|---|---|---|
| Explicit dependency graph with entry/exit gates | Per-slice `next_hop` routing graph (9 steps, `allowed_exits` per step, entry role) and mission-level `depends_on` DAG (8 steps); both validated before use | `rig/workflows/urlshort-slice*.workflow.yaml`, `project.yaml#lifecycle` | `docs/evidence/<m>/compiled-graph.json`, `rig workflow validate` output in `docs/evidence/<m>/` |
| Sequential and parallel paths with synchronization | Slices run as concurrent workflow instances inside a mission wave (fan-out); the `wave_integration` step waits on every slice's attributed proof (`--exit waiting --wait-for-proof`) before merging serially (fan-in). The engine keeps one live packet per instance; parallelism is across instances | `project.yaml` (`wave_integration`), wave-map queue row, `rig/agents/orchestration-lead/guidance/role.md` | `docs/evidence/<m>/instances/*.trace.json` (overlapping timestamps), wave-map packet |
| Cross-stage context and decision lineage | Transactional queue handoffs carry a chain of record; every step closes with a result note and `evidence_ref`; SPEC → design → PROOF chain files; ADRs; ambiguity log | protocol in `rig/startup/project.md` §3, role files | `docs/evidence/<m>/packets/*.transitions.json`, slice `SPEC.md` §Ambiguity log, `docs/adr/` |
| Human approval checkpoints for high-impact actions | Risk-tiered gate policy: mission plan-lock and ship sign-off always human; slice plan-lock human for `tier: high`, delegated to the orchestration lead for `tier: low`; ambiguity decisions human. Mechanism: the owning packet is parked on `human@kernel`, the human resolves with a recorded decision, the owner stamps on the human's behalf | `project.yaml` gates, `rig/workflows/*` `plan_lock`, `slice.yaml tier`, `rig/startup/project.md` §4 | `rig queue transitions` of the parked packets (decision text), approval stamps in SPEC frontmatter + append-only audit rows (`rig scope … approve`) |
| Bounded retries | `loop_guards.max_hops: 24` (9 steps + 3 remediation rounds × 4-hop loop) enforced by the engine; `failed` exits from QA / code review / security review route back to `implement`; `rig workflow resume --decision` grants exactly one more bounded window | `rig/workflows/*` | traces showing `failed → implement` hops; resume receipts |
| Fallback | `rig workflow route --to <seat>` re-owns a step when a seat is dead; `rig queue fallback`; exceptions route orchestrator-first | `exception_routing` in both specs, orchestration-lead role file | drill record in `docs/scenarios/drills.md`, route receipts in traces |
| Rollback | Slice branches merged `--no-ff` by one integrator; a red `main` after merge is reverted (`git revert -m 1`) and recorded; `rig workflow abort`; `rig snapshot`/`restore` for the rig; reversible Flyway migrations documented in each design | orchestration-lead role file (Integrate), design-agent role file | `git log` (revert commits), slice `PROOF.md` rollback notes, drill record |
| Safe-stop | `rig workflow abort --reason`, `rig queue block` (held with continuation + wake), `rig down --snapshot`, `rig mode set human-led` | `docs/SETUP-FACTORY.md` | abort receipts, snapshot ids in mission `NOTES.md` |
| Policy guardrails (security, compliance, change control) | `permission_policy: builtin:standard`; Claude allow/deny lists (deny `git push`, `reset --hard`, `clean -f`, `rm -rf`, `docker push`); Codex project rules (`git push` forbidden); authors never review their own work; cross-runtime QA/review; security review step with a shortener-specific checklist; `./gradlew check` 100 % coverage gate; no remote configured | `rig/rig.yaml`, `.claude/settings.json`, `.codex/rules/urlshort.rules`, `rig/CULTURE.md`, `build.gradle.kts` | `docs/review/<slice>/02-security-review.md`, `docs/qa/coverage/`, settings files themselves |
| Audit-grade observability and traceability | Daemon-persisted queue transitions and append-only workflow trails; transcripts per seat; attributed proof judgments; approval audit rows; evidence exported per mission | `tools/evidence-export.sh` | `docs/evidence/<m>/` (INDEX.md maps files to clauses) |
| Reliability metrics: success rate, retry/rollback frequency, MTTR, end-to-end latency | Derived from exported trails and transitions by `tools/sdlc-metrics.mjs` (OpenRig has no built-in rollup); token burn from `rig usage top` | `tools/sdlc-metrics.mjs` | `docs/metrics/metrics.json`, `docs/metrics/README.md` (derivations and limits) |
| Dynamic re-planning when upstream outputs change | `rig workflow revise --apply` adopts compatible graph changes without replaying completed steps; SPEC changes re-open the plan-lock; demonstrated in the ambiguous mission | orchestration-lead role file, mission 03 | revise receipts, before/after `compiled-graph.json` |
| Controlled agent autonomy | Humans own approvals and publishing; agents act under role contracts with explicit "never" lists; idle seats add no gates (proportionality) | `rig/CULTURE.md`, role files | gate records; absence of pushes/releases in `git log` |

## Gate policy

| Gate | Approver | Trigger |
|---|---|---|
| `mission_plan_lock` | human | every mission after `decompose` |
| slice `plan_lock` | human (`tier: high`) / orchestration lead on behalf of the human (`tier: low`) | after `design` |
| ambiguity decision | human | Requirements Agent parks a question with options and a recommended default |
| `slice_accept` | QA judgments (`rig proof judge`); delivery stamp written after ship sign-off | after `integrate` |
| `ship_signoff` | human | after `release_prep` |
| safe-stop | human or orchestration lead | any time |

## Known limits (honest scope)

- OpenRig 0.6.3's workflow runtime holds **one live packet per instance**; intra-instance fan-out is not claimed. Parallelism is across slice instances and across seats (pipeline overlap).
- The human registry supports only Slack bindings in this version; the human is reached through parked packets and Mission Control, not notifications.
- Codex seats run sandboxed without network; advisory-database dependency checks run at release prep on a Claude seat.
