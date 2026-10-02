---
id: OPR.99.0.3
mission: 02-brownfield
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "Extend the shipped shortener with link expiry and custom aliases and fix a defect found by using it, without breaking existing links; demonstrates codebase reasoning, migration with rollback, and the retry, rollback, fallback and safe-stop paths through labelled fault-injection drills."
depends_on: ["OPR.99.0.2"]
---

# Mission — Brownfield: enhance and fix the shipped shortener

## Intent

Extend the shipped shortener with link expiry and custom aliases and fix a defect found by using it, without breaking existing links; demonstrates codebase reasoning, migration with rollback, and the retry, rollback, fallback and safe-stop paths through labelled fault-injection drills.

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
