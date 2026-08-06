# 开发状态

更新时间：2026-08-05

## 当前开发轮

- 当前分支：`main`。
- PR [#9](https://github.com/Fiesty-Abyss/rike-tiku/pull/9) 已普通 merge 合并；合并提交：`ffdc7a553cfaf284e03196cf1e9ef75f657c9bb0`。原远程功能分支 `feat/admin-student-import-ui` 已删除。
- 任务：管理员班级管理与学生 Excel 导入前端业务闭环。
- 当前分支：`main`。PR [#10](https://github.com/Fiesty-Abyss/rike-tiku/pull/10) 已普通 merge 合并，合并提交为`9495ecced52291c82ee67c9f6229b141754e4998`；原远程功能分支已删除。
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

- 学生普通管理、教师管理、任课关系管理。
- 题库业务 API、练习、自动判分、错题、AI Provider、AI 答疑和正式学生/教师工作台。

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

单独实现教师管理与教师导入基础，随后再实施教师—班级—科目三元任课关系；这能在不触及题库和 AI 的前提下完成教学组织权限的剩余基础。
