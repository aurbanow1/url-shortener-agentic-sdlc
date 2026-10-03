---
id: OPR.99.0.2
mission: 01-greenfield-core
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "A user can create a short link for a valid http(s) URL, be redirected through it, and read its click analytics, from a service that is observable, rate-limited and audited; built from scratch as the greenfield scenario."
depends_on: []
---

# Mission — Greenfield: core URL shortener

## Intent

A user can create a short link for a valid http(s) URL, be redirected through it, and read its click analytics, from a service that is observable, rate-limited and audited; built from scratch as the greenfield scenario.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-1 … FR-10, FR-17; NFR-L1–L3, R1–R6, S1, S3–S6, P1, O1–O3, A1–A2, M1–M3, X1. The `assumed` rows among these are confirmed or changed at this mission's plan-lock.

## Inputs carried from 00-hello

- Ordered backlog from the dry run (`missions/00-hello/NOTES.md`, "Backlog carried into mission 01"): 1. dependency overrides first — Tomcat embed 11.0.25, Jackson 3.1.7 / 2.21.7 as version overrides in `build.gradle.kts`, gate + fresh OSV run in QA (advisory packet `qitem-20261003021640-bc2477ef`); 2. OpenAPI export ownership (W1-01) in the first API slice; 3. one real-server functional journey where an AC constrains log content (W1-02); 4. unit-suite properties overlay (`src/test/resources/application.properties` shadows the shipped file).
- Product baseline: `docs/REQUIREMENTS.md` — the 8 `assumed` rows in scope are confirmed or changed at this mission's plan-lock; the decision brief lists them explicitly.
- Guidance that applies to this mission for the first time: `docs/guidance/decomposition.md` (slices, waves, tiers, allocation, the plan-lock brief), `docs/guidance/orchestration.md`, `docs/guidance/review.md`, `docs/guidance/release.md`.

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
