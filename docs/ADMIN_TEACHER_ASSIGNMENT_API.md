# 管理员教师与三元任课关系接口

所有接口均位于`/api/v1/admin`且只允许`ROLE_ADMIN`。未登录为401，学生、教师以及首次未改密管理员均由服务端返回403；前端菜单不是授权依据。

## 教师

- `GET /teachers?page=&size=&employeeNumber=&name=&username=&accountStatus=&profileStatus=`：教师分页与筛选；仅返回工号、姓名、展示职务、用户名、账号和档案状态。
- `GET /teachers/{id}`：教师公开档案、当前有效角色和全部当前/历史任课关系。
- `POST /teachers`：请求字段为`employeeNumber`、`name`、`username`、可选`displayPosition`、可选`initialPassword`和`accountStatus`。单事务写入`yong_hu`、`yong_hu_jiao_se`、`jiao_shi_dang_an`，后端固定绑定有效`TEACHER`角色。响应中的`initialPassword`仅返回一次，响应头为`Cache-Control: no-store`与`Pragma: no-cache`。
- `PUT /teachers/{id}`：只接受`name`、`displayPosition`、`accountStatus`、`profileStatus`；不接受工号、用户名、密码摘要或角色集合。
- `POST /teachers/{id}/reset-password`：单人恢复系统默认密码，原密码立即失效，下次登录必须改密。
- `POST /teachers/reset-passwords`：请求 `{"ids":[1,2,3]}`，一次批量恢复 1～100 个教师；重复 ID 自动去重，任一目标不存在则整批回滚。

错误码包括`TEACHER_NUMBER_EXISTS`、`USERNAME_EXISTS`、`TEACHER_ROLE_UNAVAILABLE`、`TEACHER_NOT_FOUND`和`PASSWORD_POLICY_VIOLATION`。不返回密码摘要、逻辑删除字段、审计字段或令牌。

恢复默认密码读取 `app.account.default-reset-password`（可由 `RIKE_TIKU_DEFAULT_RESET_PASSWORD` 覆盖，当前默认 `a1234567`），每个账号独立执行 BCrypt。成功响应仅在当前 `no-store` 响应中返回 `resetCount`、`initialPassword` 和 `mustChangePassword=true`。操作日志只记录 `RESET_PASSWORD` / `BATCH_RESET_PASSWORD`、数量和业务 ID，不记录明文、摘要或姓名。新建教师仍沿用原有随机/显式初始密码规则。

若批量范围包含当前登录管理员对应的多角色教师账号，恢复操作仍会成功；由于现有 JWT 不做服务端集中撤销，页面会提示管理员立即手动退出并以默认密码重新登录。本项目不为此引入 Redis 或 JWT 黑名单。

## 任课关系

- `GET /subjects`：返回已启用且仅限物理、化学、生物的真实科目选择数据。
- `GET /teachers/{teacherId}/teaching-assignments`：返回该教师的当前及历史三元关系。
- `POST /teachers/{teacherId}/teaching-assignments`：请求为`classId`、`subjectId`、`primary`、`startTime`；教师账号/档案、班级、科目均必须有效，班级仅允许`ACTIVE`。
- `PATCH /teaching-assignments/{id}/status`：状态为`ACTIVE`、`ENDED`或`DISABLED`；结束时写入结束时间，不提供物理删除。

`ren_ke_guan_xi`已有唯一键`(jiao_shi_id,ban_ji_id,ke_mu_id)`。因此首版保留结束或停用的原记录且拒绝再次插入相同三元组合；为避免覆盖结束时间，不支持将历史关系重新启用。若需要同一三元关系多段独立历史，必须另行设计迁移，不能修改V1–V6。
