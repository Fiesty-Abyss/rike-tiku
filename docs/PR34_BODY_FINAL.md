# PR #34 — 最终人工修补与仓库事实冻结

## Why

PR #34 收口 PR #33 merge 后的专题学习与主观题试卷质量，并处理用户人工验收唯一新增 finding：`PR34-MA-001`，教师学生版/答案版预览的“打印 / 另存为 PDF”按钮无响应。

## Scope

- 15 个专题单元、45 道原创 `SUBJECTIVE + TOPIC_LEARNING` 大题；统一 `ti_mu`，不建立第二题库。
- 教师手动组卷可检索专题主观大题；题型/专题类型用户界面中文化，质量建议不暴露 Java Map。
- V30 为发布题冻结受控附件 JSON；客观题继续确定性判分，主观题保存为 `SUBJECTIVE_PENDING`，不做 AI/规则自动正式评分。
- PR34-MA-001：`PaperPreviewView` 改为 `printPaper()` 显式调用 `window.print()`，保留原生 A4 `@media print`，不增加 PDF 包或后端服务。
- 新增点击级前端测试；独立 Chromium hook 在学生版和答案版均验证 `BUTTON_CLICK → printPaper() → window.print()`。

## Security and boundaries

- 不改变 ACTIVE 任课范围、发布冻结、STANDARD 权威、附件权限或学生答案隔离。
- 不把 Fake/Test Provider 写成真实 Provider PASS；无凭据的真实 Provider 状态仍为 `BLOCKED_EXTERNAL_PROVIDER`。
- Headless Chromium 只验证 print handler；用户已在真实 Chrome 点击学生版预览并确认系统 OS 打印窗口打开：`PRINT_USER_VERIFIED` / `OS_PRINT_DIALOG_USER_VERIFIED`。不声称已实际打印纸张。

## Evidence and documentation

- [PR34 人工验收记录](MANUAL_ACCEPTANCE_FINDINGS_PR34.md)
- [最终项目事实包](FINAL_PROJECT_FACTS.md)
- [最终截图证据目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)
- [功能—数据库表地图](FEATURE_DATABASE_TABLE_MAP.md)、[Excel 精确导入指南](EXCEL_IMPORT_GUIDE.md) 与真实模板渲染图。
- V30 无数据 schema snapshot、截图 data/API/code/table/test mapping、22 条正式文献使用矩阵。

## Validation

- Backend：`mvn test` 220 tests、0 failures、0 errors、3 skipped；随后 `mvn -DskipTests package` BUILD SUCCESS。
- Frontend：68 files、223 tests、0 failures；type-check、build、`npm audit --omit=dev` PASS，0 vulnerabilities。
- Data：随机临时 schema V1→V30 PASS；正式 `rike_tiku` 只读核验 V30、30 success、0 failed、50 business tables；科学审计 600 strings/117 formal rows/0 errors；正式文献/BibTeX 均为 22。
- Browser：`PRINT_HANDLER_MACHINE_VERIFIED`，学生版和答案版均触发一次 `window.print()`；0 console/page/unexpected failed request。用户真实 Chrome 已确认 OS 对话框打开（`PRINT_USER_VERIFIED`）。

## Merge gate

最终门禁为完整回归、Flyway、科学内容、正式文献、文档链接和 workspace clean。打印用户门禁已满足：

- `/teacher/papers/{id}/student`
- `/teacher/papers/{id}/answer`

上述预览共享同一打印组件；机器 handler 已双路验证，用户已确认实际系统打印窗口。最终门禁通过后转 Ready 并 ordinary merge；不得 squash/rebase/force。
