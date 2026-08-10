# 本地演示验收环境

PR #27 当前分支新增 V11 管理员操作日志表；当前 Demo 结构目标为 Flyway V1–V11、27 张业务表。下文对 V1–V10 的历史记录保持原样，实际 `reset/create/validate` 由当前后端 `DemoDataService` 校验 V11/27。

## 用途与安全边界

本工具只为本科毕业设计本机人工验收准备可反复重建的数据。演示数据不在 Flyway 中，不会随应用正常启动写入，也没有公开 seed 接口或免认证入口。

默认目标是 `rike_tiku_demo`。脚本和后端命令都会拒绝 `rike_tiku`、`mysql`、`information_schema`、`performance_schema`、`sys` 以及非法数据库名。数据库密码只从当前 PowerShell 的 `RIKE_TIKU_DB_PASSWORD` 读取，不写入仓库、不显示在日志中。

## 准备与执行

在仓库根目录打开 PowerShell，一条命令完成 `reset → seed → validate`：

```powershell
$env:RIKE_TIKU_DB_PASSWORD = "你的本机MySQL密码"
.\scripts\demo-environment.ps1 acceptance-prepare
```

- `acceptance-prepare`：仅允许 `rike_tiku_demo`，依次重建、播种并校验最终人工验收数据。
- `final-acceptance`：`acceptance-prepare` 的兼容入口，并显示前后端启动命令。
- `create`：保留现有演示库，仅在不存在时创建并执行 V1–V11。
- `reset`：删除并重建指定演示库，再执行 V1–V11；会清除该演示库的全部已有内容。
- `seed`：清理旧演示数据后重建固定数据，重复执行结果稳定。
- `validate`：只读检查 V1–V11/27 张表、验收账号、关系、题库、PHYSICS-S1 图片文件/hash 和学习记录。
- `clean`：只删除带受控演示标识的数据，保留 Flyway 基础科目和样例。

## 启动方案 A：IDE 默认端口

> **重要：直接点击未配置数据库名的 IDEA 默认运行配置会连接 `rike_tiku`，无法使用 demo 账号。**

在 IDEA 的 `Run → Edit Configurations → RikeTikuBackendApplication → Environment variables` 保留原有数据库密码和 JWT 配置，并增加：

```text
RIKE_TIKU_DB_NAME=rike_tiku_demo
RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
RIKE_TIKU_JWT_SECRET=你原有的本机JWT密钥
```

不设置端口变量时，后端地址是 `http://localhost:8081`。WebStorm 前端使用默认 `http://localhost:8080`，其 `.env.local` 应为：

```text
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

API 地址必须包含 `/api/v1`。然后在 WebStorm 执行 `npm run dev`。

## 启动方案 B：演示端口

IDEA 环境变量：

```text
RIKE_TIKU_DB_NAME=rike_tiku_demo
RIKE_TIKU_BACKEND_PORT=18081
RIKE_TIKU_CORS_ALLOWED_ORIGINS=http://localhost:18080
RIKE_TIKU_DB_PASSWORD=你的本机MySQL密码
RIKE_TIKU_JWT_SECRET=你原有的本机JWT密钥
```

WebStorm 环境变量：

```text
VITE_API_BASE_URL=http://localhost:18081/api/v1
```

Vite 启动参数为 `--host localhost --port 18080`。

最终人工验收不使用开发服务器。在执行 `acceptance-prepare` 的原 PowerShell 中启动后端：

```powershell
.\scripts\demo-environment.ps1 acceptance-backend
```

另开 PowerShell 启动前端（该动作不读取数据库密码）：

```powershell
.\scripts\demo-environment.ps1 acceptance-frontend
```

前端地址为 `http://localhost:18080`，后端地址为 `http://localhost:18081`，CORS 只允许此前端地址。

`acceptance-backend` 明确设置 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=false`；浏览器必须识别页面当前图形验证码。`acceptance-frontend` 先用 `VITE_API_BASE_URL=http://localhost:18081/api/v1` 构建，再执行 `vite preview --host localhost --port 18080 --strictPort`，避免最终验收依赖 Vite dev server 的生命周期与 HMR 状态。

机器 smoke 如需读取 `testCode`，必须在单独 PowerShell 临时启动：

```powershell
.\scripts\demo-environment.ps1 smoke-backend
```

前端运行时，再在第三个 PowerShell 执行：

```powershell
.\scripts\demo-environment.ps1 smoke
```

`smoke-backend` 只供脚本机器 smoke，禁止用于人工浏览器验收。smoke 检查后端健康接口、实际演示数据库、三个角色登录和错误角色入口，不输出 JWT、数据库密码或 JWT 密钥。

## 固定账号

统一登录入口为 `http://localhost:8080/login`，演示端口方案为 `http://localhost:18080/login`。所有固定账号密码均为 `a1234567`，数据库只保存 BCrypt 摘要，且均不触发首次改密。

- 原 smoke 账号：`demo_admin`、`demo_teacher`、`demo_student`。
- 场景教师：`demo_physics_admin`（ADMIN + TEACHER）、`demo_biology_teacher`、`demo_chemistry_teacher`。
- 199 班学生：`demo_199_01` 至 `demo_199_05`。
- 200 班学生：`demo_200_01` 至 `demo_200_03`。

公开 seed 使用匿名教师和学生姓名，姓名不参与权限或唯一性判断。多角色账号认证后选择本次角色，服务端仍依据数据库角色和 `jiao_shi_id + ban_ji_id + ke_mu_id` 授权。

## 固定数据

- 班级：保留 `DEMO_CLASS_01`，新增 ACTIVE 的 `DEMO_CLASS_199 / 199班` 和 `DEMO_CLASS_200 / 200班`。
- 教师：保留 `DEMO_T001`；新增物理管理员、生物、化学三位场景教师。
- 学生：保留 `DEMO_S001`；199 班固定 5 名、200 班固定 3 名，每人只有 STUDENT 和一个 ACTIVE 主班级。
- 任课：保留原三条；新增物理、生物、化学教师各自对 199/200 的两条 ACTIVE 三元关系，共 9 条 ACTIVE。
- 知识点：覆盖 55 个叶子知识点，其中物理 18、化学 16、生物 21；完整矩阵见 `DEMO360_COVERAGE_MATRIX.xlsx`。
- 题库：确定性 Demo360，物理、化学、生物各 120 道；三科均覆盖单选、多选、填空和简单、中等、困难三档。`PHYSICS-S1` 额外关联一张确定性原创 PNG，同时用于题干 QUESTION 和 STANDARD_ANALYSIS 图片显示验收。正文使用 `〔图片对象 I001〕` / `〔图片对象 I002〕`，附件表使用 `I001` / `I002` 对象 ID。
- 高频考点：V8 的 `gao_pin_kao_dian` 仅绑定真实 `ren_ke_guan_xi_id`；199/200 班物理、化学、生物六条场景任课关系各预置 2 条，共 12 条 ACTIVE 自编纯文本考点。
- 私信：V9 的两张表不预置聊天内容；浏览器验收消息可由 `reset` 清理，固定账号和组织关系保持可重复 seed。
- 个人中心：V10 的简介和头像字段默认均为空，不在固定 seed 写入二进制头像；浏览器验收内容可由 `reset → seed` 完整清理。

Demo360 均为项目原创的“本科毕业设计自编演示题”，不复制网络题、高考真题或教材习题原文；除 `PHYSICS-S1` 的确定性 PNG 验收对象外，其余题目保持无附件。题目均为 `PUBLISHED + ONLINE_PRACTICE` 自动判分题，STANDARD 解析为 `PUBLISHED`。QUESTION、ANSWER、STANDARD_ANALYSIS 三项来源均为 `TEACHER_CREATED + USER_PROVIDED`，并有 SUBMITTED、APPROVED 审核轨迹。

最终 360 题只服务独立演示环境。V3.0 没有规定名为 MVP30 的 Excel 必须整体正式入库；MVP30 原始文件不修改，继续作为结构化导入能力验证素材，仓库中的网络候选题也未因本轮扩充而变为 `PUBLISHED`。

## 清理与重建

```powershell
.\scripts\demo-environment.ps1 clean
.\scripts\demo-environment.ps1 reset
.\scripts\demo-environment.ps1 seed
```

`clean` 后演示账号、组织、题目和学习记录均被删除；`reset` 是库级重建，只允许对通过安全检查的演示库名执行。MVP30 原始 Excel 和正式 `rike_tiku` 不参与上述流程。
## PR #27 分支状态

当前分支为 `feat/non-ai-final-closure`，Draft PR #27。Flyway 为 V1–V11、27 张业务表；`reset`、`seed`、`validate`、`clean` 和 `final-acceptance` 继续受数据库名保护。最终人工验收只使用 `rike_tiku_demo`，不向正式 `rike_tiku` 写入题目或其他演示业务数据。
