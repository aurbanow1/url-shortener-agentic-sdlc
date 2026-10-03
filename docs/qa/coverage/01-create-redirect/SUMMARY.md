# QA coverage — 01-create-redirect

Candidate `a922f49144049db0228c316c474ac6e890742fa5`; independent QA run 2026-10-03 UTC.

`../../scripts/gw --offline check --rerun-tasks` completed successfully: 72 unit and 87 functional invocations; zero failures, errors or skips; Javadoc gate passed. Captured in `proof/qa-check-a922f49.txt` under the slice.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|
| unit | 169/185 | 91.35% | 56/56 | 100.00% |
| functional | 176/185 | 95.14% | 50/56 | 89.29% |
| all | 185/185 | 100.00% | 56/56 | 100.00% |

Numbers are sums of the committed per-class CSV counters. Per-suite shortfalls are informational; the merged gate has no missed line or branch and no exclusion. HTML, XML and CSV are copied for all three reports.

AC-1–AC-28 have named functional tests. Live HTTP covers every externally controllable AC, all listed invalid inputs, byte boundaries, browser errors, replay/mismatch, environment override, correlated logs, audit rows, rollback and append-only snapshots. AC-19 uses the explicitly permitted suite-controlled FunctionalClock. No expiry/rate-limit, PostgreSQL, packaged artifact or release secret-scan claim.

Proof items 13–14 remain assigned to code review and release prep; see `docs/qa/01-create-redirect/proof-sequencing.md` and queue `qitem-20261003072643-917956c7`.
