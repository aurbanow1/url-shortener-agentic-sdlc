#!/usr/bin/env bash
# Design probe for 05-ci-cd: the ci.yml gate step and the cd.yml package step, run on Linux
# (Ubuntu userland + Temurin 21) as a non-root user, against a clean checkout of main.
# Usage inside the container: /probe/ci-probe.sh   (the checkout is mounted at /src)
set -euo pipefail
cd /src
echo "== environment"
head -1 /etc/os-release; uname -m; id
java -version 2>&1 | head -1
for t in curl perl mkfifo seq mktemp sed grep awk; do printf '%s: ' "$t"; command -v "$t" || echo MISSING; done
echo "== gate: ./gradlew check --no-daemon"
start=$(date +%s)
rc=0
./gradlew check --no-daemon || rc=$?
echo "GATE exit $rc after $(( $(date +%s) - start )) s"
echo "== package: ./gradlew bootJar --no-daemon"
start=$(date +%s)
./gradlew bootJar --no-daemon
echo "BOOTJAR done after $(( $(date +%s) - start )) s"
mkdir -p build/smoke
echo "== smoke: scripts/smoke.sh --jar build/libs/urlshort.jar 18091 build/smoke/jar.log"
start=$(date +%s)
src=0
scripts/smoke.sh --jar build/libs/urlshort.jar 18091 build/smoke/jar.log 2>&1 | tee build/smoke/smoke.log || src=$?
echo "SMOKE exit $src after $(( $(date +%s) - start )) s"
echo "== listening sockets seen by the jar log"
grep -o 'Tomcat started on port[^"]*' build/smoke/jar.log || true
exit $(( rc + src ))
