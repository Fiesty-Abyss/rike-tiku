from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import posixpath
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from zipfile import ZipFile

from lxml import etree
from PIL import Image
from pypdf import PdfReader


W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
R = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"
V = "{urn:schemas-microsoft-com:vml}"
O = "{urn:schemas-microsoft-com:office:office}"

SOURCE_DOC = Path(
    r"E:\BISHE2026\题库\理综\2023年高考真题——理综（新课标卷）Word版含解析.doc"
)
AUXILIARY_PDF = Path(
    r"E:\BISHE2026\题库\理综\2023年新课标卷高考理科综合试题及答案.pdf"
)
OUTPUT_ROOT = Path(r"E:\BISHE2026\题库\理综\测试结果")
PAPER_NAME = "2023年高考真题——理综（新课标卷）"

SAMPLES = {
    "物理": [15, 18, 19, 22, 23, 26],
    "化学": [7, 9, 27, 28, 29, 30],
    "生物": [1, 6, 31, 33, 34, 35],
}

OUTPUT_NAMES = {
    "物理": "2023新课标物理测试.json",
    "化学": "2023新课标化学测试.json",
    "生物": "2023新课标生物测试.json",
}

SUBSCRIPT = str.maketrans("0123456789+-=()", "₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎")
SUPERSCRIPT = str.maketrans("0123456789+-=()", "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾")
OBJECT_MARKER = re.compile(r"〔(?:公式|图片)对象 ([FI])(\d{3})〕")

# 旧版Word中有少量普通文字被错误保存成VML/WMF。这里按原始媒体指纹还原，
# 同时仍登记对象序号，保证后续对象与Word页码映射不发生偏移。
GLYPH_REPLACEMENTS = {
    "d4f48f87f653d9710ac6057e5bd0a37a7a3e3c2921317356c36a798b0656380a": "的",
    "f811231da6af1de395810b1c0a1d1abcbd132d7ade55ec3589c3ba997cebd077": "的",
    "19ab67816f9bcff21ef7129385ec891c1a614a510263319b20176309d049e012": "的",
    "3c7986883f200ed3c7e3c4dda02ccfd2482a41934574938196135eaf391f5079": ".",
}

REQUIRED_KEYS = {
    "subject",
    "year",
    "region",
    "paperName",
    "questionNumber",
    "questionType",
    "content",
    "options",
    "questionImages",
    "correctAnswer",
    "standardAnalysis",
    "analysisImages",
    "analysisReviewStatus",
    "knowledgePoints",
    "difficultyLevel",
    "difficultyReason",
    "sourceFile",
    "reviewStatus",
}


@dataclass
class WordObject:
    sequence: int
    kind: str
    preview_part: str | None
    embedding_part: str | None
    prog_id: str | None
    page: int | None
    width: float | None
    height: float | None
    replacement_text: str | None = None

    @property
    def marker(self) -> str:
        if self.kind == "glyph":
            return self.replacement_text or ""
        prefix = "F" if self.kind == "formula" else "I"
        label = "公式" if self.kind == "formula" else "图片"
        return f"〔{label}对象 {prefix}{self.sequence:03d}〕"


class WordStructureParser:
    def __init__(self, docx_path: Path, word_info: dict[str, Any]) -> None:
        self.docx_path = docx_path
        self.word_info = word_info
        self.objects: list[WordObject] = []
        self._object_pages = word_info["objects"]
        self._zip = ZipFile(docx_path)
        self._root = etree.fromstring(self._zip.read("word/document.xml"))
        rel_root = etree.fromstring(
            self._zip.read("word/_rels/document.xml.rels")
        )
        self._relationships = {
            relation.get("Id"): relation.get("Target") for relation in rel_root
        }

    def close(self) -> None:
        self._zip.close()

    def _part_for_target(self, target: str | None) -> str | None:
        if not target:
            return None
        return posixpath.normpath(posixpath.join("word", target))

    def _register_object(self, element: etree._Element) -> str:
        sequence = len(self.objects) + 1
        if element.tag == W + "object":
            kind = "formula"
            ole = element.find(".//" + O + "OLEObject")
            image_data = element.find(".//" + V + "imagedata")
            preview_rid = image_data.get(R + "id") if image_data is not None else None
            embed_rid = ole.get(R + "id") if ole is not None else None
            prog_id = ole.get("ProgID") if ole is not None else None
        else:
            kind = "image"
            image_data = element.find(".//" + V + "imagedata")
            preview_rid = image_data.get(R + "id") if image_data is not None else None
            embed_rid = None
            prog_id = None

        preview_part = self._part_for_target(
            self._relationships.get(preview_rid)
        )
        replacement_text = None
        if kind == "image" and preview_part:
            digest = hashlib.sha256(self._zip.read(preview_part)).hexdigest()
            replacement_text = GLYPH_REPLACEMENTS.get(digest)
            if replacement_text is not None:
                kind = "glyph"

        page_info = (
            self._object_pages[sequence - 1]
            if sequence <= len(self._object_pages)
            else {}
        )
        item = WordObject(
            sequence=sequence,
            kind=kind,
            preview_part=preview_part,
            embedding_part=self._part_for_target(
                self._relationships.get(embed_rid)
            ),
            prog_id=prog_id,
            page=page_info.get("page"),
            width=page_info.get("width"),
            height=page_info.get("height"),
            replacement_text=replacement_text,
        )
        self.objects.append(item)
        return item.marker

    def _render_run(self, run: etree._Element) -> str:
        vertical = None
        run_properties = run.find(W + "rPr")
        if run_properties is not None:
            vert = run_properties.find(W + "vertAlign")
            if vert is not None:
                vertical = vert.get(W + "val")

        output: list[str] = []
        for child in run:
            if child.tag == W + "rPr":
                continue
            if child.tag == W + "t":
                text = child.text or ""
                if vertical == "subscript":
                    text = text.translate(SUBSCRIPT)
                elif vertical == "superscript":
                    text = text.translate(SUPERSCRIPT)
                output.append(text)
            elif child.tag == W + "tab":
                output.append("\t")
            elif child.tag == W + "br":
                output.append("\n")
            elif child.tag == W + "object":
                output.append(self._register_object(child))
            elif child.tag == W + "pict":
                output.append(self._register_object(child))
            else:
                output.append(self._render_inline_container(child))
        return "".join(output)

    def _render_inline_container(self, element: etree._Element) -> str:
        output: list[str] = []
        for child in element:
            if child.tag == W + "r":
                output.append(self._render_run(child))
            elif child.tag == W + "object":
                output.append(self._register_object(child))
            elif child.tag == W + "pict":
                output.append(self._register_object(child))
            elif child.tag == W + "t":
                output.append(child.text or "")
            elif child.tag == W + "tab":
                output.append("\t")
            elif child.tag == W + "br":
                output.append("\n")
            elif child.tag != W + "pPr":
                output.append(self._render_inline_container(child))
        return "".join(output)

    def _render_paragraph(self, paragraph: etree._Element) -> str:
        return self._render_inline_container(paragraph).replace("\xa0", " ")

    def _render_table(self, table: etree._Element) -> str:
        rows: list[str] = ["〔表格开始〕"]
        for row in table.findall(W + "tr"):
            cells: list[str] = []
            for cell in row.findall(W + "tc"):
                blocks: list[str] = []
                for child in cell:
                    if child.tag == W + "p":
                        value = self._render_paragraph(child).strip()
                        if value:
                            blocks.append(value)
                    elif child.tag == W + "tbl":
                        blocks.append(self._render_table(child))
                cells.append(" / ".join(blocks).replace("|", "｜"))
            rows.append("| " + " | ".join(cells) + " |")
        rows.append("〔表格结束〕")
        return "\n".join(rows)

    def parse_body(self) -> str:
        body = self._root.find(W + "body")
        if body is None:
            raise RuntimeError("DOCX 缺少 document/body")
        blocks: list[str] = []
        for child in body:
            if child.tag == W + "p":
                blocks.append(self._render_paragraph(child))
            elif child.tag == W + "tbl":
                blocks.append(self._render_table(child))
        text = "\n".join(blocks)
        text = text.replace("\r", "").replace("\u3000", " ")
        text = re.sub(r"[ \t]+\n", "\n", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text

    def read_part(self, part: str) -> bytes:
        return self._zip.read(part)


def subject_for(number: int) -> str:
    if number <= 6 or number >= 31:
        return "生物"
    if 7 <= number <= 13 or 27 <= number <= 30:
        return "化学"
    return "物理"


def question_type_for(number: int) -> str:
    if number <= 18:
        return "单选题"
    if 19 <= number <= 21:
        return "多选题"
    if number in {22, 23}:
        return "实验填空题"
    return "解答题"


def clean_text(text: str) -> str:
    text = "".join(ch for ch in text if ch in "\n\t" or ord(ch) >= 32)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r" *\n *", "\n", text)
    return text.strip()


def split_question_blocks(text: str) -> dict[int, str]:
    starts: list[tuple[int, int]] = []
    cursor = 0
    for number in range(1, 36):
        pattern = re.compile(rf"(?<!\d){number}[\.．]\s+")
        match = pattern.search(text, cursor)
        if match is None:
            raise RuntimeError(f"未识别到连续题号 {number}")
        starts.append((number, match.start()))
        cursor = match.end()

    blocks: dict[int, str] = {}
    for index, (number, start) in enumerate(starts):
        end = starts[index + 1][1] if index + 1 < len(starts) else len(text)
        blocks[number] = clean_text(text[start:end])
    return blocks


def split_answer_analysis(block: str, number: int) -> tuple[str, str, str]:
    if "【答案】" not in block or "【解析】" not in block:
        raise RuntimeError(f"第 {number} 题缺少答案或解析标记")
    question_part, remainder = block.split("【答案】", 1)
    answer_part, analysis_part = remainder.split("【解析】", 1)
    question_part = re.sub(
        rf"^\s*{number}[\.．]\s*", "", question_part, count=1
    )
    return (
        clean_text(question_part),
        clean_text(answer_part),
        clean_text(analysis_part),
    )


def split_options(question_text: str, number: int) -> tuple[str, list[dict[str, str]] | None]:
    if question_type_for(number) not in {"单选题", "多选题"}:
        return question_text, None
    # 个别旧版Word把选项标点本身保存成极小的WMF图片，例如“C〔图片对象〕”。
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
                "content": clean_text(question_text[match.end() : end]),
            }
        )
    return clean_text(question_text[: matches[0].start()]), options


def marker_sequences(text: str) -> list[int]:
    return [int(match.group(2)) for match in OBJECT_MARKER.finditer(text)]


def save_object_preview(
    parser: WordStructureParser,
    item: WordObject,
    image_dir: Path,
    question_number: int,
    region: str,
) -> tuple[Path, str | None]:
    if not item.preview_part:
        raise RuntimeError(f"对象 {item.sequence} 缺少预览图关系")
    data = parser.read_part(item.preview_part)
    kind_name = "formula" if item.kind == "formula" else "image"
    region_slug = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff_-]+", "_", region).strip("_")
    output_path = image_dir / (
        f"q{question_number:02d}_{region_slug}_{kind_name}_{item.sequence:03d}.png"
    )
    try:
        with Image.open(io.BytesIO(data)) as image:
            if image.format == "WMF":
                image.load(dpi=600)
            else:
                image.load()
            if image.mode not in {"RGB", "RGBA"}:
                image = image.convert("RGBA")
            image.save(output_path, format="PNG", optimize=True)
        return output_path, None
    except Exception as exc:
        suffix = Path(item.preview_part).suffix or ".bin"
        fallback = output_path.with_suffix(suffix)
        fallback.write_bytes(data)
        return fallback, f"对象 {item.sequence} 无法转为 PNG，保留原格式: {exc}"


def build_image_records(
    parser: WordStructureParser,
    sequences: list[int],
    subject: str,
    question_number: int,
    region: str,
    image_dir: Path,
    warnings: list[str],
) -> list[dict[str, str]]:
    records: list[dict[str, str]] = []
    for sequence in sequences:
        item = parser.objects[sequence - 1]
        path, warning = save_object_preview(
            parser, item, image_dir, question_number, region
        )
        if warning:
            warnings.append(warning)
        page = str(item.page) if item.page is not None else "页码待审核"
        if item.kind == "formula":
            description = (
                f"Word第{page}页，第{question_number}题{region}中的旧式"
                f" {item.prog_id or 'OLE'} 公式对象预览；需人工核对文本化"
            )
        else:
            description = (
                f"Word第{page}页，第{question_number}题{region}中的图片对象"
            )
        records.append(
            {
                "imagePath": str(path.resolve()),
                "sourcePage": page,
                "description": description,
            }
        )
    return records


def validate_item(item: dict[str, Any]) -> None:
    if set(item) != REQUIRED_KEYS:
        missing = sorted(REQUIRED_KEYS - set(item))
        extra = sorted(set(item) - REQUIRED_KEYS)
        raise RuntimeError(f"输出字段不符合现有模型: missing={missing}, extra={extra}")
    if item["subject"] not in {"物理", "化学", "生物"}:
        raise RuntimeError("subject 非法")
    if item["questionType"] not in {
        "单选题",
        "多选题",
        "填空题",
        "实验填空题",
        "解答题",
        None,
    }:
        raise RuntimeError("questionType 非法")
    if item["analysisReviewStatus"] not in {"待审核", "审核通过", "退回修改"}:
        raise RuntimeError("analysisReviewStatus 非法")
    if item["reviewStatus"] not in {"待审核", "图片缺失", "审核通过", "退回修改"}:
        raise RuntimeError("reviewStatus 非法")
    if not item["content"] or not item["correctAnswer"] or not item["standardAnalysis"]:
        raise RuntimeError(f"第 {item['questionNumber']} 题核心内容为空")
    for collection in (item["questionImages"], item["analysisImages"]):
        for image in collection:
            if set(image) != {"imagePath", "sourcePage", "description"}:
                raise RuntimeError("图片对象字段不符合现有模型")
            if not Path(image["imagePath"]).is_file():
                raise RuntimeError(f"图片文件不存在: {image['imagePath']}")


def normalize_for_compare(text: str) -> str:
    text = OBJECT_MARKER.sub("", text)
    text = re.sub(r"\s+", "", text)
    text = text.replace("．", ".").replace("：", ":")
    return text


def auxiliary_pdf_checks(
    pdf_path: Path,
    questions: dict[int, tuple[str, str, str]],
) -> dict[str, Any]:
    reader = PdfReader(pdf_path)
    page_texts = [(page.extract_text() or "").replace("\x00", "") for page in reader.pages]
    full_text = "\n".join(page_texts)
    normalized_pdf = normalize_for_compare(full_text)

    question_matches: dict[str, bool] = {}
    for number, (question_text, _, _) in questions.items():
        probe = normalize_for_compare(question_text)
        probe = re.sub(r"〔[^〕]+〕", "", probe)
        probe = probe[:30]
        question_matches[str(number)] = bool(probe and probe in normalized_pdf)

    answer_text = "\n".join(page_texts[20:])
    choice_answers: dict[int, str] = {}
    for match in re.finditer(
        r"(?<!\d)(\d{1,2})\s*[\.．]\s*([A-D]{1,4})(?=\s|$)",
        answer_text,
    ):
        number = int(match.group(1))
        if 1 <= number <= 21:
            choice_answers[number] = match.group(2)

    choice_checks: dict[str, str] = {}
    for number in range(1, 22):
        word_answer = normalize_for_compare(questions[number][1])
        pdf_answer = choice_answers.get(number)
        if pdf_answer is None:
            choice_checks[str(number)] = "AUX_ANSWER_NOT_EXTRACTED"
        elif word_answer == pdf_answer:
            choice_checks[str(number)] = "MATCH"
        else:
            choice_checks[str(number)] = f"MISMATCH:{word_answer}!={pdf_answer}"

    return {
        "pages": len(reader.pages),
        "questionTextMatches": question_matches,
        "choiceAnswerChecks": choice_checks,
        "choiceAnswersMatched": sum(v == "MATCH" for v in choice_checks.values()),
        "choiceAnswersNotExtracted": sum(
            v == "AUX_ANSWER_NOT_EXTRACTED" for v in choice_checks.values()
        ),
        "choiceAnswerMismatches": sum(v.startswith("MISMATCH") for v in choice_checks.values()),
        "nonChoiceValidation": "题号和可提取文本人工/视觉交叉核验；公式在辅助PDF文字层中多为空白",
    }


def run_word_export(
    source_doc: Path,
    converted_docx: Path,
    visual_pdf: Path,
    helper_script: Path,
) -> dict[str, Any]:
    result = subprocess.run(
        [
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(helper_script),
            "-SourceDoc",
            str(source_doc),
            "-ConvertedDocx",
            str(converted_docx),
            "-VisualPdf",
            str(visual_pdf),
        ],
        capture_output=True,
        text=True,
        encoding="utf-8-sig",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(
            "Word COM 结构导出失败:\n" + (result.stderr or result.stdout)
        )
    lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        raise RuntimeError("Word COM 未返回结构统计")
    return json.loads(lines[-1])


def build_question(
    parser: WordStructureParser,
    number: int,
    parts: tuple[str, str, str],
    subject_dir: Path,
    warnings: list[str],
) -> dict[str, Any]:
    question_text, answer_text, analysis_text = parts
    content, options = split_options(question_text, number)
    image_dir = subject_dir / "images"
    image_dir.mkdir(parents=True, exist_ok=True)

    question_object_text = content + "\n" + "\n".join(
        option["content"] for option in (options or [])
    )
    question_sequences = marker_sequences(question_object_text)
    analysis_sequences = marker_sequences(answer_text + "\n" + analysis_text)
    question_images = build_image_records(
        parser,
        question_sequences,
        subject_for(number),
        number,
        "题干",
        image_dir,
        warnings,
    )
    analysis_images = build_image_records(
        parser,
        analysis_sequences,
        subject_for(number),
        number,
        "答案/解析",
        image_dir,
        warnings,
    )

    item = {
        "subject": subject_for(number),
        "year": 2023,
        "region": "全国",
        "paperName": PAPER_NAME,
        "questionNumber": str(number),
        "questionType": question_type_for(number),
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
            "paper": str(SOURCE_DOC),
            "answerAnalysis": str(SOURCE_DOC),
        },
        "reviewStatus": "待审核",
    }
    validate_item(item)
    return item


def object_statistics(
    parser: WordStructureParser,
    output: dict[str, list[dict[str, Any]]],
) -> dict[str, Any]:
    selected: dict[str, dict[str, int]] = {}
    for subject, items in output.items():
        counters = {
            "questionImages": 0,
            "analysisImages": 0,
            "questionFormulaObjects": 0,
            "analysisFormulaObjects": 0,
            "questionPictureObjects": 0,
            "analysisPictureObjects": 0,
        }
        for item in items:
            q_text = item["content"] + "\n" + "\n".join(
                option["content"] for option in (item["options"] or [])
            )
            a_text = item["correctAnswer"] + "\n" + item["standardAnalysis"]
            q_ids = marker_sequences(q_text)
            a_ids = marker_sequences(a_text)
            counters["questionImages"] += len(item["questionImages"])
            counters["analysisImages"] += len(item["analysisImages"])
            counters["questionFormulaObjects"] += sum(
                parser.objects[index - 1].kind == "formula" for index in q_ids
            )
            counters["analysisFormulaObjects"] += sum(
                parser.objects[index - 1].kind == "formula" for index in a_ids
            )
            counters["questionPictureObjects"] += sum(
                parser.objects[index - 1].kind == "image" for index in q_ids
            )
            counters["analysisPictureObjects"] += sum(
                parser.objects[index - 1].kind == "image" for index in a_ids
            )
        selected[subject] = counters
    return {
        "source": {
            "total": len(parser.objects),
            "formulaObjects": sum(obj.kind == "formula" for obj in parser.objects),
            "pictureObjects": sum(obj.kind == "image" for obj in parser.objects),
            "textGlyphObjects": sum(obj.kind == "glyph" for obj in parser.objects),
            "nonOlePictObjects": sum(obj.kind in {"image", "glyph"} for obj in parser.objects),
        },
        "selected": selected,
    }


def ensure_output_is_new(output_root: Path, force: bool) -> None:
    existing = [
        output_root / subject / filename
        for subject, filename in OUTPUT_NAMES.items()
        if (output_root / subject / filename).exists()
    ]
    if existing and not force:
        joined = "\n".join(str(path) for path in existing)
        raise RuntimeError("测试输出已存在；为避免覆盖，请检查后使用 --force:\n" + joined)
    if force:
        for subject in OUTPUT_NAMES:
            image_dir = output_root / subject / "images"
            if image_dir.exists():
                shutil.rmtree(image_dir)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="解析2023新课标理综Word解析版并生成三科单卷测试JSON"
    )
    parser.add_argument("--source-doc", type=Path, default=SOURCE_DOC)
    parser.add_argument("--aux-pdf", type=Path, default=AUXILIARY_PDF)
    parser.add_argument("--output", type=Path, default=OUTPUT_ROOT)
    parser.add_argument("--force", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source_doc = args.source_doc.resolve()
    auxiliary_pdf = args.aux_pdf.resolve()
    output_root = args.output.resolve()
    if not source_doc.is_file() or not auxiliary_pdf.is_file():
        raise FileNotFoundError("主Word或辅助PDF不存在")

    ensure_output_is_new(output_root, args.force)
    output_root.mkdir(parents=True, exist_ok=True)
    work_dir = output_root / ".work"
    work_dir.mkdir(parents=True, exist_ok=True)
    converted_docx = work_dir / "source-converted.docx"
    visual_pdf = work_dir / "word-visual.pdf"
    helper = Path(__file__).with_name("word_export_structure.ps1")

    word_info = run_word_export(source_doc, converted_docx, visual_pdf, helper)
    parser = WordStructureParser(converted_docx, word_info)
    warnings: list[str] = []
    try:
        full_text = parser.parse_body()
        if len(parser.objects) != len(word_info["objects"]):
            raise RuntimeError(
                "OOXML对象数与Word对象模型不一致: "
                f"{len(parser.objects)} != {len(word_info['objects'])}"
            )
        if any(
            obj.kind == "formula" and obj.prog_id != "Equation.DSMT4"
            for obj in parser.objects
        ):
            warnings.append("发现非 Equation.DSMT4 的公式对象")

        blocks = split_question_blocks(full_text)
        parsed_parts = {
            number: split_answer_analysis(block, number)
            for number, block in blocks.items()
        }
        recognized = {
            subject: sum(subject_for(number) == subject for number in blocks)
            for subject in SAMPLES
        }
        if recognized != {"物理": 13, "化学": 11, "生物": 11}:
            raise RuntimeError(f"三科识别数量异常: {recognized}")

        outputs: dict[str, list[dict[str, Any]]] = {}
        for subject, numbers in SAMPLES.items():
            subject_dir = output_root / subject
            subject_dir.mkdir(parents=True, exist_ok=True)
            items = [
                build_question(
                    parser,
                    number,
                    parsed_parts[number],
                    subject_dir,
                    warnings,
                )
                for number in numbers
            ]
            output_path = subject_dir / OUTPUT_NAMES[subject]
            output_path.write_text(
                json.dumps(items, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
            outputs[subject] = items

        pdf_checks = auxiliary_pdf_checks(auxiliary_pdf, parsed_parts)
        statistics = object_statistics(parser, outputs)
        summary = {
            "wordMethod": (
                "Word COM只读打开二进制DOC，保存临时DOCX结构副本；"
                "按OOXML文档流读取段落、表格、VML图片和Equation.DSMT4 OLE预览；"
                "PDF仅用于交叉校验和视觉核验"
            ),
            "word": {
                key: word_info[key]
                for key in (
                    "paragraphs",
                    "tables",
                    "inlineShapes",
                    "shapes",
                    "omaths",
                    "pages",
                    "sections",
                    "words",
                    "characters",
                )
            },
            "recognized": recognized,
            "samples": SAMPLES,
            "objects": statistics,
            "auxiliaryPdf": pdf_checks,
            "warnings": warnings,
            "outputs": {
                subject: str((output_root / subject / OUTPUT_NAMES[subject]).resolve())
                for subject in OUTPUT_NAMES
            },
            "visualPdf": str(visual_pdf.resolve()),
        }
        print(json.dumps(summary, ensure_ascii=False, indent=2))
    finally:
        parser.close()
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise
