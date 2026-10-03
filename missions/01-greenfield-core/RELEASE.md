# RELEASE — 01-greenfield-core (Greenfield: core URL shortener)

Prepared by `release-agent@urlshort-factory` (Claude Code, Opus 5.5), 2026-10-03 UTC,
on packet `qitem-20261003142414-ac899454` of lifecycle `01M3ZXEXAMS945ZS0QZ26KZK1V`
(step `release_prep`, after the wave review's PASS). The next step is `release_review`
by the Review Agent, then the human `ship_signoff` gate. Nothing here was pushed,
tagged or published; every service was bound to loopback and stopped afterwards.

## 1. Decision brief for ship sign-off

**What ships.** `main` at `f09010396d584fdf41702bb99856aa17a1ec1206`, whose product,
test, build and container inputs are byte-identical to the last slice merge
`8e9c065589e53385f60d6be3ddbc3683260285df` (`git diff --stat 8e9c065 HEAD -- src
build.gradle.kts settings.gradle.kts Dockerfile compose.yaml gradle gradlew .dockerignore`
is empty). Three slices, merged `--no-ff` in order:

| Slice | What it adds | Accepted candidate | Merge |
|---|---|---|---|
| `01-create-redirect` | `POST /api/links` (validation, idempotency key), `GET`/`DELETE /api/links/{code}`, `GET /{code}` 302 `no-store`, audit row in the same transaction, Flyway V1, problem details everywhere, committed OpenAPI document | `a922f49` | `16c355f` |
| `02-analytics` | asynchronous privacy-reduced click recording (referrer origin, user-agent class, daily-salted client hash), Flyway V2, `GET /api/links/{code}/stats` | `5b3490c` | `091ff46` |
| `03-operate` | per-client token-bucket rate limits with `429` + `Retry-After`, trusted-proxy rule, liveness/readiness, Prometheus metrics, 10 s graceful shutdown, non-root read-only container published on loopback | `1c8b2cf` | `8e9c065` |

**What was proven, and how.** A fresh gate on `f090103` (165 unit + 155 functional
tests, 0 failures, merged coverage 442/442 lines and 162/162 branches). The packaged
jar and the container image were run as a user would, on loopback, and every
release-level criterion of `03-operate` (AC-21 to AC-28) was exercised against them.
The latency targets were measured at the specified **offered** rates with a new
open-loop bench: redirects 100.0/s with p95 2.2 ms and p99 3.3 ms (NFR-L1: ≤ 20 / ≤ 50 ms),
creates 20.0/s with p95 2.8 ms (NFR-L2: ≤ 50 ms), no measurable added p95 from click
recording (NFR-L3: ≤ 2 ms). The OSV advisory check found 0 advisories on the 97
runtime dependencies. The secret scan found no credential in the repository.

**The one item that needs the human's judgment: AC-28 is NOT MET on this host.**
During `docker compose restart`, a request still sending its body through the
published port `127.0.0.1:8080` is cut (empty reply after the 10 s phase, 4 of 4 runs).
The service is not at fault on the evidence: the same request completes `201` when
sent inside the Docker VM through Docker's own published port (2 of 2) and from the
container's network namespace (2 of 2), and the jar's drain passes 3 of 3. The cut
is in the macOS-to-VM port forwarder of this laptop's Docker setup (Lima). Every
other AC-28 clause passes. The orchestration lead kept AC-28 as written
(`qitem-20261003151205-a94f1d92`): moving where it is measured after it failed would
narrow the claim, which is the human's call (§3.6).

**Proof status.** `rig proof show 01-greenfield-core --json` (exported in
`docs/evidence/01-greenfield-core/proof-readiness.json`): **not ready.** The three
release-owned items are pending their judges by design (they judge this record):
`01-create-redirect` item 14 (secret scan, `qa-agent`), `02-analytics` item 13
(NFR-L3, `qa-agent`), `03-operate` item 13 (AC-21 to AC-28, `qa2-agent`). Five
earlier acceptances read `unknown` because a cited file changed after the judgment:
`02-analytics` items on traceability, ADR index and `GAPS.md`, and `03-operate`'s
`GAPS.md` item (this step appended the release rows to `docs/qa/GAPS.md`; the
traceability and design documents were updated at the wave review). They need
re-affirmation by the same judges. Delivery stamps wait for all of them.

**Review verdicts** (all in [`../../docs/review/REVIEW-LEDGER.md`](../../docs/review/REVIEW-LEDGER.md)):
every slice passed QA and the combined code and security review on the same SHA
that was merged, after bounded review loops (§6). The accumulated wave review
passed on `7636264..8e9c065` from two vantages, with no MUST-FIX or HIGH, one
MEDIUM (W2-01) and four LOWs routed to the lead (`qitem-20261003142206-ff6ad817`).

**Human decisions honoured** (verbatim in `NOTES.md` §1):
- Mission plan-lock, 2026-10-03T04:39:53Z (`qitem-20261003042553-03ac8b4b`):
  "approve: four slices in three waves as briefed; the eight assumed rows (NFR-L1,
  L2, R2, R5, S3, S6, S2, P2) are confirmed as stated; dependency overrides land as
  the first gated commit of 01-create-redirect; FR-17 stays in this mission as
  04-audit-read". The overrides are `20aef57`, the first commit of `01-create-redirect`.
- Fast plan, about 05:30Z (relayed on `qitem-20261003052736-7830d02a`, plan-lock
  re-approved on the human's behalf, action `01M403RBRS6DQTXA1VW89AXQCA`): three
  slices in two waves, FR-17 moved to mission 02, FR-11/FR-12/NFR-S2 dropped, later
  slice plan-locks delegated to the lead. Shipped as decided: no alias, expiry or
  audit-read endpoint is in this release.
- D11, 06:05Z (`qitem-20261003055950-1b5ec0d3`): all slice plan-locks delegated to
  the lead; the human keeps mission plan-locks, ambiguity decisions and ship sign-offs.
- A-16 / DR-02 shutdown predicate (lead under D11, `qitem-20261003092511-dc120dcd`;
  "the human can reverse it before ship sign-off"): backlog connections the server
  never accepted are counted boundary losses, not failures. Measured here: 0 boundary
  losses in 3 jar drains (§3.4).

**Rollback in one line.** `git revert -m 1 8e9c065` (rehearsed, gate green) removes
`03-operate` with no data impact; reverting further means keeping the V1/V2 migration
files, because Flyway Community cannot undo them (§8).

**Recommended default: approve**, shipping `8e9c065` with AC-28's port-path R0 clause
recorded as a disclosed host gap of this laptop's Docker forwarder, after the three
pending proof items and the five re-affirmations are judged. The isolation points
outside the product; the service drains correctly on every path that reaches it
directly, and the shipped scope is a loopback single node. **Alternative: hold**
until AC-28 passes on a native Linux Docker host. Cost: this installation has no
such host, so the hold waits on new infrastructure and one more `release_prep` +
`release_review` pass; nothing in the product would change.

## 2. Artifact and gate

| Item | Value |
|---|---|
| `main` at build time | `f09010396d584fdf41702bb99856aa17a1ec1206`; product inputs identical to merge `8e9c065` (empty `git diff --stat` above). Later commits on `main` are documents, evidence and tooling only; the same diff command must stay empty |
| Gate run | `scripts/gw --log missions/01-greenfield-core/release/check-bootjar-f090103.txt check bootJar --rerun-tasks`: BUILD SUCCESSFUL in 33 s, 16 of 16 tasks executed, Javadoc gate green ([log](release/check-bootjar-f090103.txt)) |
| Tests from that run | unit 165 (18 classes), functional 155 (22 classes); 0 failures, 0 errors, 0 skipped (JUnit XML under `build/test-results`) |
| Coverage from that run | merged report `build/reports/jacoco/all`: lines 442/442, branches 162/162, methods 134/134, classes 41/41; `jacocoTestCoverageVerification` passed, no exclusion |
| Jar | `build/libs/urlshort.jar`, version 0.1.0, 39 613 260 bytes, git blob `1aaf1fc92b909e220fcefebc1c547611685ff891` |
| Image | `urlshort:local`, id `sha256:a38050b05d9090ec83cba905dda98f81c7573550c53710e89230fbba12ee3c33` (562 MB), built by `docker compose build` from the same tree ([log](release/docker-build-8e9c065.txt)); its jar is built inside the image from the same sources and was not compared byte for byte with the host jar |
| Toolchain | OpenJDK 21 (Homebrew `openjdk@21`), Gradle 9.7.1, Spring Boot 4.1.1, H2 2.4.240, Flyway 12.4.0, Docker Engine 28.4.0 (Ubuntu 24.04 in a Lima VM) with Compose 5.1.3, curl 8.7.1, Node 24.18.0, OpenRig 0.6.3; host macOS 15.2, Apple M4 Pro, 14 cores, 24 GiB |

## 3. Installed smoke

`scripts/smoke.sh` was brought up to the shipped surface in this step (its header
lists the modes and host prerequisites). All its HTTP goes through `scripts/http`
(loopback only) or `tools/bench.mjs` (refuses non-loopback URLs). Changes:

- the default smoke now runs the public journey: create (201, `code`, `shortUrl`) →
  redirect (302, `Location`, `Cache-Control: no-store`) → read (active) → stats (one
  click, referrer reduced to its origin) → invalid create (400 problem with `errors[]`,
  input not echoed) → unknown code (404) → retire (204) → redirect (410), besides health,
  ping, metric names, Prometheus and OpenAPI;
- `--jar` (AC-26) and `--inspect` (AC-22, AC-23, AC-28's stop timeout) are new;
- `--bench` was rewritten as an open-loop generator (§3.5);
- `--restart` now judges the held request R0 at the end of the run, so the load and
  persistence checks still report when R0 fails, and the run still ends `SMOKE FAIL`.
  A latent bug was fixed on the way: with every load response valid, `grep -v`
  exited 1 and `pipefail` ended the script silently after the R0 line;
- the script was edited while this step ran: the `--restart` changes (R0 judged
  at the end, the `grep -c` fix, the restart-window log) came after restart runs 1
  and 2, and the `--bench` link line and header text came after the jar and drain
  runs. Each output under `release/` was produced by the version current at that
  time, and the default journey, `--jar`, `--drain` and `--inspect` logic did not
  change after their runs. The committed file is the final version;
- the Perl timestamp runs under `LC_ALL=C`, which removes QA-OPR-03's host-locale
  dependency (W2-04). On this host the inherited `en_US.UTF-8` also works:
  `perl -MTime::HiRes=time -e 'printf "%d\n", time * 1000'` printed a timestamp.

### 3.1 Jar, configured by environment (AC-26)

`scripts/smoke.sh --jar build/libs/urlshort.jar 18091 <log>` started the jar on
`127.0.0.1:18091` with `URLSHORT_PUBLIC_BASE_URL=https://sho.rt.example`,
`URLSHORT_RATELIMIT_CREATEPERMINUTE=10`, `URLSHORT_RATELIMIT_REDIRECTPERMINUTE=100`,
`URLSHORT_RATELIMIT_TRUSTEDPROXIES=127.0.0.1` and `SPRING_DATASOURCE_URL` in a temp dir.
Result `SMOKE JAR OK` ([output](release/smoke-jar-ac26-f090103.txt), [log](release/jar-ac26-log-f090103.jsonl), 73 events):
full smoke passed; `shortUrl` was `https://sho.rt.example/dpMkbHzk`; the database file
appeared where `SPRING_DATASOURCE_URL` pointed; a client named by the trusted proxy's
`X-Forwarded-For: 192.0.2.10` was admitted exactly 10 times, then `429`. Graceful
shutdown completed on `SIGTERM`. The strings `192.0.2.10`, `alert(`,
`referrer.example` and `smoke-journey` occur nowhere in the log.

### 3.2 Container: compose up, smoke, inspection (AC-21, AC-22, AC-23)

`docker compose up -d --build` ([output](release/compose-up-f090103.txt)) → readiness
`UP` through `127.0.0.1:8080` → `docker compose ps`: `Up 5 seconds (healthy)
127.0.0.1:8080->8080/tcp`.

- `scripts/smoke.sh http://127.0.0.1:8080`: `SMOKE OK` ([output](release/smoke-container-f090103.txt)).
- `scripts/smoke.sh --inspect`: `SMOKE INSPECT OK` ([output](release/smoke-inspect-container-f090103.txt)):
  `docker compose port urlshort 8080` → `127.0.0.1:8080`; `PortBindings`
  `{"8080/tcp":[{"HostIp":"127.0.0.1","HostPort":"8080"}]}` (nothing on `0.0.0.0` or `::`);
  `User=urlshort`, `id` → `uid=10001(urlshort)`; `ReadonlyRootfs=true`; mounts: one
  named volume `url-shortener_urlshort-data` at `/app/data` plus the non-persistent
  `/tmp` tmpfs (A-14); `touch /app/smoke-probe` → `Read-only file system`;
  `touch /app/data/smoke-probe` → ok (removed again); `StopTimeout=20`;
  `Health=healthy` with the check `GET /actuator/health/readiness` expecting
  `"status":"UP"` (so health follows readiness, not an open port).
- Prometheus on the container ([scrape](release/container-prometheus-f090103.txt))
  still carries `disk_free_bytes{path="/app/."}` (W2-03, §7).

### 3.3 Restart and persistence (AC-24, AC-28)

`scripts/smoke.sh --restart http://127.0.0.1:8080 <log>` creates a link, runs a
load loop (one redirect per connection, about 8/s, inside the 600/min budget),
holds R0 (a create whose chunked body is completed 0.5 s after the restart is
issued), runs `docker compose restart`, then `docker compose down` (no `-v`) and
`up`.

| Run | R0 | Load responses outside 2xx/3xx | Connection failures through the port proxy (reported) | Link after restart and after down/up |
|---|---|---|---|---|
| [1](release/smoke-restart-container-f090103.txt) | curl exit 52 (empty reply) 10 485 ms after the stop | not reached (script stopped at R0, before this step's change) | — | — |
| [2](release/smoke-restart-container-f090103-run2.txt) | exit 52 at 10 489 ms | not reached | — | — |
| [3](release/smoke-restart-container-f090103-run3.txt) | exit 52 at 10 393 ms | 0 of 250 | 204 | `302` to the same target, `GET /api/links/{code}` unchanged, both |
| [4](release/smoke-restart-container-f090103-run4.txt) | exit 52 at 10 370 ms | 0 of 251 | 206 | same, both |

**AC-24 passes** (2 of 2 runs that reached it). **AC-28: stop timeout 20 s > 10 s
passes; 0 responses outside 2xx/3xx passes; connection failures reported; R0
fails 4 of 4. AC-28 is NOT MET on this host.**

The container log of run 4 ([log](release/container-restart-log-f090103.jsonl))
shows R0 dispatched: `Commencing graceful shutdown` at 15:02:29.748Z, then
`Shutdown phase … ends with 1 bean still running after timeout of 10000ms`,
`HttpMessageNotReadableException: JSON parse error: java.io.EOFException` on
R0's request id `d28cbe37-…`, and `Graceful shutdown aborted with one or more
requests still active` at 15:02:39.779Z. The rest of R0's body never reached Tomcat.

### 3.4 Shutdown drain on the jar (AC-25)

`scripts/smoke.sh --drain build/libs/urlshort.jar 18092 <log>`: two closed load
loops (one create per connection, 50 ms sleep), R0 held, `SIGTERM`, a probe
connection 0.5 s later, then every load request classified from the server's
request ids against the complete responses.

| Run | R0 | Probe | Load requests | OK | Refused before acceptance | Boundary losses | Failures |
|---|---|---|---|---|---|---|---|
| [1](release/smoke-drain-f090103.txt) ([log](release/jar-drain-log-f090103.jsonl)) | 201, complete 514 ms after the stop | refused | 82 | 64 | 18 | **0** | 0 |
| [2](release/smoke-drain-f090103-run2.txt) | 201 at 513 ms | refused | 78 | 58 | 20 | **0** | 0 |
| [3](release/smoke-drain-f090103-run3.txt) | 201 at 514 ms | refused | 80 | 63 | 17 | **0** | 0 |

**AC-25 passes 3 of 3.** DR-02 boundary-loss count: **0** in each run, at the
script's load rate: two closed loops, about 25 requests/s combined (78 to 82
requests in a window of roughly 3 s; estimated from the script's sleeps, not
timed per request). The 10 s phase is the shipped `spring.lifecycle.timeout-per-shutdown-phase=10s`;
the log shows `Graceful shutdown complete`. This rate is far below NFR-L1's
100/s; boundary losses at higher load were measured by the design probe (four per
stop under high load, `docs/review/03-operate/proof/design-probe-rerun.txt`) and
are not re-measured here.

### 3.5 Latency bench (AC-27; NFR-L1, L2, L3)

QA's closed loops could offer only about 82/16 req/s (QA-OPR-01). `scripts/smoke.sh
--bench` now calls `tools/bench.mjs`: each scenario sends request *i* at
*t0* + *i*/rate regardless of earlier responses, over keep-alive connections, and
measures latency from the request's due time to the end of its response, so a
slow response cannot delay or hide later ones. A first try with curl's `--rate`
was dropped because curl 8.7.1 does not pace `--parallel` transfers (bursts of 50).

The jar ran on `127.0.0.1:18094` with the shipped configuration and H2 file mode,
budgets raised by command-line settings
(`--urlshort.rate-limit.create-per-minute=1000000`, `…redirect-per-minute=1000000`)
so the single load client is not refused, on the same laptop as the load client.
Result ([output](release/bench-jar-f090103.txt), [log](release/jar-bench-log-f090103.jsonl),
[scrape](release/bench-prometheus-f090103.txt)):

| Scenario (60 s each) | Offered | Achieved | Requests | Non-2xx/3xx | p50 | p95 | p99 | Target |
|---|---|---|---|---|---|---|---|---|
| redirect `GET /{code}` with creates running | 100/s | 100.0/s | 6 000 | 0 | 1.1 ms | **2.2 ms** | **3.3 ms** | NFR-L1 p95 ≤ 20, p99 ≤ 50: **met** |
| create `POST /api/links` with redirects running | 20/s | 20.0/s | 1 200 | 0 | 1.5 ms | **2.8 ms** | 4.9 ms | NFR-L2 p95 ≤ 50: **met** |
| redirect `GET` alone (records a click) | 100/s | 100.0/s | 6 000 | 0 | 0.9 ms | 1.7 ms | 2.8 ms | |
| redirect `HEAD` alone (same path, returns before recording) | 100/s | 100.0/s | 6 000 | 0 | 1.0 ms | 1.8 ms | 3.1 ms | |

**NFR-L3:** GET p95 minus HEAD p95 = −0.1 ms, which is no added p95 above
run-to-run noise; the ≤ 2 ms bound holds on this run. The bench link's
statistics count 12 000 clicks ([stats](release/bench-stats-7YBO6Hrx-f090103.json)),
exactly the GETs sent, so recording was not skipped. The log has 3 WARN events, all
startup notices (Flyway/H2 version, two springdoc defaults), and no ERROR.

A first bench run is kept but **discarded**
([file](release/bench-jar-f090103-discarded-run1.txt)): the datasource URL was
not quoted, the shell split it at `;`, and the jar ran without `MODE=PostgreSQL`
and without its log file. Its numbers were close (redirect p95 2.2 ms,
create p95 2.7 ms) but they do not count.

### 3.6 AC-28 isolation

| Path for R0 during `docker compose restart` | Result | Evidence |
|---|---|---|
| macOS host → `127.0.0.1:8080` (Lima forwarder → VM → Docker port publish → container) | cut, 4 of 4 | §3.3 |
| inside the VM → VM's `127.0.0.1:8080` (Docker port publish → container), `docker run --network host` | `201` 3.6 s and 4.0 s into the drain, then `Graceful shutdown complete`, 2 of 2 | [run 1](release/r0-vm-published-port-f090103-run1.txt), [run 2](release/r0-vm-published-port-f090103-run2.txt), [log](release/container-vm-path-log-f090103.jsonl) |
| inside the container's network namespace, `docker run --network container:…` | `201` 3.7 s into the drain, then `Graceful shutdown complete`, 2 of 2 (first run's log was replaced by the next `down`/`up`) | [output](release/r0-in-namespace-control-f090103.txt), [log](release/container-control-log-f090103.jsonl) |

So the cut happens in the macOS-to-VM host forwarder. Not tested on a native Linux
Docker host. The lead's working hypothesis, that the forwarder drops established
connections when the guest's listener closes at the start of the drain, is
consistent with these results but was not observed directly.

### 3.7 Not exercised

Load beyond the stated rates and any capacity limit; the bench against the
container; boundary losses at 100/s during a stop; a natural UTC midnight (salt
rotation); concurrent instances (single node by design); restore from a backup (none
exists); `HEAD`/`OPTIONS` beyond the bench; a real reverse proxy in front of the
service (the trusted-proxy rule was exercised with `127.0.0.1` as the proxy).

Everything started here was stopped: the jars on 18091 to 18094 by `SIGTERM`
(graceful shutdown logged), the compose stack by `docker compose down -v`
([output](release/compose-down-f090103.txt)), the probe containers by `--rm`.
`lsof` shows nothing listening on 8080 or 18091 to 18094.

## 4. Dependency advisories (network check)

`scripts/gw --log missions/01-greenfield-core/release/runtime-dependencies-f090103.txt dependencies --configuration runtimeClasspath`
([tree](release/runtime-dependencies-f090103.txt)), then
`node tools/dep-advisories.mjs … release/osv-advisories-f090103.json`:
**97 runtime dependencies queried against OSV at 2026-10-03T14:42:11Z; 0 advisories**
([response](release/osv-advisories-f090103.json)).

The ten advisories open at 00-hello (Tomcat 11.0.24, Jackson 3.1.5 / 2.21.5;
`qitem-20261003021640-bc2477ef`) are closed by the overrides that landed as the
first commit of `01-create-redirect` (`20aef57`): the tree resolves
`tomcat-embed-core 11.0.25`, `tools.jackson.core:jackson-databind 3.1.7`,
`com.fasterxml.jackson.core:jackson-databind 2.21.7`. No new dependency since
00-hello other than `micrometer-registry-prometheus` (managed by Boot, `03-operate`).

**Secret scan (NFR-S4; `01-create-redirect` item 14).** No `gitleaks` on the host,
so two `git grep` passes over every tracked file at `HEAD` (`f090103`):

1. High-signal patterns (private-key headers, AWS `AKIA…`, GitHub `gh[pousr]_…`,
   Slack `xox…`, `sk-…`/`sk-ant-…`, Google `AIza…`, JWT-shaped tokens), all files:
   **no match.**
2. Credential-like assignments (`password|passwd|secret|token|api_key|credential …
   [=:] <6+ chars>`) over `src`, build files, `Dockerfile`, `compose.yaml`,
   `scripts`, `tools`, `.claude`, `.codex`, `rig/rig.yaml`, `project.yaml`: 12
   matches, all in the vendored OpenRig plugin scripts and all variable reads
   (`token = firstString(env.OPENRIG_ACTIVITY_HOOK_TOKEN, …)`,
   `transcriptTokens: Math.round(…)`), none a value.

The only credential-shaped setting in the product is `spring.datasource.password=`
(empty, the embedded H2 file). The public base URL is `urlshort.public-base-url`
with default `http://localhost:8080`, overridden by `URLSHORT_PUBLIC_BASE_URL`
(observed in §3.1). `.gitignore` covers `data/`, `*.db`, logs and local harness
settings; no `.env` file exists. **No secret found.**

## 5. Evidence per slice

### 01-create-redirect (w1; high tier, plan-lock by the lead under D11)

| Artifact | Path |
|---|---|
| Requirements (locked `0acbc9d`) | [`slices/01-create-redirect/SPEC.md`](slices/01-create-redirect/SPEC.md) |
| Design (locked `6367fa0`), ADR-0005 to 0010 | [`slices/01-create-redirect/design.md`](slices/01-create-redirect/design.md), [`../../docs/adr/`](../../docs/adr/) |
| Requirements review (FAIL RQ-01, then PASS) | [`../../docs/review/01-create-redirect/requirements-review.md`](../../docs/review/01-create-redirect/requirements-review.md) |
| Design review (FAIL DR-01/DR-02, then PASS) | [`../../docs/review/01-create-redirect/design-review.md`](../../docs/review/01-create-redirect/design-review.md) |
| Code review (PASS on `a922f49`, CR-01 MEDIUM backlog) / security review (PASS) | [`01-code-review.md`](../../docs/review/01-create-redirect/01-code-review.md), [`02-security-review.md`](../../docs/review/01-create-redirect/02-security-review.md) |
| QA coverage (72 unit / 87 functional at the candidate) | [`../../docs/qa/coverage/01-create-redirect/SUMMARY.md`](../../docs/qa/coverage/01-create-redirect/SUMMARY.md) |
| Proof sequencing (items 13–14) | [`../../docs/qa/01-create-redirect/proof-sequencing.md`](../../docs/qa/01-create-redirect/proof-sequencing.md) |
| Builder and QA proof, progress | [`slices/01-create-redirect/PROOF.md`](slices/01-create-redirect/PROOF.md), [`proof/`](slices/01-create-redirect/proof/), [`PROGRESS.md`](slices/01-create-redirect/PROGRESS.md) |
| Merge gate | [`../../docs/evidence/01-greenfield-core/integrate-01-create-redirect-check-16c355f.txt`](../../docs/evidence/01-greenfield-core/integrate-01-create-redirect-check-16c355f.txt) |

### 02-analytics (w2; low tier, delegated plan-lock)

| Artifact | Path |
|---|---|
| Requirements / design, ADR-0011 to 0013 | [`slices/02-analytics/SPEC.md`](slices/02-analytics/SPEC.md), [`slices/02-analytics/design.md`](slices/02-analytics/design.md) |
| Requirements review (PASS) / design review (FAIL DR-01/02/04, then PASS) / DR-01 escalation | [`requirements-review.md`](../../docs/review/02-analytics/requirements-review.md), [`design-review.md`](../../docs/review/02-analytics/design-review.md), [`design-dr01-escalation.md`](../../docs/review/02-analytics/design-dr01-escalation.md) |
| Code review (FAIL CR-01 test timing, then PASS on `5b3490c`) / security review (PASS, salt record) | [`01-code-review.md`](../../docs/review/02-analytics/01-code-review.md), [`02-security-review.md`](../../docs/review/02-analytics/02-security-review.md) |
| QA coverage (121 unit / 126 functional at the candidate) | [`../../docs/qa/coverage/02-analytics/SUMMARY.md`](../../docs/qa/coverage/02-analytics/SUMMARY.md) |
| Proof sequencing (items 12–13) | [`../../docs/qa/02-analytics/proof-sequencing.md`](../../docs/qa/02-analytics/proof-sequencing.md) |
| Builder and QA proof, progress | [`slices/02-analytics/PROOF.md`](slices/02-analytics/PROOF.md), [`proof/`](slices/02-analytics/proof/), [`PROGRESS.md`](slices/02-analytics/PROGRESS.md) |
| Merge gate | [`../../docs/evidence/01-greenfield-core/integrate-02-analytics-check-091ff46.txt`](../../docs/evidence/01-greenfield-core/integrate-02-analytics-check-091ff46.txt) |

### 03-operate (w2; low tier, delegated plan-lock; judges `qa2-agent`, `review2-agent`)

| Artifact | Path |
|---|---|
| Requirements / design, ADR-0014 to 0017 | [`slices/03-operate/SPEC.md`](slices/03-operate/SPEC.md), [`slices/03-operate/design.md`](slices/03-operate/design.md) |
| Requirements review (FAIL RQ-01, then PASS) / design review (FAIL DR-01/DR-02, then PASS) / shutdown decision | [`requirements-review.md`](../../docs/review/03-operate/requirements-review.md), [`design-review.md`](../../docs/review/03-operate/design-review.md), [`shutdown-decision.md`](../../docs/review/03-operate/shutdown-decision.md) |
| Code + security review (FAIL CR-01/CR-02 HIGH, then PASS on `1c8b2cf`) | [`01-code-review.md`](../../docs/review/03-operate/01-code-review.md), [`02-security-review.md`](../../docs/review/03-operate/02-security-review.md) |
| QA coverage (165 unit / 155 functional at the candidate), findings QA-OPR-01..03 | [`../../docs/qa/coverage/03-operate/SUMMARY.md`](../../docs/qa/coverage/03-operate/SUMMARY.md), [`../../docs/qa/03-operate/findings.md`](../../docs/qa/03-operate/findings.md) |
| Proof sequencing (items 11, 13) | [`../../docs/qa/03-operate/proof-sequencing.md`](../../docs/qa/03-operate/proof-sequencing.md) |
| Builder and QA proof, progress | [`slices/03-operate/PROOF.md`](slices/03-operate/PROOF.md), [`proof/`](slices/03-operate/proof/), [`PROGRESS.md`](slices/03-operate/PROGRESS.md) |
| Merge gate | [`../../docs/evidence/01-greenfield-core/integrate-03-operate-check-8e9c065.txt`](../../docs/evidence/01-greenfield-core/integrate-03-operate-check-8e9c065.txt) |

### Mission level

| Artifact | Path |
|---|---|
| Mission intent, plan-lock brief, status | [`SPEC.md`](SPEC.md), [`NOTES.md`](NOTES.md), [`PROGRESS.md`](PROGRESS.md) |
| Decomposition review and rework | [`../../docs/review/01-greenfield-core/decomposition-review.md`](../../docs/review/01-greenfield-core/decomposition-review.md) |
| Wave map, wave integration record | [`../../docs/evidence/01-greenfield-core/wave-map.md`](../../docs/evidence/01-greenfield-core/wave-map.md), [`wave-integration.md`](../../docs/evidence/01-greenfield-core/wave-integration.md) |
| Wave review, both vantages; forward fixes | [`wave-2-review-review-agent.md`](../../docs/review/01-greenfield-core/wave-2-review-review-agent.md), [`wave-2-review-design-agent.md`](../../docs/review/01-greenfield-core/wave-2-review-design-agent.md), [`wave-2-forward-fixes.md`](../../docs/review/01-greenfield-core/wave-2-forward-fixes.md) |
| Committed API document | [`../../docs/api/openapi.json`](../../docs/api/openapi.json) |
| Traceability, gaps (release rows appended) | [`../../docs/qa/TRACEABILITY.md`](../../docs/qa/TRACEABILITY.md), [`../../docs/qa/GAPS.md`](../../docs/qa/GAPS.md) |
| Evidence export at `release_prep` (8 instances, 88 packets), proof readiness | [`../../docs/evidence/01-greenfield-core/INDEX.md`](../../docs/evidence/01-greenfield-core/INDEX.md), [`proof-readiness.json`](../../docs/evidence/01-greenfield-core/proof-readiness.json) |
| Metrics | [`../../docs/metrics/README.md`](../../docs/metrics/README.md), [`../../docs/metrics/metrics.json`](../../docs/metrics/metrics.json) |
| Drills and natural failures (rollback rehearsal, AC-28) | [`../../docs/scenarios/drills.md`](../../docs/scenarios/drills.md) |
| This step's raw evidence | [`release/`](release/) |

## 6. Metrics, read plainly

From `node tools/sdlc-metrics.mjs` over the export taken at 15:23Z (definitions and
limits in `docs/metrics/README.md`). The export includes every instance the daemon
knows (also 00-hello and the two later missions' lifecycles, all labelled with this
mission's directory); the mission 01 rows are:

| Instance | E2E | Step closures | Failed | Retries | MTTR | Human wait | Hops |
|---|---|---|---|---|---|---|---|
| lifecycle `01M3ZXEX…` (active, at this step) | 11.8 h so far | 36 | 0 | 0 | — | 14 min | 5 |
| `01-create-redirect` `01M40149…` | 3.3 h | 13 | 2 | 6 | 17 min | 0 | 12 |
| `02-analytics` `01M40CP0…` | 3.4 h | 16 | 2 | 7 | 36 min | 0 | 13 |
| `03-operate` `01M40CPB…` | 5.9 h | 22 | 3 | 10 | 36 min | 0 | 15 |

- **Loops and their causes.** Seven failed review verdicts across the three
  slices, each repaired in one round: `01` requirements RQ-01 (idempotency binding)
  and design DR-01/02 (key leak in error logs, client input echoed in problems);
  `02` design DR-01/02/04 (unbounded shutdown, stale salt, CHECK failure on a
  retired connection) and code CR-01 (a flaky clock test); `03` requirements RQ-01
  (shutdown not observable), design DR-01/02 (path WARN leak, shutdown predicate),
  code CR-01/CR-02 (limiter race, smoke false pass). The tool counts 23 retries
  because every re-entered step counts, not only the verdicts.
- **MTTR** 17 to 36 minutes per slice, failed verdict to the next passing closure
  of the same step.
- **Human wait.** 14 minutes on the mission plan-lock. After D11 no slice gate
  waited on the human; `01`'s plan-lock was decided by the lead.
- **Rollbacks.** The tool reports 1 each for the lifecycle, `01` and `03`; those
  are evidence notes that mention rollback (Flyway and design rollback notes), not
  events. There was **no revert on `main`** in `7636264..main`
  (`git log --grep=revert -i` is empty) and no engine resume (`resumeCount` 0).
  The genuine rollback count for this mission is 0; the rehearsal in §8 was on a
  throwaway worktree.
- **Parallelism.** `02` and `03` ran as concurrent instances from 08:04Z; `03`
  waited on `02`'s merge by design (ordered custody of the API document) and
  merged at 13:59Z.

## 7. Known gaps, complete

**From `docs/qa/GAPS.md`.** The table holds the gap rows of every slice since
00-hello. Rows for this mission's three slices are copied here as their final
dispositions; the full verbatim text is the file itself, which a reviewer opens
beside this list.

| Slice | Gap (verbatim key) | Final status at this release |
|---|---|---|
| 01-create-redirect @a922f49 | None for this slice (merged coverage or AC verification) | unchanged; item 14 judged against §4 |
| 02-analytics @862c52e / @5b3490c | Per-suite coverage informational; merged 359/359, 118/118 | unchanged |
| 02-analytics @862c52e / @5b3490c | NFR-L3 added redirect p95 pending release | measured §3.5: no added p95 measurable, ≤ 2 ms holds; item 13 to its judge |
| 02-analytics @862c52e | Clock-boundary and lifetime observations use controlled tests | unchanged (no natural UTC midnight crossed here either) |
| 03-operate @a7c533f / @1c8b2cf | None for merged coverage or AC-1–AC-20 | unchanged |
| 03-operate @a7c533f / @1c8b2cf | Controlled time, request peers and JDBC availability | unchanged |
| 03-operate @1c8b2cf | Backward clock step outside contract; fails closed | unchanged, disclosed limit (below) |
| 03-operate @a7c533f / @1c8b2cf | AC-21 to AC-26 PENDING release | observed PASS §3.1–3.4 |
| 03-operate @a7c533f / @1c8b2cf | AC-27 offered rate not established (QA-OPR-01) | 100/20 achieved; L1/L2 met §3.5 |
| 03-operate @a7c533f / @1c8b2cf | AC-28 PENDING release | **NOT MET on this host** §3.3, §3.6 |
| 03-operate @1c8b2cf | Perl/Time::HiRes and locale for R0 modes (QA-OPR-03) | locale dependency removed (`LC_ALL=C`); Perl remains a prerequisite |
| 03-operate @1c8b2cf | Anonymous disk-gauge working-directory path (QA-OPR-02) | open, W2-03 below |
| 03-operate @1c8b2cf | Proof items 11/13 need downstream evidence | 11 accepted at review; 13 to `qa2-agent` |
| release @8e9c065 (appended by this step) | AC-28 NOT MET; AC-21–26 observed; AC-27; NFR-L3; R0 prerequisites | as in §3 |

**Wave-review findings and backlog** (routed to the lead, `qitem-20261003142206-ff6ad817`):

- **W2-01, MEDIUM** (was CR-01 of `01-create-redirect`): the OpenAPI ProblemDetail
  schema omits the runtime top-level `errors[{field, rule, message}]`. Live
  responses carry it (seen in §3 smoke); typed clients cannot discover it.
  Pre-listed in the dogfood request as a found defect for mission 02.
- **W2-02, LOW** (A-9 / ADR-0015): behind a *configured* trusted proxy, the limiter
  sees each forwarded client but click recording hashes the proxy's address, so
  clicks from different clients share one hash. Totals, per-day counts and
  referrers stay correct; nothing shipped reads the hash; stored hashes cannot be
  separated later. The shipped default has no trusted proxy. Fix before any
  configured-proxy deployment or any unique-client consumer.
- **W2-03, LOW** (QA-OPR-02): the anonymous `/actuator/prometheus` scrape carries
  Boot's disk meter with the working-directory path (`disk_free_bytes{path="/app/."}`
  on the container). Loopback operator scope only. Pre-listed in the dogfood request.
- **W2-04, LOW**: smoke host prerequisites. Addressed in this step (header lists them;
  locale pinned); Perl with Time::HiRes and node (for `--bench`) remain.
- **W2-05, LOW**: a reduction/HMAC failure in click recording reports reason
  `rejected`, which design §5 reserves for a full or closed queue.

**Disclosed limits:**

- **AC-28 on this host** (above; `qitem-20261003151205-a94f1d92`).
- **Backward wall-clock step fails closed** (lead's clock policy, `NOTES.md` §2
  12:40Z): after a clock rollback the limiter can refuse a client until the clock
  catches up; it never admits extra.
- **Single node** (NFR-R4, `docs/RISKS.md`): no replica, limiter state in memory
  (a restart refills every bucket), H2 file database on one volume with **no backup
  or restore procedure**.
- **No `docs/RUNBOOK.md`** yet (release agent's reliability duty, mission 02).
- **Flyway/H2 version notice**: Flyway 12.4.0 has verified H2 up to 2.3.232; the
  shipped H2 is 2.4.240. V1 and V2 apply cleanly on both the file and the in-memory
  database (gate, jar, container).
- **Bench limits**: one 60 s run per scenario, load client on the same laptop,
  budgets raised, jar only (the container was not benched).
- **springdoc** serves `/v3/api-docs` and `/swagger-ui.html` (startup WARN); on the
  decided loopback scope this is the intended public API document.
- **Proof readiness not ready** (§1): three release-owned items pending, five
  re-affirmations needed.

**Dogfood.** Requested from `qa-agent` as `qitem-20261003151220-a22accf5`, report
due at `docs/qa/01-greenfield-core/dogfood.md`, with W2-01 and W2-03 pre-listed. It
is mission 02's input for its bug-fix slice. This release does not wait on it; a
severe finding goes to the lead.

## 8. Rollback

**Rehearsed once** (drill row in [`../../docs/scenarios/drills.md`](../../docs/scenarios/drills.md)):
`git worktree add --detach .worktrees/rollback-rehearsal-01 f090103`, then
`git -C .worktrees/rollback-rehearsal-01 revert -m 1 --no-edit 8e9c065` applied
cleanly as `e6f062a` (26 files, +43/−2043), and the gate on the reverted tree passed
(14 of 14 tasks, lines 358/358, branches 118/118,
[log](release/rollback-rehearsal-check-e6f062a.txt)). Worktree removed; `main` untouched.

Rollback is the integrator's act on `main`, newest slice first:

1. **`03-operate`**: `git revert -m 1 8e9c065589e53385f60d6be3ddbc3683260285df`, then
   `scripts/gw check`. Removes the limiter, operator settings, container hardening
   and the `429` contract. No migration, **no data lost**. `scripts/smoke.sh` changed again in
   this step, so the revert on today's `main` may conflict on that file (the
   rehearsal ran on `f090103`, before this change); keep the current file, whose
   operate checks then fail as expected.
2. **`02-analytics`**: `git revert -m 1 091ff46`. Removes click recording, statistics
   and `V2__create_click.sql`. **Not rehearsed.** Flyway Community has no undo: an
   existing database still lists V2 as applied, and Flyway's validation fails at
   startup when a migration that was applied is missing. So after the revert restore the file
   (`git checkout 091ff46 -- src/main/resources/db/migration/V2__create_click.sql`)
   and commit it; the `click` table then stays, unused. Collected clicks are kept
   but no longer readable through the API.
3. **`01-create-redirect`**: `git revert -m 1 16c355f`. Removes the whole link API
   and V1; same Flyway rule for `V1__create_link_and_audit_log.sql`. Links stop
   redirecting: **every short link becomes unusable** to Visitors, though the rows
   remain in the H2 file.

**Database.** Keep the data file (`data/urlshort.mv.db`, or the `urlshort-data`
volume): do not run `docker compose down -v`, which deletes the volume and every
link, click and audit row with it. Copy the file or volume before any rollback;
there is no other backup.

**Artifacts.** The jar is a build output; the image is local only (`urlshort:local`),
nothing was pushed. Rebuild from the reverted commit with `scripts/gw bootJar` or
`docker compose up -d --build` (the volume survives).

**Verification after a rollback.** `scripts/gw check` green on the reverted `main`;
then `scripts/smoke.sh` against the rebuilt artifact. After step 1 the smoke's
metric-name check for `urlshort.ratelimit.rejections` and the `--inspect`/`--restart`
modes fail as expected while the journey passes; after step 2 the stats step fails;
after step 3 only health, ping and OpenAPI remain.

**Factory.** If the human holds the ship gate, the packet stays parked; this step
waits on what the human asks for. No workflow rollback is needed.

## Self-check

- §1 Brief: what ships is pinned to an empty product diff; the one judgment item
  (AC-28) is stated with its isolation, the lead's decision, a default and the
  alternative's cost; proof readiness is reported as not ready, with the reason;
  human decisions quoted from `NOTES.md` §1; nothing published, pushed or tagged.
- §2 Artifact and gate: fresh `--rerun-tasks` gate on `f090103`; counts read from
  the JUnit XML and the merged JaCoCo XML of that run; jar blob and image id recorded.
- §3 Smoke: jar and container both run on loopback with logs captured; every AC-21
  to AC-28 clause has an observed result; AC-28 is reported failed, not narrowed;
  the discarded bench run is kept and labelled; what was not exercised is listed;
  everything started was stopped (checked with `lsof`).
- §4 Advisories and secrets: fresh OSV query with the raw response kept; secret
  scan commands and every match stated. Not done: a dedicated scanner (none installed).
- §5 Evidence: every path was listed with `ls` before writing; SHAs are the ones in
  the integration record and the ledger.
- §6 Metrics: generated by the committed tool from the fresh export; the tool's
  rollback count is explained rather than repeated, and nothing in the tool was changed.
- §7 Gaps: every mission-01 row of `docs/qa/GAPS.md` has a disposition here (the
  file holds the verbatim text); wave findings W2-01 to W2-05, limits and backlog
  carry an owner or a routed item.
- §8 Rollback: the first revert rehearsed with a green gate; the two migration-carrying
  reverts described step by step but **not rehearsed**; data loss stated.
