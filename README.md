# urlshort

A URL shortener (Spring Boot 4.1 · Java 21 · H2 + Flyway) built end to end by a
governed, agentic SDLC running on [OpenRig](https://openrig.dev) 0.6.3. This
repository holds three things:

- **the product**: `src/`, runnable without OpenRig;
- **the factory** that built it: `rig/`, twelve agent seats, workflows and
  human gates;
- **the evidence trail** of every step: specs, designs, reviews, QA proof,
  releases, traces and metrics.

**Status:** four missions shipped and closed: a dry run, a greenfield core, a
brownfield change of the shipped code, and an ambiguous analytics request.
The final product passes 268 unit and 322 functional tests with 100 % line
and branch coverage (583/583, 206/206). Every slice's proof is accepted
(123/123). GitHub Actions runs the same gate on every pull request.

**Start here:** [docs/FINAL-SUMMARY.md](docs/FINAL-SUMMARY.md) (what was built,
how, and its limits) → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (the
factory) → [docs/scenarios/](docs/scenarios/) (the three scenarios as
narratives).

## Quick start (no OpenRig needed)

You need a JDK 21. `scripts/gw` pins Homebrew's `openjdk@21`; elsewhere set
`URLSHORT_JAVA_HOME`, or run `./gradlew` with `JAVA_HOME` pointing at a JDK 21.
Docker is optional.

```sh
scripts/gw check          # unit + functional suites, 100 % line/branch coverage gate, Javadoc doclint
scripts/gw bootJar && java -jar build/libs/urlshort.jar --server.address=127.0.0.1
scripts/smoke.sh          # the whole public journey against the running instance
```

Or in a container: `docker compose up -d --build`, then `scripts/smoke.sh`. The
image uses a multi-stage `Dockerfile` with a JRE 21. It runs non-root on a
read-only filesystem, keeps the H2 file database in the `urlshort-data` volume,
and is published on `127.0.0.1:8080` only.

```sh
curl -i -X POST -H 'Content-Type: application/json' \
     -d '{"url":"https://example.com"}' http://localhost:8080/api/links   # 201, {"code": …, "shortUrl": …}
curl -i http://localhost:8080/<code>                                     # 302 to the target
curl -s http://localhost:8080/api/links/<code>/stats                     # clicks per UTC day
```

## API

The OpenAPI document is served at `/v3/api-docs`, committed as
[docs/api/openapi.json](docs/api/openapi.json), and browsable at
`/swagger-ui.html`. Every error is an RFC 9457 problem (`application/problem+json`).

| Endpoint | Does | Main responses |
|---|---|---|
| `POST /api/links` | create a short link for an `http(s)` URL; optional `Idempotency-Key` header makes a repeat within 24 h return the same link | `201` + `Location`; `400`, `413` (body over 16 KiB), `415`, `422` (key reused with another URL) |
| `GET /{code}` | redirect a visitor and record a privacy-reduced click | `302` (`Cache-Control: no-store`); `404`; `410` once retired |
| `GET /api/links/{code}` | read a link, retired ones included | `200` (`state` is `active` or `retired`); `404` |
| `DELETE /api/links/{code}` | retire a link (the record is kept) | `204`; `404`; `410` if already retired |
| `GET /api/links/{code}/stats` | statistics: `totalClicks`, `clicksPerDay` (`date`, `clicks`, `uniqueVisitors`, `botClicks`), `topReferrers` | `200`; `404` |
| `GET /api/audit?limit=&cursor=` | the audit trail, newest first, keyset pages (`limit` 1–100, default 50) | `200`; `403` unless direct loopback (below) |
| `GET /api/ping` | a trivial API answer: `status` `ok` and the server time | `200` |
| `/actuator/health`, `…/health/liveness`, `…/health/readiness`, `/actuator/prometheus` | operations | not rate limited |

Each client gets 60 `/api` requests and 600 redirects per minute (GCRA);
above that the answer is `429` with `Retry-After`.

## Configuration

All settings are environment variables with safe defaults.

| Variable | Default | Meaning |
|---|---|---|
| `URLSHORT_PUBLIC_BASE_URL` | `http://localhost:8080` | base of every `shortUrl`; `Host` and forwarding headers are never used |
| `URLSHORT_RATELIMIT_CREATEPERMINUTE` | `60` | `/api` budget per client |
| `URLSHORT_RATELIMIT_REDIRECTPERMINUTE` | `600` | redirect budget per client |
| `URLSHORT_RATELIMIT_TRUSTEDPROXIES` | empty | comma-separated exact peer addresses whose `X-Forwarded-For` is trusted |
| `URLSHORT_CLICK_RETENTIONDAYS` | `90` | clicks older than this many UTC days are purged |
| `URLSHORT_CLICK_PURGEENABLED` | `true` | `false` holds both purges and logs the hold at every start |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/urlshort;…` | the H2 file database |

The [runbook](docs/RUNBOOK.md) covers starting, checking and stopping the
service, every setting, reading analytics and diagnosing lost clicks.

## Behaviour worth knowing

- **Client identity.** The rate limiter and click uniques both use the
  connection's peer address. When the peer is a listed trusted proxy, they use
  the rightmost `X-Forwarded-For` entry that is not itself trusted. No other
  header supplies identity. One component, `web/ClientIdentity`, answers this
  question for the whole service
  ([ADR-0015](docs/adr/0015-client-identity-trusted-proxies.md)).
- **Analytics.** A click stores four reduced facts: the referrer's origin, a
  user-agent class, an HMAC of the client address under a salt that changes
  every UTC day, and the instant. It never stores the raw address, user
  agent, full referrer or request id. Unique visitors are counted within one
  UTC day only, and a restart during the day starts a new salt. Bots stay in
  every count, and `botClicks` also counts them separately. There is no
  cross-day unique count, CSV export, dashboard or time-zone option
  ([analytics contract](missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md)).
- **Retention.** The purge runs at startup and daily at 00:10 UTC. It deletes
  clicks strictly before today's UTC date minus the retention period. All
  statistics, `totalClicks` included, count retained rows, not a lifetime
  total.
- **Audit read.** Every create and retire is written to an append-only audit
  log in the same transaction. `GET /api/audit` is served **only to a direct
  loopback peer that sends no `X-Forwarded-For` or `Forwarded` header**, and
  **no setting opens it beyond loopback**. Trust boundary: the service sees
  only the connection address and the headers, so do not relay `/api/audit`
  through a local proxy, or have the proxy add a forwarding header, which then
  refuses. Setting any forwarded-header or Tomcat remote-IP option closes the
  read entirely ([ADR-0019](docs/adr/0019-audit-read-loopback-keyset.md),
  [design](docs/DESIGN.md)).
- **Observability.** Logs are ECS JSON lines, each carrying a server-issued
  `requestId` and never a raw IP or user agent. Prometheus exposes request
  metrics plus `urlshort_clicks_recorded_total`,
  `urlshort_clicks_lost_total{reason}` and
  `urlshort_ratelimit_rejections_total{budget}`.
- **Schema.** Flyway owns it: `V1` link and audit log, `V2` click, `V3` and
  `V4` audit columns (`created_at`/`updated_at`/`created_by`/`updated_by`).
  `V3` and `V4` have a written rollback, rehearsed on a copy of real data
  ([mission 02 release](missions/02-brownfield/RELEASE.md)); Flyway Community
  cannot undo `V1` and `V2`, as mission 01's release records.

## Tests, quality gate and CI/CD

- `scripts/gw check` is the gate. It runs the unit suite (`src/test`) and the
  HTTP journeys against a temporary H2 database (`src/functionalTest`). It
  enforces 100 % line and branch coverage on the merged data and Javadoc
  doclint on every public type. Any honest exclusion is written in
  [docs/qa/GAPS.md](docs/qa/GAPS.md), never configured away.
- [docs/TESTING.md](docs/TESTING.md) describes the strategy.
  [docs/qa/TRACEABILITY.md](docs/qa/TRACEABILITY.md) maps each acceptance
  criterion to the tests that prove it.
- `scripts/smoke.sh` has more modes (`--jar`, `--inspect`, `--restart`,
  `--drain`, `--bench`), with their prerequisites listed in its header.
- [ci.yml](.github/workflows/ci.yml) runs the gate on every pull request and on
  pushes to `main`. [cd.yml](.github/workflows/cd.yml) builds the jar, smokes
  it on loopback and builds the image; it publishes nothing.
  [Dependabot](.github/dependabot.yml) proposes updates weekly. See the
  [CI/CD guide](docs/guidance/ci-cd.md).

## Repository map

| Path | What |
|---|---|
| `src/` | the product: `link`, `click`, `audit`, `web`, `ping` packages; Flyway migrations in `src/main/resources/db/migration` |
| `missions/<mission>/` | one mission's `SPEC.md`, `RELEASE.md` and `NOTES.md`; each slice's `SPEC.md`, `design.md`, `PROOF.md` and `proof/` |
| `docs/` | architecture, design, ADRs, governance, testing, risks, runbook, guidance |
| `docs/review/`, `docs/qa/` | every review with its findings and resolutions; coverage, traceability and gaps |
| `docs/evidence/<mission>/` | exported workflow traces, queue transitions and gate decisions (`INDEX.md` per mission) |
| `docs/metrics/` | reliability metrics derived from the engine's records: success rate, retries, rollbacks, MTTR, latency, human wait |
| `rig/` | the factory: rig spec, seat roles, workflows, culture |
| `docs/factory/` | the rig as a shareable OpenRig bundle |
| `PLAN.md` | the plan of record and the decision log (D1–D21) |

## How it was built

Every change went through one governed lifecycle, with no agent judging its
own work:

`requirements → review → design → review → plan-lock → implement → QA → code
and security review → integrate`

The team is twelve agent seats on two model families: Claude for lead,
design and development, Codex for requirements, QA, review and release. Code
is always judged by the other model family. A human decided every mission
plan-lock, ambiguity and ship sign-off through parked queue gates, and agents
never push or publish.

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): the seats, workflows, gates
  and why.
- [docs/GOVERNANCE.md](docs/GOVERNANCE.md): who may decide what.
- [docs/review/REVIEW-LEDGER.md](docs/review/REVIEW-LEDGER.md): every review
  and its verdict.
- [docs/scenarios/drills.md](docs/scenarios/drills.md): rollback, rejection
  loops and safe-stop, exercised on purpose.

To run the factory yourself (OpenRig 0.6.3, with Claude Code and Codex logged
in), follow [docs/SETUP-FACTORY.md](docs/SETUP-FACTORY.md):
`rig up rig/rig.yaml --cwd "$PWD"`. Then watch and decide gates in Mission
Control (`rig tui`, or `rig ui open` for the browser), or with
`rig queue resolve <qitem> --decision "…"`.
