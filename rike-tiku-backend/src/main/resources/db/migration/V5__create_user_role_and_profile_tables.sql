CREATE TABLE yong_hu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    yong_hu_ming VARCHAR(64) NOT NULL,
    mi_ma_zhai_yao VARCHAR(255) NOT NULL,
    zhang_hao_zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    shi_fou_shou_ci_deng_lu TINYINT NOT NULL DEFAULT 1,
    mi_ma_xiu_gai_shi_jian DATETIME(3) NULL,
    zui_hou_deng_lu_shi_jian DATETIME(3) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_yong_hu_yong_hu_ming (yong_hu_ming),
    KEY idx_yong_hu_zhuang_tai_shan_chu (zhang_hao_zhuang_tai, yi_shan_chu),
    CONSTRAINT ck_yong_hu_zhang_hao_zhuang_tai
        CHECK (zhang_hao_zhuang_tai IN ('ENABLED', 'DISABLED', 'LOCKED')),
    CONSTRAINT ck_yong_hu_shou_ci_deng_lu
        CHECK (shi_fou_shou_ci_deng_lu IN (0, 1)),
    CONSTRAINT ck_yong_hu_mi_ma_zhai_yao
        CHECK (CHAR_LENGTH(mi_ma_zhai_yao) >= 50),
    CONSTRAINT ck_yong_hu_yi_shan_chu
        CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号';

CREATE TABLE jiao_se (
    id BIGINT NOT NULL AUTO_INCREMENT,
    jiao_se_dai_ma VARCHAR(32) NOT NULL,
    jiao_se_ming_cheng VARCHAR(64) NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jiao_se_dai_ma (jiao_se_dai_ma),
    KEY idx_jiao_se_zhuang_tai_shan_chu (zhuang_tai, yi_shan_chu),
    CONSTRAINT ck_jiao_se_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_jiao_se_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';

CREATE TABLE yong_hu_jiao_se (
    id BIGINT NOT NULL AUTO_INCREMENT,
    yong_hu_id BIGINT NOT NULL,
    jiao_se_id BIGINT NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_yong_hu_jiao_se (yong_hu_id, jiao_se_id),
    KEY idx_yong_hu_jiao_se_jiao_se (jiao_se_id, zhuang_tai),
    CONSTRAINT fk_yong_hu_jiao_se_yong_hu FOREIGN KEY (yong_hu_id) REFERENCES yong_hu (id) ON DELETE RESTRICT,
    CONSTRAINT fk_yong_hu_jiao_se_jiao_se FOREIGN KEY (jiao_se_id) REFERENCES jiao_se (id) ON DELETE RESTRICT,
    CONSTRAINT ck_yong_hu_jiao_se_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

CREATE TABLE xue_sheng_dang_an (
    id BIGINT NOT NULL AUTO_INCREMENT,
    yong_hu_id BIGINT NOT NULL,
    xue_hao VARCHAR(64) NOT NULL,
    xing_ming VARCHAR(64) NOT NULL,
    nian_ji VARCHAR(32) NOT NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_xue_sheng_dang_an_yong_hu (yong_hu_id),
    UNIQUE KEY uk_xue_sheng_dang_an_xue_hao (xue_hao),
    KEY idx_xue_sheng_nian_ji_zhuang_tai (nian_ji, zhuang_tai, yi_shan_chu),
    CONSTRAINT fk_xue_sheng_dang_an_yong_hu FOREIGN KEY (yong_hu_id) REFERENCES yong_hu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_xue_sheng_dang_an_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_xue_sheng_dang_an_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生档案';

CREATE TABLE jiao_shi_dang_an (
    id BIGINT NOT NULL AUTO_INCREMENT,
    yong_hu_id BIGINT NOT NULL,
    gong_hao VARCHAR(64) NOT NULL,
    xing_ming VARCHAR(64) NOT NULL,
    xian_shi_zhi_wu VARCHAR(128) NULL,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jiao_shi_dang_an_yong_hu (yong_hu_id),
    UNIQUE KEY uk_jiao_shi_dang_an_gong_hao (gong_hao),
    KEY idx_jiao_shi_zhuang_tai_shan_chu (zhuang_tai, yi_shan_chu),
    CONSTRAINT fk_jiao_shi_dang_an_yong_hu FOREIGN KEY (yong_hu_id) REFERENCES yong_hu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_jiao_shi_dang_an_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_jiao_shi_dang_an_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师档案';

INSERT INTO jiao_se (jiao_se_dai_ma, jiao_se_ming_cheng)
VALUES ('STUDENT', '学生'), ('TEACHER', '教师'), ('ADMIN', '管理员');

ALTER TABLE ti_mu_shen_he_ji_lu
    ADD KEY idx_ti_mu_shen_he_ren (shen_he_ren_id),
    ADD CONSTRAINT fk_ti_mu_shen_he_ji_lu_shen_he_ren
        FOREIGN KEY (shen_he_ren_id) REFERENCES yong_hu (id) ON DELETE RESTRICT;
