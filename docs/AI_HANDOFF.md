# AI 开发交接

> 当前接续分支：`feat/final-demo-question-bank`，基于 `main@23c5d79f3c23e71563d341a66bfe2fd4fce03a64`。PR #22 已普通 merge（merge commit `67b7bd7239e2ac1de3ad8c71b82b6d0a79162d3b`）；当前 Flyway V1–V10、26 张业务表，本轮不新增迁移。

更新时间：2026-08-09

## 当前状态

PR #10 至 PR #22 均已普通 merge；PR #22 merge commit 为 `67b7bd7239e2ac1de3ad8c71b82b6d0a79162d3b`，远程 `feat/personal-center` 已删除。

PR #19 已用 JDK 原生生成的 4 位随机 PNG 图形验证码替换 PR #15 历史滑块并进入 `main`。登录页默认不显示验证码；首次点击/Enter 只展开，第二次才提交。challenge 在内存保存两分钟并一次性消费，不新增 Redis、第三方依赖、数据库表或 Flyway。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。PR #18 新增 V8 高频考点表，PR #20 新增 V9 两张私信表，PR #22 新增只 ALTER `yong_hu` 的 V10；当前 Flyway 为 V1–V10，共 26 张业务表，MVP30 原始 Excel 未改动。历史 PR #13 自动化为后端 68/68、前端 68/68；PR #15 合并后自动化为后端 79/79、前端 72/72，打包、类型检查、构建、依赖审计与完整浏览器验收均已通过。

`main` 中的 `rike_tiku_demo` 显式重建工具保留原 smoke 数据，并形成 199/200 双班级场景：14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系及 Demo90。PR #23 保留 Demo90，并从 54 个原创候选中筛选 30 道变式，当前目标总题量 120；PR #18 的 12 条 ACTIVE 高频考点保持不变。演示数据不使用 Flyway 承载，不公开 seed 接口，也不在正常启动时执行。

Demo90 与 PR #23 筛选变式均是项目原创演示内容。V3.0 没有规定名为 MVP30 的 Excel 必须整体正式入库；MVP30 原始文件保持不变，定位为 PR #12 已验证的结构化导入能力素材。开发阶段由 Codex 辅助制作候选不等于系统实现运行时 AI 出题。

历史 PR #14 合并后验证：后端 74/74、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。完整脚本链及三角色真实 HTTP smoke PASS，正式库未出现演示账号、演示题或学习记录。

历史 PR #18 合并后后端 87/87、前端 83/83，package、type-check、build、audit 和 Demo `reset → seed → validate → smoke` 均通过；当时真实浏览器工作台/高频考点隔离验收通过，MA-009 尚未完成。

PR #19 合并后回归为后端 90/90、前端 91/91，package、type-check、build PASS，生产依赖 audit 为 0。Demo `reset → seed → validate → smoke` PASS；smoke 只在显式 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=true` 的本地演示后端读取测试值，正式默认关闭且没有免验证码登录入口。真实浏览器验收保持通过；正式库只读复查 Demo90、场景账号、场景班级和高频考点仍均为 0。

PR #20 已普通 merge 并删除远程功能分支。V9 新增两张私信表，V1–V8 未修改；师生私信由 ACTIVE 三元任课关系和学生当前主班级共同约束。合并后后端 92/92、前端 100/100，package、type-check、build、audit 0 和 Demo `reset → seed → validate → smoke` 均通过；199 学生↔物理教师、200 学生↔化学教师双向浏览器验收及伪造会话隔离保持通过，MA-009 已关闭。

PR #21 已普通 merge 进入 `main`，实时复用 V7 答题事实和冻结知识点快照计算 NOT_STARTED、INSUFFICIENT、WEAK、IMPROVING、MASTERED。当前学科全部 ACTIVE 知识点参与掌握度和总体统计；5 题推荐资格独立复用 StudentPracticeService 的真实题池规则，题量不足不会隐藏历史掌握事实，也不会生成无法创建的推荐。学生端“开始巩固”复用现有练习创建；教师端只按本人 ACTIVE scope 查看班内当前学科汇总。该能力是确定性规则统计，不是 AI；未新增 V10、缓存或统计表。MA-014、MA-015 已关闭。

合并后学习掌握专项 6/6、后端 98/98、前端 29 文件 106/106，package、type-check、build、生产依赖 audit 0 及 Demo `reset → seed → validate → smoke` 均通过。正式 `rike_tiku` 只读检查为 Flyway V9、26 张业务表，Demo90、场景账号、场景班级、高频考点、私信会话、私信消息与 V7 学习记录均为 0。轻量浏览器复查确认学生掌握页、5 题推荐预选、教师班级学情正常，控制台 0 error；原 15 题浏览器证据继续作为历史验收记录。

MA-013 的旧测试数据源问题已关闭。合并前使用全新随机临时库验证：临时库与正式库 V9 script 一致，checksum 均为 `1192958817`、success 均为 1；正式库两张表为空。PR #20 合并后，正式库提前执行的 V9 已与 main 正式迁移基线一致，两张 V9 结构表不属于业务数据污染；Demo90、场景账号、场景班级、高频考点、会话和消息仍均为 0。

PR #22 已将统一 `/profile` 与 `GET/PUT /api/v1/profile`、`POST/DELETE /api/v1/profile/avatar` 普通 merge 进入 `main`。学生、教师、管理员和 ADMIN+TEACHER 多角色账号均只读取本人真实资料；简介上限 500 字。头像只接受不超过 2 MB、可由 ImageIO 真实解析且 MIME/文件内容一致的 PNG/JPEG，MySQL 保存二进制，前端显示 data URL。业务档案、角色、用户名和组织关系不可在个人中心修改，主动改密继续复用现有接口；首次登录门禁不豁免 profile，必须先完成初始密码修改。

PR #22 合并后验证为后端 102/102、前端 31 文件 117/117，package、type-check、build、audit 0、Demo reset/seed/validate/smoke 均 PASS。真实浏览器完整证据覆盖三角色与 `demo_physics_admin` 多角色资料、简介、头像上传/刷新持久化/删除、现有改密入口；门禁修正后轻量抽查正常，控制台 0 error。MA-006 已关闭。正式库为 V10、26 张业务表，Demo90、场景账号、场景班级、高频考点、私信及 V7 学习记录均为 0。

PR #23 当前专项 27/27、后端 105/105、前端 31 文件 117/117，package、type-check、build、生产依赖 audit 0 及 Demo `reset → seed → validate → smoke` 均 PASS。正式库只读检查为 V10、26 张业务表，演示题与全部场景业务数据为 0；MVP30 原始文件 SHA-256 保持 `01E90ACFDFB8EF5194103C3B7DD1A99B4F351858FFFDF70CFF63187928DCAB17`。

PR #23 浏览器仅操作 `rike_tiku_demo`：物理 3、化学 3、生物 4 道新变式真实进入随机练习；Unicode 化学式、三题型控件、提交前防泄露、结果答案与 STANDARD 解析、错题和掌握度均正常。同科连续随机题集不同，控制台 0 error；最终 reset/seed/validate 已清理验收会话并恢复固定 120 题状态。

## 继续时必须保持

- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取可真正冻结的 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 单选、多选、填空：要求有效版本 1 STANDARD 解析、活动知识点、足够选项和合法答案 JSON；首版排除活动附件及图片/公式对象标记，不进入主观题。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- Entity 只对应数据库，不直接暴露密码摘要、逻辑删除等内部字段；API 输入优先 `XxxRequest`，输出优先 `XxxResponse`，可统一放入 `dto` 包。
- 不机械创建 VO、Converter、Assembler；只有真正独立的页面展示模型才考虑 VO。
- 不把规则掌握度和推荐描述为 AI；不引入 AI Provider、AI 判分、教师任务、WebSocket、Redis 或附件修复。
- 图形验证码测试值只允许在自动化或独立本地 Demo 中显式开启；正式运行保持 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=false`。

## 重要文档

- [学生练习 API](STUDENT_PRACTICE_API.md)
- [学生练习前端](STUDENT_PRACTICE_FRONTEND.md)
- [数据库模型](QUESTION_DATABASE_MODEL_V1.md)
- [开发状态](DEVELOPMENT_STATUS.md)
- [管理员学生管理 API](ADMIN_STUDENT_MANAGEMENT_API.md)
- [管理员学生管理前端](ADMIN_STUDENT_MANAGEMENT_FRONTEND.md)
- [师生私信 API](TEACHER_STUDENT_MESSAGING_API.md)
- [师生私信前端](TEACHER_STUDENT_MESSAGING_FRONTEND.md)
- [知识点掌握度与规则推荐](LEARNING_MASTERY_RULE_RECOMMENDATION.md)
- [个人中心 API](PERSONAL_CENTER_API.md)
- [个人中心前端](PERSONAL_CENTER_FRONTEND.md)
- [最终演示题库](FINAL_DEMO_QUESTION_BANK.md)
- [变式候选审核记录](DEMO_VARIANT_QUESTION_REVIEW.md)

## 当前下一步

PR #23 是当前唯一任务。完成测试、Demo 与浏览器验证后只创建 Draft PR #23；不得开始下一分支、AI Provider 或运行时 AI 出题。
