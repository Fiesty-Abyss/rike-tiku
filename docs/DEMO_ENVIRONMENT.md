# 本地演示环境

演示环境使用独立数据库 `rike_tiku_demo`，与正式库 `rike_tiku` 严格分离。演示数据**不在 Flyway 中**：应用正常启动不会写入任何演示数据，也没有公开的 seed 接口；只有显式执行下面的命令才会初始化。

## 安全边界

- 默认目标库为 `rike_tiku_demo`；脚本和后端命令都会拒绝 `rike_tiku`、`mysql`、`information_schema`、`performance_schema`、`sys` 以及非法库名。
- 数据库密码只从环境变量 `RIKE_TIKU_DB_PASSWORD` 读取，不写入仓库、不打印到日志。
- 所有演示账号初始密码为 `a1234567`（数据库只保存 BCrypt 摘要），首次登录需修改。**请勿在公网环境使用演示数据。**

## 一键准备（Windows PowerShell）

```powershell
$env:RIKE_TIKU_DB_PASSWORD = "你的本机MySQL密码"
.\scripts\demo-environment.ps1 acceptance-prepare   # reset → seed → validate
.\scripts\demo-environment.ps1 acceptance-backend   # 后端 http://localhost:18081
.\scripts\demo-environment.ps1 acceptance-frontend  # 构建并预览前端 http://localhost:18080
```

其他动作：

| 动作 | 说明 |
|---|---|
| `create` | 演示库不存在时创建并执行全部迁移 |
| `reset` | 删除并重建演示库，再执行全部迁移（清除该库全部内容） |
| `seed` | 清理旧演示数据后写入固定数据，可重复执行 |
| `validate` | 只读校验迁移版本、表数量、账号、关系、题库与附件 |
| `clean` | 只删除带演示标识的数据，保留 Flyway 基础数据 |
| `smoke-backend` / `smoke` | 临时暴露验证码测试值，供脚本冒烟测试使用 |

## 跨平台方式（Linux / macOS / Windows）

先创建空库并构建后端：

```bash
mysql -uroot -p -e "CREATE DATABASE rike_tiku_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
cd rike-tiku-backend && mvn -DskipTests package
```

然后以 `DemoEnvironmentCommand` 为入口执行迁移与初始化（`demo.action` 可取 `migrate` / `seed` / `validate` / `clean`）：

```bash
export RIKE_TIKU_DB_NAME=rike_tiku_demo RIKE_TIKU_DB_PASSWORD='你的本机MySQL密码' \
       RIKE_TIKU_JWT_SECRET="$(openssl rand -hex 32)"
java -Dloader.main=com.riketiku.demo.DemoEnvironmentCommand \
     -cp target/rike-tiku-backend-*.jar org.springframework.boot.loader.launch.PropertiesLauncher \
     --demo.action=seed --server.port=0
```

最后按普通方式启动后端与前端（端口可按需调整，前端地址需与 CORS 设置一致）：

```bash
RIKE_TIKU_BACKEND_PORT=18081 RIKE_TIKU_CORS_ALLOWED_ORIGINS=http://localhost:18080 java -jar target/rike-tiku-backend-*.jar
cd ../rike-tiku-frontend && VITE_API_BASE_URL=http://localhost:18081/api/v1 npm run dev -- --port 18080
```

## 固定账号

登录入口：`http://localhost:18080/login`（或你配置的前端地址）。所有账号的初始密码均为 `a1234567`；由于它等于系统配置的默认密码，**首次登录会要求先修改密码**（之后用新密码登录，重新执行 `seed` 会恢复初始密码）。

| 账号 | 角色 | 说明 |
|---|---|---|
| `demo_admin` | 管理员 | 系统管理、题库审核、AI 模型配置、操作日志 |
| `demo_physics_admin` | 管理员 + 教师 | 双角色账号，登录时选择本次角色；任教 199 / 200 班物理 |
| `demo_teacher` | 教师 | 理综演示教师 |
| `demo_biology_teacher`、`demo_chemistry_teacher` | 教师 | 分别任教 199 / 200 班生物、化学 |
| `demo_student` | 学生 | 演示学生 |
| `demo_199_01` ~ `demo_199_05` | 学生 | 199 班 |
| `demo_200_01` ~ `demo_200_03` | 学生 | 200 班 |

演示数据中的教师、学生均使用匿名称呼（如“物理教师”“199班学生01”）；授权始终由服务端依据数据库角色与 `教师 + 班级 + 科目` 三元任课关系判断。

## 固定数据

- 班级：`DEMO_CLASS_01`、`DEMO_CLASS_199 / 199班`、`DEMO_CLASS_200 / 200班`
- 任课：物理、生物、化学教师分别对 199 / 200 班建立任课关系
- 知识点：55 个叶子知识点（物理 18、化学 16、生物 21）
- 题库：物理、化学、生物各 120 道客观题（单选 / 多选 / 填空 × 简单 / 中等 / 困难），全部为项目自编并带 STANDARD 解析；另有 15 个专题单元的主观综合题
- 高频考点：每条场景任课关系预置 2 条自编考点
- 私信、个人简介与头像默认为空，演示过程中产生的数据可通过 `reset → seed` 完整清理

题目来源记录为 `TEACHER_CREATED + USER_PROVIDED`（“RIKE 项目自编演示题”），并带 SUBMITTED、APPROVED 审核轨迹。内容来源说明见 [CONTENT_NOTICE](../CONTENT_NOTICE.md)。
