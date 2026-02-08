#!/usr/bin/env python3
"""Validate Codex intelligence docs and source manifest."""

from __future__ import annotations

import os
import re
import sys
from datetime import date
from pathlib import Path

import requests
import yaml

DATE_RE = re.compile(r"\d{4}-\d{2}-\d{2}")
URL_RE = re.compile(r"^https?://")
REQUIRED_FILES = [
    "README.md",
    "01-strategy-org.md",
    "02-operations-playbook.md",
    "03-security-governance.md",
    "04-use-cases-lessons.md",
    "05-whats-new-timeline.md",
    "sources.yaml",
]


def fail(message: str) -> None:
    print(f"[validate] FAIL: {message}")
    raise SystemExit(1)


def load_manifest(path: Path) -> dict:
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or "sources" not in data:
        fail("sources.yaml must define top-level 'sources' list")
    return data


def check_file_structure(root: Path) -> None:
    for name in REQUIRED_FILES:
        if not (root / name).exists():
            fail(f"missing required file: docs/codex/{name}")


def check_markers_and_dates(root: Path) -> None:
    auto_files = [
        "01-strategy-org.md",
        "02-operations-playbook.md",
        "03-security-governance.md",
        "04-use-cases-lessons.md",
        "05-whats-new-timeline.md",
    ]
    for name in auto_files:
        content = (root / name).read_text(encoding="utf-8")
        if "<!-- AUTO-GENERATED:START -->" not in content or "<!-- AUTO-GENERATED:END -->" not in content:
            fail(f"auto-generated markers missing in {name}")
        if "_自動更新待ち_" in content:
            fail(f"auto-generated block not populated in {name}")
        m = re.search(r'last_updated:\s*"([^"]+)"', content)
        if not m or not DATE_RE.fullmatch(m.group(1)):
            fail(f"invalid last_updated date in {name}")

    readme = (root / "README.md").read_text(encoding="utf-8")
    m = re.search(r'last_updated:\s*"([^"]+)"', readme)
    if not m or not DATE_RE.fullmatch(m.group(1)):
        fail("invalid last_updated date in README.md")


def check_sources(manifest: dict) -> list[str]:
    ids = set()
    urls = set()
    source_urls: list[str] = []
    for source in manifest["sources"]:
        source_id = source.get("id")
        url = source.get("url")
        if not source_id or not isinstance(source_id, str):
            fail("all sources require string 'id'")
        if source_id in ids:
            fail(f"duplicate source id: {source_id}")
        ids.add(source_id)

        if not url or not isinstance(url, str) or not URL_RE.match(url):
            fail(f"invalid url in source {source_id}: {url}")
        if url in urls:
            fail(f"duplicate source url: {url}")
        urls.add(url)
        source_urls.append(url)

        source_type = source.get("source_type")
        if source_type not in {"official", "external"}:
            fail(f"invalid source_type in {source_id}: {source_type}")

        known_date = source.get("known_date")
        if known_date is not None:
            if isinstance(known_date, date):
                known_date = known_date.isoformat()
            if not isinstance(known_date, str) or not DATE_RE.fullmatch(known_date):
                fail(f"invalid known_date in {source_id}: {known_date}")

    return source_urls


def check_links(urls: list[str]) -> None:
    check = os.getenv("CHECK_LINKS", "0") == "1"
    if not check:
        print("[validate] CHECK_LINKS=0, skipping live link checks")
        return

    timeout = float(os.getenv("LINK_TIMEOUT", "12"))
    failures: list[str] = []

    for url in urls:
        try:
            response = requests.head(url, timeout=timeout, allow_redirects=True)
            status = response.status_code
            if status in {403, 401, 405}:
                print(f"[validate] WARN: non-fatal status {status} for {url}")
                continue
            if status in {404, 410} or status >= 500:
                failures.append(f"{url} -> {status}")
        except requests.RequestException as exc:
            failures.append(f"{url} -> {exc}")

    if failures:
        fail("broken links detected:\n" + "\n".join(failures))


def main() -> None:
    root = Path("docs/codex")
    check_file_structure(root)
    check_markers_and_dates(root)

    manifest = load_manifest(root / "sources.yaml")
    urls = check_sources(manifest)
    check_links(urls)

    print("[validate] OK")


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as exc:
        print(f"[validate] FAIL: {exc}")
        sys.exit(1)
