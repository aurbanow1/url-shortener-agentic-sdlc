---
slice: OPR.99.0.1.1
candidate_sha: 42a25db4a9c24fba3221c1ade4044719cab39ee3
artifact_type: qa
verdict: PASS
money_evidence: "Fresh main gate: 6 unit, 9 functional, 15/15 merged lines, zero
  branches; all seven contract outcomes have inspected evidence."
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
self_check: Verified unchanged product/test/build inputs from reviewed candidate
  through merge to main; inspected fresh XML/CSVs and saved HTTP/log evidence;
  no product edits or new server.
---

# Slice acceptance — 01-ping

QA: `qa-agent@urlshort-factory` (Codex), 2026-10-03 UTC.
Packet: `qitem-20261003013423-99b71ff1`.
Merge subject: `42a25db4a9c24fba3221c1ade4044719cab39ee3`.
Reviewed branch candidate: `f286a10863e4a8081235226f2d56e51ac121b319`.

The seven proof-contract outcomes have inspected supporting evidence. The
fresh main quality gate passed with 6 unit and 9 functional tests, no
failures, errors or skips, and merged coverage of 15/15 lines. There are no
branches; the branch threshold passes without a claim of exercised branch
logic. Attributed acceptance is recorded separately by `rig proof judge`.

## Candidate and gate

Main was at `877d6f309c625ba73a68d9c148c914941e18deaa`, one documentation
commit after the integration merge. It was left there to preserve the shared
checkout. `git diff --name-only <merge> HEAD` contained only mission NOTES,
slice PROOF and the integration log. Separate `git diff --exit-code` checks
established identical production, test and build inputs from the reviewed
branch to the merge and from the merge to the working checkout, including
the wrapper and environment script on the latter comparison.

Ran once from the main checkout:

```sh
scripts/gw --log missions/00-hello/slices/01-ping/proof/qa-accept-check-42a25db.txt --offline check --rerun-tasks
```

Exit 0; BUILD SUCCESSFUL in 19 seconds; all 13 tasks executed. JUnit XML
confirms 6 unit plus 9 functional tests. Each AC has its named functional
method. The fresh unit, functional and merged CSVs match the previously
committed candidate CSVs byte for byte: unit 15/15 lines, functional 13/15,
merged 15/15; each has zero branches. The committed HTML, XML and CSV report
entry points exist for all three suites.

## Evidence against the contract

Paths beginning `proof/` below are relative to the slice.

| Item | Observed support | Evidence |
|---|---|---|
| 1 | All eight named AC journeys pass on main; 15 executed methods have traceability rows | `proof/qa-accept-check-42a25db.txt`, `proof/qa-accept-audit-42a25db.txt` |
| 2 | Merged 15/15 lines; zero missed branches out of zero; coverage verification passes | `proof/qa-accept-audit-42a25db.txt`, `docs/qa/coverage/01-ping/all/jacocoTestReport.csv` |
| 3 | Unit and functional HTML/XML/CSV entry points are committed; CSV counts match this main run | `proof/qa-accept-audit-42a25db.txt`, `docs/qa/coverage/01-ping/` |
| 4 | Current traceability covers AC-1–8, BR-1–8 and all 15 executed test methods | `docs/qa/TRACEABILITY.md`, `proof/qa-accept-audit-42a25db.txt` |
| 5 | GAPS records no open merged-coverage/AC gap, the informational functional shortfall, and resolved QA-01 | `docs/qa/GAPS.md` |
| 6 | Saved live GET is 200 application/json with exactly status=ok and time; UTC time lies within recorded bounds; id satisfies the header shape | `proof/qa-ping-first-f286a10.txt`, `proof/qa-http-start-f286a10.txt`, `proof/qa-http-end-f286a10.txt` |
| 7 | The saved single JSON line is verbatim in the complete live log, matches that GET's id, and has no checked address/canary/user-agent content | `proof/qa-ping-log-line-f286a10.json`, `proof/qa-bootrun-f286a10.txt` |

The saved second GET, canary GET and POST were also independently parsed:
ids differ; POST is 405 application/problem+json with id and no trace or
exception name; every GET has matching JSON events; the complete log lacks
canary markers and checked loopback-address forms. The original log records
graceful shutdown. These are inspections of the earlier QA captures, not a
claim of new socket requests during slice acceptance. Identical product
inputs connect those observations to the merge; the main HTTP journeys ran
freshly.

## Limits and residue

No new server was started. Packaged jar/image, load, current dependency
advisories and out-of-scope HTTP behavior were not checked here. Release
owns the installed-artifact and advisory checks. The design owner's logging
example follow-up remains nonblocking and does not change the locked SPEC.
Delivery approval remains at mission ship sign-off.

## Self-check

- Read locked acceptance criteria and all seven proof-contract items, QA
  guidance, builder/QA proof, and both independent review verdicts.
- Ran the full gate once with rerun-tasks, read fresh XML and all CSVs,
  compared committed coverage, and checked traceability in both directions.
- Parsed the saved wire captures, UTC bounds and requestId-matched JSON
  events; checked historical failure/privacy cases and shutdown evidence.
- Recorded the actual tested HEAD and input equivalence to the merge; no
  product, test, build, locked-document or threshold edits.
- Attach this record and raw gate/audit files with a QA proof drop covering
  items 1–7, then record each attributed judgment against the merge subject.

## Media

![qa-accept-check-42a25db.txt](qa-accept-check-42a25db.txt)
![qa-accept-audit-42a25db.txt](qa-accept-audit-42a25db.txt)
