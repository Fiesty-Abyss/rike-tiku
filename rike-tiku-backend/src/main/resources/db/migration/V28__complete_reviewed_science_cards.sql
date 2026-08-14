ALTER TABLE gao_pin_kao_dian DROP CHECK ck_gao_pin_kao_dian_type;
ALTER TABLE gao_pin_kao_dian DROP CHECK ck_gao_pin_kao_dian_status;

ALTER TABLE gao_pin_kao_dian
    ADD COLUMN latex_nei_rong LONGTEXT NULL AFTER ke_xue_nei_rong,
    ADD COLUMN shi_yong_tiao_jian TEXT NULL AFTER latex_nei_rong,
    ADD COLUMN han_yi_tui_dao TEXT NULL AFTER shi_yong_tiao_jian,
    ADD COLUMN li_zi TEXT NULL AFTER han_yi_tui_dao,
    ADD COLUMN lai_yuan_ming_cheng VARCHAR(255) NULL AFTER chang_jian_wu_qu,
    ADD COLUMN lai_yuan_di_zhi VARCHAR(1000) NULL AFTER lai_yuan_ming_cheng,
    ADD COLUMN quan_li_zhuang_tai VARCHAR(32) NOT NULL DEFAULT 'PROJECT_AUTHORED' AFTER lai_yuan_di_zhi,
    ADD COLUMN chuang_jian_ren_yong_hu_id BIGINT NULL AFTER quan_li_zhuang_tai,
    ADD CONSTRAINT fk_gao_pin_kao_dian_creator FOREIGN KEY(chuang_jian_ren_yong_hu_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_gao_pin_kao_dian_type CHECK(zi_liao_lei_xing IN ('POINT','FORMULA','CHEMICAL_EQUATION','SECONDARY_CONCLUSION','INSTRUMENT','MNEMONIC','TABLE','NOTE')),
    ADD CONSTRAINT ck_gao_pin_kao_dian_rights CHECK(quan_li_zhuang_tai IN ('PROJECT_AUTHORED','PUBLIC_DOMAIN','AUTHORIZED','USER_PROVIDED'));

UPDATE gao_pin_kao_dian h
JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id
SET h.zhuang_tai=CASE WHEN h.zhuang_tai='ACTIVE' THEN 'PUBLISHED' ELSE h.zhuang_tai END,
    h.chuang_jian_ren_yong_hu_id=j.yong_hu_id,
    h.lai_yuan_ming_cheng=COALESCE(h.lai_yuan_ming_cheng,'教师课程总结'),
    h.quan_li_zhuang_tai='USER_PROVIDED';

ALTER TABLE gao_pin_kao_dian
    ADD CONSTRAINT ck_gao_pin_kao_dian_status CHECK(zhuang_tai IN ('PENDING','PUBLISHED','DISABLED'));

CREATE TABLE gao_pin_kao_dian_shen_he_ji_lu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gao_pin_kao_dian_id BIGINT NOT NULL,
    shen_he_dong_zuo VARCHAR(16) NOT NULL,
    yuan_zhuang_tai VARCHAR(16) NOT NULL,
    mu_biao_zhuang_tai VARCHAR(16) NOT NULL,
    shen_he_ren_yong_hu_id BIGINT NOT NULL,
    shen_he_yi_jian VARCHAR(1000) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY(id),
    KEY idx_gao_pin_kao_dian_review (gao_pin_kao_dian_id,chuang_jian_shi_jian),
    CONSTRAINT fk_gao_pin_kao_dian_review_card FOREIGN KEY(gao_pin_kao_dian_id) REFERENCES gao_pin_kao_dian(id) ON DELETE RESTRICT,
    CONSTRAINT fk_gao_pin_kao_dian_review_user FOREIGN KEY(shen_he_ren_yong_hu_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_gao_pin_kao_dian_review_action CHECK(shen_he_dong_zuo IN ('SUBMIT','APPROVE','REJECT','DISABLE')),
    CONSTRAINT ck_gao_pin_kao_dian_review_states CHECK(yuan_zhuang_tai IN ('PENDING','PUBLISHED','DISABLED') AND mu_biao_zhuang_tai IN ('PENDING','PUBLISHED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识卡片人工审核事实';

CREATE TABLE xue_sheng_zhi_shi_ka_pian_zhuang_tai (
    id BIGINT NOT NULL AUTO_INCREMENT,
    xue_sheng_id BIGINT NOT NULL,
    gao_pin_kao_dian_id BIGINT NOT NULL,
    shi_fou_shou_cang TINYINT(1) NOT NULL DEFAULT 0,
    zhang_wo_zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'LEARNING',
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY(id),
    UNIQUE KEY uk_xue_sheng_ka_pian_state (xue_sheng_id,gao_pin_kao_dian_id),
    KEY idx_xue_sheng_ka_pian_filter (xue_sheng_id,shi_fou_shou_cang,zhang_wo_zhuang_tai),
    CONSTRAINT fk_xue_sheng_ka_pian_state_student FOREIGN KEY(xue_sheng_id) REFERENCES xue_sheng_dang_an(id) ON DELETE RESTRICT,
    CONSTRAINT fk_xue_sheng_ka_pian_state_card FOREIGN KEY(gao_pin_kao_dian_id) REFERENCES gao_pin_kao_dian(id) ON DELETE RESTRICT,
    CONSTRAINT ck_xue_sheng_ka_pian_favorite CHECK(shi_fou_shou_cang IN (0,1)),
    CONSTRAINT ck_xue_sheng_ka_pian_mastery CHECK(zhang_wo_zhuang_tai IN ('LEARNING','MASTERED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生知识卡片收藏与掌握状态';

ALTER TABLE ai_hui_hua DROP CHECK ck_ai_hui_hua_context;
ALTER TABLE ai_hui_hua
    ADD COLUMN zhi_shi_ka_pian_id BIGINT NULL AFTER zhuan_ti_ti_mu_id,
    ADD KEY idx_ai_hui_hua_card (xue_sheng_id,zhi_shi_ka_pian_id,geng_xin_shi_jian),
    ADD CONSTRAINT fk_ai_hui_hua_knowledge_card FOREIGN KEY(zhi_shi_ka_pian_id) REFERENCES gao_pin_kao_dian(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_ai_hui_hua_context CHECK(
      (shang_xia_wen_lei_xing='PRACTICE_RESULT' AND xue_sheng_da_ti_id IS NOT NULL AND lian_xi_ti_mu_id IS NOT NULL AND zhuan_ti_ti_mu_id IS NULL AND zhi_shi_ka_pian_id IS NULL) OR
      (shang_xia_wen_lei_xing='TOPIC_QUESTION' AND xue_sheng_da_ti_id IS NULL AND lian_xi_ti_mu_id IS NULL AND zhuan_ti_ti_mu_id IS NOT NULL AND zhi_shi_ka_pian_id IS NULL) OR
      (shang_xia_wen_lei_xing='KNOWLEDGE_CARD' AND xue_sheng_da_ti_id IS NULL AND lian_xi_ti_mu_id IS NULL AND zhuan_ti_ti_mu_id IS NULL AND zhi_shi_ka_pian_id IS NOT NULL)
    );
