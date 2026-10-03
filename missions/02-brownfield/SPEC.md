---
id: OPR.99.0.3
mission: 02-brownfield
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "Change the shipped shortener safely: add an operator-facing, loopback-only audit-read endpoint over the existing audit table (impact analysis first), fix a defect found by using the service with a regression test first, and prove retry, rollback, fallback and safe-stop with recorded drills — without breaking existing links."
depends_on: ["OPR.99.0.2"]
---

# Mission — Brownfield: enhance and fix the shipped shortener

## Intent

Extend the shipped shortener with link expiry and custom aliases and fix a defect found by using it, without breaking existing links; demonstrates codebase reasoning, migration with rollback, and the retry, rollback, fallback and safe-stop paths through labelled fault-injection drills.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-13 … FR-15 (FR-13 applies to every slice through the impact analysis) and FR-17 (the audit-trail read, moved here from mission 01); NFR-S6, X2, and P2 if the purge was not delivered in mission 01. FR-11, FR-12 and NFR-S2 (custom alias, expiry) were dropped from the plan by the human's fast-plan decision of 2026-10-03 (`qitem-20261003052736-7830d02a`); the frontmatter intent predates that decision and is rewritten at this mission's `decompose`.

## Fast-plan decision (2026-10-03, recorded by the orchestration lead)

Mission 02 = the brownfield enhancement slice (`01-audit-read`, moved in from mission 01 with its scaffold; impact analysis over the shipped `audit_log` table before design) + one bug-fix slice (a dogfood defect, seeded and disclosed if none surfaces) + the fault-injection drills. Slice plan-locks are delegated to the orchestration lead; the mission plan-lock and ship sign-off stay with the human.

## Inputs (fast plan, D7)

- Scope: **two slices + drills** — (1) `04-audit-read`, moved here from mission 01: an Operator reads the audit trail through a read-only, paginated, loopback-only-by-default endpoint over the *shipped* `audit_log` table (FR-17; the brownfield demonstration: `impact-analysis.md` per `docs/guidance/brownfield.md` §2 before design — impacted modules, endpoints, schema, data flows, blast radius, compatibility of existing links FR-13); (2) one bug-fix slice (FR-14) for a defect found by using the shipped service — the dogfood pass QA runs at mission-01 release prep is the source; if nothing real surfaces, a seeded defect is used and disclosed as seeded in `docs/scenarios/brownfield.md`; plus the test/documentation improvements of FR-15 inside whichever slice touches them. Custom alias and expiry (FR-11/FR-12) are **dropped** (D7). NFR-X2 (versioned migrations with rollback) applies to any schema change; NFR-P2 (click purge) lands here if mission 01 did not deliver it.
- Shipped baseline: `main` after mission 01's wave 2 (`16c355f` create/redirect, `091ff46` analytics, `03-operate` once merged); read it, run it, then analyse.
- Fault-injection drills (release agent, `docs/scenarios/drills.md`, each labelled as a drill): QA rejects a candidate → remediation loop; integrator `git revert` after a failed installed smoke; `rig seat stop` → `workflow route`; `workflow abort` + `resume`.
- Gates: mission plan-lock and ship sign-off are the human's; slice plan-locks are delegated to the lead (D11). Guides: `brownfield.md`, `decomposition.md`, `orchestration.md` §4–§7, `release.md`.

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
