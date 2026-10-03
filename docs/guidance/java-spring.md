# Java 21 and Spring Boot 4 practices for this codebase

Facts first (they changed recently), then practices with their checks.

## 1. Stack facts that are easy to get wrong

- **Spring Boot 4.1.x** on **Spring Framework 7**, **Java 21**, Gradle 9 with the Kotlin DSL. Starters were renamed and modularised: `spring-boot-starter-webmvc`, `-data-jdbc`, `-flyway`, `-validation`, `-actuator`, each with a matching `-test` starter. Read `build.gradle.kts`; do not type Boot 3 coordinates from memory.
- **Jackson 3** lives in the `tools.jackson` packages (`tools.jackson.databind.ObjectMapper`); the `com.fasterxml.jackson` names are the old major. Dates serialise as ISO-8601 strings by default — prefer `String`/`Instant` fields and let the contract state the format.
- Test slices moved with the modules: `@AutoConfigureMockMvc` is in `org.springframework.boot.webmvc.test.autoconfigure`; `@SpringBootTest` is unchanged; `RestTestClient` is the new fluent HTTP test client in Spring Framework 7; `MockMvc` still works and is what the functional suite uses.
- Problem details: `spring.mvc.problemdetails.enabled=true` makes the platform's `ProblemDetailsExceptionHandler` answer framework errors (405, 404, 400 from binding); a project `@RestControllerAdvice` is only for domain exceptions (ADR-0002).
- Structured logging: `logging.structured.format.console=ecs` renders one JSON object per line; MDC entries become top-level fields (ADR-0004).
- **Classpath property shadowing**: a `src/<suite>/resources/application.properties` *replaces* the production `application.properties` on that suite's classpath. Suite overrides go in `application-<profile>.properties` with the profile activated by the Gradle task (`systemProperty("spring.profiles.active", "functional")`) — the DR-01 lesson, now the rule.

## 2. Language

- Records for request/response bodies and value objects; immutability by default; `final` fields.
- Sealed interfaces and pattern matching when a type genuinely has a closed set of variants; otherwise a plain class.
- `Optional` as a return type for "may be absent", never as a field or parameter.
- No checked exceptions for domain failures: a small sealed/abstract `DomainException` hierarchy mapped to problem details once, in one advice.
- Time: `Instant`, `Duration`, `Clock` — never `LocalDateTime` for an instant, never `new Date()`.
- Null: Bean Validation at the boundary, non-null by construction inside; JSpecify annotations only where a nullable value is real.
- Keep methods short enough to name honestly; a comment explains *why*, never *what*. A deliberate ceiling is marked `// ponytail: <ceiling>, <upgrade path>`.

## 3. Spring

- Constructor injection only; no field injection; no `@Autowired` on the single constructor.
- Controllers: `@RestController`, request records with Bean Validation (`@NotBlank`, `@Size`, `@Pattern`), `@Valid` on the parameter, return `ResponseEntity` only when status or headers vary; `201` creates set `Location`.
- Services own transactions: `@Transactional` on the use-case method, read-only variants where applicable; never on controllers or repositories.
- Persistence: Spring Data JDBC aggregates for simple CRUD; `JdbcClient` for anything with a non-trivial query; no JPA/Hibernate here (rationale, explicit SQL and no lazy-loading surprises, is recorded in the persistence ADR of the first slice that touches a table; ADR-0001 covers only the stack baseline).
- Configuration: `@ConfigurationProperties` records bound with `@EnableConfigurationProperties` or `@ConfigurationPropertiesScan`; validated with Bean Validation; defaults in the record.
- Filters for cross-cutting HTTP concerns (`OncePerRequestFilter`, ordered explicitly); interceptors only for controller-level concerns.
- Logging through SLF4J with parameters (`log.info("ping", kv("code", code))` or MDC), never string concatenation; never log request bodies, headers or client addresses.
- No `@Async`, scheduling, caching or messaging until a SPEC needs it and an ADR records it.

## 4. Testing in Spring (see `qa.md` for strategy)

- Unit tests (`src/test/java`): plain JUnit 5 + AssertJ, no Spring context; test the rule, not the framework. Filters and small components get direct unit tests with mock servlet objects.
- Functional tests (`src/functionalTest/java`): `@SpringBootTest` + `@AutoConfigureMockMvc`, one class per feature journey, one test per AC named after it, real Flyway migrations on an in-memory H2 (`functional` profile), shipped logging on so log assertions are real (`OutputCaptureExtension` or an in-memory appender).
- Keep the context cacheable: same configuration across functional tests → one context start per suite run.
- Never `@MockBean` the thing under test; mock only true externals (there are none yet).
- Coverage is enforced at 100 % line/branch over both suites; an uncovered branch is a missing test or dead code — decide which, never configure it away.

## 5. Gradle

- The build is `build.gradle.kts`; dependency versions come from the Boot BOM — do not pin what Boot manages.
- Two JVM test suites (`test`, `functionalTest`); JaCoCo reports per suite and merged; `check` is the gate.
- Build only through `scripts/gw` (JDK 21 pinned, repo-local Gradle home; `--log <file>` for a record). Never commit `build/`, `.gradle*`.
- A new dependency requires an ADR and a line in `docs/DESIGN.md`; prefer what the JDK or Boot already ships.

## 6. Code review checklist for Java/Spring (used by `code_review`)

- Every AC has a named test that would fail if the behaviour broke.
- No business logic in controllers; no HTTP types below the controller; no SQL in services.
- Validation at the boundary; problem details for every error path; no stack traces or class names in bodies.
- Logs: structured, parameterised, PII-free, request id present via MDC.
- Transactions on service methods; audit row written in the same transaction as the change.
- Records/immutability; `Instant` for time; no `Optional` fields.
- Dependencies: none added without an ADR; Boot-managed versions.
- `ponytail-review` lens: `delete:` / `stdlib:` / `native:` / `yagni:` / `shrink:` findings — the diff's best outcome is getting shorter.
- Tests deterministic (no sleeps, no wall-clock races, random ports), readable (Arrange/Act/Assert), named after behaviour.

## 7. Pitfalls we have already paid for

- Suite-level `application.properties` shadowing production config (fixed by profile overlays).
- Calling the application `main` from a test to cover it: fine for the bootstrap, but keep `main` to one line.
- Assuming the request id survives the error path: it does only because the filter sets the header *before* `chain.doFilter` and runs first in the chain — keep the order explicit.
- H2 "PostgreSQL mode" is not PostgreSQL: avoid vendor SQL; keep migrations portable; plan a real PostgreSQL run before any production claim.
- Tomcat names its worker threads after the connector's bound address (`http-nio-127.0.0.1-18081-exec-5`), and ECS structured logging emits the thread name as `process.thread.name`. A "no addresses in logs" criterion is therefore violated by server metadata as soon as the app binds to an explicit IP. Either exclude the member from the structured log or never assert address absence on a loopback-bound capture — decide at design time, not at QA (QA-01 on `01-ping`).
