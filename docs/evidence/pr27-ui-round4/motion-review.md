# PR #27 UI Round 4 Motion Review

## 范围

Portal 的动态叙事由现有 GSAP 与 ScrollTrigger 实现，主实现位于 `rike-tiku-frontend/src/views/PortalView.vue`，视觉状态由 Portal、motion 与 material 样式共同承载。没有增加 Three.js、WebGL、第二套动画库、粒子背景、替换光标或 scroll-jacking。

## 动效清单

| 场景 | 触发与参数 | 连续变化 |
| --- | --- | --- |
| Hero load | 首次挂载 timeline | 导航、标题、操作、世界主视觉与光学仪器按层进入；交互不等待动画结束 |
| Hero scroll | Hero 从 `top top` 到 `bottom top`，`scrub: 0.75` | 主视觉、文案、仪器与滚动提示以不同速率和方向运动，形成 depth parallax |
| Navigation material morph | 页面起点后 360px，`scrub: 0.6` | `--portal-nav-opacity` 连续提高、`--portal-nav-scale` 连续收紧，clear shell 逐步变为更凝聚的 frosted shell |
| Physics pinned scene | 桌面端 `min-width: 64rem`；`top top` 至 `+=2300`；`pin`；`scrub: 0.82` | 波形绘制、场线显现、透镜位移、光束展开、WAVE/FIELD/OPTICS 读数切换、材质转实与光谱过渡连续受滚动进度控制 |
| Discipline transition | 各学科进入视口，`scrub: 0.72` | 学科 atmosphere、主视觉与文字以同一滚动进度过渡；Physics 的钴蓝光学语言进入 Chemistry 的液体/光谱，再进入 Biology 的水滴/叶脉/生命网络 |
| Learning loop | 场景从 `top 70%` 到 `bottom 62%`，`scrub: 0.68` | 练习、判分、错题、标准解析、再练习的进度线与节点依次形成连续闭环 |
| Pointer optical response | 仅 `hover: hover` 且 `pointer: fine` | GSAP `quickTo` 驱动光学焦点，水平最大 12px、垂直最大 9px；高光原点仅作低幅响应，不替换系统光标 |
| Controls | pointer-down / active 状态 | 控件即时位移与材质反馈；不持续脉冲、不循环弹跳 |

Physics 的 `portal-physics-wave-frame.png` 与 `portal-physics-optics-frame.png` 是同一个 2300px pinned timeline 的不同进度关键帧，不是两个静态页面。

## 材质变化

- 页面初始导航保持 clear glass，滚动进度连续驱动不透明度与尺寸，最终形成更稳定的 frosted shell。
- Physics 中透明透镜、场线和光束先建立空间关系，后段 `physics-material-solid` 提升实体感，并由 `physics-spectrum` 把光学颜色带入 Chemistry。
- Portal 玻璃集中用于导航、Hero 光学焦点与局部工具层；长文本、题目、解析、表格和日志保持 solid readable surface。

## Desktop、Mobile 与 Reduced Motion

- Desktop：只有 `(min-width: 64rem) and (prefers-reduced-motion: no-preference)` 才启用 Hero scrub、Physics pin、学科连续转场、学习闭环 scrub 与精细指针响应。
- Mobile：`(max-width: 63.99rem)` 不启用 pinned scene；章节恢复浏览器自然纵向流，仅保留短时、一次性的视口 reveal。页面不是桌面 pin 场景的缩小版。
- Reduced motion：`prefers-reduced-motion: reduce` 不建立 scrub 或 pinned timeline，清除由动画准备态产生的隐藏/变换属性，直接显示完整静态构图。
- 390px：主视觉、认证表面、导航和长内容按单列组织；Physics 的波、场、透镜与光谱保留静态语义，不依赖运动才能理解。

## 生命周期与性能

- GSAP `matchMedia` 隔离桌面、移动与 reduced-motion 分支。
- 桌面 pointer listener 由该 matchMedia 分支返回的清理函数移除；高光 CSS 自定义属性同时复位。
- 路由离开时调用 GSAP context 与 matchMedia 的 `revert()`，回收本页面创建的 timelines、ScrollTrigger 与媒体分支，不使用会误杀其他页面实例的全局 `killAll()`。
- 长期滚动变化以 `transform`、`opacity` 和 CSS 自定义属性为主，没有在每个 scroll event 中执行重型同步布局计算。
- 指针计算只在精细指针设备启用，并通过 `quickTo` 平滑复用 tween；触摸设备不注册监听器。
- Portal 离开路由后移除 motion-ready 根状态，避免样式与监听器泄漏到其他页面。

## 动态证据限制

当前机器浏览器工具不支持录制 MP4、WebM 或 GIF，因此本轮没有伪造或声称存在视频证据。证据目录保留 34 张状态与连续关键帧：33 张最终证据，另 1 张修补前诊断帧。静态截图只能证明关键状态；scrub、pin、pointer response、降级与 cleanup 的真实性由实现参数、连续关键帧和机器浏览过程共同佐证，仍需用户在最终人工复验中亲自感受滚动与物理反馈。
