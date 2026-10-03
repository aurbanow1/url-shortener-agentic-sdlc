# Design — 02-click-retention

- Slice: `02-click-retention` (mission `02-brownfield`), tier low, plan-lock delegated to the
  orchestration lead (D11). Workflow `urlshort-slice-delegated-b`, judges `review2-agent` and
  `qa2-agent`.
- SPEC: `32b1ae2`. Requirements PASS on `ee7a4de`. RQ-01 was fixed in passing at `96221e8`; the
  AC-4 wording was fixed in passing at `69680e4` at my request (D-AC4 in the SPEC). AC-15 (the
  pause, `4def2fd`) and AC-16 (the human's audit-column decision, `32b1ae2`) were added after the
  first design review.
- Impact analysis, committed before this design: [`impact-analysis.md`](impact-analysis.md) (`16f3de0`).
- Decision records: [ADR-0018](../../../../docs/adr/0018-click-retention-daily-purge.md),
  [ADR-0020](../../../../docs/adr/0020-audit-columns-expand-migration.md) (the audit columns), plus
  amendments to [ADR-0011](../../../../docs/adr/0011-click-handoff-bounded-single-writer.md) and a
  note on [ADR-0013](../../../../docs/adr/0013-click-events-and-request-time-statistics.md).
- Probe: [`design-probe/`](design-probe/) (§12). Author: `design-agent@urlshort-factory`, 2026-10-03.

**In one paragraph.** A new `click.ClickPurge` owns one daemon thread, `click-purge`.
- It runs `DELETE FROM click WHERE clicked_on < :cutoff`, with `cutoff` = the application
  clock's UTC day minus the period.
- It runs once on `ApplicationReadyEvent` and is awaited, so it finishes before Boot reports
  readiness.
- After that, a 5-second tick runs it again at the first tick on or after 00:10Z of each later UTC
  day.
- The period is `urlshort.click.retention-days` (default 90, positive), validated at startup.
  `urlshort.click.purge-enabled=false` puts the purge on hold (no deletion, a WARN at start). The
  functional suite's shared contexts use it, so no purge line can enter a shipped journey's log
  window (design review DR-01).
- Each run logs one INFO line with its count, cutoff and period; a failed run logs one WARN line
  with the exception's class.
- One expand migration, V3, gives `click` and `user_agent_class` the audit columns the human
  decided (AC-16, ADR-0020). Its defaults fill new rows, so the v1 insert is unchanged.
- No index and no new endpoint are needed.
- In passing, a click lost to a reduction failure reports reason `reduction failed`.

Every mechanism claim below was run (§12).

## 1. Components touched

| Component | Change | Specification |
|---|---|---|
| `click.ClickRetentionProperties` | **new** | `@ConfigurationProperties("urlshort.click") @Validated record ClickRetentionProperties(@DefaultValue("90") @Positive int retentionDays, @DefaultValue("true") boolean purgeEnabled)`, Javadoc naming `URLSHORT_CLICK_RETENTIONDAYS`, `URLSHORT_CLICK_PURGEENABLED` and ADR-0018. Same shape as `web.RateLimitProperties`. **`purgeEnabled` is an operator hold:** `false` pauses every deletion, the startup run and the daily run, for example while an incident is investigated or under a legal hold. It is loud: one WARN at every start. The functional suite's shared contexts use it too (design review DR-01, §7) |
| `click.ClickPurge` | **new**, package-private `@Component` annotated `@EnableConfigurationProperties(ClickRetentionProperties.class)` (it is the only consumer, so no separate configuration class) | Specified below. |
| `click.ClickStore` | **one method** | `int deleteBefore(LocalDate cutoff)`: `jdbc.sql("DELETE FROM click WHERE clicked_on < :cutoff").param("cutoff", cutoff).update()`. Javadoc: one statement, one transaction, a table scan by design (ADR-0018). The class Javadoc gains "and the retention delete". |
| `click.ClickRecorder` | **one string** (W2-05, AC-12) | `ClickRecorder.java:93`, the `catch (Exception ex)` around the reduction, logs reason `reduction failed` instead of `rejected`. `rejected` stays for a full or closed queue (`:102`). The class Javadoc's fail-open sentence names the new reason. |
| `click.package-info` | text | "Belongs here" gains the retention purge and its setting. |
| `src/main/resources/db/migration/V3__add_click_audit_columns.sql` | **new**, verbatim from `design-probe/migration/` (AC-16, ADR-0020) | the audit columns on `click` and `user_agent_class`, the backfill and the written rollback (§3). V3 is the next number after `01-audit-read`, which takes none; the builder confirms it after rebasing |
| `src/main/resources/application.properties` | **two settings** (ordered custody: added after `01-audit-read` merges) | after the rate-limit block: a comment `# Click retention (NFR-P2): clicks older than this many UTC days are deleted at startup and daily at 00:10Z; a positive whole number. Overridable by URLSHORT_CLICK_RETENTIONDAYS (ADR-0018).` and `urlshort.click.retention-days=90`; then `# Hold: false pauses every deletion (startup and daily) and logs a WARN at start, e.g. while an incident is investigated or under a legal hold. Overridable by URLSHORT_CLICK_PURGEENABLED.` and `urlshort.click.purge-enabled=true` |
| `src/functionalTest/resources/application-functional.properties` | **one line, granted** by the lead (17:49Z, slice.yaml `cec7032`; design review DR-01) | `# A purge run writes an INFO line with no request id; the shared contexts' log-window journeys require every captured line to carry one (02-click-retention DR-01). The purge journeys turn it back on.` and `urlshort.click.purge-enabled=false`. No test file changes |

**`ClickPurge`**, the whole mechanism. Code sketch; the builder writes the real code, Javadoc included:

```java
@Component
@EnableConfigurationProperties(ClickRetentionProperties.class)
class ClickPurge {

	static final LocalTime DAILY_AT = LocalTime.of(0, 10);          // UTC; recorded in docs/DESIGN.md (AC-8)
	static final Duration TICK = Duration.ofSeconds(5);
	static final Duration CLOSE_DEADLINE = Duration.ofSeconds(3);   // ADR-0018 stop budget

	private static final Logger log = LoggerFactory.getLogger(ClickPurge.class);

	private final ClickStore store;
	private final Clock clock;
	private final int retentionDays;
	private final boolean purgeEnabled;                            // false: an operator hold, no deletion at all
	private final ScheduledExecutorService purger = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, "click-purge");
		thread.setDaemon(true);
		return thread;
	});
	private LocalDate lastRunDay = LocalDate.MIN;                    // touched only on the purge thread

	ClickPurge(ClickStore store, Clock clock, ClickRetentionProperties properties) { … }

	/** The startup run, finished before Boot reports readiness, then the daily tick (rule 3, AC-7). */
	@EventListener(ApplicationReadyEvent.class)
	void start() throws InterruptedException, ExecutionException {
		if (!purgeEnabled) {                                         // an operator hold (and the suite's shared contexts)
			log.atWarn().setMessage("click purge paused, no click is deleted")       // AC-15 (DR-04)
					.addKeyValue("setting", "urlshort.click.purge-enabled").addKeyValue("retentionDays", retentionDays).log();
			return;
		}
		purger.submit(this::run).get();
		purger.scheduleWithFixedDelay(this::tick, TICK.toMillis(), TICK.toMillis(), TimeUnit.MILLISECONDS);
	}

	/** The run the scheduler would start, on the same thread; for the functional suite. */
	void runNow() throws Exception {
		purger.submit(this::run).get(10, TimeUnit.SECONDS);
	}

	void tick() {
		Instant now = clock.instant();
		if (LocalDate.ofInstant(now, ZoneOffset.UTC).isAfter(lastRunDay)
				&& !LocalTime.ofInstant(now, ZoneOffset.UTC).isBefore(DAILY_AT)) {
			run();
		}
	}

	void run() {
		LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
		lastRunDay = today;                                          // a failed run is not retried today (rule 3)
		LocalDate cutoff = today.minusDays(retentionDays);           // the earliest day kept (rule 2, A-1)
		try {
			// ponytail: one DELETE and a table scan per run; past ~2.5 M rows in one startup run,
			// add ix_click_day and id-range batches (ADR-0018, probe L6/L9)
			int deleted = store.deleteBefore(cutoff);
			log.atInfo().setMessage("clicks purged").addKeyValue("deleted", deleted)
					.addKeyValue("cutoff", cutoff.toString()).addKeyValue("retentionDays", retentionDays).log();
		}
		catch (RuntimeException ex) {
			log.atWarn().setMessage("click purge failed").addKeyValue("cutoff", cutoff.toString())
					.addKeyValue("retentionDays", retentionDays).addKeyValue("errorType", ex.getClass().getName()).log();
		}
	}

	/** Takes no new run and waits up to 3 s for one in progress; never interrupts a JDBC call. */
	@PreDestroy
	void close() throws InterruptedException {
		purger.shutdown();
		purger.awaitTermination(CLOSE_DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
	}
}
```

Decisions this fixes (each measured, §12):
- **Clock.** The tick decides on the application `Clock` bean, the one the click recorder stamps
  `clicked_on` with. In the suite that is `FunctionalClock`, so AC-8 runs without a trigger
  (A8b: 4.9 s).
- **Not Spring's scheduler.** Spring's `CronTrigger`, even with `setClock`, sleeps real time once
  armed (S1: 0 runs).
- **One run per UTC day.** `lastRunDay` is set when a run starts, so the rule holds whatever the
  outcome:
  - a failed run waits for the next UTC day (A8g, A8h);
  - a clock that steps back runs nothing (A8d);
  - on a later day nothing runs at 00:09:56Z (A8e), and the run follows at 00:10:03Z (A8f).
- **Startup run before readiness.** `start()` is called before Boot publishes `ACCEPTING_TRAFFIC`:
  in A7b and A7s readiness came 1 ms after the run ended. Three consequences:
  - AC-7 holds before readiness;
  - an invalid setting means no `ApplicationReadyEvent`, so no run (A4);
  - no purge log line can fall inside a shipped journey's request-capture window
    (impact analysis, *Test impact*).
- **No overlap.** The startup run, the ticks and `runNow` share one thread (rule 3).
- **Shutdown.** `close()` never interrupts. A run that outlasts the 3 s wait finishes if the
  JVM lives (D3). If the process exits first, H2 undoes it on the next open (D4).
- **Not proxied.** `ClickPurge` is a plain `@Component` with no AOP; it depends on `ClickStore`,
  so Spring destroys it before the `DataSource`. A package-private `@EventListener` method works:
  `ApplicationListenerMethodAdapter.doInvoke` calls `ReflectionUtils.makeAccessible` (read in
  the spring-context 7.0.9 bytecode). The probe used a public method, so AC-7's tests are the
  by-effect check.

## 2. API and settings contract

- **HTTP: unchanged.** No endpoint, status, header, body, metric or health change. Statistics
  keep their four fields and now cover only the retained window (AC-5); a fully purged link
  answers `200` with `0`, `[]`, `[]` (AC-6), the existing empty shape (baseline). The API document is
  not regenerated.
- **Operator setting.** `urlshort.click.retention-days`, environment variable
  `URLSHORT_CLICK_RETENTIONDAYS`, a positive whole number of UTC days, default `90` (NFR-P2,
  transition 831). It is read at startup, so a change takes effect at the next start, whose
  startup run applies the new period at once.
- **Invalid values stop startup** (rule 1, AC-4, probe A4). For `0`, `-5` and `ninety`, Boot's
  binder fails the context before `ApplicationReadyEvent`, so no purge runs and the old clicks
  remain. Its failure analysis is one ERROR line (ECS JSON). It names the property, the
  rejected value and the value's origin (`commandLineArgs`, or
  `System Environment Property "URLSHORT_CLICK_RETENTIONDAYS"`), and no other configuration
  value. The datasource URL is absent (A4: `false`).
  - A failed validation prints the property as `urlshort.click.retentionDays`; a failed conversion
    (`ninety`) prints `urlshort.click.retention-days`. Both count as naming the setting (D-AC4).
  - Echoing the operator's own rejected value is what AC-4 now states (`69680e4`).

## 3. Data model, queries and migration

**One expand migration, V3: the audit columns (AC-16, design review DR-03; ADR-0020).** It is the
next Flyway number after `01-audit-read`, which takes none (ordered custody: the builder confirms
after rebasing onto `01-audit-read`'s merge). The file is
`src/main/resources/db/migration/V3__add_click_audit_columns.sql`, **verbatim**
[`design-probe/migration/V3__add_click_audit_columns.sql`](design-probe/migration/V3__add_click_audit_columns.sql):
- `user_agent_class` and `click` each gain `created_at` and `updated_at`
  (`TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL`, the database clock) and
  `created_by` and `updated_by` (`VARCHAR(16) NOT NULL`, default `'system'` for classes and
  `'anonymous'` for clicks).
- The backfill sets `updated_at = created_at` for the classes and
  `created_at = updated_at = clicked_at` for existing clicks.
- The header carries the rollback, which drops the eight columns and V3's history row.

What it does and does not change, measured (`output-6.txt`):
- every v1 column, value and constraint is unchanged (M2);
- the v1 insert (`ClickStore.insert`, `ClickSchemaTest`) keeps working unchanged, and new rows get
  `created_at = updated_at` and `anonymous` (M3: 500 of 500; M2b: on a connection left open after
  Flyway's closed, the H2 defect class of `02-analytics` DR-04);
- the rollback restores the exact V2 schema and V3 re-applies (M4);
- it costs 42 to 43 s inside Flyway for 1 300 000 clicks (M5), once, before readiness.

**No index**, as before.

| Query | Where | Served by |
|---|---|---|
| `INSERT INTO click …` | `ClickStore.insert` (unchanged) | primary key, `ix_click_link_day` and the two FK indexes are maintained |
| `SELECT clicked_on, referrer, COUNT(*) … WHERE link_id = :linkId GROUP BY …` | statistics (unchanged) | `ix_click_link_day` range |
| `SELECT id FROM link WHERE code = :code` | statistics (unchanged) | `uq_link_code` |
| **`DELETE FROM click WHERE clicked_on < :cutoff`** | `ClickStore.deleteBefore`, once per run | **a table scan, deliberately** (H2 plans `CLICK.tableScan`: L1, L7, L10). Measured at 1.3 million rows: a steady day (about 14 300 rows) in 1.2 s; 1 000 000 rows in 7.9 s on one link or 32 s spread over 2 000 links |

**Why no `ix_click_day (clicked_on)`.** `docs/guidance/databases.md` §3 wants an index for every
query that needs one. This query was measured with and without one (ADR-0018 table):
- with the index, the plain statement got slower in a catch-up (9.5 s against 7.9 s, L4);
- the index pays off only together with batches, and batches are slower overall (L2, L3, L5,
  L6, L8, L9);
- it would cost one more index write on every click.

The ceiling and the upgrade path are in ADR-0018 *Consequences*.

**Batch forms that do not work on this H2.** A `DELETE … WHERE id IN (SELECT id … FETCH FIRST
10000 ROWS ONLY)` re-runs its subquery for every candidate row. It was cancelled at a 20 s query
timeout without finishing one batch (L0, `batch-subquery-jstack.txt`). This is recorded as a stack
fact in `docs/DESIGN.md` §4.

**Upgrade (AC-13, AC-16).** A data directory written by `f6dd29e` has V1 and V2 applied. The
candidate applies V3: the audit columns are added and backfilled. The startup run then deletes the
clicks beyond the period before readiness.

**Rollback.** `git revert` of the merge commit, then the V3 header's rollback statements on a
stopped copy of the data directory, which restore the V2 schema (M4). Purged rows are gone by
design. `docs/DESIGN.md` §3 tells the Operator to copy `data/` while the service is
stopped before lowering the period, if an undo is wanted.

## 4. Sequences

Source: [`docs/diagrams/purge-sequence.mmd`](../../../../docs/diagrams/purge-sequence.mmd).

```mermaid
sequenceDiagram
    autonumber
    participant B as Spring Boot
    participant P as ClickPurge (click)
    participant T as click-purge thread
    participant K as Clock (UTC)
    participant ST as ClickStore → H2 click
    participant L as stdout (ECS JSON)
    participant V as Visitor / click-writer

    Note over B,ST: startup: Flyway has run, Tomcat has started, readiness is not yet reported
    B->>P: ApplicationReadyEvent
    P->>T: submit(run) and wait
    T->>K: instant() → UTC day T
    T->>T: lastRunDay = T, cutoff = T − P
    T->>ST: DELETE FROM click WHERE clicked_on < :cutoff (one statement, own transaction)
    ST-->>T: n rows
    T->>L: INFO "clicks purged" {deleted n, cutoff, retentionDays P}
    T-->>P: done
    P->>T: scheduleWithFixedDelay(tick, 5 s)
    P-->>B: return
    B->>B: readiness ACCEPTING_TRAFFIC

    par Visitors are never blocked by a run (MVCC row locks)
        V->>ST: INSERT click (today), SELECT statistics
    and every 5 s
        T->>K: instant()
        alt UTC day > lastRunDay and time ≥ 00:10Z
            T->>T: lastRunDay = today, cutoff = today − P
            T->>ST: DELETE FROM click WHERE clicked_on < :cutoff
            alt the store answers
                T->>L: INFO "clicks purged" {deleted, cutoff, retentionDays}
            else RuntimeException
                T->>L: WARN "click purge failed" {cutoff, retentionDays, errorType = class name}
                Note over T: no retry today; the next UTC day's run deletes the same rows
            end
        else same day, before 00:10Z, or the clock stepped back
            T->>T: nothing
        end
    end

    Note over B,ST: shutdown (SIGTERM)
    B->>B: graceful phase ≤ 10 s (the run, if any, keeps going)
    B->>P: @PreDestroy close()
    P->>T: shutdown(), awaitTermination(3 s), never interrupt
    Note over T,ST: a statement still running at exit is undone by H2 at the next open; the next startup run repeats it
```

The redirect and click-recording sequence (`docs/diagrams/click-sequence.mmd`) is unchanged
except for one alternative: when `salt.stamp` or building the `Click` throws, the WARN's reason
is `reduction failed`.

## 5. Logging and audit events

| Event | Logger, level | Message | Members | Never |
|---|---|---|---|---|
| a run that completed | `dev.urlshort.click.ClickPurge`, INFO | `clicks purged` | `deleted` (rows, may be 0), `cutoff` (the earliest UTC day kept, `YYYY-MM-DD`), `retentionDays` | `requestId` (a run is not a request, rule 6), any click value, link code or id |
| a run that failed | same, WARN | `click purge failed` | `cutoff`, `retentionDays`, `errorType` (class name) | the exception's message or stack trace (a driver message can quote values), click values, link code or id |
| a click lost before it was queued | `dev.urlshort.click.ClickRecorder`, WARN | `click lost` | unchanged shape: `requestId`, `reason` **`reduction failed`**, `errorType` | unchanged (ADR-0004 second amendment) |
| the purge on hold (`urlshort.click.purge-enabled=false`) | `dev.urlshort.click.ClickPurge`, WARN, once at every start | `click purge paused, no click is deleted` | `setting` = `urlshort.click.purge-enabled` (AC-15: names the setting), `retentionDays` | as above |
| an invalid period | Boot's `LoggingFailureAnalysisReporter`, ERROR, at startup | `APPLICATION FAILED TO START …` | the property, the rejected value, its origin, the constraint | any other configuration value **in this event**. Hikari's and Flyway's startup INFO lines print the JDBC URL before binding fails; that is shipped behaviour, not this slice's (design review DR-02) |

Exactly one INFO or one WARN per run (AC-9, AC-10). The `click lost` reasons an Operator can
alert on, with their full list in `docs/DESIGN.md` §2:
- `rejected`: a full or closed queue;
- **`reduction failed`**;
- `write failed`;
- `shutdown deadline`;
- `shutdown deadline, outcome unknown`.

**Audit.** None. A run is a scheduled retention action with no request and no actor, while the
audit trail records mutations of links (SPEC A-4, rule 4). The INFO line is the run's record.

## 6. Threat model (STRIDE-lite)

| Threat | Where | Mitigation | Residual |
|---|---|---|---|
| **Tampering / integrity:** clicks deleted early or the wrong rows deleted | cutoff, statement, setting | the cutoff is the application clock's UTC day minus P, deleting `<` it, so day `T−P` is kept (A-1); the statement names only `click` and binds one `LocalDate`; P is validated (AC-4); boundary tests through real redirects at shifted days (AC-1 to AC-3) | **A host clock that steps forward** deletes up to that many days early, and one set years ahead deletes every click. The Operator owns the host clock. The INFO `cutoff` shows it. A backward step deletes nothing (A8d). Accepted |
| Tampering: a period set too low by mistake | setting | positive whole days only; `docs/DESIGN.md` §3 states the effect and the backup step | deletion is irreversible by requirement |
| Retention not enforced: the hold left on | `purge-enabled=false` | a WARN `click purge off` at every start; `docs/DESIGN.md` §3 says clicks then grow beyond the period until the hold is lifted | the Operator's decision, by design (an incident or a legal hold) |
| **Information disclosure:** click values or a driver message in the log | purge events | count, day, period and a class name only (§5); the WARN never carries the message (AC-10's canary) | none |
| Information disclosure: the failure text for the setting | startup | names the setting, the operator's own value and its origin; no other configuration value (A4, D-AC4) | the rejected value appears in the Operator's own log, as AC-4 states |
| **Denial of service:** the purge slows the Visitor | locks, CPU | MVCC: no measured stall (p95 ≤ 7.4 ms during every variant, every click stored; L1–L10); one thread; one run per day | a catch-up delays readiness by 8 to 32 s per million rows (ADR-0018 ceiling, compose's 80 s health window) |
| Denial of service: the stop takes too long | shutdown | at most 3 s added after the graceful phase; worst case 18 s of the 20 s grace (ADR-0018) | — |
| **Repudiation:** a deletion without a record | run | one INFO per run with count, cutoff and period, timestamped | no audit row, by SPEC A-4 |
| **Spoofing / elevation** | — | no endpoint, command or input from a client reaches the purge | — |

Privacy gain: NFR-P2's promise to the Visitor now has an enforced end date.

## 7. Test strategy

Every purge journey runs on **its own in-memory database and context**
(`@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:<class>;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")`).
Its deletions never reach the shared suite database, and no other journey moves its
`FunctionalClock`. Clicks at past days are **recorded through real redirects**: shift that
context's clock, then `GET /{code}` from a dedicated peer (`StatsJourneyTest.openAt` and
`SHIFTED_PEER` pattern; mission 01 NOTES §2 12:40Z), then `ClickRecorder.settle()`. That
proves the purge acts on the day the recorder stores, not on a hand-written fixture. Rows are
inspected with `JdbcClient`. "Run a purge" is `ClickPurge.runNow()`, the same run on the same
thread, except AC-7, AC-8 and AC-13, which send no trigger.

**Isolation from the shipped suite (design review DR-01).** The functional profile's overlay sets
`urlshort.click.purge-enabled=false` (granted, `cec7032`). Every shared context (the default one,
the rate-limit contexts, the click-resilience context, the cold-start context) therefore runs no
startup run and arms no tick. **No purge line can be written while a shipped journey captures
the log**, whatever the time of day or however a journey moves its clock.

The purge journeys that need autonomous runs set `urlshort.click.purge-enabled=true` inline: AC-7,
AC-8 and AC-13. Their contexts are closed after their class (`@DirtiesContext`, or the
programmatic starts closed in the test), so no armed tick outlives them. The other purge journeys
call `runNow()`, which runs whatever the setting. No shipped test file changes.

| AC / rule | Test (class: what it does) | Mechanism |
|---|---|---|
| AC-1, rule 2, A-1 | `ClickRetentionJourneyTest`: two clicks each on `T−91`, `T−90`, `T−89`, `T`; full rows read before; `runNow()` at `T` | the `T−91` rows are gone; the six others are equal column for column |
| AC-2 | same class: the AC-1 fixture rebuilt, run at `T`, then the clock moved to `T+1`, `runNow()` | `T−90` gone, `T−89` and `T` kept |
| AC-3, rule 1 | `ClickRetentionSettingJourneyTest` (`urlshort.click.retention-days=7`, `@DirtiesContext`): clicks on `T−8`, `T−7`, `T`; `runNow()` | `T−8` gone; the run's INFO shows `retentionDays` 7 |
| AC-4, rule 1 | `ClickRetentionStartupJourneyTest`: for `0`, `-5`, `ninety`, a temporary file database (Flyway + one link + three clicks at `T−100` by JDBC); `new SpringApplicationBuilder(UrlshortApplication.class).run("--spring.datasource.url=…", "--server.port=0", "--urlshort.click.purge-enabled=true", "--urlshort.click.retention-days=" + v)` throws; `OutputCaptureExtension` | **scoped to the failure-analysis event (DR-02):** the one ERROR line from `LoggingFailureAnalysisReporter` names `retention-days` or `retentionDays` and `v`, and holds no `jdbc:` URL or credential. The whole capture is not checked for the URL, because Hikari's and Flyway's shipped startup INFO lines print it before binding fails. Separately: three clicks remain, and the whole capture has no `clicks purged` line. **Predicted, not measured:** a failed `run` publishes `ApplicationFailedEvent`, on which Boot cleans up its logging system, inside the functional JVM where cached contexts keep logging. So the three startups stay in this one class, with `.registerShutdownHook(false)` on the builder. If later classes' log lines change format, run the three failing startups in a child JVM, as probe D4 does |
| AC-5, rule 4 | `ClickRetentionJourneyTest`: clicks over `T−95`…`T` with origins seen only on `T−95`…`T−91`; statistics read before; `runNow()`; read again | per-day counts equal from `T−90` on, total = their sum, the old-only origins absent, the four fields unchanged |
| AC-6, rule 4 | same class: link `C` with clicks only at `T−100`; `SELECT * FROM audit_log` before; `runNow()`; statistics → `200`, `0`, `[]`, `[]`; then `GET /C` → `302` and the same `Location`; `settle()`; statistics → total 1 with one element for `T`; `GET /api/links/C` → `200`, creation body | the audit rows are identical and their count unchanged (RQ-01 order) |
| AC-7, rule 3 | `ClickRetentionStartupJourneyTest`: a temporary file database fully migrated, clicks at `T−100` and `T−10`; start the application with `--urlshort.click.purge-enabled=true` and no trigger, close it in the test | when `run(…)` returns (readiness is reported), the `T−100` rows are already gone and `T−10` kept, which is stronger than "within 60 s"; one `clicks purged` line |
| AC-8, rule 3 | `ClickRetentionScheduleJourneyTest` (`urlshort.click.purge-enabled=true`, own database, `@DirtiesContext`): after the context starts, two clicks recorded at `T−90` and two at `T−89`; clock moved to `T+1` 00:10:01Z; **no trigger**; poll the rows for at most 60 s | `T−90` gone within the bound (A8b: 4.9 s), `T−89` kept; one `clicks purged` line with `cutoff` `T−89`. The time 00:10Z is in `docs/DESIGN.md` §3 |
| AC-9, rule 6 | `ClickRetentionJourneyTest` with `OutputCaptureExtension`: a run that deletes n > 0 and a run that deletes 0 | per run exactly one line with `clicks purged`, one JSON object, level INFO, `deleted`, `cutoff`, `retentionDays`; no `requestId`; the test's link code, link id, client hash, a canary referrer origin and the user-agent class are absent |
| AC-10, rule 6 | `ClickPurgeFailureJourneyTest` (`@MockitoSpyBean ClickStore`): `doThrow(new DataAccessResourceFailureException("store down " + CANARY)).when(store).deleteBefore(any())`; `runNow()`; a redirect and a statistics read; `reset(store)`; `runNow()` | one WARN `click purge failed`, `errorType` the class name, no canary, no `error.message` or `error.stack_trace` member; `302` and `200` during the failure; the old rows deleted by the second run |
| AC-11, rule 5 | `ClickRetentionJourneyTest`: (a) a separate connection holds an **uncommitted** `DELETE FROM click WHERE clicked_on < :cutoff` over 50 000 old clicks (inserted by `JdbcClient` batch) while `GET /L`, `settle()` and the statistics run; then roll back. (b) `runNow()` on another thread over 50 000 old clicks while redirects are sent until it ends | (a) `302`, today's click stored and visible, so no lock blocks the insert or the read, deterministically. (b) every redirect `302`, every click stored; the test asserts that at least one redirect started before the run ended |
| AC-12, rule 7 | `ClickPurgeFailureJourneyTest` (`@MockitoSpyBean DailySalt`): `doThrow(new GeneralSecurityException("no HMAC " + CANARY)).when(salt).stamp(anyString())`; `GET /{code}` | `302`; exactly one WARN `click lost` with `requestId` = `X-Request-Id`, `reason` `reduction failed`, `errorType` `java.security.GeneralSecurityException`, no canary. Unit: `ClickRecorderTest.aClickThatCannotBeReducedIsOneWarn` now expects `reduction failed` (impact analysis) |
| AC-13 | `ClickRetentionStartupJourneyTest`: a temporary file database at **V2** (`Flyway.configure()…target("2")`, the `f6dd29e` schema) holding links, audit rows and clicks at `T−10`, `T−90`, `T−91`, `T−120` written in the shipped shapes; full rows read; start the candidate with `--urlshort.click.purge-enabled=true` | every migration on `main` applies with `success`; links, audit rows and the `T−10` and `T−90` clicks equal **in their v1 columns**, and every class too; their four audit columns are filled (AC-16: clicks `created_at = updated_at = clicked_at`, `anonymous`; classes `system`); `T−91` and `T−120` gone. Then a redirect after the upgrade: its new row has `created_at = updated_at` and `anonymous` (AC-16). **By-effect proof (proof contract items 6 and 8)**: the schema capture of `click` and `user_agent_class` (type, nullability) and a sample of old and new rows' audit columns, from the upgraded directory, plus: the real `f6dd29e` jar writes the directory (links, audit rows, today's clicks), then backdated clicks are inserted with H2's Shell while it is stopped (it can only write `clicked_on = today`), then the candidate jar starts on it |
| AC-14 | the shipped functional suite, test files unchanged; the overlay gains the one granted line | no shared context purges (isolation above); the impact analysis lists why each test that stores clicks, captures logs or moves the clock passes |
| DR-01 isolation, the hold | `ClickPurgeHoldJourneyTest` in the **default** shared context (no properties of its own): seed one click at `T−100` by JDBC; move the clock to `T+1` 00:10:01Z; wait 6 s (more than one tick); reset | no `clicks purged` line in the capture; the click is still there (probe A8off: 11 s, 0 runs). Unit: `start()` with `purgeEnabled=false` logs one WARN `click purge paused, no click is deleted`, submits no run and schedules nothing |
| **AC-15**, rule 3 (DR-04) | `ClickRetentionStartupJourneyTest`: a temporary file database fully migrated with clicks at `T−100` and `T−10` **inserted before the start**; start with `--urlshort.click.purge-enabled=false` (`OutputCaptureExtension`). **Observation 1**: move that context's clock 60 s past readiness and wait 6 s (more than one tick). **Observation 2**: move it to 60 s past the next 00:10Z and wait 6 s; close it | exactly one WARN at startup with `setting` = `urlshort.click.purge-enabled` and the message `click purge paused, no click is deleted`; no `clicks purged` line; all four clicks still there at both observation points. With the hold no tick is armed, so 6 s of real time covers "60 s on the suite clock" (A8off measured 11 s) |
| **AC-16** | **unit `ClickAuditColumnsTest`**, `ClickSchemaTest`'s pattern: its own Flyway database. Migrate to V2, insert a link and two clicks with the v1 column list, record every v1 column and the constraints, migrate to latest (V3), insert one more click with the v1 column list | the schema of `click` and `user_agent_class` has the four columns (`TIMESTAMP WITH TIME ZONE` / `CHARACTER VARYING`, `NOT NULL`, defaults); the v1 columns, values and constraints are unchanged; old clicks have `created_at = updated_at = clicked_at` and `anonymous`; classes have `system` and `updated_at = created_at`; the new click has `created_at = updated_at` and `anonymous`; no audit column equals a stored hash, referrer or class (probe M1–M3). Functional: AC-13's journey on the upgraded directory, above |
| rule 3 (once a day, no early retry, backward step, no overlap) | unit `ClickPurgeTest`: a mock `ClickStore` and a hand-moved `Clock`; calls `tick()` and `run()` directly | before 00:10Z → no run; at 00:10Z of a later day → one run with the right cutoff; the same day again → none; a backward step → none; a failure → one WARN, no run until the next day, then a run; `start()` returns only after its run; `close()` returns |

**Unit or functional for coverage.** `ClickPurge`'s branches (the tick's two conditions, success,
failure) are covered by `ClickPurgeTest`. The executor, `start`, `runNow` and `close` are covered
by both suites. 100 % line and branch on merged data (NFR-M1). No exclusion is expected.

**No residual.** The 00:10Z crossing that the first version accepted (design review DR-01) cannot
write a line in a shared context, because none has an armed tick. The only contexts with one are
the AC-8 class, closed after itself, and the programmatic starts of AC-7 and AC-13, closed in
their tests. No shipped journey runs in either.

## 8. Reachability check

| Mechanism | Reached by |
|---|---|
| `start()` | `ApplicationReadyEvent` in every context and every jar start (A7, A8a). With the hold on (`purge-enabled=false`, the functional overlay) it logs the WARN and returns (A8off) |
| `tick()` → `run()` | the 5 s fixed delay after `start()` (A8b, A8f, A8h) |
| `runNow()` | the functional suite only (package-private; the `click` package's journeys) |
| `close()` | context close (D1–D4) |
| `deleteBefore` | `run()` only |
| `reduction failed` | `ClickRecorder.record` when `salt.stamp` or `Click` construction throws |
| the setting | Boot's binder, from `application.properties` or `URLSHORT_CLICK_RETENTIONDAYS` (A4, A8a) |

## 9. Territory

| Path | Use |
|---|---|
| `src/main/java/dev/urlshort/click/` | `ClickPurge`, `ClickRetentionProperties`, `ClickStore`, `ClickRecorder`, `package-info` |
| `src/test/java/dev/urlshort/click/` | `ClickPurgeTest`; `ClickRecorderTest` (one assertion) |
| `src/functionalTest/java/dev/urlshort/click/` | `ClickRetentionJourneyTest`, `ClickRetentionScheduleJourneyTest`, `ClickRetentionSettingJourneyTest`, `ClickRetentionStartupJourneyTest`, `ClickPurgeFailureJourneyTest`, `ClickPurgeHoldJourneyTest` |
| `src/main/resources/application.properties` | two settings; **second holder**, added after `01-audit-read`'s merge (the candidate descends from it) |
| `src/functionalTest/resources/application-functional.properties` | **one line, granted** by the lead 17:49Z (slice.yaml `cec7032`): `urlshort.click.purge-enabled=false` (DR-01) |
| `src/main/resources/db/migration/` | `V3__add_click_audit_columns.sql` (AC-16); V3 is the next number after `01-audit-read` (ordered custody) |

**Grant request for the lead at plan-lock.** One line of `README.md`: add
`URLSHORT_CLICK_RETENTIONDAYS` to the sentence listing operator settings (line 18–20), so the README
matches the shipped behaviour (brownfield §5). If it is refused, `docs/DESIGN.md` §3 is the
record and the README gap goes to the lead's backlog.

Mine, done in this design step: `docs/DESIGN.md`, `docs/diagrams/container.mmd`,
`docs/diagrams/purge-sequence.mmd`, ADR-0018, the ADR-0011 amendment, the ADR-0013 note.

## 10. Decisions recorded as ADRs

- **ADR-0018** (new): the setting and its validation, the cutoff rule, one `DELETE` per run, no
  index, the startup run before readiness, the 5 s tick on the application clock with 00:10Z and
  `lastRunDay`, no overlap, the failure handling, the events, the 3 s close and the stop budget.
  It also gives the measured alternatives and the ceiling.
- **ADR-0020** (new, design review DR-03): the audit columns. It records database-clock defaults,
  the constant actors `anonymous` and `system`, the backfill from `clicked_at`, and one expand
  migration with a written rollback. It is the pattern for `04-audit-columns`.
- **ADR-0011 amendment:** the purge is a second owned executor, still no scheduler framework, with
  separate shutdowns.
- **ADR-0013 note:** the purge it sketched needs no index.

No ADR-0004 amendment: the purge's events follow its rules as written. A run is not a request,
logs no client value and passes no throwable.

## 11. Trade-offs

| Chosen | Over | Because | Revisit when |
|---|---|---|---|
| one `DELETE` per run, table scan | id batches, with or without `ix_click_day` | fastest in every measured case, no migration, no stall (L1–L10) | a single startup run must delete more than ~2.5 M rows (compose's health window), or the stop budget tightens |
| a 5 s tick deciding on the application clock | `@Scheduled` cron | follows the suite clock (AC-8 by effect); robust to clock steps and host sleep; S1 shows cron cannot | never for AC-8; a shared executor if a third job appears (ADR-0011) |
| startup run awaited before readiness | after readiness, not awaited | shipped log-window journeys stay deterministic (FR-13); AC-7 holds before readiness | catch-up sizes approach the health window |
| `lastRunDay` set at a run's start | retry a failed run sooner | rule 3: no tighter retry loop | — |
| the hold `purge-enabled`, set `false` in the suite overlay | filter the purge logger in the six shipped log-window classes; a test-only switch | the setting has its own operational reason (an incident or a legal hold, the lead's condition at 17:49Z) and is loud; no shipped test file changes; isolation is deterministic (DR-01) | a hold that must survive restarts beyond configuration, for example a per-link hold |
| 00:10Z | 00:00Z or an off-peak hour | deletes a day ten minutes after it leaves the window; stays clear of the day boundary the salt rotates on (ADR-0012) | the Operator asks for a setting |
| a constant run time | a setting | the SPEC asks only for the period; YAGNI | an Operator needs another hour |
| no audit row | a row per run | SPEC A-4 | FR-17's readers ask for one |

## 12. Design probe (what was verified by effect)

`RetentionProbe.java` runs the purge exactly as §1 specifies, inside the shipped application on
the functional classpath, on H2 file databases. No product file was touched. The gradle file has
the commands.

| Row | Setup | Result | Proves |
|---|---|---|---|
| baseline | shipped jar, fresh directory (`baseline-f6dd29e.txt`) | `302`, one row with `clicked_on` 2026-10-03, `ix_click_link_day` only, plan scans it | §3, impact analysis |
| S1 | `ThreadPoolTaskScheduler.setClock(suite clock)` + `CronTrigger("0 10 0 * * *", UTC)` armed at noon, clock moved past the next 00:10Z | **0 runs** after 7 s | §1 (not Spring's scheduler) |
| A8a | context start | the startup run done 5–7 ms after `ApplicationReadyEvent`; default 90 bound | AC-7 mechanism, the setting |
| A8b | clicks at `T−90`, `T−89`; clock to `T+1` 00:10:01Z, no trigger | `T−90` deleted after **4.9 s**, `T−89` kept | AC-8 |
| A8c, A8d | two more ticks; clock stepped back 3 days | no second run that day; no run on a backward step | rule 3 |
| A8e, A8f | day `T+2`, later than the last run's day `T+1`: clock at 00:09:56Z, then 00:10:03Z | no run at 00:09:56Z, one at 00:10:03Z. The day condition held in both, so together they isolate the time-of-day gate | the 00:10Z gate |
| A8g, A8h | the store throws (message with a canary), one more tick, next day | one WARN with the class only; no retry that day; the next day runs | AC-10, rule 3 |
| A4 | `0`, `-5`, `ninety` | each stops startup (`BindValidationException`, `NumberFormatException`); the failure-analysis event names the setting, echoes the value and its origin, no datasource URL; **purge started 0, old clicks 3 of 3 left**. **Corrected (DR-02):** the probe ran with `root=warn`, which hid Hikari's and Flyway's startup INFO lines; at shipped levels those print the JDBC URL, so "no URL" holds for the failure-analysis event only (review2's `invalid-setting-control.txt`) | AC-4, D-AC4 |
| A8off | `urlshort.click.purge-enabled=false`; clock moved to `T+1` 00:10:01Z for 11 s, no trigger (`output-5.txt`) | one WARN (the probe's wording was `click purge off`; the design's names the setting, DR-04); **no startup run, no daily run**; the `T−90` clicks still there | DR-01 isolation, the hold, AC-15 |
| M1 | V1–V3 on an empty database (`output-6.txt`, `MigrationProbe.java`) | the four columns on each table, `NOT NULL`, defaults `CURRENT_TIMESTAMP`, `'anonymous'` / `'system'` | AC-16 schema |
| M2, M2b | a V2 directory with a link and two clicks, upgraded to V3; then a v1-shaped insert on the connection that kept the database open while Flyway's closed | every v1 column, value and constraint unchanged; old clicks `created_at = updated_at = clicked_at`, `anonymous`; classes `system`, equal times; the insert gets equal times and `anonymous` | AC-13, AC-16, the H2 connection-retirement case |
| M3 | 500 v1-shaped inserts on a new connection | 500 of 500 with `created_at = updated_at`, `anonymous` | v1 insert compatibility (AC-14) |
| M4 | the header's rollback statements, then V3 again | exact V2 columns and history; V3 re-applies, every click backfilled | NFR-X2 rollback |
| M5 | V3 on 1 300 000 clicks | 42 to 43 s inside Flyway; every click backfilled | upgrade cost (ADR-0020) |
| L0 | `DELETE … WHERE id IN (SELECT … FETCH FIRST 10000 ROWS ONLY)` | cancelled at a 20 s timeout, H2 re-runs the subquery per row (`batch-subquery-jstack.txt`) | §3 stack fact |
| L1 / L4 | 1 000 000 of 1 300 000 old, one link, one `DELETE`, without / with `ix_click_day` | 7.9 s / 9.5 s; redirects p95 2.5 / 1.6 ms, every click stored | no stall; the index does not help |
| L2 / L3 | same, select ≤ 10 000 ids then delete them, without / with the index | 43 s / 44 s, longest batch < 0.5 s; no stall | batches are slower |
| L5 / L6 / L7 | 2 000 links, a steady day (14 286 of 1.3 M): batches / batches + index / one `DELETE` | 2.1 s / 0.9 s / **1.2 s**; redirects p95 ≤ 7.4 ms, every click stored | steady-state cost |
| L8 / L9 / L10 | 2 000 links, 1 000 000 old: batches / batches + index / one `DELETE` | 55 s / 36 s / **32 s**; no stall | catch-up cost, the ceiling |
| A7b / A7s | startup run awaited, catch-up of 1 000 000 over 2 000 links, batches / one `DELETE` | readiness `ACCEPTING_TRAFFIC` 1 ms **after** the run (65.5 s / 33.6 s) | the startup run precedes readiness |
| D1 | close during a batched run (6 batches done) | `close()` 615 ms; progress kept; the restart finished the rest | (batches' shutdown) |
| D2 | close 1 s into one 1 000 000-row `DELETE`, 10 s wait | `close()` waited 8.0 s, the run completed, the file reopened consistent | the wait |
| D3 | same, with the purge's wait at 2 s (the design's is 3 s) so the `DELETE` outlasts it, 20 redirects just before the stop | `close()` 2 034 ms; pool closed 9 ms later without waiting; the `DELETE` still committed 7.7 s after close began; **20 of 20 clicks written** | past the deadline |
| D4 | child JVM: one `DELETE` running, `System.exit` → Spring's shutdown hook, 3 s wait, JVM ends | the file reopened (4.0 s) **consistent, every old row present** (the delete undone), kept rows intact | process exit mid-run |

Not run by the probe: the dev's actual class (the probe's twin differs only in its switches); a
real `SIGTERM` to the jar (D4 used `System.exit`, which runs the same shutdown hook); PostgreSQL.

## 13. Build plan

1. `test(02-click-retention): a click reduction failure reports its own reason`:
   `ClickRecorderTest`'s assertion and the AC-12 journey. Red.
2. `fix(02-click-retention): click lost reason "reduction failed" (W2-05)`. Green.
3. `test(02-click-retention): retention purge journeys and tick rules`: the five functional classes
   and `ClickPurgeTest`. Red.
4. `feat(02-click-retention): daily click purge with a retention setting`:
   `ClickRetentionProperties`, `ClickStore.deleteBefore`, `ClickPurge`, `package-info`. Green
   with the default from the record. AC-3's journey sets its period inline.
5. When `01-audit-read` has merged: rebase `slice/02-click-retention` onto `main`, then
   `feat(02-click-retention): shipped retention setting`: the `application.properties` lines
   (ordered custody). Run `scripts/gw check`, commit the coverage reports, and hand off with that
   SHA. If the build is done earlier, exit `waiting` blocked on `01-audit-read`'s frontier packet
   (slice.yaml).

ADR-0018 and the amendments are on `main` with this design, before any dependent code (NFR-M2).

## Status

- 2026-10-03 — design written on SPEC `69680e4`, impact analysis first (`16f3de0`); handed to
  `design_review`.
- 2026-10-03 — re-review **FAIL** on `59c8762` against SPEC `32b1ae2` (review commit `a2ec57f`):
  DR-01 and DR-02 settled; new DR-03 HIGH (AC-16 migration absent) and DR-04 MEDIUM (the hold's
  WARN must name the setting; AC-15 mapping). Both fixed; see *Review response*. Rework packet
  `qitem-20261003182019-5b56b6ce`.
- 2026-10-03 — design review **FAIL** on `be80306` (`docs/review/02-click-retention/design-review.md`):
  DR-01 HIGH, DR-02 MEDIUM. Both fixed; see *Review response*. Rework packet
  `qitem-20261003173636-643c7c17`.

## Review response

| Id | Severity | Response |
|---|---|---|
| DR-01 | HIGH | **Fixed, deterministically, with no shipped test file changed.**<br>**The fix.** A new setting `urlshort.click.purge-enabled` (default `true`). `false` is an operator **hold**: no startup run, no daily run, one WARN `click purge off` at every start. Its operational reason is pausing deletion while an incident is investigated or under a legal hold; the lead made that a condition at 17:49Z, because a switch that exists only for tests was rejected for the limiter. The functional profile's overlay sets it `false`, one line granted by the lead (slice.yaml `cec7032`).<br>**Its effect.** No shared context (default, rate-limit, click-resilience, cold-start) runs a startup run or arms a tick. The reviewer's legal overlap, a scheduled run inside `ObservabilityJourneyTest`'s window, cannot arise at any time of day or under any clock move.<br>**The purge journeys** that need autonomous runs (AC-7, AC-8, AC-13) set it `true` and close their contexts after use; the others call `runNow()`.<br>**Proof.** By effect: probe A8off (`output-5.txt`) shows 0 runs in 11 s past the next 00:10Z with the click still stored. In the suite: `ClickPurgeHoldJourneyTest` in the default context (§7).<br>**Rejected.** Filtering the purge logger in the six shipped classes that assert a request id on every line (`ObservabilityJourneyTest`, `ColdStartJourneyTest`, `PingJourneyTest`, `RateLimitJourneyTest`, `StatsJourneyTest`, `ClickResilienceJourneyTest`) would change shipped tests outside this territory. Logs are not suppressed, and no request id is attached.<br>**Also routed.** I asked requirements to note the hold under rules 1 and 3 and the overlay line under AC-14. §5, §6, §7, §9, §11 and §12 are updated, as are ADR-0018, `docs/DESIGN.md` and the impact analysis. |
| DR-03 (re-review of `59c8762`) | HIGH | **Fixed.** SPEC `32b1ae2` added AC-16, the human's audit-column decision, after the design's basis.<br>**The migration.** It is now designed and measured. One expand migration, `V3__add_click_audit_columns.sql`, verbatim in `design-probe/migration/` (§1, §3). Each of `click` and `user_agent_class` gains `created_at`/`updated_at` (database clock, `DEFAULT CURRENT_TIMESTAMP NOT NULL`) and `created_by`/`updated_by` (`'anonymous'` for clicks, `'system'` for classes, `NOT NULL`).<br>**Its consequences.** The defaults fill new rows, so the v1 insert and the shipped tests are unchanged. The backfill sets old clicks to `clicked_at` and the classes to the migration time, with `updated_at = created_at`. The header carries the rollback.<br>**Measured** (`output-6.txt`):<br>• M2: v1 columns, values and constraints unchanged, every old row filled;<br>• M2b, M3: v1-shaped inserts get equal times and `anonymous`, including on the connection kept open after Flyway's closed;<br>• M4: the rollback restores V2 exactly and V3 re-applies;<br>• M5: 42 to 43 s on 1 300 000 clicks.<br>**Recorded and tested.** ADR-0020 records the pattern for `04-audit-columns`. Tests: unit `ClickAuditColumnsTest`, AC-13's upgrade journey with a post-upgrade redirect, and the proof-contract schema capture (§7). V3 is the next number after `01-audit-read`, which takes none. `link` and `audit_log` stay with `04-audit-columns`. The impact analysis and `docs/DESIGN.md` (ERD, data model) are updated. |
| DR-04 (re-review of `59c8762`) | MEDIUM | **Fixed.** The hold's WARN is now `click purge paused, no click is deleted`, with `setting` = `urlshort.click.purge-enabled` and `retentionDays`, once at every start (§1, §5). AC-15 has its own row (§7): clicks older than the period are inserted before the start; the WARN is asserted to name the setting; no purge INFO; every click still present at both observation points, 60 s past readiness and 60 s past the next 00:10Z on the suite clock, each followed by more than one tick of real time. |
| DR-02 | MEDIUM | **Fixed.** AC-4's assertion is scoped to the failure-analysis ERROR event. That event names the setting and the value and holds no `jdbc:` URL or credential. Hikari's and Flyway's shipped startup INFO lines print the URL before binding fails, so the whole capture is checked only for the absence of `clicks purged`, and the rows for being unchanged (§7). The A4 probe row and §5 now say the probe ran with `root=warn` and that "no URL" holds for that event only. |

## Self-check

- **Every AC has a mechanism and a named test** (§7), and every rule maps to an AC or a unit test.
  AC-8 is proven without a trigger, by effect (A8b), not relabelled from a direct call.
- **Every mechanism claim was run** (§12). These were written from output, not from memory:
  - Spring's scheduler cannot follow the suite clock;
  - the startup run precedes readiness;
  - an invalid setting runs no purge;
  - the lock behaviour;
  - the shutdown past its deadline, and a process exit.

  The one batch form I first wrote stalled (L0), and the record keeps it.
- **The requirements review's continuations are honoured:**
  - RQ-01's order is in AC-6's test;
  - AC-8 uses real scheduling;
  - ordered custody: the properties line is added after `01-audit-read` merges, and no Flyway
    number is taken;
  - mission 03 Q4 re-checked at its source (transition 876, Q4 A).
- **The documents agree with the decision.** `docs/DESIGN.md` §3 no longer says "no scheduler
  framework" without the purge. ADR-0011's "a second asynchronous feature" is answered.
  ADR-0013's index sketch has a note. The `click lost` reason list includes `reduction failed`.
- **Out of scope stays out:** no aggregation, no endpoint, no metric, nothing about links, audit
  rows or idempotency bindings.
- **Not verified:**
  - the shipped functional suite against a real candidate (AC-14, QA);
  - PostgreSQL;
  - `SIGTERM` to the jar (release's `--drain`);
  - whether three failed startups inside the functional JVM disturb the cached contexts' logging
    (§7 AC-4 row, with its fallback).

## Plan review (author's lenses; the skill was not invoked separately)

- **Engineering.**
  - The probe changed three decisions: no batches, no index, and the startup run awaited.
  - It also caught one coupling with the shipped suite: the log-window journeys.
  - The stop budget is arithmetic against compose's grace, not a guess.
  - The tick reads the same `Clock` bean the recorder stamps with, so there is one notion of
    "day".
- **Strategy.**
  - The slice stays the decided delete (transition 831; Q4 A at transition 876).
  - The period is a setting and the mechanism is "delete by stored day", so a different number
    from mission 03 needs no code.
  - Aggregation would sit on top of this, not replace it.
- **Operator experience.**
  - One setting, validated loudly at startup.
  - One line per run that answers "is it holding?", with count, cutoff and period.
  - A failure says so once a day, not in a loop.
  - The documented backup step comes before lowering the period.
  - No new metric, by the SPEC's scope.
