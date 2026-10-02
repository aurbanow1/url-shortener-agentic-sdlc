# ADR-0001 — Spring Boot 4.1 on Java 21, built with Gradle

- Status: accepted (baseline fixed at bootstrap, recorded by slice `01-ping`)
- Date: 2026-10-02

## Context

`urlshort` needs a mainstream, supported Java stack that gives RFC 9457
problem details, structured JSON logging, health probes, schema migrations and
Bean Validation out of the box, so that slices add product code rather than
plumbing. The quality gate is 100 % line and branch coverage over two test
suites, which the build tool has to enforce without custom tooling.

## Decision

- **Spring Boot 4.1.1** (Spring Framework 7.0.x, Jackson 3.x, Jakarta Servlet
  6.1) with the Boot 4 starters: `spring-boot-starter-webmvc`, `-data-jdbc`,
  `-flyway`, `-validation`, `-actuator`, each with a `-test` twin.
- **Java 21** via the Gradle toolchain (`languageVersion = 21`).
- **Gradle (Kotlin DSL)** with two JVM test suites, `test` (unit) and
  `functionalTest` (HTTP journeys), JaCoCo reports per suite plus merged, and
  `jacocoTestCoverageVerification` at `LINE` and `BRANCH` ≥ 1.0 wired into
  `check`.
- All builds go through `scripts/gw`, which pins JDK 21 and the repo-local
  Gradle home.

## Consequences

- Boot 4 renamed starters and relocated auto-configuration and test-slice
  classes into per-technology modules; the exact names in use are recorded in
  `docs/DESIGN.md` §Stack conventions. Nobody writes Boot 3 coordinates or
  imports from memory.
- Jackson 3 lives under `tools.jackson.*`; annotations stay under
  `com.fasterxml.jackson.annotation`. Jackson 2 support is deprecated.
- `@SpringBootTest` alone no longer configures `MockMvc`; tests add
  `@AutoConfigureMockMvc`.
- Coverage shortfalls are written to `docs/qa/GAPS.md`; exclusions in
  `build.gradle.kts` are not an option.
