# Database guidance

Applies to every slice that adds a table, a column, an index or a query.
Embedded H2 in PostgreSQL mode today (zero ops for the prototype; the first
slice that creates a table records this in its persistence ADR — ADR-0001
covers only the stack baseline); every rule below keeps a later move to
PostgreSQL a configuration change, not a rewrite.

## 1. Schema ownership

- Flyway owns the schema. Migrations live in `src/main/resources/db/migration/V<n>__<verb>_<noun>.sql` (`V2__create_link.sql`, `V3__add_link_expires_at.sql`). The application never issues DDL.
- Migrations are immutable once merged; a change is a new version. `repair` is a human act, documented.
- Each migration does one thing, is reviewed with its rollback written in the slice's `design.md` (`-- rollback: DROP COLUMN …` as documentation; destructive rollbacks are also scripted under `db/rollback/` when the change is risky).
- Zero-downtime discipline even for a prototype: expand (add nullable column / new table) → migrate data → contract (drop old) as separate migrations, each independently shippable.
- Readiness stays down until migrations have run (Boot does this by default when Flyway runs at startup; do not disable it).

## 2. Modelling

- Table and column names `snake_case`, singular table names (`link`, `click`, `audit_log`).
- Surrogate primary key `id BIGINT GENERATED ALWAYS AS IDENTITY`; natural identifiers (short `code`, `alias`) are separate columns with `UNIQUE` constraints and the right collation/case rule stated in the SPEC.
- **Audit columns on every table, no exceptions** (policy 2026-10-03): `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP` and `updated_at TIMESTAMP WITH TIME ZONE NOT NULL` (set equal to `created_at` on insert and maintained by the repository on every write — a trigger is acceptable when the engine supports it portably), plus `created_by`/`updated_by` (the acting principal: an operator, `anonymous`, or `system` for jobs) wherever an actor exists. Append-only event tables keep their domain timestamp as well (`clicked_at`, `occurred_at`) — the audit columns say when the *row* was written, the domain column says when the *event* happened. Soft deletes add `deleted_at` and every read filters on it (one place: the repository). A table that lacks these columns gets them in the next migration that touches it (expand step, with rollback); until then it is a `docs/qa/GAPS.md` entry, not a silent exception.
- Constraints express the rules: `NOT NULL` everywhere it is true, `CHECK` for enumerations and ranges, `UNIQUE` for business keys, foreign keys with explicit `ON DELETE` behaviour. If a rule cannot be a constraint, the service enforces it inside the same transaction and a test proves the race (two inserts, one wins).
- No JSON columns for data you will query or constrain; JSON only for genuinely opaque payloads (audit `before`/`after`).
- Keep PII out: store salted hashes (`ip_hash`), not addresses; document the salt rotation and what becomes unlinkable.

## 3. Indexes and queries

- Every query the service runs is listed in `design.md` with the index that serves it; an index exists because a query needs it, not "just in case" (writes pay for every index).
- Lookups by business key (`code`, `alias`) hit a unique index; time-range queries (clicks by day) get a composite index `(link_id, clicked_at)`.
- Pagination by keyset, never `OFFSET` on growing tables.
- Aggregations for analytics are either computed from indexed event rows at request time (small data) or maintained in a summary table updated in the same transaction (say which, and why, in the design).
- No N+1: load a list and its children in two queries or one join; the review checks query counts for list endpoints.

## 4. Transactions and consistency

- The service method is the transaction boundary; one use case, one transaction; read-only transactions marked.
- Idempotent writes: a retried create must not duplicate — either a unique constraint plus `409`, or an `Idempotency-Key` table that stores the first response.
- Audit rows are written in the same transaction as the change; an audit write that fails rolls the change back.
- Isolation: default (`READ COMMITTED`) unless a test shows a race; then the fix is a constraint or `SELECT … FOR UPDATE` on the specific row, documented.
- Clock: use the database clock for `created_at` defaults and the application `Clock` for business time (expiry); never mix within one rule.

## 5. H2 today, PostgreSQL tomorrow

- `MODE=PostgreSQL` keeps syntax close, not identical: avoid vendor features (arrays, `ON CONFLICT`, `RETURNING`, JSONB operators) unless the migration is written for both and tested on both.
- Keep a single `V1__baseline` that runs on both engines; CI for a real PostgreSQL run is a brownfield candidate (Testcontainers), recorded in `docs/qa/GAPS.md` until done.
- File-mode H2 under `data/` is the prototype's durability; back up by copying the file while stopped; the container mounts a volume for it.

## 6. Data lifecycle and compliance

- Retention is a requirement, not an afterthought: every event table states how long rows live and the job (or migration) that enforces it.
- Right to delete: a user-facing delete removes or anonymises personal data and leaves an audit row stating that it happened.
- Audit log is append-only: no `UPDATE`/`DELETE` grants in production; the admin read endpoint is read-only and paginated.

## 7. Testing the data layer

- Repository logic is covered through the functional journeys (real migrations, real SQL); dedicated repository tests (`@DataJdbcTest`) only for queries with branches worth isolating.
- Every constraint that encodes a rule has a test that violates it and asserts the mapped problem detail (`409` for uniqueness, `400` for check violations surfaced as validation).
- Migrations are tested by running them: the functional suite starts from an empty schema every run; a failing migration fails the suite.
- Test data through small builders, not shared fixtures; each test creates what it needs.

## 8. Review checklist (`design_review`, `security_review`)

- DDL complete with constraints, indexes justified by listed queries, rollback written.
- Time columns are timezone-aware UTC; identifiers surrogate + unique business key.
- Every table in the DDL has `created_at` and `updated_at` (and `created_by`/`updated_by` where an actor exists); a missing audit column is a HIGH finding unless a GAPS entry names the migration that adds it.
- PII hashed or absent; audit row in the same transaction; retention stated.
- Queries parameterised (Spring Data JDBC / `JdbcClient` named parameters) — no string-built SQL.
- Portability: no vendor-only SQL without a dual-engine test.
