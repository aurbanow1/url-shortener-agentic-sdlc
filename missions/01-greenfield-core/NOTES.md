---
mission: OPR.99.0.2
name: Greenfield: core URL shortener
created: 2026-10-02
---

# Notes — Greenfield: core URL shortener

Context and observations that help the mission but do not change its
`SPEC.md` contract or `PROGRESS.md` acceptance checklist belong here.

## 1. Top of mind

- Lifecycle instance: `01M3ZXEXAMS945ZS0QZ26KZK1V` (`lifecycle-urlshort-01-greenfield-core`), lifecycle operation key `urlshort-01-greenfield-core-lifecycle-1`, created by `operator-human@kernel` 2026-10-03T03:38:25Z on the ten-step authored graph (`decompose → decomposition_review → mission_plan_lock → wave_integration → wave_review → release_prep → release_review → ship_signoff → evidence_export → mission_close`).
- Entry packet (decompose): `qitem-20261003033825-0148d3da`, owner orchestration lead, claimed 03:38:35Z.
- Slices (dot-ids): `01-create-redirect` `OPR.99.0.2.1` (w1, high) · `02-analytics` `OPR.99.0.2.2` (w2, high) · `03-operate` `OPR.99.0.2.3` (w2, high) · `04-audit-read` `OPR.99.0.2.4` (w3, high). `02`, `03`, `04` depend on `01`; `04` is `SOFT-AFTER` `02`/`03` (shared-file grants), not a hard edge.
- Wave map row: `qitem-20261003040319-a45c400a` (tags `wave-map`, `format:wave-map-v1`, `mission:01-greenfield-core`), body in `docs/evidence/01-greenfield-core/wave-map.md`; closed `done` / `no-follow-on` as a composition record.
- Compiled graph: `docs/evidence/01-greenfield-core/compiled-graph.json`, bound version `1-960eb9c69dfaa4ce` (digest `960eb9c6…c8c729e`), `unknowns` empty when compiled with the lifecycle operation key; the per-slice "No authored proof contract" readiness issue is the placeholder SPECs and is filled at each slice's `requirements` step.
- Revision receipt: `rig workflow revise 01M3ZXEXAMS945ZS0QZ26KZK1V --apply` under `revision-b45cc7524d5587e0f212a2fb` at 04:03:49Z, instance version 1 → 2, previous digest `87f06000…` (empty composition) → `960eb9c6…`, source-only (executable steps and policy unchanged), frontier `qitem-20261003033825-0148d3da` preserved, no replay. Recorded so the compiled evidence and the running instance name the same version.
- Next: `decomposition_review` by `review-agent@urlshort-factory` (up and idle at 04:10Z), then the human `mission_plan_lock` with `SPEC.md` as evidence (three decisions: the eight `assumed` rows, the dependency-override placement, FR-17 in this mission).
- Seat inventory: at decompose claim (03:38Z) the orchestration lead, design, development and QA seats were up and the requirements, review and release seats absent (named as a runtime risk in the brief); by 04:10Z all seven seats were up and idle, so the handoff moves immediately.

## 2. Orchestration lead

- 2026-10-03T03:38Z — decompose claimed. Read `docs/guidance/decomposition.md`, `orchestration.md`, `requirements.md` §1–2, `architecture.md` §1–2, `docs/REQUIREMENTS.md`, the mission brief, the 00-hello mission (SPEC, NOTES, `01-ping/slice.yaml`) as the worked example, and `PLAN.md` §5 (planned shape: `01 → {02 ∥ 03}`).
- Shape chosen and why. PLAN.md's three slices became four: FR-17 (audit read) cannot share a wave with `02-analytics` without two slices regenerating `docs/api/openapi.json` and holding the same `application.properties` grant, and folding it into `03` would put two security surfaces behind one plan-lock and make `03` the "hardening grab-bag" the guide forbids. So `04-audit-read` runs alone in w3, with deferral to mission 02 (pre-authorised in `docs/REQUIREMENTS.md`) as the brief's alternative. FR-9 (idempotency) moved from PLAN.md's "reliability" into `01`: it is the create contract, and keeping it out of w2 keeps `link/` and the API document to one slice there (`02`, for its redirect hook and stats endpoint). `03-operate` carries FR-10 plus the Operator-facing NFRs; its `slice.yaml` separates what it builds and proves in-suite from what it only configures for release-level proof, so the requirements agent is not pushed into un-provable criteria.
- Tiers: all four high under `docs/guidance/decomposition.md` §4 (foundation + open-redirect surface; migration + privacy storage; rate limit + container privileges; audit read + loopback-only default). PLAN.md expected `02`/`03` delegated, but the §4 table written since then names migration, rate limit and audit read as high. Six human decisions on this mission as a result (stated in the brief); the human can lower a tier at the plan-lock and I apply it with `rig workflow revise`.
- Dependency overrides (00-hello ship condition, `qitem-20261003023502-f807af1f`): placed as the first, separately gated commit of `01-create-redirect`, not a standalone slice (a slice with no requirement id is machinery per the guide, and `01` is the first slice that parses client input). Put to the human as decision 2 with the standalone-slice alternative, because it reinterprets a recorded human decision.
- Mission's own shaping rule: one API-document-changing slice per wave; `03` must not touch `docs/api/openapi.json`; `01`'s design makes the export deterministic and key-sorted; a conflicting merge is aborted and the slice routed back to `implement` for a rebase and regeneration. Reason: a generated file regenerated on two branches can merge cleanly into a document that no longer matches the running service, and the integrator may not resolve conflicts by hand.
- Mechanics worth keeping: `rig scope slice create --depends-on <dot-id>` writes `depends_on` into the SPEC frontmatter only; `slice.yaml` is the bare scaffold and `metadata.id`, `tier`, `tier_reason`, `territory`, `execution.{actor_role,depends_on}` are authored by hand on the 01-ping pattern; `execution.depends_on` takes slice ids (`01-create-redirect`) and the compiler resolves them (`readiness.slices[].dependsOn`). `rig scope slice create` also appends the `composition.slices` entries to `mission.yaml`. The compile's only `unknowns` entry without `--operation-key` is the replay identity; passing the instance's lifecycle operation key clears it and the digest matches the bound version either way. `rig scope audit --mission 01-greenfield-core` at decompose: 8 low advisories, all placeholder-SPEC content for the requirements step; no tier drift (the `tier:` frontmatter was added to each slice SPEC to match `slice.yaml`).
- Operator note 03:55Z: do not re-derive the slice schema from the CLI source; `rig workflow compile` is the oracle. Followed.

## 3. Design agent

## 4. Development agent

## 5. Release agent

## QA Agent
