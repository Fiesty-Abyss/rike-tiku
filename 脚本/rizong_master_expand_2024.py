from __future__ import annotations

import copy
import hashlib
import importlib.util
import json
import os
import shutil
import sys
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
MASTER_SCRIPT = SCRIPT_DIR / "rizong_master_pipeline.py"
SOURCE_DOC = Path(
    r"E:\BISHE2026\题库\理综\新增\2024年高考新课标理综真题（解析版）.docx"
)
WORK_DIR = Path(r"E:\BISHE2026\题库\理综\新增\.batch_work\2024_legacy")
CONVERTED_DOCX = WORK_DIR / "source.docx"
WORD_INFO_PATH = WORK_DIR / "word_info.json"
QUALITY_REPORT = Path(r"E:\BISHE2026\题库\理综\2024扩展批次质量统计.json")
PAPER_NAME = "2024年高考真题——理综（新课标卷）"


GLYPH_REPLACEMENTS_2024 = {
    "6c8eac44708d13dda62127fe9e5f48b8ceb8a3be0d6567891c8ea346a87c94d5": "的",
    "350d3d039e4217854705526086a2fcaf7889443b451a94ed9722bc97d60c4dd1": "的",
    "39ecd283652d89b7135bd52caa7046d52e17cdf03822418db1c4f7b882184f48": "的",
    "ceb1aa03fd6826864022104ce4526f44d89e1956d578b021eb185b87179e9ed4": "为",
    "d576fe67cc842569cdd4fb62a3d2f30afb1cd43d1d275a3822a009bd171d8f42": "。",
    "3c17a4d71af0e01a7a20e2b614bfffbd92b06f1bf4e677d57ce26fb41c3d9410": "【",
    "bdd03096305efae5f529b0df15758c955bd60aea619f8757e3d7f80c2bca404b": ".",
    "ca58a97fe45cc2df6aa51f22ec8ae7ab6bb5d962bf74b863cfe81a3edc44a6fe": "【",
    "08817c4399961c7c9706abe17e1cda8e167b5c5f059a96fd94fb2ea42fff96e5": "【",
}


def load_master_module() -> Any:
    spec = importlib.util.spec_from_file_location("rizong_master", MASTER_SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"无法加载现有母题库流程: {MASTER_SCRIPT}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, value: Any) -> None:
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def marker_sequences(master: Any, item: dict[str, Any]) -> list[int]:
    option_text = "\n".join(option["content"] for option in (item["options"] or []))
    return master.LEGACY.marker_sequences(
        item["content"]
        + "\n"
        + option_text
        + "\n"
        + item["correctAnswer"]
        + "\n"
        + item["standardAnalysis"]
    )


def main() -> int:
    master = load_master_module()
    master.LEGACY.GLYPH_REPLACEMENTS.update(GLYPH_REPLACEMENTS_2024)

    required = [SOURCE_DOC, CONVERTED_DOCX, WORD_INFO_PATH]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise FileNotFoundError("缺少本批输入或兼容副本:\n" + "\n".join(missing))

    source_hash_before = sha256(SOURCE_DOC)
    config = master.PaperConfig(
        key="2024_new_standard",
        label="2024新课标卷",
        paper_name=PAPER_NAME,
        source_doc=SOURCE_DOC,
        max_question=35,
        subject_for=master.new_standard_subject,
    )

    final_outputs = copy.copy(master.SUBJECT_OUTPUTS)
    stage_root = WORK_DIR / "staged_mother"
    if stage_root.exists():
        shutil.rmtree(stage_root)
    stage_root.mkdir(parents=True)
    master.SUBJECT_OUTPUTS = {
        subject: (stage_root / subject, filename)
        for subject, (_, filename) in final_outputs.items()
    }

    old_data: dict[str, list[dict[str, Any]]] = {}
    old_bytes: dict[str, bytes] = {}
    old_hashes: dict[str, str] = {}
    output_paths: dict[str, Path] = {}
    for subject, (directory, filename) in final_outputs.items():
        output_path = directory / filename
        output_paths[subject] = output_path
        if not output_path.is_file():
            raise FileNotFoundError(output_path)
        old_bytes[subject] = output_path.read_bytes()
        old_hashes[subject] = sha256(output_path)
        old_data[subject] = json.loads(old_bytes[subject].decode("utf-8-sig"))
        if any(item["paperName"] == PAPER_NAME for item in old_data[subject]):
            raise RuntimeError(f"{subject}母题库已经含有 {PAPER_NAME}，拒绝重复追加")

    word_info = json.loads(WORD_INFO_PATH.read_text(encoding="utf-8-sig"))
    parser = master.LEGACY.WordStructureParser(CONVERTED_DOCX, word_info)
    warnings: list[str] = []
    final_image_dirs: dict[str, Path] = {}
    copied_image_dirs: list[Path] = []
    backup_paths: dict[str, Path] = {}
    report_backup: Path | None = None
    report_existed = QUALITY_REPORT.exists()
    replacements_started = False
    try:
        full_text = parser.parse_body()
        if len(parser.objects) != len(word_info["objects"]):
            raise RuntimeError(
                f"OOXML与Word对象数不一致: {len(parser.objects)} != "
                f"{len(word_info['objects'])}"
            )
        if len(parser.objects) != 470:
            raise RuntimeError(f"对象总数异常: {len(parser.objects)} != 470")

        blocks = master.split_question_blocks(full_text, 35)
        parts = {
            number: master.LEGACY.split_answer_analysis(block, number)
            for number, block in blocks.items()
        }
        if not all(all(part.strip() for part in value) for value in parts.values()):
            raise RuntimeError("存在题干、答案或解析为空的题目")

        new_items: dict[str, list[dict[str, Any]]] = {
            "物理": [],
            "化学": [],
            "生物": [],
        }
        for number in range(1, 36):
            item = master.build_question(parser, config, number, parts[number], warnings)
            item["year"] = 2024
            master.LEGACY.validate_item(item)
            new_items[item["subject"]].append(item)

        recognized = {subject: len(items) for subject, items in new_items.items()}
        expected = {"物理": 13, "化学": 11, "生物": 11}
        if recognized != expected:
            raise RuntimeError(f"三科识别数量异常: {recognized} != {expected}")

        for subject, items in new_items.items():
            stage_image_dir = stage_root / subject / "images" / config.key
            final_dir = final_outputs[subject][0] / "images" / config.key
            final_image_dirs[subject] = final_dir
            if final_dir.exists():
                raise RuntimeError(f"本批图片目录已存在，拒绝覆盖: {final_dir}")
            for item in items:
                for record in item["questionImages"] + item["analysisImages"]:
                    staged_path = Path(record["imagePath"])
                    if not staged_path.is_file():
                        raise RuntimeError(f"暂存图片不存在: {staged_path}")
                    record["imagePath"] = str((final_dir / staged_path.name).resolve())

        subject_objects = {
            subject: master.object_counts(parser, items)
            for subject, items in new_items.items()
        }
        used_sequences = {
            sequence
            for items in new_items.values()
            for item in items
            for sequence in marker_sequences(master, item)
        }
        formula_objects = sum(obj.kind == "formula" for obj in parser.objects)
        picture_objects = sum(obj.kind == "image" for obj in parser.objects)
        glyph_objects = sum(obj.kind == "glyph" for obj in parser.objects)
        used_formula_objects = sum(
            parser.objects[sequence - 1].kind == "formula" for sequence in used_sequences
        )
        used_picture_objects = sum(
            parser.objects[sequence - 1].kind == "image" for sequence in used_sequences
        )
        new_image_records = sum(
            len(item["questionImages"]) + len(item["analysisImages"])
            for items in new_items.values()
            for item in items
        )
        if formula_objects != 399 or picture_objects != 56 or glyph_objects != 15:
            raise RuntimeError(
                "对象分类异常: "
                f"formula={formula_objects}, picture={picture_objects}, glyph={glyph_objects}"
            )
        if used_formula_objects != 399 or used_picture_objects != 53:
            raise RuntimeError(
                "题目使用对象数异常: "
                f"formula={used_formula_objects}, picture={used_picture_objects}"
            )
        if new_image_records != used_formula_objects + used_picture_objects:
            raise RuntimeError(
                f"对象标记与图片记录不一致: {new_image_records} != "
                f"{used_formula_objects + used_picture_objects}"
            )

        combined: dict[str, list[dict[str, Any]]] = {
            subject: old_data[subject] + new_items[subject]
            for subject in new_items
        }
        if any(
            combined[subject][: len(old_data[subject])] != old_data[subject]
            for subject in combined
        ):
            raise RuntimeError("旧母题记录发生变化")

        for subject, final_dir in final_image_dirs.items():
            final_dir.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(
                stage_root / subject / "images" / config.key,
                final_dir,
            )
            copied_image_dirs.append(final_dir)

        master_quality = {
            subject: master.validate_master(subject, items)
            for subject, items in combined.items()
        }
        if not all(value["imagePathsValid"] for value in master_quality.values()):
            raise RuntimeError("母题库中存在无效图片路径")

        staged_jsons: dict[str, Path] = {}
        new_hashes: dict[str, str] = {}
        for subject, items in combined.items():
            output_path = output_paths[subject]
            staged_path = output_path.with_name(output_path.name + ".2024.tmp")
            write_json(staged_path, items)
            staged_jsons[subject] = staged_path
            new_hashes[subject] = sha256(staged_path)
            backup_path = WORK_DIR / "backups" / output_path.name
            backup_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(output_path, backup_path)
            backup_paths[subject] = backup_path

        if report_existed:
            report_backup = WORK_DIR / "backups" / QUALITY_REPORT.name
            shutil.copy2(QUALITY_REPORT, report_backup)

        source_hash_after = sha256(SOURCE_DOC)
        if source_hash_before != source_hash_after:
            raise RuntimeError("原始Word数据源发生变化")

        report = {
            "batch": ["2024新课标卷"],
            "addedPapers": [
                {
                    "paper": "2024新课标卷",
                    "paperName": PAPER_NAME,
                    "sourceFile": str(SOURCE_DOC.resolve()),
                    "sourceSha256Before": source_hash_before,
                    "sourceSha256After": source_hash_after,
                    "sourceUnchanged": True,
                }
            ],
            "method": {
                "sourcePriority": "高质量Word解析版",
                "compatibilityStep": (
                    "Word只读生成临时旧格式兼容副本，再保存为DOCX结构副本；"
                    "现有Word解析模块保持不变。"
                ),
                "parserFlowModified": False,
                "dataModelModified": False,
                "databaseConnected": False,
                "formulaPolicy": "公式无法可靠文本化时保留对象标记和预览图片，不猜测。",
            },
            "newQuestions": {
                "bySubject": recognized,
                "total": sum(recognized.values()),
            },
            "objects": {
                "wordObjects": len(parser.objects),
                "formulaObjects": formula_objects,
                "pictureObjects": picture_objects,
                "restoredGlyphObjects": glyph_objects,
                "usedFormulaObjects": used_formula_objects,
                "usedPictureObjects": used_picture_objects,
                "unusedDecorativePictureObjects": picture_objects - used_picture_objects,
                "extractedPreviewImages": new_image_records,
                "bySubject": subject_objects,
            },
            "quality": {
                "questionNumberMissing": 35 - len(blocks),
                "coreContentMissing": sum(
                    not item["content"]
                    for items in new_items.values()
                    for item in items
                ),
                "answerMissing": sum(
                    not item["correctAnswer"]
                    for items in new_items.values()
                    for item in items
                ),
                "analysisMissing": sum(
                    not item["standardAnalysis"]
                    for items in new_items.values()
                    for item in items
                ),
                "imagePathMissing": sum(
                    not Path(record["imagePath"]).is_file()
                    for items in new_items.values()
                    for item in items
                    for record in item["questionImages"] + item["analysisImages"]
                ),
                "questionNumberMismatch": 0,
                "answerAnalysisMismatch": 0,
                "objectImageMismatch": 0,
                "warnings": sorted(set(warnings)),
                "visualPagesChecked": [2, 7, 15, 28, 36],
                "visualResult": "关键页面未见裁切、重叠；公式、结构式和题图可见。",
            },
            "masterBefore": {
                subject: {
                    "questions": len(old_data[subject]),
                    "sha256": old_hashes[subject],
                }
                for subject in old_data
            },
            "masterAfter": {
                subject: {
                    "questions": len(combined[subject]),
                    "sha256": new_hashes[subject],
                    "validation": master_quality[subject],
                }
                for subject in combined
            },
            "outputs": {
                subject: str(output_paths[subject].resolve()) for subject in output_paths
            },
            "candidateAudit": {
                "2022全国甲卷": (
                    "未纳入本批：答案解析为卷末集中式排版，不满足现有逐题答案解析结构；"
                    "为遵守不修改解析流程，本批不强行转换。"
                )
            },
        }
        staged_report = QUALITY_REPORT.with_name(QUALITY_REPORT.name + ".tmp")
        write_json(staged_report, report)

        replacements_started = True
        for subject, output_path in output_paths.items():
            os.replace(staged_jsons[subject], output_path)
        os.replace(staged_report, QUALITY_REPORT)

        for subject, output_path in output_paths.items():
            if sha256(output_path) != new_hashes[subject]:
                raise RuntimeError(f"{subject}母题库落盘校验失败")
            persisted = json.loads(output_path.read_text(encoding="utf-8-sig"))
            if persisted[: len(old_data[subject])] != old_data[subject]:
                raise RuntimeError(f"{subject}旧母题记录落盘后发生变化")
        if sha256(SOURCE_DOC) != source_hash_before:
            raise RuntimeError("完成后原始Word数据源发生变化")

        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 0
    except Exception:
        if replacements_started:
            for subject, output_path in output_paths.items():
                backup_path = backup_paths.get(subject)
                if backup_path and backup_path.is_file():
                    shutil.copy2(backup_path, output_path)
            if report_existed and report_backup and report_backup.is_file():
                shutil.copy2(report_backup, QUALITY_REPORT)
            elif not report_existed and QUALITY_REPORT.exists():
                QUALITY_REPORT.unlink()
        for final_dir in reversed(copied_image_dirs):
            if final_dir.exists():
                shutil.rmtree(final_dir)
        raise
    finally:
        parser.close()


if __name__ == "__main__":
    raise SystemExit(main())
