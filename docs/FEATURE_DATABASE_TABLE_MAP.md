# 功能—数据库表地图（V30）

> 这是从业务功能进入数据库的导航，不替代精确 DDL：[数据库结构参考](DATABASE_SCHEMA_REFERENCE.md) 解释字段与约束，[V30 纯结构快照](../database/schema_snapshot_v30.sql) 是最终结构原文。所有表名链接到结构参考的对应小节。

## 公共门户动态统计

| 功能 | 业务作用 | 主要表 | 关系与口径 |
|---|---|---|---|
| 首页三项实时统计 | 向未登录访问者展示有效学科、可自主练习题和专题主观题的公开总量。 | [`ke_mu`](DATABASE_SCHEMA_REFERENCE.md#ke_mu)、[`ti_mu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu) | `PortalStatsService` 只做实时 COUNT：学科为 ACTIVE/未删除；练习题为 GLOBAL、PUBLISHED、未删除、`ONLINE_PRACTICE`、可自动判分且为三种客观题；专题题为 GLOBAL、PUBLISHED、未删除、`SUBJECTIVE + TOPIC_LEARNING` 且不自动判分。私有题、PENDING、DRAFT 与内部审核数据不会进入公共接口。 |

## 认证、账号与教学组织

| 功能 | 业务作用 | 主要表（点击看结构） | 关系与边界 |
|---|---|---|---|
| 认证、JWT、首次改密 | 保存可登录账号和启用状态，不在文档或日志保存明文密码。 | [`yong_hu`](DATABASE_SCHEMA_REFERENCE.md#yong_hu)、[`jiao_se`](DATABASE_SCHEMA_REFERENCE.md#jiao_se)、[`yong_hu_jiao_se`](DATABASE_SCHEMA_REFERENCE.md#yong_hu_jiao_se) | 用户与角色多对多；TEACHER/ADMIN 的权限判断来自角色表，绝不从展示职务推断。 |
| 学生档案与班级 | 学号、姓名、年级及主班级归属。 | [`xue_sheng_dang_an`](DATABASE_SCHEMA_REFERENCE.md#xue_sheng_dang_an)、[`ban_ji`](DATABASE_SCHEMA_REFERENCE.md#ban_ji)、[`ban_ji_xue_sheng`](DATABASE_SCHEMA_REFERENCE.md#ban_ji_xue_sheng) | 班级停用保留历史，不物理删除有历史关联的班级。 |
| 教师档案与任课 | 教师资料和可见、可发布的 ACTIVE 任课范围。 | [`jiao_shi_dang_an`](DATABASE_SCHEMA_REFERENCE.md#jiao_shi_dang_an)、[`ren_ke_guan_xi`](DATABASE_SCHEMA_REFERENCE.md#ren_ke_guan_xi)、[`ban_ji`](DATABASE_SCHEMA_REFERENCE.md#ban_ji)、[`ke_mu`](DATABASE_SCHEMA_REFERENCE.md#ke_mu) | 任课关系是教师私有题、组卷检索和试卷发布的服务端范围依据。 |
| 密码恢复与管理员审计 | 反枚举申请、受控处理和管理行为追溯。 | [`mi_ma_chong_zhi_shen_qing`](DATABASE_SCHEMA_REFERENCE.md#mi_ma_chong_zhi_shen_qing)、[`guan_li_cao_zuo_ri_zhi`](DATABASE_SCHEMA_REFERENCE.md#guan_li_cao_zuo_ri_zhi) | 恢复申请不泄露账号是否存在；操作日志不记录密码、Token 或 Key。 |

## 题库、审核与 Excel 导入

| 功能 | 业务作用 | 主要表 | 关系与边界 |
|---|---|---|---|
| 统一题目事实 | 保存题干、题型、答案 JSON、难度、状态、使用模式和可见范围。 | [`ti_mu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu)、[`ke_mu`](DATABASE_SCHEMA_REFERENCE.md#ke_mu)、[`zhi_shi_dian`](DATABASE_SCHEMA_REFERENCE.md#zhi_shi_dian) | `ti_mu` 是唯一题目事实源；专题不是第二题库。 |
| 选项与 STANDARD | 保存选择项和权威标准解析。 | [`ti_mu_xuan_xiang`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_xuan_xiang)、[`ti_mu_jie_xi`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_jie_xi) | 客观题由结构化答案确定性判分；STANDARD 不由 AI 覆盖。 |
| 附件、知识点、来源 | 保存受控文件元数据、题目—知识点关系和来源/权利状态。 | [`ti_mu_fu_jian`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_fu_jian)、[`ti_mu_zhi_shi_dian`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_zhi_shi_dian)、[`ti_mu_lai_yuan`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_lai_yuan) | 附件使用受控 content URL；来源与权利边界不能被 UI 文案绕过。 |
| 人工审核与导入 | 记录导入批次、审核轨迹与状态机。 | [`dao_ru_pi_ci`](DATABASE_SCHEMA_REFERENCE.md#dao_ru_pi_ci)、[`ti_mu_shen_he_ji_lu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_shen_he_ji_lu) | Excel Preview 不写库；Confirm 重新解析、校验 hash、事务写入；导入题和 STANDARD 均先 PENDING。 |

## 学生练习、错题与学习支架

| 功能 | 业务作用 | 主要表 | 关系与边界 |
|---|---|---|---|
| 自主练习与逐题作答 | 冻结练习题并保存学生答案、判分结果。 | [`lian_xi_hui_hua`](DATABASE_SCHEMA_REFERENCE.md#lian_xi_hui_hua)、[`lian_xi_ti_mu`](DATABASE_SCHEMA_REFERENCE.md#lian_xi_ti_mu)、[`xue_sheng_da_ti`](DATABASE_SCHEMA_REFERENCE.md#xue_sheng_da_ti)、[`xue_xi_jie_guo`](DATABASE_SCHEMA_REFERENCE.md#xue_xi_jie_guo) | 练习事实与当前题库编辑隔离；单选、多选、填空只走确定性规则。 |
| 错题与掌握线索 | 保留错误、再做和归档状态。 | [`cuo_ti_ji_lu`](DATABASE_SCHEMA_REFERENCE.md#cuo_ti_ji_lu) | 归档是学习状态，不删除答题历史。 |
| 高频考点与知识卡片 | 展示审核后的知识支架及学生卡片状态/练习实例。 | [`gao_pin_kao_dian`](DATABASE_SCHEMA_REFERENCE.md#gao_pin_kao_dian)、[`gao_pin_kao_dian_fu_jian`](DATABASE_SCHEMA_REFERENCE.md#gao_pin_kao_dian_fu_jian)、[`gao_pin_kao_dian_zhi_shi_dian`](DATABASE_SCHEMA_REFERENCE.md#gao_pin_kao_dian_zhi_shi_dian)、[`gao_pin_kao_dian_shen_he_ji_lu`](DATABASE_SCHEMA_REFERENCE.md#gao_pin_kao_dian_shen_he_ji_lu)、[`xue_sheng_zhi_shi_ka_pian_zhuang_tai`](DATABASE_SCHEMA_REFERENCE.md#xue_sheng_zhi_shi_ka_pian_zhuang_tai)、[`zhi_shi_ka_pian_lian_xi_shi_li`](DATABASE_SCHEMA_REFERENCE.md#zhi_shi_ka_pian_lian_xi_shi_li) | 高频结论有审核和来源边界；练习实例仍不把 AI 输出变成正式题库事实。 |

## AI、专题、私信与教师私有题

| 功能 | 业务作用 | 主要表 | 关系与边界 |
|---|---|---|---|
| AI 错因、当前题答疑、变式 | 保存学生可见的 AI 学习事实、消息、候选与质量审核。 | [`ai_cuo_ti_fen_xi`](DATABASE_SCHEMA_REFERENCE.md#ai_cuo_ti_fen_xi)、[`ai_hui_hua`](DATABASE_SCHEMA_REFERENCE.md#ai_hui_hua)、[`ai_xiao_xi`](DATABASE_SCHEMA_REFERENCE.md#ai_xiao_xi)、[`ai_xue_sheng_bian_shi_shi_li`](DATABASE_SCHEMA_REFERENCE.md#ai_xue_sheng_bian_shi_shi_li)、[`ai_sheng_cheng_ren_wu`](DATABASE_SCHEMA_REFERENCE.md#ai_sheng_cheng_ren_wu)、[`ai_hou_xuan_ti_zhi_liang_ping_jia`](DATABASE_SCHEMA_REFERENCE.md#ai_hou_xuan_ti_zhi_liang_ping_jia) | 最多 10 轮；`reasoning_content` 不展示/持久化；AI 候选须人工审核。 |
| Provider、Vision 与 Search | 保存受控模型配置、调用元数据和视觉上下文。 | [`ai_mo_xing_pei_zhi`](DATABASE_SCHEMA_REFERENCE.md#ai_mo_xing_pei_zhi)、[`ai_diao_yong_ri_zhi`](DATABASE_SCHEMA_REFERENCE.md#ai_diao_yong_ri_zhi)、[`ai_shi_jue_shang_xia_wen`](DATABASE_SCHEMA_REFERENCE.md#ai_shi_jue_shang_xia_wen) | Fake/Test 只供测试；无安全凭据时 `BLOCKED_EXTERNAL_PROVIDER` 不是 PASS。 |
| 专题学习 | 以统一题目组成三阶段教学编排。 | [`zhuan_ti_xue_xi_dan_yuan`](DATABASE_SCHEMA_REFERENCE.md#zhuan_ti_xue_xi_dan_yuan)、[`zhuan_ti_xue_xi_dan_yuan_ti_mu`](DATABASE_SCHEMA_REFERENCE.md#zhuan_ti_xue_xi_dan_yuan_ti_mu)、[`ti_mu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu)、[`ti_mu_jie_xi`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_jie_xi)、[`ti_mu_fu_jian`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_fu_jian) | 单元 1:N 关联表 N:1 `ti_mu`；专题题是 `SUBJECTIVE + TOPIC_LEARNING`，不自动评分。 |
| 教师私有题 | 在本人 ACTIVE 任课范围内创建、发布或提交审核的题目。 | [`ti_mu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu)、[`ren_ke_guan_xi`](DATABASE_SCHEMA_REFERENCE.md#ren_ke_guan_xi)、[`ti_mu_fu_jian`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_fu_jian)、[`ti_mu_shen_he_ji_lu`](DATABASE_SCHEMA_REFERENCE.md#ti_mu_shen_he_ji_lu) | GLOBAL 与 TEACHING_SCOPE_PRIVATE 由服务端复核，禁止跨班跨科泄露。 |
| 师生私信 | 会话、消息、撤回和单方隐藏。 | [`si_xin_hui_hua`](DATABASE_SCHEMA_REFERENCE.md#si_xin_hui_hua)、[`si_xin_xiao_xi`](DATABASE_SCHEMA_REFERENCE.md#si_xin_xiao_xi) | 撤回/隐藏不是物理删除历史。 |

## 试卷、发布和学生提交（核心事实链）

| 表 | 业务事实 | 与其他表的关系 |
|---|---|---|
| [`shi_juan`](DATABASE_SCHEMA_REFERENCE.md#shi_juan) | 教师编辑中的逻辑试卷；`yi_shan_chu=1` 为试卷库软删除。 | 一张试卷有当前题目编排和多次发布；仅所有 release 均已撤回或从未发布时允许软删除，本体软删除不影响历史。 |
| [`shi_juan_ti_mu`](DATABASE_SCHEMA_REFERENCE.md#shi_juan_ti_mu) | 编辑态题目、顺序和分值。 | 引用统一 `ti_mu`，手动组卷可以加入专题主观题。 |
| [`shi_juan_fa_bu`](DATABASE_SCHEMA_REFERENCE.md#shi_juan_fa_bu) | 向一个 ACTIVE 任课班级发生的一次发布事实。 | 关联教师、班级、逻辑试卷和时间边界；`CANCELLED` 使学生不可见但保留教师历史查看。 |
| [`shi_juan_fa_bu_ti_mu`](DATABASE_SCHEMA_REFERENCE.md#shi_juan_fa_bu_ti_mu) | 发布时冻结题干、选项、分值、答案、STANDARD、知识点及 V30 附件快照。 | 后续原题修改不改变学生已见的视觉和评分事实。 |
| [`shi_juan_ti_jiao`](DATABASE_SCHEMA_REFERENCE.md#shi_juan_ti_jiao) | 一名学生对一次发布的提交事实。 | 汇总客观自动得分，不把它伪装成整张卷最终分。 |
| [`shi_juan_xue_sheng_da_ti`](DATABASE_SCHEMA_REFERENCE.md#shi_juan_xue_sheng_da_ti) | 学生逐题答案、状态和客观判分结果。 | 客观题确定性判分；主观题存为 `SUBJECTIVE_PENDING`，不由 AI/规则自动评分。 |

发布快照和表结构精确字段见 [试卷表章节](DATABASE_SCHEMA_REFERENCE.md#paper) 与 [V30 DDL](../database/schema_snapshot_v30.sql)。

## 离线查询与论文入口

- 功能到页面/API/测试： [FEATURE_CODE_TECH_MAP.md](FEATURE_CODE_TECH_MAP.md)。
- 功能到截图/论文图注： [FINAL_SCREENSHOT_EVIDENCE_CATALOG.md](FINAL_SCREENSHOT_EVIDENCE_CATALOG.md)。
- 只读 SQL 查询样例： [SQL_EXAMPLES.md](SQL_EXAMPLES.md)。严禁把查询示例理解为可安全执行的修改脚本。

## 认证与演示数据卫生（PR #37）

| 业务作用 | 主要表 | 约束与保留边界 |
|---|---|---|
| 账号默认/重置密码 | `yong_hu` | 只保存 BCrypt `mi_ma_zhai_yao`；管理员分配或恢复默认密码时 `shi_fou_shou_ci_deng_lu=1`，完成首次改密后为 `0`。 |
| 203 教师范围 | `yong_hu`、`yong_hu_jiao_se`、`jiao_shi_dang_an`、`ren_ke_guan_xi`、`ban_ji`、`ke_mu` | 张生康为 `TEACHER`，仅 `CLASS_203` 的物理 ACTIVE 任课；不影响 199/200。 |
| 203 学生归属 | `yong_hu`、`xue_sheng_dang_an`、`ban_ji_xue_sheng`、`ban_ji` | 学号/用户名统一，学生主班级通过现有关系保存。 |
| V30 浏览器测试清理 | 上述根表及 `shi_juan*`、`gao_pin_kao_dian*` | 只清理有明确 `V30_BROWSER*` 根标识且关系审计无外部引用的数据；不删除任何 Flyway、结构或稳定教学历史。 |
