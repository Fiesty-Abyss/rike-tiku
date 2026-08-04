from __future__ import annotations

import argparse
import json
import sys
import traceback
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from adapters.local_files import load_sample, read_source_text
from parser import parse_questions


PROJECT_ROOT = Path(__file__).resolve().parent.parent
QUESTION_ROOT = PROJECT_ROOT / "题库"
DEFAULT_SUBJECTS = (
    "语文",
    "数学",
    "英语",
    "物理",
    "化学",
    "生物",
    "历史",
    "地理",
    "思想政治",
)


def resolve_subject_root(subject: str) -> Path:
    subject_root = (QUESTION_ROOT / subject).resolve()
    if subject_root.parent != QUESTION_ROOT.resolve():
        raise ValueError("学科名不能跳出题库目录")
    return subject_root


def subject_paths(subject: str) -> dict[str, Path]:
    root = resolve_subject_root(subject)
    return {
        "root": root,
        "raw": root / "原始文件",
        "pending": root / "解析结果" / "待审核",
        "reviewed": root / "人工审核结果",
        "state": root / "状态",
        "log": root / "日志",
    }


def ensure_subject_directories(subject: str) -> dict[str, Path]:
    paths = subject_paths(subject)
    for key, path in paths.items():
        if key == "root":
            continue
        path.mkdir(parents=True, exist_ok=True)
    return paths


def ensure_default_subjects() -> None:
    for subject in DEFAULT_SUBJECTS:
        ensure_subject_directories(subject)


def write_json(path: Path, value: Any) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def log_event(log_root: Path, level: str, event: str, **details: Any) -> None:
    record = {
        "timestamp": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "level": level,
        "event": event,
        **details,
    }
    log_path = log_root / "run.jsonl"
    with log_path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def load_state(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def add_review_issue(
    review_items: list[dict[str, Any]], question_number: str | None, issue: str
) -> None:
    for item in review_items:
        if item["questionNumber"] == question_number:
            if issue not in item["issues"]:
                item["issues"].append(issue)
            return
    review_items.append({"questionNumber": question_number, "issues": [issue]})


def prepare_hash_index(
    index_path: Path, sample_name: str
) -> dict[str, dict[str, list[dict[str, str]]]]:
    index = load_state(index_path)
    cleaned: dict[str, dict[str, list[dict[str, str]]]] = {
        "rawFiles": {},
        "questions": {},
    }
    for category in cleaned:
        for content_hash, references in index.get(category, {}).items():
            remaining = [
                reference
                for reference in references
                if reference.get("sample") != sample_name
            ]
            if remaining:
                cleaned[category][content_hash] = remaining
    return cleaned


def list_subjects() -> int:
    subjects = sorted(
        path.name
        for path in QUESTION_ROOT.iterdir()
        if path.is_dir() and (path / "原始文件").is_dir()
    )
    for subject in subjects:
        print(subject)
    return 0


def list_samples(subject: str) -> int:
    paths = ensure_subject_directories(subject)
    samples = sorted(path.name for path in paths["raw"].iterdir() if path.is_dir())
    if not samples:
        print(f"未发现样本。请把一套试卷放入：{paths['raw']}")
        return 2
    for sample in samples:
        print(sample)
    return 0


def run(subject: str, sample_name: str, force: bool) -> int:
    paths = ensure_subject_directories(subject)
    sample = load_sample(paths["raw"], sample_name)
    metadata_subject = sample.metadata.get("subject")
    if metadata_subject != subject:
        raise ValueError(
            f"metadata.json 的 subject 必须与学科目录一致："
            f"目录={subject!r}，元数据={metadata_subject!r}"
        )
    state_path = paths["state"] / f"{sample.name}.json"
    output_path = paths["pending"] / f"{sample.name}.json"
    state = load_state(state_path)
    if (
        not force
        and state.get("status") == "COMPLETED"
        and state.get("bundleHash") == sample.bundle_hash
        and output_path.is_file()
    ):
        print(f"样本未变化，已从断点跳过：{sample.name}")
        log_event(
            paths["log"],
            "INFO",
            "SAMPLE_SKIPPED_UNCHANGED",
            subject=subject,
            sample=sample.name,
        )
        return 0

    collected_at = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
    paper_text = read_source_text(sample.paper_path)
    answer_text = read_source_text(sample.answer_path) if sample.answer_path else None
    questions, review_items = parse_questions(
        paper_text, answer_text, sample.metadata, collected_at
    )
    if not questions:
        add_review_issue(review_items, None, "NO_QUESTIONS_RECOGNIZED")
    index_path = paths["state"] / "content_hash_index.json"
    hash_index = prepare_hash_index(index_path, sample.name)
    raw_file_records: list[dict[str, Any]] = []
    duplicate_file_count = 0
    for name, digest in sorted(sample.file_hashes.items()):
        duplicate_of = hash_index["rawFiles"].get(digest, [])
        if name != "metadata.json" and duplicate_of:
            duplicate_file_count += 1
        raw_file_records.append(
            {"name": name, "sha256": digest, "duplicateOf": duplicate_of}
        )
        hash_index["rawFiles"].setdefault(digest, []).append(
            {"sample": sample.name, "file": name}
        )

    cross_sample_duplicate_count = 0
    for question in questions:
        duplicate_of = hash_index["questions"].get(question.contentHash, [])
        if duplicate_of:
            cross_sample_duplicate_count += 1
            references = ",".join(
                f"{reference['sample']}#{reference['questionNumber']}"
                for reference in duplicate_of
            )
            add_review_issue(
                review_items,
                question.questionNumber,
                f"DUPLICATE_CROSS_SAMPLE:{references}",
            )
        hash_index["questions"].setdefault(question.contentHash, []).append(
            {"sample": sample.name, "questionNumber": question.questionNumber}
        )

    within_sample_duplicate_count = sum(
        any(issue.startswith("DUPLICATE_OF_") for issue in item["issues"])
        for item in review_items
    )
    duplicate_question_count = (
        within_sample_duplicate_count + cross_sample_duplicate_count
    )
    missing_answer_count = sum(q.correctAnswer is None for q in questions)
    missing_analysis_count = sum(q.standardAnalysis is None for q in questions)
    parse_failure_count = sum(not q.content for q in questions) + (not questions)
    used_sources = [sample.metadata]
    if any(q.correctAnswer is not None for q in questions):
        used_sources.append(sample.metadata.get("answerSource") or {})
    if any(q.standardAnalysis is not None for q in questions):
        used_sources.append(sample.metadata.get("analysisSource") or {})
    import_eligible = (
        bool(questions)
        and parse_failure_count == 0
        and all(
            source.get("licenseStatus") not in {None, "COPYRIGHT_UNKNOWN"}
            for source in used_sources
        )
    )

    result = {
        "schemaVersion": "1.0",
        "sampleName": sample.name,
        "generatedAt": collected_at,
        "importEligible": import_eligible,
        "importRestriction": (
            None
            if import_eligible
            else "题干、答案或解析的来源/权利信息缺失或不明确，不得进入正式题库"
        ),
        "sourceCompliance": {
            "questionSource": {
                "sourceName": sample.metadata["sourceName"],
                "sourceUrl": sample.metadata["sourceUrl"],
                "sourceCategory": sample.metadata["sourceCategory"],
                "licenseStatus": sample.metadata["licenseStatus"],
                "rightsEvidence": sample.metadata["rightsEvidence"],
            },
            "answerSource": sample.metadata.get("answerSource"),
            "analysisSource": sample.metadata.get("analysisSource"),
            "termsCheckedAt": sample.metadata.get("termsCheckedAt"),
            "robotsCheckedAt": sample.metadata.get("robotsCheckedAt"),
            "accessRestrictions": sample.metadata.get("accessRestrictions"),
            "localOnly": True,
            "networkRequestsMade": 0,
        },
        "rawFiles": raw_file_records,
        "statistics": {
            "downloadedFiles": 0,
            "readFiles": len(sample.file_hashes) - 1,
            "parsedQuestions": len(questions) - parse_failure_count,
            "parseFailures": parse_failure_count,
            "duplicates": duplicate_file_count + duplicate_question_count,
            "duplicateFiles": duplicate_file_count,
            "duplicateQuestions": duplicate_question_count,
            "missingAnswers": missing_answer_count,
            "missingAnalyses": missing_analysis_count,
        },
        "questions": [question.to_dict() for question in questions],
        "manualReviewChecklist": review_items,
    }
    write_json(output_path, result)
    write_json(index_path, hash_index)
    write_json(
        state_path,
        {
            "sampleName": sample.name,
            "status": (
                "COMPLETED" if parse_failure_count == 0 else "COMPLETED_WITH_ERRORS"
            ),
            "bundleHash": sample.bundle_hash,
            "output": str(output_path),
            "completedAt": collected_at,
            "statistics": result["statistics"],
        },
    )
    log_event(
        paths["log"],
        "INFO",
        "SAMPLE_COMPLETED",
        subject=subject,
        sample=sample.name,
        output=str(output_path),
        statistics=result["statistics"],
    )
    print(json.dumps(result["statistics"], ensure_ascii=False, indent=2))
    print(f"待审核结果：{output_path}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="离线高考试卷整理工具")
    parser.add_argument("--subject", help="学科目录名，例如：语文")
    parser.add_argument("--sample", help="原始文件目录下的一个样本目录名")
    parser.add_argument("--force", action="store_true", help="忽略断点并强制重新解析")
    parser.add_argument("--list-samples", action="store_true", help="列出可用样本")
    parser.add_argument("--list-subjects", action="store_true", help="列出学科目录")
    args = parser.parse_args()
    ensure_default_subjects()
    if args.list_subjects:
        return list_subjects()
    if args.list_samples:
        if not args.subject:
            parser.error("--list-samples 需要同时提供 --subject")
        return list_samples(args.subject)
    if not args.subject or not args.sample:
        parser.error("必须同时提供 --subject 和 --sample")
    try:
        return run(args.subject, args.sample, args.force)
    except Exception as exc:
        paths = ensure_subject_directories(args.subject)
        log_event(
            paths["log"],
            "ERROR",
            "SAMPLE_FAILED",
            subject=args.subject,
            sample=args.sample,
            errorType=type(exc).__name__,
            message=str(exc),
            traceback=traceback.format_exc(),
        )
        print(f"处理失败：{exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
