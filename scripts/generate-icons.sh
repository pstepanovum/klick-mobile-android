#!/usr/bin/env bash
# Convert brand SVG icons (Bold/Line) to Android Vector Drawable XMLs.
# No external tools required — uses Python 3 (stdlib only) for path extraction.
# Usage: bash scripts/generate-icons.sh  (run from android project root)
set -euo pipefail
cd "$(dirname "$0")/.."

SRC="/Users/pavelstepanov/Projects/Klic/klick-mobile-ios/design/icons"
OUT="app/src/main/res/drawable"
mkdir -p "$OUT"

slug() {
  echo "$1" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/_/g; s/^_+|_+$//g'
}

extract_viewport() {
  local svg="$1"
  local vb
  vb=$(grep -o 'viewBox="[^"]*"' "$svg" | head -1 | sed 's/viewBox="//;s/"//')
  if [ -z "$vb" ]; then echo "24 24"; return; fi
  local w h
  w=$(echo "$vb" | awk '{print $3}')
  h=$(echo "$vb" | awk '{print $4}')
  echo "${w:-24} ${h:-24}"
}

process_svg() {
  local svg="$1"
  local name="$2"
  local out_file="$OUT/${name}.xml"

  local vp w v_w v_h
  vp=$(extract_viewport "$svg")
  v_w=$(echo "$vp" | awk '{print $1}')
  v_h=$(echo "$vp" | awk '{print $2}')

  local paths
  paths=$(python3 - "$svg" <<'PYEOF'
import sys, re
with open(sys.argv[1], encoding='utf-8', errors='ignore') as f:
    content = f.read()
paths = re.findall(r'd="([^"]+)"', content)
for p in paths:
    print(p)
PYEOF
)

  if [ -z "$paths" ]; then
    return
  fi

  {
    echo '<?xml version="1.0" encoding="utf-8"?>'
    echo '<vector xmlns:android="http://schemas.android.com/apk/res/android"'
    echo "    android:width=\"24dp\""
    echo "    android:height=\"24dp\""
    echo "    android:viewportWidth=\"${v_w}\""
    echo "    android:viewportHeight=\"${v_h}\">"
    while IFS= read -r pd; do
      [ -z "$pd" ] && continue
      echo "  <path"
      echo "      android:fillColor=\"@android:color/black\""
      echo "      android:pathData=\"${pd}\"/>"
    done <<< "$paths"
    echo '</vector>'
  } > "$out_file"
}

count=0
for variant in Bold Line; do
  variant_slug=$(echo "$variant" | tr '[:upper:]' '[:lower:]')
  for svg in "$SRC/$variant"/*.svg; do
    [ -f "$svg" ] || continue
    basename_no_ext=$(basename "$svg" .svg)
    name="ic_${variant_slug}_$(slug "$basename_no_ext")"
    process_svg "$svg" "$name"
    count=$((count + 1))
  done
done

echo "Generated $count vector drawables in $OUT"
