#!/usr/bin/env bash
# Installed smoke: exercise the public surface of a RUNNING instance as a user
# would. Extend this as slices ship; every section must assert an effect.
#   scripts/smoke.sh [base-url]              installed smoke (default http://localhost:8080)       AC-21, AC-26
#   scripts/smoke.sh --restart [base-url]    compose restart and down/up under load; links survive AC-24, AC-28
#   scripts/smoke.sh --drain <jar> [port]    graceful shutdown of the plain jar on loopback        AC-25
#   scripts/smoke.sh --bench [base-url]      latency of redirects and creates                      AC-27
# All HTTP goes through scripts/http, so the target can only ever be loopback.
# Written for bash 3.2 (the macOS default): no associative arrays, no fractional read timeouts.
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

# ---------------------------------------------------------------- default
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
# A real create whose body arrives slowly: Tomcat dispatches it once the headers are in, so it is
# in flight while the server stops. Opened on fd 3; finished by r0_finish.
R0_BODY='{"url":"https://example.com/held-r0"}'
r0_open() { # port
	exec 3<>"/dev/tcp/127.0.0.1/$1"
	printf 'POST /api/links HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: %d\r\nConnection: close\r\n\r\n%s' \
		"${#R0_BODY}" "${R0_BODY:0:12}" >&3
}
r0_finish() { # sends the rest, keeps the whole response in $WORK/r0.txt, prints the status line
	local line="" rest
	: > "$WORK/r0.txt"
	printf '%s' "${R0_BODY:12}" >&3 2>/dev/null || true
	if IFS= read -r -t 10 line <&3; then
		printf '%s\n' "$line" >> "$WORK/r0.txt"
		while IFS= read -r -t 2 rest <&3; do printf '%s\n' "$rest" >> "$WORK/r0.txt"; done || true
	fi
	exec 3<&- 3>&- || true
	printf '%s' "$line" | tr -d '\r'
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
	while [ -f "$4" ]; do
		"$HTTP" -s -o /dev/null -w '%{http_code}\n' -H 'Connection: close' "$1/$2" >> "$3" 2>/dev/null || echo 000 >> "$3"
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
	local port statuses="$WORK/statuses-$label.txt" stop="$WORK/stop-$label" r0 bad refused
	port="${base##*:}"; port="${port%%/*}"
	: > "$statuses"; touch "$stop"
	load_loop "$base" "$code" "$statuses" "$stop" &
	local loader=$!
	sleep 1
	r0_open "$port"
	"$@" > "$WORK/$label.out" 2>&1 &
	local restarter=$!
	sleep 0.5
	r0="$(r0_finish)"
	wait "$restarter" || fail "$label: '$*' failed: $(cat "$WORK/$label.out")"
	wait_healthy
	rm -f "$stop"; wait "$loader" || true
	echo "$r0" | grep -q '^HTTP/1\.1 2' || fail "$label: held request R0 answered '$r0', expected 2xx"
	bad="$(grep -v -E '^(000|[23][0-9][0-9])$' "$statuses" | wc -l | tr -d ' ')"
	refused="$(grep -c '^000$' "$statuses" || true)"
	echo "$label: R0 '$r0'; $(wc -l < "$statuses" | tr -d ' ') load requests; non-2xx/3xx responses: $bad; connection failures through the port proxy (reported, not judged): $refused"
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
	echo "# docker compose down (no -v) and up (AC-24)"
	docker compose down > "$WORK/down.out" 2>&1 || fail "docker compose down failed: $(cat "$WORK/down.out")"
	docker compose up -d > "$WORK/up.out" 2>&1 || fail "docker compose up failed: $(cat "$WORK/up.out")"
	wait_healthy
	check_link "$BASE" "$code" "$location" "$body"
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
	local JAR="$1" PORT="${2:-18090}" BASE pid log r0 probe started ok=0 refused=0 losses=0 failures=0 requests=0
	[ -f "$JAR" ] || fail "--drain needs the jar path (scripts/gw bootJar)"
	BASE="http://127.0.0.1:$PORT"
	log="$WORK/jar.log"
	# the same JDK 21 that scripts/gw pins; a plain `java` on PATH may be older
	local java="java" home="${URLSHORT_JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
	[ -x "$home/bin/java" ] && java="$home/bin/java"
	URLSHORT_RATELIMIT_CREATEPERMINUTE=1000000 URLSHORT_RATELIMIT_REDIRECTPERMINUTE=1000000 \
		SPRING_DATASOURCE_URL="jdbc:h2:file:$WORK/data/urlshort;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE" \
		"$java" -jar "$JAR" --server.port="$PORT" --server.address=127.0.0.1 > "$log" 2>&1 &
	pid=$!
	local ready=""
	for i in $(seq 1 60); do
		# every attempt's headers are kept: a 503 before readiness was dispatched and logged too
		ready="$("$HTTP" -s -D "$WORK/ready-$i.headers" -o /dev/null -w '%{http_code}' "$BASE/actuator/health/readiness" 2>/dev/null || true)"
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
	r0_open "$PORT"
	sleep 0.2
	started=$SECONDS
	kill -TERM "$pid"
	sleep 0.5
	if (exec 4<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then probe="accepted"; else probe="refused"; fi
	r0="$(r0_finish)"
	wait "$pid" || true
	rm -f "$WORK/drain.stop"; wait "$l1" "$l2" || true
	echo "R0: '$r0' within $((SECONDS - started)) s of SIGTERM; probe connection: $probe"
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
	# the readiness probes and R0 are requests the server logged too; their complete responses count
	cat "$WORK"/ready-*.headers "$WORK/r0.txt" 2>/dev/null | grep -i '^x-request-id:' | tr -d '\r' \
		| awk '{print $2}' >> "$WORK/client-ids.txt" || true
	sort -u "$WORK/client-ids.txt" -o "$WORK/client-ids.txt"
	local undelivered
	undelivered="$(comm -23 "$WORK/server-ids.txt" "$WORK/client-ids.txt" | grep -c . || true)"
	comm -23 "$WORK/server-ids.txt" "$WORK/client-ids.txt" | sed 's/^/dispatched, not delivered: /'
	failures=$((failures + undelivered))
	echo "load: $requests requests by 2 clients at about 20 req/s each; ok $ok; refused before acceptance $refused;" \
		"boundary losses (cut off, never dispatched) $losses; failures $failures (of which dispatched but undelivered $undelivered)"
	echo "$r0" | grep -q '^HTTP/1\.1 2' || fail "held request R0 answered '$r0', expected 2xx"
	[ "$probe" = "refused" ] || fail "a new connection during the drain was accepted"
	[ "$failures" = "0" ] || fail "$failures dispatched requests did not complete"
	echo "SMOKE DRAIN OK ($JAR)"
}

# ---------------------------------------------------------------- --bench (AC-27)
bench_loop() { # out-file seconds command...: about 10 requests per second, one curl per request
	local out="$1" seconds="$2" end; shift 2
	end=$((SECONDS + seconds))
	while [ "$SECONDS" -lt "$end" ]; do
		"$@" -s -o /dev/null -w '%{http_code} %{time_total}\n' >> "$out" 2>/dev/null || echo "000 0" >> "$out"
		sleep 0.1
	done
}

report() { # label file seconds
	sort -k2 -n "$2" | awk -v label="$1" -v secs="$3" '
		{ n++; t[n] = $2 * 1000; if ($1 !~ /^[23]/) bad++ }
		END {
			if (n == 0) { print label ": no requests"; exit }
			p50 = t[int(n * 0.50 + 0.999)]; p95 = t[int(n * 0.95 + 0.999)]; p99 = t[int(n * 0.99 + 0.999)]
			printf "%s: %d requests, achieved %.1f req/s, non-2xx/3xx %d, p50 %.1f ms, p95 %.1f ms, p99 %.1f ms\n", label, n, n / secs, bad + 0, p50, p95, p99
		}'
}

bench_mode() {
	local BASE="$1" SECS="${BENCH_SECONDS:-60}" body code i pids=""
	echo "# bench: the instance's budgets must have been raised (URLSHORT_RATELIMIT_CREATEPERMINUTE," \
		"URLSHORT_RATELIMIT_REDIRECTPERMINUTE) so one load client is not refused; refusals show as non-2xx/3xx"
	echo "# limits: one curl process per request, closed client loops on the same machine as the service, each" \
		"loop sleeping 100 ms after a request (so the achieved rate falls short of the target and is printed);" \
		"latency is curl's time_total for the request; a noisy or short run is a gap, not a pass"
	body="$(create_link "$BASE" "https://example.com/bench")"
	code="$(echo "$body" | json_field code)"
	[ -n "$code" ] || fail "create failed: $body"
	for i in $(seq 1 10); do
		bench_loop "$WORK/redirect-$i.txt" "$SECS" "$HTTP" "$BASE/$code" & pids="$pids $!"
	done
	for i in 1 2; do
		bench_loop "$WORK/create-$i.txt" "$SECS" "$HTTP" -X POST -H 'Content-Type: application/json' \
			-d '{"url":"https://example.com/bench-create"}' "$BASE/api/links" & pids="$pids $!"
	done
	# shellcheck disable=SC2086
	wait $pids
	cat "$WORK"/redirect-*.txt > "$WORK/redirect.txt"
	cat "$WORK"/create-*.txt > "$WORK/create.txt"
	echo "# target: redirects 100 req/s for ${SECS} s (NFR-L1: p95 <= 20 ms, p99 <= 50 ms); creates 20 req/s (NFR-L2: p95 <= 50 ms)"
	report "redirect GET /{code}" "$WORK/redirect.txt" "$SECS"
	report "create POST /api/links" "$WORK/create.txt" "$SECS"
}

case "${1:-}" in
	--restart) restart_mode "${2:-http://localhost:8080}" ;;
	--drain) drain_mode "${2:-}" "${3:-}" ;;
	--bench) bench_mode "${2:-http://localhost:8080}" ;;
	*) smoke "${1:-http://localhost:8080}" ;;
esac
