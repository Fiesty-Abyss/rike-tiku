CREATE TABLE si_xin_hui_hua (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ren_ke_guan_xi_id BIGINT NOT NULL COMMENT '会话对应的三元任课关系',
    xue_sheng_id BIGINT NOT NULL COMMENT '会话学生档案',
    zhuang_tai VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    zui_hou_xiao_xi_shi_jian DATETIME(3) NULL,
    chuang_jian_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    geng_xin_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_si_xin_hui_hua_scope_student (ren_ke_guan_xi_id, xue_sheng_id),
    KEY idx_si_xin_hui_hua_student_recent (xue_sheng_id, yi_shan_chu, zui_hou_xiao_xi_shi_jian),
    CONSTRAINT fk_si_xin_hui_hua_scope FOREIGN KEY (ren_ke_guan_xi_id) REFERENCES ren_ke_guan_xi (id) ON DELETE RESTRICT,
    CONSTRAINT fk_si_xin_hui_hua_student FOREIGN KEY (xue_sheng_id) REFERENCES xue_sheng_dang_an (id) ON DELETE RESTRICT,
    CONSTRAINT ck_si_xin_hui_hua_status CHECK (zhuang_tai IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_si_xin_hui_hua_deleted CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受三元任课关系约束的师生私信会话';

CREATE TABLE si_xin_xiao_xi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hui_hua_id BIGINT NOT NULL,
    fa_song_ren_yong_hu_id BIGINT NOT NULL,
    nei_rong VARCHAR(1000) NOT NULL,
    yi_du TINYINT NOT NULL DEFAULT 0,
    fa_song_shi_jian DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    yi_du_shi_jian DATETIME(3) NULL,
    yi_shan_chu TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_si_xin_xiao_xi_conversation_time (hui_hua_id, fa_song_shi_jian, id),
    KEY idx_si_xin_xiao_xi_unread (hui_hua_id, yi_du, fa_song_ren_yong_hu_id, yi_shan_chu),
    CONSTRAINT fk_si_xin_xiao_xi_conversation FOREIGN KEY (hui_hua_id) REFERENCES si_xin_hui_hua (id) ON DELETE RESTRICT,
    CONSTRAINT fk_si_xin_xiao_xi_sender FOREIGN KEY (fa_song_ren_yong_hu_id) REFERENCES yong_hu (id) ON DELETE RESTRICT,
    CONSTRAINT ck_si_xin_xiao_xi_content CHECK (CHAR_LENGTH(TRIM(nei_rong)) BETWEEN 1 AND 1000),
    CONSTRAINT ck_si_xin_xiao_xi_read CHECK (yi_du IN (0, 1)),
    CONSTRAINT ck_si_xin_xiao_xi_read_time CHECK ((yi_du = 0 AND yi_du_shi_jian IS NULL) OR (yi_du = 1 AND yi_du_shi_jian IS NOT NULL)),
    CONSTRAINT ck_si_xin_xiao_xi_deleted CHECK (yi_shan_chu IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='师生私信文本消息';
