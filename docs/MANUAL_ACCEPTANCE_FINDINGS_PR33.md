# PR #33 人工验收事实台账

更新时间：2026-08-14
真人状态：`FINAL_USER_REVIEW_PENDING`

机器测试、匿名 Demo、Mock/夹具和 Provider 合同测试均不替代用户真人复验。此前泄露的智谱凭据已视为永久失效，本轮未读取、调用、保存或复述；真实 Provider 分项为 `BLOCKED_EXTERNAL_PROVIDER`。

| 编号 | 用户现象与根因 | 最终实现 | 自动化与机器浏览器证据 | 实现提交 | 真人复验 |
|---|---|---|---|---|---|
| MA33-01 | 默认首页与论文资料口径落后 | PR #33 README 以 18 个可见功能章节连接截图、精确代码、表、Flyway 与论文资料；明确 main 仍是 PR #32 基线 | README 本地链接/图片门禁；31 条匿名 Demo 路线 | 最终资料提交 | FINAL_USER_REVIEW_PENDING |
| MA33-02 | 忘记密码入口缺 CSS，Dialog 曾被容器裁切 | Aqua Future 轻量入口、append-to-body、全视口遮罩、独立滚动和移动端宽度 | 组件交互测试；桌面/390px 截图 25/26 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-03 | 变式错误被笼统映射，旧 Schema 要求模型回显内部 ID，写入非原子 | Schema V2、字段级 Parser、一次修复、六类变化方式、新颖度、单事务候选/评价/SUCCESS/实例，失败短事务 | Fake Provider 单/多选/填空、1–5 难度、回滚与 PENDING；09/10 明示 UI 夹具 | `5daa22f`、`27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-04 | 答对题默认折叠 STANDARD 和答疑 | 所有已提交题默认展开 STANDARD；答对题保留当前题答疑与变式，错因只用于答错题 | 结果页单测与 05/08 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-05 | Demo 内部“覆盖/变式”标签泄露到学生与 AI 路径 | 只对带受控 Demo 标识的前缀做统一展示规范化，覆盖练习、结果、错题、专题、试卷、打印、类似题与 Prompt | 正常“覆盖率/覆盖范围/植被覆盖”保留专项 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-06 | 主观专题是一题一页、附件与 AI 上下文不足 | V26 专题单元引用 2–3 道 `ti_mu`，支持五类专题、STEM/OPTION/ANALYSIS 附件、本地草稿、STANDARD、10 轮专题 AI 与 PENDING 变式 | `TopicLearningIntegrationTest`；28 截图 | `12c5a13` | FINAL_USER_REVIEW_PENDING |
| MA33-07 | 答案 JSON 直接显示 | `AnswerDisplay` 安全解析单选、多选、填空、主观和非法结构；诊断 JSON 默认折叠 | 组件五类结构测试；39 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-08 | GLM Base64/错误分类与 1×1 测试图不符合契约 | raw Base64、受控 HTTPS、防 SSRF、非零科学测试图、GLM/xAI 独立诊断与显式选择 | Vision 合同测试；38 截图；真实调用未执行 | `12c5a13` | FINAL_USER_REVIEW_PENDING |
| MA33-09 | 私有题、知识卡片和图片资料链不完整 | V20/V21/V28/V29 完成范围私有题、附件、审核卡片、收藏/掌握、零基础讲解与统一生成练习 | 跨班/管理员不可见、附件权限、卡片生成原子回滚；30/32/33 截图 | `12c5a13`、`df9f0ba`、`c08c89a` | FINAL_USER_REVIEW_PENDING |
| MA33-10 | 私信缺撤回/仅本人删除，操作按钮和确认定位不一致 | V22 软隐藏/撤回；低干扰菜单与居中 `ElMessageBox`，双方语义分离 | 后端并发/权限测试；浏览器真实 API 交互截图 40–42 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-11 | 经典题导入与 AI 关系不清 | 7/19 列模板、Preview/Confirm、PENDING→人工审核→PUBLISHED；页面说明导入不会训练模型 | 模板集成测试；36/37 截图 | `27dfcec` | FINAL_USER_REVIEW_PENDING |
| MA33-12 | 本地临时目录与备份风险 | 恢复包和正式库备份均在仓库外；公开资料秘密/绝对路径扫描；不提交数据库备份 | SHA-256、非空检查、Git ignore/secret scan | 不产生产品提交 | FINAL_USER_REVIEW_PENDING |
| MA33-13 | 内存占用和重复进程缺证据 | 分阶段 PID/端口记录和 10 分钟三角色路由采样；只停止 RIKE PID | Java 278.7→295.2 MB，Vite 86.9→89.6 MB，非单调泄漏；停止后 18080/18081 释放 | 最终证据提交 | FINAL_USER_REVIEW_PENDING |

附加九项闭环：错题筛选/再做/软归档、非模态当前题答疑、专题单元和附件、GLM/xAI、试卷发布/学生提交/画像、AI 试卷质量建议、append-only 日志分页/CSV 导出、知识卡片库、知识卡片生成练习均纳入 V29/50 表与 210 项后端全量门禁。真实 Provider 结果仍必须逐项独立记录，不能由 Mock 推导。
