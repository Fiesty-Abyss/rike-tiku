# 开发状态

> 当前分支为 `feat/mastery-rule-recommendation`，Base 为 PR #20 合并后的 `main@1055dee567b7afa153750792670fb0bafed1151c`。PR #21 待创建 Draft。Flyway 保持 V1–V9，共 26 张业务表；V1–V9 和 MVP30 原始 Excel 均未修改。

更新时间：2026-08-08

## 当前主线状态

- 当前分支：`feat/mastery-rule-recommendation`；PR #20 已普通 merge，PR #21 尚未合并。
- 登录页当前使用两分钟有效、内存保存、一次性消费的 4 位随机图形验证码；验证码默认隐藏，首次登录操作只展开，第二次才认证。PR #15 滑块仅为历史实现。
- 管理员单学生管理已实现分页筛选、详情与班级历史、事务新增、编辑与启停、事务调班和一次性密码重置；Excel 批量导入入口继续独立保留。
- 学生自主练习、自动判分、结果与错题闭环已进入 `main`；PR #21 当前分支已实现实时知识点掌握度、固定规则推荐和教师班级学情查看。AI、教师任务、组卷考试和主观题评分仍未实现。
- Flyway：V1–V9，共 26 张业务表；V9 新增 `si_xin_hui_hua`、`si_xin_xiao_xi`，V1–V8 未修改。
- 教师工作台已支持按本人 ACTIVE 三元任课关系读取班级、科目、学生名单和高频考点，并支持新增、编辑、启停及排序；学生端按本人有效主班级和学科只读取对应 ACTIVE 高频考点。
- PR #20 已实现受 ACTIVE 三元任课关系和学生当前主班级约束的师生纯文本私信；发送身份取自 JWT，支持会话、未读、已读、7 秒轮询和失效关系历史保留，不含 WebSocket、附件、群聊或管理员审计。
- PR #21 掌握度只统计本人已提交、已判分的单选/多选/填空答题，实时关联冻结知识点快照；NEW/REVIEWING 错题阻止“已掌握”，MASTERED 错题不阻止。推荐按活动错题、低正确率、巩固中、样本不足、未练习依次排序，最多 3 项，不属于 AI。

## 已进入 main

- `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`、`cuo_ti_ji_lu`，全部使用 BIGINT 主键、外键 `ON DELETE RESTRICT`、必要唯一键/索引和状态约束。
- 学生只能从 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题创建会话；会话创建后将题目、选项、答案、解析、知识点及顺序冻结。
- 提交整场答案在一个事务中完成锁定、答题事实、错题聚合、最终结果和会话状态切换；重复提交为 `409`。
- 单选严格匹配有效标识；多选以去重后的集合完全相等判分；填空按冻结空位、可接受答案、大小写及受控全半角/标点规则判分。
- 错误创建或累加错题；答对不删除历史，连续正确一次为 `REVIEWING`、两次为 `MASTERED`。
- 学生前端提供 `/student/practice`、`/student/practice/new`、`/student/practice/:id`、`/student/practice/:id/result`、`/student/wrong-questions`；未提交响应和页面均不显示答案或解析。

## 当前验证

- PR #21 当前分支后端：学习掌握专项 4/4 PASS；`mvn clean test` 96/96 PASS；`mvn clean package` 96/96 PASS。
- PR #21 当前分支前端：`npm test` 29 文件、106/106 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #21 浏览器 PASS（仅 `rike_tiku_demo`）：199 学生从零记录真实完成 15 道物理题，形成牛顿运动定律 0% 薄弱、电场强度 60% 巩固中、温度和内能 100% 已掌握；推荐预选学科、知识点和 5 题正确。199 物理教师看到该生 15 题、53.3%、薄弱 1、已掌握 1，200 班无混入；生物/化学教师仅见本人两班对应学科；控制台 0 error。验收记录随后由 reset/seed 清理。

- PR #20 合并后后端：`mvn clean test` 92/92 PASS；`mvn clean package` 92/92 PASS 并生成可执行 JAR；私信、权限与 V9 数据库专项 PASS。
- PR #20 合并后前端：`npm test` 25 文件、100/100 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #20 浏览器 PASS（仅 `rike_tiku_demo`）：199 学生↔物理教师、200 学生↔化学教师双向收发，未读、教师工作台范围、伪造 conversationId 拒绝和控制台 0 error 均通过。
- PR #19 合并后后端：`mvn clean test` 90/90 PASS；`mvn clean package` 90/90 PASS 并生成可执行 JAR。
- PR #19 合并后前端：`npm test` 91/91 PASS；type-check、build PASS，`npm audit --omit=dev` 为 0 vulnerabilities。
- PR #19 浏览器验收保持 PASS（仅 `rike_tiku_demo`）：验证码默认隐藏、错误后中文提示并自动换图、图片/文字刷新、三个单角色直达、多角色选择、退出重登均通过；控制台 0 error。
- PR #18 合并后后端：`mvn clean test` 87/87 PASS；`mvn clean package` 87/87 PASS，并成功生成可执行 JAR。
- PR #18 合并后前端：`npm test` 83/83 PASS；`npm run type-check` PASS；`npm run build` PASS（保留既有大 chunk 提示）；`npm audit --omit=dev` 为 0 vulnerabilities。
- `rike_tiku_demo`：`reset → seed → validate → smoke` PASS；V1–V9、26 张业务表，固定状态为 14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 任课关系、Demo90 和 12 条 ACTIVE 高频考点；固定 seed 不预置私信。
- PR #19 历史阶段 Demo `reset → seed → validate → smoke` PASS；当时正式 `rike_tiku` 的 Demo90、场景账号、场景班级、高频考点均为 0。当前正式库基线已为 Flyway V9。
- PR #18 真实浏览器 PASS（仅 `rike_tiku_demo`）：物理教师进入 199/200 工作台，查看 5/3 名学生，新增、编辑、停用、启用高频考点；生物教师仅见 199/200 生物，化学教师仅见 199/200 化学；199/200 学生分别只读取本班物理 ACTIVE 考点；控制台 error 日志为空。临时验收考点已由 reset/seed 清理。
- 合并前门禁使用全新随机临时库完整迁移 V1–V9；临时库与正式 `rike_tiku` 的 V9 script 均为 `V9__create_teacher_student_message_tables.sql`、checksum 均为 `1192958817`、success 均为 1。MA-013 导致正式库提前执行的 V9 现已与 main 正式基线一致，两张结构表不再描述为业务数据污染；测试隔离修复保持有效，MA-013 已关闭。
- 正式 `rike_tiku` 合并后只读检查：Flyway V9、26 张业务表；Demo90、场景账号、场景班级、高频考点、私信会话、私信消息均为 0。
- MA-001 至 MA-005、MA-007 至 MA-013 已关闭；MA-006 仅剩个人资料、简介和头像。
- 历史结果：PR #13 为后端 68/68、前端 68/68；PR #14 为后端 74/74、前端 68/68；PR #15 为后端 79/79、前端 72/72；PR #16 为后端 80/80；PR #17 为后端 86/86、前端 80/80。

## 已进入 main 的演示验收环境

- 显式 PowerShell 工具创建、重置、播种、校验和清理独立 `rike_tiku_demo`，正常应用启动不会自动写入演示数据。
- 保留原三角色 smoke 账号和 `DEMO_CLASS_01`，并新增 199/200 两班、三位场景教师和八名固定场景学生；共 14 账号、3 班级、4 教师、9 学生、9 条 ACTIVE 三元任课关系。
- PR #16 已将题库扩充为 Demo90：90 道项目原创自编、无附件、可自动判分的 `PUBLISHED` 演示题；每科 30 道，每科三题型、三档难度、三个演示知识点各 10 道，来源权利状态为 `USER_PROVIDED`，审核轨迹完整。可见化学符号和科学计数法使用稳定 Unicode，STANDARD 解析不包含演示操作说明。
- 本轮不修改 V1–V7，不写正式 `rike_tiku`，不修改 MVP30 原始 Excel。
- 历史 PR #14 合并后回归：后端 74/74、`mvn clean package` PASS；前端 68/68、类型检查和构建 PASS；`npm audit` 为 0 vulnerabilities。
- 真实脚本链 `reset → seed → validate → clean → reset → seed` PASS，末次 seed 后演示库保持待人工验收状态；正式库演示账号、演示题和 V7 五张学习表均为 0。
- 人工验收问题 MA-001 已关闭：后端 18081、前端 18080、`/api/v1` 基址和 `rike_tiku_demo` 连接均正确；demo_admin 登录、demo_teacher 真实 HTTP 登录和 demo_student 浏览器登录均已复验，原 `INVALID_CREDENTIALS` 不再复现。
- MA-002 至 MA-005、MA-007 至 MA-013 已通过真实浏览器或专项复验并关闭。MA-006 尚未完成，仅剩个人资料、个人简介和头像能力。
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

## 下一步

完成 PR #21 的 Draft 发布并停止等待独立审查；不得自行 merge，不开始 PR #22 或任何大模型功能。

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
