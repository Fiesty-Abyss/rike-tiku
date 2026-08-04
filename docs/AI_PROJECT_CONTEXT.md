# 跨AI项目上下文

更新时间：2026-08-04

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

## 5. 当前实现状态

- 状态：题库核心数据库模型为 `DONE_VERIFIED`；账号与教学组织模型尚未开始。
- 当前Git基线：`main`，HEAD `398b651`。
- 当前Flyway：V1–V4。
- 当前业务表：`ke_mu`、`zhi_shi_dian`、`dao_ru_pi_ci`、`ti_mu`、`ti_mu_xuan_xiang`、`ti_mu_jie_xi`、`ti_mu_zhi_shi_dian`、`ti_mu_fu_jian`、`ti_mu_lai_yuan`、`ti_mu_shen_he_ji_lu`。
- 当前样本：物理、化学、生物各1题，共3题，均为 `PENDING`；权利状态为 `COPYRIGHT_UNKNOWN`。
- 最近验证：后端8项测试、Maven测试与打包、JAR健康检查、MySQL连接均通过；前端基础工程的启动、类型检查、构建和浏览器联调在基础轮通过。
- 远程仓库目标：`https://github.com/Fiesty-Abyss/rike-tiku`。

未完成的核心模块包括账号与教学组织、认证授权、学生导入、题库业务API、练习判分、错题、AI Provider和正式角色工作台。

## 6. AI接管规则

- 接管时先读取本文件、`DEVELOPMENT_STATUS.md`、`AI_HANDOFF.md`、最新数据库文档、全部Flyway迁移、测试、Git提交和分支状态。
- 不修改已经执行的Flyway迁移；数据库变化必须新增迁移。
- 不把计划描述为已实现，不扩展到九科，不创建自由注册或邀请码。
- 不把教师—班级与教师—科目拆成两个独立权限关系。
- 不提前开发课堂WebSocket，不直接发布AI候选题，不让AI覆盖标准解析。
- 题库资料只作学习、开发和审核候选；未经版权和学科人工核验不得改为 `PUBLISHED`。
- 每轮结束更新开发状态和AI交接，并给出下一轮唯一任务。
