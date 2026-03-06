#!/usr/bin/env bash
set -euo pipefail

if [ -d "docs/codex" ]; then
  echo "[test-all] Running Codex intelligence validation..."
  CHECK_LINKS=0 bash scripts/codex/validate.sh
fi

echo "[test-all] Running Gradle test suite..."
./gradlew test
