ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_role;
ALTER TABLE ai_sheng_cheng_ren_wu
    ADD CONSTRAINT ck_ai_sheng_cheng_role CHECK (chuang_jian_ren_jiao_se IN ('ADMIN','TEACHER','STUDENT'));

CREATE TABLE ai_xue_sheng_bian_shi_shi_li (
    id BIGINT NOT NULL AUTO_INCREMENT,
    xue_sheng_id BIGINT NOT NULL,
    xue_sheng_da_ti_id BIGINT NOT NULL,
    mu_ti_mu_id BIGINT NOT NULL,
    ai_sheng_cheng_ren_wu_id BIGINT NOT NULL,
    ti_mu_id BIGINT NOT NULL,
    zhuang_tai VARCHAR(24) NOT NULL DEFAULT 'READY',
    xue_sheng_da_an JSON NULL,
    shi_fou_zheng_que TINYINT(1) NULL,
    ti_jiao_shi_jian DATETIME(3) NULL,
    shen_he_ti_jiao_shi_jian DATETIME(3) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY(id),
    UNIQUE KEY uk_ai_xue_sheng_variant_question (xue_sheng_id,ti_mu_id),
    KEY idx_ai_xue_sheng_variant_fact (xue_sheng_da_ti_id,chuang_jian_shi_jian),
    CONSTRAINT fk_ai_xue_sheng_variant_student FOREIGN KEY (xue_sheng_id) REFERENCES xue_sheng_dang_an(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_xue_sheng_variant_fact FOREIGN KEY (xue_sheng_da_ti_id) REFERENCES xue_sheng_da_ti(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_xue_sheng_variant_mother FOREIGN KEY (mu_ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_xue_sheng_variant_task FOREIGN KEY (ai_sheng_cheng_ren_wu_id) REFERENCES ai_sheng_cheng_ren_wu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_xue_sheng_variant_question FOREIGN KEY (ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_xue_sheng_variant_status CHECK (zhuang_tai IN ('READY','ANSWERED','SUBMITTED_FOR_REVIEW','DISCARDED')),
    CONSTRAINT ck_ai_xue_sheng_variant_correct CHECK (shi_fou_zheng_que IS NULL OR shi_fou_zheng_que IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定正式答题事实的学生AI结构化变式实例';
