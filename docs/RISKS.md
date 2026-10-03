# Risks, failure scenarios and guardrails

Living register. Each row names the scenario, how it would show, the guardrail
that prevents or contains it, and where the evidence lives. Items marked
*observed* happened during the missions and are linked to their record.

## 1. Orchestration risks

| Scenario | How it shows | Guardrail / response | Evidence |
|---|---|---|---|
| Unbounded remediation loop (builder and QA ping-pong forever) | hop count climbs; no merge | `loop_guards.max_hops: 24` sized from the graph (9 steps + 3 rounds × 4-hop loop); a trip is an exception routed to the orchestration lead; `rig workflow resume --decision` grants exactly one more window | `rig/workflows/*`, traces in `docs/evidence/<m>/instances/` |
| Agent approves its own work | a seat judges a candidate it authored | authors never review (CULTURE §3); QA and review run on Codex, the builder on Claude; the Integrator requires three passing verdicts on the same SHA | `docs/review/REVIEW-LEDGER.md`, result notes in trails |
| Human gate bypassed | a stamp without a recorded decision | gates are engine-parked packets; stamps are written `--on-behalf-of human@kernel` only after the resolve row exists; both land in the append-only audit | `packets/*.transitions.json` (actor `human@kernel`), SPEC frontmatter stamps |
| Agent publishes or pushes | remote changes, tags, registries | no remote configured; `git push` denied for Claude and `forbidden` for Codex; release agent prepares and stops at the gate | `.claude/settings.json`, `.codex/rules/urlshort.rules` |
| Seat stalls on a prompt and work silently stops | `rig ps --nodes` shows `needs-input`; packet stays in-progress | command-hygiene rule in the protocol; allow-lists; operator approvals are audited `rig send --dangerously-interact --reason`; watchdog parked-owner wakes | *observed* on day 0 (several times); `docs/SETUP-FACTORY.md` |
| Context loss on compaction mid-slice | a seat forgets its packet | state filed in mission `NOTES.md` before compaction; packets and files are the source of truth, not chat; compaction-restore skill | `missions/<m>/NOTES.md` |
| Dead seat holding the frontier | packet in-progress, no activity | `rig workflow route --to <seat>` re-owns the step; `rig queue fallback`; drill in mission 02 | `docs/scenarios/drills.md` |
| Plan drifts from intent | locally defensible steps accrete into something nobody asked for | plan-lock freezes SPEC + design; wave review checks drift explicitly and disposes misses as CONTEXT-GAP / JUDGMENT-GAP | `docs/review/<m>/wave-*.md` |
| Upstream output changes after downstream work started | stale design or decomposition | `rig workflow revise --apply` adopts compatible changes without replaying completed steps; the plan-lock is reopened when the SPEC changes | *observed*: lifecycle revision v2 on `00-hello` after the territory changed |
| Tool-chain failure in a sandboxed seat | Codex cannot run Gradle | `scripts/gw` allow-listed to run outside the sandbox; offline builds from a pre-warmed cache | *observed and fixed* on day 0 |
| Metrics look perfect because nothing failed | all-green dashboard | mission 02 runs labelled fault-injection drills; natural failures are recorded, never erased | `docs/scenarios/drills.md`, `docs/metrics/` |

## 2. Product risks (URL shortener)

| Scenario | Guardrail | Where proven |
|---|---|---|
| Open redirect / phishing via crafted targets | only stored, validated `http(s)` URLs are redirected; `javascript:`, `data:`, `file:` rejected at creation; security review checklist | SPEC AC for invalid URLs, `02-security-review.md` |
| Alias collisions and reserved paths (`/api`, `/actuator`) | alias charset, length and reserved-word rules; 409 on duplicates | functional tests per AC |
| Abuse / flooding | rate limiting with an explicit trusted-proxy rule for `X-Forwarded-For` | reliability slice tests |
| PII leakage in logs or analytics | IPs hashed with a rotating salt; no raw IP/UA in logs; audit rows carry no secrets | security review, log assertions in functional tests |
| Error responses leak internals | `ProblemDetail` everywhere, no stack traces | functional tests on error paths |
| Data loss on restart | file-backed H2 with Flyway-owned schema; graceful shutdown; container volume | smoke run, compose volume |
| Schema change breaks existing links (brownfield) | impact analysis with migration + rollback plan before plan-lock; reversible migrations | `impact-analysis.md`, design ADRs |

## 3. Trade-offs accepted

| Trade-off | Why accepted |
|---|---|
| Embedded H2 instead of Postgres | zero-ops runnable prototype; the repository/Flyway seam makes a Postgres swap a brownfield candidate, not a rewrite |
| One builder seat | wave parallelism is across stages and slice instances; a second builder is a `rig grow` away when implementation becomes the bottleneck |
| Operator approvals during the dry run | the first mission deliberately ran with the strictest posture to learn where the harness stops; every approval is audited and each root cause was fixed in the protocol |
| Evidence committed into the repo | larger repository, but the evaluator can audit without OpenRig |

## 4. Assumptions

- The evaluator reads the repository and may run the product, but will not run OpenRig.
- One human decision-maker (the author) resolves all gates.
- No network egress is needed by the product at runtime.

| Single-node prototype (embedded H2 file, no replication, no HA) | accepted for the assignment; the design keeps all state in the database so the move to PostgreSQL + N stateless instances is a deployment change (NFR-R4, `docs/ARCHITECTURE.md` §5) | operator |
