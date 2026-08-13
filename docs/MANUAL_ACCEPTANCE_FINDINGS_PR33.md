# PR #33 人工验收第二轮事实台账

状态：`FINAL_USER_REVIEW_PENDING`

本表将用户的 13 组人工现象绑定到代码根因、提交与证据。机器测试、Mock/夹具和 Provider smoke 均不代替真人复验；用户明确确认前，每项真人状态固定为 `FINAL_USER_REVIEW_PENDING`。

| 编号 | 用户现象 | 代码根因 | 风险 | 修复范围 | 修复提交 | 自动化证据 | Provider 真实状态 | 真人复验 |
|---|---|---|---|---|---|---|---|---|
| MA33-01 | 默认 GitHub 首页无法看到 PR #33 最终资料 | `main` README 仍是 PR #32 口径，缺少跨分支导航 | HIGH | 仅 `main/README.md` 远程预览；合并回功能分支 | `473fbc7`、`cdc6032` | GitHub Contents API；9 张 raw 图片 HTTP 200；默认首页 HTML 含预览标题 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-02 | 忘记密码 Dialog 被登录区域裁切，按钮样式粗糙 | Dialog 未 append 到 body，容器高度/滚动与移动端宽度边界不完整 | HIGH | 登录页、Dialog、样式及交互测试 | 本轮待提交 | 前端 Dialog 专项、type-check | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-03 | 学生 AI 变式真实生成失败且无目标难度 | 请求未携带目标难度，服务异常未稳定映射，任务/候选/实例缺少同事务收口 | HIGH | DTO、Controller、Service、前端、错误映射与真实 smoke | 本轮待提交 | DTO 1–5 校验、确定性判分、候选基数与事务测试；Key 未配置 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-04 | 答对题默认不展示 STANDARD 与答疑入口 | 结果页以正确状态作为解析默认折叠条件 | MEDIUM | 结果页、AI 面板与测试 | 本轮待提交 | 前端结果页专项、type-check | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-05 | 学生题干显示“覆盖”等内部 Demo 标记 | Demo 种子把内部分类拼入可见题干，缺少精确展示规范化 | HIGH | DemoDataService、统一展示规范化、各读路径、数据统计 | PENDING | PENDING | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-06 | 综合/计算/流程/分析题阅读与 AI 支持不足 | SUBJECTIVE 专题缺受控子类型与统一 AI 上下文 | HIGH | V20/V23、Topic API/UI、复用学生 AI 会话 | 本轮待提交 | 随机库 V1→V23；专题筛选/本地草稿；互斥上下文编译/type-check | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-07 | 审核页直接展示答案 JSON | 页面直接把结构字段放进 `pre`，未复用统一答案组件 | HIGH | AnswerDisplay、管理员/教师审核、变式结果 | PENDING | PENDING | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-08 | GLM Vision 真实连接格式与测试图不符合当前官方契约 | Base64 使用 data URL；连接测试图视觉信息不足 | HIGH | Provider 请求、Parser、安全测试图与 smoke | 本轮待提交 | 官方当前 raw Base64 契约；Provider 单测；Key 未配置 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-09 | 缺少教师班级私有题库及图片/公式资料 | 题目模型只有全局可见语义；知识资料能力过窄 | CRITICAL | V20/V21、范围权限、教师题库/导入/附件、知识卡片 | 本轮待提交 | 随机库约束；基础教师私有题 API/UI 已实现；完整附件/导入仍待补齐 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-10 | 私信不能撤回或仅自己删除 | 消息表虽有软删除但缺双方独立可见性和撤回事实 | HIGH | V22、消息 API/UI、并发与摘要测试 | 本轮待提交 | `SiXinIntegrationTest` 撤回/独立隐藏；前端 type-check | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-11 | 经典题导入与 AI 关系说明不清 | UI 与文档缺少从模板到审核发布的内嵌说明 | MEDIUM | README、Excel 指南、管理员/教师导入页 | 本轮待提交 | 管理员导入页已有模板、Preview→Confirm→审核与 AI 关系说明 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-12 | 本地三个目录可能含冗余、备份或秘密 | 历史临时证据未清理，备份目录仅有一份可恢复备份 | HIGH | 仓库外目录安全审计与最小清理 | 不产生 Git 提交 | `_LOCAL` 不存在；唯一备份保留并校验 SHA-256；无引用临时目录安全删除 | NOT_RUN | FINAL_USER_REVIEW_PENDING |
| MA33-13 | 开发环境内存占用与重复进程不明 | 尚无分阶段 PID/端口/JVM 事实测量 | MEDIUM | 基线、联合运行、10 分钟稳定性和退出清理 | PENDING | PENDING | NOT_RUN | FINAL_USER_REVIEW_PENDING |
