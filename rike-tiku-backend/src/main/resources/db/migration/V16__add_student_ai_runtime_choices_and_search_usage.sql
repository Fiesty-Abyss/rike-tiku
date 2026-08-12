ALTER TABLE ai_mo_xing_pei_zhi DROP CHECK ck_ai_mo_xing_usage;
ALTER TABLE ai_mo_xing_pei_zhi
    ADD CONSTRAINT ck_ai_mo_xing_usage CHECK (yong_tu IN ('TEXT','VISION','SEARCH'));

ALTER TABLE ai_hui_hua
    ADD COLUMN ai_mo_xing_pei_zhi_id BIGINT NULL AFTER lian_xi_ti_mu_id,
    ADD COLUMN si_kao_mo_shi VARCHAR(16) NOT NULL DEFAULT 'STANDARD' AFTER ai_mo_xing_pei_zhi_id,
    ADD COLUMN shi_fou_lian_wang TINYINT(1) NOT NULL DEFAULT 0 AFTER si_kao_mo_shi,
    ADD CONSTRAINT fk_ai_hui_hua_model FOREIGN KEY (ai_mo_xing_pei_zhi_id)
        REFERENCES ai_mo_xing_pei_zhi(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_ai_hui_hua_thinking CHECK (si_kao_mo_shi IN ('STANDARD','DEEP')),
    ADD CONSTRAINT ck_ai_hui_hua_search CHECK (shi_fou_lian_wang IN (0,1));

ALTER TABLE ai_xiao_xi
    ADD COLUMN lian_wang_lai_yuan JSON NULL AFTER nei_rong,
    ADD CONSTRAINT ck_ai_xiao_xi_sources CHECK (
        lian_wang_lai_yuan IS NULL OR JSON_TYPE(lian_wang_lai_yuan) = 'ARRAY'
    );
