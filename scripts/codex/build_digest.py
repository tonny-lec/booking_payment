#!/usr/bin/env python3
"""Build sectioned digest from fetched Codex source records."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SECTION_KEYS = [
    "strategy_org",
    "operations",
    "security_governance",
    "use_cases_lessons",
    "timeline",
]

TAG_TO_SECTION = {
    "strategy_org": "strategy_org",
    "operations": "operations",
    "security_governance": "security_governance",
    "use_cases_lessons": "use_cases_lessons",
    "timeline": "timeline",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--in", dest="in_path", required=True)
    parser.add_argument("--out", required=True)
    return parser.parse_args()


def infer_confidence(source_type: str) -> str:
    if source_type == "official":
        return "high"
    return "medium"


def date_or_observed(item: dict[str, Any]) -> str:
    if item.get("published_date"):
        return item["published_date"]
    if item.get("known_date"):
        return item["known_date"]
    captured = item.get("captured_at", "")
    return captured[:10] if len(captured) >= 10 else "unknown"


def summarize_claim(item: dict[str, Any]) -> str:
    source_name = (item.get("source_name") or "").strip()
    source_id = (item.get("source_id") or "").strip()
    title = (item.get("title") or "").strip()
    headings = item.get("headings") or []
    status = item.get("status")
    error = item.get("error")

    if status != "ok":
        fallback = source_name or source_id or "source"
        if error:
            return f"{fallback} (fetch degraded: {error})"
        return f"{fallback} (fetch degraded)"

    if title and headings:
        return f"{title} / {headings[0]}"
    if title:
        return title
    if headings:
        return headings[0]
    return (item.get("excerpt") or "no summary")[:120]


def build_claim(item: dict[str, Any], claim_idx: int) -> dict[str, Any]:
    return {
        "claim_id": f"{item['source_id']}#{claim_idx}",
        "claim": summarize_claim(item),
        "source_name": item.get("source_name"),
        "source_id": item.get("source_id"),
        "url": item.get("url"),
        "source_type": item.get("source_type"),
        "published_or_observed_date": date_or_observed(item),
        "confidence": infer_confidence(item.get("source_type", "external")),
        "status": item.get("status"),
    }


def normalize_sections(item: dict[str, Any]) -> set[str]:
    tags = item.get("tags") or []
    sections = {TAG_TO_SECTION[tag] for tag in tags if tag in TAG_TO_SECTION}
    if not sections:
        sections.add("use_cases_lessons")
    sections.add("timeline")
    return sections


def main() -> None:
    args = parse_args()
    in_path = Path(args.in_path)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    data = json.loads(in_path.read_text(encoding="utf-8"))
    records = data.get("records", [])

    sections: dict[str, list[dict[str, Any]]] = {k: [] for k in SECTION_KEYS}
    failures: list[dict[str, Any]] = []

    for idx, item in enumerate(records, start=1):
        claim = build_claim(item, idx)
        if item.get("status") != "ok":
            failures.append(claim)
        for section in normalize_sections(item):
            sections[section].append(claim)

    for section in SECTION_KEYS:
        sections[section].sort(
            key=lambda c: c.get("published_or_observed_date", ""),
            reverse=True,
        )

    digest = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source_count": len(records),
        "failed_sources": failures,
        "sections": sections,
    }

    out_path.write_text(json.dumps(digest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"[build_digest] wrote digest to {out_path}")


if __name__ == "__main__":
    main()
