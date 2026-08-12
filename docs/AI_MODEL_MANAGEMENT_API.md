# 管理员 AI 模型管理 API

全部接口仅允许 ADMIN，统一前缀为 `/api/v1/admin/ai-models`。当前管理 TEXT/DEEPSEEK 与 VISION/GLM 两类配置。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/` | 列出当前模型配置 |
| `POST` | `/` | 新增模型配置 |
| `PUT` | `/{id}` | 更新配置或替换 Key |
| `DELETE` | `/{id}/api-key` | 明确清除 Key |
| `POST` | `/{id}/test` | 使用该配置执行最小连接测试 |

## 保存字段

```json
{
  "provider": "DEEPSEEK",
  "model": "deepseek-v4-flash",
  "baseUrl": "https://api.deepseek.com",
  "apiKey": "仅在新增或替换时提交",
  "usage": "TEXT",
  "enabled": true,
  "defaultConfig": true,
  "timeoutMillis": 30000,
  "maxTokens": 1200,
  "retryCount": 1
}
```

`timeoutMillis` 为 1000 至 120000，`maxTokens` 为 64 至 8192，`retryCount` 只能是 0 或 1。受控组合为 DEEPSEEK/TEXT 与 GLM/VISION；GLM 视觉模型只使用 `glm-4.6v-flash`。

## Key 掩码

列表、创建和更新响应都不返回 Key，只返回 `apiKeyConfigured`。页面使用掩码表示已配置；更新时 `apiKey` 为空不会替换原 Key，清除必须调用独立 DELETE 接口。前端不持久缓存完整 Key。

本地本科毕设 Demo 模式允许 `ai_mo_xing_pei_zhi.api_mi_yao` 保存 Key。这不代表生产级 KMS。Key 不得进入 Git、V12 调用日志、异常、连接测试响应或学生端。

## 连接测试

连接测试响应包含 `success`、`provider`、`model`、`latencyMillis`、`status`、可空的 `visionSummaryPreview` 和 `safeError`。TEXT 测试不返回模型正文；VISION 只返回受控摘要预览。测试结果及最近测试时间保存在配置元数据中。

运行时优先读取当前启用的数据库默认配置；没有可用数据库配置时回退 application/env，二者均不可用时受控降级。
