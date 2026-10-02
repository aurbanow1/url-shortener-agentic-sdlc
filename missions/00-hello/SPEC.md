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

## Slices

- `01-ping` — Ping endpoint. Tier high. Wave w1. State: scaffolded, awaiting mission plan-lock.

## Status

- 2026-10-02 — decomposed into one slice, one wave; compiled graph exported to `docs/evidence/00-hello/compiled-graph.json`; held at mission plan-lock.

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `~/.openrig/reference/sdlc-conventions.md`).
