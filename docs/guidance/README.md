# Engineering guidance library

Opinionated, actionable practice guides for this project. They encode *how we
build well* (kind knowledge) and sit beside the role contracts in
`rig/agents/*/guidance/role.md`, which say *who does what*. Guides are read on
demand at the step that needs them; they are not a reading list to recite.

## 1. The guides

| Guide | Who reads it | When |
|---|---|---|
| [requirements.md](requirements.md) — capturing requirements that an agent can build from | Requirements Agent; Review Agent (`requirements_review`); Planning | before writing or reviewing a `SPEC.md`; at `decompose` |
| [decomposition.md](decomposition.md) — slices, waves, tiers, dependency graph, re-planning, the plan-lock brief | orchestration lead (`decompose`); Review Agent (`decomposition_review`); the human at plan-lock | before scaffolding a mission; before judging its decomposition |
| [architecture.md](architecture.md) — structure, boundaries, API and data design, security, reliability, ADRs, diagrams | Design Agent; Review Agent (`design_review`, `wave_review`); Planning | before writing or reviewing a `design.md`; at `decompose` |
| [brownfield.md](brownfield.md) — impact analysis, safe change management, bug fixes, refactors, test/doc improvements | Design Agent (impact analysis); Development Agent; Review Agent | any slice that changes shipped behaviour |
| [java-spring.md](java-spring.md) — Java 21 and Spring Boot 4 practices for this codebase, incl. the Javadoc contract (§8) | Development Agent; Design Agent; Review Agent (`code_review`) | before the first line of code on a slice; while reviewing code |
| [databases.md](databases.md) — schema ownership, migrations, modelling, transactions, data lifecycle | Design Agent; Development Agent; Review Agent (`design_review`, `security_review`) | any slice touching a table or a query |
| [qa.md](qa.md) — test strategy, by-effect verification, coverage policy, evidence, findings | QA Agent; Development Agent (tests first); Review Agent (`code_review` audits QA) | before writing tests; before `qa_check`; before judging proof items |
| [review.md](review.md) — independence, proof of complete coverage, severities, issue → resolution → re-review, the security checklist | Review Agent (every review step); authors receiving findings | before any review; when answering findings |
| [orchestration.md](orchestration.md) — exit semantics, bounded retries, exception dial, rollback, safe-stop, re-planning, human checkpoints, lineage | orchestration lead; every seat for the exit rules | at every mission step; when a loop does not converge |
| [release.md](release.md) — release package contract, installed smoke, advisories, rollback, metrics, evidence export, final summary | Release & Reliability Agent; Review Agent (`release_review`); lead (`mission_close`) | at `release_prep` and `evidence_export`; before the final summary |

Rules of the library:

- A guide states a practice, the reason, and the check that proves it was
  followed. No practice without a reason; no reason without a check.
- Guides are versioned with the code. When a review finds the guide was wrong
  or incomplete, the fix is a commit to the guide, not a workaround in the slice.
- Conflicts resolve in this order: the SPEC's acceptance criteria → an accepted
  ADR → these guides → the vendored skills (`ponytail`, TDD, review-team).
- Existing section numbers are stable (role files and committed reviews cite
  them); new material is appended or goes in a new guide.

## 2. Assignment coverage — item → guide → artefact that proves it

The assignment (`PLAN.md` §1 decodes it) is satisfied by artefacts; the guides
say how each artefact reaches the bar. Status is kept in `PLAN.md` §1 and the
final summary; this table is the map.

| Assignment item | Guide section(s) | Artefact |
|---|---|---|
| §4.1 Requirement understanding — interpret intent, identify ambiguity, normalise | requirements.md §1–§5 (shape, ACs, ambiguity policy, definition of ready) | `docs/REQUIREMENTS.md` (FR/NFR with `stated`/`derived`/`assumed`), slice `SPEC.md` with ambiguity log |
| §4.2 Task decomposition — tasks with dependencies and sequencing | decomposition.md §1–§7 | `missions/<m>/mission.yaml`, `slices/*/slice.yaml`, `docs/evidence/<m>/compiled-graph.json`, wave map, plan-lock brief |
| §4.3 Codebase reasoning (brownfield) — impacted modules/APIs/data flows | brownfield.md §1–§3 | `slices/*/impact-analysis.md`, `docs/DESIGN.md` deltas |
| §4.4 Workflow orchestration — graph, gates, parallel paths, lineage, human checkpoints, retries, rollback, safe-stop, guardrails, audit, metrics, re-planning | orchestration.md §1–§11; `docs/GOVERNANCE.md` (clause map) | `rig/workflows/*.yaml`, `project.yaml#lifecycle`, queue/trail exports in `docs/evidence/`, `docs/metrics/`, `docs/scenarios/drills.md` |
| §4.5 Engineering output — production-quality code, API/schema definitions, tests, docs | java-spring.md §2–§6; databases.md §1–§4; architecture.md §2–§5; qa.md §1–§3 | `src/`, Flyway migrations, `docs/api/openapi.json`, `docs/DESIGN.md`, ADRs |
| §4.6 Validation and risk control — risks, trade-offs, failure scenarios, guardrails | orchestration.md §4–§6; release.md §4–§5; architecture.md §10 | `docs/RISKS.md`, `docs/scenarios/drills.md`, permission policies (`.claude/settings.json`, `.codex/rules`), `scripts/http`, `scripts/gw` |
| §4.7 Controlled autonomy — agents execute, humans approve | orchestration.md §8; decomposition.md §4, §6 | gate packets and `rig queue resolve` records, stamps `--on-behalf-of human@kernel`, `rig/CULTURE.md` |
| §4.8 Final engineering summary | release.md §8 | `docs/FINAL-SUMMARY.md` |
| §5 Working prototype, runnable end to end | release.md §3; java-spring.md §5 | `scripts/gw check`, `java -jar build/libs/urlshort.jar`, `Dockerfile`, `compose.yaml` |
| §5 Architecture overview | architecture.md §1–§2, §9 | `docs/ARCHITECTURE.md`, `docs/diagrams/`, `docs/adr/` |
| §5 Three scenarios | decomposition.md; brownfield.md; requirements.md §3 (ambiguity) | missions 01–03, `docs/scenarios/*.md` |
| §5 Setup instructions | release.md §3 (smoke), `docs/SETUP-FACTORY.md` | `README.md`, `docs/SETUP-FACTORY.md` |
| §5 Testing approach, limitations, trade-offs | qa.md; release.md §7–§8 | `docs/TESTING.md`, `docs/qa/GAPS.md`, `docs/FINAL-SUMMARY.md` |
| §6 Evaluation — orchestration effectiveness; architecture quality; depth of decomposition; realism; validation rigor; defensible decisions; modular/testable/reliable/secure/scalable code with safe change management; engineering judgment | orchestration.md; architecture.md; decomposition.md; review.md §2–§3; architecture.md §1.6 and §10 (decisions); brownfield.md §3 (safe change); every guide's self-check | the evidence trail end to end: reviews, ledgers, drills, metrics, ADRs, decision briefs |
| AI-SDLC: Requirements Agent → user stories + acceptance criteria | requirements.md §1–§2 | slice `SPEC.md` |
| AI-SDLC: Design Agent → design document + diagrams | architecture.md §8–§9 | `docs/DESIGN.md`, slice `design.md`, `docs/diagrams/*.mmd` |
| AI-SDLC: Development Agent → error handling, logging, auditing, meaningful commits | java-spring.md §3; architecture.md §5; databases.md §4; `AGENTS.md` git rules | `src/main/java/dev/urlshort/web/`, ProblemDetail, ECS logs, `audit_log`, `git log` |
| AI-SDLC: Code Review Agent → review results proving all code was reviewed, issues, resolutions | review.md §2–§4 | `docs/review/<slice>/01-code-review.md`, `02-security-review.md`, `docs/review/REVIEW-LEDGER.md` |
| AI-SDLC: QA Agent → unit tests, unit + functional coverage reports, 100 % target, gaps | qa.md §1–§6 | `src/test`, `src/functionalTest`, `docs/qa/coverage/<slice>/{unit,functional,all}`, `docs/qa/TRACEABILITY.md`, `docs/qa/GAPS.md` |
