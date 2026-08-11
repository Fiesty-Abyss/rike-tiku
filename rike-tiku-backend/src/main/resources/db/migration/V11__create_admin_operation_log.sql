CREATE TABLE guan_li_cao_zuo_ri_zhi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cao_zuo_ren_yong_hu_id BIGINT NULL,
    mo_kuai VARCHAR(64) NOT NULL,
    cao_zuo_lei_xing VARCHAR(96) NOT NULL,
    ye_wu_dui_xiang_id BIGINT NULL,
    cao_zuo_jie_guo VARCHAR(16) NOT NULL,
    zhai_yao VARCHAR(1000) NULL,
    cuo_wu_dai_ma VARCHAR(96) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_guan_li_cao_zuo_ri_zhi_time (chuang_jian_shi_jian, id),
    KEY idx_guan_li_cao_zuo_ri_zhi_filter (mo_kuai, cao_zuo_lei_xing, cao_zuo_jie_guo),
    KEY idx_guan_li_cao_zuo_ri_zhi_operator (cao_zuo_ren_yong_hu_id, chuang_jian_shi_jian),
    CONSTRAINT fk_guan_li_cao_zuo_ri_zhi_operator
        FOREIGN KEY (cao_zuo_ren_yong_hu_id) REFERENCES yong_hu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_guan_li_cao_zuo_ri_zhi_result
        CHECK (cao_zuo_jie_guo IN ('SUCCESS', 'FAILURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员高风险操作日志';
