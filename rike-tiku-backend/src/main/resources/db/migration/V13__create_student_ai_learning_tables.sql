CREATE TABLE ai_cuo_ti_fen_xi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    xue_sheng_da_ti_id BIGINT NOT NULL,
    xue_sheng_id BIGINT NOT NULL,
    cuo_wu_lei_xing VARCHAR(32) NULL,
    cuo_wu_yuan_yin VARCHAR(1200) NULL,
    zheng_que_si_lu VARCHAR(1600) NULL,
    chang_jian_cuo_wu JSON NULL,
    fu_xi_jian_yi JSON NULL,
    provider_dai_ma VARCHAR(64) NULL,
    model_dai_ma VARCHAR(128) NULL,
    prompt_ban_ben VARCHAR(32) NOT NULL,
    shu_ru_shi_shi_ha_xi CHAR(64) NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL,
    cuo_wu_dai_ma VARCHAR(64) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_cuo_ti_fen_xi_da_ti (xue_sheng_da_ti_id),
    KEY idx_ai_cuo_ti_fen_xi_xue_sheng_status (xue_sheng_id, zhuang_tai, geng_xin_shi_jian),
    CONSTRAINT fk_ai_cuo_ti_fen_xi_da_ti FOREIGN KEY (xue_sheng_da_ti_id) REFERENCES xue_sheng_da_ti (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_cuo_ti_fen_xi_xue_sheng FOREIGN KEY (xue_sheng_id) REFERENCES xue_sheng_dang_an (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_cuo_ti_fen_xi_status CHECK (zhuang_tai IN ('GENERATING', 'SUCCESS', 'FAILED')),
    CONSTRAINT ck_ai_cuo_ti_fen_xi_type CHECK (cuo_wu_lei_xing IS NULL OR cuo_wu_lei_xing IN (
        'CONCEPT_ERROR','CALCULATION_ERROR','READING_ERROR','REASONING_ERROR','MEMORY_ERROR','CARELESS_ERROR','ANSWER_FORMAT_ERROR','UNKNOWN'
    )),
    CONSTRAINT ck_ai_cuo_ti_fen_xi_arrays CHECK (
        (chang_jian_cuo_wu IS NULL OR JSON_TYPE(chang_jian_cuo_wu) = 'ARRAY') AND
        (fu_xi_jian_yi IS NULL OR JSON_TYPE(fu_xi_jian_yi) = 'ARRAY')
    ),
    CONSTRAINT ck_ai_cuo_ti_fen_xi_success CHECK (
        zhuang_tai <> 'SUCCESS' OR (
            cuo_wu_lei_xing IS NOT NULL AND cuo_wu_yuan_yin IS NOT NULL AND zheng_que_si_lu IS NOT NULL AND
            chang_jian_cuo_wu IS NOT NULL AND fu_xi_jian_yi IS NOT NULL AND provider_dai_ma IS NOT NULL AND
            model_dai_ma IS NOT NULL AND cuo_wu_dai_ma IS NULL
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定正式答题事实的学生AI错因分析';

CREATE TABLE ai_hui_hua (
    id BIGINT NOT NULL AUTO_INCREMENT,
    xue_sheng_id BIGINT NOT NULL,
    xue_sheng_da_ti_id BIGINT NOT NULL,
    lian_xi_ti_mu_id BIGINT NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    lei_ji_lun_shu INT NOT NULL DEFAULT 0,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ai_hui_hua_xue_sheng_question (xue_sheng_id, lian_xi_ti_mu_id, geng_xin_shi_jian),
    CONSTRAINT fk_ai_hui_hua_xue_sheng FOREIGN KEY (xue_sheng_id) REFERENCES xue_sheng_dang_an (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_hui_hua_da_ti FOREIGN KEY (xue_sheng_da_ti_id) REFERENCES xue_sheng_da_ti (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_hui_hua_lian_xi_ti_mu FOREIGN KEY (lian_xi_ti_mu_id) REFERENCES lian_xi_ti_mu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_hui_hua_status CHECK (zhuang_tai IN ('ACTIVE', 'LIMIT_REACHED')),
    CONSTRAINT ck_ai_hui_hua_rounds CHECK (lei_ji_lun_shu BETWEEN 0 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定当前题和正式答题事实的学生AI有限会话';

CREATE TABLE ai_xiao_xi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ai_hui_hua_id BIGINT NOT NULL,
    fa_yan_jiao_se VARCHAR(16) NOT NULL,
    nei_rong VARCHAR(2000) NOT NULL,
    xu_hao INT NOT NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_xiao_xi_hui_hua_xu_hao (ai_hui_hua_id, xu_hao),
    KEY idx_ai_xiao_xi_hui_hua_created (ai_hui_hua_id, chuang_jian_shi_jian, id),
    CONSTRAINT fk_ai_xiao_xi_hui_hua FOREIGN KEY (ai_hui_hua_id) REFERENCES ai_hui_hua (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ai_xiao_xi_role CHECK (fa_yan_jiao_se IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_ai_xiao_xi_order CHECK (xu_hao >= 1),
    CONSTRAINT ck_ai_xiao_xi_content CHECK (CHAR_LENGTH(TRIM(nei_rong)) BETWEEN 1 AND 2000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生当前题AI会话消息';
