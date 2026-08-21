# 最终认证与数据卫生报告

日期：2026-08-21；基线：PR #36 后 main。该轮不新增 Flyway，V1–V30 与 50 张业务表保持不变。

## 账号与认证

- 张生康原实体原地修正为 `t2026004` / `T2026004`；203 三名指定学生原实体原地修正为连续的 `2026203001`–`2026203003`。
- 管理员新建学生、教师与学生 Excel 空密码导入统一使用 `AdminDefaultPasswordPolicy`；显式 Excel 密码仍被尊重。
- 默认密码、新建账号和管理员恢复均写入 BCrypt，且 `shi_fou_shou_ci_deng_lu=0`。首次登录强制门禁与前端跳转已取消；主动改密与管理员密码恢复保留。

## V30 浏览器测试业务数据

删除前文本全字段审计在正式库发现：`V30_BROWSER_CLASS`、`V30_BROWSER_T`、`V30_BROWSER_S`、`V30_BROWSER_STUDENT`、`V30_BROWSER_TEACHER`。经引用检查确认只关联一名机器学生、测试教师、两份测试卷、两次发布、两次提交、四条逐题答案、一个任课关系及十条测试高频考点关系；未关联真实学生。

已按子到父顺序删除上述专属业务记录。最终 `rike_tiku` 与 `rike_tiku_demo` 的 `V30_BROWSER|V30_NEWLINE|V30_MACHINE|V30_SMOKE` 业务文本扫描结果均为 0。Flyway V30 及其附件快照、`SUBJECTIVE_PENDING` 容量和 migration history 均未修改。

## 本地目录审计

- `E:\BISHE2026_LOCAL`：非 Git worktree，属于 2026-08-12 本地账户说明与 `rike_tiku-before-final` SQL 备份，分类 `UNIQUE_BACKUP`，保留。
- `E:\BISHE2026-backups`：包含 PR #33 recovery bundle 与 V19/V29 前 SQL dump，分类 `UNIQUE_BACKUP`，保留。
- 根目录 `.tmp-*`、浏览器 profiles、Excel renders 和 `outputs/` 已证明为 untracked、无唯一资料、无活动 RIKE 进程。当前执行环境拒绝递归删除命令；未绕过安全限制。`.gitignore` 已防止再次进入 Git。
