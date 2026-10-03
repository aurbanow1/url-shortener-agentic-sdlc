# urlshort

A URL shortener (Spring Boot 4 · Java 21) built through a governed, agentic
SDLC running on [OpenRig](https://openrig.dev). The repository holds both the
product and the factory that built it, plus the evidence trail of every stage.

## Run the product (no OpenRig needed)

```sh
scripts/gw check               # unit + functional suites, JaCoCo 100% line/branch gate (JDK 21 pinned)
scripts/gw bootJar && java -jar build/libs/urlshort.jar
scripts/smoke.sh               # health → GET /api/ping → POST 405 problem detail → OpenAPI, against the running instance
# health: http://localhost:8080/actuator/health   ping: http://localhost:8080/api/ping   OpenAPI: http://localhost:8080/v3/api-docs
```

As a container: `docker compose up -d --build` (multi-stage `Dockerfile`, JRE 21,
H2 file database in the `urlshort-data` volume; `compose.yaml` publishes port
8080 on all interfaces), then `scripts/smoke.sh`. What shipped in each mission,
with its evidence and known gaps, is in `missions/<mission>/RELEASE.md`.

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
