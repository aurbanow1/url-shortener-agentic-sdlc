#!/usr/bin/env bash
# Self-check for scripts/http: each case runs the guard against a stand-in curl that only echoes,
# so nothing leaves the machine. Exit 0 when every case is accepted or refused as expected.
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
mock=$(mktemp -d); trap 'rm -rf "$mock"' EXIT
printf '#!/bin/sh\nexit 0\n' > "$mock/curl"; chmod +x "$mock/curl"
fail=0
expect() { # expect accept|refuse <args...>
  local want=$1; shift
  if PATH="$mock:$PATH" "$here/http" "$@" >/dev/null 2>&1; then got=accept; else got=refuse; fi
  if [ "$got" != "$want" ]; then echo "FAIL want $want, got $got: $*"; fail=1; fi
}
# accepted: the shapes the smoke script and QA tooling use
expect accept -sS http://localhost:8080/actuator/health
expect accept -sS --max-time 8 -i -X POST -H 'Content-Type: application/json' -d '{"url":"https://example.com"}' http://127.0.0.1:8080/api/links
expect accept -fsS -o /dev/null -w '%{http_code}' 'http://[::1]:8080/abc?x=1'
expect accept -sSi -H 'X-Forwarded-For: 203.0.113.9' https://localhost/
expect accept -sS --path-as-is http://localhost:8080/../x
expect accept -sS -H 'Referer: https://example.com/page' http://localhost:8080/abcdefgh
# refused: destinations that are not loopback, however spelled
expect refuse https://example.invalid/
expect refuse http://127.0.0.1.example.invalid/
expect refuse http://localhost@example.invalid/
expect refuse http://localhost.example.invalid:8080/
expect refuse example.invalid
expect refuse --url=https://example.invalid/
expect refuse --url https://example.invalid/
expect refuse -sS http://localhost:8080/ https://example.invalid/
# after --, curl reads every argument as a URL, including ones that look like options
expect accept -sS -- http://localhost:8080/actuator/health
expect refuse -- -H https://example.invalid/
expect refuse -sS -- http://localhost:8080/ -H
# refused: options that change or add the destination, or read arguments from elsewhere
expect refuse -x http://proxy.invalid:3128 http://localhost:8080/
expect refuse --proxy http://proxy.invalid:3128 http://localhost:8080/
expect refuse --connect-to localhost:8080:example.invalid:80 http://localhost:8080/
expect refuse --resolve localhost:8080:203.0.113.9 http://localhost:8080/
expect refuse -K /tmp/args http://localhost:8080/
expect refuse -sSL http://localhost:8080/abcdefgh
expect refuse --location http://localhost:8080/abcdefgh
expect refuse --unix-socket /tmp/s http://localhost/
[ $fail -eq 0 ] && echo "scripts/http guard: all cases as expected"
exit $fail
