# Wave file ledger — d55a502..94aa2c0

45 changed paths / 45 reviewed; 18 product/test/API paths plus 27 evidence/coordination paths. This ledger covers each changed file/diff, not a new execution of every historical evidence capture. Immutable target SHA-256 values are in `wave-evidence-94aa2c0.json`. The application.properties LOW is an unchanged integration-context file, separately cited in the main report.

| File | Verdict |
|---|---|
| `.claude/settings.json` | PASS — checksum command allow rule only; no product runtime change. |
| `docs/api/openapi.json` | PASS — only two daily fields, statistics example/description; live export equality. |
| `docs/evidence/02-brownfield/integrate-04-audit-columns-check-305f804.txt` | PASS — complete saved command result read/parsed; successful gate or matching range-diff; historical scope retained. |
| `docs/evidence/03-ambiguous-analytics/integrate-01-analytics-v2-range-diff-ec466da-22fc8e2.txt` | PASS — complete saved command result read/parsed; successful gate or matching range-diff; historical scope retained. |
| `docs/qa/GAPS.md` | PASS — four closed audit-column debts with corrected QA actor; analytics qualifications retained. |
| `docs/review/01-analytics-v2/01-code-review.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `docs/review/01-analytics-v2/02-security-review.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `docs/review/01-analytics-v2/proof/code-check-ec466da.txt` | PASS — complete saved command result read/parsed; successful gate or matching range-diff; historical scope retained. |
| `docs/review/01-analytics-v2/proof/evidence-reconciliation-ec466da.json` | PASS — structured evidence/custody and qualifications read; current accepted receipt hashes reconciled. |
| `docs/review/01-analytics-v2/proof/live-proof-ec466da.json` | PASS — structured evidence/custody and qualifications read; current accepted receipt hashes reconciled. |
| `docs/review/01-analytics-v2/proof/reconcile-ec466da.py` | PASS — prior reviewer reconciliation source read; distinguishes hashes, raw effects and source mappings. |
| `docs/review/01-analytics-v2/proof/runtime-dependencies-ec466da.txt` | PASS — complete saved command result read/parsed; successful gate or matching range-diff; historical scope retained. |
| `docs/review/REVIEW-LEDGER.md` | PASS — separate code/security judgments on exact ec466da; existing rows retained. |
| `missions/02-brownfield/NOTES.md` | PASS — merge custody, grants and late GAPS attribution correction recorded honestly. |
| `missions/02-brownfield/slices/04-audit-columns/PROGRESS.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `missions/02-brownfield/slices/04-audit-columns/PROOF.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `missions/02-brownfield/slices/04-audit-columns/proof/judgments/00000012.md` | PASS — attributed receipt, exact subject and evidence references checked; supersession retained. |
| `missions/02-brownfield/slices/04-audit-columns/proof/qa-gap-closure-305f804.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `missions/02-brownfield/slices/04-audit-columns/proof/qa-gap-closure-final.json` | PASS — structured evidence/custody and qualifications read; current accepted receipt hashes reconciled. |
| `missions/02-brownfield/slices/04-audit-columns/proof/qa-gap-closure-final.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `missions/02-brownfield/slices/04-audit-columns/proof/qa-gap-merge-custody.json` | PASS — structured evidence/custody and qualifications read; current accepted receipt hashes reconciled. |
| `missions/02-brownfield/slices/04-audit-columns/proof/qa-gap-return-17593aa.md` | PASS — original attribution mismatch retained, explicitly resolved by correction/final record. |
| `missions/03-ambiguous-analytics/NOTES.md` | PASS — candidate/rebase/gate and remaining release proof explicitly separated. |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/builder-check-xprime-22fc8e2.txt` | PASS — complete saved command result read/parsed; successful gate or matching range-diff; historical scope retained. |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/judgments/00000011.md` | PASS — attributed receipt, exact subject and evidence references checked; supersession retained. |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/judgments/00000012.md` | PASS — attributed receipt, exact subject and evidence references checked; supersession retained. |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-item11-ec466da.md` | PASS — complete changed evidence/record read; candidate, observation limits and remaining obligations consistent. |
| `missions/03-ambiguous-analytics/slices/01-analytics-v2/proof/qa-returned-judgments-ec466da.json` | PASS — structured evidence/custody and qualifications read; current accepted receipt hashes reconciled. |
| `src/functionalTest/java/dev/urlshort/audit/AuditUpgradeJourneyTest.java` | PASS — one granted daily-shape assertion; existing V2 upgrade/audit behavior retained. |
| `src/functionalTest/java/dev/urlshort/click/ClickMetricsJourneyTest.java` | PASS — actual store failure and both metric surfaces; only static reason tags. |
| `src/functionalTest/java/dev/urlshort/click/ClickRecordingJourneyTest.java` | PASS — additive daily-field assertion; existing reduction/privacy guards preserved. |
| `src/functionalTest/java/dev/urlshort/click/ClickResilienceTrustedProxyJourneyTest.java` | PASS — inherited real-server resilience suite repeated with trusted-proxy configuration. |
| `src/functionalTest/java/dev/urlshort/click/ClickRetentionJourneyTest.java` | PASS — three disclosed shape additions; strict cutoff, snapshots and concurrency assertions retained. |
| `src/functionalTest/java/dev/urlshort/click/StatsJourneyTest.java` | PASS — daily-shape assertion only; legacy totals/ranking/errors/logs retained. |
| `src/functionalTest/java/dev/urlshort/click/StatsV2JourneyTest.java` | PASS — response-level unique/bot/day and document/legacy preservation assertions. |
| `src/functionalTest/java/dev/urlshort/click/TrustedProxyClickJourneyTest.java` | PASS — trusted chain/missing header, privacy and safe correlated events. |
| `src/main/java/dev/urlshort/click/ClickRecorder.java` | PASS — shared resolved client, static counters at outcome boundaries; unchanged bounded handoff. |
| `src/main/java/dev/urlshort/click/ClickStore.java` | PASS — parameterized one-snapshot UNION ALL, link/day-only distinct hash use. |
| `src/main/java/dev/urlshort/click/LinkStats.java` | PASS with M3S-01 LOW — retained-row fold correct; inherited totalClicks wording needs clarification. |
| `src/main/java/dev/urlshort/click/StatsController.java` | PASS — only v2 success description/example; error/API behavior unchanged. |
| `src/main/java/dev/urlshort/click/package-info.java` | PASS — package purpose updated to per-day unique and bot counts. |
| `src/main/java/dev/urlshort/web/RateLimitFilter.java` | PASS — granted public identity attribute; parser and actual peer preserved. |
| `src/test/java/dev/urlshort/click/ClickRecorderTest.java` | PASS — all loss reasons, zero registration, successful/failed/late commit counts; fresh suite. |
| `src/test/java/dev/urlshort/click/LinkStatsTest.java` | PASS — legacy fold and new day figures, empty/tie/top-ten behavior. |
| `src/test/java/dev/urlshort/web/RateLimitFilterTest.java` | PASS — shared attribute, same charged client, peer unchanged, exemption carries none. |
