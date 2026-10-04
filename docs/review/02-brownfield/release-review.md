# Mission 02 release review — PASS for human sign-off

Final package **ad0c01f6b517340ad3fb5a64fcc143c078196232**, initial package
**c9aedabd**; product **30f8de4e647b05ff54cde09f1019ae519b00069d**.
The [formal review below](#formal-release_review--c9aedabd) supersedes the
draft outcome. No MUST-FIX/HIGH remains; all three pre-read corrections and
the one new LOW formatting finding are fixed. This verdict hands the package
to the human's local-use decision; it does not approve shipping or publication.

## Historical pre-read at e425469e

**Draft outcome: pre-read complete; no release verdict or ship approval.**
This is plain assignment `qitem-20261004041721-d7a24707`, requested in parallel
with release prep. It is not the lifecycle `release_review` step. The ledger
is deliberately unchanged under that assignment. Reviewer:
`review-agent@urlshort-factory` (Codex), 2026-10-04.

## Context and pins

Mission 02 adds audit reading, retention, error/documentation corrections,
row audit columns, CI/CD and the behavior-preserving client-identity refactor.
The release must preserve the loopback boundary, explain irreversible data
loss and distinguish historical evidence from the final artifact. Confidence
is high in the historical checks below; final-build and readiness claims
are outside this pre-read.

The historical package is `47e3a07`, its metrics qualification is `8d3c536`,
and its product is `e227acf04e1ea902406532fcebd741f8a86f7f65`.
`RELEASE.md` at entry remains that explicitly provisional pre-run. Current
context/drills/GAPS were read through `e425469e`; the README CI/CD amendment
is pinned to `30f8de4e`. Neither a later chat report nor a producer's own
audit is substituted for independent evidence. The final product nominated
in the assignment is `30f8de4e`; it has not been accepted by this draft.

Reproduce the passive checks with
`python3 docs/review/02-brownfield/proof/release-preread.py`.
The [result](proof/release-preread.json) records 238 historical input hashes,
actual artifact hashes, CSV comparisons, trace discrepancies and link targets.
It also names its dependence on the retained temporary jars and the newly
prepared rollback archive. This command runs no product/build/network action.

## Coverage and independent observations

| Material read / checked | Result and boundary |
|---|---|
| Historical RELEASE, pre-run README, gate/artifact/source inventories and canonical coverage | Both 134-file inventories match the pinned Git inputs, including the declared Windows-wrapper CRLF normalization. Both retained jar files independently hash to `fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2`, 39,635,446 bytes. Saved gate log is successful; canonical XML independently gives 581/581 lines and 206/206 branches. The 226/250 invocation counts remain the authored gate summary: this directory does not retain the per-test XML. No fresh gate was run for this document-only pre-read. |
| Historical installed-v2 wire JSON, jar JSON log, both Prometheus captures and smoke output | Seven retained jar exchanges join by request id/status. Both stats bodies show exactly 2 clicks, 1 unique and 1 bot; the jar's stored rows independently have one browser/one bot and one hash. Both scrapes show recorded 3 and five zero-valued loss reasons. Recorded 3 includes the initial smoke click; it is not three clicks on the probe link. Four wrong-name metric queries are disclosed and excluded. Full historical container log joins, complete forged-audit/invalid-URL bodies and container HostConfig inspection are not in this drop; see the final-check boundary below. |
| Historical benchmark summary, jar events and rollback click projection | Summary reports combined 100 GET/s + 20 creates/s, zero bad responses, GET p95 3.0 ms/create p95 3.7 ms; standalone GET/HEAD p95 4.0/2.3 ms. Stored benchmark link has 12,000 clicks, plus 1 smoke and 2 probe clicks elsewhere. The log has 18,004 redirects and 1,203 creates, corroborating the workloads. Per-request latency samples are absent: quantiles remain dated tool output, and the 1.7 ms subtraction is a separate-distribution comparison, not an isolated recording-cost quantile. |
| Historical dependencies/advisory response and secret record | Retained OSV response has 97 queries/97 empty result objects, dated 01:06:49Z; no fresh advisory query here. Independently rescanned all pinned tracked blobs with positive controls for each recorded signature: 9,236 text blobs, 497 binary exclusions, no signature hit. This is not entropy/history/untracked/image secret scanning. |
| V4 then V3 rollback SQL, migration headers, four snapshot SQLs, all 28 raw CSVs and reapplied smoke | Statements match the migration headers. Flyway versions go 1–4 → 1–3 → 1–2 → 1–4. Exactly 7 then 8 added columns disappear; all 12 constraints are unchanged; all four legacy data projections are byte-identical through reversal. Reapplication restores the original schema and preserves every pre-existing projected row, adding the expected 1 link/2 audit rows/1 click. Smoke output is green. New row-audit metadata is intentionally lost on reversal. |
| Four drills: candidate files, workflow traces, packet transitions, stop/relaunch snapshots and branch/smoke records | DRILL 1 has the authorized BROKEN → QA failed → READY → QA done sequence, one remediation, three hops, completed/empty frontier. The two separate Codex seats and fallback scope stay explicit. DRILL 2's reverted tree `ef2272e` is independently identical to `ad83fb6`; failed no-store smoke and subsequent successful gate/smoke agree with the injected fault. DRILL 3 strands an unclaimed packet on an exited seat, routes it without advancing a step, then records a running/ready seat. DRILL 4 has one resume, canceled redrive and final aborted/empty frontier. Refusal to resume the aborted instance is the lead-attributed receipt, not a new command run by this reviewer. Natural-event DRAFT rows remain dated snapshots, not fresh failure claims. |
| Historical metrics JSON/README, generator source, committed instance traces and queue histories | Discrepancy PR-01 below is reproduced. The checked retries (51), failed closures (17), human wait (31,434 s), completed p50/p95 (20,200/31,241 s) and mean-of-instance MTTR (1,652 s) reconcile. Event-weighted MTTR would be 1,841 s; do not describe the former as that quantity. Rollbacks count text mentions plus resumes, not distinct Git reverts. The export-container caveat at `8d3c536` is necessary: directories are not mission membership. |
| README CI/CD amendment, ci.yml, cd.yml, Dependabot, CI guide and linked AC-13 record | README accurately describes PR/main gates, local jar smoke/image creation without publication, and weekly update proposals. Both workflows also allow manual dispatch. Earlier hosted success is explicitly pinned to other SHAs; API 404 does not prove global absence. Checked 70 local links across README, RELEASE and drills: no missing target or unmatched Markdown anchor. No hosted run was re-queried. |
| Mission-02 GAPS sections, slice review dispositions, wave report and lead's recorded backlog | The qualifications below must survive the final refresh. Settled findings are not reopened. The final attributed proof reaffirmations remain a formal-step check. |

The rollback archive inspected here is
`missions/02-brownfield/release/final-30f8de4e/rollback-raw-csv.tar.gz`,
SHA-256 `f9c180b250c2e136b0361d313f4589acfa0b3cceb1269036e95a27f2f786ee24`.
It was still an uncommitted release-prep addition at inspection. The historical
document pointed only to temporary CSV directories. Its durable inclusion
must be checked in the final package. This pre-read confirms schema reversal
and reapplication; it does not yet validate the producer's new prior-binary
startup or image rollback rehearsal.

## Draft findings and disposition

These are corrections to a provisional package, not a failed lifecycle exit.
All three are expected in passing before the formal handoff, with historical
records preserved. No new MUST-FIX/HIGH product finding arises from this scope.

| ID | Severity | File:line at historical pin | Evidence | Required change / current response |
|---|---|---|---|---|
| PR-01 | MEDIUM | `docs/metrics/README.md:11`, `:14` at `8d3c536` | Saved totals say 230 closures/8 rollback-note or resume hits. Committed trace/transition inputs give 233/9: D21's trace has six closures rather than three; coordination packet `qitem-20261004004828-05d2aab9` contains one rollback mention counted as zero in the report. Exact rows are in the reconciliation JSON. | Preserve/qualify the historical snapshot and publish a reproducible final snapshot with its exact input custody and export-container limitation. Release agent acknowledged at 04:28/04:32Z that the historical generator read a then-uncommitted mission-03 export; final input archival/replay is in progress. Not yet independently accepted. |
| PR-02 | MEDIUM | `missions/02-brownfield/RELEASE.md:160` | The residue row mentions LOW/INFO without naming the retained Gradle download resilience MEDIUM. Wave report line 121 and its final disposition explicitly retain it; the lead's NOTES line 149 also does. | Name the existing MEDIUM and owner/trigger in the final known-gap table; retain ping/V5-test-pin LOWs and the localhost-collision qualification. Release agent accepted this at 04:28Z. No product/toolchain repair requested. |
| PR-03 | LOW | `missions/02-brownfield/release/pre-run-e227acf/rollback-verification.md:32` | The counts 1,203 links/1,204 audit rows/12,003 clicks are data rows **excluding** headers; the text says included. CSV parsing reproduces the counts and expected post-smoke additions. | Correct the final table and explicitly qualify the preserved historical wording. Release agent accepted at 04:28Z. |

## Required carry-forward qualifications

- Audit read: the two inherited API-enumeration failures have a narrow accepted
  compatibility grant, not a behavior waiver. Known Tomcat rewriting triggers
  were fixed/re-reviewed. Simulated remote/IPv6/whitespace peers do not prove a
  physical remote TCP boundary; a headerless local relay remains an operator
  boundary. Future Boot trigger changes need review.
- Retention/audit columns: controlled clocks, actual JDBC failure fixtures,
  stopped H2 copies and authorized V3 test pins are the demonstrated scope.
  No PostgreSQL, large catch-up or SIGTERM-during-purge claim. Reversing code
  does not restore purged clicks or discarded audit metadata. The retention
  scheduling race and D21 polling race have committed fixes; old failures
  remain history.
- Dogfood/metrics: removed disk-path selectors deliberately change to 404;
  a future second disk path needs a non-sensitive distinguishing tag.
  Problem-body parity and the bounded dogfood report do not imply a visual
  Swagger/browser test or unrestricted exploration.
- CI/operations: earlier operator runs are not final-candidate evidence;
  deliberately red hosted runs, hosted artifact inspection and fully uncached
  image builds remain qualified. Retain the wrapper MEDIUM, ping/V5 LOWs,
  localhost-port collision limitation and the single-node H2 boundary.
  Mission-01 AC-28 remains NOT MET on the macOS/Colima host-forwarded path;
  a normal smoke here does not override that recorded human exception.
- D21: preserve the external fixture, class-only parameterized attribution,
  original false comparator and port-collision observations. Final readiness
  comes from current attributed receipts, not old pending or superseded
  accepted/rejected snapshots in GAPS.

## What the real release_review must add

Read the final committed package and independently verify the fresh exact-SHA
gate/coverage, jar and container installed smoke/logs, current OSV, artifact
hashes, `e227acf` → `30f8de4e` range comparison, and proof readiness after the
two assigned QA reaffirmations. Check the refreshed metrics against archived
inputs and resolve PR-01–03 explicitly. Verify the exact merge/image rollback
commands, prior-binary behavior, loopback binding, durable CSV archive and
data-loss explanation.

In particular, bind complete 400/403 bodies, container request-log correlation,
read-only root and loopback publishing to **final** raw captures. The pre-run
image metadata and jar status events alone do not establish those effects.
Release agent's 04:34Z response names `installed-wire.json`,
`audit-forbidden.txt`, `container-inspect.json` and `container-console.jsonl`
as the final evidence; these were not pre-accepted from chat.

## Self-check

The independent passive reconciliation completed successfully, with the
historical metric differences reported rather than suppressed. Read the
claimed surfaces and raw controls above; inspected actual retained jars and
all four rollback stages; checked drill custody and local links. Product,
tests, SPECs, designs and shared ledger were not edited. No new service,
build, dependency query, push, tag or publication was performed. Formal
release judgment and its ledger row remain for the authored workflow packet.

## Formal release_review — c9aedabd

Reviewer: `review-agent@urlshort-factory` (Codex), 2026-10-04 UTC.
Packet `qitem-20261004050142-3fad2270`; instance
`01M40SN34E37K96B38JPG9K41X`. The purpose is a usable local release of all six
mission-02 slices, with existing links preserved and an executable way back.
Confidence is high within the named local/H2 evidence; final hosted CI/CD and
the original AC-28 host path remain unverified/unmet as documented.

This is a separate-author review of release-agent's package. Both seats use
Codex under the recorded D17 runtime arrangement; it is not a different-model
family review. I read the final claims against raw evidence, accepted slice
reviews, current QA reaffirmations and the final wave review. Source/build/
runtime inputs at this product pin equal reviewed `e40b095`; the ten-path
pre-run delta was already fully covered by the two wave vantages. No product
delta was introduced by release preparation.

The [independent audit](proof/release-final-audit-c9aedabd.json) includes a
file-by-file hash, method and verdict for all **263 changed package paths**:
56 primary files and 207 exported JSON records. All 469 export JSON files were
parsed, with 468 manifest hashes checked; unrelated mission records retain
their original judgments. The 50 final-release files include all archive
members, read and reconciled by type. This is content reconciliation, not a
claim that every exported historical product was reviewed anew. The audit
script is [release-final-audit.py](proof/release-final-audit.py); it depends on
the retained local jars and this review's detached build directory. The
saved result remains readable without those temporary inputs.

### Independent commands and artifact custody

~~~sh
scripts/gw --log docs/review/02-brownfield/proof/release-check-30f8de4e.txt --offline -p .worktrees/review-release02-30f8de4e check --rerun-tasks
scripts/smoke.sh --jar /private/tmp/urlshort-mission02-30f8de4e.jar 18244 docs/review/02-brownfield/proof/release-jar-30f8de4e.jsonl
python3 docs/review/02-brownfield/proof/release-final-audit.py
python3 missions/02-brownfield/release/final-30f8de4e/verify-metrics.py
~~~

All completed successfully. The [fresh gate](proof/release-check-30f8de4e.txt)
ran all 14 tasks: **268 unit + 322 functional**, zero failures/errors/skips;
**583/583 source lines and 206/206 branches**. The class CSV sums 584 lines;
shared nested-class source lines explain that difference. All 74 producer and
reviewer XML reports agree in test counts/statuses. Two reports' parameterized
display names required only runtime object-id/generated-canary normalization;
the exact rule and files are disclosed in the audit. No tests were omitted.

The two actual saved release jars independently hash to
`fd6cf6f0c6c117c6138f50f9a2924c3f397edd65ae69b4a6dafebc4e6146132e`
(39,636,410 bytes). All **62 compiled class/resource entries** match the fresh
exact-pin build byte for byte. My [installed smoke](proof/release-smoke-30f8de4e.txt)
passed the public journey, environment overrides and 10 admissions followed
by 429 for the trusted test client; the [log](proof/release-jar-30f8de4e.jsonl)
is retained. The wrapper stopped its disposable app/database. The detached
worktree is clean.

### Release guidance §9 checklist

| Item | Status | Evidence and scope |
|---|---|---|
| 1. Claims trace to the stated SHA | pass | Final RELEASE, 133 input-tree rows, 263-path ledger, all 143 local links/anchors and the ten-path source equality check reconcile. Historical benchmark quantiles and dogfood are explicitly dated/reused, not a new final-SHA performance claim. |
| 2. Fresh gate, coverage and ready proof | pass | Own 590-test gate and coverage above; [own live snapshot](proof/release-proof-live-c9aedabd.json) is ready **64/64** at the same revision as the package. All **174 distinct judgment evidence files** match their receipt hashes, committed package bytes and current files. QA/QA2 closures `2a47ad28`/`a9b59315` preserve author/run attribution and the scoped content-change dispositions. |
| 3. Both installed artifacts smoked on loopback | pass | Own jar run plus audit of producer's jar/image smoke, 65 container JSON events and full wire captures. Twelve exchanges join by requestId/status, including complete non-echoing 400 and actual `/api/audit` 403. The initial wrong audit URL's 404 is retained as an instrument error. HostConfig binds 127.0.0.1:18241, read-only root, writable disposable data and 20s stop timeout; image/source specify non-root UID10001. Stopped inspect matches the captured container; graceful shutdown is logged. I did not repeat Docker startup. |
| 4. Advisories and remediation | pass | Final runtime graph and raw OSV response: **97 queries, zero advisories**, timestamp 04:21:27.259Z. This is a point-in-time result; no new advisory query by this reviewer. No unresolved advisory requires a remediation row. The pre-read's bounded tracked-blob secret scan and exclusions remain explicit. |
| 5. Complete gaps and review residue | pass | Final GAPS snapshot is byte-identical to the judged shared file. Wrapper MEDIUM, ping/V5/localhost LOWs, two structural INFOs, single-node H2, peer/proxy limits, bounded dogfood/load and original AC-28 host failure remain visible. Exact-candidate hosted CI/CD has zero represented runs and is explicitly unverified. |
| 6. Executable rollback and loss stated | pass | Exact merge/image targets and loopback-pinned recipe read. All 28 committed CSVs match the pre-read's independently checked four schema/data stages. Actual prior `f090103` jar hash/size and all 40 prior source/build files match; startup validates schema V2 with no migration, then smoke/shutdown pass. The source-revert drill supplies the mission rehearsal. D21-specific revert and separately built old-image startup are explicitly not claimed executed. Purged clicks/audit metadata and later writes lost on backup restore are named. |
| 7. Metrics derive from engine records | pass | All 430 provenance hashes in the 432-file archive match; original generator bytes match the product pin. I reran the frozen-time replay: all 17 instance rows, 181 packet metrics and totals match. Independently counted 249 closures/18 failed/55 retries. Historical 233/9 versus 230/8 discrepancy remains disclosed; export labels, text-match rollback heuristic, mean-of-instance MTTR and omitted custom human waits retain their limits. |
| 8. Human choice and publication boundary | pass | Brief recommends local use after review with explicit limits; alternative is hold for human push/hosted runs/target-host measurement. No ship stamp is claimed. Reviewed release history contains local docs commits; local Git tags are only accepted-slice tags. No push, release-tag, publication or remote exposure was performed in this review; producer records retain the same preparation boundary. Human/export work remains downstream. |

### Findings and resolutions

| ID | Severity | File:line / evidence | Resolution |
|---|---|---|---|
| PR-01 | MEDIUM, pre-read | Historical metrics mismatch reproduced above; final RELEASE §6 and archived generator inputs | **fixed** at c9aedabd. Original discrepancy preserved; final all-field replay independently passes with the limitations stated. |
| PR-02 | MEDIUM, pre-read | Historical RELEASE:160 omitted the retained wrapper MEDIUM; final §7 | **fixed** at c9aedabd. Lead owns timeout/retry follow-up after missions 02/03, sooner on a third blocking incident; LOW/INFO residue retained. |
| PR-03 | LOW, pre-read | Historical rollback-verification.md:32 said headers included | **fixed** at c9aedabd. Final §8 explicitly says excluded and qualifies the old mistake; raw CSV counts agree. |
| RR-01 | LOW | `missions/02-brownfield/release/final-30f8de4e/hosted-runs.json:1` at c9aedabd begins `+{`; Python JSON parsing fails at column 1 | Remove the stray leading plus. Producer corrected in passing; focused re-review below. |

### Re-review ad0c01f6

**RR-01 fixed.** Git shows exactly one changed file and an exact one-byte
deletion. The corrected JSON parses and preserves product SHA, zero runs and
the unverified-hosted-CI qualification. Independent audit records corrected
SHA-256 `cc47737b1550471f100f6ce3624130d1eeb53cfcb2bb55b36fad933c59c65751`.
All other package bytes are unchanged, so the substantive checks above apply.
No new finding or gate rerun is needed for this evidence-only correction.

**Final verdict: PASS / handoff to human ship_signoff.** No open MUST-FIX,
HIGH or new MEDIUM/LOW remains. Existing wrapper MEDIUM stays on the lead's
recorded trigger; ping/V5/localhost LOWs may remain backlog. The human still
decides local use with exact-SHA hosted CI/CD unverified and the stated
operational limits. This review neither waives the original AC-28 failure nor
grants publication authority.

### Formal self-check

Fresh exact-pin gate and installed jar smoke completed, apps stopped, full
package ledger and current proof hashes reconciled, metrics replayed, prior
findings disposed and correction re-read. Container/rollback executions are
producer-attributed raw evidence independently audited here, not new reviewer
runs. Product/tests/SPEC/design were untouched. No new human decision or
release stamp was invented; the authored handoff carries this final record.
