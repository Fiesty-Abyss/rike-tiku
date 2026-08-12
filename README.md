# RIKE 理科学习辅助系统

面向高中物理、化学、生物的 Spring Boot 大模型题库系统。系统把题库、练习判分、STANDARD 解析、错题学习、教师教学管理和受控 AI 能力放在同一条可追溯业务链中。

> 论文题目：面向高中物化生的 Spring Boot 大模型题库系统设计与实现

## 系统效果

| 公共入口 | 学生学习 |
|---|---|
| <img src="docs/evidence/thesis-final/01-portal-desktop.png" width="520" alt="RIKE Portal"> | <img src="docs/evidence/thesis-final/03-student-dashboard.png" width="520" alt="学生工作台"> |
| <img src="docs/evidence/thesis-final/02-login.png" width="520" alt="登录与密码恢复入口"> | <img src="docs/evidence/thesis-final/05-result-standard.png" width="520" alt="练习结果与 STANDARD"> |

| 学生 AI | 教师与管理员 |
|---|---|
| <img src="docs/evidence/thesis-final/07-student-ai-analysis.png" width="520" alt="AI 错因分析"> | <img src="docs/evidence/thesis-final/11-teacher-workspace.png" width="520" alt="教师工作台"> |
| <img src="docs/evidence/thesis-final/08-student-ai-chat.png" width="520" alt="当前题答疑"> | <img src="docs/evidence/thesis-final/13-teacher-paper-builder.png" width="520" alt="教师组卷"> |
| <img src="docs/evidence/thesis-final/09-student-ai-variant.png" width="520" alt="学生 AI 变式练习"> | <img src="docs/evidence/thesis-final/17-admin-ai-models.png" width="520" alt="AI 模型管理"> |

完整截图含练习、错题、变式结果、教师审核、学生版/答案版试卷、密码恢复通知和移动端，见 [论文插图目录](docs/evidence/thesis-final/README.md)。

## 核心闭环

```mermaid
flowchart LR
    Q[已发布题目] --> P[在线练习]
    P --> G[确定性判分]
    G --> S[STANDARD 解析]
    G --> W[错题与掌握度]
    W --> A[AI 错因与当前题答疑]
    A --> V[结构化 AI 变式]
    V --> R[教师或管理员审核]
    R -->|通过| Q
```

STANDARD 是正式答案事实。AI 可以解释、生成候选题和提供受控网络来源，但不能改写判分结果或 STANDARD。学生生成的变式题初始为 `PENDING`，人工审核通过后才能发布。

## 已实现能力

### 学生

- 三科学科练习、单选/多选/填空确定性判分、STANDARD 解析。
- 错题本、掌握度、高频考点和师生消息。
- 安全科学内容渲染：段落、列表、粗体、行内代码、KaTeX，拒绝 raw HTML。
- 当前题最多 10 轮答疑，可选择 DeepSeek V4 Flash/Pro、标准回答或深度思考。
- 可选智谱官方 Web Search；只返回 3—5 条受控来源，网络摘要按 `UNTRUSTED_WEB_CONTEXT` 处理。
- 每次生成 1 道结构化 AI 变式，支持单选、多选和填空；作答后才显示 AI 解析，可换题、丢弃或提交人工审核。

### 教师

- 按 ACTIVE 任课关系访问班级、学科、学生学习情况与高频考点。
- 生成和审核 AI 候选题，候选题不能绕过 `PENDING` 直接发布。
- 手动组卷支持已发布题筛选、题篮、顺序和分值；规则组卷支持题型、难度、知识点、数量与目标总分。
- 试卷保存后冻结，可预览学生版和答案解析版，并通过 `window.print()` 打印或另存为 PDF。

### 管理员

- 学生、教师、班级、任课、题库、附件、Excel 导入和操作日志管理。
- DeepSeek TEXT、GLM VISION、智谱 SEARCH 独立配置，Key 只显示“已配置”。
- 连接测试返回安全错误分类、HTTP 状态、延迟与时间，不返回 Key、请求正文或原始响应。
- 接收匿名密码恢复通知，使用现有验证码和统一外部响应；恢复默认密码或驳回均有事务、首次改密和审计记录。

## 技术架构

- 后端：Java 25、Spring Boot 4.1、Spring MVC、Spring Security、JWT、MyBatis-Plus、Flyway、MySQL 8.4。
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、GSAP、KaTeX。
- AI：DeepSeek Chat Completions、GLM-4.6V-Flash、智谱 Web Search、Fake/Mock Provider。
- 形态：前后端分离的模块化单体。Redis、MQ、WebSocket、向量数据库和微服务不是运行依赖。

```text
Vue 3 SPA
   │ HTTP JSON / JWT
Spring Boot 模块化单体
   ├─ 认证、组织、题库、练习、消息、试卷
   ├─ 学生 AI、候选生成、人工审核
   └─ Provider / Vision / Search 边界
   │
MySQL 8.4 · Flyway V1–V19 · 39 张业务表
```

## AI 安全边界

- 学生只能选择管理员提供的安全模型 ID，不能提交任意 model、Base URL 或 endpoint。
- 标准回答映射 `thinking.disabled`；深度思考映射 `thinking.enabled + reasoning_effort=max`。
- `reasoning_content` 不展示、不持久化、不写日志；业务表只保存最终回答。
- 搜索只调用结构化官方接口，不抓取网页；拒绝 localhost、内网地址、非 HTTP(S) 协议和非法 URL。
- AI 调用日志只保存 provider、model、purpose、业务引用、状态、延迟、Token 与安全错误码。
- 图片、Prompt、完整学生答案、Authorization、Key 和思维链不进入调用日志。

## 数据库

Flyway V1–V14 是既有历史，本轮未修改。V15–V19 依次完成 10 轮约束、学生运行时选择与 SEARCH 用途、密码恢复、教师试卷和学生 AI 变式实例。

- [V19 字段与约束参考](docs/DATABASE_SCHEMA_REFERENCE.md)
- [V19 纯结构快照](database/schema_snapshot_v19.sql)（0 条 `INSERT`）
- [数据库模块与 ER 图](database/diagrams/rike_tiku_er.md)
- [SQL 示例](docs/SQL_EXAMPLES.md)

## Excel 导入

- [学生导入模板](docs/templates/student-import-template.xlsx)
- [题目导入模板](docs/templates/question-import-template.xlsx)
- [预览、确认与字段说明](docs/EXCEL_IMPORT_GUIDE.md)

两类导入均先预检查，再由用户确认入库。题目导入进入审核状态，不因 Excel 上传直接发布。

## 快速启动

```powershell
cd rike-tiku-backend
$env:RIKE_TIKU_DB_PASSWORD = "<本机 MySQL 密码>"
$env:RIKE_TIKU_JWT_SECRET = "<至少 32 字节随机密钥>"
mvn spring-boot:run
```

```powershell
cd rike-tiku-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

默认前端 `http://localhost:8080`，后端 `http://localhost:8081`。AI 未配置时，登录、题库、练习、判分、错题和 STANDARD 仍可使用。

## 测试与论文资料

- 随机临时 MySQL 从空库执行 V1→V19；集成测试不使用正式 `rike_tiku`。
- 匿名验收库 `rike_tiku_demo` 已执行 reset、19 个迁移、seed 与 validate，保持 378 道 PUBLISHED 题基线。
- 前端专项覆盖科学内容、10 轮控制、模型/思考/搜索、变式题、密码恢复、组卷、试卷预览、Portal 和 AI 模型管理。
- 真实 Provider 只有本机已配置 Key 时才执行一次受控 smoke；缺 Key 记为 `NOT_RUN`，不伪造 PASS。

论文写作从 [资料中心](docs/THESIS_WRITING_HUB.md) 开始，功能、截图、代码、API、表与 Flyway 的完整映射见 [FEATURE_SCREENSHOT_CODE_INDEX](docs/FEATURE_SCREENSHOT_CODE_INDEX.md)，论文事实稿见 [RIKE_THESIS_DRAFT](docs/thesis/RIKE_THESIS_DRAFT.md)。
