package com.neu.riketiku.xuexizhangwo;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.xueshenglianxi.StudentPracticeService;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.KnowledgePointSummary;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.OverallSummary;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.PracticeParameters;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.Recommendation;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.StudentLearningSummary;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.SubjectResponse;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.TeacherScopeLearningSummary;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.TeacherStudentLearningSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningMasteryService {
    private static final String GOOD_PERFORMANCE_MESSAGE = "当前已练习知识点整体表现良好，可以进行综合随机练习。";
    private static final String NO_ELIGIBLE_RECOMMENDATION_MESSAGE =
            "当前暂无题量充足的知识点可生成5题巩固练习，可以先进行综合练习。";
    private final JdbcTemplate jdbc;
    private final StudentPracticeService studentPracticeService;

    public LearningMasteryService(JdbcTemplate jdbc, StudentPracticeService studentPracticeService) {
        this.jdbc = jdbc;
        this.studentPracticeService = studentPracticeService;
    }

    @Transactional(readOnly = true)
    public StudentLearningSummary studentSummary(long userId, long subjectId) {
        long studentId = requireStudent(userId);
        SubjectResponse subject = requireSubject(subjectId);
        return calculate(studentId, subject);
    }

    @Transactional(readOnly = true)
    public TeacherScopeLearningSummary teacherScopeSummary(long userId, long scopeId) {
        Scope scope = requireScope(userId, scopeId);
        SubjectResponse subject = new SubjectResponse(scope.subjectId, scope.subjectCode, scope.subjectName);
        List<StudentSeed> students = jdbc.query("""
                SELECT s.id,s.xue_hao,s.xing_ming,s.nian_ji
                FROM ban_ji_xue_sheng bx
                JOIN xue_sheng_dang_an s ON s.id=bx.xue_sheng_id AND s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0
                JOIN yong_hu u ON u.id=s.yong_hu_id AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                WHERE bx.ban_ji_id=? AND bx.shi_fou_zhu_ban_ji=1
                  AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                ORDER BY s.xue_hao,s.id
                """, (rs, row) -> new StudentSeed(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                scope.classId);
        List<TeacherStudentLearningSummary> summaries = students.stream().map(student -> {
            StudentLearningSummary summary = calculate(student.id, subject);
            OverallSummary overall = summary.overall();
            return new TeacherStudentLearningSummary(student.id, student.studentNumber, student.name, student.grade,
                    overall.totalAnsweredCount(), overall.totalCorrectCount(), overall.overallAccuracy(),
                    overall.weakKnowledgePointCount(), overall.masteredKnowledgePointCount());
        }).toList();
        return new TeacherScopeLearningSummary(scope.id, scope.className, scope.subjectId, scope.subjectName, summaries);
    }

    private StudentLearningSummary calculate(long studentId, SubjectResponse subject) {
        Map<Long, Integer> activeWrongCounts = activeWrongCounts(studentId, subject.id());
        List<KnowledgePointSummary> points = jdbc.query("""
                SELECT k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing,
                       COUNT(a.answer_id),COALESCE(SUM(a.correct),0)
                FROM zhi_shi_dian k
                LEFT JOIN (
                    SELECT DISTINCT da.id answer_id,da.shi_fou_zheng_que correct,jt.knowledge_point_id
                    FROM xue_sheng_da_ti da
                    JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                    JOIN lian_xi_hui_hua h ON h.id=lt.lian_xi_hui_hua_id AND h.zhuang_tai='SUBMITTED'
                    JOIN xue_xi_jie_guo result ON result.lian_xi_hui_hua_id=h.id
                    JOIN JSON_TABLE(lt.zhi_shi_dian_kuai_zhao, '$[*]'
                        COLUMNS (knowledge_point_id BIGINT PATH '$.id')) jt
                    WHERE da.xue_sheng_id=? AND h.xue_sheng_id=? AND h.ke_mu_id=?
                      AND lt.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')
                ) a ON a.knowledge_point_id=k.id
                WHERE k.ke_mu_id=? AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                GROUP BY k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing,k.pai_xu
                ORDER BY k.pai_xu,k.id
                """, (rs, row) -> point(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                rs.getInt(5), activeWrongCounts.getOrDefault(rs.getLong(1), 0)),
                studentId, studentId, subject.id(), subject.id());

        int[] overallCounts = jdbc.queryForObject("""
                SELECT COUNT(*),COALESCE(SUM(da.shi_fou_zheng_que),0)
                FROM xue_sheng_da_ti da
                JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                JOIN lian_xi_hui_hua h ON h.id=lt.lian_xi_hui_hua_id AND h.zhuang_tai='SUBMITTED'
                JOIN xue_xi_jie_guo result ON result.lian_xi_hui_hua_id=h.id
                WHERE da.xue_sheng_id=? AND h.xue_sheng_id=? AND h.ke_mu_id=?
                  AND lt.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')
                """, (rs, row) -> new int[]{rs.getInt(1), rs.getInt(2)}, studentId, studentId, subject.id());
        OverallSummary overall = overall(points, overallCounts[0], overallCounts[1]);
        Set<Long> eligiblePointIds = studentPracticeService.recommendationEligibleKnowledgePointIds(subject.id(), 5);
        List<Recommendation> recommendations = recommendations(subject.id(), points, eligiblePointIds);
        boolean allMastered = !points.isEmpty()
                && points.stream().allMatch(point -> point.masteryLevel().equals("MASTERED"));
        String message = recommendations.isEmpty()
                ? (allMastered ? GOOD_PERFORMANCE_MESSAGE : NO_ELIGIBLE_RECOMMENDATION_MESSAGE)
                : null;
        return new StudentLearningSummary(subject, overall, points, recommendations, message);
    }

    private Map<Long, Integer> activeWrongCounts(long studentId, long subjectId) {
        Map<Long, Integer> result = new HashMap<>();
        jdbc.query("""
                SELECT jt.knowledge_point_id,COUNT(DISTINCT c.id)
                FROM cuo_ti_ji_lu c
                JOIN xue_sheng_da_ti da ON da.id=c.zui_jin_da_ti_id
                JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                JOIN lian_xi_hui_hua h ON h.id=lt.lian_xi_hui_hua_id
                JOIN JSON_TABLE(lt.zhi_shi_dian_kuai_zhao, '$[*]'
                    COLUMNS (knowledge_point_id BIGINT PATH '$.id')) jt
                WHERE c.xue_sheng_id=? AND c.zhuang_tai IN ('NEW','REVIEWING') AND h.ke_mu_id=?
                GROUP BY jt.knowledge_point_id
                """, rs -> {
            while (rs.next()) result.put(rs.getLong(1), rs.getInt(2));
            return null;
        }, studentId, subjectId);
        return result;
    }

    private KnowledgePointSummary point(long id, String name, String path, int answered, int correct, int activeWrong) {
        BigDecimal accuracy = percentage(correct, answered);
        String level;
        if (answered == 0) {
            level = "NOT_STARTED";
        } else if (answered <= 2) {
            level = "INSUFFICIENT";
        } else if (accuracy.compareTo(BigDecimal.valueOf(60)) < 0) {
            level = "WEAK";
        } else if (accuracy.compareTo(BigDecimal.valueOf(80)) < 0 || activeWrong > 0) {
            level = "IMPROVING";
        } else {
            level = "MASTERED";
        }
        return new KnowledgePointSummary(id, name, path, answered, correct, answered - correct, accuracy, activeWrong, level);
    }

    private OverallSummary overall(List<KnowledgePointSummary> points, int answered, int correct) {
        return new OverallSummary(
                count(points, point -> point.answeredCount() > 0),
                points.size(), answered, correct, percentage(correct, answered),
                count(points, point -> point.masteryLevel().equals("WEAK")),
                count(points, point -> point.masteryLevel().equals("IMPROVING")),
                count(points, point -> point.masteryLevel().equals("MASTERED")),
                count(points, point -> point.masteryLevel().equals("INSUFFICIENT")),
                count(points, point -> point.masteryLevel().equals("NOT_STARTED")));
    }

    private List<Recommendation> recommendations(long subjectId, List<KnowledgePointSummary> points,
            Set<Long> eligiblePointIds) {
        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (KnowledgePointSummary point : points) {
            if (!eligiblePointIds.contains(point.knowledgePointId())) {
                continue;
            }
            int priority;
            String reason;
            if (point.activeWrongQuestionCount() > 0) {
                priority = 1;
                reason = "该知识点仍有未完成复习的错题。";
            } else if (point.answeredCount() >= 3 && point.accuracy().compareTo(BigDecimal.valueOf(60)) < 0) {
                priority = 2;
                reason = "该知识点近期练习正确率较低，建议优先巩固。";
            } else if (point.answeredCount() >= 3 && point.accuracy().compareTo(BigDecimal.valueOf(80)) < 0) {
                priority = 3;
                reason = "该知识点仍处于巩固阶段。";
            } else if (point.answeredCount() > 0 && point.answeredCount() <= 2) {
                priority = 4;
                reason = "当前练习样本较少，建议继续练习以确认掌握情况。";
            } else if (point.answeredCount() == 0) {
                priority = 5;
                reason = "该知识点尚未开始练习。";
            } else {
                continue;
            }
            candidates.add(new RecommendationCandidate(point, priority, reason));
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(RecommendationCandidate::priority)
                        .thenComparing(candidate -> candidate.point().accuracy(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.point().knowledgePointId()))
                .limit(3)
                .map(candidate -> new Recommendation(candidate.point().knowledgePointId(),
                        candidate.point().knowledgePointName(), candidate.reason(),
                        new PracticeParameters(subjectId, candidate.point().knowledgePointId(), 5)))
                .toList();
    }

    private long requireStudent(long userId) {
        Long id = jdbc.query("""
                SELECT s.id FROM xue_sheng_dang_an s
                JOIN yong_hu u ON u.id=s.yong_hu_id AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                WHERE s.yong_hu_id=? AND s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (id == null) fail("STUDENT_PROFILE_REQUIRED", "当前账号没有有效学生档案", HttpStatus.FORBIDDEN);
        return id;
    }

    private SubjectResponse requireSubject(long subjectId) {
        SubjectResponse subject = jdbc.query("""
                SELECT id,ke_mu_dai_ma,ke_mu_ming_cheng FROM ke_mu
                WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, rs -> rs.next() ? new SubjectResponse(rs.getLong(1), rs.getString(2), rs.getString(3)) : null, subjectId);
        if (subject == null) fail("SUBJECT_NOT_FOUND", "科目不存在或已停用", HttpStatus.BAD_REQUEST);
        return subject;
    }

    private Scope requireScope(long userId, long scopeId) {
        Scope scope = jdbc.query("""
                SELECT r.id,r.ban_ji_id,b.ban_ji_ming_cheng,r.ke_mu_id,k.ke_mu_dai_ma,k.ke_mu_ming_cheng
                FROM jiao_shi_dang_an t
                JOIN ren_ke_guan_xi r ON r.jiao_shi_id=t.id AND r.id=? AND r.zhuang_tai='ACTIVE'
                JOIN ban_ji b ON b.id=r.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                JOIN ke_mu k ON k.id=r.ke_mu_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                WHERE t.yong_hu_id=? AND t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0
                """, rs -> rs.next() ? new Scope(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4),
                rs.getString(5), rs.getString(6)) : null, scopeId, userId);
        if (scope == null) fail("TEACHING_SCOPE_FORBIDDEN", "无权查看该任教范围的学习情况", HttpStatus.FORBIDDEN);
        return scope;
    }

    private BigDecimal percentage(int correct, int answered) {
        if (answered == 0) return null;
        return BigDecimal.valueOf(correct).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answered), 1, RoundingMode.HALF_UP);
    }

    private int count(List<KnowledgePointSummary> points, java.util.function.Predicate<KnowledgePointSummary> predicate) {
        return (int) points.stream().filter(predicate).count();
    }

    private void fail(String code, String message, HttpStatus status) {
        throw new RenZhengYeWuYiChang(code, message, status);
    }

    private record Scope(long id, long classId, String className, long subjectId, String subjectCode, String subjectName) {
    }

    private record StudentSeed(long id, String studentNumber, String name, String grade) {
    }

    private record RecommendationCandidate(KnowledgePointSummary point, int priority, String reason) {
    }
}
