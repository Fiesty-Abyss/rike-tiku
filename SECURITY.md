# 安全策略

## 支持的版本

| 版本 | 是否接收安全修复 |
|---|---|
| 1.0.x | ✅ |
| < 1.0 | ❌ |

## 报告漏洞

请**不要**在公开 Issue 中披露漏洞细节。请通过 GitHub 的 [私密安全通告](https://github.com/Fiesty-Abyss/rike-tiku/security/advisories/new) 提交，内容尽量包含：

- 受影响的版本 / 提交与组件（后端、前端、迁移脚本等）
- 复现步骤或概念验证
- 可能的影响范围

我们会在 7 天内确认收到，并在修复发布后致谢（如你愿意署名）。

## 部署安全提示

- `RIKE_TIKU_JWT_SECRET` 必须为不少于 32 字节的随机值，且不同环境不要复用
- 生产环境保持 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=false`（默认值）
- 演示数据（`demo_*` 账号，统一密码 `a1234567`）只用于本地演示库 `rike_tiku_demo`，不要部署到公网
- 首次部署后修改 `RIKE_TIKU_DEFAULT_RESET_PASSWORD`
- 管理员界面保存的 AI API Key 只用于本地或受信环境；生产环境建议通过环境变量或密钥管理服务注入
- 后端仅允许 `RIKE_TIKU_CORS_ALLOWED_ORIGINS` 中列出的前端来源，请按实际域名配置
