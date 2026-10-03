#!/usr/bin/env bash
# Package the submission: a zip of a fresh clone of this repository (history
# included, nothing uncommitted, nothing ignored — so no PDF, no build output,
# no Gradle home, no worktrees, no local data) plus the exported rig bundle.
# Usage: tools/package-deliverable.sh [out-dir]   → <out-dir>/url-shortener-agentic-sdlc.zip
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/dist}"; mkdir -p "$OUT"
STAGE="$(mktemp -d)"; trap 'rm -rf "$STAGE"' EXIT
NAME=url-shortener-agentic-sdlc

if [ -n "$(git -C "$ROOT" status --porcelain --untracked-files=no)" ]; then
  echo "refusing: uncommitted tracked changes in $ROOT (commit or stash first)" >&2; exit 1
fi

git clone --quiet --no-hardlinks "$ROOT" "$STAGE/$NAME"
git -C "$STAGE/$NAME" remote remove origin
HEAD_SHA="$(git -C "$ROOT" rev-parse --short HEAD)"

# the factory as a shareable artifact (best effort: the zip is complete without it)
mkdir -p "$STAGE/$NAME/dist"
if rig bundle create "$ROOT/rig/rig.yaml" --out "$STAGE/$NAME/dist/urlshort-factory.rigbundle" >/dev/null 2>&1 \
   || rig bundle create "$ROOT/rig/rig.yaml" --output "$STAGE/$NAME/dist/urlshort-factory.rigbundle" >/dev/null 2>&1; then
  echo "rig bundle: $STAGE/$NAME/dist/urlshort-factory.rigbundle"
else
  echo "rig bundle: not created (rig bundle create failed or unavailable); continuing without it" >&2
  rmdir "$STAGE/$NAME/dist" 2>/dev/null || true
fi

# belt and braces: nothing classified ships
if find "$STAGE/$NAME" -iname '*.pdf' | grep -q .; then echo "refusing: a PDF is inside the stage" >&2; exit 1; fi

( cd "$STAGE" && rm -f "$OUT/$NAME.zip" && zip -qr "$OUT/$NAME.zip" "$NAME" -x "*/.DS_Store" )
echo "wrote $OUT/$NAME.zip (HEAD $HEAD_SHA, $(du -h "$OUT/$NAME.zip" | cut -f1))"
unzip -l "$OUT/$NAME.zip" | tail -1
