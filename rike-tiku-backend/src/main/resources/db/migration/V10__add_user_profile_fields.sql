ALTER TABLE yong_hu
    ADD COLUMN ge_ren_jian_jie VARCHAR(500) NULL COMMENT '个人简介' AFTER zui_hou_deng_lu_shi_jian,
    ADD COLUMN tou_xiang_mime VARCHAR(64) NULL COMMENT '头像MIME类型' AFTER ge_ren_jian_jie,
    ADD COLUMN tou_xiang MEDIUMBLOB NULL COMMENT '头像原始二进制' AFTER tou_xiang_mime,
    ADD COLUMN tou_xiang_geng_xin_shi_jian DATETIME(3) NULL COMMENT '头像更新时间' AFTER tou_xiang;
