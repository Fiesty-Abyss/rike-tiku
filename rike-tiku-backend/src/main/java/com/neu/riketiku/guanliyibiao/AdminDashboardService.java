package com.neu.riketiku.guanliyibiao;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final JdbcTemplate jdbc;

    public AdminDashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDtos.Dashboard dashboard() {
        long activeClasses = count("SELECT COUNT(*) FROM ban_ji WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0");
        long enabledStudents = count("""
                SELECT COUNT(*) FROM xue_sheng_dang_an s JOIN yong_hu u ON u.id=s.yong_hu_id
                WHERE s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0 AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                """);
        long enabledTeachers = count("""
                SELECT COUNT(*) FROM jiao_shi_dang_an t JOIN yong_hu u ON u.id=t.yong_hu_id
                WHERE t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0 AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                """);
        long published = count("SELECT COUNT(*) FROM ti_mu WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0");
        long pending = count("SELECT COUNT(*) FROM ti_mu WHERE zhuang_tai='PENDING' AND yi_shan_chu=0");
        List<Long> subjectCounts = List.of(
                publishedSubjectCount("PHYSICS"),
                publishedSubjectCount("CHEMISTRY"),
                publishedSubjectCount("BIOLOGY"));
        List<AdminDashboardDtos.RecentOperation> recent = jdbc.query("""
                SELECT l.id,u.yong_hu_ming,l.mo_kuai,l.cao_zuo_lei_xing,l.cao_zuo_jie_guo,l.zhai_yao,l.chuang_jian_shi_jian
                FROM guan_li_cao_zuo_ri_zhi l LEFT JOIN yong_hu u ON u.id=l.cao_zuo_ren_yong_hu_id
                ORDER BY l.chuang_jian_shi_jian DESC,l.id DESC LIMIT 5
                """, (rs, row) -> new AdminDashboardDtos.RecentOperation(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getObject(7, java.time.LocalDateTime.class)));
        return new AdminDashboardDtos.Dashboard(activeClasses, enabledStudents, enabledTeachers, published, pending,
                subjectCounts.get(0), subjectCounts.get(1), subjectCounts.get(2), recent);
    }

    private long publishedSubjectCount(String code) {
        return count("""
                SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE s.ke_mu_dai_ma=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                """, code);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
