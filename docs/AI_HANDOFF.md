# AI开发交接

更新时间：2026-08-04

## 1. 本轮唯一目标

创建公开GitHub仓库，并以V3.0为基线完成账号、角色、档案、班级、班级学生历史和教师—班级—科目三元任课关系数据库模型。只创建数据库验证所需最小Entity/Mapper，不实现登录、JWT、导入、前端或AI业务。

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
