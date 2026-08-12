# 开发状态

更新时间：2026-08-12

## 当前状态

PR #30 已 ordinary merge，merge commit 为 `d67ebc83bf0b8a2fbd889290d5a0f78a27d7640e`。当前阶段为 PR #31 `chore/final-ai-integration-verification`，只执行最终集成、全量回归、Demo、真实 Provider、机器浏览器、文档与人工验收准备，原则上不新增业务功能。

| 范围 | 状态 |
| --- | --- |
| 非 AI 主链 | `DONE_VERIFIED` |
| Provider Core | `DONE_VERIFIED` |
| 学生错因分析与当前题答疑 | `DONE_VERIFIED` |
| 管理员 AI 模型配置 | `DONE_VERIFIED` |
| 候选题生成、PENDING 审核与质量评价 | `DONE_VERIFIED` |
| Vision 实现 | `DONE_VERIFIED` |
| 真实 DeepSeek | `PASS` |
| 真实 GLM | `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX` |
| 最终机器集成 | `AUTO_FINAL_VERIFICATION_PASS` |
| 最终用户人工验收 | `FINAL_MANUAL_ACCEPTANCE_PENDING` |

- 架构保持前后端分离的模块化单体，不使用微服务。
- Flyway 为 V1–V14，共 35 张业务表；V1–V14 均为已执行迁移，不得修改。
- 正式答案与 STANDARD 标准解析是权威事实，AI 不能覆盖。
- DeepSeek 负责文本推理，GLM-4.6V-Flash 只提供受控 `UNTRUSTED_VISION_CONTEXT`。
- 学生端统一显示“RIKE 理科学习助手”，不显示 Provider、模型代码、API 地址、Key 或 Token。
- PR #31 已完成全量自动化、Demo、真实 DeepSeek、权限与降级、机器浏览器和文档统一。用户人工验收尚未执行，不能写为最终封板 PASS。

## PR #31 机器验证

- 后端 `mvn clean test` 为 173 tests、0 failures、0 errors、3 skipped。两个真实 Provider 测试因全量阶段未注入 Key而按条件跳过，另一个是 Windows symbolic-link assumption；真实 Provider 另行单独执行。
- 前端全量为 58 files、190 tests、0 failures；type-check、build 和 `npm audit --omit=dev` 通过，audit 为 0 vulnerabilities。build 保留大于 500 kB chunk 的已知 warning。
- 随机临时 MySQL 完整迁移 V1–V14，验证 35 张业务表；`mvn -DskipTests package` 生成可执行 JAR。
- `rike_tiku_demo` 已完成 reset、seed、validate 和 smoke。三科各 120 道 Demo360、每科 6 道 Topic18，总计 378 道；PHYSICS-S1 图片和 hash 正常。
- 真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped。文本 1008 ms、22/11 Token；结构化 JSON 1722 ms、400/110 Token，Parser 与 V12 脱敏通过。
- Demo 真实学生错因分析为 `CONCEPT_ERROR`，2911 ms，五字段齐全；第二次读取 5 ms 命中缓存且没有新增收费调用。当前题真实答疑 3014 ms；身份、无关请求与修改 STANDARD 由代码 guard 控制。
- 真实 DeepSeek 生成 1 道候选，2318 ms，只进入 PENDING；机器审核验证 APPROVED 状态机，不计作用户人工质量评价。
- 真实 GLM 第一次窗口为 429；第二个最终窗口返回完整 JSON 代码围栏，旧 Parser 拒绝。Parser 已按严格完整围栏规则修复并通过全量自动化，遵守两次窗口上限未第三次调用，状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`。
- 权限专项确认学生所有权、教师跨科、三角色路由和学生私聊边界；未提交练习响应不含 correctAnswer 或 standardAnalysis。
- 机器浏览器覆盖 25 条路由，0 console errors、0 page errors、0 failed requests、0 horizontal overflow routes。证据见 [PR #31 最终机器证据](evidence/pr31-final/README.md)。
- 正式 `rike_tiku` 仅只读核对，当前为 V11、27 张业务表，没有 Demo 账号或 Demo 题；未迁移、未写入。最终验收只使用 `rike_tiku_demo`。

## PR #30 验证

主实现专项结果如下。

- 后端 54/54 PASS。
- 前端 12/12 PASS。
- V14 随机临时 MySQL 迁移 PASS。
- `mvn -DskipTests package` PASS。
- `npm run type-check` PASS。
- `npm run build` PASS。

集中修正结果如下。

- 后端 38/38 PASS。
- 前端 32/32 PASS。
- 候选 Provider、Parser 与 prepare 保持在事务外。
- 同批候选题创建、母题关联、DRAFT 至 PENDING、质量评价行和任务 SUCCESS 使用同一个 `TransactionTemplate` 事务。
- 确定性第二候选写入故障验证整批回滚。任务保留为 FAILED，生成数、质量行、关联候选与 PENDING 候选均为 0，母题和 STANDARD 不变。
- 教师端 `/teacher/ai-generation` 只调用教师 API，并继续受本人 ACTIVE 三元任课关系约束。
- package、type-check、build 与 `git diff --check` PASS。
- 未运行 PR #31 全量、Demo reset/seed、全站机器浏览器或用户人工验收。

PR #30 的真实 GLM 历史结果为 HTTP 429。本文件顶部的 PR #31 结果是后续事实，不覆盖这条历史记录。

## 当前业务能力

非 AI 主链已经覆盖认证、三角色权限、班级与任课、题库导入审核、附件、练习、自动判分、结果、错题、掌握度、规则推荐、高频考点、师生消息、个人中心、操作日志和 RIKE Aqua Future 三角色界面。

AI 主链已经覆盖统一 Provider、Fake/Stub、DeepSeek、脱敏调用日志、学生错因分析、当前题有限多轮答疑、管理员模型配置、GLM 视觉上下文、候选变式题生成、重复控制、PENDING、教师或管理员人工审核和五项质量评价。

更细的接口与数据边界见 [文档索引](README.md)。

## 历史阶段

### PR #29 学生 AI 学习主链

- ordinary merge commit 为 `d04e5dcf9639182303e26e38ccfa4351ad91c5d9`。
- V13 新增 `ai_cuo_ti_fen_xi`、`ai_hui_hua`、`ai_xiao_xi`。
- 错因分析绑定本人已提交正式答题事实；错题进入 REVIEWING 或 MASTERED 后仍查询最近一次错误正式事实。
- 分析使用八类受控错误和五字段严格 JSON，首次无效最多纠正一次。
- 当前题答疑最多 8 轮，单条用户消息 500 字，最近 12 条消息和 6000 字上下文预算。
- 真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped。文本调用 848 ms，输入/输出 Token 22/11；结构化分析 1670 ms，输入/输出 Token 400/132，JSON 调用 1 次；Parser、V12 日志脱敏、V13 SUCCESS 和 STANDARD 不变均通过。

### PR #28 Provider Core

- ordinary merge commit 为 `54c1669b3113086a2fb22e756e0656ea8cb751c8`。
- V12 新增 `ai_diao_yong_ri_zhi`。
- 已实现统一 Provider 契约、确定性 Fake、DeepSeek HTTP Provider、连接与请求超时、最多一次临时失败重试、受控错误和安全降级。
- 自动化不需要真实 Key，AI 默认关闭不影响非 AI 主链。

### PR #27 非 AI 最终封板

- ordinary merge commit 为 `84a82fc3bd4972fc11c0811d8332bae306b7e5c0`。
- MA-017 至 MA-026 已关闭，非 AI A 层为 `DONE_VERIFIED`。
- V11 新增管理员操作日志，Demo360、Topic18、附件、Golden30 导入闭环与 RIKE Aqua Future 的测试和验收记录均保留为历史证据。
- 原始审计过程和各轮机器、人工结论见 [V3.0 非 AI 完工审计](V3_NON_AI_COMPLETION_AUDIT.md)、[人工验收发现](MANUAL_ACCEPTANCE_FINDINGS.md) 与 [历史证据目录](evidence/)。

## 当前下一步

用户在 `http://localhost:18080` 按 [最终人工验收清单](FINAL_MANUAL_ACCEPTANCE_CHECKLIST.md) 完成一次真实 CAPTCHA 验收。用户确认前保持 `FINAL_MANUAL_ACCEPTANCE_PENDING`，Draft PR #31 不合并，也不创建 PR #32。
