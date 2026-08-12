# 管理员学生管理 API

所有接口均要求当前角色为 `ADMIN`；`TEACHER`、`STUDENT` 与未完成首次改密的管理员不能调用。列表和详情响应不包含密码摘要、Token、逻辑删除字段或班级关系内部 ID。

## 接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/v1/admin/students` | 分页并按学号、姓名、用户名、班级、年级、账号状态、档案状态筛选 |
| GET | `/api/v1/admin/students/{id}` | 查询基本资料、STUDENT 角色、当前主班级和班级历史 |
| POST | `/api/v1/admin/students` | 事务创建用户、STUDENT 角色、学生档案和 ACTIVE 主班级 |
| PUT | `/api/v1/admin/students/{id}` | 修改姓名、年级、账号状态和档案状态 |
| POST | `/api/v1/admin/students/{id}/transfer` | 事务结束旧主班级关系并新增目标 ACTIVE 主班级 |
| POST | `/api/v1/admin/students/{id}/reset-password` | 单人恢复系统默认密码并启用首次改密 |
| POST | `/api/v1/admin/students/reset-passwords` | 批量恢复 1～100 个学生的系统默认密码 |

## 创建、编辑与调班

新增使用 `StudentCreateRequest`：`studentNumber`、`name`、`username`、`grade`、`classId`。新建和 Excel 导入仍由 `StudentInitialPasswordGenerator` 生成随机一次性密码；本轮的默认恢复策略不会改变创建流程。成功响应只在本次返回初始密码，并设置 `Cache-Control: no-store` 与 `Pragma: no-cache`；数据库只保存 BCrypt 摘要。

编辑使用 `StudentUpdateRequest`：`name`、`grade`、`accountStatus`、`profileStatus`。调班使用 `StudentTransferRequest.classId`，服务端要求当前恰有一个 ACTIVE 主班级，并在同一事务结束旧关系、创建新关系。

## 恢复默认密码

批量请求为：

```json
{
  "ids": [1, 2, 3]
}
```

- `ids` 必填，允许 1～100 个正整数；重复 ID 自动去重。
- 服务端先确认全部目标存在，再在单个事务中逐个生成独立 BCrypt 摘要；任一 ID 无效则整批回滚。
- 默认值为本地毕设配置 `app.account.default-reset-password`，可由 `RIKE_TIKU_DEFAULT_RESET_PASSWORD` 覆盖。当前默认值为 `a1234567`。
- 恢复只修改密码摘要、首次登录标志和密码修改时间，不修改账号状态、学生档案、班级或学习事实。
- 成功响应为 `resetCount`、仅本次返回的 `initialPassword` 与 `mustChangePassword=true`；响应禁止缓存。
- `RESET_PASSWORD` / `BATCH_RESET_PASSWORD` 操作日志只记录类型、数量和业务 ID，不记录默认密码、摘要或姓名。

忘记密码不提供匿名重置入口。用户联系管理员，由管理员在学生管理页面执行恢复；恢复后原密码失效，下次登录必须修改密码。

## 数据库边界

该能力复用 `yong_hu` 的 BCrypt 摘要、首次登录标志和既有操作日志，不新增迁移；Flyway 仍为 V1–V14。
