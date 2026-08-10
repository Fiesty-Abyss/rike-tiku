# 人工验收问题记录

只记录亲自复现的结果，不预填或虚构问题。每个问题使用独立编号；修复后保留原记录并将状态更新为“已修复待复验”或“已关闭”。截图应使用脱敏相对说明，不提交 Token、密码、数据库配置或个人隐私。

| 编号 | 角色 | 页面/接口 | 操作 | 预期 | 实际 | 严重级别 | 截图/说明 | 状态 |
|---|---|---|---|---|---|---|---|---|
| MA-001 | 全部 | 登录 | 使用 IDEA 和 WebStorm 默认运行配置启动后登录 demo 账号 | 对应角色登录成功 | 原问题返回 `INVALID_CREDENTIALS`，修复后不再复现 | BLOCKER | 根因是后端默认连接 `rike_tiku` 而非 `rike_tiku_demo`，并伴随演示脚本端口、CORS 和 API 基础地址变量错误。复验：demo_admin 登录成功；demo_teacher 真实 HTTP 登录成功；demo_student 浏览器登录成功；浏览器地址为 `localhost:18080`；后端连接 `rike_tiku_demo` | 已关闭 |
| MA-002 | STUDENT | 学生首页 | 查看学生首页的学习入口 | 按物理、化学、生物清晰组织练习与错题入口 | 已实现三科学科卡片、学科页和错题预选入口 | HIGH | 2026-08-07 `rike_tiku_demo` 浏览器复验：三入口、三学科页、条件练习和错题预选均通过 | 已关闭 |
| MA-003 | STUDENT | 学生首页 | 查看首屏布局与信息呈现 | 首屏布局紧凑、信息层级清楚 | 已以三科工作台替换大横幅，并统一卡片、间距和导航层级 | UX | 2026-08-07 浏览器复验通过 | 已关闭 |
| MA-004 | 全部 | 登录 | 登录系统 | 账号验证后自动识别单角色，多角色账号再选择角色 | 已统一为 `/login`，单角色直达，多角色认证后选择 | MEDIUM | 2026-08-08 PR #19 浏览器复验：图形验证码默认隐藏、错误自动刷新；三单角色直达，`demo_physics_admin` 进入角色选择页，退出重登正常且控制台 0 error。PR #15 滑块为历史实现 | 已关闭 |
| MA-005 | 全部 | 分页、表格与状态展示 | 用户可见文本统一使用中文 | 已补全题型、使用模式和版权来源 formatter | MEDIUM | 2026-08-07 管理员题库列表浏览器复验：单选题、多选题、填空题、在线练习、用户提供及分页均为中文 | 已关闭 |
| MA-010 | STUDENT | 练习结果 | 完成一次含单选、多选、填空的练习并查看结果 | 结果页应展示已提交答案、正确答案和标准解析 | 已修复 Jackson 3 `JsonNode` 请求反序列化和选择题答案对象格式化 | HIGH | 2026-08-07 浏览器复验：创建、作答、提交、结果、错题链路通过；未提交响应不含答案/解析 | 已关闭 |
| MA-011 | 全部 | 主动修改密码 | 打开修改密码对话框并提交 | 对话框应提供取消和确认修改操作 | 已将 footer 置于对话框插槽 | HIGH | 2026-08-07 浏览器复验：按钮可见；错误旧密码、策略与同密码被拒绝，正确改密后旧密码 401、新密码 200；临时账号已清理 | 已关闭 |
| MA-012 | ADMIN | 学生管理 | 打开列表并执行密码重置确认 | 列表正常加载，取消确认不产生控制台 error | 首次连接到旧后端进程时列表 500；关闭重置确认时出现未处理的 `cancel` error | HIGH | 重启当前分支后端后列表恢复；确认取消改为受控返回并新增 2 条专项测试。2026-08-07 新浏览器页复验列表、确认重置、取消确认及控制台 0 error | 已关闭 |
| MA-006 | 全部 | 个人中心 | 查看和维护个人信息 | 可查看资料和简介、上传头像 | 已实现三角色统一 `/profile`、真实角色/业务档案只读展示、简介、头像持久化/删除和现有改密入口 | MEDIUM | 2026-08-08 `rike_tiku_demo` 浏览器复验：学生资料和班级、PNG 上传/刷新持久化/删除、教师档案与任课摘要、纯管理员简介和改密入口、ADMIN+TEACHER 双角色切换均通过；控制台 0 error，随后 reset/seed 清理 | 已关闭 |
| MA-007 | ADMIN | 学生管理 | 管理单个学生 | 支持手动新增、详情、编辑、调班和密码重置 | 已实现独立 `/admin/students`，保留 Excel 批量导入 | HIGH | 2026-08-07 `rike_tiku_demo` 浏览器复验：新增、编辑、启停专项、调班历史、一次性密码重置和首次改密通过；临时学生已清理 | 已关闭 |
| MA-008 | STUDENT/TEACHER | 高频考点 | 查看或维护高频考点 | 按科目展示高频考点，并由教师维护 | 已实现教师本人 ACTIVE 三元任课关系内的工作台维护，以及学生按本人班级和学科读取 ACTIVE 内容 | MEDIUM | 2026-08-08 `rike_tiku_demo` 浏览器复验：物理教师 199/200 工作台新增、编辑、停用、启用；生物/化学范围隔离；199/200 学生内容隔离；控制台 0 error | 已关闭 |
| MA-009 | STUDENT/TEACHER | 私信 | 师生沟通 | 师生可在三元任课关系约束下私信 | 已实现基于 ACTIVE 三元任课关系与学生当前主班级的纯文本私信、未读和历史保留 | MEDIUM | 2026-08-08 `rike_tiku_demo` 浏览器复验：199 学生与物理教师、200 学生与化学教师均完成双向收发；未读、范围隔离、伪造 conversationId 403 和控制台 0 error 通过 | 已关闭 |
| MA-013 | 工程 | 后端测试隔离 | 执行全量测试 | 测试必须只使用随机临时库，不连接正式 `rike_tiku` | 旧测试上下文首次执行时误用默认数据源，对正式库自动执行 V9，创建两张空私信表；未写入演示账号、班级、题目、高频考点、会话或消息 | HIGH | 已让全部 Spring 测试继承随机临时库支持，并增加数据库名断言；修复后 92/92 PASS，正式库 V9 安装时间未再变化。未获授权，不执行回滚或删表 | 已关闭 |
| MA-014 | STUDENT | 推荐练习 | 零答题时点击首个“开始巩固” | 推荐知识点应有足够可用题目 | 首次浏览器验收把无题的父级目录知识点纳入推荐 | HIGH | 掌握度统计覆盖当前学科全部 ACTIVE 知识点；5 题规则推荐单独复用 StudentPracticeService 真实题池资格，仅题量至少 5 道的知识点生成“开始巩固”；专项断言与浏览器复验通过 | 已关闭 |
| MA-015 | STUDENT | 条件练习预选 | 从推荐卡进入并创建练习 | 学科、知识点、5题预选应完整进入创建请求 | 学科监听的并发加载会在路由预选后再次清空知识点，首次实际创建成全科练习 | HIGH | 重载选项仅移除当前学科无效选择，新增并发预选专项测试；浏览器复验题目全部属于目标知识点 | 已关闭 |
| MA-016 | 全部 | 公共入口 | 未登录访问根路径 | 显示公共首页、功能介绍、三科学科介绍和统一登录入口 | 根路径已渲染无需认证的公共门户，统一登录继续复用 `/login`；首屏明确运行时 AI 尚未上线 | BLOCKER | 2026-08-09 `feat/public-portal`：前端 122/122、type-check、build、audit 0；独立 Demo 三角色登录、登录态/未登录态刷新、320–1280 宽度和控制台复验通过 | 已关闭 |
| MA-017 | ADMIN/STUDENT | 题库附件 | 导入含附件题并在详情/练习查看 | 附件可安全访问，题干或解析中真实显示 | PNG/JPEG 受控存储，SHA-256 回读校验；管理员详情及学生题面/结果/错题统一渲染。未提交会话拒绝 STANDARD_ANALYSIS；PDF 仍不进入普通练习 | BLOCKER | 后端附件/权限/导入/题池专项 27 个，26 PASS、1 个符号链接 assumption skipped；全量 112 个测试 0 失败、1 skipped、package PASS；前端附件专项 4/4、全量 127/127、type-check/build/audit；Demo `reset → seed → validate → smoke` PASS，PHYSICS-S1 两条附件实际文件/hash 回读通过。按新策略，用户 CAPTCHA 浏览器验收延期至非 AI 最终集成验收，不属于 PR #26 merge gate | IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE |
| MA-018 | ADMIN | 高风险操作日志 | 执行账号、题库、审核等管理员写操作 | 关键高风险操作可追溯 | PR #27 已加入 V11 日志表、真实 Service 记录和 ADMIN 查询 API/页面，覆盖当前管理员高风险写操作；最终集成浏览器验收待统一执行 | BLOCKER | 日志专项已验证真实操作者、成功/失败、脱敏摘要、ADMIN 查询权限；正式库未写入 | 机器实现完成，待最终集成验收 |

| MA-019 | ADMIN | 题库批量导入 | 用 30 道合法样例完成预检查、确认、审核、发布、查询和附件显示 | 全链路可重复验收 | PR #27 新增独立 Golden30 测试，复用三科清洗候选素材，在隔离库真实经过 preview、confirm、来源权利补充、审核、发布、查询、附件和学生练习；物理/化学/生物各 10 道 | BLOCKER | Golden30 测试通过；30 道均发布，29 道固定答案题进入自动练习，1 道主观题保留专题学习；原始 Excel SHA-256 未改变，正式库未写入 | 机器实现完成，待最终集成验收 |
| MA-020 | STUDENT | 练习提交 API | 对提交接口发送空请求体 | 返回明确 4xx 校验错误 | 空 body 与不可解析 body 均返回 `400 INVALID_REQUEST`；正常提交、判分和重复提交回归不变 | MEDIUM | `RenZhengJiChengTest.emptyOrUnreadablePracticeSubmitBodyReturnsBadRequest` 通过；正式库未写入 | 机器实现完成，待最终集成验收 |

MA-017 机器证据更新（PR #26 独立审查修正后）：附件/权限/导入/题池专项共 27 个测试，26 PASS、1 个符号链接 assumption skipped；真实 `QuestionImportService` 已覆盖 Excel preview → confirm → `ti_mu_fu_jian` 的 I001/I002 → 受控 storage → 管理员 detail/content → 学生题池、提交前 STEM 和提交后 STANDARD_ANALYSIS。`mvn clean test` 为 112 个测试 0 失败、1 个 skipped；前端 127/127、type-check、build、audit 和 Demo `reset → seed → validate → smoke` 均通过。状态为 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。用户 CAPTCHA、ADMIN/STUDENT/TEACHER/多角色视觉验收统一延期至非 AI 最终集成验收，不属于 PR #26 merge gate。

验收策略更新（2026-08-09）：不再为每个非 AI PR 单独执行人工浏览器验收；机器可验证的 HTTP、权限、文件、数据库、Demo 和构建证据仍按 PR 记录。真实 CAPTCHA 与页面视觉交互统一积累到非 AI 工程完成后的最终集成验收。未实际执行的人工结果不写为 PASS，MA-017 在最终验收前保持 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。

PR #26 已普通 merge，merge commit 为 `b992bffef07465665b371b7b707ca8814ec2d36d`。当前 `feat/non-ai-final-closure` 为最后一个非 AI 分支，PR #27 为最后一个非 AI Draft PR；机器实现完成后直接在该 PR 内修复最终审计发现，不创建 PR #28，也不开始 AI。MA-017、MA-018、MA-019、MA-020 涉及的最终浏览器视觉/CAPTCHA验收统一延期至非 AI 最终集成验收，未执行结果不写为人工 PASS。

PR #27 最终机器门禁补充（2026-08-10）：后端 `mvn clean test` 117 个测试 0 失败、1 个 symbolic-link assumption skipped，`mvn clean package` PASS；前端 34 个文件 129/129、type-check、build、audit 0；Demo `reset → seed → validate → smoke` PASS，业务题 120 道（物理 40、化学 39、生物 41）。这些是机器证据，不代表最终人工 CAPTCHA/视觉验收已执行。

严重级别：`BLOCKER`、`HIGH`、`MEDIUM`、`LOW`、`UX`。

补充：管理员重置学生密码对应登录页“忘记密码请联系管理员”；不包含自助找回、邮件或短信。个人中心已关闭 MA-006，不包含头像裁剪、云存储或自助修改业务档案。PR #23 浏览器抽查 10 道新变式、结果/错题/掌握度与随机变化均通过。2026-08-09 V3.0 总审计新增 MA-016 至 MA-020；其中 MA-016 后续由公共门户分支关闭，历史审计快照仍保持原 REJECT。公共门户验收后 Demo reset/seed/validate/smoke PASS，末次 reset/seed/validate 恢复固定状态；MA-017 至 MA-020 未改动。正式库为 V10、26 张业务表；演示题、场景账号/班级、高频考点、私信、学习及验收个人资料污染计数均为 0。
