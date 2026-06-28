#!/usr/bin/env bash
# Convert the brand SVG icon set (design/icons/{Bold,Line}) into Android vector drawables
# at app/src/main/res/drawable/ic_<variant>_<name>.xml. Tint at use-site with `tint = ...`.
#
# Requires svg2vectordrawable (no install needed if you have npx):  npx s2v ...
set -euo pipefail
cd "$(dirname "$0")/.."

SRC="design/icons"
OUT="app/src/main/res/drawable"
mkdir -p "$OUT"

slug() { echo "$1" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/_/g; s/^_+|_+$//g'; }

for variant in Bold Line; do
  for svg in "$SRC/$variant"/*.svg; do
    name="ic_$(slug "$variant")_$(slug "$(basename "$svg" .svg)")"
    npx --yes svg2vectordrawable -i "$svg" -o "$OUT/$name.xml"
  done
done
echo "Generated vector drawables into $OUT (reference as R.drawable.ic_<variant>_<name>)"
