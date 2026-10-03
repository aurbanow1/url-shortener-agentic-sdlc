# QA coverage — 03-operate

Candidate `a7c533ffef55650e5b422377ffe0c4e38d41400c`; independent QA by
`qa2-agent@urlshort-factory` (Codex), 2026-10-03 UTC.

`../../scripts/gw --log ../../docs/qa/03-operate/check-a7c533f.txt --offline check --rerun-tasks`
ran in the clean candidate worktree: **BUILD SUCCESSFUL**, all 14 tasks executed,
Javadoc and coverage verification included. JUnit XML reports 163 unit and 155
functional invocations, zero failures, errors or skips.

| Suite | Lines covered / total | Line coverage | Branches covered / total | Branch coverage |
|---|---:|---:|---:|---:|
| Unit | 398 / 441 | 90.25% | 160 / 160 | 100.00% |
| Functional | 406 / 441 | 92.06% | 129 / 160 | 80.63% |
| Merged | 441 / 441 | **100.00%** | 160 / 160 | **100.00%** |

Totals were summed from the three CSVs, not inferred from build success.
HTML/XML/CSV reports are copied under `unit/`, `functional/` and `all/`; 321
files have independently verified copy hashes in the slice's
`proof/qa-report-hashes-a7c533f.json`. No exclusion or threshold change.

Independent observation: 2,303 recorded HTTP exchanges, all AC-1–AC-20 observed
through real Tomcat and migrated file H2. Deterministic clock, request peers
and a database-availability gate are explicitly disclosed in
`missions/01-greenfield-core/slices/03-operate/PROOF.md` §QA. Unmodified jar
smoke, environment overrides, a 60-second bench and shutdown drain were also
run. Exact captures and instruments are under the slice's `proof/qa-*` paths.

The live and committed OpenAPI documents are equal after key sorting, with
no field normalization. Candidate ancestry includes 02's merge `091ff46`.

**QA PASS for the assigned boundary.** Container AC-21–AC-24/AC-28 and the
release-level judgment for AC-25–AC-27 remain pending under the locked SPEC.
The benchmark achieved 82.5 redirects/s and 16.5 creates/s, below the required
100/20 input rates; its latency numbers do not prove NFR-L1/L2. Proof item 11
needs the following code/security reviews; item 13 needs release evidence.
See `docs/qa/03-operate/proof-sequencing.md` and `docs/qa/GAPS.md`.

## Self-check

Fresh gate and every in-suite AC observed; error/privacy paths tried; CSV totals
and copy hashes read; all 184 test methods and 318 invocations accounted for;
release gaps disclosed; captured rejections correlated to logs; apps stopped;
product source/tests/config untouched and worktree remains at the candidate.
