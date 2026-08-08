# 师生私信 API

更新时间：2026-08-08

## 权限模型

私信只允许当前登录的教师和学生使用。教师侧由有效教师档案及本人 ACTIVE `ren_ke_guan_xi` 推导班级和科目；学生侧由有效学生档案、当前 ACTIVE 主班级和对应 ACTIVE 任课关系推导可联系教师。请求不接收 `teacherId`、`senderId` 或任意用户 ID，发送人始终取自 JWT。

同一 `ren_ke_guan_xi_id + xue_sheng_id` 只有一个有效会话。任课关系停用或学生调班后，历史消息仍可读取，但双方不能继续发送；管理员角色不因此获得私信正文读取权限。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/messages/contacts` | 返回当前用户可联系的真实教师或学生 |
| GET | `/api/v1/messages/conversations` | 会话列表、最近消息和未读数量 |
| POST | `/api/v1/messages/conversations` | 获取或创建会话；教师提交 `teachingAssignmentId + studentId`，学生只提交 `teachingAssignmentId` |
| GET | `/api/v1/messages/conversations/{id}/messages` | 参与者读取按时间正序排列的消息 |
| POST | `/api/v1/messages/conversations/{id}/messages` | 当前用户发送 1～1000 字纯文本 |
| POST | `/api/v1/messages/conversations/{id}/read` | 将对方发送给当前用户的未读消息标为已读 |

常见结果：无权限为 `403`，会话不存在为 `404`，当前教学关系已失效时发送返回 `409`，空消息或超长消息返回 `400`。

## 数据库

V9 新增 `si_xin_hui_hua` 和 `si_xin_xiao_xi`。两表均保留历史、使用外键和状态约束，不支持图片、文件、撤回、群聊或管理员审计。
