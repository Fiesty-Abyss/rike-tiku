# RIKE 论文事实核对表

更新时间：2026-08-17

| 论文陈述 | 可核验依据 | 状态 |
|---|---|---|
| 系统采用前后端分离模块化单体 | `DESIGN.md`、代码模块和 Maven/Vite 工程 | 已核验 |
| Java 25、Spring Boot 4.1、Vue 3、MySQL 8.4 | POM、package.json、本机命令输出 | 已核验 |
| Flyway V1–V30、50 张业务表 | 迁移目录、随机测试库、正式 `rike_tiku`；V30 纯结构快照为当前、V29 仅保留历史查阅 | 已核验 |
| 既有 V1–V25 未修改 | Git blob hash 与迁移校验 | 已核验 |
| 公共门户内容规模采用实时统计 | 候选分支 `PortalView.vue`、`publicPortal.ts`、`PortalStatsController/Service` 与 `PortalStatsServiceIntegrationTest`；只统计 ACTIVE 学科、PUBLISHED GLOBAL 可练习客观题和 PUBLISHED GLOBAL 专题主观题 | 候选已专项核验；待合并后冻结为主线事实 |
| 203 班权限隔离演示 | 候选分支 `TeacherScopeIsolationIntegrationTest`；唯一 `teachingAssignmentId`、203 私有题可见、199 私有题/发布范围拒绝 | 候选已专项核验；待合并后冻结为主线事实 |
| 四份 Excel 演示资料 | `docs/demo-import/`、`FinalDemoImportWorkbooksIntegrationTest`；随机 V1→V30 schema Preview/Confirm，题目进入 PENDING | 候选已专项核验；待合并后冻结为主线事实 |
| Demo 题量 378 | DemoDataService validate 输出 | 已核验 |
| 答疑最多 10 轮 | V15、`StudentAiService.MAX_ROUNDS`、集成测试 | 已核验 |
| 标准/深度思考映射 | DeepSeek Provider request 测试 | 已核验 |
| 搜索使用智谱官方结构化 Web Search | Search Client 与 Mock HTTP 测试 | 已核验；真实 smoke 取决于 Key |
| 学生变式支持单选、多选、填空 | Parser、共享判分器、UI 与接口 | 已核验 |
| 候选题审核前不发布 | AI generation 集成测试 | 已核验 |
| 密码恢复不泄露账号存在性 | 统一响应与集成测试 | 已核验 |
| 教师组卷受任教学科约束，手动组卷可纳入专题主观大题 | PaperService / PaperAssignment 集成测试、正式机器浏览器 | 已核验 |
| AI 科学文本不使用未过滤 v-html | `AiScientificContent` 源码与测试 | 已核验 |
| 本轮点名后端专项 | 新颖度 3/3、候选生成集成 7/7、学生变式集成 4/4；使用随机临时 schema | 已执行 |
| 本轮学生前端专项 | 5 files、12 tests，0 failures；type-check/build 通过 | 已执行 |
| 后端最终全量 | PR34 打印修补后的实际 `mvn test`/`mvn package` 结果见 `FINAL_PROJECT_FACTS.md` | 本轮重跑中 |
| 前端最终全量 | PR34 打印修补后的实际测试、type-check、build、audit 结果见 `FINAL_PROJECT_FACTS.md` | 本轮重跑中 |
| 专题内容与科学审计 | 15 单元 / 45 题、计算 14 / 实验 9 / 流程 5 / 材料分析 13 / 综合 4；600 strings、117 database rows、`SCIENTIFIC_CONTENT_ERRORS=0` | 已执行 |
| 最终两项用户反馈 | 忘记密码弹窗不再展示内部安全实现说明；教师列表按真实角色表显示 TEACHER / ADMIN，授权、撤销、重新授权、最后管理员与当前管理员保护均有回归测试 | 已执行 |
| 机器浏览器 | 正式 `rike_tiku`：历史 V30 为 11 pages / 56 assertions，0 console/page/failed-request error，0 overflow；PR34 另补学生/答案预览 `window.print` handler hook | `MACHINE_BROWSER_VERIFIED`；不等同真人验收或 OS 对话框 |
| 正式库迁移与本轮内容 | Flyway 从 V29 正常升级至 V30；`shi_juan_fa_bu_ti_mu.fu_jian_kuai_zhao` 冻结受控附件元数据，学生主观题保存为 `SUBJECTIVE_PENDING`；15 单元、45 关系 | 已核验；不新增表 |
| V1–V30 checksum 与临时库 | 正式 `flyway_schema_history` 30 行均 `success=1`；随机临时 schema V1→V30 通过；V1–V29 迁移文件未改动 | 已核验 |
| Word/PPT 交付物 | 学校模板尚未提供；本轮只同步 Markdown 事实、截图目录和引用矩阵，不生成或声称学校最终排版 | 本轮未生成 |
| 真实 DeepSeek/GLM/xAI/Search 本轮 smoke | 没有可安全使用的轮换后凭据，未消费真实 Provider | BLOCKED_EXTERNAL_PROVIDER；历史结果不外推为本轮 PASS |
| PR #34 用户打印复验、问卷或学习成效提升 | PR34-MA-001 已 `PRINT_USER_VERIFIED`，仅证明系统打印窗口打开；仍无问卷或课堂效果数据 | 禁止声称学习成效提升 |
| PR #35 教师发布历史与试卷库软删除 | `PaperService`、`PaperAssignmentService`、随机临时 schema 集成测试；后端 221 tests、前端 68 files/224 tests；release 历史与学生提交不物理删除 | 已 ordinary merge（`fde39c53efca316010abf63acf56fda2c631315c`）；`USER_MANUAL_ACCEPTANCE` 与自动化验证完成，`MACHINE_BROWSER = NOT_RUN`，不将其写成课堂效果证据 |

论文中的“有效”只指指定测试或约束得到验证，不等同于教学效果经真实学生群体实验证明。

## PR #37/PR #38 最终认证与演示数据卫生事实

- 203 隔离演示的教师账户为张生康（`t2026004`、`TEACHER`），指定学生编号/用户名为 `2026203001` 至 `2026203003`；199/200 及张锡鹏、吴雪莉、谢亚坤的既有教学事实不重建、不删除。
- 系统默认/管理员重置密码由配置项 `app.account.default-reset-password` 管理；本地演示为 `a1234567`，仅以 BCrypt 保存。管理员新建、Excel 导入和恢复账户均进入 `shi_fou_shou_ci_deng_lu=1` 初始密码状态；即使历史 flag 漂移为 `0`，BCrypt 仍匹配默认密码时也必须改密。两类改密均禁止将新密码设为系统默认密码，之后才可访问正常角色业务。用户主动改密与管理员密码恢复仍然存在。
- 本项是认证可用性与演示数据卫生调整，不构成真实课堂教学效果或口令安全性实验结论。PR #37 已于 `2026-08-21T03:07:23Z` 以 `cb785631c359b88dc4841a9eeed3af14879516cb` 合入 main；最终回归数字见 [FINAL_PROJECT_FACTS.md](../FINAL_PROJECT_FACTS.md)。

## 正式参考文献边界

开题报告与毕业论文正文只能引用 `docs/THESIS_REFERENCES.md` 及
`docs/references/references.bib` 中固定的 22 条白名单文献。扩展调研资料已经物理隔离到
`docs/references/research-only/`，不得作为老师审查版本的正文引用或参考文献。本仓库未包含学校正式开题报告 Word 模板；相关内容仍需待套用学校模板。
