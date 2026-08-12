# 开发状态

> 2026-08-12 PR #30 集中修正：`DONE_VERIFIED`（受影响专项和必要构建）。候选 Provider/Parser/prepare 保持在事务外，候选题创建、母题关联、DRAFT→PENDING、质量评价行与任务 SUCCESS 改为同一 `TransactionTemplate` 批次事务；确定性第二候选写入故障验证任务保留 FAILED、生成数 0、质量行 0、关联/PENDING 候选 0、母题与 STANDARD 不变。教师工作台新增 `/teacher/ai-generation` 真实入口，共享管理员候选工作区主体，但仅调用 `/teacher/ai-generation/**`，只读取本人 ACTIVE 任教学科母题、知识点与任务，不请求全局 stats 或模型配置。集中修正后端专项 38/38、前端专项 32/32，package/type-check/build PASS；Flyway 仍为 V1–V14，无 V15。真实 GLM 状态仍为 `REAL_GLM_VISION_SMOKE_FAIL_429`，本轮未重复调用。

> 2026-08-11 PR #30 AI 候选题/视觉主链：`DONE_VERIFIED`（受影响专项和必要构建）。V14 新增管理员 AI 模型配置、生成任务、候选题质量评价、视觉上下文缓存四表；数据库启用配置优先、环境配置回退。DeepSeek 负责文本推理，GLM `glm-4.6v-flash` 只生成受控 `UNTRUSTED_VISION_CONTEXT`。候选一次 1–3 道、同母题 AI_GENERATED PENDING 最多 6 道，request hash/内容 hash/Jaccard 三层重复控制，结果只进入 PENDING，五项人工评价通过后才能 PUBLISHED。后端专项 54/54、前端专项 12/12、package/type-check/build PASS。真实 GLM 单图 smoke 到达官方接口，但有限重试后仍为 HTTP 429，状态为 `REAL_GLM_VISION_SMOKE_FAIL_429`；最终真实全链路、全量回归和人工验收留 PR #31。

> 2026-08-11 PR #29 学生 AI 学习主链：`DONE_VERIFIED`。Mock/随机临时 MySQL 专项覆盖分析、复用、一次 JSON correction、当前题最多 8 轮答疑、严格所有权、Prompt 数据隔离和 STANDARD 降级；集中修正轮保证 REVIEWING/MASTERED 后 AI 仍查询最近一次错误正式答题事实。真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped：HTTP 2xx，文本 848 ms（22/11 token），结构化分析 1670 ms（400/132 token）、1 次 JSON 调用，Parser 与 V12 日志脱敏 PASS，V13 SUCCESS 且 STANDARD 未改变。Flyway 保持 V1–V13、31 张业务表；V1–V13 未修改。候选题生成仍为 `NOT_STARTED`。

> 2026-08-11 PR #28 `AiModelProvider Core`：`DONE_VERIFIED`（Provider 专项与 package 门禁）。已实现统一 Provider 契约、确定性 Fake、DeepSeek OpenAI-compatible HTTP Provider、连接/请求超时、最多一次临时失败重试、有限错误分类、受控降级和 V12 脱敏调用日志。AI 默认关闭，自动化不需要真实 Key。学生错因分析、多轮答疑、候选题生成和前端 AI 页面仍为 `NOT_STARTED`；完整全量回归留到 PR #31。

> 2026-08-09 V3.0 非 AI 正式完工审计已由 PR #24 普通 merge（merge commit `bcfb2181af2197d2524a2df8ca64895e435a4857`）进入 `main`。结论为 REJECT，当前不能认证 100% DONE_VERIFIED。正式证据见 [V3_NON_AI_COMPLETION_AUDIT.md](V3_NON_AI_COMPLETION_AUDIT.md)。

> PR #25 已普通 merge（merge commit `0559a4e4eba041dd74a7bcb7d4c9f2cd8b29e617`）并关闭 MA-016。其后历史接续分支 `feat/question-attachment-rendering` 已通过 PR #26 合并；V1–V10、26 张业务表是当时口径。

> PR #26 已普通 merge（merge commit `b992bffef07465665b371b7b707ca8814ec2d36d`）。当前接续分支为 `feat/non-ai-final-closure`，PR #27 为最后一个非 AI Draft PR。当前分支已加入 V11 管理员操作日志、MA-020 空 body 4xx、管理员题目图片上传、来源权利补充 API、Golden30 正常导入闭环、菜单整理和多角色切换；Flyway 为 V1–V11、27 张业务表；V1–V10 未修改。

> Round 3 视觉人工验收已明确失败并新增 MA-026。Round 4 以 `RIKE Aqua Future` 完成机器修正，用户已完成最终人工复验；MA-017 至 MA-026 均已关闭，非 AI A 层正式为 `DONE_VERIFIED`。PR #27 是最后一个普通非 AI 工程 PR，AI 阶段从其合并后的最新 `main` 开始。

更新时间：2026-08-11（PR #30 AI 候选题/视觉主链）

## 当前主线状态

- 当前分支：`feat/ai-question-generation-review`；基线为 PR #29 ordinary merge commit `d04e5dcf9639182303e26e38ccfa4351ad91c5d9`。
- Flyway：V1–V14，共 35 张业务表；V14 新增四张 AI 配置/生成/评价/视觉缓存表，V1–V13 未修改。
- Provider Core：`DONE_VERIFIED`。学生 AI 主链：`DONE_VERIFIED`。候选题生成、PENDING 审核、质量评价与视觉桥接：`DONE_VERIFIED`。真实 GLM smoke：`FAIL_429`。

- 当前分支：`feat/non-ai-final-closure`；PR #26 已合并，公共门户和附件机器实现已进入 `main`；PR #27 已完成最终人工验收，准备 ordinary merge 封板。
- 根路径 `/` 是无需认证的公共门户。Round 4 以 Hero、Physics、Chemistry、Biology、Learning Loop、Entrance 六场景组织真实产品内容，保留 3 / 360 / 18 与统一 `/login`，不写 AI 宣传或虚构指标；登录、角色选择、首次改密和受保护路由守卫的业务行为未改动。MA-016 已关闭。
- V3.0 未指定名为 MVP30 的 Excel 必须整体正式入库；它要求 30 题 MVP 验证导入、审核、发布、查询和附件显示闭环，并强调少量高质量题目。MVP30 因此保留为结构化导入能力验证素材，原始文件不修改。
- 三角色共用 `/profile`，本人身份从 JWT 推导；页面展示真实账号角色及学生/教师档案，只允许维护 500 字简介和本人头像，并复用现有主动修改密码流程。
- 登录页当前使用两分钟有效、内存保存、一次性消费的 4 位随机图形验证码；登录页首次渲染即加载验证码，用户名、密码、验证码一次填写并一次提交，失败后自动刷新 challenge。PR #15 滑块和 PR #19 的二阶段交互仅为历史实现。
- 管理员单学生管理已实现分页筛选、详情与班级历史、事务新增、编辑与启停、事务调班和一次性密码重置；Excel 批量导入入口继续独立保留。
- 学生自主练习、自动判分、结果、错题闭环、实时知识点掌握度、固定规则推荐和教师班级学情查看均已进入 `main`。掌握度与推荐是确定性规则统计，不属于 AI；AI、教师任务、组卷考试和主观题评分仍未实现。
- MA-017 当前已完成机器实现：图片附件仅接受受控路径下真实 PNG/JPEG，3MB 内、扩展名/MIME 一致并按 SHA-256 回读；管理员题目详情和学生练习题面、结果、错题通过带 JWT 的 Blob 请求显示，创建中会话拒绝 STANDARD_ANALYSIS，缺失/损坏/403/404 显示占位。正文继续使用完整 marker（如 `〔图片对象 I001〕`），`ti_mu_fu_jian.dui_xiang_biao_shi` 统一只保存对象 ID（如 `I001`/`F107`），前后端解析正文后按对象 ID 匹配。当前状态为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。
- MA-017 本轮机器门禁：附件/权限/导入/题池专项共 27 个测试，26 PASS、1 个符号链接权限 assumption skipped；`mvn clean test` 112 个测试 0 失败、1 个符号链接权限 assumption skipped，`mvn clean package` PASS；前端附件专项 4/4、`npm test` 33 文件 127/127 PASS，type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。新增真实 QuestionImportService 导入链覆盖 preview → confirm → 受控 storage → 管理员 detail/content → 学生题池及提交前后权限。Demo `reset → seed → validate → smoke` PASS；Demo 业务题 120 道（物理 40、化学 39、生物 41），PHYSICS-S1 的两个附件记录均为 I001/I002 并指向可回读且 hash 正确的 PNG。按项目验收策略，用户 CAPTCHA 和浏览器视觉验收延期至非 AI 最终集成验收，不属于 PR #26 的 merge gate。
- MA-018 机器实现已完成：新增 V11 `guan_li_cao_zuo_ri_zhi` 管理员高风险操作日志表、真实操作者/模块/动作/业务对象/成功失败/脱敏摘要/错误码和 ADMIN 查询 API/页面；学生、教师、班级、任课、题目审核发布、题目导入确认等现有管理员写操作已接入记录。
- MA-020 已机器修复：练习提交空 body 和不可解析 body 均由全局异常处理返回 `400 INVALID_REQUEST`，正常提交/判分回归不变。
- MA-019 已加入独立 `Golden30ImportIntegrationTest`：复用现有三科清洗候选素材，在独立测试数据库真实走 preview → confirm → 来源权利补充 → 审核发布 → 查询 → 题池 → 学生提交/标准解析；物理 10、化学 10、生物 10，29 道固定答案题进入自动练习，1 道主观题保留为专题学习题。原始 MVP30 文件未修改，正式库未写入。
- 管理员题目页已支持草稿题干/标准解析 PNG/JPEG 上传、预览、替换和删除，复用 PR #26 受控存储；正式菜单已将“批量导入题目”并入题库入口、将学生 Excel 导入并入学生管理，并为多角色账号增加“切换身份”。
- PR #27 当前机器专项：MA-020、管理员图片上传、Golden30、导入回归、日志权限、管理员 Dashboard、教师密码重置、练习 availability、错题 `subjectCode` 隔离、规则型类似练习、Topic18 与 Demo 分布均通过。用户最终人工验收已通过，MA-017 至 MA-026 均已关闭。
- PR #27 第三轮综合机器门禁（历史执行结果）：后端 `mvn clean test` 为 133 个测试，0 failure、0 error、1 个 symbolic-link assumption skipped，`mvn clean package` PASS；前端当时为 49 个文件、170/170 PASS，type-check、build PASS，audit 0；Demo `acceptance-prepare → validate → smoke` PASS，Golden30 独立导入测试 PASS。Demo360 固定为物理/化学/生物各 120 道，叶子知识点 18/16/21；每科题型分布 44/38/38、难度分布 36/48/36；Topic18 每科 6 道，总题量 378。题干附件内容 hash、文件/数据库事务回滚、草稿文本保护、SUCCESS 审计事务和附件外键稳定性修正继续保持。
- 第三轮最终人工验收修正 MA-021 至 MA-025 已关闭：Portal 使用 Hero、物理、化学、生物、学习闭环与登录六场景和四张原创 Aqua WebP；学生/教师继续由真实 `subjectCode` 进入三科环境，管理员保持中性；学习掌握显示内联 `0 / 38`；结果与错题使用冻结选项快照显示完整答案；Demo360 的 246 道选择题解析含结论、全部选项逐项判断、题目依据和易错点；Topic18 全部使用多段结构并由 `StandardAnalysis` 安全渲染；BV-06 仅通过显式 accepted answers 接受 `1/2`、`0.5`、`50%`、`50％`。未增加迁移、求值器或运行时 AI。
- Round 3 production-like 机器浏览器完成 Portal 1280/390、三科 Portal 章节、三科学生环境、教师三科 scope、管理员中性 Dashboard、`0 / 38`、单选/多选冻结完整答案及逐项解析、三科 Topic 和生物 `50%` 正确判分；390px 无横向溢出，最终完整复跑 console/page error/HTTP 4xx+ 计数为 0。截图与抽样见 `docs/evidence/pr27-ui-round3/`。机器证据不等于用户人工 CAPTCHA 或视觉验收。
- Round 3 随后被用户明确判定视觉人工验收失败：普通极简 SaaS、无效留白、三科学科环境不足、Frutiger Aero 材质缺失以及缺少连续滚动叙事均未达要求。历史机器截图继续保留，不改写为人工 PASS。
- Round 4 MA-026 已关闭：`RIKE Aqua Future` 重建设计 token、材料、八级排版、共享 Aqua shell、Portal/Auth 和 Student/Teacher/Admin 视觉边界；四张原创 Aqua WebP 进入构建，Portal 使用 Hero depth、导航 material morph、Physics 2300px pin+scrub、学科环境 transition、学习闭环进度与细指针光学反馈；低于 64rem、390px 与 reduced motion 使用完整静态/自然纵向降级。用户已完成最终人工复验。
- 最终人工环境使用 `acceptance-backend` + `acceptance-frontend`：后端连接 `rike_tiku_demo:18081` 且 challenge JSON 不含 `testCode`；前端先 build 再以 strict-port Vite preview 运行于 18080。先前动态模块失败的真实根因是旧浏览器页面仍引用已停止的 Vite dev server（`ERR_CONNECTION_REFUSED`），不是 lazy import 源码错误。
- 用户最终人工验收反馈已在同一 PR #27 修正：管理员总览使用真实只读指标；管理员路由标题按 meta 变化；列表筛选改为响应式 grid；管理员可安全重置教师密码；练习显示中文题型/难度规则与实时可用题数；结果按单题导航；知识点可进入定向练习；错题不再假设学科固定 ID，而是按 `subjectCode` 实时过滤；高频考点选择器使用可空值与完整路径；重复错误 Toast 由单一路径处理。用户仍需在最终环境复验这些页面，未记为人工 PASS。
- 第二次人工视觉反馈同样未隐藏：Portal 已删除学习流程、角色和 AI 规划等自我解释区块，只保留品牌、三科、真实数据快照和登录；三科使用有语义的原创 SVG；学生页面与教师任课范围通过稳定 `subjectCode` 进入 physics/chemistry/biology 环境，管理员继续保持中性；新增 `ScientificText`/`MetricFraction`，仅解析显式 TeX 片段并通过受控 KaTeX DOM renderer 输出 HTML+MathML，普通旧文本和 Excel 导入保持兼容。该轮仍等待用户复验，不得写为 `DONE_VERIFIED`。
- Flyway：V1–V11，共 27 张业务表；V11 只新增管理员操作日志表，V1–V10 未修改。
- 教师工作台已支持按本人 ACTIVE 三元任课关系读取班级、科目、学生名单和高频考点，并支持新增、编辑、启停及排序；学生端按本人有效主班级和学科只读取对应 ACTIVE 高频考点。
- PR #20 已实现受 ACTIVE 三元任课关系和学生当前主班级约束的师生纯文本私信；发送身份取自 JWT，支持会话、未读、已读、7 秒轮询和失效关系历史保留，不含 WebSocket、附件、群聊或管理员审计。
- PR #21 掌握度只统计本人已提交、已判分的单选/多选/填空答题，实时关联冻结知识点快照；当前学科全部 ACTIVE 知识点参与掌握度和总体统计，不能因当前题量不足而丢失历史事实。NEW/REVIEWING 错题阻止“已掌握”，MASTERED 错题不阻止。5 题推荐单独复用现有练习题池资格，按活动错题、低正确率、巩固中、样本不足、未练习依次排序，最多 3 项，不属于 AI。

## 已进入 main

- `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`、`cuo_ti_ji_lu`，全部使用 BIGINT 主键、外键 `ON DELETE RESTRICT`、必要唯一键/索引和状态约束。
- 学生只能从 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题创建会话；会话创建后将题目、选项、答案、解析、知识点及顺序冻结。
- 提交整场答案在一个事务中完成锁定、答题事实、错题聚合、最终结果和会话状态切换；重复提交为 `409`。
- 单选严格匹配有效标识；多选以去重后的集合完全相等判分；填空按冻结空位、可接受答案、大小写及受控全半角/标点规则判分。
- 错误创建或累加错题；答对不删除历史，连续正确一次为 `REVIEWING`、两次为 `MASTERED`。
- 学生前端提供 `/student/practice`、`/student/practice/new`、`/student/practice/:id`、`/student/practice/:id/result`、`/student/wrong-questions`；未提交响应和页面均不显示答案或解析。

## 当前验证

- Round 4 当前前端门禁：`npm test -- --run` 为 50/50 文件、174/174 测试 PASS；`npm run type-check` PASS；`npm run build` PASS；`npm audit --omit=dev` 为 0 vulnerabilities。Vite main chunk 为 797.43 kB（gzip 254.25 kB），超过 500 kB 的 warning 如实保留。Round 4 没有修改后端 Java、API contract、Flyway 或数据库，因此未机械重跑 `mvn clean test` / `mvn clean package`；最近一次真实后端执行仍是第三轮的 133 tests、0 failure、0 error、1 symbolic-link assumption skipped 和 package PASS。
- 公共门户本轮验证：前端 32 文件、122/122 PASS；type-check、build PASS，生产依赖 audit 为 0 vulnerabilities。后端未改动，`mvn clean test` 与 `mvn clean package` 均为 105/105 PASS，可执行 JAR 已生成。
- 公共门户浏览器验证仅使用 `rike_tiku_demo`：清除登录状态后 `/` 展示完整门户，统一登录 CTA 进入原 `/login`；`demo_student`、`demo_teacher`、`demo_admin` 均登录正常；登录态返回和刷新 `/` 保持门户。320/375/414/768/1280 宽度无横向溢出，控制台 0 error、0 warning，无未处理 500。Demo `reset → seed → validate → smoke` PASS，末次 `reset → seed → validate` 恢复 120 题固定状态，18080/18081 端口已清理。
- PR #23 合并后自动化：`mvn clean test` 105/105 PASS；`mvn clean package` PASS。前端 `npm test` 31 文件、117/117 PASS，type-check、build PASS，生产依赖 audit 为 0 vulnerabilities。合并前 Demo 题库/学生题池/数据库模型专项 27/27 PASS。
- PR #23 Demo `reset → seed → validate → smoke` PASS，固定题量 120；正式 `rike_tiku` 只读检查为 Flyway V10、26 张业务表，演示题、场景账号、场景班级、高频考点、私信和 V7 学习记录均为 0。MVP30 SHA-256 仍为 `01E90ACFDFB8EF5194103C3B7DD1A99B4F351858FFFDF70CFF63187928DCAB17`。
- PR #23 浏览器 PASS（仅 `rike_tiku_demo`）：随机抽查物理 3、化学 3、生物 4 道新变式；单选、多选、填空、提交前防泄露、提交后答案与 STANDARD 解析、错题和掌握度均正常。同科连续随机题集发生变化，控制台 0 error；验收数据已由最终 `reset → seed → validate` 清理。
- PR #22 合并后后端：首次改密门禁与个人中心专项 25/25 PASS；`mvn clean test` 102/102 PASS；`mvn clean package` PASS。
- PR #22 合并后前端：`npm test` 31 文件、117/117 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #22 合并后 Demo：`reset → seed → validate → smoke` PASS；完整三角色和多角色真实浏览器个人中心验收保持 PASS，门禁修正后轻量抽查正常、控制台 0 error。
- 正式 `rike_tiku` 已按正常 Flyway 流程升级 V10，只读检查为 26 张业务表；Demo90、场景账号、场景班级、高频考点、私信会话、私信消息、V7 五张学习记录表和演示简介/头像均为 0。

- PR #21 合并后后端：学习掌握专项 6/6 PASS（包含掌握度统计资格与 5 题推荐资格分离场景）；`mvn clean test` 98/98 PASS；`mvn clean package` PASS。
- PR #21 合并后前端：`npm test` 29 文件、106/106 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #21 合并后 Demo：`reset → seed → validate → smoke` PASS；正式 `rike_tiku` 只读检查为 Flyway V9、26 张业务表，Demo90、场景账号、场景班级、高频考点、私信会话、私信消息及 V7 五张学习记录表均为 0；敏感信息扫描 PASS。
- PR #21 浏览器 PASS（仅 `rike_tiku_demo`）：199 学生从零记录真实完成 15 道物理题，形成牛顿运动定律 0% 薄弱、电场强度 60% 巩固中、温度和内能 100% 已掌握；推荐预选学科、知识点和 5 题正确。199 物理教师看到该生 15 题、53.3%、薄弱 1、已掌握 1，200 班无混入；生物/化学教师仅见本人两班对应学科；控制台 0 error。验收记录随后由 reset/seed 清理。
- 独立审查修正后轻量浏览器抽查 PASS：学生学习掌握页正常展示全部 ACTIVE 知识点，推荐按钮仍正确预选学科、知识点和 5 题；教师班级学情正常，控制台 0 error。无需重新制造 15 题数据。

- PR #20 合并后后端：`mvn clean test` 92/92 PASS；`mvn clean package` 92/92 PASS 并生成可执行 JAR；私信、权限与 V9 数据库专项 PASS。
- PR #20 合并后前端：`npm test` 25 文件、100/100 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #20 浏览器 PASS（仅 `rike_tiku_demo`）：199 学生↔物理教师、200 学生↔化学教师双向收发，未读、教师工作台范围、伪造 conversationId 拒绝和控制台 0 error 均通过。
- PR #19 合并后后端：`mvn clean test` 90/90 PASS；`mvn clean package` 90/90 PASS 并生成可执行 JAR。
- PR #19 合并后前端：`npm test` 91/91 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #19 浏览器验收保持 PASS（仅 `rike_tiku_demo`）：验证码默认隐藏、错误后中文提示并自动换图、图片/文字刷新、三个单角色直达、多角色选择、退出重登均通过；控制台 0 error。
- PR #18 合并后后端：`mvn clean test` 87/87 PASS；`mvn clean package` 87/87 PASS，并成功生成可执行 JAR。
- PR #18 合并后前端：`npm test` 83/83 PASS；`npm run type-check` PASS；`npm run build` PASS（保留既有大 chunk 提示）；`npm audit --omit=dev` 为 0 vulnerabilities。
- `rike_tiku_demo` 当前 seed 目标：V1–V11、27 张业务表，固定状态为 14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 任课关系、12 条 ACTIVE 高频考点、55 个叶子知识点和 Demo360（物理/化学/生物各 120 道）；固定 seed 不预置练习、错题、私信、操作日志、简介或头像。
- PR #19 历史阶段 Demo `reset → seed → validate → smoke` PASS；当时正式 `rike_tiku` 的 Demo90、场景账号、场景班级、高频考点均为 0。当前正式库基线已为 Flyway V9。
- PR #18 真实浏览器 PASS（仅 `rike_tiku_demo`）：物理教师进入 199/200 工作台，查看 5/3 名学生，新增、编辑、停用、启用高频考点；生物教师仅见 199/200 生物，化学教师仅见 199/200 化学；199/200 学生分别只读取本班物理 ACTIVE 考点；控制台 error 日志为空。临时验收考点已由 reset/seed 清理。
- 合并前门禁使用全新随机临时库完整迁移 V1–V9；临时库与正式 `rike_tiku` 的 V9 script 均为 `V9__create_teacher_student_message_tables.sql`、checksum 均为 `1192958817`、success 均为 1。MA-013 导致正式库提前执行的 V9 现已与 main 正式基线一致，两张结构表不再描述为业务数据污染；测试隔离修复保持有效，MA-013 已关闭。
- 正式 `rike_tiku` 合并后只读检查：Flyway V9、26 张业务表；Demo90、场景账号、场景班级、高频考点、私信会话、私信消息均为 0。
- MA-001 至 MA-015 已关闭；其中 MA-006 在 PR #22 三角色与多角色真实浏览器验收后关闭。
- 历史结果：PR #13 为后端 68/68、前端 68/68；PR #14 为后端 74/74、前端 68/68；PR #15 为后端 79/79、前端 72/72；PR #16 为后端 80/80；PR #17 为后端 86/86、前端 80/80。

## 已进入 main 的演示验收环境

- 显式 PowerShell 工具创建、重置、播种、校验和清理独立 `rike_tiku_demo`，正常应用启动不会自动写入演示数据。
- 保留原三角色 smoke 账号和 `DEMO_CLASS_01`，并新增 199/200 两班、三位场景教师和八名固定场景学生；共 14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系。
- PR #16 已将题库扩充为 Demo90：90 道项目原创自编、无附件、可自动判分的 `PUBLISHED` 演示题；每科 30 道，每科三题型、三档难度、三个演示知识点各 10 道，来源权利状态为 `USER_PROVIDED`，审核轨迹完整。可见化学符号和科学计数法使用稳定 Unicode，STANDARD 解析不包含演示操作说明。
- PR #23 当时保留 Demo90，并从物理、化学、生物各 18 个原创候选中接受 10、9、11 道变式，形成历史 Demo120。当前 PR #27 的最终验收库已扩充为 Demo360；PR #23 候选审核历史仍见 `FINAL_DEMO_QUESTION_BANK.md`、`DEMO_VARIANT_QUESTION_REVIEW.md`。
- 本轮不修改 V1–V7，不写正式 `rike_tiku`，不修改 MVP30 原始 Excel。
- 历史 PR #14 合并后回归：后端 74/74、`mvn clean package` PASS；前端 68/68、类型检查和构建 PASS；`npm audit` 为 0 vulnerabilities。
- 真实脚本链 `reset → seed → validate → clean → reset → seed` PASS，末次 seed 后演示库保持待人工验收状态；正式库演示账号、演示题和 V7 五张学习表均为 0。
- 人工验收问题 MA-001 已关闭：后端 18081、前端 18080、`/api/v1` 基址和 `rike_tiku_demo` 连接均正确；demo_admin 登录、demo_teacher 真实 HTTP 登录和 demo_student 浏览器登录均已复验，原 `INVALID_CREDENTIALS` 不再复现。
- MA-001 至 MA-015 已通过专项与相应真实浏览器复验并关闭。PR #22 的 MA-006 证据覆盖三角色资料、简介、头像持久化/删除、多角色和改密入口。
- PR #16 合并后回归：Demo90 专项 7/7、后端 80/80、前端 72/72，package、type-check、build 均 PASS，生产依赖审计为 0；脚本链 `reset → seed → validate` PASS。此前完成的三科筛选、随机题集变化、未提交防泄露、提交/结果/错题链路和浏览器抽查保持 PASS。
- Demo90 不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题没有因此变为 `PUBLISHED`。
- PR #18 Demo 高频考点为 12 条项目原创自编纯文本演示内容：199/200 班物理、化学、生物每条 ACTIVE 任课关系各 2 条，均关联该科 Demo90 知识点。高频考点不使用附件、富文本或 AI 生成内容。

## 已合并基线

- PR #10：教师与三元任课关系，普通 merge `9495ecc`。
- PR #11：管理员题库审核发布，普通 merge `dda66d4`。
- PR #12：管理员 MVP30 题库导入，普通 merge `f499f0c`；MVP30 原始 Excel 仍未确认入库。
- PR #13：学生练习、自动判分、结果与错题闭环，普通 merge `db04fbc`。
- PR #14：本地演示数据与人工验收环境，普通 merge `4ffbcbd`。
- PR #15：UI、统一认证、学生三科工作台与教师任教范围，普通 merge `12d636f`。
- PR #16：Demo90 原创演示题库，普通 merge `588db6e`。
- PR #17：管理员单学生完整管理与 199/200 双班级演示场景，普通 merge `3e5454d`。
- PR #18：教师班级学科工作台与高频考点，普通 merge `b615bc1`。
- PR #19：登录随机图形验证码与交互优化，普通 merge `0a12943`。
- PR #20：三元任课关系约束下的师生私信，普通 merge `1055dee`。
- PR #21：知识点掌握度、确定性规则推荐与教师班级学情，普通 merge `c0d6553`；MA-014、MA-015 已关闭。
- PR #22：三角色个人中心、个人简介与头像，普通 merge `67b7bd7`；MA-006 已关闭。

## 下一步

 MA-017 已关闭：附件仅接受 3MB 内 PNG/JPEG，路径始终位于受控 storage 根目录且回读复核类型与 SHA-256。管理员与学生通过带 JWT 的 Blob 请求显示图片；未提交练习只暴露 STEM/OPTION，提交后才允许 STANDARD_ANALYSIS。PDF 与历史无文件元数据不进入普通练习；练习附件未单独快照，文件被替换或删除时按 hash 失败并友好降级。用户已完成最终人工浏览器复验。

附件对象标识规范：正文保留 `〔图片对象 I001〕`、`〔公式对象 F107〕`；Excel 导入和历史数据写入 `dui_xiang_biao_shi` 的是 `I001`、`F107`，不是带括号的完整正文 marker。管理员详情、学生题池校验和 `QuestionContent` 均先从正文提取对象 ID，再按位置和对象 ID 匹配。`QuestionAttachmentStorage` 还会拒绝 storage 根目录、父目录、中间目录或最终文件路径上的符号链接；当前 Windows 无创建符号链接权限，因此对应 JUnit 用例按 assumption 明确跳过，未伪造 PASS。

用户最终人工复验已完成，MA-017 至 MA-026 全部关闭，非 AI A 层为 `DONE_VERIFIED`。下一步是在 PR #27 ordinary merge 后同步最新 `main`，删除 `feat/non-ai-final-closure`，创建并推送 `feat/ai-provider-core` 作为 AI Provider Core 的干净入口；本阶段不修改 AI 代码、不创建 PR #28、不索取 API Key。

## 非 AI 工程基础完成门槛

只有以下各项全部完成并验证后，非 AI 工程基础才允许标记为 100%；任一项未满足时不得标记 100%：

- 基础工程与认证；
- 班级、教师和三元任课；
- 学生 Excel 导入与单学生完整管理；
- 基础个人资料、简介、头像；
- 管理员题库 CRUD、导入、审核、发布；
- 稳定可用的物理、化学、生物演示题库，以及已验证的结构化题库导入能力；
- 学生三科练习、随机练习、判分、结果、错题；
- 教师基础正式工作台；
- 高频考点；
- 师生私信；
- 非 AI 的基础掌握度与规则推荐；
- 三角色完整浏览器验收；
- 全量自动化与构建通过；
- Git、Flyway、文档和代码状态完全一致。

DeepSeek、GLM、AI Provider、AI 错题分析、AI 对话和 AI 生成题不属于该 100% 门槛。
