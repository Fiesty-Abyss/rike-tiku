# 开发状态

更新时间：2026-08-16

## 当前状态

> PR #33 学生端第三轮最终集中修复（2026-08-16）：当前唯一交付分支为 `feat/final-product-completion`，本轮收口提交 `54fc43f` 已普通提交，PR #33 仍为 Draft / OPEN / 未合并。Flyway 为 V1–V29、50 张业务表；本轮补齐显式科学公式/化学式渲染、新颖度分层与专题内容扩充，并收掉错题筛选的运行时 warning。正式 `rike_tiku` 当前为 V29/50，15 个已发布专题单元、45 条单元题目关系、65 张已发布卡片；人工验收仍为 `FINAL_USER_REVIEW_PENDING`。

最终门禁：后端全量 215 tests（0 failures、0 errors、3 skipped），`mvn clean test` 和 package 通过；前端全量 68 files / 220 tests、type-check、build、`npm audit --omit=dev` 通过，audit 为 0 vulnerabilities。本轮新颖度单测 3/3、候选生成集成 7/7、学生变式集成 4/4、学生前端专项 5 files / 12 tests 均通过；科学内容审计 600 strings / 105 database rows / 0 errors。Demo 浏览器 4 routes 无 console/page/failed-request error、无 overflow；正式浏览器因本机无轮换正式学生凭据为 `BLOCKED_LOCAL_CREDENTIAL`。09/10 变式截图明确是确定性 UI 夹具，不是 Provider PASS。真实 Provider 因没有可安全使用的轮换后凭据而分别记为 `BLOCKED_EXTERNAL_PROVIDER`。

正式 `rike_tiku` 的迁移前真实版本为 V24/41 表。仓库外备份为 1,326,218 bytes，SHA-256 `039C9E885007EB79ED317E1A1E5C5A6DCEB7EC2746C0777957E41E60FE65E622`。首次迁移揭示 V25 与历史 `NUMERIC_CONDITION` 的约束兼容缺陷；从已校验备份恢复到 V24 后，由版本门禁 Flyway callback 将该旧枚举语义映射为 `CONDITION_RECOMBINATION`，随后 V25–V29 正常迁移。当前为 V29/50 表、0 failed migration；9 用户、389 题、378 PUBLISHED 保持，另有 65 张已发布高频考点卡片和 15 个已发布专题单元、45 条单元题目关系；本轮通过受控内容脚本写入学生展示内容，没有 reset/seed/迁移。

历史记录：PR #31 已由用户明确决定 ordinary merge，merge commit 为 `c79b7a6f93e32509989282995419bbaf64666182`；此前 PR #32 `chore/final-local-production-thesis-package` 曾记录正式库由 V11 正规迁移至 V14。本段不代表当前版本，当前版本以本节首段的 PR #33、V29/50 和最终门禁记录为准。

| 范围 | 状态 |
| --- | --- |
| 非 AI 主链 | `DONE_VERIFIED` |
| Provider Core | `DONE_VERIFIED` |
| 学生错因分析与当前题答疑 | `DONE_VERIFIED` |
| 管理员 AI 模型配置 | `DONE_VERIFIED` |
| 候选题生成、PENDING 审核与质量评价 | `DONE_VERIFIED` |
| Vision 实现 | `DONE_VERIFIED` |
| 真实 DeepSeek variant / tutor | `BLOCKED_EXTERNAL_PROVIDER`（本轮 Key ABSENT，未调用） |
| 真实 GLM Vision | `BLOCKED_EXTERNAL_PROVIDER`（本轮 Key ABSENT，未调用） |
| 真实 xAI Vision / Web Search | `BLOCKED_EXTERNAL_PROVIDER`（本轮未调用） |
| 最终机器集成 | `DONE_VERIFIED` |
| 最终用户人工验收 | `FINAL_MANUAL_ACCEPTANCE_PENDING` |

- 架构保持前后端分离的模块化单体，不使用微服务。
- Flyway 为 V1–V29，共 50 张业务表；V1–V29 均为已执行迁移，不得修改。
- 正式答案与 STANDARD 标准解析是权威事实，AI 不能覆盖。
- DeepSeek 负责文本推理，GLM-4.6V-Flash 只提供受控 `UNTRUSTED_VISION_CONTEXT`。
- 学生端统一显示“RIKE 理科学习助手”，不显示 Provider、模型代码、API 地址、Key 或 Token。
- 学生端错题筛选、再做确认、专题单元、高频考点与 AI 候选私有边界已在本轮集中修复；正式浏览器证据位于 `docs/evidence/pr33-formal-student/`。
- PR #31 已完成全量自动化、Demo、真实 DeepSeek、权限与降级、机器浏览器和文档统一。用户人工验收尚未执行，不能写为最终封板 PASS。

## PR #33 本轮学生端收口

- 代码与测试提交 `469fe04` 已推送到 `feat/final-product-completion`；未创建 PR #34，未 force push、rebase、squash 或合并 PR #33。
- 正式库使用 v2 科学内容源和 guard + 事务幂等脚本写入结构化高频考点，并扩充为 15 个三题专题单元；第二次执行 `CARDS_INSERTED=0`、`UNITS_CREATED=0`、`QUESTIONS_CREATED=0`，证明没有重复插入。
- 正式浏览器本轮在 8080/8081 接口登录时被本机正式学生账号返回 `INVALID_CREDENTIALS`，状态记为 `BLOCKED_LOCAL_CREDENTIAL`；Demo 18080/18081 独立 Chromium profile 4 routes 全部通过，证据只代表 Demo 机器巡检。
- 最终集中回归已完成：后端 215 tests / 3 skipped，前端 68 files / 220 tests，type-check、build、npm audit 通过；仅保留构建的大 chunk warning 和无 Provider Key 的条件跳过。
- 正式只读校验为 Flyway V29、50 张业务表、15 个已发布专题单元、45 条单元题目关系、65 张已发布卡片；数据库中没有残留 `rike_tiku_` 临时测试 schema。

## PR #32 本地正式化与资料包

- 正式库经仓库外备份后由 V11/27 表正规迁移至 V14/35 表；最终保留 378 道 PUBLISHED 题、3 位教师、6 位学生、2 个班级和 6 条 ACTIVE 任课关系，机器事务事实已清空。
- 9 个正式账号均使用 BCrypt，恢复统一初始密码状态并启用首次改密；真实姓名、账号清单、备份与 AI Key 均只存在本机受控边界，Git 精确扫描为 0 命中。
- 后端导入模板集成测试 1/1 PASS；`mvn -DskipTests package`、前端 type-check/build、`npm audit --omit=dev` 与 `git diff --check` 均通过，audit 为 0 vulnerabilities；build 保留大于 500 kB chunk 的已知 warning。
- 真实 IDEA `RikeTikuBackendApplication` 与 WebStorm `RIKE Frontend` 已分别点击 Run；四种 Run/Stop 顺序验证 8081/8080、health 200、CORS、Flyway V14 与正式库连接。旧 RIKE Node/Java orphan 是端口冲突根因，新增的受控脚本只按仓库路径和进程特征回收 RIKE 端口，拒绝停止无关进程。
- 论文资料包含写作入口、功能—代码—技术映射、导入指南及可验证 XLSX、35 表结构参考、纯结构快照、SQL 示例、匿名截图索引、开发时间线、中期材料草稿和已核验参考文献。
- 独立临时 Profile 的真实 Chrome 通过 CDP/Playwright 完成固定 URL 操作者巡检；代表性正式账号覆盖多角色、单角色教师和两个班级学生主链，1440/390 无溢出，console/page/request error 为 0，结论 `PASS_WITH_NOTES`，BLOCKER/HIGH 为 0。Docker 因本机环境不可用记为 `SKIPPED_DOCKER_ENVIRONMENT`；Redis 明确为 `NOT ADOPTED BY DESIGN`。
- 管理员学生/教师管理新增单人及批量“恢复默认密码”：配置化默认值、逐账号独立 BCrypt、首次改密、no-store、操作日志脱敏与批量原子回滚均由专项验证；新建/导入账号继续使用随机初始密码。真实 WebStorm 已使用 npm `run dev` 配置启动，禁止直接以 Node 执行 `src/main.ts`。
- 用户选择自行完成新恢复界面的真实 CAPTCHA 人工验收；该部分没有被自动化冒充为人工 PASS，PR #32 保持 Draft / OPEN。

## PR #31 机器验证

- 后端 `mvn clean test` 为 173 tests、0 failures、0 errors、3 skipped。两个真实 Provider 测试因全量阶段未注入 Key而按条件跳过，另一个是 Windows symbolic-link assumption；真实 Provider 另行单独执行。
- 前端全量为 58 files、190 tests、0 failures；type-check、build 和 `npm audit --omit=dev` 通过，audit 为 0 vulnerabilities。build 保留大于 500 kB chunk 的已知 warning。
- 随机临时 MySQL 完整迁移 V1–V14，验证 35 张业务表；`mvn -DskipTests package` 生成可执行 JAR。
- `rike_tiku_demo` 已完成 reset、seed、validate 和 smoke。三科各 120 道 Demo360、每科 6 道 Topic18，总计 378 道；PHYSICS-S1 图片和 hash 正常。
- 真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped。文本 1008 ms、22/11 Token；结构化 JSON 1722 ms、400/110 Token，Parser 与 V12 脱敏通过。
- Demo 真实学生错因分析为 `CONCEPT_ERROR`，2911 ms，五字段齐全；第二次读取 5 ms 命中缓存且没有新增收费调用。当前题真实答疑 3014 ms；身份、无关请求与修改 STANDARD 由代码 guard 控制。
- 真实 DeepSeek 生成 1 道候选，2318 ms，只进入 PENDING；机器审核验证 APPROVED 状态机，不计作用户人工质量评价。
- 真实 GLM 第一次窗口为 429；第二个最终窗口返回完整 JSON 代码围栏，旧 Parser 拒绝。Parser 已按严格完整围栏规则修复并通过全量自动化，遵守两次窗口上限未第三次调用，状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`。
- 权限专项确认学生所有权、教师跨科、三角色路由和学生私聊边界；未提交练习响应不含 correctAnswer 或 standardAnalysis。
- 机器浏览器覆盖 25 条路由，0 console errors、0 page errors、0 failed requests、0 horizontal overflow routes。证据见 [PR #31 最终机器证据](evidence/pr31-final/README.md)。
- PR #31 当时对正式 `rike_tiku` 仅只读核对。PR #32 经用户明确授权，已先完成仓库外备份，再通过原有 Flyway 将其由 V11/27 表迁移至 V14/35 表，并置入 378 道最终题库和最小正式组织基线；这不改变 PR #31 的历史审计事实。

## PR #30 验证

主实现专项结果如下。

- 后端 54/54 PASS。
- 前端 12/12 PASS。
- V14 随机临时 MySQL 迁移 PASS。
- `mvn -DskipTests package` PASS。
- `npm run type-check` PASS。
- `npm run build` PASS。

集中修正结果如下。

- 后端 38/38 PASS。
- 前端 32/32 PASS。
- 候选 Provider、Parser 与 prepare 保持在事务外。
- 同批候选题创建、母题关联、DRAFT 至 PENDING、质量评价行和任务 SUCCESS 使用同一个 `TransactionTemplate` 事务。
- 确定性第二候选写入故障验证整批回滚。任务保留为 FAILED，生成数、质量行、关联候选与 PENDING 候选均为 0，母题和 STANDARD 不变。
- 教师端 `/teacher/ai-generation` 只调用教师 API，并继续受本人 ACTIVE 三元任课关系约束。
- package、type-check、build 与 `git diff --check` PASS。
- 未运行 PR #31 全量、Demo reset/seed、全站机器浏览器或用户人工验收。

PR #30 的真实 GLM 历史结果为 HTTP 429。本文件顶部的 PR #31 结果是后续事实，不覆盖这条历史记录。

## 当前业务能力

非 AI 主链已经覆盖认证、三角色权限、班级与任课、题库导入审核、附件、练习、自动判分、结果、错题、掌握度、规则推荐、高频考点、师生消息、个人中心、操作日志和 RIKE Aqua Future 三角色界面。

AI 主链已经覆盖统一 Provider、Fake/Stub、DeepSeek、脱敏调用日志、学生错因分析、当前题有限多轮答疑、管理员模型配置、GLM 视觉上下文、候选变式题生成、重复控制、PENDING、教师或管理员人工审核和五项质量评价。

更细的接口与数据边界见 [文档索引](README.md)。

## 历史阶段

### PR #29 学生 AI 学习主链

- ordinary merge commit 为 `d04e5dcf9639182303e26e38ccfa4351ad91c5d9`。
- V13 新增 `ai_cuo_ti_fen_xi`、`ai_hui_hua`、`ai_xiao_xi`。
- 错因分析绑定本人已提交正式答题事实；错题进入 REVIEWING 或 MASTERED 后仍查询最近一次错误正式事实。
- 分析使用八类受控错误和五字段严格 JSON，首次无效最多纠正一次。
- 当前题答疑最多 8 轮，单条用户消息 500 字，最近 12 条消息和 6000 字上下文预算。
- 真实 `deepseek-v4-flash` smoke 为 1/1 PASS、0 skipped。文本调用 848 ms，输入/输出 Token 22/11；结构化分析 1670 ms，输入/输出 Token 400/132，JSON 调用 1 次；Parser、V12 日志脱敏、V13 SUCCESS 和 STANDARD 不变均通过。

### PR #28 Provider Core

- ordinary merge commit 为 `54c1669b3113086a2fb22e756e0656ea8cb751c8`。
- V12 新增 `ai_diao_yong_ri_zhi`。
- 已实现统一 Provider 契约、确定性 Fake、DeepSeek HTTP Provider、连接与请求超时、最多一次临时失败重试、受控错误和安全降级。
- 自动化不需要真实 Key，AI 默认关闭不影响非 AI 主链。

### PR #27 非 AI 最终封板

- ordinary merge commit 为 `84a82fc3bd4972fc11c0811d8332bae306b7e5c0`。
- MA-017 至 MA-026 已关闭，非 AI A 层为 `DONE_VERIFIED`。
- V11 新增管理员操作日志，Demo360、Topic18、附件、Golden30 导入闭环与 RIKE Aqua Future 的测试和验收记录均保留为历史证据。
- 原始审计过程和各轮机器、人工结论见 [V3.0 非 AI 完工审计](V3_NON_AI_COMPLETION_AUDIT.md)、[人工验收发现](MANUAL_ACCEPTANCE_FINDINGS.md) 与 [历史证据目录](evidence/)。

## 当前下一步

核心业务停止扩张。当前已完成 PR #33 学生端第三轮修复、正式内容补齐、机器证据、集中全量回归和论文事实更新；Word/PPT 本轮未重新生成，学校模板视觉检查与 GPT 独立审查仍待完成。用户本人最终人工验收仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`，不得写成 PASS。

用户仍需按 [最终人工验收清单](FINAL_MANUAL_ACCEPTANCE_CHECKLIST.md) 完成一次真实 CAPTCHA 验收。用户确认前保持 `FINAL_MANUAL_ACCEPTANCE_PENDING`；PR #33 不得自行合并，不能把机器浏览器或历史 Provider 结果伪造为真人/本轮 PASS。
