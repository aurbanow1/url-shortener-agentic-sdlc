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

## The doghouse

A Creator posts a valid http(s) URL and gets a short link; a Visitor who opens it is redirected with a `302` and the click is recorded without slowing them; an Analyst reads the counts; an Operator sees abuse answered with `429`, truthful readiness, metrics, and every mutation in a readable audit trail, all from one container on one H2 file.

Nothing on the out-of-scope list of `docs/REQUIREMENTS.md` §4 is in this mission. Custom aliases, expiry, retention purge and "better analytics" are missions 02 and 03.

## Requirements in scope

From `docs/REQUIREMENTS.md`: FR-1 … FR-10, FR-17; NFR-L1–L3, R1–R6, S1, S3–S6, P1, O1–O3, A1–A2, M1–M3, X1. The `assumed` rows among these are confirmed or changed at this mission's plan-lock (listed in the decision brief below).

### Allocation (every id to exactly one slice; cross-cutting ids noted)

| Slice | FR | NFR | Cross-cutting ids this slice must also honour |
|---|---|---|---|
| `01-create-redirect` | FR-1, FR-2, FR-3, FR-4, FR-5, FR-6, FR-9 | S1, S3, S4, R5, R6, A1, A2, M3, O1, O2 | M1, M2 |
| `02-analytics` | FR-7, FR-8 | P1, L3 | M1, M2, M3 (the stats endpoint joins the committed API document), O1, O2 (request id and structured log on the new endpoint) |
| `03-operate` | FR-10 | R1, R2, R3, R4, S5, O3, X1, L1, L2 | M1, M2 |
| `04-audit-read` | FR-17 | S6 | M1, M2, M3, O1, O2 |

Notes on the allocation:

- **M1** (100 % line and branch coverage, honest gaps) and **M2** (an ADR before the code that depends on a cross-cutting decision) apply to every slice; they are listed once as cross-cutting and owned by no single slice.
- **O1** and **O2** were proven on `00-hello`; they are allocated to `01-create-redirect`, which re-proves them on the first real endpoints, and every later endpoint-adding slice inherits the obligation.
- **M3** (committed OpenAPI document): `01-create-redirect` owns the export mechanism and the first document (backlog W1-01); `02-analytics` and `04-audit-read` extend it, one per wave (shaping rule 5 below).
- **`03-operate` has two kinds of ids.** It builds and proves in-suite: FR-10, R2 (at the limit, one over, spoofed `X-Forwarded-For`), R1 (readiness group includes the database), O3 (metric names and the rejection counter readable through the actuator). It owns the configuration, `Dockerfile`, `compose.yaml` and `scripts/smoke.sh` lines for R3, R4, S5, X1, L1 and L2, whose proofs are release-level per the proof column of `docs/REQUIREMENTS.md` (installed smoke, `docker inspect`, restart loop, bench). What the slice cannot prove in-suite it records in `docs/qa/GAPS.md`; it does not claim it.
- **R5** (idempotency keys honoured for 24 h) sits with FR-9 in `01-create-redirect`.
- Count: 11 FR ids and 20 NFR ids in scope (M1 and M2 counted once as cross-cutting), all allocated; no slice without ids.

## Inputs carried from 00-hello

- Ordered backlog from the dry run (`missions/00-hello/NOTES.md`, "Backlog carried into mission 01"), and where each item landed:
  1. dependency overrides first (Tomcat embed 11.0.25, Jackson 3.1.7 / 2.21.7 as version overrides in `build.gradle.kts`, gate + fresh OSV run; advisory packet `qitem-20261003021640-bc2477ef`) → the **first commit of `01-create-redirect`**, gated green on its own before any feature commit (decision item 2 in the brief);
  2. OpenAPI export ownership (W1-01) → `01-create-redirect`;
  3. one real-server functional journey where an AC constrains log content (W1-02) → `02-analytics` (NFR-P1 constrains log content), weighed at its design step;
  4. unit-suite properties overlay (`src/test/resources/application.properties` shadows the shipped file) → optional in-passing fix granted to `01-create-redirect`'s territory; not an acceptance criterion.
- Product baseline: `docs/REQUIREMENTS.md`; the `assumed` rows are confirmed or changed at this mission's plan-lock (brief, decision item 1).
- Guidance that applies to this mission for the first time: `docs/guidance/decomposition.md`, `docs/guidance/orchestration.md`, `docs/guidance/review.md`, `docs/guidance/release.md`.

## Decision brief (mission plan-lock)

**Outcome.** Four slices over three waves. When this mission closes, `main` carries the public API (`POST /api/links`, `GET`/`DELETE /api/links/{code}`, `GET /{code}`, `GET /api/links/{code}/stats`, an audit read endpoint), a `link`, `click` and `audit_log` schema owned by Flyway, problem details on every error, a request id and a structured log line on every request, a rate limiter, truthful readiness, metrics, and a hardened container; every mutation writes an audit row that an Operator can read. Each slice arrives through requirements, design, their reviews, a plan-lock, test-first implementation, QA with coverage proof, code review and security review on one candidate SHA, serial integration and attributed proof acceptance.

**Slices, in order.**

| # | Slice | Outcome (one sentence) | Tier | Tier reason | Depends on | Territory (summary; full list in `slice.yaml`) |
|---|---|---|---|---|---|---|
| 1 | `01-create-redirect` | A Creator can create, read and retire a short link for a valid http(s) URL, and a Visitor who opens it is redirected (`302`) or told it is gone (`410`), with every mutation audited, every error a problem detail, and a retried create with the same `Idempotency-Key` returning the first link. | high | foundation: first migration (`link`, `audit_log`), error contract, audit wiring, OpenAPI export; new security-relevant surface (the target allow-list is the open-redirect boundary); carries the dependency overrides | none | `link/`, `audit/` (write side), `web/` (error advice), `db/migration/` (V1), `docs/api/openapi.json` (new), `build.gradle.kts`, `application.properties`, test property overlays |
| 2 | `02-analytics` | An Analyst can read a link's click statistics (total, per day, top referrers) because every redirect records a privacy-safe click event without slowing the Visitor. | high | adds a migration (`click` table) and decides privacy-sensitive storage under NFR-P1; the guide lists a migration as high | `01` (the `link` table and code lookup; the redirect path it hooks; the API document it extends) | `click/`, `db/migration/` (V2), `link/` **grant limited to the redirect hook**, `docs/api/openapi.json` (w2 grant), `docs/diagrams/erd.mmd` |
| 3 | `03-operate` | An Operator can run urlshort in production shape: clients above the rate limit are answered `429` with `Retry-After` and a counted rejection, readiness reflects the database, metrics are exposed for scraping, and the container runs non-root with durable data on a loopback-published port. | high | new security-relevant surface (rate limiter with the trusted-proxy rule; the guide names rate limit as high) and container privilege changes; no migration | `01` (the endpoints it limits; the problem-detail contract it reuses for `429`) | `web/` (the filter beside the request-id filter), `application.properties` (w2 grant), `build.gradle.kts` (w2 grant, Prometheus registry only), `Dockerfile`, `compose.yaml`, `scripts/smoke.sh`; **not** `link/`, `click/`, `db/migration/`, `docs/api/openapi.json` |
| 4 | `04-audit-read` | An Operator can read the audit trail of every mutation (who, what, when, before, after, request id) through a read-only, paginated endpoint that is loopback-only by default. | high | new security-relevant surface (operator data; loopback-only default is NFR-S6, an assumed row confirmed at this gate); the guide names audit read as high; no migration | `01` (the `audit_log` table and row shape) | `audit/` (read side), `docs/api/openapi.json` (w3 grant), `application.properties` (w3 grant) |

Every slice is a vertical through controller, service, repository, migration and both test suites for its own outcome; none is a layer. Slice 1 is one endpoint over the guide's "roughly ≤ 3 endpoints" ceiling: `GET /api/links/{code}` is a read of the same row, `DELETE` is required inside the slice because `410`-on-retired is a failure path of the redirect, and `Idempotency-Key` is the create contract (putting it in a later slice would re-open `link/` and the API document in a wave that already has one contract-changing slice). The requirements agent keeps each endpoint's criteria small; if the design review judges the slice too large, the split is FR-3/FR-4 off by outcome, applied with `rig workflow revise`, never by layer.

**Waves and synchronisation.** Recorded as the `wave-map` queue row (format `wave-map-v1`, body in `docs/evidence/01-greenfield-core/wave-map.md`).

| Wave | Slices | Why together / why alone | Sync point |
|---|---|---|---|
| `w1` | `01-create-redirect` | the foundation everything else depends on | `wave_integration` waits for its proof, merges, then launches `w2` |
| `w2` | `02-analytics` ∥ `03-operate` | no edge between them; disjoint territories (table below); one adds a migration and extends the API document, the other changes neither | waits for both proofs; merges serially (`02` first, then `03`), gate on `main` after each |
| `w3` | `04-audit-read` | hard edge only to `01`, but it needs the two shared files that `02` and `03` hold in `w2` (`docs/api/openapi.json`, `application.properties`), and a third contract-changing slice would queue on the single reviewer | waits for its proof, merges, then `wave_review` |

Territory check for `w2` (the only concurrent wave):

| Path | `02-analytics` | `03-operate` |
|---|---|---|
| `src/main/java/dev/urlshort/click/` (+ tests) | owner | — |
| `src/main/java/dev/urlshort/link/` | grant: the redirect hook only | — |
| `src/main/java/dev/urlshort/web/` (+ tests) | — | owner |
| `src/main/resources/db/migration/` | V2 only | — |
| `src/main/resources/application.properties` | — | grant |
| `build.gradle.kts` | — | grant (registry line only) |
| `docs/api/openapi.json` | grant | — |
| `Dockerfile`, `compose.yaml`, `scripts/smoke.sh` | — | owner |
| `docs/diagrams/erd.mmd` | owner | — |

Documents edited on `main` by single seats (`docs/DESIGN.md`, `docs/adr/`, `docs/qa/`, `docs/review/`) are serialised by their owning seat and are not worktree territory.

**Shaping rules applied** (`docs/guidance/decomposition.md` §2–§3, plus one of this mission's own):

1. Foundations first: `01` carries the schema, the error contract, audit wiring and the first journey; `02`–`04` add outcomes.
2. Vertical, not horizontal: see the table.
3. Disjoint territories inside `w2`; every shared-file grant is in `slice.yaml` with its reason.
4. Every `depends_on` edge names a crossing artefact (table above); no edge inside a wave.
5. **One API-document-changing slice per wave.** `docs/api/openapi.json` is generated; two branches regenerating it from one base can conflict textually and, worse, merge cleanly into a document that no longer matches the running service. So `01` (w1), `02` (w2) and `04` (w3) are the only slices that change it; `03` documents its `429` in design and `docs/DESIGN.md` and leaves the generated file alone. `01`'s design makes the export deterministic and key-sorted so that additions merge textually in later missions. If a merge still conflicts, the integrator runs `git merge --abort` and routes the slice back to `implement` for a rebase and regeneration; the three reviews re-earn their verdict on the new SHA in one short turn each.

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

3. **FR-17 in this mission.** `docs/REQUIREMENTS.md` allocates FR-17 to "01 → reliability (or 02 if time-boxed out; recorded either way)". **Default: keep it, as `04-audit-read` in `w3`.** Alternative: defer `04-audit-read` to mission 02 as an additive brownfield slice, saving one serial wave (roughly three hours of pipeline time) at the cost of shipping an audit trail nobody can read until mission 02.

**Human gates on this mission.** With every slice high-tier under the guide's §4 table, the human decides six times: this plan-lock, four slice plan-locks (`SPEC.md` + `design.md` each, after the independent design review), and the ship sign-off. `PLAN.md` expected three for mission 01; the tier rules written since then are stricter and the tiers above follow them rather than the count. The human can lower a tier at this gate ("revise: 02-analytics low"), which I apply with `rig workflow revise`; no tier was chosen to avoid a gate.

**Risks.**

- **Seat availability.** At decompose (2026-10-03T03:38Z) `rig ps --nodes` listed the orchestration lead, design, development and QA seats; the requirements, review and release seats were absent. `decomposition_review` itself waits on the review seat, and every slice waits at `requirements`. Runtime risk, not scope risk; the operator has been informed.
- **Size of `01`.** One endpoint over the guide's ceiling, for the reasons given above; the mitigation is the FR-3/FR-4 split by outcome if the design review asks for it.
- **The generated API document** in a concurrent wave: handled by shaping rule 5 and the integrator's abort-and-route fallback.
- **The dependency overrides change Tomcat and Jackson patch versions under the whole service**: gated alone as the first commit; a regression surfaces in `scripts/gw check` before any feature code is on the branch.
- **QA's Codex sandbox has no network**: the OSV check is a builder capture and a release-prep repeat; QA verifies the resolved versions offline from the Gradle dependency report.
- **Wall clock.** `00-hello`'s trivial slice took 3.8 h end to end; four real slices over three waves will not fit the single day `PLAN.md` hoped for, and the parallel wave does not halve the time on the single QA and single review seats. The deferral alternative (decision 3) is the lever; proportional reviews on small steps are the other.
- **NFR-L1/L2 need a load generator that does not exist yet**: `03-operate` adds `--bench` to `scripts/smoke.sh`; a noisy measurement on the reference laptop is recorded as a gap, not a checkmark.
- **`02`'s hook into the redirect path crosses into `link/`**: the grant is limited to that hook; `01`'s design should leave an obvious seam, and the decomposition does not prescribe the mechanism.
- **O3 asks for Prometheus format**: `/actuator/metrics` already answers; the registry is one `runtimeOnly` line if the design decides the wording requires it (ponytail ladder: platform before dependency).
- **Single node, H2 file**: NFR-R4's ceiling, accepted for the prototype and recorded in `docs/RISKS.md`.

**Explicitly not in this mission.** FR-11 (custom alias), FR-12 (expiry), FR-13–FR-15 (brownfield change management, bug fix, test and doc improvements), FR-16 (analytics v2 decisions); NFR-S2, P2 (purge), X2 (migration rollback discipline beyond the first migrations' written rollbacks); everything in `docs/REQUIREMENTS.md` §4 (auth, tenants, custom domains, UI, previews, HA, GDPR workflows, QR codes).

**Recommended default.** Approve: four slices, three waves, the tiers as listed, the eight `assumed` rows confirmed, the dependency overrides as the first commit of `01`, FR-17 kept as `04-audit-read`. Alternative: approve with `04-audit-read` deferred to mission 02 (three waves become two).

## Self-check (decompose)

Recorded 2026-10-03 before the handoff to `decomposition_review`.

- One buildable user outcome per slice, failure paths inside: yes; each intent is one persona's sentence, and `01` carries its `400`/`404`/`405`/`410`/`413`/`500` paths, `02` its not-found and privacy paths, `03` its at-limit/over-limit/spoofed-proxy paths, `04` its non-loopback refusal.
- Disjoint territories within a wave, shared-file grants explicit with reasons: yes; `w2` table above, grants in each `slice.yaml` with the reason and the reviewer's check.
- Every `depends_on` edge names a crossing artefact, no edge inside a wave: yes; `02`, `03`, `04` → `01` with the artefact named; `w2` has no internal edge; `04`'s placement in `w3` is a `SOFT-AFTER` line in its `slice.yaml`, not a hard edge.
- Tiers with reasons that hold against §4: yes; all four high, each reason quoting the §4 row it matches; none chosen to avoid a gate; the gate count is stated.
- Every in-scope id allocated exactly once, cross-cutting noted, no orphan slice: yes; allocation table, 31 ids.
- Compiled graph committed, `unknowns` empty: see `docs/evidence/01-greenfield-core/compiled-graph.json` and NOTES §1 for the compile and revise receipts.
- Decision brief complete and honest about `assumed` rows and risks: yes; eight `assumed` rows by name, two secondary assumptions, three decisions with defaults and alternatives, nine risks with mitigations, the not-in-scope list, the gate count.
- Brownfield impact analysis: not applicable; greenfield mission.
- Doghouse stated: yes, in §The doghouse.
- Not verified by me: nothing in this decomposition has been built; the territory lists are predictions of where the change happens and may be extended by grant at design time, as on `00-hello`.

## Slices

- `01-create-redirect` — Create and redirect. Tier high. Wave w1. State: scaffolded, awaiting mission plan-lock.
- `02-analytics` — Click analytics. Tier high. Wave w2. State: scaffolded.
- `03-operate` — Operate safely. Tier high. Wave w2. State: scaffolded.
- `04-audit-read` — Audit trail read. Tier high. Wave w3. State: scaffolded.

## Status

- 2026-10-03 — decomposed into four slices over three waves; compiled graph exported to `docs/evidence/01-greenfield-core/compiled-graph.json`; handed to `decomposition_review`.

---

> Work from this `SPEC.md`; keep durable acceptance state in `PROGRESS.md`
> and context that does not belong in the contract in `NOTES.md`. Load the
> `mission-slice-sop` skill for the operating procedure. Conventions SSOT:
> `docs/reference/sdlc-conventions.md` (installed:
> `$OPENRIG_HOME/reference/sdlc-conventions.md`).
