# 个人中心 API

PR #22 为三种角色提供同一套本人个人中心 API。所有身份均从 JWT 获取，不接收 `userId`、`studentId` 或 `teacherId`。

## 接口

- `GET /api/v1/profile`：返回本人账号、全部真实角色、显示名称、可选学生档案、可选教师档案、简介和头像。
- `PUT /api/v1/profile`：仅更新 `introduction`，最大 500 字；前后空白会去除，空字符串表示清空。
- `POST /api/v1/profile/avatar`：multipart 字段 `file`，上传本人头像。
- `DELETE /api/v1/profile/avatar`：删除本人头像并恢复前端默认头像。

主动修改密码继续使用已有 `/api/v1/auth/change-password`，个人中心不建立第二套密码接口。

## 返回边界

账号响应包含用户名、状态、真实角色、首次登录状态、最近改密和最近登录时间。学生档案只返回学号、姓名、年级和当前主班级；教师档案只返回工号、姓名、职务和本人 ACTIVE 任课摘要。纯管理员没有业务档案时相应字段为 `null`。

响应不包含 BCrypt 摘要、JWT、逻辑删除字段或其他用户资料。活动前端角色只影响返回工作台的位置，不改变数据库角色事实。

## 头像规则

- 只接受 PNG、JPEG，最大 2 MB；
- 同时检查 multipart 非空、声明 MIME、文件魔数与 ImageIO 实际解析结果；
- MySQL `yong_hu.tou_xiang` 保存原始二进制，响应按实际 MIME 生成 Base64 data URL；
- 上传和删除只影响当前 JWT 用户。

V10 仅 ALTER `yong_hu`，V1–V9 未修改，业务表仍为 26 张。
