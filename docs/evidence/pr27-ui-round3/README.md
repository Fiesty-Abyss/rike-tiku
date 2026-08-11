# PR #27 UI Round 3 机器证据

- 日期：2026-08-11（Asia/Shanghai）
- 分支：`feat/non-ai-final-closure`
- 开始 Base / HEAD：`b992bffef07465665b371b7b707ca8814ec2d36d` / `d94cf67e593fd2401ae4ceae49f4eb69b61782f4`
- 实现与证据 commit：`d098ca17b7d96b73c0c3c04b38365ea4b79daab2`
- 前端模式：当前源码执行 `npm run build` 后的 Vite preview，`localhost:18080`，`strictPort`
- 后端机器链路模式：`smoke`，`localhost:18081`，仅用 demo-only `testCode` 建立三角色机器会话；脚本不记录 testCode、JWT、数据库密码或密钥
- 最终交付后端模式：`acceptance`，`localhost:18081`，CAPTCHA `testCode=false`
- 数据库：`rike_tiku_demo`，Flyway V1–V11，27 张业务表
- Demo：物理/化学/生物普通题各 120，Topic 每科 6，总题量 378
- 浏览器：Codex in-app Browser（匿名与可视页面）+ 本地 Playwright（demo-only 认证链路）
- 桌面分辨率：1280×900；移动分辨率：390×844
- 最终完整认证复跑：console error 0、page error 0、HTTP 4xx/5xx 0、无白屏
- 移动布局：`innerWidth=390`，页面 `scrollWidth=clientWidth=375`，无横向溢出
- 限制：以下全部是机器证据，不能替代用户真实 CAPTCHA、视觉质量或最终人工验收。

## Portal 与环境

| 证据 | 文件 | 结果 |
| --- | --- | --- |
| Portal 1280 | `portal-1280.png` | 五章节入口、原创系统视觉、真实 3/360/18 |
| Portal 390 | `portal-390.png` | 无横向溢出、主操作可见 |
| 物理章节 | `portal-physics.png` | 光学介质、波、场线与钴蓝环境 |
| 化学章节 | `portal-chemistry.png` | 玻璃器皿、液面、分子键与梅紫灰环境 |
| 生物章节 | `portal-biology.png` | 叶脉、膜、遗传与玉石绿环境 |
| 学生三科 | `student-physics-environment.png`、`student-chemistry-environment.png`、`student-biology-environment.png` | API `subjectCode` 驱动三种整体环境 |
| 教师三科 scope | `teacher-physics-scope.png`、`teacher-chemistry-scope.png`、`teacher-biology-scope.png` | 真实任课 scope 驱动三种环境 |
| 管理员 | `admin-dashboard-neutral.png` | 中性 Dashboard，无学科染色 |

## 业务修正

| 证据 | 文件 | 结果 |
| --- | --- | --- |
| 掌握度 | `student-mastery-0-of-38.png` | 显示自然内联 `0 / 38` |
| 单选结果 | `result-single-complete-answer.png` | 你的答案与正确答案均显示冻结 label + 内容，解析逐项 |
| 多选结果 | `result-multiple-complete-answer.png` | 多个冻结选项逐项显示，解析逐项 |
| Topic 物理 | `topic-physics-structured.png` | 多段结构与科学排版 |
| Topic 化学 | `topic-chemistry-structured.png` | 多段结构与科学排版 |
| Topic 生物 | `topic-biology-structured.png` | 多段结构与科学排版 |
| 生物数值填空 | `biology-50-percent-correct.png` | 提交 `50%`，canonical `1/2`，结果为正确 |
| Demo 解析抽样 | `demo-choice-analysis-samples.md` | 三科各固定 10 道结构抽样；不冒充内容人工审校 |
| CAPTCHA OFF | `login-captcha-testcode-off.png` | 最终 acceptance 环境 challenge 仅含 `challengeId`、`image`、`expiresAt` |

## 自动门禁

- 后端：`mvn clean test` 133 tests，0 failures，0 errors，1 skipped；`mvn clean package` PASS。
- skipped：Windows symbolic-link 权限 assumption，继续如实记为 skipped。
- 前端：49 files，170/170 PASS；type-check PASS；build PASS；audit 0。
- build 说明：1835 modules；存在既有主 chunk 超过 500 kB 警告，没有构建失败。
- Demo：`acceptance-prepare`、`validate`、`smoke` PASS；最终交付前再次 reset，清除机器练习、错题、私信、草稿和操作日志。

## 结论边界

MA-021 至 MA-025 的机器修正状态均为 `FIXED_AWAITING_USER_RETEST`。PR #27 保持 Draft / OPEN / 未 merge；非 AI A 层仍不得标记 `DONE_VERIFIED`，AI 尚未开始。
