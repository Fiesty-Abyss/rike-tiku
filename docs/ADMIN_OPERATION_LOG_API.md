# 管理员高风险操作日志

PR #27 在不修改 V1–V10 的前提下新增 Flyway V11：`guan_li_cao_zuo_ri_zhi`。

## 记录内容

- 真实操作者用户 ID 和用户名；
- 模块、动作和业务对象 ID（批量导入使用导入批次 ID）；
- `SUCCESS` 或 `FAILURE`；
- 固定的脱敏摘要和业务错误码；
- 创建时间。

不记录密码、JWT、API Key、初始密码或异常详情。失败记录使用独立事务写入，业务操作回滚时仍保留失败审计；审计写入失败不会伪装成业务成功。

当前已接入的管理员写操作包括：班级创建/编辑/状态变更，学生创建/编辑/调班/密码重置，教师创建/编辑，任课关系创建/状态变更，题目创建/编辑/审核/发布/停用/重新发布，题库批量导入确认和学生批量导入确认。

## 查询 API

```http
GET /api/v1/admin/operation-logs?page=1&size=20&module=QUESTION&action=APPROVED&result=SUCCESS
Authorization: Bearer <ADMIN JWT>
```

仅 `ADMIN` 可访问。`page` 从 1 开始，`size` 范围为 1–100；`module`、`action`、`result` 为可选精确筛选条件。响应返回 `records`、`total`、`page`、`size` 和 `pages`。

管理员页面为 `/admin/operation-logs`，查询页面支持模块、动作、结果和分页筛选。`GuanLiCaoZuoRiZhiIntegrationTest` 与认证集成测试覆盖日志写入及 ADMIN/非 ADMIN 查询权限；最终非 AI 审计和统一浏览器验收仍按 PR #27 状态执行。在最终集成人工验收前，不把 MA-017 写成 `DONE_VERIFIED`。

## 题目来源权利补充

Golden30 使用现有正常导入链后，通过管理员 API 补充独立演示库的来源权利状态：

```http
PATCH /api/v1/admin/questions/{questionId}/source-rights
Authorization: Bearer <ADMIN JWT>
Content-Type: application/json

{"rightsStatus":"USER_PROVIDED","rightsBasis":"本地演示题库：用户提供，仅用于独立验收数据库"}
```

该接口只允许 `DRAFT` 或 `PENDING` 题目，发布型权利状态必须填写非空依据，并记录 `UPDATE_SOURCE_RIGHTS` 审计动作；不会修改题干、答案、解析、附件或原始文件路径。
