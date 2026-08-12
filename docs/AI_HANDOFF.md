# AI 开发交接

更新时间：2026-08-12

## CURRENT HANDOFF

- PR #30 已 ordinary merge，merge commit 为 `d67ebc83bf0b8a2fbd889290d5a0f78a27d7640e`。当前分支为 PR #31 `chore/final-ai-integration-verification`。
- 当前 Flyway 为 V1–V14，共 35 张业务表；没有 V15。
- 非 AI 主链、Provider Core、学生 AI、管理员 AI 配置、候选题生成与人工审核均为 `DONE_VERIFIED`。
- PR #31 机器阶段为 `AUTO_FINAL_VERIFICATION_PASS`。后端全量 173 tests、0 failures、0 errors、3 skipped；前端全量 58 files、190 tests，type-check、build、audit 通过。
- PR #31 真实 `deepseek-v4-flash` smoke 与 Demo 错因、当前题答疑、单题候选生成均为 PASS。
- GLM-4.6V-Flash 代码链、Stub 和最终 Parser 全量通过。真实第一次窗口为 429，第二个最终窗口暴露完整 JSON 代码围栏兼容；Parser 已严格修复但未发起第三次调用，状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`。
- Demo 已清理重建为 V1–V14、35 表、378 题，并启动真实 CAPTCHA 验收环境。下一唯一动作是用户按最终清单验收；当前为 `FINAL_MANUAL_ACCEPTANCE_PENDING`，PR #31 不合并。

## 当前 AI 架构

- DeepSeek V4 负责错因分析、当前题答疑与候选变式题生成。
- GLM-4.6V-Flash 只把题目图片转换为严格校验的 `UNTRUSTED_VISION_CONTEXT`。
- GLM 和 DeepSeek 都不能修改正式判分、正确答案或 STANDARD 标准解析。
- 学生只看到“RIKE 理科学习助手”，不看到 Provider、model、base URL、Key 或 Token。
- V12 调用日志只保存 provider、model、purpose、success/error、latency 和可空 Token 等安全元数据。
- 自动化使用 Fake/Stub 和随机临时 MySQL，不依赖真实 Key 或外网。

## PR #30 AI 配置、视觉、候选题与审核

- V14 新增 `ai_mo_xing_pei_zhi`、`ai_sheng_cheng_ren_wu`、`ai_hou_xuan_ti_zhi_liang_ping_jia`、`ai_shi_jue_shang_xia_wen`。
- 管理员 `/admin/ai-models` 管理 DeepSeek TEXT 与 GLM VISION 的模型、base URL、Key、启停、默认、超时、最多一次重试、Token 上限和连接测试。
- 运行时优先使用启用的数据库默认配置，没有可用数据库配置时回退 application/env。二者均不可用时受控降级。
- 本地毕设 Demo 模式允许 MySQL 保存 Key。API 只返回 `apiKeyConfigured`，前端只显示掩码；Key 不进入 Git、V12、异常或学生端。
- Vision 每题最多 2 张 PNG/JPEG，单图 3 MB，总量 6 MB，按 SHA-256 去重；输出正文不超过 1500 字，max tokens 1000，相同 question/附件集/provider/model/Prompt 版本优先复用缓存。
- 候选生成仅允许 ADMIN 或本人 ACTIVE 任教学科教师从 PUBLISHED 母题发起。单次 1 至 3 道，同母题 PENDING AI_GENERATED 加本次最多 6 道。
- request hash 绑定母题、题型、排序知识点、难度、变化方式和 Prompt 版本；内容 hash 拒绝精确重复，批内 hash 去重，trigram/Jaccard 大于等于 0.72 标记 `SUSPECTED_DUPLICATE`。
- 候选题复用 `ti_mu`，来源记录为 `AI_GENERATED`，设置 `fu_ti_mu_id` 并只进入 `PENDING`。填写五项 0/1 质量评价且人工 APPROVED 后才进入 PUBLISHED；REJECTED 回到 DRAFT 并保留任务与评价事实。
- Provider、Parser 与 prepare 位于事务外；一个候选批次的题目创建、母题关联、PENDING 转换、质量行与任务 SUCCESS 位于同一个 `TransactionTemplate`。后项失败时整批回滚，任务独立标记 FAILED。
- 教师端入口为 `/teacher/ai-generation`，只调用 `/teacher/ai-generation/**`，不读取全局 stats 或模型配置；后端按本人 ACTIVE `ren_ke_guan_xi` 限定学科。
- 主实现专项为后端 54/54、前端 12/12。集中修正专项为后端 38/38、前端 32/32。package、type-check、build 与 `git diff --check` 均通过。

## PR #29 学生 AI 学习主链

- V13 新增 `ai_cuo_ti_fen_xi`、`ai_hui_hua`、`ai_xiao_xi`。
- 错因分析唯一绑定本人 `xue_sheng_da_ti.id`。错题详情另行查询当前学生、当前题最近一次错误且已提交的正式答题事实，不相信最近一次可能已答对的记录。
- 分析输出使用八类受控错误和五字段严格 JSON，Prompt 版本为 `student-ai-v1`。首次 JSON 无效最多纠正一次；第二次仍无效则失败，不保存成功分析。
- 成功分析按正式答题事实、Prompt 版本和输入事实 SHA-256 复用。
- 会话同时绑定学生、答题事实和冻结练习题。最多 8 轮，用户消息 500 字，助手消息 2000 字，Provider 上下文最多最近 12 条并受 6000 字预算限制。
- studentId 从 JWT 推导，answerFactId 和 conversationId 均重新反查所有权。教师和管理员不能读取学生 AI 私聊正文。
- Provider、JSON 或 Vision 失败不影响练习结果、错题、掌握度、规则推荐与 STANDARD。
- 真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped。HTTP 2xx；文本 848 ms、22/11 Token；结构化分析 1670 ms、400/132 Token、1 次 JSON 调用；Parser、V12 脱敏、V13 SUCCESS 与 STANDARD 不变均通过。

## PR #28 Provider Core

- V12 新增 `ai_diao_yong_ri_zhi`。
- `AiModelProvider` 提供请求、结果、Token、状态和受控错误契约。
- 已有确定性 Fake、DeepSeek OpenAI-compatible HTTP Provider、连接与请求超时、最多一次有限重试和受控降级。
- 只有网络异常、超时、429 和部分 5xx 会重试；400、401、403、配置错误和缺 Key 不盲目重试。
- AI 默认关闭，应用和非 AI 主链不依赖 Provider 可用。

## 历史非 AI 基线

PR #27 ordinary merge commit 为 `84a82fc3bd4972fc11c0811d8332bae306b7e5c0`。MA-017 至 MA-026 已关闭，非 AI A 层为 `DONE_VERIFIED`。Demo360、Topic18、附件、Golden30、RIKE Aqua Future 以及各轮机器和人工验收属于历史证据，不应覆盖当前 Git、Flyway 和测试事实。

历史原始材料见 [V3.0 非 AI 完工审计](V3_NON_AI_COMPLETION_AUDIT.md)、[人工验收发现](MANUAL_ACCEPTANCE_FINDINGS.md) 和 [证据目录](evidence/)。

## 接管规则

- 事实优先级为代码与配置、Flyway、真实测试、Git、当前状态文档、设计文档、聊天总结。
- 不修改已经执行的迁移；数据库变化必须新增迁移。
- 不把计划或 skipped 写成 PASS。
- 不把 AI 候选直接发布，不让 AI 覆盖 STANDARD。
- 不为了最终封板引入微服务、Redis、MQ、RAG、向量数据库、WebSocket 或流式输出。
- PR #31 原则上不新增业务功能，不新增 V15；只处理最终集成、BLOCKER/HIGH、测试、Demo、浏览器和文档问题。
