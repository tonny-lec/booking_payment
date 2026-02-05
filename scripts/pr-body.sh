#!/usr/bin/env bash
set -euo pipefail

src=".agents/skills/pr-template-writer/pr-body/pr-body.md"
dst="/tmp/pr-body.md"

if [[ ! -f "${src}" ]]; then
  echo "Source file not found: ${src}" >&2
  exit 1
fi

cp "${src}" "${dst}"
