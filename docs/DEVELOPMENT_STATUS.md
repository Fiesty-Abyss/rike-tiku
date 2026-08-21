# 开发状态

> 当前事实入口：[FINAL_PROJECT_FACTS.md](FINAL_PROJECT_FACTS.md)。本文件不把历史阶段的测试数字当作当前基线。

更新时间：2026-08-18。PR #33、PR #34 与 PR #35 已 ordinary merge；PR #35 merge commit 为 `fde39c53efca316010abf63acf56fda2c631315c`，`mergedAt=2026-08-18T07:30:00Z`。项目已回到 `THESIS_AND_DEFENSE_DELIVERY`，`PRODUCT DEVELOPMENT = FROZEN`。

## 当前产品事实

- Flyway：V1–V30；业务表：50。V1–V29 均为已发布历史，未修改；V30 仅在发布试卷题目快照上增加附件 JSON，并使学生答题状态可存储 `SUBJECTIVE_PENDING`，不新增表。
- 专题：15 个单元、45 道原创 `SUBJECTIVE + TOPIC_LEARNING` 大题；物理 6、化学 5、生物 4；计算 14、实验 9、流程 5、材料分析 13、综合 4。题目事实仍在 `ti_mu`，专题单元只编排。
- 试卷：教师手动组卷可检索和纳入合法范围的专题主观题；随机/规则仍只抽确定性客观题。发布冻结附件、题干、选项、答案、STANDARD 与知识点。学生主观作答保存为 `SUBJECTIVE_PENDING`，不进入 AI 或规则自动正式评分；客观得分不等于整卷最终成绩。
- PR #35（已 ordinary merge）：教师可查看单张或全局班级发布历史、按任课范围/状态/名称筛选、查看已发生的提交与统计，并撤回 release。撤回只使学生不可见；软删除 `shi_juan` 仅清理“我的试卷”，发布快照与学生作答历史保留。有效 `PUBLISHED`/`CLOSED` release 存在时服务端拒绝软删除。
- PR #35 最终回归：后端 **221 tests、0 failures、0 errors、3 skipped**；前端 **68 files、224 tests、0 failures**；`mvn test`、`mvn package`、type-check、build、audit（0 vulnerabilities）通过。用户人工已接受本轮发布历史、软删除、范围区分与答卷审查；`MACHINE_BROWSER = NOT_RUN` 如实保留。
- UI：题型和专题类型映射为中文，发布质量区域不展示 Java Map/internal enum。打印继续使用浏览器原生 `window.print()` 与 A4 CSS。
- AI：Provider Core、DeepSeek TEXT、GLM Vision、xAI Vision 历史适配、Web Search 和 Fake/Test Provider 均有工程实现；无当前凭据时真实外部状态是 `BLOCKED_EXTERNAL_PROVIDER`，Fake 不计作真实 PASS。当前题答疑最多 10 轮，AI 不覆盖 STANDARD。

## 验收状态

| 项目 | 结论 |
|---|---|
| PR #33 用户页面审查 | 两项最终修复后按用户条件授权 ordinary merge；PR #33 已合并。 |
| PR #34 用户页面审查 | 专题、主观组卷、发布、学生作答、附件、STANDARD 和题型中文化已获范围内正向反馈。 |
| PR34-MA-001 | `FIXED`；代码、点击级自动化、独立 Chromium handler 和用户真实 Chrome OS 打印窗口均已验证。 |
| V30 机器浏览器 | 历史结果：11 页面、56 断言、0 console/page/非预期失败请求/overflow；仅为机器验收。 |
| 正式数据库 | 历史已核验 `rike_tiku` 为 V30、50 业务表且未 reset；本轮 `FORMAL_DB_LIVE_RECHECK = LOCAL_NOT_VERIFIED`，因为没有可安全使用的本机凭据。 |
| 随机临时 schema | 后端全量使用随机临时 schema，V1→V30 通过。 |
| 科学与文献 | 本轮科学审计 600 strings/0 live DB rows/0 errors（未读取正式库）；正式参考文献与 BibTeX 均为 22。 |

## 当前门槛

本轮打印修复后的 backend/frontend 全量、type-check、build、audit、Flyway、科学审计和文献审计将以 merge 前最终 HEAD 的实际输出为准；实际数字见 [最终事实包](FINAL_PROJECT_FACTS.md)。不继续扩产品功能。

## final-demo-cleanup 收口范围（待本轮最终回归/PR 结论更新）

- 保持正式 199/200 与张锡鹏、吴雪莉、谢亚坤的已核验事实；新增 `CLASS_203`、张生康（仅物理）和三名 203 主班级学生，用于演示教师范围隔离。
- 新增公共只读统计接口 `/api/v1/public/portal-stats`，首页三项数值按 `ke_mu`/`ti_mu` 的公开 PUBLISHED 业务口径实时读取；不新增 Flyway，表数仍为 50。
- 提交四份真实模板演示工作簿，且由 `FinalDemoImportWorkbooksIntegrationTest` 在一次性 schema 验证 Preview/Confirm；演示库本身保持尚未导入这些工作簿。
- 进行中的最终门禁、清理与 PR 状态以 [最终项目事实包](FINAL_PROJECT_FACTS.md) 和本轮最终回归报告为准；不得把本段中的中间状态当作合并完成事实。
