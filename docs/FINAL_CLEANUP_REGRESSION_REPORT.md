# 最终演示收口回归报告

日期：2026-08-21；PR #36 head：`d1a76015f25f963d43524a44b9f87e9841545936`。该候选已以 ordinary merge 合入 main（merge commit `ac6e4679e8688bec5ed63a284aa962b3ca6cb618`；`mergedAt=2026-08-21T02:21:22Z`）。本报告的测试均在 merge 前最终 HEAD 上执行。

## 数据保全与新增演示范围

- 正式 `rike_tiku` 中张锡鹏（199/200 物理，ADMIN+TEACHER）、谢亚坤（199/200 生物）和吴雪莉（199/200 化学）的原 ACTIVE 任课关系已只读核验，未修改。
- 新增 `CLASS_203`（203班、高三、ACTIVE）、张生康（仅 TEACHER）及其 `203班→物理→ACTIVE` 任课关系；未建立张生康到 199/200 的任课关系。
- 203 班当前含张鸿敏、李胡张博、赵雪尧三名指定学生。张生康选物理，是因为正式 GLOBAL 可练习物理题 120 道、专题主观题 20 道，足以完整演示范围、练习、组卷、发布与分析。

## 导入演示资料

- [学生导入_203班_演示.xlsx](demo-import/学生导入_203班_演示.xlsx)
- [题库导入_物理_演示.xlsx](demo-import/题库导入_物理_演示.xlsx)
- [题库导入_化学_演示.xlsx](demo-import/题库导入_化学_演示.xlsx)
- [题库导入_生物_演示.xlsx](demo-import/题库导入_生物_演示.xlsx)

四份工作簿均保留真实模板和 Sheet/列规则。`FinalDemoImportWorkbooksIntegrationTest` 在随机 V1→V30 schema 中对四份文件执行 Preview 与 Confirm：学生 2 行有效并确认；每个题库文件 3 题有效并确认进入 `PENDING`。工作簿未 Confirm 写入 `rike_tiku_demo`，可用于现场首次演示。

对 `rike_tiku` 与 `rike_tiku_demo` 的同学科现有题进行 hash 审计：9 题精确冲突为 0；题干相似度最高为 0.533，低于 0.60 审计阈值；知识点、答案和 STANDARD 均由 Preview 通过。

## 动态门户统计

`GET /api/v1/public/portal-stats` 仅返回 `subjectCount`、`automaticPracticeQuestionCount`、`topicQuestionCount`。统计口径：ACTIVE 未删除学科；PUBLISHED、未删除、GLOBAL、`ONLINE_PRACTICE`、可自动判分且客观题型；以及 PUBLISHED、未删除、GLOBAL、`TOPIC_LEARNING`、SUBJECTIVE、非自动判分题。首页请求失败显示 `—`，不回退硬编码值。

实际接口：`rike_tiku_demo=3/360/18`；正式库 `rike_tiku=3/360/47`（专题题按物理 20、化学 15、生物 12）。

## 回归与数据库

- 后端：`mvn clean test`：224 tests，0 failures，0 errors，3 skipped；`mvn -DskipTests package`：PASS。
- 前端：68 test files、225 tests、0 failures；`npm run type-check`、`npm run build`、`npm audit --omit=dev`：PASS，0 vulnerabilities。
- 随机 schema：Flyway V1→V30 PASS。正式 `rike_tiku` 只读核验：V30、30 success、0 failed、50 business tables。
- 科学审计：600 strings、117 formal database rows、0 errors；正式参考文献/BibTeX：22/22。

## 风险结论

BLOCKER=0；HIGH=0；MEDIUM=0。外部真实 Provider 状态仍以 `AI_FINAL_EXPERIMENT_RESULTS.md` 为准，未因本次回归改写为成功。机器浏览器只验证了门户公开接口响应；动态门户的历史截图不用于证明动态数字。

## PR #37 认证与数据卫生追加门禁

本次维护不增加迁移、不改动 50 张业务表。测试完成前的数据库审计已确认：正式库和演示库中的活跃演示账号均为非强制首次改密状态；203 指定账户按统一编号存在；全库字段扫描与关系审计后，仅定向删除 `V30_BROWSER*` 浏览器测试根数据及其已验证无外部引用的派生事实。清理清单、保留边界和本机受安全策略限制而未物理删除的纯临时目录见 [FINAL_AUTH_DATA_HYGIENE_REPORT.md](FINAL_AUTH_DATA_HYGIENE_REPORT.md)。本节的最终测试数字只以 PR #37 完整回归输出为准。

## PR #40 试卷提交闭环回归追加（2026-08-24）

- 后端：227 tests，0 failures，0 errors，3 skipped；package PASS。混合卷专项固定验证单选正确、多选逆序正确、填空错误和主观答案，结果为 20/30、2/3、1 道 `SUBJECTIVE_PENDING`；重复提交幂等，客观题缺失整卷拒绝且不落部分提交。
- 前端：68 files / 231 tests，0 failures；type-check/build/audit PASS，0 vulnerabilities。覆盖未完成题号提示、禁止无效请求、自动保存串行、服务端错误反馈、重新加载结果、逐题得分、教师手动/定时刷新和操作菜单。
- 数据：随机 schema V1→V30 PASS；正式库与 Demo 均为 V30、30 success、0 failed、50 表，未新增迁移；科学审计 600 strings/107 formal rows/0 errors；参考文献 22/22。
- 浏览器：真实 `rike_tiku_demo` 发布完成学生提交、学生结果、任务列表、教师轮询同步和教师答卷，8/8 assertions，0 console/page/failed request/overflow。证据为 `MACHINE_BROWSER_VERIFIED`，不等同真人验收。
- Git：PR #40 final head `7fbdbb6f8fe9f73ba736f3a47166fa5000856d30` 已于 `2026-08-24T02:37:02Z` ordinary merge；merge commit `e44aee35c66e39ad44fb7a8f9582a45e229ff674`。
