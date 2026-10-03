---
intent: >-
  Ship a production-grade URL shortener (create, redirect, analytics,
  reliability) through a governed agentic SDLC on OpenRig, where every stage's
  artifact, decision and verification is retained as evidence a reviewer can
  audit without running the factory.
---

# Project: urlshort

Two deliverables, one repository:

1. **The product** — a Spring Boot 4 / Java 21 URL-shortener service
   (`src/`), runnable standalone with `./gradlew check` and `java -jar`.
2. **The factory** — the orchestration layer that builds it (`rig/`,
   `project.yaml`, `missions/`, `tools/`), running on OpenRig.

Missions are the three assignment scenarios:

| Mission | Scenario | Outcome |
|---|---|---|
| `00-hello` | dry run | one trivial endpoint through every pipeline step and both human gates |
| `01-greenfield-core` | greenfield | create + redirect, analytics, reliability features |
| `02-brownfield` | brownfield | expiry + custom alias enhancement, a bug fix, fault-injection drills |
| `03-ambiguous-analytics` | ambiguous | "better analytics" resolved through an ambiguity log and a human decision |

The product requirements baseline — functional and non-functional, each row
tagged `stated` / `derived` / `assumed` — is `docs/REQUIREMENTS.md`; missions
allocate its ids, slice SPECs cite them, QA traceability carries them to tests.

Operating rules for anyone working here are in `AGENTS.md` (humans and
agents) and `rig/CULTURE.md` (the factory's constitution). The plan of record
is `PLAN.md`.
