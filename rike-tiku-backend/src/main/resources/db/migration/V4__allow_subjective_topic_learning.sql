ALTER TABLE ti_mu DROP CHECK ck_ti_mu_lei_xing;

ALTER TABLE ti_mu
    ADD CONSTRAINT ck_ti_mu_lei_xing
        CHECK (ti_mu_lei_xing IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'FILL_BLANK', 'SUBJECTIVE')),
    ADD CONSTRAINT ck_ti_mu_zhu_guan_mo_shi
        CHECK (
            ti_mu_lei_xing <> 'SUBJECTIVE'
            OR (shi_yong_mo_shi = 'TOPIC_LEARNING' AND shi_fou_ke_zi_dong_pan_fen = 0)
        );
