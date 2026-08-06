# 管理员 MVP30 题库导入接口

## 接口

- `POST /api/v1/admin/question-import/preview`：multipart 字段 `file`，仅 ADMIN 可用。只读解析 `.xlsx`，返回文件哈希、学科、统计、每行受控映射结果、错误码与中文原因，不写业务表。
- `POST /api/v1/admin/question-import/confirm`：multipart 字段 `file`、`previewFileHash`。服务端重新解析并复验；任一行无效、文件哈希变化或已导入均拒绝。

## 校验与入库

仅接受非空 `.xlsx`（10MB、100 行上限）、`题目检查` Sheet、第二行固定表头和非公式数据单元格。题型集中映射：单选/多选/实验填空对应在线练习，解答对应 `SUBJECTIVE + TOPIC_LEARNING` 且不自动判分。知识点仅以同科目的 `zhi_shi_dian.wan_zheng_lu_jing` 精确匹配。

附件从题干、选项、最终保存的答案 JSON 和标准解析中的对象标识解析。声明有图片但无图片对象标记、声明数与 IMAGE 对象数不一致、对象缺失、跨全部允许根目录多候选、同一位置重复对象或路径越界都会令该行 `INVALID`。仅保存受控相对路径、哈希、位置和对象标识；绝不返回本机绝对路径，也不根据目录数量、顺序或模糊名称猜测。

一份文件必须只有一个有效学科；空数据文件直接拒绝，混合学科文件逐行标记 `SUBJECT_MIXED_FILE`，预览学科为 `null`。数据库完全重复仅按既有唯一约束 `ke_mu_id + nei_rong_ha_xi` 判断。

来源文件必须拆分为“试题文件”和“答案解析文件”，且均为 `sourceRoot` 内真实普通文件。QUESTION 使用试题来源，ANSWER 与 STANDARD_ANALYSIS 使用答案解析来源。上传 Excel 未持久化时，批次只保存原文件名和 SHA-256，`yuan_shi_wen_jian_lu_jing` 保持 `NULL`，不伪造路径。

确认在一个事务内写入既有 `dao_ru_pi_ci`、`ti_mu`、选项、STANDARD 解析、知识点、附件、三类来源和审核记录。所有题目与 STANDARD 解析均为 `PENDING`，审核动作为 `SUBMITTED`，来源权利状态为 `COPYRIGHT_UNKNOWN`。主观题答案保留为包含 `referenceAnswer` 的版本化 JSON；填空题每个空位保存稳定 `index` 和非空 `acceptedAnswers`。

预检查不会自动创建知识点。MVP30 工作簿的纯 V1–V6 基线与“测试事务预置工作簿所需知识点后”的附件专项统计必须分开记录；后者仅用于隔离附件完整性问题，不能作为当前数据库可导入数量。浏览器临时库联调仍为 `NOT_RUN`，不得写为通过。
