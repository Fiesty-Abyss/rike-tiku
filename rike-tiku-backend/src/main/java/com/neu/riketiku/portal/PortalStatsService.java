package com.neu.riketiku.portal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalStatsService {
    private final JdbcTemplate jdbc;

    public PortalStatsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PortalStats current() {
        return new PortalStats(
                count("SELECT COUNT(*) FROM ke_mu WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0"),
                count("""
                        SELECT COUNT(*) FROM ti_mu
                        WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0
                          AND ke_jian_fan_wei='GLOBAL'
                          AND shi_yong_mo_shi='ONLINE_PRACTICE'
                          AND shi_fou_ke_zi_dong_pan_fen=1
                          AND ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')
                        """),
                count("""
                        SELECT COUNT(*) FROM ti_mu
                        WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0
                          AND ke_jian_fan_wei='GLOBAL'
                          AND ti_mu_lei_xing='SUBJECTIVE'
                          AND shi_yong_mo_shi='TOPIC_LEARNING'
                          AND shi_fou_ke_zi_dong_pan_fen=0
                        """));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
