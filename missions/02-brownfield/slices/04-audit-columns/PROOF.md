# PROOF — OPR.99.0.3.4 Audit Columns

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.4 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

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

## Builder

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003223549-c479c189`.
This section is builder evidence, not a verdict.

**Status: built on `main` `583278c` (V1 and V2 only), not yet a candidate.** The plan-lock requires a
rebase onto `02-click-retention`'s merge commit, which brings V3, then confirming that V4 is the next
Flyway number and a fresh gate. Click-retention is in code review.

| SHA | Commit |
|---|---|
| `8f72b2f` | `test`: `LinkAuditColumnsTest` (AC-1, AC-11), `LinkServiceStampTest`, `LinkAuditColumnsJourneyTest` (AC-2 to AC-6, AC-8, AC-10), `LinkAuditColumnsFailureJourneyTest` (AC-5's failed retire), `LinkUpgradeJourneyTest` (AC-7) |
| `49f0cee` | `feat`: V4 copied verbatim from `design-probe/migration/`, the retire SQL that also stamps, `LinkRepository.stamp(id, at)`, and the two `LinkService.stamp` calls |

### Commands and outcomes

| Command | Outcome | Record |
|---|---|---|
| the new journeys on `8f72b2f`, before V4 | red: 9 run, **7 failed** (no audit columns; no V4 in `flyway_schema_history`). AC-8 and the failed-retire case pass, as expected before the change: nothing is visible yet, and nothing is stamped | `proof/builder-red-functional.txt` |
| the unit suite on `8f72b2f` | red as a compile failure: `LinkRepository.stamp` does not exist | `proof/builder-red-unit.txt` |
| `scripts/gw --offline check --rerun-tasks` on `49f0cee` | BUILD SUCCESSFUL; **unit 208/208, functional 211/211**; merged **496/496 lines, 194/194 branches**; `javadoc` green. Every shipped test passes unchanged, `ClickSchemaTest`'s and `ObservabilityJourneyTest`'s v1-shaped inserts included (AC-9) | `proof/builder-check-49f0cee.txt` |
| `git diff --stat 583278c 49f0cee` | eight files: `link/` main and tests, and V4. No shipped test, `audit/` or response class changed | — |

### Deviations from design.md

1. **"Before V4" is computed, not `target("3")`.** Flyway 11 refuses a target with no migration ("No migration with a target version 3 could be found"), and this base has no V3. `LinkAuditColumnsTest` and `LinkUpgradeJourneyTest` therefore migrate to the highest migration version below 4 on the classpath. That is V2 today and V3 after the rebase, with no test change.
2. **Each journey test sends from its own loopback peer** (`127.44.0.n`). With one shared peer, AC-4's +25 h shift left that peer's rate-limit bucket ahead, and the next test got `429`s (first red run). Loopback peers also admit the AC-8 audit read.
3. **The upgrade journey's released link** is a link whose key is `NULL` next to a later link holding `K`, in the shipped shape. A key release leaves no other trace (rule 6).

### Not verified yet

- The rebase onto V3, the Flyway-number check, and the gate on that SHA.
- The by-effect upgrade (proof item 6) from the real `f6dd29e` jar. I will run it on the final candidate.
- PostgreSQL's `LEAST` and `ALTER … DEFAULT … NOT NULL`, which the design did not run either.
