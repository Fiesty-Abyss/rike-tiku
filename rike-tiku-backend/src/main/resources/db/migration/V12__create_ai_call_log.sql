CREATE TABLE ai_diao_yong_ri_zhi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_dai_ma VARCHAR(64) NOT NULL,
    model_dai_ma VARCHAR(128) NOT NULL,
    yong_tu VARCHAR(96) NOT NULL,
    ye_wu_guan_lian VARCHAR(128) NULL,
    shi_fou_cheng_gong TINYINT(1) NOT NULL,
    hao_shi_hao_miao BIGINT NOT NULL,
    shu_ru_token INT NULL,
    shu_chu_token INT NULL,
    cuo_wu_dai_ma VARCHAR(64) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ai_diao_yong_created (chuang_jian_shi_jian, id),
    KEY idx_ai_diao_yong_provider_success (provider_dai_ma, shi_fou_cheng_gong, chuang_jian_shi_jian),
    CONSTRAINT ck_ai_diao_yong_success CHECK (shi_fou_cheng_gong IN (0, 1)),
    CONSTRAINT ck_ai_diao_yong_latency CHECK (hao_shi_hao_miao >= 0),
    CONSTRAINT ck_ai_diao_yong_tokens CHECK (
        (shu_ru_token IS NULL OR shu_ru_token >= 0) AND
        (shu_chu_token IS NULL OR shu_chu_token >= 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏AI模型调用结果日志';
