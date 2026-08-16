# PR #33 最终收口资料（保持 Draft）

本 PR 继续作为 RIKE 最终交付的唯一产品 PR，目标分支为 `main`，产品分支为 `feat/final-product-completion`。本轮没有创建 PR #34，没有 force push、rebase、squash，也没有合并 PR #33。

## 本轮范围

- 学生端题干演示前缀统一清理、AI 专用超时和等待反馈；答案结构安全渲染。
- 错题筛选、再做和归档交互收口：答错重做保持活跃，答对后由学生确认是否归档。
- 专题改为单元入口；正式库核验 15 个已发布单元、45 条单元题目关系，学科覆盖物理 6 / 化学 5 / 生物 4；学生 AI 候选先为本人可见 DRAFT，提交后才进入 PENDING 审核。
- 学生入口改为物化生高频考点；正式库核验 65 张已发布卡片，其中 60 张来自本轮结构化内容源。没有伪造考试年份、频次或学习效果。
- 科学内容统一使用显式 `\\(...\\)` / `\\[...\\]` 数学分隔符，化学式只在 KaTeX 数学片段内使用 `\\ce{...}`；正式库科学内容审计为 600 个字符串、105 条数据库行、0 errors。
- 变式新颖度分为 ACCEPT/WARN/REJECT，按变化方式要求维度组合；WARN 只能预览或进入 DRAFT/PENDING，REJECT 不得发布，修复预算最多一次。

## 最终验证

- 后端：`mvn clean test`，215 tests、0 failures、0 errors、3 skipped；`mvn -DskipTests package` 通过。
- 前端：68 个测试文件、220 tests；`npm run type-check`、`npm run build`、`npm audit --omit=dev` 通过，0 vulnerabilities。构建保留已知大 chunk warning。
- 正式数据库：`rike_tiku` Flyway V29、50 张业务表；V1–V29 history 29 行全部成功，迁移文件未改动，无 `rike_tiku_` 临时 schema 残留。本轮正式内容写入为受控幂等脚本，不做 reset/seed/repair/迁移。
- 正式浏览器本轮未完成登录后的页面验收：安全轮换正式账号在本机不存在，试验账号返回 `INVALID_CREDENTIALS`，状态为 `BLOCKED_LOCAL_CREDENTIAL`。Demo 18080/18081 独立 Chromium profile 覆盖 4 条学生路线，0 console/page/failed-request error、0 horizontal overflow；机器结果不等同真人复验。
- 正式参考文献：22 条白名单，缺失 0、额外 0、BibTeX 22、正文外来引用 0、重复 key 0。

## Provider 与安全边界

本轮没有可安全使用的轮换后凭据。DeepSeek variant、DeepSeek tutor、GLM Vision、xAI Vision、Web Search 均记录为 `BLOCKED_EXTERNAL_PROVIDER`，没有发起真实请求；Mock/Fake 和历史窗口不推导为本轮 `REAL_PASS`。Key、JWT、密码、Authorization、Prompt、reasoning_content、Base64 和真实姓名不进入提交或截图。

## 文档交付

- 论文事实稿、事实核验表、答辩提纲和论文资料中心已按 V29/50、215/220、15/45、公式渲染审计和 Provider 边界更新。
- `docs/thesis/deliverables/RIKE_论文事实稿_待套学校模板.docx`
- `docs/thesis/deliverables/RIKE_答辩PPT_待套学校模板.pptx`
- Word/PPT 使用前仍需套用学校模板；本机没有 Word/PowerPoint/LibreOffice 渲染器，已完成 OOXML/PPTX 结构、文字、图片和边界检查，未把系统渲染写成 PASS。

## 合并门禁

当前仍为 `FINAL_USER_REVIEW_PENDING`。等待用户完成人工验收和 GPT 独立审查后，才允许用户明确决定 ordinary merge。
