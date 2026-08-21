# 答辩代码—业务速查图

> 本表只记录当前 `main` 的真实代码边界。它用于答辩定位，不把 Fake/Test Provider 或无凭据的外部调用写成真实模型效果。

## 学生错因分析

`StudentAiLearningPanel.vue → api/student/aiLearning.ts → StudentAiController → StudentAiService → StudentAiPromptFactory → StudentAiProviderClient/ProviderStudentAiProviderClient → AiProviderService → DeepSeekAiModelProvider → StudentAiAnalysisParser → ai_cuo_ti_fen_xi`

- 输入是已提交题目的冻结事实、学生答案、确定性判分和 STANDARD；题干等业务事实属于 `UNTRUSTED_DATA`，不能覆盖系统指令。
- 缓存不是 Redis：`ai_cuo_ti_fen_xi` 以事实哈希和 prompt version 复用可验证的错因结果。
- STANDARD、正式分数和正确答案不受 AI 输出覆盖；`reasoning_content` 不展示、不持久化。

## 当前题答疑

`StudentAiLearningPanel.vue → aiLearning.ts → StudentAiController → StudentAiService → StudentAiPromptFactory → AiProviderService → DeepSeekAiModelProvider → ai_hui_hua / ai_xiao_xi`

- 服务端限制 `MAX_ROUNDS = 10`，同时限制单条消息、最近消息数和 history character budget；所有会话经过 owner guard 与 prompt-injection guard。
- 会话把当前题、知识点、学生答案、STANDARD 作为受控上下文；模型仅能解释和追问，不能改正式事实。

## AI 候选题

`教师/管理员生成工作区 → AiQuestionGenerationTeacherController 或 AiQuestionGenerationAdminController → AiQuestionGenerationService → AiQuestionGenerationPromptFactory → AiProviderService → DeepSeekAiModelProvider → Parser → 一次 repair → novelty ACCEPT/WARN/REJECT → QuestionAdminService → ti_mu / ti_mu_xuan_xiang / ti_mu_jie_xi → PENDING → 人工审核`

- AI 输出必须符合 Schema V2；解析失败最多修复一次。
- 候选题绝不自动 PUBLISHED；人工审核后才可以成为正式题库事实。

## Vision 与 Web Search

- Prompt/Provider 文件：`ai/vision/GlmVisionProvider.java`、`ai/vision/XaiVisionProvider.java`、`ai/provider/DeepSeekAiModelProvider.java`。
- Vision 上下文使用附件集 hash、provider、model 和 prompt version 存于 `ai_shi_jue_shang_xia_wen`；它是数据库级可失效缓存，不是共享 Redis。
- Web Search 是独立受控上下文；真实调用是否可用取决于运行时凭据。当前没有可用凭据时状态必须写作 `BLOCKED_EXTERNAL_PROVIDER`。

## 存储与缓存地图

| 业务事实 | 主要存储 | 缓存/边界 |
|---|---|---|
| CAPTCHA | 内存 `ConcurrentHashMap` | 一次性、短生命周期；无 Redis。 |
| AI 错因 | `ai_cuo_ti_fen_xi` | fact hash + prompt version。 |
| 当前题会话 | `ai_hui_hua`、`ai_xiao_xi` | owner guard、10 轮限制。 |
| 生成任务/候选质量 | `ai_sheng_cheng_ren_wu`、`ai_hou_xuan_ti_zhi_liang_ping_jia` | 需审核，不能改 STANDARD。 |
| 模型配置与调用日志 | `ai_mo_xing_pei_zhi`、`ai_diao_yong_ri_zhi` | 每次业务调用查询配置，确保改配置立即生效；无共享配置缓存。 |
| 视觉上下文 | `ai_shi_jue_shang_xia_wen` | 附件 hash + provider/model/prompt version。 |
| 最终正式候选 | `ti_mu`、`ti_mu_xuan_xiang`、`ti_mu_jie_xi` 等 | 进入 PENDING 后仍需人工审核。 |

## 演示认证与数据卫生速查

| 事项 | 真实实现 / 事实 | 答辩边界 |
|---|---|---|
| 默认/重置密码 | `app.account.default-reset-password` → `AdminDefaultPasswordPolicy` → BCrypt `mi_ma_zhai_yao` | 本地配置为 `a1234567`；不在 API、日志、截图或 GitHub 文档保存哈希/密钥。 |
| 新增账号 | `StudentManagementService`、`StudentImportConfirmService`、`JiaoShiGuanLiFuWu` | 新建账户写入 `shi_fou_shou_ci_deng_lu=1`；首次登录必须改密。 |
| 密码恢复 | `PasswordRecoveryService` | 管理员处理后使用统一默认策略并重新进入首次改密状态；用户主动改密仍可用。 |
| 初始密码门禁 | `ChuShiMiMaMenJinGuoLvQi`、`SecurityConfig`、`router/index.ts`、`ChangeInitialPasswordView.vue` | JWT 后强制门禁返回 `MUST_CHANGE_PASSWORD`；只允许身份确认和改密接口，前端同步跳转。 |
| 203 隔离演示 | `ren_ke_guan_xi`、`ban_ji`、`jiao_shi_dang_an` | 张生康仅 203 班物理、非 ADMIN；199/200 和三位旧核心教师不被清理或重建。 |

## 论文与答辩可说/不可说

- 可说：系统采用模块化单体；客观题确定性判分；主观题 `SUBJECTIVE_PENDING` 不自动评分；AI 解释/生成候选与 STANDARD 隔离；Provider 有统一切换层。
- 不可说：Fake/Test Provider 是真实模型验证；AI 自动给正式主观题分数；真实课堂成绩已提升；无凭据的外部 Provider 已成功。

相关事实入口：[最终项目事实包](FINAL_PROJECT_FACTS.md)、[AI 最终实验结果](AI_FINAL_EXPERIMENT_RESULTS.md)、[功能技术地图](FEATURE_CODE_TECH_MAP.md)。
