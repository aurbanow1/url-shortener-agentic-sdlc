---
id: OPR.99.0.3
mission: 02-brownfield
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "Change the shipped shortener safely: add an operator-facing, loopback-only audit-read endpoint over the existing audit table (impact analysis first), purge clicks past the retention period, fix a defect found by using the service with a regression test first, and prove retry, rollback, fallback and safe-stop with recorded drills — without breaking existing links."
depends_on: ["OPR.99.0.2"]
---

# Mission — Brownfield: enhance and fix the shipped shortener

## Intent

Change the shipped shortener safely: add an operator-facing, loopback-only audit-read endpoint over the existing audit table (impact analysis first), purge clicks past the retention period, fix a defect found by using the service with a regression test first, and prove retry, rollback, fallback and safe-stop with recorded drills — without breaking existing links.

(Rewritten at decompose, 2026-10-03, as this SPEC said it would be. The text before the fast plan, "extend the shipped shortener with link expiry and custom aliases …", is superseded: FR-11 and FR-12 are dropped. The lifecycle instance's root objective still carries the old text; this SPEC is the current scope. The purge clause follows the D7 inputs below: NFR-P2 lands here because mission 01 did not deliver it.)

## The doghouse

On a shipped service whose existing links keep working, an Operator can read the audit trail from the machine itself and can rely on old clicks being deleted on schedule, a defect found by using the service is fixed test-first, and the factory's retry, rollback, fallback and safe-stop paths are shown working in labelled drills.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-13 … FR-15 (FR-13 applies to every slice through the impact analysis) and FR-17 (the audit-trail read, moved here from mission 01); NFR-S6, X2, and P2 if the purge was not delivered in mission 01. FR-11, FR-12 and NFR-S2 (custom alias, expiry) were dropped from the plan by the human's fast-plan decision of 2026-10-03 (`qitem-20261003052736-7830d02a`); the frontmatter intent predates that decision and is rewritten at this mission's `decompose`.

## Fast-plan decision (2026-10-03, recorded by the orchestration lead)

Mission 02 = the brownfield enhancement slice (`01-audit-read`, moved in from mission 01 with its scaffold; impact analysis over the shipped `audit_log` table before design) + one bug-fix slice (a dogfood defect, seeded and disclosed if none surfaces) + the fault-injection drills. Slice plan-locks are delegated to the orchestration lead; the mission plan-lock and ship sign-off stay with the human.

## Inputs (fast plan, D7)

- Scope: **two slices + drills** — (1) `04-audit-read`, moved here from mission 01: an Operator reads the audit trail through a read-only, paginated, loopback-only-by-default endpoint over the *shipped* `audit_log` table (FR-17; the brownfield demonstration: `impact-analysis.md` per `docs/guidance/brownfield.md` §2 before design — impacted modules, endpoints, schema, data flows, blast radius, compatibility of existing links FR-13); (2) one bug-fix slice (FR-14) for a defect found by using the shipped service — the dogfood pass QA runs at mission-01 release prep is the source; if nothing real surfaces, a seeded defect is used and disclosed as seeded in `docs/scenarios/brownfield.md`; plus the test/documentation improvements of FR-15 inside whichever slice touches them. Custom alias and expiry (FR-11/FR-12) are **dropped** (D7). NFR-X2 (versioned migrations with rollback) applies to any schema change; NFR-P2 (click purge) lands here if mission 01 did not deliver it.
- Shipped baseline: `main` after mission 01's wave 2 (`16c355f` create/redirect, `091ff46` analytics, `03-operate` once merged); read it, run it, then analyse.
- Fault-injection drills (release agent, `docs/scenarios/drills.md`, each labelled as a drill): QA rejects a candidate → remediation loop; integrator `git revert` after a failed installed smoke; `rig seat stop` → `workflow route`; `workflow abort` + `resume`.
- Gates: mission plan-lock and ship sign-off are the human's; slice plan-locks are delegated to the lead (D11). Guides: `brownfield.md`, `decomposition.md`, `orchestration.md` §4–§7, `release.md`.

## Allocation (every id to exactly one slice; cross-cutting ids noted)

| Slice | FR | NFR | Cross-cutting ids the slice must also honour |
|---|---|---|---|
| `01-audit-read` | FR-17 | S6 | FR-13 (impact analysis), X2 (if the pagination index is a migration), M1, M2, M3 (the audit endpoint in the API document), O1, O2 |
| `02-click-retention` | — | P2 | FR-13, X2 (if the purge needs an index), M1, M2, O1, O2 (the purge job's log events) |
| `03-dogfood-fix` | FR-14, FR-15 | — | FR-13, X2 (if the fix needs a migration), M1, M2 |

Count: 3 FR ids with a primary owner (FR-14, FR-15, FR-17), FR-13 on every slice; 2 NFR ids with a primary owner (S6, P2), X2 on every slice that changes the schema; M1, M2 on every slice. No slice without ids; no id without a slice.

## Decision brief (mission plan-lock)

**Outcome.** Three slices in two waves, plus four labelled drills. When this mission closes, `main` additionally carries a loopback-only, paginated audit-read endpoint, a scheduled purge of clicks older than the retention period, and one fixed defect with its regression test; existing links behave exactly as before (FR-13, checked by an impact analysis on every slice).

**Why three slices when D7's inputs say "two slices + drills".** The same inputs say NFR-P2 (the click purge) lands in this mission if mission 01 did not deliver it, and mission 01 did not (`02-analytics` built no purge). A purge is its own outcome (an Operator's data-retention guarantee), so folding it into the audit read or the bug fix would break the one-outcome rule. Alternative: move the purge to mission 03, which revisits retention at its ambiguity park anyway. Not recommended: it leaves click data growing without bound until mission 03 builds, and it couples a decided requirement (90 days) to an open question.

**Slices, in order.**

| # | Slice | Outcome | Tier | Tier reason | Depends on | Territory (prediction) |
|---|---|---|---|---|---|---|
| 1 | `01-audit-read` | An Operator can read the audit trail of every mutation (who, what, when, before, after, request id) through a read-only, paginated endpoint that is loopback-only by default. | low (fast plan) | delegated by the fast plan and D11; the lead's plan review weighs what §4 calls high: an audit read surface (NFR-S6) and a likely index migration | mission 01 (shipped `audit_log`, `16c355f`) | `audit/` (main, unit, functional), `db/migration/` (pagination index, first w1 holder), `docs/api/openapi.json`, `application.properties` (first w1 holder) |
| 2 | `02-click-retention` | An Operator can rely on clicks older than the retention period (90 days by default, an operator setting) being deleted on schedule. | low (fast plan) | delegated; the plan review weighs a job that deletes user data and a likely index migration (NFR-X2 rollback) | mission 01 (shipped `click` table, `091ff46`) | `click/` (main, unit, functional), `db/migration/` (second w1 holder), `application.properties` (second w1 holder) |
| 3 | `03-dogfood-fix` | A defect found by using the shipped service is fixed with a regression test written first, and the tests and documentation it touches match the shipped behaviour. | low (fast plan) | delegated; re-weighed at plan-lock once the defect is known | the dogfood report from mission 01's `release_prep` (external), and w1 merged | set at its `requirements` step from the defect report and adopted by `rig workflow revise`; runs alone in w2 |

**Waves and synchronisation** (wave map v1, `docs/evidence/02-brownfield/wave-map.md`).

| Wave | Slices | Why together / why alone | Sync point |
|---|---|---|---|
| `w1` | `01-audit-read` ∥ `02-click-retention` | no edge between them; disjoint feature packages (`audit/` vs `click/`); two shared items held in order: `application.properties` and the next Flyway version number, `01` first, `02` from `01`'s merge | **launches only after mission 01's `03-operate` is integrated** (it holds `application.properties` and `docs/api/openapi.json` until then; the lead holds both packets and sequences it), so both impact analyses read a `main` with every mission-01 change. `01` merges on its own verdicts; `02`'s candidate must descend from `01`'s merge (ancestry checked at integrate); `01` runs on `urlshort-slice-delegated` (`qa-agent`, `review-agent`), `02` on `urlshort-slice-delegated-b` (`qa2-agent`, `review2-agent`; its proof judge is set in its `slice.yaml`) |
| `w2` | `03-dogfood-fix` | its input is the dogfood report from mission 01's `release_prep`, and its territory is unknown until then, so it runs alone after w1 | launches when w1 is integrated; if the dogfood report is not there yet, its requirements step waits on it |

**Drills** (mission intent; run by the release agent, each labelled as a drill and recorded in `docs/scenarios/drills.md`; none of them touches a product slice's custody).

| Drill | Runs against | Triggered by | Recorded |
|---|---|---|---|
| QA rejects a candidate → remediation loop | a natural QA rejection on a mission-02 slice, if one occurs (00-hello had one; mission 01's loops were review rejections, not QA ones); otherwise a labelled drill instance (`rig workflow instantiate` with a "DRILL" root objective, no product code) whose candidate QA fails on purpose | release agent, at w1 launch | drills table, with the hop trail |
| Integrator `git revert` after a failed installed smoke | a throwaway branch and worktree off `main`, as the 00-hello rehearsal did; never `main` itself unless a real smoke fails | release agent, with the lead | drills table, gate log |
| `rig seat stop` → `rig workflow route` | the labelled drill instance | release agent and the lead (the route is the lead's dial) | drills table, `NOTES.md` dial record |
| `rig workflow abort` + `resume` | the labelled drill instance | the lead, at the release agent's request | drills table, `NOTES.md` |

The durable trigger for each is a queue item to `release-agent@urlshort-factory`, filed by the lead when w1 starts.

**Assumed rows this mission treats as decided.** None new. NFR-S6 (audit endpoint loopback-only by default) and NFR-P2's 90 days were decided at mission 01's plan-lock.

**Risks.**

- *Breaking existing links (FR-13).* Every slice changes shipped code. Mitigation: an impact analysis before each design (`brownfield.md`), regression journeys from mission 01 stay green, and the gate runs on `main` after every merge.
- *Two migrations in one wave.* `01` and `02` may both add an index. Mitigation: ordered custody of the next Flyway number (`01` first); `02` rebases and takes the next number; each migration carries a written rollback (NFR-X2).
- *Deleting the wrong clicks.* A purge is irreversible. Mitigation: the purge deletes by the stored UTC day only (`clicked_on`, ADR-0013), the period is a setting with the decided default, and functional tests prove the boundary day by day; the security review checks it.
- *Coupling with mission 03.* Mission 03 revisits retention at its ambiguity park. Mitigation: the purge's period is an operator setting, so a changed number is configuration; only a different mechanism (aggregate instead of delete) becomes mission-03 work. Its `01-analytics-v2` slice plan-lock also checks `main`'s migration head and `click/` against this mission.
- *No real defect surfaces.* Mitigation: a seeded defect, disclosed as seeded in `docs/scenarios/brownfield.md`, seeded by the release agent (not the lead, who merges it, and not the QA seat that judges it).
- *Drills disturbing real work.* Mitigation: the stop, route and abort drills run on a labelled drill instance, never on a product slice.

**Not in this mission.** Custom aliases and expiry (FR-11, FR-12, NFR-S2; dropped by the fast plan); authentication or access beyond loopback for the audit read (NFR-S6); analytics changes beyond the purge (mission 03); aggregation of old clicks instead of deletion (mission 03's question).

**Recommended default: approve.** Alternatives: move the purge to mission 03 (see above); or serialise w1 (`01`, then `02`) at the cost of the parallel design and review time that mission 01's wave 2 showed.

## Self-check (decompose, 2026-10-03)

- One buildable user outcome per slice: yes; audit read, click purge, and one defect fix, each with its failure paths inside the slice.
- Territories disjoint within a wave: `01` (`audit/`) and `02` (`click/`) share only `application.properties` and the Flyway version sequence, held in order with the reason; `03` runs alone in w2. w1 waits for mission 01's `03-operate`, which holds `application.properties` and `docs/api/openapi.json` until it merges.
- Every `depends_on` edge names a crossing artefact: no sibling edges; mission-level dependency on mission 01 (the shipped `audit_log` and `click` tables); `03`'s external input is mission 01's dogfood report.
- Tiers hold against §4 as written: all low by the human's fast plan, with what §4 would call high named in each `tier_reason` for the lead's plan review.
- Every in-scope id allocated exactly once: FR-14, FR-15, FR-17, S6, P2 with one owner each; FR-13 and X2 cross-cutting, noted.
- Compiled graph committed with `unknowns` empty; receipts in `NOTES.md`.
- Decision brief complete (§6): outcome, the slice-count reasoning, slices with tiers and territory, waves with sync points, drills, assumed rows, risks, not-in-mission, default and alternatives.
- Brownfield: every slice's manifest names its impact analysis before design.
- Not verified by me: whether a real defect surfaces at mission 01's dogfood pass; whether either index is needed (design decides); `03`'s territory, set at its requirements step.

## Slices

- `01-audit-read` (`OPR.99.0.3.1`) — Audit trail read. Low (fast plan). w1, first holder of `application.properties` and the next migration number. Scaffolded (moved from mission 01).
- `02-click-retention` (`OPR.99.0.3.2`) — Click retention purge. Low. w1, second holder; `-b` judges. Scaffolded.
- `03-dogfood-fix` (`OPR.99.0.3.3`) — Dogfood defect fix. Low. w2, alone. Scaffolded; territory set at requirements.

## Status

- 2026-10-03T11:51Z — lifecycle instance `01M40SN34E37K96B38JPG9K41X` created by the operator; decompose claimed by the orchestration lead.
- 2026-10-03 — decomposed into three slices in two waves with four labelled drills; intent rewritten (aliases and expiry dropped, purge added); handed to `decomposition_review`.

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `$OPENRIG_HOME/reference/sdlc-conventions.md`).
