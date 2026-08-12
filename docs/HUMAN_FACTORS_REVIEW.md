# 最终人体工学与易用性复查

状态：`PASS_WITH_NOTES`

性质：Codex desktop-controlled walkthrough using actual IDEA, WebStorm, Chrome and the local formal `rike_tiku` environment。该复查实际点击 IDEA/WebStorm Run，并控制独立临时 Profile 的真实 Chrome；浏览器通过 CDP/Playwright 读取确定 URL 和执行 DOM 操作，避免复用用户日常浏览器或猜测窗口焦点。它仍是 machine-controlled walkthrough，不是多人真实用户研究，不能替代 `FINAL_MANUAL_ACCEPTANCE`。

## 覆盖与结果

- 真实 IDEA `RikeTikuBackendApplication` 与 WebStorm `RIKE Frontend` 均由 Run 按钮启动；后端连接正式 `rike_tiku`、校验 V1–V14 后监听 8081，前端监听 8080，health/front 均为 200。
- 专用 Chrome 使用独立临时 Profile 和固定 CDP URL；真实 PNG CAPTCHA 不使用 `testCode`。代表性正式账号完成首次改密、双角色选择、教师学科边界、学生练习/提交/STANDARD/错题/AI 入口与个人中心。
- 1440 px 与 390 px 覆盖 Portal、学生首页、练习配置、错题、个人中心和消息，无横向溢出；0 console error、0 page error、0 failed request。
- CAPTCHA 为真实随机 PNG，响应不含 `testCode`。
- 管理员 AI 模型页仅显示 Key 已配置，不回显字面值；管理员/教师候选题工作区、教师管理员路由隔离和 RIKE 学生助手边界均正常。
- PR #31 匿名机器浏览器的 25 路由证据继续作为不含正式姓名的论文证据；本轮正式账号页面只保留脱敏文字结论。

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

本轮没有重新装修 UI，也没有修改生产业务代码。实际桌面与正式库流程结论为 `PASS_WITH_NOTES`，BLOCKER/HIGH 为 0；正式姓名不出现在截图或提交文件。用户人工验收仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。
