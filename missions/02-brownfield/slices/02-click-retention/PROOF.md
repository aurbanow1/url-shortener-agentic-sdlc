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

**Status: built and green, not yet the candidate.** Build-plan steps 1–4 plus the V3 pair are on
`slice/02-click-retention` at `056c8db`. Step 5 (the two `application.properties` lines and the
README grant) waits for `01-audit-read`'s merge (ordered custody). The candidate SHA is the commit
after that rebase. This section is updated with it, and `scripts/gw check` is re-run then.

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
