#!/usr/bin/env bash
# Vendor OpenRig's shared agent resource pool (skills + runtime fragments) into
# rig/agents/shared so our custom AgentSpecs import it with a relative
# `local:../shared` ref and the rig stays reproducible from the repo alone.
# Source: @openrig/cli (Apache-2.0). Re-run after an OpenRig upgrade.
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
SRC="$(npm root -g)/@openrig/cli/daemon/specs/agents/shared"
DST="$ROOT/rig/agents/shared"
VER="$(rig --version)"
rm -rf "$DST" && mkdir -p "$DST"
cp -R "$SRC/." "$DST/"
cp "$(npm root -g)/@openrig/cli/LICENSE" "$DST/LICENSE"
cat > "$DST/VENDORED.md" <<MD
# Vendored from @openrig/cli $VER — daemon/specs/agents/shared

Copied verbatim by scripts/vendor-openrig-shared.sh on $(date -u +%Y-%m-%dT%H:%MZ).
License: Apache-2.0 (see LICENSE). Do not edit here; re-run the script after an
OpenRig upgrade. Our own role specs live in the sibling directories.
MD
echo "vendored $(find "$DST" -type f | wc -l | tr -d ' ') files from OpenRig $VER into rig/agents/shared"
