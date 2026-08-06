# 开发状态

更新时间：2026-08-06

## 当前开发轮

- 开始基线：`main@4f10f6486de8f4d732abb6e52eeca7734bc3dfde`。
- 当前分支：`feat/student-practice-loop`；Draft PR #13 已创建、当前尚未合并。
- 本轮范围：学生自主练习、自动判分、结果与错题闭环；不开发 AI、掌握度、推荐、教师任务、组卷考试或主观题评分。
- Flyway：新增 `V7__create_student_practice_and_wrong_question_tables.sql`；V1–V6 未修改。业务表由 18 张增至 23 张。

## 当前分支已实现

- `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`、`cuo_ti_ji_lu`，全部使用 BIGINT 主键、外键 `ON DELETE RESTRICT`、必要唯一键/索引和状态约束。
- 学生只能从 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题创建会话；会话创建后将题目、选项、答案、解析、知识点及顺序冻结。
- 提交整场答案在一个事务中完成锁定、答题事实、错题聚合、最终结果和会话状态切换；重复提交为 `409`。
- 单选严格匹配有效标识；多选以去重后的集合完全相等判分；填空按冻结空位、可接受答案、大小写及受控全半角/标点规则判分。
- 错误创建或累加错题；答对不删除历史，连续正确一次为 `REVIEWING`、两次为 `MASTERED`。
- 学生前端提供 `/student/practice`、`/student/practice/new`、`/student/practice/:id`、`/student/practice/:id/result`、`/student/wrong-questions`；未提交响应和页面均不显示答案或解析。

## 当前验证

- 后端：`mvn clean test` 68/68 PASS；`mvn clean package` PASS。
- 前端：`npm test` 68/68 PASS；`npm run type-check`、`npm run build` PASS。构建仅有既有大 chunk 提示。
- `npm audit`：0 vulnerabilities；最终敏感信息扫描通过。
- 真实 HTTP：`PASS`，已覆盖会话创建、未提交响应防泄露、权限和结果访问。学生页面回查：`NOT_RUN`，受控后台服务无法启动。综合结论：`PASS_WITH_ENV_LIMITATION`。

## 已合并基线

- PR #10：教师与三元任课关系，普通 merge `9495ecc`。
- PR #11：管理员题库审核发布，普通 merge `dda66d4`。
- PR #12：管理员 MVP30 题库导入，普通 merge `f499f0c`；MVP30 原始 Excel 仍未确认入库。

## 下一步

PR #13 合并后，下一模块候选为最小 AI 错题答疑闭环。AI 当前尚未实现，本轮不得提前开发。
