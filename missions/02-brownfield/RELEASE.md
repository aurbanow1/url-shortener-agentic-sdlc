# DRAFT, not a release record — 02-brownfield

Groundwork snapshot: 2026-10-03 21:17Z, plain queue item
`qitem-20261003211439-20ea2949`. The mission is still in `wave_integration`.
This document prepares the rollback and documentation work; it does not name
a release candidate or assert that the mission is ready to ship. Final evidence
belongs to `release_prep`, on the final SHA.

## 1. Decision brief for ship sign-off

Planned result: a loopback-only audit read, configurable click retention,
complete row-audit columns, corrected API/metrics documentation and CI/CD.
The [mission SPEC](SPEC.md) and [NOTES](NOTES.md) hold the human decisions and
the expanded slice allocation. The final candidate, proof readiness, review
verdicts and recommendation for ship sign-off are **release_prep, on the final
SHA**. The rollback requires a stopped-service data backup; a code or schema
rollback cannot recover clicks already purged.

## 2. Artifact and gate

**release_prep, on the final SHA**: record the merged candidate, tool versions,
fresh `scripts/gw --offline check bootJar`, jar hash, image digest, proof
readiness and CI/CD run URLs/conclusions for that same candidate. Follow
[release guidance](../../docs/guidance/release.md) and
[CI/CD guidance §5](../../docs/guidance/ci-cd.md#5-release-package).

Earlier hosted runs are recorded in §5 below. They do not supply this final
candidate's gate or artifact identities.

## 3. Installed smoke

**release_prep, on the final SHA**: jar and container on loopback, public and
operator journeys, upgrade/persistence checks, shutdown and runtime binding
inspection; capture results and logs, disclose untested paths and stop the
processes/containers started for the checks. No installed smoke was run for
this draft.

## 4. Dependency advisories

**release_prep, on the final SHA**: resolve the runtime classpath, run the OSV
check, state advisory severity/fixed version/reachability, and link remediation
queue items and their deadlines. No advisory lookup was run for this draft.

## 5. Evidence per slice and operator-doc deltas

These are the source documents for work in flight, not acceptance of a final
merged candidate. Fill in final QA/review/coverage and readiness at release prep.

| Slice | Source documents | Draft operator delta |
|---|---|---|
| 01-audit-read | [SPEC](slices/01-audit-read/SPEC.md), [design](slices/01-audit-read/design.md), [PROOF](slices/01-audit-read/PROOF.md) | Read-only `GET /api/audit`; rule-2 boundary below. CR-01 rework `7ac8af5` is in QA at this snapshot. |
| 02-click-retention | [SPEC](slices/02-click-retention/SPEC.md), [design](slices/02-click-retention/design.md), [PROOF](slices/02-click-retention/PROOF.md) | Retention setting, purge hold, irreversible deletion, V3. Design `f044cbe`; not yet merged. |
| 03-dogfood-fix | [SPEC](slices/03-dogfood-fix/SPEC.md), [design](slices/03-dogfood-fix/design.md), [PROOF](slices/03-dogfood-fix/PROOF.md) | Intended correction of ProblemDetail `errors` to the wire format and removal of installation-path meter tags; final rebased candidate still pending. |
| 04-audit-columns | [SPEC](slices/04-audit-columns/SPEC.md), [design](slices/04-audit-columns/design.md), [PROOF](slices/04-audit-columns/PROOF.md) | Proposed V4 on `link`/`audit_log`; design `5a6d168`, **pending plan-lock** and numbering check after V3. |
| 05-ci-cd | [SPEC](slices/05-ci-cd/SPEC.md), [design](slices/05-ci-cd/design.md), [PROOF](slices/05-ci-cd/PROOF.md) | CI `gate`, CD `package`, report/jar/smoke-log artifacts and weekly dependency proposals; slice integrated as `0aa3695`. |

### Audit-read boundary

The [system design §3](../../docs/DESIGN.md#3-cross-cutting-contracts) and slice rule 2 require
a loopback peer (`127.0.0.0/8`, `::1`, mapped loopback) with neither
`X-Forwarded-For` nor `Forwarded`. Other requests return `403` without trail
content. A headerless local relay is indistinguishable from the Operator:
**do not relay this endpoint through a local proxy**. Adding a forwarding
header makes the read refuse. No setting opens the read beyond loopback.

The corrected guard also requires effective `server.forward-headers-strategy=none`
and neither `server.tomcat.remoteip.remote-ip-header` nor
`server.tomcat.remoteip.protocol-header` set; native/framework or either
remoteip setting closes the read. This is the intended corrected behavior,
awaiting exact-candidate QA/re-review; the original `35590f0` bypass is in §7.

### Retention and hold

Per the [system design §3](../../docs/DESIGN.md#3-cross-cutting-contracts),
`urlshort.click.retention-days` (`URLSHORT_CLICK_RETENTIONDAYS`) is a positive
whole number, default **90**. On UTC day T, delete `clicked_on < T - P`;
day T - P is retained. Run once at startup before readiness, then once daily
at the first five-second application-clock tick at/after **00:10Z**. A failed
run is retried on the next day's run.

`urlshort.click.purge-enabled=false` (`URLSHORT_CLICK_PURGEENABLED`, default
`true`) holds both startup and daily deletion. Every held start logs WARN
`click purge paused, no click is deleted`, naming the setting and retention
period. Clicks accumulate while held. **Before lowering retention, stop the
service and copy `data/` if an undo is wanted**; the next startup deletes the
difference before readiness. Deletion has no archived or aggregated copy.

### CI/CD and Gradle

The integrated [.github workflows](../../.github/workflows/ci.yml) run `gate`
on pull requests/main/dispatch; [CD](../../.github/workflows/cd.yml) packages
the jar, smokes it on `127.0.0.1` and builds an image. The workflows upload
artifacts and publish nothing. The [wrapper properties](../../gradle/wrapper/gradle-wrapper.properties)
now select **Gradle 9.8.0**, commit `f3e6b0b` on local `main`.

The operator's [AC-13 record](slices/05-ci-cd/PROOF.md#ac-13-first-github-runs-recorded-by-the-operator-2026-10-03)
reports these earlier successful runs; this draft does not independently
re-run them or attribute them to the eventual release SHA:

| Run | Recorded commit and result |
|---|---|
| [PR CI 37153245436](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153245436) | `2e33568`, success, Gradle 9.7.1; `gate-reports`. |
| [Main CI 37153380482](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380482) | `a3d6867`, success, Gradle 9.8.0; `gate-reports`. |
| [Main CD 37153380418](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380418) | `a3d6867`, success; jar, loopback smoke and image-build steps; `urlshort-jar`/`smoke-logs`. |

The initial main runs on `ecf8dfd` were cancelled when `a3d6867` arrived.
AC-13's attributed judgment remains QA's responsibility; later final-SHA
runs and any missing/red runs must be reconciled in §2 and §7.

## 6. Metrics and bench

**release_prep, on the final SHA**: export evidence, refresh
`tools/sdlc-metrics.mjs`, and explain the derived retries, rollback frequency,
MTTR and end-to-end latency with their limits. Cite the generated evidence
index against [GOVERNANCE](../../docs/GOVERNANCE.md).

**Bench — release_prep, on the final SHA**: record the actual offered rates,
latencies, host and scope of the installed-artifact measurements. Earlier
slice or mission measurements do not establish this release's performance.

## 7. Known gaps so far

Snapshot of the mission-02 entries and assigned schema debt in
[GAPS](../../docs/qa/GAPS.md). Historical checks retain their candidate scope.
The QA gap list and proof judgments are not edited by this draft.

| Subject | Gap/qualification and disposition at this snapshot |
|---|---|
| `click`, `user_agent_class` audit columns | Four required audit columns per table are missing on shipped V1/V2. V3 is assigned to `02-click-retention`; **open until its merge**. |
| `link`, `audit_log` audit columns | `link` lacks update/actor columns; `audit_log` lacks row-audit timestamps/actors. Proposed V4 in `04-audit-columns`; **open until its merge**, plan-lock still pending. Audit events remain append-only. |
| Audit-read per-suite coverage | Historical `35590f0`: unit 436/492 lines, 184/190 branches; functional 455/492, 158/190. Merged 492/492 and 190/190; per-suite misses informational, no waiver. Final-candidate coverage pending. |
| Audit-read AC-17 | Unchanged shipped suite has exactly two API-enumeration failures (153/155 pass). Explicit grant `428e9e1`, transition 1156; behavioral compatibility evidence is qualified accordingly. |
| Audit-read peer checks | Remote/IPv6/mapped peers use disclosed Servlet inputs; real servers bind `127.0.0.1`. No actual remote TCP/IPv6 penetration claim. Headerless local relay remains an operator boundary. |
| Audit-read concurrency/store faults | Held JDBC transaction and induced SELECT failure prove the selected effects; no natural hardware failure, cross-process concurrency or crash-recovery claim. |
| Audit-read item 12 | Security review follows QA. The independent security record and its proof judgment remain required before acceptance. |
| **QA-AUD-01 / CR-01 HIGH** | On `35590f0`, either remoteip header setting bypassed the NONE-only guard (forwarded GET/HEAD `200` instead of `403`). Original QA PASS was superseded. Design corrected/re-locked; builder candidate `7ac8af5` awaits QA/re-review. **Unaccepted until those checks close it**, no waiver. [Findings](../../docs/qa/01-audit-read/findings.md), [review](../../docs/review/01-audit-read/01-code-review.md). |
| CI/CD per-suite coverage | Historical `add7ab5`: unit 400/443 lines, 162/162 branches; functional 408/443, 131/162. Merged 443/443 and 162/162; no exclusion/waiver. |
| CI/CD AC-13 | GAPS still carries the earlier pending hosted-run/action/cache/wrapper/Dependabot observation row. The later operator PROOF record supplies the runs in §5 and the Gradle dependency bump. **Reconcile with QA**, including final-SHA evidence; this draft does not declare the whole row closed. |
| CI/CD failing-run behavior | Deliberately red GitHub run remains unobserved; upload-always, exit propagation and 30-day retention were checked as configuration under SPEC A-6. Branch protection also remains unobserved in the operator record. |
| CI/CD lint | QA used YAML 1.2 parsing/source checks; it did not rerun the builder's actionlint 1.7.12 container/negative control. Allowed by AC-12. |
| CI/CD image | First local wholly uncached build stalled. Later exact-candidate `--pull` build completed with some cached layers. The operator records a later hosted-runner build with no layer cache; keep both scopes, do not erase the failed attempt. |
| CI/CD runner environment | Local jar smoke uses JDK 21; earlier QA did not execute hosted JAVA_HOME/shell behavior. Later operator CD evidence is for `a3d6867`, not a final release candidate. |
| CI/CD traceability | Workflow requirements use configured/local/run evidence; inherited product tests unchanged. This is the SPEC's evidence strategy, not new product HTTP coverage. |

No QA gap entries exist yet for the other in-flight mission-02 candidates in
this snapshot; their final QA and release evidence remains pending. At
release prep reconcile **all** then-current GAPS/review/advisory rows, including
inherited mission-01 limits such as the Mac/Lima restart path, isolated added
click-cost measurement and smoke-host prerequisites. Keep the single-node H2
operational limit explicit.

## 8. Rollback — draft procedure

Source: [retention design `f044cbe`](slices/02-click-retention/design.md), V3's
header on `slice/02-click-retention`, and
[proposed V4 header](slices/04-audit-columns/design-probe/migration/V4__add_link_audit_columns.sql)
from design `5a6d168`. **V4 remains pending its plan-lock**; confirm the merged
file and version before using this recipe. These are written rollback steps,
not a rehearsal of the eventual release.

1. Stop the service and its writers. Copy the complete stopped `data/`
   directory to a fresh backup location **before upgrade, lowering retention,
   or schema rollback**. For Compose, back up the actual named volume mounted
   at `/app/data`; a separate host `data/` directory is not that volume.
   Preserve the matching prior artifact/configuration and JDBC file path.
2. Record the applied Flyway versions. With the app stopped, open that same
   database with the matching H2 tooling. If V4 was applied, run its header's
   rollback **first**, before V3. If only V3 was applied, skip V4. Preserve the
   backup through every DDL step; this draft makes no atomic-DDL claim.
3. V4, **proposed**, removes only its added columns and history entry:

   ```sql
   ALTER TABLE link DROP COLUMN updated_by;
   ALTER TABLE link DROP COLUMN created_by;
   ALTER TABLE link DROP COLUMN updated_at;
   ALTER TABLE audit_log DROP COLUMN updated_by;
   ALTER TABLE audit_log DROP COLUMN created_by;
   ALTER TABLE audit_log DROP COLUMN updated_at;
   ALTER TABLE audit_log DROP COLUMN created_at;
   DELETE FROM "flyway_schema_history" WHERE "version" = '4';
   ```

4. V3 removes only its added columns and history entry:

   ```sql
   ALTER TABLE click DROP COLUMN updated_by;
   ALTER TABLE click DROP COLUMN created_by;
   ALTER TABLE click DROP COLUMN updated_at;
   ALTER TABLE click DROP COLUMN created_at;
   ALTER TABLE user_agent_class DROP COLUMN updated_by;
   ALTER TABLE user_agent_class DROP COLUMN created_by;
   ALTER TABLE user_agent_class DROP COLUMN updated_at;
   ALTER TABLE user_agent_class DROP COLUMN created_at;
   DELETE FROM "flyway_schema_history" WHERE "version" = '3';
   ```

5. Deploy the matching prior jar/image and configuration while still stopped;
   a current artifact containing V3/V4 would apply the forgotten migrations
   again. For a source rollback, **release_prep, on the final SHA** must fill
   in the merge SHAs, reverse dependency order and exact image identifiers,
   retain `127.0.0.1` port publishing, and rehearse the recipe on a throwaway
   worktree/database. Do not revert shared `main` as a rehearsal.
6. Verify the expected schema/history and retained domain values, then the
   matching artifact's gate, health, create/read/redirect/statistics/errors
   and loopback smoke. Verify absence of the reverted operator features and
   restart persistence. Record the checks and process/container teardown at
   release prep; no such verification is asserted here.

Dropping these columns discards the added row-audit metadata, while the older
domain columns/rows remain. **Purged clicks are already gone**: reducing
retention back, disabling purge, reverting code or dropping columns does not
recreate them. Recovery requires a pre-deletion data backup and loses writes
after that backup. Keep purge held during recovery where the selected artifact
supports the setting. V1/V2 are retained by this draft rollback.

## 9. Self-check

- Read the release contract and CI/CD §5, current GAPS, system design,
  V3 branch header, proposed V4 header and operator AC-13 record; claims above
  identify the source and its candidate scope.
- Recorded the schema/data-loss steps and pending V4 plan-lock; copied the
  mission-02 gap qualifications without closing QA's rows or judgments.
- Checked all 42 local links/anchors across both drafts and compared both
  SQL rollback blocks statement-for-statement with the source headers;
  all checks passed. `git diff --check` found no whitespace errors.
- Read live traces and routing packet transitions for the accompanying
  [scenario draft](../../docs/scenarios/drills.md). Natural review failure is
  distinct from the still-pending QA-rejection drill.
- Final SHA, gate, installed smoke, OSV, bench, metrics and rollback rehearsal
  remain release-prep work. This documentation packet changes only the two
  authorized Markdown files; it performs no push, tag or publication.
