# RIKE Aqua Liminal Future

Status: PR #27 唯一正式设计系统。内部主题名：`mizuiro-aero`。

## Product truth

RIKE 面向高中物理、化学、生物的长时间学习与教学管理。题目、解析、表格、权限和真实业务状态始终优先于装饰；公共入口可以有环境感，登录后的工作区必须高效、清晰。

## Hallmark third-round distill

第三轮审计要求在第二轮减法基础上重建真正的 editorial hierarchy。优先级最高的五项问题是：

1. Portal 的 Hero、三张卡片与巨大留白仍像通用模板，缺少章节节奏和科学视觉焦点。
2. 三科学科图形仍偏图标化，不能承载物理场、化学器皿/光谱和生物叶脉/膜/遗传结构的完整语义。
3. 学科环境需要进入学生与教师真实工作区，同时保持题面、答案、解析的长时间可读性。
4. 历史结果、掌握度与标准解析需要更自然的展示结构，不能让视觉重构掩盖业务事实。
5. 动效必须服从内容、reduced motion 和窄屏，不得以粒子、霓虹或 scroll-jacking 制造表面复杂度。

最终 Portal 采用五个清楚章节：事实型 Hero、物理、化学、生物、学习闭环与登录。三科不再压缩成卡片或小图标；每科使用大幅原创主视觉、短事实说明和少量课程标签。学习闭环只保留练习、判分、错题、解析、再练习五步及真实数字 3 / 360 / 18，不写 AI 规划或设计理念。

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

Portal 系统 Hero 使用项目原创语义 SVG；三科学科章节使用同一系列的项目原创静态 WebP，不使用网络图片或运行时生成服务：

- `physics-optical-field.webp`：透明光学介质、钴蓝波动、场线和运动轨迹，1600×1000，约 46 KB。
- `chemistry-glass-spectrum.webp`：日光玻璃器皿、梅紫液面、分子键和光谱折射，1600×1000，约 32 KB。
- `biology-living-network.webp`：叶脉、细胞膜、遗传双螺旋与生态连接，1600×1000，约 99 KB。

三张源图由本轮实际可用的图像生成工具生成，再本地压缩为构建时资产；不含文字、Logo、人物、密钥或外部服务配置。物理首幅按首屏需求 eager + high priority，其余两幅 lazy load；每幅都有准确中文 alt。没有学科含义的矩形、椭圆和装饰性几何不再承担学科主视觉。

## Scientific typography

`ScientificText` 只解析显式 `\\(...\\)` 与 `\\[...\\]` 片段，旧纯文本保持原样；普通 `/` 不会被猜成分数。`MathFragment` 通过 KaTeX 0.18.3 的 DOM renderer 输出 HTML + MathML，不使用 `v-html`，并固定 `trust=false`、大小/宏展开限制与可见 fallback。`QuestionContent` 继续负责附件 marker 与文本组合。

`MetricFraction` 仍可用于真正的数学分数；“已练习知识点 / 总知识点”是统计比例，改为普通内联 `0 / 38` 并保留完整 `aria-label`。`StandardAnalysis` 按换行安全拆分标题、步骤和正文，每一段继续交给 `QuestionContent` / `ScientificText` / KaTeX，不使用 `v-html` 或 Markdown HTML renderer。Topic18 全部 18 道解析已改为多段结构；旧单段纯文本仍兼容。

## Typography, spacing and interaction

- 使用本地 humanist 中文字体栈，无远程字体依赖。
- 工作区不使用营销页字号；题目/解析保持舒适行宽和行高。
- 4/8 spacing；普通控件至少 42px，粗指针环境 44px。
- 键盘 focus 始终可见；危险操作与普通操作分层。
- 学生主导航同一时刻只有一个 active。
- 管理员筛选使用响应式 grid，表格标识不在 token 中间断行。
- display math 在窄屏可水平滚动，不撑破 390px 布局。

## Motion

Portal 保留一次短促 Hero 入场，并用 GSAP + ScrollTrigger 为三科学科章节提供进入视口时的轻量 reveal 和极小幅图文视差；不 pin、不锁滚动、不使用粒子、鼠标追光或 3D 引擎。Vue 在 `onMounted` 后通过 `gsap.context()` 和 `gsap.matchMedia()` 创建，卸载时 `revert()`；动画只改 transform 与 opacity。`prefers-reduced-motion` 下所有内容直接可见。练习切题和解析展开使用短 Vue transition，不为表格和管理员页面增加营销动效。

## Skill execution record

实际发现并读取：

- `hallmark`：本机已安装 skill 的 audit/study/redesign/distill 参考；
- `impeccable`：本机已安装 skill 的 craft-floor 与静态检测规则；
- `gsap-core`、`gsap-timeline`、`gsap-frameworks`、`gsap-performance`、`gsap-scrolltrigger`：对应 `SKILL.md` 均已读取。

第三轮按 Hallmark 的 Split Studio 结构重建 Portal，强调章节节奏、大幅科学主视觉、短事实文案和克制留白；Impeccable 用于审查层级、对比、窄屏、防横向溢出和无障碍，最终静态检测返回 `[]`。这只表示已知静态反模式为 0，不代替浏览器视觉验收。GSAP 只用于 Portal 有内容意义的短入场、视口 reveal 和小幅视差，并完整尊重 reduced motion。

## Evidence

第三轮 production-like 浏览器证据位于 `docs/evidence/pr27-ui-round3/`，包含 Portal 1280/390、三张原创主视觉、三科学生环境、三科教师任课环境、管理员中性 Dashboard、`0 / 38`、单选/多选完整冻结答案、三科结构化 Topic 和 `50%` 判定正确。机器结果不能替代用户真实 CAPTCHA 与最终视觉复验；在用户复验前，PR #27 保持 Draft，MA-021 至 MA-025 均为 `FIXED_AWAITING_USER_RETEST`。
