# 最终认证与数据卫生报告

日期：2026-08-21；基线：PR #36 后 main。该轮不新增 Flyway，V1–V30 与 50 张业务表保持不变。

## 账号与认证

- 张生康原实体原地修正为 `t2026004` / `T2026004`；203 三名指定学生原实体原地修正为连续的 `2026203001`–`2026203003`。
- 管理员新建学生、教师与学生 Excel 空密码导入统一使用 `AdminDefaultPasswordPolicy`；显式 Excel 密码仍被尊重。
- 默认密码、新建账号和管理员恢复均写入 BCrypt；管理员分配初始密码或恢复默认密码时写入 `shi_fou_shou_ci_deng_lu=1`。登录成功后只能访问身份确认和改密接口；修改成功写回 `0` 后才可进入正常业务。主动改密与管理员密码恢复保留。

## PR #38 认证语义冻结（merge 前门禁通过）

- 不比较明文默认密码；门禁只依据正式业务字段 `shi_fou_shou_ci_deng_lu`，因此部署环境可安全覆盖默认口令配置。
- `ChuShiMiMaMenJinGuoLvQi` 位于 JWT 身份识别之后，对普通学生、教师、管理员业务端点统一返回 `403 MUST_CHANGE_PASSWORD`；`/api/v1/auth/me`、`/api/v1/auth/change-initial-password` 与 `/api/v1/auth/change-password` 继续可访问。
- 前端登录路由、会话恢复与 API 错误处理统一跳转 `/change-initial-password`；初始密码修改成功后取得新 JWT 并回到真实角色首页。
- 管理员操作日志保留查询、筛选、排序、分页、详情、刷新与删除；CSV 下载入口、前端 API、后端 endpoint 和服务端 CSV 生成已移除。
- merge 前回归：后端 224 tests、0 failures、0 errors、3 skipped；前端 68 files、225 tests、0 failures；type-check、build、`npm audit --omit=dev`（0 vulnerabilities）、科学审计（600 strings、0 errors）与正式 22 条文献审计均通过。
- 正式 `rike_tiku` 只读复核：Flyway V30、30 条成功迁移、0 failed migration、50 张业务表、`V30_BROWSER*` 账号标记为 0；随机临时测试 schema 已删除，仅保留 `rike_tiku` 与 `rike_tiku_demo`。
- `MACHINE_API_SMOKE`：使用受控 203 学生账号完成默认口令登录（`mustChangePassword=true`）→ 普通业务被 `MUST_CHANGE_PASSWORD` 拒绝 → 初始密码修改成功 → 旧口令失效、新口令登录成功；随后该账号已恢复为默认口令且首次改密状态，供本地演示使用。该记录不是用户人工浏览器验收。

## V30 浏览器测试业务数据

删除前文本全字段审计在正式库发现：`V30_BROWSER_CLASS`、`V30_BROWSER_T`、`V30_BROWSER_S`、`V30_BROWSER_STUDENT`、`V30_BROWSER_TEACHER`。经引用检查确认只关联一名机器学生、测试教师、两份测试卷、两次发布、两次提交、四条逐题答案、一个任课关系及十条测试高频考点关系；未关联真实学生。

已按子到父顺序删除上述专属业务记录。最终 `rike_tiku` 与 `rike_tiku_demo` 的 `V30_BROWSER|V30_NEWLINE|V30_MACHINE|V30_SMOKE` 业务文本扫描结果均为 0。Flyway V30 及其附件快照、`SUBJECTIVE_PENDING` 容量和 migration history 均未修改。

## 本地目录审计

- `E:\BISHE2026_LOCAL`：非 Git worktree，属于 2026-08-12 本地账户说明与 `rike_tiku-before-final` SQL 备份，分类 `UNIQUE_BACKUP`，保留。
- `E:\BISHE2026-backups`：包含 PR #33 recovery bundle 与 V19/V29 前 SQL dump，分类 `UNIQUE_BACKUP`，保留。
- 根目录 `.tmp-*`、浏览器 profiles、Excel renders 和 `outputs/` 已证明为 untracked、无唯一资料、无活动 RIKE 进程。当前执行环境拒绝递归删除命令；未绕过安全限制。`.gitignore` 已防止再次进入 Git。
