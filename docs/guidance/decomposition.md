# Decomposition guidance

How a mission brief becomes slices, waves, tiers and a dependency graph that
the engine can run and a reviewer can judge. Read by the orchestration lead at
`decompose`, by the Review Agent at `decomposition_review`, and by the human
before a mission plan-lock. Every rule has the reason and the check.

## 1. The unit: a slice is one buildable user outcome

- A slice delivers something a persona can do end to end (`POST /api/links` →
  `201` → `GET /{code}` → `302`), proven by effect through the public surface.
  *Reason:* outcomes are testable and reviewable; layers ("the repository
  slice", "the DTO slice") are not. *Check:* the slice intent is one sentence a
  Visitor, Creator, Analyst or Operator would recognise.
- Smallest slice that completes the outcome, including its failure paths
  (invalid input, not found, conflict). *Reason:* half an outcome cannot be
  accepted; a failure path left for later is a defect shipped. *Check:* the
  requirement ids allocated to the slice (`docs/REQUIREMENTS.md`) are all
  reachable from its intent.
- Size ceiling: one builder, one or two days, roughly ≤ 3 endpoints or one
  migration plus the code that uses it. Larger → split by outcome, never by
  layer. *Check:* the territory list fits on one screen.

## 2. Shaping rules

1. **Foundations first.** The first slice of a mission carries the schema,
   the error contract, logging/audit wiring and the first journey; later
   slices add outcomes, not infrastructure.
2. **Vertical, not horizontal.** Each slice touches controller + service +
   repository + migration + tests for *its* outcome.
3. **Disjoint territories.** Two slices that may run concurrently never share
   a file; shared files (`build.gradle.kts`, `application.properties`,
   `docs/DESIGN.md`) are granted to one slice at a time, listed in
   `slice.yaml`, with the grant's reason.
4. **Brownfield slices carry an impact analysis** (`docs/guidance/brownfield.md`)
   before design; their territory is derived from the analysis, not guessed.
5. **Name by outcome** (`01-create-redirect`, `04-expiry-alias`), never by
   component (`02-service-layer`).

## 3. Dependencies, waves and synchronisation

- `depends_on` names a real artefact dependency (slice B needs A's table,
  endpoint or contract); never "A is more important". *Check:* for each edge
  you can name the file or contract that crosses it.
- A **wave** is a set of slices with no dependency edges between them; they run
  as concurrent slice instances on disjoint territories. The mission's
  `wave_integration` step is the synchronisation point: it waits for every
  slice instance in the wave to reach `slice_accept` before `wave_review`.
- Keep waves small (2–3 slices): review and QA capacity is one seat each; a
  wide wave queues on the reviewer, not on the builder.
- `rig workflow compile missions/<mission> --json` renders the graph; every
  entry under `unknowns` is resolved before plan-lock. The compiled graph is
  committed as evidence (`docs/evidence/<mission>/compiled-graph.json`).

## 4. Tiers and gates

| Tier | When | Plan-lock |
|---|---|---|
| `high` | foundation slice, schema migration, new security-relevant surface (redirect target, alias, rate limit, audit read), ambiguous scope, anything the human asked to see | human (`urlshort-slice`) |
| `low` | additive outcome on an existing foundation, no migration, no new trust boundary | delegated to the orchestration lead, recorded (`urlshort-slice-delegated`) |

`tier_reason` is written in `slice.yaml`; the reviewer checks the reason, not
the label.

## 5. Requirement allocation

Every `FR-n`/`NFR-n` in the mission's `## Requirements in scope` is allocated to
exactly one slice (cross-cutting ids such as FR-13 to each slice they touch),
written as a slice → ids table in the mission `SPEC.md`. An id with no slice is
a gap to explain; a slice with no ids is machinery. The slice SPEC cites the
ids; QA traceability carries them to tests.

## 6. The decision brief (mission plan-lock)

The human reads one page: outcome in one sentence · slices in order with
intent, tier + reason, territory, requirement ids · waves and the sync points ·
the `assumed` requirement rows that this mission will treat as decided ·
risks with the mitigation · what is explicitly not in this mission ·
recommended default ("approve") and the alternative. Evidence ref = the
mission `SPEC.md`.

## 7. Re-planning (when upstream outputs change)

- A SPEC change after its plan-lock is the human's decision (parked packet
  with options), never a silent edit.
- A new finding that changes the graph (a slice must split, a dependency
  appears) goes through `rig workflow revise`; the engine refuses changes to
  completed steps — then the answer is a **new slice**, not a rewrite of
  history. Record the receipt (accepted or refused) in
  `docs/evidence/<mission>/` and the reasoning in `NOTES.md`.
- A failed check inside a slice is not a re-plan: it is the bounded loop
  (`docs/guidance/orchestration.md` §3).

## 8. Anti-patterns

Layer slices · a "misc"/"hardening" slice that collects leftovers · a slice
whose territory is "everything under `src/`" · dependencies drawn to force an
order rather than to express a need · a wave wider than the review capacity ·
tiers chosen to avoid a gate · requirement ids allocated to no slice.

## 9. `decomposition_review` checklist (fail = HIGH)

1. One buildable user outcome per slice; failure paths inside the slice.
2. Territories disjoint within a wave; shared-file grants explicit with reasons.
3. Every `depends_on` edge names a crossing artefact; waves have no internal edges.
4. Tier reasons hold against §4; high-tier slices use the human plan-lock workflow.
5. Every in-scope requirement id allocated exactly once (cross-cutting noted); no orphan slice.
6. Compiled graph committed, `unknowns` empty.
7. Decision brief complete (§6) and honest about `assumed` rows and risks.
8. Brownfield slices have an impact analysis before design.
