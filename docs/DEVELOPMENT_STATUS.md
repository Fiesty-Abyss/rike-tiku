# 开发状态

> 当前事实入口：[FINAL_PROJECT_FACTS.md](FINAL_PROJECT_FACTS.md)。本文件不把历史阶段的测试数字当作当前基线。

更新时间：2026-08-24。PR #33 至 PR #39 已 ordinary merge；PR #40 是冻结后真实人工操作发现的试卷提交闭环修复，当前完整门禁已通过并等待 ordinary merge。

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

## PR #36 final-demo-cleanup（已 ordinary merge）

- 保持正式 199/200 与张锡鹏、吴雪莉、谢亚坤的已核验事实；新增 `CLASS_203`、张生康（仅物理）和三名 203 主班级学生，用于演示教师范围隔离。
- 新增公共只读统计接口 `/api/v1/public/portal-stats`，首页三项数值按 `ke_mu`/`ti_mu` 的公开 PUBLISHED 业务口径实时读取；不新增 Flyway，表数仍为 50。
- 提交四份真实模板演示工作簿，且由 `FinalDemoImportWorkbooksIntegrationTest` 在一次性 schema 验证 Preview/Confirm；演示库本身保持尚未导入这些工作簿。
- 最终门禁、清理与可追溯回归见 [最终项目事实包](FINAL_PROJECT_FACTS.md) 和 [最终回归报告](FINAL_CLEANUP_REGRESSION_REPORT.md)。PR #36 合入后的产品开发重新冻结，不再主动扩展功能。

## PR #37 final-auth-data-hygiene（已 ordinary merge）

- 范围仅限最终演示账号口径、取消强制首次改密门禁、已授权的 V30 浏览器测试业务数据定向清理、演示学生工作簿与事实文档；不修改 Flyway、表结构、题库业务规则或 199/200 教学事实。
- 203 固定账号、默认口令策略、真实数据审计范围和兼容性边界见 [FINAL_DEMO_ACCOUNTS.md](FINAL_DEMO_ACCOUNTS.md) 与 [FINAL_AUTH_DATA_HYGIENE_REPORT.md](FINAL_AUTH_DATA_HYGIENE_REPORT.md)。PR #37 于 `2026-08-21T03:07:23Z` 以 merge commit `cb785631c359b88dc4841a9eeed3af14879516cb` 合入 main；最终为后端 224 tests（0/0/3）、前端 68 files/225 tests（0 failures）、type-check/build/audit PASS。

## PR #38 restore-initial-password-gate（merge 前门禁通过）

- 纠正 PR #37 的认证语义：管理员创建学生/教师、学生 Excel 导入、管理员恢复密码和密码恢复申请处理后均进入首次改密状态；默认口令策略仍为统一配置，不恢复随机口令。
- JWT 后端门禁与前端改密路由同时恢复，保证手工改 URL 不能绕过；普通改密接口继续可用。
- 删除管理员操作日志 CSV 的 UI、TS API、Controller endpoint 和 Service 生成方法；保留日志管理的其他能力。
- 无 Flyway、表结构或 199/200/203 教学数据改动。merge 前最终回归：后端 **224 tests、0 failures、0 errors、3 skipped**；前端 **68 files、225 tests、0 failures**；type-check、build、audit（0 vulnerabilities）、科学审计、22 条文献审计和 V1→V30 随机 schema 通过。正式库只读复核为 V30、30 success、0 failed、50 业务表。

## PR #39 default-password-detection（已 ordinary merge）

- 默认密码门禁以 `shi_fou_shou_ci_deng_lu || BCrypt.matches(app.account.default-reset-password)` 为唯一权限口径；因此 flag 漂移为 `0` 的默认密码账号仍必须改密。登录响应、JWT、`/auth/me` 与后端门禁保持同一结果。
- 初始改密和主动改密均拒绝把新密码设置为配置的系统默认密码，受控返回 `PASSWORD_MUST_NOT_BE_DEFAULT`；不硬编码明文默认值。
- 操作日志 CSV 导出继续保持完全移除，列表、筛选、排序、分页、详情、刷新和删除不受影响。
- PR #39 于 `2026-08-21T07:22:27Z` 以 ordinary merge commit `8e824718dde9aa4f54ab99fa52735f6eb6d46dbc` 合入 main。最终回归：后端 **225 tests、0 failures、0 errors、3 skipped**；前端 **68 files、225 tests、0 failures**；type-check、build、audit（0 vulnerabilities）、科学审计和 22 条文献审计通过；正式库只读复核为 V30、30 success、0 failed、50 业务表。

## PR #40 paper-submission-grading-sync（merge 前门禁通过）

- 真实根因是学生端缺少题型化客观题完整性提示、自动保存与正式提交没有串行边界，以及教师作答统计只在打开时读取一次；后端共享 `ObjectiveAnswerGrader` 判分链本身已存在且继续复用。
- 学生提交成功后重新读取服务器结果，显示已提交、客观得分、学生答案、正确答案、本题得分与 STANDARD；主观题仍为 `SUBJECTIVE_PENDING`。
- 教师作答情况支持手动刷新和弹窗开启期间 5 秒只读轮询；试卷行操作收口为学生版、答案版、发布到班级和“更多操作（发布管理/删除试卷）”。
- 回归：后端 **227 tests、0 failures、0 errors、3 skipped**；前端 **68 files、231 tests、0 failures**；package、type-check、build、audit（0 vulnerabilities）通过。正式库/Demo 均 V30、30 success、0 failed、50 表；无 V31。
- 真实 Demo 机器浏览器为 8/8 assertions，0 console/page/failed-request/overflow；证据见 [paper-submission-sync](evidence/paper-submission-sync/README.md)，不冒充用户人工验收。
