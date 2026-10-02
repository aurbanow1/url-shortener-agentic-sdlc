# urlshort — working agreement for agents and humans

## Build and run

```sh
source scripts/env.sh        # JDK 21 + repo-local Gradle home (machine default JDK is 11)
./gradlew check              # the quality gate: unit + functional suites + JaCoCo 100% line/branch
./gradlew test               # unit suite only (fast inner loop)
./gradlew functionalTest     # HTTP journeys against a temp H2 database
./gradlew bootRun            # http://localhost:8080  (health: /actuator/health)
./gradlew bootJar && java -jar build/libs/urlshort.jar
```

Sandboxed seats (Codex) build with `./gradlew --offline check`; the cache in
`.gradle-home/` is pre-warmed. If Gradle complains about the JDK, you forgot
`source scripts/env.sh`.

## Stack facts that are easy to get wrong

- Spring Boot **4.1.1** on Java 21. Starters were renamed in Boot 4:
  `spring-boot-starter-webmvc`, `-data-jdbc`, `-flyway`, `-validation`,
  `-actuator`, each with a matching `-test` starter. Do not write Boot 3
  coordinates from memory; read `build.gradle.kts`.
- Persistence: Spring Data JDBC + H2 (file DB under `data/`, WAL-free, zero ops),
  schema owned by Flyway migrations in `src/main/resources/db/migration/V<n>__*.sql`.
- Errors: RFC 9457 `ProblemDetail` everywhere (`@RestControllerAdvice`); never a
  stack trace in a response.
- Logging: structured JSON (Spring Boot structured logging), every request line
  carries `requestId`; no raw IPs or user agents in logs.
- Tests: JUnit 5. `src/test/java` = unit, `src/functionalTest/java` = HTTP
  journeys (`@SpringBootTest` + `MockMvc`/`RestTestClient`). Coverage is enforced
  at 100% line and branch by `jacocoTestCoverageVerification`; an honest
  exclusion is documented in `docs/qa/GAPS.md`, never silently configured.

## Repository map

| Path | What |
|---|---|
| `src/` | the product |
| `missions/<mission>/` | mission `SPEC.md`, `NOTES.md`, `mission.yaml`; `slices/<slice>/{SPEC.md,design.md,PROGRESS.md,PROOF.md,proof/,slice.yaml}` |
| `docs/` | architecture, design, governance, testing, risks, review results, QA coverage, metrics, evidence exports, ADRs |
| `rig/` | the orchestration layer: rig spec, culture, role specs, workflow specs |
| `tools/` | evidence export, metrics, graph rendering (node scripts, no build step) |
| `.worktrees/<slice>/` | per-slice git worktree on branch `slice/<slice>` (code lives here while a slice is in flight) |

## Git rules

- `main` is written only by the orchestration lead (merges `--no-ff`, mission files).
- Slice code is developed in `.worktrees/<slice>` on branch `slice/<slice>`.
- Mission/slice markdown and `docs/` evidence are edited in the main checkout and
  committed with an explicit pathspec, e.g.
  `git commit -m "docs(01-create-redirect): QA coverage reports" -- docs/qa missions/01-greenfield-core/slices/01-create-redirect`.
  If `.git/index.lock` exists, another seat is committing: wait two seconds and retry.
- Conventional commits, scope = slice id: `feat(01-create-redirect): …`,
  `test(…)`, `fix(…)`, `docs(…)`, `chore(…)`. Messages describe the change and why
  it is correct — never who approved it or which gate it passed.
- Every agent commit ends with a trailer naming the runtime that wrote it:
  `Co-Authored-By: Claude <noreply@anthropic.com>` or
  `Co-Authored-By: Codex <noreply@openai.com>`.
- Never: `git push`, force-push, `reset --hard` on `main`, history rewrites,
  tags other than `slice/<id>/accepted`, committing secrets or `.pdf` files.
