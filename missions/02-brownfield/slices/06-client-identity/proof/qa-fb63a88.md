---
slice: OPR.99.0.3.6
candidate_sha: fb63a88a9b92c1fec97ba74686af1a2f30304160
artifact_type: qa
verdict: PASS
money_evidence: 268 unit + 322 functional; merged584/584 lines206/206 branches;
  all15 AC effects observed against original production and candidate; proof16
  pending downstream.
evidences:
  - "1"
  - "2"
  - "3"
  - "4"
  - "5"
  - "6"
  - "7"
  - "8"
  - "9"
  - "10"
  - "11"
  - "12"
  - "13"
  - "14"
  - "15"
  - "17"
self_check: Raw HTTP/log/row effects, matrix/settings, CSV sums/378 hashes,326
  mappings, GAPS, stopped apps and clean exact candidate inspected;
  product/tests untouched;16 deferred by lead1882.
---

# Coverage and QA — 06-client-identity

QA PASS on exact `fb63a88a9b92c1fec97ba74686af1a2f30304160` (QA2, independent Codex). Fresh `../../scripts/gw --log <capture> --offline check --rerun-tasks`:268 unit +322 functional =590 passing invocations; zero failures/errors/skips; Javadoc and coverage verification green.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|---:|
| unit | 510 / 584 | 87.33 | 198 / 206 | 96.12 |
| functional | 550 / 584 | 94.18 | 172 / 206 | 83.5 |
| all | 584 / 584 | 100.0 | 206 / 206 | 100.0 |

Totals are sums of each copied CSV, not averages of suite percentages. Overall report merges only fresh `test.exec` and `functionalTest.exec`; no shipped-replay execution file. All378 HTML/XML/CSV/report-resource hashes rechecked against the saved fresh outputs. No new exclusion or threshold change.

Logs: `docs/qa/06-client-identity/check-fb63a88-diagnostic-02.txt`; XML/custody: `gate-final/`; first red retained in `gate-attempt-01/` and `localhost-collision.md`. Independent original-production characterization gate also268/322 green at1b4e0a7.

Every AC observed by effect:2041 controlled HTTP responses across original/candidate,10380 passing outcome checks,1017 exact response/log pairs plus two separately scoped metric/privacy comparisons per lane; 59+59 unmodified-jar responses/336 assertions per run;72 actual blank-setting jar responses/372 assertions. All51 apps stopped, counting the ten pre-work baseline apps; shutdown is bounded process/port observation, not an additional in-flight shutdown guarantee.

See `docs/qa/06-client-identity/README.md`, TRACEABILITY and GAPS for fixtures, comparisons and qualifications. Rule6 substitution dictionaries are retained. The original broad metric comparator failure remains visible; runtime telemetry and the existing springdoc duration measurement are explicitly reported outside AC-14. No promised HTTP/log/access/grouping difference remains.

**Proof16 remains pending:** lead confirmed qitem-20261004015644-b1493e3f/transition1882. Independent review, merge and design-owner guidance updates return as a durable QA item. This PASS covers qa_check, not future merge/lifecycle completion.

## Media

![qa-media/README.md](qa-media/README.md)
![qa-media/SUMMARY.md](qa-media/SUMMARY.md)
![qa-media/source-custody.json](qa-media/source-custody.json)
![qa-media/test-inventory.json](qa-media/test-inventory.json)
![qa-media/scoped-comparison.json](qa-media/scoped-comparison.json)
![qa-media/jar-scoped-comparison.json](qa-media/jar-scoped-comparison.json)
![qa-media/blank-settings-summary.json](qa-media/blank-settings-summary.json)
