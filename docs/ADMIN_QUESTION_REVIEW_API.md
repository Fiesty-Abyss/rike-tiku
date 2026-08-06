# 管理员题库审核发布接口

当前分支 `feat/admin-question-review` 在既有 V1–V6 表上提供管理员题目管理接口。所有接口均要求正常 `ROLE_ADMIN`；JWT、密码摘要和内部审计字段不会出现在响应中。

## 接口

- `GET /api/v1/admin/questions`：分页与学科、题型、使用模式、难度、状态、关键词、来源权利状态筛选。
- `GET /api/v1/admin/questions/{id}`：题目、答案 JSON、选项、标准解析、知识点、受控附件信息、来源、审核记录及可执行动作。
- `POST /api/v1/admin/questions`、`PUT /api/v1/admin/questions/{id}`：创建或编辑草稿；编辑仅限 `DRAFT`。
- `POST /api/v1/admin/questions/{id}/submit-review`：`DRAFT → PENDING`。
- `POST /api/v1/admin/questions/{id}/approve`：`PENDING → PUBLISHED`。
- `POST /api/v1/admin/questions/{id}/return`：`PENDING → DRAFT`，意见必填。
- `POST /api/v1/admin/questions/{id}/disable`：`PUBLISHED → DISABLED`。
- `POST /api/v1/admin/questions/{id}/republish`：`DISABLED → PUBLISHED`。
- `GET /api/v1/admin/knowledge-points?subjectId=`：只读知识点下拉数据；科目列表复用既有 `/api/v1/admin/subjects`。

创建、编辑和状态变更均在事务内完成。状态变更会同步当前未逻辑删除的 `STANDARD` 解析（版本号 1）到相同状态，并在写入审核记录前完成；审核记录写入失败会使题目与解析状态一起回滚。审核人由认证上下文取得，不接受前端提交。题干、答案、解析来源必须齐全；存在 `COPYRIGHT_UNKNOWN` 或 `RESTRICTED` 来源时不得发布。

详情响应包含完整题干（列表仅返回摘要）、受控附件业务元数据、标准答案 JSON、标准解析、知识点、三类来源、审核历史和允许动作；附件的 `xiang_dui_lu_jing` 不向前端返回。单选/多选答案必须使用 `optionLabels` 与正确选项一致；填空答案使用 `blanks[].acceptedAnswers`；主观题只能是 `TOPIC_LEARNING` 且不可自动判分。后端题库专项测试共 13 项，使用随机临时 MySQL 库从 V1–V6 迁移，并另由认证 HTTP 集成测试验证未登录、学生、教师和首次未改密管理员拒绝访问，正常管理员可访问。

`ti_mu` 在 V1–V6 中没有创建人字段。创建接口从认证上下文接收当前管理员且绝不接受客户端创建人 ID，但在不新增迁移的约束下不能将创建人持久化；审核人可准确写入既有 `ti_mu_shen_he_ji_lu.shen_he_ren_id`。

## MVP30 数据兼容核对

三份原始文件均仅以 `openpyxl` 只读方式检查，未改写文件，也未写入正式数据库：物理、化学、生物各 10 道，共 30 道；每份均有 `题目检查`、`质量统计` 两个 Sheet，题目表头位于第 2 行，未发现空白题目行或同文件题干重复。

| Excel 实际列 | 当前表映射 | 结论 |
| --- | --- | --- |
| 学科 | `ke_mu` / `ti_mu.ke_mu_id` | 需中文到物化生代码映射 |
| 题型、题干、答案、难度、难度说明 | `ti_mu` | 物理“实验填空题”“解答题”需映射为 `FILL_BLANK`、`SUBJECTIVE` 并按规则拆分 |
| 选项 | `ti_mu_xuan_xiang` | 单选、多选可解析；非选择题为空属正常 |
| 标准解析 | `ti_mu_jie_xi` | 三份均存在 |
| 知识点 | `ti_mu_zhi_shi_dian` | 当前是路径文本，需与已有知识点编码/名称逐项匹配 |
| 图片数、图片对象、公式对象 | `ti_mu_fu_jian` | Excel 无嵌入媒体，须从外部 images 目录建立对象标记到受控相对路径映射 |
| 年份、区域、试卷来源、题号、来源文件 | `ti_mu_lai_yuan` | 需拆成三份内容来源并补充真实权利状态 |
| 审核状态 | `ti_mu`、`ti_mu_shen_he_ji_lu` | 均为“待审核”，下一轮入库应统一进入 `PENDING`，不得自动发布 |

物理题型分布为单选 6、多选 2、实验填空 1、解答 1；化学、生物均为单选 10。难度是 `easy`/`medium`/`hard`，需映射至 1/2/3。三份来源文件路径均存在；图片目录分别为物理 54、化学 157、生物 5 个文件，但表中声明的图片对象总数为物理 67、化学 115、生物 14，不能仅按数量推断关联。所有来源均尚未提供可发布的权利依据，必须视为 `COPYRIGHT_UNKNOWN`。公式对象与图片对象占位符也需要逐条校验，否则会有附件缺失和内容哈希重复风险。

当前 PR #11 仍为 Draft、尚未合并。仅在其合并后，下一轮才建议实现 MVP30 Excel 的预检查、逐行错误、附件与知识点映射、确认入库，并使成功记录全部进入 `PENDING` 后与本页面审核流程衔接。
