# 论文插图索引

所有图位于 [thesis-final](evidence/thesis-final/README.md)，已按二进制敏感门禁目视检查。UI 数据环境为匿名 `rike_tiku_demo`，架构图为当前 V14 事实；均不含本机正式人员信息。

| 建议图号 | 文件 | 中文标题 | 章节 | 页面说明 / 裁剪建议 | 图注建议 |
|---|---|---|---|---|---|
| 图 4-1 | `01-portal-desktop.png` | 系统门户桌面界面 | 系统实现 | 可裁剪首屏；1440 px | RIKE 理科学习辅助系统公开门户 |
| 图 4-2 | `02-login.png` | 真实图形验证码登录 | 认证实现 | 保留登录卡片与 CAPTCHA | 系统统一登录与随机图形验证码 |
| 图 4-3 | `03-student-dashboard.png` | 学生三科学习工作台 | 学生功能 | 全图 | 学生按物理、化学、生物进入学习场景 |
| 图 4-4 | `04-student-ai-analysis.png` | 练习结果与 AI 错因 | AI 学习 | 保留 STANDARD 与 AI 两区 | STANDARD 权威解析与 AI 辅助错因并列展示 |
| 图 4-5 | `05-student-current-question-chat.png` | 当前题智能答疑 | AI 学习 | 保留题目绑定和 Drawer | RIKE 当前题有限多轮答疑 |
| 图 4-6 | `06-student-wrong-questions.png` | 学生错题本 | 学生功能 | 全图 | 错题历史和状态管理界面 |
| 图 4-7 | `07-teacher-ai-generation.png` | 教师变式题生成与审核 | 教师功能 | 可裁剪生成表单与任务卡 | 教师授权学科内的 AI 候选题工作区 |
| 图 4-8 | `08-admin-ai-models-masked.png` | 管理员 AI 模型配置 | 管理员功能 | 必须保留掩码，不显示输入明文 | DeepSeek 与 GLM 的本地模型配置及 Key 掩码 |
| 图 4-9 | `09-admin-ai-generation.png` | 管理员候选题工作区 | 管理员功能 | 保留统计、表单与候选卡 | AI 候选生成、重复提示和人工审核 |
| 图 4-10 | `10-portal-mobile.png` | 移动端公开门户 | 界面设计 | 390 px 全长 | RIKE 门户移动端响应式布局 |
| 图 4-11 | `11-student-mobile.png` | 移动端学生工作台 | 界面设计 | 390 px | 学生核心入口在移动端的响应式表现 |
| 图 3-1 | `12-system-architecture.svg` | 系统总体架构 | 总体设计 | SVG 可按页面宽度缩放 | 前后端分离模块化单体架构 |
| 图 3-2 | `13-ai-controlled-flow.svg` | 受控 AI 主链 | AI 架构 | SVG 可按页面宽度缩放 | GLM 视觉、DeepSeek 推理、人工审核与 STANDARD 权威边界 |
| 图 3-3 | `14-database-modules.svg` | 数据库模块划分 | 数据库设计 | SVG 可按页面宽度缩放 | Flyway V14 的 35 张业务表模块划分 |

如学校模板要求位图，可在论文排版阶段从 SVG 导出 300 dpi PNG；不要通过截图开发者工具或正式数据库页面重新取图。
