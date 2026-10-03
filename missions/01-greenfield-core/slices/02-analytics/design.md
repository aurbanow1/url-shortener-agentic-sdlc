---
slice: 02-analytics
mission: 01-greenfield-core
spec: SPEC.md
spec_candidate: 173bd60
status: proposed
created: 2026-10-03
---

# Design — Slice 02 Click analytics

Reader: the Development Agent, with `SPEC.md` (candidate `173bd60`: the
reviewed `488788f` plus RQ-01's label fix at `7200107` and, for design review
DR-03, rule 7's HEAD/OPTIONS wording, new AC-22 and A-18) open beside this
file. This is the smallest structure that reaches AC-1 to AC-22 and rules
1 to 9 on top of the merged `01-create-redirect` (`16c355f`). Tags as in slice
01: **[probe]** = run on Tomcat by `design-probe/ClickProbe.java` (§12),
**[jar]** = read from the 4.1.1 / 7.0.9 class files, **[docs]** = reference
documentation, not executed by me.

The shape in one paragraph. The redirect thread reduces the request to four
facts (referrer origin, user-agent class, HMAC of the address under the day's
salt, the instant) and hands a `Click` to a bounded queue with one writer
thread. It never waits, and a full queue or a failed write costs one `WARN`,
never the `302`. Statistics come from one grouped query over the click rows,
folded into the three figures in Java. Nothing else is added: no summary
table, no scheduler framework, no new dependency, no new property.

## 1. Components touched

| Component | Package / class | New or changed | Responsibility |
|---|---|---|---|
| Click feature package | `dev.urlshort.click` (`package-info.java`) | **new** | Records clicks and serves their statistics. Owns the `click` table. Reads `link` only to turn a code into its id (see "Link lookup" below). |
| Recorder (the hook's target) | `click.ClickRecorder` (**public**, `@Component`; its constructor stays package-private because it takes the package-private `ClickStore` and `DailySalt`) | **new** | `public void record(long linkId, HttpServletRequest request)`, called by the redirect on the request thread. Returns at once for `HEAD` (rule 1; the `GET` mapping also serves `HEAD` **[probe C2]**). Otherwise it reads `MDC.get("requestId")`, `Referer`, `User-Agent` and `getRemoteAddr()`, takes the click's instant and hash together from `salt.stamp(address)` (below), builds the reduced `Click` and calls `writer.execute(new ClickWrite(click, requestId))`. The whole body sits in one `try`: any `RuntimeException`, including `RejectedExecutionException` from a full or closed queue, becomes one `click lost` WARN (reason `rejected`) and the method returns normally (rule 5). The writer is `new ThreadPoolExecutor(1, 1, 0, MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY), daemon thread "click-writer", new AbortPolicy())` with `QUEUE_CAPACITY = 10_000`. Each task is a `final class ClickWrite implements Runnable` holding the `Click`, the request id and an `AtomicInteger state` (`QUEUED` → `RUNNING` → `DONE`, or `CLAIMED` by shutdown). **Whoever moves a click's state first owns its report (DR-01, second round).** `record` adds the task to a `Set<ClickWrite> outstanding` (`ConcurrentHashMap.newKeySet()`, at most queue size + 1 entries) before `execute`, and removes it again if `execute` is rejected. `run` first does `state.compareAndSet(QUEUED, RUNNING)`; if that fails, shutdown already reported the click, so it returns. Otherwise it puts the request id into the MDC and calls `store.insert(click)`. On success it does `compareAndSet(RUNNING, DONE)`. On a `RuntimeException` it logs one `click lost` (reason `write failed`) only if its own `compareAndSet(RUNNING, DONE)` wins. In `finally` it removes the task from `outstanding` and the MDC key. `void settle()` (package-private, for the functional suite) submits an empty task and waits for it up to 10 s; with one FIFO writer, that means every earlier click is written or lost. **`@PreDestroy void close()` is bounded and accounts for every click before it returns (DR-01):** `writer.shutdown()`; if `awaitTermination(DRAIN_DEADLINE)` (5 s) returns true, every click was written or reported. Otherwise `writer.shutdownNow()` stops the worker; any returned non-`ClickWrite` task (a `settle()` waiting behind a stuck write) is cancelled with `Future.cancel(false)`. Then `claimOutstanding()` walks `outstanding`. A click still `QUEUED` is claimed with `compareAndSet(QUEUED, CLAIMED)` and logs `click lost`, reason `shutdown deadline`. A click `RUNNING` is claimed with `compareAndSet(RUNNING, CLAIMED)` and logs `click lost`, reason `shutdown deadline, outcome unknown`. Its insert may still commit later (H2 does not always honour an interrupt), so the report does not call it lost or failed. Each report carries the click's own request id. Because every report is made inside `close()`, a normal process exit right after it loses no report even while the daemon writer is still stuck in the store. The worker's later completion or failure finds the state `CLAIMED`, its `compareAndSet` fails, and it reports nothing. The first revision relied on the pool close to end a stuck write, and the design review disproved that (a child JVM exited with 5 of 6 clicks accounted). Every click is now written or reported exactly once, and `close()` returns within the deadline **[revision probe DR-01, ownership cases]**. The recorder depends on the store, so Spring destroys it before the `DataSource`. A package-private constructor takes the deadline (unit tests use 200 ms); the `@Autowired` one passes `DRAIN_DEADLINE`. |
| Click record + reductions | `click.Click` | **new** | `record Click(long linkId, Instant clickedAt, LocalDate clickedOn, @Nullable String referrer, String userAgentClass, String clientHash)`. These are rule 2's facts, plus `clickedOn` = `LocalDate.ofInstant(clickedAt, UTC)`, the grouping key decided by the application, not the database (§3). Two static pure functions: `@Nullable String referrerOrigin(@Nullable String header)` (rule 3: `null` when absent, longer than 2 048, unparseable by `java.net.URI`, scheme not `http`/`https` ignoring case, or no host; otherwise lowercase scheme + `://` + lowercase host + `:port` unless the port is absent or the scheme's default) and `String userAgentClass(@Nullable String header)` (rule 4's order: absent or empty → `unknown`; lowercase contains `bot`, `crawler` or `spider` → `bot`; starts with `Mozilla/` → `browser`; else `other`). |
| Daily salt | `click.DailySalt` (`@Component`; `Clock`, `SecureRandom`) | **new** | `Stamp stamp(String address)` with `record Stamp(Instant at, String clientHash)`: the click's instant and the HMAC-SHA256 of the address's UTF-8 bytes under that instant's UTC-day salt, as 64 lowercase hex characters. The salt is 32 bytes from the application's `SecureRandom` bean, held in one field, and never logged, returned or stored. **The instant and the key are chosen in one locked step (DR-02).** Under the object's lock: `at = clock.instant()`; if `at`'s day differs from the held salt's day, zero the old bytes, draw a new salt for that day, and schedule `expire(day)` at that day's end with `CompletableFuture.delayedExecutor(Duration.between(at, dayEnd))`; then copy the key into a `SecretKeySpec`. The HMAC runs outside the lock. Time is read inside the lock, so no request can carry an older day into the lock after a newer day has rotated the salt. A request that chose day D just before midnight finishes with D's key copy, and day D+1's salt is never replaced **[revision probe DR-02]**. `expire(day)` zeroes and drops the salt only if it still belongs to `day`, so a stale callback is a no-op and a salt never outlives its UTC day, even with no click after midnight **[probe S2, revision probe DR-02]**. `@PreDestroy close()` drops it too. The one remaining boundary: if the clock itself moves back across midnight (an NTP step, or the suite's clock shifts), the earlier day gets a fresh salt. ADR-0012. |
| Store | `click.ClickStore` (`@Component`, `JdbcClient`; not `@Repository`, whose exception-translation proxy `JdbcClient` does not need and which would sit between the spy of §7 and the class) | **new** | `void insert(Click)` (one `INSERT`, instants bound as `atOffset(UTC)` like `AuditLog`); `Optional<Long> findLinkId(String code)`; `List<DayReferrerCount> countByDayAndReferrer(long linkId)`, one grouped `SELECT` (§3), with nested `record DayReferrerCount(LocalDate day, @Nullable String referrer, long clicks)`. Package-private. |
| Statistics endpoint | `click.StatsController` (`@RestController`) | **new** | `GET /api/links/{code:[A-Za-z0-9]{6,32}}/stats` → `200 LinkStats`; `Problems.notFound()` when `findLinkId` is empty. springdoc annotations for AC-21 (§2). |
| Statistics body | `click.LinkStats` | **new** | `record LinkStats(String code, long totalClicks, List<DayClicks> clicksPerDay, List<ReferrerClicks> topReferrers)` with nested `record DayClicks(LocalDate date, long clicks)` and `record ReferrerClicks(String referrer, long clicks)`; `static LinkStats of(String code, List<DayReferrerCount> rows)` folds the grouped rows: total = sum of all rows; per day = sum per day in a `TreeMap` (ascending); per referrer = sum per non-null referrer, sorted by `clicks` descending then `referrer` by `String.compareTo`, first `TOP_REFERRERS = 10`. Pure; unit-tested. |
| Redirect hook | `link.RedirectController` | **changed (grant: the hook only)** | Constructor gains `ClickRecorder clicks`; `redirect` gains an `HttpServletRequest request` parameter (springdoc ignores it, so the API document does not change for this operation **[probe C9]**); the comment line becomes `clicks.record(link.id(), request);`, after `resolve` and before the `302` is built. Javadoc names the hook. Nothing else under `link/` changes. |
| API document test | `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | **changed (grant: one line)** | The exact path list at line 70 gains `"/api/links/{code}/stats"`. Granted by the orchestration lead on `qitem-20261003082940-4da408ce` (08:30Z, `slice.yaml` revision receipt 8). AC-21's own assertions live in `click/`. |
| Schema | `src/main/resources/db/migration/V2__create_click.sql` | **new** | §3. ADR-0013. |
| Committed API document | `docs/api/openapi.json` | **changed** | Regenerated on this slice's candidate (first `w2` holder). |
| ERD | `docs/diagrams/erd.mmd` | **changed** | The `click` table and its edge to `link`. |

**Not touched:** `application.properties` and `build.gradle.kts` (both are
`03-operate`'s in `w2`; the constants above need no property and HMAC-SHA256
is in the JDK); `web/` main code (`Problems` and the advice are used as they
are); `audit/` (clicks are not mutations, rule 8).

**Link lookup.** `link/`'s types are package-private and the grant covers the
hook only, so `click/` cannot call `LinkService`. The hook passes the
resolved link's id, and the statistics path turns a code into an id with one
read of `link` (`SELECT id FROM link WHERE code = :code`, served by
`uq_link_code`). `click.link_id` already has a foreign key to `link.id`
(§3), so this read follows the feature's own key; it adds no Java dependency
on `link/`. The alternative, a public lookup in `link/`, is outside the grant.
§11 records the trade-off.

**Client address.** `request.getRemoteAddr()`: the connection's peer under
the default configuration, with forwarding headers ignored (rule 4, AC-6). If
`03-operate`'s trusted-proxy rule rewrites the remote address for named
proxies, clicks follow it without a change here. If it resolves the client
privately, aligning the two stays the lead's backlog (A-9).

Mechanics the builder relies on:

- **`HEAD` reaches a `@GetMapping` handler** and answers `302`. The recorder
  must skip it; with the skip, no click is stored **[probe C2]**.
- **Only reduced values cross threads.** Tomcat recycles the request object
  after the response, so nothing may read it on the writer thread. `record`
  copies the three headers, the address and the request id on the request
  thread and queues only the `Click` plus the request id. A real-server test
  guards this (§7, AC-16 row).
- **The hook costs microseconds:** about 20 µs at p50 and 36 µs at p95 on
  the reference laptop for parse + HMAC + enqueue, against NFR-L3's 2 ms
  **[probe C4]**. This is indicative only; the release bench owns the number.
- **The redirect does not wait for the store:** with every write sleeping
  2 s, twenty sequential redirects answered in under 1 ms each and the queue
  held 19 **[probe C7]**.
- **A lost click is one WARN with the request id restored on the writer
  thread, and no click value** even when the exception's message quotes the
  client hash (DR-01's lesson, **[probe C8]**).
- **`LocalDate` renders as `"2026-10-01"`** under Boot 4.1's Jackson 3
  defaults, and the body is `application/json` **[probe C5]**.
- **H2 stored and returned a `TIMESTAMP WITH TIME ZONE` in the session
  zone** (`…T01:33:23.008-07:00` on the reference machine **[probe C3]**,
  where the probe bound a `java.sql.Timestamp`). The design binds
  `atOffset(UTC)` like `AuditLog`, and with that binding H2 may well keep
  `Z`; I did not run it. The decision does not depend on it: a day computed
  in SQL depends on the binding and the session zone and is spelled
  differently on H2 and PostgreSQL. A day computed in Java from the
  instant does not, which is why the day is computed in Java and stored
  (§3).

## 2. API contract

`01-create-redirect`'s common contract applies unchanged: `X-Request-Id` on
every response; every error is a problem detail with `status`, `title`,
`instance` = `urn:uuid:<request id>` and no `detail`, regardless of `Accept`
(ADR-0002). That is why AC-13's "the body does not contain the code" holds
with no new code.

### 2.1 `GET /api/links/{code}/stats` — statistics

Path pattern `/api/links/{code:[A-Za-z0-9]{6,32}}/stats`. Anonymous (NFR-S6),
read-only, no parameters (rule 7).

| Case | Status | Body | Produced by |
|---|---|---|---|
| a link exists (active or retired, AC-7 to AC-12) | `200`, `application/json` | `{"code":"Ab3dE9fG","totalClicks":6,"clicksPerDay":[{"date":"2026-10-01","clicks":2},{"date":"2026-10-02","clicks":3},{"date":"2026-10-04","clicks":1}],"topReferrers":[{"referrer":"https://a.example","clicks":5}]}`: exactly four members; `clicksPerDay` ascending, days without clicks omitted; `topReferrers` at most 10, `clicks` descending then `referrer` ascending by code point; clicks without a referrer count in the first two figures only | `StatsController` → `ClickStore` → `LinkStats.of` |
| no link has the code (AC-13) | `404` problem detail | bare | `Problems.notFound()` |
| segment cannot be a code, e.g. 40 letters (AC-13) | `404` problem detail | bare | no handler → `NoResourceFoundException` (framework) |
| `POST`, `DELETE`, `PUT`, `PATCH` on the path (AC-13) | `405` problem detail, `Allow: GET` | bare | parent handler |
| `HEAD` on the path (AC-22, rule 7 at `173bd60`) | `200`, the `GET` headers, no body | — | framework: the `GET` mapping serves `HEAD` |
| `OPTIONS` on the path (AC-22) | `200` with `Allow` containing `GET` | — | framework (`dispatchOptionsRequest`) |

A never-clicked link answers `200` with `0`, `[]`, `[]` (AC-7). `HEAD` and
`OPTIONS` stay at the framework defaults, as the SPEC's rule 7 now states
(DR-03, A-18). Neither is a click, because the hook sits only on the
redirect, so neither changes `totalClicks` (AC-22). No handler is added for
them, so the API document gains no operation for either.

springdoc: `@Operation(summary = "Read a link's click statistics")`;
`@ApiResponse(responseCode = "200", content = @Content(mediaType =
"application/json", schema = @Schema(implementation = LinkStats.class),
examples = @ExampleObject(<the 200 body above>)))`; `@ApiResponse(responseCode
= "404", content = @Content(mediaType = "application/problem+json", schema =
@Schema(implementation = ProblemDetail.class)))`. That is what AC-21 asks
for. The handler method is named `stats`, which no other controller uses, so
the `operationId` stays stable.

### 2.2 `GET /{code}` — redirect (unchanged contract)

Status, `Location`, `Cache-Control: no-store`, `404` and `410` are exactly
`01-create-redirect`'s (rule 8). What is added happens only after `resolve`
succeeded and only for `GET`: one `ClickRecorder.record` call that cannot
throw and returns within microseconds. A redirect refused with `429` by
`03-operate`'s limiter never reaches the controller, so it is not a click,
which is consistent with rule 1.

## 3. Data model & migration

`V2__create_click.sql` (runs on H2 in PostgreSQL mode and on PostgreSQL; no
vendor syntax; DDL executed on H2 by the probes, and verified to keep
accepting valid rows and rejecting invalid ones after the pooled connection
that ran it is retired **[constraint probe]**):

```sql
-- V2: click events for analytics (02-analytics), reduced before they are written:
-- no raw address, user agent, referrer path or request id is ever stored (NFR-P1).
-- The closed set of user-agent classes is a lookup table, not CHECK (... IN ...): on H2 2.4.240 a
-- CHECK built from a multi-value condition stops working once the connection that created it closes.
-- rollback: DROP TABLE click; DROP TABLE user_agent_class;  -- click history only; V1 untouched

CREATE TABLE user_agent_class (
    token VARCHAR(16) PRIMARY KEY
);

INSERT INTO user_agent_class (token) VALUES ('browser'), ('bot'), ('other'), ('unknown');

CREATE TABLE click (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    link_id          BIGINT        NOT NULL,
    clicked_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    clicked_on       DATE          NOT NULL,
    referrer         VARCHAR(2048),
    user_agent_class VARCHAR(16)   NOT NULL,
    client_hash      VARCHAR(64)   NOT NULL,
    CONSTRAINT fk_click_link FOREIGN KEY (link_id) REFERENCES link (id) ON DELETE CASCADE,
    CONSTRAINT fk_click_user_agent_class FOREIGN KEY (user_agent_class) REFERENCES user_agent_class (token),
    CONSTRAINT ck_click_client_hash_length CHECK (LENGTH(client_hash) = 64)
);

CREATE INDEX ix_click_link_day ON click (link_id, clicked_on);
```

**Why a lookup table (DR-04).** On H2 2.4.240, `CHECK (c IN (...))` and its
`c = 'a' OR c = 'b' …` spelling both answer "The database has been closed"
(`90098`) for every insert once the pooled connection that ran the DDL is
retired. Hikari retires every connection after 30 minutes (`maxLifetime`), so
in production every click would fail open into a WARN from then on. The
constraint probe ran each candidate on in-memory and file databases, retiring
the pool with `softEvictConnections()` while a second connection kept the
database open (the reviewer's method):

| Form | Valid insert after retirement | Invalid insert after retirement |
|---|---|---|
| `CHECK (c IN ('browser', …))` | **rejected, `90098`** | rejected, `90098` |
| `CHECK (c = 'browser' OR c = 'bot' …)` | **rejected, `90098`** | rejected, `90098` |
| `CHECK (c = 'browser')`, `CHECK (c <> 'nope')`, `CHECK (LENGTH(h) = 64)` | accepted | rejected by the check |
| `FOREIGN KEY (c) REFERENCES user_agent_class (token)` | accepted | rejected by the FK |
| **V1's shipped `ck_link_code_length` and `ck_link_url_not_empty`**, each tested alone | accepted | rejected by the check |
| **this V2** (FK + `LENGTH` check), valid click / unknown class / short hash | accepted | rejected (FK; check) |

So the shipped `link` table is not affected. Only multi-value conditions
are. The FK costs one primary-key lookup in a four-row table per insert.
H2 also indexes the referencing column; PostgreSQL does not, and needs no
index there because nothing deletes a class.

Design notes (ADR-0013):

- **One row per click, statistics computed at request time.** This is the
  "event rows at request time" option of `databases.md` §3, not a summary
  table. The figures are cheap folds of a grouped query, a summary table
  would be a second write on the hot path (or a second consumer), and rows
  keep every later choice open: FR-16's uniques, NFR-P2's purge by age, and
  a summary table if reads ever get heavy.
- **`clicked_on` is stored, computed in Java as the UTC day of `clicked_at`.**
  The guide suggests an index on `(link_id, clicked_at)`. The only query here
  groups by UTC day, and a day computed in SQL depends on the parameter
  binding and the session time zone (the probe, binding a `Timestamp`, got
  `-07:00` back from H2), with different functions on H2 and PostgreSQL. A stored `DATE` makes the grouping key exact, portable and
  indexable. It is derived from `clicked_at`, not a fifth fact (rule 2), and
  only `Click`'s constructor call in `ClickRecorder` writes it.
- **`referrer` is the origin or `NULL`**, `VARCHAR(2048)` because an origin is
  never longer than the header it came from (rule 3 caps that at 2 048).
  `user_agent_class` is one of four tokens, enforced by the foreign key to
  `user_agent_class`. `client_hash` is exactly 64 hex characters, enforced by
  a `LENGTH` check.
  There is no request id column (A-16) and no column for any raw value.
- **Foreign key with `ON DELETE CASCADE`.** A click means nothing without its
  link. Nothing deletes links today (retire is a timestamp), and if a future
  erasure deletes one, its clicks go with it.
- **Time** comes from the application `Clock` (`link.LinkConfig`, the bean
  the functional suite replaces), as for every other stored instant
  (ADR-0005). There are no database defaults.
- **NFR-P2 compatibility:** every row carries its own instant and day, so a
  90-day purge is one `DELETE … WHERE clicked_on < ?` in mission 02, needing
  at most a second index. No schema change is needed for it.

Queries and their indexes:

| Query | Where | Index |
|---|---|---|
| `INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash) VALUES (…)` | writer thread | — (the FK checks use the primary keys of `link` and `user_agent_class`) |
| `SELECT id FROM link WHERE code = :code` | statistics | `uq_link_code` |
| `SELECT clicked_on, referrer, COUNT(*) AS clicks FROM click WHERE link_id = :linkId GROUP BY clicked_on, referrer` | statistics | `ix_click_link_day` (range on the leading `link_id`) |

**One statement feeds all three figures**, so AC-11 holds even while clicks
are being written: `totalClicks` is the sum of the per-day sums, and the
referrer sums are a subset of the same rows. The result has one row per
(day, origin) pair of the link, and the probe's 26-click, 14-origin case
folded into the expected body **[probe C5]**. Ranking and the code-point
tie order are done in Java (`String.compareTo`; an origin is ASCII because
`java.net.URI` accepts only ASCII host names **[docs]**), so the order does
not depend on a database collation. H2's own `ORDER BY` happens to give the
same order **[probe C6]**, but PostgreSQL with a linguistic collation would
not.

Rollback of V2: `DROP TABLE click; DROP TABLE user_agent_class;` (in that
order; the index goes with its table). These are additive tables on an
existing baseline. Rolling back loses only click history and touches nothing
that V1 owns.

## 4. Sequences

Redirect with click recording, and the statistics read (`docs/diagrams/click-sequence.mmd`):

```mermaid
sequenceDiagram
    autonumber
    participant V as Visitor
    participant F as RequestIdFilter (web)
    participant RC as RedirectController (link)
    participant S as LinkService (link)
    participant CR as ClickRecorder (click)
    participant DS as DailySalt (click)
    participant Q as queue (10 000) + click-writer thread
    participant ST as ClickStore → H2 click
    participant A as Analyst
    participant SC as StatsController (click)

    V->>F: GET /C (Referer, User-Agent, peer address)
    F->>F: X-Request-Id R, MDC requestId=R
    F->>RC: chain.doFilter
    RC->>S: resolve(C)
    alt unknown or retired
        S-->>V: 404 / 410 problem detail (no click)
    end
    S-->>RC: link
    RC->>CR: record(link.id, request)
    alt HEAD
        CR-->>RC: return (not a click)
    else GET
        CR->>CR: origin(Referer); class(User-Agent)
        CR->>DS: stamp(peer address)
        DS->>DS: under the lock: at = clock.instant(); rotate if at's day changed; copy key
        DS-->>CR: (at, HMAC-SHA256 under at's day salt, 64 hex)
        CR->>Q: execute(ClickWrite(Click, R))
        alt queue full or closed
            CR->>CR: WARN "click lost" {requestId R, reason rejected, errorType}
        end
    end
    RC-->>V: 302 Location: <url>, Cache-Control: no-store
    F->>F: INFO "request completed" {status 302}; MDC.remove
    Q->>Q: MDC requestId=R (restored on the writer thread)
    Q->>ST: insert(Click)
    alt write fails
        Q->>Q: WARN "click lost" {requestId R, reason write failed, errorType}; nothing stored
    end
    Q->>Q: MDC.remove
    Note over Q: at shutdown: drain up to 5 s, then each unwritten ClickWrite → WARN "click lost" {its requestId, reason shutdown deadline}

    A->>SC: GET /api/links/C/stats
    SC->>ST: findLinkId(C)
    alt no link
        SC-->>A: 404 problem detail
    end
    SC->>ST: countByDayAndReferrer(id): one grouped SELECT
    SC->>SC: LinkStats.of: total, per day ascending, top 10 referrers
    SC-->>A: 200 {code, totalClicks, clicksPerDay, topReferrers}
```

## 5. Logging & audit events

| Event | Logger | Level | `message` | Fields | Must never appear |
|---|---|---|---|---|---|
| click lost | `dev.urlshort.click.ClickRecorder` | WARN | `click lost` | `requestId` (the redirect's: from the MDC on the request thread, restored on the writer thread, or carried by the cancelled `ClickWrite` at shutdown); `reason`: `rejected` (full or closed queue), `write failed` (the insert threw while the writer still owned the click), `shutdown deadline` (still queued when the 5 s drain ended; never written) or `shutdown deadline, outcome unknown` (being written when the drain ended; the row may still commit, so the outcome is not claimed); `errorType`, the exception's class name, when there is one | the exception's message or stack trace (a driver message can quote the bound `client_hash` or `referrer`, DR-01), the client address, its hash, the `User-Agent`, the `Referer` whole or as origin, any forwarding value, the link id |
| request completed | `dev.urlshort.web.RequestIdFilter` | INFO | unchanged (slice 01) | `requestId`, `status`, for the statistics endpoint too | as slice 01 |

A recorded click logs nothing, and a statistics read logs only the filter's
event. Exactly one WARN per lost click (rule 5): each lost click passes
through exactly one of three places: the request-thread `catch`, the
writer-thread `catch` when the writer owns the click, or the shutdown claim
when shutdown owns it. Ownership is one `compareAndSet` on the click's own
state, so two of them can never both report. None of them retries. Work
done for a request on the writer thread puts the request id back into the
MDC for the length of that task. That is the cross-cutting rule this slice
adds to ADR-0004 (amendment, §10). Overload is visible as a run of `click
lost` WARNs; under sustained overload this is one line per lost click, which
rule 5 requires, and `03-operate`'s rate limit bounds it.

Audit: none. Recording a click and reading statistics are not mutations of a
link (rule 8); `audit_log` is neither written nor read here, and AC-20 proves
the rows are unchanged.

## 6. Threat model

Assets: the Visitor's address, browser string and referring page (NFR-P1);
the daily salt; redirect latency and availability; the integrity of the
statistics; the logs.

Entry points (new in this slice): the `Referer`, `User-Agent` and peer
address of every `GET /{code}` that answers `302` (through the hook); the
forwarding headers on that request, which are ignored; `GET
/api/links/{code}/stats` (path segment, method); the `click` table; the salt
in process memory.

| STRIDE | Threat | Mitigation in this slice | Residual / owner |
|---|---|---|---|
| Spoofing | A client forges `X-Forwarded-For`/`Forwarded` to change its recorded identity | `getRemoteAddr()` only; no header is read for identity (rule 4, AC-6) | trusted-proxy alignment is `03-operate` / lead backlog (A-9) |
| Spoofing | Scripts inflate a link's clicks | raw clicks are the decided semantics (A-3, FR-16 in mission 03) | per-client rate limit is `03-operate` |
| Tampering | SQL injection through `Referer` or `User-Agent` | nothing raw is stored; the origin is rebuilt from `java.net.URI` parts, the class is one of four constants (also a foreign key to `user_agent_class`); every statement binds named parameters | none |
| Tampering | Referrer spam: fake origins pushed into `topReferrers` | the origin only, never a path; statistics show what clients sent, by design | rate limit (`03-operate`); accepted for an analytics read |
| Repudiation | A click without a trace | clicks are not audited by rule (rule 8); a lost click leaves a WARN with the request id | accepted by the SPEC |
| Information disclosure | Raw address, `User-Agent`, referrer path/query/fragment/userinfo or forwarding values stored | reduced on the request thread before the queue; the row has exactly the reduced columns; AC-3 to AC-6 canaries; real-server test checks stored rows | none |
| Information disclosure | Address recovered from its hash (IPv4 is only 2³² values) | HMAC-SHA256 under a 32-byte random salt held only in memory; the salt is never logged, returned or stored; it is zeroed and dropped at its UTC day's end by a scheduled expiry even when no click follows (**[probe S2]**); a restart draws a new one; the hash is never exposed (AC-17). Within its day the salt lives in the heap, so whoever can read process memory can test addresses for that day | process compromise is out of scope; `03-operate` runs the container non-root |
| Information disclosure | Statistics reveal individuals | aggregates only: four members, no hash, no class, origins only (AC-17); anonymous by NFR-S6, so anyone with a code reads its statistics, which are as public as the short link | accepted (NFR-S6) |
| Information disclosure | Click data in logs | no click value is passed to a logger; the lost-click WARN logs the exception class, never its message (**[probe C8]**: a message quoting the hash stayed out); AC-18 canaries on MockMvc and on Tomcat | none |
| Information disclosure | Driver messages in H2's trace file (`data/*.trace.db`) | a failed click insert is either an integrity violation (`23xxx`: the FKs, the `LENGTH` check), traced below H2's default level, or a connection failure with no values; `22001` (value too long) cannot occur because every column is at least as wide as its validated input | as slice 01 §6 (`TRACE_LEVEL_FILE` is `03-operate`'s) |
| Denial of service | A slow or failing click store slows or fails redirects | fail open: the redirect only enqueues; a full or closed queue drops with a WARN; AC-14, AC-15 | none |
| Denial of service | A slow or stalled store holds process shutdown (DR-01) | the drain at close is bounded at 5 s; unwritten clicks are cancelled and each reported once; `03-operate`'s 10 s phase + 5 s drain + pool close fit its 20 s stop grace (**[revision probe DR-01]**: 6 writes of 2 s, `close()` returned at 5 006 ms) | clicks still queued at the deadline are lost, each with its WARN (rule 5) |
| Denial of service | A pool connection is retired and the click constraints stop working (DR-04) | no multi-value `CHECK`; the closed set is a lookup-table FK, the hash length a `LENGTH` check; both verified after retirement (**[constraint probe]**) | none |
| Information disclosure | A delayed request rotates the salt back into an expired day (DR-02) | instant and key chosen in one locked step; an expiry acts only on its own day (**[revision probe DR-02]**) | a clock that itself jumps back across midnight draws a fresh salt for the earlier day (documented) |
| Denial of service | Memory through the queue | bounded at 10 000 tasks; each holds about 2.3 KB at worst (a 2 048-character origin, the hash, the request id), so about 23 MB at the bound | none beyond the bound |
| Denial of service | WARN flood when every click is lost | one line per lost click, required by rule 5 | bounded by `03-operate`'s rate limit |
| Denial of service | Statistics cost grows with distinct (day, origin) pairs, inflatable by referrer spam | one indexed range scan per read; rows grouped in SQL | rate limit (`03-operate`); NFR-P2 purge (mission 02) |
| Denial of service | Unbounded growth of `click` | none in this slice | NFR-P2 retention purge, mission 02 |
| Elevation of privilege | — | no principals (NFR-S6); nothing from the request is executed or fetched | none |

## 7. Test strategy hints

### 7.1 Suites and classes

Functional (`src/functionalTest/java/dev/urlshort/click/`), one test per AC
named after it, tabled ACs as one `@ParameterizedTest`:

| Class | Context | ACs |
|---|---|---|
| `ClickRecordingJourneyTest` | base (MockMvc) | AC-1 (**polls** the store every 50 ms up to rule 6's 5 s; this is the visibility bound's test, then `settle()` and still exactly one), AC-2 (table; `settle()` then none), AC-3 (table), AC-4 (table), AC-5 (`setRemoteAddr` per request, `FunctionalClock`), AC-6, AC-17, AC-18 |
| `StatsJourneyTest` | base (MockMvc) | AC-7, AC-8, AC-9 (`FunctionalClock`), AC-10, AC-11 (over the bodies of AC-8, AC-9, AC-10), AC-12, AC-13 (table), AC-19 rows `200`, `404`, `405` and settled `302`, AC-20, AC-22 (`HEAD` → `200` with no body, `OPTIONS` → `200` with `Allow` containing `GET`, `totalClicks` unchanged after both), AC-21 (`GET /v3/api-docs`: the stats path, its `200` schema with four properties (springdoc emits a `$ref` to `components/schemas/LinkStats`; resolve it) and an example, its `404` as `application/problem+json`; every path of slice 01's AC-28 still present) |
| `ClickResilienceJourneyTest` | **own**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@MockitoSpyBean ClickStore` | AC-14 and AC-15 over real HTTP; AC-16 (200 redirects from 20 threads over real HTTP); AC-19's failing-store row; the W1-02 real-server journey (below) |
| `ClickSchemaJourneyTest` | base | after `softEvictConnections()` on the context's pool, a redirect with each of the four user-agent classes is recorded (settled, four rows). This is an end-to-end smoke test, **not** the DR-04 regression: the suite's in-memory database is shared by every context, and Flyway ran V2 in whichever context started first, so this pool may not hold the DDL connection |

Plus the granted line in `web/OpenApiDocumentTest` (§1) and the regenerated
`docs/api/openapi.json`. Slice 01's own tests (AC-12 to AC-14 among them)
run unchanged; they are AC-20's other half.

**W1-02, decided: one real-server class, folded into the spy context.**
MockMvc cannot see Tomcat's request recycling, and that is the failure a
click hook is most likely to have: a value read from the request after the
response has gone. `ClickResilienceJourneyTest` sends AC-16's 200 redirects
over real HTTP with a fixed `Referer` (`https://ref.example/p?q=realcanary`)
and `User-Agent` (`Mozilla/5.0 realcanary`). It then asserts that every one
of the 200 stored rows has origin `https://ref.example` and class `browser`.
One more test sends one redirect and one statistics read with canaries
(`User-Agent`, `Referer` path and query, `X-Forwarded-For`) and asserts, on
Tomcat, AC-18's absence and AC-19's correlation. The peer is `127.0.0.1`
there, so the address canary of AC-18 is proven on MockMvc, where
`setRemoteAddr` can set it. Folding into the spy class costs no extra
context beyond the spy's own. It also keeps the class off
`ColdStartJourneyTest`'s plain `RANDOM_PORT` context: that class must remain
the only request its server ever sees.

Unit (`src/test/java/dev/urlshort/click/`, no Spring context):

| Class | What it proves |
|---|---|
| `ClickTest` | `referrerOrigin`: every AC-3 row; explicit default ports (`http://x:80`, `https://x:443`) dropped; non-default kept; uppercase scheme accepted and lowercased; IPv6 host kept in brackets; `ftp://`, relative, `http:///x` → `null`; exactly 2 048 characters parsed, 2 049 → `null`. `userAgentClass`: every AC-4 row plus `BOT` in capitals and `mozilla/` lowercase → `other` |
| `DailySaltTest` | with a mutable test clock: same address and day → equal; other address → different; next day → different; never the unsalted SHA-256 hex or Base64 of the address; 64 lowercase hex; each `Stamp`'s instant is the clock's; **the reviewer's interleaving** (DR-02): a selection made at `23:59:59.999Z` of day D finishes after a stamp at `00:00:00.000Z` of D+1, then D+1 is stamped again → the delayed hash equals D's, and both D+1 hashes are equal (no replacement); **a stale `expire(D)` after D+1 began** → D+1's hash unchanged; `expire(currentDay)` → the next stamp draws a new salt (dropping is observable without an accessor); `close()` drops; a salt created at `23:59:59.900Z` is gone after its scheduled expiry fires (wait ≤ 2 s). The interleaving needs the locked step exposed package-private (`select()` returning the instant, day and key copy; `stamp` = `select` + HMAC) |
| `ClickRecorderTest` | with a mock `ClickStore`, a fixed `DailySalt` and `MockHttpServletRequest`: `HEAD` → no `insert`; `GET` → one `insert` whose `Click` has the reduced values and no raw ones (after `settle()`); store throws `new DataAccessResourceFailureException("…<canary>…")` → one WARN `click lost`, reason `write failed`, with `requestId` restored and `errorType`, without the canary, and the MDC is empty afterwards; after `close()`, `record` → one WARN, reason `rejected`, `errorType` `RejectedExecutionException`, and no exception escapes; **bounded close with ownership (DR-01)**, with a store that blocks on a latch and **ignores interrupts** (as an H2 lock wait does) and a 200 ms deadline: (a) one click in flight and four queued → `close()` returns within about 200 ms, and **when it returns** there are already five WARNs: the in-flight id with `shutdown deadline, outcome unknown` and four with `shutdown deadline`, five ids, no duplicates. This is the normal-process-exit case: nothing is left for the daemon writer to report. (b) Release the latch afterwards: the stuck insert completes and no further WARN appears. (c) The same, but the store throws after the release: no `write failed` WARN (suppressed). (d) Start race, deterministic on one thread via the package-private `claimOutstanding()` and `ClickWrite.run()`: claim first, then run → one `shutdown deadline` report and no insert. (e) Completion race: run to `DONE` first, then claim → written, no report. (f) A fast store → `close()` writes everything and reports nothing. (g) A rejected `execute` leaves no entry in `outstanding` |
| `ClickSchemaTest` | **the DR-04 regression, deterministic:** its own `HikariDataSource` (pool size 2) on a fresh `jdbc:h2:mem:<unique>;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`; `Flyway.configure().dataSource(ds).load().migrate()` runs the real `db/migration` files (Flyway, H2 and Hikari are on the unit runtime classpath through the main dependencies); a valid click is inserted; `getHikariPoolMXBean().softEvictConnections()` retires the connection that ran the DDL while a held connection keeps the database open; then all four classes are accepted, and class `tablet` and a 3-character hash are rejected. A multi-value `CHECK` put back into V2 fails this test |
| `LinkStatsTest` | no rows → `0`, `[]`, `[]`; per-day ascending; `null` referrer counted in the total and per day, absent from the top; ties by code point (`d1`, `d10`, `d11`, `d2`); cap at 10; total = sum of per day |

### 7.2 Mechanisms the tests depend on

- **Settling.** `ClickRecorder.settle()` is package-private, so the click
  journeys (same package) call it. It waits until every click queued before
  the call is written or lost. AC-1 deliberately does not use it at first:
  it polls to rule 6's 5 s bound, so rule 6 has its own test (as the
  requirements review asked). Before opening any capture window, a test
  calls `settle()`, so an earlier request's async event cannot fall into it.
  Before closing the window, it calls `settle()` again, so events written
  after the response are inside it (AC-19). On the real server
  (`ClickResilienceJourneyTest`) that is not enough: the filter writes
  `request completed` in the Tomcat thread's `finally`, which can run after
  the client already has the reply (slice 01's probe needed 300 ms). So
  before closing a window there, the test also polls the capture (50 ms
  steps, at most 5 s) for the `request completed` line carrying `R`, as
  `ColdStartJourneyTest` does.
- **Why no click write fails in the base context**, which matters for slice
  01's `ObservabilityJourneyTest`, which asserts every line in its windows
  carries its own id. The link FK cannot fail, because the id comes from a
  link `resolve` just read and links are never deleted. The class FK and the
  hash check cannot fail, because the class is one of the four seeded tokens
  and the hash is always 64 hex characters. Unlike the first design's
  `IN`-list `CHECK`, both keep working after a pooled connection is retired
  (DR-04, **[constraint probe]**, `ClickSchemaTest`). The queue cannot
  fill at suite volume. So no foreign WARN can appear in those windows.
- **Client addresses (AC-5, AC-6, AC-18):** MockMvc
  `.with(r -> { r.setRemoteAddr("203.0.113.77"); return r; })`, the pattern
  of slice 01's `ObservabilityJourneyTest`.
- **Absolute instants (AC-5, AC-9)** with the offset `FunctionalClock`
  (`src/functionalTest/java/dev/urlshort/link/`, used, not changed):
  `clock.reset(); clock.shift(Duration.between(Instant.now(), target));`, then
  the redirect. Real time keeps running between the shift and
  `DailySalt.stamp`'s `clock.instant()`, which is milliseconds. So `23:59:59Z` lands at
  `23:59:59.00x`, still that day and that second, and the test shifts again
  for each target instead of relying on elapsed time. `@AfterEach` resets
  the clock. AC-5's day `D` is any past day (for example `2026-09-01`). Moving
  the suite clock back to `D` is the documented "clock moved back" boundary,
  which draws a fresh salt for `D`; moving on to `D+1` draws the next. Those
  are the two salts the test needs.
- **Slow and failing store (AC-14, AC-15)** with `@MockitoSpyBean ClickStore
  store`. For AC-14,
  `doAnswer(inv -> { Thread.sleep(2000); return inv.callRealMethod(); }).when(store).insert(any())`,
  twenty sequential `HttpClient` sends, each timed from send to reply on the
  client (`< 250 ms` is client-observed latency). Then `Mockito.reset(store)`,
  so the 19 queued writes run at normal speed, and `settle()`: it waits at
  most for the one write still sleeping, not 40 s. For AC-15, `doThrow(new
  DataAccessResourceFailureException("store down <canary>"))`; then
  `settle()`, no row for the link, and exactly one `click lost` line with
  `requestId == R` and no canary.
- **AC-16 concurrency:** 20 platform threads (or an executor), each sending
  10 redirects through one shared `HttpClient`; every reply `302`; `settle()`;
  `totalClicks` from the statistics endpoint is `200` and `SELECT COUNT(*)`
  for the link is `200`.
- **Reading click rows** in tests: the context's `JdbcClient`,
  `SELECT c.* FROM click c JOIN link l ON l.id = c.link_id WHERE l.code = :code`.
  The in-memory database is shared by every context in the JVM, so each test
  creates its own links and counts per link, never globally (slice 01's
  rule).
- **AC-20:** create and retire links, read every `audit_log` row (ordered by
  `id`), run the click and statistics operations, then compare: same rows,
  same content, same count.
- **Contexts:** slice 01's four (base, `AuditLog` spy, base URL, cold start)
  plus one (`ClickResilienceJourneyTest`). Its spy and `RANDOM_PORT` give it
  its own cache key.

### 7.3 Coverage

Every branch has a named test: the `HEAD` skip and the request-thread `catch`
(`ClickRecorderTest`); the writer `catch` (`ClickRecorderTest`, AC-15); each
reduction branch (`ClickTest`); rotation, same-day reuse and both `expire`
outcomes (`DailySaltTest`); empty and non-empty lookups (AC-7, AC-13); and
the null-referrer branch of the fold (`LinkStatsTest`, AC-10). The `host ==
null` test in `referrerOrigin` is the only host check: `java.net.URI` never
returns an empty host, so an `isEmpty()` branch would be unreachable.
`close()` is unit-tested because the functional contexts close after
JaCoCo's agent may already have dumped. Both `awaitTermination` outcomes are
covered by the fast-store and blocking-store cases. The shutdown loop's
non-`ClickWrite` branch is covered too: a `settle()` task can sit queued
behind a blocked write, so the loop cancels it (`Future.cancel(false)`), and
the bounded-close test calls `settle()` from a helper thread first, which
then returns at once. Every `compareAndSet` outcome is a named case of
`ClickRecorderTest`'s (a)–(e): `QUEUED`→`CLAIMED`, `RUNNING`→`CLAIMED`, the
worker's start CAS failing, its end CAS failing on success and on failure,
and both succeeding. Expected `docs/qa/GAPS.md` row: the
NFR-L3 number until the release bench measures it (SPEC proof contract), and
nothing else.

## 8. Reachability check

| AC / rule | Reached by | Error path explicit? | Threat model covers the entry point? |
|---|---|---|---|
| AC-1 | hook → `ClickRecorder.record` → writer → `ClickStore.insert` | — | redirect headers ✔ |
| AC-2 | `resolve` throws before the hook (`404`/`410`); `HEAD` skipped; `405`, reads and stats never call the hook | yes | ✔ |
| AC-3 | `Click.referrerOrigin` | — | `Referer` ✔ |
| AC-4 | `Click.userAgentClass` | — | `User-Agent` ✔ |
| AC-5 | `DailySalt.stamp` (instant and key in one locked step, HMAC, day salt, rotation) | — | peer address, salt ✔ |
| AC-6 | `getRemoteAddr()` only | — | forwarding headers ✔ |
| AC-7, AC-8, AC-9, AC-10, AC-11, AC-12 | `StatsController` → `findLinkId` → `countByDayAndReferrer` → `LinkStats.of` | — | stats path ✔ |
| AC-13 | `Problems.notFound()`; no handler; parent `405` | yes | ✔ |
| AC-14, AC-15 | bounded queue, one writer, both `catch` blocks | yes (WARN) | store DoS ✔ |
| AC-16 | one FIFO writer, no drop below 10 000 queued | — | ✔ |
| AC-17 | `LinkStats` has four members of aggregates | — | statistics disclosure ✔ |
| AC-18 | no click value reaches a logger; WARN logs the class | — | logs ✔ |
| AC-19 | filter event; MDC restored on the writer thread | — | logs ✔ |
| AC-20 | no `audit/` call anywhere in `click/`; the redirect contract untouched | — | ✔ |
| AC-21 | `StatsController` annotations; `OpenApiDocumentTest` line | — | ✔ |
| AC-22 | framework `HEAD` (from the `GET` mapping) and `OPTIONS`; no hook on the stats path | — | stats path ✔ |
| rules 1–9 | 1: hook placement + `HEAD` skip; 2: `Click` + V2 columns; 3: `referrerOrigin`; 4: `getRemoteAddr` + `DailySalt` + `userAgentClass`; 5: queue + two catches; 6: one writer, AC-1's poll; 7: `LinkStats`; 8: hook after `resolve`, no audit; 9: §5 | — | — |

## 9. Territory (`slice.yaml` at revision receipt 8)

| Path | In `slice.yaml` | Needed by this design |
|---|---|---|
| `src/{main,test,functionalTest}/java/dev/urlshort/click/` | yes | seven production files, four unit classes, three journey classes |
| `src/main/resources/db/migration/` | yes (V2 only) | `V2__create_click.sql` |
| `src/main/java/dev/urlshort/link/` | yes, the hook only | `RedirectController`: the `ClickRecorder` field and constructor parameter, the `HttpServletRequest` parameter, one call, its Javadoc |
| `docs/api/openapi.json` | yes, first holder in `w2` | regenerated |
| `docs/diagrams/erd.mmd` | yes | the `click` table |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | yes, granted 08:30Z (`qitem-20261003082940-4da408ce`), the path list only | one path added to line 70 |
| `src/main/resources/application.properties`, `build.gradle.kts`, `web/` main | no (`03-operate`) | not needed |
| `src/functionalTest/java/dev/urlshort/link/FunctionalClock*` | no | used as it is, not changed |

`docs/DESIGN.md`, `docs/adr/`, `docs/diagrams/container.mmd` and
`docs/diagrams/click-sequence.mmd` are design documents written in this step
on `main`, not slice code.

## 10. Decisions recorded as ADRs

| ADR | Decision | Status |
|---|---|---|
| [ADR-0011](../../../../docs/adr/0011-click-handoff-bounded-single-writer.md) | clicks are reduced on the request thread and handed to a bounded queue (10 000) with one writer thread; fail open: a full or closed queue or a failed write loses that click with one WARN carrying the request id; the writer restores the request id in the MDC; `close()` drains for at most 5 s, then cancels and reports each unwritten click once (DR-01); abrupt stops may lose queued clicks (A-11) | proposed, revised after design review |
| [ADR-0012](../../../../docs/adr/0012-client-hash-daily-salt.md) | the client hash is HMAC-SHA256 under a 32-byte random salt per UTC day, held only in memory, zeroed and dropped at the day's end by a scheduled expiry; the instant and the key are chosen in one locked step (DR-02); a restart draws a new salt | proposed, revised after design review |
| [ADR-0013](../../../../docs/adr/0013-click-events-and-request-time-statistics.md) | one row per click with a stored UTC day; the closed set of user-agent classes as a lookup-table FK, not a multi-value `CHECK` (DR-04); statistics computed per request from one grouped query, ranked in Java; FK `ON DELETE CASCADE`; NFR-P2 purge needs no schema change | proposed, revised after design review |
| [ADR-0004](../../../../docs/adr/0004-structured-ecs-logs-no-client-pii.md) (amended) | work done for a request on another thread restores `requestId` in the MDC for its duration; its events follow the same content rules | amended 2026-10-03 |

ADR numbers 0014 onward are left for `03-operate`, whose design follows this
one.

## 11. Trade-offs

| Option | Why not now | What would change the decision |
|---|---|---|
| Synchronous insert in the redirect | a slow or failing store would delay or fail the `302` (rules 5, AC-14, AC-15) | — |
| Spring `@Async` / `@EnableAsync` | global switches outside `click/`, an unbounded default queue unless configured, and `java-spring.md` asks for an ADR anyway; a `ThreadPoolExecutor` field says the bound and the policy in one line | a second feature that needs async work |
| `CallerRunsPolicy` on a full queue | it would run the insert on the redirect thread exactly when the store is slowest | — |
| Several writer threads or batched inserts | one writer drains thousands of single-row inserts per second on H2 and keeps FIFO order, which `settle()` relies on | the release bench showing the queue growing at NFR-L1's 100 req/s |
| A persistent salt (survives restart) | keeps the secret longer, needs storage and a disposal job; nothing reads the hash in this slice (A-3, A-10) | FR-16 (uniques) needing day-long stability across restarts |
| A salt derived from a master secret | the master secret would let anyone holding it recompute every past day's hashes, which defeats "not kept after its day" | — |
| Lazy salt rotation only (on the next click) | on a quiet day the salt would stay in memory past midnight (rule 4) | — |
| A summary table updated per click | a second write on every click, and rows are needed anyway for uniques and purge | statistics reads becoming the bottleneck |
| Day grouped in SQL from `clicked_at` | the result depends on the binding and the session time zone (with a `Timestamp` binding, `-07:00` came back from H2), and the function differs between H2 and PostgreSQL | a database-side UTC function both engines share |
| `ExecutorService.close()` (unbounded drain), as in the first design | the design review showed a slow store holding shutdown past 10 s (six 2-s writes ended at about 12 s), and a stalled store would hold it indefinitely (DR-01) | — |
| A drain deadline longer than 5 s | `03-operate`'s 10 s shutdown phase plus the drain plus the pool close must fit its 20 s stop grace; 5 s leaves room and drains thousands of normal writes | a measured backlog at shutdown that 5 s cannot clear under normal load |
| Keep the closed set as `CHECK (… IN …)` and recycle no connections | relies on the creating connection living forever; Hikari's `maxLifetime` retires it after 30 minutes, so clicks would start failing open in production (DR-04) | an H2 fix, re-verified by the constraint probe |
| No database constraint on the class (application only) | the four tokens are constants in code, but a constraint keeps any other writer, now or later, honest; the lookup FK is verified and costs a four-row primary-key probe | — |
| Handle a delayed request by hashing it with the current day's salt, or a one-off salt | the first links one day's click to another day's hashes; the second breaks same-day stability; reading the clock inside the lock removes the case instead (DR-02) | — |
| `ORDER BY … LIMIT 10` in SQL for the top referrers | the tie order would follow the database collation (PostgreSQL's linguistic collations ignore punctuation); separate statements could also break AC-11 under concurrent writes | an index-only path for very high-cardinality referrers |
| A public lookup in `link/` (code → id) | outside the `link/` grant; the FK already ties `click` to `link.id` | a third feature needing the same lookup |
| Recording the click from a servlet filter or interceptor after the response | it would have to infer "this was a `302` of the redirect route" from status and path, and would see `HEAD` and `429`s too; the hook knows | — |
| A second plain `RANDOM_PORT` class for W1-02 | it would share `ColdStartJourneyTest`'s cached context and silently break that class's "only request" property | — |

## 12. Design probe (what was verified by effect)

`design-probe/ClickProbe.java` boots the shipped main code (functional
profile, in-memory H2 with Flyway V1, Tomcat on a random loopback port). It
creates the `click` table with §3's DDL and runs probe beans that implement
§1 in compact form (reductions, `DailySalt`, the recorder with its bounded
single writer, the store, the fold), behind two probe endpoints standing in
for `RedirectController` with the hook and for `StatsController`. Run from
the repo root:

```sh
scripts/gw --log missions/01-greenfield-core/slices/02-analytics/design-probe/output.txt \
    --offline -I missions/01-greenfield-core/slices/02-analytics/design-probe/click-probe.gradle designClickProbe
```

| Case | Result in `design-probe/output.txt` | Design item |
|---|---|---|
| C0 | the first design's V2 DDL (table, FK `ON DELETE CASCADE`, both `CHECK`s, index) runs on H2 in PostgreSQL mode. That is true but not enough: its `IN`-list check fails once the creating connection is retired (DR-04, superseded by the constraint probe below) | §3 |
| C1 | link created through the shipped `POST /api/links`; its id read by code | link lookup |
| C2 | `HEAD` on the `@GetMapping` route: `302`, handler invoked once, no click stored | rule 1, AC-2 |
| C3 | one redirect with `Referer`, `User-Agent` and `X-Forwarded-For` canaries: `302`, `no-store`; stored row `REFERRER=https://news.example`, `USER_AGENT_CLASS=browser`, a 64-hex hash, no canary, no address; the only log line in the window is `request completed` | AC-3, AC-4, AC-6, AC-18 |
| C4 | time spent in the hook, 1 000 redirects after warm-up: p50 20.4 µs, p95 35.8 µs, p99 46.2 µs | NFR-L3 (indicative) |
| C5 | 26 clicks over three days, 14 origins, 4 without referrer: one grouped query, folded into `totalClicks` 26, three days ascending, top 10 in AC-10's order (`d1`, `d10`, `d11`, `d2` …); `application/json`; `LocalDate` as `"2026-10-01"` | AC-9, AC-10, AC-11 |
| C6 | H2's own `ORDER BY` on the same origins gives code-point order (shown for comparison; the design sorts in Java) | §3 |
| C7 | every write sleeping 2 s: 20 sequential redirects all `302`, slowest reply under 1 ms, 19 writes queued | AC-14 |
| C8 | failing store whose exception message quotes the client hash: `302`; exactly one WARN `click lost` with the redirect's `requestId` and `errorType`, no canary, no hash; both lines of the window carry the id | AC-15, AC-19 |
| C9 | springdoc lists only the `code` path parameter for a handler that also takes `HttpServletRequest` | §1 hook |
| S1 | HMAC daily salt: same address and day equal, other address differs, next day differs, not the unsalted SHA-256 (hex or Base64), 64 characters | AC-5 |
| S2 | a salt created 0.5 s before UTC midnight is gone 1.5 s later with no further click | rule 4 disposal |

**Revision probes (design review DR-01, DR-02, DR-04).** The reviewer's own
probes reproduced the three defects against the first design
(`docs/review/02-analytics/proof/design-boundary.txt`,
`design-h2-constraint.txt`). These two probes run the revised mechanisms:

```sh
scripts/gw --log missions/01-greenfield-core/slices/02-analytics/design-probe/revision-output.txt \
    --offline -I missions/01-greenfield-core/slices/02-analytics/design-probe/revision-probe.gradle designRevisionProbe
scripts/gw --log missions/01-greenfield-core/slices/02-analytics/design-probe/constraint-output.txt \
    --offline -I missions/01-greenfield-core/slices/02-analytics/design-probe/constraint-probe.gradle designConstraintProbe
```

| Case | Result | Finding |
|---|---|---|
| DR-01, 5 s deadline, six 2-s writes (the reviewer's setup) | `close()` returned after 5 006 ms; written `req-1`, `req-2`; `req-3` interrupted (write failed); `req-4`…`req-6` shutdown deadline; every click accounted once | DR-01 |
| DR-01, 500 ms deadline | returned after 504 ms; one interrupted, five cancelled; every click accounted once | DR-01 |
| DR-01, fast store | returned at once, all six written, nothing lost | DR-01 |
| DR-01 ownership (second round), interruption-ignoring store, 300 ms deadline, 6 clicks | `close()` returned after 304 ms with all six already accounted: `req-1` `shutdown deadline, outcome unknown`, five `shutdown deadline` | DR-01 |
| … the stuck write commits after shutdown | written afterwards, marked as committed after its unknown-outcome report; no second report | DR-01 |
| … the stuck write fails after shutdown | no `write failed` report (suppressed by the failed CAS) | DR-01 |
| … start race: shutdown claims before the worker starts | one `shutdown deadline` report, never written | DR-01 |
| … completion race: the worker finishes before shutdown claims | written, no report | DR-01 |
| DR-02, the reviewer's interleaving | the delayed request's hash equals day D's; day D+1 hashed twice gives the same value; D+1 differs from D | DR-02 |
| DR-02, stale `expire(D)` after D+1 began | D+1's hash unchanged | DR-02 |
| DR-02, `expire` of the current day; clock moved back across midnight; scheduled expiry | next stamp draws a new salt; the earlier day gets a fresh salt (the documented boundary); the salt is gone at the day's end without a click | DR-02 |
| constraint probe, memory and file | the table in §3: multi-value `CHECK`s fail with `90098` after retirement, single comparisons, `LENGTH`, FKs and V1's shipped checks keep working; the revised V2 accepts a valid click and rejects an unknown class and a short hash after retirement | DR-04 |

Not verified by me, left to the builder's tests: `@MockitoSpyBean` on a
package-private `@Component` in a `RANDOM_PORT` context; MockMvc per-request
`setRemoteAddr` combined with the shifted clock (both mechanisms already run
in slice 01's suite); `@PreDestroy` draining in a real context close (the
annotation is `jakarta.annotation-api` 3.0.0, already in the dependency
cache through the Boot starters; the probe used `destroyMethod = "close"`);
and
springdoc's exact output for `LinkStats`. Each has a fallback in §7 that
keeps the contract unchanged.

## 13. Build plan

Test-first inside each commit; `scripts/gw check` green on every commit:

1. `feat(02-analytics): click table and reductions`: V2, `Click`,
   `DailySalt`, their unit tests.
2. `feat(02-analytics): record clicks without blocking the redirect`:
   `ClickStore.insert`, `ClickRecorder`, the `RedirectController` hook,
   `ClickRecorderTest`, `ClickRecordingJourneyTest`.
3. `feat(02-analytics): click statistics endpoint`: `findLinkId`,
   `countByDayAndReferrer`, `LinkStats`, `StatsController`, `LinkStatsTest`,
   `StatsJourneyTest`, the granted `OpenApiDocumentTest` line, the
   regenerated `docs/api/openapi.json`.
4. `test(02-analytics): slow, failing and concurrent stores on Tomcat, and
   the schema after connection retirement`: `ClickResilienceJourneyTest`,
   `ClickSchemaJourneyTest`; `ClickSchemaTest` lands with V2 in commit 1.
5. Traceability, coverage reports, `GAPS.md` (NFR-L3 row), proof captures
   (SPEC proof contract), the ERD.

Javadoc per `docs/guidance/java-spring.md` §8 applies to every public type
and method. `package-info.java` for `click/` states the outcome (privacy-safe
click recording and per-link statistics) and the boundary (the one hook in
`link/`, read-only use of `link` by id and code).

## Status

- 2026-10-03 08:22Z — design packet `qitem-20261003082147-22c46f0c` claimed; SPEC `488788f` reviewed PASS (RQ-01 MEDIUM, capture labels), updated in passing at `7200107` (labels only).
- 2026-10-03 08:29Z — territory request and w2 sequencing to the lead (`qitem-20261003082940-4da408ce`); decided 08:30Z: the one-line `OpenApiDocumentTest` grant, `slice.yaml` revision receipt 8; `02` designed before `03`.
- 2026-10-03 08:33Z — design probe run (`design-probe/output.txt`, cases C0–C9, S1, S2).
- 2026-10-03 — design written; ADR-0011 to ADR-0013 and the ADR-0004 amendment drafted; `docs/DESIGN.md`, `erd.mmd`, `container.mmd` and `click-sequence.mmd` updated. No question parked on `human@kernel`. Handed to `design_review` at `71b2e10`.
- 2026-10-03 09:01Z — design review **FAIL** on `71b2e10` (`docs/review/02-analytics/design-review.md`, evidence `38c1001`): DR-01, DR-02, DR-04 HIGH; DR-03 MEDIUM. Rework packet `qitem-20261003090152-625de326` claimed 09:11Z, after `03-operate`'s design handoff (the lead's suggested order).
- 2026-10-03 09:13Z–09:15Z — DR-03 routed to the requirements agent (`qitem-20261003091357-40bfa3a7`) as a SPEC alignment; done at `173bd60` (rule 7, AC-22, A-18).
- 2026-10-03 09:32Z–09:38Z — re-review on `45c98c4` (`891ad99`): DR-02, DR-03, DR-04 closed; DR-01 kept HIGH (an interruption-ignoring write at normal process exit went unreported). Escalated by the reviewer (`qitem-20261003093729-34972a35`); the lead authorized one focused correction (`qitem-20261003093809-6bf44217`). Per-click ownership designed, probed (`revision-output.txt`, ownership cases) and written into §1, §5, §7 and ADR-0011.
- 2026-10-03 09:15Z–09:20Z — constraint probe (`constraint-output.txt`) and revision probe (`revision-output.txt`) run. The constraint probe ran twice: the second run separated V1's two checks and single comparisons from the failing multi-value forms. Design revised; ADR-0011 to ADR-0013 revised; `docs/DESIGN.md`, `erd.mmd`, `click-sequence.mmd` aligned. Handed back to `design_review`.

## Review response

Review `docs/review/02-analytics/design-review.md` on candidate `71b2e10`:
FAIL on DR-01, DR-02 and DR-04 (HIGH), with DR-03 (MEDIUM). Every finding is
answered below; none is disputed. The reviewer's reproductions were right on
all three HIGHs.

| Id | Severity | Response | Where | Evidence |
|---|---|---|---|---|
| DR-01 | HIGH | **Fixed in two rounds.** Round 1: `close()` drains for at most `DRAIN_DEADLINE` (5 s), then `shutdownNow()`, which bounds shutdown (5 006 ms with the reviewer's six 2-s writes). The re-review showed round 1 still lost one report: a write stuck in H2 ignores the interrupt, the pool close does not end it, and a normal JVM exit kills the daemon writer silently (5 of 6 clicks accounted, `design-exit-boundary.txt`). My clause claiming otherwise was unverified, and I said so at 09:33Z. **Round 2 (lead-authorized focused correction `qitem-20261003093809-6bf44217`):** per-click atomic ownership. Each `ClickWrite` has a state the worker and shutdown move with `compareAndSet`, and `close()` claims every unfinished click before it returns. A click still queued is reported `shutdown deadline`; one being written is reported `shutdown deadline, outcome unknown`, because it may still commit. A later completion or failure finds the claim and reports nothing. This is the 09:33Z proposal with one change: the ownership is a state on each click, not a single in-flight reference. A single reference misses a task the worker has dequeued but not yet registered, the start race the lead asked to be tested, and a set of outstanding clicks covers it deterministically | §1, §5, §7.1, §7.3; ADR-0011 | revision probe DR-01 ownership cases (all six accounted when `close()` returns at 304 ms; no duplicate after a late commit or failure; both races deterministic) |
| DR-02 | HIGH | **Fixed** by removing the case. `DailySalt.stamp(address)` reads the clock, picks or rotates the day's salt and copies the key in one locked step, and the recorder takes the click's instant from it. No request can carry an older day into the lock after a newer day rotated the salt, so day D+1's key is never replaced. `expire(day)` acts only on its own day, so a stale callback is a no-op. The remaining boundary is a clock that itself moves back across midnight, which draws a fresh salt for the earlier day; it is written down. Rule 4's same-day stability holds, and no old salt is retained | §1, §4, §6, §7.1, §7.2, §11; ADR-0012 | revision probe DR-02 (the reviewer's interleaving: same-day hashes equal; stale expiry no-op) |
| DR-03 | MEDIUM | **Fixed in the SPEC**, as the reviewer allowed: rule 7 at `173bd60` keeps `HEAD` and `OPTIONS` at the framework defaults, A-18 records it, and AC-22 checks both and that `totalClicks` is unchanged. The design states the contract (§2.1) and adds AC-22 to `StatsJourneyTest` | §2.1, §7.1, §8 | SPEC `173bd60` |
| DR-04 | HIGH | **Fixed.** The closed set of user-agent classes is a four-row `user_agent_class` table referenced by a foreign key; the hash length stays a `LENGTH` check. Both keep accepting valid rows and rejecting invalid ones after the pooled connection that ran the DDL is retired, on memory and file databases. Only multi-value `CHECK`s (`IN`, `OR` of equalities) are affected on H2 2.4.240; V1's shipped checks were tested alone and keep working, so `01-create-redirect` is not affected. The regression is `ClickSchemaTest`: its own Flyway-migrated database and pool, so retiring the pool certainly retires the DDL connection | §3, §6, §7.1, §7.2, §11; ADR-0013; `docs/DESIGN.md` §4 | constraint probe (memory and file, eight forms) |

## Self-check

"Verified" means I read the SPEC clause and the design section side by side;
what was executed is the probe in §12 and the jar and source reads named.

| # | Item | Result | Where |
|---|---|---|---|
| 1 | Every AC reachable, component named | ✔ 22 ACs and 9 rules, each with its class and method | §8 |
| 2 | Every error AC an explicit problem detail | ✔ `404` (two producers), `405`; inherited shape (no `detail`, request-id `instance`), so AC-13's "no code in the body" holds without new code | §2 |
| 3 | Migration has a written rollback | ✔ `DROP TABLE click; DROP TABLE user_agent_class;`, order and scope stated; the constraints verified after connection retirement | §3 |
| 4 | Log/audit events PII-free | ✔ one new event (`click lost`) with request id, reason and exception class only; no event on success; no audit by rule 8; exception messages never logged | §5 |
| 5 | Threat model covers every new entry point | ✔ three redirect headers and the peer address, forwarding headers, the stats path, the table, the salt in memory, shutdown, pooled connections; 19 rows | §6 |
| 6 | Test strategy maps each AC to a suite and names the mechanisms | ✔ four journey classes, five unit classes; settle, polling for rule 6, clock instants, remote addresses, spy reset, concurrency, contexts, connection retirement, bounded close, the midnight interleaving | §7 |
| 7 | No structure beyond the SPEC | ✔ no summary table, no `@Async`, no scheduler framework, no new dependency or property, no service class; every "no" in §11 | §1, §11 |
| 8 | Territory respected | ✔ every path in `slice.yaml`; the `web/` test line by the lead's grant; `link/` limited to the hook | §9 |
| 9 | ADRs for cross-cutting choices before dependent code | ✔ three new ADRs (handoff and overload; hash and salt; storage and aggregation) and the ADR-0004 amendment, indexed in `docs/DESIGN.md` §7 | §10 |
| 10 | Requirements review carry-overs | ✔ all nine rules designed; overload (§1, §5, ADR-0011); five-second visibility with its own test (AC-1 polls); salt disposal (`expire` at day end, **[probe S2]**); correlated private async logs (§5, AC-19); RQ-01 is the SPEC's (done at `7200107`); A-9 and CR-01 stay lead backlog; NFR-L3's number stays a release measurement or a `GAPS.md` row | §1, §5, §7, §12 |
| 11 | W1-02 weighed and decided | ✔ one real-server class, folded into the spy context, with the request-recycling check | §7.1 |
| 12 | Coherence with `03-operate` | ✔ `getRemoteAddr()` so a trusted-proxy rewrite flows through; a `429` is never a click; `03` rebases onto `02` for the document and the test line; ADR numbers from 0014 left to `03`; the 5 s drain fits `03`'s 20 s stop grace | §1, §2.2, §9, §10 |
| 13 | Design review answered | ✔ DR-01 to DR-04 each fixed (DR-03 in the SPEC at `173bd60`), with location and probe evidence; no settled check reopened | *Review response* |

### plan-review (three lenses, applied by hand on the finished draft)

**Strategy — 9/10.** Scope is FR-7 and FR-8 with their NFRs, nothing more.
FR-16's question stays open on purpose; raw rows plus hash and class keep
every answer possible. The Visitor's cost is measured (µs) rather than
asserted.

**Design (API surface) — 8/10; UX n/a.** One read-only endpoint under the
existing resource, the same problem shape, `200` with empty arrays for an
unused link, a retired link's history readable.

**Engineering — 8/10.** Six small classes plus a one-line hook. The riskiest
mechanism, async handoff with correlated logs and fail-open, ran on Tomcat
with a slow and a failing store. The data model has one table, one index and
constraints for the closed sets. Issues found and fixed in the draft:
(1) `HEAD` hits the `GET` mapping, so the skip is in the recorder (probe C2);
(2) a SQL-side day would depend on the binding and the session zone, so the
day is computed in Java and stored (probe C3); (3) a lazily rotated salt would outlive a quiet day, so expiry is
scheduled (probe S2); (4) a second plain real-server class would have
weakened `ColdStartJourneyTest`, so the real-server checks went into the spy
context.

**Recommended action:** approve `SPEC.md` + `design.md` at the delegated
plan-lock once the independent design review passes. No human question is
open.
