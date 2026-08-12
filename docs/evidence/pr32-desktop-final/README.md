# PR #32 真实桌面最终验证

日期：2026-08-12

环境：本机正式 `rike_tiku`，前端 8080，后端 8081

状态：`PASS_WITH_NOTES`

本目录只保存脱敏文字证据。IDE 控制台包含本机绝对路径和完整 classpath，正式业务页面包含本机人员信息，因此不上传相关截图。

## IDE 与端口

- IDEA `RikeTikuBackendApplication`：实际点击 Run，连接 `rike_tiku`，Flyway 14 个迁移验证通过，schema V14，Tomcat 8081，health 200。
- WebStorm `RIKE Frontend`：实际点击 Run，Vite 成功监听 `http://localhost:8080/`，页面 200，API base 指向后端 `/api/v1`。
- 启动顺序：后端→前端、前端→后端、前端保持时重启后端、关闭 Run Console 后再次 Run，均通过。
- 原端口冲突来自当前 RIKE 仓库的旧 Node/Vite 与 Java/Spring Boot 实例。使用 IDE Stop 或精确 PID 停止，没有全局终止 Java/Node。
- `scripts/reclaim-rike-port.ps1` 验证了空闲端口、真实 RIKE 占用、无关进程占用三种场景；无关进程被拒绝停止。

## 专用 Chrome

- 启动独立临时 Chrome Profile，并通过 CDP/Playwright 读取每次实际 `page.url()`；未复用日常 Cookie、扩展或历史。
- 使用真实 PNG CAPTCHA，不开启或读取 `testCode`。
- 代表性账号覆盖多角色教师、两名单角色教师、199/200 班学生主链及额外同班抽查。
- 管理员模型配置只显示 Key 已配置；教师只见授权学科；管理员路由对单角色教师关闭。
- 学生完成练习、提交、结果、正确答案、STANDARD、错题、RIKE 当前题答疑入口和个人中心；AI 与 STANDARD 界限清楚。
- 1440 px 与 390 px 核心路由横向溢出 0；console error 0、page error 0、failed request 0。

## 收尾状态

- BLOCKER：0。
- HIGH：0。
- MEDIUM：0 个需在本轮修改的问题。
- LOW：复杂管理员表格仍建议桌面使用；前端保留大 chunk build warning。
- 正式库恢复为 V14、35 表、378 题、3 位教师、6 位学生、2 个班级、6 条 ACTIVE 任课、2 条启用 AI 配置。
- 9 个账号恢复 BCrypt 初始密码摘要与首次改密门禁；目标事务表合计 0。
- `FINAL_MANUAL_ACCEPTANCE_PENDING` 保持不变。本证据是桌面控制的机器巡检，不是多人真实用户研究。
