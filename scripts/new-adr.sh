#!/usr/bin/env bash
set -euo pipefail

num="${1:-}"
slug="${2:-}"

if [ -z "$num" ] || [ -z "$slug" ]; then
  echo "Usage: $0 <number> <slug>"
  exit 2
fi

mkdir -p docs/adr
out="docs/adr/${num}-${slug}.md"
if [ -f "$out" ]; then
  echo "[new-adr] Already exists: $out"
  exit 3
fi

cp docs/templates/adr-template.md "$out"
sed -i "s/adr-<number>-<slug>/adr-${num}-${slug}/" "$out" || true
echo "[new-adr] Created: $out"
