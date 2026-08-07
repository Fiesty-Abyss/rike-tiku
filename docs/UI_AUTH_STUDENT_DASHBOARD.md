# UI、统一登录与学生三科工作台

本模块已通过 PR #15 普通 merge 进入 `main`，merge commit 为 `12d636fde4afa198edc78eb0c295f5b88c8e3456`。PR #15 不修改 Flyway V1–V7，也不新增迁移。

统一登录为 `/login`；历史三条角色登录路径重定向到该入口。`GET /api/v1/auth/slider-challenge` 提供服务端短时一次性滑块验证，登录提交 `challengeId` 和 `sliderOffset`；挑战成功、失败或取用后即失效。`expectedRole` 可选以兼容旧调用；单角色直达工作台，多角色在认证后选择真实拥有的角色。`activeRole` 仅保存在 sessionStorage，后端授权始终依据 JWT 与数据库角色。

Element Plus 全局为中文 locale，公共枚举由 `src/utils/formatters.ts` 集中转换。学生主页提供物理、化学、生物入口和真实可用知识点数；各学科页支持随机五题、条件练习与本学科错题预选。教师仅显示当前教师档案对应的三元任教范围；教师任务、统计、高频考点和私信仍未实现。三类用户均可主动修改密码；忘记密码仅提示联系管理员。

2026-08-07 已在 `rike_tiku_demo` 完成真实浏览器验收：MA-001 至 MA-005、MA-010、MA-011 已关闭；滑块错误、过期和重放均由服务端拒绝，三单角色直达、临时多角色进入选择页。MA-006 至 MA-009、头像、个人资料与简介、管理员学生完整管理与账号恢复、高频考点、私信、DeepSeek、GLM、AI 与 MVP30 正式入库均未实现。
