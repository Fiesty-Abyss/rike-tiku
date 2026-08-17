ALTER TABLE shi_juan_fa_bu_ti_mu
    ADD COLUMN fu_jian_kuai_zhao JSON NULL
        COMMENT '发布时冻结的题干与STANDARD解析附件数组'
        AFTER zhi_shi_dian_kuai_zhao;

UPDATE shi_juan_fa_bu_ti_mu
SET fu_jian_kuai_zhao = JSON_ARRAY();

ALTER TABLE shi_juan_fa_bu_ti_mu
    MODIFY COLUMN fu_jian_kuai_zhao JSON NOT NULL
        COMMENT '发布时冻结的题干与STANDARD解析附件数组',
    ADD CONSTRAINT ck_shi_juan_fa_bu_ti_mu_attachment_snapshot
        CHECK (JSON_TYPE(fu_jian_kuai_zhao) = 'ARRAY');

ALTER TABLE shi_juan_xue_sheng_da_ti
    MODIFY COLUMN zhuang_tai VARCHAR(32) NOT NULL DEFAULT 'DRAFT';
