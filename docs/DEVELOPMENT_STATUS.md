# 开发状态

> 当前实施分支：`feat/demo-question-bank-expansion`（未合并），基线 `main@b40bc79eeb04c40bdf2eb05ef975193e4444eb31`。PR #15 已普通 merge。Flyway 保持 V1–V7（23 张业务表），本轮不新增迁移、不写正式 `rike_tiku`、不修改 MVP30 原始 Excel。

更新时间：2026-08-07

## 当前主线状态

- 当前分支：`feat/demo-question-bank-expansion`，基于 `main@b40bc79eeb04c40bdf2eb05ef975193e4444eb31`；PR #15 已普通 merge，merge commit 为 `12d636fde4afa198edc78eb0c295f5b88c8e3456`。
- 学生自主练习、自动判分、结果与错题闭环已进入 `main`；AI、掌握度、推荐、教师任务、组卷考试和主观题评分仍未实现。
- Flyway：V1–V7，共 23 张业务表；本轮未修改 V1–V7，未新增迁移。

## 已进入 main

- `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`、`cuo_ti_ji_lu`，全部使用 BIGINT 主键、外键 `ON DELETE RESTRICT`、必要唯一键/索引和状态约束。
- 学生只能从 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题创建会话；会话创建后将题目、选项、答案、解析、知识点及顺序冻结。
- 提交整场答案在一个事务中完成锁定、答题事实、错题聚合、最终结果和会话状态切换；重复提交为 `409`。
- 单选严格匹配有效标识；多选以去重后的集合完全相等判分；填空按冻结空位、可接受答案、大小写及受控全半角/标点规则判分。
- 错误创建或累加错题；答对不删除历史，连续正确一次为 `REVIEWING`、两次为 `MASTERED`。
- 学生前端提供 `/student/practice`、`/student/practice/new`、`/student/practice/:id`、`/student/practice/:id/result`、`/student/wrong-questions`；未提交响应和页面均不显示答案或解析。

## 当前验证

- PR #15 合并后后端：`mvn clean test` 79/79 PASS；`mvn clean package` PASS。
- PR #15 合并后前端：`npm test` 72/72 PASS；`npm run type-check`、`npm run build` PASS。构建仅有既有大 chunk 提示。
- PR #15 合并后 `npm audit --omit=dev`：0 vulnerabilities；最终敏感信息扫描通过。
- PR #15 `rike_tiku_demo` 三角色真实浏览器验收：PASS；MA-001 至 MA-005、MA-010、MA-011 已关闭。
- 合并后首次回归发现旧测试仍假定随机题集按 ID 固定排序；已将断言修正为合格题集顺序无关，并重新完成上述 79/79 全量回归。
- 历史 PR #13 结果：真实 HTTP 为 `PASS`，学生页面回查为 `NOT_RUN`，阶段综合结论为 `PASS_WITH_ENV_LIMITATION`。该结论不代表 PR #15 当前状态。

## 已进入 main 的演示验收环境

- 显式 PowerShell 工具创建、重置、播种、校验和清理独立 `rike_tiku_demo`，正常应用启动不会自动写入演示数据。
- 固定三角色演示账号、学生/教师档案、演示班级、三科三元任课关系和九个知识点。
- 当前分支将题库扩充为 Demo90：90 道项目原创自编、无附件、可自动判分的 `PUBLISHED` 演示题；每科 30 道，每科三题型、三档难度、三个演示知识点各 10 道，来源权利状态为 `USER_PROVIDED`，审核轨迹完整。
- 本轮不修改 V1–V7，不写正式 `rike_tiku`，不修改 MVP30 原始 Excel。
- 历史 PR #14 合并后回归：后端 74/74、`mvn clean package` PASS；前端 68/68、类型检查和构建 PASS；`npm audit` 为 0 vulnerabilities。
- 真实脚本链 `reset → seed → validate → clean → reset → seed` PASS，末次 seed 后演示库保持待人工验收状态；正式库演示账号、演示题和 V7 五张学习表均为 0。
- 人工验收问题 MA-001 已关闭：后端 18081、前端 18080、`/api/v1` 基址和 `rike_tiku_demo` 连接均正确；demo_admin 登录、demo_teacher 真实 HTTP 登录和 demo_student 浏览器登录均已复验，原 `INVALID_CREDENTIALS` 不再复现。
- MA-002 至 MA-005 已通过真实浏览器复验并关闭；MA-010、MA-011 也已关闭。MA-006 至 MA-009 尚未完成，其中 MA-006 仅剩个人资料、个人简介和头像能力，主动修改密码已实现。
- Demo90 专项与脚本链 `reset → seed → validate` PASS；三科筛选、随机题集变化、未提交防泄露、提交/结果/错题链路和浏览器抽查 PASS。后端 80/80、前端 72/72，打包、类型检查、构建和生产依赖审计均通过。
- Demo90 不等于 MVP30 正式真实题库；MVP30 仍未正式入库，网络候选题没有因此变为 `PUBLISHED`。

## 已合并基线

- PR #10：教师与三元任课关系，普通 merge `9495ecc`。
- PR #11：管理员题库审核发布，普通 merge `dda66d4`。
- PR #12：管理员 MVP30 题库导入，普通 merge `f499f0c`；MVP30 原始 Excel 仍未确认入库。
- PR #13：学生练习、自动判分、结果与错题闭环，普通 merge `db04fbc`。
- PR #14：本地演示数据与人工验收环境，普通 merge `4ffbcbd`。
- PR #15：UI、统一认证、学生三科工作台与教师任教范围，普通 merge `12d636f`。

## 下一步

唯一候选：管理员学生完整管理与账号恢复（`PLANNED`）。

## 非 AI 工程基础完成门槛

只有以下各项全部完成并验证后，非 AI 工程基础才允许标记为 100%；任一项未满足时不得标记 100%：

- 基础工程与认证；
- 班级、教师和三元任课；
- 学生 Excel 导入与单学生完整管理；
- 基础个人资料、简介、头像；
- 管理员题库 CRUD、导入、审核、发布；
- MVP30 正式可用演示题库；
- 学生三科练习、随机练习、判分、结果、错题；
- 教师基础正式工作台；
- 高频考点；
- 师生私信；
- 非 AI 的基础掌握度与规则推荐；
- 三角色完整浏览器验收；
- 全量自动化与构建通过；
- Git、Flyway、文档和代码状态完全一致。

DeepSeek、GLM、AI Provider、AI 错题分析、AI 对话和 AI 生成题不属于该 100% 门槛。
