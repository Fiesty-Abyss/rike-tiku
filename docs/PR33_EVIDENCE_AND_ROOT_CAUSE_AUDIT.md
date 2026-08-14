# PR #33 证据与根因审计

> 审计状态：`PR33_EVIDENCE_AND_ROOT_CAUSE_AUDIT_BLOCKED`
>
> 审计对象：`feat/final-product-completion@2e0c1332f66df29cc9e4f2edc6c15393688edf82`
>
> 审计边界：只读代码、Git、GitHub、`rike_tiku` 与 `rike_tiku_demo`；除本文件和 `docs/evidence/pr33-audit/` 外没有修改仓库文件。没有修改正式库或 Demo 数据，没有调用真实 Provider，没有执行产品测试。

## 1. Git 与 PR 事实

| 项目 | 事实 |
|---|---|
| 本地分支 | `feat/final-product-completion` |
| local HEAD | `2e0c1332f66df29cc9e4f2edc6c15393688edf82` |
| `origin/main` | `c8747c84802f0a0ecbb8115a1a8913762a59d4b9` |
| `origin/feat/final-product-completion` | `2e0c1332f66df29cc9e4f2edc6c15393688edf82` |
| PR #33 | OPEN / Draft / GitHub 字段 `MERGEABLE` / 未合并 |
| PR base/head | `c8747c8…` / `2e0c133…` |
| PR commit/file count | 该历史审计时点的统计已失效；当前值必须由 PR API 实时读取 |
| PR body HEAD | `99296dce65f2eb400474d7e64a803e4a2e174b32`，已过期 |
| PR body数据库口径 | V23 / 41 表 / `schema_snapshot_v23.sql`，未记录仓库已有 V24 |

接管时工作区 clean。审计过程中只新增本报告和审计证据目录。没有创建分支、PR #34、force push、rebase、squash 或 merge。

## 2. 文档口径漂移

仓库最新迁移文件为 `V24__allow_audited_student_variant_tasks.sql`；两个可读取数据库的已安装版本均为 V23，业务表均为 41 张。因 V24 只改约束、不建表，迁移后表数仍应为 41，但必须由迁移实测确认，不能仅凭脚本推断写成运行事实。

| 文件 | 当前口径 | 当前代码/运行事实 | 状态 | 后续修正要求 |
|---|---|---|---|---|
| `README.md` | V23 / 41 / snapshot V23 | 仓库迁移最新 V24；DB 仍 V23 | STALE | 完成 V24 临时库、Demo、正式库验证后统一口径并链接新快照 |
| `docs/DATABASE_SCHEMA_REFERENCE.md` | V23 / 41 | 最新迁移 V24 | STALE | 由最终 `information_schema` 重新生成约束来源 |
| `database/schema_snapshot_v23.sql` | V23 | 没有 V24 snapshot | STALE | 从迁移到最终版的空库导出纯结构，不手工改旧快照 |
| `docs/thesis/RIKE_THESIS_DRAFT.md` | V1–V23 / 41 | 仓库最新 V24，运行库 V23 | STALE | 区分“仓库最新”“Demo 已执行”“正式已执行” |
| `docs/thesis/RIKE_THESIS_FACT_CHECK.md` | V23 尚未应用正式库 | 正式库当前已是 V23 | STALE | 用真实 Flyway 查询、备份 SHA 和基线查询更新 |
| `docs/thesis/RIKE_DEFENSE_OUTLINE.md` | V1–V23 / 41 | 仓库最新 V24 | STALE | 最终验证后更新 |
| `docs/DEVELOPMENT_STATUS.md` | 同时出现 V14/35、V19/39 | 当前正式/Demo V23/41，仓库最新 V24 | STALE | 历史事实与当前事实分节，不再混写为现状 |
| `docs/evidence/thesis-final/24-database-modules.svg` | 图内 V14/35、snapshot V14；清单称 V19/39 | 当前仓库 V24、运行库 V23、41 表 | STALE | 从最终表清单重绘并同步清单说明 |
| `docs/evidence/browser-results-pr33.json` | 17 路由、0 错误 | 提交于 `99c1e03…`，早于当前 HEAD 和最新 UI 补丁 | STALE | 当前 HEAD 上重跑并记录 API/console/page error |
| PR #33 body | HEAD `99296d…`、V23、snapshot V23 | HEAD `2e0c133…`、仓库最新 V24 | STALE | 最终证据提交后一次性同步，不提前宣称 Provider/真人通过 |

## 3. V19/V24 约束结论

### 3.1 当前数据库事实

只读查询结果：

| 数据库 | Flyway | 表数 | `ck_ai_sheng_cheng_role` | `ck_lian_xi_ti_mu_nan_du` |
|---|---:|---:|---|---|
| `rike_tiku` | V1–V23 全部 success | 41 | `ADMIN,TEACHER,STUDENT` | `IN (1,2,3)` |
| `rike_tiku_demo` | V1–V23 全部 success | 41 | `ADMIN,TEACHER,STUDENT` | `IN (1,2,3)` |

`V19__create_student_ai_variant_instances.sql` 已明确 drop 并重建 `ck_ai_sheng_cheng_role`，允许 `STUDENT`。因此：

- 当前没有证据支持“V14 CHECK 拒绝 STUDENT 是用户 500 的根因”。
- 用户遇错时究竟运行 V19、V23 或其他版本，原现场没有保留 DB version、异常栈或任务 ID，现阶段无法反推。
- V24 再次以相同表达式重建 role CHECK，没有新增语义，属于冗余修复。
- V24 的另一部分把 `lian_xi_ti_mu.nan_du_kuai_zhao` 从 1–3 扩到 1–5。这与目标难度 1–5 的总体数据模型一致，但当前学生变式实例不会新建 `lian_xi_ti_mu`，所以它不是当前变式生成链必然需要的修复；需用具体业务路径测试证明必要性。
- “上一轮 V14 CHECK 拒绝 STUDENT”的原始 SQL 错误、异常类、堆栈和当时 constraint 查询均不存在，必须撤销“已证实根因”口径。

### 3.2 当前数据旁证

正式库只读可见 3 个 STUDENT 生成任务（id 2/5/6）均为 SUCCESS，且各有 READY 学生实例；当前有 4 道 DISTINCT AI PENDING 候选。它证明 V23 数据库允许 STUDENT 任务落库，但不证明用户当时的请求成功，也不授权复用正式数据做 smoke。

## 4. AI 变式真实复现

### 4.1 本轮未能合法执行的命令

目标命令：`POST /api/v1/student/ai/variants`。

阻塞事实：

- `rike_tiku_demo`：14 用户、0 `xue_sheng_da_ti`、0 `ai_mo_xing_pei_zhi`、0 消息、0 学生变式实例；不存在可用 `answerFactId` 或 Provider 配置。
- 本轮明确禁止修改 Demo 数据，所以不能完成练习、插入模型配置或生成前置事实。
- `rike_tiku` 有可用 TEXT 数据库配置和既有答题事实，但本轮禁止修改正式库；调用生成接口必然创建任务、候选题和实例，不能执行。
- Process/User/Machine 都没有 AI Key 环境变量，无法让隔离临时库在不读取/复制正式库秘密的情况下调用 Provider。

因此没有合法方式同时满足“当前分支 + 匿名 Demo + 真实 Provider + 不修改 Demo/正式库”。本轮没有伪造 HTTP status、业务 code、异常栈或 smoke PASS。

### 4.2 可证实与不可证实的根因

当前 HEAD `2e0c133…` 修复了 `StudentAiVariantService.require()` 中 MySQL 不支持的 `JSON_ARRAYAGG(JSON_OBJECT(...) ORDER BY ...)` 写法，改为有序派生表后再聚合。这个改动与“候选及实例已写入、随后读取详情时产生通用 500”高度吻合，是有代码差异支撑的候选根因。

但缺少用户现场的后端异常和请求关联 ID，故结论只能是 **PROBABLE_NOT_PROVEN**，不能写成已证实。当前 DB 也不支持“role CHECK 是根因”的说法。

## 5. 事务原子性风险

当前调用顺序：

1. `AiQuestionGenerationService.insertTask()` 以独立自动提交写入 GENERATING task；
2. Provider + Parser；
3. `TransactionTemplate` 内持久化 `ti_mu` 及子表、`ai_hou_xuan_ti_zhi_liang_ping_jia`，并把 task 改为 SUCCESS；
4. 返回 `StudentAiVariantService.generate()` 后，再以独立语句插入 `ai_xue_sheng_bian_shi_shi_li`。

`StudentAiVariantService.generate()` 在当前 HEAD 已去掉 `@Transactional`。若第 4 步失败，将遗留：

- SUCCESS `ai_sheng_cheng_ren_wu`；
- PENDING `ti_mu`；
- 其 options、answer、STANDARD、knowledge、sources 等子记录；
- `ai_hou_xuan_ti_zhi_liang_ping_jia`；
- 没有 `ai_xue_sheng_bian_shi_shi_li`。

这会形成可审核候选但无法被学生继续作答的孤儿链。

下一轮确定性测试设计：在随机临时库为 `ai_xue_sheng_bian_shi_shi_li` 创建仅测试生命周期的 `BEFORE INSERT` trigger，`SIGNAL SQLSTATE '45000'`；Fake Provider 返回严格合法单选候选。断言实例 0、该 task 不得为 SUCCESS、该 task 对应 PENDING 候选/质量评价为 0、母题 STANDARD hash 不变、AI 调用日志仅保留安全 Provider 成功元数据。测试结束删除 trigger/随机库。

建议不是事后删除整棵候选，而是重构生成编排：候选持久化、质量评价、task SUCCESS 与学生实例插入进入同一业务事务；失败时回滚候选链，并在独立短事务把 task 标记 FAILED/稳定业务码。必须一致的表为 `ai_sheng_cheng_ren_wu`、`ti_mu` 及七类题目子表、`ai_hou_xuan_ti_zhi_liang_ping_jia`、`ai_xue_sheng_bian_shi_shi_li`。

## 6. Provider 配置来源

### 6.1 环境和本地配置

五个指定变量在 Process/User/Machine 全部为 ABSENT。仓库没有 `.idea/runConfigurations`、`.idea/workspace.xml` 或 `_LOCAL`；两个 PowerShell profile 不存在；仓库脚本没有这些变量名。没有读取或输出任何值。

### 6.2 数据库配置（仅布尔）

| 数据库 | 用途 | Provider/model | enabled/default | base URL | Key | 最近状态 |
|---|---|---|---|---:|---:|---|
| `rike_tiku` | TEXT | DEEPSEEK / `deepseek-v4-flash` | 是/是 | 1 | 1 | SUCCESS，963 ms，2026-08-12 15:44:09 |
| `rike_tiku` | VISION | GLM / `glm-4.6v-flash` | 是/是 | 1 | 1 | FAILED，384 ms，2026-08-13 09:48:14 |
| `rike_tiku_demo` | 全部 | 无记录 | 否 | 0 | 0 | 无 |

实际解析规则来自 `AiRuntimeConfigurationService`：优先查询启用的数据库配置，没有记录才 fallback 到环境属性。

| 调用 | 正式运行库来源 | Demo 来源 |
|---|---|---|
| DeepSeek TEXT/variant/tutor | `DATABASE_CONFIG` | `NONE` |
| GLM Vision | `DATABASE_CONFIG` | `NONE` |
| Web Search | `NONE`（没有 SEARCH DB 记录；fallback 禁用且无 Key） | `NONE` |

`RealDeepSeekSmokeTest` 只从 `RIKE_TIKU_AI_API_KEY` 取 Key，并用随机测试库；它不能直接复用数据库配置。`docs/AI_FINAL_EXPERIMENT_RESULTS.md` 记录 PR #31 的 DeepSeek 2xx 历史事实，但没有保存当时进程启动命令或配置来源证据，因此只能证明历史调用，不足以证明当时由 ENV 还是 DB 提供。当前可安全确认的是正式库 DB 配置存在；因本轮禁止写正式库且禁止复制秘密到 Demo，没有执行真实 smoke。

## 7. UI 浏览器事实

隔离运行方式：当前 HEAD，`rike_tiku_demo`，Flyway runtime disabled（避免自动应用 V24），只读打开登录页；CAPTCHA test code 仅由机器运行态暴露但没有用于登录。结束后 18080/18081 已释放。

证据：

- [`login-recovery-trigger-desktop.png`](evidence/pr33-audit/login-recovery-trigger-desktop.png)
- [`login-recovery-dialog-desktop.png`](evidence/pr33-audit/login-recovery-dialog-desktop.png)
- [`login-recovery-dialog-mobile.png`](evidence/pr33-audit/login-recovery-dialog-mobile.png)
- [`ui-audit.json`](evidence/pr33-audit/ui-audit.json)

| 检查 | 浏览器事实 |
|---|---|
| 登录恢复入口 | `appearance:none`、透明背景、透明边框、245.6×38 px；不是浏览器默认灰色按钮 |
| hover | 半透明背景、颜色变化、`translateY(-1px)` |
| focus | 元素获得 focus，2.66667 px 可见 outline |
| 1440×900 Dialog | 520×876，CAPTCHA 和提交按钮可见，横向溢出 0，console error 0 |
| 390×844 Dialog | 366×820，CAPTCHA 和提交按钮可见，横向溢出 0，console error 0 |
| Teleport | Element Plus overlay 位于 body 下；桌面/移动端覆盖视口 |

未取得并因此构成门禁缺口：

- Demo 消息表为 0，不能在禁止写 Demo 的前提下生成 `message-menu-*`、recall/delete 请求或双方可见性证据。
- Demo 没有练习事实，不能进入真实审核/结果链生成单选、多选、填空答案截图。
- 没有解 CAPTCHA 或使用 token 绕过登录；因此未把认证页面的组件存在冒充浏览器 PASS。

## 8. 前端测试缺口

| 功能 | 当前测试文件 | 当前实际覆盖 | 缺失测试 | 下一轮应新增 |
|---|---|---|---|---|
| 登录恢复入口 | `LoginView.spec.ts` | 文本、class、aria、组件存在 | computed style、hover、focus-visible、390px | 真实 CSS 加载的 browser/component style assertions |
| Recovery Dialog | `PasswordRecoveryDialog.spec.ts` | 反枚举文案和字段文本 | teleport、宽度、CAPTCHA refresh、disabled、成功关闭、失败刷新、登录表单保持、Esc/overlay/Tab | 不把 `ElDialog` stub 掉的 DOM/browser tests |
| 消息菜单 | `MessageConversationView.spec.ts` | 加载、标记已读、发送、轮询停止 | dropdown、Popconfirm、recall/hide API、撤回后菜单、本人/对方差异、移动端 | API mock + Element Plus 非空 stub + browser two-party flow |
| 消息 API | `api/messages.spec.ts` | contacts/conversations/fetch/send/read | `recallMessage`、`hideMessage` 路径/方法 | 精确 POST/DELETE assertions |
| AnswerDisplay | `AnswerDisplay.spec.ts` | 单选、多选、填空、JSON string、主观、非法 JSON | 空答案状态、诊断 details 权限、三个真实审核页面集成 | empty + parent view integration + browser screenshots |
| 学生变式面板 | `StudentAiVariantPanel.spec.ts` | 一个 Mock 单选成功链 | 多选、填空、错误码、换题、PENDING、难度 1–5、真实 API | 参数化组件测试和 browser real chain |
| 学生变式后端 | `StudentAiVariantServiceIntegrationTest` | Fake Provider 正常结构/判分的部分路径 | auth/429/timeout、invalid/缺选项/答案不一致、实例写入失败原子性、换题审计、真实 Provider | 独立故障注入测试与一次受控 smoke |
| 移动端 | 无专门消息/答案测试 | 本轮仅密码恢复 390px 机器截图 | 消息菜单、答案、variant 横向溢出 | 390×844 browser suite |

## 9. 截图与论文证据矩阵

“过期”表示图片提交早于审计 HEAD，不能证明之后补丁；不等于图片内容必然错误。`99c1e03` 组为 2026-08-12 机器 Demo，README 9 张缩略图为 `99296dc`。09/10 明确是 UI 夹具。

| 功能 | 当前图片 | 图片 SHA | 当前/过期 | Demo/Mock/Real | 可用于论文 | 缺失原因 |
|---|---|---|---|---|---|---|
| Portal | 01 / 20 | `99c1e03` | 过期 | 静态页面 | 可，需注明版本 | 未在当前 HEAD 重拍 |
| 登录 | 02 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 早于 UI 补丁 |
| 密码恢复 | 本审计 3 张 | 未提交前 | 当前审计 | Demo API | 否，审计临时证据 | 非最终匿名论文目录 |
| 首次改密 | 无 | — | 缺失 | — | 否 | 无截图 |
| 学生导入 | 无 | — | 缺失 | — | 否 | 无截图 |
| 教师管理 | 无 | — | 缺失 | — | 否 | 无截图 |
| 班级与任课 | 11（仅工作台） | `99c1e03` | 过期/不完整 | Demo API | 部分 | 无管理流程图 |
| 题库导入 | 无 | — | 缺失 | — | 否 | 无 Preview/Confirm |
| 题目草稿 | 无 | — | 缺失 | — | 否 | 无截图 |
| 附件 | 无 | — | 缺失 | — | 否 | 无下载/权限证据 |
| 审核发布 | 12 / 19 | `99c1e03` | 过期 | Demo API | 部分 | 不含答案新展示 |
| 学生首页 | 03 / 21 | `ea286e2` / `99c1e03` | 过期 | Demo API | 可，需重拍 | 早于当前 HEAD |
| 练习配置 | 无（04 已在答题） | — | 缺失 | — | 否 | 配置页未单列 |
| 实际答题 | 04 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 早于当前 HEAD |
| 结果 STANDARD | 05 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 未证明答对题默认展开 |
| 错题详情 | 06 | `99c1e03` | 过期 | Demo API | 部分 | 仅列表/旧版 |
| AI 错因 | 07 | `99c1e03` | 过期 | Demo 已有证据，非 real smoke | 可作 UI，非 Provider 实验 | 无当前 Provider 事实 |
| AI 答疑 | 08 | `99c1e03` | 过期 | Demo 已有证据 | 可作 UI，非 real smoke | 无当前 Provider 事实 |
| 搜索来源 | 无 | — | 缺失 | — | 否 | 当前 SEARCH 来源为 NONE |
| AI 变式生成 | 09 | `99c1e03` | 过期 | UI fixture | 否作真实链 | 非 API/Provider |
| AI 变式作答 | 10 | `99c1e03` | 过期 | UI fixture | 否作真实链 | 非 API/Provider |
| PENDING | 10 仅文案 | `99c1e03` | 过期/不足 | UI fixture | 否 | 无 DB/API 状态证据 |
| 专题题 | 无 | — | 缺失 | — | 否 | 五类专题未截图 |
| 专题图片 | 无 | — | 缺失 | — | 否 | DTO/权限链未形成证据 |
| 专题 AI | 无 | — | 缺失 | — | 否 | 无 10 轮会话证据 |
| 消息列表 | 无 | — | 缺失 | — | 否 | Demo 无消息 |
| 消息菜单 | 本轮未取得 | — | 缺失 | — | 否 | 禁止写 Demo，消息表 0 |
| 教师工作台 | 11 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 早于 V20+ UI |
| 私有题库 | 无 | — | 缺失 | — | 否 | 无完整浏览器链 |
| 组卷 | 13 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 旧版 |
| 学生版试卷 | 14 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 旧版 |
| 答案版试卷 | 15 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 旧版 |
| 知识卡片 | 无 | — | 缺失 | — | 否 | 数据结构存在但无最终 UI 证据 |
| 管理员总览 | 16 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 旧版 |
| 模型配置 | 17 | `99c1e03` | 过期 | Demo 安全元数据 | 可作配置 UI | 非真实连接 |
| GLM 诊断 | 无 | — | 缺失 | — | 否 | 正式配置最近 FAILED，Demo 无配置 |
| 密码通知 | 18 | `99c1e03` | 过期 | Demo API | 可，需重拍 | 旧版 |
| 操作日志 | 无 | — | 缺失 | — | 否 | 无截图 |

附加不一致：`24-database-modules.svg` 图内仍是 V14/35，而清单写 V19/39；两者都落后。当前 README 仅 9 张预览图，不能覆盖上述最终实现矩阵。

## 10. 数据库与论文资料缺口

下一轮必须以这些数据源生成，而不是直接替换数字：

| 文件 | 必须使用的数据源 |
|---|---|
| `README.md` | 最终远程 HEAD、`gh pr view`、最终 Demo browser matrix、最终 Provider 状态 |
| `docs/DATABASE_SCHEMA_REFERENCE.md` | 最终随机空库 `information_schema` + migration 文件 |
| 新 `database/schema_snapshot_vNN.sql` | 从 V1 迁移到最终版的空库 `mysqldump --no-data`，秘密/INSERT 扫描 |
| `docs/evidence/thesis-final/24-database-modules.svg` | 最终表清单、FK 模块和 Flyway history |
| `docs/thesis/RIKE_THESIS_DRAFT.md` | 实测测试数、迁移结果、Provider 明确分级 |
| `docs/thesis/RIKE_THESIS_FACT_CHECK.md` | 命令输出、备份大小/SHA、正式库迁移前后基线 |
| `docs/thesis/RIKE_DEFENSE_OUTLINE.md` | 已验证事实，不复用计划数字 |
| `docs/DEVELOPMENT_STATUS.md` | 历史 V14/V19 与当前状态分段 |
| `docs/evidence/browser-results-pr33.json` | 当前 HEAD 上真实路由、console/page/request/overflow assertions |
| PR #33 body | 最终 HEAD、最新迁移/表数、真实 Provider 状态、`FINAL_USER_REVIEW_PENDING` |

本轮没有新测试数字，不能把旧的 186/207 等数字写成当前 PASS。

## 11. 下一轮文件级实施方案（最多 6 个逻辑提交）

### 提交 1：AI 变式原子性与真实链

- 目标：消除 SUCCESS/PENDING 孤儿链，并复验真实 API。
- 根因：生成事务在实例插入前结束；旧详情 SQL 是候选 500 根因但现场未证实。
- 修改文件：`AiQuestionGenerationService.java`、`StudentAiVariantService.java`、两者测试、必要 DTO/error mapping；仅在状态约束确需扩展时新增连续迁移，禁止改 V1–V24。
- DB/API/UI：候选+实例原子；稳定错误 code；UI 显示精确安全文案。
- 测试：故障 trigger、多题型、1–5、Provider error 分类、换题、PENDING、STANDARD hash。
- 浏览器：完整选择难度→生成→作答→判分→换题→PENDING。
- Provider：在随机库/Demo 安全注入已有配置，最多一次 DeepSeek。
- 完成判据：HTTP 2xx、Parser、实例、判分、PENDING 同时成立；失败不留孤儿。

### 提交 2：登录、消息与答案 UI 闭环

- 目标：把本轮机器样式事实扩展为认证后的交互证据。
- 根因：测试只 mount/stub，消息菜单无 recall/hide 覆盖，答案缺父页面集成。
- 修改文件：`LoginView.spec.ts`、`PasswordRecoveryDialog.spec.ts`、`MessageConversationView.vue/.spec.ts`、`api/messages.spec.ts`、`AnswerDisplay.vue/.spec.ts` 及三个调用页。
- DB/API/UI：不改 DB；验证现有 recall/delete API；统一答案组件。
- 测试/浏览器：桌面/390、dropdown/Popconfirm、双方视角、单/多/填/主观/非法。
- Provider：无。
- 完成判据：0 默认按钮、0 overflow、API 请求与双方状态均有证据。

### 提交 3：专题附件与 Topic AI

- 目标：专题 DTO/页面包含题干与解析附件，并复用统一会话。
- 根因：现有证据只有纯文本专题，附件/权限/AI 浏览器链缺失。
- 修改文件：`TopicLearningDtos/Service/Controller`、`TopicLearningView.vue/.spec.ts`、附件内容服务、Topic/AI 集成测试。
- DB/API/UI：优先复用 V20/V23/现有附件表；若约束不足才新增迁移。
- 测试/浏览器：五类专题、题图/解析图、私有权限、10 轮、不评分、不修改 STANDARD。
- Provider：Tutor 可 Mock 自动化；真实状态单列。
- 完成判据：公开/私有专题附件与 AI 会话均有权限证据。

### 提交 4：教师私有题与知识卡片完整业务链

- 目标：补齐新建、编辑、删除草稿、图片、19 列导入、发布、跨班与知识卡 UI。
- 根因：当前有 Controller/Service/页面和 V20/V21 结构，但缺少专门私有题集成测试与最终浏览器证据。
- 修改文件：`TeacherPrivateQuestion*`、教师页面与 API、`JiaoShiGaoPinKaoDian*`、学生卡片页面、Excel parser 调用及测试。
- DB/API/UI：先证明 V20/V21 足够；199/200/admin/other teacher 全路径过滤。
- 测试/浏览器：CRUD、附件、Preview/Confirm、PUBLISHED、历史快照、卡片六类型、KaTeX。
- Provider：私有 AI 候选仍需人工审核，不要求真实调用。
- 完成判据：所有角色隔离和页面闭环可复验。

### 提交 5：性能、Demo 与安全迁移

- 目标：形成可重放的 V1→latest、Demo、正式库和 10 分钟性能事实。
- 根因：当前 Demo V23 且缺关键验收事实，V24 未执行；旧性能/测试数字不可沿用。
- 修改文件：安全启动/停止与测量脚本、Demo seed/validate（仅经批准的实现轮次）、迁移测试；不得改既有迁移。
- DB/API/UI：随机库先行，Demo 重建；全部通过后仓库外备份正式库并正常 Flyway。
- 测试/浏览器：全量、package、type-check、build、audit、10 分钟采样。
- Provider：各自单列 REAL/MOCK/blocked。
- 完成判据：备份非空+SHA、基线不变、端口释放、无重复进程；无泄漏则写 `NO_REPRODUCIBLE_APPLICATION_LEAK`。

### 提交 6：最终截图、README 与论文同步

- 目标：当前 HEAD 匿名截图矩阵、V24+ 数据库资料和远程导航一致。
- 根因：现有图与文档均早于当前 HEAD，数据库图严重过期。
- 修改文件：`docs/evidence/thesis-final/`、`readme-preview/`、README、数据库参考/snapshot/图、thesis 三件套、DEVELOPMENT_STATUS、PR body。
- DB/API/UI：只消费提交 1–5 的实测结果。
- 测试/浏览器：完整路由矩阵、链接/raw、秘密/绝对路径、`git diff --check`。
- Provider：图片逐张标 Demo/Mock/Real，09/10 类 fixture 不得冒充。
- 完成判据：所有口径指向同一远程 HEAD；`FINAL_USER_REVIEW_PENDING` 保留。

## 12. 阻塞门禁结论

本审计不能标记 COMPLETE，精确阻塞为：

1. `POST /api/v1/student/ai/variants`：Demo `xue_sheng_da_ti=0` 且模型配置 0；建立前置事实会修改 Demo，正式库调用会修改正式数据，均被本轮禁止。
2. 消息菜单/撤回/删除浏览器证据：Demo `si_xin_xiao_xi=0`；创建消息及 recall/delete 都是被禁止的写操作。
3. AnswerDisplay 三种认证页面截图：本轮不能要求用户参与 CAPTCHA，也没有授权绕过认证或植入 fixture；因此未取得。
4. 用户原始 500 现场的数据库版本、异常类和最小堆栈不存在，无法把候选根因升级为已证实。

这些缺口不能由类存在、Mock、旧截图或正式库既有 SUCCESS 记录替代。
