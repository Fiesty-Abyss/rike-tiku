# AI 开发交接

> 当前接续分支：`main`。PR #20 已普通 merge，merge commit 与合并业务基线 main HEAD 为 `1055dee567b7afa153750792670fb0bafed1151c`；当前 Flyway V1–V9、26 张业务表。

更新时间：2026-08-08

## 当前状态

PR #10 至 PR #19 均已普通 merge；PR #19 merge commit 为 `0a12943e901e844520e3801264fa4a43590ff28e`，远程 `feat/login-image-captcha` 已删除。

PR #19 已用 JDK 原生生成的 4 位随机 PNG 图形验证码替换 PR #15 历史滑块并进入 `main`。登录页默认不显示验证码；首次点击/Enter 只展开，第二次才提交。challenge 在内存保存两分钟并一次性消费，不新增 Redis、第三方依赖、数据库表或 Flyway。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。PR #18 新增 V8 高频考点表，当前 Flyway 为 V1–V8，共 24 张业务表；V1–V7 和 MVP30 原始 Excel 未改动。历史 PR #13 自动化为后端 68/68、前端 68/68；PR #15 合并后自动化为后端 79/79、前端 72/72，打包、类型检查、构建、依赖审计与完整浏览器验收均已通过。

`main` 中的 `rike_tiku_demo` 显式重建工具保留原 smoke 数据，并形成 199/200 双班级场景：14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系及 Demo90。PR #18 另预置 12 条 ACTIVE 高频考点。它不使用 Flyway 承载演示数据，不公开 seed 接口，也不在正常启动时执行。

Demo90 是“本科毕业设计自编演示题”，不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题没有因此变为 `PUBLISHED`。PR #18 已普通 merge，教师班级学科工作台和高频考点读写隔离已进入 `main`。

历史 PR #14 合并后验证：后端 74/74、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。完整脚本链及三角色真实 HTTP smoke PASS，正式库未出现演示账号、演示题或学习记录。

历史 PR #18 合并后后端 87/87、前端 83/83，package、type-check、build、audit 和 Demo `reset → seed → validate → smoke` 均通过；当时真实浏览器工作台/高频考点隔离验收通过，MA-009 尚未完成。

PR #19 合并后回归为后端 90/90、前端 91/91，package、type-check、build PASS，生产依赖 audit 为 0。Demo `reset → seed → validate → smoke` PASS；smoke 只在显式 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=true` 的本地演示后端读取测试值，正式默认关闭且没有免验证码登录入口。真实浏览器验收保持通过；正式库只读复查 Demo90、场景账号、场景班级和高频考点仍均为 0。

PR #20 已普通 merge 并删除远程功能分支。V9 新增两张私信表，V1–V8 未修改；师生私信由 ACTIVE 三元任课关系和学生当前主班级共同约束。合并后后端 92/92、前端 100/100，package、type-check、build、audit 0 和 Demo `reset → seed → validate → smoke` 均通过；199 学生↔物理教师、200 学生↔化学教师双向浏览器验收及伪造会话隔离保持通过，MA-009 已关闭。

MA-013 的旧测试数据源问题已关闭。合并前使用全新随机临时库验证：临时库与正式库 V9 script 一致，checksum 均为 `1192958817`、success 均为 1；正式库两张表为空。PR #20 合并后，正式库提前执行的 V9 已与 main 正式迁移基线一致，两张 V9 结构表不属于业务数据污染；Demo90、场景账号、场景班级、高频考点、会话和消息仍均为 0。

## 继续时必须保持

- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取可真正冻结的 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 单选、多选、填空：要求有效版本 1 STANDARD 解析、活动知识点、足够选项和合法答案 JSON；首版排除活动附件及图片/公式对象标记，不进入主观题。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- Entity 只对应数据库，不直接暴露密码摘要、逻辑删除等内部字段；API 输入优先 `XxxRequest`，输出优先 `XxxResponse`，可统一放入 `dto` 包。
- 不机械创建 VO、Converter、Assembler；只有真正独立的页面展示模型才考虑 VO。
- 不引入 AI Provider、AI 判分、掌握度、推荐、教师任务、WebSocket、Redis 或附件修复。
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

## 当前下一步

PR #20 已完成普通 merge 和合并后回归。当前停止，不创建 PR #21；不得开始掌握度、推荐、DeepSeek、GLM 或 AI。
