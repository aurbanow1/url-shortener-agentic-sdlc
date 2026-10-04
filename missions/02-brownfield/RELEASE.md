# Mission 02 release pre-run — prepared on e227acf04e1ea902406532fcebd741f8a86f7f65, pending the refactor merge (06-client-identity, D21)

This is the authorized plain pre-run (`qitem-20261004005451-9e2c091d`), not
the lifecycle `release_prep` completion. It prepares evidence while wave review
waits for D21. The pinned candidate contains the five merged mission slices and
the reviewed W2F-01 scheduling-test correction; D21 is absent. No readiness,
ship approval, tag, push or publication is claimed.

## 1. Decision brief for ship sign-off

The pinned artifact passed the fresh local gate, canonical merged coverage,
loopback jar and container smoke, analytics-v2 wire checks, dependency check,
secret scan, three offered-rate benchmark scenarios and a V4-then-V3 schema
rollback rehearsal followed by migration reapplication and smoke. The jar and
image contain the same 39,635,446 bytes and SHA-256
`fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2`.

The result remains provisional because D21 must merge and receive its own
candidate gate, QA/review and range-diff. The recommended default is to carry
this evidence forward after D21 only where the final gate and behavior remain
bound to the same inputs; the alternative is to repeat every release check,
which costs time but removes the refactor-delta comparison. Rollback in one
line: stop writers, copy the stopped H2 data, run literal V4 then V3 rollback,
deploy the prior jar/image, verify schema/history and smoke; purged clicks are
irrecoverable without the backup.

## 2. Artifact and gate

Evidence: [gate artifact](release/pre-run-e227acf/gate-artifact.json),
[toolchain](release/pre-run-e227acf/toolchain.txt),
[artifact comparison](release/pre-run-e227acf/artifact-comparison.json),
[image inspect](release/pre-run-e227acf/image-inspect.json), and
[Docker build log](release/pre-run-e227acf/docker-build.txt).

`scripts/gw --log missions/02-brownfield/release/pre-run-e227acf/check-bootjar.txt --offline check bootJar --rerun-tasks`
passed with 226 unit and 250 functional tests, zero failures/errors/skips,
and 16/16 tasks executed. The canonical report uses only fresh
`test.exec` + `functionalTest.exec`: 581/581 lines and 206/206 branches,
with zero missed instructions, complexity, methods or classes. The jar was
copied before later source activity and its source inventory matched the pin.

The image is `urlshort:mission02-prerun-e227acf`, digest
`sha256:277703a6510ec2924fef9b423c0f48714c081fd2fe9da76461f54d702afd4b3b`,
Linux arm64, non-root UID 10001 and read-only root. It was built with the
plain `docker build` command after the local Docker client rejected the
unsupported legacy `--progress` option; the successful build is the claimed
build.

The candidate-SHA GitHub Actions lookup returned HTTP 404 on both sandboxed and
networked reads. This records no hosted CI/CD success for this SHA. Earlier
operator runs in the historical draft concern other SHAs and are not carried
as candidate evidence. The final lifecycle package must quote exact candidate
CI/CD runs or retain this as a gap, per [CI/CD §5](../../docs/guidance/ci-cd.md#5-release-package).

`rig proof show 02-brownfield --json` is exported at
`../../docs/evidence/02-brownfield/proof-readiness.json`; it remains
non-ready/unknown while D21 proof is pending.

## 3. Installed smoke

The saved jar ran at `127.0.0.1:18220` on disposable H2 data and the image ran
at `127.0.0.1:18221` with a named disposable volume. Both passed
[`scripts/smoke.sh`](../../scripts/smoke.sh); raw container smoke output is in
[container-smoke.txt](release/pre-run-e227acf/container-smoke.txt), and the
jar's structured file log is [jar.jsonl](release/pre-run-e227acf/jar.jsonl).
The container was inspected for loopback-only publishing, UID 10001,
read-only root and its data mount, then stopped and removed. The jar ended with
`Graceful shutdown complete`.

The analytics v2 probe created one link on each artifact and observed two GET
clicks, one unique visitor and one bot click for each day; HEAD added no click.
Both artifacts exposed all five static loss reasons, `urlshort_clicks_recorded`
at 3 for the probe, `urlshort_clicks_lost` at 0, and Prometheus text matched
the expected families. Audit requests with forged XFF returned 403 and an
invalid URL returned a non-echoing ProblemDetail 400. See
[installed-v2.json](release/pre-run-e227acf/installed-v2.json) and the two
Prometheus captures.

The smoke did not prove restart durability, PostgreSQL, a natural disk crash,
large-directory startup, SIGTERM during purge, remote TCP identity or hosted
runner behavior. The rollback rehearsal proves a stopped H2 copy and migration
reapplication; the remaining limits stay in §7 and the complete GAPS snapshot.

## 4. Dependency advisories and secret scan

`scripts/gw --offline dependencies --configuration runtimeClasspath` resolved
97 dependencies. `node tools/dep-advisories.mjs` queried them at
2026-10-04T01:06:49.753Z and found zero advisories. The complete result is
[advisories.json](release/pre-run-e227acf/advisories.json). The historical
dependency overrides and their reachability reasoning remain recorded in the
review/GAPS files; no new remediation item is needed for this unchanged pin.

The tracked blobs at the pinned SHA were scanned for high-signal private-key,
AWS-key and GitHub-token patterns: 9,236 text files, no findings. 497 binary
files were not entropy-scanned; this is a bounded repository scan, not a claim
about untracked files, history or external secret stores. See
[secret-scan.json](release/pre-run-e227acf/secret-scan.json).

## 5. Evidence per slice

The five merged slices are documented by their SPEC, design, PROOF and QA
records; their independent reviews and ledger entries are in
[REVIEW-LEDGER.md](../../docs/review/REVIEW-LEDGER.md). The relevant merged
product range is `cb148c4` (audit read), `ed2b940` (retention), `5c264db`
(dogfood fix), `d55a502` (audit columns), `0aa3695` (CI/CD), plus the W2F-01
correction at `50ad9c3`.

| Slice | SPEC / design / PROOF / QA coverage |
|---|---|
| 01-audit-read | [SPEC](slices/01-audit-read/SPEC.md), [design](slices/01-audit-read/design.md), [PROOF](slices/01-audit-read/PROOF.md), [coverage](../../docs/qa/coverage/01-audit-read/SUMMARY.md) |
| 02-click-retention | [SPEC](slices/02-click-retention/SPEC.md), [design](slices/02-click-retention/design.md), [PROOF](slices/02-click-retention/PROOF.md), [coverage](../../docs/qa/coverage/02-click-retention/SUMMARY.md) |
| 03-dogfood-fix | [SPEC](slices/03-dogfood-fix/SPEC.md), [design](slices/03-dogfood-fix/design.md), [PROOF](slices/03-dogfood-fix/PROOF.md), [coverage](../../docs/qa/coverage/03-dogfood-fix/SUMMARY.md) |
| 04-audit-columns | [SPEC](slices/04-audit-columns/SPEC.md), [design](slices/04-audit-columns/design.md), [PROOF](slices/04-audit-columns/PROOF.md), [coverage](../../docs/qa/coverage/04-audit-columns/SUMMARY.md) |
| 05-ci-cd | [SPEC](slices/05-ci-cd/SPEC.md), [design](slices/05-ci-cd/design.md), [PROOF](slices/05-ci-cd/PROOF.md), [coverage](../../docs/qa/coverage/05-ci-cd/SUMMARY.md) |

The wave review initially held on W2F-01 and then passed the correction; the
mission remains waiting only for D21's sixth slice. D21's before-capture is
pre-work, not an acceptance judgment. Final proof and review must bind to the
post-D21 SHA.

QA's independent bounded installed dogfood passed with no new defects at
`d83bbc587df57174ca30c1094ef954b1a6ff9f5c`; see
[dogfood report](../../docs/qa/dogfood/02-brownfield.md). It reused only the
explicitly byte-equivalent artifact observations and added its own audit,
pagination, retention, privacy, problem, metric and log-correlation checks.

## 6. Metrics and benchmark

The three 60-second open-loop runs are captured in
[bench.txt](release/pre-run-e227acf/bench.txt). Combined 100 redirect GET/s +
20 creates/s achieved exactly the offered rates with zero bad responses:
redirect p95 3.0 ms/p99 6.3 ms and create p95 3.7 ms/p99 7.7 ms. GET alone
was p95 4.0 ms/p99 16.2 ms; HEAD alone p95 2.3 ms/p99 5.2 ms. The 1.7 ms
GET-minus-HEAD p95 is a separate-distribution comparison proxy, not an
isolated added-cost quantile. This is one laptop/Colima observation and not a
capacity claim.

The coordinated final evidence export and `node tools/sdlc-metrics.mjs` output
are the sources for the mission-wide reliability numbers. Zero retries or
rollbacks must be read as zero for a mission with none; non-zero historical
drills are recorded separately. The generated metrics README explains the
engine-trail derivation, active-instance treatment, human wait and latency
limits.

## 7. Known gaps, complete

The complete point-in-time source is [GAPS-snapshot.md](release/pre-run-e227acf/GAPS-snapshot.md)
and the repository source remains [docs/qa/GAPS.md](../../docs/qa/GAPS.md). No
row is silently dropped. The release-relevant qualifications are:

| Area | Honest qualification and owner |
|---|---|
| D21 client identity | Behavior-preserving refactor is still in flight; requirements, characterization, QA, review, final gate, proof and range-diff are pending. Lead/integrator owns the lifecycle. |
| Audit read | Accepted compatibility grant covers the two inherited API-enumeration failures; real remote TCP/IPv6 and hardware/store-crash claims are outside scope. Review/GAPS retain the headerless local-relay boundary. |
| Retention/audit columns | Controlled clocks, JDBC faults and H2 rollback are covered; PostgreSQL, large catch-up and SIGTERM-during-purge are not. Purged clicks cannot be restored from code. |
| Analytics and benchmark | GET-minus-HEAD is a qualified comparison; no isolated recording-cost quantile or broad capacity claim. |
| CI/CD | Candidate hosted runs were unavailable on the checked API surface; failing-run behavior, branch protection, hosted runner shell/JDK and actionlint are not independently claimed. Local image layers were cached. |
| Operations | Single-node H2 file database, localhost-only binding and host-specific macOS/Colima restart behavior remain operational limits. Functional test localhost collision is a LOW test-environment backlog item. |
| Security/advisories | Current OSV is clean; prior review qualifications remain. The secret scan is bounded to tracked high-signal patterns and does not cover entropy/history/untracked state. |
| Review residue | LOW/INFO forward items stay in the review ledger and GAPS; no MEDIUM/HIGH issue is being hidden by this pre-run. |

## 8. Rollback

The stranger-executable recipe is in [rollback-verification.md](release/pre-run-e227acf/rollback-verification.md),
with literal [V4](release/pre-run-e227acf/rollback-v4.sql) and
[V3](release/pre-run-e227acf/rollback-v3.sql) scripts copied from the
migration headers. Stop the jar/container and all writers. Copy the complete
stopped `data/` directory (or the named Compose volume), record Flyway
versions, run V4 first and V3 second, then deploy the prior jar/image and keep
the backup. Verify row projections, constraints/history, health, create,
redirect, stats, problem responses and loopback binding. The rehearsed
candidate reapplied both migrations and passed the full smoke on port 18222.

For a source rollback after a future merge, the integrator must use
`git revert -m 1 <merge>` in dependency order on a throwaway branch and then
repeat the gate/smoke. The final package must fill in the exact D21 merge and
image identifiers. Dropping V3/V4 discards their audit metadata; lowering
retention or reverting code cannot recreate purged clicks.

## 9. Self-check

- Artifact claims link to the gate, image, jar, source inventory, logs and raw probes.
- The smoke and benchmark used the same pinned jar/image SHA; all started services and the container are stopped.
- The canonical coverage inputs and exact totals are recorded; no old auxiliary probe was counted.
- The complete GAPS snapshot is preserved and the release table names every relevant qualification.
- The rollback was executed on a disposable stopped-data copy, including reapplication and a full smoke.
- Candidate hosted CI/CD was queried and unavailable; no different SHA is presented as proof.
- This is a provisional pre-run only: D21, final candidate gate, review and the human ship gate remain.
