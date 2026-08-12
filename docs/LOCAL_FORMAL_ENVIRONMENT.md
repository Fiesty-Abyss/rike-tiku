# 本机正式运行环境

本文只记录可公开的运行方法和核验口径。正式账号姓名、初始账号清单、数据库密码、JWT secret 与 AI Key 保存在仓库外本机资料中，不进入 Git。

## 最终结构

- 数据库：`rike_tiku`，已先做仓库外 SQL 备份，再由现有 Flyway 从 V11 正规迁移至 V14。
- 结构：14 个成功迁移、35 张业务表，无 repair、无 V15。
- 题库：378 道 PUBLISHED；三科各 120 道普通题和 6 道专题题。
- 组织：3 位教师、6 位学生、2 个班级、6 条 ACTIVE 任课关系；每名学生恰有一个 ACTIVE 主班级。
- 初始状态：密码均为 BCrypt 摘要，9 个账号均启用首次改密门禁；学习、错题、私信和 AI 事务事实为空。
- AI：本机正式库可保留 TEXT/DeepSeek 与 VISION/GLM 两条启用配置；Key 只存本机数据库，API 只回显掩码。

## IDEA / WebStorm

本机已建立被 `.gitignore` 排除的 Run Configuration：后端使用 `rike_tiku`、端口 8081、允许源 `http://localhost:8080`；前端使用 8080 并把 API 指向 `http://localhost:8081/api/v1`。敏感数据库密码和 JWT secret 继续通过本机已有安全环境注入。

IDEA 配置名为 `RikeTikuBackendApplication`，WebStorm 配置名为 `RIKE Frontend`。本轮已在真实 IDEA 与 WebStorm 中分别点击 Run，并在 IDE 控制台确认 Spring Boot 完整启动和 Vite 监听成功，不再以命令行等价启动代替 IDE 结论。

WebStorm 必须使用 **npm Run Configuration**：`package.json` 指向前端项目，Command 为 `run`，Script 为 `dev`，Node interpreter 使用本机 Node 24。不要创建 JavaScript/TypeScript 配置直接运行 `src/main.ts`；浏览器入口需要 Vite 处理 Vue、CSS 与模块依赖，直接交给 Node 会产生 `ERR_UNKNOWN_FILE_EXTENSION`。正确控制台应显示 `vite` 和 `Local: http://localhost:8080/`。

后端关键变量：

```text
RIKE_TIKU_DB_NAME=rike_tiku
RIKE_TIKU_BACKEND_PORT=8081
RIKE_TIKU_CORS_ALLOWED_ORIGINS=http://localhost:8080
```

前端 `.env.local`：

```text
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

## 固定端口与旧实例回收

正式端口固定为前端 8080、后端 8081，不允许 Vite 自动漂移到其他端口。IDE 启动前可以执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/reclaim-rike-port.ps1 -Port 8080
powershell -ExecutionPolicy Bypass -File scripts/reclaim-rike-port.ps1 -Port 8081
```

脚本从自身位置推导仓库根目录，只会停止命令行明确属于当前 RIKE 仓库的 Vite/Node 或 Spring Boot/JAR 进程。若端口属于其他程序，脚本显示安全的程序名与 PID、返回非零状态并拒绝停止。日常使用仍优先在 IDEA/WebStorm 的 Run/Services 窗口点击红色 Stop；只有 IDE 已无对应实例而进程仍残留时才使用脚本。

## 启动复验

已通过真实 IDEA/WebStorm Run/Stop 验证后端→前端、前端→后端、后端重启而前端保持、关闭 Run Console 后再次点击 Run 四种顺序。每轮 8080/8081 均只有一个 RIKE listener，health 与前端为 200，CORS 为 8080，Flyway 保持 V14，运行库保持 `rike_tiku`，AI 数据库配置在重启后仍可读取。端口冲突根因是旧 RIKE Node/Java 实例残留；按精确 PID 停止后恢复正常。

## 正式账号复验与恢复

真实 Chrome 使用随机 PNG CAPTCHA 完成代表性账号流程：多角色教师、两名单角色教师、两个班级各一名学生走完首次改密与角色/练习主链，并额外抽查同班账号。重复账号不机械复测；角色、班级与学科差异由代表流程和数据库约束共同核验。测试后通过受控事务恢复 9 个账号的 BCrypt 初始密码摘要与首次改密标识，并清空练习、错题、AI、候选、私信和调用日志事实。

管理员学生/教师页面现提供单人和批量“恢复默认密码”。默认密码由 `RIKE_TIKU_DEFAULT_RESET_PASSWORD`（未配置时为 `a1234567`）控制；数据库仅保存 BCrypt，响应仅当次显示且禁止缓存，恢复后必须首次改密。忘记密码应联系管理员，不存在公共匿名重置 API。若管理员恢复了当前登录账号，应立即手动退出；现有 JWT 在到期前不做服务端集中撤销。

新增恢复界面的真实 CAPTCHA 页面验收由用户本人按最终清单执行，当前状态仍为 `FINAL_MANUAL_ACCEPTANCE_PENDING`；自动化、IDE 启动或既有代表流程不能替用户将该状态改为 PASS。

## 数据边界

- 正式库只用于本机答辩与后续个人使用，不作为自动化测试库。
- 匿名截图、模板与论文资料使用 `rike_tiku_demo` 或虚构数据。
- 仓库不保存正式姓名、账号文件、数据库备份、数据库 dump 或真实 Key。
- Docker 环境在本机不可用，因此本轮记录 `SKIPPED_DOCKER_ENVIRONMENT`；系统当前交付不依赖 Docker。
