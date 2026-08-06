# AI 开发交接

更新时间：2026-08-06

## 当前状态

当前分支为 `feat/student-practice-loop`，开始基线为 `main@4f10f6486de8f4d732abb6e52eeca7734bc3dfde`。PR #10、#11、#12 均已普通 merge；本分支尚未合并。

本轮新增 V7 的学生练习、正式答题、结果和错题聚合模型。V1–V6、MVP30 原始 Excel 和既有管理员模块均未改动。当前后端 61/61、前端 64/64 自动化测试通过；真实临时库浏览器联调因自动环境拒绝启动后台服务而为 `NOT_RUN`，不得写成 PASS。

## 继续时必须保持

- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 的单选、多选、填空；不进入主观题。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- 不引入 AI Provider、AI 判分、掌握度、推荐、教师任务、WebSocket、Redis 或附件修复。

## 重要文档

- [学生练习 API](STUDENT_PRACTICE_API.md)
- [学生练习前端](STUDENT_PRACTICE_FRONTEND.md)
- [数据库模型](QUESTION_DATABASE_MODEL_V1.md)
- [开发状态](DEVELOPMENT_STATUS.md)

## 后续唯一动作

完成本轮真实临时库浏览器验收、完整回归、提交、普通 push 与 Draft PR 后停止，等待独立审查。未经审查不得创建下一模块分支。
