# AI 最终实验事实

更新时间：2026-08-14

本文只记录 PR #31 已真实执行的数据，不推导 AI 准确率、用户满意度或虚构平均质量分。

## PR #33 增量说明

PR #31 的真实调用数据继续作为历史实验事实，不外推到当前 Schema V2、新颖度、GLM/xAI、深度思考或搜索。PR #33 自动化使用 Fake/Mock HTTP 验证请求映射、字段级 Parser、一次修复、新颖度、429/超时/非法 URL、确定性判分和原子回滚，不把 Mock 写成真实 Provider PASS。当前没有可安全使用的轮换后凭据，DeepSeek variant、DeepSeek tutor、GLM Vision、xAI Vision 与 Web Search 分别记为 `BLOCKED_EXTERNAL_PROVIDER`。

GLM/xAI 管理端按 HTTP 400、401/403、429、5xx、TIMEOUT、INVALID_RESPONSE 与 CONFIGURATION_ERROR 安全分类，并只显示状态、延迟、时间和安全错误码。完整请求、Key、Authorization、Base64、Prompt、reasoning_content 与原始响应均不回显；Provider 由管理员显式选择，不隐式自动切换。

## DeepSeek 真实结果

模型：`deepseek-v4-flash`。官方 OpenAI-compatible endpoint 返回 HTTP 2xx。

| 场景 | 结果 | 延迟 | 输入 Token | 输出 Token |
| --- | --- | ---: | ---: | ---: |
| Provider 普通文本 smoke | PASS | 1008 ms | 22 | 11 |
| 错因严格 JSON smoke | PASS | 1722 ms | 400 | 110 |
| Demo 学生错因分析 | SUCCESS / `CONCEPT_ERROR` | 2911 ms | 由 V12 记录 | 由 V12 记录 |
| Demo 当前题正常答疑 | PASS | 3014 ms | 由 V12 记录 | 由 V12 记录 |
| Demo 候选题生成 1 道 | SUCCESS | 2318 ms | 由 V12 记录 | 由 V12 记录 |
| 管理员连接测试 | PASS | 707 ms | 未向管理员返回 | 未向管理员返回 |

结构化 smoke 只发起 1 次 JSON 调用，Parser PASS。Demo 错因五字段齐全，第二次读取命中 SUCCESS cache（5 ms），`STUDENT_ERROR_ANALYSIS` 日志只新增 1 次。正常答疑之外的身份、模型、闲聊和修改 STANDARD 输入由代码 guard 处理，没有额外 Provider 调用。

## 候选题事实

真实 DeepSeek 从 PUBLISHED 纯文本母题生成 1 道候选题。任务为 SUCCESS，候选初始状态为 PENDING，来源为 `AI_GENERATED`，`fu_ti_mu_id` 正确，request hash 存在，候选答案与解析非空。随后执行的 APPROVED 仅用于机器验证状态机，不代表用户人工质量评价；母题答案与 STANDARD hash 未变化。

## GLM Vision

模型为 `glm-4.6v-flash`。PR #31 第一次真实窗口到达官方 endpoint，Provider 按规则最多重试一次后仍为 HTTP 429。第二个也是最后一个受控窗口收到模型内容，但内容是完整的 Markdown `json` 代码围栏，原 Parser 将其判定为无效响应。

本轮已修复 Parser，仅在整个响应恰好是一个完整 JSON 代码围栏时剥离围栏，围栏外存在任何文字、额外字段或不合法结构仍拒绝。修复后的 Parser、GLM Provider、Vision cache 与候选生成专项以及 173 项后端全量均通过。遵守本轮两次真实窗口上限，没有发起第三次请求，因此真实状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`，不是 PASS，也不能继续沿用“最终 429”描述。

本地 Stub/Mock 视觉专项覆盖图片数量、格式、大小、SHA 去重、缓存、429、5xx、timeout、401、无 Key、invalid JSON、最多一次重试和安全降级。新增的围栏 Parser 3/3 专项通过。GLM 不可用时，图片必要的候选任务为 FAILED，generatedCount=0；STANDARD 与文本题能力不受影响。

## 安全与日志

V12 Demo 日志只有 provider、model、purpose、业务关联、success/error、latency、input/output token 和时间等安全元数据；不存在 Prompt、输出、图片 Base64、Key 或 JWT 列。PR #31 真实调用后 V12 共 4 条安全元数据记录。学生响应不显示 DeepSeek、GLM、model id、API URL、Key 或 Token。

`FINAL_MANUAL_ACCEPTANCE_PENDING`：五项质量评价只有用户真正完成候选审核后才可进入论文人工评价统计。
