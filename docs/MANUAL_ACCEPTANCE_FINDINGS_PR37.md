# PR #37 最终认证与数据卫生验收记录

## 范围

本维护分支仅统一 203 演示账号、默认口令与首次登录语义，并定向清理可识别的 V30 浏览器测试业务数据。它不修改 Flyway、数据库结构、题库业务规则、199/200 或三位既有核心教师的教学事实。

## 已验证事实

- 张生康固定为 `t2026004` / `T2026004`，仅有 `TEACHER` 身份；203 指定学生使用 `2026203001`、`2026203002`、`2026203003` 作为学号和用户名。
- 使用真实 `/api/v1/auth/login` 与 CAPTCHA 流程在独立本地端口连接 `rike_tiku_demo` 验证了张生康和 `2026203001`：默认口令可登录，响应 `mustChangePassword=false`。
- 带历史首次登录标记的账号不再被安全过滤器拦截；用户仍可经 `/api/v1/auth/change-password` 主动改密，管理员重置和密码恢复仍将新口令保存为 BCrypt。
- `V30_BROWSER*` 根标识及其经过关系审计的发布、提交、逐题答案和高频考点测试关联已从正式库定向清理；清理不物理删除稳定教学事实。

## 机器验证与人工验收边界

`AUTOMATED_TEST_VERIFIED`、正式库只读审计和受控 Demo 登录验证均已执行。没有把这一次受控登录验证写作用户逐页人工验收；若需要课堂演示，可由用户使用 [FINAL_DEMO_ACCOUNTS.md](FINAL_DEMO_ACCOUNTS.md) 的账号在本地页面复核。

## 最终状态

`USER_MERGE_AUTHORIZATION = GRANTED_CONDITIONALLY` 的全部门禁已满足。PR #37 于 `2026-08-21T03:07:23Z` ordinary merge，merge commit `cb785631c359b88dc4841a9eeed3af14879516cb`；最终 main 已复核。该记录仍区分受控机器登录验证与用户逐页人工验收，不虚构后者。
