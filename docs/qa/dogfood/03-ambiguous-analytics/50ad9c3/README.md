# Installed artifact dogfood evidence

Subject: 50ad9c3ab9e65baa4100ede1772b514322957fa5, preserved jar SHA-256 fbe67b61f0f6f668e65949bc01a92802e5de64015ca78eb2d6bbc0462b2ebde2. See [the report](../../03-ambiguous-analytics.md).

runtime-settings.json records the normal operator settings. No clock, peer wrapper, fault endpoint, product edit, or alternate build is used. Both databases are disposable file H2 stores under /private/tmp/urlshort-m03-dogfood-50ad9c3/.

http-ledger.json records every curl exchange (method, URL, submitted headers, time interval, status, response body and headers); corresponding http/<name>.request files preserve JSON/text request bytes where used. All create requests used application/json except error-text (text/plain). Each Java HEAD exchange has a separate http/<name>.wire.json including actual body byte length; HeadWireProbe.java is its probe source.

default-console.jsonl and trusted-console.jsonl capture PTY console output. Supplemental *-file.jsonl sinks use ECS format and include startup/shutdown. The restarted instance's initial launch call truncated four startup lines; that portion is absent from console capture, but no HTTP request had yet been sent. Subsequent request events are reconciled completely against both sinks. Startup logs are complete in the supplemental file.

checks.json preserves the exploratory comparisons, including corrected probe assumptions. During paced traffic, another orchestration cell captured rate-limit exchanges; their two ledgers were merged by unique request name after traffic completed. Final verification requires one matching request-completed event per response, identical status and unique server-issued requestId, and no unaccounted events.

raw-captures.tar.gz preserves HTTP capture files and both log sinks before display whitespace normalization. raw-manifest.json hashes each original member and the archive. Display headers/logs have CRLF and trailing whitespace normalized for Git. Prometheus display bodies also trim empty HELP-description trailing whitespace. JSON/request body bytes and the full ledger remain unchanged; original Prometheus bytes reconcile with the ledger through the archive.

Run python3 docs/qa/dogfood/03-ambiguous-analytics/50ad9c3/verify.py from the repository root to reconcile captures, logs, statistics shapes, problem details, checks and the locally preserved jar digest. It writes verification-summary.json. An absent jar after temporary artifact cleanup will need the same preserved jar restored at runtime-settings.json's installedJar path.

This artifact is bounded dogfood evidence. It does not supply or accept the NFR-L1 benchmark, a full quality-gate rerun, salt-storage review, injected store failures, or controlled-clock expiry/retention checks.
