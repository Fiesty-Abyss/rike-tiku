ALTER TABLE si_xin_xiao_xi
    ADD COLUMN che_hui_shi_jian DATETIME(3) NULL AFTER yi_du_shi_jian,
    ADD COLUMN fa_song_zhe_yi_cang TINYINT(1) NOT NULL DEFAULT 0 AFTER che_hui_shi_jian,
    ADD COLUMN jie_shou_zhe_yi_cang TINYINT(1) NOT NULL DEFAULT 0 AFTER fa_song_zhe_yi_cang,
    ADD KEY idx_si_xin_xiao_xi_sender_visibility (hui_hua_id,fa_song_zhe_yi_cang,fa_song_shi_jian,id),
    ADD KEY idx_si_xin_xiao_xi_receiver_visibility (hui_hua_id,jie_shou_zhe_yi_cang,fa_song_shi_jian,id),
    ADD CONSTRAINT ck_si_xin_xiao_xi_sender_hidden CHECK (fa_song_zhe_yi_cang IN (0,1)),
    ADD CONSTRAINT ck_si_xin_xiao_xi_receiver_hidden CHECK (jie_shou_zhe_yi_cang IN (0,1));
