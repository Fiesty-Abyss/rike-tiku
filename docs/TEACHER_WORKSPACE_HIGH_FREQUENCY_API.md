# 教师班级学科工作台与高频考点 API

> 本文为历史阶段快照，当前项目状态请查看 [README](../README.md) 和 [开发状态](DEVELOPMENT_STATUS.md)。

当前分支：`feat/teacher-workspace-high-frequency`（Draft PR #18）。本组接口只服务教师本人 ACTIVE 三元任课关系；服务端从登录用户的教师档案和 `ren_ke_guan_xi.id` 推导权限，不接受前端传入 teacherId 授权。

## 教师接口

需要 `TEACHER` 角色，路径前缀为 `/api/v1`。

### `GET /teacher/teaching-scopes`

保留原任教范围接口，并返回 `teachingAssignmentId`、班级、年级、科目、主任课标识和状态。

### `GET /teacher/scopes/{scopeId}`

返回当前任课关系的班级、科目、教师、学生人数、基础学生名单和该科知识点选项。`scopeId` 必须属于当前教师、状态为 `ACTIVE`，班级和科目也必须有效，否则返回 403。

### 高频考点维护

- `GET /teacher/scopes/{scopeId}/high-frequency-points`：列出当前工作台的考点。
- `POST /teacher/scopes/{scopeId}/high-frequency-points`：创建考点，输入 `knowledgePointId`、`title`、`content`、可选口诀/误区和非负 `sortOrder`。
- `PUT /teacher/high-frequency-points/{id}`：编辑标题、正文、知识点、口诀、误区和排序；不能改到其他科目。
- `POST /teacher/high-frequency-points/{id}/status`：输入 `status=ACTIVE|DISABLED` 启停考点。

创建、编辑、启停均重新验证本人 ACTIVE 任课关系；跨教师、跨班级、跨科目和停用任课关系均拒绝。响应只返回业务展示字段，不返回密码摘要、Token 或内部逻辑删除字段。

## 学生接口

### `GET /student/high-frequency-points?subjectId={id}`

需要 `STUDENT` 角色。服务端从当前登录用户读取有效学生档案、ACTIVE 主班级，再匹配该班级和科目的 ACTIVE 任课关系，只返回 ACTIVE 高频考点及知识点名称、教师姓名、排序。请求不接受 studentId、teacherId 或 classId；没有对应内容时返回空数组，由前端显示“当前学科暂无高频考点”。

## 数据库

V8 新增 `gao_pin_kao_dian`，不修改 V1–V7；当前业务表共 24 张。高频考点只保存 `ren_ke_guan_xi_id` 和同科 `zhi_shi_dian_id`，不重复保存教师、班级、科目。
