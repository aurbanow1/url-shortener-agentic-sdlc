# PRE-REVIEW of the merged range at `ed2b940` (draft; wave_review adds 04-audit-columns)

**Draft verdict: no MUST-FIX or HIGH in the reviewed product range.** One
MEDIUM documentation finding was fixed in passing and independently rechecked
at `2d3de57` / `bfc642d` below. This is the primary
pre-review assigned by `qitem-20261003231757-7d90ba52`, not the lifecycle
`wave_review`, acceptance of the fifth slice, or a release verdict.
Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-03.

## Context and exact boundary

The doghouse is a working shortener with local audit reads, deletion of expired
clicks, two observed dogfood defects corrected, and visible CI/CD checks. Existing
links, audit writes and response bodies must retain their meaning. The remaining
`04-audit-columns` slice supplies V4 for `link` and `audit_log`; its implementation
is outside this pre-review. Mission drills and release claims need their own final
evidence, not an inference from this green gate.

Reviewed accumulated product/tool changes on `main`, **`8e9c065..ed2b940`**;
exact head **`ed2b940dafb71104bebe392d133191fd88b3bd7d`**, in the clean detached
worktree `.worktrees/review-wave02-ed2b940`. Merges: CI/CD `0aa3695`, audit read
`cb148c4`, dogfood `5c264db`, retention `ed2b940`. The range also contains the
mission-01 release smoke/bench corrections and wrapper changes; these are
accounted for below rather than attributed to a mission-02 slice.

Read the mission brief and amendments, the four slice contracts/design context,
the shared architecture and D20 register, the accumulated diff and relevant
proof records. Confidence is high for the source and merged gate; hosted runs,
installed captures and later V4 behavior retain the qualifications stated below.
The architecture register advanced during this review: **`5f90090` has 13 rows**,
so the table covers the original 11 and both additions. The other wave reviewer's
report was not used to form this primary verdict.

## Independent verification

- Fresh `scripts/gw --log <review-log> --offline check --rerun-tasks` at the
  exact head: **213 unit + 224 functional tests, zero failures/errors/skips**;
  14 tasks executed, Javadoc passed. [Gate log](proof/pre-wave-check-ed2b940.txt).
- Merged coverage **554/554 lines, 200/200 branches**. Only fresh `test.exec`
  and `functionalTest.exec` exist in this new worktree; no old auxiliary replay
  contributes coverage. Unit alone: 487/554 lines and 194/200 branches;
  functional alone: 520/554 and 166/200. These are not separate 100% claims.
- [Reconciliation script](proof/pre-wave-audit.py) and
  [result](proof/pre-wave-audit-ed2b940.json): all 43 selected product/tool paths
  hashed; 11 audit paths equal reviewed `7ac8af5`, six dogfood paths equal
  `4fe7042`, three workflows equal `add7ab5`, and all 16 non-context retention
  paths equal `a8fc8b6`. Earlier full-file reviews remain applicable to those
  identical bytes; the new gate checks their combination.
- Live generated OpenAPI equals the committed document as parsed JSON; the
  audit route and corrected shared problem schema are both present. The
  functional equality/conformance tests also pass in the merged run.
- [Actual benchmark control](proof/bench-response-control.mjs) and
  [outcome](proof/bench-response-control.txt): complete response exits 0;
  truncated body exits 13 with no latency report on Node 24.18.0. Thus the
  suspected silent success/hang was not reproduced; no finding. This is not
  a new service latency measurement or validation of every supported Node version.
- Product/tool `git diff --check` passed. No product, test, SPEC, design, branch
  merge, tag, publication or shared ledger edit was performed by this review.

## Claims against source and SPEC

| Outcome / criteria | Accumulated-source verdict and evidence |
|---|---|
| Audit read AC-1–10, AC-20 | PASS. `AuditTrail` uses one bound keyset SELECT, `limit + 1`, explicit columns and descending identity. Empty/last pages, row equality, bounds, stable traversal and concurrent-commit qualification remain covered by the unchanged audit journeys. Sequence is not commit order; the SPEC explicitly bounds the guarantee. No audit write is added by reading. |
| Audit boundary AC-11–16, AC-21 | PASS. `AuditController:59–67` requires strategy NONE and both Tomcat rewriting headers unset, then checks the connection and rejects forwarding headers before parameters. The original RemoteIpValve defect stays resolved; real-server setting/header tests pass in this merged run. Errors retain the shared ProblemDetail and correlated, content-free logging. A same-host relay that removes forwarding evidence remains the documented deployment boundary, not authentication. |
| Audit compatibility/document AC-17–19 | PASS. Explicit audit columns and no audit migration preserve the shipped write side; upgrade journey passes. Live OpenAPI includes the audit response, paging, error responses and inherited 429 after dogfood's customizer. Prior installed-jar evidence remains attributed to QA, not rerun here. |
| Retention AC-1–3, AC-5–8, AC-11 | PASS. `ClickStore.deleteBefore` binds the strict UTC-day cutoff; the boundary day survives. Startup run is awaited, the serial daily tick follows the injected clock, and the linked statistics naturally read retained rows. Fresh boundary, moving-day, empty-before-verification-redirect, daily schedule and concurrent-delete journeys pass. No link or audit row is changed. |
| Retention AC-4, AC-9/10/12/15 | PASS. Positive validated period; invalid startup fails before purge. Default on; explicit hold disables startup and scheduling and emits the naming WARN. Purge events contain count/cutoff/period or exception class, not stored click data. Reduction failure now says `reduction failed`, while queue refusal stays `rejected`; the correlated fail-open journey passes. |
| Retention AC-13/14/16 | PASS for V3 at this head. Expand-only V3 adds/backfills eight columns on click/reference tables with constant actors and write-time defaults; legacy columns and constraints survive. The header rollback was read: drop those columns and V3 history only. It cannot recover purged rows; a stopped-copy backup is needed. The authorized functional hold isolates inherited request-log windows. V4-sensitive test assumptions are separately tracked below. |
| Dogfood AC-1–6, AC-9 | PASS. One OpenAPI customizer replaces the incorrect `properties` member with optional `errors`, whose item requires field/rule/message; response producers are unchanged. One native MeterFilter drops `path` tags. All six reviewed files are identical to `4fe7042`; fresh conformance/live equality, pathless-gauge and inherited tests pass. Historical red-first controls and six full-body pairs are preserved in the prior review/QA record, not rerun as new evidence. |
| Dogfood AC-7/8 and living documents | Core corrected schema/metric descriptions are present. `docs/DESIGN.md` at this head still labels now-merged retention/dogfood pieces designed; GAPS has conditional-open V3 rows and historical X′ pending text. These are conservative stale statuses, not missing product behavior. Finish the already assigned shared-document reconciliation `qitem-20261003195138-8eb72ecb` before release/export; preserve historical observations. |
| CI/CD AC-1–12 | PASS. Workflows retain unfiltered PR gate, main/dispatch triggers, Java 21, full check without skips, read-only permissions, pinned actions, bounded concurrency, always-upload reports, loopback installed jar smoke and local image build. No publish/deploy/secret step. Files match reviewed `add7ab5`; this merged local gate passes. Product build/dependency declarations, Dockerfile and compose are unchanged in the range. |
| CI/CD AC-13 | Operator's committed record `b6b4a29`, subsequently judged by QA, records successful PR CI on `2e33568` and main CI/CD on `a3d6867`, including jar/smoke artifacts. Main runs included Gradle 9.8; prior runs were cancelled by concurrency. I read that record, not fresh hosted logs. QA's failed direct-access attempts and permitted source fallback remain disclosed. No claim of a hosted run on `ed2b940`. |

## Retention X to X′ and the shared seams

[Own range-diff](proof/retention-range-diff.txt), from
`git range-diff 16312da..a8fc8b6 2ead709..a2c34c1`, shows six identical patches.
Only the seventh patch's `application.properties` **context** changes: it now
follows the audit strategy pin rather than the old rate-limit tail. Its added
retention lines and README change are identical. There is no additional product
fix hidden in this rebase. The 16 remaining retention files are byte-identical
to reviewed X. X′ `a2c34c1` is the merged parent of `ed2b940`; the lead's 23:08Z
record names QA custody receipt 11 / `5ae084c`, and this independent merged
gate supplies fresh combination evidence. This does not relabel the original
QA captures as taken on X′.

| Seam | Verdict |
|---|---|
| Configuration custody | `application.properties:14–19` retains NONE, retention 90 and purge true together. No default from either slice was lost. |
| OpenAPI custody | Audit schemas/path coexist with dogfood's single corrected ProblemDetail. Committed/live equality passes; retention does not touch the API document. |
| Functional overlay | `application-functional.properties:9–11` pauses purge in shared contexts. Startup/daily tests opt in in isolated contexts. No log filter suppresses a real background event; jobs correctly have no request ID. |
| Loss vocabulary | `ClickRecorder:94` is `reduction failed`; queue overload remains `rejected`; write/shutdown reasons unchanged. DESIGN lists all five tokens, although its merge-status qualifier needs the shared-document cleanup. Analytics-v2's counters are not yet in this range. |

## Cross-cutting register — D20

| Concern | Consistency verdict at this range |
|---|---|
| Client identity / proxy trust | Audit guard and NONE pin agree with ADR-0019. Existing W2-02 drift remains: `ClickRecorder:87` hashes the peer while `RateLimitFilter:70,96` resolves trusted forwarding. Preserve its forward fix in mission-03 analytics-v2; do not call that unmerged fix complete. No new identity implementation in this range. |
| Time | Retention reuses `LinkConfig`'s Clock and stored UTC day; V3 row-write defaults deliberately use the DB clock. Ping's direct time is the known LOW exception below. No additional business clock introduced. |
| Schema change | One new migration, V3, with additive backfill and literal rollback. Version ownership preserved through rebase. The two latest-version tests need their already assigned V4 correction. |
| Audit columns | Click and user_agent_class comply at V3. Link/audit_log remain explicitly deferred to V4 in GAPS, which names the migration owner. Final wave review must check all tables after that merge. |
| Error shape | Same Advice/Problems contract and filter 429. Audit reuses it; dogfood corrects the one document schema without a second response format. |
| Request ID / logging | RequestIdFilter and writer MDC restoration unchanged. Background purge uses fixed job fields with no false request ID; inherited capture isolation is explicit. Fresh failures/privacy tests pass. |
| Audit trail writes | AuditLog remains the append-only writer inside link transactions; AuditTrail is a reader with explicit columns. The purge's lack of an audit row is retention SPEC rule 4/A-4, with its own count/cutoff log. Statistics do change when rows expire: do not interpret the register's housekeeping shorthand as claiming otherwise. |
| Client hashing | DailySalt/key lifecycle unchanged; purge removes entire expired rows rather than retaining another identifier form. Input-identity drift is the same W2-02 above, not a second finding. |
| Metrics / health | Same exposure list and static rejection labels; native `ignoreTags("path")` removes the installation path without removing gauges. A second disk path would require a safe distinguishing tag, an explicit accepted ceiling. |
| API document | One OpenApiConfig and one live-export equality gate; ordered audit/dogfood changes survive together. |
| CI/CD | One gate and package workflow plus two weekly update ecosystems. Existing download-resilience backlog remains; no parallel gate or publishing path added. |
| Background work (new row) | Two owned JDK executors and bounded close waits are appropriate. **W2P-01:** the row incorrectly generalizes purge's no-interrupt behavior to the writer; source and bytecode contradict that wording. |
| Operator settings (new row) | Validated/defaulted property records and shipped defaults agree. `URLSHORT_CLICK_PURGEENABLED` is documented in properties and DESIGN but missing from README's list; include it in shared-document reconciliation (LOW, CONTEXT-GAP: the locked design requested only the retention entry there). |

## Findings and dispositions

| ID | Severity / class | File:line | Evidence and required change |
|---|---|---|---|
| W2P-01 | MEDIUM / JUDGMENT-GAP | `docs/adr/0011-click-handoff-bounded-single-writer.md:113`; `docs/adr/0018-click-retention-daily-purge.md:126`; `docs/guidance/architecture.md:130` at `5f90090` | The retention amendment/register say neither executor interrupts JDBC. Existing `ClickRecorder.java:121` calls `shutdownNow()` after its deadline; [fresh compiled bytecode](proof/writer-close-bytecode.txt) contains that invocation, and its latch test explicitly ignores interrupts at `ClickRecorderTest:263–270`. ADR-0011's earlier shutdown paragraph already describes this correctly. Correct the shared prose: purge does not interrupt; writer attempts interruption, claims/reports remaining clicks, and a write may still commit. No behavior change requested. Fix in passing through the design owner. |
| Existing ping backlog | LOW / CONTEXT-GAP | `src/main/java/dev/urlshort/ping/PingController.java:30` | **Agree with deferral.** Ping predates the application Clock. Its unit and functional tests bracket the result with real `Instant.now()`; the latter is not merely an ISO-format assertion. No stored business rule depends on ping. On the next ping touch, inject the shared Clock and reconcile AC-2/test expectations. Lead record 21:33/21:36Z, `qitem-20261003213157-c0a336f9`. |
| Existing download backlog | MEDIUM / JUDGMENT-GAP | `gradle/wrapper/gradle-wrapper.properties:4–6`; `Dockerfile` distribution-build step | **Agree with severity and scheduled follow-up.** Timeout 10000 ms, retries 0; lead's 23:06Z record names hosted 503 / later image-build download timeout and manual reruns. This review observed configuration, not another network incident. Add bounded retries/longer timeout and verify both PR and CD paths after missions 02/03, sooner on a third blocker. Retries mitigate transient failures; they cannot guarantee availability. Reusing the proved jar is a later option, not required redesign here. |
| Existing V3 test coupling | MEDIUM / JUDGMENT-GAP; already owned | `src/test/java/dev/urlshort/click/ClickAuditColumnsTest.java:50`; `src/functionalTest/java/dev/urlshort/click/ClickRetentionStartupJourneyTest.java:118–122` | The first migrates to latest while comparing all non-click columns; the second requires exactly V1–V3 after a latest application start. Both pass at this head. Lead's 23:12Z record reports V4 exposing both assumptions and grants target-3 pins without weakening assertions. I checked the source mismatch, not a new V4 failure run. Keep this correction in `04-audit-columns`, plus its independent latest-version upgrade tests; verify the final delta. |
| Documentation reconciliation | LOW / CONTEXT-GAP | `README.md:19–21`; `docs/DESIGN.md:107–152`; `docs/qa/GAPS.md:82–85` | Add the purge-hold environment variable; mark merged implementations and closed V3/custody conditions accurately. Continue the existing shared-document reconciliation item before export. Do not rewrite historical failed observations. |

There is no new MUST-FIX/HIGH. W2P-01 was corrected in passing; the ping
and download items remain backlog with the above triggers. V4 test corrections
and living-document updates have named existing delivery paths. The final
workflow review, not this draft, records the ledger row and decides whether
its expanded range is ready.

### Focused re-review — `2d3de57` and `bfc642d`

**W2P-01: fixed.** The design author acknowledged the contradiction and corrected
the register and DESIGN row at `2d3de57067a2a438f1869835bcec76bf93342fa3`, then
the accepted ADR-0011 amendment and ADR-0018 consequence at `bfc642d` with the
lead's authorization. I read both complete correction diffs: all four now
distinguish the purge's non-interrupting wait from the writer's deadline-triggered
interrupt and possible later commit. This matches the pinned source, compiled
invocation and existing shutdown tests. No product or test change, so the fresh
437-test gate remains applicable; no additional build was needed. The initial
finding is retained above as the issue-to-resolution record. No settled finding
was reopened and no additional low-severity re-review finding was added.

## Wrapper and release tooling

`f3e6b0b` adopts Gradle **9.8.0** from the human-merged update. The wrapper jar
SHA-256 is `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`;
it equals the wrapper resource inside the cached 9.8.0 distribution. This is
local byte/provenance consistency, not a new online authenticity check. The
fresh offline gate ran 9.8.0 successfully. `gradlew.bat`'s upstream safety/exit
changes were read; subsequent `9bbf6e5` is normalization only (`--ignore-cr-at-eol`
diff empty). No Windows execution claim. Unix gradlew/build scripts are unchanged.

The smoke additions retain loopback-only application starts, the public journey,
safe one-row-per-attempt restart classification (including `cut<status>`), and
separate host-path loss disclosure. `tools/bench.mjs` is open-loop and measures
from scheduled due time. Its sequential GET/HEAD distributions do **not** prove
an isolated added-cost quantile; the corrected mission-01 GAPS qualification
still applies. No new container, host restart, capacity, advisory-database or
release smoke result is claimed by this pre-review.

## Product/tool file ledger

The accumulated diff contains 4,747 paths, chiefly evidence and governance
documents. The following 43 are the complete product, workflow, wrapper,
installed-tool and API-document subset (**3,657 additions / 86 deletions**).
All their changes were read; audit/dogfood full-file context is retained from
the prior independent reviews and checked for byte equality above. This is
not a claim of line-reading every generated coverage page or historical capture.
The two seat-permission diffs and `.gitignore` were also inspected as operational
metadata; rig orchestration edits are outside this product pre-review.

| File | Verdict |
|---|---|
| `.github/dependabot.yml` | PASS — two weekly ecosystems. |
| `.github/workflows/cd.yml` | PASS — bounded local package/smoke, no publish. |
| `.github/workflows/ci.yml` | PASS — same gate, durable failure reports. |
| `README.md` | PASS behavior; LOW documentation carry above. |
| `docs/api/openapi.json` | PASS — audit and corrected schema; live equality. |
| `gradle/wrapper/gradle-wrapper.jar` | PASS — binary compared with cached distribution. |
| `gradle/wrapper/gradle-wrapper.properties` | PASS update; MEDIUM resilience backlog. |
| `gradlew.bat` | PASS inspected upstream/normalization diff; not executed. |
| `scripts/smoke.sh` | PASS inspected public/installed/restart/bench changes. |
| `tools/bench.mjs` | PASS inspected; truncated-body control fails safely. |
| `src/main/java/dev/urlshort/audit/AuditController.java` | PASS — guarded peer/settings and paging validation. |
| `src/main/java/dev/urlshort/audit/AuditEntry.java` | PASS — explicit stored public fields. |
| `src/main/java/dev/urlshort/audit/AuditPage.java` | PASS — page/cursor contract. |
| `src/main/java/dev/urlshort/audit/AuditTrail.java` | PASS — bounded parameterized keyset read. |
| `src/main/java/dev/urlshort/audit/package-info.java` | PASS — read/write boundary. |
| `src/main/java/dev/urlshort/click/ClickPurge.java` | PASS — owned serial job, safe cutoff, bounded close. |
| `src/main/java/dev/urlshort/click/ClickRetentionProperties.java` | PASS — defaults and startup validation. |
| `src/main/java/dev/urlshort/click/ClickStore.java` | PASS — bound delete, existing readers/writer preserved. |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS — distinct reduction reason; existing shutdown semantics. |
| `src/main/java/dev/urlshort/click/package-info.java` | PASS — feature boundary. |
| `src/main/java/dev/urlshort/web/MetricsConfig.java` | PASS — native tag filter. |
| `src/main/java/dev/urlshort/web/OpenApiConfig.java` | PASS — one schema correction. |
| `src/main/resources/application.properties` | PASS — merged guard and purge defaults. |
| `src/main/resources/db/migration/V3__add_click_audit_columns.sql` | PASS — expand/backfill/defaults and rollback read. |
| `src/test/java/dev/urlshort/audit/AuditControllerTest.java` | PASS — access, header and paging branches. |
| `src/test/java/dev/urlshort/click/ClickAuditColumnsTest.java` | PASS at V3; known version-pin correction due with V4. |
| `src/test/java/dev/urlshort/click/ClickPurgeTest.java` | PASS — clock, failure, cutoff, hold and ownership. |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — exact reduction reason assertion. |
| `src/test/java/dev/urlshort/web/MetricsConfigTest.java` | PASS — tag/value preservation. |
| `src/functionalTest/java/dev/urlshort/audit/AuditAccessSettingsJourneyTest.java` | PASS — rewriting settings refuse reads. |
| `src/functionalTest/java/dev/urlshort/audit/AuditForwardedHeadersJourneyTest.java` | PASS — real-container/header controls. |
| `src/functionalTest/java/dev/urlshort/audit/AuditReadFailureJourneyTest.java` | PASS — safe 500/log failure. |
| `src/functionalTest/java/dev/urlshort/audit/AuditReadJourneyTest.java` | PASS — complete public paging and access journeys. |
| `src/functionalTest/java/dev/urlshort/audit/AuditUpgradeJourneyTest.java` | PASS — legacy directory compatibility. |
| `src/functionalTest/java/dev/urlshort/click/ClickPurgeFailureJourneyTest.java` | PASS — purge/reduction fail-open effects and logs. |
| `src/functionalTest/java/dev/urlshort/click/ClickPurgeHoldJourneyTest.java` | PASS — shared context stays quiet across scheduled time. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionJourneyTest.java` | PASS — boundaries, statistics, preservation and concurrent redirect. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java` | PASS — autonomous daily run on shifted clock. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionSettingJourneyTest.java` | PASS — seven-day setting and event. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionStartupJourneyTest.java` | PASS at V3; known version-pin correction due with V4. |
| `src/functionalTest/java/dev/urlshort/web/HealthMetricsJourneyTest.java` | PASS — both anonymous metric surfaces path-free. |
| `src/functionalTest/java/dev/urlshort/web/OpenApiDocumentTest.java` | PASS — live equality and representative error shapes. |
| `src/functionalTest/resources/application-functional.properties` | PASS — authorized purge hold, no log suppression. |

## Ponytail review

Lean already. Ship. No new unnecessary dependency, generic layer or hand-built
platform replacement identified. The two executor owners have distinct lifecycles;
one shared scheduler would add coupling. The documented dataset and multiple-disk
ceilings are accepted intent, not reasons to build ahead of the contract.

## Continuation and self-check

Exact head and clean worktree verified; fresh canonical-only gate; all 43
product/tool changes accounted for; rebase independently reconciled; all 13
current register rows walked; claims distinguish current source, prior captures,
operator reports and unmerged work. W2P-01 has source and compiled-command evidence.
No ledger row per the packet's explicit instruction.

At the real wave step, review `ed2b940..final-main` including V4, its version-pinned
V3 tests and latest upgrade tests; reconcile living documentation and retain
the verified W2P-01 correction;
run the final merged gate; then record both review vantages and the ledger.
This draft does not pre-accept that delta.
