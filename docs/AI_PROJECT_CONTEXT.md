# 跨 AI 项目上下文

更新时间：2026-08-12

## 项目身份

- 产品名为 RIKE 理科学习辅助系统。
- 正式论文题目为“面向高中物化生的 Spring Boot 大模型题库系统设计与实现”。
- 范围为高中物理、化学、生物，采用前后端分离的模块化单体。
- 角色为 `STUDENT`、`TEACHER`、`ADMIN`，同一账号可以拥有多个角色。
- 核心闭环为题目练习、自动判分、STANDARD、错题、AI 辅助分析、当前题答疑和再练习。

## 当前事实

- PR #31 已 ordinary merge，merge commit 为 `c79b7a6f93e32509989282995419bbaf64666182`。当前分支为 PR #32 `chore/final-local-production-thesis-package`。
- Flyway 为 V1–V14，共 35 张业务表；V1–V14 不得修改，没有 V15。
- 非 AI A 层、Provider Core、学生 AI 主链、管理员 AI 配置、Vision 代码链、AI 候选题和人工审核均为 `DONE_VERIFIED`。
- PR #31 全量自动化、Demo、真实 DeepSeek、权限与降级、机器浏览器和文档封板为 `AUTO_FINAL_VERIFICATION_PASS`。
- 真实 `deepseek-v4-flash` smoke、学生错因、当前题答疑和候选生成均为 PASS。
- 真实 GLM 第一次窗口为 429，第二个最终窗口返回完整 JSON 代码围栏；严格 Parser 已修复并通过全量，但没有第三次真实调用，状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`。
- PR #32 只处理本机正式环境、论文资料和最终维护；核心业务不再扩张。用户最终人工验收仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。

## 事实优先级

```text
实际代码与配置
> Flyway 迁移和真实数据库结构
> 自动化与真实 smoke 结果
> Git 提交与 PR 状态
> DEVELOPMENT_STATUS
> AI_HANDOFF
> 设计文档
> 聊天总结
```

计划不得写成已实现，未执行或 skipped 的测试不得标记为通过。

## 技术与业务基线

- 后端使用 Java 25、Spring Boot 4.1、Spring MVC、Spring Security、JWT、MyBatis-Plus、Flyway 和 MySQL 8.4。
- 前端使用 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、GSAP 和 KaTeX。
- 教师权限由 `jiao_shi_id + ban_ji_id + ke_mu_id` 三元任课关系表达，不能由前端入口、姓名或职务替代。
- 单选、多选、填空支持自动判分；综合大题用于专题学习，不做 AI 自动评分。
- 正式答案和 STANDARD 标准解析是权威事实，AI 分析与候选内容不能覆盖。
- AI 故障不能影响登录、题库、练习、判分、错题、掌握度、规则推荐或 STANDARD。
- 不采用微服务。Redis、MQ、RAG、向量数据库、WebSocket、SSE 和本地大模型不是当前主线依赖。

## AI 冻结架构

DeepSeek V4 是文本推理模型，负责学生错因分析、当前题有限答疑和候选变式题生成。GLM-4.6V-Flash 只负责图片题的视觉语义提取。

```text
题目图片 → GLM → UNTRUSTED_VISION_CONTEXT → DeepSeek
```

- GLM 不正式判分、不修改 STANDARD、不自动发布题目。
- 学生统一看到“RIKE 理科学习助手”，不显示 Provider、model、API URL、Key 或 Token。
- V12 只记录安全调用元数据，不保存 Prompt、输出、图片 Base64 或 Key。
- 数据库启用配置优先，application/env 作为回退；均不可用时受控降级。

## 学生 AI 边界

- 分析唯一绑定本人已提交正式答题事实，输出为八类受控错误与五字段严格 JSON。
- 成功分析按答题事实、Prompt 版本和输入事实 SHA-256 复用。
- 当前题答疑最多 8 轮；用户消息最多 500 字；助手消息最多 2000 字；最近 12 条、6000 字上下文预算。
- studentId 只从 JWT 推导；answerFactId 与 conversationId 均重新反查本人所有权。
- 教师和管理员不默认读取学生 AI 私聊正文。

## 候选题与 Vision 边界

- Vision 每题最多 2 张 PNG/JPEG，单图 3 MB，总量 6 MB，按 SHA-256 去重并缓存受控 JSON。
- 候选生成仅接受 PUBLISHED 母题，单次 1 至 3 道，同母题 PENDING AI_GENERATED 最多 6 道。
- request hash 阻止相同有效请求；内容 hash 和批内 hash 拒绝精确重复；trigram/Jaccard 只产生疑似重复提示。
- 候选复用 `ti_mu`，关联母题并只进入 PENDING。人工填写五项质量评价且 APPROVED 后才可 PUBLISHED。
- 一个生成批次的正式候选持久化是原子事务；任何后项失败都不允许留下部分候选。
- 教师只可对本人 ACTIVE 任教学科生成和审核；管理员拥有全局题库权限。

## 开发与交接规则

- 先读取本文件、`DEVELOPMENT_STATUS.md`、`AI_HANDOFF.md`、数据库文档、Flyway、测试和 Git 状态。
- 数据库变化只能新增迁移，不能改写 V1–V14。
- 测试使用随机临时 MySQL 或明确的 `rike_tiku_demo` 路径，不能连接正式 `rike_tiku` 执行写操作。
- Key 不进入 Git、日志、异常、截图或学生 API。管理员本地 Demo 配置可以在 MySQL 保存 Key，但 API 只返回是否配置。
- 代码保持简单、可解释；不为了封板引入大型框架、复杂抽象或新基础设施。
- 历史阶段事实保留在审计、验收和 evidence 文档中，不能拿历史聊天总结覆盖当前代码与测试。

## 历史基线

- PR #27 ordinary merge commit `84a82fc3bd4972fc11c0811d8332bae306b7e5c0`，非 AI A 层封板。
- PR #28 ordinary merge commit `54c1669b3113086a2fb22e756e0656ea8cb751c8`，Provider Core 与 V12。
- PR #29 ordinary merge commit `d04e5dcf9639182303e26e38ccfa4351ad91c5d9`，学生 AI 与 V13。
- PR #30 ordinary merge commit `d67ebc83bf0b8a2fbd889290d5a0f78a27d7640e`，管理员配置、Vision、候选生成、审核与 V14。

完整历史见 [文档索引](README.md) 和 [历史证据](evidence/)。
