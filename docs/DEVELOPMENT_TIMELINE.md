# RIKE 开发时间线

> **当前事实不从旧时间线行读取。** 当前 Flyway、表数、测试、Provider、人工验收和 PR 状态以 [FINAL_PROJECT_FACTS.md](FINAL_PROJECT_FACTS.md) 为准；本文件仅保存阶段性历史。

| 日期 / 阶段 | PR / 分支 | 当期交付 | 当期事实（非当前基线） |
|---|---|---|---|
| 早期基础 | PR #1–#19 | 科目知识点、题库、账户角色、教学组织、练习错题、消息、日志、AI 与试卷基础 | 逐步建立 V1–V19；早期测试与表数只解释该阶段。 |
| PR #28–#31 | Provider Core 与 AI 学习链 | DeepSeek、Fake、视觉上下文、学生错因/变式、候选审核、最多 10 轮答疑 | 外部 Provider 实验依赖当时凭据；历史真实调用不等同当前可用。 |
| PR #32 | 本地正式化与论文资料包 | 正式环境、导入、组织与历史截图资料 | 当时的 V14/35 表等数字是历史事实。 |
| 2026-08-12 至 PR #33 | `feat/final-product-completion` | 组织/教师 UI、私有题、试卷发布、管理员收口、科学内容与正式验收 | PR #33 后以 ordinary merge 进入 `main`，merge commit `fba1276862fee973129ee8b85c6fc3a1d55b8662`。 |
| 2026-08-17，PR #34 | `fix/topic-learning-paper-polish` | 15/45 专题大题、SUBJECTIVE 手动组卷、中文题型、V30 发布附件快照、主观待处理语义 | 已 ordinary merge，merge commit `ea784b5a1b6572ea1a2625db347859bd6e410eda`；V30、50 表是当前结构基线。 |
| 2026-08-18，PR #35 | `fix/paper-release-management` | 任课范围唯一选择、私有题隔离、班级发布记录、学生提交查看、撤回、试卷软删除与历史保留 | 已 ordinary merge，merge commit `fde39c53efca316010abf63acf56fda2c631315c`；最终自动化为后端 221（0/0/3）、前端 68 files / 224 tests（0 failures）；用户人工接受，`MACHINE_BROWSER = NOT_RUN`。 |

历史条目不得用于声称当前仍为 V14/V29、35 表、8 轮答疑、旧测试计数或 PR #33 未合并。当前可引用产品事实见 [最终项目事实包](FINAL_PROJECT_FACTS.md)。
