package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.jiaoshi.dto.JiaoShiRenKeFanWeiXiangYing;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiaoShiGongZuoTaiFuWu {
    private final JdbcTemplate jdbcTemplate;

    public JiaoShiGongZuoTaiFuWu(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<JiaoShiRenKeFanWeiXiangYing> myTeachingScopes(long userId) {
        return jdbcTemplate.query("""
                SELECT r.id,b.id,b.ban_ji_ming_cheng,b.nian_ji,k.id,k.ke_mu_dai_ma,k.ke_mu_ming_cheng,
                       r.shi_fou_zhu_ren_ke,r.zhuang_tai
                FROM jiao_shi_dang_an j
                JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id
                JOIN ban_ji b ON b.id=r.ban_ji_id
                JOIN ke_mu k ON k.id=r.ke_mu_id
                WHERE j.yong_hu_id=? AND j.yi_shan_chu=0
                  AND b.yi_shan_chu=0 AND k.yi_shan_chu=0
                ORDER BY CASE r.zhuang_tai WHEN 'ACTIVE' THEN 0 ELSE 1 END,
                         b.nian_ji,b.ban_ji_ming_cheng,k.pai_xu,k.id
                """, (rs, row) -> new JiaoShiRenKeFanWeiXiangYing(
                rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getLong(5),
                rs.getString(6), rs.getString(7), rs.getBoolean(8), rs.getString(9)), userId);
    }
}
