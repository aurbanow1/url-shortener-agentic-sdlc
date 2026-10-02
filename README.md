# urlshort

A URL shortener (Spring Boot 4 · Java 21) built through a governed, agentic
SDLC running on [OpenRig](https://openrig.dev). The repository holds both the
product and the factory that built it, plus the evidence trail of every stage.

## Run the product (no OpenRig needed)

```sh
source scripts/env.sh          # JDK 21 + repo-local Gradle home
./gradlew check                # unit + functional suites, JaCoCo 100% line/branch gate
./gradlew bootJar && java -jar build/libs/urlshort.jar
# health: http://localhost:8080/actuator/health   OpenAPI: http://localhost:8080/v3/api-docs
```

`scripts/smoke.sh` exercises the public journey against a running instance.

## Read the SDLC evidence (no OpenRig needed)

| Question | Where |
|---|---|
| What was asked, decided, built, reviewed, proven? | `missions/<mission>/slices/<slice>/{SPEC.md,design.md,PROOF.md,proof/}` |
| How is the factory designed and governed? | `PLAN.md`, `docs/ARCHITECTURE.md`, `docs/GOVERNANCE.md`, `rig/` |
| Code review results and resolutions | `docs/review/` |
| Coverage reports, traceability, honest gaps | `docs/qa/` |
| Orchestration traces, queue transitions, proof judgments | `docs/evidence/<mission>/` |
| Reliability metrics (success rate, retries, rollbacks, MTTR, latency) | `docs/metrics/` |

## Run the factory (OpenRig 0.6.3, Claude Code + Codex logged in)

See `docs/SETUP-FACTORY.md`. In short: `rig up rig/rig.yaml --cwd "$PWD"`,
then approve gates in Mission Control (`rig ui open`) or with
`rig queue resolve <qitem> --decision "..."`.
