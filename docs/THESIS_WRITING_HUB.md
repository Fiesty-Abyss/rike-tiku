# RIKE 论文写作资料中心

本文是“面向高中物化生的 Spring Boot 大模型题库系统设计与实现”的一站式写作入口。当前事实先读 [FINAL_PROJECT_FACTS](FINAL_PROJECT_FACTS.md)，逐图论文材料读 [FINAL_SCREENSHOT_EVIDENCE_CATALOG](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)。定量结论只引用已执行测试和真实实验；机器浏览器、用户人工验收和外部 Provider 状态不得混写。

| 论文材料 | 首选资料 | 用途 |
|---|---|---|
| 1. 项目简介 | [项目首页](../README.md)、[产品说明](../PRODUCT.md) | 研究背景、系统边界、产品定位 |
| 2. 需求分析 | [总体设计](../DESIGN.md)、[项目上下文](AI_PROJECT_CONTEXT.md) | 三角色需求与权限边界 |
| 3. 用户角色 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | STUDENT / TEACHER / ADMIN 职责 |
| 4. 功能模块 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | 功能分解与实现位置 |
| 5. 系统架构 | [总体设计](../DESIGN.md)、[论文实现事实](THESIS_IMPLEMENTATION_FACTS.md) | 前后端分离模块化单体 |
| 6. AI 架构 | [AI 交接](AI_HANDOFF.md)、[AI Provider 配置](AI_PROVIDER_CONFIGURATION.md) | DeepSeek 文本推理、GLM 视觉上下文、STANDARD 权威 |
| 7. 技术栈 | [项目首页](../README.md) | Java 25、Spring Boot 4.1、Vue 3、MySQL 8.4 |
| 8. 数据库设计 | [数据库结构参考](DATABASE_SCHEMA_REFERENCE.md)、[V30 结构快照](../database/schema_snapshot_v30.sql) | 50 表、V30 附件快照、约束与生命周期；V29 仅历史查阅 |
| 9. API | [文档索引](README.md) 的 Admin / Teacher / Student / AI 区 | 接口、授权与降级行为 |
| 10. Excel 导入 | [Excel 导入指南](EXCEL_IMPORT_GUIDE.md) | 学生与题目 preview/confirm |
| 11. 功能代码映射 | [功能代码技术映射](FEATURE_CODE_TECH_MAP.md) | 路由、组件、Controller、Service、表、测试 |
| 12. 测试 | [开发状态](DEVELOPMENT_STATUS.md)、[PR31 证据](evidence/pr31-final/README.md) | 全量自动化与机器浏览器事实 |
| 13. 实验结果 | [AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md) | 真实延迟、Token、Parser、Provider 限制 |
| 14. 截图 | [最终截图证据目录](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md) | 每张截图的路径、数据、代码、API、表、测试、图注和证据边界 |
| 15. 开发过程 | [开发时间线](DEVELOPMENT_TIMELINE.md) | PR、迁移、问题与解决方案 |
| 16. 中期检查 | [中期进展材料草稿](MIDTERM_PROGRESS_SUMMARY.md) | 学校中期检查材料底稿 |
| 17. 正式参考文献 | [22条正式白名单](THESIS_REFERENCES.md)、[正式 BibTeX](references/references.bib)、[使用矩阵](thesis/RIKE_REFERENCE_USAGE_MATRIX.md) | 开题报告和毕业论文正文唯一允许引用的文献 |
| 17a. 扩展研究资料 | [research-only 警示与索引](references/research-only/README.md)、[研究资料 BibTeX](references/research-only/research_materials.bib) | 仅供工程调研，不得进入老师审查版本 |
| 18. 创新点 | [答辩事实与问答](DEFENSE_FACTS_AND_QA.md) | 受控 AI、视觉桥接、人工审核与降级 |
| 19. 局限 | [AI 实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)、[人体工学复查](HUMAN_FACTORS_REVIEW.md) | 外部限流、单机部署、人工验收待完成 |
| 20. 答辩问答 | [答辩事实与问答](DEFENSE_FACTS_AND_QA.md) | 真实实现口径 |

## 推荐写作顺序

先冻结题目、研究范围和架构，再根据功能—代码—数据库映射写设计与实现；实验章节只引用自动化、真实 Provider 和数据库核验结果；最后补用户本人完成的人工验收结论。文献用于解释研究依据和设计取舍，不把文献中的实验结果冒充本项目数据。

## 当前事实

- PR #33、PR #34 已 ordinary merge；PR #35 为冻结后唯一真实教学闭环维护，已获用户人工接受并待 ordinary merge。论文写作应在 PR #35 合并后的 `main` 上取最终事实，详情见 `MANUAL_ACCEPTANCE_FINDINGS_PAPER_RELEASE_MANAGEMENT.md`。
- Flyway V1–V30，50 张业务表；V1–V29 已发布迁移未修改。V30 只新增发布题附件快照 JSON，并扩展主观题待处理状态的字段长度。
- 专题学习仍由统一 `ti_mu` 事实和专题单元编排关系组成：正式库 15 个单元、45 道 `SUBJECTIVE + TOPIC_LEARNING` 原创大题，物理 6 / 化学 5 / 生物 4；主观题可手动组卷但不自动评分。
- 正式 `rike_tiku` 的独立 Chromium 历史机器浏览器已覆盖三科专题、教师混合组卷/质量建议/打印预览、学生混合试卷提交；11 页面、56 断言、0 console/page/failed-request error、0 overflow。打印 OS 对话框并未由 headless 环境声明验证；本轮补 handler machine check 和用户复验。
- 打印修补后的最终集中回归数字以 `FINAL_PROJECT_FACTS.md` 的实际结果为准；学校 Word/PPT 模板尚未提供，本轮不生成新文档。
- 学生候选变式在生成后保持本人可见的 DRAFT；只有显式提交后才进入 PENDING 教师审核。Provider 未配置时不得把候选预览写成真实 AI PASS。
- 论文事实稿、核验表和答辩提纲位于 [thesis](thesis/)；PR #34 打印项为 `PRINT_USER_VERIFIED`，最终 main SHA 将在合并后补写。
