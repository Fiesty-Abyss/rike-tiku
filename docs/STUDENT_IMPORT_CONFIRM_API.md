# 管理员学生Excel确认入库接口

`POST /api/v1/admin/student-import/confirm`仅允许ADMIN上传`.xlsx`。服务端重新解析并执行全部预检查；任意一行无效即拒绝整批。

全部有效后在一个事务中依次写入`yong_hu`、`yong_hu_jiao_se`、`xue_sheng_dang_an`和`ban_ji_xue_sheng`。角色按`STUDENT`代码查询，班级须仍为`ACTIVE`。任何冲突或插入异常都会整批回滚。

成功响应仅一次返回每名学生初始明文密码，并设置`Cache-Control: no-store`和`Pragma: no-cache`；数据库只保存BCrypt摘要。Excel未提供密码时使用SecureRandom生成12位、含大小写和数字且不含易混淆字符的独立密码。

不新增V7、不使用`dao_ru_pi_ci`、不保存上传文件/批次/预览结果，也不提供再次查询初始密码。重复提交会因学号或用户名已存在而整批失败。

非法文件沿用既有统一错误响应：前端可直接按错误码和`message`显示；逐行数据错误仍通过预检查接口先展示。本MVP不增加新的错误框架。并发唯一竞争由数据库约束兜底并转换为`IMPORT_CONFLICT`；尚未执行真实并发压测。
