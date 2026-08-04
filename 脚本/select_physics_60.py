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


IMAGE_MARKERS = ("如图", "图中", "如下图")
BAD_TEXT_PATTERNS = (
    re.compile(r"关注公众号"),
    re.compile(r"菁优网"),
    re.compile(r"第\s*\d+\s*页（共"),
    re.compile(r"[æöç]"),
    re.compile(r"�"),
    re.compile(r"□"),
)
KNOWLEDGE_TARGETS = {
    "力学": 20,
    "电磁学": 20,
    "热学": 8,
    "光学": 7,
    "近代物理": 5,
}
CHOICE_KNOWLEDGE_TARGETS = {
    "力学": 12,
    "电磁学": 12,
    "热学": 7,
    "光学": 6,
    "近代物理": 5,
}


def requires_image(question: dict[str, Any]) -> bool:
    return any(marker in question.get("content", "") for marker in IMAGE_MARKERS)


def has_display_anomaly(question: dict[str, Any]) -> bool:
    content = question.get("content") or ""
    analysis = question.get("standardAnalysis") or ""
    answer = question.get("correctAnswer") or ""
    text = f"{content} {analysis} {answer}"
    if any(pattern.search(text) for pattern in BAD_TEXT_PATTERNS):
        return True
    if len(content) < 18 or len(analysis) < 20:
        return True
    if question.get("questionType") in {"单选题", "多选题"} and not question.get("options"):
        return True
    options = question.get("options")
    if options and len(options) not in {4, 5}:
        return True
    if (
        options
        and any(not str((option or {}).get("content") or "").strip() for option in options)
        and not question.get("questionImages")
    ):
        return True
    return False


def quality_status(question: dict[str, Any]) -> str:
    source_file = question.get("sourceFile") or {}
    if (
        not str(question.get("paperName") or "").strip()
        or not str(question.get("questionNumber") or "").strip()
        or not str(source_file.get("answerAnalysis") or "").strip()
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


def effective_type(question: dict[str, Any]) -> str:
    content = question.get("content") or ""
    subparts = len(re.findall(r"[（(]\d+[）)]", content))
    is_experiment = any(word in content for word in ("实验", "测量", "探究", "验证"))
    if question.get("questionType") == "实验填空题":
        return "实验填空题"
    if is_experiment and (subparts >= 2 or "____" in content):
        return "实验填空题"
    if question.get("questionType") in {"单选题", "多选题"}:
        return "选择题"
    if question.get("questionType") == "解答题":
        return "计算题"
    return question.get("questionType") or "未识别"


def supplement_selected_question(question: dict[str, Any]) -> dict[str, Any]:
    result = copy.deepcopy(question)
    if effective_type(result) == "实验填空题":
        result["questionType"] = "实验填空题"
        if not result.get("knowledgePoints"):
            result["knowledgePoints"] = ["力学", "力学实验"]
        if result.get("difficultyLevel") in {None, "easy"} and (
            result["paperName"] == "2021年北京市高考物理试卷"
            and result["questionNumber"] == "16"
            or result.get("difficultyLevel") is None
        ):
            result["difficultyLevel"] = "medium"
            result["difficultyReason"] = "需要完成实验原理判断、数据处理或多个操作步骤。"
    if result.get("reviewStatus") != "待审核":
        raise ValueError("候选题 reviewStatus 不是待审核，拒绝覆盖原状态")
    if result.get("analysisReviewStatus") != "待审核":
        raise ValueError("候选题 analysisReviewStatus 不是待审核，拒绝覆盖原状态")
    return result


def normalized_content(content: str) -> str:
    return re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "", content).casefold()


def duplicate_extra_count(questions: list[dict[str, Any]]) -> int:
    groups: Counter[str] = Counter(
        normalized_content(question.get("content") or "") for question in questions
    )
    return sum(count - 1 for key, count in groups.items() if key and count > 1)


def secondary_point(question: dict[str, Any]) -> str:
    points = question.get("knowledgePoints") or []
    return " > ".join(points) if points else "未识别"


def selection_score(
    fixed: list[dict[str, Any]],
    choices: list[dict[str, Any]],
) -> float:
    selected = fixed + choices
    paper_counts = Counter(question["paperName"] for question in selected)
    point_counts = Counter(secondary_point(question) for question in selected)
    score = 0.0
    score += sum(count * count for count in paper_counts.values()) * 2.5
    score += sum(max(0, count - 3) ** 2 for count in point_counts.values()) * 4.0
    score -= len(paper_counts) * 3.0
    for question in choices:
        analysis_length = len(question.get("standardAnalysis") or "")
        if analysis_length < 70:
            score += 3.0
        if analysis_length > 800:
            score += 2.0
        if question.get("questionType") == "多选题":
            score -= 0.3
    return score


def select_questions(
    questions: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    quality_counts = Counter(quality_status(question) for question in questions)
    complete = [question for question in questions if quality_status(question) == "COMPLETE"]

    experiments = [
        supplement_selected_question(question)
        for question in complete
        if effective_type(question) == "实验填空题"
    ]
    calculations = [
        supplement_selected_question(question)
        for question in complete
        if effective_type(question) == "计算题"
        and question.get("difficultyLevel") == "hard"
        and question.get("knowledgePoints")
    ]
    # 两道力学计算题与同卷其他高质量实验题重叠较多，留在候选池而不进入草案。
    calculation_exclusions = {
        ("2021年北京市高考物理试卷", "20"),
        ("2021年浙江省物理选考（6月）物理试卷", "20"),
    }
    calculations = [
        question
        for question in calculations
        if (question["paperName"], question["questionNumber"])
        not in calculation_exclusions
    ]
    if len(experiments) != 12:
        raise ValueError(f"严格质量池中的实验题应为 12 道，实际为 {len(experiments)}")
    if len(calculations) != 6:
        raise ValueError(f"严格质量池中的高难计算题应为 6 道，实际为 {len(calculations)}")
    fixed = experiments + calculations

    choice_pool = [
        supplement_selected_question(question)
        for question in complete
        if effective_type(question) == "选择题"
        and question.get("knowledgePoints")
        and question.get("difficultyLevel") in {"easy", "medium"}
    ]
    by_knowledge: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for question in choice_pool:
        by_knowledge[question["knowledgePoints"][0]].append(question)
    for knowledge, target in CHOICE_KNOWLEDGE_TARGETS.items():
        if len(by_knowledge[knowledge]) < target:
            raise ValueError(f"{knowledge}选择题不足：需要 {target}，实际 {len(by_knowledge[knowledge])}")

    rng = random.Random(1)
    best: list[dict[str, Any]] | None = None
    best_score = float("inf")
    fixed_paper_counts = Counter(question["paperName"] for question in fixed)
    for _ in range(500_000):
        choices: list[dict[str, Any]] = []
        for knowledge, target in CHOICE_KNOWLEDGE_TARGETS.items():
            choices.extend(rng.sample(by_knowledge[knowledge], target))
        difficulty = Counter(question["difficultyLevel"] for question in choices)
        if difficulty != Counter({"medium": 27, "easy": 15}):
            continue
        paper_counts = fixed_paper_counts.copy()
        paper_counts.update(question["paperName"] for question in choices)
        if max(paper_counts.values()) > 6:
            continue
        score = selection_score(fixed, choices)
        if score < best_score:
            best_score = score
            best = choices
    if best is None:
        raise RuntimeError("未找到满足题型、知识点、难度和来源上限的选择题组合")

    selected = fixed + best
    order = {"选择题": 0, "实验填空题": 1, "计算题": 2}
    selected.sort(
        key=lambda question: (
            order[effective_type(question)],
            question["knowledgePoints"][0],
            -(question.get("year") or 0),
            question["paperName"],
            int(question["questionNumber"]),
        )
    )
    if len(selected) != 60:
        raise AssertionError(f"精选数量不是 60：{len(selected)}")
    if len({(q["paperName"], q["questionNumber"]) for q in selected}) != 60:
        raise AssertionError("精选结果包含重复复合题号")
    normalized = [normalized_content(question["content"]) for question in selected]
    if len(normalized) != len(set(normalized)):
        raise AssertionError("精选结果包含重复题干")

    type_distribution = Counter(effective_type(question) for question in selected)
    difficulty_distribution = Counter(question["difficultyLevel"] for question in selected)
    knowledge_distribution = Counter(question["knowledgePoints"][0] for question in selected)
    source_distribution = Counter(question["paperName"] for question in selected)
    if type_distribution != Counter({"选择题": 42, "实验填空题": 12, "计算题": 6}):
        raise AssertionError(type_distribution)
    if difficulty_distribution != Counter({"medium": 38, "easy": 15, "hard": 7}):
        raise AssertionError(difficulty_distribution)
    if knowledge_distribution != Counter(KNOWLEDGE_TARGETS):
        raise AssertionError(knowledge_distribution)

    priority_review = sorted(
        {
            f"{question['paperName']} 第{question['questionNumber']}题"
            for question in selected
            if question["difficultyLevel"] == "hard"
            or len(question.get("questionImages") or []) >= 3
        }
    )
    summary = {
        "selectedCount": len(selected),
        "qualityDistribution": dict(sorted(quality_counts.items())),
        "completeDirectSelected": len(selected),
        "pageCropImageCount": 0,
        "qualityRejectedCount": len(questions) - quality_counts["COMPLETE"],
        "duplicateExcludedCount": duplicate_extra_count(questions),
        "typeDistribution": dict(sorted(type_distribution.items())),
        "difficultyDistribution": dict(sorted(difficulty_distribution.items())),
        "knowledgeDistribution": dict(sorted(knowledge_distribution.items())),
        "sourceDistribution": dict(sorted(source_distribution.items())),
        "priorityReview": priority_review,
        "selectionDigest": hashlib.sha256(
            "\n".join(
                f"{q['paperName']}#{q['questionNumber']}" for q in selected
            ).encode("utf-8")
        ).hexdigest(),
    }
    return selected, summary


def main() -> int:
    parser = argparse.ArgumentParser(description="从物理候选题库生成精选 60 题待审核草案")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    questions = json.loads(args.input.read_text(encoding="utf-8"))
    if not isinstance(questions, list) or len(questions) != 687:
        raise ValueError("输入必须是经过验证的 687 道物理候选题")
    selected, summary = select_questions(questions)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(selected, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        json.dumps(
            {**summary, "output": str(args.output.resolve())},
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
