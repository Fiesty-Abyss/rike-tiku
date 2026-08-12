# Excel 批量导入指南

本文依据当前 `StudentImportService`、`StudentImportConfirmService`、`QuestionImportService` 及其集成测试编写。模板位于 [docs/templates](templates/)；两个模板均已在随机临时 MySQL 上执行真实 preview/confirm，正式库未参与。

## 学生导入

- 接口：`GET /api/v1/admin/student-import/template`、`POST .../preview`、`POST .../confirm`，仅 ADMIN。
- 文件：只支持 `.xlsx`，最大 5 MB，最多 500 个非空数据行；禁止公式单元格和宏。
- Sheet：必须名为 `学生导入`；第 1 行是固定表头，列顺序不能改变；空行跳过。

| 顺序 | 列名 | 必填 | 规则 |
|---:|---|---:|---|
| 1 | `xue_hao` | 是 | 1–64 字符，全局唯一；按文本保存以保留前导零 |
| 2 | `xing_ming` | 是 | 2–32 字符，不能纯空白 |
| 3 | `ban_ji_bian_ma` | 是 | 班级必须存在且为 ACTIVE |
| 4 | `nian_ji` | 是 | 必须与班级年级完全一致 |
| 5 | `yong_hu_ming` | 否 | 为空使用学号；1–64 字符，只能含字母、数字、点、下划线或连字符 |
| 6 | `chu_shi_mi_ma` | 否 | 8–64 位且同时含字母和数字；为空则 Confirm 时随机生成 |
| 7 | `zhang_hao_zhuang_tai` | 否 | `ENABLED` / `DISABLED`，为空默认 `ENABLED` |

Preview 只解析和返回逐行错误，不写业务表。文件内重复学号/用户名、数据库已有值、班级或密码规则错误会使行无效。Confirm 会重新完整校验，任一行无效则拒绝整批；用户、STUDENT 角色、学生档案和唯一主班级在事务中写入。初始密码只在确认响应中一次性返回，数据库保存 BCrypt 摘要，账号进入首次改密流程。

模板：[student-import-template.xlsx](templates/student-import-template.xlsx)。示例班级 `CLASS_TEMPLATE` 是虚构占位，使用前必须替换为目标环境真实存在的 ACTIVE 班级编码。

## 题目导入

- 接口：`POST /api/v1/admin/question-import/preview` 和 `/confirm`，仅 ADMIN。
- 文件：只支持 `.xlsx`，最大 10 MB，最多 100 个非空数据行；同一文件只能是一个学科。
- Sheet：必须名为 `题目检查`；第 1 行为标题，第 2 行 19 列固定表头，第 3 行起为数据；数据区域禁止公式。
- 流程：Preview 计算 SHA-256 文件哈希并完成无写入校验；Confirm 要求传回 `previewFileHash`，服务端重新解析，哈希变化、重复导入或任一无效行均拒绝。

| 列 | 要点 |
|---|---|
| 学科 | 仅物理、化学、生物；一个文件不能混科 |
| 年份、区域、试卷来源、题号 | 来源事实，均须合法；年份为整数 |
| 题型 | 单选题、多选题、实验填空题、解答题，映射到受控题型与使用模式 |
| 题干 | 必填；不能与同文件或本学科数据库现有规范化内容 hash 完全重复 |
| 选项 | 选择题按 `A. 内容` 每项换行；选项标签和数量由 Parser 校验 |
| 答案 | 单选一个标签；多选用分隔符列出；填空用 `①. 内容`；解答题保留参考答案 |
| 标准解析 | 必填；Confirm 后仅为 PENDING STANDARD，人工审核后发布 |
| 图片数 | 必须与正文 `〔图片对象 I001〕` 等标记和受控目录文件精确对应 |
| 一级知识点、知识点 | 知识点填写当前学科已存在的完整路径；多个路径换行分隔 |
| 难度 | 仅 `easy`、`medium`、`hard`，映射为 1、3、5 |
| 审核状态 | 导入不会绕过审核，题目和 STANDARD 进入 PENDING |
| 来源文件 | 必须同时包含 `试题文件：...` 与 `答案解析文件：...`，路径必须位于受控题库根目录且文件存在 |

附件文件名按题号、位置类型和对象编号匹配，例如 `q11_题干_image_001.png`。对象不能在同一关联位置重复，声明数量、标记和实际文件必须一致；路径越界、歧义或缺失都会使 Preview 无效。Confirm 在一个事务内写导入批次、题目、选项、答案 JSON、PENDING STANDARD、知识点、来源、附件和 SUBMITTED 审核轨迹。

模板：[question-import-template.xlsx](templates/question-import-template.xlsx)。示例是项目自编物理题，知识点路径来自 V3；来源文件引用仓库 `题库` 下现有说明文件，使用其他环境时应替换为具有合法权利的本地来源。

## 验证事实

执行 `mvn "-Dtest=FinalImportTemplatesIntegrationTest" test`：1 test，0 failures，0 errors，0 skipped。测试创建随机临时数据库、执行 Flyway V1–V14，再对两个已发布模板执行 preview 和 confirm；题目确认结果为 PENDING。首次因模板尚未从仓库根目录生成而失败，修正执行目录后重跑通过，未触碰 `rike_tiku`。
