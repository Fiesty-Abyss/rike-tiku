# 学生自主练习、自动判分与错题 API

当前分支：`feat/student-practice-loop`（尚未合并）。接口仅允许已经完成首次改密、且具备有效学生档案的 `STUDENT` 调用。

## 练习配置与会话

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/v1/student/practice-options?subjectId=` | 返回可选科目；传入科目时返回该科有效知识点。 |
| `POST` | `/api/v1/student/practice-sessions` | 创建并冻结一场自主练习。 |
| `GET` | `/api/v1/student/practice-sessions/{id}` | 获取本人未提交或已提交会话的无答案题面。 |
| `POST` | `/api/v1/student/practice-sessions/{id}/submit` | 一次提交整场答案并自动判分。 |
| `GET` | `/api/v1/student/practice-sessions/{id}/result` | 提交后查看结果、答案和标准解析。 |

创建请求示例：

```json
{
  "subjectId": 1,
  "knowledgePointIds": [3],
  "questionTypes": ["SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK"],
  "difficulty": 2,
  "count": 5
}
```

题池严格限定为 `PUBLISHED + ONLINE_PRACTICE + 自动判分` 的单选、多选、填空题。题目不足时返回 `PRACTICE_QUESTION_INSUFFICIENT`，不会调用 AI 补题。会话响应在提交前不含正确答案或标准解析。

提交请求示例：

```json
{
  "answers": [
    {"practiceQuestionId": 101, "answer": "A", "elapsedSeconds": 23},
    {"practiceQuestionId": 102, "answer": ["A", "C"]},
    {"practiceQuestionId": 103, "answer": ["波长", "频率"]}
  ]
}
```

单选统一 trim 与大写；多选去重后按集合完全相等判分；填空按冻结的 `blanks.index` 顺序、`acceptedAnswers`、`caseSensitive` 判分，统一全半角及指定中文标点。没有部分得分，也不调用 AI 判断同义答案。重复提交返回 `409 PRACTICE_ALREADY_SUBMITTED`。

## 错题

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/v1/student/wrong-questions` | 获取当前学生的错题聚合列表。 |
| `GET` | `/api/v1/student/wrong-questions/{questionId}` | 获取本人错题的最近答案、正确答案、解析、知识点和受控附件元数据。 |

错误时创建或累加 `cuo_wu_ci_shu`，并重置连续正确次数与状态为 `NEW`；已有错题答对后保留历史错误次数，连续正确一次为 `REVIEWING`，连续正确两次为 `MASTERED`。所有资源通过当前学生档案过滤，篡改会话或题目 ID 不会越权。
