# PR #34 人工验收记录

## 身份与边界

- PR：[#34](https://github.com/Fiesty-Abyss/rike-tiku/pull/34)（`MERGED`，ordinary merge commit `ea784b5a1b6572ea1a2625db347859bd6e410eda`，`mergedAt=2026-08-18T01:24:08Z`）
- Base：`fba1276862fee973129ee8b85c6fc3a1d55b8662`（PR #33 ordinary merge 后的 `main`）
- Branch：`fix/topic-learning-paper-polish`
- 本记录的初始 HEAD：`aba887a92e1bfa8bb24e184a5f1b09489efc7533`
- 人工验收来源：用户对本机正式环境的页面审查，2026-08-17。

本文件区分用户人工结论、自动化测试和机器浏览器结论；三者不能互相替代。2026-08-18 用户已完成实际浏览器打印复验并授权进入 ordinary merge 门禁。

## 用户已确认正常的 PR #34 范围

用户已明确反馈下列内容没有发现新的大问题：15 个专题单元和 45 道原创 `SUBJECTIVE + TOPIC_LEARNING` 专题大题；物理、化学、生物专题内容；教师手动检索和组入主观大题；题型中文化；发布质量建议；主观题发布、附件展示和学生作答；`SUBJECTIVE_PENDING`；主观题不由 AI 或规则自动评分；STANDARD 及页面表现。

这是一份范围内的人工页面审查结论，不等同长期课堂效果、真实 Provider 可用性或系统打印对话框的验收。

## Finding

### PR34-MA-001 — 教师试卷预览的打印按钮无响应

| 字段 | 事实 |
|---|---|
| 来源 | `USER_MANUAL_ACCEPTANCE` |
| 发现页面 | `/teacher/papers/1/student`；同一共享预览组件也用于 `/teacher/papers/{id}/answer` |
| 严重度 | LOW（A4 打印样式和预览内容已存在；不影响保存、发布、作答或评分） |
| 初始状态 | OPEN |
| 根因 | Vue 模板事件直接表达 `@click="window.print()"`。模板只解析组件实例作用域，未显式暴露的全局 `window` 不是可靠的模板成员，因此点击没有形成受测的组件调用链。 |
| 修复 | 在 `PaperPreviewView.vue` 中定义 `printPaper()`，由模板 `@click="printPaper"` 调用浏览器原生 `window.print()`；未引入 PDF 包、后端 PDF 服务或替代打印系统。 |
| 代码 | `rike-tiku-frontend/src/views/teacher/PaperPreviewView.vue` |
| 自动化测试 | `rike-tiku-frontend/src/views/teacher/PaperPreviewView.spec.ts`：spy `window.print`，点击按钮并断言调用一次。 |
| 机器浏览器 | `PRINT_HANDLER_MACHINE_VERIFIED`：独立 Chromium 在学生版和答案版路由中 hook `window.print` 后点击，均记录调用。该结论不声称 OS 打印对话框已被 headless 浏览器验证。 |
| 当前状态 | `FIXED` |
| 用户复验 | `PRINT_USER_VERIFIED`；`OS_PRINT_DIALOG_USER_VERIFIED`。用户在真实 Chrome 点击学生版预览的“打印 / 另存为 PDF”后，系统打印窗口成功出现；未声称实际打印纸张。 |

## 合并门槛

`PR34-MA-001` 已满足用户复验门槛：`PRINT_HANDLER_MACHINE_VERIFIED` 与 `PRINT_USER_VERIFIED` 均为 YES。其余 PR #34 范围已由用户人工页面审查确认，无新的 BLOCKER/HIGH finding；最终自动化、数据库、科学内容和文献门禁通过后，PR 已转 Ready 并 ordinary merge。
