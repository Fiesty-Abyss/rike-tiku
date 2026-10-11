# 数据库目录

- 数据库：MySQL 8.4（推荐 8.4 LTS）
- 字符集 / 排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`

## 事实来源

数据库结构的唯一事实来源是 Flyway 迁移脚本：

```text
rike-tiku-backend/src/main/resources/db/migration/   # V1–V30，50 张业务表
```

后端启动时自动执行迁移（`spring.flyway.validate-on-migrate=true`，`clean` 被禁用）。**已发布的迁移不得修改**，结构变更一律新增 `V{n}__*.sql`。

## 目录内容

| 文件 | 说明 |
|---|---|
| `schema_snapshot_v30.sql` | 由隔离库导出的 V30 无数据结构快照，便于离线查阅；不作为建表或升级入口 |
| `diagrams/rike_tiku_er.md` | 按业务模块划分的 Mermaid ER 图 |

字段级说明见 [docs/DATABASE_SCHEMA_REFERENCE.md](../docs/DATABASE_SCHEMA_REFERENCE.md)，可用 `scripts/generate-schema-reference.py` 重新生成。

## 导出结构快照

密码只通过临时环境变量或交互提示提供，不写入脚本或仓库：

```bash
MYSQL_PWD=... mysqldump --host=localhost --port=3306 --user=root \
  --no-data --routines=false --triggers=false --skip-comments \
  --result-file=database/schema_snapshot_vNN.sql rike_tiku
```

## 业务模块概览

- 题库核心：科目、知识点、导入批次、题目、选项、解析、题目知识点、附件、来源、审核记录
- 账号与教学组织：用户、角色、用户角色、学生 / 教师档案、班级、班级学生历史、三元任课关系
- 学习：练习会话、作答、错题、掌握度、规则推荐
- 教学：高频考点、组卷、试卷发布、学生提交与逐题作答、专题单元
- 互动：师生私信会话与消息、个人中心
- AI：模型配置、调用日志、答疑会话、候选题生成与评价、视觉缓存
- 审计：管理员操作日志
