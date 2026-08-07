# AI 开发交接

> 当前接续分支：`main`。PR #16 已普通 merge，merge commit 为 `588db6ee5b2a6c466c618249f072591af47609a1`。独立 `rike_tiku_demo` 的 Demo90 已进入 `main`；Flyway 保持 V1–V7（23 张业务表），PR #16 未修改既有迁移或新增迁移。

更新时间：2026-08-07

## 当前状态

当前分支为 `main`。PR #10 至 PR #16 均已普通 merge；PR #16 merge commit 为 `588db6ee5b2a6c466c618249f072591af47609a1`。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。当前 Flyway 为 V1–V7，共 23 张业务表；既有迁移和 MVP30 原始 Excel 未改动。历史 PR #13 自动化为后端 68/68、前端 68/68；PR #15 合并后自动化为后端 79/79、前端 72/72，打包、类型检查、构建、依赖审计与完整浏览器验收均已通过。

`main` 中的 `rike_tiku_demo` 显式重建工具包含三角色账号、教学组织、九个演示知识点和 90 道无附件原创演示题。每科 30 道，每科三题型、三档难度、三个演示知识点各 10 道；化学式、电荷和科学计数法使用稳定 Unicode，STANDARD 解析只保留学科说明。它不使用 Flyway 承载演示数据，不公开 seed 接口，也不在正常启动时执行。

Demo90 是“本科毕业设计自编演示题”，不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题没有因此变为 `PUBLISHED`。本轮后续业务候选仍只有“管理员学生完整管理与账号恢复”（`PLANNED`）。

历史 PR #14 合并后验证：后端 74/74、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。完整脚本链及三角色真实 HTTP smoke PASS，正式库未出现演示账号、演示题或学习记录。

人工验收问题 MA-001 的根因不是账号或 BCrypt：IDEA 默认连接 `rike_tiku`，而演示账号只在 `rike_tiku_demo`。PR #14 已修复脚本使用的 `RIKE_TIKU_BACKEND_PORT`、`RIKE_TIKU_CORS_ALLOWED_ORIGINS` 和带 `/api/v1` 的前端 API 地址，并补充 IDE 配置与真实 HTTP smoke。MA-001 至 MA-005、MA-010、MA-011 已关闭；MA-006 至 MA-009 尚未完成。MA-006 仅剩个人资料、个人简介和头像，主动修改密码已实现。

## 继续时必须保持

- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取可真正冻结的 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 单选、多选、填空：要求有效版本 1 STANDARD 解析、活动知识点、足够选项和合法答案 JSON；首版排除活动附件及图片/公式对象标记，不进入主观题。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- Entity 只对应数据库，不直接暴露密码摘要、逻辑删除等内部字段；API 输入优先 `XxxRequest`，输出优先 `XxxResponse`，可统一放入 `dto` 包。
- 不机械创建 VO、Converter、Assembler；只有真正独立的页面展示模型才考虑 VO。
- 不引入 AI Provider、AI 判分、掌握度、推荐、教师任务、WebSocket、Redis 或附件修复。

## 重要文档

- [学生练习 API](STUDENT_PRACTICE_API.md)
- [学生练习前端](STUDENT_PRACTICE_FRONTEND.md)
- [数据库模型](QUESTION_DATABASE_MODEL_V1.md)
- [开发状态](DEVELOPMENT_STATUS.md)

## 当前下一步

唯一下一步候选为“管理员学生完整管理与账号恢复”，状态为 `PLANNED`。不得启动或把高频考点、私信、DeepSeek、GLM、AI、教师正式业务工作台写成已完成。
