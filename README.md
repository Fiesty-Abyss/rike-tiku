# RIKE 理科学习辅助系统

> **分支事实：本分支对应 Draft PR [#33](https://github.com/Fiesty-Abyss/rike-tiku/pull/33)，当前为 Flyway V23、41 张业务表，尚未合并到 `main`。**

面向高中物理、化学、生物的 Spring Boot 大模型题库系统。正式判分与 STANDARD 始终由确定性业务事实控制；AI 只承担解释、答疑和待人工审核的候选生成。

- [论文插图原始证据](docs/evidence/thesis-final/README.md) · [功能—截图—代码—表索引](docs/FEATURE_SCREENSHOT_CODE_INDEX.md)
- [Excel 精确导入指南](docs/EXCEL_IMPORT_GUIDE.md) · [学生模板](docs/templates/student-import-template.xlsx) · [题目模板](docs/templates/question-import-template.xlsx)
- [V23 数据库参考](docs/DATABASE_SCHEMA_REFERENCE.md) · [V23 纯结构快照](database/schema_snapshot_v23.sql) · [SQL 示例](docs/SQL_EXAMPLES.md)
- [论文初稿](docs/thesis/RIKE_THESIS_DRAFT.md) · [事实核对表](docs/thesis/RIKE_THESIS_FACT_CHECK.md) · [答辩提纲](docs/thesis/RIKE_DEFENSE_OUTLINE.md)

## 1. 公共门户

[![RIKE 公共门户首屏 Hero](docs/evidence/readme-preview/01-portal-hero.png)](docs/evidence/thesis-final/01-portal-desktop.png)

### 功能说明

公共门户用物理、化学、生物的共同视觉语言介绍系统，并把未登录用户引导到统一登录入口。README 只展示首屏 Hero；点击缩略图可查看完整长页证据。

### 设计思路

门户承担产品定位和角色入口，不读取业务数据，也不把认证逻辑复制到展示页。首屏先说明系统服务对象，再由独立认证流程处理身份与权限。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：[`/` 路由定义](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/router/index.ts)
- Vue 页面或组件：[`PortalView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/PortalView.vue)
- TypeScript API：无网络 API；入口与权限跳转由 [`router/index.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/router/index.ts) 控制
- 后端 Controller：无；公共门户是静态 SPA 路由
- 后端 Service：无；不读取业务事实
- 主要数据库表：无
- Flyway：无
- 主要技术：Vue 3、Vue Router、响应式 CSS、GSAP

</details>

### 论文与参考

- 可用于论文第 3 章总体架构与第 5 章公共入口设计。
- [Vue Router 官方文档](https://router.vuejs.org/)；[Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)。

## 2. 登录与密码恢复

[![登录、CAPTCHA 与密码恢复入口](docs/evidence/readme-preview/02-login-recovery.png)](docs/evidence/thesis-final/02-login.png)

### 功能说明

用户通过账号、密码和一次性图形验证码登录；忘记密码入口采用统一响应，避免暴露账号是否存在。首次恢复密码后仍受首次改密门禁约束。

### 设计思路

认证、验证码和恢复通知共享安全边界，但恢复流程不改变正常登录表单状态。JWT 只用于后续 API 身份验证，README 和日志不展示 Token 或密码摘要。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：[`/login`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/router/index.ts)
- Vue 页面或组件：[`LoginView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/auth/LoginView.vue)、[`PasswordRecoveryDialog.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/components/auth/PasswordRecoveryDialog.vue)
- TypeScript API：[`auth.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/auth.ts)
- 后端 Controller：[`RenZhengController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/renzheng/RenZhengController.java)、[`PasswordRecoveryController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/zhanghao/PasswordRecoveryController.java)
- 后端 Service：[`RenZhengFuWu.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/renzheng/RenZhengFuWu.java)、[`PasswordRecoveryService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/zhanghao/PasswordRecoveryService.java)
- 主要数据库表：`yong_hu`、`jiao_se`、`yong_hu_jiao_se`、`mi_ma_chong_zhi_shen_qing`
- Flyway：[`V5`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V5__create_user_role_and_profile_tables.sql)、[`V17`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V17__create_password_recovery_requests.sql)
- 主要技术：Spring Security、BCrypt、JWT、一次性 CAPTCHA、反账号枚举

</details>

### 论文与参考

- 可用于论文第 5 章认证流程和第 7 章安全设计。
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)；[OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)。

## 3. 学生自主练习

[![学生自主练习条件与创建流程](docs/evidence/readme-preview/03-student-practice.png)](docs/evidence/thesis-final/04-practice.png)

### 功能说明

学生按学科、知识点、题型、难度和数量创建自主练习。系统只抽取授权范围内的已发布、可自动判分题，并在练习中冻结题干、选项、答案与解析版本。

### 设计思路

冻结快照避免题库后续编辑改变历史答题事实；所有权校验确保学生只能访问本人的练习会话。单选、多选和填空由共享确定性判分器处理，不调用 AI 决定分数。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/student/practice/new`、`/student/practice/:id`
- Vue 页面或组件：[`PracticeNewView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/student/PracticeNewView.vue)、[`PracticeSessionView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/student/PracticeSessionView.vue)
- TypeScript API：[`student/practice.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/student/practice.ts)
- 后端 Controller：[`StudentPracticeController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/xueshenglianxi/StudentPracticeController.java)
- 后端 Service：[`StudentPracticeService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/xueshenglianxi/StudentPracticeService.java)、[`ObjectiveAnswerGrader.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/xueshenglianxi/ObjectiveAnswerGrader.java)
- 主要数据库表：`ti_mu`、`lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`
- Flyway：[`V7`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V7__create_student_practice_and_wrong_question_tables.sql)
- 主要技术：事务、冻结快照、JSON 结构化答案、确定性判分

</details>

### 论文与参考

- 可用于论文第 4 章练习数据模型和第 5 章确定性判分。
- [MySQL 8.4 事务模型](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-model.html)；[Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)。

## 4. STANDARD 与结果

[![练习结果、正确答案与 STANDARD 解析](docs/evidence/readme-preview/04-standard-result.png)](docs/evidence/thesis-final/05-result-standard.png)

### 功能说明

提交后页面对照学生答案与确定性正确答案，并默认展示 STANDARD 解析和知识点。STANDARD 是经过审核、随练习冻结的正式事实，不由 AI 生成结果覆盖。

### 设计思路

把正式答案、解析与 AI 辅助拆开，是系统最重要的可信边界。即使 Provider 未配置或调用失败，分数、正确答案和 STANDARD 仍完整可用。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/student/practice/:id/result`
- Vue 页面或组件：[`PracticeResultView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/student/PracticeResultView.vue)、[`StandardAnalysis.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/components/question/StandardAnalysis.vue)、[`AnswerDisplay.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/components/question/AnswerDisplay.vue)
- TypeScript API：[`student/practice.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/student/practice.ts)
- 后端 Controller：[`StudentPracticeController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/xueshenglianxi/StudentPracticeController.java)
- 后端 Service：[`StudentPracticeService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/xueshenglianxi/StudentPracticeService.java)
- 主要数据库表：`ti_mu_jie_xi`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`
- Flyway：[`V2`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V2__create_question_core_tables.sql)、[`V7`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V7__create_student_practice_and_wrong_question_tables.sql)
- 主要技术：版本冻结、KaTeX、安全科学文本、确定性 JSON 判分

</details>

### 论文与参考

- 可用于论文第 5 章结果反馈和第 6 章 AI/正式事实边界。
- [VanLehn (2011), tutoring effectiveness](https://doi.org/10.1080/00461520.2011.611369)；[Kasneci et al. (2023)](https://doi.org/10.1016/j.lindif.2023.102274)。

## 5. AI 当前题答疑

[![绑定当前题与 STANDARD 的 AI 答疑](docs/evidence/readme-preview/05-ai-question-tutor.png)](docs/evidence/thesis-final/08-student-ai-chat.png)

### 功能说明

学生可以围绕当前已提交题目开启有限会话，并选择管理员开放的模型、标准/深度思考和受控联网搜索。会话绑定冻结题干、学生答案、STANDARD 与知识点，最多 10 轮。

### 设计思路

模型只能读取当前学生有权访问的事实，不能选择任意 endpoint，也不能改写 STANDARD。搜索结果被标记为不可信补充上下文；最终页面不展示或保存 `reasoning_content`。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/student/practice/:id/result`
- Vue 页面或组件：[`StudentAiLearningPanel.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/components/student/StudentAiLearningPanel.vue)
- TypeScript API：[`student/aiLearning.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/student/aiLearning.ts)
- 后端 Controller：[`StudentAiController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/aixuesheng/StudentAiController.java)
- 后端 Service：[`StudentAiService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/aixuesheng/StudentAiService.java)、[`OfficialWebSearchClient.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/ai/search/OfficialWebSearchClient.java)
- 主要数据库表：`ai_hui_hua`、`ai_xiao_xi`、`ai_mo_xing_pei_zhi`、`ai_diao_yong_ri_zhi`
- Flyway：[`V13`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V13__create_student_ai_learning_tables.sql)、[`V15`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V15__expand_student_ai_conversation_to_ten_rounds.sql)、[`V16`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V16__add_student_ai_runtime_choices_and_search_usage.sql)
- 主要技术：DeepSeek Chat Completions、字符预算、Prompt 注入防护、metadata-only 日志、智谱 Web Search

</details>

### 论文与参考

- 可用于论文第 2 章智能辅导与个性化反馈、第 6 章受控 Provider 设计。
- [Reddig, Arora & MacLellan (2025)](https://doi.org/10.1007/s40593-025-00505-6)；[DeepSeek Chat Completion](https://api-docs.deepseek.com/api/create-chat-completion)；[智谱 Web Search](https://docs.bigmodel.cn/cn/guide/tools/web-search)。

## 6. AI 变式练习

[![结构化 AI 变式练习](docs/evidence/readme-preview/06-ai-variant.png)](docs/evidence/thesis-final/09-student-ai-variant.png)

### 功能说明

学生可以基于已提交题目生成一题结构化变式，选择目标难度并完成单选、多选或填空作答。正确答案与解析在作答前隐藏；提交人工审核后仍保持 `PENDING`。

### 设计思路

生成结果必须通过共享 Parser 和确定性判分器，不能直接成为正式题库 STANDARD。母题、生成任务、实例和审核事实分开保存，使换题与失败均可追踪。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/student/practice/:id/result`
- Vue 页面或组件：[`StudentAiVariantPanel.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/components/student/StudentAiVariantPanel.vue)
- TypeScript API：[`student/aiLearning.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/student/aiLearning.ts)
- 后端 Controller：[`StudentAiVariantController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/aixuesheng/StudentAiVariantController.java)
- 后端 Service：[`StudentAiVariantService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/aixuesheng/StudentAiVariantService.java)、[`AiQuestionGenerationService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/aishengcheng/AiQuestionGenerationService.java)
- 主要数据库表：`ai_xue_sheng_bian_shi_shi_li`、`ai_sheng_cheng_ren_wu`、`ti_mu`、`ti_mu_shen_he_ji_lu`
- Flyway：[`V14`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V14__create_ai_generation_and_model_tables.sql)、[`V19`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V19__create_student_ai_variant_instances.sql)
- 主要技术：结构化生成、严格 Parser、内容 hash、事务、确定性判分、PENDING 人工门禁

</details>

### 论文与参考

- 可用于论文第 2 章自动出题、第 6 章结构化生成与第 7 章人工审核。
- [Das et al. (2021), automatic question generation survey](https://doi.org/10.1186/s41039-021-00151-1)；[Elkins et al. (2024)](https://doi.org/10.1609/aaai.v38i21.30353)。

## 7. 教师任课工作台

[![教师 ACTIVE 任课范围工作台](docs/evidence/readme-preview/07-teacher-workspace.png)](docs/evidence/thesis-final/11-teacher-workspace.png)

### 功能说明

教师工作台只展示本人 ACTIVE 任课关系关联的班级、科目和学生学习信息，并提供考点、组卷、消息和候选审核入口。不同班级与学科通过明确的任课范围隔离。

### 设计思路

权限基点不是前端选择项，而是服务端再次验证的“教师—班级—科目”三元关系。统计和内容查询都从同一范围出发，避免横向越权。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/teacher/scopes/:scopeId`
- Vue 页面或组件：[`TeacherScopeWorkspaceView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/teacher/TeacherScopeWorkspaceView.vue)
- TypeScript API：[`teacher.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/teacher.ts)
- 后端 Controller：[`JiaoShiGaoPinKaoDianController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/jiaoshi/JiaoShiGaoPinKaoDianController.java)
- 后端 Service：[`JiaoShiGaoPinKaoDianFuWu.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/jiaoshi/JiaoShiGaoPinKaoDianFuWu.java)
- 主要数据库表：`jiao_shi_dang_an`、`ban_ji`、`ren_ke_guan_xi`、`gao_pin_kao_dian`
- Flyway：[`V6`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V6__create_class_and_teaching_relationship_tables.sql)、[`V8`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V8__create_high_frequency_point_table.sql)
- 主要技术：三元任课授权、ACTIVE scope、事务、聚合查询

</details>

### 论文与参考

- 可用于论文第 4 章组织模型和第 7 章应用层权限隔离。
- [MySQL 8.4 Foreign Key Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html)；[Spring Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)。

## 8. 教师组卷

[![教师手动与规则组卷](docs/evidence/readme-preview/08-teacher-paper-builder.png)](docs/evidence/thesis-final/13-teacher-paper-builder.png)

### 功能说明

教师可从本人任教学科的已发布题中手动组卷，也可按题型、难度、知识点和数量进行规则组卷。保存后冻结题序和分值，并分别生成学生版与答案解析版。

### 设计思路

组卷只消费正式 `PUBLISHED` 题目，试卷题目关系通过唯一约束保证顺序和题目不重复。打印采用浏览器 A4 样式，不额外引入 PDF 服务。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/teacher/papers`
- Vue 页面或组件：[`TeacherPaperBuilderView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/teacher/TeacherPaperBuilderView.vue)、[`PaperPreviewView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/teacher/PaperPreviewView.vue)
- TypeScript API：[`teacher/papers.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/teacher/papers.ts)
- 后端 Controller：[`PaperController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/shijuan/PaperController.java)
- 后端 Service：[`PaperService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/shijuan/PaperService.java)
- 主要数据库表：`shi_juan`、`shi_juan_ti_mu`、`ti_mu`、`ti_mu_zhi_shi_dian`
- Flyway：[`V18`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V18__create_teacher_papers.sql)
- 主要技术：规则过滤、冻结顺序/分值、唯一约束、CSS `@media print`

</details>

### 论文与参考

- 可用于论文第 5 章教师组卷工作流和第 4 章试卷关系模型。
- [MySQL 8.4 CHECK Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html)；[MDN Printing](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_media_queries/Printing)。

## 9. 管理员 AI 模型配置

[![管理员 DeepSeek、GLM 与 Web Search 配置](docs/evidence/readme-preview/09-admin-ai-models.png)](docs/evidence/thesis-final/17-admin-ai-models.png)

### 功能说明

管理员分别管理 TEXT、VISION 和 SEARCH 用途的受控配置。页面只显示 Key 是否已配置；连接测试返回安全状态、延迟和时间，不回显 Key、Base64、Prompt 或 Provider 原始响应。

### 设计思路

Provider 配置与学生业务 API 分离，前端只能提交受控模型 ID。DeepSeek 负责文本能力，GLM-4.6V-Flash负责视觉上下文，智谱 Web Search 提供结构化来源；未配置时确定性主链仍可运行。

<details>
<summary>实现与数据库映射</summary>

- 前端路由：`/admin/ai-models`
- Vue 页面或组件：[`AdminAiModelsView.vue`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/views/admin/AdminAiModelsView.vue)
- TypeScript API：[`admin/aiModels.ts`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-frontend/src/api/admin/aiModels.ts)
- 后端 Controller：[`AiModelConfigController.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/ai/admin/AiModelConfigController.java)
- 后端 Service：[`AiModelConfigService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/ai/admin/AiModelConfigService.java)、[`AiProviderService.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/ai/AiProviderService.java)、[`GlmVisionProvider.java`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/java/com/neu/riketiku/ai/vision/GlmVisionProvider.java)
- 主要数据库表：`ai_mo_xing_pei_zhi`、`ai_diao_yong_ri_zhi`、`ai_shi_jue_shang_xia_wen`
- Flyway：[`V12`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V12__create_ai_call_log.sql)、[`V14`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V14__create_ai_generation_and_model_tables.sql)、[`V16`](https://github.com/Fiesty-Abyss/rike-tiku/blob/feat/final-product-completion/rike-tiku-backend/src/main/resources/db/migration/V16__add_student_ai_runtime_choices_and_search_usage.sql)
- 主要技术：DeepSeek Chat Completions、GLM-4.6V-Flash、智谱 Web Search、超时/重试、安全错误分类、metadata-only 日志

</details>

### 论文与参考

- 可用于论文第 3 章技术架构、第 6 章 Provider 边界和第 7 章隐私治理。
- [DeepSeek API](https://api-docs.deepseek.com/api/create-chat-completion)；[GLM-4.6V-Flash](https://docs.bigmodel.cn/cn/guide/models/free/glm-4.6v-flash)；[UNESCO 生成式 AI 教育指南](https://unesdoc.unesco.org/ark:/48223/pf0000386693)。

## 工程、数据库与论文导航

- 技术栈：Java 25、Spring Boot 4.1、Vue 3、TypeScript、MySQL 8.4、Flyway V1–V23。
- 数据模型：41 张业务表；[字段/约束参考](docs/DATABASE_SCHEMA_REFERENCE.md)、[纯结构快照](database/schema_snapshot_v23.sql)、[ER 模块图](database/diagrams/rike_tiku_er.md)。
- Excel：[学生模板](docs/templates/student-import-template.xlsx)、[题目19列模板](docs/templates/question-import-template.xlsx)、[Preview/Confirm 指南](docs/EXCEL_IMPORT_GUIDE.md)。
- 论文：[写作资料中心](docs/THESIS_WRITING_HUB.md)、[论文初稿](docs/thesis/RIKE_THESIS_DRAFT.md)、[事实核对表](docs/thesis/RIKE_THESIS_FACT_CHECK.md)、[答辩提纲](docs/thesis/RIKE_DEFENSE_OUTLINE.md)。
- 完整文献：[THESIS_REFERENCES.md](docs/THESIS_REFERENCES.md)；引用管理：[references.bib](docs/references/references.bib)。文献用于说明研究与设计依据，不代表 RIKE 自身实验结果。

真实 Provider 状态以当前验收记录为准：DeepSeek variant、DeepSeek tutor、GLM Vision、Web Search 均为 `NOT_RUN`。人工验收状态为 `FINAL_USER_REVIEW_PENDING`。
