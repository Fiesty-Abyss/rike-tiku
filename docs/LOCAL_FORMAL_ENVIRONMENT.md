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

## 启动复验

已执行后端→前端、前端→后端、双端关闭后后端→前端三种冷/热顺序；每次 health 与前端均为 200，CORS 为 8080，Flyway 保持 V14，运行库保持 `rike_tiku`，AI 数据库配置在重启后仍可读取。IDE 配置文件已核对并被 ignore；由于当前自动化环境没有可调用的桌面控制接口，“在 IDEA/WebStorm 中手动点击 Run”留给用户本人按上述同等配置确认，不冒充机器 PASS。

## 正式账号复验与恢复

三位教师和六位学生均使用真实随机 PNG CAPTCHA 完成登录，均要求首次改密。主教师多角色账号完成一次真实初始密码修改，随后通过受控本机 SQL 恢复原 BCrypt 摘要和首次改密标识；其他账号未改变。最终 9 个账号全部回到统一初始密码及首次改密状态，事务表保持空白。

## 数据边界

- 正式库只用于本机答辩与后续个人使用，不作为自动化测试库。
- 匿名截图、模板与论文资料使用 `rike_tiku_demo` 或虚构数据。
- 仓库不保存正式姓名、账号文件、数据库备份、数据库 dump 或真实 Key。
- Docker 环境在本机不可用，因此本轮记录 `SKIPPED_DOCKER_ENVIRONMENT`；系统当前交付不依赖 Docker。
