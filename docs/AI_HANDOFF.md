# AI 开发交接

更新时间：2026-08-06

## 当前状态

当前分支为 `feat/demo-data-manual-acceptance`，开始基线为 `main@2161080427fd432634325bea3c3d1ebd7e0f519a`。PR #10、#11、#12、#13 均已普通 merge；本演示环境分支尚未合并。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。当前 Flyway 为 V1–V7，共 23 张业务表；V1–V6 和 MVP30 原始 Excel 未改动。合并后后端 68/68、前端 68/68 自动化测试通过，真实 HTTP 验证为 `PASS`，学生页面回查为 `NOT_RUN`，综合结论为 `PASS_WITH_ENV_LIMITATION`。

当前分支增加独立 `rike_tiku_demo` 的显式重建工具、三角色账号、教学组织、九个知识点和 18 道无附件演示题。它不使用 Flyway 承载演示数据，不公开 seed 接口，也不在正常启动时执行。用户下一步应先阅读 `DEMO_ENVIRONMENT.md`，再按 `MANUAL_ACCEPTANCE_CHECKLIST.md` 人工验收，并把真实问题登记到 `MANUAL_ACCEPTANCE_FINDINGS.md`。

当前分支验证：后端 72/72、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。`reset → seed → validate → clean → reset → seed` 真实执行 PASS，正式库未出现演示账号、演示题或学习记录。

## 继续时必须保持

- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取可真正冻结的 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 单选、多选、填空：要求有效版本 1 STANDARD 解析、活动知识点、足够选项和合法答案 JSON；首版排除活动附件及图片/公式对象标记，不进入主观题。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- 不引入 AI Provider、AI 判分、掌握度、推荐、教师任务、WebSocket、Redis 或附件修复。

## 重要文档

- [学生练习 API](STUDENT_PRACTICE_API.md)
- [学生练习前端](STUDENT_PRACTICE_FRONTEND.md)
- [数据库模型](QUESTION_DATABASE_MODEL_V1.md)
- [开发状态](DEVELOPMENT_STATUS.md)

## 当前下一步

先完成人工验收和同一 PR 内的问题修正，不启动新业务模块。AI Provider、DeepSeek、GLM、AI 答疑、掌握度和推荐均未实现；教师正式业务工作台也未实现。
