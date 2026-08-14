#!/usr/bin/env python3
"""Verify the fixed 22-entry formal thesis reference whitelist without dependencies."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MARKDOWN = ROOT / "docs" / "THESIS_REFERENCES.md"
FORMAL_BIB = ROOT / "docs" / "references" / "references.bib"
RESEARCH_BIB = ROOT / "docs" / "references" / "research-only" / "research_materials.bib"
WARNING = ROOT / "docs" / "references" / "research-only" / "README.md"
THESIS = ROOT / "docs" / "thesis" / "RIKE_THESIS_DRAFT.md"

EXPECTED_KEYS = [
    "yang2023limited", "zheng2023chatgpt", "lu2023education", "wang2023lamp",
    "huang2023transformation", "zhou2023ecosystem", "wei2023feedback", "sun2024platform",
    "wan2024collaborative", "zheng2024regulation", "wei2024science", "mu2024spoc",
    "chu2024youth", "sun2025literacy", "zhang2025visualization", "luo2025ladder",
    "lin2025k12", "wang2025collaborative", "liu2025opensource", "uvarov2026indicators",
    "wang2025biochemistry", "pesek2026governance",
]
EXPECTED_DOIS = {
    "10.16382/j.cnki.1000-5560.2023.07.009", "10.13541/j.cnki.chinade.20230301.001",
    "10.14100/j.cnki.65-1039/g4.20230224.001", "10.13564/j.cnki.issn.1672-9382.2023.03.007",
    "10.13927/j.cnki.yuan.20240422.001", "10.16512/j.cnki.jsjjy.2024.09.017",
    "10.13927/j.cnki.yuan.20250319.001", "10.16209/j.cnki.cust.2025.06.015",
}

def bib_keys(text: str) -> list[str]:
    return re.findall(r"(?m)^@\w+\{([^,]+),", text)

md = MARKDOWN.read_text(encoding="utf-8")
formal = FORMAL_BIB.read_text(encoding="utf-8")
research = RESEARCH_BIB.read_text(encoding="utf-8")
warning = WARNING.read_text(encoding="utf-8")
thesis = THESIS.read_text(encoding="utf-8")

numbers = [int(value) for value in re.findall(r"(?m)^\[(\d+)\]\s", md)]
formal_keys = bib_keys(formal)
research_keys = bib_keys(research)
missing = len(set(EXPECTED_KEYS) - set(formal_keys))
extra = len(set(formal_keys) - set(EXPECTED_KEYS))
foreign_numeric = [int(value) for value in re.findall(r"\[(\d+)\]", thesis) if not 1 <= int(value) <= 22]
foreign_keys = (set(re.findall(r"\[@([A-Za-z][\w:-]*)\]", thesis))
                | set(re.findall(r"\\cite\{([A-Za-z][\w:-]*)", thesis))) - set(EXPECTED_KEYS)
research_key_hits = set(research_keys) & set(re.findall(r"[A-Za-z][\w:-]+", thesis))
all_keys = formal_keys + research_keys
duplicates = len(all_keys) - len(set(all_keys))
dois = set(re.findall(r"(?i)doi\s*=\s*\{([^}]+)\}", formal))
warning_present = int("不得出现在老师审查版本的正文引用、参考文献表或正式 BibTeX 中" in warning)

print(f"FORMAL_REFERENCE_COUNT={len(numbers)}")
print(f"FORMAL_REFERENCE_MISSING={missing}")
print(f"FORMAL_REFERENCE_EXTRA={extra}")
print(f"FORMAL_BIBTEX_COUNT={len(formal_keys)}")
print(f"FORMAL_THESIS_FOREIGN_CITATION={len(foreign_numeric) + len(foreign_keys) + len(research_key_hits)}")
print(f"RESEARCH_ONLY_WARNING_PRESENT={warning_present}")
print(f"DUPLICATE_BIBTEX_KEYS={duplicates}")

ok = (numbers == list(range(1, 23)) and formal_keys == EXPECTED_KEYS and missing == 0 and extra == 0
      and not foreign_numeric and not foreign_keys and not research_key_hits and duplicates == 0
      and dois == EXPECTED_DOIS and warning_present == 1)
sys.exit(0 if ok else 1)
