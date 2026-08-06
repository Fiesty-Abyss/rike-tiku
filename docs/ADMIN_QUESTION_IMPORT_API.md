# 管理员 MVP30 题库导入接口

## 接口

- `POST /api/v1/admin/question-import/preview`：multipart 字段 `file`，仅 ADMIN 可用。只读解析 `.xlsx`，返回文件哈希、学科、统计、每行受控映射结果、错误码与中文原因，不写业务表。
- `POST /api/v1/admin/question-import/confirm`：multipart 字段 `file`、`previewFileHash`。服务端重新解析并复验；任一行无效、文件哈希变化或已导入均拒绝。

## 校验与入库

仅接受非空 `.xlsx`（10MB、100 行上限）、`题目检查` Sheet、第二行固定表头和非公式数据单元格。题型集中映射：单选/多选/实验填空对应在线练习，解答对应 `SUBJECTIVE + TOPIC_LEARNING` 且不自动判分。知识点仅以同科目的 `zhi_shi_dian.wan_zheng_lu_jing` 精确匹配。

附件从题干、选项、答案和标准解析中的对象标识解析，严格验证受控目录中的唯一精确对象文件、哈希、位置和相对路径；绝不返回本机绝对路径，也不根据目录数量、顺序或模糊名称猜测。

确认在一个事务内写入既有 `dao_ru_pi_ci`、`ti_mu`、选项、STANDARD 解析、知识点、附件、三类来源和审核记录。所有题目与 STANDARD 解析均为 `PENDING`，审核动作为 `SUBMITTED`，来源权利状态为 `COPYRIGHT_UNKNOWN`。
