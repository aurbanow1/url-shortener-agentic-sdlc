# QA coverage — 02-analytics

Candidate 862c52eea8294e438b1f98b832ae4f64f7a16923; independent QA run 2026-10-03 UTC.

../../scripts/gw --offline check --rerun-tasks: BUILD SUCCESSFUL, 121 unit / 126 functional invocations, zero failures/errors/skips; Javadoc and merged verification passed. Complete log: missions/01-greenfield-core/slices/02-analytics/proof/qa-check-862c52e.txt.

| Suite | Lines covered / total | Line % | Branches covered / total | Branch % |
|---|---:|---:|---:|---:|
| unit | 325/359 | 90.53% | 118/118 | 100.00% |
| functional | 324/359 | 90.25% | 92/118 | 77.97% |
| all | 359/359 | 100.00% | 118/118 | 100.00% |

Counters summed from each committed per-class CSV; all HTML/XML/CSV copied. Per-suite shortfalls are informational; merged coverage has no missed line/branch or exclusion.

312 captured HTTP exchanges; 256 stored clicks, including exactly 200 redirects from 20 clients. Full referrer and UA tables, non-click paths, exact top-ten order, empty/retired stats, aggregate shape, request ids, safe failures, real rejected click insert, identical 24 audit/22 link snapshots, and live API document equality checked independently. Controlled functional effects prove two client addresses and UTC-day salt/grouping boundaries (AC-5/9), and the 2-second-per-write slow-store condition (AC-14); these are the SPEC-authorized mechanisms.

NFR-L3 numeric added p95 remains release-level, explicitly recorded in GAPS.md. Proof items 12 (future security-review record) and 13 (release bench) remain pending per proof-sequencing.md; QA does not attribute acceptance before their evidence exists. No packaged artifact, PostgreSQL, proxy alignment, expiry feature, rate-limit feature or exhaustive scheduling claim.
