---
slice: OPR.99.0.3.5
candidate_sha: add7ab5ca37dcd6f51aef3cd43c85455e1be6d14
artifact_type: qa
verdict: PASS
money_evidence: "AC-13 judged from operator record b6b4a29: PR gate success,
  main CI/CD success, urlshort-jar and smoke-logs attached. QA browser/API
  retrieval failed; explicit packet fallback used. A-6 red run remains
  unexercised."
evidences:
  - "5"
  - "6"
  - "7"
self_check: Read exact committed operator section and checked URLs, events,
  refs, conclusions, PR gate name and both CD artifacts. Retained browser cache
  misses/API404, no independent live verification claimed. Verified local merge
  second parent and unchanged workflow bytes; changed only 05-ci-cd shared rows,
  preserved operator section, retained A-6.
---

# 05-ci-cd AC-13 — first GitHub runs

QA2 follow-up on qitem-20261003211407-91a03acb, 2026-10-03.
Accepted configuration candidate: `add7ab5ca37dcd6f51aef3cd43c85455e1be6d14`;
local integration `0aa36952d3e9f2a0ac8b1519f26f349a82cb2e93` has that exact
second parent. The three workflow/config files remain byte-identical to the
candidate on current local main.

**AC-13 satisfied from the operator's committed record, not an independent
GitHub fetch.** The packet explicitly allows this source when QA cannot open
the run URLs. Browser requests for all three pages failed with cache misses.
Authenticated `gh api` requests for all three runs, PR/CD jobs and CD artifacts
returned HTTP 404; that response does not establish that the runs are absent.
No success, job or artifact metadata was fetched by this QA seat. Attempts are
retained in [github-access-attempts.json](github-access-attempts.json).

I read the operator's AC-13 section in PROOF.md at
`b6b4a29fedeaaef8a5903891178a3e02383de4f8`, preserved as
`proof/qa-ac13-operator-record-b6b4a29.txt`, and checked the following fields:

| Run URL | Recorded event / commit | Recorded conclusion and artifacts |
|---|---|---|
| [PR ci 37153245436](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153245436) | pull_request, pr/07-ci-cd-merged, 2e33568 | success; job gate; gate-reports |
| [main ci 37153380482](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380482) | push, main, a3d6867 | success; gate-reports |
| [main cd 37153380418](https://github.com/aurbanow1/url-shortener-agentic-sdlc/actions/runs/37153380418) | push, main, a3d6867 | success; job package; bootJar, loopback jar smoke and docker build steps succeeded; urlshort-jar (35.8 MB) and smoke-logs (4 KB) |

These meet the recorded-run outcomes of AC-13: a successful PR gate, and a
successful main CD run with both required artifacts. The earlier pending-record
judgment is superseded by a judgment accepting this operator evidence.

The operator also records the first main runs on ecf8dfd (ci 37153261623,
cd 37153261626) as cancelled by concurrency when a3d6867 arrived. The successful
main runs contain Dependabot PR #8's wrapper change to Gradle 9.8.0; the PR gate
used 9.7.1. Local main adopted that wrapper change at f3e6b0b. This context
does not change the subject of the configuration judgment or supply a new
local gate claim. QA did not download artifacts, inspect hosted-run logs or
independently confirm a wholly uncached build. The deliberate red GitHub run
remains unexercised under SPEC A-6, and branch protection is outside scope.

The 05-ci-cd sections of GAPS and TRACEABILITY were unchanged between the
original QA evidence commit f10c796 and this follow-up; only the unrelated
01-audit-read correction had invalidated their whole-file receipts. Their
05-ci-cd rows now record AC-13's operator evidence with this qualification.
Items 5/6 are reaffirmed against current file hashes; item 7 accepts the new
run record. No product, tests, workflow, coverage report or threshold changed.

## Self-check

Read all three URLs in the operator's exact committed section and checked
events, refs, commits, conclusions, PR job name and both CD artifact names.
Attempted direct access and retained failures without claiming a live fetch.
Preserved the original operator section and the candidate/merge relationship.
Updated only the slice's QA evidence and its shared-document section; kept
A-6 and unverified artifact/log details explicit. New attributed receipts
name the exact candidate, URLs, source commit and source limitation.

## Media

![qa-ac13-operator-record-b6b4a29.txt](qa-ac13-operator-record-b6b4a29.txt)
![github-access-attempts.json](github-access-attempts.json)
