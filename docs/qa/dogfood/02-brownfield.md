# Dogfood Report: mission 02 installed pre-run

| Field | Value |
|---|---|
| Date | 2026-10-04 UTC |
| Session | qa-agent@urlshort-factory, Codex |
| Packet | qitem-20261004012639-c8669151; ordinary installed-artifact request |
| Subject | e227acf04e1ea902406532fcebd741f8a86f7f65, D21 absent |
| App URL | localhost:18234; isolated invalid-setting probe18235 |
| Scope | Audit reader, retention operator settings, stopped backup/restore, problem/API/metric documentation; explicitly reused byte-equivalent analytics journeys |

**Verdict: PASS within the bounded pre-run dogfood scope. No new defect reproduced.** This result does not accept D21 or judge a proof contract. The packet was re-read at01:36Z after the lead's recovery instruction; jar equivalence, reused evidence, new effects and documentation were freshly checked.

## Summary

| Severity | New findings |
|---|---|
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 0 |
| Total | 0 |

## Artifact equivalence and reuse

An independent byte comparison confirms /private/tmp/urlshort-mission02-e227acf.jar equals /private/tmp/urlshort-mission03-50ad9c3.jar:39,635,446 bytes; both SHA-256 fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2. [Equivalence record](02-brownfield/e227acf/jar-equivalence.json) binds the two candidate labels; the comparison establishes jar bytes, not document or future-candidate equivalence.

As the packet allows, the [20-minute mission03 exploration](03-ambiguous-analytics.md), committed8cf894a, supplies the analytics campaign, quota recovery, visitor error/privacy paths and same-day restart observations. At01:36Z I freshly reconciled all401 curl exchanges plus4 HEAD wire responses, their exact status/requestId events in both sinks, and840 archived member hashes. I also inspected selected mixed-statistics, audit, quota and scrape contents. [Reused evidence record](02-brownfield/e227acf/reused-verification.json) retains the fresh reconciliation and selected exchanges. No second analytics campaign was run or implied.

## Fresh bounded checks

| Journey | Observed effect | Captures |
|---|---|---|
| Empty audit / mutations | Empty page is exactly items[]/next:null. Three creates and one retire appear newest first by sequential write, with exactly eight public row fields and matching mutation requestIds. | audit-empty, create-*, retire-second, audit-before-reads |
| Pagination | Two-page traversal returns its four prior rows exactly once and excludes a create made after the first page. Later read has all five rows. | audit-page1, new-write-after-page1, audit-page2, audit-after-reads |
| Refusal / errors | XFF, Forwarded and an explicitly empty XFF header refuse403. POST audit gives405; bad limit/cursor gives400; unknown stats404; retired visitor410. Browser Accept still gets a problem, correct request-id instance and no submitted canary/trail content. | audit-xff, audit-forwarded, audit-empty-xff, audit-post, audit-limit-*, audit-cursor-bad, missing-stats, retired-visitor |
| Read-only effect | Audit reads, error requests, stats and an active redirect add no audit row. One successful redirect records one click and origin-only referrer. | active-visitor, hold-click-stats, audit-after-reads |
| Operator hold | CLI retention-days7 and purge-enabled:false start healthy, emit one WARN naming the paused setting/7 days, and permit normal requests. | hold-health, purge-events.json |
| Stopped backup / restore | Process stopped and port refused connections before copying its disposable H2 directory. Restored backup starts with the same jar and released hold. Five audit rows and click statistics are identical; process recorded counter resets0. | settings.json, restored-health, restored-audit, restored-stats, restored-recorded |
| Released hold | Startup INFO reports retentionDays7, cutoff2026-09-27 and deleted0. The fresh click, links and audit rows remain. This observes the configured cutoff and retained fresh data, not old-row deletion. | purge-events.json |
| Invalid period | Separate disposable startup with retention-days0 exits1. Failure report names the setting origin, rejected value0 and reason must be greater than0. Port18235 is closed afterward. | invalid-retention-console.txt, shutdown.json |
| Problem / API document | Live OpenAPI equals the pinned e227acf committed document, including optional field/rule/message errors and audit200/400/403/429/500. | live-openapi.json, pinned-openapi.json |
| Metrics / documentation | recorded is1 before restart with no tags; lost is0 with the five exact documented static reasons. Both families scrape. Pinned Runbook setting effects and counter names agree; its retained-row and reset descriptions match observed behavior. Pinned DESIGN's Operator boundary and ADR-0019 explicitly qualify headerless local relays. | recorded, lost, scrape, RUNBOOK-e227acf.md, documentation-check.json |

No new issue is filed. Existing release/backlog records remain their owners' work.

## Evidence and verification

[Evidence directory](02-brownfield/e227acf/README.md) contains30 fresh HTTP exchanges,32 comparisons, console/file logs, exact pinned API/Runbook copies, settings, purge/invalid-start records, shutdown and raw archive. [Fresh verifier result](02-brownfield/e227acf/verification-summary.json) has no failure:30/30 exact status/requestId correlations in each sink, no unaccounted responses and no request canary in either log stream. All67 raw members verify; archive SHA-2566091aa5f49113649c95243b1e3a0c452ce7af1f9c0c2bae7204c2495848240f6.

Raw bytes are preserved before display whitespace normalization. Headers/logs, the invalid-start display and Prometheus HELP trailing whitespace are normalized; original header/body bytes reconcile against the unchanged ledger and archive. The initial invalid-period probe looked for the word positive; the actual report's must be greater than0 expresses the required rule. That probe correction is retained in checks.json and no product change was made.

## Limits and self-check

- Tested CLI property forms; reviewed the documented environment-variable names without separately executing environment overrides.
- No artificially aged click rows, controlled-clock midnight/daily run, forced store failure, schema/actor-column query, container restore, downgrade, CI provider, or calibrated latency benchmark was exercised here. Startup delete0 does not prove a90-day boundary deletion.
- Audit is qualified to the peer and headers seen by the service. No headerless local-relay deployment or external exposure was attempted. Salt storage/lifecycle and hash internals are outside these API observations.
- The original packet's jar was copied and its equivalence freshly verified. Only disposable local databases were used. No product code, shipped test, threshold, proof judgment or global QA gap was changed.
- Normal instances stopped by Ctrl-C; invalid instance exited1. Ports18234/18235 both returned curl exit7 afterward.
- No fresh Gradle gate or final D21 acceptance is claimed. This ordinary pre-run request closes with no new brownfield follow-on.
