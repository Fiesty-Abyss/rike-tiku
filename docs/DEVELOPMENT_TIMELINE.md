# RIKE 开发时间线

本时间线来自仓库 Git 与 GitHub PR；只按工程阶段归纳，不把每个小提交展开为流水账。测试数字引用当期已记录事实，后续修复不篡改历史阶段结论。

| 阶段 | 代表 PR / merge commit | 完成功能 | Flyway / 测试与问题解决 |
|---|---|---|---|
| 架构和数据库基础 | PR #1 `5d4aa1b`、PR #2 `b75ace4`、PR #3 `74af933` | 用户、角色、教学组织模型与本机启动约定 | 建立 Flyway 基线；解决 IDEA 本机数据库启动可靠性 |
| 认证与账户 | PR #4 `9783435`、PR #5 `b519134`、PR #19 `0a12943` | JWT、Spring Security、前端认证、随机图形 CAPTCHA | 后端认证集成与前端登录专项；验证码一次性和角色边界 |
| 教学组织 | PR #6 `02646fa` 至 PR #10 `9495ecc`、PR #17 `3e5454d` | 班级、学生导入、教师及任课三元关系 | preview/confirm 与事务；教师授权不依赖前端或姓名 |
| 题库导入审核 | PR #11 `dda66d4`、PR #12 `f499f0c`、PR #26 `b992bff` | DRAFT/PENDING/PUBLISHED、Excel 导入、附件安全渲染 | 内容 hash、审核轨迹、MIME/hash/鉴权读取 |
| 学生练习 | PR #13 `db04fbc`、PR #14 `4ffbcbd` | 冻结练习、自动判分、结果、STANDARD 与可重复 Demo | 正式答题事实和答案防提前泄漏 |
| 教师与学习闭环 | PR #18 `b615bc1`、PR #20 `1055dee`、PR #21 `c0d6553`、PR #22 `67b7bd7` | 教师工作台、高频考点、私信、掌握度/推荐、个人中心 | 三元任课授权、消息所有权、确定性规则推荐 |
| 非 AI 封板 | PR #23 `3677c76` 至 PR #27 `84a82fc` | 378 题 Demo、Portal、附件、操作日志和三角色 RIKE UI | 非 AI 主链 `DONE_VERIFIED`；历史证据保留 |
| AI Provider | PR #28 `54c1669` | Fake/DeepSeek、超时、有限重试、受控错误、V12 脱敏日志 | 无真实 Key 自动化；网络/429/5xx 最多重试一次 |
| 学生 AI | PR #29 `d04e5dc` | V13 错因分析、成功缓存、当前题 8 轮答疑、所有权和身份 guard | 真实 DeepSeek smoke PASS；修复错题 REVIEWING/MASTERED 仍绑定最近错误事实 |
| GLM 与候选题 | PR #30 `d67ebc8` | V14 模型配置、GLM 视觉上下文、候选生成、去重、质量评价、教师/管理员审核 | 候选批次 `TransactionTemplate` 原子回滚；补齐教师前端闭环 |
| 最终集成 | PR #31 `c79b7a6` | 全量回归、Demo、真实 Provider、权限/降级、机器浏览器与验收材料 | 后端 173、前端 190；`AUTO_FINAL_VERIFICATION_PASS`；人工验收仍 PENDING |
| 本地正式化与论文资料包 | PR #32（当前） | 正式库备份和 V11→V14、正式匿名化边界、Excel/DDL/SQL/截图/文献资料 | 不新增 V15 或业务功能；进入维护和论文写作模式 |

## 当前封板口径

- 正式题目：面向高中物化生的 Spring Boot 大模型题库系统设计与实现。
- 产品：RIKE 理科学习辅助系统。
- 架构：前后端分离模块化单体；MySQL 8.4；Flyway V1–V14、35 表。
- 机器状态：`AUTO_FINAL_VERIFICATION_PASS`。
- 人工状态：`FINAL_MANUAL_ACCEPTANCE_PENDING`。
