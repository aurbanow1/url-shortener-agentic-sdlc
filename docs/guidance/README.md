# Engineering guidance library

Opinionated, actionable practice guides for this project. They encode *how we
build well* (kind knowledge) and sit beside the role contracts in
`rig/agents/*/guidance/role.md`, which say *who does what*. Guides are read on
demand at the step that needs them; they are not a reading list to recite.

| Guide | Who reads it | When |
|---|---|---|
| [requirements.md](requirements.md) — capturing requirements that an agent can build from | Requirements Agent; Review Agent (`requirements_review`); Planning | before writing or reviewing a `SPEC.md`; at `decompose` |
| [architecture.md](architecture.md) — structure, boundaries, API and data design, security, reliability, ADRs, diagrams | Design Agent; Review Agent (`design_review`, `wave_review`); Planning | before writing or reviewing a `design.md`; at `decompose` |
| [java-spring.md](java-spring.md) — Java 21 and Spring Boot 4 practices for this codebase | Development Agent; Design Agent; Review Agent (`code_review`) | before the first line of code on a slice; while reviewing code |
| [databases.md](databases.md) — schema ownership, migrations, modelling, transactions, data lifecycle | Design Agent; Development Agent; Review Agent (`design_review`, `security_review`) | any slice touching a table or a query |
| [qa.md](qa.md) — test strategy, by-effect verification, coverage policy, evidence, findings | QA Agent; Development Agent (tests first); Review Agent (`code_review` audits QA) | before writing tests; before `qa_check`; before judging proof items |

Rules of the library:

- A guide states a practice, the reason, and the check that proves it was
  followed. No practice without a reason; no reason without a check.
- Guides are versioned with the code. When a review finds the guide was wrong
  or incomplete, the fix is a commit to the guide, not a workaround in the slice.
- Conflicts resolve in this order: the SPEC's acceptance criteria → an accepted
  ADR → these guides → the vendored skills (`ponytail`, TDD, review-team).
