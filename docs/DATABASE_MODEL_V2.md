# 数据库模型 V2：题库、账号与教学组织

PR #27 当前分支在 V1–V10 之后新增 V11 `guan_li_cao_zuo_ri_zhi` 管理员高风险操作日志表，当前结构为 V1–V11、27 张业务表。本文前面的 V1–V10 说明保留为历史快照；V1–V10 不修改。

更新时间：2026-08-08
设计基线：V3.0  
数据库：MySQL 8.4 / `rike_tiku`  
迁移版本：Flyway V1–V10

## 1. 范围和事实来源

本版在题库核心 V1 模型上增加账号、角色、学生/教师档案、班级、班级学生历史、教师—班级—科目三元任课关系、高频考点、师生私信及用户简介/头像字段。V10 后仍为 26 张业务表。

Flyway 迁移是数据库结构的唯一事实来源。本文和 `database/schema/rike_tiku_schema.sql` 是阅读快照，不得替代迁移，也不得反向手工修改已经执行的 V1–V10。V10 只 ALTER `yong_hu`，V1–V9 保持不变。

高频考点表只绑定真实 `ren_ke_guan_xi_id` 和同科 `zhi_shi_dian_id`；教师与学生 API 的数据权限仍由当前用户和三元任课关系推导。

PR #21 不增加数据库结构。知识点掌握度是由 `lian_xi_hui_hua`、`lian_xi_ti_mu.zhi_shi_dian_kuai_zhao`、`xue_sheng_da_ti`、`xue_xi_jie_guo` 和 `cuo_ti_ji_lu` 实时派生的查询结果，不维护冗余统计表；因此 Flyway 仍为 V1–V9、26 张业务表。

PR #22 的 `V10__add_user_profile_fields.sql` 只向 `yong_hu` 增加 500 字简介、头像 MIME、MEDIUMBLOB 原始图片和头像更新时间。没有新增表，业务表仍为 26 张；头像不保存文件名或 Base64 字符串。

## 2. 总体关系

- `yong_hu` 与 `jiao_se` 通过 `yong_hu_jiao_se` 构成多对多，同一账号可拥有多个角色。
- 一个 `yong_hu` 最多对应一份 `xue_sheng_dang_an`，也最多对应一份 `jiao_shi_dang_an`。同一账号在被授权时可以同时拥有不同类型档案。
- `ban_ji_xue_sheng` 保存学生加入、退出和主班级历史；同一学生同一时刻最多一个有效主班级。
- `ren_ke_guan_xi` 直接表达 `jiao_shi_id + ban_ji_id + ke_mu_id`，避免拆成教师—班级和教师—科目后产生组合歧义。
- `ti_mu_shen_he_ji_lu.shen_he_ren_id` 可空并外键关联 `yong_hu`。系统提交或历史导入可以没有人工审核人；已有人工审核历史由逻辑删除和 `ON DELETE RESTRICT` 保留。

## 3. 字段字典

### 3.1 `yong_hu` 用户账号

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 用户主键 |
| `yong_hu_ming` | VARCHAR(64) | 非空，唯一 | 全局唯一登录名 |
| `mi_ma_zhai_yao` | VARCHAR(255) | 非空 | BCrypt等安全摘要；长度至少50，禁止保存明文 |
| `zhang_hao_zhuang_tai` | VARCHAR(16) | `ENABLED` | `ENABLED` / `DISABLED` / `LOCKED` |
| `shi_fou_shou_ci_deng_lu` | TINYINT | 1 | 首次登录必须改初始密码 |
| `mi_ma_xiu_gai_shi_jian` | DATETIME(3) | 可空 | 最近改密时间 |
| `zui_hou_deng_lu_shi_jian` | DATETIME(3) | 可空 | 最近登录时间 |
| `ge_ren_jian_jie` | VARCHAR(500) | 可空 | 本人可维护的个人简介 |
| `tou_xiang_mime` | VARCHAR(64) | 可空 | 已验证头像 MIME，仅 PNG/JPEG |
| `tou_xiang` | MEDIUMBLOB | 可空 | 最大 2 MB 的头像原始二进制 |
| `tou_xiang_geng_xin_shi_jian` | DATETIME(3) | 可空 | 最近头像更新时间 |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | MyBatis-Plus逻辑删除 |

关键约束：`UK(yong_hu_ming)`；状态和布尔检查；`IDX(zhang_hao_zhuang_tai, yi_shan_chu)`。

### 3.2 `jiao_se` 系统角色

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 角色主键 |
| `jiao_se_dai_ma` | VARCHAR(32) | 非空，唯一 | `STUDENT` / `TEACHER` / `ADMIN` |
| `jiao_se_ming_cheng` | VARCHAR(64) | 非空 | 中文显示名称 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | 逻辑删除 |

V5只初始化三种基础角色，不初始化用户或统一初始密码。

### 3.3 `yong_hu_jiao_se` 用户角色关联

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 关系主键 |
| `yong_hu_id` | BIGINT | 非空，FK | 用户 |
| `jiao_se_id` | BIGINT | 非空，FK | 角色 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |

关键约束：`UK(yong_hu_id, jiao_se_id)`；两个外键均为 `ON DELETE RESTRICT`。

### 3.4 `xue_sheng_dang_an` 学生档案

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 学生档案主键，班级关系引用此ID |
| `yong_hu_id` | BIGINT | 非空，唯一，FK | 一个用户最多一份学生档案 |
| `xue_hao` | VARCHAR(64) | 非空，唯一 | 全局唯一学号 |
| `xing_ming` | VARCHAR(64) | 非空 | 授权范围内的显示姓名，不参与权限判断 |
| `nian_ji` | VARCHAR(32) | 非空 | 当前年级显示值 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | 逻辑删除 |

不保存身份证、家庭住址、手机号等当前业务不需要的信息。

### 3.5 `jiao_shi_dang_an` 教师档案

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 教师档案主键，任课关系引用此ID |
| `yong_hu_id` | BIGINT | 非空，唯一，FK | 一个用户最多一份教师档案 |
| `gong_hao` | VARCHAR(64) | 非空，唯一 | 全局唯一工号 |
| `xing_ming` | VARCHAR(64) | 非空 | 显示姓名，不参与权限判断 |
| `xian_shi_zhi_wu` | VARCHAR(128) | 可空 | 现实职务，仅展示 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | 逻辑删除 |

### 3.6 `ban_ji` 班级

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 班级主键 |
| `ban_ji_bian_ma` | VARCHAR(64) | 非空，唯一 | 全局唯一班级编码 |
| `ban_ji_ming_cheng` | VARCHAR(128) | 非空 | 班级显示名称 |
| `nian_ji` | VARCHAR(32) | 非空 | 年级 |
| `ru_xue_nian_fen` | SMALLINT | 非空 | 入学年份，2000–2100 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `GRADUATED` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | 逻辑删除 |

### 3.7 `ban_ji_xue_sheng` 班级学生历史

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 关系主键 |
| `ban_ji_id` | BIGINT | 非空，FK | 班级 |
| `xue_sheng_id` | BIGINT | 非空，FK | 学生档案 |
| `shi_fou_zhu_ban_ji` | TINYINT | 0 | 是否主班级 |
| `jia_ru_shi_jian` | DATETIME(3) | 当前时间 | 加入时间 |
| `tui_chu_shi_jian` | DATETIME(3) | 可空 | 退出时间 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `EXITED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `you_xiao_guan_xi_biao_shi` | TINYINT | 生成列 | 有效关系为1，历史关系为NULL |
| `you_xiao_zhu_ban_ji_xue_sheng_id` | BIGINT | 生成列 | 有效主班级时生成学生ID |

V3.0原建议 `UK(ban_ji_id, xue_sheng_id)` 无法同时允许“退出后保留历史、未来可重新加入”。本轮没有静默采用该建议，而是用两个生成列唯一索引：

- `UK(ban_ji_id, xue_sheng_id, you_xiao_guan_xi_biao_shi)`：拒绝同一学生和班级的重复有效关系，允许多条已退出历史。
- `UK(you_xiao_zhu_ban_ji_xue_sheng_id)`：数据库层保证一个学生不能同时有两个有效主班级。

这是对当前任务历史保留约束的必要细化，不改变业务含义。

### 3.8 `ren_ke_guan_xi` 三元任课关系

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 任课关系主键 |
| `jiao_shi_id` | BIGINT | 非空，FK | 教师档案 |
| `ban_ji_id` | BIGINT | 非空，FK | 班级 |
| `ke_mu_id` | BIGINT | 非空，FK | 科目 |
| `shi_fou_zhu_ren_ke` | TINYINT | 0 | 是否主任课教师 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `ENDED` / `DISABLED` |
| `kai_shi_shi_jian` | DATETIME(3) | 非空 | 任课开始时间 |
| `jie_shu_shi_jian` | DATETIME(3) | 可空 | 任课结束时间 |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |

关键约束：`UK(jiao_shi_id, ban_ji_id, ke_mu_id)`；`IDX(ban_ji_id, ke_mu_id, zhuang_tai)`。教师现实职务、姓名或前端入口均不能替代这条关系授予数据权限。

### 3.9 `gao_pin_kao_dian` 高频考点（V8）

| 字段 | 类型 | 空值/默认 | 说明 |
|---|---|---|---|
| `id` | BIGINT | PK，自增 | 高频考点主键 |
| `ren_ke_guan_xi_id` | BIGINT | 非空，FK | 真实三元任课关系，不重复保存教师、班级、科目 |
| `zhi_shi_dian_id` | BIGINT | 非空，FK | 必须属于该任课关系的科目 |
| `biao_ti` | VARCHAR(200) | 非空 | 标题 |
| `nei_rong` | TEXT | 非空 | 纯文本正文 |
| `ji_yi_kou_jue` | VARCHAR(500) | 可空 | 记忆口诀 |
| `chang_jian_wu_qu` | TEXT | 可空 | 常见误区 |
| `pai_xu` | INT | 0 | 普通整数排序值 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` | `ACTIVE` / `DISABLED` |
| `chuang_jian_shi_jian` | DATETIME(3) | 当前时间 | 创建审计 |
| `geng_xin_shi_jian` | DATETIME(3) | 自动更新 | 更新审计 |
| `yi_shan_chu` | TINYINT | 0 | 逻辑删除 |

V8 表使用外键和状态检查，不引入附件、富文本、AI 或审核流。教师只能维护自己的 ACTIVE 任课关系；学生端由有效学生档案、ACTIVE 主班级和所选科目反推任课关系，只返回 ACTIVE 内容。

### 3.10 `si_xin_hui_hua` 私信会话（V9）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `ren_ke_guan_xi_id` | BIGINT | 绑定真实三元任课关系的外键 |
| `xue_sheng_id` | BIGINT | 学生档案外键 |
| `zhuang_tai` | VARCHAR(16) | `ACTIVE` / `DISABLED` |
| `zui_hou_xiao_xi_shi_jian` | DATETIME(3) | 最近消息时间，可空 |
| `chuang_jian_shi_jian` / `geng_xin_shi_jian` | DATETIME(3) | 审计时间 |
| `yi_shan_chu` | TINYINT | 逻辑删除 |

有效会话唯一约束为 `ren_ke_guan_xi_id + xue_sheng_id`。任课关系停用或学生调班不删除会话，只停止新发送。

### 3.11 `si_xin_xiao_xi` 私信消息（V9）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `hui_hua_id` | BIGINT | 会话外键 |
| `fa_song_ren_yong_hu_id` | BIGINT | 发送人用户外键，由 JWT 决定 |
| `nei_rong` | VARCHAR(1000) | 纯文本消息 |
| `yi_du` / `yi_du_shi_jian` | TINYINT / DATETIME(3) | 已读状态和时间 |
| `fa_song_shi_jian` | DATETIME(3) | 发送时间 |
| `yi_shan_chu` | TINYINT | 逻辑删除 |

V9 不引入图片、文件、群聊、撤回或管理员审计。两表外键均使用 `ON DELETE RESTRICT`，历史数据不会因教学关系失效而物理删除。

## 4. 删除和历史策略

- 核心主体使用逻辑删除或状态停用，不级联物理删除历史。
- 所有本轮外键使用 `ON DELETE RESTRICT`。
- 题目审核人允许为空，适配系统提交和旧数据；填写后必须引用真实用户。
- 审核人逻辑删除不会破坏审核记录；若尝试物理删除被引用用户，数据库拒绝。

## 5. 密码和账号治理

- 数据库只保存密码摘要，`mi_ma_zhai_yao` 不保存明文。
- 本轮没有创建任何真实用户，也没有统一初始密码。
- 未来导入时每个账号生成独立随机初始密码，首次登录强制修改。
- 角色由后端根据账号关系读取，前端入口不能授予角色。

## 6. MyBatis-Plus最小映射

本轮只为数据库测试创建 `YongHu`、`JiaoSe`、`BanJi`、`BanJiXueSheng`、`RenKeGuanXi` 及对应Mapper。学生/教师档案等表通过明确SQL验证，未提前创建空DTO、Service或CRUD接口。

`ShenJiZiDuanTianChongChuLiQi` 验证创建/更新时间自动填充；`YongHu` 的 `@TableLogic` 验证逻辑删除。

## 7. 已验证约束

自动化测试覆盖：V5/V6、空库V1–V6、原题库回归、用户名/角色码/学号/工号/班级码唯一、多角色、重复关联拒绝、每用户单份档案、单一有效主班级、班级历史、任课三元唯一、不同班级同科和同班不同科、无关系不授权、审核人外键、自动填充、逻辑删除和事务回滚。
