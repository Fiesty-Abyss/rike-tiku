# 本地演示验收环境

## 用途与安全边界

本工具只为本科毕业设计本机人工验收准备可反复重建的数据。演示数据不在 Flyway 中，不会随应用正常启动写入，也没有公开 seed 接口或免认证入口。

默认目标是 `rike_tiku_demo`。脚本和后端命令都会拒绝 `rike_tiku`、`mysql`、`information_schema`、`performance_schema`、`sys` 以及非法数据库名。数据库密码只从当前 PowerShell 的 `RIKE_TIKU_DB_PASSWORD` 读取，不写入仓库、不显示在日志中。

## 准备与执行

在仓库根目录打开 PowerShell：

```powershell
$env:RIKE_TIKU_DB_PASSWORD = "你的本机MySQL密码"
.\scripts\demo-environment.ps1 reset
.\scripts\demo-environment.ps1 seed
.\scripts\demo-environment.ps1 validate
```

- `create`：保留现有演示库，仅在不存在时创建并执行 V1–V7。
- `reset`：删除并重建指定演示库，再执行 V1–V7；会清除该演示库的全部已有内容。
- `seed`：清理旧演示数据后重建固定数据，重复执行结果稳定。
- `validate`：只读检查结构、账号、关系、题库和学习记录。
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

也可以分别打开两个 PowerShell，让脚本自动设置上述演示端口变量：

```powershell
.\scripts\demo-environment.ps1 backend
.\scripts\demo-environment.ps1 frontend
```

前端地址为 `http://localhost:18080`，后端地址为 `http://localhost:18081`，CORS 只允许此前端地址。

服务启动后，在第三个 PowerShell 执行真实 HTTP 烟雾检查：

```powershell
.\scripts\demo-environment.ps1 smoke
```

该操作检查后端健康接口、实际演示数据库、三个角色登录和错误角色入口。它不会输出 JWT、数据库密码或 JWT 密钥。

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
- 知识点：每科 3 个，共 9 个。
- 题库：Demo90 共 90 道，物理、化学、生物各 30 道；每科单选、多选、填空各 10 道，难度 1、2、3 各 10 道，三个演示知识点各 10 道。

所有 Demo90 题均为项目原创的“本科毕业设计自编演示题”，不复制网络题原文，也不读取 MVP30。题目均为无附件 `PUBLISHED + ONLINE_PRACTICE` 自动判分题，STANDARD 解析为 `PUBLISHED`。QUESTION、ANSWER、STANDARD_ANALYSIS 三项来源均为 `TEACHER_CREATED + USER_PROVIDED`，并有 SUBMITTED、APPROVED 审核轨迹。

Demo90 只服务独立演示环境，不等于 MVP30 正式真实题库。MVP30 仍未正式入库，仓库中的网络候选题也未因本轮扩充而变为 `PUBLISHED`。

## 清理与重建

```powershell
.\scripts\demo-environment.ps1 clean
.\scripts\demo-environment.ps1 reset
.\scripts\demo-environment.ps1 seed
```

`clean` 后演示账号、组织、题目和学习记录均被删除；`reset` 是库级重建，只允许对通过安全检查的演示库名执行。MVP30 原始 Excel 和正式 `rike_tiku` 不参与上述流程。
## PR #17 分支状态

当前分支为 `feat/admin-student-management`，PR #17 尚未合并。`reset`、`seed`、`validate`、`clean` 继续受数据库名保护；`seed` 可重复执行并会清理临时验收学生，固定场景数据保持稳定。正式 `rike_tiku` 不参与任何演示操作。
