# AI Provider 配置

PR #28 提供统一 `AiModelProvider` 基础层。AI 默认关闭；不配置 API Key 时应用仍可启动，题库、练习、判分、错题等非 AI 功能不依赖 Provider 可用。

## 环境变量

| 环境变量 | 安全默认值 | 说明 |
| --- | --- | --- |
| `RIKE_TIKU_AI_ENABLED` | `false` | 是否启用 AI Provider |
| `RIKE_TIKU_AI_PROVIDER` | `deepseek` | `deepseek` 或仅用于确定性测试的 `fake` |
| `RIKE_TIKU_AI_BASE_URL` | `https://api.deepseek.com` | OpenAI-compatible API 根地址 |
| `RIKE_TIKU_AI_MODEL` | `deepseek-v4-flash` | 模型代码；启用真实调用前复核官方文档 |
| `RIKE_TIKU_AI_API_KEY` | 空 | 只从环境变量读取，禁止写入仓库、数据库或日志 |
| `RIKE_TIKU_AI_CONNECT_TIMEOUT` | `3s` | HTTP 连接超时 |
| `RIKE_TIKU_AI_REQUEST_TIMEOUT` | `30s` | 单次 HTTP 请求超时 |
| `RIKE_TIKU_AI_RETRY_COUNT` | `1` | 有限重试；代码强制限制为 `0..1` |

真实 Provider 使用 `POST /chat/completions`，不启用 streaming、SSE 或 WebSocket。仅网络异常、超时、HTTP 429 和 5xx 可重试；400、401、403、缺 Key及配置错误不重试。

## 测试与降级

自动化使用 `FakeAiModelProvider` 或 JDK 本地 HTTP stub，不访问外网、不需要真实 Key。受控错误类型为 `DISABLED`、`CONFIGURATION_ERROR`、`AUTHENTICATION_ERROR`、`RATE_LIMITED`、`TIMEOUT`、`PROVIDER_UNAVAILABLE`、`INVALID_RESPONSE`、`UNKNOWN`。

V12 `ai_diao_yong_ri_zhi` 只保存 provider、model、用途、可空业务引用、成功状态、耗时、输入/输出 token、错误码和创建时间。它不保存 Prompt、模型输出、API Key、JWT、密码或完整题目。

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
