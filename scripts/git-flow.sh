#!/usr/bin/env bash
set -euo pipefail

if ! command -v git >/dev/null 2>&1; then
  echo "git is required" >&2
  exit 1
fi

cmd="${1:-}"

print_usage() {
  cat <<'USAGE'
Usage:
  scripts/git-flow.sh start <branch-name>
  scripts/git-flow.sh sync
  scripts/git-flow.sh status

Commands:
  start   Checkout main, pull latest, create new branch
  sync    Checkout main and pull latest
  status  Show concise status and last commit
USAGE
}

ensure_clean_worktree() {
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Working tree is dirty. Commit or stash changes first." >&2
    exit 1
  fi
}

case "$cmd" in
  start)
    branch="${2:-}"
    if [[ -z "$branch" ]]; then
      echo "Branch name is required." >&2
      print_usage
      exit 1
    fi
    ensure_clean_worktree
    git checkout main
    git pull origin main
    git checkout -b "$branch"
    git status -sb
    ;;
  sync)
    ensure_clean_worktree
    git checkout main
    git pull origin main
    git status -sb
    ;;
  status)
    git status -sb
    git log -1 --oneline
    ;;
  -h|--help|help|"")
    print_usage
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    print_usage
    exit 1
    ;;
esac
