package com.neu.riketiku.zhuantixuexi;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.QuestionDisplayTextNormalizer;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicLearningService {
    private static final String PREFIX = "【专题演示】";
    private final JdbcTemplate jdbc;
    private final QuestionDisplayTextNormalizer normalizer;

    public TopicLearningService(JdbcTemplate jdbc,QuestionDisplayTextNormalizer normalizer) {
        this.jdbc = jdbc;this.normalizer=normalizer;
    }

    @Transactional(readOnly = true)
    public List<TopicLearningDtos.TopicItem> list(Long userId, String subjectCode) {
        requireStudent(userId);
        String normalized = subjectCode == null || subjectCode.isBlank() ? null : subjectCode.trim().toUpperCase();
        return jdbc.query("""
                SELECT q.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_gan,q.zhuan_ti_lei_xing,q.nan_du
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                  AND (? IS NULL OR s.ke_mu_dai_ma=?)
                  AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (SELECT 1 FROM xue_sheng_dang_an xs JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=xs.id JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id AND r.ban_ji_id=bx.ban_ji_id WHERE xs.yong_hu_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL AND r.zhuang_tai='ACTIVE'))
                ORDER BY s.pai_xu,q.id
                """, (rs, row) -> new TopicLearningDtos.TopicItem(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), splitStem(rs.getString(5))[0],rs.getString(6), rs.getInt(7), knowledgePoints(rs.getLong(1))),
                normalized, normalized,userId);
    }

    @Transactional(readOnly = true)
    public List<TopicLearningDtos.UnitItem> units(Long userId, String subjectCode) {
        requireStudent(userId);
        String normalized = subjectCode == null || subjectCode.isBlank() ? null : subjectCode.trim().toUpperCase();
        return jdbc.query("""
                SELECT u.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,u.biao_ti,u.jian_jie,u.nan_du_ceng_ji,
                       k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing,COUNT(i.ti_mu_id)
                FROM zhuan_ti_xue_xi_dan_yuan u
                JOIN ke_mu s ON s.id=u.ke_mu_id
                JOIN zhi_shi_dian k ON k.id=u.zhu_zhi_shi_dian_id
                JOIN zhuan_ti_xue_xi_dan_yuan_ti_mu i ON i.dan_yuan_id=u.id
                JOIN ti_mu q ON q.id=i.ti_mu_id AND q.ke_mu_id=u.ke_mu_id
                  AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                WHERE u.zhuang_tai='PUBLISHED' AND u.yi_shan_chu=0 AND (? IS NULL OR s.ke_mu_dai_ma=?)
                  AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (
                    SELECT 1 FROM xue_sheng_dang_an xs
                    JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=xs.id AND bx.shi_fou_zhu_ban_ji=1
                      AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                    JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id AND r.ban_ji_id=bx.ban_ji_id AND r.zhuang_tai='ACTIVE'
                    WHERE xs.yong_hu_id=?))
                GROUP BY u.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,u.biao_ti,u.jian_jie,u.nan_du_ceng_ji,
                         k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing
                HAVING COUNT(i.ti_mu_id) BETWEEN 2 AND 3
                ORDER BY s.pai_xu,u.pai_xu,u.id
                """, (rs,row)->new TopicLearningDtos.UnitItem(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getInt(7),new TopicLearningDtos.KnowledgePoint(rs.getLong(8),rs.getString(9),rs.getString(10)),rs.getInt(11)),
                normalized,normalized,userId);
    }

    @Transactional(readOnly = true)
    public TopicLearningDtos.UnitDetail unit(Long userId, Long unitId) {
        TopicLearningDtos.UnitItem header=units(userId,null).stream().filter(item->item.id().equals(unitId)).findFirst()
                .orElseThrow(()->new RenZhengYeWuYiChang("TOPIC_UNIT_NOT_FOUND","专题单元不存在或不可访问",HttpStatus.NOT_FOUND));
        List<TopicLearningDtos.UnitQuestion> questions=jdbc.query("""
                SELECT i.xue_xi_jie_duan,i.pai_xu,q.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_gan,q.zhuan_ti_lei_xing,q.nan_du
                FROM zhuan_ti_xue_xi_dan_yuan_ti_mu i
                JOIN zhuan_ti_xue_xi_dan_yuan u ON u.id=i.dan_yuan_id AND u.zhuang_tai='PUBLISHED' AND u.yi_shan_chu=0
                JOIN ti_mu q ON q.id=i.ti_mu_id AND q.ke_mu_id=u.ke_mu_id
                  AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE i.dan_yuan_id=?
                  AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (
                    SELECT 1 FROM xue_sheng_dang_an xs
                    JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=xs.id AND bx.shi_fou_zhu_ban_ji=1
                      AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                    JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id AND r.ban_ji_id=bx.ban_ji_id
                      AND r.ke_mu_id=q.ke_mu_id AND r.zhuang_tai='ACTIVE'
                    WHERE xs.yong_hu_id=?))
                ORDER BY i.pai_xu
                """,(rs,row)->new TopicLearningDtos.UnitQuestion(rs.getString(1),rs.getInt(2),
                new TopicLearningDtos.TopicItem(rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getString(6),
                        splitStem(rs.getString(7))[0],rs.getString(8),rs.getInt(9),knowledgePoints(rs.getLong(3)))),unitId,userId);
        if(questions.size()<2||questions.size()>3){
            throw new RenZhengYeWuYiChang("TOPIC_UNIT_NOT_FOUND","专题单元不存在或可访问题目不足",HttpStatus.NOT_FOUND);
        }
        return new TopicLearningDtos.UnitDetail(header.id(),header.subjectId(),header.subjectCode(),header.subjectName(),
                header.title(),header.introduction(),header.difficulty(),header.primaryKnowledgePoint(),questions);
    }

    @Transactional(readOnly = true)
    public TopicLearningDtos.TopicDetail detail(Long userId, Long questionId) {
        requireStudent(userId);
        return jdbc.query("""
                SELECT q.id,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_gan,q.zhuan_ti_lei_xing,q.nan_du,a.jie_xi_nei_rong
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD'
                  AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0
                WHERE q.id=? AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                  AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (SELECT 1 FROM xue_sheng_dang_an xs JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=xs.id JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id AND r.ban_ji_id=bx.ban_ji_id WHERE xs.yong_hu_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL AND r.zhuang_tai='ACTIVE'))
                """, (rs, row) -> {
                    String[] content = splitStem(rs.getString(5));
                    return new TopicLearningDtos.TopicDetail(rs.getLong(1), rs.getLong(2), rs.getString(3),
                            rs.getString(4), content[0], content[1],rs.getString(6), rs.getInt(7), rs.getString(8),
                            knowledgePoints(rs.getLong(1)),attachments(rs.getLong(1),"QUESTION"),
                            attachments(rs.getLong(1),"STANDARD_ANALYSIS"));
                }, questionId,userId).stream().findFirst().orElseThrow(() -> new RenZhengYeWuYiChang(
                "TOPIC_LEARNING_NOT_FOUND", "专题题不存在或不可访问", HttpStatus.NOT_FOUND));
    }

    private List<TopicLearningDtos.Attachment> attachments(long questionId,String position){
        return jdbc.query("""
                SELECT id,guan_lian_wei_zhi,dui_xiang_biao_shi,yuan_shi_wen_jian_ming,fu_jian_lei_xing,
                       COALESCE(dui_xiang_biao_shi,''),pai_xu
                FROM ti_mu_fu_jian WHERE ti_mu_id=? AND guan_lian_wei_zhi=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY pai_xu,id
                """,(rs,row)->{String name=rs.getString(4);long id=rs.getLong(1);
                    return new TopicLearningDtos.Attachment(id,rs.getString(2),rs.getString(5),name,rs.getString(3),"ACTIVE","AVAILABLE",rs.getString(6),rs.getInt(7),
                            "/api/v1/student/topic-learning/"+questionId+"/attachments/"+id+"/content");},questionId,position);
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
        String value = normalizer.normalize(stem);
        int separator = value == null ? -1 : value.indexOf('｜');
        if (separator < 0) return new String[] { "综合题", value == null ? "" : value };
        return new String[] { value.substring(0, separator), value.substring(separator + 1) };
    }
}
