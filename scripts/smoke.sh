#!/usr/bin/env bash
# Installed smoke: exercise the public surface of a RUNNING instance as a user
# would. Extend this as slices ship; every section must assert an effect.
#   scripts/smoke.sh [base-url]   (default http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}"
fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }

echo "# health"
curl -fsS "$BASE/actuator/health" | grep -q '"status":"UP"' || fail "health not UP"

echo "# openapi"
curl -fsS "$BASE/v3/api-docs" | grep -q '"openapi"' || fail "openapi document missing"

echo "SMOKE OK against $BASE"
