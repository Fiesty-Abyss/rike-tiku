# UI/UX Foundation 审计与收口

## 审计发现

- Typography：公共门户与工作区缺少共享字级、行高和数字排版规则。
- Spacing：旧全局样式混用大量一次性像素值，页面节奏不一致。
- Hierarchy：学生、教师、管理员标题区与操作区结构不同，角色间连续性弱。
- Navigation：管理员窄屏菜单、学生窄屏导航和教师返回路径需要明确响应式策略。
- Density：表格操作、筛选区和附件编辑区在窄屏容易拥挤。
- Feedback：Element Plus 默认加载和空态可用，但缺少统一边界与恢复动作语言。
- Accessibility：需要统一 focus-visible、40–44px 点击目标、reduced-motion 和防横向溢出规则。
- Route continuity：Portal → Login → Role Selection → Workspace 的品牌、颜色和控件语言不够连续。

## Round 4 当前方向

当前唯一方向是 `RIKE Aqua Future`，内部主题名保持 `mizuiro-aero`。系统以清水、空气、日光、折射玻璃、柔和高光和自然生命结构形成统一环境，再用科学仪器的刻度、实体阅读面和明确状态保证可信度。`modern-minimal`、`Split Studio` 与第三轮冷色纸面方案不再是现行规范。

公共 Portal、Login 与 Role Selection 允许较强的空间和材质叙事；学生与教师通过真实 `subjectCode` 获得不同学科环境；管理员保持 neutral Aqua。题面、答案、标准解析、Topic 正文、表格、筛选、日志和长表单使用实体表面，玻璃只用于 shell、入口和局部工具面。

## 覆盖页面

- 公共：Portal、Login、Role Selection。
- 管理员：Layout、Dashboard、班级、学生、教师与任课、题库、操作日志。
- 学生：Home、Practice New、Practice Session、Result、Wrong Questions、掌握度、推荐。
- 教师：Home、Scope Workspace、高频考点、学情。
- 共享：Profile、Messages、Dialog、Table、Form、Empty/Error/Loading。

## 不变边界

本轮只调整前端表现和交互连续性，不改变数据库、Flyway、API contract、权限、题目状态机、评分、附件安全或业务路由。

## Round 4 foundation

### Material and typography

- `aero-shell` 组织环境和共享导航，`aero-glass` / `aero-glass-heavy` 只用于少量浮动层，`aero-solid` 承载长阅读与数据，`aero-control` 提供即时按压反馈，`aero-orb` 只用于科学含义明确的光学焦点。
- 环境使用 sky / water / mist / horizon，表面使用 clear / frosted / thick glass、specular、edge light、depth shadow、caustic 与 iridescence；不使用霓虹紫、粒子、满屏 blob 或大量嵌套玻璃。
- 字级统一为 display、hero、section、title、body、caption、metric、scientific；中文使用本地系统字体栈，不提交 Apple 或商业字体文件。
- 题目、解析和表格不依赖 backdrop blur 获得对比；不支持 blur 时回退高不透明度实体表面。

### Portal, motion and auth

- Portal 为 Hero、Physics、Chemistry、Biology、Learning Loop、Entrance 六场景。Hero 建立统一 Aqua Future 世界；Physics 用桌面 pinned + continuous scrub 展开波、场、光路和 clear-to-solid 材质；化学与生物随滚动切换环境；闭环用连续轨道表达练习到再练习。
- Hero 图像、文字和光学仪器使用不同速率的 depth parallax；导航表面随滚动连续增加不透明度并缩小；细指针才启用低幅 pointer response；控件 pointer-down 立即反馈。
- 不改写自然滚动，不使用 scroll-jacking、Three.js、WebGL、粒子、替换 cursor、循环弹跳或自动播放背景视频。
- Login 与 Role Selection 复用 Aqua 世界图和光学入口，但账号、密码、CAPTCHA、错误、角色权限和返回路径保持首屏可用。

### Workspaces

- Student：物理的场/波/光路、化学的液面/折射/转化、生物的叶脉/膜/生长进入背景、标题和局部仪器；答题与结果主体保持实体和高对比。
- Teacher：任课 scope、班级学生、学情和高频考点形成四层结构；三科环境可辨认，但列表和数据仍以扫描效率为先。
- Administrator：共享 neutral Aqua shell，以 selective Element Plus token 和 wrapper 统一 Dashboard、filter、table、dialog、import 与 logs；不按某道题给整页染色，不做营销动画。
- Shared：Profile、Messages、空态、错误、加载和导航使用同一 token、焦点与控制语言；业务状态不仅靠颜色表达。

### Responsive, accessibility and lifecycle

- 1440、1280、1024、768 与 390px 使用同一信息架构；低于 64rem 取消 pin、scrub parallax 与 pointer effect，改为完整自然纵向叙事，触控目标至少 44px。
- `prefers-reduced-motion` 下直接显示最终静态构图，动画和 transition 缩至 1ms；内容、导航、CAPTCHA 和业务操作不缺失。
- Portal 卸载时移除 pointer listener，revert GSAP context 与 matchMedia，并销毁其 ScrollTrigger，避免跨路由残留。
- 图片有准确 alt 和真实 width/height；首屏统一世界图 eager/high，其余学科图 lazy；公式只在自身区域横向滚动，页面不得横向溢出。

### Business capabilities retained

视觉重构不得覆盖 `0 / 38`、`AnswerDisplay` 冻结选项、Demo360 逐项标准解析、`StandardAnalysis`、Topic18 分段、ScientificText / KaTeX、`50%` / `0.5` / `1/2` 显式 accepted answers、附件 Blob、`availableCount`、错题即时更新、类似练习、角色切换、权限和 CAPTCHA。

## PR #27 Round 3 历史记录（人工验收未通过）

以下内容只记录第三轮做过什么以及当时的机器证据，不再指导当前视觉实现。用户已明确判定第三轮 Portal、三科学科环境与动态叙事未达到要求；旧 Hero SVG、三张旧 WebP、Split Studio 构图和轻量 reveal 均可由 Round 4 替换。

- Portal 不再使用“Hero + 三张卡片”的模板结构，改为事实 Hero、物理、化学、生物、学习闭环与登录五个 editorial 章节；三科使用同一系列的原创压缩 WebP，系统 Hero 使用原创科学语义 SVG。
- 页面主背景采用冷灰、雾白、石英中性色；钴蓝、梅紫灰、玉石绿只进入对应学科的图像、标题光线和局部环境。管理员继续保持中性，不增加营销动效。
- 学生和教师的具体学科工作区继续由 API 返回的稳定 `subjectCode` 驱动整页环境，不以题目 ID 或当前按钮颜色推导。题目、答案和解析仍置于高对比实体表面。
- GSAP 只承担短 Hero 入场、视口 reveal 和小幅视差；动画以 transform/opacity 为主，组件卸载时 revert，`prefers-reduced-motion` 下内容直接展示，不 pin、不锁滚动、不追踪鼠标。
- 390px 机器复验无横向溢出；图片均有准确 alt，物理首图预加载，其他学科图 lazy load；长公式由现有科学文本组件在窄屏内部滚动。
- 结果/错题使用 `AnswerDisplay` 展示冻结选项内容，Topic18 使用 `StandardAnalysis` 按换行安全分段并继续复用 ScientificText/KaTeX；不使用原始 `v-html` 或完整 Markdown HTML 渲染器。

Round 3 机器证据继续保留在 `docs/evidence/pr27-ui-round3/`，不替代用户的真实视觉、CAPTCHA 和浏览器验收。第三轮视觉被拒绝不回退同期已经完成的答案展示、标准解析、Topic 分段和确定性判分修正。
