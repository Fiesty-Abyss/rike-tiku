# AI 开发交接

> 当前接续分支：`feat/ui-auth-student-dashboard`（未合并）。已实现统一登录、后端滑块挑战、主动修改密码、学生三科工作台、教师任教范围与中文显示层。2026-08-07 已完成 `rike_tiku_demo` 真实浏览器验收，MA-002 至 MA-005 已关闭；MA-006 至 MA-009、DeepSeek、GLM、AI、MVP30 正式入库、教师正式业务工作台均未实现。不得修改 V1–V7。

更新时间：2026-08-06

## 当前状态

当前分支为 `feat/ui-auth-student-dashboard`，尚未合并。PR #10、#11、#12、#13、#14 均已普通 merge；PR #14 合并提交为 `4ffbcbda66f26e7390192985ce179f30d3a6b664`。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。当前 Flyway 为 V1–V7，共 23 张业务表；V1–V6 和 MVP30 原始 Excel 未改动。合并后后端 68/68、前端 68/68 自动化测试通过，真实 HTTP 验证为 `PASS`，学生页面回查为 `NOT_RUN`，综合结论为 `PASS_WITH_ENV_LIMITATION`。

`main` 已包含独立 `rike_tiku_demo` 的显式重建工具、三角色账号、教学组织、九个知识点和 18 道无附件演示题。它不使用 Flyway 承载演示数据，不公开 seed 接口，也不在正常启动时执行。人工验收步骤与真实反馈分别记录在 `MANUAL_ACCEPTANCE_CHECKLIST.md` 和 `MANUAL_ACCEPTANCE_FINDINGS.md`。

PR #14 合并后验证：后端 74/74、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。完整脚本链及三角色真实 HTTP smoke PASS，正式库未出现演示账号、演示题或学习记录。

人工验收问题 MA-001 的根因不是账号或 BCrypt：IDEA 默认连接 `rike_tiku`，而演示账号只在 `rike_tiku_demo`。PR #14 已修复脚本使用的 `RIKE_TIKU_BACKEND_PORT`、`RIKE_TIKU_CORS_ALLOWED_ORIGINS` 和带 `/api/v1` 的前端 API 地址，并补充 IDE 配置与真实 HTTP smoke。demo_admin 登录、demo_teacher 真实 HTTP 登录和 demo_student 浏览器登录均已复验，MA-001 已关闭。MA-002 至 MA-009 是用户登记的待规划反馈，尚未实现。

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

PR #14 已合并。后续顺序为：先处理 UI、认证和学生三科工作台；再补管理员学生手动管理；随后规划高频考点和受三元任课关系约束的师生私信；最后才接入 DeepSeek 与 GLM。以上均未实现，不得提前写成完成状态。AI 答疑、掌握度、推荐和教师正式业务工作台也仍未实现。
