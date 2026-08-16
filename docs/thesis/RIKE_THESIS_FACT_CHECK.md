# RIKE 论文事实核对表

更新时间：2026-08-16

| 论文陈述 | 可核验依据 | 状态 |
|---|---|---|
| 系统采用前后端分离模块化单体 | `DESIGN.md`、代码模块和 Maven/Vite 工程 | 已核验 |
| Java 25、Spring Boot 4.1、Vue 3、MySQL 8.4 | POM、package.json、本机命令输出 | 已核验 |
| Flyway V1–V29、50 张业务表 | 迁移目录、随机测试库、`rike_tiku_demo`、V29 纯结构快照 | 已核验 |
| 既有 V1–V25 未修改 | Git blob hash 与迁移校验 | 已核验 |
| Demo 题量 378 | DemoDataService validate 输出 | 已核验 |
| 答疑最多 10 轮 | V15、`StudentAiService.MAX_ROUNDS`、集成测试 | 已核验 |
| 标准/深度思考映射 | DeepSeek Provider request 测试 | 已核验 |
| 搜索使用智谱官方结构化 Web Search | Search Client 与 Mock HTTP 测试 | 已核验；真实 smoke 取决于 Key |
| 学生变式支持单选、多选、填空 | Parser、共享判分器、UI 与接口 | 已核验 |
| 候选题审核前不发布 | AI generation 集成测试 | 已核验 |
| 密码恢复不泄露账号存在性 | 统一响应与集成测试 | 已核验 |
| 教师组卷受任教学科约束 | PaperService 集成测试 | 已核验 |
| AI 科学文本不使用未过滤 v-html | `AiScientificContent` 源码与测试 | 已核验 |
| 本轮点名后端专项 | 39 tests，0 failures，0 errors；使用随机临时 schema，包含题目前缀、判分、学生变式、候选 Parser、候选生命周期和专题单元 | 已执行 |
| 本轮学生前端专项 | 9 files、30 tests，0 failures；type-check/build 通过 | 已执行 |
| 后端最终全量 | `mvn clean test`：212 tests，0 failures，0 errors，3 skipped；`mvn -DskipTests package` 通过 | 已执行 |
| 前端最终全量 | 68 files、219 tests；type-check、build、`npm audit --omit=dev` 通过，0 vulnerabilities | 已执行 |
| 机器浏览器 | 正式 `rike_tiku` 4 routes / 19 assertions，0 console/page/failed-request error，0 overflow；独立临时 profile | 已执行；不等同真人验收 |
| 正式库迁移与本轮内容 | 仓库外 1,326,218-byte 备份（SHA-256 `039C9E885007EB79ED317E1A1E5C5A6DCEB7EC2746C0777957E41E60FE65E622`）后，由 Flyway 从实际 V24 正常迁移到 V29；9 用户、389 题/378 PUBLISHED 基线不变；本轮核验 6 单元、18 关系、65 张已发布卡片（60 张结构化来源） | 已核验；无新迁移 |
| V1–V29 checksum 与临时库 | 正式 `flyway_schema_history` 29 行均 `success=1`；V1–V29 迁移文件在本轮无改动；最终回查无 `rike_tiku_` 临时 schema | 已核验 |
| Word/PPT 交付物 | `docs/thesis/deliverables/` 下生成通用 Word 事实稿和 12 页 PPT；OOXML/PPTX 结构、文字、图片和边界检查通过；本机无 Word/PowerPoint/LibreOffice 渲染器 | 已生成；系统视觉渲染待学校环境复核 |
| 真实 DeepSeek/GLM/xAI/Search 本轮 smoke | 没有可安全使用的轮换后凭据，未消费真实 Provider | BLOCKED_EXTERNAL_PROVIDER；历史结果不外推为本轮 PASS |
| 真人用户验收、问卷或学习成效提升 | 无本轮数据 | 禁止声称 |

论文中的“有效”只指指定测试或约束得到验证，不等同于教学效果经真实学生群体实验证明。

## 正式参考文献边界

开题报告与毕业论文正文只能引用 `docs/THESIS_REFERENCES.md` 及
`docs/references/references.bib` 中固定的 22 条白名单文献。扩展调研资料已经物理隔离到
`docs/references/research-only/`，不得作为老师审查版本的正文引用或参考文献。本仓库未包含学校正式开题报告 Word 模板；相关内容仍需待套用学校模板。
