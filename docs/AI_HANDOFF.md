# AI 开发交接

更新时间：2026-08-05

## 当前状态

当前工作在 `main`。管理员班级管理与学生 Excel 导入前端闭环已通过普通 merge 合并：PR [#9](https://github.com/Fiesty-Abyss/rike-tiku/pull/9)，合并提交 `ffdc7a553cfaf284e03196cf1e9ef75f657c9bb0`；原远程功能分支 `feat/admin-student-import-ui` 已删除。

### 本轮变更

- 新增 `src/api/admin/classes.ts` 与 `src/api/admin/studentImport.ts`，复用已有 Axios 实例与认证错误处理。
- 新增管理员布局和三个路由：`/admin`、`/admin/classes`、`/admin/students/import`。
- 班级页面仅调用已有五个后端接口；没有删除功能，编辑请求没有 `classCode`。
- 导入页面重新上传原始文件确认入库；预览 JSON 不参与确认。初始密码结果不写 `localStorage`、`sessionStorage` 或控制台。
- 使用 `write-excel-file@4.1.1` 取代存在High风险的`xlsx@0.18.5`，只用于前端账号发放表；`package-lock.json` 已同步。该库只写出受控确认响应，绝不读取或解析用户上传Excel。
- 为使浏览器能调用已有的班级 `PUT/PATCH` 接口，`SecurityConfig` 的 CORS 方法白名单补充 `PUT`、`PATCH`；没有新增业务接口。
- 认证集成测试将 JWT 篡改位置移至签名段首字符，修复最后一位 Base64URL 非有效位改变时仍可验签的测试偶发问题。

### 已完成验证

- 前端 31/31 测试、类型检查、生产构建：PASS。
- 后端 26/26 测试及打包：PASS。
- `npm audit`：0 vulnerabilities。
- PR #9 合并后在 `main@ffdc7a5` 回归：前端 31/31、类型检查、构建、审计均 PASS；后端 26/26 与打包均 PASS。
- 账号发放表已实际生成并通过OOXML工作表名、中文、列顺序和文本初始密码校验；本机没有Excel/WPS/LibreOffice，桌面应用打开验证为`NOT_RUN`。
- 真实浏览器临时库联调：PASS；管理员操作班级、模板请求、有效/无效导入预检查、确认入库、账号结果、下载按钮、导入学生首次改密均执行。
- 临时库、临时 Excel、前后端进程已清理，正式库 `rike_tiku.yong_hu` 为 0 行。

## 事实来源和限制

- 代码、Flyway、测试与 Git 优先于旧文档和历史会话。
- 不修改 V1–V6；首版角色固定为 STUDENT、TEACHER、ADMIN；学生不能自行获得角色。
- 班级删除、教师管理、任课关系、题库、练习、错题和 AI 均未实现。
- 后续不得把一次性初始密码持久化到浏览器存储或增加查询初始密码接口。

## 下一步

等待本分支审查和合并。后续只建议：教师管理基础与教师导入，再单独实现三元任课关系；不要提前开发题库、练习或 AI。
