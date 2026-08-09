# 学生练习前端

本模块已通过普通 merge 进入 `main`（PR #13，合并提交 `db04fbc9caeeb5e4eb003a45581e62e76dbab420`）。合并后前端 68/68 测试、类型检查和构建通过，`npm audit` 为 0 vulnerabilities。真实 HTTP 验证为 `PASS`，学生页面回查为 `NOT_RUN`，综合结论为 `PASS_WITH_ENV_LIMITATION`。学生页面不把题目、答题内容或练习结果写入 `localStorage`、`sessionStorage` 或控制台。

| 路由 | 页面 | 行为 |
|---|---|---|
| `/student` | 学生工作台 | 进入练习或错题本。 |
| `/student/practice`、`/student/practice/new` | 创建练习 | 科目、知识点、题型、难度、数量筛选。 |
| `/student/practice/:id` | 作答 | 单选、多选、填空；以组件内存累计每题用时，刷新后重新计时。 |
| `/student/practice/:id/result` | 结果 | 提交后展示正确数、分数、本人答案、正确答案和标准解析。 |
| `/student/wrong-questions` | 错题本 | 展示聚合状态并打开安全详情。 |

前端在提交前检查未答题并二次确认；后端仍是唯一判分、状态与越权校验来源。`401`、`403`、题库不足、重复提交等业务错误使用中文提示。结果与错题答案会按题型格式化，不直接展示答案 JSON。题干和选项图片、提交后标准解析图片统一由 `QuestionContent` 通过带 JWT 的 Blob 请求加载；组件卸载时 revoke Blob URL，组件按正文完整 marker 提取 I001/F107 等对象 ID 后匹配附件，403/404/损坏附件显示“图片附件暂不可用”占位。未提交页面不显示 STANDARD_ANALYSIS 图片。
# 当前分支补充（未合并）

学生主页升级为物理、化学、生物三科工作台。`/student/subjects/physics`、`/student/subjects/chemistry`、`/student/subjects/biology` 提供随机五题、条件练习和本学科错题入口。创建页支持 `subjectId`、`knowledgePointId` query 预选；错题页支持 `subjectId` query 筛选，不新增或伪造学习统计。

MA-017 附件机器验证已覆盖学生题干图片、提交后 STANDARD_ANALYSIS 图片、结果/错题上下文和 Blob URL 生命周期；未提交页面不显示标准解析图片。当前状态为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`，真实 CAPTCHA 与视觉验收统一延期至非 AI 最终集成验收。
