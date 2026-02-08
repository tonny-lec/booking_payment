#!/usr/bin/env python3
"""Fetch Codex intelligence sources and emit normalized raw JSON."""

from __future__ import annotations

import argparse
import concurrent.futures
import hashlib
import html
import json
import re
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

import requests
import yaml


DATE_PATTERNS = [
    re.compile(r'property=["\']article:published_time["\'][^>]*content=["\']([^"\']+)["\']', re.I),
    re.compile(r'name=["\']publish_date["\'][^>]*content=["\']([^"\']+)["\']', re.I),
    re.compile(r'name=["\']date["\'][^>]*content=["\']([^"\']+)["\']', re.I),
    re.compile(r'<time[^>]*datetime=["\']([^"\']+)["\']', re.I),
    re.compile(r'"datePublished"\s*:\s*"([^"]+)"', re.I),
]

TITLE_PATTERN = re.compile(r"<title[^>]*>(.*?)</title>", re.I | re.S)
H_PATTERN = re.compile(r"<(h1|h2)[^>]*>(.*?)</\\1>", re.I | re.S)
TAG_PATTERN = re.compile(r"<[^>]+>")
SCRIPT_STYLE_PATTERN = re.compile(r"<(script|style)[^>]*>.*?</\\1>", re.I | re.S)
WS_PATTERN = re.compile(r"\s+")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--mode", choices=["full", "incremental"], default="full")
    parser.add_argument("--include-external", choices=["true", "false"], default="true")
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--timeout", type=float, default=20.0)
    return parser.parse_args()


def clean_text(value: str) -> str:
    no_tags = TAG_PATTERN.sub(" ", value)
    return WS_PATTERN.sub(" ", html.unescape(no_tags)).strip()


def normalize_date(value: str) -> str | None:
    if not value:
        return None
    m = re.search(r"(\d{4}-\d{2}-\d{2})", value)
    if m:
        return m.group(1)
    m = re.search(r"(\d{4}/\d{2}/\d{2})", value)
    if m:
        return m.group(1).replace("/", "-")
    return None


def normalize_known_date(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, date):
        return value.isoformat()
    return normalize_date(str(value))


def extract_title(html: str) -> str | None:
    m = TITLE_PATTERN.search(html)
    if not m:
        return None
    return clean_text(m.group(1))


def extract_date(html: str) -> str | None:
    for pattern in DATE_PATTERNS:
        m = pattern.search(html)
        if m:
            parsed = normalize_date(m.group(1))
            if parsed:
                return parsed
    return None


def extract_headings(html: str, limit: int = 6) -> list[str]:
    headings: list[str] = []
    for _tag, text in H_PATTERN.findall(html):
        cleaned = clean_text(text)
        if cleaned and cleaned not in headings:
            headings.append(cleaned)
        if len(headings) >= limit:
            break
    return headings


def extract_excerpt(html: str, limit: int = 240) -> str:
    cleaned = SCRIPT_STYLE_PATTERN.sub(" ", html)
    cleaned = clean_text(cleaned)
    return cleaned[:limit]


def fetch_one(source: dict[str, Any], timeout: float, user_agent: str) -> dict[str, Any]:
    now = datetime.now(timezone.utc).isoformat()
    source_id = source.get("id")
    url = source.get("url")
    result: dict[str, Any] = {
        "source_id": source_id,
        "source_name": source.get("name"),
        "url": url,
        "source_type": source.get("source_type", "external"),
        "tags": source.get("tags", []),
        "category": source.get("category"),
        "expected_update": source.get("expected_update"),
        "known_date": normalize_known_date(source.get("known_date")),
        "captured_at": now,
        "status": "ok",
    }

    try:
        response = requests.get(
            url,
            timeout=timeout,
            headers={"User-Agent": user_agent},
            allow_redirects=True,
        )
        html = response.text or ""
        content_type = (response.headers.get("content-type") or "").lower()
        is_html = "text/html" in content_type or "application/xhtml+xml" in content_type
        is_pdf = "application/pdf" in content_type or url.lower().endswith(".pdf")

        title = result.get("source_name") or source_id
        published_date = result.get("known_date")
        headings: list[str] = []
        excerpt = ""

        if response.status_code < 400 and is_html:
            title = extract_title(html) or title
            published_date = extract_date(html) or published_date
            headings = extract_headings(html)
            excerpt = extract_excerpt(html)
        elif response.status_code < 400 and is_pdf:
            excerpt = "PDF document fetched"
        elif response.status_code < 400:
            excerpt = f"Fetched content type: {content_type or 'unknown'}"

        result.update(
            {
                "http_status": response.status_code,
                "final_url": response.url,
                "content_type": content_type,
                "title": title,
                "published_date": published_date,
                "headings": headings,
                "excerpt": excerpt,
                "content_sha256": hashlib.sha256(html.encode("utf-8", errors="ignore")).hexdigest(),
            }
        )
        if response.status_code >= 400:
            result["status"] = "error"
            result["error"] = f"http_{response.status_code}"
            result["title"] = title
            result["excerpt"] = f"Fetch degraded with status {response.status_code}"
    except Exception as exc:  # pragma: no cover - network dependent
        result["status"] = "error"
        result["error"] = str(exc)
        result["title"] = result.get("source_name") or source_id
        result["excerpt"] = "Fetch degraded due to request failure"

    return result


def load_manifest(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict) or "sources" not in data:
        raise ValueError("invalid manifest: 'sources' key is required")
    return data


def main() -> None:
    args = parse_args()
    manifest_path = Path(args.manifest)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    manifest = load_manifest(manifest_path)
    include_external = args.include_external == "true"

    sources = []
    for source in manifest["sources"]:
        if not include_external and source.get("source_type") == "external":
            continue
        sources.append(source)

    user_agent = manifest.get("default_user_agent", "codex-intel-bot/1.0")

    records: list[dict[str, Any]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=max(1, args.workers)) as executor:
        futures = [executor.submit(fetch_one, src, args.timeout, user_agent) for src in sources]
        for future in concurrent.futures.as_completed(futures):
            records.append(future.result())

    records.sort(key=lambda x: x.get("source_id") or "")
    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "mode": args.mode,
        "source_count": len(records),
        "records": records,
    }
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)

    print(f"[fetch_sources] wrote {len(records)} records to {out_path}")


if __name__ == "__main__":
    main()
