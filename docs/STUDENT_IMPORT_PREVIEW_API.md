# 管理员学生Excel导入模板与预检查接口

当前实现位于`feat/student-import-preview`。本轮只生成模板并返回预览，不会创建任何账号、学生档案或班级学生关系。

## 权限和边界

- 统一前缀：`/api/v1/admin/student-import`；所有接口仅限`ROLE_ADMIN`。
- 未登录返回401，学生和教师返回403，首次登录未改密管理员继续受服务端门禁限制。
- 不新增Flyway，不使用`dao_ru_pi_ci`，不保存上传文件或预检查结果。

## 下载模板

`GET /api/v1/admin/student-import/template`

后端使用Apache POI生成有效`.xlsx`。工作表“学生导入”固定列为：

`xue_hao`、`xing_ming`、`ban_ji_bian_ma`、`nian_ji`、`yong_hu_ming`、`chu_shi_mi_ma`、`zhang_hao_zhuang_tai`。

模板有匿名示例、填写说明、加粗表头、冻结首行和状态下拉（`ENABLED`、`DISABLED`）；没有宏、公式、真实账号或统一初始密码。

## 上传预检查

`POST /api/v1/admin/student-import/preview`，`multipart/form-data`，字段名`file`。

- 仅接受`.xlsx`，最大5MB，最多500个非空数据行；固定读取“学生导入”Sheet。
- 公式、损坏工作簿、缺失Sheet或表头会直接拒绝；空白行忽略。
- 学号、姓名、班级编码和年级必填。班级必须存在且为`ACTIVE`，年级必须匹配。
- 用户名为空时使用学号；状态为空时为`ENABLED`。初始密码为空表示确认入库阶段随机生成；非空仅校验8至64位且含字母和数字。
- 预览不会回显密码，使用`passwordProvided`和`passwordWillGenerate`标记。

响应包含文件名、总数、有效/无效数和逐行结果。逐行结果含原Excel行号、归一化字段、`VALID/INVALID`和错误数组。

常见错误码：`FILE_EMPTY`、`FILE_TYPE_INVALID`、`FILE_TOO_LARGE`、`WORKBOOK_INVALID`、`SHEET_NOT_FOUND`、`ROW_LIMIT_EXCEEDED`、`STUDENT_NUMBER_REQUIRED`、`STUDENT_NUMBER_DUPLICATE_IN_FILE`、`STUDENT_NUMBER_ALREADY_EXISTS`、`NAME_REQUIRED`、`NAME_INVALID`、`CLASS_CODE_REQUIRED`、`CLASS_NOT_FOUND`、`CLASS_NOT_ACTIVE`、`GRADE_REQUIRED`、`GRADE_CLASS_MISMATCH`、`USERNAME_DUPLICATE_IN_FILE`、`USERNAME_ALREADY_EXISTS`、`USERNAME_INVALID`、`PASSWORD_POLICY_VIOLATION`、`INVALID_ACCOUNT_STATUS`、`FORMULA_CELL_NOT_ALLOWED`。

## 后续边界

下一独立轮次才会设计确认入库、随机密码交付和批次生命周期；本轮不得将预检查结果视为已导入学生。
