# Dogfood Report: urlshort analytics v2

| Field | Value |
|---|---|
| Date | 2026-10-04 UTC |
| App URL | localhost:18232 (default), localhost:18233 (trusted loopback proxy) |
| Session | qa-agent@urlshort-factory, Codex |
| Subject | 50ad9c3ab9e65baa4100ede1772b514322957fa5, preserved pre-D21 jar |
| Scope | Anonymous creator, visitor, analyst and operator API journeys |
| Use interval | 01:00:34–01:20:47 UTC, 20 minutes 13 seconds; one same-day restart |

**Verdict: PASS within the observed dogfood scope. No new defect reproduced.** This is an installed-artifact exploration, not a new slice acceptance or proof item 12 judgment.

## Summary

| Severity | New findings |
|---|---|
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 0 |
| Total | 0 |

The preserved jar digest independently matches fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2 before and after use. Normal operator settings only: two loopback ports, disposable H2 file databases, explicit public base URLs, and the documented trusted-proxies setting on the second instance. No clock/peer wrapper, QA endpoint, product edit, or alternate build was used. The API-only request uses wire captures and structured logs; browser screenshots/video do not apply.

## Issues

No new issue filed. The previously recorded M3S-01 totalClicks wording gap remains the lead's release backlog; this report interprets totals as retained rows, as SPEC rule 7 requires. The artifact precedes D21; this dogfood does not judge that later slice or its configuration changes.

## Observed journeys

| Persona / journey | Observed effect and capture names |
|---|---|
| Creator | Same key/URL returns the same code; mismatched URL gives422 and preserves its binding. An unkeyed duplicate produces another code. Forged Host does not alter the configured shortUrl. Retrying after retirement returns the same retired link. See default-create, default-mismatch, default-retry-after-mismatch, default-unkeyed-duplicate and default-retired-idempotent-retry. |
| Visitor / analyst | Initial seven-click campaign yields raw7/bot3 in both instances; default trust gives unique1, trusted loopback gives unique4. One client using browser and bot agents shares a unique; other/unknown agents remain included. Empty links have the exact unchanged empty body. See *-stats-empty and *-stats-mixed. |
| Proxy operator | Trusted identity follows the rightmost untrusted XFF entry and skips a trailing trusted hop. Missing/all-trusted/empty XFF falls back to the peer. Forwarded and X-Real-IP do not select a click identity. Default trust ignores varied XFF. See trusted-stats-chains. |
| Referrers | Origin reduction removes paths, queries, fragments and userinfo; invalid/non-http values contribute no referrer. Twelve origins produce the top10 ordered by count and lexical ties; unknown timezone/date queries leave UTC semantics unchanged. See trusted-ranking-stats. |
| Concurrent and returning visitors | All100 concurrent redirects return302 with the stored Location, no-store and empty body; statistics settle at112 raw/15 unique/23 bot. Ten paced browser/bot sequences reach142/17/33; the recorded counter reaches157 across both links. See concurrent-*, paced-*-analyst and restart-before-*. |
| Retirement | Visitor410 and repeat-retire410 add no clicks; retired statistics stay7 and an unopened duplicate stays0. Mutations appear once in audit with matching requestId and exact before/after state. See default-retire, default-audit-retirement and default-final-audit. |
| Visitor error/privacy paths | 400 malformed/missing/bad URL or key;413 oversized body;415 text body;404 unknown code;405 wrong method;422 mismatch. Browser error requests still receive problem details. Server-generated request IDs replace inbound IDs; error bodies and logs omit submitted privacy canaries. See error-*, missing-*, stats-post, visitor-post and actual-empty-key-header. |
| Operator audit | XFF/Forwarded refuse403 under both settings. Cursor traversal returns the five pre-existing mutations exactly once, excludes a later write, and a fresh traversal sees it. Malformed limit/cursor returns400. Clicks and reads add no audit rows. See *-audit-spoof-*, default-audit-page*, default-audit-fresh and restart-final-audit. |
| HEAD / OPTIONS | Four Java HEAD wire checks show0 body bytes; HEAD/OPTIONS and other non-redirect paths do not add clicks. See http/*.wire.json and default-stats-after-errors. |
| Rate limit / health | Fresh trusted client receives60x200 then15x429. Retry-After is positive and natural recovery returns200. Another forwarded client has its own API bucket. Health/readiness/metrics stay available during refusal. See limit-*. |
| Restart / persistence | Links, retained statistics and audit rows survive restart byte-for-byte as JSON values; process click counters reset to0. Revisiting the same address raises uniques17→18, the documented fresh-salt upper bound. Further browser/bot visits keep unique18 while reaching145 raw/34 bot; new-process recorded counter3 and lost0. See restart-after-*, restart-same-client-*, restart-returning-* and trusted-final-*. |
| Metrics / retention visibility | Both counter families scrape with only the five static lost-reason labels; lost stays0. Startup purge logs declare90 days, cutoff2026-07-06, deleted0. This observes configuration/publication, not old-row deletion. See *-final-scrape and *-file.jsonl. |

## Evidence and verification

[Evidence directory](03-ambiguous-analytics/50ad9c3/README.md) includes request bodies, headers, responses, timestamps, console/file logs, settings and shutdown captures. [Final reconciliation](03-ambiguous-analytics/50ad9c3/verification-summary.json) verifies401 curl exchanges plus4 HEAD responses:94 default and311 trusted request-completed events in **each** sink, identical status and unique server-issued requestId, with no unaccounted events. Statistics retain the exact aggregate schema and raw total equals the daily sum. Privacy canaries are absent from captured error/statistics bodies and both complete request-log streams.

The220 exploratory comparisons have no remaining failure. The [raw manifest](03-ambiguous-analytics/50ad9c3/raw-manifest.json) and archived840 members verify; raw archive SHA-256 is cbacb225ad05677089f39ee32c07b7d876a77435760956c8b2fef6f9d0bc83c4. Display headers/logs and Prometheus HELP trailing whitespace are normalized; original wire bytes remain hashed in the archive and reconcile with the unchanged ledger. Run the included verify.py as described in its README.

### Instrument qualifications

Initial probe assumptions were corrected against the SPEC and captured inputs: audit refuses XFF/Forwarded specifically; successful management JSON sent Accept:text/html has normal406 negotiation; the pagination baseline had five mutations after two exploratory creates. These original assumptions remain in checks.json. Curl -H 'Idempotency-Key: ' omits the header; -H 'Idempotency-Key;' sends a truly empty header and correctly gets400. During restart, the launch tool truncated four startup console lines before any HTTP request was sent; subsequent request events are complete, and startup is complete in the supplemental file sink.

## Limits and self-check

- Natural UTC midnight,24-hour idempotency expiry,90-day old-row deletion and injected click/audit store failures were not exercised. Lost-click reasons were observed at0; increments under induced failure were not re-proven.
- Salt memory/disposal/non-persistence and stored click-hash contents cannot be established by these API journeys. No cross-day identity or security-review judgment is claimed.
- No Docker/UI, production proxy deployment, external target fetch, calibrated p95/p99 benchmark, or fresh full Gradle gate was run. The release-provided gate was read as provenance, not claimed as my rerun. Proof12 remains with its assigned return.
- Request/log correlation and raw hashes were read and reconciled after traffic ended. No product code or shipped tests were edited.
- Both instances were stopped; localhost18232 and18233 returned curl exit7 afterward. The preserved installed jar remains available for reproduction; only disposable data was used.

No new brownfield follow-on is required from this exploration.
