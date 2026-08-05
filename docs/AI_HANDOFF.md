# AI开发交接

更新时间：2026-08-05

## 当前轮次：管理员班级基础管理后端

当前分支为`feat/admin-class-management`。实现提交基线为`b3115a8`，后续仅包含本轮文档一致性修复。草稿PR为[#6](https://github.com/Fiesty-Abyss/rike-tiku/pull/6)，尚未合并；应以PR页面的最新提交为准。

已实现：

- 管理员班级分页查询、详情、创建、修改和状态变更：`/api/v1/admin/classes`。
- 分页支持`page`、`size`、`code`、`name`、`grade`、`status`；响应只返回公开业务字段。
- 创建默认`ACTIVE`；编码全局唯一且首版不可修改；状态只接受`ACTIVE`、`GRADUATED`、`DISABLED`。
- Spring Security已将全部班级接口限制为`ROLE_ADMIN`；现有JWT和首次改密门禁仍然生效。
- 没有删除接口、没有新增表、没有新增或修改Flyway V1-V6。

已验证：

- `mvn clean test`：24/24 PASS；`mvn clean package`：24/24 PASS。
- 集成测试使用随机临时MySQL库，V1-V6迁移、班级业务规则、ADMIN/非ADMIN访问边界和原认证/数据库回归均通过。
- JAR在随机临时库上实测：健康接口`UP/UP`，未登录班级接口401，ADMIN Token班级接口200。
- 临时库已删除，正式`rike_tiku.yong_hu`为0行；没有提交测试账号、密码、Token、JWT密钥或真实数据库凭据。

尚未实现：班级管理前端、学生Excel导入、学生管理、教师管理、任课关系、题库业务、练习、错题和AI。

详细接口见[管理员班级管理接口](ADMIN_CLASS_MANAGEMENT_API.md)。

## 当前唯一下一步

仅在PR #6合并后，实施管理员学生Excel批量导入后端。导入必须使用现有`ban_ji`，且只允许绑定存在并处于`ACTIVE`状态的班级。

## 已合并历史轮次

### 前端登录与认证状态基础（PR #5，MERGED）

- 三角色登录入口、共用表单、Pinia认证状态、Axios Bearer注入、会话恢复、首次改密、角色路由守卫、最小工作台和退出登录已完成。
- 前端26项测试、类型检查、生产构建及后端23项回归均通过；用户已在临时库完成真实浏览器联调，临时库已清理且正式库未污染。

### 后端统一认证与JWT（PR #4，MERGED）

- `POST /api/v1/auth/login`、`GET /api/v1/auth/me`和`POST /api/v1/auth/change-initial-password`已完成。
- 角色只从`yong_hu_jiao_se`和有效`jiao_se`读取；`expectedRole`只校验入口，不授予角色。
- BCrypt、JWT、三角色鉴权、账号状态、Token无效/过期和首次改密门禁均已测试完成。

### 账号与教学组织数据库模型（PR #1，MERGED）

- Flyway V1-V6已合并；业务表18张。`ban_ji`、`ban_ji_xue_sheng`、`ren_ke_guan_xi`为既有数据模型。
- 不修改已执行V1-V6；未来结构变化必须新增迁移。教师数据权限应以教师、班级、科目的三元任课关系查询，不能以名称授权。

## 接管注意事项

- 事实优先级：当前`main`代码和配置 > Flyway > 测试 > Git/PR > 状态文档。
- 不把初始三角色误写成真实用户；正式库用户保持为空，测试必须使用临时库。
- 不发布`PENDING`或`COPYRIGHT_UNKNOWN`题目；AI不得覆盖标准答案或标准解析。
