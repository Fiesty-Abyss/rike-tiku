# 离线题库数据整理工具

本工具只读取 `E:\BISHE2026\题库\<学科>\原始文件` 中由用户主动放入的文件，不搜索网站、不绕过访问限制，也不写入生产数据库。

## 目录

```text
题库/
├─ 数据模型.schema.json
├─ 语文/
│  ├─ 原始文件/             # 用户提供的原始试卷、答案和来源说明
│  │  └─ <样本名>/
│  │     ├─ metadata.json   # 必需，subject 必须为“语文”
│  │     ├─ paper.txt|md|pdf
│  │     └─ answer.txt|md|pdf
│  ├─ 解析结果/待审核/
│  ├─ 人工审核结果/
│  ├─ 状态/
│  └─ 日志/
├─ 数学/
└─ 其他学科目录……
```

## 最小样本的来源说明

在样本目录创建 `metadata.json`：

```json
{
  "sourceName": "来源名称",
  "sourceUrl": "本地文件或公开来源的原始地址",
  "sourceCategory": "USER_PROVIDED",
  "licenseStatus": "USER_OWNED",
  "rightsEvidence": "用户确认已合法取得并允许用于毕业设计的数据整理",
  "termsCheckedAt": null,
  "robotsCheckedAt": null,
  "accessRestrictions": "LOCAL_FILE_ONLY",
  "subject": "语文",
  "year": 2025,
  "region": "全国",
  "paperType": "高考真题",
  "answerSource": {
    "sourceName": "答案来源名称",
    "sourceUrl": "答案的本地或公开原始地址",
    "sourceCategory": "USER_PROVIDED",
    "licenseStatus": "USER_OWNED",
    "rightsEvidence": "答案的权利或授权说明"
  },
  "analysisSource": {
    "sourceName": "解析来源名称",
    "sourceUrl": "解析的本地或公开原始地址",
    "sourceCategory": "USER_PROVIDED",
    "licenseStatus": "USER_OWNED",
    "rightsEvidence": "解析的权利或授权说明"
  }
}
```

允许的明确权利状态为 `PUBLIC_DOMAIN`、`OPEN_LICENSE`、`AUTHORIZED`、`USER_OWNED`。不明确时必须填写 `COPYRIGHT_UNKNOWN`；程序仍可生成隔离的待审核结果，但会标记为禁止导入。

`sourceName` 等顶层字段描述题干来源；`answerSource` 和 `analysisSource` 分别描述答案、解析来源。即使三者相同也应分别明确填写，程序不会擅自认定它们同源。没有答案或解析时，对应来源可为 `null`。

本地文件无需检查网站 robots.txt；此时将 `sourceUrl` 写为 `local://...`，并将 `termsCheckedAt`、`robotsCheckedAt` 保持为 `null`。如果原始文件来自网站，应在元数据中填写真实地址和检查日期，并把条款/授权证据一并保存在样本目录。

## 文本格式

第一版优先处理可复制文本的 `.txt` 或 `.md`。题号需要独占题目开头，例如：

```text
一、选择题
1. 题干
A. 选项一
B. 选项二

2. 题干
```

答案文件建议写成：

```text
1. A
解析：解析正文

2. 答案正文
解析：解析正文
```

PDF 仅支持包含文本层的文件，需要安装 `pypdf`。扫描版 PDF 不做 OCR，也不抓取渲染图片，应进入人工处理清单。

## 运行

在 `E:\BISHE2026` 中执行：

```powershell
python .\脚本\main.py --subject "语文" --sample "<样本目录名>"
python .\脚本\main.py --subject "语文" --sample "<样本目录名>" --force
python .\脚本\main.py --subject "语文" --list-samples
python .\脚本\main.py --list-subjects
```

正常输出写入 `题库\<学科>\解析结果\待审核\<样本名>.json`。再次运行相同且未改变的样本时，程序使用状态文件跳过，实现简单断点续传。`--force` 可强制重跑。程序要求 `metadata.json` 的 `subject` 与学科目录一致，避免错放或擅自推断。每个学科单独维护内容哈希索引，用于发现重复原始文件、同卷重复题和跨样本重复题；只标记，不自动删除。
