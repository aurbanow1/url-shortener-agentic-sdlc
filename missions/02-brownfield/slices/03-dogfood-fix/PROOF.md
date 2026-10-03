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

**Candidate:** `4fe70427bd0d182e886d6a19b217daa1d9e39f5d`. The branch was built stacked on `35590f0`,
then rebased onto `01-audit-read`'s merge `cb148c4` and onto `main` `15db6c5` (docs only after the
merge). The rebase was clean. `git merge-base --is-ancestor cb148c4 4fe7042` succeeds.
Regenerating `docs/api/openapi.json` on this base gave no difference, because the live document
already equals the committed one.

Rebased SHAs: `9b2788a`→`cce7cf7`, `edc1815`→`a28a20a`, `3224036`→`72dfffb`, `5233c29`→`4fe7042`. The
table and the red runs below name the pre-rebase SHAs. Those red runs ran on the stacked base, whose
product code differs from `cb148c4` only by audit-read's CR-01 guard, which these tests do not touch.

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
| `scripts/gw --offline check --rerun-tasks` on `5233c29` (stacked) | BUILD SUCCESSFUL; unit 201/201, functional 205/205; merged 506/506 lines, 190/190 branches | `proof/builder-check-5233c29.txt` |
| `scripts/gw --offline check --rerun-tasks` on the candidate `4fe7042`, after the rebase | BUILD SUCCESSFUL; **unit 204/204, functional 207/207**; merged **508/508 lines, 194/194 branches**; `javadoc` green | `proof/builder-check-4fe7042.txt` |
| `git diff --stat cb148c4 4fe7042 -- src docs/api README.md` | only the territory: `OpenApiConfig`, the new `MetricsConfig` and `MetricsConfigTest`, `openapi.json`, and the two tests (+102 and +41 lines, 0 removed) | — |

### Deviations from design.md

1. **`ProblemFieldError` is built in a local variable** with `addProperty` and `setRequired`, not chained. The design allows this; it avoids the unchecked warning, and the document is identical.
2. **The `MetricsConfig` comment is a `ponytail:` line naming the ceiling**: every meter, and a second disk path would need its own tag. This follows the design's §6 known ceiling.
3. **AC-2 sends each create from an explicit peer** (`127.0.0.1` in the shared context, `10.88.0.2` in the nested one). The `422` uses a fresh key per run, so the shared database cannot interfere.

### Not verified, and residual risks

- **Repository line endings, not this slice:** `gradlew.bat` shows as modified in every checkout of `main` after the wrapper bump `f3e6b0b`. It has `text eol=crlf` attributes, but the committed blob does not match its normalised form, so the "modification" is line endings only. I did not commit it. To rebase, I hid it with a temporary `--assume-unchanged` flag in this worktree, then cleared the flag. Reported to the lead.
- **Jar-level captures** (proof contract item 7: the `400` beside the live schema, the committed-vs-live diff, a path-free scrape) are QA's, per the design's §7.
- **AC-4** (the wire unchanged, as full bodies compared between `main` and the candidate jars) is QA's recorded check. By construction, no request-path code changed.

## Self-check (builder)

- **Test before fix**, each pair in its own commits, and each red run captured with messages that name `errors`/`properties` and `path`.
- **Territory:** `OpenApiConfig`, the new `MetricsConfig` and `MetricsConfigTest`, additions to `OpenApiDocumentTest` and `HealthMetricsJourneyTest`, and `openapi.json`. Nothing in `Problems`, `ProblemDetailsAdvice`, `RateLimitFilter`, `application.properties`, `click/`, `link/` or `audit/`.
- **AC-9:** the two shipped tests only gained lines (143 insertions, 0 deletions).
- **Gate** re-run on the rebased candidate `4fe7042` after the last change, green.
