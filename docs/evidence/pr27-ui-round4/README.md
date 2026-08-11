# PR #27 UI Round 4 机器浏览器证据

## 证据元数据

| 项目 | 记录 |
| --- | --- |
| 日期 | 2026-08-11 |
| 基线 HEAD | `d4f4a512ac0f39517256b229854c6a0846c71e10` |
| 证据对应版本 | 本证据对应待提交工作区，最终 HEAD 见 PR #27 |
| 前端 | 最新构建产物，Vite preview，`http://localhost:18080` |
| 后端 | acceptance 模式，`http://localhost:18081` |
| 数据库 | `rike_tiku_demo` |
| 浏览器 | 机器浏览器 |
| 视口 | 1440px、1280px、390px |
| CAPTCHA | smoke 机器登录阶段完成后，最终 acceptance 环境为 `testCode=false` |
| 证据数量 | 34 张状态与连续关键帧；其中 33 张为最终证据，1 张为诊断帧 |

最终 acceptance CAPTCHA challenge 不暴露测试验证码。机器登录仅用于自动化 smoke 与受保护页面取证，不构成人工 CAPTCHA 验收。

## Portal

- Hero（1440px）：[`portal-hero-1440.png`](portal-hero-1440.png)
- Hero（1280px）：[`portal-hero-1280.png`](portal-hero-1280.png)
- Hero（390px）：[`portal-hero-390.png`](portal-hero-390.png)
- Physics 连续滚动——波动阶段：[`portal-physics-wave-frame.png`](portal-physics-wave-frame.png)
- Physics 连续滚动——光学阶段：[`portal-physics-optics-frame.png`](portal-physics-optics-frame.png)
- Chemistry 场景：[`portal-chemistry.png`](portal-chemistry.png)
- Biology 场景：[`portal-biology.png`](portal-biology.png)
- 学习闭环：[`portal-learning-loop.png`](portal-learning-loop.png)
- 最终入口：[`portal-final-entrance.png`](portal-final-entrance.png)

Portal 的 1280px 浏览过程覆盖 Hero、Physics、Chemistry、Biology、学习闭环与最终入口；1440px 和 390px 分别覆盖大屏 Hero 与移动端自然纵向降级。静态关键帧不能单独证明动画，连续滚动实现与参数见 [`motion-review.md`](motion-review.md)。

## Auth

- 登录桌面端：[`auth-login-desktop.png`](auth-login-desktop.png)
- 登录 390px：[`auth-login-390.png`](auth-login-390.png)
- 多角色选择：[`auth-role-selection.png`](auth-role-selection.png)

用户名、密码与 CAPTCHA 在首屏可操作，移动端无横向溢出；角色选择延续 Aqua shell，同时保留明确的权限信息。

## Student

- 学生首页：[`student-home.png`](student-home.png)
- 物理环境：[`student-physics-dashboard.png`](student-physics-dashboard.png)
- 化学环境：[`student-chemistry-dashboard.png`](student-chemistry-dashboard.png)
- 生物环境：[`student-biology-dashboard.png`](student-biology-dashboard.png)
- 普通练习：[`student-practice.png`](student-practice.png)
- 结果页桌面端：[`student-result.png`](student-result.png)
- 结果页 390px：[`student-result-390.png`](student-result-390.png)
- 错题：[`student-wrong-questions.png`](student-wrong-questions.png)
- 掌握度内联比例：[`student-mastery-inline-ratio.png`](student-mastery-inline-ratio.png)
- 物理专题：[`student-topic-physics.png`](student-topic-physics.png)
- 化学专题：[`student-topic-chemistry.png`](student-topic-chemistry.png)
- 生物专题：[`student-topic-biology.png`](student-topic-biology.png)

取证过程保留了 `0 / 38`、冻结答案快照、完整选项答案、STANDARD 解析、Topic 分段、ScientificText 与 KaTeX 的业务表现。

## Teacher

- 教师首页最终状态：[`teacher-home.png`](teacher-home.png)
- 物理 scope：[`teacher-physics-scope.png`](teacher-physics-scope.png)
- 化学 scope：[`teacher-chemistry-scope.png`](teacher-chemistry-scope.png)
- 生物 scope：[`teacher-biology-scope.png`](teacher-biology-scope.png)

三科 scope 由真实 `subjectCode` 驱动，班级、任课范围与数据区仍以快速扫描为优先。

`teacher-home-before-final-css.png` 是修补共享样式前的诊断帧，仅用于定位问题，不属于最终视觉证据。

## Admin

- 中性 Dashboard：[`admin-dashboard.png`](admin-dashboard.png)
- Dashboard 390px：[`admin-dashboard-390.png`](admin-dashboard-390.png)
- 高密度表格与筛选：[`admin-dense-table-filter.png`](admin-dense-table-filter.png)
- 题目管理：[`admin-question-management.png`](admin-question-management.png)
- 操作日志：[`admin-operation-logs.png`](admin-operation-logs.png)

管理员保持 neutral Aqua shell；表格、筛选与日志使用实体表面，未按单科给整页染色。

## 浏览器检查结果

- Console error：`0`
- Page error：`0`
- HTTP：页面资源与受检业务 API 正常；未见 dynamic import failure 或异常 HTTP 响应
- 白屏：未出现
- 390px 横向溢出：未出现
- 受检链路：Portal、Login、Role Selection、Student、Teacher、Admin、三科学科、Practice、Result、Wrong Questions、Topic

本目录记录的是机器浏览器证据，不能替代用户的真实浏览器体验、人工 CAPTCHA 验证或最终人工验收 PASS。本环境与证据均未被表述为人工验收完成。
