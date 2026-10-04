# Scenario: brownfield — mission `02-brownfield`

> **Draft — completed at mission close.** Facts below are from the mission brief,
> the decomposition review and the live run so far; `[final]` marks what is
> filled in from `missions/02-brownfield/RELEASE.md` and the evidence export.

## Decomposition

The brief (`missions/02-brownfield/SPEC.md`) changes the *shipped* shortener
(`main` after mission 01: create/redirect, analytics, operate). The lead
proposed three slices in two waves plus four labelled drills; the
decomposition review passed with no findings (15/15 artefacts); the human
approved at the mission plan-lock.

| Wave | Slice | Outcome | Brownfield obligation |
|---|---|---|---|
| w1 | `01-audit-read` | an Operator reads the audit trail through a read-only, paginated, loopback-only-by-default endpoint over the existing `audit_log` table (FR-17) | `impact-analysis.md` before design; existing links unchanged (FR-13) |
| w1 | `02-click-retention` | clicks older than the retention period (90 days, operator setting) are deleted on schedule (NFR-P2) | migration with written rollback (NFR-X2); purge by stored UTC day only |
| w2 | `03-dogfood-fix` | a defect found by using the shipped service is fixed with a regression test first (FR-14); tests and docs it touches match shipped behaviour (FR-15) | input: QA's dogfood report from mission 01's release prep (two known defects confirmed) |
| w2 | `04-audit-columns` | the human's audit-column policy (created/updated at and by) reaches `link` and `audit_log` through one expand migration, V4 | added mid-mission by a human decision; migration with written rollback; no response shape changes |
| w2 | `05-ci-cd` | CI/CD in GitHub Actions: the `check` gate on every pull request and on `main`, a loopback smoke of the shippable jar, weekly dependency proposals | added mid-mission by human decision D14; `.github/` only |
| w3 | `06-client-identity` | a behaviour-preserving refactor: one `web/ClientIdentity` answers "who is this client" for the rate limiter, click recording and the audit guard (architecture.md §11 row 1, ADR-0015) | added by human decision D21 after the coverage review found no pure refactor; characterization tests first, the move in separate commits, QA before-and-after captures of responses and logs |

Shared-file custody inside w1: `application.properties` and the next Flyway
version number, `01` first; disjoint feature packages (`audit/` vs `click/`).

The plan changed twice while the mission ran, each time recorded as a
revision of the mission's compiled graph with a receipt, and with the
mission plan-lock re-stamped on the human's behalf:

- **Two slices added by human decisions** (`04-audit-columns`, `05-ci-cd`).
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
pairs; w2 launched after w1 integrated. Drills, each labelled and recorded in
`docs/scenarios/drills.md`, none on a product slice:

| Drill | What was exercised | Evidence |
|---|---|---|
| QA rejects a candidate → remediation loop | DRILL 1, labelled drill instance `urlshort-drill` (no natural QA rejection occurred; the day's natural rejection was a code review, CR-01) | QA failed the deliberately defective candidate (MUST-FIX DRILL1-01) at 23:29:04Z, the fix was re-checked and the instance completed (`01M4212A8BKA6JRZHZQBD90D07`); `docs/scenarios/drills.md` |
| Integrator `git revert` after a failed installed smoke | throwaway branch and worktree off `main` | rehearsed at mission 01 release prep (`rollback-rehearsal`); `[final]` |
| `rig seat stop` → `rig workflow route` | DRILL 3, 16:54–16:56Z | the stranded step was re-owned by the lead's route without advancing or losing it, and the stopped seat came back; ordinary routes during the run (capacity moves) are listed separately as natural routing events; `docs/scenarios/drills.md` |
| `rig workflow resume` then `abort` | DRILL 4, 16:57–16:58Z; order taken from the CLI's own contract | a failed step redriven once, then the whole instance safely stopped (`01M41B1ABGY3WR0DKEPZCJE9D3`); `docs/scenarios/drills.md` |

## Validation `[final]`

Gate on the shipped SHA, regression journeys from mission 01 unchanged,
migration rollback rehearsed, installed smoke, release review, ship sign-off.

## Metrics `[final]`
