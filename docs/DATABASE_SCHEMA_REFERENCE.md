# RIKE V14 数据库结构参考

> 本文由 `information_schema` 只读生成，校验对象为 Flyway V1–V14 的 35 张业务表。字段与约束以迁移脚本为准；`database/schema_snapshot_v14.sql` 仅是便于查阅的纯结构快照，不能替代 Flyway。

## 总体约定

- MySQL 8.4，默认 `utf8mb4`。
- 业务主键均为 `BIGINT` 自增标识；关系约束和状态枚举由外键、唯一索引、Check 与服务层共同维护。
- `yi_shan_chu` 为软删除标识时，查询必须同时考虑状态字段。AI Key 只存在本地配置表，API/日志不得回显。

## Authentication

### `jiao_se`

用途：角色字典。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `jiao_se_dai_ma` | `varchar(32)` | 否 | UNI | — |
| `jiao_se_ming_cheng` | `varchar(64)` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | MUL | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_jiao_se_yi_shan_chu`；`CHECK:ck_jiao_se_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_jiao_se_dai_ma`。
生命周期：由角色字典对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `yong_hu`

用途：登录账号与密码摘要。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `yong_hu_ming` | `varchar(64)` | 否 | UNI | — |
| `mi_ma_zhai_yao` | `varchar(255)` | 否 | — | — |
| `zhang_hao_zhuang_tai` | `varchar(16)` | 否 | MUL | — |
| `shi_fou_shou_ci_deng_lu` | `tinyint` | 否 | — | — |
| `mi_ma_xiu_gai_shi_jian` | `datetime(3)` | 是 | — | — |
| `zui_hou_deng_lu_shi_jian` | `datetime(3)` | 是 | — | — |
| `ge_ren_jian_jie` | `varchar(500)` | 是 | — | 个人简介 |
| `tou_xiang_mime` | `varchar(64)` | 是 | — | 头像MIME类型 |
| `tou_xiang` | `mediumblob` | 是 | — | 头像原始二进制 |
| `tou_xiang_geng_xin_shi_jian` | `datetime(3)` | 是 | — | 头像更新时间 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_yong_hu_mi_ma_zhai_yao`；`CHECK:ck_yong_hu_shou_ci_deng_lu`；`CHECK:ck_yong_hu_yi_shan_chu`；`CHECK:ck_yong_hu_zhang_hao_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_yong_hu_yong_hu_ming`。
生命周期：由登录账号与密码摘要对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `yong_hu_jiao_se`

用途：账号角色关系。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | MUL | — |
| `jiao_se_id` | `bigint` | 否 | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_yong_hu_jiao_se_zhuang_tai`；`FOREIGN KEY:fk_yong_hu_jiao_se_jiao_se`；`FOREIGN KEY:fk_yong_hu_jiao_se_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_yong_hu_jiao_se`。
生命周期：由账号角色关系对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Organization

### `ban_ji`

用途：班级。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ban_ji_bian_ma` | `varchar(64)` | 否 | UNI | — |
| `ban_ji_ming_cheng` | `varchar(128)` | 否 | — | — |
| `nian_ji` | `varchar(32)` | 否 | MUL | — |
| `ru_xue_nian_fen` | `smallint` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_ban_ji_ru_xue_nian_fen`；`CHECK:ck_ban_ji_yi_shan_chu`；`CHECK:ck_ban_ji_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ban_ji_bian_ma`。
生命周期：由班级对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ban_ji_xue_sheng`

用途：学生班级归属。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ban_ji_id` | `bigint` | 否 | MUL | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `shi_fou_zhu_ban_ji` | `tinyint` | 否 | — | — |
| `jia_ru_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `tui_chu_shi_jian` | `datetime(3)` | 是 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `you_xiao_guan_xi_biao_shi` | `tinyint` | 是 | STORED GENERATED | — |
| `you_xiao_zhu_ban_ji_xue_sheng_id` | `bigint` | 是 | UNI, STORED GENERATED | — |

约束：`CHECK:ck_ban_ji_xue_sheng_shi_jian`；`CHECK:ck_ban_ji_xue_sheng_tui_chu`；`CHECK:ck_ban_ji_xue_sheng_zhu_ban_ji`；`CHECK:ck_ban_ji_xue_sheng_zhuang_tai`；`FOREIGN KEY:fk_ban_ji_xue_sheng_ban_ji`；`FOREIGN KEY:fk_ban_ji_xue_sheng_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ban_ji_xue_sheng_active`；`UNIQUE:uk_xue_sheng_active_main_class`。
生命周期：由学生班级归属对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `jiao_shi_dang_an`

用途：教师档案。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | UNI | — |
| `gong_hao` | `varchar(64)` | 否 | UNI | — |
| `xing_ming` | `varchar(64)` | 否 | — | — |
| `xian_shi_zhi_wu` | `varchar(128)` | 是 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | MUL | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_jiao_shi_dang_an_yi_shan_chu`；`CHECK:ck_jiao_shi_dang_an_zhuang_tai`；`FOREIGN KEY:fk_jiao_shi_dang_an_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_jiao_shi_dang_an_gong_hao`；`UNIQUE:uk_jiao_shi_dang_an_yong_hu`。
生命周期：由教师档案对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ren_ke_guan_xi`

用途：教师—班级—科目授权。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `jiao_shi_id` | `bigint` | 否 | MUL | — |
| `ban_ji_id` | `bigint` | 否 | MUL | — |
| `ke_mu_id` | `bigint` | 否 | MUL | — |
| `shi_fou_zhu_ren_ke` | `tinyint` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `kai_shi_shi_jian` | `datetime(3)` | 否 | — | — |
| `jie_shu_shi_jian` | `datetime(3)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ren_ke_shi_jian`；`CHECK:ck_ren_ke_zhu_ren_ke`；`CHECK:ck_ren_ke_zhuang_tai`；`FOREIGN KEY:fk_ren_ke_guan_xi_ban_ji`；`FOREIGN KEY:fk_ren_ke_guan_xi_jiao_shi`；`FOREIGN KEY:fk_ren_ke_guan_xi_ke_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ren_ke_jiao_shi_ban_ji_ke_mu`。
生命周期：由教师—班级—科目授权对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_sheng_dang_an`

用途：学生档案。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | UNI | — |
| `xue_hao` | `varchar(64)` | 否 | UNI | — |
| `xing_ming` | `varchar(64)` | 否 | — | — |
| `nian_ji` | `varchar(32)` | 否 | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_xue_sheng_dang_an_yi_shan_chu`；`CHECK:ck_xue_sheng_dang_an_zhuang_tai`；`FOREIGN KEY:fk_xue_sheng_dang_an_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_sheng_dang_an_xue_hao`；`UNIQUE:uk_xue_sheng_dang_an_yong_hu`。
生命周期：由学生档案对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Question bank

### `dao_ru_pi_ci`

用途：题目导入批次。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | 主键 |
| `pi_ci_bian_hao` | `varchar(64)` | 否 | UNI | 批次业务编号 |
| `dao_ru_lei_xing` | `varchar(32)` | 否 | — | 导入对象类型 |
| `yuan_shi_wen_jian_ming` | `varchar(255)` | 否 | — | 原始文件名 |
| `yuan_shi_wen_jian_lu_jing` | `varchar(1000)` | 是 | — | 仓库相对路径或受控存储路径 |
| `wen_jian_ha_xi` | `char(64)` | 是 | — | 原始文件SHA-256 |
| `zong_ji_lu_shu` | `int` | 否 | — | — |
| `cheng_gong_shu` | `int` | 否 | — | — |
| `shi_bai_shu` | `int` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | UPLOADED/VALIDATED/IMPORTED/FAILED |
| `bei_zhu` | `varchar(1000)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_dao_ru_pi_ci_ji_shu`；`CHECK:ck_dao_ru_pi_ci_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_dao_ru_pi_ci_bian_hao`。
生命周期：由题目导入批次对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ke_mu`

用途：物理/化学/生物科目。创建/演进：V1。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | 主键 |
| `ke_mu_dai_ma` | `varchar(32)` | 否 | UNI | 科目英文代码 |
| `ke_mu_ming_cheng` | `varchar(32)` | 否 | UNI | 科目名称 |
| `pai_xu` | `int` | 否 | — | 显示顺序 |
| `zhuang_tai` | `varchar(16)` | 否 | — | ACTIVE/DISABLED |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ke_mu_yi_shan_chu`；`CHECK:ck_ke_mu_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ke_mu_dai_ma`；`UNIQUE:uk_ke_mu_ming_cheng`。
生命周期：由物理/化学/生物科目对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu`

用途：题目及权威答案事实。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | 主键 |
| `ke_mu_id` | `bigint` | 否 | MUL | 科目 |
| `fu_ti_mu_id` | `bigint` | 是 | MUL | 结构化子题的母题 |
| `dao_ru_pi_ci_id` | `bigint` | 是 | MUL | 导入批次 |
| `ti_mu_lei_xing` | `varchar(32)` | 否 | — | SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_BLANK |
| `shi_yong_mo_shi` | `varchar(32)` | 否 | — | ONLINE_PRACTICE/TOPIC_LEARNING |
| `ti_gan` | `longtext` | 否 | — | 题干正文，保留附件对象标记 |
| `zheng_que_da_an` | `json` | 否 | — | 按题型定义的版本化答案JSON |
| `nan_du` | `tinyint` | 否 | — | 1 easy，2 medium，3 hard |
| `nan_du_shuo_ming` | `varchar(500)` | 是 | — | 难度判定说明 |
| `shi_fou_ke_zi_dong_pan_fen` | `tinyint(1)` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | DRAFT/PENDING/PUBLISHED/DISABLED |
| `nei_rong_ha_xi` | `char(64)` | 否 | — | 规范化题干与选项SHA-256 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_da_an_json`；`CHECK:ck_ti_mu_ke_pan_fen`；`CHECK:ck_ti_mu_lei_xing`；`CHECK:ck_ti_mu_nan_du`；`CHECK:ck_ti_mu_shi_yong_mo_shi`；`CHECK:ck_ti_mu_yi_shan_chu`；`CHECK:ck_ti_mu_zhu_guan_mo_shi`；`CHECK:ck_ti_mu_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_dao_ru_pi_ci`；`FOREIGN KEY:fk_ti_mu_fu_ti_mu`；`FOREIGN KEY:fk_ti_mu_ke_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_nei_rong_ha_xi`。
生命周期：由题目及权威答案事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_fu_jian`

用途：图片/公式附件。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `ti_mu_xuan_xiang_id` | `bigint` | 是 | MUL | 关联位置为OPTION时使用 |
| `ti_mu_jie_xi_id` | `bigint` | 是 | MUL | 关联位置为STANDARD_ANALYSIS时使用 |
| `guan_lian_wei_zhi` | `varchar(32)` | 否 | — | QUESTION/OPTION/STANDARD_ANALYSIS/ANSWER |
| `fu_jian_lei_xing` | `varchar(16)` | 否 | — | IMAGE/FORMULA/OTHER |
| `yuan_shi_wen_jian_ming` | `varchar(255)` | 否 | — | — |
| `xiang_dui_lu_jing` | `varchar(1000)` | 否 | — | 文件系统相对路径，不保存BLOB |
| `nei_rong_ha_xi` | `char(64)` | 否 | — | 附件SHA-256 |
| `dui_xiang_biao_shi` | `varchar(64)` | 是 | — | 例如I126、F107，与正文对象标记对应 |
| `zheng_wen_zi_fu_wei_zhi` | `int` | 是 | — | 对象标记在关联正文中的1基字符位置 |
| `yuan_shi_ye_ma` | `varchar(32)` | 是 | — | — |
| `fu_jian_shuo_ming` | `varchar(1000)` | 是 | — | — |
| `pai_xu` | `int` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_fu_jian_guan_lian`；`CHECK:ck_ti_mu_fu_jian_lei_xing`；`CHECK:ck_ti_mu_fu_jian_pai_xu`；`CHECK:ck_ti_mu_fu_jian_wei_zhi`；`CHECK:ck_ti_mu_fu_jian_yi_shan_chu`；`CHECK:ck_ti_mu_fu_jian_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_fu_jian_jie_xi`；`FOREIGN KEY:fk_ti_mu_fu_jian_ti_mu`；`FOREIGN KEY:fk_ti_mu_fu_jian_xuan_xiang`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_fu_jian_dui_xiang`。
生命周期：由图片/公式附件对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_jie_xi`

用途：STANDARD 解析。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `jie_xi_lei_xing` | `varchar(16)` | 否 | — | STANDARD/TEACHER/AI |
| `jie_xi_nei_rong` | `longtext` | 否 | — | 解析正文，保留附件对象标记 |
| `ban_ben_hao` | `int` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_jie_xi_ban_ben`；`CHECK:ck_ti_mu_jie_xi_lei_xing`；`CHECK:ck_ti_mu_jie_xi_yi_shan_chu`；`CHECK:ck_ti_mu_jie_xi_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_jie_xi_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_jie_xi_ban_ben`。
生命周期：由STANDARD 解析对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_lai_yuan`

用途：题目来源与权利。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `nei_rong_lei_xing` | `varchar(32)` | 否 | — | QUESTION/ANSWER/STANDARD_ANALYSIS |
| `lai_yuan_lei_xing` | `varchar(32)` | 否 | — | REAL_EXAM/AI_GENERATED/TEACHER_CREATED |
| `lai_yuan_ming_cheng` | `varchar(500)` | 否 | — | — |
| `lai_yuan_di_zhi` | `varchar(1000)` | 是 | — | URL或受控文件相对路径 |
| `nian_fen` | `smallint` | 是 | MUL | — |
| `di_qu` | `varchar(100)` | 是 | — | — |
| `shi_juan_ming_cheng` | `varchar(500)` | 是 | — | — |
| `ti_hao` | `varchar(64)` | 是 | — | — |
| `quan_li_zhuang_tai` | `varchar(32)` | 否 | — | — |
| `quan_li_yi_ju` | `varchar(1000)` | 是 | — | — |
| `huo_qu_shi_jian` | `datetime(3)` | 是 | — | 未知时保持NULL，不猜测 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_lai_yuan_lei_xing`；`CHECK:ck_ti_mu_lai_yuan_nei_rong`；`CHECK:ck_ti_mu_lai_yuan_quan_li`；`CHECK:ck_ti_mu_lai_yuan_yi_shan_chu`；`FOREIGN KEY:fk_ti_mu_lai_yuan_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_lai_yuan_nei_rong`。
生命周期：由题目来源与权利对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_shen_he_ji_lu`

用途：审核状态轨迹。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `shen_he_dong_zuo` | `varchar(32)` | 否 | — | SUBMITTED/APPROVED/REJECTED/DISABLED |
| `yuan_zhuang_tai` | `varchar(16)` | 是 | — | — |
| `mu_biao_zhuang_tai` | `varchar(16)` | 否 | — | — |
| `shen_he_ren_id` | `bigint` | 是 | MUL | 用户模块建立后再加外键 |
| `shen_he_yi_jian` | `varchar(2000)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |

约束：`CHECK:ck_ti_mu_shen_he_dong_zuo`；`CHECK:ck_ti_mu_shen_he_mu_biao_zhuang_tai`；`CHECK:ck_ti_mu_shen_he_yuan_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_shen_he_ji_lu_shen_he_ren`；`FOREIGN KEY:fk_ti_mu_shen_he_ji_lu_ti_mu`；`PRIMARY KEY:PRIMARY`。
生命周期：由审核状态轨迹对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_xuan_xiang`

用途：选择题选项。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `xuan_xiang_biao_shi` | `varchar(16)` | 否 | — | A/B/C/D或未来更多标识 |
| `xuan_xiang_nei_rong` | `longtext` | 否 | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 否 | — | — |
| `pai_xu` | `int` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_xuan_xiang_pai_xu`；`CHECK:ck_ti_mu_xuan_xiang_yi_shan_chu`；`CHECK:ck_ti_mu_xuan_xiang_zheng_que`；`FOREIGN KEY:fk_ti_mu_xuan_xiang_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_xuan_xiang_biao_shi`。
生命周期：由选择题选项对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_zhi_shi_dian`

用途：题目知识点。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `zhi_shi_dian_id` | `bigint` | 否 | MUL | — |
| `shi_fou_zhu_yao` | `tinyint(1)` | 否 | — | — |
| `pai_xu` | `int` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_ti_mu_zhi_shi_dian_pai_xu`；`CHECK:ck_ti_mu_zhi_shi_dian_yi_shan_chu`；`CHECK:ck_ti_mu_zhi_shi_dian_zhu_yao`；`FOREIGN KEY:fk_ti_mu_zhi_shi_dian_ti_mu`；`FOREIGN KEY:fk_ti_mu_zhi_shi_dian_zhi_shi_dian`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_zhi_shi_dian`。
生命周期：由题目知识点对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `zhi_shi_dian`

用途：层级知识点。创建/演进：V1。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | 主键 |
| `ke_mu_id` | `bigint` | 否 | MUL | 所属科目 |
| `fu_zhi_shi_dian_id` | `bigint` | 是 | MUL | 父知识点 |
| `zhi_shi_dian_ming_cheng` | `varchar(128)` | 否 | — | 知识点名称 |
| `wan_zheng_lu_jing` | `varchar(500)` | 否 | — | 从一级到当前节点的完整路径 |
| `ceng_ji` | `smallint` | 否 | — | 层级，从1开始 |
| `pai_xu` | `int` | 否 | — | 同级显示顺序 |
| `zhuang_tai` | `varchar(16)` | 否 | — | ACTIVE/DISABLED |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_zhi_shi_dian_ceng_ji`；`CHECK:ck_zhi_shi_dian_yi_shan_chu`；`CHECK:ck_zhi_shi_dian_zhuang_tai`；`FOREIGN KEY:fk_zhi_shi_dian_fu`；`FOREIGN KEY:fk_zhi_shi_dian_ke_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_zhi_shi_dian_lu_jing`。
生命周期：由层级知识点对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Practice

### `lian_xi_hui_hua`

用途：练习会话。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `ke_mu_id` | `bigint` | 否 | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `ti_mu_shu` | `int` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 是 | — | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_lian_xi_hui_hua_ti_mu_shu`；`CHECK:ck_lian_xi_hui_hua_zhuang_tai`；`FOREIGN KEY:fk_lian_xi_hui_hua_ke_mu`；`FOREIGN KEY:fk_lian_xi_hui_hua_xue_sheng`；`PRIMARY KEY:PRIMARY`。
生命周期：由练习会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `lian_xi_ti_mu`

用途：冻结练习题。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `lian_xi_hui_hua_id` | `bigint` | 否 | MUL | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `ti_mu_shun_xu` | `int` | 否 | — | — |
| `fen_zhi` | `decimal(8,2)` | 否 | — | — |
| `ti_mu_lei_xing` | `varchar(32)` | 否 | — | — |
| `nan_du_kuai_zhao` | `tinyint` | 否 | — | — |
| `ti_gan_kuai_zhao` | `longtext` | 否 | — | — |
| `xuan_xiang_kuai_zhao` | `json` | 是 | — | — |
| `zheng_que_da_an_kuai_zhao` | `json` | 否 | — | — |
| `biao_zhun_jie_xi_kuai_zhao` | `longtext` | 否 | — | — |
| `zhi_shi_dian_kuai_zhao` | `json` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |

约束：`CHECK:ck_lian_xi_ti_mu_da_an_json`；`CHECK:ck_lian_xi_ti_mu_fen_zhi`；`CHECK:ck_lian_xi_ti_mu_lei_xing`；`CHECK:ck_lian_xi_ti_mu_nan_du`；`CHECK:ck_lian_xi_ti_mu_shun_xu`；`CHECK:ck_lian_xi_ti_mu_xuan_xiang_json`；`CHECK:ck_lian_xi_ti_mu_zhi_shi_dian_json`；`FOREIGN KEY:fk_lian_xi_ti_mu_hui_hua`；`FOREIGN KEY:fk_lian_xi_ti_mu_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_lian_xi_ti_mu_hui_hua_shun_xu`；`UNIQUE:uk_lian_xi_ti_mu_hui_hua_ti_mu`。
生命周期：由冻结练习题对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_sheng_da_ti`

用途：正式答题事实。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `lian_xi_ti_mu_id` | `bigint` | 否 | UNI | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `xue_sheng_da_an` | `json` | 否 | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 否 | — | — |
| `de_fen` | `decimal(8,2)` | 否 | — | — |
| `yong_shi_miao_shu` | `int` | 是 | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |

约束：`CHECK:ck_xue_sheng_da_ti_de_fen`；`CHECK:ck_xue_sheng_da_ti_yong_shi`；`CHECK:ck_xue_sheng_da_ti_zheng_que`；`FOREIGN KEY:fk_xue_sheng_da_ti_lian_xi_ti_mu`；`FOREIGN KEY:fk_xue_sheng_da_ti_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_sheng_da_ti_lian_xi_ti_mu`。
生命周期：由正式答题事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_xi_jie_guo`

用途：练习最终结果。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `lian_xi_hui_hua_id` | `bigint` | 否 | UNI | — |
| `zong_ti_shu` | `int` | 否 | — | — |
| `zheng_que_shu` | `int` | 否 | — | — |
| `zong_de_fen` | `decimal(10,2)` | 否 | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |

约束：`CHECK:ck_xue_xi_jie_guo_de_fen`；`CHECK:ck_xue_xi_jie_guo_ji_shu`；`FOREIGN KEY:fk_xue_xi_jie_guo_hui_hua`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_xi_jie_guo_hui_hua`。
生命周期：由练习提交服务创建；掌握度服务以该结果和正式答题事实实时派生知识点统计，归档/删除遵循外键和业务规则。

## Wrong / mastery

### `cuo_ti_ji_lu`

用途：错题生命周期。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `cuo_wu_ci_shu` | `int` | 否 | — | — |
| `lian_xu_zheng_que_ci_shu` | `int` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `zui_jin_da_ti_id` | `bigint` | 否 | MUL | — |
| `zui_jin_cuo_wu_shi_jian` | `datetime(3)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_cuo_ti_ji_lu_ci_shu`；`CHECK:ck_cuo_ti_ji_lu_zhuang_tai`；`FOREIGN KEY:fk_cuo_ti_ji_lu_ti_mu`；`FOREIGN KEY:fk_cuo_ti_ji_lu_xue_sheng`；`FOREIGN KEY:fk_cuo_ti_ji_lu_zui_jin_da_ti`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_cuo_ti_ji_lu_xue_sheng_ti_mu`。
生命周期：由错题生命周期对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `gao_pin_kao_dian`

用途：班级科目高频考点。创建/演进：V8。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ren_ke_guan_xi_id` | `bigint` | 否 | MUL | 所属三元任课关系 |
| `zhi_shi_dian_id` | `bigint` | 否 | MUL | 所属科目知识点 |
| `biao_ti` | `varchar(200)` | 否 | — | — |
| `nei_rong` | `text` | 否 | — | — |
| `ji_yi_kou_jue` | `varchar(500)` | 是 | — | — |
| `chang_jian_wu_qu` | `text` | 是 | — | — |
| `pai_xu` | `int` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | — | — |

约束：`CHECK:ck_gao_pin_kao_dian_content`；`CHECK:ck_gao_pin_kao_dian_deleted`；`CHECK:ck_gao_pin_kao_dian_order`；`CHECK:ck_gao_pin_kao_dian_status`；`CHECK:ck_gao_pin_kao_dian_title`；`FOREIGN KEY:fk_gao_pin_kao_dian_knowledge`；`FOREIGN KEY:fk_gao_pin_kao_dian_scope`；`PRIMARY KEY:PRIMARY`。
生命周期：由班级科目高频考点对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Communication

### `si_xin_hui_hua`

用途：师生私信会话。创建/演进：V9。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ren_ke_guan_xi_id` | `bigint` | 否 | MUL | 会话对应的三元任课关系 |
| `xue_sheng_id` | `bigint` | 否 | MUL | 会话学生档案 |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `zui_hou_xiao_xi_shi_jian` | `datetime(3)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_si_xin_hui_hua_deleted`；`CHECK:ck_si_xin_hui_hua_status`；`FOREIGN KEY:fk_si_xin_hui_hua_scope`；`FOREIGN KEY:fk_si_xin_hui_hua_student`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_si_xin_hui_hua_scope_student`。
生命周期：由师生私信会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `si_xin_xiao_xi`

用途：师生私信消息。创建/演进：V9。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `hui_hua_id` | `bigint` | 否 | MUL | — |
| `fa_song_ren_yong_hu_id` | `bigint` | 否 | MUL | — |
| `nei_rong` | `varchar(1000)` | 否 | — | — |
| `yi_du` | `tinyint` | 否 | — | — |
| `fa_song_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `yi_du_shi_jian` | `datetime(3)` | 是 | — | — |
| `yi_shan_chu` | `tinyint` | 否 | — | — |

约束：`CHECK:ck_si_xin_xiao_xi_content`；`CHECK:ck_si_xin_xiao_xi_deleted`；`CHECK:ck_si_xin_xiao_xi_read`；`CHECK:ck_si_xin_xiao_xi_read_time`；`FOREIGN KEY:fk_si_xin_xiao_xi_conversation`；`FOREIGN KEY:fk_si_xin_xiao_xi_sender`；`PRIMARY KEY:PRIMARY`。
生命周期：由师生私信消息对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Audit

### `guan_li_cao_zuo_ri_zhi`

用途：管理员操作审计。创建/演进：V11。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `cao_zuo_ren_yong_hu_id` | `bigint` | 是 | MUL | — |
| `mo_kuai` | `varchar(64)` | 否 | MUL | — |
| `cao_zuo_lei_xing` | `varchar(96)` | 否 | — | — |
| `ye_wu_dui_xiang_id` | `bigint` | 是 | — | — |
| `cao_zuo_jie_guo` | `varchar(16)` | 否 | — | — |
| `zhai_yao` | `varchar(1000)` | 是 | — | — |
| `cuo_wu_dai_ma` | `varchar(96)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | MUL, DEFAULT_GENERATED | — |

约束：`CHECK:ck_guan_li_cao_zuo_ri_zhi_result`；`FOREIGN KEY:fk_guan_li_cao_zuo_ri_zhi_operator`；`PRIMARY KEY:PRIMARY`。
生命周期：由管理员操作审计对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## AI Provider

### `ai_diao_yong_ri_zhi`

用途：AI 调用安全元数据。创建/演进：V12。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `provider_dai_ma` | `varchar(64)` | 否 | MUL | — |
| `model_dai_ma` | `varchar(128)` | 否 | — | — |
| `yong_tu` | `varchar(96)` | 否 | — | — |
| `ye_wu_guan_lian` | `varchar(128)` | 是 | — | — |
| `shi_fou_cheng_gong` | `tinyint(1)` | 否 | — | — |
| `hao_shi_hao_miao` | `bigint` | 否 | — | — |
| `shu_ru_token` | `int` | 是 | — | — |
| `shu_chu_token` | `int` | 是 | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | MUL, DEFAULT_GENERATED | — |

约束：`CHECK:ck_ai_diao_yong_latency`；`CHECK:ck_ai_diao_yong_success`；`CHECK:ck_ai_diao_yong_tokens`；`PRIMARY KEY:PRIMARY`。
生命周期：由AI 调用安全元数据对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_mo_xing_pei_zhi`

用途：本地 AI Provider/模型配置。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `provider_dai_ma` | `varchar(32)` | 否 | MUL | DEEPSEEK/GLM |
| `mo_xing_dai_ma` | `varchar(128)` | 否 | — | — |
| `api_di_zhi` | `varchar(500)` | 否 | — | — |
| `api_mi_yao` | `varchar(1000)` | 是 | — | 仅本地毕设演示模式保存，API与日志禁止回显 |
| `yong_tu` | `varchar(16)` | 否 | MUL | TEXT/VISION |
| `shi_fou_qi_yong` | `tinyint(1)` | 否 | — | — |
| `shi_fou_mo_ren` | `tinyint(1)` | 否 | — | — |
| `chao_shi_hao_miao` | `int` | 否 | — | — |
| `zui_da_token` | `int` | 否 | — | — |
| `retry_count` | `tinyint` | 否 | — | — |
| `zui_jin_ce_shi_zhuang_tai` | `varchar(16)` | 否 | — | — |
| `zui_jin_ce_shi_hao_shi` | `bigint` | 是 | — | — |
| `zui_jin_ce_shi_shi_jian` | `datetime(3)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_mo_xing_flags`；`CHECK:ck_ai_mo_xing_provider`；`CHECK:ck_ai_mo_xing_retry`；`CHECK:ck_ai_mo_xing_test_status`；`CHECK:ck_ai_mo_xing_timeout`；`CHECK:ck_ai_mo_xing_tokens`；`CHECK:ck_ai_mo_xing_usage`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_mo_xing_provider_model_usage`。
生命周期：由本地 AI Provider/模型配置对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Student AI

### `ai_cuo_ti_fen_xi`

用途：错题结构化 AI 分析。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `xue_sheng_da_ti_id` | `bigint` | 否 | UNI | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `cuo_wu_lei_xing` | `varchar(32)` | 是 | — | — |
| `cuo_wu_yuan_yin` | `varchar(1200)` | 是 | — | — |
| `zheng_que_si_lu` | `varchar(1600)` | 是 | — | — |
| `chang_jian_cuo_wu` | `json` | 是 | — | — |
| `fu_xi_jian_yi` | `json` | 是 | — | — |
| `provider_dai_ma` | `varchar(64)` | 是 | — | — |
| `model_dai_ma` | `varchar(128)` | 是 | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | — | — |
| `shu_ru_shi_shi_ha_xi` | `char(64)` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_cuo_ti_fen_xi_arrays`；`CHECK:ck_ai_cuo_ti_fen_xi_status`；`CHECK:ck_ai_cuo_ti_fen_xi_success`；`CHECK:ck_ai_cuo_ti_fen_xi_type`；`FOREIGN KEY:fk_ai_cuo_ti_fen_xi_da_ti`；`FOREIGN KEY:fk_ai_cuo_ti_fen_xi_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_cuo_ti_fen_xi_da_ti`。
生命周期：由错题结构化 AI 分析对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_hui_hua`

用途：当前题 AI 会话。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | MUL | — |
| `xue_sheng_da_ti_id` | `bigint` | 否 | MUL | — |
| `lian_xi_ti_mu_id` | `bigint` | 否 | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `lei_ji_lun_shu` | `int` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_hui_hua_rounds`；`CHECK:ck_ai_hui_hua_status`；`FOREIGN KEY:fk_ai_hui_hua_da_ti`；`FOREIGN KEY:fk_ai_hui_hua_lian_xi_ti_mu`；`FOREIGN KEY:fk_ai_hui_hua_xue_sheng`；`PRIMARY KEY:PRIMARY`。
生命周期：由当前题 AI 会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_xiao_xi`

用途：当前题 AI 消息。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ai_hui_hua_id` | `bigint` | 否 | MUL | — |
| `fa_yan_jiao_se` | `varchar(16)` | 否 | — | — |
| `nei_rong` | `varchar(2000)` | 否 | — | — |
| `xu_hao` | `int` | 否 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |

约束：`CHECK:ck_ai_xiao_xi_content`；`CHECK:ck_ai_xiao_xi_order`；`CHECK:ck_ai_xiao_xi_role`；`FOREIGN KEY:fk_ai_xiao_xi_hui_hua`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_xiao_xi_hui_hua_xu_hao`。
生命周期：由当前题 AI 消息对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## AI generation / vision

### `ai_hou_xuan_ti_zhi_liang_ping_jia`

用途：候选题人工质量评价。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ai_sheng_cheng_ren_wu_id` | `bigint` | 否 | MUL | — |
| `ti_mu_id` | `bigint` | 否 | UNI | — |
| `bian_shi_zhai_yao` | `varchar(1000)` | 否 | — | — |
| `chong_fu_ti_shi` | `varchar(32)` | 否 | — | — |
| `shi_fou_shi_yong_shi_jue` | `tinyint(1)` | 否 | — | — |
| `xue_ke_zheng_que_xing` | `tinyint` | 是 | — | — |
| `da_an_zheng_que_xing` | `tinyint` | 是 | — | — |
| `ke_jie_xing` | `tinyint` | 是 | — | — |
| `zhi_shi_yi_zhi_xing` | `tinyint` | 是 | — | — |
| `nan_du_pi_pei` | `tinyint` | 是 | — | — |
| `shen_he_jie_guo` | `varchar(16)` | 否 | — | — |
| `shen_he_hao_shi_fen_zhong` | `int` | 是 | — | — |
| `shen_he_ren_id` | `bigint` | 是 | MUL | — |
| `shen_he_ping_lun` | `varchar(2000)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_quality_binary`；`CHECK:ck_ai_quality_duplicate`；`CHECK:ck_ai_quality_result`；`CHECK:ck_ai_quality_review_minutes`；`CHECK:ck_ai_quality_vision`；`FOREIGN KEY:fk_ai_quality_question`；`FOREIGN KEY:fk_ai_quality_reviewer`；`FOREIGN KEY:fk_ai_quality_task`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_quality_question`。
生命周期：由候选题人工质量评价对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_sheng_cheng_ren_wu`

用途：候选变式题生成任务。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `mu_ti_mu_id` | `bigint` | 否 | MUL | — |
| `chuang_jian_ren_id` | `bigint` | 否 | MUL | — |
| `chuang_jian_ren_jiao_se` | `varchar(16)` | 否 | — | — |
| `mu_biao_ti_xing` | `varchar(32)` | 否 | — | — |
| `zhi_shi_dian_ids` | `json` | 否 | — | — |
| `mu_biao_nan_du` | `tinyint` | 否 | — | — |
| `bian_shi_fang_shi` | `varchar(32)` | 否 | — | — |
| `sheng_cheng_shu_liang` | `tinyint` | 否 | — | — |
| `qing_qiu_ha_xi` | `char(64)` | 否 | UNI | — |
| `provider_dai_ma` | `varchar(32)` | 是 | — | — |
| `model_dai_ma` | `varchar(128)` | 是 | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `yi_sheng_cheng_shu_liang` | `tinyint` | 否 | — | — |
| `shi_fou_shi_yong_shi_jue` | `tinyint(1)` | 否 | — | — |
| `shi_bai_dai_ma` | `varchar(64)` | 是 | — | — |
| `hao_shi_hao_miao` | `bigint` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `wan_cheng_shi_jian` | `datetime(3)` | 是 | — | — |

约束：`CHECK:ck_ai_sheng_cheng_count`；`CHECK:ck_ai_sheng_cheng_difficulty`；`CHECK:ck_ai_sheng_cheng_mode`；`CHECK:ck_ai_sheng_cheng_points`；`CHECK:ck_ai_sheng_cheng_role`；`CHECK:ck_ai_sheng_cheng_status`；`CHECK:ck_ai_sheng_cheng_type`；`CHECK:ck_ai_sheng_cheng_vision`；`FOREIGN KEY:fk_ai_sheng_cheng_creator`；`FOREIGN KEY:fk_ai_sheng_cheng_mother`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_sheng_cheng_request_hash`。
生命周期：由候选变式题生成任务对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_shi_jue_shang_xia_wen`

用途：受控视觉上下文缓存。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |
|---|---|---:|---|---|
| `id` | `bigint` | 否 | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | MUL | — |
| `fu_jian_ji_he_ha_xi` | `char(64)` | 否 | — | — |
| `provider_dai_ma` | `varchar(32)` | 否 | — | — |
| `model_dai_ma` | `varchar(128)` | 否 | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | — | — |
| `shi_jue_json` | `json` | 是 | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_vision_json`；`CHECK:ck_ai_vision_status`；`FOREIGN KEY:fk_ai_vision_question`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_vision_context`。
生命周期：由受控视觉上下文缓存对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。
