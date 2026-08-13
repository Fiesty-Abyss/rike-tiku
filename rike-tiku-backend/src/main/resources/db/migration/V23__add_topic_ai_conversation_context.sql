ALTER TABLE ai_hui_hua
    MODIFY COLUMN xue_sheng_da_ti_id BIGINT NULL,
    MODIFY COLUMN lian_xi_ti_mu_id BIGINT NULL,
    ADD COLUMN shang_xia_wen_lei_xing VARCHAR(24) NOT NULL DEFAULT 'PRACTICE_RESULT' AFTER lian_xi_ti_mu_id,
    ADD COLUMN zhuan_ti_ti_mu_id BIGINT NULL AFTER shang_xia_wen_lei_xing,
    ADD KEY idx_ai_hui_hua_topic (xue_sheng_id,zhuan_ti_ti_mu_id,geng_xin_shi_jian),
    ADD CONSTRAINT fk_ai_hui_hua_topic_question FOREIGN KEY (zhuan_ti_ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_ai_hui_hua_context CHECK (
        (shang_xia_wen_lei_xing='PRACTICE_RESULT' AND xue_sheng_da_ti_id IS NOT NULL AND lian_xi_ti_mu_id IS NOT NULL AND zhuan_ti_ti_mu_id IS NULL) OR
        (shang_xia_wen_lei_xing='TOPIC_QUESTION' AND xue_sheng_da_ti_id IS NULL AND lian_xi_ti_mu_id IS NULL AND zhuan_ti_ti_mu_id IS NOT NULL)
    );
