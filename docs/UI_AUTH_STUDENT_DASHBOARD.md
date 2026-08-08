# UI、统一登录与学生三科工作台

本模块已通过 PR #15 普通 merge 进入 `main`，merge commit 为 `12d636fde4afa198edc78eb0c295f5b88c8e3456`。PR #15 不修改 Flyway V1–V7，也不新增迁移。

统一登录为 `/login`；历史三条角色登录路径重定向到该入口。PR #19 当前分支使用 `GET /api/v1/auth/captcha-challenge` 获取服务端两分钟有效的一次性图形验证码，登录提交 `challengeId` 和 `captchaCode`；挑战成功、失败、过期或取用后即失效。验证码默认隐藏，首次登录操作只展开，第二次才提交。`expectedRole` 可选以兼容旧调用；单角色直达工作台，多角色在认证后选择真实拥有的角色。`activeRole` 仅保存在 sessionStorage，后端授权始终依据 JWT 与数据库角色。

Element Plus 全局为中文 locale，公共枚举由 `src/utils/formatters.ts` 集中转换。学生主页提供物理、化学、生物入口和真实可用知识点数；各学科页支持随机五题、条件练习与本学科错题预选。教师只显示当前教师档案对应的三元任教范围；教师班级学科工作台、高频考点和师生私信已实现，教师任务与统计仍未实现。三类用户均可主动修改密码；忘记密码仅提示联系管理员。

2026-08-08 已在 `rike_tiku_demo` 完成 PR #19 浏览器复验：验证码默认隐藏、错误后自动刷新、图片和文字刷新均通过；三个 smoke 单角色账号直达各自工作台，`demo_physics_admin` 进入角色选择页，退出重登正常且控制台 0 error。PR #15 滑块为历史实现，当前登录不再使用。
