# 功能、截图、代码与数据映射

下表用于代码审查、论文写作和答辩定位。截图均来自匿名 `rike_tiku_demo`；路径为仓库相对路径。

| 功能 | 截图 | 前端路由 / 页面 | 前端 API | Controller / Service | 主要表 | Flyway / 技术 |
|---|---|---|---|---|---|---|
| 登录与 CAPTCHA | `02-login.png` | `/login` · `LoginView.vue` | `api/auth.ts` | `RenZhengKongZhiQi` / `TuXingYanZhengMaFuWu` | `yong_hu`、`yong_hu_jiao_se` | V5；JWT、一次性图形验证码 |
| 首次改密 | `02-login.png` | `/change-initial-password` | `api/auth.ts` | 认证 Controller / Service | `yong_hu` | V5/V10；BCrypt、no-store |
| 学生/教师/班级/任课 | `11-teacher-workspace.png` | `/admin/students`、`/admin/teachers`、`/admin/classes` | `api/admin/*` | Admin Controllers / Services | 档案、班级、任课关系表 | V5/V6；三元权限边界 |
| Excel 导入 | 管理后台截图组 | `/admin/students/import`、`/admin/questions/import` | 导入 API 模块 | Import Controllers / Services | `dao_ru_pi_ci` 等 | V2/V5；preview/confirm 事务 |
| 题库与附件 | 管理后台截图组 | `/admin/questions` | `api/admin/questions.ts` | `QuestionAdminController/Service` | `ti_mu`、选项、解析、附件 | V2；状态机、SHA-256 |
| 练习/判分/STANDARD | `04-practice.png`、`05-result-standard.png` | `/student/practice/*` | `api/student/practice.ts` | `StudentPracticeController/Service` | `lian_xi_*`、`xue_sheng_da_ti` | V7；冻结快照、确定性判分 |
| 错题/掌握度/高频考点 | `06-wrong-questions.png` | `/student/wrong-questions`、教师 scope | student/teacher API | Practice 与 Teacher Workspace | `cuo_ti_ji_lu`、`gao_pin_kao_dian` | V7/V8 |
| 师生私信 | 角色工作台 | `/messages` | `api/messages.ts` | Messaging Controller / Service | `si_xin_hui_hua`、`si_xin_xiao_xi` | V9 |
| AI 错因 | `07-student-ai-analysis.png` | 练习结果 Drawer | `api/student/aiLearning.ts` | `StudentAiController/Service` | `ai_cuo_ti_fen_xi` | V13；事实 hash、严格 JSON |
| AI 当前题答疑 | `08-student-ai-chat.png` | `StudentAiLearningPanel.vue` | `aiLearning.ts` | `StudentAiService` | `ai_hui_hua`、`ai_xiao_xi` | V13/V15；10 轮、字符预算 |
| 模型/思考/搜索 | `08-student-ai-chat.png` | AI Drawer 控件 | `model-options` / `capabilities` | Runtime config、DeepSeek、Search Client | 模型配置、会话与来源 JSON | V16；安全配置 ID、UNTRUSTED context |
| 学生 AI 变式 | `09-student-ai-variant.png`、`10-student-ai-variant-result.png` | `StudentAiVariantPanel.vue` | variants API | `StudentAiVariantController/Service` | `ai_xue_sheng_bian_shi_shi_li` 等 | V19；共享 parser、确定性判分 |
| AI Provider 与 GLM | `17-admin-ai-models.png` | `/admin/ai-models` | `api/admin/aiModels.ts` | `AiModelConfigService`、Provider | `ai_mo_xing_pei_zhi`、视觉缓存 | V14/V16；安全诊断、Mock HTTP |
| AI 候选生成与审核 | `12-teacher-ai-review.png`、`19-admin-ai-generation.png` | teacher/admin AI generation | generation API | `AiQuestionGenerationService` | 生成任务、质量评价、题目 | V14/V19；PENDING→人工审核 |
| 密码恢复通知 | `18-admin-password-notifications.png` | 登录 Dialog、`/admin/password-recovery` | auth/passwordRecovery API | `PasswordRecoveryController/Service` | `mi_ma_chong_zhi_shen_qing` | V17；反枚举、行锁、审计 |
| 组卷/打印/PDF | `13-15` | `/teacher/papers`、预览路由 | `api/teacher/papers.ts` | `PaperController/Service` | `shi_juan`、`shi_juan_ti_mu` | V18；教师 scope、A4 print CSS |
| 操作日志 | 管理后台截图组 | `/admin/operation-logs` | logs API | `GuanLiCaoZuoRiZhiFuWu` | `guan_li_cao_zuo_ri_zhi` | V11；事务结果元数据 |

## 论文插图文件

`docs/evidence/thesis-final` 依次保存 Portal、登录、学生首页、练习、STANDARD、错题、AI 分析、AI 对话、学生变式、教师工作台与审核、组卷、两种试卷预览、管理员首页、模型、密码通知、AI 审核、移动端和三张架构图。未执行的新截图不会标记为已验证；目录中的 README 记录每张图的生成状态。
