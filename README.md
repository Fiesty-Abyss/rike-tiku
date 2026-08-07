# 集成大模型智能答疑的在线题库实训管理系统

本科毕业设计项目，面向高中物理、化学、生物的在线题库实训场景。系统计划围绕“题目练习 → 自动判分 → 标准解析 → 错题沉淀 → AI辅助答疑”构建可演示、可测试、可降级的学习闭环。

> 当前仓库处于分阶段开发中，不代表完整系统已经完成或投入真实学校使用。

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

已完成前端认证基础：三角色登录入口、Pinia认证状态、Bearer Token注入、会话恢复、首次改密、路由守卫、管理员业务页及最小学生练习工作台。尚未完成：AI Provider、AI 答疑、掌握度、推荐、教师任务与考试。题库30题候选数据尚未正式发布。

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

历史 PR #14 自动化验证为后端 74/74、前端 68/68，后端打包、前端类型检查与构建均通过，`npm audit` 为 0 vulnerabilities；真实脚本链及三角色 HTTP smoke 已通过。PR #15 已普通 merge 进入 `main`（merge commit `12d636fde4afa198edc78eb0c295f5b88c8e3456`）：统一登录使用服务端短时一次性滑块验证，并提供中文化、学生三科工作台、教师任教范围和主动改密。合并后回归为后端 79/79、前端 72/72，打包、类型检查、构建和 `npm audit --omit=dev` 均通过，且不新增迁移。2026-08-07 已在 `rike_tiku_demo` 完成真实浏览器验收，MA-001 至 MA-005、MA-010、MA-011 均已关闭，MA-006 至 MA-009 尚未完成。

PR #16 已普通 merge（merge commit `588db6ee5b2a6c466c618249f072591af47609a1`），独立 `rike_tiku_demo` 已扩充为 Demo90：物理、化学、生物各 30 道项目原创自编演示题，每科三题型、三档难度和三个演示知识点各 10 道。Demo90 使用稳定 Unicode 表达化学式、电荷与科学计数法，STANDARD 解析为简短学科解析。它仅用于稳定演示筛选、随机练习、判分和错题链路，不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题也未因此改为 `PUBLISHED`。PR #16 未新增 Flyway 迁移，未写正式 `rike_tiku`。

PR #17 已普通 merge（merge commit `3e5454de8257075d1ccdf11d5f6d3a35b464adc1`）进入 `main`。它新增 `/admin/students` 单学生管理，保留 `/admin/students/import` 批量导入；支持分页筛选、详情与班级历史、事务新增、资料和状态编辑、调班、启停及管理员重置密码。`rike_tiku_demo` 同时扩充为保留原 smoke 数据的 3 班、4 教师、9 学生、9 条 ACTIVE 三元任课关系场景，其中 199 班 5 名固定学生、200 班 3 名固定学生。Flyway 仍为 V1–V7，未修改 MVP30 原始 Excel。合并后回归为后端 86/86、前端 80/80，package、type-check、build、生产依赖 audit 及 `reset → seed → validate → smoke` 均通过。

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
- V3.0总体设计公开脱敏版（位于 `docs/`）

## 题库资料和权利说明

`题库/` 中的Excel、JSON、图片、公式对象、Word和PDF资料仅作为本科毕业设计的学习、开发与人工审核候选材料。当前样本保持 `PENDING`，来源权利状态保持 `COPYRIGHT_UNKNOWN`；仓库不声明这些资料已经获得公开传播授权。使用者应在正式发布前完成学科质量和版权核验。

仓库不得包含数据库真实密码、API Key、JWT密钥、访问令牌、学生手机号、身份证、家庭住址或真实成绩等敏感信息。

## 下一阶段

PR #13 至 PR #17 均已普通 merge；PR #17 合并基线为 `main@3e5454de8257075d1ccdf11d5f6d3a35b464adc1`。当前停止并等待下一轮明确指令，不创建下一分支。MVP30 尚未正式入库，基础个人资料、头像、教师正式业务工作台、高频考点、私信、掌握度、推荐、DeepSeek、GLM 和 AI 能力均未实现，非 AI 工程基础不得标记为 100%。
