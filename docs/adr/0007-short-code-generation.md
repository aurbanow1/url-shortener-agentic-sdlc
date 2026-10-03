# ADR-0007 — Short codes: 8 random characters from `[A-Za-z0-9]`, unique by constraint

- Status: proposed (becomes accepted at the `01-create-redirect` plan-lock)
- Date: 2026-10-03
- Slice: `01-create-redirect`

## Context

Business rule 1 of the slice SPEC bounds generated codes to
`^[A-Za-z0-9]{6,32}$`, case-sensitive, unique forever, unpredictable, and never
equal to a first path segment the service serves (`api`, `actuator`, `v3`,
`swagger-ui`, `error`). The exact length and strategy are left to the design.

## Decision

- **Length 8, alphabet `[A-Za-z0-9]`** (62 symbols, 62⁸ ≈ 2.2 × 10¹⁴ codes,
  47.6 bits). Short enough to type, large enough that enumeration is
  impractical at any realistic table size (one in 2 × 10⁸ guesses hits at a
  million links).
- **Source of randomness: `java.security.SecureRandom`**, one bean, injected
  into `dev.urlshort.link.ShortCodes` as `java.util.Random` so a unit test can
  script it. No counter, timestamp or hash of the target is involved, so a
  code reveals nothing about earlier codes or about the URL.
- **Reserved segments** are refused at generation: a draw equal to one of
  `api`, `actuator`, `v3`, `swagger-ui`, `error` is redrawn. At length 8 only
  `actuator` is reachable; the whole set is checked so a later change of
  length cannot reintroduce the collision.
- **Uniqueness is the database's**: `UNIQUE (code)` on `link`. The service does
  not pre-check. A collision fails the insert and the request answers `500`
  (fail closed, nothing stored); the client retries. `// ponytail: collision
  answers 500; add an existsByCode retry if the table nears 10⁹ rows`.
- **Matching is case-sensitive** (`/Abc123` ≠ `/abc123`): the route pattern
  and the `UNIQUE` index both compare bytes.
- The generated namespace is a strict subset of the alias charset the product
  baseline reserved for custom aliases (`[A-Za-z0-9_-]{4,32}`); if aliases
  ever return to the plan, generated codes and aliases share one column and
  one uniqueness rule without migration.

## Consequences

- Codes are not sortable by creation time and carry no meaning; that is a
  property, not a limitation.
- A shorter code or a sequential scheme would need a new decision here and a
  review of the enumeration row in the threat model.
