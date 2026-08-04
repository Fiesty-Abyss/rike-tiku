package com.neu.riketiku.renzheng;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RenZhengShuJuCangKu {
    private final JdbcTemplate jdbcTemplate;

    public RenZhengShuJuCangKu(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<YongHuRenZhengShuJu> anYongHuMingChaZhao(String username) {
        return jdbcTemplate.query("""
                SELECT id,yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,
                       shi_fou_shou_ci_deng_lu,zui_hou_deng_lu_shi_jian
                FROM yong_hu
                WHERE yong_hu_ming=? AND yi_shan_chu=0
                """, (resultSet, rowNumber) -> user(resultSet), username)
                .stream().findFirst();
    }

    public Optional<YongHuRenZhengShuJu> anIdChaZhao(long userId) {
        return jdbcTemplate.query("""
                SELECT id,yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,
                       shi_fou_shou_ci_deng_lu,zui_hou_deng_lu_shi_jian
                FROM yong_hu
                WHERE id=? AND yi_shan_chu=0
                """, (resultSet, rowNumber) -> user(resultSet), userId)
                .stream().findFirst();
    }

    public List<String> chaZhaoYouXiaoJiaoSe(long userId) {
        return jdbcTemplate.queryForList("""
                SELECT js.jiao_se_dai_ma
                FROM yong_hu_jiao_se yhjs
                JOIN jiao_se js ON js.id=yhjs.jiao_se_id
                WHERE yhjs.yong_hu_id=?
                  AND yhjs.zhuang_tai='ACTIVE'
                  AND js.zhuang_tai='ACTIVE'
                  AND js.yi_shan_chu=0
                ORDER BY js.jiao_se_dai_ma
                """, String.class, userId);
    }

    public int gengXinZuiHouDengLuShiJian(long userId, LocalDateTime time) {
        return jdbcTemplate.update("""
                UPDATE yong_hu
                SET zui_hou_deng_lu_shi_jian=?,geng_xin_shi_jian=?
                WHERE id=? AND yi_shan_chu=0
                """, time, time, userId);
    }

    public int gengXinChuShiMiMa(long userId, String passwordHash, LocalDateTime time) {
        return jdbcTemplate.update("""
                UPDATE yong_hu
                SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0,
                    mi_ma_xiu_gai_shi_jian=?,geng_xin_shi_jian=?
                WHERE id=? AND yi_shan_chu=0
                """, passwordHash, time, time, userId);
    }

    public DangAnXianShiShuJu chaZhaoDangAn(long userId) {
        Optional<DangAnXianShiShuJu> student = jdbcTemplate.query("""
                SELECT xing_ming,xue_hao
                FROM xue_sheng_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, (resultSet, rowNumber) -> new DangAnXianShiShuJu(
                        resultSet.getString("xing_ming"), resultSet.getString("xue_hao"), null), userId)
                .stream().findFirst();
        if (student.isPresent()) {
            return student.get();
        }
        return jdbcTemplate.query("""
                SELECT xing_ming,gong_hao
                FROM jiao_shi_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, (resultSet, rowNumber) -> new DangAnXianShiShuJu(
                        resultSet.getString("xing_ming"), null, resultSet.getString("gong_hao")), userId)
                .stream().findFirst()
                .orElse(new DangAnXianShiShuJu(null, null, null));
    }

    private YongHuRenZhengShuJu user(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new YongHuRenZhengShuJu(
                resultSet.getLong("id"),
                resultSet.getString("yong_hu_ming"),
                resultSet.getString("mi_ma_zhai_yao"),
                resultSet.getString("zhang_hao_zhuang_tai"),
                resultSet.getBoolean("shi_fou_shou_ci_deng_lu"),
                resultSet.getObject("zui_hou_deng_lu_shi_jian", LocalDateTime.class));
    }
}
