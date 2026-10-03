# ADR-0011 — Click recording: reduced on the request thread, written by one bounded writer, fail open

- Status: proposed (becomes accepted at the `02-analytics` plan-lock)
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
  or closed queue, logs `click lost` and returns. The writer task catches a
  failed insert the same way. Nothing is retried. The WARN carries
  `requestId` and `errorType` (the exception's class name) and never the
  exception's message. A driver message can quote the bound client hash or
  referrer (the lesson of `01-create-redirect`'s DR-01).
- **Correlation after the response.** The writer task puts the captured
  request id into the MDC for its duration and removes it in `finally`
  (ADR-0004 amendment).
- **Shutdown.** `@PreDestroy close()` calls `ExecutorService.close()`, which
  drains the queue. The recorder depends on the store and so on the
  `DataSource`, which Spring therefore destroys after the recorder. An
  abrupt stop loses what is queued (A-11).
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
  (C2 `HEAD`, C4 hook time, C7 slow store, C8 failing store).
