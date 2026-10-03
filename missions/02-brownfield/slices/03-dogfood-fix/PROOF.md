# PROOF — OPR.99.0.3.3 Dogfood Fix

> **WHO/WHEN:** the impl/QA pair that worked the slice, at slice-close — a slice is NOT done until this file exists and every `SPEC.md` proof-contract item has evidence (mapped 1:1, artifacts under `proof/`). See the `mission-slice-sop` skill + the conventions SSOT (`docs/reference/sdlc-conventions.md` in the repo, `$OPENRIG_HOME/reference/sdlc-conventions.md` on an installed package).
>
> **HOW (the drop verb, not hand-placement):** put media files under `proof/`, then ATTACH them with `rig proof add OPR.99.0.3.3 --artifact-type qa --verdict PASS --candidate-sha <tip> --money-evidence "<one line>" --evidences "1" --media "screenshot-01.png"` — the drop writes the C1 header the Living Notes DELIVERED pairing joins on. Hand-placing files without a drop leaves the deliverable unpaired and `unverified`.

QA checked by: qa-agent@urlshort-factory (Codex),2026-10-03.
QA verdict: PASS on 4fe7042; integration and independent review follow.

## What this proves

The API document describes existing validation errors without changing
problem responses. Anonymous disk metrics retain values while removing the
installation path. Independent evidence covers all nine ACs and five rules.

## Artifacts (media in proof/)

QA drop proof/qa-pass-4fe7042.md is attached through rig proof add,
covers items 1–8. Independent evidence: proof/qa-4fe7042/, gate
proof/qa-check-4fe7042.txt, reports/CSV summary
docs/qa/coverage/03-dogfood-fix/.

## Residue / caveats

Default single-disk-path scope, removed-selector compatibility and inherited
Windows-wrapper line endings are disclosed in GAPS and QA below.
No merged coverage shortfall or required AC failure.

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


## QA — independent check of 4fe70427bd0d182e886d6a19b217daa1d9e39f5d

Seat qa-agent@urlshort-factory (Codex), packet qitem-20261003215524-d56667a8,
instance 01M41HGF6AHB6AZR3Q0KQ8MQR7. Primary worktree HEAD equals packet SHA.
QA authored no product, build or test change.

**Observed by effect.** Fresh offline check executes all 14 tasks:
204 unit/207 functional, no failures/errors/skips; Javadoc/verification green.
Copied CSVs: unit 441/508 lines188/194 branches; functional 471/508 lines162/194
branches; merged 508/508 and194/194. All 354 copied report hashes checked.

| AC | Independent observation |
|---|---|
| 1 | Live ProblemDetail optional errors array of ProblemFieldError; exactly required string field/rule/message; no properties; five other property schemas unchanged. |
| 2 | Real jars: invalid create400, same-key different-URL422, unknown-code404, retired redirect410, over-create-budget429, malformed audit400. Every body field documented; both400s and422 one errors item; other statuses omit it. Default60-create budget gives30 captured429 refusals per jar with positive Retry-After. |
| 3 | Whole candidate live/committed document equality; empty committed-vs-live.diff. Baseline live also matches committed doc. Paths/operations/responses/headers/examples identical outside two problem components; all 22 problem refs use one schema. |
| 4 | Six full semantic bodies including title and every field/rule/message, status/content type match baseline jar; only instance/request IDs differ. wire-comparison.json retains both normalized values. |
| 5 | Independently checked out cce7cf7 and reran OpenApiDocumentTest: exactly two assertions fail naming missing errors/extra properties; fresh candidate green. History establishes test before a28a20a fix. |
| 6 | Baseline scrape/disk metric endpoints reproduce known absolute path. Candidate no sample path tag, no known cwd anywhere in scrape, no path availableTags; both gauges numeric/positive. Independent72dfffb run: exactly endpoint/scrape assertions fail on path; candidate passes after 4fe7042. |
| 7 | QA closes QA-OPR-02 original row with installed evidence, appends honest qualifications. W2-01 recorded fixed; no open W2-01 row. |
| 8 | Read DESIGN error/API/metric descriptions, ADR-0010/0016 amendments/§7 index, README smoke/endpoints/documentation lines12/18/39. Descriptions match wire/schema/pathless gauges; README needs no correction. ADR design 0d000da/clarification 0982cb5 precede dependent commits by ancestry. |
| 9 | Extracted all 50 original test/resource files from 15db6c5 with Git-blob hashes; external QA init tasks compile against candidate classes and run203 unit/202 functional unchanged, all green. Shipped test diffs add102+41 lines only; new27-line unit test allowed; no other shipped test changes. |

**Logs and audit effects.** All 208 HTTP captures have a unique UUID response
header and one completion/status in default JSON console and ECS file log.
Create/retire audit rows from functional audit endpoint match response IDs,
code and before/after states; replay adds no row. No peer/URL/user-agent
canary in either whole-run log. Added sink was ECS from startup, default
console checked separately. Raw transport/JUnit/console bytes archived and
hashed before whitespace-only display normalization; verify.py checks it.

**Limits.** Recorded Git/document checks expressly allowed by SPEC, not
claimed as JUnit-only. Parameterized XML names omit source methods; every233
named method has a row and green class-group attribution where needed,
all 411 invocations retained. No second disk path configured; adding one
requires non-sensitive tag per ADR-0016. Old path selector now404,
deliberately documented; unfiltered gauges200. No new Docker/load/migration/
Swagger rendering or natural-clock-boundary proof. Baseline source/build
hashes match15db6c5; jar built from later docs-only main6140c3c. Inherited
candidate gradlew.bat difference only line endings; QA neither repairs nor
commits it.

### Self-check

- Every AC checked through promised effect; six failure cases captured from
  both installed jars, full values compared.
- Fresh gate and unchanged original suites independently green; coverage
  read from three CSVs, merged 100%line/branch.
- All 233 source methods mapped both ways; parameterized-group boundary named.
- GAPS entry written, QA-OPR-02 closed; README check recorded.
- Both historical regressions independently RED before fixes; ADR
  amendments/index precede dependent code.
- Proof drop covers items 1–8 with --evidences and retained media; attributed
  candidate judgments follow evidence commit.
- Both apps stopped (exit130/SIGINT); ports18151/18152 refuse connections.
  Primary worktree exact 4fe7042, inherited wrapper-line-ending noise only;
  no authored source/build/test edit.
