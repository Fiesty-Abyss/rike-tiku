# Product — RIKE 理科学习辅助系统

> 当前可引用产品事实、已验证边界与已知限制见 [docs/FINAL_PROJECT_FACTS.md](docs/FINAL_PROJECT_FACTS.md)；逐图论文/PPT 资料见 [docs/FINAL_SCREENSHOT_EVIDENCE_CATALOG.md](docs/FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)。PR #34 在用户完成打印复验前保持 Draft。

## Product truth

- 面向高中物理、化学、生物的在线题库实训与学习辅助系统。
- 当前已交付非 AI 主链：教学组织、题库导入与审核发布、附件、学生练习与错题、掌握度与规则推荐、师生消息和个人中心。
- 运行时 AI 尚未上线；STANDARD 答案与解析始终是权威事实。
- 使用者包括学生、教师、管理员，以及同时拥有管理员和教师身份的账号。
- 选择题历史结果和错题详情使用练习会话冻结的选项快照展示 `label + 完整内容`；题库后续修改不改变既有学习事实。
- 填空题仍以人工 canonical answer 为展示基准；只有 seed 明确列出的 accepted answers 才参与确定性等价判分，当前生物数值题显式接受 `1/2`、`0.5`、`50%`、`50％`。

## Primary jobs

- 学生：快速进入一门学科，按知识点练习，提交后查看结果、解析和错题。
- 教师：查看真实任课范围、班级学生、学情和高频考点，并与学生私信。
- 教师：在手动组卷中检索本人任教学科内已发布的客观题和专题主观大题，设置分值与顺序后发布；随机/规则组卷只抽可确定性判分的客观题。
- 管理员：维护教学组织、账号和题库，完成导入、审核、发布与高风险操作审计；教师列表将展示职务与真实系统角色分开显示，并可在保留教师身份和任课关系的前提下授予或撤销 ADMIN。

## Experience principles

- 清楚优先：每页先呈现当前任务、范围和下一步操作。
- 事实优先：不展示虚构指标，不模糊 AI 与规则能力边界。
- 角色清楚：共享 RIKE 产品身份，但不同角色采用符合其任务密度的导航。
- 可恢复：加载、空状态和错误状态都说明发生了什么以及下一步怎么做。
- 可访问：键盘焦点可见，触控目标足够，动效尊重 reduced-motion。
- 可阅读：标准解析按标题、步骤和正文安全分段，公式继续使用 ScientificText / KaTeX；不把数据库文本作为 HTML 执行。
- 主观题边界：专题学习和试卷中的 `SUBJECTIVE` 作答可以保存并供教师按 STANDARD 人工处理，但系统不会由 AI 或规则给出正式分数；客观题自动得分不得被表述为整张卷最终成绩。

## Round 4 visual experience

`RIKE Aqua Future` 是当前唯一视觉方向，内部主题名仍为 `mizuiro-aero`。它用清水、空气、日光、透明光学与自然生命结构建立统一世界，同时保持科学仪器的精密度；`modern-minimal` 与 `Split Studio` 不再作为现行方向。这里借鉴的是编辑层级、滚动节奏、材质变化和物理反馈，不复制 Apple 产品构图，也不复刻 Windows 7 控件。

- Portal 通过 Hero、Physics、Chemistry、Biology、学习闭环和进入系统六个连续场景说明真实产品；只展示 3 科、360 道自动练习题和 45 道专题主观大题，不写 AI 宣传或虚构指标。
- 登录和角色选择延续同一光学入口语言，但用户名、密码、CAPTCHA、错误和返回首页始终处于首要位置。
- 学生按真实学科进入物理、化学、生物环境，题目、答案、结果和标准解析仍放在高对比实体表面。
- 教师按真实 `subjectCode` 进入任课 scope，先表达科目、班级、学情和高频考点，不把教师工作台做成营销页。
- 管理员使用 neutral Aqua；玻璃只用于 shell 与局部工具面，表格、筛选、日志、导入和危险操作使用高密度实体表面。
- 桌面 Portal 允许连续 scrub、Physics pinned scene、小范围 pointer response 和材质转换；移动端改为自然纵向叙事，reduced motion 直接显示完整静态内容。
- 原创 WebP 是构建时静态资产，不引入运行时图像生成或外部图床；现有 GSAP / ScrollTrigger 只服务视觉叙事，不改变浏览器自然滚动。

本轮视觉重设计不增加产品能力，不修改业务流程、API、路由、权限、数据库、Flyway、自动判分或 STANDARD 权威规则。`0 / 38`、冻结完整答案、逐项解析、Topic18 分段、ScientificText / KaTeX、显式 accepted answers、附件 Blob 和角色切换继续按既有行为工作。

## Technical constraints

- Vue 3、TypeScript、Vite、Element Plus；不更换技术栈。
- 不引入 Tailwind、大型动画框架或第二套业务组件系统。
- UI 调整不得改变 API、权限、题目状态机、评分或附件安全语义。
