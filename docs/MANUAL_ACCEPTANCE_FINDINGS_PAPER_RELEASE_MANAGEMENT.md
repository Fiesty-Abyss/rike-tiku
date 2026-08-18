# PR #35 教师试卷发布管理人工验收记录

- PR：#35（Draft）
- 分支：`fix/paper-release-management`
- 基线：`f95effcc1ea681530b4be6d01de724f4f999d9f6`
- 记录日期：2026-08-18
- 验收来源：用户人工页面审查与本轮机器专项。

## 已确认的基础闭环

用户已确认撤回发布后，学生“我的试卷”立即不再可见。该行为以 release 的 `CANCELLED` 状态实现，不物理删除发布或作答历史。

## 本轮补充项（待用户复验）

| 编号 | 内容 | 状态 |
|---|---|---|
| PR35-MA-UI-05 | 全局“班级发布记录”：按任课范围、状态和试卷名称集中查询 | `AUTOMATED_VERIFIED`，`USER_RETEST_PENDING` |
| PR35-MA-UI-06 | 试卷本体软删除：有效发布保护；全部撤回后可清理试卷库；历史 release/提交继续可查 | `AUTOMATED_VERIFIED`，`USER_RETEST_PENDING` |

自动化回归：后端 221 tests（0 failures / 0 errors / 3 skipped），前端 68 files / 224 tests（0 failures）；后端 package、前端 type-check/build/audit（0 vulnerabilities）通过。

## 数据边界

- `shi_juan.yi_shan_chu=1` 只从教师“我的试卷”隐藏本体；不删除 `shi_juan_fa_bu`、`shi_juan_fa_bu_ti_mu`、`shi_juan_ti_jiao` 或 `shi_juan_xue_sheng_da_ti`。
- 只允许创建教师软删除自己的试卷；仍有 `PUBLISHED` 或 `CLOSED` 班级发布时，服务端拒绝删除。
- 已删除本体的历史 release 仍只允许原发布教师通过发布历史查看；当前发布、撤回和新发布仍受 ACTIVE 任课关系约束。
- `AUTOMATED_VERIFIED` 不等同于用户人工验收通过。
