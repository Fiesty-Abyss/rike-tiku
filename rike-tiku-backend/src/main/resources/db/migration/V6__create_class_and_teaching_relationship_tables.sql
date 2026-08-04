CREATE TABLE ban_ji (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ban_ji_bian_ma VARCHAR(64) NOT NULL,
    ban_ji_ming_cheng VARCHAR(128) NOT NULL,
    nian_ji VARCHAR(32) NOT NULL,
    ru_xue_nian_fen SMALLINT NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ban_ji_bian_ma (ban_ji_bian_ma),
    KEY idx_ban_ji_nian_ji_zhuang_tai (nian_ji, zhuang_tai, yi_shan_chu),
    CONSTRAINT ck_ban_ji_ru_xue_nian_fen CHECK (ru_xue_nian_fen BETWEEN 2000 AND 2100),
    CONSTRAINT ck_ban_ji_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'GRADUATED', 'DISABLED')),
    CONSTRAINT ck_ban_ji_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级';

CREATE TABLE ban_ji_xue_sheng (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ban_ji_id BIGINT NOT NULL,
    xue_sheng_id BIGINT NOT NULL,
    shi_fou_zhu_ban_ji TINYINT NOT NULL DEFAULT 0,
    jia_ru_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    tui_chu_shi_jian DATETIME(3) NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    you_xiao_guan_xi_biao_shi TINYINT
        GENERATED ALWAYS AS (
            CASE WHEN zhuang_tai = 'ACTIVE' AND tui_chu_shi_jian IS NULL THEN 1 ELSE NULL END
        ) STORED,
    you_xiao_zhu_ban_ji_xue_sheng_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN shi_fou_zhu_ban_ji = 1 AND zhuang_tai = 'ACTIVE' AND tui_chu_shi_jian IS NULL
                THEN xue_sheng_id ELSE NULL
            END
        ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ban_ji_xue_sheng_active (ban_ji_id, xue_sheng_id, you_xiao_guan_xi_biao_shi),
    UNIQUE KEY uk_xue_sheng_active_main_class (you_xiao_zhu_ban_ji_xue_sheng_id),
    KEY idx_ban_ji_xue_sheng_student (xue_sheng_id, zhuang_tai),
    KEY idx_ban_ji_xue_sheng_class (ban_ji_id, zhuang_tai),
    CONSTRAINT fk_ban_ji_xue_sheng_ban_ji FOREIGN KEY (ban_ji_id) REFERENCES ban_ji (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ban_ji_xue_sheng_xue_sheng FOREIGN KEY (xue_sheng_id) REFERENCES xue_sheng_dang_an (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ban_ji_xue_sheng_zhu_ban_ji CHECK (shi_fou_zhu_ban_ji IN (0, 1)),
    CONSTRAINT ck_ban_ji_xue_sheng_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'EXITED')),
    CONSTRAINT ck_ban_ji_xue_sheng_tui_chu
        CHECK (
            (zhuang_tai = 'ACTIVE' AND tui_chu_shi_jian IS NULL)
            OR (zhuang_tai = 'EXITED' AND tui_chu_shi_jian IS NOT NULL)
        ),
    CONSTRAINT ck_ban_ji_xue_sheng_shi_jian
        CHECK (tui_chu_shi_jian IS NULL OR tui_chu_shi_jian >= jia_ru_shi_jian)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级学生关系及历史';

CREATE TABLE ren_ke_guan_xi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    jiao_shi_id BIGINT NOT NULL,
    ban_ji_id BIGINT NOT NULL,
    ke_mu_id BIGINT NOT NULL,
    shi_fou_zhu_ren_ke TINYINT NOT NULL DEFAULT 0,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    kai_shi_shi_jian DATETIME(3) NOT NULL,
    jie_shu_shi_jian DATETIME(3) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ren_ke_jiao_shi_ban_ji_ke_mu (jiao_shi_id, ban_ji_id, ke_mu_id),
    KEY idx_ren_ke_ban_ji_ke_mu_zhuang_tai (ban_ji_id, ke_mu_id, zhuang_tai),
    KEY idx_ren_ke_jiao_shi_zhuang_tai (jiao_shi_id, zhuang_tai),
    CONSTRAINT fk_ren_ke_guan_xi_jiao_shi FOREIGN KEY (jiao_shi_id) REFERENCES jiao_shi_dang_an (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ren_ke_guan_xi_ban_ji FOREIGN KEY (ban_ji_id) REFERENCES ban_ji (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ren_ke_guan_xi_ke_mu FOREIGN KEY (ke_mu_id) REFERENCES ke_mu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ren_ke_zhu_ren_ke CHECK (shi_fou_zhu_ren_ke IN (0, 1)),
    CONSTRAINT ck_ren_ke_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'ENDED', 'DISABLED')),
    CONSTRAINT ck_ren_ke_shi_jian CHECK (jie_shu_shi_jian IS NULL OR jie_shu_shi_jian >= kai_shi_shi_jian)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师班级科目三元任课关系';
