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

Shared-file custody inside w1: `application.properties` and the next Flyway
version number, `01` first; disjoint feature packages (`audit/` vs `click/`).

## Codebase reasoning `[final]`

Per slice: the impact analysis (impacted modules, endpoints, schema and data
flows, blast radius, compatibility, test impact, observability impact,
rollback) and what the design review checked it against
(`docs/guidance/brownfield.md` §2, §7).

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
