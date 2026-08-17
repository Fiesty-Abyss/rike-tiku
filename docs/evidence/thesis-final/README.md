# 论文最终匿名截图清单（01–42）

本目录是 PR #33 唯一论文插图目录，包含连续、无歧义的 01–42 文件及机器检查清单 `browser-results-pr33.json`。PR #32 的旧编号别名已迁至 [`../pr32-thesis-history`](../pr32-thesis-history/README.md)，没有删除审计证据。

- 桌面视口 1440×1000；移动视口 390×844；页面全长截图的实际像素高度可大于视口。
- “Demo API”表示访问匿名 `rike_tiku_demo` 的真实后端接口，不表示调用了真实 DeepSeek、GLM 或 Search Provider。
- 09/10 使用浏览器确定性 UI 夹具，仅验证结构化题目、选项、提交与确定性判分，不记作真人或真实 Provider PASS。
- 所有 PNG/SVG 均非零字节；逐图敏感信息检查未发现真实姓名、Key、JWT、数据库密码、本地绝对路径或 DevTools 请求头。
- 全部证据均为机器浏览器/仓库图，不属于真人验收；统一状态为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。

| 编号 / 文件 | 功能 | 路由 | 视口 | 数据来源 | Demo API / UI 夹具 | 真人验收 |
|---|---|---|---|---|---|---|
| `01-portal-desktop.png` | 公共 Portal | `/` | 1440×1000 | 公共静态页面 | 无需业务 API | 否 |
| `02-login.png` | 登录、CAPTCHA、密码恢复入口 | `/login` | 1440×1000 | 匿名 Demo CAPTCHA API | Demo API；未提交真人凭据 | 否 |
| `03-student-dashboard.png` | 学生三科学习首页 | `/student` | 1440×1000 | 匿名 Demo 学生与统计 | Demo API | 否 |
| `04-practice.png` | 在线练习 | `/student/practice/15` | 1440×1000 | 匿名 Demo 冻结练习 | Demo API | 否 |
| `05-result-standard.png` | 确定性判分与 STANDARD | `/student/practice/15/result` | 1440×1000 | 匿名 Demo 答题与已审核解析 | Demo API；判分不调用 AI | 否 |
| `06-wrong-questions.png` | 错题本 | `/student/wrong-questions` | 1440×1000 | 匿名 Demo 错题记录 | Demo API | 否 |
| `07-student-ai-analysis.png` | AI 错因分析 | 练习结果页 AI Drawer | 1440×1000 | 匿名 Demo 已有分析证据 | Demo API；本轮未调用真实 Provider | 否 |
| `08-student-ai-chat.png` | 当前题 10 轮答疑及模型/思考/搜索控件 | 练习结果页 AI Drawer | 1440×1000 | 匿名 Demo 已有会话证据 | Demo API；本轮未调用真实 Provider | 否 |
| `09-student-ai-variant.png` | 学生结构化变式题 | 练习结果页变式 Drawer | 1440×1000 | 确定性结构化响应 | UI 夹具；非真实 Provider | 否 |
| `10-student-ai-variant-result.png` | 变式题确定性判分结果 | 练习结果页变式 Drawer | 1440×1000 | 确定性结构化响应与前端作答 | UI 夹具；非真实 Provider | 否 |
| `11-teacher-workspace.png` | 教师任课工作台 | `/teacher/scopes/1` | 1440×1000 | 匿名 Demo 任课范围 | Demo API | 否 |
| `12-teacher-ai-review.png` | 教师候选题生成与人工审核区 | `/teacher/ai-generation` | 1440×1000 | 匿名 Demo 候选数据 | Demo API；未调用真实 Provider | 否 |
| `13-teacher-paper-builder.png` | 手动/规则组卷 | `/teacher/papers` | 1440×1000 | 匿名 Demo PUBLISHED 题库 | Demo API | 否 |
| `14-paper-student-preview.png` | 学生版试卷预览 | `/teacher/papers/1/student` | 1440×1000 | 匿名 Demo 冻结试卷 | Demo API | 否 |
| `15-paper-answer-preview.png` | 答案解析版试卷 | `/teacher/papers/1/answer` | 1440×1000 | 匿名 Demo 冻结答案与 STANDARD | Demo API | 否 |
| `16-admin-dashboard.png` | 管理员总览 | `/admin` | 1440×1000 | 匿名 Demo 聚合统计 | Demo API | 否 |
| `17-admin-ai-models.png` | TEXT / VISION / SEARCH 模型配置 | `/admin/ai-models` | 1440×1000 | 匿名 Demo 安全配置元数据 | Demo API；Key 仅显示配置状态 | 否 |
| `18-admin-password-notifications.png` | 密码恢复通知 | `/admin/password-recovery` | 1440×1000 | 匿名 Demo 请求记录 | Demo API | 否 |
| `19-admin-ai-generation.png` | 管理员候选题与审核 | `/admin/ai-generation` | 1440×1000 | 匿名 Demo 候选数据 | Demo API；未调用真实 Provider | 否 |
| `20-portal-mobile.png` | 移动 Portal | `/` | 390×844 | 公共静态页面 | 无需业务 API | 否 |
| `21-student-mobile.png` | 移动学生首页 | `/student` | 390×844 | 匿名 Demo 学生与统计 | Demo API | 否 |
| `22-system-architecture.svg` | 系统架构 | 非页面路由 | 1600×900 SVG | 当前模块与运行依赖 | 仓库事实绘图 | 否 |
| `23-ai-controlled-flow.svg` | AI 受控业务流 | 非页面路由 | 1600×900 SVG | 当前 AI / STANDARD / 审核边界 | 仓库事实绘图 | 否 |
| `24-database-modules.svg` | V29 数据库模块 | 非页面路由 | 1600×900 SVG | Flyway V1–V29 与 50 表 | 仓库事实绘图 | 否 |
| `25-password-recovery-desktop.png` | 密码恢复桌面弹窗 | `/login` | 1440×900 | 匿名 Demo CAPTCHA | Demo API | 否 |
| `26-password-recovery-mobile.png` | 密码恢复移动弹窗 | `/login` | 390×844 | 匿名 Demo CAPTCHA | Demo API | 否 |
| `27-wrong-question-review.png` | 错题筛选、再做与归档 | `/student/wrong-questions` | 1440×900 | 匿名 Demo 错题事实 | Demo API | 否 |
| `28-topic-units.png` | 专题单元与附件 | `/student/topics` | 1440×900 | 匿名 Demo 专题 | Demo API | 否 |
| `29-student-papers.png` | 学生试卷任务 | `/student/papers` | 1440×900 | 匿名 Demo 发布事实 | Demo API | 否 |
| `30-knowledge-cards.png` | 学生知识卡片 | `/student/knowledge-cards` | 1440×900 | 匿名 Demo 已审核卡片 | Demo API | 否 |
| `31-message-actions.png` | 私信与低干扰操作入口 | `/messages/:id` | 1440×900 | 匿名 Demo 会话 | Demo API | 否 |
| `32-private-question-bank.png` | 教师班级私有题库 | `/teacher/private-questions` | 1440×900 | 匿名 Demo 任课范围 | Demo API | 否 |
| `33-teacher-knowledge-cards.png` | 教师知识卡片审核 | `/teacher/knowledge-cards` | 1440×900 | 匿名 Demo 卡片 | Demo API | 否 |
| `34-paper-publish-quality.png` | 试卷发布与 AI 质量评估入口 | `/teacher/papers` | 1440×900 | 匿名 Demo；AI 未真实调用 | Demo API | 否 |
| `35-operation-log-search.png` | 操作日志分页检索 | `/admin/operation-logs` | 1440×900 | 匿名 Demo 安全元数据 | Demo API | 否 |
| `36-question-import.png` | 19 列题库导入 | `/admin/question-import` | 1440×900 | 匿名 Demo | Demo API | 否 |
| `37-student-import.png` | 7 列学生导入 | `/admin/student-import` | 1440×900 | 匿名 Demo | Demo API | 否 |
| `38-vision-provider-config.png` | GLM / xAI 显式配置 | `/admin/ai-models` | 1440×900 | 无 Key 的安全配置元数据 | Demo API；无真实 Provider | 否 |
| `39-question-review.png` | 管理员题目审核 | `/admin/questions` | 1440×900 | 匿名 Demo | Demo API | 否 |
| `40-message-menu.png` | 消息操作菜单 | `/messages/:id` | 1440×900 | 匿名 Demo 会话 | Demo API | 否 |
| `41-message-recall-confirm.png` | 居中撤回确认 | `/messages/:id` | 1440×900 | 匿名 Demo 会话 | Demo API | 否 |
| `42-message-delete-confirm.png` | 居中仅本人删除确认 | `/messages/:id` | 1440×900 | 匿名 Demo 会话 | Demo API | 否 |

`browser-results-pr33.json` 记录 17 条基础路线；[`../pr33-final-browser/browser-results-v29.json`](../pr33-final-browser/browser-results-v29.json) 记录 14 条新增路线与消息交互。合计 31 条机器检查均为 0 console error、0 page error、0 unexpected failed request、0 horizontal overflow、0 missing assertion。该 JSON 与 09/10 夹具都不得解释为真人验收或真实 Provider PASS。
