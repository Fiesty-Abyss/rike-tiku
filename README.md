# 集成大模型智能答疑的在线题库实训管理系统

本科毕业设计项目，面向高中物理、化学、生物的在线题库实训场景。系统计划围绕“题目练习 → 自动判分 → 标准解析 → 错题沉淀 → AI辅助答疑”构建可演示、可测试、可降级的学习闭环。

> 当前仓库处于分阶段开发中，不代表完整系统已经完成或投入真实学校使用。

> 2026-08-09 V3.0 非 AI 正式完工审计结论为 **REJECT**：公共门户首页、附件真实显示、管理员高风险操作日志、30 道合法样例完整导入发布显示闭环仍是 A 层硬缺口，因此当前不得标记为 100% DONE_VERIFIED，也不得开始 AI。详见 [V3.0 非 AI 完工审计](docs/V3_NON_AI_COMPLETION_AUDIT.md)。

## 工程范围

- 学科：高中物理、化学、生物。
- 题型：首版规划支持单选、多选、填空自动判分；综合大题只用于专题学习，不自动评分。
- 用户：学生、教师、管理员。首版关闭自由注册，账号由管理员创建或批量导入。
- AI：标准答案和标准解析始终是权威基础；AI解析不得覆盖标准解析，AI不可用时不得影响题库、练习、判分和错题等核心功能。

## 技术栈

- 后端：Java 25、Maven、Spring Boot 4.1、Spring MVC、Spring Security、MyBatis-Plus、Flyway、MySQL、SpringDoc OpenAPI、JUnit 5。
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios。
- 数据库：MySQL 8.4，业务表和字段使用 `pinyin_snake_case`。

## 目录

```text
rike-tiku-backend/    Spring Boot 后端
rike-tiku-frontend/   Vue 3 前端
docs/                 设计、状态与AI交接文档
database/             数据库说明、ER图和结构快照
题库/                 离线题库候选资料及质量检查结果
脚本/                 离线整理与检查脚本
scripts/              本地开发与演示环境PowerShell工具
```

Flyway 是数据库结构的唯一建表和升级入口。已经执行的迁移不得修改，后续结构变化必须新增版本迁移。

## 当前进度

已经完成并验证：

- 前后端基础工程、健康检查、真实数据库连接和开发环境CORS联调；
- 题库核心数据库V1版，Flyway V1–V4；
- 10张题库业务表；
- 物理、化学、生物各1道 `PENDING` 最小真实样本；
- 账号、角色、学生/教师档案、班级、班级学生历史和三元任课关系数据库模型；
- Flyway V1–V6，共18张业务表；
- 最小MyBatis-Plus映射、数据库约束测试和空库全迁移测试。
- 后端统一登录、BCrypt密码校验、JWT访问Token、当前用户、首次改密门禁和三角色鉴权。
- 管理员班级基础管理后端：分页查询、详情、创建、修改和状态变更；所有接口仅限管理员（PR #6已普通merge）。
- 管理员学生Excel导入模板与预检查后端：仅生成模板和逐行预览，不创建账号、学生档案或班级学生关系（PR #7已普通merge）。
- 管理员学生Excel导入后端：模板、预检查与确认入库；确认时在一个事务中创建账号、STUDENT角色、学生档案和主班级关系。
- 管理员班级管理与学生Excel导入前端：管理员布局、班级分页筛选/创建/编辑/状态切换，以及模板下载、预检查、确认入库、账号发放结果与Excel下载。
- 管理员教师账号与档案、教师—班级—科目三元任课关系：管理员分页筛选、创建、修改、一次性初始密码、任课关系创建与状态结束/停用。
- 管理员题库审核发布：题目分页与筛选、草稿创建/编辑、完整详情、版权复核和 `DRAFT → PENDING → PUBLISHED → DISABLED → PUBLISHED` 状态流；已通过普通 merge 合并到 `main`（PR #11）。
- 管理员 MVP30 题库导入：单文件 Excel 预检查、逐行错误、知识点精确匹配、来源文件追溯、附件对象精确映射与全批次确认入库；成功题目和 STANDARD 解析统一为 `PENDING`。已通过普通 merge 合并至 `main`（PR #12，合并提交 `f499f0c2e1e3b4637d22480868e94dbdacdcbaa0`）。纯 V1–V6 测试库预检查结果为物理 0/10、化学 1/10、生物 1/10；仅在测试事务预置 Excel 所需知识点后，附件专项结果为 2/10、1/10、6/10。随机临时库的真实 HTTP multipart 与浏览器回查结论为 `PASS_WITH_ENV_LIMITATION`；匿名临时题已清理，MVP30 原始 Excel 尚未确认入库。
- 学生自主练习、自动判分与错题闭环已通过普通 merge 进入 `main`（PR #13，合并提交 `db04fbc9caeeb5e4eb003a45581e62e76dbab420`）：创建时仅冻结无活动附件、无图片/公式对象标记的题集，提交整场答案后完成单选/多选/填空自动判分、结果与错题聚合；未提交前不返回标准答案或解析。历史 PR #13 自动化结果为后端 68/68、前端 68/68；附件文件访问、图片展示和公式渲染不在首版范围内。

已完成前端认证基础：三角色登录入口、Pinia认证状态、Bearer Token注入、会话恢复、首次改密、路由守卫、管理员业务页及学生三科学习工作台。PR #21 已将基于真实答题事实的知识点掌握度、规则推荐和教师班级学情查看普通 merge 进入 `main`；它是确定性规则统计，不属于 AI。尚未完成：AI Provider、AI 答疑、教师任务与考试。当前最终演示题库为保留 Demo90 加 30 道筛选变式，共 120 道；MVP30 是结构化导入能力验证素材，不等于最终演示内容。

准确状态请以 [开发状态](docs/DEVELOPMENT_STATUS.md) 和 [AI交接](docs/AI_HANDOFF.md) 为准。

## 本地演示验收环境

`main` 已通过 PR #14（普通 merge `4ffbcbda66f26e7390192985ce179f30d3a6b664`）提供显式执行的独立演示库工具。它默认操作 `rike_tiku_demo`，拒绝操作 `rike_tiku` 及 MySQL 系统库，不会在应用正常启动时自动写入数据。准备好 `RIKE_TIKU_DB_PASSWORD` 后执行：

```powershell
.\scripts\demo-environment.ps1 reset
.\scripts\demo-environment.ps1 seed
.\scripts\demo-environment.ps1 validate
```

演示账号为 `demo_admin`、`demo_teacher`、`demo_student`，本地演示密码均为 `a1234567`；数据库仅保存 BCrypt 摘要。分别启动前后端：

```powershell
.\scripts\demo-environment.ps1 backend
.\scripts\demo-environment.ps1 frontend
```

详细安全边界与操作说明见 [演示环境说明](docs/DEMO_ENVIRONMENT.md)，人工检查步骤见 [人工验收清单](docs/MANUAL_ACCEPTANCE_CHECKLIST.md)。

使用 IDEA 直接启动时必须在运行配置增加 `RIKE_TIKU_DB_NAME=rike_tiku_demo`；否则后端默认连接正式开发库 `rike_tiku`，其中不存在演示账号。默认端口方案的前端 API 地址为 `http://localhost:8081/api/v1`。脚本演示端口方案可在服务启动后执行 `.\scripts\demo-environment.ps1 smoke` 验证健康状态和三角色登录。

历史 PR #14 自动化验证为后端 74/74、前端 68/68，后端打包、前端类型检查与构建均通过，`npm audit` 为 0 vulnerabilities；真实脚本链及三角色 HTTP smoke 已通过。PR #15 已普通 merge 进入 `main`（merge commit `12d636fde4afa198edc78eb0c295f5b88c8e3456`）：当时统一登录使用服务端短时一次性滑块验证，并提供中文化、学生三科工作台、教师任教范围和主动改密。PR #15 合并后回归为后端 79/79、前端 72/72；该滑块只属于历史实现，已由 PR #19 的随机图形验证码替换。

PR #16 已普通 merge（merge commit `588db6ee5b2a6c466c618249f072591af47609a1`），独立 `rike_tiku_demo` 已扩充为 Demo90：物理、化学、生物各 30 道项目原创自编演示题，每科三题型、三档难度和三个演示知识点各 10 道。Demo90 使用稳定 Unicode 表达化学式、电荷与科学计数法，STANDARD 解析为简短学科解析。它仅用于稳定演示筛选、随机练习、判分和错题链路，不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题也未因此改为 `PUBLISHED`。PR #16 未新增 Flyway 迁移，未写正式 `rike_tiku`。

PR #17 已普通 merge（merge commit `3e5454de8257075d1ccdf11d5f6d3a35b464adc1`）进入 `main`。它新增 `/admin/students` 单学生管理，保留 `/admin/students/import` 批量导入；支持分页筛选、详情与班级历史、事务新增、资料和状态编辑、调班、启停及管理员重置密码。`rike_tiku_demo` 同时扩充为保留原 smoke 数据的 3 班、4 教师、9 学生、9 条 ACTIVE 三元任课关系场景，其中 199 班 5 名固定学生、200 班 3 名固定学生。PR #17 合并时 Flyway 为 V1–V7；未修改 MVP30 原始 Excel。合并后回归为后端 86/86、前端 80/80，package、type-check、build、生产依赖 audit 及 `reset → seed → validate → smoke` 均通过。

PR #18 已普通 merge（merge commit `b615bc1a78d842d61928abc8f89b839f52c88b7f`）进入 `main`。它新增 V8（不修改 V1–V7），将业务表扩展为 24 张：教师可在本人 ACTIVE 三元任课关系内进入班级学科工作台，查看班级学生和维护高频考点；学生端依据本人有效主班级和所选科目只读取对应 ACTIVE 高频考点。Demo 环境预置 199/200 六条场景任课关系各 2 条，共 12 条自编高频考点。浏览器验收已覆盖物理教师新增、编辑、停用、启用、双班切换，生物/化学学科隔离，以及 199/200 学生内容隔离；MA-008 已关闭。合并后验证为后端 87/87、前端 83/83、package/type-check/build/audit 和 Demo 脚本链均通过。

PR #19 已普通 merge（merge commit `0a12943e901e844520e3801264fa4a43590ff28e`）进入 `main`，将登录页历史滑块替换为 JDK 生成的 4 位随机 PNG 图形验证码。验证码默认隐藏，第一次点击或 Enter 仅展开并获取 challenge，第二次才提交登录；challenge 在内存保存 2 分钟并一次性消费，不新增数据库、Redis 或第三方依赖。合并后回归为后端 90/90、前端 91/91，package、type-check、build 和生产依赖 audit（0 vulnerabilities）均通过；Demo `reset → seed → validate → smoke` PASS，正式库四项污染检查均为 0。

PR #20 已普通 merge（merge commit `1055dee567b7afa153750792670fb0bafed1151c`）进入 `main`。V9 新增 `si_xin_hui_hua`、`si_xin_xiao_xi`，业务表共 26 张；师生私信受 ACTIVE 三元任课关系和学生当前主班级约束，发送身份由 JWT 决定，使用 REST polling，不包含 WebSocket、附件、群聊或管理员消息审计。合并后后端 92/92、前端 100/100，package、type-check、build、audit 0、Demo 脚本链及此前真实浏览器双向私信验收均通过，MA-009 已关闭。

PR #21 已普通 merge（merge commit `c0d655324fec0a36772c2d095b6025e5f708fc4c`）进入 `main`。它基于 V7 已提交答题、冻结知识点快照、学习结果和错题状态实时计算掌握度，不新增迁移、缓存或统计表。掌握度覆盖当前学科全部 ACTIVE 知识点；5 题规则推荐单独复用真实练习题池资格。学生三科学科页展示知识点掌握与最多 3 项固定规则推荐，教师只在本人 ACTIVE 三元任课范围内查看班内学生当前学科汇总。合并后回归为后端 98/98、前端 29 文件 106/106，package、type-check、build、audit 0 及 Demo `reset → seed → validate → smoke` 均通过；MA-014、MA-015 已关闭。该功能是确定性规则统计，不属于 AI。

PR #22 已普通 merge（merge commit `67b7bd7239e2ac1de3ad8c71b82b6d0a79162d3b`）进入 `main`，提供三角色共用的 `/profile`：本人账号、真实角色、学生/教师档案只读展示，500 字个人简介，PNG/JPEG 小头像上传、持久化和删除，以及现有主动改密入口。V10 只为 `yong_hu` 增加简介与头像字段，不增加业务表、不修改 V1–V9；当前仍为 26 张业务表。首次登录用户必须先完成初始密码修改，不能提前访问或修改个人中心。合并后回归为后端 102/102、前端 31 文件 117/117，package、type-check、build、audit 0 和 Demo `reset → seed → validate → smoke` 均通过；MA-006 已关闭。

PR #23 已普通 merge（merge commit `3677c7623e08e34ee63e45dbcfd557a27b32f990`）进入 `main`，冻结最终非 AI 演示题库口径：保留经验证的 Demo90，并从 54 个开发阶段原创候选中筛选 30 道变式，形成 120 道最终 Demo 题库。V3.0 要求的是 30 题 MVP 对导入、审核、发布、查询和附件显示闭环的验证，并未指定名为 MVP30 的 Excel 必须整体成为最终演示题库；MVP30 原始文件保持不变，继续作为结构化导入能力验证素材。合并后回归为后端 105/105、前端 31 文件 117/117，package、type-check、build、audit 0 和 Demo `reset → seed → validate → smoke` 均通过；Flyway 仍为 V1–V10、26 张业务表。

## 本地启动

### 后端

后端的 `.env.example` 仅用于说明配置项。当前 Spring Boot 工程和 IDEA 不会自动加载 `.env` 或 `.env.example`。首次使用IDEA时，推荐在后端目录执行一次安全初始化脚本：

```powershell
cd rike-tiku-backend
.\scripts\setup-idea-local-env.ps1
```

脚本隐藏密码输入，只设置当前Windows用户环境变量，不向Git文件写入密码。执行后必须完全退出并重新打开IDEA，再运行 `RikeTikuBackendApplication`。

脚本还会在本机缺少JWT密钥时生成随机 `RIKE_TIKU_JWT_SECRET`。JWT默认有效期为7200秒，可通过 `RIKE_TIKU_JWT_EXPIRATION_SECONDS` 覆盖。

从终端临时启动时也可以只为当前PowerShell设置密码：

```powershell
$env:RIKE_TIKU_DB_PASSWORD="your-local-password"
cd rike-tiku-backend
mvn spring-boot:run
```

如果不希望设置Windows用户环境变量，也可在IDEA私有 Run Configuration中添加同名变量。数据库地址或账号不是默认值时，再配置 `RIKE_TIKU_DB_HOST`、`RIKE_TIKU_DB_PORT`、`RIKE_TIKU_DB_NAME` 和 `RIKE_TIKU_DB_USERNAME`。项目不读取 `RIKE_TIKU_DB_URL`。

详细步骤和故障排查见 [后端本地启动说明](rike-tiku-backend/README.md)。默认后端地址为 `http://localhost:8081`，健康接口为 `http://localhost:8081/api/v1/health`。

### 前端

```powershell
cd rike-tiku-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

默认前端地址为 `http://localhost:8080`。前端API地址通过 `VITE_API_BASE_URL` 配置。

前端 Vite 会按其规则加载 `.env.local`；这与后端 Spring Boot 的环境变量加载方式不同，请勿混淆。

## 验证命令

```powershell
cd rike-tiku-backend
mvn clean test
mvn clean package

cd ../rike-tiku-frontend
npm run type-check
npm run build
```

## 文档索引

- [跨AI项目上下文](docs/AI_PROJECT_CONTEXT.md)
- [开发状态](docs/DEVELOPMENT_STATUS.md)
- [AI交接](docs/AI_HANDOFF.md)
- [题库核心数据库模型V1](docs/QUESTION_DATABASE_MODEL_V1.md)
- [后端认证接口](docs/AUTHENTICATION_API.md)
- [管理员班级管理接口](docs/ADMIN_CLASS_MANAGEMENT_API.md)
- [学生Excel导入预检查接口](docs/STUDENT_IMPORT_PREVIEW_API.md)
- [管理员班级与学生导入前端](docs/ADMIN_STUDENT_IMPORT_FRONTEND.md)
- [管理员学生管理 API](docs/ADMIN_STUDENT_MANAGEMENT_API.md)
- [管理员学生管理前端](docs/ADMIN_STUDENT_MANAGEMENT_FRONTEND.md)
- [教师工作台与高频考点 API](docs/TEACHER_WORKSPACE_HIGH_FREQUENCY_API.md)
- [教师工作台与高频考点前端](docs/TEACHER_WORKSPACE_HIGH_FREQUENCY_FRONTEND.md)
- [师生私信 API](docs/TEACHER_STUDENT_MESSAGING_API.md)
- [师生私信前端](docs/TEACHER_STUDENT_MESSAGING_FRONTEND.md)
- [知识点掌握度与规则推荐](docs/LEARNING_MASTERY_RULE_RECOMMENDATION.md)
- [最终演示题库](docs/FINAL_DEMO_QUESTION_BANK.md)
- [变式候选审核记录](docs/DEMO_VARIANT_QUESTION_REVIEW.md)
- [管理员教师与任课关系接口](docs/ADMIN_TEACHER_ASSIGNMENT_API.md)
- [管理员教师与任课关系前端](docs/ADMIN_TEACHER_ASSIGNMENT_FRONTEND.md)
- [管理员题库审核发布接口](docs/ADMIN_QUESTION_REVIEW_API.md)
- [管理员题库审核发布前端](docs/ADMIN_QUESTION_REVIEW_FRONTEND.md)
- [管理员题库导入接口](docs/ADMIN_QUESTION_IMPORT_API.md)
- [管理员题库导入前端](docs/ADMIN_QUESTION_IMPORT_FRONTEND.md)
- [学生练习与自动判分接口](docs/STUDENT_PRACTICE_API.md)
- [学生练习前端](docs/STUDENT_PRACTICE_FRONTEND.md)
- [本地演示环境](docs/DEMO_ENVIRONMENT.md)
- [人工验收清单](docs/MANUAL_ACCEPTANCE_CHECKLIST.md)
- [人工验收问题记录](docs/MANUAL_ACCEPTANCE_FINDINGS.md)
- [V3.0 非 AI 正式完工审计](docs/V3_NON_AI_COMPLETION_AUDIT.md)
- V3.0总体设计公开脱敏版（位于 `docs/`）

## 题库资料和权利说明

`题库/` 中的Excel、JSON、图片、公式对象、Word和PDF资料仅作为本科毕业设计的学习、开发与人工审核候选材料。当前样本保持 `PENDING`，来源权利状态保持 `COPYRIGHT_UNKNOWN`；仓库不声明这些资料已经获得公开传播授权。使用者应在正式发布前完成学科质量和版权核验。

仓库不得包含数据库真实密码、API Key、JWT密钥、访问令牌、学生手机号、身份证、家庭住址或真实成绩等敏感信息。

## 下一阶段

PR #24 已普通 merge（merge commit `bcfb2181af2197d2524a2df8ca64895e435a4857`）进入 `main`，V3.0 非 AI 正式完工审计结论为 REJECT；下一轮唯一任务是实现 A 层公共门户首页。当前不开始 AI；DeepSeek、GLM 和运行时 AI 能力仍未实现。
