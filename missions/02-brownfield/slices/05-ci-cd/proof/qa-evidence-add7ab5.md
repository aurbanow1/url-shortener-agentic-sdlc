---
slice: OPR.99.0.3.5
candidate_sha: add7ab5ca37dcd6f51aef3cd43c85455e1be6d14
artifact_type: qa
verdict: PASS
money_evidence: "AC1-12 verified: 165 unit/155 functional, merged443/443
  lines162/162 branches, own loopback jar smoke, three-file YAML/diff/pins and
  completed builder base-pull image capture; AC13 explicitly pending per SPEC
  A5"
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
self_check: Read candidate source and parsed YAML; independently ran fresh
  offline check, bootJar and shipped smoke; verified321 copied report hashes/CSV
  totals,186 source-method trace mappings, unique request log IDs and
  shutdown/PID/port; read both pulled-base digests,15 image steps and exit0 plus
  attached builder drops. No GitHub execution claimed.
---

# 05-ci-cd — independent QA coverage

Candidate: `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14` (clean `slice/05-ci-cd`
worktree). Base: `06818cab00cab0c3a9ab225172307aaea96d8d53`. QA seat:
`qa2-agent@urlshort-factory`, Codex, 2026-10-03. **QA PASS on AC-1–12; AC-13 PENDING as allowed.**

Fresh command, from `.worktrees/05-ci-cd`:
`../../scripts/gw --log <main>/docs/qa/05-ci-cd/check-qa-add7ab5.txt --offline check --rerun-tasks`.
Exit 0, `BUILD SUCCESSFUL in 36s`, 14/14 tasks executed, including both suites,
merged coverage verification and Javadoc doclint. XML confirms 165 unit and
155 functional invocations, zero failures, errors or skips in either suite.

Percentages below are sums of the fresh CSV class rows, not rounded HTML
labels or builder claims. The gate reads merged execution data; 100% applies
to merged lines and branches. Per-suite figures are informational.

| Suite | Tests | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|---:|
| Unit | 165 | 400 / 443 | 90.2935% | 162 / 162 | 100% |
| Functional | 155 | 408 / 443 | 92.0993% | 131 / 162 | 80.8642% |
| Merged | 320 | 443 / 443 | 100% | 162 / 162 | 100% |

All 321 report files (HTML, XML, CSV, including the merged report) were copied
from this run and SHA-256 checked against their source bytes. The inventory,
all 186 named source-test mappings, XML invocation names and copy hashes are in
[`qa-report-audit-add7ab5.json`](../../../../missions/02-brownfield/slices/05-ci-cd/proof/qa-report-audit-add7ab5.json).
No product, test, build or threshold changed; the candidate diff is three
`.github/` files, 129 insertions. No new excluded path or coverage waiver.

AC-1 through AC-11: QA parsed all three YAML documents as YAML 1.2, checked the
parsed values and read the numbered source. Nine action references match four
builder network tag captures, including the annotated Gradle action's peeled
commit. Native actionlint was absent; parsing is the SPEC's allowed fallback.

AC-12 local checks: independent `bootJar` exit 0 and the unchanged shipped
`smoke.sh --jar` exit 0 against `127.0.0.1:18105`, no SMOKE FAIL. The smoke
observed health/readiness, request IDs, create, redirect Location/no-store,
active read, click count/referrer reduction, invalid-input problem, unknown
code, retirement and 410, metrics, OpenAPI, environment overrides and exactly
10 admitted requests then 429. The JSON log has 29 completed requests with
29 unique request IDs and statuses 200, 201, 204, 302, 400, 404, 405, 410 and
429. Graceful shutdown is logged; PID 54941 is absent and port 18105 refuses
connections. Existing functional audit tests inspect actual JDBC rows; no
manual audit read on this jar or independent duplicate/expiry curl replay is
claimed for this configuration-only slice.

AC-12 image: **PASS by the contracted builder capture**, independently read
and reconciled. `docker-build-pull-add7ab5.txt` records the exact candidate,
`docker build --pull`, both Temurin base pulls/digests, all 15 ordered steps,
step 7's in-container `bootJar` success and image/tag success, exit 0. Steps
5–7 ran anew; other layers used cache. `builder-docker-build-pull-add7ab5.md`
and `builder-action-pins-add7ab5.md` attach the two network captures through
`rig proof add` (builder evidence commit `ad79bb4`). The first stopped
`--no-cache` attempt is retained honestly; a wholly uncached build is not
claimed and is not required by AC-12.

AC-13: **PENDING the human's push**, allowed by SPEC A-5/D2/D13. No GitHub
check/run URL, hosted-runner execution, upload, action/cache or Dependabot
execution is claimed. A deliberate failing GitHub run was not exercised (A-6);
configuration checks establish no task skip, tolerated failure or masked exit.
See the new 05-ci-cd entry in `docs/qa/GAPS.md`.

## Self-check

Exact candidate and clean product tree checked; AC-1..11 traced to numbered
files; YAML parse output, fresh check, bootJar, jar smoke, structured log and
three-file diff retained under slice proof. All 321 copied report hashes and
CSV totals checked. AC/rule table records all 13 ACs and all seven rules;
GitHub-dependent effects are pending, not passing. App stopped and worktree
left at candidate. Builder network captures, candidate SHA, both pulls, completed build and C1 media attachments checked; all seven proof-contract items evidenced. The only unobserved acceptance effect is the explicitly allowed pending AC-13.

## Media

![qa-file-checks-add7ab5.txt](qa-file-checks-add7ab5.txt)
![qa-check-add7ab5.txt](qa-check-add7ab5.txt)
![qa-bootjar-add7ab5.txt](qa-bootjar-add7ab5.txt)
![qa-smoke-add7ab5.txt](qa-smoke-add7ab5.txt)
![qa-jar-log-add7ab5.jsonl](qa-jar-log-add7ab5.jsonl)
![qa-diff-add7ab5.txt](qa-diff-add7ab5.txt)
![qa-report-audit-add7ab5.json](qa-report-audit-add7ab5.json)
