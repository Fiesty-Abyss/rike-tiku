# AI开发交接

更新时间：2026-08-05

## 0. 最新轮次：管理员班级基础管理后端

当前分支：`feat/admin-class-management`。本轮只实现后端班级基础管理，已完成自动化和JAR实测，当前PR尚未创建/合并。

已实现：

- `GET /api/v1/admin/classes`：分页查询，支持`page`、`size`、`code`、`name`、`grade`、`status`。
- `GET /api/v1/admin/classes/{id}`、`POST /api/v1/admin/classes`、`PUT /api/v1/admin/classes/{id}`、`PATCH /api/v1/admin/classes/{id}/status`。
- 创建默认`ACTIVE`；班级编码全局唯一且首版不可修改；状态只允许`ACTIVE`、`GRADUATED`、`DISABLED`。
- 全部接口由Spring Security限制为`ROLE_ADMIN`；认证服务端首次改密门禁继续生效。
- 不提供删除接口，未新增迁移或新表，未改变V1-V6。

验证：

- `mvn clean test`与`mvn clean package`：PASS，24/24；认证和数据库原有23项回归继续通过。
- 随机临时库迁移V1-V6后启动JAR：健康接口`UP/UP`，未登录班级接口401，ADMIN Token班级接口200。
- 临时库`rike_tiku_jar_verify_f62284422a6444b38fbfed4dd5bf65d4`已删除；正式库`rike_tiku.yong_hu`复查为0行。
- 接口细节见`docs/ADMIN_CLASS_MANAGEMENT_API.md`。

尚未实现：班级前端、学生Excel导入、学生/教师CRUD、任课关系、题库、练习、错题和AI。

下一轮唯一建议：在本PR合并后单独实现管理员端学生Excel批量导入后端接口，复用现有`ban_ji`并只接受ACTIVE班级。

## 0.1 已合并轮次：前端登录与认证状态基础

## 0.1 进行中：前端登录与认证状态基础

前端认证轮已合入`main`。PR [#5](https://github.com/Fiesty-Abyss/rike-tiku/pull/5)以普通merge合并，提交`b519134`；远程功能分支已删除。合并后前端26项测试、类型检查、构建与后端23项回归均通过。未新增迁移、未修改后端接口，未开发导入、题库、练习、错题或AI。

当前验证：前端26项Vitest测试、类型检查、生产构建均通过；后端`mvn clean test`23/23通过。用户已在临时库完成真实浏览器认证联调，全部通过；临时库与进程已清理，正式库未污染。PR #5已普通merge。详见`docs/FRONTEND_AUTHENTICATION.md`。

后端认证轮已完成并合入`main`。Pull Request [#4](https://github.com/Fiesty-Abyss/rike-tiku/pull/4)从Draft转为Ready后以普通merge合并；PR最终HEAD为`caf1b5369128e0928bf3f2f3f2a2a31390d4fbb5`，merge提交为`9783435b6bd61166145aa1c734c5e610bd129943`，远程功能分支已删除。合并后在`main`重新执行`mvn clean test`：PASS，23/23，0 failures，0 errors，0 skipped。

已实现：

- `POST /api/v1/auth/login`：从数据库读取有效账号和全部真实角色，`expectedRole`只做入口校验。
- `GET /api/v1/auth/me`：返回当前用户、真实角色、首次改密状态和可用档案显示信息。
- `POST /api/v1/auth/change-initial-password`：校验旧密码和新密码规则，事务更新BCrypt摘要、修改时间和首次登录标志，并返回新Token。
- JJWT 0.13.0 + HS256；密钥来自`RIKE_TIKU_JWT_SECRET`，有效期来自`RIKE_TIKU_JWT_EXPIRATION_SECONDS`，默认7200秒。
- Bearer Token过滤、SecurityContext、认证失败、Token无效/过期、权限不足和首次改密门禁。
- `/api/v1/test/student`、`teacher`、`admin`仅作为三角色技术验证接口。
- 健康检查、登录和SpringDoc开发接口匿名可访问，其余接口默认认证。

数据库与测试：

- 没有新增迁移，没有修改V1-V6，仍为18张业务表。
- 自动化认证测试使用随机临时MySQL数据库，从V1迁移到V6，结束后自动删除。
- 正式库`yong_hu`保持0行，没有生产测试账号污染。
- `mvn clean test`：PASS，23/23；原16项数据库测试全部回归通过。
- `mvn clean package`：PASS，23/23并生成可执行JAR。
- JAR在18088端口启动：健康接口`UP/UP`；无Token访问`/auth/me`返回401；匿名错误登录返回统一401；验证后进程已停止。

安全边界：

- 不提交JWT真实密钥或数据库密码；`.env.example`只有变量名和占位说明。
- 不实现Refresh Token或Token表。
- 访问Token无状态且默认两小时。数据库中的账号/角色变化不会立即撤销已签发Token；这是当前MVP明确边界。
- 本机初始化脚本会通过安全提示读取数据库密码，并在缺少JWT密钥时生成随机密钥写入Windows用户环境；已打开的IDEA需重启才能继承。

下一轮唯一任务为前端登录与认证状态基础：三角色入口、Pinia、Axios Token、首次改密和路由守卫。不同时开发导入、题库、练习或AI。

## 1. 当前修复轮唯一目标

修复IDEA图形界面启动时数据库密码环境变量未生效。上轮只补充了说明，用户人工重试仍出现 `using password: NO`；本轮必须实际修复启动链路，不再把文档修改等同于IDEA验证完成。

实际配置以 `application.yml` 为准：数据库使用 `RIKE_TIKU_DB_HOST`、`RIKE_TIKU_DB_PORT`、`RIKE_TIKU_DB_NAME`、`RIKE_TIKU_DB_USERNAME` 和 `RIKE_TIKU_DB_PASSWORD`；项目不读取 `RIKE_TIKU_DB_URL`。本地默认条件下只需提供密码变量。

修复轮结果：

- 后端 `.env.example` 已明确标注不会被自动加载，并补齐后端端口示例。
- 后端README已增加IDEA Run Configuration逐步说明、实际变量表、PowerShell启动和 `using password: NO/YES` 排查。
- 根README已区分后端环境变量与前端Vite `.env.local` 的加载方式。
- `mvn clean test`：PASS，16/16；`mvn clean package`：PASS，16/16。
- 空密码JAR启动：按预期失败，退出码1并显示 `using password: NO`。
- 注入密码的JAR启动：PASS；临时端口18085健康接口返回 `UP/UP`，随后已停止进程。
- IDEA图形界面人工启动：`AWAITING_USER_VERIFICATION`，不得写成PASS。

后续定位出的直接证据：

- 本机 `.idea/workspace.xml` 中确实存在 `RikeTikuBackendApplication`，但其环境变量列表为空。
- Windows用户环境、系统环境和当前IDEA父进程中均不存在 `RIKE_TIKU_DB_PASSWORD`。
- 因此Spring Boot收到空密码，与日志 `using password: NO` 完全一致。
- 本轮采用Windows用户环境变量方案：真实密码只保存在本机用户环境，不进入Git；增加安全提示输入脚本 `rike-tiku-backend/scripts/setup-idea-local-env.ps1`。
- 已实际为当前Windows用户设置该变量；由于IDEA在设置前已经启动，必须完全重启IDEA才能继承。
- 新脚本通过PowerShell语法解析，0个错误；真实密码未写入脚本、README或Git文件。
- `mvn clean test`：PASS，16/16；`mvn clean package`：PASS，16/16。
- 空密码JAR启动：PASS（按预期失败，退出码1，`using password: NO`）。
- 使用Windows用户变量启动JAR：PASS；临时端口18087健康接口返回 `UP/UP`，随后已停止进程。
- IDEA重启后点击Run：`AWAITING_USER_VERIFICATION`。当前IDEA在变量设置前已启动，不能将未执行的图形界面复核写成PASS。

## 2. 实际完成

- 创建公开仓库 `https://github.com/Fiesty-Abyss/rike-tiku`，默认分支 `main`。
- 建立根README和 `AI_PROJECT_CONTEXT.md`，纳入经安全扫描的项目文档、脚本和题库候选资料。
- 原V3.0 DOCX含本机开发密码字面值，原件未修改、未删除并在Git忽略；仓库只提交替换为“弱口令示例”的公开脱敏副本。
- 创建V5：`yong_hu`、`jiao_se`、`yong_hu_jiao_se`、`xue_sheng_dang_an`、`jiao_shi_dang_an`，初始化三基础角色，并给题目审核人补充用户外键。
- 创建V6：`ban_ji`、`ban_ji_xue_sheng`、`ren_ke_guan_xi`。
- 使用生成列唯一索引同时实现重复有效班级关系拒绝、历史保留和单一有效主班级。
- 创建5个最小Entity/Mapper和MyBatis-Plus审计字段自动填充处理器。
- 新增8个账号/教学组织数据库测试，连同原测试共16项。
- 真实开发库迁移到V6；随机临时空库从V1执行到V6后自动删除。
- 创建数据库V2文档、ER图、数据库目录说明和真实结构快照。
- Maven测试、打包、JAR启动和真实健康检查全部通过。

## 3. 关键设计决定

- 不修改已执行的V1–V4，所有结构变化只在V5/V6。
- 基础角色固定为 `STUDENT`、`TEACHER`、`ADMIN`；同一用户可多角色。
- 密码字段只接受长度至少50的摘要，本轮不创建任何用户或初始密码。
- 学生和教师档案对 `yong_hu_id` 分别唯一，学号和工号分别全局唯一。
- `ban_ji_xue_sheng` 没有采用会阻止历史重入的永久二元唯一，而是只对有效关系唯一；历史退出行保留。
- `ren_ke_guan_xi` 对教师、班级、科目三元联合唯一，不拆分成两张权限关系。
- 审核人字段保持可空，外键使用 `ON DELETE RESTRICT`；逻辑删除用户不破坏历史审核轨迹。
- 姓名和现实职务只展示，不能用于授权。

## 4. 代码和文档

实现提交：`56cb779 feat(database): add user and teaching organization model`

Pull Request：`https://github.com/Fiesty-Abyss/rike-tiku/pull/1`，状态 `MERGED`；远程功能分支已删除，当前分支为 `main`。

新增迁移：

- `V5__create_user_role_and_profile_tables.sql`
- `V6__create_class_and_teaching_relationship_tables.sql`

新增Java：

- `ShenJiZiDuanTianChongChuLiQi`
- `YongHu`、`JiaoSe`及Mapper
- `BanJi`、`BanJiXueSheng`、`RenKeGuanXi`及Mapper
- `UserTeachingDatabaseModelTest`

新增文档：

- `README.md`
- `docs/AI_PROJECT_CONTEXT.md`
- `docs/DATABASE_MODEL_V2.md`
- `database/README.md`
- `database/diagrams/rike_tiku_er.md`
- `database/schema/rike_tiku_schema.sql`

## 5. 数据库结果

- MySQL 8.4.10，`utf8mb4 / utf8mb4_0900_ai_ci`。
- Flyway V1–V6，6条迁移全部成功。
- 18张业务表和1张Flyway系统表。
- 新表8张；旧题库表和3道样本未被破坏。
- 仅角色表有3条初始化数据；用户、档案、班级和任课表均为空。
- 3道题仍为 `PENDING`，未导入完整30题。

## 6. 实际测试

- `mvn clean test`：PASS，16 tests。
- `mvn clean package`：PASS，16 tests并生成可执行JAR。
- 临时空库V1–V6：PASS，执行6条迁移、18张业务表、3道样本，随后删除临时库。
- `java -jar ... --port 18083`等效环境变量启动：PASS。
- `GET http://localhost:18083/api/v1/health`：PASS，`UP/UP`；验证进程已停止。
- 约束、外键、自动填充、逻辑删除和事务回滚：PASS，详见 `UserTeachingDatabaseModelTest`。

测试时密码只通过进程环境变量注入，仓库中没有真实密码。

## 7. 安全和公开资料

- 未发现GitHub Token、AI API Key、JWT密钥、真实数据库密码或学生隐私。
- V3.0公开脱敏副本经Microsoft Word导出为53页PDF并检查全部页面总览，无裁切、重叠或异常分页。
- 题库资料为学习、开发和审核候选，权利状态未知；不能据此宣称已获传播授权。
- `.gitattributes` 将PDF、Office文档和图片固定为二进制，避免Windows换行转换破坏文件。

## 8. 接管注意事项

- 先以实际Git、Flyway、测试和真实数据库为准。
- 不修改V1–V6；后续数据库变化从V7开始。
- 不把三个初始化角色误写成已经创建真实账号。
- 不把数据库模型完成误写成登录、JWT、导入或权限业务已经实现。
- 教师数据权限必须查询三元任课关系。
- 不发布 `PENDING` / `COPYRIGHT_UNKNOWN` 题目。

## 9. 下一步唯一任务

实现后端统一认证与JWT登录基础，覆盖账号状态、首次登录改密门禁和 `STUDENT` / `TEACHER` / `ADMIN` 鉴权测试。不要同时开发学生导入、练习、前端工作台或AI接口。
