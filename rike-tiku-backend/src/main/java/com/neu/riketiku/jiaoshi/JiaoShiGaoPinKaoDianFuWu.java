package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianXiangYing;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianXiuGaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianZhuangTaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.XueShengGaoPinKaoDianXiangYing;
import com.neu.riketiku.jiaoshi.dto.JiaoShiGongZuoTaiXiangYing;
import com.neu.riketiku.jiaoshi.dto.JiaoShiGongZuoTaiXiangYing.XueShengJiBenXiangYing;
import com.neu.riketiku.jiaoshi.dto.JiaoShiGongZuoTaiXiangYing.KnowledgePointOption;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiaoShiGaoPinKaoDianFuWu {
    private final JdbcTemplate jdbc;

    public JiaoShiGaoPinKaoDianFuWu(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public JiaoShiGongZuoTaiXiangYing getWorkspace(long userId, long scopeId) {
        Scope scope = requireScope(userId, scopeId);
        List<XueShengJiBenXiangYing> students = jdbc.query("""
                SELECT p.id,p.xue_hao,p.xing_ming,p.nian_ji
                FROM ban_ji_xue_sheng bx
                JOIN xue_sheng_dang_an p ON p.id=bx.xue_sheng_id
                JOIN yong_hu u ON u.id=p.yong_hu_id
                WHERE bx.ban_ji_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE'
                  AND bx.tui_chu_shi_jian IS NULL AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0
                  AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                ORDER BY p.xue_hao,p.id
                """, (rs, row) -> new XueShengJiBenXiangYing(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), scope.classId);
        List<GaoPinKaoDianXiangYing> points = findTeacherPoints(scopeId);
        List<KnowledgePointOption> knowledgePoints = jdbc.query("""
                SELECT id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing
                FROM zhi_shi_dian
                WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY pai_xu,id
                """, (rs, row) -> new KnowledgePointOption(rs.getLong(1), rs.getString(2), rs.getString(3)), scope.subjectId);
        return new JiaoShiGongZuoTaiXiangYing(scope.id, scope.classId, scope.className, scope.grade,
                scope.subjectId, scope.subjectCode, scope.subjectName, scope.teacherName, students.size(), students, points, knowledgePoints);
    }

    @Transactional(readOnly = true)
    public List<GaoPinKaoDianXiangYing> listForTeacher(long userId, long scopeId) {
        requireScope(userId, scopeId);
        return findTeacherPoints(scopeId);
    }

    @Transactional
    public GaoPinKaoDianXiangYing create(long userId, long scopeId, GaoPinKaoDianChuangJianQingQiu request) {
        Scope scope = requireScope(userId, scopeId);
        requireKnowledgePoint(scope.subjectId, request.knowledgePointId());
        jdbc.update("""
                INSERT INTO gao_pin_kao_dian(
                    ren_ke_guan_xi_id,zhi_shi_dian_id,biao_ti,nei_rong,ji_yi_kou_jue,chang_jian_wu_qu,pai_xu,zhuang_tai)
                VALUES (?,?,?,?,?,?,?,'PUBLISHED')
                """, scopeId, request.knowledgePointId(), trim(request.title()), trim(request.content()),
                trim(request.memoryTrick()), trim(request.commonMistake()), request.sortOrder());
        return findTeacherPointById(userId, lastId());
    }

    @Transactional
    public GaoPinKaoDianXiangYing update(long userId, long pointId, GaoPinKaoDianXiuGaiQingQiu request) {
        requirePointScope(userId, pointId);
        jdbc.update("""
                UPDATE gao_pin_kao_dian
                SET biao_ti=?,nei_rong=?,ji_yi_kou_jue=?,chang_jian_wu_qu=?,pai_xu=?
                WHERE id=? AND yi_shan_chu=0
                """, trim(request.title()), trim(request.content()), trim(request.memoryTrick()),
                trim(request.commonMistake()), request.sortOrder(), pointId);
        return findTeacherPointById(userId, pointId);
    }

    @Transactional
    public GaoPinKaoDianXiangYing updateStatus(long userId, long pointId, GaoPinKaoDianZhuangTaiQingQiu request) {
        requirePointScope(userId, pointId);
        if (!"PUBLISHED".equals(request.status()) && !"DISABLED".equals(request.status())) {
            fail("HIGH_FREQUENCY_STATUS_INVALID", "知识卡片状态只能是已发布或停用", HttpStatus.BAD_REQUEST);
        }
        jdbc.update("UPDATE gao_pin_kao_dian SET zhuang_tai=? WHERE id=? AND yi_shan_chu=0", request.status(), pointId);
        return findTeacherPointById(userId, pointId);
    }

    @Transactional(readOnly = true)
    public List<XueShengGaoPinKaoDianXiangYing> listForStudent(long userId, long subjectId) {
        Long studentId = jdbc.query("""
                SELECT p.id
                FROM xue_sheng_dang_an p JOIN yong_hu u ON u.id=p.yong_hu_id
                WHERE p.yong_hu_id=? AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0
                  AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (studentId == null) {
            fail("STUDENT_PROFILE_UNAVAILABLE", "当前账号没有有效学生档案", HttpStatus.FORBIDDEN);
        }
        if (count("SELECT COUNT(*) FROM ke_mu WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", subjectId) == 0) {
            fail("SUBJECT_UNAVAILABLE", "科目不存在或已停用", HttpStatus.BAD_REQUEST);
        }
        return jdbc.query("""
                SELECT h.id,h.zhi_shi_dian_id,k.zhi_shi_dian_ming_cheng,h.biao_ti,h.nei_rong,
                       h.ji_yi_kou_jue,h.chang_jian_wu_qu,h.pai_xu,t.xing_ming
                FROM ban_ji_xue_sheng bx
                JOIN ren_ke_guan_xi r ON r.ban_ji_id=bx.ban_ji_id AND r.ke_mu_id=? AND r.zhuang_tai='ACTIVE'
                JOIN gao_pin_kao_dian h ON h.ren_ke_guan_xi_id=r.id AND h.zhuang_tai='PUBLISHED' AND h.yi_shan_chu=0
                JOIN zhi_shi_dian k ON k.id=h.zhi_shi_dian_id AND k.ke_mu_id=r.ke_mu_id
                    AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id AND t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0
                WHERE bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE'
                  AND bx.tui_chu_shi_jian IS NULL
                ORDER BY h.pai_xu,h.id
                """, (rs, row) -> new XueShengGaoPinKaoDianXiangYing(
                rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getInt(8), rs.getString(9)), subjectId, studentId);
    }

    private Scope requireScope(long userId, long scopeId) {
        Scope scope = jdbc.query("""
                SELECT r.id,r.ban_ji_id,b.ban_ji_ming_cheng,b.nian_ji,r.ke_mu_id,k.ke_mu_dai_ma,k.ke_mu_ming_cheng,j.xing_ming
                FROM ren_ke_guan_xi r
                JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id
                JOIN ban_ji b ON b.id=r.ban_ji_id
                JOIN ke_mu k ON k.id=r.ke_mu_id
                WHERE r.id=? AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE'
                  AND j.zhuang_tai='ACTIVE' AND j.yi_shan_chu=0
                  AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                  AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                """, rs -> rs.next() ? new Scope(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getLong(5), rs.getString(6), rs.getString(7), rs.getString(8)) : null, scopeId, userId);
        if (scope == null) {
            fail("TEACHING_SCOPE_FORBIDDEN", "任教关系不存在、已停用或不属于当前教师", HttpStatus.FORBIDDEN);
        }
        return scope;
    }

    private List<GaoPinKaoDianXiangYing> findTeacherPoints(long scopeId) {
        return jdbc.query("""
                SELECT h.id,h.ren_ke_guan_xi_id,h.zhi_shi_dian_id,k.zhi_shi_dian_ming_cheng,h.biao_ti,h.nei_rong,
                       h.ji_yi_kou_jue,h.chang_jian_wu_qu,h.pai_xu,h.zhuang_tai,t.xing_ming
                FROM gao_pin_kao_dian h
                JOIN zhi_shi_dian k ON k.id=h.zhi_shi_dian_id
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id
                WHERE h.ren_ke_guan_xi_id=? AND h.yi_shan_chu=0
                ORDER BY h.pai_xu,h.id
                """, this::teacherPointMapper, scopeId);
    }

    private GaoPinKaoDianXiangYing findTeacherPointById(long userId, long pointId) {
        requirePointScope(userId, pointId);
        List<GaoPinKaoDianXiangYing> points = jdbc.query("""
                SELECT h.id,h.ren_ke_guan_xi_id,h.zhi_shi_dian_id,k.zhi_shi_dian_ming_cheng,h.biao_ti,h.nei_rong,
                       h.ji_yi_kou_jue,h.chang_jian_wu_qu,h.pai_xu,h.zhuang_tai,t.xing_ming
                FROM gao_pin_kao_dian h JOIN zhi_shi_dian k ON k.id=h.zhi_shi_dian_id
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id
                WHERE h.id=? AND h.yi_shan_chu=0
                """, this::teacherPointMapper, pointId);
        if (points.isEmpty()) fail("HIGH_FREQUENCY_POINT_NOT_FOUND", "高频考点不存在", HttpStatus.NOT_FOUND);
        return points.getFirst();
    }

    private long requirePointScope(long userId, long pointId) {
        Long scopeId = jdbc.query("""
                SELECT h.ren_ke_guan_xi_id FROM gao_pin_kao_dian h
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
                WHERE h.id=? AND h.yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, pointId);
        if (scopeId == null) fail("HIGH_FREQUENCY_POINT_NOT_FOUND", "高频考点不存在", HttpStatus.NOT_FOUND);
        requireScope(userId, scopeId);
        return scopeId;
    }

    private void requireKnowledgePoint(long subjectId, Long pointId) {
        if (count("SELECT COUNT(*) FROM zhi_shi_dian WHERE id=? AND ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", pointId, subjectId) == 0) {
            fail("KNOWLEDGE_POINT_INVALID", "知识点不存在、已停用或不属于当前科目", HttpStatus.BAD_REQUEST);
        }
    }

    private GaoPinKaoDianXiangYing teacherPointMapper(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new GaoPinKaoDianXiangYing(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getInt(9), rs.getString(10), rs.getString(11));
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void fail(String code, String message, HttpStatus status) {
        throw new RenZhengYeWuYiChang(code, message, status);
    }

    private record Scope(long id, long classId, String className, String grade, long subjectId,
            String subjectCode, String subjectName, String teacherName) {
    }
}
