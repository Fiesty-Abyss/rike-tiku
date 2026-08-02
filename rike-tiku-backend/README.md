# rike-tiku-backend

本工程是项目的后端技术验证基线，只包含应用启动、数据库连接、最小安全配置、CORS 和健康检查，不包含正式业务功能或业务表。

## 本地配置

数据库密码不写入受 Git 跟踪的配置。启动前在 IDEA Run Configuration 中添加环境变量：

```text
RIKE_TIKU_DB_PASSWORD=your-local-password
```

其他可选变量见 `.env.example`。默认后端地址为 `http://localhost:8081`，健康接口为 `GET /api/v1/health`。

## 启动

在 IDEA 中选择 Java 25，导入 Maven 项目，运行：

```text
com.neu.riketiku.RikeTikuBackendApplication
```

也可以在已设置环境变量的终端运行：

```powershell
mvn spring-boot:run
```

