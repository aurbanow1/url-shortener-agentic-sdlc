# Mission 01 release review

**Latest verdict: PASS for human ship-signoff handoff on `973bc1a`, with final
GAPS disclosure `367567e` and the explicit AC-28 exception.** RR-01/02 and the
in-passing RR-03 are fixed. Current proof is 39 accepted / one rejected: AC-28
remains unmet for the human to decide. This is not ship approval. The original
failed review is preserved.

Package: `40067fc` (release commits `34308c9`, `52c38b3`, `40067fc`). Application/build/container inputs: `8e9c065589e53385f60d6be3ddbc3683260285df`, built at `f090103`. Reviewer: review-agent@urlshort-factory (Codex), 2026-10-03 UTC. Packet: `qitem-20261003153114-04cfbe0a`, instance `01M3ZXEXAMS945ZS0QZ26KZK1V`.

**Verdict: FAIL — one HIGH rollback finding, one MEDIUM reporting finding.** Mission-level rework is routed to the release agent in `qitem-20261003154417-0f04cc71` while this packet waits. The unmet AC-28 host-path criterion remains a separate, explicitly recorded human decision; it is not converted to a pass by this review.

## Context and coverage

This is the release package for the integrated create/redirect, privacy-reduced analytics and operations service already reviewed at the wave boundary. It adds installed-artifact evidence and changes the smoke/benchmark tools; it changes no Java, migration, build or container input. Confidence is high in the package audit and the reproduced findings. I did not independently repeat the long load run or start the container; I checked their complete recorded results, tool source and logs. The host-forwarder explanation is an inference supported by the two control paths, not a packet-level observation.

Read all nine authored release inputs (README, TESTING, GAPS, drills, metrics README/JSON, RELEASE, smoke and bench) and all 32 release evidence files. Large logs were parsed in full for levels, completion statuses and shutdown outcomes. The 199 engine exports were parsed and used to reproduce the metrics; their old judgments were not re-adjudicated. [File inventory](release-review-file-inventory.json) gives each path its scope, hash and disposition: 240 release-package paths, plus four surrounding context paths. Relevant mission notes and the live lead decisions were read separately. Prior code/security verdicts remain settled.

The product identity check `git diff --name-only 8e9c065 40067fc -- src build.gradle.kts settings.gradle.kts Dockerfile compose.yaml gradle gradlew .dockerignore` is empty. Smoke and bench changed after the build and were reviewed as such; the release's chronology correction is explicit. No product or producer document was edited by this reviewer.

## Release checklist (§9)

| Item | Result and evidence |
|---|---|
| 1. Claims traced to exact evidence | Application identity matches. The jar is 39,613,260 bytes, Git blob `1aaf1fc92b909e220fcefebc1c547611685ff891`, SHA256 `61d9ce3417be75abdcac463f2171409905ceb6be6ef0f4d988378c9d11ff9377`, matching both the package and my earlier wave journey. Every release Markdown link resolves. Restart counts need RR-02 correction. |
| 2. Gate, coverage, proof readiness | Fresh reviewer command `scripts/gw --log docs/review/01-greenfield-core/release-review-check-40067fc.txt --offline check --rerun-tasks` passed: 14/14 tasks executed, 165 unit + 155 functional, zero failures/errors/skips, 442/442 distinct source lines and 162/162 branches, Javadoc green. Proof initially has three pending/five unknown items; existing QA/QA2 judgment items own them. Re-review must read their receipts before handoff, with only the explicit AC-28 exception below. |
| 3. Installed jar and container | Recorded jar smoke, environment overrides and three direct drains agree with the source assertions and logs. Container build/inspection show image `a38050b05d90`, uid 10001, read-only root, data volume, tmpfs, loopback binding, readiness health check and stop timeout 20 s. Recorded restart/down-up preserves links. Host-path R0 fails four times, VM published-port control passes twice; one namespace control's output/log is retained, the other is explicitly only an author's observation after its log was replaced. AC-28 remains NOT MET. |
| 4. Advisories and secrets | Recorded OSV report has exactly 97 dependencies and 97 raw responses with no vulnerabilities; independently parsed runtime coordinates match all 97. This confirms what the saved query returned at 14:42Z, not a new network scan. Secret/environment proof remains with QA's existing release judgment; no fabricated acceptance. |
| 5. Gaps and review ledger | W2-01–05 and operational/benchmark limits are carried. Locale pin in `now_ms` addresses W2-04's inherited locale, retaining its declared Perl prerequisite. No new coverage exclusion. AC-28 stays unmet, and the corrected connection totals must flow into the narrative/GAPS. |
| 6. Rollback | Rehearsal commit `e6f062a` and its successful gate exist, with V1/V2 preservation and data-loss limits described. **FAIL RR-01:** the recommended container rebuild would use the reverted all-interface binding. A successful build alone does not prove a safe operational rollback. |
| 7. Metrics | Re-ran unchanged `tools/sdlc-metrics.mjs` into a temporary output directory with `Date.now` fixed to the stored generation instant. JSON is exactly equal to the committed metrics. The release explains that rollback-note counts are not actual Git reverts; no `main` revert was claimed. |
| 8. Decision brief and publication | Default/alternative and the AC-28 decision are explicit. No Git remote is configured; tags are only the four accepted-slice tags. Build logs show a local image tag, which is not a published release. No publish command or new release tag was issued by this reviewer; the record shows local build/run/cleanup. |

[Artifact audit](release-review-artifact-audit.json) records hashes, full-log aggregates, jar identity, test/coverage counts and link validation. The 19,237-event benchmark log has 1,201 create completions (setup plus 1,200 measured), 18,000 redirects (GET and HEAD), four other successful reads, three known startup WARNs and no ERROR. Statistics show exactly 12,000 clicks. The benchmark source schedules independently of response completion and measures from scheduled due time; the reported offered-rate and GET-minus-HEAD comparison are supported within the stated single-run, same-host, jar-only limits. I do not turn that comparison into an exhaustive attribution or container-performance claim.

## Findings

| Id | Severity | File:line | Evidence and consequence | Required change |
|---|---|---|---|---|
| RR-01 | HIGH | `missions/01-greenfield-core/RELEASE.md:477`, `:502`; reverted `compose.yaml:6` | The instructed revert removes the loopback restriction, and the artifact recipe then runs `docker compose up -d --build`. `git show e6f062a:compose.yaml` gives `8080:8080`; rendering that exact file with Compose confirms a published port with no `host_ip`. An operator following this rollback exposes the unauthenticated service beyond the release's loopback boundary. [Independent config capture](release-review-rollback-config.json). No service was started for this repro. | Provide exact rollback/build/run commands that preserve the loopback binding and existing data volume. Rehearse that actual operational recipe on a disposable copy/volume, capture the binding and continued access to a pre-existing link, and document smoke expectations on the rolled-back feature set. Do not change the shipped application just to repair a runbook. |
| RR-02 | MEDIUM | `scripts/smoke.sh:159`; `missions/01-greenfield-core/RELEASE.md:175` | `load_loop` appends curl's `000` and then a fallback `000` for one failed request. My controlled peer closes without a response; one actual invocation writes two rows. Thus the run-3/run-4 figures of 250/251 requests and 204/206 failures count rows. The producer independently confirmed 148 attempts each, 102/103 failures. This changes the reported scale, not the already-failing R0 result. [Control output](release-review-tool-controls.json). | Record exactly one result per attempt; correct or explicitly relabel historical row counts, retaining raw originals. Capture a corrected restart result and update dependent summaries/GAPS. Expected to be fixed in passing with RR-01. |

The probe is [release-review-tool-probe.py](release-review-tool-probe.py). Benchmark controls against two complete 200s and two complete 500s report the correct bad-response counts; a truncated response exits 13 with unsettled-await diagnostics and no passing report. A zero exit alone is not a benchmark verdict: the human/QA judge must read bad counts and latency, as this package does. Those controls do not establish a separate release-blocking defect. The first probe attempt was denied a sandbox bind; an authorized retry produced these observations. A discarded connection-control setup timed out and was replaced with the explicit empty-response peer; no finding rests on that setup failure.

## Proof readiness and the AC-28 exception

Initial live proof: `01-create-redirect` #14 pending; `02-analytics` #1/#4/#5/#10 unknown and #13 pending; `03-operate` #5 unknown and #13 pending. They are already assigned in `qitem-20261003153201-3bc62f5f` (QA) and `qitem-20261003153203-9be9445d` (QA2); no duplicate judgment work is created here.

The lead recorded the exception on this review packet, transition 825: wait for both judges; any remaining not-ready item is an ordinary §9.2 HIGH **except 03-operate #13 if rejected for AC-28's host path**. That one may go to the human ship gate explicitly unmet. A human acceptance then becomes a recorded SPEC decision, QA2 re-judges it, and only afterwards may a delivery stamp be recorded. A hold waits for the additional host evidence. This is an explicit lead disposition, not an inference from the absence of a rule. The reviewer grants no waiver and does not approve shipping.

## Ponytail review

No new complexity finding. The open-loop driver uses Node's HTTP client and avoids a new dependency. The fixes need a precise operational recipe and one status record per attempt, not new application layers.

## Self-check and continuation

Exact package and application identity verified; fresh offline gate and independent artifact/log/metrics audit complete; both findings have reproduced evidence and producer acknowledgment. No product, tests, SPEC or design edits. Append the ledger, commit only these review artifacts, route producer rework and park this packet on it. Re-review RR-01/RR-02 and their changed evidence at the returned SHA, read both existing QA receipts, and preserve the lead's single AC-28 human exception. No clean release handoff is made in this pass.

## Re-review 973bc1a

Rework `qitem-20261003154417-0f04cc71` closed at `ad83fb6`. Included handed-off
follow-ups `67d168f` (completed dogfood record) and `973bc1a5e7b4464acdf7d93834839d064acd2e46`
(loss-counter correction), plus QA's final GAPS clarification `367567e`.
Same application `8e9c065`; 2026-10-03 UTC, same reviewer and release packet.

Read all **20 changed package paths** since the original review: four authored
inputs (RELEASE, GAPS, drills and smoke), plus 16 new evidence files. Both new
restart logs and the rollback log were parsed in full. Also read the completed
dogfood report, the final QA GAPS amendment and relevant queue/NOTES updates.
[Per-file audit](release-rereview-audit-973bc1a.json) records hashes, log counts,
input identity, volume/binding assertions, link-body equality and link checks.
No new Java, build, migration or container input differs from the original
release. The original fresh 165/155 offline gate therefore remains applicable;
this correction needed shell syntax and focused tool controls, both checked.

| Finding | Resolution | Evidence |
|---|---|---|
| RR-01 HIGH | Fixed | RELEASE §8 now pins `127.0.0.1:8080:8080` before starting the reverted compose stack and verifies the binding. Safe/unsafe rendered configs differ at the host binding and retain the identical named volume. Commit `8104e05` differs from the previously gated rollback `e6f062a` in container/application inputs only by this pin. Producer build/up/log/smoke captures show the rolled-back application operating; three captured bodies for `sgjjgNZ4` are byte-identical before rollback, after rollback and after roll-forward. Smoke completes the retained journey and stops at the expected removed rate-limit metric. The first failed image download and untested earlier migration rollbacks/volume backup remain disclosed. |
| RR-02 MEDIUM | Fixed | The actual revised `load_loop` makes one request and writes one row for an empty reply. Historical run-3/4 rows are relabelled as 148 attempts with 102/103 failures; raw originals remain. Run 5 records 149 attempts/105 failures. No change to the failing R0 verdict. |
| RR-03 MEDIUM, introduced by RR-02 fix | Fixed in passing at `973bc1a` | At `ad83fb6`, `scripts/smoke.sh:163` discarded curl's failure after an HTTP status arrived. A controlled peer sent status 200 with Content-Length 20 and one byte, then closed: the actual loop recorded a successful-looking `200`. The correction records `cut200`, which the existing bad-response classifier rejects. Fresh actual-function controls yield exactly one row/request: empty=`000`, complete=`302`, truncated=`cut200`. No application change. |

The probe and both before/after controls are
[release-count-probe.py](release-count-probe.py),
[ad83fb6](release-count-controls-ad83fb6.json) and
[973bc1a](release-count-controls-973bc1a.json). The first bind was denied by the
sandbox; an authorized localhost-only retry supplied the actual observations.
No result is inferred from the denied attempt. `bash -n scripts/smoke.sh` passes.

Run 6 records 149 attempts, zero responses outside 2xx/3xx or cut short, 103
connection failures and R0 lost at 10,514 ms. **AC-28 remains NOT MET, six of six
host-path runs.** The recorded rebuilt image differs in id, and RELEASE §2 says
so; unchanged source inputs are verified, not byte identity of the images.
QA2's original receipt covers runs 1–5. GAPS retains that historical scope;
the added run does not weaken the rejection. The VM/namespace controls support
path isolation, without proving the precise forwarding mechanism or a native
Linux-host result. Only one namespace control retains its evidence.

Dogfood's completed installed-jar report confirms the already-recorded W2-01
MEDIUM and W2-03 LOW, with no additional severe finding. Those remain routed
to mission 02. No new complexity finding and no unhandled review residue.

The final GAPS amendment correctly limits NFR-L3: sequential GET/HEAD p95
comparison does not establish an isolated added-cost quantile. The original
SPEC explicitly allows this measurement limitation with disclosure and the
slow-store compensating check. This review retains that limitation rather
than certifying an isolated ≤2 ms contribution. It is separate from AC-28's
human exception.

### Receipt continuation

QA2's closed `qitem-20261003153203-9be9445d` records receipt 27 rejecting
03-operate #13 for AC-28 and receipt 26 accepting its GAPS record. Final
GAPS `367567e` requires the already-routed refresh
`qitem-20261003164452-5a8a64eb`. QA's six remaining receipts belong to
`qitem-20261003153201-3bc62f5f`. Do not hand off until those are current and
the live proof has no not-ready item beyond 03-operate #13. If either adds a
different rejected/not-clear item, route it as ordinary HIGH rework. The
lead's transition-825 exception is retained unchanged; no waiver or delivery
stamp is issued by this reviewer.

### Final receipt verification

Live proof checked at 16:49Z: **01-create-redirect 14/14 accepted; 02-analytics
13/13 accepted; 03-operate 12 accepted and #13 rejected.** No pending, unknown
or not-clear item remains. QA receipts: create sequence 14; analytics sequences
25–29. QA2 sequence 28 reaffirms GAPS at `367567e`; sequence 27's AC-28 rejection
remains current. Independently checked all 36 evidence references on those seven
new receipts against their recorded SHA256s. Read QA's release judgment and
its explicit measurement limits; do not replace either judge's conclusion.
The [proof snapshot](release-final-proof-973bc1a.json) records every current item
and receipt. No Git remote and only the four previously accepted-slice tags
are present; no push, release tag or publication was performed.

**Final review verdict: PASS for handoff with the one authorized human exception.**
All review findings are fixed. The human receives the unchanged AC-28 failure,
the limitation on isolated NFR-L3 attribution, carried MEDIUM/LOW backlog and
the explicit unrehearsed rollback/backup limits. Per the lead's recorded
disposition, accepting the AC-28 gap still needs a written human decision,
the corresponding SPEC decision and QA2 re-judgment before any delivery stamp.
Holding it instead requires the additional host evidence. The review supplies
neither decision on the human's behalf.
