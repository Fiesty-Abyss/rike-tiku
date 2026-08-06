# 开发状态

更新时间：2026-08-06

## 当前开发轮

- 当前基线：`main@f499f0c2e1e3b4637d22480868e94dbdacdcbaa0`。
- PR [#9](https://github.com/Fiesty-Abyss/rike-tiku/pull/9) 已普通 merge 合并；合并提交：`ffdc7a553cfaf284e03196cf1e9ef75f657c9bb0`。原远程功能分支 `feat/admin-student-import-ui` 已删除。
- PR #12 的管理员 MVP30 题库导入已普通 merge；不开发练习、判分、错题或 AI。
- PR [#10](https://github.com/Fiesty-Abyss/rike-tiku/pull/10) 已普通 merge 合并，合并提交为`9495ecced52291c82ee67c9f6229b141754e4998`；原远程功能分支已删除。
- PR [#11](https://github.com/Fiesty-Abyss/rike-tiku/pull/11) 已普通 merge 合并，合并提交为`dda66d4c7b530b9af44c692aa4d03027718a5e65`；远程 `feat/admin-question-review` 已删除。
- Flyway：V1–V6，18 张业务表；本轮未修改迁移或新增表。

## 已实现并验证

- 管理员布局：`/admin`、`/admin/classes`、`/admin/students/import` 均受现有真实 ADMIN 路由守卫保护。
- 班级页面：分页、编码/名称/年级/状态筛选、重置、刷新、创建、编辑与状态切换；没有删除入口，更新请求不提交班级编码。
- 学生导入：带 Bearer Token 的模板 Blob 下载；`.xlsx` 单文件及 5MB 前端校验；原始文件 multipart 预检查与确认；无效行禁止确认；一次性账号结果仅存组件内存；使用 `write-excel-file@4.1.1` 写出真正的账号发放表。
- CORS 最小修正：允许既有班级更新接口所需的 `PUT`、`PATCH` 预检方法。
- JWT 篡改用例：将签名段首字符确定性篡改，避免最后一个 Base64URL 非有效位变化造成的偶发假阳性。

## 验证

- 前端：`npm test` 31/31 PASS；`npm run type-check` PASS；`npm run build` PASS。
- 依赖：`npm audit` 为 0 vulnerabilities；原 `xlsx@0.18.5` 已移除。
- 后端：`mvn clean test` 26/26 PASS；`mvn clean package` 26/26 PASS。
- 合并后回归（`main@ffdc7a5`）：前端 31/31、类型检查、构建、`npm audit` 0 vulnerabilities；后端 26/26 测试及打包均 PASS。
- 浏览器：随机临时库已从 V1–V6 迁移；管理员登录、班级列表/创建/编辑/状态切换、模板请求、合法和错误 Excel 预检查、确认入库、账号结果与下载按钮、导入学生首次改密均已验证。浏览器控制台无 error。
- 清理：临时库 `rike_tiku_ui_e2e_019fd`、测试 Excel 和本地前后端进程已删除/停止；正式 `rike_tiku.yong_hu` 为 0 行。

## 当前未实施

- 学生普通管理、练习、自动判分、错题、AI Provider、AI 答疑和正式学生/教师工作台。
- 练习、自动判分、错题和 AI；题库审核发布已进入 `main`。

## 已知边界

- 班级没有删除接口。
- 初始密码结果刷新后不可恢复，不提供再次查询。
- `write-excel-file@4.1.1` 只接收确认接口返回的受控账号数据以写出 `.xlsx`，从不读取、解析或处理用户上传的Excel；依赖审计目前没有High风险。
- 本机没有Excel、WPS或LibreOffice可执行程序；生成文件已做OOXML结构与中文/列顺序/密码文本校验，桌面应用打开验证为`NOT_RUN`。
- JDK 25 下 Mockito/Byte Buddy 动态 Agent 警告仍存在，测试实际通过。
- 三元关系受既有唯一键限制：结束或停用的同一三元组合不重新插入，也不重新启用，以保留单条历史记录的结束状态。
- 本轮验证：后端`mvn clean test`30/30 PASS、`mvn clean package` PASS；前端`npm test`35/35 PASS、类型检查与构建 PASS、`npm audit`为0 vulnerabilities。随机临时库从V1–V6迁移后，已在真实浏览器完成管理员登录、教师创建、一次性密码显示、任课创建、重复拒绝、结束和历史查询；临时库与进程已清理。
- PR #10 合并后在`main@9495ecc`回归：后端30/30与打包 PASS；前端35/35、类型检查、构建、审计0漏洞 PASS。

## 下一轮建议

管理员 MVP30 题库导入已进入 `main`；下一模块必须在新的独立任务中确定。

## MVP30 管理员题库导入（已合并至 `main`）

- 接口：`POST /api/v1/admin/question-import/preview` 与 `POST /api/v1/admin/question-import/confirm`，均使用 multipart 字段 `file`；确认请求还必须携带预检查返回的文件哈希。
- 预检查不写业务表；确认时服务端重新解析原文件、复验全部规则，并在一个事务内写入既有批次、题目、选项、解析、知识点、附件、来源与审核记录表。
- 严格附件规则按正文对象标识及文件名精确匹配；没有唯一匹配或文件缺失即逐行无效，整批确认禁用，绝不按目录数量或模糊名称猜测。
- 纯 V1–V6 真实基线（不预置 Excel 知识点）：物理 0/10 有效、化学 1/10 有效、生物 1/10 有效；错误同时覆盖 `KNOWLEDGE_POINT_NOT_FOUND` 与附件完整性错误。预检查前后导入批次、题目及全部题目子表行数不变。
- 附件专项基线（仅在测试事务预置 Excel 所需知识点后）：物理 2/10 有效、化学 1/10 有效、生物 6/10 有效。其余行因声明图片数与正文 IMAGE 对象不一致、对象缺失或多候选被阻断；此数据只用于隔离附件问题，不能表示 V1–V6 数据库天然可导入数量。
- 原始三份 Excel、Flyway V1–V6 和正式 `rike_tiku` 均未修改。
- PR #12 合并后回归（`main@f499f0c`）：后端 `mvn clean test` 54/54、`mvn clean package` 通过；前端 `npm test` 56/56、`npm run type-check`、`npm run build` 通过，`npm audit` 为 0 vulnerabilities。
- 本轮随机临时库联调：`PASS_WITH_ENV_LIMITATION`。管理员通过浏览器完成登录、导入页访问、导入后题库列表/详情回查、版权发布拒绝与刷新持久化；匿名临时物理题通过真实 HTTP multipart 预检查 1/1、确认 1/1，批次、PENDING 题目和 STANDARD 解析、三条 `COPYRIGHT_UNKNOWN` 受控来源及当前管理员的 `SUBMITTED` 记录均已核对。内置浏览器无法操作系统本地文件选择器，因此上传步骤由真实 HTTP multipart 完成；该匿名题不是 MVP30 正式入库数据。
- PR #12 已普通 merge 至 `main`，合并提交：`f499f0c2e1e3b4637d22480868e94dbdacdcbaa0`；远程 `feat/admin-question-import` 已删除。原始 MVP30 三份 Excel 仍未确认入库。
## 题库审核发布（已合并至 `main`）

- PR #11 已普通 merge 至 `main`，合并提交：`dda66d4c7b530b9af44c692aa4d03027718a5e65`；未修改 Flyway V1–V6，继续使用既有题目、来源、解析、审核轨迹和附件表。
- 已实现：管理员题目分页和组合筛选、草稿创建/编辑、详情、受控附件信息、来源与版权复核，以及 `DRAFT → PENDING → PUBLISHED → DISABLED → PUBLISHED` 状态机；仅 `DRAFT` 可编辑。
- 合并后验证（`main@dda66d4`）：后端 `mvn clean test` 44/44、`mvn clean package` PASS；前端 `npm test` 51/51、`npm run type-check`、`npm run build` PASS、`npm audit` 为 0 vulnerabilities。题库专项测试使用随机临时 MySQL 库并从 V1–V6 完整迁移。
- 浏览器联调：随机临时 MySQL 库已完整执行 V1–V6；ADMIN 登录、PENDING 样本详情、单选草稿创建/编辑/提交、合法来源发布、停用、重新发布、多选/填空/主观题动态表单、版权拒绝、退回草稿后再次编辑、刷新持久化均已完成。STUDENT、TEACHER 与首次未改密 ADMIN 调用管理员题库接口均为 403，浏览器控制台无 error。正式 `rike_tiku` 未写入联调数据；临时库、临时配置和前后端进程均已清理。
- 已知结构边界：V1–V6 的 `ti_mu` 没有创建人字段；创建请求不接受前端创建人 ID，审核人则由认证上下文写入既有审核记录。不得伪造创建人持久化结果。
- MVP30 三份 Excel 已只读核对，共 30 道；没有修改原文件、没有写入正式数据库、没有实现批量导入。详细映射和风险见 `ADMIN_QUESTION_REVIEW_API.md`。
