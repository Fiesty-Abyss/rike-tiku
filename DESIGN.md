# RIKE Aqua Liminal Future

Status: PR #27 唯一正式设计系统。内部主题名：`mizuiro-aero`。

## Product truth

RIKE 面向高中物理、化学、生物的长时间学习与教学管理。题目、解析、表格、权限和真实业务状态始终优先于装饰；公共入口可以有环境感，登录后的工作区必须高效、清晰。

## Hallmark second-round distill

第二轮审计把“继续加设计”改成“删去解释设计的内容”。优先级最高的五项问题是：

1. Portal 把学习流程和设计理念写成了面向用户的宣传文案。
2. Learning Current、角色说明和 AI 规划形成了作品集概念页，而不是简洁入口。
3. 化学矩形与生物椭圆缺乏学科含义。
4. 学科色只停留在卡片局部，没有进入学生与教师的真实工作环境。
5. 科学公式仍混在普通字符串中，理科学习内容缺少可靠的排版层。

最终 Portal 只保留四个区段：事实型 Hero、三科学科、真实数据快照、登录入口。删除学习流程解说、角色说明和 AI 规划区块；总文字量显著下降，ScrollTrigger 也随冗余叙事一起移除。

## Reference study DNA

Apple 公开产品页面只作为宏观设计研究：保留清楚的标题/正文节奏、目的明确的留白、渐进披露、材质层级和服从内容的动效。没有复制文案、图片、图标、字体、设备构图或具体页面结构。

## Macrostructure

- Public portal：品牌、三科学科、3/360/18 真实快照和登录入口。
- Student：以当前学习任务为视觉中心；当前学科决定整页环境，而不是只改变按钮颜色。
- Teacher：任课范围使用响应 DTO 的 `subjectCode` 决定环境主题；不依赖用户名或数据库固定 ID。
- Administrator：保持中性 Aqua 环境，同时用学科 accent 表达混合学科数据。
- Authentication：清楚的一次 CAPTCHA 登录，并保留返回 Portal 的自然出口。

## Colour, subject environments and material

语义 token 位于 `rike-tiku-frontend/src/styles/tokens.css`，主主题位于 `themes/mizuiro-aero.css`，学科环境位于 `subject-environments.css`。

- Canvas：水雾白和冷蓝灰。
- Ink：深海蓝黑，不使用大面积纯黑。
- Physics：克制 cobalt、ice blue 和清楚的线性结构。
- Chemistry：mist violet、periwinkle、mauve silver，不使用高饱和 SaaS 紫。
- Biology：jade mist、seafoam、muted forest，不使用卡通草绿。

学科根节点统一使用 `data-subject="physics|chemistry|biology"`，来源只接受稳定的 `subjectCode`。学生学科首页、练习、结果、错题、专题学习和教师任课范围共享同一环境解析；管理员不按单一题目染整页。

材质分层：

1. `surface-solid`：表格、长表单、题目、答案、解析、Topic 长文和日志。
2. `surface-glass`：顶部栏、登录、角色选择、学科页面标题、Dialog/Drawer 和少量浮动工具条。
3. `optical-glass`：Portal/Auth 的少数视觉焦点。

玻璃层使用可见的环境背景、半透明表面、24px 左右 blur/saturate、冷白边缘高光和轻微内外阴影建立前后深度；同屏不铺满 blur。`@supports not (backdrop-filter)` 使用高不透明度冷色实体表面，文字对比不依赖模糊。

## Semantic science visuals

Portal 三科使用同一线条与水色材质语言的原创 SVG，不使用网络资源：

- 物理：轨迹、波、矢量和场线。
- 化学：烧瓶液面、分子键和光谱边缘。
- 生物：细胞膜、细胞核、叶脉网络和螺旋结构。

没有学科含义的矩形、椭圆和装饰性几何已删除。

## Scientific typography

`ScientificText` 只解析显式 `\\(...\\)` 与 `\\[...\\]` 片段，旧纯文本保持原样；普通 `/` 不会被猜成分数。`MathFragment` 通过 KaTeX 0.18.3 的 DOM renderer 输出 HTML + MathML，不使用 `v-html`，并固定 `trust=false`、大小/宏展开限制与可见 fallback。`QuestionContent` 继续负责附件 marker 与文本组合。

`MetricFraction` 用于少数关键统计的堆叠分数，并提供完整 `aria-label`。Topic18 的代表性物理公式、化学式/离子、电荷上下标、科学单位和遗传分数已迁移到显式标记；后续题库可以渐进采用，不改变 Excel 导入兼容性，也不批量改写全部 378 道题。

## Typography, spacing and interaction

- 使用本地 humanist 中文字体栈，无远程字体依赖。
- 工作区不使用营销页字号；题目/解析保持舒适行宽和行高。
- 4/8 spacing；普通控件至少 42px，粗指针环境 44px。
- 键盘 focus 始终可见；危险操作与普通操作分层。
- 学生主导航同一时刻只有一个 active。
- 管理员筛选使用响应式 grid，表格标识不在 token 中间断行。
- display math 在窄屏可水平滚动，不撑破 390px 布局。

## Motion

Portal 只保留一次短促的 Hero 入场；删除为了长页面而存在的滚动叙事。Vue 中的 GSAP 在 `onMounted` 后通过 `gsap.context()` 和 `gsap.matchMedia()` 创建，卸载时 `revert()`；只动画 opacity、x/y 和小幅 scale。`prefers-reduced-motion` 下内容立即可见。练习切题和解析展开使用短 Vue transition，不为表格和普通按钮增加动画。

## Skill execution record

实际发现并读取：

- `hallmark`：`D:/CodexHome/skills/hallmark/SKILL.md` 及 audit/study/redesign/distill 相关参考；
- `impeccable`：`D:/CodexHome/skills/impeccable/SKILL.md` 与 `reference/craft-floor.md`；
- `gsap-core`、`gsap-timeline`、`gsap-frameworks`、`gsap-performance`、`gsap-scrolltrigger`：对应 `SKILL.md` 均已读取。

Hallmark 本轮执行 audit Portal copy → distill Portal → audit subject pages → redesign subject environments，结论是删掉区块、删掉概念文案、删除 ScrollTrigger，并用语义 SVG/学科环境替代装饰图形。Impeccable 按 audit → critique → normalize → polish → distill 检查文字层级、玻璃可感知度、科学内容可读性、响应式与视觉噪声；最终执行 `node D:\\CodexHome\\skills\\impeccable\\scripts\\detect.mjs --json src`，返回 `[]`。这只表示已知静态反模式为 0，不代替浏览器视觉验收。GSAP 的实际保留范围只有 Portal/Auth/Dashboard 中有内容意义的短动效；本轮 Portal 不再使用 ScrollTrigger。

## Evidence

第二轮 production-like 浏览器证据位于 `docs/evidence/pr27-ui-round2/`，包含 Portal 1280/390、三科学生环境、三科教师任课环境和科学排版页面。机器结果不能替代用户真实 CAPTCHA 与最终视觉复验；在用户复验前，PR #27 保持 Draft，MA-017 保持 `IMPLEMENTED_AWAITING_FINAL_MANUAL_ACCEPTANCE`。
