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

## The doghouse

The human tells us what "better analytics" means (what is counted, how long clicks are kept, what the hashed address may be used for, who reads the figures) before anything is built, and an Analyst then gets exactly that, built and proven on top of the shipped v1.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-16 and FR-8 (v2); NFR-P2 revisited, O3 (new metrics), L1 if the redirect path changes. Decisions come out of the slice's ambiguity log and the human decision gate.

### Allocation (every id to exactly one slice)

| Slice | FR | NFR | Cross-cutting ids the slice must also honour |
|---|---|---|---|
| `01-analytics-v2` | FR-16, FR-8 (v2) | P2 (retention, revisited), O3 (new metrics), L1 (only if the redirect path changes) | M1, M2, M3 (the statistics operation in the committed API document), O1, O2 |

Count: 2 FR and 3 NFR ids in scope, all on the one slice; M1, M2 cross-cutting as on every slice; M3, O1, O2 inherited because the slice changes a public endpoint. NFR-L1 is conditional by the requirements table itself: it applies only if the decided design touches the redirect path, and the slice's SPEC says which.

## Inputs (fast plan, D7)

- Scope: **one slice** — analytics v2 decided through an ambiguity log and a human decision gate, then built. Requirements in scope per `docs/REQUIREMENTS.md` §5: FR-16, FR-8 (v2); NFR-P2 (retention, revisited), NFR-O3 (new metrics), NFR-L1 if the redirect path changes.
- The shipped v1 to improve on: `missions/01-greenfield-core/slices/02-analytics/{SPEC.md,design.md}` (click events, per-link totals, per-day counts, top referrers; privacy-safe salted hashing; merged as `091ff46`).
- The ambiguous ask, verbatim from the mission intent: "marketing says the analytics are not good enough". The Requirements Agent must surface the real questions — what is counted (raw vs unique clicks, bots), retention, privacy (what the hashed address may be used for), who reads it (API vs report vs dashboard), time zones — as an ambiguity log with options, a recommended default and the consequence of each, and **park the decision on the human** (`rig queue block … --on human@kernel`) rather than widen scope.
- Dynamic re-planning is the point of this mission: after the human decides, the plan (SPEC, possibly the slice shape) is revised and the change recorded (`rig workflow revise` receipt or a new slice), per `docs/guidance/decomposition.md` §7 and `docs/guidance/orchestration.md` §7.
- Delegated slice plan-lock (D11); the mission plan-lock and the ship sign-off stay with the human. Guides: `docs/guidance/requirements.md` §3, `decomposition.md`, `orchestration.md`, `review.md`, `release.md`.

## Decision brief (mission plan-lock)

**Outcome.** One slice in one wave. Approving this plan launches `01-analytics-v2`; its first step writes the ambiguity log and parks the analytics questions on you. You approve the *shape* here, not the answers: the answers are yours at that park, and nothing is designed or built before they are recorded (FR-16).

**Two human touches, in order.**

1. This mission plan-lock (now): approve one slice whose first act is to ask you.
2. The ambiguity park at the slice's `requirements` step (soon after approval): the Requirements Agent lists the real questions with options, a recommended default and the consequence of each, at least: what is counted (raw vs unique clicks, bots), retention (keep 90 days, change it, aggregate old clicks), privacy (what the daily-salted client hash may be used for: nothing, uniques within a day, more), who reads the figures (the API, a report, a dashboard) and time zones. Your answer is recorded verbatim and the SPEC follows it.

The slice plan-lock after design is the orchestration lead's (D11); the ship sign-off is yours.

**Slices.**

| # | Slice | Outcome | Tier | Tier reason | Depends on | Territory (prediction; design may request grants) |
|---|---|---|---|---|---|---|
| 1 | `01-analytics-v2` | An Analyst gets the click analytics marketing actually needs, with what is counted, how long clicks are kept, what the hashed address may be used for and who reads the figures decided by the human before anything is built, then built and proven on top of the shipped v1. | high | ambiguous scope by design (`decomposition.md` §4); the answer may add a migration and changes what the privacy-sensitive hash may be used for. The human checkpoint is the ambiguity park; the plan-lock handler is the lead (D11) | mission 01 (shipped v1: click table, redirect hook, statistics endpoint, merged `091ff46`); no sibling | `click/` (main, unit, functional), `db/migration/` (only if stored data changes), `docs/api/openapi.json`, `docs/diagrams/erd.mmd`, `application.properties` (only for a new operator setting); **not** `link/`, `web/`, `audit/` |

**Waves.** `w1` = `01-analytics-v2` alone (wave map v1, `docs/evidence/03-ambiguous-analytics/wave-map.md`, queue row `qitem-20261003114210-1c6a50a1`). `wave_integration` launches it on `rig/workflows/urlshort-slice.workflow.yaml` and hands off to `wave_review` when it is integrated.

**Re-planning, declared in advance.** The decision lands before the slice's plan-lock, so a SPEC change is the normal requirements loop. If the decided scope is more than one buildable outcome (for example uniques *and* a report *and* a retention change), the lead adds slices (`rig scope slice create` + `rig workflow revise` + wave map v2), records the receipt in `docs/evidence/03-ambiguous-analytics/` and the reasoning in `NOTES.md`; a structural change after this plan-lock comes back to you as a parked decision (`decomposition.md` §7).

**Assumed rows this mission treats as decided.** None. FR-8's shape beyond the three v1 figures is `assumed` in `docs/REQUIREMENTS.md`, and that assumption is exactly what the ambiguity park asks you to decide. NFR-P2's 90 days is `decided`; mission 03 may change it only through your answer.

**Risks.**

- *Cross-mission coupling with mission 02.* Mission 02 builds the NFR-P2 90-day purge on `click/` (it was not delivered in mission 01). If both missions build at once, they share `click/` and the next Flyway version number. Mitigation: this slice's plan-lock checks `main`'s migration head and any in-flight mission-02 territory; the later plan-lock takes the next free `V` number and the later `implement` rebases onto `main`. If your retention answer here changes NFR-P2, it feeds mission 02's purge if that is not yet built; the lead routes it.
- *Scope creep from an open question.* "Better analytics" can grow into a dashboard product. Mitigation: the ambiguity log offers narrow defaults with their consequences, and anything you do not choose stays out; extra outcomes become extra slices, each with its own review.
- *Privacy regression.* Using the client hash for uniques changes what the daily salt protects. Mitigation: the privacy question is explicit in the park; the design and security reviews check NFR-P1 again; the salt never leaves memory (ADR-0012) unless you decide otherwise.
- *Waiting on the human.* The slice is parked until you answer. That is a safe state, not idle work; wave 2 of mission 01 and mission 02 continue meanwhile.

**Not in this mission.** Custom aliases and expiry (FR-11, FR-12, dropped); the NFR-P2 purge build itself (mission 02's, unless your answer moves it); a dashboard or report, unless you pick that reader at the park; changes to the redirect, link management or audit.

**Recommended default: approve.** Alternative: fold the analytics questions into mission 02's plan-lock and build analytics v2 there (fewer gates, but it couples a product decision to a brownfield mission and delays both).

## Self-check (decompose, 2026-10-03)

- One buildable user outcome per slice: yes; one slice, as the mission SPEC fixes it (D7), whose outcome is decided before it is built.
- Territories disjoint within the wave: trivially (one slice); the cross-mission overlap with mission 02 is named as a risk with a mitigation.
- Every `depends_on` edge names a crossing artefact: the only dependency is mission-level (mission 01's merged v1: click table, redirect hook, statistics endpoint); no sibling edges.
- Tier reason holds against §4: high for ambiguous scope; the human checkpoint is the ambiguity park, the plan-lock handler is the lead by D11, and both are written in `slice.yaml`.
- Every in-scope id allocated exactly once: FR-16, FR-8 v2, P2, O3, L1 (conditional) on `01-analytics-v2`; cross-cutting M1, M2 and inherited M3, O1, O2 noted.
- Compiled graph committed with `unknowns` empty (`docs/evidence/03-ambiguous-analytics/compiled-graph.json`, compiled with the lifecycle operation key); revision receipts `revision-7350d685c252fac429c78a11` (composition adopted) and `revision-f7dca08ae962cadb65ec94be` (manifest names the impact analysis), both source-only, frontier kept. Its one readiness issue, "No authored proof contract", is the placeholder slice SPEC, filled at `requirements`, as for mission 01's slices; `rig scope audit` reports the same two low advisories and no tier drift.
- Decision brief complete (§6): outcome, slices, waves, tier with reason, assumed rows (none), risks, not-in-mission, default and alternative.
- Brownfield impact analysis: the slice changes shipped code (`click/`), so per `brownfield.md` its design step carries an impact analysis over v1 (`02-analytics`'s design and ADR-0011…0013).
- Not verified by me: the size of the decided scope (it does not exist yet); the territory is a prediction that design may extend by grant.

## Slices

- `01-analytics-v2` (`OPR.99.0.4.1`) — Analytics v2. Tier high (ambiguous scope; plan-lock handler the lead, D11). Wave w1. State: scaffolded; SPEC written at its `requirements` step after the mission plan-lock.

## Status

- 2026-10-03T11:37Z — lifecycle instance `01M40RVNDQ0KT7FPWN1KJW0DC3` created by the operator (fast plan item 5: decompose in parallel with mission 01's wave 2); decompose claimed by the orchestration lead.
- 2026-10-03 — decomposed into one slice in one wave as the mission SPEC fixes it; compiled graph exported; composition adopted (receipts `revision-7350d685c252fac429c78a11`, `revision-f7dca08ae962cadb65ec94be`); handed to `decomposition_review`.

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `$OPENRIG_HOME/reference/sdlc-conventions.md`).
