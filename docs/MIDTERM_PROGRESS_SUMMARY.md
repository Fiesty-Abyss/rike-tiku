# 本科毕业设计中期进展材料草稿

> 本文是根据真实代码、Git 和测试整理的可提交材料草稿，不表示学校已经组织或通过中期检查。

## 课题目标

设计并实现面向高中物理、化学、生物的前后端分离题库系统，在确定性题库、规则判分和 STANDARD 标准解析之上，提供受控的大模型错因分析、当前题答疑、图片语义桥接和需人工审核的变式题生成。

## 已完成内容

- STUDENT：认证、练习、自动判分、结果、错题、掌握度、规则推荐、高频考点、私信、个人中心、AI 错因和当前题答疑。
- TEACHER：任教范围、班级学习情况、高频考点、消息、AI 候选生成、质量评价和审核。
- ADMIN：组织与账号、题库导入审核、附件、操作日志、AI 模型配置、连接测试、候选题和统计。
- AI：DeepSeek 文本推理；GLM-4.6V-Flash 仅生成 `UNTRUSTED_VISION_CONTEXT`；Fake/Stub 自动化；V12 元数据日志。

## 技术路线与成果

系统采用 Java 25、Spring Boot 4.1、Spring Security/JWT、MyBatis-Plus、Flyway、MySQL 8.4，以及 Vue 3、TypeScript、Vite、Element Plus。架构保持模块化单体，不引入微服务、Redis、RAG 或向量数据库。当前数据库为 V1–V14、35 张业务表，最终题库基线为 378 道有效题。

## 测试事实

PR #31 后端全量 173 tests（0 failure、0 error、3 skipped），前端全量 58 files / 190 tests（0 failure）。真实 DeepSeek 的文本、结构化错因和单题候选均已验证；GLM 真实调用未在严格 Parser 修复后第三次复验，因此保持 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`。机器浏览器覆盖 25 条路由，无 console/page/request/overflow 错误。

## 存在问题与后续

- 最终用户人工验收仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。
- GLM 外部服务存在 429 历史和最终 Parser 修复后未复验限制；Stub、错误分类与降级已覆盖。
- 前端构建有大于 500 kB chunk warning，不影响当前正确性，可在维护阶段做按路由拆分。
- 后续工作只包括用户验收、论文撰写、答辩准备和缺陷维护，不再扩张核心业务。

## 代表材料

代表界面与图注见 [论文插图索引](THESIS_FIGURE_INDEX.md)，功能实现位置见 [功能代码技术映射](FEATURE_CODE_TECH_MAP.md)，数据库事实见 [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md)。
