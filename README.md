# urlshort

A URL shortener (Spring Boot 4 · Java 21) built through a governed, agentic
SDLC running on [OpenRig](https://openrig.dev). The repository holds both the
product and the factory that built it, plus the evidence trail of every stage.

## Run the product (no OpenRig needed)

```sh
scripts/gw check               # unit + functional suites, JaCoCo 100% line/branch gate (JDK 21 pinned)
scripts/gw bootJar && java -jar build/libs/urlshort.jar
scripts/smoke.sh               # health, ping, create → redirect → read → stats → retire, error cases, metrics, OpenAPI
```

Try it: `curl -i -X POST -H 'Content-Type: application/json' -d '{"url":"https://example.com"}' http://localhost:8080/api/links`
returns a `code` and `shortUrl`; `GET /{code}` redirects (302); `GET /api/links/{code}/stats` counts clicks;
`GET /api/audit` pages through the audit trail (loopback only, and no setting opens it);
`DELETE /api/links/{code}` retires the link. Health `/actuator/health`, metrics `/actuator/prometheus`,
API document `/v3/api-docs`. Settings are environment variables (`URLSHORT_PUBLIC_BASE_URL`,
`URLSHORT_RATELIMIT_CREATEPERMINUTE`, `URLSHORT_RATELIMIT_REDIRECTPERMINUTE`,
`URLSHORT_RATELIMIT_TRUSTEDPROXIES`, `SPRING_DATASOURCE_URL`).

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

See `docs/SETUP-FACTORY.md`. In short: `rig up rig/rig.yaml --cwd "$PWD"`,
then approve gates in Mission Control (`rig ui open`) or with
`rig queue resolve <qitem> --decision "..."`.
