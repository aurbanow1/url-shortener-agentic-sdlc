# QA custody judgment — X′ a2c34c1

QA2 / Codex, 2026-10-03. Packet qitem-20261003225644-1b96a603.
Scope: proof contract item 9 and the lead's 21:42Z rebase conditions.

**PASS:** X′ = `a2c34c146c75cfabe24b16ec9e30ad40628dd676` descends from
audit-read merge `cb148c4fecd40e39dc7e1c8788506b0f37a2f119`, and V3 is the
next migration after audit-read's unchanged V1/V2. The worktree HEAD equals X′
and is clean. `git merge-base --is-ancestor cb148c4 a2c34c1` exited 0.

Independent range-diff:

```sh
git range-diff 16312da..a8fc8b6 2ead709..a2c34c1
```

Patches 1–6 are identical. Patch 7 changes only application.properties context
around audit-read's existing forward-headers pin. Its additions/removals and
README's additions/removals equal the original patch byte for byte, independently
extracted and compared. This meets the authorized exception. No product/source
patch changed beyond that context; earlier QA and review remain attributed to
X = `a8fc8b6b67e3a3cbdaada43a3233191b6c7610e1`, not silently relabeled X′.

Migration tree at cb148c4: V1, V2. Tree at X′: V1, V2, V3. V1/V2 bytes equal the
merge's originals; V3 bytes equal X and the reviewed/copied migration, SHA-256
`908715401b5c84aa8b4d1525b91a9bd472d2a9ada605a8dbd5651ff1fdfc5fb6`.

The lead-run fresh gate log was read: BUILD SUCCESSFUL, 14/14 tasks executed.
Its current JUnit XML sums to 213 unit and 224 functional tests, no failures,
errors or skips. This fresh test execution was the lead's, not a QA rerun.
An older auxiliary qaShippedTest.exec remained. QA therefore independently ran
coverage verification and the merged report using the saved external review
init script, restricting input to fresh test.exec and functionalTest.exec:

```sh
../../scripts/gw --log /Users/andrzej/Documents/projekty/test/openrig/url-shortener/missions/02-brownfield/slices/02-click-retention/proof/qa-custody-coverage-a2c34c1.txt --offline -I /Users/andrzej/Documents/projekty/test/openrig/url-shortener/docs/review/02-click-retention/proof/canonical-coverage.gradle jacocoTestCoverageVerification jacocoAllReport
```

BUILD SUCCESSFUL. The fresh suites remained UP-TO-DATE; the verification and
report executed. Canonical CSV totals: **555/555 lines, 200/200 branches**, 100%.
CSV/XML copies and hashes are saved next to this record. No src/, test, build
configuration or script was edited; no app was started for this narrow packet.

Evidence: qa-custody-git-checks-a2c34c1.json contains command outputs/exit codes;
qa-custody-shared-hunks-a2c34c1.json contains both exact patches;
qa-custody-coverage-audit-a2c34c1.json contains counts, report hashes and migration
hashes; qa-custody-coverage-a2c34c1.txt is QA's restricted-coverage output.
The lead's full gate is docs/evidence/02-brownfield/integrate-02-click-retention-xprime-check-a2c34c1.txt.

## Self-check

Observed actual Git ancestry exit code, exact HEAD, full range-diff, extracted
changed lines and migration trees/hashes. Read the full-gate result and fresh
JUnit XML; independently verified merged coverage without auxiliary execution
data. Preserve attribution: items 1–8/10 on X; item 9 only on X′. No new public
journey claim, rebase or merge by QA. This fulfills the deferred custody
obligation; integration and its accepted tag must target X′ under the lead's rule.
