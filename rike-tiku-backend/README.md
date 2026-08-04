# rike-tiku-backend

本工程是项目的 Spring Boot 后端。当前已经具备数据库迁移、题库核心模型、账号与教学组织数据库模型、统一登录、JWT访问令牌、首次改密门禁、三角色鉴权、CORS和健康检查；尚未实现前端登录、用户导入、题库正式业务API或AI功能。

## 本地数据库配置

数据库密码不得写入 `application.yml`、`.env.example` 或其他受 Git 跟踪的文件。本项目的后端配置读取以下环境变量：

| 环境变量 | 默认值 | 是否通常需要设置 |
|---|---|---|
| `RIKE_TIKU_DB_HOST` | `localhost` | 否 |
| `RIKE_TIKU_DB_PORT` | `3306` | 否 |
| `RIKE_TIKU_DB_NAME` | `rike_tiku` | 否 |
| `RIKE_TIKU_DB_USERNAME` | `root` | 否 |
| `RIKE_TIKU_DB_PASSWORD` | 空 | 是 |
| `RIKE_TIKU_BACKEND_PORT` | `8081` | 否 |
| `RIKE_TIKU_CORS_ALLOWED_ORIGINS` | `http://localhost:8080` | 前端地址不同时设置 |
| `RIKE_TIKU_JWT_SECRET` | 空 | 是，至少32个UTF-8字节 |
| `RIKE_TIKU_JWT_EXPIRATION_SECONDS` | `7200` | 否 |

项目不读取 `RIKE_TIKU_DB_URL`。数据库 URL 由上述 host、port 和 name 组合生成。

`.env.example` 只是安全的配置项示例。当前项目没有引入 dotenv 依赖，Spring Boot 和 IDEA 都不会默认自动加载 `.env` 或 `.env.example`。不要只复制该文件后就直接启动后端。

## 在 IDEA 中启动

### 推荐：一次性设置Windows用户环境变量

在后端目录打开PowerShell，执行：

```powershell
.\scripts\setup-idea-local-env.ps1
```

脚本会隐藏数据库密码输入，设置 `RIKE_TIKU_DB_PASSWORD`，并在缺失时生成随机 `RIKE_TIKU_JWT_SECRET`。这些值只写入当前Windows用户环境，不会写入项目文件或Git。设置完成后必须完全退出并重新打开IDEA，因为已经运行的IDEA进程不会自动获得后来新增的用户环境变量。

重新打开IDEA后：

1. 使用Java 25导入Maven工程。
2. 选择已有运行配置 `RikeTikuBackendApplication`。
3. 确认 Main class 为：

   ```text
   com.neu.riketiku.RikeTikuBackendApplication
   ```

4. 点击Run。该配置默认继承IDEA进程的父环境变量，无须将密码保存到项目运行配置。

可以用以下PowerShell命令确认用户变量是否存在；命令只返回 `True` 或 `False`，不会显示密码：

```powershell
[bool][Environment]::GetEnvironmentVariable("RIKE_TIKU_DB_PASSWORD", "User")
[bool][Environment]::GetEnvironmentVariable("RIKE_TIKU_JWT_SECRET", "User")
```

### 备选：只保存到IDEA私有运行配置

如果不希望设置Windows用户环境变量，可以打开 `Run` → `Edit Configurations` → `RikeTikuBackendApplication`，在 `Environment variables` 中至少添加数据库密码和不少于32字节的随机JWT密钥：

   ```text
   RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
   RIKE_TIKU_JWT_SECRET=你的本机随机JWT密钥
   ```

如果本机数据库不是默认地址，再按实际情况添加 host、port、name 和 username。完整示例为：

   ```text
   RIKE_TIKU_DB_HOST=localhost
   RIKE_TIKU_DB_PORT=3306
   RIKE_TIKU_DB_NAME=rike_tiku
   RIKE_TIKU_DB_USERNAME=root
   RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
   RIKE_TIKU_JWT_SECRET=你的本机随机JWT密钥
   ```

保存运行配置并启动。IDEA的环境变量编辑器可以逐项填写；不要勾选 `Store as project file`，也不要把含真实密码的配置提交或共享。

项目没有提交共享 `.run` 密码配置，因为共享配置无法安全提供每台机器不同的真实密码。默认私有配置位于 `.idea/workspace.xml`，该目录已被Git忽略。

启动成功后访问：

```text
http://localhost:8081/api/v1/health
```

预期返回：

```json
{
  "status": "UP",
  "database": "UP"
}
```

## 从 PowerShell 启动

设置数据库密码和JWT密钥即可使用其他默认值：

```powershell
$env:RIKE_TIKU_DB_PASSWORD="你的本机MySQL密码"
$env:RIKE_TIKU_JWT_SECRET="不少于32字节的随机密钥"
mvn spring-boot:run
```

环境变量只对当前 PowerShell 进程及其子进程生效。关闭终端后需要重新设置。

## 常见问题

如果日志包含：

```text
Access denied for user 'root'@'localhost' (using password: NO)
```

其中 `using password: NO` 表示后端进程没有获得密码，并不表示数据库模型或 Flyway 迁移损坏。请检查当前 IDEA Run Configuration 是否设置了 `RIKE_TIKU_DB_PASSWORD`，修改后重新启动。

如果已经设置Windows用户环境变量，但仍显示 `using password: NO`，请确认IDEA是在设置变量之后完全重启的。仅关闭项目或重新点击Run不足以刷新IDEA进程继承的环境。

如果显示 `using password: YES` 但仍被拒绝，则说明进程拿到了密码，但用户名、密码或 MySQL 账号权限不正确，应核对本机数据库配置。

认证接口、JWT字段、首次改密和错误码详见 [认证接口说明](../docs/AUTHENTICATION_API.md)。
