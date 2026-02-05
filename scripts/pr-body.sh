#!/usr/bin/env bash
set -euo pipefail

src="${1:-}"
if [[ -z "${src}" ]]; then
  echo "Usage: bash scripts/pr-body.sh <src>" >&2
  exit 1
fi

if [[ ! -f "${src}" ]]; then
  echo "Source file not found: ${src}" >&2
  exit 1
fi

cp "${src}" /tmp/pr-body.md
