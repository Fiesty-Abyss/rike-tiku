CREATE TABLE shi_juan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chuang_jian_jiao_shi_id BIGINT NOT NULL,
    ke_mu_id BIGINT NOT NULL,
    shi_juan_ming_cheng VARCHAR(120) NOT NULL,
    zu_juan_mo_shi VARCHAR(16) NOT NULL,
    zong_fen DECIMAL(8,2) NOT NULL DEFAULT 0,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    yi_shan_chu TINYINT(1) NOT NULL DEFAULT 0,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY(id),
    KEY idx_shi_juan_teacher (chuang_jian_jiao_shi_id,yi_shan_chu,geng_xin_shi_jian),
    CONSTRAINT fk_shi_juan_teacher FOREIGN KEY (chuang_jian_jiao_shi_id) REFERENCES jiao_shi_dang_an(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shi_juan_subject FOREIGN KEY (ke_mu_id) REFERENCES ke_mu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_shi_juan_mode CHECK (zu_juan_mo_shi IN ('MANUAL','RULE')),
    CONSTRAINT ck_shi_juan_status CHECK (zhuang_tai IN ('DRAFT','READY')),
    CONSTRAINT ck_shi_juan_score CHECK (zong_fen>=0),
    CONSTRAINT ck_shi_juan_deleted CHECK (yi_shan_chu IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师冻结试卷';

CREATE TABLE shi_juan_ti_mu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    shi_juan_id BIGINT NOT NULL,
    ti_mu_id BIGINT NOT NULL,
    ti_mu_shun_xu INT NOT NULL,
    fen_zhi DECIMAL(8,2) NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY uk_shi_juan_question (shi_juan_id,ti_mu_id),
    UNIQUE KEY uk_shi_juan_order (shi_juan_id,ti_mu_shun_xu),
    CONSTRAINT fk_shi_juan_ti_mu_paper FOREIGN KEY (shi_juan_id) REFERENCES shi_juan(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shi_juan_ti_mu_question FOREIGN KEY (ti_mu_id) REFERENCES ti_mu(id) ON DELETE RESTRICT,
    CONSTRAINT ck_shi_juan_ti_mu_order CHECK (ti_mu_shun_xu>=1),
    CONSTRAINT ck_shi_juan_ti_mu_score CHECK (fen_zhi>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='试卷冻结题目和分值';
