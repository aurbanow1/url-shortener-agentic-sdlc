# QA coverage — 03-operate

Candidate `1c8b2cff20ad8b73a060bc817c8d0011782f876f`; independent QA re-check
by `qa2-agent@urlshort-factory` (Codex), 2026-10-03 UTC, packet
`qitem-20261003130258-8d163c70`.

`../../scripts/gw --log ../../docs/qa/03-operate/check-1c8b2cf.txt --offline check --rerun-tasks`
ran in the exact clean candidate worktree: **BUILD SUCCESSFUL**, all 14 tasks
executed, Javadoc and coverage verification included. JUnit XML: **165 unit /
155 functional**, zero failures, errors or skips.

| Suite | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---:|
| Unit | 400 / 443 | 90.29% | 162 / 162 | 100.00% |
| Functional | 408 / 443 | 92.10% | 131 / 162 | 80.86% |
| Merged | 443 / 443 | 100.00% | 162 / 162 | 100.00% |

Totals are summed from the three CSVs. All 321 HTML/XML/CSV copies under
`unit/`, `functional/` and `all/` match original SHA-256 hashes in the slice's
`proof/qa-report-hashes-1c8b2cf.json`; no threshold change or exclusion.
Historical a7c533f copies are in evidence commit `0b10ca9`.

**QA PASS for AC-1–AC-20 and the assigned QA boundary.** Independent fresh
HTTP run: 2,303 captured exchanges, all in-suite ACs observed, all 30 rejected
ids correlate exactly once to safe JSON completions; real H2 audit rows and
stored privacy effects inspected. Controlled Clock/peers/JDBC availability
are disclosed in `PROOF.md` §QA re-check. Live and committed OpenAPI are
identical with no field normalization. All 186 source methods / 320 JUnit
invocations map both ways; every release AC has its check and explicit gap.

The reviewer's unchanged actual-class probe now admits only 60 requests in
1 ms (was 120) and leaves 2 clients after rollback cleanup (was 10,002).
Strict independent R0 controls give 8/8 expected verdicts, including complete
and truncated fixed/chunked bodies, no response, 500 and an 11-second 201.
The unmodified candidate jar drain: complete R0 201, curl exit 0 at 532 ms,
new connection refused, 62 complete / 16 refused / 0 losses / 0 failures.

The initial drain/control attempts hit this macOS host's unsupported inherited
`C.UTF-8` locale in Perl. Failed outputs are retained; they count as no R0
verdict. Verified `C` locale reruns pass; host requirement is LOW QA-OPR-03.
No product or toolchain change. Unmodified jar smoke and env overrides pass.

Release AC-21–AC-28 remain pending as the locked SPEC assigns. The fresh
60-second bench achieved 82.1 redirects/s and 16.4 creates/s, below required
100/20: numeric NFR-L1/L2 targets remain unclaimed. Proof item 11 awaits new
code/security records; 13 awaits release. The lead already retains their
continuation in `qitem-20261003120849-f4cbfa97`; see proof-sequencing.md and GAPS.

## Self-check

Fresh full gate and every in-suite AC by effect; failures/privacy tried;
CSV totals/copy hashes read; both-way traceability and gaps written; corrected
shutdown predicate independently observed; all apps stopped; exact candidate
unchanged/clean; source/tests/build untouched. Proof drop/judgments cover
1–10 and 12; review/release judgments remain pending.
