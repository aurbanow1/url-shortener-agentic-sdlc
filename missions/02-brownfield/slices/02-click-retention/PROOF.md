# PROOF — OPR.99.0.3.2 Click Retention

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.2 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

Closed by: <seat>   Date: <date>   Verdict: <pass | pass-with-residue | ...>

## What this proves

<1-3 sentences: the claim the slice made, now demonstrated>

## Artifacts (media in proof/)

Dropped via `rig proof add … --evidences … --media …` (one drop per verdict; media attached, never only hand-listed):

- proof/screenshot-01.png — <what it shows>
- proof/capture-behavior.gif — <what it shows>
- proof/command-output.txt — <what it proves>

## Residue / caveats (if any)

<documented residue: what's not covered + where it's tracked>

## Builder (dev2-agent@urlshort-factory)

**Candidate `a8fc8b6`** (handed off 2026-10-03 21:4xZ). Build-plan steps 1–5 and the V3 pair are on
`slice/02-click-retention`. **Custody changed at 21:42Z** (mission 02 NOTES §2, the lead, at the
human's speed-up request): step 5 lands now and is judged with the rest. The base is `main` **before**
`01-audit-read`'s merge, at `16312da` via `f6dd29e`. After that merge the lead sends a separate item.
I then rebase onto the merge, resolving `application.properties` and `README.md` in custody order
(audit-read's lines first), which gives X′. The integrate rule of §2 21:42Z then applies: range-diff,
`check --rerun-tasks` on X′, and QA re-judges proof item 9.

- **`a8fc8b6` `feat(02-click-retention): shipped retention setting`.** It adds, after the rate-limit
  block in `application.properties`, the two settings with design §1's comments:
  `urlshort.click.retention-days=90` and `urlshort.click.purge-enabled=true`. It also adds
  `URLSHORT_CLICK_RETENTIONDAYS` to the README's operator-settings sentence. Nothing else changes.
  The record's defaults equal the file's values, so behaviour is unchanged.
- **Gate on `a8fc8b6`:** `scripts/gw --offline -p .worktrees/02-click-retention check --rerun-tasks`,
  log at [`proof/builder-check-step5.txt`](proof/builder-check-step5.txt). BUILD SUCCESSFUL. Unit 174,
  functional 172, 0 failures. Merged lines 490/490, branches 168/168 (100 %). Javadoc green.
- **By effect on `a8fc8b6`:** the jar on `--server.address=127.0.0.1`, fresh directory. Readiness `UP`;
  the startup run logged `{"message":"clicks purged","deleted":0,"cutoff":"2026-07-05","retentionDays":90}`.
  So the shipped file binds, and the upgrade capture below (taken on `056c8db`, the same code without
  the two lines) stands for the purge.
- **Proof item 9 (ancestry from `01-audit-read`'s merge, V3 the next number) is deferred** to X′ by
  the lead's rule. V3 is still the next free number on `main` today: `main` has only V1 and V2, and
  audit-read takes none.

Earlier status, kept for the record: steps 1–4 and V3 were at `056c8db` while step 5 waited for the
merge.

Commits (red first, then green):

| Commit | What | Red observed |
|---|---|---|
| `0498c56` test | AC-12 journey + `ClickRecorderTest` expects `reduction failed` | both failed: `expected "reduction failed" but was "rejected"` |
| `2c5b8e8` fix | `ClickRecorder` reason `reduction failed` (W2-05) | green |
| `1d63e4c` test | `ClickAuditColumnsTest` (AC-16) | failed: audit columns `[]` before V3 |
| `632ecb9` feat | `V3__add_click_audit_columns.sql` verbatim from the design probe | green |
| `64abbfe` test | `ClickPurgeTest` + six purge journeys | red against a no-op `ClickPurge` stub (signatures only, not committed): 7/8 unit and 12/17 functional failed on assertions (`Wanted but not invoked: deleteBefore`, rows of `T−91` still present, no `clicks purged` line, …) |
| `056c8db` feat | `ClickRetentionProperties`, `ClickPurge`, `ClickStore.deleteBefore`, `package-info`, the granted overlay line | green |

Passed even against the stub, and why that is expected: `aClockThatStepsBackRunsNothing` and
`ClickPurgeHoldJourneyTest` assert an absence; AC-4 ×3 exercise the real `ClickRetentionProperties`
binding, which is configuration rather than the stubbed mechanism. To show the hold journey is not
vacuous, it was run with the overlay line flipped to `true`: it failed on a `clicks purged` line in the
shared context. The line was then restored, byte for byte.

**Gate.** `scripts/gw --log build/check.log --offline check` in the worktree after the last code edit:
BUILD SUCCESSFUL. Unit 174 tests, functional 172 tests, 0 failures or skips. JaCoCo merged: lines
490/490, branches 168/168 (100 %). `javadoc -Xdoclint:all -Werror` green.

**By effect** ([`proof/builder-by-effect-056c8db.txt`](proof/builder-by-effect-056c8db.txt)). The real `f6dd29e` jar wrote
a data directory: 2 links, 4 clicks today, 3 audit rows. Backdated clicks were added with H2
`RunScript` while it was stopped. Then the candidate jar started on it:
- V3 applied in 28 ms.
- The startup run logged `{"message":"clicks purged","deleted":6,"cutoff":"2026-07-05","retentionDays":90}`,
  with no request id and no click value.
- Readiness came about 1.8 s after `Starting`.
- `T−90` was kept; `T−91` and `T−120` were gone.
- Links and audit rows were unchanged. Old clicks were backfilled `created_at = updated_at = clicked_at`, `anonymous`.
- A post-upgrade redirect stored a row with `created_at = updated_at`, `anonymous`. The classes are `system`.
- Statistics cover only the retained window.

Error path: `--urlshort.click.retention-days=0` stops startup. The single failure-analysis ERROR names
`urlshort.click.retentionDays`, value `"0"` and its origin, with no `jdbc:` URL. No purge line was written.
The time-to-readiness figure answers the lead's named check only at this size (16 clicks). It does not
cover the 1.3 M-click case (ADR-0018 ceiling, probe M5/L10).

## Self-check

- Re-read `git diff main...slice/02-click-retention`. No dead code, and no duplicated logic in
  production. Every error path has a test: a failed run (AC-10), a reduction failure (AC-12) and an
  invalid setting (AC-4). No log line carries a click value, link code or id (AC-9 and AC-10 by canary
  and member checks). The one `ponytail:` comment names its ceiling and upgrade path (ADR-0018).
- Ladder: no new dependency, no interface, no Spring scheduler (design S1). The tick is a JDK
  `ScheduledExecutorService`, the setting a `@ConfigurationProperties` record like `RateLimitProperties`.
- Every AC has a named test that was seen red for the right reason, except the absence and binding
  cases listed above.
- Stale design line honoured: the hold's WARN is `click purge paused, no click is deleted` (design
  §1/§5, DR-04), not §6's `click purge off`.
- Test-side deviations from design §7, all within the class and file names it lists. AC-11 (a) scopes
  its uncommitted `DELETE` to the test's link, because sibling tests in the same database leave old
  clicks. `ClickAuditColumnsTest` compares indexes by table, kind and columns, because H2 renames its
  generated index names when `ALTER TABLE` rebuilds a table.
- AC-4's three failed starts run inside the functional JVM, and the whole suite stayed green. The
  logging-cleanup risk predicted in design §7 did not show, so no child JVM was needed.
- **Not verified by me:** the env-var form `URLSHORT_CLICK_RETENTIONDAYS` (probe A4 did); a real
  `SIGTERM` during a run; a large directory; PostgreSQL. My first two jar starts (shipped and candidate)
  omitted `--server.address=127.0.0.1`, which the operator flagged. They ran about 20 s each on a
  developer machine, and the invalid-setting run was repeated loopback-bound.

## QA — qa2-agent@urlshort-factory (Codex), 2026-10-03

**PASS on X = a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1**, with proof item 9
explicitly left for X′ under the lead's 21:42Z custody amendment. QA packet
qitem-20261003214414-12d75069. This is QA handoff, not slice acceptance.

Fresh offline gate: 174 unit / 172 functional, no failures/errors/skips;
merged 490/490 lines, 168/168 branches; Javadoc green. Independent original
f6dd29e functional-source replay: 155/155, with only the granted profile overlay.
All 333 copied coverage reports were hashed twice against source. All 210 named
source test methods map to an AC/business rule; all 16 ACs have functional tests.

The corrected independent effects driver completed 249 assertions. I observed
all ACs through actual HTTP/rows/logs: strict boundary and advancing window,
real 7-day environment override, all three invalid values without deletion,
96→91 daily statistics, empty→one-click statistics with identical creation/audit
rows, installed f6dd29e→X upgrade, automatic startup and no-trigger 00:10Z daily
purge, zero/nonzero one-INFO outcomes, a real store failure followed by successful
retry, redirect plus writer commit while actual H2 DELETE remained blocked,
actual keyed-hash reduction failure with correlated distinct WARN, environment
pause at both required +60-second service-clock observations, and fresh/upgraded
schema and row audit values. Full original column lengths/defaults/precision,
PK/FK/check semantics and index columns were compared unchanged. All 207 saved
HTTP responses correlate to JSON product events. Normal/invalid app logs and
raw commands/responses/JDBC query results are saved in proof/qa-effects-final-a8fc8b6/.

The default installed upgrade had five original clicks on five UTC days. At
readiness before another redirect it retained only July 5, September 23 and
October 3 (one each), deleted June 5 and July 4, and logged deleted=2,
cutoff=2026-07-05, retentionDays=90. Links and audit rows were byte-identical;
retained v1 click columns/classes stayed unchanged and V3 audit values were filled.

Independent design re-review read/executed the exact migration rollback (29
assertions); its SQL equals the candidate at SHA-256
908715401b5c84aa8b4d1525b91a9bd472d2a9ada605a8dbd5651ff1fdfc5fb6.
ADRs 0018/0020 and amendments 0011/0013 existed and were indexed before dependent
implementation commits. Product diff from base 16312da has 18 paths in the
granted territory (builder packet described 16); no extra territory issue.

Not checked: Docker/PostgreSQL, large-data catch-up budgets or SIGTERM during
purge. The design's probes retain their own attribution. Clock/JDBC/HMAC controls
are QA fixtures, not product endpoints. Script assumptions corrected during
probing and the sandbox denial are retained in qa-instrumentation-notes.md.
No product, canonical test, build config or script edit was made.

Evidence entry: docs/qa/coverage/02-click-retention/SUMMARY.md; traceability and
gaps appended. Custody continuation qitem-20261003221121-7686465a returns item 9
to QA on X′ before acceptance; X stays at the named SHA through review. Fresh
range-diff and full gate on X′ are required by the lead's rule.

## Self-check

- All ACs exercised by effect, including bad input, replay/mismatch/expiry,
  retirement, rate limit, deletion/store/hash failure and pause cases.
- Actual merged CSV read; per-suite deficits documented with merged complement.
- Reports copied and SHA-256 verified; original 24 functional sources unchanged.
- Traceability complete both ways: 210 methods, AC-1..16, BR-1..7.
- Gap entry written; performance/shutdown scope and custody sequencing explicit.
- Proof drop names items 1–8 and 10; each judgment attributed to X. Item 9 is
  deliberately unjudged until X′ under the lead's explicit exception.
- Six healthy final-run JVMs stopped; three invalid starts exited; independent
  probes confirm ports 18141..18149 closed. Candidate worktree unchanged at X.

QA evidence committed at 84d3604. Attributed receipts 1–10 accept proof items
1–8 and 10 against X; item 7 was re-affirmed with a durable exact migration
copy instead of a worktree evidence path. Live state is nine accepted, item 9
pending, no issues, saved in proof/qa-judgment-state-a8fc8b6.json.
