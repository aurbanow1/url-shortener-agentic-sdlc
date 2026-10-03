# PROOF — OPR.99.0.3.3 Dogfood Fix

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.3 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

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

Seat `development-agent@urlshort-factory` (Claude), 2026-10-03, packet `qitem-20261003200223-d361adc6`.
This section is builder evidence, not a verdict.

**Status: built on the stacked base, not yet a candidate.** `slice/03-dogfood-fix` is stacked on
`01-audit-read`'s `35590f0`, which is in code review. Code review reported a HIGH on it at 20:07Z,
and its packet exit is pending. The plan-lock requires a rebase onto audit-read's merge commit, a
regenerated document and a fresh gate before handoff. This section records the work up to that point.

| SHA | Commit |
|---|---|
| `9b2788a` | `test`: W2-01. AC-1 checks the schema. AC-2 checks the problem bodies: `400`, `422`, `404`, `410`, the audit read's `400`, and `429` from a nested context with a create budget of 1. 102 lines added, none changed. |
| `edc1815` | `fix`: an `OpenApiCustomizer` that removes `properties` and adds `errors` and `ProblemFieldError`; the regenerated document |
| `3224036` | `test`: W2-03 on `/actuator/metrics/disk.free` and `disk.total`, and on the scrape, from a nested `@AutoConfigureMetrics` context. 41 lines added, none changed. |
| `5233c29` | `fix`: `MetricsConfig` (`MeterFilter.ignoreTags("path")`) and `MetricsConfigTest` |

### Commands and outcomes

| Command | Outcome | Record |
|---|---|---|
| `functionalTest --tests '*OpenApiDocumentTest*'` on `9b2788a` | red. AC-1: "ProblemDetail documents the errors member the service sends and no properties member … [detail, instance, properties, status, title, type]". AC-2: "[400 member errors is documented] Expecting value to be true". The `429` case passed, because its members were already documented. | `proof/red-w2-01-openapi.txt`, `proof/red-w2-01-report.xml` |
| the same on `edc1815`, before regenerating | AC-1 and AC-2 green; only `NFRM3` red, because the committed document was stale | — |
| `git diff docs/api/openapi.json` after regenerating | only `components.schemas.ProblemDetail` (`properties` removed, `errors` added) and the new `ProblemFieldError` (AC-3) | `edc1815` |
| `functionalTest --tests '*HealthMetricsJourneyTest*'` on `3224036` | red. "[disk.free carries no path tag] Expecting ["path"] not to contain ["path"]". The scrape: "[the scrape carries no path label]" | `proof/red-w2-03-metrics.txt`, `proof/red-w2-03-report.xml`, `proof/red-w2-03-scrape-report.xml` |
| `scripts/gw --offline check --rerun-tasks` on `5233c29` | BUILD SUCCESSFUL; **unit 201/201, functional 205/205**; merged **506/506 lines, 190/190 branches**; `javadoc` green | `proof/builder-check-5233c29.txt` |

### Deviations from design.md

1. **`ProblemFieldError` is built in a local variable** with `addProperty` and `setRequired`, not chained. The design allows this; it avoids the unchecked warning, and the document is identical.
2. **The `MetricsConfig` comment is a `ponytail:` line naming the ceiling**: every meter, and a second disk path would need its own tag. This follows the design's §6 known ceiling.
3. **AC-2 sends each create from an explicit peer** (`127.0.0.1` in the shared context, `10.88.0.2` in the nested one). The `422` uses a fresh key per run, so the shared database cannot interfere.

### Not verified, and residual risks

- **Not yet done:** the rebase onto `01-audit-read`'s merge commit, the document regenerated there, and the gate on that SHA. If code review changes audit-read (its HIGH at 20:07Z), this branch absorbs that change on the rebase.
- **Jar-level captures** (proof contract item 7: the `400` beside the live schema, the committed-vs-live diff, a path-free scrape) are QA's, per the design's §7.
- **AC-4** (the wire unchanged, as full bodies compared between `main` and the candidate jars) is QA's recorded check. By construction, no request-path code changed.

## Self-check (builder)

- **Test before fix**, each pair in its own commits, and each red run captured with messages that name `errors`/`properties` and `path`.
- **Territory:** `OpenApiConfig`, the new `MetricsConfig` and `MetricsConfigTest`, additions to `OpenApiDocumentTest` and `HealthMetricsJourneyTest`, and `openapi.json`. Nothing in `Problems`, `ProblemDetailsAdvice`, `RateLimitFilter`, `application.properties`, `click/`, `link/` or `audit/`.
- **AC-9:** the two shipped tests only gained lines (143 insertions, 0 deletions).
- **Gate** on the stacked tip, after the last code edit. It must be re-run after the rebase.
