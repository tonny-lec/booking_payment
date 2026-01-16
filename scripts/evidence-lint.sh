#!/usr/bin/env bash
set -euo pipefail

# Evidence-First lint: ensure usecase docs include an Evidence heading.

fail=0
for f in docs/design/usecases/*.md; do
  if ! grep -q '^# .*Evidence' "$f"; then
    echo "[evidence-lint] MISSING Evidence heading: $f"
    fail=1
  fi
done

if [ "$fail" -ne 0 ]; then
  echo "[evidence-lint] FAILED"
  exit 1
fi

echo "[evidence-lint] OK"
