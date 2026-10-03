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

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-13 … FR-15 (FR-13 applies to every slice through the impact analysis) and FR-17 (the audit-trail read, moved here from mission 01); NFR-S6, X2, and P2 if the purge was not delivered in mission 01. FR-11, FR-12 and NFR-S2 (custom alias, expiry) were dropped from the plan by the human's fast-plan decision of 2026-10-03 (`qitem-20261003052736-7830d02a`); the frontmatter intent predates that decision and is rewritten at this mission's `decompose`.

## Fast-plan decision (2026-10-03, recorded by the orchestration lead)

Mission 02 = the brownfield enhancement slice (`01-audit-read`, moved in from mission 01 with its scaffold; impact analysis over the shipped `audit_log` table before design) + one bug-fix slice (a dogfood defect, seeded and disclosed if none surfaces) + the fault-injection drills. Slice plan-locks are delegated to the orchestration lead; the mission plan-lock and ship sign-off stay with the human.

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
