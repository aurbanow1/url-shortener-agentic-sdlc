---
mission: OPR.99.0.4
name: Ambiguous: marketing wants better analytics
created: 2026-10-02
---

# Notes — Ambiguous: marketing wants better analytics

Context and observations that help the mission but do not change its
`SPEC.md` contract or `PROGRESS.md` acceptance checklist belong here.

## 1. Top of mind

- Lifecycle instance: `01M40RVNDQ0KT7FPWN1KJW0DC3` (`lifecycle-urlshort-03-ambiguous-analytics`), operation key `urlshort-03-ambiguous-analytics-lifecycle-1`, created by `operator-human@kernel` 2026-10-03T11:37:14Z at the orchestration lead's request (`qitem-20261003094253-537ab7dc`, fast plan item 5: decompose in parallel once mission 01's wave 2 builds). Entry packet (decompose) `qitem-20261003113714-4cbdf1e0`, claimed 11:37:29Z by the orchestration lead.
- Slice: `01-analytics-v2` (`OPR.99.0.4.1`), tier high, wave w1, alone. Wave map v1: `docs/evidence/03-ambiguous-analytics/wave-map.md`, queue row `qitem-20261003114210-1c6a50a1` (closed as a composition record).
- Compiled graph: `docs/evidence/03-ambiguous-analytics/compiled-graph.json`, `unknowns` empty. Revision receipts: (1) `revision-7350d685c252fac429c78a11`, composition adopted, digest `3635bf63…` → `865d9b70…`; (2) `revision-f7dca08ae962cadb65ec94be`, manifest names the brownfield impact analysis, `865d9b70…` → `3bee4e79…`. Both source-only, frontier preserved.
- Human touches: mission plan-lock (after `decomposition_review`), then the ambiguity park at the slice's `requirements` step, then the ship sign-off. The slice plan-lock is the lead's (D11).
- Cross-mission coupling to watch: mission 02's NFR-P2 purge on `click/` and the next Flyway version number (mission SPEC, Risks).

## 2. Orchestration lead

- 2026-10-03T11:38Z — decompose. Read the mission SPEC (the fast plan, D7, fixes one slice), `docs/REQUIREMENTS.md` rows FR-8, FR-16, NFR-P2, O3, L1 and the §5 mission table, mission 02's SPEC (for the NFR-P2 overlap), `docs/guidance/decomposition.md` and `orchestration.md` in full. Shape: one slice decided through an ambiguity park, then built; re-planning declared in advance (new slices by `rig scope slice create` + `rig workflow revise` + wave map v2 if the answer needs more than one outcome).
- Mechanics worth keeping: `rig scope slice create <mission> <slug>` numbers the folder itself, so the slug must not carry the number. I passed `01-analytics-v2` and got `01-01-analytics-v2`; renamed the (uncommitted) folder, the `mission.yaml` ref and the SPEC `slice:` field by hand before anything was compiled; `rig scope slice ls` then reported `01-analytics-v2` with the same id `OPR.99.0.4.1`.
- `rig scope audit --mission 03-ambiguous-analytics`: two low advisories (placeholder mini-requirements and proof contract), filled at the slice's `requirements` step; no tier drift (`tier: high` in both the SPEC frontmatter and `slice.yaml`).
