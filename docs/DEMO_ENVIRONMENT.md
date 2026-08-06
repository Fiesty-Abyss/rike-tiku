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

启动服务时分别打开两个 PowerShell：

```powershell
.\scripts\demo-environment.ps1 backend
.\scripts\demo-environment.ps1 frontend
```

前端地址为 `http://localhost:18080`，后端地址为 `http://localhost:18081`，CORS 只允许此前端地址。

## 固定账号

| 角色 | 用户名 | 本地演示密码 |
|---|---|---|
| ADMIN | demo_admin | a1234567 |
| TEACHER | demo_teacher | a1234567 |
| STUDENT | demo_student | a1234567 |

三个账号均启用且不触发首次改密。数据库中只保存 BCrypt 摘要；密码仅为本地演示固定凭据，禁止用于任何正式环境。

## 固定数据

- 教师：`DEMO_T001`，演示教师。
- 学生：`DEMO_S001`，演示学生，高三。
- 班级：`DEMO_CLASS_01`，高三理综演示班。
- 任课：演示教师对该班的物理、化学、生物三条 ACTIVE 三元任课关系。
- 知识点：每科 3 个，共 9 个。
- 题库：每科 6 道，共 18 道；每科单选、多选、填空各 2 道，难度覆盖 1、2、3。

所有演示题均为自行编写的无附件 `PUBLISHED + ONLINE_PRACTICE` 自动判分题，STANDARD 解析为 `PUBLISHED`。QUESTION、ANSWER、STANDARD_ANALYSIS 三项来源均为 `TEACHER_CREATED + USER_PROVIDED`，并有 SUBMITTED、APPROVED 审核轨迹。

## 清理与重建

```powershell
.\scripts\demo-environment.ps1 clean
.\scripts\demo-environment.ps1 reset
.\scripts\demo-environment.ps1 seed
```

`clean` 后演示账号、组织、题目和学习记录均被删除；`reset` 是库级重建，只允许对通过安全检查的演示库名执行。MVP30 原始 Excel 和正式 `rike_tiku` 不参与上述流程。
