# Final engineering summary — urlshort on an agentic SDLC

> **Status: DRAFT, completed at mission 03 close.** Sections marked `[final]` are
> filled from the release packages and the evidence exports when the last
> mission closes; everything else is already true of the repository as it
> stands.

## 1. What was built, and how

Two deliverables in one repository:

- **The product** — `urlshort`, a URL-shortener service: Spring Boot 4.1.1 on
  Java 21, Gradle 9 (Kotlin DSL), embedded H2 in PostgreSQL mode with Flyway
  migrations, RFC 9457 problem details, ECS structured logging with a
  server-issued request id, an append-only audit log, privacy-reduced click
  analytics, per-client rate limiting, liveness/readiness, Prometheus metrics,
  graceful shutdown, a non-root read-only container. Run it with
  `scripts/gw check` → `java -jar build/libs/urlshort.jar` or
  `docker compose up --build` (`README.md`).
- **The factory** — the orchestration layer that built it on OpenRig 0.6.3: a
  rig of eleven seats (five on Claude Opus 5.5, six on OpenAI models: QA, review, requirements and release), three
  slice workflows and a mission lifecycle with entry/exit gates, independent
  review after every producing step, human checkpoints, bounded retries,
  rollback, safe-stop and dynamic re-planning (`docs/ARCHITECTURE.md`,
  `docs/GOVERNANCE.md`, `rig/`).

The plan of record and its decision log (D1–D12) are in `PLAN.md`; every
assignment clause is mapped to the guide section and the artefact that proves
it in `docs/guidance/README.md` §2.

## 2. Artefact map (assignment item → artefact → status) `[final]`

| Assignment item | Artefact | Status |
|---|---|---|
| §4.1 requirement understanding | `docs/REQUIREMENTS.md` (44 FR/NFR rows tagged stated / derived / decided / dropped), slice `SPEC.md`s with ambiguity logs, the mission-03 ambiguity park and the human's six answers | done |
| §4.2 decomposition | `missions/*/mission.yaml`, `slices/*/slice.yaml`, `docs/evidence/*/compiled-graph.json`, wave maps, plan-lock briefs | done (3 missions) |
| §4.3 brownfield reasoning | `missions/02-brownfield/slices/*/impact-analysis.md` | `[final]` |
| §4.4 orchestration | `rig/workflows/*.yaml`, `project.yaml#lifecycle`, `docs/GOVERNANCE.md`, `docs/evidence/*/` (trails, packets, gates), `docs/metrics/`, `docs/scenarios/drills.md` | done |
| §4.5 engineering output | `src/`, Flyway `V1`/`V2`, `docs/api/openapi.json`, Javadoc on every public type (`-Xdoclint:all -Werror` in `check`), `docs/DESIGN.md`, ADRs | done |
| §4.6 validation and risk control | `docs/RISKS.md`, `docs/scenarios/drills.md`, permission policies, loopback-only tooling | done |
| §4.7 controlled autonomy | gate packets and `rig queue resolve` records, delegation records (D11), role `Never` lists | done |
| §4.8 final summary | this document | `[final]` |
| §5 deliverables | prototype, `docs/ARCHITECTURE.md`, `docs/scenarios/*.md`, `README.md` + `docs/SETUP-FACTORY.md`, `docs/TESTING.md` | `[final]` for the scenario narratives |
| AI-SDLC artefacts | stories + ACs (`SPEC.md`), design docs + Mermaid, error handling/logging/audit + conventional commits, review files + `docs/review/REVIEW-LEDGER.md`, QA coverage/traceability/gaps | done |

## 3. The three scenarios `[final]`

| Mission | Scenario | What it demonstrated | Narrative |
|---|---|---|---|
| `00-hello` | dry run | one endpoint through every step and both human gates; two bounded remediation loops (DR-01, QA-01); stuck-sweep recovery; a refused `workflow revise` | `docs/scenarios/drills.md` |
| `01-greenfield-core` | greenfield | 3 slices in 2 waves (parallel wave with ordered custody of shared files), 28+ ACs per slice, review loops that caught a flaky test, a rate-limiter race and a fail-open smoke reader before merge; release with bench, OSV, secret scan; one explicitly human-decided gap (AC-28 host forwarder) | `docs/scenarios/greenfield.md` |
| `02-brownfield` | brownfield | impact analyses on shipped code, a purge with written rollback, a dogfood-sourced bug fix with regression test first, four labelled drills (QA rejection loop, revert after failed smoke, stop→route, abort+resume) | `docs/scenarios/brownfield.md` |
| `03-ambiguous-analytics` | ambiguous | "marketing wants better analytics" turned into six decisions with options and consequences, parked on the human before design, built to the decided scope | `docs/scenarios/ambiguous.md` |

## 4. Validation `[final]`

- Gate: `scripts/gw check` — unit + functional suites, 100 % line and branch on
  the merged execution data, Javadoc doclint; per-slice reports under
  `docs/qa/coverage/<slice>/`, AC ↔ test ↔ requirement-id traceability in
  `docs/qa/TRACEABILITY.md`, honest gaps in `docs/qa/GAPS.md`.
- Independent review of every artefact on the other model family; the ledger
  `docs/review/REVIEW-LEDGER.md` shows every file reviewed and every verdict.
- Installed smoke of jar and container on loopback; benchmark against the
  stated NFRs; OSV advisory check (0 advisories after the overrides); secret
  scan.
- Reliability metrics derived from engine records, not self-reports
  (`docs/metrics/README.md`): success rate, retries, rollbacks, MTTR,
  end-to-end latency, human wait — final numbers `[final]`.

## 5. Risks, trade-offs and decisions

`docs/RISKS.md` and `PLAN.md` §10 (D1–D12). The ones that shaped the result:
Java/Spring Boot 4 on Gradle; `standard` permissions with allow-lists rather
than autonomy without guardrails; an independent reviewer after every chunk
plus an author self-check inside it; slice plan-locks delegated to the lead
(D11) while mission plan-locks, ambiguity decisions and ship sign-offs stayed
human; a timed auto-approval was considered and rejected; scope trimmed under
the fast plan (custom alias and expiry dropped, audit-read moved to the
brownfield mission).

## 6. Assumptions

The eight `assumed` requirement rows (latency targets, rate limits, idempotency
window, body limit, no authentication, alias rules, 90-day retention) were put
to the human at the mission-01 plan-lock and confirmed as stated; the analytics
shape was decided by the human at mission 03's ambiguity park. Both decisions
are recorded verbatim in the queue transitions exported under `docs/evidence/`.

## 7. Limitations `[final]`

- OpenRig's workflow runtime keeps one live packet per instance; parallelism is
  achieved with concurrent slice instances, disjoint territories, ordered
  custody of shared files and a `wave_integration` barrier — not with
  parallel steps inside one instance.
- Codex seats run sandboxed without network; loopback probes and daemon calls
  from those seats escalate to an operator prompt, which the operator approved
  one by one (audited); read-only command classes were allow-listed as the run
  taught us which ones recur.
- The product is a single node on an embedded file database; the scale-out
  path is documented, not exercised.
- AC-28's published-port clause could not be met through macOS Docker's host
  forwarder; it ships as a disclosed host gap by human decision.
- `[final]` anything not delivered by mission 03 close.

## 8. How to verify in 15 minutes

```sh
scripts/gw check                        # gate: both suites, 100 % merged coverage, Javadoc
scripts/gw bootJar && java -jar build/libs/urlshort.jar   # http://localhost:8080
docker compose up --build               # same, in a container
```

Then read, in order: `docs/ARCHITECTURE.md` → `docs/GOVERNANCE.md` →
`missions/<mission>/RELEASE.md` → `docs/review/REVIEW-LEDGER.md` →
`docs/metrics/README.md` → `docs/evidence/<mission>/INDEX.md`. No OpenRig
installation is needed to read any of it; `docs/SETUP-FACTORY.md` explains how
to run the factory itself.
