# 开发状态

更新时间：2026-08-12

## 当前状态

当前实现基线为 PR #30，代码已经通过第二次独立审查，获准 ordinary merge。生产代码审查 HEAD 为 `509f52ed08633801ec18cc2dce576a320357a08c`；合并前只允许整理 README、当前事实文档和仓库元数据。

| 范围 | 状态 |
| --- | --- |
| 非 AI 主链 | `DONE_VERIFIED` |
| Provider Core | `DONE_VERIFIED` |
| 学生错因分析与当前题答疑 | `DONE_VERIFIED` |
| 管理员 AI 模型配置 | `DONE_VERIFIED` |
| 候选题生成、PENDING 审核与质量评价 | `DONE_VERIFIED` |
| Vision 实现 | `DONE_VERIFIED` |
| 真实 DeepSeek | `PASS` |
| 真实 GLM | `REAL_GLM_VISION_SMOKE_FAIL_429` |
| 最终集成 | `PR #31 PENDING` |
| 最终用户人工验收 | `PR #31 PENDING` |

- 架构保持前后端分离的模块化单体，不使用微服务。
- Flyway 为 V1–V14，共 35 张业务表；V1–V14 均为已执行迁移，不得修改。
- 正式答案与 STANDARD 标准解析是权威事实，AI 不能覆盖。
- DeepSeek 负责文本推理，GLM-4.6V-Flash 只提供受控 `UNTRUSTED_VISION_CONTEXT`。
- 学生端统一显示“RIKE 理科学习助手”，不显示 Provider、模型代码、API 地址、Key 或 Token。
- 最终全量、Demo、机器浏览器、真实 DeepSeek 与 GLM 全链路及一次用户人工验收尚未执行，不能写为最终 PASS。

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

真实 GLM 单图请求已经到达官方 endpoint。Provider 按规则最多重试一次后仍返回 HTTP 429，状态保持 `REAL_GLM_VISION_SMOKE_FAIL_429`，没有记为 PASS，也没有继续重复请求。

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

## 下一阶段

唯一下一阶段为 PR #31 `chore/final-ai-integration-verification`。

范围是全量自动化回归、Flyway、Demo、DeepSeek、GLM、AI 配置后台、学生错因、当前题聊天、候选题生成审核、权限、降级、全站机器浏览器、一次最终用户人工验收，以及论文、README 与答辩口径统一。原则上不新增业务功能，不新增 V15；只处理集成缺陷、BLOCKER/HIGH 安全问题、测试和文档封板。
