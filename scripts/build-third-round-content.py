"""Validate and normalize the authored topic-learning source used by the formal sync script."""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "docs" / "content" / "topic-units.v2.json"
STAGES = ("FOUNDATION", "TRANSFER", "ADVANCED")
TOPIC_TYPES = {"CALCULATION", "EXPERIMENT", "PROCESS", "MATERIAL_ANALYSIS", "COMPREHENSIVE"}


def normalize(value: object) -> object:
    """Convert the authored visible newline tokens to real line breaks.

    The canonical JSON is intentionally human-readable.  A literal ``\\n`` in
    an authored stem would otherwise reach the rich-text renderer as two visible
    characters, instead of becoming the line break required by a multi-part
    examination question.  Formula backslashes are untouched.
    """
    if isinstance(value, str):
        return value.replace("\\n", "\n")
    if isinstance(value, list):
        return [normalize(item) for item in value]
    if isinstance(value, dict):
        return {key: normalize(item) for key, item in value.items()}
    return value


def main() -> None:
    data = normalize(json.loads(CONTENT.read_text(encoding="utf-8")))
    units = data.get("units", [])
    if data.get("version") != 3 or len(units) != 15:
        raise SystemExit("TOPIC_CONTENT_VERSION_GUARD_FAILED")
    questions = [question for unit in units for question in unit.get("questions", [])]
    if len(questions) != 45 or any([question.get("stage") for question in unit.get("questions", [])] != list(STAGES) for unit in units):
        raise SystemExit("TOPIC_CONTENT_STRUCTURE_GUARD_FAILED")
    if any(question.get("topicType") not in TOPIC_TYPES or not question.get("stem") or len(question.get("standardAnalysis", "")) < 120
           for question in questions):
        raise SystemExit("TOPIC_CONTENT_QUALITY_GUARD_FAILED")
    CONTENT.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("TOPIC_CONTENT_VERSION=3")
    print(f"TOPIC_UNITS={len(units)}")
    print(f"TOPIC_QUESTIONS={len(questions)}")
    print("TOPIC_TYPES=" + ",".join(f"{key}:{value}" for key, value in sorted(Counter(q["topicType"] for q in questions).items())))


if __name__ == "__main__":
    main()
