ALTER TABLE ti_mu
    ADD COLUMN zhuan_ti_lei_xing VARCHAR(32) NULL COMMENT '仅 SUBJECTIVE + TOPIC_LEARNING 使用的受控专题类型' AFTER shi_yong_mo_shi,
    ADD COLUMN ke_jian_fan_wei VARCHAR(32) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/TEACHING_SCOPE_PRIVATE' AFTER zhuan_ti_lei_xing,
    ADD COLUMN ren_ke_guan_xi_id BIGINT NULL COMMENT '私有题所属任课关系' AFTER ke_jian_fan_wei,
    ADD COLUMN chuang_jian_ren_id BIGINT NULL COMMENT '题目创建用户；历史全局题允许为空' AFTER ren_ke_guan_xi_id,
    ADD KEY idx_ti_mu_visibility (ke_jian_fan_wei, ren_ke_guan_xi_id, ke_mu_id, zhuang_tai, yi_shan_chu),
    ADD KEY idx_ti_mu_creator_scope (chuang_jian_ren_id, ren_ke_guan_xi_id, zhuang_tai),
    ADD CONSTRAINT fk_ti_mu_teaching_scope FOREIGN KEY (ren_ke_guan_xi_id) REFERENCES ren_ke_guan_xi(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_ti_mu_creator FOREIGN KEY (chuang_jian_ren_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_ti_mu_visibility CHECK (
        (ke_jian_fan_wei='GLOBAL' AND ren_ke_guan_xi_id IS NULL) OR
        (ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE' AND ren_ke_guan_xi_id IS NOT NULL AND chuang_jian_ren_id IS NOT NULL)
    );

UPDATE ti_mu
SET zhuan_ti_lei_xing='COMPREHENSIVE'
WHERE ti_mu_lei_xing='SUBJECTIVE' AND shi_yong_mo_shi='TOPIC_LEARNING' AND zhuan_ti_lei_xing IS NULL;

ALTER TABLE ti_mu ADD CONSTRAINT ck_ti_mu_topic_category CHECK (
    (ti_mu_lei_xing='SUBJECTIVE' AND shi_yong_mo_shi='TOPIC_LEARNING' AND zhuan_ti_lei_xing IN ('CALCULATION','EXPERIMENT','PROCESS','MATERIAL_ANALYSIS','COMPREHENSIVE')) OR
    (NOT (ti_mu_lei_xing='SUBJECTIVE' AND shi_yong_mo_shi='TOPIC_LEARNING') AND zhuan_ti_lei_xing IS NULL)
);
