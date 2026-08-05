# 开发状态

更新时间：2026-08-04

## 当前基线

- 设计基线：V3.0
- 当前轮次：前端登录与认证状态基础
- 当前状态：DONE_VERIFIED；PR #5已完成真实人工联调，Ready for review
- 当前分支：`feat/frontend-auth`
- Pull Request：[#5](https://github.com/Fiesty-Abyss/rike-tiku/pull/5)，DRAFT，未合并
- 本轮实现分支：`feat/backend-auth-jwt`（远程分支已删除）
- 开始时HEAD：`74af93327f8fd41a15401cab3fa87475a4bd1f0a`
- 远程仓库：`https://github.com/Fiesty-Abyss/rike-tiku`
- Pull Request：[#4](https://github.com/Fiesty-Abyss/rike-tiku/pull/4)，MERGED
- PR最终HEAD：`caf1b5369128e0928bf3f2f3f2a2a31390d4fbb5`
- 普通merge提交：`9783435b6bd61166145aa1c734c5e610bd129943`
- 当前Flyway：V6；本轮没有新增或修改迁移

## 本轮已完成

| 内容 | 状态 | 证据 |
|---|---|---|
| BCrypt密码校验与更新 | DONE_VERIFIED | 登录和首次改密集成测试 |
| JWT签发、解析和SecurityContext | DONE_VERIFIED | JJWT 0.13.0、HS256；有效、过期和篡改Token测试 |
| 登录与当前用户接口 | DONE_VERIFIED | `/api/v1/auth/login`、`/api/v1/auth/me` |
| 首次登录改密与服务端门禁 | DONE_VERIFIED | `/api/v1/auth/change-initial-password`及受保护接口拦截测试 |
| 三角色鉴权 | DONE_VERIFIED | STUDENT、TEACHER、ADMIN允许和越权拒绝测试 |
| 账号状态与入口角色校验 | DONE_VERIFIED | DISABLED、LOCKED、无角色、角色停用、入口角色不匹配测试 |
| 数据库隔离 | DONE_VERIFIED | 随机临时库执行V1-V6，测试结束自动删除；正式库用户数保持0 |
| Maven测试与打包 | DONE_VERIFIED | 23 tests，0 failures，0 errors；可执行JAR构建成功 |
| 真实JAR健康检查 | DONE_VERIFIED | 临时端口18088返回`UP/UP`；验证进程已停止 |

## 认证设计事实

- 角色只从`yong_hu_jiao_se`和有效`jiao_se`读取；`expectedRole`只校验入口，不授予权限。
- 同一用户可以拥有多个角色，Token和`/me`均保留全部真实角色。
- JWT密钥通过`RIKE_TIKU_JWT_SECRET`注入，有效期通过`RIKE_TIKU_JWT_EXPIRATION_SECONDS`配置，默认7200秒。
- 本轮不实现Refresh Token，不创建Token表。
- 首次登录用户可以获得Token，但服务端只允许其访问健康检查、`/me`和首次改密接口。
- 登录成功更新时间`zui_hou_deng_lu_shi_jian`；更新失败会阻止登录，避免返回与数据库状态不一致的成功结果。
- 改密成功会更新BCrypt摘要、`mi_ma_xiu_gai_shi_jian`并关闭首次登录标志，同时返回新Token。
- SpringDoc开发接口保持匿名可访问；其他未明确公开的接口默认要求认证。

## 当前数据库事实

- MySQL：8.4.10；数据库：`rike_tiku`。
- 字符集/排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`。
- Flyway：12.4.0；V1-V6全部成功；业务表18张，另有`flyway_schema_history`。
- 本轮未修改V1-V6，未创建新表或生产测试用户。
- 正式库`yong_hu`仍为0行；原3道题仍为`PENDING`。

## 本轮测试结论

- 正确学生、教师、管理员和多角色账号登录：PASS。
- 不存在用户、错误密码、DISABLED、LOCKED、无有效角色和入口角色不匹配：PASS。
- 前端提交ADMIN不能给学生授予ADMIN：PASS。
- JWT有效、过期、篡改、缺失：PASS。
- 三角色允许访问和越权拒绝：PASS。
- `/auth/me`真实用户、角色及学生/教师显示档案：PASS。
- 首次登录Token、门禁、旧密码、新密码规则和成功改密：PASS。
- BCrypt摘要变化、首次登录标志关闭、新旧密码登录结果：PASS。
- 登录时间更新、公开健康接口、参数错误：PASS。
- 原16项数据库回归测试：PASS。
- `mvn clean test`：PASS，23/23。
- `mvn clean package`：PASS，23/23。
- JAR启动、健康接口、匿名认证错误：PASS。
- PR合并后在`main@9783435`重新执行`mvn clean test`：PASS，23/23，0 failures，0 errors，0 skipped。
- 首次全量测试曾因测试断言的Java泛型编译错误失败；已修复断言并重新完整执行通过，没有将失败轮次记作PASS。

## 明确未实施

- 自由注册、邀请码、Refresh Token、Token表、找回密码和验证码
- 用户CRUD、学生Excel导入、教师、班级和任课管理接口
- 前端登录页、Pinia认证状态、Axios Token注入和路由守卫
- 题库业务API、练习、错题、AI、Redis、WebSocket、Docker和微服务

## 已知边界

- 当前采用两小时无状态访问Token。账号状态或角色在数据库中变化后，已签发Token不会即时撤销；`/me`会重新读取数据库，但角色测试接口在Token过期前使用签发时角色。MVP暂不引入撤销表或Refresh Token。
- 本机新增JWT用户环境变量后，需要完全重启已打开的IDEA才能让图形界面进程继承。
- V1/V2旧迁移仍有MySQL整数显示宽度弃用警告；已执行迁移不改写。
- JDK 25下Mockito/Byte Buddy仍输出动态Agent兼容性警告，测试实际通过。

## 本轮前端认证基础

- 已实现三角色登录入口、共用登录表单、Pinia认证状态、Axios Bearer Token注入、会话恢复、首次改密页、角色路由守卫、最小工作台和退出登录。
- 未修改后端接口、Flyway或数据库；未实现任何学生导入、题库、练习、错题、AI或正式工作台业务。
- 前端`npm test`：26/26 PASS（新增Axios拦截器10项）；`npm run type-check`与`npm run build`：PASS；后端`mvn clean test`：23/23 PASS。
- 真实人工浏览器联调：DONE_VERIFIED。学生/教师/管理员登录、首次改密、刷新恢复、角色越权拦截、多角色、退出、CORS和控制台均通过；临时库已删除，正式库用户数仍为0。

## 下一步唯一任务

待本轮PR经用户确认合并后，再单独实现管理员端学生Excel批量导入后端接口；本轮结束前不提前开发。
