> Current lifecycle verdict: **WAITING for the D21 sixth-slice merge. W2F-01 is fixed on 50ad9c3**, with a fresh independent full gate. See the re-review below. The original five-slice review and its failed gate remain unchanged as history; this is not yet a release handoff.

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


# Final wave review — `8e9c065..d55a502`

**Verdict: NOT READY — one HIGH, W2F-01; workflow waits for a test-only forward fix.**
Reviewer `review-agent@urlshort-factory` (Codex), 2026-10-04 UTC.
Packet `qitem-20261004002455-b7ea811b`, instance `01M40SN34E37K96B38JPG9K41X`.

This section adopts the preceding independently completed pre-review at `82340b8`, including W2P-01's resolution, and extends it through the fifth mission-02 product merge **d55a502**. Mission 03's analytics merge `94aa2c0` is outside this pinned product range and has its own wave review. No settled finding is reopened. After this review began, human decision D21 added `06-client-identity` as w3 (`5cfdf8a`, instance `01M425CY9Z4K03G14Y677AT0PA`). The lead explicitly requires this packet to retain its five-slice review, then add only that slice's merge delta and the W2F-01 correction before release handoff. This record does not pre-accept the sixth slice.

## Final scope and source claims

At this pinned range the five approved outcomes are local audit read, scheduled click retention, observed dogfood fixes, complete row-audit columns, and CI/CD, with existing links unchanged. The two earlier human-approved scope amendments account for V4 and CI/CD; D21 adds a behavior-preserving shared client-identity component to the remaining mission scope. Read the full mission brief, V4 SPEC/design, ten-file delta, relevant full files, final register and prior independent code/security/QA records.

[Custody inventory](proof/final-wave-custody-d55a502.json) records **51 accumulated product/tool paths**: the prior 43 plus eight new paths; two earlier V3 tests change again. All ten delta paths are byte-identical to independently reviewed candidate `305f804`. The complete range includes 7,635 paths, mostly generated evidence and governance documents; the coverage claim is the complete product/tool subset and relevant contract/evidence changes, not line-reading every generated HTML page. [Range log](proof/final-wave-range-log.txt), [complete source delta](proof/final-wave-delta-d55a502.diff).

| Added/changed since the pre-review | Verdict |
|---|---|
| `src/main/java/dev/urlshort/link/LinkRepository.java` | PASS — bound stamp method; retire updates its timestamp in the same conditional write. |
| `src/main/java/dev/urlshort/link/LinkService.java` | PASS — create/key-release stamps use the same service-clock instant, inside the existing transaction; no-op paths return before mutation. |
| `src/main/resources/db/migration/V4__add_link_audit_columns.sql` | PASS — seven additive columns, documented defaults/backfill and eight-statement rollback; exact reviewed design. |
| `src/test/java/dev/urlshort/link/LinkAuditColumnsTest.java` | PASS — schema/constraint preservation and literal header rollback/reapply executed in this fresh unit suite. |
| `src/test/java/dev/urlshort/link/LinkServiceStampTest.java` | PASS — statement sequencing and common clock instant, complemented by stored-row functional checks. |
| `src/functionalTest/java/dev/urlshort/link/LinkAuditColumnsJourneyTest.java` | PASS — new stamps, no-op snapshots, invisible columns, static actors and append-only audit rows. |
| `src/functionalTest/java/dev/urlshort/link/LinkAuditColumnsFailureJourneyTest.java` | PASS — failing audit append rolls back the retirement and update stamp together. |
| `src/functionalTest/java/dev/urlshort/link/LinkUpgradeJourneyTest.java` | PASS — actual V4 startup preserves old fields, backfills active/retired/released rows and caps future event times. |
| `src/test/java/dev/urlshort/click/ClickAuditColumnsTest.java` | PASS — authorized target-3 pin; prior assertions retained. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionStartupJourneyTest.java` | PASS — authorized target-3 startup; prior assertions retained, latest-schema coverage supplied by new V4 journeys. |

V4 AC-1–11 survive integration: the new migration/stamp tests all pass in the fresh run. `Link`, public response records, named audit SELECT and OpenAPI are unchanged, so no new column leaks into the API. The application adds no audit UPDATE/DELETE path; the migration's one-time backfill is explicit. Default columns keep legacy inserts valid. Rollback was read and run by the fresh unit test, with restored schema/values and successful reapplication. Installed-directory and stopped-copy rollback evidence remain the attributed QA observations, not a second installed run by this reviewer.

The prior V3 test-coupling item is fixed by `132a884`'s two narrow pins, not weakened assertions. GAPS closure `17593aa` closes all four audited-table debts; `382a7b2` corrects the retention judge to QA2. These are explicit documentary follow-through after the pinned merge. [Live V4 proof](proof/final-wave-v4-proof.json) is **9/9 accepted**, with all **28 evidence references** matching their hashes. This does not supersede the newly observed integrated-suite race.

The four drill records are present in `docs/scenarios/drills.md`; the QA-rejection/remediation fallback is labelled and its recorded completion was added at `315da52`. This wave review does not re-execute rig-stop, abort or container-revert drills or promote their historical results to a new release smoke claim.

## Fresh final gate and reproduced finding

I ran `scripts/gw --offline check --rerun-tasks` with wrapper `--log` in a new clean detached worktree `.worktrees/review-wave02-d55a502` at exact d55a502. **The gate failed:** 218 unit passed, 233 functional ran with one failure. [Gate log](proof/final-wave-check-d55a502.txt), [counts](proof/final-wave-first-run-counts.json), [original failing XML](proof/final-wave-first-failure-d55a502.xml). Coverage/Javadoc tasks were not reached; no fresh final-wave 100% claim is made. Prior candidate/pre-review green coverage remains historical evidence only.

`ClickRetentionScheduleJourneyTest` successfully observed the intended two retained rows, then failed line 96: expected one purge event, observed zero. The completed XML contains the later correct event (`deleted=2`, cutoff `2026-07-07`). Source ordering is `deleteBefore` followed by the log call; the test waits only for rows, so its immediate output snapshot can precede publication.

An unchanged isolated rerun passes ([log](proof/final-wave-schedule-recheck-d55a502.txt), [XML](proof/final-wave-schedule-recheck-d55a502.xml)); this does not resolve the first failure. A deterministic review-only control runs the actual `ClickPurge` and `ClickStore` against H2, placing a latch at log publication: it observes the committed deletion with zero published events, releases the latch, then observes one event. [Probe](proof/PurgePublicationProbe.java), [Gradle init](proof/purge-probe.init.gradle), [control output](proof/purge-publication-control.txt). It deliberately widens a legal scheduling gap; it neither edits the product/test nor claims a naturally delayed production logger.

| ID | Severity / classification | File:line | Evidence / required change |
|---|---|---|---|
| W2F-01 | **HIGH / JUDGMENT-GAP** | `src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java:87` and `:96`; producer ordering `src/main/java/dev/urlshort/click/ClickPurge.java:99` | Actual mandatory gate failure plus controlled ordering reproduction above. Wait within the existing bound for both the expected row state and log publication, retaining exact-one-event/cutoff assertions. No arbitrary sleep, logging suppression, weakened assertion or product change. Route a test-only forward fix, independent QA/review, then a fresh complete merged gate. |

Severity reflects intermittent false failures of main/CI, not a claim that purge data or logging is wrong. This test and the producer are unchanged by V4; it is an inherited defect exposed at the accumulated boundary. The SPEC already requires both autonomous deletion and its event, so this is a builder/test judgment gap, not missing product context.

## D20 register at d55a502 — all 13 concerns

| Concern | Final consistency judgment |
|---|---|
| Client identity / proxy trust | Consistent with prior qualified verdict: audit guard and NONE pin unchanged; peer-versus-limiter click identity remains carried W2-02 at this pinned head, assigned to the separate analytics mission. |
| Time | Link writes reuse the application Clock and one create instant; audit row-write defaults use DB time under ADR-0020. Ping Instant.now remains existing LOW backlog. |
| Schema change | V1–V4 in order; additive V4 and literal rollback proven by this run. V3-specific tests now pin V3. Register's pre-merge V4 wording is a documentation status refresh, not missing DDL. |
| Audit columns | All four domain tables now satisfy the column policy. Link create/key-release use a targeted stamp inside the same transaction, as the explicit ADR-0020 amendment specifies; retire uses one conditional statement. GAPS closure verified separately. |
| Error shape | Existing shared ProblemDetail unchanged; failing audit still rolls back and returns safe 500. No new format or handler. |
| Request ID / logging | Request/event format unchanged. **W2F-01** concerns a test snapshot before background event publication; the eventual correct event is present, with no request id invented for the job. |
| Audit trail writes | Existing AuditLog remains the only application writer in link transactions; named reader prevents V4 exposure. Key-release housekeeping retains its explicit no-audit-event contract. |
| Client hashing | DailySalt/storage reduction unchanged at this mission head; analytics changes are outside this range. |
| Metrics / health | Existing bounded metrics/pathless gauges unchanged. Startup migration occurs before readiness; no new health exposure. |
| API document | Byte-unchanged from pre-review; no V4 field added. Existing generated equality test passes within the otherwise failing functional suite. |
| CI/CD | Same gate, bounded workflows and no publishing. **W2F-01 blocks a clean final verdict because this test can make that gate falsely red.** Existing download retry backlog retained. |
| Background work | The verified W2P-01 correction remains: purge waits without interrupting its DELETE; writer interrupts after its drain and reports uncertain in-flight outcomes. No executor change in V4. |
| Operator settings | No new setting. Existing README purge-hold omission remains assigned documentation cleanup; defaults/validation unchanged. |

## Remaining dispositions and continuation

No new complexity finding. The targeted stamp preserves the shipped record/API and stays transactional, as already weighed by the design; no new dependency/layer. Preserve the prior ping LOW / CONTEXT-GAP and wrapper-download MEDIUM / JUDGMENT-GAP backlog, with their existing follow-up triggers. Keep the README/status reconciliation on its existing release/export path. W2P-01 stays fixed.

Second vantage is committed at **cda00ef**, closing `qitem-20261004002621-3d2dc808`: [design-agent's report](wave-review-design-agent.md). Its author relationship is explicit: the V4 designer supplies a consistency check, while the independent source/gate judgment is this review and prior QA/review2. All 13 register rows are walked; documentary refreshes are `ea84e77` and `fe9529a`. Its W2F-02 is **this report's W2F-01**, one shared HIGH with one forward-fix route. Its distinct W2F-01 is a LOW future V5 test-pinning risk, carried to the lead's backlog with the trigger that the slice adding V5 pins V4-specific tests; the designer explicitly labels that risk reasoned rather than reproduced under a V5. No present product failure is asserted for that LOW.

New rework is routed to the lead as **qitem-20261004003207-25b6f4c0** ([brief](proof/w2f-01-rework.md)), already delegated to a builder. The mission lifecycle has no review back-edge: this packet exits **waiting** on that item. The D21 sixth-slice dependency remains outstanding even if W2F-01 is resolved first.

On continuation: read the lead's exact forward-fix SHA and independent receipts, verify W2F-01 and any fix-introduced changes, and append the resolution. Wait for the lead's `06-client-identity` merge notice, review that added range with the required structural vantage, and run/reconcile the full merged gate at the named final boundary before handoff. Keep the original five-slice range and later deltas explicit; do not silently substitute analytics into this mission's original product range.

Self-check: original range and final merge pinned; prior 43-path review adopted and ten-file delta fully inspected (51 distinct product/tool files); real failed gate retained; ordering defect empirically reproduced; no source/test/SPEC/design edits; V4 receipts and documentary closure checked; new HIGH has file/line/evidence/repair and a durable routed owner. Final-wave row recorded in REVIEW-LEDGER; not a release approval.

## Re-review 50ad9c3 — W2F-01 fixed; D21 still outstanding

2026-10-04 UTC, review-agent (Codex). Returned under the same wave packet after the lead closed `qitem-20261004003207-25b6f4c0`. Exact merged head **50ad9c3ab9e65baa4100ede1772b514322957fa5**, candidate **0552b815e39b9899fa4fdaacf3ea3454e116720c**. I read the full changed class and complete first-parent merge diff, then ran the gate in clean detached worktree `.worktrees/review-w2f01-50ad9c3` at that exact merge.

Context/confidence: high. The repair synchronizes a test with two already-required effects: committed rows and the subsequent log event. Production purge behavior, deadline and exact terminal assertions remain the contract. The original finding is preserved; its unchanged green rerun was never the resolution.

| File — complete correction delta | Verdict |
|---|---|
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionScheduleJourneyTest.java` (+11/-4) | **PASS** — lines 90–93 continue polling while old rows remain **or** the post-window event is absent. The 60 s bound and existing 100 ms polling sleep remain. Lines 95–97 retain exact rows, `singleElement` and exact cutoff; lines 101–105 extract the same JSON filter for both observations. No product/build file changed. |

| Finding | Author response | Independent disposition |
|---|---|---|
| W2F-01 HIGH / JUDGMENT-GAP (design vantage calls it W2F-02) | Fixed by `0552b81`, merged `50ad9c3`; no product change | **FIXED.** In the reproduced state (DELETE visible, log not yet published), `runs(...).isEmpty()` now keeps the test in its bounded poll. An absent event still fails after timeout, and duplicates/wrong cutoff/wrong rows still fail the unchanged terminal assertions. No new finding introduced. |

**Fresh verification:** `scripts/gw --offline check --rerun-tasks` with wrapper `--log` completed successfully, all 14 tasks executed: **226 unit + 250 functional**, zero failures/errors/skips, **582/582 lines and 206/206 branches**, Javadoc passed. Only canonical `test.exec` and `functionalTest.exec` existed. [Full log](proof/w2f01-recheck-50ad9c3.txt), [actual scheduled-test XML](proof/w2f01-recheck-50ad9c3.xml), [merged CSV](proof/w2f01-recheck-50ad9c3.csv). The XML records the startup event followed by the daily event with `deleted=2`, `cutoff=2026-07-07`; the scheduled test's retained exact assertions pass.

**QA/custody audit:** all 109 hashes in QA2's `f4e08b3` manifest match. I parsed all ten targeted XMLs: each contains exactly the intended passing test and the expected startup/daily purge events. QA's merged coverage agrees with my independently recomputed totals. The merged file is byte-identical to candidate `0552b81`, and `src`, scripts, Gradle/build files and workflows have no differences from that QA candidate. [Reconciliation](proof/w2f01-reconciliation-50ad9c3.json) and [reproducible verifier](proof/w2f01-verify.py). The lead's separate merged-gate log is `docs/evidence/02-brownfield/integrate-w2f-01-check-0552b81.txt`.

This merged full gate includes the already independently reviewed analytics merge `94aa2c0`; it is not a relabelling of the original mission-02 range. The reviewed correction is exactly `50ad9c3^1..50ad9c3` (one test file). Original range `8e9c065..d55a502` and its 51-file coverage remain as recorded. No forced logger barrier was injected into the fixed test; the closed ordering gap follows from the inspected condition, with actual scheduled execution confirmed by this run and QA's ten repeats.

### Ponytail review

No finding. One existing event filter is shared by the poll and assertion; no dependency, layer or generic waiting abstraction added. Security surface is unchanged because the entire correction is test-only.

**Verdict for W2F-01: PASS / fixed**, with no remaining MUST-FIX/HIGH in the already reviewed five-slice outcome plus this correction. Existing non-blocking backlog stays on its recorded triggers, including V4 test pinning when V5 is introduced. Prior structural vantage `cda00ef` already described this exact required repair; its W2F-02 resolves with this finding.

**Lifecycle continuation:** remain **waiting** on lead-owned **qitem-20261004005331-36607695**, the canonical merge notification for `06-client-identity` (D21). [Continuation brief](proof/d21-wave-continuation.md). At its closure, inspect the exact accepted candidate, merge range and gate; add that range and its structural/register review before release_prep. Do not reopen settled findings or infer sixth-slice acceptance from this test repair.

Self-check: exact SHA and clean worktree verified before/after; one changed file fully read and accounted for; own complete gate and upstream hashes checked; original red evidence retained; resolution and ledger row recorded; only review evidence edited. No release approval or publication.

## Added D21 range — PASS at b8d7fc16 / fda42757

2026-10-04 UTC, review-agent (Codex), same wave packet `qitem-20261004002455-b7ea811b`.
The lead's merge notification `qitem-20261004005331-36607695` is closed. The actual two-parent
merge is **b8d7fc165ad079eca1b321eb44015264f18493ec**, second parent the accepted candidate
**e40b09541feb0b7555c475baa82587fdd09e4890**. Returned review head
**fda427573dfeea7e27602e99b094e2bd6363c2d3** is the next documentation commit; the lead corrected
the merge label in NOTES/PROGRESS. I verified all three have identical source, tests, build,
scripts, workflows, container and API trees. My clean detached worktree is
`.worktrees/review-wave02-fda42757`.

**Context/confidence: high.** D21 asks for one authority for client identity while preserving
two distinct answers: the visitor charged/hashed, and the direct peer admitted to the audit
trail. This addition completes that outcome without changing the shortener's public contract.
I read the SPEC, locked design and impact analysis, both ADR amendments, all ten changed
source/config/test files in full, and the intervening README change. Independence: dev2/design2
authored this refactor; I did not. Prior five-slice review, analytics review and settled W2F-01
correction remain attributed to their original ranges. The added range is **50ad9c3..fda42757**.

### Complete file ledger for the added product range

| File | Verdict |
|---|---|
| `src/main/java/dev/urlshort/web/ClientIdentity.java` | PASS — stateless owner of the existing trust scan, request attribute and two audit predicates; no new parser or trust policy. |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java` | PASS — resolves after exemption and before charging; the same request object and peer reach downstream consumers. Filter returns to package-private. |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS — the same attribute-or-peer ternary moves behind `of`; hash timing, reduction, writer, counters and loss reporting are unchanged. |
| `src/main/java/dev/urlshort/audit/AuditController.java` | PASS — configuration guard still computed once; per-request direct-peer guard precedes validation/negotiation. It never uses the visitor attribute or trusted list. |
| `src/main/resources/application.properties` | PASS — one granted comment names trusted proxies' existing analytics effect and absence of audit authority; no setting/value change. |
| `src/test/java/dev/urlshort/web/ClientIdentityTest.java` | PASS — exact-text trust, opaque tokens, blank hops, loopback forms and forwarding-header presence preserve prior answers. |
| `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | PASS — references change; existing expected values, budget/429 and unchanged-peer assertions remain. |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — reference change plus absent/non-string attribute cases; existing loss, shutdown, privacy and metric assertions remain. |
| `src/test/java/dev/urlshort/audit/AuditControllerTest.java` | PASS — predicate reference change plus both-header/whitespace settings; existing admission/refusal and parameter assertions remain. |
| `src/functionalTest/java/dev/urlshort/web/ClientIdentityCharacterizationJourneyTest.java` | PASS — all new matrix/budget/audit/settings contexts inspected. Corrected helper waits on stored rows then performs one checked HTTP read; full date/3-click/2-visitor/privacy oracle remains. Real-server contexts exercise both remote-IP triggers. |
| `README.md` | PASS — intervening mission-03 documentation `e227acf0`, separately identified: loopback command, retained statistics, salt restart behavior, proxy rules, hold setting and counters agree with integrated source. Closes the structural vantage's missing purge-hold documentation. |

The verification script enumerates the ten source/config/test paths; README is the eleventh,
documentary path above. This is a delta review, not a claim to reread every historical evidence
export committed during the interval. Addressed documentary inputs also include the manifest,
SPEC/design/impact/proof, QA summary/traceability/gaps, ADR-0015/0019, current DESIGN/register,
the merge gate and the two attributed slice review reports.

### Preservation and empirical evidence

**Own fresh gate:** `scripts/gw --offline check --rerun-tasks`, with wrapper `--log`, succeeded
with all 14 tasks executed: **268 unit + 322 functional**, zero failures/errors/skips; Javadoc
and coverage verification passed. Only canonical `test.exec` and `functionalTest.exec` exist.
Merged CSV totals are **584/584 lines, 206/206 branches**; JaCoCo's source-deduplicated BUNDLE
is **583/583 lines, 206/206 branches**, both 100%. This is the same class-versus-source line
count distinction as earlier reviews, not a coverage gap. Generated OpenAPI is byte-identical
to the committed document. [Gate](proof/d21-check-fda42757.txt),
[CSV](proof/d21-coverage-fda42757.csv), [reconciliation](proof/d21-reconciliation-fda42757.json),
[reproducible passive verifier](proof/d21-verify.py).

All **43 pre-existing functional files** are byte-unchanged from `50ad9c3`; the new suite adds
72 invocations. Source chronology separates characterization (`240b230`, completed `1b4e0a7`)
from the production move (`7e232599`), reference cleanup (`d0e74c43`), comment (`fb63a88a`) and
test-only repair (`e40b0954`). The characterized `1b4e0a7` production tree equals `50ad9c3`.
ADR amendments `57cb9aee` precede the production move by recorded commit time. QA independently
ran the baseline characterization and replayed the corrected 72 invocations on original
production; I checked that evidence and tree relationship, rather than claiming another
baseline application run.

My passive audit verifies **672 current QA hashes, 378 coverage-resource hashes and 3490
historical hashes**, with the declared archive aliases. I independently parsed **159 saved
original-baseline/current-candidate response pairs**, reconstructed their request-ID joins
from full JSON logs, and compared all response fields and complete correlated events.
Only response-derived generated UUID/code associations, HTTP Date, log timestamp and PID were
substituted. All pairs agree. All twelve matrix statistics have exactly one fixed UTC day,
3 clicks, 2 unique visitors and 0 bots; shared budgets give 429/Retry-After 30 while unrelated
clients succeed. This uses raw evidence, not just QA's comparison result.

AC-1/2/11/13 preservation is supported by unchanged production outside the moved rules, the
fresh unchanged journeys and QA's original before/after effects. AC-3/4/5 are exercised by the
fresh budget/grouping contexts and the independently reconciled matrix. AC-6–10 include the
direct-peer/header cases and real Tomcat rewriting settings in this run. AC-12/14 retain
privacy/correlation assertions, the raw matrix joins and QA's broader recorded canary effects;
AC-15 has the independently checked identical generated document. Full live-jar, copied-H2 and
all original canary observations remain **attributed QA evidence**, not new installed runs
by me. QA's simulated-peer fixture, telemetry-comparison qualifications and class-level
parameterized attributions remain explicit in its README/GAPS; no network-topology, shutdown
or performance guarantee is added.

Review2's **CR-01 is fixed**, as independently re-reviewed at `981eb8e0`: its delayed-writer
controls invoke the actual helper, wait about three seconds and observe one HTTP statistics
read without 429/NPE. I read the corrected helper and retained full oracle, verified candidate
equality and reran the full integrated gate. This does not reopen the settled failure or claim
to have rerun its artificial latch control. Our earlier retention **W2F-01 stays fixed**.

### D20 register — all 13 concerns at the final boundary

| Concern | Judgment |
|---|---|
| Client identity / proxy trust | Consistent: sole rule/attribute owner is `ClientIdentity`; separate direct-peer predicates never consult visitor identity. |
| Time | Consistent with carried ping `Instant.now()` LOW backlog; refactor adds no clock. |
| Schema change | Unchanged V1–V4; future V5 owner must pin V4-specific tests (existing structural LOW). |
| Audit columns | Unchanged; all four tables retain the resolved V3/V4 policy. |
| Error shape | Same MVC advice and limiter 429; audit refusal remains before parameter/content checks. |
| Request ID / logging | No new event, field or logger; server-issued correlation and privacy survive the move. |
| Audit trail writes | Unchanged insert-only application path in link transactions; read remains separately guarded. |
| Client hashing | Same daily salt and hash input via `ClientIdentity.of`; storage/query/salt lifetime unchanged. |
| Metrics / health | Same bounded tags/counters/exposure; no identity telemetry added. |
| API document | No diff; fresh generator result equals committed bytes. |
| CI/CD | No workflow/build change; complete merged gate green. Existing download-retry MEDIUM remains backlog. |
| Background work | Writer/purge ownership, bounds and interrupt distinction unchanged; prior wording correction stands. |
| Operator settings | No new setting; granted proxy comment and README now explain existing effects/hold variable. |

I read post-merge register/DESIGN update **3b2ecd0b** and ADR status update **8e55ddcc** against
the source. They name the actual merge and three callers, preserve the two predicates and
remove stale designed/status labels. Structural vantage **fea4749b** closes
`qitem-20261004033531-e96f5a30`: [design-agent report](wave-review-design-agent.md). The designer
of this refactor was design2; the structural reviewer discloses authorship of historical
code being moved and of register upkeep. This independent source/gate review also checks
that upkeep. Both required vantages are now recorded.

### Findings, disposition and Ponytail review

No new MUST-FIX/HIGH/MEDIUM/LOW. No complexity finding: one static authority replaces
distributed rules without a bean, interface, dependency or duplicate implementation; the
Boot-upgrade `ponytail:` ceiling is retained. Existing ping LOW, future-V5 pin LOW and Gradle
download-retry MEDIUM stay with the lead's recorded backlog, not fixes demanded by this delta.

I adopt the structural report's **W2D21-01 INFO / CONTEXT-GAP**: `slice.yaml:73` still lists
`application.properties` as excluded below its explicit comment-only grant at lines 64–71.
The authorized one-comment change is unambiguous; correct the stale exclusion when next
editing the manifest. **W2D21-02 INFO** records the intervening README change and closes that
vantage's W2P-01 documentation item; it is not a new product defect. The initial merge-SHA
mislabel was corrected in passing by the lead and is recorded above, with product equality
verified. No forward-fix slice is needed for these informational records.

**Final wave verdict: PASS; handoff to release_prep.** Six mission-02 slices are merged,
both vantages are complete and no MUST-FIX/HIGH remains. The observed
[proof snapshot](proof/d21-proof-snapshot.json) has 16/17 accepted for 06-client-identity;
only item 16 awaits the attributed post-merge register/independent-review judgment under
the lead's existing downstream custody. It must be completed for release readiness; this
wave verdict does not impersonate QA or claim a ship approval. Release preparation may
continue while that documentary judgment returns. Boot-trigger maintenance, headerless
local-relay boundary, unchanged offline-advisory and operational qualifications remain.

Self-check: exact returned head/actual merge/QA candidate reconciled; all 11 added-range
product/document paths read; own complete gate, coverage and raw evidence checked; all
13 concerns recorded and both vantages linked; settled findings preserved; ledger row
appended; only review artifacts edited. No product, test, SPEC or design edits, publishing
or release approval.
