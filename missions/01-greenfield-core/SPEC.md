---
id: OPR.99.0.2
mission: 01-greenfield-core
stage: wip
verified: 2026-10-02 against scaffold (rig scope create)
created: 2026-10-02
intent: "A user can create a short link for a valid http(s) URL, be redirected through it, and read its click analytics, from a service that is observable, rate-limited and audited; built from scratch as the greenfield scenario."
depends_on: []
approved-spec-by: orchestration-lead@urlshort-factory
approved-spec-at: 2026-10-03T05:28:26.648Z
provenance: transport:v1
approved-spec-priors: 1
---

# Mission — Greenfield: core URL shortener

## Intent

A user can create a short link for a valid http(s) URL, be redirected through it, and read its click analytics, from a service that is observable, rate-limited and audited; built from scratch as the greenfield scenario.

## The doghouse

A Creator posts a valid http(s) URL and gets a short link; a Visitor who opens it is redirected with a `302` and the click is recorded without slowing them; an Analyst reads the counts; an Operator sees abuse answered with `429`, truthful readiness, metrics, and every mutation written to an audit trail, all from one container on one H2 file. Reading that trail (FR-17) is mission 02's enhancement slice.

Nothing on the out-of-scope list of `docs/REQUIREMENTS.md` §4 is in this mission. Custom aliases and expiry (FR-11, FR-12, NFR-S2) are dropped from the plan by the human's fast-plan decision of 2026-10-03; the audit read and the retention purge are mission 02, "better analytics" is mission 03. (Historical: before the fast plan this mission also promised a readable audit trail, and aliases and expiry were planned for later missions.)

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-1 … FR-10; NFR-L1–L3, R1–R6, S1, S3–S5, P1, O1–O3, A1–A2, M1–M3, X1. The `assumed` rows among these were confirmed at this mission's plan-lock (listed in the decision brief below). FR-17 and NFR-S6 were in scope until the human's fast-plan decision of 2026-10-03 moved them to mission 02 (Status, 05:30Z).

### Allocation (every id to exactly one slice; cross-cutting ids noted)

| Slice | FR | NFR | Cross-cutting ids this slice must also honour |
|---|---|---|---|
| `01-create-redirect` | FR-1, FR-2, FR-3, FR-4, FR-5, FR-6, FR-9 | S1, S3, S4, R5, R6, A1, A2, M3, O1, O2 | M1, M2 |
| `02-analytics` | FR-7, FR-8 | P1, L3 | M1, M2, M3 (the stats endpoint joins the committed API document), O1, O2 (request id and structured log on the new endpoint) |
| `03-operate` | FR-10 | R1, R2, R3, R4, S5, O3, X1, L1, L2 | M1, M2, M3 (the `429` problem-detail response, the `Retry-After` header and examples join the committed API document on every operation) |
| (`04-audit-read`) | FR-17 | S6 | moved to mission 02 as `01-audit-read` by the fast plan (2026-10-03); listed for the record |

Notes on the allocation:

- **M1** (100 % line and branch coverage, honest gaps) and **M2** (an ADR before the code that depends on a cross-cutting decision) apply to every slice; they are listed once as cross-cutting and owned by no single slice.
- **O1** and **O2** were proven on `00-hello`; they are allocated to `01-create-redirect`, which re-proves them on the first real endpoints, and every later endpoint-adding slice inherits the obligation.
- **M3** (committed OpenAPI document): `01-create-redirect` owns the export mechanism and the first document (backlog W1-01); `02-analytics` (stats endpoint) and `03-operate` (the `429` response with `Retry-After` and examples, since a filter-produced response is still public API) extend it, one holder of the file at a time (shaping rule 5 below); the audit endpoint's entry comes with mission 02.
- **`03-operate` has two kinds of ids.** It builds and proves in-suite: FR-10, R2 (at the limit, one over, spoofed `X-Forwarded-For`), R1 (readiness group includes the database), O3 (metric names and the rejection counter readable through the actuator). It owns the configuration, `Dockerfile`, `compose.yaml` and `scripts/smoke.sh` lines for R3, R4, S5, X1, L1 and L2, whose proofs are release-level per the proof column of `docs/REQUIREMENTS.md` (installed smoke, `docker inspect`, restart loop, bench). What the slice cannot prove in-suite it records in `docs/qa/GAPS.md`; it does not claim it.
- **R5** (idempotency keys honoured for 24 h) sits with FR-9 in `01-create-redirect`.
- Count (after the fast plan): 10 FR ids (FR-1…FR-10) and 23 NFR ids (L1–L3, R1–R6, S1, S3–S5, P1, O1–O3, A1–A2, M1–M3, X1) in scope, 33 in all; 21 NFR ids sit in a slice column and M1, M2 are cross-cutting on every slice; all allocated; no slice without ids. At the plan-lock the count was 11 FR and 24 NFR, 35 in all, with FR-17 and S6 on `04-audit-read`.

## Inputs carried from 00-hello

- Ordered backlog from the dry run (`missions/00-hello/NOTES.md`, "Backlog carried into mission 01"), and where each item landed:
  1. dependency overrides first (Tomcat embed 11.0.25, Jackson 3.1.7 / 2.21.7 as version overrides in `build.gradle.kts`, gate + fresh OSV run; advisory packet `qitem-20261003021640-bc2477ef`) → the **first commit of `01-create-redirect`**, gated green on its own before any feature commit (decision item 2 in the brief);
  2. OpenAPI export ownership (W1-01) → `01-create-redirect`;
  3. one real-server functional journey where an AC constrains log content (W1-02) → `02-analytics` (NFR-P1 constrains log content), weighed at its design step;
  4. unit-suite properties overlay (`src/test/resources/application.properties` shadows the shipped file) → optional in-passing fix granted to `01-create-redirect`'s territory; not an acceptance criterion.
- Product baseline: `docs/REQUIREMENTS.md`; the `assumed` rows are confirmed or changed at this mission's plan-lock (brief, decision item 1).
- Guidance that applies to this mission for the first time: `docs/guidance/decomposition.md`, `docs/guidance/orchestration.md`, `docs/guidance/review.md`, `docs/guidance/release.md`.
- **Javadoc policy** (human decision 2026-10-03 after the plan-lock, `docs/guidance/java-spring.md` §8, operator packet `qitem-20261003051852-2c3bd470`): the build enforces Javadoc on `src/main/java` (`javadoc` with `-Xdoclint:all -Werror`, `check` depends on it); code review judges that the text says what the signature does not. Routed into `01-create-redirect` as its second gated commit (after the dependency overrides, before feature code), with Javadoc-only grants on `ping/` and `UrlshortApplication.java`; `02`–`04` inherit the gate. No SPEC or allocation change: the guide binds the builder, and the obligation rides with NFR-M1's gate.

## Decision brief (mission plan-lock)

**Outcome.** Three slices over two waves (since the fast plan of 2026-10-03). When this mission closes, `main` carries the public API (`POST /api/links`, `GET`/`DELETE /api/links/{code}`, `GET /{code}`, `GET /api/links/{code}/stats`), a `link`, `click` and `audit_log` schema owned by Flyway, problem details on every error, a request id and a structured log line on every request, a rate limiter, truthful readiness, metrics, and a hardened container; every mutation writes an audit row, and the endpoint to read it comes with mission 02. (Historical: the brief approved at the 04:39Z plan-lock said four slices over three waves, with an audit read endpoint in this mission.) Each slice arrives through requirements, design, their reviews, a plan-lock, test-first implementation, QA with coverage proof, code review and security review on one candidate SHA, serial integration and attributed proof acceptance.

**Slices, in order.**

| # | Slice | Outcome (one sentence) | Tier | Tier reason | Depends on | Territory (summary; full list in `slice.yaml`) |
|---|---|---|---|---|---|---|
| 1 | `01-create-redirect` | A Creator can create, read and retire a short link for a valid http(s) URL, and a Visitor who opens it is redirected (`302`) or told it is gone (`410`), with every mutation audited, every error a problem detail, and a retried create with the same `Idempotency-Key` returning the first link. | high | foundation: first migration (`link`, `audit_log`), error contract, audit wiring, OpenAPI export; new security-relevant surface (the target allow-list is the open-redirect boundary); carries the dependency overrides | none | `link/`, `audit/` (write side), `web/` (error advice), `db/migration/` (V1), `docs/api/openapi.json` (new), `build.gradle.kts`, `application.properties`, test property overlays |
| 2 | `02-analytics` | An Analyst can read a link's click statistics (total, per day, top referrers) because every redirect records a privacy-safe click event without slowing the Visitor. | low (fast plan; briefed as high) | plan-lock delegated to the lead by the human's fast-plan decision of 2026-10-03; the lead's own plan review weighs the additive migration (`click` table) and the NFR-P1 storage design the guide would have called high | `01` (the `link` table and code lookup; the redirect path it hooks; the API document it extends) | `click/`, `db/migration/` (V2), `link/` **grant limited to the redirect hook**, `docs/api/openapi.json` (w2 grant), `docs/diagrams/erd.mmd` |
| 3 | `03-operate` | An Operator can run urlshort in production shape: clients above the rate limit are answered `429` with `Retry-After` and a counted rejection, readiness reflects the database, metrics are exposed for scraping, and the container runs non-root with durable data on a loopback-published port. | low (fast plan; briefed as high) | plan-lock delegated to the lead by the human's fast-plan decision of 2026-10-03; the lead's own plan review weighs the rate limiter's trusted-proxy rule and the container privilege changes the guide would have called high; no migration | `01` (the endpoints it limits; the problem-detail contract it reuses for `429`) | `web/` (the filter beside the request-id filter), `application.properties` (w2 grant), `build.gradle.kts` (w2 grant, Prometheus registry only), `Dockerfile`, `compose.yaml`, `scripts/smoke.sh`, `docs/api/openapi.json` (w2 ordered grant, second holder after `02` merges); **not** `link/`, `click/`, `db/migration/` |
| (4) | `04-audit-read` | moved to mission 02 as its brownfield enhancement slice `01-audit-read` by the human's fast plan (2026-10-03); it was wave w3 of this mission with FR-17 and NFR-S6 | — | — | — | — |

Every slice is a vertical through controller, service, repository, migration and both test suites for its own outcome; none is a layer. Slice 1 is one endpoint over the guide's "roughly ≤ 3 endpoints" ceiling: `GET /api/links/{code}` is a read of the same row, `DELETE` is required inside the slice because `410`-on-retired is a failure path of the redirect, and `Idempotency-Key` is the create contract (putting it in a later slice would re-open `link/` and the API document in a wave that already has one contract-changing slice). The requirements agent keeps each endpoint's criteria small; if the design review judges the slice too large, the split is FR-3/FR-4 off by outcome, applied with `rig workflow revise`, never by layer.

**Waves and synchronisation.** Recorded as the `wave-map` queue row (format `wave-map-v1`, body in `docs/evidence/01-greenfield-core/wave-map.md`).

| Wave | Slices | Why together / why alone | Sync point |
|---|---|---|---|
| `w1` | `01-create-redirect` | the foundation everything else depends on | `01` merges at its own `integrate` step once its three verdicts name one SHA; its proof is judged at `slice_accept` against the merge; `wave_integration` launches `w2` when `01` is accepted |
| `w2` | `02-analytics` ∥ `03-operate` | no edge between them; disjoint territories at any moment (table below); both extend the API document, so that file is an ordered grant, `02` then `03` (shaping rule 5) | merges are per slice and in order: `02` merges at its own `integrate` step on its own three verdicts, **without waiting for `03`**, and is accepted at its `slice_accept`; only then does `03`'s builder hand off a candidate descending from `02`'s merge, which earns its own verdicts, merges and is accepted; gate on `main` after each merge; `wave_integration` hands off to `wave_review` when both are accepted (w2 is the last wave since the fast plan) |
| (`w3`) | `04-audit-read` | removed by the fast plan (2026-10-03): the slice moved to mission 02 | — |

The order inside every slice is fixed by the slice workflow: `integrate` (the merge, once three verdicts name one SHA) precedes `slice_accept` (the proof, judged against the merge). `wave_integration` therefore never holds a merge for another slice's proof; each slice's `integrate` packet is worked as it arrives, and the wave-level wait (`--wait-for-proof`) is for every slice of the wave to be accepted before the next wave is launched. In `w2` that gives the acyclic sequence merge `02` → hand off `03` → prove `03` → merge `03` → accept both → `wave_review` (decomposition review DC-02; w3 removed by the fast plan).

Territory check for `w2` (the only concurrent wave):

| Path | `02-analytics` | `03-operate` |
|---|---|---|
| `src/main/java/dev/urlshort/click/` (+ tests) | owner | — |
| `src/main/java/dev/urlshort/link/` | grant: the redirect hook only | — |
| `src/main/java/dev/urlshort/web/` (+ tests) | — | owner |
| `src/main/resources/db/migration/` | V2 only | — |
| `src/main/resources/application.properties` | — | grant |
| `build.gradle.kts` | — | grant (registry line only) |
| `docs/api/openapi.json` | ordered grant, first holder (until `02` merges) | ordered grant, second holder (from `02`'s merge; `03`'s candidate descends from it) |
| `Dockerfile`, `compose.yaml`, `scripts/smoke.sh` | — | owner |
| `docs/diagrams/erd.mmd` | owner | — |

Documents edited on `main` by single seats (`docs/DESIGN.md`, `docs/adr/`, `docs/qa/`, `docs/review/`) are serialised by their owning seat and are not worktree territory.

**Shaping rules applied** (`docs/guidance/decomposition.md` §2–§3, plus one of this mission's own):

1. Foundations first: `01` carries the schema, the error contract, audit wiring and the first journey; `02`–`04` add outcomes.
2. Vertical, not horizontal: see the table.
3. Disjoint territories inside `w2`; every shared-file grant is in `slice.yaml` with its reason.
4. Every `depends_on` edge names a crossing artefact (table above); no edge inside a wave.
5. **One holder of the generated API document at a time (ordered grants).** `docs/api/openapi.json` is generated; two branches regenerating it from one base can conflict textually or, worse, merge cleanly into a document that no longer matches the running service. So the file has one holder at a time: `01` in w1; in w2 the grant is ordered, `02` (its stats endpoint) until `02` merges, then `03` (the `429` problem-detail response with `Retry-After` and examples on every operation, NFR-M3 carried through FR-10); the audit endpoint's entry comes with mission 02. `02` merges at its own `integrate` step on its own three verdicts, never waiting for `03`; the serialisation point is `03`'s `implement` handoff: its candidate descends from `02`'s merge commit, with the document regenerated on that base so it carries both changes. If `03` is otherwise done before `02` has merged, the builder waits on `02`'s frontier packet with the continuation "rebase onto main, regenerate, hand off". The integrator checks the ancestry (`git merge-base --is-ancestor <02 merge> <03 candidate>`) before merging `03`; a candidate that predates `02`'s merge is routed back to `implement`, and the three reviews re-earn their verdict on the regenerated diff in one short turn each. QA verifies each holder's committed document against the live `/v3/api-docs` of its candidate, so the document on `main` matches the integrated service after every merge. `01`'s design makes the export deterministic and key-sorted so the regenerations diff cleanly. The arrangement never depended on `04`, which the fast plan moved to mission 02.

**Decisions the human is asked to make at this gate** (default in bold):

1. **The `assumed` rows.** `docs/REQUIREMENTS.md` has eight rows whose first tag is `assumed`; this gate confirms or changes them, after which the tag becomes `decided (<this gate's qitem>)`. **Default: confirm all eight as written.**

   | Id | Assumed value | Built in |
   |---|---|---|
   | NFR-L1 | redirect p95 ≤ 20 ms, p99 ≤ 50 ms at 100 req/s sustained 60 s, single instance, H2 file DB | `03-operate` adds the bench mode; measured at `release_prep` |
   | NFR-L2 | create p95 ≤ 50 ms at 20 req/s | same |
   | NFR-R2 | 60 creates/min/client, 600 redirects/min/client, token bucket, client = remote address behind an explicit trusted-proxy rule | `03-operate` |
   | NFR-R5 | idempotency keys honoured for 24 h | `01-create-redirect` |
   | NFR-S3 | JSON body ≤ 16 KiB, headers at Tomcat defaults, no multipart | `01-create-redirect` |
   | NFR-S6 | no authentication, every endpoint anonymous; the audit endpoint loopback-only by default | anonymous everywhere from `01`; the loopback default in `04-audit-read` |
   | NFR-S2 | alias `[A-Za-z0-9_-]{4,32}`, case-sensitive, reserved words refused | confirmed now, built in mission 02 |
   | NFR-P2 | click retention 90 days, then deleted or aggregated | confirmed now, built in mission 02 (mission 03 may revisit); `02-analytics` must not preclude it |

   Two secondary assumptions ride on `derived`/`stated` rows: NFR-R4's single-node ceiling (no replication, no HA) is acceptable for the prototype and is recorded in `docs/RISKS.md`; FR-8's statistics shape is exactly total clicks, clicks per day and top referrers, and anything beyond is mission 03.

2. **Where the dependency overrides land.** The ship decision on `00-hello` (`qitem-20261003023502-f807af1f`) reads: "Tomcat/Jackson advisories tracked in qitem-20261003021640-bc2477ef and fixed before the first slice that parses client input", and the mission-close backlog says "Dependency upgrade slice, first." **Default: the overrides are the first commit of `01-create-redirect`**, gated green on their own before any feature commit, with a fresh OSV run captured by the builder (a Claude seat with network; the QA sandbox has none) and repeated by the release agent. Reason: `01` is the first slice that parses client input, so no such code reaches `main` without the fix; a standalone slice would carry no requirement id (the guide calls that machinery) and would cost a full pipeline pass before any product work. Alternative: a standalone low-tier slice `00-dependency-overrides` ahead of `w1`, if the human prefers the fix to be reviewed and merged on its own.

3. **FR-17 in this mission.** `docs/REQUIREMENTS.md` allocates FR-17 to "01 → reliability (or 02 if time-boxed out; recorded either way)". **Default: keep it, as `04-audit-read` in `w3`.** Alternative: defer `04-audit-read` to mission 02 as an additive brownfield slice, saving one serial wave (roughly three hours of pipeline time) at the cost of shipping an audit trail nobody can read until mission 02. **Outcome:** the plan-lock kept it; the human's fast plan (05:30Z) then moved it to mission 02 as the brownfield enhancement slice.

**Human gates on this mission.** As briefed at the plan-lock, every slice was high-tier under the guide's §4 table and the human would have decided six times. The fast plan (05:30Z) delegates the plan-locks of `02-analytics`, `03-operate` and every later slice to the orchestration lead (tier low, `urlshort-slice-delegated`, the lead's reasoning recorded at each closure); after the fast plan the human decided three times on this mission: this plan-lock (done), `01-create-redirect`'s slice plan-lock, and the ship sign-off. Human decision D11 (2026-10-03T06:05Z, operator item `qitem-20261003055950-1b5ec0d3`) then delegated **every** slice plan-lock for the rest of the run to the orchestration lead, `01-create-redirect`'s included (the `urlshort-slice` workflow's `plan_lock` gate now targets the orchestrator); the human now decides twice on this mission: this plan-lock (done) and the ship sign-off, plus any ambiguity decision. `01-create-redirect` keeps `tier: high` as its risk classification (the guide's §4 reasons still hold and the lead's plan review weighs them); only the gate's handler changed. No tier was chosen by an agent to avoid a gate; the lowering and the delegation are the human's.

**Risks.**

- **Seat availability.** At decompose (2026-10-03T03:38Z) `rig ps --nodes` listed the orchestration lead, design, development and QA seats; the requirements, review and release seats were absent. `decomposition_review` itself waits on the review seat, and every slice waits at `requirements`. Runtime risk, not scope risk; the operator has been informed.
- **Size of `01`.** One endpoint over the guide's ceiling, for the reasons given above; the mitigation is the FR-3/FR-4 split by outcome if the design review asks for it.
- **The generated API document** in a concurrent wave: handled by shaping rule 5 (ordered grant; `03` rebases onto `02`'s merge and regenerates) and the integrator's ancestry check; the cost is at most one short re-review round on `03` if its builder hands off early, and `03` may wait on `02`'s merge if `02` loops.
- **The dependency overrides change Tomcat and Jackson patch versions under the whole service**: gated alone as the first commit; a regression surfaces in `scripts/gw check` before any feature code is on the branch.
- **QA's Codex sandbox has no network**: the OSV check is a builder capture and a release-prep repeat; QA verifies the resolved versions offline from the Gradle dependency report.
- **Wall clock.** `00-hello`'s trivial slice took 3.8 h end to end; four real slices over three waves will not fit the single day `PLAN.md` hoped for, and the parallel wave does not halve the time on the single QA and single review seats. The deferral alternative (decision 3) is the lever; proportional reviews on small steps are the other.
- **NFR-L1/L2 need a load generator that does not exist yet**: `03-operate` adds `--bench` to `scripts/smoke.sh`; a noisy measurement on the reference laptop is recorded as a gap, not a checkmark.
- **`02`'s hook into the redirect path crosses into `link/`**: the grant is limited to that hook; `01`'s design should leave an obvious seam, and the decomposition does not prescribe the mechanism.
- **O3 asks for Prometheus format**: `/actuator/metrics` already answers; the registry is one `runtimeOnly` line if the design decides the wording requires it (ponytail ladder: platform before dependency).
- **Single node, H2 file**: NFR-R4's ceiling, accepted for the prototype and recorded in `docs/RISKS.md`.

**Explicitly not in this mission.** FR-17 and NFR-S6 (audit-trail read, moved to mission 02 by the fast plan of 2026-10-03), FR-11 (custom alias) and FR-12 (expiry) (both dropped from the plan by the same decision), FR-13–FR-15 (brownfield change management, bug fix, test and doc improvements), FR-16 (analytics v2 decisions); NFR-S2, P2 (purge), X2 (migration rollback discipline beyond the first migrations' written rollbacks); everything in `docs/REQUIREMENTS.md` §4 (auth, tenants, custom domains, UI, previews, HA, GDPR workflows, QR codes).

**Recommended default.** Approve: four slices, three waves, the tiers as listed, the eight `assumed` rows confirmed, the dependency overrides as the first commit of `01`, FR-17 kept as `04-audit-read`. Alternative: approve with `04-audit-read` deferred to mission 02 (three waves become two). **Decided:** approved as briefed at 04:39Z; amended by the human's fast plan at 05:30Z to three slices in two waves with `02`/`03` delegated (Status).

## Self-check (decompose)

Recorded 2026-10-03 before the handoff to `decomposition_review`.

- One buildable user outcome per slice, failure paths inside: yes; each intent is one persona's sentence, and `01` carries its `400`/`404`/`405`/`410`/`413`/`500` paths, `02` its not-found and privacy paths, `03` its at-limit/over-limit/spoofed-proxy paths, `04` its non-loopback refusal.
- Disjoint territories within a wave, shared-file grants explicit with reasons: yes; `w2` table above, grants in each `slice.yaml` with the reason and the reviewer's check; the one file both `w2` slices change (`docs/api/openapi.json`) is an ordered grant with a named serialisation point and an integrator precondition (decomposition review DC-01).
- Every `depends_on` edge names a crossing artefact, no edge inside a wave: yes; `02`, `03`, `04` → `01` with the artefact named; `w2` has no internal edge; `04`'s placement in `w3` is a `SOFT-AFTER` line in its `slice.yaml`, not a hard edge.
- Schedule acyclic: yes; merges happen per slice at `integrate` and precede the proof at `slice_accept`; `02` never waits for `03`; the wave-level wait is for accepted slices, after their merges (decomposition review DC-02).
- Tiers with reasons that hold against §4: yes; all four high, each reason quoting the §4 row it matches; none chosen to avoid a gate; the gate count is stated.
- Every in-scope id allocated exactly once, cross-cutting noted, no orphan slice: yes; allocation table, 35 ids (11 FR, 24 NFR).
- Compiled graph committed, `unknowns` empty: see `docs/evidence/01-greenfield-core/compiled-graph.json` and NOTES §1 for the compile and revise receipts.
- Decision brief complete and honest about `assumed` rows and risks: yes; eight `assumed` rows by name, two secondary assumptions, three decisions with defaults and alternatives, nine risks with mitigations, the not-in-scope list, the gate count.
- Brownfield impact analysis: not applicable; greenfield mission.
- Doghouse stated: yes, in §The doghouse.
- Not verified by me: nothing in this decomposition has been built; the territory lists are predictions of where the change happens and may be extended by grant at design time, as on `00-hello`.
- Amended 2026-10-03T05:30Z by the human's fast plan: three slices, two waves; `02`/`03` tier low by human decision (tier reasons rewritten to cite it); `04-audit-read` moved to mission 02 with its scaffold; counts redone (10 FR, 23 NFR); shared-file custody in w2 unchanged; the review agent re-checks the amended set on a scoped queue item, not by reopening the completed lifecycle step.

## Slices

- `01-create-redirect` — Create and redirect. Tier high (risk); plan-lock delegated to the lead by D11. Wave w1. State: in flight; requirements passed review at `0acbc9d`, design handed off at `0aaab2f`, design review in progress.
- `02-analytics` — Click analytics. Tier low (delegated, fast plan). Wave w2. State: scaffolded.
- `03-operate` — Operate safely. Tier low (delegated, fast plan). Wave w2. State: scaffolded.
- `04-audit-read` — moved to mission 02 as `01-audit-read` by the fast plan (2026-10-03).

## Status

- 2026-10-03 — decomposed into four slices over three waves; compiled graph exported to `docs/evidence/01-greenfield-core/compiled-graph.json`; handed to `decomposition_review`.
- 2026-10-03T04:12Z — decomposition review returned one HIGH finding, DC-01 (`docs/review/01-greenfield-core/decomposition-review.md`): the `429`/`Retry-After` contract had no owner in the committed API document. Fixed: `03-operate` owns it under an ordered w2 grant with a named serialisation point (shaping rule 5 rewritten, M3 carried into `03`, w2 merge order recorded in the wave map); compiled binding re-adopted.
- 2026-10-03T04:20Z — re-review of `ed7672f`: DC-01 settled; new HIGH DC-02, the `w2` row still said "waits for both proofs; merges serially" while rule 5 makes `03`'s handoff depend on `02`'s merge, a cycle. Fixed: merges are per slice at `integrate` and precede `slice_accept`; `02` merges without waiting for `03`; the wave-level wait is for both accepted slices before `w3`. All three wave rows now state the real order.
- 2026-10-03T04:25Z — decomposition review **PASS** on `6b5e17f` (`docs/review/01-greenfield-core/decomposition-review.md`, review evidence committed at `93b9245`): DC-01 and DC-02 settled, no open findings, compiled binding `1-de9659cfc2f86cdb` matches the live instance. Held at **mission plan-lock** on `qitem-20261003042553-03ac8b4b`, parked on `human@kernel` with this file as evidence. The human decides the eight `assumed` rows, the dependency-override placement and FR-17 keep/defer (decision brief above).
- 2026-10-03T04:39Z — **mission plan-lock approved** by `human@kernel` on `qitem-20261003042553-03ac8b4b`: "approve: four slices in three waves as briefed; the eight assumed rows (NFR-L1, L2, R2, R5, S3, S6, S2, P2) are confirmed as stated; dependency overrides land as the first gated commit of 01-create-redirect; FR-17 stays in this mission as 04-audit-read". Stamp recorded on the human's behalf (frontmatter `approved-spec-*`); the eight rows in `docs/REQUIREMENTS.md` now carry `decided`. Wave w1 launching.
- 2026-10-03T05:18Z — Javadoc policy adopted by the human (see Inputs); routed into `01-create-redirect` by the lead, manifest grants added, binding re-adopted. Slice `01` at `design`.
- 2026-10-03T05:30Z — **Human decision, fast plan** (relayed verbatim by the operator on `qitem-20261003052736-7830d02a`; mission plan-lock re-approved on the human's behalf, action `01M403RBRS6DQTXA1VW89AXQCA`): slice plan-locks after `01` delegated to the lead (`02`, `03` now tier low); `04-audit-read` moved to mission 02 as `01-audit-read`; FR-11, FR-12, NFR-S2 dropped from the plan; w2 = `02` ∥ `03` is the last wave; a second reviewer and a second QA seat are coming with a routing variant for `03`; mission 03 is decomposed in parallel once w2 builds. Decision text verbatim in `NOTES.md` §1. Scoped re-check requested from the review agent.
- 2026-10-03T05:45Z — fast-plan re-check **PASS** on `a7945e6` (review evidence `b4d7c4d`); its one non-blocking MEDIUM, DC-03 (opening brief still described the plan-lock shape), fixed at `a0e52a5`.
- 2026-10-03T06:05Z — **Human decision D11** (relayed verbatim by the operator on `qitem-20261003055950-1b5ec0d3`): "all slice plan-locks are delegated to the orchestration lead for the rest of the run; the human keeps mission plan-locks, ambiguity decisions and ship sign-offs." `01-create-redirect`'s plan-lock routes to the lead as handler at its next hop; human gates on this mission: two (see "Human gates on this mission").

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `$OPENRIG_HOME/reference/sdlc-conventions.md`).
