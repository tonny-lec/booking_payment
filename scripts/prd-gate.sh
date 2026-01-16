#!/usr/bin/env bash
set -euo pipefail

# PRD-First gate:
# If changes touch src/ infra/ .github/ -> require docs/prd-platform.md status: approved

if ! command -v git >/dev/null 2>&1; then
  echo "[prd-gate] git not found. Skipping."
  exit 0
fi
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[prd-gate] Not inside a git repo. Skipping."
  exit 0
fi

BASE_REF="${1:-origin/main}"
changed="$(git diff --name-only "${BASE_REF}"...HEAD || true)"

needs_prd=0
if echo "${changed}" | grep -E '^(src/|infra/|\.github/)' >/dev/null 2>&1; then
  needs_prd=1
fi

if [ "${needs_prd}" -eq 0 ]; then
  echo "[prd-gate] No code/infra workflow changes detected."
  exit 0
fi

if grep -E '^status:\s*"?approved"?' docs/prd-platform.md >/dev/null 2>&1; then
  echo "[prd-gate] OK: docs/prd-platform.md is approved."
  exit 0
fi

echo "[prd-gate] FAIL: code/infra changes require docs/prd-platform.md status: approved."
exit 1
