# rike-tiku-backend

本工程是项目的 Spring Boot 后端。当前已经具备数据库迁移、题库核心模型、账号与教学组织数据库模型、最小安全配置、CORS 和健康检查；尚未实现登录、JWT、正式业务 API 或 AI 功能。

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

项目不读取 `RIKE_TIKU_DB_URL`。数据库 URL 由上述 host、port 和 name 组合生成。

`.env.example` 只是安全的配置项示例。当前项目没有引入 dotenv 依赖，Spring Boot 和 IDEA 都不会默认自动加载 `.env` 或 `.env.example`。不要只复制该文件后就直接启动后端。

## 在 IDEA 中启动

1. 使用 Java 25 导入 Maven 工程。
2. 打开 `Run` → `Edit Configurations`。
3. 新建或选择运行配置 `RikeTikuBackendApplication`。
4. 确认 Main class 为：

   ```text
   com.neu.riketiku.RikeTikuBackendApplication
   ```

5. 在 `Environment variables` 中至少添加：

   ```text
   RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
   ```

6. 如果本机数据库不是默认地址，再按实际情况添加 host、port、name 和 username。完整示例为：

   ```text
   RIKE_TIKU_DB_HOST=localhost
   RIKE_TIKU_DB_PORT=3306
   RIKE_TIKU_DB_NAME=rike_tiku
   RIKE_TIKU_DB_USERNAME=root
   RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
   ```

7. 保存运行配置并启动。IDEA 的环境变量编辑器可以逐项填写；不要把真实密码保存到仓库文件或共享的运行配置中。

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

只设置密码即可使用其他默认值：

```powershell
$env:RIKE_TIKU_DB_PASSWORD="你的本机MySQL密码"
mvn spring-boot:run
```

环境变量只对当前 PowerShell 进程及其子进程生效。关闭终端后需要重新设置。

## 常见问题

如果日志包含：

```text
Access denied for user 'root'@'localhost' (using password: NO)
```

其中 `using password: NO` 表示后端进程没有获得密码，并不表示数据库模型或 Flyway 迁移损坏。请检查当前 IDEA Run Configuration 是否设置了 `RIKE_TIKU_DB_PASSWORD`，修改后重新启动。

如果显示 `using password: YES` 但仍被拒绝，则说明进程拿到了密码，但用户名、密码或 MySQL 账号权限不正确，应核对本机数据库配置。
