# 论文最终匿名截图证据

来源：PR #33 最终分支。UI 图片取自匿名 `rike_tiku_demo` 机器浏览器证据，架构图根据当前代码与 Flyway V19 绘制为可编辑 SVG。

- 视口：桌面 1440×1000（页面全长截图可能更高）；移动端 390×844。
- 敏感门禁：逐图目视检查，无本机真实姓名、API Key、JWT、数据库密码、绝对路径或开发者工具敏感头。
- 管理员 AI 模型页只显示 Key 掩码和 `已配置`，不显示完整值。
- 图片中的“演示学生”“demo_admin”属于匿名 Demo 身份，不是本机正式人员。
- 这些是机器控制证据，不等同于正式用户人工验收；当前仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。
- `browser-results-pr33.json` 记录 17 条真实路由：0 console error、0 page error、0 failed request、0 horizontal overflow、0 missing assertion。
- 09/10 为确定性拦截的结构化变式响应，用于验证 UI 题目/选项/判分状态；不记作真实 Provider smoke。其余路由使用 Demo 真实 API。

| 编号 | 内容 | 文件 |
|---:|---|---|
| 01—06 | Portal、登录、学生首页、练习、结果与 STANDARD、错题 | `01`—`06` PNG |
| 07—10 | AI 错因、答疑、变式题、变式结果 | `07`—`10` PNG |
| 11—15 | 教师工作台、候选审核、组卷、学生版与答案版试卷 | `11`—`15` PNG |
| 16—21 | 管理端、模型、密码通知、候选审核、Portal 与学生移动端 | `16`—`21` PNG |
| 22—24 | 系统架构、受控 AI 流、数据库模块 | `22`—`24` SVG |
