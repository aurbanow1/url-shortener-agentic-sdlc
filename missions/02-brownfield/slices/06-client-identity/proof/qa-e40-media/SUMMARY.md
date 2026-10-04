# Coverage and QA — 06-client-identity, re-check e40b095

QA PASS on exact `e40b09541feb0b7555c475baa82587fdd09e4890`. Own fresh offline check: 268 unit and 322 functional tests, zero failures/errors/skips ; 14 tasks/Javadoc/coverage verification green.

| Suite | Lines | Line % | Branches | Branch % |
|---|---:|---:|---:|---:|
| Unit |510/584|87.33|198/206|96.12|
| Functional |550/584|94.18|172/206|83.50|
| Overall |584/584|100|206/206|100|

CSV sums, not percentage averages; fresh test.exec+functionalTest.exec only. 378 copied report resources rechecked. Prior reports/SUMMARY preserved under archive-fb63a88; all 3490 historic artifact hashes verified using 379 explicit archive aliases. No exclusion/threshold change.

CR-01: QA actual-helper held-writer checks pass at 3036/3042ms with no trust/P trust, one helper stats read each, no 429/NPE/full 3/2/day/privacy oracle. Current characterization: 72/72 also passes on original production without modifying baseline source/tests. Affected 12-row HTTP replay: 159 responses/818 assertions, all 159 complete response/log comparisons identical under documented substitutions; 3 apps stopped, both probe JVMs exited.

Remaining AC effects are carried from prior independent QA by identical production/unit trees and actual jar SHA 92e1b7aef3b91749facdc39bfbc35cc8df8367119294d801436a7db34b38e58f, per re-check guidance §5; they were not all rerun. Earlier external 401/metrics comparator/springdoc measurement qualifications remain visible.

Evidence: `docs/qa/06-client-identity/recheck-e40b095/README.md`, `check.txt`, `validation.json`; current TRACE/GAPS and PROOF QA re-check. Proof 16 remains pending under lead transition 1882 until independent re-review/merge/design-owner register updates. This QA PASS is not final merge acceptance.
