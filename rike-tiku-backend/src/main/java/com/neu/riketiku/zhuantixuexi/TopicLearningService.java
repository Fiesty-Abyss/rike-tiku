package com.neu.riketiku.zhuantixuexi;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicLearningService {
    private static final String PREFIX = "【专题演示】";
    private final JdbcTemplate jdbc;

    public TopicLearningService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<TopicLearningDtos.TopicItem> list(Long userId, String subjectCode) {
        requireStudent(userId);
        String normalized = subjectCode == null || subjectCode.isBlank() ? null : subjectCode.trim().toUpperCase();
        return jdbc.query("""
                SELECT q.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_gan,q.nan_du
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                  AND (? IS NULL OR s.ke_mu_dai_ma=?)
                ORDER BY s.pai_xu,q.id
                """, (rs, row) -> new TopicLearningDtos.TopicItem(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), splitStem(rs.getString(5))[0], rs.getInt(6), knowledgePoints(rs.getLong(1))),
                normalized, normalized);
    }

    @Transactional(readOnly = true)
    public TopicLearningDtos.TopicDetail detail(Long userId, Long questionId) {
        requireStudent(userId);
        return jdbc.query("""
                SELECT q.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_gan,q.nan_du,a.jie_xi_nei_rong
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD'
                  AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0
                WHERE q.id=? AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                """, (rs, row) -> {
                    String[] content = splitStem(rs.getString(5));
                    return new TopicLearningDtos.TopicDetail(rs.getLong(1), rs.getLong(2), rs.getString(3),
                            rs.getString(4), content[0], content[1], rs.getInt(6), rs.getString(7), knowledgePoints(rs.getLong(1)));
                }, questionId).stream().findFirst().orElseThrow(() -> new RenZhengYeWuYiChang(
                "TOPIC_LEARNING_NOT_FOUND", "专题题不存在或不可访问", HttpStatus.NOT_FOUND));
    }

    private List<TopicLearningDtos.KnowledgePoint> knowledgePoints(long questionId) {
        return jdbc.query("""
                SELECT k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing
                FROM ti_mu_zhi_shi_dian qk JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id
                WHERE qk.ti_mu_id=? AND qk.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                ORDER BY qk.pai_xu,qk.id
                """, (rs, row) -> new TopicLearningDtos.KnowledgePoint(rs.getLong(1), rs.getString(2), rs.getString(3)), questionId);
    }

    private void requireStudent(Long userId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM xue_sheng_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, Long.class, userId);
        if (count == null || count != 1) {
            throw new RenZhengYeWuYiChang("STUDENT_PROFILE_UNAVAILABLE", "当前账号没有有效学生档案", HttpStatus.FORBIDDEN);
        }
    }

    private String[] splitStem(String stem) {
        String value = stem != null && stem.startsWith(PREFIX) ? stem.substring(PREFIX.length()) : stem;
        int separator = value == null ? -1 : value.indexOf('｜');
        if (separator < 0) return new String[] { "综合题", value == null ? "" : value };
        return new String[] { value.substring(0, separator), value.substring(separator + 1) };
    }
}
