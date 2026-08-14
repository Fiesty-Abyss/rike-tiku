# `rike_tiku` 核心ER图

以下图用于阅读和论文说明；外键、索引、检查约束的最终事实以Flyway迁移和真实结构快照为准。

```mermaid
erDiagram
    YONG_HU ||--o{ YONG_HU_JIAO_SE : "拥有"
    JIAO_SE ||--o{ YONG_HU_JIAO_SE : "授予"
    YONG_HU ||--o| XUE_SHENG_DANG_AN : "学生档案"
    YONG_HU ||--o| JIAO_SHI_DANG_AN : "教师档案"
    BAN_JI ||--o{ BAN_JI_XUE_SHENG : "包含历史"
    XUE_SHENG_DANG_AN ||--o{ BAN_JI_XUE_SHENG : "加入"
    JIAO_SHI_DANG_AN ||--o{ REN_KE_GUAN_XI : "任课"
    BAN_JI ||--o{ REN_KE_GUAN_XI : "开设"
    KE_MU ||--o{ REN_KE_GUAN_XI : "科目"
    YONG_HU o|--o{ TI_MU_SHEN_HE_JI_LU : "审核"

    KE_MU ||--o{ ZHI_SHI_DIAN : "拥有"
    KE_MU ||--o{ TI_MU : "归属"
    DAO_RU_PI_CI o|--o{ TI_MU : "导入"
    TI_MU o|--o{ TI_MU : "父子题"
    TI_MU ||--o{ TI_MU_XUAN_XIANG : "选项"
    TI_MU ||--o{ TI_MU_JIE_XI : "解析版本"
    TI_MU ||--o{ TI_MU_ZHI_SHI_DIAN : "标注"
    ZHI_SHI_DIAN ||--o{ TI_MU_ZHI_SHI_DIAN : "关联"
    TI_MU ||--o{ TI_MU_FU_JIAN : "附件"
    TI_MU_JIE_XI o|--o{ TI_MU_FU_JIAN : "解析附件"
    TI_MU_XUAN_XIANG o|--o{ TI_MU_FU_JIAN : "选项附件"
    TI_MU ||--o{ TI_MU_LAI_YUAN : "分项来源"
    TI_MU ||--o{ TI_MU_SHEN_HE_JI_LU : "审核轨迹"
    TI_MU ||--o{ ZHUAN_TI_XUE_XI_DAN_YUAN_TI_MU : "专题引用"
    ZHUAN_TI_XUE_XI_DAN_YUAN ||--o{ ZHUAN_TI_XUE_XI_DAN_YUAN_TI_MU : "组织2至3题"
    SHI_JUAN ||--o{ SHI_JUAN_FA_BU : "发布"
    REN_KE_GUAN_XI ||--o{ SHI_JUAN_FA_BU : "授权班级科目"
    SHI_JUAN_FA_BU ||--o{ SHI_JUAN_FA_BU_TI_MU : "冻结题目"
    SHI_JUAN_FA_BU ||--o{ SHI_JUAN_TI_JIAO : "学生提交"
    SHI_JUAN_TI_JIAO ||--o{ SHI_JUAN_XUE_SHENG_DA_TI : "逐题事实"
    GAO_PIN_KAO_DIAN ||--o{ GAO_PIN_KAO_DIAN_SHEN_HE_JI_LU : "卡片审核"
    GAO_PIN_KAO_DIAN ||--o{ XUE_SHENG_ZHI_SHI_KA_PIAN_ZHUANG_TAI : "收藏掌握"
    GAO_PIN_KAO_DIAN ||--o{ ZHI_SHI_KA_PIAN_LIAN_XI_SHI_LI : "生成练习"
```

## V29 结构口径

当前结构为 Flyway V1–V29、50 张业务表。V25 保存候选题新颖度审计元数据；V26 增加专题单元并扩展显式 GLM/xAI 视觉配置；V27 增加试卷发布快照、学生提交和逐题事实；V28 完成知识卡片审核与学生状态；V29 增加知识卡片生成练习实例。纯结构细节见 [`schema_snapshot_v29.sql`](../schema_snapshot_v29.sql)。

## 权限数据路径

```mermaid
flowchart LR
    U["yong_hu 用户"] --> UR["yong_hu_jiao_se"] --> R["jiao_se 角色"]
    U --> TP["jiao_shi_dang_an 教师档案"]
    TP --> REL["ren_ke_guan_xi 三元关系"]
    REL --> C["ban_ji 班级"]
    REL --> S["ke_mu 科目"]
```

教师对班级和科目的数据范围必须落到一条明确的 `ren_ke_guan_xi`，不能通过姓名、职务或两个独立关系的笛卡尔组合推断。
