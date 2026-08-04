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
```

Flyway 是数据库结构的唯一建表和升级入口。已经执行的迁移不得修改，后续结构变化必须新增版本迁移。

## 当前进度

已经完成并验证：

- 前后端基础工程、健康检查、真实数据库连接和开发环境CORS联调；
- 题库核心数据库V1版，Flyway V1–V4；
- 10张题库业务表；
- 物理、化学、生物各1道 `PENDING` 最小真实样本；
- 最小MyBatis-Plus读取映射和数据库自动化测试。

尚未完成：账号与教学组织模型、登录/JWT、学生导入、题库正式业务API、练习判分、错题、AI Provider和正式角色工作台。题库30题候选数据尚未正式发布。

准确状态请以 [开发状态](docs/DEVELOPMENT_STATUS.md) 和 [AI交接](docs/AI_HANDOFF.md) 为准。

## 本地启动

### 后端

复制环境变量示例并通过终端或IDEA Run Configuration设置真实本机值，不要提交本地配置：

```powershell
$env:RIKE_TIKU_DB_HOST="localhost"
$env:RIKE_TIKU_DB_PORT="3306"
$env:RIKE_TIKU_DB_NAME="rike_tiku"
$env:RIKE_TIKU_DB_USERNAME="root"
$env:RIKE_TIKU_DB_PASSWORD="your-local-password"
cd rike-tiku-backend
mvn spring-boot:run
```

默认后端地址为 `http://localhost:8081`，健康接口为 `http://localhost:8081/api/v1/health`。

### 前端

```powershell
cd rike-tiku-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

默认前端地址为 `http://localhost:8080`。前端API地址通过 `VITE_API_BASE_URL` 配置。

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
- V3.0总体设计公开脱敏版（位于 `docs/`）

## 题库资料和权利说明

`题库/` 中的Excel、JSON、图片、公式对象、Word和PDF资料仅作为本科毕业设计的学习、开发与人工审核候选材料。当前样本保持 `PENDING`，来源权利状态保持 `COPYRIGHT_UNKNOWN`；仓库不声明这些资料已经获得公开传播授权。使用者应在正式发布前完成学科质量和版权核验。

仓库不得包含数据库真实密码、API Key、JWT密钥、访问令牌、学生手机号、身份证、家庭住址或真实成绩等敏感信息。

## 下一阶段

下一阶段只实现账号、角色、档案、班级及教师—班级—科目三元任课关系数据库模型，不提前开发登录、JWT、练习、前端页面或AI接口。
