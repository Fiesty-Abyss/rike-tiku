# RIKE V29 数据库结构参考

> 本文由 `information_schema` 只读生成，校验对象为隔离库 `rike_tiku_demo` 的 Flyway V1–V29 业务表。字段与约束以迁移脚本为准；`database/schema_snapshot_v29.sql` 仅是便于查阅的纯结构快照，不能替代 Flyway。

## 总体约定

- MySQL 8.4，默认 `utf8mb4`。
- 业务主键均为 `BIGINT` 自增标识；关系约束和状态枚举由外键、唯一索引、Check 与服务层共同维护。
- `yi_shan_chu` 为软删除标识时，查询必须同时考虑状态字段。AI Key 只存在本地配置表，API/日志不得回显。

- 本参考完整列出 50 张业务表；每张表给出 MySQL 类型、NULL、默认值、主键/外键/UNIQUE/CHECK、精确索引与生命周期。

- 本轮没有新增 V30；学生高频考点内容与专题单元是 V21/V26 已有表中的受控内容写入，不改变 V29 schema snapshot。正式库核验结果为 6 个 `PUBLISHED` 专题单元、18 条单元题目关系、65 张 `PUBLISHED` 卡片，其中 60 张来自结构化内容源。

## Authentication

### `jiao_se`

用途：角色字典。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `jiao_se_dai_ma` | `varchar(32)` | 否 | `NULL` | UNI | — |
| `jiao_se_ming_cheng` | `varchar(64)` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | MUL | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_jiao_se_yi_shan_chu`；`CHECK:ck_jiao_se_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_jiao_se_dai_ma`。
索引：`INDEX:idx_jiao_se_zhuang_tai_shan_chu(zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_jiao_se_dai_ma(jiao_se_dai_ma)`。
生命周期：由角色字典对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `yong_hu`

用途：登录账号与密码摘要。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `yong_hu_ming` | `varchar(64)` | 否 | `NULL` | UNI | — |
| `mi_ma_zhai_yao` | `varchar(255)` | 否 | `NULL` | — | — |
| `zhang_hao_zhuang_tai` | `varchar(16)` | 否 | `ENABLED` | MUL | — |
| `shi_fou_shou_ci_deng_lu` | `tinyint` | 否 | `1` | — | — |
| `mi_ma_xiu_gai_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `zui_hou_deng_lu_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `ge_ren_jian_jie` | `varchar(500)` | 是 | `NULL` | — | 个人简介 |
| `tou_xiang_mime` | `varchar(64)` | 是 | `NULL` | — | 头像MIME类型 |
| `tou_xiang` | `mediumblob` | 是 | `NULL` | — | 头像原始二进制 |
| `tou_xiang_geng_xin_shi_jian` | `datetime(3)` | 是 | `NULL` | — | 头像更新时间 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_yong_hu_mi_ma_zhai_yao`；`CHECK:ck_yong_hu_shou_ci_deng_lu`；`CHECK:ck_yong_hu_yi_shan_chu`；`CHECK:ck_yong_hu_zhang_hao_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_yong_hu_yong_hu_ming`。
索引：`INDEX:idx_yong_hu_zhuang_tai_shan_chu(zhang_hao_zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_yong_hu_yong_hu_ming(yong_hu_ming)`。
生命周期：由登录账号与密码摘要对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `yong_hu_jiao_se`

用途：账号角色关系。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `jiao_se_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_yong_hu_jiao_se_zhuang_tai`；`FOREIGN KEY:fk_yong_hu_jiao_se_jiao_se`；`FOREIGN KEY:fk_yong_hu_jiao_se_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_yong_hu_jiao_se`。
索引：`INDEX:idx_yong_hu_jiao_se_jiao_se(jiao_se_id,zhuang_tai)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_yong_hu_jiao_se(yong_hu_id,jiao_se_id)`。
生命周期：由账号角色关系对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Organization

### `ban_ji`

用途：班级。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ban_ji_bian_ma` | `varchar(64)` | 否 | `NULL` | UNI | — |
| `ban_ji_ming_cheng` | `varchar(128)` | 否 | `NULL` | — | — |
| `nian_ji` | `varchar(32)` | 否 | `NULL` | MUL | — |
| `ru_xue_nian_fen` | `smallint` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_ban_ji_ru_xue_nian_fen`；`CHECK:ck_ban_ji_yi_shan_chu`；`CHECK:ck_ban_ji_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ban_ji_bian_ma`。
索引：`INDEX:idx_ban_ji_nian_ji_zhuang_tai(nian_ji,zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ban_ji_bian_ma(ban_ji_bian_ma)`。
生命周期：由班级对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ban_ji_xue_sheng`

用途：学生班级归属。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ban_ji_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_fou_zhu_ban_ji` | `tinyint` | 否 | `0` | — | — |
| `jia_ru_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `tui_chu_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `you_xiao_guan_xi_biao_shi` | `tinyint` | 是 | `NULL` | STORED GENERATED | — |
| `you_xiao_zhu_ban_ji_xue_sheng_id` | `bigint` | 是 | `NULL` | UNI, STORED GENERATED | — |

约束：`CHECK:ck_ban_ji_xue_sheng_shi_jian`；`CHECK:ck_ban_ji_xue_sheng_tui_chu`；`CHECK:ck_ban_ji_xue_sheng_zhu_ban_ji`；`CHECK:ck_ban_ji_xue_sheng_zhuang_tai`；`FOREIGN KEY:fk_ban_ji_xue_sheng_ban_ji`；`FOREIGN KEY:fk_ban_ji_xue_sheng_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ban_ji_xue_sheng_active`；`UNIQUE:uk_xue_sheng_active_main_class`。
索引：`INDEX:idx_ban_ji_xue_sheng_class(ban_ji_id,zhuang_tai)`；`INDEX:idx_ban_ji_xue_sheng_student(xue_sheng_id,zhuang_tai)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ban_ji_xue_sheng_active(ban_ji_id,xue_sheng_id,you_xiao_guan_xi_biao_shi)`；`UNIQUE/PRIMARY:uk_xue_sheng_active_main_class(you_xiao_zhu_ban_ji_xue_sheng_id)`。
生命周期：由学生班级归属对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `jiao_shi_dang_an`

用途：教师档案。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | `NULL` | UNI | — |
| `gong_hao` | `varchar(64)` | 否 | `NULL` | UNI | — |
| `xing_ming` | `varchar(64)` | 否 | `NULL` | — | — |
| `xian_shi_zhi_wu` | `varchar(128)` | 是 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | MUL | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_jiao_shi_dang_an_yi_shan_chu`；`CHECK:ck_jiao_shi_dang_an_zhuang_tai`；`FOREIGN KEY:fk_jiao_shi_dang_an_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_jiao_shi_dang_an_gong_hao`；`UNIQUE:uk_jiao_shi_dang_an_yong_hu`。
索引：`INDEX:idx_jiao_shi_zhuang_tai_shan_chu(zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_jiao_shi_dang_an_gong_hao(gong_hao)`；`UNIQUE/PRIMARY:uk_jiao_shi_dang_an_yong_hu(yong_hu_id)`。
生命周期：由教师档案对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ren_ke_guan_xi`

用途：教师—班级—科目授权。创建/演进：V6。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `jiao_shi_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ban_ji_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_fou_zhu_ren_ke` | `tinyint` | 否 | `0` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `kai_shi_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `jie_shu_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ren_ke_shi_jian`；`CHECK:ck_ren_ke_zhu_ren_ke`；`CHECK:ck_ren_ke_zhuang_tai`；`FOREIGN KEY:fk_ren_ke_guan_xi_ban_ji`；`FOREIGN KEY:fk_ren_ke_guan_xi_jiao_shi`；`FOREIGN KEY:fk_ren_ke_guan_xi_ke_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ren_ke_jiao_shi_ban_ji_ke_mu`。
索引：`INDEX:fk_ren_ke_guan_xi_ke_mu(ke_mu_id)`；`INDEX:idx_ren_ke_ban_ji_ke_mu_zhuang_tai(ban_ji_id,ke_mu_id,zhuang_tai)`；`INDEX:idx_ren_ke_jiao_shi_zhuang_tai(jiao_shi_id,zhuang_tai)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ren_ke_jiao_shi_ban_ji_ke_mu(jiao_shi_id,ban_ji_id,ke_mu_id)`。
生命周期：由教师—班级—科目授权对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_sheng_dang_an`

用途：学生档案。创建/演进：V5。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | `NULL` | UNI | — |
| `xue_hao` | `varchar(64)` | 否 | `NULL` | UNI | — |
| `xing_ming` | `varchar(64)` | 否 | `NULL` | — | — |
| `nian_ji` | `varchar(32)` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_xue_sheng_dang_an_yi_shan_chu`；`CHECK:ck_xue_sheng_dang_an_zhuang_tai`；`FOREIGN KEY:fk_xue_sheng_dang_an_yong_hu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_sheng_dang_an_xue_hao`；`UNIQUE:uk_xue_sheng_dang_an_yong_hu`。
索引：`INDEX:idx_xue_sheng_nian_ji_zhuang_tai(nian_ji,zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_xue_sheng_dang_an_xue_hao(xue_hao)`；`UNIQUE/PRIMARY:uk_xue_sheng_dang_an_yong_hu(yong_hu_id)`。
生命周期：由学生档案对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Question bank

### `dao_ru_pi_ci`

用途：题目导入批次。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | 主键 |
| `pi_ci_bian_hao` | `varchar(64)` | 否 | `NULL` | UNI | 批次业务编号 |
| `dao_ru_lei_xing` | `varchar(32)` | 否 | `QUESTION` | — | 导入对象类型 |
| `yuan_shi_wen_jian_ming` | `varchar(255)` | 否 | `NULL` | — | 原始文件名 |
| `yuan_shi_wen_jian_lu_jing` | `varchar(1000)` | 是 | `NULL` | — | 仓库相对路径或受控存储路径 |
| `wen_jian_ha_xi` | `char(64)` | 是 | `NULL` | — | 原始文件SHA-256 |
| `zong_ji_lu_shu` | `int` | 否 | `0` | — | — |
| `cheng_gong_shu` | `int` | 否 | `0` | — | — |
| `shi_bai_shu` | `int` | 否 | `0` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | UPLOADED/VALIDATED/IMPORTED/FAILED |
| `bei_zhu` | `varchar(1000)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_dao_ru_pi_ci_ji_shu`；`CHECK:ck_dao_ru_pi_ci_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_dao_ru_pi_ci_bian_hao`。
索引：`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_dao_ru_pi_ci_bian_hao(pi_ci_bian_hao)`。
生命周期：由题目导入批次对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ke_mu`

用途：物理/化学/生物科目。创建/演进：V1。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | 主键 |
| `ke_mu_dai_ma` | `varchar(32)` | 否 | `NULL` | UNI | 科目英文代码 |
| `ke_mu_ming_cheng` | `varchar(32)` | 否 | `NULL` | UNI | 科目名称 |
| `pai_xu` | `int` | 否 | `0` | — | 显示顺序 |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | ACTIVE/DISABLED |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ke_mu_yi_shan_chu`；`CHECK:ck_ke_mu_zhuang_tai`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ke_mu_dai_ma`；`UNIQUE:uk_ke_mu_ming_cheng`。
索引：`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ke_mu_dai_ma(ke_mu_dai_ma)`；`UNIQUE/PRIMARY:uk_ke_mu_ming_cheng(ke_mu_ming_cheng)`。
生命周期：由物理/化学/生物科目对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu`

用途：题目及权威答案事实。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | 主键 |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | 科目 |
| `fu_ti_mu_id` | `bigint` | 是 | `NULL` | MUL | 结构化子题的母题 |
| `dao_ru_pi_ci_id` | `bigint` | 是 | `NULL` | MUL | 导入批次 |
| `ti_mu_lei_xing` | `varchar(32)` | 否 | `NULL` | — | SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_BLANK |
| `shi_yong_mo_shi` | `varchar(32)` | 否 | `NULL` | — | ONLINE_PRACTICE/TOPIC_LEARNING |
| `zhuan_ti_lei_xing` | `varchar(32)` | 是 | `NULL` | — | 仅 SUBJECTIVE + TOPIC_LEARNING 使用的受控专题类型 |
| `ke_jian_fan_wei` | `varchar(32)` | 否 | `GLOBAL` | MUL | GLOBAL/TEACHING_SCOPE_PRIVATE |
| `ren_ke_guan_xi_id` | `bigint` | 是 | `NULL` | MUL | 私有题所属任课关系 |
| `chuang_jian_ren_id` | `bigint` | 是 | `NULL` | MUL | 题目创建用户；历史全局题允许为空 |
| `ti_gan` | `longtext` | 否 | `NULL` | — | 题干正文，保留附件对象标记 |
| `zheng_que_da_an` | `json` | 否 | `NULL` | — | 按题型定义的版本化答案JSON |
| `nan_du` | `tinyint` | 否 | `NULL` | — | 1 easy，2 medium，3 hard |
| `nan_du_shuo_ming` | `varchar(500)` | 是 | `NULL` | — | 难度判定说明 |
| `shi_fou_ke_zi_dong_pan_fen` | `tinyint(1)` | 否 | `1` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `DRAFT` | — | DRAFT/PENDING/PUBLISHED/DISABLED |
| `nei_rong_ha_xi` | `char(64)` | 否 | `NULL` | — | 规范化题干与选项SHA-256 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_da_an_json`；`CHECK:ck_ti_mu_ke_pan_fen`；`CHECK:ck_ti_mu_lei_xing`；`CHECK:ck_ti_mu_nan_du`；`CHECK:ck_ti_mu_shi_yong_mo_shi`；`CHECK:ck_ti_mu_topic_category`；`CHECK:ck_ti_mu_visibility`；`CHECK:ck_ti_mu_yi_shan_chu`；`CHECK:ck_ti_mu_zhu_guan_mo_shi`；`CHECK:ck_ti_mu_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_creator`；`FOREIGN KEY:fk_ti_mu_dao_ru_pi_ci`；`FOREIGN KEY:fk_ti_mu_fu_ti_mu`；`FOREIGN KEY:fk_ti_mu_ke_mu`；`FOREIGN KEY:fk_ti_mu_teaching_scope`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_nei_rong_ha_xi`。
索引：`INDEX:fk_ti_mu_teaching_scope(ren_ke_guan_xi_id)`；`INDEX:idx_ti_mu_creator_scope(chuang_jian_ren_id,ren_ke_guan_xi_id,zhuang_tai)`；`INDEX:idx_ti_mu_dao_ru_pi_ci(dao_ru_pi_ci_id)`；`INDEX:idx_ti_mu_fu_ti_mu(fu_ti_mu_id)`；`INDEX:idx_ti_mu_ke_mu_zhuang_tai_nan_du(ke_mu_id,zhuang_tai,nan_du)`；`INDEX:idx_ti_mu_visibility(ke_jian_fan_wei,ren_ke_guan_xi_id,ke_mu_id,zhuang_tai,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_nei_rong_ha_xi(ke_mu_id,nei_rong_ha_xi)`。
生命周期：由题目及权威答案事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_fu_jian`

用途：图片/公式附件。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_xuan_xiang_id` | `bigint` | 是 | `NULL` | MUL | 关联位置为OPTION时使用 |
| `ti_mu_jie_xi_id` | `bigint` | 是 | `NULL` | MUL | 关联位置为STANDARD_ANALYSIS时使用 |
| `guan_lian_wei_zhi` | `varchar(32)` | 否 | `NULL` | — | QUESTION/OPTION/STANDARD_ANALYSIS/ANSWER |
| `fu_jian_lei_xing` | `varchar(16)` | 否 | `NULL` | — | IMAGE/FORMULA/OTHER |
| `yuan_shi_wen_jian_ming` | `varchar(255)` | 否 | `NULL` | — | — |
| `xiang_dui_lu_jing` | `varchar(1000)` | 否 | `NULL` | — | 文件系统相对路径，不保存BLOB |
| `nei_rong_ha_xi` | `char(64)` | 否 | `NULL` | — | 附件SHA-256 |
| `dui_xiang_biao_shi` | `varchar(64)` | 是 | `NULL` | — | 例如I126、F107，与正文对象标记对应 |
| `zheng_wen_zi_fu_wei_zhi` | `int` | 是 | `NULL` | — | 对象标记在关联正文中的1基字符位置 |
| `yuan_shi_ye_ma` | `varchar(32)` | 是 | `NULL` | — | — |
| `fu_jian_shuo_ming` | `varchar(1000)` | 是 | `NULL` | — | — |
| `pai_xu` | `int` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_fu_jian_guan_lian`；`CHECK:ck_ti_mu_fu_jian_lei_xing`；`CHECK:ck_ti_mu_fu_jian_pai_xu`；`CHECK:ck_ti_mu_fu_jian_wei_zhi`；`CHECK:ck_ti_mu_fu_jian_yi_shan_chu`；`CHECK:ck_ti_mu_fu_jian_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_fu_jian_jie_xi`；`FOREIGN KEY:fk_ti_mu_fu_jian_ti_mu`；`FOREIGN KEY:fk_ti_mu_fu_jian_xuan_xiang`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_fu_jian_dui_xiang`。
索引：`INDEX:idx_ti_mu_fu_jian_jie_xi(ti_mu_jie_xi_id)`；`INDEX:idx_ti_mu_fu_jian_pai_xu(ti_mu_id,guan_lian_wei_zhi,pai_xu)`；`INDEX:idx_ti_mu_fu_jian_xuan_xiang(ti_mu_xuan_xiang_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_fu_jian_dui_xiang(ti_mu_id,guan_lian_wei_zhi,dui_xiang_biao_shi)`。
生命周期：由图片/公式附件对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_jie_xi`

用途：STANDARD 解析。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `jie_xi_lei_xing` | `varchar(16)` | 否 | `NULL` | — | STANDARD/TEACHER/AI |
| `jie_xi_nei_rong` | `longtext` | 否 | `NULL` | — | 解析正文，保留附件对象标记 |
| `ban_ben_hao` | `int` | 否 | `1` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `DRAFT` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_jie_xi_ban_ben`；`CHECK:ck_ti_mu_jie_xi_lei_xing`；`CHECK:ck_ti_mu_jie_xi_yi_shan_chu`；`CHECK:ck_ti_mu_jie_xi_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_jie_xi_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_jie_xi_ban_ben`。
索引：`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_jie_xi_ban_ben(ti_mu_id,jie_xi_lei_xing,ban_ben_hao)`。
生命周期：由STANDARD 解析对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_lai_yuan`

用途：题目来源与权利。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `nei_rong_lei_xing` | `varchar(32)` | 否 | `NULL` | — | QUESTION/ANSWER/STANDARD_ANALYSIS |
| `lai_yuan_lei_xing` | `varchar(32)` | 否 | `NULL` | — | REAL_EXAM/AI_GENERATED/TEACHER_CREATED |
| `lai_yuan_ming_cheng` | `varchar(500)` | 否 | `NULL` | — | — |
| `lai_yuan_di_zhi` | `varchar(1000)` | 是 | `NULL` | — | URL或受控文件相对路径 |
| `nian_fen` | `smallint` | 是 | `NULL` | MUL | — |
| `di_qu` | `varchar(100)` | 是 | `NULL` | — | — |
| `shi_juan_ming_cheng` | `varchar(500)` | 是 | `NULL` | — | — |
| `ti_hao` | `varchar(64)` | 是 | `NULL` | — | — |
| `quan_li_zhuang_tai` | `varchar(32)` | 否 | `NULL` | — | — |
| `quan_li_yi_ju` | `varchar(1000)` | 是 | `NULL` | — | — |
| `huo_qu_shi_jian` | `datetime(3)` | 是 | `NULL` | — | 未知时保持NULL，不猜测 |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_lai_yuan_lei_xing`；`CHECK:ck_ti_mu_lai_yuan_nei_rong`；`CHECK:ck_ti_mu_lai_yuan_quan_li`；`CHECK:ck_ti_mu_lai_yuan_yi_shan_chu`；`FOREIGN KEY:fk_ti_mu_lai_yuan_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_lai_yuan_nei_rong`。
索引：`INDEX:idx_ti_mu_lai_yuan_shi_juan(nian_fen,di_qu,shi_juan_ming_cheng)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_lai_yuan_nei_rong(ti_mu_id,nei_rong_lei_xing)`。
生命周期：由题目来源与权利对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_shen_he_ji_lu`

用途：审核状态轨迹。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shen_he_dong_zuo` | `varchar(32)` | 否 | `NULL` | — | SUBMITTED/APPROVED/REJECTED/DISABLED |
| `yuan_zhuang_tai` | `varchar(16)` | 是 | `NULL` | — | — |
| `mu_biao_zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `shen_he_ren_id` | `bigint` | 是 | `NULL` | MUL | 用户模块建立后再加外键 |
| `shen_he_yi_jian` | `varchar(2000)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_ti_mu_shen_he_dong_zuo`；`CHECK:ck_ti_mu_shen_he_mu_biao_zhuang_tai`；`CHECK:ck_ti_mu_shen_he_yuan_zhuang_tai`；`FOREIGN KEY:fk_ti_mu_shen_he_ji_lu_shen_he_ren`；`FOREIGN KEY:fk_ti_mu_shen_he_ji_lu_ti_mu`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:idx_ti_mu_shen_he_ji_lu(ti_mu_id,chuang_jian_shi_jian)`；`INDEX:idx_ti_mu_shen_he_ren(shen_he_ren_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由审核状态轨迹对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_xuan_xiang`

用途：选择题选项。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xuan_xiang_biao_shi` | `varchar(16)` | 否 | `NULL` | — | A/B/C/D或未来更多标识 |
| `xuan_xiang_nei_rong` | `longtext` | 否 | `NULL` | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 否 | `0` | — | — |
| `pai_xu` | `int` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_xuan_xiang_pai_xu`；`CHECK:ck_ti_mu_xuan_xiang_yi_shan_chu`；`CHECK:ck_ti_mu_xuan_xiang_zheng_que`；`FOREIGN KEY:fk_ti_mu_xuan_xiang_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_xuan_xiang_biao_shi`。
索引：`INDEX:idx_ti_mu_xuan_xiang_pai_xu(ti_mu_id,pai_xu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_xuan_xiang_biao_shi(ti_mu_id,xuan_xiang_biao_shi)`。
生命周期：由选择题选项对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ti_mu_zhi_shi_dian`

用途：题目知识点。创建/演进：V2。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhi_shi_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_fou_zhu_yao` | `tinyint(1)` | 否 | `0` | — | — |
| `pai_xu` | `int` | 否 | `1` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_ti_mu_zhi_shi_dian_pai_xu`；`CHECK:ck_ti_mu_zhi_shi_dian_yi_shan_chu`；`CHECK:ck_ti_mu_zhi_shi_dian_zhu_yao`；`FOREIGN KEY:fk_ti_mu_zhi_shi_dian_ti_mu`；`FOREIGN KEY:fk_ti_mu_zhi_shi_dian_zhi_shi_dian`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ti_mu_zhi_shi_dian`。
索引：`INDEX:idx_ti_mu_zhi_shi_dian_fan_cha(zhi_shi_dian_id,ti_mu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id)`。
生命周期：由题目知识点对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `zhi_shi_dian`

用途：层级知识点。创建/演进：V1。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | 主键 |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | 所属科目 |
| `fu_zhi_shi_dian_id` | `bigint` | 是 | `NULL` | MUL | 父知识点 |
| `zhi_shi_dian_ming_cheng` | `varchar(128)` | 否 | `NULL` | — | 知识点名称 |
| `wan_zheng_lu_jing` | `varchar(500)` | 否 | `NULL` | — | 从一级到当前节点的完整路径 |
| `ceng_ji` | `smallint` | 否 | `NULL` | — | 层级，从1开始 |
| `pai_xu` | `int` | 否 | `0` | — | 同级显示顺序 |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | ACTIVE/DISABLED |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_zhi_shi_dian_ceng_ji`；`CHECK:ck_zhi_shi_dian_yi_shan_chu`；`CHECK:ck_zhi_shi_dian_zhuang_tai`；`FOREIGN KEY:fk_zhi_shi_dian_fu`；`FOREIGN KEY:fk_zhi_shi_dian_ke_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_zhi_shi_dian_lu_jing`。
索引：`INDEX:idx_zhi_shi_dian_fu(fu_zhi_shi_dian_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_zhi_shi_dian_lu_jing(ke_mu_id,wan_zheng_lu_jing)`。
生命周期：由层级知识点对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Practice

### `lian_xi_hui_hua`

用途：练习会话。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | `CREATED` | — | — |
| `ti_mu_shu` | `int` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_lian_xi_hui_hua_ti_mu_shu`；`CHECK:ck_lian_xi_hui_hua_zhuang_tai`；`FOREIGN KEY:fk_lian_xi_hui_hua_ke_mu`；`FOREIGN KEY:fk_lian_xi_hui_hua_xue_sheng`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:idx_lian_xi_hui_hua_ke_mu(ke_mu_id)`；`INDEX:idx_lian_xi_hui_hua_xue_sheng_zhuang_tai(xue_sheng_id,zhuang_tai,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由练习会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `lian_xi_ti_mu`

用途：冻结练习题。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `lian_xi_hui_hua_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_shun_xu` | `int` | 否 | `NULL` | — | — |
| `fen_zhi` | `decimal(8,2)` | 否 | `1.00` | — | — |
| `ti_mu_lei_xing` | `varchar(32)` | 否 | `NULL` | — | — |
| `nan_du_kuai_zhao` | `tinyint` | 否 | `NULL` | — | — |
| `ti_gan_kuai_zhao` | `longtext` | 否 | `NULL` | — | — |
| `xuan_xiang_kuai_zhao` | `json` | 是 | `NULL` | — | — |
| `zheng_que_da_an_kuai_zhao` | `json` | 否 | `NULL` | — | — |
| `biao_zhun_jie_xi_kuai_zhao` | `longtext` | 否 | `NULL` | — | — |
| `zhi_shi_dian_kuai_zhao` | `json` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_lian_xi_ti_mu_da_an_json`；`CHECK:ck_lian_xi_ti_mu_fen_zhi`；`CHECK:ck_lian_xi_ti_mu_lei_xing`；`CHECK:ck_lian_xi_ti_mu_nan_du`；`CHECK:ck_lian_xi_ti_mu_shun_xu`；`CHECK:ck_lian_xi_ti_mu_xuan_xiang_json`；`CHECK:ck_lian_xi_ti_mu_zhi_shi_dian_json`；`FOREIGN KEY:fk_lian_xi_ti_mu_hui_hua`；`FOREIGN KEY:fk_lian_xi_ti_mu_ti_mu`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_lian_xi_ti_mu_hui_hua_shun_xu`；`UNIQUE:uk_lian_xi_ti_mu_hui_hua_ti_mu`。
索引：`INDEX:idx_lian_xi_ti_mu_ti_mu(ti_mu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_lian_xi_ti_mu_hui_hua_shun_xu(lian_xi_hui_hua_id,ti_mu_shun_xu)`；`UNIQUE/PRIMARY:uk_lian_xi_ti_mu_hui_hua_ti_mu(lian_xi_hui_hua_id,ti_mu_id)`。
生命周期：由冻结练习题对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_sheng_da_ti`

用途：正式答题事实。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `lian_xi_ti_mu_id` | `bigint` | 否 | `NULL` | UNI | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_da_an` | `json` | 否 | `NULL` | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 否 | `NULL` | — | — |
| `de_fen` | `decimal(8,2)` | 否 | `NULL` | — | — |
| `yong_shi_miao_shu` | `int` | 是 | `NULL` | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_xue_sheng_da_ti_de_fen`；`CHECK:ck_xue_sheng_da_ti_yong_shi`；`CHECK:ck_xue_sheng_da_ti_zheng_que`；`FOREIGN KEY:fk_xue_sheng_da_ti_lian_xi_ti_mu`；`FOREIGN KEY:fk_xue_sheng_da_ti_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_sheng_da_ti_lian_xi_ti_mu`。
索引：`INDEX:idx_xue_sheng_da_ti_xue_sheng(xue_sheng_id,ti_jiao_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_xue_sheng_da_ti_lian_xi_ti_mu(lian_xi_ti_mu_id)`。
生命周期：由正式答题事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_xi_jie_guo`

用途：练习最终结果。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `lian_xi_hui_hua_id` | `bigint` | 否 | `NULL` | UNI | — |
| `zong_ti_shu` | `int` | 否 | `NULL` | — | — |
| `zheng_que_shu` | `int` | 否 | `NULL` | — | — |
| `zong_de_fen` | `decimal(10,2)` | 否 | `NULL` | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_xue_xi_jie_guo_de_fen`；`CHECK:ck_xue_xi_jie_guo_ji_shu`；`FOREIGN KEY:fk_xue_xi_jie_guo_hui_hua`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_xi_jie_guo_hui_hua`。
索引：`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_xue_xi_jie_guo_hui_hua(lian_xi_hui_hua_id)`。
生命周期：由练习最终结果对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Wrong / mastery

### `cuo_ti_ji_lu`

用途：错题生命周期。创建/演进：V7。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `cuo_wu_ci_shu` | `int` | 否 | `1` | — | — |
| `lian_xu_zheng_que_ci_shu` | `int` | 否 | `0` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `NEW` | — | — |
| `zui_jin_da_ti_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zui_jin_cuo_wu_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_cuo_ti_ji_lu_ci_shu`；`CHECK:ck_cuo_ti_ji_lu_zhuang_tai`；`FOREIGN KEY:fk_cuo_ti_ji_lu_ti_mu`；`FOREIGN KEY:fk_cuo_ti_ji_lu_xue_sheng`；`FOREIGN KEY:fk_cuo_ti_ji_lu_zui_jin_da_ti`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_cuo_ti_ji_lu_xue_sheng_ti_mu`。
索引：`INDEX:fk_cuo_ti_ji_lu_ti_mu(ti_mu_id)`；`INDEX:fk_cuo_ti_ji_lu_zui_jin_da_ti(zui_jin_da_ti_id)`；`INDEX:idx_cuo_ti_ji_lu_xue_sheng_zhuang_tai(xue_sheng_id,zhuang_tai,zui_jin_cuo_wu_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_cuo_ti_ji_lu_xue_sheng_ti_mu(xue_sheng_id,ti_mu_id)`。
生命周期：由错题生命周期对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `gao_pin_kao_dian`

用途：班级科目知识卡片/公式与口诀。创建/演进：V8。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ren_ke_guan_xi_id` | `bigint` | 否 | `NULL` | MUL | 所属三元任课关系 |
| `zhi_shi_dian_id` | `bigint` | 否 | `NULL` | MUL | 所属科目知识点 |
| `zi_liao_lei_xing` | `varchar(32)` | 否 | `POINT` | — | — |
| `biao_ti` | `varchar(200)` | 否 | `NULL` | — | — |
| `nei_rong` | `text` | 否 | `NULL` | — | — |
| `ke_xue_nei_rong` | `longtext` | 是 | `NULL` | — | — |
| `latex_nei_rong` | `longtext` | 是 | `NULL` | — | — |
| `shi_yong_tiao_jian` | `text` | 是 | `NULL` | — | — |
| `han_yi_tui_dao` | `text` | 是 | `NULL` | — | — |
| `li_zi` | `text` | 是 | `NULL` | — | — |
| `ji_yi_kou_jue` | `varchar(500)` | 是 | `NULL` | — | — |
| `chang_jian_wu_qu` | `text` | 是 | `NULL` | — | — |
| `lai_yuan_ming_cheng` | `varchar(255)` | 是 | `NULL` | — | — |
| `lai_yuan_di_zhi` | `varchar(1000)` | 是 | `NULL` | — | — |
| `quan_li_zhuang_tai` | `varchar(32)` | 否 | `PROJECT_AUTHORED` | — | — |
| `chuang_jian_ren_yong_hu_id` | `bigint` | 是 | `NULL` | MUL | — |
| `pai_xu` | `int` | 否 | `0` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_gao_pin_kao_dian_content`；`CHECK:ck_gao_pin_kao_dian_deleted`；`CHECK:ck_gao_pin_kao_dian_order`；`CHECK:ck_gao_pin_kao_dian_rights`；`CHECK:ck_gao_pin_kao_dian_status`；`CHECK:ck_gao_pin_kao_dian_title`；`CHECK:ck_gao_pin_kao_dian_type`；`FOREIGN KEY:fk_gao_pin_kao_dian_creator`；`FOREIGN KEY:fk_gao_pin_kao_dian_knowledge`；`FOREIGN KEY:fk_gao_pin_kao_dian_scope`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_gao_pin_kao_dian_creator(chuang_jian_ren_yong_hu_id)`；`INDEX:idx_gao_pin_kao_dian_knowledge(zhi_shi_dian_id)`；`INDEX:idx_gao_pin_kao_dian_scope_status(ren_ke_guan_xi_id,zhuang_tai,yi_shan_chu,pai_xu,id)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由班级科目知识卡片/公式与口诀对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `gao_pin_kao_dian_fu_jian`

用途：知识卡片受控图片附件。创建/演进：V21。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `gao_pin_kao_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `yuan_shi_wen_jian_ming` | `varchar(255)` | 否 | `NULL` | — | — |
| `xiang_dui_lu_jing` | `varchar(1000)` | 否 | `NULL` | — | — |
| `mime_lei_xing` | `varchar(32)` | 否 | `NULL` | — | — |
| `nei_rong_ha_xi` | `char(64)` | 否 | `NULL` | — | — |
| `wen_jian_da_xiao` | `bigint` | 否 | `NULL` | — | — |
| `pai_xu` | `int` | 否 | `1` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_kao_dian_attachment_deleted`；`CHECK:ck_kao_dian_attachment_mime`；`CHECK:ck_kao_dian_attachment_order`；`CHECK:ck_kao_dian_attachment_size`；`CHECK:ck_kao_dian_attachment_status`；`FOREIGN KEY:fk_kao_dian_attachment_card`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_kao_dian_attachment_hash`。
索引：`INDEX:idx_kao_dian_attachment(gao_pin_kao_dian_id,zhuang_tai,yi_shan_chu,pai_xu)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_kao_dian_attachment_hash(gao_pin_kao_dian_id,nei_rong_ha_xi)`。
生命周期：由知识卡片受控图片附件对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `gao_pin_kao_dian_shen_he_ji_lu`

用途：知识卡片人工审核轨迹。创建/演进：V28。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `gao_pin_kao_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shen_he_dong_zuo` | `varchar(16)` | 否 | `NULL` | — | — |
| `yuan_zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `mu_biao_zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `shen_he_ren_yong_hu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shen_he_yi_jian` | `varchar(1000)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_gao_pin_kao_dian_review_action`；`CHECK:ck_gao_pin_kao_dian_review_states`；`FOREIGN KEY:fk_gao_pin_kao_dian_review_card`；`FOREIGN KEY:fk_gao_pin_kao_dian_review_user`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_gao_pin_kao_dian_review_user(shen_he_ren_yong_hu_id)`；`INDEX:idx_gao_pin_kao_dian_review(gao_pin_kao_dian_id,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由知识卡片人工审核轨迹对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `gao_pin_kao_dian_zhi_shi_dian`

用途：知识卡片多知识点关系。创建/演进：V21。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `gao_pin_kao_dian_id` | `bigint` | 否 | `NULL` | PRI | — |
| `zhi_shi_dian_id` | `bigint` | 否 | `NULL` | PRI | — |
| `pai_xu` | `int` | 否 | `1` | — | — |

约束：`CHECK:ck_kao_dian_point_order`；`FOREIGN KEY:fk_kao_dian_point_card`；`FOREIGN KEY:fk_kao_dian_point_point`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:idx_kao_dian_point_reverse(zhi_shi_dian_id,gao_pin_kao_dian_id)`；`UNIQUE/PRIMARY:PRIMARY(gao_pin_kao_dian_id,zhi_shi_dian_id)`。
生命周期：由知识卡片多知识点关系对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `xue_sheng_zhi_shi_ka_pian_zhuang_tai`

用途：学生卡片收藏与掌握状态。创建/演进：V28。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `gao_pin_kao_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_fou_shou_cang` | `tinyint(1)` | 否 | `0` | — | — |
| `zhang_wo_zhuang_tai` | `varchar(16)` | 否 | `LEARNING` | — | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_xue_sheng_ka_pian_favorite`；`CHECK:ck_xue_sheng_ka_pian_mastery`；`FOREIGN KEY:fk_xue_sheng_ka_pian_state_card`；`FOREIGN KEY:fk_xue_sheng_ka_pian_state_student`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_xue_sheng_ka_pian_state`。
索引：`INDEX:fk_xue_sheng_ka_pian_state_card(gao_pin_kao_dian_id)`；`INDEX:idx_xue_sheng_ka_pian_filter(xue_sheng_id,shi_fou_shou_cang,zhang_wo_zhuang_tai)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_xue_sheng_ka_pian_state(xue_sheng_id,gao_pin_kao_dian_id)`。
生命周期：由学生卡片收藏与掌握状态对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `zhi_shi_ka_pian_lian_xi_shi_li`

用途：知识卡片临时生成练习及审核状态。创建/演进：V29。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `gao_pin_kao_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ai_sheng_cheng_ren_wu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_da_an` | `json` | 是 | `NULL` | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 是 | `NULL` | — | — |
| `zhuang_tai` | `varchar(24)` | 否 | `READY` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `zuo_da_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |

约束：`CHECK:ck_card_practice_answer_state`；`CHECK:ck_card_practice_correct`；`CHECK:ck_card_practice_state`；`FOREIGN KEY:fk_card_practice_card`；`FOREIGN KEY:fk_card_practice_question`；`FOREIGN KEY:fk_card_practice_student`；`FOREIGN KEY:fk_card_practice_task`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_card_practice_task_question_student`。
索引：`INDEX:fk_card_practice_question(ti_mu_id)`；`INDEX:idx_card_practice_card(gao_pin_kao_dian_id,chuang_jian_shi_jian)`；`INDEX:idx_card_practice_student(xue_sheng_id,zhuang_tai,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_card_practice_task_question_student(ai_sheng_cheng_ren_wu_id,ti_mu_id,xue_sheng_id)`。
生命周期：由知识卡片临时生成练习及审核状态对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Communication

### `si_xin_hui_hua`

用途：师生私信会话。创建/演进：V9。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ren_ke_guan_xi_id` | `bigint` | 否 | `NULL` | MUL | 会话对应的三元任课关系 |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | 会话学生档案 |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `zui_hou_xiao_xi_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_si_xin_hui_hua_deleted`；`CHECK:ck_si_xin_hui_hua_status`；`FOREIGN KEY:fk_si_xin_hui_hua_scope`；`FOREIGN KEY:fk_si_xin_hui_hua_student`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_si_xin_hui_hua_scope_student`。
索引：`INDEX:idx_si_xin_hui_hua_student_recent(xue_sheng_id,yi_shan_chu,zui_hou_xiao_xi_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_si_xin_hui_hua_scope_student(ren_ke_guan_xi_id,xue_sheng_id)`。
生命周期：由师生私信会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `si_xin_xiao_xi`

用途：支持撤回与按用户隐藏的师生私信消息。创建/演进：V9。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `hui_hua_id` | `bigint` | 否 | `NULL` | MUL | — |
| `fa_song_ren_yong_hu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `nei_rong` | `varchar(1000)` | 否 | `NULL` | — | — |
| `yi_du` | `tinyint` | 否 | `0` | — | — |
| `fa_song_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `yi_du_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `che_hui_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `fa_song_zhe_yi_cang` | `tinyint(1)` | 否 | `0` | — | — |
| `jie_shou_zhe_yi_cang` | `tinyint(1)` | 否 | `0` | — | — |
| `yi_shan_chu` | `tinyint` | 否 | `0` | — | — |

约束：`CHECK:ck_si_xin_xiao_xi_content`；`CHECK:ck_si_xin_xiao_xi_deleted`；`CHECK:ck_si_xin_xiao_xi_read`；`CHECK:ck_si_xin_xiao_xi_read_time`；`CHECK:ck_si_xin_xiao_xi_receiver_hidden`；`CHECK:ck_si_xin_xiao_xi_sender_hidden`；`FOREIGN KEY:fk_si_xin_xiao_xi_conversation`；`FOREIGN KEY:fk_si_xin_xiao_xi_sender`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_si_xin_xiao_xi_sender(fa_song_ren_yong_hu_id)`；`INDEX:idx_si_xin_xiao_xi_conversation_time(hui_hua_id,fa_song_shi_jian,id)`；`INDEX:idx_si_xin_xiao_xi_receiver_visibility(hui_hua_id,jie_shou_zhe_yi_cang,fa_song_shi_jian,id)`；`INDEX:idx_si_xin_xiao_xi_sender_visibility(hui_hua_id,fa_song_zhe_yi_cang,fa_song_shi_jian,id)`；`INDEX:idx_si_xin_xiao_xi_unread(hui_hua_id,yi_du,fa_song_ren_yong_hu_id,yi_shan_chu)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由支持撤回与按用户隐藏的师生私信消息对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Audit

### `guan_li_cao_zuo_ri_zhi`

用途：管理员操作审计。创建/演进：V11。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `cao_zuo_ren_yong_hu_id` | `bigint` | 是 | `NULL` | MUL | — |
| `mo_kuai` | `varchar(64)` | 否 | `NULL` | MUL | — |
| `cao_zuo_lei_xing` | `varchar(96)` | 否 | `NULL` | — | — |
| `ye_wu_dui_xiang_id` | `bigint` | 是 | `NULL` | — | — |
| `cao_zuo_jie_guo` | `varchar(16)` | 否 | `NULL` | — | — |
| `zhai_yao` | `varchar(1000)` | 是 | `NULL` | — | — |
| `cuo_wu_dai_ma` | `varchar(96)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | MUL, DEFAULT_GENERATED | — |

约束：`CHECK:ck_guan_li_cao_zuo_ri_zhi_result`；`FOREIGN KEY:fk_guan_li_cao_zuo_ri_zhi_operator`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:idx_guan_li_cao_zuo_ri_zhi_filter(mo_kuai,cao_zuo_lei_xing,cao_zuo_jie_guo)`；`INDEX:idx_guan_li_cao_zuo_ri_zhi_operator(cao_zuo_ren_yong_hu_id,chuang_jian_shi_jian)`；`INDEX:idx_guan_li_cao_zuo_ri_zhi_time(chuang_jian_shi_jian,id)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由管理员操作审计对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## AI Provider

### `ai_diao_yong_ri_zhi`

用途：AI 调用安全元数据。创建/演进：V12。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `provider_dai_ma` | `varchar(64)` | 否 | `NULL` | MUL | — |
| `model_dai_ma` | `varchar(128)` | 否 | `NULL` | — | — |
| `yong_tu` | `varchar(96)` | 否 | `NULL` | — | — |
| `ye_wu_guan_lian` | `varchar(128)` | 是 | `NULL` | — | — |
| `shi_fou_cheng_gong` | `tinyint(1)` | 否 | `NULL` | — | — |
| `hao_shi_hao_miao` | `bigint` | 否 | `NULL` | — | — |
| `shu_ru_token` | `int` | 是 | `NULL` | — | — |
| `shu_chu_token` | `int` | 是 | `NULL` | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | MUL, DEFAULT_GENERATED | — |

约束：`CHECK:ck_ai_diao_yong_latency`；`CHECK:ck_ai_diao_yong_success`；`CHECK:ck_ai_diao_yong_tokens`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:idx_ai_diao_yong_created(chuang_jian_shi_jian,id)`；`INDEX:idx_ai_diao_yong_provider_success(provider_dai_ma,shi_fou_cheng_gong,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由AI 调用安全元数据对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_mo_xing_pei_zhi`

用途：本地 AI Provider/模型配置。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `provider_dai_ma` | `varchar(32)` | 否 | `NULL` | MUL | DEEPSEEK/GLM |
| `mo_xing_dai_ma` | `varchar(128)` | 否 | `NULL` | — | — |
| `api_di_zhi` | `varchar(500)` | 否 | `NULL` | — | — |
| `api_mi_yao` | `varchar(1000)` | 是 | `NULL` | — | 仅本地毕设演示模式保存，API与日志禁止回显 |
| `yong_tu` | `varchar(16)` | 否 | `NULL` | MUL | TEXT/VISION |
| `shi_fou_qi_yong` | `tinyint(1)` | 否 | `0` | — | — |
| `shi_fou_mo_ren` | `tinyint(1)` | 否 | `0` | — | — |
| `chao_shi_hao_miao` | `int` | 否 | `30000` | — | — |
| `zui_da_token` | `int` | 否 | `1200` | — | — |
| `retry_count` | `tinyint` | 否 | `1` | — | — |
| `zui_jin_ce_shi_zhuang_tai` | `varchar(16)` | 否 | `NOT_TESTED` | — | — |
| `zui_jin_ce_shi_hao_shi` | `bigint` | 是 | `NULL` | — | — |
| `zui_jin_ce_shi_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_mo_xing_flags`；`CHECK:ck_ai_mo_xing_provider`；`CHECK:ck_ai_mo_xing_retry`；`CHECK:ck_ai_mo_xing_test_status`；`CHECK:ck_ai_mo_xing_timeout`；`CHECK:ck_ai_mo_xing_tokens`；`CHECK:ck_ai_mo_xing_usage`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_mo_xing_provider_model_usage`。
索引：`INDEX:idx_ai_mo_xing_default(yong_tu,shi_fou_qi_yong,shi_fou_mo_ren)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_mo_xing_provider_model_usage(provider_dai_ma,mo_xing_dai_ma,yong_tu)`。
生命周期：由本地 AI Provider/模型配置对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Student AI

### `ai_cuo_ti_fen_xi`

用途：错题结构化 AI 分析。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_da_ti_id` | `bigint` | 否 | `NULL` | UNI | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `cuo_wu_lei_xing` | `varchar(32)` | 是 | `NULL` | — | — |
| `cuo_wu_yuan_yin` | `varchar(1200)` | 是 | `NULL` | — | — |
| `zheng_que_si_lu` | `varchar(1600)` | 是 | `NULL` | — | — |
| `chang_jian_cuo_wu` | `json` | 是 | `NULL` | — | — |
| `fu_xi_jian_yi` | `json` | 是 | `NULL` | — | — |
| `provider_dai_ma` | `varchar(64)` | 是 | `NULL` | — | — |
| `model_dai_ma` | `varchar(128)` | 是 | `NULL` | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | `NULL` | — | — |
| `shu_ru_shi_shi_ha_xi` | `char(64)` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_cuo_ti_fen_xi_arrays`；`CHECK:ck_ai_cuo_ti_fen_xi_status`；`CHECK:ck_ai_cuo_ti_fen_xi_success`；`CHECK:ck_ai_cuo_ti_fen_xi_type`；`FOREIGN KEY:fk_ai_cuo_ti_fen_xi_da_ti`；`FOREIGN KEY:fk_ai_cuo_ti_fen_xi_xue_sheng`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_cuo_ti_fen_xi_da_ti`。
索引：`INDEX:idx_ai_cuo_ti_fen_xi_xue_sheng_status(xue_sheng_id,zhuang_tai,geng_xin_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_cuo_ti_fen_xi_da_ti(xue_sheng_da_ti_id)`。
生命周期：由错题结构化 AI 分析对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_hui_hua`

用途：练习结果或专题题互斥上下文 AI 会话。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_da_ti_id` | `bigint` | 是 | `NULL` | MUL | — |
| `lian_xi_ti_mu_id` | `bigint` | 是 | `NULL` | MUL | — |
| `shang_xia_wen_lei_xing` | `varchar(24)` | 否 | `PRACTICE_RESULT` | — | — |
| `zhuan_ti_ti_mu_id` | `bigint` | 是 | `NULL` | MUL | — |
| `zhi_shi_ka_pian_id` | `bigint` | 是 | `NULL` | MUL | — |
| `ai_mo_xing_pei_zhi_id` | `bigint` | 是 | `NULL` | MUL | — |
| `si_kao_mo_shi` | `varchar(16)` | 否 | `STANDARD` | — | — |
| `shi_fou_lian_wang` | `tinyint(1)` | 否 | `0` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `ACTIVE` | — | — |
| `lei_ji_lun_shu` | `int` | 否 | `0` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_hui_hua_context`；`CHECK:ck_ai_hui_hua_rounds`；`CHECK:ck_ai_hui_hua_search`；`CHECK:ck_ai_hui_hua_status`；`CHECK:ck_ai_hui_hua_thinking`；`FOREIGN KEY:fk_ai_hui_hua_da_ti`；`FOREIGN KEY:fk_ai_hui_hua_knowledge_card`；`FOREIGN KEY:fk_ai_hui_hua_lian_xi_ti_mu`；`FOREIGN KEY:fk_ai_hui_hua_model`；`FOREIGN KEY:fk_ai_hui_hua_topic_question`；`FOREIGN KEY:fk_ai_hui_hua_xue_sheng`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_ai_hui_hua_da_ti(xue_sheng_da_ti_id)`；`INDEX:fk_ai_hui_hua_knowledge_card(zhi_shi_ka_pian_id)`；`INDEX:fk_ai_hui_hua_lian_xi_ti_mu(lian_xi_ti_mu_id)`；`INDEX:fk_ai_hui_hua_model(ai_mo_xing_pei_zhi_id)`；`INDEX:fk_ai_hui_hua_topic_question(zhuan_ti_ti_mu_id)`；`INDEX:idx_ai_hui_hua_card(xue_sheng_id,zhi_shi_ka_pian_id,geng_xin_shi_jian)`；`INDEX:idx_ai_hui_hua_topic(xue_sheng_id,zhuan_ti_ti_mu_id,geng_xin_shi_jian)`；`INDEX:idx_ai_hui_hua_xue_sheng_question(xue_sheng_id,lian_xi_ti_mu_id,geng_xin_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由练习结果或专题题互斥上下文 AI 会话对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_xiao_xi`

用途：当前题 AI 消息。创建/演进：V13。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ai_hui_hua_id` | `bigint` | 否 | `NULL` | MUL | — |
| `fa_yan_jiao_se` | `varchar(16)` | 否 | `NULL` | — | — |
| `nei_rong` | `varchar(2000)` | 否 | `NULL` | — | — |
| `lian_wang_lai_yuan` | `json` | 是 | `NULL` | — | — |
| `xu_hao` | `int` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_ai_xiao_xi_content`；`CHECK:ck_ai_xiao_xi_order`；`CHECK:ck_ai_xiao_xi_role`；`CHECK:ck_ai_xiao_xi_sources`；`FOREIGN KEY:fk_ai_xiao_xi_hui_hua`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_xiao_xi_hui_hua_xu_hao`。
索引：`INDEX:idx_ai_xiao_xi_hui_hua_created(ai_hui_hua_id,chuang_jian_shi_jian,id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_xiao_xi_hui_hua_xu_hao(ai_hui_hua_id,xu_hao)`。
生命周期：由当前题 AI 消息对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Student AI variants

### `ai_xue_sheng_bian_shi_shi_li`

用途：绑定答题事实的学生结构化变式。创建/演进：V19。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_da_ti_id` | `bigint` | 否 | `NULL` | MUL | — |
| `mu_ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ai_sheng_cheng_ren_wu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(24)` | 否 | `READY` | — | — |
| `xue_sheng_da_an` | `json` | 是 | `NULL` | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 是 | `NULL` | — | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `shen_he_ti_jiao_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_xue_sheng_variant_correct`；`CHECK:ck_ai_xue_sheng_variant_status`；`FOREIGN KEY:fk_ai_xue_sheng_variant_fact`；`FOREIGN KEY:fk_ai_xue_sheng_variant_mother`；`FOREIGN KEY:fk_ai_xue_sheng_variant_question`；`FOREIGN KEY:fk_ai_xue_sheng_variant_student`；`FOREIGN KEY:fk_ai_xue_sheng_variant_task`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_xue_sheng_variant_question`。
索引：`INDEX:fk_ai_xue_sheng_variant_mother(mu_ti_mu_id)`；`INDEX:fk_ai_xue_sheng_variant_question(ti_mu_id)`；`INDEX:fk_ai_xue_sheng_variant_task(ai_sheng_cheng_ren_wu_id)`；`INDEX:idx_ai_xue_sheng_variant_fact(xue_sheng_da_ti_id,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_xue_sheng_variant_question(xue_sheng_id,ti_mu_id)`。
生命周期：由绑定答题事实的学生结构化变式对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## AI generation / vision

### `ai_hou_xuan_ti_zhi_liang_ping_jia`

用途：候选题人工质量评价。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ai_sheng_cheng_ren_wu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | UNI | — |
| `bian_shi_zhai_yao` | `varchar(1000)` | 否 | `NULL` | — | — |
| `bian_shi_fang_shi` | `varchar(48)` | 是 | `NULL` | — | — |
| `bian_hua_wei_du` | `json` | 是 | `NULL` | — | — |
| `xin_ying_du_fen_shu` | `decimal(6,5)` | 是 | `NULL` | — | — |
| `xiang_si_du_fen_shu` | `decimal(6,5)` | 是 | `NULL` | — | — |
| `ju_jue_yuan_yin` | `varchar(96)` | 是 | `NULL` | — | — |
| `chong_fu_ti_shi` | `varchar(32)` | 否 | `NONE` | — | — |
| `shi_fou_shi_yong_shi_jue` | `tinyint(1)` | 否 | `0` | — | — |
| `xue_ke_zheng_que_xing` | `tinyint` | 是 | `NULL` | — | — |
| `da_an_zheng_que_xing` | `tinyint` | 是 | `NULL` | — | — |
| `ke_jie_xing` | `tinyint` | 是 | `NULL` | — | — |
| `zhi_shi_yi_zhi_xing` | `tinyint` | 是 | `NULL` | — | — |
| `nan_du_pi_pei` | `tinyint` | 是 | `NULL` | — | — |
| `shen_he_jie_guo` | `varchar(16)` | 否 | `PENDING` | — | — |
| `shen_he_hao_shi_fen_zhong` | `int` | 是 | `NULL` | — | — |
| `shen_he_ren_id` | `bigint` | 是 | `NULL` | MUL | — |
| `shen_he_ping_lun` | `varchar(2000)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_candidate_novelty_score`；`CHECK:ck_ai_candidate_similarity_score`；`CHECK:ck_ai_candidate_variation_mode`；`CHECK:ck_ai_quality_binary`；`CHECK:ck_ai_quality_duplicate`；`CHECK:ck_ai_quality_result`；`CHECK:ck_ai_quality_review_minutes`；`CHECK:ck_ai_quality_vision`；`FOREIGN KEY:fk_ai_quality_question`；`FOREIGN KEY:fk_ai_quality_reviewer`；`FOREIGN KEY:fk_ai_quality_task`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_quality_question`。
索引：`INDEX:fk_ai_quality_reviewer(shen_he_ren_id)`；`INDEX:idx_ai_quality_task(ai_sheng_cheng_ren_wu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_quality_question(ti_mu_id)`。
生命周期：由候选题人工质量评价对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_sheng_cheng_ren_wu`

用途：候选变式题生成任务。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `mu_ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `chuang_jian_ren_id` | `bigint` | 否 | `NULL` | MUL | — |
| `chuang_jian_ren_jiao_se` | `varchar(16)` | 否 | `NULL` | — | — |
| `mu_biao_ti_xing` | `varchar(32)` | 否 | `NULL` | — | — |
| `zhi_shi_dian_ids` | `json` | 否 | `NULL` | — | — |
| `mu_biao_nan_du` | `tinyint` | 否 | `NULL` | — | — |
| `bian_shi_fang_shi` | `varchar(32)` | 否 | `NULL` | — | — |
| `sheng_cheng_shu_liang` | `tinyint` | 否 | `NULL` | — | — |
| `qing_qiu_ha_xi` | `char(64)` | 否 | `NULL` | UNI | — |
| `provider_dai_ma` | `varchar(32)` | 是 | `NULL` | — | — |
| `model_dai_ma` | `varchar(128)` | 是 | `NULL` | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `yi_sheng_cheng_shu_liang` | `tinyint` | 否 | `0` | — | — |
| `shi_fou_shi_yong_shi_jue` | `tinyint(1)` | 否 | `0` | — | — |
| `shi_bai_dai_ma` | `varchar(64)` | 是 | `NULL` | — | — |
| `hao_shi_hao_miao` | `bigint` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `wan_cheng_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |

约束：`CHECK:ck_ai_sheng_cheng_count`；`CHECK:ck_ai_sheng_cheng_difficulty`；`CHECK:ck_ai_sheng_cheng_mode`；`CHECK:ck_ai_sheng_cheng_points`；`CHECK:ck_ai_sheng_cheng_role`；`CHECK:ck_ai_sheng_cheng_status`；`CHECK:ck_ai_sheng_cheng_type`；`CHECK:ck_ai_sheng_cheng_vision`；`FOREIGN KEY:fk_ai_sheng_cheng_creator`；`FOREIGN KEY:fk_ai_sheng_cheng_mother`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_sheng_cheng_request_hash`。
索引：`INDEX:fk_ai_sheng_cheng_creator(chuang_jian_ren_id)`；`INDEX:idx_ai_sheng_cheng_mother_status(mu_ti_mu_id,zhuang_tai,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_sheng_cheng_request_hash(qing_qiu_ha_xi)`。
生命周期：由候选变式题生成任务对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `ai_shi_jue_shang_xia_wen`

用途：受控视觉上下文缓存。创建/演进：V14。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `fu_jian_ji_he_ha_xi` | `char(64)` | 否 | `NULL` | — | — |
| `provider_dai_ma` | `varchar(32)` | 否 | `NULL` | — | — |
| `model_dai_ma` | `varchar(128)` | 否 | `NULL` | — | — |
| `prompt_ban_ben` | `varchar(32)` | 否 | `NULL` | — | — |
| `shi_jue_json` | `json` | 是 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `NULL` | — | — |
| `cuo_wu_dai_ma` | `varchar(64)` | 是 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_ai_vision_json`；`CHECK:ck_ai_vision_status`；`FOREIGN KEY:fk_ai_vision_question`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_ai_vision_context`。
索引：`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_ai_vision_context(ti_mu_id,fu_jian_ji_he_ha_xi,provider_dai_ma,model_dai_ma,prompt_ban_ben)`。
生命周期：由受控视觉上下文缓存对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Account recovery

### `mi_ma_chong_zhi_shen_qing`

用途：匿名密码恢复请求与处理事实。创建/演进：V17。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `yong_hu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | `PENDING` | MUL | — |
| `shen_qing_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `chu_li_ren_id` | `bigint` | 是 | `NULL` | MUL | — |
| `chu_li_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `chu_li_jie_guo` | `varchar(500)` | 是 | `NULL` | — | — |
| `pending_yong_hu_id` | `bigint` | 是 | `NULL` | UNI, STORED GENERATED | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_mi_ma_chong_zhi_resolution`；`CHECK:ck_mi_ma_chong_zhi_status`；`FOREIGN KEY:fk_mi_ma_chong_zhi_handler`；`FOREIGN KEY:fk_mi_ma_chong_zhi_user`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_mi_ma_chong_zhi_pending`。
索引：`INDEX:fk_mi_ma_chong_zhi_handler(chu_li_ren_id)`；`INDEX:fk_mi_ma_chong_zhi_user(yong_hu_id)`；`INDEX:idx_mi_ma_chong_zhi_status_time(zhuang_tai,shen_qing_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_mi_ma_chong_zhi_pending(pending_yong_hu_id)`。
生命周期：由匿名密码恢复请求与处理事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Paper

### `shi_juan`

用途：教师冻结试卷。创建/演进：V18。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `chuang_jian_jiao_shi_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_juan_ming_cheng` | `varchar(120)` | 否 | `NULL` | — | — |
| `zu_juan_mo_shi` | `varchar(16)` | 否 | `NULL` | — | — |
| `zong_fen` | `decimal(8,2)` | 否 | `0.00` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `DRAFT` | — | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_shi_juan_deleted`；`CHECK:ck_shi_juan_mode`；`CHECK:ck_shi_juan_score`；`CHECK:ck_shi_juan_status`；`FOREIGN KEY:fk_shi_juan_subject`；`FOREIGN KEY:fk_shi_juan_teacher`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_shi_juan_subject(ke_mu_id)`；`INDEX:idx_shi_juan_teacher(chuang_jian_jiao_shi_id,yi_shan_chu,geng_xin_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由教师冻结试卷对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `shi_juan_fa_bu`

用途：绑定任课范围的试卷发布。创建/演进：V27。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `shi_juan_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ren_ke_guan_xi_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ban_ji_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `fa_bu_jiao_shi_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ban_ben_hao` | `int` | 否 | `1` | — | — |
| `kuai_zhao_ha_xi` | `char(64)` | 否 | `NULL` | — | — |
| `fa_bu_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `jie_zhi_shi_jian` | `datetime(3)` | 否 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `PUBLISHED` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_shi_juan_fa_bu_status`；`CHECK:ck_shi_juan_fa_bu_time`；`CHECK:ck_shi_juan_fa_bu_version`；`FOREIGN KEY:fk_shi_juan_fa_bu_class`；`FOREIGN KEY:fk_shi_juan_fa_bu_paper`；`FOREIGN KEY:fk_shi_juan_fa_bu_scope`；`FOREIGN KEY:fk_shi_juan_fa_bu_subject`；`FOREIGN KEY:fk_shi_juan_fa_bu_teacher`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_shi_juan_fa_bu_version_scope`。
索引：`INDEX:fk_shi_juan_fa_bu_scope(ren_ke_guan_xi_id)`；`INDEX:fk_shi_juan_fa_bu_subject(ke_mu_id)`；`INDEX:idx_shi_juan_fa_bu_class_subject(ban_ji_id,ke_mu_id,zhuang_tai,jie_zhi_shi_jian)`；`INDEX:idx_shi_juan_fa_bu_teacher(fa_bu_jiao_shi_id,chuang_jian_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_shi_juan_fa_bu_version_scope(shi_juan_id,ban_ben_hao,ren_ke_guan_xi_id)`。
生命周期：由绑定任课范围的试卷发布对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `shi_juan_fa_bu_ti_mu`

用途：已发布试卷冻结题目快照。创建/演进：V27。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `shi_juan_fa_bu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_shun_xu` | `int` | 否 | `NULL` | — | — |
| `fen_zhi` | `decimal(8,2)` | 否 | `NULL` | — | — |
| `ti_mu_lei_xing` | `varchar(32)` | 否 | `NULL` | — | — |
| `ti_gan_kuai_zhao` | `longtext` | 否 | `NULL` | — | — |
| `xuan_xiang_kuai_zhao` | `json` | 是 | `NULL` | — | — |
| `zheng_que_da_an_kuai_zhao` | `json` | 否 | `NULL` | — | — |
| `biao_zhun_jie_xi_kuai_zhao` | `longtext` | 否 | `NULL` | — | — |
| `zhi_shi_dian_kuai_zhao` | `json` | 否 | `NULL` | — | — |

约束：`CHECK:ck_shi_juan_fa_bu_ti_mu_answer`；`CHECK:ck_shi_juan_fa_bu_ti_mu_options`；`CHECK:ck_shi_juan_fa_bu_ti_mu_order`；`CHECK:ck_shi_juan_fa_bu_ti_mu_points`；`CHECK:ck_shi_juan_fa_bu_ti_mu_score`；`CHECK:ck_shi_juan_fa_bu_ti_mu_type`；`FOREIGN KEY:fk_shi_juan_fa_bu_ti_mu_question`；`FOREIGN KEY:fk_shi_juan_fa_bu_ti_mu_release`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_shi_juan_fa_bu_ti_mu_order`；`UNIQUE:uk_shi_juan_fa_bu_ti_mu_question`。
索引：`INDEX:idx_shi_juan_fa_bu_ti_mu_question(ti_mu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_shi_juan_fa_bu_ti_mu_order(shi_juan_fa_bu_id,ti_mu_shun_xu)`；`UNIQUE/PRIMARY:uk_shi_juan_fa_bu_ti_mu_question(shi_juan_fa_bu_id,ti_mu_id)`。
生命周期：由已发布试卷冻结题目快照对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `shi_juan_ti_jiao`

用途：学生试卷草稿、提交与确定性得分。创建/演进：V27。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `shi_juan_fa_bu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_id` | `bigint` | 否 | `NULL` | MUL | — |
| `zhuang_tai` | `varchar(16)` | 否 | `IN_PROGRESS` | — | — |
| `ke_guan_de_fen` | `decimal(10,2)` | 否 | `0.00` | — | — |
| `ke_guan_zong_fen` | `decimal(10,2)` | 否 | `0.00` | — | — |
| `kai_shi_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `ti_jiao_shi_jian` | `datetime(3)` | 是 | `NULL` | — | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |

约束：`CHECK:ck_shi_juan_ti_jiao_score`；`CHECK:ck_shi_juan_ti_jiao_status`；`CHECK:ck_shi_juan_ti_jiao_time`；`FOREIGN KEY:fk_shi_juan_ti_jiao_release`；`FOREIGN KEY:fk_shi_juan_ti_jiao_student`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_shi_juan_ti_jiao_release_student`。
索引：`INDEX:idx_shi_juan_ti_jiao_student(xue_sheng_id,zhuang_tai,geng_xin_shi_jian)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_shi_juan_ti_jiao_release_student(shi_juan_fa_bu_id,xue_sheng_id)`。
生命周期：由学生试卷草稿、提交与确定性得分对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `shi_juan_ti_mu`

用途：试卷题目顺序与分值。创建/演进：V18。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `shi_juan_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `ti_mu_shun_xu` | `int` | 否 | `NULL` | — | — |
| `fen_zhi` | `decimal(8,2)` | 否 | `NULL` | — | — |

约束：`CHECK:ck_shi_juan_ti_mu_order`；`CHECK:ck_shi_juan_ti_mu_score`；`FOREIGN KEY:fk_shi_juan_ti_mu_paper`；`FOREIGN KEY:fk_shi_juan_ti_mu_question`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_shi_juan_order`；`UNIQUE:uk_shi_juan_question`。
索引：`INDEX:fk_shi_juan_ti_mu_question(ti_mu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_shi_juan_order(shi_juan_id,ti_mu_shun_xu)`；`UNIQUE/PRIMARY:uk_shi_juan_question(shi_juan_id,ti_mu_id)`。
生命周期：由试卷题目顺序与分值对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `shi_juan_xue_sheng_da_ti`

用途：学生逐题作答事实。创建/演进：V27。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `shi_juan_ti_jiao_id` | `bigint` | 否 | `NULL` | MUL | — |
| `shi_juan_fa_bu_ti_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `xue_sheng_da_an` | `json` | 是 | `NULL` | — | — |
| `shi_fou_zheng_que` | `tinyint(1)` | 是 | `NULL` | — | — |
| `de_fen` | `decimal(8,2)` | 是 | `NULL` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `DRAFT` | — | — |
| `bao_cun_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |

约束：`CHECK:ck_shi_juan_xue_sheng_da_ti_correct`；`CHECK:ck_shi_juan_xue_sheng_da_ti_score`；`CHECK:ck_shi_juan_xue_sheng_da_ti_status`；`FOREIGN KEY:fk_shi_juan_xue_sheng_da_ti_item`；`FOREIGN KEY:fk_shi_juan_xue_sheng_da_ti_submission`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_shi_juan_xue_sheng_da_ti_submission_item`。
索引：`INDEX:idx_shi_juan_xue_sheng_da_ti_item(shi_juan_fa_bu_ti_mu_id)`；`UNIQUE/PRIMARY:PRIMARY(id)`；`UNIQUE/PRIMARY:uk_shi_juan_xue_sheng_da_ti_submission_item(shi_juan_ti_jiao_id,shi_juan_fa_bu_ti_mu_id)`。
生命周期：由学生逐题作答事实对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

## Topic units

### `zhuan_ti_xue_xi_dan_yuan`

用途：分层专题学习单元。创建/演进：V26。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `id` | `bigint` | 否 | `NULL` | PRI, auto_increment | — |
| `ke_mu_id` | `bigint` | 否 | `NULL` | MUL | — |
| `biao_ti` | `varchar(200)` | 否 | `NULL` | — | — |
| `jian_jie` | `varchar(1000)` | 否 | `NULL` | — | — |
| `nan_du_ceng_ji` | `tinyint` | 否 | `NULL` | — | — |
| `zhu_zhi_shi_dian_id` | `bigint` | 否 | `NULL` | MUL | — |
| `pai_xu` | `int` | 否 | `1` | — | — |
| `zhuang_tai` | `varchar(16)` | 否 | `DRAFT` | — | — |
| `chuang_jian_ren_id` | `bigint` | 否 | `NULL` | MUL | — |
| `lai_yuan_lei_xing` | `varchar(32)` | 否 | `NULL` | — | — |
| `lai_yuan_ming_cheng` | `varchar(300)` | 否 | `NULL` | — | — |
| `quan_li_zhuang_tai` | `varchar(32)` | 否 | `NULL` | — | — |
| `chuang_jian_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED | — |
| `geng_xin_shi_jian` | `datetime(3)` | 否 | `CURRENT_TIMESTAMP(3)` | DEFAULT_GENERATED on update CURRENT_TIMESTAMP(3) | — |
| `yi_shan_chu` | `tinyint(1)` | 否 | `0` | — | — |

约束：`CHECK:ck_zhuan_ti_unit_deleted`；`CHECK:ck_zhuan_ti_unit_difficulty`；`CHECK:ck_zhuan_ti_unit_order`；`CHECK:ck_zhuan_ti_unit_rights`；`CHECK:ck_zhuan_ti_unit_source`；`CHECK:ck_zhuan_ti_unit_status`；`FOREIGN KEY:fk_zhuan_ti_unit_creator`；`FOREIGN KEY:fk_zhuan_ti_unit_point`；`FOREIGN KEY:fk_zhuan_ti_unit_subject`；`PRIMARY KEY:PRIMARY`。
索引：`INDEX:fk_zhuan_ti_unit_creator(chuang_jian_ren_id)`；`INDEX:idx_zhuan_ti_unit_point(zhu_zhi_shi_dian_id,zhuang_tai)`；`INDEX:idx_zhuan_ti_unit_subject_status(ke_mu_id,zhuang_tai,yi_shan_chu,pai_xu)`；`UNIQUE/PRIMARY:PRIMARY(id)`。
生命周期：由分层专题学习单元对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。

### `zhuan_ti_xue_xi_dan_yuan_ti_mu`

用途：专题单元与题目关系。创建/演进：V26。

| 字段 | SQL 类型 | 可空 | 默认值 | 键/附加 | 说明 |
|---|---|---:|---|---|---|
| `dan_yuan_id` | `bigint` | 否 | `NULL` | PRI | — |
| `ti_mu_id` | `bigint` | 否 | `NULL` | PRI | — |
| `xue_xi_jie_duan` | `varchar(16)` | 否 | `NULL` | — | — |
| `pai_xu` | `int` | 否 | `NULL` | — | — |

约束：`CHECK:ck_zhuan_ti_unit_item_order`；`CHECK:ck_zhuan_ti_unit_stage`；`FOREIGN KEY:fk_zhuan_ti_unit_item_question`；`FOREIGN KEY:fk_zhuan_ti_unit_item_unit`；`PRIMARY KEY:PRIMARY`；`UNIQUE:uk_zhuan_ti_unit_order`；`UNIQUE:uk_zhuan_ti_unit_stage`。
索引：`INDEX:idx_zhuan_ti_unit_question(ti_mu_id,dan_yuan_id)`；`UNIQUE/PRIMARY:PRIMARY(dan_yuan_id,ti_mu_id)`；`UNIQUE/PRIMARY:uk_zhuan_ti_unit_order(dan_yuan_id,pai_xu)`；`UNIQUE/PRIMARY:uk_zhuan_ti_unit_stage(dan_yuan_id,xue_xi_jie_duan)`。
生命周期：由专题单元与题目关系对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。
