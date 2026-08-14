ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_mode;
ALTER TABLE ai_sheng_cheng_ren_wu ADD CONSTRAINT ck_ai_sheng_cheng_mode CHECK (bian_shi_fang_shi IN ('SCENARIO_TRANSFER','CONDITION_RECOMBINATION','REPRESENTATION_SWITCH','MULTI_STEP_EXTENSION','DISTRACTOR_REDESIGN','COMBINED'));

ALTER TABLE ai_hou_xuan_ti_zhi_liang_ping_jia
    ADD COLUMN bian_shi_fang_shi VARCHAR(48) NULL AFTER bian_shi_zhai_yao,
    ADD COLUMN bian_hua_wei_du JSON NULL AFTER bian_shi_fang_shi,
    ADD COLUMN xin_ying_du_fen_shu DECIMAL(6,5) NULL AFTER bian_hua_wei_du,
    ADD COLUMN xiang_si_du_fen_shu DECIMAL(6,5) NULL AFTER xin_ying_du_fen_shu,
    ADD COLUMN ju_jue_yuan_yin VARCHAR(96) NULL AFTER xiang_si_du_fen_shu,
    ADD CONSTRAINT ck_ai_candidate_novelty_score CHECK (xin_ying_du_fen_shu IS NULL OR xin_ying_du_fen_shu BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_ai_candidate_similarity_score CHECK (xiang_si_du_fen_shu IS NULL OR xiang_si_du_fen_shu BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_ai_candidate_variation_mode CHECK (bian_shi_fang_shi IS NULL OR bian_shi_fang_shi IN ('SCENARIO_TRANSFER','CONDITION_RECOMBINATION','REPRESENTATION_SWITCH','MULTI_STEP_EXTENSION','DISTRACTOR_REDESIGN','COMBINED'));
