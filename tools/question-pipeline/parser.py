from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass
from typing import Any

from models import Question


QUESTION_START = re.compile(r"(?m)^\s*(\d{1,3})\s*[.．、]\s*(?=\S)")
OPTION_START = re.compile(r"(?m)^\s*([A-H])\s*[.．、:：]\s*(.+?)(?=^\s*[A-H]\s*[.．、:：]|\Z)", re.S)
ANALYSIS_MARK = re.compile(r"(?m)^\s*(?:【?解析】?|解析)\s*[:：]\s*")
SECTION_HEADING = re.compile(
    r"(?m)^\s*(?:[一二三四五六七八九十]+[、.．]|第[一二三四五六七八九十]+部分)"
    r"\s*(.+?)\s*$"
)


@dataclass
class ParsedBlock:
    number: str
    body: str
    question_type: str | None


def _normalize(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n").strip()


def _explicit_question_type(heading: str | None) -> str | None:
    if not heading:
        return None
    mappings = (
        ("选择题", "选择题"),
        ("填空题", "填空题"),
        ("解答题", "解答题"),
        ("材料题", "材料题"),
        ("作文", "作文题"),
        ("判断题", "判断题"),
    )
    for marker, value in mappings:
        if marker in heading:
            return value
    return None


def split_numbered_blocks(text: str) -> list[ParsedBlock]:
    text = _normalize(text)
    matches = list(QUESTION_START.finditer(text))
    headings = list(SECTION_HEADING.finditer(text))
    blocks: list[ParsedBlock] = []
    for index, match in enumerate(matches):
        previous_headings = [
            heading for heading in headings if heading.start() < match.start()
        ]
        current_heading = (
            previous_headings[-1].group(1) if previous_headings else None
        )
        next_question_start = (
            matches[index + 1].start() if index + 1 < len(matches) else len(text)
        )
        next_headings = [
            heading.start()
            for heading in headings
            if match.end() < heading.start() < next_question_start
        ]
        end = min(next_headings) if next_headings else next_question_start
        blocks.append(
            ParsedBlock(
                number=match.group(1),
                body=text[match.end() : end].strip(),
                question_type=_explicit_question_type(current_heading),
            )
        )
    return blocks


def _parse_options(body: str) -> tuple[str, list[dict[str, str]] | None]:
    matches = list(OPTION_START.finditer(body))
    if len(matches) < 2:
        return body.strip(), None
    first = matches[0]
    content = body[: first.start()].strip()
    options = [
        {"label": match.group(1), "content": " ".join(match.group(2).split())}
        for match in matches
    ]
    return content, options


def parse_answers(text: str | None) -> dict[str, tuple[str | None, str | None]]:
    if not text:
        return {}
    answers: dict[str, tuple[str | None, str | None]] = {}
    for block in split_numbered_blocks(text):
        parts = ANALYSIS_MARK.split(block.body, maxsplit=1)
        answer = parts[0].strip() or None
        analysis = parts[1].strip() if len(parts) == 2 and parts[1].strip() else None
        answers[block.number] = (answer, analysis)
    return answers


def parse_questions(
    paper_text: str,
    answer_text: str | None,
    metadata: dict[str, Any],
    collected_at: str,
) -> tuple[list[Question], list[dict[str, Any]]]:
    answer_map = parse_answers(answer_text)
    questions: list[Question] = []
    review_items: list[dict[str, Any]] = []
    seen_hashes: dict[str, str] = {}
    answer_source = metadata.get("answerSource") or {}
    analysis_source = metadata.get("analysisSource") or {}

    for block in split_numbered_blocks(paper_text):
        content, options = _parse_options(block.body)
        answer, analysis = answer_map.get(block.number, (None, None))
        normalized = "\n".join(
            [content]
            + [
                f"{option['label']}:{option['content']}"
                for option in (options or [])
            ]
        )
        content_hash = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
        duplicate_of = seen_hashes.get(content_hash)
        if duplicate_of is None:
            seen_hashes[content_hash] = block.number

        issues: list[str] = []
        if not content:
            issues.append("EMPTY_CONTENT")
        if block.question_type is None:
            issues.append("QUESTION_TYPE_UNRECOGNIZED")
        if answer is None:
            issues.append("MISSING_ANSWER")
        elif not answer_source:
            issues.append("MISSING_ANSWER_SOURCE")
        elif answer_source.get("licenseStatus") == "COPYRIGHT_UNKNOWN":
            issues.append("ANSWER_COPYRIGHT_UNKNOWN_DO_NOT_IMPORT")
        if analysis is None:
            issues.append("MISSING_ANALYSIS")
        elif not analysis_source:
            issues.append("MISSING_ANALYSIS_SOURCE")
        elif analysis_source.get("licenseStatus") == "COPYRIGHT_UNKNOWN":
            issues.append("ANALYSIS_COPYRIGHT_UNKNOWN_DO_NOT_IMPORT")
        if duplicate_of is not None:
            issues.append(f"DUPLICATE_OF_{duplicate_of}")
        if metadata["licenseStatus"] == "COPYRIGHT_UNKNOWN":
            issues.append("COPYRIGHT_UNKNOWN_DO_NOT_IMPORT")

        review_status = "NEEDS_REVIEW"
        questions.append(
            Question(
                subject=metadata.get("subject"),
                year=metadata.get("year"),
                region=metadata.get("region"),
                paperType=metadata.get("paperType"),
                questionType=block.question_type,
                content=content,
                options=options,
                correctAnswer=answer,
                standardAnalysis=analysis,
                difficultyLevel=None,
                knowledgePoints=None,
                sourceName=metadata["sourceName"],
                sourceUrl=metadata["sourceUrl"],
                sourceCategory=metadata["sourceCategory"],
                licenseStatus=metadata["licenseStatus"],
                sourceRightsEvidence=metadata["rightsEvidence"],
                answerSourceName=answer_source.get("sourceName") if answer else None,
                answerSourceUrl=answer_source.get("sourceUrl") if answer else None,
                answerSourceCategory=(
                    answer_source.get("sourceCategory") if answer else None
                ),
                answerLicenseStatus=(
                    answer_source.get("licenseStatus") if answer else None
                ),
                answerRightsEvidence=(
                    answer_source.get("rightsEvidence") if answer else None
                ),
                analysisSourceName=(
                    analysis_source.get("sourceName") if analysis else None
                ),
                analysisSourceUrl=(
                    analysis_source.get("sourceUrl") if analysis else None
                ),
                analysisSourceCategory=(
                    analysis_source.get("sourceCategory") if analysis else None
                ),
                analysisLicenseStatus=(
                    analysis_source.get("licenseStatus") if analysis else None
                ),
                analysisRightsEvidence=(
                    analysis_source.get("rightsEvidence") if analysis else None
                ),
                collectedAt=collected_at,
                reviewStatus=review_status,
                questionNumber=block.number,
                contentHash=content_hash,
            )
        )
        if issues:
            review_items.append(
                {"questionNumber": block.number, "issues": issues}
            )

    paper_numbers = {question.questionNumber for question in questions}
    for number in sorted(set(answer_map) - paper_numbers, key=lambda value: int(value)):
        review_items.append(
            {"questionNumber": number, "issues": ["ANSWER_WITHOUT_QUESTION"]}
        )
    return questions, review_items
