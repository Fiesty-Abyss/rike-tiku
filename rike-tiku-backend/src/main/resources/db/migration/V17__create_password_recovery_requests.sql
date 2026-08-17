CREATE TABLE mi_ma_chong_zhi_shen_qing (
    id BIGINT NOT NULL AUTO_INCREMENT,
    yong_hu_id BIGINT NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    shen_qing_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    chu_li_ren_id BIGINT NULL,
    chu_li_shi_jian DATETIME(3) NULL,
    chu_li_jie_guo VARCHAR(500) NULL,
    pending_yong_hu_id BIGINT GENERATED ALWAYS AS (CASE WHEN zhuang_tai='PENDING' THEN yong_hu_id ELSE NULL END) STORED,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mi_ma_chong_zhi_pending (pending_yong_hu_id),
    KEY idx_mi_ma_chong_zhi_status_time (zhuang_tai,shen_qing_shi_jian),
    CONSTRAINT fk_mi_ma_chong_zhi_user FOREIGN KEY (yong_hu_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT fk_mi_ma_chong_zhi_handler FOREIGN KEY (chu_li_ren_id) REFERENCES yong_hu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_mi_ma_chong_zhi_status CHECK (zhuang_tai IN ('PENDING','RESOLVED','REJECTED')),
    CONSTRAINT ck_mi_ma_chong_zhi_resolution CHECK (
      (zhuang_tai='PENDING' AND chu_li_ren_id IS NULL AND chu_li_shi_jian IS NULL)
      OR (zhuang_tai<>'PENDING' AND chu_li_ren_id IS NOT NULL AND chu_li_shi_jian IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='匿名密码恢复请求与管理员处理事实';
