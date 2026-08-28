# RIKE 最终项目事实包

> 本文件是论文、答辩和后续维护的当前事实入口，不是开发日志。历史时间线中的旧测试数、旧 Flyway 版本或旧表数只能解释当时阶段，不能覆盖本文件。最后一次事实刷新：2026-08-24；PR #40 已 ordinary merge，产品开发重新永久冻结。

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
| PR #35 | 冻结后真实教学闭环维护；base `f95effcc1ea681530b4be6d01de724f4f999d9f6`，final head `e1f90dc756baae02d6b501eeef967d511e0731ab`，已 ordinary merge。覆盖任课范围、私有题隔离、release 管理、撤回、历史答卷与试卷软删除；不新增 Flyway 或表。 |
| PR #35 merge | `fde39c53efca316010abf63acf56fda2c631315c`，`mergedAt=2026-08-18T07:30:00Z`。这是产品代码最终合并基线；随后的 docs-only 事实提交不改变产品行为。 |
| PR #36 / PR #37 | 最终演示资料与数据卫生维护；均已 ordinary merge。PR #37 merge commit 为 `cb785631c359b88dc4841a9eeed3af14879516cb`。 |
| PR #38 | 初始密码门禁语义恢复与操作日志 CSV 删除；base `b3fea4e39b794e3b89412a7089a81cba867c7a10`，final head `1636e2dc493fcd5a8c5d8c4be9f60751975e8445`，ordinary merge commit `4da94b79fc682c8756cfab12dd73c40fbbe87e8b`，`mergedAt=2026-08-21T06:54:58Z`。后端 224 tests（0/0/3）、前端 68 files / 225 tests（0 failures）、type-check、build、audit、科学审计和 22 条文献审计均通过。 |
| PR #39 | 默认密码实值门禁补丁；base `237d37709ccc4d5fdff5f219fc5468113e81c0fc`，final head `e3153632139ca0e9757b5e505409ecc85a0552c6`，ordinary merge commit `8e824718dde9aa4f54ab99fa52735f6eb6d46dbc`，`mergedAt=2026-08-21T07:22:27Z`。认证门禁采用 `firstLogin || BCrypt.matches(configuredDefaultPassword)`，初始/主动改密均禁止重设为系统默认口令；操作日志 CSV 导出保持移除。后端 225 tests（0/0/3）、前端 68 files / 225 tests（0 failures）、type-check、build、audit、科学审计和 22 条文献审计均通过；无 Flyway、表结构或 199/200/203 教学数据改动。 |
| PR #40 | 真实 Demo 发现的试卷提交、反馈和教师同步修复；base `fa4735af5852848751637529d1d02723f26351bf`，final head `7fbdbb6f8fe9f73ba736f3a47166fa5000856d30`，ordinary merge commit `e44aee35c66e39ad44fb7a8f9582a45e229ff674`，`mergedAt=2026-08-24T02:37:02Z`。学生端对客观题做题型化完整性检查，串行处理草稿保存与正式提交，并重新读取服务端判分事实；教师作答弹窗提供手动刷新和 5 秒只读轮询；试卷行改为三个直接操作加“更多操作（发布管理/删除试卷）”。无生产后端判分改写、无 Flyway、无表结构变化。 |
| 当前产品阶段 | `THESIS_AND_DEFENSE_DELIVERY`；`PRODUCT DEVELOPMENT = FROZEN`；`NO OPEN PRODUCT DEVELOPMENT PR`。仅处理学校模板、论文、答辩材料、真实 BLOCKER 或老师明确要求。 |

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

PR #40 候选的最终回归为：后端 **227 tests、0 failures、0 errors、3 skipped**，package PASS；前端 **68 files、231 tests、0 failures**，type-check/build PASS，audit **0 vulnerabilities**。随机临时 schema 完整执行 V1→V30；正式 `rike_tiku` 与 `rike_tiku_demo` 均只读核验为 V30、30 success、0 failed、50 业务表，项目 schema 仅这两个。科学审计为 **600 strings、107 formal database rows、0 errors**；正式文献/BibTeX 为 **22/22**。

现有 V30 机器浏览器证据为 11 页面、56 断言、0 console/page/非预期失败请求/横向溢出，属于 `MACHINE_BROWSER_VERIFIED`，不等同用户逐页验收。PR34-MA-001 增加两个独立 Chromium handler 断言：student preview=1、answer preview=1、0 console/page/failed request；受控路由响应仅证明处理器，不能证明 OS 打印对话框。

- PR #33：用户完成最终页面审查并条件授权 ordinary merge；两项反馈修复后 PR #33 已合并。历史记录见 [PR #33 人工验收](MANUAL_ACCEPTANCE_FINDINGS_PR33.md)。
- PR #34：用户已审查专题、主观组卷、发布、作答、STANDARD、附件和题型中文化；唯一新增 finding 为 [PR34-MA-001](MANUAL_ACCEPTANCE_FINDINGS_PR34.md)，现为 `FIXED`。代码点击链为 `PRINT_HANDLER_MACHINE_VERIFIED`；用户真实 Chrome 已确认系统打印窗口打开（`PRINT_USER_VERIFIED` / `OS_PRINT_DIALOG_USER_VERIFIED`），未声称已实际打印纸张。
- 已知限制：真实外部 Provider 依赖运行时凭据，因此本轮可为 `BLOCKED_EXTERNAL_PROVIDER`；主观题不做 AI/规则自动正式评分；headless 浏览器不能证明 OS 级打印窗口。

### PR #40 试卷提交闭环（已 ordinary merge）

- 学生提交始终携带当前发布项 `itemId` 的完整内存答案；未完成客观题在前端显示具体题号并定位，后端 `PAPER_ANSWER_INCOMPLETE` 门禁继续保留。
- 正式得分仍由 `PaperAssignmentService → ObjectiveAnswerGrader → shi_juan_xue_sheng_da_ti / shi_juan_ti_jiao` 生成，前端不自行计算分数。单选、多选和填空写入 `GRADED`；主观题只写入 `SUBJECTIVE_PENDING`。
- 学生提交后重新读取详情与任务列表；教师 stats/submissions 从同一 MySQL 事实读取，并在作答弹窗打开时每 5 秒只读刷新，关闭后停止。
- 真实 Demo 机器浏览器证据为 8/8 断言、0 console/page/failed request/overflow，见 [试卷提交同步证据](evidence/paper-submission-sync/README.md)。该证据不等同用户人工验收。

## 8. 离线资料入口

- [最终截图证据目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)：截图、数据、代码、API、表、迁移、测试、论文图注与不应声称的结论。
- [快速功能—截图—代码索引](FEATURE_SCREENSHOT_CODE_INDEX.md) 与 [功能技术地图](FEATURE_CODE_TECH_MAP.md)。
- [答辩事实问答](DEFENSE_FACTS_AND_QA.md)。

## 8a. PR #35 试卷发布管理维护（已 ordinary merge）

PR #35 仅补齐教师试卷发布后的管理闭环，不新增迁移、表或自动评分：教师可集中查询本人所有班级 release，并按唯一任课范围、状态和试卷名称筛选；可查看历史统计和已提交答卷。撤回将 release 标记为 `CANCELLED`，学生立即不可见，但冻结快照、提交和逐题答案继续保留。试卷本体使用既有 `shi_juan.yi_shan_chu` 软删除：从未发布或全部 release 已撤回时可清理“我的试卷”，只要存在 `PUBLISHED`/`CLOSED` release 即由服务端拒绝。详情见 [PR #35 人工验收记录](MANUAL_ACCEPTANCE_FINDINGS_PAPER_RELEASE_MANAGEMENT.md)。

该维护已完成随机临时 schema 回归：后端 **221 tests、0 failures、0 errors、3 skipped**，前端 **68 files、224 tests、0 failures**；`mvn test`、`mvn package`、前端 type-check/build/audit 均通过（audit 0 vulnerabilities）。用户人工确认教师发布管理、班级作答/历史、纵向答卷审查、答案显示、撤回不可见、任课范围区分和当前更多菜单均可接受。`MACHINE_BROWSER = NOT_RUN`，因此不把用户人工验收误写为机器浏览器证据。PR #35 于 `2026-08-18T07:30:00Z` 以 ordinary merge 合入 `main`。

## 9. 论文快速取材

| 论文主题 | 截图/代码入口 | 数据库与文献入口 |
|---|---|---|
| 需求分析与总体设计 | [截图目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)、[功能技术地图](FEATURE_CODE_TECH_MAP.md) | [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md) |
| 数据库设计 | [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md)、[功能—表地图](FEATURE_DATABASE_TABLE_MAP.md) | [V30 DDL](../database/schema_snapshot_v30.sql) |
| 学生学习闭环 | [学生/练习/错题截图](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#pr-33-历史匿名图0142) | `lian_xi_*`、`xue_sheng_da_ti`、`cuo_ti_ji_lu`；见功能—表地图 |
| AI 边界 | [AI 截图与图注](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md) | [AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)、引用矩阵；不得把外部阻塞写成 PASS |
| 专题学习 | [V30 专题证据](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#v30-机器浏览器证据) | `ti_mu` 与专题单元关系；V20/V26 |
| 教师组卷与发布 | [混合试卷证据](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#v30-机器浏览器证据) | `shi_juan*`、V27/V30；主观题不自动评分 |
| 管理员与 Excel | [Excel 图和导入页](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md#excel-模板与导入页面资料) | [Excel 导入指南](EXCEL_IMPORT_GUIDE.md)、导入/审核表 |
| 测试与答辩 | [最终截图证据目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)、[答辩事实问答](DEFENSE_FACTS_AND_QA.md) | [开发状态](DEVELOPMENT_STATUS.md) |

## 10. 最终演示数据与门户实时统计（final-demo-cleanup）

- 稳定教学事实：正式 `rike_tiku` 的 199班、200班及张锡鹏（物理、兼 ADMIN）、吴雪莉（化学）、谢亚坤（生物）的既有 ACTIVE 任课关系只读核验后保留，未删除、迁移或重建。
- 新增隔离范围：`CLASS_203`（203班，高三，ACTIVE）与张生康（`TEACHER`，非 ADMIN）仅建立 `PHYSICS` 的 ACTIVE 任课关系；203 班已有张鸿敏、李胡张博、赵雪尧三名主班级学生。选择物理的原因是当前正式库中 PUBLISHED 自主练习题 120 道、专题题 20 道，足以完整演示工作台、题库、组卷、发布和教学分析。
- 演示导入资料：[四份真实模板工作簿](demo-import/README.md)。学生文件与三份单学科题库文件在一次性 schema 中完成 Preview/Confirm；它们未 Confirm 到 `rike_tiku_demo`，保留现场首次导入路径。三科题目均为 RIKE 项目原创，导入后仍必须经历 `PENDING → 人工审核`。
- 公共门户不再写死 `3 / 360 / 18`。`GET /api/v1/public/portal-stats` 只读统计 ACTIVE 学科、GLOBAL + PUBLISHED 的可自动判分练习题和 GLOBAL + PUBLISHED 的专题主观题；接口只开放该精确路径。失败时前端显示 `—`，绝不回退到旧常量。
- 当前只读口径样本：`rike_tiku = 3 / 360 / 47`，`rike_tiku_demo = 3 / 360 / 18`。数字随数据库发布、停用或软删除即时变化，不是论文或业务规则中的固定承诺。
- 代码速查：[答辩代码—业务速查图](DEFENSE_CODE_BUSINESS_QUICK_MAP.md)；数据库关系见 [功能—数据库表地图](FEATURE_DATABASE_TABLE_MAP.md)。
# 最终演示收口（2026-08-21，PR #36 已 ordinary merge）

PR #36（head `d1a76015f25f963d43524a44b9f87e9841545936`）已于 `2026-08-21T02:21:22Z` 以 ordinary merge 合入 main；merge commit 为 `ac6e4679e8688bec5ed63a284aa962b3ca6cb618`。它保留 199/200 与张锡鹏、谢亚坤、吴雪莉的既有教学事实，新增 203 班、张生康（203班物理）和三名指定学生；四份可重复 Preview/Confirm 的演示 Excel 位于 [demo-import](demo-import/README.md)。公共门户不再写死 3/360/18，而是调用受限的实时统计接口；正式库当前返回 3/360/47，Demo 库返回 3/360/18。完整的保全、导入、回归和风险证据见 [FINAL_CLEANUP_REGRESSION_REPORT](FINAL_CLEANUP_REGRESSION_REPORT.md)。

## 最终认证与数据卫生口径（待 PR #37 合并后冻结 SHA）

- 203 演示账号统一为张生康 `t2026004`（工号 `T2026004`，仅 `TEACHER`）及张鸿敏、李胡张博、赵雪尧 `2026203001`、`2026203002`、`2026203003`；三位旧核心教师和 199/200 的已存在教学关系不修改。
- 账户默认口令只由 `app.account.default-reset-password` 管理；当前本地开发配置值为 `a1234567`，数据库只保存 BCrypt 哈希。所有管理员新建、导入或恢复账户均写入 `shi_fou_shou_ci_deng_lu=1`；首次登录只能完成初始密码修改，成功后写回 `0` 并进入正常业务。用户主动改密与管理员密码恢复保留。

### PR #38 认证语义修复（merge 前门禁通过）

- `ChuShiMiMaMenJinGuoLvQi` 在 JWT 认证后执行，受控返回 `MUST_CHANGE_PASSWORD`，不通过比较明文默认密码实现。
- 前端 `/change-initial-password` 路由和登录后跳转恢复；刷新会话与服务端 403 都会保持该安全语义。
- 管理员操作日志不再提供 CSV 导出；列表、筛选、排序、分页、详情、刷新和删除仍可用。
- 已完成对 `rike_tiku` 与 `rike_tiku_demo` 的 `V30_BROWSER`、`V30_BROWSER_STUDENT`、`V30_BROWSER_TEACHER`、`V30_BROWSER_CLASS` 全字段审计；仅发现并定向清除了本轮浏览器测试班级、两账号、任课范围、试卷/release 快照、提交、逐题答案和高频考点测试关联。Flyway V1–V30、schema、199/200、三位旧教师及其历史均未修改。
- merge 前回归为后端 224 tests（0 failures、0 errors、3 skipped）、前端 68 files / 225 tests（0 failures）、type-check、build、audit（0 vulnerabilities）、科学审计和正式 22 条文献审计均通过；正式库为 V30、30 success、0 failed、50 业务表。详见[演示账号清单](FINAL_DEMO_ACCOUNTS.md)与[数据卫生报告](FINAL_AUTH_DATA_HYGIENE_REPORT.md)。合并 SHA 将在 ordinary merge 后冻结。

### PR #37 合并冻结

PR #37 final head `f6456be59313e3bf88c45947f362c0c32cadba87` 已于 `2026-08-21T03:07:23Z` ordinary merge；merge commit 与当前 main 基线均为 `cb785631c359b88dc4841a9eeed3af14879516cb`。最终回归为后端 **224 tests、0 failures、0 errors、3 skipped**，`mvn -DskipTests package` PASS；前端 **68 files、225 tests、0 failures**，type-check/build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。Flyway 保持 V30、正式库 30 success/0 failed、50 business tables；科学审计为 600 strings/0 errors，正式文献与 BibTeX 为 22/22。产品继续处于 `PRODUCT DEVELOPMENT = FROZEN` / `PROJECT PHASE = THESIS_AND_DEFENSE_DELIVERY`。
