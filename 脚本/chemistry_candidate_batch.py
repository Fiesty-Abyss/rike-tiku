from __future__ import annotations

import argparse
import hashlib
import json
import re
import traceback
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path

from seed_pdf_pipeline import SeedQuestion, build_questions, extract_pdf_text


IMAGE_MARKERS = ("如图", "图中", "如下图", "下图", "图示", "示意图", "装置图", "流程图")
ANALYSIS_LABELS = ("含解析版", "解析版", "含解析", "含答案", "答案")
REGIONS = (
    "北京", "天津", "上海", "重庆", "河北", "河南", "山东", "山西", "陕西",
    "江苏", "浙江", "安徽", "福建", "江西", "湖北", "湖南", "广东", "广西",
    "海南", "四川", "贵州", "云南", "辽宁", "吉林", "黑龙江", "内蒙古",
    "甘肃", "青海", "宁夏", "新疆", "西藏",
)
COPY_SUFFIX = re.compile(r"\s*[（(]\d+[）)]\s*$")


@dataclass(frozen=True)
class PaperSpec:
    paper_path: Path
    analysis_path: Path
    year: int | None
    region: str | None
    paper_name: str


def strip_copy_suffix(text: str) -> str:
    return COPY_SUFFIX.sub("", text).strip()


def is_original(path: Path) -> bool:
    return "原卷版" in path.stem


def is_analysis(path: Path) -> bool:
    return any(label in path.stem for label in ANALYSIS_LABELS)


def pairing_key(path: Path) -> str:
    text = strip_copy_suffix(path.stem)
    text = re.sub(r"[（(]\s*(?:原卷版|含解析版|解析版|含解析|含答案|答案)\s*[）)]", "", text)
    text = re.sub(r"(?:原卷版|含解析版|解析版|含解析|含答案|答案)", "", text)
    text = re.sub(r"\s*A3\s*$", "", text, flags=re.I)
    text = re.sub(r"[^0-9A-Za-z\u4e00-\u9fffⅰ-ⅹⅠ-Ⅹ]+", "", text)
    return text.casefold()


def clean_paper_name(path: Path) -> str:
    text = strip_copy_suffix(path.stem)
    text = re.sub(r"[（(]\s*(?:原卷版|含解析版|解析版|含解析|含答案|答案)\s*[）)]", "", text)
    text = re.sub(r"(?:原卷版|含解析版|解析版|含解析|含答案|答案)", "", text)
    text = re.sub(r"\s*A3\s*$", "", text, flags=re.I)
    text = text.replace("(", "（").replace(")", "）")
    return re.sub(r"\s+", " ", text).strip(" -—_")


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


def text_digest(path: Path) -> str:
    normalized = re.sub(r"\s+", "", extract_pdf_text(path))
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def representative_rank(path: Path) -> tuple[int, int, str]:
    has_copy_suffix = 1 if COPY_SUFFIX.search(path.stem) else 0
    return has_copy_suffix, len(path.name), str(path)


def unique_text_pdfs(pdfs: list[Path]) -> tuple[list[Path], int]:
    groups: dict[str, list[Path]] = defaultdict(list)
    for path in pdfs:
        groups[text_digest(path)].append(path)
    representatives = [min(paths, key=representative_rank) for paths in groups.values()]
    representatives.sort(key=str)
    return representatives, len(pdfs) - len(representatives)


def inventory_papers(root: Path) -> tuple[list[PaperSpec], list[Path], int, list[str]]:
    raw_pdfs = sorted(
        (path.resolve() for path in root.rglob("*.pdf") if "解析结果" not in path.parts),
        key=str,
    )
    pdfs, duplicate_pdf_count = unique_text_pdfs(raw_pdfs)
    originals = [path for path in pdfs if is_original(path)]
    analysis_by_key: dict[str, list[Path]] = defaultdict(list)
    for path in pdfs:
        if not is_original(path):
            analysis_by_key[pairing_key(path)].append(path)

    specs: list[PaperSpec] = []
    used_analysis: set[Path] = set()
    warnings: list[str] = []
    for original in originals:
        candidates = [
            path for path in analysis_by_key.get(pairing_key(original), [])
            if path not in used_analysis
        ]
        analysis = min(candidates, key=representative_rank) if candidates else None
        if analysis is None:
            warnings.append(f"未找到解析版：{original}")
            analysis = original
        else:
            used_analysis.add(analysis)
        name = clean_paper_name(original)
        specs.append(PaperSpec(original, analysis, infer_year(name), infer_region(name), name))

    for combined in pdfs:
        if is_original(combined) or combined in used_analysis:
            continue
        name = clean_paper_name(combined)
        specs.append(PaperSpec(combined, combined, infer_year(name), infer_region(name), name))

    specs.sort(key=lambda item: (-(item.year or 0), item.paper_name))
    return specs, raw_pdfs, duplicate_pdf_count, warnings


def build_candidate_bank(
    root: Path, images_dir: Path
) -> tuple[list[SeedQuestion], list[dict[str, str | None]], dict[str, object]]:
    specs, raw_pdfs, duplicate_pdf_count, pairing_warnings = inventory_papers(root)
    questions: list[SeedQuestion] = []
    issues: list[dict[str, str | None]] = []
    seen_issues: set[tuple[str, str | None, str]] = set()
    paper_failures = 0
    match_failed_count = 0

    def add_issue(paper: str, number: str | None, kind: str, message: str, source: str) -> None:
        key = (paper, number, kind)
        if key in seen_issues:
            return
        seen_issues.add(key)
        issues.append({
            "paperName": paper, "questionNumber": number, "issueType": kind,
            "message": message, "sourceFile": source,
        })

    for warning in pairing_warnings:
        match_failed_count += 1
        add_issue("PDF配对", None, "MATCH_FAILED", warning, str(root))

    for index, spec in enumerate(specs, start=1):
        print(f"[{index}/{len(specs)}] {spec.paper_name}", flush=True)
        try:
            parsed, failures = build_questions(
                "化学", spec.paper_path, spec.analysis_path, spec.year, spec.region,
                spec.paper_name, images_dir,
            )
        except Exception as exc:
            paper_failures += 1
            add_issue(spec.paper_name, None, "PAPER_PARSE_FAILED", f"{type(exc).__name__}: {exc}", str(spec.paper_path))
            print(traceback.format_exc(limit=2), flush=True)
            continue

        for failure in failures:
            number_match = re.search(r"第(\d+)题", failure)
            number = number_match.group(1) if number_match else None
            if "题号未能配对" in failure:
                match_failed_count += 1
                add_issue(spec.paper_name, number, "MATCH_FAILED", failure, str(spec.analysis_path))
            elif "缺少" not in failure:
                add_issue(spec.paper_name, number, "QUESTION_PARSE_WARNING", failure, str(spec.analysis_path))

        for question in parsed:
            if question.reviewStatus not in {"待审核", "图片缺失"}:
                raise ValueError(f"异常 reviewStatus：{question.reviewStatus}")
            question.reviewStatus = "待审核"
            question.analysisReviewStatus = "待审核"
            needs_image = any(marker in question.content for marker in IMAGE_MARKERS)
            if needs_image and not question.questionImages:
                add_issue(spec.paper_name, question.questionNumber, "IMAGE_MISSING", "题干依赖图片但未提取到题图", str(spec.paper_path))
            if not question.correctAnswer:
                add_issue(spec.paper_name, question.questionNumber, "ANSWER_MISSING", "未能可靠识别答案", str(spec.analysis_path))
            if not question.standardAnalysis:
                add_issue(spec.paper_name, question.questionNumber, "ANALYSIS_MISSING", "未能可靠识别解析", str(spec.analysis_path))
            questions.append(question)

    questions.sort(key=lambda q: (-(q.year or 0), q.paperName, int(q.questionNumber)))
    image_missing = sum(
        any(marker in q.content for marker in IMAGE_MARKERS) and not q.questionImages
        for q in questions
    )
    answer_missing = sum(not q.correctAnswer for q in questions)
    analysis_missing = sum(not q.standardAnalysis for q in questions)
    complete = sum(
        bool(q.content and q.correctAnswer and q.standardAnalysis)
        and not (any(marker in q.content for marker in IMAGE_MARKERS) and not q.questionImages)
        for q in questions
    )
    knowledge = Counter((q.knowledgePoints or ["未识别"])[0] for q in questions)
    difficulty_counts = Counter(q.difficultyLevel or "未识别" for q in questions)
    issue_counts = Counter(str(issue["issueType"]) for issue in issues)
    summary: dict[str, object] = {
        "processedPdfCount": len(raw_pdfs),
        "uniqueTextPdfCount": len(raw_pdfs) - duplicate_pdf_count,
        "duplicatePdfCount": duplicate_pdf_count,
        "logicalPaperCount": len(specs),
        "paperFailureCount": paper_failures,
        "parsedQuestionCount": len(questions),
        "completeCount": complete,
        "imageMissingCount": image_missing,
        "answerMissingCount": answer_missing,
        "analysisMissingCount": analysis_missing,
        "matchFailedCount": match_failed_count,
        "imageCount": sum(len(q.questionImages) for q in questions),
        "issueTypeDistribution": dict(sorted(issue_counts.items())),
        "knowledgeDistribution": dict(sorted(knowledge.items())),
        "difficultyDistribution": dict(sorted(difficulty_counts.items())),
    }
    return questions, issues, summary


def write_log(path: Path, root: Path, issues: list[dict[str, str | None]], summary: dict[str, object]) -> None:
    lines = [
        "化学候选题库批量解析日志",
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
            lines.append(
                f"[{issue['issueType']}] {issue['paperName']} 第{issue['questionNumber'] or '-'}题："
                f"{issue['message']} | {issue['sourceFile']}"
            )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="批量生成化学候选题库")
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--images-dir", required=True, type=Path)
    parser.add_argument("--output-json", required=True, type=Path)
    parser.add_argument("--log", required=True, type=Path)
    args = parser.parse_args()
    if not args.root.is_dir():
        raise NotADirectoryError(f"化学输入目录不存在：{args.root}")
    questions, issues, summary = build_candidate_bank(args.root.resolve(), args.images_dir.resolve())
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(
        json.dumps([asdict(question) for question in questions], ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    write_log(args.log.resolve(), args.root.resolve(), issues, summary)
    print(json.dumps({**summary, "outputJson": str(args.output_json.resolve()), "log": str(args.log.resolve())}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
