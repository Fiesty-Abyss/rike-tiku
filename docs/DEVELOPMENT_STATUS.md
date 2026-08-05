# 开发状态

更新时间：2026-08-05

## 当前基线

- 设计基线：V3.0。
- 当前轮次：PR #7合并收尾。
- 当前分支：`main`。
- 班级管理普通merge提交：`02646fa`。
- 合并后回归基线：`main@02646fa`；本文件的后续文档提交将继续位于`main`。
- Pull Request：[#6](https://github.com/Fiesty-Abyss/rike-tiku/pull/6)，MERGED；远程功能分支已删除。
- 当前Flyway：V6；18张业务表。本轮没有新增或修改迁移。
- PR #7普通merge提交：`68cbb17`；远程`feat/student-import-preview`已删除。
- 当前Flyway：V6；本轮不新增或修改迁移，也不使用`dao_ru_pi_ci`。
- 合并后验证：`main@68cbb17`执行`mvn clean test`与`mvn clean package`均为25/25 PASS。

## 当前开发轮：学生Excel确认入库

- 当前分支：`feat/student-import-confirm`；当前PR尚未合并。
- 不新增V7，不使用`dao_ru_pi_ci`或批次表；确认接口使用单一事务写入账号、角色关系、学生档案和主班级关系。
- 成功响应只一次返回初始密码，数据库只保存BCrypt摘要；确认入库前重新执行预检查。

## 当前轮次：学生Excel模板与预检查

- 仅管理员可下载后端Apache POI生成的`.xlsx`模板，模板包含“学生导入”和“填写说明”两个Sheet、匿名示例和状态下拉。
- `POST /api/v1/admin/student-import/preview`在内存中解析固定Sheet，限制5MB和500行，逐行返回错误与预览字段；不回显初始密码。
- 预检查只读`ban_ji`、`yong_hu`与`xue_sheng_dang_an`，不写`yong_hu`、`yong_hu_jiao_se`、`xue_sheng_dang_an`、`ban_ji_xue_sheng`或`dao_ru_pi_ci`。
- PR #7已合并；确认入库留给下一独立轮次。详见`docs/STUDENT_IMPORT_PREVIEW_API.md`。

## 当前轮次：管理员班级基础管理后端

已实现并验证：

- `GET /api/v1/admin/classes`：分页和编码、名称、年级、状态筛选。
- `GET /api/v1/admin/classes/{id}`、创建、修改和状态变更接口。
- 创建默认`ACTIVE`；编码全局唯一且不允许通过修改接口变更；状态限定为`ACTIVE`、`GRADUATED`、`DISABLED`。
- 所有接口要求`ROLE_ADMIN`；未登录401，学生/教师403，首次登录未改密管理员继续受服务端门禁限制。
- 不提供删除接口；返回不暴露逻辑删除或内部审计字段。

已验证：

- 班级创建、默认状态、重复编码、空值、年份、分页与全部筛选、详情、修改、状态变更及非法状态：PASS。
- 未登录、学生、教师、管理员和首次登录管理员权限边界：PASS。
- 认证与数据库既有测试继续通过；`mvn clean package`：PASS，24/24。
- 随机临时库从V1迁移至V6后启动JAR：健康接口`UP/UP`、未登录班级接口401、ADMIN Token访问200：PASS。
- 临时库`rike_tiku_jar_verify_f62284422a6444b38fbfed4dd5bf65d4`已删除；正式库`rike_tiku.yong_hu`复查为0行。

接口细节见[管理员班级管理接口](ADMIN_CLASS_MANAGEMENT_API.md)。PR #6已普通merge，因此`main`已包含本轮班级管理后端。

## 当前未实施

- 班级管理前端。
- 管理员学生Excel确认入库、学生管理、教师管理和任课关系管理。
- 题库业务API、练习、判分、错题和AI Provider。
- 自由注册、邀请码、Refresh Token、Token表、找回密码、验证码、Redis、WebSocket、Docker和微服务。

## 已知边界

- 班级删除业务尚无明确需求，本轮没有删除接口；未来如有需求，只允许在独立任务中设计逻辑删除与关联数据边界。
- 账号状态或角色变动不会即时撤销已签发的两小时无状态访问Token；当前MVP不引入撤销表或Refresh Token。
- V1/V2旧迁移仍会产生MySQL整数显示宽度弃用警告；已执行迁移不改写。
- JDK 25下Mockito/Byte Buddy仍会产生动态Agent兼容性警告，测试实际通过。

## 已合并历史

- PR [#6](https://github.com/Fiesty-Abyss/rike-tiku/pull/6)：管理员班级基础管理后端，已普通merge到`main`，合并提交`02646fa`。
- PR [#5](https://github.com/Fiesty-Abyss/rike-tiku/pull/5)：前端认证基础，已普通merge到`main`；三角色登录、Pinia认证状态、Axios Bearer注入、会话恢复、首次改密、路由守卫和最小工作台已完成并经人工浏览器联调验证。
- PR [#4](https://github.com/Fiesty-Abyss/rike-tiku/pull/4)：后端统一认证与JWT，已普通merge到`main`；登录、`/auth/me`、首次改密、JWT及三角色鉴权已完成。
- PR [#1](https://github.com/Fiesty-Abyss/rike-tiku/pull/1)：账号与教学组织数据库模型，已普通merge到`main`；V5/V6及现有班级、学生关系、任课关系数据模型已完成。

## 下一步唯一任务

PR #6合并后，单独实现管理员学生Excel批量导入后端；只接受存在且为`ACTIVE`状态的班级，不同时扩展前端、教师管理、题库、练习或AI。
