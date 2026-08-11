# RIKE Aqua Future

Status: PR #27 Round 4 现行设计系统。内部主题名：`mizuiro-aero`。

## Round 4 Design Thesis

1. RIKE 的主要用户是需要长时间做题、复盘与教学管理的高中物理、化学、生物学生、教师和管理员。
2. 首次打开应先感到清澈、明亮、可触摸的科学世界，然后立即理解这是能真正练习与管理课程的工具。
3. 视觉母题是“水中的精密科学仪器”：天空、清水、折射玻璃、日光高光与自然生命结构共同组成 RIKE Aqua Future。
4. 物理以场、波、光路、轨迹和刻度表达精密运动；不用原子图标代替整门学科。
5. 化学以器皿、液面、弯月面、分子几何和光谱表达转化与平衡；不用烧瓶图标或荧光液体。
6. 生物以叶脉、水滴、膜结构、遗传与生态连接表达生长系统；不用卡通细胞或椭圆拼图。
7. 它不是普通教育 SaaS：公共入口是一段连续科学叙事，工作区则把真实题目、解析、数据和权限置于视觉中心。
8. 它不是 Apple 官网复制品：只采用编辑层级、滚动节奏、材质转换与物理反馈，不复制产品构图、文案、资产、字体或 DOM。
9. 它不是 Windows 7 复刻：继承 Frutiger Aero 的生态乐观、空气与触感，不复刻桌面、控件皮肤或怀旧装饰。
10. Portal、登录入口、角色选择和 Topic 长文允许较强的空间叙事；它们负责建立世界、引导进入与解释知识。
11. 答题、结果、错题、教师数据、管理员表格与弹窗必须克制，采用实体可读表面和明确状态。
12. 动效只解释深度、学科转场、滚动进度、因果步骤和按压反馈；不阻止操作，不承载唯一信息。
13. 桌面 Portal 允许 pinned scene、连续 scrub、轻量视差与光学 pointer response；移动端改为完整的自然纵向叙事。
14. `prefers-reduced-motion` 下直接呈现最终静态构图，全部内容、导航和业务操作仍完整可用。
15. `modern-minimal` 与 `Split Studio` 自 Round 4 起正式废弃；Round 3 只在文末作为人工验收失败的历史记录保留。

## Product truth and scope

RIKE 面向高中物理、化学、生物的长时间学习与教学管理。题目、解析、表格、权限和真实业务状态始终优先于装饰。Round 4 只重建设计 token、材质、构图、导航和动态体验，不改变 API、路由、权限、判分、STANDARD 权威事实、附件安全、数据库或 Flyway。

公共 Portal、登录和角色选择负责建立 RIKE 世界；学生与教师工作区延续学科环境；管理员使用 neutral Aqua。答题、结果、解析、Topic 长文、表格、日志和表单使用可读实体表面，不把整站铺成透明卡片。

## Aqua Future visual system

语义 token 位于 `rike-tiku-frontend/src/styles/tokens.css`，主主题位于 `themes/mizuiro-aero.css`，共享组件位于 `components.css`，动效规则位于 `motion.css`，学科环境位于 `subject-environments.css`。

- 环境 token：`--aero-sky`、`--aero-water`、`--aero-mist`、`--aero-horizon`，组合蓝天、清水、雾白和日光。
- 光学 token：`--aero-glass-clear`、`--aero-glass-frosted`、`--aero-glass-thick`、`--aero-specular`、`--aero-edge-light`、`--aero-depth-shadow`、`--aero-caustic`、`--aero-iridescence`。
- 物理 token：`--physics-field`、`--physics-light`、`--physics-cobalt`、`--physics-deep`、`--physics-silver`。
- 化学 token：`--chemistry-fluid`、`--chemistry-plum`、`--chemistry-spectrum`、`--chemistry-silver`、`--chemistry-deep`。
- 生物 token：`--biology-water`、`--biology-jade`、`--biology-leaf`、`--biology-forest`、`--biology-deep`。

材质按职责分层：`aero-shell` 用于页面环境和共享导航；`aero-glass` 用于局部工具面；`aero-glass-heavy` 只用于入口、Dialog 或旗舰视觉；`aero-solid` 承载题目、解析、表格与长文本；`aero-control` 提供可按压控件；`aero-orb` 只作为少量有科学含义的光学焦点。玻璃必须有真实背景、边缘高光和同一光源；`backdrop-filter` 不可用时回退为高不透明度实体表面，文字对比不依赖 blur。

## Typography and scientific reading

字级 token 为 `display`、`hero`、`section`、`title`、`body`、`caption`、`metric`、`scientific`。中文使用本地系统字体栈，不提交 Apple SF 或商业字体；正文控制行长、行距和中英文标点，指标与正文不共用夸张字重。触控目标在粗指针环境至少 44px，键盘 `focus-visible` 必须清楚。

`ScientificText` 只解析显式 `\\(...\\)` 与 `\\[...\\]`，普通文本和 `/` 不被猜测为公式。`MathFragment` 继续使用 KaTeX DOM renderer 输出 HTML + MathML，`trust=false`，不使用原始 `v-html`。`StandardAnalysis` 安全拆分标题、步骤和正文，窄屏长公式只在自身区域横向滚动。`MetricFraction` 保留数学用途；掌握度比例继续显示内联 `0 / 38`。

## Subject and role environments

学科根节点只接受由真实 `subjectCode` 解析出的 `data-subject="physics|chemistry|biology"`。物理使用线性场、波、光路和较明确的速度感；化学使用液面、折射、光谱和转化感；生物使用叶脉、膜、水滴与低速生长感。三科共享天空、水体、玻璃和日光，因此属于同一个 RIKE 世界。

学生工作区以学科和当前学习任务为中心；教师以任课 scope、班级、学情和高频考点为层级；管理员保持 neutral Aqua，只让具体数据带学科标识。共享导航使用 Aqua shell，滚动后材质连续加深；管理员表格、筛选、分页和危险操作仍以扫描效率为先，不加入营销式动画。

## Portal: six connected scenes

1. **Hero**：`rike-aqua-world.webp` 建立天空、清水、透明仪器、波动、液面与生命结构共处的 RIKE 世界；标题、主视觉、光学仪器和水光采用不同深度。
2. **Physics**：桌面 pinned scene 连续绘制波、场和光路，使 readout 从 WAVE 转到 FIELD、OPTICS，并让 clear glass 逐步转为 solid reading surface。
3. **Chemistry**：器皿、弯月面、分子几何和光谱进入梅紫银灰环境，强调条件、证据与平衡，不使用烧瓶图标或荧光液体。
4. **Biology**：叶脉、膜、水滴、根系和生态连接构成生命系统，强调结构、功能与连接，不使用单一细胞或椭圆拼图。
5. **Learning loop**：一条连续进度轨依次连接练习、判分、错题、标准解析与再练习，并只展示真实数字 3、360、18。
6. **Entrance**：光学入口自然收束到登录，按学生、教师、管理员三种真实工作台说明下一步，不写 AI 宣传文案。

## Original generated assets

四张图均由本轮真实可用的图像生成工具生成，经过人工目视检查并压缩为构建时 WebP；无文字、Logo、人物、密钥或外部图床。实际文件均为 RGB、1586×992：

| 资产 | 体积 | 用途与准确 alt | 加载策略 |
| --- | ---: | --- | --- |
| `assets/aqua/rike-aqua-world.webp` | 188,464 B（184.0 KiB） | RIKE 统一世界；Portal alt 为“清水与日光中的透明光学仪器连接波动、化学液面和生命叶脉” | Portal 与 Login `eager` + `fetchpriority="high"`；Role Selection `eager`；当前无额外 `<link rel="preload">` |
| `assets/aqua/physics-field-lab.webp` | 169,722 B（165.7 KiB） | “清水实验场中的透明透镜、钴蓝波干涉、场线和精密测量环” | `lazy` + `decoding="async"` |
| `assets/aqua/chemistry-equilibrium-lab.webp` | 98,496 B（96.2 KiB） | “水面实验平台上的透明器皿、梅紫液面、分子几何和光谱折射” | `lazy` + `decoding="async"` |
| `assets/aqua/biology-living-system.webp` | 302,356 B（295.3 KiB） | “清水上下相连的巨幅叶脉、膜结构、水滴和根系生命网络” | `lazy` + `decoding="async"` |

四图合计 759,038 B（约 741.2 KiB）。HTML `width` / `height` 与真实像素一致，避免布局偏移。首屏用 eager/high 明确提高优先级，其余场景延迟加载；不在运行时调用图像生成服务。

## Motion implementation

- Hero load choreography 依次进入导航、标题、操作、世界图与光学仪器；页面不等待动画结束即可交互。
- Hero 使用 `scrub: 0.75` 连续改变世界图、标题、仪器和滚动提示的深度；导航以 `scrub: 0.6` 连续改变表面不透明度与尺寸。
- Physics 在桌面以 2300px 自然滚动区间、`pin` 与 `scrub: 0.82` 展开波、场、透镜、光束、读数和 clear-to-solid 材质变化；不改写滚轮距离，不做 scroll-jacking。
- Chemistry 与 Biology 使用 `scrub: 0.72` 联动环境光、图像和文字；学习闭环以 `scrub: 0.68` 推进轨道和五个步骤。
- Hero 的 pointer response 只在桌面、`hover: hover`、细指针且未请求 reduced motion 时启用；`quickTo` 只移动光学焦点 12px / 9px 并改变小范围高光坐标，不替换 cursor。
- 按钮与共享控制在 pointer-down 时 70ms 内缩放至 0.972，业务 loading、disabled 与错误状态继续由既有逻辑控制。
- 长期动画和滚动动画以 transform、opacity 与 CSS 变量为主；不使用 Three.js、WebGL、粒子、霓虹、循环弹跳或自动播放视频。

## Responsive, reduced motion and cleanup

桌面增强条件为最小 64rem；低于 64rem 时取消 pinned、scrub parallax 和 pointer effect，Portal 变为完整自然纵向叙事，只保留一次性轻量 reveal。75rem、63.99rem、47.99rem 与 24.4rem 响应规则覆盖 1440、1280、1024、768 与 390px；移动端触控目标至少 44px，导航、CAPTCHA、题面和公式不得造成页面横向溢出。

`prefers-reduced-motion: reduce` 同时由 GSAP `matchMedia` 和 CSS 处理：内容直接呈现最终静态构图，动画/transition 缩至 1ms，浏览器自然滚动不变。路由离开时移除 pointer listener，`gsap.context().revert()`、`matchMedia().revert()`，删除 `aqua-motion-ready`；ScrollTrigger 由 context 一并销毁，避免跨路由残留。

## Evidence boundary

Round 4 的截图、连续关键帧、motion review、console/page error 和响应式结论写入独立 `docs/evidence/pr27-ui-round4/`。静态截图不证明动画，机器浏览器也不替代用户对 Frutiger Aero、学科语义、滚动体验和真实 CAPTCHA 的最终人工验收。

## Round 3 historical record — human rejected

本节只保存第三轮设计与机器证据的来龙去脉，不是当前实现规范。用户在 Round 4 明确判定第三轮视觉人工验收失败：Portal 仍像普通极简 SaaS，首屏留白与 split composition 缺少科学世界，三科主要停留在 accent，旧 Hero blob 和动效不足以构成 Frutiger Aero 或连续滚动叙事。

### Historical direction and assets

第三轮内部方向为 `RIKE Aqua Liminal Future / Split Studio`：事实 Hero、物理、化学、生物、学习闭环与登录章节，冷灰纸面配钴蓝、梅紫灰和玉石绿。旧 Hero 是 Portal 内联 wave / molecule / vein SVG；旧三图为 `physics-optical-field.webp`（1600×1000，46,978 B）、`chemistry-glass-spectrum.webp`（1600×1000，31,988 B）与 `biology-living-network.webp`（1600×1000，99,490 B）。这些资产在 Round 4 已由四张 Aqua Future WebP 替换，不再作为当前页面依赖。

第三轮 GSAP 只做短 Hero 入场、视口 reveal 与极小幅视差，明确不 pin、不做连续场景，也不做 pointer response。该策略是本次人工验收失败原因之一，不能覆盖前文 Round 4 motion contract。

### Historical skill record

第三轮曾读取并使用 Hallmark、Impeccable 与当时的旧 GSAP skill 集合，以 Split Studio、静态反模式检查和轻量 reveal 指导实现。这些记录只解释第三轮为什么形成当时方案；Round 4 未读取已移出活动目录的备份，也不把这些旧 skill 当作当前设计依据。项目现有 GSAP / ScrollTrigger 运行时依赖继续保留。

### Historical evidence and retained business fixes

第三轮 production-like 机器证据保留在 `docs/evidence/pr27-ui-round3/`，包括 Portal 1280/390、三科学生/教师环境、管理员 Dashboard、`0 / 38`、冻结完整答案、结构化 Topic 和 `50%` 判定。机器证据没有证明视觉人工通过。

Round 4 只替换第三轮视觉方向；`0 / 38`、`AnswerDisplay` 冻结选项、Demo360 逐项解析、`StandardAnalysis`、Topic18 分段、ScientificText / KaTeX 与显式 accepted answers 判分继续保留，不得因视觉重构回退。
