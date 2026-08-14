# PR #33 V29 机器浏览器与性能证据

环境：匿名 `rike_tiku_demo`，Flyway V1–V29 / 50 张业务表；前端 18080、后端 18081。该目录保存可复验的 JSON 结果，不是用户真人验收，也不把 Fake/Mock/夹具记作真实 Provider。

## 浏览器

- [`browser-results-v29.json`](browser-results-v29.json)：14 条新增路线和交互，0 console error、0 page error、0 unexpected failed request、0 horizontal overflow、0 missing assertion。
- [`../thesis-final/browser-results-pr33.json`](../thesis-final/browser-results-pr33.json)：17 条基础路线。两组共 31 条。
- 消息场景通过真实 Demo API 验证居中确认、发送者撤回后双方占位、仅本人删除后对方仍可见。
- 199 班试卷可见、200 班列表为空，专题单元显示 3 道题；密码恢复覆盖 1440×900 与 390×844。
- 09/10 变式截图是明确披露的确定性 UI 夹具，仅验证结构化作答和判分界面；真实 DeepSeek 未调用。

## 性能

- [`performance-v29.json`](performance-v29.json)：三角色轮换学生、教师、管理员主要路由，持续 10 分钟并保存 11 个样本。
- RIKE Java listener Working Set：278.7 MB → 295.2 MB（+16.5 MB，非持续单调增长）。
- RIKE Vite listener Working Set：86.9 MB → 89.6 MB（+2.7 MB，非持续单调增长）。
- 结论：`NO_REPRODUCIBLE_APPLICATION_LEAK`。这只描述本机开发采样，不是生产容量标准。
- 采样结束后按监听端口和 PID 停止 RIKE 进程，18080/18081 均已释放；未全局结束 Java、Node 或 Chrome。

## Provider 披露

此前暴露的智谱凭据未使用。当前没有可安全使用的轮换后凭据，因此 DeepSeek variant、DeepSeek tutor、GLM Vision、xAI Vision 与 Web Search 的本轮真实 smoke 均为 `BLOCKED_EXTERNAL_PROVIDER`。自动化中的 Fake/Mock 仅记合同测试通过。
