# RIKE 论文写作资料中心

本文是“面向高中物化生的 Spring Boot 大模型题库系统设计与实现”的一站式写作入口。定量结论只引用已执行测试和真实实验；`FINAL_MANUAL_ACCEPTANCE_PENDING` 不得改写为人工验收通过。

| 论文材料 | 首选资料 | 用途 |
|---|---|---|
| 1. 项目简介 | [项目首页](../README.md)、[产品说明](../PRODUCT.md) | 研究背景、系统边界、产品定位 |
| 2. 需求分析 | [总体设计](../DESIGN.md)、[项目上下文](AI_PROJECT_CONTEXT.md) | 三角色需求与权限边界 |
| 3. 用户角色 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | STUDENT / TEACHER / ADMIN 职责 |
| 4. 功能模块 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | 功能分解与实现位置 |
| 5. 系统架构 | [总体设计](../DESIGN.md)、[论文实现事实](THESIS_IMPLEMENTATION_FACTS.md) | 前后端分离模块化单体 |
| 6. AI 架构 | [AI 交接](AI_HANDOFF.md)、[AI Provider 配置](AI_PROVIDER_CONFIGURATION.md) | DeepSeek 文本推理、GLM 视觉上下文、STANDARD 权威 |
| 7. 技术栈 | [项目首页](../README.md) | Java 25、Spring Boot 4.1、Vue 3、MySQL 8.4 |
| 8. 数据库设计 | [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md)、[V23 快照](../database/schema_snapshot_v23.sql) | 41 表、约束与生命周期 |
| 9. API | [文档索引](README.md) 的 Admin / Teacher / Student / AI 区 | 接口、授权与降级行为 |
| 10. Excel 导入 | [Excel 导入指南](EXCEL_IMPORT_GUIDE.md) | 学生与题目 preview/confirm |
| 11. 功能代码映射 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | 路由、组件、Controller、Service、表、测试 |
| 12. 测试 | [开发状态](DEVELOPMENT_STATUS.md)、[PR31 证据](evidence/pr31-final/README.md) | 全量自动化与机器浏览器事实 |
| 13. 实验结果 | [AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md) | 真实延迟、Token、Parser、Provider 限制 |
| 14. 截图 | [PR #33 最终截图](evidence/thesis-final/README.md) | 论文可用的 01—24 匿名截图与图表 |
| 15. 开发过程 | [开发时间线](DEVELOPMENT_TIMELINE.md) | PR、迁移、问题与解决方案 |
| 16. 中期检查 | [中期进展材料草稿](MIDTERM_PROGRESS_SUMMARY.md) | 学校中期检查材料底稿 |
| 17. 参考文献 | [文献与官方资料](THESIS_REFERENCES.md)、[BibTeX](references/references.bib) | 相关工作、政策与技术引用 |
| 18. 创新点 | [答辩事实与问答](DEFENSE_FACTS_AND_QA.md) | 受控 AI、视觉桥接、人工审核与降级 |
| 19. 局限 | [AI 实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)、[人体工学复查](HUMAN_FACTORS_REVIEW.md) | 外部限流、单机部署、人工验收待完成 |
| 20. 答辩问答 | [答辩事实与问答](DEFENSE_FACTS_AND_QA.md) | 真实实现口径 |

## 推荐写作顺序

先冻结题目、研究范围和架构，再根据功能—代码—数据库映射写设计与实现；实验章节只引用自动化、真实 Provider 和数据库核验结果；最后补用户本人完成的人工验收结论。文献用于解释研究依据和设计取舍，不把文献中的实验结果冒充本项目数据。

## 当前事实

- PR #32 已 ordinary merge，merge commit 为 `359fe61e7622b7f623afa212d37c24145273d47b`；PR #33 是唯一最终交付 PR，保持 Draft、未合并。
- Flyway V1–V23，41 张业务表；V1–V19 历史迁移未修改。
- V20—V23 覆盖专题分类/任课范围私有题、知识卡片附件、私信撤回与按用户隐藏，以及专题 AI 互斥会话上下文。
- `rike_tiku_demo` 保持 378 道 PUBLISHED 题基线；机器浏览器 17 条真实路由为 0 console/page/request error、0 overflow。
- 论文事实稿、核验表和答辩提纲位于 [thesis](thesis/)；人工阶段仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`。
