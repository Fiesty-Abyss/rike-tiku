# AI 开发交接

更新时间：2026-08-04

## 1. 本轮唯一目标

基于V3.0和三科真实清洗数据，完成题库核心数据库第一版设计；只通过Flyway创建允许的业务表，提供最小MyBatis-Plus映射与自动化测试，并真实导入物理、化学、生物各1题。禁止完整30题导入、前端、用户、练习和AI业务。

## 2. 实际完成内容

- 阅读`DEVELOPMENT_STATUS.md`、`AI_HANDOFF.md`、V3.0、当前后端和Git状态。
- 读取物理/化学/生物三份`待审核_清洗版.xlsx`，每份10题、19个检查字段。
- 读取对应JSON、质量报告和附件目录；确认题干/解析对象标记与附件记录可一一对应。
- 读取用户允许的两个历史任务，确认公式/图片常为独立对象、题干与答案解析必须分项追溯、异常母题不能自动进入MVP。
- 创建ER关系、完整字段字典、答案扩展方案和Excel/JSON映射文档。
- 引入Spring Boot 4.1的Flyway starter与MySQL模块，由Flyway真实创建10张业务表。
- 创建最小实体`KeMu`、`TiMu`、`TiMuFuJian`和3个`BaseMapper`。
- 真实写入物理2023新课标Q14、化学2023新课标Q7、生物2023新课标Q1；均为`PENDING`。
- 写入1个物理真实公式附件F107，保存相对路径、SHA-256、对象标识、顺序和正文字符位置。
- 建立4个数据库模型测试，覆盖表范围、迁移数、三科样本、Mapper、答案JSON、附件位置、自动判分例外和难度约束。

## 3. 关键设计决定

- 数据库表/字段全部使用`pinyin_snake_case`，没有V1.0大驼峰表。
- 正确答案使用受控JSON而不是EAV：单选/多选保存`optionLabels`，未来填空按`blanks[].acceptedAnswers`扩展。
- 难度固定1/2/3映射easy/medium/hard。
- `SUBJECTIVE`只允许`TOPIC_LEARNING`且必须不自动判分，用于未来保留和拆分结构异常母题；异常Q34本轮没有入库。
- 标准解析是独立`STANDARD`记录，未来`AI`解析不能覆盖标准解析。
- 附件不存BLOB，正文对象标记+对象标识+字符位置+顺序共同定位。
- 题干、答案、标准解析各有独立来源行；当前权利状态保守设为`COPYRIGHT_UNKNOWN`。
- 用户模块不在本轮范围，因此审核人ID暂不设外键，后续用户迁移建立后再补。

## 4. 新增和修改文件

新增：

- `docs/QUESTION_DATABASE_MODEL_V1.md`
- `rike-tiku-backend/src/main/resources/db/migration/V1__create_subject_and_knowledge_tables.sql`
- `rike-tiku-backend/src/main/resources/db/migration/V2__create_question_core_tables.sql`
- `rike-tiku-backend/src/main/resources/db/migration/V3__insert_three_subject_question_samples.sql`
- `rike-tiku-backend/src/main/resources/db/migration/V4__allow_subjective_topic_learning.sql`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/entity/KeMu.java`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/entity/TiMu.java`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/entity/TiMuFuJian.java`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/mapper/KeMuMapper.java`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/mapper/TiMuMapper.java`
- `rike-tiku-backend/src/main/java/com/neu/riketiku/tiku/mapper/TiMuFuJianMapper.java`
- `rike-tiku-backend/src/test/java/com/neu/riketiku/tiku/QuestionDatabaseModelTest.java`

修改：

- `.gitignore`：忽略任务本地检查目录`.codex_work/`。
- `rike-tiku-backend/pom.xml`：增加Flyway starter和MySQL模块。
- `rike-tiku-backend/src/main/resources/application.yml`：启用迁移、校验并禁用clean。
- `docs/DEVELOPMENT_STATUS.md`
- `docs/AI_HANDOFF.md`

删除：无。

前端文件：无变化。

## 5. 数据库结果

- MySQL：8.4.10；数据库`rike_tiku`。
- 字符集/排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`。
- Flyway：12.4.0，schema版本v4，4条迁移全部success=1。
- 业务表10张；另有Flyway系统表`flyway_schema_history`。
- 样本数据：3题、12选项、3标准解析、3知识点关联、1附件、9来源、3审核记录。
- 没有完整30题导入，没有其他业务表。

## 6. 实际执行命令与结果

- 三科XLSX通过工作区表格运行时导入检查：PASS，均含`题目检查`和`质量统计`工作表。
- `mvn clean test`（首次）：FAIL，Flyway自动配置未装配，数据库仍为空。
- `mvn clean test`（第二次）：FAIL，迁移V1-V3成功，4个新测试中3通过；非法难度确实被数据库拒绝，但异常类型断言过窄。
- `mvn clean test`（修正后）：PASS，8/8。
- `mvn test -Dtest=QuestionDatabaseModelTest`：PASS，4/4，并实际迁移至v4。
- `mvn clean package`：PASS，8/8，可执行JAR生成。
- MySQL information_schema/Flyway/样本聚合查询：PASS，表和行数与设计一致。
- 物理F107文件SHA-256：`a15be0633cd8b36cdc7c68d16a1df43dc050d34661edad049931136e89e15b1f`，与数据库一致。
- 默认端口JAR启动：该次FAIL，8081当时被现存Java进程占用；未终止该进程，最终端口复查时已自行释放。
- 临时端口18082启动和`GET /api/v1/health`：PASS，`status=UP`、`database=UP`；本轮进程已停止。

执行测试时只在进程环境变量中注入本机数据库密码，没有写入Git文件。实际密码不应出现在提交、日志说明或启动配置中。

## 7. 已知问题和风险

- `TINYINT(1)`首次迁移有MySQL显示宽度弃用警告，但功能和约束正常；已执行迁移文件不得改写，后续用新迁移演进。
- 8081曾被PID 33576的Java进程占用，最终复查时已经释放；本轮没有越权终止来源不明的进程。
- 原始附件目前仍位于用户题库目录，数据库只保存相对路径。正式文件存储策略尚未实现。
- 三道样本为数据库结构验证数据，不代表版权或学科人工审核通过；必须保持`PENDING`。
- 目前只为最小读取链路创建3组实体/Mapper；其余表等正式题库服务开发时按用例补实体，避免本轮提前开发CRUD。
- JDK 25 Mockito动态Agent预警仍存在。

## 8. Git状态

- 仓库：`E:/BISHE2026`
- 分支：`main`
- 远程：无，不得push。
- 上一轮HEAD：`81cbc38`。
- 本轮实施提交：`39ea9c7`（`feat(database): add question core model`）。
- 状态与交接文档在实施提交之后单独提交；以当前`git log -1`确认最终HEAD。
- 用户原有DOCX、`脚本/`、`题库/`保持未跟踪，不纳入本轮提交。

## 9. 下一步唯一任务

未设置。本轮完成后停止，不继续开发题库API、导入全量数据或前端页面。

## 10. 下一位AI接管提示

先读取`docs/DEVELOPMENT_STATUS.md`、`docs/AI_HANDOFF.md`和`docs/QUESTION_DATABASE_MODEL_V1.md`，再检查Flyway历史、Git状态和本轮提交。不要修改已应用的V1-V4迁移；任何结构变化必须新增迁移。三道样本仍是PENDING且权利状态未知，不得发布或扩展为完整30题。等待用户给出新的唯一任务后再行动。
