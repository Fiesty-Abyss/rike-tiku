# 开发状态

更新时间：2026-08-06

## 当前主线状态

- 本轮开始基线：`main@2161080427fd432634325bea3c3d1ebd7e0f519a`。
- 当前分支：`feat/demo-data-manual-acceptance`，演示验收环境尚未合并。PR #13 已普通 merge，合并提交为 `db04fbc9caeeb5e4eb003a45581e62e76dbab420`。
- 学生自主练习、自动判分、结果与错题闭环已进入 `main`；AI、掌握度、推荐、教师任务、组卷考试和主观题评分仍未实现。
- Flyway：V1–V7，共 23 张业务表；V1–V6 未修改。

## 已进入 main

- `lian_xi_hui_hua`、`lian_xi_ti_mu`、`xue_sheng_da_ti`、`xue_xi_jie_guo`、`cuo_ti_ji_lu`，全部使用 BIGINT 主键、外键 `ON DELETE RESTRICT`、必要唯一键/索引和状态约束。
- 学生只能从 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题创建会话；会话创建后将题目、选项、答案、解析、知识点及顺序冻结。
- 提交整场答案在一个事务中完成锁定、答题事实、错题聚合、最终结果和会话状态切换；重复提交为 `409`。
- 单选严格匹配有效标识；多选以去重后的集合完全相等判分；填空按冻结空位、可接受答案、大小写及受控全半角/标点规则判分。
- 错误创建或累加错题；答对不删除历史，连续正确一次为 `REVIEWING`、两次为 `MASTERED`。
- 学生前端提供 `/student/practice`、`/student/practice/new`、`/student/practice/:id`、`/student/practice/:id/result`、`/student/wrong-questions`；未提交响应和页面均不显示答案或解析。

## 当前验证

- 合并后后端：`mvn clean test` 68/68 PASS；`mvn clean package` PASS。
- 合并后前端：`npm test` 68/68 PASS；`npm run type-check`、`npm run build` PASS。构建仅有既有大 chunk 提示。
- `npm audit`：0 vulnerabilities；最终敏感信息扫描通过。
- 真实 HTTP：`PASS`，已覆盖会话创建、未提交响应防泄露、权限和结果访问。学生页面回查：`NOT_RUN`，受控后台服务无法启动。综合结论：`PASS_WITH_ENV_LIMITATION`。

## 当前功能分支

- 显式 PowerShell 工具创建、重置、播种、校验和清理独立 `rike_tiku_demo`，正常应用启动不会自动写入演示数据。
- 固定三角色演示账号、学生/教师档案、演示班级、三科三元任课关系和九个知识点。
- 18 道自行编写、无附件、可自动判分的 `PUBLISHED` 演示题；每科 6 道，单选/多选/填空各 2 道，来源权利状态为 `USER_PROVIDED`，审核轨迹完整。
- 本轮不修改 V1–V7，不写正式 `rike_tiku`，不修改 MVP30 原始 Excel。
- 当前分支回归：后端 74/74、`mvn clean package` PASS；前端 68/68、类型检查和构建 PASS；`npm audit` 为 0 vulnerabilities。
- 真实脚本链 `reset → seed → validate → clean → reset → seed` PASS，末次 seed 后演示库保持待人工验收状态；正式库演示账号、演示题和 V7 五张学习表均为 0。
- 人工验收问题 MA-001 已关闭：后端 18081、前端 18080、`/api/v1` 基址和 `rike_tiku_demo` 连接均正确；demo_admin 登录、demo_teacher 真实 HTTP 登录和 demo_student 浏览器登录均已复验，原 `INVALID_CREDENTIALS` 不再复现。
- 用户已登记 MA-002 至 MA-009，覆盖学生三科学科入口与首页体验、登录角色识别、中文展示、个人中心、管理员单个学生管理、高频考点和受三元任课关系约束的师生私信。这些均为待规划事项，尚未实现。

## 已合并基线

- PR #10：教师与三元任课关系，普通 merge `9495ecc`。
- PR #11：管理员题库审核发布，普通 merge `dda66d4`。
- PR #12：管理员 MVP30 题库导入，普通 merge `f499f0c`；MVP30 原始 Excel 仍未确认入库。
- PR #13：学生练习、自动判分、结果与错题闭环，普通 merge `db04fbc`。

## 下一步

后续按以下顺序规划，不代表已经实现：

1. UI、认证和学生三科工作台；
2. 管理员学生手动管理；
3. 高频考点和受三元任课关系约束的师生私信；
4. 最后再接入 DeepSeek 与 GLM。

AI、教师任务、统计等正式工作台仍未实现。
