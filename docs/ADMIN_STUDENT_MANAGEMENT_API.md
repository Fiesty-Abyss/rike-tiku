# 管理员学生管理 API

当前实现位于 PR #17 分支 `feat/admin-student-management`。所有接口均要求当前角色为 `ADMIN`；`TEACHER` 和 `STUDENT` 返回 403。响应不包含密码摘要、Token、逻辑删除字段或班级关系内部 ID。

## 接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/v1/admin/students` | 分页并按学号、姓名、用户名、班级、年级、账号状态、档案状态筛选 |
| GET | `/api/v1/admin/students/{id}` | 查询基本资料、STUDENT 角色、当前主班级和班级历史 |
| POST | `/api/v1/admin/students` | 事务创建用户、STUDENT 角色、学生档案和 ACTIVE 主班级 |
| PUT | `/api/v1/admin/students/{id}` | 修改姓名、年级、账号状态和档案状态 |
| POST | `/api/v1/admin/students/{id}/transfer` | 事务结束旧主班级关系并新增目标 ACTIVE 主班级 |
| POST | `/api/v1/admin/students/{id}/reset-password` | 生成新初始密码并设置首次改密 |

## 关键请求与响应

新增使用 `StudentCreateRequest`：`studentNumber`、`name`、`username`、`grade`、`classId`。成功响应只在本次返回 `initialPassword`，并设置 `Cache-Control: no-store`；数据库保存 BCrypt 摘要。重复学号、重复用户名、班级不存在或停用均拒绝，事务任一步失败整体回滚。

编辑使用 `StudentUpdateRequest`：`name`、`grade`、`accountStatus`、`profileStatus`。不能通过该接口修改用户 ID、密码摘要、角色或班级历史。

调班使用 `StudentTransferRequest`：`classId`。服务端要求当前恰有一个 ACTIVE 主班级；旧关系写入退出时间并变为 `EXITED`，随后新增目标关系。同班调班、无效班级及关系冲突均拒绝。

密码重置成功返回 `PasswordResetResponse.initialPassword`，同样只显示一次且禁止缓存；原密码立即失效，新密码首次登录进入既有初始密码修改流程。

实现继续使用简单 Request / Response，统一放在 `dto` 包；Entity 只对应数据库，没有新增 VO、Converter、Assembler 或空壳 Service 接口。

## 数据库边界

现有 V1–V7 已包含用户、角色、学生档案、班级学生加入/退出时间和 ACTIVE 主班级唯一约束，因此 PR #17 不新增迁移，不修改 V1–V7。
