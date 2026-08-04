# 开发状态

更新时间：2026-08-04

## 当前基线

- 设计基线：V3.0
- 当前轮次：题库核心数据库模型第一版
- 当前状态：DONE_VERIFIED
- Git 分支：`main`
- 远程仓库：未配置
- 本轮实施提交：`39ea9c7`（`feat(database): add question core model`）

## 本轮已完成

| 内容 | 状态 | 证据 |
|---|---|---|
| 阅读交接、V3.0、后端、Git和三科清洗数据 | DONE_VERIFIED | 三份XLSX均为10题；JSON附件审计合计196个对象且原报告路径有效 |
| 题库ER与字段字典 | DONE_VERIFIED | `docs/QUESTION_DATABASE_MODEL_V1.md` |
| Flyway迁移 | DONE_VERIFIED | MySQL实际执行V1-V4，`flyway_schema_history`全部success=1 |
| 10张题库业务表 | DONE_VERIFIED | information_schema查询结果与允许清单完全一致 |
| 最小MyBatis-Plus实体与Mapper | DONE_VERIFIED | `KeMu`、`TiMu`、`TiMuFuJian`及对应Mapper实际查询通过 |
| Excel/JSON字段映射 | DONE_VERIFIED | 19列Excel映射及结构化JSON附件映射已记录 |
| 三科最小真实样本 | DONE_VERIFIED | 物理Q14、化学Q7、生物Q1，各1题，均为PENDING |
| 附件引用验证 | DONE_VERIFIED | 物理公式对象F107的正文位置、相对路径、SHA-256与Mapper查询通过 |
| 后端测试与打包 | DONE_VERIFIED | 8 tests通过；可执行JAR生成；临时端口健康检查UP/UP |

## 当前数据库事实

- 数据库：`rike_tiku`，MySQL 8.4.10。
- 字符集：`utf8mb4`；排序规则：`utf8mb4_0900_ai_ci`。
- Flyway：12.4.0，当前版本v4。
- 业务表：`ke_mu`、`zhi_shi_dian`、`dao_ru_pi_ci`、`ti_mu`、`ti_mu_xuan_xiang`、`ti_mu_jie_xi`、`ti_mu_zhi_shi_dian`、`ti_mu_fu_jian`、`ti_mu_lai_yuan`、`ti_mu_shen_he_ji_lu`。
- 系统表：`flyway_schema_history`。
- 数据量：3题、12选项、3标准解析、1附件、9分项来源、3审核轨迹。
- 三道题和三条标准解析全部为`PENDING`；正式30题未导入。

## 明确未实施

以下内容保持NOT_STARTED：

- 题库CRUD、导入API和人工审核API
- 用户、角色、学生、教师、班级和任课关系
- 登录、注册和JWT
- 练习、判分、错题和学习统计
- AI Provider、DeepSeek、Redis、MinIO、WebSocket、Docker和微服务
- 任何前端页面变更
- 完整30题导入

## 本轮测试结论

- `mvn clean test`：PASS，8 tests，0 failures，0 errors。
- `mvn test -Dtest=QuestionDatabaseModelTest`：PASS，4 tests。
- `mvn clean package`：PASS，8 tests并生成可执行JAR。
- Flyway迁移：PASS，V1-V4实际执行/校验。
- 数据库表范围：PASS，10张业务表与清单完全一致。
- MyBatis-Plus查询：PASS。
- 三科样本及PENDING状态：PASS。
- 自动判分例外：PASS，事务测试证明`shi_fou_ke_zi_dong_pan_fen=0`可保存。
- 难度约束：PASS，数据库拒绝难度4。
- 附件对象位置：PASS，F107记录位置等于解析正文中的实际位置。
- 打包JAR健康检查：PASS，临时地址`http://localhost:18082/api/v1/health`返回UP/UP，验证后进程已停止。

## 已知事项

- 首次测试只加入`flyway-core`，在Spring Boot 4.1模块化结构下没有触发自动配置，数据库保持为空；改用官方`spring-boot-starter-flyway`后迁移成功。该失败没有手工建表。
- 第二次测试仅因MySQL检查约束错误在Spring JDBC中被翻译为更通用的`DataAccessException`而断言失败；约束本身已正确拒绝非法难度。调整测试断言后完整复测通过。
- MySQL首次执行包含`TINYINT(1)`的迁移时提示整数显示宽度弃用；不影响MySQL 8.4约束和读写。已执行迁移不可静默改写，后续新迁移可逐步改为不带显示宽度的`TINYINT`。
- 默认端口8081在一次JAR验证时曾被PID 33576的现存Java进程占用，本轮没有终止来源不明的进程；改用18082完成验证并只停止本轮进程。最终端口复查时8081已自行释放。
- JDK 25下Mockito/Byte Buddy仍输出动态Agent兼容性预警，测试实际通过。
- `ti_mu_lai_yuan.quan_li_zhuang_tai`暂为`COPYRIGHT_UNKNOWN`，在版权人工核验前不得发布样本。

## 下一步

本轮已停止。未开始题库API、完整导入或前端页面；下一任务由用户另行指定。
