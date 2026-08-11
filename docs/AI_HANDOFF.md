# AI 开发交接

## PR #29 学生 AI 学习主链（2026-08-11）

- 状态：`REAL_PROVIDER_SMOKE_PENDING`。V13 随机临时库、分析/复用/纠正/会话/所有权/注入/降级专项和前端门禁均已覆盖；真实 Provider 结果不得由 Mock 代替，全量最终回归和人工验收仍留 PR #31。
- 基线：PR #28 ordinary merge commit `54c1669b3113086a2fb22e756e0656ea8cb751c8`；分支 `feat/ai-student-learning-core`。
- 数据：V13 新增 `ai_cuo_ti_fen_xi`、`ai_hui_hua`、`ai_xiao_xi`；分析唯一绑定 `xue_sheng_da_ti.id`，会话同时绑定学生、正式答题事实和冻结练习题。V1–V12 不修改。
- 分析：受控 8 类错误、严格 5 字段 JSON、数组/长度上限；`response_format=json_object`、`thinking=disabled`、`max_tokens=1200`。首次无效只允许一次业务纠正，第二次失败不保存成功分析。
- 复用：成功分析按正式答题事实、Prompt 版本 `student-ai-v1` 和受控输入事实 SHA-256 复用；并发请求通过行锁避免重复调用。
- 错题绑定修正：详情中的最近答案仍来自 `cuo_ti_ji_lu.zui_jin_da_ti_id`；独立的 `aiAnalysisAnswerFactId` 则按当前学生、当前题、`shi_fou_zheng_que=0`、对应冻结练习题及 `SUBMITTED` 会话，依提交时间/id 倒序取最近错误正式事实。后续答对进入 REVIEWING 或 MASTERED 不会把 AI 指针切到正确事实。
- 答疑：只围绕当前题；单条用户消息 500 字、助手消息 2000 字、最多 8 轮；Provider 上下文最多最近 12 条消息并受 6000 字预算约束。
- 权限：studentId 只从 JWT 推导；答题事实和 conversationId 都重新反查本人所有权；未提交练习、其他学生、TEACHER、ADMIN 均不能读取或发送学生 AI 私聊。
- 安全：冻结题干、选项、正式学生答案仅放入 user 数据区；STANDARD 正确答案/解析明确不可变；不传姓名、手机号、班级或整份历史；API 不返回 provider/model/token；V12/V13 均不保存 Prompt、输出或 Key。
- 降级：Provider/JSON 失败只返回受控错误，练习结果、错题、判分、掌握度、规则推荐和 STANDARD 解析保持不变。
- 集中修正轮门禁：后端受影响专项 65/65 PASS（其中生命周期组合 24/24 PASS），无 Key smoke 门禁 1/1 assumption skipped；`mvn -DskipTests package` PASS。前端 AI/错题专项 15/15 PASS，`npm run type-check` 与 `npm run build` PASS；既有 500 kB chunk warning 保留。未机械运行完整历史全量、Demo reset/seed/smoke、全站浏览器或人工验收。
- 真实 DeepSeek smoke：`REAL_PROVIDER_SMOKE_PENDING`。当前 Codex 进程检测到 `RIKE_TIKU_AI_API_KEY/ENABLED/PROVIDER/MODEL` 均不存在；聊天中出现过的旧 Key 未使用。新增 `RealDeepSeekSmokeTest`，只在同一 PowerShell 安全设置轮换后的环境变量时执行两个最小真实请求，并验证 V12 脱敏列和 V13 成功分析；无变量时以 assumption 跳过，禁止输出或落盘 Key。
- 后续：PR #30 仅做候选题生成 + PENDING + 人工审核 + 质量评价；PR #31 做真实 Provider 集成、全量测试、最终文档与一次人工验收。

## PR #28 Provider Core（2026-08-11）

- 状态：`DONE_VERIFIED`（Provider 专项、V12 随机临时库迁移与日志专项、AI 关闭上下文、package、`git diff --check`）。
- 基线：PR #27 merge commit `84a82fc3bd4972fc11c0811d8332bae306b7e5c0`；分支 `feat/ai-provider-core`。
- 已有能力：`AiModelProvider` 请求/结果/token/status 契约，确定性 Fake，DeepSeek OpenAI-compatible HTTP Provider，连接/请求超时，最多一次重试，受控错误与 V12 脱敏日志。
- 安全边界：AI 默认关闭；Key 只读 `RIKE_TIKU_AI_API_KEY`；测试使用本地 stub；不保存 Prompt/输出/Key/JWT/密码；不写正式 `rike_tiku`。
- 官方核对：2026-08-11 查询 DeepSeek 官方文档，Base URL 为 `https://api.deepseek.com`，Chat Completions 路径为 `/chat/completions`，当前模型为 `deepseek-v4-flash` / `deepseek-v4-pro`；旧 `deepseek-chat` / `deepseek-reasoner` 已于 2026-07-24 后停用。
- 下一阶段唯一任务：PR #29 学生错因分析 + 当前题目有限多轮答疑。候选题生成属于 PR #30，完整全量与真实 Key 集成属于 PR #31。

> 2026-08-09 V3.0 非 AI 正式完工审计结论为 REJECT。历史审计快照识别出 4 个 A 层硬缺口；其中公共门户 MA-016 已在 `feat/public-portal` 完成并验证，剩余附件真实显示、管理员高风险操作日志、30 道合法样例完整导入发布显示闭环仍阻止开始 AI。完整原始证据见 [V3_NON_AI_COMPLETION_AUDIT.md](V3_NON_AI_COMPLETION_AUDIT.md)。

> 历史接续分支 `feat/question-attachment-rendering` 已通过 PR #26 普通合并；其 V1–V10、26 张业务表口径仅属于当时基线。

> 当前接续分支：`feat/non-ai-final-closure`，从 PR #26 merge commit `b992bffef07465665b371b7b707ca8814ec2d36d` 创建。PR #26 已普通 merge；PR #27 已完成用户最终人工复验，MA-017 至 MA-026 均已关闭。PR #27 是最后一个普通非 AI 工程 PR，已加入 V11 管理员操作日志、MA-020、管理员图片上传、来源权利更新、Golden30 正常导入闭环、菜单整理和多角色切换；Flyway V1–V11、27 张业务表，V1–V10 不得修改。

> PR #27 最终机器准备：Demo360 为物理/化学/生物各 120 道、55 个叶子知识点，另含 Topic18，总题量 378。Round 4 前端为 50/50 文件、174/174 测试，type-check、build、audit 0；build 保留 main chunk 797.43 kB（gzip 254.25 kB）warning。用户已在 CAPTCHA testCode 关闭的 acceptance 环境完成最终人工浏览器复验；机器测试、机器浏览器和用户人工结果已分开记录。

> 第三轮 MA-021 至 MA-025 的机器修正包括：五章 Portal 与三张原创静态 WebP、内联掌握比例、冻结完整答案、Demo360 选择题逐项 STANDARD 解析、Topic18 安全分段以及显式 accepted answers 等价判分。没有新增 Flyway、运行时 AI 或通用表达式求值。

> 第三轮视觉随后被用户人工判定失败并新增 MA-026。Round 4 已用 `RIKE Aqua Future`、四张原创 Aqua WebP、六场景 Portal、Physics pin+scrub、学科环境 transition、Aqua Auth/Workspace、移动端和 reduced-motion 降级完成机器修正；用户最终复验后 MA-026 已关闭，非 AI A 层正式为 `DONE_VERIFIED`。PR #27 是最后一个普通非 AI 工程 PR，AI 仍未开始。

更新时间：2026-08-11

## 当前状态

PR #10 至 PR #22 均已普通 merge；PR #22 merge commit 为 `67b7bd7239e2ac1de3ad8c71b82b6d0a79162d3b`，远程 `feat/personal-center` 已删除。

PR #19 已用 JDK 原生生成的 4 位随机 PNG 图形验证码替换 PR #15 历史滑块并进入 `main`。当前 PR #27 登录页首次渲染即自动获取并显示 challenge，用户名、密码和验证码一次填写、一次提交；失败后刷新验证码。challenge 仍在内存保存两分钟并一次性消费，不新增 Redis、第三方依赖、数据库表或 Flyway。

V7 的学生练习、正式答题、结果和错题聚合模型已进入 `main`。PR #18 新增 V8 高频考点表，PR #20 新增 V9 两张私信表，PR #22 新增只 ALTER `yong_hu` 的 V10；PR #27 当前新增 V11 管理员操作日志表，Flyway 为 V1–V11，共 27 张业务表，MVP30 原始 Excel 未改动。历史 PR #13 自动化为后端 68/68、前端 68/68；PR #15 合并后自动化为后端 79/79、前端 72/72，打包、类型检查、构建、依赖审计与完整浏览器验收均已通过。

`rike_tiku_demo` 显式重建工具保留 199/200 双班级场景：14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系和 12 条 ACTIVE 高频考点。PR #27 的当前验收目标为确定性 Demo360：物理、化学、生物各 120 道、55 个叶子知识点，并额外提供每科 6 道 `TOPIC_LEARNING` 综合题，总题量 378。演示数据不使用 Flyway 承载，不公开 seed 接口，也不在正常启动时执行。

Demo90 与 PR #23 筛选变式均是项目原创演示内容。V3.0 没有规定名为 MVP30 的 Excel 必须整体正式入库；MVP30 原始文件保持不变，定位为 PR #12 已验证的结构化导入能力素材。开发阶段由 Codex 辅助制作候选不等于系统实现运行时 AI 出题。

历史 PR #14 合并后验证：后端 74/74、打包 PASS；前端 68/68、类型检查与构建 PASS；依赖审计 0 vulnerabilities。完整脚本链及三角色真实 HTTP smoke PASS，正式库未出现演示账号、演示题或学习记录。

历史 PR #18 合并后后端 87/87、前端 83/83，package、type-check、build、audit 和 Demo `reset → seed → validate → smoke` 均通过；当时真实浏览器工作台/高频考点隔离验收通过，MA-009 尚未完成。

PR #19 合并后回归为后端 90/90、前端 91/91，package、type-check、build PASS，生产依赖 audit 为 0。Demo `reset → seed → validate → smoke` PASS；smoke 只在显式 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=true` 的本地演示后端读取测试值，正式默认关闭且没有免验证码登录入口。真实浏览器验收保持通过；正式库只读复查 Demo90、场景账号、场景班级和高频考点仍均为 0。

PR #20 已普通 merge 并删除远程功能分支。V9 新增两张私信表，V1–V8 未修改；师生私信由 ACTIVE 三元任课关系和学生当前主班级共同约束。合并后后端 92/92、前端 100/100，package、type-check、build、audit 0 和 Demo `reset → seed → validate → smoke` 均通过；199 学生↔物理教师、200 学生↔化学教师双向浏览器验收及伪造会话隔离保持通过，MA-009 已关闭。

PR #21 已普通 merge 进入 `main`，实时复用 V7 答题事实和冻结知识点快照计算 NOT_STARTED、INSUFFICIENT、WEAK、IMPROVING、MASTERED。当前学科全部 ACTIVE 知识点参与掌握度和总体统计；5 题推荐资格独立复用 StudentPracticeService 的真实题池规则，题量不足不会隐藏历史掌握事实，也不会生成无法创建的推荐。学生端“开始巩固”复用现有练习创建；教师端只按本人 ACTIVE scope 查看班内当前学科汇总。该能力是确定性规则统计，不是 AI；未新增 V10、缓存或统计表。MA-014、MA-015 已关闭。

合并后学习掌握专项 6/6、后端 98/98、前端 29 文件 106/106，package、type-check、build、生产依赖 audit 0 及 Demo `reset → seed → validate → smoke` 均通过。正式 `rike_tiku` 只读检查为 Flyway V9、26 张业务表，Demo90、场景账号、场景班级、高频考点、私信会话、私信消息与 V7 学习记录均为 0。轻量浏览器复查确认学生掌握页、5 题推荐预选、教师班级学情正常，控制台 0 error；原 15 题浏览器证据继续作为历史验收记录。

MA-013 的旧测试数据源问题已关闭。合并前使用全新随机临时库验证：临时库与正式库 V9 script 一致，checksum 均为 `1192958817`、success 均为 1；正式库两张表为空。PR #20 合并后，正式库提前执行的 V9 已与 main 正式迁移基线一致，两张 V9 结构表不属于业务数据污染；Demo90、场景账号、场景班级、高频考点、会话和消息仍均为 0。

PR #22 已将统一 `/profile` 与 `GET/PUT /api/v1/profile`、`POST/DELETE /api/v1/profile/avatar` 普通 merge 进入 `main`。学生、教师、管理员和 ADMIN+TEACHER 多角色账号均只读取本人真实资料；简介上限 500 字。头像只接受不超过 2 MB、可由 ImageIO 真实解析且 MIME/文件内容一致的 PNG/JPEG，MySQL 保存二进制，前端显示 data URL。业务档案、角色、用户名和组织关系不可在个人中心修改，主动改密继续复用现有接口；首次登录门禁不豁免 profile，必须先完成初始密码修改。

PR #22 合并后验证为后端 102/102、前端 31 文件 117/117，package、type-check、build、audit 0、Demo reset/seed/validate/smoke 均 PASS。真实浏览器完整证据覆盖三角色与 `demo_physics_admin` 多角色资料、简介、头像上传/刷新持久化/删除、现有改密入口；门禁修正后轻量抽查正常，控制台 0 error。MA-006 已关闭。正式库为 V10、26 张业务表，Demo90、场景账号、场景班级、高频考点、私信及 V7 学习记录均为 0。

PR #23 已普通 merge。合并后后端 105/105、前端 31 文件 117/117，package、type-check、build、生产依赖 audit 0 及 Demo `reset → seed → validate → smoke` 均 PASS；合并前专项 27/27 PASS。正式库只读检查为 V10、26 张业务表，演示题与全部场景业务数据为 0；MVP30 原始文件 SHA-256 保持 `01E90ACFDFB8EF5194103C3B7DD1A99B4F351858FFFDF70CFF63187928DCAB17`。

PR #23 浏览器仅操作 `rike_tiku_demo`：物理 3、化学 3、生物 4 道新变式真实进入随机练习；Unicode 化学式、三题型控件、提交前防泄露、结果答案与 STANDARD 解析、错题和掌握度均正常。同科连续随机题集不同，控制台 0 error；最终 reset/seed/validate 已清理验收会话并恢复固定 120 题状态。

`feat/public-portal` 将根路径 `/` 从重定向登录改为无需认证的正式公共门户，复用唯一 `/login` 入口且不改认证协议或守卫。门户包含系统名称、副标题、三科介绍、单选/多选/填空题型边界、非 AI 功能介绍、六步学习闭环和三角色说明；首屏及能力区均明确运行时 AI 尚未上线。前端 32 文件 122/122、type-check、build、audit 0 通过；后端回归及 package 为 105/105。独立 Demo 的 reset/seed/validate/smoke、三角色真实登录、登录态/未登录态门户刷新和常见宽度复验均通过；控制台 0 error、0 warning，最终 Demo 已恢复固定 120 题。MA-016 已关闭，MA-017 至 MA-020 未改动。

PR #26 的 MA-017 机器证据为：后端附件/权限/HTTP/导入/题池专项 27 个，26 PASS、1 个符号链接权限 assumption skipped；`mvn clean test` 112 个测试 0 失败、1 个符号链接权限 assumption skipped，`mvn clean package` PASS；前端附件专项 4/4、`npm test` 127/127 PASS，type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。当时 Demo 题库为物理 40、化学 39、生物 41，共 120 道；当前验收库已由 PR #27 扩充为 Demo360。PHYSICS-S1 两条附件记录实际文件/hash 回读继续通过。状态仍为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。

PR #27 当前机器收口已完成主要实现：V11 管理员高风险操作日志及 ADMIN 查询页面、空提交体 4xx、草稿题干/标准解析图片上传/替换/删除、来源权利补充 API、Golden30 的真实 preview → confirm → 权利补充 → 审核 → 发布 → 查询 → 学生练习闭环、题库/学生菜单整理和多角色“切换身份”均已进入当前分支。Golden30 独立测试验证物理 10、化学 10、生物 10，共 30 道已发布题，其中 29 道固定答案题可进入自动练习，1 道主观题按现有设计保留为专题学习题；原始候选 Excel 未修改，正式库未写入。第三轮五项机器修正继续保留：内联掌握比例、历史冻结完整选项展示与 Demo360 246 道选择题逐项解析、Topic18 全部结构化解析及安全排版、BV-06 显式 accepted answers 等价判分。Round 3 视觉人工验收失败后，Round 4 又以 `RIKE Aqua Future` 重建设计系统、Portal/Auth 和三角色工作台，MA-026 已完成机器修正。当前前端门禁为 50/50 文件、174/174 测试、type-check/build/audit 通过，main chunk 797.43 kB（gzip 254.25 kB）warning 保留；本轮未修改后端，故未重跑 Maven，第三轮后端 133/0/0/1 skipped 与 package PASS 作为最近执行事实保留。Demo360 + Topic18 最近一次 `acceptance-prepare → validate → smoke` PASS。Round 3 与 Round 4 机器证据分别位于 `docs/evidence/pr27-ui-round3/`、`docs/evidence/pr27-ui-round4/`，均不替代用户验收。PR #27 保持 Draft，MA-017 及 MA-021 至 MA-026 仍等待用户最终复验；AI 未开始。

第二轮视觉反馈在同一 PR 内继续收敛：Portal 由长叙事减为四段事实入口，删除 AI 规划和解释设计的文案；三科视觉改为原创语义 SVG；学生与教师具体工作页统一由 `subjectCode` 解析学科环境，教师 workspace DTO 因此最小补充 `subjectCode`；管理员保持 neutral。科学排版只接受显式 `\\(...\\)` / `\\[...\\]`，通过 KaTeX DOM API 安全输出，普通 `/` 与旧题纯文本不自动猜测；Topic18 代表题已渐进迁移。该轮证据位于 `docs/evidence/pr27-ui-round2/`，仍须用户最终复验。

## 继续时必须保持

- PR #26 修正后的附件证据以本轮结果为准：附件/权限/导入/题池专项共 27 个测试，26 PASS、1 个符号链接权限 assumption skipped；`mvn clean test` 112 个测试 0 失败、1 个符号链接权限 assumption skipped，`mvn clean package` PASS；前端附件专项 4/4、`npm test` 33 文件 127/127 PASS，type-check、build、`npm audit --omit=dev` 为 0 vulnerabilities；Demo `reset → seed → validate → smoke` PASS，物理 40、化学 39、生物 41，共 120 道。当前仍为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`，人工验收延期至非 AI 最终集成验收。
- 附件对象标识保持兼容规范：正文是 `〔图片对象 I001〕` / `〔公式对象 F107〕`，数据库 `dui_xiang_biao_shi` 只存 `I001` / `F107`；任何新导入、Demo seed、后端权限判断和前端渲染都必须按提取后的对象 ID 匹配。
- 仅 `STUDENT` 且有有效 `xue_sheng_dang_an` 可访问学生练习资源；会话、结果和错题均以当前学生档案隔离。
- 题池只取可真正冻结的 `PUBLISHED + ONLINE_PRACTICE + shi_fou_ke_zi_dong_pan_fen=1` 单选、多选、填空：要求有效版本 1 STANDARD 解析、活动知识点、足够选项和合法答案 JSON。仅真实存在、类型与 SHA-256 均校验成功的 PNG/JPEG STEM/OPTION/STANDARD 图片可进入；PDF、公式、ANSWER 附件、缺失或不安全附件继续排除。
- 未提交会话 API 绝不返回正确答案或标准解析；答题内容和结果不存浏览器持久化存储。
- 提交事务必须同步保存答题事实、错题聚合、结果和会话 `SUBMITTED`，重复提交返回 `409`。
- 错题错误次数永久保留；答对只更新连续正确次数和复习状态。
- Entity 只对应数据库，不直接暴露密码摘要、逻辑删除等内部字段；API 输入优先 `XxxRequest`，输出优先 `XxxResponse`，可统一放入 `dto` 包。
- 不机械创建 VO、Converter、Assembler；只有真正独立的页面展示模型才考虑 VO。
- 不把规则掌握度和推荐描述为 AI；不引入 AI Provider、AI 判分、教师任务、WebSocket、Redis 或超出 MA-017 范围的附件能力。
- 图形验证码测试值只允许在自动化或独立本地 Demo 中显式开启；正式运行保持 `RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=false`。

## 重要文档

- [学生练习 API](STUDENT_PRACTICE_API.md)
- [学生练习前端](STUDENT_PRACTICE_FRONTEND.md)
- [数据库模型](QUESTION_DATABASE_MODEL_V1.md)
- [开发状态](DEVELOPMENT_STATUS.md)
- [管理员学生管理 API](ADMIN_STUDENT_MANAGEMENT_API.md)
- [管理员学生管理前端](ADMIN_STUDENT_MANAGEMENT_FRONTEND.md)
- [师生私信 API](TEACHER_STUDENT_MESSAGING_API.md)
- [师生私信前端](TEACHER_STUDENT_MESSAGING_FRONTEND.md)
- [知识点掌握度与规则推荐](LEARNING_MASTERY_RULE_RECOMMENDATION.md)
- [个人中心 API](PERSONAL_CENTER_API.md)
- [个人中心前端](PERSONAL_CENTER_FRONTEND.md)
- [最终演示题库](FINAL_DEMO_QUESTION_BANK.md)
- [变式候选审核记录](DEMO_VARIANT_QUESTION_REVIEW.md)
- [Round 4 设计规范](../DESIGN.md)
- [Round 4 UI/UX Foundation](UI_UX_FOUNDATION.md)

## 当前下一步

PR #27 用户人工复验已通过，封板动作是 ordinary merge；合并后同步最新 `main`，删除 `feat/non-ai-final-closure`，再创建干净的 `feat/ai-provider-core`。下一轮只实施 Fake/Test Provider、配置模型、超时、一次有限重试、降级和基础调用日志；不索取 API Key，不创建 PR #28，不在本轮修改 AI 代码。
