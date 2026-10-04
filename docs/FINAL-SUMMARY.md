# Final engineering summary — urlshort on an agentic SDLC

> **Status: complete.** All four missions are closed (mission 02 last, at
> `2522e6c2`, 2026-10-04 05:36Z). Every mission and every slice reads ready:
> 123 of 123 proof items accepted across eleven slices (`rig proof show`,
> 2026-10-04 05:46Z, after the last re-affirmation `ba34d9e6`). Its
> project-level field reads `not-ready` only because it aggregates *active*
> missions and none remains. The metrics are the run-end refresh, read from
> the live daemon with every workflow instance terminal.

## 1. What was built, and how

Two deliverables in one repository:

- **The product** — `urlshort`, a URL-shortener service: Spring Boot 4.1.1 on
  Java 21, Gradle 9 (Kotlin DSL), embedded H2 in PostgreSQL mode with Flyway
  migrations, RFC 9457 problem details, ECS structured logging with a
  server-issued request id, an append-only audit log, privacy-reduced click
  analytics, per-client rate limiting, liveness/readiness, Prometheus metrics,
  graceful shutdown, a non-root read-only container. Run it with
  `scripts/gw check` → `java -jar build/libs/urlshort.jar` or
  `docker compose up --build` (`README.md`). The deliverable is the private
  GitHub repository `aurbanow1/url-shortener-agentic-sdlc` (D19), whose `main`
  is verified by GitHub Actions: the `check` gate on every pull request and on
  `main`, and a CD job that builds the jar and the image and smokes the jar on
  loopback without publishing anything (D14).
- **The factory** — the orchestration layer that built it on OpenRig 0.6.3: a
  rig of twelve seats (five on Claude Opus 5.5, seven on OpenAI models: QA, review, requirements and release), three
  slice workflows and a mission lifecycle with entry/exit gates, independent
  review after every producing step, human checkpoints, bounded retries,
  rollback, safe-stop and dynamic re-planning (`docs/ARCHITECTURE.md`,
  `docs/GOVERNANCE.md`, `rig/`).

The plan of record and its decision log (D1–D21; D10 unused) are in `PLAN.md`; every
assignment clause is mapped to the guide section and the artefact that proves
it in `docs/guidance/README.md` §2.

## 2. Artefact map (assignment item → artefact → status)

| Assignment item | Artefact | Status |
|---|---|---|
| §4.1 requirement understanding | `docs/REQUIREMENTS.md` (44 FR/NFR rows tagged stated / derived / decided / dropped), slice `SPEC.md`s with ambiguity logs, the mission-03 ambiguity park and the human's six answers | done |
| §4.2 decomposition | `missions/*/mission.yaml`, `slices/*/slice.yaml`, `docs/evidence/*/compiled-graph.json`, wave maps, plan-lock briefs | done (4 missions: the 00-hello dry run plus three scenarios) |
| §4.3 brownfield reasoning | `missions/02-brownfield/slices/*/impact-analysis.md` (six slices, the D21 refactor included), `docs/scenarios/brownfield.md` §Codebase reasoning, including the forwarded-header path one analysis missed and review caught | done |
| §4.4 orchestration | `rig/workflows/*.yaml`, `project.yaml#lifecycle`, `docs/GOVERNANCE.md`, `docs/evidence/*/` (trails, packets, gates), `docs/metrics/`, `docs/scenarios/drills.md` | done |
| §4.5 engineering output | `src/`, Flyway `V1`–`V4`, `docs/api/openapi.json`, Javadoc on every public type (`-Xdoclint:all -Werror` in `check`), `docs/DESIGN.md`, ADRs | done |
| §4.6 validation and risk control | `docs/RISKS.md`, `docs/scenarios/drills.md`, permission policies, loopback-only tooling, CI/CD on GitHub Actions (`.github/workflows/`, D14) with the first runs recorded in `missions/02-brownfield/slices/05-ci-cd/PROOF.md` | done |
| §4.7 controlled autonomy | gate packets and `rig queue resolve` records, delegation records (D11), role `Never` lists | done |
| §4.8 final summary | this document | done |
| §5 deliverables | prototype, `docs/ARCHITECTURE.md`, `docs/scenarios/*.md`, `README.md` + `docs/SETUP-FACTORY.md`, `docs/TESTING.md`, the GitHub repository with its stacked pull requests and green CI/CD runs | done |
| AI-SDLC artefacts | stories + ACs (`SPEC.md`), design docs + Mermaid, error handling/logging/audit + conventional commits, review files + `docs/review/REVIEW-LEDGER.md`, QA coverage/traceability/gaps | done |

## 3. The three scenarios

| Mission | Scenario | What it demonstrated | Narrative |
|---|---|---|---|
| `00-hello` | dry run | one endpoint through every step and all three human gates; two bounded remediation loops (DR-01, QA-01); stuck-sweep recovery; a refused `workflow revise` | `docs/scenarios/drills.md` |
| `01-greenfield-core` | greenfield | 3 slices in 2 waves (parallel wave with ordered custody of shared files), 28+ ACs per slice, review loops that caught a flaky test, a rate-limiter race and a fail-open smoke reader before merge; release with bench, OSV, secret scan; one explicitly human-decided gap (AC-28 host forwarder) | `docs/scenarios/greenfield.md` |
| `02-brownfield` | brownfield | six slices on shipped code: an enhancement read (audit), a purge with written rollback, a dogfood-sourced bug fix with regression tests first, an expand migration for the human's audit-column policy, CI/CD, and a behaviour-preserving refactor proven by characterization tests and before/after captures (D21); impact analyses first; a security finding caught by review after QA (CR-01); four drills (QA rejection loop, revert after failed smoke, stop→route, resume+abort); shipped at `30f8de4e` under the human's local-use sign-off, with the V4/V3 migration rollback rehearsed on a copy of its data | `docs/scenarios/brownfield.md` |
| `03-ambiguous-analytics` | ambiguous | "marketing wants better analytics" turned into six decisions with options and consequences, parked on the human before design, built to the decided scope; stacked on mission 02's click work; shipped at `50ad9c3` under the human's sign-off with one disclosed and since-closed CI gap | `docs/scenarios/ambiguous.md` |

## 4. Validation

- Gate: `scripts/gw check` — unit + functional suites, 100 % line and branch on
  the merged execution data, Javadoc doclint; per-slice reports under
  `docs/qa/coverage/<slice>/`, AC ↔ test ↔ requirement-id traceability in
  `docs/qa/TRACEABILITY.md`, honest gaps in `docs/qa/GAPS.md`.
- Independent review of every artefact by a seat that did not write it, on the
  other model family for design, code, QA and security (requirements and release share
  a runtime with their reviewer since D17, see §7); the ledger
  `docs/review/REVIEW-LEDGER.md` shows every file reviewed and every verdict.
- The final shipped product (`30f8de4e`): 268 unit and 322 functional tests,
  583/583 lines and 206/206 branches, re-run independently by the release
  reviewer. The same code passed GitHub's `gate` in pull request #14.
- Installed smoke of jar and container on loopback; benchmark against the
  stated NFRs; OSV advisory check (0 advisories on 97 runtime coordinates);
  secret scan; migration rollback rehearsed with the previous binary started
  on the rolled-back data.
- Reliability metrics derived from engine records, not self-reports
  (`docs/metrics/README.md`): success rate, retries, rollbacks, MTTR,
  end-to-end latency, human wait. Final numbers, the whole run:

| Metric | Value |
|---|---|
| Workflow instances | 17: 16 completed, 1 aborted (DRILL 4, on purpose), 0 failed |
| Instance success rate (completed ÷ terminal) | 0.941; every product instance completed |
| Step closures (failed) | 254 (18); step success 0.929 |
| Retries (failed verdicts + step re-entries) | 55, all review loops working as designed |
| Rollbacks executed | 6: release rehearsals and drills, each with raw evidence (`docs/metrics/rollbacks.json`); no revert reached `main`, and no production rollback was needed |
| Engine recoveries | 1 resume and 1 abort, both DRILL 4 |
| MTTR (failed check → next handoff or done of that step) | 31 min, the mean over 16 repairs: repair of a rejected candidate, not incident recovery |
| End-to-end latency, completed instances | p50 5.6 h, p95 15.7 h |
| Time parked on the human | 8.8 h, reported apart from agent throughput |

  `node tools/sdlc-metrics.mjs --check` regenerates these numbers offline from
  `docs/evidence/run-end/` and fails on any difference. The derivations and their
  limits are in `docs/metrics/README.md`; per-mission
  rows are in each scenario's Metrics section.

## 5. Risks, trade-offs and decisions

`docs/RISKS.md` and `PLAN.md` §10 (D1–D21). The ones that shaped the result:
Java/Spring Boot 4 on Gradle; `standard` permissions with allow-lists rather
than autonomy without guardrails; an independent reviewer after every chunk
plus an author self-check inside it; slice plan-locks delegated to the lead
(D11) while mission plan-locks, ambiguity decisions and ship sign-offs stayed
human; a timed auto-approval was considered and rejected; scope trimmed under
the fast plan (custom alias and expiry dropped, audit-read moved to the
brownfield mission). Later in the run the human added seats where the trails
showed queues (second builder, designer and release seat, D15, D16, D18),
moved requirements and release to Codex models (D17, with a disclosed cost to
review independence), made the GitHub repository the deliverable with stacked
pull requests and CI/CD (D13, D14, D19), gave architecture consistency an
owner instead of a new seat (D20), and added the refactor the assignment's
scope names (D21).

## 6. Assumptions

The eight `assumed` requirement rows (latency targets, rate limits, idempotency
window, body limit, no authentication, alias rules, 90-day retention) were put
to the human at the mission-01 plan-lock and confirmed as stated; the analytics
shape was decided by the human at mission 03's ambiguity park. Both decisions
are recorded verbatim in the queue transitions exported under `docs/evidence/`.

## 7. Limitations

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
- Review independence is weaker on two steps since D17: a SPEC is written and
  reviewed by the same model (GPT-6-Astra), and a release package on the same
  runtime. Code, QA and security review keep author and judge on different
  runtimes.
- Throughput was bounded by the code chain, not by seats: slices that touch
  the same files merge one after another, and a slice's steps run one at a
  time, so one long QA run holds everything downstream. Building on a reviewed
  sibling branch shortened that chain where the lead judged the risk
  acceptable.
- Codex seats run sandboxed, so each local app start or stop, Docker call or
  daemon call needs an operator approval. QA alone needed about twenty in one
  afternoon.
- Proof judgments bind the exact bytes of their evidence. Shared documents
  that later slices extend (traceability, gaps, the ERD) turn shipped items
  from accepted to unknown. They are re-affirmed by QA on the final `main`;
  each re-affirmation names the exact byte-level change it checked. Mission
  01's and mission 02's items were re-affirmed on `30f8de4e` before mission
  02's release review, and mission 03's last three after mission 02's evidence
  export.
- One dependency update (the Gradle wrapper, 9.7.1 to 9.8.0) was merged on
  GitHub outside the rig's review. The lead adopted the same change in the
  rig's `main` with a fresh gate and a warmed Gradle home (`f3e6b0b`), so the
  rig and GitHub build with the same Gradle.
- The Codex account hit its weekly usage limit once (about 18:27–18:33 local).
  The seats fell back to a weaker model until the human reset the usage. Later
  an operator keystroke on a rate-limit prompt left two seats on that model at
  medium effort for about 20 minutes, until the operator relaunched them. Work
  from those windows was re-checked: release review independently re-derived
  the one evidence commit, and the restored QA seat redid its proof item from
  raw files.
- Several pull requests were merged on GitHub before their `gate` check
  finished. Every such gate later passed, and `main`'s CI and CD runs after
  each merge are green; the later pull requests were merged only on green.
- Gradle's distribution server failed twice during CI (a 503 and a
  timeout). The failed jobs were re-run green, and making the gate tolerant
  of that is a recorded MEDIUM backlog item.
- Not delivered, each recorded with an owner or trigger in the mission notes:
  - custom aliases and link expiry, dropped from scope under the fast plan;
  - a bounded retry and timeout for the Gradle wrapper download (MEDIUM);
  - the shared `Clock` in `ping/` (LOW);
  - pinning functional journeys to `127.0.0.1` so they cannot reach a foreign
    loopback listener (LOW);
  - `totalClicks` documented as "retained click rows" rather than implying a
    lifetime total, in its Javadoc and API schema (M3S-01, LOW); the README and
    runbook already say it correctly;
  - a future `V5` migration must pin the V4-specific tests, as V4 had to for
    V3 (LOW, owner: the V5 author).
- GitHub has no CI run on the exact commit the human signed off for mission 02
  (`30f8de4e`), nor for mission 03 (`50ad9c3`), because agents never push. The
  human accepted each: mission 02's code passed CI one documentation-only
  change earlier (`e43ed246`, pull request #14), and mission 03's one test-only
  change earlier (`18db1de`). Later CI runs on `main` include both.
- `scripts/http`, the curl wrapper agents use for HTTP checks, at first
  checked only that a URL argument began with a loopback address, so
  `http://127.0.0.1.example.invalid/` or `--url=…` passed (found by the final
  fact-check and two external reviews). It now accepts only the curl options
  the repository's callers use, so redirect following, proxies, `--next` and
  `--url` are refused; it checks every destination exactly, clears proxy
  variables and ignores `~/.curlrc`. `scripts/http-guard-check.sh` runs 36
  cases and a proxy-variable check in CI. It remains an argument guard: for
  the Codex seats the hard network boundary is their sandbox.

## 8. How to verify in 15 minutes

The same gate runs on GitHub for every pull request; the green runs are linked
from the pull requests in the repository.

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
