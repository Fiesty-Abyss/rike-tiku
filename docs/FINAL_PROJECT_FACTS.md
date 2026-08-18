# RIKE 最终项目事实包

> 本文件是论文、答辩和后续维护的当前事实入口，不是开发日志。历史时间线中的旧测试数、旧 Flyway 版本或旧表数只能解释当时阶段，不能覆盖本文件。最后一次事实刷新：2026-08-18，PR #35 已完成用户人工验收与最终回归，待 ordinary merge；合并后产品继续冻结。

## 1. 项目身份与运行边界

| 项目 | 事实 |
|---|---|
| 论文题目 | 面向高中物化生的 Spring Boot 大模型题库系统设计与实现 |
| 产品名 | RIKE 理科学习辅助系统 |
| 历史名称 | 集成大模型智能答疑的在线题库实训管理系统（仅用于解释演变） |
| 架构 | 前后端分离 + 模块化单体；不是微服务，不依赖注册中心、MQ、Redis、向量数据库或分布式事务。 |
| 后端 | Java 25、Spring Boot 4.1、Maven、MyBatis-Plus、Spring Security、JWT、Flyway、MySQL 8.4。 |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、GSAP、KaTeX、mhchem。 |
| 正式运行 | Frontend `http://localhost:8080`；Backend `http://localhost:8081`；MySQL schema `rike_tiku`。自动化测试使用随机临时 schema，绝不 reset 正式库。 |

## 2. Git 与阶段

| 里程碑 | 事实 |
|---|---|
| PR #33 | 主体产品完成 PR；已以 ordinary merge 合入 `main`，merge commit `fba1276862fee973129ee8b85c6fc3a1d55b8662`。 |
| PR #34 | post-merge 的专题内容、主观题组卷、V30 附件快照、题型中文化和打印修补 PR；base 为 `fba1276862fee973129ee8b85c6fc3a1d55b8662`，final head 为 `a456ace11df83018d285b04b198448cf0bbe5ba7`，已 ordinary merge。 |
| PR #34 merge | `ea784b5a1b6572ea1a2625db347859bd6e410eda`，`mergedAt=2026-08-18T01:24:08Z`。 |
| PR #35 | 冻结后真实教学闭环维护；base `f95effcc1ea681530b4be6d01de724f4f999d9f6`，final head 待 ordinary merge 时写入。覆盖任课范围、私有题隔离、release 管理、撤回、历史答卷与试卷软删除；不新增 Flyway 或表。 |
| 当前产品阶段 | `THESIS_AND_DEFENSE_DELIVERY`；`PRODUCT DEVELOPMENT = FROZEN`。仅处理学校模板、论文、答辩材料、真实 BLOCKER 或老师明确要求。 |

## 3. 已实现、已验证与边界

| 模块 | IMPLEMENTED | VERIFIED / 边界 |
|---|---|---|
| 认证与安全 | CAPTCHA、JWT、首次改密、反账号枚举的密码恢复、BCrypt、角色路由。 | 测试和机器浏览器验证受控流程；不公开验收账户或凭据。 |
| 学生学习 | 练习、单选/多选/填空确定性判分、STANDARD、错题、掌握度、推荐、专题、高频考点、试卷、私信。 | AI 不可用不影响登录、练习、判分、错题和 STANDARD。 |
| 教师 | ACTIVE 任课范围、私有题、候选审核、手动/随机/规则组卷、发布、打印预览和教学分析。 | 手动组卷支持专题主观题；随机/规则默认只抽客观题，教师不可跨任课范围取题。 |
| 管理员 | 学生、教师、班级、任课关系、题库审核、导入、模型配置、密码恢复与操作日志。 | TEACHER/ADMIN 来自角色表；最后一个 ENABLED ADMIN 及当前管理员自撤销受保护。 |
| 题库与附件 | `ti_mu` 是唯一题目事实源；题干、选项、STANDARD、知识点、附件和审核记录受状态机约束。 | 不创建第二套专题题库；附件走受控存储和 content API。 |
| 专题学习 | 15 单元、45 道原创 `SUBJECTIVE + TOPIC_LEARNING` 题，物理 6 / 化学 5 / 生物 4，每单元 FOUNDATION→TRANSFER→ADVANCED。 | 题目事实在 `ti_mu`，专题单元只编排；STANDARD 是权威解析。 |
| 试卷 | 保存、发布、冻结题干/选项/答案/STANDARD/知识点/附件，学生作答与提交。 | 客观题确定性得分写入 `ke_guan_de_fen`；主观题保存为 `SUBJECTIVE_PENDING`，不自动评分、不由 AI 给正式分。 |

## 4. 学习闭环与对应事实

`账号与教学组织 → 结构化题库 → 在线练习 → 确定性判分 → STANDARD → 错题事实 → AI 错因/当前题答疑 → AI 候选变式 → 人工审核 → 专题学习 → 高频考点 → 教师题库 → 组卷发布 → 学生作答 → 教学分析`。

认证与组织主要使用 `yong_hu`、`yong_hu_jiao_se`、`ban_ji`、`ren_ke_guan_xi`；题库核心为 `ti_mu`、`ti_mu_jie_xi`、`ti_mu_zhi_shi_dian`、`ti_mu_fu_jian`；练习事实为 `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`cuo_ti_ji_lu`；专题编排为 `zhuan_ti_xue_xi_dan_yuan` 与其题目关系表；试卷发布和提交为 `shi_juan`、`shi_juan_ti_mu`、`shi_juan_fa_bu`、`shi_juan_fa_bu_ti_mu`、`shi_juan_ti_jiao`、`shi_juan_xue_sheng_da_ti`。具体字段、索引和约束见 [数据库最终参考](DATABASE_SCHEMA_REFERENCE.md)。

## 5. AI：工程能力与真实实验必须分开

| 能力 | 工程实现 | 可作为真实 Provider 结论的证据 |
|---|---|---|
| DeepSeek TEXT | Provider Core、Schema V2、字段级 Parser、最多一次修复、受控错误、错因/答疑/候选生成入口。 | PR #31 留有历史真实调用证据；本轮没有安全可用凭据时状态为 `BLOCKED_EXTERNAL_PROVIDER`，不能写为实时 PASS。 |
| GLM Vision | 图片受控上下文、尺寸/MIME 限制与测试 wrapper。 | 历史实验与当前凭据状态分开记录；无本轮凭据不推断真实成功。 |
| xAI Vision | 历史兼容适配及配置模型。 | 历史适配，不把测试 Fake 作为真实调用。 |
| Web Search | 独立 SEARCH runtime、结构化结果、最大返回控制。 | 真实 smoke 取决于凭据；无凭据为外部阻塞。 |
| Fake/Test Provider | 用于自动化可重复性。 | 永远不计作真实 Provider PASS。 |

AI 只能解释、对话、生成候选和给质量建议；候选需人工审核。`reasoning_content` 不展示、不持久化；当前题答疑最多 10 轮；AI 不能覆盖 PUBLISHED STANDARD、确定性判分、正式答案或主观题分数。完整边界见 [AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)。

## 6. 数据库最终事实

- MySQL 8.4；正式 schema：`rike_tiku`；当前迁移链为 V1–V30，所有历史迁移不可回改。
- 结构快照：[`database/schema_snapshot_v30.sql`](../database/schema_snapshot_v30.sql)（DDL only，含 Flyway history 表 + 50 张业务表；无 INSERT、账户、密码、token、Key 或业务记录）。V29 快照仅为 [历史快照](../database/schema_snapshot_v29.sql)。
- V30 不新增业务表：为 `shi_juan_fa_bu_ti_mu` 增加 `fu_jian_kuai_zhao` JSON 附件冻结数组和 JSON ARRAY check，并扩展学生试卷答题状态长度以承载 `SUBJECTIVE_PENDING`。

| 迁移 | 一句话用途 |
|---|---|
| V1 | 科目与知识点骨架。 |
| V2 | 题目、选项、解析、知识点、来源和附件核心。 |
| V3 | 三科学科样题。 |
| V4 | 允许专题学习的主观题使用模式。 |
| V5 | 用户、角色、学生/教师档案。 |
| V6 | 班级、学生归属和任课关系。 |
| V7 | 练习会话、答题、错题和学习结果。 |
| V8 | 高频考点。 |
| V9 | 师生消息。 |
| V10 | 用户档案扩展。 |
| V11 | 管理员操作日志。 |
| V12 | AI 调用日志。 |
| V13 | 学生 AI 学习事实。 |
| V14 | AI 生成任务、候选与模型配置。 |
| V15 | 当前题 AI 对话扩展为最多 10 轮。 |
| V16 | 学生 AI runtime 选择与搜索使用记录。 |
| V17 | 密码恢复申请。 |
| V18 | 教师试卷和试卷题目。 |
| V19 | 学生 AI 变式练习实例。 |
| V20 | 任课范围私有题与专题题分类。 |
| V21 | 知识卡片范围扩展。 |
| V22 | 消息撤回和按用户隐藏。 |
| V23 | 专题 AI 对话上下文。 |
| V24 | 已审核学生变式任务。 |
| V25 | 变式新颖度审计元数据。 |
| V26 | 专题单元编排与 xAI Vision 配置。 |
| V27 | 试卷发布、题目冻结快照、学生提交和自动评分事实。 |
| V28 | 已审核科学知识卡片。 |
| V29 | 知识卡片练习实例。 |
| V30 | 发布试卷题目附件快照与主观题待人工处理状态容量。 |

## 7. 验证、人工验收与已知限制

PR #35 merge 前最终回归：后端 `mvn test` 与 `mvn package` 均为 **221 tests、0 failures、0 errors、3 skipped、BUILD SUCCESS**；前端单 worker `npm test -- --run --maxWorkers=1` 为 **68 files、224 tests、0 failures**；`npm run type-check`、`npm run build` 与 `npm audit --omit=dev` 通过，audit 为 **0 vulnerabilities**（构建仅保留既有 >500 kB chunk warning）。随机临时 schema 已从 V1 完整迁移至 V30；科学审计为 **600 strings、0 live database rows、0 errors**，其中 0 行表示本次不读取正式库，不可与历史正式库 117 行审计混写；正式文献/BibTeX 各 **22** 且一一对应。不得沿用 215、217、220（前端）或 222 等旧基线。

正式数据库 `rike_tiku` 的本轮只读复核状态为 `FORMAL_DB_LIVE_RECHECK = LOCAL_NOT_VERIFIED`：本机没有可安全使用的数据库凭据，本轮没有猜测、输出或硬编码密码，也没有修改正式库。历史 V30/30 success/0 failed/50 表的已存档核验仍可查，但不得误写为本次 live recheck。

现有 V30 机器浏览器证据为 11 页面、56 断言、0 console/page/非预期失败请求/横向溢出，属于 `MACHINE_BROWSER_VERIFIED`，不等同用户逐页验收。PR34-MA-001 增加两个独立 Chromium handler 断言：student preview=1、answer preview=1、0 console/page/failed request；受控路由响应仅证明处理器，不能证明 OS 打印对话框。

- PR #33：用户完成最终页面审查并条件授权 ordinary merge；两项反馈修复后 PR #33 已合并。历史记录见 [PR #33 人工验收](MANUAL_ACCEPTANCE_FINDINGS_PR33.md)。
- PR #34：用户已审查专题、主观组卷、发布、作答、STANDARD、附件和题型中文化；唯一新增 finding 为 [PR34-MA-001](MANUAL_ACCEPTANCE_FINDINGS_PR34.md)，现为 `FIXED`。代码点击链为 `PRINT_HANDLER_MACHINE_VERIFIED`；用户真实 Chrome 已确认系统打印窗口打开（`PRINT_USER_VERIFIED` / `OS_PRINT_DIALOG_USER_VERIFIED`），未声称已实际打印纸张。
- 已知限制：真实外部 Provider 依赖运行时凭据，因此本轮可为 `BLOCKED_EXTERNAL_PROVIDER`；主观题不做 AI/规则自动正式评分；headless 浏览器不能证明 OS 级打印窗口。

## 8. 离线资料入口

- [最终截图证据目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)：截图、数据、代码、API、表、迁移、测试、论文图注与不应声称的结论。
- [快速功能—截图—代码索引](FEATURE_SCREENSHOT_CODE_INDEX.md) 与 [功能技术地图](FEATURE_CODE_TECH_MAP.md)。
- [论文写作中心](THESIS_WRITING_HUB.md)、[论文事实核验](thesis/RIKE_THESIS_FACT_CHECK.md)、[答辩提纲](thesis/RIKE_DEFENSE_OUTLINE.md)、[答辩事实问答](DEFENSE_FACTS_AND_QA.md)。
- [正式参考文献 22 条](THESIS_REFERENCES.md)、[引用使用矩阵](thesis/RIKE_REFERENCE_USAGE_MATRIX.md)。`research-only` 资料不属于正式论文引用。

## 8a. PR #35 试卷发布管理维护（用户已接受，待 ordinary merge）

PR #35 仅补齐教师试卷发布后的管理闭环，不新增迁移、表或自动评分：教师可集中查询本人所有班级 release，并按唯一任课范围、状态和试卷名称筛选；可查看历史统计和已提交答卷。撤回将 release 标记为 `CANCELLED`，学生立即不可见，但冻结快照、提交和逐题答案继续保留。试卷本体使用既有 `shi_juan.yi_shan_chu` 软删除：从未发布或全部 release 已撤回时可清理“我的试卷”，只要存在 `PUBLISHED`/`CLOSED` release 即由服务端拒绝。详情见 [PR #35 人工验收记录](MANUAL_ACCEPTANCE_FINDINGS_PAPER_RELEASE_MANAGEMENT.md)。

该维护已完成随机临时 schema 回归：后端 **221 tests、0 failures、0 errors、3 skipped**，前端 **68 files、224 tests、0 failures**；`mvn test`、`mvn package`、前端 type-check/build/audit 均通过（audit 0 vulnerabilities）。用户人工确认教师发布管理、班级作答/历史、纵向答卷审查、答案显示、撤回不可见、任课范围区分和当前更多菜单均可接受。`MACHINE_BROWSER = NOT_RUN`，因此不把用户人工验收误写为机器浏览器证据。

## 9. 论文快速取材

| 论文主题 | 截图/代码入口 | 数据库与文献入口 |
|---|---|---|
| 需求分析与总体设计 | [截图目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)、[功能技术地图](FEATURE_CODE_TECH_MAP.md) | [引用使用矩阵](thesis/RIKE_REFERENCE_USAGE_MATRIX.md) |
| 数据库设计 | [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md)、[功能—表地图](FEATURE_DATABASE_TABLE_MAP.md) | [V30 DDL](../database/schema_snapshot_v30.sql) |
| 学生学习闭环 | [学生/练习/错题截图](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#pr-33-历史匿名图0142) | `lian_xi_*`、`xue_sheng_da_ti`、`cuo_ti_ji_lu`；见功能—表地图 |
| AI 边界 | [AI 截图与图注](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md) | [AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)、引用矩阵；不得把外部阻塞写成 PASS |
| 专题学习 | [V30 专题证据](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#v30-机器浏览器证据) | `ti_mu` 与专题单元关系；V20/V26 |
| 教师组卷与发布 | [混合试卷证据](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#v30-机器浏览器证据) | `shi_juan*`、V27/V30；主观题不自动评分 |
| 管理员与 Excel | [Excel 图和导入页](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#excel-模板与导入页面资料) | [Excel 导入指南](EXCEL_IMPORT_GUIDE.md)、导入/审核表 |
| 测试与答辩 | [事实核验](thesis/RIKE_THESIS_FACT_CHECK.md)、[答辩提纲](thesis/RIKE_DEFENSE_OUTLINE.md) | [开发状态](DEVELOPMENT_STATUS.md)、[引用使用矩阵](thesis/RIKE_REFERENCE_USAGE_MATRIX.md) |
