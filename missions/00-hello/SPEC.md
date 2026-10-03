---
id: OPR.99.0.1
mission: 00-hello
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "Prove the factory end to end: one trivial endpoint travels every pipeline step and both human gates, leaving a complete evidence trail."
depends_on: []
approved-spec-by: orchestration-lead@urlshort-factory
approved-spec-at: 2026-10-02T22:02:45.361Z
provenance: transport:v1
---

# Mission — Hello: factory dry run

## Intent

Prove the factory end to end: one trivial endpoint travels every pipeline step and both human gates, leaving a complete evidence trail.

## The doghouse

A `GET /api/ping` endpoint that answers `200` with `{"status":"ok","time":"<ISO-8601 UTC>"}` and a request-id header, merged to `main` through every slice step and every human gate, so that the evidence trail (specs, design, proofs, reviews, traces, stamps) exists before any real feature is attempted.

Nothing beyond that endpoint is in scope. The product value is the proven pipeline, not the ping.

## Decision brief (mission plan-lock)

**Outcome.** One slice, one wave, one merge. When this mission closes, `main` carries `/api/ping` and the repository carries a complete, attributed record of how it got there: requirements, design, human plan-lock, TDD implementation, QA coverage proof, independent code review, security review, serial integration, proof acceptance, wave review, release preparation, human ship sign-off, evidence export.

**Slices, in order.**

| # | Slice | Outcome | Tier | Tier reason | Depends on |
|---|---|---|---|---|---|
| 1 | `01-ping` | `GET /api/ping` returns 200 with `{status, time}` and a request-id header, logged as structured JSON, 100% unit and functional coverage | high | dry run of the full pipeline including the human plan-lock gate; first slice to add a cross-cutting request-id filter (foundation) | none |

**Waves.** One wave, `w1`, containing `01-ping`. Recorded as a `wave-map` queue row (format `wave-map-v1`).

**Territory of `01-ping`.** `src/{main,test,functionalTest}/java/dev/urlshort/ping/` for the endpoint and `src/{main,test,functionalTest}/java/dev/urlshort/web/` for the request-id filter. The filter is cross-cutting (every request line must carry `requestId` per `AGENTS.md`) and does not exist yet, so this slice owns it. Being the only slice in the wave, the wider territory collides with nothing.

**Human gates on this mission.** The frontmatter intent says "both human gates", meaning the two mission-level gates. With the slice's high-tier plan-lock there are three human decisions in total, which matches the root objective of the running lifecycle instance:

1. mission plan-lock (this brief),
2. slice `01-ping` plan-lock (SPEC.md + design.md, high tier),
3. ship sign-off (against RELEASE.md).

**Risks.**

- Seat availability. At decompose time `rig ps --nodes` listed only the orchestration lead, design agent, development agent and QA agent. The requirements, review and release seats were not in the inventory. The slice instance will wait at `requirements` until that seat is up, and `wave_review`, `code_review`, `security_review` and `release_prep` depend on the other two. This is a runtime risk, not a scope risk.
- Request-id filter scope creep. The filter should be the smallest thing that puts `requestId` on the MDC and the response header. The design step must not grow it into a tracing framework.
- Structured logging already configured (`logging.structured.format.console=ecs`). The slice should verify, not re-implement, that MDC fields reach the log line.

**Recommended default.** Approve. The decomposition is the smallest that exercises every step and gate once.

## Self-check (decompose)

Added 2026-10-02T22:47Z under the protocol update of 22:45Z, after the plan-lock stamp; it records checks that were already true at the decompose handoff and changes no contract content.

- One buildable user outcome per slice: yes, `01-ping` is one endpoint with its filter.
- Disjoint territories: yes, trivially, one slice in the wave.
- Tier per slice with reason: yes, `01-ping` high, reason in `slice.yaml` and the brief.
- Waves consistent with `depends_on`: yes, one wave, no edges.
- Risks named: yes, seat availability, filter scope creep, logging already configured.
- Doghouse stated: yes, in §The doghouse.

## Self-check (wave_integration)

Recorded 2026-10-03T01:52Z before the handoff to `wave_review`.

- Every slice of every wave accepted: yes, wave w1 = `01-ping`, slice instance `01M3ZA8Q39QEB3R1QDQCVTER18` completed with `done` (QA accept, 7/7 proof-contract items, readiness ready).
- Three independent verdicts on one SHA before merge: yes, QA, code review and security review all named `f286a10863e4a8081235226f2d56e51ac121b319`.
- Serial `--no-ff` merge, gate green on main, tag, worktree removed: yes, merge `42a25db4a9c24fba3221c1ade4044719cab39ee3`, `scripts/gw --offline check` green on main (log under `slices/01-ping/proof/`), tag `slice/01-ping/accepted`, `.worktrees/01-ping` gone.
- Reverts needed: none.
- Human decisions honoured: mission plan-lock (one slice, one wave, minimal filter) and slice plan-lock (SPEC `4e581cc` + design `d0521de`); neither locked artifact edited after its stamp.
- Exceptions handled and recorded: two bounded loops (design review DR-01, QA-01), one permission-prompt stall, two territory grants and one lead decision, all in `NOTES.md` §2.
- Open items carried forward: unit-suite properties shadowing (backlog); release-time network advisory checks (review agent's note for release). The ADR-0004 / `docs/DESIGN.md` thread-name example reconciliation closed at `23f7a8c`.
- Wave review outcome (2026-10-03T02:01Z): PASS from both vantages on `42a25db4`; two LOW follow-ups, W1-01 (OpenAPI export ownership, to the first mission-01 API slice) and W1-02 (real-server journey for log-privacy criteria), recorded as backlog in `NOTES.md` §2.

## Slices

- `01-ping` — Ping endpoint. Tier high. Wave w1. State: **accepted and merged** (`42a25db4`, tag `slice/01-ping/accepted`); proof contract 7/7 judged by QA; delivery stamp follows ship sign-off.

## Status

- 2026-10-02 — decomposed into one slice, one wave; compiled graph exported to `docs/evidence/00-hello/compiled-graph.json`; held at mission plan-lock.
- 2026-10-02T22:02Z — mission plan-lock approved; wave w1 launched.
- 2026-10-03T01:33Z — `01-ping` merged into main; 01:49Z slice accepted. Wave w1 complete.
- 2026-10-03T01:52Z — handed to `wave_review` (review agent + design agent, authors excluded). Wave range on main: `f43ecd1..42a25db4` for product code (nine files), plus the slice and mission documents committed on main since `adfa5ca`.

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `~/.openrig/reference/sdlc-conventions.md`).
