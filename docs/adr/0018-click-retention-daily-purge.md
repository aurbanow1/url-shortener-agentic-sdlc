# ADR-0018 — Click retention: one DELETE per run, at startup and daily at 00:10Z, decided on the application clock

- Status: accepted at the `02-click-retention` plan-lock (2026-10-03); merged in `ed2b940`
- Date: 2026-10-03
- Slice: `02-click-retention` (mission 02)

## Context

NFR-P2 decided 90 days of click retention, enforced by a job. The human confirmed it at mission 02's
plan-lock: "purge stays here with the 90-day default as an operator setting" (transition 831). Mission
03's Q4 kept that delete (transition 876, "Q4 A").

The slice SPEC (`69680e4`) asks for the following:
- delete every click whose stored UTC day is before `T − P` (rule 2, A-1);
- run once at startup and once every UTC day at a fixed, recorded time, without overlap, a failed
  run retried by the next one (rule 3);
- one log event per run (rule 6);
- a period that stops startup when invalid (rule 1);
- no stall of redirects or click writes, with how that is bounded recorded here (*Non-functional*,
  NFR-M2);
- AC-8: the daily run must be shown to happen without a trigger when the suite-controlled clock
  passes its time.

The click table (ADR-0013) stores `clicked_on`. Its only index besides the keys is
`ix_click_link_day (link_id, clicked_on)`.

## Decision

- **The setting.**
  - `urlshort.click.retention-days`, a positive whole number of days, default 90. The environment
    variable is `URLSHORT_CLICK_RETENTIONDAYS`.
  - It is bound by `click.ClickRetentionProperties`, a `@Validated` record with `@Positive int`,
    like `RateLimitProperties`.
  - `0`, `-5` and `ninety` stop startup before any request is served or any click deleted. Boot's
    failure analysis names the setting, the rejected value and its origin, and no other
    configuration value (probe A4; SPEC AC-4 as amended at `69680e4`).
- **What a run deletes.**
  - `cutoff = UTC day of clock.instant() − P`, then `DELETE FROM click WHERE clicked_on < :cutoff`.
  - It is one statement per run, in its own transaction. `clicked_on` is the day the click
    recorder stored, so nothing is recomputed. The cutoff day itself is kept.
  - The statement scans the table. No index is added.
- **When.** One daemon thread, `click-purge`, owned by `click.ClickPurge` (a single-thread
  `ScheduledExecutorService`):
  - **Startup run.** On `ApplicationReadyEvent` the run is submitted to that thread and awaited, so
    it completes before Boot reports readiness (`ACCEPTING_TRAFFIC`). This was measured in probe A7.
  - **Daily run.** A fixed-delay tick every 5 s reads the application `Clock`. A run is due when the
    clock's UTC day is later than the last run's day and its time of day is 00:10Z or later. Every
    run records its day when it starts, whether it then succeeds or fails.
  - Consequences of that rule:
    - one run per UTC day;
    - a failed run is retried by the next day's run, never sooner;
    - a clock that steps back runs nothing until it passes the last run's day again;
    - a clock that steps forward runs once at the next tick;
    - a service started after 00:10Z has already covered that day with its startup run.
  - **No overlap.** The startup run, the daily runs and the functional suite's trigger all execute
    on the one thread, so they cannot overlap.
- **Failure.** A `RuntimeException` from the store is caught. It is one WARN `click purge failed`
  with `cutoff`, `retentionDays` and `errorType` (the class name only). The thread and the tick
  carry on.
- **Record of a run.** One INFO `clicks purged` with `deleted`, `cutoff` (the earliest UTC day kept,
  `YYYY-MM-DD`) and `retentionDays`. The event has no `requestId`, because a run is not a request.
  There is no audit row (SPEC A-4), and nothing else is logged.
- **Hold.** `urlshort.click.purge-enabled`, environment variable `URLSHORT_CLICK_PURGEENABLED`,
  default `true`. `false` pauses every deletion: no startup run, no daily tick, one WARN
  `click purge off` at every start.
  - Its reason is operational: keep clicks while an incident is investigated or under a legal
    hold. The lead made that a condition (2026-10-03T17:49Z), because a switch that exists only
    for tests was rejected for the rate limiter.
  - The functional profile's overlay sets it `false`, so no shared test context ever purges
    (design review DR-01, measured in probe A8off). The purge journeys turn it on in their own
    contexts.
- **Shutdown.** `@PreDestroy` stops the executor and waits up to 3 s for a run in progress. It never
  interrupts it.
  - **Budget** against compose's `stop_grace_period: 20s` (ADR-0017). After SIGTERM come the
    graceful phase (≤ 10 s, while the purge keeps running), then the bean destruction: the purge's
    wait (≤ 3 s) and the click drain (≤ 5 s, ADR-0011), one after the other. The worst case is
    18 s plus the pool close.
  - **Realistic case.** No request is in flight, so the graceful phase is about 0 s. A daily run
    takes about 1.2 s on a 1.3-million-row table (probe L7).
  - **Only a catch-up can outlast the wait**, and a catch-up runs in the startup run. If one does,
    `close()` returns at its deadline and the pool shuts down at once without waiting for the
    statement (D3: `close()` 2 034 ms, pool closed 9 ms later).
    - While the JVM lives, the statement finishes and commits, and clicks queued before the stop
      are written (D3: 20 of 20).
    - When the process then exits, H2 discards the uncommitted delete on the next open: the file
      reopened consistent with every old row still present, after a 4 s recovery. The next startup
      run repeats the delete (D4).
    - An interrupted run therefore loses nothing and keeps nothing half-done.

## Why not the alternatives (all measured: `missions/02-brownfield/slices/02-click-retention/design-probe/`)

| Alternative | Measured | Verdict |
|---|---|---|
| Spring `@Scheduled` / `CronTrigger` with `ThreadPoolTaskScheduler.setClock(…)` | armed at noon, suite clock moved past 00:10Z of the next day: **0 runs after 7 s** (S1). The trigger reads the clock when arming and then sleeps real time | cannot satisfy AC-8 by effect; it would also enable scheduling application-wide for one job |
| Batches of `DELETE … WHERE id IN (SELECT id … FETCH FIRST 10000 ROWS ONLY)` | H2 2.4.240 re-runs the subquery for every candidate row: one batch was still running at a 20 s query timeout (L0, `batch-subquery-jstack.txt`) | unusable on this H2 |
| Batches of "select ≤ 10 000 ids, then `DELETE … WHERE id IN (:ids)`" | 1 000 000 old rows: 43 s on one link (L2), 55 s over 2 000 links (L8), against 7.9 s and 32 s for one statement (L1, L10); a steady day 2.1 s against 1.2 s (L5, L7) | slower in every case; it would buy prompt stopping and a bounded transaction, which the budget above does not need at this size |
| An index `ix_click_day (clicked_on)` (one migration) | with one statement a catch-up got slower (9.5 s against 7.9 s, L4); with batches a steady day took 0.9 s against 1.2 s for one plain statement (L6, L7), and a catch-up 36 s against 32 s (L9, L10). Creating it on 1.3 million rows: 1.5 to 2.0 s | no measurable gain for the plain statement; costs a migration (NFR-X2) and a write on every click |
| A startup run after readiness, on the thread, not awaited | its INFO line can land inside the capture window of the shipped log-correlation journeys (`ColdStartJourneyTest`, `ObservabilityJourneyTest`), which require every line to carry the request's id (FR-13, AC-14) | rejected; awaiting it also makes AC-7 hold before readiness |
| A purge per request, per insert or per hour | — | more events than "one per day" (rule 3), and work on the Visitor's path |
| Accepting a run at a real 00:10Z inside a shipped journey's log window as unlikely (the first version) | design review DR-01 reproduced the failure: six shipped classes assert a request id on every captured line | rejected; the hold in the test overlay makes it impossible |
| Filtering `ClickPurge` events inside those six classes | changes shipped tests outside the slice; every future background job would need the same filter | rejected in favour of the hold |

## Consequences

- **The lock behaviour holds by MVCC, not by batching.** Redirects stayed at p95 ≤ 2.5 ms on one
  link and ≤ 7.4 ms over 2 000 links (L1, L7, L10). No redirect failed, and every redirect's click
  was stored after the run in every variant.
  - H2's MVStore takes row locks only on the rows a statement deletes. The rows being inserted
    and read are different rows.
  - On the same machine, redirects during a deletion were sometimes faster than the baseline
    measured just before it, so these are indicative figures, not a benchmark. NFR-L1 is still
    measured by `03-operate`'s bench at `release_prep`.
- **Ceiling.** Each run is one transaction and one table scan.
  - A steady day costs about 1.2 s at 1.3 million rows and grows with the table.
  - A catch-up after the period is lowered, or after a long outage, costs 8 to 32 s per million
    deleted rows, depending on how scattered they are over links (L1, L10). It delays readiness
    by that long.
  - Compose marks the service unhealthy after a 20 s start period plus 6 × 10 s of failed checks.
    That stays safe up to roughly 2.5 million deleted rows in one startup run.
  - `// ponytail:` comment in `ClickPurge`: past that, add `ix_click_day` and id-range batches
    stopped between batches (L6 and L9 measured both). Batches with a subquery in the `DELETE` stay
    unusable on H2 (L0).
- **Clock.** A forward step of the host clock deletes up to that many days early, and a host
  clock years ahead deletes every click. The INFO event's `cutoff` shows it. The Operator owns
  the host clock. This is an accepted residual (slice design §6). A backward step deletes nothing.
- **Shutdown.** The purge adds at most 3 s after the graceful phase. The purge never interrupts
  its `DELETE`. The click writer, by contrast, interrupts after its drain deadline and reports a
  running insert as outcome unknown (ADR-0011).
- **Tests.**
  - No shared functional context purges (the hold in the overlay), so no purge line can enter a
    shipped journey's capture window.
  - The functional suite triggers the run the scheduler would start through
    `ClickPurge.runNow()`. It is package-private and submits to the same thread.
  - AC-8 is proven without a trigger: the suite clock is moved past 00:10Z of the next day, and
    the tick runs within 5 s (probe A8b: 4.9 s).
- **Not changed:** the click table, the statistics query, the API, the metrics.
- **Rollback.** Revert the merge. Purged rows are gone by design. The Operator copies `data/`
  while the service is stopped before lowering the period, if an undo is wanted.
- Verified before implementation, in `missions/02-brownfield/slices/02-click-retention/design-probe/`:
  `output.txt` (S1, A8, A4, L0–L4, D1, D2), `output-2.txt` (L5–L10, A7), `output-3.txt` (D3) and
  `output-4.txt` (D4) and `output-5.txt` (A8off, the hold). The probe implements the purge as specified here, with switches for the
  rejected variants.
