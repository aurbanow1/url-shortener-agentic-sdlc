# ADR-0012 — Client hash: HMAC-SHA256 under a random salt per UTC day, held in memory and dropped at the day's end

- Status: proposed (becomes accepted at the `02-analytics` plan-lock)
- Date: 2026-10-03
- Slice: `02-analytics`

## Context

NFR-P1 allows the client address to be stored only as a salted hash with a
daily-rotated salt. The slice SPEC's rule 4 makes the salt secret, random,
never logged and never exposed. The same address hashes to the same value
within one UTC day of uninterrupted service and to an unrelated value on
another day. A restart may start a fresh salt (A-10). A salt is not kept
after its day ends, so a stored hash cannot be recomputed from a guessed
address after that day. IPv4 has only 2³² addresses, so a plain or
predictable hash would be reversible by enumeration. The salt's lifetime is
the protection.

## Decision

- **Keyed hash.** `HMAC-SHA256(salt, address bytes)`, rendered as 64
  lowercase hex characters. It is never equal to the unsalted SHA-256 of
  the address, which AC-5 checks.
- **One salt per UTC day, in memory only.** `click.DailySalt` holds the day
  and 32 bytes from the application's `SecureRandom` bean in one field. The
  salt is never written to the database, a log, a response or a file.
- **The instant and the key are chosen together.**
  `stamp(address)` reads the application `Clock` under the salt's lock. If
  that instant's day differs from the held salt's, it zeroes the old bytes
  and draws a new salt for the day. It then copies the key, and the click
  takes its instant from the same call. A request that chose day D just
  before midnight finishes with D's key copy after D+1 has begun, and D+1's
  key is never replaced. The first draft let a request take its instant
  before entering the lock, so a delayed day-D request could rotate the salt
  back to D and break D+1's same-day stability (design review DR-02).
- **Dropped at the day's end, even on a quiet day.** Drawing a salt also
  schedules `expire(day)` at that UTC day's end
  (`CompletableFuture.delayedExecutor`). That zeroes and drops the salt only
  if it still belongs to that day, so a late callback for an earlier day is
  a no-op. Rotating only on the next click would leave yesterday's salt in
  memory until traffic resumed.
- **Concurrency.** Reading the clock, rotation and building the
  `SecretKeySpec` (which copies the key) happen under the object's lock.
  The HMAC itself runs outside it.
- **Restart.** Nothing is persisted, so a restart draws a new salt for the
  rest of the day. That is the SPEC's stated exception (A-10).
- **A clock that moves back across midnight** (an NTP step, or the
  functional suite shifting its clock) gets a fresh salt for the earlier
  day. It is the one remaining way one day can see two salts, and it errs
  toward unlinkable.

## Consequences

- Within one day, hashes link clicks from one address. Across days they do
  not, and after the day nothing can recompute them. FR-16 (unique visitors,
  mission 03) therefore gets per-day uniqueness at most, which is what NFR-P1
  permits.
- Whoever can read process memory during a day can test addresses against
  that day's hashes. Process compromise is outside this slice. `03-operate`
  runs the container non-root.
- A persistent or derived salt was rejected: either would keep the means to
  recompute past days.
- Verified before implementation:
  `missions/01-greenfield-core/slices/02-analytics/design-probe/output.txt`
  (S1 hash properties, S2 a salt created 0.5 s before midnight gone without a
  further click) and `…/design-probe/revision-output.txt` (DR-02: the
  reviewer's interleaving keeps same-day hashes equal; a stale expiry is a
  no-op).
