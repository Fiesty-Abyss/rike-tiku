from __future__ import annotations

import argparse
import copy
import hashlib
import json
import random
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


IMAGE_MARKERS = ("如图", "图中", "如下图", "下图", "图示", "示意图", "装置图", "流程图")
BAD_TEXT_PATTERNS = (
    re.compile(r"关注公众号"),
    re.compile(r"菁优网"),
    re.compile(r"第\s*\d+\s*页（共"),
    re.compile(r"[�□¾]"),
    re.compile(r"[（(]\s*[）)]\s*\d+"),
    re.compile(
        r"(?:结构简式|结构式|电子式|化学式|化学方程式|反应方程式)"
        r"[^。；]{0,16}(?:为|是)[：:]?\s*(?:与|[，；、。]|$)"
    ),
    re.compile(r"故答案为[：:]\s*(?:[，；、。]|$)"),
    re.compile(r"[、，；]{2,}"),
    re.compile(r"\bHCI\b"),
    re.compile(r"二、非选择题"),
    re.compile(r"本题共\s*\d+\s*小题"),
    re.compile(r"【答案】|【解析】|【详解】"),
)
SPLIT_CHARGE_PATTERN = re.compile(
    r"\b(?:[A-Z][a-z]?\d*){1,4}\s+\d+[+﹣−-](?!\d)"
)
REVERSED_FORMULA_PATTERN = re.compile(
    r"(?<![A-Za-z0-9)\].·•])(?:[2-9]\d?\s*){1,2}(?:[A-Z][a-z]?){1,3}"
)
KNOWLEDGE_TARGETS = {
    "化学基本概念": 12,
    "物质结构": 9,
    "化学反应原理": 14,
    "有机化学": 13,
    "实验化学": 12,
}
DIFFICULTY_TARGETS = {"easy": 15, "medium": 36, "hard": 9}
TYPE_TARGETS = {"选择题": 47, "实验与填空": 6, "综合题": 7}
CIRCLED_NUMBERS = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳"


def requires_image(question: dict[str, Any]) -> bool:
    return any(marker in question.get("content", "") for marker in IMAGE_MARKERS)


def grouped_type(question: dict[str, Any]) -> str:
    if question.get("questionType") in {"单选题", "多选题"}:
        return "选择题"
    if question.get("questionType") in {"实验填空题", "填空题"}:
        return "实验与填空"
    if question.get("questionType") == "解答题":
        return "综合题"
    return "未识别"


def multipart_answer_incomplete(question: dict[str, Any]) -> bool:
    if grouped_type(question) == "选择题":
        return False
    content_numbers = set(re.findall(r"[（(](\d+)[）)]", question.get("content") or ""))
    if len(content_numbers) < 2:
        return False
    answer = question.get("correctAnswer") or ""
    answer_numbers = set(re.findall(r"[（(](\d+)[）).]", answer))
    circled_numbers = {character for character in answer if character in CIRCLED_NUMBERS}
    if len(answer_numbers) + len(circled_numbers) < len(content_numbers):
        return True
    marker = rf"(?:[（(]\d+[）).]|[{CIRCLED_NUMBERS}])"
    return bool(re.search(marker + rf"\s*[.．、:：;；]*\s*(?={marker}|$)", answer))


def has_reversed_formula_text(question: dict[str, Any]) -> bool:
    texts = [
        question.get("content") or "",
        question.get("correctAnswer") or "",
        question.get("standardAnalysis") or "",
    ] + [
        str((option or {}).get("content") or "")
        for option in (question.get("options") or [])
    ]
    for text in texts:
        for match in REVERSED_FORMULA_PATTERN.finditer(text):
            local_context = text[max(0, match.start() - 4) : match.end() + 4]
            if not any(operator in local_context for operator in ("=", "→", "ƒ", "⇌", "+")):
                return True
    return False


def has_display_anomaly(question: dict[str, Any]) -> bool:
    content = question.get("content") or ""
    analysis = question.get("standardAnalysis") or ""
    answer = question.get("correctAnswer") or ""
    option_text = " ".join(
        str((option or {}).get("content") or "")
        for option in (question.get("options") or [])
    )
    text = f"{content} {option_text} {answer} {analysis}"
    if any(pattern.search(text) for pattern in BAD_TEXT_PATTERNS):
        return True
    if SPLIT_CHARGE_PATTERN.search(text):
        return True
    if has_reversed_formula_text(question):
        return True
    if len(content) < 6 or len(analysis) < 20:
        return True
    if len(question.get("questionImages") or []) > 4:
        return True
    if not question.get("questionType") or not question.get("knowledgePoints"):
        return True
    if not question.get("difficultyLevel") or not question.get("difficultyReason"):
        return True
    options = question.get("options") or []
    if any(
        re.search(r"(?:^|\s)\d{1,2}[.．、]\s*[A-Z\u4e00-\u9fff]", str((option or {}).get("content") or ""))
        for option in options
    ):
        return True
    blank_options = sum(
        not str((option or {}).get("content") or "").strip() for option in options
    )
    if blank_options > len(question.get("questionImages") or []):
        return True
    if multipart_answer_incomplete(question):
        return True
    if grouped_type(question) == "选择题":
        if len(options) not in {4, 5}:
            return True
        if not re.fullmatch(r"[A-E]{1,5}", str(answer).strip()):
            return True
    return False


def quality_status(question: dict[str, Any]) -> str:
    source = question.get("sourceFile") or {}
    if (
        not str(question.get("paperName") or "").strip()
        or not str(question.get("questionNumber") or "").strip()
        or not str(source.get("answerAnalysis") or "").strip()
        or question.get("reviewStatus") in {"题号无法匹配", "匹配失败"}
    ):
        return "MATCH_FAILED"
    if not question.get("correctAnswer"):
        return "ANSWER_MISSING"
    if not question.get("standardAnalysis"):
        return "ANALYSIS_MISSING"
    if requires_image(question) and not question.get("questionImages"):
        return "IMAGE_MISSING"
    if has_display_anomaly(question):
        return "MANUAL_REVIEW_REQUIRED"
    return "COMPLETE"


def normalized_content(text: str) -> str:
    return re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "", text or "").casefold()


def remove_exact_duplicates(
    questions: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], int]:
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for question in questions:
        groups[normalized_content(question.get("content") or "")].append(question)
    unique: list[dict[str, Any]] = []
    excluded = 0
    for key in sorted(groups):
        members = sorted(
            groups[key],
            key=lambda q: (
                -len(q.get("standardAnalysis") or ""),
                q.get("paperName") or "",
                int(q.get("questionNumber") or 0),
            ),
        )
        unique.append(members[0])
        excluded += len(members) - 1
    return unique, excluded


def secondary_point(question: dict[str, Any]) -> str:
    return " > ".join(question.get("knowledgePoints") or ["未识别"])


def score_selection(selected: list[dict[str, Any]]) -> tuple[int, int, int, str]:
    knowledge = Counter(q["knowledgePoints"][0] for q in selected)
    difficulty = Counter(q["difficultyLevel"] for q in selected)
    sources = Counter(q["paperName"] for q in selected)
    secondary = Counter(secondary_point(q) for q in selected)
    target_penalty = 12 * sum(
        abs(knowledge.get(name, 0) - target) for name, target in KNOWLEDGE_TARGETS.items()
    ) + 10 * sum(
        abs(difficulty.get(name, 0) - target) for name, target in DIFFICULTY_TARGETS.items()
    )
    source_penalty = 500 * sum(max(0, count - 9) for count in sources.values())
    concentration_penalty = sum(count * count for count in sources.values())
    repeat_penalty = 3 * sum(max(0, count - 4) for count in secondary.values())
    digest = hashlib.sha256(
        "|".join(sorted(f"{q['paperName']}#{q['questionNumber']}" for q in selected)).encode("utf-8")
    ).hexdigest()
    return target_penalty + source_penalty + repeat_penalty, concentration_penalty, -len(sources), digest


def select_questions(questions: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    status_distribution = Counter(quality_status(q) for q in questions)
    complete = [q for q in questions if quality_status(q) == "COMPLETE"]
    pool, duplicate_excluded = remove_exact_duplicates(complete)
    if len(pool) <= 60:
        selected = [copy.deepcopy(q) for q in pool]
        best_score = score_selection(selected)
    else:
        selected = []
    buckets = {
        group: [q for q in pool if grouped_type(q) == group]
        for group in TYPE_TARGETS
    }
    if len(pool) > 60:
        for group, target in TYPE_TARGETS.items():
            if len(buckets[group]) < target:
                raise RuntimeError(f"{group}高质量题不足：需要{target}，实际{len(buckets[group])}")

        rng = random.Random(20260803)
        best: list[dict[str, Any]] | None = None
        best_score = None
        for _ in range(90000):
            candidate: list[dict[str, Any]] = []
            for group, target in TYPE_TARGETS.items():
                candidate.extend(rng.sample(buckets[group], target))
            if len({normalized_content(q["content"]) for q in candidate}) != 60:
                continue
            if max(Counter(q["paperName"] for q in candidate).values()) > 10:
                continue
            score = score_selection(candidate)
            if best_score is None or score < best_score:
                best, best_score = candidate, score
                if score[:3] == (0, 364, -10):
                    break
        if best is None:
            raise RuntimeError("未找到满足题型约束的60题组合")
        selected = [copy.deepcopy(q) for q in best]
    selected.sort(
        key=lambda q: (
            {"easy": 0, "medium": 1, "hard": 2}.get(q["difficultyLevel"], 9),
            q["knowledgePoints"][0],
            q["paperName"],
            int(q["questionNumber"]),
        )
    )
    for question in selected:
        if question.get("reviewStatus") != "待审核":
            raise ValueError("拒绝覆盖非待审核 reviewStatus")
        if question.get("analysisReviewStatus") != "待审核":
            raise ValueError("拒绝覆盖非待审核 analysisReviewStatus")

    type_distribution = Counter(q["questionType"] for q in selected)
    grouped_distribution = Counter(grouped_type(q) for q in selected)
    difficulty_distribution = Counter(q["difficultyLevel"] for q in selected)
    knowledge_distribution = Counter(q["knowledgePoints"][0] for q in selected)
    source_distribution = Counter(q["paperName"] for q in selected)
    priority_review = sorted(
        f"{q['paperName']} 第{q['questionNumber']}题"
        for q in selected
        if q["difficultyLevel"] == "hard"
        or len(q.get("questionImages") or []) >= 3
        or any(term in f"{q['content']} {q['standardAnalysis']}" for term in ("结构简式", "同分异构体", "化学方程式", "电极反应"))
    )
    summary = {
        "selectedCount": len(selected),
        "qualityDistribution": dict(sorted(status_distribution.items())),
        "strictCompleteCount": len(complete),
        "completeDirectSelected": len(selected),
        "duplicateExcludedCount": duplicate_excluded,
        "typeDistribution": dict(sorted(type_distribution.items())),
        "groupedTypeDistribution": dict(sorted(grouped_distribution.items())),
        "difficultyDistribution": dict(sorted(difficulty_distribution.items())),
        "knowledgeDistribution": dict(sorted(knowledge_distribution.items())),
        "sourceDistribution": dict(sorted(source_distribution.items())),
        "questionImageCount": sum(len(q.get("questionImages") or []) for q in selected),
        "priorityReview": priority_review,
        "selectionDigest": hashlib.sha256(
            "|".join(f"{q['paperName']}#{q['questionNumber']}" for q in selected).encode("utf-8")
        ).hexdigest(),
    }
    return selected, summary


def main() -> int:
    parser = argparse.ArgumentParser(description="清洗化学候选题库并生成精选60题草案")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    questions = json.loads(args.input.read_text(encoding="utf-8"))
    selected, summary = select_questions(questions)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(selected, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({**summary, "output": str(args.output.resolve())}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
