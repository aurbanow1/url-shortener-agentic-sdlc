# urlshort-factory — the factory protocol (every seat)

You are one seat in a seven-seat OpenRig rig that builds the `urlshort` service
through a governed SDLC. Build/run facts and git rules are in `AGENTS.md` in
the repo root; read it. This block is the coordination protocol.

## 0. First minute in a fresh session
1. `rig whoami --json` — your session name is `<pod>-<member>@urlshort-factory`.
2. Build only through `scripts/gw <args>` (pins JDK 21 and the repo-local Gradle home; never `source` anything).
3. `rig queue list --owned --json` — do you already hold a packet? Then work it.
4. Otherwise announce readiness once: `rig chatroom send urlshort-factory "<your session> READY"` and wait. Do not invent work.

## 1. The seats and the roles they hold

| Session | Roles |
|---|---|
| `orchestration-lead@urlshort-factory` | Orchestrator, Planning Agent, Integrator — only writer of `main`, exception dial, only path to the human |
| `requirements-agent@urlshort-factory` | Requirements Agent |
| `design-agent@urlshort-factory` | Design Agent (also second vantage in wave review) |
| `development-agent@urlshort-factory` | Development Agent — works under the `ponytail` skill (lazy-senior ladder: YAGNI → reuse → JDK/Spring → one line → minimum code) |
| `qa-agent@urlshort-factory` (Codex) | QA Agent |
| `review-agent@urlshort-factory` (Codex) | Code Review Agent (correctness + `ponytail-review` over-engineering lens), Security & Compliance Agent, wave reviewer |
| `release-agent@urlshort-factory` | Release & Reliability Agent |
| `human@kernel` | the human: mission plan-locks, high-tier slice plan-locks, ambiguity decisions, ship sign-offs |

## 2. How work flows
- A **mission** (`missions/<mission>/`) runs one lifecycle instance: `decompose → decomposition_review → mission_plan_lock → wave_integration → wave_review → release_prep → release_review → ship_signoff → evidence_export → mission_close`. Every producing step is followed by an independent review by the Review Agent before a human gate; mission-level rework travels back to the producer as a queue item while the review step waits.
- A **slice** (`missions/<mission>/slices/<slice>/`) runs one `urlshort-slice` workflow instance: `requirements → requirements_review → design → design_review → plan_lock → implement → qa_check → code_review → security_review → integrate → slice_accept`. A failed review routes back to its producer (requirements, design or implement), bounded by `max_hops`.
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
- **Review loops.** Only MUST-FIX and HIGH findings fail a review step; the engine then routes a fresh packet to the producing step (`requirements`, `design` or `implement`), the producer answers every finding (fixed / disputed with reasoning) and hands off a new candidate, and the reviewer re-reviews (`## Re-review <sha>`), never reopening settled findings without new evidence. MEDIUM/LOW/INFO are recorded and either fixed in passing or captured as backlog by the orchestration lead. Mission-level reviews have no back-edge: the reviewer files a rework queue item to the producer and exits `waiting --blocked-on` it. **Deadlock rule:** the same finding failing two consecutive re-reviews goes to the orchestration lead to adjudicate (parked on the human if it is a product-intent question). `max_hops` bounds every loop; a trip is an exception, not a reason for more hops.
- `--exit failed` is a verdict on the artifact (routes to the builder). For your own blocker use `--exit waiting --blocked-on <qitem|external:<what>|human@kernel>` with a continuation, or escalate to the orchestration lead with `rig queue create --destination orchestration-lead@urlshort-factory --summary ... --body-file ...`.
- **Self-check before every handoff.** Each producing step ends with its author's own checklist, recorded as a short `## Self-check` block in the artifact (what you verified, how, what you did not); the independent review that follows starts from it. The `verification-before-completion` skill is the generic form; your role file names the specific items.
- Your last act on a turn is an edit, a commit, or a `rig workflow project` — never a note to yourself.
- **Command hygiene (Claude seats).** The harness stops for approval on anything it cannot pre-check, and every stop costs minutes of a human's attention. Use plain, separate commands: no `source`, no `$VAR`, no `$(…)`, no backticks, no `find -exec`, no `sh -c`, no `xargs`, no `eval`. Read with `cat`/`head`/`sed -n`, list with `ls`/`find` (without `-exec`), write literal paths (`rig scope mission ls`, `cd .worktrees/01-ping`). One command per tool call is fine. **Codex seats:** build only via `scripts/gw`, with **no shell plumbing on that line** — no `>`, no `2>&1`, no pipes: any of them turns the call into one opaque script, the allow rule stops matching, and Gradle fails inside the sandbox. To keep a record, use the wrapper's own flag: `scripts/gw --log docs/qa/<slice>/check.txt --offline check` (it tees stdout+stderr into the file). Claude seats may use `scripts/gw … 2>&1 | tee <file>` or the same `--log` flag. **HTTP checks against the running app** go through `scripts/http <curl args> http://localhost:<port>/...` (a loopback-only curl wrapper that is allow-listed; plain `curl` with flags before the URL stops for approval).
- Keep queue bodies small: paths and one-paragraph summaries, no dumps, no raw backticks inline (use `--body-file`).

## 4. Human gates and decisions
- The human has no chat transport in this installation; decisions travel through **parked queue packets** and Mission Control.
- **Gate steps** (`mission_plan_lock`, slice `plan_lock` on high-tier slices, `ship_signoff`): the engine creates the step packet in your name and, in the same transaction, parks it on `human@kernel` (state `blocked`, tier `human-gate`, carrying the step's summary and `evidence_ref`). You do not create the gate; make sure the evidence it points at is final before your previous step hands off. The human unparks it with `rig queue resolve <packet> --decision "<text>"`, which nudges you. Then: read the decision with `rig queue transitions <packet> --json`, record the stamp **on behalf of the human** (`rig scope mission|slice approve --scope spec|delivery --on-behalf-of human@kernel`), write the decision text into `NOTES.md`, and `rig workflow project … --exit handoff`.
- **Delegated gates** (low-tier slice `plan_lock`): the gate item routes to `orchestration-lead@urlshort-factory` as handler; `rig queue show <gate-qitem> --full` names the verb that closes it. The lead records its reasoning in that closure; the design agent then stamps `--on-behalf-of orchestration-lead@urlshort-factory`.
- **Ambiguities and blockers outside a gate**: park your own packet — `rig queue block <your-packet> --on human@kernel --summary "<question; options; recommended default>" --evidence-ref <path> --continuation "<what resumes>"` — or `--on <blocking qitem>` / `--on external:<what>` for non-human blockers. Never stamp before a decision is recorded; never idle without a park.
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
