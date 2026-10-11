# question-bank：题库源数据目录

本目录是后端“题目 Excel 导入”的**受控来源根目录**（配置项 `rike.tiku.question-import.source-root`，默认 `../question-bank`），也是离线整理工具 [`tools/question-pipeline`](../tools/question-pipeline/README.md) 的工作目录。

仓库只提交目录说明、数据模型和元数据示例，**不分发任何第三方试卷原件或由其整理出的题目数据**。版权与来源说明见 [CONTENT_NOTICE.md](../CONTENT_NOTICE.md)。

## 目录约定

```text
question-bank/
├─ README.md                # 本说明（导入模板“来源文件”列默认引用它）
├─ PARSING.md               # 离线解析规则（导入模板“来源文件”列默认引用它）
├─ question-schema.json     # 离线整理输出的数据模型
├─ metadata.example.json    # 样本来源元数据示例
└─ <学科>/                  # 本地数据，已被 .gitignore 忽略，不会提交
   ├─ 原始文件/<样本名>/     # 自行合法取得的试卷、答案与 metadata.json
   ├─ 母题库/images|attachments/   # 导入时可被引用的图片 / 附件
   ├─ 解析结果/待审核/
   ├─ 人工审核结果/
   ├─ 状态/
   └─ 日志/
```

## 使用要点

1. 只放入你**有权使用**的材料（自编、获得授权、开放许可或公有领域），并在 `metadata.json` 中如实填写题干、答案、解析各自的来源与权利状态。
2. 权利状态不明确时填写 `COPYRIGHT_UNKNOWN`，整理结果会被标记为禁止导入。
3. 通过管理员界面导入的题目一律进入 `PENDING`，需人工审核后才会发布给学生。
4. 若需要运行依赖本地真题源数据集的导入测试，把整理好的 `待审核_清洗版.xlsx` 放到 `question-bank/理综/测试结果/{物理,化学,生物}/`，相关测试会自动启用；否则自动跳过。
