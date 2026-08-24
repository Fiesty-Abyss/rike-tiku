# RIKE 最终截图证据目录

> 这是论文与答辩的截图事实档案。它与 [快速索引](FEATURE_SCREENSHOT_CODE_INDEX.md) 配合：快速索引用于 30 秒定位，本文逐图说明可见内容、数据、代码、证据边界和可直接使用的图注。所有相对链接均可在 GitHub 直接打开。

## 使用规则

- `THESIS_READY`：页面原生渲染、可直接挑选进 Word/PPT；不是 Photoshop 改图。`RAW_EVIDENCE`：保留原始机器记录或历史匿名截图。`MACHINE_ONLY` 不等于用户人工验收。
- 历史 01–42 使用匿名 Demo 或确定性 UI 夹具；其中 09/10 不可证明真实 Provider 调用。它们不因 V30 存在而删除，状态为 `HISTORICAL` 或 `SUPERSEDED`。
- V30 图来自本机正式 `rike_tiku` 的独立 Chromium profile；不公开账户、密码、token 或 API key。新增打印图只证明 handler 调用；OS 打印窗口须由用户重新点击确认。
- 所有图均不能证明长期课堂成绩提升、真实学生群体效果或无凭据 Provider 的实时成功。

## 代码和数据缩写（用于逐图准确追溯）

| 代号 | 前端 / API | 后端 | 表 / Flyway / 测试 |
|---|---|---|---|
| A 认证 | `views/auth/LoginView.vue`、`components/auth/LoginForm.vue`、`api/auth.ts`；`POST /api/v1/auth/login`、`GET /api/v1/auth/captcha-challenge` | `RenZhengController`、`RenZhengFuWu`、`TuXingYanZhengMaFuWu` | `yong_hu`,`jiao_se`,`yong_hu_jiao_se`；V5/V10/V17；`RenZhengJiChengTest`,`LoginView.spec.ts` |
| P 练习 | `Practice*View.vue`、`AnswerDisplay.vue`、`api/student/practice.ts`；`/api/v1/student/practice-sessions/*` | `StudentPracticeController`、`StudentPracticeService` | `lian_xi_hui_hua`,`lian_xi_ti_mu`,`xue_sheng_da_ti`,`cuo_ti_ji_lu`；V7；`StudentPracticeIntegrationTest`,`Practice*View.spec.ts` |
| T 专题 | `views/student/TopicLearningView.vue`、`QuestionContent.vue`、`StandardAnalysis.vue`、`api/student/topicLearning.ts`；`GET /api/v1/student/topic-learning/units/{id}` | `TopicLearningController`、`TopicLearningService` | `ti_mu`,`ti_mu_jie_xi`,`ti_mu_fu_jian`,`ti_mu_zhi_shi_dian`,`zhuan_ti_xue_xi_dan_yuan`,`zhuan_ti_xue_xi_dan_yuan_ti_mu`；V4/V20/V26；`TopicLearningContentContractTest`,`TopicLearningIntegrationTest`,`TopicLearningView.spec.ts` |
| W AI 学习 | `StudentAiLearningPanel.vue`、`api/student/aiLearning.ts` | `StudentAiController`、`StudentAiService`、Provider Core | `ai_cuo_ti_fen_xi`,`ai_hui_hua`,`ai_xiao_xi`,`ai_sheng_cheng_ren_wu`；V13/V15/V16/V19/V24/V25；`StudentAiServiceIntegrationTest`,`StudentAiLearningPanel.spec.ts` |
| R 组卷/发布 | `TeacherPaperBuilderView.vue`、`PaperPreviewView.vue`、`StudentPapersView.vue`、`api/teacher/papers.ts`、`api/student/papers.ts`；`/api/v1/teacher/papers/*`、`/api/v1/student/papers/*` | `PaperController`、`PaperAssignmentTeacherController`、`PaperAssignmentStudentController`、`PaperService`、`PaperAssignmentService` | `shi_juan`,`shi_juan_ti_mu`,`shi_juan_fa_bu`,`shi_juan_fa_bu_ti_mu`,`shi_juan_ti_jiao`,`shi_juan_xue_sheng_da_ti`；V18/V27/V30；`PaperAssignmentIntegrationTest`,`TeacherPaperBuilderView.spec.ts`,`PaperPreviewView.spec.ts`,`StudentPapersView.spec.ts` |
| O 教学组织 | `TeachersView.vue`,`ClassesView.vue`,`StudentImportView.vue`、相应 `api/admin/*` | `JiaoShiGuanLiController`/Service、`BanJiController`/Service、`StudentImportController`/Service | `yong_hu`,`jiao_shi_dang_an`,`xue_sheng_dang_an`,`ban_ji`,`ban_ji_xue_sheng`,`ren_ke_guan_xi`,`yong_hu_jiao_se`；V5/V6；组织/导入集成测试 |
| Q 管理题库 | `QuestionsView.vue`,`QuestionImportView.vue`、`api/admin/questions.ts` | `QuestionAdminController`/Service、附件 Controller/Service | `ti_mu` 及选项/解析/附件/审核/导入表；V2；题库与附件集成测试 |
| M 消息/日志/模型 | `Messages*View.vue`,`OperationLogsView.vue`,`AdminAiModelsView.vue`、相应 API | `SiXinController`/Service、日志 Controller/Service、`AiModelConfigController`/Service | 消息、日志、模型配置表；V9/V11/V14/V22；对应集成与前端测试 |

## V30 当前机器证据（推荐优先使用）

V30-01–14、V30-17–18 为 `THESIS_READY + MACHINE_BROWSER_VERIFIED`，环境为正式 `rike_tiku`、`localhost:8080/8081`、独立 Chromium；V30-15–16 为 `MACHINE_ONLY` handler 证据。V30 基础验收 commit 为 `aba887a92e1bfa8bb24e184a5f1b09489efc7533`；题目/单元 ID、试卷 ID、发布 ID 是受控本地验收数据，图中不暴露凭据；需要复核时用文末只读 SQL 在同一环境查询。

| ID / 截图 | 推荐论文图名 / PPT 名 | 路由、角色、可见内容 | 数据和数据库来源 | 代码/API/迁移/测试 | 证明什么；不能证明什么；可用图注 |
|---|---|---|---|---|---|
| V30-01 [物理专题](evidence/v30-machine-browser/student-physics-topic.png) | 图6-x 物理专题学习界面 / 物理大题三阶段学习 | `/student/topics/units/{physicsUnitId}`，STUDENT；单元、FOUNDATION/TRANSFER/ADVANCED 导航、材料题干、知识点、草稿与 AI 学习入口。 | 物理单元（15 单元中的 6 个之一）、`SUBJECTIVE + TOPIC_LEARNING`、PUBLISHED、专题类型和附件由 T 表组决定。 | T；V4/V20/V26；Topic tests。 | 证明统一题目事实经专题编排进入学生页；不能证明 AI 准确率或课堂效果。图注：**物理专题学习界面展示三阶段主观大题、学生草稿与受控学习入口。** |
| V30-02 [物理 STANDARD 与图片](evidence/v30-machine-browser/student-physics-topic-standard.png) | 图6-x 物理专题标准解析 / 图片与 STANDARD | 同上，STUDENT；图像、分步 STANDARD、公式渲染和附件。 | `ti_mu_fu_jian` 受控图片、`ti_mu_jie_xi` STANDARD、知识点关系；附件 content API HTTP 200。 | T；V26；Topic contract/attachment tests。 | 证明题目附件和权威解析能显示；不能把解析说成 AI 自动生成答案。图注：**物理专题题目附件与分步 STANDARD 解析。** |
| V30-03 [化学专题](evidence/v30-machine-browser/student-chemistry-topic.png) | 图6-x 化学实验/流程专题 / 化学综合大题 | `/student/topics/units/{chemistryUnitId}`，STUDENT；材料、分问、公式/反应式、STANDARD。 | 化学单元（5 个之一）、PUBLISHED 专题主观题，专题类型为 EXPERIMENT/PROCESS/COMPREHENSIVE 中的真实字段值。 | T；V20/V26；Topic tests。 | 证明化学专题是统一题库中的主观材料题；不能证明化学反应在实验室执行。图注：**化学专题以实验或流程材料组织分步分析与 STANDARD。** |
| V30-04 [生物专题](evidence/v30-machine-browser/student-biology-topic.png) | 图6-x 生物材料分析专题 / 生物实验与材料分析 | `/student/topics/units/{biologyUnitId}`，STUDENT；材料/数据或流程、草稿、解析。 | 生物单元（4 个之一）；`MATERIAL_ANALYSIS`、`EXPERIMENT` 或 `COMPREHENSIVE` 的真实类别来自 `ti_mu.zhuan_ti_lei_xing`。 | T；V20/V26；Topic tests。 | 证明生物专题主观分析和统一题目关系；不能证明受试者学习改善。图注：**生物专题以材料或实验过程支持变量、证据和结论分析。** |
| V30-05 [教师主观题检索](evidence/v30-machine-browser/teacher-subjective-search.png) | 图6-x 教师检索专题主观大题 / SUBJECTIVE 检索 | `/teacher/papers`，TEACHER；题型显示“主观大题”、专题类型中文标签、知识点和可加入题篮的结果。 | 当前教师 ACTIVE 任课科目下 PUBLISHED 题；GLOBAL 或合法教学范围；不返回越权私有题。 | R；V18/V20/V26；Paper assignment/backend and builder tests。 | 证明 `SUBJECTIVE` 可被手动检索且不泄露跨范围题；不能证明随机/规则默认会加入主观题。图注：**教师在任教学科范围内检索并选择专题主观大题。** |
| V30-06 [教师组卷](evidence/v30-machine-browser/teacher-paper-builder.png) | 图6-x 混合试卷组卷 / 客观题与主观题题篮 | `/teacher/papers`，TEACHER；筛选、题篮、顺序、分值和保存。 | `shi_juan`,`shi_juan_ti_mu`；混合题型、教师范围和试卷总分由服务端校验。 | R；V18/V20；PaperService/tests。 | 证明同一 `ti_mu.id` 可进专题和试卷；不能证明提交后主观题会自动评分。图注：**教师调整混合试卷题序和分值并保存。** |
| V30-07 [带图组卷](evidence/v30-machine-browser/teacher-illustrated-paper-builder.png) | 图6-x 带附件主观题组卷 / 试卷附件链路 | `/teacher/papers`，TEACHER；主观题题干和必要图片可见。 | `ti_mu_fu_jian` 及题目关系；发布时将进入 `fu_jian_kuai_zhao`。 | R+T；V30；Paper attachment snapshot tests。 | 证明组卷读取的是原题受控附件；不能证明已发布试卷的 OS 打印。图注：**教师组卷页展示专题主观题的受控图片附件。** |
| V30-08 [质量建议](evidence/v30-machine-browser/teacher-paper-quality.png) | 图6-x 试卷质量建议 / 中文题型分布 | `/teacher/papers`，TEACHER；题型分布、知识点覆盖、总分、客观自动判分总分、主观题提示与风险。 | `shi_juan`,`shi_juan_ti_mu`；混合题型分布由服务确定性计算。 | R；V18/V27/V30；quality tests。 | 证明 UI 不直接显示 `{SINGLE_CHOICE=...}`，主观题不自动评分；不能把建议当正式 STANDARD。图注：**试卷质量区域以中文呈现题型分布和主观题评分边界。** |
| V30-09 [发布质量建议](evidence/v30-machine-browser/teacher-paper-publish-quality.png) | 图6-x 发布前质量核验 / 发布边界 | `/teacher/papers`，TEACHER；发布班级、质量事实与风险提示。 | `ren_ke_guan_xi`,`ban_ji`,`shi_juan_fa_bu`；只能选本人 ACTIVE 班级范围。 | R+O；V6/V27；PaperAssignmentIntegrationTest。 | 证明发布被组织范围约束；不能证明具体班级真实教学结果。图注：**发布前展示班级范围和确定性质量核验。** |
| V30-10 [学生版打印](evidence/v30-machine-browser/teacher-paper-student-print.png) | 图6-x 学生版 A4 预览 / 学生版打印样式 | `/teacher/papers/{paperId}/student`，TEACHER；题干、附件、主观答题空间，不含答案和 STANDARD。 | `shi_juan`,`shi_juan_ti_mu` 或当前预览 DTO；打印样式不改题目事实。 | R；V18/V30；PaperPreviewView.spec.ts。 | 证明学生版内容与答案隔离、A4 CSS；不能证明 OS 对话框已打开。图注：**教师查看不含答案与解析的学生版试卷预览。** |
| V30-11 [答案版打印](evidence/v30-machine-browser/teacher-paper-answer-print.png) | 图6-x 答案解析版 A4 预览 / STANDARD 答案版 | `/teacher/papers/{paperId}/answer`，TEACHER；正确答案、STANDARD 和解析附件。 | 试卷题和原题/冻结内容；权限由教师所有权约束。 | R；V18/V30；PaperPreviewView.spec.ts。 | 证明答案版显示 STANDARD；不能证明学生能访问答案。图注：**教师答案解析版试卷预览展示正确答案与 STANDARD。** |
| V30-12 [学生试卷列表](evidence/v30-machine-browser/student-mixed-paper-list.png) | 图6-x 学生混合试卷任务 / 发布后的可见性 | `/student/papers`，STUDENT；已发布混合试卷任务与状态。 | `shi_juan_fa_bu`,`shi_juan_ti_jiao`，由班级/科目发布范围过滤。 | R；V27；Student papers tests。 | 证明学生只看到其范围内的发布版本；不能证明已提交得分。图注：**学生端列出面向其班级发布的混合试卷。** |
| V30-13 [学生带图作答](evidence/v30-machine-browser/student-illustrated-paper-answering.png) | 图6-x 学生主观题作答 / 图片与文本输入 | `/student/papers/{releaseId}`，STUDENT；客观控件、主观文本框、图片与“不自动评分”说明。 | `shi_juan_fa_bu_ti_mu.fu_jian_kuai_zhao`、`shi_juan_ti_jiao`、`shi_juan_xue_sheng_da_ti`。 | R；V27/V30；StudentPapersView.spec.ts。 | 证明发布附件快照和主观答题输入；不能证明得到主观正式分。图注：**学生在含图片的主观题试卷中保存文字作答，界面明确不自动评分。** |
| V30-14 [学生提交结果](evidence/v30-machine-browser/student-illustrated-paper-submitted.png) | 图6-x 混合试卷提交结果 / 客观得分与待处理主观题 | 同上，STUDENT；客观得分、客观总分、主观待人工处理。 | `shi_juan_ti_jiao.ke_guan_de_fen/ke_guan_zong_fen` 与答题状态 `SUBJECTIVE_PENDING`。 | R；V27/V30；PaperAssignmentIntegrationTest。 | 证明客观自动判分与主观题分离；不能把客观得分称整卷最终成绩。图注：**混合试卷提交后，系统分别呈现客观自动得分和主观题待人工处理状态。** |
| V30-15 [PR34 学生版打印 handler](evidence/v30-machine-browser/pr34-print-student-preview.png) | 附图：学生版打印处理器 / 原生打印调用 | `/teacher/papers/{paperId}/student`，TEACHER；预览和打印按钮。 | 独立 Chromium 的受控教师/试卷 API 响应；无凭据、无正式业务记录，hook 不更改后端数据。 | R；`PaperPreviewView.vue`/spec；PR34-MA-001。 | 证明 click→`window.print()` handler；不能证明 OS 系统对话框或正式库数据。图注：**学生版预览的原生打印处理器机器调用记录。** |
| V30-16 [PR34 答案版打印 handler](evidence/v30-machine-browser/pr34-print-answer-preview.png) | 附图：答案版打印处理器 / 原生打印调用 | `/teacher/papers/{paperId}/answer`，TEACHER；答案预览和打印按钮。 | 同一受控浏览器响应，详见 `pr34-print-handler-results.json`。 | R；`PaperPreviewView.vue`/spec；PR34-MA-001。 | 证明同一组件在答案路由也调用 handler；不能证明 OS 系统对话框或正式库数据。图注：**答案解析版预览的原生打印处理器机器调用记录。** |
| V30-17 [带图主观题检索](evidence/v30-machine-browser/teacher-illustrated-subjective-search.png) | 备选图：带图专题题检索 / 附件可见性 | `/teacher/papers`，TEACHER；中文“主观大题”筛选、题干图片和题篮操作。 | 正式 `rike_tiku` 的受控 V30 验收数据；题目附件来自 `ti_mu_fu_jian`。 | R+T；V20/V26/V30；Paper/Topic tests。 | 与 V30-05/07 同一工作流的补充视角，证明附件不妨碍合法检索；不能证明跨范围可见。图注：**教师在手动组卷中检索带附件的专题主观题。** |
| V30-18 [带图试卷质量建议](evidence/v30-machine-browser/teacher-illustrated-paper-quality.png) | 备选图：混合试卷质量核验 / 主观题评分边界 | `/teacher/papers`，TEACHER；题型分布、客观自动判分总分和主观题风险。 | 正式 `rike_tiku` 的受控 V30 混合试卷；类型统计来自试卷题目关系。 | R；V27/V30；quality/backend and builder tests。 | 与 V30-08 同一质量区域的带图试卷视角，证明中文化和评分边界；不能证明 AI 给出正式分数。图注：**混合试卷质量建议将客观自动判分与主观人工处理明确分开。** |

## PR #33 历史匿名图（01–42）

这些图均为 `RAW_EVIDENCE + HISTORICAL`，原始清单见 [thesis-final README](evidence/thesis-final/README.md)。数据均为匿名 `rike_tiku_demo` API，除 09/10 明确为确定性 UI 夹具。截图日期/历史 commit 以相邻 `browser-results-pr33.json` 和 PR #33 提交记录为准；不把 Demo 实体 ID 写成正式库事实。每行均给出页面实际可见范围、追溯代码和可直接使用的客观图注。

| 图 | 页面/角色/可见内容 | 数据、代码/API、表/Flyway、测试 | 论文图注；可证明 / 不可证明 |
|---|---|---|---|
| [01](evidence/thesis-final/01-portal-desktop.png) | `/`，PUBLIC；门户、三科学习入口和主链。 | 无业务数据；`PortalView.vue`；前端 Portal spec。 | **RIKE 公共门户展示三科学习系统入口。** 证明响应式页面；不证明登录或学习效果。 |
| [02](evidence/thesis-final/02-login.png) | `/login`，PUBLIC；角色登录、CAPTCHA、密码恢复入口。 | A；Demo CAPTCHA；V5/V10/V17。 | **登录页集成角色选择与一次性图形验证码。** 证明 UI；不证明真实账户登录。 |
| [03](evidence/thesis-final/03-student-dashboard.png) | `/student`，STUDENT；三科学习概览、统计和导航。 | 学习汇总 API、学习/组织表；V6/V7；dashboard specs。 | **学生工作台汇总三科学习入口和学习状态。** 不证明真实学生数据。 |
| [04](evidence/thesis-final/04-practice.png) | `/student/practice/15`，STUDENT；题干、选项和提交。 | P；V7。 | **在线练习会话展示冻结题目和作答控件。** 不证明题目来自本轮 V30 专题。 |
| [05](evidence/thesis-final/05-result-standard.png) | 练习结果，STUDENT；得分、正确答案和 STANDARD。 | P；确定性判分。 | **练习结果在提交后展示确定性判分与 STANDARD。** 不证明 AI 评分。 |
| [06](evidence/thesis-final/06-wrong-questions.png) | `/student/wrong-questions`，STUDENT；错题列表和状态。 | P；`cuo_ti_ji_lu`；V7。 | **错题本呈现学生历史错误题目。** 不证明掌握度提升。 |
| [07](evidence/thesis-final/07-student-ai-analysis.png) | 结果页 AI Drawer，STUDENT；AI 错因字段。 | W；Demo 已有分析记录；V13。 | **AI 错因分析以辅助字段呈现。** 不证明本次真实 Provider 调用或正确率。 |
| [08](evidence/thesis-final/08-student-ai-chat.png) | AI Drawer，STUDENT；当前题对话、模型/搜索控件。 | W；V15/V16，最多 10 轮。 | **当前题答疑界面绑定已提交题目。** 不证明模型真实在线。 |
| [09](evidence/thesis-final/09-student-ai-variant.png) | 变式 Drawer，STUDENT；结构化候选题。 | W；确定性 UI 夹具；V19/V24/V25。 | **变式题预览使用结构化字段和作答控件。** 不证明真实 AI 生成。 |
| [10](evidence/thesis-final/10-student-ai-variant-result.png) | 变式结果，STUDENT；确定性判分反馈。 | W；UI 夹具和共享判分。 | **变式练习结果复用确定性判分界面。** 不证明真实 Provider。 |
| [11](evidence/thesis-final/11-teacher-workspace.png) | `/teacher/scopes/1`，TEACHER；任课范围、班级和学情。 | O；`ren_ke_guan_xi`；V6。 | **教师工作台以任课范围组织教学任务。** 不证明所有班级数据。 |
| [12](evidence/thesis-final/12-teacher-ai-review.png) | `/teacher/ai-generation`，TEACHER；候选生成和人工审核。 | W/Q；候选/审核表；V14/V25。 | **教师候选题工作区保留人工审核入口。** 不证明真实生成调用。 |
| [13](evidence/thesis-final/13-teacher-paper-builder.png) | `/teacher/papers`，TEACHER；手动/规则组卷。 | R；V18。 | **教师组卷页支持题目筛选和题篮。** 已被 V30 当前图替代为 `SUPERSEDED`，不证明主观组卷。 |
| [14](evidence/thesis-final/14-paper-student-preview.png) | `/teacher/papers/1/student`，TEACHER；学生版预览。 | R；V18。 | **学生版预览隐藏答案和 STANDARD。** `SUPERSEDED`：当前专题主观题看 V30-10。 |
| [15](evidence/thesis-final/15-paper-answer-preview.png) | `/teacher/papers/1/answer`，TEACHER；答案版。 | R；V18。 | **答案解析版展示正确答案和 STANDARD。** `SUPERSEDED`：当前附件快照看 V30-11。 |
| [16](evidence/thesis-final/16-admin-dashboard.png) | `/admin`，ADMIN；管理总览。 | O/Q/M；组织/题库汇总。 | **管理员总览呈现管理入口。** 不证明每一管理操作完成。 |
| [17](evidence/thesis-final/17-admin-ai-models.png) | `/admin/ai-models`，ADMIN；TEXT/VISION/SEARCH 安全配置状态。 | M；模型配置表；V14。 | **模型配置页仅呈现安全元数据而非完整 Key。** 不证明 Provider 可用。 |
| [18](evidence/thesis-final/18-admin-password-notifications.png) | `/admin/password-recovery`，ADMIN；恢复记录。 | A/M；`mi_ma_chong_zhi_shen_qing`；V17。 | **管理员处理密码恢复请求。** 不证明用户名是否存在被泄露。 |
| [19](evidence/thesis-final/19-admin-ai-generation.png) | `/admin/ai-generation`，ADMIN；候选审核。 | W/Q；V14/V25。 | **管理员审核 AI 候选题。** 不证明自动发布或真实调用。 |
| [20](evidence/thesis-final/20-portal-mobile.png) | `/`，PUBLIC；移动门户。 | `PortalView.vue`；Portal spec。 | **RIKE 门户的移动端原生响应式布局。** 不证明业务 API。 |
| [21](evidence/thesis-final/21-student-mobile.png) | `/student`，STUDENT；移动学生首页。 | 学习摘要 API/组织表。 | **学生首页在移动端保持学习入口。** 不证明移动全链路。 |
| [22](evidence/thesis-final/22-system-architecture.svg) | 非页面，架构图；前后端、数据库和 AI 边界。 | `DESIGN.md`、代码包结构；Flyway V1–V29 当时事实。 | **RIKE 采用前后端分离模块化单体架构。** `HISTORICAL`，V30 以本文和 DB reference 为准。 |
| [23](evidence/thesis-final/23-ai-controlled-flow.svg) | 非页面，受控 AI 流。 | W；STANDARD/人工审核边界。 | **AI 输出经受控解析和人工审核，不覆盖 STANDARD。** 不证明 Provider 实时可用。 |
| [24](evidence/thesis-final/24-database-modules.svg) | 非页面，V29 数据库模块。 | Flyway V1–V29、50 表。 | **数据库模块按当时 V29 演进组织。** `SUPERSEDED`：当前结构是 V30 快照。 |
| [25](evidence/thesis-final/25-password-recovery-desktop.png) | `/login`，PUBLIC；桌面恢复弹窗。 | A；V17。 | **密码恢复表单使用 CAPTCHA 提交安全申请。** 不展示反枚举实现细节。 |
| [26](evidence/thesis-final/26-password-recovery-mobile.png) | `/login`，PUBLIC；移动恢复弹窗。 | A；V17。 | **移动端密码恢复表单保留必要字段。** 不证明处理结果。 |
| [27](evidence/thesis-final/27-wrong-question-review.png) | `/student/wrong-questions`，STUDENT；筛选、再做、归档。 | P；V7。 | **错题页支持复习和状态管理。** 不证明学习提升。 |
| [28](evidence/thesis-final/28-topic-units.png) | `/student/topics`，STUDENT；单元与附件入口。 | T；V20/V26。 | **专题学习以单元编排已有题目。** `SUPERSEDED`：当前大题质量看 V30-01–04。 |
| [29](evidence/thesis-final/29-student-papers.png) | `/student/papers`，STUDENT；试卷任务。 | R；V27。 | **学生端展示班级范围内的发布试卷。** 不证明混合主观题状态；看 V30-12–14。 |
| [30](evidence/thesis-final/30-knowledge-cards.png) | `/student/knowledge-cards`，STUDENT；知识卡片。 | 学生知识卡片 API、`gao_pin_kao_dian` 等；V21/V28/V29。 | **知识卡片支持学生查看和学习状态。** 不证明真实频次。 |
| [31](evidence/thesis-final/31-message-actions.png) | `/messages/:id`，登录用户；消息操作。 | M；`si_xin_hui_hua`,`si_xin_xiao_xi`；V9/V22。 | **私信会话提供低干扰操作入口。** 不证明他人消息可见性。 |
| [32](evidence/thesis-final/32-private-question-bank.png) | `/teacher/private-questions`，TEACHER；范围私有题。 | Q/O；`ti_mu`,`ren_ke_guan_xi`；V20。 | **教师私有题库受任课范围约束。** 不证明跨范围访问。 |
| [33](evidence/thesis-final/33-teacher-knowledge-cards.png) | `/teacher/knowledge-cards`，TEACHER；卡片审核。 | 知识卡片 teacher API；V21/V28。 | **教师可审核知识卡片。** 不证明 AI 生成质量。 |
| [34](evidence/thesis-final/34-paper-publish-quality.png) | `/teacher/papers`，TEACHER；发布/质量入口。 | R；V27。 | **发布流程有质量建议入口。** `SUPERSEDED`：中文题型事实看 V30-08/09。 |
| [35](evidence/thesis-final/35-operation-log-search.png) | `/admin/operation-logs`，ADMIN；分页检索。 | M；`guan_li_cao_zuo_ri_zhi`；V11。 | **管理员可分页检索操作日志。** 不证明日志不可删除。 |
| [36](evidence/thesis-final/36-question-import.png) | `/admin/questions/import`，ADMIN；19 列题目导入。 | Q；导入批次表；V2。 | **题目导入采用预览/确认流程。** 不证明当前导入文件成功。 |
| [37](evidence/thesis-final/37-student-import.png) | `/admin/students/import`，ADMIN；7 列学生导入。 | O；V5/V6。 | **学生导入采用受控预览/确认。** 不证明真实学生信息。 |
| [38](evidence/thesis-final/38-vision-provider-config.png) | `/admin/ai-models`，ADMIN；GLM/xAI 状态。 | M；V14/V26。 | **视觉 Provider 配置不回显 Key。** 不证明可调用。 |
| [39](evidence/thesis-final/39-question-review.png) | `/admin/questions`，ADMIN；题目审核。 | Q；题目审核表；V2。 | **管理员题库审核遵循状态机。** 不证明自动审核。 |
| [40](evidence/thesis-final/40-message-menu.png) | `/messages/:id`，登录用户；消息菜单。 | M；V9/V22。 | **消息菜单提供会话内操作。** 不证明操作已提交。 |
| [41](evidence/thesis-final/41-message-recall-confirm.png) | `/messages/:id`，登录用户；撤回确认。 | M；V22。 | **撤回操作需用户确认。** 不证明跨用户删除。 |
| [42](evidence/thesis-final/42-message-delete-confirm.png) | `/messages/:id`，登录用户；仅本人删除确认。 | M；V22。 | **消息删除区分仅本人隐藏。** 不证明物理删除。 |

## Excel 模板与导入页面资料

### 候选收口说明（2026-08-21）

本轮未把旧的门户静态截图改标为动态统计截图。公共门户的动态统计由 `PortalView.vue → publicPortal.ts → GET /api/v1/public/portal-stats → PortalStatsController/Service → ke_mu/ti_mu` 交付，证据为 `PortalView.spec.ts`、`PortalStatsServiceIntegrationTest` 与 [最终回归报告](FINAL_CLEANUP_REGRESSION_REPORT.md)。因此历史门户图只能证明 UI，不可证明统计数字；新的可用于答辩的 Excel 工作簿见 [demo-import](demo-import/README.md)，并以 Preview/Confirm 集成测试证明格式和导入链路。

这些图补足“模板本身”和“实际导入页面”的可离线阅读资料。模板图由仓库内真实 `.xlsx` 文件原样渲染，未填真实学生、密码或 Token；页面图是已存档的匿名原始证据。模板没有业务 API，导入页面才调用 API。

| 图 | 状态、数据与页面 | 代码 / API / 表 / Flyway / 测试 | 论文/PPT 用途、证明与边界 |
|---|---|---|---|
| [EXCEL-01 学生模板](evidence/final-documentation/excel-student-template-overview.png) | `THESIS_READY`；源为 [`student-import-template.xlsx`](templates/student-import-template.xlsx)，Sheet `学生导入`，7 列，匿名模板数据。 | `StudentExcelTemplate`；`GET /api/v1/admin/student-import/template`；`yong_hu`、`yong_hu_jiao_se`、`xue_sheng_dang_an`、`ban_ji_xue_sheng`；V5/V6；`FinalImportTemplatesIntegrationTest`。 | 图注：**学生批量导入模板固定 7 个字段，导入前需保持 Sheet 名和表头不变。** 证明模板字段；不证明真实学生已被导入。 |
| [EXCEL-02 题目模板](evidence/final-documentation/excel-question-template-overview.png) | `THESIS_READY`；源为 [`question-import-template.xlsx`](templates/question-import-template.xlsx)，Sheet `题目检查`，说明行、19 列和匿名示例。 | `QuestionImportService`；`POST /api/v1/admin/question-import/preview`、`confirm`；`dao_ru_pi_ci`、`ti_mu`、选项/解析/附件/来源/审核关系表；V2；`FinalImportTemplatesIntegrationTest`。 | 图注：**题目模板以 19 列承载题目、STANDARD、知识点、附件对象和来源信息。** 证明列结构；不证明导入内容已获发布授权。 |
| [EXCEL-03 题目列细节](evidence/final-documentation/excel-question-template-columns.png) | `THESIS_READY`；EXCEL-02 的 A1:S6 精确列视图，无个人信息。 | 同 EXCEL-02；详见 [Excel 指南](EXCEL_IMPORT_GUIDE.md)。 | 图注：**题目导入通过精确表头和对象标记约束附件、来源与审核流程。** 不能证明 Excel 中的审核状态能绕过服务端状态机。 |
| [EXCEL-04 题目导入页面](evidence/thesis-final/36-question-import.png) | `RAW_EVIDENCE + HISTORICAL`；`/admin/questions/import`，ADMIN，匿名 Demo 页面。 | `QuestionImportView.vue`、`QuestionImportService`；preview/confirm API；`dao_ru_pi_ci` 等；V2；导入集成测试。 | 图注：**管理员题目导入采用先预览、后确认的受控流程。** 不能证明截图中的文件已经成功写库。 |
| [EXCEL-05 学生导入页面](evidence/thesis-final/37-student-import.png) | `RAW_EVIDENCE + HISTORICAL`；`/admin/students/import`，ADMIN，匿名 Demo 页面。 | `StudentImportView.vue`、`StudentImportService`；preview/confirm API；组织/账号表；V5/V6；导入集成测试。 | 图注：**管理员学生导入在确认前先呈现校验结果。** 不能证明任何真实学生信息。 |

推荐论文位置：第 5 章批量导入与题库审核；推荐 PPT 标题：**“可核验的 Excel Preview/Confirm 导入链”**。完整字段、限制、对象标记和事务语义见 [Excel 精确导入指南](EXCEL_IMPORT_GUIDE.md)。

## PR #35 发布管理的验收边界

PR #35 没有新增可提交的本地浏览器截图，因而本目录不虚构 `THESIS_READY` 或 `MACHINE_BROWSER_VERIFIED` 图片路径。其教学闭环由用户实际页面审查确认，记录为 [`USER_MANUAL_ACCEPTANCE`](MANUAL_ACCEPTANCE_FINDINGS_PAPER_RELEASE_MANAGEMENT.md)：任课范围“学科（班级）”、全局班级发布记录、班级作答/已提交答卷、纵向 STANDARD 审查、撤回不可见、历史保留和软删除试卷库均已接受。

对应实现为 `TeacherPaperBuilderView.vue`、`api/teacher/papers.ts`、`PaperController`、`PaperAssignmentTeacherController`、`PaperService` 与 `PaperAssignmentService`；核心表为 `shi_juan`、`shi_juan_fa_bu`、`shi_juan_fa_bu_ti_mu`、`shi_juan_ti_jiao`、`shi_juan_xue_sheng_da_ti`，迁移仍是 V27/V30。自动化覆盖见 `PaperAssignmentIntegrationTest`、`PaperServiceIntegrationTest` 和 `TeacherPaperBuilderView.spec.ts`。该文字记录可证明用户已接受相应界面，不可替代截图或机器浏览器视觉证据。

## PR #40 试卷提交与教师同步证据

### FIG-PAPER-SUBMIT-RESULT-01

- **截图文件**：[学生提交后的逐题结果](evidence/paper-submission-sync/student-submitted-result.png)
- **推荐论文图名**：图6-x 学生试卷客观题判分结果界面
- **推荐 PPT 图名**：试卷提交：后端判分事实回读
- **页面路由 / 角色**：`/student/papers/{releaseId}`；STUDENT
- **状态 / 时间 / Git**：`THESIS_READY + MACHINE_BROWSER_VERIFIED`；2026-08-24；`fix/paper-submission-grading-sync@da8f1fb`
- **可见内容**：已提交状态、学生答案、正确/错误、本题得分、正确答案、STANDARD；输入控件已冻结。
- **截图数据**：`rike_tiku_demo` 的 199班物理 release 1；3 道客观题（2 单选、1 多选），总分 30；浏览器使用提交接口返回和随后详情查询，不使用前端伪造分数。学生从该 release 的未提交 Demo 名单动态选择，证据不固定真实个人身份。
- **数据库来源 / 主要字段**：`shi_juan_fa_bu`；`shi_juan_fa_bu_ti_mu.id/ti_mu_lei_xing_kuai_zhao/fen_zhi/zheng_que_da_an_kuai_zhao`；`shi_juan_ti_jiao.zhuang_tai/ke_guan_de_fen/ke_guan_zong_fen/ti_jiao_shi_jian`；`shi_juan_xue_sheng_da_ti.ti_jiao_da_an/zhuang_tai/shi_fou_zheng_que/de_fen`。
- **前端 / API**：`StudentPapersView.vue`、`api/student/papers.ts`、`QuestionContent`、`AnswerDisplay`、`StandardAnalysis`；`POST /api/v1/student/papers/{releaseId}/submit`、`GET /api/v1/student/papers/{releaseId}`。
- **后端**：`PaperAssignmentStudentController`、`PaperAssignmentService`、共享 `ObjectiveAnswerGrader`、`PaperAssignmentDtos`。
- **Flyway / 测试**：V27、V30；`PaperAssignmentIntegrationTest`、`StudentPapersView.spec.ts`、`scripts/paper-submission-sync-browser.cjs`。
- **证明什么**：证明浏览器当前答案按 release itemId 提交，经后端确定性判分写库，再由学生端重新读取逐题结果。
- **论文位置 / 图注**：第5章学生试卷作答与判分。图注：**学生提交试卷后，系统从后端提交事实呈现客观题判分、本题得分和 STANDARD。**
- **不能声称**：不能证明主观题自动评分、真实学生长期学习成效、AI 判分准确率或用户真人验收。

### FIG-PAPER-TEACHER-SYNC-02

- **截图文件**：[教师同步后的已提交答卷](evidence/paper-submission-sync/teacher-submission-synced.png)
- **推荐论文图名**：图6-x 教师查看学生已提交答卷界面
- **推荐 PPT 图名**：教师同步：同一 MySQL 提交事实
- **页面路由 / 角色**：`/teacher/papers`，发布管理 → 作答情况 → 查看答卷；TEACHER
- **状态 / 时间 / Git**：`THESIS_READY + MACHINE_BROWSER_VERIFIED`；2026-08-24；`fix/paper-submission-grading-sync@da8f1fb`
- **可见内容**：学生提交状态、客观得分、提交时间、查看答卷入口，以及纵向的题干、选项、学生答案、正确答案、得分与 STANDARD。
- **截图数据**：与上一图同一 `rike_tiku_demo` release；教师在学生提交前已打开作答弹窗，提交后由 5 秒轮询重新请求 stats/submissions 并出现已提交记录。
- **数据库来源 / 主要字段**：同上一图；班级人数来自 release 班级成员，已提交数只统计 `shi_juan_ti_jiao.zhuang_tai='SUBMITTED'`。
- **前端 / API**：`TeacherPaperBuilderView.vue`、`api/teacher/papers.ts`、`QuestionContent`、`AnswerDisplay`、`StandardAnalysis`；`GET /api/v1/teacher/papers/releases/{releaseId}/stats`、`submissions`、`students/{studentId}/submission`。
- **后端**：`PaperAssignmentTeacherController`、`PaperAssignmentService`、`PaperAssignmentDtos`。
- **Flyway / 测试**：V27、V30；`PaperAssignmentIntegrationTest`、`TeacherPaperBuilderView.spec.ts`、机器浏览器 8/8 assertions。
- **证明什么**：证明教师端不依赖前端学生状态，而是通过两个只读 GET 重新读取同一数据库提交事实；关闭弹窗后轮询停止。
- **论文位置 / 图注**：第5章教师发布与作答管理。图注：**教师作答情况从同一提交事实同步学生状态、客观得分和已提交答卷。**
- **不能声称**：不能证明 WebSocket 实时推送、主观题已批阅、课堂成绩提升或用户真人验收。

## 截图数据只读查询说明

以下 SQL 只用于受控本地或匿名 Demo 环境的核对；不得写入、重置或暴露个人信息。

```sql
-- 专题单元、三阶段题目、题型和知识点关系
SELECT u.id, u.ming_cheng, q.id AS question_id, r.jie_duan, q.ti_mu_lei_xing,
       q.shi_yong_mo_shi, q.zhuan_ti_lei_xing, q.zhuang_tai
FROM zhuan_ti_xue_xi_dan_yuan u
JOIN zhuan_ti_xue_xi_dan_yuan_ti_mu r ON r.zhuan_ti_xue_xi_dan_yuan_id=u.id
JOIN ti_mu q ON q.id=r.ti_mu_id
ORDER BY u.id, r.jie_duan;

-- 专题类型分布
SELECT zhuan_ti_lei_xing, COUNT(*)
FROM ti_mu WHERE shi_yong_mo_shi='TOPIC_LEARNING' AND ti_mu_lei_xing='SUBJECTIVE'
GROUP BY zhuan_ti_lei_xing;

-- 一张试卷的中文化前内部题型分布（展示层必须映射中文）
SELECT q.ti_mu_lei_xing, COUNT(*)
FROM shi_juan_ti_mu p JOIN ti_mu q ON q.id=p.ti_mu_id
WHERE p.shi_juan_id=? GROUP BY q.ti_mu_lei_xing;

-- 发布附件快照与学生主观题待处理数
SELECT p.id, JSON_LENGTH(p.fu_jian_kuai_zhao) AS attachment_count
FROM shi_juan_fa_bu_ti_mu p WHERE p.shi_juan_fa_bu_id=?;
SELECT zhuang_tai, COUNT(*) FROM shi_juan_xue_sheng_da_ti
WHERE shi_juan_ti_jiao_id=? GROUP BY zhuang_tai;
```

这些查询能复核截图数据模型，但不能由截图或查询推导真实学生长期成绩、AI 科学准确率或 OS 打印对话框状态。
