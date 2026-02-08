#!/usr/bin/env python3
"""Update Codex intelligence docs from digest.json."""

from __future__ import annotations

import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

START_MARKER = "<!-- AUTO-GENERATED:START -->"
END_MARKER = "<!-- AUTO-GENERATED:END -->"
DATE_LINE_PATTERN = re.compile(r'last_updated:\s*"\d{4}-\d{2}-\d{2}"')


DOC_MAP = {
    "strategy_org": "01-strategy-org.md",
    "operations": "02-operations-playbook.md",
    "security_governance": "03-security-governance.md",
    "use_cases_lessons": "04-use-cases-lessons.md",
    "timeline": "05-whats-new-timeline.md",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--digest", required=True)
    parser.add_argument("--docs-root", required=True)
    return parser.parse_args()


def md_escape(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ").strip()


def render_section_table(claims: list[dict[str, Any]]) -> str:
    if not claims:
        return "_更新データなし_"

    lines = [
        "| Date | Type | Claim | Source | Confidence |",
        "|---|---|---|---|---|",
    ]
    for c in claims:
        date = md_escape(c.get("published_or_observed_date", "unknown"))
        source_type = md_escape(c.get("source_type", "unknown"))
        claim = md_escape(c.get("claim", ""))[:180]
        source = md_escape(c.get("source_id", ""))
        url = c.get("url", "")
        conf = md_escape(c.get("confidence", ""))
        lines.append(f"| {date} | {source_type} | {claim} | [{source}]({url}) | {conf} |")
    return "\n".join(lines)


def render_source_matrix(claims: list[dict[str, Any]]) -> str:
    if not claims:
        return ""
    lines = ["", "### Source Matrix", "| claim_id | status | url |", "|---|---|---|"]
    for c in claims:
        cid = md_escape(c.get("claim_id", ""))
        status = md_escape(c.get("status", "ok"))
        url = c.get("url", "")
        lines.append(f"| `{cid}` | {status} | {url} |")
    return "\n".join(lines)


def render_timeline(claims: list[dict[str, Any]]) -> str:
    if not claims:
        return "_更新データなし_"
    lines = [
        "| Date | Update | Source |",
        "|---|---|---|",
    ]
    for c in claims:
        date = md_escape(c.get("published_or_observed_date", "unknown"))
        claim = md_escape(c.get("claim", ""))[:180]
        source = md_escape(c.get("source_id", ""))
        url = c.get("url", "")
        lines.append(f"| {date} | {claim} | [{source}]({url}) |")
    return "\n".join(lines)


def replace_auto_block(content: str, generated: str) -> str:
    if START_MARKER not in content or END_MARKER not in content:
        raise ValueError("auto-generated markers are missing")

    start = content.index(START_MARKER) + len(START_MARKER)
    end = content.index(END_MARKER)
    return content[:start] + "\n" + generated + "\n" + content[end:]


def update_last_updated(content: str, date: str) -> str:
    return DATE_LINE_PATTERN.sub(f'last_updated: "{date}"', content, count=1)


def main() -> None:
    args = parse_args()
    digest_path = Path(args.digest)
    docs_root = Path(args.docs_root)

    digest = json.loads(digest_path.read_text(encoding="utf-8"))
    sections = digest.get("sections", {})
    today = datetime.now(timezone.utc).date().isoformat()

    for section_key, file_name in DOC_MAP.items():
        path = docs_root / file_name
        content = path.read_text(encoding="utf-8")
        claims = sections.get(section_key, [])

        if section_key == "timeline":
            generated_block = render_timeline(claims)
        else:
            generated_block = render_section_table(claims) + render_source_matrix(claims)

        updated = replace_auto_block(content, generated_block)
        updated = update_last_updated(updated, today)
        path.write_text(updated, encoding="utf-8")
        print(f"[update_docs] updated {path}")

    # Update index date as well.
    readme_path = docs_root / "README.md"
    readme = readme_path.read_text(encoding="utf-8")
    readme = update_last_updated(readme, today)
    readme_path.write_text(readme, encoding="utf-8")
    print(f"[update_docs] updated {readme_path}")


if __name__ == "__main__":
    main()
