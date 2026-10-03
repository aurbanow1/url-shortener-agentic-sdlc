#!/usr/bin/env bash
# Installed smoke: exercise the public surface of a RUNNING instance as a user
# would. Extend this as slices ship; every section must assert an effect.
#   scripts/smoke.sh [base-url]              installed smoke (default http://localhost:8080)       AC-21
#   scripts/smoke.sh --jar <jar> [port] [log] plain jar configured by environment, then the smoke  AC-26
#   scripts/smoke.sh --inspect               compose container: binding, user, filesystem, timeout AC-22, AC-23, AC-28
#   scripts/smoke.sh --restart [base-url] [log] compose restart and down/up under load; links survive AC-24, AC-28
#   scripts/smoke.sh --drain <jar> [port] [log] graceful shutdown of the plain jar on loopback     AC-25
#   scripts/smoke.sh --bench [base-url]      open-loop latency of redirects and creates            AC-27, NFR-L3
# All HTTP goes through scripts/http, so the target can only ever be loopback.
# Host prerequisites: bash 3.2 or later (no associative arrays, no fractional read timeouts), curl, Perl with
# Time::HiRes (shipped with macOS; run under LC_ALL=C), awk, sed, mkfifo; node 18 or later for --bench
# (tools/bench.mjs); docker compose for --restart and --inspect; a JDK 21 for --jar and --drain
# (URLSHORT_JAVA_HOME, default Homebrew's).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
HTTP="$HERE/http"
fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

json_field() { sed -n "s/.*\"$1\":\"\\([^\"]*\\)\".*/\\1/p"; }

create_link() { # base url -> prints the 201 body
	"$HTTP" -sS -X POST -H 'Content-Type: application/json' -d "{\"url\":\"$2\"}" "$1/api/links"
}

java_bin() { # the same JDK 21 that scripts/gw pins; a plain `java` on PATH may be older
	local home="${URLSHORT_JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
	if [ -x "$home/bin/java" ]; then echo "$home/bin/java"; else echo java; fi
}

# ---------------------------------------------------------------- default
journey() { # base: the public journey, create -> redirect -> read -> stats -> retire, and the error cases
	local BASE="$1" target="https://example.com/smoke-journey" out code
	echo "# create: 201 with code and a shortUrl ending in it"
	out="$("$HTTP" -sS -i -X POST -H 'Content-Type: application/json' -d "{\"url\":\"$target\"}" "$BASE/api/links")"
	echo "$out" | head -1 | grep -q ' 201 ' || fail "create status not 201: $(echo "$out" | head -1)"
	code="$(echo "$out" | tail -1 | json_field code)"
	[ -n "$code" ] || fail "create body has no code"
	echo "$out" | tail -1 | grep -q "\"shortUrl\":\"[^\"]*/$code\"" || fail "create body has no shortUrl ending in /$code"

	echo "# redirect: 302 to the stored url, not cacheable"
	out="$("$HTTP" -sS -i -H 'Referer: https://referrer.example/page?q=1' "$BASE/$code")"
	echo "$out" | head -1 | grep -q ' 302 ' || fail "redirect status not 302"
	echo "$out" | grep -qi "^location: $target" || fail "redirect Location is not $target"
	echo "$out" | grep -qi '^cache-control: no-store' || fail "redirect has no Cache-Control: no-store"

	echo "# read: 200, active"
	"$HTTP" -fsS "$BASE/api/links/$code" | grep -q '"state":"active"' || fail "read does not show the link active"

	echo "# stats: the click is counted with its referrer reduced to an origin"
	sleep 1 # clicks are stored asynchronously
	out="$("$HTTP" -fsS "$BASE/api/links/$code/stats")"
	echo "$out" | grep -q '"totalClicks":1' || fail "stats do not count the one click: $out"
	echo "$out" | grep -q '"referrer":"https://referrer.example"' || fail "stats lack the reduced referrer: $out"

	echo "# invalid create: 400 problem with errors[], the input not echoed"
	out="$("$HTTP" -sS -i -X POST -H 'Content-Type: application/json' -d '{"url":"javascript:alert(1)"}' "$BASE/api/links")"
	echo "$out" | head -1 | grep -q ' 400 ' || fail "invalid create status not 400"
	echo "$out" | grep -qi '^content-type: application/problem+json' || fail "invalid create answer is not a problem detail"
	echo "$out" | tail -1 | grep -q '"errors":\[{"field":"url"' || fail "invalid create has no errors[] entry for url"
	if echo "$out" | tail -1 | grep -q 'alert'; then fail "invalid create echoes the client's input"; fi

	echo "# unknown code: 404"
	"$HTTP" -sS -o /dev/null -w '%{http_code}' "$BASE/zzzzzzzz" | grep -q '^404$' || fail "unknown code is not 404"

	echo "# retire: 204, then the redirect answers 410"
	"$HTTP" -sS -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/links/$code" | grep -q '^204$' || fail "retire is not 204"
	"$HTTP" -sS -o /dev/null -w '%{http_code}' "$BASE/$code" | grep -q '^410$' || fail "retired redirect is not 410"
}

smoke() {
	local BASE="$1"
	echo "# health, liveness and readiness"
	"$HTTP" -fsS "$BASE/actuator/health" | grep -q '"status":"UP"' || fail "health not UP"
	"$HTTP" -fsS "$BASE/actuator/health/liveness" | grep -q '"status":"UP"' || fail "liveness not UP"
	"$HTTP" -fsS "$BASE/actuator/health/readiness" | grep -q '"status":"UP"' || fail "readiness not UP"

	echo "# ping: 200 application/json, {status:ok,time}, server-issued X-Request-Id"
	PING="$("$HTTP" -sS -i "$BASE/api/ping")"
	echo "$PING" | head -1 | grep -q ' 200 ' || fail "ping status not 200"
	echo "$PING" | grep -qi '^content-type: application/json' || fail "ping content-type not application/json"
	echo "$PING" | grep -qi '^x-request-id: .' || fail "ping has no X-Request-Id"
	echo "$PING" | tail -1 | grep -q '"status":"ok"' || fail "ping body has no status ok"
	echo "$PING" | tail -1 | grep -q '"time":"' || fail "ping body has no time"

	echo "# ping wrong method: 405 application/problem+json, still carries X-Request-Id, no trace"
	POST="$("$HTTP" -sS -i -X POST "$BASE/api/ping")"
	echo "$POST" | head -1 | grep -q ' 405 ' || fail "POST ping status not 405"
	echo "$POST" | grep -qi '^content-type: application/problem+json' || fail "405 content-type not application/problem+json"
	echo "$POST" | grep -qi '^x-request-id: .' || fail "405 has no X-Request-Id"
	if echo "$POST" | tail -1 | grep -qi 'exception\|at dev\.urlshort'; then fail "405 body leaks exception details"; fi

	journey "$BASE"

	echo "# metrics: request timer, rejection counter, pool gauges"
	NAMES="$("$HTTP" -fsS "$BASE/actuator/metrics")"
	for name in http.server.requests urlshort.ratelimit.rejections hikaricp.connections.active hikaricp.connections.idle; do
		echo "$NAMES" | grep -q "\"$name\"" || fail "metric $name not listed"
	done

	echo "# prometheus scrape"
	"$HTTP" -fsS "$BASE/actuator/prometheus" > "$WORK/prometheus.txt" || fail "prometheus scrape failed"
	[ -s "$WORK/prometheus.txt" ] || fail "prometheus scrape empty"
	grep -q '^urlshort_ratelimit_rejections_total' "$WORK/prometheus.txt" || fail "rejection counter not scraped"

	echo "# openapi"
	"$HTTP" -fsS "$BASE/v3/api-docs" | grep -q '"openapi"' || fail "openapi document missing"

	echo "SMOKE OK against $BASE"
}

# ---------------------------------------------------------------- held request R0
# A real create whose chunked body is sent in two parts: Tomcat dispatches it once the headers are in,
# so it is in flight while the server stops. curl judges the response: exit 0 only once all of it has
# arrived (a short body is exit 18, a connection cut without a response 52 or 56).
R0_BODY='{"url":"https://example.com/held-r0"}'
# LC_ALL=C: Perl panics under a locale the host does not install (macOS with C.UTF-8, QA-OPR-03)
now_ms() { LC_ALL=C perl -MTime::HiRes=time -e 'printf "%d\n", time * 1000'; }
r0_open() { # base-url: starts R0 and sends the first part of its body
	mkfifo "$WORK/r0.fifo"
	"$HTTP" -sS -X POST -T - -H 'Content-Type: application/json' -H 'Expect:' --max-time 30 \
		-D "$WORK/r0.headers" -o "$WORK/r0.body" -w '%{http_code}' "$1/api/links" \
		< "$WORK/r0.fifo" > "$WORK/r0.status" 2> "$WORK/r0.err" &
	R0_PID=$!
	exec 3> "$WORK/r0.fifo"
	printf '%s' "${R0_BODY:0:12}" >&3
}
r0_finish() { # stop-ms: sends the rest and waits for curl. Sets R0 (a summary), R0_RC (curl's exit)
	# and R0_OK=1 only for a complete 2xx/3xx no later than 10 s after stop-ms (AC-25, rule 13)
	local status elapsed
	trap '' PIPE # a write after the server cut R0 must fail, not end the script
	printf '%s' "${R0_BODY:12}" >&3 2>/dev/null || true
	trap - PIPE
	exec 3>&-
	R0_RC=0
	wait "$R0_PID" || R0_RC=$?
	elapsed=$(( $(now_ms) - $1 ))
	status="$(cat "$WORK/r0.status")"
	R0="curl exit $R0_RC, status $status, complete ${elapsed} ms after the stop"
	R0_OK=0
	if [ "$R0_RC" = 0 ] && [ "$elapsed" -le 10000 ]; then
		case "$status" in 2??|3??) R0_OK=1 ;; esac
	fi
}

# ---------------------------------------------------------------- --restart (compose)
wait_healthy() {
	local id i
	id="$(docker compose ps -q urlshort)"
	for i in $(seq 1 60); do
		[ "$(docker inspect -f '{{.State.Health.Status}}' "$id" 2>/dev/null || true)" = "healthy" ] && return 0
		sleep 2
	done
	fail "container not healthy after 120 s"
}

load_loop() { # base code out-file stop-file: one redirect per connection, about 8 per second (inside the 600/min budget)
	local status
	while [ -f "$4" ]; do
		# exactly one row per attempt: curl's -w already prints 000 when no response arrives
		status="$("$HTTP" -s -o /dev/null -w '%{http_code}' -H 'Connection: close' "$1/$2" 2>/dev/null)" || true
		echo "${status:-000}" >> "$3"
		sleep 0.1
	done
}

check_link() { # base code location body
	local got
	got="$("$HTTP" -sS -o /dev/null -w '%{http_code} %{redirect_url}' "$1/$2")"
	[ "$got" = "302 $3" ] || fail "after restart GET /$2 answered '$got', expected '302 $3'"
	got="$("$HTTP" -sS "$1/api/links/$2")"
	[ "$got" = "$4" ] || fail "after restart GET /api/links/$2 changed: $got"
}

restart_under_load() { # base code location body label command...
	local base="$1" code="$2" location="$3" body="$4" label="$5"; shift 5
	local statuses="$WORK/statuses-$label.txt" stop="$WORK/stop-$label" bad refused started
	: > "$statuses"; touch "$stop"
	load_loop "$base" "$code" "$statuses" "$stop" &
	local loader=$!
	sleep 1
	r0_open "$base"
	sleep 0.2
	started="$(now_ms)"
	"$@" > "$WORK/$label.out" 2>&1 &
	local restarter=$!
	sleep 0.5
	r0_finish "$started"
	wait "$restarter" || fail "$label: '$*' failed: $(cat "$WORK/$label.out")"
	wait_healthy
	rm -f "$stop"; wait "$loader" || true
	# R0 is judged at the end of the run, so the load and persistence checks below still report
	[ "$R0_OK" = 1 ] || { R0_FAILED="$label: held request R0: $R0; expected a complete 2xx/3xx within 10 s"; echo "FAIL $R0_FAILED"; }
	rm -f "$WORK/r0.fifo"
	bad="$(grep -c -v -E '^(000|[23][0-9][0-9])$' "$statuses" || true)" # grep -c exits 1 on a zero count
	refused="$(grep -c '^000$' "$statuses" || true)"
	echo "$label: R0 $R0; $(wc -l < "$statuses" | tr -d ' ') load requests; non-2xx/3xx responses: $bad; connection failures through the port proxy (reported, not judged): $refused"
	[ "$bad" = "0" ] || fail "$label: $bad responses outside 2xx/3xx: $(grep -v -E '^(000|[23][0-9][0-9])$' "$statuses" | sort | uniq -c | tr '\n' ' ')"
	check_link "$base" "$code" "$location" "$body"
}

restart_mode() {
	local BASE="$1" body code location timeout
	command -v docker > /dev/null || fail "--restart needs docker compose"
	wait_healthy
	body="$(create_link "$BASE" "https://example.com/survives-restart")"
	code="$(echo "$body" | json_field code)"
	[ -n "$code" ] || fail "create failed: $body"
	location="https://example.com/survives-restart"
	timeout="$(docker inspect -f '{{.Config.StopTimeout}}' "$(docker compose ps -q urlshort)")"
	[ -n "$timeout" ] && [ "$timeout" -gt 10 ] || fail "container stop timeout '$timeout' is not longer than the 10 s shutdown phase"
	echo "# stop timeout ${timeout} s > 10 s shutdown phase"
	echo "# docker compose restart under load (AC-24, AC-28)"
	restart_under_load "$BASE" "$code" "$location" "$body" restart docker compose restart
	# keep the restart window's log: down/up below replaces the container and its log
	docker compose logs --no-log-prefix urlshort > "${2:-$WORK/restart.log}" 2>&1
	echo "# docker compose down (no -v) and up (AC-24)"
	docker compose down > "$WORK/down.out" 2>&1 || fail "docker compose down failed: $(cat "$WORK/down.out")"
	docker compose up -d > "$WORK/up.out" 2>&1 || fail "docker compose up failed: $(cat "$WORK/up.out")"
	wait_healthy
	check_link "$BASE" "$code" "$location" "$body"
	echo "links survive restart and down/up: GET /$code 302 to $location, GET /api/links/$code unchanged"
	[ -z "${R0_FAILED:-}" ] || fail "$R0_FAILED"
	echo "SMOKE RESTART OK against $BASE"
}

# ---------------------------------------------------------------- --drain (plain jar, AC-25)
drain_loop() { # base stop-file prefix: one create per connection, about 20 per second, every request recorded
	local i=0 rc
	while [ -f "$2" ]; do
		i=$((i + 1))
		# `|| rc=$?` keeps set -e from ending the loop at the first refused connection
		rc=0
		"$HTTP" -s -o /dev/null -D "$3-$i.headers" -w '%{http_code}' -H 'Connection: close' \
			-H 'Content-Type: application/json' -d '{"url":"https://example.com/drain"}' "$1/api/links" \
			> "$3-$i.status" 2>/dev/null || rc=$?
		echo "$rc" > "$3-$i.exit"
		sleep 0.05
	done
}

drain_mode() {
	local JAR="$1" PORT="${2:-18090}" BASE pid log probe started ok=0 refused=0 losses=0 failures=0 requests=0 rc
	[ -f "$JAR" ] || fail "--drain needs the jar path (scripts/gw bootJar)"
	BASE="http://127.0.0.1:$PORT"
	log="${3:-$WORK/jar.log}"
	URLSHORT_RATELIMIT_CREATEPERMINUTE=1000000 URLSHORT_RATELIMIT_REDIRECTPERMINUTE=1000000 \
		SPRING_DATASOURCE_URL="jdbc:h2:file:$WORK/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE" \
		"$(java_bin)" -jar "$JAR" --server.port="$PORT" --server.address=127.0.0.1 > "$log" 2>&1 &
	pid=$!
	local ready=""
	for i in $(seq 1 60); do
		# every complete attempt's headers are kept: a 503 before readiness was dispatched and logged too
		rc=0
		ready="$("$HTTP" -s -D "$WORK/ready-$i.headers" -o /dev/null -w '%{http_code}' "$BASE/actuator/health/readiness" 2>/dev/null)" || rc=$?
		[ "$rc" = 0 ] || rm -f "$WORK/ready-$i.headers"
		[ "$ready" = "200" ] && break
		sleep 1
	done
	[ "$ready" = "200" ] || fail "jar did not become ready: $(tail -5 "$log")"
	touch "$WORK/drain.stop"
	drain_loop "$BASE" "$WORK/drain.stop" "$WORK/a" &
	local l1=$!
	drain_loop "$BASE" "$WORK/drain.stop" "$WORK/b" &
	local l2=$!
	sleep 2
	r0_open "$BASE"
	sleep 0.2
	started="$(now_ms)"
	kill -TERM "$pid"
	sleep 0.5
	if (exec 4<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then probe="accepted"; else probe="refused"; fi
	r0_finish "$started"
	wait "$pid" || true
	rm -f "$WORK/drain.stop"; wait "$l1" "$l2" || true
	echo "R0: $R0 (SIGTERM); probe connection: $probe"
	grep -q 'Graceful shutdown complete' "$log" || fail "no 'Graceful shutdown complete' in the jar log"

	# classify every load request from evidence (AC-25): the server's request ids against complete responses
	grep -o '"requestId":"[^"]*","status":[0-9]*' "$log" | sed 's/"requestId":"\([^"]*\)".*/\1/' | sort -u > "$WORK/server-ids.txt"
	: > "$WORK/client-ids.txt"
	for exitfile in "$WORK"/a-*.exit "$WORK"/b-*.exit; do
		[ -f "$exitfile" ] || continue
		requests=$((requests + 1))
		local stem="${exitfile%.exit}" code status id
		code="$(cat "$exitfile")"; status="$(cat "$stem.status" 2>/dev/null || echo 000)"
		id="$(grep -i '^x-request-id:' "$stem.headers" 2>/dev/null | tr -d '\r' | awk '{print $2}' || true)"
		case "$code:$status" in
			0:2??|0:3??) ok=$((ok + 1)); echo "$id" >> "$WORK/client-ids.txt" ;;
			7:*) refused=$((refused + 1)) ;;
			52:*|56:*) if [ -z "$id" ]; then losses=$((losses + 1)); else failures=$((failures + 1)); fi ;;
			*) failures=$((failures + 1)); echo "failure: curl exit $code, status $status, id '$id'" ;;
		esac
	done
	# the readiness probes and R0 are requests the server logged too; only their complete responses count
	[ "$R0_RC" = 0 ] || rm -f "$WORK/r0.headers"
	cat "$WORK"/ready-*.headers "$WORK/r0.headers" 2>/dev/null | grep -i '^x-request-id:' | tr -d '\r' \
		| awk '{print $2}' >> "$WORK/client-ids.txt" || true
	sort -u "$WORK/client-ids.txt" -o "$WORK/client-ids.txt"
	local undelivered
	undelivered="$(comm -23 "$WORK/server-ids.txt" "$WORK/client-ids.txt" | grep -c . || true)"
	comm -23 "$WORK/server-ids.txt" "$WORK/client-ids.txt" | sed 's/^/dispatched, not delivered: /'
	failures=$((failures + undelivered))
	echo "load: $requests requests by 2 clients at about 20 req/s each; ok $ok; refused before acceptance $refused;" \
		"boundary losses (cut off, never dispatched) $losses; failures $failures (of which dispatched but undelivered $undelivered)"
	[ "$R0_OK" = 1 ] || fail "held request R0: $R0; expected a complete 2xx/3xx within 10 s of SIGTERM"
	[ "$probe" = "refused" ] || fail "a new connection during the drain was accepted"
	[ "$failures" = "0" ] || fail "$failures dispatched requests did not complete"
	echo "SMOKE DRAIN OK ($JAR)"
}

# ---------------------------------------------------------------- --jar (plain jar, AC-26)
jar_mode() { # the plain jar configured by environment only, on loopback; the smoke, then each override observed
	local JAR="$1" PORT="${2:-18091}" LOG="${3:-$WORK/jar.log}" BASE pid i out status="" admitted=0
	[ -f "$JAR" ] || fail "--jar needs the jar path (scripts/gw bootJar)"
	BASE="http://127.0.0.1:$PORT"
	URLSHORT_PUBLIC_BASE_URL=https://sho.rt.example URLSHORT_RATELIMIT_CREATEPERMINUTE=10 \
		URLSHORT_RATELIMIT_REDIRECTPERMINUTE=100 URLSHORT_RATELIMIT_TRUSTEDPROXIES=127.0.0.1 \
		SPRING_DATASOURCE_URL="jdbc:h2:file:$WORK/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE" \
		"$(java_bin)" -jar "$JAR" --server.port="$PORT" --server.address=127.0.0.1 > "$LOG" 2>&1 &
	pid=$!
	for i in $(seq 1 60); do
		"$HTTP" -fs "$BASE/actuator/health/readiness" > /dev/null 2>&1 && break
		sleep 1
	done
	echo "# environment: URLSHORT_PUBLIC_BASE_URL=https://sho.rt.example URLSHORT_RATELIMIT_CREATEPERMINUTE=10" \
		"URLSHORT_RATELIMIT_REDIRECTPERMINUTE=100 URLSHORT_RATELIMIT_TRUSTEDPROXIES=127.0.0.1 SPRING_DATASOURCE_URL=<temp dir>"
	smoke "$BASE"
	echo "# AC-26: the overrides are in effect"
	out="$(create_link "$BASE" "https://example.com/ac26")"
	echo "$out" | grep -q '"shortUrl":"https://sho.rt.example/' || fail "shortUrl ignores URLSHORT_PUBLIC_BASE_URL: $out"
	echo "shortUrl from URLSHORT_PUBLIC_BASE_URL: $(echo "$out" | json_field shortUrl)"
	[ -f "$WORK/data/urlshort.mv.db" ] || fail "no database file where SPRING_DATASOURCE_URL points"
	echo "database file where SPRING_DATASOURCE_URL points: present"
	# a fresh client, named by the trusted proxy's X-Forwarded-For, spends exactly the overridden create budget
	for i in $(seq 1 11); do
		status="$("$HTTP" -s -o /dev/null -w '%{http_code}' -H 'X-Forwarded-For: 192.0.2.10' "$BASE/api/ping")"
		[ "$status" = 200 ] || break
		admitted=$((admitted + 1))
	done
	[ "$status" = 429 ] && [ "$admitted" = 10 ] \
		|| fail "forwarded client admitted $admitted, then $status; expected 10 then 429"
	echo "forwarded client 192.0.2.10 via trusted proxy 127.0.0.1: $admitted admitted, then 429 (create budget 10)"
	kill -TERM "$pid"
	wait "$pid" || true
	grep -q 'Graceful shutdown complete' "$LOG" || fail "no 'Graceful shutdown complete' in the jar log"
	echo "SMOKE JAR OK ($JAR on $BASE)"
}

# ---------------------------------------------------------------- --inspect (compose, AC-22 AC-23 AC-28)
inspect_mode() { # the running compose container: binding, user, filesystem, mounts, stop timeout
	local id facts
	id="$(docker compose ps -q urlshort)"
	[ -n "$id" ] || fail "--inspect needs the compose stack up"
	echo "# docker compose port urlshort 8080"
	docker compose port urlshort 8080 | tee "$WORK/port.txt"
	grep -qx '127.0.0.1:8080' "$WORK/port.txt" || fail "published binding is not 127.0.0.1:8080"
	echo "# docker inspect"
	facts="$(docker inspect -f 'User={{.Config.User}} ReadonlyRootfs={{.HostConfig.ReadonlyRootfs}} StopTimeout={{.Config.StopTimeout}} PortBindings={{json .HostConfig.PortBindings}} Mounts={{json .Mounts}}' "$id")"
	echo "$facts"
	docker inspect -f 'Health={{.State.Health.Status}} Healthcheck={{json .Config.Healthcheck.Test}}' "$id" | tee "$WORK/health.txt"
	grep -q 'Health=healthy' "$WORK/health.txt" || fail "container is not healthy"
	grep -q 'health/readiness' "$WORK/health.txt" || fail "the health check does not ask readiness"
	echo "$facts" | grep -q 'ReadonlyRootfs=true' || fail "root filesystem is writable"
	echo "$facts" | grep -q 'User=root \|User= \|User=0 ' && fail "container runs as root"
	echo "$facts" | grep -o '"HostIp":"[^"]*"' | grep -v '"HostIp":"127.0.0.1"' && fail "a port is bound beyond loopback"
	[ "$(echo "$facts" | sed 's/.*StopTimeout=\([0-9]*\).*/\1/')" -gt 10 ] || fail "stop timeout is not longer than the 10 s phase"
	[ "$(echo "$facts" | grep -o '"Type":"volume"' | wc -l | tr -d ' ')" = 1 ] || fail "expected exactly one volume mount"
	echo "$facts" | grep -q '"Type":"volume","Name":"[^"]*","Source":"[^"]*","Destination":"/app/data"' \
		|| fail "the volume is not mounted at /app/data"
	echo "$facts" | grep -o '"Type":"[a-z]*"' | grep -v -E '"Type":"(volume|tmpfs)"' && fail "a bind or other persistent mount exists"
	echo "# inside the container: identity, a write outside data/ fails, a write inside data/ succeeds"
	docker compose exec -T urlshort id
	docker compose exec -T urlshort id -u | grep -qx 0 && fail "process runs as uid 0"
	if docker compose exec -T urlshort touch /app/smoke-probe 2> "$WORK/ro.err"; then fail "write under /app succeeded"; fi
	echo "touch /app/smoke-probe: $(cat "$WORK/ro.err")"
	docker compose exec -T urlshort touch /app/data/smoke-probe || fail "write under /app/data failed"
	docker compose exec -T urlshort rm /app/data/smoke-probe
	echo "touch /app/data/smoke-probe: ok (removed again)"
	echo "SMOKE INSPECT OK"
}

# ---------------------------------------------------------------- --bench (AC-27)
bench_mode() {
	local BASE="$1" SECS="${BENCH_SECONDS:-60}" body code bench="$HERE/../tools/bench.mjs"
	command -v node > /dev/null || fail "--bench needs node (tools/bench.mjs)"
	echo "# bench: the instance's budgets must have been raised (URLSHORT_RATELIMIT_CREATEPERMINUTE," \
		"URLSHORT_RATELIMIT_REDIRECTPERMINUTE) so one load client is not refused; refusals show as non-2xx/3xx"
	echo "# method: tools/bench.mjs, open loop (request i due at i/rate s whatever earlier ones do), keep-alive" \
		"connections, on the same machine as the service; latency from the due time to the end of the response;" \
		"a noisy or short run is a gap, not a pass"
	body="$(create_link "$BASE" "https://example.com/bench")"
	code="$(echo "$body" | json_field code)"
	[ -n "$code" ] || fail "create failed: $body"
	echo "# bench link: $code (its statistics afterwards count the GET redirects whose clicks were stored)"
	echo "# 1. redirects 100 req/s and creates 20 req/s together for ${SECS} s" \
		"(NFR-L1: redirect p95 <= 20 ms, p99 <= 50 ms; NFR-L2: create p95 <= 50 ms)"
	node "$bench" "$BASE" "$SECS" "[{\"label\":\"redirect GET /{code}\",\"method\":\"GET\",\"path\":\"/$code\",\"rate\":100},
		{\"label\":\"create POST /api/links\",\"method\":\"POST\",\"path\":\"/api/links\",\"rate\":20,
		 \"body\":\"{\\\"url\\\":\\\"https://example.com/bench-create\\\"}\"}]" || fail "bench run failed"
	echo "# 2. NFR-L3 (click recording adds <= 2 ms p95): the same redirect alone at 100 req/s for ${SECS} s, GET" \
		"then HEAD; HEAD runs the same lookup and response and returns before the click is reduced and queued"
	node "$bench" "$BASE" "$SECS" "[{\"label\":\"redirect GET /{code} alone\",\"method\":\"GET\",\"path\":\"/$code\",\"rate\":100}]" \
		|| fail "bench run failed"
	node "$bench" "$BASE" "$SECS" "[{\"label\":\"redirect HEAD /{code} alone\",\"method\":\"HEAD\",\"path\":\"/$code\",\"rate\":100}]" \
		|| fail "bench run failed"
}

case "${1:-}" in
	--restart) restart_mode "${2:-http://localhost:8080}" "${3:-}" ;;
	--drain) drain_mode "${2:-}" "${3:-}" "${4:-}" ;;
	--jar) jar_mode "${2:-}" "${3:-}" "${4:-}" ;;
	--inspect) inspect_mode ;;
	--bench) bench_mode "${2:-http://localhost:8080}" ;;
	*) smoke "${1:-http://localhost:8080}" ;;
esac
