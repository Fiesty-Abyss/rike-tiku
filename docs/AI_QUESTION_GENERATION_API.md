# AI 候选题生成与审核 API

AI 候选题只能从 PUBLISHED 母题生成，结果只能先进入 PENDING。ADMIN 拥有全局题库权限；TEACHER 只允许访问本人 ACTIVE 三元任课关系授权的学科。

## ADMIN

前缀为 `/api/v1/admin/ai-generation`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/mothers` | 查询全局可用 PUBLISHED 母题 |
| `GET` | `/tasks` | 查询生成任务 |
| `GET` | `/tasks/{id}` | 查询任务和候选详情 |
| `POST` | `/tasks` | 发起生成 |
| `POST` | `/candidates/{id}/review` | 评价并审核候选 |
| `GET` | `/stats` | 查询全局生成与审核统计 |

## TEACHER

前缀为 `/api/v1/teacher/ai-generation`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/mothers` | 查询本人授权学科的 PUBLISHED 母题 |
| `GET` | `/knowledge-points?subjectId={id}` | 查询本人授权学科知识点 |
| `GET` | `/tasks` | 查询本人可访问任务 |
| `GET` | `/tasks/{id}` | 查询授权任务和候选详情 |
| `POST` | `/tasks` | 在授权学科发起生成 |
| `POST` | `/candidates/{id}/review` | 评价并审核授权候选 |

教师没有全局 stats 接口，也不能访问管理员模型配置。

## 发起生成

```json
{
  "motherQuestionId": 101,
  "questionType": "SINGLE_CHOICE",
  "knowledgePointIds": [12, 13],
  "targetDifficulty": 3,
  "variationMode": "SCENARIO",
  "count": 2
}
```

题型限单选、多选、填空，难度为 1 至 5，变化方式为 `NUMERIC_CONDITION`、`SCENARIO`、`KNOWLEDGE_ANGLE`、`DISTRACTOR` 或 `COMBINED`，单次数量为 1 至 3。同一母题当前 PENDING AI_GENERATED 加本次不得超过 6。

request hash 绑定母题、题型、排序知识点、难度、变化方式和 Prompt 版本。相同有效请求不会创建第二个任务。Provider、Parser 和 prepare 位于事务外；候选正式写入使用单个批次事务，任何候选失败都会整批回滚，任务保留为 FAILED 且生成数为 0。

## 候选与重复控制

模型输出经过固定字段、题型、选项、答案、难度、知识点、学科、长度和危险 HTML 校验。候选本体写入 `ti_mu`，来源记录为 `AI_GENERATED`，设置母题关联并进入 PENDING。

- 现有 `nei_rong_ha_xi` 拒绝精确重复。
- 同一批次的内容 hash 拒绝批内重复。
- trigram/Jaccard 大于等于 0.72 标记 `SUSPECTED_DUPLICATE`，交由人工判断，不自动发布。

## 人工评价与审核

```json
{
  "subjectCorrectness": 1,
  "answerCorrectness": 1,
  "solvability": 1,
  "knowledgeConsistency": 1,
  "difficultyMatch": 1,
  "reviewResult": "APPROVED",
  "reviewMinutes": 5,
  "reviewComment": "审核说明"
}
```

五项评价只能为 0 或 1；`reviewResult` 为 `APPROVED` 或 `REJECTED`；审核分钟数为 0 至 10080；评论最多 2000 字。APPROVED 复用现有审核状态机进入 PUBLISHED，REJECTED 保留任务和评价事实并回到 DRAFT。未审核候选不能进入学生练习。

ADMIN 的 stats 返回任务、成功/失败、请求/生成、疑似重复、通过/驳回、平均耗时和平均审核分钟等基础统计，不生成虚假论文数据。
