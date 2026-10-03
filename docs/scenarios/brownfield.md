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
the other model family found it before merge. `[final]`: the re-review verdict
and the merged SHA.

## Orchestration and the drills `[final]`

Delegated slice plan-locks (D11); concurrent w1 instances on the two judge
pairs; w2 launched after w1 integrated. Drills, each labelled and recorded in
`docs/scenarios/drills.md`, none on a product slice:

| Drill | What was exercised | Evidence |
|---|---|---|
| QA rejects a candidate → remediation loop | natural if one occurs, else a labelled drill instance | `[final]` |
| Integrator `git revert` after a failed installed smoke | throwaway branch and worktree off `main` | rehearsed at mission 01 release prep (`rollback-rehearsal`); `[final]` |
| `rig seat stop` → `rig workflow route` | labelled drill instance (`urlshort-drill`) | `[final]` |
| `rig workflow abort` + `resume` | labelled drill instance | aborted instance `01M41B1ABGY3WR0DKEPZCJE9D3`; `[final]` |

## Validation `[final]`

Gate on the shipped SHA, regression journeys from mission 01 unchanged,
migration rollback rehearsed, installed smoke, release review, ship sign-off.

## Metrics `[final]`
