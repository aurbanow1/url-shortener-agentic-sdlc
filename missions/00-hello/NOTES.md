---
mission: OPR.99.0.1
name: Hello: factory dry run
created: 2026-10-02
---

# Notes — Hello: factory dry run

Context and observations that help the mission but do not change its
`SPEC.md` contract or `PROGRESS.md` acceptance checklist belong here.

## 1. Top of mind

- Lifecycle instance: `01M3Z8AJ1EDFTHQ1HPAWPNP2YN` (`lifecycle-urlshort-00-hello`), operation key `hello-run-1`, created by `operator-human@kernel` 2026-10-02T21:29Z.
- Entry packet (decompose): `qitem-20261002212902-0f6e128b`, owner orchestration lead.
- Wave map row: `qitem-20261002213604-5d55a7ba` (tags `wave-map`, `format:wave-map-v1`, `mission:00-hello`): one wave `w1` = [`01-ping`].
- Compiled graph: `docs/evidence/00-hello/compiled-graph.json` (bound version `1-acb61bc08740b27d`).
- Slice instance ids: none yet (launched at `wave_integration`).
- Current step after decompose: `mission_plan_lock`, packet `qitem-20261002213817-6d848ac6` (mine), parked on `human@kernel`, evidence `missions/00-hello/SPEC.md` §Decision brief. Decompose closed at commit `adfa5ca`.

## 2. Orchestration lead

- 2026-10-02 — decompose. The scaffold already held `slices/01-ping/slice.yaml` (tier high) and the `mission.yaml` composition entry. Widened the slice territory to include `src/{main,test,functionalTest}/java/dev/urlshort/web/` so the cross-cutting request-id filter has a home outside `ping/`; adopted the change on the running instance with `rig workflow revise 01M3Z8AJ1EDFTHQ1HPAWPNP2YN --apply` (revision `revision-06cf30eaa295267ec7327a05`, instance version 1 → 2, digest `08fe06…` → `acb61b…`). Wrote the plan-lock decision brief into the mission `SPEC.md`.
- Seat inventory at decompose: orchestration lead, design, development, QA present; requirements, review, release seats absent from `rig ps --nodes`. Named as a runtime risk in the brief.

## Notes

- 2026-10-02 — mission scaffolded.
