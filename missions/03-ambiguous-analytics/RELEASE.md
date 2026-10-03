# DRAFT, not a release record

Mission `03-ambiguous-analytics` — release groundwork, 2026-10-03.
Assignment: `qitem-20261003211441-9576d047`; release owner:
`release2-agent@urlshort-factory` (D18, [mission notes](NOTES.md)).

This draft records the decided contract and preparation still owed. It names
no release candidate and makes no build, coverage, installed-artifact,
advisory, latency or readiness claim. `01-analytics-v2` is in implementation
on a stack based on `slice/02-click-retention`; its final rebased candidate
and independent checks are still owed. The eventual `release_prep` packet
follows wave review and replaces these placeholders with final-SHA evidence.

## 1. Decision brief for ship sign-off

**Draft outcome.** An Analyst will read unique visitors and bot clicks per
UTC day beside the existing statistics. The existing raw figures retain
their meaning. Click identity will follow the rate limiter's trusted-proxy
setting, and the Operator will get recorded/lost click counters. This is
the intended behavior in the [slice SPEC](slices/01-analytics-v2/SPEC.md#decision-recorded-verbatim),
not an assertion that a candidate implements it.

The human resolved `qitem-20261003154347-19e96a75` at
2026-10-03T16:10:30.782Z, transition **876**, as `human@kernel`:

> accept all recommended: Q1 B, Q2 B, Q3 B, Q4 A, Q5 A, Q6 A

The transition was read with
`rig queue transitions qitem-20261003154347-19e96a75 --json`.
The [SPEC Decision](slices/01-analytics-v2/SPEC.md#decision-recorded-verbatim)
and [mission notes](NOTES.md) retain the answer and its consequences.

| Question | Chosen | Intended behavior | Unchosen options: not part of this release's scope |
|---|---|---|---|
| Q1: what is counted? | B | Add `uniqueVisitors` per UTC day beside raw clicks. Include every user-agent class. Never combine uniques across days. | A: remain raw-click-only; C: add weekly or monthly uniques. Raw clicks themselves remain. |
| Q2: permitted hash use? | B | Count distinct stored client hashes only within their own UTC day. Never expose/export the hash, join it with other data or compare it across days. Keep the salt in memory and drop it at UTC midnight. | A: use the hash for nothing; C: create a longer-lived identity. No persistent salt or cross-day identity is added. |
| Q3: bots? | B | Add `botClicks` per day. Bots remain in `clicks`, `totalClicks`, `uniqueVisitors` and the unchanged referrer statistics. | A: keep existing figures without a bot figure; C: exclude bots from every figure. Existing figures themselves remain. |
| Q4: retention? | A | Keep mission 02's 90-day delete policy. Figures reflect stored clicks, becoming a rolling window once that purge runs. This mission builds no purge. | B: choose a different retention period; C: roll old clicks into daily counts retained indefinitely. No lifetime totals or old-click aggregation. |
| Q5: reader? | A | Add fields to the existing `GET /api/links/{code}/stats` endpoint. | B: CSV export; C: HTML dashboard; D: cross-link view. No new endpoint, export, report or UI. |
| Q6: day boundary? | A | UTC calendar days, `YYYY-MM-DD`. | B: a time-zone parameter; C: an operator-configured time zone. |

The [SPEC Scope](slices/01-analytics-v2/SPEC.md#scope) also excludes time-range
parameters, changes to top referrers or user-agent classification, changes
to redirect responses/link management/audit, and rewriting stored hashes.
No option requiring an extra slice was chosen.

**Sign-off recommendation and proof readiness:** release_prep, on the final SHA.
No ship decision is requested by this draft. The human retains ship sign-off.

## 2. Artifact and gate

**release_prep, on the final SHA.** Record candidate and merge SHAs, JDK/Gradle
and Docker versions, jar hash, image digest, fresh `scripts/gw --offline check
bootJar` output, test/coverage results and `rig proof show` readiness here.
Quote the candidate's CI and CD run URLs and conclusions under
[CI/CD guidance §5](../../docs/guidance/ci-cd.md#5-release-package). Missing or
red runs belong in Known gaps. No gate result is supplied by this draft.

## 3. Installed smoke

**release_prep, on the final SHA.** Build and run both the jar and image on
loopback, capture `scripts/smoke.sh` health/create/redirect/stats/error
journeys and structured logs, verify the new statistics fields and scrape
counters, and record teardown. Link evidence for restart/shutdown and name
what the smoke did not exercise. Request installed-artifact dogfood from QA
under the release packet. No artifact is started or smoked for this draft.

## 4. Dependency advisories (OSV)

**release_prep, on the final SHA.** Resolve the final runtime dependencies,
run the network advisory lookup through `tools/dep-advisories.mjs`, and
record affected coordinates, severity, fixes, reachability and routed
remediation/deadlines. No advisory lookup or security verdict is claimed.

## 5. Evidence per slice

| Slice | Existing contract and planning evidence | Evidence still owed on the final candidate |
|---|---|---|
| `01-analytics-v2` | [SPEC](slices/01-analytics-v2/SPEC.md), [design](slices/01-analytics-v2/design.md), [impact analysis](slices/01-analytics-v2/impact-analysis.md), [requirements review](../../docs/review/01-analytics-v2/requirements-review.md), [design review](../../docs/review/01-analytics-v2/design-review.md) | Final [PROOF](slices/01-analytics-v2/PROOF.md), QA captures and coverage, traceability/gaps, code/security review, merged candidate and installed evidence. The current PROOF is interim builder testimony. |

Relevant shared records: [review ledger](../../docs/review/REVIEW-LEDGER.md),
[traceability](../../docs/qa/TRACEABILITY.md), [gaps](../../docs/qa/GAPS.md),
and [governance](../../docs/GOVERNANCE.md). Reconcile the final export's
`docs/evidence/03-ambiguous-analytics/INDEX.md` against governance at
`release_prep`, then refresh it at `evidence_export`. The export and final
coverage directories are pending; this draft does not link invented files.

## 6. Metrics and redirect benchmark

**Benchmark: release_prep, on the final SHA.** NFR-L1 applies because client
identity changes on the redirect path. Measure redirect **p95 ≤ 20 ms and
p99 ≤ 50 ms** at the specified load using the release bench
(`scripts/smoke.sh --bench` per the SPEC), record offered/achieved rate,
errors, environment and raw observations, or retain the gap. AC-15's
slow/failing-store and concurrency checks do not establish these numbers.
Earlier mission-01 measurements do not prove this candidate's performance.

**Factory metrics: release_prep, on the final SHA.** Run the evidence exporter
and `node tools/sdlc-metrics.mjs`; read the resulting counts, retry/rollback
frequency, MTTR and latency with their derivation limits. This draft has no
metric values. No latency target is specified for the statistics read.

## 7. Known gaps so far

Sources are the [SPEC rules, non-functional requirements and proof contract](slices/01-analytics-v2/SPEC.md),
the [design](slices/01-analytics-v2/design.md), and the shared
[GAPS.md](../../docs/qa/GAPS.md) read during this draft. This is an interim
inventory. Final release prep must re-read every gap and review residue,
preserve disclosed historical limits, and reconcile subsequent fixes and
QA judgments against the final SHA.

| Gap or limit | Meaning and follow-up | Owner |
|---|---|---|
| NFR-L1 p95/p99 unmeasured for this mission | Until the final release bench, no numeric redirect-performance acceptance. The SPEC requires a `01-analytics-v2` GAPS row; the shared file read for this draft has no such row yet. | QA records the row; release2 measures/reports at release prep. |
| Restart upper bound on daily uniques (rule 3, A-8) | A fresh in-memory salt after a same-day restart can count a returning visitor twice that day. Accept the upper bound implied by Q2 B; no persistent identity/salt. This restart effect is separate from old proxy hashes below. | Release2 documents; Operator accounts for restarts. |
| Old proxy hashes cannot be separated (rule 6) | Previously recorded hashes behind a configured proxy are not rewritten or backfilled. Historical visitors collapsed into a proxy hash cannot be recovered by the new identity rule. | Release2 documents; Operator interprets historical figures. |
| Bot classification (rule 4) | The existing classifier is unchanged; a bot presenting as a browser is counted as a browser. All classes remain in unique counts. | Release2 documents; Analyst interprets `botClicks`. |
| Final candidate evidence pending | QA, merged coverage, final OpenAPI equality, code/security records, installed smoke, OSV and benchmark still belong to the candidate workflow and release prep. No interim builder result is promoted to a release verdict. | Builder, QA/review and release2 under their packets. |
| Audit-column debt inherited from mission 02 | GAPS lists missing row-audit columns on `click`, `user_agent_class`, `link` and `audit_log`, assigned to `02-click-retention`/`04-audit-columns`. Reconcile their merges and verification; this mission adds no migration. | Mission-02 builders/QA and orchestration lead. |
| Audit-read access defect in the shared gap record | QA-AUD-01 HIGH on `35590f0`: Tomcat remoteip overrides can rewrite a peer and expose audit content. The shared record does not yet establish closure; final prep must find the independent remediation/review record. No waiver or fix is claimed here. | Mission-02 builder/QA/review and orchestration lead. |
| Hosted CI/CD and failure-run limits | `05-ci-cd` GAPS leaves the first hosted PR gate/main CD, uploads and hosted environment unobserved until the human pushes. Failure uploads are configuration evidence; a fully uncached image build is also unobserved. Local parsing/builds do not prove these hosted effects. | Human/operator records runs; release2 reports final-SHA status. |
| Accepted Mac-to-VM restart gap | Mission-01 GAPS preserves failed held-R0 responses through the Mac published-port path. Human transition 1011 amended the measurement to direct paths (A-19); native Linux was not tested. Keep that boundary when reporting new installed evidence. | Release2, using the accepted contract and actual host. |
| Inherited operational/instrument limits | Single node/H2 file database; backward Clock/fail-closed wait and arbitrary stalled-sweep limits; anonymous disk-gauge working-directory path; Perl/Time::HiRes for shutdown/restart probes. Controlled clock/Servlet peers/store faults, informational per-suite coverage misses and allowed API-shape/enumeration changes remain qualified evidence, not real remote-traffic, natural-outage or all-original-assertions claims. Mission-01 GET-minus-HEAD p95 is a comparison proxy, not an isolated added-cost quantile. | Release2 preserves the relevant GAPS qualifications; Operator follows deployment/probe requirements. |

### Operator-doc deltas to apply at release prep

Update the operator/API documentation and README examples on the final
candidate; this assignment edits only this draft.

- Each `clicksPerDay` element will have `date`, `clicks`, **`uniqueVisitors`**
  and **`botClicks`**. The four top-level fields remain `code`, `totalClicks`,
  `clicksPerDay`, `topReferrers`; a never-opened link keeps empty arrays and
  zero total. Explain UTC-only uniques, restart double-counting, bots included,
  and rolling retention after mission 02's purge.
- Add `urlshort.clicks.recorded` and `urlshort.clicks.lost` on Actuator metrics;
  Prometheus names are `urlshort_clicks_recorded_total` and
  `urlshort_clicks_lost_total{reason="…"}`. `lost` has only static `reason`
  values matching `click lost`: `rejected`, `reduction failed`, `write failed`,
  `shutdown deadline`, `shutdown deadline, outcome unknown`. No client/link
  tags. Under an unknown shutdown outcome a later successful write can also
  increment `recorded`; these counters are not necessarily disjoint (design §1).
- Click identity will use **`urlshort.rate-limit.trusted-proxies`**, the same
  setting and rule as the limiter. With no listed proxy, use the peer and
  ignore forwarding headers. With a listed proxy, use the right-most untrusted
  `X-Forwarded-For` entry, falling back to the peer when none exists. Do not use
  `Forwarded`, `X-Real-IP` or other headers; trust only proxies that append
  correctly. Explain that previously stored hashes are not rewritten.

## 8. Rollback

The [design](slices/01-analytics-v2/design.md#3-data-model-and-queries) specifies
**no schema migration** for this mission. Reverting its eventual merge
restores v1's statistics shape and click identity behavior and removes the
new counters. It leaves mission 02's schema/retention changes in place.
Stored click hashes are not rewritten by this slice (SPEC rule 6), and a
code revert neither separates old proxy hashes nor restores purged clicks.

Draft sequence for the orchestration lead/Operator:

1. Identify this mission's actual `--no-ff` merge and preserve the service's
   H2 data while stopped. The merge SHA and backup path are pending.
2. Rehearse `git revert -m 1 <mission-03-merge-sha>` on a throwaway branch,
   retaining the mission-02 base. The placeholder is not executable evidence.
3. Run `scripts/gw --offline check bootJar`, rebuild the image, and restart
   the selected artifact on loopback using the retained data volume.
4. Run installed smoke and verify v1's per-day element has only `date` and
   `clicks`, with existing links/targets, totals/referrers and health still
   working. Confirm the mission-03 counters are absent and inspect logs.

**Rehearsal, literal commands with resolved identifiers, previous artifact
identifiers and post-rollback evidence: release_prep, on the final SHA.**
No revert, deployment or data change was performed for this draft.

## 9. Self-check (draft only)

- Read the human transition 876 and matched its exact answer to all six SPEC
  choices. Every alternative is listed, with retained raw figures distinguished
  from the unchosen raw-only options.
- Checked the SPEC's rules 3–7/10, scope and proof contract against the draft;
  read GAPS and preserved pending defects and inherited limits without claiming
  they are fixed. Final-SHA reconciliation remains explicit.
- Local evidence links are checked before committing. Pending evidence has
  named placeholders rather than links to nonexistent files.
- Rollback is described step by step and has no mission-03 migration reversal;
  its rehearsal and concrete merge/artifact identifiers remain pending.
- Edited only this `RELEASE.md`. No candidate was judged, built, smoked or
  benchmarked for this assignment. Nothing was pushed, tagged or published.
