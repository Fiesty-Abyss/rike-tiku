# 学生练习前端

当前分支：`feat/student-practice-loop`（尚未合并）。学生页面不把题目、答题内容或练习结果写入 `localStorage`、`sessionStorage` 或控制台。

| 路由 | 页面 | 行为 |
|---|---|---|
| `/student` | 学生工作台 | 进入练习或错题本。 |
| `/student/practice`、`/student/practice/new` | 创建练习 | 科目、知识点、题型、难度、数量筛选。 |
| `/student/practice/:id` | 作答 | 单选、多选、填空；页面刷新后由会话接口重新获取冻结题面。 |
| `/student/practice/:id/result` | 结果 | 提交后展示正确数、分数、本人答案、正确答案和标准解析。 |
| `/student/wrong-questions` | 错题本 | 展示聚合状态并打开安全详情。 |

前端在提交前检查未答题并二次确认；后端仍是唯一判分、状态与越权校验来源。`401`、`403`、题库不足、重复提交等业务错误使用中文提示。附件仅展示文件名、位置、对象标识和状态，不展示本机路径。
