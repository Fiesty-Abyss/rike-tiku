# 学生 AI 学习 API

学生 AI 只服务当前登录学生本人，统一前缀为 `/api/v1/student/ai`。studentId 从 JWT 推导，客户端不能指定。AI 失败不改变答题、判分、错题、掌握度、规则推荐或 STANDARD。

## 错因分析

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/analyses/{answerFactId}` | 读取本人已有分析；成功结果可复用 |
| `POST` | `/analyses/{answerFactId}` | 为本人错误且已提交的正式答题事实生成分析 |

`answerFactId` 对应 `xue_sheng_da_ti.id`。服务端重新检查学生所有权、练习已提交、题目与冻结事实一致且本次作答错误。错题进入 REVIEWING 或 MASTERED 后，错题详情仍提供最近一次错误正式答题事实，而不是直接相信最近一次可能正确的答题记录。

分析输出包含 `errorType`、`errorReason`、`correctThinking`、`commonMistakes` 和 `reviewSuggestions`。`errorType` 限定为八类受控枚举；JSON、必填字段、长度和数组均严格校验。首次无效最多发起一次业务纠正，第二次仍无效则返回受控失败，不保存成功分析。

## 当前题会话

| 方法 | 路径 | 请求 | 用途 |
| --- | --- | --- | --- |
| `POST` | `/conversations` | `{ "answerFactId": 123 }` | 为本人正式答题事实创建或取得当前题会话 |
| `GET` | `/conversations/{conversationId}` | 无 | 读取本人会话与消息 |
| `POST` | `/conversations/{conversationId}/messages` | `{ "content": "..." }` | 在本人当前题会话发送追问 |

每个会话同时绑定学生、正式答题事实和冻结练习题。服务端对 conversationId 重新反查所有权，不能只检查记录存在。其他学生、TEACHER 和 ADMIN 不能读取或追加学生 AI 私聊。

## 上下文限制

- 最多 8 轮，达到上限后提示新开会话。
- 单条用户消息最多 500 字。
- 单条助手消息最多 2000 字。
- Provider 上下文最多最近 12 条有效消息，总字符预算 6000。
- 只讨论当前物理、化学、生物题目；无关闲聊会被限制回学习场景。
- 身份与模型询问使用确定性 RIKE 身份回复，不调用 Provider。

## 失败与隐私

Provider disabled、缺 Key、401、429、timeout、5xx、无效 JSON 和纠正失败都转换为受控错误。接口不返回底层堆栈、Provider 原始响应、provider/model、URL、Key 或 Token。STANDARD 始终由原练习结果与错题接口正常展示。
