# RIKE 论文事实核对表

更新时间：2026-08-12

| 论文陈述 | 可核验依据 | 状态 |
|---|---|---|
| 系统采用前后端分离模块化单体 | `DESIGN.md`、代码模块和 Maven/Vite 工程 | 已核验 |
| Java 25、Spring Boot 4.1、Vue 3、MySQL 8.4 | POM、package.json、本机命令输出 | 已核验 |
| Flyway V1–V19、39 张业务表 | 迁移目录、随机测试库、`rike_tiku_demo` | 已核验 |
| 既有 V1–V14 未修改 | Git diff | 已核验 |
| Demo 题量 378 | DemoDataService validate 输出 | 已核验 |
| 答疑最多 10 轮 | V15、`StudentAiService.MAX_ROUNDS`、集成测试 | 已核验 |
| 标准/深度思考映射 | DeepSeek Provider request 测试 | 已核验 |
| 搜索使用智谱官方结构化 Web Search | Search Client 与 Mock HTTP 测试 | 已核验；真实 smoke 取决于 Key |
| 学生变式支持单选、多选、填空 | Parser、共享判分器、UI 与接口 | 已核验 |
| 候选题审核前不发布 | AI generation 集成测试 | 已核验 |
| 密码恢复不泄露账号存在性 | 统一响应与集成测试 | 已核验 |
| 教师组卷受任教学科约束 | PaperService 集成测试 | 已核验 |
| AI 科学文本不使用未过滤 v-html | `AiScientificContent` 源码与测试 | 已核验 |
| 后端集中受影响专项 | 35 tests，0 failures/errors/skipped | 已执行 |
| 前端新增功能专项 | 11 files、21 tests | 已执行 |
| 后端最终全量 | 186 tests，0 failures，0 errors，3 skipped | 已执行 |
| 前端最终全量 | 66 files、207 tests；type-check/build/audit 通过 | 已执行 |
| 机器浏览器 | 17 routes，0 console/page/request error，0 overflow | 已执行；09/10 使用已披露 UI 夹具 |
| 正式库迁移 | 仓库外备份后 V14/35 → V19/39，378 PUBLISHED、9 用户不变 | 已核验 |
| 真实 DeepSeek/GLM/Search 本轮 smoke | 本轮未消费真实 Provider | NOT_RUN；沿用 PR #31 历史事实，不伪造 PASS |
| 真人用户验收、问卷或学习成效提升 | 无本轮数据 | 禁止声称 |

论文中的“有效”只指指定测试或约束得到验证，不等同于教学效果经真实学生群体实验证明。
