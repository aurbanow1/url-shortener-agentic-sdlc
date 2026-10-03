---
id: OPR.99.0.3.2
slice: 02-click-retention
mission: 02-brownfield
status: placeholder
stage: wip
tier: low
verified: 2026-10-03 against scaffold (rig scope create)
created: 2026-10-03
intent: "An Operator can rely on clicks older than the retention period (90 days by default, an operator setting) being deleted on schedule, so click data stops growing without bound and no visit is kept longer than decided."
depends_on: []
---

# Slice 02 — Click Retention

## Intent

An Operator can rely on clicks older than the retention period (90 days by default, an operator setting) being deleted on schedule, so click data stops growing without bound and no visit is kept longer than decided.

## Mini-requirements

1. [The concise one-glance requirement tier — numbered observable outcomes. For a small slice this may BE the whole plan.]

## Proof contract

- [ ] [One promised deliverable, written as an observable outcome — captured. Each item pairs with its proof via `rig proof add … --evidences` (media attached with `--media`); UI deliverables name their planned mockup.]

## Source material

- [Paths or refs]

## Intent visual

Non-visual slices: mark this section N/A.

- Intent image: ![Intent visual](./intent.png)
- Durable diff: [change.diff](./change.diff)
- Regenerate preview: from `packages/ui`, run `TWIN_ROUTE=<route> npm run twin:build` to rebuild `twin-out/intent.html` (gitignored).

## Status

- TODO: [next steps]

## Dependencies

- [Cross-slice / cross-release]

---

> **How you work this slice (SOP):** conventions SSOT: `docs/reference/sdlc-conventions.md` (installed: `$OPENRIG_HOME/reference/sdlc-conventions.md`) — read its COMPONENT MENU first: your mission chooses the build path (the simple default flow · the wave model · the assigned rigorous overlay) and the planning rigor (the P0–P4 dial); do not assume the heavy flow unless your mission or dispatch assigns it. Full flow for the default path: the `mission-slice-sop` skill. The floor on every path: track on PROGRESS.md; evidence lands via `rig proof add` (never hand-placed); a slice is **not done** until its promised outcomes have evidence; verify with `rig scope audit`.
