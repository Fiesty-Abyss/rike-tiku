# 跨AI项目上下文

> 2026-08-09 V3.0 非 AI 正式完工审计已确认当时不是 100% DONE_VERIFIED。PR #25 已合并关闭公共门户 MA-016，MA-017 后续已实现安全图片附件显示；管理员高风险操作日志与 30 道合法样例完整闭环仍是 A 层硬缺口。历史审计结论保持 REJECT；见 [V3_NON_AI_COMPLETION_AUDIT.md](V3_NON_AI_COMPLETION_AUDIT.md)。

> PR #26 已普通 merge（merge commit `b992bffef07465665b371b7b707ca8814ec2d36d`）。当前工作分支为 `feat/non-ai-final-closure`；人工 CAPTCHA/浏览器验收延期至非 AI 最终集成验收，MA-017 仍为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。PR #27 是最后一个非 AI Draft PR，当前加入 V11 管理员操作日志、MA-020、管理员题目图片上传、来源权利补充 API、Golden30 正常导入闭环、菜单整理和多角色切换；Flyway V1–V11、27 张业务表；V1–V10 和 MVP30 原始 Excel 未修改。

> PR #27 当前最终机器口径：后端 133 个测试，0 failure、0 error、1 个符号链接 assumption skipped，package PASS；前端 49 个文件、170/170、type-check、build、audit 0。Demo360 为物理/化学/生物各 120 道，叶子知识点 18/16/21，另有 Topic18，总题量 378。MA-021 至 MA-025 的机器修正均为 `FIXED_AWAITING_USER_RETEST`；最终人工后端不暴露 CAPTCHA `testCode`，真实 CAPTCHA/视觉验收仍由用户执行。

> 第二轮视觉复验正在同一 PR #27 收口：Portal 已去宣传化并删除 AI 规划区；三科入口采用语义 SVG；学生/教师学科环境统一由稳定 `subjectCode` 驱动；显式 TeX 片段由受控 KaTeX DOM renderer 输出 HTML+MathML。唯一正式主题仍为 `mizuiro-aero`，不增加第二主题。最终全量数字以本轮复跑后更新为准。

> 第三轮 MA-021 至 MA-025 已完成机器修正并统一为 `FIXED_AWAITING_USER_RETEST`。Portal、完整冻结答案、Demo360/Topic18 STANDARD 解析和 accepted answers 判分均在既有模块化单体与 V1–V11 内完成；AI 仍未开始。

更新时间：2026-08-11

## 1. 项目身份

- 正式选题：集成大模型智能答疑的在线题库实训管理系统。
- 工程范围：高中物理、化学、生物，前后端分离的模块化单体。
- 技术栈：Java 25、Spring Boot 4.x、MyBatis-Plus、Flyway、MySQL 8.4；Vue 3、TypeScript、Vite、Element Plus。
- 目标用户：学生、教师、管理员。
- 核心闭环：题目练习、自动判分、标准解析、错题沉淀、AI辅助答疑。

## 2. 事实优先级

```text
实际代码与配置
> Flyway迁移和真实数据库结构
> 自动化测试结果
> Git提交状态
> DEVELOPMENT_STATUS
> AI_HANDOFF
> 设计文档
> 聊天总结
```

计划不得写成已实现；未实际执行的测试不得标记为通过。

## 3. 用户开发偏好

- 不盲目迎合；方案有错误、冲突、范围膨胀或答辩风险时直接指出。
- 优先保证本科毕业设计按期完成，并保持代码、数据库、接口、前端、论文和答辩一致。
- 代码应简单、清晰、容易讲解，优先普通条件判断、循环、小方法、明确SQL和基础MyBatis-Plus CRUD。
- 不采用无必要的微服务、复杂反射、过度泛型和大型设计模式。
- 每轮只完成一个明确主任务；开发轮必须测试、更新交接、提交并推送。

## 4. 冻结设计

- V3.0是当前唯一有效设计基线；V1.0、V1.1只保留为历史资料。
- 数据库表和字段使用 `pinyin_snake_case`；Java类使用PascalCase拼音；Java字段使用lowerCamelCase拼音；API路径和枚举使用英文。
- 首版关闭学生自由注册和教师自由申请；学生由管理员Excel批量导入，教师由管理员创建或导入；邀请码不进入首版。
- 基础角色只有 `STUDENT`、`TEACHER`、`ADMIN`；同一用户可以拥有多个角色。
- 教师数据权限必须通过 `jiao_shi_id + ban_ji_id + ke_mu_id` 三元任课关系表达。
- 单选、多选、填空支持自动判分；综合大题只做专题学习，不自动评分。
- 标准答案和标准解析是权威事实；AI解析不得覆盖标准解析。
- AI候选题必须为 `PENDING`，经过人工审核后才能发布。
- AI故障不能影响登录、题库、练习、判分、错题和标准解析。
- 不采用微服务；Redis、MinIO、WebSocket、Docker和本地大模型不阻塞MVP。
- 登录验证码是本科毕设演示型能力，不扩展 Redis、OCR、轨迹分析、第三方验证码或风控系统。

## 5. 当前实现状态

- 状态：题库、账号、教学组织、学生练习闭环、教师班级学科工作台、高频考点、师生私信、非 AI 掌握度与确定性规则推荐、三角色个人中心均已进入 `main`。
- 当前分支：`feat/non-ai-final-closure`；PR #26 已合并，公共门户和附件机器实现已进入 `main`。
- 公共根路径 `/` 已实现无需认证的门户，第二轮视觉收敛后只保留系统事实说明、三科学科语义视觉、真实题量快照和统一 `/login` 入口；学习闭环、角色分工、设计理念和未上线 AI 规划不再作为首页宣传区块。自动化、三角色 Demo 浏览器、登录态与刷新、响应式及控制台证据均通过，MA-016 已关闭。
- 当前 MA-017 已完成机器实现：受控 PNG/JPEG 存储、3MB 与真实 MIME 校验、SHA-256 回读、附件归属权限、未提交 STANDARD_ANALYSIS 防泄露、管理员详情和学生练习/结果/错题 Blob 显示；状态为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。用户 CAPTCHA 和浏览器视觉验收延期至非 AI 最终集成验收，未验收前不关闭。
- PR #26 独立审查修正已完成：正文 marker 保留 `〔图片对象 I001〕`，数据库 `dui_xiang_biao_shi` 只保存 `I001`；真实 QuestionImportService 导入链已覆盖 preview、confirm、受控 storage、管理员 HTTP 内容和学生提交前后权限。机器门禁和 Demo 最新结果记录在 `DEVELOPMENT_STATUS.md`，人工验收不属于 PR #26 merge gate。
- 当前 Flyway：V1–V11，共 27 张业务表；V10 只增加 `yong_hu` 简介与头像字段，V11 只新增管理员操作日志表，V1–V10 未修改。
- PR #27 当前机器实现已覆盖 MA-018、MA-020、管理员题目图片上传、来源权利补充、Golden30 正常导入闭环、正式菜单整理和多角色切换，并根据用户反馈补齐 Dashboard、教师密码重置、练习可用题数、逐题结果、知识点与类似练习、错题实时过滤和 Topic18。第三轮 MA-021 至 MA-025 增加五章 Portal 与原创静态图、内联掌握比例、冻结完整答案、Demo 246 道选择题逐项解析、Topic18 安全分段和显式 accepted answers 数值等价。最新全量门禁为后端 133 个测试 0 failure、0 error、1 个 symbolic-link assumption skipped、package PASS，前端 49 个文件 170/170、type-check/build/audit 通过，Demo `acceptance-prepare → validate → smoke` PASS。题干附件内容 hash、附件文件/数据库事务、草稿文字、班级 SUCCESS 审计和附件外键稳定性修正继续保持。Golden30 在独立测试库真实验证物理/化学/生物各 10 道从 preview、confirm 到审核发布和学生练习，原始候选文件与正式库均未被写入。
- 独立 `rike_tiku_demo` 当前使用确定性 Demo360：物理、化学、生物各 120 道，覆盖 55 个叶子知识点；另有 Topic18（每科 6 道），总题量 378。历史 Demo90/Demo120 口径仅用于说明先前 PR。V3.0 不要求名为 MVP30 的 Excel 整体正式入库；该原始文件保持不变，定位为结构化导入能力验证素材。
- PR #17 已进入 `main`：新增管理员单学生分页、详情、事务新增、编辑启停、事务调班和密码重置；Demo 扩充为 14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系。
- PR #18 合并后后端 87/87、前端 83/83，package、type-check、build、audit、Demo 脚本链均通过；真实浏览器高频考点权限验收保持通过。正式库污染检查包含 Demo90、场景账号、场景班级和高频考点，均为 0。
- PR #19 已将 PR #15 历史滑块替换为 4 位随机图形验证码并进入 `main`；后端使用 JDK 原生图片 API、两分钟内存 challenge 和一次性消费。PR #27 当前前端首次渲染即显示验证码，三项一次填写、一次登录，失败后刷新 challenge；最终人工环境响应不含 `testCode`。
- PR #20 使用 REST polling 实现受 ACTIVE 三元任课关系和学生当前主班级约束的纯文本私信、未读和历史保留，已普通 merge。合并后后端 92/92、前端 100/100，package、type-check、build、audit 0、Demo 脚本链通过；双班级双向浏览器验收及 conversationId 越权隔离保持通过，MA-009 已关闭。
- PR #21 已基于 V7 已提交自动判分答题、冻结知识点快照和错题状态实时计算掌握度并进入 `main`；当前学科全部 ACTIVE 知识点参与掌握度和总体统计，5 题推荐资格独立复用真实学生练习题池规则。题量不足不会隐藏历史掌握事实，也不会生成无法创建的推荐。推荐采用公开固定优先级并最多返回 3 项，教师学情查询继续受本人 ACTIVE 三元任课关系约束；无 V10。合并后后端 98/98、前端 29 文件 106/106，package、type-check、build、audit 0、Demo 链与正式库只读检查均通过；MA-014、MA-015 已关闭。
- PR #22 已实现并普通 merge 三角色统一 `/profile`、本人资料/真实角色/业务档案只读展示、简介、MySQL 小头像和现有主动改密入口。所有 profile API 从 JWT 推导本人，不接收 userId/studentId/teacherId；首次登录必须先完成初始密码修改。合并后自动化为后端 102/102、前端 31 文件 117/117；Demo 脚本与浏览器验收通过，MA-006 已关闭。
- PR #23 已普通 merge，保留 Demo90 题量和覆盖并接受 30/54 道原创变式，最终演示题库为 120 道。合并后后端 105/105、前端 31 文件 117/117、package/type-check/build/audit 0、Demo 脚本链均通过；合并前专项 27/27 及浏览器抽查物理 3、化学 3、生物 4 道新变式均 PASS，三题型、结果/解析、错题、掌握度、随机变化和控制台正常。正式库只读污染项为 0。开发阶段 Codex 辅助候选制作不是系统运行时 AI 能力。
- MA-013 已关闭：全新临时库与正式库的 V9 script、checksum `1192958817` 和 success 完全一致。PR #20 进入 main 后，正式库提前执行的 V9 已成为正式基线的一部分，两张结构表不属于业务数据污染；六项演示/私信数据仍均为 0。
- 历史 PR #13 自动化为后端 68/68、前端 68/68；历史 PR #14 自动化为后端 74/74、前端 68/68。PR #15 合并后自动化为后端 79/79、前端 72/72，打包、类型检查、构建和依赖审计均通过。
- 远程仓库：`https://github.com/Fiesty-Abyss/rike-tiku`，公开仓库，默认分支 `main`。

学生导入、管理员单学生管理、教师管理、三元任课关系、题库审核发布、结构化 Excel 导入、学生练习闭环、UI、统一认证、学生三科工作台、教师班级学科工作台、高频考点、私信、规则掌握度和推荐、三角色个人中心均已进入 `main`。掌握度与推荐不属于 AI；PR #23 的 Codex 辅助候选制作也不等于系统运行时 AI 出题，AI 能力仍未实现。

## 6. AI接管规则

- 接管时先读取本文件、`DEVELOPMENT_STATUS.md`、`AI_HANDOFF.md`、最新数据库文档、全部Flyway迁移、测试、Git提交和分支状态。
- 不修改已经执行的Flyway迁移；数据库变化必须新增迁移。
- 不把计划描述为已实现，不扩展到九科，不创建自由注册或邀请码。
- 不把教师—班级与教师—科目拆成两个独立权限关系。
- 不提前开发课堂WebSocket，不直接发布AI候选题，不让AI覆盖标准解析。
- 题库资料只作学习、开发和审核候选；未经版权和学科人工核验不得改为 `PUBLISHED`。
- Entity 只对应数据库，不直接暴露密码摘要、逻辑删除等内部字段；API 输入优先使用 `XxxRequest`，输出优先使用 `XxxResponse`，可统一放在 `dto` 包。
- 不机械创建 VO、Converter 或 Assembler；只有出现真正独立的页面展示模型时才考虑 VO。
- 每轮结束更新开发状态和AI交接，并给出下一轮唯一任务。
