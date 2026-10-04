# Risks, failure scenarios and guardrails

Living register. Each row names the scenario, how it would show, the guardrail
that prevents or contains it, and where the evidence lives. Items marked
*observed* happened during the missions and are linked to their record.

## 1. Orchestration risks

| Scenario | How it shows | Guardrail / response | Evidence |
|---|---|---|---|
| Unbounded remediation loop (builder and QA ping-pong forever) | hop count climbs; no merge | `loop_guards.max_hops: 30` sized from the graph (9 baseline steps + 2 requirements-review rounds × 2 hops + 2 design-review rounds × 2 hops + 3 remediation rounds × the 3-hop loop `implement → qa_check → code_review` = 26, rounded up to 30); a trip is an exception routed to the orchestration lead; `rig workflow resume --decision` grants exactly one more window | `rig/workflows/*`, traces in `docs/evidence/<m>/instances/` |
| Agent approves its own work | a seat judges a candidate it authored | authors never review (CULTURE §3); QA and review run on Codex, the builder on Claude; the Integrator requires three passing verdicts on the same SHA | `docs/review/REVIEW-LEDGER.md`, result notes in trails |
| Human gate bypassed | a stamp without a recorded decision | gates are engine-parked packets; stamps are written `--on-behalf-of human@kernel` only after the resolve row exists; both land in the append-only audit | `packets/*.transitions.json` (actor `human@kernel`), SPEC frontmatter stamps |
| Agent publishes or pushes | remote changes, tags, registries | a GitHub remote exists (D13), but only the human pushes; `git push` denied for Claude and `forbidden` for Codex; release agent prepares and stops at the gate | `.claude/settings.json`, `.codex/rules/urlshort.rules` |
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
| Short-code collisions and reserved paths (`/api`, `/actuator`) | 8 random characters from `[A-Za-z0-9]` drawn with `SecureRandom` (62⁸ codes); a draw equal to a reserved first segment (`api`, `actuator`, `v3`, `swagger-ui`, `error`) is drawn again; `UNIQUE (code)` makes a collision fail the insert closed (`500`, nothing stored, the client retries) (ADR-0007). Custom aliases were dropped (D7) | `ShortCodesTest`, `uq_link_code` in V1, ADR-0007 |
| Abuse / flooding | rate limiting with an explicit trusted-proxy rule for `X-Forwarded-For` | reliability slice tests |
| PII leakage in logs or analytics | IPs hashed with a rotating salt; no raw IP/UA in logs; audit rows carry no secrets | security review, log assertions in functional tests |
| Error responses leak internals | `ProblemDetail` everywhere, no stack traces | functional tests on error paths |
| Data loss on restart | file-backed H2 with Flyway-owned schema; graceful shutdown; container volume | smoke run, compose volume |
| Schema change breaks existing links (brownfield) | impact analysis with migration + rollback plan before plan-lock; Flyway applies migrations forward only, so each brownfield migration ships with a written rollback, rehearsed for V3 and V4 on a stopped data copy in mission 02 | `impact-analysis.md`, design ADRs, `missions/02-brownfield/RELEASE.md` §8 |
| *Observed:* a request still in flight through the host's published port is cut during `docker compose restart` on macOS Docker (Lima VM): the rest of the request never reaches the service, and isolation places the cut in the Mac-to-VM port forwarder (why it drops the connection, possibly because the server closes its listener at the start of the drain, is not established) | the service's 10 s graceful drain completes in-flight requests on every path that reaches it directly (the jar, Docker's own published port inside the VM, the container's network namespace); the human accepted the forwarder cut at mission 01's ship sign-off as a disclosed host gap, and AC-28 is measured on the direct paths. Not tested on a native Linux Docker host. To avoid it, restart when no request is in flight, or run on a Docker host whose port publish keeps established connections | `missions/01-greenfield-core/RELEASE.md` §3.3 and §3.6, `docs/qa/GAPS.md` (03-operate AC-28), ship decision `qitem-20261003165209-ce7abb0e` (transition 1011) |

## 3. Trade-offs accepted

| Trade-off | Why accepted |
|---|---|
| Embedded H2 instead of Postgres | zero-ops runnable prototype; the repository/Flyway seam makes a Postgres swap a brownfield candidate, not a rewrite |
| Two builder seats (`development-agent`, `dev2-agent`) | wave parallelism is across stages and slice instances; the second builder was added (D15) when two slices waited in `implement` on one seat |
| Operator approvals during the dry run | the first mission deliberately ran with the strictest posture to learn where the harness stops; every approval is audited and each root cause was fixed in the protocol |
| Evidence committed into the repo | larger repository, but the evaluator can audit without OpenRig |
| Single-node prototype (embedded H2 file, no replication, no HA) | accepted for the assignment; rate-limit state is in memory per instance (ADR-0014), so scaling out needs a shared limiter store and PostgreSQL, neither built (NFR-R4, `docs/ARCHITECTURE.md` §5) |

## 4. Assumptions

- The evaluator reads the repository and may run the product, but will not run OpenRig.
- One human decision-maker (the author) resolves mission plan-locks, ambiguity decisions and ship sign-offs; slice plan-locks after `01-ping` are delegated to the orchestration lead (D11).
- No network egress is needed by the product at runtime.
