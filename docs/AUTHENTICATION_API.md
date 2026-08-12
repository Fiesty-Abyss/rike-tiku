# 后端认证接口说明

> 本文包含历史分支补充记录，当前项目状态请查看 [README](../README.md) 和 [开发状态](DEVELOPMENT_STATUS.md)。

更新时间：2026-08-08

## 1. 范围

本轮只实现统一登录、JWT访问令牌、当前用户、首次登录改密和三角色鉴权验证。没有实现自由注册、Refresh Token、Token表、找回密码、用户CRUD或前端登录页。

## 2. 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `RIKE_TIKU_JWT_SECRET` | 是 | 无 | HS256密钥，至少32个UTF-8字节 |
| `RIKE_TIKU_JWT_EXPIRATION_SECONDS` | 否 | `7200` | 访问Token有效期，单位秒 |
| `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE` | 否 | `false` | 仅本地自动化/Demo smoke 使用；正式运行必须保持关闭 |

本机可执行 `rike-tiku-backend/scripts/setup-idea-local-env.ps1` 自动生成随机JWT密钥。真实密钥不得提交到Git。

## 3. 接口

| 方法 | 路径 | 匿名 | 权限/说明 |
|---|---|---:|---|
| GET | `/api/v1/health` | 是 | 真实数据库健康检查 |
| POST | `/api/v1/auth/login` | 是 | 统一登录；`expectedRole`只校验入口 |
| GET | `/api/v1/auth/captcha-challenge` | 是 | 获取两分钟有效的一次性 4 位图形验证码 |
| GET | `/api/v1/auth/me` | 否 | 当前真实用户、有效角色和可选档案显示信息 |
| POST | `/api/v1/auth/change-initial-password` | 否 | 仅修改当前登录用户的初始密码 |
| GET | `/api/v1/test/student` | 否 | 技术验证接口，需要 `ROLE_STUDENT` |
| GET | `/api/v1/test/teacher` | 否 | 技术验证接口，需要 `ROLE_TEACHER` |
| GET | `/api/v1/test/admin` | 否 | 技术验证接口，需要 `ROLE_ADMIN` |
| GET | `/v3/api-docs/**` | 是 | 仅开发阶段公开的OpenAPI文档 |

除表中匿名接口外，其他接口默认需要认证。

### 3.1 登录

请求：

```json
{
  "username": "test-user",
  "password": "test-password",
  "expectedRole": "STUDENT",
  "challengeId": "一次性挑战ID",
  "captchaCode": "AB7K"
}
```

响应：

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "mustChangePassword": true,
  "user": {
    "id": 1,
    "username": "test-user",
    "roles": ["STUDENT"]
  }
}
```

角色只从 `yong_hu_jiao_se` 与有效 `jiao_se` 查询。客户端提交 `ADMIN` 不能创建或授予管理员角色。登录成功会在同一事务中更新 `zui_hou_deng_lu_shi_jian`；更新失败则不签发Token。

### 3.2 图形验证码

`GET /api/v1/auth/captcha-challenge` 返回 `challengeId`、Base64 PNG data URL `image` 和 `expiresAt`。可选查询参数 `previousChallengeId` 用于刷新时立即废弃旧 challenge。验证码使用不含 `0/O/1/I/L` 的字符集，大小写不敏感；challenge 只在进程内存中保存正确值和到期时间，不保存账号、密码或 JWT。

验证码错误、过期或校验成功都会消费 challenge，重放会被拒绝。错误码为 `CAPTCHA_CHALLENGE_REQUIRED`、`CAPTCHA_INCORRECT`、`CAPTCHA_CHALLENGE_EXPIRED`、`CAPTCHA_CHALLENGE_REUSED`。

自动化测试与 `rike_tiku_demo` smoke 可显式设置 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=true`，响应才额外包含 `testCode`。该字段只用于无 OCR 的本地自动化，默认值为 `false`，正式运行不存在无条件免验证码登录入口。

### 3.3 当前用户

请求头：

```text
Authorization: Bearer <accessToken>
```

响应包含用户ID、用户名、当前数据库有效角色、首次改密标志，以及可选学生姓名/学号或教师姓名/工号。档案不存在不影响角色判断。

### 3.4 修改初始密码

请求：

```json
{
  "oldPassword": "...",
  "newPassword": "...",
  "confirmPassword": "..."
}
```

密码规则：8至64位，同时包含字母和数字，不能是纯空格，不能与旧密码相同。成功后使用BCrypt保存新摘要，更新 `mi_ma_xiu_gai_shi_jian`，关闭 `shi_fou_shou_ci_deng_lu`，并返回一枚 `mustChangePassword=false` 的新Token。

## 4. JWT设计

- 库：JJWT 0.13.0。
- 算法：HS256。
- 标准字段：`sub`、`iat`、`exp`。
- 自定义字段：`uid`、`roles`、`mustChangePassword`。
- 默认有效期：7200秒，可配置。
- 不包含密码、姓名、学号、工号或其他隐私。
- 不实现Refresh Token，不创建Token数据库表。

本轮没有实现Token吊销列表。账号或角色状态在Token有效期内发生变化时，旧Token不会主动吊销；当前用户接口会重新查询数据库状态。该边界与首版短期访问Token设计一致，后续如确需即时吊销再单独设计。

## 5. 首次改密门禁

`mustChangePassword=true` 的用户可以获得Token，但后端只允许访问：

- `GET /api/v1/auth/me`
- `POST /api/v1/auth/change-initial-password`
- `GET /api/v1/health`

访问其他受保护接口返回403和 `MUST_CHANGE_PASSWORD`，不能依赖前端绕过。

## 6. 错误边界

错误响应统一包含 `code`、`message`、`timestamp`，不包含密码、完整Token、密钥、SQL、堆栈或内部类名。主要错误码包括：

- `INVALID_CREDENTIALS`
- `ACCOUNT_DISABLED`
- `ACCOUNT_LOCKED`
- `ACCOUNT_HAS_NO_ROLE`
- `ROLE_MISMATCH`
- `UNAUTHENTICATED`
- `TOKEN_INVALID`
- `TOKEN_EXPIRED`
- `ACCESS_DENIED`
- `MUST_CHANGE_PASSWORD`
- `OLD_PASSWORD_INCORRECT`
- `PASSWORD_CONFIRMATION_MISMATCH`
- `PASSWORD_POLICY_VIOLATION`
- `PASSWORD_UNCHANGED`

# 当前分支补充（未合并）

当前分支 `feat/login-image-captcha` 删除历史 `GET /api/v1/auth/slider-challenge`，登录请求中的 `sliderOffset` 替换为 `captchaCode`。PR #15 滑块仅保留为历史说明，当前认证主链只使用随机图形验证码。

新增认证后 `POST /api/v1/auth/change-password`，请求为 `oldPassword`、`newPassword`、`confirmPassword`。复用 8–64 位、字母数字、非空白、不可与旧密码相同的策略，使用 BCrypt 保存并重新签发 Token；首次改密门禁保持不变。
