from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ALLOWED_LICENSES = {
    "PUBLIC_DOMAIN",
    "OPEN_LICENSE",
    "AUTHORIZED",
    "USER_OWNED",
    "COPYRIGHT_UNKNOWN",
}


@dataclass(frozen=True)
class LocalSample:
    name: str
    directory: Path
    metadata: dict[str, Any]
    paper_path: Path
    answer_path: Path | None
    file_hashes: dict[str, str]

    @property
    def bundle_hash(self) -> str:
        digest = hashlib.sha256()
        for name, value in sorted(self.file_hashes.items()):
            digest.update(name.encode("utf-8"))
            digest.update(value.encode("ascii"))
        return digest.hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _find_single(directory: Path, stems: tuple[str, ...], required: bool) -> Path | None:
    supported = {".txt", ".md", ".pdf"}
    matches = [
        path
        for path in directory.iterdir()
        if path.is_file()
        and path.suffix.lower() in supported
        and path.stem.lower() in stems
    ]
    if not matches and required:
        raise ValueError(
            f"{directory} 缺少试卷文件；需要 paper.txt、paper.md 或 paper.pdf"
        )
    if len(matches) > 1:
        names = "、".join(path.name for path in matches)
        raise ValueError(f"{directory} 中同类文件不唯一：{names}")
    return matches[0] if matches else None


def load_sample(raw_root: Path, sample_name: str) -> LocalSample:
    directory = (raw_root / sample_name).resolve()
    if directory.parent != raw_root.resolve():
        raise ValueError("样本名不能跳出原始文件目录")
    if not directory.is_dir():
        raise FileNotFoundError(f"样本目录不存在：{directory}")

    metadata_path = directory / "metadata.json"
    if not metadata_path.is_file():
        raise ValueError(f"缺少来源说明：{metadata_path}")
    metadata = json.loads(metadata_path.read_text(encoding="utf-8-sig"))
    required_fields = (
        "sourceName",
        "sourceUrl",
        "sourceCategory",
        "licenseStatus",
        "rightsEvidence",
    )
    missing = [field for field in required_fields if not metadata.get(field)]
    if missing:
        raise ValueError(f"metadata.json 缺少必填字段：{', '.join(missing)}")
    if metadata["licenseStatus"] not in ALLOWED_LICENSES:
        raise ValueError(
            "licenseStatus 必须是 "
            + "、".join(sorted(ALLOWED_LICENSES))
            + " 之一"
        )
    for source_key in ("answerSource", "analysisSource"):
        source = metadata.get(source_key)
        if source is None:
            continue
        if not isinstance(source, dict):
            raise ValueError(f"{source_key} 必须是对象或 null")
        source_required = (
            "sourceName",
            "sourceUrl",
            "sourceCategory",
            "licenseStatus",
            "rightsEvidence",
        )
        source_missing = [field for field in source_required if not source.get(field)]
        if source_missing:
            raise ValueError(
                f"{source_key} 缺少必填字段：{', '.join(source_missing)}"
            )
        if source["licenseStatus"] not in ALLOWED_LICENSES:
            raise ValueError(
                f"{source_key}.licenseStatus 必须是 "
                + "、".join(sorted(ALLOWED_LICENSES))
                + " 之一"
            )

    paper_path = _find_single(directory, ("paper", "试卷"), required=True)
    answer_path = _find_single(
        directory, ("answer", "answers", "答案", "答案解析"), required=False
    )
    source_files = [metadata_path, paper_path]
    if answer_path:
        source_files.append(answer_path)
    hashes = {path.name: sha256_file(path) for path in source_files}
    return LocalSample(
        name=sample_name,
        directory=directory,
        metadata=metadata,
        paper_path=paper_path,
        answer_path=answer_path,
        file_hashes=hashes,
    )


def read_source_text(path: Path) -> str:
    if path.suffix.lower() in {".txt", ".md"}:
        return path.read_text(encoding="utf-8-sig")
    if path.suffix.lower() == ".pdf":
        try:
            from pypdf import PdfReader
        except ImportError as exc:
            raise RuntimeError(
                "读取 PDF 需要 pypdf；请执行 python -m pip install -r "
                r".\脚本\requirements.txt"
            ) from exc
        reader = PdfReader(str(path))
        pages = [(page.extract_text() or "").strip() for page in reader.pages]
        text = "\n\n".join(page for page in pages if page)
        if not text.strip():
            raise ValueError(f"{path.name} 没有可提取文本层；扫描版 PDF 需人工处理")
        return text
    raise ValueError(f"不支持的文件类型：{path.suffix}")
