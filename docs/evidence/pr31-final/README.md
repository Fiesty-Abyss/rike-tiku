# PR #31 最终机器证据

- 环境：Windows，本地 `rike_tiku_demo`，backend `http://localhost:18081`，production preview frontend `http://localhost:18080`。
- 基线：PR #30 merge commit `d67ebc83bf0b8a2fbd889290d5a0f78a27d7640e`；PR #31 最终 HEAD 以 Draft PR 为准。
- Viewport：桌面 1440×1000，移动 390×844。
- 浏览器：独立 headless Chrome 临时上下文，不复用或修改用户浏览器资料。
- 覆盖：portal、真实登录、学生练习/结果/错题/AI、教师 scope/AI 候选、管理员题库/AI 配置/候选等 25 条路由。
- 结果：0 console errors、0 page errors、0 failed requests、0 horizontal overflow routes。
- 安全：截图不含完整 Provider Key、JWT 或 Prompt；管理员页面只显示 Key 已配置/掩码。
- 已知限制：真实 `glm-4.6v-flash` 第一次受控窗口为 HTTP 429；第二个最终窗口收到完整 JSON 代码围栏，旧 Parser 拒绝。围栏兼容已修复并通过全量自动化，但遵守两次真实窗口上限未再次调用，状态为 `REAL_GLM_VISION_NOT_REVERIFIED_AFTER_WRAPPER_FIX`，不能视为真实多模态 PASS。

`browser-results.json` 保存路由级机器结果；PNG 只保留有代表性的 portal、登录、学生 AI、教师 AI、管理员 AI 和移动端页面。

机器证据不能替代用户人工验收。当前状态：`FINAL_MANUAL_ACCEPTANCE_PENDING`。
