CREATE TABLE ke_mu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    ke_mu_dai_ma VARCHAR(32) NOT NULL COMMENT '科目英文代码',
    ke_mu_ming_cheng VARCHAR(32) NOT NULL COMMENT '科目名称',
    pai_xu INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ke_mu_dai_ma (ke_mu_dai_ma),
    UNIQUE KEY uk_ke_mu_ming_cheng (ke_mu_ming_cheng),
    CONSTRAINT ck_ke_mu_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_ke_mu_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目';

CREATE TABLE zhi_shi_dian (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    ke_mu_id BIGINT NOT NULL COMMENT '所属科目',
    fu_zhi_shi_dian_id BIGINT NULL COMMENT '父知识点',
    zhi_shi_dian_ming_cheng VARCHAR(128) NOT NULL COMMENT '知识点名称',
    wan_zheng_lu_jing VARCHAR(500) NOT NULL COMMENT '从一级到当前节点的完整路径',
    ceng_ji SMALLINT NOT NULL COMMENT '层级，从1开始',
    pai_xu INT NOT NULL DEFAULT 0 COMMENT '同级显示顺序',
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_zhi_shi_dian_lu_jing (ke_mu_id, wan_zheng_lu_jing),
    KEY idx_zhi_shi_dian_fu (fu_zhi_shi_dian_id),
    CONSTRAINT fk_zhi_shi_dian_ke_mu FOREIGN KEY (ke_mu_id) REFERENCES ke_mu (id),
    CONSTRAINT fk_zhi_shi_dian_fu FOREIGN KEY (fu_zhi_shi_dian_id) REFERENCES zhi_shi_dian (id),
    CONSTRAINT ck_zhi_shi_dian_ceng_ji CHECK (ceng_ji >= 1),
    CONSTRAINT ck_zhi_shi_dian_zhuang_tai CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_zhi_shi_dian_yi_shan_chu CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识点树';
