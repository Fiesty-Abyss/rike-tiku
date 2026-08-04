# 数据库目录

数据库：MySQL 8.4 / `rike_tiku`  
字符集：`utf8mb4`  
排序规则：`utf8mb4_0900_ai_ci`

## 事实来源

数据库结构的唯一事实来源是：

```text
rike-tiku-backend/src/main/resources/db/migration/
```

当前迁移为V1–V6。禁止修改已经执行的迁移，数据库升级必须新增版本。

## 目录内容

- `diagrams/rike_tiku_er.md`：便于论文和AI阅读的Mermaid ER图。
- `schema/rike_tiku_schema.sql`：从真实本机数据库自动导出的无数据结构快照。

结构快照不作为建表、恢复或升级入口，也不与Flyway维护两套独立DDL。每次结构迁移完成并验证后重新从真实数据库导出即可。

## 安全导出

密码只能通过临时环境变量、MySQL登录路径或交互提示提供，不写入命令脚本或仓库文件。例如：

```powershell
$env:MYSQL_PWD="your-local-password"
mysqldump --host=localhost --port=3306 --user=root `
  --no-data --routines=false --triggers=false `
  --result-file=database/schema/rike_tiku_schema.sql rike_tiku
Remove-Item Env:MYSQL_PWD
```

## 当前结构

- 题库核心10表：科目、知识点、导入批次、题目、选项、解析、题目知识点、附件、来源、审核记录。
- 账号与教学组织8表：用户、角色、用户角色、学生档案、教师档案、班级、班级学生历史、三元任课关系。
- Flyway系统表：`flyway_schema_history`。

总业务表：18张。
