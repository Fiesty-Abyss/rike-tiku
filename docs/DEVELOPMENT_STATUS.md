# 开发状态

> 当前事实入口：[FINAL_PROJECT_FACTS.md](FINAL_PROJECT_FACTS.md)。本文件不把历史阶段的测试数字当作当前基线。

更新时间：2026-08-17。PR #33 已 ordinary merge；PR #34（`fix/topic-learning-paper-polish`）为 Draft，正在处理唯一的打印人工复验项 `PR34-MA-001`。在用户确认实际浏览器打印或接受浏览器限制前，不得合并 PR #34。

## 当前产品事实

- Flyway：V1–V30；业务表：50。V1–V29 均为已发布历史，未修改；V30 仅在发布试卷题目快照上增加附件 JSON，并使学生答题状态可存储 `SUBJECTIVE_PENDING`，不新增表。
- 专题：15 个单元、45 道原创 `SUBJECTIVE + TOPIC_LEARNING` 大题；物理 6、化学 5、生物 4；计算 14、实验 9、流程 5、材料分析 13、综合 4。题目事实仍在 `ti_mu`，专题单元只编排。
- 试卷：教师手动组卷可检索和纳入合法范围的专题主观题；随机/规则仍只抽确定性客观题。发布冻结附件、题干、选项、答案、STANDARD 与知识点。学生主观作答保存为 `SUBJECTIVE_PENDING`，不进入 AI 或规则自动正式评分；客观得分不等于整卷最终成绩。
- UI：题型和专题类型映射为中文，发布质量区域不展示 Java Map/internal enum。打印继续使用浏览器原生 `window.print()` 与 A4 CSS。
- AI：Provider Core、DeepSeek TEXT、GLM Vision、xAI Vision 历史适配、Web Search 和 Fake/Test Provider 均有工程实现；无当前凭据时真实外部状态是 `BLOCKED_EXTERNAL_PROVIDER`，Fake 不计作真实 PASS。当前题答疑最多 10 轮，AI 不覆盖 STANDARD。

## 验收状态

| 项目 | 结论 |
|---|---|
| PR #33 用户页面审查 | 两项最终修复后按用户条件授权 ordinary merge；PR #33 已合并。 |
| PR #34 用户页面审查 | 专题、主观组卷、发布、学生作答、附件、STANDARD 和题型中文化已获范围内正向反馈。 |
| PR34-MA-001 | 代码、点击级自动化和独立 Chromium handler 已修复/验证；用户实际 OS 打印复验仍为 `USER_RETEST_PENDING`。 |
| V30 机器浏览器 | 历史结果：11 页面、56 断言、0 console/page/非预期失败请求/overflow；仅为机器验收。 |
| 正式数据库 | `rike_tiku` 使用 V30、50 业务表；只读 validate/结构核验，未 reset。 |
| 随机临时 schema | 后端全量使用随机临时 schema，V1→V30 通过。 |
| 科学与文献 | 本轮科学审计 600 strings/117 正式库行/0 errors；正式参考文献与 BibTeX 均为 22。 |

## 当前门槛

本轮打印修复后的 backend/frontend 全量、type-check、build、audit、Flyway、科学审计和文献审计均已执行；实际数字见 [最终事实包](FINAL_PROJECT_FACTS.md)。用户确认打印前，PR #34 保持 Draft；不继续扩产品功能。
