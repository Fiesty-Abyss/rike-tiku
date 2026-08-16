from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FILES = (
    ROOT / "docs/content/high-frequency-points.v2.json",
    ROOT / "docs/content/topic-units.v2.json",
)
COMMANDS = re.compile(r"\\(?:ce|frac|sqrt|sum|prod|Delta|mu|theta|pi|nu|times|cdot|text|mathrm|mathbf|vec|rightarrow|Rightarrow|propto|infty)\b")


def check_math(value: str, location: str) -> list[str]:
    errors: list[str] = []
    if "\\\\(" in value or "\\\\[" in value:
        errors.append(f"{location}: doubled math delimiter")
    spans: list[tuple[int, int]] = []
    cursor = 0
    while cursor < len(value):
        inline = value.find(r"\(", cursor)
        display = value.find(r"\[", cursor)
        starts = [item for item in (inline, display) if item >= 0]
        if not starts:
            break
        start = min(starts)
        close = r"\)" if start == inline and (display < 0 or inline <= display) else r"\]"
        end = value.find(close, start + 2)
        if end < 0:
            errors.append(f"{location}: unclosed {value[start:start + 2]}")
            break
        spans.append((start, end + 2))
        cursor = end + 2

    for index, (start, end) in enumerate(spans):
        if index and start < spans[index - 1][1]:
            errors.append(f"{location}: nested or overlapping math delimiters")
        expression = value[start + 2 : end - 2]
        if expression.strip() in ("", "..."):
            errors.append(f"{location}: empty or placeholder math expression")
        if r"\ce{...}" in expression:
            errors.append(f"{location}: placeholder mhchem expression")
        if expression.count(r"\ce{"):
            balance = 0
            for char in expression[expression.find(r"\ce{") :]:
                if char == "{":
                    balance += 1
                elif char == "}":
                    balance -= 1
                    if balance < 0:
                        break
            if balance != 0:
                errors.append(f"{location}: unbalanced mhchem braces")

    outside: list[str] = []
    last = 0
    for start, end in spans:
        outside.append(value[last:start])
        last = end
    outside.append(value[last:])
    outside_text = "".join(outside)
    for match in COMMANDS.finditer(outside_text):
        errors.append(f"{location}: bare LaTeX command {match.group(0)}")
    return errors


def walk(value: object, location: str) -> tuple[int, list[str]]:
    if isinstance(value, str):
        return 1, check_math(value, location)
    if isinstance(value, list):
        errors: list[str] = []
        count = 0
        for index, item in enumerate(value):
            item_count, item_errors = walk(item, f"{location}[{index}]")
            count += item_count
            errors.extend(item_errors)
        return count, errors
    if isinstance(value, dict):
        errors = []
        count = 0
        for key, item in value.items():
            item_count, item_errors = walk(item, f"{location}.{key}")
            count += item_count
            errors.extend(item_errors)
        return count, errors
    return 0, []


def mysql_rows(database: str, query: str) -> list[tuple[str, ...]]:
    if database != "rike_tiku":
        raise SystemExit("DATABASE_GUARD_FAILED: only rike_tiku may be scanned")
    password = os.environ.get("RIKE_TIKU_DB_PASSWORD")
    if not password:
        raise SystemExit("DATABASE_SCAN_BLOCKED: RIKE_TIKU_DB_PASSWORD is absent")
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    result = subprocess.run(
        ["mysql", "--default-character-set=utf8mb4", "-uroot", "-h", os.environ.get("RIKE_TIKU_DB_HOST", "localhost"),
         "-P", os.environ.get("RIKE_TIKU_DB_PORT", "3306"), "-N", "-B", database, "-e", query],
        env=env, capture_output=True, text=True, encoding="utf-8", check=False,
    )
    if result.returncode:
        raise SystemExit(f"DATABASE_SCAN_FAILED={result.returncode}")
    return [tuple(line.split("\t")) for line in result.stdout.splitlines() if line]


def scan_database(database: str) -> tuple[int, list[str]]:
    rows = mysql_rows(database, """
        SELECT id,HEX(latex_nei_rong) FROM gao_pin_kao_dian
        WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0 AND latex_nei_rong IS NOT NULL;
        SELECT q.id,HEX(q.ti_gan),HEX(a.jie_xi_nei_rong)
        FROM ti_mu q JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD'
        WHERE q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
          AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 AND a.yi_shan_chu=0;
    """)
    errors: list[str] = []
    for row in rows:
        if len(row) == 2:
            try:
                errors.extend(check_math(bytes.fromhex(row[1]).decode("utf-8"), f"database.card[{row[0]}]"))
            except ValueError:
                errors.append(f"database.card[{row[0]}]: invalid hex payload")
        elif len(row) == 3:
            for column, encoded in (("stem", row[1]), ("analysis", row[2])):
                errors.extend(check_math(bytes.fromhex(encoded).decode("utf-8"), f"database.question[{row[0]}].{column}"))
    return len(rows), errors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", choices=("rike_tiku",), help="also scan the protected formal database")
    args = parser.parse_args()
    total = 0
    errors: list[str] = []
    for path in DEFAULT_FILES:
        if not path.exists():
            errors.append(f"missing content file: {path}")
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"{path}: invalid JSON at {exc.lineno}:{exc.colno}")
            continue
        count, file_errors = walk(data, str(path.relative_to(ROOT)))
        total += count
        errors.extend(file_errors)
    database_rows = 0
    if args.database:
        database_rows, database_errors = scan_database(args.database)
        errors.extend(database_errors)
    print(f"SCIENTIFIC_CONTENT_STRINGS={total}")
    print(f"SCIENTIFIC_DATABASE_ROWS={database_rows}")
    print(f"SCIENTIFIC_CONTENT_ERRORS={len(errors)}")
    for error in errors[:40]:
        print(f"ERROR={error}")
    raise SystemExit(1 if errors else 0)


if __name__ == "__main__":
    main()
