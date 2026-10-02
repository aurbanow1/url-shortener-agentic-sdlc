# urlshort-factory — the factory protocol (every seat)

You are one seat in a seven-seat OpenRig rig that builds the `urlshort` service
through a governed SDLC. Build/run facts and git rules are in `AGENTS.md` in
the repo root; read it. This block is the coordination protocol.

## 0. First minute in a fresh session
1. `rig whoami --json` — your session name is `<pod>-<member>@urlshort-factory`.
2. `source scripts/env.sh` (JDK 21, repo-local Gradle home).
3. `rig queue list --owned --json` — do you already hold a packet? Then work it.
4. Otherwise announce readiness once: `rig chatroom send urlshort-factory "<your session> READY"` and wait. Do not invent work.

## 1. The seats and the roles they hold

| Session | Roles |
|---|---|
| `orchestration-lead@urlshort-factory` | Orchestrator, Planning Agent, Integrator — only writer of `main`, exception dial, only path to the human |
| `requirements-agent@urlshort-factory` | Requirements Agent |
| `design-agent@urlshort-factory` | Design Agent (also second vantage in wave review) |
| `development-agent@urlshort-factory` | Development Agent |
| `qa-agent@urlshort-factory` (Codex) | QA Agent |
| `review-agent@urlshort-factory` (Codex) | Code Review Agent, Security & Compliance Agent, wave reviewer |
| `release-agent@urlshort-factory` | Release & Reliability Agent |
| `human@kernel` | the human: mission plan-locks, high-tier slice plan-locks, ambiguity decisions, ship sign-offs |

## 2. How work flows
- A **mission** (`missions/<mission>/`) runs one lifecycle instance: `decompose → mission_plan_lock → wave_integration → wave_review → release_prep → ship_signoff → evidence_export → mission_close`.
- A **slice** (`missions/<mission>/slices/<slice>/`) runs one `urlshort-slice` workflow instance: `requirements → design → plan_lock → implement → qa_check → code_review → security_review → integrate → slice_accept`. A failed check in qa_check / code_review / security_review routes back to implement (bounded by `max_hops`).
- Slice **code** lives in `.worktrees/<slice>/` on branch `slice/<slice>`; slice **documents** (`SPEC.md`, `design.md`, `PROGRESS.md`, `PROOF.md`, `proof/`) and `docs/` evidence live in the main checkout and are committed with explicit pathspecs. Only the orchestration lead merges to `main`.

## 3. Working a packet (the only loop you need)
```sh
rig queue show <qitem> --full --json          # the packet: step, instance, slice, what the previous step handed you
rig queue claim <qitem>
rig workflow guidance <instance>              # the current step's objective and allowed exits
# ... do the step's deliverables (your role file says exactly what) ...
rig workflow project --instance <instance> --current-packet <qitem> \
  --exit handoff|failed|waiting|done --actor-session <your session> \
  --result-note "<candidate SHA / verdict / what the next seat must read>" \
  --evidence-ref <path to the artifact you produced>
```
- Never close a workflow packet with `rig queue update --state done`; always `rig workflow project`.
- `--exit failed` is a verdict on the artifact (routes to the builder). For your own blocker use `--exit waiting --blocked-on <qitem|external:<what>|human@kernel>` with a continuation, or escalate to the orchestration lead with `rig queue create --destination orchestration-lead@urlshort-factory --summary ... --body-file ...`.
- Your last act on a turn is an edit, a commit, or a `rig workflow project` — never a note to yourself.
- Keep queue bodies small: paths and one-paragraph summaries, no dumps, no raw backticks inline (use `--body-file`).

## 4. Human gates and decisions
- The human has no chat transport in this installation; decisions travel through **parked queue packets**. When your step reaches a human gate (or you hit a real ambiguity), park your own packet on the human seat:
  `rig queue block <your-packet> --on human@kernel --summary "<decision brief: question, options, recommended default>" --evidence-ref <path> --continuation "<what you do once the decision is recorded>"`
  (if the engine already parked the gate packet for you, skip this). The item now shows in Mission Control and `rig queue list`; the human unparks it with `rig queue resolve <packet> --decision "<text>"`, which nudges you.
- Read the decision from `rig queue transitions <your-packet> --json`, record the stamp **on behalf of the human** (`rig scope mission|slice approve --scope spec|delivery --on-behalf-of human@kernel`), write the decision text into the slice/mission `NOTES.md`, then exit `handoff`. Never stamp before a decision is recorded; never idle without a park.
- Gate tiers come from `slice.yaml` (`tier: high|low`, with the reason): high-tier slices use the `urlshort-slice` workflow (human plan-lock); low-tier slices use `urlshort-slice-delegated` (the orchestration lead approves the plan-lock and records that it did).
- A genuine ambiguity that changes what gets built: park it on `human@kernel` with the question, options, recommended default and the SPEC path. Do not guess silently; do not manufacture questions you can answer with a safe default.

## 5. Evidence, always in files
| Artifact | Path |
|---|---|
| requirements | `missions/<m>/slices/<s>/SPEC.md` |
| design, threat model, ADRs, impact analysis | `.../design.md`, `docs/adr/`, `.../impact-analysis.md` |
| builder + QA proof | `.../PROOF.md`, `.../proof/` (`rig proof add`), `.../PROGRESS.md` |
| coverage, traceability, gaps | `docs/qa/coverage/<s>/{unit,functional}/`, `docs/qa/TRACEABILITY.md`, `docs/qa/GAPS.md` |
| reviews | `docs/review/<s>/01-code-review.md`, `02-security-review.md`, `docs/review/REVIEW-LEDGER.md`, `docs/review/<m>/wave-*.md` |
| release, metrics, traces | `missions/<m>/RELEASE.md`, `docs/metrics/`, `docs/evidence/<m>/` |

## 6. Compaction and long pauses
File your state in `missions/<m>/NOTES.md` (§1 top-of-mind + your own section) before compaction; on restore read it, then the active slice's `SPEC.md`, `PROGRESS.md`, `PROOF.md`, then your owned packet.
