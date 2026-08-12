# 论文实现事实口径

## 项目身份

- 论文题目：面向高中物化生的 Spring Boot 大模型题库系统设计与实现
- 产品名：RIKE 理科学习辅助系统
- 架构：前后端分离的模块化单体
- 学科：高中物理、化学、生物

## 技术与数据

- 后端：Java 25、Spring Boot 4.1、Spring MVC、Spring Security、JWT、MyBatis-Plus、Flyway。
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios。
- 数据库：MySQL 8.4，Flyway V1–V14，35 张业务表。
- 测试：随机临时 MySQL 跑自动化；`rike_tiku_demo` 用于最终演示；正式 `rike_tiku` 不作为测试库。

## AI 架构

- DeepSeek V4 负责文本推理：错因分析、当前题有限答疑、候选变式题生成。
- GLM-4.6V-Flash 只负责把图片转换为严格校验的 `UNTRUSTED_VISION_CONTEXT`。
- 学生统一看到“RIKE 理科学习助手”，不显示底层 Provider 身份。
- V12 AI 调用日志只保存安全元数据。
- 数据库启用配置优先，application/env 为回退；均不可用时受控降级。

## 核心 AI 能力

- 错因分析绑定本人已提交的正式答题事实，输出八类受控错误与五字段严格 JSON，成功结果按事实 hash 复用。
- 当前题答疑绑定具体题目与答题事实，最多 8 轮，单条用户消息 500 字，最近 12 条和 6000 字上下文预算。
- 候选变式题只从 PUBLISHED 母题生成，一次 1–3 道，同母题 PENDING 最多 6 道；严格 JSON、request hash、内容 hash、批内去重和 Jaccard 疑似重复共同约束。
- 候选题只进入 PENDING，教师或管理员完成五项质量评价并 APPROVED 后才进入 PUBLISHED。

## 权威与边界

系统规则是 `STANDARD > AI`。AI 不参与正式客观题判分，不修改正确答案或 STANDARD，不自动发布题目。GLM 失败时不猜测图片内容；DeepSeek 失败时登录、题库、练习、判分、错题、掌握度与 STANDARD 继续工作。

当前实现没有微服务、Redis、MQ、RAG、向量数据库、WebSocket/SSE、流式输出、AI 自动评分或 AI 自动发布。不要在论文中把这些内容写成已实现能力。

工程机器验证状态为 `AUTO_FINAL_VERIFICATION_PASS`；最终用户人工验收仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。真实 DeepSeek 全链路已经复验；真实 GLM 的围栏兼容修复通过自动化，但遵守真实调用次数上限未第三次请求，不能写为真实 Vision PASS。
