from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable


SCRIPT_DIR = Path(__file__).resolve().parent
LEGACY_SCRIPT = SCRIPT_DIR / "word_rizong_single_test.py"
WORD_HELPER = SCRIPT_DIR / "word_export_structure.ps1"
SOURCE_ROOT = Path(r"E:\BISHE2026\题库\理综")
WORK_ROOT = SOURCE_ROOT / "master_work"
QUALITY_REPORT = SOURCE_ROOT / "2023首批母题库质量统计.json"

SUBJECT_OUTPUTS = {
    "物理": (
        Path(r"E:\BISHE2026\题库\物理\母题库"),
        "physics_master.json",
    ),
    "化学": (
        Path(r"E:\BISHE2026\题库\化学\母题库"),
        "chemistry_master.json",
    ),
    "生物": (
        Path(r"E:\BISHE2026\题库\生物\母题库"),
        "biology_master.json",
    ),
}


def load_legacy_module() -> Any:
    spec = importlib.util.spec_from_file_location("word_single", LEGACY_SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"无法加载已验证解析模块: {LEGACY_SCRIPT}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


LEGACY = load_legacy_module()

# 全国甲卷中普通文字/标点被保存成 VML/WMF 的媒体指纹。
# 只还原能由上下文和重复实例确定的字形；其余对象一律保留图片。
LEGACY.GLYPH_REPLACEMENTS.update(
    {
        "37db16463c4a89b2e2ae9e4ab529ef5aff3047e69c86cd34fdef54fca8603f22": ".",
        "e2da3ab0e93b80dfa45573297d27d1a3d4782f77724de5bd03d85fad5e5e0ec7": "的",
        "77100db95a04a453b251583f8107d710331560760fb118a22256e999b8622d7f": "的",
        "0d898300d2fff6d89bd5ab5f6fcb34553378068b0a12357ebafa86ce76236f41": "的",
        "12760e1bb0012dd401a7ddf5b956341fcb6eb7ea5c3e004678bf5c47435f800d": ".",
        "6c62b45780055935534113eb1a32ff2dd10fcb803263a08312129bba6bee126c": "【",
    }
)


@dataclass(frozen=True)
class PaperConfig:
    key: str
    label: str
    paper_name: str
    source_doc: Path
    max_question: int
    subject_for: Callable[[int], str]


def new_standard_subject(number: int) -> str:
    if number <= 6 or number >= 31:
        return "生物"
    if 7 <= number <= 13 or 27 <= number <= 30:
        return "化学"
    return "物理"


def national_a_subject(number: int) -> str:
    if number <= 6 or 29 <= number <= 32 or 37 <= number <= 38:
        return "生物"
    if 7 <= number <= 13 or 26 <= number <= 28 or 35 <= number <= 36:
        return "化学"
    return "物理"


PAPERS = (
    PaperConfig(
        key="2023_new_standard",
        label="2023新课标卷",
        paper_name="2023年高考真题——理综（新课标卷）",
        source_doc=SOURCE_ROOT / "2023年高考真题——理综（新课标卷）Word版含解析.doc",
        max_question=35,
        subject_for=new_standard_subject,
    ),
    PaperConfig(
        key="2023_national_a",
        label="2023全国甲卷",
        paper_name="2023年高考真题——理综（全国甲卷）",
        source_doc=SOURCE_ROOT / "2023年高考真题——理综（全国甲卷）Word版含解析.doc",
        max_question=38,
        subject_for=national_a_subject,
    ),
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def split_question_blocks(text: str, max_question: int) -> dict[int, str]:
    starts: list[tuple[int, int]] = []
    cursor = 0
    for number in range(1, max_question + 1):
        regular = re.compile(rf"(?<!\d){number}[\.．][ \t]+")
        match = regular.search(text, cursor)
        if match is None and number >= 33:
            optional = re.compile(rf"(?<!\d){number}[\.．](?=[\[【])")
            match = optional.search(text, cursor)
        if match is None:
            raise RuntimeError(f"未识别到连续题号 {number}")
        starts.append((number, match.start()))
        cursor = match.end()

    blocks: dict[int, str] = {}
    for index, (number, start) in enumerate(starts):
        end = starts[index + 1][1] if index + 1 < len(starts) else len(text)
        blocks[number] = LEGACY.clean_text(text[start:end])
    return blocks


def question_type(number: int) -> str:
    if number <= 18:
        return "单选题"
    if number <= 21:
        return "多选题"
    if number in {22, 23}:
        return "实验填空题"
    return "解答题"


def split_options(question_text: str, number: int) -> tuple[str, list[dict[str, str]] | None]:
    if number > 21:
        return question_text, None
    matches = list(
        re.finditer(
            r"(?<!\S)([A-D])(?:[\.．]|〔图片对象 I\d{3}〕)\s*",
            question_text,
        )
    )
    if len(matches) < 4:
        raise RuntimeError(f"第 {number} 题未能稳定识别 A-D 四个选项")
    matches = matches[-4:]
    labels = [match.group(1) for match in matches]
    if labels != ["A", "B", "C", "D"]:
        raise RuntimeError(f"第 {number} 题选项顺序异常: {labels}")
    options: list[dict[str, str]] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(question_text)
        options.append(
            {
                "label": match.group(1),
                "content": LEGACY.clean_text(question_text[match.end() : end]),
            }
        )
    return LEGACY.clean_text(question_text[: matches[0].start()]), options


def build_question(
    parser: Any,
    config: PaperConfig,
    number: int,
    parts: tuple[str, str, str],
    warnings: list[str],
) -> dict[str, Any]:
    question_text, answer_text, analysis_text = parts
    content, options = split_options(question_text, number)
    subject = config.subject_for(number)
    subject_dir = SUBJECT_OUTPUTS[subject][0]
    image_dir = subject_dir / "images" / config.key
    image_dir.mkdir(parents=True, exist_ok=True)

    option_text = "\n".join(option["content"] for option in (options or []))
    question_sequences = LEGACY.marker_sequences(content + "\n" + option_text)
    analysis_sequences = LEGACY.marker_sequences(answer_text + "\n" + analysis_text)
    question_images = LEGACY.build_image_records(
        parser,
        question_sequences,
        subject,
        number,
        f"{config.label}_题干",
        image_dir,
        warnings,
    )
    analysis_images = LEGACY.build_image_records(
        parser,
        analysis_sequences,
        subject,
        number,
        f"{config.label}_答案解析",
        image_dir,
        warnings,
    )

    item = {
        "subject": subject,
        "year": 2023,
        "region": "全国",
        "paperName": config.paper_name,
        "questionNumber": str(number),
        "questionType": question_type(number),
        "content": content,
        "options": options,
        "questionImages": question_images,
        "correctAnswer": answer_text,
        "standardAnalysis": analysis_text,
        "analysisImages": analysis_images,
        "analysisReviewStatus": "待审核",
        "knowledgePoints": None,
        "difficultyLevel": None,
        "difficultyReason": None,
        "sourceFile": {
            "paper": str(config.source_doc.resolve()),
            "answerAnalysis": str(config.source_doc.resolve()),
        },
        "reviewStatus": "待审核",
    }
    LEGACY.validate_item(item)
    return item


def object_counts(parser: Any, items: list[dict[str, Any]]) -> dict[str, int]:
    counts = {
        "questionFormulaObjects": 0,
        "analysisFormulaObjects": 0,
        "questionPictureObjects": 0,
        "analysisPictureObjects": 0,
        "questionImages": 0,
        "analysisImages": 0,
    }
    for item in items:
        option_text = "\n".join(option["content"] for option in (item["options"] or []))
        question_ids = LEGACY.marker_sequences(item["content"] + "\n" + option_text)
        analysis_ids = LEGACY.marker_sequences(
            item["correctAnswer"] + "\n" + item["standardAnalysis"]
        )
        counts["questionImages"] += len(item["questionImages"])
        counts["analysisImages"] += len(item["analysisImages"])
        counts["questionFormulaObjects"] += sum(
            parser.objects[index - 1].kind == "formula" for index in question_ids
        )
        counts["analysisFormulaObjects"] += sum(
            parser.objects[index - 1].kind == "formula" for index in analysis_ids
        )
        counts["questionPictureObjects"] += sum(
            parser.objects[index - 1].kind == "image" for index in question_ids
        )
        counts["analysisPictureObjects"] += sum(
            parser.objects[index - 1].kind == "image" for index in analysis_ids
        )
    return counts


def process_paper(config: PaperConfig) -> tuple[dict[str, list[dict[str, Any]]], dict[str, Any]]:
    if not config.source_doc.is_file():
        raise FileNotFoundError(config.source_doc)
    before_hash = sha256(config.source_doc)
    work_dir = WORK_ROOT / config.key
    work_dir.mkdir(parents=True, exist_ok=True)
    converted_docx = work_dir / "source.docx"
    visual_pdf = work_dir / "visual.pdf"
    word_info_path = work_dir / "word_info.json"

    if converted_docx.is_file() and visual_pdf.is_file() and word_info_path.is_file():
        word_info = json.loads(word_info_path.read_text(encoding="utf-8-sig"))
    else:
        word_info = LEGACY.run_word_export(
            config.source_doc,
            converted_docx,
            visual_pdf,
            WORD_HELPER,
        )
        if not converted_docx.is_file() or not visual_pdf.is_file():
            raise RuntimeError(f"{config.label} 的 Word 结构副本或视觉 PDF 未生成")
        word_info_path.write_text(
            json.dumps(word_info, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    parser = LEGACY.WordStructureParser(converted_docx, word_info)
    warnings: list[str] = []
    try:
        full_text = parser.parse_body()
        if len(parser.objects) != len(word_info["objects"]):
            raise RuntimeError(
                f"{config.label} OOXML与Word对象数不一致: "
                f"{len(parser.objects)} != {len(word_info['objects'])}"
            )
        blocks = split_question_blocks(full_text, config.max_question)
        parts = {
            number: LEGACY.split_answer_analysis(block, number)
            for number, block in blocks.items()
        }

        outputs: dict[str, list[dict[str, Any]]] = {"物理": [], "化学": [], "生物": []}
        for number in range(1, config.max_question + 1):
            item = build_question(parser, config, number, parts[number], warnings)
            outputs[item["subject"]].append(item)

        recognized = {subject: len(items) for subject, items in outputs.items()}
        expected = (
            {"物理": 13, "化学": 11, "生物": 11}
            if config.key == "2023_new_standard"
            else {"物理": 14, "化学": 12, "生物": 12}
        )
        if recognized != expected:
            raise RuntimeError(f"{config.label} 三科识别数量异常: {recognized}")

        used_sequences: set[int] = set()
        subject_objects: dict[str, Any] = {}
        for subject, items in outputs.items():
            subject_objects[subject] = object_counts(parser, items)
            for item in items:
                option_text = "\n".join(
                    option["content"] for option in (item["options"] or [])
                )
                used_sequences.update(
                    LEGACY.marker_sequences(item["content"] + "\n" + option_text)
                )
                used_sequences.update(
                    LEGACY.marker_sequences(
                        item["correctAnswer"] + "\n" + item["standardAnalysis"]
                    )
                )

        after_hash = sha256(config.source_doc)
        paper_quality = {
            "paper": config.label,
            "sourceFile": str(config.source_doc.resolve()),
            "sourceSha256Before": before_hash,
            "sourceSha256After": after_hash,
            "sourceUnchanged": before_hash == after_hash,
            "word": {
                "pages": word_info["pages"],
                "paragraphs": word_info["paragraphs"],
                "tables": word_info["tables"],
                "inlineShapes": word_info["inlineShapes"],
                "floatingShapes": word_info["shapes"],
                "objects": len(parser.objects),
                "formulaObjects": sum(obj.kind == "formula" for obj in parser.objects),
                "pictureObjects": sum(obj.kind == "image" for obj in parser.objects),
                "restoredGlyphObjects": sum(obj.kind == "glyph" for obj in parser.objects),
                "usedFormulaOrPictureObjects": len(used_sequences),
                "unusedDecorativeObjects": len(
                    [
                        obj
                        for obj in parser.objects
                        if obj.kind != "glyph" and obj.sequence not in used_sequences
                    ]
                ),
            },
            "recognizedQuestions": recognized,
            "subjectObjects": subject_objects,
            "questionNumberMatched": len(blocks) == config.max_question,
            "answerAnalysisMatched": all(
                question and answer and analysis
                for question, answer, analysis in parts.values()
            ),
            "warnings": sorted(set(warnings)),
            "visualPdf": str(visual_pdf.resolve()),
        }
        return outputs, paper_quality
    finally:
        parser.close()


def validate_master(subject: str, items: list[dict[str, Any]]) -> dict[str, Any]:
    for item in items:
        LEGACY.validate_item(item)
        if item["subject"] != subject:
            raise RuntimeError(f"{subject}母题库混入其他学科")
    pairs = [(item["paperName"], item["questionNumber"]) for item in items]
    if len(pairs) != len(set(pairs)):
        raise RuntimeError(f"{subject}母题库存在同卷重复题号")

    formula_markers = sum(
        len(LEGACY.marker_sequences(
            item["content"]
            + "\n"
            + "\n".join(option["content"] for option in (item["options"] or []))
            + "\n"
            + item["correctAnswer"]
            + "\n"
            + item["standardAnalysis"]
        ))
        for item in items
    )
    image_records = sum(
        len(item["questionImages"]) + len(item["analysisImages"]) for item in items
    )
    return {
        "questions": len(items),
        "byPaper": {
            paper.label: sum(item["paperName"] == paper.paper_name for item in items)
            for paper in PAPERS
        },
        "byType": {
            kind: sum(item["questionType"] == kind for item in items)
            for kind in ["单选题", "多选题", "实验填空题", "解答题"]
        },
        "questionImages": sum(len(item["questionImages"]) for item in items),
        "analysisImages": sum(len(item["analysisImages"]) for item in items),
        "formulaAndPictureMarkers": formula_markers,
        "imageRecords": image_records,
        "missingContent": sum(not item["content"] for item in items),
        "missingAnswer": sum(not item["correctAnswer"] for item in items),
        "missingAnalysis": sum(not item["standardAnalysis"] for item in items),
        "pendingReview": sum(item["reviewStatus"] == "待审核" for item in items),
        "imagePathsValid": all(
            Path(image["imagePath"]).is_file()
            for item in items
            for image in item["questionImages"] + item["analysisImages"]
        ),
    }


def ensure_clean_targets(force: bool) -> None:
    existing = []
    for _, (directory, filename) in SUBJECT_OUTPUTS.items():
        path = directory / filename
        if path.exists():
            existing.append(path)
    if QUALITY_REPORT.exists():
        existing.append(QUALITY_REPORT)
    if existing and not force:
        raise RuntimeError(
            "母题库输出已存在；请核验后使用 --force：\n"
            + "\n".join(str(path) for path in existing)
        )
    if force:
        for directory, filename in SUBJECT_OUTPUTS.values():
            image_dir = directory / "images"
            if image_dir.exists():
                shutil.rmtree(image_dir)
            output = directory / filename
            if output.exists():
                output.unlink()


def main() -> int:
    args_parser = argparse.ArgumentParser(description="生成首批第二代理综母题库")
    args_parser.add_argument("--force", action="store_true")
    args = args_parser.parse_args()
    ensure_clean_targets(args.force)

    combined: dict[str, list[dict[str, Any]]] = {"物理": [], "化学": [], "生物": []}
    papers_quality: list[dict[str, Any]] = []
    for config in PAPERS:
        paper_outputs, paper_quality = process_paper(config)
        for subject, items in paper_outputs.items():
            combined[subject].extend(items)
        papers_quality.append(paper_quality)

    masters_quality: dict[str, Any] = {}
    output_files: dict[str, str] = {}
    for subject, items in combined.items():
        directory, filename = SUBJECT_OUTPUTS[subject]
        directory.mkdir(parents=True, exist_ok=True)
        output_path = directory / filename
        output_path.write_text(
            json.dumps(items, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        masters_quality[subject] = validate_master(subject, items)
        output_files[subject] = str(output_path.resolve())

    report = {
        "batch": [paper.label for paper in PAPERS],
        "wordMethod": (
            "Word COM只读打开二进制DOC并保存临时DOCX结构副本；"
            "按OOXML文档流解析段落、表格、VML图片与Equation.DSMT4 OLE公式预览；"
            "公式无法可靠文本化时保留对象标记和对应图片，不猜测公式内容。"
        ),
        "dataModel": (
            "保持现有数据模型；公式对象通过题干/答案/解析内的〔公式对象 Fxxx〕标记，"
            "并在questionImages或analysisImages中保存预览图片。"
        ),
        "papers": papers_quality,
        "masters": masters_quality,
        "outputs": output_files,
    }
    QUALITY_REPORT.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
