# RIKE 理科学习辅助系统

**面向高中物化生的 Spring Boot 大模型题库系统设计与实现**

面向高中物理、化学、生物学习场景，集成题库、在线练习、错题分析、AI 当前题答疑、可控变式题生成和人工审核的本科毕业设计系统。

项目早期使用过“集成大模型智能答疑的在线题库实训管理系统”这一标题，当前论文题目与仓库首页统一采用上方正式名称。

## 当前状态

| 能力 | 状态 |
| --- | --- |
| 非 AI 主链 | `DONE_VERIFIED` |
| AI Provider Core | `DONE_VERIFIED` |
| 学生 AI 错因分析 | `DONE_VERIFIED` |
| 当前题有限多轮答疑 | `DONE_VERIFIED` |
| 真实 DeepSeek | `PASS` |
| 管理员 AI 模型配置 | `DONE_VERIFIED` |
| GLM Vision 代码链 | `DONE_VERIFIED` |
| 真实 GLM Smoke | `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX` |
| AI 候选变式题 | `DONE_VERIFIED` |
| 教师、管理员人工审核 | `DONE_VERIFIED` |
| 最终机器集成验证 | `AUTO_FINAL_VERIFICATION_PASS` |
| 最终用户人工验收 | `FINAL_MANUAL_ACCEPTANCE_PENDING` |

这里的 `DONE_VERIFIED` 表示对应业务实现与专项验证已通过。PR #31 已完成全量自动化、Demo、真实 DeepSeek、权限与降级、机器浏览器和文档统一；真实 GLM 的第二次受控调用暴露了完整 JSON 代码围栏兼容问题，Parser 已修复并通过全量自动化，但遵守两次真实调用上限没有继续请求，因此不能写成真实 GLM PASS。最终用户人工验收仍待本人完成。详细事实见 [开发状态](docs/DEVELOPMENT_STATUS.md) 与 [最终实验事实](docs/AI_FINAL_EXPERIMENT_RESULTS.md)。

PR #31 已由用户明确决定 ordinary merge；当前 PR #32 只进行本机正式化、论文资料整理和维护，不扩张核心业务。论文写作请从 [论文写作资料中心](docs/THESIS_WRITING_HUB.md) 开始。

本机 IDEA/WebStorm 正式运行口径见 [本机正式运行环境](docs/LOCAL_FORMAL_ENVIRONMENT.md)。

## 核心学习闭环

```mermaid
flowchart LR
    A[题目练习] --> B[自动判分]
    B --> C[STANDARD 标准解析]
    C --> D[错题记录]
    D --> E[AI 个性化错因]
    E --> F[当前题 AI 答疑]
    F --> G[薄弱点]
    G --> A
```

AI 候选题始终经过人工审核，不直接进入学生练习。

```mermaid
flowchart LR
    A[PUBLISHED 母题] --> B[参数化变式]
    B --> C{图片题}
    C -->|是| D[GLM Vision]
    C -->|否| E[DeepSeek]
    D --> E
    E --> F[JSON 校验]
    F --> G[重复检测]
    G --> H[PENDING]
    H --> I[教师或管理员评价审核]
    I --> J[PUBLISHED]
```

## 三类角色

### STUDENT

- 登录、练习、自动判分与 STANDARD 标准解析
- 错题本、高频考点与学习情况
- AI 错因分析与 RIKE 当前题答疑
- 师生消息

### TEACHER

- 按任教班级与学科查看学生学习情况和高频考点
- 师生消息
- AI 变式题生成、候选质量评价与候选题审核

### ADMIN

- 学生、教师、班级与任课关系管理
- 题库、导入、审核发布、附件与操作日志
- AI 模型配置、DeepSeek 与 GLM Key、连接测试
- AI 候选题与全局质量统计

## AI 架构

DeepSeek V4 是文本推理“大脑”，负责错因分析、当前题答疑和候选变式题生成。GLM-4.6V-Flash 是视觉“眼睛”，只负责图片题视觉语义提取。

```text
图片 → GLM → UNTRUSTED_VISION_CONTEXT → DeepSeek
```

- GLM 不负责正式判分，也不能覆盖 STANDARD。
- DeepSeek 的分析和候选答案不覆盖 STANDARD。
- 学生端不显示 Provider、模型代码、API 地址、Key 或 Token。
- 学生端统一使用“RIKE 理科学习助手”身份。
- Provider 调用日志只保存安全元数据，不保存 Prompt、输出、图片 Base64 或 Key。

## 受控限制

学生答疑只绑定当前题，最多 8 轮；单条用户消息最多 500 字；Provider 上下文最多最近 12 条消息，总字符预算 6000。无关闲聊会被限制回当前物理、化学、生物学习场景。

Vision 每题最多 2 张图片，单图不超过 3 MB，总量不超过 6 MB，只接受 PNG/JPEG。附件按 SHA-256 去重，GLM 输出必须通过受控 JSON 校验，相同视觉上下文优先复用缓存。

变式题每次生成 1 至 3 道，同一母题的 `AI_GENERATED + PENDING` 候选最多 6 道。系统使用 request hash、精确内容 hash、批内去重和 trigram/Jaccard 疑似重复提示。候选只能先进入 `PENDING`，人工审核通过后才能发布。

## 技术栈

### Backend

- Java 25
- Spring Boot 4.1
- Maven、Spring MVC、Spring Security、JWT
- MyBatis-Plus 3.5.17、Flyway、MySQL 8.4
- Apache POI、SpringDoc

### Frontend

- Vue 3、TypeScript、Vite
- Element Plus、Pinia、Vue Router、Axios
- GSAP、KaTeX

### AI

- DeepSeek OpenAI-compatible API
- GLM-4.6V-Flash Vision
- Fake/Stub Provider
- Structured JSON
- Metadata-only AI call log

Redis、消息队列、向量数据库、WebSocket 和微服务不是当前主线依赖。

## 数据库

当前 Flyway 为 V1–V14，共 35 张业务表。已执行迁移不得修改。

- V12：`ai_diao_yong_ri_zhi`
- V13：`ai_cuo_ti_fen_xi`、`ai_hui_hua`、`ai_xiao_xi`
- V14：`ai_mo_xing_pei_zhi`、`ai_sheng_cheng_ren_wu`、`ai_hou_xuan_ti_zhi_liang_ping_jia`、`ai_shi_jue_shang_xia_wen`

完整模型见 [数据库模型](docs/DATABASE_MODEL_V2.md)。

## 管理员 AI 配置

管理员在 `/admin/ai-models` 配置 DeepSeek TEXT 与 GLM VISION，包括 model、base URL、Key、启停、默认配置、超时、重试、max tokens 和连接测试。

本地本科毕设 Demo 模式允许 MySQL 保存 Key。API 不回显完整 Key，页面只显示是否已配置，调用日志不保存 Key。这是本地演示便利设计，不是生产级 KMS。

## 快速启动

### MySQL

准备 MySQL 8.4 和数据库 `rike_tiku`。应用启动时由 Flyway 自动校验并迁移到 V14。正式库与独立验收库 `rike_tiku_demo` 的用途不同，Demo 工具不会接受正式库名。

### Backend

```powershell
cd rike-tiku-backend
$env:RIKE_TIKU_DB_PASSWORD = "<本机 MySQL 密码>"
$env:RIKE_TIKU_JWT_SECRET = "<至少 32 字节的本机随机密钥>"
mvn spring-boot:run
```

后端默认运行于 `http://localhost:8081`，健康检查为 `http://localhost:8081/api/v1/health`。数据库地址、账号和端口可通过 `RIKE_TIKU_DB_HOST`、`RIKE_TIKU_DB_PORT`、`RIKE_TIKU_DB_NAME`、`RIKE_TIKU_DB_USERNAME` 和 `RIKE_TIKU_BACKEND_PORT` 覆盖。

也可以在后端目录运行 `./scripts/setup-idea-local-env.ps1`，按提示为 IDEA 准备本机环境变量。详细说明见 [后端启动说明](rike-tiku-backend/README.md)。

### Frontend

```powershell
cd rike-tiku-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

前端默认运行于 `http://localhost:8080`，默认 API 地址为 `http://localhost:8081/api/v1`。详细说明见 [前端启动说明](rike-tiku-frontend/README.md)。

### 启用真实 AI

可以在管理员后台 `/admin/ai-models` 保存并启用数据库配置，也可以使用 application/env 回退配置。AI 默认关闭，未配置 Key 不影响登录、题库、练习、判分、错题和 STANDARD。

环境变量名与安全默认值见 [AI Provider 配置](docs/AI_PROVIDER_CONFIGURATION.md)。仓库不应保存真实数据库密码、JWT 密钥或 Provider Key。

## 项目结构

```text
rike-tiku-backend/   Spring Boot 模块化单体后端
rike-tiku-frontend/  Vue 3 前端
docs/                当前文档、历史验收与证据索引
题库/                题库素材与导入验证资料
脚本/                清洗、核验等辅助脚本
scripts/             Demo 环境与验收启动脚本
```

文档导航见 [docs/README.md](docs/README.md)。

## 测试与验收

- 后端 `mvn clean test`：173 tests，0 failures，0 errors，3 skipped。跳过项是两个无 Key 的真实 Provider 条件测试和一个 Windows symbolic-link assumption；真实 Provider 已另行单独执行。
- 前端 `npm test -- --run`：58 files、190 tests，全部通过；type-check、build、`npm audit --omit=dev` 均通过，audit 为 0 vulnerabilities。build 保留已知的单个大于 500 kB chunk warning。
- `mvn -DskipTests package` 通过并生成可执行 JAR；随机临时 MySQL 完整执行 14 个迁移并验证 35 张业务表。
- `rike_tiku_demo` 已完成 reset、V1–V14、seed、validate 与 smoke，固定题量 378。机器浏览器覆盖 25 条路由，0 console errors、0 page errors、0 failed requests、0 horizontal overflow routes。
- PR #31 真实 `deepseek-v4-flash` smoke、学生错因、当前题答疑和 1 道候选生成均通过。真实 GLM 没有记为 PASS，详情见 [最终实验事实](docs/AI_FINAL_EXPERIMENT_RESULTS.md)。
- 最终人工验收环境已准备，清单见 [最终人工验收清单](docs/FINAL_MANUAL_ACCEPTANCE_CHECKLIST.md)。

## 最终维护阶段

PR #31 的机器阶段为 `AUTO_FINAL_VERIFICATION_PASS`，并已由用户明确决定 ordinary merge。当前 PR #32 只完成本机正式环境和论文资料包；之后进入维护模式。用户按 25 项清单完成真人验收前，状态始终保持 `FINAL_MANUAL_ACCEPTANCE_PENDING`。
