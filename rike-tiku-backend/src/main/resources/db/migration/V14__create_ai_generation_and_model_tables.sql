CREATE TABLE ai_mo_xing_pei_zhi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_dai_ma VARCHAR(32) NOT NULL COMMENT 'DEEPSEEK/GLM',
    mo_xing_dai_ma VARCHAR(128) NOT NULL,
    api_di_zhi VARCHAR(500) NOT NULL,
    api_mi_yao VARCHAR(1000) NULL COMMENT '仅本地毕设演示模式保存，API与日志禁止回显',
    yong_tu VARCHAR(16) NOT NULL COMMENT 'TEXT/VISION',
    shi_fou_qi_yong TINYINT(1) NOT NULL DEFAULT 0,
    shi_fou_mo_ren TINYINT(1) NOT NULL DEFAULT 0,
    chao_shi_hao_miao INT NOT NULL DEFAULT 30000,
    zui_da_token INT NOT NULL DEFAULT 1200,
    retry_count TINYINT NOT NULL DEFAULT 1,
    zui_jin_ce_shi_zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'NOT_TESTED',
    zui_jin_ce_shi_hao_shi BIGINT NULL,
    zui_jin_ce_shi_shi_jian DATETIME(3) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_mo_xing_provider_model_usage (provider_dai_ma,mo_xing_dai_ma,yong_tu),
    KEY idx_ai_mo_xing_default (yong_tu,shi_fou_qi_yong,shi_fou_mo_ren),
    CONSTRAINT ck_ai_mo_xing_provider CHECK (provider_dai_ma IN ('DEEPSEEK','GLM')),
    CONSTRAINT ck_ai_mo_xing_usage CHECK (yong_tu IN ('TEXT','VISION')),
    CONSTRAINT ck_ai_mo_xing_flags CHECK (shi_fou_qi_yong IN (0,1) AND shi_fou_mo_ren IN (0,1)),
    CONSTRAINT ck_ai_mo_xing_timeout CHECK (chao_shi_hao_miao BETWEEN 1000 AND 120000),
    CONSTRAINT ck_ai_mo_xing_tokens CHECK (zui_da_token BETWEEN 64 AND 8192),
    CONSTRAINT ck_ai_mo_xing_retry CHECK (retry_count BETWEEN 0 AND 1),
    CONSTRAINT ck_ai_mo_xing_test_status CHECK (zui_jin_ce_shi_zhuang_tai IN ('NOT_TESTED','SUCCESS','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地毕设演示AI模型配置';

CREATE TABLE ai_sheng_cheng_ren_wu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mu_ti_mu_id BIGINT NOT NULL,
    chuang_jian_ren_id BIGINT NOT NULL,
    chuang_jian_ren_jiao_se VARCHAR(16) NOT NULL,
    mu_biao_ti_xing VARCHAR(32) NOT NULL,
    zhi_shi_dian_ids JSON NOT NULL,
    mu_biao_nan_du TINYINT NOT NULL,
    bian_shi_fang_shi VARCHAR(32) NOT NULL,
    sheng_cheng_shu_liang TINYINT NOT NULL,
    qing_qiu_ha_xi CHAR(64) NOT NULL,
    provider_dai_ma VARCHAR(32) NULL,
    model_dai_ma VARCHAR(128) NULL,
    prompt_ban_ben VARCHAR(32) NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL,
    yi_sheng_cheng_shu_liang TINYINT NOT NULL DEFAULT 0,
    shi_fou_shi_yong_shi_jue TINYINT(1) NOT NULL DEFAULT 0,
    shi_bai_dai_ma VARCHAR(64) NULL,
    hao_shi_hao_miao BIGINT NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    wan_cheng_shi_jian DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_sheng_cheng_request_hash (qing_qiu_ha_xi),
    KEY idx_ai_sheng_cheng_mother_status (mu_ti_mu_id,zhuang_tai,chuang_jian_shi_jian),
    CONSTRAINT fk_ai_sheng_cheng_mother FOREIGN KEY (mu_ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_sheng_cheng_creator FOREIGN KEY (chuang_jian_ren_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_sheng_cheng_role CHECK (chuang_jian_ren_jiao_se IN ('ADMIN','TEACHER')),
    CONSTRAINT ck_ai_sheng_cheng_type CHECK (mu_biao_ti_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')),
    CONSTRAINT ck_ai_sheng_cheng_difficulty CHECK (mu_biao_nan_du BETWEEN 1 AND 5),
    CONSTRAINT ck_ai_sheng_cheng_mode CHECK (bian_shi_fang_shi IN ('NUMERIC_CONDITION','SCENARIO','KNOWLEDGE_ANGLE','DISTRACTOR','COMBINED')),
    CONSTRAINT ck_ai_sheng_cheng_count CHECK (sheng_cheng_shu_liang BETWEEN 1 AND 3 AND yi_sheng_cheng_shu_liang BETWEEN 0 AND 3),
    CONSTRAINT ck_ai_sheng_cheng_status CHECK (zhuang_tai IN ('GENERATING','SUCCESS','FAILED')),
    CONSTRAINT ck_ai_sheng_cheng_vision CHECK (shi_fou_shi_yong_shi_jue IN (0,1)),
    CONSTRAINT ck_ai_sheng_cheng_points CHECK (JSON_TYPE(zhi_shi_dian_ids)='ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI候选变式题生成任务';

CREATE TABLE ai_hou_xuan_ti_zhi_liang_ping_jia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ai_sheng_cheng_ren_wu_id BIGINT NOT NULL,
    ti_mu_id BIGINT NOT NULL,
    bian_shi_zhai_yao VARCHAR(1000) NOT NULL,
    chong_fu_ti_shi VARCHAR(32) NOT NULL DEFAULT 'NONE',
    shi_fou_shi_yong_shi_jue TINYINT(1) NOT NULL DEFAULT 0,
    xue_ke_zheng_que_xing TINYINT NULL,
    da_an_zheng_que_xing TINYINT NULL,
    ke_jie_xing TINYINT NULL,
    zhi_shi_yi_zhi_xing TINYINT NULL,
    nan_du_pi_pei TINYINT NULL,
    shen_he_jie_guo VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    shen_he_hao_shi_fen_zhong INT NULL,
    shen_he_ren_id BIGINT NULL,
    shen_he_ping_lun VARCHAR(2000) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_quality_question (ti_mu_id),
    KEY idx_ai_quality_task (ai_sheng_cheng_ren_wu_id),
    CONSTRAINT fk_ai_quality_task FOREIGN KEY (ai_sheng_cheng_ren_wu_id) REFERENCES ai_sheng_cheng_ren_wu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_quality_question FOREIGN KEY (ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_quality_reviewer FOREIGN KEY (shen_he_ren_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_quality_duplicate CHECK (chong_fu_ti_shi IN ('NONE','SUSPECTED_DUPLICATE')),
    CONSTRAINT ck_ai_quality_vision CHECK (shi_fou_shi_yong_shi_jue IN (0,1)),
    CONSTRAINT ck_ai_quality_binary CHECK (
        (xue_ke_zheng_que_xing IS NULL OR xue_ke_zheng_que_xing IN (0,1)) AND
        (da_an_zheng_que_xing IS NULL OR da_an_zheng_que_xing IN (0,1)) AND
        (ke_jie_xing IS NULL OR ke_jie_xing IN (0,1)) AND
        (zhi_shi_yi_zhi_xing IS NULL OR zhi_shi_yi_zhi_xing IN (0,1)) AND
        (nan_du_pi_pei IS NULL OR nan_du_pi_pei IN (0,1))
    ),
    CONSTRAINT ck_ai_quality_result CHECK (shen_he_jie_guo IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_ai_quality_review_minutes CHECK (shen_he_hao_shi_fen_zhong IS NULL OR shen_he_hao_shi_fen_zhong BETWEEN 0 AND 10080)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI候选题人工质量评价';

CREATE TABLE ai_shi_jue_shang_xia_wen (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ti_mu_id BIGINT NOT NULL,
    fu_jian_ji_he_ha_xi CHAR(64) NOT NULL,
    provider_dai_ma VARCHAR(32) NOT NULL,
    model_dai_ma VARCHAR(128) NOT NULL,
    prompt_ban_ben VARCHAR(32) NOT NULL,
    shi_jue_json JSON NULL,
    zhuang_tai VARCHAR(16) NOT NULL,
    cuo_wu_dai_ma VARCHAR(64) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_vision_context (ti_mu_id,fu_jian_ji_he_ha_xi,provider_dai_ma,model_dai_ma,prompt_ban_ben),
    CONSTRAINT fk_ai_vision_question FOREIGN KEY (ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_vision_status CHECK (zhuang_tai IN ('SUCCESS','FAILED')),
    CONSTRAINT ck_ai_vision_json CHECK (shi_jue_json IS NULL OR JSON_TYPE(shi_jue_json)='OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受控且可复用的题目视觉上下文';

ALTER TABLE ti_mu DROP CHECK ck_ti_mu_nan_du;
ALTER TABLE ti_mu ADD CONSTRAINT ck_ti_mu_nan_du CHECK (nan_du BETWEEN 1 AND 5);
