# ADR-0011 — Click recording: reduced on the request thread, written by one bounded writer, fail open

- Status: accepted at the `02-analytics` plan-lock (2026-10-03T09:48Z); amendment proposed by `02-click-retention` (see *Amendment*)
- Date: 2026-10-03
- Slice: `02-analytics`

## Context

FR-7 makes every `302` redirect a click, and NFR-L3 allows recording it to
add at most 2 ms p95 to the redirect. The slice SPEC (rules 5 and 6, A-11,
A-12) requires four things. The redirect never waits for, changes or fails
because of the click store. A click that cannot be recorded is lost with
exactly one `WARN` carrying the redirect's `requestId` and no client value.
A recorded click shows in the statistics within 5 s under normal load. No
click is lost under normal concurrent load (AC-16). `java-spring.md` allows
asynchronous work only with an ADR. The slice owns `click/` and a one-line
hook in `link/`, but not `application.properties` or `build.gradle.kts`.

## Decision

- **The hook** in `link.RedirectController` calls
  `click.ClickRecorder.record(linkId, request)` after `resolve` and before
  the `302` is built. `record` returns at once for `HEAD`: the `GET` mapping
  serves `HEAD` too, and rule 1 says a `HEAD` is not a click.
- **Reduction happens on the request thread.** `record` reads the peer
  address, `User-Agent`, `Referer`, the service clock and the MDC
  `requestId`, then reduces them to the stored facts (ADR-0012, ADR-0013).
  Only the reduced `Click` and the request id are queued. Tomcat recycles the
  request object after the response, so nothing may read it later, and no
  raw value sits in the queue.
- **One writer, bounded queue, abort on full.** A field
  `ThreadPoolExecutor(1, 1, 0, ms, new ArrayBlockingQueue<>(10_000),
  daemon "click-writer", AbortPolicy)`. One writer keeps FIFO order and
  drains thousands of single-row inserts per second on H2. Ten thousand
  queued clicks hold about 23 MB at worst. Never `CallerRunsPolicy`, which
  would run the insert on the redirect thread exactly when the store is
  slowest.
- **Fail open, one WARN per lost click.** `record`'s whole body is one `try`:
  any `RuntimeException`, including `RejectedExecutionException` from a full
  or closed queue, logs `click lost` (reason `rejected`) and returns. The
  writer task catches a failed insert the same way (reason `write failed`).
  Nothing is retried. The WARN carries `requestId`, `reason` (`rejected`,
  `write failed`, `shutdown deadline` or `shutdown deadline, outcome
  unknown`) and, when there is an exception, `errorType` (its class name),
  never the exception's message. A driver message can quote the bound client hash or
  referrer (the lesson of `01-create-redirect`'s DR-01).
- **Correlation after the response.** The writer task puts the captured
  request id into the MDC for its duration and removes it in `finally`
  (ADR-0004 amendment).
- **Shutdown, bounded, every click accounted before `close()` returns.**
  Each queued `ClickWrite` carries an atomic state (`QUEUED`, `RUNNING`,
  `DONE`, or `CLAIMED` by shutdown) and sits in a set of outstanding clicks
  from `record` until the worker finishes it. Whoever moves a click's
  state first with `compareAndSet` owns its report.
  - `@PreDestroy close()` calls `shutdown()` and waits at most 5 s
    (`awaitTermination`).
  - If writes remain, `shutdownNow()` stops the worker and any `settle()`
    task found is cancelled. Then shutdown claims every outstanding click:
    a queued one is reported `click lost`, reason `shutdown deadline`; one
    being written is reported `shutdown deadline, outcome unknown`, because
    an H2 write can ignore the interrupt and still commit later. Each report
    carries the click's own request id.
  - A worker that later finishes or fails finds the claim and reports
    nothing; a worker that had not yet started finds it and skips the
    write.

  Every report is made inside `close()`, so a normal JVM exit afterwards
  cannot lose one even while the daemon writer is still stuck. The
  recorder depends on the store and so on the `DataSource`, which Spring
  destroys after the recorder. The first draft called
  `ExecutorService.close()`, which has no deadline (design review DR-01: a
  six-write backlog held context close for about 12 s). The second relied
  on the pool close to end a stuck write, and the re-review disproved that:
  a child JVM exited with 5 of 6 clicks accounted. 5 s fits `03-operate`'s
  budget of a 10 s shutdown phase, then the drain and the pool close, inside
  a 20 s stop grace. An abrupt stop loses what is queued (A-11).
- **A test seam, not a feature.** A package-private `settle()` submits an
  empty task and waits up to 10 s. With one FIFO writer, that means every
  earlier click has been written or lost. It is the "flush" the SPEC's
  definition of "settled" allows. One acceptance test (AC-1) polls to the 5 s
  bound instead, so rule 6 is proven on its own.

## Consequences

- The redirect pays microseconds: 20 µs at p50 and 36 µs at p95 on the
  reference laptop, an indicative probe figure. NFR-L3's number is still
  measured by `03-operate`'s bench at `release_prep`, or recorded as a gap.
- Under sustained overload or a dead store, clicks are lost and each loss is
  one log line. `03-operate`'s rate limit bounds the rate of both.
- Visibility is "after the writer catches up". Under normal load that is
  milliseconds. Under overload the queue absorbs up to 10 000 clicks before
  dropping.
- A second asynchronous feature would justify a shared executor
  configuration; until then the executor is one field in `ClickRecorder`.
- Verified on Tomcat before implementation:
  `missions/01-greenfield-core/slices/02-analytics/design-probe/output.txt`
  (C2 `HEAD`, C4 hook time, C7 slow store, C8 failing store). The bounded
  close is in `…/design-probe/revision-output.txt` (DR-01: the reviewer's
  six 2-s writes, close returned at 5 006 ms, every click accounted once).

## Amendment — `02-click-retention` (2026-10-03, proposed; accepted at that slice's plan-lock)

The *Consequences* above said a second asynchronous feature would justify a shared executor
configuration. The click purge (ADR-0018) is that second feature. It keeps the pattern instead:

- **One more owned executor, not a shared one.** `click.ClickPurge` holds a single-thread
  `ScheduledExecutorService` with one daemon thread, `click-purge`, as a field, the way
  `ClickRecorder` holds `click-writer`. The two share nothing: the writer is a bounded queue on
  the Visitor's path, and the purge is one timed job.
- **Still no scheduler framework.** Spring's `@Scheduled` with a cron trigger cannot follow the
  suite-controlled clock: it arms against the clock and then sleeps real time (ADR-0018, probe S1).
  `@EnableScheduling` would also switch scheduling on for the whole application for one job.
- **Each owner controls its own shutdown, and neither interrupts a JDBC call.** The writer drains
  for at most 5 s, as above. The purge waits for a run in progress for at most 3 s. Both are
  `@PreDestroy` methods, so their waits add up after the graceful phase (ADR-0018's budget).
- A third asynchronous feature, or one that needs a pool, reopens the shared-executor question.
