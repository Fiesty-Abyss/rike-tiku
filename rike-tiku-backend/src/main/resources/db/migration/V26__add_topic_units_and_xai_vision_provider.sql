CREATE TABLE zhuan_ti_xue_xi_dan_yuan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ke_mu_id BIGINT NOT NULL,
    biao_ti VARCHAR(200) NOT NULL,
    jian_jie VARCHAR(1000) NOT NULL,
    nan_du_ceng_ji TINYINT NOT NULL,
    zhu_zhi_shi_dian_id BIGINT NOT NULL,
    pai_xu INT NOT NULL DEFAULT 1,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    chuang_jian_ren_id BIGINT NOT NULL,
    lai_yuan_lei_xing VARCHAR(32) NOT NULL,
    lai_yuan_ming_cheng VARCHAR(300) NOT NULL,
    quan_li_zhuang_tai VARCHAR(32) NOT NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_zhuan_ti_unit_subject_status (ke_mu_id,zhuang_tai,yi_shan_chu,pai_xu),
    KEY idx_zhuan_ti_unit_point (zhu_zhi_shi_dian_id,zhuang_tai),
    CONSTRAINT fk_zhuan_ti_unit_subject FOREIGN KEY (ke_mu_id) REFERENCES ke_mu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_zhuan_ti_unit_point FOREIGN KEY (zhu_zhi_shi_dian_id) REFERENCES zhi_shi_dian(id) ON DELETE RESTRICT,
    CONSTRAINT fk_zhuan_ti_unit_creator FOREIGN KEY (chuang_jian_ren_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_zhuan_ti_unit_difficulty CHECK (nan_du_ceng_ji BETWEEN 1 AND 5),
    CONSTRAINT ck_zhuan_ti_unit_order CHECK (pai_xu >= 1),
    CONSTRAINT ck_zhuan_ti_unit_status CHECK (zhuang_tai IN ('DRAFT','PUBLISHED','DISABLED')),
    CONSTRAINT ck_zhuan_ti_unit_source CHECK (lai_yuan_lei_xing IN ('PROJECT_AUTHORED','TEACHER_CREATED','AUTHORIZED_IMPORT')),
    CONSTRAINT ck_zhuan_ti_unit_rights CHECK (quan_li_zhuang_tai IN ('PROJECT_AUTHORED','USER_PROVIDED','AUTHORIZED')),
    CONSTRAINT ck_zhuan_ti_unit_deleted CHECK (yi_shan_chu IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专题学习单元，仅编排现有TOPIC_LEARNING题目';

CREATE TABLE zhuan_ti_xue_xi_dan_yuan_ti_mu (
    dan_yuan_id BIGINT NOT NULL,
    ti_mu_id BIGINT NOT NULL,
    xue_xi_jie_duan VARCHAR(16) NOT NULL,
    pai_xu INT NOT NULL,
    PRIMARY KEY (dan_yuan_id,ti_mu_id),
    UNIQUE KEY uk_zhuan_ti_unit_stage (dan_yuan_id,xue_xi_jie_duan),
    UNIQUE KEY uk_zhuan_ti_unit_order (dan_yuan_id,pai_xu),
    KEY idx_zhuan_ti_unit_question (ti_mu_id,dan_yuan_id),
    CONSTRAINT fk_zhuan_ti_unit_item_unit FOREIGN KEY (dan_yuan_id) REFERENCES zhuan_ti_xue_xi_dan_yuan(id) ON DELETE RESTRICT,
    CONSTRAINT fk_zhuan_ti_unit_item_question FOREIGN KEY (ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_zhuan_ti_unit_stage CHECK (xue_xi_jie_duan IN ('FOUNDATION','TRANSFER','ADVANCED')),
    CONSTRAINT ck_zhuan_ti_unit_item_order CHECK (pai_xu BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专题单元题目编排，题目事实仍由ti_mu保存';

ALTER TABLE ai_mo_xing_pei_zhi DROP CHECK ck_ai_mo_xing_provider;
ALTER TABLE ai_mo_xing_pei_zhi
    ADD CONSTRAINT ck_ai_mo_xing_provider CHECK (provider_dai_ma IN ('DEEPSEEK','GLM','XAI'));

ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_type;
ALTER TABLE ai_sheng_cheng_ren_wu
    ADD CONSTRAINT ck_ai_sheng_cheng_type CHECK (mu_biao_ti_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK','SUBJECTIVE'));
