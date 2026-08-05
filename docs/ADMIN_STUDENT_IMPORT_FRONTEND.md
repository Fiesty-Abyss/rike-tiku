# 管理员班级与学生导入前端

该功能已通过普通 merge 合并至`main`：PR [#9](https://github.com/Fiesty-Abyss/rike-tiku/pull/9)，合并提交为`ffdc7a5`；原远程功能分支`feat/admin-student-import-ui`已删除。合并后已实际执行前端31/31、类型检查、构建、`npm audit`（0 vulnerabilities）以及后端26/26测试和打包。

## 路由与权限

| 路由 | 用途 | 前端门禁 |
| --- | --- | --- |
| `/admin` | 管理员工作台 | 真实角色包含 `ADMIN` |
| `/admin/classes` | 班级管理 | 真实角色包含 `ADMIN` |
| `/admin/students/import` | 学生 Excel 导入 | 真实角色包含 `ADMIN` |

三条路由复用现有 Pinia 认证状态和路由守卫；后端仍是最终权限边界。

## 班级管理

- 列表请求：`GET /api/v1/admin/classes`，传递分页及编码、名称、年级、状态筛选。
- 支持创建、编辑、状态切换；班级编码仅在创建时可编辑。
- 状态仅展示并提交 `ACTIVE`、`GRADUATED`、`DISABLED`，没有删除入口。
- 对 `CLASS_CODE_EXISTS`、`CLASS_NOT_FOUND`、网络错误和通用业务错误显示可读提示，不展示 SQL 或内部异常。

## 学生 Excel 导入

1. 模板通过带认证请求获取 Blob，并固定下载名为 `学生批量导入模板.xlsx`。
2. 文件只接受一个 `.xlsx`，不超过 5MB；重新选择会清除旧预览与账号结果。
3. 预检查请求 `POST /api/v1/admin/student-import/preview`，显示总数、有效/无效数、Excel 行号与字段错误，但绝不显示 Excel 密码内容。
4. 存在无效行时确认按钮禁用。确认请求 `POST /api/v1/admin/student-import/confirm`，再次上传当前原始文件，并在二次确认后提交。
5. 成功响应的初始密码只保留在页面组件内存；刷新页面、移除文件或清空敏感结果都会丢弃它。
6. `write-excel-file@4.1.1` 将当前确认响应生成 `学生账号发放表_时间戳.xlsx`，仅包含学号、姓名、班级、用户名、初始密码、账号状态和首次登录提示。所有值按文本写出，避免学号或初始密码被自动转换。

## 联调与安全

- CORS 允许 `GET`、`POST`、`PUT`、`PATCH`、`OPTIONS`，以支撑现有班级接口的浏览器预检。
- `IMPORT_CONFLICT` 提示整批未导入并保留文件、预览，方便重新预检查。
- 不把 Token 放在下载 URL；不将导入文件、预览或初始密码写入浏览器存储。
- 导出库只写出当前确认响应的受控数据，从不读取用户上传文件；`npm audit` 当前为0 vulnerabilities。实际OOXML校验已验证工作表名、中文、列顺序和密码文本；由于本机缺少Excel/WPS/LibreOffice，桌面应用打开验证为`NOT_RUN`。
