# 管理员班级基础管理接口

本接口属于管理员班级基础管理后端。当前实现分支为`feat/admin-class-management`，草稿PR未合并前不代表`main`已具备此功能。

## 认证与边界

- 统一前缀：`/api/v1/admin/classes`。
- 所有接口要求`ROLE_ADMIN`；无Token返回401，学生和教师返回403。
- 首次登录且尚未改密的管理员仍受现有服务端门禁限制，不能访问这些接口。
- 本轮不提供删除接口、不创建班级前端，也不包含学生导入、教师管理或任课关系。

## 接口

### 分页查询

`GET /api/v1/admin/classes?page=1&size=10&code=&name=&grade=&status=`

- `page`从1开始，`size`范围1至100。
- `code`、`name`为包含匹配；`grade`和`status`为精确匹配。
- 返回MyBatis-Plus分页字段：`records`、`total`、`current`、`size`、`pages`。

### 详情

`GET /api/v1/admin/classes/{id}`

不存在或已逻辑删除时返回`404 CLASS_NOT_FOUND`。

### 创建

`POST /api/v1/admin/classes`

```json
{
  "classCode": "G1-01",
  "className": "高一一班",
  "grade": "高一",
  "enrollmentYear": 2026
}
```

创建后状态固定为`ACTIVE`。字段会去除首尾空格；编码最多64字符、名称最多128字符、年级最多32字符，年份范围为2000至2100。重复编码返回`409 CLASS_CODE_EXISTS`。

### 修改

`PUT /api/v1/admin/classes/{id}`

```json
{
  "className": "高一一班（理科）",
  "grade": "高一",
  "enrollmentYear": 2026
}
```

只能修改名称、年级和入学年份；请求中不存在`classCode`，班级编码保持不变。

### 修改状态

`PATCH /api/v1/admin/classes/{id}/status`

```json
{
  "status": "DISABLED"
}
```

状态只接受`ACTIVE`、`GRADUATED`、`DISABLED`；其他值返回`400 INVALID_CLASS_STATUS`。

## 返回字段

接口返回班级公开业务字段：`id`、`classCode`、`className`、`grade`、`enrollmentYear`、`status`。不直接暴露逻辑删除和内部审计字段。
