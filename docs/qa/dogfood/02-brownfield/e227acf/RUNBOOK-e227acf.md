# Service and factory runbook

Run these commands from the repository root. The service uses Java 21, a
single H2 file database and Flyway V1–V4. Its counting rules are in the
[analytics SPEC](../missions/03-ambiguous-analytics/slices/01-analytics-v2/SPEC.md);
candidate-specific proof, gaps and rollback commands are in each mission's
`RELEASE.md`. These are operator instructions, not an installed-smoke receipt.

## Start, check and stop the service

For a jar, keep the second command running in one terminal and run the third
in another:

```sh
scripts/gw check bootJar
java -jar build/libs/urlshort.jar --server.address=127.0.0.1 --server.port=8080
scripts/smoke.sh http://127.0.0.1:8080
```

Ctrl-C in the jar terminal requests graceful shutdown. For a container:

```sh
docker compose up -d --build
docker compose ps
scripts/smoke.sh http://127.0.0.1:8080
docker compose logs --tail 100 urlshort
docker compose down
```

Compose publishes only `127.0.0.1:8080`, uses a non-root/read-only image and
keeps H2 in its `urlshort-data` volume. `down` retains the volume; adding `-v`
destroys it. The jar's default database is under `data/` in its working
directory. Run one service against a given H2 directory at a time.

```sh
scripts/http -i http://127.0.0.1:8080/actuator/health/liveness
scripts/http -i http://127.0.0.1:8080/actuator/health/readiness
scripts/http http://127.0.0.1:8080/actuator/prometheus
```

Readiness includes the database; liveness does not. A failed readiness check
calls for inspecting startup/Flyway/H2 logs before restarting. Check for a
second process using the database and confirm its directory/permissions.
Stop the service before copying the complete data directory or volume for a
backup. Restore a stopped backup with its matching artifact and migration
history; follow that mission's rollback recipe, then check readiness and smoke.
Flyway Community has no automatic down-migration. Lowering retention can
delete history at the next startup: take a stopped backup first. Deleted rows
cannot be recovered from the aggregate statistics or process counters.

## Settings and client identity

| Environment variable | Default / effect |
|---|---|
| `URLSHORT_PUBLIC_BASE_URL` | `http://localhost:8080`; base of returned short URLs |
| `URLSHORT_RATELIMIT_CREATEPERMINUTE` | `60` per client for `/api` requests |
| `URLSHORT_RATELIMIT_REDIRECTPERMINUTE` | `600` per client for other limited requests |
| `URLSHORT_RATELIMIT_TRUSTEDPROXIES` | Empty; comma-separated exact peer addresses; controls both limiter identity and the address hashed for click uniques |
| `URLSHORT_CLICK_RETENTIONDAYS` | Positive whole number, default `90` UTC days |
| `URLSHORT_CLICK_PURGEENABLED` | `true`; `false` holds startup and daily deletion and logs a WARN at each start |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE` |

With no trusted peer, forwarding headers do not change client identity. From
an explicitly trusted peer, the rightmost non-empty `X-Forwarded-For` entry
that is not itself trusted is the client; if there is none, the peer is used.
`Forwarded` and `X-Real-IP` do not supply identity. The same resolved client is
charged by the limiter and hashed for clicks. Existing proxy hashes remain
unchanged; previously collapsed visitors cannot be separated retrospectively.

Audit reads use the connection peer separately: `/api/audit` requires direct
loopback, no `X-Forwarded-For` or `Forwarded`, and no enabled peer rewriting.
Keep `server.forward-headers-strategy=none` and both Tomcat `remoteip` header
settings unset. Proxy trust for analytics does not grant audit access. A host
request forwarded into a container may arrive as a non-loopback container
peer and be denied; the guard is intentional.

## Read analytics and diagnose losses

`GET /api/links/{code}/stats` returns `code`, `totalClicks`, `clicksPerDay` and
`topReferrers`. A day item has `date`, `clicks`, `uniqueVisitors`, `botClicks`.
Bots count in all raw/unique/referrer figures and in the extra bot count; the
classification uses the user-agent's bot/crawler/spider tokens, so bots posing
as browsers may count as browsers. There is no CSV, dashboard, cross-day
unique count or time-zone setting. Click writes are asynchronous and fail
open: a successful redirect does not guarantee that its click was stored.

Uniques compare hashes only within one UTC day. A same-day restart draws a new
salt and can count a returning client again. All figures use retained rows;
they are not lifetime totals. The startup purge and the first daily tick on
or after 00:10 UTC delete rows whose UTC day is strictly before today's day
minus the retention period. The cutoff day stays; an operator hold keeps all
rows until released. Purge logs are `clicks purged` (deleted count, cutoff,
retention days), `click purge failed`, or `click purge paused, no click is deleted`.

| Actuator metric | Prometheus series | Meaning |
|---|---|---|
| `urlshort.clicks.recorded` | `urlshort_clicks_recorded_total` | Every insert that returns; no labels |
| `urlshort.clicks.lost` | `urlshort_clicks_lost_total{reason="…"}` | Every `click lost` event, with only the static reason label |

The five exact loss reasons are `rejected`, `reduction failed`, `write failed`,
`shutdown deadline`, and `shutdown deadline, outcome unknown`. All are
registered at startup, including zero series. The counters reset with the
process and are not decremented by retention deletion. A write reported with
an unknown shutdown outcome may later return: that click appears in both
counters, so subtracting lost from recorded does not yield stored clicks.
Use rates/deltas and the correlated `click lost` WARN (`requestId`, `reason`,
exception class); these counters contain no client, link, referrer or user-agent
labels. Investigate `rejected` as queue/closing pressure, `write failed` as a
database fault, and shutdown reasons in the shutdown log. Keep the original
evidence when the outcome is unknown.

## Start, stop and recover the factory

```sh
rig up rig/rig.yaml --cwd "$PWD" --plan
rig up rig/rig.yaml --cwd "$PWD"
rig tui
rig ps --nodes --rig urlshort-factory
rig view show held
```

`rig tui --shared` joins the existing kernel terminal; detach with Ctrl-b d.
`rig ui open` opens the browser UI. Inspect an owed item with
`rig queue show <qitem> --full`; inspect its history with
`rig queue transitions <qitem> --json` and its instance with
`rig workflow trace <instance> --json`. Resume a packet from its recorded
continuation. A chat message informs; a queue handoff transfers custody.

```sh
rig down urlshort-factory --snapshot
rig up urlshort-factory --existing --cwd "$PWD"
```

Save the returned snapshot reference and inspect seats/owned queues after
resuming. If a seat fails, inspect `rig seat status <session>` and its owned
queue before the lead routes work or relaunches it. Use the
[factory setup](SETUP-FACTORY.md) for prerequisites and human gate decisions,
and the [drill records](scenarios/drills.md) for observed stop/route/resume
behavior. Do not run fault drills against a live product instance.

Service logs go to the jar terminal or `docker compose logs`; request events
are structured JSON with `requestId`. Live service metrics are on Actuator and
Prometheus. Factory seat transcripts are read with `rig transcript <session>`;
queue/trail evidence lives in `docs/evidence/<mission>/`, and derived factory
reliability metrics and their limits live in [docs/metrics](metrics/README.md).
