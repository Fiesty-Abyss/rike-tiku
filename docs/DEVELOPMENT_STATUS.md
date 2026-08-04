# 开发状态

更新时间：2026-08-04

## 当前基线

- 设计基线：V3.0
- 当前轮次：GitHub公开仓库与账号、教学组织数据库模型
- 当前状态：DONE_VERIFIED
- 功能分支：`feat/database-user-teaching`
- 远程仓库：`https://github.com/Fiesty-Abyss/rike-tiku`
- 仓库可见性：PUBLIC
- 实现提交：`56cb779`（`feat(database): add user and teaching organization model`）

## 本轮已完成

| 内容 | 状态 | 证据 |
|---|---|---|
| GitHub公开仓库和main基线 | DONE_VERIFIED | `Fiesty-Abyss/rike-tiku`，基线提交`58fd551`已推送 |
| 公开资料安全检查 | DONE_VERIFIED | 文本、OOXML、164个PDF及9个旧DOC扫描；无真实密钥和学生隐私 |
| V3.0公开脱敏副本 | DONE_VERIFIED | 原件保留本地且忽略；公开副本替换1处本机弱口令字面值，Word导出53页逐页总览正常 |
| 账号、角色和档案模型 | DONE_VERIFIED | Flyway V5成功；5张表、3个基础角色、审核人外键 |
| 班级和三元任课模型 | DONE_VERIFIED | Flyway V6成功；3张表，有效主班级和任课三元唯一约束 |
| 最小MyBatis-Plus映射 | DONE_VERIFIED | 5个Entity/Mapper及审计字段填充处理器 |
| 数据库自动化测试 | DONE_VERIFIED | 16 tests，0 failures，0 errors |
| 空数据库全迁移 | DONE_VERIFIED | 随机临时库V1–V6执行成功，18张业务表、3道样本；测试后临时库已删除 |
| 结构文档和快照 | DONE_VERIFIED | `DATABASE_MODEL_V2.md`、ER图、真实`mysqldump --no-data`快照 |
| Maven打包和JAR健康 | DONE_VERIFIED | `mvn clean package`成功；18083端口返回UP/UP后已停止进程 |

## 当前数据库事实

- MySQL：8.4.10；数据库：`rike_tiku`。
- 字符集/排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`。
- Flyway：12.4.0；当前版本v6；V1–V6全部success=1。
- 业务表：18张；另有Flyway系统表 `flyway_schema_history`。
- 新增表：`yong_hu`、`jiao_se`、`yong_hu_jiao_se`、`xue_sheng_dang_an`、`jiao_shi_dang_an`、`ban_ji`、`ban_ji_xue_sheng`、`ren_ke_guan_xi`。
- 初始化数据：只增加 `STUDENT`、`TEACHER`、`ADMIN` 三个角色；没有创建真实用户、档案、班级或任课关系。
- 原题库：3题、12选项、3标准解析、1附件、9来源、3条原审核轨迹；3题均为 `PENDING`。

## 本轮测试结论

- `mvn clean test`：PASS，16/16。
- `mvn clean package`：PASS，16/16并生成可执行JAR。
- V5、V6迁移：PASS。
- 空库V1–V6：PASS。
- 原题库10表和3题回归：PASS。
- 用户名、角色码、学号、工号、班级编码唯一：PASS。
- 一个用户多个角色、重复用户角色拒绝：PASS。
- 一个用户最多一份学生/教师档案：PASS。
- 重复有效班级学生关系拒绝、一个有效主班级、退出历史保留：PASS。
- 任课三元关系唯一、跨班同科、同班不同科、无关系不授权：PASS。
- 审核人外键、自动填充、逻辑删除、事务回滚：PASS。
- JAR启动和健康接口：PASS，`status=UP`、`database=UP`。

## 明确未实施

- 登录Controller、JWT签发、注册和账号CRUD
- 学生Excel导入和教师导入
- 前端登录页、权限菜单和角色工作台
- 题库业务API、练习、判分、错题和统计
- AI Provider、DeepSeek、Redis、MinIO、WebSocket、Docker和微服务
- 完整30题导入或发布

## 已知事项

- V1/V2旧迁移仍会产生MySQL整数显示宽度弃用警告；已执行迁移没有改写，V5/V6使用无显示宽度的 `TINYINT`。
- JDK 25下Mockito/Byte Buddy仍输出动态Agent兼容性警告，测试实际通过。
- 题库资料权利状态为 `COPYRIGHT_UNKNOWN`，只作学习、开发和人工审核候选；仓库不声明已获公开传播授权。
- 公共仓库首次推送约252 MB资料，耗时较长但最终成功；没有使用强推。

## 下一步唯一任务

实现后端统一认证与JWT登录基础，覆盖账号状态、首次登录改密门禁和三角色鉴权测试；本轮不再继续。
