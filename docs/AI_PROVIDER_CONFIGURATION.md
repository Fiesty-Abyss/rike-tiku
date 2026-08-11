# AI Provider 配置

PR #28 提供统一 `AiModelProvider` 基础层。AI 默认关闭；不配置 API Key 时应用仍可启动，题库、练习、判分、错题等非 AI 功能不依赖 Provider 可用。

## 环境变量

| 环境变量 | 安全默认值 | 说明 |
| --- | --- | --- |
| `RIKE_TIKU_AI_ENABLED` | `false` | 是否启用 AI Provider |
| `RIKE_TIKU_AI_PROVIDER` | `deepseek` | `deepseek` 或仅用于确定性测试的 `fake` |
| `RIKE_TIKU_AI_BASE_URL` | `https://api.deepseek.com` | OpenAI-compatible API 根地址 |
| `RIKE_TIKU_AI_MODEL` | `deepseek-v4-flash` | 模型代码；启用真实调用前复核官方文档 |
| `RIKE_TIKU_AI_API_KEY` | 空 | 环境默认配置的 Key；禁止写入仓库或日志 |
| `RIKE_TIKU_AI_CONNECT_TIMEOUT` | `3s` | HTTP 连接超时 |
| `RIKE_TIKU_AI_REQUEST_TIMEOUT` | `30s` | 单次 HTTP 请求超时 |
| `RIKE_TIKU_AI_RETRY_COUNT` | `1` | 有限重试；代码强制限制为 `0..1` |

真实 Provider 使用 `POST /chat/completions`，不启用 streaming、SSE 或 WebSocket。仅网络异常、超时、HTTP 429 和 5xx 可重试；400、401、403、缺 Key及配置错误不重试。

## 测试与降级

自动化使用 `FakeAiModelProvider` 或 JDK 本地 HTTP stub，不访问外网、不需要真实 Key。受控错误类型为 `DISABLED`、`CONFIGURATION_ERROR`、`AUTHENTICATION_ERROR`、`RATE_LIMITED`、`TIMEOUT`、`PROVIDER_UNAVAILABLE`、`INVALID_RESPONSE`、`UNKNOWN`。

V12 `ai_diao_yong_ri_zhi` 只保存 provider、model、用途、可空业务引用、成功状态、耗时、输入/输出 token、错误码和创建时间。它不保存 Prompt、模型输出、API Key、JWT、密码或完整题目。

## PR #30 管理员数据库配置

管理员 `/admin/ai-models` 可以维护 TEXT/DEEPSEEK 和 VISION/GLM 配置。运行时优先读取当前启用且标记默认的数据库配置；没有可用数据库配置时回退本页环境/application 默认，二者均不可用时返回受控降级。每次业务调用最多一次索引查询，配置保存、切换或清除后立即生效，不使用 Redis。

`ai_mo_xing_pei_zhi.api_mi_yao` 仅为本地本科毕设演示便利而允许保存 Key，不代表生产级密钥管理。列表与详情 API 只返回 `apiKeyConfigured`，管理员页面只显示掩码；空输入不替换已有 Key，显式“清除 Key”才置空。Key 不进入前端缓存、异常、V12 日志、调用正文或 Git。

当前受控组合：

- TEXT：`DEEPSEEK`，模型 `deepseek-v4-flash` 或 `deepseek-v4-pro`，默认 `deepseek-v4-flash`。
- VISION：`GLM`，只允许 `glm-4.6v-flash`，默认 Base URL `https://open.bigmodel.cn/api/paas/v4`。
- GLM 请求使用 `/chat/completions`、image data URI、`thinking={"type":"disabled"}` 和 max tokens 1000；只返回严格五字段视觉 JSON。
- `RIKE_TIKU_VISION_ENABLED`、`RIKE_TIKU_GLM_BASE_URL`、`RIKE_TIKU_GLM_MODEL`、`RIKE_TIKU_GLM_API_KEY`、`RIKE_TIKU_GLM_REQUEST_TIMEOUT`、`RIKE_TIKU_GLM_RETRY_COUNT`、`RIKE_TIKU_GLM_MAX_TOKENS` 提供无数据库配置时的安全回退；视觉默认关闭。

2026-08-11 已按官方文档复核：DeepSeek Chat Completions 为 `https://api.deepseek.com/chat/completions`，项目受控模型为 `deepseek-v4-flash`/`deepseek-v4-pro`；智谱 GLM-4.6V-Flash 使用 `https://open.bigmodel.cn/api/paas/v4/chat/completions`，支持 image URL/Base64 输入和文本输出。模型 ID 集中配置，不散落在学生响应中。

## PR #29 学生 AI 请求策略

- 错因分析和当前题答疑都显式发送 `thinking={"type":"disabled"}`，不保存或返回 `reasoning_content`。
- 错因分析使用 `response_format={"type":"json_object"}` 与 `max_tokens=1200`；Prompt 内明确包含 json 及目标结构示例。
- 首次 JSON 无效时业务层最多纠正一次；这与 Provider Core 的网络/429/5xx 最多一次 HTTP retry 相互独立，不存在控制器重试。
- 学生 AI 仍服从本页 `enabled/provider/base-url/model/api-key/connect-timeout/request-timeout/retry-count` 配置。默认关闭或 Key 缺失不会影响非 AI 启动、练习、判分、错题与 STANDARD 解析。

## 最小真实 Provider smoke

真实 smoke 不属于默认自动化，只能在同一 PowerShell 临时设置轮换后的 `RIKE_TIKU_AI_API_KEY`、`RIKE_TIKU_AI_ENABLED=true`、`RIKE_TIKU_AI_PROVIDER=deepseek` 和 `RIKE_TIKU_AI_MODEL=deepseek-v4-flash` 后执行：

```powershell
cd rike-tiku-backend
mvn "-Dtest=RealDeepSeekSmokeTest" test
```

测试只报告模型、HTTP 结果、耗时、token、Parser 与日志脱敏结果，不输出 Key；环境变量缺失时以 assumption 跳过。测试完成后应立即从当前 PowerShell 删除上述四个临时环境变量。

2026-08-11 真实门禁结果：`deepseek-v4-flash`、HTTP 2xx；文本 848 ms（22/11 token），结构化分析 1670 ms（400/132 token）、1 次 JSON 调用；Parser 与日志脱敏均 PASS。真实 Key 未记录。

## 最小真实 GLM Vision smoke

`RealGlmVisionSmokeTest` 仅在当前进程存在 `RIKE_TIKU_GLM_API_KEY` 时启用，使用程序生成的无隐私 PNG、随机临时 MySQL 和真实 `glm-4.6v-flash`。默认自动化无 Key 时跳过，不访问外网。

2026-08-11 实际结果：请求到达官方接口；按 transient 规则最多一次重试后仍为 HTTP 429，记录为 `REAL_GLM_VISION_SMOKE_FAIL_429`。Parser 与成功日志没有被伪造为 PASS，未继续重复调用；Key 未输出、未写文件、未写数据库、未写日志、未进入 Git。PR #31 再做最终真实 DeepSeek + GLM 全链路。
