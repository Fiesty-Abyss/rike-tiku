# 最终人体工学与易用性复查

状态：`PASS_WITH_NOTES`

性质：machine-controlled operator walkthrough。该复查使用 Chrome/Playwright、1440×1000 与 390×844 视口、真实 PNG CAPTCHA 和既有匿名业务证据，不能替代用户研究或 `FINAL_MANUAL_ACCEPTANCE`。

## 覆盖与结果

- 正式环境 `http://localhost:8080` 与后端 8081：Portal、Login、未认证 Admin 路由保护均为 HTTP 200 页面响应；受保护路由重定向 Login。
- 桌面 Portal、Login 和 390 px Portal 均无横向溢出；0 console error、0 page error、0 failed request。
- CAPTCHA 为真实随机 PNG，响应不含 `testCode`。
- PR #31 匿名机器浏览器已覆盖学生 Dashboard/练习/结果/STANDARD/错题/AI、教师工作台/AI 生成、管理员模型/候选审核，共 25 条路由，0 console/page/request/overflow 错误。

## 评价

| 维度 | 结论 | 级别/说明 |
|---|---|---|
| 信息层级 | 角色首页到主任务层级清楚 | PASS |
| 菜单命名 | “AI 候选题”“AI 模型管理”“错题本”与业务一致 | PASS |
| 首次进入 | 首次登录门禁明确要求修改密码，角色页提供可选入口 | PASS |
| 表单顺序 | 练习配置、候选生成与导入按“范围→参数→确认”组织 | PASS |
| 操作按钮 | 主操作靠近表单/详情，审核动作有状态语义 | PASS |
| 危险操作 | 发布、驳回、重置和清 Key 由后端状态/确认约束 | PASS |
| 错误恢复 | loading、空状态、AI 降级和可重试提示存在 | PASS_WITH_NOTES：外部 Provider 429 仍需等待账户窗口 |
| 管理员密度 | 表格能承载当前数据，但窄屏不适合作为完整后台操作面 | LOW：答辩建议桌面使用 |
| 学生答题专注度 | 题干、作答、提交、结果分段明确 | PASS |
| AI / STANDARD | STANDARD 始终单独展示，AI 标为辅助分析 | PASS |
| 聊天气泡 | 本人右侧、RIKE 左侧，身份与剩余轮数明确 | PASS |
| 390 px 触控 | Portal 和学生核心页无横向溢出，主要按钮具备可点击面积 | PASS_WITH_NOTES |
| 键盘焦点 | Element Plus 原生控件保留焦点与表单顺序 | PASS_WITH_NOTES：未做屏幕阅读器用户研究 |
| 对比度 | Aqua 主文本/背景可读，禁用态可辨识 | PASS_WITH_NOTES：未声明 WCAG 认证 |
| loading/disabled | 生成、发送和请求过程禁用重复提交 | PASS |
| 生成/审核点击数 | 进入工作区后同页完成母题选择、生成、详情和审核 | PASS；未增加无意义全局统计步骤给教师 |

## 问题分级

- BLOCKER：0。
- HIGH：0。
- MEDIUM：0 个需要在本轮修改的明显问题。
- LOW：管理员复杂表格建议保持桌面操作；前端 build 的大 chunk warning 可在未来维护时按路由拆分，但不影响正确性。

本轮没有重新装修 UI，也没有修改生产业务代码。正式真实姓名不出现在截图或提交文件；账号首次改密和完整真人操作仍由用户最终验收确认。
