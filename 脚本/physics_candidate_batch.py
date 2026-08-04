from __future__ import annotations

import argparse
import json
import re
import traceback
from collections import Counter
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path

from seed_pdf_pipeline import SeedQuestion, build_questions


ANALYSIS_LABELS = ("含解析版", "解析版", "含解析", "含答案")
IMAGE_MARKERS = ("如图", "图中", "如下图")
REGIONS = (
    "北京",
    "天津",
    "上海",
    "重庆",
    "河北",
    "河南",
    "山东",
    "山西",
    "陕西",
    "江苏",
    "浙江",
    "安徽",
    "福建",
    "江西",
    "湖北",
    "湖南",
    "广东",
    "广西",
    "海南",
    "四川",
    "贵州",
    "云南",
    "辽宁",
    "吉林",
    "黑龙江",
    "内蒙古",
    "甘肃",
    "青海",
    "宁夏",
    "新疆",
    "西藏",
)


@dataclass(frozen=True)
class PaperSpec:
    paper_path: Path
    analysis_path: Path
    year: int | None
    region: str | None
    paper_name: str


def is_original(path: Path) -> bool:
    return "原卷版" in path.stem


def pairing_key(path: Path) -> str:
    text = path.stem
    text = re.sub(r"[（(]\s*(?:原卷版|含解析版|解析版|含解析|含答案)\s*[）)]", "", text)
    text = re.sub(r"(?:原卷版|含解析版|解析版|含解析|含答案)", "", text)
    text = re.sub(r"[^0-9A-Za-z\u4e00-\u9fffⅰ-ⅹⅠ-Ⅹ]+", "", text)
    return text.casefold()


def clean_paper_name(path: Path) -> str:
    text = path.stem
    text = re.sub(r"[（(]\s*(?:原卷版|含解析版|解析版|含解析|含答案)\s*[）)]", "", text)
    text = re.sub(r"(?:原卷版|含解析版|解析版|含解析|含答案)", "", text)
    text = re.sub(r"\s*A3\s*$", "", text, flags=re.I)
    text = text.replace("(", "（").replace(")", "）")
    text = re.sub(r"\s+", " ", text).strip(" -—_")
    return text


def infer_year(name: str) -> int | None:
    match = re.search(r"(20\d{2})年", name)
    return int(match.group(1)) if match else None


def infer_region(name: str) -> str | None:
    for region in REGIONS:
        if region in name:
            return region
    if "全国" in name or "新课标" in name or "大纲" in name:
        return "全国"
    return None


def inventory_papers(root: Path) -> tuple[list[PaperSpec], list[Path], list[str]]:
    pdfs = sorted(
        (
            path.resolve()
            for path in root.rglob("*.pdf")
            if "解析结果" not in path.parts
        ),
        key=lambda path: str(path),
    )
    originals = [path for path in pdfs if is_original(path)]
    analysis_by_key: dict[str, list[Path]] = {}
    for path in pdfs:
        if is_original(path):
            continue
        analysis_by_key.setdefault(pairing_key(path), []).append(path)

    specs: list[PaperSpec] = []
    used_analysis: set[Path] = set()
    pairing_warnings: list[str] = []
    for original in originals:
        candidates = [
            path
            for path in analysis_by_key.get(pairing_key(original), [])
            if path not in used_analysis
        ]
        analysis = candidates[0] if candidates else None
        if analysis is None:
            pairing_warnings.append(f"未找到解析版：{original}")
            analysis = original
        else:
            used_analysis.add(analysis)
        paper_name = clean_paper_name(original)
        specs.append(
            PaperSpec(
                paper_path=original,
                analysis_path=analysis,
                year=infer_year(paper_name),
                region=infer_region(paper_name),
                paper_name=paper_name,
            )
        )

    for combined in pdfs:
        if is_original(combined) or combined in used_analysis:
            continue
        paper_name = clean_paper_name(combined)
        specs.append(
            PaperSpec(
                paper_path=combined,
                analysis_path=combined,
                year=infer_year(paper_name),
                region=infer_region(paper_name),
                paper_name=paper_name,
            )
        )

    specs.sort(key=lambda item: (-(item.year or 0), item.paper_name))
    return specs, pdfs, pairing_warnings


def issue_key(paper_name: str, number: str | None, issue_type: str) -> tuple[str, str | None, str]:
    return paper_name, number, issue_type


def build_candidate_bank(
    root: Path,
    images_dir: Path,
) -> tuple[list[SeedQuestion], list[dict[str, str | None]], dict[str, object]]:
    specs, pdfs, pairing_warnings = inventory_papers(root)
    questions: list[SeedQuestion] = []
    issues: list[dict[str, str | None]] = []
    seen_issues: set[tuple[str, str | None, str]] = set()
    paper_failures = 0
    unmatched_question_issues = 0

    def add_issue(
        paper_name: str,
        number: str | None,
        issue_type: str,
        message: str,
        source_file: str,
    ) -> None:
        key = issue_key(paper_name, number, issue_type)
        if key in seen_issues:
            return
        seen_issues.add(key)
        issues.append(
            {
                "paperName": paper_name,
                "questionNumber": number,
                "issueType": issue_type,
                "message": message,
                "sourceFile": source_file,
            }
        )

    for warning in pairing_warnings:
        add_issue("PDF配对", None, "ANALYSIS_PDF_NOT_PAIRED", warning, str(root))

    for index, spec in enumerate(specs, start=1):
        print(f"[{index}/{len(specs)}] {spec.paper_name}", flush=True)
        try:
            parsed, failures = build_questions(
                "物理",
                spec.paper_path,
                spec.analysis_path,
                spec.year,
                spec.region,
                spec.paper_name,
                images_dir,
            )
        except Exception as exc:
            paper_failures += 1
            add_issue(
                spec.paper_name,
                None,
                "PAPER_PARSE_FAILED",
                f"{type(exc).__name__}: {exc}",
                str(spec.paper_path),
            )
            print(traceback.format_exc(limit=2), flush=True)
            continue

        for failure in failures:
            number_match = re.search(r"第(\d+)题", failure)
            number = number_match.group(1) if number_match else None
            # 缺答案/解析会在题目记录检查中拆成明确的单项告警，避免重复日志。
            if "缺少" in failure:
                continue
            if "题号未能配对" in failure:
                issue_type = "QUESTION_NUMBER_MISMATCH"
                unmatched_question_issues += 1
            else:
                issue_type = "QUESTION_PARSE_WARNING"
            add_issue(spec.paper_name, number, issue_type, failure, str(spec.analysis_path))

        for question in parsed:
            question.reviewStatus = "待审核"
            question.analysisReviewStatus = "待审核"
            needs_image = any(marker in question.content for marker in IMAGE_MARKERS)
            if needs_image and not question.questionImages:
                add_issue(
                    spec.paper_name,
                    question.questionNumber,
                    "IMAGE_MISSING",
                    "题干包含图示提示词，但未提取到题图",
                    str(spec.paper_path),
                )
            if not question.correctAnswer:
                add_issue(
                    spec.paper_name,
                    question.questionNumber,
                    "ANSWER_MISSING",
                    "未能可靠识别答案",
                    str(spec.analysis_path),
                )
            if not question.standardAnalysis:
                add_issue(
                    spec.paper_name,
                    question.questionNumber,
                    "ANALYSIS_MISSING",
                    "未能可靠识别解析",
                    str(spec.analysis_path),
                )
            questions.append(question)

    questions.sort(
        key=lambda item: (
            -(item.year or 0),
            item.paperName,
            int(item.questionNumber),
        )
    )
    complete_questions = [
        question
        for question in questions
        if question.content
        and question.correctAnswer
        and question.standardAnalysis
        and not (
            any(marker in question.content for marker in IMAGE_MARKERS)
            and not question.questionImages
        )
    ]
    incomplete_records = len(questions) - len(complete_questions)
    issue_type_counts = Counter(str(issue["issueType"]) for issue in issues)
    knowledge_distribution: Counter[str] = Counter()
    for question in questions:
        if question.knowledgePoints:
            knowledge_distribution[question.knowledgePoints[0]] += 1
        else:
            knowledge_distribution["未识别"] += 1
    difficulty_distribution = Counter(
        question.difficultyLevel or "未识别" for question in questions
    )
    summary: dict[str, object] = {
        "processedPdfCount": len(pdfs),
        "logicalPaperCount": len(specs),
        "paperFailureCount": paper_failures,
        "parsedQuestionCount": len(questions),
        "successQuestionCount": len(complete_questions),
        "incompleteRecordCount": incomplete_records,
        "unmatchedQuestionCount": unmatched_question_issues,
        "failureCount": incomplete_records + unmatched_question_issues + paper_failures,
        "imageCount": sum(len(question.questionImages) for question in questions),
        "issueTypeDistribution": dict(sorted(issue_type_counts.items())),
        "knowledgeDistribution": dict(sorted(knowledge_distribution.items())),
        "difficultyDistribution": dict(sorted(difficulty_distribution.items())),
    }
    return questions, issues, summary


def write_log(
    path: Path,
    root: Path,
    issues: list[dict[str, str | None]],
    summary: dict[str, object],
) -> None:
    lines = [
        "物理候选题库批量解析日志",
        f"采集时间：{datetime.now().astimezone().isoformat(timespec='seconds')}",
        f"输入目录：{root}",
        "运行方式：离线、单线程、不连接数据库",
        "",
        "汇总：",
        json.dumps(summary, ensure_ascii=False, indent=2),
        "",
        "问题明细：",
    ]
    if not issues:
        lines.append("无")
    else:
        for issue in issues:
            number = issue["questionNumber"] or "-"
            lines.append(
                f"[{issue['issueType']}] {issue['paperName']} 第{number}题："
                f"{issue['message']} | {issue['sourceFile']}"
            )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="批量生成物理候选题库")
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--images-dir", required=True, type=Path)
    parser.add_argument("--output-json", required=True, type=Path)
    parser.add_argument("--log", required=True, type=Path)
    args = parser.parse_args()

    if not args.root.is_dir():
        raise NotADirectoryError(f"物理输入目录不存在：{args.root}")
    questions, issues, summary = build_candidate_bank(
        args.root.resolve(),
        args.images_dir.resolve(),
    )
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(
        json.dumps([asdict(question) for question in questions], ensure_ascii=False, indent=2)
        + "\n",
        encoding="utf-8",
    )
    write_log(args.log.resolve(), args.root.resolve(), issues, summary)
    print(
        json.dumps(
            {
                **summary,
                "outputJson": str(args.output_json.resolve()),
                "log": str(args.log.resolve()),
            },
            ensure_ascii=False,
            indent=2,
        ),
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
