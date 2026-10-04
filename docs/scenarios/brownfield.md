# Scenario: brownfield — mission `02-brownfield`

> **Mission closed 2026-10-04 (`2522e6c2`).** Validation is from
> `missions/02-brownfield/RELEASE.md`; metrics are from the run-end refresh of
> `docs/metrics/`.

## Decomposition

The brief (`missions/02-brownfield/SPEC.md`) changes the *shipped* shortener
(`main` after mission 01: create/redirect, analytics, operate). The lead
proposed three slices in two waves plus four labelled drills; the
decomposition review passed with no findings (15/15 artefacts); the human
approved at the mission plan-lock.

| Wave | Slice | Outcome | Brownfield obligation |
|---|---|---|---|
| w1 | `01-audit-read` | an Operator reads the audit trail through a read-only, paginated, loopback-only endpoint over the existing `audit_log` table (FR-17) | `impact-analysis.md` before design; existing links unchanged (FR-13) |
| w1 | `02-click-retention` | clicks older than the retention period (90 days, operator setting) are deleted on schedule (NFR-P2) | migration with written rollback (NFR-X2); purge by stored UTC day only |
| w2 | `03-dogfood-fix` | a defect found by using the shipped service is fixed with a regression test first (FR-14); tests and docs it touches match shipped behaviour (FR-15) | input: QA's dogfood report from mission 01's release prep (two known defects confirmed) |
| w2 | `04-audit-columns` | the human's audit-column policy (created/updated at and by) reaches `link` and `audit_log` through one expand migration, V4 | added mid-mission by a human decision; migration with written rollback; no response shape changes |
| w2 | `05-ci-cd` | CI/CD in GitHub Actions: the `check` gate on every pull request and on `main`, a loopback smoke of the shippable jar, weekly dependency proposals | added mid-mission by human decision D14; `.github/` only |
| w3 | `06-client-identity` | a behaviour-preserving refactor: one `web/ClientIdentity` answers "who is this client" for the rate limiter, click recording and the audit guard (architecture.md §11 row 1, ADR-0015) | added by human decision D21 after the coverage review found no pure refactor; characterization tests first, the move in separate commits, QA before-and-after captures of responses and logs |

Shared-file custody inside w1: `application.properties` and the next Flyway
version number, `01` first; disjoint feature packages (`audit/` vs `click/`).

The plan changed three times while the mission ran, each change recorded as
a revision of the mission's compiled graph with a receipt, and with the
mission plan-lock re-stamped on the human's behalf; three slices were added:

- **Two slices added by human decisions** (`04-audit-columns`, `05-ci-cd`).
- **A refactor slice added as wave 3** by human decision D21
  (`06-client-identity`); the mission's release and sign-off waited for it.
- **Wave 2 pulled forward** (wave map v4): its requirements and designs ran
  during wave 1's builds once a second designer and builder existed (D15,
  D16). Its builds still waited for the code they depend on. Two slices were
  built stacked on another slice's reviewed branch: `03-dogfood-fix` on
  `01-audit-read`, and mission 03's `01-analytics-v2` on `02-click-retention`.
  Each must descend from its base's merge commit, checked at integration.

## Codebase reasoning

Every slice wrote `impact-analysis.md` before its design, covering the rows of
`docs/guidance/brownfield.md` §2. The design review judged the design against
it (§7). The table gives each slice's change, the risk its analysis ranked
first, and where that risk was caught or mitigated.

| Slice | Change, from the analysis | Top-ranked risk | Mitigation, and what happened |
|---|---|---|---|
| `01-audit-read` | read-only, cursor-paginated `GET /api/audit`, loopback clients only, `server.forward-headers-strategy=none` pinned | the trail reachable from beyond loopback (NFR-S6) | Design probe P4b found that without the pin, a detected cloud platform admits a forged `X-Forwarded-For`, so the pin went in. **The analysis still missed one path**: Tomcat's own `remoteip` header settings rewrite the peer even under `NONE`. QA passed it; code and security review failed it (CR-01 HIGH, reproduced with a planted canary). The guard now refuses whenever any remote-IP setting is active, with real-Tomcat regression cases for each, and QA recorded the miss as a gap in its own first pass. |
| `02-click-retention` | daily and startup purge of clicks older than the period, 90 days by default; V3 expand migration with backfill | clicks deleted early and irreversibly | The cutoff is the application clock's UTC day minus P, deleting rows strictly before it; boundary criteria run through real redirects on shifted days. The `DELETE` names only `click`, and the setting is validated at startup (an invalid period stops the service). A purge hold for incidents is loud at every start. |
| `03-dogfood-fix` | the API document's problem schema matches the bodies actually sent; disk gauges lose their `path` tag on anonymous metrics | the document drifting elsewhere | A regression test for each defect was written first and seen failing (`proof/red-*`). QA diffs the regenerated document against the live one. |
| `04-audit-columns` | V4 adds `updated_at` and `created_by`/`updated_by` to `link` and `audit_log` | a new column leaking into a response | The records don't map the new columns, and the audit read names its columns. The design review ran 32 independent V4 assertions; the plan-lock re-checks against merged `main` because `01-audit-read` also changes `audit/`. |
| `05-ci-cd` | three files under `.github/`, no product code | a green `gate` that did not run the whole gate | The gate is one `./gradlew check` line with no exclusions, reviewed against `ci-cd.md` §6 and linted by `actionlint` in a no-network container. The first GitHub runs were green (`PROOF.md` §AC-13). |

The audit-read row is the scenario's most useful record: a ranked risk with a
probe and a pin still had a second activation path. The independent review on
the other model family found it before merge. The corrected guard passed QA's
re-check and the re-review, and merged as `cb148c4`.

The refactor (`06-client-identity`) then gave that concern its one code path.
Its own code review failed once on a test, not on the refactor: a new
characterization test polled statistics under the rate limiter's fixed budget,
which a slow write could exhaust (CR-01, HIGH, because the gate must be
deterministic). The fix waited on storage instead of polling, QA re-checked
the 12 identity and grouping rows on the fixed build, and the re-review
passed. The refactor merged as `b8d7fc16`, with every journey and the API
document unchanged.

## Orchestration and the drills

Delegated slice plan-locks (D11); concurrent w1 instances on the two judge
pairs; w2's builds waited for w1. Drills, each labelled and recorded in
`docs/scenarios/drills.md`, none on a product slice:

| Drill | What was exercised | Evidence |
|---|---|---|
| QA rejects a candidate → remediation loop | DRILL 1, labelled drill instance `urlshort-drill` (no natural QA rejection occurred; the day's natural rejection was a code review, CR-01) | QA failed the deliberately defective candidate (MUST-FIX DRILL1-01) at 23:29:04Z, the fix was re-checked and the instance completed (`01M4212A8BKA6JRZHZQBD90D07`); `docs/scenarios/drills.md` |
| Integrator `git revert` after a failed installed smoke | DRILL 2, 16:20–16:35Z, throwaway branch and worktree off `main` | a merged fault (`Cache-Control: no-store` dropped) failed the installed smoke; the lead's `git revert -m 1` restored a tree equal to `main`, and gate and smoke passed again; `main` never touched; `docs/scenarios/drills.md` |
| `rig seat stop` → `rig workflow route` | DRILL 3, 16:54–16:56Z | the stranded step was re-owned by the lead's route without advancing or losing it, and the stopped seat came back; ordinary routes during the run (capacity moves) are listed separately as natural routing events; `docs/scenarios/drills.md` |
| `rig workflow resume` then `abort` | DRILL 4, 16:57–16:58Z; order taken from the CLI's own contract | a failed step redriven once, then the whole instance safely stopped (`01M41B1ABGY3WR0DKEPZCJE9D3`); `docs/scenarios/drills.md` |

## Validation

From `missions/02-brownfield/RELEASE.md` (product `30f8de4e`) and the release
review (`docs/review/02-brownfield/release-review.md`, PASS at `446eca31`):

| Check | Result |
|---|---|
| Fresh gate on the shipped commit | 268 unit and 322 functional tests, zero failures; 583/583 lines and 206/206 branches; Javadoc doclint. The reviewer re-ran all 590 tests independently |
| Regression on shipped behaviour (FR-13) | mission 01's journeys run unchanged in the same suite; links created before the change keep redirecting |
| Installed smoke | the jar (127.0.0.1:18240) and the container image (127.0.0.1:18241, read-only root, UID 10001) both passed `scripts/smoke.sh`; the image's jar equals the tested jar byte for byte |
| Migration rollback | V4 then V3 rolled back on a copy of real data; the row counts prove versions 1–4, then 1–3, then 1–2, then 1–4 again with older data unchanged. Then the previous binary (`f090103`) started on the rolled-back copy and passed smoke |
| Dependency advisories | OSV on 97 runtime coordinates: zero advisories (point in time) |
| Hosted CI | the refactored code passed GitHub's `gate` in pull request #14, and `main`'s CI and CD runs after it; the shipped commit differs from it in documentation only |
| Proof | 64/64 items accepted across the six slices, re-affirmed where shared documents drifted |
| Ship sign-off, by the human, 2026-10-04 05:23:06Z | "approve: ship mission 02 (six slices) at 30f8de4e for local use; the exact-SHA hosted CI gap is accepted because the delta from the CI-verified e43ed246 (pull request #14) is documentation only; the release's measurement limits are accepted" |

Not proven, and stated as such in the release package (§7): remote TCP and
IPv6 behaviour, natural disk failure, a large purge catch-up, and performance
on the intended host. The latency figures are dated, single-host quantiles
from a macOS developer machine (Docker in Colima), and no load test was re-run
on the shipped commit. GitHub has no run on the exact shipped commit, because
agents never push. The original host-path criterion AC-28 did not pass; the
amended criterion was accepted by a human decision. The refactor's own revert
is described but was not executed; the integrator revert drill covers that
path generically.

Mission 02 closed at `2522e6c2`, with its backlog recorded in
`missions/02-brownfield/NOTES.md`.

## Metrics

From `docs/metrics/README.md`, the run-end refresh (generated
2026-10-04T05:38:42Z from the live daemon after this mission closed, every
instance terminal):

| Instance | E2E latency | Hops | Closures | Retries | Rollback text matches | Human wait | MTTR |
|---|---|---|---|---|---|---|---|
| mission lifecycle `02-brownfield` | 17.7 h | 9 | 14 | 0 | 2 | 3.6 h | – |
| `01-audit-read` | 6.1 h | 15 | 16 | 10 | 0 | 0 s | 41 min |
| `02-click-retention` | 7.4 h | 12 | 18 | 6 | 2 | 0 s | 38 min |
| `03-dogfood-fix` | 1.8 h | 8 | 9 | 0 | 0 | 0 s | – |
| `04-audit-columns` | 5.6 h | 8 | 11 | 0 | 0 | 0 s | – |
| `05-ci-cd` | 4.0 h | 8 | 10 | 0 | 0 | 0 s | – |
| `06-client-identity` | 3.2 h | 11 | 14 | 4 | 0 | 0 s | 35 min |

The six slices had six failed closures behind 20 retry counts: the audit read's
requirements, design and code review (CR-01), two retention design reviews,
and the refactor's test-only polling race. Each retry is a review loop working
as designed. No rollback was executed in production. The "rollback" column is
a text-match heuristic: the lifecycle's two hits are the plan brief that names
a revert drill and the ship gate's summary of the rollback recipe; retention's
two describe its migration rollback requirement. The real drills and the V4/V3
rehearsal have their own evidence (above). Human wait is the mission plan-lock
(12,869 s) and the ship gate (100 s). The delegated slice plan-locks went to
the lead, so they count as 0 s here.
