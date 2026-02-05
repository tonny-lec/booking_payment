#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 0 ]]; then
  echo "Usage: bash scripts/pr-body.sh" >&2
  exit 1
fi

src=".agents/skills/pr-template-writer/pr-body/pr-body.md"

if [[ ! -f "${src}" ]]; then
  echo "Source file not found: ${src}" >&2
  exit 1
fi

cp "${src}" /tmp/pr-body.md
