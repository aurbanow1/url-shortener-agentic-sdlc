---
id: OPR.99.0.4
mission: 03-ambiguous-analytics
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "Marketing says the analytics are not good enough; turn that ambiguous ask into a decided, built and proven improvement by surfacing the real questions (what is counted, retention, privacy, who reads it) to the human before building, and re-plan when the answer changes the design."
depends_on: ["OPR.99.0.2"]
---

# Mission — Ambiguous: marketing wants better analytics

## Intent

Marketing says the analytics are not good enough; turn that ambiguous ask into a decided, built and proven improvement by surfacing the real questions (what is counted, retention, privacy, who reads it) to the human before building, and re-plan when the answer changes the design.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-16 and FR-8 (v2); NFR-P2 revisited, O3 (new metrics), L1 if the redirect path changes. Decisions come out of the slice's ambiguity log and the human decision gate.

## Inputs (fast plan, D7)

- Scope: **one slice** — analytics v2 decided through an ambiguity log and a human decision gate, then built. Requirements in scope per `docs/REQUIREMENTS.md` §5: FR-16, FR-8 (v2); NFR-P2 (retention, revisited), NFR-O3 (new metrics), NFR-L1 if the redirect path changes.
- The shipped v1 to improve on: `missions/01-greenfield-core/slices/02-analytics/{SPEC.md,design.md}` (click events, per-link totals, per-day counts, top referrers; privacy-safe salted hashing; merged as `091ff46`).
- The ambiguous ask, verbatim from the mission intent: "marketing says the analytics are not good enough". The Requirements Agent must surface the real questions — what is counted (raw vs unique clicks, bots), retention, privacy (what the hashed address may be used for), who reads it (API vs report vs dashboard), time zones — as an ambiguity log with options, a recommended default and the consequence of each, and **park the decision on the human** (`rig queue block … --on human@kernel`) rather than widen scope.
- Dynamic re-planning is the point of this mission: after the human decides, the plan (SPEC, possibly the slice shape) is revised and the change recorded (`rig workflow revise` receipt or a new slice), per `docs/guidance/decomposition.md` §7 and `docs/guidance/orchestration.md` §7.
- Delegated slice plan-lock (D11); the mission plan-lock and the ship sign-off stay with the human. Guides: `docs/guidance/requirements.md` §3, `decomposition.md`, `orchestration.md`, `review.md`, `release.md`.

## Slices

[List notable slices and their state]

## Status

[Where the mission is right now]

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `$OPENRIG_HOME/reference/sdlc-conventions.md`).
