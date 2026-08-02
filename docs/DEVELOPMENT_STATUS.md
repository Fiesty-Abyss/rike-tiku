# 开发状态

更新时间：2026-08-02

## 当前基线

- 设计基线：V3.0
- 当前轮次：基础工程初始化与连通性验证
- 当前状态：DONE_VERIFIED
- Git 分支：`main`
- 远程仓库：未配置

## 本轮已完成

| 内容 | 状态 | 证据 |
|---|---|---|
| 初始化 `rike-tiku-backend` | DONE_VERIFIED | Java 25 编译；Spring Boot 4.1.0 启动；Maven 测试和打包通过 |
| 初始化 `rike-tiku-frontend` | DONE_VERIFIED | npm 依赖安装、TypeScript 检查、Vite 构建和开发服务器启动通过 |
| 创建 `rike_tiku` 数据库 | DONE_VERIFIED | MySQL 8.4.10 实际查询；字符集 `utf8mb4`；排序规则 `utf8mb4_0900_ai_ci` |
| 验证数据库为空 | DONE_VERIFIED | `information_schema.TABLES` 查询结果为 0 |
| 健康检查接口 | DONE_VERIFIED | `GET /api/v1/health` 返回 `status=UP`、`database=UP` |
| 数据库失败降级 | DONE_VERIFIED | 使用不可达端口启动临时实例，接口返回 HTTP 503、`database=DOWN` |
| CORS | DONE_VERIFIED | `http://localhost:8080` 获得允许头；非白名单 Origin 返回 HTTP 403 |
| 前后端浏览器联调 | DONE_VERIFIED | 页面真实显示后端和数据库 UP；重新检查成功；控制台无错误或警告 |

## 明确未实施

以下内容保持 NOT_STARTED：

- 正式业务表和数据库迁移
- 用户、角色、学生、教师、班级与任课业务
- 登录、注册和 JWT
- 题库、练习、错题和学习统计业务
- AI Provider、DeepSeek、Redis、MinIO、WebSocket、Docker 和微服务

## 本轮测试结论

- `mvn clean test`：PASS，4 tests，0 failures，0 errors
- `mvn clean package`：PASS，可执行 JAR 已生成
- `npm ci`：PASS
- `npm run type-check`：PASS
- `npm run build`：PASS
- 前端开发服务器：PASS，`http://localhost:8080`
- 后端应用：PASS，`http://localhost:8081`
- 浏览器联调：PASS

## 已知事项

- MyBatis-Plus 在没有 Mapper 时输出警告，符合本轮“不创建业务功能”的边界。
- Vite 构建提示主包超过 500 kB，原因是当前最小页直接引入完整 Element Plus；不影响本轮验证，后续正式页面阶段再按需拆包。
- JDK 25 下 Mockito/Byte Buddy 输出动态代理兼容性预警，但测试实际通过。
- 本轮曾验证 TypeScript 7.0.2 与 `vue-tsc` 3.3.9 不兼容，最终冻结为 TypeScript 6.0.3。

## 下一步

本轮已停止。未开始任何下一模块；后续任务需由用户单独下达。

