ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_role;
ALTER TABLE ai_sheng_cheng_ren_wu
    ADD CONSTRAINT ck_ai_sheng_cheng_role
    CHECK (chuang_jian_ren_jiao_se IN ('ADMIN','TEACHER','STUDENT'));

ALTER TABLE lian_xi_ti_mu DROP CHECK ck_lian_xi_ti_mu_nan_du;
ALTER TABLE lian_xi_ti_mu
    ADD CONSTRAINT ck_lian_xi_ti_mu_nan_du CHECK (nan_du_kuai_zhao BETWEEN 1 AND 5);
