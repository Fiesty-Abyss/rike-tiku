# PR #33 人工验收事实台账

更新时间：2026-08-16
真人状态：`FINAL_USER_REVIEW_PENDING`

机器测试、匿名 Demo、Mock/夹具和 Provider 合同测试均不替代用户真人复验。此前泄露的智谱凭据已视为永久失效，本轮未读取、调用、保存或复述；真实 Provider 分项为 `BLOCKED_EXTERNAL_PROVIDER`。

| 编号 | 用户现象与根因 | 最终实现 | 自动化与机器浏览器证据 | 实现提交 | 真人复验 |
|---|---|---|---|---|---|
| MA33-01 | 默认首页与论文资料口径落后 | PR #33 README 以 18 个可见功能章节连接截图、精确代码、表、Flyway 与论文资料；明确 main 仍是 PR #32 基线 | README 本地链接/图片门禁；31 条匿名 Demo 路线 | 最终资料提交 | FINAL_USER_REVIEW_PENDING |
| MA33-02 | 忘记密码入口缺 CSS，Dialog 曾被容器裁切 | Aqua Future 轻量入口、append-to-body、全视口遮罩、独立滚动和移动端宽度 | 组件交互测试；桌面/390px 截图 25/26 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-03 | 变式错误被笼统映射，旧 Schema 要求模型回显内部 ID，写入非原子 | Schema V2、字段级 Parser、一次修复、六类变化方式、新颖度、单事务候选/评价/SUCCESS/实例，失败短事务 | Fake Provider 单/多选/填空、1–5 难度、回滚与 PENDING；09/10 明示 UI 夹具 | `5daa22f`、`27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-04 | 答对题默认折叠 STANDARD 和答疑 | 所有已提交题默认展开 STANDARD；答对题保留当前题答疑与变式，错因只用于答错题 | 结果页单测与 05/08 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-05 | Demo 内部“覆盖/变式”标签泄露到学生与 AI 路径 | 只对带受控 Demo 标识的前缀做统一展示规范化，覆盖练习、结果、错题、专题、试卷、打印、类似题与 Prompt | 正常“覆盖率/覆盖范围/植被覆盖”保留专项 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-06 | 主观专题是一题一页、附件与 AI 上下文不足 | V26 专题单元引用 2–3 道 `ti_mu`，支持五类专题、STEM/OPTION/ANALYSIS 附件、本地草稿、STANDARD、10 轮专题 AI 与 PENDING 变式 | `TopicLearningIntegrationTest`；28 截图 | `12c5a13` | FINAL_USER_REVIEW_PENDING |
| MA33-07 | 答案 JSON 直接显示 | `AnswerDisplay` 安全解析单选、多选、填空、主观和非法结构；诊断 JSON 默认折叠 | 组件五类结构测试；39 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-08 | GLM Base64/错误分类与 1×1 测试图不符合契约 | raw Base64、受控 HTTPS、防 SSRF、非零科学测试图、GLM/xAI 独立诊断与显式选择 | Vision 合同测试；38 截图；真实调用未执行 | `12c5a13` | FINAL_USER_REVIEW_PENDING |
| MA33-09 | 私有题、知识卡片和图片资料链不完整 | V20/V21/V28/V29 完成范围私有题、附件、审核卡片、收藏/掌握、零基础讲解与统一生成练习 | 跨班/管理员不可见、附件权限、卡片生成原子回滚；30/32/33 截图 | `12c5a13`、`df9f0ba`、`c08c89a` | FINAL_USER_REVIEW_PENDING |
| MA33-10 | 私信缺撤回/仅本人删除，操作按钮和确认定位不一致 | V22 软隐藏/撤回；低干扰菜单与居中 `ElMessageBox`，双方语义分离 | 后端并发/权限测试；浏览器真实 API 交互截图 40–42 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-11 | 经典题导入与 AI 关系不清 | 7/19 列模板、Preview/Confirm、PENDING→人工审核→PUBLISHED；页面说明导入不会训练模型 | 模板集成测试；36/37 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-12 | 本地临时目录与备份风险 | 恢复包和正式库备份均在仓库外；公开资料秘密/绝对路径扫描；不提交数据库备份 | SHA-256、非空检查、Git ignore/secret scan | 不产生产品提交 | FINAL_USER_REVIEW_PENDING |
| MA33-13 | 内存占用和重复进程缺证据 | 分阶段 PID/端口记录和 10 分钟三角色路由采样；只停止 RIKE PID | Java 278.7→295.2 MB，Vite 86.9→89.6 MB，非单调泄漏；停止后 18080/18081 释放 | 最终证据提交 | FINAL_USER_REVIEW_PENDING |

附加九项闭环：错题筛选/再做/软归档、非模态当前题答疑、专题单元和附件、GLM/xAI、试卷发布/学生提交/画像、AI 试卷质量建议、append-only 日志分页/CSV 导出、知识卡片库、知识卡片生成练习均纳入 V29/50 表与 210 项后端全量门禁。真实 Provider 结果仍必须逐项独立记录，不能由 Mock 推导。

## 本轮学生端最终集中修复（2026-08-16）

下表按用户要求保留“现象—根因—修改—专项—浏览器—提交—复验”闭环。浏览器证据是 Codex 独立临时 profile 的 machine-controlled walkthrough，不等同于真人复验。

| 编号 | 用户现象 | 严重级别 | 真实根因 | 修改文件 | 专项测试 | 浏览器证据 | 提交 | 用户复验状态 |
|---|---|---|---|---|---|---|---|---|
| MA33-14 | 学生题干、错题、专题或 AI 母题文本可能显示“覆盖：/变式：”等内部演示前缀 | MEDIUM | 展示层只处理了带完整演示标记的旧前缀，独立 `覆盖：`、`变式：` 未在同一规范化器中收口 | `QuestionDisplayTextNormalizer.java`、`QuestionDisplayTextNormalizerTest.java`、`AiQuestionGenerationPromptFactory.java` | `QuestionDisplayTextNormalizerTest`；前端题干/科学文本专项 | `pr33-formal-student/student-wrong-questions.png`、`student-topic-unit-detail.png` | `469fe04` | FINAL_USER_REVIEW_PENDING |
| MA33-15 | 真实 AI 生成、当前题答疑或专题变式可能被前端 5000 ms 提前中断，缺少等待反馈 | HIGH | Axios 全局默认超时覆盖了慢 Provider 业务，没有按用途设置专用预算 | `api/student/aiLearning.ts`、`api/student/topicLearning.ts`、`api/admin/aiModels.ts`、`StudentAiLearningPanel.vue`、`StudentAiVariantPanel.vue`、`TopicLearningView.vue` | 前端 type-check/build；9 文件 30 项学生组件/视图专项；Provider 未配置，未宣称真实 PASS | 页面提示文案与 loading 状态由正式学生路由截图覆盖；真实 Provider 窗口未执行 | `469fe04` | FINAL_USER_REVIEW_PENDING |
| MA33-16 | 当前题 AI 摘要或结果页可能直接展示答案 JSON，学生难以阅读 | MEDIUM | 答案结构在部分学生路径仍以原始 JSON 进入模板，缺少统一的题型标签和安全渲染 | `AnswerDisplay.vue`、`StudentAiLearningPanel.vue`、`ScientificText.spec.ts`、`PracticeResultView.vue` | `AnswerDisplay.spec.ts`、`ScientificText.spec.ts`、PracticeResult 前端专项；目标后端专项 39 项全绿 | 正式学生截图无 raw JSON；API Key、JWT、Prompt、reasoning_content 未出现在证据文件 | `469fe04` | FINAL_USER_REVIEW_PENDING |
| MA33-17 | 错题本不应允许列表直接移出；答错重做保持活跃，答对后再询问是否移出 | HIGH | 原列表按钮把“归档”暴露成即时动作，重做结果没有保留一次性确认上下文 | `WrongQuestionsView.vue`、`PracticeSessionView.vue`、`PracticeResultView.vue`、对应 spec 文件、`StudentPracticeService.java` | `StudentPracticeIntegrationTest` 20 项、`StudentAiServiceIntegrationTest` 生命周期断言、`ObjectiveAnswerGraderTest` 1 项、前端错题/练习专项；机器正式库当前无活跃错题，非破坏性 retry 未执行 | `student-wrong-questions.png` 证明无日期筛选/直接移出；正式浏览器结果记录“无活跃错题，retry skipped”，不冒充完整真人交互 | `469fe04`、`6ee3d15` | FINAL_USER_REVIEW_PENDING |
| MA33-18 | 专题页应以专题单元为一级入口，每单元 2–3 道主观题；候选变式需先私有预览，再提交审核 | HIGH | 旧页面按单题展示；学生候选沿用了教师审核队列语义，DRAFT 可被教师查询/直达审核 | `TopicLearningView.vue`、`TopicLearningController.java`、`AiQuestionGenerationService.java`、`StudentAiService.java`、`AiQuestionGenerationIntegrationTest.java` | `AiQuestionGenerationIntegrationTest` 7 项验证 DRAFT 私有、显式提交后 PENDING、教师审核；`TopicLearningIntegrationTest` 2 项；前端专题专项 | `student-topic-units.png`、`student-topic-unit-detail.png`；正式 API 6 单元且首单元 3 题；Provider 未配置，未执行真实变式生成 | `469fe04` | FINAL_USER_REVIEW_PENDING |
| MA33-19 | “知识卡片”入口和内容应改成物化生高频考点，按学科与二级结论阅读，不伪造统计次数 | MEDIUM | 原卡片内容稀疏且展示名称、类型标签和学科内容不足，正式库没有可审计的结构化卡片来源 | `StudentHomeView.vue`、`StudentKnowledgeCardsView.vue`、`StudentKnowledgeCardsView.spec.ts`、`docs/content/*`、`scripts/ensure-formal-student-content.ps1` | 前端高频考点专项；正式库受控脚本幂等执行（第二次 `CARDS_INSERTED=0`） | `student-high-frequency-points.png`；正式库 65 张已发布卡片，其中本轮结构化来源卡片 60 张，物理/化学/生物均可读 | `469fe04` | FINAL_USER_REVIEW_PENDING |
| MA33-20 | GLM Vision 最终验证必须使用真实单图和安全状态，不能用 Mock 代替 | HIGH | 当前进程环境没有可安全使用的轮换 GLM Key；历史窗口不能覆盖本轮 Parser/业务链变化 | 无产品代码变更；Provider 结果写入 `docs/AI_FINAL_EXPERIMENT_RESULTS.md` | GLM 合同测试可通过；本轮真实 GLM smoke `NOT_RUN`，不能写 REAL_PASS | 未产生真实 Provider 浏览器证据；Key 状态仅记录 PRESENT/ABSENT，不输出值 | `71e8539` | FINAL_USER_REVIEW_PENDING |

本轮正式库机器验收摘要：`rike_tiku` V29/50，6 个已发布专题单元、18 个单元题目、65 张已发布卡片（其中 60 张结构化来源卡片）；正式浏览器 19 项断言、4 个页面、0 console error、0 page error、0 failed request、0 horizontal overflow。上述数字均不表示真人验收，也不表示 Provider PASS。
