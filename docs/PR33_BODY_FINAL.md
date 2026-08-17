# PR #33 最终产品收口资料

本 PR 是 RIKE 最终交付的唯一产品 PR，目标分支为 `main`，产品分支为 `feat/final-product-completion`。本轮没有创建 PR #34，没有 force push、rebase 或 squash；最终门禁通过后使用 ordinary merge commit 合并。

## 本轮范围

- 学生端题干演示前缀统一清理、AI 专用超时和等待反馈；答案结构安全渲染。
- 错题筛选、再做和归档交互收口：答错重做保持活跃，答对后由学生确认是否归档。
- 专题改为单元入口；正式库核验 15 个已发布单元、45 条单元题目关系，学科覆盖物理 6 / 化学 5 / 生物 4；学生 AI 候选先为本人可见 DRAFT，提交后才进入 PENDING 审核。
- 学生入口改为物化生高频考点；正式库核验 65 张已发布卡片，其中 60 张来自本轮结构化内容源。没有伪造考试年份、频次或学习效果。
- 科学内容统一使用显式 `\\(...\\)` / `\\[...\\]` 数学分隔符，化学式只在 KaTeX 数学片段内使用 `\\ce{...}`；正式库科学内容审计为 600 个字符串、105 条数据库行、0 errors。
- 变式新颖度分为 ACCEPT/WARN/REJECT，按变化方式要求维度组合；WARN 只能预览或进入 DRAFT/PENDING，REJECT 不得发布，修复预算最多一次。
- 用户最终页面审查已确认除最后两项外无其他大问题：忘记密码弹窗已移除账号枚举与管理员核验等内部实现说明；教师列表已基于 `yong_hu_jiao_se` 的真实 `roles` 区分教师和管理员，并可直接授予、撤销和重新授予 ADMIN。
- 角色撤销保留教师和任课关系；当前管理员自撤销、最后 ENABLED ADMIN 撤销、当前管理员自停用均被后端保护。撤销使用角色关联表合法的 `DISABLED`，重新授权恢复为 `ACTIVE`。

## 最终验证

- 后端：`mvn clean test`，217 tests、0 failures、0 errors、3 skipped；`mvn -DskipTests package` 通过。
- 前端：68 个测试文件、221 tests；`npm run type-check`、`npm run build`、`npm audit --omit=dev` 通过，0 vulnerabilities。构建保留已知大 chunk warning。
- 随机临时 schema：Flyway V1 → V29 全量通过。正式数据库：`rike_tiku` Flyway V29、50 张业务表、0 failed migration；不执行 reset/seed/repair。
- 用户已完成最终页面人工审查；机器自动化不表述为用户逐页验收。
- 正式参考文献：22 条白名单，缺失 0、额外 0、BibTeX 22、正文外来引用 0、重复 key 0。

## Provider 与安全边界

本轮没有可安全使用的轮换后凭据。DeepSeek variant、DeepSeek tutor、GLM Vision、xAI Vision、Web Search 均记录为 `BLOCKED_EXTERNAL_PROVIDER`，没有发起真实请求；Mock/Fake 和历史窗口不推导为本轮 `REAL_PASS`。Key、JWT、密码、Authorization、Prompt、reasoning_content、Base64 和真实姓名不进入提交或截图。

## 文档交付

- README、PRODUCT、开发状态、验收台账、证据审计、功能映射、数据库参考、最终清单和论文事实核验表已按 V29/50、217/221、公式审计和 Provider 边界更新。
- `docs/thesis/deliverables/RIKE_论文事实稿_待套学校模板.docx`
- `docs/thesis/deliverables/RIKE_答辩PPT_待套学校模板.pptx`
- Word/PPT 使用前仍需套用学校模板；本机没有 Word/PowerPoint/LibreOffice 渲染器，已完成 OOXML/PPTX 结构、文字、图片和边界检查，未把系统渲染写成 PASS。

## 合并门禁

`FINAL_USER_ACCEPTED`：用户已明确授权在上述两项修复与最终 HEAD 回归通过后 ordinary merge；无需再次等待人工审查。真实 Provider 仍为 `BLOCKED_EXTERNAL_PROVIDER`，不阻塞确定性产品主链与合并。
