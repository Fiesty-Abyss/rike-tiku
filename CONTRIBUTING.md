# 参与贡献

感谢你愿意改进 RIKE！提交前请花几分钟阅读以下约定。

## 开发环境

| 组件 | 版本 |
|---|---|
| JDK | 25 |
| Maven | 3.9+ |
| Node.js | 22+（推荐 24） |
| MySQL | 8.4 |
| Python（可选，离线题库工具） | 3.11+ |

快速启动见 [README](README.md#快速开始)，演示数据见 [docs/DEMO_ENVIRONMENT.md](docs/DEMO_ENVIRONMENT.md)。

## 分支与提交

- 从 `main` 切出分支：`feat/…`、`fix/…`、`docs/…`、`refactor/…`、`chore/…`
- 提交信息使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 风格，例如 `fix(practice): 修复填空题等价答案判分`
- 一个 PR 聚焦一件事；界面改动请附截图
- 合并方式为普通 merge，不改写 `main` 历史

## 代码约定

- **数据库**：只能新增 Flyway 迁移 `V{n}__description.sql`，禁止修改已发布迁移；同步更新 `docs/DATABASE_SCHEMA_REFERENCE.md`（`scripts/generate-schema-reference.py`）
- **权限**：所有业务授权在服务端完成，教师范围以 `教师 + 班级 + 科目` 三元任课关系为准，不信任前端传入的角色或范围
- **判分**：正式判分只依据人工审核过的标准答案；AI 输出不得直接写入正式成绩或 STANDARD 解析
- **内容**：只提交自编或已获授权的题目；第三方材料放在本地 `question-bank/<学科>/`（已忽略），见 [CONTENT_NOTICE](CONTENT_NOTICE.md)
- **安全**：不要提交密码、JWT 密钥、API Key 或真实个人信息；配置一律走环境变量
- **前端**：数据库文本通过 `ScientificText` 等组件安全渲染，不使用 `v-html` 渲染题目内容

## 提交前自检

```bash
# 后端（需要本机 MySQL 8.4，账号需有建库权限）
cd rike-tiku-backend && mvn verify

# 前端
cd rike-tiku-frontend && npm ci && npm run type-check && npm test && npm run build

# 离线题库工具
cd tools/question-pipeline && python -m unittest test_parser.py
```

CI 会在每个 PR 上自动运行以上检查。
