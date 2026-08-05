# 前端认证基础说明

更新时间：2026-08-05

## 已实现范围

- 三个入口：`/login/student`、`/login/teacher`、`/login/admin`，共用一个表单组件。
- Pinia认证状态：Token、当前用户、角色、首次改密状态和初始化状态。
- `localStorage`仅保存访问Token、Token类型和有效期；不保存密码或完整用户响应。
- Axios请求拦截器统一注入Bearer Token；401、无效/过期Token清理会话；首次改密和无权限错误分别处理。
- 启动或刷新时调用`GET /api/v1/auth/me`恢复当前用户；失败即退出。
- `/change-initial-password`完成初始密码修改并替换后端返回的新Token。
- 路由守卫实现公开、认证、首次改密和角色限制；多角色账号保留全部真实角色。
- `/student`、`/teacher`、`/admin`仅为认证后的最小占位工作台。

## 明确未实现

学生导入、用户与班级管理、题库、练习、错题、AI和正式工作台业务均未实现；没有新增数据库迁移或后端接口。

## 本轮验证

- `npm ci --no-audit --no-fund`：PASS
- `npm run type-check`：PASS
- `npm test`：PASS，26项（含Axios认证拦截器10项）
- `npm run build`：PASS
- `mvn clean test`：PASS，23/23

随机临时库`rike_tiku_frontend_auth_9e5ef4b6882d4e998626d1069b8be3e1`已完成V1-V6迁移并注入匿名账号，后端健康检查`UP/UP`；验证结束后已停止后端并删除临时库，正式库`yong_hu`仍为0行。受本地启动环境限制，浏览器真实登录、改密和刷新会话联调仍为NOT_RUN，不得作为PR合并依据。
