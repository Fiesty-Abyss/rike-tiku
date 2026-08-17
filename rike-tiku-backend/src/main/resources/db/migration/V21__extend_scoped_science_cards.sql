ALTER TABLE gao_pin_kao_dian
    ADD COLUMN zi_liao_lei_xing VARCHAR(32) NOT NULL DEFAULT 'POINT' AFTER zhi_shi_dian_id,
    ADD COLUMN ke_xue_nei_rong LONGTEXT NULL AFTER nei_rong,
    ADD CONSTRAINT ck_gao_pin_kao_dian_type CHECK (zi_liao_lei_xing IN ('POINT','FORMULA','CHEMICAL_EQUATION','MNEMONIC','TABLE','NOTE'));

CREATE TABLE gao_pin_kao_dian_zhi_shi_dian (
    gao_pin_kao_dian_id BIGINT NOT NULL,
    zhi_shi_dian_id BIGINT NOT NULL,
    pai_xu INT NOT NULL DEFAULT 1,
    PRIMARY KEY (gao_pin_kao_dian_id,zhi_shi_dian_id),
    KEY idx_kao_dian_point_reverse (zhi_shi_dian_id,gao_pin_kao_dian_id),
    CONSTRAINT fk_kao_dian_point_card FOREIGN KEY (gao_pin_kao_dian_id) REFERENCES gao_pin_kao_dian(id) ON DELETE RESTRICT,
    CONSTRAINT fk_kao_dian_point_point FOREIGN KEY (zhi_shi_dian_id) REFERENCES zhi_shi_dian(id) ON DELETE RESTRICT,
    CONSTRAINT ck_kao_dian_point_order CHECK (pai_xu>=1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识卡片多知识点关系';

INSERT INTO gao_pin_kao_dian_zhi_shi_dian(gao_pin_kao_dian_id,zhi_shi_dian_id,pai_xu)
SELECT id,zhi_shi_dian_id,1 FROM gao_pin_kao_dian;

CREATE TABLE gao_pin_kao_dian_fu_jian (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gao_pin_kao_dian_id BIGINT NOT NULL,
    yuan_shi_wen_jian_ming VARCHAR(255) NOT NULL,
    xiang_dui_lu_jing VARCHAR(1000) NOT NULL,
    mime_lei_xing VARCHAR(32) NOT NULL,
    nei_rong_ha_xi CHAR(64) NOT NULL,
    wen_jian_da_xiao BIGINT NOT NULL,
    pai_xu INT NOT NULL DEFAULT 1,
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY(id),
    UNIQUE KEY uk_kao_dian_attachment_hash (gao_pin_kao_dian_id,nei_rong_ha_xi),
    KEY idx_kao_dian_attachment (gao_pin_kao_dian_id,zhuang_tai,yi_shan_chu,pai_xu),
    CONSTRAINT fk_kao_dian_attachment_card FOREIGN KEY (gao_pin_kao_dian_id) REFERENCES gao_pin_kao_dian(id) ON DELETE RESTRICT,
    CONSTRAINT ck_kao_dian_attachment_mime CHECK (mime_lei_xing IN ('image/png','image/jpeg')),
    CONSTRAINT ck_kao_dian_attachment_size CHECK (wen_jian_da_xiao BETWEEN 1 AND 3145728),
    CONSTRAINT ck_kao_dian_attachment_status CHECK (zhuang_tai IN ('ACTIVE','DISABLED')),
    CONSTRAINT ck_kao_dian_attachment_deleted CHECK (yi_shan_chu IN (0,1)),
    CONSTRAINT ck_kao_dian_attachment_order CHECK (pai_xu>=1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级知识卡片安全图片附件';
