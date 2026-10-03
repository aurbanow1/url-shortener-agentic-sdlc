# ADR-0005 — Persistence: H2 in PostgreSQL mode, Flyway-owned schema, Spring Data JDBC and `JdbcClient`

- Status: accepted at the `01-create-redirect` plan-lock (2026-10-03T06:38Z)
- Date: 2026-10-03
- Slice: `01-create-redirect` (first tables: `link`, `audit_log`)

## Context

`01-create-redirect` stores the first resource and the first audit rows.
`AGENTS.md` fixes embedded H2 under `data/` and Flyway migrations; ADR-0001
covers only the stack baseline. The guides (`docs/guidance/databases.md`,
`java-spring.md` §3) ask the first persisting slice to record the engine, the
access technology and the schema conventions every later migration follows,
so that a move to PostgreSQL stays a configuration change.

## Decision

- **Engine.** H2 file database under `data/` with `MODE=PostgreSQL` in
  production (`spring.datasource.url` as shipped); in-memory H2 with the same
  mode in both test suites. No vendor-specific SQL (`ON CONFLICT`,
  `RETURNING`, arrays, JSONB operators, regex `CHECK`s) in any migration or
  query; column and table names avoid words reserved in either engine
  (`KEY`, `VALUE`, and the keywords `AT`, `BEFORE`, `AFTER`).
- **Schema ownership.** Flyway only. Migrations live in
  `src/main/resources/db/migration/V<n>__<verb>_<noun>.sql`; `V1` is the
  baseline (`V1__create_link_and_audit_log.sql`); each later migration does one
  thing, is immutable once merged, and carries its rollback as a comment in the
  file and in the slice's `design.md`. Destructive changes follow expand →
  migrate → contract. Flyway runs at startup in every profile, so the functional
  suite tests every migration on every run and readiness stays down until the
  schema is current.
- **Access.** Spring Data JDBC for aggregates with simple CRUD: Java records
  with `@Id`, repositories declared as `Repository<T, ID>` sub-interfaces that
  expose only the methods a slice uses, derived finders for business keys, and
  `@Modifying @Query` for **targeted** updates (`UPDATE … SET one column WHERE
  id = :id AND <guard>`) so a re-read aggregate is never saved back over a
  concurrent change. `JdbcClient` with named parameters where a component is
  one explicit statement (the insert-only audit writer). No JPA/Hibernate:
  explicit SQL, no lazy loading, no second-level state.
- **Conventions.** `snake_case`, singular table names; surrogate
  `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, never exposed; public
  identifiers in their own `UNIQUE` column; `NOT NULL` wherever true; `CHECK`
  for ranges; state that is a one-way transition is a nullable
  `TIMESTAMP WITH TIME ZONE` (`retired_at`) rather than an enum plus
  `updated_at`; opaque payloads (audit before/after) are JSON text in
  `VARCHAR`, never a JSON column type and never queried by field; no foreign
  key from `audit_log` to the audited table.
- **Time.** Every stored instant is a UTC `Instant` taken from the application
  `Clock` bean (`Clock.tickMillis(ZoneOffset.UTC)`, defined in
  `dev.urlshort.link.LinkConfig`); no database-side defaults for business
  time. Millisecond ticks make a value identical before and after a round
  trip through a microsecond column, which the API's byte-identical read
  contract depends on. Tests replace the bean to control time.
- **Transactions.** The service method is the boundary (`@Transactional` on
  the use case, `readOnly = true` for reads); the audit row is written inside
  it; a failed audit write rolls the change back (ADR-0008). Races on
  business keys are decided by unique constraints, not by application checks.

## Consequences

- A PostgreSQL run is a datasource change plus a Testcontainers job; until one
  exists the gap is recorded in `docs/qa/GAPS.md` by the first slice that
  claims portability, not assumed.
- `GENERATED ALWAYS AS IDENTITY` means inserts must omit the id column; Spring
  Data JDBC does so for a null `@Id`. Code that needs the generated id reads it
  from the returned aggregate.
- Later slices add tables with their own `V<n>` and record the queries they
  run with the index that serves each (`databases.md` §3).
- Reading `Instant` back from H2 relies on the driver's timestamp conversion;
  the functional suite proves it on the real migration.
