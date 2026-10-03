#!/usr/bin/env bash
# Installed smoke: exercise the public surface of a RUNNING instance as a user
# would. Extend this as slices ship; every section must assert an effect.
#   scripts/smoke.sh [base-url]   (default http://localhost:8080)
# All HTTP goes through scripts/http, so the target can only ever be loopback.
set -euo pipefail
BASE="${1:-http://localhost:8080}"
HTTP="$(dirname "$0")/http"
fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }

echo "# health"
"$HTTP" -fsS "$BASE/actuator/health" | grep -q '"status":"UP"' || fail "health not UP"

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

echo "# openapi"
"$HTTP" -fsS "$BASE/v3/api-docs" | grep -q '"openapi"' || fail "openapi document missing"

echo "SMOKE OK against $BASE"
