# urlshort

A URL shortener (Spring Boot 4 · Java 21) built through a governed, agentic
SDLC running on [OpenRig](https://openrig.dev). The repository holds both the
product and the factory that built it, plus the evidence trail of every stage.

## Run the product (no OpenRig needed)

```sh
scripts/gw check               # unit + functional suites, JaCoCo 100% line/branch gate (JDK 21 pinned)
scripts/gw bootJar && java -jar build/libs/urlshort.jar --server.address=127.0.0.1
scripts/smoke.sh               # health, ping, create → redirect → read → stats → retire, error cases, metrics, OpenAPI
```

Try it: `curl -i -X POST -H 'Content-Type: application/json' -d '{"url":"https://example.com"}' http://localhost:8080/api/links`
returns a `code` and `shortUrl`; `GET /{code}` redirects (302); `GET /api/links/{code}/stats` counts clicks;
`GET /api/audit` pages through the audit trail (direct loopback only; forwarding headers deny access);
`DELETE /api/links/{code}` retires the link. Health `/actuator/health`, metrics `/actuator/prometheus`,
API document `/v3/api-docs`. Settings are environment variables (`URLSHORT_PUBLIC_BASE_URL`,
`URLSHORT_RATELIMIT_CREATEPERMINUTE`, `URLSHORT_RATELIMIT_REDIRECTPERMINUTE`,
`URLSHORT_RATELIMIT_TRUSTEDPROXIES`, `URLSHORT_CLICK_RETENTIONDAYS`,
`URLSHORT_CLICK_PURGEENABLED`, `SPRING_DATASOURCE_URL`).

Statistics keep `code`, `totalClicks`, `clicksPerDay` and `topReferrers`.
Each UTC-day item has `date`, `clicks`, `uniqueVisitors` and `botClicks`.
Bots remain in raw clicks, unique visitors and referrer counts; `botClicks`
also counts them separately. Uniques count distinct daily-salted client hashes
within that day only. A restart during the day creates a new salt, so a returning
client can count again. There is no cross-day unique count, CSV export,
dashboard or time-zone option.

All statistics count retained click rows. By default the purge runs at startup
and daily from 00:10 UTC, deleting rows strictly before today's UTC date minus
90 days; the cutoff day remains. `totalClicks` is therefore a retained-row
total, not a lifetime total. `URLSHORT_CLICK_PURGEENABLED=false` pauses both
purges and logs the hold at startup.

`URLSHORT_RATELIMIT_TRUSTEDPROXIES` is a comma-separated list of exact peer
addresses, empty by default. Both the limiter and click hashing use the peer
unless it is listed; then they use the rightmost non-empty `X-Forwarded-For`
entry that is not itself trusted, falling back to the peer. No other forwarding
header supplies identity. Previously stored proxy hashes are not rewritten or
split into individual visitors.

Click counters are `urlshort.clicks.recorded` and `urlshort.clicks.lost` on
Actuator; Prometheus exposes `urlshort_clicks_recorded_total` and
`urlshort_clicks_lost_total{reason="…"}`. Recorded counts inserts that return;
lost counts `click lost` events with fixed reason labels. A write reported as
unknown at shutdown can later return and appear in both counters. See the
[runbook](docs/RUNBOOK.md) for reasons, settings and recovery, and the
[analytics contract](missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md)
for the counting rules.

As a container: `docker compose up -d --build` (multi-stage `Dockerfile`, JRE 21, non-root on a
read-only filesystem, H2 file database in the `urlshort-data` volume, published on `127.0.0.1:8080`
only), then `scripts/smoke.sh`. The smoke script's other modes (`--jar`, `--inspect`, `--restart`,
`--drain`, `--bench`) and their host prerequisites are listed in its header. What shipped in each mission, with its evidence and
known gaps, is in `missions/<mission>/RELEASE.md`.

## Read the SDLC evidence (no OpenRig needed)

| Question | Where |
|---|---|
| What was asked, decided, built, reviewed, proven? | `missions/<mission>/slices/<slice>/{SPEC.md,design.md,PROOF.md,proof/}` |
| What shipped, installed-smoke and advisory results, known gaps, rollback | `missions/<mission>/RELEASE.md` |
| How is the factory designed and governed? | `PLAN.md`, `docs/ARCHITECTURE.md`, `docs/GOVERNANCE.md`, `rig/` |
| Code review results and resolutions | `docs/review/` |
| Coverage reports, traceability, honest gaps | `docs/qa/` |
| Orchestration traces, queue transitions, proof judgments | `docs/evidence/<mission>/` |
| Reliability metrics (success rate, retries, rollbacks, MTTR, latency) | `docs/metrics/` |

## Run the factory (OpenRig 0.6.3, Claude Code + Codex logged in)

See [factory setup](docs/SETUP-FACTORY.md). Run `rig up rig/rig.yaml --cwd "$PWD"`,
then inspect gates in Mission Control (`rig tui`; `rig ui open` for the browser) or decide with
`rig queue resolve <qitem> --decision "..."`.
