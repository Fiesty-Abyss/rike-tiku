from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

import pdfplumber
from pypdf import PdfReader


ALLOWED_SUBJECTS = {"物理", "化学", "生物"}
ANSWER_MARK = "【答案】"
ANALYSIS_MARKS = ("【详解】", "【解析】", "【分析】")
QUESTION_START = re.compile(
    r"(?m)^[ \t]*(\d{1,3})[.．、]\s*(?=[\u4e00-\u9fff“（(]|20\d{2}年)"
)
OPTION_START = re.compile(r"(?<![A-Za-z])([A-E])[.．、]\s*")
WATERMARK_LINES = (
    re.compile(r"关注公众号【AIgaokao】[^\n]*"),
)

# 只包含已经对照 PDF 页面人工核验过的最小验证样本修订。
# 后续试卷不得照搬这些修订，仍需逐卷审核。
VERIFIED_SAMPLE_OVERRIDES: dict[str, dict[str, dict[str, str]]] = {
    "2021年北京市高考物理试卷": {
        "4": {
            "answer": "B",
            "analysis": r"温度是分子平均动能的标志。$100\,^\circ\mathrm{C}$ 水蒸气温度更高，分子平均动能和热运动剧烈程度均更大；“所有分子速率都更大”不成立。相同质量的热水变成水蒸气需要吸热，因此热水内能较小，选 B。",
        },
        "6": {
            "answer": "D",
            "analysis": r"由 $a=GM/r^2$ 可知近火点加速度较大；由开普勒第二定律可知近火点速度较大；探测器在同一椭圆轨道上运行时机械能不变。近火点速度高于该处圆轨道速度，减速可转入圆轨道，选 D。",
        },
        "8": {
            "answer": "B",
            "analysis": r"汽车定速且各路段坡度不变。上坡段牵引力为 $F_{\mathrm{up}}=f+mg\sin\theta$，功率大于水平路段且保持不变；下坡段牵引力为 $F_{\mathrm{down}}=f-mg\sin\theta$，功率小于水平路段且保持不变，选 B。",
        },
        "10": {
            "answer": "D",
            "analysis": r"圆盘转动时，静摩擦力指向圆心并提供向心力；转一周后物体动量不变，合冲量为零。圆盘骤停后物体沿切线方向滑动，摩擦力使其最终停止，冲量大小等于初动量 $I=|\Delta p|=m\omega r$，选 D。",
        },
        "11": {
            "answer": "D",
            "analysis": "表针晃动时线圈切割磁感线，会产生感应电动势。未接导线时回路不闭合，没有感应电流和安培力；接上导线后形成闭合回路，感应电流受到的安培力阻碍线圈运动，使表针迅速减弱，选D。",
        },
        "13": {
            "answer": "A",
            "analysis": r"弹簧原长对应 $20\,\mathrm{cm}$；静止在 $40\,\mathrm{cm}$ 时，伸长 $20\,\mathrm{cm}$，且 $k(0.20\,\mathrm{m})=mg$。指针在 $30\,\mathrm{cm}$ 时弹力为 $0.5mg$，取向上为正，有 $a=(0.5mg-mg)/m=-0.5g$。加速度与刻度呈线性关系，故选 A。",
        },
        "14": {
            "answer": "D",
            "analysis": r"同步辐射来自高速电子在磁场中的加速运动，机理不同于氢原子能级跃迁。同步辐射光子能量可超过氢原子电离能，且波长范围覆盖蛋白质尺度，可用于衍射。电子每圈损失能量约占 $10^{-5}$，能量不会明显减小，选 D。",
        },
        "15": {
            "answer": "（1）31.4 mm；（2）0.44 m/s；（3）必须满足m≪M，使细绳拉力T近似等于桶和砂的重力mg",
            "analysis": (
                r"（1）根据游标卡尺主尺和游标读数，金属管内径为 $31.4\,\mathrm{mm}$。"
                "\n\n"
                r"（2）相邻计数点时间间隔为 $0.10\,\mathrm{s}$，用中间时刻速度公式 $v_C=(x_D-x_B)/(2T)$，得 $v_C=0.44\,\mathrm{m/s}$。"
                "\n\n"
                r"（3）设小车质量为 $M$、桶和砂质量为 $m$，系统加速度 $a=mg/(M+m)$，细绳拉力 $T=Ma=Mmg/(M+m)=mg/(1+m/M)$。只有 $m\ll M$ 时，才有 $T\approx mg$。"
            ),
        },
        "16": {
            "answer": "（1）6 Ω；（2）A、C；（3）按数据点作U-I拟合直线，R=5.80 Ω；（4）见解析",
            "analysis": (
                r"（1）欧姆表使用 $\times 1$ 挡，读数为 $6\,\Omega$。"
                "\n\n"
                r"（2）电源电动势为 $3.0\,\mathrm{V}$，回路最大电流约为 $I_{\max}=3.0/6=0.5\,\mathrm{A}$，电流表选 A；为便于调节并提高测量精度，滑动变阻器选 C。"
                "\n\n"
                r"（3）连接 $U$-$I$ 数据点并作拟合直线，斜率给出 $R=U/I=5.80\,\Omega$。"
                "\n\n"
                r"（4）刚闭合开关时灯丝温度低、电阻小，电流较大；随后灯丝升温、电阻增大，电流逐渐减小；发热与散热平衡后温度和电阻稳定，电流保持不变。"
            ),
        },
        "17": {
            "answer": "（1）0.30 s；（2）2.0 m/s；（3）0.10 J",
            "analysis": (
                r"（1）竖直方向做自由落体运动，由 $h=\frac{1}{2}gt^2$ 得 $t=0.30\,\mathrm{s}$。"
                "\n\n"
                r"（2）碰后水平速度 $v=s/t=1.0\,\mathrm{m/s}$；碰撞过程动量守恒，$mv_0=2mv$，得 $v_0=2.0\,\mathrm{m/s}$。"
                "\n\n"
                r"（3）损失的机械能为 $\Delta E=\frac{1}{2}mv_0^2-\frac{1}{2}(2m)v^2=0.10\,\mathrm{J}$。"
            ),
        },
        "18": {
            "answer": "（1）U=mv²/(2q)；（2）E=vB，方向垂直导体板向下；（3）Eₖ=mv²/2+qvBd",
            "analysis": "（1）粒子经加速器直线加速，由qU=mv²/2得U=mv²/(2q)。（2）粒子在速度选择器中直线运动，电场力与洛伦兹力平衡，qE=qvB，故E=vB，方向垂直导体板向下。（3）另一粒子运动全程中电场力做正功，由功能关系Eₖ=qU+qEd，代入前两问结果得Eₖ=mv²/2+qvBd。",
        },
        "20": {
            "answer": "（1）T=mg+mv²/l₁；（2）a. θ₂>θ₁；b. ΔEₖ≥(5/2)mgl₂-mgl₁(1-cosθ)",
            "analysis": "（1）最低点由牛顿第二定律T-mg=mv²/l₁，故T=mg+mv²/l₁。（2）a. 站起前后速度相同，分别由机械能守恒得mgl₁(1-cosθ₁)=mv²/2、mgl₂(1-cosθ₂)=mv²/2；因l₁>l₂，所以θ₂>θ₁。b. 从最大摆角到最低点的动能为mgl₁(1-cosθ)。以摆长l₂完成竖直圆周运动，最低点动能至少为(5/2)mgl₂，因此ΔEₖ≥(5/2)mgl₂-mgl₁(1-cosθ)。",
        },
    }
}


@dataclass
class SeedQuestion:
    subject: str
    year: int | None
    region: str | None
    paperName: str
    questionNumber: str
    questionType: str | None
    content: str
    options: list[dict[str, str]] | None
    questionImages: list[dict[str, str]]
    correctAnswer: str | None
    standardAnalysis: str | None
    analysisImages: list[dict[str, str]]
    analysisReviewStatus: str
    knowledgePoints: list[str] | None
    difficultyLevel: str | None
    difficultyReason: str | None
    sourceFile: dict[str, str]
    reviewStatus: str


def extract_pdf_text(path: Path) -> str:
    reader = PdfReader(str(path))
    pages = [page.extract_text() or "" for page in reader.pages]
    if not any(page.strip() for page in pages):
        raise ValueError(f"PDF 没有可读取的文本层：{path}")
    return "\n".join(pages)


def clean_raw_text(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = text.replace("\u00a0", " ").replace("\ufeff", "")
    text = (
        text.replace("", "°")
        .replace("´", "×")
        .replace("", "×")
        .replace("®", "→")
    )
    for pattern in WATERMARK_LINES:
        text = pattern.sub("", text)
    return text


def compact_text(text: str) -> str:
    text = clean_raw_text(text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{2,}", "\n", text)
    text = re.sub(
        r"(?<=[\u4e00-\u9fff，。；：！？、（）])\s+"
        r"(?=[\u4e00-\u9fff，。；：！？、（）])",
        "",
        text,
    )
    text = re.sub(r"\s*\n\s*", " ", text)
    return text.strip()


def repair_verified_content(paper_name: str, number: str, content: str) -> str:
    if paper_name != "2021年北京市高考物理试卷":
        return content
    replacements: dict[str, tuple[tuple[str, str], ...]] = {
        "6": (("2.8×102", "2.8×10²"), ("5.9×105", "5.9×10⁵")),
        "14": (
            ("10-5m", "10⁻⁵ m"),
            ("10-11m", "10⁻¹¹ m"),
            ("10-\n1eV", "10⁻¹ eV"),
            ("10- 1eV", "10⁻¹ eV"),
            ("105eV", "10⁵ eV"),
            ("109eV", "10⁹ eV"),
            ("104eV", "10⁴ eV"),
        ),
        "17": (("v0", "v₀"), ("m/s2", "m/s²"), (" ED ", " ΔE ")),
        "18": ((" kE ", " Eₖ "),),
        "15": (("_____ __mm", "________ mm"), ("vC", "v_C")),
        "16": (
            ("“ 1× ”挡", "“×1”挡"),
            ("_ ______ Ω", "________ Ω"),
            ("0. 02 Ω", "0.02 Ω"),
            ("U- I", "U-I"),
        ),
        "20": (
            ("1l", "l₁"),
            ("2l", "l₂"),
            ("1q", "θ₁"),
            ("2q", "θ₂"),
            ("kED", "ΔEₖ"),
        ),
    }
    for old, new in replacements.get(number, ()):
        content = content.replace(old, new)
    if number == "20":
        content = re.sub(r"(?<![A-Za-z])q(?![A-Za-z])", "θ", content)
        content = re.sub(r"通过计算证明\s*2\s*θ₁\s*θ>", "通过计算证明 θ₂>θ₁", content)
    return content


def repair_verified_options(
    paper_name: str, number: str, options: list[dict[str, str]] | None
) -> list[dict[str, str]] | None:
    if paper_name != "2021年北京市高考物理试卷" or not options:
        return options
    for option in options:
        if number == "10":
            option["content"] = option["content"].replace("m rw", "mωr")
        if number == "14":
            option["content"] = option["content"].replace("10-8 m", "10⁻⁸ m")
            option["content"] = option["content"].split("第二部分", 1)[0].strip()
    return options


def split_questions(text: str, keep: str = "last") -> dict[str, str]:
    if keep not in {"first", "last"}:
        raise ValueError("keep 必须是 first 或 last")
    text = clean_raw_text(text)
    matches = list(QUESTION_START.finditer(text))
    result: dict[str, str] = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        number = match.group(1)
        if not 1 <= int(number) <= 100:
            continue
        block = text[match.end() : end].strip()
        if keep == "last" or number not in result:
            result[number] = block
    return result


def locate_question_starts(pdf: pdfplumber.PDF) -> list[dict[str, float | int]]:
    starts: list[dict[str, float | int]] = []
    for page_index, page in enumerate(pdf.pages):
        for word in page.extract_words(use_text_flow=True):
            match = re.fullmatch(r"(\d{1,3})[.．、]", word["text"])
            if not match or float(word["x0"]) > 115:
                continue
            number = int(match.group(1))
            if 1 <= number <= 100:
                starts.append(
                    {
                        "number": number,
                        "pageIndex": page_index,
                        "top": float(word["top"]),
                    }
                )
    starts.sort(key=lambda item: (int(item["pageIndex"]), float(item["top"])))
    unique: list[dict[str, float | int]] = []
    seen: set[int] = set()
    for start in starts:
        number = int(start["number"])
        if number not in seen:
            unique.append(start)
            seen.add(number)
    return unique


def extract_question_images(
    paper_path: Path,
    output_directory: Path,
    subject: str,
    year: int | None,
    region: str | None,
    paper_name: str,
) -> dict[str, list[dict[str, str]]]:
    output_directory.mkdir(parents=True, exist_ok=True)
    extracted: dict[str, list[dict[str, str]]] = {}
    safe_prefix = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "_", paper_name).strip("_")
    with pdfplumber.open(paper_path) as pdf:
        starts = locate_question_starts(pdf)
        for index, start in enumerate(starts):
            number = str(int(start["number"]))
            next_start = starts[index + 1] if index + 1 < len(starts) else None
            first_page = int(start["pageIndex"])
            last_page = int(next_start["pageIndex"]) if next_start else len(pdf.pages) - 1
            records: list[dict[str, str]] = []
            image_index = 0
            for page_index in range(first_page, last_page + 1):
                page = pdf.pages[page_index]
                segment_top = float(start["top"]) if page_index == first_page else 0.0
                segment_bottom = (
                    float(next_start["top"])
                    if next_start and page_index == int(next_start["pageIndex"])
                    else float(page.height)
                )
                for image in page.images:
                    width = float(image["x1"]) - float(image["x0"])
                    height = float(image["bottom"]) - float(image["top"])
                    if width < 24 or height < 24 or width * height < 1200:
                        continue
                    center_y = (float(image["top"]) + float(image["bottom"])) / 2
                    if not (segment_top <= center_y < segment_bottom):
                        continue
                    image_index += 1
                    padding = 7.0
                    bbox = (
                        max(0.0, float(image["x0"]) - padding),
                        max(segment_top, float(image["top"]) - padding),
                        min(float(page.width), float(image["x1"]) + padding),
                        min(segment_bottom, float(image["bottom"]) + padding),
                    )
                    filename = (
                        f"{subject}_{year or '未知年份'}_{region or '未知地区'}_"
                        f"{safe_prefix}_q{int(number):02d}_p{page_index + 1:02d}_{image_index:02d}.png"
                    )
                    image_path = (output_directory / filename).resolve()
                    page.crop(bbox).to_image(resolution=200, antialias=True).save(
                        image_path, format="PNG"
                    )
                    records.append(
                        {
                            "imagePath": str(image_path),
                            "sourcePage": str(page_index + 1),
                            "description": (
                                f"{paper_name}原卷第{page_index + 1}页，"
                                f"第{number}题题目区域内插图"
                            ),
                        }
                    )
            extracted[number] = records
    return extracted


def parse_options(block: str) -> tuple[str, list[dict[str, str]] | None]:
    cut_positions = [
        block.find(marker)
        for marker in (
            ANSWER_MARK,
            "【考点】",
            "【分析】",
            "【解析】",
            "【详解】",
            "【解答】",
            "【点评】",
        )
        if marker in block
    ]
    if cut_positions:
        block = block[: min(cut_positions)]
    matches = list(OPTION_START.finditer(block))
    labels = [match.group(1) for match in matches]
    if labels[:4] != ["A", "B", "C", "D"]:
        return compact_text(block), None
    option_count = 5 if len(labels) >= 5 and labels[4] == "E" else 4
    content = compact_text(block[: matches[0].start()])
    options: list[dict[str, str]] = []
    for index, match in enumerate(matches[:option_count]):
        end = (
            matches[index + 1].start()
            if index + 1 < option_count
            else len(block)
        )
        options.append(
            {"label": match.group(1), "content": compact_text(block[match.end() : end])}
        )
    return content, options


def answer_and_analysis(block: str) -> tuple[str | None, str | None]:
    if ANSWER_MARK in block:
        after_answer = block.split(ANSWER_MARK, 1)[1]
        marker_positions = [
            (after_answer.find(marker), marker)
            for marker in ANALYSIS_MARKS
            if marker in after_answer
        ]
        if not marker_positions:
            return compact_text(after_answer) or None, None
        first_position, _ = min(marker_positions, key=lambda item: item[0])
        answer = compact_text(after_answer[:first_position]) or None
        detail_position = after_answer.find("【详解】")
        if detail_position >= 0:
            analysis = after_answer[detail_position + len("【详解】") :]
        else:
            analysis_start, marker = min(marker_positions, key=lambda item: item[0])
            analysis = after_answer[analysis_start + len(marker) :]
        analysis = analysis.split("【点评】", 1)[0]
        return answer, compact_text(analysis) or None

    legacy_starts = [
        block.find(marker)
        for marker in ("【分析】", "【解析】", "【详解】", "【解答】")
        if marker in block
    ]
    if not legacy_starts:
        return None, None
    detail = block[min(legacy_starts) :].split("【点评】", 1)[0]
    detail = re.sub(r"【(?:分析|解析|详解|解答)】", " ", detail)
    analysis = compact_text(detail) or None
    solution_at = block.find("【解答】")
    solution = block[solution_at + len("【解答】") :] if solution_at >= 0 else detail
    solution = solution.split("【点评】", 1)[0]
    answer: str | None = None
    choice_match = re.search(r"故选[：:]?\s*([A-E]{1,5})", solution)
    if choice_match:
        answer = choice_match.group(1)
    if answer is None:
        fill_match = re.search(
            r"故答案为[：:]?\s*(.+?)(?=【|。\s|答[：:]|$)",
            solution,
            flags=re.S,
        )
        if fill_match:
            answer = compact_text(fill_match.group(1)) or None
    if answer is None:
        final_match = re.search(r"答[：:]\s*(.+)$", solution, flags=re.S)
        if final_match:
            answer = compact_text(final_match.group(1)) or None
    return answer, analysis


def question_type(
    subject: str,
    number: int,
    content: str,
    options: Any,
    answer: str | None = None,
) -> str | None:
    if options:
        answer_text = (answer or "").strip()
        if re.fullmatch(r"[A-E]{1,5}", answer_text):
            return "多选题" if len(answer_text) > 1 else "单选题"
        if not re.search(r"[（(]\d+[）)]", content):
            return "单选题"
    if "实验" in content and ("____" in content or "______" in content):
        return "实验填空题"
    if re.search(r"[（(]\d+[）)]", content) and not options:
        return "解答题"
    if "____" in content or "______" in content:
        return "填空题"
    return None


def physics_knowledge(content: str, analysis: str | None) -> list[str] | None:
    text = f"{content} {analysis or ''}"
    rules = (
        (("核反应", "α粒子", "同步辐射", "氢原子", "电离能"), ["近代物理", "原子与原子核"]),
        (("电阻率", "欧姆表", "电压表", "U-I"), ["电磁学", "恒定电流与电学实验"]),
        (("分子", "内能", "热运动", "水蒸汽", "温度"), ["热学", "分子动理论与内能"]),
        (("折射", "光束", "干涉", "色散", "衍射"), ["光学", "光的传播与波动性"]),
        (("自感", "电流表", "感应电动势", "安培力"), ["电磁学", "电磁感应"]),
        (("磁场", "洛伦兹力", "速度选择器", "带电粒子"), ["电磁学", "带电粒子在电磁场中的运动"]),
        (("电场", "电势", "点电荷"), ["电磁学", "静电场"]),
        (("交变电流", "有效值"), ["电磁学", "交变电流"]),
        (("纸带", "游标卡尺", "加速度与力"), ["力学", "力学实验"]),
        (("火星", "近火点", "远火点", "开普勒"), ["力学", "万有引力与天体运动"]),
        (("弹簧", "胡克", "加速度测量仪"), ["力学", "牛顿运动定律与弹力"]),
        (("秋千", "单摆", "摆长"), ["力学", "机械能与圆周运动"]),
        (("碰撞", "水平抛出", "机械能"), ["力学", "动量与能量综合"]),
        (("圆盘", "角速度", "冲量"), ["力学", "圆周运动与动量"]),
        (("汽车", "输出功率", "上坡", "下坡"), ["力学", "功和功率"]),
        (("简谐横波", "波形图", "质点"), ["力学", "机械振动与机械波"]),
    )
    for keywords, points in rules:
        if any(keyword in text for keyword in keywords):
            return points
    return None


def chemistry_knowledge(content: str, analysis: str | None) -> list[str] | None:
    text = f"{content} {analysis or ''}"
    rules = (
        (
            (
                "实验装置",
                "实验操作",
                "滴定",
                "分液",
                "萃取",
                "蒸馏",
                "过滤",
                "洗涤",
                "容量瓶",
                "移液管",
                "制备实验",
                "检验气密性",
            ),
            ["实验化学", "化学实验基本操作与物质制备"],
        ),
        (
            (
                "有机物",
                "有机化合物",
                "同分异构体",
                "官能团",
                "结构简式",
                "烷烃",
                "烯烃",
                "炔烃",
                "苯",
                "醇",
                "酚",
                "醛",
                "羧酸",
                "酯",
                "聚合反应",
            ),
            ["有机化学", "有机物结构、性质与合成"],
        ),
        (
            (
                "元素周期",
                "周期表",
                "电子式",
                "价电子",
                "化学键",
                "晶体",
                "晶胞",
                "杂化",
                "分子构型",
                "电负性",
            ),
            ["物质结构", "原子结构、化学键与物质结构"],
        ),
        (
            (
                "化学平衡",
                "平衡常数",
                "反应速率",
                "活化能",
                "原电池",
                "电解池",
                "电极反应",
                "电离平衡",
                "水解平衡",
                "沉淀溶解平衡",
                "溶度积",
                "pH",
                "焓变",
                "热化学",
            ),
            ["化学反应原理", "反应热、速率、平衡与电化学"],
        ),
        (
            (
                "物质的量",
                "摩尔质量",
                "阿伏加德罗",
                "离子方程式",
                "氧化还原",
                "氧化数",
                "物质分类",
                "胶体",
                "离子共存",
                "溶液浓度",
            ),
            ["化学基本概念", "物质组成、分类与化学用语"],
        ),
    )
    for keywords, points in rules:
        if any(keyword in text for keyword in keywords):
            return points
    return None


def knowledge_points(subject: str, content: str, analysis: str | None) -> list[str] | None:
    if subject == "物理":
        return physics_knowledge(content, analysis)
    if subject == "化学":
        return chemistry_knowledge(content, analysis)
    return None


def difficulty(
    subject: str,
    qtype: str | None,
    content: str,
    analysis: str | None,
    points: list[str] | None,
) -> tuple[str | None, str | None]:
    if not qtype or not analysis or not points:
        return None, None
    subparts = len(re.findall(r"[（(]\d+[）)]", content))
    if subject == "化学":
        chemistry_markers = sum(
            marker in f"{content} {analysis}"
            for marker in (
                "化学平衡",
                "电极反应",
                "离子方程式",
                "有机合成",
                "同分异构体",
                "实验步骤",
                "反应机理",
                "计算",
            )
        )
        if qtype == "解答题" and (subparts >= 3 or chemistry_markers >= 3):
            return "hard", "包含多个小问，需要综合化学原理、实验或计算完成多步骤推理。"
        if qtype in {"解答题", "实验填空题", "填空题"} or len(analysis) >= 320:
            return "medium", "需要结合反应原理、实验信息或多个判断步骤。"
        if qtype in {"单选题", "多选题"} and len(analysis) <= 240 and chemistry_markers <= 1:
            return "easy", "主要考查单一化学概念、性质或基本规律的直接应用。"
        return "medium", "需要结合题目信息完成两步以上化学判断。"
    combined_markers = sum(
        marker in analysis
        for marker in (
            "动量守恒",
            "动量定理",
            "机械能",
            "功能关系",
            "牛顿第二定律",
            "几何关系",
        )
    )
    if qtype == "解答题" and (
        subparts >= 3
        or combined_markers >= 2
        or "证明" in content
        or "完整的圆周运动" in content
    ):
        return "hard", "包含多个小问，需要综合两个以上规律并进行多步骤推理。"
    if qtype == "单选题" and points[-1] in {
        "万有引力与天体运动",
        "圆周运动与动量",
        "牛顿运动定律与弹力",
        "原子与原子核",
    }:
        return "medium", "需要结合两个相关规律或逐项完成多步判断。"
    if qtype in {"解答题", "实验填空题"} or len(analysis) >= 300:
        return "medium", "需要公式推导、实验数据处理或多个判断步骤。"
    if len(analysis) <= 260 and combined_markers == 0:
        return "easy", "主要考查单一基础概念或规律的直接应用。"
    return "medium", "需要结合题目信息完成两步以上判断。"


def build_questions(
    subject: str,
    paper_path: Path,
    analysis_path: Path,
    year: int | None,
    region: str | None,
    paper_name: str,
    images_directory: Path,
) -> tuple[list[SeedQuestion], list[str]]:
    paper_blocks = split_questions(extract_pdf_text(paper_path), keep="first")
    analysis_blocks = split_questions(extract_pdf_text(analysis_path), keep="last")
    failures: list[str] = []
    questions: list[SeedQuestion] = []
    question_images = extract_question_images(
        paper_path,
        images_directory,
        subject,
        year,
        region,
        paper_name,
    )
    all_numbers = sorted(set(paper_blocks) | set(analysis_blocks), key=int)
    for number_text in all_numbers:
        paper_block = paper_blocks.get(number_text)
        analysis_block = analysis_blocks.get(number_text)
        if paper_block is None or analysis_block is None:
            failures.append(f"第{number_text}题：原卷与解析版题号未能配对")
            continue
        content, options = parse_options(paper_block)
        content = repair_verified_content(paper_name, number_text, content)
        options = repair_verified_options(paper_name, number_text, options)
        answer, analysis = answer_and_analysis(analysis_block)
        override = VERIFIED_SAMPLE_OVERRIDES.get(paper_name, {}).get(number_text)
        if override:
            answer = override.get("answer", answer)
            analysis = override.get("analysis", analysis)
        number = int(number_text)
        qtype = question_type(subject, number, content, options, answer)
        if qtype != "单选题":
            content = re.sub(r"（\s*）$", "", content).strip()
        points = knowledge_points(subject, content, analysis)
        level, reason = difficulty(subject, qtype, content, analysis, points)
        if not content or not answer or not analysis:
            missing = [
                name
                for value, name in (
                    (content, "题干"),
                    (answer, "答案"),
                    (analysis, "解析"),
                )
                if not value
            ]
            failures.append(f"第{number_text}题：缺少{'/'.join(missing)}")
        images = question_images.get(number_text, [])
        review_status = image_review_status(content, images)
        questions.append(
            SeedQuestion(
                subject=subject,
                year=year,
                region=region,
                paperName=paper_name,
                questionNumber=number_text,
                questionType=qtype,
                content=content,
                options=options,
                questionImages=images,
                correctAnswer=answer,
                standardAnalysis=analysis,
                analysisImages=[],
                analysisReviewStatus="待审核",
                knowledgePoints=points,
                difficultyLevel=level,
                difficultyReason=reason,
                sourceFile={
                    "paper": str(paper_path),
                    "answerAnalysis": str(analysis_path),
                },
                reviewStatus=review_status,
            )
        )
    return questions, failures


def image_review_status(content: str, images: list[dict[str, str]]) -> str:
    needs_image = any(
        marker in content
        for marker in ("如图", "图中", "如下图", "下图", "图示", "示意图", "装置图", "流程图")
    )
    return "图片缺失" if needs_image and not images else "待审核"


def quality_score(question: SeedQuestion, paper_name: str) -> tuple[int, int]:
    score = 0
    if question.correctAnswer:
        score += 3
    if question.standardAnalysis:
        score += 3
    if question.questionImages:
        score += 1
    if question.options and len(question.options) == 4:
        score += 2
    if question.questionNumber in VERIFIED_SAMPLE_OVERRIDES.get(paper_name, {}):
        score += 8
    suspicious = " ".join(
        [question.content]
        + [option["content"] for option in (question.options or [])]
    )
    if re.search(r"(?:\d+\s+){3,}\d+", suspicious):
        score -= 4
    return score, -int(question.questionNumber)


def select_questions(
    questions: list[SeedQuestion], target: int, paper_name: str
) -> list[SeedQuestion]:
    eligible = [
        question
        for question in questions
        if question.content
        and question.correctAnswer
        and question.standardAnalysis
        and question.reviewStatus != "图片缺失"
    ]
    choice_target = min(len(eligible), int(target * 0.65 + 0.999))
    experiment_target = min(len(eligible) - choice_target, round(target * 0.20))
    calculation_target = target - choice_target - experiment_target
    buckets = {
        "choice": [q for q in eligible if q.questionType in {"单选题", "多选题"}],
        "experiment": [
            q for q in eligible if q.questionType in {"实验填空题", "填空题"}
        ],
        "calculation": [q for q in eligible if q.questionType == "解答题"],
    }
    for bucket in buckets.values():
        bucket.sort(key=lambda question: quality_score(question, paper_name), reverse=True)
    selected = (
        buckets["choice"][:choice_target]
        + buckets["experiment"][:experiment_target]
        + buckets["calculation"][:calculation_target]
    )
    if len(selected) < target:
        selected_numbers = {question.questionNumber for question in selected}
        remaining = [q for q in eligible if q.questionNumber not in selected_numbers]
        remaining.sort(key=lambda question: quality_score(question, paper_name), reverse=True)
        selected.extend(remaining[: target - len(selected)])
    selected.sort(key=lambda question: int(question.questionNumber))
    return selected[:target]


def prune_unselected_images(
    images_directory: Path,
    selected: list[SeedQuestion],
    subject: str,
    year: int | None,
    region: str | None,
    paper_name: str,
) -> None:
    if not images_directory.is_dir():
        return
    keep = {
        Path(image["imagePath"]).resolve()
        for question in selected
        for image in question.questionImages
    }
    safe_prefix = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", "_", paper_name).strip("_")
    filename_prefix = (
        f"{subject}_{year or '未知年份'}_{region or '未知地区'}_{safe_prefix}_q"
    )
    for image_path in images_directory.glob(f"{filename_prefix}*.png"):
        resolved = image_path.resolve()
        if resolved.parent == images_directory.resolve() and resolved not in keep:
            image_path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="离线高考 PDF 精选种子题库整理")
    parser.add_argument("--subject", required=True, choices=sorted(ALLOWED_SUBJECTS))
    parser.add_argument("--paper", required=True, type=Path, help="原卷版 PDF")
    parser.add_argument("--analysis", required=True, type=Path, help="含答案解析版 PDF")
    parser.add_argument("--year", type=int)
    parser.add_argument("--region")
    parser.add_argument("--paper-name", required=True)
    parser.add_argument("--include-numbers", help="只输出指定题号，如 4,6,8")
    parser.add_argument("--target", type=int, default=10, help="精选输出数量")
    parser.add_argument("--images-dir", required=True, type=Path)
    parser.add_argument("--output-json", required=True, type=Path)
    args = parser.parse_args()

    for path in (args.paper, args.analysis):
        if path.suffix.lower() != ".pdf" or not path.is_file():
            raise FileNotFoundError(f"PDF 不存在：{path}")
    questions, failures = build_questions(
        args.subject,
        args.paper.resolve(),
        args.analysis.resolve(),
        args.year,
        args.region,
        args.paper_name,
        args.images_dir,
    )
    parsed_question_count = len(questions)
    image_missing_count = sum(q.reviewStatus == "图片缺失" for q in questions)
    eligible_candidate_count = sum(
        bool(q.content and q.correctAnswer and q.standardAnalysis)
        and q.reviewStatus != "图片缺失"
        for q in questions
    )
    if args.include_numbers:
        requested = [item.strip() for item in args.include_numbers.split(",") if item.strip()]
        by_number = {question.questionNumber: question for question in questions}
        missing_requested = [number for number in requested if number not in by_number]
        if missing_requested:
            raise ValueError(f"指定题号未解析：{', '.join(missing_requested)}")
        questions = [by_number[number] for number in requested]
    else:
        questions = select_questions(questions, args.target, args.paper_name)
    prune_unselected_images(
        args.images_dir,
        questions,
        args.subject,
        args.year,
        args.region,
        args.paper_name,
    )

    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(
        json.dumps([asdict(question) for question in questions], ensure_ascii=False, indent=2)
        + "\n",
        encoding="utf-8",
    )
    print(
        json.dumps(
            {
                "paperQuestions": len(split_questions(extract_pdf_text(args.paper))),
                "analysisQuestions": len(split_questions(extract_pdf_text(args.analysis))),
                "parsedQuestions": parsed_question_count,
                "eligibleCandidates": eligible_candidate_count,
                "imageMissingCandidates": image_missing_count,
                "outputQuestions": len(questions),
                "outputImages": sum(len(q.questionImages) for q in questions),
                "failures": failures,
                "output": str(args.output_json),
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0 if not failures else 2


if __name__ == "__main__":
    raise SystemExit(main())
