# 题库与内容说明（CONTENT NOTICE）

RIKE 是一个学习辅助**软件**。仓库中的源代码按 [MIT License](LICENSE) 开源；题目、解析、知识卡片等**内容**的来源与使用边界如下。

## 1. 仓库内随附的内容

| 内容 | 位置 | 来源 | 说明 |
|---|---|---|---|
| 演示题库（三科各 120 道客观题） | `rike-tiku-backend/src/main/java/com/neu/riketiku/demo/` | 项目自编 | 按高中物理 / 化学 / 生物课程知识点原创编写，未复制教材、教辅或网络题目原文；只在显式执行演示初始化时写入 `rike_tiku_demo` 演示库 |
| 专题单元主观题（15 单元 / 45 道） | `docs/content/topic-units.v2.json`、`DemoTopicQuestionBank` | 项目自编 | 原创综合题，用于专题学习与组卷演示 |
| 高频考点知识卡片 | `docs/content/high-frequency-points.v*.json` | 项目自编 | 依据课程标准知识点自行归纳；不含真题原文、题号或统计次数，详见 [来源说明](docs/content/HIGH_FREQUENCY_POINTS_SOURCE.md) |
| Excel 导入模板与演示文件 | `docs/templates/`、`docs/demo-import/` | 项目自编 | 示例题均为原创，示例学生 / 教师姓名均为虚构 |
| 迁移样例题（3 道） | `db/migration/V3__insert_three_subject_question_samples.sql` | 2023 年普通高等学校招生全国统一考试（新课标卷）理科综合公开试题 | 仅作为数据结构样例，状态为 `PENDING`（不会发布给学生），权利状态标记为 `COPYRIGHT_UNKNOWN`；版权归原权利人所有 |

## 2. 不随仓库分发的内容

- 第三方试卷原件（Word / PDF）、由其拆分整理出的母题库、图片与候选工作簿**不再随仓库分发**。如需使用自有题库，请放入本地 `question-bank/<学科>/`（已被 `.gitignore` 忽略），并遵循 [question-bank/README.md](question-bank/README.md) 中的来源元数据与权利状态要求。
- 通过管理员界面导入的任何题目都会先进入 `PENDING`，需人工审核来源与权利后才能发布。

## 3. AI 生成内容

AI 答疑、解释与变式候选仅作为学习辅助：正式判分与 STANDARD 标准解析始终来自人工审核过的题库数据；AI 生成的候选题必须经教师 / 管理员审核后才会进入题库。

## 4. 侵权处理

我们尊重知识产权。如果你认为本仓库中的任何内容侵犯了你的合法权益，请通过 GitHub Issue（可选择不公开细节）或仓库所有者主页公开的联系方式告知，并附上权属说明与具体位置。**如有侵权，请联系删除**，我们会在核实后尽快移除相关内容。
